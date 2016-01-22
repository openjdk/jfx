/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

GdkCursor* get_native_cursor(int type)
{
    GdkCursor *cursor = NULL;
    switch (type) {
        case com_sun_glass_ui_Cursor_CURSOR_DEFAULT:
            cursor = gdk_cursor_new(GDK_LEFT_PTR);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_TEXT:
            cursor = gdk_cursor_new(GDK_XTERM);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_CROSSHAIR:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "cross");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "crosshair");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_CROSSHAIR);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_CLOSED_HAND:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "closedhand");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_HAND2);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_OPEN_HAND:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "openhand");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_HAND2);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_POINTING_HAND:
            cursor = gdk_cursor_new(GDK_HAND2);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_UP:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "n-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "ns-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "size_ver");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_TOP_SIDE);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_DOWN:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "s-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "ns-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "size_ver");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_BOTTOM_SIDE);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_UPDOWN:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "ns-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "size_ver");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_SB_V_DOUBLE_ARROW);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_LEFT:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "w-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "ew-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "size_hor");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_LEFT_SIDE);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_RIGHT:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "e-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "ew-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "size_hor");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_RIGHT_SIDE);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_LEFTRIGHT:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "ew-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "size_hor");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_SB_H_DOUBLE_ARROW);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_SOUTHWEST:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "sw-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "nesw-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "size_bdiag");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_BOTTOM_LEFT_CORNER);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_NORTHEAST:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "ne-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "nesw-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "size_bdiag");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_TOP_RIGHT_CORNER);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_SOUTHEAST:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "se-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "nwse-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "size_fdiag");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_BOTTOM_RIGHT_CORNER);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_NORTHWEST:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "nw-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "nwse-resize");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "size_fdiag");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_TOP_LEFT_CORNER);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_MOVE:
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "fleur");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "move");
            if (cursor == NULL)
                cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "all-scroll");
            if (cursor == NULL)
                cursor = gdk_cursor_new(GDK_SIZING);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_WAIT:
            cursor = gdk_cursor_new(GDK_WATCH);
            break;
        case com_sun_glass_ui_Cursor_CURSOR_DISAPPEAR:
        case com_sun_glass_ui_Cursor_CURSOR_NONE:
            cursor = gdk_cursor_new(GDK_BLANK_CURSOR);
            break;
        default:
            cursor = gdk_cursor_new(GDK_LEFT_PTR);
            break;
    }

    return cursor;
}

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
