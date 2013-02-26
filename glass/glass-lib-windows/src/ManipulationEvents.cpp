/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "ManipulationEvents.h"
#include "GlassView.h"
#include "ViewContainer.h"

// Required for some _CLSID and _IID declarations
#include <manipulations_i.c>

namespace {

template <class IFace>
HRESULT Connect(_IManipulationEvents* sink, IFace *cp)
{
    ASSERT(cp);

    HRESULT hr = S_OK;

    IConnectionPointContainer* spConnectionContainer;
    hr = cp->QueryInterface(IID_IConnectionPointContainer,
            (LPVOID*)&spConnectionContainer);
    if (!spConnectionContainer){
        // TODO: report an error and return
        return hr;
    }

    IConnectionPoint* pConnPoint;
    hr = spConnectionContainer->FindConnectionPoint(
            __uuidof(_IManipulationEvents), &pConnPoint);
    if (!pConnPoint) {
        spConnectionContainer->Release();
        // TODO: report an error and return
        return hr;
    }

    DWORD dwCookie;
    hr = pConnPoint->Advise(sink, &dwCookie);
    
    spConnectionContainer->Release();
    pConnPoint->Release();

    return hr;
}

} // namespace


ManipulationEventSink::ManipulationEventSink(IManipulationProcessor *manip, 
                                             ViewContainer *window, HWND hwnd)
    : m_window(window)
    , m_hwnd(hwnd)
    , m_isInertia(false)
{
    Connect(this, manip);
}

ManipulationEventSink::ManipulationEventSink(IInertiaProcessor *inertia, 
                                             ViewContainer *window, HWND hwnd)
    : m_window(window)
    , m_hwnd(hwnd)
    , m_isInertia(true)
{
    if (inertia) {
        Connect(this, inertia);
    }
}

ManipulationEventSink::~ManipulationEventSink()
{
}

HRESULT STDMETHODCALLTYPE ManipulationEventSink::ManipulationStarted(
    FLOAT x, FLOAT y)
{
    // NOP by design.
    return S_OK;
}

HRESULT STDMETHODCALLTYPE ManipulationEventSink::ManipulationDelta(
    FLOAT x, FLOAT y, FLOAT deltaX, FLOAT deltaY,
    FLOAT scaleDelta, FLOAT expansionDelta, FLOAT rotationDelta,
    FLOAT cumulativeDeltaX, FLOAT cumulativeDeltaY,
    FLOAT cumulativeScale, FLOAT cumulativeExpansion, FLOAT cumulativeRotation)
{
    // TBD: set to 'true' if source device is a touch screen 
    // and to 'false' if source device is a touch pad.
    // So far assume source device on Windows is always a touch screen.
    const bool isDirect = true;

    m_window->NotifyGesturePerformed(m_hwnd, isDirect, m_isInertia, 
        x, y, deltaX, deltaY,
        scaleDelta, expansionDelta, rotationDelta,
        cumulativeDeltaX, cumulativeDeltaY,
        cumulativeScale, cumulativeExpansion, cumulativeRotation);
    return S_OK;
}

HRESULT STDMETHODCALLTYPE ManipulationEventSink::ManipulationCompleted(
    FLOAT x, FLOAT y,
    FLOAT cumulativeDeltaX, FLOAT cumulativeDeltaY,
    FLOAT cumulativeScale, FLOAT cumulativeExpansion, FLOAT cumulativeRotation)
{
    // NOP by design.
    return S_OK;
}

HRESULT STDMETHODCALLTYPE ManipulationEventSinkWithInertia::ManipulationStarted(
    FLOAT x, FLOAT y)
{
    m_window->StopTouchInputInertia(m_hwnd);
    return S_OK;
}

HRESULT STDMETHODCALLTYPE ManipulationEventSinkWithInertia::ManipulationCompleted(
    FLOAT x, FLOAT y,
    FLOAT cumulativeTranslationX, FLOAT cumulativeTranslationY,
    FLOAT cumulativeScale, FLOAT cumulativeExpansion, FLOAT cumulativeRotation)
{
    m_window->StartTouchInputInertia(m_hwnd);
    return S_OK;
}
