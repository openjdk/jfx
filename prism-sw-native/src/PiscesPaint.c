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

#include <PiscesUtil.h>
#include <PiscesRenderer.h>

#include <PiscesSysutils.h>
#include <PiscesMath.h>


static jlong lmod(jlong x, jlong y) {
    x = x % y;
    if (x < 0) {
        x += y;
    }
    return x;
}

static INLINE jint interp(jint x0, jint x1, jint frac) {
    return ((x0 << 16) + (x1 - x0) * frac + 0x8000) >> 16;
}

static INLINE jint
pad(jint ifrac, jint cycleMethod) {
    switch (cycleMethod) {
    case CYCLE_NONE:
        if (ifrac < 0) {
            ifrac = 0;
        } else if (ifrac > 0xffff) {
            ifrac = 0xffff;
        }
        break;
    case CYCLE_REPEAT:
        ifrac &= 0xffff;
        break;
    case CYCLE_REFLECT:
        if (ifrac < 0) {
            ifrac = -ifrac;
        }
        ifrac &= 0x1ffff;
        if (ifrac > 0xffff) {
            ifrac = 0x1ffff - ifrac;
        }
        break;
    }

    return ifrac;
}

void
genLinearGradientPaint(Renderer *rdr, jint height) {
    jint paintOffset = 0;
    jint width = rdr->_alphaWidth;

    jint minX, maxX;
    jfloat frac;
    jint pidx;

    jint x, y;
    jint i, j;

    jint cycleMethod = rdr->_gradient_cycleMethod;
    jfloat mx = rdr->_lg_mx;
    jfloat my = rdr->_lg_my;
    jfloat b = rdr->_lg_b;

    jint* paint = rdr->_paint;
    jint* colors = rdr->_gradient_colors;

    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;

    y = rdr->_currY;
    for (j = 0; j < height; j++, y++) {
        x = rdr->_currX;
        pidx = paintOffset;
        
        frac = x * mx + y * my + b;
        for (i = 0; i < width; i++, pidx++) {
            jint ifrac = pad((jint)frac, cycleMethod);
            ifrac >>= 16 - LG_GRADIENT_MAP_SIZE;
            paint[pidx] = colors[ifrac];

            frac += mx;
        }

        paintOffset += width;
    }
}

