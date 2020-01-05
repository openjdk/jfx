/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <com_sun_glass_ui_gtk_GtkWindow.h>
#include <com_sun_glass_events_WindowEvent.h>
#include <com_sun_glass_events_ViewEvent.h>

#include <cstdlib>
#include <cstring>
#include "glass_general.h"
#include "glass_evloop.h"
#include "glass_window.h"

#define JLONG_TO_WINDOW_CTX(ptr) ((WindowContext*)JLONG_TO_PTR(ptr))

static WindowFrameType glass_mask_to_window_frame_type(jint mask) {
    if (mask & com_sun_glass_ui_gtk_GtkWindow_TRANSPARENT) {
        return TRANSPARENT;
    }
    if (mask & com_sun_glass_ui_gtk_GtkWindow_TITLED) {
        return TITLED;
    }
    return UNTITLED;
}

static WindowType glass_mask_to_window_type(jint mask) {
    if (mask & com_sun_glass_ui_gtk_GtkWindow_POPUP) {
        return POPUP;
    }
    if (mask & com_sun_glass_ui_gtk_GtkWindow_UTILITY) {
        return UTILITY;
    }
    return NORMAL;
}

static GdkWMFunction glass_mask_to_wm_function(jint mask) {
    int func = GDK_FUNC_RESIZE | GDK_FUNC_MOVE;

    if (mask & com_sun_glass_ui_gtk_GtkWindow_CLOSABLE) {
        func |= GDK_FUNC_CLOSE;
    }
    if (mask & com_sun_glass_ui_gtk_GtkWindow_MAXIMIZABLE) {
        func |= GDK_FUNC_MAXIMIZE;
    }
    if (mask & com_sun_glass_ui_gtk_GtkWindow_MINIMIZABLE) {
        func |= GDK_FUNC_MINIMIZE;
    }

    return (GdkWMFunction) func;
}

extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _createWindow
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1createWindow
  (JNIEnv * env, jobject obj, jlong owner, jlong screen, jint mask)
{
    (void)env;

    WindowContext* parent = JLONG_TO_WINDOW_CTX(owner);

    WindowContext* ctx = new WindowContext(obj,
            parent,
            screen,
            glass_mask_to_window_frame_type(mask),
            glass_mask_to_window_type(mask),
            glass_mask_to_wm_function(mask)
            );

    return PTR_TO_JLONG(ctx);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _createChildWindow
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1createChildWindow
  (JNIEnv * env, jobject obj , jlong owner)
{
    (void)env;

    return 0;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _close
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1close
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    destroy_and_delete_ctx(ctx);
    return JNI_TRUE; // return value not used
}
/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setView
 * Signature: (JLcom/sun/glass/ui/View;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setView
  (JNIEnv * env, jobject obj, jlong ptr, jobject view)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    return (ctx->set_view(view)) ? JNI_TRUE : JNI_FALSE;
}
/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _showOrHideChildren
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1showOrHideChildren
  (JNIEnv *env, jobject obj, jlong ptr, jboolean show)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->show_or_hide_children(show);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    minimizeImpl
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow_minimizeImpl
  (JNIEnv * env, jobject obj, jlong ptr, jboolean minimize)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_minimized(minimize);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    maximizeImpl
 * Signature: (JZZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow_maximizeImpl
  (JNIEnv * env, jobject obj, jlong ptr, jboolean maximize, jboolean wasMaximized)
{
    (void)env;
    (void)obj;
    (void)wasMaximized;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_maximized(maximize);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    setBoundsImpl
 * Signature: (JIIZZIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow_setBoundsImpl
  (JNIEnv * env, jobject obj, jlong ptr, jint x, jint y, jboolean xSet, jboolean ySet, jint w, jint h, jint cw, jint ch)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_bounds(x, y, xSet, ySet, w, h, cw, ch);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    setVisibleImpl
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow_setVisibleImpl
    (JNIEnv * env, jobject obj, jlong ptr, jboolean visible)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_visible(visible);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setResizable
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setResizable
  (JNIEnv * env, jobject obj, jlong ptr, jboolean resizable)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_resizable(resizable);
    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _requestFocus
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1requestFocus
  (JNIEnv * env, jobject obj, jlong ptr, jint focus)
{
    (void)env;
    (void)obj;
    (void)focus;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->request_focus();
    return JNI_TRUE; //not used
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setFocusable
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setFocusable
  (JNIEnv * env, jobject obj, jlong ptr, jboolean focusable)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_focusable(focusable);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _grabFocus
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1grabFocus
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    return ctx->grab_focus();
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _ungrabFocus
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1ungrabFocus
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->ungrab_focus();
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setTitle
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setTitle
  (JNIEnv * env, jobject obj, jlong ptr, jstring title)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    const char* ctitle = mainEnv->GetStringUTFChars(title, NULL);
    ctx->set_title(ctitle);
    mainEnv->ReleaseStringUTFChars(title, ctitle);

    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setLevel
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setLevel
  (JNIEnv * env, jobject obj, jlong ptr, jint level)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_level(level);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setAlpha
 * Signature: (JF)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setAlpha
  (JNIEnv * env, jobject obj, jlong ptr, jfloat alpha)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_alpha(alpha);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setBackground
 * Signature: (JFFF)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setBackground
  (JNIEnv * env, jobject obj, jlong ptr, jfloat r, jfloat g, jfloat b)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_background(r, g, b);
    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setEnabled
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setEnabled
  (JNIEnv * env, jobject obj, jlong ptr, jboolean enabled)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_enabled(enabled);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setMinimumSize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setMinimumSize
  (JNIEnv * env, jobject obj, jlong ptr, jint w, jint h)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    if (w < 0 || h < 0) return JNI_FALSE;
    ctx->set_minimum_size(w, h);
    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setMaximumSize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setMaximumSize
  (JNIEnv * env, jobject obj, jlong ptr, jint w, jint h)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    if (w == 0 || h == 0) return JNI_FALSE;

    ctx->set_maximum_size(w, h);
    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setIcon
 * Signature: (JLcom/sun/glass/ui/Pixels;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setIcon
  (JNIEnv * env, jobject obj, jlong ptr, jobject pixels)
{
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    GdkPixbuf *pixbuf = NULL;
    if (pixels != NULL) {
        env->CallVoidMethod(pixels, jPixelsAttachData, PTR_TO_JLONG(&pixbuf));
    }
    if (!EXCEPTION_OCCURED(env)) {
        ctx->set_icon(pixbuf);
    }
    if (pixbuf != NULL) g_object_unref(pixbuf);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _toFront
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1toFront
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->restack(true);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _toBack
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1toBack
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->restack(false);

}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _enterModal
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1enterModal
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_modal(true);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _enterModalWithWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1enterModalWithWindow
  (JNIEnv * env, jobject obj, jlong ptrDialog, jlong ptrWindow)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptrDialog);
    WindowContext* parent_ctx = JLONG_TO_WINDOW_CTX(ptrWindow);
    ctx->set_modal(true, parent_ctx);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _exitModal
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1exitModal
  (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_modal(false);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkCursor
 * Method:    _setCursorType
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setCursorType
  (JNIEnv * env, jobject obj, jlong ptr, jint type)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    GdkCursor *cursor = get_native_cursor(type);
    ctx->set_cursor(cursor);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkCursor
 * Method:    _setCustomCursor
 * Signature: (JLcom/sun/glass/ui/Cursor;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setCustomCursor
  (JNIEnv * env, jobject obj, jlong ptr, jobject jCursor)
{
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    GdkCursor *cursor = (GdkCursor*)JLONG_TO_PTR(env->GetLongField(jCursor, jCursorPtr));

    ctx->set_cursor(cursor);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    isVisible
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkWindow_isVisible
    (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    return ctx->is_visible() ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1getNativeWindowImpl
    (JNIEnv * env, jobject obj, jlong ptr)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    return GDK_WINDOW_XID(ctx->get_gdk_window());
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _setGravity
 * Signature: (JFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1setGravity
    (JNIEnv * env, jobject obj, jlong ptr, jfloat xGravity, jfloat yGravity)
{
    (void)env;
    (void)obj;

    WindowContext* ctx = JLONG_TO_WINDOW_CTX(ptr);
    ctx->set_gravity(xGravity, yGravity);

}


/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _getEmbeddedX
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1getEmbeddedX
  (JNIEnv *env, jobject obj, jlong ptr) {
    (void)env;
    (void)obj;

    return 0;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    _getEmbeddedY
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkWindow__1getEmbeddedY
  (JNIEnv *env, jobject obj, jlong ptr) {
    (void)env;
    (void)obj;

    return 0;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkWindow
 * Method:    getFrameExtents
 * Signature: (J[I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkWindow_getFrameExtents
    (JNIEnv * env, jobject obj, jlong ptr, jintArray extarr)
{
    (void)obj;
}

} // extern "C"
