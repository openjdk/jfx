/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.com.sun.javafx.font;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The machine gate and the file format of {@link FontGoldens}, checked without any native code so that the
 * rules the two Windows goldens rely on hold on every platform: a golden captured elsewhere is skipped, never
 * failed; informational keys never take part in a comparison; every escape round-trips.
 */
public class FontGoldensTest {

    /** A golden as {@link FontGoldens#format} writes it: comments, a blank line, escaped backslashes and NULs. */
    private static final String GOLDEN_TEXT = """
            # a comment line
            #
            machine.os.name=Windows 10
            machine.font.dir.fingerprint=537:abc
            info.windows.version=Version 10.0.19045.6466
            capture.provenance=some JVM on some OS

            fontPath=C:\\\\WINDOWS\\\\Fonts
            fontLink.Tahoma=MSGOTHIC.TTC,MS UI Gothic\\0MINGLIU.TTC\\0\\0
            eudcFontFile=#null
            """;

    private static final String FONT_LINK_VALUE = "MSGOTHIC.TTC,MS UI Gothic\0MINGLIU.TTC\0\0";

    @Test
    public void parseSkipsCommentsAndBlankLinesAndUnescapesValues() {
        Map<String, String> parsed = FontGoldens.parse(GOLDEN_TEXT);
        assertEquals(7, parsed.size(), parsed.keySet().toString());
        assertEquals("Windows 10", parsed.get("machine.os.name"));
        assertEquals("C:\\WINDOWS\\Fonts", parsed.get("fontPath"));
        assertEquals(FONT_LINK_VALUE, parsed.get("fontLink.Tahoma"));
        assertEquals(FontGoldens.NULL, parsed.get("eudcFontFile"));
    }

    @Test
    public void parseAcceptsCarriageReturnLineEndings() {
        Map<String, String> parsed = FontGoldens.parse(GOLDEN_TEXT.replace("\n", "\r\n"));
        assertEquals(FontGoldens.parse(GOLDEN_TEXT), parsed);
    }

