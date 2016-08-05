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

#include "common.h"

#include "GlassDnD.h"
#include "GlassWindow.h"

#include "com_sun_glass_ui_win_WinMenuImpl.h"
#include "com_sun_glass_ui_win_WinMenuDelegate.h"
#include "com_sun_glass_ui_win_WinMenubarDelegate.h"

static jclass jMenuClass = NULL;
static jmethodID midNotifyCommand = NULL;


// helper functions
void* GetMenuItemDataPtr(HMENU hMenu, int pos)
{
    MENUITEMINFOW itemInfo;
    memset(&itemInfo, 0, sizeof(itemInfo));
    itemInfo.cbSize = sizeof(itemInfo);
    itemInfo.fMask  = MIIM_DATA;
    if (::GetMenuItemInfo(hMenu, pos, TRUE, &itemInfo)) {
        return (void *)itemInfo.dwItemData;
    }
    return NULL;
}

// returns -1 if item not found
int FindItemBySubmenu(HMENU hMenu, HMENU hSubmenu)
{
    if (hSubmenu == NULL) {
        return -1;
    }
    int count = ::GetMenuItemCount(hMenu);
    for (int pos=0; pos<count; pos++) {
        MENUITEMINFOW itemInfo;
        memset(&itemInfo, 0, sizeof(itemInfo));
        itemInfo.cbSize = sizeof(itemInfo);
        itemInfo.fMask  = MIIM_SUBMENU;
        if (::GetMenuItemInfo(hMenu, pos, TRUE, &itemInfo)) {
            if (itemInfo.hSubMenu == hSubmenu) {
                return pos;
            }
        }
    }
    return -1;

}


bool HandleMenuCommand(HWND hWnd, WORD cmdID)
{
    if (jMenuClass != NULL && midNotifyCommand != NULL) {
        JNIEnv *env = GetEnv();
        jobject jWindow = NULL;
        GlassWindow *pWnd = GlassWindow::FromHandle(hWnd);
        if (pWnd != NULL) {
            jWindow = pWnd->GetJObject();
        }

        jboolean result = env->CallStaticBooleanMethod(jMenuClass,
                midNotifyCommand, jWindow, cmdID);
        CheckAndClearException(env);
        if (result == JNI_TRUE) {
            return true;
        }
    }
    return false;
}


