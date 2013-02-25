/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.camera;

import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.GeneralTransform3D;

public class PrismParallelCameraImpl extends PrismCameraImpl {


    private static final PrismParallelCameraImpl theInstance = new PrismParallelCameraImpl();

    public static PrismParallelCameraImpl getInstance() {
        return theInstance;
    }

    /**
     * Constructs a orthographic camera object with default parameters.
     */
    private PrismParallelCameraImpl() {}

    @Override
    protected void computeProjection(GeneralTransform3D proj) {
        double width = viewport.width;
        double height = viewport.height;
        double halfDepth = (width > height) ? width / 2.0 : height / 2.0;
        proj.ortho(0.0, width, height, 0.0, -halfDepth, halfDepth);
    }

    @Override
    protected void computeViewTransform(Affine3D view) {
        view.setToTranslation(0.0, 0.0, 0.0);
    }

    @Override
    public PickRay computePickRay(float x, float y, PickRay pickRay) {
        if (pickRay == null) {
            pickRay = new PickRay();
        }
        pickRay.getOriginNoClone()   .set(x, y, 0);
        pickRay.getDirectionNoClone().set(0, 0, 1);
        return pickRay;
    }
}
