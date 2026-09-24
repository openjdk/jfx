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

#ifndef _GLASS_APPLICATION_
#define _GLASS_APPLICATION_

#include "BaseWnd.h"
#include "PlatformSupport.h"

class Action {
public:
    virtual void Do() = 0;
    virtual ~Action() {}
};

#define ENTER_MAIN_THREAD_AND_RETURN(RetType) \
    class _MyAction : public Action {    \
        public: \
                RetType _retValue;  \
                virtual void Do() {   \
                    _retValue = _UserDo(); \
                }   \
                RetType _UserDo()

#define ENTER_MAIN_THREAD() \
    class _MyAction : public Action {    \
        public: \
                virtual void Do()

#define LEAVE_MAIN_THREAD   \
    } _action;

#define LEAVE_MAIN_THREAD_LATER   \
    };                                      \
    _MyAction * _pAction = new _MyAction(); \
    _MyAction & _action = *_pAction;

#define ARG(var) _action.var

#define PERFORM() GlassApplication::ExecAction(&_action)

#define PERFORM_AND_RETURN() (PERFORM(), _action._retValue)

#define PERFORM_LATER() GlassApplication::ExecActionLater(_pAction)

#define WM_DO_ACTION        (WM_USER+1)
#define WM_DO_ACTION_LATER  (WM_USER+2)

class GlassApplication : protected BaseWnd {
public:
    // gwin_app_create's constructor (glass_win_api.h): the toolkit window, with a PlatformSupport that holds no
    // JNI state.
    GlassApplication();
    virtual ~GlassApplication();

    static HWND GetToolkitHWND() { return  (NULL == pInstance) ? NULL : pInstance->GetHWND(); }
    static GlassApplication *GetInstance() { return pInstance; }
    static void ExecAction(Action *action);
    static void ExecActionLater(Action *action);
    /*
     * The registration behind gwin_clipboard_register_viewer (glass_win_api.h): remembers
     * clipboardId as the target of GwinClipboardCallbacks.content_changed. Disposes whatever was
     * registered before; 0 only disposes (the WM_DESTROY path).
     */
    void RegisterClipboardViewerId(int64_t clipboardId);
    void UnregisterClipboardViewer();

    /*
     * The live PlatformSupport, or NULL before Application.run() creates the toolkit window and after
     * it is destroyed. glass_win_api.h's gwin_prefs_query_ui_settings / gwin_prefs_query_network reach
     * the WinRT objects through this; NULL means "no WinRT keys" (both report available == 0), not "no
     * preference keys": WinPreferences.collect still returns the user32 keys it reads itself
     * (Windows.SysColor.*, Windows.SPI.*).
     */
    static PlatformSupport* GetPlatformSupport() {
        return pInstance ? &pInstance->m_platformSupport : NULL;
    }

    inline static DWORD GetMainThreadId()
    {
        return pInstance == NULL ? 0 : pInstance->m_mainThreadId;
    }

    /*
     * The nested event loop, JVM-free: glass_win_api.h's gwin_enter_nested_event_loop /
     * gwin_leave_nested_event_loop are one-line forwarders to these. The loop's return value never
     * left Java in the first place; WinApplication holds it itself. Toolkit thread only.
     */
    static void EnterNestedEventLoop();
    static void LeaveNestedEventLoop();

    static ULONG IncrementAccessibility();
    static ULONG DecrementAccessibility();
    static ULONG GetAccessibilityCount();

protected:
    virtual LRESULT WindowProc(UINT msg, WPARAM wParam, LPARAM lParam);
    virtual LPCTSTR GetWindowClassNameSuffix();

private:
    int64_t m_clipboardId;   // the registered clipboard peer's id; 0 when none is registered
    void DisposeRegisteredClipboard();
    static GlassApplication *pInstance;
    HWND    m_hNextClipboardView;
    DWORD m_mainThreadId;
    PlatformSupport m_platformSupport;

    // These are static because the GlassApplication instance may be
    // destroyed while the nested loop is spinning
    static bool sm_shouldLeaveNestedLoop;

    static ULONG s_accessibilityCount;
};


#endif //_GLASS_APPLICATION_
