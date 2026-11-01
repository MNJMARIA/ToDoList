package com.example.todolist.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProfileNamesTest {

    @Test
    public void accountNameWinsWhenPresent() {
        assertEquals("Mariia Stepanova", ProfileNames.displayName("  Mariia   Stepanova ", "m@x.com"));
    }

    @Test
    public void missingNameFallsBackToTheMailPrefix() {
        assertEquals("anna.k", ProfileNames.displayName(null, "anna.k@gmail.com"));
        assertEquals("anna.k", ProfileNames.displayName("   ", "anna.k@gmail.com"));
    }

    @Test
    public void usernameIsTheMailPrefix() {
        assertEquals("anna.k", ProfileNames.username("anna.k@gmail.com"));
        assertEquals("plain", ProfileNames.username("plain"));
        assertEquals("", ProfileNames.username(null));
    }

    @Test
    public void limitsMatchTheSignUpForm() {
        String longName = new String(new char[80]).replace('\0', 'a');
        assertEquals(ProfileNames.MAX_NAME, ProfileNames.displayName(longName, "x@y.com").length());
        assertEquals(ProfileNames.MAX_USERNAME, ProfileNames.username(longName + "@y.com").length());
    }

    @Test
    public void cutNeverSplitsASurrogatePair() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 29; i++) sb.append('a');
        sb.append("😀"); // an emoji: two chars, would end at index 31 > 30
        String cut = ProfileNames.username(sb + "@y.com");
        assertEquals(29, cut.length());
    }
}
