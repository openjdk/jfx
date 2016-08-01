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

#include <jni.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#include "../PrismES2Defs.h"
#include "com_sun_prism_es2_X11GLPixelFormat.h"

extern void setGLXAttrs(jint *attrs, int *glxAttrs);
extern void printAndReleaseResources(Display *display, GLXFBConfig *fbConfigList,
        XVisualInfo *visualInfo, Window win, GLXContext ctx, Colormap cmap,
        const char *message);

/*
 * Class:     com_sun_prism_es2_X11GLPixelFormat
 * Method:    nCreatePixelFormat
 * Signature: (J[I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_X11GLPixelFormat_nCreatePixelFormat
(JNIEnv *env, jclass class, jlong nativeScreen, jintArray attrArr) {
    int glxAttrs[MAX_GLX_ATTRS_LENGTH]; /* value, attr pair plus a None */
    jint *attrs;
    PixelFormatInfo *pfInfo = NULL;

    GLXFBConfig *fbConfigList = NULL;
    XVisualInfo *visualInfo = NULL;
    int numFBConfigs;
    Display *display;
    int screen;
    Window root;
    Window win = None;
    XSetWindowAttributes win_attrs;
    Colormap cmap;
    unsigned long win_mask;

    if (attrArr == NULL) {
        return 0;
    }
    attrs = (*env)->GetIntArrayElements(env, attrArr, NULL);
    setGLXAttrs(attrs, glxAttrs);
    (*env)->ReleaseIntArrayElements(env, attrArr, attrs, JNI_ABORT);

    // RT-27386
    // TODO: Need to use nativeScreen to create this requested pixelformat
    // currently hack to work on a single monitor system
    display = XOpenDisplay(0);
    if (display == NULL) {
        fprintf(stderr, "Failed in XOpenDisplay\n");
        return 0;
    }

    screen = DefaultScreen(display);

    fbConfigList = glXChooseFBConfig(display, screen, glxAttrs, &numFBConfigs);

    if (fbConfigList == NULL) {
        fprintf(stderr, "Failed in glXChooseFBConfig\n");
        return 0;
    }

#if 0 // TESTING ONLY
    visualInfo = glXGetVisualFromFBConfig(display, fbConfigList[0]);
    if (visualInfo == NULL) {
        printAndReleaseResources(display, fbConfigList, NULL,
                None, NULL, None,
                "Failed in  glXGetVisualFromFBConfig");
        return 0;
    }

    fprintf(stderr, "found a %d-bit visual (visual ID = 0x%x)\n",
            visualInfo->depth, (unsigned int) visualInfo->visualid);
#endif

    visualInfo = glXGetVisualFromFBConfig(display, fbConfigList[0]);
    if (visualInfo == NULL) {
        printAndReleaseResources(display, fbConfigList, NULL,
                None, NULL, None,
                "Failed in glXGetVisualFromFBConfig");
        return 0;
    }

#if 0 // TESTING ONLY
    fprintf(stderr, "found a %d-bit visual (visual ID = 0x%x)\n",
            visualInfo->depth, (unsigned int) visualInfo->visualid);
#endif

    root = RootWindow(display, visualInfo->screen);

    /* Create a colormap */
    cmap = XCreateColormap(display, root, visualInfo->visual, AllocNone);

    /* Create a 1x1 window */
    win_attrs.colormap = cmap;
    win_attrs.border_pixel = 0;
    win_attrs.event_mask = KeyPressMask | ExposureMask | StructureNotifyMask;
    win_mask = CWColormap | CWBorderPixel | CWEventMask;
    win = XCreateWindow(display, root, 0, 0, 1, 1, 0,
            visualInfo->depth, InputOutput, visualInfo->visual, win_mask, &win_attrs);

    if (win == None) {
        printAndReleaseResources(display, fbConfigList, visualInfo,
                win, NULL, cmap,
                "Failed in XCreateWindow");
        return 0;
    }

    /* allocate the structure */
    pfInfo = (PixelFormatInfo *) malloc(sizeof (PixelFormatInfo));
    if (pfInfo == NULL) {
        fprintf(stderr, "nCreatePixelFormat: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializePixelFormatInfo(pfInfo);
    pfInfo->display = display;
    pfInfo->fbConfig = fbConfigList[0];
    pfInfo->dummyWin = win;
    pfInfo->dummyCmap = cmap;

    XFree(visualInfo);
    XFree(fbConfigList);

    return ptr_to_jlong(pfInfo);
}
