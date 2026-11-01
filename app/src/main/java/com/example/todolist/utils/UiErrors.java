package com.example.todolist.utils;

import android.content.Context;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.example.todolist.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Shows database/network problems in a dialog the user cannot miss. A Toast disappears in two seconds,
 * which made "the task was not saved" look like "nothing happened". The text of the underlying error is
 * included so that a screenshot is enough to find the cause.
 */
public final class UiErrors {

    /** Several listeners often fail together (tasks, score, stats): one dialog is enough. */
    private static final long QUIET_MILLIS = 8_000;
    private static long lastShown = -QUIET_MILLIS;

    private UiErrors() {
    }

    /**
     * @param titleRes  what the app was trying to do ("Could not save the task")
     * @param technical the message from Firebase (may be null)
     */
    public static void show(@Nullable Context context, @StringRes int titleRes, @Nullable String technical) {
        if (context == null) return;
        report(context, titleRes, technical);
        long now = SystemClock.elapsedRealtime();
        if (now - lastShown < QUIET_MILLIS) return;
        lastShown = now;

        String detail = technical == null ? "" : technical.trim();
        String explanation = isPermissionDenied(detail)
                ? context.getString(R.string.error_permission_denied)
                : context.getString(R.string.error_db_generic);
        String message = detail.isEmpty() ? explanation : explanation + "\n\n" + detail;
        try {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(titleRes)
                    .setMessage(message)
                    .setPositiveButton(R.string.got_it, null)
                    .show();
        } catch (RuntimeException e) {
            // The screen is already gone (bad window token): nothing to show it on.
        }
    }

    /**
     * Also sends the failure to Crashlytics as a non-fatal error, so a problem like "permission denied" is
     * visible in the console even when the user never mentions it. Reported before the quiet-period check:
     * the dialog is throttled, the log is not.
     */
    private static void report(Context context, @StringRes int titleRes, @Nullable String technical) {
        try {
            FirebaseCrashlytics.getInstance().recordException(
                    new RuntimeException(context.getString(titleRes) + ": " + technical));
        } catch (RuntimeException ignored) {
            // Reporting must never hide the dialog.
        }
    }

    static boolean isPermissionDenied(String message) {
        return message != null && message.toLowerCase(java.util.Locale.ROOT).contains("permission");
    }
}
