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


/**
 *  \file PiscesRenderer.inl
 *  Renderer related inline-functions implementation. C high level API.  
 */

#include <PiscesRenderer.h>

#include <PiscesUtil.h>
#include <PiscesBlit.h>
#include <PiscesPaint.h>
#include <PiscesTransform.h>

#include <PiscesSysutils.h>

#ifdef PISCES_AA_LEVEL
#define DEFAULT_SUBPIXEL_LG_POSITIONS_X PISCES_AA_LEVEL
#define DEFAULT_SUBPIXEL_LG_POSITIONS_Y PISCES_AA_LEVEL
#else
#define DEFAULT_SUBPIXEL_LG_POSITIONS_X 1
#define DEFAULT_SUBPIXEL_LG_POSITIONS_Y 1
#endif

#define PISCES_ACV (jlong)(65536.0 * 0.22385762508460333)

#define INVALID_COLOR_ALPHA_MAP 1
#define INVALID_PAINT_ALPHA_MAP 2
#define INVALID_INTERNAL_COLOR 8


#define INVALID_COMPOSITE_DEPENDED_ROUTINES 32
#define INVALID_PAINT_DEPENDED_ROUTINES 64
#define INVALID_MASK_DEPENDED_ROUTINES 128

#define INVALID_BLITTING_MASK (INVALID_INTERNAL_COLOR |                        \
                               INVALID_RENDERER_SURFACE |                      \
                               INVALID_COMPOSITE_DEPENDED_ROUTINES |           \
                               INVALID_PAINT_DEPENDED_ROUTINES |               \
                               INVALID_MASK_DEPENDED_ROUTINES)
                               
#define INVALID_ALL (INVALID_COLOR_ALPHA_MAP |                                 \
                     INVALID_PAINT_ALPHA_MAP |                                 \
                     INVALID_BLITTING_MASK)

#define VALIDATE_SURFACE(rdr)                                                  \
    if ((rdr)->_rendererState & INVALID_RENDERER_SURFACE) {                    \
            updateRendererSurface(rdr);                                        \
    }

#define VALIDATE_BLITTING(rdr)                                                 \
    if ((rdr)->_rendererState & INVALID_BLITTING_MASK) {                       \
        jint __state = (rdr)->_rendererState;                                  \
                                                                               \
        if (__state & INVALID_RENDERER_SURFACE) {                              \
            updateRendererSurface(rdr);                                        \
        }                                                                      \
                                                                               \
        if (__state & INVALID_INTERNAL_COLOR) {                                \
            updateInternalColor(rdr);                                          \
        }                                                                      \
                                                                               \
        if (__state & INVALID_MASK_DEPENDED_ROUTINES) {                        \
            /* Optimization: validates also INVALID_PAINT_DEPENDED_ROUTINES */ \
            updateMaskDependedRoutines(rdr);                                   \
        } else if (__state & INVALID_COMPOSITE_DEPENDED_ROUTINES) {            \
            updateCompositeDependedRoutines(rdr);                              \
        } else if (__state & INVALID_PAINT_DEPENDED_ROUTINES) {                \
            updatePaintDependedRoutines(rdr);                                  \
        }                                                                      \
                                                                               \
        assert((rdr->_rendererState & INVALID_BLITTING_MASK) == 0);            \
    }

static INLINE Renderer* renderer_create(Surface* surface);
static INLINE void renderer_dispose(Renderer* rdr);

static INLINE void renderer_setClip(Renderer* rdr, jint minX, jint minY,
                                    jint width, jint height);

static INLINE void renderer_setCompositeRule(Renderer *rdr, jint compositeRule);
static INLINE void renderer_setColor(Renderer* rdr, jint red, jint green,
                                     jint blue, jint alpha);
static INLINE void renderer_setLinearGradient(Renderer* rdr, jint x0, jint y0,
        jint x1, jint y1, jint* colors,
        Transform6 *transform);
