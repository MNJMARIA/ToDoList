package com.example.todolist.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.example.todolist.R;
import com.example.todolist.core.DbProbe;
import com.example.todolist.data.ConnectionMonitor;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.FirebaseDatabase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Why does it say there is no connection?" Asks the database server directly over HTTPS and explains the
 * answer: the database does not exist at the configured address, it lives in another region, the rules
 * refuse access, or the phone cannot reach it at all.
 */
public final class ConnectionDiagnostics {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int TIMEOUT_MILLIS = 8000;
    private static final int MAX_BODY_CHARS = 600;

    private ConnectionDiagnostics() {
    }

    public static void show(@Nullable Context context) {
        if (context == null) return;
        final String databaseUrl = databaseUrl();
        final boolean online = isOnline(context);
        final String intro = intro(context, databaseUrl, online);

        AlertDialog dialog;
        try {
            dialog = new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.diag_title)
                    .setMessage(intro + "\n\n" + context.getString(R.string.diag_checking))
                    .setPositiveButton(R.string.got_it, null)
                    .setNeutralButton(R.string.diag_reconnect, (d, which) -> ConnectionMonitor.reconnect())
                    .show();
        } catch (RuntimeException e) {
            return; // the screen is already gone
        }
        final AlertDialog shown = dialog;
        final Context appContext = context.getApplicationContext();

        IO.execute(() -> {
            Answer answer = ask(DbProbe.probeUrl(databaseUrl));
            final String text = intro + "\n\n" + describe(appContext, answer);
            MAIN.post(() -> {
                if (shown.isShowing()) shown.setMessage(text);
            });
        });
    }

    // ------------------------------------------------------------------ pieces

    @Nullable
    private static String databaseUrl() {
        try {
            return FirebaseDatabase.getInstance().getReference().toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network network = cm.getActiveNetwork();
            NetworkCapabilities caps = network == null ? null : cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String intro(Context context, @Nullable String databaseUrl, boolean online) {
        String address = databaseUrl == null ? "?" : databaseUrl;
        return context.getString(R.string.diag_server, address) + "\n"
                + context.getString(online ? R.string.diag_network_ok : R.string.diag_network_off);
    }

    private static final class Answer {
        final int status;
        final String body;
        final DbProbe.Verdict verdict;

        Answer(int status, String body) {
            this.status = status;
            this.body = body;
            this.verdict = DbProbe.diagnose(status, body);
        }
    }

    private static Answer ask(@Nullable String url) {
        if (url == null) return new Answer(-1, "no database address");
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(false); // a "wrong region" answer must be seen, not followed
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new Answer(status, read(stream));
        } catch (IOException | RuntimeException e) {
            return new Answer(-1, e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String read(@Nullable InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[512];
            int n;
            while (sb.length() < MAX_BODY_CHARS && (n = reader.read(buffer)) > 0) sb.append(buffer, 0, n);
        }
        return sb.toString();
    }

    @NonNull
    private static String describe(Context context, Answer answer) {
        String technical = context.getString(R.string.diag_technical,
                answer.status < 0 ? "-" : String.valueOf(answer.status),
                DbProbe.excerpt(answer.body, 220));
        String meaning;
        switch (answer.verdict) {
            case OPEN_ACCESS:
                meaning = context.getString(R.string.diag_open);
                // The shallow listing is only used to see that the server answers; its content is not shown.
                technical = context.getString(R.string.diag_technical, String.valueOf(answer.status), "OK");
                break;
            case RULES_DENY:
                meaning = context.getString(R.string.diag_rules);
                break;
            case NO_DATABASE:
                meaning = context.getString(R.string.diag_no_db);
                break;
            case DEACTIVATED:
                meaning = context.getString(R.string.diag_deactivated);
                break;
            case WRONG_REGION:
                String suggested = DbProbe.suggestedUrl(answer.body);
                meaning = context.getString(R.string.diag_region, suggested == null ? "?" : suggested);
                break;
            case UNREACHABLE:
                meaning = context.getString(R.string.diag_unreachable);
                break;
            case UNKNOWN:
            default:
                meaning = context.getString(R.string.diag_unknown);
                break;
        }
        return meaning + "\n\n" + technical;
    }
}
