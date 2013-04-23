/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

// this whole file comment related to huge comment in D3DWindow.java

#if 0
#include <jni.h>
#include <jni_md.h>
#define NO_JAWT
#ifndef NO_JAWT
#include <jawt.h>
#include <jawt_md.h>
#else
#include <windows.h>
#endif

#include <assert.h>

#include "com_sun_prism_d3d_D3DWindow.h"


JNIEXPORT  void JNICALL Java_com_sun_prism_d3d_D3DWindow_setParentWindow(JNIEnv *env, jclass clz, jlong hChild, jlong hParent) {
    HWND result = SetParent((HWND)hChild, (HWND)hParent);
    // printf("SetParent returned result=%x, lastError=%d", result, GetLastError());fflush(stdout);
    // SetWindowStyle(hChild, WS_OVERLAPPED|WS_CHILD);
}


JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d_D3DWindow_getWindowHandle
  (JNIEnv *env, jclass clz, jobject object) {
#ifndef NO_JAWT
    JAWT awt;
    JAWT_DrawingSurface* ds;
    JAWT_DrawingSurfaceInfo* dsi;
    JAWT_Win32DrawingSurfaceInfo* dsi_win;
    jboolean result;
    jint lock;
    jlong ret = 0;

    // Get the AWT interface, 1.3 or greater
    awt.version = JAWT_VERSION_1_3;
    result = JAWT_GetAWT(env, &awt);
    if (result == JNI_FALSE) {
        printf("D3DWindow:: failed to get AWT Native Interface.\n");
        fflush(stdout);
        return (jlong) 0;
    };

    // Get the drawing surface
    ds = awt.GetDrawingSurface(env, object);
    if (ds == NULL) {
        printf("D3DWindow:: failed to get AWT Native Interface.\n");
        fflush(stdout);
        return (jlong) 0;
    };

    // Lock the drawing surface
    lock = ds->Lock(ds);
    if ((lock & JAWT_LOCK_ERROR) != 0) {
        printf("D3DWindow:: lock failed.\n");
        fflush(stdout);
        awt.FreeDrawingSurface(ds);
        return (jlong) 0;
    };

    // Get the drawing surface info
    dsi = ds->GetDrawingSurfaceInfo(ds);

    if (dsi == NULL) {
        printf("Error getting surface info\n");
        ds->Unlock(ds);
        awt.FreeDrawingSurface(ds);
        return (jlong) 0;
    }
    ret = NULL;
    // Get the platform-specific drawing info
    dsi_win = (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;
//    printf("HWND=%x\n", dsi_win->hwnd); fflush(stdout);
//    printf("showing window.\n"); fflush(stdout);
    ShowWindow(dsi_win->hwnd, SW_SHOW);
    {
        // NOTE: investigate AWT issues with window being non-visible child?
        ret = (jlong)dsi_win->hwnd;
    }


    // Free the drawing surface info
    ds->FreeDrawingSurfaceInfo(dsi);

    // Unlock the drawing surface
    ds->Unlock(ds);

    // Free the drawing surface
    awt.FreeDrawingSurface(ds);
    return ret;
#else
    printf("Incompatible jawt, skipping\n");fflush(stdout);
    return (jlong)NULL;
#endif

}
#endif
