package com.example.todolist.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.todolist.R;
import com.example.todolist.core.AchievementRules;
import com.example.todolist.core.ActivityStats;
import com.example.todolist.core.TaskDateTime;
import com.example.todolist.core.TaskSummary;
import com.example.todolist.core.Tally;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.databinding.FragmentStatsBinding;
import com.example.todolist.utils.AchievementUtils;
import com.example.todolist.utils.CategoryLabels;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.Stats;
import com.example.todolist.utils.model.ToDoData;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Statistics: level and points, streak and completion rate, columns for the last and the next seven days,
 * a ring by priority and bars by category. Three live sources feed it (counters, open tasks, finished per day),
 * and every change redraws the whole screen from the latest of each.
 */
public class StatsFragment extends Fragment {

    private static final int CATEGORY_ROWS = 5;

    @Nullable
    private FragmentStatsBinding binding;
    @Nullable
    private DatabaseReference statsDatabase;
    @Nullable
    private ValueEventListener statsListener;
    @Nullable
    private TaskRepository.Subscription taskSubscription;
    @Nullable
    private TaskRepository.Subscription activitySubscription;

    private Stats stats = new Stats();
    @Nullable
    private List<ToDoData> openTasks; // null until the first list arrives
    private Map<String, Integer> activity = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentStatsBinding b = binding;
        String uid = FirebaseAuth.getInstance().getUid();
        if (b == null || uid == null) return;

        b.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        TaskRepository repository = TaskRepository.forCurrentUser();
        if (repository == null) return;
        repository.reconcileStatsOnce(); // heals counters damaged by older versions
        render();

