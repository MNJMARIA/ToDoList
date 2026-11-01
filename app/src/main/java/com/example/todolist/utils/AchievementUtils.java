package com.example.todolist.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import com.example.todolist.R;
import com.example.todolist.core.AchievementRules;

public class AchievementUtils {
    private static final String TAG = "AchievementUtils";

    private static final int[] DIALOG_IMAGES = {
            R.drawable.ribbon_achieve_1, R.drawable.ribbon_achieve_2, R.drawable.ribbon_achieve_3,
            R.drawable.ribbon_achieve_4, R.drawable.ribbon_achieve_5, R.drawable.ribbon_achieve_6,
            R.drawable.ribbon_achieve_7, R.drawable.ribbon_achieve_8, R.drawable.ribbon_achieve_9,
            R.drawable.ribbon_achieve_10, R.drawable.ribbon_achieve_11, R.drawable.ribbon_achieve_12};

    private static final int[] RANK_IMAGES = {
            R.drawable.rank_12_level_1, R.drawable.rank_12_level_2, R.drawable.rank_12_level_3,
            R.drawable.rank_12_level_4, R.drawable.rank_12_level_5, R.drawable.rank_12_level_6,
            R.drawable.rank_12_level_7, R.drawable.rank_12_level_8, R.drawable.rank_12_level_9,
            R.drawable.rank_12_level_10, R.drawable.rank_12_level_11, R.drawable.rank_12_level_12};

    /** Avatar of the highest level reached; level 1 until the first threshold is passed. */
    public static int avatarResForScore(int totalScores) {
        return RANK_IMAGES[AchievementRules.levelIndex(totalScores)];
    }

    /**
     * Shows the "new achievement" dialog if moving from {@code previousScore} to {@code newScore}
     * crossed a threshold. Comparing the two scores (instead of testing {@code score == threshold})
     * means an achievement is never skipped when points arrive in different steps.
     */
    public static void checkForAchievements(Context context, int previousScore, int newScore) {
        int index = AchievementRules.newlyUnlockedIndex(previousScore, newScore);
        if (index >= 0) {
            Log.d(TAG, "Achievement unlocked: " + AchievementRules.titles()[index]);
            showAchievementDialog(context, index);
        }
    }

    public static void showAchievementDialog(Context context, int achievementIndex) {
        if (context == null || achievementIndex < 0 || achievementIndex >= DIALOG_IMAGES.length) return;
        try {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.new_achieve_dialog, null);
            ImageView image = dialogView.findViewById(R.id.newAchieveImage);
            image.setImageResource(DIALOG_IMAGES[achievementIndex]);

            AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
            dialogView.findViewById(R.id.gotIt).setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (RuntimeException e) {
            // E.g. the host activity is already finishing; an achievement popup is never worth a crash.
            Log.e(TAG, "Error showing achievement dialog: " + e.getMessage(), e);
        }
    }
}
