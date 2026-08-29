/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

import java.text.DecimalFormat;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.stage.Screen;
import javafx.util.StringConverter;

/**
 * Various formatting methods.
 */
public class Formats {
    private static final DecimalFormat FORMAT_2DP = new DecimalFormat("0.##");
    private static StringConverter universalConverter;

    public static String formatDouble(Number value) {
        if (value == null) {
            return "null";
        }
        double v = value.doubleValue();
        if (v == Math.rint(v)) {
            return String.valueOf(value.longValue());
        }
        return String.valueOf(v);
    }

    public static String hit(HitInfo h) {
        StringBuilder sb = new StringBuilder(32);
        sb.append("ix=").append(h.getInsertionIndex());
        sb.append(" char=").append(h.getCharIndex());
        if (h.isLeading()) {
            sb.append(" leading");
        }
        return sb.toString();
    }

    public static String num2(double v) {
        return FORMAT_2DP.format(v);
    }

    public static String insets(Insets v) {
        if (v == null) {
            return "<null>";
        }
        if (
            (v.getTop() == v.getBottom()) &&
            (v.getBottom() == v.getLeft()) &&
            (v.getLeft() == v.getRight()))
        {
            return "[" + num2(v.getTop()) + "]";
        }
        return "[" +
            "t:" + num2(v.getTop()) +
            " r:" + num2(v.getRight()) +
            " b:" + num2(v.getBottom()) +
            " l:" + num2(v.getLeft()) +
            "]";
    }

    public static String screen(Screen s) {
        if (s == null) {
            return "<null>";
        }
        if (Screen.getPrimary().equals(s)) {
            return "Primary";
        }
        Rectangle2D r = s.getBounds();
        return
            "[" +
            Formats.formatDouble(r.getWidth()) +
            " x " +
            Formats.formatDouble(r.getHeight()) +
            "] @(" +
            Formats.formatDouble(r.getMinX()) +
            ", " +
            Formats.formatDouble(r.getMinY()) +
            ")";
    }

    public static String font(Font f) {
        if (f == null) {
            return null;
        }
        String fam = f.getFamily();
        String sty = f.getStyle();
        double sz = f.getSize();
        return fam + " " + sty + " " + num2(sz);
    }

    public static StringConverter universalConverter() {
        if (universalConverter == null) {
            // raw by design
            universalConverter = new StringConverter() {
                @Override
                public String toString(Object x) {
                    if (x == null) {
                        return "<null>";
                    } else if (x instanceof NamedValue v) {
                        return v.getDisplay();
                    } else if (x instanceof Insets m) {
                        return Formats.insets(m);
                    }
                    return x.toString();
                }

                @Override
                public Object fromString(String string) {
                    throw new UnsupportedOperationException();
                }
            };
        }
        return universalConverter;
    }
}
