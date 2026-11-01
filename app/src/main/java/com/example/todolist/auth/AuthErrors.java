package com.example.todolist.auth;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.example.todolist.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/** Turns a Firebase Authentication failure into a message the user can act on. */
public final class AuthErrors {

    private AuthErrors() {
    }

    /** @param fallback shown for anything that is not recognised */
    @StringRes
    public static int messageFor(@Nullable Exception e, @StringRes int fallback) {
        if (e instanceof FirebaseAuthUserCollisionException) return R.string.auth_email_in_use;
        // Must come before FirebaseAuthInvalidCredentialsException: it is a subclass of it.
        if (e instanceof FirebaseAuthWeakPasswordException) return R.string.password_too_short;
        if (e instanceof FirebaseAuthInvalidCredentialsException
                || e instanceof FirebaseAuthInvalidUserException) {
            return R.string.auth_wrong_credentials;
        }
        if (e instanceof FirebaseNetworkException) return R.string.error_no_network;
        if (e instanceof FirebaseTooManyRequestsException) return R.string.auth_too_many_requests;
        return fallback;
    }

    /**
     * Message for a failed "send password reset" call. An unknown address is deliberately reported like a
     * success by the caller (see {@link #isUnknownAccount}), so nobody can probe which emails are registered.
     */
    @StringRes
    public static int resetMessageFor(@Nullable Exception e) {
        if (e instanceof FirebaseNetworkException) return R.string.error_no_network;
        if (e instanceof FirebaseTooManyRequestsException) return R.string.auth_too_many_requests;
        if (e instanceof FirebaseAuthInvalidCredentialsException) return R.string.invalid_email_address;
        return R.string.reset_failed;
    }

    public static boolean isUnknownAccount(@Nullable Exception e) {
        return e instanceof FirebaseAuthInvalidUserException;
    }
}
