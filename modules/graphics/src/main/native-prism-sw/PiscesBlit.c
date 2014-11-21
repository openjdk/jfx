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

#include <PiscesBlit.h>

#include <PiscesUtil.h>
#include <PiscesRenderer.h>

#include <PiscesSysutils.h>
#include <PiscesMath.h>

#include <limits.h>

#define HALF_ALPHA (MAX_ALPHA >> 1)
#define ALPHA_SHIFT 8
#define HALF_1_SHIFT_23 (jint)(1L << 23) 

static jfloat currentGamma = -1;
static jint gammaArray[256];
static jint invGammaArray[256];

static INLINE void blendSrcOver8888_pre(jint *intData, jint aval, jint sred,
                                 jint sgreen, jint sblue);
static INLINE void blendSrcOver8888_pre_pre(jint *intData, jint frac,
                             jint aval,
                             jint sred, jint sgreen, jint sblue);
static INLINE void blendSrcOver8888_pre_pre_fullFrac(jint *intData, jint aval,
                             jint sred, jint sgreen, jint sblue);


static INLINE void blendLCDSrcOver8888_pre(jint *intData,
    jint ared, jint agreen, jint ablue, jint sred, jint sgreen, jint sblue);

static INLINE void blendSrc8888_pre(jint *intData, jint aval, jint raaval, jint sred,
                             jint sgreen, jint sblue);
static INLINE void blendSrc8888_pre_pre(jint *intData, jint aval, jint raaval, jint sred,
                             jint sgreen, jint sblue);

static INLINE jint div255(jint x) {
    return (x*257 + 257) >> 16;
}

static INLINE jint A(jint x) {
    return (x >> 24) & 0xFF;
}
static INLINE jint R(jint x) {
    return (x >> 16) & 0xFF;
}
static INLINE jint G(jint x) {
    return (x >> 8) & 0xFF;
}
static INLINE jint B(jint x) {
    return x & 0xFF;
}

void
emitLineSource8888_pre(Renderer *rdr, jint height, jint frac) {
    jint j, minX, maxX, w, iidx;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;

    jint *a, *am;

    jint calpha = rdr->_calpha;
    jint cred = rdr->_cred;
    jint cgreen = rdr->_cgreen;
    jint cblue = rdr->_cblue;
    jint alpha = (calpha * frac) >> 16;

    jint lfrac = rdr->_el_lfrac;
    jint rfrac = rdr->_el_rfrac;

    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = rdr->_alphaWidth;
    w -= (lfrac) ? 1 : 0;
    w -= (rfrac) ? 1 : 0;

    if (frac == 0x10000) { // full coverage
        jint pre_red = ((calpha+1) * cred) >> 8;
        jint pre_green = ((calpha+1) * cgreen) >> 8;
        jint pre_blue = ((calpha+1) * cblue) >> 8;
        jint pixel = (calpha << 24) | (pre_red << 16) | (pre_green << 8) | pre_blue;
        for (j = 0; j < height; j++) {
            iidx = imageOffset + minX * imagePixelStride;
            a = intData + iidx;
            if (lfrac) {
                blendSrc8888_pre(a, calpha, 255 - (lfrac >> 8), cred, cgreen, cblue);
                a += imagePixelStride;
            }
            am = a + w;
            while (a < am) {
                *a = pixel;
                a += imagePixelStride;
            }
            if (rfrac) {
                blendSrc8888_pre(a, calpha, 255 - (rfrac >> 8), cred, cgreen, cblue);
            }
            imageOffset += imageScanlineStride;
        }
    } else {
        jint comp_frac = 255 - (frac >> 8);
        jlong llfrac = (lfrac * (jlong)frac);
        jlong lrfrac = (rfrac * (jlong)frac);
        lfrac = (jint)(llfrac >> 16);
        rfrac = (jint)(lrfrac >> 16);
        for (j = 0; j < height; j++) {
            iidx = imageOffset + minX * imagePixelStride;
            a = intData + iidx;
            if (lfrac) {
                blendSrc8888_pre(a, calpha, 255 - (lfrac >> 8), cred, cgreen, cblue);
                a += imagePixelStride;
            }
            am = a + w;        
            while (a < am) {
                blendSrc8888_pre(a, calpha, comp_frac, cred, cgreen, cblue);
                a += imagePixelStride;
            }
            if (rfrac) {
                blendSrc8888_pre(a, calpha, 255 - (rfrac >> 8), cred, cgreen, cblue);
            }
            imageOffset += imageScanlineStride;
        }
    }
}

