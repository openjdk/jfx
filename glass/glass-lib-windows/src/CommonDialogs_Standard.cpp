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

#include <Commdlg.h>
#include <Shlobj.h>

#include "CommonDialogs_Standard.h"

#include "com_sun_glass_ui_CommonDialogs_Type.h"

/*************************************************
 * GetOpenFileName/GetSaveFileName implementation
 *************************************************/

extern void ConvertFilter(jobject jFilter,  DNTString &filters);

void ConvertFilters(jobjectArray jFilters, DNTString &filters)
{
    JNIEnv *env = GetEnv();

    jsize size = env->GetArrayLength(jFilters);
    for (int i = 0; i < size; i++) {
        jobject jFilter = env->GetObjectArrayElement(jFilters, i);
        ConvertFilter(jFilter, filters);
    }
    filters.append(_T("\0"), 1, true);
}

void ConvertFilter(jobject jFilter, DNTString &filters)
{
    JNIEnv* env = GetEnv();

    jstring jDesc = (jstring)env->CallObjectMethod(jFilter, javaIDs.CommonDialogs.ExtensionFilter.getDescription);
    CheckAndClearException(env);

    JString desc(env, jDesc);
    filters.append(desc, wcslen(desc), true);
    filters.append(_T("\0"), 1, true);

    jobjectArray jExtensions = (jobjectArray)env->CallObjectMethod(jFilter,
                                    javaIDs.CommonDialogs.ExtensionFilter.extensionsToArray);
    CheckAndClearException(env);

    jsize size = env->GetArrayLength(jExtensions);

    BOOL isHeading = TRUE; // extension without semicolon

    for (int i = 0; i < size; i++) {
        if (!isHeading) {
            filters.append(_T(";"), 1, true);
        }
        isHeading = FALSE;

        jstring jExtension = (jstring)env->GetObjectArrayElement(jExtensions, i);
        JString extension(env, jExtension);

        filters.append(extension, wcslen(extension), true);
    }

    filters.append(_T("\0"), 1, true);
}

jobjectArray ConvertFiles(DNTString &files)
{
    jobjectArray ret = NULL;
    JNIEnv *env = GetEnv();
    JLClass cls(env, env->FindClass("java/lang/String"));

    UINT count = files.count();

    if (count == 0)      // the user cancels the file chooser
    {
        ret = env->NewObjectArray(0, cls, NULL);
    }
    else if (count == 1) // the user selects one file
    {
        ret = env->NewObjectArray(1, cls, NULL);

        // there's no null delimiter b/w dir and file in this case
        JLString name(env, CreateJString(env, files));

        env->SetObjectArrayElement(ret, 0, name);
        CheckAndClearException(env);
    }
    else if (count > 1)  // the user selects multiple files
    {
        // ignore one item as it's for folder
        ret = env->NewObjectArray(count-1, cls, NULL);

        JLString dir(env, CreateJString(env, files.substring(0)));
        JLString backslash(env, CreateJString(env, _T("\\")));
        JLString dirWithBackslash(env, ConcatJStrings(env, dir, backslash));

        for (UINT i = 1; i < count; i++)
        {           
            JLString shortname(env, CreateJString(env, files.substring(i)));
            JLString name(env, ConcatJStrings(env, dirWithBackslash, shortname));

            env->SetObjectArrayElement(ret, i-1, name);
            CheckAndClearException(env);
        }
    }
    return ret;
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

jobject StandardFileChooser_Show(HWND owner, LPCTSTR folder, LPCTSTR filename, LPCTSTR title, jint type,
                                      jboolean multipleMode, jobjectArray jFilters)
{
    DNTString files(MAX_PATH);
    DNTString filters(MAX_PATH);

    if (jFilters != NULL) {
        ConvertFilters(jFilters, filters);
    }

    if (type == com_sun_glass_ui_CommonDialogs_Type_SAVE && filename && *filename) {
        files.append(filename, wcslen(filename));
    } else {
        ((LPTSTR)files)[0] = L'\0';
    }

    OPENFILENAME ofn = {0};
    ofn.lStructSize       = sizeof(OPENFILENAME);
    ofn.hwndOwner         = owner;
    ofn.lpstrFilter       = filters;
    ofn.nFilterIndex      = 1;
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

    JNIEnv *env = GetEnv();
    jobjectArray retValue;

    if (!ret) {
        JLClass cls(env, env->FindClass("java/lang/String"));
        retValue = env->NewObjectArray(0, cls, NULL);
    } else {
        files.calculateLength();  // the result is stored in the files variable
        retValue = ConvertFiles(files);
    }

    JLClass cls(env, env->FindClass("com/sun/glass/ui/CommonDialogs"));
    return env->CallStaticObjectMethod(cls, javaIDs.CommonDialogs.createFileChooserResult,
            retValue, jFilters, (jint)(ofn.nFilterIndex - 1));
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

jstring StandardFolderChooser_Show(HWND owner, LPCTSTR folder, LPCTSTR title)
{
    OLEHolder _ole_;
    JNIEnv *env = GetEnv();

    BROWSEINFO bi = {0};
    bi.hwndOwner = owner;
    bi.lpszTitle = title;
    bi.ulFlags   = BIF_USENEWUI;
    bi.lpfn      = FolderChooserCallbackProc;
    bi.lParam    = (LPARAM) folder;

    LPITEMIDLIST p = ::SHBrowseForFolder(&bi);
    if (!p) {
        return NULL;
    }

    wchar_t selectedFolder[MAX_PATH] = _T("");
    if (SHGetTargetFolderPath(p, selectedFolder) != S_OK) {
        return NULL;
    }
    return CreateJString(env, selectedFolder);
}
