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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentHomeBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment implements NavigationView.OnNavigationItemSelectedListener {
    private FragmentHomeBinding binding;
    private DrawerLayout drawerLayout;
    private NavController navController;
    private TextView textForScores;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        drawerLayout = view.findViewById(R.id.drawer_layout);

        NavigationView navigationView = view.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // Знаходження TextView за ідентифікатором
        textForScores = view.findViewById(R.id.textForScores);
        // Отримання ID поточного користувача
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Знаходження посилання на вузол користувача
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference().child("Stats").child(currentUserId);

        currentUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Перевірка чи існують дані для поточного користувача
                if (snapshot.exists()) {
                    // Отримання totalScores для поточного користувача
                    int totalScores = snapshot.child("totalScores").getValue(Integer.class);
                    // Встановлення тексту на TextView
                    textForScores.setText(""+totalScores);
                } else {
                    textForScores.setText("000000");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), R.string.error_downloading_data, Toast.LENGTH_SHORT).show();
            }
        });
        // Завантаження даних з Firebase при відкритті Drawer Menu
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                loadUserData(navigationView);
            }
        });

        binding.bottomNavigationView.setSelectedItemId(R.id.nav_day); // Вибрати вкладку "День" за замовчуванням
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_menu) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else if (item.getItemId() == R.id.nav_day) {
                replaceFragment(new DayFragment());
            } else if (item.getItemId() == R.id.nav_month) {
                replaceFragment(new MonthFragment());
            }
            return true;
        });

        // Отримання інформації про користувача з аргументів
        Bundle bundle = getArguments();
        if (bundle != null) {
            String name = bundle.getString("name");
            String username = bundle.getString("username");
            String email = bundle.getString("email");

            // Знаходження відповідних TextView у вашому дизайні і встановлення значень
            View headerView = navigationView.getHeaderView(0);
            TextView usernameTextView = headerView.findViewById(R.id.username);
            TextView emailTextView = headerView.findViewById(R.id.email);

            usernameTextView.setText(name);
            emailTextView.setText(email);
            changeAccountPhotoOnCurrentAchieve(headerView);
        }
        // Встановлення вмісту вкладки "День" за замовчуванням
        replaceFragment(new DayFragment());
    }

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

    private void changeAccountPhotoOnCurrentAchieve(View headerView) {
        DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("Stats")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Отримайте значення поля totalScores
                    int totalScores = snapshot.child("totalScores").getValue(Integer.class);
                    // Використовуйте це значення для встановлення зображення досягнення або для інших потреб
                    int achievementImageResId = getAchievementImageByScore(totalScores);
                    ImageView accountView = headerView.findViewById(R.id.accountIcon);
                    accountView.setImageResource(achievementImageResId);
                } else {
                    // Якщо такого вузла не існує, обробіть цей випадок за вашим власним розсудом
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Обробіть помилку при читанні з бази даних, якщо потрібно
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            replaceFragment(new HomeFragment());
        }else if (itemId == R.id.nav_settings) {
            openSettingsFragmentFullScreen();
        } else if (itemId == R.id.nav_statistics) {
            openStatisticsFragmentFullScreen();
        } else if (itemId == R.id.nav_achievements) {
            openAchievementsFragmentFullScreen();
        } else if (itemId == R.id.nav_contact) {
            openContactUsFragmentFullScreen();
        } else if (itemId == R.id.nav_about) {
            openAboutFragmentFullScreen();
        } else if (itemId == R.id.nav_logout) {
            confirmLogoutDialog();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragment(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView2, fragment)
                .commit();
    }

    private void openSettingsFragmentFullScreen() {
        Bundle bundle = new Bundle();
        // Передати актуальні дані
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String username = snapshot.child("username").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        String password = snapshot.child("password").getValue(String.class);

                        bundle.putString("name", name);
                        bundle.putString("username", username);
                        bundle.putString("email", email);
                        bundle.putString("password", password);

                        SettingsFragment settingsFragment = new SettingsFragment();
                        settingsFragment.setArguments(bundle);

                        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.replace(android.R.id.content, settingsFragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(requireContext(), R.string.error_downloading_data, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void openStatisticsFragmentFullScreen() {
        // Отримати FragmentManager від активності
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        // Почати транзакцію фрагменту
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // Замінити поточний фрагмент на SettingsFragment
        StatsFragment statsFragment = new StatsFragment();
        fragmentTransaction.replace(android.R.id.content, statsFragment); // Використайте android.R.id.content, щоб замінити весь вміст активності
        fragmentTransaction.addToBackStack(null);
        // Завершити транзакцію
        fragmentTransaction.commit();
    }
    private void openAchievementsFragmentFullScreen() {
        AchievementsFragment achievementsFragment = new AchievementsFragment();
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(android.R.id.content, achievementsFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    private void openContactUsFragmentFullScreen() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        ContactUsFragment contactUsFragment = new ContactUsFragment();
        fragmentTransaction.add(android.R.id.content, contactUsFragment); // Використайте add замість replace
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    private void openAboutFragmentFullScreen() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        AboutFragment aboutFragment = new AboutFragment();
        fragmentTransaction.replace(android.R.id.content, aboutFragment); // Використайте add замість replace
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    private void confirmLogoutDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.confirm_exit_dialog);

        TextView confirmExitText = dialog.findViewById(R.id.confirmExitText);
        TextView exitText = dialog.findViewById(R.id.exitText);
        Button noButton = dialog.findViewById(R.id.noButton);
        Button yesButton = dialog.findViewById(R.id.yesButton);

        noButton.setOnClickListener(v -> dialog.dismiss());

        yesButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            navController.navigate(R.id.action_homeFragment_to_signInFragment);
            Toast.makeText(requireContext(), R.string.exit_successful, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadUserData(NavigationView navigationView) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String username = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String password = snapshot.child("password").getValue(String.class);

                    View headerView = navigationView.getHeaderView(0);
                    TextView usernameTextView = headerView.findViewById(R.id.username);
                    TextView emailTextView = headerView.findViewById(R.id.email);

                    usernameTextView.setText(username);
                    emailTextView.setText(email);
                    changeAccountPhotoOnCurrentAchieve(headerView);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), R.string.error_downloading_data, Toast.LENGTH_SHORT).show();
            }
        });
    }
}