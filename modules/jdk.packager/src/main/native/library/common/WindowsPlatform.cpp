/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates.
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

#include "JavaVirtualMachine.h"
#include "WindowsPlatform.h"
#include "Package.h"
#include "Helpers.h"
#include "PlatformString.h"
#include "Macros.h"

#include <Shlobj.h>
#include <map>
#include <vector>
#include <regex>

#define WINDOWS_PACKAGER_TMP_DIR L"\\AppData\\LocalLow\\Sun\\Java\\Packager\\tmp"

typedef HRESULT (WINAPI* lpSHGetKnownFolderPath)(
    REFKNOWNFOLDERID rfid,
    DWORD dwFlags,
    HANDLE hToken,
    PWSTR *ppszPath
);

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

void WindowsPlatform::ShowMessage(TString title, TString description) {
    MessageBox(NULL, description.data(), !title.empty() ? title.data() : description.data(), MB_ICONERROR | MB_OK);
}

void WindowsPlatform::ShowMessage(TString description) {
    TString appname = GetModuleFileName();
    appname = FilePath::ExtractFileName(appname);
    MessageBox(NULL, description.data(), appname.data(), MB_ICONERROR | MB_OK);
}

MessageResponse WindowsPlatform::ShowResponseMessage(TString title, TString description) {
    MessageResponse result = mrCancel;

    if (::MessageBox(NULL, description.data(), title.data(), MB_OKCANCEL) == IDOK) {
        result = mrOK;
    }

    return result;
}

//MessageResponse WindowsPlatform::ShowResponseMessage(TString description) {
//    TString appname = GetModuleFileName();
//    appname = FilePath::ExtractFileName(appname);
//    return ShowResponseMessage(appname, description);
//}

TString WindowsPlatform::GetBundledJVMLibraryFileName(TString RuntimePath) {
    TString result = FilePath::IncludeTrailingSeparater(RuntimePath) +
        _T("jre\\bin\\jli.dll");

    if (FilePath::FileExists(result) == false) {
        result = FilePath::IncludeTrailingSeparater(RuntimePath) +
            _T("bin\\jli.dll");
    }

    return result;
}

