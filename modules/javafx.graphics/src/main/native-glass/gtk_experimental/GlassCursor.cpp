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
#include <com_sun_glass_ui_gtk_GtkCursor.h>

#include <gdk/gdk.h>
#include <stdlib.h>
#include <jni.h>

#include "com_sun_glass_ui_Cursor.h"
#include "glass_general.h"

#ifndef GLASS_GTK3
static GdkCursor* find_best_cursor(const gchar* options, GdkCursorType type) {
    gchar **opts = g_strsplit(options, ",", -1);
    gint size = g_strv_length(opts);

    GdkCursor *cursor = NULL;

    for (int i = 0; i < size; i++) {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), opts[i]);

        if (cursor != NULL) {
            break;
        }
    }

    g_strfreev(opts);

    if (cursor != NULL) {
        return cursor;
    }

    return gdk_cursor_new_for_display(gdk_display_get_default(), type);
}

GdkCursor* get_native_cursor(int type)
{
    GdkCursor *cursor = NULL;
    switch (type) {
        case com_sun_glass_ui_Cursor_CURSOR_DEFAULT:
            cursor = find_best_cursor("default", GDK_LEFT_PTR);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_TEXT:
            cursor = find_best_cursor("text", GDK_XTERM);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_CROSSHAIR:
            cursor = find_best_cursor("cross,crosshair", GDK_CROSSHAIR);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_CLOSED_HAND:
            cursor = find_best_cursor("closedhand", GDK_HAND2);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_OPEN_HAND:
            cursor = find_best_cursor("openhand", GDK_HAND2);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_POINTING_HAND:
            cursor = gdk_cursor_new(GDK_HAND2);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_UP:
            cursor = find_best_cursor("n-resize,ns-resize,size_ver", GDK_TOP_SIDE);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_DOWN:
            cursor = find_best_cursor("s-resize,ns-resize,size_ver", GDK_BOTTOM_SIDE);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_UPDOWN:
            cursor = find_best_cursor("ns-resize,ew-resize,size_ver", GDK_SB_V_DOUBLE_ARROW);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_LEFT:
            cursor = find_best_cursor("w-resize,ew-resize,size_hor", GDK_LEFT_SIDE);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_RIGHT:
            cursor = find_best_cursor("e-resize,ew-resize,size_hor", GDK_RIGHT_SIDE);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_LEFTRIGHT:
            cursor = find_best_cursor("ew-resize,size_hor", GDK_SB_H_DOUBLE_ARROW);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_SOUTHWEST:
            cursor = find_best_cursor("sw-resize,nesw-resize,size_bdiag", GDK_BOTTOM_LEFT_CORNER);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_NORTHEAST:
            cursor = find_best_cursor("ne-resize,nesw-resize,size_bdiag", GDK_TOP_RIGHT_CORNER);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_SOUTHEAST:
            cursor = find_best_cursor("se-resize,nwse-resize,size_fdiag", GDK_BOTTOM_RIGHT_CORNER);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_NORTHWEST:
            cursor = find_best_cursor("nw-resize,nwse-resize,size_fdiag", GDK_TOP_LEFT_CORNER);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_MOVE:
            cursor = find_best_cursor("fleur,move,alt-scroll", GDK_SIZING);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_WAIT:
            cursor = find_best_cursor("wait", GDK_WATCH);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_DISAPPEAR:
        case com_sun_glass_ui_Cursor_CURSOR_NONE:
            cursor = find_best_cursor("none", GDK_BLANK_CURSOR);
            break;
        default:
            cursor = find_best_cursor("default", GDK_LEFT_PTR);
            break;
    }

    if (cursor == NULL) {
        cursor = find_best_cursor("default", GDK_LEFT_PTR);
    }

    return cursor;
}
#else
GdkCursor* get_native_cursor(int type)
{
    gchar* cursor_name = NULL;

    switch (type) {
        case com_sun_glass_ui_Cursor_CURSOR_DEFAULT:
            cursor_name = g_strdup("default");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_TEXT:
            cursor_name = g_strdup("text");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_CROSSHAIR:
            cursor_name = g_strdup("crosshair");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_CLOSED_HAND:
            cursor_name = g_strdup("grabbing");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_OPEN_HAND:
            cursor_name = g_strdup("grab");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_POINTING_HAND:
            cursor_name = g_strdup("pointer");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_UP:
            cursor_name = g_strdup("n-resize");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_DOWN:
            cursor_name = g_strdup("s-resize");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_UPDOWN:
            cursor_name = g_strdup("ns-resize");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_LEFT:
            cursor_name = g_strdup("w-resize");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_RIGHT:
            cursor_name = g_strdup("e-resize");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_LEFTRIGHT:
            cursor_name = g_strdup("ew-resize");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_SOUTHWEST:
            cursor_name = g_strdup("sw-resize");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_NORTHEAST:
            cursor_name = g_strdup("ne-resize");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_SOUTHEAST:
            cursor_name = g_strdup("se-resize");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_NORTHWEST:
            cursor_name = g_strdup("nw-resize");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_MOVE:
            cursor_name = g_strdup("move");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_WAIT:
            cursor_name = g_strdup("wait");
            break;
        case com_sun_glass_ui_Cursor_CURSOR_DISAPPEAR:
        case com_sun_glass_ui_Cursor_CURSOR_NONE:
            cursor_name = g_strdup("none");
            break;
        default:
            cursor_name = g_strdup("default");
            break;
    }

    GdkCursor* cursor = gdk_cursor_new_from_name(gdk_display_get_default(), cursor_name);

    if (cursor == NULL) {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "default");
    }

    g_free(cursor_name);

    return cursor;
}
#endif

extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkCursor
 * Method:    _createCursor
 * Signature: (IILcom/sun/glass/ui/Pixels;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_gtk_GtkCursor__1createCursor
  (JNIEnv * env, jobject obj, jint x, jint y, jobject pixels)
{
    (void)obj;

    GdkPixbuf *pixbuf = NULL;
    GdkCursor *cursor = NULL;
    env->CallVoidMethod(pixels, jPixelsAttachData, PTR_TO_JLONG(&pixbuf));
    if (!EXCEPTION_OCCURED(env)) {
        cursor = gdk_cursor_new_from_pixbuf(gdk_display_get_default(), pixbuf, x, y);
    }
    g_object_unref(pixbuf);

    return PTR_TO_JLONG(cursor);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkCursor
 * Method:    _getBestSize
 * Signature: (II)Lcom.sun.glass.ui.Size
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_gtk_GtkCursor__1getBestSize
        (JNIEnv *env, jclass jCursorClass, jint width, jint height)
{
    (void)jCursorClass;
    (void)width;
    (void)height;

    int size = gdk_display_get_default_cursor_size(gdk_display_get_default());

    jclass jc = env->FindClass("com/sun/glass/ui/Size");
    if (env->ExceptionCheck()) return NULL;
    jobject jo =  env->NewObject(
            jc,
            jSizeInit,
            size,
            size);
    EXCEPTION_OCCURED(env);
    return jo;
}

} // extern "C"
