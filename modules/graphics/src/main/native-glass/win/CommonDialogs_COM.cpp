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

#include <ShObjIdl.h>

#include "CommonDialogs_COM.h"

#include "com_sun_glass_ui_CommonDialogs_Type.h"

/*****************************
 * IFileDialog implementation
 *****************************/

_COM_SMARTPTR_TYPEDEF(IFileDialog, __uuidof(IFileDialog));
_COM_SMARTPTR_TYPEDEF(IFileOpenDialog, __uuidof(IFileOpenDialog));
_COM_SMARTPTR_TYPEDEF(IShellItem, __uuidof(IShellItem));
_COM_SMARTPTR_TYPEDEF(IShellItemArray, __uuidof(IShellItemArray));

#if (_WIN32_IE < _WIN32_IE_IE70)
SHSTDAPI SHCreateItemFromParsingName(__in PCWSTR pszPath, __in_opt IBindCtx *pbc, __in REFIID riid, __deref_out void **ppv);
#endif  // (_WIN32_IE < _WIN32_IE_IE70)

const HRESULT CANCEL_HRT = HRESULT_FROM_WIN32(ERROR_CANCELLED);

jstring CreateJString(JNIEnv *env, IShellItemPtr pFile)
{
    LPWSTR path = NULL;
    jstring ret = NULL;

    OLE_TRY
    OLE_HRT( pFile->GetDisplayName(SIGDN_FILESYSPATH, &path) );
    OLE_CATCH

    ret = CreateJString(env, path);
    CoTaskMemFree(path);
    return ret;
}

wchar_t *GetDescription(JNIEnv *env, jobject jFilter)
{
    JLString jDesc(env, (jstring)env->CallObjectMethod(jFilter,
                        javaIDs.CommonDialogs.ExtensionFilter.getDescription));
    CheckAndClearException(env);
    JString desc(env, jDesc, false);
    return desc;
}

wchar_t *GetExtensions(JNIEnv *env, jobject jFilter)
{
    JLObjectArray jExts(env, (jobjectArray)env->CallObjectMethod(jFilter,
                               javaIDs.CommonDialogs.ExtensionFilter.extensionsToArray));
    CheckAndClearException(env);

    jsize size = env->GetArrayLength(jExts);
    BOOL isHeading = TRUE; // extension without semicolon

    JLString jExt(env, CreateJString(env, _T("")));
    JLString semicolon(env, CreateJString(env, _T(";")));

    for (int j = 0; j < size; j++) {
        if (!isHeading) {
            jExt.Attach( ConcatJStrings(env, jExt, semicolon) );
        }
        isHeading = FALSE;

        JLString jExtension(env, (jstring)env->GetObjectArrayElement(jExts, j));
        jExt.Attach( ConcatJStrings(env, jExt, jExtension) );
    }

    JString ext(env, jExt, false);
    return ext;
}

void SetFilters(IFileDialogPtr pDialog, jobjectArray jFilters, jint defaultFilterIndex)
{
    JNIEnv *env = GetEnv();

    jsize size = env->GetArrayLength(jFilters);
    COMDLG_FILTERSPEC *filterSpec = new COMDLG_FILTERSPEC[size];

    for (int i = 0; i < size; i++) {
        JLObject jFilter(env, env->GetObjectArrayElement(jFilters, i));
        COMDLG_FILTERSPEC c = {GetDescription(env, jFilter),
                               GetExtensions(env, jFilter)};
        filterSpec[i] = c;
    }

    OLE_TRY
    OLE_HRT( pDialog->SetDefaultExtension(L"") );
    OLE_HRT( pDialog->SetFileTypes(size, filterSpec) );
    if (size > 0) {
        OLE_HRT( pDialog->SetFileTypeIndex(defaultFilterIndex + 1) ); // 1-based index required
    }
    OLE_CATCH

    for (int i = 0; i < size; i++) {
        if (filterSpec[i].pszName) {
            delete[] filterSpec[i].pszName;
        }
        if (filterSpec[i].pszSpec) {
            delete[] filterSpec[i].pszSpec;
        }
    }
    delete[] filterSpec;
}

jobjectArray GetFiles(IFileDialogPtr pDialog, BOOL isCancelled, jint type)
{
    JNIEnv* env = GetEnv();
    jclass jc = env->FindClass("java/lang/String");    
    if (CheckAndClearException(env)) return NULL;
    JLClass cls(env, jc);

    jobjectArray ret = NULL;

    if (isCancelled) {
        ret = env->NewObjectArray(0, cls, NULL);
        if (CheckAndClearException(env)) return NULL;
        return ret;
    }

    OLE_TRY
    if (type == com_sun_glass_ui_CommonDialogs_Type_SAVE) {
        ret = env->NewObjectArray(1, cls, NULL);
        if (CheckAndClearException(env)) return NULL;

        IShellItemPtr pFile;
        OLE_HRT( pDialog->GetResult(&pFile) );
        OLE_CHECK_NOTNULLSP(pFile)

        env->SetObjectArrayElement(ret, 0,
                    jstring(JLString(env, CreateJString(env, pFile))));
        CheckAndClearException(env);
        return ret;
    }

    IFileOpenDialogPtr pOpenDialog(pDialog);
    OLE_CHECK_NOTNULLSP(pOpenDialog)

    IShellItemArrayPtr pFiles;
    OLE_HRT( pOpenDialog->GetResults(&pFiles) );
    OLE_CHECK_NOTNULLSP(pFiles)

    DWORD count = 0;
    OLE_HRT( pFiles->GetCount(&count) );

    ret = env->NewObjectArray(count, cls, NULL);    
    if (CheckAndClearException(env)) return NULL;

    for (DWORD i = 0; i < count; i++) {
        IShellItemPtr pFile;
        OLE_HRT( pFiles->GetItemAt(i, &pFile) );
        OLE_CHECK_NOTNULLSP(pFile)

        env->SetObjectArrayElement(ret, i,
                    jstring(JLString(env, CreateJString(env, pFile))));
        CheckAndClearException(env);
    }
    OLE_CATCH

    return ret;
}

