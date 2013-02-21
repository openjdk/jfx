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
#include "com_sun_glass_ui_lens_LensView.h"
#include "wm/LensWindowManager.h"

LensResult glass_view_releaseNativeView(JNIEnv *env, NativeView view) {

    GLASS_LOG_FINE("releaseNativeView on view %p", view);

    glass_view_PlatformViewRelease(env, view);

    GLASS_LOG_FINE("Releasing LensView global reference for view (%p)", view);
    (*env)->DeleteGlobalRef(env, view->lensView);

    GLASS_LOG_FINE("freeing view (%p)", view);
    free(view);

    return LENS_OK;
}

void glass_view_fitSurfaceToScreen(NativeScreen screen,
                                   NativeView view) {

    /**
    * Calculating the new diminsions is done by comparing the
    * vertical and the horizontal ratios, between the screen and
    * the view, and using the smaller ratio of the two to
    * calculate the surface size(the window must occupy the whole
    * screen).
    * 0.5 is added to the calculation for rounding conversion (so
    * 3.7 will become 4 when converted to int)
    *
    * Example:
    * Screen is 1200 X 700, view is 356 X 321
    * 1. width ratio = 1200/356 = 3.370
    * 2. height ratio = 700/321 = 2.180
    * 3. 2.180 is smaller, so we use it
    * 4. new width = (int)356*2.180+0.5 = 776
    * 5. new height = (int) 321*2.180+0.5 = 700
    * 6. new ratio = 776/700 = 1.1 - old ratio = 356/321 = 1.1
    *
    */

    float wRatio = screen->width / view->bounds.width;
    float hRatio = screen->height / view->bounds.height;
    float ratio  = (wRatio < hRatio) ? wRatio : hRatio;

    GLASS_LOG_FINE("Got screen->width=%d, screen->height=%d "
                   "view->width=%d, ciew->height=%d",
                   screen->width, screen->height,
                   view->bounds.width, view->bounds.height);

    view->bounds.width  = (int)(screen->width * ratio + 0.5);
    view->bounds.height = (int)(screen->height * ratio + 0.5);

    //center the surface on the screen
    view->bounds.x = (screen->width - view->bounds.width) / 2;
    view->bounds.y = (screen->height - view->bounds.height) / 2;

    GLASS_LOG_FINE("New bounds are width=%d, height=%d, x=%, y=%d, "
                   "used ratio is %f",
                   view->bounds.width, view->bounds.height,
                   view->bounds.x, view->bounds.y, ratio);

}


