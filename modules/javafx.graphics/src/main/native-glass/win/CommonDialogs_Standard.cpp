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

#include <Commdlg.h>
#include <Shlobj.h>

#include <string>
#include <vector>

#include "CommonDialogs_Standard.h"
#include "GlassStringBlock.h"

#include "com_sun_glass_ui_CommonDialogs_Type.h"

/*************************************************
 * GetOpenFileName/GetSaveFileName implementation
 *************************************************/

/*
 * JNI-free: the ExtensionFilter getters and the String[] building moved to Java, the
 * caller of gwin_dialog_file (ABI 4, the only path); the DNTString filter and result handling is
 * unchanged.
 */

// The former ConvertFilter / ConvertFilters over the flattened GwinFileFilter: the description, its
// NUL, the ';'-joined extensions (joined by the caller now), its NUL - then the closing NUL.
static void ConvertFilters(const GwinFileFilter* filters, int32_t filterCount, DNTString &filtersOut)
{
    for (int32_t i = 0; i < filterCount; i++) {
        const wchar_t* desc = reinterpret_cast<const wchar_t*>(filters[i].description);
        const wchar_t* extensions = reinterpret_cast<const wchar_t*>(filters[i].extensions);

        filtersOut.append(desc, wcslen(desc), true);
        filtersOut.append(_T("\0"), 1, true);

        filtersOut.append(extensions, wcslen(extensions), true);
        filtersOut.append(_T("\0"), 1, true);
    }
    filtersOut.append(_T("\0"), 1, true);
}

// The former ConvertFiles: the OFN buffer as full paths.
static void ConvertFiles(DNTString &files, std::vector<std::wstring> &out)
{
    UINT count = files.count();

    if (count == 0)      // the user cancels the file chooser
    {
        // nothing
    }
    else if (count == 1) // the user selects one file
    {
        // there's no null delimiter b/w dir and file in this case
        out.push_back(std::wstring((LPCWSTR)(LPWSTR) files));
    }
    else if (count > 1)  // the user selects multiple files
    {
        // ignore one item as it's for folder
        std::wstring dirWithBackslash(files.substring(0));
        dirWithBackslash += _T("\\");

        for (UINT i = 1; i < count; i++)
        {
            out.push_back(dirWithBackslash + files.substring(i));
        }
    }
}

/*
 * Implemented in the same way as shown in the example from:
 * http://support.microsoft.com/kb/131462
 */
UINT_PTR CALLBACK DialogHook(HWND hwnd, UINT uMsg, WPARAM wParam,
                                 LPARAM lParam)
{
    LPOFNOTIFY lpofn;
    unsigned int cbLength;

    switch (uMsg)
    {
        case WM_INITDIALOG:
            ::SetProp(GetParent(hwnd), TEXT("OFN"), (void *) lParam);
            break;
        case WM_NOTIFY:
            // The OFNOTIFY struct is passed in the lParam of this message.
            lpofn = (LPOFNOTIFY) lParam;
            if (lpofn->hdr.code == CDN_SELCHANGE)
            {
                LPOPENFILENAME lpofn;
                cbLength = CommDlg_OpenSave_GetSpec(GetParent(hwnd), NULL, 0);
                cbLength += _MAX_PATH;
                // The OFN struct is stored in a property of dialog window
                lpofn = (LPOPENFILENAME) GetProp(GetParent(hwnd), TEXT("OFN"));
                if (lpofn->nMaxFile < cbLength) {
                    DNTString *files = (DNTString*)lpofn->lCustData;
                    files->setLimit(cbLength);
                    if (*files) {
                        lpofn->lpstrFile = *files;
                        lpofn->nMaxFile  = cbLength;
                    }
                }
            }
            break;
        case WM_DESTROY:
            RemoveProp(GetParent(hwnd), TEXT("OFN"));
            break;
    }
    return (0);
}

int32_t StandardFileChooser_Show(HWND owner, LPCWSTR folder, LPCWSTR filename, LPCWSTR title, int32_t type,
                                 int32_t multipleMode, const GwinFileFilter* filters, int32_t filterCount,
                                 int32_t defaultFilterIndex,
                                 uint16_t** outFiles, int32_t* outCount, int32_t* outFilterIndex)
{
    DNTString files(MAX_PATH);
    DNTString filterString(MAX_PATH);

    if (filters != NULL) {
        ConvertFilters(filters, filterCount, filterString);
    }

    if (type == com_sun_glass_ui_CommonDialogs_Type_SAVE && filename && *filename) {
        files.append(filename, wcslen(filename));
    } else {
        ((LPTSTR)files)[0] = L'\0';
    }

    OPENFILENAME ofn = {0};
    ofn.lStructSize       = sizeof(OPENFILENAME);
    ofn.hwndOwner         = owner;
    ofn.lpstrFilter       = filterString;
    ofn.nFilterIndex      = defaultFilterIndex + 1; // nFilterIndex is 1-based
    ofn.lpstrFile         = files;
    ofn.nMaxFile          = MAX_PATH;
    ofn.lpstrInitialDir   = folder;
    ofn.lpstrTitle        = title;
    ofn.Flags             = OFN_EXPLORER | OFN_NOCHANGEDIR;
    if (multipleMode) {
        ofn.Flags        |= OFN_ALLOWMULTISELECT;
        // to implement reallocating too small buffers:
        ofn.Flags        |= OFN_ENABLEHOOK;
        ofn.lpfnHook      = DialogHook;
        ofn.lCustData     = (LPARAM)(&files);
    }

    BOOL ret = FALSE;

    switch(type) {
        case com_sun_glass_ui_CommonDialogs_Type_OPEN:
            ret = ::GetOpenFileName(&ofn);
            break;
        case com_sun_glass_ui_CommonDialogs_Type_SAVE:
            ofn.Flags |= OFN_OVERWRITEPROMPT;
            ret = ::GetSaveFileName(&ofn);
            break;
    }

    std::vector<std::wstring> paths;
    DWORD dialogError = 0;

    if (!ret) {
        // cancel or failure: the JNI returned an empty array for both
        dialogError = ::CommDlgExtendedError();
    } else {
        files.calculateLength();  // the result is stored in the files variable
        ConvertFiles(files, paths);
    }

    *outFilterIndex = (int32_t)(ofn.nFilterIndex - 1);
    *outFiles = GwinMakeStringBlock(paths);
    *outCount = (*outFiles != NULL) ? (int32_t) paths.size() : 0;

    if (*outFiles == NULL) {
        return GWIN_DIALOG_FAILED;
    }
    if (!ret) {
        return dialogError == 0 ? GWIN_DIALOG_CANCELLED : GWIN_DIALOG_FAILED;
    }
    return GWIN_DIALOG_OK;
}