static INLINE void renderer_setRadialGradient(Renderer* rdr, jint cx, jint cy,
        jint fx, jint fy, jint radius,
        jint* colors,
        Transform6 *transform);

static Renderer* createCommon(Surface* surface);
static void setPaintMode(Renderer* rdr, jint newPaintMode);
static void setAntialiasing(Renderer* rdr, jint subpixelLgPositionsX,
                            jint subpixelLgPositionsY);

static void updateInternalColor(Renderer* rdr);
static void validateAlphaMap(Renderer* rdr);

static void updateRendererSurface(Renderer* rdr);

static void updateSurfaceDependedRoutines(Renderer* rdr);
static void updateMaskDependedRoutines(Renderer* rdr);
static void updateCompositeDependedRoutines(Renderer* rdr);
static void updatePaintDependedRoutines(Renderer* rdr);

static INLINE Renderer* 
renderer_create(Surface* surface) {
    return createCommon(surface);
}

static INLINE void
renderer_dispose(Renderer* rdr) {
    my_free(rdr->_rowAAInt);
    if (rdr->_texture_free == JNI_TRUE) {
        my_free(rdr->_texture_intData);
        my_free(rdr->_texture_byteData);
        my_free(rdr->_texture_alphaData);
    }
    
    my_free(rdr->_paint);

    my_free(rdr);
}

/**
 * This function sets clip-rect. Any part of object which is determined outside
 * of clip-rect is cliped == not drawn to destination surface.
 * Values are INTEGERS but NOT fixed point and represents rectangle
 * in surface coordinates.
 * @param rdr pointer to renderer structure
 * @param minX clip-rects upper-left corner x-coordinate
 * @param minY clip-rects upper-left corner y-coordinate
 * @param width clip-rects width
 * @param height clip-rects height
 */   
static INLINE void
renderer_setClip(Renderer* rdr, jint minX, jint minY, jint width, jint height) {
    rdr->_clip_bbMinX = minX;
    rdr->_clip_bbMinY = minY;
    rdr->_clip_bbMaxX = minX + width - 1;
    rdr->_clip_bbMaxY = minY + height- 1;
}

static INLINE void
renderer_setColor(Renderer* rdr, jint red, jint green, jint blue, jint alpha) {
    if ((rdr->_cred != red) ||
        (rdr->_cgreen != green) ||
        (rdr->_cblue != blue) ||
        (rdr->_calpha != alpha))
    {
        rdr->_rendererState |= INVALID_INTERNAL_COLOR;
        if (rdr->_calpha != alpha) {
            rdr->_rendererState |= INVALID_COLOR_ALPHA_MAP |
                                   INVALID_PAINT_ALPHA_MAP;
        }
        
        rdr->_cred = red;
        rdr->_cgreen = green;
        rdr->_cblue = blue;
        rdr->_calpha = alpha;
    }

    setPaintMode(rdr, PAINT_FLAT_COLOR);
}

/**
 * This function sets composite rule of rdr. It initializes bliting functions 
 * pointers for current surface-type too. 
 * @param rdr pointer to Renderer structure 
 * @param compositeRule compositing rule    
 * @see For supported compositeRule values see CompositingRules, 
 * renderer_setComposite(Renderer *, jint, jfloat)
 */
static INLINE void
renderer_setCompositeRule(Renderer* rdr, jint compositeRule) {
    if (rdr->_compositeRule != compositeRule) {
        // composite mode COMPOSITE_CLEAR changes the internal color
        rdr->_rendererState |= INVALID_INTERNAL_COLOR | 
                               INVALID_COMPOSITE_DEPENDED_ROUTINES;

        if ((compositeRule == COMPOSITE_SRC_OVER) ||
            (((compositeRule == COMPOSITE_CLEAR) ||
            (compositeRule == COMPOSITE_SRC)) &&
            ((rdr->_imageType == TYPE_INT_ARGB) || (rdr->_imageType == TYPE_INT_ARGB_PRE))))
        {
            rdr->_rendererState |= INVALID_COLOR_ALPHA_MAP |
                                   INVALID_PAINT_ALPHA_MAP;
        }
        
        rdr->_compositeRule = compositeRule;
    }
}

