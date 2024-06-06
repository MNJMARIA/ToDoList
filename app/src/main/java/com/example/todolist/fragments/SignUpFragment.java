package com.example.todolist.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentSignUpBinding;
import com.example.todolist.utils.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpFragment extends Fragment {
    private NavController navController;
    private FirebaseAuth mAuth;
    private FragmentSignUpBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);

        binding.textViewSignIn.setOnClickListener(v -> navController.navigate(R.id.action_signUpFragment_to_signInFragment));
        binding.nextBtn.setOnClickListener(v -> {
            String name = binding.nameEt.getText().toString();
            String username = binding.usernameEt.getText().toString();
            String email = binding.emailEt.getText().toString();
            String password = binding.passEt.getText().toString();
            String verifyPass = binding.verifyPassEt.getText().toString();

            if (!name.isEmpty() && !username.isEmpty() && !email.isEmpty() && !password.isEmpty() && !verifyPass.isEmpty()) {
                if (password.equals(verifyPass)) {
                    registerUser(name, username, email, password);
                } else {
                    Toast.makeText(getContext(), R.string.password_is_not_the_same, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), R.string.empty_fields_are_not_allowed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(String name, String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = mAuth.getCurrentUser().getUid();
                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
                User user = new User(userId, name, username, email, password);

                usersRef.child(userId).setValue(user).addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful()) {
                        Log.d("SignUpFragment", "Дані користувача збережено успішно!");
                        navController.navigate(R.id.action_signUpFragment_to_homeFragment);
                    } else {
                        Log.e("SignUpFragment", "Error saving user data: " + userTask.getException().getMessage());
                        Toast.makeText(getContext(), R.string.error_saving_user_data, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e("SignUpFragment", "User registration error: " + task.getException().getMessage());
                Toast.makeText(getContext(), R.string.user_registration_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void init(View view) {
        navController = Navigation.findNavController(view);
        mAuth = FirebaseAuth.getInstance();
    }
}