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

#include <jni.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <X11/Xutil.h>

#include "../PrismES2Defs.h"
#include "com_sun_prism_es2_X11GLFactory.h"

void setGLXAttrs(jint *attrs, int *glxAttrs) {
    int index = 0;

    /* Specify pbuffer as default */
    glxAttrs[index++] = GLX_DRAWABLE_TYPE;
    if (attrs[ONSCREEN] != 0) {
        glxAttrs[index++] = (GLX_PBUFFER_BIT | GLX_WINDOW_BIT);
    } else {
        glxAttrs[index++] = GLX_PBUFFER_BIT;
    }

    /* only interested in RGBA type */
    glxAttrs[index++] = GLX_RENDER_TYPE;
    glxAttrs[index++] = GLX_RGBA_BIT;

    /* only interested in FBConfig with associated X Visual type */
    glxAttrs[index++] = GLX_X_RENDERABLE;
    glxAttrs[index++] = True;

    glxAttrs[index++] = GLX_DOUBLEBUFFER;
    if (attrs[DOUBLEBUFFER] != 0) {
        glxAttrs[index++] = True;
    } else {
        glxAttrs[index++] = False;
    }

    glxAttrs[index++] = GLX_RED_SIZE;
    glxAttrs[index++] = attrs[RED_SIZE];
    glxAttrs[index++] = GLX_GREEN_SIZE;
    glxAttrs[index++] = attrs[GREEN_SIZE];
    glxAttrs[index++] = GLX_BLUE_SIZE;
    glxAttrs[index++] = attrs[BLUE_SIZE];
    glxAttrs[index++] = GLX_ALPHA_SIZE;
    glxAttrs[index++] = attrs[ALPHA_SIZE];

    glxAttrs[index++] = GLX_DEPTH_SIZE;
    glxAttrs[index++] = attrs[DEPTH_SIZE];

    glxAttrs[index] = None;
}

void printAndReleaseResources(Display *display, GLXFBConfig *fbConfigList,
        XVisualInfo *visualInfo, Window win, GLXContext ctx, Colormap cmap,
        const char *message) {
    if (message != NULL) {
        fprintf(stderr, "%s\n", message);
    }
    if (display == NULL) {
        return;
    }
    glXMakeCurrent(display, None, NULL);
    if (fbConfigList != NULL) {
        XFree(fbConfigList);
    }
    if (visualInfo != NULL) {
        XFree(visualInfo);
    }
    if (ctx != NULL) {
        glXDestroyContext(display, ctx);
    }
    if (win != None) {
        XDestroyWindow(display, win);
    }
    if (cmap != None) {
        XFreeColormap(display, cmap);
    }
}