static INLINE void
renderer_setLinearGradient(Renderer* rdr,
                           jint x0, jint y0, jint x1, jint y1,
                           jint *colors,
                           Transform6 *transform)
{
    jfloat fx0, fx1, fy0, fy1, fdx, fdy, flensq, t;
    jfloat a00, a01, a02, a10, a11, a12;

    pisces_transform_assign(&rdr->_gradient_transform, transform);
    pisces_transform_assign(&rdr->_gradient_inverse_transform, transform);
    pisces_transform_invert(&rdr->_gradient_inverse_transform);

    a00 = rdr->_gradient_inverse_transform.m00;
    a01 = rdr->_gradient_inverse_transform.m01;
    a02 = rdr->_gradient_inverse_transform.m02/65536.0f;
    a10 = rdr->_gradient_inverse_transform.m10;
    a11 = rdr->_gradient_inverse_transform.m11;
    a12 = rdr->_gradient_inverse_transform.m12/65536.0f;

    fx0 = x0/65536.0f;
    fx1 = x1/65536.0f;
    fy0 = y0/65536.0f;
    fy1 = y1/65536.0f;
    fdx = fx1 - fx0;
    fdy = fy1 - fy0;
    flensq = fdx*fdx + fdy*fdy;
    t = fdx*fx0 + fdy*fy0;

    rdr->_lg_mx = (a00*fdx + a10*fdy)/flensq;
    rdr->_lg_my = (a01*fdx + a11*fdy)/flensq;
    rdr->_lg_b = (65536.0f*(a02*fdx + a12*fdy - t)/flensq);

    setPaintMode(rdr, PAINT_LINEAR_GRADIENT);
    memcpy(rdr->_gradient_colors, colors, GRADIENT_MAP_SIZE*sizeof(jint));
}

static INLINE void
renderer_setRadialGradient(Renderer* rdr,
                           jint cx, jint cy, jint fx, jint fy, jint radius,
                           jint *colors,
                           Transform6 *transform)
{
    jfloat _fx, _fy, _cx, _cy;
    jfloat fcx, fcy;
    jfloat dsq;
    
    pisces_transform_assign(&rdr->_gradient_transform, transform);
    pisces_transform_assign(&rdr->_gradient_inverse_transform, transform);
    pisces_transform_invert(&rdr->_gradient_inverse_transform);

    rdr->_rg_a00 = rdr->_gradient_inverse_transform.m00 / 65536.0f;
    rdr->_rg_a01 = rdr->_gradient_inverse_transform.m01 / 65536.0f;
    rdr->_rg_a02 = rdr->_gradient_inverse_transform.m02 / 65536.0f;
    rdr->_rg_a10 = rdr->_gradient_inverse_transform.m10 / 65536.0f;
    rdr->_rg_a11 = rdr->_gradient_inverse_transform.m11 / 65536.0f;
    rdr->_rg_a12 = rdr->_gradient_inverse_transform.m12 / 65536.0f;

    rdr->_rg_a00a00 = rdr->_rg_a00 * rdr->_rg_a00;
    rdr->_rg_a10a10 = rdr->_rg_a10 * rdr->_rg_a10;
    rdr->_rg_a00a10 = rdr->_rg_a00 * rdr->_rg_a10;

    _cx = cx / 65536.0f;
    _cy = cy / 65536.0f;
    _fx = fx / 65536.0f;
    _fy = fy / 65536.0f;
    rdr->_rg_r = radius / 65536.0f;
    rdr->_rg_rsq = rdr->_rg_r * rdr->_rg_r;

    fcx = _fx - _cx;
    fcy = _fy - _cy;
    dsq = fcx * fcx + fcy * fcy;
    if (dsq > rdr->_rg_rsq * 0.94f) {
        jfloat f = (rdr->_rg_r * 0.97f) / ((jfloat) PISCESsqrt(dsq));
        _fx = _cx + f * fcx;
        _fy = _cy + f * fcy;
    }
    
    rdr->_rg_cx = _cx;
    rdr->_rg_cy = _cy;
    rdr->_rg_fx = _fx;
    rdr->_rg_fy = _fy;

    setPaintMode(rdr, PAINT_RADIAL_GRADIENT);
    memcpy(rdr->_gradient_colors, colors, GRADIENT_MAP_SIZE*sizeof(jint));
}


