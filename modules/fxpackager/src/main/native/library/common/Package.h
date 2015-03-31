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


#ifndef PACKAGE_H
#define PACKAGE_H


#include "Platform.h"
#include "PlatformString.h"
#include "FilePath.h"
#include "PropertyFile.h"

#include <map>
#include <list>

class PackageBootFields {
public:
    enum MemoryState {msManual, msAuto};

public:
    OrderedMap<TString, TString> FJVMArgs;
    std::list<TString> FArgs;

    TString FPackageRootDirectory;
    TString FPackageAppDirectory;
    TString FPackageLauncherDirectory;
    TString FAppDataDirectory;
    TString FAppID;
    TString FPackageAppDataDirectory;
    TString FClassPath;
    TString FMainJar;
    TString FMainClassName;
    bool FIsRuntimeBundled;
    TString FJVMRuntimeDirectory;
    TString FJVMLibraryFileName;
    TString FSplashScreenFileName;
    bool FUseJavaPreferences;
    TString FCommandName;

    TString FAppCDSCacheFileName;

    TPlatformNumber FMemorySize;
    MemoryState FMemoryState;
};


class Package {
private:
    Package(Package const&); // Don't Implement.
    void operator=(Package const&); // Don't implement

private:
    bool FInitialized;
    PackageBootFields* FBootFields;
    TString FJVMUserArgsConfigFileName;
    TString FAppCDSCacheDirectory;

    DebugState FDebugging;

    OrderedMap<TString, TString> FJVMUserArgsOverrides;
    OrderedMap<TString, TString> FDefaultJVMUserArgs; // Contains JVM user defaults
    OrderedMap<TString, TString> FJVMUserArgs; // Contains a merge of JVM defaults and user overrides


    Package(void);

    //void Initialize();
    void MergeJVMDefaultsWithOverrides();
    TString GetMainJar();
    void SaveJVMUserArgOverrides(OrderedMap<TString, TString> Data);
    void ReadJVMArgs(ISectionalPropertyContainer* Config);

public:
    static Package& GetInstance();
    ~Package(void);

    void Initialize();
    void Clear();
    void FreeBootFields();

    void SetCommandLineArguments(int argc, TCHAR* argv[]);

    OrderedMap<TString, TString> GetJVMArgs();
    OrderedMap<TString, TString> GetDefaultJVMUserArgs();
    OrderedMap<TString, TString> GetJVMUserArgOverrides();
    void SetJVMUserArgOverrides(OrderedMap<TString, TString> Value);
    OrderedMap<TString, TString> GetJVMUserArgs();

    std::list<TString> GetArgs();

    TString GetPackageRootDirectory();
    TString GetPackageAppDirectory();
    TString GetPackageLauncherDirectory();
    TString GetAppDataDirectory();

    TString GetJVMUserArgsConfigFileName();
    TString GetAppCDSCacheDirectory();
    TString GetAppCDSCacheFileName();

    TString GetAppID();
    TString GetPackageAppDataDirectory();
    TString GetClassPath();
    TString GetMainClassName();
    bool IsRuntimeBundled();
    TString GetJVMLibraryFileName();
    TString GetJVMRuntimeDirectory();
    TString GetSplashScreenFileName();
    bool HasSplashScreen();
    TString GetCommandName();

    TPlatformNumber GetMemorySize();
    PackageBootFields::MemoryState GetMemoryState();

    DebugState Debugging();
};

#endif //PACKAGE_H
