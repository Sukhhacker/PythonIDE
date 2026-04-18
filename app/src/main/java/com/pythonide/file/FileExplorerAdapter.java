package com.pythonide.file;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pythonide.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileExplorerAdapter extends RecyclerView.Adapter<FileExplorerAdapter.ViewHolder> {
    
    private List<FileManager.FileItem> files = new ArrayList<>();
    private FileManager.FileItem parentItem;
    private OnItemClickListener itemClickListener;
    private OnItemLongClickListener itemLongClickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    
    public void setFiles(List<FileManager.FileItem> files) {
        this.files = files;
        notifyDataSetChanged();
    }
    
    public void setParentItem(FileManager.FileItem parentItem) {
        this.parentItem = parentItem;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.itemLongClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileManager.FileItem item;
        if (parentItem != null && position == 0) {
            item = parentItem;
        } else {
            item = files.get(parentItem != null ? position - 1 : position);
        }
        
        holder.nameText.setText(item.name);
        
        if (item.isDirectory) {
            holder.iconImage.setImageResource(R.drawable.ic_folder);
            holder.infoText.setText(dateFormat.format(new Date(item.lastModified)));
        } else {
            // Set icon based on file type
            switch (item.fileType) {
                case PYTHON:
                    holder.iconImage.setImageResource(R.drawable.ic_python);
                    break;
                case TEXT:
                    holder.iconImage.setImageResource(R.drawable.ic_text);
                    break;
                case JSON:
                    holder.iconImage.setImageResource(R.drawable.ic_json);
                    break;
                default:
                    holder.iconImage.setImageResource(R.drawable.ic_file);
                    break;
            }
            
            // Format size
            String sizeStr;
            if (item.size < 1024) {
                sizeStr = item.size + " B";
            } else if (item.size < 1024 * 1024) {
                sizeStr = String.format(Locale.getDefault(), "%.1f KB", item.size / 1024.0);
            } else {
                sizeStr = String.format(Locale.getDefault(), "%.1f MB", item.size / (1024.0 * 1024.0));
            }
            holder.infoText.setText(sizeStr + " • " + dateFormat.format(new Date(item.lastModified)));
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(item);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (itemLongClickListener != null && !item.isDirectory) {
                itemLongClickListener.onItemLongClick(item);
                return true;
            }
            return false;
        });
    }
    
    @Override
    public int getItemCount() {
        int count = files.size();
        if (parentItem != null) {
            count++;
        }
        return count;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImage;
        TextView nameText;
        TextView infoText;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImage = itemView.findViewById(R.id.iconImage);
            nameText = itemView.findViewById(R.id.nameText);
            infoText = itemView.findViewById(R.id.infoText);
        }
    }
    
    public interface OnItemClickListener {
        void onItemClick(FileManager.FileItem item);
    }
    
    public interface OnItemLongClickListener {
        void onItemLongClick(FileManager.FileItem item);
    }
}