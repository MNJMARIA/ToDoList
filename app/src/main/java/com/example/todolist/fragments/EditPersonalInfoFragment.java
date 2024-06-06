package com.example.todolist.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentEditPersonalInfoBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditPersonalInfoFragment extends Fragment {
    private FragmentEditPersonalInfoBinding binding;
    private FirebaseUser user;
    private DatabaseReference databaseReference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditPersonalInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserDataFromFirebase();
        binding.saveButton.setOnClickListener(v -> saveUserInfo());
        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void loadUserDataFromFirebase() {
        String userId = user.getUid();
        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String username = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String password = snapshot.child("password").getValue(String.class);

                    binding.nameEditText.setText(name);
                    binding.usernameEditText.setText(username);
                    binding.emailEditText.setText(email);
                    binding.passwordEditText.setText(password);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), getString(R.string.error_downloading_data), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInfo() {
        String name = binding.nameEditText.getText().toString().trim();
        String username = binding.usernameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(requireContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), getString(R.string.invalid_email_address), Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(requireContext(), getString(R.string.password_too_short), Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        databaseReference.child(userId).child("name").setValue(name);
        databaseReference.child(userId).child("username").setValue(username);
        databaseReference.child(userId).child("password").setValue(password);
        databaseReference.child(userId).child("email").setValue(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(requireContext(), getString(R.string.data_successfully_saved), Toast.LENGTH_SHORT).show();

                Bundle result = new Bundle();
                result.putString("name", name);
                result.putString("username", username);
                result.putString("email", email);
                result.putString("password", password);
                getParentFragmentManager().setFragmentResult("userDataKey", result);

                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(requireContext(), R.string.error_saving_data, Toast.LENGTH_SHORT).show();
            }
        });
    }
}