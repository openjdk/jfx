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

import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.Shape;

/**
 * Used to access internal methods of QuadCurve.
 */
public class QuadCurveHelper extends ShapeHelper {

    private static final QuadCurveHelper theInstance;
    private static QuadCurveAccessor quadCurveAccessor;

    static {
        theInstance = new QuadCurveHelper();
        Utils.forceInit(QuadCurve.class);
    }

    private static QuadCurveHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(QuadCurve quadCurve) {
        setHelper(quadCurve, getInstance());
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        return quadCurveAccessor.doCreatePeer(node);
    }

    @Override
    protected void updatePeerImpl(Node node) {
        super.updatePeerImpl(node);
        quadCurveAccessor.doUpdatePeer(node);
    }

    @Override
    protected com.sun.javafx.geom.Shape configShapeImpl(Shape shape) {
        return quadCurveAccessor.doConfigShape(shape);
    }

    public static void setQuadCurveAccessor(final QuadCurveAccessor newAccessor) {
        if (quadCurveAccessor != null) {
            throw new IllegalStateException();
        }

        quadCurveAccessor = newAccessor;
    }

    public interface QuadCurveAccessor {
        NGNode doCreatePeer(Node node);
        void doUpdatePeer(Node node);
        com.sun.javafx.geom.Shape doConfigShape(Shape shape);
    }

}

