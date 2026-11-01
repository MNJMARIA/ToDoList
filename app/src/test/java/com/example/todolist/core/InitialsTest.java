package com.example.todolist.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InitialsTest {

    @Test
    public void twoWordsGiveTwoLetters() {
        assertEquals("MS", Initials.of("Mariia Stepanova", "x@y.com"));
        assertEquals("МС", Initials.of("марія степанова", ""));
    }

    @Test
    public void oneWordGivesOneLetter() {
        assertEquals("M", Initials.of("mariia", "x@y.com"));
    }

    @Test
    public void firstAndLastWordAreUsed() {
        assertEquals("AB", Initials.of("Anna Maria Boyko", null));
    }

    @Test
    public void extraSpacesAndSeparatorsAreIgnored() {
        assertEquals("AB", Initials.of("  anna   boyko ", null));
        assertEquals("AB", Initials.of("anna-boyko", null));
    }

    @Test
    public void emailIsUsedWhenThereIsNoName() {
        assertEquals("S", Initials.of("", "stepa.maria@gmail.com"));
        assertEquals("T", Initials.of(null, "test@x.com"));
    }

    @Test
    public void nothingAtAllGivesAQuestionMark() {
        assertEquals("?", Initials.of(null, null));
        assertEquals("?", Initials.of("  ", ""));
        assertEquals("?", Initials.of("!!!", ""));
    }

    @Test
    public void nonLatinScriptsAndSurrogatePairsAreKeptWhole() {
        assertEquals("山", Initials.of("山田", null));
        assertEquals("𝐀", Initials.of("𝐀bc", null)); // a letter outside the BMP
    }
}