void
emitLinePTSource8888_pre(Renderer *rdr, jint height, jint frac) {
    jint j, minX, maxX, w, iidx, aidx;
    jint paint_offset = 0;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;

    jint* paint = rdr->_paint;
    jint cval, paint_stride;

    jint *a, *am;
    jint comp_frac = 255 - (frac >> 8);
    jlong llfrac = (rdr->_el_lfrac * (jlong)frac);
    jlong lrfrac = (rdr->_el_rfrac * (jlong)frac);
    jint lfrac = (jint)(llfrac >> 16);
    jint rfrac = (jint)(lrfrac >> 16);

    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    paint_stride = w = rdr->_alphaWidth;
    w -= (lfrac) ? 1 : 0;
    w -= (rfrac) ? 1 : 0;

    for (j = 0; j < height; j++) {
        aidx = paint_offset;
        iidx = imageOffset + minX * imagePixelStride;
        a = intData + iidx;
        if (lfrac) {
            cval = paint[aidx];
            blendSrc8888_pre_pre(a, A(cval), 255 - (lfrac >> 8), R(cval), G(cval), B(cval));
            a += imagePixelStride;
            aidx++;
        }
        am = a + w;
        if (frac == 0x10000) { // full coverage
            while (a < am) {
                *a = paint[aidx];
                a += imagePixelStride;
                aidx++;
            }
        } else {
            while (a < am) {
                cval = paint[aidx];
                blendSrc8888_pre_pre(a, A(cval), comp_frac, R(cval), G(cval), B(cval));
                a += imagePixelStride;
                aidx++;
            }
        }
        if (rfrac) {
            cval = paint[aidx];
            blendSrc8888_pre_pre(a, A(cval), 255 - (rfrac >> 8), R(cval), G(cval), B(cval));
        }
        imageOffset += imageScanlineStride;
        paint_offset += paint_stride;
    }
}

void
emitLineSourceOver8888_pre(Renderer *rdr, jint height, jint frac) {
    jint j, minX, maxX, w, iidx;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;

    jint *a, *am;

    jint calpha = rdr->_calpha;
    jint cred = rdr->_cred;
    jint cgreen = rdr->_cgreen;
    jint cblue = rdr->_cblue;
    jint alpha = (calpha * frac) >> 16;

    jint lfrac = rdr->_el_lfrac;
    jint rfrac = rdr->_el_rfrac;

    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = rdr->_alphaWidth;
    w -= (lfrac) ? 1 : 0;
    w -= (rfrac) ? 1 : 0;

    if (alpha == MAX_ALPHA) {
        jint solid_pixel =  0xFF000000 | (cred << 16) | (cgreen << 8) | cblue;
        for (j = 0; j < height; j++) {
            iidx = imageOffset + minX * imagePixelStride;
            a = intData + iidx;
            if (lfrac) {
                blendSrcOver8888_pre(a, lfrac >> 8, cred, cgreen, cblue);
                a += imagePixelStride;
            }
            am = a + w;
            while (a < am) {
                *a = solid_pixel;
                a += imagePixelStride;
            }
            if (rfrac) {
                blendSrcOver8888_pre(a, rfrac >> 8, cred, cgreen, cblue);
            }
            imageOffset += imageScanlineStride;
        }
    } else {
        jint lalpha = (lfrac * alpha) >> 16;
        jint ralpha = (rfrac * alpha) >> 16;
        for (j = 0; j < height; j++) {
            iidx = imageOffset + minX * imagePixelStride;
            a = intData + iidx;
            if (lfrac) {
                blendSrcOver8888_pre(a, lalpha, cred, cgreen, cblue);
                a += imagePixelStride;
            }
            am = a + w;        
            while (a < am) {
                blendSrcOver8888_pre(a, alpha, cred, cgreen, cblue);
                a += imagePixelStride;
            }
            if (rfrac) {
                blendSrcOver8888_pre(a, ralpha, cred, cgreen, cblue);
            }
            imageOffset += imageScanlineStride;
        }
    }
}

