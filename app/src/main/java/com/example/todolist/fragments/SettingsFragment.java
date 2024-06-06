package com.example.todolist.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentSettingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import java.util.Locale;
import java.util.Objects;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Button;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SwitchCompat switchMode;
    private SwitchCompat switchNotifications;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean nightMode;
    private boolean notificationsEnabled;
    private DatabaseReference database;
    private FirebaseAuth auth;
    private String authId;
    private LinearLayout headerLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    // Отримання зображення досягнення за кількістю балів
    private int getAchievementImageByScore(int totalScores) {
        if (totalScores >= 100000) {
            return R.drawable.rank_12_level_12;
        } else if (totalScores >= 70000) {
            return R.drawable.rank_12_level_11;
        } else if (totalScores >= 50000) {
            return R.drawable.rank_12_level_10;
        } else if (totalScores >= 30000) {
            return R.drawable.rank_12_level_9;
        } else if (totalScores >= 25000) {
            return R.drawable.rank_12_level_8;
        } else if (totalScores >= 15000) {
            return R.drawable.rank_12_level_7;
        } else if (totalScores >= 10000) {
            return R.drawable.rank_12_level_6;
        } else if (totalScores >= 7500) {
            return R.drawable.rank_12_level_5;
        } else if (totalScores >= 5000) {
            return R.drawable.rank_12_level_4;
        } else if (totalScores >= 2000) {
            return R.drawable.rank_12_level_3;
        } else if (totalScores >= 1000) {
            return R.drawable.rank_12_level_2;
        } else if (totalScores >= 500) {
            return R.drawable.rank_12_level_1;
        } else {
            return R.drawable.rank_12_level_1; // Зображення за замовчуванням
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        changeAccountPhotoOnCurrentAchieve();// Отримайте кількість балів користувача з Firebase або з іншого місця

        headerLayout = view.findViewById(R.id.headerLayout);

        // Додавання FragmentResultListener
        getParentFragmentManager().setFragmentResultListener("userDataKey", this, (requestKey, bundle) -> {
            String name = bundle.getString("name");
            String username = bundle.getString("username");
            String email = bundle.getString("email");
            binding.nameTextView.setText(name);
            binding.usernameTextView.setText(username);
            binding.emailTextView.setText(email);
        });
        // Отримання інформації про користувача з аргументів
        Bundle bundle = getArguments();
        if (bundle != null) {
            String name = bundle.getString("name");
            String username = bundle.getString("username");
            String email = bundle.getString("email");
            // Встановлення значень у відповідні TextView
            binding.nameTextView.setText(name);
            binding.usernameTextView.setText(username);
            binding.emailTextView.setText(email);
        }
        binding.editPersonalInfo.setOnClickListener(v -> openEditPersonalInfoFragmentFullScreen());

        switchMode = binding.switchTheme;
        switchNotifications = binding.switchNotifications; // Assuming you have a switch for notifications

        sharedPreferences = requireActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
        nightMode = sharedPreferences.getBoolean("nightMode", false);
        notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", true);

        switchMode.setChecked(nightMode);
        switchNotifications.setChecked(notificationsEnabled);

        switchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            editor = sharedPreferences.edit();
            editor.putBoolean("nightMode", isChecked);
            editor.apply();
            nightMode = isChecked; // Оновлення стану nightMode
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor = sharedPreferences.edit();
            editor.putBoolean("notificationsEnabled", isChecked);
            editor.apply();
            notificationsEnabled = isChecked;
        });

        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.changeLanguageButton.setOnClickListener(v -> showLanguageSelectionDialog());
    }

    private void changeAccountPhotoOnCurrentAchieve() {
        DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("Stats")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Отримайте значення поля totalScores
                    int totalScores = snapshot.child("totalScores").getValue(Integer.class);
                    // Використовуєм це значення для встановлення зображення досягнення або для інших потреб
                    int achievementImageResId = getAchievementImageByScore(totalScores);
                    binding.accountPhoto.setImageResource(achievementImageResId);
                } else {
                    // Якщо такого вузла не існує, обробити цей випадок за вашим власним розсудом
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Обробити помилку при читанні з бази даних, якщо потрібно
            }
        });
    }

    private void changeAppLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        getActivity().recreate();
    }

    private void showLanguageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.choose_language_dialog, null);
        builder.setView(dialogView);

        RadioGroup radioGroupLanguages = dialogView.findViewById(R.id.radioGroupLanguages);
        Button selectButton = dialogView.findViewById(R.id.selectButton);

        SharedPreferences preferences = requireActivity().getSharedPreferences("LANGUAGE_PREFS", Context.MODE_PRIVATE);
        String[] languages = {"uk", "en", "fr", "de", "ko", "ja"};
        String selectedLanguageCode = preferences.getString("selectedLanguage", "uk");

        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(selectedLanguageCode)) {
                radioGroupLanguages.check(radioGroupLanguages.getChildAt(i).getId());
                break;
            }
        }
        AlertDialog alertDialog = builder.create();
        selectButton.setOnClickListener(v -> {
            int selectedId = radioGroupLanguages.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                int selectedIndex = radioGroupLanguages.indexOfChild(selectedRadioButton);
                String newLanguageCode = languages[selectedIndex];

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("selectedLanguage", newLanguageCode);
                editor.apply();

                changeAppLanguage(newLanguageCode);
                alertDialog.dismiss();
            } else {
                Toast.makeText(requireContext(), getString(R.string.please_select_a_language), Toast.LENGTH_SHORT).show();
            }
        });
        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPersInfoFromFirebase(); // Оновлення даних при поверненні до фрагменту
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openEditPersonalInfoFragmentFullScreen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.verify_your_pasword_dialog, null);
        builder.setView(dialogView);

        EditText passwordEditText = dialogView.findViewById(R.id.passwordText);

        AlertDialog alertDialog = builder.create();

        Button verifyButton = dialogView.findViewById(R.id.selectButton);
        verifyButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString().trim();
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), getString(R.string.please_enter_your_password), Toast.LENGTH_SHORT).show();
                return;
            }
            // Перевірка пароля в Firebase
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String savedPassword = snapshot.child("password").getValue(String.class);
                    if (savedPassword.equals(password)) {
                        // Пароль вірний, тому відкрити EditPersonalInfoFragment
                        alertDialog.dismiss();
                        openEditPersonalInfoFragment();
                    } else {
                        // Пароль невірний, відобразити повідомлення про помилку
                        Toast.makeText(requireContext(), getString(R.string.incorrect_password), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(requireContext(), getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
                }
            });
        });
        alertDialog.show();
    }

    private void openEditPersonalInfoFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        EditPersonalInfoFragment editPersonalInfoFragment = new EditPersonalInfoFragment();

        Bundle bundle = new Bundle();
        bundle.putString("name", binding.nameTextView.getText().toString());
        bundle.putString("username", binding.usernameTextView.getText().toString());
        bundle.putString("email", binding.emailTextView.getText().toString());
        bundle.putString("password", getArguments().getString("password"));

        editPersonalInfoFragment.setArguments(bundle);

        fragmentTransaction.replace(android.R.id.content, editPersonalInfoFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit(); // Отримати FragmentManager від активності
    }
    public void getPersInfoFromFirebase() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String username = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String password = snapshot.child("password").getValue(String.class);

                    binding.nameTextView.setText(name);
                    binding.usernameTextView.setText(username);
                    binding.emailTextView.setText(email);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), getString(R.string.error_downloading_data), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void init() {
        auth = FirebaseAuth.getInstance();
        authId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        database = FirebaseDatabase.getInstance().getReference().child("Users").child(authId);
    }
}