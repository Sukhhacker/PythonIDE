package com.pythonide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.pythonide.python.PythonExecutor;

public class ConsoleFragment extends Fragment {
    
    private TextView outputText;
    private EditText inputEdit;
    private Button runButton;
    private Button clearButton;
    private ScrollView scrollView;
    private MainViewModel viewModel;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_console, container, false);
        
        outputText = view.findViewById(R.id.outputText);
        inputEdit = view.findViewById(R.id.inputEdit);
        runButton = view.findViewById(R.id.runButton);
        clearButton = view.findViewById(R.id.clearButton);
        scrollView = view.findViewById(R.id.scrollView);
        
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        // Observe console output
        viewModel.getConsoleOutput().observe(getViewLifecycleOwner(), output -> {
            outputText.setText(output);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
        
        runButton.setOnClickListener(v -> {
            String code = inputEdit.getText().toString().trim();
            if (!code.isEmpty()) {
                executeCode(code);
            }
        });
        
        clearButton.setOnClickListener(v -> {
            viewModel.clearConsoleOutput();
            inputEdit.setText("");
        });
        
        return view;
    }
    
    private void executeCode(String code) {
        viewModel.appendConsoleOutput(">>> " + code + "\n");
        
        PythonExecutor.getInstance().executeCode(code, new PythonExecutor.ExecutionCallback() {
            @Override
            public void onOutput(String output) {
                requireActivity().runOnUiThread(() -> {
                    viewModel.appendConsoleOutput(output);
                    if (!output.endsWith("\n")) {
                        viewModel.appendConsoleOutput("\n");
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    viewModel.appendConsoleOutput("Error: " + error + "\n");
                });
            }
            
            @Override
            public void onComplete() {
                requireActivity().runOnUiThread(() -> {
                    inputEdit.setText("");
                });
            }
        });
    }
}