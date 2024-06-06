package com.example.todolist.fragments;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentAchievementsBinding;
import com.example.todolist.utils.adapter.AchievementsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AchievementsFragment extends Fragment {
    private FragmentAchievementsBinding binding;
    private AchievementsAdapter adapter;
    private DatabaseReference statsDatabase;
    private DatabaseReference achievementsDatabase;
    private int totalScores;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAchievementsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String[] titles = {"500", "1000", "2000", "5000", "7500", "10000", "15000", "25000", "30000", "50000", "70000", "100000"};
        int[] greyImages = {R.drawable.grey_rank_12_level_1, R.drawable.grey_rank_12_level_2, R.drawable.grey_rank_12_level_3, R.drawable.grey_rank_12_level_4,
                R.drawable.grey_rank_12_level_5, R.drawable.grey_rank_12_level_6, R.drawable.grey_rank_12_level_7, R.drawable.grey_rank_12_level_8,
                R.drawable.grey_rank_12_level_9, R.drawable.grey_rank_12_level_10, R.drawable.grey_rank_12_level_11, R.drawable.grey_rank_12_level_12};

        int[] colorImages = {R.drawable.rank_12_level_1, R.drawable.rank_12_level_2, R.drawable.rank_12_level_3, R.drawable.rank_12_level_4,
                R.drawable.rank_12_level_5, R.drawable.rank_12_level_6, R.drawable.rank_12_level_7, R.drawable.rank_12_level_8,
                R.drawable.rank_12_level_9, R.drawable.rank_12_level_10, R.drawable.rank_12_level_11, R.drawable.rank_12_level_12};

        // Отримання поточного користувача
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            return;
        }

        // Отримання ідентифікатора поточного користувача
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        statsDatabase = FirebaseDatabase.getInstance().getReference().child("Stats").child(userId);
        achievementsDatabase = FirebaseDatabase.getInstance().getReference().child("achievements");

        // Отримання доступу до SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("AchievementPrefs", Context.MODE_PRIVATE);

        statsDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.exists() && snapshot.child("totalScores").getValue() != null) {
                        int previousTotalScores = totalScores;
                        totalScores = snapshot.child("totalScores").getValue(Integer.class);

                        Log.d(TAG, "Total Scores: " + totalScores);

                        if (adapter == null) {
                            adapter = new AchievementsAdapter(titles, greyImages, colorImages, totalScores);
                            binding.mainRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
                            binding.mainRecyclerView.setAdapter(adapter);
                        } else {
                            adapter.updateTotalScores(totalScores);
                        }
                    } else {
                        Log.e(TAG, "Total scores not found or snapshot does not exist.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in onDataChange: " + e.getMessage(), e);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage(), error.toException());
            }
        });
        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}