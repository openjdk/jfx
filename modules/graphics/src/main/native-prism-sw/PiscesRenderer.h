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
 *  \file PiscesRenderer.h
 *  Renderer struct declaration. This file is the main PISCES include. I.e. all
 *  PISCES wrappers, should include this file. It provides C - high level API.
 */

#ifndef PISCES_RENDERER_H
#define PISCES_RENDERER_H

#ifdef ANDROID_NDK
#include <sys/types.h>
#endif
#include <PiscesDefs.h>
#include <PiscesSurface.h>
#include <PiscesTransform.h>

#include "com_sun_pisces_RendererBase.h"

/**
 * @defgroup CompositingRules Compositing rules supported by PISCES
 * When drawing two objects to one pixel area, there are several possibilities
 * how composite color is made of source and destination contributions.
 * Objects can overlap pixel fully and/or partialy. One object could be above
 * the second one and they both can be partialy or fully transparent (alpha).
 * The way, we count composite color and alpha from theirs contributions is
 * called composite rule (Porter-Duff).
 * @def COMPOSITE_CLEAR
 * @ingroup CompositingRules
 * Compositing rule COMPOSITE_CLEAR. This rule applied to destination pixel sets
 * its color to 0x00000000 - transparent black - regardless to source color.
 * @see renderer_setCompositeRule(Renderer *, jint),
 * renderer_setComposite(Renderer *, jint, jfloat)
 * @def COMPOSITE_SRC
 * @ingroup CompositingRules
 * Compositing rule COMPOSITE_SRC. This rule applied to destination pixel sets
 * its color to source color - regardless to previous color of destination
 * pixel.
 * @see renderer_setCompositeRule(Renderer *, jint),
 * renderer_setComposite(Renderer *, jint, jfloat)
 * @def COMPOSITE_SRC_OVER
 * @ingroup CompositingRules
 * Compositing rule COMPOSITE_SRC_OVER. This rule is kind of intuitive. When we
 * look through transparent green glass bottle at some object, we can see
 * mixture of glass and objects colors. Composite color is alpha-weigth average
 * of source and destination.
 * @see renderer_setCompositeRule(Renderer *, jint),
 * renderer_setComposite(Renderer *, jint, jfloat)
 */
//Compositing rules
#define COMPOSITE_CLEAR    com_sun_pisces_RendererBase_COMPOSITE_CLEAR
#define COMPOSITE_SRC      com_sun_pisces_RendererBase_COMPOSITE_SRC
#define COMPOSITE_SRC_OVER com_sun_pisces_RendererBase_COMPOSITE_SRC_OVER

/**
 * @defgroup WindingRules Winding rules - shape interior
 * Winding rule determines what part of shape is determined as interior. This is
 * important to determine what part of shape to fill.
 * @def WIND_EVEN_ODD
 * @ingroup WindingRules
 * This define represents the even-odd winding rule. To see how this winding
 * rule works, draw any closed shape and draw a line through the entire shape.
 * Each time the line crosses the shape's border, increment a counter. When the
 * counter is even, the line is outside the shape. When the counter is odd,
 * the line is in the interior of the shape.
 * @def WIND_NON_ZERO
 * @ingroup WindingRules
 * This define represents the non-zero winding rule. Similar to even-odd.
 * We draw line through the entire shape. If intersecting edge is drawn from
 * left-to-right, we add 1 to counter. If it goes from right to left we add -1.
 * Everytime the counter is not zero, we assume it's interior part of shape.
 */
#define WIND_NON_ZERO 1
#define WIND_EVEN_ODD 0

