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

#include "lensPort.h"
#include "lensPortInternal.h"

int platform_log_level = 0;

Platform_Logger *platform_logf = 0;

static void setPlatformLogging(Platform_Logger *logger, int level) {
    platform_logf = logger;
    platform_log_level = level;
}

#ifdef USE_DISPMAN
extern jboolean select_dispman_cursor(LensNativePort *lensPort);
#endif // USE_DISPMAN

#if defined(OMAP3) || defined(IMX6_PLATFORM)
extern jboolean fbFBRobotScreen(jint x, jint y,
                                    jint width, jint height,
                                    jint *pixels); 
#endif

#ifdef OMAP3
extern jboolean select_omap_cursor(LensNativePort *lensPort);
#endif

#ifdef IMX6_PLATFORM
extern jboolean check_imx6_cursor(LensNativePort *lensPort);
#endif

jboolean lens_platform_initialize(LensNativePort* lensPort) {

    // check if we are within the range of what we can accept
    if ((!lensPort) || lensPort->version != NATIVE_PRISM_PORT_VERSION) {
        // something is really wrong here !
        printf("lensPort VERSION FAILED\n");
        return JNI_FALSE;
    }

    // report the version we actually are
    lensPort->version = NATIVE_LENS_PORT_VERSION;
    lensPort->setLogger = &setPlatformLogging;    

#ifdef USE_DISPMAN
    if (select_dispman_cursor(lensPort)) {
        return JNI_TRUE;
    } 
#endif // USE_DISPMAN

#ifdef IMX6_PLATFORM
    if (check_imx6_cursor(lensPort)) {
        lensPort->robotScreenCapture = fbFBRobotScreen;
        return JNI_TRUE;
    }
#endif //IMX6_PLATFORM

#ifdef OMAP3
    { // this is our default, no real test
        select_omap_cursor(lensPort);
        lensPort->robotScreenCapture = fbFBRobotScreen;
        return JNI_TRUE;
    }
#endif //OMAP3

#ifdef ANDROID_NDK
    return JNI_TRUE;
#endif //Android
    
    // Fatal Error
    fprintf(stderr,"Fatal error loading native porting layer in Lens\n");
    exit(-1);

    return JNI_FALSE;
}

// fix these return values?
extern void* util_getNativeWindowType(void);
extern void* util_getNativeDisplayType(void);
extern void* util_wr_eglGetDisplay(void *);
extern void* util_getLibGLEShandle(void);

jboolean prism_platform_initialize(PrismNativePort* prismPort) {

    // check if we are within the range of what we can accept
    if ((!prismPort) || prismPort->version != NATIVE_PRISM_PORT_VERSION) {
        // something is really wrong here !
        fprintf(stderr,"failed (version?) in prism_platform_initialize\n");
        exit(-1);
    }

    prismPort->version = NATIVE_PRISM_PORT_VERSION;
    prismPort->getNativeWindowType = &util_getNativeWindowType;
    prismPort->getNativeDisplayType = &util_getNativeDisplayType;
    prismPort->wr_eglGetDisplay = &util_wr_eglGetDisplay;
    prismPort->getLibGLEShandle = &util_getLibGLEShandle;

    return JNI_TRUE;
}
 
