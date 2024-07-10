/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include "RoActivationSupport.h"
#include <utility>
#include <comdef.h>
#include <winstring.h>
#include <jni.h>

namespace
{
    typedef HRESULT WINAPI FnRoInitialize(RO_INIT_TYPE initType);
    typedef void WINAPI FnRoUninitialize();
    typedef HRESULT WINAPI FnRoActivateInstance(HSTRING activatableClassId, IInspectable** instance);
    typedef HRESULT WINAPI FnWindowsCreateString(PCNZWCH sourceString, UINT32 length, HSTRING* string);
    typedef HRESULT WINAPI FnWindowsDeleteString(HSTRING string);

    bool initialized = false;
    const char* moduleNotFoundMessage = "WinRT: %s not found\n";
    HMODULE hLibComBase = NULL;
    FnRoInitialize* pRoInitialize = NULL;
    FnRoUninitialize* pRoUninitialize = NULL;
    FnRoActivateInstance* pRoActivateInstance = NULL;
    FnWindowsCreateString* pWindowsCreateString = NULL;
    FnWindowsDeleteString* pWindowsDeleteString = NULL;

    template<class T>
    bool loadFunction(HMODULE lib, T*& fnptr, const char* name)
    {
        fnptr = reinterpret_cast<T*>(GetProcAddress(lib, name));
        if (fnptr == NULL) {
            fprintf(stderr, "GetProcAddress: %s not loaded\n", name);
            initialized = false;
            return false;
        }

        return true;
    }
}

void tryInitializeRoActivationSupport()
{
    if (initialized) {
        return;
    }

    wchar_t path[MAX_PATH];
    wchar_t file[MAX_PATH];

    UINT pathSize = sizeof(path) / sizeof(wchar_t);
    UINT rval = GetSystemDirectoryW(path, pathSize);
    if (rval == 0 || rval >= pathSize) {
        fprintf(stderr, "WinRT: Failed to fetch system directory");
        return;
    }

    memcpy_s(file, sizeof(file), path, sizeof(path));
    if (wcscat_s(file, MAX_PATH-1, L"\\combase.dll") != 0) {
        fprintf(stderr, "WinRT: Failed to form path to combase.dll");
        return;
    }

    hLibComBase = LoadLibraryW(file);
    if (!hLibComBase) {
        fprintf(stderr, moduleNotFoundMessage, "combase.dll");
        return;
    }

    bool loaded =
        loadFunction(hLibComBase, pRoInitialize, "RoInitialize") &&
        loadFunction(hLibComBase, pRoUninitialize, "RoUninitialize") &&
        loadFunction(hLibComBase, pRoActivateInstance, "RoActivateInstance") &&
        loadFunction(hLibComBase, pWindowsCreateString, "WindowsCreateString") &&
        loadFunction(hLibComBase, pWindowsDeleteString, "WindowsDeleteString");

    if (!loaded) {
        uninitializeRoActivationSupport();
    } else {
        HRESULT res = RoInitialize(RO_INIT_SINGLETHREADED);
        if (FAILED(res)) {
            fprintf(stderr, RoException("RoInitialize failed: ", res).message());
            uninitializeRoActivationSupport();
        } else {
            initialized = true;
        }
    }
}

void uninitializeRoActivationSupport()
{
    if (initialized) {
        RoUninitialize();
    }

    initialized = false;

    if (hLibComBase) {
        FreeLibrary(hLibComBase);
        hLibComBase = NULL;
        pRoInitialize = NULL;
        pRoUninitialize = NULL;
        pRoActivateInstance = NULL;
        pWindowsCreateString = NULL;
        pWindowsDeleteString = NULL;
    }
}

bool isRoActivationSupported()
{
    return initialized;
}

HRESULT WINAPI RoInitialize(RO_INIT_TYPE initType)
{
    return pRoInitialize(initType);
}

void WINAPI RoUninitialize()
{
    pRoUninitialize();
}

HRESULT WINAPI RoActivateInstance(HSTRING activatableClassId, IInspectable** instance)
{
    return pRoActivateInstance(activatableClassId, instance);
}

HRESULT WINAPI WindowsCreateString(PCNZWCH sourceString, UINT32 length, HSTRING* string)
{
    return pWindowsCreateString(sourceString, length, string);
}

HRESULT WINAPI WindowsDeleteString(HSTRING string)
{
    return pWindowsDeleteString(string);
}

hstring::hstring(const char* str) : hstr_(NULL)
{
    int wstr_len = MultiByteToWideChar(CP_UTF8, 0, str, -1, NULL, 0);
    if (wstr_len == 0) return;

    WCHAR* wstr = new WCHAR[wstr_len];
    if (wstr == NULL) return;

    memset(wstr, 0, wstr_len * sizeof(WCHAR));
    if (MultiByteToWideChar(CP_UTF8, 0, str, -1, wstr, wstr_len) > 0) {
        WindowsCreateString(wstr, wstr_len - 1, &hstr_);
    }

    delete[] wstr;
}

hstring::~hstring()
{
    if (hstr_ != NULL) {
        WindowsDeleteString(hstr_);
    }
}

hstring::operator HSTRING()
{
    return hstr_;
}

RoException::RoException(const char* message) : message_(NULL)
{
    if (message == NULL) {
        message = "";
    }

    size_t len = strlen(message);
    if (len == 0) return;

    char* msg = new char[len + 1];
    if (msg == NULL) return;

    strcpy_s(msg, len + 1, message);
    message_ = msg;
}

RoException::RoException(const char* message, HRESULT res) : message_(NULL)
{
    if (message == NULL) {
        message = "";
    }

    const wchar_t* error = _com_error(res).ErrorMessage();
    if (error == NULL) return;

    int error_length = WideCharToMultiByte(CP_ACP, 0, error, -1, NULL, 0, NULL, FALSE);
    if (error_length == 0) return;

    int message_length = int(strlen(message));
    char* result = new char[message_length + error_length];
    if (result == NULL) return;

    // Concatenate the "message" and "error" strings.
    WideCharToMultiByte(CP_ACP, 0, error, -1, result + message_length, error_length, NULL, FALSE);
    memcpy_s(result, message_length, message, message_length);
    message_ = result;
}

RoException::RoException(const RoException& source) :
    RoException(source.message()) {}

RoException::~RoException()
{
    if (message_ != NULL) {
        delete[] message_;
    }
}

RoException& RoException::operator=(RoException source)
{
    std::swap(message_, source.message_);
    return *this;
}

const char* RoException::message() const
{
    return message_ != NULL ? message_ : "";
}
