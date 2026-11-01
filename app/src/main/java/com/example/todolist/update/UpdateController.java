package com.example.todolist.update;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.todolist.BuildConfig;
import com.example.todolist.core.ReleaseInfo;
import com.example.todolist.core.ReleaseNotes;
import com.example.todolist.core.UpdatePolicy;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One place that owns the whole update flow: check GitHub, download the APK, verify it, hand it to
 * the installer. Screens only observe {@link #getState()} and call the few action methods, so the
 * flow keeps running (and its progress stays visible) when the dialog is closed or rotated.
 *
 * <p>All state changes happen on the main thread; the network work runs on one background thread.
 */
public final class UpdateController {

    public enum Phase {
        IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY, NEEDS_PERMISSION, ERROR
    }

    /** Immutable snapshot of the flow. */
    public static final class State {
        public final Phase phase;
        /** The release being offered/downloaded; null until one is known. */
        @Nullable
        public final ReleaseInfo release;
        /** 0..100 while downloading. */
        public final int percent;
        /** Set when {@code phase == ERROR}. */
        @Nullable
        public final UpdateException.Kind errorKind;
        /** The verified APK when {@code phase == READY} or {@code NEEDS_PERMISSION}. */
        @Nullable
        public final File apk;

        State(Phase phase, @Nullable ReleaseInfo release, int percent,
              @Nullable UpdateException.Kind errorKind, @Nullable File apk) {
            this.phase = phase;
            this.release = release;
            this.percent = percent;
            this.errorKind = errorKind;
            this.apk = apk;
        }
    }

    private static final long AUTO_CHECK_INTERVAL_MILLIS = 24L * 60 * 60 * 1000;
    private static final int NOTES_MAX_CHARS = 1500;

    private static volatile UpdateController instance;

    public static UpdateController get(Context context) {
        UpdateController local = instance;
        if (local == null) {
            synchronized (UpdateController.class) {
                local = instance;
                if (local == null) {
                    local = new UpdateController(context.getApplicationContext());
                    instance = local;
                }
            }
        }
        return local;
    }

    private final Context appContext;
    private final UpdatePrefs prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state =
            new MutableLiveData<>(new State(Phase.IDLE, null, 0, null, null));
    private final LiveData<ReleaseInfo> availableRelease;
    private AtomicBoolean cancelFlag = new AtomicBoolean(false);

    private UpdateController(Context appContext) {
        this.appContext = appContext;
        this.prefs = new UpdatePrefs(appContext);
        // Non-null whenever a newer release is known, whatever step of the flow we are in.
        this.availableRelease = Transformations.map(state, s -> {
            switch (s.phase) {
                case AVAILABLE:
                case DOWNLOADING:
                case READY:
                case NEEDS_PERMISSION:
                case ERROR: // a failed download keeps the offer alive so "retry" is possible
                    return s.release;
                default:
                    return null;
            }
        });
    }

    public LiveData<State> getState() {
        return state;
    }

    public LiveData<ReleaseInfo> getAvailableRelease() {
        return availableRelease;
    }

    private State current() {
        State s = state.getValue();
        return s != null ? s : new State(Phase.IDLE, null, 0, null, null);
    }

    private void post(State next) {
        mainHandler.post(() -> state.setValue(next));
    }

    // ---------------------------------------------------------------- checking

    /** Background check on app start: at most once a day and silent when it fails. */
    public void autoCheck() {
        if (!UpdatePolicy.shouldAutoCheck(prefs.lastCheckMillis(), System.currentTimeMillis(), AUTO_CHECK_INTERVAL_MILLIS)) {
            return;
        }
        check(true);
    }

    /** Looks for a newer release now. A user-requested check ({@code silent == false}) reports failures. */
    public void check(boolean silent) {
        Phase phase = current().phase;
        if (phase == Phase.CHECKING || phase == Phase.DOWNLOADING) return;
        if (phase == Phase.READY || phase == Phase.NEEDS_PERMISSION) return; // already have a verified APK
        State before = current();
        if (!silent) state.setValue(new State(Phase.CHECKING, null, 0, null, null));

        worker.execute(() -> {
            try {
                ReleaseInfo latest = UpdateChecker.fetchLatest();
                prefs.setLastCheckMillis(System.currentTimeMillis());
                if (latest != null && UpdatePolicy.isUpdateAvailable(BuildConfig.VERSION_NAME, latest)) {
                    post(new State(Phase.AVAILABLE, latest, 0, null, null));
                } else {
                    post(new State(Phase.UP_TO_DATE, latest, 0, null, null));
                }
            } catch (UpdateException e) {
                if (silent) {
                    post(before.phase == Phase.AVAILABLE ? before : new State(Phase.IDLE, null, 0, null, null));
                } else {
                    post(new State(Phase.ERROR, null, 0, e.kind, null));
                }
            }
        });
    }

    // ---------------------------------------------------------------- downloading

    /** Downloads and verifies the offered APK. Safe to call again after an error. */
    public void download() {
        State s = current();
        ReleaseInfo release = s.release;
        if (release == null || s.phase == Phase.DOWNLOADING) return;

        cancelFlag = new AtomicBoolean(false);
        AtomicBoolean cancelled = cancelFlag;
        state.setValue(new State(Phase.DOWNLOADING, release, 0, null, null));

        worker.execute(() -> {
            try {
                File apk = ApkDownloader.download(appContext, release, cancelled,
                        percent -> post(new State(Phase.DOWNLOADING, release, percent, null, null)));
                ApkInstaller.verify(appContext, apk);
                prefs.rememberPendingNotes(release.versionName(),
                        ReleaseNotes.toPlainText(release.body, NOTES_MAX_CHARS));
                post(new State(Phase.READY, release, 100, null, apk));
            } catch (UpdateException e) {
                ApkDownloader.deleteDownloads(appContext);
                if (cancelled.get()) {
                    post(new State(Phase.AVAILABLE, release, 0, null, null));
                } else {
                    post(new State(Phase.ERROR, release, 0, e.kind, null));
                }
            }
        });
    }

    public void cancelDownload() {
        if (current().phase == Phase.DOWNLOADING) cancelFlag.set(true);
    }

    // ---------------------------------------------------------------- installing

    /**
     * Opens the system installer for the verified APK.
     *
     * @return false if the user must first allow installing from this app (state becomes NEEDS_PERMISSION)
     */
    public boolean install(Context context) {
        State s = current();
        File apk = s.apk;
        if (apk == null || !apk.exists()) {
            state.setValue(new State(Phase.ERROR, s.release, 0, UpdateException.Kind.INVALID_APK, null));
            return true;
        }
        try {
            if (ApkInstaller.install(context, apk) == ApkInstaller.Result.NEEDS_PERMISSION) {
                state.setValue(new State(Phase.NEEDS_PERMISSION, s.release, 100, null, apk));
                return false;
            }
            return true;
        } catch (UpdateException e) {
            state.setValue(new State(Phase.ERROR, s.release, 0, e.kind, null));
            return true;
        }
    }

    /** After the user came back from the "install unknown apps" screen. */
    public void refreshPermission(Context context) {
        State s = current();
        if (s.phase == Phase.NEEDS_PERMISSION && context.getPackageManager().canRequestPackageInstalls()) {
            state.setValue(new State(Phase.READY, s.release, 100, null, s.apk));
        }
    }

    @NonNull
    public android.content.Intent unknownSourcesSettingsIntent(Context context) {
        return ApkInstaller.unknownSourcesSettings(context);
    }

    /** Back to a neutral state, e.g. when the dialog is dismissed after an error or "up to date". */
    public void resetIfFinished() {
        Phase phase = current().phase;
        if (phase == Phase.UP_TO_DATE || (phase == Phase.ERROR && current().release == null)) {
            state.setValue(new State(Phase.IDLE, null, 0, null, null));
        }
    }
}
