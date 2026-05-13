package com.example.fonos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonos.R;
import com.example.fonos.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<Category> categoryList;
    private final OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvChipName.setText(category.getName());
        
        holder.itemView.setSelected(category.isSelected());
        if (category.isSelected()) {
            holder.tvChipName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white, null));
        } else {
            holder.tvChipName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary, null));
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION || currentPosition == selectedPosition) return;

            categoryList.get(selectedPosition).setSelected(false);
            categoryList.get(currentPosition).setSelected(true);
            
            int oldSelected = selectedPosition;
            selectedPosition = currentPosition;
            
            notifyItemChanged(oldSelected);
            notifyItemChanged(selectedPosition);
            
            listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvChipName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChipName = itemView.findViewById(R.id.tvChipName);
        }
    }
}
