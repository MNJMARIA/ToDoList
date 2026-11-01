package com.example.todolist.fragments;

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
import com.example.todolist.core.TaskCategory;
import com.example.todolist.core.TaskDateTime;
import com.example.todolist.databinding.FragmentToDoDialogBinding;
import com.example.todolist.utils.CategoryLabels;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.ToDoData;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

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
    private ArrayAdapter<String> categoryAdapter;
    /** What is saved for each spinner row; parallel to the adapter (labels are translated, these are not). */
    private final List<String> categoryValues = new ArrayList<>();
    private String initialDate;
    private String initialTime;
    /** When the dialog was opened: the default deadline is "now" at that moment, however long typing takes. */
    private long openedAtMillis = System.currentTimeMillis();

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

        // Spinner shows translated labels; what is saved is the language-independent key (see TaskCategory).
        categoryValues.clear();
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (TaskCategory category : TaskCategory.values()) {
            categoryAdapter.add(getString(CategoryLabels.labelRes(category)));
            categoryValues.add(category.key());
        }
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
            fillForEdit(b, args);
        } else {
            fillForNew(b, args);
        }
        initialDate = b.dateTextView.getText().toString();
        initialTime = b.timeTextView.getText().toString();
        openedAtMillis = System.currentTimeMillis();

        b.todoClose.setOnClickListener(v -> dismiss());
        b.todoNextBtn.setOnClickListener(v -> submit());

        View.OnClickListener pickDate = v -> showDatePicker();
        b.calendarImageView.setOnClickListener(pickDate);
        b.dateTextView.setOnClickListener(pickDate);

        View.OnClickListener pickTime = v -> showTimePicker();
        b.clockImageView.setOnClickListener(pickTime);
        b.timeTextView.setOnClickListener(pickTime);
    }

    private void fillForEdit(FragmentToDoDialogBinding b, Bundle args) {
        Priority priority = (Priority) args.getSerializable("priority");
        editing = new ToDoData(args.getString("taskId"), args.getString("task"), args.getString("category"),
                priority, args.getString("date"), args.getString("time"));

        b.todoEt.setText(editing.getTask());
        String category = editing.getCategory();
        if (category != null && !category.trim().isEmpty()) {
            TaskCategory known = TaskCategory.fromStored(category);
            int position;
            if (known != null) {
                position = known.ordinal(); // a key, or a label saved by an older version in any language
            } else {
                // Free text from an older version: keep it instead of silently replacing it.
                categoryAdapter.insert(category.trim(), 0);
                categoryValues.add(0, category.trim());
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
            // An untouched default ("now" at opening) must not expire while the title is being typed.
            long reference = dateTimeChanged ? System.currentTimeMillis() : openedAtMillis;
            TaskDateTime.DeadlineCheck check = TaskDateTime.checkDeadline(date, time, reference);
            if (check == TaskDateTime.DeadlineCheck.INVALID_FORMAT) {
                Toast.makeText(requireContext(), R.string.invalid_date_time, Toast.LENGTH_LONG).show();
                return;
            }
            if (check == TaskDateTime.DeadlineCheck.IN_PAST) {
                Toast.makeText(requireContext(), R.string.deadline_in_past, Toast.LENGTH_LONG).show();
                return;
            }
            // Store one spelling for new and changed deadlines; untouched legacy values stay as they were.
            String normalizedDate = TaskDateTime.normalizeDate(date);
            if (normalizedDate != null) date = normalizedDate;
            LocalTime parsedTime = TaskDateTime.parseTime(time);
            if (parsedTime != null) time = TaskDateTime.formatTime(parsedTime);
        }

        int categoryIndex = b.categorySpinner.getSelectedItemPosition();
        String categoryValue = categoryIndex >= 0 && categoryIndex < categoryValues.size()
                ? categoryValues.get(categoryIndex) : "";
        int priorityIndex = Math.max(0, Math.min(b.prioritySpinner.getSelectedItemPosition(), Priority.values().length - 1));

        Bundle result = new Bundle();
        result.putString(RESULT_TASK_ID, editing == null ? null : editing.getTaskId());
        result.putString(RESULT_TITLE, title);
        result.putString(RESULT_CATEGORY, categoryValue);
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
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(current.getHour())
                .setMinute(current.getMinute())
                .setTitleText(R.string.select_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            FragmentToDoDialogBinding live = binding;
            if (live != null) {
                live.timeTextView.setText(TaskDateTime.formatTime(LocalTime.of(picker.getHour(), picker.getMinute())));
            }
        });
        picker.show(getChildFragmentManager(), "time_picker");
    }

    private void showDatePicker() {
        FragmentToDoDialogBinding b = binding;
        if (b == null) return;
        LocalDate today = LocalDate.now();
        LocalDate current = TaskDateTime.parseDate(b.dateTextView.getText().toString());
        // The picker works in UTC midnights. A date that is already past (editing an old task) opens on today.
        if (current == null || current.isBefore(today)) current = today;
        long todayUtc = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long currentUtc = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(todayUtc)
                .setValidator(DateValidatorPointForward.from(todayUtc))
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(currentUtc)
                .setCalendarConstraints(constraints)
                .setTitleText(R.string.select_date)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            FragmentToDoDialogBinding live = binding;
            if (live == null || selection == null) return;
            LocalDate picked = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
            live.dateTextView.setText(TaskDateTime.formatDate(picked));
        });
        picker.show(getChildFragmentManager(), "date_picker");
    }

    private static String textOf(@Nullable CharSequence text) {
        return text == null ? "" : text.toString();
    }
}
