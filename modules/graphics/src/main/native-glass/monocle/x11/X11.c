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

#include "com_sun_glass_ui_monocle_x11_X.h"
#include <X11/Xlib.h>
#include "Monocle.h"

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_XOpenDisplay
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
 Java_com_sun_glass_ui_monocle_x11_X_DefaultScreenOfDisplay
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display) {
    Screen *screen = DefaultScreenOfDisplay((Display *) asPtr(display));
    return asJLong(screen);
}

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_RootWindowOfScreen
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong screen) {
    return asJLong(RootWindowOfScreen((Screen *) asPtr(screen)));
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_WidthOfScreen
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong screen) {
    return (jint) WidthOfScreen((Screen *) asPtr(screen));
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_HeightOfScreen
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong screen) {
    return (jint) HeightOfScreen((Screen *) asPtr(screen));
}

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_XCreateWindow
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
 Java_com_sun_glass_ui_monocle_x11_X_XMapWindow
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display, jlong window) {
    XMapWindow((Display *) asPtr(display), (Window) window);
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XSetWindowAttributes_sizeof
 (JNIEnv *UNUSED(env), jobject UNUSED(obj)) {
    return (jint) sizeof(XSetWindowAttributes);
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XSetWindowAttributes_setEventMask
 (JNIEnv *UNUSED(env), jclass UNUSED(attrClass), jlong attrsL, jlong mask) {
    XSetWindowAttributes *attrs = (XSetWindowAttributes *) asPtr(attrsL);
    attrs->event_mask = (unsigned long) mask;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XSetWindowAttributes_setCursor
 (JNIEnv *UNUSED(env), jclass UNUSED(attrClass), jlong attrsL, jlong cursor) {
    XSetWindowAttributes *attrs = (XSetWindowAttributes *) asPtr(attrsL);
    attrs->cursor = (Cursor) cursor;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XSetWindowAttributes_setOverrideRedirect
 (JNIEnv *UNUSED(env), jclass UNUSED(attrClass), jlong attrsL, jboolean override) {
    XSetWindowAttributes *attrs = (XSetWindowAttributes *) asPtr(attrsL);
    attrs->override_redirect = override ? True : False;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_XStoreName
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
 Java_com_sun_glass_ui_monocle_x11_X_XSync
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass), jlong display, jboolean flush) {
    XSync((Display *) asPtr(display), flush ? True : False);
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_XGetGeometry
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
 Java_com_sun_glass_ui_monocle_x11_X_00024XEvent_sizeof
 (JNIEnv *UNUSED(env), jobject UNUSED(obj)) {
    return (jint) sizeof(XEvent);
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_XNextEvent
 (JNIEnv *UNUSED(env), jclass UNUSED(xClass),
        jlong display, jlong eventL) {
    XNextEvent((Display *) asPtr(display),
               (XEvent *) asPtr(eventL));
}

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_XInternAtom
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
 Java_com_sun_glass_ui_monocle_x11_X_XSendEvent
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
 Java_com_sun_glass_ui_monocle_x11_X_XGrabKeyboard
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

JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XEvent_getWindow
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL) {
    XEvent *event = (XEvent *) asPtr(eventL);
    return (jlong) event->xany.window;
}


JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XEvent_setWindow
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL, jlong window) {
    XEvent *event = (XEvent *) asPtr(eventL);
    event->xany.window = (Window) window;
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XEvent_getType
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL) {
    XEvent *event = (XEvent *) asPtr(eventL);
    return (jint) event->type;
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XButtonEvent_getButton
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL) {
    XButtonEvent *event = (XButtonEvent *) asPtr(eventL);
    return (jint) event->button;
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XMotionEvent_getX
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL) {
    XMotionEvent *event = (XMotionEvent *) asPtr(eventL);
    return (jint) event->x;
}

JNIEXPORT jint JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XMotionEvent_getY
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL) {
    XMotionEvent *event = (XMotionEvent *) asPtr(eventL);
    return (jint) event->y;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XClientMessageEvent_setMessageType
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL, jlong messageType) {
    XClientMessageEvent *event = (XClientMessageEvent *) asPtr(eventL);
    event->message_type = (Atom) messageType;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XClientMessageEvent_setFormat
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL, jlong format) {
    XClientMessageEvent *event = (XClientMessageEvent *) asPtr(eventL);
    event->format = (int) format;
}

JNIEXPORT void JNICALL
 Java_com_sun_glass_ui_monocle_x11_X_00024XClientMessageEvent_setDataLong
 (JNIEnv *UNUSED(env), jclass UNUSED(eClass), jlong eventL, jint index, jlong element) {
    XClientMessageEvent *event = (XClientMessageEvent *) asPtr(eventL);
    event->data.l[index] = (long) element;
}
