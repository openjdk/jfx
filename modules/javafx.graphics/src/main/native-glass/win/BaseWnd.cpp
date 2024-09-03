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

#include "common.h"

#include "BaseWnd.h"
#include "GlassApplication.h"


//NOTE: it's not thread-safe
unsigned int BaseWnd::sm_classNameCounter = 0;

static LPCTSTR szBaseWndProp = TEXT("BaseWndProp");

BaseWnd::BaseWnd(HWND ancestor) :
    m_hWnd(NULL),
    m_ancestor(ancestor),
    m_wndClassAtom(0),
    m_isCommonDialogOwner(false),
    m_hCursor(NULL),
    m_messageCount(0),
    m_isDead(false)
{

}

BaseWnd::~BaseWnd()
{
    if (m_wndClassAtom)
    {
        // This is called from WM_NCDESTROY, and ::UnregisterClass() will fail.
        // Schedule the operation for later time when the HWND is dead and
        // the window class is really free already.
        ENTER_MAIN_THREAD()
        {
            if (!::UnregisterClass(reinterpret_cast<LPCTSTR>(wndClassAtom),
                        ::GetModuleHandle(NULL)))
            {
                _tprintf_s(L"BaseWnd::UnregisterClass(%i) error: %u\n",
                        (int)wndClassAtom, ::GetLastError());
            }
        }
        ATOM wndClassAtom;
        LEAVE_MAIN_THREAD_LATER;

        ARG(wndClassAtom) = m_wndClassAtom;

        PERFORM_LATER();
    }
}

/*static*/
BaseWnd* BaseWnd::FromHandle(HWND hWnd)
{
    return (BaseWnd *)::GetProp(hWnd, szBaseWndProp);
}

HWND BaseWnd::Create(HWND hParent, int x, int y, int width, int height,
        LPCTSTR lpWindowName, DWORD dwExStyle, DWORD dwStyle, HBRUSH hbrBackground)
{
    HINSTANCE hInst = ::GetModuleHandle(NULL);
    TCHAR szClassName[256];

    ::ZeroMemory(szClassName, sizeof(szClassName));
    _stprintf_s(szClassName, sizeof(szClassName)/sizeof(szClassName[0]),
            _T("GlassWndClass-%s-%u"), GetWindowClassNameSuffix(), ++BaseWnd::sm_classNameCounter);

    WNDCLASSEX wndcls;
    wndcls.cbSize           = sizeof(WNDCLASSEX);
    wndcls.style            = CS_HREDRAW | CS_VREDRAW;
    wndcls.lpfnWndProc      = StaticWindowProc;
    wndcls.cbClsExtra       = 0;
    wndcls.cbWndExtra       = 0;
    wndcls.hInstance        = hInst;
    wndcls.hIcon            = NULL;
    wndcls.hCursor          = ::LoadCursor(NULL, IDC_ARROW);
    wndcls.hbrBackground    = hbrBackground;
    wndcls.lpszMenuName     = NULL;
    wndcls.lpszClassName    = szClassName;
    wndcls.hIconSm          = NULL;

    m_hCursor               = wndcls.hCursor;

    m_wndClassAtom = ::RegisterClassEx(&wndcls);

    if (!m_wndClassAtom) {
        _tprintf_s(L"BaseWnd::RegisterClassEx(%s) error: %u\n", szClassName, ::GetLastError());
    } else {
        if (lpWindowName == NULL) {
            lpWindowName = TEXT("");
        }
        ::CreateWindowEx(dwExStyle, szClassName, lpWindowName,
                dwStyle, x, y, width, height, hParent,
                NULL, hInst, (void *)this);

        if (GetHWND() == NULL) {
            _tprintf_s(L"BaseWnd::Create(%s) error: %u\n", szClassName, ::GetLastError());
        }
    }

    return m_hWnd;

}

/*static*/
BOOL BaseWnd::GetDefaultWindowBounds(LPRECT r)
{
    HINSTANCE hInst = ::GetModuleHandle(NULL);
    TCHAR* szClassName = L"GLASSDEFAULTWINDOW";

    WNDCLASS wndcls;
    ::ZeroMemory(&wndcls, sizeof(WNDCLASS));
    wndcls.lpfnWndProc      = StaticWindowProc;
    wndcls.hInstance        = hInst;
    wndcls.lpszClassName    = szClassName;
    ::RegisterClass(&wndcls);

    HWND hwnd = ::CreateWindow(szClassName, L"", WS_OVERLAPPED,
                               CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
                               0, 0, 0, 0);
    BOOL res = ::GetWindowRect(hwnd, r);
    ::DestroyWindow(hwnd);
    ::UnregisterClass(szClassName, hInst);

    return res;
}

/*static*/
LRESULT CALLBACK BaseWnd::StaticWindowProc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    BaseWnd *pThis = NULL;
    if (msg == WM_CREATE) {
        pThis = (BaseWnd *)((CREATESTRUCT *)lParam)->lpCreateParams;
        ::SetProp(hWnd, szBaseWndProp, (HANDLE)pThis);
        if (pThis != NULL) {
            pThis->m_hWnd = hWnd;
        }
    } else {
        pThis = (BaseWnd *)::GetProp(hWnd, szBaseWndProp);
    }
    if (pThis != NULL) {
        pThis->BeginMessageProcessing(msg);
        LRESULT result = pThis->WindowProc(msg, wParam, lParam);
        if (pThis->EndMessageProcessing()) {
            ::RemoveProp(hWnd, szBaseWndProp);
            delete pThis;
        }
        return result;
    }
    return ::DefWindowProc(hWnd, msg, wParam, lParam);
}

/*non-static*/
void BaseWnd::BeginMessageProcessing(UINT msg)
{
    if (msg == WM_NCDESTROY) {
        m_isDead = true;
    }
    m_messageCount += 1;
}

bool BaseWnd::EndMessageProcessing()
{
    if (m_messageCount > 0) {
        m_messageCount -= 1;
    }
    return m_isDead && (m_messageCount == 0);
}

/*virtual*/
MessageResult BaseWnd::CommonWindowProc(UINT msg, WPARAM wParam, LPARAM lParam)
{
    static const MessageResult NOT_PROCESSED;

    switch (msg) {
        case WM_SETCURSOR:
            if (LOWORD(lParam) == HTCLIENT) {
                ::SetCursor(m_hCursor);
                return TRUE;
            }
            break;
    }

    return NOT_PROCESSED;
}

void BaseWnd::SetCursor(HCURSOR cursor)
{
    m_hCursor = cursor;

    // Might be worth checking the current cursor position.
    // However, we've always set cursor unconditionally relying on the caller
    // invoking this method only when it processes mouse_move or alike events.
    // As long as there's no bugs filed, let it be.
    ::SetCursor(m_hCursor);
}

