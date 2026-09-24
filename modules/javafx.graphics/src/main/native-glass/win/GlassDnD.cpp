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

#include "common.h"

#include "GlassDnD.h"
#include "GlassClipboard.h"
#include "GlassApplication.h"

#include "com_sun_glass_events_MouseEvent.h"

/*
 * Every site below dials GwinDndCallbacks (glass_win_api.h); with no table installed
 * (GlassDndCallbacks() == NULL) a site delivers nothing and answers S_OK, as it does without a view.
 * The drop target identifies its view by ViewContainer::GetViewId() - the view_id of glass_win_api.h's IDENTITY.
 */

//Ctrl + Shift -> ACTION_LINK
//Ctrl         -> ACTION_COPY
//Shift        -> ACTION_MOVE
GlassDropTarget::GlassDropTarget(ViewContainer *viewContainer, HWND hwnd)
{
    OLE_TRY

    m_viewContainer = viewContainer;
    m_hwnd = hwnd;

    OLE_HRT(CoCreateInstance(
        CLSID_DragDropHelper,
        NULL,
        CLSCTX_ALL,
        IID_IDropTargetHelper,
        (LPVOID*)&m_spDropTargetHelper
    ))
    OLE_HRT(::RegisterDragDrop(m_hwnd, this))
    OLE_CATCH
    STRACE(_T("{GlassDropTarget"));
}

GlassDropTarget::~GlassDropTarget()
{
    m_spDropTargetHelper = NULL;
    ::RevokeDragDrop(m_hwnd);
    STRACE(_T("}GlassDropTarget"));
}

HRESULT  GlassDropTarget::UpdateDnDClipboardData(
    IDataObject *pDataObj)
{
    const GwinDndCallbacks* cb = GlassDndCallbacks();
    if (cb == NULL) {
        return S_OK;
    }

    //Get "DnD" clipboard: dnd_get_data_object resolves WinDnDClipboard.getInstance() itself
    void* pOld = NULL;
    HRESULT result = GwinStatusToHR(cb->dnd_get_data_object(&pOld));
    if (result != S_OK) {
        return result;
    }

    IDataObject *pOldDataObj = reinterpret_cast<IDataObject *>(pOld);
    if (pOldDataObj != pDataObj) {
        if (NULL != pDataObj) {
            //lock it till clipboard close
            pDataObj->AddRef();
        }
        result = GwinStatusToHR(cb->dnd_set_data_object(pDataObj));
        if (result != S_OK) {
            //the set did not happen: give back the reference just taken, keep the old one
            if (NULL != pDataObj) {
                pDataObj->Release();
            }
            return result;
        }
        if (NULL != pOldDataObj) {
            //unlock old data instance
            pOldDataObj->Release();
        }
    }

    return S_OK;
}

HRESULT  GlassDropTarget::CallbackToJava(
    /* [in] */ DragCallback which,
    /* [in] */ DWORD grfKeyState,
    /* [in] */ POINTL pt,
    /* [out][in] */ DWORD *pdwEffect)
{
    const GwinDndCallbacks* cb = GlassDndCallbacks();
    //no view (or no table): S_OK, no upcall, *pdwEffect untouched
    if (cb == NULL || m_viewContainer->GetGlassView() == NULL) {
        return S_OK;
    }

    POINT ptClient = *(LPPOINT)&pt;
    ::ScreenToClient(m_hwnd, &ptClient);

    SetSourceSupportedActions(getACTION(*pdwEffect));

    //We want to be Explorer-like.
    //Found the action from keyboard state.
    DWORD like = DROPEFFECT_MOVE;
    grfKeyState &= MK_CONTROL | MK_SHIFT | MK_ALT;
    if ( (MK_CONTROL | MK_SHIFT)== grfKeyState || MK_ALT == grfKeyState) {
        like = DROPEFFECT_LINK;
    } else if (MK_CONTROL == grfKeyState) {
        like = DROPEFFECT_COPY;
    }

    static DROPEFFECT DesiredActions[] = {
        //Actions in order of priority (the same order is in Explorer)
        DROPEFFECT_COPY,
        DROPEFFECT_MOVE,
        DROPEFFECT_LINK
    };

    //Let's check the target ability for selected action.
    for( int iDesiredIndex = 0;

        (like & *pdwEffect) == 0 //action is not supported by target
        && iDesiredIndex < sizeof(DesiredActions)/sizeof(*DesiredActions);

        ++iDesiredIndex)
    {
        //target cannot do the action, let's try the next
        like = DesiredActions[iDesiredIndex];
    }

    //a failed slot leaves 0 here: DROPEFFECT_NONE, what CallIntMethod's 0 with a pending exception gave
    int32_t action = 0;
    int32_t (*slot)(int64_t, int32_t, int32_t, int32_t, int32_t, int32_t, int32_t*) =
        which == DRAG_ENTER ? cb->drag_enter
        : which == DRAG_OVER ? cb->drag_over
        : cb->drag_drop;
    const int32_t status = slot(m_viewContainer->GetViewId(),
        (int32_t) ptClient.x, (int32_t) ptClient.y, (int32_t) pt.x, (int32_t) pt.y,
        (int32_t) getACTION(like), &action);
    //written before the status test, as the JNI wrote it before its exception check
    *pdwEffect = getDROPEFFECT(DROPEFFECT(action));
    return GwinStatusToHR(status);
}