ISectionalPropertyContainer* WindowsPlatform::GetConfigFile(TString FileName) {
    IniFile *result = new IniFile();

    if (result->LoadFromFile(FileName) == false) {
        // New property file format was not found, attempt to load old property file format.
        Helpers::LoadOldConfigFile(FileName, result);
    }

    return result;
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

TString WindowsPlatform::GetTempDirectory() {
    HMODULE hndlShell32;
    lpSHGetKnownFolderPath pSHGetKnownFolderPath;
    TString tmpDir;

    hndlShell32 = ::LoadLibrary(L"shell32");
    if (NULL != hndlShell32) {

        pSHGetKnownFolderPath = (lpSHGetKnownFolderPath)
            GetProcAddress(hndlShell32, "SHGetKnownFolderPath");

        if (pSHGetKnownFolderPath != NULL) {
            PWSTR userDir = 0;
            if (SUCCEEDED(pSHGetKnownFolderPath(
                            FOLDERID_Profile,
                            0,
                            NULL,
                            &userDir))) {
                tmpDir = userDir;
                tmpDir += WINDOWS_PACKAGER_TMP_DIR;
        CoTaskMemFree(userDir);
            }
        }
        FreeLibrary(hndlShell32);
    }
    return tmpDir;
}

static BOOL CALLBACK enumWindows(HWND winHandle, LPARAM lParam) {
    DWORD pid = (DWORD)lParam, wPid = 0;
    GetWindowThreadProcessId(winHandle, &wPid);
    if (pid == wPid)  {
        SetForegroundWindow(winHandle);
        return FALSE;
    }
    return TRUE;
}

void WindowsPlatform::reactivateAnotherInstance() {
    if (singleInstanceProcessId == 0) {
        printf("Unable to reactivate another instance, PID is undefined");
        return;
    }
    EnumWindows(&enumWindows, (LPARAM)singleInstanceProcessId);
}

// returns true if another instance is already running.
// if false, we need to continue regular launch.
bool WindowsPlatform::CheckForSingleInstance(TString name) {
    if (SingleInstance::getInstance(name)->IsAnotherInstanceRunning()) {
        // read PID
        DWORD pid = SingleInstance::getInstance(name)->readPid();
        if (pid != 0) {
            singleInstanceProcessId = pid;
            return true;
        }
    } else {
        // it is the first intance
        // write pid and continue regular launch
        SingleInstance::getInstance(name)->writePid(GetCurrentProcessId());
    }
    return false;
}

SingleInstance::SingleInstance(TString& name_): BUF_SIZE(256), _name(name_), _hMapFile(NULL), _pBuf(NULL) {
    _mutex = CreateMutex(NULL, TRUE, name_.data());
    _lastError = GetLastError();
    _sharedMemoryName = _T("Local\\javapackager-") + _name;
}

SingleInstance::~SingleInstance() {
    if (_pBuf != NULL) {
        UnmapViewOfFile(_pBuf);
        _pBuf = NULL;
    }

    if (_hMapFile != NULL) {
        CloseHandle(_hMapFile);
        _hMapFile = NULL;
    }

    if (_mutex != NULL) {
        CloseHandle(_mutex);
        _mutex = NULL;
    }
}

bool SingleInstance::writePid(DWORD pid) {
    _hMapFile = CreateFileMapping(
                 INVALID_HANDLE_VALUE,
                 NULL,
                 PAGE_READWRITE,
                 0,
                 BUF_SIZE,
                 _sharedMemoryName.data());

    if (_hMapFile == NULL) {
        return false;
    }

    _pBuf = (LPTSTR) MapViewOfFile(_hMapFile,
                        FILE_MAP_ALL_ACCESS,
                        0,
                        0,
                        BUF_SIZE);

    if (_pBuf == NULL) {
        CloseHandle(_hMapFile);
        _hMapFile = NULL;
        return false;
    }

    CopyMemory((PVOID)_pBuf, &pid, sizeof(DWORD));

    return true;
}

DWORD SingleInstance::readPid() {
    _hMapFile = OpenFileMapping(
                   FILE_MAP_ALL_ACCESS,
                   FALSE,
                   _sharedMemoryName.data());

    if (_hMapFile == NULL) {
        return 0;
    }

   _pBuf = (LPTSTR) MapViewOfFile(_hMapFile,
               FILE_MAP_ALL_ACCESS,
               0,
               0,
               BUF_SIZE);

    if (_pBuf == NULL) {
        CloseHandle(_hMapFile);
        _hMapFile = NULL;
        return 0;
    }

    DWORD pid = 0;
    CopyMemory(&pid, (PVOID)_pBuf,  sizeof(DWORD));

    return pid;
}

TPlatformNumber WindowsPlatform::GetMemorySize() {
    SYSTEM_INFO si;
    GetSystemInfo(&si);
    size_t result = (size_t)si.lpMaximumApplicationAddress;
    result = result / 1048576; // Convert from bytes to megabytes.
    return result;
}

std::vector<TString> WindowsPlatform::GetLibraryImports(const TString FileName) {
 std::vector<TString> result;
    WindowsLibrary library(FileName);
    result = library.GetImports();
 return result;
}

std::vector<TString> FilterList(std::vector<TString> &Items, std::wregex Pattern) {
    std::vector<TString> result;

    for (std::vector<TString>::iterator it = Items.begin(); it != Items.end(); ++it) {
        TString item = *it;
        std::wsmatch match;

        if (std::regex_search(item, match, Pattern)) {
            result.push_back(item);
        }
    }
    return result;
}

std::vector<TString> WindowsPlatform::FilterOutRuntimeDependenciesForPlatform(std::vector<TString> Imports) {
 std::vector<TString> result;

    Package& package = Package::GetInstance();
    Macros& macros = Macros::GetInstance();
    TString runtimeDir = macros.ExpandMacros(package.GetJVMRuntimeDirectory());
    std::vector<TString> filelist = FilterList(Imports, std::wregex(_T("MSVCR.*.DLL"), std::regex_constants::icase));

    for (std::vector<TString>::iterator it = filelist.begin(); it != filelist.end(); ++it) {
        TString filename = *it;
        TString msvcr100FileName = FilePath::IncludeTrailingSeparater(runtimeDir) + _T("jre\\bin\\") + filename;

        if (FilePath::FileExists(msvcr100FileName) == true) {
            result.push_back(msvcr100FileName);
            break;
        }
        else {
            msvcr100FileName = FilePath::IncludeTrailingSeparater(runtimeDir) + _T("bin\\") + filename;

            if (FilePath::FileExists(msvcr100FileName) == true) {
                result.push_back(msvcr100FileName);
                break;
            }
        }
    }

 return result;
}

Process* WindowsPlatform::CreateProcess() {
    return new WindowsProcess();
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
        OrderedMap<TString, TString> mapOfKeysAndValues;

        for (std::list<TString>::const_iterator iterator = keys.begin(); iterator != keys.end(); iterator++) {
            TString key = *iterator;
            TString value = registry.ReadString(key);
            key = ConvertJavaEcodedStringToString(key);
            value = ConvertJavaEcodedStringToString(value);

            if (key.empty() == false) {
                mapOfKeysAndValues.Append(key, value);
                result = true;
            }
        }

        FMap = mapOfKeysAndValues;
    }

    return result;
}

