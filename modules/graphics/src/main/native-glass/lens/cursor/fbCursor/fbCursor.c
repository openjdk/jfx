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
 
#include "input/LensInput.h"

#define FB_CURSOR_DECLARE //cause the fbPlatform variables to be declared here.
#include "fbCursor.h"

typedef struct {
    int width;
    int height;
    int bpp;
    jbyte *buffer;
} FBCursorImage;

#if defined(OMAP3)

#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <linux/fb.h>
#include <linux/omapfb.h>
#include <sys/ioctl.h>

#define FB_CURSOR_DEVICE "/dev/fb1"
#define LENSFB_CURSOR_COLOR_KEY 0xABABABAB

typedef struct {
    int fd;
    struct omapfb_plane_info plane;
    int width;
    int height;
    int screenWidth;
    int screenHeight;
    jlong currentCursor;
    jboolean isVisible;
} FBCursor;

static FBCursor cursor = { .fd = -1, .width = 0, .height = 0, .currentCursor = 0, .isVisible = 0};

static void fbCreateCursor(jbyte *cursorImage, int width, int height, int bpp) {

    struct fb_var_screeninfo screenInfo;
    cursor.width = width;
    cursor.height = height;

    GLASS_LOG_FINE("open(%s, O_RDWR)", FB_CURSOR_DEVICE);
    cursor.fd = open(FB_CURSOR_DEVICE, O_RDWR);
    if (cursor.fd < 0) {
        GLASS_LOG_SEVERE("Cannot open frame buffer device for cursor");
        return;
    }
    if (ioctl(cursor.fd, FBIOGET_VSCREENINFO, &screenInfo)) {
        GLASS_LOG_SEVERE("Cannot query screen info");
        fbCursorClose();
        return;
    }
    screenInfo.xoffset = 0;
    screenInfo.yoffset = 0;
    screenInfo.xres = screenInfo.xres_virtual = cursor.width;
    screenInfo.yres = screenInfo.yres_virtual = cursor.height;

    if (ioctl(cursor.fd, FBIOPUT_VSCREENINFO, &screenInfo)) {
        GLASS_LOG_SEVERE("Cannot set screen info");
        fbCursorClose();
        return;
    }
    cursor.plane.enabled = 1;
    cursor.plane.out_width = cursor.width;
    cursor.plane.out_height = cursor.height;
    if (ioctl(cursor.fd, OMAPFB_SETUP_PLANE, &cursor.plane)) {
        GLASS_LOG_SEVERE("Cannot set plane info");
        fbCursorClose();
        return;
    }

    if (ioctl(cursor.fd, OMAPFB_QUERY_PLANE, &cursor.plane)) {
        GLASS_LOG_SEVERE("Cannot query plane info");
        fbCursorClose();
        return;
    }

    // Set up the color key
    struct omapfb_color_key color_key;
    if (ioctl(cursor.fd, OMAPFB_GET_COLOR_KEY, &color_key)) {
        GLASS_LOG_SEVERE("Cannot set color key");
        return;
    }

    color_key.key_type = OMAPFB_COLOR_KEY_VID_SRC;
    color_key.trans_key = LENSFB_CURSOR_COLOR_KEY;
    if (ioctl(cursor.fd, OMAPFB_SET_COLOR_KEY, &color_key)) {
        GLASS_LOG_SEVERE("OMAPFB_SET_COLOR_KEY");
        return;
    }

    int cursorSize = cursor.width * cursor.height * bpp;
    if (write(cursor.fd, cursorImage, cursorSize) < cursorSize) {
        GLASS_LOG_SEVERE("Cannot write cursor plane");
        return;
    }

}

void fbCursorInitialize() {
    NativeScreen screen = glass_screen_getMainScreen();

    check_dispman_cursor();

    if (fbPlatformCursorInitialize) {
        (*fbPlatformCursorInitialize)(screen->width, screen->height);
    } else {
        cursor.screenWidth = screen->width;
        cursor.screenHeight = screen->height;
    }
}

void fbCursorSetPosition(int x, int y) {

    if (fbPlatformCursorSetPosition) {
        return (*fbPlatformCursorSetPosition)(x, y);
    }

    if (x < 0) {
        x = 0;
    }
    if (y < 0) {
        y = 0;
    }
    if (x > cursor.screenWidth - cursor.width) {
        x = cursor.screenWidth - cursor.width;
    }
    if (y > cursor.screenHeight - cursor.height) {
        y = cursor.screenHeight - cursor.height;
    }
    cursor.plane.enabled = 1;
    cursor.plane.pos_x = x;
    cursor.plane.pos_y = y;
    if (cursor.fd >= 0) {
        if (ioctl(cursor.fd, OMAPFB_SETUP_PLANE, &cursor.plane)) {
            GLASS_LOG_SEVERE("Cannot set plane info to show cursor at %i,%i", x, y);
        }
    }
}


