package com.pythonide.file;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.anggrayudi.storage.SimpleStorageHelper;
import com.pythonide.MainViewModel;
import com.pythonide.R;
import com.pythonide.editor.EditorActivity;

import java.util.List;

public class FileExplorerFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private FileExplorerAdapter adapter;
    private FileManager fileManager;
    private MainViewModel viewModel;
    private SimpleStorageHelper storageHelper;
    
    private String currentPath;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        
        fileManager = FileManager.getInstance();
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        storageHelper = new SimpleStorageHelper(this);
        fileManager.init(requireContext(), storageHelper);
        
        // Set initial path
        currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_explorer, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FileExplorerAdapter();
        adapter.setOnItemClickListener(this::onItemClick);
        adapter.setOnItemLongClickListener(this::onItemLongClick);
        recyclerView.setAdapter(adapter);
        
        swipeRefresh.setOnRefreshListener(this::loadFiles);
        
        loadFiles();
        
        return view;
    }
    
    private void loadFiles() {
        swipeRefresh.setRefreshing(true);
        
        new Thread(() -> {
            List<FileManager.FileItem> files = fileManager.listFiles(currentPath);
            
            requireActivity().runOnUiThread(() -> {
                adapter.setFiles(files);
                swipeRefresh.setRefreshing(false);
                
                // Update title
                FileManager.FileItem parentItem = new FileManager.FileItem();
                parentItem.name = "...";
                parentItem.path = fileManager.getParentPath(currentPath);
                parentItem.isDirectory = true;
                adapter.setParentItem(parentItem);
            });
        }).start();
    }
    
    private void onItemClick(FileManager.FileItem item) {
        if (item.isDirectory) {
            // Navigate to directory
            currentPath = item.path;
            loadFiles();
        } else {
            // Open file in editor
            Intent intent = new Intent(getActivity(), EditorActivity.class);
            intent.putExtra("file_path", item.path);
            intent.putExtra("file_name", item.name);
            startActivity(intent);
        }
    }
    
    private void onItemLongClick(FileManager.FileItem item) {
        showFileOptionsDialog(item);
    }
    
    private void showFileOptionsDialog(FileManager.FileItem item) {
        String[] options = {"Rename", "Delete", "Copy Path", "Run Python Script"};
        
        new AlertDialog.Builder(requireContext())
                .setTitle(item.name)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showRenameDialog(item);
                            break;
                        case 1:
                            showDeleteDialog(item);
                            break;
                        case 2:
                            copyPathToClipboard(item.path);
                            break;
                        case 3:
                            if (item.fileType == FileManager.FileType.PYTHON) {
                                runPythonScript(item.path);
                            } else {
                                Toast.makeText(getContext(), "Not a Python file", Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showRenameDialog(FileManager.FileItem item) {
        EditText input = new EditText(getContext());
        input.setText(item.name);
        input.setPadding(32, 16, 32, 16);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(item.name)) {
                        if (fileManager.renameFile(item.path, newName)) {
                            Toast.makeText(getContext(), "Renamed successfully", Toast.LENGTH_SHORT).show();
                            loadFiles();
                        } else {
                            Toast.makeText(getContext(), "Failed to rename", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showDeleteDialog(FileManager.FileItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete " + item.name + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (fileManager.deleteFile(item.path)) {
                        Toast.makeText(getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                        loadFiles();
                    } else {
                        Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void copyPathToClipboard(String path) {
        android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("File Path", path);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), "Path copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    private void runPythonScript(String path) {
        // Implementation for running Python script
        viewModel.setCurrentFile(path);
        Toast.makeText(getContext(), "Running: " + path, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.file_explorer_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_new_file) {
            showCreateFileDialog();
            return true;
        } else if (itemId == R.id.action_new_folder) {
            showCreateFolderDialog();
            return true;
        } else if (itemId == R.id.action_refresh) {
            loadFiles();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showCreateFileDialog() {
        EditText input = new EditText(getContext());
        input.setHint("filename.py");
        input.setPadding(32, 16, 32, 16);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Create New File")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        if (!name.endsWith(".py")) {
                            name += ".py";
                        }
                        if (fileManager.createFile(currentPath, name)) {
                            Toast.makeText(getContext(), "File created", Toast.LENGTH_SHORT).show();
                            loadFiles();
                            
                            // Open in editor
                            String filePath = currentPath + "/" + name;
                            Intent intent = new Intent(getActivity(), EditorActivity.class);
                            intent.putExtra("file_path", filePath);
                            intent.putExtra("file_name", name);
                            intent.putExtra("is_new_file", true);
                            startActivity(intent);
                        } else {
                            Toast.makeText(getContext(), "Failed to create file", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showCreateFolderDialog() {
        EditText input = new EditText(getContext());
        input.setHint("folder name");
        input.setPadding(32, 16, 32, 16);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Create New Folder")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        if (fileManager.createDirectory(currentPath, name)) {
                            Toast.makeText(getContext(), "Folder created", Toast.LENGTH_SHORT).show();
                            loadFiles();
                        } else {
                            Toast.makeText(getContext(), "Failed to create folder", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        storageHelper.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        storageHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}