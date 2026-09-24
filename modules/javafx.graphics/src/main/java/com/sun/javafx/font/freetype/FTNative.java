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

package com.sun.javafx.font.freetype;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.util.Logging;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Objects;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * The FreeType calls behind {@link FTFactory}, {@link FTFontFile} and {@link FTDisposer}, bound directly from
 * {@code libfreetype.so.6}: what the {@code Java_com_sun_javafx_font_freetype_OSFreetype_*} bodies of
 * {@code freetype.c} wrapped in JNI. The JNI library they lived in, {@code libjavafx_font_freetype.so}, needed
 * nothing but {@code libfreetype.so.6} and {@code libc.so.6}; ten of its fourteen entry points were one FreeType
 * call each, two read the glyph slot of a face, two returned build-time constants. This class keeps the method
 * names and signatures of those natives so that the callers changed nothing but the owner class.
 *
 * <h2>Binding</h2>
 * The ten functions are public FreeType API ({@code freetype.h}, {@code ftoutln.h}, {@code ftlcdfil.h}), looked
 * up by the soname the JNI library had as {@code NEEDED}. Every {@code FT_Long}, {@code FT_Pos},
 * {@code FT_Fixed} and {@code FT_F26Dot6} is a C {@code long}, 8 bytes on the LP64 Linux targets this module is
 * built for (x86_64, aarch64), so {@code face_index} and the character sizes cross as {@code JAVA_LONG}, never
 * as an {@code int} that would zero-extend -1; {@code FT_Error}, {@code FT_Int}, {@code FT_UInt},
 * {@code FT_Int32} and the enums are 4 bytes. The record layouts below are the public structs of
 * {@code freetype.h} field by field; their sizes and offsets were measured with {@code sizeof}/{@code offsetof}
 * on x86_64 against FreeType 2.14.2 ({@code FT_FaceRec} 248 bytes, {@code glyph} at 152;
 * {@code FT_GlyphSlotRec} 304 bytes, {@code metrics} at 48, {@code linearHoriAdvance} at 112, {@code advance}
 * at 128, {@code format} at 144, {@code bitmap} at 152, {@code bitmap_left} at 192, {@code outline} at 200;
 * {@code FT_Bitmap} 40 bytes; {@code FT_Outline_Funcs} 48 bytes) and follow from the LP64 alignment rules on
 * aarch64 as well. Only the fields the C read or wrote are dereferenced.
 *
 * <h2>Handles, memory, threads</h2>
 * {@code FT_Library} and {@code FT_Face} cross as {@code long}, as they did through JNI; the callers serialise
 * every use of a face under the {@code FTFontFile} monitor and release it on the "Prism Font Disposer" thread.
 * Per-call scratch (out parameters, the path bytes, {@code FT_Matrix}, {@code FT_Vector}) lives in a confined
 * arena for the duration of the call, exactly as long as the C's stack and JNI array copies lived; FreeType
 * retains none of it ({@code FT_Set_Transform} copies the matrix). The four outline callbacks are process-wide
 * upcall stubs to static methods, created once with the {@code FT_Outline_Funcs} table they fill in
 * {@link Arena#global()}, like the {@code const FT_Outline_Funcs} of the C; the per-call accumulator is a
 * {@link FTOutlineSink} reached through a thread-local slot, since FreeType calls back synchronously on the
 * calling thread while other threads decompose other faces.
 *
 * <h2>Failure to load</h2>
 * {@code libfreetype.so.6} is bound by the lazy holder {@code Lib} on the first call, which is where
 * {@code OSFreetype}'s static initializer loaded {@code libjavafx_font_freetype.so}: the first
 * {@code FT_Init_FreeType} of {@code FTFactory.getFactory()}. A library or symbol that cannot be found surfaces
 * there as {@link UnsatisfiedLinkError}, as {@code NativeLibLoader.loadLibrary} threw it; being an
 * {@code Error} it leaves class initialisation unwrapped, later uses see {@link NoClassDefFoundError}, and
 * {@code PrismFontFactory.getFontFactory} turns either into its {@code InternalError} as before. Initialising
 * this class itself runs nothing native (constants and layouts only), so it is loadable on every platform.
 * <p>
 * Line numbers into {@code freetype.c}, bare {@code :N} forms included, refer to it at commit {@code 7b43255b30}
 * ({@code git show 7b43255b30:modules/javafx.graphics/src/main/native-font/freetype.c}).
 */
final class FTNative {

    /**
     * The soname {@code libjavafx_font_freetype.so} had as {@code NEEDED}; {@code libfreetype.so} is only the
     * development package's link.
     */
    static final String LIBRARY = "libfreetype.so.6";

    /* freetype.h constants, as OSFreetype mirrored them. */
    static final int FT_FACE_FLAG_SCALABLE          = 1 <<  0;
    static final int FT_FACE_FLAG_FIXED_SIZES       = 1 <<  1;
    static final int FT_FACE_FLAG_FIXED_WIDTH       = 1 <<  2;
    static final int FT_FACE_FLAG_SFNT              = 1 <<  3;
    static final int FT_FACE_FLAG_HORIZONTAL        = 1 <<  4;
    static final int FT_FACE_FLAG_VERTICAL          = 1 <<  5;
    static final int FT_FACE_FLAG_KERNING           = 1 <<  6;
    static final int FT_FACE_FLAG_FAST_GLYPHS       = 1 <<  7;
    static final int FT_FACE_FLAG_MULTIPLE_MASTERS  = 1 <<  8;
    static final int FT_FACE_FLAG_GLYPH_NAMES       = 1 <<  9;
    static final int FT_FACE_FLAG_EXTERNAL_STREAM   = 1 << 10;
    static final int FT_FACE_FLAG_HINTER            = 1 << 11;
    static final int FT_FACE_FLAG_CID_KEYED         = 1 << 12;
    static final int FT_FACE_FLAG_TRICKY            = 1 << 13;
    static final int FT_STYLE_FLAG_ITALIC  = 1 << 0;
    static final int FT_STYLE_FLAG_BOLD    = 1 << 1;
    static final int FT_RENDER_MODE_NORMAL = 0;
    static final int FT_RENDER_MODE_LIGHT = 1;
    static final int FT_RENDER_MODE_MONO = 2;
    static final int FT_RENDER_MODE_LCD = 3;
    static final int FT_RENDER_MODE_LCD_V = 4;
    static final int FT_PIXEL_MODE_NONE = 0;
    static final int FT_PIXEL_MODE_MONO = 1;
    static final int FT_PIXEL_MODE_GRAY = 2;
    static final int FT_PIXEL_MODE_GRAY2 = 3;
    static final int FT_PIXEL_MODE_GRAY4 = 4;
    static final int FT_PIXEL_MODE_LCD = 5;
    static final int FT_PIXEL_MODE_LCD_V = 6;
    static final int FT_LOAD_DEFAULT                      = 0x0;
    static final int FT_LOAD_NO_SCALE                     = 1 << 0;
    static final int FT_LOAD_NO_HINTING                   = 1 << 1;
    static final int FT_LOAD_RENDER                       = 1 << 2;
    static final int FT_LOAD_NO_BITMAP                    = 1 << 3;
    static final int FT_LOAD_VERTICAL_LAYOUT              = 1 << 4;
    static final int FT_LOAD_FORCE_AUTOHINT               = 1 << 5;
    static final int FT_LOAD_CROP_BITMAP                  = 1 << 6;
    static final int FT_LOAD_PEDANTIC                     = 1 << 7;
    static final int FT_LOAD_IGNORE_GLOBAL_ADVANCE_WIDTH  = 1 << 9;
    static final int FT_LOAD_NO_RECURSE                   = 1 << 10;
    static final int FT_LOAD_IGNORE_TRANSFORM             = 1 << 11;
    static final int FT_LOAD_MONOCHROME                   = 1 << 12;
    static final int FT_LOAD_LINEAR_DESIGN                = 1 << 13;
    static final int FT_LOAD_NO_AUTOHINT                  = 1 << 15;
    static final int FT_LOAD_TARGET_NORMAL  = (FT_RENDER_MODE_NORMAL & 15 ) << 16;
    static final int FT_LOAD_TARGET_LIGHT   = (FT_RENDER_MODE_LIGHT  & 15 ) << 16;
    static final int FT_LOAD_TARGET_MONO    = (FT_RENDER_MODE_MONO   & 15 ) << 16;
    static final int FT_LOAD_TARGET_LCD     = (FT_RENDER_MODE_LCD    & 15 ) << 16;
    static final int FT_LOAD_TARGET_LCD_V   = (FT_RENDER_MODE_LCD_V  & 15 ) << 16;
    static final int FT_LCD_FILTER_NONE    = 0;
    static final int FT_LCD_FILTER_DEFAULT = 1;
    static final int FT_LCD_FILTER_LIGHT   = 2;
    static final int FT_LCD_FILTER_LEGACY  = 16;

    /** {@code FT_Err_Ok} and {@code FT_Err_Array_Too_Large} (fterrdef.h), the two errors the C produced itself. */
    static final int FT_ERR_OK = 0;
    static final int FT_ERR_ARRAY_TOO_LARGE = 10;

    /** {@code FT_Vector} (ftimage.h): two {@code FT_Pos}, 16 bytes. */
    static final StructLayout FT_VECTOR = MemoryLayout.structLayout(
            JAVA_LONG.withName("x"),                  // offset 0
            JAVA_LONG.withName("y"));                 // offset 8, byteSize 16

    /** {@code FT_Matrix} (fttypes.h): four {@code FT_Fixed} in the order xx, xy, yx, yy, 32 bytes. */
    static final StructLayout FT_MATRIX = MemoryLayout.structLayout(
            JAVA_LONG.withName("xx"),                 // offset 0
            JAVA_LONG.withName("xy"),                 // offset 8
            JAVA_LONG.withName("yx"),                 // offset 16
            JAVA_LONG.withName("yy"));                // offset 24, byteSize 32

    /** {@code FT_Generic} (fttypes.h): a pointer and a finalizer, 16 bytes. */
    static final StructLayout FT_GENERIC = MemoryLayout.structLayout(
            ADDRESS.withName("data"),                 // offset 0
            ADDRESS.withName("finalizer"));           // offset 8, byteSize 16

    /** {@code FT_BBox} (ftimage.h): four {@code FT_Pos}, 32 bytes. */
    static final StructLayout FT_BBOX = MemoryLayout.structLayout(
            JAVA_LONG.withName("xMin"),               // offset 0
            JAVA_LONG.withName("yMin"),               // offset 8
            JAVA_LONG.withName("xMax"),               // offset 16
            JAVA_LONG.withName("yMax"));              // offset 24, byteSize 32

    /** {@code FT_ListRec} (fttypes.h): head and tail, 16 bytes. */
    static final StructLayout FT_LIST_REC = MemoryLayout.structLayout(
            ADDRESS.withName("head"),                 // offset 0
            ADDRESS.withName("tail"));                // offset 8, byteSize 16

    /** {@code FT_Glyph_Metrics} (freetype.h): eight {@code FT_Pos}, 64 bytes; mirrored by {@link FT_Glyph_Metrics}. */
    static final StructLayout FT_GLYPH_METRICS = MemoryLayout.structLayout(
            JAVA_LONG.withName("width"),              // offset 0
            JAVA_LONG.withName("height"),             // offset 8
            JAVA_LONG.withName("horiBearingX"),       // offset 16
            JAVA_LONG.withName("horiBearingY"),       // offset 24
            JAVA_LONG.withName("horiAdvance"),        // offset 32
            JAVA_LONG.withName("vertBearingX"),       // offset 40
            JAVA_LONG.withName("vertBearingY"),       // offset 48
            JAVA_LONG.withName("vertAdvance"));       // offset 56, byteSize 64

    /**
     * {@code FT_Bitmap} (ftimage.h): 40 bytes, 8-byte aligned; mirrored by {@link FT_Bitmap}. {@code rows} and
     * {@code width} are {@code unsigned int}, {@code pitch} is {@code int} (negative for bottom-up bitmaps),
     * {@code num_grays} {@code unsigned short}, {@code pixel_mode} and {@code palette_mode} {@code unsigned char}.
     */
    static final StructLayout FT_BITMAP = MemoryLayout.structLayout(
            JAVA_INT.withName("rows"),                // offset 0
            JAVA_INT.withName("width"),               // offset 4
            JAVA_INT.withName("pitch"),               // offset 8
            MemoryLayout.paddingLayout(4),            // offset 12
            ADDRESS.withName("buffer"),               // offset 16
            JAVA_SHORT.withName("num_grays"),         // offset 24
            JAVA_BYTE.withName("pixel_mode"),         // offset 26
            JAVA_BYTE.withName("palette_mode"),       // offset 27
            MemoryLayout.paddingLayout(4),            // offset 28
            ADDRESS.withName("palette"));             // offset 32, byteSize 40

    /** {@code FT_Outline} (ftimage.h): 40 bytes; only its address is used, as {@code &slot->outline} was. */
    static final StructLayout FT_OUTLINE = MemoryLayout.structLayout(
            JAVA_SHORT.withName("n_contours"),        // offset 0
            JAVA_SHORT.withName("n_points"),          // offset 2
            MemoryLayout.paddingLayout(4),            // offset 4
            ADDRESS.withName("points"),               // offset 8
            ADDRESS.withName("tags"),                 // offset 16
            ADDRESS.withName("contours"),             // offset 24
            JAVA_INT.withName("flags"),               // offset 32
            MemoryLayout.paddingLayout(4));           // offset 36, byteSize 40

    /**
     * {@code FT_Outline_Funcs} (ftoutln.h): the four callback pointers, {@code int shift} and {@code FT_Pos delta},
     * 48 bytes. The C's table ({@code JFX_Outline_Funcs}, freetype.c:597-604) had shift 0 and delta 0.
     */
    static final StructLayout FT_OUTLINE_FUNCS = MemoryLayout.structLayout(
            ADDRESS.withName("move_to"),              // offset 0
            ADDRESS.withName("line_to"),              // offset 8
            ADDRESS.withName("conic_to"),             // offset 16
            ADDRESS.withName("cubic_to"),             // offset 24
            JAVA_INT.withName("shift"),               // offset 32
            MemoryLayout.paddingLayout(4),            // offset 36
            JAVA_LONG.withName("delta"));             // offset 40, byteSize 48

    /** {@code FT_GlyphSlotRec} (freetype.h): 304 bytes, 8-byte aligned; mirrored in part by {@link FT_GlyphSlotRec}. */
    static final StructLayout FT_GLYPH_SLOT_REC = MemoryLayout.structLayout(
            ADDRESS.withName("library"),              // offset 0
            ADDRESS.withName("face"),                 // offset 8
            ADDRESS.withName("next"),                 // offset 16
            JAVA_INT.withName("glyph_index"),         // offset 24
            MemoryLayout.paddingLayout(4),            // offset 28
            FT_GENERIC.withName("generic"),           // offset 32
            FT_GLYPH_METRICS.withName("metrics"),     // offset 48
            JAVA_LONG.withName("linearHoriAdvance"),  // offset 112
            JAVA_LONG.withName("linearVertAdvance"),  // offset 120
            FT_VECTOR.withName("advance"),            // offset 128
            JAVA_INT.withName("format"),              // offset 144
            MemoryLayout.paddingLayout(4),            // offset 148
            FT_BITMAP.withName("bitmap"),             // offset 152
            JAVA_INT.withName("bitmap_left"),         // offset 192
            JAVA_INT.withName("bitmap_top"),          // offset 196
            FT_OUTLINE.withName("outline"),           // offset 200
            JAVA_INT.withName("num_subglyphs"),       // offset 240
            MemoryLayout.paddingLayout(4),            // offset 244
            ADDRESS.withName("subglyphs"),            // offset 248
            ADDRESS.withName("control_data"),         // offset 256
            JAVA_LONG.withName("control_len"),        // offset 264
            JAVA_LONG.withName("lsb_delta"),          // offset 272
            JAVA_LONG.withName("rsb_delta"),          // offset 280
            ADDRESS.withName("other"),                // offset 288
            ADDRESS.withName("internal"));            // offset 296, byteSize 304

    /**
     * {@code FT_FaceRec} (freetype.h): 248 bytes, 8-byte aligned. Only {@code glyph} is read, as
     * {@code face->glyph} was; the fields before it are the public part of the record, declared before its
     * "private fields, internal to FreeType" comment, and fix its offset.
     */
    static final StructLayout FT_FACE_REC = MemoryLayout.structLayout(
            JAVA_LONG.withName("num_faces"),          // offset 0
            JAVA_LONG.withName("face_index"),         // offset 8
            JAVA_LONG.withName("face_flags"),         // offset 16
            JAVA_LONG.withName("style_flags"),        // offset 24
            JAVA_LONG.withName("num_glyphs"),         // offset 32
            ADDRESS.withName("family_name"),          // offset 40
            ADDRESS.withName("style_name"),           // offset 48
            JAVA_INT.withName("num_fixed_sizes"),     // offset 56
            MemoryLayout.paddingLayout(4),            // offset 60
            ADDRESS.withName("available_sizes"),      // offset 64
            JAVA_INT.withName("num_charmaps"),        // offset 72
            MemoryLayout.paddingLayout(4),            // offset 76
            ADDRESS.withName("charmaps"),             // offset 80
            FT_GENERIC.withName("generic"),           // offset 88
            FT_BBOX.withName("bbox"),                 // offset 104
            JAVA_SHORT.withName("units_per_EM"),      // offset 136
            JAVA_SHORT.withName("ascender"),          // offset 138
            JAVA_SHORT.withName("descender"),         // offset 140
            JAVA_SHORT.withName("height"),            // offset 142
            JAVA_SHORT.withName("max_advance_width"), // offset 144
            JAVA_SHORT.withName("max_advance_height"),// offset 146
            JAVA_SHORT.withName("underline_position"),// offset 148
            JAVA_SHORT.withName("underline_thickness"),// offset 150
            ADDRESS.withName("glyph"),                // offset 152
            ADDRESS.withName("size"),                 // offset 160
            ADDRESS.withName("charmap"),              // offset 168
            ADDRESS.withName("driver"),               // offset 176
            ADDRESS.withName("memory"),               // offset 184
            ADDRESS.withName("stream"),               // offset 192
            FT_LIST_REC.withName("sizes_list"),       // offset 200
            FT_GENERIC.withName("autohint"),          // offset 216
            ADDRESS.withName("extensions"),           // offset 232
            ADDRESS.withName("internal"));            // offset 240, byteSize 248

    /* The offsets the code dereferences, derived from the layouts above. */
    static final long VECTOR_X = FT_VECTOR.byteOffset(PathElement.groupElement("x"));
    static final long VECTOR_Y = FT_VECTOR.byteOffset(PathElement.groupElement("y"));
    static final long FACE_GLYPH = FT_FACE_REC.byteOffset(PathElement.groupElement("glyph"));
    static final long SLOT_METRICS_WIDTH = slotOffset("metrics", "width");
    static final long SLOT_METRICS_HEIGHT = slotOffset("metrics", "height");
    static final long SLOT_METRICS_HORI_BEARING_X = slotOffset("metrics", "horiBearingX");
    static final long SLOT_METRICS_HORI_BEARING_Y = slotOffset("metrics", "horiBearingY");
    static final long SLOT_METRICS_HORI_ADVANCE = slotOffset("metrics", "horiAdvance");
    static final long SLOT_METRICS_VERT_BEARING_X = slotOffset("metrics", "vertBearingX");
    static final long SLOT_METRICS_VERT_BEARING_Y = slotOffset("metrics", "vertBearingY");
    static final long SLOT_METRICS_VERT_ADVANCE = slotOffset("metrics", "vertAdvance");
    static final long SLOT_LINEAR_HORI_ADVANCE = slotOffset("linearHoriAdvance");
    static final long SLOT_LINEAR_VERT_ADVANCE = slotOffset("linearVertAdvance");
    static final long SLOT_ADVANCE_X = slotOffset("advance", "x");
    static final long SLOT_ADVANCE_Y = slotOffset("advance", "y");
    static final long SLOT_FORMAT = slotOffset("format");
    static final long SLOT_BITMAP_ROWS = slotOffset("bitmap", "rows");
    static final long SLOT_BITMAP_WIDTH = slotOffset("bitmap", "width");
    static final long SLOT_BITMAP_PITCH = slotOffset("bitmap", "pitch");
    static final long SLOT_BITMAP_BUFFER = slotOffset("bitmap", "buffer");
    static final long SLOT_BITMAP_NUM_GRAYS = slotOffset("bitmap", "num_grays");
    static final long SLOT_BITMAP_PIXEL_MODE = slotOffset("bitmap", "pixel_mode");
    static final long SLOT_BITMAP_PALETTE_MODE = slotOffset("bitmap", "palette_mode");
    static final long SLOT_BITMAP_PALETTE = slotOffset("bitmap", "palette");
    static final long SLOT_BITMAP_LEFT = slotOffset("bitmap_left");
    static final long SLOT_BITMAP_TOP = slotOffset("bitmap_top");
    static final long SLOT_OUTLINE = slotOffset("outline");
    static final long FUNCS_MOVE_TO = FT_OUTLINE_FUNCS.byteOffset(PathElement.groupElement("move_to"));
    static final long FUNCS_LINE_TO = FT_OUTLINE_FUNCS.byteOffset(PathElement.groupElement("line_to"));
    static final long FUNCS_CONIC_TO = FT_OUTLINE_FUNCS.byteOffset(PathElement.groupElement("conic_to"));
    static final long FUNCS_CUBIC_TO = FT_OUTLINE_FUNCS.byteOffset(PathElement.groupElement("cubic_to"));
    static final long FUNCS_SHIFT = FT_OUTLINE_FUNCS.byteOffset(PathElement.groupElement("shift"));
    static final long FUNCS_DELTA = FT_OUTLINE_FUNCS.byteOffset(PathElement.groupElement("delta"));

    private static final Linker LINKER = Linker.nativeLinker();

    /** The accumulator of the {@code FT_Outline_Decompose} call in progress on this thread, else {@code null}. */
    private static final ThreadLocal<FTOutlineSink> CURRENT_SINK = new ThreadLocal<>();

    private FTNative() {
    }

    /* ---------------------------------------------------------------------------------------------
     * Linkage
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code libfreetype.so.6} and everything bound from it, initialised on the first call into FreeType. The
     * ten downcall handles are {@code static final} so that every call site inlines its handle; the
     * {@code FT_Outline_Funcs} table lives for the process, as the C's did.
     */
    private static final class Lib {
        static final SymbolLookup FREETYPE = load(LIBRARY);

        /** {@code FT_Error FT_Init_FreeType(FT_Library*)}. */
        static final MethodHandle FT_INIT_FREETYPE = bind(FREETYPE, "FT_Init_FreeType",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /** {@code FT_Error FT_Done_FreeType(FT_Library)}. */
        static final MethodHandle FT_DONE_FREETYPE = bind(FREETYPE, "FT_Done_FreeType",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /** {@code void FT_Library_Version(FT_Library, FT_Int*, FT_Int*, FT_Int*)}. */
        static final MethodHandle FT_LIBRARY_VERSION = bind(FREETYPE, "FT_Library_Version",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        /** {@code FT_Error FT_Library_SetLcdFilter(FT_Library, FT_LcdFilter)}; the enum is 4 bytes. */
        static final MethodHandle FT_LIBRARY_SET_LCD_FILTER = bind(FREETYPE, "FT_Library_SetLcdFilter",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        /** {@code FT_Error FT_New_Face(FT_Library, const char*, FT_Long face_index, FT_Face*)}. */
        static final MethodHandle FT_NEW_FACE = bind(FREETYPE, "FT_New_Face",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS));
        /** {@code FT_Error FT_Done_Face(FT_Face)}. */
        static final MethodHandle FT_DONE_FACE = bind(FREETYPE, "FT_Done_Face",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /** {@code FT_Error FT_Set_Char_Size(FT_Face, FT_F26Dot6, FT_F26Dot6, FT_UInt, FT_UInt)}. */
        static final MethodHandle FT_SET_CHAR_SIZE = bind(FREETYPE, "FT_Set_Char_Size",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT));
        /** {@code FT_Error FT_Load_Glyph(FT_Face, FT_UInt glyph_index, FT_Int32 load_flags)}. */
        static final MethodHandle FT_LOAD_GLYPH = bind(FREETYPE, "FT_Load_Glyph",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
        /** {@code void FT_Set_Transform(FT_Face, FT_Matrix*, FT_Vector*)}. */
        static final MethodHandle FT_SET_TRANSFORM = bind(FREETYPE, "FT_Set_Transform",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
        /**
         * {@code FT_Error FT_Outline_Decompose(FT_Outline*, const FT_Outline_Funcs*, void* user)}. Never
         * {@code critical}: it calls back into Java for every segment.
         */
        static final MethodHandle FT_OUTLINE_DECOMPOSE = bind(FREETYPE, "FT_Outline_Decompose",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

        /*
         * The outline callback signatures (ftoutln.h): const FT_Vector* arguments arrive as 16-byte segments.
         * AddressLayout.withTargetLayout is a restricted method, so these live in the holder, with every other
         * restricted call of this class, and are created only on the first call into FreeType.
         */
        static final FunctionDescriptor MOVE_LINE_TO_FD = FunctionDescriptor.of(JAVA_INT,
                ADDRESS.withTargetLayout(FT_VECTOR), ADDRESS);
        static final FunctionDescriptor CONIC_TO_FD = FunctionDescriptor.of(JAVA_INT,
                ADDRESS.withTargetLayout(FT_VECTOR), ADDRESS.withTargetLayout(FT_VECTOR), ADDRESS);
        static final FunctionDescriptor CUBIC_TO_FD = FunctionDescriptor.of(JAVA_INT,
                ADDRESS.withTargetLayout(FT_VECTOR), ADDRESS.withTargetLayout(FT_VECTOR),
                ADDRESS.withTargetLayout(FT_VECTOR), ADDRESS);

        /** The {@code JFX_Outline_Funcs} table (freetype.c:597-604): four stubs, shift 0, delta 0. */
        static final MemorySegment OUTLINE_FUNCS = outlineFuncs();
    }

    /**
     * {@code dlopen} of a system library by soname. A library that cannot be loaded is an
     * {@link UnsatisfiedLinkError} naming it, the error {@code NativeLibLoader.loadLibrary} raised for
     * {@code libjavafx_font_freetype.so} - whose load failed for the same reason when {@code libfreetype.so.6},
     * its {@code NEEDED} entry, was absent. The record layouts of this class assume LP64, so a platform whose C
     * {@code long} is not 8 bytes is refused the same way.
     */
    @SuppressWarnings("restricted")
    static SymbolLookup load(String soname) {
        long longSize = LINKER.canonicalLayouts().get("long").byteSize();
        if (longSize != 8) {
            throw new UnsatisfiedLinkError("cannot bind " + soname + ": C long is " + longSize
                    + " bytes, FreeType's FT_Long, FT_Pos and FT_Fixed are bound as 8");
        }
        try {
            return SymbolLookup.libraryLookup(soname, Arena.global());
        } catch (IllegalArgumentException e) {
            UnsatisfiedLinkError error = new UnsatisfiedLinkError("cannot load " + soname + ": " + e.getMessage());
            error.initCause(e);
            throw error;
        }
    }

    /** {@code dlsym} plus linkage: a symbol the library does not export is an {@link UnsatisfiedLinkError}. */
    @SuppressWarnings("restricted")
    private static MethodHandle bind(SymbolLookup library, String name, FunctionDescriptor descriptor) {
        MemorySegment symbol = library.find(name)
                .orElseThrow(() -> new UnsatisfiedLinkError(LIBRARY + " does not export " + name));
        return LINKER.downcallHandle(symbol, descriptor);
    }

    /** One upcall stub in {@link Arena#global()}: legitimate only because the targets are static. */
    @SuppressWarnings("restricted")
    private static MemorySegment stub(MethodHandles.Lookup lookup, String name, FunctionDescriptor descriptor,
                                      int vectors) throws ReflectiveOperationException {
        Class<?>[] parameters = new Class<?>[vectors + 1];
        Arrays.fill(parameters, MemorySegment.class);
        MethodHandle target = lookup.findStatic(FTNative.class, name, MethodType.methodType(int.class, parameters));
        return LINKER.upcallStub(target, descriptor, Arena.global());
    }

    private static MemorySegment outlineFuncs() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MemorySegment funcs = Arena.global().allocate(FT_OUTLINE_FUNCS);
            funcs.set(ADDRESS, FUNCS_MOVE_TO, stub(lookup, "outlineMoveTo", Lib.MOVE_LINE_TO_FD, 1));
            funcs.set(ADDRESS, FUNCS_LINE_TO, stub(lookup, "outlineLineTo", Lib.MOVE_LINE_TO_FD, 1));
            funcs.set(ADDRESS, FUNCS_CONIC_TO, stub(lookup, "outlineConicTo", Lib.CONIC_TO_FD, 2));
            funcs.set(ADDRESS, FUNCS_CUBIC_TO, stub(lookup, "outlineCubicTo", Lib.CUBIC_TO_FD, 3));
            funcs.set(JAVA_INT, FUNCS_SHIFT, 0);
            funcs.set(JAVA_LONG, FUNCS_DELTA, 0L);
            return funcs;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("outline callback target missing", e);
        }
    }

    private static long slotOffset(String... path) {
        PathElement[] elements = new PathElement[path.length];
        for (int i = 0; i < path.length; i++) {
            elements[i] = PathElement.groupElement(path[i]);
        }
        return FT_GLYPH_SLOT_REC.byteOffset(elements);
    }

    /* ---------------------------------------------------------------------------------------------
     * Reading FreeType-owned records
     * ------------------------------------------------------------------------------------------- */

    /** {@code face->glyph}: the slot's address, 0 for a face record without one. */
    @SuppressWarnings("restricted")
    private static long glyphSlot(long face) {
        return MemorySegment.ofAddress(face).reinterpret(FT_FACE_REC.byteSize()).get(ADDRESS, FACE_GLYPH).address();
    }

    /** The 304-byte glyph slot record at {@code slot}, valid until the next load or the face's release. */
    @SuppressWarnings("restricted")
    private static MemorySegment slotRecord(long slot) {
        return MemorySegment.ofAddress(slot).reinterpret(FT_GLYPH_SLOT_REC.byteSize());
    }

    /** {@code (unsigned char*) buffer} with {@code size} readable bytes. */
    @SuppressWarnings("restricted")
    private static MemorySegment bitmapBuffer(long buffer, long size) {
        return MemorySegment.ofAddress(buffer).reinterpret(size);
    }

    /* ---------------------------------------------------------------------------------------------
     * The natives of OSFreetype, in its declaration order
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code FT_Outline_Decompose} of the outline in the face's glyph slot ({@code :606-678}): the segments
     * FreeType reports through the four callbacks, as a {@code Path2D} with winding rule 0 over arrays of
     * exactly the segment and coordinate counts. {@code null} for face 0, for a face without a glyph slot, and
     * when FreeType or a callback reports an error; a glyph slot whose outline is empty - a space, or the slot
     * FreeType cleared after a failed {@code FT_Load_Glyph} - gives a path with no segment.
     */
    static Path2D FT_Outline_Decompose(long face) {
        return decompose(face, new FTOutlineSink());
    }

    /**
     * {@link #FT_Outline_Decompose(long)} into a caller-supplied accumulator. A callback that fails answers
     * FreeType with {@code FT_Err_Array_Too_Large}, which aborts the decomposition (the C's {@code checkSize}
     * failure, {@code :506-534}), and the recorded failure decides the outcome once the downcall has returned:
     * an {@code Error} propagates, a {@code RuntimeException} is logged and gives {@code null} - what the C gave
     * for the only failure it could have, a failed {@code realloc}.
     */
    static Path2D decompose(long face, FTOutlineSink sink) {
        if (face == 0) {
            return null;
        }
        long slot = glyphSlot(face);
        if (slot == 0) {
            return null;
        }
        MethodHandle handle = Lib.FT_OUTLINE_DECOMPOSE;
        MemorySegment funcs = Lib.OUTLINE_FUNCS;
        MemorySegment outline = MemorySegment.ofAddress(slot + SLOT_OUTLINE);
        int error;
        CURRENT_SINK.set(sink);
        try {
            error = (int) handle.invokeExact(outline, funcs, MemorySegment.NULL);
        } catch (Throwable t) {
            throw unexpected(t);
        } finally {
            CURRENT_SINK.remove();
        }
        Throwable failure = sink.failure();
        if (failure instanceof Error e) {
            throw e;
        }
        if (failure != null) {
            PlatformLogger logger = Logging.getJavaFXLogger();
            if (logger.isLoggable(PlatformLogger.Level.SEVERE)) {
                logger.severe("glyph outline callback failed after " + sink.numTypes() + " segments", failure);
            }
            return null;
        }
        return error == FT_ERR_OK ? sink.toPath() : null;
    }

    /**
     * {@code FT_Init_FreeType} ({@code :451-461}): the new library handle is stored in {@code alibrary[0]}, as the
     * JNI array copy-back stored it. The C handed a null array to FreeType as a null pointer, which faults
     * inside {@code FT_Init_FreeType}; no caller passes one, and this method refuses it instead.
     */
    static int FT_Init_FreeType(long[] alibrary) {
        Objects.requireNonNull(alibrary, "alibrary");
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment out = scratch.allocateFrom(JAVA_LONG, alibrary);
            int rc = (int) Lib.FT_INIT_FREETYPE.invokeExact(out);
            MemorySegment.copy(out, JAVA_LONG, 0, alibrary, 0, alibrary.length);
            return rc;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code FT_Done_FreeType} ({@code :445-449}); releases the library's faces with it. */
    static int FT_Done_FreeType(long library) {
        try {
            return (int) Lib.FT_DONE_FREETYPE.invokeExact(MemorySegment.ofAddress(library));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code FT_Library_Version} ({@code :429-443}): each non-null array receives the value FreeType writes; a
     * null array is passed as a null pointer, which FreeType skips.
     */
    static void FT_Library_Version(long library, int[] amajor, int[] aminor, int[] apatch) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment major = amajor == null ? MemorySegment.NULL : scratch.allocateFrom(JAVA_INT, amajor);
            MemorySegment minor = aminor == null ? MemorySegment.NULL : scratch.allocateFrom(JAVA_INT, aminor);
            MemorySegment patch = apatch == null ? MemorySegment.NULL : scratch.allocateFrom(JAVA_INT, apatch);
            Lib.FT_LIBRARY_VERSION.invokeExact(MemorySegment.ofAddress(library), major, minor, patch);
            copyBack(major, amajor);
            copyBack(minor, aminor);
            copyBack(patch, apatch);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code FT_Library_SetLcdFilter} ({@code :417-421}); {@code FT_Err_Unimplemented_Feature} (7) from a FreeType
     * built without subpixel rendering, which is how {@code FTFactory.LCD_SUPPORT} is decided.
     */
    static int FT_Library_SetLcdFilter(long library, int filter) {
        try {
            return (int) Lib.FT_LIBRARY_SET_LCD_FILTER.invokeExact(MemorySegment.ofAddress(library), filter);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code FT_New_Face} ({@code :469-482}) on the path bytes exactly as the caller encoded them, terminator
     * included ({@code (file + "\0").getBytes()} in both callers, the JVM's default charset, no re-encoding);
     * the segment carries one extra zero byte so that FreeType's {@code strlen} can never leave it. The out
     * parameter is pre-seeded with {@code aface[0]} and copied back unconditionally, as the JNI copy-back did:
     * FreeType writes {@code *aface} only on success, so on failure {@code aface[0]} keeps its previous value.
     * A null array crosses as a null pointer, which FreeType answers with {@code FT_Err_Invalid_Argument}.
     */
    static int FT_New_Face(long library, byte[] filepathname, long face_index, long[] aface) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment path = MemorySegment.NULL;
            if (filepathname != null) {
                path = scratch.allocate(filepathname.length + 1L);
                MemorySegment.copy(filepathname, 0, path, JAVA_BYTE, 0, filepathname.length);
            }
            MemorySegment out = aface == null ? MemorySegment.NULL : scratch.allocateFrom(JAVA_LONG, aface);
            int rc = (int) Lib.FT_NEW_FACE.invokeExact(MemorySegment.ofAddress(library), path, face_index, out);
            if (aface != null) {
                MemorySegment.copy(out, JAVA_LONG, 0, aface, 0, aface.length);
            }
            return rc;
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code FT_Done_Face} ({@code :423-427}); {@code FT_Err_Invalid_Face_Handle} (35) for face 0. */
    static int FT_Done_Face(long face) {
        try {
            return (int) Lib.FT_DONE_FACE.invokeExact(MemorySegment.ofAddress(face));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code FT_Set_Char_Size} ({@code :484-488}); the sizes are 26.6 {@code FT_F26Dot6}, C longs. */
    static int FT_Set_Char_Size(long face, long char_width, long char_height, int horz_resolution,
                                int vert_resolution) {
        try {
            return (int) Lib.FT_SET_CHAR_SIZE.invokeExact(MemorySegment.ofAddress(face), char_width, char_height,
                                                          horz_resolution, vert_resolution);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code FT_Load_Glyph} ({@code :463-467}); the glyph index is an {@code FT_UInt}, so a negative Java value is
     * the large index the C's cast made of it.
     */
    static int FT_Load_Glyph(long face, int glyph_index, int load_flags) {
        try {
            return (int) Lib.FT_LOAD_GLYPH.invokeExact(MemorySegment.ofAddress(face), glyph_index, load_flags);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code FT_Set_Transform} ({@code :400-415}): no call at all for a null matrix (the C skipped it, so the
     * face keeps its previous transform); otherwise the four 16.16 fields in the order xx, xy, yx, yy, and a
     * delta vector only when either component is non-zero - the C passed a null delta for {@code 0, 0}, which
     * is what the only caller passes. FreeType copies both, so per-call scratch suffices, as the C's stack did.
     */
    static void FT_Set_Transform(long face, FT_Matrix matrix, long delta_x, long delta_y) {
        if (matrix == null) {
            return;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment delta = MemorySegment.NULL;
            if (delta_x != 0 || delta_y != 0) {
                delta = scratch.allocate(FT_VECTOR);
                delta.set(JAVA_LONG, VECTOR_X, delta_x);
                delta.set(JAVA_LONG, VECTOR_Y, delta_y);
            }
            MemorySegment m = scratch.allocate(FT_MATRIX);
            m.setAtIndex(JAVA_LONG, 0, matrix.xx);
            m.setAtIndex(JAVA_LONG, 1, matrix.xy);
            m.setAtIndex(JAVA_LONG, 2, matrix.yx);
            m.setAtIndex(JAVA_LONG, 3, matrix.yy);
            Lib.FT_SET_TRANSFORM.invokeExact(MemorySegment.ofAddress(face), m, delta);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code getGlyphSlot} ({@code :258-375}): a fresh mirror of {@code face->glyph} with every field the C
     * copied - the eight metrics, both linear advances, {@code advance}, {@code format}, the whole
     * {@code FT_Bitmap} (its two pointers as {@code long}, {@code palette_mode} zero-extended into the
     * {@code char} the mirror declares) and the bitmap origin - or {@code null} for face 0 or a face without a
     * glyph slot. The values are whatever the slot holds, also after a failed load, as they were.
     */
    static FT_GlyphSlotRec getGlyphSlot(long face) {
        if (face == 0) {
            return null;
        }
        long slotAddress = glyphSlot(face);
        if (slotAddress == 0) {
            return null;
        }
        MemorySegment slot = slotRecord(slotAddress);
        FT_GlyphSlotRec rec = new FT_GlyphSlotRec();
        FT_Glyph_Metrics metrics = rec.metrics;
        metrics.width = slot.get(JAVA_LONG, SLOT_METRICS_WIDTH);
        metrics.height = slot.get(JAVA_LONG, SLOT_METRICS_HEIGHT);
        metrics.horiBearingX = slot.get(JAVA_LONG, SLOT_METRICS_HORI_BEARING_X);
        metrics.horiBearingY = slot.get(JAVA_LONG, SLOT_METRICS_HORI_BEARING_Y);
        metrics.horiAdvance = slot.get(JAVA_LONG, SLOT_METRICS_HORI_ADVANCE);
        metrics.vertBearingX = slot.get(JAVA_LONG, SLOT_METRICS_VERT_BEARING_X);
        metrics.vertBearingY = slot.get(JAVA_LONG, SLOT_METRICS_VERT_BEARING_Y);
        metrics.vertAdvance = slot.get(JAVA_LONG, SLOT_METRICS_VERT_ADVANCE);
        rec.linearHoriAdvance = slot.get(JAVA_LONG, SLOT_LINEAR_HORI_ADVANCE);
        rec.linearVertAdvance = slot.get(JAVA_LONG, SLOT_LINEAR_VERT_ADVANCE);
        rec.advance_x = slot.get(JAVA_LONG, SLOT_ADVANCE_X);
        rec.advance_y = slot.get(JAVA_LONG, SLOT_ADVANCE_Y);
        rec.format = slot.get(JAVA_INT, SLOT_FORMAT);
        FT_Bitmap bitmap = rec.bitmap;
        bitmap.rows = slot.get(JAVA_INT, SLOT_BITMAP_ROWS);
        bitmap.width = slot.get(JAVA_INT, SLOT_BITMAP_WIDTH);
        bitmap.pitch = slot.get(JAVA_INT, SLOT_BITMAP_PITCH);
        bitmap.buffer = slot.get(ADDRESS, SLOT_BITMAP_BUFFER).address();
        bitmap.num_grays = slot.get(JAVA_SHORT, SLOT_BITMAP_NUM_GRAYS);
        bitmap.pixel_mode = slot.get(JAVA_BYTE, SLOT_BITMAP_PIXEL_MODE);
        bitmap.palette_mode = (char) (slot.get(JAVA_BYTE, SLOT_BITMAP_PALETTE_MODE) & 0xFF);
        bitmap.palette = slot.get(ADDRESS, SLOT_BITMAP_PALETTE).address();
        rec.bitmap_left = slot.get(JAVA_INT, SLOT_BITMAP_LEFT);
        rec.bitmap_top = slot.get(JAVA_INT, SLOT_BITMAP_TOP);
        return rec;
    }

    /**
     * {@code getBitmapData} ({@code :377-398}): a copy of the slot bitmap's {@code pitch * rows} bytes - the
     * padded rows FreeType rendered, which {@code FTFontFile.initGlyph} strips to {@code width} when the two
     * differ. {@code null} for face 0, a face without a glyph slot, a null buffer (an unrendered load), a pitch
     * that is not positive (bottom-up bitmaps), or {@code rows > INT_MAX / pitch}, {@code rows} being unsigned.
     */
    static byte[] getBitmapData(long face) {
        if (face == 0) {
            return null;
        }
        long slotAddress = glyphSlot(face);
        if (slotAddress == 0) {
            return null;
        }
        MemorySegment slot = slotRecord(slotAddress);
        long buffer = slot.get(ADDRESS, SLOT_BITMAP_BUFFER).address();
        if (buffer == 0) {
            return null;
        }
        int pitch = slot.get(JAVA_INT, SLOT_BITMAP_PITCH);
        if (pitch <= 0) {
            return null;
        }
        int rows = slot.get(JAVA_INT, SLOT_BITMAP_ROWS);
        if (Integer.compareUnsigned(rows, Integer.MAX_VALUE / pitch) > 0) {
            return null;
        }
        int size = pitch * rows;
        byte[] result = new byte[size];
        MemorySegment.copy(bitmapBuffer(buffer, size), JAVA_BYTE, 0, result, 0, size);
        return result;
    }

    /**
     * {@code isPangoEnabled} ({@code :680-687}): {@code _ENABLE_PANGO} was defined for every Linux build of the
     * font libraries (the {@code fontFreetype} and {@code fontPango} targets of {@code native/linux.cmake} at
     * commit {@code 7b43255b30}, removed with the C), so the answer is {@code true}. The Android arm of
     * {@code freetype.c} was not built by this project.
     */
    static boolean isPangoEnabled() {
        return true;
    }

    /** {@code isHarfbuzzEnabled} ({@code :689-696}): {@code _ENABLE_HARFBUZZ} was defined by no build file. */
    static boolean isHarfbuzzEnabled() {
        return false;
    }

    /* ---------------------------------------------------------------------------------------------
     * The FT_Outline_Funcs targets (freetype.c:536-595), one upcall per segment on the calling thread
     * ------------------------------------------------------------------------------------------- */

    private static int outlineMoveTo(MemorySegment to, MemorySegment user) {
        FTOutlineSink sink = CURRENT_SINK.get();
        if (sink == null) {
            return FT_ERR_ARRAY_TOO_LARGE;
        }
        try {
            sink.moveTo(to.get(JAVA_LONG, VECTOR_X), to.get(JAVA_LONG, VECTOR_Y));
            return FT_ERR_OK;
        } catch (Throwable t) {
            sink.failed(t);
            return FT_ERR_ARRAY_TOO_LARGE;
        }
    }

    private static int outlineLineTo(MemorySegment to, MemorySegment user) {
        FTOutlineSink sink = CURRENT_SINK.get();
        if (sink == null) {
            return FT_ERR_ARRAY_TOO_LARGE;
        }
        try {
            sink.lineTo(to.get(JAVA_LONG, VECTOR_X), to.get(JAVA_LONG, VECTOR_Y));
            return FT_ERR_OK;
        } catch (Throwable t) {
            sink.failed(t);
            return FT_ERR_ARRAY_TOO_LARGE;
        }
    }

    private static int outlineConicTo(MemorySegment control, MemorySegment to, MemorySegment user) {
        FTOutlineSink sink = CURRENT_SINK.get();
        if (sink == null) {
            return FT_ERR_ARRAY_TOO_LARGE;
        }
        try {
            sink.conicTo(control.get(JAVA_LONG, VECTOR_X), control.get(JAVA_LONG, VECTOR_Y),
                         to.get(JAVA_LONG, VECTOR_X), to.get(JAVA_LONG, VECTOR_Y));
            return FT_ERR_OK;
        } catch (Throwable t) {
            sink.failed(t);
            return FT_ERR_ARRAY_TOO_LARGE;
        }
    }

    private static int outlineCubicTo(MemorySegment control1, MemorySegment control2, MemorySegment to,
                                      MemorySegment user) {
        FTOutlineSink sink = CURRENT_SINK.get();
        if (sink == null) {
            return FT_ERR_ARRAY_TOO_LARGE;
        }
        try {
            sink.cubicTo(control1.get(JAVA_LONG, VECTOR_X), control1.get(JAVA_LONG, VECTOR_Y),
                         control2.get(JAVA_LONG, VECTOR_X), control2.get(JAVA_LONG, VECTOR_Y),
                         to.get(JAVA_LONG, VECTOR_X), to.get(JAVA_LONG, VECTOR_Y));
            return FT_ERR_OK;
        } catch (Throwable t) {
            sink.failed(t);
            return FT_ERR_ARRAY_TOO_LARGE;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Helpers
     * ------------------------------------------------------------------------------------------- */

    /** The JNI release-mode-0 copy-back of an {@code int[]} that crossed as a pointer. */
    private static void copyBack(MemorySegment segment, int[] array) {
        if (array != null) {
            MemorySegment.copy(segment, JAVA_INT, 0, array, 0, array.length);
        }
    }

    private static Error unexpected(Throwable t) {
        if (t instanceof Error e) {
            return e;
        }
        if (t instanceof RuntimeException e) {
            throw e;
        }
        // Only linkage failures can land here: FreeType reports failure in its FT_Error, it cannot throw.
        return new AssertionError(t);
    }
}
