package com.pythonide.python;

import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PythonExecutor {
    
    private static final String TAG = "PythonExecutor";
    private static PythonExecutor instance;
    private final ExecutorService executorService;
    
    private PythonExecutor() {
        executorService = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized PythonExecutor getInstance() {
        if (instance == null) {
            instance = new PythonExecutor();
        }
        return instance;
    }
    
    public void executeCode(String code, ExecutionCallback callback) {
        executorService.execute(() -> {
            try {
                Python python = PythonInitializer.getInstance().getPython();
                
                // Capture stdout and stderr
                ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
                ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
                PrintStream originalOut = System.out;
                PrintStream originalErr = System.err;
                
                System.setOut(new PrintStream(baosOut));
                System.setErr(new PrintStream(baosErr));
                
                try {
                    // Execute Python code
                    PyObject builtins = python.getBuiltins();
                    PyObject execFunc = builtins.get("exec");
                    execFunc.call(code);
                    
                    String output = baosOut.toString();
                    String error = baosErr.toString();
                    
                    if (!output.isEmpty()) {
                        callback.onOutput(output);
                    }
                    if (!error.isEmpty()) {
                        callback.onError(error);
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Python execution error", e);
                    callback.onError(e.getMessage());
                } finally {
                    System.setOut(originalOut);
                    System.setErr(originalErr);
                }
                
                callback.onComplete();
                
            } catch (Exception e) {
                Log.e(TAG, "Execution failed", e);
                callback.onError(e.getMessage());
                callback.onComplete();
            }
        });
    }
    
    public void executeScript(String scriptPath, ExecutionCallback callback) {
        executorService.execute(() -> {
            try {
                Python python = PythonInitializer.getInstance().getPython();
                PyObject builtins = python.getBuiltins();
                
                // Read and execute script
                PyObject openFunc = builtins.get("open");
                PyObject file = openFunc.call(scriptPath, "r");
                String code = file.callAttr("read").toString();
                file.callAttr("close");
                
                executeCode(code, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Script execution failed", e);
                callback.onError(e.getMessage());
                callback.onComplete();
            }
        });
    }
    
    public void getAutocompleteSuggestions(String code, int cursorPosition, AutocompleteCallback callback) {
        executorService.execute(() -> {
            try {
                Python python = PythonInitializer.getInstance().getPython();
                
                // Use Jedi for autocomplete
                String jediCode = 
                    "import jedi\n" +
                    "try:\n" +
                    "    script = jedi.Script('" + code.replace("'", "\\'").replace("\n", "\\n") + "')\n" +
                    "    completions = script.complete(" + cursorPosition / 2 + ", 0)\n" +
                    "    result = [c.name for c in completions]\n" +
                    "except:\n" +
                    "    result = []\n";
                
                PyObject builtins = python.getBuiltins();
                builtins.get("exec").call(jediCode);
                PyObject result = python.getModule("__main__").get("result");
                
                String[] suggestions = new String[result.asList().size()];
                for (int i = 0; i < suggestions.length; i++) {
                    suggestions[i] = result.asList().get(i).toString();
                }
                
                callback.onSuggestions(suggestions);
                
            } catch (Exception e) {
                Log.e(TAG, "Autocomplete failed", e);
                callback.onSuggestions(new String[0]);
            }
        });
    }
    
    public interface ExecutionCallback {
        void onOutput(String output);
        void onError(String error);
        void onComplete();
    }
    
    public interface AutocompleteCallback {
        void onSuggestions(String[] suggestions);
    }
}