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

import com.sun.glass.ui.monocle.EglVendorShim;
import org.junit.jupiter.api.Test;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Java side of the {@code monocle_egl_ext.h} contract, on every platform and without a vendor library:
 * the 23 functions in the header's order, each descriptor's widths against the header's C types
 * ({@code int64_t} and handles 8 bytes, {@code int32_t} 4, {@code uint8_t} 1, {@code float} 4, pointers the
 * address width), the reading of a {@code uint8_t} as a boolean, and the constants the platform passes.
 */
public class EglVendorLayoutTest {

    /**
     * The header, one row a function: name, return type, argument types (J int64_t, I int32_t, B uint8_t,
     * F float, A pointer, V void).
     */
    private static final List<String[]> HEADER = List.of(
            new String[] {"getNativeWindowHandle", "J", "A"},
            new String[] {"getEglDisplayHandle", "J"},
            new String[] {"doEglInitialize", "B", "A"},
            new String[] {"doEglBindApi", "B", "I"},
            new String[] {"doEglChooseConfig", "J", "J", "A"},
            new String[] {"doEglCreateWindowSurface", "J", "J", "J", "J"},
            new String[] {"doEglCreateContext", "J", "J", "J"},
            new String[] {"doEglMakeCurrent", "B", "J", "J", "J", "J"},
            new String[] {"doEglSwapBuffers", "B", "J", "J"},
            new String[] {"doGetNumberOfScreens", "I"},
            new String[] {"doGetHandle", "J", "I"},
            new String[] {"doGetDepth", "I", "I"},
            new String[] {"doGetWidth", "I", "I"},
            new String[] {"doGetHeight", "I", "I"},
            new String[] {"doGetOffsetX", "I", "I"},
            new String[] {"doGetOffsetY", "I", "I"},
            new String[] {"doGetDpi", "I", "I"},
            new String[] {"doGetNativeFormat", "I", "I"},
            new String[] {"doGetScale", "F", "I"},
            new String[] {"doInitCursor", "V", "I", "I"},
            new String[] {"doSetCursorVisibility", "V", "B"},
            new String[] {"doSetLocation", "V", "I", "I"},
            new String[] {"doSetCursorImage", "V", "A", "I"});

    private static String code(MemoryLayout layout) {
        if (layout.equals(JAVA_LONG)) {
            return "J";
        }
        if (layout.equals(JAVA_INT)) {
            return "I";
        }
        if (layout.equals(JAVA_BYTE)) {
            return "B";
        }
        if (layout.equals(JAVA_FLOAT)) {
            return "F";
        }
        if (layout.equals(ADDRESS)) {
            return "A";
        }
        return "?" + layout;
    }

    private static String signature(FunctionDescriptor descriptor) {
        StringBuilder text = new StringBuilder(descriptor.returnLayout().map(EglVendorLayoutTest::code).orElse("V"));
        for (MemoryLayout argument : descriptor.argumentLayouts()) {
            text.append(' ').append(code(argument));
        }
        return text.toString();
    }

    @Test
    public void theContractHasThe23FunctionsOfTheHeaderInItsOrder() {
        List<String> names = new ArrayList<>(EglVendorShim.contract().keySet());
        List<String> expected = HEADER.stream().map(row -> row[0]).toList();
        assertEquals(23, expected.size());
        assertEquals(expected, names);
    }

    @Test
    public void everyDescriptorHasTheHeadersTypes() {
        Map<String, FunctionDescriptor> contract = EglVendorShim.contract();
        for (String[] row : HEADER) {
            StringBuilder expected = new StringBuilder(row[1]);
            for (int i = 2; i < row.length; i++) {
                expected.append(' ').append(row[i]);
            }
            assertEquals(expected.toString(), signature(contract.get(row[0])), row[0]);
        }
    }

    @Test
    public void theLayoutsHaveTheWidthsOfTheStdintTypes() {
        assertEquals(8, JAVA_LONG.byteSize(), "int64_t and every handle");
        assertEquals(4, JAVA_INT.byteSize(), "int32_t");
        assertEquals(1, JAVA_BYTE.byteSize(), "uint8_t");
        assertEquals(4, JAVA_FLOAT.byteSize(), "float");
        assertEquals(8, ADDRESS.byteSize(), "the pointers of an LP64 target");
    }

    @Test
    public void anyNonZeroByteIsTrueAndOnlyZeroIsFalse() {
        assertFalse(EglVendorShim.truth((byte) 0));
        assertTrue(EglVendorShim.truth((byte) 1));
        assertTrue(EglVendorShim.truth((byte) 2));
        assertTrue(EglVendorShim.truth((byte) -1));
        assertTrue(EglVendorShim.truth(Byte.MIN_VALUE));
    }

    @Test
    public void theConstantsThePlatformPasses() {
        assertEquals("monocle.egl.lib", EglVendorShim.LIBRARY_PROPERTY);
        assertEquals(0x30A0, EglVendorShim.EGL_OPENGL_ES_API, "EGL_OPENGL_ES_API");
        assertEquals("/dev/dri/card1", EglVendorShim.DEFAULT_DISPLAY_ID);
        assertEquals(16, EglVendorShim.CURSOR_WIDTH);
        assertEquals(16, EglVendorShim.CURSOR_HEIGHT);
    }
}
