/*
 * Copyright (c) 2008, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include "SSEUtils.h"
#include "com_sun_scenario_effect_impl_sw_sse_SSERendererDelegate.h"

#ifdef WIN32 /* WIN32 */
#include <windows.h>
#endif

JNIEXPORT jboolean JNICALL
Java_com_sun_scenario_effect_impl_sw_sse_SSERendererDelegate_isSupported
    (JNIEnv *env, jclass klass)
{
#ifdef WIN32 /* WIN32 */
    return ::IsProcessorFeaturePresent(PF_XMMI64_INSTRUCTIONS_AVAILABLE) ?
        JNI_TRUE : JNI_FALSE;
#else
    /* No reports of any other platform generating SSE2 instructions */
    return JNI_TRUE;
#endif
}

static void laccum(jint pixel, jfloat mul, jfloat *fvals) {
    mul /= 255.f;
    fvals[FVAL_R] += ((pixel >> 16) & 0xff) * mul;
    fvals[FVAL_G] += ((pixel >>  8) & 0xff) * mul;
    fvals[FVAL_B] += ((pixel      ) & 0xff) * mul;
    fvals[FVAL_A] += ((pixel >> 24) & 0xff) * mul;
}

void lsample(jint *img,
             jfloat floc_x, jfloat floc_y,
             jint w, jint h, jint scan,
             jfloat *fvals)
{
    fvals[0] = 0.f;
    fvals[1] = 0.f;
    fvals[2] = 0.f;
    fvals[3] = 0.f;
    // If we subtract 0.5 then floc_xy could go negative and the
    // integer cast will not perform a true floor operation so
    // instead we add 0.5 and then iloc_xy will be off by 1
    floc_x = floc_x * w + 0.5f;
    floc_y = floc_y * h + 0.5f;
    jint iloc_x = (int) floc_x;  // 0 <= iloc_x <= w
    jint iloc_y = (int) floc_y;  // 0 <= iloc_y <= h
    // Note we test floc against 0 because iloc may have rounded the wrong way
    // for some numbers.  But, iloc values are valid for testing against w,h
    if (floc_x > 0 && floc_y > 0 && iloc_x <= w && iloc_y <= h) {
        floc_x -= iloc_x;   // now fractx
        floc_y -= iloc_y;   // now fracty
        // sample box from iloc_x-1,y-1 to iloc_x,y
        jint offset = iloc_y * scan + iloc_x;
        jfloat fract = floc_x * floc_y;
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
                laccum(img[offset-scan-1], 1.f - floc_x - floc_y + fract, fvals);
            }
        }
    }
}

void laccumsample(jint *img,
                  jfloat fpix_x, jfloat fpix_y,
                  jint w, jint h, jint scan,
                  jfloat factor, jfloat *fvals)
{
    factor *= 255.0f;
    // If we subtract 0.5 then fpix_xy could go negative and the
    // integer cast will not perform a true floor operation so
    // instead we add 0.5 and then ipix_xy will be off by 1
    fpix_x = fpix_x + 0.5f;
    fpix_y = fpix_y + 0.5f;
    jint ipix_x = (int) fpix_x;  // 0 <= ipix_x <= w
    jint ipix_y = (int) fpix_y;  // 0 <= ipix_y <= h
    // Note we test fpix against 0 because ipix may have rounded the wrong way
    // for some numbers.  But, ipix values are valid for testing against w,h
    if (fpix_x > 0 && fpix_y > 0 && ipix_x <= w && ipix_y <= h) {
        fpix_x -= ipix_x;   // now fractx
        fpix_y -= ipix_y;   // now fracty
        // sample box from ipix_x-1,y-1 to ipix_x,y
        jint offset = ipix_y * scan + ipix_x;
        jfloat fract = fpix_x * fpix_y;
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
                laccum(img[offset-scan-1], (1.0f - fpix_x - fpix_y + fract) * factor, fvals);
            }
        }
    }
}

static void faccum(jfloat *map, jint offset, jfloat fract, jfloat *fvals) {
    fvals[0] += map[offset  ] * fract;
    fvals[1] += map[offset+1] * fract;
    fvals[2] += map[offset+2] * fract;
    fvals[3] += map[offset+3] * fract;
}

void fsample(jfloat *map,
             jfloat floc_x, jfloat floc_y,
             jint w, jint h, jint scan,
             jfloat *fvals)
{
    fvals[0] = 0.f;
    fvals[1] = 0.f;
    fvals[2] = 0.f;
    fvals[3] = 0.f;
    // If we subtract 0.5 then floc_xy could go negative and the
    // integer cast will not perform a true floor operation so
    // instead we add 0.5 and then iloc_xy will be off by 1
    floc_x = floc_x * w + 0.5f;
    floc_y = floc_y * h + 0.5f;
    jint iloc_x = (int) floc_x;  // 0 <= iloc_x <= w
    jint iloc_y = (int) floc_y;  // 0 <= iloc_y <= h
    // Note we test floc against 0 because iloc may have rounded the wrong way
    // for some numbers.  But, iloc values are valid for testing against w,h
    if (floc_x > 0 && floc_y > 0 && iloc_x <= w && iloc_y <= h) {
        floc_x -= iloc_x;   // now fractx
        floc_y -= iloc_y;   // now fracty
        // sample box from iloc_x-1,y-1 to iloc_x,y
        jint offset = 4*(iloc_y * scan + iloc_x);
        jfloat fract = floc_x * floc_y;
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
                faccum(map, offset-scan*4-4, 1.f - floc_x - floc_y + fract, fvals);
            }
        }
    }
}

/*
 * checkRange function returns true if source or destination
 * dimensions are not in the required bounds and returns false
 * if dimensions are within required bounds.
 */
bool checkRange(JNIEnv *env,
                jintArray dstPixels_arr, jint dstw, jint dsth,
                jintArray srcPixels_arr, jint srcw, jint srch)
{
    return (srcPixels_arr == NULL ||
            dstPixels_arr == NULL ||
            srcw <= 0 ||
            srch <= 0 ||
            srcw > INT_MAX / srch ||
            dstw <= 0 ||
            dsth <= 0 ||
            dstw > INT_MAX / dsth ||
            (srcw * srch) > env->GetArrayLength(srcPixels_arr) ||
            (dstw * dsth) > env->GetArrayLength(dstPixels_arr));
}