void
emitLinePTSourceOver8888_pre(Renderer *rdr, jint height, jint frac) {
    jint j, minX, maxX, w, iidx, aidx;
    jint paint_offset = 0;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;

    jint* paint = rdr->_paint;
    jint cval, palpha, paint_stride;

    jint *a, *am;
    jlong llfrac = (rdr->_el_lfrac * (jlong)frac);
    jlong lrfrac = (rdr->_el_rfrac * (jlong)frac);
    jint lfrac = (jint)(llfrac >> 16);
    jint rfrac = (jint)(lrfrac >> 16);

    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    paint_stride = w = rdr->_alphaWidth;
    w -= (lfrac) ? 1 : 0;
    w -= (rfrac) ? 1 : 0;

    for (j = 0; j < height; j++) {
        aidx = paint_offset;
        iidx = imageOffset + minX * imagePixelStride;
        a = intData + iidx;
        if (lfrac) {
            cval = paint[aidx];
            blendSrcOver8888_pre_pre(a, lfrac >> 8, A(cval), R(cval), G(cval), B(cval));
            a += imagePixelStride;
            aidx++;
        }
        am = a + w;
        if (frac == 0x10000) { // full coverage
            while (a < am) {
                cval = paint[aidx];
                palpha = A(cval);
                switch (palpha) {
                case 0:
                    break;
                case MAX_ALPHA:
                    *a = cval;
                    break;
                default:
                    blendSrcOver8888_pre_pre_fullFrac(a, palpha, R(cval), G(cval), B(cval));
                    break;
                }
                a += imagePixelStride;
                aidx++;
            }
        } else {
            while (a < am) {
                cval = paint[aidx];
                blendSrcOver8888_pre_pre(a, frac >> 8, A(cval), R(cval), G(cval), B(cval));
                a += imagePixelStride;
                aidx++;
            }
        }
        if (rfrac) {
            cval = paint[aidx];
            blendSrcOver8888_pre_pre(a, rfrac >> 8, A(cval), R(cval), G(cval), B(cval));
        }
        imageOffset += imageScanlineStride;
        paint_offset += paint_stride;
    }
}
/* EMIT LINES routines END */

void 
blitSrc8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint iidx, aval, acoverage;
    jint aval_relative;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jint *alpha = rdr->_rowAAInt;
    jint alphaOffset = 0;
    jint alphaStride = rdr->_alphaWidth;

    jint *a, *am;

    jint calpha = rdr->_calpha;
    jint cred = rdr->_cred;
    jint cgreen = rdr->_cgreen;
    jint cblue = rdr->_cblue;
    jbyte *alphaMap = rdr->alphaMap;

    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;
    
    for (j = 0; j < height; j++) {
        iidx = imageOffset + minX * imagePixelStride;
        
        aval_relative = 0;
        a = alpha;
        am = a + w;
        while (a < am) {
            aval_relative += *a;
            *a++ = 0;
            acoverage = alphaMap[aval_relative] & 0xff;
            if (acoverage == MAX_ALPHA) {
                intData[iidx] = (calpha << 24) | (cred << 16) | (cgreen << 8) | cblue;
            } else if (acoverage > 0) {
                aval = ((acoverage+1) * calpha) >> 8;
                blendSrc8888_pre(&intData[iidx], aval, 255 - acoverage,
                    cred, cgreen, cblue);
            }
            iidx += imagePixelStride;
        }

        imageOffset += imageScanlineStride;
        alphaOffset += alphaStride;
    }
}

void
blitSrcMask8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint iidx, aval, acoverage;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jbyte *alpha = rdr->_mask_byteData;
    jint alphaOffset = rdr->_maskOffset;
    jint alphaStride = rdr->_alphaWidth;

    jbyte *a, *am;

    jint calpha = rdr->_calpha;
    jint cred = rdr->_cred;
    jint cgreen = rdr->_cgreen;
    jint cblue = rdr->_cblue;
    
    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;

    for (j = 0; j < height; j++) {
        iidx = imageOffset + minX * imagePixelStride;
        
        a = alpha + alphaOffset;
        am = a + w;
        while (a < am) {
            acoverage = *a++ & 0xff;
            // run in integers otherwise it overflows
            if (acoverage == MAX_ALPHA) {
                intData[iidx] = (calpha << 24) | (cred << 16) | (cgreen << 8) | cblue;
            } else if (acoverage > 0) {
                aval = ((acoverage+1) * calpha) >> 8;
                blendSrc8888_pre(&intData[iidx], aval, 255 - acoverage,
                    cred, cgreen, cblue);
            }
            iidx += imagePixelStride;
        }
        
        imageOffset += imageScanlineStride;
        alphaOffset += alphaStride;
    }
}

