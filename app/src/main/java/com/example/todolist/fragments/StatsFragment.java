package com.example.todolist.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.todolist.databinding.FragmentStatsBinding;
import com.example.todolist.utils.model.Stats;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StatsFragment extends Fragment {
    private FragmentStatsBinding binding;
    private DatabaseReference statsDatabase;
    private FirebaseAuth auth;
    private String authId;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public StatsFragment() {
        // Required empty public constructor
    }

    public static StatsFragment newInstance(String param1, String param2) {
        StatsFragment fragment = new StatsFragment();
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
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        authId = auth.getCurrentUser().getUid();
        statsDatabase = FirebaseDatabase.getInstance().getReference().child("Stats").child(authId);

        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        loadStats();
    }

    private void loadStats() {
        statsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Stats stats = snapshot.getValue(Stats.class);
                if (stats != null) {
                    binding.totalTasksText.setText(String.valueOf(stats.getTotalTasks()));
                    binding.tasksToDoText.setText(String.valueOf(stats.getTasksToDo()));
                    binding.completedTasksText.setText(String.valueOf(stats.getCompletedTasks()));
                    binding.totalBonusesText.setText(String.valueOf(stats.getTotalScores()));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors.
            }
        });
    }
}