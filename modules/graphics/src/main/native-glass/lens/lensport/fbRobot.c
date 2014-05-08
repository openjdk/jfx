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
    GLASS_LOG_FINE("Read screen info: res=%ix%i, offset=%ix%i depth=%d",
                   screenInfo.xres, screenInfo.yres,
                   screenInfo.xoffset, screenInfo.yoffset,
                   screenInfo.bits_per_pixel);
    GLASS_LOG_FINE("close(%s)", FB_DEVICE);
    close(fbFileHandle);
    depth = screenInfo.bits_per_pixel / 8; // bytes per pixel

    int inStride = screenInfo.xres * depth;

    // allocate room for one scan line
    int pixelBufferLength = inStride;
    pixelBuffer = (unsigned char *) malloc(pixelBufferLength);
    if (pixelBuffer ==  ((unsigned char *)0)) {
        GLASS_LOG_SEVERE("Failed to allocate temporary pixel buffer");
        return JNI_FALSE;
    }

    fb = fopen(FB_DEVICE, "r");
    if (fb == NULL) {
        GLASS_LOG_SEVERE("FB: Cannot open framebuffer for reading");
        free(pixelBuffer);
        return JNI_FALSE;
    }

    int row, col;

    // initial skip... offset plus a jump to the first row
    int nextbyte = (screenInfo.yoffset * screenInfo.xres) * depth;

    if ((y > 0) && (y < (int)screenInfo.yres)) {
        nextbyte += y * inStride;
    }

    if(fseek(fb, nextbyte, SEEK_SET)) {
        GLASS_LOG_SEVERE("fseek(fb0) failed");
        free(pixelBuffer);
        return JNI_FALSE;
    }

    unsigned int * dstPixel = (unsigned int *)pixels;

    for (row=y; row < y + height; row ++) {
        if ((row < 0) || (row >= (int)screenInfo.yres))   {
            // off the top or bottom of the window
            for (col = 0; col < width; col ++ ,dstPixel ++) {
                *dstPixel = 0xff000000; //black
            }
        } else {
            // in the y body

            // if off the edge, left
            for (col = x; col < 0; col ++ ,dstPixel ++) {
                *dstPixel = 0xff000000; //black
            }

            // real one row of pixels in the image 
            if (col < (int)screenInfo.xres) {
                int numRead = fread(pixelBuffer, 1,
                            inStride,
                            fb);
                if (numRead != inStride) {
                    GLASS_LOG_SEVERE("Mismatch reading pixels in screen capture");
                    free(pixelBuffer);
                    return JNI_FALSE;
                }

                unsigned short * srcShortPixel = (unsigned short *)pixelBuffer;
                unsigned int * srcIntPixel = (unsigned int *)pixelBuffer;

                for (; col < x + width && col < (int)screenInfo.xres ; col ++ ,dstPixel ++) {
                   if (depth == 2) {
                        // assuming 565 here
                        unsigned short sp = srcShortPixel[col];
                        unsigned int red = (int)((sp & 0xF800) >> 11) << 3;
                        unsigned int green =(int) ((sp & 0x7E0) >> 5) << 2;
                        unsigned int blue =(int) (sp & 0x1F) << 3;
                        unsigned int pixel = (unsigned int) (0xff000000 | (red << 16) | (green << 8) | blue); 
                        *dstPixel = pixel;
                   } else {
                        *dstPixel = 0xff000000 | srcIntPixel[col];
                   }
                }
            }

            // if off the edge right
            for (; col < x + width; col ++ ,dstPixel ++) {
                *dstPixel = 0xff000000; //black
            }
        }
    }

    free(pixelBuffer);
    GLASS_LOG_FINE("fclose(%s)", FB_DEVICE);
    fclose(fb);
    return JNI_TRUE;
}

#endif // USE_FB_ROBOT
