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
#include <unistd.h>
#include <string.h>
#include <strings.h>

#include "lensPort.h"

#include <EGL/egl.h>

#ifdef USE_DISPMAN
//Broadcom specials

#ifndef BCM_HOST_H
#include "bcm_host.h"
#endif

#define WRAPPEDAPI
#include "wrapped_bcm.h"
static void (*wr_bcm_host_init)(void);
#endif /* USE_DISPMAN */

//Vivante specials
static EGLNativeDisplayType (*wr_fbGetDisplayByIndex)(int DisplayIndex);
static EGLNativeWindowType (*wr_fbCreateWindow)(EGLNativeDisplayType Display, int X, int Y, int Width, int Height);
int useDispman = 0;
int useVivanteFB = 0;

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

EGLDisplay util_wr_eglGetDisplay(EGLNativeDisplayType display_id) {
    EGLDisplay ret = (*_eglGetDisplay)(display_id);
    return ret;
}

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


int load_bcm_symbols() {
#ifdef USE_DISPMAN
    static int bcm_loaded = -1;

    if (bcm_loaded != -1) {
        return bcm_loaded;
    }

    void *lib = dlopen("libbcm_host.so", RTLD_LAZY);
    if (!lib) {
        bcm_loaded = 1;
        return bcm_loaded;
    }

    int error = 0;

    if (!(wr_bcm_host_init = dlsym(lib,"bcm_host_init"))) error++;
    if (!(wr_vc_dispmanx_display_close = dlsym(lib,"vc_dispmanx_display_close"))) error++;
    if (!(wr_vc_dispmanx_display_open = dlsym(lib,"vc_dispmanx_display_open"))) error++;
    if (!(wr_vc_dispmanx_display_get_info = dlsym(lib, "vc_dispmanx_display_get_info")))  error++; //
    if (!(wr_vc_dispmanx_element_add = dlsym(lib,"vc_dispmanx_element_add"))) error++;
    if (!(wr_vc_dispmanx_update_start = dlsym(lib,"vc_dispmanx_update_start"))) error++;
    if (!(wr_vc_dispmanx_update_submit_sync = dlsym(lib,"vc_dispmanx_update_submit_sync"))) error++;
    if (!(wr_vc_dispmanx_resource_write_data = dlsym(lib, "vc_dispmanx_resource_write_data"))) error++;
    if (!(wr_vc_dispmanx_resource_read_data = dlsym(lib, "vc_dispmanx_resource_read_data"))) error++;
    if (!(wr_vc_dispmanx_element_remove = dlsym(lib, "vc_dispmanx_element_remove"))) error++;
    if (!(wr_vc_dispmanx_element_change_attributes = dlsym(lib, "vc_dispmanx_element_change_attributes"))) error++;
    if (!(wr_vc_dispmanx_resource_create = dlsym(lib, "vc_dispmanx_resource_create"))) error++;
    if (!(wr_vc_dispmanx_resource_delete = dlsym(lib, "vc_dispmanx_resource_delete"))) error++;
    if (!(wr_vc_dispmanx_snapshot = dlsym(lib, "vc_dispmanx_snapshot"))) error++;
    if (!(wr_vc_dispmanx_element_change_source = dlsym(lib, "vc_dispmanx_element_change_source"))) error++;

    if (error) {
        // handle error conditions better ?
        fprintf(stderr, "failed to load all bcm_host symbols %d\n", error);
        return 1;
    }

    useDispman = 1; 
    bcm_loaded = 0;

    return bcm_loaded;
#else
    return 1;
#endif /* USE_DISPMAN */
}

static int load_vivante_symbols(void *lib) {
    int error = 0;
    if (!(wr_fbGetDisplayByIndex = GET_SYMBOL(lib, "fbGetDisplayByIndex"))) {
        error++;
    }
    if (!(wr_fbCreateWindow = GET_SYMBOL(lib, "fbCreateWindow"))) {
        error++;
    }
    if (error != 0) {
        fprintf(stderr, "failed to load all Vivante symbols %d\n", error);
        return 1;
    }
    return error;
}
static int done_loading_symbols = 0;

/***************************** UTILITY ********************************/

void * util_getLibGLEShandle() {
    return libglesv2;
}

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

    if (load_bcm_symbols() == 0) {
        // useDispman
    } else if (access("/dev/mxc_vpu", F_OK) == 0) {
        useVivanteFB = 1;
        error += load_vivante_symbols(libegl);
    }

    error += load_egl_symbols(libegl);

    return error;
}

EGLNativeDisplayType util_getNativeDisplayType() {
    static EGLNativeDisplayType cachedNativeDisplayType;
    static int cached = 0;

    if (!done_loading_symbols) {
        load_wrapped_gles_symbols();
    }

    if (!cached) {
        if (useDispman) {
            cachedNativeDisplayType = EGL_DEFAULT_DISPLAY;
        } else if (useVivanteFB)  {
            cachedNativeDisplayType = wr_fbGetDisplayByIndex(0);
        } else {
            cachedNativeDisplayType = (EGLNativeDisplayType)NULL;
        }

        cached ++;
    }


    return cachedNativeDisplayType;
}

EGLNativeWindowType util_getNativeWindowType() {
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
            VC_RECT_T dst = { 0, 0, 0, 0 };
            VC_RECT_T src = { 0, 0, 0, 0 };

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

            VC_DISPMANX_ALPHA_T alpha;
            alpha.flags = DISPMANX_FLAGS_ALPHA_FROM_SOURCE;
            alpha.opacity = 0xff;
            alpha.mask = (DISPMANX_RESOURCE_HANDLE_T) 0;
            update = (*wr_vc_dispmanx_update_start)(0);
            element = (*wr_vc_dispmanx_element_add)(
                          update,
                          display,
                          1 /*layer*/,
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

            cachedWindowType = (NativeWindowType)dispmanWindow;
#endif /* USE_DISPMAN */
        } else if (useVivanteFB)  {
            cachedWindowType = (*wr_fbCreateWindow)(util_getNativeDisplayType(), 0, 0, 0, 0);
        } else {
            cachedWindowType = NULL;      // hence the EGL NULL
        }
        cached ++;
    }

    return cachedWindowType;
}

