/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene;

import com.sun.glass.ui.Accessible;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import java.util.List;
import java.util.Map;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.css.CssMetaData;
import javafx.css.Style;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Shape3D;

/**
 * Used to access internal methods of Node.
 */
public abstract class NodeHelper {
    private static NodeAccessor nodeAccessor;

    static {
        Utils.forceInit(Node.class);
    }

    protected NodeHelper() {
    }

    protected static NodeHelper getHelper(Node node) {

        NodeHelper helper = nodeAccessor.getHelper(node);
        if (helper == null) {
            String nodeType;
            if (node instanceof Shape) {
                nodeType = "Shape";
            } else if (node instanceof Shape3D) {
                nodeType = "Shape3D";
            } else {
                nodeType = "Node";
            }

            throw new UnsupportedOperationException(
                    "Applications should not extend the "
                    + nodeType + " class directly.");
        }
        return helper;
    }

    protected static void setHelper(Node node, NodeHelper nodeHelper) {
        nodeAccessor.setHelper(node, nodeHelper);
    }

    /*
     * Static helper methods for cases where the implementation is done in an
     * instance method that is overridden by subclasses.
     * These methods exist in the base class only.
     */

    public static NGNode createPeer(Node node) {
        return getHelper(node).createPeerImpl(node);
    }

    public static void markDirty(Node node, DirtyBits dirtyBit) {
        getHelper(node).markDirtyImpl(node, dirtyBit);
    }

    public static void updatePeer(Node node) {
        getHelper(node).updatePeerImpl(node);
    }

    public static Bounds computeLayoutBounds(Node node) {
        return getHelper(node).computeLayoutBoundsImpl(node);
    }

    /*
     * Computes the geometric bounds for this Node. This method is abstract
     * and must be implemented by each Node subclass.
     */
    public static BaseBounds computeGeomBounds(Node node,
            BaseBounds bounds, BaseTransform tx) {
        return getHelper(node).computeGeomBoundsImpl(node, bounds, tx);
    }

    public static void transformsChanged(Node node) {
        getHelper(node).transformsChangedImpl(node);
    }

    public static boolean computeContains(Node node, double localX, double localY) {
        return getHelper(node).computeContainsImpl(node, localX, localY);
    }

    public static void pickNodeLocal(Node node, PickRay localPickRay,
            PickResultChooser result) {
        getHelper(node).pickNodeLocalImpl(node, localPickRay, result);
    }

    public static boolean computeIntersects(Node node, PickRay pickRay,
            PickResultChooser pickResult) {
        return getHelper(node).computeIntersectsImpl(node, pickRay, pickResult);
    }

    public static void geomChanged(Node node) {
        getHelper(node).geomChangedImpl(node);
    }

    public static void notifyLayoutBoundsChanged(Node node) {
        getHelper(node).notifyLayoutBoundsChangedImpl(node);
    }

    public static void processCSS(Node node) {
        getHelper(node).processCSSImpl(node);
    }

    /*
     * Methods that will be overridden by subclasses
     */

    protected abstract NGNode createPeerImpl(Node node);
    protected abstract boolean computeContainsImpl(Node node, double localX, double localY);
    protected abstract BaseBounds computeGeomBoundsImpl(Node node,
            BaseBounds bounds, BaseTransform tx);

    protected void markDirtyImpl(Node node, DirtyBits dirtyBit) {
        nodeAccessor.doMarkDirty(node, dirtyBit);
    }

    protected void updatePeerImpl(Node node) {
        nodeAccessor.doUpdatePeer(node);
    }

    protected Bounds computeLayoutBoundsImpl(Node node) {
        return nodeAccessor.doComputeLayoutBounds(node);
    }

    protected void transformsChangedImpl(Node node) {
        nodeAccessor.doTransformsChanged(node);
    }

    protected void pickNodeLocalImpl(Node node, PickRay localPickRay,
            PickResultChooser result) {
        nodeAccessor.doPickNodeLocal(node, localPickRay, result);
    }

    protected boolean computeIntersectsImpl(Node node, PickRay pickRay,
            PickResultChooser pickResult) {
        return nodeAccessor.doComputeIntersects(node, pickRay, pickResult);
    }

    protected void geomChangedImpl(Node node) {
        nodeAccessor.doGeomChanged(node);
    }

    protected void notifyLayoutBoundsChangedImpl(Node node) {
        nodeAccessor.doNotifyLayoutBoundsChanged(node);
    }

    protected void processCSSImpl(Node node) {
        nodeAccessor.doProcessCSS(node);
    }

    /*
     * Methods used by Node (base) class only
     */

    public static boolean isDirty(Node node, DirtyBits dirtyBit) {
        return nodeAccessor.isDirty(node, dirtyBit);
    }

    public static boolean isDirtyEmpty(Node node) {
        return nodeAccessor.isDirtyEmpty(node);
    }

    public static void syncPeer(Node node) {
        nodeAccessor.syncPeer(node);
    }

    public static <P extends NGNode> P getPeer(Node node) {
        return nodeAccessor.getPeer(node);
    }

    public static BaseTransform getLeafTransform(Node node) {
        return nodeAccessor.getLeafTransform(node);
    }

    public static void layoutBoundsChanged(Node node) {
        nodeAccessor.layoutBoundsChanged(node);
    }

