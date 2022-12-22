/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;

/**
 * Used to access internal methods of MeshView.
 */
public class MeshViewHelper extends Shape3DHelper {

    private static final MeshViewHelper theInstance;
    private static MeshViewAccessor meshViewAccessor;

    static {
        theInstance = new MeshViewHelper();
        Utils.forceInit(MeshView.class);
    }

    private static MeshViewHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(MeshView meshView) {
        setHelper(meshView, getInstance());
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        return meshViewAccessor.doCreatePeer(node);
    }

    @Override
    protected void updatePeerImpl(Node node) {
        super.updatePeerImpl(node);
        meshViewAccessor.doUpdatePeer(node);
    }

    @Override
    protected BaseBounds computeGeomBoundsImpl(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return meshViewAccessor.doComputeGeomBounds(node, bounds, tx);
    }

    @Override
    protected boolean computeContainsImpl(Node node, double localX, double localY) {
        return meshViewAccessor.doComputeContains(node, localX, localY);
    }

    @Override
    protected boolean computeIntersectsImpl(Node node, PickRay pickRay,
            PickResultChooser pickResult) {
        return meshViewAccessor.doComputeIntersects(node, pickRay, pickResult);
    }

    public static void setMeshViewAccessor(final MeshViewAccessor newAccessor) {
        if (meshViewAccessor != null) {
            throw new IllegalStateException();
        }

        meshViewAccessor = newAccessor;
    }

    public interface MeshViewAccessor {
        NGNode doCreatePeer(Node node);
        void doUpdatePeer(Node node);
        BaseBounds doComputeGeomBounds(Node node, BaseBounds bounds, BaseTransform tx);
        boolean doComputeContains(Node node, double localX, double localY);
        boolean doComputeIntersects(Node node, PickRay pickRay,
                PickResultChooser pickResult);
    }

}
