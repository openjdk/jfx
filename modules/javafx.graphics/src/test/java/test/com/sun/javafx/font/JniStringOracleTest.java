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

import com.sun.javafx.font.JniStringCodec;
import com.sun.javafx.font.JniStringOracleShim;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link JniStringCodec} against the running JVM's own {@code NewStringUTF} and {@code GetStringUTFRegion} (the
 * encoder of {@code GetStringUTFChars}), reached through {@link JniStringOracleShim} on every platform: the eight
 * measured rows in this JVM, then a seeded corpus of random and structured byte strings through the JVM's decoder
 * and a seeded corpus of random code-unit strings (with {@code U+0000}, surrogate pairs and unpaired surrogates)
 * through the JVM's encoder, compared code unit by code unit and byte by byte with the codec.
 *
 * <p>A {@code String} made by {@code NewStringUTF} from malformed bytes may carry the UTF-16 coder for Latin-1
 * content, so the comparison is on the characters, never through {@code String.equals}.
 *
 * <p>The corpus is large but each JVM can create only a few dozen JNI string references before its handle block
 * faults ({@link JniStringOracleShim} explains why this is the tool's limit, not the codec's), so the fuzz runs one
 * bounded batch per fresh child JVM ({@link JniStringOracleChild}), launched with this fork's module and native
 * options; the eight measured rows and the version check run in this JVM, well under the limit. The child launch
 * reuses {@code ManagementFactory.getRuntimeMXBean().getInputArguments()}, so it needs no knowledge of the module
 * path or {@code --enable-native-access} list.
 */
public class JniStringOracleTest {

    /** References per child; {@link JniStringOracleShim#MAX_BATCH} caps it. */
    private static final int BATCH = JniStringOracleShim.MAX_BATCH;
    private static final int DECODE_BATCHES = 40;
    private static final int ENCODE_BATCHES = 20;

    private static JniStringOracleShim.Probe probe;

    @BeforeAll
    public static void probeTheJvm() {
        probe = JniStringOracleShim.probe();
        System.out.println("JniStringOracleTest: JNI version 0x" + Integer.toHexString(probe.jniVersion()) + " from "
                + probe.libjvm() + "; " + probe.slotEvidence());
    }

    @Test
    public void theJvmReportsAJniVersion() {
        assertTrue(probe.jniVersion() >= 0x00010008, "JNI version 0x" + Integer.toHexString(probe.jniVersion()));
    }

