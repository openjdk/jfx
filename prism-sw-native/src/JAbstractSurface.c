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

#include <JAbstractSurface.h>

#include <JNIUtil.h>
#include <PiscesSysutils.h>

#include <PiscesSurface.inl>

#define SURFACE_NATIVE_PTR 0
#define SURFACE_LAST SURFACE_NATIVE_PTR

static jfieldID fieldIds[SURFACE_LAST + 1];
static jboolean fieldIdsInitialized = JNI_FALSE;

static jboolean initializeSurfaceFieldIds(JNIEnv* env, jobject objectHandle);
static void disposeNativeImpl(JNIEnv* env, jobject objectHandle);

AbstractSurface*
surface_get(JNIEnv* env, jobject surfaceHandle) {
    return (AbstractSurface*)JLongToPointer(
               (*env)->GetLongField(env, surfaceHandle, 
                                    fieldIds[SURFACE_NATIVE_PTR]));
}

jboolean
surface_initialize(JNIEnv* env, jobject surfaceHandle) {
    return initializeSurfaceFieldIds(env, surfaceHandle);
}

JNIEXPORT void JNICALL
Java_com_sun_pisces_AbstractSurface_nativeFinalize(JNIEnv* env,
        jobject objectHandle) {
    disposeNativeImpl(env, objectHandle);
}

JNIEXPORT void JNICALL
Java_com_sun_pisces_AbstractSurface_getRGB(JNIEnv* env, jobject objectHandle,
        jintArray arrayHandle, jint offset, jint scanLength,
        jint x, jint y, jint width, jint height) {
    jint dstX = 0;
    jint dstY = 0;

    Surface* surface;

    surface = (Surface*)JLongToPointer(
                  (*env)->GetLongField(env, objectHandle, 
                                       fieldIds[SURFACE_NATIVE_PTR]));

    CORRECT_DIMS(surface, x, y, width, height, dstX, dstY);

    if ((width > 0) && (height > 0)) {
        jint* dstData = (jint*)(*env)->GetPrimitiveArrayCritical(env, 
                                                                 arrayHandle,
                                                                 NULL);
        if (dstData != NULL) {
            jint* src;
            jint* dst;
            jint srcScanRest = surface->width - width;
            jint dstScanRest = scanLength - width;

            ACQUIRE_SURFACE(surface, env, objectHandle);
            src = (jint*)surface->data + y * surface->width + x;
            dst = dstData + offset + dstY * scanLength + dstX;
            for (; height > 0; --height) {
                jint w2 = width;
                for (; w2 > 0; --w2) {
                    *dst++ = *src++;
                }
                src += srcScanRest;
                dst += dstScanRest;
            }
            RELEASE_SURFACE(surface, env, objectHandle);

            if (JNI_TRUE == readAndClearMemErrorFlag()) {
                JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                             "Allocation of internal renderer buffer failed.");
            }

            (*env)->ReleasePrimitiveArrayCritical(env, arrayHandle, dstData, 0);
        } else {
            JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                      "Allocation of temporary renderer memory buffer failed.");
        }
    }
}

JNIEXPORT void JNICALL
Java_com_sun_pisces_AbstractSurface_setRGB(JNIEnv* env, jobject objectHandle,
        jintArray arrayHandle, jint offset, jint scanLength,
        jint x, jint y, jint width, jint height) {
    jint srcX = 0;
    jint srcY = 0;

    Surface* surface;
    surface = (Surface*)JLongToPointer(
                  (*env)->GetLongField(env, objectHandle, 
                                       fieldIds[SURFACE_NATIVE_PTR]));

    CORRECT_DIMS(surface, x, y, width, height, srcX, srcY);

    if ((width > 0) && (height > 0)) {
        jint* srcData = (jint*)(*env)->GetPrimitiveArrayCritical(env, 
                                                                 arrayHandle,
                                                                 NULL);
        if (srcData != NULL) {
            jint* src;

            ACQUIRE_SURFACE(surface, env, objectHandle);
            src = srcData + offset + srcY * scanLength + srcX;
            surface_setRGB(surface, x, y, width, height, src, scanLength);
            RELEASE_SURFACE(surface, env, objectHandle);

            (*env)->ReleasePrimitiveArrayCritical(env, arrayHandle, srcData, 0);

            if (JNI_TRUE == readAndClearMemErrorFlag()) {
                JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                             "Allocation of internal renderer buffer failed.");
            }
        } else {
            JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                      "Allocation of temporary renderer memory buffer failed.");
        }
    }
}

static jboolean
initializeSurfaceFieldIds(JNIEnv* env, jobject objectHandle) {
    static const FieldDesc surfaceFieldDesc[] = {
                { "nativePtr", "J" },
                { NULL, NULL }
            };

    jboolean retVal;
    jclass classHandle;

    if (fieldIdsInitialized) {
        return JNI_TRUE;
    }

    retVal = JNI_FALSE;

    classHandle = (*env)->GetObjectClass(env, objectHandle);

    if (initializeFieldIds(fieldIds, env, classHandle, surfaceFieldDesc)) {
        retVal = JNI_TRUE;
        fieldIdsInitialized = JNI_TRUE;
    }

    return retVal;
}

static void
disposeNativeImpl(JNIEnv* env, jobject objectHandle) {
    AbstractSurface* surface;

    if (!fieldIdsInitialized) {
        return;
    }

    surface = (AbstractSurface*)JLongToPointer(
                  (*env)->GetLongField(env, objectHandle, 
                                       fieldIds[SURFACE_NATIVE_PTR]));

    if (surface != NULL) {
        surface->cleanup(surface);
        surface_dispose(&surface->super);
        (*env)->SetLongField(env, objectHandle, fieldIds[SURFACE_NATIVE_PTR],
                             (jlong)0);

        if (JNI_TRUE == readAndClearMemErrorFlag()) {
            JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                         "Allocation of internal renderer buffer failed.");
        }
    }
}

