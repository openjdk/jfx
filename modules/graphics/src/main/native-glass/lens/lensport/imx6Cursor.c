/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
 
#ifdef IMX6_PLATFORM

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <linux/fb.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <dlfcn.h>
#include <errno.h>
#include <linux/mxcfb.h>  // note: i.MX unique header

#include "lensPort.h"
#include "lensPortInternal.h"
#include "lensPortLogger.h"

#define LENSFB_IMX6_CURSOR_DEVICE "/dev/fb1"
#define LENSFB_IMX6_CURSOR_SIZE 16

typedef struct {
    int fd;
    int width;
    int height;
    int x,y;
    int screenWidth;
    int screenHeight;
    jlong currentCursor;
    // When the cursor is at the extreme right or bottom of the screen, it
    // needs to be shifted to show in the correct location. IMX doesn't let us
    // position the framebuffer so that it is only partically visible.
    int xShift;
    int yShift;
    jboolean isVisible;
} Imx6FBCursor;

static Imx6FBCursor cursor = { .fd = -1, .width = 0, .height = 0, .x = 0, .y = 0, .currentCursor = 0, .isVisible = 0,
                              .xShift = 0, .yShift = 0 };

//TODO : platform supports 32 and 16 bits, how do we choose what to use ?
static int use32bit = 0;

typedef struct {
    jint width;
    jint height;
    jint x;
    jint y;
    jbyte* buffer;
    jint bufferSize;
} Imx6CursorImage;


static void fbImx6BlankCursor(){
     char buffer[256];
     int bytesToWrite;

     // Set buffer to be transparent
     memset((void*)buffer, use32bit ? 0 : LENSFB_16_CURSOR_COLOR_KEY, sizeof(buffer));

     if (lseek(cursor.fd, 0, SEEK_SET) == -1) {
         GLASS_LOG_SEVERE("Cannot rewrite cursor image");
         return;
     } 

     bytesToWrite = cursor.width * cursor.height * (use32bit ? 4 : 2);
     while (bytesToWrite > 0) {
         int n = bytesToWrite > sizeof(buffer) ? sizeof(buffer) : bytesToWrite;
         int res;
         if ((res = write(cursor.fd, buffer, n)) < n) {
             GLASS_LOG_SEVERE("Cannot write cursor plane %i bytes, wrote %i bytes", n, res);
             return;
         }
         bytesToWrite -= n;
     }
}


/* Updates values of xShift and yShift based on the cursor location */
static void fbImx6AdjustShift(){
    if (cursor.x > cursor.screenWidth - cursor.width) {
        cursor.xShift = cursor.width + cursor.x - cursor.screenWidth;
    } else {
        cursor.xShift = 0;
    }
    if (cursor.y > cursor.screenHeight - cursor.height) {
        cursor.yShift = cursor.height + cursor.y - cursor.screenHeight;
    } else {
        cursor.yShift = 0;
    }
}

/* Writes an image into the cursor framebuffer with the given x and y shifts. */
static void fbImx6WriteCursor(int fd, jbyte *cursorImage, int bpp) {
    int i, j, k;
    char buffer[256];
    int cursorSize = cursor.width * cursor.height * bpp;
    int xShift = cursor.xShift;
    int yShift = cursor.yShift;

    if (!cursor.isVisible) {
        return;
    }

    if (lseek(cursor.fd, 0, SEEK_SET) == -1) {
        GLASS_LOG_SEVERE("Cannot rewrite cursor image");
        return;
     }

    GLASS_LOG_FINEST("Cursor shift = (%i, %i) at (%i, %i)\n",
                     xShift, yShift, cursor.x, cursor.y);
    if (xShift == 0 && yShift == 0) {
        GLASS_LOG_FINEST("write(cursor.fd, .. %i)", cursorSize);
        if ((i = write(cursor.fd, cursorImage, cursorSize)) < cursorSize) {
            GLASS_LOG_SEVERE("Cannot write cursor plane cursorSize : %i, wrote %i bytes", cursorSize, i);
        }
        return;
    }

    // Set buffer to be transparent
    memset((void*)buffer, use32bit ? 0 : LENSFB_16_CURSOR_COLOR_KEY, sizeof(buffer));

    // fill the y-shift rectangular area
    for (i = 0; i < yShift; i++) {
        for (j = 0; j < cursor.width * bpp; j += sizeof(buffer)) {
            size_t n = cursor.width * bpp - j;
            if (n > sizeof(buffer)) {
                n = sizeof(buffer);
            }
            GLASS_LOG_FINEST("write(cursor.fd, .. %u)", n);
            if (write(cursor.fd, buffer, n) < (int)n) {
                GLASS_LOG_SEVERE("Cannot write cursor plane");
                return;
            }
        }
    }
    // set the rest of the image
    for (i = 0; i < cursor.height - yShift; i++) {
        for (j = 0; j < xShift * bpp; j += sizeof(buffer)) {
            size_t n = xShift * bpp;
            if (n > sizeof(buffer)) {
                n = sizeof(buffer);
            }
            GLASS_LOG_FINEST("write(cursor.fd, .. %u)", n);
            if (write(cursor.fd, buffer, n) < (int)n) {
                GLASS_LOG_SEVERE("Cannot write cursor plane");
                return;
            }
        }
        size_t n = (cursor.width - xShift) * bpp;
        GLASS_LOG_FINEST("write(cursor.fd, .. %u)", n);
        if (write(cursor.fd, cursorImage + i * cursor.width * bpp, n) < (int)n) {
            GLASS_LOG_SEVERE("Cannot write cursor plane");
            return;
        }
    }
}





