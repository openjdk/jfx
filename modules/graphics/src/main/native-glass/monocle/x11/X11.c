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

#include "com_sun_glass_ui_monocle_X.h"
#include <X11/Xlib.h>
#include <X11/Xlibint.h>
#include "Monocle.h"

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XInitThreads
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass)) {
    XInitThreads();
 }

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XLockDisplay
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display) {
    XLockDisplay((Display *) asPtr(display));
 }

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XUnlockDisplay
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display) {
    XUnlockDisplay((Display *) asPtr(display));
 }

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_X_XOpenDisplay
 (JNIEnv *env, jclass UNUSED(xClass), jstring displayName) {
    const char *s = NULL;
    if (displayName) {
        s = (*env)->GetStringUTFChars(env, displayName, NULL);
    }
    Display *display = XOpenDisplay(s);
    if (displayName) {
        (*env)->ReleaseStringUTFChars(env, displayName, s);
    }
    return asJLong(display);
}

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_X_DefaultScreenOfDisplay
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display) {
    Screen *screen = DefaultScreenOfDisplay((Display *) asPtr(display));
    return asJLong(screen);
}

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_X_RootWindowOfScreen
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong screen) {
    return asJLong(RootWindowOfScreen((Screen *) asPtr(screen)));
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_X_WidthOfScreen
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong screen) {
    return (jint) WidthOfScreen((Screen *) asPtr(screen));
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_X_HeightOfScreen
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong screen) {
    return (jint) HeightOfScreen((Screen *) asPtr(screen));
}

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_X_XCreateWindow
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong parent,
        jint x, jint y, jint width, jint height,
        jint borderWidth, jint depth, jint windowClass,
        jlong visual, jlong valueMask, jlong attributes) {
    return asJLong(XCreateWindow(
            (Display *) asPtr(display), (Window) parent,
            (int) x, (int) y, (int) width, (int) height,
            (int) borderWidth, (int) depth, (int) windowClass,
            (Visual *) asPtr(visual), (long) valueMask,
            (XSetWindowAttributes *) asPtr(attributes)));
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XMapWindow
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display, jlong window) {
    XMapWindow((Display *) asPtr(display), (Window) window);
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XSetWindowAttributes_sizeof
 (JNIEnv *UNUSED(env), jobject UNUSED(obj)) {
    return (jint) sizeof(XSetWindowAttributes);
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XSetWindowAttributes_setEventMask
 (JNIEnv *UNUSED(env), jclass UNUSED(attrClass), jlong attrsL, jlong mask) {
    XSetWindowAttributes *attrs = (XSetWindowAttributes *) asPtr(attrsL);
    attrs->event_mask = (unsigned long) mask;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XSetWindowAttributes_setCursor
 (JNIEnv *UNUSED(env), jclass UNUSED(attrClass), jlong attrsL, jlong cursor) {
    XSetWindowAttributes *attrs = (XSetWindowAttributes *) asPtr(attrsL);
    attrs->cursor = (Cursor) cursor;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XSetWindowAttributes_setOverrideRedirect
 (JNIEnv *UNUSED(env), jclass UNUSED(attrClass), jlong attrsL, jboolean override) {
    XSetWindowAttributes *attrs = (XSetWindowAttributes *) asPtr(attrsL);
    attrs->override_redirect = override ? True : False;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XStoreName
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display, jlong window,
        jstring nameString) {
    const char *name = NULL;
    if (nameString) {
        name = (*env)->GetStringUTFChars(env, nameString, NULL);
    }
    XStoreName((Display *) asPtr(display), (Window) window, name);
    if (nameString) {
        (*env)->ReleaseStringUTFChars(env, nameString, name);
    }
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XSync
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display, jboolean flush) {
    XSync((Display *) asPtr(display), flush ? True : False);
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XGetGeometry
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong window,
        jlongArray rootBuffer,
        jintArray xBuffer, jintArray yBuffer,
        jintArray widthBuffer, jintArray heightBuffer,
        jintArray borderWidthBuffer, jintArray depthBuffer) {
    Window root;
    int x, y;
    unsigned int width, height, borderWidth, depth;
    XGetGeometry((Display *) asPtr(display), (Window) window,
            &root, &x, &y, &width, &height, &borderWidth, &depth);
    monocle_returnLong(env, rootBuffer, root);
    monocle_returnInt(env, xBuffer, x);
    monocle_returnInt(env, yBuffer, y);
    monocle_returnInt(env, widthBuffer, width);
    monocle_returnInt(env, heightBuffer, height);
    monocle_returnInt(env, borderWidthBuffer, borderWidth);
    monocle_returnInt(env, depthBuffer, depth);
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XEvent_sizeof
 (JNIEnv *UNUSED(env), jobject UNUSED(obj)) {
    return (jint) sizeof(XEvent);
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XNextEvent
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong eventL) {
    XNextEvent((Display *) asPtr(display),
               (XEvent *) asPtr(eventL));
}

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_X_XInternAtom
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display,
        jstring atomNameString, jboolean onlyIfExists) {
    const char *atomName = NULL;
    Atom atom;
    if (atomNameString) {
        atomName = (*env)->GetStringUTFChars(env, atomNameString, NULL);
    }
    atom = XInternAtom((Display *) asPtr(display),
                       atomName,
                       onlyIfExists ? True : False);
    if (atomNameString) {
        (*env)->ReleaseStringUTFChars(env, atomNameString, atomName);
    }
    return (jlong) atom;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XSendEvent
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong window, jboolean propagate,
        jlong mask, jlong eventL) {
    XSendEvent((Display *) asPtr(display),
               (Window) window,
               propagate ? True : False,
               (long) mask,
               (XEvent *) asPtr(eventL));
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XGrabKeyboard
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong window, jboolean ownerEvents,
        jlong pointerMode, jlong keyboardMode, jlong time) {
    XGrabKeyboard((Display *) asPtr(display),
                  (Window) window,
                  ownerEvents ? True : False,
                  (int) pointerMode,
                  (int) keyboardMode,
                  (Time) time);
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XWarpPointer
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong src_window, jlong dst_window,
        jint src_x, jint src_y, jint src_width, jint src_height,
        jint dst_x, jint dst_y) {
    XWarpPointer((Display *) asPtr(display), (Window) src_window,
                 (Window) dst_window, src_x, src_y, src_width, src_height,
                 dst_x, dst_y);
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XFlush
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display) {
      XFlush((Display *) asPtr(display));
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XQueryPointer
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong window, jintArray position) {
    Window root, child;
    int rootX, rootY, winX, winY;
    unsigned int mask;
    XQueryPointer((Display *) asPtr(display), (Window) window,
                  &root, &child,&rootX, &rootY, &winX, &winY, &mask);
    (*env)->SetIntArrayRegion(env, position, 0, 1, &winX);
    (*env)->SetIntArrayRegion(env, position, 1, 1, &winY);
}


JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_X_XCreateBitmapFromData
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong drawable, jobject buf,
        jint UNUSED(width), jint UNUSED(height)) {
    void *data = (*env)->GetDirectBufferAddress(env, buf);
    return asJLong(XCreateBitmapFromData((Display *) asPtr(display),
                                (Window) drawable, data, 1, 1));
}

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_X_XCreatePixmapCursor
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong source, jlong mask,
        jlong UNUSED(fg), jlong UNUSED(bg),
        jint UNUSED(x), jint UNUSED(y)) {
    XColor black;
    black.red = black.green = black.blue = 0;
    return asJLong(XCreatePixmapCursor ((Display *) asPtr(display),
                   source, mask, &black, &black, 0, 0));
}


JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XDefineCursor
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong window, jlong cursor) {
    XDefineCursor((Display *) asPtr(display), (Window) window, (Cursor) cursor);
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XUndefineCursor
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong window) {
    XUndefineCursor((Display *) asPtr(display), (Window) window);
}


JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_XFreePixmap
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display, jlong pixmap) {
    XFreePixmap((Display *) asPtr(display), pixmap);
 }


JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XEvent_getWindow
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL) {
    XEvent *event = (XEvent *) asPtr(eventL);
    return (jlong) event->xany.window;
}


JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XEvent_setWindow
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL, jlong window) {
    XEvent *event = (XEvent *) asPtr(eventL);
    event->xany.window = (Window) window;
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XEvent_getType
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL) {
    XEvent *event = (XEvent *) asPtr(eventL);
    return (jint) event->type;
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XButtonEvent_getButton
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL) {
    XButtonEvent *event = (XButtonEvent *) asPtr(eventL);
    return (jint) event->button;
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XMotionEvent_getX
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL) {
    XMotionEvent *event = (XMotionEvent *) asPtr(eventL);
    return (jint) event->x;
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XMotionEvent_getY
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL) {
    XMotionEvent *event = (XMotionEvent *) asPtr(eventL);
    return (jint) event->y;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XClientMessageEvent_setMessageType
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL, jlong messageType) {
    XClientMessageEvent *event = (XClientMessageEvent *) asPtr(eventL);
    event->message_type = (Atom) messageType;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XClientMessageEvent_setFormat
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL, jlong format) {
    XClientMessageEvent *event = (XClientMessageEvent *) asPtr(eventL);
    event->format = (int) format;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XClientMessageEvent_setDataLong
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL, jint index, jlong element) {
    XClientMessageEvent *event = (XClientMessageEvent *) asPtr(eventL);
    event->data.l[index] = (long) element;
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XDisplay_sizeof
 (JNIEnv *UNUSED(env), jclass UNUSED(clazz)) {
    return (jint) sizeof(struct _XDisplay);
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XColor_setRed
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong colorL, jint red) {
    XColor *color = (XColor *) asPtr(colorL);
    color->red = (unsigned short) red;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XColor_setGreen
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong colorL, jint green) {
    XColor *color = (XColor *) asPtr(colorL);
    color->green = (unsigned short) green;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XColor_setBlue
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong colorL, jint blue) {
    XColor *color = (XColor *) asPtr(colorL);
    color->blue = (unsigned short) blue;
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_X_00024XColor_sizeof
 (JNIEnv *UNUSED(env), jclass UNUSED(clazz)) {
    return (jint) sizeof(XColor);
}
