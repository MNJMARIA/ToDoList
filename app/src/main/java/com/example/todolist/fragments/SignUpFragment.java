package com.example.todolist.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.example.todolist.databinding.FragmentSignUpBinding;
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

        b.textViewSignIn.setOnClickListener(v -> navController.navigate(R.id.action_signUpFragment_to_signInFragment));
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
                toast(R.string.user_registration_error);
                return;
            }
            // No password here: Firebase Authentication already stores it, hashed. Storing it again in
            // the database (as older versions did) only exposes it to anyone who can read that node.
            User user = new User(created.getUid(), name, username, email);
            FirebaseDatabase.getInstance().getReference().child("Users").child(created.getUid())
                    .setValue(user).addOnCompleteListener(userTask -> {
                        if (!isAdded()) return;
                        if (!userTask.isSuccessful()) {
                            // The account exists. The profile is recreated on the next load, so do not strand the user.
                            Exception e = userTask.getException();
                            Log.e(TAG, "Error saving user data: " + (e == null ? "unknown" : e.getMessage()));
                            toast(R.string.error_saving_user_data);
                        }
                        Bundle bundle = new Bundle();
                        bundle.putString("name", name);
                        bundle.putString("username", username);
                        bundle.putString("email", email);
                        navController.navigate(R.id.action_signUpFragment_to_homeFragment, bundle);
                    });
        });
    }

    private void setBusy(boolean busy) {
        FragmentSignUpBinding b = binding;
        if (b != null) b.nextBtn.setEnabled(!busy);
    }

    private void toast(int resId) {
        if (isAdded()) Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
    }

    private static String textOf(@Nullable CharSequence text) {
        return text == null ? "" : text.toString();
    }
}
