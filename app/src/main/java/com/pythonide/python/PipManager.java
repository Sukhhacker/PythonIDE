package com.pythonide.python;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PipManager {
    
    private static final String TAG = "PipManager";
    private static PipManager instance;
    private final ExecutorService executorService;
    
    private PipManager() {
        executorService = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized PipManager getInstance() {
        if (instance == null) {
            instance = new PipManager();
        }
        return instance;
    }
    
    public void installPackage(Context context, String packageName, InstallCallback callback) {
        executorService.execute(() -> {
            try {
                Python python = PythonInitializer.getInstance().getPython();
                
                // Get pip module
                PyObject pip = python.getModule("pip");
                
                // Build install command
                List<String> args = new ArrayList<>();
                args.add("install");
                args.add(packageName);
                
                // Execute pip install
                PyObject result = pip.callAttr("main", args);
                
                int exitCode = result.toInt();
                if (exitCode == 0) {
                    Log.i(TAG, "Successfully installed: " + packageName);
                    callback.onSuccess("Package installed successfully");
                } else {
                    Log.e(TAG, "Failed to install package, exit code: " + exitCode);
                    callback.onError("Installation failed with exit code: " + exitCode);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Installation error", e);
                
                // Try alternative method using subprocess
                tryAlternativeInstall(packageName, callback);
            }
        });
    }
    
    private void tryAlternativeInstall(String packageName, InstallCallback callback) {
        try {
            Python python = PythonInitializer.getInstance().getPython();
            PyObject subprocess = python.getModule("subprocess");
            
            List<String> cmd = new ArrayList<>();
            cmd.add("pip");
            cmd.add("install");
            cmd.add(packageName);
            
            PyObject result = subprocess.callAttr("run", cmd, 
                    python.getBuiltins().callAttr("dict", "capture_output", true, "text", true));
            
            String stdout = result.get("stdout").toString();
            String stderr = result.get("stderr").toString();
            
            if (result.get("returncode").toInt() == 0) {
                callback.onSuccess(stdout);
            } else {
                callback.onError(stderr);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Alternative installation failed", e);
            callback.onError("Both installation methods failed: " + e.getMessage());
        }
    }
    
    public void listInstalledPackages(ListPackagesCallback callback) {
        executorService.execute(() -> {
            try {
                Python python = PythonInitializer.getInstance().getPython();
                
                String listCode = 
                    "import pkg_resources\n" +
                    "packages = [(d.project_name, d.version) for d in pkg_resources.working_set]\n" +
                    "result = packages\n";
                
                PyObject builtins = python.getBuiltins();
                builtins.get("exec").call(listCode);
                PyObject result = python.getModule("__main__").get("result");
                
                List<PackageInfo> packages = new ArrayList<>();
                for (PyObject item : result.asList()) {
                    String name = item.asList().get(0).toString();
                    String version = item.asList().get(1).toString();
                    packages.add(new PackageInfo(name, version));
                }
                
                callback.onSuccess(packages);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to list packages", e);
                callback.onError(e.getMessage());
            }
        });
    }
    
    public void uninstallPackage(String packageName, InstallCallback callback) {
        executorService.execute(() -> {
            try {
                Python python = PythonInitializer.getInstance().getPython();
                PyObject pip = python.getModule("pip");
                
                List<String> args = new ArrayList<>();
                args.add("uninstall");
                args.add("-y");
                args.add(packageName);
                
                PyObject result = pip.callAttr("main", args);
                
                if (result.toInt() == 0) {
                    callback.onSuccess("Package uninstalled successfully");
                } else {
                    callback.onError("Uninstall failed");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Uninstall error", e);
                callback.onError(e.getMessage());
            }
        });
    }
    
    public static class PackageInfo {
        public String name;
        public String version;
        
        public PackageInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }
    
    public interface InstallCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public interface ListPackagesCallback {
        void onSuccess(List<PackageInfo> packages);
        void onError(String error);
    }
}