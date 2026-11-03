package com.example.todolist.fragments;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.todolist.R;
import com.example.todolist.core.InputValidator;
import com.example.todolist.core.Repeat;
import com.example.todolist.core.TaskCategory;
import com.example.todolist.core.TaskDateTime;
import com.example.todolist.databinding.FragmentToDoDialogBinding;
import com.example.todolist.utils.CategoryLabels;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.Subtask;
import com.example.todolist.utils.model.ToDoData;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
 * Create / edit sheet. It validates the input itself, hands the finished task back through the
 * Fragment Result API (so the screen that opened it needs no listener reference, which also survives
 * rotation) and closes itself.
 */
public class ToDoDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "DialogFragment";

    /** Key to register with {@code getChildFragmentManager().setFragmentResultListener(...)}. */
    public static final String RESULT_KEY = "todo_dialog_result";
    public static final String RESULT_TASK_ID = "taskId";
    public static final String RESULT_TITLE = "task";
    public static final String RESULT_CATEGORY = "category";
    public static final String RESULT_PRIORITY = "priority";
    public static final String RESULT_DATE = "date";
    public static final String RESULT_TIME = "time";
    public static final String RESULT_NOTES = "notes";
    public static final String RESULT_REPEAT = "repeat";
    public static final String RESULT_SUBTASK_TITLES = "subtaskTitles";
    public static final String RESULT_SUBTASK_DONE = "subtaskDone";

    private static final String ARG_PRESET_DATE = "preset_date";
    private static final int MAX_SUBTASKS = 20;

    @Nullable
    private FragmentToDoDialogBinding binding;
    @Nullable
    private ToDoData editing;
    /** Chip ids by position, parallel to {@code Priority.values()}, {@link #categoryValues} and {@code Repeat.values()}. */
    private final List<Integer> priorityChipIds = new ArrayList<>();
    private final List<Integer> categoryChipIds = new ArrayList<>();
    private final List<Integer> repeatChipIds = new ArrayList<>();
    /** What is saved for each category chip (labels are translated, these are not). */
    private final List<String> categoryValues = new ArrayList<>();
    private String initialDate;
    private String initialTime;
    /** When the dialog was opened: the default deadline is "now" at that moment, however long typing takes. */
    private long openedAtMillis = System.currentTimeMillis();

    /** Edit an existing task. */
    public static ToDoDialogFragment newInstance(@NonNull ToDoData task) {
        ToDoDialogFragment fragment = new ToDoDialogFragment();
        Bundle args = new Bundle();
        args.putString("taskId", task.getTaskId());
        args.putString("task", task.getTask());
        args.putString("category", task.getCategory());
        args.putSerializable("priority", task.getPriority());
        args.putString("date", task.getDate());
        args.putString("time", task.getTime());
        args.putString("notes", task.getNotes());
        args.putString("repeat", task.getRepeat());
        List<Subtask> subtasks = task.getSubtasks() == null ? new ArrayList<Subtask>() : task.getSubtasks();
        String[] titles = new String[subtasks.size()];
        boolean[] done = new boolean[subtasks.size()];
        for (int i = 0; i < subtasks.size(); i++) {
            titles[i] = subtasks.get(i).getTitle();
            done[i] = subtasks.get(i).isDone();
        }
        args.putStringArray("subtaskTitles", titles);
        args.putBooleanArray("subtaskDone", done);
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
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            // Open fully, so the keyboard and the long form never hide the Save button.
            BottomSheetBehavior<FrameLayout> behavior = ((BottomSheetDialog) dialog).getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
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

        buildPriorityChips(b);
        buildCategoryChips(b, null);
        buildRepeatChips(b);

        Bundle args = getArguments();
        if (args != null && args.getString("taskId") != null) {
            fillForEdit(b, args);
        } else {
            fillForNew(b, args);
        }
        initialDate = b.dateButton.getText().toString();
        initialTime = b.timeButton.getText().toString();
        openedAtMillis = System.currentTimeMillis();

        b.todoClose.setOnClickListener(v -> dismiss());
        b.todoNextBtn.setOnClickListener(v -> submit());
        b.dateButton.setOnClickListener(v -> showDatePicker());
        b.timeButton.setOnClickListener(v -> showTimePicker());

        b.addSubtaskBtn.setOnClickListener(v -> addSubtaskFromInput());
        b.subtaskEt.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_DONE || enter) {
                addSubtaskFromInput();
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------ chips

    private Chip newChip(ChipGroup group, String text) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_choice, group, false);
        chip.setId(View.generateViewId());
        chip.setText(text);
        group.addView(chip);
        return chip;
    }

    private void buildPriorityChips(FragmentToDoDialogBinding b) {
        priorityChipIds.clear();
        for (Priority priority : Priority.values()) {
            Chip chip = newChip(b.priorityGroup, priority.getDisplayString(requireContext()));
            // A coloured dot in front: the same colours as the strip on the task card.
            chip.setChipIconResource(R.drawable.ic_dot_12);
            chip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorOf(priority))));
            chip.setChipIconVisible(true);
            priorityChipIds.add(chip.getId());
        }
    }

    @ColorRes
    private static int colorOf(Priority priority) {
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

    /** @param legacyCategory free text saved by an older version, kept as an extra first chip; null for none */
    private void buildCategoryChips(FragmentToDoDialogBinding b, @Nullable String legacyCategory) {
        b.categoryGroup.removeAllViews();
        categoryChipIds.clear();
        categoryValues.clear();
        if (legacyCategory != null) {
            Chip chip = newChip(b.categoryGroup, legacyCategory);
            categoryChipIds.add(chip.getId());
            categoryValues.add(legacyCategory);
        }
        for (TaskCategory category : TaskCategory.values()) {
            Chip chip = newChip(b.categoryGroup, getString(CategoryLabels.labelRes(category)));
            categoryChipIds.add(chip.getId());
            categoryValues.add(category.key());
        }
    }

    private void buildRepeatChips(FragmentToDoDialogBinding b) {
        repeatChipIds.clear();
        for (Repeat repeat : Repeat.values()) {
            Chip chip = newChip(b.repeatGroup, getString(repeatLabel(repeat)));
            repeatChipIds.add(chip.getId());
        }
    }

    private static int repeatLabel(Repeat repeat) {
        switch (repeat) {
            case DAILY:
                return R.string.repeat_daily;
            case WEEKLY:
                return R.string.repeat_weekly;
            case MONTHLY:
                return R.string.repeat_monthly;
            case NONE:
            default:
                return R.string.repeat_none;
        }
    }

    // ------------------------------------------------------------ filling

    private void fillForEdit(FragmentToDoDialogBinding b, Bundle args) {
        Priority priority = (Priority) args.getSerializable("priority");
        editing = new ToDoData(args.getString("taskId"), args.getString("task"), args.getString("category"),
                priority, args.getString("date"), args.getString("time"));

        b.dialogTitle.setText(R.string.edit_task);
        b.todoEt.setText(editing.getTask());

        String category = editing.getCategory();
        int categoryIndex = 0;
        if (category != null && !category.trim().isEmpty()) {
            TaskCategory known = TaskCategory.fromStored(category);
            if (known != null) {
                categoryIndex = known.ordinal(); // a key, or a label saved by an older version in any language
            } else {
                // Free text from an older version: keep it instead of silently replacing it.
                buildCategoryChips(b, category.trim());
                categoryIndex = 0;
            }
        }
        b.categoryGroup.check(categoryChipIds.get(Math.min(categoryIndex, categoryChipIds.size() - 1)));
        b.priorityGroup.check(priorityChipIds.get(priority == null ? 0 : priority.ordinal()));
        b.repeatGroup.check(repeatChipIds.get(Repeat.fromStored(args.getString("repeat")).ordinal()));

        String date = editing.getDate() == null ? "" : editing.getDate();
        String time = editing.getTime() == null ? "" : editing.getTime();
        b.dateButton.setText(date);
        b.timeButton.setText(time);

        String notes = args.getString("notes");
        if (notes != null) b.notesEt.setText(notes);

        String[] titles = args.getStringArray("subtaskTitles");
        boolean[] done = args.getBooleanArray("subtaskDone");
        if (titles != null) {
            for (int i = 0; i < titles.length; i++) {
                addSubtaskRow(titles[i], done != null && i < done.length && done[i]);
            }
        }
    }

    private void fillForNew(FragmentToDoDialogBinding b, @Nullable Bundle args) {
        LocalDateTime now = LocalDateTime.now();
        String preset = args == null ? null : TaskDateTime.normalizeDate(args.getString(ARG_PRESET_DATE));
        b.dateButton.setText(preset != null ? preset : TaskDateTime.formatDate(now.toLocalDate()));
        b.timeButton.setText(TaskDateTime.formatTime(now.toLocalTime()));
        b.priorityGroup.check(priorityChipIds.get(0));
        b.categoryGroup.check(categoryChipIds.get(0));
        b.repeatGroup.check(repeatChipIds.get(0));
    }

    // ------------------------------------------------------------ subtasks

    private void addSubtaskFromInput() {
        FragmentToDoDialogBinding b = binding;
        if (b == null) return;
        String title = InputValidator.normalizeTitle(textOf(b.subtaskEt.getText()));
        if (title.isEmpty()) return;
        if (b.subtasksContainer.getChildCount() >= MAX_SUBTASKS) {
            Toast.makeText(requireContext(), R.string.subtask_limit, Toast.LENGTH_SHORT).show();
            return;
        }
        addSubtaskRow(title, false);
        b.subtaskEt.setText("");
    }

    private void addSubtaskRow(@Nullable String title, boolean done) {
        FragmentToDoDialogBinding b = binding;
        if (b == null || title == null || title.trim().isEmpty()) return;
        View row = getLayoutInflater().inflate(R.layout.item_subtask_edit, b.subtasksContainer, false);
        CheckBox check = row.findViewById(R.id.subtaskCheck);
        check.setText(title);
        check.setChecked(done);
        row.findViewById(R.id.subtaskRemove).setOnClickListener(v -> b.subtasksContainer.removeView(row));
        b.subtasksContainer.addView(row);
    }

    // ------------------------------------------------------------ saving

    private void submit() {
        FragmentToDoDialogBinding b = binding;
        if (b == null) return;

        // A step typed but not added with "+" is still wanted: add it instead of dropping it silently.
        addSubtaskFromInput();

        String title = InputValidator.normalizeTitle(textOf(b.todoEt.getText()));
        if (title.isEmpty()) {
            b.titleLayout.setError(getString(R.string.task_title_empty));
            return;
        }
        b.titleLayout.setError(null);

        String date = b.dateButton.getText().toString();
        String time = b.timeButton.getText().toString();
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

        int categoryIndex = categoryChipIds.indexOf(b.categoryGroup.getCheckedChipId());
        String categoryValue = categoryIndex >= 0 && categoryIndex < categoryValues.size()
                ? categoryValues.get(categoryIndex) : "";
        int priorityIndex = Math.max(0, priorityChipIds.indexOf(b.priorityGroup.getCheckedChipId()));
        int repeatIndex = Math.max(0, repeatChipIds.indexOf(b.repeatGroup.getCheckedChipId()));

        int steps = b.subtasksContainer.getChildCount();
        String[] subtaskTitles = new String[steps];
        boolean[] subtaskDone = new boolean[steps];
        for (int i = 0; i < steps; i++) {
            CheckBox check = b.subtasksContainer.getChildAt(i).findViewById(R.id.subtaskCheck);
            subtaskTitles[i] = String.valueOf(check.getText());
            subtaskDone[i] = check.isChecked();
        }

        Bundle result = new Bundle();
        result.putString(RESULT_TASK_ID, editing == null ? null : editing.getTaskId());
        result.putString(RESULT_TITLE, title);
        result.putString(RESULT_CATEGORY, categoryValue);
        result.putString(RESULT_PRIORITY, Priority.values()[priorityIndex].name());
        result.putString(RESULT_DATE, date);
        result.putString(RESULT_TIME, time);
        result.putString(RESULT_NOTES, textOf(b.notesEt.getText()).trim());
        result.putString(RESULT_REPEAT, Repeat.values()[repeatIndex].key());
        result.putStringArray(RESULT_SUBTASK_TITLES, subtaskTitles);
        result.putBooleanArray(RESULT_SUBTASK_DONE, subtaskDone);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    // ------------------------------------------------------------ pickers

    private void showTimePicker() {
        FragmentToDoDialogBinding b = binding;
        if (b == null) return;
        LocalTime current = TaskDateTime.parseTime(b.timeButton.getText().toString());
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
                live.timeButton.setText(TaskDateTime.formatTime(LocalTime.of(picker.getHour(), picker.getMinute())));
            }
        });
        picker.show(getChildFragmentManager(), "time_picker");
    }

    private void showDatePicker() {
        FragmentToDoDialogBinding b = binding;
        if (b == null) return;
        LocalDate today = LocalDate.now();
        LocalDate current = TaskDateTime.parseDate(b.dateButton.getText().toString());
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
            live.dateButton.setText(TaskDateTime.formatDate(picked));
        });
        picker.show(getChildFragmentManager(), "date_picker");
    }

    private static String textOf(@Nullable CharSequence text) {
        return text == null ? "" : text.toString();
    }
}
