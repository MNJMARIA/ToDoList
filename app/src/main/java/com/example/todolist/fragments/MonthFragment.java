package com.example.todolist.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.todolist.R;
import com.example.todolist.core.TaskDateTime;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.databinding.FragmentMonthBinding;
import com.example.todolist.utils.TaskActions;
import com.example.todolist.utils.UiErrors;
import com.example.todolist.utils.adapter.TaskAdapter;
import com.example.todolist.utils.model.ToDoData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Calendar with the tasks of the selected day below it. */
public class MonthFragment extends Fragment {

    private static final String STATE_SELECTED_DATE = "selected_date";

    @Nullable
    private FragmentMonthBinding binding;
    @Nullable
    private TaskRepository.Subscription subscription;
    private TaskAdapter taskAdapter;

    private final List<ToDoData> allTasks = new ArrayList<>();
    /** Selected day in the canonical dd/MM/yyyy form. */
    private String selectedDate = TaskDateTime.formatDate(LocalDate.now());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString(STATE_SELECTED_DATE);
            if (TaskDateTime.parseDate(saved) != null) selectedDate = saved;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_DATE, selectedDate);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMonthBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentMonthBinding b = binding;
        TaskRepository repository = TaskRepository.forCurrentUser();
        if (b == null || repository == null) return;

        taskAdapter = new TaskAdapter();
        TaskActions actions = new TaskActions(this, repository, () -> taskAdapter.setList(tasksOfSelectedDay()));
        taskAdapter.setListener(actions);
        b.mainRecyclerView.setHasFixedSize(true);
        b.mainRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.mainRecyclerView.setAdapter(taskAdapter);

        LocalDate selected = TaskDateTime.parseDate(selectedDate);
        if (selected != null) {
            b.calendarView.setDate(selected.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli(), false, true);
        }
        b.calendarView.setOnDateChangeListener((calendar, year, month, dayOfMonth) -> {
            selectedDate = TaskDateTime.formatDate(LocalDate.of(year, month + 1, dayOfMonth));
            List<ToDoData> forDay = tasksOfSelectedDay();
            taskAdapter.setList(forDay);
            int message = forDay.isEmpty() ? R.string.there_are_no_tasks_on : R.string.task_on;
            Toast.makeText(requireContext(), getString(message) + selectedDate, Toast.LENGTH_SHORT).show();
        });

        // The new task starts on the day that is selected in the calendar.
        b.addTaskBtn.setOnClickListener(v -> actions.showAddDialog(selectedDate));

        repository.reconcileStatsOnce();
        subscription = repository.observeTasks(new TaskRepository.TasksListener() {
            @Override
            public void onTasks(@NonNull List<ToDoData> tasks) {
                allTasks.clear();
                allTasks.addAll(tasks);
                if (taskAdapter != null) taskAdapter.setList(tasksOfSelectedDay());
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) UiErrors.show(requireContext(), R.string.error_load_title, message);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (subscription != null) {
            subscription.remove();
            subscription = null;
        }
        taskAdapter = null;
        binding = null;
    }

    /** Matches by calendar day, so "5/3/2026" (old tasks) and "05/03/2026" (new tasks) are the same day. */
    private List<ToDoData> tasksOfSelectedDay() {
        List<ToDoData> result = new ArrayList<>();
        for (ToDoData task : allTasks) {
            if (TaskDateTime.sameDay(task.getDate(), selectedDate)) result.add(task);
        }
        return result;
    }
}
