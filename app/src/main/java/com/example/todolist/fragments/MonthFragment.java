package com.example.todolist.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Toast;
import com.example.todolist.DeadlineNotificationManager;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentMonthBinding;
import com.example.todolist.utils.AchievementUtils;
import com.example.todolist.utils.adapter.TaskAdapter;
import com.example.todolist.utils.adapter.TaskAdapterInterface;
import com.example.todolist.utils.model.Priority;
import com.example.todolist.utils.model.Stats;
import com.example.todolist.utils.model.ToDoData;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MonthFragment extends Fragment implements ToDoDialogFragment.OnDialogNextBtnClickListener, TaskAdapterInterface {
    private static final String TAG = "MonthFragment";
    private FragmentMonthBinding binding;
    private DatabaseReference database;
    private DatabaseReference statsDatabase;
    private ToDoDialogFragment frag;
    private FirebaseAuth auth;
    private String authId;
    private TaskAdapter taskAdapter;
    private List<ToDoData> toDoItemList;
    private String selectedDate;
    private int totalScores;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public MonthFragment() {
        // Required empty public constructor
    }
    public static MonthFragment newInstance(String param1, String param2) {
        MonthFragment fragment = new MonthFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentMonthBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        getTaskFromFirebase();
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        // Отримання поточної дати
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Місяць починається з 0, тому додаємо 1
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        String currentDate = dayOfMonth + "/" + month + "/" + year;

        // Фільтрація списку завдань за поточною датою
        taskAdapter.setList(filterListByDate(currentDate));
        if (toDoItemList.isEmpty()) {
            // Якщо список пустий, показати тост про відсутність завдань на обрану дату
            Toast.makeText(requireContext(), getString(R.string.there_are_no_tasks_on) + currentDate, Toast.LENGTH_SHORT).show();
            taskAdapter.setList(filterListByDate(currentDate));
        } else {
            Toast.makeText(requireContext(), getString(R.string.task_on) + currentDate, Toast.LENGTH_SHORT).show();
            // Якщо список не пустий, показати завдання на обрану дату
            taskAdapter.setList(filterListByDate(currentDate));
        }

        binding.addTaskBtn.setOnClickListener(v -> {
            if (frag != null)
                getChildFragmentManager().beginTransaction().remove(frag).commit();
            frag = new ToDoDialogFragment();
            frag.setListener(this);
            frag.show(getChildFragmentManager(), ToDoDialogFragment.TAG);
        });

        // Встановлення слухача подій для CalendarView
        binding.calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                // Фільтрація списку завдань за датою
                taskAdapter.setList(filterListByDate(selectedDate));
                String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;

                // Оновлення списку завдань перед фільтрацією за датою
                getTaskFromFirebase();
                toDoItemList = filterListByDate(selectedDate);
                if (toDoItemList.isEmpty()) {
                    // Якщо список пустий, показати тост про відсутність завдань на обрану дату
                    Toast.makeText(requireContext(), getString(R.string.there_are_no_tasks_on) + selectedDate, Toast.LENGTH_SHORT).show();
                    // Фільтрація списку завдань за датою
                    taskAdapter.setList(filterListByDate(selectedDate));
                } else {
                    Toast.makeText(requireContext(), getString(R.string.task_on) + selectedDate, Toast.LENGTH_SHORT).show();
                    // Фільтрація списку завдань за датою
                    taskAdapter.setList(filterListByDate(selectedDate));
                }
            }
        });;
    }

    private void showNewAchieveDialog() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        statsDatabase = FirebaseDatabase.getInstance().getReference().child("Stats").child(userId);

        statsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.child("totalScores").getValue() != null) {
                    int previousTotalScores = totalScores;
                    totalScores = snapshot.child("totalScores").getValue(Integer.class);
                    AchievementUtils.checkForAchievements(getContext(), totalScores);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors.
            }
        });
    }

    private void addTask(long selectedDateInMillis) {
        // Your code to create dialog or call add task function
        if (frag != null)
            getChildFragmentManager().beginTransaction().remove(frag).commit();
        frag = new ToDoDialogFragment();
        frag.setListener(this);
        Bundle bundle = new Bundle();
        bundle.putLong("selected_date", selectedDateInMillis);
        frag.setArguments(bundle);
        frag.show(getChildFragmentManager(), ToDoDialogFragment.TAG);

        // Calculate the deadline
        long deadline = selectedDateInMillis - (7 * 24 * 60 * 60 * 1000);
        long currentTime = System.currentTimeMillis();

        // Check if the deadline is within 7 days
        if (deadline <= currentTime) {
            // Schedule the notification
            DeadlineNotificationManager.scheduleNotification(requireContext(), deadline);
        }
    }

    private void getTaskFromFirebase() {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                toDoItemList.clear();
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    String taskId = taskSnapshot.getKey();
                    String task = taskSnapshot.child("task").getValue(String.class);
                    String category = taskSnapshot.child("category").getValue(String.class);
                    Priority priority = taskSnapshot.child("priority").getValue(Priority.class);
                    String date = taskSnapshot.child("date").getValue(String.class);
                    String time = taskSnapshot.child("time").getValue(String.class);
                    ToDoData todoTask = new ToDoData(taskId, task, category, priority, date, time);
                    if (todoTask != null) {
                        toDoItemList.add(todoTask);
                    }
                }
                Log.d(TAG, "onDataChange: " + toDoItemList);
                taskAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void init() {
        auth = FirebaseAuth.getInstance();
        authId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        database = FirebaseDatabase.getInstance().getReference().child("Tasks").child(authId);
        statsDatabase = FirebaseDatabase.getInstance().getReference().child("Stats").child(authId);
        binding.mainRecyclerView.setHasFixedSize(true);
        binding.mainRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        toDoItemList = new ArrayList<>();
        taskAdapter = new TaskAdapter(toDoItemList);
        taskAdapter.setListener(this);
        binding.mainRecyclerView.setAdapter(taskAdapter);

        // Ініціалізація статистики
        initializeStats();
    }

    private void initializeStats() {
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalTasks = snapshot.getChildrenCount();
                statsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot statsSnapshot) {
                        if (!statsSnapshot.exists()) {
                            Stats stats = new Stats(authId, (int) totalTasks, 0, (int) totalTasks, 0);
                            statsDatabase.setValue(stats);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void saveTask(String todoTask, String category, Priority priority, String date, String time, TextInputEditText todoEdit) {
        String taskId = database.push().getKey();
        ToDoData data = new ToDoData(taskId, todoTask, category, priority, date, time);
        database.child(taskId).setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), R.string.task_added_successfully, Toast.LENGTH_SHORT).show();
                todoEdit.setText(null);
                // Оновіть дизайн, щоб відображати нові дані (назва завдання, категорія, пріоритет, дата, час)
                taskAdapter.notifyDataSetChanged();
                // Update user stats
                updateStatsOnTaskAdded();
                // Отримайте дедлайн завдання в мілісекундах
                long deadlineMillis = convertDateAndTimeToMillis(date, time);
                // Викличте scheduleNotification з дедлайном завдання
                DeadlineNotificationManager.scheduleNotification(getContext(), deadlineMillis);

            } else {
                Toast.makeText(getContext(), task.getException().toString(), Toast.LENGTH_SHORT).show();
            }
            frag.dismiss();
        });
    }

    private long convertDateAndTimeToMillis(String date, String time) {
        try {
            // Формат дати та часу
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            // Об'єднуюємо дату та час в одну рядок
            String dateTimeStr = date + " " + time;
            // Парсимо рядок у об'єкт дати
            Date dateTime = dateFormat.parse(dateTimeStr);
            // Повертаємо дату у мілісекундах
            if (dateTime != null) {
                return dateTime.getTime();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0; // Повернемо 0 у випадку невдачі
    }

    private void updateStatsOnTaskAdded() {
        statsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Stats stats = snapshot.getValue(Stats.class);
                if (stats != null) {
                    stats.setTotalTasks(stats.getTotalTasks() + 1);
                    stats.setTasksToDo(stats.getTasksToDo() + 1);
                    statsDatabase.setValue(stats);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void updateTask(ToDoData toDoData, TextInputEditText todoEdit) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(toDoData.getTaskId(), toDoData);
        database.updateChildren(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), R.string.updated_successfully, Toast.LENGTH_SHORT).show();
                // Оновіть дизайн, щоб відображати нові дані (назва завдання, категорія, пріоритет, дата, час)
                taskAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getContext(), task.getException().toString(), Toast.LENGTH_SHORT).show();
            }
            frag.dismiss();
        });
    }

    @Override
    public void onDeleteItemClicked(ToDoData toDoData, int position) {
        database.child(toDoData.getTaskId()).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), getString(R.string.deleted_successfully), Toast.LENGTH_SHORT).show();
                // Update user stats
                updateStatsOnTaskDeleted();
            } else {
                Toast.makeText(getContext(), task.getException().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatsOnTaskDeleted() {
        statsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Stats stats = snapshot.getValue(Stats.class);
                if (stats != null) {
                    stats.setTotalTasks(stats.getTotalTasks() - 1); // Якщо потрібно, також можна зменшити загальну кількість завдань
                    stats.setTasksToDo(stats.getTasksToDo() - 1);
                    statsDatabase.setValue(stats);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditItemClicked(ToDoData toDoData, int position) {
        if (frag != null)
            getChildFragmentManager().beginTransaction().remove(frag).commit();
        frag = ToDoDialogFragment.newInstance(toDoData.getTaskId(), toDoData.getTask(), toDoData.getCategory(), toDoData.getPriority(), toDoData.getDate(), toDoData.getTime());
        frag.setListener(this);
        frag.show(getChildFragmentManager(), ToDoDialogFragment.TAG);
    }

    @Override
    public void onDoneItemClicked(ToDoData toDoData, int position) {
        // Оновлення адаптера, щоб відобразити зміни
        taskAdapter.notifyItemChanged(position);

        // Створення AlertDialog для підтвердження виконання завдання
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.success_dialog, null);
        alertDialogBuilder.setView(dialogView);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        Button doneButton = dialogView.findViewById(R.id.successDone);
        doneButton.setOnClickListener(v -> {
            alertDialog.dismiss();
            // Після закриття success_dialog показати new_achieve_dialog
            showNewAchieveDialog();
        });

        // Видалення завдання з Firebase
        database.child(toDoData.getTaskId()).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Оновлення статистики
                updateStatsOnTaskCompleted(toDoData);
                Toast.makeText(getContext(), R.string.task_completed_successfully, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), task.getException().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatsOnTaskCompleted(ToDoData toDoData) {
        statsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Stats stats = snapshot.getValue(Stats.class);
                if (stats != null) {
                    stats.setCompletedTasks(stats.getCompletedTasks() + 1);
                    stats.setTotalScores(stats.getTotalScores() + 25);
                    statsDatabase.setValue(stats);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<ToDoData> filterListByDate(String date) {
        List<ToDoData> filteredList = new ArrayList<>();
        for (ToDoData task : toDoItemList) {
            if (task.getDate().equals(date)) {
                filteredList.add(task);
            }
        }
        return filteredList;
    }
}