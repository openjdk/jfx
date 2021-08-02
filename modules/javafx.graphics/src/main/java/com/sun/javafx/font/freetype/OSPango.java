/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.glass.utils.NativeLibLoader;

class OSPango {

    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            NativeLibLoader.loadLibrary("javafx_font_pango");
            return null;
        });
    }

    /* Pango */
    static final int PANGO_SCALE = 1024;
    static final int PANGO_STRETCH_ULTRA_CONDENSED = 0x0;
    static final int PANGO_STRETCH_EXTRA_CONDENSED = 0x1;
    static final int PANGO_STRETCH_CONDENSED = 0x2;
    static final int PANGO_STRETCH_SEMI_CONDENSED = 0x3;
    static final int PANGO_STRETCH_NORMAL = 0x4;
    static final int PANGO_STRETCH_SEMI_EXPANDED = 0x5;
    static final int PANGO_STRETCH_EXPANDED = 0x6;
    static final int PANGO_STRETCH_EXTRA_EXPANDED = 0x7;
    static final int PANGO_STRETCH_ULTRA_EXPANDED = 0x8;
    static final int PANGO_STYLE_ITALIC = 0x2;
    static final int PANGO_STYLE_NORMAL = 0x0;
    static final int PANGO_STYLE_OBLIQUE = 0x1;
    static final int PANGO_WEIGHT_BOLD = 0x2bc;
    static final int PANGO_WEIGHT_NORMAL = 0x190;
    static final int PANGO_DIRECTION_RTL = 1;

    static final native void pango_context_set_base_dir(long context, int direction);
    static final native long pango_ft2_font_map_new();
    static final native long pango_font_map_create_context(long fontmap);
    static final native long pango_font_describe(long font);
    static final native long pango_font_description_new();
    static final native void pango_font_description_free(long desc);
    static final native String pango_font_description_get_family(long desc);
    static final native int pango_font_description_get_stretch(long desc);
    static final native int pango_font_description_get_style(long desc);
    static final native int pango_font_description_get_weight(long desc);
    static final native void pango_font_description_set_family(long desc, String family);
    static final native void pango_font_description_set_absolute_size(long desc, double size);
    static final native void pango_font_description_set_stretch(long desc, int stretch);
    static final native void pango_font_description_set_style(long desc, int style);
    static final native void pango_font_description_set_weight(long desc, int weight);
    static final native long pango_attr_list_new();
    static final native long pango_attr_font_desc_new(long desc);
    static final native long pango_attr_fallback_new(boolean enable_fallback);
    static final native void pango_attr_list_unref(long list);
    static final native void pango_attr_list_insert(long list, long attr);
    static final native long pango_itemize(long context, long text, int start_index, int length, long attrs, long cached_iter);
    static final native PangoGlyphString pango_shape(long text, long pangoItem);
    static final native void pango_item_free(long item);

    /* Miscellaneous (glib, fontconfig) */
    static final native long g_utf8_offset_to_pointer(long str, long offset);
    static final native long g_utf8_pointer_to_offset(long str, long pos);
    static final native long g_utf8_strlen(long str, long max);
    static final native long g_utf16_to_utf8(char[] str);
    static final native void g_free(long ptr);
    static final native int g_list_length(long list);
    static final native long g_list_nth_data(long list, int n);
    static final native void g_list_free(long list);
    static final native void g_object_unref(long object);
    static final native boolean FcConfigAppFontAddFile(long config, String file);

}
