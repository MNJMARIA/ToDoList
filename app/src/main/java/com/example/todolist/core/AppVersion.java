package com.example.todolist.core;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Semantic-version style comparison for release tags such as {@code v1.2.3}, {@code 1.2},
 * {@code 1.2.0-beta.1}. Missing numeric segments count as zero, pre-releases sort before the
 * release they belong to, build metadata ({@code +45}) is ignored.
 */
public final class AppVersion implements Comparable<AppVersion> {

    private static final Pattern CORE = Pattern.compile("\\d{1,9}(\\.\\d{1,9}){0,3}");
    private static final Pattern IDENTIFIER = Pattern.compile("[0-9A-Za-z-]+");
    private static final Pattern NUMERIC = Pattern.compile("\\d+");

    private final int[] numbers;     // always 4 entries
    private final int printedSegments;
    private final String[] preRelease; // null for a normal release

    private AppVersion(int[] numbers, int printedSegments, String[] preRelease) {
        this.numbers = numbers;
        this.printedSegments = printedSegments;
        this.preRelease = preRelease;
    }

    /** @return the parsed version, or {@code null} when the text is not a version. */
    public static AppVersion parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        int plus = s.indexOf('+');
        if (plus >= 0) s = s.substring(0, plus);

        String core = s;
        String[] pre = null;
        int dash = s.indexOf('-');
        if (dash >= 0) {
            core = s.substring(0, dash);
            String preText = s.substring(dash + 1);
            if (preText.isEmpty()) return null;
            pre = preText.split("\\.", -1);
            for (String id : pre) {
                if (!IDENTIFIER.matcher(id).matches()) return null;
            }
        }
        if (!CORE.matcher(core).matches()) return null;

        String[] segments = core.split("\\.");
        int[] numbers = new int[4];
        for (int i = 0; i < segments.length; i++) {
            numbers[i] = Integer.parseInt(segments[i]);
        }
        return new AppVersion(numbers, Math.max(3, segments.length), pre);
    }

    public boolean isPreRelease() {
        return preRelease != null;
    }

    /** True only if both texts are versions and {@code remote} is strictly higher than {@code local}. */
    public static boolean isNewer(String remote, String local) {
        AppVersion r = parse(remote);
        AppVersion l = parse(local);
        return r != null && l != null && r.compareTo(l) > 0;
    }

    @Override
    public int compareTo(AppVersion other) {
        for (int i = 0; i < numbers.length; i++) {
            if (numbers[i] != other.numbers[i]) return numbers[i] < other.numbers[i] ? -1 : 1;
        }
        if (preRelease == null && other.preRelease == null) return 0;
        if (preRelease == null) return 1;
        if (other.preRelease == null) return -1;

        int common = Math.min(preRelease.length, other.preRelease.length);
        for (int i = 0; i < common; i++) {
            int c = compareIdentifier(preRelease[i], other.preRelease[i]);
            if (c != 0) return c;
        }
        return Integer.compare(preRelease.length, other.preRelease.length);
    }

    private static int compareIdentifier(String a, String b) {
        boolean aNum = NUMERIC.matcher(a).matches();
        boolean bNum = NUMERIC.matcher(b).matches();
        if (aNum && bNum) {
            String x = stripLeadingZeros(a);
            String y = stripLeadingZeros(b);
            if (x.length() != y.length()) return x.length() < y.length() ? -1 : 1;
            return Integer.signum(x.compareTo(y));
        }
        if (aNum) return -1;
        if (bNum) return 1;
        return Integer.signum(a.compareTo(b));
    }

    private static String stripLeadingZeros(String digits) {
        int i = 0;
        while (i < digits.length() - 1 && digits.charAt(i) == '0') i++;
        return digits.substring(i);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AppVersion && compareTo((AppVersion) o) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(numbers) + (preRelease == null ? 0 : Arrays.hashCode(preRelease));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < printedSegments; i++) {
            if (i > 0) sb.append('.');
            sb.append(numbers[i]);
        }
        if (preRelease != null) sb.append('-').append(String.join(".", preRelease));
        return sb.toString();
    }
}
