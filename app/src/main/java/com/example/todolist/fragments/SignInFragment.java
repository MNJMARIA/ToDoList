package com.example.todolist.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.todolist.R;
import com.example.todolist.core.InputValidator;
import com.example.todolist.data.UserProfileRepository;
import com.example.todolist.databinding.FragmentSignInBinding;
import com.example.todolist.utils.model.User;
import com.google.firebase.auth.FirebaseAuth;

public class SignInFragment extends Fragment {
    private NavController navController;
    private FirebaseAuth mAuth;
    @Nullable
    private FragmentSignInBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSignInBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentSignInBinding b = binding;
        if (b == null) return;
        navController = Navigation.findNavController(view);
        mAuth = FirebaseAuth.getInstance();

        b.textViewSignUp.setOnClickListener(v ->
                navController.navigate(R.id.action_signInFragment_to_signUpFragment));

        b.nextBtn.setOnClickListener(v -> {
            // Trailing spaces from keyboards' autocomplete are the most common reason for "wrong email".
            String email = textOf(b.emailEt.getText()).trim();
            String pass = textOf(b.passEt.getText());

            if (InputValidator.isBlank(email) || pass.isEmpty()) {
                Toast.makeText(requireContext(), R.string.empty_fields_are_not_allowed, Toast.LENGTH_SHORT).show();
            } else if (!InputValidator.isValidEmail(email)) {
                Toast.makeText(requireContext(), R.string.invalid_email_address, Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, pass);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loginUser(String email, String pass) {
        setBusy(true);
        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (!isAdded()) return;
            if (!task.isSuccessful()) {
                setBusy(false);
                Exception e = task.getException();
                String reason = e == null || e.getMessage() == null ? "" : e.getMessage();
                Toast.makeText(requireContext(), getString(R.string.authentication_failed) + reason, Toast.LENGTH_SHORT).show();
                return;
            }
            UserProfileRepository profiles = UserProfileRepository.forCurrentUser();
            if (profiles == null) {
                setBusy(false);
                return;
            }
            profiles.loadOrCreate(new UserProfileRepository.ProfileCallback() {
                @Override
                public void onProfile(@NonNull User user) {
                    openHome(user.getName(), user.getUsername(), user.getEmail());
                }

                @Override
                public void onError(@NonNull String message) {
                    // Signed in fine; the profile can be loaded later (the drawer reloads it), so do not block the user.
                    openHome(null, null, email);
                }
            });
        });
    }

    private void openHome(@Nullable String name, @Nullable String username, @Nullable String email) {
        if (!isAdded()) return;
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("username", username);
        bundle.putString("email", email);
        navController.navigate(R.id.action_signInFragment_to_homeFragment, bundle);
    }

    private void setBusy(boolean busy) {
        FragmentSignInBinding b = binding;
        if (b != null) b.nextBtn.setEnabled(!busy);
    }

    private static String textOf(@Nullable CharSequence text) {
        return text == null ? "" : text.toString();
    }
}
