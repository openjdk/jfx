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

package com.sun.javafx.font;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;

/**
 * An independent observer of the system font libraries for the Linux font tests: its own
 * {@code java.lang.foreign} bindings to {@code libfreetype.so.6}, {@code libpango-1.0.so.0},
 * {@code libpangoft2-1.0.so.0}, {@code libgobject-2.0.so.0}, {@code libglib-2.0.so.0},
 * {@code libharfbuzz.so.0}, {@code libfontconfig.so.1} and libc.
 * <p>
 * The goldens use it to see what the production font layer cannot report about itself: the bytes Pango
 * stored for a family name, the public fields of a {@code PangoItem}, the number of fontconfig application
 * fonts, the C environment, the C heap in use and the resident set. The ABI tests
 * ({@code test.com.sun.javafx.font.LinuxFontAbiTest}) use it to check
 * the record layouts the Java side depends on against the running libraries, with no C of the project
 * involved. It must never delegate to the production bindings it observes: an observer that calls the code
 * under test proves nothing.
 * <p>
 * Every library is bound lazily in a holder class, so loading this class binds nothing. fontconfig is the
 * exception to the holders: it is looked up in a confined arena for the duration of each call and unloaded
 * afterwards, so that the observer never keeps it mapped in a process whose mappings a test counts.
 * <p>
 * Offsets are those of the LP64 Linux ABI (x86_64 and aarch64 have the same layouts for these records),
 * measured with {@code offsetof} against FreeType 2.14.2, Pango 1.57.0 and fontconfig 2.17.1 on x86_64.
 */
public final class LinuxFontOracleShim {

    private static final Linker LINKER = Linker.nativeLinker();

    private static final int MAX_C_STRING = 1 << 20;

    /** {@code FT_FaceRec} and {@code FT_GlyphSlotRec} offsets read by the FreeType binding. */
    private static final long FACE_NUM_GLYPHS = 32;
    private static final long FACE_UNITS_PER_EM = 136;
    private static final long FACE_GLYPH = 152;

    private LinuxFontOracleShim() {
    }

    // ---------------------------------------------------------------------------------------------
    // Memory
    // ---------------------------------------------------------------------------------------------

    /** The bytes of the NUL-terminated string at {@code address}, without the NUL; {@code null} for address 0. */
    @SuppressWarnings("restricted")
    public static byte[] cString(long address, int max) {
        if (address == 0) {
            return null;
        }
        MemorySegment string = MemorySegment.ofAddress(address).reinterpret(max);
        for (int i = 0; i < max; i++) {
            if (string.get(JAVA_BYTE, i) == 0) {
                return string.asSlice(0, i).toArray(JAVA_BYTE);
            }
        }
        throw new IllegalStateException("no NUL within " + max + " bytes at 0x" + Long.toHexString(address));
    }

    public static long peekLong(long address, long offset) {
        return at(address, offset, Long.BYTES).get(JAVA_LONG_UNALIGNED, 0);
    }

    public static int peekInt(long address, long offset) {
        return at(address, offset, Integer.BYTES).get(JAVA_INT_UNALIGNED, 0);
    }

    public static int peekUnsignedShort(long address, long offset) {
        return at(address, offset, Short.BYTES).get(JAVA_SHORT_UNALIGNED, 0) & 0xFFFF;
    }

    public static int peekUnsignedByte(long address, long offset) {
        return at(address, offset, 1).get(JAVA_BYTE, 0) & 0xFF;
    }

    @SuppressWarnings("restricted")
    private static MemorySegment at(long address, long offset, long size) {
        if (address == 0) {
            throw new IllegalArgumentException("read through a NULL pointer at offset " + offset);
        }
        return MemorySegment.ofAddress(address + offset).reinterpret(size);
    }

