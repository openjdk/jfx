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
#include "FullScreenWindow.h"
#include "GlassView.h"
#include "GlassDnD.h"
#include "GlassWindow.h"

#include "com_sun_glass_events_WindowEvent.h"

//TODO: is that possible to move all the code to the shared level?

static LPCTSTR szFullScreenWindowClassName = TEXT("FullScreenWindowClass");
static LPCTSTR szBackgroundWindowClassName = TEXT("BackgroundWindowClass");

static const UINT ANIMATION_MAX_ITERATION = 30;
static const UINT ANIMATION_TIMER_ELAPSE = USER_TIMER_MINIMUM; // 0xA ms

FullScreenWindow::FullScreenWindow() :
    BaseWnd(),
    ViewContainer()
{
    m_animationStage = 0;
    m_bgWindow = NULL;
}

FullScreenWindow::~FullScreenWindow()
{
}

HWND FullScreenWindow::Create()
{
    m_bgWindow = new BackgroundWindow();
    m_bgWindow->Create();

    DWORD dwStyle = WS_POPUP | WS_CLIPCHILDREN;
    DWORD dwExStyle = 0;

    HWND hwnd = BaseWnd::Create(NULL, 0, 0, 0, 0,
                             TEXT(""), dwExStyle, dwStyle, NULL);

    ViewContainer::InitDropTarget(hwnd);
     ViewContainer::InitManipProcessor(hwnd);

    return hwnd;
}

void FullScreenWindow::Close()
{
    if (m_bgWindow) {
        m_bgWindow->Close();
        m_bgWindow = NULL;
    }

    ViewContainer::ReleaseDropTarget();
    ViewContainer::ReleaseManipProcessor();

    ::DestroyWindow(GetHWND());
}

/* static */
void FullScreenWindow::ClientRectInScreen(HWND hwnd, RECT * rect)
{
    ::GetClientRect(hwnd, rect);
    ::MapWindowPoints(hwnd, (HWND)NULL, (LPPOINT)rect, (sizeof(RECT)/sizeof(POINT)));
}

void FullScreenWindow::AttachView(GlassView * view, BOOL keepRatio)
{
    SetGlassView(view);

    m_oldViewParent = GetGlassView()->GetHostHwnd();

    FullScreenWindow::ClientRectInScreen(m_oldViewParent, &m_viewRect);

    InitWindowRect(keepRatio);

    GlassWindow * window = GlassWindow::FromHandle(m_oldViewParent);
    if (window) {
        window->SetDelegateWindow(GetHWND());
    }

    ::ShowWindow(m_oldViewParent, SW_HIDE);
    GetGlassView()->SetHostHwnd(GetHWND());
}

void FullScreenWindow::DetachView()
{
    RECT r;
    HWND oldWnd = m_oldViewParent;
    GlassView* view = GetGlassView();

    m_oldViewParent = NULL;

    view->SetHostHwnd(oldWnd);
    
    ::ShowWindow(oldWnd, SW_SHOW);
    ::SetForegroundWindow(oldWnd);
    ::SetFocus(oldWnd);

    GlassWindow * window = GlassWindow::FromHandle(oldWnd);
    if (window) {
        window->SetDelegateWindow(NULL);
    }

    ::GetClientRect(oldWnd, &r);
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(GetView(), javaIDs.View.notifyResize,
            r.right-r.left, r.bottom - r.top);
    CheckAndClearException(env);

    SetGlassView(NULL);
}

/* static */
void FullScreenWindow::CalculateBounds(HWND hwnd, RECT * screenRect,
        RECT * contentRect, BOOL keepRatio, const RECT & viewRect)
{
    MONITORINFOEX mix;
    HMONITOR hMonitor = ::MonitorFromWindow(hwnd, MONITOR_DEFAULTTOPRIMARY);

    memset(&mix, 0, sizeof(MONITORINFOEX));
    mix.cbSize = sizeof(MONITORINFOEX);
    ::GetMonitorInfo(hMonitor, &mix);

    ::CopyRect(screenRect, &mix.rcMonitor);
    ::CopyRect(contentRect, &mix.rcMonitor);

    if (keepRatio) {
        int viewWidth = viewRect.right - viewRect.left;
        int viewHeight = viewRect.bottom - viewRect.top;
        int screenWidth = screenRect->right - screenRect->left;
        int screenHeight = screenRect->bottom - screenRect->top;

        float ratioWidth = (float)viewWidth / (float)screenWidth;
        float ratioHeight = (float)viewHeight / (float)screenHeight;

        if (ratioWidth > ratioHeight) {
            float ratio = (float)viewWidth / (float)viewHeight;
            int height = (int)(screenWidth / ratio);
            contentRect->top += (screenHeight - height) / 2;
            contentRect->bottom = contentRect->top + height;
        } else {
            float ratio = (float)viewHeight / (float)viewWidth;
            int width = (int)(screenHeight / ratio);
            contentRect->left += (screenWidth - width) / 2;
            contentRect->right = contentRect->left + width;
        }
    }
}

