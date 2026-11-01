package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReleaseParserTest {

    private static final String APK_URL =
            "https://github.com/MNJMARIA/ToDoList/releases/download/v1.2.0/ToDoList-v1.2.0.apk";
    private static final String SHA = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    private static String asset(String name, String state, String digestField) {
        return "{\"name\":\"" + name + "\",\"size\":12345678,\"state\":\"" + state + "\","
                + "\"browser_download_url\":\"https://github.com/MNJMARIA/ToDoList/releases/download/v1.2.0/" + name + "\""
                + digestField + "}";
    }

    private static String release(String tag, boolean draft, boolean prerelease, String assetsJson) {
        return "{\"tag_name\":\"" + tag + "\",\"name\":\"ToDoList 1.2.0\","
                + "\"body\":\"## What's new\\n- Fixed reminders\",\"html_url\":\"https://github.com/MNJMARIA/ToDoList/releases/tag/" + tag + "\","
                + "\"draft\":" + draft + ",\"prerelease\":" + prerelease + ",\"assets\":[" + assetsJson + "]}";
    }

    @Test
    public void parsesTypicalGithubLatestRelease() {
        String json = release("v1.2.0", false, false,
                asset("ToDoList-v1.2.0.apk", "uploaded", ",\"digest\":\"sha256:" + SHA.toUpperCase() + "\"")
                        + "," + asset("test-report.html", "uploaded", ""));
        ReleaseInfo r = ReleaseParser.parse(json);
        assertEquals("v1.2.0", r.tagName);
        assertEquals("1.2.0", r.versionName());
        assertEquals("ToDoList 1.2.0", r.name);
        assertEquals("## What's new\n- Fixed reminders", r.body);
        assertEquals("https://github.com/MNJMARIA/ToDoList/releases/tag/v1.2.0", r.htmlUrl);
        assertFalse(r.draft);
        assertFalse(r.prerelease);
        assertTrue(r.hasApk());
        assertEquals("ToDoList-v1.2.0.apk", r.apkName);
        assertEquals(APK_URL, r.apkUrl);
        assertEquals(12345678L, r.apkSize);
        assertEquals(SHA, r.sha256);
    }

    @Test
    public void releaseWithoutApkAsset() {
        ReleaseInfo r = ReleaseParser.parse(release("v1.2.0", false, false, asset("notes.txt", "uploaded", "")));
        assertFalse(r.hasApk());
        assertNull(r.apkUrl);
    }

    @Test
    public void releaseWithEmptyAssets() {
        assertFalse(ReleaseParser.parse(release("v1.0.0", false, false, "")).hasApk());
    }

    @Test
    public void ignoresChecksumSidecarFiles() {
        String assets = asset("ToDoList-v1.2.0.apk.sha256", "uploaded", "") + "," + asset("ToDoList-v1.2.0.apk", "uploaded", "");
        assertEquals("ToDoList-v1.2.0.apk", ReleaseParser.parse(release("v1.2.0", false, false, assets)).apkName);
    }

    @Test
    public void prefersReleaseApkOverDebugApk() {
        String assets = asset("app-debug.apk", "uploaded", "") + "," + asset("ToDoList-release.apk", "uploaded", "");
        assertEquals("ToDoList-release.apk", ReleaseParser.parse(release("v1.2.0", false, false, assets)).apkName);
    }

    @Test
    public void usesDebugApkWhenItIsTheOnlyOne() {
        assertEquals("app-debug.apk",
                ReleaseParser.parse(release("v1.2.0", false, false, asset("app-debug.apk", "uploaded", ""))).apkName);
    }

    @Test
    public void skipsAssetsThatAreNotFullyUploaded() {
        String assets = asset("half.apk", "starter", "") + "," + asset("ToDoList.apk", "uploaded", "");
        assertEquals("ToDoList.apk", ReleaseParser.parse(release("v1.2.0", false, false, assets)).apkName);
    }

    @Test
    public void apkExtensionIsCaseInsensitive() {
        assertEquals("ToDoList.APK",
                ReleaseParser.parse(release("v1.2.0", false, false, asset("ToDoList.APK", "uploaded", ""))).apkName);
    }

    @Test
    public void missingDigestMeansUnknownChecksum() {
        ReleaseInfo r = ReleaseParser.parse(release("v1.2.0", false, false, asset("a.apk", "uploaded", "")));
        assertNull(r.sha256);
    }

    @Test
    public void unsupportedDigestAlgorithmIsIgnored() {
        ReleaseInfo r = ReleaseParser.parse(
                release("v1.2.0", false, false, asset("a.apk", "uploaded", ",\"digest\":\"sha512:abcdef\"")));
        assertNull(r.sha256);
    }

    @Test
    public void draftAndPrereleaseFlagsAreParsed() {
        ReleaseInfo r = ReleaseParser.parse(release("v1.3.0-beta.1", true, true, asset("a.apk", "uploaded", "")));
        assertTrue(r.draft);
        assertTrue(r.prerelease);
    }

    @Test
    public void nullNameAndBodyBecomeEmptyStrings() {
        ReleaseInfo r = ReleaseParser.parse("{\"tag_name\":\"v1.0.0\",\"name\":null,\"body\":null,\"assets\":null}");
        assertEquals("", r.name);
        assertEquals("", r.body);
        assertFalse(r.hasApk());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidJsonThrows() {
        ReleaseParser.parse("<html>rate limit exceeded</html>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyInputThrows() {
        ReleaseParser.parse("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullInputThrows() {
        ReleaseParser.parse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void jsonArrayInsteadOfObjectThrows() {
        ReleaseParser.parse("[]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void githubErrorObjectWithoutTagThrows() {
        ReleaseParser.parse("{\"message\":\"Not Found\",\"documentation_url\":\"https://docs.github.com\"}");
    }
}
