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


#include "GenericPlatform.h"


#include <fstream>
#include <locale>

#ifdef WINDOWS
#include <codecvt>
#endif //WINDOWS


GenericPlatform::GenericPlatform(void) {
}

GenericPlatform::~GenericPlatform(void) {
}

TString GenericPlatform::GetConfigFileName() {
    TString result;
    TString basedir = GetPackageAppDirectory();
    
    if (basedir.empty() == false) {
        basedir = FilePath::IncludeTrailingSlash(basedir);
        TString appConfig = basedir + GetAppName() + _T(".cfg");
        
        if (FilePath::FileExists(appConfig) == true) {
            result = appConfig;
        }
        else {
            result = basedir + _T("package.cfg");
            
            if (FilePath::FileExists(result) == false) {
                result = _T("");
            }
        }
    }
    
    return result;
}

TString GenericPlatform::GetPackageAppDirectory() {
#if defined(WINDOWS) || defined(LINUX)
    return FilePath::IncludeTrailingSlash(GetPackageRootDirectory()) + _T("app");
#endif //WINDOWS || LINUX
#ifdef MAC
    return FilePath::IncludeTrailingSlash(GetPackageRootDirectory()) + _T("Java");
#endif
}

TString GenericPlatform::GetPackageLauncherDirectory() {
#if defined(WINDOWS) || defined(LINUX)
    return GetPackageRootDirectory();
#endif //WINDOWS || LINUX
#ifdef MAC
    return FilePath::IncludeTrailingSlash(GetPackageRootDirectory()) + _T("MacOS");
#endif
}

std::list<TString> GenericPlatform::LoadFromFile(TString FileName) {
    std::list<TString> result;

    if (FilePath::FileExists(FileName) == true) {
        std::wifstream stream(FileName.data());

#ifdef WINDOWS
        const std::locale empty_locale = std::locale::empty();
#endif //WINDOWS
#ifdef POSIX
        const std::locale empty_locale = std::locale::classic();
#endif //POSIX
#if defined(WINDOWS)
        const std::locale utf8_locale = std::locale(empty_locale, new std::codecvt_utf8<wchar_t>());
        stream.imbue(utf8_locale);
#endif //WINDOWS

        if (stream.is_open() == true) {
            while (stream.eof() == false) {
                std::wstring line;
                std::getline(stream, line);

                // # at the first character will comment out the line.
                if (line.empty() == false && line[0] != '#') {
                    result.push_back(PlatformString(line).toString());
                }
            }
        }
    }

    return result;
}

void GenericPlatform::SaveToFile(TString FileName, std::list<TString> Contents, bool ownerOnly) {
    TString path = FilePath::ExtractFilePath(FileName);

    if (FilePath::DirectoryExists(path) == false) {
        FilePath::CreateDirectory(path, ownerOnly);
    }

    std::wofstream stream(FileName.data());

    FilePath::ChangePermissions(FileName.data(), ownerOnly);

#ifdef WINDOWS
    const std::locale empty_locale = std::locale::empty();
#endif //WINDOWS
#ifdef POSIX
    const std::locale empty_locale = std::locale::classic();
#endif //POSIX
#if defined(WINDOWS)
    const std::locale utf8_locale = std::locale(empty_locale, new std::codecvt_utf8<wchar_t>());
    stream.imbue(utf8_locale);
#endif //WINDOWS || MAC

    if (stream.is_open() == true) {
        for (std::list<TString>::const_iterator iterator = Contents.begin(); iterator != Contents.end(); iterator++) {
            TString line = *iterator;
            stream << PlatformString(line).toUnicodeString() << std::endl;
        }
    }
}

#if defined(WINDOWS) || defined(LINUX)
TString GenericPlatform::GetAppName() {
    TString result = GetModuleFileName();
    result = FilePath::ExtractFileName(result);
    result = FilePath::ChangeFileExt(result, _T(""));
    return result;
}
#endif //WINDOWS || LINUX

std::map<TString, TString> GenericPlatform::GetKeys() {
    std::map<TString, TString> keys;
    keys.insert(std::map<TString, TString>::value_type(CONFIG_MAINJAR_KEY,       _T("app.mainjar")));
    keys.insert(std::map<TString, TString>::value_type(CONFIG_MAINCLASSNAME_KEY, _T("app.mainclass")));
    keys.insert(std::map<TString, TString>::value_type(CONFIG_CLASSPATH_KEY,     _T("app.classpath")));
    keys.insert(std::map<TString, TString>::value_type(APP_NAME_KEY,             _T("app.name")));
    keys.insert(std::map<TString, TString>::value_type(CONFIG_SPLASH_KEY,        _T("app.splash")));
    keys.insert(std::map<TString, TString>::value_type(CONFIG_APP_ID_KEY,        _T("app.preferences.id")));
    keys.insert(std::map<TString, TString>::value_type(CONFIG_APP_MEMORY,        _T("app.memory")));
    keys.insert(std::map<TString, TString>::value_type(JVM_RUNTIME_KEY,          _T("app.runtime")));
    keys.insert(std::map<TString, TString>::value_type(PACKAGER_APP_DATA_DIR,    _T("app.preferences.id")));
    return keys;
}

#ifdef DEBUG
Platform::DebugState GenericPlatform::GetDebugState() {
    Platform::DebugState result = Platform::dsNone;
    
    if (IsNativeDebuggerPresent() == true) {
        result = Platform::dsNative;
    }
    
    return result;
}
#endif //DEBUG