/***********************************
 * SHBrowseForFolder implementation
 ***********************************/

// Implemented in the same way as shown in the example from:
// http://msdn.microsoft.com/en-us/library/bb762115%28v=vs.85%29.aspx

int CALLBACK FolderChooserCallbackProc(HWND hwnd, UINT uMsg,
                                       LPARAM lParam,
                                       LPARAM lpData)
{
    if (uMsg == BFFM_INITIALIZED) {
        SendMessage(hwnd, BFFM_SETSELECTION, TRUE, lpData);
    }

    return 0;
}

// Retrieves the UIObject interface for the specified full PIDL
STDAPI SHGetUIObjectFromFullPIDL(LPCITEMIDLIST pidl, HWND hwnd, REFIID riid, void **ppv)
{
    LPCITEMIDLIST pidlChild;
    IShellFolder* psf;

    *ppv = NULL;

    HRESULT hr = SHBindToParent(pidl, IID_IShellFolder, (void**) &psf, &pidlChild);
    if (SUCCEEDED(hr))
    {
        hr = psf->GetUIObjectOf(hwnd, 1, &pidlChild, riid, NULL, ppv);
        psf->Release();
    }
    return hr;
}

// ILSkip and ILNext may already be defined.
#ifndef ILSkip
#   define ILSkip(pidl, cb)       ((LPITEMIDLIST)(((BYTE*)(pidl))+cb))
#endif
#ifndef ILNext
#   define ILNext(pidl)           ILSkip(pidl, (pidl)->mkid.cb)
#endif

HRESULT SHILClone(LPCITEMIDLIST pidl, LPITEMIDLIST *ppidl)
{
    DWORD cbTotal = 0;

    if (pidl)
    {
        LPCITEMIDLIST pidl_temp = pidl;
        cbTotal += sizeof (pidl_temp->mkid.cb);

        while (pidl_temp->mkid.cb)
        {
            cbTotal += pidl_temp->mkid.cb;
            pidl_temp = ILNext (pidl_temp);
        }
    }

    *ppidl = (LPITEMIDLIST)CoTaskMemAlloc(cbTotal);

    if (*ppidl)
        CopyMemory(*ppidl, pidl, cbTotal);

    return  *ppidl ? S_OK: E_OUTOFMEMORY;
}

// Get the target PIDL for a folder PIDL. This also deals with cases of a folder
// shortcut or an alias to a real folder.
STDAPI SHGetTargetFolderIDList(LPCITEMIDLIST pidlFolder, LPITEMIDLIST *ppidl)
{
    IShellLink *psl;

    *ppidl = NULL;

    HRESULT hr = SHGetUIObjectFromFullPIDL(pidlFolder, NULL, IID_IShellLink, (LPVOID*)&psl);

    if (SUCCEEDED(hr))
    {
        hr = psl->GetIDList(ppidl);
        psl->Release();
    }

    // It's not a folder shortcut so get the PIDL normally.
    if (FAILED(hr))
        hr = SHILClone(pidlFolder, ppidl);

    return hr;
}

// Get the target folder for a folder PIDL. This deals with cases where a folder
// is an alias to a real folder, folder shortcuts, the My Documents folder, and
// other items of that nature.

STDAPI SHGetTargetFolderPath(LPCITEMIDLIST pidlFolder, LPWSTR pszPath)
{
    LPITEMIDLIST pidlTarget;

    *pszPath = 0;

    HRESULT hr = SHGetTargetFolderIDList(pidlFolder, &pidlTarget);

    if (SUCCEEDED(hr))
    {
        SHGetPathFromIDListW(pidlTarget, pszPath);   // Make sure it is a path
        CoTaskMemFree(pidlTarget);
    }

    return *pszPath ? S_OK : E_FAIL;
}

int32_t StandardFolderChooser_Show(HWND owner, LPCWSTR folder, LPCWSTR title, uint16_t** outPath)
{
    OLEHolder _ole_;

    BROWSEINFO bi = {0};
    bi.hwndOwner = owner;
    bi.lpszTitle = title;
    bi.ulFlags   = BIF_USENEWUI;
    bi.lpfn      = FolderChooserCallbackProc;
    bi.lParam    = (LPARAM) folder;

    LPITEMIDLIST p = ::SHBrowseForFolder(&bi);
    if (!p) {
        *outPath = NULL;
        return GWIN_DIALOG_CANCELLED;
    }

    wchar_t selectedFolder[MAX_PATH] = _T("");
    if (SHGetTargetFolderPath(p, selectedFolder) != S_OK) {
        *outPath = NULL;
        return GWIN_DIALOG_FAILED;
    }
    *outPath = GwinMakeString(selectedFolder);
    return *outPath != NULL ? GWIN_DIALOG_OK : GWIN_DIALOG_FAILED;
}