HRESULT GlassDropTarget::DragEnter(
        /* [unique][in] */ IDataObject *pDataObj,
        /* [in] */ DWORD grfKeyState,
        /* [in] */ POINTL pt,
        /* [out][in] */ DWORD *pdwEffect)
{
    OLE_TRY
    OLE_HRT(UpdateDnDClipboardData(pDataObj))
    //dragAction = View.notifyDragEnter(...)
    OLE_HRT(CallbackToJava(DRAG_ENTER, grfKeyState, pt, pdwEffect))
    //ignore HRESULT - just no image
    m_spDropTargetHelper->DragEnter(m_hwnd, pDataObj, (LPPOINT)&pt, *pdwEffect);
    OLE_CATCH
    OLE_RETURN_HR
}

HRESULT GlassDropTarget::DragOver(
    /* [in] */ DWORD grfKeyState,
    /* [in] */ POINTL pt,
    /* [out][in] */ DWORD *pdwEffect)
{
    OLE_TRY
    //dragAction = View.notifyDragOver(...)
    OLE_HRT(OLE_HRT(CallbackToJava(DRAG_OVER, grfKeyState, pt, pdwEffect)))
    //ignore HRESULT - just no image
    m_spDropTargetHelper->DragOver((LPPOINT)&pt, *pdwEffect);
    OLE_CATCH
    OLE_RETURN_HR
}

HRESULT GlassDropTarget::DragLeave()
{
    const GwinDndCallbacks* cb = GlassDndCallbacks();
    if (cb == NULL || m_viewContainer->GetGlassView() == NULL) {
        return S_OK;
    }

    OLE_TRY
    //View.notifyDragLeave()
    OLE_HRT(GwinStatusToHR(cb->drag_leave(m_viewContainer->GetViewId())))
    //ignore HRESULT - just no image
    m_spDropTargetHelper->DragLeave();
    OLE_CATCH
    OLE_RETURN_HR
}

HRESULT GlassDropTarget::Drop(
    /* [unique][in] */ IDataObject *pDataObj,
    /* [in] */ DWORD grfKeyState,
    /* [in] */ POINTL pt,
    /* [out][in] */ DWORD *pdwEffect)
{
    OLE_TRY
    OLE_HRT(UpdateDnDClipboardData(pDataObj))
    //performedAction = View.notifyDragDrop(...)
    OLE_HRT(OLE_HRT(CallbackToJava(DRAG_DROP, grfKeyState, pt, pdwEffect)))
    //ignore HRESULT - just no image
    m_spDropTargetHelper->Drop(pDataObj, (LPPOINT)&pt, *pdwEffect);
    OLE_CATCH
    OLE_RETURN_HR
}

/*static*/
HRESULT GlassDropTarget::SetSourceSupportedActions(/*in*/int32_t actions)
{
    const GwinDndCallbacks* cb = GlassDndCallbacks();
    if (cb == NULL) {
        return S_OK;
    }

    //WinDnDClipboard.getInstance().setSourceSupportedActions(actions), resolved by the slot
    return GwinStatusToHR(cb->dnd_set_source_supported_actions((int32_t) actions));
}

//////////////////////////////////////////////////////////////////////////
// GlassDropSource
//////////////////////////////////////////////////////////////////////////

/*static*/
HRESULT  GlassDropSource::SetDragButton(int32_t button)
{
    const GwinDndCallbacks* cb = GlassDndCallbacks();
    if (cb == NULL) {
        return S_OK;
    }

    //WinDnDClipboard.getInstance().setDragButton(button), resolved by the slot
    return GwinStatusToHR(cb->dnd_set_drag_button((int32_t) button));
}

GlassDropSource::GlassDropSource()
{
    int32_t jbutton = 0;
    const GwinDndCallbacks* cb = GlassDndCallbacks();
    if (cb != NULL) {
        //WinDnDClipboard.getDragButton(), resolved by the slot. The JNI never checked for a pending
        //exception here - a throw left 0 and made the drop immediate - so the status is not consulted.
        int32_t button = 0;
        (void) cb->dnd_get_drag_button(&button);
        jbutton = button;
    }
    switch (jbutton) {
    case com_sun_glass_events_MouseEvent_BUTTON_LEFT:
        m_button = MK_LBUTTON;
        break;
    case com_sun_glass_events_MouseEvent_BUTTON_RIGHT:
        m_button = MK_RBUTTON;
        break;
    case com_sun_glass_events_MouseEvent_BUTTON_OTHER:
        m_button = MK_MBUTTON;
        break;
    case com_sun_glass_events_MouseEvent_BUTTON_BACK:
        m_button = MK_XBUTTON1;
        break;
    case com_sun_glass_events_MouseEvent_BUTTON_FORWARD:
        m_button = MK_XBUTTON2;
        break;
    default:
        m_button = 0;
        break;
    }
    STRACE(_T("{GlassDropSource"));
}

GlassDropSource::~GlassDropSource()
{
    STRACE(_T("}GlassDropSource"));
}

HRESULT GlassDropSource::QueryContinueDrag(
    /* [in] */ BOOL fEscapePressed,
    /* [in] */ DWORD grfKeyState)
{
    return fEscapePressed
        ? DRAGDROP_S_CANCEL
        : 0 == (grfKeyState & m_button)
          ? DRAGDROP_S_DROP
          : S_OK;
}

HRESULT GlassDropSource::GiveFeedback(/* [in] */DWORD dwEffect)
{
    return DRAGDROP_S_USEDEFAULTCURSORS;
}
