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

package test.com.sun.pisces;

import com.sun.pisces.JavaSurface;
import com.sun.pisces.PiscesNativeShim;
import com.sun.pisces.PiscesRenderer;
import com.sun.pisces.RendererBase;
import com.sun.pisces.Transform6;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binding tests for {@code com.sun.pisces.PiscesNative}, the FFM facade over the {@code psw_*} C ABI of
 * {@code prism_sw} ({@code src/main/native-prism-sw/prism_sw_api.h}): symbol resolution, the ABI guard,
 * the {@code PswTransform6} layout against the C compiler's {@code sizeof}, the constants the two sides
 * share, handle lifecycles and the status-to-exception mapping.
 */
public class PiscesNativeTest {

    /** Every function {@code prism_sw_api.h} exports, in header order. */
    static final List<String> EXPORTED_SYMBOLS = List.of(
            "psw_abi_version", "psw_sizeof_transform6", "psw_constant",
            "psw_surface_create", "psw_surface_dispose", "psw_surface_get_rgb", "psw_surface_set_rgb",
            "psw_renderer_create", "psw_renderer_dispose",
            "psw_renderer_set_clip", "psw_renderer_set_color", "psw_renderer_set_composite_rule",
            "psw_renderer_set_linear_gradient", "psw_renderer_set_radial_gradient", "psw_renderer_set_texture",
            "psw_renderer_clear_rect", "psw_renderer_fill_rect", "psw_renderer_emit_and_clear_alpha_row",
            "psw_renderer_fill_alpha_mask", "psw_renderer_fill_lcd_alpha_mask", "psw_renderer_draw_image",
            "psw_lcd_gamma_set");

    /** {@code RendererBase} in the order {@code psw_constant} returns them. */
    static final int[] RENDERER_BASE_CONSTANTS = {
        RendererBase.COMPOSITE_CLEAR, RendererBase.COMPOSITE_SRC, RendererBase.COMPOSITE_SRC_OVER,
        RendererBase.TYPE_INT_ARGB_PRE,
        RendererBase.IMAGE_MODE_NORMAL, RendererBase.IMAGE_MODE_MULTIPLY,
        RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_PAD, RendererBase.IMAGE_FRAC_EDGE_TRIM,
    };

    @BeforeAll
    static void requireNatives() {
        PiscesNatives.require();
    }

    @Test
    public void facadeBindsEveryExportedSymbolAndNothingElse() {
        List<String> bound = PiscesNativeShim.boundSymbols();
        assertEquals(EXPORTED_SYMBOLS.size(), bound.size(), "bound symbols: " + bound);
        for (String name : EXPORTED_SYMBOLS) {
            assertTrue(bound.contains(name), "facade does not bind " + name);
        }
    }

    @Test
    public void everyExportedSymbolResolvesInTheLoadedLibrary() {
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        for (String name : EXPORTED_SYMBOLS) {
            assertTrue(lookup.find(name).isPresent(), "prism_sw does not export " + name);
        }
    }

    @Test
    public void abiVersionIsTheOneTheFacadeWasWrittenFor() {
        assertEquals(1, PiscesNativeShim.expectedAbiVersion());
        assertEquals(PiscesNativeShim.expectedAbiVersion(), PiscesNativeShim.abiVersion());
    }

    @Test
    public void transform6LayoutMatchesTheCStruct() {
        assertEquals(24, PiscesNativeShim.sizeofTransform6(), "sizeof(PswTransform6)");
        assertEquals(PiscesNativeShim.sizeofTransform6(), PiscesNativeShim.transform6LayoutByteSize());
        assertEquals(24, PiscesNativeShim.transform6Ints() * 4);
        String[] fields = {"m00", "m01", "m10", "m11", "m02", "m12"};
        for (int i = 0; i < fields.length; i++) {
            assertEquals(4L * i, PiscesNativeShim.transform6LayoutOffset(fields[i]), "offset of " + fields[i]);
        }
    }

    @Test
    public void transform6FillsTheScratchArrayInStructOrder() {
        Transform6 transform = new Transform6(11, 12, 21, 22, 13, 23);
        assertArrayEquals(new int[] {11, 12, 21, 22, 13, 23}, PiscesNativeShim.transform6ToInts(transform));
        assertArrayEquals(new int[] {1 << 16, 0, 0, 1 << 16, 0, 0}, PiscesNativeShim.transform6ToInts(new Transform6()),
                "identity in PswTransform6 order");
    }

    @Test
    public void constantsAgreeWithRendererBase() {
        assertEquals(RENDERER_BASE_CONSTANTS.length, PiscesNativeShim.constantCount());
        for (int i = 0; i < RENDERER_BASE_CONSTANTS.length; i++) {
            assertEquals(RENDERER_BASE_CONSTANTS[i], PiscesNativeShim.constant(i), "psw_constant(" + i + ")");
        }
        assertEquals(-1, PiscesNativeShim.constant(RENDERER_BASE_CONSTANTS.length));
        assertEquals(-1, PiscesNativeShim.constant(-1));
    }

