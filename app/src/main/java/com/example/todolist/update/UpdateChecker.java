package com.example.todolist.update;

import com.example.todolist.BuildConfig;
import com.example.todolist.core.ReleaseInfo;
import com.example.todolist.core.ReleaseParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Asks GitHub for the latest published release of the repository. Blocking: call from a background thread. */
final class UpdateChecker {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 15_000;
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    private UpdateChecker() {
    }

    /** @return the latest release, or {@code null} when the repository has no release yet */
    static ReleaseInfo fetchLatest() throws UpdateException {
        String address = "https://api.github.com/repos/" + BuildConfig.GITHUB_OWNER + "/"
                + BuildConfig.GITHUB_REPO + "/releases/latest";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            // GitHub rejects API requests without a User-Agent.
            connection.setRequestProperty("User-Agent", "ToDoList-Android-Updater");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return null; // no releases published yet
            }
            if (code == 403 || code == 429) {
                throw new UpdateException(UpdateException.Kind.RATE_LIMIT, "GitHub API limit reached (HTTP " + code + ")");
            }
            if (code != HttpURLConnection.HTTP_OK) {
                throw new UpdateException(UpdateException.Kind.BAD_RESPONSE, "Unexpected HTTP " + code);
            }
            String body = readLimited(connection.getInputStream());
            try {
                return ReleaseParser.parse(body);
            } catch (IllegalArgumentException e) {
                throw new UpdateException(UpdateException.Kind.BAD_RESPONSE, "Unreadable release data", e);
            }
        } catch (IOException e) {
            throw new UpdateException(UpdateException.Kind.NETWORK, "Network error: " + e.getMessage(), e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readLimited(InputStream in) throws IOException {
        try (InputStream stream = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() > MAX_RESPONSE_BYTES) {
                    throw new IOException("Response too large");
                }
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
