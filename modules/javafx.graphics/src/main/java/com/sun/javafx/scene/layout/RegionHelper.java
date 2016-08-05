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

package com.sun.javafx.scene.layout;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.ParentHelper;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * Used to access internal methods of Region.
 */
public class RegionHelper extends ParentHelper {

    private static final RegionHelper theInstance;
    private static RegionAccessor regionAccessor;

    static {
        theInstance = new RegionHelper();
        Utils.forceInit(Region.class);
    }

    private static RegionHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(Region region) {
        setHelper(region, getInstance());
    }

    public static BaseBounds superComputeGeomBounds(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return ((RegionHelper) getHelper(node)).superComputeGeomBoundsImpl(node, bounds, tx);
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        return regionAccessor.doCreatePeer(node);
    }

    @Override
    protected void updatePeerImpl(Node node) {
        super.updatePeerImpl(node);
        regionAccessor.doUpdatePeer(node);
    }

   BaseBounds superComputeGeomBoundsImpl(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return super.computeGeomBoundsImpl(node, bounds, tx);
   }

   @Override
    protected Bounds computeLayoutBoundsImpl(Node node) {
        return regionAccessor.doComputeLayoutBounds(node);
    }

    @Override
    protected BaseBounds computeGeomBoundsImpl(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return regionAccessor.doComputeGeomBounds(node, bounds, tx);
    }

    @Override
    protected boolean computeContainsImpl(Node node, double localX, double localY) {
        return regionAccessor.doComputeContains(node, localX, localY);
    }

    @Override
    protected void notifyLayoutBoundsChangedImpl(Node node) {
        regionAccessor.doNotifyLayoutBoundsChanged(node);
    }
    @Override
    protected void pickNodeLocalImpl(Node node, PickRay localPickRay,
            PickResultChooser result) {
        regionAccessor.doPickNodeLocal(node, localPickRay, result);
    }

    public static void setRegionAccessor(final RegionAccessor newAccessor) {
        if (regionAccessor != null) {
            throw new IllegalStateException();
        }

        regionAccessor = newAccessor;
    }

    public interface RegionAccessor {
        void doUpdatePeer(Node node);
        NGNode doCreatePeer(Node node);
        Bounds doComputeLayoutBounds(Node node);
        BaseBounds doComputeGeomBounds(Node node, BaseBounds bounds, BaseTransform tx);
        boolean doComputeContains(Node node, double localX, double localY);
        void doNotifyLayoutBoundsChanged(Node node);
        void doPickNodeLocal(Node node, PickRay localPickRay,
                PickResultChooser result);
    }

}
