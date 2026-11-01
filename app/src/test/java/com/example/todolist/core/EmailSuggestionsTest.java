package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class EmailSuggestionsTest {

    @Test
    public void afterAtSignOffersTheCommonProviders() {
        List<String> s = EmailSuggestions.suggest("anna@");
        assertEquals(Arrays.asList(
                "anna@gmail.com", "anna@ukr.net", "anna@i.ua", "anna@meta.ua", "anna@icloud.com"), s);
    }

    @Test
    public void typedDomainStartNarrowsTheList() {
        assertEquals(Arrays.asList("anna@gmail.com"), EmailSuggestions.suggest("anna@g"));
        assertEquals(Arrays.asList("anna@outlook.com"), EmailSuggestions.suggest("anna@out"));
        // Order of the suggestions is the order of the provider list (i.ua before icloud.com).
        assertEquals(Arrays.asList("anna@i.ua", "anna@icloud.com"), EmailSuggestions.suggest("anna@i"));
    }

    @Test
    public void domainMatchingIgnoresCase() {
        assertEquals(Arrays.asList("Anna@gmail.com"), EmailSuggestions.suggest("Anna@GM"));
    }

    @Test
    public void nothingBeforeTheAtSign() {
        assertTrue(EmailSuggestions.suggest("anna").isEmpty());
        assertTrue(EmailSuggestions.suggest("").isEmpty());
        assertTrue(EmailSuggestions.suggest(null).isEmpty());
    }

    @Test
    public void emptyNameOrSecondAtSignGivesNothing() {
        assertTrue(EmailSuggestions.suggest("@gmail.com").isEmpty());
        assertTrue(EmailSuggestions.suggest("a@b@").isEmpty());
    }

    @Test
    public void completeDomainNeedsNoHelp() {
        assertTrue(EmailSuggestions.suggest("anna@gmail.com").isEmpty());
    }

    @Test
    public void unknownDomainGivesNothing() {
        assertTrue(EmailSuggestions.suggest("anna@company.org").isEmpty());
    }

    @Test
    public void surroundingSpacesAreIgnoredButSpacesInsideAreNot() {
        assertEquals(Arrays.asList("anna@gmail.com"), EmailSuggestions.suggest("  anna@g "));
        assertTrue(EmailSuggestions.suggest("an na@g").isEmpty());
    }

    @Test
    public void everySuggestionIsAValidAddress() {
        for (String s : EmailSuggestions.suggest("anna.k+todo@")) {
            assertTrue(s, InputValidator.isValidEmail(s));
        }
    }
}
