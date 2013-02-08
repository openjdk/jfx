/*
 * Copyright (c) 2009, 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.state;

import com.sun.scenario.effect.Color4f;
import com.sun.scenario.effect.Effect.AccelType;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;

/**
 * The helper class for defining a 1 dimensional linear convolution shadow
 * kernel for the LinearConvolveShadow Shader.  This class leverages the
 * {@link BoxBlurState} class for defining the kernel and simply stores
 * additional {@code shadowColor} and {@code spread} properties for the
 * associated support methods for the shadow version of the shader.
 */
public class BoxShadowState extends BoxBlurState {
    private Color4f shadowColor;
    private float spread;

    public Color4f getShadowColor() {
        return shadowColor;
    }

    public void setShadowColor(Color4f shadowColor) {
        if (shadowColor == null) {
            throw new IllegalArgumentException("Color must be non-null");
        }
        this.shadowColor = shadowColor;
    }

    @Override
    public float getSpread() {
        return spread;
    }

    public void setSpread(float spread) {
        if (spread < 0f || spread > 1f) {
            throw new IllegalArgumentException("Spread must be in the range [0,1]");
        }
        this.spread = spread;
    }

    @Override
    public boolean isNop() {
        // The shadow operation is never a NOP since it replaces the colors
        // if nothing else.
        return false;
    }

    @Override
    public boolean isNop(int pass) {
        // Only the first pass of a shadow can be a NOP since the second
        // pass always replaces the colors if nothing else.
        return (pass == 0) && super.isNop(pass);
    }

    @Override
    public boolean isShadow() {
        return true;
    }

    @Override
    public float[] getShadowColorComponents(int pass) {
        return (pass == 0)
            ? BLACK_COMPONENTS
            : shadowColor.getPremultipliedRGBComponents();
    }

    @Override
    public EffectPeer getPeer(Renderer r, FilterContext fctx, int pass) {
        int ksize = getScaledKernelSize(pass);
        if (pass == 0 && ksize <= 1) {
            // The result of ksize being too <= 1 is a NOP
            // so we return null here to skip the corresponding
            // filter pass.
            // Note that we can only skip the first pass, the
            // second pass is always required to convert the
            // colors of the source into the desired shadow color.
            return null;
        }
        int psize = getPeerSize(ksize);
        AccelType actype = r.getAccelType();
        String name;
        switch (actype) {
            case NONE:
            case SIMD:
                if (spread == 0.0f) {
                    name = "BoxShadow";
                    break;
                }
                /* FALLS THROUGH */
            default:
                name = "LinearConvolveShadow";
                break;
        }
        EffectPeer peer = r.getPeerInstance(fctx, name, psize);
        return peer;
    }
}
