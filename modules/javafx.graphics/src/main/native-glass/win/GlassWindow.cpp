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
#include "GlassWindow.h"
#include "GlassScreen.h"
#include "GlassMenu.h"
#include "GlassView.h"
#include "GlassDnD.h"

#include "com_sun_glass_events_WindowEvent.h"
#include "com_sun_glass_ui_Window.h"
#include "com_sun_glass_ui_win_WinWindow.h"

#define ABM_GETAUTOHIDEBAREX 0x0000000b // multimon aware autohide bars

#define HTUNSPECIFIED ('H' << 24 | 'T' << 16 | 'U' << 8 | 'N') // see WinWindow.java:nonClientHitTest

static LPCTSTR szGlassWindowClassName = TEXT("GlassWindowClass");

unsigned int GlassWindow::sm_instanceCounter = 0;
HHOOK GlassWindow::sm_hCBTFilter = NULL;
HWND GlassWindow::sm_grabWindow = NULL;
static HWND activeTouchWindow = NULL;

/*
 * ---- The callback table of glass_win_api.h's window section ----
 *
 * Installed by gwin_window_set_callbacks. A slot Java leaves NULL is replaced by the matching no-op
 * below, so the 11 upcall sites in this file and ~GlassWindow dial slots without testing them; what
 * they test is WindowCallbacks() returning NULL, which means "nothing installed, deliver nothing".
 */
namespace {

void NoopNotifyClose(int64_t) {}
void NoopNotifyDestroy(int64_t) {}
int32_t NoopNotifyMoving(int64_t, int32_t, int32_t, int32_t, int32_t, float, float, int32_t, int32_t,
                         int32_t, int32_t, int32_t, int32_t, int32_t, int32_t*) { return 0; }
void NoopNotifyMove(int64_t, int32_t, int32_t) {}
void NoopNotifyResize(int64_t, int32_t, int32_t, int32_t) {}
void NoopNotifyScaleChanged(int64_t, float, float, float, float) {}
void NoopNotifyFocus(int64_t, int32_t) {}
void NoopNotifyFocusDisabled(int64_t) {}
void NoopNotifyFocusUngrab(int64_t) {}
void NoopNotifyDelegatePtr(int64_t, void*) {}
int32_t NoopNonClientHitTest(int64_t, int32_t, int32_t) { return 0; }
void NoopNotifyDispose(int64_t) {}

const GwinWindowCallbacks NOOP_WINDOW_CALLBACKS = {
    NoopNotifyClose, NoopNotifyDestroy, NoopNotifyMoving, NoopNotifyMove, NoopNotifyResize,
    NoopNotifyScaleChanged, NoopNotifyFocus, NoopNotifyFocusDisabled, NoopNotifyFocusUngrab,
    NoopNotifyDelegatePtr, NoopNonClientHitTest, NoopNotifyDispose
};

} // namespace

GwinWindowCallbacks GlassWindow::sm_windowCallbacks = NOOP_WINDOW_CALLBACKS;
bool GlassWindow::sm_windowCallbacksInstalled = false;

/* By value - this library never retains the caller's struct - and a NULL slot keeps its no-op.
 * Installed once, before any window can exist; not safe against a concurrent install - the flag is a
 * plain bool and the table a plain struct assignment, with no release store between them. */
void GlassWindow::SetWindowCallbacks(const GwinWindowCallbacks* cb)
{
    GwinWindowCallbacks t = NOOP_WINDOW_CALLBACKS;
    if (cb != NULL) {
        if (cb->notify_close) t.notify_close = cb->notify_close;
        if (cb->notify_destroy) t.notify_destroy = cb->notify_destroy;
        if (cb->notify_moving) t.notify_moving = cb->notify_moving;
        if (cb->notify_move) t.notify_move = cb->notify_move;
        if (cb->notify_resize) t.notify_resize = cb->notify_resize;
        if (cb->notify_scale_changed) t.notify_scale_changed = cb->notify_scale_changed;
        if (cb->notify_focus) t.notify_focus = cb->notify_focus;
        if (cb->notify_focus_disabled) t.notify_focus_disabled = cb->notify_focus_disabled;
        if (cb->notify_focus_ungrab) t.notify_focus_ungrab = cb->notify_focus_ungrab;
        if (cb->notify_delegate_ptr) t.notify_delegate_ptr = cb->notify_delegate_ptr;
        if (cb->non_client_hit_test) t.non_client_hit_test = cb->non_client_hit_test;
        if (cb->notify_dispose) t.notify_dispose = cb->notify_dispose;
    }
    sm_windowCallbacksInstalled = false;
    sm_windowCallbacks = t;
    sm_windowCallbacksInstalled = cb != NULL;
}

