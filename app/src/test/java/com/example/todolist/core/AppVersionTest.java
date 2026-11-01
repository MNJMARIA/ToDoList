package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppVersionTest {

    private static int cmp(String a, String b) {
        AppVersion x = AppVersion.parse(a);
        AppVersion y = AppVersion.parse(b);
        assertNotNull("parse " + a, x);
        assertNotNull("parse " + b, y);
        return Integer.signum(x.compareTo(y));
    }

    @Test
    public void parse_acceptsCommonTagShapes() {
        assertEquals("1.2.3", AppVersion.parse("1.2.3").toString());
        assertEquals("1.2.3", AppVersion.parse("v1.2.3").toString());
        assertEquals("1.2.3", AppVersion.parse("V1.2.3").toString());
        assertEquals("2.0.0", AppVersion.parse("2").toString());
        assertEquals("1.2.0", AppVersion.parse("1.2").toString());
        assertEquals("1.2.3.4", AppVersion.parse("1.2.3.4").toString());
        assertEquals("1.0.1", AppVersion.parse("  v1.0.1  ").toString());
    }

    @Test
    public void parse_rejectsGarbage() {
        assertNull(AppVersion.parse(null));
        assertNull(AppVersion.parse(""));
        assertNull(AppVersion.parse("v"));
        assertNull(AppVersion.parse("abc"));
        assertNull(AppVersion.parse("1..2"));
        assertNull(AppVersion.parse("1.x"));
        assertNull(AppVersion.parse("1.2.3.4.5"));
        assertNull(AppVersion.parse("-1.0"));
    }

    @Test
    public void compare_isNumericNotLexical() {
        assertEquals(1, cmp("1.10.0", "1.9.0"));
        assertEquals(1, cmp("2.0.0", "1.99.99"));
        assertEquals(-1, cmp("1.0.9", "1.0.10"));
    }

    @Test
    public void compare_missingSegmentsAreZero() {
        assertEquals(0, cmp("1.2", "1.2.0"));
        assertEquals(0, cmp("1", "1.0.0.0"));
        assertEquals(-1, cmp("1.2", "1.2.1"));
    }

    @Test
    public void compare_preReleaseIsLowerThanRelease() {
        assertEquals(-1, cmp("1.2.0-beta", "1.2.0"));
        assertEquals(1, cmp("1.2.0", "1.2.0-rc.1"));
        assertTrue(AppVersion.parse("1.2.0-beta").isPreRelease());
        assertFalse(AppVersion.parse("1.2.0").isPreRelease());
    }

    @Test
    public void compare_preReleaseIdentifiers() {
        assertEquals(-1, cmp("1.2.0-alpha", "1.2.0-beta"));
        assertEquals(-1, cmp("1.2.0-beta.1", "1.2.0-beta.2"));
        assertEquals(1, cmp("1.2.0-beta.11", "1.2.0-beta.2"));
        assertEquals(-1, cmp("1.2.0-beta", "1.2.0-beta.1"));
        assertEquals(-1, cmp("1.2.0-1", "1.2.0-alpha"));
    }

    @Test
    public void compare_buildMetadataIsIgnored() {
        assertEquals(0, cmp("1.2.3+45", "1.2.3"));
        assertEquals(0, cmp("1.2.3+a", "1.2.3+b"));
    }

    @Test
    public void isNewer() {
        assertTrue(AppVersion.isNewer("v1.1.0", "1.0"));
        assertTrue(AppVersion.isNewer("1.0.1", "1.0.0"));
        assertFalse(AppVersion.isNewer("1.0.0", "1.0.0"));
        assertFalse(AppVersion.isNewer("v1.0.0", "1.0"));
        assertFalse(AppVersion.isNewer("0.9", "1.0"));
    }

    @Test
    public void isNewer_isFalseWhenEitherSideIsUnparseable() {
        assertFalse(AppVersion.isNewer("latest", "1.0"));
        assertFalse(AppVersion.isNewer("1.0", "dev"));
        assertFalse(AppVersion.isNewer(null, "1.0"));
        assertFalse(AppVersion.isNewer("1.0", null));
    }
}