void FullScreenWindow::InitWindowRect(BOOL keepRatio)
{
    RECT screenRect;

    FullScreenWindow::CalculateBounds(m_oldViewParent, &screenRect,
            &m_windowRect, keepRatio, m_viewRect);

    m_bgWindow->SetWindowRect(&screenRect);
}

void FullScreenWindow::ShowWindow(BOOL animate)
{
    m_bgWindow->ShowWindow(animate);

    RECT rect;
    if (animate) {
        CopyRect(&rect, &m_viewRect);
    } else {
        CopyRect(&rect, &m_windowRect);
    }

    ::SetWindowPos(GetHWND(), HWND_TOPMOST, rect.left, rect.top,
                    rect.right - rect.left, rect.bottom - rect.top,
                    SWP_SHOWWINDOW);
    ::SetForegroundWindow(GetHWND());
}

void FullScreenWindow::HideWindow()
{
    m_bgWindow->HideWindow();
    ::ShowWindow(GetHWND(), SW_HIDE);
}

void FullScreenWindow::HandleSizeEvent()
{
}

void FullScreenWindow::HandleViewTimerEvent(HWND hwnd, UINT_PTR timerID)
{
    switch (timerID) {
    case IDT_GLASS_ANIMATION_ENTER:
    case IDT_GLASS_ANIMATION_EXIT:
        break;
    default:
        ViewContainer::HandleViewTimerEvent(hwnd, timerID);
        return;
    }
        
    if (timerID == IDT_GLASS_ANIMATION_ENTER) {
        if (m_animationStage > ANIMATION_MAX_ITERATION) {
            StopAnimation(TRUE);
            return;
        }
    } else if (timerID == IDT_GLASS_ANIMATION_EXIT) {
        if (m_animationStage < 1) {
            StopAnimation(FALSE);
            return;
        }
    }

    m_bgWindow->UpdateAnimationOpacity(m_animationStage);
    UpdateAnimationRect();

    if (timerID == IDT_GLASS_ANIMATION_ENTER) {
        m_animationStage++;
    } else if (timerID == IDT_GLASS_ANIMATION_EXIT) {
        m_animationStage--;
    }
}

