package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class TaskCategoryTest {

    @Test
    public void keysAreUniqueAndLowerCase() {
        Set<String> seen = new HashSet<>();
        for (TaskCategory c : TaskCategory.values()) {
            assertEquals(c.key().toLowerCase(java.util.Locale.ROOT), c.key());
            assertTrue("duplicate key " + c.key(), seen.add(c.key()));
        }
    }

    @Test
    public void keyRoundTrips() {
        for (TaskCategory c : TaskCategory.values()) assertEquals(c, TaskCategory.fromStored(c.key()));
    }

    @Test
    public void legacyLabelsOfEveryLanguageAreRecognised() {
        assertEquals(TaskCategory.WORK, TaskCategory.fromStored("Work"));
        assertEquals(TaskCategory.WORK, TaskCategory.fromStored("Робота"));
        assertEquals(TaskCategory.WORK, TaskCategory.fromStored("Travail"));
        assertEquals(TaskCategory.WORK, TaskCategory.fromStored("Arbeit"));
        assertEquals(TaskCategory.WORK, TaskCategory.fromStored("仕事"));
        assertEquals(TaskCategory.WORK, TaskCategory.fromStored("작업"));
        assertEquals(TaskCategory.PERSONAL, TaskCategory.fromStored("Особисті справи"));
        assertEquals(TaskCategory.STUDY, TaskCategory.fromStored("Études"));
        assertEquals(TaskCategory.HEALTH, TaskCategory.fromStored("Gesundheit"));
        assertEquals(TaskCategory.FAMILY, TaskCategory.fromStored("家族"));
        assertEquals(TaskCategory.HOBBY, TaskCategory.fromStored("취미"));
        assertEquals(TaskCategory.HOBBY, TaskCategory.fromStored("Loisir"));
    }

    @Test
    public void apostropheVariantsMatch() {
        assertEquals(TaskCategory.HEALTH, TaskCategory.fromStored("Здоров'я"));
        assertEquals(TaskCategory.HEALTH, TaskCategory.fromStored("Здоров’я"));
        assertEquals(TaskCategory.FAMILY, TaskCategory.fromStored("Сімʼя"));
    }

    @Test
    public void caseAndSurroundingSpacesAreIgnored() {
        assertEquals(TaskCategory.STUDY, TaskCategory.fromStored("  STUDY "));
        assertEquals(TaskCategory.STUDY, TaskCategory.fromStored("навчання"));
    }

    @Test
    public void everyLabelMapsBackToItsOwnCategory() {
        // A label shared by two categories would silently map to the later one.
        String[][] labels = {
                {"work", "Work", "Робота", "Travail", "Arbeit", "仕事", "작업"},
                {"personal", "Personal", "Особисті справи", "Personnel", "Persönlich", "個人", "개인"},
                {"study", "Study", "Навчання", "Études", "Studium", "勉強", "공부"},
                {"health", "Health", "Здоров'я", "Santé", "Gesundheit", "健康", "건강"},
                {"family", "Family", "Сім'я", "Famille", "Familie", "家族", "가족"},
                {"hobby", "Hobby", "Хобі", "Loisir", "Hobby", "趣味", "취미"}};
        assertEquals(TaskCategory.values().length, labels.length);
        for (int row = 0; row < labels.length; row++) {
            for (String label : labels[row]) {
                assertEquals(label, TaskCategory.values()[row], TaskCategory.fromStored(label));
            }
        }
    }

    @Test
    public void freeTextEmptyAndNullAreNotBuiltIn() {
        assertNull(TaskCategory.fromStored("Подорожі"));
        assertNull(TaskCategory.fromStored(""));
        assertNull(TaskCategory.fromStored(null));
        assertTrue(TaskCategory.isCustom("Подорожі"));
        assertFalse(TaskCategory.isCustom(""));
        assertFalse(TaskCategory.isCustom("   "));
        assertFalse(TaskCategory.isCustom(null));
        assertFalse(TaskCategory.isCustom("Work"));
    }

    @Test
    public void normalizeMigratesBuiltInsAndKeepsCustomText() {
        assertEquals("work", TaskCategory.normalize("Робота"));
        assertEquals("health", TaskCategory.normalize("health"));
        assertEquals("Подорожі", TaskCategory.normalize("  Подорожі "));
        assertEquals("", TaskCategory.normalize(null));
        assertEquals("", TaskCategory.normalize("  "));
    }

    @Test
    public void normalizeIsIdempotent() {
        for (String s : new String[]{"Робота", "Study", "Custom", "", "家族"}) {
            assertEquals(TaskCategory.normalize(s), TaskCategory.normalize(TaskCategory.normalize(s)));
        }
    }
}
