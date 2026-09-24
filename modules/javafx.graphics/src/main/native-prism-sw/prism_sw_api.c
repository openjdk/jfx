/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * prism_sw_api.c - implementation of the flat C ABI declared in prism_sw_api.h.
 *
 * The bodies are the JNI-era ones: the renderer_* functions of PiscesRenderer.inl and the
 * fillRect / fillAlphaMask logic of the former JNI glue, with every VM environment / object handle
 * parameter replaced by the explicit pixels / transform / length parameters of the ABI. Both .inl
 * files define only static functions, so including them here is a per-translation-unit idiom. The
 * jint/jbyte/jfloat spellings are the fixed-width typedefs of PiscesDefs.h.
 */

#include "prism_sw_api.h"

#include <PiscesRenderer.inl>
#include <PiscesSurface.inl>

#include <limits.h>
#include <stddef.h>

/* ------------------------------------------------------------------------------------------------
 * Compile-time guards: PswTransform6 must be pointer-cast / memcpy compatible with Transform6,
 * and the PiscesDefs.h typedefs must have the widths the ABI promises. The internal COMPOSITE_* /
 * TYPE_* / IMAGE_* macros (PiscesRenderer.h, PiscesSurface.h) alias the PSW_* enum directly.
 * ---------------------------------------------------------------------------------------------- */

/* MSVC accepts _Static_assert in C only under /std:c11, which win.cmake does not pass: use the
 * negative-array-size idiom there (and on any other pre-C11 compiler). */
#if defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
#  define PSW_STATIC_ASSERT(cond, tag) _Static_assert(cond, #tag)
#else
#  define PSW_STATIC_ASSERT(cond, tag) typedef char psw_static_assert_##tag[(cond) ? 1 : -1]
#endif

PSW_STATIC_ASSERT(sizeof(PswTransform6) == 24, PswTransform6_is_24_bytes);
PSW_STATIC_ASSERT(sizeof(PswTransform6) == sizeof(Transform6), PswTransform6_same_size_as_Transform6);
PSW_STATIC_ASSERT(offsetof(PswTransform6, m00) == offsetof(Transform6, m00), PswTransform6_m00_offset);
PSW_STATIC_ASSERT(offsetof(PswTransform6, m01) == offsetof(Transform6, m01), PswTransform6_m01_offset);
PSW_STATIC_ASSERT(offsetof(PswTransform6, m10) == offsetof(Transform6, m10), PswTransform6_m10_offset);
PSW_STATIC_ASSERT(offsetof(PswTransform6, m11) == offsetof(Transform6, m11), PswTransform6_m11_offset);
PSW_STATIC_ASSERT(offsetof(PswTransform6, m02) == offsetof(Transform6, m02), PswTransform6_m02_offset);
PSW_STATIC_ASSERT(offsetof(PswTransform6, m12) == offsetof(Transform6, m12), PswTransform6_m12_offset);
PSW_STATIC_ASSERT(sizeof(int32_t) == sizeof(jint), jint_is_32_bits);
PSW_STATIC_ASSERT(sizeof(uint8_t) == sizeof(jbyte), jbyte_is_8_bits);
PSW_STATIC_ASSERT(sizeof(float) == sizeof(jfloat), jfloat_is_float);

static const int32_t psw_constants[] = {
    PSW_COMPOSITE_CLEAR,
    PSW_COMPOSITE_SRC,
    PSW_COMPOSITE_SRC_OVER,
    PSW_TYPE_INT_ARGB_PRE,
    PSW_IMAGE_MODE_NORMAL,
    PSW_IMAGE_MODE_MULTIPLY,
    PSW_IMAGE_FRAC_EDGE_KEEP,
    PSW_IMAGE_FRAC_EDGE_PAD,
    PSW_IMAGE_FRAC_EDGE_TRIM
};
PSW_STATIC_ASSERT(sizeof(psw_constants) / sizeof(psw_constants[0]) == PSW_CONSTANT_COUNT, psw_constants_count);

/* ------------------------------------------------------------------------------------------------
 * Helpers
 * ---------------------------------------------------------------------------------------------- */

/*
 * Turns the process-global mem_Error_Flag into the return status and clears it - the tail every
 * JNI entry point had as "if (readAndClearMemErrorFlag()) throw OutOfMemoryError".
 */
static int32_t
psw_finish(void) {
    return (readAndClearMemErrorFlag() == XNI_TRUE) ? PSW_ERR_OOM : PSW_OK;
}

