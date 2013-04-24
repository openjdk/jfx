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

static INLINE void blendSrcOver8888(jint *intData, jint aval,
                             jint sred, jint sgreen, jint sblue);
                             
static INLINE void blendSrcOver8888_pre(jint *intData, jint aval, jint sred, 
                                 jint sgreen, jint sblue);

static INLINE void blendLCDSrcOver8888_pre(jint *intData,
    jint ared, jint agreen, jint ablue, jint sred, jint sgreen, jint sblue,
    jfloat gamma, jfloat invgamma);

static INLINE void blendSrc8888(jint *intData, jint aval, jint aaval,
                         jint sred, jint sgreen, jint sblue);
                         
static INLINE void blendSrc8888_pre(jint *intData, jint aval, jint raaval, jint sred, 
                             jint sgreen, jint sblue);

static INLINE jint div255(jint x) {
    return (x*257 + 257) >> 16;
}

/* EMIT LINES routines - used by PiscesRenderer.fillRect(...) function */
void
emitLineSource8888(Renderer *rdr, jint height, jint frac) {
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
        jint solid_pixel =  0xFF000000 | (cred << 16) | (cgreen << 8) | cblue;
        for (j = 0; j < height; j++) {
            iidx = imageOffset + minX * imagePixelStride;
            a = intData + iidx;
            if (lfrac) {
                blendSrc8888(a, calpha, 255 - (lfrac >> 8), cred, cgreen, cblue);
                a += imagePixelStride;
            }
            am = a + w;
            while (a < am) {
                *a = solid_pixel;
                a += imagePixelStride;
            }
            if (rfrac) {
                blendSrc8888(a, calpha, 255 - (rfrac >> 8), cred, cgreen, cblue);
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
                blendSrc8888(a, calpha, 255 - (lfrac >> 8), cred, cgreen, cblue);
                a += imagePixelStride;
            }
            am = a + w;
            while (a < am) {
                blendSrc8888(a, calpha, comp_frac, cred, cgreen, cblue);
                a += imagePixelStride;
            }
            if (rfrac) {
                blendSrc8888(a, calpha, 255 - (rfrac >> 8), cred, cgreen, cblue);
            }
            imageOffset += imageScanlineStride;
        }
    }
}

