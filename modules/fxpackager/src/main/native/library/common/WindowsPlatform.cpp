/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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

#include "WindowsPlatform.h"
#include "Package.h"
#include "Helpers.h"
#include "PlatformString.h"

#include <map>
#include <vector>


//--------------------------------------------------------------------------------------------------

class Registry {
private:
    HKEY FKey;
    HKEY FOpenKey;
    bool FOpen;

public:
    Registry(HKEY Key) {
        FOpen = false;
        FKey = Key;
    }

    ~Registry() {
        Close();
    }

    void Close() {
        if (FOpen == true) {
            RegCloseKey(FOpenKey);
        }
    }

    bool Open(TString SubKey) {
        bool result = false;
        Close();

        if (RegOpenKeyEx(FKey, SubKey.data(), 0, KEY_READ, &FOpenKey) == ERROR_SUCCESS) {
            result = true;
        }

        return result;
    }

    std::list<TString> GetKeys() {
        std::list<TString> result;
        DWORD count;

        if (RegQueryInfoKey(FOpenKey, NULL, NULL, NULL, NULL, NULL, NULL,
                            &count, NULL, NULL, NULL, NULL) == ERROR_SUCCESS) {

            DWORD length = 255;
            DynamicBuffer<TCHAR> buffer(length);

            for (unsigned int index = 0; index < count; index++) {
                buffer.Zero();
                DWORD status = RegEnumValue(FOpenKey, index, buffer.GetData(),
                                            &length, NULL, NULL, NULL, NULL);

                while (status == ERROR_MORE_DATA) {
                    length = length * 2;
                    buffer.Resize(length);
                    status = RegEnumValue(FOpenKey, index, buffer.GetData(),
                                          &length, NULL, NULL, NULL, NULL);
                }

                if (status == ERROR_SUCCESS) {
                    TString value = buffer.GetData();
                    result.push_back(value);
                }
            }
        }

        return result;
    }

    TString ReadString(TString Name) {
        TString result;
        DWORD length;
        DWORD dwRet;
        DynamicBuffer<wchar_t> buffer(0);
        length = 0;

        dwRet = RegQueryValueEx(FOpenKey, Name.data(), NULL, NULL, NULL, &length);
        if (dwRet == ERROR_MORE_DATA || dwRet == 0) {
            buffer.Resize(length + 1);
            dwRet = RegQueryValueEx(FOpenKey, Name.data(), NULL, NULL, (LPBYTE)buffer.GetData(), &length);
            result = buffer.GetData();
        }

        return result;
    }
};

//--------------------------------------------------------------------------------------------------

WindowsPlatform::WindowsPlatform(void) : Platform(), GenericPlatform() {
    FMainThread = ::GetCurrentThreadId();
}

WindowsPlatform::~WindowsPlatform(void) {
}

TCHAR* WindowsPlatform::ConvertStringToFileSystemString(TCHAR* Source, bool &release) {
    // Not Implemented.
    return NULL;
}

TCHAR* WindowsPlatform::ConvertFileSystemStringToString(TCHAR* Source, bool &release) {
    // Not Implemented.
    return NULL;
}

void WindowsPlatform::SetCurrentDirectory(TString Value) {
    _wchdir(Value.data());
}

TString WindowsPlatform::GetPackageRootDirectory() {
    TString filename = GetModuleFileName();
    return FilePath::ExtractFilePath(filename);
}

TString WindowsPlatform::GetAppDataDirectory() {
    TString result;
    TCHAR path[MAX_PATH];
    
    if (SHGetFolderPath(NULL, CSIDL_APPDATA, NULL, 0, path) == S_OK) {
        result = path;
    }
    
    return result;
}

#define JAVA_RUNTIME_SUBKEY _T("SOFTWARE\\JavaSoft\\Java Runtime Environment")
#define BUFFER_SIZE 256

// try to find current Java Home from registry
// HKLM\Software\JavaSoft\Java Runtime Environment\CurrentVersion
// HKLM\Software\JavaSoft\Java Runtime Environment\[CurrentVersion]\JavaHome
// return TRUE if found, and path is set in lpszJavaHome
// return FALSE otherwise
TString WindowsPlatform::GetSystemJRE() {
    TString result;
    Registry registry(HKEY_LOCAL_MACHINE);

    if (registry.Open(JAVA_RUNTIME_SUBKEY) == true) {
        TString version = registry.ReadString(_T("CurrentVersion"));

        if (version.empty() == false) {
            if (registry.Open(JAVA_RUNTIME_SUBKEY + TString(_T("\\")) + TString(version)) == true) {
                TString javaHome = registry.ReadString(_T("JavaHome"));

                if (FilePath::DirectoryExists(javaHome) == true) {
                    result = javaHome;
                }
            }
        }
    }

    return result;
}

void WindowsPlatform::ShowError(TString title, TString description) {
    MessageBox(NULL, description.data(), !title.empty() ? title.data() : description.data(), MB_ICONERROR | MB_OK);
}

void WindowsPlatform::ShowError(TString description) {
    TString appname = GetModuleFileName();
    appname = FilePath::ExtractFileName(appname);
    MessageBox(NULL, appname.data(), description.data(), MB_ICONERROR | MB_OK);
}

TString WindowsPlatform::GetBundledJVMLibraryFileName(TString RuntimePath) {

    TString result = FilePath::IncludeTrailingSlash(RuntimePath) +
        _T("jre\\bin\\client\\jvm.dll");

    if (FilePath::FileExists(result) == false) {
        result = FilePath::IncludeTrailingSlash(RuntimePath) +
            _T("jre\\bin\\server\\jvm.dll");
    }

    if (FilePath::FileExists(result) == false) {
        result = FilePath::IncludeTrailingSlash(RuntimePath) +
            _T("bin\\client\\jvm.dll");
    }

    if (FilePath::FileExists(result) == false) {
        result = FilePath::IncludeTrailingSlash(RuntimePath) +
            _T("bin\\server\\jvm.dll");
    }

    return result;
}