static int fbImx6ChangeCursorSize(int width, int height) {

    struct fb_var_screeninfo screenInfo;
    if (ioctl(cursor.fd, FBIOGET_VSCREENINFO, &screenInfo)) {
        GLASS_LOG_SEVERE("Error %s in getting screen info", strerror(errno));
        return -1;
    }

    screenInfo.xres = width;
    screenInfo.yres = height;
    screenInfo.xres_virtual = width;
    screenInfo.yres_virtual = height;
    screenInfo.xoffset = 0;
    screenInfo.yoffset = 0;
    screenInfo.activate = 0;

    if(ioctl(cursor.fd, FBIOPUT_VSCREENINFO, &screenInfo)) {
        GLASS_LOG_SEVERE("Error %s in setting screen info", strerror(errno));
        return -1;
    }

    cursor.width = width;
    cursor.height = height;

    return 0;
}



static void fbImx6CursorInitialize(int screenWidth, int screenHeight, int screenDepth) {

    struct fb_var_screeninfo screenInfo;
    int rc;
    char * zero = "0\n";
    int fbc = -1;
    int fbo = -1;
 
    //TODO : the following 2 settings can be moved to a setup script procedure
    if ((fbc = open("/sys/class/graphics/fbcon/cursor_blink",O_RDWR)) < 0) {
        GLASS_LOG_SEVERE("Error %s in opening /sys/class/graphics/fbcon/cursor_blink", strerror(errno));
    } else {
        write(fbc,zero,1);
        close(fbc);
    }
    if ((fbo = open("/sys/class/graphics/fb1/blank", O_RDWR)) < 0) {
        GLASS_LOG_SEVERE("Error %s in opening /sys/class/graphics/fb1/blank", strerror(errno));
    } else {
        write(fbo,zero,1);
        close(fbo);
    }

    // Init cursor global variable fields
    cursor.width = LENSFB_IMX6_CURSOR_SIZE;
    cursor.height = LENSFB_IMX6_CURSOR_SIZE;
    cursor.x = 0;
    cursor.y = 0;
    cursor.currentCursor = 0;
    cursor.isVisible = 0;
    cursor.screenWidth = screenWidth;
    cursor.screenHeight = screenHeight;

    cursor.fd = open(LENSFB_IMX6_CURSOR_DEVICE, O_RDWR);
    if (cursor.fd < 0) {
        GLASS_LOG_SEVERE("Cannot open framebuffer device %s",LENSFB_IMX6_CURSOR_DEVICE);
        return;
    }

    if (ioctl(cursor.fd, FBIOGET_VSCREENINFO, &screenInfo)) {
        GLASS_LOG_SEVERE("Error %s in getting screen info", strerror(errno));
        return;
    }

    GLASS_LOG_INFO("Initializing %d bits pixel %dx%d cursor, current %d bits\n",
                   (use32bit ? 32 : 16), LENSFB_IMX6_CURSOR_SIZE, LENSFB_IMX6_CURSOR_SIZE, screenInfo.bits_per_pixel);

    screenInfo.xres = LENSFB_IMX6_CURSOR_SIZE;
    screenInfo.yres = LENSFB_IMX6_CURSOR_SIZE;
    screenInfo.xres_virtual = LENSFB_IMX6_CURSOR_SIZE;
    screenInfo.yres_virtual = LENSFB_IMX6_CURSOR_SIZE;
    screenInfo.xoffset = 0;
    screenInfo.yoffset = 0;
    screenInfo.activate = 0;

    if (use32bit) {
        screenInfo.bits_per_pixel = 32;
        screenInfo.red.length = 8;
        screenInfo.red.offset = 16;
        screenInfo.green.length = 8;
        screenInfo.green.offset = 8;
        screenInfo.blue.length = 8;
        screenInfo.blue.offset = 0;
        screenInfo.transp.length = 8;
        screenInfo.transp.offset = 24;
    }  else {
        // 565
        screenInfo.bits_per_pixel = 16;
        screenInfo.red.length = 5;
        screenInfo.red.offset = 11;
        screenInfo.green.length = 6;
        screenInfo.green.offset = 5;
        screenInfo.blue.length = 5;
        screenInfo.blue.offset = 0;
        screenInfo.transp.length = 0;
        screenInfo.transp.offset = 0;
    }

    if(ioctl(cursor.fd, FBIOPUT_VSCREENINFO, &screenInfo)) {
        GLASS_LOG_SEVERE("Error %s in setting screen info", strerror(errno));
        return;
    }

    if(ioctl(cursor.fd, FBIOBLANK, FB_BLANK_UNBLANK)) {
         GLASS_LOG_SEVERE("Error %s in gstting cursor no-blanking", strerror(errno));
         return;
    }


    if (use32bit) {
        // alpha is taken from each pixel
        struct mxcfb_loc_alpha loc_alpha;
        loc_alpha.enable = 1;
        loc_alpha.alpha_in_pixel = 1;
        if (ioctl(cursor.fd, MXCFB_SET_LOC_ALPHA, &loc_alpha) < 0) {
            GLASS_LOG_SEVERE("Error %s in setting local alpha", strerror(errno));
        } 

    } else {
        struct mxcfb_color_key color_key;
        color_key.color_key = RGB565TOCOLORKEY(LENSFB_16_CURSOR_COLOR_KEY);
        color_key.enable = 1;
        if ( ioctl(cursor.fd, MXCFB_SET_CLR_KEY, &color_key) < 0) {
            GLASS_LOG_SEVERE("Error %s in setting 16 bits color key", strerror(errno));
        }

        struct mxcfb_gbl_alpha gbl_alpha;
        gbl_alpha.alpha = 255;
        gbl_alpha.enable = 1;
        if(ioctl(cursor.fd, MXCFB_SET_GBL_ALPHA, &gbl_alpha) < 0) {
              GLASS_LOG_SEVERE("Error %s in setting global alpha", strerror(errno));
        }
    }

    struct mxcfb_pos cpos = {(screenWidth - 16)/2, (screenHeight - 16)/2};
    if (ioctl(cursor.fd, MXCFB_SET_OVERLAY_POS, &cpos)) {
        GLASS_LOG_SEVERE("Error %s in setting overlay position", strerror(errno));
    }

    fbImx6BlankCursor();
}



