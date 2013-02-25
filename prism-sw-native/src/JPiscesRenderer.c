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
    JNIEnv *env, jobject this, jint maskType, jbyteArray jmask, jint x, jint y, jint maskWidth, jint maskHeight, jint offset, jint stride);

JNIEXPORT void JNICALL
Java_com_sun_pisces_PiscesRenderer_initialize(JNIEnv* env,
                                              jobject objectHandle) {
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
Java_com_sun_pisces_PiscesRenderer_setClip(JNIEnv* env, jobject objectHandle,
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
Java_com_sun_pisces_PiscesRenderer_resetClip(JNIEnv* env,
        jobject objectHandle) {
    Renderer* rdr;
    rdr = (Renderer*)JLongToPointer(
              (*env)->GetLongField(env, objectHandle,
                                   fieldIds[RENDERER_NATIVE_PTR]));

    renderer_resetClip(rdr);

    if (JNI_TRUE == readAndClearMemErrorFlag()) {
        JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                     "Allocation of internal renderer buffer failed.");
    }
}

JNIEXPORT void JNICALL
Java_com_sun_pisces_PiscesRenderer_setColor(JNIEnv* env, jobject objectHandle,
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
Java_com_sun_pisces_PiscesRenderer_setCompositeRule(JNIEnv* env,
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

JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_clearRect(JNIEnv* env, jobject objectHandle,
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

    rdr->_gradient_cycleMethod = cycleMethod;
    renderer_setLinearGradient(rdr, x0, y0, x1, y1,
                               ramp, &gradientTransform);

    (*env)->ReleaseIntArrayElements(env, jramp, ramp, 0);
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

    rdr->_gradient_cycleMethod = cycleMethod;
    renderer_setRadialGradient(rdr, cx, cy, fx, fy, radius,
                               ramp, &gradientTransform);

    (*env)->ReleaseIntArrayElements(env, jramp, ramp, 0);
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    setTexture
 * Signature: (I[IIILcom/sun/pisces/Transform6;Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_setTexture
  (JNIEnv *env, jobject this, jint imageType, jintArray dataArray, jint width, jint height,
      jobject jTransform, jboolean repeat, jboolean hasAlpha)
{
    Renderer* rdr;
    Transform6 textureTransform;

    jint *data;
    jint *alloc_data;
    jboolean throw_exc = JNI_FALSE;

    transform_get6(&textureTransform, env, jTransform);

    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this, fieldIds[RENDERER_NATIVE_PTR]));

    data = (jint*)(*env)->GetPrimitiveArrayCritical(env, dataArray, NULL);

    alloc_data = my_malloc(jint, width * height);
    if (alloc_data != NULL) {
        memcpy(alloc_data, data, sizeof(jint) * width * height);
        renderer_setTexture(rdr, PAINT_TEXTURE8888, alloc_data, width, height, width, repeat, JNI_TRUE, 
            &textureTransform, JNI_TRUE, hasAlpha,
            0, 0, width-1, height-1);
    } else {
        throw_exc = JNI_TRUE;
    }

    (*env)->ReleasePrimitiveArrayCritical(env, dataArray, data, 0);

    if (throw_exc == JNI_TRUE) {
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
    } else {
        classHandle = NULL;
        JNI_ThrowNew(env, "java/lang/NullPointerException",
                "Specify object instance or class name.");
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

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    fillRect
 * Signature: (IIII)V
 * x, y, w, h are already transformed (is surface coordinates)
 * and rectangle is in an up-right position ie. no rotate or shear
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_fillRect
  (JNIEnv *env, jobject this, jint x, jint y, jint w, jint h) 
{
    Renderer* rdr;
    Surface* surface;
    jobject surfaceHandle;
    jint x_from, x_to, y_from, y_to;
    jint lfrac, rfrac, tfrac, bfrac;
    jint rows_to_render_by_loop, rows_being_rendered;

    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this, fieldIds[RENDERER_NATIVE_PTR]));

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
        rdr->_alphaOffset = 0;

        rdr->_currImageOffset = y_from * surface->width;
        rdr->_imageScanlineStride = surface->width;
        rdr->_imagePixelStride = 1;

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
                jint l = (x_to - x_from + 1) * sizeof(jint);
                ALLOC3(rdr->_paint, jint, l);
                rdr->_genPaint(rdr, 1);
            }
            rdr->_emitLine(rdr, 1, tfrac);
            rows_to_render_by_loop--;
            rdr->_currX = x_from;
            rdr->_currY++;
            rdr->_currImageOffset = rdr->_currY * surface->width;
        }

        // emit "full" lines that are in the middle
        while (rows_to_render_by_loop > 0) {
            rows_being_rendered = MIN(rows_to_render_by_loop, NUM_ALPHA_ROWS);
            if (rdr->_genPaint) {
                jint l = (x_to - x_from + 1) * rows_being_rendered * sizeof(jint);
                ALLOC3(rdr->_paint, jint, l);
                rdr->_genPaint(rdr, rows_being_rendered);
            }
            rdr->_emitLine(rdr, rows_being_rendered, 0x10000);

            rows_to_render_by_loop -= rows_being_rendered;
            rdr->_currX = x_from;
            rdr->_currY += rows_being_rendered;
            rdr->_currImageOffset = rdr->_currY * surface->width;
        }

        // emit fractional bottom line
        if (bfrac) {
            if (rdr->_genPaint) {
                jint l = (x_to - x_from + 1) * sizeof(jint);
                ALLOC3(rdr->_paint, jint, l);
                rdr->_genPaint(rdr, 1);
            }
            rdr->_emitLine(rdr, 1, bfrac);
        }
        RELEASE_SURFACE(surface, env, surfaceHandle);
    }
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    emitAndClearAlphaRow
 * Signature: ([B[IIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_emitAndClearAlphaRow
  (JNIEnv *env, jobject this, jbyteArray jAlphaMap, jintArray jAlphaDeltas, jint y, jint x_from, jint x_to,
   jint rowNum)
{
    Renderer* rdr;
    Surface* surface;
    jobject surfaceHandle;
    
    jbyte* alphaMap = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, jAlphaMap, NULL);
    jint* alphaRow = (jint*)(*env)->GetPrimitiveArrayCritical(env, jAlphaDeltas, NULL);
    
    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this, fieldIds[RENDERER_NATIVE_PTR]));
    
    SURFACE_FROM_RENDERER(surface, env, surfaceHandle, this);
    ACQUIRE_SURFACE(surface, env, surfaceHandle);
    INVALIDATE_RENDERER_SURFACE(rdr);
    VALIDATE_BLITTING(rdr); 
    
    rdr->_minTouched = x_from;
    rdr->_maxTouched = x_to;
    rdr->_currX = x_from;
    rdr->_currY = y;

    rdr->_rowAAOffset = 0;
    rdr->_rowNum = rowNum;
    
    rdr->alphaMap = alphaMap;
    rdr->_rowAAInt = alphaRow;
    rdr->_alphaWidth = x_to - x_from + 1;
    rdr->_alphaOffset = 0;
    
    rdr->_currImageOffset = y * surface->width;
    rdr->_imageScanlineStride = surface->width;
    rdr->_imagePixelStride = 1;
    
    if (rdr->_genPaint) {
        jint l = (x_to - x_from + 1)*sizeof(jint);
        ALLOC3(rdr->_paint, jint, l);
        rdr->_genPaint(rdr, 1);
    }

    rdr->_emitRows(rdr, 1);
    
    RELEASE_SURFACE(surface, env, surfaceHandle);
    
    rdr->_rowAAInt = NULL;
    (*env)->ReleasePrimitiveArrayCritical(env, jAlphaDeltas, alphaRow, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, jAlphaMap, alphaMap, 0);
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    drawImage
 * Signature: (I[IIIIILcom/sun/pisces/Transform6;ZIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_drawImage
(JNIEnv *env, jobject this, jint imageType, jintArray dataArray, jint width, jint height, jint offset, jint stride,
    jobject jTransform, jboolean repeat, jint bboxX, jint bboxY, jint bboxW, jint bboxH,
    jint interpolateMinX, jint interpolateMinY, jint interpolateMaxX, jint interpolateMaxY,
    jint topOpacity, jint bottomOpacity,
    jboolean hasAlpha)
{
    jint i, rowsToBeRendered, rowsBeingRendered;
    jint minX, minY, maxX, maxY;
    jint scanLineAlphaDiff = topOpacity - bottomOpacity;
    Renderer* rdr;
    Surface* surface;
    jobject surfaceHandle;
    
    rdr = (Renderer*)JLongToPointer((*env)->GetLongField(env, this, fieldIds[RENDERER_NATIVE_PTR]));
    
    minX = MAX(bboxX, rdr->_clip_bbMinX);
    minY = MAX(bboxY, rdr->_clip_bbMinY);
    maxX = MIN(bboxX + bboxW - 1, rdr->_clip_bbMaxX);
    maxY = MIN(bboxY + bboxH - 1, rdr->_clip_bbMaxY);
    
    if (maxX >= minX && maxY >= minY)
    {
        Transform6 textureTransform;
        jint* data = (jint*)(*env)->GetPrimitiveArrayCritical(env, dataArray, NULL);
    
        transform_get6(&textureTransform, env, jTransform);
        renderer_setTexture(rdr, PAINT_IMAGE, data + offset, width, height, stride, 
            repeat, JNI_TRUE, &textureTransform, JNI_FALSE, hasAlpha,
            interpolateMinX, interpolateMinY, interpolateMaxX, interpolateMaxY);
        
        SURFACE_FROM_RENDERER(surface, env, surfaceHandle, this);
        ACQUIRE_SURFACE(surface, env, surfaceHandle);
        INVALIDATE_RENDERER_SURFACE(rdr);
        VALIDATE_BLITTING(rdr);
        
        rdr->_minTouched = minX;
        rdr->_maxTouched = maxX;
        rdr->_currX = minX;
        rdr->_currY = minY;
    
        rdr->alphaMap = NULL;
        rdr->_rowAAInt = NULL;
        rdr->_alphaWidth = maxX - minX + 1;
        rdr->_alphaOffset = (minY - bboxY) * width + minX - bboxX;
        
        rdr->_imageScanlineStride = surface->width;
        rdr->_imagePixelStride = 1;
        rdr->_rowNum = 0;
    
        rowsToBeRendered = maxY - minY + 1;
        
        if (!scanLineAlphaDiff) {
            for (i = 0; i < NUM_ALPHA_ROWS; i++) {
                rdr->_scanLineAlpha[i] = topOpacity;
            }
        }
        
        while (rowsToBeRendered > 0) {
            rowsBeingRendered = MIN(rowsToBeRendered, NUM_ALPHA_ROWS);
        
            if (scanLineAlphaDiff) {
                // scanLineAlpha
                jfloat scanLineAlphaInc = ((jfloat)scanLineAlphaDiff) / bboxH;
                for (i = 0; i < rowsBeingRendered; i++) {
                    rdr->_scanLineAlpha[i] = (jint)(topOpacity - scanLineAlphaInc * (rdr->_rowNum + i));
                }
            } 
    
            rdr->_currImageOffset = rdr->_currY * surface->width;
            if (rdr->_genPaint) {
                jint l = (bboxW * rowsBeingRendered)*sizeof(jint);
                ALLOC3(rdr->_paint, jint, l);
                rdr->_genPaint(rdr, rowsBeingRendered);
            }
            rdr->_emitRows(rdr, rowsBeingRendered);
            rdr->_rowNum += rowsBeingRendered;
            rowsToBeRendered -= rowsBeingRendered;
            rdr->_currX = minX;
            rdr->_currY += rowsBeingRendered;
        }
        
        RELEASE_SURFACE(surface, env, surfaceHandle);
        rdr->_texture_intData = NULL;
        (*env)->ReleasePrimitiveArrayCritical(env, dataArray, data, 0);
    }
}

/*
 * Class:     com_sun_pisces_PiscesRenderer
 * Method:    fillAlphaMask
 * Signature: ([BIIIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_fillAlphaMask
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
 * Method:    fillLCDAlphaMask
 * Signature: ([BIIIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_pisces_PiscesRenderer_fillLCDAlphaMask
(JNIEnv *env, jobject this, jbyteArray jmask, jint x, jint y, jint maskWidth, jint maskHeight, jint offset, jint stride)
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
        jbyte* mask = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, jmask, NULL);
        jint width = maxX - minX + 1;
        jint height = maxY - minY + 1;
        
        renderer_setMask(rdr, maskType, mask, maskWidth, maskHeight, JNI_FALSE);
        
        SURFACE_FROM_RENDERER(surface, env, surfaceHandle, this);
        ACQUIRE_SURFACE(surface, env, surfaceHandle);
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
                jint l = (width * rowsBeingRendered)*sizeof(jint);
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
        
        RELEASE_SURFACE(surface, env, surfaceHandle);
        
        renderer_removeMask(rdr);
        (*env)->ReleasePrimitiveArrayCritical(env, jmask, mask, 0);
    }
}

