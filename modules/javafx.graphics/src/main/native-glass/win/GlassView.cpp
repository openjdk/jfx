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

#include "common.h"

#include "FullScreenWindow.h"
#include "GlassApplication.h"
#include "GlassView.h"
#include "GlassWindow.h"
#include "Pixels.h"

#include "com_sun_glass_events_ViewEvent.h"


/*
 * ---- The callback tables of glass_win_api.h's view section ----
 *
 * Installed by gwin_view_set_callbacks / gwin_gesture_set_callbacks. A slot Java leaves NULL is
 * replaced by the matching no-op below, so the upcall sites in this file, ViewContainer.cpp and
 * FullScreenWindow.cpp dial slots without testing them; what those sites test is ViewCallbacks() /
 * GestureCallbacks() returning NULL, which means "nothing installed, deliver nothing".
 */
namespace {

void NoopNotifyView(int64_t, int32_t) {}
void NoopNotifyResize(int64_t, int32_t, int32_t) {}
void NoopNotifyRepaint(int64_t, int32_t, int32_t, int32_t, int32_t) {}
void NoopNotifyMenu(int64_t, int32_t, int32_t, int32_t, int32_t, int32_t) {}
void NoopNotifyKey(int64_t, int32_t, int32_t, const uint16_t*, int32_t, int32_t) {}
void NoopNotifyMouse(int64_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t,
                     int32_t) {}
void NoopNotifyScroll(int64_t, int32_t, int32_t, int32_t, int32_t, double, double, int32_t, int32_t,
                      int32_t, int32_t, int32_t, double, double) {}
void NoopNotifyInputMethod(int64_t, const uint16_t*, int32_t, const int32_t*, int32_t, const int32_t*,
                           const uint8_t*, int32_t, int32_t, int32_t, int32_t) {}
int32_t NoopNotifyImeCandidatePosRequest(int64_t, int32_t, double*) { return 0; }
int64_t NoopGetAccessible(int64_t) { return 0; }

void NoopGesturePerformed(int64_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t,
                          float, float, float, float, float, float, float) {}
void NoopInertiaGestureFinished(int64_t) {}
void NoopNotifyBeginTouchEvent(int64_t, int32_t, int32_t, int32_t) {}
void NoopNotifyNextTouchEvent(int64_t, int32_t, int64_t, int32_t, int32_t, int32_t, int32_t) {}
void NoopNotifyEndTouchEvent(int64_t) {}

const GwinViewCallbacks NOOP_VIEW_CALLBACKS = {
    NoopNotifyView, NoopNotifyResize, NoopNotifyRepaint, NoopNotifyMenu, NoopNotifyKey,
    NoopNotifyMouse, NoopNotifyScroll, NoopNotifyInputMethod, NoopNotifyImeCandidatePosRequest,
    NoopGetAccessible
};

const GwinGestureCallbacks NOOP_GESTURE_CALLBACKS = {
    NoopGesturePerformed, NoopInertiaGestureFinished, NoopNotifyBeginTouchEvent,
    NoopNotifyNextTouchEvent, NoopNotifyEndTouchEvent
};

} // namespace

GwinViewCallbacks GlassView::sm_viewCallbacks = NOOP_VIEW_CALLBACKS;
bool GlassView::sm_viewCallbacksInstalled = false;
GwinGestureCallbacks GlassView::sm_gestureCallbacks = NOOP_GESTURE_CALLBACKS;
bool GlassView::sm_gestureCallbacksInstalled = false;

/* By value - this library never retains the caller's struct - and a NULL slot keeps its no-op.
 * Installed once, before any view can exist; not safe against a concurrent install - the flag is a
 * plain bool and the table a plain struct assignment, with no release store between them. */
void GlassView::SetViewCallbacks(const GwinViewCallbacks* cb)
{
    GwinViewCallbacks t = NOOP_VIEW_CALLBACKS;
    if (cb != NULL) {
        if (cb->notify_view) t.notify_view = cb->notify_view;
        if (cb->notify_resize) t.notify_resize = cb->notify_resize;
        if (cb->notify_repaint) t.notify_repaint = cb->notify_repaint;
        if (cb->notify_menu) t.notify_menu = cb->notify_menu;
        if (cb->notify_key) t.notify_key = cb->notify_key;
        if (cb->notify_mouse) t.notify_mouse = cb->notify_mouse;
        if (cb->notify_scroll) t.notify_scroll = cb->notify_scroll;
        if (cb->notify_input_method) t.notify_input_method = cb->notify_input_method;
        if (cb->notify_ime_candidate_pos_request) {
            t.notify_ime_candidate_pos_request = cb->notify_ime_candidate_pos_request;
        }
        if (cb->get_accessible) t.get_accessible = cb->get_accessible;
    }
    sm_viewCallbacksInstalled = false;
    sm_viewCallbacks = t;
    sm_viewCallbacksInstalled = cb != NULL;
}

void GlassView::SetGestureCallbacks(const GwinGestureCallbacks* cb)
{
    GwinGestureCallbacks t = NOOP_GESTURE_CALLBACKS;
    if (cb != NULL) {
        if (cb->gesture_performed) t.gesture_performed = cb->gesture_performed;
        if (cb->inertia_gesture_finished) t.inertia_gesture_finished = cb->inertia_gesture_finished;
        if (cb->notify_begin_touch_event) t.notify_begin_touch_event = cb->notify_begin_touch_event;
        if (cb->notify_next_touch_event) t.notify_next_touch_event = cb->notify_next_touch_event;
        if (cb->notify_end_touch_event) t.notify_end_touch_event = cb->notify_end_touch_event;
    }
    sm_gestureCallbacksInstalled = false;
    sm_gestureCallbacks = t;
    sm_gestureCallbacksInstalled = cb != NULL;
}

