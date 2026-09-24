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

import com.sun.javafx.font.JniStringOracleShim;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * One batch of the {@code NewStringUTF} / {@code GetStringUTFRegion} fuzz oracle, in a fresh JVM so its JNI handle
 * block starts empty ({@link JniStringOracleShim} documents why a batch must stay small and why the corpus is split
 * across child JVMs). {@code test.com.sun.javafx.font.JniStringOracleTest} launches this class with the fork's
 * module and native options; it never runs from the test JVM.
 *
 * <p>Arguments: {@code decode|encode inputFile outputFile}. Each input line is hexadecimal.
 * <ul>
 * <li>{@code decode}: the raw bytes handed to {@code NewStringUTF} (no terminator; the oracle appends one). Each
 * output line is the hexadecimal of the resulting UTF-16 code units, two bytes big-endian each.</li>
 * <li>{@code encode}: the UTF-16 code units of a string, two bytes big-endian each. Each output line is the
 * hexadecimal of the modified UTF-8 {@code GetStringUTFChars} would return, the trailing {@code 00} included.</li>
 * </ul>
 */
public final class JniStringOracleChild {

    private JniStringOracleChild() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("usage: JniStringOracleChild decode|encode <in> <out>");
            System.exit(2);
        }
        String mode = args[0];
        List<String> lines = Files.readAllLines(Path.of(args[1]), StandardCharsets.US_ASCII);
        HexFormat hex = HexFormat.of();
        List<String> out = new ArrayList<>(lines.size());
        switch (mode) {
            case "decode" -> {
                byte[][] inputs = new byte[lines.size()][];
                for (int i = 0; i < inputs.length; i++) {
                    inputs[i] = hex.parseHex(lines.get(i));
                }
                char[][] results = JniStringOracleShim.newStringUtf(inputs);
                for (char[] chars : results) {
                    out.add(hex.formatHex(toBytes(chars)));
                }
            }
            case "encode" -> {
                char[][] inputs = new char[lines.size()][];
                for (int i = 0; i < inputs.length; i++) {
                    inputs[i] = toChars(hex.parseHex(lines.get(i)));
                }
                byte[][] results = JniStringOracleShim.getStringUtfRegion(inputs);
                for (byte[] bytes : results) {
                    out.add(hex.formatHex(bytes));
                }
            }
            default -> {
                System.err.println("unknown mode " + mode);
                System.exit(2);
            }
        }
        Files.write(Path.of(args[2]), out, StandardCharsets.US_ASCII);
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
}
