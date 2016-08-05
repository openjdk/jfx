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

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.util.Utils;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.PathElement;

/**
 * Used to access internal methods of CubicCurveTo.
 */
public class CubicCurveToHelper extends PathElementHelper {

    private static final CubicCurveToHelper theInstance;
    private static CubicCurveToAccessor cubicCurveToAccessor;

    static {
        theInstance = new CubicCurveToHelper();
        Utils.forceInit(CubicCurveTo.class);
    }

    private static CubicCurveToHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(CubicCurveTo cubicCurveTo) {
        setHelper(cubicCurveTo, getInstance());
    }

    @Override
    protected  void addToImpl(PathElement pathElement, Path2D path) {
        cubicCurveToAccessor.doAddTo(pathElement, path);
    }

    public static void setCubicCurveToAccessor(final CubicCurveToAccessor newAccessor) {
        if (cubicCurveToAccessor != null) {
            throw new IllegalStateException();
        }

        cubicCurveToAccessor = newAccessor;
    }

    public interface CubicCurveToAccessor {
        void doAddTo(PathElement pathElement, Path2D path);
    }

}

