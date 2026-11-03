package com.example.todolist.utils.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.utils.CategoryLabels;
import com.example.todolist.core.SubtaskProgress;
import com.example.todolist.core.TaskDateTime;
import com.example.todolist.core.TaskSummary;
import com.example.todolist.databinding.EachTodoItemBinding;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.ToDoData;
import java.time.ZoneId;
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

    /** The task shown at a position, or null when the position is gone (the list changed meanwhile). */
    public ToDoData taskAt(int position) {
        return position >= 0 && position < list.size() ? list.get(position) : null;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        final EachTodoItemBinding binding;
        final ColorStateList defaultCardColor;
        final ColorStateList defaultDateColor;
        final ColorStateList defaultMonthColor;

        TaskViewHolder(EachTodoItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.defaultCardColor = binding.cardTask.getCardBackgroundColor();
            this.defaultDateColor = binding.dateTextView.getTextColors();
            this.defaultMonthColor = binding.monthTextView.getTextColors();
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
        String category = CategoryLabels.display(context, task.getCategory());
        b.contentTextView.setText(category);
        b.contentTextView.setVisibility(category.trim().isEmpty() ? View.GONE : View.VISIBLE);

        b.repeatIcon.setVisibility(task.repeatMode().repeats() ? View.VISIBLE : View.GONE);
        b.notesIcon.setVisibility(task.hasNotes() ? View.VISIBLE : View.GONE);
        SubtaskProgress progress = task.progress();
        b.subtaskRow.setVisibility(progress.isEmpty() ? View.GONE : View.VISIBLE);
        if (!progress.isEmpty()) {
            b.subtaskBar.setProgressCompat(progress.percent(), false);
            b.subtaskCount.setText(context.getString(R.string.subtask_progress, progress.done, progress.total));
        }

        boolean overdue = TaskSummary.isOverdue(task.getDate(), task.getTime(), System.currentTimeMillis(),
                ZoneId.systemDefault());
        b.overdueBadge.setVisibility(overdue ? View.VISIBLE : View.GONE);
        b.priorityStrip.setBackgroundColor(ContextCompat.getColor(context, priorityColor(task.getPriority())));

        String[] parts = TaskDateTime.splitForDisplay(task.getDate());
        b.dateTextView.setText(parts[0]);
        b.monthTextView.setText(localizedMonth(parts[1], context));
        b.yearTextView.setText(parts[2]);
        // A late deadline is the one thing on the card that should not look calm.
        int dateColor = overdue ? R.color.overdue : 0;
        if (dateColor != 0) {
            b.dateTextView.setTextColor(ContextCompat.getColor(context, dateColor));
            b.monthTextView.setTextColor(ContextCompat.getColor(context, dateColor));
        } else {
            b.dateTextView.setTextColor(holder.defaultDateColor);
            b.monthTextView.setTextColor(holder.defaultMonthColor);
        }
        b.timeTextView.setText(task.getTime() == null ? "" : task.getTime());
        b.priorityTextView.setText(task.getPriority() == null ? "" : task.getPriority().getDisplayString(context));

        // Views are recycled: undo the "done" look of whichever task used this holder before.
        ImageViewCompat.setImageTintList(b.doneTask,
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray)));
        b.cardTask.setCardBackgroundColor(holder.defaultCardColor);
        b.doneTask.setEnabled(true);

        // The whole card opens the task (subtasks are ticked there); the pencil does the same.
        b.cardTask.setOnClickListener(v -> {
            int current = holder.getBindingAdapterPosition();
            if (listener != null && current != RecyclerView.NO_POSITION) {
                listener.onEditItemClicked(list.get(current), current);
            }
        });

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

    private static int priorityColor(Priority priority) {
        if (priority == null) return R.color.priority_none;
        switch (priority) {
            case URGENT_AND_IMPORTANT:
                return R.color.priority_urgent_important;
            case URGENT_BUT_NOT_IMPORTANT:
                return R.color.priority_urgent;
            case NOT_URGENT_BUT_IMPORTANT:
                return R.color.priority_important;
            case NOT_URGENT_AND_NOT_IMPORTANT:
            default:
                return R.color.priority_none;
        }
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
