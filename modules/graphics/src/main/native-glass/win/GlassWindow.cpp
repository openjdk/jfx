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

#include "FullScreenWindow.h"
#include "GlassApplication.h"
#include "GlassWindow.h"
#include "GlassScreen.h"
#include "GlassMenu.h"
#include "GlassView.h"
#include "GlassDnD.h"
#include "Pixels.h"
#include "AccessibleRoot.h"
#include "GlassCursor.h"
#include "GlassScreen.h"

#include "com_sun_glass_events_WindowEvent.h"
#include "com_sun_glass_ui_Window.h"
#include "com_sun_glass_ui_Window_Level.h"
#include "com_sun_glass_ui_win_WinWindow.h"


// Helper LEAVE_MAIN_THREAD for GlassWindow
#define LEAVE_MAIN_THREAD_WITH_hWnd  \
    HWND hWnd;  \
    LEAVE_MAIN_THREAD;  \
    ARG(hWnd) = (HWND)ptr;

static LPCTSTR szGlassWindowClassName = TEXT("GlassWindowClass");

static jmethodID midNotifyClose;
static jmethodID midNotifyMove;
static jmethodID midNotifyResize;
static jmethodID midNotifyMoveToAnotherScreen;

unsigned int GlassWindow::sm_instanceCounter = 0;
HHOOK GlassWindow::sm_hCBTFilter = NULL;
HWND GlassWindow::sm_grabWindow = NULL;

GlassWindow::GlassWindow(jobject jrefThis, bool isTransparent, bool isDecorated, bool isUnified, bool isChild, HWND parentOrOwner)
    : BaseWnd(parentOrOwner),
    ViewContainer(),
    m_state(Normal),
    m_isFocusable(true),
    m_isFocused(false),
    m_focusEvent(0),
    m_isResizable(true),
    m_isTransparent(isTransparent),
    m_isDecorated(isDecorated),
    m_isUnified(isUnified),
    m_hMenu(NULL),
    m_alpha(255),
    m_isEnabled(true),
    m_parent(isChild ? parentOrOwner : NULL),
    m_delegateWindow(NULL),
    m_isInFullScreen(false),
    m_beforeFullScreenStyle(0),
    m_beforeFullScreenExStyle(0),
    m_beforeFullScreenMenu(NULL),
    m_pProvider(NULL),
    m_a11yInitRequested(false),
    m_a11yTreeIsReady(false),
    m_hIcon(NULL)
{
    m_grefThis = GetEnv()->NewGlobalRef(jrefThis);
    m_minSize.x = m_minSize.y = -1;   // "not set" value
    m_maxSize.x = m_maxSize.y = -1;   // "not set" value
    m_hMonitor = NULL;
    m_insets.left = m_insets.top = m_insets.right = m_insets.bottom = 0;
    m_beforeFullScreenRect.left = m_beforeFullScreenRect.top =
        m_beforeFullScreenRect.right = m_beforeFullScreenRect.bottom = 0;

    if (++GlassWindow::sm_instanceCounter == 1) {
        GlassWindow::sm_hCBTFilter =
            ::SetWindowsHookEx(WH_CBT,
                    (HOOKPROC)GlassWindow::CBTFilter,
                    0, GlassApplication::GetMainThreadId());
    }
    if (isChild) {
        GlassApplication::InstallMouseLLHook();
    }
}

GlassWindow::~GlassWindow()
{
    if (m_hIcon) {
        ::DestroyIcon(m_hIcon);
    }

    if (m_grefThis) {
        GetEnv()->DeleteGlobalRef(m_grefThis);
    }

    if (IsChild()) {
        GlassApplication::UninstallMouseLLHook();
    }

    if (--GlassWindow::sm_instanceCounter == 0) {
        ::UnhookWindowsHookEx(GlassWindow::sm_hCBTFilter);
    }
}

LPCTSTR GlassWindow::GetWindowClassNameSuffix()
{
    return szGlassWindowClassName;
}

HWND GlassWindow::Create(DWORD dwStyle, DWORD dwExStyle, HMONITOR hMonitor, HWND owner)
{
    m_hMonitor = hMonitor;

    int x = CW_USEDEFAULT;
    int y = CW_USEDEFAULT;
    int w = CW_USEDEFAULT;
    int h = CW_USEDEFAULT;
    if ((dwStyle & WS_POPUP) != 0) {
        // CW_USEDEFAULT doesn't work for WS_POPUP windows
        RECT r;
        if (BaseWnd::GetDefaultWindowBounds(&r)) {
            x = r.left;
            y = r.top;
            w = r.right - r.left;
            h = r.bottom - r.top;
        }
    }

    HWND hwnd = BaseWnd::Create(owner, x, y, w, h,
                                TEXT(""), dwExStyle, dwStyle, NULL);

    ViewContainer::InitDropTarget(hwnd);
    ViewContainer::InitManipProcessor(hwnd);

    return hwnd;
}

void GlassWindow::Close()
{
    UngrabFocus();
    ViewContainer::ReleaseDropTarget();
    ViewContainer::ReleaseManipProcessor();
}

void GlassWindow::setMinSize(long width, long height)
{
    m_minSize.x = width;
    m_minSize.y = height;
}

void GlassWindow::setMaxSize(long width, long height)
{
    m_maxSize.x = width;
    m_maxSize.y = height;
}

void GlassWindow::updateMinMaxSize(RECT &windowRect)
{
    if (m_minSize.x >= 0) {
        // min size has been set
        if (windowRect.right - windowRect.left < m_minSize.x) {
            windowRect.right = windowRect.left + m_minSize.x;
        }
        if (windowRect.bottom - windowRect.top < m_minSize.y) {
            windowRect.bottom = windowRect.top + m_minSize.y;
        }
    }
    if (m_maxSize.x >= 0) {
        // max size has been set
        if (windowRect.right - windowRect.left > m_maxSize.x) {
            windowRect.right = windowRect.left + m_maxSize.x;
        }
        if (windowRect.bottom - windowRect.top > m_maxSize.y) {
            windowRect.bottom = windowRect.top + m_maxSize.y;
        }
    }

}

void GlassWindow::SetFocusable(bool isFocusable)
{
    m_isFocusable = isFocusable;

    LONG exStyle = ::GetWindowLong(GetHWND(), GWL_EXSTYLE);
    if (!isFocusable) {
        //NOTE: this style works 'by itself' when there's only one window
        //      in this application. It does prevent the window from activation
        //      then. However, as soon as there is another window, we also need
        //      to handle WM_MOUSEACTIVATE and use the CBTFilter() hook.
        //      The useful part of the style: it removes the window from the
        //      task bar (and the Alt-Tab list).
        ::SetWindowLong(GetHWND(), GWL_EXSTYLE, exStyle | WS_EX_NOACTIVATE);

        if (::GetFocus() == GetHWND()) {
            // We can't resign activation, but at least we can reset the focus
            ::SetFocus(NULL);
        }
    } else {
        ::SetWindowLong(GetHWND(), GWL_EXSTYLE, exStyle & ~WS_EX_NOACTIVATE);
    }
}

