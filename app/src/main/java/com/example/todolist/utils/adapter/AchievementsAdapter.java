package com.example.todolist.utils.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.core.AchievementRules;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.AchievementsViewHolder> {
    private final String[] titles;
    private final int[] greyImages;
    private final int[] colorImages;
    private int totalScores;

    public AchievementsAdapter(String[] titles, int[] greyImages, int[] colorImages, int totalScores) {
        this.titles = titles;
        this.greyImages = greyImages;
        this.colorImages = colorImages;
        this.totalScores = totalScores;
    }

    @NonNull
    @Override
    public AchievementsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.each_achieve_item, parent, false);
        return new AchievementsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementsViewHolder holder, int position) {
        holder.titleTextView.setText(titles[position]);
        boolean unlocked = AchievementRules.isUnlocked(position, totalScores);
        holder.iconImageView.setImageResource(unlocked ? colorImages[position] : greyImages[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    public void updateTotalScores(int newTotalScores) {
        if (newTotalScores == totalScores) return;
        this.totalScores = newTotalScores;
        notifyDataSetChanged();
    }

    static class AchievementsViewHolder extends RecyclerView.ViewHolder {
        final TextView titleTextView;
        final ImageView iconImageView;

        AchievementsViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.achievementTitle);
            iconImageView = itemView.findViewById(R.id.image);
        }
    }
}