GlassView::GlassView(int64_t viewId) :
    m_viewId(viewId),
    m_fullScreenWindow(NULL),
    m_hostHwnd(NULL)
{
}

GlassView::~GlassView()
{
}

BOOL GlassView::Close()
{
    if (m_fullScreenWindow) {
        FullScreenWindow * fs = dynamic_cast<FullScreenWindow*>(m_fullScreenWindow);
        if (fs) {
            fs->Close();
        }
        m_fullScreenWindow = NULL;
    }

    return TRUE;
}

BOOL GlassView::EnterFullScreen(BOOL animate, BOOL keepRatio)
{
    GlassWindow *pWindow = GlassWindow::FromHandle(GetHostHwnd());
    if (pWindow) {
        m_fullScreenWindow = pWindow;
    } else {
        // create new FullScreen window to handle "ownerless" views
        FullScreenWindow * w = new FullScreenWindow();
        w->Create();
        m_fullScreenWindow = w;
    }

    BOOL ret = m_fullScreenWindow->EnterFullScreenMode(this, animate, keepRatio);
    if (ret) {
        NotifyFullscreen(true);
    }

    return ret;
}

void GlassView::ExitFullScreen(BOOL animate)
{
    if (!m_fullScreenWindow) {
        return;
    }

    m_fullScreenWindow->ExitFullScreenMode(animate);
    m_fullScreenWindow = NULL;
    NotifyFullscreen(false);
}

void GlassView::NotifyFullscreen(bool entered)
{
    const int type = entered ? com_sun_glass_events_ViewEvent_FULLSCREEN_ENTER :
            com_sun_glass_events_ViewEvent_FULLSCREEN_EXIT;
    const GwinViewCallbacks* cb = ViewCallbacks();
    if (cb != NULL) {
        cb->notify_view(GetViewId(), type);
    }
}

void GlassView::SetHostHwnd(HWND m_hostHwnd)
{
    if (this->m_hostHwnd == m_hostHwnd) {
        return;
    }

    const GwinViewCallbacks* cb = ViewCallbacks();

    if (this->m_hostHwnd) {
        this->m_hostHwnd = NULL;

        if (cb != NULL) {
            cb->notify_view(GetViewId(), com_sun_glass_events_ViewEvent_REMOVE);
        }
    }

    this->m_hostHwnd = m_hostHwnd;

    if (m_hostHwnd) {
        if (cb != NULL) {
            cb->notify_view(GetViewId(), com_sun_glass_events_ViewEvent_ADD);
        }
    }
}

void GlassView::EnableInputMethodEvents(BOOL enable)
{
    m_InputMethodEventsEnabled = enable;
}

void GlassView::FinishInputMethodComposition()
{
    HWND hwnd = GetHostHwnd();
    if (hwnd)
    {
        HIMC hIMC = ::ImmGetContext(hwnd);
        if(hIMC)
        {
            ::ImmNotifyIME(hIMC, NI_COMPOSITIONSTR, CPS_COMPLETE, 0);
            ::ImmReleaseContext(hwnd, hIMC);
        }
    }
}

void GlassView::UploadPixels(int width, int height, const void* bits)
{
    HWND hWnd = GetHostHwnd();
    if (!::IsWindow(hWnd)) {
        //NOTE: uploadPixels() may be invoked from a thread other than the
        //      toolkit thread. Therefore, when SendMessage() is finally
        //      processed, the hWnd may be stale already, and hence
        //      the pWindow == NULL shouldn't be surprising.
        return;
    }

    GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);

    if (!pWindow || !pWindow->IsTransparent()) {
        // Either a non-glass window (FullScreenWindow), or not transparent
        BITMAPINFOHEADER bmi;

        ZeroMemory(&bmi, sizeof(bmi));
        bmi.biSize = sizeof(bmi);
        bmi.biWidth = width;
        bmi.biHeight = -height;
        bmi.biPlanes = 1;
        bmi.biBitCount = 32;
        bmi.biCompression = BI_RGB;

        HDC hdcDst = ::GetDC(hWnd);
        ::SetDIBitsToDevice(
                hdcDst,
                0, 0, width, height,
                0, 0,
                0, height,
                bits,
                (BITMAPINFO*)&bmi, DIB_RGB_COLORS);
        ::ReleaseDC(hWnd, hdcDst);
    } else { // IsTransparent() == TRUE
        // http://msdn.microsoft.com/en-us/library/ms997507.aspx
        RECT rect;
        ::GetWindowRect(hWnd, &rect);
        SIZE size = { rect.right - rect.left, rect.bottom - rect.top };

        if (size.cx != width || size.cy != height) {
            //XXX: should report a error? OTOH, we could proceed, but
            //this will cause the window to resize to the size of the bitmap
            return;
        }

        POINT ptSrc = { 0, 0 };
        POINT ptDst = { rect.left, rect.top };

        BLENDFUNCTION bf;
        bf.SourceConstantAlpha = pWindow->GetAlpha();
        bf.AlphaFormat = AC_SRC_ALPHA;
        bf.BlendOp = AC_SRC_OVER;
        bf.BlendFlags = 0;

        DIBitmap bitmap(width, height, bits);

        HDC hdcDst = ::GetDC(NULL);
        HDC hdcSrc = ::CreateCompatibleDC(NULL);
        HBITMAP oldBitmap = (HBITMAP)::SelectObject(hdcSrc, bitmap);

        ::UpdateLayeredWindow(hWnd, hdcDst, &ptDst, &size, hdcSrc, &ptSrc,
                RGB(0, 0, 0), &bf, ULW_ALPHA);

        ::SelectObject(hdcSrc, oldBitmap);
        ::DeleteDC(hdcSrc);
        ::ReleaseDC(NULL, hdcDst);
    }
}