/*
 * Class:     com_sun_glass_ui_lens_LensView
 * Method:    _createNativeView
 * Signature: (Ljava/util/Map;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_lens_LensView__1createNativeView
(JNIEnv *env, jobject lensView, jobject MapObject) {

    NativeView view = (NativeView)calloc(1, sizeof(struct _NativeView));
    if (view != NULL) {
        view->lensView = (*env)->NewGlobalRef(env, lensView);

        GLASS_LOG_FINE("Created NativeView view = %p lensView=%p",
                       view, view->lensView);

        view->parent = NULL;

        if (glass_view_PlatformViewData_create(view)) {
            GLASS_LOG_SEVERE("Failed to init platform view");
            glass_view_releaseNativeView(env , view);
            view = NULL;
        }
    } else {
        GLASS_LOG_SEVERE("Failed to allocate NativeView");
        return 0;
    }
    return ptr_to_jlong(view);
}

/*
 * Class:     com_sun_glass_ui_lens_LensView
 * Method:    _begin
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensView__1begin
(JNIEnv *env, jobject ViewObject, jlong nativeViewPtr) {

    NativeView view = (NativeView) jlong_to_ptr(nativeViewPtr);
    if (view == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "View handle is null");
    }

    glass_view_drawBegin(view);
}

/*
 * Class:     com_sun_glass_ui_lens_LensView
 * Method:    _end
 * Signature: (JZZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensView__1end
(JNIEnv *env , jobject ViewObject, jlong nativeViewPtr) {

    NativeView view = (NativeView) jlong_to_ptr(nativeViewPtr);
    if (view == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "View handle is null");
    }

    glass_view_drawEnd(view);
}


/*
 * Class:     com_sun_glass_ui_lens_LensView
 * Method:    _paintIntBuffer
 * Signature: (JIILjava/nio/IntBuffer;[II)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensView__1paintInt(JNIEnv *env,
        jobject ViewObject,
        jlong nativeViewPtr,
        jint width,
        jint height,
        jobject intBuffer,
        jintArray srcArray,
        jint offset) {

    NativeView view = (NativeView)jlong_to_ptr(nativeViewPtr);
    NativeWindow window = view->parent;
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null paintint");
    }

    jint *src = (*env)->GetPrimitiveArrayCritical(env, srcArray, 0);

    glass_pixel_attachIntBuffer(env,
                                src,
                                window,
                                width, height,  offset);
    lens_wm_notifyWindowUpdate(window, width, height);

    (*env)->ReleasePrimitiveArrayCritical(env, srcArray, src, JNI_ABORT);
}


/*
 * Class:     com_sun_glass_ui_lens_LensView
 * Method:    _paintByte
 * Signature: (JIILjava/nio/ByteBuffer;[BI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensView__1paintByte
(JNIEnv *env, jobject ViewObject, jlong nativeViewPtr, jint width,
 jint height, jobject bytes, jbyteArray array, jint offset) {
    glass_throw_exception_by_name(env, glass_RuntimeException, "Unimplemented");
}

/*
 * Class:     com_sun_glass_ui_lens_LensView
 * Method:    _paintIntDirect
 * Signature: (JIILjava/nio/Buffer;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensView__1paintIntDirect
(JNIEnv *env, jobject ViewObject, jlong nativeViewPtr, jint width,
 jint height, jobject intBuffer) {

    NativeView view = (NativeView)jlong_to_ptr(nativeViewPtr);
    NativeWindow window = view->parent;
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    }

    jint *src = (*env)->GetDirectBufferAddress(env, intBuffer);
    int offset = 0;

    if (src) {
        glass_pixel_attachIntBuffer(env,
                                    src,
                                    window,
                                    width, height,  offset);
        lens_wm_notifyWindowUpdate(window, width, height);
    }
}

/*
 * Class:     com_sun_glass_ui_lens_LensView
 * Method:    _close
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensView__1close
(JNIEnv *env , jobject ViewObject, jlong nativeViewPtr) {

    //It seems that there is no need for view.close() notification
    NativeView view = (NativeView) jlong_to_ptr(nativeViewPtr);
    if (view == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "View handle is null");
    }

    int result;

    GLASS_LOG_FINE("close view %p", view);
    result = glass_view_releaseNativeView(env , view);
    if (result != LENS_OK) {
        GLASS_LOG_SEVERE("Failed to close a native view");
    }

    return (result ? JNI_FALSE : JNI_TRUE);
}

/*
 * Class:     com_sun_glass_ui_lens_LensView
 * Method:    _enterFullscreen
 * Signature: (JZZZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensView__1enterFullscreen
(JNIEnv *env, jobject ViewObject, jlong nativeViewPtr, jboolean animate,
 jboolean keepRatio, jboolean hideCursor) {

    NativeView view = (NativeView) jlong_to_ptr(nativeViewPtr);
    if (view == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "View handle is null");
    }

    GLASS_LOG_FINE(
        "enter fullscreen for view %p, animate=%s, keepRatio=%s, hideCursor=%s",
        view,
        animate ? "true" : "false",
        keepRatio ? "true" : "false",
        hideCursor ? "true" : "false");
    return glass_view_enterFullscreen(env, view, animate, keepRatio,
                                      hideCursor);
}

/*
 * Class:     com_sun_glass_ui_lens_LensView
 * Method:    _exitFullscreen
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensView__1exitFullscreen
(JNIEnv *env, jobject ViewObject, jlong nativeViewPtr, jboolean animate) {

    NativeView view = (NativeView) jlong_to_ptr(nativeViewPtr);
    if (view == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "View handle is null");
    }

    GLASS_LOG_FINE(
        "exit fullscreen for view %p", view);
    (void) glass_view_exitFullscreen(env, view, animate);
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensView__1setParent
(JNIEnv *env, jobject _this, jlong nativeViewPtr, jlong nativeWindowPtr) {


    NativeView view = (NativeView) jlong_to_ptr(nativeViewPtr);
    if (view == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "View handle is null");
    }

    NativeWindow parent = (NativeWindow) jlong_to_ptr(nativeWindowPtr);
    // window can be NULL if being removed

    GLASS_LOG_FINE("set parent of view %p to window %p old window %p", view, parent, view->parent);
    glass_view_setParent(env, parent, view);
}