void
genRadialGradientPaint(Renderer *rdr, jint height) {
    jint cycleMethod = rdr->_gradient_cycleMethod;
    jint width = rdr->_alphaWidth;
    jint minX, maxX;
    jint paintOffset = 0;
    jint pidx;
    jint i, j;
    jint x, y;

    jfloat a00, a01, a02, a10, a11, a12;
    jfloat cx, cy, fx, fy, r, rsq;
    jfloat cfxcfx, cfycfy, cfxcfy;
    jfloat a00a00, a10a10, a00a10, sube;
    
    float txx, tyy, fxx, fyy, cfx, cfy;
    float A, B, B2, C, C2, U, dU, V, dV, ddV, tmp;
    float _Csq, _C;
    jint ifrac;

    jint* paint = rdr->_paint;
    jint* colors = rdr->_gradient_colors;

    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;

    a00 = rdr->_rg_a00;
    a01 = rdr->_rg_a01;
    a02 = rdr->_rg_a02;
    a10 = rdr->_rg_a10;
    a11 = rdr->_rg_a11;
    a12 = rdr->_rg_a12;

    a00a00 = rdr->_rg_a00a00;
    a10a10 = rdr->_rg_a10a10;
    a00a10 = rdr->_rg_a00a10;

    cx = rdr->_rg_cx;
    cy = rdr->_rg_cy;
    fx = rdr->_rg_fx;
    fy = rdr->_rg_fy;
    r = rdr->_rg_r;
    rsq = rdr->_rg_rsq;

    y = rdr->_currY;
    for (j = 0; j < height; j++, y++) {
        pidx = paintOffset;
        x = rdr->_currX;

        txx = x * a00 + y * a01 + a02;
        tyy = x * a10 + y * a11 + a12;

        fxx = fx - txx;
        fyy = fy - tyy;
        A = fxx * fxx + fyy * fyy;
        cfx = cx - fx;
        cfy = cy - fy;
        cfxcfx = (jfloat)(cfx * cfx);
        cfycfy = (jfloat)(cfy * cfy);
        cfxcfy = (jfloat)(cfx * cfy);
        B = (cfx * fxx + cfy * fyy);
        B2 = -B * 2.0f;
        C = cfxcfx + cfycfy - rsq;
        C2 = 2.0f * C;
        _C = 1.0f / C;
        _Csq = _C * _C;
        U = (-B * _C);
        dU = (a00 * cfx + a10 * cfy) * _C;
        V =  ((B * B - A * C) * _Csq);
        sube = 2.0f * a00a10 *cfxcfy;
        dV =  (sube +
              (a00a00 * (cfxcfx - C) + a00 * (B2 * cfx + C2 * fxx)) +
              (a10a10 * (cfycfy - C) + a10 * (B2 * cfy + C2 * fyy))) * _Csq;
        tmp = a00a00*cfycfy - sube + a10a10*cfxcfx;
        ddV = 2.0f * ((a00a00 + a10a10) * rsq - tmp) * _Csq;

        U   = (65536.0f * U); // 65536.0f to be in fixed-point level needed by "frac"
        V   = (65536.0f * 65536.0f * V); // 65536.0f * 65536.0f to stay in fixed point level after sqrt 
        dU  = (65536.0f * dU);
        dV  = (65536.0f * 65536.0f * dV);
        ddV = (65536.0f * 65536.0f * ddV);
        for (i = 0; i < width; i++, pidx++) {
            if (V < 0) {
                V = 0;
            }
            
            ifrac = (jint)(U + PISCESsqrt(V));
            
            U += dU;
            V += dV ;
            dV += ddV;
            
            ifrac = pad(ifrac, cycleMethod);
            ifrac >>= (16 - LG_GRADIENT_MAP_SIZE);
            paint[pidx] = colors[ifrac];
        }
        
        paintOffset += width;
    }
}

static INLINE jint interpolate2points(jint p0, jint p1, jint frac) {
    jint a0 = (p0 >> 24) & 0xff;
    jint r0 = (p0 >> 16) & 0xff;
    jint g0 = (p0 >> 8)  & 0xff;
    jint b0 =  p0        & 0xff;
    
    jint a1 = (p1 >> 24) & 0xff;
    jint r1 = (p1 >> 16) & 0xff;
    jint g1 = (p1 >> 8)  & 0xff;
    jint b1 =  p1        & 0xff;
    
    jint aa = interp(a0, a1, frac);
    jint rr = interp(r0, r1, frac);
    jint gg = interp(g0, g1, frac);
    jint bb = interp(b0, b1, frac);
    
    return (aa << 24) | (rr << 16) | (gg << 8) | bb;
}

/**
 * Function interpolate4points() takes color ARGB-value of pixel p00 and 
 * recalculates (using linear interpolation) it's color with ARGB values of
 * neighbouring pixels. 
 * p01 - right neighbour of p00
 * p10 - below p00
 * p11 - below right 
 */    

