/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.effect;

import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.Effect;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;

public class EffectUtils {
    public static BaseBounds transformBounds(BaseTransform tx, BaseBounds r) {
        if (tx == null || tx.isIdentity()) {
            return r;
        }
        BaseBounds ret = new RectBounds();
        ret = tx.transform(r, ret);
        return ret;
    }

    // utility method used in calculation of bounds in BoxBlur and DropShadow effects
    public static int getKernelSize(int ksize, int iterations) {
        if (ksize < 1) ksize = 1;
        ksize = (ksize-1) * iterations + 1;
        ksize |= 1;
        return ksize / 2;
    }

    // utility method used for calculation of bounds in Shadow and DropShadow effects
    public static BaseBounds getShadowBounds(BaseBounds bounds, 
                                             BaseTransform tx,
                                             float width,
                                             float height,
                                             BlurType blurType) {
        int hgrow = 0;
        int vgrow = 0;

        switch (blurType) {
            case GAUSSIAN:
                float hradius = width < 1.0f ? 0.0f : ((width - 1.0f) / 2.0f);
                float vradius = height < 1.0f ? 0.0f : ((height - 1.0f) / 2.0f);
                hgrow = (int) Math.ceil(hradius);
                vgrow = (int) Math.ceil(vradius);
                break;
            case ONE_PASS_BOX:
                hgrow = getKernelSize(Math.round(width/3.0f), 1);
                vgrow = getKernelSize(Math.round(height/3.0f), 1);
                break;
            case TWO_PASS_BOX:
                hgrow = getKernelSize(Math.round(width/3.0f), 2);
                vgrow = getKernelSize(Math.round(height/3.0f), 2);
                break;
            case THREE_PASS_BOX:
                hgrow = getKernelSize(Math.round(width/3.0f), 3);
                vgrow = getKernelSize(Math.round(height/3.0f), 3);
                break;
        }

        bounds = bounds.deriveWithPadding(hgrow, vgrow, 0);

        return transformBounds(tx, bounds);
    }

    // Returns input bounds for an effect. These are either bounds of input effect or
    // geometric bounds of the node on which the effect calling this method is applied.
    public static BaseBounds getInputBounds(BaseBounds bounds,
                                            BaseTransform tx,
                                            Node node,
                                            BoundsAccessor boundsAccessor,
                                            Effect input) {
        if (input != null) {
            bounds = input.impl_getBounds(bounds, tx, node, boundsAccessor);
        } else {
            bounds = boundsAccessor.getGeomBounds(bounds, tx, node);
        }

        return bounds;
    }
}
