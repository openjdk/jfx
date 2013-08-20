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

// this define causes us to define our static vars in this module
#define FB_PLATFORM_DECLARE

#include "utilInternal.h"
#include "platformUtil.h"

//this is a hack until we can figure out how to share between glass/prism/port
int glass_log_level = 0;

#ifdef USE_DISPMAN
extern int load_bcm_symbols();
extern void select_dispman();

static jboolean try_dispman() {
    if (load_bcm_symbols()) {
        select_dispman();
        return JNI_TRUE;
    } 
    return JNI_FALSE;
}
#else
static jboolean try_dispman() { return JNI_FALSE; }
#endif // USE_DISPMAN

#ifdef OMAP3
extern void select_omap_cursor();

extern jboolean fbFBRobotScreen(jint x, jint y,
                                    jint width, jint height,
                                    jint *pixels); 

static jboolean try_omap3() { 
    select_omap_cursor();
    fbRobotScreenCapture = fbFBRobotScreen;
    return JNI_TRUE;
}
#else
static jboolean try_omap3() { return JNI_FALSE; }
#endif

#ifdef ANDROID_NDK
static jboolean try_android() { return JNI_TRUE; }
#else
static jboolean try_android() { return JNI_FALSE; }
#endif

#ifdef EGL_X11_FB_CONTAINER
static jboolean try_x11_container() { return JNI_TRUE; }
#else
static jboolean try_x11_container() { return JNI_FALSE; }
#endif

void platform_initialize() {
    if(try_dispman()) {
       return;
    }

    if(try_omap3()) {
        return;
    }

    if(try_android()) {
        return;
    }

    if(try_x11_container()) {
        return;
    }

    // Fatal Error
    fprintf(stderr,"Fatal error loading native porting layer in Lens\n");
    exit(-1);
}
 
