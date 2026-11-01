package com.example.todolist.update;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/** Small persistent state of the updater (excluded from backups, see backup_rules.xml). */
final class UpdatePrefs {

    private static final String PREFS = "update_prefs";
    private static final String KEY_LAST_CHECK = "last_check_millis";
    private static final String KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code";
    private static final String KEY_PENDING_VERSION = "pending_notes_version";
    private static final String KEY_PENDING_NOTES = "pending_notes";

    private final SharedPreferences prefs;
    private final Context context;

    UpdatePrefs(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    long lastCheckMillis() {
        return prefs.getLong(KEY_LAST_CHECK, 0);
    }

    void setLastCheckMillis(long millis) {
        prefs.edit().putLong(KEY_LAST_CHECK, millis).apply();
    }

    /**
     * The version code the user last ran, or a small positive number when this is an upgrade from a
     * build that did not record it (the very first updater-enabled release), so that upgrade is
     * announced too. 0 means a fresh install.
     */
    int lastSeenVersionCode() {
        int stored = prefs.getInt(KEY_LAST_SEEN_VERSION_CODE, 0);
        if (stored > 0) return stored;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            // Replaced after it was first installed, yet nothing recorded: it came from an older build.
            if (info.lastUpdateTime - info.firstInstallTime > 1000) return 1;
        } catch (PackageManager.NameNotFoundException ignored) {
            // cannot happen for our own package
        }
        return 0;
    }

    void setLastSeenVersionCode(int versionCode) {
        prefs.edit().putInt(KEY_LAST_SEEN_VERSION_CODE, versionCode).apply();
    }

    /** Keeps the release notes of the version being installed so the first launch after it can show them. */
    void rememberPendingNotes(String versionName, String notes) {
        prefs.edit().putString(KEY_PENDING_VERSION, versionName).putString(KEY_PENDING_NOTES, notes).apply();
    }

    /** Notes saved for {@code versionName}, or an empty string. */
    String pendingNotesFor(String versionName) {
        if (versionName != null && versionName.equals(prefs.getString(KEY_PENDING_VERSION, null))) {
            return prefs.getString(KEY_PENDING_NOTES, "");
        }
        return "";
    }

    void clearPendingNotes() {
        prefs.edit().remove(KEY_PENDING_VERSION).remove(KEY_PENDING_NOTES).apply();
    }
}
