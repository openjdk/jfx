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

/*
 * an EGL library wrapper
 *
 * The why of this GL/EGL library wrapper:
 * Vendors provide very different versions of the libraries, and linking
 * directly with those libraries introduces a loader depenancy chain.
 * But using this wrapper allows us to avoid this at the code of loading
 * all the symbols needed at runtime and one level of indirection.
 *
 * to use, simply link this file, as well as the provided stub versions of -lEGL -lGLESv2
 * call openNativeFramebuffer() to get the NativeDisplayType and the NativeWindowType
 * that match the platform.
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <linux/fb.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <sys/ioctl.h>

#define WRAPPEDAPI
#include "wrapped_egl.h"

#ifdef USE_DISPMAN
//Broadcom specials

#ifndef BCM_HOST_H
#include "bcm_host.h"
#endif

static void (*wr_bcm_host_init)(void);
static  int (*wr_vc_dispmanx_display_close)(DISPMANX_DISPLAY_HANDLE_T display);
static  int (*wr_vc_dispmanx_display_get_info)(DISPMANX_DISPLAY_HANDLE_T display, DISPMANX_MODEINFO_T *pinfo);
static  DISPMANX_DISPLAY_HANDLE_T(*wr_vc_dispmanx_display_open)(uint32_t device);
static  DISPMANX_ELEMENT_HANDLE_T(*wr_vc_dispmanx_element_add)(
    DISPMANX_UPDATE_HANDLE_T update, DISPMANX_DISPLAY_HANDLE_T display,
    int32_t layer, const VC_RECT_T *dest_rect, DISPMANX_RESOURCE_HANDLE_T src,
    const VC_RECT_T *src_rect, DISPMANX_PROTECTION_T protection,
    VC_DISPMANX_ALPHA_T *alpha, DISPMANX_CLAMP_T *clamp, DISPMANX_TRANSFORM_T transform);
static  DISPMANX_UPDATE_HANDLE_T(*wr_vc_dispmanx_update_start)(int32_t priority);
static  int (*wr_vc_dispmanx_update_submit_sync)(DISPMANX_UPDATE_HANDLE_T update);
#endif /* USE_DISPMAN */

int useDispman = 0;

#define DEBUG
#ifdef DEBUG

// This method is good for early debug, but is unneeded for general use
static void *get_check_symbol(void *handle, const char *name) {
    void *ret = dlsym(handle, name);
    if (!ret) {
        fprintf(stderr, "failed to load symbol %s\n", name);
    }
    return ret;
}
#define GET_SYMBOL(handle,name) get_check_symbol(handle,name)

#else // #ifdef DEBUG

#define GET_SYMBOL(handle,name) dlsym(handle,name)

#endif

void *libglesv2;
void *libegl;


/***************************** Special cases  ***************************/

static EGLDisplay(*_eglGetDisplay)(EGLNativeDisplayType display_id);

EGLDisplay wr_eglGetDisplay(EGLNativeDisplayType display_id) {
    EGLDisplay ret = (*_eglGetDisplay)(display_id);
    return ret;
}

static EGLNativeWindowType(*_ANDROID_getNativeWindow)();

/***************************** EGL *************************************/

static int load_egl_symbols(void *lib) {
    int error = 0;

    if (!(_eglGetDisplay = GET_SYMBOL(lib, "eglGetDisplay"))) {
        error++;
    }

    if (error) {
        // handle error conditions better ?
        fprintf(stderr, "failed to load all EGL symbols %d\n", error);
        return 1;
    }
    return 0;
}

/*************************************** BROADCOM ******************************************/

static int load_bcm_symbols(void *lib) {
#ifdef USE_DISPMAN
    int error = 0;

    if (!(wr_bcm_host_init = GET_SYMBOL(lib, "bcm_host_init"))) {
        error++;
    }
    if (!(wr_vc_dispmanx_display_close = GET_SYMBOL(lib, "vc_dispmanx_display_close"))) {
        error++;
    }
    if (!(wr_vc_dispmanx_display_get_info = GET_SYMBOL(lib, "vc_dispmanx_display_get_info"))) {
        error++;
    }
    if (!(wr_vc_dispmanx_display_open = GET_SYMBOL(lib, "vc_dispmanx_display_open"))) {
        error++;
    }
    if (!(wr_vc_dispmanx_element_add = GET_SYMBOL(lib, "vc_dispmanx_element_add"))) {
        error++;
    }
    if (!(wr_vc_dispmanx_update_start = GET_SYMBOL(lib, "vc_dispmanx_update_start"))) {
        error++;
    }
    if (!(wr_vc_dispmanx_update_submit_sync = GET_SYMBOL(lib, "vc_dispmanx_update_submit_sync"))) {
        error++;
    }

    if (error) {
        // handle error conditions better ?
        fprintf(stderr, "failed to load all bcm_host symbols %d\n", error);
        return 1;
    }
    return 0;
#else
    return 1;
#endif /* USE_DISPMAN */
}