static INLINE jint interpolate4points(jint p00, jint p01, jint p10, jint p11,
                               jint hfrac, jint vfrac) {
    jint a00 = (p00 >> 24) & 0xff;
    jint r00 = (p00 >> 16) & 0xff;
    jint g00 = (p00 >> 8)  & 0xff;
    jint b00 =  p00        & 0xff;

    jint a01 = (p01 >> 24) & 0xff;
    jint r01 = (p01 >> 16) & 0xff;
    jint g01 = (p01 >> 8)  & 0xff;
    jint b01 =  p01        & 0xff;

    jint a0 = interp(a00, a01, hfrac);
    jint r0 = interp(r00, r01, hfrac);
    jint g0 = interp(g00, g01, hfrac);
    jint b0 = interp(b00, b01, hfrac);

    jint a10 = (p10 >> 24) & 0xff;
    jint r10 = (p10 >> 16) & 0xff;
    jint g10 = (p10 >> 8)  & 0xff;
    jint b10 =  p10        & 0xff;

    jint a11 = (p11 >> 24) & 0xff;
    jint r11 = (p11 >> 16) & 0xff;
    jint g11 = (p11 >> 8)  & 0xff;
    jint b11 =  p11        & 0xff;

    jint a1 = interp(a10, a11, hfrac);
    jint r1 = interp(r10, r11, hfrac);
    jint g1 = interp(g10, g11, hfrac);
    jint b1 = interp(b10, b11, hfrac);

    jint aa = interp(a0, a1, vfrac);
    jint rr = interp(r0, r1, vfrac);
    jint gg = interp(g0, g1, vfrac);
    jint bb = interp(b0, b1, vfrac);

    return (aa << 24) | (rr << 16) | (gg << 8) | bb;
}

static INLINE jint interpolate2pointsNoAlpha(jint p0, jint p1, jint frac) {
    jint r0 = (p0 >> 16) & 0xff;
    jint g0 = (p0 >> 8)  & 0xff;
    jint b0 =  p0        & 0xff;

    jint r1 = (p1 >> 16) & 0xff;
    jint g1 = (p1 >> 8)  & 0xff;
    jint b1 =  p1        & 0xff;

    jint rr = interp(r0, r1, frac);
    jint gg = interp(g0, g1, frac);
    jint bb = interp(b0, b1, frac);

    return (0xff000000) | (rr << 16) | (gg << 8) | bb;
}

static INLINE jint interpolate4pointsNoAlpha(jint p00, jint p01, jint p10, jint p11,
                                      jint hfrac, jint vfrac) {
    jint r00 = (p00 >> 16) & 0xff;
    jint g00 = (p00 >> 8)  & 0xff;
    jint b00 =  p00        & 0xff;

    jint r01 = (p01 >> 16) & 0xff;
    jint g01 = (p01 >> 8)  & 0xff;
    jint b01 =  p01        & 0xff;

    jint r0 = interp(r00, r01, hfrac);
    jint g0 = interp(g00, g01, hfrac);
    jint b0 = interp(b00, b01, hfrac);

    jint r10 = (p10 >> 16) & 0xff;
    jint g10 = (p10 >> 8)  & 0xff;
    jint b10 =  p10        & 0xff;

    jint r11 = (p11 >> 16) & 0xff;
    jint g11 = (p11 >> 8)  & 0xff;
    jint b11 =  p11        & 0xff;

    jint r1 = interp(r10, r11, hfrac);
    jint g1 = interp(g10, g11, hfrac);
    jint b1 = interp(b10, b11, hfrac);

    jint rr = interp(r0, r1, vfrac);
    jint gg = interp(g0, g1, vfrac);
    jint bb = interp(b0, b1, vfrac);

    return (0xff000000) | (rr << 16) | (gg << 8) | bb;
}

static INLINE jboolean isInBounds(jint *a, jlong *la, jint min, jint max, jboolean repeat) {
    jboolean inBounds = XNI_TRUE;
    jint aval = *a;
    if (aval < min || aval > max) {
        if (repeat) {
            if (max > 0) {
                *la = lmod(*la, (max+1) << 16);
                *a = (jint)(*la >> 16);
            } else {
                *la = 0;
                *a = 0;
            }
        } else {
            inBounds = XNI_FALSE;
        }
    }
    return inBounds;
}

static INLINE void getPointsToInterpolate(jint *pts, jint *data, jint sidx, jint stride, jint p00,
    jint tx, jint txMin, jint txMax, jint ty, jint tyMin, jint tyMax)
{
    jint sidx2 = ((ty < tyMax) && (ty >= tyMin)) ? sidx + stride : sidx;
    jboolean isXin = ((tx < txMax) && (tx >= txMin));
    pts[0] = (isXin) ? data[sidx + 1] : p00;
    pts[1] = data[sidx2];
    pts[2] = (isXin) ? data[sidx2 + 1] : pts[0];
}

