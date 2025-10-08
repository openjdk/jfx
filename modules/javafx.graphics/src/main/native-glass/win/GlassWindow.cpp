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

#include "common.h"

#include "FullScreenWindow.h"
#include "GlassApplication.h"
#include "GlassWindow.h"
#include "GlassScreen.h"
#include "GlassMenu.h"
#include "GlassView.h"
#include "GlassDnD.h"
#include "Pixels.h"
#include "GlassCursor.h"
#include "GlassScreen.h"

#include "com_sun_glass_events_WindowEvent.h"
#include "com_sun_glass_ui_Window.h"
#include "com_sun_glass_ui_Window_Level.h"
#include "com_sun_glass_ui_win_WinWindow.h"

#define ABM_GETAUTOHIDEBAREX 0x0000000b // multimon aware autohide bars

// Helper LEAVE_MAIN_THREAD for GlassWindow
#define LEAVE_MAIN_THREAD_WITH_hWnd  \
    HWND hWnd;  \
    LEAVE_MAIN_THREAD;  \
    ARG(hWnd) = (HWND)ptr;

static LPCTSTR szGlassWindowClassName = TEXT("GlassWindowClass");

static jmethodID midNotifyClose;
static jmethodID midNotifyMoving;
static jmethodID midNotifyMove;
static jmethodID midNotifyResize;
static jmethodID midNotifyScaleChanged;
static jmethodID midNotifyMoveToAnotherScreen;

unsigned int GlassWindow::sm_instanceCounter = 0;
HHOOK GlassWindow::sm_hCBTFilter = NULL;
HWND GlassWindow::sm_grabWindow = NULL;
static HWND activeTouchWindow = NULL;

GlassWindow::GlassWindow(jobject jrefThis, bool isTransparent, bool isDecorated, bool isUnified,
                         bool isExtended, HWND parentOrOwner)
    : BaseWnd(parentOrOwner),
    ViewContainer(),
    m_winChangingReason(Unknown),
    m_state(Normal),
    m_isFocusable(true),
    m_isFocused(false),
    m_focusEvent(0),
    m_isResizable(true),
    m_isTransparent(isTransparent),
    m_isDecorated(isDecorated),
    m_isUnified(isUnified),
    m_isExtended(isExtended),
    m_hMenu(NULL),
    m_alpha(255),
    m_isEnabled(true),
    m_delegateWindow(NULL),
    m_isInFullScreen(false),
    m_beforeFullScreenStyle(0),
    m_beforeFullScreenExStyle(0),
    m_beforeFullScreenMenu(NULL),
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
}

