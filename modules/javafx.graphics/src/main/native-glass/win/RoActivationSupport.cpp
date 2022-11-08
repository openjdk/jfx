/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
#include <comdef.h>
#include <winstring.h>
#include <jni.h>

namespace
{
    bool initialized = false;
    const char* moduleNotFoundMessage = "WinRT: %s not found\n";

    const char* catStrW(const char* s1, const wchar_t* s2w)
    {
        int s1_len = int(strlen(s1));
        int s2_len = WideCharToMultiByte(CP_ACP, 0, s2w, -1, NULL, 0, NULL, FALSE);
        char* res = new char[s1_len + s2_len];
        WideCharToMultiByte(CP_ACP, 0, s2w, -1, res + s1_len, s2_len, NULL, FALSE);
        memcpy_s(res, s1_len + s2_len, s1, s1_len);
        return res;
    }

    typedef HRESULT WINAPI FnRoInitialize(RO_INIT_TYPE initType);
    typedef void WINAPI FnRoUninitialize();
    typedef HRESULT WINAPI FnRoActivateInstance(HSTRING activatableClassId, IInspectable** instance);
    typedef HRESULT WINAPI FnWindowsCreateString(PCNZWCH sourceString, UINT32 length, HSTRING* string);
    typedef HRESULT WINAPI FnWindowsDeleteString(HSTRING string);

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
        if (fnptr == nullptr) {
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

    if (GetSystemDirectory(path, sizeof(path) / sizeof(wchar_t)) == 0) {
        return;
    }

    memcpy_s(file, sizeof(file), path, sizeof(path));
    wcscat_s(file, MAX_PATH-1, L"\\combase.dll");
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

hstring::hstring(const char* str)
{
    int wstr_len = MultiByteToWideChar(CP_UTF8, 0, str, -1, nullptr, 0);
    WCHAR* wstr = new WCHAR[wstr_len];
    memset(wstr, 0, wstr_len * sizeof(WCHAR));
    MultiByteToWideChar(CP_UTF8, 0, str, -1, wstr, wstr_len);
    WindowsCreateString(wstr, wstr_len - 1, &hstr_);
    delete[] wstr;
}

hstring::~hstring()
{
    WindowsDeleteString(hstr_);
}

hstring::operator HSTRING()
{
    return hstr_;
}

RoException::RoException(const char* message)
{
    size_t len = strlen(message);
    char* msg = new char[len + 1];
    strcpy_s(msg, len + 1, message);
    message_ = msg;
}

RoException::RoException(const char* message, HRESULT res)
{
    message_ = catStrW(message, _com_error(res).ErrorMessage());
}

RoException::RoException(const RoException& source) : RoException(source.message()) {}

RoException::RoException(RoException&& source)
{
    message_ = source.message_;
    source.message_ = nullptr;
}

RoException::~RoException()
{
    if (message_ != nullptr) {
        delete[] message_;
    }
}

RoException& RoException::operator=(const RoException& source)
{
    if (message_ != nullptr) {
        delete[] message_;
    }

    size_t len = strlen(source.message());
    char* msg = new char[len + 1];
    strcpy_s(msg, len + 1, source.message());
    message_ = msg;

    return *this;
}

RoException& RoException::operator=(RoException&& source)
{
    if (message_ != nullptr) {
        delete[] message_;
    }

    message_ = source.message_;
    source.message_ = nullptr;

    return *this;
}

const char* RoException::message() const
{
    return message_;
}
