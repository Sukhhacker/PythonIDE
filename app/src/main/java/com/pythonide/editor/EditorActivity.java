package com.pythonide.editor;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.pythonide.MainViewModel;
import com.pythonide.R;
import com.pythonide.file.FileManager;
import com.pythonide.python.PythonExecutor;

import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import io.github.rosemoe.sora.lang.completion.CompletionItem;

public class EditorActivity extends AppCompatActivity {
    
    private CodeEditor editor;
    private ProgressBar progressBar;
    private FileManager fileManager;
    private MainViewModel viewModel;
    
    private String currentFilePath;
    private String currentFileName;
    private boolean isNewFile;
    private boolean isModified = false;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        
        editor = findViewById(R.id.editor);
        progressBar = findViewById(R.id.progressBar);
        
        fileManager = FileManager.getInstance();
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        setupEditor();
        
        // Get file info from intent
        if (getIntent() != null) {
            currentFilePath = getIntent().getStringExtra("file_path");
            currentFileName = getIntent().getStringExtra("file_name");
            isNewFile = getIntent().getBooleanExtra("is_new_file", false);
            
            if (currentFileName == null && currentFilePath != null) {
                currentFileName = currentFilePath.substring(currentFilePath.lastIndexOf('/') + 1);
            }
            
            if (currentFileName != null) {
                setTitle(currentFileName);
            }
            
            if (!isNewFile && currentFilePath != null) {
                loadFile();
            } else {
                editor.setText("# New Python File\n\n");
                isModified = false;
            }
        }
        
        // Setup change listener
        editor.getText().addOnTextChangedListener(() -> {
            isModified = true;
            updateTitle();
        });
    }
    
    private void setupEditor() {
        // Set color scheme
        editor.setColorScheme(new SchemeDarcula());
        
        // Use basic Python language syntax highlighting
        editor.setEditorLanguage(new PythonLanguage(editor));
        
        // Setup editor components
        editor.getProps().tabWidth = 4;
        editor.getProps().useTab = false;
        editor.getProps().lineNumberEnabled = true;
        editor.getProps().pinLineNumber = true;
        editor.getProps().symbolInputEnabled = true;
        editor.getProps().wordwrap = false;
        
        // Enable autocomplete
        EditorAutoCompletion autoCompletion = new EditorAutoCompletion(editor);
        autoCompletion.setEnabled(true);
        autoCompletion.setAutoCompletionItemsLoader((code, position) -> {
            List<CompletionItem> items = new ArrayList<>();
            
            // Python keywords
            String[] keywords = {
                "False", "None", "True", "and", "as", "assert", "async", "await",
                "break", "class", "continue", "def", "del", "elif", "else", "except",
                "finally", "for", "from", "global", "if", "import", "in", "is",
                "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
                "try", "while", "with", "yield"
            };
            
            for (String keyword : keywords) {
                items.add(new CompletionItem(keyword, "Keyword"));
            }
            
            // Built-in functions
            String[] builtins = {
                "abs", "all", "any", "bin", "bool", "callable", "chr", "dict",
                "dir", "enumerate", "eval", "exec", "filter", "float", "format",
                "getattr", "hasattr", "hash", "help", "hex", "id", "input", "int",
                "isinstance", "issubclass", "iter", "len", "list", "locals", "map",
                "max", "min", "next", "oct", "open", "ord", "pow", "print",
                "range", "repr", "reversed", "round", "set", "setattr", "slice",
                "sorted", "str", "sum", "tuple", "type", "vars", "zip"
            };
            
            for (String builtin : builtins) {
                items.add(new CompletionItem(builtin, "Built-in function"));
            }
            
            return items;
        });
        editor.setComponent(EditorAutoCompletion.class, autoCompletion);
        
        // Enable text actions
        editor.setComponent(EditorTextActionWindow.class, new EditorTextActionWindow(editor));
        
        // Additional setup
        editor.setLineSpacing(2f, 1.1f);
        editor.setTextSize(14);
    }
    
    private void loadFile() {
        progressBar.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            try {
                String content = fileManager.readFile(currentFilePath);
                
                runOnUiThread(() -> {
                    editor.setText(content);
                    progressBar.setVisibility(View.GONE);
                    isModified = false;
                    updateTitle();
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load file: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void saveFile() {
        if (currentFilePath == null) {
            showSaveAsDialog();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            String content = editor.getText().toString();
            boolean success = fileManager.writeFile(currentFilePath, content);
            
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (success) {
                    isModified = false;
                    updateTitle();
                    Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    private void showSaveAsDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(currentFileName != null ? currentFileName : "untitled.py");
        input.setPadding(32, 16, 32, 16);
        
        new AlertDialog.Builder(this)
                .setTitle("Save As")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String fileName = input.getText().toString().trim();
                    if (!fileName.isEmpty()) {
                        if (!fileName.endsWith(".py")) {
                            fileName += ".py";
                        }
                        java.io.File downloadsDir = 
                                android.os.Environment.getExternalStoragePublicDirectory(
                                        android.os.Environment.DIRECTORY_DOWNLOADS);
                        currentFilePath = new java.io.File(downloadsDir, fileName).getAbsolutePath();
                        currentFileName = fileName;
                        setTitle(currentFileName);
                        saveFile();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void runCode() {
        String code = editor.getText().toString();
        if (code.trim().isEmpty()) {
            Toast.makeText(this, "No code to run", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isModified && currentFilePath != null) {
            saveFile();
        }
        
        viewModel.clearConsoleOutput();
        viewModel.appendConsoleOutput("Running: " + currentFileName + "\n\n");
        
        PythonExecutor.getInstance().executeCode(code, new PythonExecutor.ExecutionCallback() {
            @Override
            public void onOutput(String output) {
                runOnUiThread(() -> viewModel.appendConsoleOutput(output));
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> viewModel.appendConsoleOutput("Error: " + error + "\n"));
            }
            
            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    viewModel.appendConsoleOutput("\nExecution completed.\n");
                    Toast.makeText(EditorActivity.this, "Execution finished", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void updateTitle() {
        String title = (currentFileName != null ? currentFileName : "Untitled");
        if (isModified) {
            title = "*" + title;
        }
        setTitle(title);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_save) {
            saveFile();
            return true;
        } else if (itemId == R.id.action_save_as) {
            showSaveAsDialog();
            return true;
        } else if (itemId == R.id.action_run) {
            runCode();
            return true;
        } else if (itemId == R.id.action_undo) {
            editor.undo();
            return true;
        } else if (itemId == R.id.action_redo) {
            editor.redo();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (isModified) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("Do you want to save changes?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        saveFile();
                        finish();
                    })
                    .setNegativeButton("Discard", (dialog, which) -> finish())
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}