jobject COMFileChooser_Show(HWND owner, LPCTSTR folder, LPCTSTR filename, LPCTSTR title, jint type,
                                 jboolean multipleMode, jobjectArray jFilters, jint defaultFilterIndex)
{
    OLEHolder _ole_;
    IFileDialogPtr pDialog;

    OLE_TRY

    switch(type) {
        case com_sun_glass_ui_CommonDialogs_Type_OPEN:
            OLE_HRT( ::CoCreateInstance(CLSID_FileOpenDialog, NULL, CLSCTX_ALL,
                                        IID_IFileOpenDialog, (void**)&pDialog) );

            if (multipleMode == TRUE) {
                DWORD dwOptions = 0;
                OLE_HRT( pDialog->GetOptions(&dwOptions) );
                dwOptions |= FOS_ALLOWMULTISELECT;
                OLE_HRT( pDialog->SetOptions(dwOptions) );
            }

            break;
        case com_sun_glass_ui_CommonDialogs_Type_SAVE:
            OLE_HRT( ::CoCreateInstance(CLSID_FileSaveDialog, NULL, CLSCTX_ALL,
                                        IID_IFileSaveDialog, (void**)&pDialog) );
            break;
    }

    if (folder) {
        IShellItemPtr pItem;
        OLE_HRT( SHCreateItemFromParsingName((PCWSTR)folder, NULL,
                                             IID_IShellItem, (void **)&pItem) );
        if (pItem) {
            OLE_HRT( pDialog->SetFolder( pItem ) );
        }
    }

    if (type == com_sun_glass_ui_CommonDialogs_Type_SAVE && filename && *filename) {
        OLE_HRT( pDialog->SetFileName(filename); );
    }

    if (title) {
        OLE_HRT( pDialog->SetTitle(title) );
    }

    if (jFilters != NULL) {
        SetFilters(pDialog, jFilters, defaultFilterIndex);
    }

    OLE_HR = pDialog->Show(owner);
    if (OLE_HR != CANCEL_HRT && FAILED(OLE_HR)) {
        OLE_THROW_LASTERROR(_T("pDialog->Show(NULL)"))
    }
    OLE_CATCH

    jobjectArray ret = GetFiles(pDialog, OLE_HR == CANCEL_HRT, type);

    UINT index = 0;
    pDialog->GetFileTypeIndex(&index);

    JNIEnv* env = GetEnv();
    jclass jc = env->FindClass("com/sun/glass/ui/CommonDialogs");    
    if (CheckAndClearException(env)) return NULL;
    JLClass cls(env, jc);
    return env->CallStaticObjectMethod(cls, javaIDs.CommonDialogs.createFileChooserResult,
            ret, jFilters, (jint)(index - 1));
}

/*****************************
 * IFileDialog implementation
 *****************************/

jstring GetFolder(IFileDialogPtr pDialog, BOOL isCancelled)
{
    if (isCancelled) {
        return NULL;
    }

    JNIEnv* env = GetEnv();
    IShellItemPtr pFile;

    OLE_TRY
    OLE_HRT( pDialog->GetResult(&pFile) );
    OLE_CATCH

    return CreateJString(env, pFile);
}

jstring COMFolderChooser_Show(HWND owner, LPCTSTR folder, LPCTSTR title)
{
    OLEHolder _ole_;
    IFileDialogPtr pDialog;

    OLE_TRY
    OLE_HRT( ::CoCreateInstance(CLSID_FileOpenDialog, NULL, CLSCTX_ALL,
                                IID_IFileOpenDialog, (void**)&pDialog) );

    DWORD dwOptions = 0;
    OLE_HRT( pDialog->GetOptions(&dwOptions) );
    dwOptions |= FOS_PICKFOLDERS | FOS_FORCEFILESYSTEM;
    OLE_HRT( pDialog->SetOptions(dwOptions) );

    if (folder) {
        IShellItemPtr pItem;
        OLE_HRT( SHCreateItemFromParsingName((PCWSTR)folder, NULL,
                                             IID_IShellItem, (void **)&pItem) );
        if (pItem) {
            OLE_HRT( pDialog->SetFolder( pItem ) );
        }
    }

    if (title) {
        OLE_HRT( pDialog->SetTitle(title) );
    }

    OLE_HR = pDialog->Show(owner);
    if (OLE_HR != CANCEL_HRT && FAILED(OLE_HR)) {
        OLE_THROW_LASTERROR(_T("pDialog->Show(NULL)"))
    }
    OLE_CATCH

    return GetFolder(pDialog, OLE_HR == CANCEL_HRT);
}
