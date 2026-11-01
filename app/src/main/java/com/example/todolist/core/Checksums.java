package com.example.todolist.core;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** SHA-256 helpers used to verify a downloaded APK against the digest GitHub publishes for the asset. */
public final class Checksums {

    public enum Result { MATCH, MISMATCH, UNAVAILABLE }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Checksums() {
    }

    public static String sha256Hex(byte[] data) {
        MessageDigest md = newDigest();
        return toHex(md.digest(data));
    }

    public static String sha256Hex(InputStream in) throws IOException {
        MessageDigest md = newDigest();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            md.update(buffer, 0, n);
        }
        return toHex(md.digest());
    }

    /**
     * @param expected value as published ("sha256:abcd..." or bare hex, any case) or null/blank if unknown
     * @param actualHex hex computed from the downloaded file
     */
    public static Result verify(String expected, String actualHex) {
        if (expected == null) return Result.UNAVAILABLE;
        String e = expected.trim();
        if (e.regionMatches(true, 0, "sha256:", 0, 7)) e = e.substring(7).trim();
        if (e.isEmpty()) return Result.UNAVAILABLE;
        String a = actualHex == null ? "" : actualHex.trim();
        return e.toLowerCase(Locale.ROOT).equals(a.toLowerCase(Locale.ROOT)) ? Result.MATCH : Result.MISMATCH;
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required on every Java platform", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