//Paint methods
/**
 * @defgroup PaintMethods Paint methods in PISCES
 * Paint method says what source color should be used while filling shapes. We
 * can use solid color for every touched pixel, or we can use gradient-fills or
 * textures. Setting paint method you can draw any primitive (even line or oval)
 * filled with gradient or texture.
 * @see setColor
 * @see setFill
 * @def PAINT_FLAT_COLOR
 * @ingroup PaintMethods
 * Paint method uses flat color. Source color set by setColor() is used.
 * @see setColor
 * @def PAINT_LINEAR_GRADIENT
 * @ingroup PaintMethods
 * Paint method. Source color value is precalculated linear gradients color.
 * @see setLinearGradient
 * @def PAINT_RADIAL_GRADIENT
 * @ingroup PaintMethods
 * Paint method. Source color value is precalculated radial gradients color.
 * @see setRadialGradient
 * @def PAINT_TEXTURE8888
 * @def PAINT_TEXTURE565ALPHA
 * @def PAINT_TEXTURE565NOALPHA
 * @ingroup PaintMethods
 * Paint method. Source color value is texture.
 * @see setTexture
 */
#define PAINT_FLAT_COLOR 0
#define PAINT_LINEAR_GRADIENT 1
#define PAINT_RADIAL_GRADIENT 2
#define PAINT_TEXTURE8888 4
#define PAINT_TEXTURE8888_MULTIPLY 5

#define IMAGE_MODE_NORMAL   com_sun_pisces_RendererBase_IMAGE_MODE_NORMAL
#define IMAGE_MODE_MULTIPLY com_sun_pisces_RendererBase_IMAGE_MODE_MULTIPLY

#define IMAGE_FRAC_EDGE_KEEP com_sun_pisces_RendererBase_IMAGE_FRAC_EDGE_KEEP
#define IMAGE_FRAC_EDGE_PAD  com_sun_pisces_RendererBase_IMAGE_FRAC_EDGE_PAD
#define IMAGE_FRAC_EDGE_TRIM com_sun_pisces_RendererBase_IMAGE_FRAC_EDGE_TRIM

#define LG_GRADIENT_MAP_SIZE 8
#define GRADIENT_MAP_SIZE (1 << LG_GRADIENT_MAP_SIZE)

#define DEFAULT_INDICES_SIZE (8*292)
#define DEFAULT_CROSSINGS_SIZE (8*292*4)
#define NUM_ALPHA_ROWS 8
#define MIN_QUAD_OPT_WIDTH (100 << 16)

#define INVALID_RENDERER_SURFACE 16

/**
 * @defgroup CycleMethods Gradient cycle methods
 * Gradient cycle methods. Specifies wheteher to repeat gradient fill in cycle
 * or not. We will explain possible methods on linear gradient behaviour.
 * @see setLinearGradient, setRadialGradient
 * @def CYCLE_NONE
 * @ingroup CycleMethods
 * Gradient without repetition. Imagine linear gradient from blue to red color.
 * Color of start point (line perpendicular to vector(start,end)) will be blue.
 * Color of end point (line) will be red. Between these two points (lines),
 * there will be smooth color gradient. Outside gradient area everything will be
 * blue or red when CYCLE_NONE used. It works similar way with radial gradient.
 * @def CYCLE_REPEAT
 * @ingroup CycleMethods
 * Gradient with repetition. Gradient fill is repeated with period given by
 * start,end distance.
 * @def CYCLE_REFLECT
 * @ingroup CycleMethods
 * Gradient is repeated. Start and end color in new cycle are swaped. Gradient
 * fill is repeated with period given by start,end distance. You can imagine
 * this as if you'd put mirror to end point (line).
 */
#define CYCLE_NONE 0
#define CYCLE_REPEAT 1
#define CYCLE_REFLECT 2

#define NO_MASK 0
#define ALPHA_MASK 1
#define LCD_ALPHA_MASK 2

#define TEXTURE_TRANSFORM_IDENTITY 1
#define TEXTURE_TRANSFORM_TRANSLATE 2
#define TEXTURE_TRANSFORM_SCALE_TRANSLATE 3
#define TEXTURE_TRANSFORM_GENERIC 4

