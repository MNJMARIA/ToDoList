package com.example.todolist.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.todolist.R;
import com.example.todolist.core.TaskQuery;
import com.example.todolist.core.TaskSummary;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.databinding.FragmentDayBinding;
import com.example.todolist.utils.TaskActions;
import com.example.todolist.utils.ToDoAccessor;
import com.example.todolist.utils.UiErrors;
import com.example.todolist.utils.adapter.TaskAdapter;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.ToDoData;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * All tasks of the user with a summary on top, search, sorting and a filter by Eisenhower priority.
 */
public class DayFragment extends Fragment {

    private static final String STATE_SORT = "sort";

    @Nullable
    private FragmentDayBinding binding;
    @Nullable
    private TaskRepository.Subscription subscription;
    private TaskAdapter taskAdapter;

    /** Everything the database returned; the adapter shows the filtered view of it. */
    private final List<ToDoData> allTasks = new ArrayList<>();
    /** False until the first list arrives, so "no tasks yet" is not shown while the list is still loading. */
    private boolean dataLoaded;
    /** Null = "All". */
    @Nullable
    private Priority selectedPriority;
    /** What the user typed in the search field (the text itself is restored by the view). */
    private String searchQuery = "";
    private TaskQuery.Sort sort = TaskQuery.Sort.DEFAULT;

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
        setupFilterChips(b);
        updateFilterChips();
        setupSearch(b);

        repository.reconcileStatsOnce();
        // One listener per view; it is removed in onDestroyView, so reopening the screen never stacks listeners.
        subscription = repository.observeTasks(new TaskRepository.TasksListener() {
            @Override
            public void onTasks(@NonNull List<ToDoData> tasks) {
                allTasks.clear();
                allTasks.addAll(tasks);
                dataLoaded = true;
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

    // ------------------------------------------------------------ filter chips

    private void setupFilterChips(FragmentDayBinding b) {
        b.btnAllSort.setOnClickListener(v -> selectPriority(null));
        b.btnUrgentImportant.setOnClickListener(v -> selectPriority(Priority.URGENT_AND_IMPORTANT));
        b.btnUrgentNotImportant.setOnClickListener(v -> selectPriority(Priority.URGENT_BUT_NOT_IMPORTANT));
        b.btnNotUrgentImportant.setOnClickListener(v -> selectPriority(Priority.NOT_URGENT_BUT_IMPORTANT));
        b.btnNotUrgentNotImportant.setOnClickListener(v -> selectPriority(Priority.NOT_URGENT_AND_NOT_IMPORTANT));
    }

    private void selectPriority(@Nullable Priority priority) {
        selectedPriority = priority;
        updateFilterChips();
        refresh();
    }

    /** A chip toggles itself when tapped; this puts the visible state back in line with the selection. */
    private void updateFilterChips() {
        FragmentDayBinding b = binding;
        if (b == null) return;
        b.btnAllSort.setChecked(selectedPriority == null);
        b.btnUrgentImportant.setChecked(selectedPriority == Priority.URGENT_AND_IMPORTANT);
        b.btnUrgentNotImportant.setChecked(selectedPriority == Priority.URGENT_BUT_NOT_IMPORTANT);
        b.btnNotUrgentImportant.setChecked(selectedPriority == Priority.NOT_URGENT_BUT_IMPORTANT);
        b.btnNotUrgentNotImportant.setChecked(selectedPriority == Priority.NOT_URGENT_AND_NOT_IMPORTANT);
    }

    // ------------------------------------------------------------ search and sort

    private void setupSearch(FragmentDayBinding b) {
        // A restored field (rotation) already holds text: show the matching list, not everything.
        CharSequence restored = b.searchEt.getText();
        searchQuery = restored == null ? "" : restored.toString();
        b.clearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);

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
                FragmentDayBinding live = binding;
                if (live != null) live.clearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                refresh();
            }
        });
        b.searchEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
        b.clearSearch.setOnClickListener(v -> b.searchEt.setText(""));
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

    // ------------------------------------------------------------ drawing

    /** Re-applies priority filter, search and sort to what the database returned and redraws everything. */
    private void refresh() {
        FragmentDayBinding b = binding;
        if (b == null || taskAdapter == null || !isAdded() || !dataLoaded) return;
        Context context = requireContext();

        List<ToDoData> visible = visibleTasks(context);
        taskAdapter.setList(visible);

        TaskSummary summary = TaskSummary.of(allTasks, new ToDoAccessor(context),
                System.currentTimeMillis(), ZoneId.systemDefault());
        b.statTodayValue.setText(String.valueOf(summary.today));
        b.statOverdueValue.setText(String.valueOf(summary.overdue));
        b.statTotalValue.setText(String.valueOf(summary.total));
        b.statOverdueValue.setTextColor(ContextCompat.getColor(context,
                summary.overdue > 0 ? R.color.overdue_text : R.color.white));

        if (visible.isEmpty()) {
            boolean noTasksAtAll = allTasks.isEmpty();
            b.emptyTitle.setText(noTasksAtAll ? R.string.empty_title_none : R.string.search_no_results);
            b.emptySubtitle.setVisibility(noTasksAtAll ? View.VISIBLE : View.GONE);
            b.emptyView.setVisibility(View.VISIBLE);
        } else {
            b.emptyView.setVisibility(View.GONE);
        }
    }

    private List<ToDoData> visibleTasks(Context context) {
        List<ToDoData> byPriority = new ArrayList<>();
        for (ToDoData task : allTasks) {
            // A task without a priority never matches a specific filter; it used to crash the whole list.
            if (selectedPriority == null || selectedPriority == task.getPriority()) byPriority.add(task);
        }
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        return TaskQuery.apply(byPriority, searchQuery, sort, new ToDoAccessor(context), locale);
    }
}