static int done_loading_symbols = 0;

/***************************** UTILITY ********************************/

int load_wrapped_gles_symbols() {

    if (done_loading_symbols)  {
        return 0;
    }
    done_loading_symbols = 1;

    //Note that there is an order depenacy here - The PI wants GLES first.
    // Other platfroms needs the RTLD_GLOBAL to resolve symbols correctly.


    libglesv2 = dlopen("libGLESv2.so", RTLD_LAZY | RTLD_GLOBAL);
    if (!libglesv2) {
        fprintf(stderr, "Did not find libGLESv2.so %s\n", dlerror());
        return 0;
    }

    libegl = dlopen("libEGL.so", RTLD_LAZY | RTLD_GLOBAL);
    if (!libegl) {
        fprintf(stderr, "Did not find libEGL.so %s\n", dlerror());
        return 0;
    }

    void *libbcm = dlopen("libbcm_host.so", RTLD_LAZY);

    int error = 0;

    if (libbcm) {
        useDispman = 1;
        error += load_bcm_symbols(libbcm);
    }

    error += load_egl_symbols(libegl);

    return error;
}

EGLNativeDisplayType getNativeDisplayType() {
    static EGLNativeDisplayType cachedNativeDisplayType;
    static int cached = 0;

    if (!done_loading_symbols) {
        load_wrapped_gles_symbols();
    }

    if (!cached) {
        if (useDispman) {
            cachedNativeDisplayType = EGL_DEFAULT_DISPLAY;
        } else {
            cachedNativeDisplayType = (EGLNativeDisplayType)NULL;
        }

        cached ++;
    }


    return cachedNativeDisplayType;
}

EGLNativeWindowType getNativeWindowType() {
    static NativeWindowType cachedWindowType;
    static int cached = 0;

    if (!cached) {

        if (!done_loading_symbols) {
            load_wrapped_gles_symbols();
        }

        if (useDispman) {
#ifdef USE_DISPMAN

            EGL_DISPMANX_WINDOW_T *dispmanWindow;
            DISPMANX_DISPLAY_HANDLE_T display = 0;
            DISPMANX_ELEMENT_HANDLE_T element;
            DISPMANX_UPDATE_HANDLE_T update;
            VC_RECT_T dst = { 0, 0, };
            VC_RECT_T src = { 0, 0, };

            (*wr_bcm_host_init)();

            dispmanWindow = (EGL_DISPMANX_WINDOW_T *)calloc(sizeof(EGL_DISPMANX_WINDOW_T), 1);

            display = (*wr_vc_dispmanx_display_open)(0 /* LCD */);
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

            update = (*wr_vc_dispmanx_update_start)(0);
            element = (*wr_vc_dispmanx_element_add)(
                          update,
                          display,
                          0 /*layer*/,
                          &dst,
                          0 /*src*/,
                          &src,
                          DISPMANX_PROTECTION_NONE,
                          0 /*alpha*/,
                          0 /*clamp*/,
                          0 /*transform*/);

            dispmanWindow->element = element;
            dispmanWindow->width = screenInfo.xres;
            dispmanWindow->height = screenInfo.yres;
            (*wr_vc_dispmanx_update_submit_sync)(update);

            cachedWindowType = (NativeWindowType)dispmanWindow;
#endif /* USE_DISPMAN */
        } else {
            printf("Using good old NULL\n");
            cachedWindowType = NULL;      // hence the EGL NULL
        }
        cached ++;
    }
#ifdef ANDROID_NDK
    //don't cache for Android!
    printf("Using getAndroidNativeWindow() from glass.\n");
    void *libglass_android = dlopen("libglass_lens_android.so", RTLD_LAZY | RTLD_GLOBAL);
    if (!libglass_android) {
        fprintf(stderr, "Did not find libglass_lens_android.so %s\n", dlerror());
           return NULL;
    }
    _ANDROID_getNativeWindow = GET_SYMBOL(libglass_android, "ANDROID_getNativeWindow");
    if (!_ANDROID_getNativeWindow) {
       fprintf(stderr, "Did not find symbol \"ANDROID_getNativeWindow\" %s\n", dlerror());
       return NULL;
    }
    return (*_ANDROID_getNativeWindow)();
#endif

    return cachedWindowType;
}



//void __attribute__ ((constructor)) wr_init(void) {
//    printf("LOADING IN INIT\n");
//    load_wrapped_gles_symbols();
//}