LRESULT CALLBACK GlassWindow::CBTFilter(int nCode, WPARAM wParam, LPARAM lParam)
{
    if (nCode == HCBT_ACTIVATE || nCode == HCBT_SETFOCUS) {
        BaseWnd *pWindow = BaseWnd::FromHandle((HWND)wParam);
        if (pWindow && pWindow->IsGlassWindow()) {
            GlassWindow * window = (GlassWindow*)pWindow;

            if (!window->IsEnabled()) {
                window->HandleFocusDisabledEvent();
                return 1;
            }
            if (!window->IsFocusable()) {
                return 1;
            }
        }
    }
    return ::CallNextHookEx(GlassWindow::sm_hCBTFilter, nCode, wParam, lParam);
}

LRESULT GlassWindow::WindowProc(UINT msg, WPARAM wParam, LPARAM lParam)
{
    MessageResult commonResult = BaseWnd::CommonWindowProc(msg, wParam, lParam);
    if (commonResult.processed) {
        return commonResult.result;
    }

    switch (msg) {
        case WM_SHOWWINDOW:
            // It's possible that move/size events are reported by the platform
            // before the peer listener is set. As a result, location/size are
            // not reported, so resending them from here.
            HandleMoveEvent(NULL);
            HandleSizeEvent(com_sun_glass_events_WindowEvent_RESIZE, NULL);
            // The call below may be restricted to WS_POPUP windows
            NotifyViewSize(GetHWND());

            if (!wParam) {
                ResetMouseTracking(GetHWND());
            }
            if (IS_WINVISTA) {
                ::SendMessage(GetHWND(), WM_DWMCOMPOSITIONCHANGED, 0, 0);
            }
            break;
        case WM_DWMCOMPOSITIONCHANGED:
            if (m_isUnified && (IS_WINVISTA)) {
                BOOL bEnabled = FALSE;
                if(SUCCEEDED(::DwmIsCompositionEnabled(&bEnabled)) && bEnabled) {
                    MARGINS dwmMargins = { -1, -1, -1, -1 };
                    ::DwmExtendFrameIntoClientArea(GetHWND(), &dwmMargins);
                }
            }
            //When toggling between Aero and Classic theme the size of window changes
            //No predefined WM_SIZE event type for this, so using -1 as parameters
            HandleViewSizeEvent(GetHWND(), -1, -1, -1);
            break;
        case WM_SIZE:
            switch (wParam) {
                case SIZE_RESTORED:
                    if (m_state != Normal) {
                        HandleSizeEvent(com_sun_glass_events_WindowEvent_RESTORE, NULL);
                        m_state = Normal;
                    } else {
                        HandleSizeEvent(com_sun_glass_events_WindowEvent_RESIZE, NULL);
                    }
                    break;
                case SIZE_MINIMIZED:
                    HandleSizeEvent(com_sun_glass_events_WindowEvent_MINIMIZE, NULL);
                    m_state = Minimized;
                    break;
                case SIZE_MAXIMIZED:
                    HandleSizeEvent(com_sun_glass_events_WindowEvent_MAXIMIZE, NULL);
                    m_state = Maximized;
                    break;
            }
            HandleViewSizeEvent(GetHWND(), msg, wParam, lParam);
            break;
//        case WM_MOVING:
//            HandleMoveEvent((RECT *)lParam);
//            break;
        case WM_MOVE:
            HandleMoveEvent(NULL);
            break;
        case WM_WINDOWPOSCHANGED:
            HandleWindowPosChangedEvent();
            break;
        case WM_CLOSE:
            HandleCloseEvent();
            return 0;
        case WM_DESTROY:
            HandleDestroyEvent();
            return 0;
        case WM_ACTIVATE:
            if (IsInFullScreenMode()) {
                HWND hWndInsertAfter = LOWORD(wParam) != WA_INACTIVE ? HWND_TOPMOST : HWND_BOTTOM;
                ::SetWindowPos(GetHWND(), hWndInsertAfter, 0, 0, 0, 0,
                        SWP_ASYNCWINDOWPOS | SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOOWNERZORDER | SWP_NOSIZE);
            }
            if (!GetDelegateWindow()) {
                HandleActivateEvent(LOWORD(wParam) != WA_INACTIVE ?
                        com_sun_glass_events_WindowEvent_FOCUS_GAINED :
                        com_sun_glass_events_WindowEvent_FOCUS_LOST);
            }
            // Let the DefWindowProc() set the focus to this window
            break;
        case WM_MOUSEACTIVATE:
            if (!IsEnabled()) {
                HandleFocusDisabledEvent();
                // Do not activate, and discard the event
                return MA_NOACTIVATEANDEAT;
            }
            if (!IsFocusable()) {
                // Do not activate, but pass the mouse event
                return MA_NOACTIVATE;
            }
            break;
        case WM_SETFOCUS:
            if (!GetDelegateWindow()) {
                SetFocused(true);
                if (IsChild()) {
                    // Synthesize the event
                    HandleActivateEvent(m_focusEvent ? m_focusEvent : com_sun_glass_events_WindowEvent_FOCUS_GAINED);
                    m_focusEvent = 0;
                }
            }
            break;
        case WM_KILLFOCUS:
            if (!GetDelegateWindow()) {
                SetFocused(false);
                if (IsChild()) {
                    // Synthesize the event
                    HandleActivateEvent(com_sun_glass_events_WindowEvent_FOCUS_LOST);
                }
            }
            break;
        case WM_GETMINMAXINFO:
            if (m_minSize.x >= 0 || m_minSize.y >= 0 ||
                    m_maxSize.x >= 0 || m_maxSize.y >= 0)
            {
                MINMAXINFO *info = (MINMAXINFO *)lParam;
                if (m_minSize.x >= 0) {
                    info->ptMinTrackSize.x = m_minSize.x;
                }
                if (m_minSize.y >= 0) {
                    info->ptMinTrackSize.y = m_minSize.y;
                }
                if (m_maxSize.x >= 0) {
                    info->ptMaxTrackSize.x = m_maxSize.x;
                }
                if (m_maxSize.y >= 0) {
                    info->ptMaxTrackSize.y = m_maxSize.y;
                }
                return 0;
            }
            break;
        case WM_COMMAND:
            if (HandleCommand(LOWORD(wParam))) {
                return 0;
            }
            break;
        case WM_INPUTLANGCHANGE:
            HandleViewInputLangChange(GetHWND(), msg, wParam, lParam);
            return 0;
        case WM_NCCALCSIZE:
// Workaround for RT-13998. It has some side effects and thus commented out
//            if ((BOOL)wParam && !IsDecorated()) {
//                NCCALCSIZE_PARAMS *p = (NCCALCSIZE_PARAMS *)lParam;
//                p->rgrc[0].right++;
//                p->rgrc[0].bottom++;
//                return WVR_VALIDRECTS;
//            }
            break;
        case WM_PAINT:
            HandleViewPaintEvent(GetHWND(), msg, wParam, lParam);
            break;
        case WM_CONTEXTMENU:
            HandleViewMenuEvent(GetHWND(), msg, wParam, lParam);
            break;
        case WM_LBUTTONDOWN:
        case WM_RBUTTONDOWN:
        case WM_MBUTTONDOWN:
            CheckUngrab(); // check if other owned windows hierarchy holds the grab
            if (IsChild() && !IsFocused() && IsFocusable()) {
                RequestFocus(com_sun_glass_events_WindowEvent_FOCUS_GAINED);
            }
            // ... and fall through for other mouse events
        case WM_LBUTTONUP:
        case WM_LBUTTONDBLCLK:
        case WM_RBUTTONUP:
        case WM_RBUTTONDBLCLK:
        case WM_MBUTTONUP:
        case WM_MBUTTONDBLCLK:
        case WM_MOUSEWHEEL:
        case WM_MOUSEHWHEEL:
        case WM_MOUSELEAVE:
        case WM_MOUSEMOVE:
            if (IsEnabled()) {
                if (msg == WM_MOUSELEAVE && GetDelegateWindow()) {
                    // Skip generating MouseEvent.EXIT when entering FullScreen
                    return 0;
                }
                BOOL handled = HandleViewMouseEvent(GetHWND(), msg, wParam, lParam);
                if (handled && msg == WM_RBUTTONUP) {
                    // By default, DefWindowProc() sends WM_CONTEXTMENU from WM_LBUTTONUP
                    // Since DefWindowProc() is not called, call the mouse menu handler directly 
                    HandleViewMenuEvent(GetHWND(), WM_CONTEXTMENU, (WPARAM) GetHWND(), ::GetMessagePos ());
                    //::DefWindowProc(GetHWND(), msg, wParam, lParam);
                }
                if (handled) {
                    // Do not call the DefWindowProc() for mouse events that were handled
                    return 0;
                }
            } else {
                HandleFocusDisabledEvent();
                return 0;
            }
            break;
        case WM_CAPTURECHANGED:
            ViewContainer::NotifyCaptureChanged(GetHWND(), (HWND)lParam);
            break;
        case WM_SYSKEYDOWN:
        case WM_SYSKEYUP:
        case WM_KEYDOWN:
        case WM_KEYUP:
            if (!IsEnabled()) {
                return 0;
            }
            HandleViewKeyEvent(GetHWND(), msg, wParam, lParam);
            // Always pass the message down to the DefWindowProc() to handle
            // system keys (Alt+F4, etc.) with only excpetion for F10 and ALT:
            if (!GetMenu()) {
                if (wParam == VK_MENU || (wParam == VK_F10 && !GetModifiers())) {
                    // Disable activation of the window's system menu
                    return 0;
                }
            }
            break;
        case WM_CHAR:
        case WM_IME_CHAR:
            if (IsEnabled()) {
                HandleViewTypedEvent(GetHWND(), msg, wParam, lParam);
                return 0;
            }
            break;
        case WM_IME_COMPOSITION:
        case WM_IME_ENDCOMPOSITION:
        case WM_IME_NOTIFY:
        case WM_IME_STARTCOMPOSITION:
            if (IsEnabled() &&
                HandleViewInputMethodEvent(GetHWND(), msg, wParam, lParam)) {
                return 0;
            }
            break;
        case WM_NCLBUTTONDOWN:
        case WM_NCMBUTTONDOWN:
        case WM_NCRBUTTONDOWN:
        case WM_NCXBUTTONDOWN:
            UngrabFocus(); // ungrab itself
            CheckUngrab(); // check if other owned windows hierarchy holds the grab
            // Pass the event to DefWindowProc()
            break;
        case WM_TOUCH:
            if (IsEnabled()) {
                HandleViewTouchEvent(GetHWND(), msg, wParam, lParam);
                return 0;
            }
            break;
        case WM_TIMER:
            HandleViewTimerEvent(GetHWND(), wParam);
            return 0;
        case WM_GETOBJECT:
            //setvbuf(stdout, NULL, _IONBF, 0);  // turn off stdout buffering
            if (!m_a11yInitRequested) {
                m_a11yInitRequested = true;  // only call once
                HandleAccessibilityInitEvent();  // initialize
            } else if (m_a11yTreeIsReady) {
                //TODO: From spec: When a window that previously returned providers has been destroyed,
                // you should notify UI Automation by calling UiaReturnRawElementProvider(hwnd, 0, 0, NULL)
                // Do this from WM_DESTROY?
                LRESULT lr = UiaReturnRawElementProvider(GetHWND(), wParam, lParam, m_pProvider);
                //TODO: It's not clear that Release() is needed.  Some examples use it; some don't.
                // I'm getting a premature call to the d'tor so I'm removing it at least for now
                //m_pProvider->Release();
                return lr;
            }
            break;
    }

    return ::DefWindowProc(GetHWND(), msg, wParam, lParam);
}

