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

#ifndef _FULLSCREEN_WINDOW_
#define _FULLSCREEN_WINDOW_

#include "BaseWnd.h"
#include "ViewContainer.h"


class BackgroundWindow;
class GlassView;

class FullScreenWindow : public BaseWnd, public ViewContainer {

public:
    FullScreenWindow();
    virtual ~FullScreenWindow();
    HWND Create();
    void Close();

    virtual BOOL EnterFullScreenMode(GlassView * view, BOOL animate, BOOL keepRatio);
    virtual void ExitFullScreenMode(BOOL animate);

    static void ClientRectInScreen(HWND hwnd, RECT * rect);
    static void CalculateBounds(HWND hwnd, RECT * screenRect,
            RECT * contentRect, BOOL keepRatio, const RECT & viewRect);

protected:
    virtual LRESULT WindowProc(UINT msg, WPARAM wParam, LPARAM lParam);
    virtual LPCTSTR GetWindowClassNameSuffix();

    virtual void HandleViewTimerEvent(HWND hwnd, UINT_PTR timerID); // override ViewContainer
    void HandleSizeEvent();

private:
    HWND m_oldViewParent; // view's parent window
    RECT m_viewRect;
    RECT m_windowRect;
    UINT m_animationStage;
    BackgroundWindow *m_bgWindow;

    void AttachView(GlassView * view, BOOL keepRatio);// attaches content view
    void DetachView();                                // detaches content view

    void ShowWindow(BOOL animate);
    void HideWindow();
    void InitWindowRect(BOOL keepRatio);

    void StartAnimation(BOOL enter);
    void StopAnimation(BOOL enter);
    BOOL IsAnimationInProcess();
    void UpdateAnimationRect();
};

// Transparent background window to implement fade-in/fade-out effect.
// Note that full screen window can't be made transparent, otherwise,
// view would be made transparent also.

class BackgroundWindow : public BaseWnd {

public:
    BackgroundWindow();
    virtual ~BackgroundWindow();
    HWND Create();
    void Close();

    void ShowWindow(BOOL animate);
    void HideWindow();
    void UpdateAnimationOpacity(int animationStage);
    void SetWindowRect(RECT * rect);

protected:
    virtual LRESULT WindowProc(UINT msg, WPARAM wParam, LPARAM lParam);
    virtual LPCTSTR GetWindowClassNameSuffix();

private:
    RECT m_rect;
};

#endif
