package com.example.todolist.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import com.example.todolist.R;

public class AchievementUtils {
    private static final String TAG = "AchievementUtils";
    private static String[] titles = {"500", "1000", "2000", "5000", "7500", "10000", "15000", "25000", "30000", "50000", "70000", "100000"};

    private static int[] dialogImages = {R.drawable.ribbon_achieve_1, R.drawable.ribbon_achieve_2, R.drawable.ribbon_achieve_3,
            R.drawable.ribbon_achieve_4, R.drawable.ribbon_achieve_5, R.drawable.ribbon_achieve_6,
            R.drawable.ribbon_achieve_7, R.drawable.ribbon_achieve_8, R.drawable.ribbon_achieve_9,
            R.drawable.ribbon_achieve_10, R.drawable.ribbon_achieve_11, R.drawable.ribbon_achieve_12};

    public static void checkForAchievements(Context context, int totalScores) {
        int scoreThreshold = 500;
        int i = 0;
        for (i = 0; i < titles.length; i++) {
            try {
                scoreThreshold = Integer.parseInt(titles[i]);
            } catch (NumberFormatException e) {
                break; // Якщо не можна перетворити в число, переходимо до наступного елемента
            }
            if (totalScores == scoreThreshold) {
                Log.d(TAG, "Achievement unlocked: " + titles[i]);
                showAchievementDialog(context, dialogImages[i]);
                break;
            }
        }
    }
    private static void showAchievementDialog(Context context, int imageResId) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.new_achieve_dialog, null);
            ImageView successImage = dialogView.findViewById(R.id.newAchieveImage);
            successImage.setImageResource(imageResId);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            dialogView.findViewById(R.id.gotIt).setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing achievement dialog: " + e.getMessage(), e);
        }
    }
}