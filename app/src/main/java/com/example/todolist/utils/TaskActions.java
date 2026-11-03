package com.example.todolist.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.todolist.R;
import com.example.todolist.data.ConnectionMonitor;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.fragments.ToDoDialogFragment;
import com.example.todolist.reminders.ReminderScheduler;
import com.example.todolist.utils.adapter.TaskAdapterInterface;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.Subtask;
import com.example.todolist.utils.model.ToDoData;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

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
        show(ToDoDialogFragment.newInstance(task));
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

        String notes = result.getString(ToDoDialogFragment.RESULT_NOTES, "");
        String repeat = result.getString(ToDoDialogFragment.RESULT_REPEAT, "");
        String[] stepTitles = result.getStringArray(ToDoDialogFragment.RESULT_SUBTASK_TITLES);
        boolean[] stepDone = result.getBooleanArray(ToDoDialogFragment.RESULT_SUBTASK_DONE);
        List<Subtask> subtasks = new ArrayList<>();
        if (stepTitles != null) {
            for (int i = 0; i < stepTitles.length; i++) {
                subtasks.add(new Subtask(stepTitles[i], stepDone != null && i < stepDone.length && stepDone[i]));
            }
        }
        // Empty values are not stored at all, so a plain task stays as small as before.
        ToDoData task = new ToDoData(taskId, title, category, priority, date, time,
                notes.isEmpty() ? null : notes,
                subtasks.isEmpty() ? null : subtasks,
                repeat.isEmpty() || "none".equals(repeat) ? null : repeat);

        if (taskId == null) {
            saveNew(task);
        } else {
            saveEdited(task);
        }
    }

    private void saveNew(ToDoData draft) {
        String title = draft.getTask();
        String date = draft.getDate();
        String time = draft.getTime();
        repository.addTask(draft, new TaskRepository.AddCallback() {
            @Override
            public void onSuccess(@NonNull String taskId) {
                Context context = host.getContext();
                if (context != null) {
                    ReminderScheduler.schedule(context, taskId, title, date, time);
                    // Be honest when the server is not reachable: the task is kept on the phone, not yet saved online.
                    if (ConnectionMonitor.isConnected()) {
                        toast(R.string.task_added_successfully);
                    } else {
                        Toast.makeText(context, R.string.task_saved_offline, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onError(@NonNull String message) {
                showSaveError(message);
            }

            @Override
            public void onRejected(@NonNull String taskId, @NonNull String message) {
                // The server refused a task that was already listed: it disappears again, so also drop its alarm.
                Context context = host.getContext();
                if (context != null) ReminderScheduler.cancel(context, taskId);
                showSaveError(message);
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
                showSaveError(message);
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
                showSaveError(message);
            }
        });
    }

    @Override
    public void onDoneItemClicked(ToDoData task, int position) {
        repository.completeTask(task, new TaskRepository.CompleteCallback() {
            @Override
            public void onCompleted(@NonNull TaskRepository.Completion completion) {
                Context context = host.getContext();
                if (context == null) return;
                ReminderScheduler.cancel(context, task.getTaskId());
                scheduleNext(context, completion);
                showSuccessDialog(context, completion.onTime, completion.previousScore, completion.newScore);
            }

            @Override
            public void onError(@NonNull String message) {
                showSaveError(message);
                redrawList.run();
            }
        });
    }

    // ------------------------------------------------------------ swipes (with "Undo")

    /** Swipe right: finish the task and offer to take it back for a few seconds. */
    public void swipeComplete(ToDoData task) {
        repository.completeTask(task, new TaskRepository.CompleteCallback() {
            @Override
            public void onCompleted(@NonNull TaskRepository.Completion completion) {
                Context context = host.getContext();
                if (context == null) return;
                ReminderScheduler.cancel(context, task.getTaskId());
                scheduleNext(context, completion);
                View anchor = host.getView();
                if (anchor == null) {
                    AchievementUtils.checkForAchievements(context, completion.previousScore, completion.newScore);
                    return;
                }
                Snackbar bar = Snackbar.make(anchor,
                        completion.onTime ? R.string.task_completed_successfully : R.string.task_completed_late,
                        Snackbar.LENGTH_LONG);
                bar.setAction(R.string.action_undo, v -> undoComplete(completion));
                bar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        // A new achievement is announced only once the user can no longer take the task back.
                        Context now = host.getContext();
                        if (event != DISMISS_EVENT_ACTION && now != null && host.isAdded()) {
                            AchievementUtils.checkForAchievements(now, completion.previousScore, completion.newScore);
                        }
                    }
                });
                bar.show();
            }

            @Override
            public void onError(@NonNull String message) {
                showSaveError(message);
                redrawList.run();
            }

            @Override
            public void onGone() {
                redrawList.run();
            }
        });
    }

    private void undoComplete(TaskRepository.Completion completion) {
        redrawList.run(); // show the task again at once, before the database round trip
        repository.undoCompletion(completion, new TaskRepository.Callback() {
            @Override
            public void onSuccess() {
                Context context = host.getContext();
                if (context == null) return;
                ReminderScheduler.schedule(context, completion.task.getTaskId(), completion.task.getTask(),
                        completion.task.getDate(), completion.task.getTime());
                if (completion.next != null) ReminderScheduler.cancel(context, completion.next.getTaskId());
            }

            @Override
            public void onError(@NonNull String message) {
                showSaveError(message);
            }
        });
    }

    /** Swipe left: delete the task and offer to bring it back for a few seconds. */
    public void swipeDelete(ToDoData task) {
        repository.deleteTask(task, new TaskRepository.Callback() {
            @Override
            public void onSuccess() {
                Context context = host.getContext();
                if (context == null) return;
                ReminderScheduler.cancel(context, task.getTaskId());
                View anchor = host.getView();
                if (anchor == null) return;
                Snackbar bar = Snackbar.make(anchor, context.getString(R.string.snack_deleted, task.getTask()),
                        Snackbar.LENGTH_LONG);
                bar.setAction(R.string.action_undo, v -> undoDelete(task));
                bar.show();
            }

            @Override
            public void onError(@NonNull String message) {
                showSaveError(message);
                redrawList.run();
            }

            @Override
            public void onGone() {
                redrawList.run();
            }
        });
    }

    private void undoDelete(ToDoData task) {
        redrawList.run();
        repository.restoreDeleted(task, new TaskRepository.AddCallback() {
            @Override
            public void onSuccess(@NonNull String taskId) {
                Context context = host.getContext();
                if (context != null) {
                    ReminderScheduler.schedule(context, taskId, task.getTask(), task.getDate(), task.getTime());
                }
            }

            @Override
            public void onError(@NonNull String message) {
                showSaveError(message);
            }
        });
    }

    /** A repeating task creates its next turn when it is finished: it needs its own reminder. */
    private void scheduleNext(Context context, TaskRepository.Completion completion) {
        ToDoData next = completion.next;
        if (next != null) {
            ReminderScheduler.schedule(context, next.getTaskId(), next.getTask(), next.getDate(), next.getTime());
        }
    }

    private void showSuccessDialog(Context context, boolean onTime, int previousScore, int newScore) {
        if (!host.isAdded()) return;
        View dialogView = LayoutInflater.from(context).inflate(R.layout.success_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
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

    private void showSaveError(String message) {
        UiErrors.show(host.getContext(), R.string.error_save_title, message);
    }
}
