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

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test access to what {@link PangoNative} keeps package-private beyond the entry points {@link OSPangoShim}
 * delegates to: the struct layouts it reads through, the sonames it binds, its library loader's error mapping
 * and the glyph count at which {@code pango_shape} gives up. Compiled into the module by the
 * {@code compile-shims} execution of the javafx.graphics pom; reached by the tests through the
 * {@code --add-exports javafx.graphics/com.sun.javafx.font.freetype=ALL-UNNAMED} line of
 * {@code src/test/addExports}. No restricted {@code java.lang.foreign} call is made here: every one stays in
 * {@code PangoNative}. Not one of the pure delegate shims the goldens tie to production.
 */
public final class PangoNativeShim {

    private PangoNativeShim() {
    }

    /** The sonames {@link PangoNative} binds, fontconfig last. */
    public static List<String> libraries() {
        List<String> libraries = new ArrayList<>(PangoNative.LIBRARIES);
        libraries.add(PangoNative.LIB_FONTCONFIG);
        return libraries;
    }

    /**
     * {@link PangoNative}'s library loader on {@code soname}: returns normally when it loads, throws what the
     * binding's initializer would throw when it does not.
     */
    public static void library(String soname) {
        PangoNative.library(soname);
    }

    /** {@code PangoNative.MAX_GLYPHS}: the glyph count at which {@code pango_shape} returns nothing. */
    public static int maxGlyphs() {
        return PangoNative.MAX_GLYPHS;
    }

    /** Sizes and offsets of the struct layouts {@link PangoNative} reads through, in bytes. */
    public static Map<String, Long> layout() {
        Map<String, Long> layout = new LinkedHashMap<>();
        StructLayout item = PangoNative.PANGO_ITEM_LAYOUT;
        StructLayout analysis = PangoNative.PANGO_ANALYSIS_LAYOUT;
        StructLayout glyphString = PangoNative.PANGO_GLYPH_STRING_LAYOUT;
        StructLayout glyphInfo = PangoNative.PANGO_GLYPH_INFO_LAYOUT;
        StructLayout geometry = PangoNative.PANGO_GLYPH_GEOMETRY_LAYOUT;
        layout.put("PangoAnalysis.size", analysis.byteSize());
        layout.put("PangoAnalysis.align", analysis.byteAlignment());
        layout.put("PangoAnalysis.font", offset(analysis, "font"));
        layout.put("PangoAnalysis.level", offset(analysis, "level"));
        layout.put("PangoAnalysis.script", offset(analysis, "script"));
        layout.put("PangoItem.size", item.byteSize());
        layout.put("PangoItem.align", item.byteAlignment());
        layout.put("PangoItem.offset", offset(item, "offset"));
        layout.put("PangoItem.length", offset(item, "length"));
        layout.put("PangoItem.num_chars", offset(item, "num_chars"));
        layout.put("PangoItem.analysis", offset(item, "analysis"));
        layout.put("PangoItem.analysis.font", offset(item, "analysis") + offset(analysis, "font"));
        layout.put("PangoItem.analysis.level", offset(item, "analysis") + offset(analysis, "level"));
        layout.put("PangoItem.analysis.script", offset(item, "analysis") + offset(analysis, "script"));
        layout.put("PangoGlyphGeometry.size", geometry.byteSize());
        layout.put("PangoGlyphGeometry.width", offset(geometry, "width"));
        layout.put("PangoGlyphInfo.size", glyphInfo.byteSize());
        layout.put("PangoGlyphInfo.align", glyphInfo.byteAlignment());
        layout.put("PangoGlyphInfo.glyph", offset(glyphInfo, "glyph"));
        layout.put("PangoGlyphInfo.geometry", offset(glyphInfo, "geometry"));
        layout.put("PangoGlyphInfo.geometry.width", offset(glyphInfo, "geometry") + offset(geometry, "width"));
        layout.put("PangoGlyphInfo.attr", offset(glyphInfo, "attr"));
        layout.put("PangoGlyphString.size", glyphString.byteSize());
        layout.put("PangoGlyphString.align", glyphString.byteAlignment());
        layout.put("PangoGlyphString.num_glyphs", offset(glyphString, "num_glyphs"));
        layout.put("PangoGlyphString.glyphs", offset(glyphString, "glyphs"));
        layout.put("PangoGlyphString.log_clusters", offset(glyphString, "log_clusters"));
        layout.put("PangoGlyphString.space", offset(glyphString, "space"));
        return layout;
    }

    private static long offset(StructLayout layout, String field) {
        return layout.byteOffset(MemoryLayout.PathElement.groupElement(field));
    }
}
