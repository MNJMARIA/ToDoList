package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InputValidatorTest {

    @Test
    public void validEmails() {
        assertTrue(InputValidator.isValidEmail("a@b.co"));
        assertTrue(InputValidator.isValidEmail("user.name+tag@example.com"));
        assertTrue(InputValidator.isValidEmail("first_last@sub.domain.org"));
        assertTrue(InputValidator.isValidEmail("  padded@example.com  "));
    }

    @Test
    public void invalidEmails() {
        assertFalse(InputValidator.isValidEmail(null));
        assertFalse(InputValidator.isValidEmail(""));
        assertFalse(InputValidator.isValidEmail("plain"));
        assertFalse(InputValidator.isValidEmail("a@b"));
        assertFalse(InputValidator.isValidEmail("@b.com"));
        assertFalse(InputValidator.isValidEmail("a@.com"));
        assertFalse(InputValidator.isValidEmail("a@b..com"));
        assertFalse(InputValidator.isValidEmail("a b@c.com"));
        assertFalse(InputValidator.isValidEmail("a@b.com."));
    }

    @Test
    public void passwordMustHaveSixCharacters() {
        assertFalse(InputValidator.isValidPassword(null));
        assertFalse(InputValidator.isValidPassword(""));
        assertFalse(InputValidator.isValidPassword("12345"));
        assertTrue(InputValidator.isValidPassword("123456"));
        assertTrue(InputValidator.isValidPassword("correct horse battery"));
    }

    @Test
    public void passwordIsNotTrimmedButAllWhitespaceIsRejected() {
        assertTrue(InputValidator.isValidPassword("  pw  x"));
        assertFalse(InputValidator.isValidPassword("       "));
    }

    @Test
    public void normalizeTitle_trimsAndCollapsesWhitespace() {
        assertEquals("buy milk", InputValidator.normalizeTitle("  buy   milk \n"));
        assertEquals("a b c", InputValidator.normalizeTitle("a\t b\n\nc"));
        assertEquals("", InputValidator.normalizeTitle(null));
        assertEquals("", InputValidator.normalizeTitle("   \n "));
    }

    @Test
    public void normalizeTitle_capsLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) sb.append('x');
        assertEquals(InputValidator.MAX_TITLE_LENGTH, InputValidator.normalizeTitle(sb.toString()).length());
    }

    @Test
    public void isBlank() {
        assertTrue(InputValidator.isBlank(null));
        assertTrue(InputValidator.isBlank(""));
        assertTrue(InputValidator.isBlank(" \t\n"));
        assertFalse(InputValidator.isBlank(" x "));
    }
}
