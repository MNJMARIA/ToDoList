package com.example.todolist.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.todolist.BuildConfig;
import com.example.todolist.R;
import com.example.todolist.data.UserProfileRepository;
import com.example.todolist.databinding.FragmentSettingsBinding;
import com.example.todolist.reminders.ReminderScheduler;
import com.example.todolist.update.UpdateController;
import com.example.todolist.update.UpdateDialogFragment;
import com.example.todolist.utils.AchievementUtils;
import com.example.todolist.utils.model.User;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private static final String PREFS_MODE = "MODE";

    @Nullable
    private FragmentSettingsBinding binding;
    private SharedPreferences sharedPreferences;

    /** Android 13+ asks for permission before the first notification can be shown. */
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                FragmentSettingsBinding b = binding;
                if (b == null) return;
                if (granted) {
                    applyNotificationsEnabled(true);
                } else {
                    Toast.makeText(requireContext(), R.string.notifications_permission_denied, Toast.LENGTH_LONG).show();
                    b.switchNotifications.setChecked(false); // listener stores "off" and cancels alarms
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentSettingsBinding b = binding;
        if (b == null) return;
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_MODE, Context.MODE_PRIVATE);

        loadAvatar();

        Bundle args = getArguments();
        if (args != null) {
            showProfile(args.getString("name"), args.getString("username"), args.getString("email"));
        }
        b.editPersonalInfo.setOnClickListener(v -> openEditPersonalInfoFragmentFullScreen());

        b.switchTheme.setChecked(sharedPreferences.getBoolean("nightMode", false));
        b.switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            sharedPreferences.edit().putBoolean("nightMode", isChecked).apply();
        });

        b.switchNotifications.setChecked(ReminderScheduler.isEnabled(requireContext()));
        b.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && needsNotificationPermission()) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                applyNotificationsEnabled(isChecked);
            }
        });

        b.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        b.changeLanguageButton.setOnClickListener(v -> showLanguageSelectionDialog());

        setupVersionRow(b);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfile(); // pick up edits made on the "edit personal info" screen
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ------------------------------------------------------------ notifications

    private boolean needsNotificationPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    /** Stores the choice and really starts/stops the alarms; before, switching off cancelled nothing useful. */
    private void applyNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean("notificationsEnabled", enabled).apply();
        if (enabled) {
            ReminderScheduler.rescheduleAll(requireContext());
        } else {
            ReminderScheduler.cancelAllAlarms(requireContext());
        }
    }

    // ------------------------------------------------------------ version / updates

    private void setupVersionRow(FragmentSettingsBinding b) {
        b.appVersionText.setText(getString(R.string.version_format, BuildConfig.VERSION_NAME));
        b.appVersionRow.setOnClickListener(v -> UpdateDialogFragment.show(getChildFragmentManager()));
        UpdateController.get(requireContext()).getAvailableRelease().observe(getViewLifecycleOwner(), release -> {
            FragmentSettingsBinding live = binding;
            if (live != null) {
                live.updateArrow.setVisibility(release == null ? View.GONE : View.VISIBLE);
            }
        });
    }

    // ------------------------------------------------------------ profile

    private void showProfile(@Nullable String name, @Nullable String username, @Nullable String email) {
        FragmentSettingsBinding b = binding;
        if (b == null) return;
        b.nameTextView.setText(name == null ? "" : name);
        b.usernameTextView.setText(username == null ? "" : username);
        b.emailTextView.setText(email == null ? "" : email);
    }

    private void refreshProfile() {
        UserProfileRepository profiles = UserProfileRepository.forCurrentUser();
        if (profiles == null) return;
        profiles.loadOrCreate(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onProfile(@NonNull User user) {
                showProfile(user.getName(), user.getUsername(), user.getEmail());
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.error_downloading_data, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadAvatar() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseDatabase.getInstance().getReference("Stats").child(uid).child("totalScores")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        FragmentSettingsBinding b = binding;
                        if (b == null) return;
                        Integer score = snapshot.getValue(Integer.class);
                        b.accountPhoto.setImageResource(AchievementUtils.avatarResForScore(score == null ? 0 : score));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // The default avatar stays; nothing to tell the user.
                    }
                });
    }

    /** Asks for the account password and checks it with Firebase Authentication (never with a stored copy). */
    private void openEditPersonalInfoFragmentFullScreen() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        View dialogView = requireActivity().getLayoutInflater().inflate(R.layout.verify_your_pasword_dialog, null);
        EditText passwordEditText = dialogView.findViewById(R.id.passwordText);
        Button verifyButton = dialogView.findViewById(R.id.selectButton);
        AlertDialog alertDialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

        verifyButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString();
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), getString(R.string.please_enter_your_password), Toast.LENGTH_SHORT).show();
                return;
            }
            verifyButton.setEnabled(false);
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                verifyButton.setEnabled(true);
                if (!isAdded()) return;
                if (task.isSuccessful()) {
                    alertDialog.dismiss();
                    openEditPersonalInfoFragment();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.incorrect_password), Toast.LENGTH_SHORT).show();
                }
            });
        });
        alertDialog.show();
    }

    private void openEditPersonalInfoFragment() {
        FragmentSettingsBinding b = binding;
        if (b == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("name", b.nameTextView.getText().toString());
        bundle.putString("username", b.usernameTextView.getText().toString());
        bundle.putString("email", b.emailTextView.getText().toString());

        EditPersonalInfoFragment fragment = new EditPersonalInfoFragment();
        fragment.setArguments(bundle);

        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(android.R.id.content, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // ------------------------------------------------------------ language

    private void changeAppLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireContext().getResources().updateConfiguration(config, requireContext().getResources().getDisplayMetrics());
        requireActivity().recreate();
    }

    private void showLanguageSelectionDialog() {
        View dialogView = requireActivity().getLayoutInflater().inflate(R.layout.choose_language_dialog, null);
        RadioGroup radioGroupLanguages = dialogView.findViewById(R.id.radioGroupLanguages);
        Button selectButton = dialogView.findViewById(R.id.selectButton);

        SharedPreferences preferences = requireActivity().getSharedPreferences("LANGUAGE_PREFS", Context.MODE_PRIVATE);
        String[] languages = {"uk", "en", "fr", "de", "ko", "ja"};
        String selectedLanguageCode = preferences.getString("selectedLanguage", "uk");

        for (int i = 0; i < languages.length && i < radioGroupLanguages.getChildCount(); i++) {
            if (languages[i].equals(selectedLanguageCode)) {
                radioGroupLanguages.check(radioGroupLanguages.getChildAt(i).getId());
                break;
            }
        }
        AlertDialog alertDialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        selectButton.setOnClickListener(v -> {
            int selectedId = radioGroupLanguages.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(requireContext(), getString(R.string.please_select_a_language), Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
            int selectedIndex = radioGroupLanguages.indexOfChild(selectedRadioButton);
            if (selectedIndex < 0 || selectedIndex >= languages.length) return;
            String newLanguageCode = languages[selectedIndex];
            preferences.edit().putString("selectedLanguage", newLanguageCode).apply();
            alertDialog.dismiss();
            changeAppLanguage(newLanguageCode);
        });
        alertDialog.show();
    }
}