//--------------------------------------------------------------------------------------------------

FileHandle::FileHandle(std::wstring FileName) {
    FHandle = ::CreateFile(FileName.data(), GENERIC_READ, FILE_SHARE_READ, NULL,
                            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, 0);
}

FileHandle::~FileHandle() {
    if (IsValid() == true) {
        ::CloseHandle(FHandle);
    }
}

bool FileHandle::IsValid() {
    return FHandle != INVALID_HANDLE_VALUE;
}

HANDLE FileHandle::GetHandle() {
    return FHandle;
}

FileMappingHandle::FileMappingHandle(HANDLE FileHandle) {
    FHandle = ::CreateFileMapping(FileHandle, NULL, PAGE_READONLY, 0, 0, NULL);
}

bool FileMappingHandle::IsValid() {
    return FHandle != NULL;
}

FileMappingHandle::~FileMappingHandle() {
    if (IsValid() == true) {
        ::CloseHandle(FHandle);
    }
}

HANDLE FileMappingHandle::GetHandle() {
    return FHandle;
}

FileData::FileData(HANDLE Handle) {
    FBaseAddress = ::MapViewOfFile(Handle, FILE_MAP_READ, 0, 0, 0);
}

FileData::~FileData() {
    if (IsValid() == true) {
        ::UnmapViewOfFile(FBaseAddress);
    }
}

bool FileData::IsValid() {
    return FBaseAddress != NULL;
}

LPVOID FileData::GetBaseAddress() {
    return FBaseAddress;
}


WindowsLibrary::WindowsLibrary(std::wstring FileName) {
    FFileName = FileName;
}

std::vector<TString> WindowsLibrary::GetImports() {
    std::vector<TString> result;
    FileHandle library(FFileName);

    if (library.IsValid() == true) {
        FileMappingHandle mapping(library.GetHandle());

        if (mapping.IsValid() == true) {
            FileData fileData(mapping.GetHandle());

            if (fileData.IsValid() == true) {
                PIMAGE_DOS_HEADER dosHeader = (PIMAGE_DOS_HEADER)fileData.GetBaseAddress();
                PIMAGE_FILE_HEADER pImgFileHdr = (PIMAGE_FILE_HEADER)fileData.GetBaseAddress();

                if (dosHeader->e_magic == IMAGE_DOS_SIGNATURE) {
                    result = DumpPEFile(dosHeader);
                }
            }
        }
    }

    return result;
}

