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

#include <ShObjIdl.h>

#include <string>
#include <vector>

#include "CommonDialogs_COM.h"
#include "GlassStringBlock.h"

#include "com_sun_glass_ui_CommonDialogs_Type.h"

/*****************************
 * IFileDialog implementation
 *****************************/

/*
 * JNI-free: the ExtensionFilter getters and the String[] / FileChooserResult building
 * moved to Java, the caller of gwin_dialog_file (ABI 4, the only path), and what remains is the COM
 * half of every former helper, with the same OLE_TRY structure and the same
 * failure shapes - see the comments at each former JNI boundary.
 */

_COM_SMARTPTR_TYPEDEF(IFileDialog, __uuidof(IFileDialog));
_COM_SMARTPTR_TYPEDEF(IFileOpenDialog, __uuidof(IFileOpenDialog));
_COM_SMARTPTR_TYPEDEF(IShellItem, __uuidof(IShellItem));
_COM_SMARTPTR_TYPEDEF(IShellItemArray, __uuidof(IShellItemArray));

#if (_WIN32_IE < _WIN32_IE_IE70)
SHSTDAPI SHCreateItemFromParsingName(__in PCWSTR pszPath, __in_opt IBindCtx *pbc, __in REFIID riid, __deref_out void **ppv);
#endif  // (_WIN32_IE < _WIN32_IE_IE70)

const HRESULT CANCEL_HRT = HRESULT_FROM_WIN32(ERROR_CANCELLED);

// The former CreateJString(env, IShellItemPtr): the item's file-system path, or false where the JNI
// produced a null jstring (a NULL item, or GetDisplayName failed).
static bool GetShellItemPath(IShellItemPtr pFile, std::wstring &out)
{
    LPWSTR path = NULL;

    OLE_TRY
    OLE_HRT( pFile->GetDisplayName(SIGDN_FILESYSPATH, &path) );
    OLE_CATCH

    if (path == NULL) {
        return false;
    }
    out = path;
    CoTaskMemFree(path);
    return true;
}

// The COM half of the former SetFilters: the description / extension strings arrive flattened.
static void SetFilters(IFileDialogPtr pDialog, const GwinFileFilter* filters, int32_t filterCount,
                       int32_t defaultFilterIndex)
{
    // one spare element so that &filterSpec[0] is a valid (non-NULL) pointer for an empty array, as
    // the former new COMDLG_FILTERSPEC[0] was
    std::vector<COMDLG_FILTERSPEC> filterSpec(size_t(filterCount > 0 ? filterCount : 0) + 1);

    for (int32_t i = 0; i < filterCount; i++) {
        filterSpec[i].pszName = reinterpret_cast<LPCWSTR>(filters[i].description);
        filterSpec[i].pszSpec = reinterpret_cast<LPCWSTR>(filters[i].extensions);
    }

    OLE_TRY
    OLE_HRT( pDialog->SetDefaultExtension(L"") );
    OLE_HRT( pDialog->SetFileTypes(filterCount > 0 ? filterCount : 0, &filterSpec[0]) );
    if (filterCount > 0) {
        OLE_HRT( pDialog->SetFileTypeIndex(defaultFilterIndex + 1) ); // 1-based index required
    }
    OLE_CATCH
}

/*
 * The COM half of the former GetFiles. Returns false exactly where the JNI returned a NULL array
 * (which made createFileChooserResult throw NullPointerException and the entry point return null):
 * an OPEN dialog whose results could not be read. Cancel is an empty list; a SAVE dialog whose
 * GetResult failed is an empty list too (the JNI's one-element array held a null the Java skipped);
 * an OPEN item whose path could not be read is skipped for the same reason.
 */
