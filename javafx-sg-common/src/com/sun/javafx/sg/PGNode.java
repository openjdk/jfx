/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.sg;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Blend;

/**
 *
 */
public interface PGNode {
    public void setTransformMatrix(BaseTransform tx);
    public void setContentBounds(BaseBounds bounds);
    public void setTransformedBounds(BaseBounds bounds, boolean byTransformChangeOnly);
    public void setVisible(boolean visible);
    public void setOpacity(float opacity);
    public void setNodeBlendMode(Blend.Mode blendMode);
    public void setDepthTest(boolean depthTest);
    public void setClipNode(PGNode clipNode);

    /**
     * com.sun.javafx.sg package version of javafx.scene.CacheHint
     * We could remove this, except that javafx-sg-common then has
     * a circular dependency with javafx-ui-common.
     */
    public enum CacheHint {
        DEFAULT,          // aka QUALITY
        SCALE,
        ROTATE,
        SCALE_AND_ROTATE, // aka SPEED
    }

    public void setCachedAsBitmap(boolean cached, CacheHint cacheHint);

    public void setEffect(Object effect);
    public void effectChanged();

    public void release();
}
