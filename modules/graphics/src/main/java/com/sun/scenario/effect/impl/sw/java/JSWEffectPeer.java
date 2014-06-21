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

package com.sun.scenario.effect.impl.sw.java;

import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.RenderState;

public abstract class JSWEffectPeer<T extends RenderState> extends EffectPeer<T> {

    protected JSWEffectPeer(FilterContext fctx, Renderer r, String uniqueName) {
        super(fctx, r, uniqueName);
    }

    protected final static int FVALS_A = 3;
    protected final static int FVALS_R = 0;
    protected final static int FVALS_G = 1;
    protected final static int FVALS_B = 2;

    protected final void laccum(int pixel, float mul, float fvals[]) {
        mul /= 255f;
        fvals[FVALS_R] += ((pixel >>  16) & 0xff) * mul;
        fvals[FVALS_G] += ((pixel >>   8) & 0xff) * mul;
        fvals[FVALS_B] += ((pixel       ) & 0xff) * mul;
        fvals[FVALS_A] += ((pixel >>> 24)       ) * mul;
    }

    protected final void lsample(int img[],
                                 float floc_x, float floc_y,
                                 int w, int h, int scan,
                                 float fvals[])
    {
        fvals[0] = 0f;
        fvals[1] = 0f;
        fvals[2] = 0f;
        fvals[3] = 0f;
        // If we subtract 0.5 then floc_xy could go negative and the
        // integer cast will not perform a true floor operation so
        // instead we add 0.5 and then iloc_xy will be off by 1
        floc_x = floc_x * w + 0.5f;
        floc_y = floc_y * h + 0.5f;
        int iloc_x = (int) floc_x;  // 0 <= iloc_x <= w
        int iloc_y = (int) floc_y;  // 0 <= iloc_y <= h
        // Note we test floc against 0 because iloc may have rounded the wrong way
        // for some numbers.  But, iloc values are valid for testing against w,h
        if (floc_x > 0 && floc_y > 0 && iloc_x <= w && iloc_y <= h) {
            floc_x -= iloc_x;   // now fractx
            floc_y -= iloc_y;   // now fracty
            // sample box from iloc_x-1,y-1 to iloc_x,y
            int offset = iloc_y * scan + iloc_x;
            float fract = floc_x * floc_y;
            if (iloc_y < h) {
                if (iloc_x < w) {
                    laccum(img[offset], fract, fvals);
                }
                if (iloc_x > 0) {
                    laccum(img[offset-1], floc_y - fract, fvals);
                }
            }
            if (iloc_y > 0) {
                if (iloc_x < w) {
                    laccum(img[offset-scan], floc_x - fract, fvals);
                }
                if (iloc_x > 0) {
                    laccum(img[offset-scan-1], 1f - floc_x - floc_y + fract, fvals);
                }
            }
        }
    }

    protected final void laccumsample(int img[],
                                      float fpix_x, float fpix_y,
                                      int w, int h, int scan,
                                      float factor, float fvals[])
    {
        factor *= 255f;
        // If we subtract 0.5 then floc_xy could go negative and the
        // integer cast will not perform a true floor operation so
        // instead we add 0.5 and then iloc_xy will be off by 1
        fpix_x = fpix_x + 0.5f;
        fpix_y = fpix_y + 0.5f;
        int ipix_x = (int) fpix_x;  // 0 <= ipix_x <= w
        int ipix_y = (int) fpix_y;  // 0 <= ipix_y <= h
        // Note we test fpix against 0 because ipix may have rounded the wrong way
        // for some numbers.  But, ipix values are valid for testing against w,h
        if (fpix_x > 0 && fpix_y > 0 && ipix_x <= w && ipix_y <= h) {
            fpix_x -= ipix_x;   // now fractx
            fpix_y -= ipix_y;   // now fracty
            // sample box from ipix_x-1,y-1 to ipix_x,y
            int offset = ipix_y * scan + ipix_x;
            float fract = fpix_x * fpix_y;
            if (ipix_y < h) {
                if (ipix_x < w) {
                    laccum(img[offset], fract * factor, fvals);
                }
                if (ipix_x > 0) {
                    laccum(img[offset-1], (fpix_y - fract) * factor, fvals);
                }
            }
            if (ipix_y > 0) {
                if (ipix_x < w) {
                    laccum(img[offset-scan], (fpix_x - fract) * factor, fvals);
                }
                if (ipix_x > 0) {
                    laccum(img[offset-scan-1], (1f - fpix_x - fpix_y + fract) * factor, fvals);
                }
            }
        }
    }

    protected final void faccum(float map[], int offset, float mul,
                                float fvals[])
    {
        fvals[0] += map[offset  ] * mul;
        fvals[1] += map[offset+1] * mul;
        fvals[2] += map[offset+2] * mul;
        fvals[3] += map[offset+3] * mul;
    }

    protected final void fsample(float map[],
                                 float floc_x, float floc_y,
                                 int w, int h, int scan,
                                 float fvals[])
    {
        fvals[0] = 0f;
        fvals[1] = 0f;
        fvals[2] = 0f;
        fvals[3] = 0f;
        // If we subtract 0.5 then floc_xy could go negative and the
        // integer cast will not perform a true floor operation so
        // instead we add 0.5 and then iloc_xy will be off by 1
        floc_x = floc_x * w + 0.5f;
        floc_y = floc_y * h + 0.5f;
        int iloc_x = (int) floc_x;  // 0 <= iloc_x <= w
        int iloc_y = (int) floc_y;  // 0 <= iloc_y <= h
        // Note we test floc against 0 because iloc may have rounded the wrong way
        // for some numbers.  But, iloc values are valid for testing against w,h
        if (floc_x > 0 && floc_y > 0 && iloc_x <= w && iloc_y <= h) {
            floc_x -= iloc_x;   // now fractx
            floc_y -= iloc_y;   // now fracty
            // sample box from iloc_x-1,y-1 to iloc_x,y
            int offset = 4*(iloc_y * scan + iloc_x);
            float fract = floc_x * floc_y;
            if (iloc_y < h) {
                if (iloc_x < w) {
                    faccum(map, offset, fract, fvals);
                }
                if (iloc_x > 0) {
                    faccum(map, offset-4, floc_y - fract, fvals);
                }
            }
            if (iloc_y > 0) {
                if (iloc_x < w) {
                    faccum(map, offset-scan*4, floc_x - fract, fvals);
                }
                if (iloc_x > 0) {
                    faccum(map, offset-scan*4-4, 1f - floc_x - floc_y + fract, fvals);
                }
            }
        }
    }
}