void 
blitPTSrc8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint cval, aidx, iidx, aval, acoverage;
    jint aval_relative;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jint *alpha = rdr->_rowAAInt;

    jint *a, *am;

    jbyte *alphaMap = rdr->alphaMap;
    
    jint* paint = rdr->_paint;
    jint palpha;
    
    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;

    for (j = 0; j < height; j++) {
        aidx = 0;
        iidx = imageOffset + minX * imagePixelStride;
        
        aval_relative = 0;
        a = alpha;
        am = a + w;
        while (a < am) {
            assert(aidx >= 0);
            assert(aidx < rdr->_paint_length);

            cval = paint[aidx];
            palpha = A(cval);

            aval_relative += *a;
            *a++ = 0;
            acoverage = alphaMap[aval_relative] & 0xff;

            if (acoverage == MAX_ALPHA) {
                intData[iidx] = cval;
            } else if (acoverage > 0) {
                aval = ((acoverage+1) * palpha) >> 8;
                blendSrc8888_pre_pre(&intData[iidx], aval, 255 - acoverage, R(cval), G(cval), B(cval));
            }
            iidx += imagePixelStride;
            ++aidx;
        }

        imageOffset += imageScanlineStride;
    }
}

void
blitPTSrcMask8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint cval, aidx, iidx, aval, acoverage;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jbyte *alpha = rdr->_mask_byteData;
    jint alphaOffset = rdr->_maskOffset;

    jbyte *a, *am;

    jint* paint = rdr->_paint;
    jint palpha;
    
    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;

    for (j = 0; j < height; j++) {
        aidx = 0;
        iidx = imageOffset + minX * imagePixelStride;
        
        a = alpha + alphaOffset;
        am = a + w;
        while (a < am) {
            cval = paint[aidx];
            palpha = A(cval);

            acoverage = *a++ & 0xff;

            if (acoverage == MAX_ALPHA) {
                intData[iidx] = cval;
            } else if (acoverage > 0) {
                aval = ((acoverage+1) * palpha) >> 8;
                blendSrc8888_pre_pre(&intData[iidx], aval, 255 - acoverage, R(cval), G(cval), B(cval));
            }
            iidx += imagePixelStride;
            ++aidx;
        }

        imageOffset += imageScanlineStride;
    }
}

void
blitSrcOver8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint  iidx, aval;
    jint aval_relative;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jint *alpha = rdr->_rowAAInt;
    jint alphaOffset = 0;
    jint alphaStride = rdr->_alphaWidth;

    jint *a, *am;

    jint calpha = rdr->_calpha;
    jint cred = rdr->_cred;
    jint cgreen = rdr->_cgreen;
    jint cblue = rdr->_cblue;
    jbyte *alphaMap = rdr->alphaMap;
    
    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;

    for (j = 0; j < height; j++) {
        iidx = imageOffset + minX * imagePixelStride;
        
        aval_relative = 0;
        a = alpha;
        am = a + w;
        while (a < am) {
            aval_relative += *a;
            *a++ = 0;
            if (aval_relative) {
                aval = alphaMap[aval_relative] & 0xff;
                aval = ((aval+1) * calpha) >> 8;
                if (aval == MAX_ALPHA) {
                    intData[iidx] = 0xff000000 | (cred << 16) | (cgreen << 8) | cblue;
                } else if (aval > 0) {
                    blendSrcOver8888_pre(&intData[iidx], aval, cred, cgreen, cblue);
                }
            }
            iidx += imagePixelStride;
        }

        imageOffset += imageScanlineStride;
        alphaOffset += alphaStride;
    }
}

