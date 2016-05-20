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
import javafx.scene.shape.PathElement;

/**
 * Used to access internal methods of PathElement.
 */
public abstract class PathElementHelper {
    private static PathElementAccessor pathElementAccessor;

    static {
        Utils.forceInit(PathElement.class);
    }

    protected PathElementHelper() {
    }

    private static PathElementHelper getHelper(PathElement pathElement) {
        return pathElementAccessor.getHelper(pathElement);
    }

    protected static void setHelper(PathElement pathElement, PathElementHelper pathElementHelper) {
        pathElementAccessor.setHelper(pathElement, pathElementHelper);
    }

    /*
     * Static helper methods for cases where the implementation is done in an
     * instance method that is overridden by subclasses.
     * These methods exist in the base class only.
     */

    public static void addTo(PathElement pathElement, Path2D path) {
        getHelper(pathElement).addToImpl(pathElement, path);
    }

    /*
     * Methods that will be overridden by subclasses
     */

    protected abstract void addToImpl(PathElement pathElement, Path2D path);

    /*
     * Methods used by PathElement (base) class only
     */

    public static void setPathElementAccessor(final PathElementAccessor newAccessor) {
        if (pathElementAccessor != null) {
            throw new IllegalStateException();
        }

        pathElementAccessor = newAccessor;
    }

    public interface PathElementAccessor {
        PathElementHelper getHelper(PathElement pathElement);
        void setHelper(PathElement pathElement, PathElementHelper pathElementHelper);
    }

}

