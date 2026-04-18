package com.pythonide;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    
    private final MutableLiveData<Boolean> pythonReady = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentFile = new MutableLiveData<>();
    private final MutableLiveData<String> consoleOutput = new MutableLiveData<>("");
    
    public void setPythonReady(boolean ready) {
        pythonReady.setValue(ready);
    }
    
    public LiveData<Boolean> getPythonReady() {
        return pythonReady;
    }
    
    public void setCurrentFile(String filePath) {
        currentFile.setValue(filePath);
    }
    
    public LiveData<String> getCurrentFile() {
        return currentFile;
    }
    
    public void appendConsoleOutput(String output) {
        String current = consoleOutput.getValue();
        consoleOutput.setValue(current + output);
    }
    
    public void clearConsoleOutput() {
        consoleOutput.setValue("");
    }
    
    public LiveData<String> getConsoleOutput() {
        return consoleOutput;
    }
}