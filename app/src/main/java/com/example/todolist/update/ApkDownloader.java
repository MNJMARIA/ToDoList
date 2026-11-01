package com.example.todolist.update;

import android.content.Context;
import com.example.todolist.BuildConfig;
import com.example.todolist.core.Checksums;
import com.example.todolist.core.ReleaseInfo;
import com.example.todolist.core.UpdatePolicy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Downloads the release APK into the app cache and checks its size and SHA-256. Blocking. */
final class ApkDownloader {

    interface Progress {
        void onProgress(int percent);
    }

    static final String DIR_NAME = "updates";
    static final String FILE_NAME = "update.apk";

    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;
    private static final long SPACE_MARGIN_BYTES = 20L * 1024 * 1024;

    private ApkDownloader() {
    }

    static File updatesDir(Context context) {
        return new File(context.getCacheDir(), DIR_NAME);
    }

    static void deleteDownloads(Context context) {
        File[] files = updatesDir(context).listFiles();
        if (files != null) {
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    /**
     * @return the verified APK file
     * @throws UpdateException on any problem; returns normally only for a complete, matching file.
     *                         When {@code cancelled} is set, throws a NETWORK exception with message "cancelled".
     */
    static File download(Context context, ReleaseInfo release, AtomicBoolean cancelled, Progress progress)
            throws UpdateException {
        if (!release.hasApk()
                || !UpdatePolicy.isTrustedDownloadUrl(release.apkUrl, BuildConfig.GITHUB_OWNER, BuildConfig.GITHUB_REPO)) {
            // Never download from an address the release data merely claims; only this repository's release assets.
            throw new UpdateException(UpdateException.Kind.NO_APK, "No trusted APK in this release");
        }

        File dir = updatesDir(context);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new UpdateException(UpdateException.Kind.NO_SPACE, "Cannot create " + dir);
        }
        deleteDownloads(context);
        if (release.apkSize > 0 && dir.getUsableSpace() < release.apkSize + SPACE_MARGIN_BYTES) {
            throw new UpdateException(UpdateException.Kind.NO_SPACE, "Not enough free storage");
        }

        File part = new File(dir, FILE_NAME + ".part");
        File target = new File(dir, FILE_NAME);
        HttpURLConnection connection = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            connection = (HttpURLConnection) new URL(release.apkUrl).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestProperty("User-Agent", "ToDoList-Android-Updater");
            connection.setRequestProperty("Accept", "application/octet-stream");

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new UpdateException(UpdateException.Kind.BAD_RESPONSE, "Download failed: HTTP " + code);
            }
            long total = connection.getContentLengthLong() > 0 ? connection.getContentLengthLong() : release.apkSize;

            long written = 0;
            int lastPercent = -1;
            try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(part)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (cancelled.get()) {
                        throw new UpdateException(UpdateException.Kind.NETWORK, "cancelled");
                    }
                    out.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                    written += read;
                    if (total > 0) {
                        int percent = (int) Math.min(100, written * 100 / total);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            progress.onProgress(percent);
                        }
                    }
                }
            }

            if (release.apkSize > 0 && written != release.apkSize) {
                throw new UpdateException(UpdateException.Kind.NETWORK,
                        "Incomplete download: " + written + " of " + release.apkSize + " bytes");
            }
            String actual = toHex(digest.digest());
            if (Checksums.verify(release.sha256, actual) == Checksums.Result.MISMATCH) {
                throw new UpdateException(UpdateException.Kind.CHECKSUM, "SHA-256 does not match the published value");
            }
            // UNAVAILABLE (older releases without a digest) is allowed: the signature check still guards the install.
            if (!part.renameTo(target)) {
                throw new UpdateException(UpdateException.Kind.NO_SPACE, "Cannot store the downloaded file");
            }
            return target;
        } catch (IOException e) {
            throw new UpdateException(UpdateException.Kind.NETWORK, "Network error: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new UpdateException(UpdateException.Kind.BAD_RESPONSE, "SHA-256 is not available", e);
        } finally {
            if (connection != null) connection.disconnect();
            if (part.exists()) {
                //noinspection ResultOfMethodCallIgnored
                part.delete(); // after a successful rename it no longer exists; after a failure it is junk
            }
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
