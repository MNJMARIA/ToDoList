package com.example.todolist.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentContactUsBinding;

public class ContactUsFragment extends Fragment {
    private static final String FEEDBACK_ADDRESS = "stepanovamasha70@gmail.com";

    @Nullable
    private FragmentContactUsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentContactUsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentContactUsBinding b = binding;
        if (b == null) return;
        b.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        b.sendMessage.setOnClickListener(v -> {
            CharSequence text = b.message.getText();
            String message = text == null ? "" : text.toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(requireContext(), R.string.contact_message_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            if (openEmailApp(message)) b.message.setText("");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Hands the text to the user's mail app. This only opens a draft: the message is NOT sent until the
     * user presses send there, so the screen no longer claims "Message sent successfully".
     *
     * @return true if a mail app was opened
     */
    private boolean openEmailApp(String message) {
        // mailto: matches only real mail apps (ACTION_SEND with message/rfc822 also offered Bluetooth, Drive, ...).
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{FEEDBACK_ADDRESS});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.contact_email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, message);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.contact_choose_email_app)));
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.contact_no_email_app, Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
