package com.example.todolist.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.todolist.R;
import com.example.todolist.data.UserProfileRepository;
import com.example.todolist.utils.model.User;
import com.google.firebase.auth.FirebaseAuth;

public class SplashFragment extends Fragment {
    private static final long SPLASH_DELAY_MILLIS = 2000;
    private static final long PROFILE_TIMEOUT_MILLIS = 4000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        handler.postDelayed(this::route, SPLASH_DELAY_MILLIS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Without this the delayed callback fires after the user left the screen and navigates from a dead fragment.
        handler.removeCallbacksAndMessages(null);
    }

    private void route() {
        if (!isAdded()) return;
        UserProfileRepository profiles = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : UserProfileRepository.forCurrentUser();
        if (profiles == null) {
            navController.navigate(R.id.action_splashFragment_to_signInFragment);
            return;
        }
        // The profile only fills the menu header. If the database does not answer in time (no connection and
        // nothing cached yet) the user must not stay on the splash screen: continue and let the header load later.
        boolean[] routed = {false};
        Runnable giveUp = () -> {
            if (routed[0]) return;
            routed[0] = true;
            openHome(null, null, null);
        };
        handler.postDelayed(giveUp, PROFILE_TIMEOUT_MILLIS);
        profiles.loadOrCreate(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onProfile(@NonNull User user) {
                if (routed[0]) return;
                routed[0] = true;
                handler.removeCallbacks(giveUp);
                openHome(user.getName(), user.getUsername(), user.getEmail());
            }

            @Override
            public void onError(@NonNull String message) {
                // Offline or a permission problem must not leave the user staring at the splash screen forever.
                if (routed[0]) return;
                routed[0] = true;
                handler.removeCallbacks(giveUp);
                openHome(null, null, null);
            }
        });
    }

    private void openHome(@Nullable String name, @Nullable String username, @Nullable String email) {
        if (!isAdded()) return;
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("username", username);
        bundle.putString("email", email);
        navController.navigate(R.id.action_splashFragment_to_homeFragment, bundle);
    }
}
