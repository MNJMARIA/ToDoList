package com.example.todolist.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
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
import com.example.todolist.data.TaskRepository;
import com.example.todolist.data.UserProfileRepository;
import com.example.todolist.databinding.FragmentHomeBinding;
import com.example.todolist.reminders.ReminderScheduler;
import com.example.todolist.update.UpdateController;
import com.example.todolist.update.UpdateDialogFragment;
import com.example.todolist.utils.AchievementUtils;
import com.example.todolist.utils.model.User;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
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

        b.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_menu) {
                drawerLayout.openDrawer(GravityCompat.START);
                return false; // "Menu" is a button, not a tab: it must not stay highlighted
            } else if (id == R.id.nav_day) {
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

        Bundle bundle = getArguments();
        if (bundle != null) {
            View headerView = navigationView.getHeaderView(0);
            showHeader(headerView, bundle.getString("name"), bundle.getString("username"), bundle.getString("email"));
        }

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
        binding = null;
    }

    /** Live total score in the top bar, and the avatar in the drawer header follows it. */
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
                ImageView avatar = headerView.findViewById(R.id.accountIcon);
                if (avatar != null) avatar.setImageResource(AchievementUtils.avatarResForScore(totalScores));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.error_downloading_data, Toast.LENGTH_SHORT).show();
                }
            }
        };
        statsRef.addValueEventListener(statsListener);
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

    private void openSettings() {
        UserProfileRepository profiles = UserProfileRepository.forCurrentUser();
        if (profiles == null) return;
        profiles.loadOrCreate(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onProfile(@NonNull User user) {
                if (!isAdded()) return;
                Bundle bundle = new Bundle();
                bundle.putString("name", user.getName());
                bundle.putString("username", user.getUsername());
                bundle.putString("email", user.getEmail());
                SettingsFragment settings = new SettingsFragment();
                settings.setArguments(bundle);
                openFullScreen(settings);
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.error_downloading_data, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void confirmLogoutDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.confirm_exit_dialog);

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
