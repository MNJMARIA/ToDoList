package com.example.todolist;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Завантажити збережену мову
        loadLanguage();
        setContentView(R.layout.activity_main);
    }

    private void loadLanguage() {
        SharedPreferences preferences = getSharedPreferences("LANGUAGE_PREFS", Context.MODE_PRIVATE);
        String selectedLanguageCode = preferences.getString("selectedLanguage", "uk");
        setAppLocale(selectedLanguageCode);
    }

    private void setAppLocale(String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}