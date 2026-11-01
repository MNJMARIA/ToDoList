package com.example.todolist.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.todolist.R;
import com.example.todolist.core.TaskQuery;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.databinding.FragmentDayBinding;
import com.example.todolist.utils.CategoryLabels;
import com.example.todolist.utils.TaskActions;
import com.example.todolist.utils.UiErrors;
import com.example.todolist.utils.adapter.TaskAdapter;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.ToDoData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    /** What the user typed in the search field (the text itself is restored by the view). */
    private String searchQuery = "";
    private TaskQuery.Sort sort = TaskQuery.Sort.DEFAULT;

    private static final String STATE_SORT = "sort";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            try {
                sort = TaskQuery.Sort.valueOf(savedInstanceState.getString(STATE_SORT, ""));
            } catch (IllegalArgumentException ignored) {
                sort = TaskQuery.Sort.DEFAULT;
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SORT, sort.name());
    }

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
        TaskActions actions = new TaskActions(this, repository, this::refresh);
        taskAdapter.setListener(actions);
        b.mainRecyclerView.setHasFixedSize(true);
        b.mainRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.mainRecyclerView.setAdapter(taskAdapter);

        b.addTaskBtn.setOnClickListener(v -> actions.showAddDialog(null));
        setupFilterButtons(b);
        updateFilterButtons();
        setupSearch(b);

        repository.reconcileStatsOnce();
        // One listener per view; it is removed in onDestroyView, so reopening the screen never stacks listeners.
        subscription = repository.observeTasks(new TaskRepository.TasksListener() {
            @Override
            public void onTasks(@NonNull List<ToDoData> tasks) {
                allTasks.clear();
                allTasks.addAll(tasks);
                refresh();
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
        refresh();
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

    private void setupSearch(FragmentDayBinding b) {
        // A restored field (rotation) already holds text: show the matching list, not everything.
        searchQuery = b.searchEt.getText() == null ? "" : b.searchEt.getText().toString();
        b.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s == null ? "" : s.toString();
                refresh();
            }
        });
        b.searchEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                v.clearFocus();
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) requireContext()
                                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
        b.sortButton.setOnClickListener(v -> showSortDialog());
    }

    private void showSortDialog() {
        final TaskQuery.Sort[] options = TaskQuery.Sort.values();
        String[] labels = new String[options.length];
        int checked = 0;
        for (int i = 0; i < options.length; i++) {
            labels[i] = getString(sortLabel(options[i]));
            if (options[i] == sort) checked = i;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sort_dialog_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    sort = options[which];
                    dialog.dismiss();
                    refresh();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private static int sortLabel(TaskQuery.Sort s) {
        switch (s) {
            case DEADLINE:
                return R.string.sort_deadline;
            case PRIORITY:
                return R.string.sort_priority;
            case TITLE:
                return R.string.sort_title_az;
            case DEFAULT:
            default:
                return R.string.sort_default;
        }
    }

    /** Re-applies priority filter, search and sort to what the database returned and redraws. */
    private void refresh() {
        FragmentDayBinding b = binding;
        if (b == null || taskAdapter == null || !isAdded()) return;
        List<ToDoData> visible = visibleTasks();
        taskAdapter.setList(visible);
        // "Nothing found" only when there is something to find; an empty account keeps its empty screen.
        b.emptyView.setVisibility(visible.isEmpty() && !allTasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private List<ToDoData> visibleTasks() {
        List<ToDoData> byPriority = new ArrayList<>();
        for (ToDoData task : allTasks) {
            // A task without a priority never matches a specific filter; it used to crash the whole list.
            if (selectedPriority == null || selectedPriority == task.getPriority()) byPriority.add(task);
        }
        final android.content.Context context = requireContext();
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        return TaskQuery.apply(byPriority, searchQuery, sort, new TaskQuery.Accessor<ToDoData>() {
            @Override
            public String title(ToDoData t) {
                return t.getTask();
            }

            @Override
            public String categoryLabel(ToDoData t) {
                return CategoryLabels.display(context, t.getCategory());
            }

            @Override
            public int priorityRank(ToDoData t) {
                return t.getPriority() == null ? -1 : t.getPriority().ordinal();
            }

            @Override
            public String date(ToDoData t) {
                return t.getDate();
            }

            @Override
            public String time(ToDoData t) {
                return t.getTime();
            }
        }, locale);
    }

    private void setButtonColor(Button button, boolean isSelected) {
        int color = isSelected ? R.color.selected_button_color : R.color.unselected_button_color;
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), color));
    }
}
