/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.prism.NGNode;

import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.scene.transform.Transform;

public class NodeShim {

    public static boolean boundsChanged(Node n) {
        return n.boundsChanged;
    }

    public static Node getClipParent(Node n) {
        return n.getClipParent();
    }

    public static Transform getCurrentLocalToSceneTransformState(Node n) {
        return n.getCurrentLocalToSceneTransformState();
    }

    public static SubScene getSubScene(Node n) {
        return n.getSubScene();
    }

    public static boolean hasMirroring(Node n) {
        return n.hasMirroring();
    }

    public static void clearDirty(Node n, DirtyBits dirtyBit) {
        n.clearDirty(dirtyBit);
    }

    public static boolean isDirty(Node n, DirtyBits dirtyBit) {
        return n.isDirty(dirtyBit);
    }

    public static boolean isDerivedDepthTest(Node n) {
        return n.isDerivedDepthTest();
    }

    public static void set_boundsChanged(Node n, boolean b) {
        n.boundsChanged = b;
    }

    public static void updateBounds(Node n) {
        n.updateBounds();
    }

    public static <P extends NGNode> P getPeer(Node n) {
        return n.getPeer();
    }

    public static ObservableSet<PseudoClass> pseudoClassStates(Node n) {
        return n.pseudoClassStates;
    }
}