LRESULT FullScreenWindow::WindowProc(UINT msg, WPARAM wParam, LPARAM lParam)
{
    MessageResult commonResult = BaseWnd::CommonWindowProc(msg, wParam, lParam);
    if (commonResult.processed) {
        return commonResult.result;
    }

    switch (msg) {
        case WM_TIMER:
            HandleViewTimerEvent(GetHWND(), wParam);
            break;
        case WM_SIZE:
            if (wParam == SIZE_RESTORED || wParam == SIZE_MAXIMIZED) {
                HandleSizeEvent();
            }
            HandleViewSizeEvent(GetHWND(), msg, wParam, lParam);
            break;
        case WM_ACTIVATE:
            {
                // The fActive shouldn't be WA_INACTIVE && the window shouldn't be minimized:
                const bool isFocusGained = LOWORD(wParam) != WA_INACTIVE && HIWORD(wParam) == 0;

                if (!isFocusGained && IsCommonDialogOwner()) {
                    // Remain in full screen while a file dialog is showing
                    break;
                }

                HWND hWndInsertAfter = isFocusGained ? HWND_TOPMOST : HWND_BOTTOM;

                if (m_bgWindow) {
                    ::SetWindowPos(m_bgWindow->GetHWND(), hWndInsertAfter, 0, 0, 0, 0,
                            SWP_ASYNCWINDOWPOS | SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOOWNERZORDER | SWP_NOSIZE);
                }
                ::SetWindowPos(GetHWND(), hWndInsertAfter, 0, 0, 0, 0,
                        SWP_ASYNCWINDOWPOS | SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOOWNERZORDER | SWP_NOSIZE);

                GlassWindow * window = GlassWindow::FromHandle(m_oldViewParent);
                if (window) {
                    window->HandleActivateEvent(isFocusGained ?
                        com_sun_glass_events_WindowEvent_FOCUS_GAINED :
                        com_sun_glass_events_WindowEvent_FOCUS_LOST);

                    // Child windows don't have a taskbar button, therefore
                    // we force exiting from the FS mode if the window looses
                    // focus.
                    if (!isFocusGained) {
                        ExitFullScreenMode(FALSE);
                    }
                }
            }
            break;
        case WM_CLOSE:
            {
                GlassWindow * window = GlassWindow::FromHandle(m_oldViewParent);
                ExitFullScreenMode(FALSE);
                if (window) {
                    window->HandleCloseEvent();
                }
            }
            return 0;
        case WM_INPUTLANGCHANGE:
            HandleViewInputLangChange(GetHWND(), msg, wParam, lParam);
            return 0;
        case WM_PAINT:
            HandleViewPaintEvent(GetHWND(), msg, wParam, lParam);
            break;
        case WM_CONTEXTMENU:
            HandleViewMenuEvent(GetHWND(), msg, wParam, lParam);
            break;
        case WM_MOUSEMOVE:
        case WM_LBUTTONDOWN:
        case WM_LBUTTONUP:
        case WM_LBUTTONDBLCLK:
        case WM_RBUTTONDOWN:
        case WM_RBUTTONUP:
        case WM_RBUTTONDBLCLK:
        case WM_MBUTTONDOWN:
        case WM_MBUTTONUP:
        case WM_MBUTTONDBLCLK:
        case WM_MOUSEWHEEL:
        case WM_MOUSEHWHEEL:
        case WM_MOUSELEAVE: {
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
            break;
        }
        case WM_CAPTURECHANGED:
            ViewContainer::NotifyCaptureChanged(GetHWND(), (HWND)lParam);
            break;
        case WM_SYSKEYDOWN:
        case WM_SYSKEYUP:
        case WM_KEYDOWN:
        case WM_KEYUP:
            HandleViewKeyEvent(GetHWND(), msg, wParam, lParam);
            // Always pass the message down to the DefWindowProc() to handle
            // system keys (Alt+F4, etc.)
            break;
        case WM_CHAR:
        case WM_IME_CHAR:
            HandleViewTypedEvent(GetHWND(), msg, wParam, lParam);
            return 0;
        case WM_IME_COMPOSITION:
        case WM_IME_ENDCOMPOSITION:
        case WM_IME_NOTIFY:
        case WM_IME_STARTCOMPOSITION:
            if (HandleViewInputMethodEvent(GetHWND(), msg, wParam, lParam)) {
                return 0;
            }
            break;
        case WM_TOUCH:
            HandleViewTouchEvent(GetHWND(), msg, wParam, lParam);
            return 0;
        case WM_GETOBJECT: {
            LRESULT lr = HandleViewGetAccessible(GetHWND(), wParam, lParam);
            if (lr) return lr;
            break;
        }
    }

    return ::DefWindowProc(GetHWND(), msg, wParam, lParam);
}

LPCTSTR FullScreenWindow::GetWindowClassNameSuffix()
{
    return szFullScreenWindowClassName;
}

BOOL FullScreenWindow::EnterFullScreenMode(GlassView * view, BOOL animate, BOOL keepRatio)
{
    if (IsAnimationInProcess()) {
        return TRUE;
    }

    AttachView(view, keepRatio);
    ShowWindow(animate);

    if (animate) {
        StartAnimation(TRUE);
    }

    return TRUE;
}

void FullScreenWindow::StartAnimation(BOOL enter)
{
    m_animationStage = (enter ? 1 : ANIMATION_MAX_ITERATION);
    UINT_PTR eventID = (enter ? IDT_GLASS_ANIMATION_ENTER : IDT_GLASS_ANIMATION_EXIT);
    ::SetTimer(GetHWND(), eventID, ANIMATION_TIMER_ELAPSE, NULL);
}

