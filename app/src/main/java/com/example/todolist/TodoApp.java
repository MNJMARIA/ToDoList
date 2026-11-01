package com.example.todolist;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import com.example.todolist.data.ConnectionMonitor;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Turns on the Realtime Database disk cache. With it a task created without a connection is kept on the
 * phone and sent to the server later, and the score/tasks of the last session are shown immediately
 * at start instead of an empty screen until the server answers.
 */
public class TodoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Must run before the first use of the database, which is why it lives here and not in an Activity.
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (RuntimeException ignored) {
            // Already initialised (e.g. by a receiver in the same process): keep the default, online-only mode.
        }
        // Start watching the connection once, for the whole app (the banner and the "saved on the phone" hint use it).
        ConnectionMonitor.start();
        enableCrashReports();
    }

    /**
     * Crash reports (stack trace, device model, Android version, app version; no account data) go to the
     * Firebase console. Off for debug builds, see the manifest.
     */
    private void enableCrashReports() {
        boolean debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (debuggable) return;
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        } catch (RuntimeException ignored) {
            // Reporting is a convenience; never let it stop the app from starting.
        }
    }
}
