package com.example.todolist.core;

/** The parts of a GitHub release the updater cares about. Immutable. */
public final class ReleaseInfo {

    public final String tagName;
    public final String name;
    public final String body;
    public final String htmlUrl;
    public final boolean draft;
    public final boolean prerelease;
    /** Name of the APK asset, or {@code null} if the release has none. */
    public final String apkName;
    public final String apkUrl;
    /** Size in bytes, or 0 if unknown. */
    public final long apkSize;
    /** Lower-case hex SHA-256 published by GitHub for the APK, or {@code null} if unknown. */
    public final String sha256;

    public ReleaseInfo(String tagName, String name, String body, String htmlUrl,
                       boolean draft, boolean prerelease,
                       String apkName, String apkUrl, long apkSize, String sha256) {
        this.tagName = tagName;
        this.name = name == null ? "" : name;
        this.body = body == null ? "" : body;
        this.htmlUrl = htmlUrl == null ? "" : htmlUrl;
        this.draft = draft;
        this.prerelease = prerelease;
        this.apkName = apkName;
        this.apkUrl = apkUrl;
        this.apkSize = apkSize;
        this.sha256 = sha256;
    }

    public boolean hasApk() {
        return apkUrl != null && !apkUrl.isEmpty();
    }

    /** Tag without the leading "v", e.g. {@code v1.2.0} becomes {@code 1.2.0}. */
    public String versionName() {
        String t = tagName == null ? "" : tagName.trim();
        if (t.startsWith("v") || t.startsWith("V")) t = t.substring(1);
        return t;
    }
}
