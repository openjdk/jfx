/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.rich;

import java.util.HashMap;
import javafx.incubator.scene.control.rich.model.StyleAttribute;
import javafx.incubator.scene.control.rich.model.StyleAttrs;

/**
 * Contains the utility methods which help dealing with StyleAttributes.
 */
public class StyleUtil {
    @FunctionalInterface
    private interface Generator<T> {
        /**
         * Creates a direct style string derived from the attribute value.
         * This method must return a valid CSS style followed by a semicolon, for example:
         * {@code "-fx-font-weight:bold;"}
         * This method must silently ignore any errors or values of incorrect type.
         *
         * @param a the parent StyleAttrs instance
         * @param value the attribute value
         * @return the direct style string
         */
        public String createCssStyle(T value);
    }

    private final static class Context {
        public StyleAttrs attrs;
        public boolean unwrapped;
    }

    private static final HashMap<StyleAttribute<?>, Generator> generators = initGenerators();
    private static final Context context = new Context();

    private static HashMap<StyleAttribute<?>, Generator> initGenerators() {
        HashMap<StyleAttribute<?>, Generator> m = new HashMap<>();

        put(m, StyleAttrs.BACKGROUND, (v) -> {
            String color = RichUtils.toCssColor(v);
            return "-fx-background-color:" + color + ";";
        });

        put(m, StyleAttrs.BOLD, (v) -> {
            return v ? "-fx-font-weight:bold;" : "-fx-font-weight:normal;";
        });

        put(m, StyleAttrs.FONT_FAMILY, (v) -> {
            return "-fx-font-family:'" + v + "';";
        });

        put(m, StyleAttrs.FONT_SIZE, (v) -> {
            return "-fx-font-size:" + v + "pt;";
        });

        put(m, StyleAttrs.ITALIC, (v) -> {
            return v ? "-fx-font-style:italic;" : "";
        });

        put(m, StyleAttrs.LINE_SPACING, (v) -> {
            return "-fx-line-spacing:" + v + ";";
        });

        put(m, StyleAttrs.STRIKE_THROUGH, (v) -> {
            return v ? "-fx-strikethrough:true;" : "";
        });

        put(m, StyleAttrs.TEXT_ALIGNMENT, (v) -> {
            if(context.unwrapped) {
                return "";
            }
            String alignment = RichUtils.toCss(v);
            return "-fx-text-alignment:" + alignment + ";";
        });

        put(m, StyleAttrs.TEXT_COLOR, (v) -> {
            String color = RichUtils.toCssColor(v);
            return "-fx-fill:" + color + ";";
        });

        put(m, StyleAttrs.UNDERLINE, (v) -> {
            return v ? "-fx-underline:true;" : "";
        });

        return m;
    }

    /**
     * Creates an fx style string from the given StyleAttrs, using either character or paragraph attributes.
     * @param attrs the style attributes
     * @param forParagraph when true, use the paragraph attributes, otherwise use character attributes
     * @param unwrapped true when style string is for paragraph with text wrap off
     * @return the style string
     */
    public static String getStyleString(StyleAttrs attrs, boolean forParagraph, boolean unwrapped) {
        if ((attrs == null) || attrs.isEmpty()) {
            return null;
        }

        // ok to access static context because this code is always executed in the FX thread
        context.attrs = attrs; // TODO may not be needed
        context.unwrapped = unwrapped;

        StringBuilder sb = null;
        for (StyleAttribute<?> a : attrs.getAttributes()) {
            if (a.isParagraphAttribute() == forParagraph) {
                String style = createStyle(attrs, a);
                if (style != null) {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    sb.append(style);
                }
            }
        }

        return sb == null ? null : sb.toString();
    }

    private static <T> String createStyle(StyleAttrs attrs, StyleAttribute<T> a) {
        Generator<T> g = (Generator<T>)generators.get(a);
        if (g != null) {
            T v = attrs.get(a);
            if (v != null) {
                return g.createCssStyle(v);
            }
        }
        return null;
    }

    /**
     * Generates fx style string applicable to TextCell.
     * @param a the style attributes
     * @return the style string
     */
    public static String generateTextCellStyle(StyleAttrs a) {
        if (
            a.contains(StyleAttrs.SPACE_ABOVE) ||
            a.contains(StyleAttrs.SPACE_RIGHT) ||
            a.contains(StyleAttrs.SPACE_BELOW) ||
            a.contains(StyleAttrs.SPACE_LEFT)) 
        {
            // TODO border attributes?
            double top = a.getDouble(StyleAttrs.SPACE_ABOVE, 0);
            double right = a.getDouble(StyleAttrs.SPACE_RIGHT, 0);
            double bottom = a.getDouble(StyleAttrs.SPACE_BELOW, 0);
            double left = a.getDouble(StyleAttrs.SPACE_LEFT, 0);

            return
                "-fx-padding:" + top + ' ' + right + ' ' + bottom + ' ' + left + ";";
        }
        return null;
    }

    // a tick to work around the generics
    private static <T> void put(HashMap<StyleAttribute<?>, Generator> m, StyleAttribute<T> a, Generator<T> g) {
        m.put(a, g);
    }
}
