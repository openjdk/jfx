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

package test.com.sun.prism.es2;

import com.sun.prism.es2.ES2NativeShim;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The token match {@code ES2Native.isExtensionSupported} took over from {@code isExtensionSupported} in
 * {@code GLFactory.c}, and the values the FFM port absorbed as constants from {@code nGetAdapterCount},
 * {@code nGetAdapterOrdinal} and {@code nGetIsGL2}.
 * <p>
 * Pure Java: nothing here loads {@code prism_es2}, so this class runs, and cannot skip, on every platform -
 * including the default Windows build, which leaves ES2 out and skips {@link ES2NativeTest}. What the match
 * decides is not small: MSAA ({@code ES2Pipeline}), non-power-of-two textures
 * ({@code GLFactory.isNPOTSupported}) and the BGRA8888 / {@code GL_APPLE_ycbcr_422} upload formats all hang
 * on it, and a boundary error there is a silent, session-wide quality regression that no rendering test
 * turns red.
 */
public class ES2ExtensionMatchTest {

    /** A {@code GL_EXTENSIONS} string in the shape drivers return it: space-separated, no trailing space. */
    private static final String EXTENSIONS = "GL_ARB_multisample GL_ARB_texture_float GL_EXT_bgra"
            + " GL_ARB_texture_non_power_of_two GL_APPLE_ycbcr_422";

    private static boolean supported(String allExtensions, String extension) {
        return ES2NativeShim.isExtensionSupported(allExtensions, extension);
    }

    @Test
    public void firstMiddleAndLastTokensMatch() {
        assertTrue(supported(EXTENSIONS, "GL_ARB_multisample"), "first token");
        assertTrue(supported(EXTENSIONS, "GL_EXT_bgra"), "middle token");
        assertTrue(supported(EXTENSIONS, "GL_ARB_texture_non_power_of_two"), "the NPOT gate");
        assertTrue(supported(EXTENSIONS, "GL_APPLE_ycbcr_422"), "last token");
    }

    @Test
    public void aSingleTokenMatchesWithAndWithoutATrailingSpace() {
        assertTrue(supported("GL_ARB_multisample", "GL_ARB_multisample"), "single token");
        assertTrue(supported("GL_ARB_multisample ", "GL_ARB_multisample"), "single token, trailing space");
        assertTrue(supported("GL_ARB_multisample GL_EXT_bgra ", "GL_EXT_bgra"), "last token before a trailing space");
    }

    @Test
    public void anEmptyOrSpaceContainingExtensionNeverMatches() {
        assertFalse(supported(EXTENSIONS, ""), "empty extension");
        assertFalse(supported(EXTENSIONS, "GL_ARB_multisample GL_ARB_texture_float"),
                "an extension containing a space is rejected even though the substring occurs");
        assertFalse(supported(EXTENSIONS, " "), "a lone space");
        assertFalse(supported("", "GL_ARB_multisample"), "an empty extension string");
        assertFalse(supported(null, "GL_ARB_multisample"), "no extension string at all");
        assertFalse(supported(EXTENSIONS, null), "no extension at all");
    }

    @Test
    public void aPrefixOfALongerTokenDoesNotMatch() {
        assertFalse(supported(EXTENSIONS, "GL_ARB_texture"),
                "GL_ARB_texture must not match inside GL_ARB_texture_float");
        assertFalse(supported("GL_ARB_multisample_coverage", "GL_ARB_multisample"), "unbounded on the right");
        assertFalse(supported("XGL_EXT_bgra", "GL_EXT_bgra"), "unbounded on the left");
        assertFalse(supported(EXTENSIONS, "GL_EXT_bgr"), "a shorter name");
    }

    @Test
    public void theSearchContinuesPastAnUnboundedPrefixMatch() {
        assertTrue(supported("GL_ARB_multisample_coverage GL_ARB_multisample", "GL_ARB_multisample"),
                "the first occurrence is a prefix of a longer token; the loop must go on to the real one");
        assertTrue(supported("GL_ARB_texture_float GL_ARB_texture", "GL_ARB_texture"));
        assertTrue(supported("GL_ARB_multisample_coverage GL_ARB_multisample_x GL_ARB_multisample",
                "GL_ARB_multisample"), "two unbounded occurrences before the bounded one");
        assertTrue(supported("XGL_EXT_bgra GL_EXT_bgra", "GL_EXT_bgra"), "past a left-unbounded occurrence");
    }

    @Test
    public void theAbsorbedAdapterAndProfileConstantsAreTheOnesTheNativesReturned() {
        assertEquals(1, ES2NativeShim.x11AdapterCount(), "X11 nGetAdapterCount always returned 1 (JDK-8091992)");
        assertEquals(0, ES2NativeShim.x11AdapterOrdinal(0L), "X11 nGetAdapterOrdinal always returned 0");
        assertEquals(0, ES2NativeShim.x11AdapterOrdinal(0x7f00_0000_1234L), "for any screen handle");
        assertEquals(1, ES2NativeShim.winAdapterCount(), "WGL nGetAdapterCount always returned 1");
        assertEquals(0, ES2NativeShim.winAdapterOrdinal(0L), "WGL nGetAdapterOrdinal always returned 0");
        assertEquals(0, ES2NativeShim.winAdapterOrdinal(0x7f00_0000_1234L), "for any screen handle");
        assertEquals(1, ES2NativeShim.macAdapterCount(), "NSOpenGL nGetAdapterCount always returned 1");
        assertEquals(0, ES2NativeShim.macAdapterOrdinal(0L), "NSOpenGL nGetAdapterOrdinal always returned 0");
        assertTrue(ES2NativeShim.desktopIsGL2(), "nGetIsGL2 answered true on X11, WGL and NSOpenGL: the desktop"
                + " ES2 pipe is the GL2 profile, never GLES2");
    }
}