    @Test
    public void surfaceAndRendererCreateAndDisposeInPairs() {
        for (int i = 0; i < 64; i++) {
            MemorySegment surface = PiscesNativeShim.surfaceCreate(RendererBase.TYPE_INT_ARGB_PRE, 8, 8);
            assertNotEquals(0L, surface.address(), "surface handle");
            MemorySegment renderer = PiscesNativeShim.rendererCreate(surface);
            assertNotEquals(0L, renderer.address(), "renderer handle");
            PiscesNativeShim.rendererSetClip(renderer, 0, 0, 8, 8);
            PiscesNativeShim.rendererFillRect(renderer, new int[64], 0, 0, 8 << 16, 8 << 16);
            // Either order is allowed: the renderer never touches surface memory on disposal.
            if ((i & 1) == 0) {
                PiscesNativeShim.rendererDispose(renderer);
                PiscesNativeShim.surfaceDispose(surface);
            } else {
                PiscesNativeShim.surfaceDispose(surface);
                PiscesNativeShim.rendererDispose(renderer);
            }
        }
    }

    @Test
    public void unsupportedImageTypeIsRejectedBeforeTheDowncall() {
        assertThrows(IllegalArgumentException.class,
                () -> PiscesNativeShim.surfaceCreate(RendererBase.TYPE_INT_ARGB_PRE + 1, 8, 8));
    }

