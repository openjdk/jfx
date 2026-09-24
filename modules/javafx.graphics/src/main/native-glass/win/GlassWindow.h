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

#ifndef _GLASS_WINDOW_
#define _GLASS_WINDOW_

#include "BaseWnd.h"
#include "ViewContainer.h"
#include "glass_win_api.h"

// Converts a float [0..1] to a BYTE [0..255] - the caller's side of _setAlpha / gwin_window_set_alpha
#define F2B(value) BYTE(255.f * (value))


class GlassWindow : public BaseWnd, public ViewContainer {
public:
    /*
     * windowId is the Java-assigned identity the window callback table carries (glass_win_api.h,
     * IDENTITY); gwin_window_create is the only way a GlassWindow is made. It holds no jobject.
     */
    GlassWindow(bool isTransparent, bool isDecorated, bool isUnified,
                bool isExtended, HWND parentOrOwner, int64_t windowId);
    virtual ~GlassWindow();

    static GlassWindow* FromHandle(HWND hWnd) {
        return (GlassWindow*)BaseWnd::FromHandle(hWnd);
    }

    HWND Create(DWORD dwStyle, DWORD dwExStyle, HMONITOR hMonitor, HWND owner);
    void Close();

    /*
     * The action bodies of the gwin_window_* exports (glass_win_api.cpp); each runs INSIDE the
     * ENTER_MAIN_THREAD action of its caller. CreateFromMask is gwin_window_create's action: the
     * style computation, new GlassWindow, Create and the closeable / dark-frame tail. DoSetView /
     * DoSetVisible are the set_view / set_visible actions, kept in GlassWindow.cpp because they
     * touch its activeTouchWindow.
     */
    static HWND CreateFromMask(int64_t windowId, HWND owner, HMONITOR hMonitor, int32_t mask);
    static void DoSetView(HWND hWnd, GlassView* view);
    static void DoSetVisible(HWND hWnd, bool visible);

    // BaseWnd::SetCursor on this window and on the delegate window when one exists - the tail of
    // gwin_window_set_cursor's action.
    void ApplyCursor(HCURSOR cursor);

    void setMinSize(long width, long height);
    void setMaxSize(long width, long height);
    POINT getMinSize() { return m_minSize; }
    POINT getMaxSize() { return m_maxSize; }

    HMONITOR GetMonitor();
    void SetMonitor(HMONITOR hMonitor);

    inline int64_t GetWindowId() const { return m_windowId; }

    void SetFocusable(bool val);
    bool RequestFocus(int32_t event);
    inline bool IsFocusable() { return m_isFocusable; }
    inline bool IsTransparent() { return m_isTransparent; }
    inline bool IsResizable() { return m_isResizable; }
    inline bool IsDecorated() { return m_isDecorated; }
    inline virtual bool IsGlassWindow() { return true; }

    bool SetResizable(bool resizable);

    void SetAlpha(BYTE alpha);
    inline BYTE GetAlpha() { return m_alpha; }

    void UpdateInsets();
    inline RECT GetInsets() { return m_insets; }

    LONG GetStyle() { return ::GetWindowLong(GetHWND(), GWL_STYLE); }
    void SetStyle(LONG style, bool setWindowPos = true)
    {
        ::SetWindowLong(GetHWND(), GWL_STYLE, style);
        if (setWindowPos) {
            ::SetWindowPos(GetHWND(), NULL, 0, 0, 0, 0,
                    SWP_FRAMECHANGED | SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOOWNERZORDER | SWP_NOREPOSITION | SWP_NOSIZE | SWP_NOZORDER);
        }
    }

    HMENU GetMenu() { return m_hMenu; }
    void SetMenu(HMENU hMenu) { m_hMenu = hMenu; }

    inline bool IsEnabled() { return m_isEnabled; }
    void SetEnabled(bool enabled);

    bool GrabFocus();
    void UngrabFocus();
    void CheckUngrab();
    static void ResetGrab();

    void SetDelegateWindow(HWND hWnd);
    HWND GetDelegateWindow() { return m_delegateWindow; }

    void HandleActivateEvent(int32_t event);
    void HandleCloseEvent();

    virtual BOOL EnterFullScreenMode(GlassView * view, BOOL animate, BOOL keepRatio);
    virtual void ExitFullScreenMode(BOOL animate);

