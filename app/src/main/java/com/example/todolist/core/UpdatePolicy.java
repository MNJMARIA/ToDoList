package com.example.todolist.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Pure decisions of the self-updater: is there something to install, when to look, what to trust. */
public final class UpdatePolicy {

    private UpdatePolicy() {
    }

    /** Only finished, non-draft, non-pre-release releases that carry an APK and are newer than the installed version. */
    public static boolean isUpdateAvailable(String localVersionName, ReleaseInfo release) {
        return release != null
                && !release.draft
                && !release.prerelease
                && release.hasApk()
                && AppVersion.isNewer(release.tagName, localVersionName);
    }

    /** Automatic checks are rate limited; a clock that moved backwards also triggers a check. */
    public static boolean shouldAutoCheck(long lastCheckMillis, long nowMillis, long minIntervalMillis) {
        return lastCheckMillis <= 0
                || nowMillis < lastCheckMillis
                || nowMillis - lastCheckMillis >= minIntervalMillis;
    }

    /** Show "app updated" only after a real upgrade, not on first install, same version or downgrade. */
    public static boolean shouldShowUpdatedMessage(int lastSeenVersionCode, int currentVersionCode) {
        return lastSeenVersionCode > 0 && currentVersionCode > lastSeenVersionCode;
    }

    /**
     * Only APKs hosted as release assets of the configured repository over HTTPS are downloaded.
     * Rejects other hosts, look-alike hosts, user-info tricks, other ports, other repositories and
     * path traversal.
     */
    public static boolean isTrustedDownloadUrl(String url, String owner, String repo) {
        if (url == null || owner == null || repo == null || owner.isEmpty() || repo.isEmpty()) return false;
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
        if (uri.getUserInfo() != null) return false;
        if (uri.getHost() == null || !"github.com".equalsIgnoreCase(uri.getHost())) return false;
        if (uri.getPort() != -1 && uri.getPort() != 443) return false;

        String path = uri.getPath();
        if (path == null) return false;
        for (String segment : path.split("/", -1)) {
            if (segment.equals("..") || segment.equals(".")) return false;
        }
        String prefix = "/" + owner + "/" + repo + "/releases/download/";
        return path.length() > prefix.length()
                && path.regionMatches(true, 0, prefix, 0, prefix.length())
                && !path.toLowerCase(Locale.ROOT).endsWith("/");
    }
}
