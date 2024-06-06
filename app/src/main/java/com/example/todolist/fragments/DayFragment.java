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
import android.widget.Toast;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentDayBinding;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.example.todolist.DeadlineNotificationManager;

public class DayFragment extends Fragment implements ToDoDialogFragment.OnDialogNextBtnClickListener, TaskAdapterInterface {
    private static final String TAG = "DayFragment";
    private FragmentDayBinding binding;
    private DatabaseReference database;
    private DatabaseReference statsDatabase;
    private ToDoDialogFragment frag;
    private FirebaseAuth auth;
    private String authId;
    private TaskAdapter taskAdapter;
    private List<ToDoData> toDoItemList;
    private int totalScores;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public DayFragment() {
        // Required empty public constructor
    }
    public static DayFragment newInstance(String param1, String param2) {
        DayFragment fragment = new DayFragment();
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
        binding = FragmentDayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        getTaskFromFirebase();
        setupButtonListeners();
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
    private void setupButtonListeners() {
        // Початково встановлюємо колір кнопок
        setButtonColor(binding.btnAllSort, true); // "Всі" буде виділено по дефолту
        setButtonColor(binding.btnUrgentImportant, false); // Інші кнопки не виділені
        setButtonColor(binding.btnUrgentNotImportant, false); // Інші кнопки не виділені
        setButtonColor(binding.btnNotUrgentImportant, false); // Інші кнопки не виділені
        setButtonColor(binding.btnNotUrgentNotImportant, false); // Інші кнопки не виділені

        binding.addTaskBtn.setOnClickListener(v -> {
            if (frag != null)
                getChildFragmentManager().beginTransaction().remove(frag).commit();
            frag = new ToDoDialogFragment();
            frag.setListener(this);
            frag.show(getChildFragmentManager(), ToDoDialogFragment.TAG);
        });

        // Встановлення списку всіх завдань по дефолту
        taskAdapter.setList(toDoItemList);

        // Обробники кліку на кнопки категорій
        binding.btnAllSort.setOnClickListener(v -> {
            taskAdapter.setList(toDoItemList); // Показати всі завдання
            setButtonColor(binding.btnAllSort, true); // "Всі" буде виділено по дефолту
            setButtonColor(binding.btnUrgentImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnUrgentNotImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnNotUrgentImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnNotUrgentNotImportant, false); // Інші кнопки не виділені
        });

        binding.btnUrgentImportant.setOnClickListener(v -> {
            taskAdapter.setList(filterListByPriority(Priority.URGENT_AND_IMPORTANT)); // Показати важливі термінові завдання
            setButtonColor(binding.btnAllSort, false); // "Всі" буде виділено по дефолту
            setButtonColor(binding.btnUrgentImportant, true); // Інші кнопки не виділені
            setButtonColor(binding.btnUrgentNotImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnNotUrgentImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnNotUrgentNotImportant, false); // Інші кнопки не виділені
        });

        binding.btnUrgentNotImportant.setOnClickListener(v -> {
            taskAdapter.setList(filterListByPriority(Priority.URGENT_BUT_NOT_IMPORTANT)); // Показати важливі термінові завдання
            setButtonColor(binding.btnAllSort, false); // "Всі" буде виділено по дефолту
            setButtonColor(binding.btnUrgentImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnUrgentNotImportant, true); // Інші кнопки не виділені
            setButtonColor(binding.btnNotUrgentImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnNotUrgentNotImportant, false); // Інші кнопки не виділені
        });

        binding.btnNotUrgentImportant.setOnClickListener(v -> {
            taskAdapter.setList(filterListByPriority(Priority.NOT_URGENT_BUT_IMPORTANT)); // Показати важливі термінові завдання
            setButtonColor(binding.btnAllSort, false); // "Всі" буде виділено по дефолту
            setButtonColor(binding.btnUrgentImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnUrgentNotImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnNotUrgentImportant, true); // Інші кнопки не виділені
            setButtonColor(binding.btnNotUrgentNotImportant, false); // Інші кнопки не виділені
        });

        binding.btnNotUrgentNotImportant.setOnClickListener(v -> {
            taskAdapter.setList(filterListByPriority(Priority.NOT_URGENT_AND_NOT_IMPORTANT)); // Показати важливі термінові завдання
            setButtonColor(binding.btnAllSort, false); // "Всі" буде виділено по дефолту
            setButtonColor(binding.btnUrgentImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnUrgentNotImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnNotUrgentImportant, false); // Інші кнопки не виділені
            setButtonColor(binding.btnNotUrgentNotImportant, true); // Інші кнопки не виділені
        });
    }

    private void getTaskFromFirebase() {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                toDoItemList.clear();
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    //TODO change on ToDoData todoTask = taskSnapshot.getValue(ToDoData.class);
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
                Toast.makeText(getContext(), getString(R.string.task_added_successfully), Toast.LENGTH_SHORT).show();
                todoEdit.setText(null);
                // Оновити дизайн, щоб відображати нові дані (назва завдання, категорія, пріоритет, дата, час)
                taskAdapter.notifyDataSetChanged();
                // Оновлення списку завдань
                getTaskFromFirebase();
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

    @Override
    public void updateTask(ToDoData toDoData, TextInputEditText todoEdit) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(toDoData.getTaskId(), toDoData);
        database.updateChildren(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), getString(R.string.updated_successfully), Toast.LENGTH_SHORT).show();
                // Оновити дизайн, щоб відображати нові дані (назва завдання, категорія, пріоритет, дата, час)
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
            showNewAchieveDialog();
        });
        // Видалення завдання з Firebase
        database.child(toDoData.getTaskId()).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Оновлення статистики
                updateStatsOnTaskCompleted(toDoData);

                Toast.makeText(getContext(), getString(R.string.task_completed_successfully), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), task.getException().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Метод для оновлення загальної кількості балів
    private void updateScores(int score) {
        statsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Stats stats = snapshot.getValue(Stats.class);
                if (stats != null) {
                    stats.setTotalScores(stats.getTotalScores() + score);
                    statsDatabase.setValue(stats);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
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

    private void updateStatsOnTaskCompleted(ToDoData toDoData) {
        statsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Stats stats = snapshot.getValue(Stats.class);
                if (stats != null) {
                    stats.setCompletedTasks(stats.getCompletedTasks() + 1);
                    // Отримання дедлайну завдання у мілісекундах
                    long deadlineMillis = convertDateAndTimeToMillis(toDoData.getDate(), toDoData.getTime());
                    // Отримання поточного часу у мілісекундах
                    long currentTimeMillis = System.currentTimeMillis();
                    Log.d(TAG, "deadlineMillis: " + deadlineMillis);
                    Log.d(TAG, "currentTimeMillis: " + currentTimeMillis);

                    // Порівняння дедлайну з поточним часом
                    if (deadlineMillis > currentTimeMillis) {
                        Log.d(TAG, "In 25: " );
                        // Якщо завдання виконано до дедлайну, нарахувати 25 балів
                        stats.setTotalScores(stats.getTotalScores() + 25);
                    } else {
                        Log.d(TAG, "in 0: " );
                    }
                    statsDatabase.setValue(stats);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private long convertDateAndTimeToMillis(String date, String time) {
        try {
            // Формат дати та часу
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
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

    private List<ToDoData> filterListByPriority(Priority priority) {
        List<ToDoData> filteredList = new ArrayList<>();
        for (ToDoData task : toDoItemList) {
            if (task.getPriority().equals(priority)) {
                filteredList.add(task);
            }
        }
        return filteredList;
    }

    private void setButtonColor(Button button, boolean isSelected) {
        if (isSelected) {
            button.setBackgroundColor(getResources().getColor(R.color.selected_button_color));
        } else {
            button.setBackgroundColor(getResources().getColor(R.color.unselected_button_color));
        }
    }
}