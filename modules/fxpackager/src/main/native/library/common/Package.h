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
    std::map<TString, TValueIndex> FJVMArgs;
    std::list<TString> FArgs;
    
    TString FPackageRootDirectory;
    TString FPackageAppDirectory;
    TString FPackageLauncherDirectory;
    TString FAppID;
    TString FClassPath;
    TString FMainJar;
    TString FMainClassName;
    bool FIsRuntimeBundled;
    TString FJVMPath;
    TString FSplashScreenFileName;
    bool FUseJavaPreferences;
    TString FCommandName;
    
    size_t FMemorySize;
    MemoryState FMemoryState;
};

class Package {
private:
    Package(Package const&); // Don't Implement.
    void operator=(Package const&); // Don't implement

private:
    PackageBootFields* FBootFields;
    
    bool FDebugging;

    PropertyFile* FJVMUserConfig; // Contains JVM user overrides
    std::map<TString, TValueIndex> FDefaultJVMUserArgs; // Contains JVM user defaults
    std::map<TString, TValueIndex> FJVMUserArgs; // Contains a merge of JVM defaults and user overrides


    Package(void);

    void Initialize();
    void MergeJVMDefaultsWithOverrides();
    TString GetMainJar();
    
public:
    static Package& GetInstance();
    ~Package(void);

    void FreeBootFields();

    void SetCommandLineArguments(int argc, TCHAR* argv[]);

    std::map<TString, TValueIndex> GetJVMArgs();
    std::map<TString, TValueIndex> GetDefaultJVMUserArgs();
    std::map<TString, TValueIndex> GetJVMUserArgOverrides();
    void SetJVMUserArgOverrides(std::map<TString, TValueIndex> Value);
    std::map<TString, TValueIndex> GetJVMUserArgs();

    std::list<TString> GetArgs();

    TString GetPackageRootDirectory();
    TString GetPackageAppDirectory();
    TString GetPackageLauncherDirectory();

    TString GetAppID();
    TString GetClassPath();
    TString GetMainClassName();
    bool IsRuntimeBundled();
    TString GetJVMPath();
    TString GetSplashScreenFileName();
    bool HasSplashScreen();
    TString GetCommandName();
    
    size_t GetMemorySize();
    PackageBootFields::MemoryState GetMemoryState();
};

#endif //PACKAGE_H
