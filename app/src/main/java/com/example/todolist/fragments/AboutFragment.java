package com.example.todolist.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentAboutBinding;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class AboutFragment extends Fragment {
    private FragmentAboutBinding binding;
    private NavController navController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        binding = FragmentAboutBinding.bind(view);
        binding.backButton.setOnClickListener(v -> {
            // Закриваємо поточний фрагмент
            requireActivity().onBackPressed();
        });
    }
}