void
genTexturePaintTarget(Renderer *rdr, jint *paint, jint height) {
    jint j;
    jint minX, maxX;
    jint paintStride = rdr->_alphaWidth;
    jint firstRowNum = rdr->_rowNum;

    jint x, y;
    jint* txtData = rdr->_texture_intData;
    jint txtWidth = rdr->_texture_imageWidth;
    jint txtHeight = rdr->_texture_imageHeight;
    jint txMin = rdr->_texture_interpolateMinX;
    jint tyMin = rdr->_texture_interpolateMinY;
    jint txMax = rdr->_texture_interpolateMaxX;
    jint tyMax = rdr->_texture_interpolateMaxY;

    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;

    switch (rdr->_texture_transformType) {
    case TEXTURE_TRANSFORM_IDENTITY:
        {
        jint txtOffset = rdr->_alphaOffset;
        jint txtOffsetRepeat = 0;
        jint txtRowNumRepeat = 0;

        if (rdr->_texture_repeat) {
            txtOffsetRepeat = rdr->_currX % txtWidth;
            txtRowNumRepeat = rdr->_currY % txtHeight;
        }

        for (j = 0; j < height; j++) {
            if (rdr->_texture_repeat) {
                jint *tStart = txtData + rdr->_texture_stride * txtRowNumRepeat;
                jint *t = tStart + txtOffsetRepeat;
                jint *tEnd = tStart + txtWidth;
                jint *d = paint + paintStride * j;
                jint *dEnd = d + paintStride;
                while (d < dEnd) {
                    *d++ = *t++;
                    if (t == tEnd) {
                        t = tStart;
                    }
                }
                txtRowNumRepeat++;
                if (txtRowNumRepeat == txtHeight) {
                    txtRowNumRepeat = 0;
                }
            } else {
                memcpy(paint + paintStride * j, 
                     txtData + txtOffset + rdr->_texture_stride * (firstRowNum + j),
                     sizeof(jint) * paintStride);
            }
        }
        }
        break;

    // just TRANSLATION
    case TEXTURE_TRANSFORM_TRANSLATE:
        {
        jint cval, pidx;
        jint *a, *am;
        jlong ltx, lty;
        jint tx, ty, vfrac, hfrac;
        jboolean inBoundsY;
        jint paintOffset = 0;
        jint pts[3];

        y = rdr->_currY;

        if (rdr->_texture_hasAlpha == XNI_TRUE) {
            for (j = 0; j < height; j++, y++) {
                pidx = paintOffset;

                x = rdr->_currX;

                ltx = (x << 16) + rdr->_texture_m02;
                lty = (y << 16) + rdr->_texture_m12;

                // we can compute here since (m00 == 65536) && (m10 == 0)                    
                tx = (jint)(ltx >> 16);
                ty = (jint)(lty >> 16);
                hfrac = (jint)(ltx & 0xffff);
                vfrac = (jint)(lty & 0xffff);

                inBoundsY = isInBounds(&ty, &lty, tyMin-1, tyMax, rdr->_texture_repeat);

                a = paint + pidx;
                am = a + paintStride;
                while (a < am) {
                    jboolean inBounds = inBoundsY && isInBounds(&tx, &ltx,
                        txMin-1, txMax, rdr->_texture_repeat);

                    if (inBounds) {
                        jint sidx = MAX(tyMin, ty) * rdr->_texture_stride + MAX(txMin, tx);
                        jint p00 = txtData[sidx];
                        if (rdr->_texture_interpolate) {
                            getPointsToInterpolate(pts, txtData, sidx, rdr->_texture_stride, p00,
                                tx, txMin, txMax, ty, tyMin, tyMax);

                            if (hfrac && vfrac) {
                                cval = interpolate4points(p00, pts[0], pts[1], pts[2], hfrac, vfrac);
                            } else if (hfrac) {
                                cval = interpolate2points(p00, pts[0], hfrac);
                            } else if (vfrac) {
                                cval = interpolate2points(p00, pts[1], vfrac);
                            } else {
                                cval = p00;
                            }
                            assert(pidx >= 0);
                            assert(pidx < rdr->_paint_length);
                            paint[pidx] = cval;
                        } else {
                            assert(pidx >= 0);
                            assert(pidx < rdr->_paint_length);
                            paint[pidx] = p00;
                        }
                    } else {
                        assert(pidx >= 0);
                        assert(pidx < rdr->_paint_length);
                        paint[pidx] = 0x00000000;
                    }
                    ++a;
                    ++pidx;
                    ++tx;
                    ltx += 0x10000;
                } // while (a < am)
                paintOffset += paintStride;
            } // for
        } else { // alpha
            for (j = 0; j < height; j++, y++) {
                pidx = paintOffset;

                x = rdr->_currX;

                ltx = (x << 16) + rdr->_texture_m02;
                lty = (y << 16) + rdr->_texture_m12;

                // we can compute here since (m00 == 65536) && (m10 == 0)                    
                tx = (jint)(ltx >> 16);
                ty = (jint)(lty >> 16);
                hfrac = (jint)(ltx & 0xffff);
                vfrac = (jint)(lty & 0xffff);

                inBoundsY = isInBounds(&ty, &lty, tyMin-1, tyMax, rdr->_texture_repeat);

                a = paint + pidx;
                am = a + paintStride;
                while (a < am) {
                    jboolean inBounds = inBoundsY && isInBounds(&tx, &ltx,
                        txMin-1, txMax, rdr->_texture_repeat);

                    if (inBounds) {
                        jint sidx = MAX(tyMin, ty) * rdr->_texture_stride + MAX(txMin, tx);
                        jint p00 = txtData[sidx];
                        if (rdr->_texture_interpolate) {
                            getPointsToInterpolate(pts, txtData, sidx, rdr->_texture_stride, p00,
                                tx, txMin, txMax, ty, tyMin, tyMax);

                            if (hfrac && vfrac) {
                                cval = interpolate4pointsNoAlpha(p00, pts[0], pts[1], pts[2], hfrac, vfrac);
                            } else if (hfrac) {
                                cval = interpolate2pointsNoAlpha(p00, pts[0], hfrac);
                            } else if (vfrac) {
                                cval = interpolate2pointsNoAlpha(p00, pts[1], vfrac);
                            } else {
                                cval = p00;
                            }
                            assert(pidx >= 0);
                            assert(pidx < rdr->_paint_length);
                            paint[pidx] = cval;
                        } else {
                            assert(pidx >= 0);
                            assert(pidx < rdr->_paint_length);
                            paint[pidx] = p00;
                        }
                    } else {
                        assert(pidx >= 0);
                        assert(pidx < rdr->_paint_length);
                        paint[pidx] = 0x00000000;
                    }
                    ++a;
                    ++pidx;
                    ++tx;
                    ltx += 0x10000;
                } // while (a < am)
                paintOffset += paintStride;
            } // for
        }
        }
        break;

    // generic transform
    case TEXTURE_TRANSFORM_GENERIC:
        {
        jint cval, pidx;
        jint *a, *am;
        jlong ltx, lty;
        jint paintOffset = 0;
        jint pts[3];
        
        y = rdr->_currY;
        
        if (rdr->_texture_hasAlpha == XNI_TRUE) {
            for (j = 0; j < height; j++, y++) {
                pidx = paintOffset;

                x = rdr->_currX;

                ltx = x * rdr->_texture_m00 + y * rdr->_texture_m01 + rdr->_texture_m02;
                lty = x * rdr->_texture_m10 + y * rdr->_texture_m11 + rdr->_texture_m12;

                a = paint + pidx;
                am = a + paintStride;
                while (a < am) {
                    jint tx = (jint)(ltx >> 16);
                    jint ty = (jint)(lty >> 16);

                    jint hfrac = (jint)(ltx & 0xffff);
                    jint vfrac = (jint)(lty & 0xffff);

                    jboolean inBounds =
                        isInBounds(&tx, &ltx, txMin-1, txMax, rdr->_texture_repeat) &&
                        isInBounds(&ty, &lty, tyMin-1, tyMax, rdr->_texture_repeat);

                    if (inBounds) {
                        jint sidx = MAX(tyMin, ty) * rdr->_texture_stride + MAX(txMin, tx);
                        jint p00 = txtData[sidx];
                        if (rdr->_texture_interpolate) {
                            getPointsToInterpolate(pts, txtData, sidx, rdr->_texture_stride, p00,
                                tx, txMin, txMax, ty, tyMin, tyMax);

                            if (hfrac && vfrac) {
                                cval = interpolate4points(p00, pts[0], pts[1], pts[2], hfrac, vfrac);
                            } else if (hfrac) {
                                cval = interpolate2points(p00, pts[0], hfrac);
                            } else if (vfrac) {
                                cval = interpolate2points(p00, pts[1], vfrac);
                            } else {
                                cval = p00;
                            }

                            assert(pidx >= 0);
                            assert(pidx < rdr->_paint_length);

                            paint[pidx] = cval;
                        } else {
                            assert(pidx >= 0);
                            assert(pidx < rdr->_paint_length);

                            paint[pidx] = p00;
                        }
                    } else {
                        assert(pidx >= 0);
                        assert(pidx < rdr->_paint_length);

                        paint[pidx] = 0x00000000;
                    }
                    
                    ++a;
                    ++pidx;

                    ltx += rdr->_texture_m00;
                    lty += rdr->_texture_m10;
                } // while (a < am)b
                paintOffset += paintStride;
            }//for
        }//else _texture_hasAlpha == XNI_FALSE
        else {
            for (j = 0; j < height; j++, y++) {
                pidx = paintOffset;

                x = rdr->_currX;

                ltx = x * rdr->_texture_m00 + y * rdr->_texture_m01 + rdr->_texture_m02;
                lty = x * rdr->_texture_m10 + y * rdr->_texture_m11 + rdr->_texture_m12;

                a = paint + pidx;
                am = a + paintStride;
                while (a < am) {
                    jint tx = (jint)(ltx >> 16);
                    jint ty = (jint)(lty >> 16);

                    jint hfrac = (jint)(ltx & 0xffff);
                    jint vfrac = (jint)(lty & 0xffff);

                    jboolean inBounds = 
                        isInBounds(&tx, &ltx, txMin-1, txMax, rdr->_texture_repeat) &&
                        isInBounds(&ty, &lty, tyMin-1, tyMax, rdr->_texture_repeat);

                    if (inBounds) {
                        jint sidx = MAX(tyMin, ty) * rdr->_texture_stride + MAX(txMin, tx);
                        jint p00 = txtData[sidx];
                        if (rdr->_texture_interpolate) {
                            getPointsToInterpolate(pts, txtData, sidx, rdr->_texture_stride, p00,
                                tx, txMin, txMax, ty, tyMin, tyMax);

                            if (hfrac && vfrac) {
                                cval = interpolate4pointsNoAlpha(p00, pts[0], pts[1], pts[2], hfrac, vfrac);
                            } else if (hfrac) {
                                cval = interpolate2pointsNoAlpha(p00, pts[0], hfrac);
                            } else if (vfrac) {
                                cval = interpolate2pointsNoAlpha(p00, pts[1], vfrac);
                            } else {
                                cval = p00;
                            }

                            assert(pidx >= 0);
                            assert(pidx < rdr->_paint_length);

                            paint[pidx] = cval;
                        } else {
                            assert(pidx >= 0);
                            assert(pidx < rdr->_paint_length);

                            paint[pidx] = p00;
                        }
                    } else {
                        assert(pidx >= 0);
                        assert(pidx < rdr->_paint_length);

                        paint[pidx] = 0x00000000;
                    }

                    ++a;
                    ++pidx;

                    ltx += rdr->_texture_m00;
                    lty += rdr->_texture_m10;
                }
                paintOffset += paintStride;
            }
        }
        }
        break;
    }
}