static void
psw_transform_copy(const PswTransform6* src, Transform6* dst) {
    dst->m00 = src->m00;
    dst->m01 = src->m01;
    dst->m10 = src->m10;
    dst->m11 = src->m11;
    dst->m02 = src->m02;
    dst->m12 = src->m12;
}

/*
 * Binds the caller's pixel array to the surface for the duration of one call. This is the JNI
 * surface_acquire (JJavaSurface.c) without the array-length test, which the JavaSurface
 * constructor performs in Java; the width/height test is kept as it was.
 */
static int32_t
psw_surface_bind(Surface* surface, int32_t* pixels) {
    if (pixels == NULL || surface->width < 0 || surface->height < 0) {
        return PSW_ERR_ARG;
    }
    surface->data = pixels;
    return PSW_OK;
}

static void
psw_surface_unbind(Surface* surface) {
    surface->data = NULL;
}

/* Unbinds the renderer's surface and drops the copy of the pixel pointer the last validation took. */
static void
psw_renderer_unbind(Renderer* rdr) {
    psw_surface_unbind(rdr->_surface);
    rdr->_data = NULL;
}

/*
 * JPiscesRenderer.c fillRect with SURFACE_FROM_RENDERER / ACQUIRE_SURFACE / RELEASE_SURFACE and
 * the exception tail removed: the caller has already bound `pixels` to `surface` and reads the
 * OOM flag afterwards. x, y, w, h are S15.16 surface coordinates of an axis-aligned rectangle.
 */
static void
psw_fill_rect(Renderer* rdr, Surface* surface,
              jint x, jint y, jint w, jint h,
              jint lEdge, jint rEdge, jint tEdge, jint bEdge)
{
    jint x_from, x_to, y_from, y_to;
    jint lfrac, rfrac, tfrac, bfrac;
    jint rows_to_render_by_loop, rows_being_rendered;

    lfrac = (0x10000 - (x & 0xFFFF)) & 0xFFFF;
    rfrac = (x + w) & 0xFFFF;
    tfrac = (0x10000 - (y & 0xFFFF)) & 0xFFFF;
    bfrac = (y + h) & 0xFFFF;

    x_from = x >> 16;
    x_to = x + w;
    x_to = (rfrac) ? x_to >> 16 : (x_to >> 16) - 1;
    y_from = y >> 16;
    y_to = y + h;
    y_to = (bfrac) ? y_to >> 16 : (y_to >> 16) - 1;

    rdr->_rectX = x_from;
    rdr->_rectY = y_from;

    switch (lEdge) {
    case IMAGE_FRAC_EDGE_PAD:
        lfrac = 0;
        break;
    case IMAGE_FRAC_EDGE_TRIM:
        if (lfrac) { x_from++; }
        lfrac = 0;
        break;
    }

    switch (rEdge) {
    case IMAGE_FRAC_EDGE_PAD:
        rfrac = 0;
        break;
    case IMAGE_FRAC_EDGE_TRIM:
        if (rfrac) { x_to--; }
        rfrac = 0;
        break;
    }

    switch (tEdge) {
    case IMAGE_FRAC_EDGE_PAD:
        tfrac = 0;
        break;
    case IMAGE_FRAC_EDGE_TRIM:
        if (tfrac) { y_from++; }
        tfrac = 0;
        break;
    }

    switch (bEdge) {
    case IMAGE_FRAC_EDGE_PAD:
        bfrac = 0;
        break;
    case IMAGE_FRAC_EDGE_TRIM:
        if (bfrac) { y_to--; }
        bfrac = 0;
        break;
    }

    // apply clip
    if (x_from < rdr->_clip_bbMinX) {
        x_from = rdr->_clip_bbMinX;
        lfrac = 0;
    }
    if (y_from < rdr->_clip_bbMinY) {
        y_from = rdr->_clip_bbMinY;
        tfrac = 0;
    }
    if (x_to > rdr->_clip_bbMaxX) {
        x_to = rdr->_clip_bbMaxX;
        rfrac = 0;
    }
    if (y_to > rdr->_clip_bbMaxY) {
        y_to = rdr->_clip_bbMaxY;
        bfrac = 0;
    }

    if ((x_from <= x_to) && (y_from <= y_to)) {
        rows_to_render_by_loop = y_to - y_from + 1;

        INVALIDATE_RENDERER_SURFACE(rdr);
        VALIDATE_BLITTING(rdr);

        rdr->_minTouched = x_from;
        rdr->_maxTouched = x_to;
        rdr->_currX = x_from;
        rdr->_currY = y_from;

        rdr->_alphaWidth = x_to - x_from + 1;

        rdr->_currImageOffset = y_from * surface->width;
        rdr->_imageScanlineStride = surface->width;
        rdr->_imagePixelStride = 1;
        rdr->_rowNum = 0;

        if (y_from == y_to && (tfrac | bfrac)) {
            // rendering single horizontal fractional line bfrac > (y & 0xFFFF)
            tfrac = (bfrac - 0x10000 + tfrac) & 0xFFFF;
            bfrac = 0;
        }
        if (x_from == x_to && (lfrac | rfrac)) {
            // rendering single vertival fractional line rfrac > (x & 0xFFFF)
            lfrac = (rfrac - 0x10000 + lfrac) & 0xFFFF;
            rfrac = 0;
        }

        rdr->_el_lfrac = lfrac;
        rdr->_el_rfrac = rfrac;

        if (bfrac) {
            // one "full" line less -> will be rendered at the end
            rows_to_render_by_loop--;
        }

        // emit fractional top line
        if (tfrac) {
            if (rdr->_genPaint) {
                size_t l = (x_to - x_from + 1);
                ALLOC3(rdr->_paint, jint, l);
                rdr->_genPaint(rdr, 1);
            }
            rdr->_emitLine(rdr, 1, tfrac);
            rows_to_render_by_loop--;
            rdr->_currX = x_from;
            rdr->_currY++;
            rdr->_currImageOffset = rdr->_currY * surface->width;
            rdr->_rowNum++;
        }

        // emit "full" lines that are in the middle
        while (rows_to_render_by_loop > 0) {
            rows_being_rendered = MIN(rows_to_render_by_loop, NUM_ALPHA_ROWS);

            if (rdr->_genPaint) {
                size_t l = (x_to - x_from + 1) * rows_being_rendered;
                ALLOC3(rdr->_paint, jint, l);
                rdr->_genPaint(rdr, rows_being_rendered);
            }
            rdr->_emitLine(rdr, rows_being_rendered, 0x10000);

            rows_to_render_by_loop -= rows_being_rendered;
            rdr->_currX = x_from;
            rdr->_currY += rows_being_rendered;
            rdr->_currImageOffset = rdr->_currY * surface->width;
            rdr->_rowNum += rows_being_rendered;
        }

        // emit fractional bottom line
        if (bfrac) {
            if (rdr->_genPaint) {
                size_t l = (x_to - x_from + 1);
                ALLOC3(rdr->_paint, jint, l);
                rdr->_genPaint(rdr, 1);
            }
            rdr->_emitLine(rdr, 1, bfrac);
        }
    }
}

