package com.example.todolist.utils.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.AchievementsViewHolder> {
    private String[] titles;
    private int[] greyImages;
    private int[] colorImages;
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

        int scoreThreshold;
        try {
            scoreThreshold = Integer.parseInt(titles[position]);
        } catch (NumberFormatException e) {
            scoreThreshold = Integer.MAX_VALUE; // Or some safe default value
        }

        if (totalScores >= scoreThreshold) {
            holder.iconImageView.setImageResource(colorImages[position]);
        } else {
            holder.iconImageView.setImageResource(greyImages[position]);
        }
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    public void updateTotalScores(int newTotalScores) {
        this.totalScores = newTotalScores;
        notifyDataSetChanged(); // Refresh the adapter with the new score
    }

    static class AchievementsViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ImageView iconImageView;
        AchievementsViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.achievementTitle);
            iconImageView = itemView.findViewById(R.id.image);
        }
    }
}