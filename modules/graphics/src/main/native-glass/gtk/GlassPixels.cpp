/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <string.h>
#include <gdk/gdk.h>
#include <cairo.h>
#include <assert.h>
#include <com_sun_glass_ui_gtk_GtkPixels.h>
#include <gdk-pixbuf/gdk-pixbuf-core.h>

#include "glass_general.h"

static void my_free(guchar *pixels, gpointer data) {
    (void)data;

    g_free(pixels);
}

extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkPixels
 * Method:    _copyPixels
 * Signature: (Ljava/nio/Buffer;Ljava/nio/Buffer;I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkPixels__1copyPixels
  (JNIEnv *env, jobject obj, jobject jDst, jobject jSrc, jint jSize)
{
    (void)obj;

    //Taken from MacPixels (and fixed)
    void *src = env->GetDirectBufferAddress(jSrc);
    void *dst = env->GetDirectBufferAddress(jDst);
    if ((src != NULL) && (dst != NULL) && (jSize > 0))
    {
        memcpy(dst, src, jSize * 4);
    }
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkPixels
 * Method:    _attachInt
 * Signature: (JIILjava/nio/IntBuffer;[II)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkPixels__1attachInt
  (JNIEnv * env, jobject obj, jlong ptr, jint w, jint h, jobject ints, jintArray array, jint offset)
{
    (void)obj;

    jint *data;
    GdkPixbuf **pixbuf;
    guint8 *dataRGBA;

    if (array == NULL) {
        data = (jint*) env->GetDirectBufferAddress(ints);
        assert((w*h*4 + offset * 4) == env->GetDirectBufferCapacity(ints));
    } else {
        assert((w*h + offset) == env->GetArrayLength(array));
        data = (jint*) env->GetPrimitiveArrayCritical(array, 0);
    }

    pixbuf = (GdkPixbuf**)JLONG_TO_PTR(ptr);
    dataRGBA = convert_BGRA_to_RGBA(data + offset, w*4, h);
    *pixbuf = gdk_pixbuf_new_from_data(dataRGBA, GDK_COLORSPACE_RGB, TRUE, 8,
                  w, h, w * 4, (GdkPixbufDestroyNotify) my_free, NULL);
    if (array != NULL) {
        env->ReleasePrimitiveArrayCritical(array, data, 0);
    }
}


/*
 * Class:     com_sun_glass_ui_gtk_GtkPixels
 * Method:    _attachByte
 * Signature: (JIILjava/nio/ByteBuffer;[BI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkPixels__1attachByte
  (JNIEnv * env, jobject obj, jlong ptr, jint w, jint h, jobject bytes, jbyteArray array, jint offset)
{
    (void)obj;

    jbyte *data;
    GdkPixbuf **pixbuf;
    guint8 *dataRGBA;

    if (array == NULL) {
        data = (jbyte*) env->GetDirectBufferAddress(bytes);
        assert((w*h*4 + offset) == env->GetDirectBufferCapacity(bytes));
    } else {
        assert((w*h*4 + offset) == env->GetArrayLength(array));
        data = (jbyte*) env->GetPrimitiveArrayCritical(array, 0);
    }

    pixbuf = (GdkPixbuf**)JLONG_TO_PTR(ptr);
    dataRGBA = convert_BGRA_to_RGBA((const int*)(data + offset), w*4, h);
    *pixbuf = gdk_pixbuf_new_from_data(dataRGBA, GDK_COLORSPACE_RGB, TRUE, 8,
                  w, h, w * 4, (GdkPixbufDestroyNotify) my_free, NULL);
    if (array != NULL) {
        env->ReleasePrimitiveArrayCritical(array, data, 0);
    }
}

} // extern "C"