GlassWindow::~GlassWindow()
{
    if (m_hIcon) {
        ::DestroyIcon(m_hIcon);
    }

    if (m_grefThis) {
        GetEnv()->DeleteGlobalRef(m_grefThis);
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

#ifndef WM_DPICHANGED
#define WM_DPICHANGED       0x02E0
#endif

char *StringForMsg(UINT msg) {
    switch (msg) {
        case WM_DPICHANGED: return "WM_DPICHANGED";
        case WM_ERASEBKGND: return "WM_ERASEBKGND";
        case WM_NCPAINT: return "WM_NCPAINT";
        case WM_SETCURSOR: return "WM_SETCURSOR";
        case WM_NCMOUSEMOVE: return "WM_NCMOUSEMOVE";
        case WM_NCHITTEST: return "WM_NCHITTEST";
        case WM_NCMOUSELEAVE: return "WM_NCMOUSELEAVE";
        case WM_ENTERSIZEMOVE: return "WM_ENTERSIZEMOVE";
        case WM_EXITSIZEMOVE: return "WM_EXITSIZEMOVE";
        case WM_CREATE: return "WM_CREATE";
        case WM_NCDESTROY: return "WM_NCDESTROY";
        case WM_STYLECHANGED: return "WM_STYLECHANGED";
        case WM_STYLECHANGING: return "WM_STYLECHANGING";
        case WM_GETICON: return "WM_GETICON";
        case WM_SETICON: return "WM_SETICON";
        case WM_ACTIVATEAPP: return "WM_ACTIVATEAPP";
        case WM_NCACTIVATE: return "WM_NCACTIVATE";
        case WM_IME_SETCONTEXT: return "WM_IME_SETCONTEXT";
        case WM_SETTEXT: return "WM_SETTEXT";
        case WM_DWMNCRENDERINGCHANGED: return "WM_DWMNCRENDERINGCHANGED";
        case WM_SYSCOMMAND: return "WM_SYSCOMMAND";

        case WM_SHOWWINDOW: return "WM_SHOWWINDOW";
        case WM_DWMCOMPOSITIONCHANGED: return "WM_DWMCOMPOSITIONCHANGED";
        case WM_SIZING: return "WM_SIZING";
        case WM_SIZE: return "WM_SIZE";
        case WM_MOVING: return "WM_MOVING";
        case WM_MOVE: return "WM_MOVE";
        case WM_WINDOWPOSCHANGING: return "WM_WINDOWPOSCHANGING";
        case WM_WINDOWPOSCHANGED: return "WM_WINDOWPOSCHANGED";
        case WM_CLOSE: return "WM_CLOSE";
        case WM_DESTROY: return "WM_DESTROY";
        case WM_ACTIVATE: return "WM_ACTIVATE";
        case WM_MOUSEACTIVATE: return "WM_MOUSEACTIVATE";
        case WM_SETFOCUS: return "WM_SETFOCUS";
        case WM_KILLFOCUS: return "WM_KILLFOCUS";
        case WM_GETMINMAXINFO: return "WM_GETMINMAXINFO";
        case WM_COMMAND: return "WM_COMMAND";
        case WM_INPUTLANGCHANGE: return "WM_INPUTLANGCHANGE";
        case WM_NCCALCSIZE: return "WM_NCCALCSIZE";
        case WM_PAINT: return "WM_PAINT";
        case WM_CONTEXTMENU: return "WM_CONTEXTMENU";
        case WM_LBUTTONDOWN: return "WM_LBUTTONDOWN";
        case WM_RBUTTONDOWN: return "WM_RBUTTONDOWN";
        case WM_MBUTTONDOWN: return "WM_MBUTTONDOWN";
        case WM_XBUTTONDOWN: return "WM_XBUTTONDOWN";
        case WM_LBUTTONUP: return "WM_LBUTTONUP";
        case WM_LBUTTONDBLCLK: return "WM_LBUTTONDBLCLK";
        case WM_RBUTTONUP: return "WM_RBUTTONUP";
        case WM_RBUTTONDBLCLK: return "WM_RBUTTONDBLCLK";
        case WM_MBUTTONUP: return "WM_MBUTTONUP";
        case WM_MBUTTONDBLCLK: return "WM_MBUTTONDBLCLK";
        case WM_XBUTTONUP: return "WM_XBUTTONUP";
        case WM_XBUTTONDBLCLK: return "WM_XBUTTONDBLCLK";
        case WM_MOUSEWHEEL: return "WM_MOUSEWHEEL";
        case WM_MOUSEHWHEEL: return "WM_MOUSEHWHEEL";
        case WM_MOUSELEAVE: return "WM_MOUSELEAVE";
        case WM_MOUSEMOVE: return "WM_MOUSEMOVE";
        case WM_CAPTURECHANGED: return "WM_CAPTURECHANGED";
        case WM_SYSKEYDOWN: return "WM_SYSKEYDOWN";
        case WM_SYSKEYUP: return "WM_SYSKEYUP";
        case WM_KEYDOWN: return "WM_KEYDOWN";
        case WM_KEYUP: return "WM_KEYUP";
        case WM_DEADCHAR: return "WM_DEADCHAR";
        case WM_CHAR: return "WM_CHAR";
        case WM_IME_CHAR: return "WM_IME_CHAR";
        case WM_IME_COMPOSITION: return "WM_IME_COMPOSITION";
        case WM_IME_ENDCOMPOSITION: return "WM_IME_ENDCOMPOSITION";
        case WM_IME_NOTIFY: return "WM_IME_NOTIFY";
        case WM_IME_STARTCOMPOSITION: return "WM_IME_STARTCOMPOSITION";
        case WM_NCLBUTTONDOWN: return "WM_NCLBUTTONDOWN";
        case WM_NCMBUTTONDOWN: return "WM_NCMBUTTONDOWN";
        case WM_NCRBUTTONDOWN: return "WM_NCRBUTTONDOWN";
        case WM_NCXBUTTONDOWN: return "WM_NCXBUTTONDOWN";
        case WM_TOUCH: return "WM_TOUCH";
        case WM_TIMER: return "WM_TIMER";
        case WM_GETOBJECT: return "WM_GETOBJECT";
    }
    return "Unknown";
}

LRESULT GlassWindow::WindowProc(UINT msg, WPARAM wParam, LPARAM lParam)
{
//    fprintf(stdout, "msg = 0x%04x (%s)\n", msg, StringForMsg(msg));
//    fflush(stdout);
    MessageResult commonResult = BaseWnd::CommonWindowProc(msg, wParam, lParam);
    if (commonResult.processed) {
//        fprintf(stdout, "   (handled by CommonWindowProc)\n");
//        fflush(stdout);
        return commonResult.result;
    }

    switch (msg) {
        case WM_SHOWWINDOW:
            // It's possible that move/size events are reported by the platform
            // before the peer listener is set. As a result, location/size are
            // not reported, so resending them from here.
            if (!::IsIconic(GetHWND())) {
                HandleMoveEvent(NULL);
                HandleSizeEvent(com_sun_glass_events_WindowEvent_RESIZE, NULL);
                // The call below may be restricted to WS_POPUP windows
                NotifyViewSize(GetHWND());
            }

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
        case WM_SIZING:
            m_winChangingReason = WasSized;
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
        case WM_MOVING:
            m_winChangingReason = WasMoved;
            break;
        case WM_MOVE:
            if (!::IsIconic(GetHWND())) {
                HandleMoveEvent(NULL);
            }
            break;
        case WM_WINDOWPOSCHANGING:
            HandleWindowPosChangingEvent((WINDOWPOS *)lParam);
            break;
        case WM_CLOSE:
            HandleCloseEvent();
            return 0;
        case WM_DESTROY:
            HandleDestroyEvent();
            return 0;
        case WM_ACTIVATE:
            {
                // The fActive shouldn't be WA_INACTIVE && the window shouldn't be minimized:
                const bool isFocusGained = LOWORD(wParam) != WA_INACTIVE && HIWORD(wParam) == 0;

                if (IsInFullScreenMode()) {
                    HWND hWndInsertAfter = isFocusGained ? HWND_TOPMOST : HWND_BOTTOM;
                    ::SetWindowPos(GetHWND(), hWndInsertAfter, 0, 0, 0, 0,
                            SWP_ASYNCWINDOWPOS | SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOOWNERZORDER | SWP_NOSIZE);
                }
                if (!GetDelegateWindow()) {
                    HandleActivateEvent(isFocusGained ?
                            com_sun_glass_events_WindowEvent_FOCUS_GAINED :
                            com_sun_glass_events_WindowEvent_FOCUS_LOST);
                }
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
            }
            break;
        case WM_KILLFOCUS:
            if (!GetDelegateWindow()) {
                SetFocused(false);
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
// Workaround for JDK-8112996. It has some side effects and thus commented out
//            if ((BOOL)wParam && !IsDecorated()) {
//                NCCALCSIZE_PARAMS *p = (NCCALCSIZE_PARAMS *)lParam;
//                p->rgrc[0].right++;
//                p->rgrc[0].bottom++;
//                return WVR_VALIDRECTS;
//            }

            if (BOOL(wParam) && m_isExtended) {
                return HandleNCCalcSizeEvent(msg, wParam, lParam);
            }
            break;
        case WM_NCHITTEST: {
            LRESULT res;
            if (m_isExtended && HandleNCHitTestEvent(GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam), res)) {
                return res;
            }
            break;
        }
        case WM_PAINT:
            HandleViewPaintEvent(GetHWND(), msg, wParam, lParam);
            break;
        case WM_CONTEXTMENU:
            HandleViewMenuEvent(GetHWND(), msg, wParam, lParam);
            break;
        case WM_LBUTTONDOWN:
        case WM_RBUTTONDOWN:
        case WM_MBUTTONDOWN:
        case WM_XBUTTONDOWN:
            CheckUngrab(); // check if other owned windows hierarchy holds the grab
            // ... and fall through for other mouse events
        case WM_LBUTTONUP:
        case WM_LBUTTONDBLCLK:
        case WM_RBUTTONUP:
        case WM_RBUTTONDBLCLK:
        case WM_MBUTTONUP:
        case WM_MBUTTONDBLCLK:
        case WM_XBUTTONUP:
        case WM_XBUTTONDBLCLK:
        case WM_MOUSEWHEEL:
        case WM_MOUSEHWHEEL:
        case WM_MOUSELEAVE:
        case WM_MOUSEMOVE:
            if (!IsEnabled()) {
                HandleFocusDisabledEvent();
                return 0;
            } else if (HandleMouseEvents(msg, wParam, lParam)) {
                return 0;
            }
            break;
        case WM_CAPTURECHANGED:
            ViewContainer::NotifyCaptureChanged(GetHWND(), (HWND)lParam);
            break;
        case WM_MENUCHAR:
            // Stop the beep when missing mnemonic or accelerator key JDK-8089986
            return MNC_CLOSE << 16;
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
        case WM_DEADCHAR:
            if (IsEnabled()) HandleViewDeadKeyEvent(GetHWND(), msg, wParam, lParam);
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

            if (m_isExtended) {
                HandleNonClientMouseEvents(msg, wParam, lParam);

                // We need to return 0 for clicks on the min/max/close regions, as otherwise Windows will
                // draw very ugly buttons on top of our window.
                if (wParam == HTMINBUTTON || wParam == HTMAXBUTTON || wParam == HTCLOSE) {
                    return 0;
                }
            }

            // Pass the event to DefWindowProc()
            break;
        case WM_NCLBUTTONUP:
        case WM_NCLBUTTONDBLCLK:
        case WM_NCRBUTTONUP:
        case WM_NCRBUTTONDBLCLK:
        case WM_NCMBUTTONUP:
        case WM_NCMBUTTONDBLCLK:
        case WM_NCXBUTTONUP:
        case WM_NCXBUTTONDBLCLK:
        case WM_NCMOUSELEAVE:
        case WM_NCMOUSEMOVE:
            if (m_isExtended) {
                HandleNonClientMouseEvents(msg, wParam, lParam);

                if (wParam == HTMINBUTTON || wParam == HTMAXBUTTON || wParam == HTCLOSE) {
                    return 0;
                }
            }
            break;
        case WM_TOUCH:
            if (IsEnabled()) {
                if (activeTouchWindow == 0 || activeTouchWindow == GetHWND()) {
                    if(HandleViewTouchEvent(GetHWND(), msg, wParam, lParam) > 0) {
                        activeTouchWindow = GetHWND();
                    } else {
                        activeTouchWindow = 0;
                    }
                }
                return 0;
            }
            break;
        case WM_TIMER:
            HandleViewTimerEvent(GetHWND(), wParam);
            return 0;
        case WM_GETOBJECT: {
            LRESULT lr = HandleViewGetAccessible(GetHWND(), wParam, lParam);
            if (lr) return lr;
            break;
        }
    }

    return ::DefWindowProc(GetHWND(), msg, wParam, lParam);
}

bool GlassWindow::HandleMouseEvents(UINT msg, WPARAM wParam, LPARAM lParam)
{
    if (msg == WM_MOUSELEAVE && GetDelegateWindow()) {
        // Skip generating MouseEvent.EXIT when entering FullScreen
        return true;
    }

    BOOL handled = HandleViewMouseEvent(GetHWND(), msg, wParam, lParam, m_isExtended);
    if (handled && msg == WM_RBUTTONUP) {
        // By default, DefWindowProc() sends WM_CONTEXTMENU from WM_LBUTTONUP
        // Since DefWindowProc() is not called, call the mouse menu handler directly
        HandleViewMenuEvent(GetHWND(), WM_CONTEXTMENU, (WPARAM) GetHWND(), ::GetMessagePos());
        //::DefWindowProc(GetHWND(), msg, wParam, lParam);
    }

    if (handled) {
        // Do not call the DefWindowProc() for mouse events that were handled
        return true;
    }

    return false;
}

void GlassWindow::HandleNonClientMouseEvents(UINT msg, WPARAM wParam, LPARAM lParam)
{
    HandleViewNonClientMouseEvent(GetHWND(), msg, wParam, lParam);
    LRESULT result;

    // If the right mouse button was released on a HTCAPTION area, we synthesize a WM_CONTEXTMENU event.
    // This allows JavaFX applications to respond to context menu events in the non-client header bar area.
    if (msg == WM_NCRBUTTONUP
            && HandleNCHitTestEvent(GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam), result)
            && result == HTCAPTION) {
        HandleViewMenuEvent(GetHWND(), WM_CONTEXTMENU, (WPARAM)GetHWND(), ::GetMessagePos());
    }
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

#if 0
#define _PFLAG(name) \
    if ((flags & SWP_ ## name) != 0) { \
        fprintf(stdout, ", " #name); \
        flags &= ~SWP_ ## name; \
    }
void printFlags(int flags)
{
    _PFLAG(NOMOVE);
    _PFLAG(NOSIZE);
    _PFLAG(DRAWFRAME);
    _PFLAG(FRAMECHANGED);
    _PFLAG(HIDEWINDOW);
    _PFLAG(NOACTIVATE);
    _PFLAG(NOCOPYBITS);
    _PFLAG(NOOWNERZORDER);
    _PFLAG(NOREDRAW);
    _PFLAG(NOREPOSITION);
    _PFLAG(NOSENDCHANGING);
    _PFLAG(NOZORDER);
    _PFLAG(SHOWWINDOW);
    if (flags != 0) {
        fprintf(stdout, ", unrecognized flags = 0x%08x", flags);
    }
}
#undef _PFLAG
#endif

void GlassWindow::HandleWindowPosChangingEvent(WINDOWPOS *pWinPos)
{
//    fprintf(stdout, "WM_POSCHANGING enter: (%d, %d), [%d x %d]",
//            pWinPos->x, pWinPos->y, pWinPos->cx, pWinPos->cy);
//    printFlags(pWinPos->flags);
//    fprintf(stdout, "\n");
//    fflush(stdout);

    jint resizeMode = (m_winChangingReason == WasSized)
            ? com_sun_glass_ui_win_WinWindow_RESIZE_DISABLE
            : com_sun_glass_ui_win_WinWindow_RESIZE_AROUND_ANCHOR;
    m_winChangingReason = Unknown;

    jboolean noMove = ((pWinPos->flags & SWP_NOMOVE) != 0);
    jboolean noSize = ((pWinPos->flags & SWP_NOSIZE) != 0);
    // Only evaluate bounds if they have changed...
    if (noMove && noSize) return;

    JNIEnv* env = GetEnv();
    HWND hWnd = GetHWND();

    POINT anchor;
    if (hWnd == ::GetCapture()) {
        if (::GetCursorPos(&anchor)) {
            anchor.x -= pWinPos->x;
            anchor.y -= pWinPos->y;
        } else {
            anchor.x = anchor.y = 0;
        }
    } else {
        anchor.x = anchor.y = 0;
    }

    if (noMove || noSize) {
        RECT wBounds;
        ::GetWindowRect(hWnd, &wBounds);
        if (noMove) {
            pWinPos->x = wBounds.left;
            pWinPos->y = wBounds.top;
        }
        if (noSize) {
            pWinPos->cx = wBounds.right - wBounds.left;
            pWinPos->cy = wBounds.bottom - wBounds.top;
        }
    }

    UpdateInsets();

    jintArray jret = (jintArray) env->CallObjectMethod(m_grefThis, midNotifyMoving,
                                                       pWinPos->x, pWinPos->y,
                                                       pWinPos->cx, pWinPos->cy,
                                                       0, 0, anchor.x, anchor.y,
                                                       resizeMode,
                                                       m_insets.left, m_insets.top,
                                                       m_insets.right, m_insets.bottom);
    if (CheckAndClearException(env)) {
//        fprintf(stderr, "Exception from upcall");
    } else if (jret == NULL) {
//        fprintf(stdout, "ret val is null\n");
//        fflush(stdout);
    } else {
        if (env->GetArrayLength(jret) != 4) {
            fprintf(stderr, "bad array length = %d\n", env->GetArrayLength(jret));
        } else {
            jint ret[4];
            env->GetIntArrayRegion(jret, 0, 4, ret);
            if (!CheckAndClearException(env)) {
                if (noMove &&
                    (pWinPos->x != ret[0] ||
                     pWinPos->y != ret[1]))
                {
                    pWinPos->flags &= ~SWP_NOMOVE;
                }
                pWinPos->x  = ret[0];
                pWinPos->y  = ret[1];
                if (noSize &&
                    (pWinPos->cx != ret[2] ||
                     pWinPos->cy != ret[3]))
                {
                    pWinPos->flags &= ~SWP_NOSIZE;
                }
                pWinPos->cx = ret[2];
                pWinPos->cy = ret[3];
//                fprintf(stdout, "WM_POSCHANGING override: (%d, %d), [%d x %d]",
//                        pWinPos->x, pWinPos->y, pWinPos->cx, pWinPos->cy);
//                printFlags(pWinPos->flags);
//                fprintf(stdout, "\n");
//                fflush(stdout);
            }
        }
        env->DeleteLocalRef(jret);
    }
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

LRESULT GlassWindow::HandleNCCalcSizeEvent(UINT msg, WPARAM wParam, LPARAM lParam)
{
    // Capture the top before DefWindowProc applies the default frame.
    NCCALCSIZE_PARAMS *p = (NCCALCSIZE_PARAMS*)lParam;
    LONG originalTop = p->rgrc[0].top;

    // Apply the default window frame.
    LRESULT res = DefWindowProc(GetHWND(), msg, wParam, lParam);
    if (res != 0) {
        return res;
    }

    // Restore the original top, which might have been overwritten by DefWindowProc.
    RECT newSize = p->rgrc[0];
    newSize.top = originalTop;

    // A maximized window extends slightly beyond the screen, so we need to account for that
    // by adding the border width to the top.
    bool maximized = (::GetWindowLong(GetHWND(), GWL_STYLE) & WS_MAXIMIZE) != 0;
    if (maximized && !m_isInFullScreen) {
        // Note: there is no SM_CYPADDEDBORDER
        newSize.top += ::GetSystemMetrics(SM_CXPADDEDBORDER) + ::GetSystemMetrics(SM_CYSIZEFRAME);
    }

    // If we have an auto-hide taskbar, we need to reduce the size of a maximized or fullscreen
    // window a little bit where the taskbar is located, as otherwise the taskbar cannot be
    // summoned.
    HMONITOR monitor = ::MonitorFromWindow(GetHWND(), MONITOR_DEFAULTTONEAREST);
    if (monitor && (maximized || m_isInFullScreen)) {
        MONITORINFO monitorInfo = { 0 };
        monitorInfo.cbSize = sizeof(MONITORINFO);
        ::GetMonitorInfo(monitor, &monitorInfo);

        APPBARDATA data = { 0 };
        data.cbSize = sizeof(data);

        if ((::SHAppBarMessage(ABM_GETSTATE, &data) & ABS_AUTOHIDE) == ABS_AUTOHIDE) {
            data.rc = monitorInfo.rcMonitor;
            DWORD appBarMsg = ::IsWindows8OrGreater() ? ABM_GETAUTOHIDEBAREX : ABM_GETAUTOHIDEBAR;

            // Reduce the window size by one pixel on the taskbar side.
            if ((data.uEdge = ABE_TOP), ::SHAppBarMessage(appBarMsg, &data) != NULL) {
                newSize.top += 1;
            } else if ((data.uEdge = ABE_BOTTOM), ::SHAppBarMessage(appBarMsg, &data) != NULL) {
                newSize.bottom -= 1;
            } else if ((data.uEdge = ABE_LEFT), ::SHAppBarMessage(appBarMsg, &data) != NULL) {
                newSize.left += 1;
            } else if ((data.uEdge = ABE_RIGHT), ::SHAppBarMessage(appBarMsg, &data) != NULL) {
                newSize.right -= 1;
            }
        }
    }

    p->rgrc[0] = newSize;
    return 0;
}

// Handling this message tells Windows which parts of the window are non-client regions.
// This enables window behaviors like dragging or snap layouts.
BOOL GlassWindow::HandleNCHitTestEvent(SHORT x, SHORT y, LRESULT& result)
{
    if (::DefWindowProc(GetHWND(), WM_NCHITTEST, 0, MAKELONG(x, y)) != HTCLIENT) {
        return FALSE;
    }

    POINT pt = { x, y };

    if (!::ScreenToClient(GetHWND(), &pt)) {
        return FALSE;
    }

    // Unmirror the X coordinate we send to JavaFX if this is a RTL window.
    LONG style = ::GetWindowLong(GetHWND(), GWL_EXSTYLE);
    if (style & WS_EX_LAYOUTRTL) {
        RECT rect = {0};
        ::GetClientRect(GetHWND(), &rect);
        pt.x = max(0, rect.right - rect.left) - pt.x;
    }

    JNIEnv* env = GetEnv();
    jint res = env->CallIntMethod(m_grefThis, javaIDs.WinWindow.nonClientHitTest, pt.x, pt.y);
    CheckAndClearException(env);

    // The left, right, and bottom resize borders are outside of the client area and are provided for free.
    // In contrast, the top resize border is not outside, but inside the client area and below user controls.
    // For example, if a control extends to the top of the client area, it covers the resize border at that
    // location. We know that the cursor is on top of the caption area (and not on top of a control) when
    // the nonClientHitTest() function returns HTCAPTION (instead of HTCLIENT). In this case, we apply the
    // default resize border.
    if (res == HTCAPTION) {
        // Note: there is no SM_CYPADDEDBORDER
        int topBorderHeight = ::GetSystemMetrics(SM_CXPADDEDBORDER) + ::GetSystemMetrics(SM_CYSIZEFRAME);
        RECT windowRect;

        if (m_isResizable && ::GetWindowRect(GetHWND(), &windowRect) && y < windowRect.top + topBorderHeight) {
            result = LRESULT(HTTOP);
            return TRUE;
        }
    }

    result = LRESULT(res);
    return TRUE;
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
    // Fix JDK-8128445: use GetAncestor() instead of ::GetParent() to support embedded windows
    for (BaseWnd * window = this; window != NULL; window = BaseWnd::FromHandle(window->GetAncestor())) {
        if (window->GetHWND() == sm_grabWindow) {
            return;
        }
    }

    GlassWindow::ResetGrab();
}

bool GlassWindow::RequestFocus(jint event)
{
    ASSERT(event == com_sun_glass_events_WindowEvent_FOCUS_GAINED);
    // The event will be delivered as a part of WM_ACTIVATE message handling
    return ::SetForegroundWindow(GetHWND()) != FALSE;
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
    if (!IsInFullScreenMode()) {
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

void GlassWindow::SetDarkFrame(bool dark)
{
    // The value of the DWMWA_USE_IMMERSIVE_DARK_MODE constant may be different depending on the OS version.
    // We are going to query the file version of dwmapi.dll to make sure we use the right constant, or the
    // value 0 to indicate that we don't support this feature.
    // See: https://github.com/MicrosoftDocs/sdk-api/commit/c19f1c8a148b930444dce998d3c717c8fb7751e1
    static const DWORD DWMWA_USE_IMMERSIVE_DARK_MODE = []() {
        DWORD ignored;
        DWORD infoSize = GetFileVersionInfoSizeExW(FILE_VER_GET_NEUTRAL, L"dwmapi.dll", &ignored);
        if (infoSize <= 0) {
            return 0;
        }

        std::vector<char> buffer(infoSize);
        if (!GetFileVersionInfoExW(FILE_VER_GET_NEUTRAL, L"dwmapi.dll", ignored,
                                   static_cast<DWORD>(buffer.size()), &buffer[0])) {
            return 0;
        }

        UINT size = 0;
        VS_FIXEDFILEINFO* fileInfo = nullptr;
        if (!VerQueryValueW(buffer.data(), L"\\", reinterpret_cast<LPVOID*>(&fileInfo), &size)) {
            return 0;
        }

        WORD major = HIWORD(fileInfo->dwFileVersionMS);
        WORD minor = LOWORD(fileInfo->dwFileVersionMS);
        WORD build = HIWORD(fileInfo->dwFileVersionLS);

        // Windows 10 before build 10.0.17763: not supported
        if (major < 10 || (major == 10 && minor == 0 && build < 17763)) {
            return 0;
        }

        // Windows 10 build 10.0.17763 until 10.0.18985
        if (major == 10 && minor == 0 && build >= 17763 && build < 18985) {
            return 19;
        }

        // Windows 10 build 10.0.18985 or later
        return 20;
    }();

    if (DWMWA_USE_IMMERSIVE_DARK_MODE) {
        BOOL darkMode = dark;
        DwmSetWindowAttribute(GetHWND(), DWMWA_USE_IMMERSIVE_DARK_MODE, &darkMode, sizeof(darkMode));
    }
}

void GlassWindow::ShowSystemMenu(int x, int y)
{
    WINDOWPLACEMENT placement;
    if (!::GetWindowPlacement(GetHWND(), &placement)) {
        return;
    }

    // Mirror the X coordinate we get from JavaFX if this is a RTL window.
    LONG exStyle = ::GetWindowLong(GetHWND(), GWL_EXSTYLE);
    if (exStyle & WS_EX_LAYOUTRTL) {
        RECT rect = {0};
        ::GetClientRect(GetHWND(), &rect);
        x = max(0, rect.right - rect.left) - x;
    }

    HMENU systemMenu = GetSystemMenu(GetHWND(), FALSE);
    bool maximized = placement.showCmd == SW_SHOWMAXIMIZED;

    LONG style = ::GetWindowLong(GetHWND(), GWL_STYLE);
    bool canMinimize = (style & WS_MINIMIZEBOX) && !(exStyle & WS_EX_TOOLWINDOW);
    bool canMaximize = (style & WS_MAXIMIZEBOX) && !maximized;

    MENUITEMINFO menuItemInfo { sizeof(MENUITEMINFO) };
    menuItemInfo.fMask = MIIM_STATE;
    menuItemInfo.fType = MFT_STRING;

    menuItemInfo.fState = maximized ? MF_ENABLED : MF_DISABLED;
    SetMenuItemInfo(systemMenu, SC_RESTORE, FALSE, &menuItemInfo);

    menuItemInfo.fState = maximized ? MF_DISABLED : MF_ENABLED;
    SetMenuItemInfo(systemMenu, SC_MOVE, FALSE, &menuItemInfo);

    menuItemInfo.fState = !m_isResizable || maximized ? MF_DISABLED : MF_ENABLED;
    SetMenuItemInfo(systemMenu, SC_SIZE, FALSE, &menuItemInfo);

    menuItemInfo.fState = canMinimize ? MF_ENABLED : MF_DISABLED;
    SetMenuItemInfo(systemMenu, SC_MINIMIZE, FALSE, &menuItemInfo);

    menuItemInfo.fState = canMaximize ? MF_ENABLED : MF_DISABLED;
    SetMenuItemInfo(systemMenu, SC_MAXIMIZE, FALSE, &menuItemInfo);

    menuItemInfo.fState = MF_ENABLED;
    SetMenuItemInfo(systemMenu, SC_CLOSE, FALSE, &menuItemInfo);
    SetMenuDefaultItem(systemMenu, UINT_MAX, FALSE);

    POINT ptAbs = { x, y };
    ::ClientToScreen(GetHWND(), &ptAbs);

    BOOL menuItem = TrackPopupMenu(systemMenu, TPM_RETURNCMD, ptAbs.x, ptAbs.y, 0, GetHWND(), NULL);
    if (menuItem != 0) {
        PostMessage(GetHWND(), WM_SYSCOMMAND, menuItem, 0);
    }
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
     if (env->ExceptionCheck()) return;

     midNotifyMoving = env->GetMethodID(cls, "notifyMoving", "(IIIIFFIIIIIII)[I");
     ASSERT(midNotifyMoving);
     if (env->ExceptionCheck()) return;

     midNotifyMove = env->GetMethodID(cls, "notifyMove", "(II)V");
     ASSERT(midNotifyMove);
     if (env->ExceptionCheck()) return;

     midNotifyResize = env->GetMethodID(cls, "notifyResize", "(III)V");
     ASSERT(midNotifyResize);
     if (env->ExceptionCheck()) return;

     midNotifyScaleChanged = env->GetMethodID(cls, "notifyScaleChanged", "(FFFF)V");
     ASSERT(midNotifyScaleChanged);
     if (env->ExceptionCheck()) return;

     javaIDs.Window.notifyFocus = env->GetMethodID(cls, "notifyFocus", "(I)V");
     ASSERT(javaIDs.Window.notifyFocus);
     if (env->ExceptionCheck()) return;

     javaIDs.Window.notifyFocusDisabled = env->GetMethodID(cls, "notifyFocusDisabled", "()V");
     ASSERT(javaIDs.Window.notifyFocusDisabled);
     if (env->ExceptionCheck()) return;

     javaIDs.Window.notifyFocusUngrab = env->GetMethodID(cls, "notifyFocusUngrab", "()V");
     ASSERT(javaIDs.Window.notifyFocusUngrab);
     if (env->ExceptionCheck()) return;

     midNotifyMoveToAnotherScreen = env->GetMethodID(cls, "notifyMoveToAnotherScreen", "(Lcom/sun/glass/ui/Screen;)V");
     ASSERT(midNotifyMoveToAnotherScreen);
     if (env->ExceptionCheck()) return;

     javaIDs.Window.notifyDestroy = env->GetMethodID(cls, "notifyDestroy", "()V");
     ASSERT(javaIDs.Window.notifyDestroy);
     if (env->ExceptionCheck()) return;

     javaIDs.Window.notifyDelegatePtr = env->GetMethodID(cls, "notifyDelegatePtr", "(J)V");
     ASSERT(javaIDs.Window.notifyDelegatePtr);
     if (env->ExceptionCheck()) return;

     javaIDs.WinWindow.nonClientHitTest = env->GetMethodID(cls, "nonClientHitTest", "(II)I");
     ASSERT(javaIDs.WinWindow.nonClientHitTest);
     if (env->ExceptionCheck()) return;
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

        if (mask & com_sun_glass_ui_Window_EXTENDED) {
            mask |= com_sun_glass_ui_Window_TITLED;
        }

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
            // if undecorated or transparent and not modal, enable taskbar iconification toggling
            if (!(mask & com_sun_glass_ui_Window_MODAL)) {
                dwStyle |= WS_MINIMIZEBOX;
            }
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
                (mask & com_sun_glass_ui_Window_EXTENDED) != 0,
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

            if (mask & com_sun_glass_ui_Window_DARK_FRAME) {
                pWindow->SetDarkFrame(true);
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

        if (activeTouchWindow == hWnd) {
            activeTouchWindow = 0;
        }
        pWindow->ResetMouseTracking(hWnd);
        pWindow->SetGlassView(view);
    }
    GlassView * view;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(view) = view == NULL ? NULL : (GlassView*)env->GetLongField(view, javaIDs.View.ptr);

    PERFORM();
    return JNI_TRUE;
}

/**
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _updateViewSize
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1updateViewSize
    (JNIEnv * env, jobject jThis, jlong ptr)
{
    ENTER_MAIN_THREAD()
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);

        // The condition below may be restricted to WS_POPUP windows
        if (::IsWindowVisible(hWnd)) {
            pWindow->NotifyViewSize(hWnd);
        }
    }
    LEAVE_MAIN_THREAD_WITH_hWnd;

    PERFORM();
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
 * Method:    _setBackground2
 * Signature: (JFFF)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinWindow__1setBackground2
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
 * Method:    _setDarkFrame
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1setDarkFrame
    (JNIEnv *env, jobject jThis, jlong ptr, jboolean dark)
{
    ENTER_MAIN_THREAD()
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        if (pWindow) {
            pWindow->SetDarkFrame(dark);
        }
    }
    jboolean dark;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(dark) = dark;
    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _getAnchor
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinWindow__1getAnchor
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    HWND hWnd = (HWND) ptr;
    if (!::IsWindow(hWnd)) return 0L;

    RECT wRect;
    POINT anchor;
    if (hWnd == ::GetCapture()) {
        if (::GetCursorPos(&anchor) && ::GetWindowRect(hWnd, &wRect)) {
            anchor.x -= wRect.left;
            anchor.y -= wRect.top;
            return ((((jlong) anchor.x) << 32) |
                    (((jlong) anchor.y) & 0xffffffffL));
        }
    }
    return com_sun_glass_ui_win_WinWindow_ANCHOR_NO_CAPTURE;
}

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _getInsets
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinWindow__1getInsets
    (JNIEnv *env, jobject jThis, jlong ptr)
{
    HWND hWnd = (HWND) ptr;
    if (!::IsWindow(hWnd)) return 0L;
    GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);

    pWindow->UpdateInsets();
    RECT is = pWindow->GetInsets();

    return ((((jlong) is.left)  << 48) |
            (((jlong) is.top)   << 32) |
            (((jlong) is.right) << 16) |
            (((jlong) is.bottom)     ));
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

        POINT minSize = pWindow->getMinSize();
        POINT maxSize = pWindow->getMaxSize();
        if (minSize.x >= 0) newW = max(newW, minSize.x);
        if (minSize.y >= 0) newH = max(newH, minSize.y);
        if (maxSize.x >= 0) newW = min(newW, maxSize.x);
        if (maxSize.y >= 0) newH = min(newH, maxSize.y);

        if (xSet || ySet) {
            ::SetWindowPos(hWnd, NULL, newX, newY, newW, newH,
                           SWP_NOACTIVATE | SWP_NOZORDER | SWP_NOSENDCHANGING);
        } else {
            ::SetWindowPos(hWnd, NULL, 0, 0, newW, newH,
                           SWP_NOACTIVATE | SWP_NOZORDER | SWP_NOMOVE | SWP_NOSENDCHANGING);
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

            if (activeTouchWindow == hWnd) {
                pWindow->HandleViewTouchEvent(hWnd, 0, 0, 0);
                activeTouchWindow = 0;
            }
        }


        ::ShowWindow(hWnd, visible ? SW_SHOW : SW_HIDE);

        if (visible) {
            if (pWindow) {
                if (pWindow->IsFocusable()) {
                    ::SetForegroundWindow(hWnd);
                } else {
                    // JDK-8112905:
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

/*
 * Class:     com_sun_glass_ui_win_WinWindow
 * Method:    _showSystemMenu
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinWindow__1showSystemMenu
    (JNIEnv *env, jobject jThis, jlong ptr, jint x, jint y)
{
    ENTER_MAIN_THREAD()
    {
        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        if (pWindow) {
            pWindow->ShowSystemMenu(x, y);
        }
    }
    jint x, y;
    LEAVE_MAIN_THREAD_WITH_hWnd;

    ARG(x) = x;
    ARG(y) = y;
    PERFORM();
}

}   // extern "C"
