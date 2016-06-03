/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import javafx.scene.SubScene;

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
        return nodeAccessor.getHelper(node);
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

    /*
     * Methods that will be overridden by subclasses
     */

    protected abstract NGNode createPeerImpl(Node node);

    protected void markDirtyImpl(Node node, DirtyBits dirtyBit) {
        nodeAccessor.doMarkDirty(node, dirtyBit);
    }

    protected void updatePeerImpl(Node node) {
        nodeAccessor.doUpdatePeer(node);
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

    public static void layoutNodeForPrinting(Node node) {
        nodeAccessor.layoutNodeForPrinting(node);
    }

    public static boolean isDerivedDepthTest(Node node) {
        return nodeAccessor.isDerivedDepthTest(node);
    };

    public static SubScene getSubScene(Node node) {
        return nodeAccessor.getSubScene(node);
    };

    public static Accessible getAccessible(Node node) {
        return nodeAccessor.getAccessible(node);
    };

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
        boolean isDirty(Node node, DirtyBits dirtyBit);
        boolean isDirtyEmpty(Node node);
        void syncPeer(Node node);
        <P extends NGNode> P getPeer(Node node);
        void layoutNodeForPrinting(Node node);
        boolean isDerivedDepthTest(Node node);
        SubScene getSubScene(Node node);
        void setLabeledBy(Node node, Node labeledBy);
        Accessible getAccessible(Node node);
    }

}