    void SetIcon(HICON hIcon);
    void SetDarkFrame(bool);
    void ShowSystemMenu(int x, int y);

    /*
     * The callback table of glass_win_api.h's window section. NULL while Java has installed nothing,
     * and an upcall site (or ~GlassWindow) that finds NULL delivers nothing - there is no JNI path
     * to take over.
     * Once installed no slot is ever NULL: the setter replaces a NULL slot with a no-op. Written on
     * the launcher thread before the toolkit thread exists and read on the toolkit thread only - no
     * lock, like the view tables.
     */
    static inline const GwinWindowCallbacks* WindowCallbacks()
    {
        return sm_windowCallbacksInstalled ? &sm_windowCallbacks : NULL;
    }
    static void SetWindowCallbacks(const GwinWindowCallbacks* cb);

protected:
    virtual LRESULT WindowProc(UINT msg, WPARAM wParam, LPARAM lParam);

    virtual LPCTSTR GetWindowClassNameSuffix();

private:
    int64_t m_windowId;   // IDENTITY in glass_win_api.h

    enum State {
        Normal = SIZE_RESTORED,
        Minimized = SIZE_MINIMIZED,
        Maximized = SIZE_MAXIMIZED
    };
    State m_state;

    enum WinChangingReason {
        Unknown,
        WasMoved,
        WasSized
    };
    WinChangingReason m_winChangingReason;

    // -1 for x or y indicate the values aren't set
    POINT m_minSize;
    POINT m_maxSize;

    HMONITOR m_hMonitor;

    bool m_isFocusable;
    bool m_isFocused;
    // 'synthetic' focus event to be sent from WM_SETFOCUS for a child window
    int32_t m_focusEvent;

    bool IsFocused() { return m_isFocused; }
    void SetFocused(bool focused) { m_isFocused = focused; }

    static HWND sm_grabWindow;

    const bool m_isTransparent;
    const bool m_isDecorated;
    const bool m_isUnified;
    const bool m_isExtended;

    bool m_isResizable;

    BYTE m_alpha;

    HMENU m_hMenu;

    HICON m_hIcon;

    //NOTE: this is not a rectangle. The left, top, right, and bottom
    //components contain corresponding insets values.
    RECT m_insets;

    struct {
        bool entered = false;
        bool tracking = false;
        POINT dragStart = {};
    } m_caption;

    static unsigned int sm_instanceCounter;
    static HHOOK sm_hCBTFilter;
    static LRESULT CALLBACK CBTFilter(int nCode, WPARAM wParam, LPARAM lParam);

    bool m_isEnabled;

    // Used in Fullscreen Mode
    HWND m_delegateWindow;

    HWND GetCurrentHWND() { return m_delegateWindow ? m_delegateWindow : GetHWND(); }

    bool m_isInFullScreen;
    RECT m_beforeFullScreenRect;
    LONG m_beforeFullScreenStyle, m_beforeFullScreenExStyle;
    HMENU m_beforeFullScreenMenu;

    bool IsInFullScreenMode() { return m_isInFullScreen; }

    void HandleDestroyEvent();
    // if pRect == NULL => get position/size by GetWindowRect
    void HandleWindowPosChangingEvent(WINDOWPOS *pWinPos);
    void HandleMoveEvent(RECT *pRect);
    // if pRect == NULL => get position/size by GetWindowRect
    void HandleSizeEvent(int type, RECT *pRect);
    void HandleDPIEvent(WPARAM wParam, LPARAM lParam);
    bool HandleCommand(WORD cmdID);
    void HandleFocusDisabledEvent();
    bool HandleMouseEvents(UINT msg, WPARAM wParam, LPARAM lParam);
    bool HandleCaptionMouseEvents(UINT msg, WPARAM wParam, LPARAM lParam);
    void HandleNonClientMouseEvents(UINT msg, WPARAM wParam, LPARAM lParam);
    LRESULT HandleNCCalcSizeEvent(UINT msg, WPARAM wParam, LPARAM lParam);
    BOOL HandleNCHitTestEvent(SHORT, SHORT, LRESULT&);

    static GwinWindowCallbacks sm_windowCallbacks;
    static bool sm_windowCallbacksInstalled;
};


#endif