    /**
     * {@code PSW_ERR_STATE} stands for a null renderer handle. The JNI version had no defined behaviour
     * here (a use after dispose dereferenced freed memory); disposal only ever runs from a
     * {@code Disposer} record after the peer is unreachable, so no caller can observe either.
     */
    @Test
    public void nullRendererHandleIsAnIllegalState() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> PiscesNativeShim.rendererSetClip(MemorySegment.NULL, 0, 0, 1, 1));
        assertEquals(PiscesNativeShim.stateMessage(), e.getMessage());
    }

    @Test
    public void disposingNullHandlesIsIgnored() {
        PiscesNativeShim.rendererDispose(MemorySegment.NULL);
        PiscesNativeShim.surfaceDispose(MemorySegment.NULL);
    }

    /**
     * The one range check only the C side makes: {@code AbstractSurface.rgbCheck} accepts an array that
     * holds the last row only up to {@code width}, the C check requires {@code offset + height *
     * scanLength} elements. The message is the one the JNI code passed to {@code ThrowNew} - which it
     * then never delivered, because its {@code JNI_ThrowNew} took the pending exception for a failure
     * and called {@code FatalError("Failed to throw an exception!")}. On the JNI build this input
     * aborted the JVM; the status-code ABI turns it into the exception that was always intended.
     */
    @Test
    public void getRGBBeyondTheBufferThrowsTheJniEraMessage() {
        JavaSurface surface = new JavaSurface(new int[64 * 64], RendererBase.TYPE_INT_ARGB_PRE, 64, 64);
        int[] tooShort = new int[15]; // Java: 0 + 10 * (2 - 1) + 5 = 15 <= 15 passes; C: 2 * 10 > 15
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> surface.getRGB(tooShort, 0, 10, 0, 0, 5, 2));
        assertEquals("Out of range access of buffer", e.getMessage());
    }

    /** As above for the write direction; the JNI text differed only in its first letter. */
    @Test
    public void setRGBBeyondTheBufferThrowsTheJniEraMessage() {
        JavaSurface surface = new JavaSurface(new int[64 * 64], RendererBase.TYPE_INT_ARGB_PRE, 64, 64);
        int[] tooShort = new int[15];
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> surface.setRGB(tooShort, 0, 10, 0, 0, 5, 2));
        assertEquals("out of range access of buffer", e.getMessage());
    }

    @Test
    public void javaSurfaceRejectsUnsupportedDataTypes() {
        assertThrows(IllegalArgumentException.class,
                () -> new JavaSurface(new int[64], RendererBase.TYPE_INT_ARGB_PRE + 1, 8, 8));
    }

    /**
     * A texture of width or height 0 passes {@code PiscesRenderer.inputImageCheck}, which rejects only
     * negative dimensions, and reaches the C guard of {@code psw_renderer_set_texture} - the JNI-era
     * {@code setTextureImpl} test that fell through to the OOM error when it failed. The status ABI
     * keeps that: {@code PSW_ERR_OOM}, so an {@link OutOfMemoryError} with the common text rather than
     * an {@code IllegalArgumentException}.
     */
    @Test
    public void setTextureWithAZeroDimensionReachesTheCGuardAndIsAnOutOfMemoryError() {
        PiscesRenderer pr = new PiscesRenderer(
                new JavaSurface(new int[64 * 64], RendererBase.TYPE_INT_ARGB_PRE, 64, 64));
        int[] texture = PiscesGoldenRenderTest.opaqueTexture();
        Transform6 identity = new Transform6();

        OutOfMemoryError zeroWidth = assertThrows(OutOfMemoryError.class, () -> pr.setTexture(
                RendererBase.TYPE_INT_ARGB_PRE, texture, 0, 16, 16, identity, false, false, false));
        assertEquals(PiscesNativeShim.oomMessage(), zeroWidth.getMessage());
        OutOfMemoryError zeroHeight = assertThrows(OutOfMemoryError.class, () -> pr.setTexture(
                RendererBase.TYPE_INT_ARGB_PRE, texture, 16, 0, 16, identity, false, false, false));
        assertEquals(PiscesNativeShim.oomMessage(), zeroHeight.getMessage());
    }

    /**
     * The width/height test of the JNI {@code surface_acquire} survives in {@code psw_surface_bind}, and
     * the facade is the only way to reach it: {@code AbstractSurface} rejects a negative size in Java,
     * so the descriptor is created directly. The first pixel-touching call fails with the generic
     * JNI-era text, before anything is bound or written.
     */
    @Test
    public void aSurfaceWithANegativeHeightIsRejectedByTheFirstPixelCall() {
        MemorySegment surface = PiscesNativeShim.surfaceCreate(RendererBase.TYPE_INT_ARGB_PRE, 8, -1);
        MemorySegment renderer = PiscesNativeShim.rendererCreate(surface);
        try {
            int[] pixels = new int[64];
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> PiscesNativeShim.rendererFillRect(renderer, pixels, 0, 0, 8 << 16, 8 << 16));
            assertEquals(PiscesNativeShim.argMessage(), e.getMessage());
            assertArrayEquals(new int[64], pixels, "a rejected call must not have touched the pixels");
        } finally {
            PiscesNativeShim.rendererDispose(renderer);
            PiscesNativeShim.surfaceDispose(surface);
        }
    }

    /**
     * A coverage row outside the clip is dropped whole: {@code psw_renderer_emit_and_clear_alpha_row}
     * tests {@code y} against the clip before it touches anything, so neither the surface nor the delta
     * array - which the in-clip path consumes and zeroes in place - changes.
     */
    @Test
    public void emitAndClearAlphaRowOutsideTheClipLeavesTheDeltasAndPixelsUntouched() {
        int[] pixels = new int[64 * 64];
        PiscesRenderer pr = new PiscesRenderer(new JavaSurface(pixels, RendererBase.TYPE_INT_ARGB_PRE, 64, 64));
        pr.setClip(0, 0, 64, 32);
        pr.setColor(10, 20, 30, 255);
        int[] deltas = new int[80];
        int width = PiscesGoldenRenderTest.fillRampDeltas(deltas, 4);
        int[] before = deltas.clone();

        pr.emitAndClearAlphaRow(PiscesGoldenRenderTest.alphaMap(), deltas, 40, 3, 3 + width - 1, 4, 0);

        assertArrayEquals(before, deltas, "a row outside the clip must leave the deltas untouched");
        assertArrayEquals(new int[64 * 64], pixels, "a row outside the clip must not render");
    }

    /**
     * {@code drawImage} with {@code hasAlpha == false} takes the opaque paint path, which the golden
     * script never exercises (it passes {@code true} throughout). On an opaque image it renders, and it
     * renders the same pixels as the {@code hasAlpha == true} path.
     */
    @Test
    public void drawImageWithoutAlphaRendersAsTheAlphaPathDoesForAnOpaqueImage() {
        int[] image = PiscesGoldenRenderTest.opaqueTexture();
        int[] withAlpha = drawOpaqueImage(image, true);
        int[] withoutAlpha = drawOpaqueImage(image, false);

        assertTrue(Arrays.stream(withoutAlpha).anyMatch(pixel -> pixel != 0),
                "drawImage with hasAlpha=false must render something");
        assertArrayEquals(withAlpha, withoutAlpha,
                "an opaque image must render identically with and without hasAlpha");
    }

    /** Draws the 16x16 {@code image} at the origin, 1:1, onto a fresh 64x64 surface and returns its pixels. */
    private static int[] drawOpaqueImage(int[] image, boolean hasAlpha) {
        int[] pixels = new int[64 * 64];
        PiscesRenderer pr = new PiscesRenderer(new JavaSurface(pixels, RendererBase.TYPE_INT_ARGB_PRE, 64, 64));
        pr.setClip(0, 0, 64, 64);
        pr.setColor(0, 0, 0, 255);
        pr.drawImage(RendererBase.TYPE_INT_ARGB_PRE, RendererBase.IMAGE_MODE_NORMAL, image, 16, 16, 0, 16,
                new Transform6(), false, false, 0, 0, 16 << 16, 16 << 16,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                RendererBase.IMAGE_FRAC_EDGE_KEEP, RendererBase.IMAGE_FRAC_EDGE_KEEP,
                0, 0, 15, 15, hasAlpha);
        return pixels;
    }
}
