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

package com.sun.jfx.incubator.scene.control.richtext;

import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.util.Utils;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.model.EmbeddedImage;

/// Helper for the EmbeddedImage class.
public class EmbeddedImageHelper {

    public interface Accessor {

        public byte[] getBytes(EmbeddedImage im);

        public EmbeddedImage create(
            byte[] bytes,
            double width,
            double height,
            double targetWidth,
            double targetHeight,
            boolean keepAspectRatio
        );
    }

    static {
        Utils.forceInit(EmbeddedImage.class);
    }

    private static Accessor accessor;

    public static void setAccessor(Accessor a) {
        if (accessor != null) {
            throw new IllegalStateException();
        }
        accessor = a;
    }

    /// Returns the image bytes without making a defensive copy.
    public static byte[] getBytes(EmbeddedImage im) {
        return accessor.getBytes(im);
    }

    /// Creates an EmbeddedImage without making defensive copy of the bytes.
    public static EmbeddedImage create(
        byte[] bytes,
        double width,
        double height,
        double targetWidth,
        double targetHeight,
        boolean keepAspectRatio
    ) {
        return accessor.create(bytes, width, height, targetWidth, targetHeight, keepAspectRatio);
    }

    /// Returns visible TextCells.
    public static List<TextCell> getVisibleTextCells(RichTextArea r) {
        ArrayList<TextCell> rv = new ArrayList<>();
        VFlow f = RichTextAreaSkinHelper.getVFlow(r);
        if (f != null) {
            CellArrangement ar = f.arrangement();
            int ct = ar.getVisibleCellCount();
            for (int i = 0; i < ct; i++) {
                TextCell cell = ar.getCellAt(i);
                rv.add(cell);
            }
        }
        return rv;
    }
}
