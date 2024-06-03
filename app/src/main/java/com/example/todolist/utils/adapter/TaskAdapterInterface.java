package com.example.todolist.utils.adapter;

import com.example.todolist.utils.model.ToDoData;

public interface TaskAdapterInterface {
    void onDeleteItemClicked(ToDoData toDoData, int position);
    void onEditItemClicked(ToDoData toDoData, int position);
    void onDoneItemClicked(ToDoData toDoData, int position);
}
