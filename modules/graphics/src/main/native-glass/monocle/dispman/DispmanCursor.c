/* Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_glass_ui_monocle_DispmanCursor.h"
 #include <stdio.h>
 #include <pthread.h>
 #include <semaphore.h>
 #include <unistd.h>
 #include <sys/types.h>
 #include <sys/stat.h>
 #include <fcntl.h>
 #include <linux/fb.h>
 #include <sys/ioctl.h>
 #include <stdlib.h>
 #include <string.h>
 #include "Monocle.h"

 #ifdef USE_DISPMAN
 #include "wrapped_bcm.h"

 #define FB_DEVICE "/dev/fb0"

typedef struct {
    DISPMANX_ELEMENT_HANDLE_T element;
    int screenWidth, screenHeight;
    pthread_t thread;
    pthread_mutex_t mutex;
    sem_t semaphore;
    int x, y;
    int cursorWidth, cursorHeight;
    jlong currentCursor;
    jboolean isVisible;
} DispManCursor;


typedef struct {
    jint width;
    jint height;
    jint x;
    jint y;
    DISPMANX_RESOURCE_HANDLE_T resource;
} DispmanCursorImage;

static DispManCursor cursor;

static void addDispmanxElement();
static void setNativeCursor(jlong nativeCursorHandle);
static void addDispmanxElement();
static void removeDispmanxElement();
static void updateCursor();

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_DispmanCursor__1initDispmanCursor
  (JNIEnv *env, jobject obj, jint width, jint height) {
    cursor.cursorWidth = width;
    cursor.cursorHeight = height;
    cursor.x = 0, cursor.y = 0;
    cursor.element = 0;
    cursor.currentCursor = 0;
    cursor.isVisible = 0;
    addDispmanxElement();
    updateCursor();
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_DispmanCursor__1setVisible
  (JNIEnv *env, jobject obj, jboolean isVisible)
{
    if (isVisible) {
        if (!cursor.isVisible && cursor.currentCursor != 0) {
            setNativeCursor(cursor.currentCursor);
            DispmanCursorImage *cursorImage =
                (DispmanCursorImage *)asPtr(cursor.currentCursor);
            DISPMANX_UPDATE_HANDLE_T update = vc_dispmanx_update_start(0);
            vc_dispmanx_element_change_source(update, cursor.element,
                                              cursorImage->resource);
            vc_dispmanx_update_submit_sync(update);
        }
    } else {
        DISPMANX_UPDATE_HANDLE_T update;
        update = vc_dispmanx_update_start(0);
        vc_dispmanx_element_change_source(update, cursor.element, 0 );
        vc_dispmanx_update_submit_sync(update);
    }
    cursor.isVisible = isVisible;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_DispmanCursor__1setLocation
  (JNIEnv *env, jobject obj, jint x, jint y) {
    cursor.x = x;
    cursor.y = y;
    updateCursor();
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_DispmanCursor__1setImage
  (JNIEnv *env, jobject obj, jbyteArray srcArray)
{
     VC_RECT_T pixelRect;
     DISPMANX_UPDATE_HANDLE_T update;
     int rc;
     int i, j;
     uint32_t imagePtr;
     DispmanCursorImage *cursorImage = (DispmanCursorImage *)malloc(sizeof(DispmanCursorImage));

     pixelRect.x = 0;
     pixelRect.y = 0;
     pixelRect.width = cursor.cursorWidth;
     pixelRect.height = cursor.cursorHeight;

     cursorImage->x = cursor.x;
     cursorImage->y = cursor.y;
     cursorImage->width = cursor.cursorWidth;
     cursorImage->height = cursor.cursorHeight;
     cursorImage->resource = vc_dispmanx_resource_create(VC_IMAGE_ARGB8888,
                             cursor.cursorWidth,
                             cursor.cursorHeight,
                             &imagePtr);

     if (cursorImage->resource == 0) {
         fprintf(stderr, "Cannot create resource\n");
         free(cursorImage);
         return;
     }

     jbyte *srcBytes = (*env)->GetByteArrayElements(env, srcArray, 0);

     rc = vc_dispmanx_resource_write_data(cursorImage->resource,
                                          VC_IMAGE_ARGB8888,
                                          cursor.cursorWidth * 4,
                                          srcBytes,
                                          &pixelRect);

     (*env)->ReleaseByteArrayElements(env, srcArray, srcBytes, 0);

     if (rc != 0) {
         fprintf(stderr, "Cannot write pixels");
         free(cursorImage);
         return;
     }
     cursor.currentCursor = asJLong(cursorImage);
     if (cursor.isVisible) {
         update = vc_dispmanx_update_start(0);
         vc_dispmanx_element_change_source(update, cursor.element, cursorImage->resource);
         vc_dispmanx_update_submit_sync(update);
     }
}

static void setNativeCursor(jlong nativeCursorHandle) {

    DISPMANX_UPDATE_HANDLE_T update;
    DispmanCursorImage *cursorImage = (DispmanCursorImage *)asPtr(nativeCursorHandle);
    if (cursorImage != NULL && cursor.element != 0) {
        if (cursorImage->width != cursor.cursorWidth ||
                cursorImage->height != cursor.cursorHeight) {

            removeDispmanxElement();

            cursor.cursorWidth = cursorImage->width;
            cursor.cursorHeight = cursorImage->height;

            addDispmanxElement();
        }
        cursor.currentCursor = nativeCursorHandle;

        if (cursor.isVisible) {
            update = vc_dispmanx_update_start(0);
            vc_dispmanx_element_change_source(update, cursor.element, cursorImage->resource);
            vc_dispmanx_update_submit_sync(update);
        }

    }
}

static void addDispmanxElement() {

    DISPMANX_DISPLAY_HANDLE_T display = 0;
    DISPMANX_UPDATE_HANDLE_T update;
    VC_DISPMANX_ALPHA_T alpha;
    VC_RECT_T dst;
    VC_RECT_T src = { 0, 0, cursor.cursorWidth << 16, cursor.cursorHeight << 16};
    VC_RECT_T pixelRect = { 0, 0, cursor.cursorWidth, cursor.cursorHeight };
    int rc;

    display = vc_dispmanx_display_open(0 /* LCD */);
    if (display == 0) {
        fprintf(stderr, "Cannot open display\n");
        return;
    }

    update = vc_dispmanx_update_start(0);
    alpha.flags = DISPMANX_FLAGS_ALPHA_FROM_SOURCE;
    alpha.opacity = 0xff;
    alpha.mask = (DISPMANX_RESOURCE_HANDLE_T) 0;
    dst.x = cursor.x;
    dst.y = cursor.y;
    dst.width = cursor.cursorWidth;
    dst.height = cursor.cursorHeight;
    cursor.element = vc_dispmanx_element_add(
                         update,
                         display,
                         2 /*layer*/,
                         &dst,
                         0 /*resource*/,
                         &src,
                         DISPMANX_PROTECTION_NONE,
                         &alpha,
                         0 /*clamp*/,
                         0 /*transform*/);

    vc_dispmanx_update_submit_sync(update);

}


static void removeDispmanxElement() {
    if (cursor.element) {
        DISPMANX_UPDATE_HANDLE_T update;
        update = vc_dispmanx_update_start(0);
        vc_dispmanx_element_remove(update, cursor.element);
        vc_dispmanx_update_submit_sync(update);

        cursor.element = 0;
    }
}

static void updateCursor() {
    DISPMANX_UPDATE_HANDLE_T update;
    VC_RECT_T dst;
    dst.x = cursor.x;
    dst.y = cursor.y;
    dst.width = cursor.cursorWidth;
    dst.height = cursor.cursorHeight;
    update = vc_dispmanx_update_start(0);
    vc_dispmanx_element_change_attributes(update,
                                          cursor.element,
                                          0x4 ,
                                          0 , 0,
                                          &dst,
                                          0 ,
                                          0 , 0 );
    vc_dispmanx_update_submit_sync(update);
}

 #endif //USE_DISPMAN