/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.prism.ps;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.RTTexture;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.PrRenderer;
import com.sun.scenario.effect.impl.prism.PrTexture;
import com.sun.scenario.effect.impl.state.RenderState;

public class PPStoPSWDisplacementMapPeer extends EffectPeer  {
    PrRenderer softwareRenderer;
    EffectPeer softwarePeer;

    public PPStoPSWDisplacementMapPeer(FilterContext fctx, Renderer r, String shaderName) {
        super(fctx, r, shaderName);
        softwareRenderer = (PrRenderer) Renderer.getRenderer(fctx);
        softwarePeer = softwareRenderer.getPeerInstance(fctx, "DisplacementMap", 0);
    }

    @Override
    public ImageData filter(Effect effect,
                            RenderState rstate,
                            BaseTransform transform,
                            Rectangle outputClip,
                            ImageData... inputs)
    {
        ImageData input = inputs[0];
        PrTexture srcTex = (PrTexture) input.getUntransformedImage();
        RTTexture srcRT = (RTTexture) srcTex.getTextureObject();
        // The software renderer produces drawables that also implement HeapImage
        PrDrawable srcDrawable = softwareRenderer.createDrawable(srcRT);
        ImageData heapinput = new ImageData(getFilterContext(), srcDrawable,
                                            input.getUntransformedBounds());
        heapinput = heapinput.transform(input.getTransform());

        // The software peer will return a PrDrawable that can produce a
        // prism Texture on demand as needed.
        ImageData ret = softwarePeer.filter(effect, rstate, transform, outputClip, heapinput);
        // Note that heapinput should not be unref()ed since it shares the
        // rtt with input/srcTex and we do not want it to dispose the rtt.
        return ret;
    }
}
