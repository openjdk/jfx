/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 
#include <stdio.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>
#include <jni.h>
#ifdef USE_DISPMAN
#include <bcm_host.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <linux/fb.h>
#include <sys/ioctl.h>
#include <stdlib.h>
#include <string.h>
#include "LensCommon.h"

#define FB_DEVICE "/dev/fb0"

#include "fbCursor.h"

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

static void *fbCursorUpdater(void *data);
static void fbDispmanAddDispmanxElement(void);
static void fbDispmanRemoveDispmanxElement(void);


static void fbDispmanSetNativeCursor(jlong nativeCursorHandle) {

    DISPMANX_UPDATE_HANDLE_T update;
    DispmanCursorImage *cursorImage = (DispmanCursorImage *)jlong_to_ptr(nativeCursorHandle);


    if (cursorImage != NULL && cursor.element != 0 &&
        cursor.currentCursor != nativeCursorHandle) 
    {
        if (cursorImage->width != cursor.cursorWidth ||
                cursorImage->height != cursor.cursorHeight) {

            fbDispmanRemoveDispmanxElement();

            cursor.cursorWidth = cursorImage->width;
            cursor.cursorHeight = cursorImage->height;

            fbDispmanAddDispmanxElement();
        }

        cursor.currentCursor = nativeCursorHandle;

        if (cursor.isVisible) {
            update = vc_dispmanx_update_start(0);
            vc_dispmanx_element_change_source(update, cursor.element, cursorImage->resource);
            vc_dispmanx_update_submit_sync(update);
        }
    }
}


static void fbDispmanAddDispmanxElement(void) {

    DISPMANX_DISPLAY_HANDLE_T display = 0;
    DISPMANX_UPDATE_HANDLE_T update;
    VC_DISPMANX_ALPHA_T alpha;
    VC_RECT_T dst;
    VC_RECT_T src = { 0, 0, cursor.cursorWidth << 16, cursor.cursorHeight << 16};
    VC_RECT_T pixelRect = { 0, 0, cursor.cursorWidth, cursor.cursorHeight };
    int rc;

    display = vc_dispmanx_display_open(0 /* LCD */);
    if (display == 0) {
        GLASS_LOG_SEVERE("Cannot open display");
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
                         0 /*layer*/,
                         &dst,
                         0 /*resource*/,
                         &src,
                         DISPMANX_PROTECTION_NONE,
                         &alpha,
                         0 /*clamp*/,
                         0 /*transform*/);

    vc_dispmanx_update_submit_sync(update);

}


static void fbDispmanRemoveDispmanxElement(void) {

    if (cursor.element) {
        DISPMANX_UPDATE_HANDLE_T update;
        update = vc_dispmanx_update_start(0);
        vc_dispmanx_element_remove(update, cursor.element);
        vc_dispmanx_update_submit_sync(update);

        cursor.element = 0;
    }
}

static void fbDispmanCursorInitialize(int screenWidth, int screenHeight) {

    // Init cursor fields
    cursor.element = 0;
    cursor.cursorWidth = 16;
    cursor.cursorHeight = 16;
    cursor.x = 0;
    cursor.y = 0;
    cursor.currentCursor = 0;
    cursor.isVisible = 0;

    cursor.screenWidth = screenWidth;
    cursor.screenHeight = screenHeight;

    fbDispmanAddDispmanxElement();

    sem_init(&cursor.semaphore, 0, 0);
    pthread_mutex_init(&cursor.mutex, NULL);
    pthread_create(&cursor.thread, NULL, fbCursorUpdater, NULL);

}


