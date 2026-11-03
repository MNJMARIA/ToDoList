package com.example.todolist.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.todolist.R;
import com.example.todolist.core.TaskDateTime;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.databinding.FragmentMonthBinding;
import com.example.todolist.ui.TaskSwipeCallback;
import com.example.todolist.utils.TaskActions;
import com.example.todolist.utils.UiErrors;
import com.example.todolist.utils.adapter.TaskAdapter;
import com.example.todolist.utils.model.ToDoData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Calendar with the tasks of the selected day below it. */
public class MonthFragment extends Fragment {

    private static final String STATE_SELECTED_DATE = "selected_date";

    @Nullable
    private FragmentMonthBinding binding;
    @Nullable
    private TaskRepository.Subscription subscription;
    private TaskAdapter taskAdapter;
    /** False until the first list arrives, so "nothing planned" is not shown while the list is still loading. */
    private boolean dataLoaded;

    private final List<ToDoData> allTasks = new ArrayList<>();
    /** Tasks swiped away a moment ago: hidden at once, before the database confirms. */
    private final Set<String> hiddenIds = new HashSet<>();
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
        TaskActions actions = new TaskActions(this, repository, () -> {
            hiddenIds.clear();
            showDay();
        });
        taskAdapter.setListener(actions);
        b.mainRecyclerView.setHasFixedSize(true);
        b.mainRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.mainRecyclerView.setAdapter(taskAdapter);

        new ItemTouchHelper(new TaskSwipeCallback(requireContext(), new TaskSwipeCallback.Listener() {
            @Override
            public void onSwipedRight(int position) {
                ToDoData task = taskAdapter == null ? null : taskAdapter.taskAt(position);
                if (task == null) return;
                hide(task);
                actions.swipeComplete(task);
            }

            @Override
            public void onSwipedLeft(int position) {
                ToDoData task = taskAdapter == null ? null : taskAdapter.taskAt(position);
                if (task == null) return;
                hide(task);
                actions.swipeDelete(task);
            }
        })).attachToRecyclerView(b.mainRecyclerView);

        LocalDate selected = TaskDateTime.parseDate(selectedDate);
        if (selected != null) {
            b.calendarView.setDate(selected.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli(), false, true);
        }
        b.calendarView.setOnDateChangeListener((calendar, year, month, dayOfMonth) -> {
            selectedDate = TaskDateTime.formatDate(LocalDate.of(year, month + 1, dayOfMonth));
            showDay();
        });
        b.selectedDayTitle.setText(getString(R.string.tasks_on_date, selectedDate));

        // The new task starts on the day that is selected in the calendar.
        b.addTaskBtn.setOnClickListener(v -> actions.showAddDialog(selectedDate));

        repository.reconcileStatsOnce();
        subscription = repository.observeTasks(new TaskRepository.TasksListener() {
            @Override
            public void onTasks(@NonNull List<ToDoData> tasks) {
                allTasks.clear();
                allTasks.addAll(tasks);
                Set<String> listed = new HashSet<>();
                for (ToDoData task : tasks) listed.add(task.getTaskId());
                hiddenIds.retainAll(listed);
                dataLoaded = true;
                showDay();
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

    private void hide(ToDoData task) {
        if (task.getTaskId() != null) hiddenIds.add(task.getTaskId());
        showDay();
    }

    /** Shows the tasks of the selected day under the calendar, or a friendly note when there are none. */
    private void showDay() {
        FragmentMonthBinding b = binding;
        if (b == null || taskAdapter == null) return;
        List<ToDoData> forDay = tasksOfSelectedDay();
        taskAdapter.setList(forDay);
        b.selectedDayTitle.setText(getString(R.string.tasks_on_date, selectedDate));
        b.emptyView.setVisibility(dataLoaded && forDay.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Matches by calendar day, so "5/3/2026" (old tasks) and "05/03/2026" (new tasks) are the same day. */
    private List<ToDoData> tasksOfSelectedDay() {
        List<ToDoData> result = new ArrayList<>();
        for (ToDoData task : allTasks) {
            if (hiddenIds.contains(task.getTaskId())) continue;
            if (TaskDateTime.sameDay(task.getDate(), selectedDate)) result.add(task);
        }
        return result;
    }
}
