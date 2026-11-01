package com.example.todolist.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.todolist.R;
import com.example.todolist.core.ProfileNames;
import com.example.todolist.data.ConnectionMonitor;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.data.UserProfileRepository;
import com.example.todolist.databinding.FragmentHomeBinding;
import com.example.todolist.reminders.ReminderScheduler;
import com.example.todolist.update.UpdateController;
import com.example.todolist.update.UpdateDialogFragment;
import com.example.todolist.utils.AchievementUtils;
import com.example.todolist.ui.AvatarView;
import com.example.todolist.utils.ConnectionDiagnostics;
import com.example.todolist.utils.UiErrors;
import com.example.todolist.utils.model.User;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment implements NavigationView.OnNavigationItemSelectedListener {

    /** The "new version" hint is shown once per version and process, not on every visit to this screen. */
    private static String lastAnnouncedVersion;

    @Nullable
    private FragmentHomeBinding binding;
    private DrawerLayout drawerLayout;
    private NavController navController;
    @Nullable
    private DatabaseReference statsRef;
    @Nullable
    private ValueEventListener statsListener;

    private static final long OFFLINE_DELAY_MILLIS = 4000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable showOffline = () -> {
        FragmentHomeBinding live = binding;
        if (live != null) live.connectionBanner.setVisibility(View.VISIBLE);
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentHomeBinding b = binding;
        if (b == null) return;
        navController = Navigation.findNavController(view);
        drawerLayout = view.findViewById(R.id.drawer_layout);

        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Signed out in the meantime: go back to the sign-in screen instead of crashing.
            navController.navigate(R.id.action_homeFragment_to_signInFragment);
            return;
        }

        observeScore(view.findViewById(R.id.textForScores), navigationView.getHeaderView(0));

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                loadUserData(navigationView);
            }
        });

        b.menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        // Back closes the open side menu first instead of leaving the screen.
        OnBackPressedCallback closeDrawerOnBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), closeDrawerOnBack);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                closeDrawerOnBack.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                closeDrawerOnBack.setEnabled(false);
            }
        });

        b.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_day) {
                replaceFragment(new DayFragment());
            } else if (id == R.id.nav_month) {
                replaceFragment(new MonthFragment());
            }
            return true;
        });
        if (savedInstanceState == null) {
            // Only on first creation; after rotation the child fragment and the selected tab are restored.
            b.bottomNavigationView.setSelectedItemId(R.id.nav_day);
        }

        View headerView = navigationView.getHeaderView(0);
        Bundle bundle = getArguments();
        if (bundle != null && bundle.getString("email") != null) {
            showHeader(headerView, bundle.getString("name"), bundle.getString("username"), bundle.getString("email"));
        } else {
            // Show the sign-in account's data at once; the profile from the database replaces it when it arrives.
            FirebaseUser account = FirebaseAuth.getInstance().getCurrentUser();
            if (account != null) {
                String email = account.getEmail() == null ? "" : account.getEmail();
                showHeader(headerView, ProfileNames.displayName(account.getDisplayName(), email),
                        ProfileNames.username(email), email);
            }
        }
        loadUserData(navigationView);
        observeConnection(b);

        announceUpdates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (statsRef != null && statsListener != null) {
            statsRef.removeEventListener(statsListener);
        }
        statsRef = null;
        statsListener = null;
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }

    /** Live total score in the top bar; the rank badge on the drawer avatar follows it. */
    private void observeScore(TextView scoreView, View headerView) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        statsRef = FirebaseDatabase.getInstance().getReference().child("Stats").child(uid);
        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer value = snapshot.child("totalScores").getValue(Integer.class);
                int totalScores = value == null ? 0 : value;
                if (scoreView != null) scoreView.setText(String.valueOf(totalScores));
                AvatarView avatar = headerView.findViewById(R.id.accountIcon);
                if (avatar != null) avatar.setRank(AchievementUtils.avatarResForScore(totalScores));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) UiErrors.show(requireContext(), R.string.error_load_title, error.getMessage());
            }
        };
        statsRef.addValueEventListener(statsListener);
    }

    /**
     * Shows a red strip when the phone has been without a connection to the database for a few seconds.
     * Tapping it explains why (see {@link ConnectionDiagnostics}).
     */
    private void observeConnection(FragmentHomeBinding b) {
        b.connectionBanner.setOnClickListener(v -> ConnectionDiagnostics.show(requireContext()));
        ConnectionMonitor.connected().observe(getViewLifecycleOwner(), connected -> {
            handler.removeCallbacks(showOffline);
            if (Boolean.TRUE.equals(connected)) {
                b.connectionBanner.setVisibility(View.GONE);
            } else {
                handler.postDelayed(showOffline, OFFLINE_DELAY_MILLIS);
            }
        });
    }

    /** Checks GitHub for a newer release (at most once a day) and offers it in a Snackbar. */
    private void announceUpdates() {
        UpdateController controller = UpdateController.get(requireContext());
        controller.autoCheck();
        controller.getAvailableRelease().observe(getViewLifecycleOwner(), release -> {
            FragmentHomeBinding b = binding;
            if (release == null || b == null) return;
            String version = release.versionName();
            if (version.equals(lastAnnouncedVersion)) return;
            lastAnnouncedVersion = version;
            Snackbar.make(b.getRoot(), getString(R.string.update_available_snackbar, version), Snackbar.LENGTH_LONG)
                    .setAction(R.string.update_action, v -> showUpdateDialog())
                    .show();
        });
    }

    private void showUpdateDialog() {
        if (isAdded()) UpdateDialogFragment.show(getChildFragmentManager());
    }

    private void showHeader(View headerView, @Nullable String name, @Nullable String username, @Nullable String email) {
        TextView nameView = headerView.findViewById(R.id.username);
        TextView emailView = headerView.findViewById(R.id.email);
        // Always the same field (the display name), whether the header is filled at start or on drawer open.
        String shown = name != null && !name.isEmpty() ? name : username;
        nameView.setText(shown == null ? "" : shown);
        emailView.setText(email == null ? "" : email);
        AvatarView avatar = headerView.findViewById(R.id.accountIcon);
        if (avatar != null) avatar.setUser(shown, email);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            FragmentHomeBinding b = binding;
            if (b != null) b.bottomNavigationView.setSelectedItemId(R.id.nav_day);
        } else if (itemId == R.id.nav_settings) {
            openSettings();
        } else if (itemId == R.id.nav_statistics) {
            openFullScreen(new StatsFragment());
        } else if (itemId == R.id.nav_achievements) {
            openFullScreen(new AchievementsFragment());
        } else if (itemId == R.id.nav_contact) {
            openFullScreen(new ContactUsFragment());
        } else if (itemId == R.id.nav_about) {
            openFullScreen(new AboutFragment());
        } else if (itemId == R.id.nav_update) {
            showUpdateDialog();
        } else if (itemId == R.id.nav_logout) {
            confirmLogoutDialog();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragment(Fragment fragment) {
        // Child manager: the content belongs to this screen and goes away with it.
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView2, fragment)
                .commit();
    }

    private void openFullScreen(Fragment fragment) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(android.R.id.content, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Opens Settings at once. The names come from the sign-in account, so the screen never waits for the
     * database (before, an unreachable database meant Settings simply never opened); Settings itself
     * replaces them with the saved profile as soon as the database answers.
     */
    private void openSettings() {
        UserProfileRepository profiles = UserProfileRepository.forCurrentUser();
        if (profiles == null) return;
        User user = profiles.fallbackProfile();
        Bundle bundle = new Bundle();
        bundle.putString("name", user.getName());
        bundle.putString("username", user.getUsername());
        bundle.putString("email", user.getEmail());
        SettingsFragment settings = new SettingsFragment();
        settings.setArguments(bundle);
        openFullScreen(settings);
    }

    private void confirmLogoutDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.confirm_exit_dialog);
        if (dialog.getWindow() != null) {
            // The card draws its own rounded background; without this the square window shows behind the corners.
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button noButton = dialog.findViewById(R.id.noButton);
        Button yesButton = dialog.findViewById(R.id.yesButton);

        noButton.setOnClickListener(v -> dialog.dismiss());
        yesButton.setOnClickListener(v -> {
            dialog.dismiss();
            // The next account must not inherit this account's alarms or the "already repaired" marker.
            ReminderScheduler.clearAll(requireContext());
            TaskRepository.onSignedOut();
            FirebaseAuth.getInstance().signOut();
            navController.navigate(R.id.action_homeFragment_to_signInFragment);
            Toast.makeText(requireContext(), R.string.exit_successful, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void loadUserData(NavigationView navigationView) {
        UserProfileRepository profiles = UserProfileRepository.forCurrentUser();
        if (profiles == null) return;
        profiles.loadOrCreate(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onProfile(@NonNull User user) {
                if (!isAdded() || binding == null) return;
                showHeader(navigationView.getHeaderView(0), user.getName(), user.getUsername(), user.getEmail());
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.error_downloading_data, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
