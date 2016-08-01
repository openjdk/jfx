/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.text;

import com.sun.javafx.util.Utils;
import javafx.scene.text.Font;

/**
 * Used to access internal methods of Font.
 */
public class FontHelper {

    private static FontAccessor fontAccessor;

    static {
        Utils.forceInit(Font.class);
    }

    private FontHelper() {
    }

    public static Object getNativeFont(Font font) {
        return fontAccessor.getNativeFont(font);
    }

    public static void setNativeFont(Font font, Object f, String nam,
            String fam, String styl) {
        fontAccessor.setNativeFont(font, f, nam, fam, styl);
    }

    public static Font nativeFont(Object f, String name, String family,
                                       String style, double size) {
        return fontAccessor.nativeFont(f, name, family, style, size);
    }

    public static void setFontAccessor(final FontAccessor newAccessor) {
        if (fontAccessor != null) {
            throw new IllegalStateException();
        }

        fontAccessor = newAccessor;
    }

    public interface FontAccessor {
        Object getNativeFont(Font font);
        void setNativeFont(Font font, Object f, String nam, String fam, String styl);
        Font nativeFont(Object f, String name, String family, String style, double size);
    }

}
