/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.pgstub;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.FontMetrics;

public class StubFontLoader extends FontLoader {

    @Override
    public void loadFont(Font font) {
        String name = font.getName().trim().toLowerCase();
        if (name.equals("system") || name.equals("system regular")) {
            font.impl_setNativeFont(font, font.getName(), "System", "Regular");
        } else if (name.equals("amble regular")) {
            font.impl_setNativeFont(font, font.getName(), "Amble", "Regular");
        } else if (name.equals("amble bold")) {
            font.impl_setNativeFont(font, font.getName(), "Amble", "Bold");
        } else if (name.equals("amble italic")) {
            font.impl_setNativeFont(font, font.getName(), "Amble", "Italic");
        } else if (name.equals("amble bold italic")) {
            font.impl_setNativeFont(font, font.getName(), "Amble",
                    "Bold Italic");
        } else if (name.equals("amble condensed")) {
            font.impl_setNativeFont(font, font.getName(), "Amble Cn", "Regular");
        } else if (name.equals("amble bold condensed")) {
            font.impl_setNativeFont(font, font.getName(), "Amble Cn", "Bold");
        } else if (name.equals("amble condensed italic")) {
            font.impl_setNativeFont(font, font.getName(), "Amble Cn", "Italic");
        } else if (name.equals("amble bold condensed italic")) {
            font.impl_setNativeFont(font, font.getName(), "Amble Cn",
                    "Bold Italic");
        } else if (name.equals("amble light")) {
            font.impl_setNativeFont(font, font.getName(), "Amble Lt", "Regular");
        } else if (name.equals("amble light italic")) {
            font.impl_setNativeFont(font, font.getName(), "Amble Lt", "Italic");
        } else if (name.equals("amble light condensed")) {
            font.impl_setNativeFont(font, font.getName(), "Amble LtCn",
                    "Regular");
        } else if (name.equals("amble light condensed italic")) {
            font.impl_setNativeFont(font, font.getName(), "Amble LtCn",
                    "Italic");
        }
    }

    @Override
    public List<String> getFamilies() {
        return Arrays.asList("Amble", "Amble Cn", "Amble Lt", "Amble LtCn");
    }

    @Override
    public List<String> getFontNames() {
        return Arrays.asList("Amble Regular", "Amble Bold", "Amble Italic",
                "Amble Bold Italic", "Amble Condensed", "Amble Bold Condensed",
                "Amble Condensed Italic", "Amble Bold Condensed Italic",
                "Amble Light", "Amble Light Italic", "Amble Light Condensed",
                "Amble Light Condensed Italic");
    }

    @Override
    public List<String> getFontNames(String family) {
        String lower = family.trim().toLowerCase();
        if ("amble".equals(lower)) {
            return Arrays.asList("Amble Regular", "Amble Bold", "Amble Italic",
                    "Amble Bold Italic");
        } else if ("amble cn".equals(lower)) {
            return Arrays.asList("Amble Condensed", "Amble Bold Condensed",
                    "Amble Condensed Italic", "Amble Bold Condensed Italic");
        } else if ("amble lt".equals(lower)) {
            return Arrays.asList("Amble Light", "Amble Light Italic");
        } else if ("amble ltcn".equals(lower)) {
            return Arrays.asList("Amble Light Condensed",
                    "Amble Light Condensed Italic");
        } else {
            return Arrays.asList();
        }
    }

    @Override
    public Font font(String family, FontWeight weight, FontPosture posture,
            float size) {
        family = family.trim();
        String fam = family.toLowerCase();
        String name = "";
        if ("amble".equals(fam)) {
            if (weight != null
                    && weight.ordinal() < FontWeight.NORMAL.ordinal()) {
                name = name + " Light";
            } else if (weight != null
                    && weight.ordinal() > FontWeight.NORMAL.ordinal()) {
                name = name + " Bold";
            } else if (posture != FontPosture.ITALIC) {
                name = name + " Regular";
            }
        } else if ("amble cn".equals(fam) || "amble condensed".equals(fam)) {
            if (weight != null
                    && weight.ordinal() < FontWeight.NORMAL.ordinal()) {
                name = name + " Light";
            } else if (weight != null
                    && weight.ordinal() > FontWeight.NORMAL.ordinal()) {
                name = name + " Bold";
            }
            name = name + " Condensed";
        } else if ("amble lt".equals(fam)) {
            if (weight.ordinal() <= FontWeight.NORMAL.ordinal()) {
                name = name + " Light";
            } else if (weight != null
                    && weight.ordinal() < FontWeight.BOLD.ordinal()
                    && posture != FontPosture.ITALIC) {
                name = name + " Regular";
            } else if (weight != null
                    && weight.ordinal() >= FontWeight.BOLD.ordinal()) {
                name = name + " Bold";
            }
        } else if ("amble ltcn".equals(fam)) {
            if (weight.ordinal() <= FontWeight.NORMAL.ordinal()) {
                name = name + " Light";
            } else if (weight != null
                    && weight.ordinal() >= FontWeight.BOLD.ordinal()) {
                name = name + " Bold";
            }
            name = name + " Condensed";
        }
        if (posture == FontPosture.ITALIC) {
            name = name + " Italic";
        }
        String fn = "Amble" + name;
        return new Font(fn, size);
    }

    @Override
    public Font font(Object in, float size) {
        return new Font("not implemented", size);
    }

    @Override
    public Font loadFont(InputStream in, double size) {
        return new Font("not implemented", size);
    }    

    @Override
    public Font loadFont(String urlPath, double size) {
        return new Font("not implemented", size);
    }    

    @Override
    public FontMetrics getFontMetrics(Font font) {
        float size = (float) font.getSize();
        return new FontMetrics(size, size, size, size/2, size/2, size/5, font);
    }

    @Override
    public float computeStringWidth(String s, Font f) {
        // Assume that the font glyph size == font point size, mono-spaced
        // TODO needs to make sense given getFontMetrics implementation
        return (float) (f.getSize() * s.length());
    }

    @Override
    public float getSystemFontSize() {
        return 12;
    }
}