GlassWindow::GlassWindow(bool isTransparent, bool isDecorated, bool isUnified,
                         bool isExtended, HWND parentOrOwner, int64_t windowId)
    : BaseWnd(parentOrOwner),
    m_windowId(windowId),
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

    // Java's registry entry for m_windowId goes exactly where the JNI global ref used to go
    // (glass_win_api.h, GwinWindowCallbacks.notify_dispose).
    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        cb->notify_dispose(m_windowId);
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

#ifndef USER_DEFAULT_SCREEN_DPI
#define USER_DEFAULT_SCREEN_DPI 96
#endif

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
        case WM_DISPLAYCHANGE: return "WM_DISPLAYCHANGE";
        case WM_SETTINGCHANGE: return "WM_SETTINGCHANGE";
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
        case WM_DPICHANGED:
            HandleDPIEvent(wParam, lParam);
            GlassScreen::HandleDisplayChange();
            break;
        case WM_MOVING:
            m_winChangingReason = WasMoved;
            break;
        case WM_DISPLAYCHANGE:
        case WM_SETTINGCHANGE:
          // Trigger a move event to notify the stage about possible changes in the window location
          // (for instance, if this is a secondary screen and the primary screen changed its resolution,
          // or if the screens relative position was rearranged)
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

    if (m_isExtended && HandleCaptionMouseEvents(msg, wParam, lParam)) {
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

/*
 * This method is only called for windows with the EXTENDED style to handle a press-drag-release interaction
 * on the title bar, which will move the window. Ordinarily, this wouldn't be required at all, since correctly
 * responding to the WM_NCHITTEST message tells Windows which part of our window it should treat as the title
 * bar area; this automatically enables the press-drag-release interaction.
 *
 * However, there's a small problem with serious consequences: Windows will send us a WM_NCLBUTTONDOWN message
 * when the mouse button is pressed on the title bar, but it will NOT send us a WM_NCLBUTTONUP message when the
 * button is released again. Pressing the mouse button on the title bar area starts a move-modal message loop,
 * and the corresponding WM_LBUTTONUP (NB: not WM_NCLBUTTONUP) event is handled inside of this modal loop;
 * it doesn't arrive at the message loop in this class where we could react to it. This is a problem because
 * JavaFX has its own press-drag-release detection in the Scene class, and this missing release event messes
 * up its state.
 *
 * So, how can we still get a notification when the mouse button was released on the title bar? For future
 * reference, here are potential solutions that don't work well for various reasons:
 *
 * 1. Listen to WM_ENTERSIZEMOVE/WM_EXITSIZEMOVE, which are posted at the beginning and the end of a PDR
 *    interaction. Unfortunately, a single short click on the title bar doesn't count as a PDR interaction,
 *    and none of the messages are posted. Interestingly, a single click with about a one-second delay between
 *    press and release does count as a PDR interaction.
 *
 * 2. Install a WH_MOUSE hook to intercept mouse messages before they are posted to the move-modal message loop,
 *    and use it to discover the "missing" WM_LBUTTONUP message. While this works, some anti-virus tools might
 *    dislike us using the SetWindowsHookEx API.
 *
 * What seems to work best is to not rely on Windows to start the move-modal message loop for us, but to start
 * it ourselves. When it returns, we know that the mouse button was released. The prerequisite for this solution
 * is that we never tell Windows that the cursor is on the title bar area, as Windows would then invariably start
 * the move-modal message loop. So every time we determine in HandleNCHitTestEvent() that the cursor is on the
 * title bar, we tell Windows that it's HTCLIENT instead.
 */
bool GlassWindow::HandleCaptionMouseEvents(UINT msg, WPARAM wParam, LPARAM lParam)
{
    switch (msg) {
        // Pressing the left mouse button on the title bar area initializes PDR tracking.
        // We don't consume this message, so it will be handled as usual in HandleViewMouseEvent()
        // to generate a normal JavaFX event. Note that HandleViewMouseEvent() calls SetCapture().
        case WM_LBUTTONDOWN:
            if (m_caption.entered) {
                m_caption.tracking = true;
                m_caption.dragStart = { GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam) };
            }
            break;

        // Releasing the left mouse button without moving the cursor cancels PDR tracking.
        // We don't consume this message, so it will be handled as usual in HandleViewMouseEvent()
        // to generate a normal JavaFX event.
        case WM_LBUTTONUP:
            if (m_caption.tracking && GetCapture() == GetHWND()) {
                m_caption.tracking = false;
            }
            break;

        // Moving the cursor starts up a modal message loop that handles the window movement.
        // Note that we only start the window drag operation when the cursor movement exceeds the
        // minimum distance for a PDR interaction.
        case WM_MOUSEMOVE:
            if (m_caption.tracking && GetCapture() == GetHWND()) {
                POINT pt = { GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam) };
                const int dx = abs(pt.x - m_caption.dragStart.x);
                const int dy = abs(pt.y - m_caption.dragStart.y);
                const int cx = GetSystemMetrics(SM_CXDRAG);
                const int cy = GetSystemMetrics(SM_CYDRAG);

                if (dx >= cx || dy >= cy) {
                    // Start the move-modal message loop, and return when the drag operation has finished.
                    ReleaseCapture();
                    SendMessage(GetHWND(), WM_SYSCOMMAND, SC_MOVE | HTCAPTION, 0);

                    // Cancel tracking and synthesize a WM_LBUTTONUP event.
                    m_caption.tracking = false;
                    HandleViewMouseEvent(GetHWND(), WM_LBUTTONUP, wParam, lParam, true);
                }

                // Always consume WM_MOUSEMOVE when we're in a PDR interaction.
                return true;
            }
            break;

        // Cancel tracking when an in-progress PDR interaction is canceled.
        case WM_CANCELMODE:
        case WM_CAPTURECHANGED:
            m_caption.tracking = false;
            break;
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
    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        cb->notify_close(m_windowId);
    }
}

