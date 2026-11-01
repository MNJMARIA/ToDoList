package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReleaseNotesTest {

    @Test
    public void nullAndBlankBecomeEmpty() {
        assertEquals("", ReleaseNotes.toPlainText(null, 500));
        assertEquals("", ReleaseNotes.toPlainText("   \n\n ", 500));
    }

    @Test
    public void stripsHeadingMarkers() {
        assertEquals("What's new", ReleaseNotes.toPlainText("## What's new", 500));
        assertEquals("Title", ReleaseNotes.toPlainText("# Title", 500));
    }

    @Test
    public void convertsBullets() {
        assertEquals("• one\n• two\n• three", ReleaseNotes.toPlainText("- one\n* two\n+ three", 500));
    }

    @Test
    public void stripsEmphasisAndInlineCode() {
        assertEquals("Fixed the crash in Settings", ReleaseNotes.toPlainText("Fixed the **crash** in `Settings`", 500));
        assertEquals("italic text", ReleaseNotes.toPlainText("__italic__ text", 500));
    }

    @Test
    public void replacesLinksWithTheirText() {
        assertEquals("See the guide",
                ReleaseNotes.toPlainText("See [the guide](https://example.com/guide)", 500));
    }

    @Test
    public void normalizesLineEndingsAndBlankLines() {
        assertEquals("a\n\nb", ReleaseNotes.toPlainText("a\r\n\r\n\r\n\r\nb", 500));
    }

    @Test
    public void truncatesWithEllipsisWithinLimit() {
        String longText = "word ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) sb.append(longText);
        String out = ReleaseNotes.toPlainText(sb.toString(), 50);
        assertTrue(out.length() <= 50);
        assertTrue(out.endsWith("…"));
    }

    @Test
    public void shortTextIsNotTruncated() {
        String out = ReleaseNotes.toPlainText("short", 50);
        assertEquals("short", out);
        assertFalse(out.endsWith("…"));
    }

    @Test
    public void nonPositiveLimitGivesEmpty() {
        assertEquals("", ReleaseNotes.toPlainText("anything", 0));
        assertEquals("", ReleaseNotes.toPlainText("anything", -5));
    }
}