void
blitSrcOverMask8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint iidx, aval;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jbyte *alpha = rdr->_mask_byteData;
    jint alphaOffset = rdr->_maskOffset;
    jint alphaStride = rdr->_alphaWidth;

    jbyte *a, *am;

    jint calpha = rdr->_calpha;
    jint cred = rdr->_cred;
    jint cgreen = rdr->_cgreen;
    jint cblue = rdr->_cblue;
    
    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;

    for (j = 0; j < height; j++) {
        iidx = imageOffset + minX * imagePixelStride;
        
        a = alpha + alphaOffset;
        am = a + w;
        while (a < am) {
            if (*a) {
                aval = *a & 0xff;
                // run in integers otherwise it overflows
                aval = ((aval+1) * calpha) >> 8;
                if (aval == MAX_ALPHA) {
                    intData[iidx] = 0xff000000 | (cred << 16) | (cgreen << 8) | cblue;
                } else if (aval > 0) {
                    blendSrcOver8888_pre(&intData[iidx], aval, cred, cgreen, cblue);
                }
            }
            a++;
            iidx += imagePixelStride;
        }
        
        imageOffset += imageScanlineStride;
        alphaOffset += alphaStride;
    }
}

void
blitSrcOverLCDMask8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint iidx, aval_ismax, ared, agreen, ablue;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jbyte *alpha = rdr->_mask_byteData;
    jint alphaOffset = rdr->_maskOffset;
    jint alphaStride = rdr->_alphaWidth;

    jbyte *a, *am;

    jint calpha = invGammaArray[rdr->_calpha];
    jint cred = invGammaArray[rdr->_cred];
    jint cgreen = invGammaArray[rdr->_cgreen];
    jint cblue = invGammaArray[rdr->_cblue];

    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;

    for (j = 0; j < height; j++) {
        iidx = imageOffset + minX * imagePixelStride;

        a = alpha + alphaOffset;
        am = a + 3*w;
        while (a < am) {
            ared = *a++ & 0xff;
            agreen = *a++ & 0xff;
            ablue = *a++ & 0xff;
            if (calpha < MAX_ALPHA) {
                ared = ((ared+1) * calpha) >> 8;
                agreen = ((agreen+1) * calpha) >> 8;
                ablue = ((ablue+1) * calpha) >> 8;
            }
            aval_ismax = ared & agreen & ablue;            
            if (aval_ismax == MAX_ALPHA) {
                intData[iidx] = 0xff000000 | (cred << 16) | (cgreen << 8) | cblue;
            } else {
                blendLCDSrcOver8888_pre(&intData[iidx], ared, agreen, ablue,
                    cred, cgreen, cblue);
            }
            iidx += imagePixelStride;
        }

        imageOffset += imageScanlineStride;
        alphaOffset += alphaStride;
    }
}

void
blitPTSrcOver8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint cval, aidx, iidx, aval;
    jint aval_relative;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jint *alpha = rdr->_rowAAInt;

    jint *a, *am;

    jbyte *alphaMap = rdr->alphaMap;
    
    jint* paint = rdr->_paint;
    jint palpha, malpha;
    
    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;

    for (j = 0; j < height; j++) {
        aidx = 0;
        iidx = imageOffset + minX * imagePixelStride;
        
        aval_relative = 0;
        a = alpha;
        am = a + w;
        while (a < am) {
            assert(aidx >= 0);
            assert(aidx < rdr->_paint_length);

            cval = paint[aidx];
            palpha = A(cval);
            
            aval_relative += *a;
            *a++ = 0;
            if (aval_relative) {
                malpha = alphaMap[aval_relative] & 0xff;
                aval = ((malpha+1) * palpha) >> 8;
                
                if (aval == MAX_ALPHA) {
                    intData[iidx] = cval;
                } else if (aval > 0) {
                    blendSrcOver8888_pre_pre(&intData[iidx], malpha+1, palpha, R(cval), G(cval), B(cval));
                }
            }
            iidx += imagePixelStride;
            ++aidx;
        }

        imageOffset += imageScanlineStride;
    }
}

void
blitPTSrcOverMask8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint cval, aidx, iidx, aval;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jbyte *alpha = rdr->_mask_byteData;
    jint alphaOffset = rdr->_maskOffset;

    jbyte *a, *am;

    jint* paint = rdr->_paint;
    jint palpha, malpha;
    
    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;

    for (j = 0; j < height; j++) {
        aidx = 0;
        iidx = imageOffset + minX * imagePixelStride;
        
        a = alpha + alphaOffset;
        am = a + w;
        while (a < am) {
            if (*a) {
                cval = paint[aidx];
                palpha = A(cval);
            
                malpha = *a & 0xff;
                aval = ((malpha+1) * palpha) >> 8;
                
                if (aval == MAX_ALPHA) {
                    intData[iidx] = cval;
                } else if (aval > 0) {
                    blendSrcOver8888_pre_pre(&intData[iidx], malpha+1, palpha, R(cval), G(cval), B(cval));
                }
            }
            a++;
            iidx += imagePixelStride;
            ++aidx;
        }
        
        imageOffset += imageScanlineStride;
    }
}