void fbCursorClose() {
    if (fbPlatformCursorClose) {
        return (*fbPlatformCursorClose)();
    }

    if (cursor.fd >= 0) {
        cursor.plane.enabled = 0;
        if (ioctl(cursor.fd, OMAPFB_SETUP_PLANE, &cursor.plane)) {
            GLASS_LOG_SEVERE("Failed to disable cursor plane");
        }
        close(cursor.fd);
        cursor.fd = -1;
    }
}

void glass_cursor_setVisible(jboolean isVisible) {

    if (fbPlatformSetVisible) {
        return (*fbPlatformSetVisible)(isVisible);
    }

    if (isVisible) {
        if (!cursor.isVisible && cursor.currentCursor != 0) {
            FBCursorImage *cursorImage = (FBCursorImage *)jlong_to_ptr(cursor.currentCursor);
            fbCreateCursor(cursorImage->buffer, cursorImage->width, cursorImage->height, cursorImage->bpp);
        } 
    } else {
        fbCursorClose();
    }

    cursor.isVisible = isVisible;
}

void glass_cursor_setNativeCursor(jlong nativeCursorPointer) {

    FBCursorImage *cursorImage = (FBCursorImage *)jlong_to_ptr(nativeCursorPointer);

    if (fbPlatformSetNativeCursor) {
        return (*fbPlatformSetNativeCursor)(nativeCursorPointer);
    }

    if (cursor.currentCursor == nativeCursorPointer) {
        return;
    }

    cursor.currentCursor = nativeCursorPointer;

    if (cursor.isVisible) {
        fbCursorClose();
        fbCreateCursor(cursorImage->buffer, cursorImage->width, cursorImage->height, cursorImage->bpp);
    }
}

void glass_cursor_releaseNativeCursor(jlong nativeCursorPointer) {

    if (fbPlatformReleaseNativeCursor) {
        return (*fbPlatformReleaseNativeCursor)(nativeCursorPointer);
    }

    if (nativeCursorPointer != 0) {
        FBCursorImage *cursorImage = (FBCursorImage *)jlong_to_ptr(nativeCursorPointer);
        free(cursorImage);
    }

    if (cursor.currentCursor == nativeCursorPointer) {
        fbCursorClose();
        cursor.currentCursor = 0;
    }
}


jlong glass_cursor_createNativeCursor(JNIEnv *env, jint x, jint y,  jbyte *srcArray, jint width, jint height) {
    FBCursorImage *cursorImage;
    int imageSize = width * height * 4;

    if (fbPlatformCreateNativeCursor) {
        return (*fbPlatformCreateNativeCursor)(env, x, y, srcArray, width, height);
    }

    cursorImage = (FBCursorImage *)malloc(sizeof(FBCursorImage) + imageSize);

    cursorImage->width = width;
    cursorImage->height = height;
    cursorImage->bpp = 4;
    cursorImage->buffer = (jbyte *)(cursorImage + 1);

    {
        int i;
        for (i = 0; (i + 3) < imageSize; i += 4) {
            if (srcArray[i + 3] != 0) {
                cursorImage->buffer[i] = srcArray[i];
                cursorImage->buffer[i + 1] = srcArray[i + 1];
                cursorImage->buffer[i + 2] = srcArray[i + 2];
                cursorImage->buffer[i + 3] = srcArray[i + 3];
            } else {
                // 171 == 0xAB of the color key.
                cursorImage->buffer[i] = 171;
                cursorImage->buffer[i + 1] = 171;
                cursorImage->buffer[i + 2] = 171;
                cursorImage->buffer[i + 3] = 171;
            }
        }
    }
    return ptr_to_jlong(cursorImage);
}


jboolean glass_cursor_supportsTranslucency() {
    return fbPlatformCursorTranslucency;
}



void glass_cursor_terminate(void) {
    fbCursorClose();
}

#else /* !defined(OMAP3) */

void fbCursorInitialize() { }
void fbCursorSetPosition(int x, int y) { }
void fbCursorClose() { }


void glass_cursor_setVisible(jboolean isVisible) {}
void glass_cursor_setNativeCursor(jlong nativeCursorPointer) {}
void glass_cursor_releaseNativeCursor(jlong nativeCursorPointer) {}
jlong glass_cursor_createNativeCursor(JNIEnv *env, jint x, jint y,  jbyte *srcArray, jint width, jint height) {
    return 0;
}
jboolean glass_cursor_supportsTranslucency() {
    return 0;
}
void glass_cursor_terminate(void) {}

#endif
