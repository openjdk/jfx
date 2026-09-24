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

package test.com.sun.glass.ui.monocle;

import com.sun.glass.ui.monocle.EGLShim;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Java {@code setEGLAttrs} against the golden {@code egl-attrs-golden.txt}, printed by the C function of
 * EGL.c of commit 21d5a654f6 compiled verbatim: every {@code in=} row must produce exactly its {@code out=}
 * list, and the {@code EGL_*} values of the golden header must be the constants of EGL. The golden is
 * immutable; a port that needs it changed is wrong. Runs on every platform: pure arithmetic, no libEGL.
 */
public class EglAttrsGoldenTest {

    private static final String GOLDEN = "egl-attrs-golden.txt";
    private static final int CORPUS_ROWS = 19;

    private record Row(int[] in, int[] out) {
    }

    private static List<String> goldenLines() throws IOException {
        try (InputStream in = EglAttrsGoldenTest.class.getResourceAsStream(GOLDEN)) {
            assertNotNull(in, "test resource " + GOLDEN + " next to " + EglAttrsGoldenTest.class.getName());
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        lines.add(line);
                    }
                }
            }
            return lines;
        }
    }

    private static int[] ints(String csv) {
        return Arrays.stream(csv.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    private static List<Row> corpus(List<String> lines) {
        List<Row> rows = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("in=")) {
                int out = line.indexOf(" out=");
                assertTrue(out > 0, line);
                rows.add(new Row(ints(line.substring(3, out)), ints(line.substring(out + 5))));
            }
        }
        return rows;
    }

    /** The {@code NAME=0x...} pairs of the header line that carries the EGL values. */
    private static Map<String, Integer> headerConstants(List<String> lines) {
        Map<String, Integer> constants = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.startsWith("# EGL_")) {
                for (String pair : line.substring(2).split(" ")) {
                    int equals = pair.indexOf('=');
                    constants.put(pair.substring(0, equals), Integer.decode(pair.substring(equals + 1)));
                }
            }
        }
        return constants;
    }

    @Test
    public void everyGoldenRowIsReproduced() throws IOException {
        List<Row> rows = corpus(goldenLines());
        assertEquals(CORPUS_ROWS, rows.size(), "corpus rows");
        for (Row row : rows) {
            assertArrayEquals(row.out(), EGLShim.setEGLAttrs(row.in()), () -> "in=" + Arrays.toString(row.in()));
        }
    }

    @Test
    public void theGoldenCoversBothSurfaceTypesAndTheBufferSizeCase() throws IOException {
        List<Row> rows = corpus(goldenLines());
        int windowBit = EGLShim.constant("EGL_WINDOW_BIT");
        int pbufferBit = EGLShim.constant("EGL_PBUFFER_BIT");
        int bufferSize = EGLShim.constant("EGL_BUFFER_SIZE");
        assertTrue(rows.stream().anyMatch(row -> row.out()[1] == windowBit), "an ONSCREEN row");
        assertTrue(rows.stream().anyMatch(row -> row.out()[1] == pbufferBit), "an offscreen row");
        assertTrue(rows.stream().anyMatch(row -> row.out()[2] == bufferSize && row.out()[3] == 16),
                "the 5/6/5/0 row");
        assertFalse(rows.stream().anyMatch(row -> row.out()[2] == bufferSize && row.in()[3] != 0),
                "5/6/5 with alpha is not the buffer-size case");
    }

    @Test
    public void theHeaderValuesAreTheConstantsOfEgl() throws IOException {
        Map<String, Integer> header = headerConstants(goldenLines());
        assertEquals(12, header.size(), header::toString);
        for (Map.Entry<String, Integer> constant : header.entrySet()) {
            assertEquals(constant.getValue().intValue(), EGLShim.constant(constant.getKey()), constant.getKey());
        }
        assertEquals(0x0004, EGLShim.constant("EGL_OPENGL_ES2_BIT"));
        assertEquals(0x30A0, EGLShim.constant("EGL_OPENGL_ES_API"));
        assertEquals(0x3000, EGLShim.constant("EGL_SUCCESS"));
        assertEquals(0x3004, EGLShim.constant("EGL_BAD_ATTRIBUTE"));
        assertEquals(0x3009, EGLShim.constant("EGL_BAD_MATCH"));
        assertEquals(0x3098, EGLShim.constant("EGL_CONTEXT_CLIENT_VERSION"));
    }

    @Test
    public void theListEndsWithEglNoneAndTheDoubleBufferIndexIsIgnored() {
        int none = EGLShim.constant("EGL_NONE");
        int[] single = EGLShim.setEGLAttrs(new int[] {8, 8, 8, 8, 24, 0, 1});
        int[] doubled = EGLShim.setEGLAttrs(new int[] {8, 8, 8, 8, 24, 1, 1});
        assertArrayEquals(single, doubled, "DOUBLEBUFFER is not read");
        assertEquals(none, single[single.length - 1]);
        assertEquals(15, single.length);
        assertEquals(9, EGLShim.setEGLAttrs(new int[] {5, 6, 5, 0, 0, 0, 0}).length);
    }
}
