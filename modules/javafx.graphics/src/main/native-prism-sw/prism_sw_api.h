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
 * prism_sw_api.h - the flat C ABI of the prism_sw library (the Pisces software compositor).
 *
 * This is the only surface com.sun.pisces binds through java.lang.foreign. It knows nothing
 * about the JVM: every function takes and returns <stdint.h> scalars, opaque void* handles or
 * plain pointers into caller-owned memory, and no function ever calls back. Everything the JNI
 * glue used to read from Java objects - the renderer and surface nativePtr, the surface pixel
 * array, the six Transform6 fields, array lengths - is an explicit parameter here.
 *
 * Memory ownership. The library never retains a pointer it was handed beyond the call that
 * received it: pixel-touching operations bind `pixels` to the surface on entry and clear it on
 * exit (the span the JNI ACQUIRE_SURFACE/RELEASE_SURFACE pair used to cover), gradient ramps and
 * textures set through psw_renderer_set_texture are copied, masks, alpha rows and drawImage
 * sources are used only while the call runs. The one caller-visible write-back is
 * psw_renderer_emit_and_clear_alpha_row zeroing `alpha_deltas` in place.
 *
 * Errors. Nothing is thrown across the boundary: functions return a PSW_* status. PSW_ERR_OOM is
 * the former process-global mem_Error_Flag, read and cleared at the end of every call that can
 * allocate; PSW_ERR_ARG is returned for exactly the conditions where the JNI code threw
 * IllegalArgumentException, plus NULL pointer arguments; PSW_ERR_STATE stands for the
 * IllegalStateException a not-yet-initialized peer produced and is returned for a NULL renderer
 * handle. The blits are integer-only and short, so every function here may be bound with
 * Linker.Option.critical(true) and heap segments.
 *
 * Threading. Like the JNI version, nothing is synchronized: a renderer and its surface belong to
 * one thread at a time, the LCD gamma tables are process-global, and the OOM flag is a plain
 * global - Prism drives this library from its single render thread.
 *
 * Transitional ABI. This psw_* surface is a behaviour-neutral replacement for the JNI glue of a
 * library that is pure computation throughout, hot loops included: it makes no OS call and
 * imports nothing beyond the C runtime. The whole library is a deletion candidate - a Java port -
 * once its parity gates are settled: the integer core (blits, masks, fill/clear, texture sampling,
 * get/set RGB) is PARITY exact against the golden harness; the gradient set-up and generators are
 * PARITY tolerance, with a bound still to be tested and accepted; the LCD gamma LUT built from
 * pow() is PARITY unknown.
 * Do not extend this ABI: new functionality belongs in Java, and anything added here only enlarges
 * the deletion diff.
 */

#ifndef PRISM_SW_API_H
#define PRISM_SW_API_H

#include <stdint.h>

#if defined(_WIN32)
#  define PRISM_SW_EXPORT __declspec(dllexport)
#else
#  define PRISM_SW_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* ------------------------------------------------------------------------------------------------
 * ABI version, status codes, constants, layouts
 * ---------------------------------------------------------------------------------------------- */

#define PSW_ABI_VERSION 1u

/*
 * Status codes. Java maps PSW_ERR_OOM -> OutOfMemoryError("Allocation of internal renderer buffer
 * failed."), PSW_ERR_ARG -> IllegalArgumentException, PSW_ERR_STATE -> IllegalStateException,
 * keeping the JNI-era messages.
 */
enum {
    PSW_OK        = 0,
    PSW_ERR_OOM   = 1,
    PSW_ERR_ARG   = 2,
    PSW_ERR_STATE = 3
};

/*
 * Same values as the constants of com.sun.pisces.RendererBase; the internal COMPOSITE_* / TYPE_* /
 * IMAGE_* macros of PiscesRenderer.h and PiscesSurface.h alias this enum. psw_constant() returns
 * the values in this declaration order so the Java-side test can prove the two sides agree.
 */
enum {
    PSW_COMPOSITE_CLEAR      = 0,
    PSW_COMPOSITE_SRC        = 1,
    PSW_COMPOSITE_SRC_OVER   = 2,
    PSW_TYPE_INT_ARGB_PRE    = 1,
    PSW_IMAGE_MODE_NORMAL    = 1,
    PSW_IMAGE_MODE_MULTIPLY  = 2,
    PSW_IMAGE_FRAC_EDGE_KEEP = 0,
    PSW_IMAGE_FRAC_EDGE_PAD  = 1,
    PSW_IMAGE_FRAC_EDGE_TRIM = 2
};
#define PSW_CONSTANT_COUNT 9

/*
 * S15.16 affine matrix. Field order is identical to com.sun.pisces.Transform6 and to the C
 * Transform6 (PiscesTransform.h); sizeof == 24, no padding. Java builds it as
 * MemorySegment.ofArray(new int[] {m00, m01, m10, m11, m02, m12}).
 */
