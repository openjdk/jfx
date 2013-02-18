/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * The helper class for defining a 1 dimensional linear convolution shadow
 * kernel for the gaussian Shadow shader.  This class leverages the
 * {@link GaussianBlurState} class for defining the kernel and simply stores
 * additional {@code shadowColor} and {@code spread} properties for the
 * associated support methods for the shadow version of the shader.
 */
public class GaussianShadowState extends GaussianBlurState {
    private Color4f shadowColor;
    private float spread;

    @Override
    void checkRadius(float radius) {
        if (radius < 0f || radius > 127f) {
            throw new IllegalArgumentException("Radius must be in the range [1,127]");
        }
    }

    private int getPow2Scale(int pass) {
        // Technically a radius of 127 would be scaled once down to a radius
        // of 63.5 which generates a kernel of length 129, but we will be
        // fudging it so that it only generates the central 127 values and
        // the last value should be so close to 3*sigma to not be noticeable.
        // This avoids a special "extra" scaling just for values > 126.
        // So, we only ever scale at most once and then just short the kernel
        // for those last couple of values.
        return (getRadius(pass) > 63) ? -1 : 0;
    }

    @Override
    public int getPow2ScaleX() {
        return getPow2Scale(0);
    }

    @Override
    public int getPow2ScaleY() {
        return getPow2Scale(1);
    }

    @Override
    public float getScaledRadius(int pass) {
        float r = getRadius(pass);
        int s = getPow2Scale(pass);
        while (s < 0) {
            r /= 2;
            s++;
        }
        return r;
    }

    @Override
    public int getScaledPad(int pass) {
        float r = getScaledRadius(pass);
        // A radius of 127 will be scaled back to 63.5 which technically
        // requires a pad of 64 and a kernel size of 129, but we need to
        // clip the kernel at 128 and thus the pad at 63.
        return (r > 63.0f) ? 63 : (int) Math.ceil(r);
    }

    @Override
    public int getScaledKernelSize(int pass) {
        return getScaledPad(pass) * 2 + 1;
    }

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
}
