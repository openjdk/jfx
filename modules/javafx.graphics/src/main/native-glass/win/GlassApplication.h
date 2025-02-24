/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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
#define DECL_JREF(T, var) JGlobalRef<T> var
#define DECL_jobject(var) DECL_JREF(jobject, var)

#define PERFORM() GlassApplication::ExecAction(&_action)

#define PERFORM_AND_RETURN() (PERFORM(), _action._retValue)

#define PERFORM_LATER() GlassApplication::ExecActionLater(_pAction)

#define WM_DO_ACTION        (WM_USER+1)
#define WM_DO_ACTION_LATER  (WM_USER+2)

class GlassApplication : protected BaseWnd {
public:
    GlassApplication(jobject jrefThis);
    virtual ~GlassApplication();

    static HWND GetToolkitHWND() { return  (NULL == pInstance) ? NULL : pInstance->GetHWND(); }
    static GlassApplication *GetInstance() { return pInstance; }
    static void ExecAction(Action *action);
    static void ExecActionLater(Action *action);
    void RegisterClipboardViewer(jobject clipboard);
    void UnregisterClipboardViewer();

    static jobject GetPlatformPreferences() {
        return pInstance ? pInstance->m_platformSupport.collectPreferences(PlatformSupport::PT_ALL) : NULL;
    }

    inline static DWORD GetMainThreadId()
    {
        return pInstance == NULL ? 0 : pInstance->m_mainThreadId;
    }

    static jobject EnterNestedEventLoop(JNIEnv * env);
    static void LeaveNestedEventLoop(JNIEnv * env, jobject retValue);
    static void SetGlassClassLoader(JNIEnv *env, jobject classLoader);
    static jclass ClassForName(JNIEnv *env, char *className);

    static void SetHInstance(HINSTANCE hInstace) { GlassApplication::hInstace = hInstace; }
    static HINSTANCE GetHInstance() { return GlassApplication::hInstace; }

    static ULONG IncrementAccessibility();
    static ULONG DecrementAccessibility();
    static ULONG GetAccessibilityCount();

    static jfloat overrideUIScale;

    inline static jboolean IsUIScaleOverridden()
    {
        return (overrideUIScale > 0.0f);
    }

    inline static jfloat GetUIScale(UINT dpi)
    {
        return IsUIScaleOverridden()
            ? overrideUIScale
            : dpi / ((float) USER_DEFAULT_SCREEN_DPI);
    }

protected:
    virtual LRESULT WindowProc(UINT msg, WPARAM wParam, LPARAM lParam);
    virtual LPCTSTR GetWindowClassNameSuffix();

private:
    jobject m_grefThis;
    jobject m_clipboard;
    static GlassApplication *pInstance;
    HWND    m_hNextClipboardView;
    DWORD m_mainThreadId;
    static jobject sm_glassClassLoader;
    PlatformSupport m_platformSupport;

    // These are static because the GlassApplication instance may be
    // destroyed while the nested loop is spinning
    static bool sm_shouldLeaveNestedLoop;
    static JGlobalRef<jobject> sm_nestedLoopReturnValue;

    static HINSTANCE hInstace;
    static ULONG s_accessibilityCount;
};


#endif //_GLASS_APPLICATION_
