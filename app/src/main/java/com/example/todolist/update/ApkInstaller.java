package com.example.todolist.update;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.content.pm.PackageInfoCompat;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/** Checks a downloaded APK against the installed app and hands it to the system installer. */
final class ApkInstaller {

    enum Result { STARTED, NEEDS_PERMISSION }

    private ApkInstaller() {
    }

    /**
     * Android only installs an update that has the same package name, a higher version and the same
     * signing key. Checking this first turns a cryptic "App not installed" into a clear message and
     * stops a swapped file before it reaches the installer.
     */
    static void verify(Context context, File apk) throws UpdateException {
        PackageManager pm = context.getPackageManager();
        int flags = signatureFlag();
        PackageInfo archive = apk.exists() ? pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags) : null;
        if (archive == null) {
            throw new UpdateException(UpdateException.Kind.INVALID_APK, "The file is not a readable APK");
        }
        if (!context.getPackageName().equals(archive.packageName)) {
            throw new UpdateException(UpdateException.Kind.INVALID_APK, "The APK belongs to another app");
        }
        PackageInfo installed;
        try {
            installed = pm.getPackageInfo(context.getPackageName(), flags);
        } catch (PackageManager.NameNotFoundException e) {
            throw new UpdateException(UpdateException.Kind.INVALID_APK, "Installed app not found", e);
        }
        if (PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)) {
            throw new UpdateException(UpdateException.Kind.NOT_NEWER, "The APK is not newer than the installed app");
        }
        Set<String> newSigners = signerHashes(archive);
        Set<String> oldSigners = signerHashes(installed);
        if (newSigners.isEmpty() || !newSigners.equals(oldSigners)) {
            throw new UpdateException(UpdateException.Kind.SIGNATURE,
                    "The APK is signed with a different key than the installed app");
        }
    }

    /** Opens the system package installer for the verified file. */
    static Result install(Context context, File apk) throws UpdateException {
        if (!context.getPackageManager().canRequestPackageInstalls()) {
            return Result.NEEDS_PERMISSION;
        }
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apk);
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (RuntimeException e) {
            throw new UpdateException(UpdateException.Kind.INVALID_APK, "No installer available", e);
        }
        return Result.STARTED;
    }

    /** Screen where the user allows this app to install packages ("Install unknown apps"). */
    static Intent unknownSourcesSettings(Context context) {
        return new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.getPackageName()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @SuppressWarnings("deprecation")
    private static int signatureFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? PackageManager.GET_SIGNING_CERTIFICATES
                : PackageManager.GET_SIGNATURES;
    }

    /** SHA-256 of every signing certificate, as hex strings. */
    @SuppressWarnings("deprecation")
    private static Set<String> signerHashes(PackageInfo info) {
        Set<String> hashes = new HashSet<>();
        Signature[] signatures = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            SigningInfo signingInfo = info.signingInfo;
            if (signingInfo != null) {
                signatures = signingInfo.hasMultipleSigners()
                        ? signingInfo.getApkContentsSigners()
                        : signingInfo.getSigningCertificateHistory();
            }
        } else {
            signatures = info.signatures;
        }
        if (signatures == null) return hashes;
        for (Signature signature : signatures) {
            String hash = sha256(signature.toByteArray());
            if (hash != null) hashes.add(hash);
        }
        return hashes;
    }

    @Nullable
    private static String sha256(byte[] data) {
        try {
            return com.example.todolist.core.Checksums.sha256Hex(data);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
