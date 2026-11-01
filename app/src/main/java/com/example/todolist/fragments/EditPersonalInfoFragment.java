package com.example.todolist.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.todolist.R;
import com.example.todolist.core.InputValidator;
import com.example.todolist.data.UserProfileRepository;
import com.example.todolist.databinding.FragmentEditPersonalInfoBinding;
import com.example.todolist.utils.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Edit name and username, and optionally set a new password. The e-mail is shown but not editable:
 * it is the sign-in identity owned by Firebase Authentication, and changing only the copy in the
 * database used to make the two disagree.
 */
public class EditPersonalInfoFragment extends Fragment {
    @Nullable
    private FragmentEditPersonalInfoBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEditPersonalInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentEditPersonalInfoBinding b = binding;
        if (b == null) return;

        b.emailEditText.setEnabled(false);
        b.passwordEditText.setText("");
        b.passwordEditText.setHint(R.string.new_password_optional);

        loadProfile();
        b.saveButton.setOnClickListener(v -> saveUserInfo());
        b.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadProfile() {
        UserProfileRepository profiles = UserProfileRepository.forCurrentUser();
        if (profiles == null) return;
        profiles.loadOrCreate(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onProfile(@NonNull User user) {
                FragmentEditPersonalInfoBinding b = binding;
                if (b == null) return;
                b.nameEditText.setText(user.getName());
                b.usernameEditText.setText(user.getUsername());
                b.emailEditText.setText(user.getEmail());
            }

            @Override
            public void onError(@NonNull String message) {
                toast(R.string.error_downloading_data);
            }
        });
    }

    private void saveUserInfo() {
        FragmentEditPersonalInfoBinding b = binding;
        UserProfileRepository profiles = UserProfileRepository.forCurrentUser();
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (b == null || profiles == null || authUser == null) return;

        String name = textOf(b.nameEditText.getText()).trim();
        String username = textOf(b.usernameEditText.getText()).trim();
        String newPassword = textOf(b.passwordEditText.getText());

        if (InputValidator.isBlank(name) || InputValidator.isBlank(username)) {
            toast(R.string.fill_all_fields);
            return;
        }
        boolean changePassword = !newPassword.isEmpty();
        if (changePassword && !InputValidator.isValidPassword(newPassword)) {
            toast(R.string.password_too_short);
            return;
        }

        b.saveButton.setEnabled(false);
        profiles.saveNames(name, username, new UserProfileRepository.Callback() {
            @Override
            public void onSuccess() {
                if (changePassword) {
                    authUser.updatePassword(newPassword).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            finishSaving();
                        } else {
                            enableSave();
                            toast(R.string.error_saving_data);
                        }
                    });
                } else {
                    finishSaving();
                }
            }

            @Override
            public void onError(@NonNull String message) {
                enableSave();
                toast(R.string.error_saving_data);
            }
        });
    }

    private void finishSaving() {
        if (!isAdded()) return;
        toast(R.string.data_successfully_saved);
        // The settings screen reloads the profile in onResume, so nothing needs to be passed back.
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void enableSave() {
        FragmentEditPersonalInfoBinding b = binding;
        if (b != null) b.saveButton.setEnabled(true);
    }

    private void toast(int resId) {
        if (isAdded()) Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
    }

    private static String textOf(@Nullable CharSequence text) {
        return text == null ? "" : text.toString();
    }
}
