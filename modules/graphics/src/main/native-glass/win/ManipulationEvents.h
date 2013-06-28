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

#ifndef _MANIPULATION_EVENTS_
#define _MANIPULATION_EVENTS_


class ViewContainer;

class ManipulationEventSink : public IUnknownImpl<_IManipulationEvents>
{
private:
    ManipulationEventSink(const ManipulationEventSink&);
    ManipulationEventSink& operator=(const ManipulationEventSink&);
public:
    ManipulationEventSink(IManipulationProcessor *manip,
                          ViewContainer *window, HWND hwnd);
    ManipulationEventSink(IInertiaProcessor *inertia,
                          ViewContainer *window, HWND hwnd);
    ~ManipulationEventSink();

    virtual HRESULT STDMETHODCALLTYPE ManipulationStarted(
        FLOAT x, FLOAT y);

    virtual HRESULT STDMETHODCALLTYPE ManipulationDelta(
        FLOAT x, FLOAT y,
        FLOAT translationDeltaX, FLOAT translationDeltaY,
        FLOAT scaleDelta, FLOAT expansionDelta, FLOAT rotationDelta,
        FLOAT cumulativeTranslationX, FLOAT cumulativeTranslationY,
        FLOAT cumulativeScale, FLOAT cumulativeExpansion, FLOAT cumulativeRotation);

    virtual HRESULT STDMETHODCALLTYPE ManipulationCompleted(
        FLOAT x, FLOAT y,
        FLOAT cumulativeTranslationX, FLOAT cumulativeTranslationY,
        FLOAT cumulativeScale, FLOAT cumulativeExpansion, FLOAT cumulativeRotation);

protected:
    ViewContainer *m_window;
    const HWND m_hwnd;
    const bool m_isInertia;
};




class ManipulationEventSinkWithInertia : public ManipulationEventSink
{    
public:
    ManipulationEventSinkWithInertia(IManipulationProcessor *manip, 
                                     IInertiaProcessor *inertia,
                                     ViewContainer *window, HWND hwnd)
        : ManipulationEventSink(manip, window, hwnd)
    {
        m_inertiaSink = new ManipulationEventSink(inertia, window, hwnd);
    }
    
    virtual ~ManipulationEventSinkWithInertia()
    {
        if (m_inertiaSink) {
            m_inertiaSink->Release();
        }
    }

    virtual HRESULT STDMETHODCALLTYPE ManipulationStarted(
        FLOAT x, FLOAT y);

    virtual HRESULT STDMETHODCALLTYPE ManipulationCompleted(
        FLOAT x, FLOAT y,
        FLOAT cumulativeTranslationX, FLOAT cumulativeTranslationY,
        FLOAT cumulativeScale, FLOAT cumulativeExpansion, FLOAT cumulativeRotation);

private:
    _IManipulationEvents* m_inertiaSink;
};


#endif // _MANIPULATION_EVENTS_
