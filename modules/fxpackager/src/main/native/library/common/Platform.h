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


#ifndef PLATFORM_H
#define PLATFORM_H

#include "OrderedMap.h"

#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <string>
#include <map>
#include <list>
#include <vector>


#ifdef WIN32
#define WINDOWS
#endif //WIN32

#ifdef __APPLE__
#define MAC
#define POSIX
#endif //__APPLE__


#ifdef __linux
#define LINUX
#endif //__linux

#ifdef LINUX
#define POSIX
#endif //LINUX



#ifdef WINDOWS
// Define Windows compatibility requirements XP or later
#define WINVER 0x0501
#define _WIN32_WINNT 0x0501

#include <Windows.h>
#include <tchar.h>
#include <shlobj.h>
#include <direct.h>
#include <process.h>
#include <malloc.h>

typedef std::wstring TString;
#define StringLength wcslen

#define TRAILING_PATHSEPARATOR '\\'
#define BAD_TRAILING_PATHSEPARATOR '/'
#define PATH_SEPARATOR ';'
#define BAD_PATH_SEPARATOR ':'

typedef ULONGLONG TPlatformNumber;
typedef DWORD TProcessID;

#if defined _DEBUG && !defined DEBUG
    #define DEBUG
#endif

#endif //WINDOWS


#ifdef POSIX
#include <errno.h>
#include <unistd.h>
#include <sys/stat.h>
#include <dlfcn.h>
#include <libgen.h>

#define _T(x) x

typedef char TCHAR;
typedef std::string TString;
#define StringLength strlen

typedef unsigned long DWORD;

#define TRAILING_PATHSEPARATOR '/'
#define BAD_TRAILING_PATHSEPARATOR '\\'
#define PATH_SEPARATOR ':'
#define BAD_PATH_SEPARATOR ';'
#define MAX_PATH 1000

typedef long TPlatformNumber;
typedef pid_t TProcessID;

#define HMODULE void*
#endif //POSIX


// Config file sections
#define CONFIG_SECTION_APPLICATION            _T("CONFIG_SECTION_APPLICATION")
#define CONFIG_SECTION_JVMOPTIONS             _T("CONFIG_SECTION_JVMOPTIONS")
#define CONFIG_SECTION_JVMUSEROPTIONS         _T("CONFIG_SECTION_JVMUSEROPTIONS")
#define CONFIG_SECTION_JVMUSEROVERRIDESOPTIONS         _T("CONFIG_SECTION_JVMUSEROVERRIDESOPTIONS")
#define CONFIG_SECTION_APPCDSJVMOPTIONS       _T("CONFIG_SECTION_APPCDSJVMOPTIONS")
#define CONFIG_SECTION_APPCDSGENERATECACHEJVMOPTIONS _T("CONFIG_SECTION_APPCDSGENERATECACHEJVMOPTIONS")
#define CONFIG_SECTION_ARGOPTIONS             _T("CONFIG_SECTION_ARGOPTIONS")

// Config file keys.
#define CONFIG_VERSION            _T("CONFIG_VERSION")
#define CONFIG_MAINJAR_KEY        _T("CONFIG_MAINJAR_KEY")
#define CONFIG_MAINCLASSNAME_KEY  _T("CONFIG_MAINCLASSNAME_KEY")
#define CONFIG_CLASSPATH_KEY      _T("CONFIG_CLASSPATH_KEY")
#define APP_NAME_KEY              _T("APP_NAME_KEY")
#define CONFIG_SPLASH_KEY         _T("CONFIG_SPLASH_KEY")
#define CONFIG_APP_ID_KEY         _T("CONFIG_APP_ID_KEY")
#define CONFIG_APP_MEMORY         _T("CONFIG_APP_MEMORY")

#define JVM_RUNTIME_KEY           _T("JVM_RUNTIME_KEY")
#define PACKAGER_APP_DATA_DIR     _T("CONFIG_APP_IDENTIFIER")



typedef void* Module;
typedef void* Procedure;


class Process {
public:
    Process() {}
    virtual ~Process() {}

    virtual bool IsRunning() = 0;
    virtual bool Terminate() = 0;
    virtual bool Execute(const TString Application, const std::vector<TString> Arguments,
        bool AWait = false) = 0;
    virtual bool Wait() = 0;
    virtual TProcessID GetProcessID() = 0;
};


template <typename T>
class AutoFreePtr {
private:
    T* FObject;

public:
    AutoFreePtr() {
        FObject = NULL;
    }

    AutoFreePtr(T* Value) {
        FObject = Value;
    }

    ~AutoFreePtr() {
        if (FObject != NULL) {
            delete FObject;
        }
    }

    operator T* () const {
        return FObject;
    }

    T& operator* () const {
        return *FObject;
    }

    T* operator->() const {
        return FObject;
    }

    T** operator&() {
        return &FObject;
    }

    T* operator=(const T * rhs) {
        FObject = rhs;
        return FObject;
    }
};


class IPropertyContainer {
public:
    IPropertyContainer(void) {}
    virtual ~IPropertyContainer(void) {}

