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
#include "com_sun_glass_events_ViewEvent.h"
#include "com_sun_glass_events_WindowEvent.h"

#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <stdint.h>
#include <string.h>
#include <linux/fb.h>
#include <sys/ioctl.h>

#include "platform-util/platformUtil.h"

static struct _NativeScreen fbScreen;

/* fbScanLine is a buffer large enough to hold a row of pixels in the
 * framebuffer format. It is only used in the final blit to the framebuffer,
 * but is pre-allocated at startup in mat_initialize. */
static unsigned char *fbScanLine = NULL;
static int fbScanLineSize = 0;

#define FB_DEVICE "/dev/fb0"
static int windowIndex = 1;


jboolean glass_application_initialize(JNIEnv *env) {
    //nothing to do
    return JNI_TRUE;
}

LensResult glass_window_PlatformWindowData_create(JNIEnv *env,
                                                  NativeWindow window) {

    window->id = windowIndex++;
    window->data = NULL; //no platform specific data

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

jboolean glass_window_setAlpha(JNIEnv *env, NativeWindow window, float alpha) {    
    lens_wm_repaint(env, window);
    return JNI_TRUE;
}

void glass_pixel_attachIntBuffer(JNIEnv *env,
                                 jint *srcPixels,
                                 NativeWindow fbWindow,
                                 jint width,
                                 jint height, int offset) {
    FILE *fb;
    NativeScreen fbScreen = glass_screen_getMainScreen();
    
    GLASS_LOG_FINE("fopen(%s, \"w\") to write %ix%i pixels at depth %i",
                   FB_DEVICE, width, height, fbScreen->depth);
    fb = fopen(FB_DEVICE, "w");
    if (fb == NULL) {
        GLASS_LOG_SEVERE("FB: Cannot open framebuffer for writing");
        return;
    }
    switch (fbScreen->depth) {
            /* To draw a window:
             * 1. Blit blank lines above the window.
             * 2. Blit the window
             * 3. Blit blank lines below the window
             * All blitting is done with a scan line buffer, that can in the
             * future be used to compose multiple windows before the final
             * write to the framebuffer device.
             */
        case 32: {
            int y;
            memset(fbScanLine, 0, fbScanLineSize);
            for (y = 0; y < fbWindow->currentBounds.y; y++) {
                fwrite(fbScanLine, 4, fbScanLineSize >> 2, fb);
            }
            for (y = 0; y < fbWindow->currentBounds.height; y++) {
                // pixels are written unmodified to a 32-bit framebuffer
                memcpy(
                    fbScanLine + (fbWindow->currentBounds.x << 2),
                    srcPixels + fbWindow->currentBounds.width * y,
                    fbWindow->currentBounds.width << 2);
                fwrite(fbScanLine, 4, fbScanLineSize >> 2, fb);
            }
            memset(fbScanLine, 0, fbScanLineSize);
            for (y = fbWindow->currentBounds.x + fbWindow->currentBounds.height; y < fbScreen->height; y++) {
                fwrite(fbScanLine, 4, fbScanLineSize >> 2, fb);
            }
            break;
        }
        case 16: {
            int y;
            memset(fbScanLine, 0, fbScanLineSize);
            for (y = 0; y < fbWindow->currentBounds.y; y++) {
                fwrite(fbScanLine, 2, fbScanLineSize >> 1, fb);
            }
            for (y = 0; y < fbWindow->currentBounds.height; y++) {
                // pixels are packed from 32-bit to 16-bit before writing
                int x;
                jchar *destPixels = (jchar *)
                                    (fbScanLine + (fbWindow->currentBounds.x << 1));

                for (x = 0; x < fbWindow->currentBounds.width; x++) {
                    jint pixel = *srcPixels++;
                    *destPixels++ = ((pixel >> 8) & 0xf800)
                                    | ((pixel >> 5) & 0x7e0)
                                    | ((pixel >> 3) & 0x1f);
                }
                fwrite(fbScanLine, 2, fbScanLineSize >> 1, fb);
            }
            memset(fbScanLine, 0, fbScanLineSize);
            for (y = fbWindow->currentBounds.y + fbWindow->currentBounds.height; y < fbScreen->height; y++) {
                fwrite(fbScanLine, 2, fbScanLineSize >> 1, fb);
            }
            break;
        }
        default:
            GLASS_LOG_SEVERE("Cannot write to screen of depth %i", fbScreen->depth);
    }
    GLASS_LOG_FINE("fclose(%s)", FB_DEVICE);
    fclose(fb);
}


void glass_screen_clear() {
#ifdef ISEGLFB
    //noop for eglfb as screen is managed in prism
    return;
#else
    FILE *fb;
    int y;
    NativeScreen fbScreen = glass_screen_getMainScreen();

    GLASS_LOG_FINE("fopen(%s, \"w\") to clear the background",
                   FB_DEVICE);
    fb = fopen(FB_DEVICE, "w");
    if (fb == NULL) {
        GLASS_LOG_SEVERE("FB: Cannot open framebuffer for writing");
        return;
    }

    memset(fbScanLine, 0, fbScanLineSize);
    switch (fbScreen->depth) {
        case 32: 
            for (y = 0; y < fbScreen->height; y++) {
                fwrite(fbScanLine, 4, fbScanLineSize >> 2, fb);
            }
            GLASS_LOG_FINE("Screen cleared (32bit mode)");
        
            break;
        case 16: 
            for (y = 0; y < fbScreen->height; y++) {
                fwrite(fbScanLine, 2, fbScanLineSize >> 1, fb);
            }
            GLASS_LOG_FINE("Screen cleared (16bit mode)");
        
            break;
        default:
            GLASS_LOG_SEVERE("Cannot write to screen of depth %i", fbScreen->depth);
    }

    GLASS_LOG_FINE("fclose(%s)", FB_DEVICE);
    fclose(fb);
#endif
}


void lens_platform_shutdown(JNIEnv *env) {
    //nothing to do;
}


NativeScreen lens_screen_initialize(JNIEnv *env) {
    int fbFileHandle;
    struct fb_var_screeninfo screenInfo;
    GLASS_LOG_FINE("open(%s, O_RDONLY)", FB_DEVICE);
    fbFileHandle = open(FB_DEVICE, O_RDONLY);
    if (fbFileHandle < 0) {
        GLASS_LOG_SEVERE("Cannot open framebuffer (%s)", strerror(errno));
        return NULL;
    }
    GLASS_LOG_FINE("ioctl(%s, FBIOGET_VSCREENINFO)", FB_DEVICE);
    if (ioctl(fbFileHandle, FBIOGET_VSCREENINFO, &screenInfo)) {
        GLASS_LOG_SEVERE("Cannot get screen info");
        close(fbFileHandle);
        return NULL;
    }

    //print screen properties
    GLASS_IF_LOG_CONFIG {
        GLASS_LOG_CONFIG("%s configuration:", FB_DEVICE);
        GLASS_LOG_CONFIG("xres=%u", screenInfo.xres);
        GLASS_LOG_CONFIG("yres=%u", screenInfo.yres);
        GLASS_LOG_CONFIG("xres_virtual=%u", screenInfo.xres_virtual);
        GLASS_LOG_CONFIG("yres_virtual=%u", screenInfo.yres_virtual);
        GLASS_LOG_CONFIG("xoffset=%u", screenInfo.xoffset);
        GLASS_LOG_CONFIG("yoffset=%u", screenInfo.yoffset);
        GLASS_LOG_CONFIG("bits_per_pixel=%u", screenInfo.bits_per_pixel);
        GLASS_LOG_CONFIG("grayscale=%u", screenInfo.grayscale);
        // width and height are supposed to be unsigned
        // but can return -1 as a signed integer
        GLASS_LOG_CONFIG("width=%imm", screenInfo.width);
        GLASS_LOG_CONFIG("height=%imm", screenInfo.height);
        GLASS_LOG_CONFIG("sync=%u", screenInfo.sync);
        GLASS_LOG_CONFIG("vmode=%u", screenInfo.vmode);
        GLASS_LOG_CONFIG("rotate=%u", screenInfo.rotate);
    }

    /* In screenInfo:
     * xres = physical width in pixels
     * xres_virtual = virtual width in pixels
     * xoffset = X offset of physical display into virtual display
     * width = physical width of display in millimeters
     */
    fbScreen.width = screenInfo.xres;
    fbScreen.height = screenInfo.yres;
    fbScreen.visibleWidth = screenInfo.xres;
    fbScreen.visibleHeight = screenInfo.yres;
    fbScreen.x = screenInfo.xoffset;
    fbScreen.y = screenInfo.yoffset;
    fbScreen.depth = screenInfo.bits_per_pixel;
    // Only use reported physical screen size if it makes sense.
    // Sometimes -1 is reported for the physical width and height.
    if (screenInfo.width > 0 && screenInfo.width < INT32_MAX) {
        // convert pixels/mm to pixels/inch
        fbScreen.resolutionX = (fbScreen.visibleWidth * 254)
                               / (fbScreen.width * 10);
    } else {
        // take a guess at pixel density
        fbScreen.resolutionX = 96;
    }
    if (screenInfo.height > 0 && screenInfo.height < INT32_MAX) {
        fbScreen.resolutionY = (fbScreen.visibleHeight * 254)
                               / (fbScreen.height * 10);
    } else {
        fbScreen.resolutionY = 96;
    }

    GLASS_LOG_CONFIG("Set resolution to %ix%i dots per inch",
                     fbScreen.resolutionX, fbScreen.resolutionY);

    if (fbScanLine == NULL) {
        int bytesPerPixel = 0;
        switch (fbScreen.depth) {
            case 16:
                bytesPerPixel = 2;
                break;
            case 24:
            case 32:
                bytesPerPixel = 4;
                break;
            default:
                GLASS_LOG_SEVERE("Cannot write to screen of depth %i", fbScreen.depth);
                return NULL;
        }
        fbScanLineSize = fbScreen.width * bytesPerPixel;
        fbScanLine = malloc(fbScanLineSize);
        if (fbScanLine == NULL) {
            GLASS_LOG_SEVERE("Cannot allocate scan line of size %i", fbScanLineSize);
            return NULL;
        }
    }

    close(fbFileHandle);

    return &fbScreen;

}

void *glass_window_getPlatformWindow(JNIEnv *env, NativeWindow window) {
    return window;
}

char *lens_screen_getFrameBuffer() {
    return NULL;
}


jboolean glass_screen_capture(jint x, jint y,
                                    jint width, jint height,
                                    jint *pixels) {

    if (fbRobotScreenCapture) {
       return (*fbRobotScreenCapture)(x, y, width, height, pixels);
    }
    return 0;
}

LensResult lens_platform_windowMinimize(JNIEnv *env,
                                        NativeWindow window,
                                        jboolean toMinimize) {
    //noop for fb
    return LENS_OK;
}

LensResult lens_platform_windowSetVisible(JNIEnv *env,
                                        NativeWindow window,
                                        jboolean visible) {
    //noop for fb
    return LENS_OK;
}
