package com.example.todolist.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.todolist.R;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.fragments.ToDoDialogFragment;
import com.example.todolist.reminders.ReminderScheduler;
import com.example.todolist.utils.adapter.TaskAdapterInterface;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.ToDoData;

/**
 * What happens when the user adds, edits, deletes or finishes a task. The Day and Month screens
 * differ only in how they list tasks, so both delegate here instead of keeping two diverging copies.
 */
public class TaskActions implements TaskAdapterInterface {

    private final Fragment host;
    private final TaskRepository repository;
    private final Runnable redrawList;

    /**
     * @param redrawList redraws the list from scratch; used to undo the optimistic "done" look of a card
     *                   when the database write failed
     */
    public TaskActions(@NonNull Fragment host, @NonNull TaskRepository repository, @NonNull Runnable redrawList) {
        this.host = host;
        this.repository = repository;
        this.redrawList = redrawList;
        // The dialog reports back here; registering by key survives rotation and needs no listener field.
        host.getChildFragmentManager().setFragmentResultListener(
                ToDoDialogFragment.RESULT_KEY, host.getViewLifecycleOwner(),
                (key, bundle) -> onDialogResult(bundle));
    }

    // ------------------------------------------------------------ opening dialogs

    /** @param presetDate day to pre-fill (d/M/yyyy) or null for today */
    public void showAddDialog(@Nullable String presetDate) {
        show(ToDoDialogFragment.newInstanceForDate(presetDate));
    }

    @Override
    public void onEditItemClicked(ToDoData task, int position) {
        show(ToDoDialogFragment.newInstance(task.getTaskId(), task.getTask(), task.getCategory(),
                task.getPriority(), task.getDate(), task.getTime()));
    }

    private void show(ToDoDialogFragment dialog) {
        FragmentManager fm = host.getChildFragmentManager();
        if (fm.isStateSaved() || fm.findFragmentByTag(ToDoDialogFragment.TAG) != null) return;
        dialog.show(fm, ToDoDialogFragment.TAG);
    }

    // ------------------------------------------------------------ results from the dialog

    private void onDialogResult(Bundle result) {
        String taskId = result.getString(ToDoDialogFragment.RESULT_TASK_ID);
        String title = result.getString(ToDoDialogFragment.RESULT_TITLE, "");
        String category = result.getString(ToDoDialogFragment.RESULT_CATEGORY, "");
        String date = result.getString(ToDoDialogFragment.RESULT_DATE, "");
        String time = result.getString(ToDoDialogFragment.RESULT_TIME, "");
        Priority priority;
        try {
            priority = Priority.valueOf(result.getString(ToDoDialogFragment.RESULT_PRIORITY, ""));
        } catch (IllegalArgumentException e) {
            priority = Priority.NOT_URGENT_AND_NOT_IMPORTANT;
        }

        if (taskId == null) {
            saveNew(title, category, priority, date, time);
        } else {
            saveEdited(new ToDoData(taskId, title, category, priority, date, time));
        }
    }

    private void saveNew(String title, String category, Priority priority, String date, String time) {
        repository.addTask(title, category, priority, date, time, new TaskRepository.AddCallback() {
            @Override
            public void onSuccess(@NonNull String taskId) {
                Context context = host.getContext();
                if (context != null) {
                    ReminderScheduler.schedule(context, taskId, title, date, time);
                    toast(R.string.task_added_successfully);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                toastError(message);
            }
        });
    }

    private void saveEdited(ToDoData task) {
        repository.updateTask(task, new TaskRepository.Callback() {
            @Override
            public void onSuccess() {
                Context context = host.getContext();
                if (context != null) {
                    // A changed deadline needs a new alarm; the old one is replaced because it has the same id.
                    ReminderScheduler.schedule(context, task.getTaskId(), task.getTask(), task.getDate(), task.getTime());
                    toast(R.string.updated_successfully);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                toastError(message);
            }
        });
    }

    // ------------------------------------------------------------ delete / complete

    @Override
    public void onDeleteItemClicked(ToDoData task, int position) {
        Context context = host.getContext();
        if (context == null) return;
        new AlertDialog.Builder(context)
                .setTitle(R.string.delete_task_title)
                .setMessage(context.getString(R.string.delete_task_message, task.getTask()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteNow(task))
                .show();
    }

    private void deleteNow(ToDoData task) {
        repository.deleteTask(task, new TaskRepository.Callback() {
            @Override
            public void onSuccess() {
                Context context = host.getContext();
                if (context != null) {
                    ReminderScheduler.cancel(context, task.getTaskId());
                    toast(R.string.deleted_successfully);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                toastError(message);
            }
        });
    }

    @Override
    public void onDoneItemClicked(ToDoData task, int position) {
        repository.completeTask(task, new TaskRepository.CompleteCallback() {
            @Override
            public void onCompleted(int previousScore, int newScore, boolean onTime) {
                Context context = host.getContext();
                if (context == null) return;
                ReminderScheduler.cancel(context, task.getTaskId());
                showSuccessDialog(context, onTime, previousScore, newScore);
            }

            @Override
            public void onError(@NonNull String message) {
                toastError(message);
                redrawList.run();
            }
        });
    }

    private void showSuccessDialog(Context context, boolean onTime, int previousScore, int newScore) {
        if (!host.isAdded()) return;
        View dialogView = LayoutInflater.from(context).inflate(R.layout.success_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        dialog.show();

        Button done = dialogView.findViewById(R.id.successDone);
        done.setOnClickListener(v -> {
            dialog.dismiss();
            AchievementUtils.checkForAchievements(context, previousScore, newScore);
        });

        // Late completions earn nothing: say so instead of silently showing the same message.
        toast(onTime ? R.string.task_completed_successfully : R.string.task_completed_late);
    }

    // ------------------------------------------------------------ helpers

    private void toast(int resId) {
        Context context = host.getContext();
        if (context != null) Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    private void toastError(String message) {
        Context context = host.getContext();
        if (context != null) {
            Toast.makeText(context, context.getString(R.string.error_with_message, message), Toast.LENGTH_LONG).show();
        }
    }
}
