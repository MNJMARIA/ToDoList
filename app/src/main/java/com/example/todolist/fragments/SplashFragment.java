package com.example.todolist.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.todolist.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

public class SplashFragment extends Fragment {
    private FirebaseAuth mAuth;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);

        final boolean isLogin = mAuth.getCurrentUser() != null;

        Handler handler = new Handler(Looper.myLooper());
        handler.postDelayed(() -> {
            if (isLogin) {
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

                                Bundle bundle = new Bundle();
                                bundle.putString("name", name);
                                bundle.putString("username", username);
                                bundle.putString("email", email);

                                navController.navigate(R.id.action_splashFragment_to_homeFragment, bundle);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(getContext(), R.string.failed_to_load_user_data, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                navController.navigate(R.id.action_splashFragment_to_signInFragment);
            }
        }, 2000);
    }

    private void init(View view) {
        mAuth = FirebaseAuth.getInstance();
        navController = Navigation.findNavController(view);
    }
}