typedef struct PswTransform6 {
    int32_t m00;
    int32_t m01;
    int32_t m10;
    int32_t m11;
    int32_t m02;
    int32_t m12;
} PswTransform6;

PRISM_SW_EXPORT uint32_t psw_abi_version(void);          /* PSW_ABI_VERSION */
PRISM_SW_EXPORT int32_t  psw_sizeof_transform6(void);    /* 24 */
PRISM_SW_EXPORT int32_t  psw_constant(int32_t index);    /* enum above in order; -1 if out of range */

/* ------------------------------------------------------------------------------------------------
 * Surfaces (replaces the JNI-era JavaSurface / AbstractSurface natives)
 * ---------------------------------------------------------------------------------------------- */

/*
 * Creates a surface descriptor of the given size. Only PSW_TYPE_INT_ARGB_PRE is supported; any
 * other type or an allocation failure returns NULL. The surface does not own pixels: every call
 * that touches them receives `int32_t* pixels`, width*height ints, row-major, stride == width,
 * offset 0 (as the JNI JavaSurface fixed them). Java guarantees pixels.length >= width*height
 * (JavaSurface constructor); C does not re-check that length, but a negative width or height
 * still fails every pixel-touching call with PSW_ERR_ARG as the JNI surface_acquire did.
 */
PRISM_SW_EXPORT void*   psw_surface_create(int32_t image_type, int32_t width, int32_t height);
PRISM_SW_EXPORT void    psw_surface_dispose(void* surface);      /* NULL is ignored */

/*
 * Copy a w x h block of surface pixels to/from `argb` (argb_len ints), starting at argb[offset]
 * with row pitch scan_length. NULL surface -> PSW_ERR_ARG ("Invalid surface"); the same range
 * checks as the JNI code -> PSW_ERR_ARG ("Illegal arguments", "Out of bounds offset or scan
 * length", "Out of range access of buffer"). A zero-area block is a no-op returning PSW_OK.
 */
PRISM_SW_EXPORT int32_t psw_surface_get_rgb(void* surface, const int32_t* pixels,
                                            int32_t* argb, int32_t argb_len,
                                            int32_t offset, int32_t scan_length,
                                            int32_t x, int32_t y, int32_t w, int32_t h);
PRISM_SW_EXPORT int32_t psw_surface_set_rgb(void* surface, int32_t* pixels,
                                            const int32_t* argb, int32_t argb_len,
                                            int32_t offset, int32_t scan_length,
                                            int32_t x, int32_t y, int32_t w, int32_t h);

/* ------------------------------------------------------------------------------------------------
 * Renderer (replaces the JNI-era PiscesRenderer natives)
 * ---------------------------------------------------------------------------------------------- */

/*
 * Creates a renderer bound to `surface` (NULL surface or allocation failure -> NULL). The surface
 * must outlive the renderer; psw_renderer_dispose never touches surface memory, so the two dispose
 * calls may run in either order, as the Java disposer records require.
 */
PRISM_SW_EXPORT void*   psw_renderer_create(void* surface);
PRISM_SW_EXPORT void    psw_renderer_dispose(void* rdr);        /* NULL is ignored */

/* State setters. Integer (not S15.16) surface coordinates for the clip. */
PRISM_SW_EXPORT int32_t psw_renderer_set_clip(void* rdr, int32_t min_x, int32_t min_y, int32_t w, int32_t h);
PRISM_SW_EXPORT int32_t psw_renderer_set_color(void* rdr, int32_t r, int32_t g, int32_t b, int32_t a);
PRISM_SW_EXPORT int32_t psw_renderer_set_composite_rule(void* rdr, int32_t rule);

/*
 * Gradients. `colors` is the 256-entry ramp (GradientColorMap.colors); colors_len < 256 ->
 * PSW_ERR_ARG. The ramp is copied. Coordinates and radius are S15.16. `tx` is copied too.
 */
PRISM_SW_EXPORT int32_t psw_renderer_set_linear_gradient(void* rdr, int32_t x0, int32_t y0, int32_t x1, int32_t y1,
                                                         const int32_t* colors, int32_t colors_len,
                                                         int32_t cycle_method, const PswTransform6* tx);
PRISM_SW_EXPORT int32_t psw_renderer_set_radial_gradient(void* rdr, int32_t cx, int32_t cy, int32_t fx, int32_t fy,
                                                         int32_t radius, const int32_t* colors, int32_t colors_len,
                                                         int32_t cycle_method, const PswTransform6* tx);