void GlassWindow::HandleDestroyEvent()
{
    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        cb->notify_destroy(m_windowId);
    }
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

// The tail of HandleWindowPosChangingEvent once Java has answered with four ints (the out-parameter
// of GwinWindowCallbacks.notify_moving). Verbatim from the former JNI block.
static void ApplyMovingOverride(WINDOWPOS *pWinPos, const int32_t ret[4], uint8_t noMove, uint8_t noSize)
{
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
}

void GlassWindow::HandleWindowPosChangingEvent(WINDOWPOS *pWinPos)
{
//    fprintf(stdout, "WM_POSCHANGING enter: (%d, %d), [%d x %d]",
//            pWinPos->x, pWinPos->y, pWinPos->cx, pWinPos->cy);
//    printFlags(pWinPos->flags);
//    fprintf(stdout, "\n");
//    fflush(stdout);

    int32_t resizeMode = (m_winChangingReason == WasSized)
            ? com_sun_glass_ui_win_WinWindow_RESIZE_DISABLE
            : com_sun_glass_ui_win_WinWindow_RESIZE_AROUND_ANCHOR;
    m_winChangingReason = Unknown;

    uint8_t noMove = ((pWinPos->flags & SWP_NOMOVE) != 0);
    uint8_t noSize = ((pWinPos->flags & SWP_NOSIZE) != 0);
    // Only evaluate bounds if they have changed...
    if (noMove && noSize) return;

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

    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        // The jintArray became an out-parameter; the "bad array length" branch is unreachable here.
        // The two floats were dead int literals in the JNI varargs call (see glass_win_api.h).
        int32_t ret[4];
        if (cb->notify_moving(m_windowId, pWinPos->x, pWinPos->y, pWinPos->cx, pWinPos->cy,
                              0.0f, 0.0f, anchor.x, anchor.y, resizeMode,
                              m_insets.left, m_insets.top, m_insets.right, m_insets.bottom,
                              ret)) {
            ApplyMovingOverride(pWinPos, ret, noMove, noSize);
        }
    }
}

// if pRect == NULL => get position/size by GetWindowRect
void GlassWindow::HandleMoveEvent(RECT *pRect)
{
    RECT r;
    if (pRect == NULL) {
        ::GetWindowRect(GetHWND(), &r);
        pRect = &r;
    }

    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        cb->notify_move(m_windowId, pRect->left, pRect->top);
    }
}

// if pRect == NULL => get position/size by GetWindowRect
void GlassWindow::HandleSizeEvent(int type, RECT *pRect)
{
    RECT r;
    if (pRect == NULL) {
        ::GetWindowRect(GetHWND(), &r);
        pRect = &r;
    }

    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        cb->notify_resize(m_windowId, type, pRect->right-pRect->left, pRect->bottom-pRect->top);
    }
}

void GlassWindow::HandleDPIEvent(WPARAM wParam, LPARAM lParam)
{
    float scale = (float) LOWORD(wParam) / USER_DEFAULT_SCREEN_DPI;

    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        cb->notify_scale_changed(m_windowId, scale, scale, scale, scale);
    }
}