static INLINE void
renderer_setTexture(Renderer* rdr, jint renderMode, jint* data, jint width, jint height, jint stride,
                    jboolean repeat, jboolean smooth, 
                    const Transform6* transform, jboolean freeData,
                    jboolean textureHasAlpha,
                    jint interpolateMinX, jint interpolateMinY, jint interpolateMaxX, jint interpolateMaxY) {
    Transform6 compoundTransform;

    pisces_transform_assign(&rdr->_paint_transform, transform);

    pisces_transform_assign(&compoundTransform, transform);
    pisces_transform_invert(&compoundTransform);

    setPaintMode(rdr, (renderMode == IMAGE_MODE_NORMAL) ?
        PAINT_TEXTURE8888 : PAINT_TEXTURE8888_MULTIPLY);

    if (rdr->_texture_free == JNI_TRUE) {
        my_free(rdr->_texture_intData);
        my_free(rdr->_texture_byteData);
        my_free(rdr->_texture_alphaData);
    }

    rdr->_texture_free = freeData;

    rdr->_texture_hasAlpha = textureHasAlpha;

    rdr->_texture_intData = data;
    rdr->_texture_byteData = NULL;
    rdr->_texture_alphaData = NULL;

    rdr->_texture_imageWidth = width;
    rdr->_texture_imageHeight = height;
    rdr->_texture_stride = stride;
    rdr->_texture_repeat = repeat;
    rdr->_texture_interpolateMinX = interpolateMinX;
    rdr->_texture_interpolateMinY = interpolateMinY;
    rdr->_texture_interpolateMaxX = interpolateMaxX;
    rdr->_texture_interpolateMaxY = interpolateMaxY;

    rdr->_texture_m00 = compoundTransform.m00;
    rdr->_texture_m01 = compoundTransform.m01;
    rdr->_texture_m10 = compoundTransform.m10;
    rdr->_texture_m11 = compoundTransform.m11;
    rdr->_texture_m02 = compoundTransform.m02;
    rdr->_texture_m12 = compoundTransform.m12;

    if (smooth == XNI_TRUE) { 
        rdr->_texture_interpolate = XNI_TRUE;
        rdr->_texture_m02 += (rdr->_texture_m00 >> 1) + (rdr->_texture_m01 >> 1) - 32768;   // interpolate == true
        rdr->_texture_m12 += (rdr->_texture_m10 >> 1) + (rdr->_texture_m11 >> 1) - 32768;   // interpolate == true
    } else {
        rdr->_texture_interpolate = XNI_FALSE;
    }

    //Do we have identity matrix? Actualy even translate is not bad for us ...
    if (rdr->_texture_m00 == 65536 &&
        rdr->_texture_m11 == 65536 &&
        rdr->_texture_m01 == 0 &&
        rdr->_texture_m10 == 0)
    {
        if (rdr->_texture_m02 == 0 && rdr->_texture_m12 == 0)
        {
            rdr->_texture_transformType = TEXTURE_TRANSFORM_IDENTITY;
        } else {
            rdr->_texture_transformType = TEXTURE_TRANSFORM_TRANSLATE;
            if ((rdr->_texture_m02 & 0xFFFF) == 0 &&
                (rdr->_texture_m12 & 0xFFFF) == 0)
            {
                // we can disable interpolation since TX and TX has no fraction part
                rdr->_texture_interpolate = XNI_FALSE;
            }
        }
    } else {
        rdr->_texture_transformType = TEXTURE_TRANSFORM_GENERIC;
    }
}

