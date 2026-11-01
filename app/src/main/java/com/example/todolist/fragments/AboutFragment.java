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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
