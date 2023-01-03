/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.util;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;

/**************************************************************************
 *                                                                        *
 * Temporary state, used to reduce the occurrence of temporary garbage    *
 * while computing things such as bounds and transforms, and while        *
 * picking. Since these operations happen extremely often and must be     *
 * very fast, we need to reduce the load on the garbage collector.        *
 *                                                                        *
 *************************************************************************/
public final class TempState {
    /**
     * A temporary rect used for computing bounds by the various bounds
     * variables. This bounds starts life as a RectBounds, but may be promoted
     * to a BoxBounds if there is a 3D transform mixed into its computation.
     */
    public BaseBounds bounds = new RectBounds(0, 0, -1, -1);

    /**
     * A temporary affine transform used when picking to avoid creating
     * temporary garbage.
     */
    public final BaseTransform pickTx = new Affine3D();

    /**
     * A temporary affine transform used by the path transition to avoid
     * creating temporary garbage.
     */
    public final Affine3D leafTx = new Affine3D();

    /**
     * A temporary point used for picking and other purposes.
     */
    public final com.sun.javafx.geom.Point2D point =
        new com.sun.javafx.geom.Point2D(0, 0);

    public final com.sun.javafx.geom.Vec3d vec3d =
        new com.sun.javafx.geom.Vec3d(0, 0, 0);


    /**
     * A temporary general transform used by LOD helper method, in node,
     * to compute area in scene.
     */
    public final GeneralTransform3D projViewTx = new GeneralTransform3D();

   /**
     * A temporary affine transform used by the LOD helper method to get an
     * affine transform.
     */
    public final Affine3D tempTx = new Affine3D();

    private static final ThreadLocal<TempState> tempStateRef =
            new ThreadLocal<>() {
                @Override
                protected TempState initialValue() {
                    return new TempState();
                }
            };

    private TempState() {
    }

    public static TempState getInstance() {
        return tempStateRef.get();
    }
}
