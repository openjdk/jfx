/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Bounds;
import javafx.scene.Node;
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

    public static BaseBounds superComputeGeomBounds(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return ((TextHelper) getHelper(node)).superComputeGeomBoundsImpl(node, bounds, tx);
    }

    public static Bounds superComputeLayoutBounds(Node node) {
        return ((TextHelper) getHelper(node)).superComputeLayoutBoundsImpl(node);
    }

    public static float getVisualWidth(Text t) {
        return textAccessor.getVisualWidth(t);
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        return textAccessor.doCreatePeer(node);
    }

    @Override
    protected void updatePeerImpl(Node node) {
        super.updatePeerImpl(node);
        textAccessor.doUpdatePeer(node);
    }

    BaseBounds superComputeGeomBoundsImpl(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return super.computeGeomBoundsImpl(node, bounds, tx);
    }

    Bounds superComputeLayoutBoundsImpl(Node node) {
        return super.computeLayoutBoundsImpl(node);
    }

    @Override
    protected BaseBounds computeGeomBoundsImpl(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return textAccessor.doComputeGeomBounds(node, bounds, tx);
    }

    @Override
    protected Bounds computeLayoutBoundsImpl(Node node) {
        return textAccessor.doComputeLayoutBounds(node);
    }

    @Override
    protected boolean computeContainsImpl(Node node, double localX, double localY) {
        return textAccessor.doComputeContains(node, localX, localY);
    }

    @Override
    protected void geomChangedImpl(Node node) {
        super.geomChangedImpl(node);
        textAccessor.doGeomChanged(node);
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
        NGNode doCreatePeer(Node node);
        void doUpdatePeer(Node node);
        BaseBounds doComputeGeomBounds(Node node, BaseBounds bounds, BaseTransform tx);
        Bounds doComputeLayoutBounds(Node node);
        boolean doComputeContains(Node node, double localX, double localY);
        void doGeomChanged(Node node);
        com.sun.javafx.geom.Shape doConfigShape(Shape shape);
        public float getVisualWidth(Text t);
    }
}