static INLINE void
renderer_setMask(Renderer* rdr, jint maskType, jbyte* data, jint width, jint height,
    jboolean freeData)
{
    if (rdr->_mask_free == JNI_TRUE) {
        my_free(rdr->_mask_byteData);
    }

    rdr->_mask_free = freeData;

    rdr->_maskType = maskType;
    rdr->_mask_byteData = data;

    rdr->_mask_width = width;
    rdr->_mask_height = height;

    rdr->_rendererState |= INVALID_BLITTING_MASK | INVALID_MASK_DEPENDED_ROUTINES;
}

static INLINE void
renderer_removeMask(Renderer* rdr)
{
    if (rdr->_mask_free == JNI_TRUE) {
        my_free(rdr->_mask_byteData);
    }
    rdr->_maskType = NO_MASK;
    rdr->_mask_byteData = NULL;
    rdr->_rendererState |= INVALID_BLITTING_MASK | INVALID_MASK_DEPENDED_ROUTINES;
}

static INLINE void
renderer_clearRect(Renderer* rdr, jint x, jint y, jint w, jint h) {
    jint maxX = x + w - 1;
    jint maxY = y + h - 1;

    VALIDATE_BLITTING(rdr);

    x = MAX(x, 0);
    x = MAX(x, rdr->_clip_bbMinX);

    y = MAX(y, 0);
    y = MAX(y, rdr->_clip_bbMinY);

    maxX = MIN(maxX, rdr->_width - 1);
    maxX = MIN(maxX, rdr->_clip_bbMaxX);

    maxY = MIN(maxY, rdr->_height - 1);
    maxY = MIN(maxY, rdr->_clip_bbMaxY);

    if ((x <= maxX) && (y <= maxY)) {
        rdr->_clearRect(rdr, x, y, maxX - x + 1, maxY - y + 1);
    }
}

static Renderer*
createCommon(Surface* surface) {
    Renderer* rdr = (Renderer*)my_malloc(Renderer, 1);

    ASSERT_ALLOC_POINTER(rdr);

    // initialize image type to an invalid value (will be corrected later)
    rdr->_imageType = -1;

    // initialize composite mode
    rdr->_compositeRule = COMPOSITE_SRC_OVER;
    
    rdr->_maskType = NO_MASK;

    // initialize paint mode
    rdr->_paintMode = PAINT_FLAT_COLOR;

    // initialize surface reference
    rdr->_surface = surface;

    // initialize the clip region
    rdr->_clip_bbMinX = 0;
    rdr->_clip_bbMinY = 0;
    rdr->_clip_bbMaxX = surface->width - 1;
    rdr->_clip_bbMaxY = surface->height - 1;

    // initialize renderer state
    rdr->_rendererState = INVALID_ALL;

    return rdr;
}

static void 
updateInternalColor(Renderer* rdr) {
    if (rdr->_compositeRule == COMPOSITE_CLEAR) {
        rdr->_cred = 0;
        rdr->_cgreen = 0;
        rdr->_cblue = 0;
        rdr->_calpha = 0;
    }
    rdr->_rendererState &= ~INVALID_INTERNAL_COLOR;
}

static void
updateRendererSurface(Renderer* rdr) {
    Surface* surface = rdr->_surface;
  
    rdr->_width = 
            surface->width;
    rdr->_height = 
            surface->height;
    rdr->_data = 
            surface->data;
    rdr->_imageOffset = 
            surface->offset;
    rdr->_imageScanlineStride = 
            surface->scanlineStride;
    rdr->_imagePixelStride = 
            surface->pixelStride;

    if (rdr->_imageType != surface->imageType)
    {
        if ((rdr->_compositeRule != COMPOSITE_SRC_OVER) && 
            (surface->imageType == TYPE_INT_ARGB || surface->imageType == TYPE_INT_ARGB_PRE))
        {
            rdr->_rendererState |= INVALID_COLOR_ALPHA_MAP |
                                   INVALID_PAINT_ALPHA_MAP;
        }
    
        rdr->_imageType = surface->imageType;
        updateSurfaceDependedRoutines(rdr);
    }
    
    rdr->_rendererState &= ~INVALID_RENDERER_SURFACE;
}

