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

package com.sun.webkit.graphics;

import com.sun.webkit.SharedBuffer;
import com.sun.webkit.WKJStringCodec;
import com.sun.webkit.WebKitNative;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The {@code WKJHostGraphics} group: the 69 upcalls of
 * {@code Source/WebCore/platform/graphics/java} against {@link WCGraphicsManager}, {@link Ref},
 * {@link WCRenderQueue}, {@link WCPath}, {@link WCFont}, {@link WCFontCustomPlatformData},
 * {@link WCTextRun}, {@link WCImage}, {@link WCImageDecoder} and {@link WCImageFrame}. All 69 are
 * filled.
 * <p>
 * The slots that were instance methods on {@code WCGraphicsManager} take no target ref, because the
 * manager is a process singleton on the Java side; {@code PL_GetGraphicsManager()} disappeared with
 * the table. A manager that has not been set yet answers the documented default rather than
 * throwing, which is what the JNI code did when the static field read produced null.
 * <p>
 * <b>Arrays.</b> Every {@code int[]}, {@code float[]} and {@code WCRectangle} the JNI code
 * allocated and read back is now a caller-provided out parameter: the slot writes it and returns 1,
 * or returns 0 and writes nothing when Java answered {@code null}. Two of them -
 * {@code font_get_glyph_bounding_box} and {@code text_run_get_glyph_pos_and_advance} - close a
 * crash rather than changing a result, because the JNI version dereferenced the returned array
 * without testing it.
 * <p>
 * <b>Threading.</b> The ten {@code image_decoder_} slots and {@code ref_deref} are reached from
 * threads other than the main one: {@code BitmapImage} drives {@code ImageDecoderJava} from a
 * {@code WorkQueue}, and {@code ~RQRef} runs wherever the last reference dies. Their stubs come
 * from the one process-wide upcall arena, which is what makes that safe; it was already true of the
 * JNI version, which held a global ref.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation.
 */
public final class GraphicsUpcalls {

    /** {@code Ref.getID()} answers this when there is no {@code Ref}: {@code RQRef}'s unresolved. */
    private static final int REF_ID_UNRESOLVED = -1;

    private GraphicsUpcalls() {
    }