TString WindowsPlatform::GetSystemJVMLibraryFileName() {
    TString result;
    TString jvmPath = GetSystemJRE();

    if (jvmPath.empty() == false) {
        result = GetBundledJVMLibraryFileName(jvmPath);
    }

    return result;
}

PropertyContainer* WindowsPlatform::GetConfigFile(TString FileName) {
    return new PropertyFile(FileName);
}

TString WindowsPlatform::GetModuleFileName() {
    TString result;
    DynamicBuffer<wchar_t> buffer(MAX_PATH);
    ::GetModuleFileName(NULL, buffer.GetData(), buffer.GetSize());

    while (ERROR_INSUFFICIENT_BUFFER == GetLastError()) {
        buffer.Resize(buffer.GetSize() * 2);
        ::GetModuleFileName(NULL, buffer.GetData(), buffer.GetSize());
    }

    result = buffer.GetData();
    return result;
}

Module WindowsPlatform::LoadLibrary(TString FileName) {
    return ::LoadLibrary(FileName.data());
}

void WindowsPlatform::FreeLibrary(Module AModule) {
    ::FreeLibrary((HMODULE)AModule);
}

Procedure WindowsPlatform::GetProcAddress(Module AModule, std::string MethodName) {
    return ::GetProcAddress((HMODULE)AModule, MethodName.c_str());
}

bool WindowsPlatform::IsMainThread() {
    bool result = (FMainThread == ::GetCurrentThreadId());
    return result;
}

TPlatformNumber WindowsPlatform::GetMemorySize() {
    SYSTEM_INFO si;
    GetSystemInfo(&si);
    size_t result = (size_t)si.lpMaximumApplicationAddress;
    result = result / 1048576; // Convert from bytes to megabytes.
    return result;
}

#ifdef DEBUG
bool WindowsPlatform::IsNativeDebuggerPresent() {
    bool result = false;
    
    if (IsDebuggerPresent() == TRUE) {
        result = true;
    }
    
    return result;
}

int WindowsPlatform::GetProcessID() {
    int pid = GetProcessId(GetCurrentProcess());
    return pid;
}
#endif //DEBUG

//--------------------------------------------------------------------------------------------------

WindowsJavaUserPreferences::WindowsJavaUserPreferences(void) : JavaUserPreferences() {
}

WindowsJavaUserPreferences::~WindowsJavaUserPreferences(void) {
}

// Java Preferences API encodes it's strings, so we need to match what Java does to work with Java.
// CAVEAT: Java also does unicode encoding which this doesn't do yet. Should be sufficient for jvm args.
// See WindowsPreferences.java toWindowsName()
TString ConvertStringToJavaEcodedString(TString Value) {
    TString result;
    TCHAR* p = (TCHAR*)Value.c_str();
    TCHAR c = *p;

    while (c != 0) {
        switch (c) {
            case '\\':
                result += _T("//");
                break;
        
            case '/':
                result += '\\';
                break;
            default:
                if ((c >= 'A') && (c <= 'Z')) {
                    result += '/';
                    result += c;
                }
                else
                    result += c;
                break;
        }

        p++;
        c = *p;
    }

    return result;
}

// Java Preferences API encodes it's strings, so we need to match what Java does to work with Java.
// CAVEAT: Java also does unicode encoding which this doesn't do yet. Should be sufficient for jvm args.
// See WindowsPreferences.java toJavaName()
TString ConvertJavaEcodedStringToString(TString Value) {
    TString result;
    
    for (size_t index = 0; index < Value.length(); index++) {
        TCHAR c = Value[index];
        
        switch (c) {
            case '/':
                if ((index + 1) < Value.length()) {
                    index++;
                    TCHAR nextc = Value[index];
                    
                    if (nextc >= 'A' && nextc <= 'Z') {
                        result += nextc;
                    }
                    else if (nextc == '/') {
                        result += '\\';
                    }
                }
                break;
            case '\\':
                result += '/';
                break;
            default:
                result += c;
                break;
        }
    }
    
    return result;
}

bool WindowsJavaUserPreferences::Load(TString Appid) {
    bool result = false;
    TString lappid = Helpers::ConvertIdToFilePath(Appid);
    lappid = ConvertStringToJavaEcodedString(Appid);
    TString registryKey = TString(_T("SOFTWARE\\JavaSoft\\Prefs\\")) + lappid + TString(_T("\\/J/V/M/User/Options"));
    Registry registry(HKEY_CURRENT_USER);

    if (registry.Open(registryKey) == true) {
        std::list<TString> keys = registry.GetKeys();
        TOrderedMap mapOfKeysAndValues;
        int index = 1;

        for (std::list<TString>::const_iterator iterator = keys.begin(); iterator != keys.end(); iterator++) {
            TString key = *iterator;
            TString value = registry.ReadString(key);
            key = ConvertJavaEcodedStringToString(key);
            value = ConvertJavaEcodedStringToString(value);

            if (key.empty() == false) {
                TValueIndex item;
                item.value = value;
                item.index = index;

                mapOfKeysAndValues.insert(TOrderedMap::value_type(key, item));
                result = true;
                index++;
            }
        }

        FMap = mapOfKeysAndValues;
    }

    return result;
}

//--------------------------------------------------------------------------------------------------

#endif //WINDOWS
