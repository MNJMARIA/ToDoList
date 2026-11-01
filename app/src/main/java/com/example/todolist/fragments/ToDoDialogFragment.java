package com.example.todolist.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.todolist.R;
import com.example.todolist.core.InputValidator;
import com.example.todolist.core.TaskDateTime;
import com.example.todolist.databinding.FragmentToDoDialogBinding;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.ToDoData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Create / edit dialog. It validates the input itself, hands the finished task back through the
 * Fragment Result API (so the screen that opened it needs no listener reference, which also survives
 * rotation) and closes itself.
 */
public class ToDoDialogFragment extends DialogFragment {

    public static final String TAG = "DialogFragment";

    /** Key to register with {@code getChildFragmentManager().setFragmentResultListener(...)}. */
    public static final String RESULT_KEY = "todo_dialog_result";
    public static final String RESULT_TASK_ID = "taskId";
    public static final String RESULT_TITLE = "task";
    public static final String RESULT_CATEGORY = "category";
    public static final String RESULT_PRIORITY = "priority";
    public static final String RESULT_DATE = "date";
    public static final String RESULT_TIME = "time";

    private static final String ARG_PRESET_DATE = "preset_date";

    @Nullable
    private FragmentToDoDialogBinding binding;
    @Nullable
    private ToDoData editing;
    private String initialDate;
    private String initialTime;

    public static ToDoDialogFragment newInstance(String taskId, String task, String category,
                                                 Priority priority, String date, String time) {
        ToDoDialogFragment fragment = new ToDoDialogFragment();
        Bundle args = new Bundle();
        args.putString("taskId", taskId);
        args.putString("task", task);
        args.putString("category", category);
        args.putSerializable("priority", priority);
        args.putString("date", date);
        args.putString("time", time);
        fragment.setArguments(args);
        return fragment;
    }

    /** New task whose date is already chosen (the day picked in the month calendar). */
    public static ToDoDialogFragment newInstanceForDate(@Nullable String presetDate) {
        ToDoDialogFragment fragment = new ToDoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRESET_DATE, presetDate);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentToDoDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentToDoDialogBinding b = binding;
        if (b == null) return;

        String[] categories = {
                getString(R.string.category_work),
                getString(R.string.category_personal),
                getString(R.string.category_study),
                getString(R.string.category_health),
                getString(R.string.category_family),
                getString(R.string.category_hobby)
        };
        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.categorySpinner.setAdapter(categoryAdapter);

        // Spinner position == Priority ordinal, so there is no string-to-enum round trip that breaks on translation.
        ArrayAdapter<String> priorityAdapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (Priority priority : Priority.values()) {
            priorityAdapter.add(priority.getDisplayString(requireContext()));
        }
        b.prioritySpinner.setAdapter(priorityAdapter);

        Bundle args = getArguments();
        if (args != null && args.containsKey("taskId") && args.getString("taskId") != null) {
            fillForEdit(b, args, categoryAdapter);
        } else {
            fillForNew(b, args);
        }
        initialDate = b.dateTextView.getText().toString();
        initialTime = b.timeTextView.getText().toString();

        b.todoClose.setOnClickListener(v -> dismiss());
        b.todoNextBtn.setOnClickListener(v -> submit());

        View.OnClickListener pickDate = v -> showDatePicker();
        b.calendarImageView.setOnClickListener(pickDate);
        b.dateTextView.setOnClickListener(pickDate);