void
clearRect8888_any(Renderer *rdr, jint x, jint y, jint w, jint h) {
    jint cval = (rdr->_calpha << 24) | (rdr->_cred << 16) | 
                (rdr->_cgreen << 8) | rdr->_cblue;
    jint pixelStride = rdr->_imagePixelStride;
    //jint scanlineSkip = rdr->_imageScanlineStride - w * pixelStride;
    jint* intData = (jint*)rdr->_data + rdr->_imageOffset +
                    y * rdr->_imageScanlineStride + x * pixelStride;
    jint* intData2 = intData;
    jint* intData2End = intData + w;
    
    if (cval == 0) {
        int size = sizeof(jint) * w;
        if (x == 0 && w == rdr->_width) {
            //printf("full clear 8888 ZERO, x: %d, y: %d, w: %d, h: %d\n", x, y, w, h);
            memset(intData, 0, size * h);
        } else {
            //printf("part clear 8888 ZERO, x: %d, y: %d, w: %d, h: %d\n", x, y, w, h);
            for (; h > 0; --h) {
                memset(intData, 0, size);
                intData += rdr->_imageScanlineStride;
            }
        }    
    } else {
        //printf("clear 8888, x: %d, y: %d, w: %d, h: %d\n", x, y, w, h);
        int size = sizeof(jint) * w;
        //set first scanline to cval
        while(intData2 < intData2End) {
            *intData2++ = cval;
        }
        //set to starting pixel again
        intData2 = intData;
        //set to starting pixel in the second row
        intData += rdr->_imageScanlineStride;
        for (h--; h > 0; --h) {
            memcpy(intData, intData2, size);
            intData += rdr->_imageScanlineStride;
        }
    }
    //fflush(stdout);    
}

// *intData are premultiplied, sred, sgreen, sblue are non-premultiplied
static void
blendSrcOver8888_pre(jint *intData,
                             jint aval,
                             jint sred, jint sgreen, jint sblue) {
    jint ival = *intData;
    //destination alpha
    jint dalpha = (ival >> 24) & 0xff;
    //destination components premultiplied by dalpha
    jint dred = (ival >> 16) & 0xff;
    jint dgreen = (ival >> 8) & 0xff;
    jint dblue = ival & 0xff;
    
    jint oneminusaval = (255 - aval);
    
    jint oalpha  = div255(255 * aval    + oneminusaval * dalpha);
    jint ored    = div255(sred * aval   + oneminusaval * dred);
    jint ogreen  = div255(sgreen * aval + oneminusaval * dgreen);
    jint oblue   = div255(sblue * aval  + oneminusaval * dblue);
    
    *intData = (oalpha << 24) | (ored << 16) | (ogreen << 8) | oblue;
}

// *intData are premultiplied, sred, sgreen, sblue are premultiplied
static void
blendSrcOver8888_pre_pre(jint *intData, jint frac,
                             jint aval,
                             jint sred, jint sgreen, jint sblue) {
    jint ival = *intData;
    //destination alpha
    jint dalpha = (ival >> 24) & 0xff;
    //destination components premultiplied by dalpha
    jint dred = (ival >> 16) & 0xff;
    jint dgreen = (ival >> 8) & 0xff;
    jint dblue = ival & 0xff;

    jint aval2 = (aval * frac) >> 8;
    jint oneminusaval = (255 - aval2);

    jint oalpha  = aval2                  + div255(oneminusaval * dalpha);
    jint ored    = ((sred * frac) >> 8)   + div255(oneminusaval * dred);
    jint ogreen  = ((sgreen * frac) >> 8) + div255(oneminusaval * dgreen);
    jint oblue   = ((sblue * frac) >> 8)  + div255(oneminusaval * dblue);

    *intData = (oalpha << 24) | (ored << 16) | (ogreen << 8) | oblue;
}

