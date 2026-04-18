package com.pythonide.file;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.SimpleStorageHelper;
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.MediaFile;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    
    private static final String TAG = "FileManager";
    private static FileManager instance;
    private Context context;
    private SimpleStorageHelper storageHelper;
    
    private FileManager() {}
    
    public static synchronized FileManager getInstance() {
        if (instance == null) {
            instance = new FileManager();
        }
        return instance;
    }
    
    public void init(Context context, SimpleStorageHelper storageHelper) {
        this.context = context.getApplicationContext();
        this.storageHelper = storageHelper;
    }
    
    public String readFile(String path) throws IOException {
        File file = new File(path);
        if (file.exists() && file.canRead()) {
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            }
        }
        return "";
    }
    
    public String readFile(Uri uri) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
    
    public boolean writeFile(String path, String content) {
        File file = new File(path);
        try {
            // Create parent directories if needed
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write file: " + path, e);
            return false;
        }
    }
    
    public boolean writeFile(Uri uri, String content) throws IOException {
        try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
                return true;
            }
        }
        return false;
    }
    
    public List<FileItem> listFiles(String directoryPath) {
        List<FileItem> items = new ArrayList<>();
        File directory = new File(directoryPath);
        
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    FileItem item = new FileItem();
                    item.name = file.getName();
                    item.path = file.getAbsolutePath();
                    item.isDirectory = file.isDirectory();
                    item.size = file.length();
                    item.lastModified = file.lastModified();
                    
                    if (!item.isDirectory) {
                        String name = item.name.toLowerCase();
                        if (name.endsWith(".py")) {
                            item.fileType = FileType.PYTHON;
                        } else if (name.endsWith(".txt")) {
                            item.fileType = FileType.TEXT;
                        } else if (name.endsWith(".json")) {
                            item.fileType = FileType.JSON;
                        } else {
                            item.fileType = FileType.OTHER;
                        }
                    }
                    
                    items.add(item);
                }
            }
        }
        
        // Sort directories first, then files alphabetically
        items.sort((a, b) -> {
            if (a.isDirectory && !b.isDirectory) return -1;
            if (!a.isDirectory && b.isDirectory) return 1;
            return a.name.compareToIgnoreCase(b.name);
        });
        
        return items;
    }
    
    public boolean createFile(String path, String name) {
        File file = new File(path, name);
        try {
            return file.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "Failed to create file", e);
            return false;
        }
    }
    
    public boolean createDirectory(String path, String name) {
        File file = new File(path, name);
        return file.mkdir();
    }
    
    public boolean deleteFile(String path) {
        File file = new File(path);
        return file.delete();
    }
    
    public boolean renameFile(String oldPath, String newName) {
        File oldFile = new File(oldPath);
        File newFile = new File(oldFile.getParent(), newName);
        return oldFile.renameTo(newFile);
    }
    
    public boolean fileExists(String path) {
        return new File(path).exists();
    }
    
    public String getParentPath(String path) {
        File file = new File(path);
        String parent = file.getParent();
        return parent != null ? parent : path;
    }
    
    public enum FileType {
        PYTHON, TEXT, JSON, OTHER, DIRECTORY
    }
    
    public static class FileItem {
        public String name;
        public String path;
        public boolean isDirectory;
        public long size;
        public long lastModified;
        public FileType fileType = FileType.OTHER;
    }
}