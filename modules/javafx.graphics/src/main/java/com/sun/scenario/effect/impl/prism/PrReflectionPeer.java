/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.Reflection;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.RenderState;

public class PrReflectionPeer extends EffectPeer {

    public PrReflectionPeer(FilterContext fctx, Renderer r, String uniqueName) {
        super(fctx, r, uniqueName);
    }

    @Override
    public ImageData filter(Effect effect,
                            RenderState rstate,
                            BaseTransform transform,
                            Rectangle outputClip,
                            ImageData... inputs)
    {
        FilterContext fctx = getFilterContext();
        Reflection reflect = (Reflection)effect;

        Rectangle inputbounds = inputs[0].getUntransformedBounds();
        int srcW = inputbounds.width;
        int srcH = inputbounds.height;
        float refY = srcH + reflect.getTopOffset();
        float refH = reflect.getFraction() * srcH;
        int irefY1 = (int) Math.floor(refY);
        int irefY2 = (int) Math.ceil(refY + refH);
        int irefH = irefY2 - irefY1;

        int dstH = (irefY2 > srcH) ? irefY2 : srcH;
        // RT-27389: take clipping into account...
        PrDrawable dst = (PrDrawable)getRenderer().getCompatibleImage(srcW, dstH);
        if (!inputs[0].validate(fctx) || dst == null) {
            return new ImageData(fctx, null, inputbounds);
        }
        PrDrawable src = (PrDrawable)inputs[0].getUntransformedImage();
        Texture srctex = src.getTextureObject();

        Graphics gdst = dst.createGraphics();
        gdst.transform(inputs[0].getTransform());
        float sx1 = 0f;
        float sy1 = srcH-irefH;
        float sx2 = srcW;
        float sy2 = srcH;
        gdst.drawTextureVO(srctex,
                           reflect.getBottomOpacity(),
                           reflect.getTopOpacity(),
                           0, irefY2, srcW, irefY1,
                           sx1, sy1, sx2, sy2);
        gdst.drawTexture(srctex, 0, 0, srcW, srcH);

        Rectangle newbounds =
            new Rectangle(inputbounds.x, inputbounds.y, srcW, dstH);
        return new ImageData(fctx, dst, newbounds);
    }
}
