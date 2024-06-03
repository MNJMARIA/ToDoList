package com.example.todolist.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.todolist.R;
import android.widget.Toast;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.todolist.databinding.FragmentSignInBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SignInFragment extends Fragment {
    private NavController navController;
    private FirebaseAuth mAuth;
    private FragmentSignInBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSignInBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);

        binding.textViewSignUp.setOnClickListener(v ->
                navController.navigate(R.id.action_signInFragment_to_signUpFragment));

        binding.nextBtn.setOnClickListener(v -> {
            String email = binding.emailEt.getText().toString();
            String pass = binding.passEt.getText().toString();

            if (!email.isEmpty() && !pass.isEmpty())
                loginUser(email, pass);
            else
                Toast.makeText(getContext(), R.string.empty_fields_are_not_allowed, Toast.LENGTH_SHORT).show();
        });
    }

    private void loginUser(String email, String pass) {
        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);

                    usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String name = dataSnapshot.child("name").getValue(String.class);
                                String username = dataSnapshot.child("username").getValue(String.class);
                                String email = dataSnapshot.child("email").getValue(String.class);
                                String password = dataSnapshot.child("password").getValue(String.class);

                                Bundle bundle = new Bundle();
                                bundle.putString("name", name);
                                bundle.putString("username", username);
                                bundle.putString("email", email);
                                bundle.putString("password", password);

                                navController.navigate(R.id.action_signInFragment_to_homeFragment, bundle);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(getContext(), getString(R.string.failed_to_load_user_data), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(getContext(), getString(R.string.authentication_failed) + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void init(View view) {
        navController = Navigation.findNavController(view);
        mAuth = FirebaseAuth.getInstance();
    }
}