package com.example.todolist.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.todolist.R;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.databinding.FragmentDayBinding;
import com.example.todolist.utils.TaskActions;
import com.example.todolist.utils.adapter.TaskAdapter;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.ToDoData;
import java.util.ArrayList;
import java.util.List;

/** All tasks of the user, optionally filtered by Eisenhower priority. */
public class DayFragment extends Fragment {

    @Nullable
    private FragmentDayBinding binding;
    @Nullable
    private TaskRepository.Subscription subscription;
    private TaskAdapter taskAdapter;

    /** Everything the database returned; the adapter shows the filtered view of it. */
    private final List<ToDoData> allTasks = new ArrayList<>();
    /** Null = "All". */
    @Nullable
    private Priority selectedPriority;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentDayBinding b = binding;
        TaskRepository repository = TaskRepository.forCurrentUser();
        if (b == null || repository == null) return; // signed out: the host screen navigates away

        taskAdapter = new TaskAdapter();
        TaskActions actions = new TaskActions(this, repository, () -> taskAdapter.setList(visibleTasks()));
        taskAdapter.setListener(actions);
        b.mainRecyclerView.setHasFixedSize(true);
        b.mainRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.mainRecyclerView.setAdapter(taskAdapter);

        b.addTaskBtn.setOnClickListener(v -> actions.showAddDialog(null));
        setupFilterButtons(b);
        updateFilterButtons();

        repository.reconcileStatsOnce();
        // One listener per view; it is removed in onDestroyView, so reopening the screen never stacks listeners.
        subscription = repository.observeTasks(new TaskRepository.TasksListener() {
            @Override
            public void onTasks(@NonNull List<ToDoData> tasks) {
                allTasks.clear();
                allTasks.addAll(tasks);
                if (taskAdapter != null) taskAdapter.setList(visibleTasks());
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.error_with_message, message),
                            Toast.LENGTH_LONG).show();
                }
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

    private void setupFilterButtons(FragmentDayBinding b) {
        b.btnAllSort.setOnClickListener(v -> selectPriority(null));
        b.btnUrgentImportant.setOnClickListener(v -> selectPriority(Priority.URGENT_AND_IMPORTANT));
        b.btnUrgentNotImportant.setOnClickListener(v -> selectPriority(Priority.URGENT_BUT_NOT_IMPORTANT));
        b.btnNotUrgentImportant.setOnClickListener(v -> selectPriority(Priority.NOT_URGENT_BUT_IMPORTANT));
        b.btnNotUrgentNotImportant.setOnClickListener(v -> selectPriority(Priority.NOT_URGENT_AND_NOT_IMPORTANT));
    }

    private void selectPriority(@Nullable Priority priority) {
        selectedPriority = priority;
        updateFilterButtons();
        if (taskAdapter != null) taskAdapter.setList(visibleTasks());
    }

    private void updateFilterButtons() {
        FragmentDayBinding b = binding;
        if (b == null) return;
        setButtonColor(b.btnAllSort, selectedPriority == null);
        setButtonColor(b.btnUrgentImportant, selectedPriority == Priority.URGENT_AND_IMPORTANT);
        setButtonColor(b.btnUrgentNotImportant, selectedPriority == Priority.URGENT_BUT_NOT_IMPORTANT);
        setButtonColor(b.btnNotUrgentImportant, selectedPriority == Priority.NOT_URGENT_BUT_IMPORTANT);
        setButtonColor(b.btnNotUrgentNotImportant, selectedPriority == Priority.NOT_URGENT_AND_NOT_IMPORTANT);
    }

    private List<ToDoData> visibleTasks() {
        if (selectedPriority == null) return new ArrayList<>(allTasks);
        List<ToDoData> filtered = new ArrayList<>();
        for (ToDoData task : allTasks) {
            // A task without a priority never matches a specific filter; it used to crash the whole list.
            if (selectedPriority == task.getPriority()) filtered.add(task);
        }
        return filtered;
    }

    private void setButtonColor(Button button, boolean isSelected) {
        int color = isSelected ? R.color.selected_button_color : R.color.unselected_button_color;
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), color));
    }
}
