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
 
#include "LensCommon.h"
#include "input/LensInput.h"
#include "wm/LensWindowManager.h"

static struct _NativeScreen localScreen;

#define SCREEN_WIDTH   600
#define SCREEN_HEIGHT  800

static void clear_background();


jboolean glass_application_initialize(JNIEnv *env) {
    //nothing to do
    return JNI_TRUE;
}
jboolean lens_input_initialize(JNIEnv *env){
    //nothing to do
    return JNI_TRUE;
}


void glass_screen_clear() {
    clear_background();
}

static int windowIndex = 1;

LensResult glass_window_PlatformWindowData_create(JNIEnv *env,
                                                  NativeWindow window) {

    window->id = windowIndex++;
    window->data = NULL; //no platfrom specific data

    return LENS_OK;
}

LensResult glass_view_PlatformViewData_create(NativeView view) {
    view->data = NULL;
    return LENS_OK;
}

LensResult glass_view_PlatformViewRelease(JNIEnv *env, NativeView view) {
    // No data to free
    return LENS_OK;
}

LensResult glass_window_PlatformWindowRelease(JNIEnv *env, NativeWindow window) {

    // No data to free
    return LENS_OK;
}

static void clear_background() {
    int i;

    NativeScreen fbScreen = glass_screen_getMainScreen();

    int *dst = (int*)fbScreen->data;
    if (!dst) {
        return;
    }

    for (i = 0;i < fbScreen->width * fbScreen->height; i++) {
        (*dst++) = 0xff000000; //todo, use a passed in background ?
    }
}

jboolean glass_window_setAlpha(JNIEnv *env, NativeWindow window, float alpha) {
    window->alpha = alpha;
    lens_wm_repaint(env, window);
    return JNI_TRUE;
}

void glass_pixel_attachIntBuffer(JNIEnv *env,
                                 jint *srcPixels,
                                 NativeWindow window,
                                 jint width,
                                 jint height, int offset) {

    NativeScreen fbScreen = glass_screen_getMainScreen();

    int win_x = window->currentBounds.x;
    int win_y = window->currentBounds.y;
    int win_width, win_height;

    int *framebuffer = (int *) fbScreen->data;

    GLASS_LOG_FINE("IntBuffer at %d,%d, %dx%d\n",
        win_x, win_y,
        width, height);

    if (win_x > fbScreen->width || win_x < 0 ||
        win_y > fbScreen->height || win_y < 0) {
        GLASS_LOG_FINE("IntBuffer window outside of screen");
    }

    win_width = win_x + width > fbScreen->width ? fbScreen->width - win_x : width;
    win_height = win_y + height > fbScreen->height ? fbScreen->height - win_y : height;

    GLASS_LOG_FINE("IntBuffer at %d,%d, %dx%d actual %dx%d\n",
        win_x, win_y,
        width, height,
        win_width, win_height);

    int x,y;
    int *dst, *src;
    int alpha = (unsigned int)(255 * window->alpha) & 0xff;

    for (y=0; y< win_height;y++) {
        dst = framebuffer + ((win_y + y) * fbScreen->width) + win_x;
        src = srcPixels + (y * width);
        for (x=0; x< win_width;x++) {
            jint argb = *src++;
            if (alpha == 255 && (argb & 0xff000000) == 0xff000000) {
                // simple case of no blending
                // if it was RGB->RGB
                //*(dst++) = argb;
                // but we deal in BGR so
                *(dst++) =  (argb & 0xff00ff00) | // A_G_
                            ((argb & 0xff) << 16) | // B
                            ((argb & 0xff0000) >> 16); // R
            } else if (alpha == 0 || (argb & 0xff000000) == 0x00000000) {
                // special case of 0 alpha, full transparent
                // nothing to change
                dst++;
            } else {
                // Src in ABGR pre
                int As = (argb & 0xff000000) >> 24;
                int Bs = (argb & 0xff0000) >> 16;
                int Gs = (argb & 0xff00) >> 8;
                int Rs = (argb & 0xff);

                As = (alpha * As) >> 8;

                // Ad = 255
                int Rd = (*(dst) & 0xff0000) >> 16;
                int Gd = (*(dst) & 0xff00) >> 8;
                int Bd = (*(dst) & 0xff);

                // Cr = Cs * As + Cd * (1 - As)
                // but as we are pre multiplied,
                // Cr = Cs + Cd * (1 - As)
                int Rr = ((Rs*As) + (Rd * (255 - As)))>>8;
                int Gr = ((Gs*As) + (Gd * (255 - As)))>>8;
                int Br = ((Bs*As) + (Bd * (255 - As)))>>8;

                *(dst++) =  0xff000000 | // A
                            (Rr << 16) | // B
                            (Gr << 8) |  // G
                            (Br);        // R
            }
        }
    }
}

void lens_platform_shutdown(JNIEnv *env) {
    //nothing to do
}

/**
 * Get the screen configuration on initialization
*/
NativeScreen lens_screen_initialize(JNIEnv *env) {

    localScreen.width = SCREEN_WIDTH;
    localScreen.height = SCREEN_HEIGHT;
    localScreen.visibleWidth = localScreen.width;
    localScreen.visibleHeight = localScreen.height;
    localScreen.x = 0;
    localScreen.y = 0;
    localScreen.depth = 24;

    // convert pixels/mm to pixels/inch
    localScreen.resolutionX = 100;
    localScreen.resolutionY = 100;

    localScreen.data = malloc(localScreen.width * localScreen.height * sizeof(int));

    return &localScreen;
}

void *glass_window_getPlatformWindow(JNIEnv *env, NativeWindow window) {
    return NULL;
}

char *lens_screen_getFrameBuffer() {
    return (char *)localScreen.data;
}


jboolean glass_screen_capture(jint x,
                              jint y,
                              jint width,
                              jint height,
                              jint *pixels) {

    NativeScreen fbScreen = glass_screen_getMainScreen();
    int *framebuffer = (int *) fbScreen->data;

    GLASS_LOG_FINE("FB Robot: glass_robot_screen_capture(%d,%d,%d,%d)",
                                            x, y, width, height);

    if (x > fbScreen->width || x < 0 ||
        y > fbScreen->height || y < 0) {
        GLASS_LOG_FINE("IntBuffer window outside of screen");

        pixels = NULL;
        return JNI_FALSE;
    }

    int ret_width = x + width > fbScreen->width ? fbScreen->width - x : width;
    int ret_height = y + height > fbScreen->height ? fbScreen->height - y : height;

    int xs, ys;

    for (ys=0; ys< height; ys++) {
        int *src = framebuffer + (y * fbScreen->width) + x;
        int *dst = pixels + (ys * width);
        for (xs=0; xs< width; xs++) {
            if (ys < fbScreen->height && xs < fbScreen->width) {
                jint argb = *src++;
                // if it was RGB->RGB
                //*(dst++) = argb;
                // but we deal in BGR so
                *(dst++) =  (argb & 0xff00ff00) | // A_G_
                            ((argb & 0xff) << 16) | // R
                            ((argb & 0xff0000) >> 16); // B
            } else {
                *(dst++) = 0xff000000; //opaque black
            }
        }
    }

    return JNI_TRUE;
}