        View.OnClickListener pickTime = v -> showTimePicker();
        b.clockImageView.setOnClickListener(pickTime);
        b.timeTextView.setOnClickListener(pickTime);
    }

    private void fillForEdit(FragmentToDoDialogBinding b, Bundle args, ArrayAdapter<String> categoryAdapter) {
        Priority priority = (Priority) args.getSerializable("priority");
        editing = new ToDoData(args.getString("taskId"), args.getString("task"), args.getString("category"),
                priority, args.getString("date"), args.getString("time"));

        b.todoEt.setText(editing.getTask());
        String category = editing.getCategory();
        if (category != null && !category.isEmpty()) {
            int position = categoryAdapter.getPosition(category);
            if (position < 0) {
                // Saved in another language or by an older version: keep it instead of silently replacing it.
                categoryAdapter.insert(category, 0);
                position = 0;
            }
            b.categorySpinner.setSelection(position);
        }
        if (priority != null) {
            b.prioritySpinner.setSelection(priority.ordinal());
        }
        String date = editing.getDate() == null ? "" : editing.getDate();
        String time = editing.getTime() == null ? "" : editing.getTime();
        b.dateTextView.setText(date);
        b.timeTextView.setText(time);
    }

    private void fillForNew(FragmentToDoDialogBinding b, @Nullable Bundle args) {
        LocalDateTime now = LocalDateTime.now();
        String preset = args == null ? null : TaskDateTime.normalizeDate(args.getString(ARG_PRESET_DATE));
        b.dateTextView.setText(preset != null ? preset : TaskDateTime.formatDate(now.toLocalDate()));
        b.timeTextView.setText(TaskDateTime.formatTime(now.toLocalTime()));
    }

    private void submit() {
        FragmentToDoDialogBinding b = binding;
        if (b == null) return;

        String title = InputValidator.normalizeTitle(textOf(b.todoEt.getText()));
        if (title.isEmpty()) {
            b.todoEt.setError(getString(R.string.task_title_empty));
            return;
        }

        String date = b.dateTextView.getText().toString();
        String time = b.timeTextView.getText().toString();
        boolean isNew = editing == null;
        boolean dateTimeChanged = !date.equals(initialDate) || !time.equals(initialTime);
        if (isNew || dateTimeChanged) {
            TaskDateTime.DeadlineCheck check = TaskDateTime.checkDeadline(date, time, System.currentTimeMillis());
            if (check == TaskDateTime.DeadlineCheck.INVALID_FORMAT) {
                Toast.makeText(requireContext(), R.string.invalid_date_time, Toast.LENGTH_SHORT).show();
                return;
            }
            if (check == TaskDateTime.DeadlineCheck.IN_PAST) {
                Toast.makeText(requireContext(), R.string.deadline_in_past, Toast.LENGTH_SHORT).show();
                return;
            }
            // Store one spelling for new and changed deadlines; untouched legacy values stay as they were.
            String normalizedDate = TaskDateTime.normalizeDate(date);
            if (normalizedDate != null) date = normalizedDate;
            LocalTime parsedTime = TaskDateTime.parseTime(time);
            if (parsedTime != null) time = TaskDateTime.formatTime(parsedTime);
        }

        Object selectedCategory = b.categorySpinner.getSelectedItem();
        int priorityIndex = Math.max(0, Math.min(b.prioritySpinner.getSelectedItemPosition(), Priority.values().length - 1));

        Bundle result = new Bundle();
        result.putString(RESULT_TASK_ID, editing == null ? null : editing.getTaskId());
        result.putString(RESULT_TITLE, title);
        result.putString(RESULT_CATEGORY, selectedCategory == null ? "" : selectedCategory.toString());
        result.putString(RESULT_PRIORITY, Priority.values()[priorityIndex].name());
        result.putString(RESULT_DATE, date);
        result.putString(RESULT_TIME, time);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    private void showTimePicker() {
        FragmentToDoDialogBinding b = binding;
        if (b == null) return;
        LocalTime current = TaskDateTime.parseTime(b.timeTextView.getText().toString());
        if (current == null) current = LocalTime.now();
        new TimePickerDialog(requireContext(), (picker, hourOfDay, minute) -> {
            FragmentToDoDialogBinding live = binding;
            if (live != null) {
                live.timeTextView.setText(TaskDateTime.formatTime(LocalTime.of(hourOfDay, minute)));
            }
        }, current.getHour(), current.getMinute(), true).show();
    }

    private void showDatePicker() {
        FragmentToDoDialogBinding b = binding;
        if (b == null) return;
        LocalDate current = TaskDateTime.parseDate(b.dateTextView.getText().toString());
        if (current == null) current = LocalDate.now();
        new DatePickerDialog(requireContext(), (picker, year, monthOfYear, dayOfMonth) -> {
            LocalDate picked = LocalDate.of(year, monthOfYear + 1, dayOfMonth);
            if (TaskDateTime.isDateInPast(picked, LocalDate.now())) {
                Toast.makeText(requireContext(), R.string.please_select_a_date_in_the_future, Toast.LENGTH_SHORT).show();
                return;
            }
            FragmentToDoDialogBinding live = binding;
            if (live != null) {
                live.dateTextView.setText(TaskDateTime.formatDate(picked));
            }
        }, current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
    }

    private static String textOf(@Nullable CharSequence text) {
        return text == null ? "" : text.toString();
    }
}