// *intData are premultiplied, sred, sgreen, sblue are premultiplied
static void
blendSrcOver8888_pre_pre_fullFrac(jint *intData, jint aval,
                             jint sred, jint sgreen, jint sblue) {
    jint ival = *intData;
    //destination alpha
    jint dalpha = (ival >> 24) & 0xff;
    //destination components premultiplied by dalpha
    jint dred = (ival >> 16) & 0xff;
    jint dgreen = (ival >> 8) & 0xff;
    jint dblue = ival & 0xff;

    jint oneminusaval = (255 - aval);

    jint oalpha  = aval   + div255(oneminusaval * dalpha);
    jint ored    = sred   + div255(oneminusaval * dred);
    jint ogreen  = sgreen + div255(oneminusaval * dgreen);
    jint oblue   = sblue  + div255(oneminusaval * dblue);

    *intData = (oalpha << 24) | (ored << 16) | (ogreen << 8) | oblue;
}

// *intData are premultiplied, sred, sgreen, sblue are NOT premultiplied
// it is required that final alpha must be fully opaque (0xFF)
static void
blendLCDSrcOver8888_pre(jint *intData,
                             jint ared, jint agreen, jint ablue,
                             jint sred, jint sgreen, jint sblue)
{
    jint ival = *intData;
    //destination alpha
    jint dalpha = (ival >> 24) & 0xff;
    //destination components premultiplied by dalpha
    jint dred = (ival >> 16) & 0xff;
    jint dgreen = (ival >> 8) & 0xff;
    jint dblue = ival & 0xff;

    jint ored, ogreen, oblue;

    dred = invGammaArray[dred];
    dgreen = invGammaArray[dgreen];
    dblue = invGammaArray[dblue];

    ored    = div255(ared * sred     + (255 - ared) * dred);
    ogreen  = div255(agreen * sgreen + (255 - agreen) * dgreen);
    oblue   = div255(ablue * sblue   + (255 - ablue) * dblue);

    ored = gammaArray[ored];
    ogreen = gammaArray[ogreen];
    oblue = gammaArray[oblue];

    *intData = 0xFF000000 | (ored << 16) | (ogreen << 8) | oblue;
}

static void
blendSrc8888_pre(jint *intData,
                 jint aval, jint raaval,
                 jint sred, jint sgreen, jint sblue) {
    jint denom;

    jint ival = *intData;
    jint dalpha = (ival >> 24) & 0xff;
    //premultiplied color components
    jint dred =   (ival >> 16) & 0xff;
    jint dgreen = (ival >>  8) & 0xff;
    jint dblue =  (ival & 0xff);

    denom = 255 * aval + dalpha * raaval;
    if (denom == 0) {
        // The output is transparent black
        *intData = 0x00000000;
    } else {
        jint oalpha, ored, ogreen, oblue;
        oalpha  = div255(denom);
        ored    = div255(aval * sred   + raaval * dred);
        ogreen  = div255(aval * sgreen + raaval * dgreen);
        oblue   = div255(aval * sblue  + raaval * dblue);
        
        ival = (oalpha << 24) | (ored << 16) | (ogreen << 8) | oblue;
        *intData = ival;
    }
}

// sred, sgreen, sblue are all premultiplied
static void
blendSrc8888_pre_pre(jint *intData,
                 jint aval, jint raaval,
                 jint sred, jint sgreen, jint sblue) {
    jint denom;

    jint ival = *intData;
    jint dalpha = (ival >> 24) & 0xff;
    //premultiplied color components
    jint dred =   (ival >> 16) & 0xff;
    jint dgreen = (ival >>  8) & 0xff;
    jint dblue =  (ival & 0xff);

    denom = 255 * aval + dalpha * raaval;
    if (denom == 0) {
        // The output is transparent black
        *intData = 0x00000000;
    } else {
        jint oalpha, ored, ogreen, oblue;
        oalpha  = div255(denom);
        ored    = sred   + div255(raaval * dred);
        ogreen  = sgreen + div255(raaval * dgreen);
        oblue   = sblue  + div255(raaval * dblue);

        ival = (oalpha << 24) | (ored << 16) | (ogreen << 8) | oblue;
        *intData = ival;
    }
}

void initGammaArrays(jfloat gamma) {
    if (currentGamma != gamma) {
        int i;
        jfloat invgamma = 1.0f / gamma;
        currentGamma = gamma;
        for (i = 0; i < 256; i++) {
            gammaArray[i] = (jint)(255 * pow(i/255.0, gamma));
            invGammaArray[i] = (jint)(255 * pow(i/255.0, invgamma));
        }
    }
}

