package com.example.todolist.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.todolist.R;
import com.example.todolist.core.AchievementRules;
import com.example.todolist.databinding.FragmentAchievementsBinding;
import com.example.todolist.utils.UiErrors;
import com.example.todolist.utils.adapter.AchievementsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AchievementsFragment extends Fragment {
    private static final String TAG = "AchievementsFragment";

    @Nullable
    private FragmentAchievementsBinding binding;
    @Nullable
    private AchievementsAdapter adapter;
    @Nullable
    private DatabaseReference statsDatabase;
    @Nullable
    private ValueEventListener statsListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAchievementsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentAchievementsBinding b = binding;
        if (b == null) return;
        b.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Log.e(TAG, "User not authenticated");
            return;
        }

        int[] greyImages = {R.drawable.grey_rank_12_level_1, R.drawable.grey_rank_12_level_2, R.drawable.grey_rank_12_level_3,
                R.drawable.grey_rank_12_level_4, R.drawable.grey_rank_12_level_5, R.drawable.grey_rank_12_level_6,
                R.drawable.grey_rank_12_level_7, R.drawable.grey_rank_12_level_8, R.drawable.grey_rank_12_level_9,
                R.drawable.grey_rank_12_level_10, R.drawable.grey_rank_12_level_11, R.drawable.grey_rank_12_level_12};
        int[] colorImages = {R.drawable.rank_12_level_1, R.drawable.rank_12_level_2, R.drawable.rank_12_level_3,
                R.drawable.rank_12_level_4, R.drawable.rank_12_level_5, R.drawable.rank_12_level_6,
                R.drawable.rank_12_level_7, R.drawable.rank_12_level_8, R.drawable.rank_12_level_9,
                R.drawable.rank_12_level_10, R.drawable.rank_12_level_11, R.drawable.rank_12_level_12};

        // The grid is shown at once, every achievement still grey (locked). The colourful ones appear as soon as
        // the score is known; before, the screen stayed empty until the database answered (or forever, if it never did).
        adapter = new AchievementsAdapter(AchievementRules.titles(), greyImages, colorImages, 0);
        b.mainRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        b.mainRecyclerView.setAdapter(adapter);

        statsDatabase = FirebaseDatabase.getInstance().getReference().child("Stats").child(uid);
        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FragmentAchievementsBinding live = binding;
                if (live == null) return;
                // A new user has no stats yet: show the locked achievements instead of an empty screen.
                Integer value = snapshot.child("totalScores").getValue(Integer.class);
                int totalScores = value == null ? 0 : value;

                if (adapter != null) adapter.updateTotalScores(totalScores);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage(), error.toException());
                if (isAdded()) UiErrors.show(requireContext(), R.string.error_load_title, error.getMessage());
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
        adapter = null;
        binding = null;
    }
}
