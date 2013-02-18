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

import java.nio.FloatBuffer;
import com.sun.scenario.effect.impl.BufferUtil;

/**
 * The state and implementation class for calculating 1 dimensional
 * linear convolution kernels for performing gaussian blurs.
 */
public class GaussianBlurState extends HVSeparableKernel {
    private float hradius;
    private float vradius;
    private FloatBuffer weights;

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

    @Override
    public boolean isNop(int pass) {
        return getRadius(pass) == 0;
    }

    public int getPad(int pass) {
        return (int) Math.ceil(getRadius(pass));
    }

    public int getScaledPad(int pass) {
        return getPad(pass);
    }

    public float getScaledRadius(int pass) {
        return getRadius(pass);
    }

    @Override
    public int getKernelSize(int pass) {
        return getPad(pass) * 2 + 1;
    }

    public float getSpread() {
        return 0f;
    }

    @Override
    public FloatBuffer getWeights(int pass) {
        float r0 = getScaledRadius(0);
        float r1 = getScaledRadius(1);
        // We need to apply the spread on only one pass
        // Prefer pass1 if r1 is not tiny (or at least bigger than r0)
        // Otherwise use pass 0 so that it doesn't disappear
        int spreadpass = (r1 > 1f || r1 >= r0) ? 1 : 0;

        float r = (pass == 0) ? r0 : r1;
        float s = (pass == spreadpass) ? getSpread() : 0f;
        weights = getGaussianWeights(weights, getScaledPad(pass), r, s);
        return weights;
    }

    static FloatBuffer getGaussianWeights(FloatBuffer weights,
                                          int pad,
                                          float radius,
                                          float spread)
    {
        int r = pad;
        int klen = (r * 2) + 1;
        if (weights == null) {
            weights = BufferUtil.newFloatBuffer(128);
        }
        weights.clear();
        float sigma = radius / 3;
        float sigma22 = 2 * sigma * sigma;
        if (sigma22 < Float.MIN_VALUE) {
            // Avoid divide by 0 below (it can generate NaN values).
            sigma22 = Float.MIN_VALUE;
        }
        float total = 0.0F;
        for (int row = -r; row <= r; row++) {
            float kval = (float) Math.exp(-(row * row) / sigma22);
            weights.put(kval);
            total += kval;
        }
        total += (weights.get(0) - total) * spread;
        for (int i = 0; i < klen; i++) {
            weights.put(i, weights.get(i) / total);
        }
        int limit = getPeerSize(klen);
        while (weights.position() < limit) {
            weights.put(0.0F);
        }
        weights.limit(limit);
        weights.rewind();
        return weights;
    }
}
