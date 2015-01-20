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

#include "com_sun_glass_ui_monocle_DispmanAcceleratedScreen.h"
#include "Monocle.h"

#include <EGL/egl.h>
#include <fcntl.h>
#include <linux/fb.h>
#include <sys/ioctl.h>

#ifdef USE_DISPMAN
//Broadcom specials
#include "wrapped_bcm.h"
#endif /* USE_DISPMAN */

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_monocle_DispmanAcceleratedScreen__1platformGetNativeWindow
    (JNIEnv *env, jobject obj, jint displayID, jint layerID) {

#ifdef USE_DISPMAN

    EGL_DISPMANX_WINDOW_T *dispmanWindow;
    DISPMANX_DISPLAY_HANDLE_T display = 0;
    DISPMANX_ELEMENT_HANDLE_T element;
    DISPMANX_UPDATE_HANDLE_T update;
    VC_RECT_T dst = { 0, 0, 0, 0 };
    VC_RECT_T src = { 0, 0, 0, 0 };

    load_bcm_symbols();

    (*wr_bcm_host_init)();

    dispmanWindow = (EGL_DISPMANX_WINDOW_T *)calloc(sizeof(EGL_DISPMANX_WINDOW_T), 1);

    display = (*wr_vc_dispmanx_display_open)(displayID);
    if (display == 0) {
        fprintf(stderr, "Dispman: Cannot open display\n");
        return 0;
    }
    int fbFileHandle;
    struct fb_var_screeninfo screenInfo;
    fbFileHandle = open("/dev/fb0", O_RDONLY);
    if (fbFileHandle < 0) {
        fprintf(stderr, "Cannot open framebuffer\n");
        return 0;
    }
    if (ioctl(fbFileHandle, FBIOGET_VSCREENINFO, &screenInfo)) {
        fprintf(stderr, "Cannot get screen info\n");
        return 0;
    }
    close(fbFileHandle);

    dst.width = screenInfo.xres;
    dst.height = screenInfo.yres;
    src.width = screenInfo.xres << 16;
    src.height = screenInfo.yres << 16;

    VC_DISPMANX_ALPHA_T alpha;
    alpha.flags = DISPMANX_FLAGS_ALPHA_FROM_SOURCE;
    alpha.opacity = 0xff;
    alpha.mask = (DISPMANX_RESOURCE_HANDLE_T) 0;
    update = (*wr_vc_dispmanx_update_start)(0);
    element = (*wr_vc_dispmanx_element_add)(
                  update,
                  display,
                  layerID,
                  &dst,
                  0 /*src*/,
                  &src,
                  DISPMANX_PROTECTION_NONE,
                  &alpha,
                  0 /*clamp*/,
                  0 /*transform*/);

    dispmanWindow->element = element;
    dispmanWindow->width = screenInfo.xres;
    dispmanWindow->height = screenInfo.yres;
    (*wr_vc_dispmanx_update_submit_sync)(update);

    return asJLong((NativeWindowType)dispmanWindow);
#else
    return 0l;
#endif /* USE_DISPMAN */
}