    virtual bool GetValue(const TString Key, TString& Value) = 0;
    virtual size_t GetCount() = 0;
};

class ISectionalPropertyContainer {
public:
    ISectionalPropertyContainer(void) {}
    virtual ~ISectionalPropertyContainer(void) {}

    virtual bool GetValue(const TString SectionName, const TString Key, TString& Value) = 0;
    virtual bool ContainsSection(const TString SectionName) = 0;
    virtual bool GetSection(const TString SectionName, OrderedMap<TString, TString> &Data) = 0;
};


enum DebugState {dsNone, dsNative, dsJava};
enum MessageResponse {mrOK, mrCancel};
enum AppCDSState {cdsNone, cdsOn, cdsGenCache, cdsAuto, cdsInteractive};

class Platform {
private:
    AppCDSState FAppCDSState;

protected:
    Platform(void) {
        FAppCDSState = cdsNone;
    }

public:
    AppCDSState GetAppCDSState() { return FAppCDSState; }
    void SetAppCDSState(AppCDSState Value) { FAppCDSState = Value; }

    static Platform& GetInstance();

    virtual ~Platform(void) {}

public:
    virtual void ShowMessage(TString title, TString description) = 0;
    virtual void ShowMessage(TString description) = 0;
    virtual MessageResponse ShowResponseMessage(TString title, TString description) = 0;
//    virtual MessageResponse ShowResponseMessage(TString description) = 0;

    virtual void SetCurrentDirectory(TString Value) = 0;

    // Caller must free result using delete[].
    virtual TCHAR* ConvertStringToFileSystemString(TCHAR* Source, bool &release) = 0;

    // Caller must free result using delete[].
    virtual TCHAR* ConvertFileSystemStringToString(TCHAR* Source, bool &release) = 0;

    // Returns:
    // Windows=C:\Users\<username>\AppData\Local\<app.identifier>\packager\jvmuserargs.cfg
    // Linux=~/.local/<app.identifier>/packager/jvmuserargs.cfg
    // Mac=~/Library/Application Support/<app.identifier>/packager/jvmuserargs.cfg
    virtual TString GetAppDataDirectory() = 0;

    virtual TString GetPackageAppDirectory() = 0;
    virtual TString GetPackageLauncherDirectory() = 0;
    virtual TString GetAppName() = 0;

    virtual TString GetConfigFileName() = 0;

    virtual TString GetBundledJVMLibraryFileName(TString RuntimePath) = 0;
    virtual TString GetSystemJVMLibraryFileName() = 0;
    virtual TString GetSystemJRE() = 0;

    // Caller must free result.
    virtual ISectionalPropertyContainer* GetConfigFile(TString FileName) = 0;

    virtual TString GetModuleFileName() = 0;
    virtual TString GetPackageRootDirectory() = 0;

    virtual Module LoadLibrary(TString FileName) = 0;
    virtual void FreeLibrary(Module Module) = 0;
    virtual Procedure GetProcAddress(Module Module, std::string MethodName) = 0;
    virtual std::vector<TString> GetLibraryImports(const TString FileName) = 0;
    virtual std::vector<TString> FilterOutRuntimeDependenciesForPlatform(std::vector<TString> Imports) = 0;

    // Caller must free result.
    virtual Process* CreateProcess() = 0;

    virtual bool IsMainThread() = 0;

    // Returns megabytes.
    virtual TPlatformNumber GetMemorySize() = 0;

    virtual std::map<TString, TString> GetKeys() = 0;

    virtual std::list<TString> LoadFromFile(TString FileName) = 0;
    virtual void SaveToFile(TString FileName, std::list<TString> Contents, bool ownerOnly) = 0;

#ifdef DEBUG
    virtual DebugState GetDebugState() = 0;
    virtual int GetProcessID() = 0;
    virtual bool IsNativeDebuggerPresent() = 0;
#endif //DEBUG
};


class Library {
private:
    std::vector<TString> *FDependentLibraryNames;
    std::vector<Library*> *FDependenciesLibraries;
    Module FModule;

    void Initialize();
    void InitializeDependencies();
    void LoadDependencies();
    void UnloadDependencies();

protected:
    void* GetProcAddress(std::string MethodName);

public:
    Library();
    Library(const TString &FileName);
    ~Library();

    bool Load(const TString &FileName);
    bool Unload();

    void AddDependency(const TString &FileName);
    void AddDependencies(const std::vector<TString> &Dependencies);
};


class Exception: public std::exception {
private:
    TString FMessage;

protected:
    void SetMessage(const TString Message) {
        FMessage = Message;
    }

public:
    explicit Exception() : exception() {}
    explicit Exception(const TString Message) : exception() {
        SetMessage(Message);
    }
    virtual ~Exception() throw() {}

    TString GetMessage() { return FMessage; }
};

class FileNotFoundException: public Exception {
public:
    explicit FileNotFoundException(const TString Message) : Exception(Message) {}
};

#endif //PLATFORM_H