static void
updateSurfaceDependedRoutines(Renderer* rdr) {
    switch (rdr->_imageType) {
        case TYPE_INT_ARGB:
            rdr->_bl_SourceOverNoMask = blitSrcOver8888;
            rdr->_bl_PT_SourceOverNoMask = blitPTSrcOver8888;
            rdr->_bl_SourceNoMask = blitSrc8888;
            rdr->_bl_PT_SourceNoMask = blitPTSrc8888;

            rdr->_bl_SourceOverMask = NULL;
            rdr->_bl_PT_SourceOverMask = NULL;
            rdr->_bl_SourceMask = NULL;
            rdr->_bl_PT_SourceMask = NULL;

            rdr->_bl_SourceOverLCDMask = NULL;
            rdr->_bl_PT_SourceOverLCDMask = NULL;
            rdr->_bl_SourceLCDMask = NULL;
            rdr->_bl_PT_SourceLCDMask = NULL;

            rdr->_bl_Clear = blitSrc8888;
            rdr->_bl_PT_Clear = blitSrc8888;
            rdr->_clearRect = clearRect8888;

            rdr->_el_Source = emitLineSource8888;
            rdr->_el_SourceOver = emitLineSourceOver8888;
            rdr->_el_PT_Source = emitLinePTSource8888;
            rdr->_el_PT_SourceOver = emitLinePTSourceOver8888;
            break;
        case TYPE_INT_ARGB_PRE:
            rdr->_bl_SourceOverNoMask = blitSrcOver8888_pre;
            rdr->_bl_PT_SourceOverNoMask = blitPTSrcOver8888_pre;
            rdr->_bl_SourceNoMask = blitSrc8888_pre;
            rdr->_bl_PT_SourceNoMask = blitPTSrc8888_pre;

            rdr->_bl_SourceOverMask = blitSrcOverMask8888_pre;
            rdr->_bl_PT_SourceOverMask = blitPTSrcOverMask8888_pre;
            rdr->_bl_SourceMask = blitSrcMask8888_pre;
            rdr->_bl_PT_SourceMask = blitPTSrcMask8888_pre;

            rdr->_bl_SourceOverLCDMask = blitSrcOverLCDMask8888_pre;
            rdr->_bl_PT_SourceOverLCDMask = NULL;
            rdr->_bl_SourceLCDMask = NULL;
            rdr->_bl_PT_SourceLCDMask = NULL;

            rdr->_bl_Clear = blitSrc8888_pre;
            rdr->_bl_PT_Clear = blitSrc8888_pre;
            rdr->_clearRect = clearRect8888;

            rdr->_el_Source = emitLineSource8888_pre;
            rdr->_el_SourceOver = emitLineSourceOver8888_pre;
            rdr->_el_PT_Source = emitLinePTSource8888_pre;
            rdr->_el_PT_SourceOver = emitLinePTSourceOver8888_pre;
            break;
        default:
            // unsupported!
            break;
    }

    updateMaskDependedRoutines(rdr);
}

