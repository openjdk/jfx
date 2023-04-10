/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.prism;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.Graphics;
import com.sun.prism.Texture;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.ImageDataRenderer;

public class PrRenderInfo implements ImageDataRenderer {
    private Graphics g;

    public PrRenderInfo(Graphics g) {
        this.g = g;
    }

    public Graphics getGraphics() {
        return g;
    }

    // RT-27390
    // TODO: Have Graphics implement ImageRenderer directly to avoid
    // needing a wrapper object...
    @Override
    public void renderImage(ImageData image,
                            BaseTransform transform,
                            FilterContext fctx)
    {
        if (image.validate(fctx)) {
            Rectangle r = image.getUntransformedBounds();
            // the actual image may be much larger than the region
            // of interest ("r"), so to improve performance we render
            // only that subregion here
            Texture tex = ((PrTexture)image.getUntransformedImage()).getTextureObject();
            BaseTransform savedTx = null;
            if (!transform.isIdentity()) {
                savedTx = g.getTransformNoClone().copy();
                g.transform(transform);
            }
            BaseTransform idtx = image.getTransform();
            if (!idtx.isIdentity()) {
                if (savedTx == null) savedTx = g.getTransformNoClone().copy();
                g.transform(idtx);
            }
            g.drawTexture(tex, r.x, r.y, r.width, r.height);
            if (savedTx != null) {
                g.setTransform(savedTx);
            }
        }
    }
}