static jlong fbImx6CreateNativeCursor(JNIEnv *env, jint x, jint y,  jbyte *srcArray, jint width, jint height) {

    Imx6CursorImage *cursorImage = (Imx6CursorImage *)malloc(sizeof(Imx6CursorImage));
    cursorImage->x = x;
    cursorImage->y = y;
    cursorImage->width = width;
    cursorImage->height = height;
    cursorImage->bufferSize = width * height * (use32bit ? 4 : 2);
    cursorImage->buffer = (jbyte*)malloc(cursorImage->bufferSize);

    GLASS_LOG_INFO("Creating x : %d y : %d width : %d height : %d cursor %d bits per pixel",x, y, width, height, (use32bit ? 32 : 16));

    if (use32bit) {
        memcpy((void*)(cursorImage->buffer), srcArray, cursorImage->bufferSize);
    } else {
        //565
        int i;
        uint16_t* dst = (uint16_t*)(cursorImage->buffer);
        uint32_t* src = (uint32_t*)srcArray;
        for (i = 0; i < cursorImage->bufferSize; i += 2) {
            int pixel = *src++;
            if ((pixel & 0xff000000) != 0) {
                *dst++ = ((pixel >> 8) & 0xf800)
                         | ((pixel >> 5) & 0x7e0)
                         | ((pixel >> 3) & 0x1f);
            } else {
                *dst++ = LENSFB_16_CURSOR_COLOR_KEY;
            }
        }
    }

    return ptr_to_jlong(cursorImage);   
}


