package com.example.todolist.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.todolist.core.StatsRules;
import com.example.todolist.core.TaskDateTime;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.Stats;
import com.example.todolist.utils.model.ToDoData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The only place that talks to Firebase about tasks and the counters derived from them.
 *
 * <p>Why it exists: the screens used to repeat the same read-modify-write code and each did it a bit
 * differently. Here every counter change is a transaction (two quick taps can no longer overwrite each
 * other), and a task can be finished or deleted exactly once, so tapping "done" twice cannot farm points.
 */
public final class TaskRepository {

    /** Result of an operation that has nothing to return. */
    public interface Callback {
        void onSuccess();

        void onError(@NonNull String message);

        /** The task was already gone (finished or deleted elsewhere). The list refreshes itself, so by default ignore. */
        default void onGone() {
        }
    }

    public interface AddCallback {
        void onSuccess(@NonNull String taskId);

        void onError(@NonNull String message);
    }

    public interface CompleteCallback {
        /** @param previousScore total score before this completion, @param newScore after it. */
        void onCompleted(int previousScore, int newScore, boolean onTime);

        void onError(@NonNull String message);

        default void onGone() {
        }
    }

    public interface TasksListener {
        /** Tasks sorted by deadline, tasks without a readable deadline last. */
        void onTasks(@NonNull List<ToDoData> tasks);

        void onError(@NonNull String message);
    }

    /** Handle to stop listening. Safe to call more than once. */
    public interface Subscription {
        void remove();
    }

    private interface StatsChange {
        void apply(Stats stats);
    }

    private interface StatsResult {
        void onDone(int previousScore, int newScore);

        void onError(String message);
    }

    /** Users whose counters were already repaired during this process. */
    private static final Set<String> RECONCILED = new HashSet<>();

    private final String uid;
    private final DatabaseReference tasksRef;
    private final DatabaseReference statsRef;

    private TaskRepository(String uid) {
        this.uid = uid;
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        this.tasksRef = root.child("Tasks").child(uid);
        this.statsRef = root.child("Stats").child(uid);
    }

