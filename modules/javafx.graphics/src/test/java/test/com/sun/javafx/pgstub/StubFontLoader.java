/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.pgstub;

import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.FontHelper;
import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.FontMetrics;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * Stub implementation of the {@link FontLoader} for testing purposes.
 * <br>
 * Can recognize and load some fonts we defined below, will otherwise fall back to the
 * System font (like in the real font loader).
 */
public class StubFontLoader extends FontLoader {

    @Override
    public void loadFont(Font font) {
        StubFont stub = new StubFont();
        stub.font = font;

        String name = font.getName();
        String nameLower = name.trim().toLowerCase(Locale.ROOT);
        switch (nameLower) {
            case "system regular" -> FontHelper.setNativeFont(font, stub, name, "System", "Regular");
            case "system bold" -> FontHelper.setNativeFont(font, stub, name, "System", "Bold");
            case "system italic" -> FontHelper.setNativeFont(font, stub, name, "System", "Italic");
            case "system bold italic" -> FontHelper.setNativeFont(font, stub, name, "System", "Bold Italic");
            case "amble regular" -> FontHelper.setNativeFont(font, stub, name, "Amble", "Regular");
            case "amble bold" -> FontHelper.setNativeFont(font, stub, name, "Amble", "Bold");
            case "amble italic" -> FontHelper.setNativeFont(font, stub, name, "Amble", "Italic");
            case "amble bold italic" -> FontHelper.setNativeFont(font, stub, name, "Amble", "Bold Italic");
            case "amble condensed" -> FontHelper.setNativeFont(font, stub, name, "Amble Cn", "Regular");
            case "amble bold condensed" -> FontHelper.setNativeFont(font, stub, name, "Amble Cn", "Bold");
            case "amble condensed italic" -> FontHelper.setNativeFont(font, stub, name, "Amble Cn", "Italic");
            case "amble bold condensed italic" -> FontHelper.setNativeFont(font, stub, name, "Amble Cn", "Bold Italic");
            case "amble light" -> FontHelper.setNativeFont(font, stub, name, "Amble Lt", "Regular");
            case "amble light italic" -> FontHelper.setNativeFont(font, stub, name, "Amble Lt", "Italic");
            case "amble light condensed" -> FontHelper.setNativeFont(font, stub, name, "Amble LtCn", "Regular");
            case "amble light condensed italic" -> FontHelper.setNativeFont(font, stub, name, "Amble LtCn", "Italic");
            default -> FontHelper.setNativeFont(font, stub, name, "System", "Regular");
        }
    }

    @Override
    public List<String> getFamilies() {
        return List.of("System", "Amble", "Amble Cn", "Amble Lt", "Amble LtCn");
    }

    @Override
    public List<String> getFontNames() {
        return List.of("System Regular", "System Bold", "System Italic", "System Bold Italic",
                "Amble Regular", "Amble Bold", "Amble Italic", "Amble Bold Italic",
                "Amble Condensed", "Amble Bold Condensed", "Amble Condensed Italic", "Amble Bold Condensed Italic",
                "Amble Light", "Amble Light Italic", "Amble Light Condensed", "Amble Light Condensed Italic");
    }

    @Override
    public List<String> getFontNames(String family) {
        String familyLower = family.trim().toLowerCase(Locale.ROOT);

        return switch (familyLower) {
            case "system" -> List.of("System Regular", "System Bold", "System Italic", "System Bold Italic");
            case "amble" -> List.of("Amble Regular", "Amble Bold", "Amble Italic", "Amble Bold Italic");
            case "amble cn" -> List.of("Amble Condensed", "Amble Bold Condensed", "Amble Condensed Italic", "Amble Bold Condensed Italic");
            case "amble lt" -> List.of("Amble Light", "Amble Light Italic");
            case "amble ltcn" -> List.of("Amble Light Condensed", "Amble Light Condensed Italic");
            default -> List.of();
        };
    }

    @Override
    public Font font(String family, FontWeight weight, FontPosture posture,
            float size) {
        String fam = family.trim().toLowerCase(Locale.ROOT);
        String name = "";
        if (fam.startsWith("system")) {
            name = "System";
        } else if (fam.startsWith("amble")) {
            name = "Amble";
        }

        switch (fam) {
            case "amble", "system" -> {
                if (weight != null
                        && weight.ordinal() < FontWeight.NORMAL.ordinal()) {
                    name = name + " Light";
                } else if (weight != null
                        && weight.ordinal() > FontWeight.NORMAL.ordinal()) {
                    name = name + " Bold";
                } else if (posture != FontPosture.ITALIC) {
                    name = name + " Regular";
                }
            }
            case "amble cn", "amble condensed" -> {
                if (weight != null
                        && weight.ordinal() < FontWeight.NORMAL.ordinal()) {
                    name = name + " Light";
                } else if (weight != null
                        && weight.ordinal() > FontWeight.NORMAL.ordinal()) {
                    name = name + " Bold";
                }
                name = name + " Condensed";
            }
            case "amble lt" -> {
                if (weight != null
                        && weight.ordinal() <= FontWeight.NORMAL.ordinal()) {
                    name = name + " Light";
                } else if (weight != null
                        && weight.ordinal() < FontWeight.BOLD.ordinal()
                        && posture != FontPosture.ITALIC) {
                    name = name + " Regular";
                } else if (weight != null
                        && weight.ordinal() >= FontWeight.BOLD.ordinal()) {
                    name = name + " Bold";
                }
            }
            case "amble ltcn" -> {
                if (weight != null
                        && weight.ordinal() <= FontWeight.NORMAL.ordinal()) {
                    name = name + " Light";
                } else if (weight != null
                        && weight.ordinal() >= FontWeight.BOLD.ordinal()) {
                    name = name + " Bold";
                }
                name = name + " Condensed";
            }
        }

        if (posture == FontPosture.ITALIC) {
            name = name + " Italic";
        }

        return new Font(name, size);
    }

    @Override
    public Font[] loadFont(InputStream in, double size, boolean all) {
        Font[] fonts = new Font[1];
        fonts[0] = new Font("not implemented", size);
        return fonts;
    }

    @Override
    public Font[] loadFont(String urlPath, double size, boolean all) {
        Font[] fonts = new Font[1];
        fonts[0] = new Font("not implemented", size);
        return fonts;
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        float size = (float) font.getSize();
        return new FontMetrics(size, size, size, size/2, size/2, size/5, font);
    }

    @Override
    public float getCharWidth(char ch, Font f) {
        // Assume that the font glyph size == font point size, mono-spaced
        // TODO needs to make sense given getFontMetrics implementation
        return (float) (f.getSize());
    }

    @Override
    public float getSystemFontSize() {
        return 12;
    }

    public static class StubFont implements PGFont {
        public Font font;

        @Override public String getFullName() {
            return font.getName();
        }

        @Override public String getFamilyName() {
            return font.getFamily();
        }

        @Override public String getStyleName() {
            return font.getStyle();
        }

        @Override public String getName() {
            return font.getName();
        }

        @Override public float getSize() {
            return (float) font.getSize();
        }

        @Override
        public FontResource getFontResource() {
            return null;
        }

        @Override
        public FontStrike getStrike(BaseTransform transform) {
            return null;
        }

        @Override
        public FontStrike getStrike(BaseTransform transform, int smoothingType) {
            return null;
        }

        @Override
        public int getFeatures() {
            return 0;
        }
    }
}
