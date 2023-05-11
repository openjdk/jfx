/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.LinearConvolveKernel;
import com.sun.scenario.effect.impl.state.LinearConvolveRenderState;

/**
 * An intermediate mix-in superclass that performs the multi-pass filtering
 * algorithm common to all linear convolution filters such as Gaussian,
 * Box, and Motion Blurs and Shadows.  In particular, it is used for all
 * filters that use the LinearConvolve and LinearConvolveShadow shader peers.
 */
public abstract class LinearConvolveCoreEffect
    extends CoreEffect<LinearConvolveRenderState>
{
    public LinearConvolveCoreEffect(Effect input) {
        super(input);
    }

    @Override
    public final LinearConvolveRenderState
        getRenderState(FilterContext fctx,
                       BaseTransform transform,
                       Rectangle outputClip,
                       Object renderHelper,
                       Effect defaultInput)
    {
        return getState().getRenderState(transform);
    }

    @Override
    abstract LinearConvolveKernel getState();

    @Override
    public ImageData filterImageDatas(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      LinearConvolveRenderState lcrstate,
                                      ImageData... inputs)
    {
        ImageData src = inputs[0];
        src.addref();
        if (lcrstate.isNop()) {
            return src;
        }
        Rectangle approxBounds = inputs[0].getUntransformedBounds();
        int approxW = approxBounds.width;
        int approxH = approxBounds.height;
        Rectangle filterClip = outputClip;

        Renderer r = Renderer.getRenderer(fctx, this, approxW, approxH);
        for (int pass = 0; pass < 2; pass++) {
            src = lcrstate.validatePassInput(src, pass);
            EffectPeer peer = lcrstate.getPassPeer(r, fctx);
            if (peer != null) {
                peer.setPass(pass);
                ImageData res = peer.filter(this, lcrstate, transform, filterClip, src);
                src.unref();
                src = res;
                if (!src.validate(fctx)) {
                    src.unref();
                    return src;
                }
            }
        }

        return src;
    }
}
