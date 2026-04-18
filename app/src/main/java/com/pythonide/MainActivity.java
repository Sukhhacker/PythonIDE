package com.pythonide;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.pythonide.editor.EditorActivity;
import com.pythonide.file.FileExplorerFragment;
import com.pythonide.python.PipManager;
import com.pythonide.python.PythonInitializer;
import com.pythonide.python.PythonExecutor;
import com.pythonide.utils.Utils;

public class MainActivity extends AppCompatActivity {
    
    private static final String[] REQUIRED_PERMISSIONS;
    
    static {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{};
        }
    }
    
    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<Intent> manageStorageLauncher;
    private MainViewModel viewModel;
    private BottomNavigationView bottomNav;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        setupPermissionLaunchers();
        setupViews();
        
        // Initialize Python
        PythonInitializer.getInstance().initialize(this, success -> {
            if (success) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Python initialized successfully", Toast.LENGTH_SHORT).show();
                    viewModel.setPythonReady(true);
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to initialize Python", Toast.LENGTH_LONG).show();
                });
            }
        });
        
        // Check permissions
        if (!hasAllPermissions()) {
            requestPermissions();
        }
        
        // Load file explorer by default
        if (savedInstanceState == null) {
            loadFragment(new FileExplorerFragment());
        }
    }
    
    private void setupPermissionLaunchers() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (!allGranted) {
                        showPermissionDeniedDialog();
                    }
                });
        
        manageStorageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(this, "Storage access granted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Storage access denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    
    private void setupViews() {
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(this::onNavigationItemSelected);
    }
    
    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_files) {
            loadFragment(new FileExplorerFragment());
            return true;
        } else if (itemId == R.id.nav_pip) {
            showPipDialog();
            return true;
        } else if (itemId == R.id.nav_new) {
            createNewFile();
            return true;
        } else if (itemId == R.id.nav_console) {
            showConsoleFragment();
            return true;
        }
        return false;
    }
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
    
    private void showPipDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Install Python Package");
        
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter package name (e.g., numpy)");
        input.setPadding(32, 16, 32, 16);
        builder.setView(input);
        
        builder.setPositiveButton("Install", (dialog, which) -> {
            String packageName = input.getText().toString().trim();
            if (!packageName.isEmpty()) {
                installPackage(packageName);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void installPackage(String packageName) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Installing " + packageName + "...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        PipManager.getInstance().installPackage(this, packageName, new PipManager.InstallCallback() {
            @Override
            public void onSuccess(String message) {
                progressDialog.dismiss();
                runOnUiThread(() -> {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Success")
                            .setMessage(packageName + " installed successfully!")
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
            
            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                runOnUiThread(() -> {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Error")
                            .setMessage("Failed to install: " + error)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }
    
    private void createNewFile() {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("is_new_file", true);
        startActivity(intent);
    }
    
    private void showConsoleFragment() {
        ConsoleFragment consoleFragment = new ConsoleFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, consoleFragment)
                .addToBackStack(null)
                .commit();
    }
    
    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }
    
    private void requestPermissions() {
        if (REQUIRED_PERMISSIONS.length > 0) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS);
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showManageStorageDialog();
            }
        }
    }
    
    private void showManageStorageDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Storage Permission Required")
                .setMessage("This app needs access to all files to manage Python scripts.")
                .setPositiveButton("Grant", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    manageStorageLauncher.launch(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("Storage permissions are required to save and open files.")
                .setPositiveButton("Try Again", (dialog, which) -> requestPermissions())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            // Open settings
            return true;
        } else if (itemId == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About Python IDE")
                .setMessage("Python IDE for Android\nVersion 1.0\n\n" +
                        "Features:\n" +
                        "- Python execution with Chaquopy\n" +
                        "- Pip package manager\n" +
                        "- Syntax highlighting\n" +
                        "- Autocomplete\n" +
                        "- Error highlighting\n" +
                        "- File management")
                .setPositiveButton("OK", null)
                .show();
    }
}