extern "C" {
/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1initIDs
  (JNIEnv *env, jclass cls)
{
    jMenuClass = (jclass)env->NewGlobalRef(cls);
//private boolean notifyCommand(com.sun.glass.ui.windows.WindowsWindowDelegate, int);
//  Signature: (Lcom/sun/glass/ui/windows/WindowsWindowDelegate;I)Z
    midNotifyCommand = env->GetStaticMethodID(cls,
            "notifyCommand", "(Lcom/sun/glass/ui/Window;I)Z");
    ASSERT(midNotifyCommand);
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _create
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1create
  (JNIEnv *env, jobject jThis)
{
    return (jlong)::CreateMenu();
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _destroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1destroy
  (JNIEnv *env, jobject jThis, jlong ptr)
{
    HMENU hMenu = (HMENU)ptr;
    if (::IsMenu(hMenu)) {
        // remove items (to keep HMENU for submenus)
        int count = ::GetMenuItemCount(hMenu);
        for (int pos=count-1; pos>=0; pos--) {
            MENUITEMINFOW itemInfo;
            memset(&itemInfo, 0, sizeof(itemInfo));
            itemInfo.cbSize = sizeof(itemInfo);
            itemInfo.fMask  = MIIM_SUBMENU | MIIM_DATA;
            if (::GetMenuItemInfo(hMenu, pos, TRUE, &itemInfo)) {
                if (itemInfo.hSubMenu != NULL) {
                    ::RemoveMenu(hMenu, pos, MF_BYPOSITION);
                }
                /* we don't store callback in itemData
                if (itemInfo.dwItemData != NULL) {
                    env->DeleteGlobalRef((jobject)itemInfo.dwItemData);
                }
                */
            }
        }
        ::DestroyMenu(hMenu);
    }
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _insertItem
 * Signature: (JIILjava/lang/String;ZZLcom/sun/glass/ui/Menu/Callback;II)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1insertItem
  (JNIEnv *env, jobject jThis, jlong ptr, jint pos, jint cmdID,
    jstring title, jboolean enabled, jboolean checked,
    jobject callback, jint shortcut, jint modifiers)
{
    HMENU hMenu = (HMENU)ptr;
    if (::IsMenu(hMenu)) {
        JString strTitle(env, title);
        jobject grefCallback = callback == NULL ? NULL : env->NewGlobalRef(callback);
        MENUITEMINFOW itemInfo;
        itemInfo.cbSize     = sizeof(itemInfo);
        itemInfo.fMask      = MIIM_FTYPE | MIIM_STATE | MIIM_ID | /*MIIM_DATA | */MIIM_STRING;
        itemInfo.fType      = MFT_STRING;
        itemInfo.fState     = (enabled == JNI_TRUE ? MFS_ENABLED : MFS_GRAYED)
                            | (checked == JNI_TRUE ? MFS_CHECKED : MFS_UNCHECKED);
        itemInfo.wID        = cmdID;
        itemInfo.hSubMenu   = NULL;         // not used
        itemInfo.hbmpChecked    = NULL;     // not used
        itemInfo.hbmpUnchecked  = NULL;     // not used
        //itemInfo.dwItemData = (ULONG_PTR)grefCallback;
        itemInfo.dwItemData = NULL;
        itemInfo.dwTypeData = strTitle;
        itemInfo.cch        = strTitle.length();
        itemInfo.hbmpItem   = NULL;         // not used
        if (::InsertMenuItemW(hMenu, pos, TRUE, &itemInfo)) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _insertSubmenu
 * Signature: (JIJLjava/lang/String;Z)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1insertSubmenu
  (JNIEnv *env, jobject jThis, jlong ptr, jint pos, jlong submenuPtr,
    jstring title, jboolean enabled)
{
    HMENU hMenu = (HMENU)ptr;
    HMENU hSubmenu = (HMENU)submenuPtr;
    if (::IsMenu(hMenu) && ::IsMenu(hSubmenu)) {
        JString strTitle(env, title);
        MENUITEMINFOW itemInfo;
        itemInfo.cbSize     = sizeof(itemInfo);
        itemInfo.fMask      = MIIM_FTYPE | MIIM_STATE | MIIM_STRING | MIIM_SUBMENU;
        itemInfo.fType      = MFT_STRING;
        itemInfo.fState     = (enabled == JNI_TRUE ? MFS_ENABLED : MFS_GRAYED);
        itemInfo.wID        = 0;            // not used
        itemInfo.hSubMenu   = hSubmenu;
        itemInfo.hbmpChecked    = NULL;     // not used
        itemInfo.hbmpUnchecked  = NULL;     // not used
        itemInfo.dwItemData = NULL;         // not used
        itemInfo.dwTypeData = strTitle;
        itemInfo.cch        = strTitle.length();
        itemInfo.hbmpItem   = NULL;         // not used
        if (::InsertMenuItemW(hMenu, pos, TRUE, &itemInfo)) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _insertSeparator
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1insertSeparator
  (JNIEnv *env, jobject jThis, jlong ptr, jint pos)
{
    HMENU hMenu = (HMENU)ptr;
    if (::IsMenu(hMenu)) {
        return bool_to_jbool(::InsertMenu(hMenu, pos, MF_SEPARATOR, NULL, NULL));
    }
    return JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _removeAtPos
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1removeAtPos
  (JNIEnv *env, jobject jThis, jlong ptr, jint pos)
{
    HMENU hMenu = (HMENU)ptr;
    if (::IsMenu(hMenu)) {
        /* we don't store callback in itemData
        // if item to delete is MenuItem, free global ref to callback
        void *data = GetMenuItemDataPtr(hMenu, pos);
        if (data != NULL) {
            env->DeleteGlobalRef((jobject)data);
        }
        */
        if (::RemoveMenu(hMenu, pos, MF_BYPOSITION)) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _setItemTitle
 * Signature: (JILjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1setItemTitle
  (JNIEnv *env, jobject jThis, jlong ptr, jint cmdID, jstring title)
{
    HMENU hMenu = (HMENU)ptr;
    if (::IsMenu(hMenu)) {
        JString strTitle(env, title);
        MENUITEMINFOW itemInfo;
        memset(&itemInfo, 0, sizeof(itemInfo));
        itemInfo.cbSize     = sizeof(itemInfo);
        itemInfo.fMask      = MIIM_STRING;
        itemInfo.dwTypeData = strTitle;
        itemInfo.cch        = strTitle.length();
        if (::SetMenuItemInfoW(hMenu, cmdID, FALSE, &itemInfo)) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _setSubmenuTitle
 * Signature: (JJLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1setSubmenuTitle
  (JNIEnv *env, jobject jThis, jlong ptr, jlong subPtr, jstring title)
{
    HMENU hMenu = (HMENU)ptr;
    HMENU hSubmenu = (HMENU)subPtr;
    if (::IsMenu(hMenu)) {
        int pos = FindItemBySubmenu(hMenu, hSubmenu);
        if (pos > 0) {
            JString strTitle(env, title);
            MENUITEMINFOW itemInfo;
            memset(&itemInfo, 0, sizeof(itemInfo));
            itemInfo.cbSize     = sizeof(itemInfo);
            itemInfo.fMask      = MIIM_STRING;
            itemInfo.dwTypeData = strTitle;
            itemInfo.cch        = strTitle.length();
            if (::SetMenuItemInfoW(hMenu, pos, TRUE, &itemInfo)) {
                return JNI_TRUE;
            }
        }
    }
    return JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _enableItem
 * Signature: (JIZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1enableItem
  (JNIEnv *env, jobject jThis, jlong ptr, jint cmdID, jboolean enable)
{
    HMENU hMenu = (HMENU)ptr;
    if (::IsMenu(hMenu)) {
        if (0 <= EnableMenuItem(hMenu, cmdID,
                MF_BYCOMMAND | (enable == JNI_TRUE ? MF_ENABLED : MF_GRAYED))) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _enableSubmenu
 * Signature: (JJZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1enableSubmenu
  (JNIEnv *env, jobject jThis, jlong ptr, jlong subPtr, jboolean enable)
{
    HMENU hMenu = (HMENU)ptr;
    HMENU hSubmenu = (HMENU)subPtr;
    if (::IsMenu(hMenu)) {
        int pos = FindItemBySubmenu(hMenu, hSubmenu);
        if (pos > 0) {
            if (0 <= EnableMenuItem(hMenu, pos,
                    MF_BYPOSITION | (enable == JNI_TRUE ? MF_ENABLED : MF_GRAYED))) {
                return JNI_TRUE;
            }
        }
    }
    return JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_win_WinMenuImpl
 * Method:    _checkItem
 * Signature: (JIZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinMenuImpl__1checkItem
  (JNIEnv *env, jobject jThis, jlong ptr, jint cmdID, jboolean check)
{
    HMENU hMenu = (HMENU)ptr;
    if (::IsMenu(hMenu)) {
        if (0 <= ::CheckMenuItem(hMenu, cmdID,
                MF_BYCOMMAND | (check == JNI_TRUE ? MF_CHECKED : MF_UNCHECKED))) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}


}   // extern "C"

