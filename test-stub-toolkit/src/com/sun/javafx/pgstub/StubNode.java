/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.pgstub;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.PGNode;
import com.sun.scenario.effect.Blend;

public class StubNode implements PGNode {
    boolean visible;
    BaseTransform tx;

    private RectBounds invalidBounds(float x1, float y1, float x2, float y2) {
        // Create the RectBounds this way because its no-arg constructor
        // sets the invalid flag to true.
        RectBounds b = new RectBounds();
        b.setMinX(x1);
        b.setMinY(y1);
        b.setMaxX(x2);
        b.setMaxY(y2);
        return b;
    }

    @Override public void setTransformMatrix(BaseTransform tx) {
        this.tx = tx;
    }

    @Override
    public void setContentBounds(BaseBounds bounds) {
    }

    @Override
    public void setTransformedBounds(BaseBounds bounds) {
    }

    @Override public void setVisible(boolean visible) {
        this.visible = visible;
    }
    @Override public void setOpacity(float opacity) {
        // ignore
    }

    @Override public void setNodeBlendMode(Blend.Mode blendMode) {
        // ignore
    }

    @Override public void setDepthTest(boolean depthTest) {
        // ignore
    }

    @Override public void setClipNode(PGNode clipNode) {
        // ignore
    }
    @Override public void setCachedAsBitmap(boolean cached, CacheHint cacheHint) {
        // ignore
    }
    // This is currently for desktop profile only (can be ignored by
    // other profiles).
    @Override public void setEffect(Object effect) {
        // ignore
    }

    // for testing only
    public boolean isVisible() {
        return visible;
    }

    private StubNode parent;

    public final StubNode getParent() {
        return parent;
    }

    final void setParent(StubNode parent) {
        this.parent = parent;
    }

    public final BaseTransform getTransformMatrix() {
        return tx;
    }

    @Override
    public void effectChanged() {
        // ignore
    }
}
