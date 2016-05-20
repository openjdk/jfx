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

package com.sun.javafx.scene.shape;

import com.sun.javafx.util.Utils;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

/**
 * Used to access internal methods of Text.
 */
public class TextHelper extends ShapeHelper {

    private static final TextHelper theInstance;
    private static TextAccessor textAccessor;

    static {
        theInstance = new TextHelper();
        Utils.forceInit(Text.class);
    }

    private static TextHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(Text text) {
        setHelper(text, getInstance());
    }

    @Override
    protected  com.sun.javafx.geom.Shape configShapeImpl(Shape shape) {
        return textAccessor.doConfigShape(shape);
    }

    public static void setTextAccessor(final TextAccessor newAccessor) {
        if (textAccessor != null) {
            throw new IllegalStateException();
        }

        textAccessor = newAccessor;
    }

    public interface TextAccessor {
        com.sun.javafx.geom.Shape doConfigShape(Shape shape);
    }

}