static bool GetFiles(IFileDialogPtr pDialog, BOOL isCancelled, int32_t type, std::vector<std::wstring> &files)
{
    if (isCancelled) {
        return true;
    }

    bool haveArray = false;

    OLE_TRY
    if (type == com_sun_glass_ui_CommonDialogs_Type_SAVE) {
        haveArray = true;   // the JNI allocated its one-element array before GetResult

        IShellItemPtr pFile;
        OLE_HRT( pDialog->GetResult(&pFile) );
        OLE_CHECK_NOTNULLSP(pFile)

        std::wstring path;
        if (GetShellItemPath(pFile, path)) {
            files.push_back(path);
        }
        return true;
    }

    IFileOpenDialogPtr pOpenDialog(pDialog);
    OLE_CHECK_NOTNULLSP(pOpenDialog)

    IShellItemArrayPtr pFiles;
    OLE_HRT( pOpenDialog->GetResults(&pFiles) );
    OLE_CHECK_NOTNULLSP(pFiles)

    DWORD count = 0;
    OLE_HRT( pFiles->GetCount(&count) );

    haveArray = true;   // the JNI allocated its count-element array here

    for (DWORD i = 0; i < count; i++) {
        IShellItemPtr pFile;
        OLE_HRT( pFiles->GetItemAt(i, &pFile) );
        OLE_CHECK_NOTNULLSP(pFile)

        std::wstring path;
        if (GetShellItemPath(pFile, path)) {
            files.push_back(path);
        }
    }
    OLE_CATCH

    return haveArray;
}

int32_t COMFileChooser_Show(HWND owner, LPCWSTR folder, LPCWSTR filename, LPCWSTR title, int32_t type,
                            int32_t multipleMode, const GwinFileFilter* filters, int32_t filterCount,
                            int32_t defaultFilterIndex,
                            uint16_t** outFiles, int32_t* outCount, int32_t* outFilterIndex)
{
    OLEHolder _ole_;
    IFileDialogPtr pDialog;
    HRESULT showResult = E_FAIL;

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

    if (filters != NULL) {
        SetFilters(pDialog, filters, filterCount, defaultFilterIndex);
    }

    OLE_HR = pDialog->Show(owner);
    showResult = OLE_HR;
    if (OLE_HR != CANCEL_HRT && FAILED(OLE_HR)) {
        OLE_THROW_LASTERROR(_T("pDialog->Show(NULL)"))
    }
    OLE_CATCH

    std::vector<std::wstring> files;
    const bool haveFiles = GetFiles(pDialog, OLE_HR == CANCEL_HRT, type, files);

    // Outside any try block on a possibly-NULL pDialog, as it always was (a failed CoCreateInstance
    // throws _com_error out of here); the export's catch reports it, the JNI entry point did not.
    UINT index = 0;
    pDialog->GetFileTypeIndex(&index);

    *outFilterIndex = (int32_t)(index - 1);
    *outFiles = haveFiles ? GwinMakeStringBlock(files) : NULL;
    *outCount = (*outFiles != NULL) ? (int32_t) files.size() : 0;

    if (*outFiles == NULL) {
        return GWIN_DIALOG_FAILED;
    }
    if (showResult == CANCEL_HRT) {
        return GWIN_DIALOG_CANCELLED;
    }
    return SUCCEEDED(showResult) ? GWIN_DIALOG_OK : GWIN_DIALOG_FAILED;
}

/*****************************
 * IFileDialog implementation
 *****************************/

static bool GetFolder(IFileDialogPtr pDialog, BOOL isCancelled, std::wstring &out)
{
    if (isCancelled) {
        return false;
    }

    IShellItemPtr pFile;

    OLE_TRY
    OLE_HRT( pDialog->GetResult(&pFile) );
    OLE_CATCH

    return GetShellItemPath(pFile, out);
}

int32_t COMFolderChooser_Show(HWND owner, LPCWSTR folder, LPCWSTR title, uint16_t** outPath)
{
    OLEHolder _ole_;
    IFileDialogPtr pDialog;
    HRESULT showResult = E_FAIL;

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
    showResult = OLE_HR;
    if (OLE_HR != CANCEL_HRT && FAILED(OLE_HR)) {
        OLE_THROW_LASTERROR(_T("pDialog->Show(NULL)"))
    }
    OLE_CATCH

    std::wstring path;
    *outPath = GetFolder(pDialog, OLE_HR == CANCEL_HRT, path) ? GwinMakeString(path.c_str()) : NULL;

    if (*outPath != NULL) {
        return GWIN_DIALOG_OK;
    }
    return showResult == CANCEL_HRT ? GWIN_DIALOG_CANCELLED : GWIN_DIALOG_FAILED;
}
