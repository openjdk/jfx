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

#include <JNIUtil.h>

#include <JAbstractSurface.h>
#include <JPiscesRenderer.h>
#include <JTransform.h>

#include <PiscesBlit.h>
#include <PiscesSysutils.h>

#include <PiscesRenderer.inl>

#define RENDERER_NATIVE_PTR 0
#define RENDERER_SURFACE 1
#define RENDERER_LAST RENDERER_SURFACE

#define SURFACE_FROM_RENDERER(surface, env, surfaceHandle, rendererHandle)     \
        (surfaceHandle) = (*(env))->GetObjectField((env), (rendererHandle),    \
                                                   fieldIds[RENDERER_SURFACE]  \
                                                   );                          \
        (surface) = &surface_get((env), (surfaceHandle))->super;

static jfieldID fieldIds[RENDERER_LAST + 1];
static jboolean fieldIdsInitialized = JNI_FALSE;
static jboolean initializeRendererFieldIds(JNIEnv *env, jobject objectHandle);

static int toPiscesCoords(unsigned int ff);
static void renderer_finalize(JNIEnv *env, jobject objectHandle);
static void fillAlphaMask(Renderer* rdr, jint minX, jint minY, jint maxX, jint maxY,
    JNIEnv *env, jobject this, jint maskType, jbyteArray jmask, jint x, jint y,
    jint maskWidth, jint maskHeight, jint offset, jint stride);

JNIEXPORT void JNICALL
Java_com_sun_pisces_PiscesRenderer_initialize(JNIEnv* env, jobject objectHandle)
{
    Renderer* rdr;
    Surface* surface;
    jboolean sfieldsOK;

    sfieldsOK = initializeRendererFieldIds(env, objectHandle);
    if (sfieldsOK) {
        jobject surfaceHandle = (*env)->GetObjectField(env, objectHandle,
                                fieldIds[RENDERER_SURFACE]);
        surface = &surface_get(env, surfaceHandle)->super;

        rdr = renderer_create(surface);

        (*env)->SetLongField(env, objectHandle, fieldIds[RENDERER_NATIVE_PTR],
                             PointerToJLong(rdr));
        if (JNI_TRUE == readAndClearMemErrorFlag()) {
            JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                         "Allocation of internal renderer buffer failed!!!");
        }

    } else {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "");
    }
}

JNIEXPORT void JNICALL
Java_com_sun_pisces_PiscesRenderer_nativeFinalize(JNIEnv* env,
                                                  jobject objectHandle)
{
    renderer_finalize(env, objectHandle);

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                     "Allocation of internal renderer buffer failed.");
    }
}

JNIEXPORT void JNICALL
Java_com_sun_pisces_PiscesRenderer_setClipImpl(JNIEnv* env, jobject objectHandle,
        jint minX, jint minY, jint width, jint height) {
    Renderer* rdr;
    rdr = (Renderer*)JLongToPointer(
              (*env)->GetLongField(env, objectHandle,
                                   fieldIds[RENDERER_NATIVE_PTR]));

    renderer_setClip(rdr, minX, minY, width, height);

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                     "Allocation of internal renderer buffer failed.");
    }
}

JNIEXPORT void JNICALL
Java_com_sun_pisces_PiscesRenderer_setColorImpl(JNIEnv* env, jobject objectHandle,
        jint red, jint green, jint blue, jint alpha) {
    Renderer* rdr;
    rdr = (Renderer*)JLongToPointer(
              (*env)->GetLongField(env, objectHandle,
                                   fieldIds[RENDERER_NATIVE_PTR]));

    renderer_setColor(rdr, red, green, blue, alpha);

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                     "Allocation of internal renderer buffer failed.");
    }
}

JNIEXPORT void JNICALL
Java_com_sun_pisces_PiscesRenderer_setCompositeRuleImpl(JNIEnv* env,
    jobject objectHandle,
    jint compositeRule)
{
    Renderer* rdr;
    rdr = (Renderer*)JLongToPointer(
              (*env)->GetLongField(env, objectHandle,
                                   fieldIds[RENDERER_NATIVE_PTR]));

    renderer_setCompositeRule(rdr, compositeRule);

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                     "Allocation of internal renderer buffer failed.");
    }
}

JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_clearRectImpl(JNIEnv* env, jobject objectHandle,
        jint x, jint y, jint w, jint h) {
    Renderer* rdr;
    Surface* surface;
    jobject surfaceHandle;

    rdr = (Renderer*)JLongToPointer(
             (*env)->GetLongField(env, objectHandle,
                                   fieldIds[RENDERER_NATIVE_PTR]));

    SURFACE_FROM_RENDERER(surface, env, surfaceHandle, objectHandle);
    ACQUIRE_SURFACE(surface, env, surfaceHandle);
    INVALIDATE_RENDERER_SURFACE(rdr);
    
    rdr->_imagePixelStride = 1;
    rdr->_imageScanlineStride = surface->width;
    renderer_clearRect(rdr, x, y, w, h);

    RELEASE_SURFACE(surface, env, surfaceHandle);

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                     "Allocation of internal renderer buffer failed.");
    }
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    setLinearGradientImpl
 * Signature: (IIII[IILcom/sun/pisces/Transform6;)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_setLinearGradientImpl(
    JNIEnv *env, jobject this, jint x0, jint y0, jint x1, jint y1,
    jintArray jramp, jint cycleMethod, jobject jTransform)
{
    Renderer* rdr;
    Transform6 gradientTransform;
    jint *ramp;

    transform_get6(&gradientTransform, env, jTransform);

    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this,
                                    fieldIds[RENDERER_NATIVE_PTR]));

    ramp = (*env)->GetIntArrayElements(env, jramp, NULL);
    if (ramp != NULL) {
        rdr->_gradient_cycleMethod = cycleMethod;
        renderer_setLinearGradient(rdr, x0, y0, x1, y1,
                                   ramp, &gradientTransform);
        (*env)->ReleaseIntArrayElements(env, jramp, ramp, 0);
    } else {
        setMemErrorFlag();
    }

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                     "Allocation of internal renderer buffer failed.");
    }
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    setRadialGradientImpl
 * Signature: (IIIII[IILcom/sun/pisces/Transform6;)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_setRadialGradientImpl(
    JNIEnv *env, jobject this, jint cx, jint cy, jint fx, jint fy, jint radius,
    jintArray jramp, jint cycleMethod, jobject jTransform)
{
    Renderer* rdr;
    Transform6 gradientTransform;

    jint *ramp;

    transform_get6(&gradientTransform, env, jTransform);

    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this,
                                    fieldIds[RENDERER_NATIVE_PTR]));

    ramp = (*env)->GetIntArrayElements(env, jramp, NULL);
    if (ramp != NULL) {
        rdr->_gradient_cycleMethod = cycleMethod;
        renderer_setRadialGradient(rdr, cx, cy, fx, fy, radius,
                                   ramp, &gradientTransform);
        (*env)->ReleaseIntArrayElements(env, jramp, ramp, 0);
    } else {
        setMemErrorFlag();
    }

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                     "Allocation of internal renderer buffer failed.");
    }
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    setTextureImpl
 * Signature: (I[IIILcom/sun/pisces/Transform6;Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_setTextureImpl
  (JNIEnv *env, jobject this, jint imageType, jintArray dataArray,
      jint width, jint height, jint stride,
      jobject jTransform, jboolean repeat, jboolean hasAlpha)
{
    Renderer* rdr;
    Transform6 textureTransform;
    jint *data;

    transform_get6(&textureTransform, env, jTransform);

    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this, fieldIds[RENDERER_NATIVE_PTR]));

    data = (jint*)(*env)->GetPrimitiveArrayCritical(env, dataArray, NULL);
    if (data != NULL) {
        jint *alloc_data = my_malloc(jint, width * height);
        if (alloc_data != NULL) {
            if (stride == width) {
                memcpy(alloc_data, data, sizeof(jint) * width * height);
            } else {
                jint i;
                for (i = 0; i < height; i++) {
                    memcpy(alloc_data + (i*width), data + (i*stride), sizeof(jint) * width);
                }
            }
            renderer_setTexture(rdr, IMAGE_MODE_NORMAL,
                alloc_data, width, height, width, repeat, JNI_TRUE,
                &textureTransform, JNI_TRUE, hasAlpha,
                0, 0, width-1, height-1);
        } else {
            setMemErrorFlag();
        }
        (*env)->ReleasePrimitiveArrayCritical(env, dataArray, data, 0);
    } else {
        setMemErrorFlag();
    }

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
            "Allocation of internal renderer buffer failed.");
    }
}

