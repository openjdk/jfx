/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control.rich.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.geometry.Point2D;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;

/**
 * Utility methods to be moved to com.sun.javafx hierarchy.
 */
public class Util {
    private static final boolean isMac = initIsMac();
    private static final boolean isWin = initIsWin();

    /**
     * Combines CssMetaData items in one unmodifiable list with the size equal to the number
     * of items it holds (i.e. with no unnecessary overhead).
     * 
     * @param list css metadata items, usually from the parent
     * @param items additional items
     * @return unmodifiable list containing all the items
     */
    public static List<CssMetaData<? extends Styleable, ?>> initStyleables(
            List<CssMetaData<? extends Styleable, ?>> list,
            CssMetaData<? extends Styleable, ?>... items) {

        int sz = list.size() + items.length;
        ArrayList<CssMetaData<? extends Styleable, ?>> rv = new ArrayList<>(sz);
        rv.addAll(list);
        for (CssMetaData<? extends Styleable, ?> p : items) {
            rv.add(p);
        }
        return Collections.unmodifiableList(rv);
    }

    public static String getResourceURL(Class<?> c, String name) {
        String pkg = c.getPackage().getName().replace(".", "/");
        if (pkg.length() != 0) {
            name = "/" + pkg + "/" + name;
        }
        return c.getResource(name).toExternalForm();
    }

    public static <T> T notNull(T item, String name) {
        if(item == null) {
            throw new IllegalArgumentException(name + " must not be null.");
        }
        return item;
    }
    
    public static <T extends Comparable<T>> void isLessThanOrEqual(T min, T max, String nameMin, String nameMax) {
        if(min.compareTo(max) > 0) {
            throw new IllegalArgumentException(nameMin + " must be less or equal to " + nameMax);
        }
    }

    public static double halfPixel(double coord) {
        return Math.round(coord + 0.5) - 0.5;
    }

    public static PathElement[] translatePath(double xoffset, Region target, Region src, PathElement[] elements) {
        Point2D p = src.localToScreen(src.snappedLeftInset(), src.snappedTopInset());
        if (p == null) {
            return null;
        }

        p = target.screenToLocal(p);
        double dx = p.getX() + xoffset;
        double dy = p.getY();

        for (int i = 0; i < elements.length; i++) {
            PathElement em = elements[i];
            if (em instanceof LineTo m) {
                em = new LineTo(m.getX() + dx, m.getY() + dy);
            } else if (em instanceof MoveTo m) {
                em = new MoveTo(m.getX() + dx, m.getY() + dy);
            } else {
                throw new RuntimeException("unexpected path element " + em);
            }

            elements[i] = em;
        }
        return elements;
    }
    
    private static boolean initIsMac() {
        // PlatformUtil
        return System.getProperty("os.name").startsWith("Mac");
    }

    private static boolean initIsWin() {
        // PlatformUtil
        return System.getProperty("os.name").startsWith("Windows");
    }
    
    public static boolean isMac() {
        return isMac;
    }

    public static boolean isWindows() {
        return isWin;
    }
    
    /**
     * Simple utility function which clamps the given value to be strictly
     * between the min and max values.
     */
    // see com.sun.javafx.util.Utils:77
    public static int clamp(int min, int value, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * A safe substring method which is tolerant to null text, and offsets being outside of the text boundaries.
     * 
     * @param text source text or null
     * @param start start offset, must be >= 0
     * @param end end offset
     * @return a non-null substring
     */
    public static String substring(String text, int start, int end) {
        if (text == null) {
            return "";
        }

        int len = text.length();
        if((end < 0) || (end > len)) {
            end = len;
        }

        if ((start == 0) && (end == len)) {
            return text;
        }

        return text.substring(start, end);
    }
    
    /** Converts Color to "#rrggbb" or "rgba(r,g,b,a)" string */ 
    public static String toCssColor(Color c) {
        if(c.getOpacity() == 1.0) {
            return String.format(
                "#%02x%02x%02x", 
                eightBit(c.getRed()),
                eightBit(c.getGreen()),
                eightBit(c.getBlue())
            );
        } else {
            return String.format(
                "rgba(%d,%d,%d,%d)", 
                eightBit(c.getRed()),
                eightBit(c.getGreen()),
                eightBit(c.getBlue()),
                c.getOpacity()
            );
        }
    }

    private static int eightBit(double val) {
        int v = (int)Math.round(val * 255);
        if (v < 0) {
            return 0;
        } else if (v > 255) {
            return 255;
        }
        return v;
    }

    /** null-tolerant !equals() */
    public static boolean notEquals(Object a, Object b) {
        return !equals(a, b);
    }

    /** null-tolerant equals() */
    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null) {
            return (b == null);
        } else if (b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }
}
