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

import java.nio.FloatBuffer;
import com.sun.scenario.effect.Effect.AccelType;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.impl.BufferUtil;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;

/**
 * The state and implementation class for calculating 1 dimensional
 * linear convolution kernels for performing multi-pass box blurs.
 */
public class BoxBlurState extends HVSeparableKernel {
    private int hsize;
    private int vsize;
    private int blurPasses;
    private FloatBuffer weights;

    public int getHsize() {
        return hsize;
    }

    public void setHsize(int hsize) {
        if (hsize < 0 || hsize > 255) {
            throw new IllegalArgumentException("Blur size must be in the range [0,255]");
        }
        this.hsize = hsize;
    }

    public int getVsize() {
        return vsize;
    }

    public void setVsize(int vsize) {
        if (vsize < 0 || vsize > 255) {
            throw new IllegalArgumentException("Blur size must be in the range [0,255]");
        }
        this.vsize = vsize;
    }

    public int getBlurPasses() {
        return blurPasses;
    }

    public void setBlurPasses(int blurPasses) {
        if (blurPasses < 0 || blurPasses > 3) {
            throw new IllegalArgumentException("Number of passes must be in the range [0,3]");
        }
        this.blurPasses = blurPasses;
    }

    public float getSpread() {
        return 0.0f;
    }

    @Override
    public boolean isNop() {
        return (blurPasses == 0 || (hsize <= 1 && vsize <= 1));
    }

    @Override
    public boolean isNop(int pass) {
        return (blurPasses == 0 || ((pass == 0) ? hsize : vsize) <= 1);
    }

    public int getKernelSize(int pass) {
        int ksize = pass == 0 ? hsize : vsize;
        if (ksize < 1) ksize = 1;
        ksize = (ksize-1) * blurPasses + 1;
        ksize |= 1;
        return ksize;
    }

    private int getScaleVal(int pass, boolean needScale) {
        int ksize = getKernelSize(pass);
        int scale = 0;
        while (ksize > 128) {
            ksize = ((ksize + 1) / 2) | 1;
            scale--;
        }
        return needScale ? scale : ksize;
    }

    public int getPow2Scale(int pass) {
        return getScaleVal(pass, true);
    }

    @Override
    public int getScaledKernelSize(int pass) {
        return getScaleVal(pass, false);
    }

    @Override
    public int getPow2ScaleX() {
        return getPow2Scale(0);
    }

    @Override
    public int getPow2ScaleY() {
        return getPow2Scale(1);
    }

    public FloatBuffer getWeights(int pass) {
        int klen = pass == 0 ? hsize : vsize;
        if (klen < 1 || blurPasses == 0) klen = 1;
        long ik[] = new long[klen];
        for (int i = 0; i < klen; i++) {
            ik[i] = 1;
        }
        for (int p = 1; p < blurPasses; p++) {
            long ik2[] = new long[ik.length + klen-1];
            for (int i = 0; i < ik.length; i++) {
                for (int k = 0; k < klen; k++) {
                    ik2[i+k] += ik[i];
                }
            }
            ik = ik2;
        }
        if ((ik.length & 1) == 0) {
            // If kernel length is odd then it is centered on a pixel.
            // If kernel length is even, then it is not centered on a pixel
            // and the weights are applied to a sample between pixels.
            // Instead of trying to sample between pixels we instead
            // distribute the weights by half a pixel by averaging
            // adjacent values together and lengthening the kernel by a pixel.
            long ik2[] = new long[ik.length + 1];
            for (int i = 0; i < ik.length; i++) {
                ik2[i] += ik[i];
                ik2[i+1] += ik[i];
            }
            ik = ik2;
        }
        int scale = getPow2Scale(pass);
        while (scale < 0) {
            int newlen = ((ik.length + 1) / 2) | 1;
            int skewi = (newlen * 2 - ik.length) / 2;
            long ik2[] = new long[newlen];
            for (int i = 0; i < ik.length; i++) {
                ik2[skewi / 2] += ik[i];
                skewi++;
                ik2[skewi / 2] += ik[i];
            }
            ik = ik2;
            scale++;
        }
        double sum = 0.0;
        for (int i = 0; i < ik.length; i++) {
            sum += ik[i];
        }
        // We need to apply the spread on only one pass
        // Prefer pass1 if r1 is not trivial
        // Otherwise use pass 0 so that it doesn't disappear
        int spreadpass = (vsize > 1) ? 1 : 0;
        float s = (pass == spreadpass) ? getSpread() : 0f;
        sum += (ik[0] - sum) * s;

        if (weights == null) {
            // peersize(MAX_KERNEL_SIZE) rounded up to the next multiple of 4
            int maxbufsize = LinearConvolveKernel.MAX_KERNEL_SIZE;
            maxbufsize = LinearConvolveKernel.getPeerSize(maxbufsize);
            maxbufsize = (maxbufsize + 3) & (~3);
            weights = BufferUtil.newFloatBuffer(maxbufsize);
        }
        weights.clear();
        for (int i = 0; i < ik.length; i++) {
            weights.put((float) (ik[i] / sum));
        }
        int limit = getPeerSize(ik.length);
        while (weights.position() < limit) {
            weights.put(0f);
        }
        weights.limit(limit);
        weights.rewind();
        return weights;
    }

    @Override
    public EffectPeer getPeer(Renderer r, FilterContext fctx, int pass) {
        int ksize = getScaledKernelSize(pass);
        if (ksize <= 1) {
            // The result of ksize being too <= 1 is a NOP
            // so we return null here to skip the corresponding
            // filter pass.
            return null;
        }
        int psize = getPeerSize(ksize);
        AccelType actype = r.getAccelType();
        String name;
        switch (actype) {
            case NONE:
            case SIMD:
                name = "BoxBlur";
                break;
            default:
                name = "LinearConvolve";
                break;
        }
        EffectPeer peer = r.getPeerInstance(fctx, name, psize);
        return peer;
    }
}
