package com.example.todolist.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.example.todolist.databinding.FragmentSignUpBinding;
import com.example.todolist.utils.adapter.EmailSuggestionAdapter;
import com.example.todolist.utils.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpFragment extends Fragment {
    private static final String TAG = "SignUpFragment";

    private NavController navController;
    private FirebaseAuth mAuth;
    @Nullable
    private FragmentSignUpBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentSignUpBinding b = binding;
        if (b == null) return;
        navController = Navigation.findNavController(view);
        mAuth = FirebaseAuth.getInstance();

        // After "@" the common providers are offered, so the address need not be typed to the end.
        b.emailEt.setAdapter(new EmailSuggestionAdapter(requireContext()));

        b.textViewSignIn.setOnClickListener(v -> navController.navigate(R.id.action_signUpFragment_to_signInFragment));
        b.googleButton.setOnClickListener(v -> signUpWithGoogle());
        b.nextBtn.setOnClickListener(v -> {
            String name = textOf(b.nameEt.getText()).trim();
            String username = textOf(b.usernameEt.getText()).trim();
            String email = textOf(b.emailEt.getText()).trim();
            String password = textOf(b.passEt.getText());
            String verifyPass = textOf(b.verifyPassEt.getText());

            if (InputValidator.isBlank(name) || InputValidator.isBlank(username) || InputValidator.isBlank(email)
                    || password.isEmpty() || verifyPass.isEmpty()) {
                toast(R.string.empty_fields_are_not_allowed);
            } else if (!InputValidator.isValidEmail(email)) {
                toast(R.string.invalid_email_address);
            } else if (!InputValidator.isValidPassword(password)) {
                // Firebase rejects short passwords anyway, but with a message the user cannot read.
                toast(R.string.password_too_short);
            } else if (!password.equals(verifyPass)) {
                toast(R.string.password_is_not_the_same);
            } else {
                registerUser(name, username, email, password);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void registerUser(String name, String username, String email, String password) {
        setBusy(true);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!isAdded()) return;
            FirebaseUser created = mAuth.getCurrentUser();
            if (!task.isSuccessful() || created == null) {
                setBusy(false);
                Exception e = task.getException();
                Log.e(TAG, "User registration error: " + (e == null ? "unknown" : e.getMessage()));
                toast(AuthErrors.messageFor(e, R.string.user_registration_error));
                return;
            }
            // No password here: Firebase Authentication already stores it, hashed. Storing it again in
            // the database (as older versions did) only exposes it to anyone who can read that node.
            User user = new User(created.getUid(), name, username, email);
            FirebaseDatabase.getInstance().getReference().child("Users").child(created.getUid())
                    .setValue(user).addOnCompleteListener(userTask -> {
                        if (!userTask.isSuccessful()) {
                            // The account exists. The profile is recreated on the next load, so nothing is lost.
                            Exception e = userTask.getException();
                            Log.e(TAG, "Error saving user data: " + (e == null ? "unknown" : e.getMessage()));
                        }
                    });
            // The account is created: go to the main screen right away. Waiting for the profile write to be
            // confirmed left people on this screen, and a second tap then reported "already registered".
            openHome(name, username, email);
        });
    }

    private void signUpWithGoogle() {
        setBusy(true);
        GoogleSignInHelper.start(requireActivity(), new GoogleSignInHelper.Callback() {
            @Override
            public void onSignedIn(@NonNull FirebaseUser user, boolean isNewUser, @Nullable String displayName) {
                if (!isAdded()) return;
                String email = user.getEmail() == null ? "" : user.getEmail();
                String name = ProfileNames.displayName(displayName, email);
                String username = ProfileNames.username(email);
                UserProfileRepository profiles = UserProfileRepository.forCurrentUser();
                // The same button works for an existing account: the profile is only created when missing.
                if (profiles != null) profiles.createIfMissing(name, username);
                openHome(name, username, email);
            }

            @Override
            public void onCancelled() {
                setBusy(false);
            }

            @Override
            public void onError(@StringRes int messageRes, @Nullable String detail) {
                setBusy(false);
                if (!isAdded()) return;
                String text = getString(messageRes);
                if (detail != null && !detail.isEmpty() && messageRes == R.string.google_failed) {
                    text = text + "\n" + detail;
                }
                Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openHome(String name, String username, String email) {
        if (!isAdded()) return;
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("username", username);
        bundle.putString("email", email);
        navController.navigate(R.id.action_signUpFragment_to_homeFragment, bundle);
    }

    private void setBusy(boolean busy) {
        FragmentSignUpBinding b = binding;
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
