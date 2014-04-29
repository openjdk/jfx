/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "common.h"

#include "GlassApplication.h"
#include "Utils.h"
#include "Pixels.h"
#include "GlassCursor.h"

#include "com_sun_glass_ui_Cursor.h"
#include "com_sun_glass_ui_win_WinCursor.h"

static HCURSOR GetNativeCursor(int type)
{
    LPCTSTR winCursor;
    switch (type) {
        case com_sun_glass_ui_Cursor_CURSOR_DEFAULT:
            winCursor = IDC_ARROW;
            break;
        case com_sun_glass_ui_Cursor_CURSOR_TEXT:
            winCursor = IDC_IBEAM;
            break;
        case com_sun_glass_ui_Cursor_CURSOR_CROSSHAIR:
            winCursor = IDC_CROSS;
            break;
        case com_sun_glass_ui_Cursor_CURSOR_CLOSED_HAND:
            winCursor = L"IDC_CLOSED_HAND";
            break;
        case com_sun_glass_ui_Cursor_CURSOR_OPEN_HAND:
            winCursor = L"IDC_OPEN_HAND";
            break;
        case com_sun_glass_ui_Cursor_CURSOR_POINTING_HAND:
            winCursor = IDC_HAND;
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_UP:
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_DOWN:
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_UPDOWN:
             winCursor = IDC_SIZENS;
             break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_LEFT:
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_RIGHT:
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_LEFTRIGHT:
            winCursor = IDC_SIZEWE;
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_SOUTHWEST:
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_NORTHEAST:
            winCursor = IDC_SIZENESW;
            break;
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_SOUTHEAST:
        case com_sun_glass_ui_Cursor_CURSOR_RESIZE_NORTHWEST:
            winCursor = IDC_SIZENWSE;
            break;
        case com_sun_glass_ui_Cursor_CURSOR_MOVE:
            winCursor = IDC_SIZEALL;
            break;
        case com_sun_glass_ui_Cursor_CURSOR_DISAPPEAR:
            // Not implemented, using CURSOR_DEFAULT instead
            winCursor = IDC_ARROW;
            break;
        case com_sun_glass_ui_Cursor_CURSOR_WAIT:
            winCursor = IDC_WAIT;
            break;
        case com_sun_glass_ui_Cursor_CURSOR_NONE:
            return NULL;
        default:
            winCursor = IDC_ARROW;
            break;
    }

    HCURSOR hCursor = ::LoadCursor(NULL, winCursor);
    if (!hCursor) {
        // Not a system cursor, check for resource
        hCursor = ::LoadCursor(GlassApplication::GetHInstance(), winCursor);
    }
    if (!hCursor) {
        hCursor = ::LoadCursor(NULL, IDC_ARROW);
    }
    ASSERT(hCursor);

    return hCursor;
}

HCURSOR JCursorToHCURSOR(JNIEnv *env, jobject jCursor)
{
    if (!jCursor) {
        return NULL;
    }

    jint type = env->CallIntMethod(jCursor, javaIDs.Cursor.getType);
    if (type != com_sun_glass_ui_Cursor_CURSOR_CUSTOM) {
        return GetNativeCursor(type);
    }

    return (HCURSOR)env->CallLongMethod(jCursor, javaIDs.Cursor.getNativeCursor);
}

extern "C" {

/*
 * Class:     com_sun_glass_ui_win_WinCursor
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinCursor__1initIDs
(JNIEnv *env, jclass cls)
{
    cls = env->FindClass("com/sun/glass/ui/Size");
    if (env->ExceptionCheck()) return;

    javaIDs.Size.init = env->GetMethodID(cls, "<init>", "(II)V");
    ASSERT(javaIDs.Size.init);
    if (env->ExceptionCheck()) return;

    cls = env->FindClass("com/sun/glass/ui/Cursor");
    if (env->ExceptionCheck()) return;
    
    javaIDs.Cursor.getType = env->GetMethodID(cls, "getType", "()I");
    ASSERT(javaIDs.Cursor.getType);
    if (env->ExceptionCheck()) return;

    javaIDs.Cursor.getNativeCursor = env->GetMethodID(cls, "getNativeCursor", "()J");
    ASSERT(javaIDs.Cursor.getNativeCursor);
    if (env->ExceptionCheck()) return;
}
        
/*
 * Class:     com_sun_glass_ui_win_WinCursor
 * Method:    _createCursor
 * Signature: (IIIILjava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinCursor__1createCursor
        (JNIEnv *env, jobject jThis, jint x, jint y, jobject pixels)
{
    return (jlong)Pixels::CreateCursor(env, pixels, x, y);
}

/*
 * Class:     com_sun_glass_ui_win_WinCursor
 * Method:    _setVisible
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinCursor__1setVisible
    (JNIEnv *env, jclass jCursorClass, jboolean jshow)
{
    static BOOL visible = TRUE;
    BOOL show = jbool_to_bool(jshow);

    //XXX: not thread safe
    if (show != visible) {
        ::ShowCursor(show);
        visible = show;
    }
}

/*
 * Class:     com_sun_glass_ui_win_WinCursor
 * Method:    _getBestSize
 * Signature: (II)Lcom.sun.glass.ui.Size;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_win_WinCursor__1getBestSize
        (JNIEnv *env, jclass jCursorClass, jint width, jint height)
{
    return env->NewObject(GlassApplication::ClassForName(env, "com.sun.glass.ui.Size"),
            javaIDs.Size.init,
            (jint)::GetSystemMetrics(SM_CXCURSOR),
            (jint)::GetSystemMetrics(SM_CYCURSOR));
}
} // extern "C"