void GlassWindow::HandleCloseEvent()
{
    JNIEnv* env = GetEnv();

    env->CallVoidMethod(m_grefThis, midNotifyClose);
    CheckAndClearException(env);
}

void GlassWindow::HandleDestroyEvent()
{
    JNIEnv* env = GetEnv();

    env->CallVoidMethod(m_grefThis, javaIDs.Window.notifyDestroy);
    CheckAndClearException(env);
}

// if pRect == NULL => get position/size by GetWindowRect
void GlassWindow::HandleMoveEvent(RECT *pRect)
{
    JNIEnv* env = GetEnv();

    RECT r;
    if (pRect == NULL) {
        ::GetWindowRect(GetHWND(), &r);
        pRect = &r;
    }

    env->CallVoidMethod(m_grefThis, midNotifyMove, pRect->left, pRect->top);
    CheckAndClearException(env);
}

// if pRect == NULL => get position/size by GetWindowRect
void GlassWindow::HandleSizeEvent(int type, RECT *pRect)
{
    JNIEnv* env = GetEnv();

    RECT r;
    if (pRect == NULL) {
        ::GetWindowRect(GetHWND(), &r);
        pRect = &r;
    }

    env->CallVoidMethod(m_grefThis, midNotifyResize,
                        type, pRect->right-pRect->left, pRect->bottom-pRect->top);
    CheckAndClearException(env);
}

