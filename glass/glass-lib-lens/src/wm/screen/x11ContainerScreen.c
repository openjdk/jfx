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
 
#include "LensCommon.h"
#include "wm/LensWindowManager.h"
#include <X11/Xlib.h>

static int windowIndex = 1;

typedef struct _X11ContainerInfo {
    Display *display;
    Window window;
} X11ContainerInfo;

static X11ContainerInfo containerInfo;

static struct _NativeScreen x11Screen;

jboolean glass_application_initialize(JNIEnv *env) {
    //nothing to do
    return JNI_TRUE;
}

NativeScreen lens_screen_initialize(JNIEnv *env) {

    Display *x11ContainerDisplay;
    Window x11ContainerWindow;

    x11ContainerDisplay = XOpenDisplay(0);
    GLASS_LOG_FINE("XOpenDisplay(0) returned %p", x11ContainerDisplay);
    if (!x11ContainerDisplay) {
        GLASS_LOG_SEVERE("Cannot open X display :0");
        return NULL;
    }
    Screen *screen = DefaultScreenOfDisplay(x11ContainerDisplay);
    XSetWindowAttributes attrs;
    attrs.event_mask = ButtonPressMask | ButtonReleaseMask | PointerMotionMask;
    attrs.cursor = None;
    x11ContainerWindow = XCreateWindow(x11ContainerDisplay,
                                       RootWindowOfScreen(screen),
                                       0, 0,
                                       WidthOfScreen(screen), HeightOfScreen(screen),
                                       0, /* border width */
                                       CopyFromParent, /* depth */
                                       InputOutput, /* class */
                                       CopyFromParent, /* visual */
                                       CWEventMask | CWCursor,
                                       &attrs);
    GLASS_LOG_FINE("XCreateWindow(..) returned %p", x11ContainerDisplay);
    if (!x11ContainerWindow) {
        GLASS_LOG_SEVERE("Cannot create an X window");
        GLASS_LOG_FINE("XCloseDisplay(%p)", x11ContainerDisplay);
        XCloseDisplay(x11ContainerDisplay);
        return NULL;
    }
    GLASS_LOG_FINE("XMapWindow(window=%p)", x11ContainerWindow);
    XMapWindow(x11ContainerDisplay, x11ContainerWindow);
    GLASS_LOG_FINE("XStoreName(window=%p)", x11ContainerWindow);
    XStoreName(x11ContainerDisplay, x11ContainerWindow,
               "JavaFX EGL/framebuffer container");
    GLASS_LOG_FINE("XSync");
    XSync(x11ContainerDisplay, False); // allow the window manager to resize us
    Window root;
    int x, y;
    unsigned int width, height, borderWidth, depth;
    XGetGeometry(x11ContainerDisplay, x11ContainerWindow,
                 &root, &x, &y, &width, &height, &borderWidth, &depth);
    GLASS_LOG_FINE("XGetGeometry(window=%p) returned %i,%i+%ix%i border width %i depth %i",
                   x11ContainerWindow, x, y, width, height, borderWidth, depth);
    x11Screen.width = width;
    x11Screen.height = height;
    x11Screen.visibleWidth = x11Screen.width;
    x11Screen.visibleHeight = x11Screen.height;
    x11Screen.x = 0;
    x11Screen.y = 0;
    x11Screen.depth = 32;
    x11Screen.resolutionX = 96;
    x11Screen.resolutionY = 96;

    containerInfo.display = x11ContainerDisplay;
    containerInfo.window = x11ContainerWindow;

    x11Screen.data = (void *)&containerInfo;

    return &x11Screen;


}

void *glass_window_getPlatformWindow(JNIEnv *env, NativeWindow window) {
    return (void *) containerInfo.window;
}

char *lens_screen_getFrameBuffer() {
    return NULL;
}

void glass_screen_clear() {
    // NOOP
}

void lens_platform_shutdown(JNIEnv *env) {
    GLASS_LOG_FINE("native shutdown");
}

jboolean glass_screen_capture(jint x,
                              jint y,
                              jint width,
                              jint height,
                              jint *pixels) {
    GLASS_LOG_SEVERE("Screen capture not implemented for X11 Container");
    return JNI_FALSE;
}

LensResult glass_window_PlatformWindowData_create(JNIEnv *env,
                                                  NativeWindow window) {

    window->id = windowIndex++;
    window->data = NULL; //no platfrom specific data

    return LENS_OK;
}

LensResult glass_view_PlatformViewData_create(NativeView view) {
    view->data = NULL;
    return LENS_OK;
}

LensResult glass_view_PlatformViewRelease(JNIEnv *env, NativeView view) {
    // No data to free
    return LENS_OK;
}

LensResult glass_window_PlatformWindowRelease(JNIEnv *env, NativeWindow window) {

    // No data to free
    return LENS_OK;
}


jboolean glass_window_setAlpha(JNIEnv *env, NativeWindow window, float alpha) {
    window->alpha = alpha;
    lens_wm_repaint(env, window);
    return JNI_TRUE;
}

void glass_pixel_attachIntBuffer(JNIEnv *env,
                                 jint *srcPixels,
                                 NativeWindow fbWindow,
                                 jint width,
                                 jint height, int offset) {
    GLASS_LOG_SEVERE("attachIntBuffer not implemented for X11 Container");
}

