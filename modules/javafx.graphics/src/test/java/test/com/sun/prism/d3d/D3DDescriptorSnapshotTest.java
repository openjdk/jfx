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

package test.com.sun.prism.d3d;

import com.sun.prism.d3d.D3DNativeShim;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The ABI guard that needs neither a device nor the library: every {@code FunctionDescriptor} that
 * {@code D3DNative} binds - {@code name + " " + descriptor}, in binding order - against {@value #TABLE}, a
 * table transcribed by hand from {@code src/main/native-prism-d3d/prism_d3d_api.h}. Not generated from the
 * Java, which would compare the facade with itself.
 * <p>
 * {@code D3DNativeTest.everyExportedSymbolResolvesInTheLoadedLibrary} only asks {@code lookup.find()}: it
 * proves the names exist, not that the Java signature matches the C prototype. A header that widened a
 * parameter, reordered two, or changed a return type would still resolve every symbol and then mis-marshal;
 * for the two functions bound {@code critical(true)} that is a memcpy past a pinned heap array - memory
 * corruption, not an exception. The descriptors are built by {@code D3DNative}'s class initializer whether or
 * not {@code prism_d3d} loads, so this runs on every platform and never skips.
 * <p>
 * The table's notation is {@code FunctionDescriptor.toString()}: {@code i4} for {@code int32_t}, {@code j8}
 * for {@code int64_t}, {@code a8} for every pointer, {@code f4} {@code float}, {@code d8} {@code double},
 * {@code v} a {@code void} return, the argument layouts concatenated. When this fails, one side of the ABI
 * changed and the header decides which; nothing here is regenerated.
 */
public class D3DDescriptorSnapshotTest {

    static final String TABLE = "d3d-descriptors.txt";

    @Test
    public void everyBoundDescriptorMatchesTheHeaderTranscription() throws IOException {
        List<String> expected = table();
        assertEquals(59, expected.size(), "prism_d3d_api.h declares 59 functions");
        List<String> actual = D3DNativeShim.descriptors();
        assertEquals(expected.size(), actual.size(), () -> "D3DNative binds " + actual.size() + ": " + actual);
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i), "descriptor " + i + " (header order)");
        }
        assertEquals(expected, actual);
    }

    @Test
    public void theDescriptorsNameTheBoundSymbolsInTheSameOrder() {
        List<String> names = D3DNativeShim.descriptors().stream()
                .map(line -> line.substring(0, line.indexOf(' ')))
                .toList();
        assertEquals(D3DNativeShim.boundSymbols(), names);
    }

    /** The data lines of {@value #TABLE}: comments and blank lines dropped, whitespace trimmed. */
    static List<String> table() throws IOException {
        try (InputStream in = D3DDescriptorSnapshotTest.class.getResourceAsStream(TABLE)) {
            assertNotNull(in, "resource " + TABLE + " is missing next to " + D3DDescriptorSnapshotTest.class.getName());
            return new String(in.readAllBytes(), StandardCharsets.US_ASCII).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
        }
    }
}
