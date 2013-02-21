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

#ifndef GLASSDND_H
#define GLASSDND_H

#include "ViewContainer.h"

//Ctrl + Shift -> ACTION_LINK
//Ctrl         -> ACTION_COPY
//Shift        -> ACTION_MOVE

_COM_SMARTPTR_TYPEDEF(IDropTargetHelper, IID_IDropTargetHelper);
_COM_SMARTPTR_TYPEDEF(IDragSourceHelper, IID_IDragSourceHelper);

class GlassDropTarget : public IUnknownImpl<IDropTarget>
{
public:
    GlassDropTarget(ViewContainer *viewContainer, HWND hwnd);
    virtual ~GlassDropTarget();

protected:
    STDMETHOD(DragEnter)(
        /* [unique][in] */ IDataObject *pDataObj,
        /* [in] */ DWORD grfKeyState,
        /* [in] */ POINTL pt,
        /* [out][in] */ DWORD *pdwEffect);

    STDMETHOD(DragOver)(
        /* [in] */ DWORD grfKeyState,
        /* [in] */ POINTL pt,
        /* [out][in] */ DWORD *pdwEffect);

    STDMETHOD(DragLeave)();

    STDMETHOD(Drop)(
        /* [unique][in] */ IDataObject *pDataObj,
        /* [in] */ DWORD grfKeyState,
        /* [in] */ POINTL pt,
        /* [out][in] */ DWORD *pdwEffect);

    HRESULT  UpdateDnDClipboardData(
        IDataObject *pDataObj);

    HRESULT  CallbackToJava(
        /* [in] */ jmethodID method,
        /* [in] */ DWORD grfKeyState,
        /* [in] */ POINTL pt,
        /* [out][in] */ DWORD *pdwEffect);

    ViewContainer *m_viewContainer;
    IDropTargetHelperPtr m_spDropTargetHelper;

private:
    static HRESULT SetSourceSupportedActions(jint actions);
    HWND m_hwnd;
};

class GlassDropSource : public IUnknownImpl<IDropSource>
{
public:
    GlassDropSource(jobject jDnDClipboard);
    virtual ~GlassDropSource();
    static HRESULT SetDragButton(jint button);

protected:
    STDMETHOD(QueryContinueDrag)(
        /* [in] */ BOOL fEscapePressed,
        /* [in] */ DWORD grfKeyState);

    STDMETHOD(GiveFeedback)(
        /* [in] */ DWORD dwEffect);

    DWORD m_button;
};

#endif //GLASSDND_H

