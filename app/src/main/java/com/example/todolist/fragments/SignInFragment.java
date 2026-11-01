package com.example.todolist.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.todolist.R;
import com.example.todolist.auth.AuthErrors;
import com.example.todolist.auth.GoogleSignInHelper;
import com.example.todolist.core.InputValidator;
import com.example.todolist.core.ProfileNames;
import com.example.todolist.data.UserProfileRepository;
import com.example.todolist.databinding.FragmentSignInBinding;
import com.example.todolist.utils.adapter.EmailSuggestionAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInFragment extends Fragment {
    /** Firebase also rate-limits this; the pause just stops accidental repeated taps. */
    private static final long RESET_COOLDOWN_MILLIS = 30_000;
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

        // After "@" the common providers are offered, so the address need not be typed to the end.
        b.emailEt.setAdapter(new EmailSuggestionAdapter(requireContext()));

        b.textViewSignUp.setOnClickListener(v ->
                navController.navigate(R.id.action_signInFragment_to_signUpFragment));
        b.googleButton.setOnClickListener(v -> signInWithGoogle());
        b.forgotPasswordTv.setOnClickListener(v -> sendPasswordReset());

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
                toast(AuthErrors.messageFor(task.getException(), R.string.authentication_failed));
                return;
            }
            // Signed in: go straight to the main screen. The profile (name for the menu header) is loaded
            // there, so a slow or unreachable database cannot keep the user on this screen.
            openHome(null, null, email);
        });
    }

    /** Sends Firebase's reset email to the address typed above, then pauses the link so it cannot be spammed. */
    private void sendPasswordReset() {
        FragmentSignInBinding b = binding;
        if (b == null) return;
        String email = textOf(b.emailEt.getText()).trim();
        if (InputValidator.isBlank(email)) {
            toast(R.string.reset_enter_email);
            b.emailEt.requestFocus();
            return;
        }
        if (!InputValidator.isValidEmail(email)) {
            toast(R.string.invalid_email_address);
            return;
        }
        b.forgotPasswordTv.setEnabled(false);
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (!isAdded()) return;
            Exception e = task.getException();
            if (task.isSuccessful() || AuthErrors.isUnknownAccount(e)) {
                // Same text for known and unknown addresses: the app must not reveal who is registered.
                Toast.makeText(requireContext(), getString(R.string.reset_email_sent, email), Toast.LENGTH_LONG).show();
                FragmentSignInBinding current = binding;
                if (current != null) {
                    current.forgotPasswordTv.postDelayed(() -> {
                        FragmentSignInBinding again = binding;
                        if (again != null) again.forgotPasswordTv.setEnabled(true);
                    }, RESET_COOLDOWN_MILLIS);
                }
            } else {
                FragmentSignInBinding current = binding;
                if (current != null) current.forgotPasswordTv.setEnabled(true);
                toast(AuthErrors.resetMessageFor(e));
            }
        });
    }

    private void signInWithGoogle() {
        setBusy(true);
        GoogleSignInHelper.start(requireActivity(), new GoogleSignInHelper.Callback() {
            @Override
            public void onSignedIn(@NonNull FirebaseUser user, boolean isNewUser, @Nullable String displayName) {
                if (!isAdded()) return;
                String email = user.getEmail() == null ? "" : user.getEmail();
                String name = ProfileNames.displayName(displayName, email);
                String username = ProfileNames.username(email);
                UserProfileRepository profiles = UserProfileRepository.forCurrentUser();
                // A brand-new Google account has no profile record yet; create it in the background.
                if (profiles != null && isNewUser) profiles.createIfMissing(name, username);
                openHome(isNewUser ? name : null, isNewUser ? username : null, email);
            }

            @Override
            public void onCancelled() {
                setBusy(false);
            }

            @Override
            public void onError(@StringRes int messageRes, @Nullable String detail) {
                setBusy(false);
                showGoogleError(messageRes, detail);
            }
        });
    }

    private void showGoogleError(@StringRes int messageRes, @Nullable String detail) {
        if (!isAdded()) return;
        String text = getString(messageRes);
        if (detail != null && !detail.isEmpty() && messageRes == R.string.google_failed) {
            text = text + "\n" + detail;
        }
        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
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
        if (b == null) return;
        b.nextBtn.setEnabled(!busy);
        b.googleButton.setEnabled(!busy);
    }

    private void toast(@StringRes int resId) {
        if (isAdded()) Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG).show();
    }

    private static String textOf(@Nullable CharSequence text) {
        return text == null ? "" : text.toString();
    }
}
