/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;

public abstract class NGNodeShim extends NGNode {

    @Override
    public boolean hasOpaqueRegion() {
        return super.hasOpaqueRegion();

    }

    //--------------------------------------------

    public static void clearDirty(NGNode node) {
        node.clearDirty();
    }

    public static RectBounds computeOpaqueRegion(NGNode node, RectBounds opaqueRegion) {
        return node.computeOpaqueRegion(opaqueRegion);
    }

    public static int cullingBits(NGNode node) {
        return node.cullingBits;
    }

    public static boolean childDirty(NGNode node) {
        return node.childDirty;
    }

    public static NGNode.DirtyFlag dirty(NGNode node) {
        return node.dirty;
    }

    public static boolean isOpaqueRegionInvalid(NGNode node) {
        return node.isOpaqueRegionInvalid();
    }

    public static void markCullRegions(
            NGNode node,
            DirtyRegionContainer drc,
            int cullingRegionsBitsOfParent,
            BaseTransform tx,
            GeneralTransform3D pvTx) {
        node.markCullRegions(drc, cullingRegionsBitsOfParent, tx, pvTx);
    }

    public static void set_dirty(NGNode node, NGNode.DirtyFlag flag) {
        node.dirty = flag;
    }

    public static void set_childDirty(NGNode node, boolean flag) {
        node.childDirty = flag;
    }

    public static boolean supportsOpaqueRegions(NGNode node) {
        return node.supportsOpaqueRegions();
    }

    public static boolean hasOpaqueRegion(NGNode node) {
        return node.hasOpaqueRegion();
    }

}