/*
 * JPiscesRenderer.c fillAlphaMask with the JNI surface/array handling and the exception tail
 * removed. [minX, maxX] x [minY, maxY] is the already-clipped destination rectangle; `offset` is
 * the index of the first mask byte for (minX, minY), computed by the callers as the JNI code did.
 */
static void
psw_fill_alpha_mask(Renderer* rdr, Surface* surface,
                    jint minX, jint minY, jint maxX, jint maxY,
                    jint maskType, const uint8_t* mask,
                    jint x, jint maskWidth, jint maskHeight, jint offset)
{
    jint rowsToBeRendered, rowsBeingRendered;
    jint width = maxX - minX + 1;
    jint height = maxY - minY + 1;

    // the mask is read-only for the renderer; renderer_setMask just stores the pointer
    renderer_setMask(rdr, maskType, (jbyte*) mask, maskWidth, maskHeight, XNI_FALSE);

    INVALIDATE_RENDERER_SURFACE(rdr);
    VALIDATE_BLITTING(rdr);

    rdr->_minTouched = minX;
    rdr->_maxTouched = maxX;
    rdr->_currX = minX;
    rdr->_currY = minY;

    rdr->_alphaWidth = width;

    rdr->_imageScanlineStride = surface->width;
    rdr->_imagePixelStride = 1;
    rdr->_rowNum = 0;
    rdr->_maskOffset = offset;

    rowsToBeRendered = height;

    while (rowsToBeRendered > 0) {
        rowsBeingRendered = 1; //MIN(rowsToBeRendered, NUM_ALPHA_ROWS);

        rdr->_currImageOffset = rdr->_currY * surface->width;
        if (rdr->_genPaint) {
            size_t l = (width * rowsBeingRendered);
            ALLOC3(rdr->_paint, jint, l);
            rdr->_genPaint(rdr, rowsBeingRendered);
        }
        rdr->_emitRows(rdr, rowsBeingRendered);

        rdr->_maskOffset += maskWidth;
        rdr->_rowNum += rowsBeingRendered;
        rowsToBeRendered -= rowsBeingRendered;
        rdr->_currX = x;
        rdr->_currY += rowsBeingRendered;
    }

    renderer_removeMask(rdr);
}

