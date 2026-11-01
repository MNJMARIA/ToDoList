package com.example.todolist.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.Locale;

/** Turns the JSON of {@code GET /repos/{owner}/{repo}/releases/latest} into a {@link ReleaseInfo}. */
public final class ReleaseParser {

    private ReleaseParser() {
    }

    /** @throws IllegalArgumentException if the text is not a release object (HTML error page, rate-limit message, ...). */
    public static ReleaseInfo parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty release response");
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Release response is not valid JSON", e);
        }
        if (root == null || !root.isJsonObject()) {
            throw new IllegalArgumentException("Release response is not a JSON object");
        }
        JsonObject obj = root.getAsJsonObject();

        String tag = string(obj, "tag_name");
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Release response has no tag_name");
        }

        String apkName = null;
        String apkUrl = null;
        long apkSize = 0;
        String sha256 = null;
        boolean bestIsDebug = true;

        JsonElement assetsEl = obj.get("assets");
        if (assetsEl != null && assetsEl.isJsonArray()) {
            JsonArray assets = assetsEl.getAsJsonArray();
            for (JsonElement el : assets) {
                if (!el.isJsonObject()) continue;
                JsonObject asset = el.getAsJsonObject();
                String name = string(asset, "name");
                String url = string(asset, "browser_download_url");
                String state = string(asset, "state");
                if (name == null || url == null) continue;
                if (!name.toLowerCase(Locale.ROOT).endsWith(".apk")) continue;
                if (state != null && !"uploaded".equals(state)) continue;

                boolean isDebug = name.toLowerCase(Locale.ROOT).contains("debug");
                boolean better = apkUrl == null || (bestIsDebug && !isDebug);
                if (!better) continue;

                apkName = name;
                apkUrl = url;
                apkSize = number(asset, "size");
                sha256 = sha256FromDigest(string(asset, "digest"));
                bestIsDebug = isDebug;
            }
        }

        return new ReleaseInfo(
                tag,
                string(obj, "name"),
                string(obj, "body"),
                string(obj, "html_url"),
                bool(obj, "draft"),
                bool(obj, "prerelease"),
                apkName, apkUrl, apkSize, sha256);
    }

    private static String sha256FromDigest(String digest) {
        if (digest == null) return null;
        String d = digest.trim();
        if (!d.regionMatches(true, 0, "sha256:", 0, 7)) return null;
        String hex = d.substring(7).trim().toLowerCase(Locale.ROOT);
        return hex.isEmpty() ? null : hex;
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) return null;
        return e.getAsString();
    }

    private static boolean bool(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() && e.getAsBoolean();
    }

    private static long number(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return 0;
        return e.getAsLong();
    }
}