static void
updateMaskDependedRoutines(Renderer* rdr) {
    switch (rdr->_maskType) {
        case NO_MASK:
            rdr->_bl_SourceOver = rdr->_bl_SourceOverNoMask;
            rdr->_bl_PT_SourceOver = rdr->_bl_PT_SourceOverNoMask;
            rdr->_bl_Source = rdr->_bl_SourceNoMask;
            rdr->_bl_PT_Source = rdr->_bl_PT_SourceNoMask;
            break;
        case ALPHA_MASK:
            rdr->_bl_SourceOver = rdr->_bl_SourceOverMask;
            rdr->_bl_PT_SourceOver = rdr->_bl_PT_SourceOverMask;
            rdr->_bl_Source = rdr->_bl_SourceMask;
            rdr->_bl_PT_Source = rdr->_bl_PT_SourceMask;
            break;
        case LCD_ALPHA_MASK:
            rdr->_bl_SourceOver = rdr->_bl_SourceOverLCDMask;
            rdr->_bl_PT_SourceOver = rdr->_bl_PT_SourceOverLCDMask;
            rdr->_bl_Source = rdr->_bl_SourceLCDMask;
            rdr->_bl_PT_Source = rdr->_bl_PT_SourceLCDMask;
            break;
        default:
            // unsupported!
            break;
    }
    updateCompositeDependedRoutines(rdr);
    rdr->_rendererState &= ~INVALID_MASK_DEPENDED_ROUTINES;
}

static void
updateCompositeDependedRoutines(Renderer* rdr) {
    switch (rdr->_compositeRule) {
        case COMPOSITE_SRC_OVER:
            rdr->_bl = rdr->_bl_SourceOver;
            rdr->_bl_PT = rdr->_bl_PT_SourceOver;
            rdr->_el = rdr->_el_SourceOver;
            rdr->_el_PT = rdr->_el_PT_SourceOver;
            break;
        case COMPOSITE_SRC:
            rdr->_bl = rdr->_bl_Source;
            rdr->_bl_PT = rdr->_bl_PT_Source;
            rdr->_el = rdr->_el_Source;
            rdr->_el_PT = rdr->_el_PT_Source;
            break;
        case COMPOSITE_CLEAR:
            rdr->_bl = rdr->_bl_Clear;
            rdr->_bl_PT = rdr->_bl_PT_Clear;
            break;
        default:
            // unsupported!
            break;
    }
    updatePaintDependedRoutines(rdr);
    rdr->_rendererState &= ~INVALID_COMPOSITE_DEPENDED_ROUTINES;
}

static void
updatePaintDependedRoutines(Renderer* rdr) {
    switch (rdr->_paintMode) {
        case PAINT_LINEAR_GRADIENT:
            rdr->_genPaint = genLinearGradientPaint;
            rdr->_emitRows = rdr->_bl_PT;
            rdr->_emitLine = rdr->_el_PT;
            break;
        case PAINT_RADIAL_GRADIENT:
            rdr->_genPaint = genRadialGradientPaint;
            rdr->_emitRows = rdr->_bl_PT;
            rdr->_emitLine = rdr->_el_PT;
            break;
        case PAINT_TEXTURE8888:
            rdr->_genPaint = genTexturePaint;
            rdr->_emitRows = rdr->_bl_PT;
            rdr->_emitLine = rdr->_el_PT;
            break;
        case PAINT_TEXTURE8888_MULTIPLY:
            rdr->_genPaint = genTexturePaintMultiply;
            rdr->_emitRows = rdr->_bl_PT;
            rdr->_emitLine = rdr->_el_PT;
            break;
        case PAINT_FLAT_COLOR:
            rdr->_genPaint = NULL;
            rdr->_emitRows = rdr->_bl;
            rdr->_emitLine = rdr->_el;
            break;
        default:
            // unsupported!
            break;
    }

    rdr->_rendererState &= ~INVALID_PAINT_DEPENDED_ROUTINES;
}

static void 
setPaintMode(Renderer* rdr, jint newPaintMode) {
    if (rdr->_paintMode != newPaintMode) {
        if (rdr->_texture_free == JNI_TRUE) {
            my_free(rdr->_texture_intData);
            my_free(rdr->_texture_byteData);
            my_free(rdr->_texture_alphaData);
        }
        rdr->_texture_intData = NULL;
        rdr->_texture_byteData = NULL;
        rdr->_texture_alphaData = NULL;

        rdr->_rendererState |= INVALID_PAINT_DEPENDED_ROUTINES;
        rdr->_prevPaintMode = rdr->_paintMode;
        rdr->_paintMode = newPaintMode;
    }
}