static void fbImx6ReleaseNativeCursor(jlong nativeCursorHandle) {

    Imx6CursorImage *cursorImage = (Imx6CursorImage *)jlong_to_ptr(nativeCursorHandle);

    if (cursorImage->buffer != NULL) {
        free(cursorImage->buffer);
    }

    free(cursorImage);
}


static void fbImx6SetNativeCursor(jlong nativeCursorHandle) {

    Imx6CursorImage *cursorImage = (Imx6CursorImage *)jlong_to_ptr(nativeCursorHandle);

    if (cursor.fd != -1 && cursor.currentCursor != nativeCursorHandle && 
        cursorImage != NULL && cursorImage->buffer != NULL) 
    {
        if (cursorImage->width != cursor.width || cursorImage->height != cursor.height) {
            fbImx6BlankCursor();
            if (fbImx6ChangeCursorSize(cursorImage->width, cursorImage->height)) {
                GLASS_LOG_SEVERE("Error in fbImx6ChangeCursorSize() w : %d h : %d", cursorImage->width, cursorImage->height);
                return;
            }
        }

        cursor.currentCursor = nativeCursorHandle;

        fbImx6AdjustShift();
        fbImx6WriteCursor(cursor.fd, cursorImage->buffer, use32bit ? 4 : 2);
    } 
}



static void fbImx6CursorSetPosition(int x, int y) {
    int xShift = cursor.xShift;
    int yShift = cursor.yShift;

    if (x < 0) {
        x = 0;
    }
    if (y < 0) {
        y = 0;
    }
    if (x > cursor.screenWidth - 1) {
        x = cursor.screenWidth - 1;
    }
    if (y > cursor.screenHeight - 1) {
        y = cursor.screenHeight - 1;
    }

    cursor.x = x;
    cursor.y = y;

    if (cursor.isVisible) {

        fbImx6AdjustShift();
        x -= cursor.xShift;
        y -= cursor.yShift;
        if (xShift != cursor.xShift || yShift != cursor.yShift) {
            GLASS_LOG_FINEST("Calling lseek to rewind cursor fd");
            Imx6CursorImage *fbCursorImage = (Imx6CursorImage *)
                jlong_to_ptr(cursor.currentCursor);
            fbImx6WriteCursor(cursor.fd, fbCursorImage->buffer, use32bit ? 4 : 2);
        }

        struct mxcfb_pos cpos = {x, y};
        if (ioctl(cursor.fd, MXCFB_SET_OVERLAY_POS, &cpos)) {
            GLASS_LOG_SEVERE("Error %s in setting overlay position", strerror(errno));
        }
    }
}


void fbImx6CursorClose() {
    if (cursor.fd >= 0) {
        if (cursor.isVisible) {
            fbImx6BlankCursor();
        }
        close(cursor.fd);
        cursor.fd = -1;
        cursor.isVisible = 0;
        cursor.currentCursor = 0;
        cursor.width = 0;
        cursor.height = 0;
    }
}

static void fbImx6SetVisible(jboolean isVisible) {
    if (isVisible) {
        if (!cursor.isVisible && cursor.currentCursor != 0) {
            cursor.isVisible = 1;
            Imx6CursorImage *fbCursorImage = (Imx6CursorImage *)
                  jlong_to_ptr(cursor.currentCursor);
            fbImx6WriteCursor(cursor.fd, fbCursorImage->buffer, use32bit ? 4 : 2);
        }
    } else {
        if (cursor.isVisible) {
            fbImx6BlankCursor();
        }
        cursor.isVisible = 0;
    }
}

static char * platformName = "imx6";

jboolean check_imx6_cursor(LensNativePort *lensPort) {

    if (access("/dev/mxc_vpu", F_OK) == 0) {
        lensPort->platformName = platformName;
        lensPort->setNativeCursor = fbImx6SetNativeCursor;
        lensPort->cursorInitialize = fbImx6CursorInitialize;
        lensPort->cursorSetPosition = fbImx6CursorSetPosition;
        lensPort->cursorClose = fbImx6CursorClose;
        lensPort->createNativeCursor = fbImx6CreateNativeCursor;
        lensPort->releaseNativeCursor = fbImx6ReleaseNativeCursor;
        lensPort->setVisible = fbImx6SetVisible;
        lensPort->cursorTranslucency = (use32bit ? JNI_TRUE : JNI_FALSE);

        return JNI_TRUE;
    } 
    
    return JNI_FALSE;
}

#endif // IMX6_PLATFORM
