/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
#include <com_sun_glass_ui_gtk_GtkView.h>
#include <com_sun_glass_events_ViewEvent.h>

#include <cstdlib>
#include <cstring>
#include <cassert>

#include "glass_general.h"
#include "glass_view.h"
#include "glass_window.h"

#define JLONG_TO_GLASSVIEW(value) ((GlassView *) JLONG_TO_PTR(value))

extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _enableInputMethodEvents
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkView_enableInputMethodEventsImpl
  (JNIEnv * env, jobject obj, jlong ptr, jboolean enable)
{
    (void)env;
    (void)obj;

    GlassView* view = JLONG_TO_GLASSVIEW(ptr);
    if (view->current_window) {
        if (enable) {
            view->current_window->enableOrResetIME();
        } else {
            view->current_window->disableIME();
        }
    }
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _create
 * Signature: (Ljava/util/Map;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_gtk_GtkView__1create
  (JNIEnv * env, jobject obj, jobject caps)
{
    (void)env;
    (void)obj;
    (void)caps;

    GlassView *view = new GlassView();
    return PTR_TO_JLONG(view);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _getNativeView
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_gtk_GtkView__1getNativeView
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;
    (void)ptr;

    return 0;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _getX
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkView__1getX
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    GlassView* view = JLONG_TO_GLASSVIEW(ptr);
    if (view && view->current_window) {
        return view->current_window->get_geometry().view_x;
    }
    return 0;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _getY
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkView__1getY
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    GlassView* view = JLONG_TO_GLASSVIEW(ptr);
    if (view && view->current_window) {
        return view->current_window->get_geometry().view_y;
    }
    return 0;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _setParent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkView__1setParent
  (JNIEnv * env, jobject obj, jlong ptr, jlong parent)
{
    GlassView* view = JLONG_TO_GLASSVIEW(ptr);
    bool is_removing = view->current_window && !parent;

    view->current_window = (WindowContext*)JLONG_TO_PTR(parent);

    if (is_removing) {
        env->CallVoidMethod(obj, jViewNotifyView, com_sun_glass_events_ViewEvent_REMOVE);
    } else {
        env->CallVoidMethod(obj, jViewNotifyView, com_sun_glass_events_ViewEvent_ADD);
    }
    CHECK_JNI_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _close
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkView__1close
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    delete JLONG_TO_GLASSVIEW(ptr);
    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _scheduleRepaint
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkView__1scheduleRepaint
  (JNIEnv * env, jobject obj, jlong ptr)
{
    // Seems to be unused
    (void)env;
    (void)obj;
    (void)ptr;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _uploadPixelsDirect
 * Signature: (JLjava/nio/Buffer;II)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkView__1uploadPixelsDirect
(JNIEnv *env, jobject jView, jlong ptr, jobject buffer, jint width, jint height)
{
    (void)jView;

    if (!ptr) return;
    if (!buffer) return;

    GlassView* view = JLONG_TO_GLASSVIEW(ptr);
    if (view->current_window) {
        void *data = env->GetDirectBufferAddress(buffer);

        view->current_window->paint(data, width, height);
    }
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _uploadPixelsIntArray
 * Signature:  (J[IIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkView__1uploadPixelsIntArray
  (JNIEnv * env, jobject obj, jlong ptr, jintArray array, jint offset, jint width, jint height)
{
    (void)obj;

    if (!ptr) return;
    if (!array) return;
    if (offset < 0) return;
    if (width <= 0 || height <= 0) return;

    if (width > ((INT_MAX - offset) / height))
    {
        return;
    }

    if ((width * height + offset) > env->GetArrayLength(array))
    {
        return;
    }

    GlassView* view = JLONG_TO_GLASSVIEW(ptr);
    if (view->current_window) {
        int *data = NULL;
        data = (int*)env->GetPrimitiveArrayCritical(array, 0);

        view->current_window->paint(data + offset, width, height);

        env->ReleasePrimitiveArrayCritical(array, data, JNI_ABORT);
    }
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _uploadPixelsByteArray
 * Signature:  (J[BIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkView__1uploadPixelsByteArray
  (JNIEnv * env, jobject obj, jlong ptr, jbyteArray array, jint offset, jint width, jint height)
{
    (void)obj;

    if (!ptr) return;
    if (!array) return;
    if (offset < 0) return;
    if (width <= 0 || height <= 0) return;

    if (width > (((INT_MAX - offset) / 4) / height))
    {
        return;
    }

    if ((4 * width * height + offset) > env->GetArrayLength(array))
    {
        return;
    }

    GlassView* view = JLONG_TO_GLASSVIEW(ptr);
    if (view->current_window) {
        unsigned char *data = NULL;

        data = (unsigned char*)env->GetPrimitiveArrayCritical(array, 0);

        view->current_window->paint(data + offset, width, height);

        env->ReleasePrimitiveArrayCritical(array, data, JNI_ABORT);
    }
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _enterFullscreen
 * Signature: (JZZZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkView__1enterFullscreen
  (JNIEnv * env, jobject obj, jlong ptr, jboolean animate, jboolean keepRation, jboolean hideCursor)
{
    (void)animate;
    (void)keepRation;
    (void)hideCursor;

    GlassView* view = JLONG_TO_GLASSVIEW(ptr);
    if (view->current_window) {
        view->current_window->enter_fullscreen();
        env->CallVoidMethod(obj, jViewNotifyView, com_sun_glass_events_ViewEvent_FULLSCREEN_ENTER);
        CHECK_JNI_EXCEPTION_RET(env, JNI_FALSE)
    }
    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkView
 * Method:    _exitFullscreen
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkView__1exitFullscreen
  (JNIEnv * env, jobject obj, jlong ptr, jboolean animate)
{
    (void)animate;

    GlassView* view = JLONG_TO_GLASSVIEW(ptr);
    if (view->current_window) {
        if (view->embedded_window) {
            view->embedded_window->exit_fullscreen();
        } else {
            view->current_window->exit_fullscreen();
        }
        env->CallVoidMethod(obj, jViewNotifyView, com_sun_glass_events_ViewEvent_FULLSCREEN_EXIT);
        CHECK_JNI_EXCEPTION(env)
    }

}

} // extern "C"