    /** Whether {@code soname} can be loaded here; the probe load is released before returning. */
    @SuppressWarnings("restricted")
    public static boolean libraryAvailable(String soname) {
        try (Arena probe = Arena.ofConfined()) {
            SymbolLookup.libraryLookup(soname, probe);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // libc
    // ---------------------------------------------------------------------------------------------

    /** libc {@code getenv}: the C environment as it is now, not the JDK's snapshot of it. */
    public static String libcGetenv(String name) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment value = (MemorySegment) LibC.GETENV.invokeExact(call.allocateFrom(name));
            byte[] bytes = cString(value.address(), MAX_C_STRING);
            return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code count} strings of {@code length} letters {@code x}, made by {@code strdup} and then all released by
     * {@code free}: every free chunk of that size class the allocator holds is handed out again, overwritten,
     * and on release given allocator links in its first bytes. A string that was passed to {@code putenv} and
     * freed afterwards is corrupted by this, so {@code getenv} no longer finds its variable.
     */
    public static void mallocChurn(int count, int length) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment text = call.allocateFrom("x".repeat(length));
            MemorySegment[] copies = new MemorySegment[count];
            for (int i = 0; i < count; i++) {
                copies[i] = (MemorySegment) LibC.STRDUP.invokeExact(text);
                if (copies[i].address() == 0) {
                    throw new IllegalStateException("strdup returned NULL after " + i + " copies");
                }
            }
            for (MemorySegment copy : copies) {
                LibC.FREE.invokeExact(copy);
            }
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * Bytes in use from the C heap: {@code mallinfo2().uordblks + hblkhd}, the allocated chunks of the main
     * arena plus the blocks served by {@code mmap}; {@code -1} where libc exports no {@code mallinfo2} (glibc
     * 2.33 and later do).
     */
    public static long mallocInUseBytes() {
        if (LibC.MALLINFO2 == null) {
            return -1;
        }
        try (Arena call = Arena.ofConfined()) {
            MemorySegment info = (MemorySegment) LibC.MALLINFO2.invokeExact((SegmentAllocator) call);
            return info.get(JAVA_LONG, MALLINFO2_UORDBLKS) + info.get(JAVA_LONG, MALLINFO2_HBLKHD);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** The resident set of this process in bytes: the second field of {@code /proc/self/statm} times the page size. */
    public static long residentSetBytes() {
        try {
            String statm = Files.readString(Path.of("/proc/self/statm"), StandardCharsets.US_ASCII);
            String[] fields = statm.trim().split(" ");
            int pageSize = (int) LibC.GETPAGESIZE.invokeExact();
            return Long.parseLong(fields[1]) * pageSize;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code struct mallinfo2}: ten {@code size_t} fields; {@code hblkhd} is field 4, {@code uordblks} field 7. */
    private static final long MALLINFO2_HBLKHD = 4 * Long.BYTES;
    private static final long MALLINFO2_UORDBLKS = 7 * Long.BYTES;

    @SuppressWarnings("restricted")
    private static final class LibC {
        static final MethodHandle GETENV = LINKER.downcallHandle(
                LINKER.defaultLookup().find("getenv").orElseThrow(), FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle GETPAGESIZE = LINKER.downcallHandle(
                LINKER.defaultLookup().find("getpagesize").orElseThrow(), FunctionDescriptor.of(JAVA_INT));
        static final MethodHandle STRDUP = LINKER.downcallHandle(
                LINKER.defaultLookup().find("strdup").orElseThrow(), FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle FREE = LINKER.downcallHandle(
                LINKER.defaultLookup().find("free").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));
        static final MemoryLayout MALLINFO2_LAYOUT = MemoryLayout.structLayout(
                JAVA_LONG.withName("arena"), JAVA_LONG.withName("ordblks"), JAVA_LONG.withName("smblks"),
                JAVA_LONG.withName("hblks"), JAVA_LONG.withName("hblkhd"), JAVA_LONG.withName("usmblks"),
                JAVA_LONG.withName("fsmblks"), JAVA_LONG.withName("uordblks"), JAVA_LONG.withName("fordblks"),
                JAVA_LONG.withName("keepcost"));
        static final MethodHandle MALLINFO2 = LINKER.defaultLookup().find("mallinfo2")
                .map(symbol -> LINKER.downcallHandle(symbol, FunctionDescriptor.of(MALLINFO2_LAYOUT)))
                .orElse(null);
    }

    // ---------------------------------------------------------------------------------------------
    // FreeType
    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("restricted")
    private static final class FreeType {
        static final SymbolLookup LIB = SymbolLookup.libraryLookup("libfreetype.so.6", Arena.global());
        static final MemoryLayout C_LONG = LINKER.canonicalLayouts().get("long");
        static final MethodHandle INIT = handle(LIB, "FT_Init_FreeType", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle DONE = handle(LIB, "FT_Done_FreeType", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle VERSION = handle(LIB, "FT_Library_Version",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        static final MethodHandle NEW_FACE = handle(LIB, "FT_New_Face",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, C_LONG, ADDRESS));
        static final MethodHandle DONE_FACE = handle(LIB, "FT_Done_Face", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle SET_CHAR_SIZE = handle(LIB, "FT_Set_Char_Size",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, C_LONG, C_LONG, JAVA_INT, JAVA_INT));
        static final MethodHandle LOAD_GLYPH = handle(LIB, "FT_Load_Glyph",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
    }

    /** {@code FT_Library_Version} of a library this observer creates and destroys. */
    public static String freetypeVersion() {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment out = call.allocate(ADDRESS);
            int rc = (int) FreeType.INIT.invokeExact(out);
            if (rc != 0) {
                throw new IllegalStateException("FT_Init_FreeType returned " + rc);
            }
            MemorySegment library = out.get(ADDRESS, 0);
            MemorySegment major = call.allocate(JAVA_INT);
            MemorySegment minor = call.allocate(JAVA_INT);
            MemorySegment patch = call.allocate(JAVA_INT);
            FreeType.VERSION.invokeExact(library, major, minor, patch);
            int done = (int) FreeType.DONE.invokeExact(library);
            if (done != 0) {
                throw new IllegalStateException("FT_Done_FreeType returned " + done);
            }
            return major.get(JAVA_INT, 0) + "." + minor.get(JAVA_INT, 0) + "." + patch.get(JAVA_INT, 0);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * Creates a library and a face for {@code fontPath}, sets the character size (skipped when
     * {@code charSize26dot6} is 0), loads {@code glyphIndex} with {@code loadFlags}, and returns the raw
     * values of the face and glyph slot records it then reads, before destroying both. Keys:
     * {@code init.rc, newFace.rc, charSize.rc, load.rc, library, face}, {@code face.<field>} and
     * {@code slot.<field>} as named in {@code freetype.h}.
     */
    public static Map<String, Long> freetypeSlotReads(String fontPath, int glyphIndex, int loadFlags,
                                                      long charSize26dot6) {
        Map<String, Long> reads = new LinkedHashMap<>();
        try (Arena call = Arena.ofConfined()) {
            MemorySegment libraryOut = call.allocate(ADDRESS);
            reads.put("init.rc", (long) (int) FreeType.INIT.invokeExact(libraryOut));
            MemorySegment library = libraryOut.get(ADDRESS, 0);
            reads.put("library", library.address());
            MemorySegment faceOut = call.allocate(ADDRESS);
            int newFace = (int) FreeType.NEW_FACE.invoke(library, call.allocateFrom(fontPath), 0L, faceOut);
            reads.put("newFace.rc", (long) newFace);
            if (newFace == 0) {
                MemorySegment face = faceOut.get(ADDRESS, 0);
                long f = face.address();
                reads.put("face", f);
                if (charSize26dot6 != 0) {
                    int size = (int) FreeType.SET_CHAR_SIZE.invoke(face, 0L, charSize26dot6, 72, 72);
                    reads.put("charSize.rc", (long) size);
                }
                reads.put("load.rc", (long) (int) FreeType.LOAD_GLYPH.invokeExact(face, glyphIndex, loadFlags));
                reads.put("face.num_glyphs", peekLong(f, FACE_NUM_GLYPHS));
                reads.put("face.units_per_EM", (long) peekUnsignedShort(f, FACE_UNITS_PER_EM));
                long slot = peekLong(f, FACE_GLYPH);
                reads.put("face.glyph", slot);
                if (slot != 0) {
                    reads.put("slot.library", peekLong(slot, 0));
                    reads.put("slot.face", peekLong(slot, 8));
                    reads.put("slot.glyph_index", (long) peekInt(slot, 24));
                    reads.put("slot.metrics.width", peekLong(slot, 48));
                    reads.put("slot.metrics.height", peekLong(slot, 56));
                    reads.put("slot.metrics.horiBearingX", peekLong(slot, 64));
                    reads.put("slot.metrics.horiBearingY", peekLong(slot, 72));
                    reads.put("slot.metrics.horiAdvance", peekLong(slot, 80));
                    reads.put("slot.linearHoriAdvance", peekLong(slot, 112));
                    reads.put("slot.advance.x", peekLong(slot, 128));
                    reads.put("slot.advance.y", peekLong(slot, 136));
                    reads.put("slot.format", (long) peekInt(slot, 144));
                    reads.put("slot.bitmap.rows", Integer.toUnsignedLong(peekInt(slot, 152)));
                    reads.put("slot.bitmap.width", Integer.toUnsignedLong(peekInt(slot, 156)));
                    reads.put("slot.bitmap.pitch", (long) peekInt(slot, 160));
                    reads.put("slot.bitmap.buffer", peekLong(slot, 168));
                    reads.put("slot.bitmap.num_grays", (long) peekUnsignedShort(slot, 176));
                    reads.put("slot.bitmap.pixel_mode", (long) peekUnsignedByte(slot, 178));
                    reads.put("slot.bitmap_left", (long) peekInt(slot, 192));
                    reads.put("slot.bitmap_top", (long) peekInt(slot, 196));
                    reads.put("slot.outline.n_contours", (long) peekUnsignedShort(slot, 200));
                    reads.put("slot.outline.n_points", (long) peekUnsignedShort(slot, 202));
                    reads.put("slot.outline.points", peekLong(slot, 208));
                    reads.put("slot.outline.tags", peekLong(slot, 216));
                    reads.put("slot.outline.contours", peekLong(slot, 224));
                }
                reads.put("doneFace.rc", (long) (int) FreeType.DONE_FACE.invokeExact(face));
            }
            reads.put("done.rc", (long) (int) FreeType.DONE.invokeExact(library));
            return reads;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Pango, GLib, HarfBuzz
    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("restricted")
    private static final class Pango {
        static final SymbolLookup PANGO = SymbolLookup.libraryLookup("libpango-1.0.so.0", Arena.global());
        static final SymbolLookup PANGOFT2 = SymbolLookup.libraryLookup("libpangoft2-1.0.so.0", Arena.global());
        static final SymbolLookup GOBJECT = SymbolLookup.libraryLookup("libgobject-2.0.so.0", Arena.global());
        static final SymbolLookup GLIB = SymbolLookup.libraryLookup("libglib-2.0.so.0", Arena.global());
        static final MethodHandle VERSION_STRING = handle(PANGO, "pango_version_string",
                FunctionDescriptor.of(ADDRESS));
        static final MethodHandle SET_FAMILY = handle(PANGO, "pango_font_description_set_family",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
        static final MethodHandle GET_FAMILY = handle(PANGO, "pango_font_description_get_family",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle FONT_MAP_NEW = handle(PANGOFT2, "pango_ft2_font_map_new",
                FunctionDescriptor.of(ADDRESS));
        static final MethodHandle CREATE_CONTEXT = handle(PANGO, "pango_font_map_create_context",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
        static final MethodHandle ATTR_LIST_NEW = handle(PANGO, "pango_attr_list_new", FunctionDescriptor.of(ADDRESS));
        static final MethodHandle ATTR_LIST_UNREF = handle(PANGO, "pango_attr_list_unref",
                FunctionDescriptor.ofVoid(ADDRESS));
        static final MethodHandle ITEMIZE = handle(PANGO, "pango_itemize",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS));
        static final MethodHandle ITEM_FREE = handle(PANGO, "pango_item_free", FunctionDescriptor.ofVoid(ADDRESS));
        static final MethodHandle SHAPE = handle(PANGO, "pango_shape",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, ADDRESS));
        static final MethodHandle GLYPH_STRING_NEW = handle(PANGO, "pango_glyph_string_new",
                FunctionDescriptor.of(ADDRESS));
        static final MethodHandle GLYPH_STRING_SET_SIZE = handle(PANGO, "pango_glyph_string_set_size",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        static final MethodHandle GLYPH_STRING_GET_WIDTH = handle(PANGO, "pango_glyph_string_get_width",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle GLYPH_STRING_FREE = handle(PANGO, "pango_glyph_string_free",
                FunctionDescriptor.ofVoid(ADDRESS));
        static final MethodHandle OBJECT_UNREF = handle(GOBJECT, "g_object_unref", FunctionDescriptor.ofVoid(ADDRESS));
        static final MethodHandle LIST_LENGTH = handle(GLIB, "g_list_length", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        static final MethodHandle LIST_NTH_DATA = handle(GLIB, "g_list_nth_data",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
        static final MethodHandle LIST_FREE = handle(GLIB, "g_list_free", FunctionDescriptor.ofVoid(ADDRESS));
    }

    @SuppressWarnings("restricted")
    private static final class HarfBuzz {
        static final SymbolLookup LIB = SymbolLookup.libraryLookup("libharfbuzz.so.0", Arena.global());
        static final MethodHandle VERSION_STRING = handle(LIB, "hb_version_string", FunctionDescriptor.of(ADDRESS));
    }

    /** {@code PangoItem} public fields: {@code offset, length, num_chars}; the analysis starts at 16. */
    private static final long ITEM_ANALYSIS = 16;
    private static final long ITEM_FONT = 32;
    private static final long ITEM_LEVEL = 40;
    private static final long ITEM_SCRIPT = 43;

    /** {@code pango_version_string()}. */
    public static String pangoVersion() {
        try {
            MemorySegment version = (MemorySegment) Pango.VERSION_STRING.invokeExact();
            return new String(cString(version.address(), MAX_C_STRING), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@code hb_version_string()}. */
    public static String harfbuzzVersion() {
        try {
            MemorySegment version = (MemorySegment) HarfBuzz.VERSION_STRING.invokeExact();
            return new String(cString(version.address(), MAX_C_STRING), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code pango_font_description_set_family} with exactly these bytes and a NUL terminator, bypassing any
     * Java string encoding. Pango copies the string, so the call arena is released on return.
     */
    public static void pangoSetFamilyRaw(long desc, byte[] bytesNoNul) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment family = call.allocate(bytesNoNul.length + 1L);
            MemorySegment.copy(bytesNoNul, 0, family, JAVA_BYTE, 0, bytesNoNul.length);
            family.set(JAVA_BYTE, bytesNoNul.length, (byte) 0);
            Pango.SET_FAMILY.invokeExact(MemorySegment.ofAddress(desc), family);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** The bytes {@code pango_font_description_get_family} returns, undecoded; {@code null} for an unset family. */
    public static byte[] pangoGetFamilyRaw(long desc) {
        try {
            MemorySegment family = (MemorySegment) Pango.GET_FAMILY.invokeExact(MemorySegment.ofAddress(desc));
            return cString(family.address(), MAX_C_STRING);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code {offset, length, num_chars, analysis.level, analysis.script}} of the {@code PangoItem} at
     * {@code item}. The level and script bytes are {@code PangoAnalysis} offsets 24 and 27 (measured).
     */
    public static int[] pangoItemPublic(long item) {
        return new int[] {
            peekInt(item, 0), peekInt(item, 4), peekInt(item, 8),
            peekUnsignedByte(item, ITEM_LEVEL), peekUnsignedByte(item, ITEM_SCRIPT)
        };
    }

    /** {@code item->analysis.font}. */
    public static long pangoItemFont(long item) {
        return peekLong(item, ITEM_FONT);
    }

    /**
     * The raw fields of a fresh {@code PangoGlyphString} ({@code new.*}) and of the same string after
     * {@code pango_glyph_string_set_size(size)} ({@code sized.*}): {@code num_glyphs} at 0, {@code glyphs} at 8,
     * {@code log_clusters} at 16.
     */
    public static Map<String, Long> pangoGlyphStringReads(int size) {
        Map<String, Long> reads = new LinkedHashMap<>();
        try {
            MemorySegment string = (MemorySegment) Pango.GLYPH_STRING_NEW.invokeExact();
            long s = string.address();
            reads.put("new.num_glyphs", (long) peekInt(s, 0));
            reads.put("new.glyphs", peekLong(s, 8));
            reads.put("new.log_clusters", peekLong(s, 16));
            Pango.GLYPH_STRING_SET_SIZE.invokeExact(string, size);
            reads.put("sized.num_glyphs", (long) peekInt(s, 0));
            reads.put("sized.glyphs", peekLong(s, 8));
            reads.put("sized.log_clusters", peekLong(s, 16));
            Pango.GLYPH_STRING_FREE.invokeExact(string);
            return reads;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * Itemizes and shapes {@code text} (UTF-8, no NUL) with a fresh {@code PangoFT2FontMap} and the context's
     * default font description, reading the records the Java side depends on: {@code items}, {@code list.data}
     * and {@code list.nthData0} ({@code GList.data} at 0 against {@code g_list_nth_data}), {@code item<i>.*}
     * for every item, and for item 0 the shaped glyph string: {@code gs.num_glyphs}, {@code gs.glyph.<g>},
     * {@code gs.width.<g>} ({@code PangoGlyphInfo} stride 20, width at 4), {@code gs.cluster.<g>},
     * {@code gs.widthSum} and {@code gs.getWidth} ({@code pango_glyph_string_get_width}).
     */
    public static Map<String, Long> pangoItemizeReads(String text) {
        Map<String, Long> reads = new LinkedHashMap<>();
        try (Arena call = Arena.ofConfined()) {
            byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
            MemorySegment string = call.allocate(utf8.length + 1L);
            MemorySegment.copy(utf8, 0, string, JAVA_BYTE, 0, utf8.length);
            string.set(JAVA_BYTE, utf8.length, (byte) 0);
            MemorySegment fontMap = (MemorySegment) Pango.FONT_MAP_NEW.invokeExact();
            MemorySegment context = (MemorySegment) Pango.CREATE_CONTEXT.invokeExact(fontMap);
            MemorySegment attrs = (MemorySegment) Pango.ATTR_LIST_NEW.invokeExact();
            MemorySegment list = (MemorySegment) Pango.ITEMIZE.invokeExact(context, string, 0, utf8.length, attrs,
                                                                           MemorySegment.NULL);
            int items = (int) Pango.LIST_LENGTH.invokeExact(list);
            reads.put("items", (long) items);
            if (items > 0) {
                reads.put("list.data", peekLong(list.address(), 0));
                MemorySegment first = (MemorySegment) Pango.LIST_NTH_DATA.invokeExact(list, 0);
                reads.put("list.nthData0", first.address());
            }
            for (int i = 0; i < items; i++) {
                MemorySegment item = (MemorySegment) Pango.LIST_NTH_DATA.invokeExact(list, i);
                long it = item.address();
                String prefix = "item" + i + ".";
                reads.put(prefix + "offset", (long) peekInt(it, 0));
                reads.put(prefix + "length", (long) peekInt(it, 4));
                reads.put(prefix + "num_chars", (long) peekInt(it, 8));
                reads.put(prefix + "font", peekLong(it, ITEM_FONT));
                reads.put(prefix + "level", (long) peekUnsignedByte(it, ITEM_LEVEL));
                reads.put(prefix + "script", (long) peekUnsignedByte(it, ITEM_SCRIPT));
                if (i == 0) {
                    MemorySegment glyphs = (MemorySegment) Pango.GLYPH_STRING_NEW.invokeExact();
                    Pango.SHAPE.invokeExact(string.asSlice(peekInt(it, 0)), peekInt(it, 4),
                                            MemorySegment.ofAddress(it + ITEM_ANALYSIS), glyphs);
                    long gs = glyphs.address();
                    int count = peekInt(gs, 0);
                    reads.put("gs.num_glyphs", (long) count);
                    long infos = peekLong(gs, 8);
                    long clusters = peekLong(gs, 16);
                    long widthSum = 0;
                    for (int g = 0; g < count; g++) {
                        reads.put("gs.glyph." + g, Integer.toUnsignedLong(peekInt(infos, 20L * g)));
                        int width = peekInt(infos, 20L * g + 4);
                        reads.put("gs.width." + g, (long) width);
                        widthSum += width;
                        reads.put("gs.cluster." + g, (long) peekInt(clusters, 4L * g));
                    }
                    reads.put("gs.widthSum", widthSum);
                    reads.put("gs.getWidth", (long) (int) Pango.GLYPH_STRING_GET_WIDTH.invokeExact(glyphs));
                    Pango.GLYPH_STRING_FREE.invokeExact(glyphs);
                }
            }
            for (int i = 0; i < items; i++) {
                MemorySegment item = (MemorySegment) Pango.LIST_NTH_DATA.invokeExact(list, i);
                Pango.ITEM_FREE.invokeExact(item);
            }
            Pango.LIST_FREE.invokeExact(list);
            Pango.ATTR_LIST_UNREF.invokeExact(attrs);
            Pango.OBJECT_UNREF.invokeExact(context);
            Pango.OBJECT_UNREF.invokeExact(fontMap);
            return reads;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // fontconfig (bound per call)
    // ---------------------------------------------------------------------------------------------

    private static final int FC_SET_SYSTEM = 0;
    private static final int FC_SET_APPLICATION = 1;
    private static final int FC_RESULT_MATCH = 0;

    /** {@code FcGetVersion()}, e.g. 21701 for 2.17.1. */
    public static int fcVersion() {
        try (Arena call = Arena.ofConfined()) {
            SymbolLookup fc = fontconfig(call);
            MethodHandle getVersion = handle(fc, "FcGetVersion", FunctionDescriptor.of(JAVA_INT));
            return (int) getVersion.invokeExact();
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * The number of application fonts in the current fontconfig configuration:
     * {@code FcConfigGetFonts(FcConfigGetCurrent(), FcSetApplication)->nfont}, 0 when there is no such set.
     */
    public static int fcApplicationFontCount() {
        try (Arena call = Arena.ofConfined()) {
            SymbolLookup fc = fontconfig(call);
            MethodHandle getCurrent = handle(fc, "FcConfigGetCurrent", FunctionDescriptor.of(ADDRESS));
            MethodHandle getFonts = handle(fc, "FcConfigGetFonts", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
            MemorySegment config = (MemorySegment) getCurrent.invokeExact();
            MemorySegment set = (MemorySegment) getFonts.invokeExact(config, FC_SET_APPLICATION);
            return set.address() == 0 ? 0 : peekInt(set.address(), 0);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * Raw reads of the {@code FcFontSet} records: {@code list.nfont}, {@code list.sfont}, {@code list.fonts} of
     * {@code FcFontList(NULL, <empty pattern>, {file})}, {@code list.font0.file.rc} and
     * {@code list.font0.file.bytes} (length) for its first pattern, and {@code system.nfont},
     * {@code system.sfont} of the current configuration's system set.
     */
    public static Map<String, Long> fontconfigFontSetReads() {
        Map<String, Long> reads = new LinkedHashMap<>();
        try (Arena call = Arena.ofConfined()) {
            SymbolLookup fc = fontconfig(call);
            MethodHandle patternCreate = handle(fc, "FcPatternCreate", FunctionDescriptor.of(ADDRESS));
            MethodHandle patternDestroy = handle(fc, "FcPatternDestroy", FunctionDescriptor.ofVoid(ADDRESS));
            MethodHandle objectSetCreate = handle(fc, "FcObjectSetCreate", FunctionDescriptor.of(ADDRESS));
            MethodHandle objectSetAdd = handle(fc, "FcObjectSetAdd",
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            MethodHandle objectSetDestroy = handle(fc, "FcObjectSetDestroy", FunctionDescriptor.ofVoid(ADDRESS));
            MethodHandle fontList = handle(fc, "FcFontList",
                    FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
            MethodHandle fontSetDestroy = handle(fc, "FcFontSetDestroy", FunctionDescriptor.ofVoid(ADDRESS));
            MethodHandle getString = handle(fc, "FcPatternGetString",
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));
            MethodHandle getCurrent = handle(fc, "FcConfigGetCurrent", FunctionDescriptor.of(ADDRESS));
            MethodHandle getFonts = handle(fc, "FcConfigGetFonts", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));

            MemorySegment pattern = (MemorySegment) patternCreate.invokeExact();
            MemorySegment objects = (MemorySegment) objectSetCreate.invokeExact();
            MemorySegment file = call.allocateFrom("file");
            int added = (int) objectSetAdd.invokeExact(objects, file);
            reads.put("objectSetAdd.rc", (long) added);
            MemorySegment set = (MemorySegment) fontList.invokeExact(MemorySegment.NULL, pattern, objects);
            long s = set.address();
            reads.put("list", s);
            if (s != 0) {
                int nfont = peekInt(s, 0);
                reads.put("list.nfont", (long) nfont);
                reads.put("list.sfont", (long) peekInt(s, 4));
                long fonts = peekLong(s, 8);
                reads.put("list.fonts", fonts);
                if (nfont > 0) {
                    long font0 = peekLong(fonts, 0);
                    reads.put("list.font0", font0);
                    MemorySegment out = call.allocate(ADDRESS);
                    int rc = (int) getString.invokeExact(MemorySegment.ofAddress(font0), file, 0, out);
                    reads.put("list.font0.file.rc", (long) rc);
                    if (rc == FC_RESULT_MATCH) {
                        byte[] bytes = cString(out.get(ADDRESS, 0).address(), MAX_C_STRING);
                        reads.put("list.font0.file.bytes", bytes == null ? -1L : bytes.length);
                    }
                }
                fontSetDestroy.invokeExact(set);
            }
            objectSetDestroy.invokeExact(objects);
            patternDestroy.invokeExact(pattern);
            MemorySegment config = (MemorySegment) getCurrent.invokeExact();
            MemorySegment system = (MemorySegment) getFonts.invokeExact(config, FC_SET_SYSTEM);
            if (system.address() != 0) {
                reads.put("system.nfont", (long) peekInt(system.address(), 0));
                reads.put("system.sfont", (long) peekInt(system.address(), 4));
            }
            return reads;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * The {@code file} of every outline pattern with a full name equal to {@code fullName} (ignoring case), in the
     * order {@code populateMapsNative} visits them: {@code FcFontList(NULL, pattern, objects)} with the pattern
     * {@code outline=true} and the object set {@code family, familylang, fullname, fullnamelang, file, fontformat}
     * in that order, as {@code fontpath_linux.c} builds them at commit {@code 7b43255b30}. fontconfig returns the
     * list in the order of a hash over exactly those values, so the same call in the same process sees the same
     * order. File names are the stored bytes decoded as UTF-8, not resolved.
     */
    public static List<String> fcOutlineFontFiles(String fullName) {
        List<String> files = new ArrayList<>();
        try (Arena call = Arena.ofConfined()) {
            SymbolLookup fc = fontconfig(call);
            MethodHandle patternCreate = handle(fc, "FcPatternCreate", FunctionDescriptor.of(ADDRESS));
            MethodHandle patternAddBool = handle(fc, "FcPatternAddBool",
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
            MethodHandle patternDestroy = handle(fc, "FcPatternDestroy", FunctionDescriptor.ofVoid(ADDRESS));
            MethodHandle objectSetCreate = handle(fc, "FcObjectSetCreate", FunctionDescriptor.of(ADDRESS));
            MethodHandle objectSetAdd = handle(fc, "FcObjectSetAdd",
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            MethodHandle objectSetDestroy = handle(fc, "FcObjectSetDestroy", FunctionDescriptor.ofVoid(ADDRESS));
            MethodHandle fontList = handle(fc, "FcFontList",
                    FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
            MethodHandle fontSetDestroy = handle(fc, "FcFontSetDestroy", FunctionDescriptor.ofVoid(ADDRESS));
            MethodHandle getString = handle(fc, "FcPatternGetString",
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));

            MemorySegment pattern = (MemorySegment) patternCreate.invokeExact();
            int added = (int) patternAddBool.invokeExact(pattern, call.allocateFrom("outline"), 1);
            if (added == 0) {
                throw new IllegalStateException("FcPatternAddBool(outline) failed");
            }
            MemorySegment objects = (MemorySegment) objectSetCreate.invokeExact();
            for (String object : List.of("family", "familylang", "fullname", "fullnamelang", "file", "fontformat")) {
                int rc = (int) objectSetAdd.invokeExact(objects, call.allocateFrom(object));
                if (rc == 0) {
                    throw new IllegalStateException("FcObjectSetAdd(" + object + ") failed");
                }
            }
            MemorySegment fullNameObject = call.allocateFrom("fullname");
            MemorySegment fileObject = call.allocateFrom("file");
            MemorySegment out = call.allocate(ADDRESS);
            MemorySegment set = (MemorySegment) fontList.invokeExact(MemorySegment.NULL, pattern, objects);
            if (set.address() != 0) {
                int nfont = peekInt(set.address(), 0);
                long fonts = peekLong(set.address(), 8);
                for (int f = 0; f < nfont; f++) {
                    MemorySegment font = MemorySegment.ofAddress(peekLong(fonts, 8L * f));
                    boolean named = false;
                    for (int n = 0; !named; n++) {
                        int rc = (int) getString.invokeExact(font, fullNameObject, n, out);
                        if (rc != FC_RESULT_MATCH) {
                            break;
                        }
                        byte[] name = cString(out.get(ADDRESS, 0).address(), MAX_C_STRING);
                        named = name != null && new String(name, StandardCharsets.UTF_8).equalsIgnoreCase(fullName);
                    }
                    if (named && (int) getString.invokeExact(font, fileObject, 0, out) == FC_RESULT_MATCH) {
                        byte[] file = cString(out.get(ADDRESS, 0).address(), MAX_C_STRING);
                        files.add(file == null ? null : new String(file, StandardCharsets.UTF_8));
                    }
                }
                fontSetDestroy.invokeExact(set);
            }
            objectSetDestroy.invokeExact(objects);
            patternDestroy.invokeExact(pattern);
            return files;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    @SuppressWarnings("restricted")
    private static SymbolLookup fontconfig(Arena call) {
        return SymbolLookup.libraryLookup("libfontconfig.so.1", call);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("restricted")
    private static MethodHandle handle(SymbolLookup library, String name, FunctionDescriptor descriptor) {
        MemorySegment symbol = library.find(name)
                .orElseThrow(() -> new UnsatisfiedLinkError("symbol " + name + " not found"));
        return LINKER.downcallHandle(symbol, descriptor);
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException runtime) {
            return runtime;
        }
        if (t instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(t);
    }
}
