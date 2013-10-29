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

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <sys/ioctl.h>
#include <linux/fb.h>

#include "lensPort.h"
#include "lensPortInternal.h"
#include "lensPortLogger.h"

#if defined(OMAP3) || defined(IMX6_PLATFORM) 
# ifndef USE_FB_ROBOT
    #define USE_FB_ROBOT
# endif
#endif

#ifdef OMAP3 
#include <linux/omapfb.h>
#endif

#ifdef USE_FB_ROBOT

jboolean fbFBRobotScreen(jint x, jint y,
                                    jint width, jint height,
                                    jint *pixels) {

    FILE *fb;
    unsigned char *pixelBuffer = NULL;
    unsigned char *dst = (unsigned char *) pixels;
    unsigned int dstByteStride = width * 4;
    int i = 0;
    int fbFileHandle;
    struct fb_var_screeninfo screenInfo;
    int depth; // pixel size in bytes

    GLASS_LOG_FINE("Capture %i,%i+%ix%i", x, y, width, height);
    jboolean result = JNI_FALSE;

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
        return JNI_FALSE;
    }
    GLASS_LOG_FINE("Read screen info: res=%ix%i, offset=%ix%i",
                   screenInfo.xres, screenInfo.yres,
                   screenInfo.xoffset, screenInfo.yoffset);
    GLASS_LOG_FINE("close(%s)", FB_DEVICE);
    close(fbFileHandle);
    depth = screenInfo.bits_per_pixel / 8;
    int pixelBufferLength = screenInfo.xres * screenInfo.yres * depth;
    pixelBuffer = (unsigned char *) malloc(pixelBufferLength);
    if (pixelBuffer == NULL) {
        GLASS_LOG_SEVERE("Failed to allocate temporary pixel buffer");
        return JNI_FALSE;
    }

    GLASS_LOG_FINE("fopen(%s, \"r\") to read %ix%i pixels at bit depth %i",
                   FB_DEVICE, width, height, screenInfo.bits_per_pixel);
    fb = fopen(FB_DEVICE, "r");
    if (fb == NULL) {
        GLASS_LOG_SEVERE("FB: Cannot open framebuffer for reading");
        free(pixelBuffer);
        return JNI_FALSE;
    }

    fseek(fb, screenInfo.yoffset * screenInfo.xres * depth, SEEK_SET);
    int numRead = fread(pixelBuffer, 1,
                        pixelBufferLength,
                        fb);

    if (x < 0) {
        dst += -x * 4;
        width += x;
        x = 0;
    }
    if (y < 0) {
        dst += -y * dstByteStride;
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
        int offset = y * screenInfo.xres * depth + x * depth;
        for (i = 0; i < heightLimit; i++) {
            memcpy(dst + i * dstByteStride, pixelBuffer + offset, widthLimit * depth);
            offset += screenInfo.xres * depth;
        }
    } else {
        free(pixelBuffer);
        fclose(fb);
        GLASS_LOG_FINE("fclose(%s)", FB_DEVICE);
        GLASS_LOG_SEVERE("Failed to take a snapshot, some of parameters are illegal");
        return JNI_FALSE;
    }

    free(pixelBuffer);
    GLASS_LOG_FINE("fclose(%s)", FB_DEVICE);
    fclose(fb);
    return JNI_TRUE;
}

#endif // USE_FB_ROBOT
