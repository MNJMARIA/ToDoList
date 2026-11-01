package com.example.todolist.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.todolist.BuildConfig;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentAboutBinding;
import com.example.todolist.update.UpdateDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AboutFragment extends Fragment {
    @Nullable
    private FragmentAboutBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentAboutBinding b = binding;
        if (b == null) return;
        b.aboutVersion.setText(getString(R.string.version_format, BuildConfig.VERSION_NAME));
        b.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        b.checkUpdateButton.setOnClickListener(v -> UpdateDialogFragment.show(getChildFragmentManager()));
        // Hidden maintenance switch: a long press on the version line triggers a test crash, which is how
        // the Firebase console is told that Crashlytics works (its setup is not complete before a first crash).
        b.aboutVersion.setOnLongClickListener(v -> {
            confirmTestCrash();
            return true;
        });
    }

    private void confirmTestCrash() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.crash_test_title)
                .setMessage(R.string.crash_test_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.crash_test_confirm, (dialog, which) -> {
                    throw new RuntimeException("Crashlytics test crash (started by the user from the About screen)");
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