    @Test
    public void measuredRowsAgreeWithTheRunningJvm() {
        char[][] jvm = JniStringOracleShim.newStringUtf(JniStringCodecTest.MEASURED_INPUTS);
        for (int i = 0; i < jvm.length; i++) {
            assertArrayEquals(JniStringCodecTest.MEASURED_CHARS[i], jvm[i],
                    "NewStringUTF of row " + i + " " + JniStringCodecTest.hex(JniStringCodecTest.MEASURED_INPUTS[i]));
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public void decoderMatchesNewStringUtfOnFuzzedBytes() throws IOException, InterruptedException {
        List<byte[]> corpus = decodeCorpus(BATCH * DECODE_BATCHES);
        List<String> mismatches = new ArrayList<>();
        long bytes = 0;
        HexFormat hex = HexFormat.of();
        for (int start = 0; start < corpus.size(); start += BATCH) {
            List<byte[]> batch = corpus.subList(start, Math.min(start + BATCH, corpus.size()));
            List<String> in = new ArrayList<>(batch.size());
            for (byte[] input : batch) {
                in.add(hex.formatHex(input));
                bytes += input.length;
            }
            List<String> out = runChild("decode", in);
            assertEquals(batch.size(), out.size(), "child returned a different count");
            for (int i = 0; i < batch.size(); i++) {
                char[] jvm = toChars(hex.parseHex(out.get(i)));
                char[] java = JniStringCodec.fromNewStringUtf(batch.get(i)).toCharArray();
                if (!Arrays.equals(jvm, java)) {
                    mismatches.add(hex.formatHex(batch.get(i)) + ": NewStringUTF " + units(jvm)
                            + ", JniStringCodec " + units(java));
                }
            }
        }
        System.out.println("JniStringOracleTest: NewStringUTF corpus " + corpus.size() + " byte strings, " + bytes
                + " bytes, in " + (corpus.size() + BATCH - 1) / BATCH + " child JVMs, " + mismatches.size()
                + " mismatches");
        report(mismatches, corpus.size(), "decode differently");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public void encoderMatchesGetStringUtfRegionOnFuzzedStrings() throws IOException, InterruptedException {
        List<String> corpus = encodeCorpus(BATCH * ENCODE_BATCHES);
        List<String> mismatches = new ArrayList<>();
        long units = 0;
        HexFormat hex = HexFormat.of();
        for (int start = 0; start < corpus.size(); start += BATCH) {
            List<String> batch = corpus.subList(start, Math.min(start + BATCH, corpus.size()));
            List<String> in = new ArrayList<>(batch.size());
            for (String s : batch) {
                in.add(hex.formatHex(toBytes(s.toCharArray())));
                units += s.length();
            }
            List<String> out = runChild("encode", in);
            assertEquals(batch.size(), out.size(), "child returned a different count");
            for (int i = 0; i < batch.size(); i++) {
                byte[] jvm = hex.parseHex(out.get(i));
                byte[] java = JniStringCodec.toModifiedUtf8(batch.get(i));
                if (!Arrays.equals(jvm, java)) {
                    mismatches.add(units(batch.get(i).toCharArray()) + ": GetStringUTFRegion " + hex.formatHex(jvm)
                            + ", JniStringCodec " + hex.formatHex(java));
                } else if (JniStringCodec.modifiedUtf8Length(batch.get(i)) != jvm.length - 1) {
                    mismatches.add(units(batch.get(i).toCharArray()) + ": GetStringUTFLength " + (jvm.length - 1)
                            + ", modifiedUtf8Length " + JniStringCodec.modifiedUtf8Length(batch.get(i)));
                }
            }
        }
        System.out.println("JniStringOracleTest: GetStringUTFRegion corpus " + corpus.size() + " strings, " + units
                + " code units, in " + (corpus.size() + BATCH - 1) / BATCH + " child JVMs, " + mismatches.size()
                + " mismatches");
        report(mismatches, corpus.size(), "encode differently");
    }

    private static void report(List<String> mismatches, int total, String what) {
        if (!mismatches.isEmpty()) {
            fail(mismatches.size() + " of " + total + " " + what + ":\n  "
                    + String.join("\n  ", mismatches.subList(0, Math.min(mismatches.size(), 20))));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Child JVM launch
    // ---------------------------------------------------------------------------------------------

    /** Runs one batch through {@link JniStringOracleChild} in a fresh JVM and returns its output lines. */
    private static List<String> runChild(String mode, List<String> input) throws IOException, InterruptedException {
        Path work = Files.createTempDirectory("jni-string-oracle");
        Path in = work.resolve("in.txt");
        Path out = work.resolve("out.txt");
        Path log = work.resolve("child.log");
        try {
            Files.write(in, input, StandardCharsets.US_ASCII);
            List<String> command = new ArrayList<>();
            command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (argument.startsWith("-agentlib") || argument.startsWith("-javaagent")
                        || argument.startsWith("-Xrunjdwp") || argument.startsWith("-Xdebug")
                        || argument.startsWith("-XX:ErrorFile")) {
                    continue;
                }
                command.add(argument);
            }
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(JniStringOracleChild.class.getName());
            command.add(mode);
            command.add(in.toString());
            command.add(out.toString());
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();
            if (!process.waitFor(2, TimeUnit.MINUTES)) {
                process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
                fail("child JVM did not finish: " + command);
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(out)) {
                fail("child JVM exited with " + process.exitValue() + "; output:\n" + readOrEmpty(log)
                        + "\ncommand: " + command);
            }
            return Files.readAllLines(out, StandardCharsets.US_ASCII);
        } finally {
            deleteTree(work);
        }
    }

    private static String readOrEmpty(Path file) {
        try {
            return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            return "(unreadable: " + e + ")";
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var files = Files.walk(root)) {
            files.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best effort; the temp directory is removed by the OS in the worst case
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Corpora (seeded, so a failure names an input that can be reproduced)
    // ---------------------------------------------------------------------------------------------

    /**
     * Random bytes ({@code 01..FF}, so the length is the string length), structured strings built from the tokens
     * the decoder distinguishes, standard UTF-8 of random code points (four-byte sequences included) and the
     * modified UTF-8 {@code DataOutputStream.writeUTF} produces for random code units.
     */
    private static List<byte[]> decodeCorpus(int count) {
        Random random = new Random(0x4E657753747255L);
        List<byte[]> corpus = new ArrayList<>(count);
        while (corpus.size() < count) {
            switch (corpus.size() % 4) {
                case 0 -> {
                    byte[] bytes = new byte[random.nextInt(33)];
                    for (int k = 0; k < bytes.length; k++) {
                        bytes[k] = (byte) (1 + random.nextInt(255));
                    }
                    corpus.add(bytes);
                }
                case 1 -> corpus.add(structured(random));
                case 2 -> corpus.add(randomCodePoints(random).getBytes(StandardCharsets.UTF_8));
                default -> corpus.add(JniStringCodecTest.writeUtfPayload(
                        JniStringCodecTest.randomCodeUnits(random, 32)));
            }
        }
        return corpus;
    }

    private static byte[] structured(Random random) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int tokens = 1 + random.nextInt(8);
        for (int t = 0; t < tokens; t++) {
            switch (random.nextInt(16)) {
                case 0 -> out.write(0x41 + random.nextInt(26));
                case 1 -> { out.write(0xC2 + random.nextInt(30)); out.write(continuation(random)); }
                case 2 -> {
                    out.write(0xE0 + random.nextInt(16));
                    out.write(continuation(random));
                    out.write(continuation(random));
                }
                case 3 -> {
                    out.write(0xF0 + random.nextInt(5));
                    out.write(continuation(random));
                    out.write(continuation(random));
                    out.write(continuation(random));
                }
                case 4 -> {
                    out.write(0xED); out.write(0xA0 + random.nextInt(16)); out.write(continuation(random));
                    out.write(0xED); out.write(0xB0 + random.nextInt(16)); out.write(continuation(random));
                }
                case 5 -> out.write(continuation(random));
                case 6 -> out.write(0xC0 + random.nextInt(32));
                case 7 -> { out.write(0xE0 + random.nextInt(16)); out.write(continuation(random)); }
                case 8 -> { out.write(0xC0); out.write(0x80); }
                case 9 -> { out.write(0xE0); out.write(0x80); out.write(continuation(random)); }
                case 10 -> out.write(0xF8 + random.nextInt(8));
                case 11 -> { out.write(0xC0 + random.nextInt(48)); out.write(0x41 + random.nextInt(26)); }
                case 12 -> out.write(0x01 + random.nextInt(0x7F));
                case 13 -> out.write(0x01 + random.nextInt(0x7F));
                case 14 -> {
                    out.write(0xF5 + random.nextInt(3));
                    out.write(continuation(random));
                    out.write(continuation(random));
                    out.write(continuation(random));
                }
                default -> out.write(1 + random.nextInt(255));
            }
        }
        return out.toByteArray();
    }

    private static int continuation(Random random) {
        return 0x80 + random.nextInt(64);
    }

    private static String randomCodePoints(Random random) {
        int count = random.nextInt(13);
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < count; i++) {
            switch (random.nextInt(5)) {
                case 0 -> s.appendCodePoint(0x01 + random.nextInt(0x7F));
                case 1 -> s.appendCodePoint(0x80 + random.nextInt(0x780));
                case 2 -> s.appendCodePoint(0x800 + random.nextInt(0xD000));
                case 3 -> s.appendCodePoint(0xE000 + random.nextInt(0x2000));
                default -> s.appendCodePoint(0x10000 + random.nextInt(0x100000));
            }
        }
        return s.toString();
    }

    /** Random code-unit strings, plus the fixed cases the encoder is documented on. */
    private static List<String> encodeCorpus(int count) {
        Random random = new Random(0x47535546436872L);
        List<String> corpus = new ArrayList<>(List.of(
                "", JniStringCodecTest.string(0), JniStringCodecTest.string(0x61, 0, 0x62), "DejaVu Sans",
                JniStringCodecTest.string(0x63, 0x61, 0x66, 0xE9), JniStringCodecTest.string(0x65E5, 0x672C, 0x8A9E),
                new String(Character.toChars(0x1F600)), JniStringCodecTest.string(0xD800),
                JniStringCodecTest.string(0xDC00, 0x41), JniStringCodecTest.string(0x41, 0xDFFF),
                JniStringCodecTest.string(0xDE00, 0xD83D), new String(Character.toChars(0x10FFFF)),
                JniStringCodecTest.string(0x0001, 0x007F, 0x0080, 0x07FF, 0x0800, 0xFFFF, 0xD7FF, 0xE000)));
        while (corpus.size() < count) {
            corpus.add(JniStringCodecTest.randomCodeUnits(random, 24));
        }
        return corpus.subList(0, count);
    }

    private static byte[] toBytes(char[] chars) {
        byte[] bytes = new byte[chars.length * 2];
        for (int i = 0; i < chars.length; i++) {
            bytes[2 * i] = (byte) (chars[i] >> 8);
            bytes[2 * i + 1] = (byte) chars[i];
        }
        return bytes;
    }

    private static char[] toChars(byte[] bytes) {
        char[] chars = new char[bytes.length / 2];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (((bytes[2 * i] & 0xFF) << 8) | (bytes[2 * i + 1] & 0xFF));
        }
        return chars;
    }

    private static String units(char[] chars) {
        StringBuilder s = new StringBuilder("[");
        for (char c : chars) {
            if (s.length() > 1) {
                s.append(' ');
            }
            s.append(String.format("%04X", (int) c));
        }
        return s.append(']').toString();
    }
}