static jlong fbDispmanCreateNativeCursor(JNIEnv *env, jint x, jint y,  jbyte *srcArray, jint width, jint height) {

    VC_RECT_T pixelRect;
    int rc;
    uint32_t imagePtr;
    jbyte *allocatedBuffer = NULL;
    DispmanCursorImage *cursorImage = (DispmanCursorImage *)malloc(sizeof(DispmanCursorImage));

    //Width should be aligned to 16 pixels
    if (width % 16 != 0) {
        int newWidth = width + 16 - (width % 16);
        allocatedBuffer = (jbyte *)malloc(newWidth * height * 4);
        int i;
        int offset = 0;
        for (i = 0; i < height; ++i) {
            memcpy(allocatedBuffer + offset, srcArray, width * 4);
            memset(allocatedBuffer + offset + (width * 4), 0, (newWidth - width) * 4);
            offset += newWidth * 4;
            srcArray += width * 4;
        }

        width = newWidth;
        srcArray = allocatedBuffer;
    }

    pixelRect.x = 0;
    pixelRect.y = 0;
    pixelRect.width = width;
    pixelRect.height = height;

    cursorImage->x = x;
    cursorImage->y = y;
    cursorImage->width = width;
    cursorImage->height = height;
    cursorImage->resource = vc_dispmanx_resource_create(VC_IMAGE_ARGB8888,
                            width,
                            height,
                            &imagePtr);
    if (cursorImage->resource == 0) {
        GLASS_LOG_SEVERE("Cannot create resource");
        if (allocatedBuffer != NULL) {
            free(allocatedBuffer);
            allocatedBuffer = NULL;
        }
        free(cursorImage);
        return 0;
    }

    rc = vc_dispmanx_resource_write_data(cursorImage->resource,
                                         VC_IMAGE_ARGB8888,
                                         width * 4,
                                         srcArray,
                                         &pixelRect);

    if (allocatedBuffer != NULL) {
        free(allocatedBuffer);
        allocatedBuffer = NULL;
    }

    if (rc != 0) {
        GLASS_LOG_SEVERE("Cannot write pixels");
        free(cursorImage);
        return 0;
    }

    return ptr_to_jlong(cursorImage);
}




static void fbDispmanReleaseNativeCursor(jlong nativeCursorHandle) {

    DispmanCursorImage *cursorImage = (DispmanCursorImage *)jlong_to_ptr(nativeCursorHandle);

    if (cursorImage != NULL && cursorImage->resource != 0) {

        if (cursor.currentCursor == nativeCursorHandle &&
            cursor.isVisible) 
        {
            DISPMANX_UPDATE_HANDLE_T update;
            update = vc_dispmanx_update_start(0);
            vc_dispmanx_element_change_source(update, cursor.element, 0 /* resource*/);
            vc_dispmanx_update_submit_sync(update);
        }
        
        vc_dispmanx_resource_delete(cursorImage->resource);
    }

    free(cursorImage);

    if (cursor.currentCursor == nativeCursorHandle) {
        cursor.currentCursor = 0;
    }
}


static void fbDispmanSetVisible(jboolean isVisible) {

    if (isVisible) {
        if (!cursor.isVisible && cursor.currentCursor != 0) {

            DispmanCursorImage *cursorImage = (DispmanCursorImage *)jlong_to_ptr(cursor.currentCursor);
            DISPMANX_UPDATE_HANDLE_T update = vc_dispmanx_update_start(0);
            vc_dispmanx_element_change_source(update, cursor.element, cursorImage->resource);
            vc_dispmanx_update_submit_sync(update);
        }
    } else {
        DISPMANX_UPDATE_HANDLE_T update;
        update = vc_dispmanx_update_start(0);
        vc_dispmanx_element_change_source(update, cursor.element, 0 /* resource*/);
        vc_dispmanx_update_submit_sync(update);
    }

    cursor.isVisible = isVisible;
}



static void *fbCursorUpdater(void *data) {
    while (1) {
        DISPMANX_UPDATE_HANDLE_T update;
        VC_RECT_T dst;
        sem_wait(&cursor.semaphore);
        pthread_mutex_lock(&cursor.mutex);
        dst.x = cursor.x;
        dst.y = cursor.y;
        dst.width = cursor.cursorWidth;
        dst.height = cursor.cursorHeight;
        pthread_mutex_unlock(&cursor.mutex);
        update = vc_dispmanx_update_start(0);
        vc_dispmanx_element_change_attributes(update,
                                              cursor.element,
                                              0x4 /* change dest rect */,
                                              0 /* layer */, 0, /*opacity */
                                              &dst,
                                              0 /* source */,
                                              0 /* mask */, 0 /* transform */);
        vc_dispmanx_update_submit_sync(update);
        usleep(16666); /* sleep a sixtieth of a second before moving again */
    }
    return NULL;
}

