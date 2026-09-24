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

/**
 * Test seam onto the Pango, GLib and fontconfig entry points of the Linux font layer: the {@link PangoNative}
 * methods that {@link PangoGlyphLayout} and {@link FTFactory} call.
 * <p>
 * The Linux Pango golden ({@code test.com.sun.javafx.font.LinuxPangoGoldenTest}) and the font registration
 * child of {@code test.com.sun.javafx.font.LinuxFontProcessGoldenTest} were captured through this class from
 * the JNI build of commit {@code 7b43255b30} ({@code OSPango}, {@code pango.c}); it now delegates to the
 * {@code java.lang.foreign} binding that replaced those natives, so the same goldens verify it. It is a pure
 * delegate with no {@code java.lang.foreign} of its own. The two natives no Java code called
 * ({@code pango_font_description_get_stretch}, {@code g_utf8_pointer_to_offset}) have no delegate and no
 * replacement.
 */
public final class OSPangoShim {

    private OSPangoShim() {
    }

    /**
     * What {@code pango_shape} hands to {@link PangoGlyphLayout}: the fields it reads, and nothing else.
     *
     * @param numGlyphs {@code PangoGlyphString.num_glyphs}
     * @param numChars {@code PangoItem.num_chars}
     * @param font {@code PangoItem.analysis.font}, a borrowed pointer
     * @param glyphs {@code PangoGlyphInfo.glyph} per glyph, unmasked
     * @param widths {@code PangoGlyphInfo.geometry.width} per glyph
     * @param logClusters per glyph, in code points from the item start
     */
    public record Shaped(int numGlyphs, int numChars, long font, int[] glyphs, int[] widths, int[] logClusters) {
    }

    /**
     * Initialises the Pango binding, which loads its libraries: the lazily initialised holder of
     * {@link PangoNative}, so that a missing library fails here rather than at the first call.
     */
    public static void ensureLoaded() {
        try {
            Class.forName(PangoNative.class.getName() + "$Lib", true, PangoNative.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void pango_context_set_base_dir(long context, int direction) {
        PangoNative.pango_context_set_base_dir(context, direction);
    }

    public static long pango_ft2_font_map_new() {
        return PangoNative.pango_ft2_font_map_new();
    }

    public static long pango_font_map_create_context(long fontmap) {
        return PangoNative.pango_font_map_create_context(fontmap);
    }

    public static long pango_font_describe(long font) {
        return PangoNative.pango_font_describe(font);
    }

    public static long pango_font_description_new() {
        return PangoNative.pango_font_description_new();
    }

    public static void pango_font_description_free(long desc) {
        PangoNative.pango_font_description_free(desc);
    }

    public static String pango_font_description_get_family(long desc) {
        return PangoNative.pango_font_description_get_family(desc);
    }

    public static int pango_font_description_get_style(long desc) {
        return PangoNative.pango_font_description_get_style(desc);
    }

    public static int pango_font_description_get_weight(long desc) {
        return PangoNative.pango_font_description_get_weight(desc);
    }

    public static void pango_font_description_set_family(long desc, String family) {
        PangoNative.pango_font_description_set_family(desc, family);
    }

    public static void pango_font_description_set_absolute_size(long desc, double size) {
        PangoNative.pango_font_description_set_absolute_size(desc, size);
    }

    public static void pango_font_description_set_stretch(long desc, int stretch) {
        PangoNative.pango_font_description_set_stretch(desc, stretch);
    }

    public static void pango_font_description_set_style(long desc, int style) {
        PangoNative.pango_font_description_set_style(desc, style);
    }

    public static void pango_font_description_set_weight(long desc, int weight) {
        PangoNative.pango_font_description_set_weight(desc, weight);
    }

    public static long pango_attr_list_new() {
        return PangoNative.pango_attr_list_new();
    }

    public static long pango_attr_font_desc_new(long desc) {
        return PangoNative.pango_attr_font_desc_new(desc);
    }

    public static long pango_attr_fallback_new(boolean enableFallback) {
        return PangoNative.pango_attr_fallback_new(enableFallback);
    }

    public static void pango_attr_list_unref(long list) {
        PangoNative.pango_attr_list_unref(list);
    }

    public static void pango_attr_list_insert(long list, long attr) {
        PangoNative.pango_attr_list_insert(list, attr);
    }

    public static long pango_itemize(long context, long text, int startIndex, int length, long attrs,
                                     long cachedIter) {
        return PangoNative.pango_itemize(context, text, startIndex, length, attrs, cachedIter);
    }

    /** {@code pango_shape} as {@link PangoGlyphLayout} receives it, or {@code null} when the binding returns none. */
    public static Shaped pango_shape(long text, long pangoItem) {
        PangoGlyphString shaped = PangoNative.pango_shape(text, pangoItem);
        if (shaped == null) {
            return null;
        }
        return new Shaped(shaped.num_glyphs, shaped.num_chars, shaped.font, shaped.glyphs, shaped.widths,
                          shaped.log_clusters);
    }

    public static void pango_item_free(long item) {
        PangoNative.pango_item_free(item);
    }

    public static long g_utf8_offset_to_pointer(long str, long offset) {
        return PangoNative.g_utf8_offset_to_pointer(str, offset);
    }

    public static long g_utf8_strlen(long str, long max) {
        return PangoNative.g_utf8_strlen(str, max);
    }

    public static long g_utf16_to_utf8(char[] str) {
        return PangoNative.g_utf16_to_utf8(str);
    }

    public static void g_free(long ptr) {
        PangoNative.g_free(ptr);
    }

    public static int g_list_length(long list) {
        return PangoNative.g_list_length(list);
    }

    public static long g_list_nth_data(long list, int n) {
        return PangoNative.g_list_nth_data(list, n);
    }

    public static void g_list_free(long list) {
        PangoNative.g_list_free(list);
    }

    public static void g_object_unref(long object) {
        PangoNative.g_object_unref(object);
    }

    public static boolean FcConfigAppFontAddFile(long config, String file) {
        return PangoNative.FcConfigAppFontAddFile(config, file);
    }

    /** A Pango constant as {@code OSPango}, which keeps them, defines it. */
    public static int constant(String name) {
        return switch (name) {
            case "PANGO_SCALE" -> OSPango.PANGO_SCALE;
            case "PANGO_STRETCH_NORMAL" -> OSPango.PANGO_STRETCH_NORMAL;
            case "PANGO_STYLE_NORMAL" -> OSPango.PANGO_STYLE_NORMAL;
            case "PANGO_STYLE_OBLIQUE" -> OSPango.PANGO_STYLE_OBLIQUE;
            case "PANGO_STYLE_ITALIC" -> OSPango.PANGO_STYLE_ITALIC;
            case "PANGO_WEIGHT_NORMAL" -> OSPango.PANGO_WEIGHT_NORMAL;
            case "PANGO_WEIGHT_BOLD" -> OSPango.PANGO_WEIGHT_BOLD;
            case "PANGO_DIRECTION_RTL" -> OSPango.PANGO_DIRECTION_RTL;
            default -> throw new IllegalArgumentException("no Pango constant " + name);
        };
    }
}
