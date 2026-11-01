package com.example.todolist.core;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.Test;

public class ChecksumsTest {

    private static final String ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private static final String EMPTY = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Test
    public void sha256_knownVectors() {
        assertEquals(ABC, Checksums.sha256Hex("abc".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(EMPTY, Checksums.sha256Hex(new byte[0]));
    }

    @Test
    public void sha256_streamMatchesBytesForLargeInput() throws IOException {
        byte[] data = new byte[100_000];
        new Random(42).nextBytes(data);
        assertEquals(Checksums.sha256Hex(data), Checksums.sha256Hex(new ByteArrayInputStream(data)));
    }

    @Test
    public void verify_match() {
        assertEquals(Checksums.Result.MATCH, Checksums.verify(ABC, ABC));
    }

    @Test
    public void verify_isCaseInsensitiveAndAcceptsDigestPrefix() {
        assertEquals(Checksums.Result.MATCH, Checksums.verify(ABC.toUpperCase(), ABC));
        assertEquals(Checksums.Result.MATCH, Checksums.verify("sha256:" + ABC, ABC));
        assertEquals(Checksums.Result.MATCH, Checksums.verify("  SHA256:" + ABC.toUpperCase() + " ", ABC));
    }

    @Test
    public void verify_mismatch() {
        assertEquals(Checksums.Result.MISMATCH, Checksums.verify(EMPTY, ABC));
        assertEquals(Checksums.Result.MISMATCH, Checksums.verify(ABC.substring(1), ABC));
    }

    @Test
    public void verify_unavailableWhenNoExpectedValue() {
        assertEquals(Checksums.Result.UNAVAILABLE, Checksums.verify(null, ABC));
        assertEquals(Checksums.Result.UNAVAILABLE, Checksums.verify("", ABC));
        assertEquals(Checksums.Result.UNAVAILABLE, Checksums.verify("   ", ABC));
    }
}