static void fbDispmanCursorSetPosition(int x, int y) {

    if (cursor.element) {
        int posted;
        pthread_mutex_lock(&cursor.mutex);
        cursor.x = x;
        cursor.y = y;
        sem_getvalue(&cursor.semaphore, &posted);
        pthread_mutex_unlock(&cursor.mutex);
        if (posted == 0) {
            sem_post(&cursor.semaphore);
        }
    } else {
        cursor.x = x;
        cursor.y = y;
    }
}

void fbDispmanCursorClose() {

    fbDispmanRemoveDispmanxElement();
    cursor.isVisible = 0;
}

jboolean dispman_glass_robot_screen_capture(jint x, jint y,
                                            jint width, jint height,
                                            jint *pixels) {
    FILE *fb;
    unsigned int *pixelBuffer = NULL;
    unsigned char *pixelBufferPtr = NULL;
    unsigned char *dst = (unsigned char *) pixels;
    int i = 0;
    int fbFileHandle;
    struct fb_var_screeninfo screenInfo;
    unsigned int dstByteStride = width * 4;
    VC_IMAGE_TRANSFORM_T transform = 0;
    DISPMANX_RESOURCE_HANDLE_T resource = 0;
    DISPMANX_DISPLAY_HANDLE_T display = 0;
    DISPMANX_RESOURCE_HANDLE_T screenResource = 0;
    uint32_t imagePtr;
    int rc;

    GLASS_LOG_FINE("Capture %i,%i+%ix%i", x, y, width, height);

    if (width < 1 || height < 1) {
        GLASS_LOG_SEVERE("Failed. width/height values must be at least = 1");
        return JNI_FALSE;
    }

    GLASS_LOG_FINE("open(%s, O_RDONLY)", FB_DEVICE);
    fbFileHandle = open(FB_DEVICE, O_RDONLY);
    if (fbFileHandle < 0) {
        GLASS_LOG_SEVERE("Cannot open framebuffer");
        return JNI_FALSE;
    }

    GLASS_LOG_FINE("ioctl(%s, FBIOGET_VSCREENINFO)", FB_DEVICE);
    if (ioctl(fbFileHandle, FBIOGET_VSCREENINFO, &screenInfo)) {
        GLASS_LOG_SEVERE("Cannot get screen info");
        GLASS_LOG_FINE("close(%s)", FB_DEVICE);
        close(fbFileHandle);
        return JNI_FALSE;
    }
    GLASS_LOG_FINE("Read screen info: res=%ix%i, offset=%ix%i",
                   screenInfo.xres, screenInfo.yres,
                   screenInfo.xoffset, screenInfo.yoffset);
    GLASS_LOG_FINE("close(%s)", FB_DEVICE);
    close(fbFileHandle);

    VC_RECT_T pixelRect = { 0, 0, screenInfo.xres, screenInfo.yres };

    int pixelBufferLength = screenInfo.xres * screenInfo.yres * 4;
    pixelBuffer = (unsigned int *) malloc(pixelBufferLength);
    pixelBufferPtr = (unsigned char *) pixelBuffer;

    if (pixelBuffer == NULL) {
        printf("Failed to allocate temporary pixel buffer\n");
        return JNI_FALSE;
    }

    GLASS_LOG_FINE("fopen(%s, \"r\") to read %ix%i pixels at bit depth %i\n",
                   FB_DEVICE, width, height, screenInfo.bits_per_pixel);

    display = vc_dispmanx_display_open(0 /* LCD */);
    if (display == 0) {
        fprintf(stderr, "fbRobotScreenCapture: Dispman: Cannot open display\n");
        free(pixelBuffer);
        return JNI_FALSE;
    }

    // create the resource for the snapshot
    screenResource = vc_dispmanx_resource_create(VC_IMAGE_ARGB8888, screenInfo.xres, screenInfo.yres, &imagePtr);
    if (!screenResource) {
        fprintf(stderr, "fbRobotScreenCapture: Cannot create resource\n");
        vc_dispmanx_display_close(display);
        free(pixelBuffer);
        return JNI_FALSE;
    }

    rc = vc_dispmanx_snapshot(display, screenResource, transform);
    if (rc) {
        fprintf(stderr, "fbRobotScreenCapture: snapshot failed\n");
        vc_dispmanx_display_close(display);
        free(pixelBuffer);
        return JNI_FALSE;
    }

    rc = vc_dispmanx_resource_read_data(screenResource, &pixelRect, pixelBuffer, screenInfo.xres * 4);
    if (rc) {
        fprintf(stderr, "fbRobotScreenCapture: Cannot read pixels %d\n", rc);
        vc_dispmanx_display_close(display);
        free(pixelBuffer);
        return JNI_FALSE;
    }

    rc = vc_dispmanx_resource_delete(screenResource);
    if (rc) {
        fprintf(stderr, "fbRobotScreenCapture: failed to free buffer %d\n", rc);
        vc_dispmanx_display_close(display);
        free(pixelBuffer);
        return JNI_FALSE;
    }
    screenResource = 0;

    if (x < 0) {
        pixelBuffer += -x;
        width += x;
        x = 0;
    }
    if (y < 0) {
        pixelBuffer += -y * (int)screenInfo.xres;
        height += y;
        y = 0;
    }

    int widthLimit = width;
    int heightLimit = height;

    // Required height is larger than screen's height
    if ((int) screenInfo.yres < height) {
        heightLimit = (int) screenInfo.yres;
    }
    // Required width is larger than screen's width
    if ((int) screenInfo.xres < width) {
        widthLimit = (int) screenInfo.xres;
    }
    // Required height is out of range
    if (((int) screenInfo.yres - y) < height) {
        heightLimit = (int) screenInfo.yres - y;
    }
    // Required width is out of range
    if (((int) screenInfo.xres - x) < width) {
        widthLimit = (int) screenInfo.xres - x;
    }

    if (widthLimit > 0 && heightLimit > 0) {
        // copy the relevant portion of the screen to the supplied pixel array
        int offset = y * screenInfo.xres * 4 + x * 4;
        for (i = 0; i < heightLimit; i++) {
            memcpy(dst + i * dstByteStride, pixelBufferPtr + offset, widthLimit * 4);
            offset += screenInfo.xres * 4;
        }
    } else {
        GLASS_LOG_SEVERE("Failed to take a snapshot, some of parameters are illegal");
        free(pixelBuffer);
        return JNI_FALSE;
    }

    vc_dispmanx_display_close(display);
    free(pixelBuffer);
    return JNI_TRUE;
}

extern int useDispman;
extern void load_bcm_symbols();

jboolean check_dispman_cursor() {
    load_bcm_symbols();

    if (useDispman) {
        fbPlatformSetNativeCursor = fbDispmanSetNativeCursor;
        fbPlatformCursorInitialize = fbDispmanCursorInitialize;
        fbPlatformCursorSetPosition = fbDispmanCursorSetPosition;
        fbPlatformCursorClose = fbDispmanCursorClose;
        fbPlatformCreateNativeCursor = fbDispmanCreateNativeCursor;
        fbPlatformReleaseNativeCursor = fbDispmanReleaseNativeCursor;
        fbPlatformSetVisible = fbDispmanSetVisible;
        fbPlatformCursorTranslucency = JNI_TRUE;
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

#else /* ! USE_DISPMAN */

jboolean check_dispman_cursor() {
    return JNI_FALSE;
}

#endif /* USE_DISPMAN */