/* ------------------------------------------------------------------------------------------------
 * ABI version, layouts, constants
 * ---------------------------------------------------------------------------------------------- */

PRISM_SW_EXPORT uint32_t
psw_abi_version(void) {
    return PSW_ABI_VERSION;
}

PRISM_SW_EXPORT int32_t
psw_sizeof_transform6(void) {
    return (int32_t) sizeof(PswTransform6);
}

PRISM_SW_EXPORT int32_t
psw_constant(int32_t index) {
    if (index < 0 || index >= PSW_CONSTANT_COUNT) {
        return -1;
    }
    return psw_constants[index];
}

/* ------------------------------------------------------------------------------------------------
 * Surfaces
 * ---------------------------------------------------------------------------------------------- */

PRISM_SW_EXPORT void*
psw_surface_create(int32_t image_type, int32_t width, int32_t height) {
    Surface* surface;

    if (image_type != TYPE_INT_ARGB_PRE) {
        return NULL;
    }
    surface = my_malloc(Surface, 1);
    if (surface == NULL) {
        return NULL;
    }
    // same geometry the JNI JavaSurface_initialize fixed
    surface->width = width;
    surface->height = height;
    surface->offset = 0;
    surface->scanlineStride = width;
    surface->pixelStride = 1;
    surface->imageType = image_type;
    surface->data = NULL;
    surface->alphaData = NULL;
    return surface;
}

PRISM_SW_EXPORT void
psw_surface_dispose(void* surface) {
    if (surface != NULL) {
        surface_dispose((Surface*) surface);
    }
}

