package com.pythonide.python;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class PythonInitializer {
    
    private static final String TAG = "PythonInitializer";
    private static PythonInitializer instance;
    private boolean initialized = false;
    private Python python;
    
    private PythonInitializer() {}
    
    public static synchronized PythonInitializer getInstance() {
        if (instance == null) {
            instance = new PythonInitializer();
        }
        return instance;
    }
    
    public void initialize(Context context, InitializationCallback callback) {
        if (initialized) {
            callback.onComplete(true);
            return;
        }
        
        new Thread(() -> {
            try {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(context));
                }
                python = Python.getInstance();
                initialized = true;
                
                // Pre-load standard modules
                python.getModule("sys");
                python.getModule("os");
                
                Log.i(TAG, "Python initialized successfully");
                callback.onComplete(true);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Python", e);
                callback.onComplete(false);
            }
        }).start();
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public Python getPython() {
        if (!initialized) {
            throw new IllegalStateException("Python not initialized");
        }
        return python;
    }
    
    public interface InitializationCallback {
        void onComplete(boolean success);
    }
}