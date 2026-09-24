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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The golden files for the Windows font layer: what the JNI build of {@code javafx_font} returned, captured
 * before any of it was rewritten.
 * <p>
 * Format: one {@code key=value} per line, LF endings, {@code #} comment lines, values escaped for backslash,
 * newline, carriage return and NUL (font-link registry data is NUL-separated). Lists are joined with
 * {@code ;}, elements escaped for {@code ;} as {@code \s}. Floats are always {@link Float#toString}, which
 * round-trips exactly. {@link #NULL} stands for a null value.
 * <p>
 * Both goldens are <em>machine-specific</em>: they record this machine's installed fonts and its DirectWrite.
 * Every key prefixed {@code machine.} is a gate: the test compares the body only when all of them match what
 * the running machine reports, and otherwise aborts (JUnit assumption) with the capture command - or, under
 * {@code -Djfx.parity.require=true}, fails naming the keys that differ and where the golden was captured, so
 * that the machine which owns the golden cannot report a skip as green ({@link ParityGate}). Keys prefixed
 * {@code info.} or {@code capture.} are recorded for the reader and never compared.
 * <p>
 * Capture is deliberate: {@code -Djfx.font.golden.capture=true} writes, and an existing golden is only
 * overwritten with {@code -Djfx.font.golden.regenerate=true} on top. A regeneration is a behaviour change to be
 * reviewed, not a test fix.
 */
final class FontGoldens {

    static final String CAPTURE_PROPERTY = "jfx.font.golden.capture";
    static final String REGENERATE_PROPERTY = "jfx.font.golden.regenerate";

    /** Stands in for a null value, which the flat format cannot otherwise distinguish from "". */
    static final String NULL = "#null";

    static final String MACHINE_PREFIX = "machine.";
    private static final String INFO_PREFIX = "info.";
    private static final String CAPTURE_PREFIX = "capture.";

    private static final char SEPARATOR = ';';
    private static final String RESOURCE_DIR = "src/test/resources/test/com/sun/javafx/font";
    private static final String CLASSES_DIR = "target/test-classes/test/com/sun/javafx/font";
    private static final int MAX_REPORTED_DIFFERENCES = 60;

    private final Map<String, String> values;

    /** The ledger of the test that loaded this golden: armed by the machine gate, counted by the comparison. */
    private final ParityGate.Ledger ledger;

    private FontGoldens(Map<String, String> values, Class<?> test) {
        this.values = values;
        this.ledger = ParityGate.ledger(test);
    }

    // ---------------------------------------------------------------------------------------------
    // Capture / load
    // ---------------------------------------------------------------------------------------------

    static boolean captureRequested() {
        return Boolean.getBoolean(CAPTURE_PROPERTY);
    }

    static String captureCommand(Class<?> test) {
        return "mvn -pl modules/javafx.graphics test -DskipNative=true -Dtest=" + test.getSimpleName()
                + " -D" + CAPTURE_PROPERTY + "=true";
    }

    /**
     * Writes {@code values} under {@code header} to the source resource directory and to the copy on the
     * test classpath, so that a capture and a verification in the same build agree. Refuses to overwrite
     * an existing golden unless {@link #REGENERATE_PROPERTY} is set.
     */
    static void write(String fileName, Map<String, String> values, List<String> header) throws IOException {
        Path module = moduleDirectory();
        Path source = module.resolve(RESOURCE_DIR).resolve(fileName);
        if (Files.exists(source) && !Boolean.getBoolean(REGENERATE_PROPERTY)) {
            fail("a golden file already exists at " + source + ". It is the record of what the JNI build"
                    + " returned; overwriting it changes what the parity tests prove. If that is intended,"
                    + " re-run with -D" + REGENERATE_PROPERTY + "=true and review the resulting diff as a"
                    + " behaviour change, not as a test fix.");
        }
        String text = format(values, header);
        Files.createDirectories(source.getParent());
        Files.writeString(source, text, StandardCharsets.UTF_8);
        Path classes = module.resolve(CLASSES_DIR).resolve(fileName);
        Files.createDirectories(classes.getParent());
        Files.writeString(classes, text, StandardCharsets.UTF_8);
        System.out.println("captured " + values.size() + " entries to " + source);
    }

    /**
     * @throws AssertionError if the golden is absent; an absent golden is a capture that has not been run,
     *         never a reason to skip
     */
    static FontGoldens load(String fileName, Class<?> test) {
        byte[] raw;
        try (InputStream in = FontGoldens.class.getResourceAsStream(fileName)) {
            raw = in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (raw == null) {
            throw new AssertionError("no golden file " + fileName + " on the classpath. The test compares"
                    + " the font layer against values captured from the JNI build; with no capture there is"
                    + " nothing to compare and passing would mean nothing. Capture with \""
                    + captureCommand(test) + "\" and commit the golden file.");
        }
        return new FontGoldens(parse(new String(raw, StandardCharsets.UTF_8)), test);
    }

    /** For {@link FontGoldensTest}: a golden from its text rather than from the classpath. */
    static FontGoldens fromText(String text) {
        return new FontGoldens(parse(text), FontGoldens.class);
    }

    // ---------------------------------------------------------------------------------------------
    // Comparison
    // ---------------------------------------------------------------------------------------------

    /**
     * Aborts the test (assumption) unless every {@code machine.} key recorded in the golden equals what
     * {@code captured} reports for this machine, and no new {@code machine.} key has appeared. Under
     * {@code -Djfx.parity.require=true} the abort is a failure instead; either way the message names every
     * differing key and the golden's {@code capture.provenance}.
     */
    void assumeSameMachine(Map<String, String> captured, Class<?> test) {
        assumeSameMachine(captured, test, ParityGate.required());
    }

    /** {@link #assumeSameMachine(Map, Class)} with the mode explicit, for {@link FontGoldensTest}. */
    void assumeSameMachine(Map<String, String> captured, Class<?> test, boolean require) {
        List<String> differences = new ArrayList<>();
        TreeSet<String> keys = new TreeSet<>();
        for (String key : values.keySet()) {
            if (key.startsWith(MACHINE_PREFIX)) {
                keys.add(key);
            }
        }
        for (String key : captured.keySet()) {
            if (key.startsWith(MACHINE_PREFIX)) {
                keys.add(key);
            }
        }
        for (String key : keys) {
            String golden = values.get(key);
            String now = captured.get(key);
            if (golden == null || !golden.equals(now)) {
                differences.add(key + ": golden=" + golden + " this machine=" + now);
            }
        }
        ledger.requireOracle(differences.isEmpty(), require, () -> "the golden was captured on a different"
                + " machine, OS build or font set, so it is not an oracle for this one and the comparison is"
                + " skipped. Differences: " + differences + ". The golden was captured on: "
                + values.getOrDefault("capture.provenance", "(not recorded)") + ". To make it an oracle for"
                + " this machine, capture here with \"" + captureCommand(test) + " -D" + REGENERATE_PROPERTY
                + "=true\" (the golden it replaces is machine-specific by design).");
    }

    /**
     * Fails with every difference between the golden body and {@code captured}, ignoring informational keys.
     * Every body key examined counts as one comparison in the loading test's {@link ParityGate.Ledger}.
     */
    void assertSameContent(Map<String, String> captured) {
        List<String> differences = new ArrayList<>();
        TreeSet<String> keys = new TreeSet<>(values.keySet());
        keys.addAll(captured.keySet());
        int total = 0;
        int compared = 0;
        for (String key : keys) {
            if (key.startsWith(INFO_PREFIX) || key.startsWith(CAPTURE_PREFIX) || key.startsWith(MACHINE_PREFIX)) {
                continue;
            }
            compared++;
            String golden = values.get(key);
            String now = captured.get(key);
            if (golden == null) {
                total++;
                report(differences, "only in this run: " + key + "=" + now);
            } else if (now == null) {
                total++;
                report(differences, "only in the golden: " + key + "=" + golden);
            } else if (!golden.equals(now)) {
                total++;
                report(differences, key + ": golden=" + golden + " now=" + now);
            }
        }
        ledger.compared(compared);
        if (total > 0) {
            fail(total + " difference(s) against the golden captured from the JNI build"
                    + (total > MAX_REPORTED_DIFFERENCES ? " (first " + MAX_REPORTED_DIFFERENCES + " shown)" : "")
                    + ":\n  " + String.join("\n  ", differences));
        }
    }

    private static void report(List<String> differences, String difference) {
        if (differences.size() < MAX_REPORTED_DIFFERENCES) {
            differences.add(difference);
        }
    }

    String get(String key) {
        return values.get(key);
    }

    int size() {
        return values.size();
    }

    // ---------------------------------------------------------------------------------------------
    // Format
    // ---------------------------------------------------------------------------------------------

    static Map<String, String> parse(String text) {
        Map<String, String> parsed = new LinkedHashMap<>();
        int line = 0;
        for (String raw : text.split("\n", -1)) {
            line++;
            String entry = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            if (entry.isBlank() || entry.startsWith("#")) {
                continue;
            }
            int equals = entry.indexOf('=');
            if (equals < 1) {
                throw new IllegalArgumentException("golden file line " + line + " is not key=value: " + entry);
            }
            String key = entry.substring(0, equals);
            if (parsed.put(key, unescape(entry.substring(equals + 1))) != null) {
                throw new IllegalArgumentException("golden file line " + line + " repeats key " + key);
            }
        }
        return parsed;
    }

    static String format(Map<String, String> values, List<String> header) {
        StringBuilder text = new StringBuilder(1 << 16);
        for (String comment : header) {
            text.append('#');
            if (!comment.isEmpty()) {
                text.append(' ').append(comment);
            }
            text.append('\n');
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            if (key.indexOf('=') >= 0 || key.indexOf('\n') >= 0 || key.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("golden key cannot contain '=' or a line break: " + key);
            }
            text.append(key).append('=').append(escape(entry.getValue() == null ? NULL : entry.getValue()))
                .append('\n');
        }
        return text.toString();
    }

    static String joinList(List<String> elements) {
        StringBuilder joined = new StringBuilder();
        for (String element : elements) {
            if (joined.length() > 0) {
                joined.append(SEPARATOR);
            }
            joined.append(escapeElement(element == null ? NULL : element));
        }
        return joined.toString();
    }

    static String joinInts(int[] values, int count) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                joined.append(SEPARATOR);
            }
            joined.append(values[i]);
        }
        return joined.toString();
    }

    static String joinFloats(float[] values, int count) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                joined.append(SEPARATOR);
            }
            joined.append(Float.toString(values[i]));
        }
        return joined.toString();
    }

    static String nullable(String value) {
        return value == null ? NULL : value;
    }

    private static String escapeElement(String element) {
        return element.replace("\\", "\\\\").replace(String.valueOf(SEPARATOR), "\\s");
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\0' -> escaped.append("\\0");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private static String unescape(String value) {
        StringBuilder plain = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                plain.append(c);
                continue;
            }
            char next = value.charAt(++i);
            switch (next) {
                case 'n' -> plain.append('\n');
                case 'r' -> plain.append('\r');
                case '0' -> plain.append('\0');
                case '\\' -> plain.append('\\');
                default -> plain.append('\\').append(next);
            }
        }
        return plain.toString();
    }

    // ---------------------------------------------------------------------------------------------
    // Machine identity helpers
    // ---------------------------------------------------------------------------------------------

    /** {@code machine.os.*} - shared by both goldens. */
    static void putOperatingSystem(Map<String, String> out) {
        out.put("machine.os.name", System.getProperty("os.name"));
        out.put("machine.os.version", System.getProperty("os.version"));
        out.put("machine.os.arch", System.getProperty("os.arch"));
        out.put("info.windows.version", windowsVersion());
        out.put("capture.provenance", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version")
                + " on " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
    }

    /** The full Windows build ({@code 10.0.19045.6466}); {@code os.version} alone stops at {@code 10.0}. */
    private static String windowsVersion() {
        try {
            Process ver = new ProcessBuilder("cmd.exe", "/c", "ver").redirectErrorStream(true).start();
            String output = new String(ver.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            ver.waitFor();
            int open = output.indexOf('[');
            int close = output.indexOf(']');
            return open >= 0 && close > open ? output.substring(open + 1, close) : output;
        } catch (IOException | InterruptedException e) {
            return "unknown (" + e + ")";
        }
    }

    static Path windowsDirectory() {
        String windir = System.getenv("WINDIR");
        if (windir == null || windir.isBlank()) {
            windir = System.getenv("SystemRoot");
        }
        return Path.of(windir == null ? "C:\\Windows" : windir);
    }

    static Path systemFontDirectory() {
        return windowsDirectory().resolve("Fonts");
    }

    static Path userFontDirectory() {
        String local = System.getenv("LOCALAPPDATA");
        return local == null ? null : Path.of(local, "Microsoft", "Windows", "Fonts");
    }

    /**
     * A fingerprint of a directory's contents: sha256 over the sorted {@code name<TAB>size} lines of its
     * regular files. Case-insensitive file systems still report the stored case, so names are lower-cased.
     */
    static String directoryFingerprint(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return NULL;
        }
        TreeMap<String, Long> entries = new TreeMap<>();
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    entries.put(file.getFileName().toString().toLowerCase(Locale.ROOT), Files.size(file));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        StringBuilder listing = new StringBuilder();
        for (Map.Entry<String, Long> entry : entries.entrySet()) {
            listing.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
        }
        return entries.size() + ":" + sha256Hex(listing.toString().getBytes(StandardCharsets.UTF_8));
    }

    static String sha256Hex(byte[] data) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String sha256Hex(Path file) {
        try {
            return sha256Hex(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            text.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return text.toString();
    }

    /**
     * The font's own version string: OpenType {@code name} table, nameID 5, Windows platform (3), English
     * (0x409) preferred. Font files carry no VERSIONINFO resource, so this is the only version they have.
     * Handles a {@code ttcf} collection by reading its first font.
     */
    static String openTypeVersion(Path fontFile) {
        try {
            ByteBuffer data = ByteBuffer.wrap(Files.readAllBytes(fontFile));
            int offset = 0;
            if (data.getInt(0) == 0x74746366) { // 'ttcf'
                offset = data.getInt(12);
            }
            int numTables = data.getShort(offset + 4) & 0xFFFF;
            int record = offset + 12;
            for (int i = 0; i < numTables; i++, record += 16) {
                if (data.getInt(record) != 0x6E616D65) { // 'name'
                    continue;
                }
                int nameTable = data.getInt(record + 8);
                int count = data.getShort(nameTable + 2) & 0xFFFF;
                int strings = nameTable + (data.getShort(nameTable + 4) & 0xFFFF);
                String fallback = null;
                for (int n = 0, entry = nameTable + 6; n < count; n++, entry += 12) {
                    int platform = data.getShort(entry) & 0xFFFF;
                    int language = data.getShort(entry + 4) & 0xFFFF;
                    int nameId = data.getShort(entry + 6) & 0xFFFF;
                    if (nameId != 5 || platform != 3) {
                        continue;
                    }
                    int length = data.getShort(entry + 8) & 0xFFFF;
                    int start = strings + (data.getShort(entry + 10) & 0xFFFF);
                    byte[] utf16 = new byte[length];
                    data.get(start, utf16);
                    String version = new String(utf16, StandardCharsets.UTF_16BE);
                    if (language == 0x409) {
                        return version;
                    }
                    if (fallback == null) {
                        fallback = version;
                    }
                }
                return fallback == null ? NULL : fallback;
            }
            return NULL;
        } catch (IOException | RuntimeException e) {
            return "unreadable (" + e + ")";
        }
    }

    /**
     * Surefire runs with the module directory as its working directory. Checked rather than assumed: a
     * capture that writes the golden somewhere else looks like it worked and commits nothing.
     */
    private static Path moduleDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))
                || !Files.isDirectory(directory.resolve("src/test/resources"))) {
            fail("expected the working directory to be modules/javafx.graphics but it is " + directory
                    + ", so the golden would be written somewhere unexpected");
        }
        return directory;
    }
}
