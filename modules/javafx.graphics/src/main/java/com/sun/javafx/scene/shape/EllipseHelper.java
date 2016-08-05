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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Shape;

/**
 * Used to access internal methods of Ellipse.
 */
public class EllipseHelper extends ShapeHelper {

    private static final EllipseHelper theInstance;
    private static EllipseAccessor ellipseAccessor;

    static {
        theInstance = new EllipseHelper();
        Utils.forceInit(Ellipse.class);
    }

    private static EllipseHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(Ellipse ellipse) {
        setHelper(ellipse, getInstance());
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        return ellipseAccessor.doCreatePeer(node);
    }

    @Override
    protected void updatePeerImpl(Node node) {
        super.updatePeerImpl(node);
        ellipseAccessor.doUpdatePeer(node);
    }

    @Override
    protected BaseBounds computeGeomBoundsImpl(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return ellipseAccessor.doComputeGeomBounds(node, bounds, tx);
    }

    @Override
    protected  com.sun.javafx.geom.Shape configShapeImpl(Shape shape) {
        return ellipseAccessor.doConfigShape(shape);
    }

    public static void setEllipseAccessor(final EllipseAccessor newAccessor) {
        if (ellipseAccessor != null) {
            throw new IllegalStateException();
        }

        ellipseAccessor = newAccessor;
    }

    public interface EllipseAccessor {
        NGNode doCreatePeer(Node node);
        void doUpdatePeer(Node node);
        BaseBounds doComputeGeomBounds(Node node, BaseBounds bounds, BaseTransform tx);
        com.sun.javafx.geom.Shape doConfigShape(Shape shape);
    }

}


