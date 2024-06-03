package com.example.todolist.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentContactUsBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


//TODO email sending through another service
public class ContactUsFragment extends Fragment {
    private FragmentContactUsBinding binding;
    private NavController navController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_us, container, false);

        // Retrieve references to the views
        final EditText messageEditText = view.findViewById(R.id.message);
        Button sendMessageButton = view.findViewById(R.id.sendMessage);

        // Set click listener for the send message button
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve the message entered by the user
                String message = messageEditText.getText().toString().trim();
                // Check if the message field is empty
                if (message.isEmpty()) {
                    showToast("Please fill in the message field");
                    return;
                }
                // Send email to default address
                sendEmailToDefaultAddress(message);
                // Clear the message field
                messageEditText.setText("");
                // Notify user
                showToast("Message sent successfully");
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get NavController from NavHostFragment
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        binding = FragmentContactUsBinding.bind(view);
        binding.backButton.setOnClickListener(v -> {
            // Close the current fragment
            requireActivity().onBackPressed();
        });
    }
    private void sendEmailToDefaultAddress(String message) {
        // Default email address
        String defaultEmailAddress = "stepanovamasha70@gmail.com";
        // Create intent to send email
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{defaultEmailAddress});
        intent.putExtra(Intent.EXTRA_SUBJECT, "User Feedback");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setType("message/rfc822");
        // Start email activity
        startActivity(Intent.createChooser(intent, "Choose an Email client :"));
    }
    private void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}