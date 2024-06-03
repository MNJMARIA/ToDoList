package com.example.todolist.utils.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class UserViewHolder extends RecyclerView.ViewHolder{
    public TextView name;    public TextView merkBarang;
    public TextView hargaBarang;
    public View view;

    public UserViewHolder(@NonNull View itemView) {
        super(itemView);
    }
}
