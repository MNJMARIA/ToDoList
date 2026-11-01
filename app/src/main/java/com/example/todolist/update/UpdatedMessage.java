package com.example.todolist.update;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import com.example.todolist.BuildConfig;
import com.example.todolist.R;
import com.example.todolist.core.UpdatePolicy;

/**
 * "The app was updated" message. Shown once, on the first launch after an upgrade, with the release
 * notes of the version the user chose to install (if they came through the in-app updater).
 */
public final class UpdatedMessage {

    private UpdatedMessage() {
    }

    public static void showIfNeeded(Activity activity) {
        UpdatePrefs prefs = new UpdatePrefs(activity);
        int lastSeen = prefs.lastSeenVersionCode();
        int current = BuildConfig.VERSION_CODE;
        // Record the running version first: whatever happens below, the message must never repeat.
        prefs.setLastSeenVersionCode(current);

        if (!UpdatePolicy.shouldShowUpdatedMessage(lastSeen, current)) {
            return;
        }
        String notes = prefs.pendingNotesFor(BuildConfig.VERSION_NAME);
        prefs.clearPendingNotes();

        String message = activity.getString(R.string.app_updated_text, BuildConfig.VERSION_NAME);
        if (!notes.isEmpty()) {
            message = message + "\n\n" + activity.getString(R.string.whats_new) + "\n" + notes;
        }
        if (activity.isFinishing() || activity.isDestroyed()) return;
        new AlertDialog.Builder(activity)
                .setTitle(R.string.app_updated_title)
                .setMessage(message)
                .setPositiveButton(R.string.got_it, null)
                .show();
    }
}