Renderer*
renderer_get(JNIEnv* env, jobject objectHandle) {
    return (Renderer*)JLongToPointer(
                (*env)->GetLongField(env, objectHandle,
                                     fieldIds[RENDERER_NATIVE_PTR]));
}

static void
renderer_finalize(JNIEnv *env, jobject objectHandle) {
    Renderer* rdr;

    if (!fieldIdsInitialized) {
        return;
    }

    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, objectHandle,
                                    fieldIds[RENDERER_NATIVE_PTR]));

    if (rdr != (Renderer*)0) {
        renderer_dispose(rdr);
        (*env)->SetLongField(env, objectHandle, fieldIds[RENDERER_NATIVE_PTR],
                         (jlong)0);
    }
}

static jboolean
initializeObjectFieldIds(JNIEnv *env,
    jobject objectHandle,
    const char * className,
    FieldDesc * fieldDesc,
    jfieldID * fieldIds,
    jboolean * initializedField)
{
    jboolean retVal;
    jclass classHandle;

    if (*initializedField) {
        return JNI_TRUE;
    }

    retVal = JNI_FALSE;

    if (objectHandle != 0) {
        classHandle = (*env)->GetObjectClass(env, objectHandle);
    } else if (className != 0) {
        classHandle = (*env)->FindClass(env, className);
        if (checkAndClearException(env)) return JNI_FALSE;
    } else {
        return JNI_FALSE;
    }

    if (initializeFieldIds(fieldIds, env, classHandle, fieldDesc)) {
        retVal = JNI_TRUE;
        *initializedField = JNI_TRUE;
    }

    return retVal;
}

static jboolean
initializeRendererFieldIds(JNIEnv *env, jobject objectHandle) {
    static FieldDesc rendererFieldDesc[] = {
                { "nativePtr", "J" },
                { "surface", "Lcom/sun/pisces/AbstractSurface;" },
                { NULL, NULL }
            };

    return initializeObjectFieldIds(env, objectHandle, 0, rendererFieldDesc,
        fieldIds, &fieldIdsInitialized);
}

/**
 * Converts floating point number into S15.16 format
 * [= (int)(f * 65536.0f)]. Doesn't correctly handle INF, NaN and -0.
 *
 * @param ff number encoded as sign [1 bit], exponent + 127 [8 bits], mantisa
 *           without the implicit 1 at the beginning [23 bits]
 * @return ff in S15.16 format
 */
static int
toPiscesCoords(unsigned int ff) {
    int shift;
    unsigned int gg;

    /* get mantisa */
    gg = ((ff & 0xffffff) | 0x800000);
    /* calculate shift from exponent */
    shift = 134 - ((ff >> 23) & 0xff);
    /* do left or right shift to get value to S15.16 format */
    gg = (shift < 0) ? (gg << -shift) : (gg >> shift);
    /* fix sign */
    gg = (gg ^ -(int)(ff >> 31)) + (ff >> 31);
    /* handle zero */
    gg &= -(ff != 0);

    return (int)gg;
}

