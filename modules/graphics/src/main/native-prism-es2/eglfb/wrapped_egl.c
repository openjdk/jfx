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
#ifndef __USE_GNU // required for dladdr() & Dl_info
#define __USE_GNU
#endif
#include <dlfcn.h>
#include <sys/ioctl.h>

#include <string.h>
#include <strings.h>

#if !defined(ANDROID_NDK)
#include "lensPort.h"
#endif

#define WRAPPEDAPI
#include "wrapped_egl.h"

int load_wrapped_gles_symbols(void);
void *libglesv2;
void *libegl;
int done_loading_symbols = 0;

/***************************** UTILITY ********************************/

#ifdef ANDROID_NDK

static EGLDisplay(*_eglGetDisplay)(EGLNativeDisplayType display_id);
static EGLNativeWindowType(*_ANDROID_getNativeWindow)();

#else 

PrismNativePort prismPort;

#endif

int load_wrapped_gles_symbols() {
    if (done_loading_symbols)  {
        return 0;
    }
    done_loading_symbols = 1;

#ifdef ANDROID_NDK
    libegl = dlopen("libEGL.so", RTLD_LAZY | RTLD_GLOBAL);
    if (!libegl) {
        fprintf(stderr, "Did not find libEGL.so %s\n", dlerror());
        return 0;
    }

    _eglGetDisplay = dlsym(libegl, "eglGetDisplay");
    
    libglesv2 = dlopen("libGLESv2.so", RTLD_LAZY | RTLD_GLOBAL);
    if (!libglesv2) {
        fprintf(stderr, "Did not find libGLESv2.so %s\n", dlerror());
        return 0;
    }
#else
    
    Dl_info dlinfo;
    if (dladdr(&load_wrapped_gles_symbols, &dlinfo)) {

        size_t rslash = (size_t)rindex(dlinfo.dli_fname,'/');
        if (rslash) {
            char *b = (char *) alloca(strlen(dlinfo.dli_fname)+20);
            rslash = rslash + 1 - (size_t)dlinfo.dli_fname;
            strncpy(b, dlinfo.dli_fname,rslash);
            strcpy(b + rslash, LENSPORT_LIBRARY_NAME);

            jboolean (*prism_platform_init)(PrismNativePort*) =  0;

            void *dlhand = dlopen(b,RTLD_NOW); 
            if (dlhand) {
                prism_platform_init =  dlsym(dlhand, "prism_platform_initialize");
                if (!prism_platform_init) {
                    fprintf(stderr,"prism_platform_initialize missing in %s\n",LENSPORT_LIBRARY_NAME);
                    exit(-1);
                }
            } else {
                fprintf(stderr,"Prism FAILED TO OPEN %s\n",b);
                fprintf(stderr,"dlopen reports %s\n",dlerror());
                exit(-1);
            }
            prismPort.version = NATIVE_PRISM_PORT_VERSION;

            if (!(*prism_platform_init)(&prismPort)) {
                fprintf(stderr,"prism_platform_initialize failed\n");
                exit(-1);
            }
        }
    } else {
        printf("Did not get DLINFO\n");
        exit(-1);
    }

#endif
    return 1;
}

EGLNativeDisplayType getNativeDisplayType() {
    if (!done_loading_symbols) {
        load_wrapped_gles_symbols();
    }
#ifdef ANDROID_NDK
    return (EGLNativeDisplayType) NULL;
#else
    return (EGLNativeDisplayType) (*prismPort.getNativeDisplayType)();
#endif

}

EGLNativeWindowType getNativeWindowType() {
    if (!done_loading_symbols) {
        load_wrapped_gles_symbols();
    }

#ifdef ANDROID_NDK
    //don't cache for Android!
    printf("Using getAndroidNativeWindow() from glass.\n");
    void *libglass_android = dlopen("libglass_lens_android.so", RTLD_LAZY | RTLD_GLOBAL);
    if (!libglass_android) {
        fprintf(stderr, "Did not find libglass_lens_android.so %s\n", dlerror());
           return NULL;
    }
    _ANDROID_getNativeWindow = dlsym(libglass_android, "ANDROID_getNativeWindow");
    if (!_ANDROID_getNativeWindow) {
       fprintf(stderr, "Did not find symbol \"ANDROID_getNativeWindow\" %s\n", dlerror());
       return NULL;
    }
    return (*_ANDROID_getNativeWindow)();
#else
    return (EGLNativeWindowType) (*prismPort.getNativeWindowType)();
#endif
}

EGLDisplay wr_eglGetDisplay(EGLNativeDisplayType display_id) {
#ifdef ANDROID_NDK
    return  (*_eglGetDisplay)(display_id);
#else
    return (EGLDisplay) (*prismPort.wr_eglGetDisplay)((void*)display_id);
#endif
}

void * getLibGLEShandle() {
#ifdef ANDROID_NDK
    return libglesv2;
#else
    return (*prismPort.getLibGLEShandle)();
#endif
}