    /**
     * Fills the {@code graphics} group of a {@code WKJHost} table under construction.
     *
     * @param host the table
     */
    public static void install(MemorySegment host) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        // WCGraphicsManager, the singleton: no target ref.
        slot(host, lookup, "create_path", "createPath", FunctionDescriptor.of(JAVA_LONG));
        slot(host, lookup, "copy_path", "copyPath",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "create_rt_image", "createRtImage",
                FunctionDescriptor.of(JAVA_LONG, JAVA_INT, JAVA_INT));
        slot(host, lookup, "create_buffered_context_rq", "createBufferedContextRq",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "create_transform", "createTransform",
                FunctionDescriptor.of(JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE,
                        JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE));
        slot(host, lookup, "get_font", "getFont",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT,
                        JAVA_FLOAT));
        slot(host, lookup, "get_image_decoder", "getImageDecoder",
                FunctionDescriptor.of(JAVA_LONG));
        slot(host, lookup, "create_font_custom_platform_data", "createFontCustomPlatformData",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "create_shared_buffer", "createSharedBuffer",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "load_from_resource", "loadFromResource",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_LONG));
        slot(host, lookup, "create_frame", "createFrame",
                FunctionDescriptor.of(JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG));

        // Ref.
        slot(host, lookup, "ref_get_id", "refGetId", FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "ref_ref", "refRef", FunctionDescriptor.ofVoid(JAVA_LONG));
        slot(host, lookup, "ref_deref", "refDeref", FunctionDescriptor.ofVoid(JAVA_LONG));

        // WCRenderQueue.
        slot(host, lookup, "rq_flush", "rqFlush", FunctionDescriptor.ofVoid(JAVA_LONG));
        slot(host, lookup, "rq_dispose_graphics", "rqDisposeGraphics",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        slot(host, lookup, "rq_add_buffer", "rqAddBuffer",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, ADDRESS, JAVA_INT));
        slot(host, lookup, "rq_ref_int_array", "rqRefIntArray",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT));
        slot(host, lookup, "rq_ref_float_array", "rqRefFloatArray",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT));

        // WCPath.
        slot(host, lookup, "path_move_to", "pathMoveTo",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE));
        slot(host, lookup, "path_add_line_to", "pathAddLineTo",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE));
        slot(host, lookup, "path_add_quad_curve_to", "pathAddQuadCurveTo",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE,
                        JAVA_DOUBLE));
        slot(host, lookup, "path_add_bezier_curve_to", "pathAddBezierCurveTo",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE,
                        JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE));
        slot(host, lookup, "path_add_arc_to", "pathAddArcTo",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE,
                        JAVA_DOUBLE, JAVA_DOUBLE));
        slot(host, lookup, "path_add_arc", "pathAddArc",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE,
                        JAVA_DOUBLE, JAVA_DOUBLE, JAVA_INT));
        slot(host, lookup, "path_add_ellipse", "pathAddEllipse",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE,
                        JAVA_DOUBLE));
        slot(host, lookup, "path_add_rect", "pathAddRect",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE,
                        JAVA_DOUBLE));
        slot(host, lookup, "path_add_path", "pathAddPath",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "path_close_subpath", "pathCloseSubpath",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        slot(host, lookup, "path_is_empty", "pathIsEmpty",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "path_transform", "pathTransform",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE,
                        JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE));
        slot(host, lookup, "path_contains", "pathContains",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE));
        slot(host, lookup, "path_stroke_contains", "pathStrokeContains",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE,
                        JAVA_DOUBLE, JAVA_INT, JAVA_INT, JAVA_DOUBLE, ADDRESS, JAVA_INT));
        slot(host, lookup, "path_get_bounds", "pathGetBounds",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS));

        // WCFont.
        slot(host, lookup, "font_get_x_height", "fontGetXHeight",
                FunctionDescriptor.of(JAVA_FLOAT, JAVA_LONG));
        slot(host, lookup, "font_get_cap_height", "fontGetCapHeight",
                FunctionDescriptor.of(JAVA_FLOAT, JAVA_LONG));
        slot(host, lookup, "font_get_ascent", "fontGetAscent",
                FunctionDescriptor.of(JAVA_FLOAT, JAVA_LONG));
        slot(host, lookup, "font_get_descent", "fontGetDescent",
                FunctionDescriptor.of(JAVA_FLOAT, JAVA_LONG));
        slot(host, lookup, "font_get_line_spacing", "fontGetLineSpacing",
                FunctionDescriptor.of(JAVA_FLOAT, JAVA_LONG));
        slot(host, lookup, "font_get_line_gap", "fontGetLineGap",
                FunctionDescriptor.of(JAVA_FLOAT, JAVA_LONG));
        slot(host, lookup, "font_has_uniform_line_metrics", "fontHasUniformLineMetrics",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "font_get_glyph_width", "fontGetGlyphWidth",
                FunctionDescriptor.of(JAVA_DOUBLE, JAVA_LONG, JAVA_INT));
        slot(host, lookup, "font_get_glyph_bounding_box", "fontGetGlyphBoundingBox",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS));
        slot(host, lookup, "font_get_glyph_codes", "fontGetGlyphCodes",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
        slot(host, lookup, "font_derive", "fontDerive",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_FLOAT));
        slot(host, lookup, "font_equals", "fontEquals",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "font_hash_code", "fontHashCode",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "font_get_text_runs", "fontGetTextRuns",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
        slot(host, lookup, "font_custom_data_create_font", "fontCustomDataCreateFont",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT));

        // WCTextRun.
        slot(host, lookup, "text_run_is_left_to_right", "textRunIsLeftToRight",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "text_run_get_glyph_count", "textRunGetGlyphCount",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "text_run_get_start", "textRunGetStart",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "text_run_get_end", "textRunGetEnd",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "text_run_get_char_offset", "textRunGetCharOffset",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT));
        slot(host, lookup, "text_run_get_glyph", "textRunGetGlyph",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT));
        slot(host, lookup, "text_run_get_glyph_pos_and_advance", "textRunGetGlyphPosAndAdvance",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS));

        // WCImage.
        slot(host, lookup, "image_to_data", "imageToData",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT,
                        ADDRESS));
        slot(host, lookup, "image_get_pixel_buffer", "imageGetPixelBuffer",
                FunctionDescriptor.of(ADDRESS, JAVA_LONG, ADDRESS));
        slot(host, lookup, "image_draw_pixel_buffer", "imageDrawPixelBuffer",
                FunctionDescriptor.ofVoid(JAVA_LONG));

        // WCImageDecoder.
        slot(host, lookup, "image_decoder_destroy", "imageDecoderDestroy",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        slot(host, lookup, "image_decoder_add_image_data", "imageDecoderAddImageData",
                FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));
        slot(host, lookup, "image_decoder_get_image_size", "imageDecoderGetImageSize",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS));
        slot(host, lookup, "image_decoder_get_frame_count", "imageDecoderGetFrameCount",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "image_decoder_get_frame", "imageDecoderGetFrame",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT));
        slot(host, lookup, "image_decoder_get_frame_duration", "imageDecoderGetFrameDuration",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT));
        slot(host, lookup, "image_decoder_get_frame_size", "imageDecoderGetFrameSize",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS));
        slot(host, lookup, "image_decoder_get_frame_complete", "imageDecoderGetFrameComplete",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT));
        slot(host, lookup, "image_decoder_get_filename_extension",
                "imageDecoderGetFilenameExtension",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));

        // WCImageFrame.
        slot(host, lookup, "image_frame_get_size", "imageFrameGetSize",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS));
    }

    private static void slot(MemorySegment host, MethodHandles.Lookup lookup, String name,
                             String method, FunctionDescriptor descriptor) {
        WebKitNative.installHostSlot(host, "graphics." + name, lookup, method, descriptor);
    }

    // ------------------------------------------------------------------- WCGraphicsManager

    /* createWCPath(). Default when NULL: 0. */
    private static long createPath() {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            return manager == null ? 0L : WebKitNative.register(manager.createWCPath());
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.create_path", t);
            return 0L;
        }
    }

    /* createWCPath(WCPath). Default when NULL: 0. */
    private static long copyPath(long path) {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            if (manager == null || !(WebKitNative.lookup(path) instanceof WCPath source)) {
                return 0L;
            }
            return WebKitNative.register(manager.createWCPath(source));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.copy_path", t);
            return 0L;
        }
    }

    /* createRTImage(int, int). Default when NULL: 0. */
    private static long createRtImage(int width, int height) {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            return manager == null ? 0L
                    : WebKitNative.register(manager.createRTImage(width, height));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.create_rt_image", t);
            return 0L;
        }
    }

    /* createBufferedContextRQ(WCImage). Default when NULL: 0. */
    private static long createBufferedContextRq(long image) {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            if (manager == null || !(WebKitNative.lookup(image) instanceof WCImage target)) {
                return 0L;
            }
            return WebKitNative.register(manager.createBufferedContextRQ(target));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.create_buffered_context_rq", t);
            return 0L;
        }
    }

    /* createTransform(double x 6). Default when NULL: 0. */
    private static long createTransform(double m00, double m10, double m01, double m11,
                                        double m02, double m12) {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            return manager == null ? 0L
                    : WebKitNative.register(manager.createTransform(m00, m10, m01, m11, m02, m12));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.create_transform", t);
            return 0L;
        }
    }

    /*
     * getWCFont(String name, boolean bold, boolean italic, float size). The argument order is the
     * Java one; FontPlatformDataJava's own helper takes (family, size, italic, bold) and reorders at
     * the call, as it always did. Default when NULL: 0.
     */
    private static long getFont(MemorySegment family, int familyLength, int bold, int italic,
                                float size) {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            if (manager == null) {
                return 0L;
            }
            String name = WebKitNative.readString(family, familyLength);
            return WebKitNative.register(manager.getWCFont(name, bold != 0, italic != 0, size));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.get_font", t);
            return 0L;
        }
    }

    /* getImageDecoder(). Default when NULL: 0. */
    private static long getImageDecoder() {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            return manager == null ? 0L : WebKitNative.register(manager.getImageDecoder());
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.get_image_decoder", t);
            return 0L;
        }
    }

    /* fwkCreateFontCustomPlatformData(SharedBuffer). Default when NULL: 0. */
    private static long createFontCustomPlatformData(long sharedBuffer) {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            if (manager == null
                    || !(WebKitNative.lookup(sharedBuffer) instanceof SharedBuffer buffer)) {
                return 0L;
            }
            return WebKitNative.register(manager.fwkCreateFontCustomPlatformData(buffer));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.create_font_custom_platform_data", t);
            return 0L;
        }
    }

    /*
     * com.sun.webkit.SharedBuffer.fwkCreate(long) - a static factory, not a WCGraphicsManager
     * method. It is in this group because its one caller is
     * graphics/java/FontCustomPlatformData.cpp. Default when NULL: 0.
     */
    private static long createSharedBuffer(long buffer) {
        try {
            return buffer == 0L ? 0L : WebKitNative.register(SharedBuffer.fwkCreate(buffer));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.create_shared_buffer", t);
            return 0L;
        }
    }

    /*
     * fwkLoadFromResource(String, long). Reached only from the !USE(IMAGEIO) branch of
     * BitmapImage::createFromName, which this build does not compile; filled so that the branch
     * stays translatable. Default when NULL: no-op.
     */
    private static void loadFromResource(MemorySegment name, int nameLength, long buffer) {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            String key = WebKitNative.readString(name, nameLength);
            if (manager != null && key != null) {
                manager.fwkLoadFromResource(key, buffer);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.load_from_resource", t);
        }
    }

    /*
     * createFrame(int, int, ByteBuffer). Also !USE(IMAGEIO) only. The bytes belong to the caller and
     * are valid for the duration of the call, so the segment is wrapped rather than copied.
     * Default when NULL: 0.
     */
    private static long createFrame(int width, int height, MemorySegment data, long length) {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            if (manager == null) {
                return 0L;
            }
            ByteBuffer bytes = data.address() == 0L || length <= 0L
                    ? null
                    : WebKitNative.resize(data, length).asByteBuffer();
            return WebKitNative.register(manager.createFrame(width, height, bytes));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.create_frame", t);
            return 0L;
        }
    }

    // ----------------------------------------------------------------------------- Ref

    /*
     * Ref.getID(). The id is assigned by WCGraphicsManager.createID() on the Java side and is what
     * the command buffer carries, so no registry entry is minted for it here.
     * Default when NULL: -1, which is RQRef's "not yet resolved" value.
     */
    private static int refGetId(long ref) {
        try {
            return WebKitNative.lookup(ref) instanceof Ref target
                    ? target.getID()
                    : REF_ID_UNRESOLVED;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.ref_get_id", t);
            return REF_ID_UNRESOLVED;
        }
    }

    /* Ref.ref(). Adds the object to WCGraphicsManager.refMap. Default when NULL: no-op. */
    private static void refRef(long ref) {
        try {
            if (WebKitNative.lookup(ref) instanceof Ref target) {
                target.ref();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.ref_ref", t);
        }
    }

    /*
     * Ref.deref(). Called from ~RQRef, which can run on any thread - the JNI version guarded on
     * GetJavaEnv() returning null after a VM detach and skipped the call. There is no equivalent
     * condition here, and Ref.deref is synchronized. Default when NULL: no-op.
     */
    private static void refDeref(long ref) {
        try {
            if (WebKitNative.lookup(ref) instanceof Ref target) {
                target.deref();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.ref_deref", t);
        }
    }

    // --------------------------------------------------------------------- WCRenderQueue

    /* fwkFlush(). Default when NULL: no-op. */
    private static void rqFlush(long rq) {
        try {
            if (WebKitNative.lookup(rq) instanceof WCRenderQueue target) {
                target.fwkFlush();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.rq_flush", t);
        }
    }

    /* fwkDisposeGraphics(). Called from ~RenderingQueue. Default when NULL: no-op. */
    private static void rqDisposeGraphics(long rq) {
        try {
            if (WebKitNative.lookup(rq) instanceof WCRenderQueue target) {
                target.fwkDisposeGraphics();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.rq_dispose_graphics", t);
        }
    }

    /*
     * fwkAddBuffer(ByteBuffer). The address is command buffer memory owned by the C++
     * WebCore::ByteBuffer; Java wraps it without copying, exactly as NewDirectByteBuffer did. The
     * returned id names that Java buffer object and is held by WebCore::ByteBuffer::m_nio_holder
     * until wkj_rq_release destroys it, so the buffer cannot be collected while the queue still
     * refers to it. Default when NULL: 0.
     */
    private static long rqAddBuffer(long rq, MemorySegment address, int length) {
        try {
            if (!(WebKitNative.lookup(rq) instanceof WCRenderQueue target)
                    || address.address() == 0L || length <= 0) {
                return 0L;
            }
            ByteBuffer buffer = WebKitNative.resize(address, length).asByteBuffer();
            target.fwkAddBuffer(buffer);
            return WebKitNative.register(buffer);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.rq_add_buffer", t);
            return 0L;
        }
    }

    /* refIntArr(int[]). Returns the slot id the command buffer stores. Default when NULL: 0. */
    private static int rqRefIntArray(long rq, MemorySegment data, int count) {
        try {
            int[] values = WebKitNative.readInts(data, count);
            if (values == null || !(WebKitNative.lookup(rq) instanceof WCRenderQueue target)) {
                return 0;
            }
            return target.refIntArr(values);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.rq_ref_int_array", t);
            return 0;
        }
    }

    /* refFloatArr(float[]). Returns the slot id the command buffer stores. Default when NULL: 0. */
    private static int rqRefFloatArray(long rq, MemorySegment data, int count) {
        try {
            float[] values = readFloats(data, count);
            if (values == null || !(WebKitNative.lookup(rq) instanceof WCRenderQueue target)) {
                return 0;
            }
            return target.refFloatArr(values);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.rq_ref_float_array", t);
            return 0;
        }
    }

    private static float[] readFloats(MemorySegment data, int count) {
        if (data.address() == 0L) {
            return null;
        }
        if (count <= 0) {
            return new float[0];
        }
        return WebKitNative.resize(data, (long) count * Float.BYTES).toArray(JAVA_FLOAT);
    }

    // ---------------------------------------------------------------------------- WCPath

    private static WCPath path(long path) {
        return WebKitNative.lookup(path) instanceof WCPath target ? target : null;
    }

    /* moveTo(double, double). Default when NULL: no-op. */
    private static void pathMoveTo(long path, double x, double y) {
        try {
            WCPath target = path(path);
            if (target != null) {
                target.moveTo(x, y);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_move_to", t);
        }
    }

    /* addLineTo(double, double). Default when NULL: no-op. */
    private static void pathAddLineTo(long path, double x, double y) {
        try {
            WCPath target = path(path);
            if (target != null) {
                target.addLineTo(x, y);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_add_line_to", t);
        }
    }

    /* addQuadCurveTo(double, double, double, double). Default when NULL: no-op. */
    private static void pathAddQuadCurveTo(long path, double cx, double cy, double x, double y) {
        try {
            WCPath target = path(path);
            if (target != null) {
                target.addQuadCurveTo(cx, cy, x, y);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_add_quad_curve_to", t);
        }
    }

    /* addBezierCurveTo(double x 6). Default when NULL: no-op. */
    private static void pathAddBezierCurveTo(long path, double c1x, double c1y, double c2x,
                                             double c2y, double x, double y) {
        try {
            WCPath target = path(path);
            if (target != null) {
                target.addBezierCurveTo(c1x, c1y, c2x, c2y, x, y);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_add_bezier_curve_to", t);
        }
    }

    /* addArcTo(double x 5). Default when NULL: no-op. */
    private static void pathAddArcTo(long path, double c1x, double c1y, double c2x, double c2y,
                                     double radius) {
        try {
            WCPath target = path(path);
            if (target != null) {
                target.addArcTo(c1x, c1y, c2x, c2y, radius);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_add_arc_to", t);
        }
    }

    /*
     * addArc(double x, double y, double r, double startAngle, double endAngle, boolean). The C
     * parameter is named "clockwise" and the Java one "aclockwise"; the value crosses unchanged, as
     * it did through JNI, because reinterpreting it here would be a rendering change rather than a
     * migration. Default when NULL: no-op.
     */
    private static void pathAddArc(long path, double cx, double cy, double radius,
                                   double startAngle, double endAngle, int clockwise) {
        try {
            WCPath target = path(path);
            if (target != null) {
                target.addArc(cx, cy, radius, startAngle, endAngle, clockwise != 0);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_add_arc", t);
        }
    }

    /* addEllipse(double x 4). Default when NULL: no-op. */
    private static void pathAddEllipse(long path, double x, double y, double width, double height) {
        try {
            WCPath target = path(path);
            if (target != null) {
                target.addEllipse(x, y, width, height);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_add_ellipse", t);
        }
    }

    /* addRect(double x 4). Default when NULL: no-op. */
    private static void pathAddRect(long path, double x, double y, double width, double height) {
        try {
            WCPath target = path(path);
            if (target != null) {
                target.addRect(x, y, width, height);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_add_rect", t);
        }
    }

    /* addPath(WCPath). Default when NULL: no-op. */
    private static void pathAddPath(long path, long other) {
        try {
            WCPath target = path(path);
            WCPath source = path(other);
            if (target != null && source != null) {
                target.addPath(source);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_add_path", t);
        }
    }

    /* closeSubpath(). Default when NULL: no-op. */
    private static void pathCloseSubpath(long path) {
        try {
            WCPath target = path(path);
            if (target != null) {
                target.closeSubpath();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_close_subpath", t);
        }
    }

    /* isEmpty(). Default when NULL: 0. */
    private static int pathIsEmpty(long path) {
        try {
            WCPath target = path(path);
            return target != null && target.isEmpty() ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_is_empty", t);
            return 0;
        }
    }

    /* transform(double x 6). Default when NULL: no-op. */
    private static void pathTransform(long path, double a, double b, double c, double d, double e,
                                      double f) {
        try {
            WCPath target = path(path);
            if (target != null) {
                target.transform(a, b, c, d, e, f);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_transform", t);
        }
    }

    /* contains(int rule, double, double). Default when NULL: 0. */
    private static int pathContains(long path, int rule, double x, double y) {
        try {
            WCPath target = path(path);
            return target != null && target.contains(rule, x, y) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_contains", t);
            return 0;
        }
    }

    /*
     * strokeContains(...). "dashes" may be NULL with dash_count 0, which is what a solid stroke
     * passed as a zero length array before, so a NULL pointer becomes a zero length array rather
     * than null. Default when NULL: 0.
     */
    private static int pathStrokeContains(long path, double x, double y, double thickness,
                                          double miterLimit, int lineCap, int lineJoin,
                                          double dashOffset, MemorySegment dashes, int dashCount) {
        try {
            WCPath target = path(path);
            if (target == null) {
                return 0;
            }
            double[] dashArray = WebKitNative.readDoubles(dashes, dashCount);
            return target.strokeContains(x, y, thickness, miterLimit, lineCap, lineJoin, dashOffset,
                    dashArray == null ? new double[0] : dashArray) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_stroke_contains", t);
            return 0;
        }
    }

    /*
     * getBounds(), formerly a WCRectangle whose four float fields the C++ read back with
     * GetFieldID/GetFloatField. Writes x, y, w, h and returns 1; returns 0 and writes nothing when
     * Java returned null, which PathJava turned into an empty FloatRect. Default when NULL: 0.
     */
    private static int pathGetBounds(long path, MemorySegment out) {
        try {
            WCPath target = path(path);
            return writeRectangle(out, target == null ? null : target.getBounds());
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.path_get_bounds", t);
            return 0;
        }
    }

    private static int writeRectangle(MemorySegment out, WCRectangle rectangle) {
        if (rectangle == null) {
            return 0;
        }
        float[] xywh = {
            rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight(),
        };
        return WebKitNative.writeFloats(out, xywh, 4) ? 1 : 0;
    }

    // ---------------------------------------------------------------------------- WCFont

    private static WCFont font(long font) {
        return WebKitNative.lookup(font) instanceof WCFont target ? target : null;
    }

    /* getXHeight(). Default when NULL: 0.0f. */
    private static float fontGetXHeight(long font) {
        try {
            WCFont target = font(font);
            return target == null ? 0.0f : target.getXHeight();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_get_x_height", t);
            return 0.0f;
        }
    }

    /* getCapHeight(). Default when NULL: 0.0f. */
    private static float fontGetCapHeight(long font) {
        try {
            WCFont target = font(font);
            return target == null ? 0.0f : target.getCapHeight();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_get_cap_height", t);
            return 0.0f;
        }
    }

    /* getAscent(). Default when NULL: 0.0f. */
    private static float fontGetAscent(long font) {
        try {
            WCFont target = font(font);
            return target == null ? 0.0f : target.getAscent();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_get_ascent", t);
            return 0.0f;
        }
    }

    /* getDescent(). Default when NULL: 0.0f. */
    private static float fontGetDescent(long font) {
        try {
            WCFont target = font(font);
            return target == null ? 0.0f : target.getDescent();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_get_descent", t);
            return 0.0f;
        }
    }

    /* getLineSpacing(). Default when NULL: 0.0f. */
    private static float fontGetLineSpacing(long font) {
        try {
            WCFont target = font(font);
            return target == null ? 0.0f : target.getLineSpacing();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_get_line_spacing", t);
            return 0.0f;
        }
    }

    /* getLineGap(). Default when NULL: 0.0f. */
    private static float fontGetLineGap(long font) {
        try {
            WCFont target = font(font);
            return target == null ? 0.0f : target.getLineGap();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_get_line_gap", t);
            return 0.0f;
        }
    }

    /* hasUniformLineMetrics(). Default when NULL: 0. */
    private static int fontHasUniformLineMetrics(long font) {
        try {
            WCFont target = font(font);
            return target != null && target.hasUniformLineMetrics() ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_has_uniform_line_metrics", t);
            return 0;
        }
    }

    /* getGlyphWidth(int) returns a Java double; the caller narrows to float as it always did. */
    private static double fontGetGlyphWidth(long font, int glyph) {
        try {
            WCFont target = font(font);
            return target == null ? 0.0 : target.getGlyphWidth(glyph);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_get_glyph_width", t);
            return 0.0;
        }
    }

    /*
     * getGlyphBoundingBox(int), formerly a float[4]. The JNI version did not test for null and
     * dereferenced the array unconditionally, so returning 0 here closes a crash rather than
     * changing a result. Default when NULL: 0.
     */
    private static int fontGetGlyphBoundingBox(long font, int glyph, MemorySegment out) {
        try {
            WCFont target = font(font);
            float[] box = target == null ? null : target.getGlyphBoundingBox(glyph);
            return WebKitNative.writeFloats(out, box, 4) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_get_glyph_bounding_box", t);
            return 0;
        }
    }

    /*
     * getGlyphCodes(char[]). Writes one int32_t glyph per input code unit and returns the number
     * written, or -1 when Java returned null - which GlyphPage::fill treats as "no glyphs".
     * Default when NULL: -1.
     */
    private static int fontGetGlyphCodes(long font, MemorySegment chars, int charCount,
                                         MemorySegment out, int capacity) {
        try {
            WCFont target = font(font);
            String text = WebKitNative.readString(chars, charCount);
            if (target == null || text == null) {
                return -1;
            }
            int[] glyphs = target.getGlyphCodes(text.toCharArray());
            if (glyphs == null) {
                return -1;
            }
            int count = Math.min(glyphs.length, capacity);
            if (count > 0 && !WebKitNative.writeInts(out, glyphs, count)) {
                return -1;
            }
            return count;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_get_glyph_codes", t);
            return -1;
        }
    }

    /* deriveFont(float). Default when NULL: 0. */
    private static long fontDerive(long font, float size) {
        try {
            WCFont target = font(font);
            return target == null ? 0L : WebKitNative.register(target.deriveFont(size));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_derive", t);
            return 0L;
        }
    }

    /* equals(Object) between two WCFonts. Default when NULL: 0. */
    private static int fontEquals(long font, long other) {
        try {
            WCFont target = font(font);
            return target != null && target.equals(WebKitNative.lookup(other)) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_equals", t);
            return 0;
        }
    }

    /* hashCode(). Default when NULL: 0. */
    private static int fontHashCode(long font) {
        try {
            WCFont target = font(font);
            return target == null ? 0 : target.hashCode();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_hash_code", t);
            return 0;
        }
    }

    /*
     * getTextRuns(String) -> WCTextRun[]. Returns the TOTAL number of runs so that a caller whose
     * buffer was too small can retry with the reported size; when the total exceeds out_cap NOTHING
     * is written and no id is minted, so a retry cannot leak. -1 when Java returned null, which
     * ComplexTextController turns into a missing glyph run. Default when NULL: -1.
     */
    private static int fontGetTextRuns(long font, MemorySegment text, int textLength,
                                       MemorySegment out, int capacity) {
        try {
            WCFont target = font(font);
            String value = WebKitNative.readString(text, textLength);
            if (target == null) {
                return -1;
            }
            WCTextRun[] runs = target.getTextRuns(value);
            if (runs == null) {
                return -1;
            }
            if (runs.length > capacity || out.address() == 0L) {
                return runs.length;
            }
            long[] refs = new long[runs.length];
            for (int i = 0; i < runs.length; i++) {
                refs[i] = WebKitNative.register(runs[i]);
            }
            if (!WebKitNative.writeLongs(out, refs, refs.length)) {
                for (long ref : refs) {
                    WebKitNative.release(ref);
                }
                return -1;
            }
            return runs.length;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_get_text_runs", t);
            return -1;
        }
    }

    /* createFont(int size, boolean bold, boolean italic). Default when NULL: 0. */
    private static long fontCustomDataCreateFont(long data, int size, int bold, int italic) {
        try {
            if (!(WebKitNative.lookup(data) instanceof WCFontCustomPlatformData target)) {
                return 0L;
            }
            return WebKitNative.register(target.createFont(size, bold != 0, italic != 0));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.font_custom_data_create_font", t);
            return 0L;
        }
    }

    // ------------------------------------------------------------------------- WCTextRun

    private static WCTextRun textRun(long run) {
        return WebKitNative.lookup(run) instanceof WCTextRun target ? target : null;
    }

    /* isLeftToRight(). Default when NULL: 0. */
    private static int textRunIsLeftToRight(long run) {
        try {
            WCTextRun target = textRun(run);
            return target != null && target.isLeftToRight() ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.text_run_is_left_to_right", t);
            return 0;
        }
    }

    /* getGlyphCount(). Default when NULL: 0. */
    private static int textRunGetGlyphCount(long run) {
        try {
            WCTextRun target = textRun(run);
            return target == null ? 0 : target.getGlyphCount();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.text_run_get_glyph_count", t);
            return 0;
        }
    }

    /* getStart(). Default when NULL: 0. */
    private static int textRunGetStart(long run) {
        try {
            WCTextRun target = textRun(run);
            return target == null ? 0 : target.getStart();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.text_run_get_start", t);
            return 0;
        }
    }

    /* getEnd(). Default when NULL: 0. */
    private static int textRunGetEnd(long run) {
        try {
            WCTextRun target = textRun(run);
            return target == null ? 0 : target.getEnd();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.text_run_get_end", t);
            return 0;
        }
    }

    /* getCharOffset(int). Default when NULL: 0. */
    private static int textRunGetCharOffset(long run, int glyphIndex) {
        try {
            WCTextRun target = textRun(run);
            return target == null ? 0 : target.getCharOffset(glyphIndex);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.text_run_get_char_offset", t);
            return 0;
        }
    }

    /* getGlyph(int). Default when NULL: 0. */
    private static int textRunGetGlyph(long run, int glyphIndex) {
        try {
            WCTextRun target = textRun(run);
            return target == null ? 0 : target.getGlyph(glyphIndex);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.text_run_get_glyph", t);
            return 0;
        }
    }

    /*
     * getGlyphPosAndAdvance(int), formerly a float[4]. As with font_get_glyph_bounding_box the JNI
     * version dereferenced unconditionally. Default when NULL: 0.
     */
    private static int textRunGetGlyphPosAndAdvance(long run, int glyphIndex, MemorySegment out) {
        try {
            WCTextRun target = textRun(run);
            float[] values = target == null ? null : target.getGlyphPosAndAdvance(glyphIndex);
            return WebKitNative.writeFloats(out, values, 4) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.text_run_get_glyph_pos_and_advance", t);
            return 0;
        }
    }

    // --------------------------------------------------------------------------- WCImage

    /*
     * toData(String mimeType) -> byte[], through the contract 13 buffer protocol on bytes. A retry
     * re-encodes the image on the Java side. Default when NULL: WKJ_STR_NULL.
     */
    private static int imageToData(long image, MemorySegment mimeType, int mimeTypeLength,
                                   MemorySegment out, int capacity, MemorySegment length) {
        try {
            byte[] data = WebKitNative.lookup(image) instanceof WCImage target
                    ? target.toData(WebKitNative.readString(mimeType, mimeTypeLength))
                    : null;
            return WebKitNative.emitBytes(data, out, capacity, length);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_to_data", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }

    /*
     * getPixelBuffer() -> ByteBuffer. Returns the address of the image's BGRA backing store and its
     * capacity in bytes, or NULL with *out_capacity = 0. The memory belongs to the Java image and
     * stays valid for as long as the image does, which is the same lifetime the JNI version relied
     * on when it let the local ref to the ByteBuffer die while keeping the address. A heap buffer
     * has no address at all and answers NULL rather than throwing. Default when NULL: NULL.
     */
    private static MemorySegment imageGetPixelBuffer(long image, MemorySegment outCapacity) {
        try {
            ByteBuffer buffer = WebKitNative.lookup(image) instanceof WCImage target
                    ? target.getPixelBuffer()
                    : null;
            if (buffer == null || !buffer.isDirect()) {
                WebKitNative.writeLong(outCapacity, 0L);
                return MemorySegment.NULL;
            }
            // A duplicate that has been cleared, so that the address and the size are the base and
            // the capacity of the buffer rather than its current position and remaining count -
            // which is what GetDirectBufferAddress and GetDirectBufferCapacity answered. The
            // duplicate shares the content, so nothing is copied and the caller's position is left
            // where it was.
            MemorySegment pixels = MemorySegment.ofBuffer(buffer.duplicate().clear());
            WebKitNative.writeLong(outCapacity, pixels.byteSize());
            return pixels;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_get_pixel_buffer", t);
            WebKitNative.writeLong(outCapacity, 0L);
            return MemorySegment.NULL;
        }
    }

    /* drawPixelBuffer(). Default when NULL: no-op. */
    private static void imageDrawPixelBuffer(long image) {
        try {
            if (WebKitNative.lookup(image) instanceof WCImage target) {
                target.drawPixelBuffer();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_draw_pixel_buffer", t);
        }
    }

    // -------------------------------------------------------------------- WCImageDecoder

    private static WCImageDecoder decoder(long decoder) {
        return WebKitNative.lookup(decoder) instanceof WCImageDecoder target ? target : null;
    }

    /* destroy(). Called from ~ImageDecoderJava. Default when NULL: no-op. */
    private static void imageDecoderDestroy(long decoder) {
        try {
            WCImageDecoder target = decoder(decoder);
            if (target != null) {
                target.destroy();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_decoder_destroy", t);
        }
    }

    /*
     * addImageData(byte[]). A NULL pointer with length 0 is the end of stream call the JNI code made
     * by passing a null array, and reaches Java as null. Default when NULL: no-op.
     */
    private static void imageDecoderAddImageData(long decoder, MemorySegment data, int length) {
        try {
            WCImageDecoder target = decoder(decoder);
            if (target != null) {
                target.addImageData(WebKitNative.readBytes(data, length));
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_decoder_add_image_data", t);
        }
    }

    /* getImageSize() -> int[2]. Default when NULL: 0. */
    private static int imageDecoderGetImageSize(long decoder, MemorySegment out) {
        try {
            WCImageDecoder target = decoder(decoder);
            return WebKitNative.writeInts(out, target == null ? null : target.getImageSize(), 2)
                    ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_decoder_get_image_size", t);
            return 0;
        }
    }

    /* getFrameCount(). Default when NULL: 0, and the caller clamps to at least 1. */
    private static int imageDecoderGetFrameCount(long decoder) {
        try {
            WCImageDecoder target = decoder(decoder);
            return target == null ? 0 : target.getFrameCount();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_decoder_get_frame_count", t);
            return 0;
        }
    }

    /* getFrame(int) -> WCImageFrame. Default when NULL: 0. */
    private static long imageDecoderGetFrame(long decoder, int index) {
        try {
            WCImageDecoder target = decoder(decoder);
            return target == null ? 0L : WebKitNative.register(target.getFrame(index));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_decoder_get_frame", t);
            return 0L;
        }
    }

    /* getFrameDuration(int), in milliseconds. Default when NULL: 0. */
    private static int imageDecoderGetFrameDuration(long decoder, int index) {
        try {
            WCImageDecoder target = decoder(decoder);
            return target == null ? 0 : target.getFrameDuration(index);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_decoder_get_frame_duration", t);
            return 0;
        }
    }

    /*
     * getFrameSize(int) -> int[2]. Returns 0 and writes nothing when Java returned null, which the
     * caller turns into the whole image size. Default when NULL: 0.
     */
    private static int imageDecoderGetFrameSize(long decoder, int index, MemorySegment out) {
        try {
            WCImageDecoder target = decoder(decoder);
            return WebKitNative.writeInts(out, target == null ? null : target.getFrameSize(index), 2)
                    ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_decoder_get_frame_size", t);
            return 0;
        }
    }

    /* getFrameCompleteStatus(int). Default when NULL: 0. */
    private static int imageDecoderGetFrameComplete(long decoder, int index) {
        try {
            WCImageDecoder target = decoder(decoder);
            return target != null && target.getFrameCompleteStatus(index) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_decoder_get_frame_complete", t);
            return 0;
        }
    }

    /*
     * getFilenameExtension() -> String. WKJ_STR_NULL and WKJ_STR_OK with length 0 both become the
     * empty WTF::String in the caller, because String(env, jstring) always collapsed a null jstring
     * to StringImpl::empty(). Default when NULL: WKJ_STR_NULL.
     */
    private static int imageDecoderGetFilenameExtension(long decoder, MemorySegment out,
                                                        int capacity, MemorySegment length) {
        try {
            WCImageDecoder target = decoder(decoder);
            return WebKitNative.emitString(target == null ? null : target.getFilenameExtension(),
                    out, capacity, length);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_decoder_get_filename_extension", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }

    // ---------------------------------------------------------------------- WCImageFrame

    /* getSize() -> int[2]. Default when NULL: 0. */
    private static int imageFrameGetSize(long frame, MemorySegment out) {
        try {
            int[] size = WebKitNative.lookup(frame) instanceof WCImageFrame target
                    ? target.getSize()
                    : null;
            return WebKitNative.writeInts(out, size, 2) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("graphics.image_frame_get_size", t);
            return 0;
        }
    }
}
