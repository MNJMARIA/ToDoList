package com.example.todolist.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdatePolicyTest {

    private static final long HOUR = 60L * 60 * 1000;
    private static final long NOW = 1_800_000_000_000L;
    private static final String OWNER = "MNJMARIA";
    private static final String REPO = "ToDoList";

    private static ReleaseInfo release(String tag, boolean draft, boolean prerelease, boolean withApk) {
        String assets = withApk
                ? "{\"name\":\"a.apk\",\"size\":1,\"state\":\"uploaded\",\"browser_download_url\":\"https://github.com/MNJMARIA/ToDoList/releases/download/" + tag + "/a.apk\"}"
                : "";
        return ReleaseParser.parse("{\"tag_name\":\"" + tag + "\",\"draft\":" + draft + ",\"prerelease\":" + prerelease
                + ",\"assets\":[" + assets + "]}");
    }

    // ---------- isUpdateAvailable ----------

    @Test
    public void newerStableReleaseWithApk_isAvailable() {
        assertTrue(UpdatePolicy.isUpdateAvailable("1.0", release("v1.0.1", false, false, true)));
        assertTrue(UpdatePolicy.isUpdateAvailable("1.0.0", release("v2.0.0", false, false, true)));
    }

    @Test
    public void sameOrOlderRelease_isNotAvailable() {
        assertFalse(UpdatePolicy.isUpdateAvailable("1.2.0", release("v1.2.0", false, false, true)));
        assertFalse(UpdatePolicy.isUpdateAvailable("1.2.0", release("v1.1.9", false, false, true)));
    }

    @Test
    public void preReleaseAndDraftAreNeverOffered() {
        assertFalse(UpdatePolicy.isUpdateAvailable("1.0", release("v2.0.0-beta.1", false, true, true)));
        assertFalse(UpdatePolicy.isUpdateAvailable("1.0", release("v2.0.0", true, false, true)));
    }

    @Test
    public void releaseWithoutApk_isNotAvailable() {
        assertFalse(UpdatePolicy.isUpdateAvailable("1.0", release("v2.0.0", false, false, false)));
    }

    @Test
    public void nullReleaseOrBadLocalVersion_isNotAvailable() {
        assertFalse(UpdatePolicy.isUpdateAvailable("1.0", null));
        assertFalse(UpdatePolicy.isUpdateAvailable("dev-build", release("v2.0.0", false, false, true)));
        assertFalse(UpdatePolicy.isUpdateAvailable(null, release("v2.0.0", false, false, true)));
    }

    // ---------- shouldAutoCheck ----------

    @Test
    public void autoCheck_neverCheckedBefore() {
        assertTrue(UpdatePolicy.shouldAutoCheck(0, NOW, 12 * HOUR));
    }

    @Test
    public void autoCheck_respectsInterval() {
        assertFalse(UpdatePolicy.shouldAutoCheck(NOW - HOUR, NOW, 12 * HOUR));
        assertFalse(UpdatePolicy.shouldAutoCheck(NOW - 12 * HOUR + 1, NOW, 12 * HOUR));
        assertTrue(UpdatePolicy.shouldAutoCheck(NOW - 12 * HOUR, NOW, 12 * HOUR));
        assertTrue(UpdatePolicy.shouldAutoCheck(NOW - 30 * HOUR, NOW, 12 * HOUR));
    }

    @Test
    public void autoCheck_clockMovedBackwardsTriggersCheck() {
        assertTrue(UpdatePolicy.shouldAutoCheck(NOW + HOUR, NOW, 12 * HOUR));
    }

    // ---------- shouldShowUpdatedMessage ----------

    @Test
    public void updatedMessage_onlyAfterRealUpgrade() {
        assertTrue(UpdatePolicy.shouldShowUpdatedMessage(5, 6));
        assertTrue(UpdatePolicy.shouldShowUpdatedMessage(1, 10200));
    }

    @Test
    public void updatedMessage_notOnFreshInstallSameVersionOrDowngrade() {
        assertFalse(UpdatePolicy.shouldShowUpdatedMessage(0, 5));
        assertFalse(UpdatePolicy.shouldShowUpdatedMessage(5, 5));
        assertFalse(UpdatePolicy.shouldShowUpdatedMessage(6, 5));
    }

    // ---------- isTrustedDownloadUrl ----------

    @Test
    public void trustedUrl_acceptsOwnReleaseAssets() {
        assertTrue(UpdatePolicy.isTrustedDownloadUrl(
                "https://github.com/MNJMARIA/ToDoList/releases/download/v1.2.0/ToDoList-v1.2.0.apk", OWNER, REPO));
        assertTrue(UpdatePolicy.isTrustedDownloadUrl(
                "https://github.com/mnjmaria/todolist/releases/download/v1.2.0/a.apk", OWNER, REPO));
        assertTrue(UpdatePolicy.isTrustedDownloadUrl(
                "https://github.com:443/MNJMARIA/ToDoList/releases/download/v1/a.apk", OWNER, REPO));
    }

    @Test
    public void trustedUrl_rejectsPlainHttp() {
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(
                "http://github.com/MNJMARIA/ToDoList/releases/download/v1/a.apk", OWNER, REPO));
    }

    @Test
    public void trustedUrl_rejectsOtherHostsAndLookalikes() {
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(
                "https://evil.example/MNJMARIA/ToDoList/releases/download/v1/a.apk", OWNER, REPO));
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(
                "https://github.com.evil.example/MNJMARIA/ToDoList/releases/download/v1/a.apk", OWNER, REPO));
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(
                "https://github.com@evil.example/MNJMARIA/ToDoList/releases/download/v1/a.apk", OWNER, REPO));
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(
                "https://github.com:8443/MNJMARIA/ToDoList/releases/download/v1/a.apk", OWNER, REPO));
    }

    @Test
    public void trustedUrl_rejectsOtherRepositories() {
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(
                "https://github.com/someone/else/releases/download/v1/a.apk", OWNER, REPO));
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(
                "https://github.com/MNJMARIA/ToDoList-fork/releases/download/v1/a.apk", OWNER, REPO));
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(
                "https://github.com/MNJMARIA/ToDoList/raw/main/a.apk", OWNER, REPO));
    }

    @Test
    public void trustedUrl_rejectsGarbage() {
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(null, OWNER, REPO));
        assertFalse(UpdatePolicy.isTrustedDownloadUrl("", OWNER, REPO));
        assertFalse(UpdatePolicy.isTrustedDownloadUrl("not a url", OWNER, REPO));
        assertFalse(UpdatePolicy.isTrustedDownloadUrl("file:///sdcard/a.apk", OWNER, REPO));
        assertFalse(UpdatePolicy.isTrustedDownloadUrl("https://github.com/MNJMARIA/ToDoList/releases/download/v1/a.apk", null, REPO));
    }

    @Test
    public void trustedUrl_rejectsPathTraversal() {
        assertFalse(UpdatePolicy.isTrustedDownloadUrl(
                "https://github.com/MNJMARIA/ToDoList/releases/download/../../../../evil/repo/a.apk", OWNER, REPO));
    }
}