        // 1. counters and points
        statsDatabase = FirebaseDatabase.getInstance().getReference().child("Stats").child(uid);
        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Stats read = null;
                try {
                    read = snapshot.getValue(Stats.class);
                } catch (RuntimeException ignored) {
                    // Unreadable record: show zeros rather than crash.
                }
                stats = read == null ? new Stats() : read;
                render();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.error_downloading_data, Toast.LENGTH_SHORT).show();
                }
            }
        };
        statsDatabase.addValueEventListener(statsListener);

        // 2. open tasks (priorities, categories, the coming week)
        taskSubscription = repository.observeTasks(new TaskRepository.TasksListener() {
            @Override
            public void onTasks(@NonNull List<ToDoData> tasks) {
                openTasks = new ArrayList<>(tasks);
                render();
            }

            @Override
            public void onError(@NonNull String message) {
                // The counters above still work; the task charts simply stay empty.
            }
        });

        // 3. finished per day (the column chart and the streak)
        activitySubscription = repository.observeActivity(perDay -> {
            activity = perDay;
            render();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (statsDatabase != null && statsListener != null) statsDatabase.removeEventListener(statsListener);
        if (taskSubscription != null) taskSubscription.remove();
        if (activitySubscription != null) activitySubscription.remove();
        statsDatabase = null;
        statsListener = null;
        taskSubscription = null;
        activitySubscription = null;
        binding = null;
    }

    // ------------------------------------------------------------ drawing

    private void render() {
        FragmentStatsBinding b = binding;
        if (b == null || !isAdded()) return;
        Context context = requireContext();
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        LocalDate today = LocalDate.now();
        List<ToDoData> tasks = openTasks == null ? new ArrayList<ToDoData>() : openTasks;

        renderLevel(b, context);
        renderTiles(b, context, today, tasks);
        renderDoneChart(b, context, locale, today);
        renderPlannedChart(b, context, locale, today, tasks);
        renderPriorities(b, context, tasks);
        renderCategories(b, context, tasks);
    }

    private void renderLevel(FragmentStatsBinding b, Context context) {
        int score = stats.getTotalScores();
        b.scoreValue.setText(String.valueOf(score));
        b.rankImage.setImageResource(AchievementUtils.avatarResForScore(score));
        b.levelRing.setPercent(AchievementRules.progressToNextPercent(score));
        int next = AchievementRules.nextThreshold(score);
        b.nextRewardText.setText(next < 0
                ? context.getString(R.string.stats_all_rewards)
                : context.getString(R.string.stats_next_reward, next - score, next));
    }

    private void renderTiles(FragmentStatsBinding b, Context context, LocalDate today, List<ToDoData> tasks) {
        int streak = ActivityStats.currentStreak(activity, today);
        b.streakValue.setText(String.valueOf(streak));
        b.bestStreakText.setText(context.getString(R.string.stats_best_streak,
                Math.max(streak, ActivityStats.bestStreak(activity))));

        int completed = stats.getCompletedTasks();
        int open = openTasks == null ? stats.getTasksToDo() : tasks.size();
        b.completedValue.setText(String.valueOf(completed));
        b.openValue.setText(String.valueOf(open));
        b.rateValue.setText(context.getString(R.string.percent_value, ActivityStats.completionPercent(completed, open)));
    }

    private void renderDoneChart(FragmentStatsBinding b, Context context, Locale locale, LocalDate today) {
        int[] counts = ActivityStats.lastDays(activity, today, 7);
        float[] values = new float[counts.length];
        String[] labels = new String[counts.length];
        for (int i = 0; i < counts.length; i++) {
            values[i] = counts[i];
            labels[i] = shortDay(today.minusDays(counts.length - 1 - i).getDayOfWeek(), locale);
        }
        b.doneChart.setData(values, labels, counts.length - 1);

        int total = ActivityStats.sum(counts);
        int peak = ActivityStats.peakIndex(counts);
        if (total == 0) {
            b.weekSummary.setText(R.string.chart_empty_hint);
        } else {
            String best = today.minusDays(counts.length - 1 - peak).getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, locale);
            b.weekSummary.setText(context.getString(R.string.chart_week_total, total, best));
        }
        b.doneChart.setContentDescription(context.getString(R.string.chart_done_week) + ": " + total);
    }

    private void renderPlannedChart(FragmentStatsBinding b, Context context, Locale locale, LocalDate today,
                                    List<ToDoData> tasks) {
        float[] values = new float[7];
        String[] labels = new String[7];
        for (int i = 0; i < 7; i++) labels[i] = shortDay(today.plusDays(i).getDayOfWeek(), locale);
        int overdue = 0;
        long now = System.currentTimeMillis();
        ZoneId zone = ZoneId.systemDefault();
        for (ToDoData task : tasks) {
            if (TaskSummary.isOverdue(task.getDate(), task.getTime(), now, zone)) overdue++;
            LocalDate date = TaskDateTime.parseDate(task.getDate());
            if (date == null) continue;
            long offset = java.time.temporal.ChronoUnit.DAYS.between(today, date);
            if (offset >= 0 && offset < 7) values[(int) offset]++;
        }
        b.plannedChart.setData(values, labels, 0);
        b.plannedNote.setText(overdue > 0 ? context.getString(R.string.chart_overdue_note, overdue) : "");
        b.plannedNote.setVisibility(overdue > 0 ? View.VISIBLE : View.GONE);
    }

    private void renderPriorities(FragmentStatsBinding b, Context context, List<ToDoData> tasks) {
        Priority[] priorities = Priority.values();
        float[] counts = new float[priorities.length];
        for (ToDoData task : tasks) {
            Priority priority = task.getPriority() == null ? Priority.NOT_URGENT_AND_NOT_IMPORTANT : task.getPriority();
            counts[priority.ordinal()]++;
        }
        int[] colors = new int[priorities.length];
        for (int i = 0; i < priorities.length; i++) {
            colors[i] = ContextCompat.getColor(context, priorityColor(priorities[i]));
        }
        b.priorityDonut.setData(counts, colors);
        b.priorityDonut.setContentDescription(context.getString(R.string.chart_priorities));

        b.priorityLegend.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < priorities.length; i++) {
            View row = inflater.inflate(R.layout.item_legend_row, b.priorityLegend, false);
            ((ImageView) row.findViewById(R.id.legendDot))
                    .setImageTintList(android.content.res.ColorStateList.valueOf(colors[i]));
            ((TextView) row.findViewById(R.id.legendLabel)).setText(priorities[i].getDisplayString(context));
            ((TextView) row.findViewById(R.id.legendCount)).setText(String.valueOf((int) counts[i]));
            b.priorityLegend.addView(row);
        }
    }

    private static int priorityColor(Priority priority) {
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

    private void renderCategories(FragmentStatsBinding b, Context context, List<ToDoData> tasks) {
        b.categoryBars.removeAllViews();
        List<String> keys = new ArrayList<>();
        for (ToDoData task : tasks) {
            String label = CategoryLabels.display(context, task.getCategory()).trim();
            keys.add(label.isEmpty() ? context.getString(R.string.category_none) : label);
        }
        List<Tally.Entry> entries = Tally.count(keys);
        b.categoryEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        if (entries.isEmpty()) return;

        int max = entries.get(0).count;
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < entries.size() && i < CATEGORY_ROWS; i++) {
            Tally.Entry entry = entries.get(i);
            View row = inflater.inflate(R.layout.item_stat_bar, b.categoryBars, false);
            ((TextView) row.findViewById(R.id.barLabel)).setText(entry.key);
            ((TextView) row.findViewById(R.id.barCount)).setText(String.valueOf(entry.count));
            LinearProgressIndicator bar = row.findViewById(R.id.barIndicator);
            bar.setProgressCompat(Math.max(4, (int) ((100L * entry.count) / max)), false);
            b.categoryBars.addView(row);
        }
    }

    private static String shortDay(DayOfWeek day, Locale locale) {
        return day.getDisplayName(TextStyle.SHORT, locale);
    }
}
