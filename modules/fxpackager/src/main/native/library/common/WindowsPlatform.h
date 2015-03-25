/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


#include "Platform.h"

#ifdef WINDOWS

#ifndef WINDOWSPLATFORM_H
#define WINDOWSPLATFORM_H

#include "GenericPlatform.h"
#include "JavaUserPreferences.h"

#include <Windows.h>


#pragma warning( push )
#pragma warning( disable : 4250 ) // C4250 - 'class1' : inherits 'class2::member'
class WindowsPlatform : virtual public Platform, GenericPlatform {
private:
    DWORD FMainThread;

public:
    WindowsPlatform(void);
    virtual ~WindowsPlatform(void);
    
    virtual TCHAR* ConvertStringToFileSystemString(TCHAR* Source, bool &release);
    virtual TCHAR* ConvertFileSystemStringToString(TCHAR* Source, bool &release);

    virtual void ShowMessage(TString title, TString description);
    virtual void ShowMessage(TString description);
    virtual void SetCurrentDirectory(TString Value);
    virtual TString GetPackageRootDirectory();
    virtual TString GetAppDataDirectory();
    virtual TString GetBundledJVMLibraryFileName(TString RuntimePath);
    virtual TString GetSystemJVMLibraryFileName();
    virtual TString GetSystemJRE();

    virtual PropertyContainer* GetConfigFile(TString FileName);

    virtual TString GetModuleFileName();
    virtual Module LoadLibrary(TString FileName);
    virtual void FreeLibrary(Module AModule);
    virtual Procedure GetProcAddress(Module AModule, std::string MethodName);
    virtual std::vector<TString> GetLibraryImports(const TString FileName);
    virtual std::vector<TString> FilterOutRuntimeDependenciesForPlatform(std::vector<TString> Imports);

    virtual bool IsMainThread();
    virtual TPlatformNumber GetMemorySize();

#ifdef DEBUG
    virtual bool IsNativeDebuggerPresent();
    virtual int GetProcessID();
#endif //DEBUG
};
#pragma warning( pop ) // C4250


class WindowsJavaUserPreferences : public JavaUserPreferences {
public:
    WindowsJavaUserPreferences(void);
    ~WindowsJavaUserPreferences(void);

    virtual bool Load(TString Appid);
};


class FileHandle {
private:
    HANDLE FHandle;

public:
    FileHandle(std::wstring FileName);
    ~FileHandle();

    bool IsValid();
    HANDLE GetHandle();
};


class FileMappingHandle {
private:
    HANDLE FHandle;

public:
    FileMappingHandle(HANDLE FileHandle);
    ~FileMappingHandle();

    bool IsValid();
    HANDLE GetHandle();
};


class FileData {
private:
    LPVOID FBaseAddress;

public:
    FileData(HANDLE Handle);
    ~FileData();

    bool IsValid();
    LPVOID GetBaseAddress();
};


class WindowsLibrary {
private:
    TString FFileName;

    // Given an RVA, look up the section header that encloses it and return a
    // pointer to its IMAGE_SECTION_HEADER
    static PIMAGE_SECTION_HEADER GetEnclosingSectionHeader(DWORD rva, PIMAGE_NT_HEADERS pNTHeader);
    static LPVOID GetPtrFromRVA(DWORD rva, PIMAGE_NT_HEADERS pNTHeader, DWORD imageBase);
    static std::vector<TString> GetImportsSection(DWORD base, PIMAGE_NT_HEADERS pNTHeader);
    static std::vector<TString> DumpPEFile(PIMAGE_DOS_HEADER dosHeader);

public:
    WindowsLibrary(const TString FileName);

    std::vector<TString> GetImports();
};

#endif //WINDOWSPLATFORM_H

#endif // WINDOWS