void GlassWindow::HandleWindowPosChangedEvent()
{
    JNIEnv* env = GetEnv();

    HMONITOR toMonitor = ::MonitorFromWindow(GetHWND(), MONITOR_DEFAULTTOPRIMARY);
    HMONITOR fromMonitor = GetMonitor();
    if (toMonitor != fromMonitor) {
        env->CallVoidMethod(m_grefThis, midNotifyMoveToAnotherScreen,
                            GlassScreen::CreateJavaMonitor(env, toMonitor));
        CheckAndClearException(env);
        SetMonitor(toMonitor);
    }
}

void GlassWindow::HandleActivateEvent(jint event)
{
    const bool active = event != com_sun_glass_events_WindowEvent_FOCUS_LOST;

    if (!active) {
        UngrabFocus();
    }
    
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_grefThis, javaIDs.Window.notifyFocus, event);
    CheckAndClearException(env);
}

void GlassWindow::HandleFocusDisabledEvent()
{
    JNIEnv* env = GetEnv();

    env->CallVoidMethod(m_grefThis, javaIDs.Window.notifyFocusDisabled);
    CheckAndClearException(env);
}

void GlassWindow::HandleAccessibilityInitEvent() {

    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_grefThis, javaIDs.Window.notifyInitAccessibility);
    CheckAndClearException(env);
}

void GlassWindow::SetAccessibilityInitIsComplete(AccessibleRoot* pAcc) {
    m_pProvider = pAcc;
    m_a11yTreeIsReady = true;
}

bool GlassWindow::HandleCommand(WORD cmdID) {
    return HandleMenuCommand(GetHWND(), cmdID);
}

HMONITOR GlassWindow::GetMonitor()
{
    return m_hMonitor;
}

void GlassWindow::SetMonitor(HMONITOR hMonitor)
{
    m_hMonitor = hMonitor;
}

void GlassWindow::SetAlpha(BYTE alpha)
{
    m_alpha = alpha;

    if (m_isTransparent) {
        // If the window is transparent, the opacity is handled in
        // uploadPixels() below (see BLENDFUNCTION structure
        // and its SourceConstantAlpha member)
        return;
    }

    // The window is opaque. We make it layered temporarily only when
    // its alpha is less than 0xFF.
    LONG exStyle = ::GetWindowLong(GetHWND(), GWL_EXSTYLE);

    if (alpha == 0xFF) {
        if (exStyle & WS_EX_LAYERED) {
            ::SetWindowLong(GetHWND(), GWL_EXSTYLE, exStyle & ~WS_EX_LAYERED);
        }
    } else {
        if (!(exStyle & WS_EX_LAYERED)) {
            ::SetWindowLong(GetHWND(), GWL_EXSTYLE, exStyle | WS_EX_LAYERED);
        }
        ::SetLayeredWindowAttributes(GetHWND(), RGB(0, 0, 0), alpha, LWA_ALPHA);
    }
}

void GlassWindow::UpdateInsets()
{
    if (::IsIconic(GetHWND())) {
        return;
    }

    RECT outer, inner;

    ::GetWindowRect(GetHWND(), &outer);
    ::GetClientRect(GetHWND(), &inner);

    ::MapWindowPoints(GetHWND(), (HWND)NULL, (LPPOINT)&inner, (sizeof(RECT)/sizeof(POINT)));

    m_insets.top = inner.top - outer.top;
    m_insets.left = inner.left - outer.left;
    m_insets.bottom = outer.bottom - inner.bottom;
    m_insets.right = outer.right - inner.right;

    if (m_insets.top < 0 || m_insets.left < 0 ||
            m_insets.bottom < 0 || m_insets.right < 0)
    {
        if (!IsDecorated()) {
            ::ZeroMemory(&m_insets, sizeof(m_insets));
        } else {
            if (GetStyle() & WS_THICKFRAME) {
                m_insets.left = m_insets.right =
                    ::GetSystemMetrics(SM_CXSIZEFRAME);
                m_insets.top = m_insets.bottom =
                    ::GetSystemMetrics(SM_CYSIZEFRAME);
            } else {
                m_insets.left = m_insets.right =
                    ::GetSystemMetrics(SM_CXDLGFRAME);
                m_insets.top = m_insets.bottom =
                    ::GetSystemMetrics(SM_CYDLGFRAME);
            }

            m_insets.top += ::GetSystemMetrics(SM_CYCAPTION);
        }
        if (GetMenu()) {
            //Well, if menu wraps on multiple lines... sorry about that.
            m_insets.top += ::GetSystemMetrics(SM_CYMENU);
        }
    }
}

bool GlassWindow::SetResizable(bool resizable)
{
    LONG style = GetStyle();

    if (style & WS_CHILD) {
        return false;
    }

    LONG resizableStyle = WS_MAXIMIZEBOX;
    if (IsDecorated()) {
        resizableStyle |= WS_THICKFRAME;
    }

    if (resizable) {
        style |= resizableStyle;
    } else {
        style &= ~resizableStyle;
    }

    SetStyle(style);
    m_isResizable = resizable;

    return true;
}

/* static */ void GlassWindow::ResetGrab()
{
    if (sm_grabWindow) {
        GlassWindow *pWindow = GlassWindow::FromHandle(sm_grabWindow);
        if (pWindow) {
            pWindow->UngrabFocus();
        }
        sm_grabWindow = NULL;
    }
}

bool GlassWindow::GrabFocus()
{
    HWND hwnd = GetCurrentHWND();

    if (sm_grabWindow == hwnd) {
        // Already grabbed
        return true;
    }

    GlassWindow::ResetGrab();

    sm_grabWindow = hwnd;

    return true;
}

void GlassWindow::UngrabFocus()
{
    HWND hwnd = GetCurrentHWND();

    if (hwnd != sm_grabWindow) {
        return;
    }

    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_grefThis, javaIDs.Window.notifyFocusUngrab);
    CheckAndClearException(env);

    sm_grabWindow = NULL;
}