static void
fillRect(JNIEnv *env, jobject this, Renderer* rdr,
    jint x, jint y, jint w, jint h,
    jint lEdge, jint rEdge, jint tEdge, jint bEdge)
{
    Surface* surface;
    jobject surfaceHandle;
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

        SURFACE_FROM_RENDERER(surface, env, surfaceHandle, this);
        ACQUIRE_SURFACE(surface, env, surfaceHandle);
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
        RELEASE_SURFACE(surface, env, surfaceHandle);

        if (JNI_TRUE == readAndClearMemErrorFlag()) {
            JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                "Allocation of internal renderer buffer failed.");
        }
    }
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    fillRect
 * Signature: (IIII)V
 * x, y, w, h are already transformed (is surface coordinates)
 * and rectangle is in an up-right position ie. no rotate or shear
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_fillRectImpl
  (JNIEnv *env, jobject this, jint x, jint y, jint w, jint h) 
{
    Renderer* rdr;
    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this, fieldIds[RENDERER_NATIVE_PTR]));
    fillRect(env, this, rdr, x, y, w, h,
        IMAGE_FRAC_EDGE_KEEP, IMAGE_FRAC_EDGE_KEEP,
        IMAGE_FRAC_EDGE_KEEP, IMAGE_FRAC_EDGE_KEEP);
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    emitAndClearAlphaRowImpl
 * Signature: ([B[IIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_emitAndClearAlphaRowImpl
  (JNIEnv *env, jobject this, jbyteArray jAlphaMap, jintArray jAlphaDeltas, jint y, jint x_from, jint x_to,
   jint rowNum)
{
    Renderer* rdr;
    Surface* surface;
    jobject surfaceHandle;
    jbyte* alphaMap;
    
    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this, fieldIds[RENDERER_NATIVE_PTR]));
    
    SURFACE_FROM_RENDERER(surface, env, surfaceHandle, this);
    ACQUIRE_SURFACE(surface, env, surfaceHandle);
    INVALIDATE_RENDERER_SURFACE(rdr);
    VALIDATE_BLITTING(rdr);

    alphaMap = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, jAlphaMap, NULL);
    if (alphaMap != NULL)
    {
        jint* alphaRow = (jint*)(*env)->GetPrimitiveArrayCritical(env, jAlphaDeltas, NULL);
        if (alphaRow != NULL)
        {
            x_from = MAX(x_from, rdr->_clip_bbMinX);
            x_to = MIN(x_to, rdr->_clip_bbMaxX);

            if (x_to >= x_from &&
                y >= rdr->_clip_bbMinY &&
                y <= rdr->_clip_bbMaxY)
            {
                rdr->_minTouched = x_from;
                rdr->_maxTouched = x_to;
                rdr->_currX = x_from;
                rdr->_currY = y;

                rdr->_rowNum = rowNum;

                rdr->alphaMap = alphaMap;
                rdr->_rowAAInt = alphaRow;
                rdr->_alphaWidth = x_to - x_from + 1;

                rdr->_currImageOffset = y * surface->width;
                rdr->_imageScanlineStride = surface->width;
                rdr->_imagePixelStride = 1;

                if (rdr->_genPaint) {
                    size_t l = (x_to - x_from + 1);
                    ALLOC3(rdr->_paint, jint, l);
                    rdr->_genPaint(rdr, 1);
                }
                rdr->_emitRows(rdr, 1);
                rdr->_rowAAInt = NULL;
            }
            (*env)->ReleasePrimitiveArrayCritical(env, jAlphaDeltas, alphaRow, 0);
        } else {
            setMemErrorFlag();
        }
        (*env)->ReleasePrimitiveArrayCritical(env, jAlphaMap, alphaMap, 0);
    } else {
        setMemErrorFlag();
    }

    RELEASE_SURFACE(surface, env, surfaceHandle);

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
            "Allocation of internal renderer buffer failed.");
    }
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    drawImageImpl
 * Signature: (I[IIIIILcom/sun/pisces/Transform6;ZIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_drawImageImpl
(JNIEnv *env, jobject this, jint imageType, jint imageMode,
    jintArray dataArray, jint width, jint height, jint offset, jint stride,
    jobject jTransform, jboolean repeat, jint bboxX, jint bboxY, jint bboxW, jint bboxH,
    jint lEdge, jint rEdge, jint tEdge, jint bEdge,
    jint txMin, jint tyMin, jint txMax, jint tyMax,
    jboolean hasAlpha)
{
    Renderer* rdr;
    jint* data;

    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this, fieldIds[RENDERER_NATIVE_PTR]));
    data = (jint*)(*env)->GetPrimitiveArrayCritical(env, dataArray, NULL);
    if (data != NULL) {
        Transform6 textureTransform;

        transform_get6(&textureTransform, env, jTransform);
        renderer_setTexture(rdr, imageMode, data + offset, width, height, stride,
            repeat, JNI_TRUE, &textureTransform, JNI_FALSE, hasAlpha,
            txMin, tyMin, txMax, tyMax);

        fillRect(env, this, rdr,
            bboxX, bboxY, bboxW, bboxH,
            lEdge, rEdge, tEdge, bEdge);

        rdr->_texture_intData = NULL;
        (*env)->ReleasePrimitiveArrayCritical(env, dataArray, data, 0);
    } else {
        setMemErrorFlag();
    }

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                     "Allocation of internal renderer buffer failed.");
    }
    PISCES_DEBUG_FLUSH(stdout);
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    fillAlphaMaskImpl
 * Signature: ([BIIIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_fillAlphaMaskImpl
(JNIEnv *env, jobject this, jbyteArray jmask, jint x, jint y, jint maskWidth, jint maskHeight, jint offset, jint stride)
{
    Renderer* rdr;
    jint minX, minY, maxX, maxY;
    jint maskOffset;
    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this, fieldIds[RENDERER_NATIVE_PTR]));
    
    minX = MAX(x, rdr->_clip_bbMinX);
    minY = MAX(y, rdr->_clip_bbMinY);
    maxX = MIN(x + maskWidth - 1, rdr->_clip_bbMaxX);
    maxY = MIN(y + maskHeight - 1, rdr->_clip_bbMaxY);
    
    maskOffset = offset + (minY - y) * maskWidth + minX - x;
    
    fillAlphaMask(rdr, minX, minY, maxX, maxY, env, this, ALPHA_MASK, jmask,
        x, y, maskWidth, maskHeight, maskOffset, stride);
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    setLCDGammaCorrection
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_setLCDGammaCorrectionImpl
(JNIEnv *env, jobject this, jfloat gamma)
{
    initGammaArrays(gamma);
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    fillLCDAlphaMaskImpl
 * Signature: ([BIIIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_fillLCDAlphaMaskImpl
(JNIEnv *env, jobject this, jbyteArray jmask, jint x, jint y,
    jint maskWidth, jint maskHeight,
    jint offset, jint stride)
{
    Renderer* rdr;
    jint minX, minY, maxX, maxY;
    jint maskOffset;
    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this, fieldIds[RENDERER_NATIVE_PTR]));

    minX = MAX(x, rdr->_clip_bbMinX);
    minY = MAX(y, rdr->_clip_bbMinY);
    maxX = MIN(x + (maskWidth/3) - 1, rdr->_clip_bbMaxX);
    maxY = MIN(y + maskHeight - 1, rdr->_clip_bbMaxY);

    maskOffset = offset + (minY - y) * maskWidth + (minX - x) * 3;

    fillAlphaMask(rdr, minX, minY, maxX, maxY, env, this, LCD_ALPHA_MASK, jmask,
        x, y, maskWidth, maskHeight, maskOffset, stride);
}

static void fillAlphaMask(Renderer* rdr, jint minX, jint minY, jint maxX, jint maxY,
    JNIEnv *env, jobject this, jint maskType, jbyteArray jmask, 
    jint x, jint y, jint maskWidth, jint maskHeight, jint offset, jint stride)
{
    jint rowsToBeRendered, rowsBeingRendered;
    
    Surface* surface;
    jobject surfaceHandle;
    
    if (maxX >= minX && maxY >= minY)
    {
        jbyte* mask;

        SURFACE_FROM_RENDERER(surface, env, surfaceHandle, this);
        ACQUIRE_SURFACE(surface, env, surfaceHandle);

        mask = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, jmask, NULL);
        if (mask != NULL) {
            jint width = maxX - minX + 1;
            jint height = maxY - minY + 1;

            renderer_setMask(rdr, maskType, mask, maskWidth, maskHeight, JNI_FALSE);

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
            (*env)->ReleasePrimitiveArrayCritical(env, jmask, mask, 0);
        } else {
            setMemErrorFlag();
        }

        RELEASE_SURFACE(surface, env, surfaceHandle);

        if (JNI_TRUE == readAndClearMemErrorFlag()) {
            JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                         "Allocation of internal renderer buffer failed.");
        }
    }
}

