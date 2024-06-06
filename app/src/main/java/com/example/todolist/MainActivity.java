package com.example.todolist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPreferences();
        setContentView(R.layout.activity_main);
    }

    private void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences("LANGUAGE_PREFS", Context.MODE_PRIVATE);
        String selectedLanguageCode = preferences.getString("selectedLanguage", "uk");
        setAppLocale(selectedLanguageCode);

        SharedPreferences modePreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        boolean nightMode = modePreferences.getBoolean("nightMode", false);
        if (nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setAppLocale(String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}