package com.example.todolist.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentToDoDialogBinding;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.ToDoData;
import com.google.android.material.textfield.TextInputEditText;
import org.jetbrains.annotations.Nullable;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Locale;

public class ToDoDialogFragment extends DialogFragment {
    private String[] categories;
    private FragmentToDoDialogBinding binding;
    private OnDialogNextBtnClickListener listener;
    private ToDoData toDoData;
    private int year, month, day, hour, minute;
    public void setListener(OnDialogNextBtnClickListener listener) {
        this.listener = listener;
    }
    public static final String TAG = "DialogFragment";

    public static ToDoDialogFragment newInstance(String taskId, String task, String category, Priority priority, String date, String time) {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentToDoDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get localized categories
        categories = new String[] {
                getString(R.string.category_work),
                getString(R.string.category_personal),
                getString(R.string.category_study),
                getString(R.string.category_health),
                getString(R.string.category_family),
                getString(R.string.category_hobby)
        };
        // Встановлення значень для категорій у спіннер
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(adapter);

        // Встановлення значень пріоритетів для спінера
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (Priority priority : Priority.values()) {
            priorityAdapter.add(priority.getDisplayString(requireContext()));
        }
        binding.prioritySpinner.setAdapter(priorityAdapter);

        if (getArguments() != null) {
            String taskId = getArguments().getString("taskId");
            String task = getArguments().getString("task");
            String category = getArguments().getString("category");
            Priority priority = (Priority) getArguments().getSerializable("priority");
            String date = getArguments().getString("date");
            String time = getArguments().getString("time");
            toDoData = new ToDoData(taskId, task, category, priority, date, time);
            binding.todoEt.setText(toDoData.getTask());
            binding.dateTextView.setText(toDoData.getDate());
            binding.timeTextView.setText(toDoData.getTime());
            // Set category spinner selection
            if (category != null) {
                int spinnerPosition = adapter.getPosition(category);
                binding.categorySpinner.setSelection(spinnerPosition);
            }
            // Set priority spinner selection
            if (toDoData != null && toDoData.getPriority() != null) {
                int spinnerPosition = priorityAdapter.getPosition(toDoData.getPriority().getDisplayString(requireContext()));
                binding.prioritySpinner.setSelection(spinnerPosition);
            }
        }else {
            // Set current date and time if there is no existing date and time
            setDefaultDateTime();
        }
        binding.todoClose.setOnClickListener(v -> dismiss());
        binding.todoNextBtn.setOnClickListener(v -> {
            String todoTask = binding.todoEt.getText().toString();
            String category = binding.categorySpinner.getSelectedItem().toString();
            Priority priority = Priority.fromString(requireContext(), binding.prioritySpinner.getSelectedItem().toString());
            String date = binding.dateTextView.getText().toString();
            String time = binding.timeTextView.getText().toString();
            if (!todoTask.isEmpty()) {
                if (toDoData == null) {
                    listener.saveTask(todoTask, category, priority, date, time, binding.todoEt);
                } else {
                    toDoData.setTask(todoTask);
                    toDoData.setCategory(category);
                    toDoData.setPriority(priority);
                    toDoData.setDate(date);
                    toDoData.setTime(time);
                    listener.updateTask(toDoData, binding.todoEt);
                }
            }else {
                Toast.makeText(getContext(), "Порожня назва завдання не допускається", Toast.LENGTH_SHORT).show();
            }
        });

        TextView dateTextView = view.findViewById(R.id.dateTextView);
        ImageView calendarImageView = view.findViewById(R.id.calendarImageView);
        TextView timeTextView = view.findViewById(R.id.timeTextView);
        ImageView clockImageView = view.findViewById(R.id.clockImageView);

        View.OnClickListener dateClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        };
        calendarImageView.setOnClickListener(dateClickListener);
        dateTextView.setOnClickListener(dateClickListener);

        // Set click listener for the clock image view
        View.OnClickListener timeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        };
        clockImageView.setOnClickListener(timeClickListener);
        timeTextView.setOnClickListener(timeClickListener);
    }

    private void showTimePickerDialog() {
        final Calendar currentTime = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                hour = hourOfDay;
                ToDoDialogFragment.this.minute = minute;

                String selectedTimeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                binding.timeTextView.setText(selectedTimeStr);
            }
        }, currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, monthOfYear, dayOfMonth);
                Calendar currentDate = Calendar.getInstance();

                if (selectedDate.before(currentDate)) {
                    Toast.makeText(getActivity(), getString(R.string.please_select_a_date_in_the_future), Toast.LENGTH_SHORT).show();
                } else {
                    ToDoDialogFragment.this.year = year;
                    month = monthOfYear;
                    day = dayOfMonth;

                    String selectedDateStr = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                    binding.dateTextView.setText(selectedDateStr);
                }
            }
        }, currentYear, currentMonth, currentDay);
        datePickerDialog.show();
    }
    private void setDefaultDateTime() {
        // Set current date if there is no existing date
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        String currentDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
        binding.dateTextView.setText(currentDate);

        // Set current time if there is no existing time
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);
        String currentTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        binding.timeTextView.setText(currentTime);
    }

    public interface OnDialogNextBtnClickListener {
        void saveTask(String todoTask, String category, Priority priority, String date, String time, TextInputEditText todoEdit);
        void updateTask(ToDoData toDoData, TextInputEditText todoEdit);
    }
}