void FullScreenWindow::ExitFullScreenMode(BOOL animate)
{
    if (IsAnimationInProcess()) {
        //TODO: the animation should be terminated
        return;
    }


    if (animate) {
        StartAnimation(FALSE);
    } else {
        GlassView * view = GetGlassView();
        DetachView();
        HideWindow();
        Close();
    }
}

void FullScreenWindow::StopAnimation(BOOL enter)
{
    UINT_PTR eventID = (enter ? IDT_GLASS_ANIMATION_ENTER : IDT_GLASS_ANIMATION_EXIT);
    ::KillTimer(GetHWND(), eventID);

    if (!enter) {
        GlassView * view = GetGlassView();
        DetachView();
        HideWindow();
        Close();
    }
}

BOOL FullScreenWindow::IsAnimationInProcess()
{
    if (m_animationStage >= 1 && m_animationStage <= ANIMATION_MAX_ITERATION) {
        return true;
    }
    return false;
}

void FullScreenWindow::UpdateAnimationRect()
{
    RECT rect;
    float stage = (float)m_animationStage / (float)ANIMATION_MAX_ITERATION;
    rect.left = m_viewRect.left +  (long)((m_windowRect.left - m_viewRect.left) * stage);                                                                ;
    rect.top = m_viewRect.top +  (long)((m_windowRect.top - m_viewRect.top) * stage);
    rect.right = m_viewRect.right +  (long)((m_windowRect.right - m_viewRect.right) * stage);
    rect.bottom = m_viewRect.bottom +  (long)((m_windowRect.bottom - m_viewRect.bottom) * stage);

    ::SetWindowPos(GetHWND(), NULL, rect.left, rect.top,
                    rect.right - rect.left, rect.bottom - rect.top,
                    SWP_NOACTIVATE | SWP_NOZORDER | SWP_NOSENDCHANGING | SWP_DEFERERASE);
}

// Transparent background window

BackgroundWindow::BackgroundWindow() : BaseWnd()
{
    m_rect.left = 0;
    m_rect.top = 0;
    m_rect.right = 0;
    m_rect.bottom = 0;
}

BackgroundWindow::~BackgroundWindow()
{
}

HWND BackgroundWindow::Create()
{
    DWORD dwStyle = WS_POPUP | WS_CLIPCHILDREN;
    DWORD dwExStyle = WS_EX_LAYERED | WS_EX_TOOLWINDOW;

    return BaseWnd::Create(NULL, 0, 0, 0, 0,
                           TEXT(""), dwExStyle, dwStyle, (HBRUSH)::GetStockObject(BLACK_BRUSH));
}

void BackgroundWindow::Close()
{
    ::DestroyWindow(GetHWND());
}

LRESULT BackgroundWindow::WindowProc(UINT msg, WPARAM wParam, LPARAM lParam)
{
    switch (msg) {
        case WM_MOUSEACTIVATE: {
            return MA_NOACTIVATE;
        }
    }

    return ::DefWindowProc(GetHWND(), msg, wParam, lParam);
}

LPCTSTR BackgroundWindow::GetWindowClassNameSuffix()
{
    return szBackgroundWindowClassName;
}

void BackgroundWindow::SetWindowRect(RECT * rect)
{
    ::CopyRect(&m_rect, rect);
}

void BackgroundWindow::ShowWindow(BOOL animate) {
    BYTE opacity = (animate ? 0x0 : 0xFF);
    ::SetLayeredWindowAttributes(GetHWND(), RGB(0, 0, 0), opacity, LWA_ALPHA);
    ::SetWindowPos(GetHWND(), HWND_TOPMOST, m_rect.left, m_rect.top,
                    m_rect.right - m_rect.left, m_rect.bottom - m_rect.top,
                    SWP_SHOWWINDOW | SWP_NOACTIVATE);
}

void BackgroundWindow::HideWindow()
{
    ::ShowWindow(GetHWND(), SW_HIDE);
}

void BackgroundWindow::UpdateAnimationOpacity(int animationStage)
{
    BYTE opacity = ((int)0xFF * animationStage) / ANIMATION_MAX_ITERATION;
    ::SetLayeredWindowAttributes(GetHWND(), RGB(0, 0, 0), opacity, LWA_ALPHA);
}
