/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Color4f;

/**
 * The state and implementation class for calculating 1 dimensional
 * linear convolution kernels for performing Gaussian blurs.
 */
public class GaussianBlurState extends HVSeparableKernel {
    private float hradius;
    private float vradius;

    void checkRadius(float radius) {
        if (radius < 0f || radius > 63f) {
            throw new IllegalArgumentException("Radius must be in the range [1,63]");
        }
    }

    public float getRadius() {
        return (hradius + vradius) / 2.0f;
    }

    public void setRadius(float radius) {
        checkRadius(radius);
        this.hradius = radius;
        this.vradius = radius;
    }

    public float getHRadius() {
        return hradius;
    }

    public void setHRadius(float hradius) {
        checkRadius(hradius);
        this.hradius = hradius;
    }

    public float getVRadius() {
        return vradius;
    }

    public void setVRadius(float vradius) {
        checkRadius(vradius);
        this.vradius = vradius;
    }

    float getRadius(int pass) {
        return (pass == 0 ? hradius : vradius);
    }

    @Override
    public boolean isNop() {
        return hradius == 0 && vradius == 0;
    }

    public int getPad(int pass) {
        return (int) Math.ceil(getRadius(pass));
    }

    @Override
    public int getKernelSize(int pass) {
        return getPad(pass) * 2 + 1;
    }

    public float getSpread() {
        return 0f;
    }

    public Color4f getShadowColor() {
        return null;
    }

    @Override
    public LinearConvolveRenderState getRenderState(BaseTransform filtertx) {
        return new GaussianRenderState(hradius, vradius, getSpread(),
                                       this instanceof GaussianShadowState, getShadowColor(),
                                       filtertx);
    }
}
