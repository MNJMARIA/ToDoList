package com.example.todolist.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.Test;

public class TaskQueryTest {

    private static final class T {
        final String title, category, date, time;
        final int rank;

        T(String title, String category, int rank, String date, String time) {
            this.title = title;
            this.category = category;
            this.rank = rank;
            this.date = date;
            this.time = time;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private static final TaskQuery.Accessor<T> ACC = new TaskQuery.Accessor<T>() {
        public String title(T t) { return t.title; }
        public String categoryLabel(T t) { return t.category; }
        public int priorityRank(T t) { return t.rank; }
        public String date(T t) { return t.date; }
        public String time(T t) { return t.time; }
    };

    private final T buy = new T("Buy milk", "Personal", 3, "10/05/2030", "09:00");
    private final T report = new T("Quarterly report", "Work", 0, "01/05/2030", "15:00");
    private final T gym = new T("Gym", "Health", 2, "01/05/2030", "08:00");
    private final T noDate = new T("Anything", "", 1, "", "");
    private final List<T> all = Arrays.asList(buy, report, gym, noDate);

    private List<T> run(String q, TaskQuery.Sort s) {
        return TaskQuery.apply(all, q, s, ACC, Locale.ENGLISH);
    }

    @Test
    public void emptyQueryKeepsEverythingInOrder() {
        assertEquals(all, run("", TaskQuery.Sort.DEFAULT));
        assertEquals(all, run(null, TaskQuery.Sort.DEFAULT));
        assertEquals(all, run("   ", null));
    }

    @Test
    public void inputListIsNotModified() {
        List<T> copy = new ArrayList<>(all);
        run("", TaskQuery.Sort.TITLE);
        assertEquals(copy, all);
    }

    @Test
    public void searchesTitleIgnoringCase() {
        assertEquals(Arrays.asList(buy), run("MILK", TaskQuery.Sort.DEFAULT));
        assertEquals(Arrays.asList(report), run("quart", TaskQuery.Sort.DEFAULT));
    }

    @Test
    public void searchesCategoryLabel() {
        assertEquals(Arrays.asList(report), run("work", TaskQuery.Sort.DEFAULT));
        assertEquals(Arrays.asList(gym), run("health", TaskQuery.Sort.DEFAULT));
    }

    @Test
    public void allWordsMustMatchInAnyOrder() {
        assertEquals(Arrays.asList(report), run("report quarterly", TaskQuery.Sort.DEFAULT));
        assertEquals(Arrays.asList(report), run("work report", TaskQuery.Sort.DEFAULT));
        assertEquals(Arrays.<T>asList(), run("milk report", TaskQuery.Sort.DEFAULT));
    }

    @Test
    public void noMatchGivesEmptyList() {
        assertEquals(Arrays.<T>asList(), run("zzz", TaskQuery.Sort.DEFAULT));
    }

    @Test
    public void accentsOnLatinLettersAreIgnored() {
        T t = new T("Révision d'étude", "Études", 0, "01/01/2030", "10:00");
        List<T> r = TaskQuery.apply(Arrays.asList(t), "etude revision", TaskQuery.Sort.DEFAULT, ACC, Locale.FRENCH);
        assertEquals(Arrays.asList(t), r);
        r = TaskQuery.apply(Arrays.asList(t), "ÉTUDE", TaskQuery.Sort.DEFAULT, ACC, Locale.FRENCH);
        assertEquals(Arrays.asList(t), r);
    }

    @Test
    public void ukrainianLettersAreNotMergedByAccentFolding() {
        T t = new T("Йти в магазин", "", 0, "", "");
        assertEquals(Arrays.asList(t), TaskQuery.apply(Arrays.asList(t), "йти", TaskQuery.Sort.DEFAULT, ACC, Locale.ENGLISH));
        assertEquals(Arrays.asList(t), TaskQuery.apply(Arrays.asList(t), "ЙТИ", TaskQuery.Sort.DEFAULT, ACC, Locale.ENGLISH));
        assertEquals(Arrays.<T>asList(), TaskQuery.apply(Arrays.asList(t), "ити", TaskQuery.Sort.DEFAULT, ACC, Locale.ENGLISH));
    }

    @Test
    public void nullFieldsDoNotCrash() {
        T t = new T(null, null, 0, null, null);
        assertEquals(1, TaskQuery.apply(Arrays.asList(t), "", TaskQuery.Sort.TITLE, ACC, Locale.ENGLISH).size());
        assertEquals(0, TaskQuery.apply(Arrays.asList(t), "x", TaskQuery.Sort.DEADLINE, ACC, Locale.ENGLISH).size());
    }

    @Test
    public void sortsByDeadlineWithUnreadableLast() {
        assertEquals(Arrays.asList(gym, report, buy, noDate), run("", TaskQuery.Sort.DEADLINE));
    }

    @Test
    public void sortsByPriorityThenDeadline() {
        T a = new T("A", "", 0, "05/05/2030", "10:00");
        T b = new T("B", "", 0, "04/05/2030", "10:00");
        T c = new T("C", "", -1, "01/01/2030", "10:00");   // unknown priority: last
        T d = new T("D", "", 3, "01/01/2030", "10:00");
        List<T> r = TaskQuery.apply(Arrays.asList(a, c, d, b), "", TaskQuery.Sort.PRIORITY, ACC, Locale.ENGLISH);
        assertEquals(Arrays.asList(b, a, d, c), r);
    }

    @Test
    public void sortsByTitleIgnoringCaseAndSpaces() {
        T x = new T("  banana", "", 0, "", "");
        T y = new T("Apple", "", 0, "", "");
        T z = new T("cherry", "", 0, "", "");
        List<T> r = TaskQuery.apply(Arrays.asList(z, x, y), "", TaskQuery.Sort.TITLE, ACC, Locale.ENGLISH);
        assertEquals(Arrays.asList(y, x, z), r);
    }

    @Test
    public void ukrainianTitlesFollowTheAlphabet() {
        T a = new T("Яблука", "", 0, "", "");
        T b = new T("Їжа", "", 0, "", "");
        T c = new T("Борщ", "", 0, "", "");
        List<T> r = TaskQuery.apply(Arrays.asList(a, b, c), "", TaskQuery.Sort.TITLE, ACC, new Locale("uk"));
        assertEquals(Arrays.asList(c, b, a), r);
    }

    @Test
    public void searchAndSortCombine() {
        T w1 = new T("Work call", "Work", 1, "03/05/2030", "10:00");
        T w2 = new T("Work email", "Work", 0, "09/05/2030", "10:00");
        List<T> r = TaskQuery.apply(Arrays.asList(w1, buy, w2), "work", TaskQuery.Sort.PRIORITY, ACC, Locale.ENGLISH);
        assertEquals(Arrays.asList(w2, w1), r);
    }

    @Test
    public void sortIsStableForEqualItems() {
        T a = new T("same", "", 0, "01/01/2030", "10:00");
        T b = new T("same", "", 0, "01/01/2030", "10:00");
        assertEquals(Arrays.asList(a, b), TaskQuery.apply(Arrays.asList(a, b), "", TaskQuery.Sort.DEADLINE, ACC, Locale.ENGLISH));
        assertEquals(Arrays.asList(b, a), TaskQuery.apply(Arrays.asList(b, a), "", TaskQuery.Sort.TITLE, ACC, Locale.ENGLISH));
    }
}