    @Test
    public void parseRejectsDuplicateKeysAndLinesWithoutAnEquals() {
        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class,
                () -> FontGoldens.parse("a=1\na=2\n"));
        assertTrue(duplicate.getMessage().contains("line 2"), duplicate.getMessage());
        IllegalArgumentException malformed = assertThrows(IllegalArgumentException.class,
                () -> FontGoldens.parse("a=1\nnot a key value pair\n"));
        assertTrue(malformed.getMessage().contains("line 2"), malformed.getMessage());
    }

    @Test
    public void formatAndParseRoundTripEveryEscape() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("backslash", "C:\\WINDOWS\\Fonts");
        values.put("lineBreaks", "line 1\nline 2\r\n");
        values.put("nul", FONT_LINK_VALUE);
        values.put("equalsInValue", "a=b=c");
        values.put("empty", "");
        values.put("nullValue", null);
        values.put("hash", "#not a comment");

        String text = FontGoldens.format(values, List.of("first header line", "", "third"));

        assertTrue(text.startsWith("# first header line\n#\n# third\nbackslash=C:\\\\WINDOWS\\\\Fonts\n"), text);
        assertTrue(text.contains("\nnul=MSGOTHIC.TTC,MS UI Gothic\\0MINGLIU.TTC\\0\\0\n"), text);
        assertTrue(text.contains("\nlineBreaks=line 1\\nline 2\\r\\n\n"), text);
        assertTrue(text.endsWith("\n"), "final newline");
        assertEquals(-1, text.indexOf('\r'), "no carriage return may reach the file");

        Map<String, String> parsed = FontGoldens.parse(text);
        Map<String, String> expected = new LinkedHashMap<>(values);
        expected.put("nullValue", FontGoldens.NULL);
        assertEquals(expected, parsed);
    }

    @Test
    public void formatRejectsKeysThatWouldBreakTheLineFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> FontGoldens.format(Map.of("a=b", "1"), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> FontGoldens.format(Map.of("a\nb", "1"), List.of()));
    }

    @Test
    public void listsEscapeTheSeparatorAndKeepNullsAndExactFloats() {
        assertEquals("a\\sb;c\\\\d;#null", FontGoldens.joinList(Arrays.asList("a;b", "c\\d", null)));
        assertEquals("", FontGoldens.joinList(List.of()));
        assertEquals("1;-2;3", FontGoldens.joinInts(new int[] {1, -2, 3, 99}, 3));
        assertEquals("-0.0;NaN;1.5;3.4028235E38",
                FontGoldens.joinFloats(new float[] {-0.0f, Float.NaN, 1.5f, Float.MAX_VALUE}, 4));
    }

    @Test
    public void sameMachineIsNotAborted() {
        FontGoldens golden = FontGoldens.fromText(GOLDEN_TEXT);
        Map<String, String> captured = FontGoldens.parse(GOLDEN_TEXT);
        captured.put("info.windows.version", "Version 10.0.26100.1");
        captured.put("capture.provenance", "another JVM");
        captured.put("fontPath", "D:\\Fonts");
        assertDoesNotThrow(() -> golden.assumeSameMachine(captured, FontGoldensTest.class));
    }

    @Test
    public void differentMachineAbortsWithTheCaptureCommand() {
        FontGoldens golden = FontGoldens.fromText(GOLDEN_TEXT);
        Map<String, String> captured = FontGoldens.parse(GOLDEN_TEXT);
        captured.put("machine.font.dir.fingerprint", "538:def");

        TestAbortedException aborted = assertThrows(TestAbortedException.class,
                () -> golden.assumeSameMachine(captured, FontGoldensTest.class, false));

        String message = aborted.getMessage();
        assertTrue(message.contains("machine.font.dir.fingerprint: golden=537:abc this machine=538:def"), message);
        assertTrue(message.contains("-Dtest=FontGoldensTest -D" + FontGoldens.CAPTURE_PROPERTY + "=true"), message);
        assertTrue(message.contains("-D" + FontGoldens.REGENERATE_PROPERTY + "=true"), message);
    }

    @Test
    public void gateKeyMissingFromTheGoldenAborts() {
        FontGoldens golden = FontGoldens.fromText(GOLDEN_TEXT);
        Map<String, String> captured = FontGoldens.parse(GOLDEN_TEXT);
        captured.put("machine.dwrite.sha256", "0123");

        TestAbortedException aborted = assertThrows(TestAbortedException.class,
                () -> golden.assumeSameMachine(captured, FontGoldensTest.class, false));
        assertTrue(aborted.getMessage().contains("machine.dwrite.sha256: golden=null this machine=0123"),
                aborted.getMessage());
    }

    @Test
    public void gateKeyMissingFromThisMachineAborts() {
        FontGoldens golden = FontGoldens.fromText(GOLDEN_TEXT);
        Map<String, String> captured = FontGoldens.parse(GOLDEN_TEXT);
        captured.remove("machine.os.name");

        TestAbortedException aborted = assertThrows(TestAbortedException.class,
                () -> golden.assumeSameMachine(captured, FontGoldensTest.class, false));
        assertTrue(aborted.getMessage().contains("machine.os.name: golden=Windows 10 this machine=null"),
                aborted.getMessage());
    }

    /** {@code -Djfx.parity.require=true}: the same gate fails instead, still naming what differs. */
    @Test
    public void differentMachineFailsUnderTheRequireProperty() {
        FontGoldens golden = FontGoldens.fromText(GOLDEN_TEXT);
        Map<String, String> captured = FontGoldens.parse(GOLDEN_TEXT);
        captured.put("machine.font.dir.fingerprint", "538:def");

        AssertionFailedError failure = assertThrows(AssertionFailedError.class,
                () -> golden.assumeSameMachine(captured, FontGoldensTest.class, true));

        String message = failure.getMessage();
        assertTrue(message.contains("-Djfx.parity.require=true"), message);
        assertTrue(message.contains("machine.font.dir.fingerprint: golden=537:abc this machine=538:def"), message);
        assertTrue(message.contains("The golden was captured on: "), message);
        assertTrue(message.contains("-D" + FontGoldens.REGENERATE_PROPERTY + "=true"), message);
    }

    @Test
    public void contentComparisonIgnoresMachineAndInformationalKeys() {
        FontGoldens golden = FontGoldens.fromText(GOLDEN_TEXT);
        Map<String, String> captured = FontGoldens.parse(GOLDEN_TEXT);
        captured.put("machine.os.name", "Windows 11");
        captured.put("info.windows.version", "Version 10.0.26100.1");
        captured.remove("capture.provenance");
        captured.put("info.new", "recorded on this run only");
        assertDoesNotThrow(() -> golden.assertSameContent(captured));
    }

    @Test
    public void contentDifferencesFailWithEveryKindOfDifferenceNamed() {
        FontGoldens golden = FontGoldens.fromText(GOLDEN_TEXT);
        Map<String, String> captured = FontGoldens.parse(GOLDEN_TEXT);
        captured.put("fontPath", "D:\\Fonts");
        captured.remove("eudcFontFile");
        captured.put("systemLCID", "1033");

        AssertionFailedError failure = assertThrows(AssertionFailedError.class,
                () -> golden.assertSameContent(captured));

        String message = failure.getMessage();
        assertTrue(message.startsWith("3 difference(s)"), message);
        assertTrue(message.contains("fontPath: golden=C:\\WINDOWS\\Fonts now=D:\\Fonts"), message);
        assertTrue(message.contains("only in the golden: eudcFontFile=#null"), message);
        assertTrue(message.contains("only in this run: systemLCID=1033"), message);
    }

    @Test
    public void nullValuesCompareAsTheNullSentinel() {
        FontGoldens golden = FontGoldens.fromText(GOLDEN_TEXT);
        Map<String, String> captured = FontGoldens.parse(GOLDEN_TEXT);
        captured.put("eudcFontFile", FontGoldens.nullable(null));
        assertDoesNotThrow(() -> golden.assertSameContent(captured));
    }

    @Test
    public void directoryFingerprintDependsOnNamesAndSizesOnly(@TempDir Path directory) throws IOException {
        assertEquals(FontGoldens.NULL, FontGoldens.directoryFingerprint(null));
        assertEquals(FontGoldens.NULL, FontGoldens.directoryFingerprint(directory.resolve("no-such-dir")));

        Files.writeString(directory.resolve("Arial.ttf"), "1234", StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("times.ttf"), "12", StandardCharsets.UTF_8);
        Files.createDirectory(directory.resolve("a-subdirectory-is-ignored"));
        String first = FontGoldens.directoryFingerprint(directory);
        assertTrue(first.matches("2:[0-9a-f]{64}"), first);

        Files.writeString(directory.resolve("times.ttf"), "34", StandardCharsets.UTF_8);
        assertEquals(first, FontGoldens.directoryFingerprint(directory), "same names and sizes, same fingerprint");

        Files.writeString(directory.resolve("times.ttf"), "345", StandardCharsets.UTF_8);
        assertNotEquals(first, FontGoldens.directoryFingerprint(directory), "a size change is a new fingerprint");
    }

    @Test
    public void sha256IsLowerCaseHexOfTheDigest() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                FontGoldens.sha256Hex(new byte[0]));
        assertEquals("00ff7f80", FontGoldens.hex(new byte[] {0, -1, 127, -128}));
    }
}
