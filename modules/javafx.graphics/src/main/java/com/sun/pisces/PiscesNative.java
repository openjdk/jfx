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

package com.sun.pisces;

import com.sun.glass.utils.NativeLibLoader;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * The single point of contact between {@code com.sun.pisces} and the {@code psw_*} C ABI exported by
 * the {@code prism_sw} library ({@code src/main/native-prism-sw/prism_sw_api.h}). It owns the library
 * load, the {@link SymbolLookup}, the {@link Linker}, every downcall handle and the one struct layout,
 * and it is the only class in this package that uses a restricted {@code java.lang.foreign} method.
 * <p>
 * The peers ({@link AbstractSurface}, {@link JavaSurface}, {@link PiscesRenderer}) call the typed
 * static wrappers below and hold their native handles as {@link MemorySegment}s. Every heap array the
 * C side reads or writes - the surface pixels, {@code argb} blocks, gradient ramps, textures, glyph
 * masks, Marlin's coverage map and delta row - crosses as {@link MemorySegment#ofArray} of the live
 * array under {@link Linker.Option#critical critical(true)}, so nothing is copied and the array is
 * pinned only for the duration of the call, exactly the span the JNI
 * {@code GetPrimitiveArrayCritical}/{@code Release...} pairs covered. The one write-back the C side
 * makes is {@link #rendererEmitAndClearAlphaRow} zeroing {@code alphaDeltas} in place, which
 * {@code com.sun.prism.sw.SWContext} relies on: the argument must be Marlin's live array, never a copy.
 * <p>
 * Nothing is thrown across the boundary. Every {@code psw_*} function returns a status, and
 * {@link #check} is the one place that turns it into the exception the JNI glue used to throw:
 * {@code PSW_ERR_OOM} into {@link OutOfMemoryError} with the JNI-era text, {@code PSW_ERR_ARG} into
 * {@link IllegalArgumentException} with the JNI-era text of the check that can still fail at that
 * call site, {@code PSW_ERR_STATE} (a null handle) into {@link IllegalStateException}.
 * <p>
 * Threading is unchanged from the JNI version: a renderer and its surface belong to one thread at a
 * time, and the LCD gamma tables behind {@link #lcdGammaSet} are process-global. Prism drives this
 * library from its single render thread.
 */
final class PiscesNative {

    /** The {@code psw_*} ABI revision this class is written against ({@code PSW_ABI_VERSION}). */
    static final int ABI_VERSION = 1;

    static final int PSW_OK = 0;
    static final int PSW_ERR_OOM = 1;
    static final int PSW_ERR_ARG = 2;
    static final int PSW_ERR_STATE = 3;

    /** How many values {@link #constant} exposes; they mirror {@link RendererBase} in declaration order. */
    static final int CONSTANT_COUNT = 9;

    /**
     * {@code PswTransform6}: six S15.16 ints in the {@link Transform6} field order, no padding. A test
     * compares {@code byteSize()} with {@link #sizeofTransform6()}. {@link Transform6#fill} writes a
     * scratch array in this order.
     */
    static final StructLayout TRANSFORM6_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("m00"), JAVA_INT.withName("m01"),
            JAVA_INT.withName("m10"), JAVA_INT.withName("m11"),
            JAVA_INT.withName("m02"), JAVA_INT.withName("m12"));

    /** Length of the {@code int[]} that stands in for a {@code PswTransform6}. */
    static final int TRANSFORM6_INTS = 6;

    /** The text the JNI glue used for every {@code mem_Error_Flag} failure. */
    static final String OOM_MESSAGE = "Allocation of internal renderer buffer failed.";

    /** The generic JNI-era {@code IllegalArgumentException} text, used where no more specific one applies. */
    static final String ARG_MESSAGE = "Illegal arguments";

    /**
     * The text for a null native handle. The JNI code had no equivalent: its only
     * {@code IllegalStateException} (empty message) reported a field-ID lookup failure at
     * {@code initialize()}, a condition that no longer exists, and a use after dispose would have
     * dereferenced freed memory.
     */
    static final String STATE_MESSAGE = "Pisces native peer is not initialized or has been disposed";

    private static final String LIBRARY_NAME = "prism_sw";

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;
    private static final List<String> BOUND_SYMBOLS = new ArrayList<>();

    static {
        // The library has to be loaded by this class loader before loaderLookup() can see it.
        // SWPipeline loads it as well; NativeLibLoader and System.load are both idempotent.
        NativeLibLoader.loadLibrary(LIBRARY_NAME);
        LOOKUP = SymbolLookup.loaderLookup();
    }

    /* Binding order matters: the ABI guard is bound and checked before any other symbol. */
    private static final MethodHandle PSW_ABI_VERSION = bind("psw_abi_version",
            FunctionDescriptor.of(JAVA_INT));

    static {
        checkAbiVersion();
    }

    private static final MethodHandle PSW_SIZEOF_TRANSFORM6 = bind("psw_sizeof_transform6",
            FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle PSW_CONSTANT = bind("psw_constant",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    private static final MethodHandle PSW_SURFACE_CREATE = bind("psw_surface_create",
            FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle PSW_SURFACE_DISPOSE = bind("psw_surface_dispose",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle PSW_SURFACE_GET_RGB = bind("psw_surface_get_rgb",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle PSW_SURFACE_SET_RGB = bind("psw_surface_set_rgb",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));

    private static final MethodHandle PSW_RENDERER_CREATE = bind("psw_renderer_create",
            FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle PSW_RENDERER_DISPOSE = bind("psw_renderer_dispose",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle PSW_RENDERER_SET_CLIP = bind("psw_renderer_set_clip",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle PSW_RENDERER_SET_COLOR = bind("psw_renderer_set_color",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle PSW_RENDERER_SET_COMPOSITE_RULE = bind("psw_renderer_set_composite_rule",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle PSW_RENDERER_SET_LINEAR_GRADIENT = bind("psw_renderer_set_linear_gradient",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT,
                    JAVA_INT, ADDRESS));
    private static final MethodHandle PSW_RENDERER_SET_RADIAL_GRADIENT = bind("psw_renderer_set_radial_gradient",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS,
                    JAVA_INT, JAVA_INT, ADDRESS));
    private static final MethodHandle PSW_RENDERER_SET_TEXTURE = bind("psw_renderer_set_texture",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle PSW_RENDERER_CLEAR_RECT = bind("psw_renderer_clear_rect",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle PSW_RENDERER_FILL_RECT = bind("psw_renderer_fill_rect",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle PSW_RENDERER_EMIT_AND_CLEAR_ALPHA_ROW =
            bind("psw_renderer_emit_and_clear_alpha_row",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT));
    private static final MethodHandle PSW_RENDERER_FILL_ALPHA_MASK = bind("psw_renderer_fill_alpha_mask",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT));
    private static final MethodHandle PSW_RENDERER_FILL_LCD_ALPHA_MASK = bind("psw_renderer_fill_lcd_alpha_mask",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT));
    private static final MethodHandle PSW_RENDERER_DRAW_IMAGE = bind("psw_renderer_draw_image",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));

    private static final MethodHandle PSW_LCD_GAMMA_SET = bind("psw_lcd_gamma_set",
            FunctionDescriptor.ofVoid(JAVA_FLOAT));

    private PiscesNative() {
    }

    /**
     * Resolves {@code name} and binds it with {@link Linker.Option#critical critical(true)}: every
     * {@code psw_*} function is short, integer-only, never blocks and never calls back, and the eleven
     * that take arrays need heap segments passed straight through.
     *
     * @throws UnsatisfiedLinkError if the library does not export {@code name}
     */
    @SuppressWarnings("restricted")
    private static MethodHandle bind(String name, FunctionDescriptor descriptor) {
        MemorySegment symbol = LOOKUP.find(name).orElseThrow(
                () -> new UnsatisfiedLinkError("missing native symbol: " + name + " in " + LIBRARY_NAME));
        BOUND_SYMBOLS.add(name);
        return LINKER.downcallHandle(symbol, descriptor, Linker.Option.critical(true));
    }

    private static void checkAbiVersion() {
        int actual;
        try {
            actual = (int) PSW_ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (actual != ABI_VERSION) {
            throw new UnsatisfiedLinkError(LIBRARY_NAME + " ABI version mismatch: expected " + ABI_VERSION
                    + ", found " + actual);
        }
    }

    private static Error unexpected(Throwable t) {
        if (t instanceof Error e) {
            return e;
        }
        if (t instanceof RuntimeException e) {
            throw e;
        }
        // Only linkage failures can land here: C cannot throw.
        return new AssertionError(t);
    }

    /**
     * The one place a {@code psw_*} status becomes an exception. None of them was ever observed under
     * JNI: {@code JNI_ThrowNew} called {@code ThrowNew} and then, finding {@code ExceptionCheck()} true
     * for the exception it had just raised, {@code FatalError}, so every argument, OOM and state failure
     * of {@code prism_sw} aborted the JVM; the status ABI delivers the exception that was always
     * intended.
     *
     * @param argumentMessage the JNI-era {@code IllegalArgumentException} text of the check that can
     *        still fail at the call site once the Java-side validation has run
     */
    private static void check(int status, String argumentMessage) {
        switch (status) {
            case PSW_OK:
                return;
            case PSW_ERR_OOM:
                throw new OutOfMemoryError(OOM_MESSAGE);
            case PSW_ERR_ARG:
                throw new IllegalArgumentException(argumentMessage);
            case PSW_ERR_STATE:
                throw new IllegalStateException(STATE_MESSAGE);
            default:
                throw new IllegalStateException("unknown " + LIBRARY_NAME + " status " + status);
        }
    }

    private static int flag(boolean value) {
        return value ? 1 : 0;
    }

    /* ---------------------------------------------------------------------------------------------
     * Test hooks (reached through PiscesNativeShim)
     * ------------------------------------------------------------------------------------------- */

    /** Runs the class initializer: loads the library, binds every symbol, checks the ABI version. */
    static void ensureLoaded() {
    }

    /** The symbols this class bound, in binding order. */
    static List<String> boundSymbols() {
        return Collections.unmodifiableList(new ArrayList<>(BOUND_SYMBOLS));
    }

    static int abiVersion() {
        try {
            return (int) PSW_ABI_VERSION.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static int sizeofTransform6() {
        try {
            return (int) PSW_SIZEOF_TRANSFORM6.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The C side's copy of the {@link RendererBase} constants, in declaration order; -1 out of range. */
    static int constant(int index) {
        try {
            return (int) PSW_CONSTANT.invokeExact(index);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Surfaces
     * ------------------------------------------------------------------------------------------- */

    /**
     * Creates the native descriptor of a {@code width x height} surface. The descriptor never owns
     * pixels: every call that touches them receives the surface's {@code int[]}.
     *
     * @throws IllegalArgumentException if {@code imageType} is not {@link RendererBase#TYPE_INT_ARGB_PRE},
     *         the only type the C side accepts
     * @throws OutOfMemoryError if the allocation failed
     */
    static MemorySegment surfaceCreate(int imageType, int width, int height) {
        if (imageType != RendererBase.TYPE_INT_ARGB_PRE) {
            throw new IllegalArgumentException("Unsupported image type: " + imageType);
        }
        MemorySegment surface;
        try {
            surface = (MemorySegment) PSW_SURFACE_CREATE.invokeExact(imageType, width, height);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (surface.address() == 0L) {
            throw new OutOfMemoryError(OOM_MESSAGE);
        }
        return surface;
    }

    static void surfaceDispose(MemorySegment surface) {
        try {
            PSW_SURFACE_DISPOSE.invokeExact(surface);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * Copies a {@code w x h} block of {@code pixels} into {@code argb}. After {@code AbstractSurface}'s
     * own range check the only C check that can still fail is the one on the end of {@code argb}, hence
     * the message.
     */
    static void surfaceGetRGB(MemorySegment surface, int[] pixels, int[] argb, int offset, int scanLength,
            int x, int y, int w, int h) {
        int status;
        try {
            status = (int) PSW_SURFACE_GET_RGB.invokeExact(surface, MemorySegment.ofArray(pixels),
                    MemorySegment.ofArray(argb), argb.length, offset, scanLength, x, y, w, h);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, "Out of range access of buffer");
    }

    /** The write direction of {@link #surfaceGetRGB}; the JNI message differed only in case. */
    static void surfaceSetRGB(MemorySegment surface, int[] pixels, int[] argb, int offset, int scanLength,
            int x, int y, int w, int h) {
        int status;
        try {
            status = (int) PSW_SURFACE_SET_RGB.invokeExact(surface, MemorySegment.ofArray(pixels),
                    MemorySegment.ofArray(argb), argb.length, offset, scanLength, x, y, w, h);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, "out of range access of buffer");
    }

    /* ---------------------------------------------------------------------------------------------
     * Renderer
     * ------------------------------------------------------------------------------------------- */

    /**
     * Creates a renderer bound to {@code surface}, which must outlive it. Disposal order of the two is
     * free: {@code psw_renderer_dispose} never touches surface memory.
     *
     * @throws OutOfMemoryError if the allocation failed, with the common {@link #OOM_MESSAGE}: the JNI
     *         renderer initialiser alone said {@code "...failed!!!"}, a text no caller could have seen
     *         (see {@link #check}) and that is not carried over
     */
    static MemorySegment rendererCreate(MemorySegment surface) {
        MemorySegment renderer;
        try {
            renderer = (MemorySegment) PSW_RENDERER_CREATE.invokeExact(surface);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        if (renderer.address() == 0L) {
            throw new OutOfMemoryError(OOM_MESSAGE);
        }
        return renderer;
    }

    static void rendererDispose(MemorySegment renderer) {
        try {
            PSW_RENDERER_DISPOSE.invokeExact(renderer);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    static void rendererSetClip(MemorySegment renderer, int minX, int minY, int width, int height) {
        int status;
        try {
            status = (int) PSW_RENDERER_SET_CLIP.invokeExact(renderer, minX, minY, width, height);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    static void rendererSetColor(MemorySegment renderer, int red, int green, int blue, int alpha) {
        int status;
        try {
            status = (int) PSW_RENDERER_SET_COLOR.invokeExact(renderer, red, green, blue, alpha);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    static void rendererSetCompositeRule(MemorySegment renderer, int compositeRule) {
        int status;
        try {
            status = (int) PSW_RENDERER_SET_COMPOSITE_RULE.invokeExact(renderer, compositeRule);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    /**
     * @param colors the 256-entry ramp of a {@link GradientColorMap}; copied by the C side
     * @param tx six ints in {@link #TRANSFORM6_LAYOUT} order; copied by the C side
     */
    static void rendererSetLinearGradient(MemorySegment renderer, int x0, int y0, int x1, int y1, int[] colors,
            int cycleMethod, int[] tx) {
        int status;
        try {
            status = (int) PSW_RENDERER_SET_LINEAR_GRADIENT.invokeExact(renderer, x0, y0, x1, y1,
                    MemorySegment.ofArray(colors), colors.length, cycleMethod, MemorySegment.ofArray(tx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    static void rendererSetRadialGradient(MemorySegment renderer, int cx, int cy, int fx, int fy, int radius,
            int[] colors, int cycleMethod, int[] tx) {
        int status;
        try {
            status = (int) PSW_RENDERER_SET_RADIAL_GRADIENT.invokeExact(renderer, cx, cy, fx, fy, radius,
                    MemorySegment.ofArray(colors), colors.length, cycleMethod, MemorySegment.ofArray(tx));
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    /** The texture ({@code width x height} ints from {@code data[0]}, row pitch {@code stride}) is copied. */
    static void rendererSetTexture(MemorySegment renderer, int imageType, int[] data, int width, int height,
            int stride, int[] tx, boolean repeat, boolean linearFiltering, boolean hasAlpha) {
        int status;
        try {
            status = (int) PSW_RENDERER_SET_TEXTURE.invokeExact(renderer, imageType, MemorySegment.ofArray(data),
                    data.length, width, height, stride, MemorySegment.ofArray(tx), flag(repeat),
                    flag(linearFiltering), flag(hasAlpha));
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    /** Integer surface coordinates. */
    static void rendererClearRect(MemorySegment renderer, int[] pixels, int x, int y, int w, int h) {
        int status;
        try {
            status = (int) PSW_RENDERER_CLEAR_RECT.invokeExact(renderer, MemorySegment.ofArray(pixels), x, y, w, h);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    /** S15.16 coordinates; fractional edges are anti-aliased. */
    static void rendererFillRect(MemorySegment renderer, int[] pixels, int x, int y, int w, int h) {
        int status;
        try {
            status = (int) PSW_RENDERER_FILL_RECT.invokeExact(renderer, MemorySegment.ofArray(pixels), x, y, w, h);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    /**
     * Composites one Marlin coverage row. {@code alphaDeltas} is consumed and zeroed in place from index
     * {@code xOff}; it has to be the live array, not a copy.
     */
    static void rendererEmitAndClearAlphaRow(MemorySegment renderer, int[] pixels, byte[] alphaMap,
            int[] alphaDeltas, int y, int xFrom, int xTo, int xOff, int rowNum) {
        int status;
        try {
            status = (int) PSW_RENDERER_EMIT_AND_CLEAR_ALPHA_ROW.invokeExact(renderer,
                    MemorySegment.ofArray(pixels), MemorySegment.ofArray(alphaMap),
                    MemorySegment.ofArray(alphaDeltas), y, xFrom, xTo, xOff, rowNum);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    static void rendererFillAlphaMask(MemorySegment renderer, int[] pixels, byte[] mask, int x, int y,
            int width, int height, int offset, int stride) {
        int status;
        try {
            status = (int) PSW_RENDERER_FILL_ALPHA_MASK.invokeExact(renderer, MemorySegment.ofArray(pixels),
                    MemorySegment.ofArray(mask), x, y, width, height, offset, stride);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    static void rendererFillLCDAlphaMask(MemorySegment renderer, int[] pixels, byte[] mask, int x, int y,
            int width, int height, int offset, int stride) {
        int status;
        try {
            status = (int) PSW_RENDERER_FILL_LCD_ALPHA_MASK.invokeExact(renderer, MemorySegment.ofArray(pixels),
                    MemorySegment.ofArray(mask), x, y, width, height, offset, stride);
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    /** The image is sampled while the call runs and detached before it returns; nothing is copied. */
    static void rendererDrawImage(MemorySegment renderer, int[] pixels, int imageType, int imageMode, int[] data,
            int width, int height, int offset, int stride, int[] tx, boolean repeat, boolean linearFiltering,
            int bboxX, int bboxY, int bboxW, int bboxH, int lEdge, int rEdge, int tEdge, int bEdge,
            int txMin, int tyMin, int txMax, int tyMax, boolean hasAlpha) {
        int status;
        try {
            status = (int) PSW_RENDERER_DRAW_IMAGE.invokeExact(renderer, MemorySegment.ofArray(pixels),
                    imageType, imageMode, MemorySegment.ofArray(data), width, height, offset, stride,
                    MemorySegment.ofArray(tx), flag(repeat), flag(linearFiltering),
                    bboxX, bboxY, bboxW, bboxH, lEdge, rEdge, tEdge, bEdge, txMin, tyMin, txMax, tyMax,
                    flag(hasAlpha));
        } catch (Throwable t) {
            throw unexpected(t);
        }
        check(status, ARG_MESSAGE);
    }

    /** Rebuilds the process-global LCD gamma tables if {@code gamma} differs from the current one. */
    static void lcdGammaSet(float gamma) {
        try {
            PSW_LCD_GAMMA_SET.invokeExact(gamma);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }
}
