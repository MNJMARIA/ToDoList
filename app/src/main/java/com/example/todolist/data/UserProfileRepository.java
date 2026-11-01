package com.example.todolist.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.todolist.utils.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

/**
 * The profile in {@code Users/<uid>}. Passwords are never stored there: Firebase Authentication owns
 * them. Older versions wrote the password in plain text, so loading a profile also removes that field.
 */
public final class UserProfileRepository {

    public interface ProfileCallback {
        void onProfile(@NonNull User user);

        void onError(@NonNull String message);
    }

    public interface Callback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    private final FirebaseUser authUser;
    private final DatabaseReference ref;

    private UserProfileRepository(FirebaseUser authUser) {
        this.authUser = authUser;
        this.ref = FirebaseDatabase.getInstance().getReference("Users").child(authUser.getUid());
    }

    @Nullable
    public static UserProfileRepository forCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? null : new UserProfileRepository(user);
    }

    /** Reads the profile once; if it is missing (e.g. the sign-up write failed) a basic one is created. */
    public void loadOrCreate(@NonNull ProfileCallback callback) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("password").exists()) {
                    ref.child("password").removeValue();
                }
                String email = authUser.getEmail() == null ? "" : authUser.getEmail();
                if (!snapshot.exists()) {
                    String fallbackName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
                    User created = new User(authUser.getUid(), fallbackName, fallbackName, email);
                    ref.setValue(created);
                    callback.onProfile(created);
                    return;
                }
                User user = new User(
                        authUser.getUid(),
                        valueOrEmpty(snapshot, "name"),
                        valueOrEmpty(snapshot, "username"),
                        email.isEmpty() ? valueOrEmpty(snapshot, "email") : email);
                callback.onProfile(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Creates the profile of a freshly registered account (sign-in with Google has no sign-up form) unless
     * one exists. Fire and forget: the caller does not wait for the server, and a failure only means the
     * profile is created later by {@link #loadOrCreate}.
     */
    public void createIfMissing(@NonNull String name, @NonNull String username) {
        String email = authUser.getEmail() == null ? "" : authUser.getEmail();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    ref.setValue(new User(authUser.getUid(), name, username, email));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Nothing to do: loadOrCreate repairs a missing profile on the next load.
            }
        });
    }

    public void saveNames(@NonNull String name, @NonNull String username, @NonNull Callback callback) {
        Map<String, Object> values = new HashMap<>();
        values.put("name", name);
        values.put("username", username);
        values.put("email", authUser.getEmail() == null ? "" : authUser.getEmail());
        ref.updateChildren(values).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                Exception e = task.getException();
                callback.onError(e == null || e.getMessage() == null ? "Unknown error" : e.getMessage());
            }
        });
    }

    private static String valueOrEmpty(DataSnapshot snapshot, String key) {
        String value = snapshot.child(key).getValue(String.class);
        return value == null ? "" : value;
    }
}