// Given an RVA, look up the section header that encloses it and return a
// pointer to its IMAGE_SECTION_HEADER
PIMAGE_SECTION_HEADER WindowsLibrary::GetEnclosingSectionHeader(DWORD rva,
                                                PIMAGE_NT_HEADERS pNTHeader) {
    PIMAGE_SECTION_HEADER result = 0;
    PIMAGE_SECTION_HEADER section = IMAGE_FIRST_SECTION(pNTHeader);

    for (unsigned index = 0; index < pNTHeader->FileHeader.NumberOfSections; index++, section++) {
        // Is the RVA is within this section?
        if ((rva >= section->VirtualAddress) &&
            (rva < (section->VirtualAddress + section->Misc.VirtualSize))) {
            result = section;
        }
    }

    return result;
}

LPVOID WindowsLibrary::GetPtrFromRVA(DWORD rva, PIMAGE_NT_HEADERS pNTHeader, DWORD imageBase) {
    LPVOID result = 0;
    PIMAGE_SECTION_HEADER pSectionHdr = GetEnclosingSectionHeader(rva, pNTHeader);

    if (pSectionHdr != NULL) {
        INT delta = (INT)(pSectionHdr->VirtualAddress-pSectionHdr->PointerToRawData);
        result = (PVOID)(imageBase + rva - delta);
    }

    return result;
}

std::vector<TString> WindowsLibrary::GetImportsSection(DWORD base, PIMAGE_NT_HEADERS pNTHeader) {
    std::vector<TString> result;

    // Look up where the imports section is located. Normally in the .idata section,
    // but not necessarily so. Therefore, grab the RVA from the data dir.
    DWORD importsStartRVA = pNTHeader->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_IMPORT].VirtualAddress;

    if (importsStartRVA != NULL) {
        // Get the IMAGE_SECTION_HEADER that contains the imports. This is
        // usually the .idata section, but doesn't have to be.
        PIMAGE_SECTION_HEADER pSection = GetEnclosingSectionHeader(importsStartRVA, pNTHeader);

        if (pSection != NULL) {
            PIMAGE_IMPORT_DESCRIPTOR importDesc = (PIMAGE_IMPORT_DESCRIPTOR)GetPtrFromRVA(importsStartRVA, pNTHeader,base);

            if (importDesc != NULL) {
                while (true)
                {
                    // See if we've reached an empty IMAGE_IMPORT_DESCRIPTOR
                    if ((importDesc->TimeDateStamp == 0) && (importDesc->Name == 0))
                        break;

                    std::string filename = (char*)GetPtrFromRVA(importDesc->Name, pNTHeader, base);
                    result.push_back(PlatformString(filename));
                    importDesc++;   // advance to next IMAGE_IMPORT_DESCRIPTOR
                }
            }
        }
    }

    return result;
}

std::vector<TString> WindowsLibrary::DumpPEFile(PIMAGE_DOS_HEADER dosHeader) {
    std::vector<TString> result;
    PIMAGE_NT_HEADERS pNTHeader = (PIMAGE_NT_HEADERS)((DWORD)(dosHeader) + (DWORD)(dosHeader->e_lfanew));

    // Verify that the e_lfanew field gave us a reasonable
    // pointer and the PE signature.
    // TODO: To really fix JDK-8131321 this condition needs to be changed. There is a matching change
    // in JavaVirtualMachine.cpp that also needs to be changed.
    if (pNTHeader->Signature == IMAGE_NT_SIGNATURE) {
        DWORD base = (DWORD)dosHeader;
        result = GetImportsSection(base, pNTHeader);
    }

    return result;
}

//--------------------------------------------------------------------------------------------------

#include <TlHelp32.h>

WindowsJob::WindowsJob() {
    FHandle = NULL;
}

