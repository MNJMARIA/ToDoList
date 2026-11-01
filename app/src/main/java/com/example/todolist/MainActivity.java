package com.example.todolist;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import com.example.todolist.notify.NotificationHelper;
import com.example.todolist.reminders.ReminderScheduler;
import com.example.todolist.update.UpdatedMessage;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_MODE = "MODE";
    private static final String KEY_PERMISSION_ASKED = "notificationPermissionAsked";

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    ReminderScheduler.rescheduleAll(this);
                } else {
                    // Without permission nothing can be shown; keep the switch in settings honest.
                    getSharedPreferences(PREFS_MODE, Context.MODE_PRIVATE)
                            .edit().putBoolean("notificationsEnabled", false).apply();
                    ReminderScheduler.cancelAllAlarms(this);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPreferences();
        setContentView(R.layout.activity_main);

        NotificationHelper.ensureChannels(this);
        requestNotificationPermissionOnce();
        if (savedInstanceState == null) {
            // "The app was updated to X" - only on a real launch, not after rotation.
            UpdatedMessage.showIfNeeded(this);
        }
    }

    /** Android 13+: reminders need POST_NOTIFICATIONS. Asked once, in context of the first launch. */
    private void requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_MODE, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_PERMISSION_ASKED, false) || !ReminderScheduler.isEnabled(this)) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        prefs.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply();
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences("LANGUAGE_PREFS", Context.MODE_PRIVATE);
        String selectedLanguageCode = preferences.getString("selectedLanguage", "uk");
        setAppLocale(selectedLanguageCode);

        SharedPreferences modePreferences = getSharedPreferences(PREFS_MODE, Context.MODE_PRIVATE);
        boolean nightMode = modePreferences.getBoolean("nightMode", false);
        AppCompatDelegate.setDefaultNightMode(
                nightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void setAppLocale(String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
