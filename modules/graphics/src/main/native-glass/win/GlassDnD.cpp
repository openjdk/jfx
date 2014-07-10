/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
    JNIEnv *env = GetEnv();
    //Get "DnD" clipboard
    JLClass jcWinDnDClipboard(env,
        GlassApplication::ClassForName(env, "com.sun.glass.ui.win.WinDnDClipboard"));
    ASSERT(jcWinDnDClipboard)
    static jmethodID midGetInstance = env->GetStaticMethodID(jcWinDnDClipboard, "getInstance",
        "()Lcom/sun/glass/ui/win/WinDnDClipboard;");
    ASSERT(midGetInstance)
    HRESULT result = checkJavaException(env);
    if (result != S_OK) {
        return result;
    }
    JLObject jDnDClipboard(env, env->CallStaticObjectMethod(jcWinDnDClipboard, midGetInstance));
    ASSERT(jDnDClipboard)

    IDataObject *pOldDataObj = getPtr(env, jDnDClipboard);
    if (pOldDataObj != pDataObj) {
        if (NULL != pDataObj) {
            //lock it till clipboard close
            pDataObj->AddRef();
        }
        setPtr(env, jDnDClipboard, pDataObj);
        if (NULL != pOldDataObj) {
            //unlock old data instance
            pOldDataObj->Release();
        }
    }

    return checkJavaException(env);
}

HRESULT  GlassDropTarget::CallbackToJava(
    /* [in] */ jmethodID method,
    /* [in] */ DWORD grfKeyState,
    /* [in] */ POINTL pt,
    /* [out][in] */ DWORD *pdwEffect)
{
    if (!m_viewContainer->GetView()) {
        return S_OK;
    }

    JNIEnv *env = GetEnv();
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

    *pdwEffect = getDROPEFFECT(DROPEFFECT(env->CallIntMethod(m_viewContainer->GetView(), method,
        jint(ptClient.x), jint(ptClient.y), jint(pt.x), jint(pt.y), getACTION(like))));

    return checkJavaException(env);
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
    OLE_HRT(CallbackToJava(javaIDs.View.notifyDragEnter, grfKeyState, pt, pdwEffect))
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
    OLE_HRT(OLE_HRT(CallbackToJava(javaIDs.View.notifyDragOver, grfKeyState, pt, pdwEffect)))
    //ignore HRESULT - just no image
    m_spDropTargetHelper->DragOver((LPPOINT)&pt, *pdwEffect);
    OLE_CATCH
    OLE_RETURN_HR
}

HRESULT GlassDropTarget::DragLeave()
{
    if (!m_viewContainer->GetView()) {
        return S_OK;
    }

    OLE_TRY
    JNIEnv *env = GetEnv();
    //View.notifyDragLeave()
    env->CallIntMethod(m_viewContainer->GetView(), javaIDs.View.notifyDragLeave);
    OLE_HRT(checkJavaException(env))
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
    OLE_HRT(OLE_HRT(CallbackToJava(javaIDs.View.notifyDragDrop, grfKeyState, pt, pdwEffect)))
    //ignore HRESULT - just no image
    m_spDropTargetHelper->Drop(pDataObj, (LPPOINT)&pt, *pdwEffect);
    OLE_CATCH
    OLE_RETURN_HR
}

/*static*/
HRESULT GlassDropTarget::SetSourceSupportedActions(/*in*/jint actions)
{
    JNIEnv *env = GetEnv();
    //Get "DnD" clipboard
    JLClass jcWinDnDClipboard(env,
        GlassApplication::ClassForName(env, "com.sun.glass.ui.win.WinDnDClipboard"));
    ASSERT(jcWinDnDClipboard)
    static jmethodID midGetInstance = env->GetStaticMethodID(jcWinDnDClipboard, "getInstance",
        "()Lcom/sun/glass/ui/win/WinDnDClipboard;");
    ASSERT(midGetInstance)
    HRESULT result = checkJavaException(env);
    if (result != S_OK) {
        return result;
    }

    static jmethodID midSetSourceSupportedActions = env->GetMethodID(jcWinDnDClipboard, "setSourceSupportedActions",
        "(I)V");
    ASSERT(midSetSourceSupportedActions)
    result = checkJavaException(env);
    if (result != S_OK) {
        return result;
    }

    JLObject jDnDClipboard(env, env->CallStaticObjectMethod(jcWinDnDClipboard, midGetInstance));
    ASSERT(jDnDClipboard)

    env->CallVoidMethod(jDnDClipboard, midSetSourceSupportedActions, actions);

    return checkJavaException(env);
}

//////////////////////////////////////////////////////////////////////////
// GlassDropSource
//////////////////////////////////////////////////////////////////////////

/*static*/
HRESULT  GlassDropSource::SetDragButton(jint button)
{
    JNIEnv *env = GetEnv();
    //Get "DnD" clipboard
    JLClass jcWinDnDClipboard(env,
        GlassApplication::ClassForName(env, "com.sun.glass.ui.win.WinDnDClipboard"));
    ASSERT(jcWinDnDClipboard)
    static jmethodID midGetInstance = env->GetStaticMethodID(jcWinDnDClipboard, "getInstance",
        "()Lcom/sun/glass/ui/win/WinDnDClipboard;");
    ASSERT(midGetInstance)
    HRESULT result = checkJavaException(env);
    if (result != S_OK) {
        return result;
    }

    static jmethodID midSetDragButton = env->GetMethodID(jcWinDnDClipboard, "setDragButton",
        "(I)V");
    ASSERT(midSetDragButton)
    result = checkJavaException(env);
    if (result != S_OK) {
        return result;
    }

    JLObject jDnDClipboard(env, env->CallStaticObjectMethod(jcWinDnDClipboard, midGetInstance));
    ASSERT(jDnDClipboard)

    env->CallVoidMethod(jDnDClipboard, midSetDragButton, button);

    return checkJavaException(env);
}

GlassDropSource::GlassDropSource(jobject jDnDClipboard)
{
    JNIEnv *env = GetEnv();
    static jmethodID midGetDragButton = 0;
    if (0 == midGetDragButton) {
        JLClass jcWinDnDClipboard(env,
            GlassApplication::ClassForName(env, "com.sun.glass.ui.win.WinDnDClipboard"));
        ASSERT(jcWinDnDClipboard)

        midGetDragButton = env->GetMethodID(jcWinDnDClipboard, "getDragButton",
            "()I");
        ASSERT(midGetDragButton)
        HRESULT result = checkJavaException(env);
        if (result != S_OK) {
            return;
        }
    }

    jint jbutton = env->CallIntMethod(jDnDClipboard, midGetDragButton);
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
