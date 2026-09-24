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

#include "GlassApplication.h"
#include "GlassClipboard.h"
#include "GlassScreen.h"
#include "GlassWindow.h"
#include "RoActivationSupport.h"


/**********************************
 * GlassApplication
 **********************************/

static LPCTSTR szGlassToolkitWindow = TEXT("GlassToolkitWindowClass");

GlassApplication *GlassApplication::pInstance = NULL;
bool GlassApplication::sm_shouldLeaveNestedLoop = false;

/*
 * gwin_app_create's constructor (glass_win_api.h, ABI 5): the four statements of the former JNI
 * constructor GlassApplication(jobject), with the no-JNI PlatformSupport - the same WinRT activation.
 */
GlassApplication::GlassApplication() : BaseWnd(), m_platformSupport()
{
    m_clipboardId = 0;
    m_hNextClipboardView = NULL;
    m_mainThreadId = ::GetCurrentThreadId();

    Create(NULL, 0, 0, 400, 300, TEXT(""), 0, 0, NULL);
}

GlassApplication::~GlassApplication()
{
}

LPCTSTR GlassApplication::GetWindowClassNameSuffix()
{
    return szGlassToolkitWindow;
}

LRESULT GlassApplication::WindowProc(UINT msg, WPARAM wParam, LPARAM lParam)
{
    switch (msg) {
        case WM_DO_ACTION:
        case WM_DO_ACTION_LATER:
            {
                Action * action = (Action *)wParam;
                action->Do();
                if (msg == WM_DO_ACTION_LATER) {
                    delete action;
                }
            }
            return 0;
        case WM_CREATE:
            pInstance = this;
            STRACE(_T("GlassApplication: created."));
            break;
        case WM_DESTROY:
            //Alarm clipboard dispose if any.
            //Please, use RegisterClipboardViewerId(0) instead of UnregisterClipboardViewer.
            RegisterClipboardViewerId(0);
            return 0;
        case WM_NCDESTROY:
            // pInstance is deleted in BaseWnd::StaticWindowProc
            pInstance = NULL;
            STRACE(_T("GlassApplication: destroyed."));
            return 0;
        case WM_CHANGECBCHAIN:
            if ((HWND)wParam == m_hNextClipboardView) {
                m_hNextClipboardView = (HWND)lParam;
            } else if (NULL != m_hNextClipboardView) {
                ::SendMessage(m_hNextClipboardView, WM_CHANGECBCHAIN, wParam, lParam);
            }
            break;
        case WM_DRAWCLIPBOARD:
            {
                // The slot fires only for a peer that registered through gwin_clipboard_register_viewer.
                const GwinClipboardCallbacks* cb = GlassClipboardCallbacks();
                if (cb != NULL && 0 != m_clipboardId) {
                    cb->content_changed(m_clipboardId);
                }
            }
            if (NULL != m_hNextClipboardView) {
                ::SendMessage(m_hNextClipboardView, WM_DRAWCLIPBOARD, wParam, lParam);
            }
            break;
        case WM_SETTINGCHANGE:
            if (m_platformSupport.onSettingChanged(wParam, lParam)) {
                return 0;
            }

            if ((UINT)wParam != SPI_SETWORKAREA) {
                break;
            }
            // Fall through
        case WM_DISPLAYCHANGE:
            GlassScreen::HandleDisplayChange();
            break;
        case WM_THEMECHANGED:
        case WM_SYSCOLORCHANGE:
        case WM_DWMCOLORIZATIONCOLORCHANGED:
            if (m_platformSupport.updatePreferences(
                    PlatformSupport::PreferenceType(PlatformSupport::PT_SYSTEM_COLORS |
                                                    PlatformSupport::PT_UI_SETTINGS))) {
                return 0;
            }
            break;
    }
    return ::DefWindowProc(GetHWND(), msg, wParam, lParam);
}

/*
 * The "alarm dispose" of whatever clipboard peer is registered: GwinClipboardCallbacks.dispose_peer
 * (glass_win_api.h) runs the peer's dispose body, which calls UnregisterClipboardViewer; if it did not (no
 * table, no-op slot, unknown id) the viewer is unregistered here so a re-registration never enters
 * the chain twice.
 */
void GlassApplication::DisposeRegisteredClipboard()
{
    if (0 != m_clipboardId) {
        const GwinClipboardCallbacks* cb = GlassClipboardCallbacks();
        if (cb != NULL) {
            cb->dispose_peer(m_clipboardId);
        }
        if (0 != m_clipboardId) {
            UnregisterClipboardViewer();
        }
    }
}

void GlassApplication::RegisterClipboardViewerId(int64_t clipboardId)
{
    DisposeRegisteredClipboard();
    if (0 != clipboardId) {
        m_clipboardId = clipboardId;
        m_hNextClipboardView = ::SetClipboardViewer(GetHWND()) ;
        STRACE(_T("RegisterClipboardViewerId"));
    }
}

/* static */
void GlassApplication::UnregisterClipboardViewer()
{
    if (NULL != m_hNextClipboardView) {
        ::ChangeClipboardChain(GetHWND(), m_hNextClipboardView);
        m_hNextClipboardView = NULL;
        STRACE(_T("UnregisterClipboardViewer"));
    }
    m_clipboardId = 0;
}

/* static */
void GlassApplication::ExecAction(Action *action)
{
    if (!pInstance) {
        return;
    }
    ::SendMessage(pInstance->GetHWND(), WM_DO_ACTION, (WPARAM)action, (LPARAM)0);
}

/* static */
void GlassApplication::ExecActionLater(Action *action)
{
    if (!pInstance) {
        delete action;
        return;
    }
    ::PostMessage(pInstance->GetHWND(), WM_DO_ACTION_LATER, (WPARAM)action, (LPARAM)0);
}

/*
 * static
 *
 * The pump itself, with no JVM in it: glass_win_api.h's gwin_enter_nested_event_loop forwards here.
 * The loop's return value is not this library's business: it never left Java, and WinApplication
 * holds it itself and stores it before calling gwin_leave_nested_event_loop.
 */
void GlassApplication::EnterNestedEventLoop()
{
    sm_shouldLeaveNestedLoop = false;

    MSG msg;
    while (GlassApplication::GetInstance()
            && !sm_shouldLeaveNestedLoop
            && ::GetMessage(&msg, NULL, 0, 0) > 0)
    {
        ::TranslateMessage(&msg);
        ::DispatchMessage(&msg);
    }

    sm_shouldLeaveNestedLoop = false;
}

/* static */
void GlassApplication::LeaveNestedEventLoop()
{
    sm_shouldLeaveNestedLoop = true;
}

ULONG GlassApplication::s_accessibilityCount = 0;

/* static */
ULONG GlassApplication::IncrementAccessibility()
{
    return InterlockedIncrement(&GlassApplication::s_accessibilityCount);
}

/* static */
ULONG GlassApplication::DecrementAccessibility()
{
    return InterlockedDecrement(&GlassApplication::s_accessibilityCount);
}

/* static */
ULONG GlassApplication::GetAccessibilityCount()
{
    return GlassApplication::s_accessibilityCount;
}

/*
 * No DllMain here, deliberately: the one that stored the module handle for the cursor code was dead. The CRT's
 * default DllMain calls DisableThreadLibraryCalls, which is ignored for a DLL with static TLS, as this one is;
 * nothing here consumes thread attach/detach notifications either way - do not re-add a DllMain.
 */
