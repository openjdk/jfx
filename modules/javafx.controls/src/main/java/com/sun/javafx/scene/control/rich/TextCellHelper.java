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

import javafx.scene.control.rich.TextCell;
import javafx.scene.shape.PathElement;
import com.sun.javafx.util.Utils;

/**
 * Allows to hide TextCell methods used internally.
 */
public class TextCellHelper {
    public interface Accessor {
        public void addBoxOutline(TextCell cell, FxPathBuilder b, double x, double w, double h);
        public PathElement[] getCaretShape(TextCell cell, int cix, boolean leading);
        public PathElement[] getRangeShape(TextCell cell, int start, int end);
        public double getHeight(TextCell cell);
        public double getY(TextCell cell);
        public void setPosition(TextCell cell, double y, double h);
    }

    static {
        Utils.forceInit(TextCell.class);
    }

    private static Accessor accessor;

    public static void setAccessor(Accessor a) {
        if (accessor != null) {
            throw new IllegalStateException();
        }
        accessor = a;
    }

    public static void addBoxOutline(TextCell cell, FxPathBuilder b, double x, double w, double h) {
        accessor.addBoxOutline(cell, b, x, w, h);
    }

    public static PathElement[] getCaretShape(TextCell cell, int cix, boolean leading) {
        return accessor.getCaretShape(cell, cix, leading);
    }

    public static PathElement[] getRangeShape(TextCell cell, int start, int end) {
        return accessor.getRangeShape(cell, start, end);
    }

    public static double getHeight(TextCell cell) {
        return accessor.getHeight(cell);
    }

    public static double getY(TextCell cell) {
        return accessor.getY(cell);
    }

    public static void setPosition(TextCell cell, double y, double h) {
        accessor.setPosition(cell, y, h);
    }
}