void GlassWindow::CheckUngrab()
{
    if (!sm_grabWindow) {
        return;
    }

    // If this window doesn't belong to an owned windows hierarchy that
    // holds the grab currently, then the grab should be released.
    // Fix RT-16490: use GetAncestor() instead of ::GetParent() to support embedded windows
    for (BaseWnd * window = this; window != NULL; window = BaseWnd::FromHandle(window->GetAncestor())) {
        if (window->GetHWND() == sm_grabWindow) {
            return;
        }
    }

    GlassWindow::ResetGrab();
}

bool GlassWindow::RequestFocus(jint event)
{
    if (!IsChild()) {
        ASSERT(event == com_sun_glass_events_WindowEvent_FOCUS_GAINED);
        // The event will be delivered as a part of WM_ACTIVATE message handling
        return ::SetForegroundWindow(GetHWND()) != FALSE;
    }

    if (event == com_sun_glass_events_WindowEvent_FOCUS_LOST) {
        if (IsFocused()) {
            ::SetFocus(NULL);
        }

        return true;
    }

    // First try to activate the toplevel window
    HWND toplevel = ::GetAncestor(GetHWND(), GA_ROOT);
    if (::GetForegroundWindow() != toplevel && !::SetForegroundWindow(toplevel)) {
        // We're unable to bring our top-level window to foreground.
        // But since it anyway becomes active, we (or the plugin) won't receive
        // any subsequent notifications. So let's pretend we got the focus - 
        //IGNORE: return false;
        //We'll anyway get a reasonable response from the ::SetFocus() later
    }

    m_focusEvent = event; // reset upon WM_SETFOCUS

    // If we request focus from 'nowhere', the SetFocus may still return NULL I guess
    return ::SetFocus(GetHWND()) != NULL || ::GetLastError() == 0;
}

static BOOL CALLBACK EnumChildWndProc(HWND hwnd, LPARAM lParam)
{
    HWND * hwnds = (HWND*)lParam;

    ::SetParent(hwnd, hwnds[1]);

    BaseWnd * window = BaseWnd::FromHandle(hwnd);
    if (window) {
        window->SetAncestor(hwnds[1]);
    }

    return TRUE;
}

static BOOL CALLBACK EnumOwnedWndProc(HWND hwnd, LPARAM lParam)
{
    HWND * hwnds = (HWND*)lParam;

    GlassWindow * window = NULL;
    if ((HWND)::GetWindowLongPtr(hwnd, GWLP_HWNDPARENT) == hwnds[0] && (window = GlassWindow::FromHandle(hwnd)) != NULL) {
        ::SetWindowLongPtr(hwnd, GWLP_HWNDPARENT, (LONG_PTR)hwnds[1]);
        window->SetAncestor(hwnds[1]);
        ::SetWindowPos(hwnd, hwnds[1], 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_FRAMECHANGED | SWP_NOACTIVATE);
    }

    return TRUE;
}

void GlassWindow::SetDelegateWindow(HWND hWnd)
{
    if (m_delegateWindow == hWnd) {
        return;
    }

    // Make sure any popups are hidden
    UngrabFocus();

    HWND hwnds[2]; // [0] = from; [1] = to;

    hwnds[0] = m_delegateWindow ? m_delegateWindow : GetHWND();
    hwnds[1] = hWnd ? hWnd : GetHWND();

    STRACE(_T("SetDelegateWindow: from %p to %p"), hwnds[0], hwnds[1]);

    // Reparent child, and then owned windows
    ::EnumChildWindows(hwnds[0], &EnumChildWndProc, (LPARAM)&hwnds);
    ::EnumThreadWindows(GlassApplication::GetMainThreadId(), &EnumOwnedWndProc, (LPARAM)&hwnds);

    m_delegateWindow = hWnd;

    GetEnv()->CallVoidMethod(m_grefThis,
            javaIDs.Window.notifyDelegatePtr, (jlong)hWnd);
    CheckAndClearException(GetEnv());
}

BOOL GlassWindow::EnterFullScreenMode(GlassView * view, BOOL animate, BOOL keepRatio)
{
    if (IsChild()) {
        return FALSE;
    }
    if (IsInFullScreenMode()) {
        return TRUE;
    }
    if (view != GetGlassView()) {
        STRACE(_T("EnterFullScreenMode(view = %p) while the real view for this window is: %p"), view, GetGlassView());
        return FALSE;
    }

    static const LONG FS_STYLE_MASK = WS_CAPTION | WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_BORDER | WS_THICKFRAME;
    static const LONG FS_EXSTYLE_MASK = WS_EX_WINDOWEDGE;

    LONG style = ::GetWindowLong(GetHWND(), GWL_STYLE);
    LONG exStyle = ::GetWindowLong(GetHWND(), GWL_EXSTYLE);

    ::GetWindowRect(GetHWND(), &m_beforeFullScreenRect);
    m_beforeFullScreenStyle = style & FS_STYLE_MASK;
    m_beforeFullScreenExStyle = exStyle & FS_EXSTYLE_MASK;
    m_beforeFullScreenMenu = ::GetMenu(GetHWND());

    RECT viewRect, screenRect, contentRect;

    FullScreenWindow::ClientRectInScreen(GetHWND(), &viewRect);
    FullScreenWindow::CalculateBounds(GetHWND(), &screenRect,
            &contentRect, keepRatio, viewRect);

    //XXX: if (keepRatio) initBlackBackground(screenRect);

    ::SetWindowLong(GetHWND(), GWL_STYLE, style & ~FS_STYLE_MASK);
    ::SetWindowLong(GetHWND(), GWL_EXSTYLE, exStyle & ~FS_EXSTYLE_MASK);

    ::SetMenu(GetHWND(), NULL);

    ::SetWindowPos(GetHWND(), HWND_TOPMOST,
            contentRect.left, contentRect.top,
            contentRect.right - contentRect.left, contentRect.bottom - contentRect.top,
            SWP_FRAMECHANGED | SWP_NOCOPYBITS);

    m_isInFullScreen = true;

    return TRUE;
}

void GlassWindow::ExitFullScreenMode(BOOL animate)
{
    if (IsChild() || !IsInFullScreenMode()) {
        return;
    }

    LONG style = ::GetWindowLong(GetHWND(), GWL_STYLE);
    LONG exStyle = ::GetWindowLong(GetHWND(), GWL_EXSTYLE);

    ::SetWindowLong(GetHWND(), GWL_STYLE, style | m_beforeFullScreenStyle);
    ::SetWindowLong(GetHWND(), GWL_EXSTYLE, exStyle | m_beforeFullScreenExStyle);

    ::SetMenu(GetHWND(), m_beforeFullScreenMenu);

    LONG swpFlags = SWP_FRAMECHANGED | SWP_NOCOPYBITS;
    if (!IsFocused()) {
        swpFlags |= SWP_NOACTIVATE;
    }
    ::SetWindowPos(GetHWND(), HWND_NOTOPMOST,
            m_beforeFullScreenRect.left, m_beforeFullScreenRect.top,
            m_beforeFullScreenRect.right - m_beforeFullScreenRect.left,
            m_beforeFullScreenRect.bottom - m_beforeFullScreenRect.top,
            swpFlags);

    m_isInFullScreen = false;
}

