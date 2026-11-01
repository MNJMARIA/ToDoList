package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DbProbeTest {

    @Test
    public void noHttpAnswerMeansUnreachable() {
        assertEquals(DbProbe.Verdict.UNREACHABLE, DbProbe.diagnose(-1, null));
        assertEquals(DbProbe.Verdict.UNREACHABLE, DbProbe.diagnose(-1, "anything"));
    }

    @Test
    public void okAnswerMeansOpenAccess() {
        assertEquals(DbProbe.Verdict.OPEN_ACCESS, DbProbe.diagnose(200, "null"));
        assertEquals(DbProbe.Verdict.OPEN_ACCESS, DbProbe.diagnose(200, "{\"Tasks\":true}"));
    }

    @Test
    public void permissionDeniedMeansRulesAreActive() {
        assertEquals(DbProbe.Verdict.RULES_DENY, DbProbe.diagnose(401, "{\"error\":\"Permission denied\"}"));
        assertEquals(DbProbe.Verdict.RULES_DENY, DbProbe.diagnose(403, ""));
        assertEquals(DbProbe.Verdict.RULES_DENY, DbProbe.diagnose(400, "Permission denied"));
    }

    @Test
    public void missingDatabaseIsRecognisedByStatusOrText() {
        assertEquals(DbProbe.Verdict.NO_DATABASE, DbProbe.diagnose(404, ""));
        assertEquals(DbProbe.Verdict.NO_DATABASE, DbProbe.diagnose(404, null));
        assertEquals(DbProbe.Verdict.NO_DATABASE, DbProbe.diagnose(400,
                "Firebase error. Please ensure that you have the URL of your Firebase Realtime Database instance configured correctly."));
    }

    @Test
    public void deactivatedDatabaseIsRecognisedWhateverTheStatus() {
        String body = "{\"error\" : \"The Firebase database 'todolist-3e189-default-rtdb' has been deactivated.\"}";
        assertEquals(DbProbe.Verdict.DEACTIVATED, DbProbe.diagnose(423, body));
        assertEquals(DbProbe.Verdict.DEACTIVATED, DbProbe.diagnose(404, body));
        assertEquals(DbProbe.Verdict.DEACTIVATED, DbProbe.diagnose(400, body));
    }

    @Test
    public void otherRegionIsRecognisedAndWinsOverTheStatus() {
        String body = "{\"error\":\"Database lives in a different region. Please change your database URL to "
                + "https://todolist-3e189-default-rtdb.us-central1.firebasedatabase.app\"}";
        assertEquals(DbProbe.Verdict.WRONG_REGION, DbProbe.diagnose(301, body));
        assertEquals(DbProbe.Verdict.WRONG_REGION, DbProbe.diagnose(404, body));
        assertEquals("https://todolist-3e189-default-rtdb.us-central1.firebasedatabase.app",
                DbProbe.suggestedUrl(body));
    }

    @Test
    public void matchingIgnoresCase() {
        assertEquals(DbProbe.Verdict.WRONG_REGION, DbProbe.diagnose(301, "DATABASE LIVES IN A DIFFERENT REGION"));
        assertEquals(DbProbe.Verdict.RULES_DENY, DbProbe.diagnose(401, "PERMISSION DENIED"));
    }

    @Test
    public void unexpectedAnswerIsUnknown() {
        assertEquals(DbProbe.Verdict.UNKNOWN, DbProbe.diagnose(500, "boom"));
        assertEquals(DbProbe.Verdict.UNKNOWN, DbProbe.diagnose(302, null));
    }

    @Test
    public void suggestedUrlHandlesOldStyleAddressesAndNothing() {
        assertEquals("https://abc.firebaseio.com", DbProbe.suggestedUrl("use https://abc.firebaseio.com now"));
        assertNull(DbProbe.suggestedUrl("no address here"));
        assertNull(DbProbe.suggestedUrl(null));
    }

    @Test
    public void excerptFlattensAndShortens() {
        assertEquals("a b c", DbProbe.excerpt("a\n  b\tc", 50));
        assertEquals("abcd…", DbProbe.excerpt("abcdefghij", 5));
        assertEquals("", DbProbe.excerpt(null, 5));
    }

    @Test
    public void probeUrlAddsTheJsonEndpoint() {
        assertEquals("https://x.firebasedatabase.app/.json?shallow=true",
                DbProbe.probeUrl("https://x.firebasedatabase.app/"));
        assertEquals("https://x.firebasedatabase.app/.json?shallow=true",
                DbProbe.probeUrl("  https://x.firebasedatabase.app  "));
        assertNull(DbProbe.probeUrl(""));
        assertNull(DbProbe.probeUrl(null));
    }
}
