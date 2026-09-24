/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _GLASS_VIEW_
#define _GLASS_VIEW_

#include "glass_win_api.h"

class BaseWnd;
class FullScreenWindow;

class GlassView {
public:
    /*
     * viewId is the Java-assigned identity the callback tables carry (glass_win_api.h, IDENTITY):
     * gwin_view_create - the only way a GlassView is made; the JNI WinView._create is gone since
     * ABI 5 - passes its int64_t view_id, and that value is what every slot delivers as
     * view_id. The view holds no jobject.
     */
    explicit GlassView(int64_t viewId);
    virtual ~GlassView();

    BOOL Close();

    BOOL EnterFullScreen(BOOL animate, BOOL keepRatio);
    void ExitFullScreen(BOOL animate);

    inline HWND GetHostHwnd() { return m_hostHwnd; }
    void SetHostHwnd(HWND m_hostHwnd);

    inline int64_t GetViewId() const { return m_viewId; }

    inline BOOL IsInputMethodEventEnabled() { return m_InputMethodEventsEnabled; }
    void EnableInputMethodEvents(BOOL enable);
    void FinishInputMethodComposition();

    // The former _uploadPixels GDI sequence - SetDIBitsToDevice, or UpdateLayeredWindow for a
    // transparent GlassWindow host - called by gwin_view_upload_pixels (the JNI body that shared it
    // is gone). bits is width * height 32-bit BGRA pixels, top-down, borrowed for the
    // call; NULL is passed through.
    void UploadPixels(int width, int height, const void* bits);

    /*
     * The callback tables of glass_win_api.h's view section. NULL while Java has installed nothing,
     * and an upcall site that finds NULL delivers nothing (there is no JNI path to take over).
     * Once installed no slot is ever NULL: the setters replace a NULL slot with a
     * no-op. Written on the launcher
     * thread before the toolkit thread exists and read on the toolkit thread only - no lock, like
     * the application, menu and preferences tables.
     */
    static inline const GwinViewCallbacks* ViewCallbacks()
    {
        return sm_viewCallbacksInstalled ? &sm_viewCallbacks : NULL;
    }
    static inline const GwinGestureCallbacks* GestureCallbacks()
    {
        return sm_gestureCallbacksInstalled ? &sm_gestureCallbacks : NULL;
    }
    static void SetViewCallbacks(const GwinViewCallbacks* cb);
    static void SetGestureCallbacks(const GwinGestureCallbacks* cb);

private:
    int64_t m_viewId;

    BaseWnd* m_fullScreenWindow;

    // The view is a lw object.
    // This HWND represents the embedder of the view
    // (a Glass window, or a FS window, or browser, whatever)
    HWND m_hostHwnd;

    // InputMethod
    BOOL m_InputMethodEventsEnabled;

    void NotifyFullscreen(bool entered);

    static GwinViewCallbacks sm_viewCallbacks;
    static bool sm_viewCallbacksInstalled;
    static GwinGestureCallbacks sm_gestureCallbacks;
    static bool sm_gestureCallbacksInstalled;
};

#endif // _GLASS_VIEW_
