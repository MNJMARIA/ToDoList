package com.example.todolist.utils.adapter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.databinding.EachTodoItemBinding;
import com.example.todolist.utils.model.ToDoData;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private static final String TAG = "TaskAdapter";
    private List<ToDoData> list;
    private TaskAdapterInterface listener;
    public TaskAdapter(List<ToDoData> list) {
        this.list = list;
    }
    public void setListener(TaskAdapterInterface listener) {
        this.listener = listener;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        EachTodoItemBinding binding;
        TaskViewHolder(EachTodoItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        EachTodoItemBinding binding = EachTodoItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ToDoData task = list.get(position);
        int currentPosition = holder.getAdapterPosition(); // Отримання поточної позиції

        holder.binding.todoTask.setText(task.getTask());
        holder.binding.contentTextView.setText(task.getCategory());

        String[] parts = task.getDate().split("/");
        String day = "";
        String month = "";
        String year = "";

        if (parts.length >= 2) {
            day = parts[0];
            month = getLocalizedMonth(parts[1], holder.itemView.getContext()); // Отримання локалізованої назви місяця
            year = parts[2];
        }
        holder.binding.dateTextView.setText(day);
        holder.binding.monthTextView.setText(month != null ? month.toString() : "");
        holder.binding.yearTextView.setText(year);
        holder.binding.timeTextView.setText(task.getTime());
        holder.binding.priorityTextView.setText(task.getPriority().getDisplayString(holder.itemView.getContext()));
        Log.d(TAG, "onBindViewHolder: " + task);

        holder.binding.editTask.setOnClickListener(v -> {
            if (listener != null)
                listener.onEditItemClicked(task, currentPosition);
        });

        holder.binding.deleteTask.setOnClickListener(v -> {
            if (listener != null)
                listener.onDeleteItemClicked(task, currentPosition);
        });

        // Додавання обробника кліку на зображенні завершеного завдання
        holder.binding.doneTask.setOnClickListener(v -> {
            if (listener != null){
                holder.binding.doneTask.setColorFilter(null); // Скидаємо фільтр
                holder.binding.doneTask.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
                holder.binding.cardTask.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.gray));

                listener.onDoneItemClicked(task, currentPosition);
                Handler handler = new Handler();

                // Постановка виконання коду на 1 секунду
                handler.postDelayed(() -> {
                    listener.onDeleteItemClicked(task, currentPosition);
                }, 1000); // 1000 мілісекунд = 1 секунда
            }
        });
    }

    private String getLocalizedMonth(String monthIndex, Context context) {
        int index = Integer.parseInt(monthIndex);
        String[] monthNames = context.getResources().getStringArray(R.array.month_names);
        if (index >= 1 && index <= monthNames.length) {
            return monthNames[index - 1];
        }
        return "";
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setList(List<ToDoData> newList) {
        list = newList;
        notifyDataSetChanged();
    }
}