WindowsJob::~WindowsJob() {
    if (FHandle != NULL) {
        CloseHandle(FHandle);
    }
}

HANDLE WindowsJob::GetHandle() {
    if (FHandle == NULL) {
        FHandle = CreateJobObject(NULL, NULL); // GLOBAL

        if (FHandle == NULL)
        {
            ::MessageBox( 0, _T("Could not create job object"), _T("TEST"), MB_OK);
        }
        else
        {
            JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli = { 0 };

            // Configure all child processes associated with the job to terminate when the
            jeli.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
            if (0 == SetInformationJobObject(FHandle, JobObjectExtendedLimitInformation, &jeli, sizeof(jeli)))
            {
                ::MessageBox( 0, _T("Could not SetInformationJobObject"), _T("TEST"), MB_OK);
            }
        }
    }

    return FHandle;
}

// Initialize static member of WindowsProcess
WindowsJob WindowsProcess::FJob;

WindowsProcess::WindowsProcess() : Process() {
    FRunning = false;
}

WindowsProcess::~WindowsProcess() {
    Terminate();
}

void WindowsProcess::Cleanup() {
    CloseHandle(FProcessInfo.hProcess);
    CloseHandle(FProcessInfo.hThread);
}

bool WindowsProcess::IsRunning() {
    bool result = false;

    HANDLE handle = ::CreateToolhelp32Snapshot(TH32CS_SNAPALL, 0);
    PROCESSENTRY32 process = { 0 };
    process.dwSize = sizeof(process);

    if (::Process32First(handle, &process)) {
        do {
            if (process.th32ProcessID == FProcessInfo.dwProcessId) {
                result = true;
                break;
            }
        }
        while (::Process32Next(handle, &process));
    }

    CloseHandle(handle);

    return result;
}

bool WindowsProcess::Terminate() {
    bool result = false;

    if (IsRunning() == true && FRunning == true) {
        FRunning = false;
    }

    return result;
}

bool WindowsProcess::Execute(const TString Application, const std::vector<TString> Arguments, bool AWait) {
    bool result = false;

    if (FRunning == false) {
        FRunning = true;

        STARTUPINFO startupInfo;
        ZeroMemory(&startupInfo, sizeof(startupInfo));
        startupInfo.cb = sizeof(startupInfo);
        ZeroMemory(&FProcessInfo, sizeof(FProcessInfo));

        TString command = Application;

        for (std::vector<TString>::const_iterator iterator = Arguments.begin(); iterator != Arguments.end(); iterator++) {
            command += TString(_T(" ")) + *iterator;
        }

        if (::CreateProcess(Application.data(), (wchar_t*)command.data(), NULL,
            NULL, FALSE, 0, NULL, NULL, &startupInfo, &FProcessInfo) == FALSE) {
            TString message = PlatformString::Format(_T("Error: Unable to create process %s"), Application.data());
            throw Exception(message);
        }
        else {
            if (FJob.GetHandle() != NULL) {
                if (::AssignProcessToJobObject(FJob.GetHandle(), FProcessInfo.hProcess) == 0) {
                    // Failed to assign process to job. It doesn't prevent anything from continuing so continue.
                }
            }

            // Wait until child process exits.
            if (AWait == true) {
                Wait();
                // Close process and thread handles.
                Cleanup();
            }
        }
    }

    return result;
}

bool WindowsProcess::Wait() {
    bool result = false;

    WaitForSingleObject(FProcessInfo.hProcess, INFINITE);
    return result;
}

TProcessID WindowsProcess::GetProcessID() {
    return FProcessInfo.dwProcessId;
}

bool WindowsProcess::ReadOutput() {
    bool result = false;
    //TODO implement
    return result;
}

void WindowsProcess::SetInput(TString Value) {
    //TODO implement
}

std::list<TString> WindowsProcess::GetOutput() {
    ReadOutput();
    return Process::GetOutput();
}

#endif //WINDOWS
