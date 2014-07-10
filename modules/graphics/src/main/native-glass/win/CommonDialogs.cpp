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

#include "CommonDialogs_COM.h"
#include "CommonDialogs_Standard.h"
#include "BaseWnd.h"

class CommonDialogOwner {
    private:
        BaseWnd * wnd;
    public:
        CommonDialogOwner(HWND hwnd)
        {
            wnd = hwnd ? BaseWnd::FromHandle(hwnd) : NULL;
            if (wnd) {
                wnd->SetCommonDialogOwner(true);
            }
        }

        ~CommonDialogOwner()
        {
            if (wnd) {
                wnd->SetCommonDialogOwner(false);
            }
        }
};

/*
 * JNI methods section
 *
 */

extern "C" {

/*
 * Class:     com_sun_glass_ui_win_WinCommonDialogs
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinCommonDialogs__1initIDs
    (JNIEnv *env, jclass cls)
{
    cls = env->FindClass("com/sun/glass/ui/CommonDialogs");
    ASSERT(cls);
    if (env->ExceptionCheck()) return;

    javaIDs.CommonDialogs.createFileChooserResult = env->GetStaticMethodID(cls,
            "createFileChooserResult",
            "([Ljava/lang/String;[Lcom/sun/glass/ui/CommonDialogs$ExtensionFilter;I)Lcom/sun/glass/ui/CommonDialogs$FileChooserResult;");
    ASSERT(javaIDs.CommonDialogs.createFileChooserResult);
    if (env->ExceptionCheck()) return;

    cls = env->FindClass("com/sun/glass/ui/CommonDialogs$ExtensionFilter");
    ASSERT(cls);
    if (env->ExceptionCheck()) return;

    javaIDs.CommonDialogs.ExtensionFilter.getDescription = env->GetMethodID(cls,
                                         "getDescription", "()Ljava/lang/String;");
    ASSERT(javaIDs.CommonDialogs.ExtensionFilter.getDescription);
    if (env->ExceptionCheck()) return;

    javaIDs.CommonDialogs.ExtensionFilter.extensionsToArray = env->GetMethodID(cls,
                                         "extensionsToArray", "()[Ljava/lang/String;");
    ASSERT(javaIDs.CommonDialogs.ExtensionFilter.extensionsToArray);
    if (env->ExceptionCheck()) return;
}

/*
 * Class:     com_sun_glass_ui_win_WinCommonDialogs
 * Method:    _showFileChooser
 * Signature: (JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;IZ[Lcom/sun/glass/ui/CommonDialogs$ExtensionFilter;)Lcom/sun/glass/ui/CommonDialogs$FileChooserResult;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_win_WinCommonDialogs__1showFileChooser
  (JNIEnv *env, jobject jThis, jlong owner, jstring jFolder, jstring jFilename, jstring jTitle, jint type,
        jboolean multipleMode, jobjectArray jFilters, jint defaultFilterIndex)
{
    CommonDialogOwner cdo((HWND)jlong_to_ptr(owner));
    JString folder(env, jFolder);
    JString filename(env, jFilename);
    JString title(env, jTitle);

    if (IS_WINVISTA) {
        return COMFileChooser_Show((HWND)jlong_to_ptr(owner), folder, filename, title, type, multipleMode, jFilters, defaultFilterIndex);
    } else {
        return StandardFileChooser_Show((HWND)jlong_to_ptr(owner), folder, filename, title, type, multipleMode, jFilters, defaultFilterIndex);
    }
}

/*
 * Class:     com_sun_glass_ui_win_WinCommonDialogs
 * Method:    _showFolderChooser
 * Signature: (JLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_win_WinCommonDialogs__1showFolderChooser
  (JNIEnv *env, jclass cls, jlong owner, jstring jFolder, jstring jTitle)
{
    CommonDialogOwner cdo((HWND)jlong_to_ptr(owner));
    JString folder(env, jFolder);
    JString title(env, jTitle);

    if (IS_WINVISTA) {
        return COMFolderChooser_Show((HWND)jlong_to_ptr(owner), folder, title);
    } else {
        return StandardFolderChooser_Show((HWND)jlong_to_ptr(owner), folder, title);
    }
}

}   // extern "C"
