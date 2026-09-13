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

#ifndef _BASEWND_INCLUDED_
#define _BASEWND_INCLUDED_


class GlassView;

struct MessageResult {
    const bool processed;
    const LRESULT result; // only valid if processed == true

    MessageResult() : processed(false), result(0) {}
    MessageResult(LRESULT r) : processed(true), result(r) {}
};

class BaseWnd {
public:
    BaseWnd(HWND ancestor = NULL);
    virtual ~BaseWnd();

    //XXX: might eliminate hParent and use m_ancestor instead
    HWND Create(HWND hParent, int x, int y, int width, int height,
                LPCTSTR lpWindowName, DWORD dwExStyle, DWORD dwStyle, HBRUSH hbrBackground);

    // Creates an overlapped window with default x, y, width and height and
    // returns its bounds. This method is used to find the default window
    // size/location when CW_USEDEFAULT can't be used (e.g. for WS_POPUP windows).
    static BOOL GetDefaultWindowBounds(LPRECT r);

    HWND GetHWND() { return m_hWnd; }

    static BaseWnd* FromHandle(HWND hWnd);

    inline virtual bool IsGlassWindow() { return false; }

    virtual BOOL EnterFullScreenMode(GlassView * view, BOOL animate, BOOL keepRatio) { return FALSE; }
    virtual void ExitFullScreenMode(BOOL animate) {}

    HWND GetAncestor() const { return m_ancestor; }
    void SetAncestor(HWND ancestor) { m_ancestor = ancestor; }

    void SetCommonDialogOwner(bool owner) { m_isCommonDialogOwner = owner; }

    void SetCursor(HCURSOR cursor);

    // Begin processing a message.
    void BeginMessageProcessing(UINT msg);
    // End processing a message. Returns 'true' if the BaseWnd should be
    // deleted.
    bool EndMessageProcessing();

private:
    HWND m_hWnd;
    static LRESULT CALLBACK StaticWindowProc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam);

    static unsigned int sm_classNameCounter;

    HWND m_ancestor;  // either owner or parent. a window can't have both at once anyway

    ATOM m_wndClassAtom;
    bool m_isCommonDialogOwner;
    HCURSOR m_hCursor;

    LONG m_messageCount;
    bool m_isDead;

protected:
    virtual LRESULT WindowProc(UINT msg, WPARAM wParam, LPARAM lParam) = 0;
    virtual MessageResult CommonWindowProc(UINT msg, WPARAM wParam, LPARAM lParam);

    virtual LPCTSTR GetWindowClassNameSuffix() = 0;

    bool IsCommonDialogOwner() { return m_isCommonDialogOwner; }

};

#endif  // _BASEWND_INCLUDED_