void
genTexturePaint(Renderer *rdr, jint height) {
    genTexturePaintTarget(rdr, rdr->_paint, height);
}

void
genTexturePaintMultiply(Renderer *rdr, jint height) {
    jint i, j, idx;
    jint x_from = rdr->_minTouched;
    jint x_to = rdr->_maxTouched;
    jint w = (x_to - x_from + 1);
    jint *paint = rdr->_paint;
    jint paintStride = rdr->_alphaWidth;
    jint pval, tval, palpha_1;
    jint calpha = rdr->_calpha;
    jint cred = rdr->_cred;
    jint cgreen = rdr->_cgreen;
    jint cblue = rdr->_cblue;
    jint oalpha, ored, ogreen, oblue;

    switch (rdr->_prevPaintMode) {
    case PAINT_FLAT_COLOR:
        genTexturePaintTarget(rdr, paint, height);
        palpha_1 = calpha + 1;
        if (cred == 0xFF && cgreen == 0xFF && cblue == 0xFF) {
            if (calpha < 0xFF) {
                for (i = 0; i < height; i++) {
                    idx = i * paintStride;
                    for (j = 0; j < w; j++) {
                        tval = paint[idx + j];
                        oalpha = (palpha_1 * ((tval >> 24) & 0xFF)) >> 8;
                        ored = (palpha_1 * ((tval >> 16) & 0xFF)) >> 8;
                        ogreen = (palpha_1 * ((tval >> 8) & 0xFF)) >> 8;
                        oblue = (palpha_1 * (tval & 0xFF)) >> 8;
                        paint[idx + j] = (oalpha << 24) | (ored << 16) | (ogreen << 8) | oblue;
                    }
                }
            }
        } else {
            for (i = 0; i < height; i++) {
                idx = i * paintStride;
                for (j = 0; j < w; j++) {
                    tval = paint[idx + j];
                    oalpha = (palpha_1 * ((tval >> 24) & 0xFF)) >> 8;
                    ored = ((((cred + 1) * ((tval >> 16) & 0xFF)) >> 8) * palpha_1) >> 8;
                    ogreen = ((((cgreen + 1) * ((tval >> 8) & 0xFF)) >> 8) * palpha_1) >> 8;
                    oblue = ((((cblue + 1) * (tval & 0xFF)) >> 8) * palpha_1) >> 8;
                    paint[idx + j] = (oalpha << 24) | (ored << 16) | (ogreen << 8) | oblue;
                }
            }
        }
        break;
    case PAINT_LINEAR_GRADIENT:
    case PAINT_RADIAL_GRADIENT:
        {
        jint *imagePaint = my_malloc(jint, w * height);
        if (imagePaint != NULL) {
            if (rdr->_prevPaintMode == PAINT_LINEAR_GRADIENT) {
                genLinearGradientPaint(rdr, height);
            } else {
                genRadialGradientPaint(rdr, height);
            }
            genTexturePaintTarget(rdr, imagePaint, height);
            for (i = 0; i < height; i++) {
                idx = i * paintStride;
                for (j = 0; j < w; j++) {
                    pval = paint[idx + j];
                    tval = imagePaint[idx + j];
                    palpha_1 = ((pval >> 24) & 0xFF) + 1;
                    oalpha = (palpha_1 * ((tval >> 24) & 0xFF)) >> 8;
                    ored = ((((((pval >> 16) & 0xFF) + 1) * ((tval >> 16) & 0xFF)) >> 8) * palpha_1) >> 8;
                    ogreen = ((((((pval >> 8) & 0xFF) + 1) * ((tval >> 8) & 0xFF)) >> 8) * palpha_1) >> 8;
                    oblue = (((((pval & 0xFF) + 1) * (tval & 0xFF)) >> 8) * palpha_1) >> 8;
                    paint[idx + j] = (oalpha << 24) | (ored << 16) | (ogreen << 8) | oblue;
                }
            }
            my_free(imagePaint);
        }
        }
        break;
    }
}