void
emitLinePTSource8888(Renderer *rdr, jint height, jint frac) {
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
            blendSrc8888(a, (cval >> 24) & 0xFF, 255 - (lfrac >> 8),
                (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
            a += imagePixelStride;
            aidx++;
        }
        am = a + w;        
        while (a < am) {
            cval = paint[aidx];
            if (frac == 0x10000) { // full coverage
                *a = cval;
            } else {
                blendSrc8888(a, (cval >> 24) & 0xFF, comp_frac,
                    (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
            }
            a += imagePixelStride;
            aidx++;
        }
        if (rfrac) {
            cval = paint[aidx];
            blendSrc8888(a, (cval >> 24) & 0xFF, 255 - (rfrac >> 8),
                (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
        }
        imageOffset += imageScanlineStride;
        paint_offset += paint_stride;
    }
}

void
emitLineSourceOver8888(Renderer *rdr, jint height, jint frac) {
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
                blendSrcOver8888(a, lfrac >> 8, cred, cgreen, cblue);
                a += imagePixelStride;
            }
            am = a + w;
            while (a < am) {
                *a = solid_pixel;
                a += imagePixelStride;
            }
            if (rfrac) {
                blendSrcOver8888(a, rfrac >> 8, cred, cgreen, cblue);
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
                blendSrcOver8888(a, lalpha, cred, cgreen, cblue);
                a += imagePixelStride;
            }
            am = a + w;        
            while (a < am) {
                blendSrcOver8888(a, alpha, cred, cgreen, cblue);
                a += imagePixelStride;
            }
            if (rfrac) {
                blendSrcOver8888(a, ralpha, cred, cgreen, cblue);
            }
            imageOffset += imageScanlineStride;
        }
    }
}

void
emitLinePTSourceOver8888(Renderer *rdr, jint height, jint frac) {
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
            palpha = (lfrac * ((cval >> 24) & 0xFF)) >> 16;
            blendSrcOver8888(a, palpha, (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
            a += imagePixelStride;
            aidx++;
        }
        am = a + w;        
        while (a < am) {
            cval = paint[aidx];
            palpha = (frac * ((cval >> 24) & 0xFF)) >> 16;
            if (palpha == MAX_ALPHA) {
                *a = cval;
            } else {
                blendSrcOver8888(a, palpha, (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
            }
            a += imagePixelStride;
            aidx++;
        }
        if (rfrac) {
            cval = paint[aidx];
            palpha = (rfrac * ((cval >> 24) & 0xFF)) >> 16;
            blendSrcOver8888(a, palpha, (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
        }
        imageOffset += imageScanlineStride;
        paint_offset += paint_stride;
    }
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
        jint solid_pixel =  0xFF000000 | (cred << 16) | (cgreen << 8) | cblue;
        for (j = 0; j < height; j++) {
            iidx = imageOffset + minX * imagePixelStride;
            a = intData + iidx;
            if (lfrac) {
                blendSrc8888_pre(a, calpha, 255 - (lfrac >> 8), cred, cgreen, cblue);
                a += imagePixelStride;
            }
            am = a + w;
            while (a < am) {
                *a = solid_pixel;
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
            blendSrc8888_pre(a, (cval >> 24) & 0xFF, 255 - (lfrac >> 8),
                (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
            a += imagePixelStride;
            aidx++;
        }
        am = a + w;        
        while (a < am) {
            cval = paint[aidx];
            if (frac == 0x10000) { // full coverage
                *a = cval;
            } else {
                blendSrc8888_pre(a, (cval >> 24) & 0xFF, comp_frac,
                    (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
            }
            a += imagePixelStride;
            aidx++;
        }
        if (rfrac) {
            cval = paint[aidx];
            blendSrc8888_pre(a, (cval >> 24) & 0xFF, 255 - (rfrac >> 8),
                (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
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
            palpha = (lfrac * ((cval >> 24) & 0xFF)) >> 16;
            blendSrcOver8888_pre(a, palpha, (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
            a += imagePixelStride;
            aidx++;
        }
        am = a + w;        
        while (a < am) {
            cval = paint[aidx];
            palpha = (frac * ((cval >> 24) & 0xFF)) >> 16;
            if (palpha == MAX_ALPHA) {
                *a = cval;
            } else {
                blendSrcOver8888_pre(a, palpha, (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
            }
            a += imagePixelStride;
            aidx++;
        }
        if (rfrac) {
            cval = paint[aidx];
            palpha = (rfrac * ((cval >> 24) & 0xFF)) >> 16;
            blendSrcOver8888_pre(a, palpha, (cval >> 16) & 0xFF, (cval >> 8) & 0xFF, cval & 0xFF);
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
blitSrc8888(Renderer *rdr, jint height) {
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
                blendSrc8888(&intData[iidx], aval, 255 - acoverage,
                    cred, cgreen, cblue);
            }
            iidx += imagePixelStride;
        }

        imageOffset += imageScanlineStride;
        alphaOffset += alphaStride;
    }
}

void 
blitPTSrc8888(Renderer *rdr, jint height) {
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
            palpha = (cval >> 24) & 0xff;
            
            aval_relative += *a;
            *a++ = 0;
            acoverage = alphaMap[aval_relative] & 0xff;
            
            if (acoverage == MAX_ALPHA) {
                intData[iidx] = cval;
            } else if (acoverage > 0) {
                aval = ((acoverage+1) * palpha) >> 8;
                blendSrc8888(&intData[iidx], aval, 255 - acoverage,
                    (cval >> 16) & 0xff, (cval >> 8) & 0xff, cval & 0xff);
            }
            iidx += imagePixelStride;
            ++aidx;
        }
        imageOffset += imageScanlineStride;
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
            palpha = (cval >> 24) & 0xff;
            
            aval_relative += *a;
            *a++ = 0;
            acoverage = alphaMap[aval_relative] & 0xff;
            
            if (acoverage == MAX_ALPHA) {
                intData[iidx] = cval;
            } else if (acoverage > 0) {
                aval = ((acoverage+1) * palpha) >> 8;
                blendSrc8888_pre(&intData[iidx], aval, 255 - acoverage,
                    (cval >> 16) & 0xff, (cval >> 8) & 0xff, cval & 0xff);
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
            palpha = (cval >> 24) & 0xff;
            
            acoverage = *a++ & 0xff;
            
            if (acoverage == MAX_ALPHA) {
                intData[iidx] = cval;
            } else if (acoverage > 0) {
                aval = ((acoverage+1) * palpha) >> 8;
                blendSrc8888_pre(&intData[iidx], aval, 255 - acoverage,
                    (cval >> 16) & 0xff, (cval >> 8) & 0xff, cval & 0xff);
            }
            iidx += imagePixelStride;
            ++aidx;
        }
        
        imageOffset += imageScanlineStride;
    }
}

void
blitImageSrc8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w, scanA;
    jint cval, aidx, iidx, aval, acoverage;

    jint paintOffset = 0;
    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jint alphaStride = rdr->_alphaWidth;
    jint textureStride = rdr->_texture_stride;
    jint *scanLineAlpha = rdr->_scanLineAlpha;

    jint* paint = rdr->_paint;
    jint pre_rval, pre_gval, pre_bval;
    
    jint am;
    
    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;
    
    aidx = 0;
    for (j = 0; j < height; j++) {
        scanA = scanLineAlpha[j];
      
        iidx = imageOffset + minX * imagePixelStride;
        
        am = aidx + w;
        while (aidx < am) {
            assert(aidx >= 0);
            assert(aidx < rdr->_paint_length);

            cval = paint[aidx];
            acoverage = aval = (cval >> 24) & 0xff;
      
            if (scanA < MAX_ALPHA) {
                aval = ((aval+1) * scanA) >> 8;
            }
            
            if (aval == MAX_ALPHA) {
                intData[iidx] = cval;
            } else if (acoverage > 0) {
                pre_rval = (cval >> 16) & 0xff;
                pre_gval = (cval >> 8) & 0xff;
                pre_bval = cval & 0xff;
                blendSrc8888_pre(&intData[iidx], aval, 255 - acoverage, 
                    (pre_rval * 255)/acoverage,
                    (pre_gval * 255)/acoverage,
                    (pre_bval * 255)/acoverage);
            }
            iidx += imagePixelStride;
            ++aidx;
        }
        imageOffset += imageScanlineStride;
        paintOffset += textureStride;
    }
}

void
blitSrcOver8888(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint cval, iidx, aval;
    jint aval_relative;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jint *alpha = rdr->_rowAAInt;
    jint alphaOffset = 0;
    jint alphaStride = rdr->_alphaWidth;
    jint width = rdr->_alphaWidth;

    jint *a, *am;

    jint calpha = rdr->_calpha;
    jint cred = rdr->_cred;
    jint cgreen = rdr->_cgreen;
    jint cblue = rdr->_cblue;
    jbyte *alphaMap = rdr->alphaMap;

    cval = (calpha << 24) | (cred << 16) | (cgreen << 8) | cblue;
    
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
            aval = alphaMap[aval_relative] & 0xff;
            aval = ((aval+1) * calpha) >> 8;
            if (aval == MAX_ALPHA) {
                intData[iidx] = cval;
            } else if (aval > 0) {
                blendSrcOver8888(&intData[iidx], aval, cred, cgreen, cblue);
            }
            iidx += imagePixelStride;
        }

        imageOffset += imageScanlineStride;
        alphaOffset += alphaStride;
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
    jint subPosXL = rdr->_mask_subPosX >> 2;
    jint subPosXR = rdr->_mask_subPosX & 3;

    jbyte *a, *am;

    jint calpha = rdr->_calpha;
    jint cred = rdr->_cred;
    jint cgreen = rdr->_cgreen;
    jint cblue = rdr->_cblue;

    jfloat gamma = rdr->_gamma;
    jfloat invgamma = rdr->_invgamma;

    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;

    for (j = 0; j < height; j++) {
        iidx = imageOffset + minX * imagePixelStride;

        a = alpha + alphaOffset;
        am = a + 3*w - (subPosXR ? 3 : 0); // right subpixel - remove from loop

        // check for left subpixel
        if (subPosXL) {
            switch (subPosXL) {
            case 1:
                ared = 0;
                agreen = *a++ & 0xff;
                ablue = *a++ & 0xff;
                break;
            case 2:
                ared = agreen = 0;
                ablue = *a++ & 0xff;
                break;
            }
            if (calpha < MAX_ALPHA) {
                ared = ((ared+1) * calpha) >> 8;
                agreen = ((agreen+1) * calpha) >> 8;
                ablue = ((ablue+1) * calpha) >> 8;
            }
            blendLCDSrcOver8888_pre(&intData[iidx], ared, agreen, ablue,
                cred, cgreen, cblue, gamma, invgamma);
            iidx += imagePixelStride;
        }

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
                    cred, cgreen, cblue,
                    gamma, invgamma);
            }
            iidx += imagePixelStride;
        }

        // check for right subpixel
        if (subPosXR) {
            switch (subPosXR) {
            case 1:
                ared = *a++ & 0xff;;
                agreen = ablue = 0;
                break;
            case 2:
                ared = *a++ & 0xff;
                agreen = *a++ & 0xff;;
                ablue = 0;
                break;
            }
            if (calpha < MAX_ALPHA) {
                ared = ((ared+1) * calpha) >> 8;
                agreen = ((agreen+1) * calpha) >> 8;
                ablue = ((ablue+1) * calpha) >> 8;
            }
            blendLCDSrcOver8888_pre(&intData[iidx], ared, agreen, ablue,
                cred, cgreen, cblue, gamma, invgamma);
            iidx += imagePixelStride;
        }

        imageOffset += imageScanlineStride;
        alphaOffset += alphaStride;
    }
}

void
blitPTSrcOver8888(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w;
    jint cval, aidx, iidx, aval;
    jint aval_relative;

    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jint *alpha = rdr->_rowAAInt;
    jint alphaStride = rdr->_alphaWidth;

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
            palpha = (cval >> 24) & 0xff;
            
            aval_relative += *a;
            *a++ = 0;
            aval = alphaMap[aval_relative] & 0xff;
            aval = ((aval+1) * palpha) >> 8;
            
            if (aval == MAX_ALPHA) {
                intData[iidx] = cval;
            } else if (aval > 0) {
                blendSrcOver8888(&intData[iidx], aval, (cval >> 16) & 0xff,
                                 (cval >> 8) & 0xff, cval & 0xff);
            }
            iidx += imagePixelStride;
            ++aidx;
        }

        imageOffset += imageScanlineStride;
    }
}

void
blitImageSrcOver8888(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w, scanA;
    jint cval, aidx, iidx, aval;

    jint paintOffset = 0;
    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jint alphaStride = rdr->_alphaWidth;
    jint textureStride = rdr->_texture_stride;
    jint *scanLineAlpha = rdr->_scanLineAlpha;

    jint* paint = rdr->_paint;
    
    jint am;
    
    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;
    
    aidx = 0;
    for (j = 0; j < height; j++) {
        scanA = scanLineAlpha[j];
      
        iidx = imageOffset + minX * imagePixelStride;
        
        am = aidx + w;
        while (aidx < am) {
            assert(aidx >= 0);
            assert(aidx < rdr->_paint_length);

            cval = paint[aidx];
            aval = (cval >> 24) & 0xff;
      
            if (scanA < MAX_ALPHA) {
                aval = ((aval+1) * scanA) >> 8;
            }
            
            if (aval == MAX_ALPHA) {
                intData[iidx] = cval;
            } else if (aval > 0) {
                blendSrcOver8888(&intData[iidx], aval, (cval >> 16) & 0xff,
                                 (cval >> 8) & 0xff, cval & 0xff);
            }
            iidx += imagePixelStride;
            ++aidx;
        }
        imageOffset += imageScanlineStride;
        paintOffset += textureStride;
    }
}

void
blitImageSrcOver8888_pre(Renderer *rdr, jint height) {
    jint j;
    jint minX, maxX, w, scanA;
    jint cval, aidx, iidx, aval, avalOrig;

    jint paintOffset = 0;
    jint *intData = rdr->_data;
    jint imageOffset = rdr->_currImageOffset;
    jint imageScanlineStride = rdr->_imageScanlineStride;
    jint imagePixelStride = rdr->_imagePixelStride;
    jint alphaStride = rdr->_alphaWidth;
    jint textureStride = rdr->_texture_stride;
    jint *scanLineAlpha = rdr->_scanLineAlpha;

    jint* paint = rdr->_paint;
    jint pre_rval, pre_gval, pre_bval;
    
    jint am;
    
    minX = rdr->_minTouched;
    maxX = rdr->_maxTouched;
    w = (maxX >= minX) ? (maxX - minX + 1) : 0;
    
    aidx = 0;
    for (j = 0; j < height; j++) {
        scanA = scanLineAlpha[j];

        iidx = imageOffset + minX * imagePixelStride;
        
        am = aidx + w;
        while (aidx < am) {
            assert(aidx >= 0);
            assert(aidx < rdr->_paint_length);

            cval = paint[aidx];
            avalOrig = aval = (cval >> 24) & 0xff;
      
            if (scanA < MAX_ALPHA) {
                aval = ((aval+1) * scanA) >> 8;
            }
            
            if (aval == MAX_ALPHA) {
                intData[iidx] = cval;
            } else if (aval > 0) {
                pre_rval = (cval >> 16) & 0xff;
                pre_gval = (cval >> 8) & 0xff;
                pre_bval = cval & 0xff;
                blendSrcOver8888_pre(&intData[iidx], aval, 
                    (pre_rval * 255)/avalOrig,
                    (pre_gval * 255)/avalOrig,
                    (pre_bval * 255)/avalOrig);
            }
            iidx += imagePixelStride;
            ++aidx;
        }
        imageOffset += imageScanlineStride;
        paintOffset += textureStride;
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
            palpha = (cval >> 24) & 0xff;
            
            aval_relative += *a;
            *a++ = 0;
            if (aval_relative) {
                aval = alphaMap[aval_relative] & 0xff;
                aval = ((aval+1) * palpha) >> 8;
                
                if (aval == MAX_ALPHA) {
                    intData[iidx] = cval;
                } else if (aval > 0) {
                    blendSrcOver8888_pre(&intData[iidx], aval, (cval >> 16) & 0xff,
                                     (cval >> 8) & 0xff, cval & 0xff);
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
            if (*a) {
                cval = paint[aidx];
                palpha = (cval >> 24) & 0xff;
            
                aval = *a & 0xff;
                aval = ((aval+1) * palpha) >> 8;
                
                if (aval == MAX_ALPHA) {
                    intData[iidx] = cval;
                } else if (aval > 0) {
                    blendSrcOver8888_pre(&intData[iidx], aval, (cval >> 16) & 0xff,
                                     (cval >> 8) & 0xff, cval & 0xff);
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
clearRect8888(Renderer *rdr, jint x, jint y, jint w, jint h) {
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

// 8-bit blend against an ARGB color
static void
blendSrcOver8888(jint *intData,
                 jint aval,
                 jint sred, jint sgreen, jint sblue) {
    jint denom;

    jint ival = *intData;
    jint dalpha = (ival >> 24) & 0xff;
    jint dred = (ival >> 16) & 0xff;
    jint dgreen = (ival >> 8) & 0xff;
    jint dblue = ival & 0xff;

    denom = 255 * dalpha + aval * (255 - dalpha);
    if (denom == 0) {
        // dalpha and aval must both be 0
        // The output is transparent black
        *intData = 0x00000000;
    } else {
        jlong recip = 16581375L / denom; // 255^3 = 16581375
        jlong fa = (255 - aval) * dalpha * recip;
        jlong fb = 255 * aval * recip;
        jint oalpha = denom / 255;
        jint ored = (jint)((fa * dred + fb * sred + HALF_1_SHIFT_23) / 16581375); // 255^3 = 16581375
        jint ogreen = (jint)((fa * dgreen + fb * sgreen + HALF_1_SHIFT_23) / 16581375);
        jint oblue = (jint)((fa * dblue + fb * sblue + HALF_1_SHIFT_23) / 16581375);

        ival = (oalpha << 24) | (ored << 16) | (ogreen << 8) | oblue;
        *intData = ival;
    }
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

// *intData are premultiplied, sred, sgreen, sblue are NOT premultiplied
// it is required that final alpha must be fully opaque (0xFF)
static void
blendLCDSrcOver8888_pre(jint *intData,
                             jint ared, jint agreen, jint ablue,
                             jint sred, jint sgreen, jint sblue,
                             jfloat gamma, jfloat invgamma)
{
    jint ival = *intData;
    //destination alpha
    jint dalpha = (ival >> 24) & 0xff;
    //destination components premultiplied by dalpha
    jint dred = (ival >> 16) & 0xff;
    jint dgreen = (ival >> 8) & 0xff;
    jint dblue = ival & 0xff;

    jint ored, ogreen, oblue;

    dred = (jint) (255 * (pow(dred / 255.0, invgamma)));
    dgreen = (jint) (255 * (pow(dgreen / 255.0, invgamma)));
    dblue = (jint) (255 * (pow(dblue / 255.0, invgamma)));

    ored    = ared * sred     + (255 - ared) * dred;
    ogreen  = agreen * sgreen + (255 - agreen) * dgreen;
    oblue   = ablue * sblue   + (255 - ablue) * dblue;

    ored = (jint) (255 * (pow(ored / 65025.0, gamma)));    // 65025 = 255*255
    ogreen = (jint) (255 * (pow(ogreen / 65025.0, gamma)));
    oblue = (jint) (255 * (pow(oblue / 65025.0, gamma)));

    *intData = 0xFF000000 | (ored << 16) | (ogreen << 8) | oblue;
}

static void
blendSrc8888(jint *intData,
             jint aval, jint raaval,
             jint sred, jint sgreen, jint sblue) {
    jint denom;

    jint ival = *intData;
    jint dalpha = (ival >> 24) & 0xff;
    jint dred = (ival >> 16) & 0xff;
    jint dgreen = (ival >> 8) & 0xff;
    jint dblue = ival & 0xff;

    denom = 255 * aval + dalpha * raaval;
    if (denom == 0) {
        // The output is transparent black
        *intData = 0x00000000;
    } else {
        jlong recip = 16581375L / denom; // 255^3 = 16581375
        jlong fa = raaval * dalpha * recip;
        jlong fb = 255 * aval * recip;
        jint oalpha = denom / 255;
        jint ored = (jint)((fa * dred + fb * sred) / 16581375);
        jint ogreen = (jint)((fa * dgreen + fb * sgreen) / 16581375);
        jint oblue = (jint)((fa * dblue + fb * sblue) / 16581375);

        ival = (oalpha << 24) | (ored << 16) | (ogreen << 8) | oblue;
        *intData = ival;
    }
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