    public static void setShowMnemonics(Node node, boolean value) {
        nodeAccessor.setShowMnemonics(node, value);
    }

    public static boolean isShowMnemonics(Node node) {
        return nodeAccessor.isShowMnemonics(node);
    }

    public static BooleanProperty showMnemonicsProperty(Node node) {
        return nodeAccessor.showMnemonicsProperty(node);
    }

    public static boolean traverse(Node node, Direction direction) {
        return nodeAccessor.traverse(node, direction);
    }

    public static double getPivotX(Node node) {
        return nodeAccessor.getPivotX(node);
    }

    public static double getPivotY(Node node) {
        return nodeAccessor.getPivotY(node);
    }

    public static double getPivotZ(Node node) {
        return nodeAccessor.getPivotZ(node);
    }

    public static void pickNode(Node node, PickRay pickRay,
            PickResultChooser result) {
        nodeAccessor.pickNode(node, pickRay, result);
    }

    public static boolean intersects(Node node, PickRay pickRay,
            PickResultChooser pickResult) {
        return nodeAccessor.intersects(node, pickRay, pickResult);
    }

    public static double intersectsBounds(Node node, PickRay pickRay) {
        return nodeAccessor.intersectsBounds(node, pickRay);
    }

    public static void layoutNodeForPrinting(Node node) {
        nodeAccessor.layoutNodeForPrinting(node);
    }

    public static boolean isDerivedDepthTest(Node node) {
        return nodeAccessor.isDerivedDepthTest(node);
    }

    public static SubScene getSubScene(Node node) {
        return nodeAccessor.getSubScene(node);
    }

    public static Accessible getAccessible(Node node) {
        return nodeAccessor.getAccessible(node);
    }

    public static void reapplyCSS(Node node) {
        nodeAccessor.reapplyCSS(node);
    }

    public static boolean isTreeVisible(Node node) {
        return nodeAccessor.isTreeVisible(node);
    }

    public static BooleanExpression treeVisibleProperty(Node node) {
        return nodeAccessor.treeVisibleProperty(node);
    }

    public static boolean isTreeShowing(Node node) {
        return nodeAccessor.isTreeShowing(node);
    }

    public static BooleanExpression treeShowingProperty(Node node) {
        return nodeAccessor.treeShowingProperty(node);
    }

    public static List<Style> getMatchingStyles(CssMetaData cssMetaData, Styleable styleable) {
        return nodeAccessor.getMatchingStyles(cssMetaData, styleable);
    }

    public static Map<StyleableProperty<?>,List<Style>> findStyles(Node node, Map<StyleableProperty<?>,List<Style>> styleMap) {
        return nodeAccessor.findStyles(node, styleMap);
    }

    public static void setNodeAccessor(final NodeAccessor newAccessor) {
        if (nodeAccessor != null) {
            throw new IllegalStateException();
        }

        nodeAccessor = newAccessor;
    }

    public static NodeAccessor getNodeAccessor() {
        if (nodeAccessor == null) {
            throw new IllegalStateException();
        }

        return nodeAccessor;
    }

    public interface NodeAccessor {
        NodeHelper getHelper(Node node);
        void setHelper(Node node, NodeHelper nodeHelper);
        void doMarkDirty(Node node, DirtyBits dirtyBit);
        void doUpdatePeer(Node node);
        BaseTransform getLeafTransform(Node node);
        Bounds doComputeLayoutBounds(Node node);
        void doTransformsChanged(Node node);
        void doPickNodeLocal(Node node, PickRay localPickRay,
                PickResultChooser result);
        boolean doComputeIntersects(Node node, PickRay pickRay,
                PickResultChooser pickResult);
        void doGeomChanged(Node node);
        void doNotifyLayoutBoundsChanged(Node node);
        void doProcessCSS(Node node);
        boolean isDirty(Node node, DirtyBits dirtyBit);
        boolean isDirtyEmpty(Node node);
        void syncPeer(Node node);
        <P extends NGNode> P getPeer(Node node);
        void layoutBoundsChanged(Node node);
        void setShowMnemonics(Node node, boolean value);
        boolean isShowMnemonics(Node node);
        BooleanProperty showMnemonicsProperty(Node node);
        boolean traverse(Node node, Direction direction);
        double getPivotX(Node node);
        double getPivotY(Node node);
        double getPivotZ(Node node);
        void pickNode(Node node, PickRay pickRay, PickResultChooser result);
        boolean intersects(Node node, PickRay pickRay, PickResultChooser pickResult);
        double intersectsBounds(Node node, PickRay pickRay);
        void layoutNodeForPrinting(Node node);
        boolean isDerivedDepthTest(Node node);
        SubScene getSubScene(Node node);
        void setLabeledBy(Node node, Node labeledBy);
        Accessible getAccessible(Node node);
        void reapplyCSS(Node node);
        boolean isTreeVisible(Node node);
        BooleanExpression treeVisibleProperty(Node node);
        boolean isTreeShowing(Node node);
        BooleanExpression treeShowingProperty(Node node);
        List<Style> getMatchingStyles(CssMetaData cssMetaData, Styleable styleable);
        Map<StyleableProperty<?>,List<Style>> findStyles(Node node,
                Map<StyleableProperty<?>,List<Style>> styleMap);
    }

}