/*
 * Texture paint. The texture (w x h ints starting at data[0], row pitch `stride`, data_len ints in
 * all) is copied into a buffer the renderer owns, so `data` may be released on return. `image_type`
 * is accepted for symmetry with drawImage and is not consulted (INT_ARGB_PRE only), exactly as the
 * JNI code ignored it. `repeat`, `linear_filtering` and `has_alpha` are booleans compared != 0.
 * The JNI-era dimension guard is kept unchanged - w > 0, h > 0, stride > 0, w * h * sizeof(int32_t)
 * below INT_MAX, (h - 1) * stride + w <= data_len - and when it fails this returns PSW_ERR_OOM, not
 * PSW_ERR_ARG, because the JNI setTextureImpl fell through to setMemErrorFlag() there. The Java
 * inputImageCheck rejects only negative sizes, stride < w and undersized arrays, so a texture with
 * w == 0 or h == 0 DOES reach this guard: it returns PSW_ERR_OOM and Java raises OutOfMemoryError.
 */
PRISM_SW_EXPORT int32_t psw_renderer_set_texture(void* rdr, int32_t image_type,
                                                 const int32_t* data, int32_t data_len,
                                                 int32_t w, int32_t h, int32_t stride,
                                                 const PswTransform6* tx,
                                                 int32_t repeat, int32_t linear_filtering, int32_t has_alpha);

/*
 * Pixel-touching operations. `pixels` is the surface array, bound for this call only.
 */

/* Integer surface coordinates, clipped to the surface and the clip rect. */
PRISM_SW_EXPORT int32_t psw_renderer_clear_rect(void* rdr, int32_t* pixels,
                                                int32_t x, int32_t y, int32_t w, int32_t h);

/* S15.16 coordinates; fractional edges are anti-aliased (IMAGE_FRAC_EDGE_KEEP on all four sides). */
PRISM_SW_EXPORT int32_t psw_renderer_fill_rect(void* rdr, int32_t* pixels,
                                               int32_t x, int32_t y, int32_t w, int32_t h);

/*
 * Composites one Marlin coverage row. `alpha_deltas` (read from index x_off) is consumed AND zeroed
 * in place - it must be the live delta array, not a copy, because SWContext relies on the zeroing.
 * `alpha_map` maps accumulated coverage to 0..255. Rows outside the clip are skipped (PSW_OK).
 */
PRISM_SW_EXPORT int32_t psw_renderer_emit_and_clear_alpha_row(void* rdr, int32_t* pixels,
                                                              const uint8_t* alpha_map, int32_t* alpha_deltas,
                                                              int32_t y, int32_t x_from, int32_t x_to,
                                                              int32_t x_off, int32_t row_num);

/*
 * Glyph masks: 8-bit coverage (mask_w x mask_h bytes, first byte at mask[offset]) composited at
 * integer position (x, y) with the current paint. The LCD variant takes three subpixel bytes per
 * output pixel, i.e. covers mask_w/3 pixels. `stride` is accepted and unused, as in the JNI code
 * (rows advance by mask_w). Both silently skip (PSW_OK) the overflow-prone geometries the JNI code
 * returned early on.
 */
PRISM_SW_EXPORT int32_t psw_renderer_fill_alpha_mask(void* rdr, int32_t* pixels, const uint8_t* mask,
                                                     int32_t x, int32_t y, int32_t mask_w, int32_t mask_h,
                                                     int32_t offset, int32_t stride);
PRISM_SW_EXPORT int32_t psw_renderer_fill_lcd_alpha_mask(void* rdr, int32_t* pixels, const uint8_t* mask,
                                                         int32_t x, int32_t y, int32_t mask_w, int32_t mask_h,
                                                         int32_t offset, int32_t stride);

/*
 * Draws `data` (w x h ints, first pixel at data[offset], row pitch `stride`) through `tx` into the
 * S15.16 box (bbox_x, bbox_y, bbox_w, bbox_h) with the given per-edge fraction modes
 * (PSW_IMAGE_FRAC_EDGE_*) and texture sampling bounds (tx_min..ty_max, texel coordinates). The
 * texture is NOT copied: it is sampled while the call runs and detached before return.
 * `image_type` is accepted and ignored as in the JNI code; `image_mode` is PSW_IMAGE_MODE_*.
 */
PRISM_SW_EXPORT int32_t psw_renderer_draw_image(void* rdr, int32_t* pixels,
                                                int32_t image_type, int32_t image_mode,
                                                const int32_t* data, int32_t w, int32_t h,
                                                int32_t offset, int32_t stride,
                                                const PswTransform6* tx,
                                                int32_t repeat, int32_t linear_filtering,
                                                int32_t bbox_x, int32_t bbox_y, int32_t bbox_w, int32_t bbox_h,
                                                int32_t l_edge, int32_t r_edge, int32_t t_edge, int32_t b_edge,
                                                int32_t tx_min, int32_t ty_min, int32_t tx_max, int32_t ty_max,
                                                int32_t has_alpha);

/*
 * Rebuilds the process-global LCD gamma tables (PiscesBlit.c) if `gamma` differs from the current
 * one. The JNI version ignored `this`; Prism calls it from the render thread.
 */
PRISM_SW_EXPORT void    psw_lcd_gamma_set(float gamma);

#ifdef __cplusplus
}
#endif

#endif /* PRISM_SW_API_H */