void GlassWindow::HandleActivateEvent(int32_t event)
{
    const bool active = event != com_sun_glass_events_WindowEvent_FOCUS_LOST;

    if (!active) {
        UngrabFocus();
    }

    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        cb->notify_focus(m_windowId, event);
    }
}

void GlassWindow::HandleFocusDisabledEvent()
{
    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        cb->notify_focus_disabled(m_windowId);
    }
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

    int32_t res = HTNOWHERE;   // no table: what a failed upcall answered
    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        res = cb->non_client_hit_test(m_windowId, pt.x, pt.y);
    }

    // The left, right, and bottom resize borders are outside of the client area and are provided for free.
    // In contrast, the top resize border is not outside, but inside the client area and may be below user
    // controls. For example, if a control with HeaderDragType.NONE extends to the top of the client area,
    // it covers the resize border at that location. We know that the cursor is on top of the caption area
    // (and not on top of a control) when the nonClientHitTest() function returns HTCAPTION instead of
    // HTCLIENT. In this case, we apply the default resize border.
    // Note that HTUNSPECIFIED is a custom JavaFX classification that means the cursor is on the HeaderBar,
    // but no user-specified HeaderDragType is known. In this case, we treat the current cursor position as
    // HTTOP if the cursor is on the top resize border, and as HTCLIENT otherwise.
    if (res == HTCAPTION || res == HTUNSPECIFIED) {
        // Note: there is no SM_CYPADDEDBORDER
        int topBorderHeight = ::GetSystemMetrics(SM_CXPADDEDBORDER) + ::GetSystemMetrics(SM_CYSIZEFRAME);
        RECT windowRect;

        if (m_isResizable && ::GetWindowRect(GetHWND(), &windowRect) && y < windowRect.top + topBorderHeight) {
            res = HTTOP;
        } else if (res == HTUNSPECIFIED) {
            res = HTCLIENT;
        }
    }

    // If the cursor is on the title bar, lie to Windows and say it's on the client area instead.
    // See GlassWindow::HandleCaptionMouseEvents() for more information.
    m_caption.entered = res == HTCAPTION;
    if (res == HTCAPTION) {
        res = HTCLIENT;
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

    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        cb->notify_focus_ungrab(m_windowId);
    }

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

bool GlassWindow::RequestFocus(int32_t event)
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

    if (const GwinWindowCallbacks* cb = WindowCallbacks()) {
        cb->notify_delegate_ptr(m_windowId, (void*)hWnd);
    }
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
 * ---- The action bodies of the gwin_window_* exports of glass_win_api.cpp (the JNI entry
 * points that once shared them are gone). Each runs INSIDE its caller's ENTER_MAIN_THREAD
 * action. ----
 */

/* gwin_window_create's action, verbatim from the former Java_..._1createWindow: windowId is the
 * IDENTITY of glass_win_api.h. */
/* static */ HWND GlassWindow::CreateFromMask(int64_t windowId, HWND owner,
                                              HMONITOR hMonitor, int32_t mask)
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
        new GlassWindow(
            (mask & com_sun_glass_ui_Window_TRANSPARENT) != 0,
            (mask & com_sun_glass_ui_Window_TITLED) != 0,
            (mask & com_sun_glass_ui_Window_UNIFIED) != 0,
            (mask & com_sun_glass_ui_Window_EXTENDED) != 0,
            owner, windowId);

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

    return hWnd;
}

/* The set_view action, verbatim from the former Java_..._1setView (no null check of the GlassWindow). */
/* static */ void GlassWindow::DoSetView(HWND hWnd, GlassView* view)
{
    GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);

    if (activeTouchWindow == hWnd) {
        activeTouchWindow = 0;
    }
    pWindow->ResetMouseTracking(hWnd);
    pWindow->SetGlassView(view);
}

/* The set_visible action, verbatim from the former Java_..._1setVisible (the touch tail does not null-check). */
/* static */ void GlassWindow::DoSetVisible(HWND hWnd, bool visible)
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

/* The tail of gwin_window_set_cursor's action: this window, then the delegate window if present. */
void GlassWindow::ApplyCursor(HCURSOR cursor)
{
    SetCursor(cursor);

    // Update the delegate window as well if present
    HWND delegateHwnd = GetDelegateWindow();
    if (delegateHwnd) {
        BaseWnd *pDelegateWindow = BaseWnd::FromHandle(delegateHwnd);
        if (pDelegateWindow) {
            pDelegateWindow->SetCursor(cursor);
        }
    }
}