PRISM_SW_EXPORT int32_t
psw_surface_get_rgb(void* surface_handle, const int32_t* pixels,
                    int32_t* argb, int32_t argb_len,
                    int32_t offset, int32_t scan_length,
                    int32_t x, int32_t y, int32_t w, int32_t h)
{
    const jint dstX = 0;
    const jint dstY = 0;
    Surface* surface = (Surface*) surface_handle;
    jint surfaceWidth, surfaceHeight;

    if (surface == NULL) {
        return PSW_ERR_ARG;                                     // "Invalid surface"
    }
    surfaceWidth = surface->width;
    surfaceHeight = surface->height;
    if (x < 0 || x >= surfaceWidth ||
        y < 0 || y >= surfaceHeight ||
        w < 0 || w > (surfaceWidth - x) ||
        h < 0 || h > (surfaceHeight - y) ||
        scan_length < w || offset < 0) {
        return PSW_ERR_ARG;                                     // "Illegal arguments"
    }

    if ((w > 0) && (h > 0)) {
        jint dstStart, dstEnd;
        const jint* src;
        jint* dst;
        jint srcScanRest, dstScanRest;

        if (dstY > ((INT_MAX - offset - dstX) / scan_length)) {
            return PSW_ERR_ARG;                                 // "Out of bounds offset or scan length"
        }
        dstStart = offset + dstY * scan_length + dstX;
        if (scan_length > ((INT_MAX - dstStart) / h)) {
            return PSW_ERR_ARG;                                 // "Out of bounds offset or scan length"
        }
        dstEnd = dstStart + h * scan_length - 1;
        if ((dstStart < 0) || (dstStart >= argb_len) || (dstEnd < 0) || (dstEnd >= argb_len)) {
            return PSW_ERR_ARG;                                 // "Out of range access of buffer"
        }
        if (pixels == NULL || argb == NULL) {
            return PSW_ERR_ARG;
        }

        // the surface is read directly; nothing is bound, nothing to release
        src = pixels + y * surface->width + x;
        dst = argb + dstStart;
        srcScanRest = surface->width - w;
        dstScanRest = scan_length - w;
        for (; h > 0; --h) {
            jint w2 = w;
            for (; w2 > 0; --w2) {
                *dst++ = *src++;
            }
            src += srcScanRest;
            dst += dstScanRest;
        }
    }
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_surface_set_rgb(void* surface_handle, int32_t* pixels,
                    const int32_t* argb, int32_t argb_len,
                    int32_t offset, int32_t scan_length,
                    int32_t x, int32_t y, int32_t w, int32_t h)
{
    const jint srcX = 0;
    const jint srcY = 0;
    Surface* surface = (Surface*) surface_handle;
    jint surfaceWidth, surfaceHeight;

    if (surface == NULL) {
        return PSW_ERR_ARG;                                     // "Invalid surface"
    }
    surfaceWidth = surface->width;
    surfaceHeight = surface->height;
    if (x < 0 || x >= surfaceWidth ||
        y < 0 || y >= surfaceHeight ||
        w < 0 || w > (surfaceWidth - x) ||
        h < 0 || h > (surfaceHeight - y) ||
        scan_length < w || offset < 0) {
        return PSW_ERR_ARG;                                     // "Illegal arguments"
    }

    if ((w > 0) && (h > 0)) {
        jint srcStart, srcEnd;
        int32_t status;

        if (srcY > ((INT_MAX - offset - srcX) / scan_length)) {
            return PSW_ERR_ARG;                                 // "Out of bounds offset or scan length"
        }
        srcStart = offset + srcY * scan_length + srcX;
        if (scan_length > ((INT_MAX - srcStart) / h)) {
            return PSW_ERR_ARG;                                 // "Out of bounds offset or scan length"
        }
        srcEnd = srcStart + h * scan_length - 1;
        if ((srcStart < 0) || (srcStart >= argb_len) || (srcEnd < 0) || (srcEnd >= argb_len)) {
            return PSW_ERR_ARG;                                 // "out of range access of buffer"
        }
        if (argb == NULL) {
            return PSW_ERR_ARG;
        }

        status = psw_surface_bind(surface, pixels);
        if (status != PSW_OK) {
            return status;
        }
        // surface_setRGB only reads from its data argument
        surface_setRGB(surface, x, y, w, h, (jint*) (argb + srcStart), scan_length);
        psw_surface_unbind(surface);
    }
    return psw_finish();
}

/* ------------------------------------------------------------------------------------------------
 * Renderer lifecycle and state
 * ---------------------------------------------------------------------------------------------- */

PRISM_SW_EXPORT void*
psw_renderer_create(void* surface) {
    Renderer* rdr;

    if (surface == NULL) {
        return NULL;
    }
    rdr = renderer_create((Surface*) surface);
    if (readAndClearMemErrorFlag() == XNI_TRUE || rdr == NULL) {
        if (rdr != NULL) {
            renderer_dispose(rdr);
        }
        return NULL;
    }
    return rdr;
}

PRISM_SW_EXPORT void
psw_renderer_dispose(void* rdr) {
    if (rdr != NULL) {
        renderer_dispose((Renderer*) rdr);
    }
}

PRISM_SW_EXPORT int32_t
psw_renderer_set_clip(void* rdr, int32_t min_x, int32_t min_y, int32_t w, int32_t h) {
    if (rdr == NULL) {
        return PSW_ERR_STATE;
    }
    renderer_setClip((Renderer*) rdr, min_x, min_y, w, h);
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_renderer_set_color(void* rdr, int32_t r, int32_t g, int32_t b, int32_t a) {
    if (rdr == NULL) {
        return PSW_ERR_STATE;
    }
    renderer_setColor((Renderer*) rdr, r, g, b, a);
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_renderer_set_composite_rule(void* rdr, int32_t rule) {
    if (rdr == NULL) {
        return PSW_ERR_STATE;
    }
    renderer_setCompositeRule((Renderer*) rdr, rule);
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_renderer_set_linear_gradient(void* rdr, int32_t x0, int32_t y0, int32_t x1, int32_t y1,
                                 const int32_t* colors, int32_t colors_len,
                                 int32_t cycle_method, const PswTransform6* tx)
{
    Renderer* r = (Renderer*) rdr;
    Transform6 gradientTransform;

    if (r == NULL) {
        return PSW_ERR_STATE;
    }
    if (colors == NULL || tx == NULL || colors_len < GRADIENT_MAP_SIZE) {
        return PSW_ERR_ARG;
    }
    psw_transform_copy(tx, &gradientTransform);
    r->_gradient_cycleMethod = cycle_method;
    // the ramp is memcpy'd into the renderer; the cast only drops const for the legacy signature
    renderer_setLinearGradient(r, x0, y0, x1, y1, (jint*) colors, &gradientTransform);
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_renderer_set_radial_gradient(void* rdr, int32_t cx, int32_t cy, int32_t fx, int32_t fy,
                                 int32_t radius, const int32_t* colors, int32_t colors_len,
                                 int32_t cycle_method, const PswTransform6* tx)
{
    Renderer* r = (Renderer*) rdr;
    Transform6 gradientTransform;

    if (r == NULL) {
        return PSW_ERR_STATE;
    }
    if (colors == NULL || tx == NULL || colors_len < GRADIENT_MAP_SIZE) {
        return PSW_ERR_ARG;
    }
    psw_transform_copy(tx, &gradientTransform);
    r->_gradient_cycleMethod = cycle_method;
    renderer_setRadialGradient(r, cx, cy, fx, fy, radius, (jint*) colors, &gradientTransform);
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_renderer_set_texture(void* rdr, int32_t image_type,
                         const int32_t* data, int32_t data_len,
                         int32_t w, int32_t h, int32_t stride,
                         const PswTransform6* tx,
                         int32_t repeat, int32_t linear_filtering, int32_t has_alpha)
{
    Renderer* r = (Renderer*) rdr;
    Transform6 textureTransform;
    const jint* src = NULL;

    (void) image_type;      // TYPE_INT_ARGB_PRE only; the JNI code did not consult it either

    if (r == NULL) {
        return PSW_ERR_STATE;
    }
    if (data == NULL || tx == NULL) {
        return PSW_ERR_ARG;
    }

    // Same guard as the JNI-era setTextureImpl (in plain int arithmetic, which
    // is equivalent because w and h have been checked positive first). A failed guard leaves `src`
    // NULL and ends in the OOM status, exactly as the JNI code fell through to setMemErrorFlag().
    if (w > 0 && h > 0 && w < (INT_MAX / h) / (jint) sizeof(jint)
        && stride > 0 && (h - 1) <= (data_len - w) / stride) {
        psw_transform_copy(tx, &textureTransform);
        src = data;
    }
    if (src != NULL) {
        jint* alloc_data = my_malloc(jint, w * h);
        if (alloc_data != NULL) {
            if (stride == w) {
                memcpy(alloc_data, src, sizeof(jint) * w * h);
            } else {
                jint i;
                for (i = 0; i < h; i++) {
                    memcpy(alloc_data + (i * w), src + (i * stride), sizeof(jint) * w);
                }
            }
            renderer_setTexture(r, IMAGE_MODE_NORMAL,
                                alloc_data, w, h, w,
                                (jboolean) (repeat != 0), (jboolean) (linear_filtering != 0),
                                &textureTransform, XNI_TRUE, (jboolean) (has_alpha != 0),
                                0, 0, w - 1, h - 1);
        } else {
            setMemErrorFlag();
        }
    } else {
        setMemErrorFlag();
    }
    return psw_finish();
}

/* ------------------------------------------------------------------------------------------------
 * Renderer pixel-touching operations
 * ---------------------------------------------------------------------------------------------- */

PRISM_SW_EXPORT int32_t
psw_renderer_clear_rect(void* rdr, int32_t* pixels, int32_t x, int32_t y, int32_t w, int32_t h) {
    Renderer* r = (Renderer*) rdr;
    Surface* surface;
    int32_t status;

    if (r == NULL) {
        return PSW_ERR_STATE;
    }
    surface = r->_surface;
    status = psw_surface_bind(surface, pixels);
    if (status != PSW_OK) {
        return status;
    }
    INVALIDATE_RENDERER_SURFACE(r);

    r->_imagePixelStride = 1;
    r->_imageScanlineStride = surface->width;
    renderer_clearRect(r, x, y, w, h);

    psw_renderer_unbind(r);
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_renderer_fill_rect(void* rdr, int32_t* pixels, int32_t x, int32_t y, int32_t w, int32_t h) {
    Renderer* r = (Renderer*) rdr;
    Surface* surface;
    int32_t status;

    if (r == NULL) {
        return PSW_ERR_STATE;
    }
    surface = r->_surface;
    status = psw_surface_bind(surface, pixels);
    if (status != PSW_OK) {
        return status;
    }
    psw_fill_rect(r, surface, x, y, w, h,
                  IMAGE_FRAC_EDGE_KEEP, IMAGE_FRAC_EDGE_KEEP,
                  IMAGE_FRAC_EDGE_KEEP, IMAGE_FRAC_EDGE_KEEP);
    psw_renderer_unbind(r);
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_renderer_emit_and_clear_alpha_row(void* rdr, int32_t* pixels,
                                      const uint8_t* alpha_map, int32_t* alpha_deltas,
                                      int32_t y, int32_t x_from, int32_t x_to,
                                      int32_t x_off, int32_t row_num)
{
    Renderer* r = (Renderer*) rdr;
    Surface* surface;
    int32_t status;

    if (r == NULL) {
        return PSW_ERR_STATE;
    }
    if (alpha_map == NULL || alpha_deltas == NULL) {
        return PSW_ERR_ARG;
    }
    surface = r->_surface;
    status = psw_surface_bind(surface, pixels);
    if (status != PSW_OK) {
        return status;
    }
    INVALIDATE_RENDERER_SURFACE(r);
    VALIDATE_BLITTING(r);

    x_from = MAX(x_from, r->_clip_bbMinX);
    x_to = MIN(x_to, r->_clip_bbMaxX);

    if (x_to >= x_from &&
        y >= r->_clip_bbMinY &&
        y <= r->_clip_bbMaxY)
    {
        r->_minTouched = x_from;
        r->_maxTouched = x_to;
        r->_currX = x_from;
        r->_currY = y;

        r->_rowNum = row_num;

        r->alphaMap = (jbyte*) alpha_map;
        r->_rowAAInt = alpha_deltas + x_off; /* add offset in alpha buffer */
        r->_alphaWidth = x_to - x_from + 1;

        r->_currImageOffset = y * surface->width;
        r->_imageScanlineStride = surface->width;
        r->_imagePixelStride = 1;

        if (r->_genPaint) {
            size_t l = (x_to - x_from + 1);
            ALLOC3(r->_paint, jint, l);
            r->_genPaint(r, 1);
        }
        // the no-mask blits consume _rowAAInt and zero it in place (PiscesBlit.c)
        r->_emitRows(r, 1);
        r->_rowAAInt = NULL;
    }
    // only the no-mask blits read alphaMap, and only in this call: do not keep the pointer
    r->alphaMap = NULL;

    psw_renderer_unbind(r);
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_renderer_fill_alpha_mask(void* rdr, int32_t* pixels, const uint8_t* mask,
                             int32_t x, int32_t y, int32_t mask_w, int32_t mask_h,
                             int32_t offset, int32_t stride)
{
    Renderer* r = (Renderer*) rdr;
    jint minX, minY, maxX, maxY;
    jint maskOffset;

    (void) stride;          // unused, as in the JNI-era fillAlphaMaskImpl

    if (r == NULL) {
        return PSW_ERR_STATE;
    }
    if (mask == NULL) {
        return PSW_ERR_ARG;
    }

    if (x <= -(INT_MAX - mask_w) || y <= -(INT_MAX - mask_h)) {
        return PSW_OK;
    }

    if (x >= INT_MAX - mask_w || y >= INT_MAX - mask_h) {
        return PSW_OK;
    }

    minX = MAX(x, r->_clip_bbMinX);
    minY = MAX(y, r->_clip_bbMinY);
    maxX = MIN(x + mask_w - 1, r->_clip_bbMaxX);
    maxY = MIN(y + mask_h - 1, r->_clip_bbMaxY);

    // offset, width, height and stride cannot be negative - checked in Java code.
    // below checks might be a bit excessive (probably won't happen because fillMaskAlpha()
    // min/max check will make maskOffset not be used at all) but better to be safe than sorry
    if (mask_w > 0 && (minY - y) >= (INT_MAX / mask_w)) {
        return PSW_OK;
    }

    if ((minX - x) >= INT_MAX - ((minY - y) * mask_w)) {
        return PSW_OK;
    }

    if (offset >= INT_MAX - ((minY - y) * mask_w + minX - x)) {
        return PSW_OK;
    }

    maskOffset = offset + (minY - y) * mask_w + minX - x;

    if (maxX >= minX && maxY >= minY) {
        Surface* surface = r->_surface;
        int32_t status = psw_surface_bind(surface, pixels);
        if (status != PSW_OK) {
            return status;
        }
        psw_fill_alpha_mask(r, surface, minX, minY, maxX, maxY, ALPHA_MASK, mask,
                            x, mask_w, mask_h, maskOffset);
        psw_renderer_unbind(r);
    }
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_renderer_fill_lcd_alpha_mask(void* rdr, int32_t* pixels, const uint8_t* mask,
                                 int32_t x, int32_t y, int32_t mask_w, int32_t mask_h,
                                 int32_t offset, int32_t stride)
{
    Renderer* r = (Renderer*) rdr;
    jint minX, minY, maxX, maxY;
    jint maskOffset;

    (void) stride;          // unused, as in the JNI-era fillLCDAlphaMaskImpl

    if (r == NULL) {
        return PSW_ERR_STATE;
    }
    if (mask == NULL) {
        return PSW_ERR_ARG;
    }

    if (x < -(INT_MAX - mask_w / 3) || y < -(INT_MAX - mask_h)) {
        return PSW_OK;
    }

    if (x >= INT_MAX - (mask_w / 3) || y >= INT_MAX - mask_h) {
        return PSW_OK;
    }

    minX = MAX(x, r->_clip_bbMinX);
    minY = MAX(y, r->_clip_bbMinY);
    maxX = MIN(x + (mask_w / 3) - 1, r->_clip_bbMaxX);
    maxY = MIN(y + mask_h - 1, r->_clip_bbMaxY);

    if (mask_w > 0 && (minY - y) >= (INT_MAX / mask_w)) {
        return PSW_OK;
    }

    if ((minX - x) >= INT_MAX / 3) {
        return PSW_OK;
    }

    if (((minX - x) * 3) >= INT_MAX - ((minY - y) * mask_w)) {
        return PSW_OK;
    }

    if (offset >= INT_MAX - ((minY - y) * mask_w + (minX - x) * 3)) {
        return PSW_OK;
    }

    maskOffset = offset + (minY - y) * mask_w + (minX - x) * 3;

    if (maxX >= minX && maxY >= minY) {
        Surface* surface = r->_surface;
        int32_t status = psw_surface_bind(surface, pixels);
        if (status != PSW_OK) {
            return status;
        }
        psw_fill_alpha_mask(r, surface, minX, minY, maxX, maxY, LCD_ALPHA_MASK, mask,
                            x, mask_w, mask_h, maskOffset);
        psw_renderer_unbind(r);
    }
    return psw_finish();
}

PRISM_SW_EXPORT int32_t
psw_renderer_draw_image(void* rdr, int32_t* pixels,
                        int32_t image_type, int32_t image_mode,
                        const int32_t* data, int32_t w, int32_t h,
                        int32_t offset, int32_t stride,
                        const PswTransform6* tx,
                        int32_t repeat, int32_t linear_filtering,
                        int32_t bbox_x, int32_t bbox_y, int32_t bbox_w, int32_t bbox_h,
                        int32_t l_edge, int32_t r_edge, int32_t t_edge, int32_t b_edge,
                        int32_t tx_min, int32_t ty_min, int32_t tx_max, int32_t ty_max,
                        int32_t has_alpha)
{
    Renderer* r = (Renderer*) rdr;
    Surface* surface;
    Transform6 textureTransform;
    int32_t status;

    (void) image_type;      // TYPE_INT_ARGB_PRE only; the JNI code did not consult it either

    if (r == NULL) {
        return PSW_ERR_STATE;
    }
    if (data == NULL || tx == NULL) {
        return PSW_ERR_ARG;
    }
    surface = r->_surface;
    status = psw_surface_bind(surface, pixels);
    if (status != PSW_OK) {
        return status;
    }

    psw_transform_copy(tx, &textureTransform);
    // the texture is sampled in place (freeData == XNI_FALSE) and detached again below
    renderer_setTexture(r, image_mode, (jint*) data + offset, w, h, stride,
                        (jboolean) (repeat != 0), (jboolean) (linear_filtering != 0),
                        &textureTransform, XNI_FALSE, (jboolean) (has_alpha != 0),
                        tx_min, ty_min, tx_max, ty_max);

    psw_fill_rect(r, surface,
                  bbox_x, bbox_y, bbox_w, bbox_h,
                  l_edge, r_edge, t_edge, b_edge);

    r->_texture_intData = NULL;

    psw_renderer_unbind(r);
    return psw_finish();
}

/* ------------------------------------------------------------------------------------------------
 * Process-global LCD gamma tables
 * ---------------------------------------------------------------------------------------------- */

PRISM_SW_EXPORT void
psw_lcd_gamma_set(float gamma) {
    initGammaArrays(gamma);
}
