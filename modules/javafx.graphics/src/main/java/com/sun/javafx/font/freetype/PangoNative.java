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

import com.sun.javafx.font.JniStringCodec;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.List;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The Pango, GLib and fontconfig calls behind {@link PangoGlyphLayout} and {@code FTFactory.registerEmbeddedFont},
 * bound from Java to the system libraries: what the {@code Java_com_sun_javafx_font_freetype_OSPango_*} bodies of
 * {@code pango.c} (commit {@code 7b43255b30}, built as {@code libjavafx_font_pango.so}) wrapped in JNI. Every
 * method here has the name, parameter list and result of the {@code OSPango} native it replaces, calls the same
 * system symbol with the same argument values, and keeps the C's guards, its order of calls and its ownership
 * rules; handles stay raw addresses in {@code long}s at the {@code PangoGlyphLayout} boundary.
 *
 * <h2>Libraries</h2>
 * {@code libjavafx_font_pango.so} needed {@code libpangoft2-1.0.so.0}, {@code libpango-1.0.so.0},
 * {@code libgobject-2.0.so.0} and {@code libglib-2.0.so.0}, and {@code dlopen}ed {@code libfontconfig.so.1}
 * (pango.c:253-261). This class binds the same sonames (never the unversioned development links) through
 * {@link SymbolLookup#libraryLookup} in {@link Arena#global()}: they stay mapped for the life of the process, as
 * the JNI library did. All Pango, GLib and GObject symbols are resolved together, on the first call of any method
 * ({@code Lib}), so that a missing library surfaces as an {@link UnsatisfiedLinkError} at the same point as the
 * failed load of {@code libjavafx_font_pango.so} by {@code OSPango}'s static initializer at that commit: from
 * {@code PangoGlyphLayout}'s static initializer or from {@code registerEmbeddedFont}, unwrapped (an {@code Error}
 * is not turned into {@code ExceptionInInitializerError}), with later calls failing in {@code NoClassDefFoundError}.
 * fontconfig is opened separately and lazily by {@link #FcConfigAppFontAddFile}, exactly as pango.c:255-261 did
 * with {@code dlopen}/{@code dlsym}: a library or symbol that cannot be found makes the call return {@code false}
 * and is looked for again on the next call; a handle once obtained is never released. That handle is this
 * class's own and is not shared with the fontconfig binding of {@code FontConfigManager}, which opens and closes
 * the library per call.
 *
 * <h2>Strings</h2>
 * Three conversions cross here, each reproduced as HotSpot performed it for the JNI code:
 * <ul>
 * <li>{@link #g_utf16_to_utf8}: the raw UTF-16 code units of the {@code char[]} are copied into a native buffer
 * and handed to GLib with their count, as {@code GetPrimitiveArrayCritical} exposed them (pango.c:430-443). No
 * charset is involved: an unpaired surrogate anywhere makes GLib return {@code NULL} and an embedded {@code U+0000}
 * ends the text, both of which {@code PangoGlyphLayout} depends on.</li>
 * <li>{@link #pango_font_description_set_family} and {@link #FcConfigAppFontAddFile}: the Java string becomes the
 * modified UTF-8 of {@code GetStringUTFChars} ({@link JniStringCodec#allocateModifiedUtf8}), for the duration of
 * the call, as at pango.c:245-248 and :264-270.</li>
 * <li>{@link #pango_font_description_get_family}: the C string becomes the Java string {@code NewStringUTF} made
 * of it ({@link JniStringCodec#fromNewStringUtf}), {@code null} for a {@code NULL} pointer (pango.c:237-238).</li>
 * </ul>
 *
 * <h2>Structs</h2>
 * {@link #pango_shape} reads the public prefix of {@code PangoItem} and {@code PangoGlyphString} and the
 * {@code PangoGlyphInfo} array, with the layouts below (pango-item.h, pango-glyph.h; sizes and offsets measured
 * with {@code offsetof} against Pango 1.57.0 and GLib 2.88.0 on x86_64, and identical under the LP64 rules of
 * AArch64: every field is an {@code int}, a byte or a pointer). Nothing else here reads native memory.
 *
 * <h2>Not carried over</h2>
 * {@code OSPango.pango_font_description_get_stretch} and {@code OSPango.g_utf8_pointer_to_offset} had no Java
 * caller; they have no counterpart in this class. {@code g_utf8_pointer_to_offset} is still bound, privately, for
 * the cluster conversion inside {@link #pango_shape}, where pango.c:195 called it.
 * <p>
 * Line numbers {@code pango.c:N} refer to {@code modules/javafx.graphics/src/main/native-font/pango.c} at commit
 * {@code 7b43255b30}.
 */
final class PangoNative {

    static final String LIB_PANGO = "libpango-1.0.so.0";
    static final String LIB_PANGOFT2 = "libpangoft2-1.0.so.0";
    static final String LIB_GOBJECT = "libgobject-2.0.so.0";
    static final String LIB_GLIB = "libglib-2.0.so.0";

    /** {@code LIB_FONTCONFIG} of pango.c:253. */
    static final String LIB_FONTCONFIG = "libfontconfig.so.1";

    /** The libraries {@code Lib} binds, Pango first so that a missing Pango is the one named. */
    static final List<String> LIBRARIES = List.of(LIB_PANGO, LIB_PANGOFT2, LIB_GOBJECT, LIB_GLIB);

    /**
     * {@code PangoAnalysis} (pango-item.h): 48 bytes, 8-byte aligned. Two dead engine pointers, the font, four
     * bytes ({@code level}, {@code gravity}, {@code flags}, {@code script}), padding, the language and the extra
     * attributes. Copied by value at pango.c:159 and passed by pointer to {@code pango_shape}; {@code font} at 16
     * is what pango.c:222 stored in {@code PangoGlyphString.font}.
     */
    static final StructLayout PANGO_ANALYSIS_LAYOUT = MemoryLayout.structLayout(
            ADDRESS.withName("shape_engine"),        // offset 0
            ADDRESS.withName("lang_engine"),         // offset 8
            ADDRESS.withName("font"),                // offset 16  PangoFont*
            JAVA_BYTE.withName("level"),             // offset 24  guint8
            JAVA_BYTE.withName("gravity"),           // offset 25  guint8
            JAVA_BYTE.withName("flags"),             // offset 26  guint8
            JAVA_BYTE.withName("script"),            // offset 27  guint8
            MemoryLayout.paddingLayout(4),           // offset 28
            ADDRESS.withName("language"),            // offset 32  PangoLanguage*
            ADDRESS.withName("extra_attrs"));        // offset 40  GSList*, byteSize 48

    /**
     * {@code PangoItem} (pango-item.h): 64 bytes. The three counts pango.c:161, :166 and :219-221 read (byte
     * offset and byte length into the UTF-8 text, and the number of characters), padding, then the analysis at 16
     * (so the font is at 32).
     */
    static final StructLayout PANGO_ITEM_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("offset"),             // offset 0   gint
            JAVA_INT.withName("length"),             // offset 4   gint
            JAVA_INT.withName("num_chars"),          // offset 8   gint
            MemoryLayout.paddingLayout(4),           // offset 12
            PANGO_ANALYSIS_LAYOUT.withName("analysis")); // offset 16, byteSize 64

    /** {@code PangoGlyphGeometry} (pango-glyph.h): three {@code PangoGlyphUnit} ({@code gint32}), 12 bytes. */
    static final StructLayout PANGO_GLYPH_GEOMETRY_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("width"),              // offset 0
            JAVA_INT.withName("x_offset"),           // offset 4
            JAVA_INT.withName("y_offset"));          // offset 8, byteSize 12

    /**
     * {@code PangoGlyphInfo} (pango-glyph.h): 20 bytes, 4-byte aligned, the stride of the {@code glyphs} array.
     * pango.c:192-193 copied {@code glyph} and {@code geometry.width}; the offsets and {@code attr} (a bit field:
     * {@code is_cluster_start} bit 0, {@code is_color} bit 1) were never read.
     */
    static final StructLayout PANGO_GLYPH_INFO_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("glyph"),              // offset 0   PangoGlyph (guint32)
            PANGO_GLYPH_GEOMETRY_LAYOUT.withName("geometry"), // offset 4
            JAVA_INT.withName("attr"));              // offset 16  PangoGlyphVisAttr, byteSize 20

    /**
     * {@code PangoGlyphString} (pango-glyph.h): 32 bytes. {@code num_glyphs}, padding, the {@code glyphs} array
     * and the {@code log_clusters} array (one {@code gint} per glyph, a byte offset into the item's text), then
     * the private {@code space}. Allocated and released only through {@code pango_glyph_string_new} and
     * {@code pango_glyph_string_free}.
     */
    static final StructLayout PANGO_GLYPH_STRING_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("num_glyphs"),         // offset 0   gint
            MemoryLayout.paddingLayout(4),           // offset 4
            ADDRESS.withName("glyphs"),              // offset 8   PangoGlyphInfo*
            ADDRESS.withName("log_clusters"),        // offset 16  gint*
            JAVA_INT.withName("space"),              // offset 24  gint, private
            MemoryLayout.paddingLayout(4));          // offset 28, byteSize 32

    private static final long ITEM_OFFSET = offsetOf(PANGO_ITEM_LAYOUT, "offset");
    private static final long ITEM_LENGTH = offsetOf(PANGO_ITEM_LAYOUT, "length");
    private static final long ITEM_NUM_CHARS = offsetOf(PANGO_ITEM_LAYOUT, "num_chars");
    private static final long ITEM_ANALYSIS = offsetOf(PANGO_ITEM_LAYOUT, "analysis");
    private static final long ANALYSIS_FONT = offsetOf(PANGO_ANALYSIS_LAYOUT, "font");
    private static final long GLYPH_STRING_NUM_GLYPHS = offsetOf(PANGO_GLYPH_STRING_LAYOUT, "num_glyphs");
    private static final long GLYPH_STRING_GLYPHS = offsetOf(PANGO_GLYPH_STRING_LAYOUT, "glyphs");
    private static final long GLYPH_STRING_LOG_CLUSTERS = offsetOf(PANGO_GLYPH_STRING_LAYOUT, "log_clusters");
    private static final long GLYPH_INFO_SIZE = PANGO_GLYPH_INFO_LAYOUT.byteSize();
    private static final long GLYPH_INFO_GLYPH = offsetOf(PANGO_GLYPH_INFO_LAYOUT, "glyph");
    private static final long GLYPH_INFO_WIDTH = offsetOf(PANGO_GLYPH_INFO_LAYOUT, "geometry")
            + offsetOf(PANGO_GLYPH_GEOMETRY_LAYOUT, "width");

    /**
     * {@code INT_MAX / sizeof(jint)} (pango.c:172): a glyph count at or above it made the C print a diagnostic and
     * return {@code NULL} rather than size its scratch arrays.
     */
    static final int MAX_GLYPHS = Integer.MAX_VALUE / Integer.BYTES;

    /**
     * The libraries and downcall handles, bound when first used - by the first call of any method of the outer
     * class, never merely by loading it. Its initializer is the one place that can throw
     * {@link UnsatisfiedLinkError}; every field is bound with the descriptor of the C prototype, {@code gboolean},
     * {@code guint} and the enums as {@code JAVA_INT}, {@code glong} and {@code gssize} as {@code JAVA_LONG}. None
     * of the calls is {@link Linker.Option#critical critical}: {@code pango_itemize} and {@code pango_shape} reach
     * fontconfig, HarfBuzz and FreeType (locks and file I/O), and the JNI versions were ordinary natives.
     */
    private static final class Lib {
        static final Linker LINKER = Linker.nativeLinker();
        static final SymbolLookup PANGO = library(LIB_PANGO);
        static final SymbolLookup PANGOFT2 = library(LIB_PANGOFT2);
        static final SymbolLookup GOBJECT = library(LIB_GOBJECT);
        static final SymbolLookup GLIB = library(LIB_GLIB);

        /* pango.c:283-287 */
        static final MethodHandle PANGO_CONTEXT_SET_BASE_DIR = bind(PANGO, LIB_PANGO, "pango_context_set_base_dir",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* pango.c:319-323 */
        static final MethodHandle PANGO_FT2_FONT_MAP_NEW = bind(PANGOFT2, LIB_PANGOFT2, "pango_ft2_font_map_new",
                FunctionDescriptor.of(ADDRESS));
        /* pango.c:325-329 */
        static final MethodHandle PANGO_FONT_MAP_CREATE_CONTEXT = bind(PANGO, LIB_PANGO,
                "pango_font_map_create_context", FunctionDescriptor.of(ADDRESS, ADDRESS));
        /* pango.c:289-293 */
        static final MethodHandle PANGO_FONT_DESCRIBE = bind(PANGO, LIB_PANGO, "pango_font_describe",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        /* pango.c:337-341 */
        static final MethodHandle PANGO_FONT_DESCRIPTION_NEW = bind(PANGO, LIB_PANGO, "pango_font_description_new",
                FunctionDescriptor.of(ADDRESS));
        /* pango.c:457-461 */
        static final MethodHandle PANGO_FONT_DESCRIPTION_FREE = bind(PANGO, LIB_PANGO,
                "pango_font_description_free", FunctionDescriptor.ofVoid(ADDRESS));
        /* pango.c:234-239 */
        static final MethodHandle PANGO_FONT_DESCRIPTION_GET_FAMILY = bind(PANGO, LIB_PANGO,
                "pango_font_description_get_family", FunctionDescriptor.of(ADDRESS, ADDRESS));
        /* pango.c:307-311 */
        static final MethodHandle PANGO_FONT_DESCRIPTION_GET_STYLE = bind(PANGO, LIB_PANGO,
                "pango_font_description_get_style", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /* pango.c:313-317 */
        static final MethodHandle PANGO_FONT_DESCRIPTION_GET_WEIGHT = bind(PANGO, LIB_PANGO,
                "pango_font_description_get_weight", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /* pango.c:241-251 */
        static final MethodHandle PANGO_FONT_DESCRIPTION_SET_FAMILY = bind(PANGO, LIB_PANGO,
                "pango_font_description_set_family", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
        /* pango.c:343-347 */
        static final MethodHandle PANGO_FONT_DESCRIPTION_SET_ABSOLUTE_SIZE = bind(PANGO, LIB_PANGO,
                "pango_font_description_set_absolute_size", FunctionDescriptor.ofVoid(ADDRESS, JAVA_DOUBLE));
        /* pango.c:349-353 */
        static final MethodHandle PANGO_FONT_DESCRIPTION_SET_STRETCH = bind(PANGO, LIB_PANGO,
                "pango_font_description_set_stretch", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* pango.c:355-359 */
        static final MethodHandle PANGO_FONT_DESCRIPTION_SET_STYLE = bind(PANGO, LIB_PANGO,
                "pango_font_description_set_style", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* pango.c:361-365 */
        static final MethodHandle PANGO_FONT_DESCRIPTION_SET_WEIGHT = bind(PANGO, LIB_PANGO,
                "pango_font_description_set_weight", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        /* pango.c:367-371 */
        static final MethodHandle PANGO_ATTR_LIST_NEW = bind(PANGO, LIB_PANGO, "pango_attr_list_new",
                FunctionDescriptor.of(ADDRESS));
        /* pango.c:373-377 */
        static final MethodHandle PANGO_ATTR_FONT_DESC_NEW = bind(PANGO, LIB_PANGO, "pango_attr_font_desc_new",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        /* pango.c:295-299 */
        static final MethodHandle PANGO_ATTR_FALLBACK_NEW = bind(PANGO, LIB_PANGO, "pango_attr_fallback_new",
                FunctionDescriptor.of(ADDRESS, JAVA_INT));
        /* pango.c:451-455 */
        static final MethodHandle PANGO_ATTR_LIST_UNREF = bind(PANGO, LIB_PANGO, "pango_attr_list_unref",
                FunctionDescriptor.ofVoid(ADDRESS));
        /* pango.c:379-383 */
        static final MethodHandle PANGO_ATTR_LIST_INSERT = bind(PANGO, LIB_PANGO, "pango_attr_list_insert",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
        /* pango.c:277-281 */
        static final MethodHandle PANGO_ITEMIZE = bind(PANGO, LIB_PANGO, "pango_itemize",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS));
        /* pango.c:162 */
        static final MethodHandle PANGO_GLYPH_STRING_NEW = bind(PANGO, LIB_PANGO, "pango_glyph_string_new",
                FunctionDescriptor.of(ADDRESS));
        /* pango.c:166 */
        static final MethodHandle PANGO_SHAPE = bind(PANGO, LIB_PANGO, "pango_shape",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, ADDRESS));
        /* pango.c:227 */
        static final MethodHandle PANGO_GLYPH_STRING_FREE = bind(PANGO, LIB_PANGO, "pango_glyph_string_free",
                FunctionDescriptor.ofVoid(ADDRESS));
        /* pango.c:397-401 */
        static final MethodHandle PANGO_ITEM_FREE = bind(PANGO, LIB_PANGO, "pango_item_free",
                FunctionDescriptor.ofVoid(ADDRESS));
        /* pango.c:409-414 */
        static final MethodHandle G_UTF8_OFFSET_TO_POINTER = bind(GLIB, LIB_GLIB, "g_utf8_offset_to_pointer",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));
        /* pango.c:195 */
        static final MethodHandle G_UTF8_POINTER_TO_OFFSET = bind(GLIB, LIB_GLIB, "g_utf8_pointer_to_offset",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS));
        /* pango.c:423-428 */
        static final MethodHandle G_UTF8_STRLEN = bind(GLIB, LIB_GLIB, "g_utf8_strlen",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_LONG));
        /* pango.c:430-443 */
        static final MethodHandle G_UTF16_TO_UTF8 = bind(GLIB, LIB_GLIB, "g_utf16_to_utf8",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS, ADDRESS));
        /* pango.c:445-449 */
        static final MethodHandle G_FREE = bind(GLIB, LIB_GLIB, "g_free", FunctionDescriptor.ofVoid(ADDRESS));
        /* pango.c:385-389 */
        static final MethodHandle G_LIST_LENGTH = bind(GLIB, LIB_GLIB, "g_list_length",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        /* pango.c:391-395 */
        static final MethodHandle G_LIST_NTH_DATA = bind(GLIB, LIB_GLIB, "g_list_nth_data",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
        /* pango.c:403-407 */
        static final MethodHandle G_LIST_FREE = bind(GLIB, LIB_GLIB, "g_list_free",
                FunctionDescriptor.ofVoid(ADDRESS));
        /* pango.c:331-335 */
        static final MethodHandle G_OBJECT_UNREF = bind(GOBJECT, LIB_GOBJECT, "g_object_unref",
                FunctionDescriptor.ofVoid(ADDRESS));

        /**
         * {@code strlen} of libc: how {@code NewStringUTF} measures the C string it is given
         * ({@code java_lang_String::create_from_str}), so the decoder gets exactly the bytes up to the terminator.
         */
        static final MethodHandle STRLEN = bind(LINKER.defaultLookup(), "libc", "strlen",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS));

        private Lib() {
        }
    }

    /**
     * {@code FcConfigAppFontAddFile} of {@code libfontconfig.so.1}, once found: the {@code static void *fp} of
     * pango.c:255. Guarded by the class monitor where the C had a benign race between threads.
     */
    private static MethodHandle fcConfigAppFontAddFile;

    private PangoNative() {
    }

    /* ---------------------------------------------------------------------------------------------
     * Linkage
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code dlopen(soname)} for a library the JNI build could not do without: an {@link UnsatisfiedLinkError}
     * when it cannot be loaded, as {@code NativeLibLoader.loadLibrary} raised when the library that needed it
     * could not be.
     */
    @SuppressWarnings("restricted")
    static SymbolLookup library(String soname) {
        try {
            return SymbolLookup.libraryLookup(soname, Arena.global());
        } catch (IllegalArgumentException e) {
            UnsatisfiedLinkError error = new UnsatisfiedLinkError(
                    "cannot load " + soname + " (Linux font support needs " + LIBRARIES + "): " + e.getMessage());
            error.initCause(e);
            throw error;
        }
    }

    /**
     * Resolves {@code name} in {@code library} and links it with {@code descriptor}.
     *
     * @throws UnsatisfiedLinkError if the library does not export {@code name}
     */
    @SuppressWarnings("restricted")
    private static MethodHandle bind(SymbolLookup library, String libraryName, String name,
                                     FunctionDescriptor descriptor) {
        MemorySegment symbol = library.find(name).orElseThrow(
                () -> new UnsatisfiedLinkError("missing symbol " + name + " in " + libraryName));
        return Lib.LINKER.downcallHandle(symbol, descriptor);
    }

    /**
     * pango.c:255-261: {@code dlopen(LIB_FONTCONFIG, RTLD_LAZY)} then {@code dlsym(handle,
     * "FcConfigAppFontAddFile")}, attempted on every call until it has succeeded, the handle never closed. Takes
     * the linker of {@code Lib} so that the Pango libraries are bound first: in the JNI build this function could
     * not be reached without {@code libjavafx_font_pango.so} and everything it needed having loaded.
     *
     * @return the handle, or {@code null} when the library or the symbol is absent
     */
    @SuppressWarnings("restricted")
    private static synchronized MethodHandle fcConfigAppFontAddFile(Linker linker) {
        if (fcConfigAppFontAddFile == null) {
            SymbolLookup fontconfig;
            try {
                fontconfig = SymbolLookup.libraryLookup(LIB_FONTCONFIG, Arena.global());
            } catch (IllegalArgumentException e) {
                return null;
            }
            MemorySegment symbol = fontconfig.find("FcConfigAppFontAddFile").orElse(null);
            if (symbol == null) {
                return null;
            }
            fcConfigAppFontAddFile = linker.downcallHandle(symbol, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        }
        return fcConfigAppFontAddFile;
    }

    private static long offsetOf(StructLayout layout, String field) {
        return layout.byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    /** The zero-length segment of a raw address, for passing it on; never read through. */
    private static MemorySegment ptr(long address) {
        return MemorySegment.ofAddress(address);
    }

    /**
     * A pointer that arrived from native code, given the size it is documented to have. Segments from outside
     * have length zero, and this is the only place in the class that widens one.
     */
    @SuppressWarnings("restricted")
    private static MemorySegment bounded(MemorySegment segment, long byteSize) {
        return segment.reinterpret(byteSize);
    }

    private static Error unexpected(Throwable t) {
        if (t instanceof Error e) {
            return e;
        }
        if (t instanceof RuntimeException e) {
            throw e;
        }
        // Only linkage failures can land here: neither Pango nor GLib can throw.
        return new AssertionError(t);
    }

    /* ---------------------------------------------------------------------------------------------
     * Custom - pango.c:153-274
     * ------------------------------------------------------------------------------------------- */

    /**
     * pango.c:153-232. Shapes the text of {@code pangoItem} (the bytes of {@code text} from the item's
     * {@code offset}, for its {@code length}) with a copy of the item's analysis into a new
     * {@code PangoGlyphString}, and returns the glyph ids, the advance widths and the cluster of every glyph as
     * the number of characters from the item start ({@code g_utf8_pointer_to_offset} of the byte offset Pango
     * reports), with the item's counts and its font pointer - a borrowed pointer, no reference taken. The Pango
     * glyph string is always freed before returning. {@code null} for a null text or item, when
     * {@code pango_glyph_string_new} returns {@code NULL}, for no glyphs, and for a count at or above
     * {@link #MAX_GLYPHS}; the arrays are sized before anything is read from the glyph string, so an
     * {@link OutOfMemoryError} escapes, after the free, with nothing returned - as the pending error of a failed
     * {@code NewIntArray} did.
     */
    static PangoGlyphString pango_shape(long text, long pangoItem) {
        if (text == 0) {
            return null;
        }
        if (pangoItem == 0) {
            return null;
        }
        MemorySegment item = bounded(ptr(pangoItem), PANGO_ITEM_LAYOUT.byteSize());
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment analysis = scratch.allocate(PANGO_ANALYSIS_LAYOUT);
            MemorySegment.copy(item, ITEM_ANALYSIS, analysis, 0, PANGO_ANALYSIS_LAYOUT.byteSize());
            MemorySegment itemText = ptr(text + item.get(JAVA_INT, ITEM_OFFSET));
            MemorySegment glyphString = (MemorySegment) Lib.PANGO_GLYPH_STRING_NEW.invokeExact();
            if (glyphString.address() == 0) {
                return null;
            }
            try {
                Lib.PANGO_SHAPE.invokeExact(itemText, item.get(JAVA_INT, ITEM_LENGTH), analysis, glyphString);
                return glyphs(bounded(glyphString, PANGO_GLYPH_STRING_LAYOUT.byteSize()), itemText, item, analysis);
            } finally {
                Lib.PANGO_GLYPH_STRING_FREE.invokeExact(glyphString);
            }
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:167-224: the arrays and the object, or {@code null} for no glyphs or a count the C refused. */
    private static PangoGlyphString glyphs(MemorySegment glyphString, MemorySegment text, MemorySegment item,
                                           MemorySegment analysis) throws Throwable {
        int count = glyphString.get(JAVA_INT, GLYPH_STRING_NUM_GLYPHS);
        if (count <= 0) {
            return null;
        }
        if (count >= MAX_GLYPHS) {
            System.err.println("OS_NATIVE error: large glyph count value in pango_1shape");
            return null;
        }
        int[] glyphs = new int[count];
        int[] widths = new int[count];
        int[] clusters = new int[count];
        MemorySegment infos = bounded(glyphString.get(ADDRESS, GLYPH_STRING_GLYPHS), count * GLYPH_INFO_SIZE);
        MemorySegment logClusters = bounded(glyphString.get(ADDRESS, GLYPH_STRING_LOG_CLUSTERS),
                                            count * JAVA_INT.byteSize());
        for (int i = 0; i < count; i++) {
            long info = i * GLYPH_INFO_SIZE;
            glyphs[i] = infos.get(JAVA_INT, info + GLYPH_INFO_GLYPH);
            widths[i] = infos.get(JAVA_INT, info + GLYPH_INFO_WIDTH);
            /* translate byte index to char index */
            MemorySegment cluster = ptr(text.address() + logClusters.getAtIndex(JAVA_INT, i));
            clusters[i] = (int) (long) Lib.G_UTF8_POINTER_TO_OFFSET.invokeExact(text, cluster);
        }
        PangoGlyphString result = new PangoGlyphString();
        result.num_glyphs = count;
        result.glyphs = glyphs;
        result.widths = widths;
        result.log_clusters = clusters;
        result.offset = item.get(JAVA_INT, ITEM_OFFSET);
        result.length = item.get(JAVA_INT, ITEM_LENGTH);
        result.num_chars = item.get(JAVA_INT, ITEM_NUM_CHARS);
        result.font = analysis.get(ADDRESS, ANALYSIS_FONT).address();
        return result;
    }

    /**
     * pango.c:234-239: {@code NewStringUTF(pango_font_description_get_family(desc))} - {@code null} for an unset
     * family, otherwise the bytes up to the terminator decoded as HotSpot decodes them. The pointer is borrowed
     * from the description and is read before returning.
     */
    static String pango_font_description_get_family(long desc) {
        try {
            MemorySegment family = (MemorySegment) Lib.PANGO_FONT_DESCRIPTION_GET_FAMILY.invokeExact(ptr(desc));
            if (family.address() == 0) {
                return null;
            }
            long length = (long) Lib.STRLEN.invokeExact(family);
            return JniStringCodec.fromNewStringUtf(bounded(family, length + 1));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * pango.c:241-251: nothing for a {@code null} family; otherwise {@code pango_font_description_set_family} with
     * the modified UTF-8 of {@code GetStringUTFChars}, released after the call (Pango copies the string).
     */
    static void pango_font_description_set_family(long desc, String family) {
        if (family == null) {
            return;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment text = JniStringCodec.allocateModifiedUtf8(scratch, family);
            Lib.PANGO_FONT_DESCRIPTION_SET_FAMILY.invokeExact(ptr(desc), text);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * pango.c:253-274: {@code FcConfigAppFontAddFile(config, file)} through the lazily opened
     * {@code libfontconfig.so.1} of this class, with the modified UTF-8 of {@code GetStringUTFChars} for the path.
     * {@code false} for a {@code null} path and whenever the library or its symbol cannot be found (the library is
     * looked for before the path is examined, as at pango.c:256-259); otherwise the {@code FcBool} result, an
     * {@code int} the C narrowed to {@code jboolean}.
     */
    static boolean FcConfigAppFontAddFile(long config, String file) {
        MethodHandle addFile = fcConfigAppFontAddFile(Lib.LINKER);
        boolean rc = false;
        if (file != null) {
            try (Arena scratch = Arena.ofConfined()) {
                MemorySegment text = JniStringCodec.allocateModifiedUtf8(scratch, file);
                if (addFile != null) {
                    int result = (int) addFile.invokeExact(ptr(config), text);
                    rc = (result & 0xFF) != 0;
                }
            } catch (Throwable t) {
                throw unexpected(t);
            }
        }
        return rc;
    }

    /* ---------------------------------------------------------------------------------------------
     * One to one - pango.c:276-461
     * ------------------------------------------------------------------------------------------- */

    /** pango.c:277-281: a new {@code GList} of new {@code PangoItem}s, each to be freed by the caller. */
    static long pango_itemize(long context, long text, int startIndex, int length, long attrs, long cachedIter) {
        try {
            return ((MemorySegment) Lib.PANGO_ITEMIZE.invokeExact(ptr(context), ptr(text), startIndex, length,
                    ptr(attrs), ptr(cachedIter))).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:283-287; {@code direction} is a {@code PangoDirection}, {@code OSPango.PANGO_DIRECTION_RTL} is 1. */
    static void pango_context_set_base_dir(long context, int direction) {
        try {
            Lib.PANGO_CONTEXT_SET_BASE_DIR.invokeExact(ptr(context), direction);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:289-293: a new description the caller frees. */
    static long pango_font_describe(long font) {
        try {
            return ((MemorySegment) Lib.PANGO_FONT_DESCRIBE.invokeExact(ptr(font))).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:295-299: the {@code jboolean} widened to a {@code gboolean}, 1 or 0. */
    static long pango_attr_fallback_new(boolean enableFallback) {
        try {
            return ((MemorySegment) Lib.PANGO_ATTR_FALLBACK_NEW.invokeExact(enableFallback ? 1 : 0)).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:307-311: a {@code PangoStyle}. */
    static int pango_font_description_get_style(long desc) {
        try {
            return (int) Lib.PANGO_FONT_DESCRIPTION_GET_STYLE.invokeExact(ptr(desc));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:313-317: a {@code PangoWeight}. */
    static int pango_font_description_get_weight(long desc) {
        try {
            return (int) Lib.PANGO_FONT_DESCRIPTION_GET_WEIGHT.invokeExact(ptr(desc));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:319-323 ({@code libpangoft2-1.0.so.0}): the font map {@code PangoGlyphLayout} keeps for the process. */
    static long pango_ft2_font_map_new() {
        try {
            return ((MemorySegment) Lib.PANGO_FT2_FONT_MAP_NEW.invokeExact()).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:325-329: a new context, released with {@link #g_object_unref}. */
    static long pango_font_map_create_context(long fontmap) {
        try {
            return ((MemorySegment) Lib.PANGO_FONT_MAP_CREATE_CONTEXT.invokeExact(ptr(fontmap))).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:331-335 ({@code libgobject-2.0.so.0}). */
    static void g_object_unref(long object) {
        try {
            Lib.G_OBJECT_UNREF.invokeExact(ptr(object));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:337-341: a new description, released with {@link #pango_font_description_free}. */
    static long pango_font_description_new() {
        try {
            return ((MemorySegment) Lib.PANGO_FONT_DESCRIPTION_NEW.invokeExact()).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:343-347: the size in Pango units, a {@code double}. */
    static void pango_font_description_set_absolute_size(long desc, double size) {
        try {
            Lib.PANGO_FONT_DESCRIPTION_SET_ABSOLUTE_SIZE.invokeExact(ptr(desc), size);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:349-353: a {@code PangoStretch}. */
    static void pango_font_description_set_stretch(long desc, int stretch) {
        try {
            Lib.PANGO_FONT_DESCRIPTION_SET_STRETCH.invokeExact(ptr(desc), stretch);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:355-359: a {@code PangoStyle}. */
    static void pango_font_description_set_style(long desc, int style) {
        try {
            Lib.PANGO_FONT_DESCRIPTION_SET_STYLE.invokeExact(ptr(desc), style);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:361-365: a {@code PangoWeight}. */
    static void pango_font_description_set_weight(long desc, int weight) {
        try {
            Lib.PANGO_FONT_DESCRIPTION_SET_WEIGHT.invokeExact(ptr(desc), weight);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:367-371: a new list, released with {@link #pango_attr_list_unref}. */
    static long pango_attr_list_new() {
        try {
            return ((MemorySegment) Lib.PANGO_ATTR_LIST_NEW.invokeExact()).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:373-377: a new attribute holding a copy of {@code desc}, owned by the list it is inserted into. */
    static long pango_attr_font_desc_new(long desc) {
        try {
            return ((MemorySegment) Lib.PANGO_ATTR_FONT_DESC_NEW.invokeExact(ptr(desc))).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:379-383: the list takes ownership of {@code attr}. */
    static void pango_attr_list_insert(long list, long attr) {
        try {
            Lib.PANGO_ATTR_LIST_INSERT.invokeExact(ptr(list), ptr(attr));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:385-389 ({@code libglib-2.0.so.0}): a {@code guint}. */
    static int g_list_length(long list) {
        try {
            return (int) Lib.G_LIST_LENGTH.invokeExact(ptr(list));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:391-395: {@code n} as a {@code guint}. */
    static long g_list_nth_data(long list, int n) {
        try {
            return ((MemorySegment) Lib.G_LIST_NTH_DATA.invokeExact(ptr(list), n)).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:397-401: frees the item and drops its reference on the analysis font. */
    static void pango_item_free(long item) {
        try {
            Lib.PANGO_ITEM_FREE.invokeExact(ptr(item));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:403-407: frees the links, not the items. */
    static void g_list_free(long list) {
        try {
            Lib.G_LIST_FREE.invokeExact(ptr(list));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:409-414: {@code 0} for a null string without a call; {@code offset} is a {@code glong}. */
    static long g_utf8_offset_to_pointer(long str, long offset) {
        if (str == 0) {
            return 0;
        }
        try {
            return ((MemorySegment) Lib.G_UTF8_OFFSET_TO_POINTER.invokeExact(ptr(str), offset)).address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:423-428: {@code 0} for a null string without a call; {@code max} is a {@code gssize}. */
    static long g_utf8_strlen(long str, long max) {
        if (str == 0) {
            return 0;
        }
        try {
            return (long) Lib.G_UTF8_STRLEN.invokeExact(ptr(str), max);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * pango.c:430-443: {@code g_utf16_to_utf8(units, length, NULL, NULL, NULL)} over the raw code units of
     * {@code str} - {@code 0} for a null array, otherwise the address of a new GLib buffer ({@code 0} when GLib
     * rejects the input, which it does for any unpaired surrogate) that the caller releases with {@link #g_free}.
     * The units are copied into a native buffer for the call; an empty array is still passed as a non-null
     * pointer with length 0, as the address of the empty Java array was, for which GLib returns an empty string.
     */
    static long g_utf16_to_utf8(char[] str) {
        if (str == null) {
            return 0;
        }
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment units = scratch.allocate(JAVA_CHAR, Math.max(str.length, 1));
            MemorySegment.copy(str, 0, units, JAVA_CHAR, 0, str.length);
            MemorySegment utf8 = (MemorySegment) Lib.G_UTF16_TO_UTF8.invokeExact(units, (long) str.length,
                    MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
            return utf8.address();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:445-449: releases a GLib buffer, on whichever thread calls. */
    static void g_free(long ptr) {
        try {
            Lib.G_FREE.invokeExact(ptr(ptr));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:451-455: also frees the attributes the list contains. */
    static void pango_attr_list_unref(long list) {
        try {
            Lib.PANGO_ATTR_LIST_UNREF.invokeExact(ptr(list));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** pango.c:457-461. */
    static void pango_font_description_free(long desc) {
        try {
            Lib.PANGO_FONT_DESCRIPTION_FREE.invokeExact(ptr(desc));
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }
}