/**
 * \struct _Renderer
 * Structure _Renderer encapsulates rendering-state information. Colors,
 * textures, counted gradient-fills, transformation-matrices, compositing rule,
 * antialiasing, paint method, surface (destination of our drawing) and much
 * more is tracked by Renderer. Simply, we can say, Renderer knows HOW AND
 * WHERE TO DRAW. It is also typedefed as Renderer.
 * \typedef Renderer
 * Typedef to struct _Renderer.
 */
typedef struct _Renderer {

    // Flat color or (Java2D) linear gradient
    jint _paintMode;
    jint _prevPaintMode;

    // Current (internal) color
    jint _cred, _cgreen, _cblue, _calpha;

    /*
     * Color and alpha for gradient value g is located in
     * color map at index (int)(g*scale + bias)
     */
    jint _lgradient_color_888[GRADIENT_MAP_SIZE];

    jint _colorAlphaMap[16*16 + 1];
    jint _paintAlphaMap[256];

    /**
     * Switches antialiasing support ON/OFF.
     * @see To switch antialising ON/OFF, use
     * renderer_setAntialiasing(Renderer*, antialiasingOn).
     */
    jboolean _antialiasingOn;
    /**
     * Current compositing rule. Renderers internal variable. To change it use
     * renderer_setCompositeRule(Renderer *, jint) or
     * renderer_setComposite(Renderer *, jint, jfloat).
     * @see See CompositingRules, renderer_setCompositeRule(Renderer *, jint),
     * renderer_setComposite(Renderer *, jint, jfloat)
     */
    jint _compositeRule;

    Surface* _surface;

    // Image layout
    void *_data;
    jint _width, _height;
    jint _imageOffset;
    jint _imageScanlineStride;
    jint _imagePixelStride;
    jint _imageType;

    void (*_bl_SourceOverMask)(struct _Renderer *rdr, jint height);
    void (*_bl_PT_SourceOverMask)(struct _Renderer *rdr, jint height);
    void (*_bl_SourceMask)(struct _Renderer *rdr, jint height);
    void (*_bl_PT_SourceMask)(struct _Renderer *rdr, jint height);

    void (*_bl_SourceOverLCDMask)(struct _Renderer *rdr, jint height);
    void (*_bl_PT_SourceOverLCDMask)(struct _Renderer *rdr, jint height);
    void (*_bl_SourceLCDMask)(struct _Renderer *rdr, jint height);
    void (*_bl_PT_SourceLCDMask)(struct _Renderer *rdr, jint height);

    void (*_bl_SourceOverNoMask)(struct _Renderer *rdr, jint height);
    void (*_bl_PT_SourceOverNoMask)(struct _Renderer *rdr, jint height);
    void (*_bl_SourceNoMask)(struct _Renderer *rdr, jint height);
    void (*_bl_PT_SourceNoMask)(struct _Renderer *rdr, jint height);

    void (*_bl_SourceOver)(struct _Renderer *rdr, jint height);
    void (*_bl_PT_SourceOver)(struct _Renderer *rdr, jint height);
    void (*_bl_Source)(struct _Renderer *rdr, jint height);
    void (*_bl_PT_Source)(struct _Renderer *rdr, jint height);

    void (*_el_Source)(struct _Renderer *rdr, jint height, jint frac);
    void (*_el_SourceOver)(struct _Renderer *rdr, jint height, jint frac);
    void (*_el_PT_Source)(struct _Renderer *rdr, jint height, jint frac);
    void (*_el_PT_SourceOver)(struct _Renderer *rdr, jint height, jint frac);

    /**
     * Pointer to function which clears rectangle - ie. sets rectangle data to
     * transparent black. Implementations are optimized for concrete surface
     * types.
     * @param height height of blitting area
     * @see renderer_setCompositeRule()
     */
    void (*_bl_Clear)(struct _Renderer *rdr, jint height);
    void (*_bl_PT_Clear)(struct _Renderer *rdr, jint height);

    /**
     * Pointer to bliting function. Bliting function is set due to
     * composite rule and surface type to appropriate optimized function. _bl_SO
     * is called in paint mode PAINT_FLAT_COLOR - when filling with solid color.
     * @param height height of blitting area
     * @see renderer_setCompositeRule()
     */
    void (*_bl)(struct _Renderer *rdr, jint height);
    /** Pointer to paint bliting function. Bliting function is set due to
     * composite rule and surface type to appropriate optimized function.
     * _bl_PT_SO is called in paint mode different from PAINT_FLAT_COLOR, ie.
     * anytime when filling with gradients or textures.
     * @param height height of blitting area
     * @see renderer_setCompositeRule()
     */
    void (*_bl_PT)(struct _Renderer *rdr, jint height);

    void (*_el)(struct _Renderer *rdr, jint height, jint frac);
    void (*_el_PT)(struct _Renderer *rdr, jint height, jint frac);

    void (*_clearRect)(struct _Renderer *rdr, jint x, jint y, jint w, jint h);
    void (*_emitRows)(struct _Renderer *rdr, jint height);
    void (*_emitLine)(struct _Renderer *rdr, jint height, jint frac);
    void (*_genPaint)(struct _Renderer *rdr, jint height);

    jint _rowNum;
    jint _alphaWidth;
    jint _minTouched;
    jint _maxTouched;
    jint _currX, _currY;
    jint _currImageOffset;

    jbyte* alphaMap;
    jint* _rowAAInt;

    // used for fillRect call - contains original rectangle's X,Y values
    jint _rectX, _rectY;

    // Mask
    jboolean _mask_free;

    // Mask data
    jint _maskType;
    jbyte* _mask_byteData;
    jint _maskOffset;
    jint _mask_width;
    jint _mask_height;

    // Paint buffer
    jint *_paint;
    size_t _paint_length;

    // Paint transform
    Transform6 _paint_transform;

    // Gradient transform
    Transform6 _gradient_transform;
    Transform6 _gradient_inverse_transform;

    // New-style linear gradient geometry
    jfloat _lg_mx, _lg_my, _lg_b;         // g(x, y) = x*mx + y*my + b

    // Radial gradient geometry
    jfloat _rg_a00, _rg_a01, _rg_a02, _rg_a10, _rg_a11, _rg_a12;
    jfloat _rg_cx, _rg_cy, _rg_fx, _rg_fy, _rg_r, _rg_rsq;
    jfloat _rg_a00a00, _rg_a10a10, _rg_a00a10;

    // Gradient color map
    jint _gradient_colors[GRADIENT_MAP_SIZE];
    jint _gradient_cycleMethod;

    // Texture paint
    jint* _texture_intData;

    //hint to image rendering
    jboolean _texture_hasAlpha;

    // 565 convenience alternative to _texture_intData
    jbyte* _texture_byteData;
    jbyte* _texture_alphaData;

    jint _texture_renderMode;
    jint _texture_imageWidth;
    jint _texture_imageHeight;
    jint _texture_stride;
    jint _texture_interpolateMinX, _texture_interpolateMinY;
    jint _texture_interpolateMaxX, _texture_interpolateMaxY;
    jboolean _texture_repeat;
    jlong _texture_m00, _texture_m01, _texture_m02;
    jlong _texture_m10, _texture_m11, _texture_m12;
    // if XNI_TRUE, then we use linear interpolation for result pixel value calc.
    // otherwise nearest neighbour value is used
    jboolean _texture_interpolate;
    jint _texture_transformType;

    jboolean _texture_free;

    // Current bounding box for all primitives
    jint _clip_bbMinX;
    jint _clip_bbMinY;
    jint _clip_bbMaxX;
    jint _clip_bbMaxY;

    jint _el_lfrac, _el_rfrac;

    jint _rendererState;

}
Renderer;

#define INVALIDATE_RENDERER_SURFACE(rdr)        \
        (rdr)->_rendererState |= INVALID_RENDERER_SURFACE;

#endif