    /** Repository of the signed-in user, or null when nobody is signed in. */
    @Nullable
    public static TaskRepository forCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? null : new TaskRepository(user.getUid());
    }

    /** Call on sign-out so the next account is checked again. */
    public static void onSignedOut() {
        synchronized (RECONCILED) {
            RECONCILED.clear();
        }
    }

    public String userId() {
        return uid;
    }

    // ---------------------------------------------------------------- reading

    public Subscription observeTasks(@NonNull TasksListener listener) {
        ValueEventListener valueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ToDoData> tasks = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ToDoData task = parseTask(child);
                    if (task != null) tasks.add(task);
                }
                Collections.sort(tasks, (a, b) ->
                        TaskDateTime.compareDeadlines(a.getDate(), a.getTime(), b.getDate(), b.getTime()));
                listener.onTasks(tasks);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        };
        tasksRef.addValueEventListener(valueListener);
        return new Subscription() {
            private boolean removed;

            @Override
            public void remove() {
                if (removed) return;
                removed = true;
                tasksRef.removeEventListener(valueListener);
            }
        };
    }

    /** One broken record must never hide the other tasks, so every field is read defensively. */
    @Nullable
    private static ToDoData parseTask(DataSnapshot snapshot) {
        String id = snapshot.getKey();
        if (id == null) return null;
        String title = snapshot.child("task").getValue(String.class);
        if (title == null) return null;
        Priority priority;
        try {
            priority = snapshot.child("priority").getValue(Priority.class);
        } catch (RuntimeException e) {
            priority = null;
        }
        if (priority == null) priority = Priority.NOT_URGENT_AND_NOT_IMPORTANT;
        return new ToDoData(
                id,
                title,
                snapshot.child("category").getValue(String.class),
                priority,
                snapshot.child("date").getValue(String.class),
                snapshot.child("time").getValue(String.class));
    }

    // ---------------------------------------------------------------- writing

    public void addTask(@NonNull String title, @Nullable String category, @NonNull Priority priority,
                        @NonNull String date, @NonNull String time, @NonNull AddCallback callback) {
        String taskId = tasksRef.push().getKey();
        if (taskId == null) {
            callback.onError("Could not create a task id");
            return;
        }
        ToDoData data = new ToDoData(taskId, title, category, priority, date, time);
        tasksRef.child(taskId).setValue(data).addOnCompleteListener(result -> {
            if (!result.isSuccessful()) {
                callback.onError(messageOf(result.getException()));
                return;
            }
            // The task is saved; the counter update is best-effort and is repaired by reconcileStatsOnce.
            changeStats(StatsRules::onTaskAdded, new StatsResult() {
                @Override
                public void onDone(int previousScore, int newScore) {
                    callback.onSuccess(taskId);
                }

                @Override
                public void onError(String message) {
                    callback.onSuccess(taskId);
                }
            });
        });
    }

    /** Overwrites an existing task; a task that was finished or deleted meanwhile is not brought back. */
    public void updateTask(@NonNull ToDoData task, @NonNull Callback callback) {
        if (task.getTaskId() == null) {
            callback.onError("Task has no id");
            return;
        }
        boolean[] existed = {false};
        tasksRef.child(task.getTaskId()).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                // Not aborting on null: before the first server answer the local value can be null even though
                // the task exists, and an abort would not be retried.
                existed[0] = data.getValue() != null;
                if (existed[0]) data.setValue(task);
                return Transaction.success(data);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (error != null) callback.onError(error.getMessage());
                else if (!existed[0]) callback.onGone();
                else callback.onSuccess();
            }
        });
    }

    /** Marks a task as done: removes it once, then awards points if it was finished on time. */
    public void completeTask(@NonNull ToDoData task, @NonNull CompleteCallback callback) {
        boolean onTime = TaskDateTime.isOnTime(task.getDate(), task.getTime(), System.currentTimeMillis());
        removeOnce(task.getTaskId(), new RemoveResult() {
            @Override
            public void onRemoved() {
                changeStats(stats -> StatsRules.onTaskCompleted(stats, onTime), new StatsResult() {
                    @Override
                    public void onDone(int previousScore, int newScore) {
                        callback.onCompleted(previousScore, newScore, onTime);
                    }

                    @Override
                    public void onError(String message) {
                        // The task is done even if the counter could not be written now.
                        callback.onCompleted(0, 0, onTime);
                    }
                });
            }

            @Override
            public void onGone() {
                callback.onGone();
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void deleteTask(@NonNull ToDoData task, @NonNull Callback callback) {
        removeOnce(task.getTaskId(), new RemoveResult() {
            @Override
            public void onRemoved() {
                changeStats(StatsRules::onTaskDeleted, new StatsResult() {
                    @Override
                    public void onDone(int previousScore, int newScore) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String message) {
                        callback.onSuccess();
                    }
                });
            }

            @Override
            public void onGone() {
                callback.onGone();
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Repairs the counters (older versions could leave negative or wrong numbers) once per user and
     * process. Safe to call from every screen that shows statistics.
     */
    public void reconcileStatsOnce() {
        synchronized (RECONCILED) {
            if (!RECONCILED.add(uid)) return;
        }
        tasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int pending = (int) snapshot.getChildrenCount();
                changeStats(stats -> StatsRules.reconcile(stats, pending), new StatsResult() {
                    @Override
                    public void onDone(int previousScore, int newScore) {
                    }

                    @Override
                    public void onError(String message) {
                        forgetReconciled();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                forgetReconciled();
            }
        });
    }

    private void forgetReconciled() {
        synchronized (RECONCILED) {
            RECONCILED.remove(uid);
        }
    }

    // ---------------------------------------------------------------- internals

    private interface RemoveResult {
        void onRemoved();

        void onGone();

        void onError(String message);
    }

    /** Removes the task and reports whether THIS call removed it; a second tap finds nothing and changes no counters. */
    private void removeOnce(@Nullable String taskId, RemoveResult result) {
        if (taskId == null) {
            result.onError("Task has no id");
            return;
        }
        boolean[] existed = {false};
        tasksRef.child(taskId).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                existed[0] = data.getValue() != null;
                if (existed[0]) data.setValue(null);
                return Transaction.success(data);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (error != null) result.onError(error.getMessage());
                else if (existed[0]) result.onRemoved();
                else result.onGone();
            }
        });
    }

    private void changeStats(StatsChange change, StatsResult result) {
        int[] scores = {0, 0};
        statsRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Stats stats;
                try {
                    stats = data.getValue(Stats.class);
                } catch (RuntimeException e) {
                    stats = null;
                }
                if (stats == null) stats = StatsRules.initial(uid);
                if (stats.getStatsId() == null) stats.setStatsId(uid);
                scores[0] = stats.getTotalScores();
                change.apply(stats);
                scores[1] = stats.getTotalScores();
                data.setValue(stats);
                return Transaction.success(data);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (error != null) result.onError(error.getMessage());
                else result.onDone(scores[0], scores[1]);
            }
        });
    }

    private static String messageOf(@Nullable Exception e) {
        return e == null || e.getMessage() == null ? "Unknown error" : e.getMessage();
    }
}