void GlassWindow::SetEnabled(bool enabled)
{
    if (!enabled) {
        ResetMouseTracking(GetHWND());
    }

    m_isEnabled = enabled;
}

void GlassWindow::SetIcon(HICON hIcon)
{
    ::SendMessage(GetHWND(), WM_SETICON, ICON_SMALL, (LPARAM)hIcon);
    ::SendMessage(GetHWND(), WM_SETICON, ICON_BIG, (LPARAM)hIcon);

    if (m_hIcon) {
        ::DestroyIcon(m_hIcon);
    }
    m_hIcon = hIcon;
}

/*
 * JNI methods section
 *
 */

extern "C" {

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1initIDs
    (JNIEnv *env, jclass cls)
{
    midNotifyClose = env->GetMethodID(cls, "notifyClose", "()V");
    ASSERT(midNotifyClose);

    midNotifyMove = env->GetMethodID(cls, "notifyMove", "(II)V");
    ASSERT(midNotifyMove);

    midNotifyResize = env->GetMethodID(cls, "notifyResize", "(III)V");
    ASSERT(midNotifyResize);

    javaIDs.Window.notifyFocus = env->GetMethodID(cls, "notifyFocus", "(I)V");
    ASSERT(javaIDs.Window.notifyFocus);

    javaIDs.Window.notifyFocusDisabled = env->GetMethodID(cls, "notifyFocusDisabled", "()V");
    ASSERT(javaIDs.Window.notifyFocusDisabled);

    javaIDs.Window.notifyFocusUngrab = env->GetMethodID(cls, "notifyFocusUngrab", "()V");
    ASSERT(javaIDs.Window.notifyFocusUngrab);

    midNotifyMoveToAnotherScreen = env->GetMethodID(cls, "notifyMoveToAnotherScreen", "(Lcom/sun/glass/ui/Screen;)V");
    ASSERT(midNotifyMoveToAnotherScreen);


    javaIDs.Window.notifyDestroy = env->GetMethodID(cls, "notifyDestroy", "()V");
    ASSERT(javaIDs.Window.notifyDestroy);

    javaIDs.Window.notifyDelegatePtr = env->GetMethodID(cls, "notifyDelegatePtr", "(J)V");
    ASSERT(javaIDs.Window.notifyDelegatePtr);

    javaIDs.Window.notifyInitAccessibility = env->GetMethodID(cls, "notifyInitAccessibility", "()V");
    ASSERT(javaIDs.Window.notifyInitAccessibility);
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _createWindow
 * Signature: (JJZI)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinWindow__1createWindow
    (JNIEnv *env, jobject jThis, jlong ownerPtr, jlong screenPtr, jint mask)
{
    ENTER_MAIN_THREAD_AND_RETURN(jlong)
    {
        DWORD dwStyle;
        DWORD dwExStyle;
        bool closeable;

        dwStyle = WS_CLIPCHILDREN | WS_SYSMENU;
        closeable = (mask & com_sun_glass_ui_Window_CLOSABLE) != 0;

        if (mask & com_sun_glass_ui_Window_TITLED) {
            dwExStyle = WS_EX_WINDOWEDGE;
            dwStyle |= WS_CAPTION;

            if (mask & com_sun_glass_ui_Window_MINIMIZABLE) {
                dwStyle |= WS_MINIMIZEBOX;
            }
            if (mask & com_sun_glass_ui_Window_MAXIMIZABLE) {
                dwStyle |= WS_MAXIMIZEBOX;
            }
        } else {
            dwExStyle = 0;
            dwStyle |= WS_POPUP;
        }

        if (mask & com_sun_glass_ui_Window_TRANSPARENT) {
            dwExStyle |= WS_EX_LAYERED;
        }

        if (mask & com_sun_glass_ui_Window_POPUP) {
            dwStyle |= WS_POPUP;
            // Popups should not appear in the taskbar, so WS_EX_TOOLWINDOW
            dwExStyle |= WS_EX_TOOLWINDOW;
        }

        if (mask & com_sun_glass_ui_Window_UTILITY) {
            dwExStyle |= WS_EX_TOOLWINDOW;
        }

        if (mask & com_sun_glass_ui_Window_RIGHT_TO_LEFT) {
            dwExStyle |= WS_EX_NOINHERITLAYOUT | WS_EX_LAYOUTRTL;
        }

        GlassWindow *pWindow =
            new GlassWindow(jThis,
                (mask & com_sun_glass_ui_Window_TRANSPARENT) != 0,
                (mask & com_sun_glass_ui_Window_TITLED) != 0,
                (mask & com_sun_glass_ui_Window_UNIFIED) != 0,
                false,
                owner);

        HWND hWnd = pWindow->Create(dwStyle, dwExStyle, hMonitor, owner);

        if (!hWnd) {
            delete pWindow;
        } else {
            if (!closeable) {
                HMENU hSysMenu = ::GetSystemMenu(hWnd, FALSE);
                if (hSysMenu != NULL) {
                    ::EnableMenuItem(hSysMenu, SC_CLOSE,
                            MF_BYCOMMAND | MF_DISABLED | MF_GRAYED);
                }
            }
        }

        return (jlong)hWnd;
    }
    DECL_jobject(jThis);
    HWND owner;
    HMONITOR hMonitor;
    jint mask;
    LEAVE_MAIN_THREAD;

    ARG(jThis) = jThis;
    ARG(owner) = (HWND)ownerPtr;
    ARG(hMonitor) = (HMONITOR)screenPtr;
    ARG(mask) = mask;

    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _createChildWindow
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinWindow__1createChildWindow
    (JNIEnv *env, jobject jThis, jlong parentPtr)
{
    ENTER_MAIN_THREAD_AND_RETURN(jlong)
    {
        // Check that the 'parent' isn't a garbage value
        if (!::IsWindow((HWND)parent)) {
            return (jlong)0;
        }

        DWORD dwStyle;
        DWORD dwExStyle;

        dwStyle = WS_CLIPCHILDREN | WS_CLIPSIBLINGS | WS_CHILD;
        dwExStyle = WS_EX_NOINHERITLAYOUT;

        GlassWindow *pWindow =
            new GlassWindow(jThis, false, false, false, true, parent);

        HWND hWnd = pWindow->Create(dwStyle, dwExStyle, NULL, parent);

        if (!hWnd) {
            delete pWindow;
        }

        return (jlong)hWnd;
    }
    DECL_jobject(jThis);
    HWND parent;
    LEAVE_MAIN_THREAD;

    ARG(jThis) = jThis;
    ARG(parent) = (HWND)parentPtr;

    return PERFORM_AND_RETURN();
}
/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _close
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1close
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        pWindow->Close();
        return bool_to_jbool(::DestroyWindow(hWnd));
    }
    LEAVE_MAIN_THREAD_WITH_hWnd;

    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setView
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1setView
    (JNIEnv * env, jobject jThis, jlong ptr, jobject view)
{
    ENTER_MAIN_THREAD()
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        pWindow->ResetMouseTracking(hWnd);
        pWindow->SetGlassView(view);
        // The condition below may be restricted to WS_POPUP windows
        if (::IsWindowVisible(hWnd)) {
            pWindow->NotifyViewSize(hWnd);
        }
    }
    GlassView * view;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(view) = view == NULL ? NULL : (GlassView*)env->GetLongField(view, javaIDs.View.ptr);

    PERFORM();
    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setMenubar
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1setMenubar
    (JNIEnv *env, jobject jThis, jlong ptr, jlong menuPtr)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        if (::SetMenu(hWnd, hMenu))
        {
            GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
            if (pWindow) {
                pWindow->SetMenu(hMenu);
            }

            return JNI_TRUE;
        }
        return JNI_FALSE;
    }
    HMENU hMenu;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(hMenu) = (HMENU)menuPtr;
    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setLevel
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1setLevel
(JNIEnv *env, jobject jwindow, jlong ptr, jint jLevel)
{
    ENTER_MAIN_THREAD()
    {
        ::SetWindowPos(hWnd, hWndInsertAfter, 0, 0, 0, 0,
                SWP_ASYNCWINDOWPOS | SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOOWNERZORDER | SWP_NOSIZE);
    }
    HWND hWndInsertAfter;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(hWndInsertAfter) = HWND_NOTOPMOST;
    switch (jLevel) {
        case com_sun_glass_ui_Window_Level_FLOATING:
        case com_sun_glass_ui_Window_Level_TOPMOST:
            ARG(hWndInsertAfter) = HWND_TOPMOST;
            break;
    }
    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setFocusable
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1setFocusable
(JNIEnv *env, jobject jwindow, jlong ptr, jboolean isFocusable)
{
    ENTER_MAIN_THREAD()
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        pWindow->SetFocusable(isFocusable);
    }
    bool isFocusable;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(isFocusable) = isFocusable == JNI_TRUE;
    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setEnabled
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1setEnabled
(JNIEnv *env, jobject jwindow, jlong ptr, jboolean isEnabled)
{
    ENTER_MAIN_THREAD()
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        pWindow->SetEnabled(isEnabled);
        ::EnableWindow(hWnd, isEnabled);
    }
    bool isEnabled;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(isEnabled) = isEnabled == JNI_TRUE;
    PERFORM();
}

// Converts a float [0..1] to a BYTE [0..255]
#define F2B(value) BYTE(255.f * (value))

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setAlpha
 * Signature: (JF)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1setAlpha
    (JNIEnv *env, jobject jThis, jlong ptr, jfloat alpha)
{
    ENTER_MAIN_THREAD()
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        pWindow->SetAlpha(alpha);
    }
    BYTE alpha;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(alpha) = F2B(alpha);
    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setBackground
 * Signature: (JFFF)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1setBackground
    (JNIEnv *env, jobject jThis, jlong ptr, jfloat r, jfloat g, jfloat b)
{
    ENTER_MAIN_THREAD()
    {
        HBRUSH hbrBackground;

        // That's a hack with 'negative' color
        if (r < 0) {
            hbrBackground = NULL;
        } else {
            hbrBackground = ::CreateSolidBrush(RGB(F2B(r), F2B(g), F2B(b)));
        }

        HBRUSH oldBrush = (HBRUSH)::SetClassLongPtr(hWnd, GCLP_HBRBACKGROUND, (LONG_PTR)hbrBackground);

        if (oldBrush) {
            ::DeleteObject(oldBrush);
        }
    }
    jfloat r, g, b;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(r) = r;
    ARG(g) = g;
    ARG(b) = b;
    PERFORM();

    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setBounds
 * Signature: (JIIZZIIIIFF)Z
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1setBounds
    (JNIEnv *env, jobject jThis, jlong ptr,
     jint x, jint y, jboolean xSet, jboolean ySet,
     jint w, jint h, jint cw, jint ch,
     jfloat xGravity, jfloat yGravity)
{
    ENTER_MAIN_THREAD()
    {
        if (!::IsWindow(hWnd)) return;
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);

        pWindow->UpdateInsets();
        RECT is = pWindow->GetInsets();

        RECT r;
        ::GetWindowRect(hWnd, &r);

        int newX = jbool_to_bool(xSet) ? x : r.left;
        int newY = jbool_to_bool(ySet) ? y : r.top;
        int newW = w > 0 ? w :
                       cw > 0 ? cw + is.right + is.left : r.right - r.left;
        int newH = h > 0 ? h :
                       ch > 0 ? ch + is.bottom + is.top : r.bottom - r.top;

        if (xSet || ySet) {
            ::SetWindowPos(hWnd, NULL, newX, newY, newW, newH,
                           SWP_NOACTIVATE | SWP_NOZORDER);
        } else {
            ::SetWindowPos(hWnd, NULL, 0, 0, newW, newH,
                           SWP_NOACTIVATE | SWP_NOZORDER | SWP_NOMOVE);
        }
    }
    jint x, y;
    jboolean xSet, ySet;
    jint w, h, cw, ch;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(x) = x;
    ARG(y) = y;
    ARG(xSet) = xSet;
    ARG(ySet) = ySet;
    ARG(w) = w;
    ARG(h) = h;
    ARG(cw) = cw;
    ARG(ch) = ch;
    PERFORM();

}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setTitle
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1setTitle
    (JNIEnv *env, jobject jThis, jlong ptr, jstring jTitle)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        if (::SetWindowText(hWnd, title)) {
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }
    LPCTSTR title;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    JString title(env, jTitle);
    ARG(title) = title;
    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setResizable
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1setResizable
(JNIEnv *env, jobject jWindow, jlong ptr, jboolean jResizable)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        if (pWindow && pWindow->SetResizable(jbool_to_bool(jResizable))) {
            return JNI_TRUE;
        }

        return JNI_FALSE;
    }
    jboolean jResizable;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(jResizable) = jResizable;
    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setVisible
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1setVisible
    (JNIEnv *env, jobject jThis, jlong ptr, jboolean visible)
{
    ENTER_MAIN_THREAD()
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        if (!visible) {
            if (pWindow) {
                pWindow->UngrabFocus();
            }
        }
        
        ::ShowWindow(hWnd, visible ? SW_SHOW : SW_HIDE);
        
        if (visible) {
            if (pWindow) {
                if (pWindow->IsFocusable()) {
                    ::SetForegroundWindow(hWnd);
                } else {
                    // RT-14197:
                    // On some latest platform versions, unfocusable windows
                    // are shown below the currently active window, so we
                    // need to pull them to front explicitly. However,
                    // neither BringWindowToTop nor SetForegroundWindow()
                    // can be used because of the window unfocusability, so
                    // here is a workaround: we first made the window TOPMOST
                    // and then reset this flag to just TOP.
                    ::SetWindowPos(hWnd, HWND_TOPMOST, 0, 0, 0, 0,
                                   SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE);
                    ::SetWindowPos(hWnd, HWND_TOP, 0, 0, 0, 0,
                                   SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE);
                }
            }
            ::UpdateWindow(hWnd);
        }
    }
    jboolean visible;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(visible) = visible;
    PERFORM();
    return visible;
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _requestFocus
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1requestFocus
    (JNIEnv *env, jobject jThis, jlong ptr, jint event)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        return bool_to_jbool(pWindow && pWindow->RequestFocus(event));
    }
    jint event;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(event) = event;

    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _grabFocus
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1grabFocus
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        return bool_to_jbool(pWindow && pWindow->GrabFocus());
    }
    LEAVE_MAIN_THREAD_WITH_hWnd;

    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _ungrabFocus
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1ungrabFocus
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    ENTER_MAIN_THREAD()
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        if (pWindow) {
            pWindow->UngrabFocus();
        }
    }
    LEAVE_MAIN_THREAD_WITH_hWnd;

    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _minimize
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1minimize
    (JNIEnv *env, jobject jThis, jlong ptr, jboolean minimize)
{
    ENTER_MAIN_THREAD()
    {
        ::ShowWindow(hWnd, minimize ? SW_MINIMIZE : SW_RESTORE);
    }
    jboolean minimize;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(minimize) = minimize;
    PERFORM();

    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _maximize
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1maximize
  (JNIEnv *env, jobject jThis, jlong ptr, jboolean maximize, jboolean wasMaximized)
{
    ENTER_MAIN_THREAD()
    {
        ::ShowWindow(hWnd, maximize ? SW_MAXIMIZE : SW_RESTORE);
    }
    jboolean maximize;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(maximize) = maximize;
    PERFORM();

    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setMinimumSize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1setMinimumSize
    (JNIEnv *env, jobject jThis, jlong ptr, jint minWidth, jint minHeight)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        if (pWindow) {
            pWindow->setMinSize(minWidth, minHeight);
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }
    jint minWidth;
    jint minHeight;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(minWidth) = minWidth == 0 ? -1 : minWidth;
    ARG(minHeight) = minHeight == 0 ? -1 : minHeight;
    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setMaximumSize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1setMaximumSize
    (JNIEnv *env, jobject jThis, jlong ptr, jint maxWidth, jint maxHeight)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        if (pWindow) {
            pWindow->setMaxSize(maxWidth, maxHeight);
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }
    jint maxWidth;
    jint maxHeight;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(maxWidth) = maxWidth;
    ARG(maxHeight) = maxHeight;
    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setIcon
 * Signature: (JLcom/sun/glass/ui/Pixels;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1setIcon
    (JNIEnv *env, jobject jThis, jlong ptr, jobject jPixels)
{
    HWND hWnd = (HWND)ptr;
    GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
    if (pWindow) {
        pWindow->SetIcon(!jPixels ? NULL : Pixels::CreateIcon(env, jPixels));
    }
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _toFront
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1toFront
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    ENTER_MAIN_THREAD()
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        // See comment in __1setVisible() above about unfocusable windows
        if (pWindow && !pWindow->IsFocusable()) {
            ::SetWindowPos(hWnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
        }
        ::SetWindowPos(hWnd, HWND_TOP, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
    }
    LEAVE_MAIN_THREAD_WITH_hWnd;

    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _toBack
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1toBack
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    ENTER_MAIN_THREAD()
    {
        ::SetWindowPos(hWnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
    }
    LEAVE_MAIN_THREAD_WITH_hWnd;

    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _getEmbeddedX
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinWindow__1getEmbeddedX
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    ENTER_MAIN_THREAD_AND_RETURN(jint)
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        HWND delegateHWnd = pWindow ? pWindow->GetDelegateWindow() : 0;
        RECT rect = {0};
        ::MapWindowPoints(delegateHWnd ? delegateHWnd : hWnd, (HWND)NULL, (LPPOINT)&rect, (sizeof(RECT)/sizeof(POINT)));
        return rect.left;
    }
    LEAVE_MAIN_THREAD_WITH_hWnd;

    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _getEmbeddedY
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinWindow__1getEmbeddedY
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    ENTER_MAIN_THREAD_AND_RETURN(jint)
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        HWND delegateHWnd = pWindow ? pWindow->GetDelegateWindow() : 0;
        RECT rect = {0};
        ::MapWindowPoints(delegateHWnd ? delegateHWnd : hWnd, (HWND)NULL, (LPPOINT)&rect, (sizeof(RECT)/sizeof(POINT)));
        return rect.top;
    }
    LEAVE_MAIN_THREAD_WITH_hWnd;

    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _setCursor
 * Signature: (Lcom/sun/glass/ui/Cursor;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1setCursor
    (JNIEnv *env, jobject jThis, jlong ptr, jobject jCursor)
{
    ENTER_MAIN_THREAD()
    {
        const HCURSOR cursor = JCursorToHCURSOR(GetEnv(), jCursor);

        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        if (pWindow) {
            pWindow->SetCursor(cursor);

            // Update the delegate window as well if present
            HWND delegateHwnd = pWindow->GetDelegateWindow();
            if (delegateHwnd) {
                BaseWnd *pDelegateWindow = BaseWnd::FromHandle(delegateHwnd);
                if (pDelegateWindow) {
                    pDelegateWindow->SetCursor(cursor);
                }
            }
        }
    }
    DECL_jobject(jCursor);
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(jCursor) = jCursor;
    PERFORM();
}

}   // extern "C"
