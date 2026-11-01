package com.example.todolist.utils.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.utils.CategoryLabels;
import com.example.todolist.core.TaskDateTime;
import com.example.todolist.databinding.EachTodoItemBinding;
import com.example.todolist.utils.model.ToDoData;
import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<ToDoData> list = new ArrayList<>();
    private TaskAdapterInterface listener;

    public TaskAdapter() {
    }

    public void setListener(TaskAdapterInterface listener) {
        this.listener = listener;
    }

    /** Replaces the shown tasks with a copy of {@code newList} (the caller may keep changing its own list). */
    public void setList(List<ToDoData> newList) {
        list.clear();
        if (newList != null) list.addAll(newList);
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        final EachTodoItemBinding binding;
        final ColorStateList defaultCardColor;

        TaskViewHolder(EachTodoItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.defaultCardColor = binding.cardTask.getCardBackgroundColor();
        }
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        EachTodoItemBinding binding =
                EachTodoItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ToDoData task = list.get(position);
        Context context = holder.itemView.getContext();
        EachTodoItemBinding b = holder.binding;

        b.todoTask.setText(task.getTask());
        b.contentTextView.setText(CategoryLabels.display(context, task.getCategory()));

        String[] parts = TaskDateTime.splitForDisplay(task.getDate());
        b.dateTextView.setText(parts[0]);
        b.monthTextView.setText(localizedMonth(parts[1], context));
        b.yearTextView.setText(parts[2]);
        b.timeTextView.setText(task.getTime() == null ? "" : task.getTime());
        b.priorityTextView.setText(task.getPriority() == null ? "" : task.getPriority().getDisplayString(context));

        // Views are recycled: undo the "done" look of whichever task used this holder before.
        ImageViewCompat.setImageTintList(b.doneTask,
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray)));
        b.cardTask.setCardBackgroundColor(holder.defaultCardColor);
        b.doneTask.setEnabled(true);

        b.editTask.setOnClickListener(v -> {
            int current = holder.getBindingAdapterPosition();
            if (listener != null && current != RecyclerView.NO_POSITION) {
                listener.onEditItemClicked(list.get(current), current);
            }
        });

        b.deleteTask.setOnClickListener(v -> {
            int current = holder.getBindingAdapterPosition();
            if (listener != null && current != RecyclerView.NO_POSITION) {
                listener.onDeleteItemClicked(list.get(current), current);
            }
        });

        b.doneTask.setOnClickListener(v -> {
            int current = holder.getBindingAdapterPosition();
            if (listener == null || current == RecyclerView.NO_POSITION) return;
            // One tap is enough; the card disappears when the database confirms the task is gone.
            b.doneTask.setEnabled(false);
            ImageViewCompat.setImageTintList(b.doneTask,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green)));
            b.cardTask.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray));
            listener.onDoneItemClicked(list.get(current), current);
        });
    }

    private static String localizedMonth(String monthNumber, Context context) {
        if (monthNumber == null || monthNumber.isEmpty()) return "";
        String[] names = context.getResources().getStringArray(R.array.month_names);
        try {
            int index = Integer.parseInt(monthNumber);
            if (index >= 1 && index <= names.length) return names[index - 1];
        } catch (NumberFormatException ignored) {
            // falls through to the empty string
        }
        return "";
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
