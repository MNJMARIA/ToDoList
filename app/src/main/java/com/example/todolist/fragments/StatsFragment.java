package com.example.todolist.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.todolist.R;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.databinding.FragmentStatsBinding;
import com.example.todolist.utils.model.Stats;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StatsFragment extends Fragment {
    @Nullable
    private FragmentStatsBinding binding;
    @Nullable
    private DatabaseReference statsDatabase;
    @Nullable
    private ValueEventListener statsListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentStatsBinding b = binding;
        String uid = FirebaseAuth.getInstance().getUid();
        if (b == null || uid == null) return;

        b.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        TaskRepository repository = TaskRepository.forCurrentUser();
        if (repository != null) repository.reconcileStatsOnce(); // heals counters damaged by older versions

        // A live listener: the numbers also update right after the repair above.
        statsDatabase = FirebaseDatabase.getInstance().getReference().child("Stats").child(uid);
        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FragmentStatsBinding live = binding;
                if (live == null) return;
                Stats stats = null;
                try {
                    stats = snapshot.getValue(Stats.class);
                } catch (RuntimeException ignored) {
                    // Unreadable record: show zeros rather than crash.
                }
                if (stats == null) stats = new Stats();
                live.totalTasksText.setText(String.valueOf(stats.getTotalTasks()));
                live.tasksToDoText.setText(String.valueOf(stats.getTasksToDo()));
                live.completedTasksText.setText(String.valueOf(stats.getCompletedTasks()));
                live.totalBonusesText.setText(String.valueOf(stats.getTotalScores()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.error_downloading_data, Toast.LENGTH_SHORT).show();
                }
            }
        };
        statsDatabase.addValueEventListener(statsListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (statsDatabase != null && statsListener != null) {
            statsDatabase.removeEventListener(statsListener);
        }
        statsDatabase = null;
        statsListener = null;
        binding = null;
    }
}