jboolean queryGLX13(Display *display) {

    int major, minor;
    int errorBase, eventBase;

    if (!glXQueryExtension(display, &errorBase, &eventBase)) {
        fprintf(stderr, "ES2 Prism: Error - GLX extension is not supported\n");
        fprintf(stderr, "    GLX version 1.3 or higher is required\n");
        return JNI_FALSE;
    }

    /* Query the GLX version number */
    if (!glXQueryVersion(display, &major, &minor)) {
        fprintf(stderr, "ES2 Prism: Error - Unable to query GLX version\n");
        fprintf(stderr, "    GLX version 1.3 or higher is required\n");
        return JNI_FALSE;
    }

    /*
        fprintf(stderr, "Checking GLX version : %d.%d\n", major, minor);
     */

    /* Check for GLX 1.3 and higher */
    if (!(major == 1 && minor >= 3)) {
        fprintf(stderr, "ES2 Prism: Error - reported GLX version = %d.%d\n", major, minor);
        fprintf(stderr, "    GLX version 1.3 or higher is required\n");

        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Class:     com_sun_prism_es2_X11GLFactory
 * Method:    nInitialize
 * Signature: ([I[J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_X11GLFactory_nInitialize
(JNIEnv *env, jclass class, jintArray attrArr) {

    int glxAttrs[MAX_GLX_ATTRS_LENGTH]; /* value, attr pair plus a None */
    jint *attrs;
    ContextInfo *ctxInfo = NULL;

    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;
    const char *glxExtensions;

    GLXFBConfig *fbConfigList = NULL;
    GLXContext ctx = NULL;
    XVisualInfo *visualInfo = NULL;
    int numFBConfigs, index, visualID;
    Display *display = NULL;
    int screen;
    Window root;
    Window win = None;
    XSetWindowAttributes win_attrs;
    Colormap cmap = None;
    unsigned long win_mask;

    if (attrArr == NULL) {
        return 0;
    }
    attrs = (*env)->GetIntArrayElements(env, attrArr, NULL);
    setGLXAttrs(attrs, glxAttrs);
    (*env)->ReleaseIntArrayElements(env, attrArr, attrs, JNI_ABORT);

    display = XOpenDisplay(0);
    if (display == NULL) {
        return 0;
    }

    screen = DefaultScreen(display);
    
    if (!queryGLX13(display)) {
        return 0;
    }

    fbConfigList = glXChooseFBConfig(display, screen, glxAttrs, &numFBConfigs);

    if (fbConfigList == NULL) {
        fprintf(stderr, "Prism ES2 Error - nInitialize: glXChooseFBConfig failed\n");
        return 0;
    }

    visualInfo = glXGetVisualFromFBConfig(display, fbConfigList[0]);
    if (visualInfo == NULL) {
        printAndReleaseResources(display, fbConfigList, visualInfo,
                win, ctx, cmap,
                "Failed in  glXGetVisualFromFBConfig");
        return 0;
    }

/*
    fprintf(stderr, "found a %d-bit visual (visual ID = 0x%x)\n",
            visualInfo->depth, (unsigned int) visualInfo->visualid);
*/
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
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "Failed in XCreateWindow");
        return 0;
    }

    ctx = glXCreateNewContext(display, fbConfigList[0], GLX_RGBA_TYPE, NULL, True);

    if (ctx == NULL) {
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "Failed in glXCreateNewContext");
        return 0;
    }

    if (!glXMakeCurrent(display, win, ctx)) {
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "Failed in glXMakeCurrent");
        return 0;
    }

    /* Get the OpenGL version */
    glVersion = (char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "glVersion == null");
        return 0;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

/*

    fprintf(stderr, "GL_VERSION string = %s\n", glVersion);
    fprintf(stderr, "GL_VERSION (major.minor) = %d.%d\n",
            versionNumbers[0], versionNumbers[1]);
*/

    /*
     * Targeted Cards: Intel HD Graphics, Intel HD Graphics 2000/3000,
     * Radeon HD 2350, GeForce FX (with newer drivers), GeForce 7 series or higher
     *
     * Check for OpenGL 2.1 or later. 
     */
    if ((versionNumbers[0] < 2) || ((versionNumbers[0] == 2) && (versionNumbers[1] < 1))) {
        fprintf(stderr, "Prism-ES2 Error : GL_VERSION (major.minor) = %d.%d\n",
                versionNumbers[0], versionNumbers[1]);
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap, NULL);
        return 0;
    }

    /* Get the OpenGL vendor and renderer */
    glVendor = (char *) glGetString(GL_VENDOR);
    if (glVendor == NULL) {
        glVendor = "<UNKNOWN>";
    }
    glRenderer = (char *) glGetString(GL_RENDERER);
    if (glRenderer == NULL) {
        glRenderer = "<UNKNOWN>";
    }

    glExtensions = (char *) glGetString(GL_EXTENSIONS);
    if (glExtensions == NULL) {
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "Prism-ES2 Error : glExtensions == null");
        return 0;
    }

    // We use GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        printAndReleaseResources(display, fbConfigList, visualInfo,
                win, ctx, cmap, "GL profile isn't PS 3.0 capable");
        return 0;
    }

    glxExtensions = (const char *) glXGetClientString(display, GLX_EXTENSIONS);
    if (glxExtensions == NULL) {
        printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap,
                "glxExtensions == null");
        return 0;
    }

    /* Note: We are only storing the string information of a driver.
     Assuming a system with a single or homogeneous GPUs. For the case
     of heterogeneous GPUs system the string information will need to move to
     GLContext class. */
    /* allocate the structure */
    ctxInfo = (ContextInfo *) malloc(sizeof (ContextInfo));
    if (ctxInfo == NULL) {
        fprintf(stderr, "nInitialize: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializeCtxInfo(ctxInfo);
    ctxInfo->versionStr = strdup(glVersion);
    ctxInfo->vendorStr = strdup(glVendor);
    ctxInfo->rendererStr = strdup(glRenderer);
    ctxInfo->glExtensionStr = strdup(glExtensions);
    ctxInfo->glxExtensionStr = strdup(glxExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->gl2 = JNI_TRUE;

    /* Information required by GLass at startup */
    ctxInfo->display = display;
    ctxInfo->screen = screen;
    ctxInfo->visualID = (int) visualInfo->visualid;

    /* Releasing native resources */
    printAndReleaseResources(display, fbConfigList, visualInfo, win, ctx, cmap, NULL);

    return ptr_to_jlong(ctxInfo);
}

/*
 * Class:     com_sun_prism_es2_X11GLFactory
 * Method:    nGetAdapterOrdinal
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_X11GLFactory_nGetAdapterOrdinal
(JNIEnv *env, jclass class, jlong screen) {
    //TODO: Needs implementation to handle multi-monitors (RT-27437)
    return 0;
}

/*
 * Class:     com_sun_prism_es2_X11GLFactory
 * Method:    nGetAdapterCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_X11GLFactory_nGetAdapterCount
(JNIEnv *env, jclass class) {
    //TODO: Needs implementation to handle multi-monitors (RT-27437)
    return 1;
}

/*
 * Class:     com_sun_prism_es2_X11GLFactory
 * Method:    nGetDefaultScreen
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_X11GLFactory_nGetDefaultScreen
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }
    return (jint) ctxInfo->screen;
}

/*
 * Class:     com_sun_prism_es2_X11GLFactory
 * Method:    nGetDisplay
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_X11GLFactory_nGetDisplay
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }
    return (jlong) ptr_to_jlong(ctxInfo->display);
}

/*
 * Class:     com_sun_prism_es2_X11GLFactory
 * Method:    nGetVisualID
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_X11GLFactory_nGetVisualID
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }
    return (jlong) ctxInfo->visualID;
}

/*
 * Class:     com_sun_prism_es2_X11_X11GLFactory
 * Method:    nGetIsGL2
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_X11GLFactory_nGetIsGL2
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    return ((ContextInfo *)jlong_to_ptr(nativeCtxInfo))->gl2;
}
