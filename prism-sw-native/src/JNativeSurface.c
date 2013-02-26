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

#include <PiscesUtil.h>
#include <PiscesSysutils.h>

#define SURFACE_NATIVE_PTR 0
#define SURFACE_LAST SURFACE_NATIVE_PTR

static jfieldID fieldIds[SURFACE_LAST + 1];
static jboolean fieldIdsInitialized = JNI_FALSE;

static jboolean initializeSurfaceFieldIds(JNIEnv* env, jobject objectHandle);

static void surface_acquire(AbstractSurface* surface, JNIEnv* env, jobject surfaceHandle);
static void surface_release(AbstractSurface* surface, JNIEnv* env, jobject surfaceHandle);
static void surface_cleanup(AbstractSurface* surface);

JNIEXPORT void JNICALL
Java_com_sun_pisces_NativeSurface_initialize(JNIEnv* env, jobject objectHandle, 
        jint dataType, jint width, jint height) {

    AbstractSurface* surface;

    if (surface_initialize(env, objectHandle)
            && initializeSurfaceFieldIds(env, objectHandle)) {
        surface = my_malloc(AbstractSurface, 1);
        if (surface != NULL) {
            jint size;
            void* data;
            switch (dataType) {
            case TYPE_INT_ARGB:
            case TYPE_INT_ARGB_PRE:
                size = width * height * sizeof(jint);
                break;
            default: //errorneous - should never happen
                size = 0;
                break;
            }
            data = PISCESmalloc(size);
            if (data != NULL) {
                memset(data, 0, size);

                surface->super.width = width;
                surface->super.height = height;
                surface->super.offset = 0;
                surface->super.scanlineStride = width;
                surface->super.pixelStride = 1;
                surface->super.imageType = dataType;
                surface->super.data = data;
                
                surface->acquire = surface_acquire;
                surface->release = surface_release;
                surface->cleanup = surface_cleanup;

                (*env)->SetLongField(env, objectHandle, fieldIds[SURFACE_NATIVE_PTR],
                                 PointerToJLong(surface));

            } else {
                my_free(surface);
                surface = NULL;
                JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                             "Allocation of internal renderer buffer failed.");
            }
        } else {
            JNI_ThrowNew(env, "java/lang/OutOfMemoryError",
                         "Allocation of internal renderer buffer failed.");
        }
    } else {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "");
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
surface_acquire(AbstractSurface* surface, JNIEnv* env, jobject surfaceHandle) {
    // do nothing
    // IMPL NOTE : to fix warning : unused parameter
    (void)surface;
    (void)surfaceHandle;
}

static void
surface_release(AbstractSurface* surface, JNIEnv* env, jobject surfaceHandle) {
    // do nothing
    // IMPL NOTE : to fix warning : unused parameter
    (void)surface;
    (void)surfaceHandle;
}

static void
surface_cleanup(AbstractSurface* surface) {
    my_free(surface->super.data);
    surface->super.data = NULL;
}
