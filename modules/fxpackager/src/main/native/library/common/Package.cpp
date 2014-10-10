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


#include "Package.h"
#include "Lock.h"
#include "Helpers.h"
#include "JavaUserPreferences.h"


Package::Package(void) {
    Initialize();
}

void Package::Initialize() {
    Platform& platform = Platform::GetInstance();

    FBootFields = new PackageBootFields();

    FDebugging = false;

    std::map<TString, TString> keys = platform.GetKeys();

    FBootFields->FPackageRootDirectory = platform.GetPackageRootDirectory();
    FBootFields->FPackageAppDirectory = platform.GetPackageAppDirectory();
    FBootFields->FPackageLauncherDirectory = platform.GetPackageLauncherDirectory();

    // Read from configure.cfg/Info.plist
    PropertyFile* config = (PropertyFile*)platform.GetConfigFile();
    config->GetValue(keys[CONFIG_APP_ID_KEY], FBootFields->FAppID);
    
    // Auto Memory.
    TString temp;
    config->GetValue(keys[CONFIG_APP_MEMORY], temp);
    
    if (temp == _T("auto")) {
        FBootFields->FMemoryState = PackageBootFields::msAuto;
        FBootFields->FMemorySize = platform.GetMemorySize();
    }
    else {
        FBootFields->FMemoryState = PackageBootFields::msManual;
        FBootFields->FMemorySize = 0;
    }

    // Main JAR.
    config->GetValue(keys[CONFIG_MAINJAR_KEY], FBootFields->FMainJar);
    FBootFields->FMainJar = FilePath::IncludeTrailingSlash(GetPackageAppDirectory()) + FBootFields->FMainJar;

    // Classpath.
    config->GetValue(keys[CONFIG_CLASSPATH_KEY], FBootFields->FClassPath);

    // 1. If class path provided by config file is empty then add main jar.
    // 2. If the provided class path contains main jar then only use provided class path.
    // 3. If main jar is not in provided class path then add it.
    if (FBootFields->FClassPath.empty() == true) {
        FBootFields->FClassPath = GetMainJar();
    }
    else {
        TString mainJarFileName = FilePath::ExtractFileName(GetMainJar());

        if (FBootFields->FClassPath.find(mainJarFileName) != TString::npos) {
            FBootFields->FClassPath = FilePath::FixPathSeparatorForPlatform(FBootFields->FClassPath);
        }
        else {
            FBootFields->FClassPath = GetMainJar() + FilePath::PathSeparator() +
                FilePath::FixPathSeparatorForPlatform(FBootFields->FClassPath);
        }
    }

    config->GetValue(keys[CONFIG_MAINCLASSNAME_KEY], FBootFields->FMainClassName);

    if (config->GetValue(keys[CONFIG_SPLASH_KEY], FBootFields->FSplashScreenFileName) == true) {
        FBootFields->FSplashScreenFileName = FilePath::IncludeTrailingSlash(GetPackageAppDirectory()) + FBootFields->FSplashScreenFileName;

        if (FilePath::FileExists(FBootFields->FSplashScreenFileName) == false) {
            FBootFields->FSplashScreenFileName = _T("");
        }
    }

    // Is a runtime bundled or is a system runtime being used.
#if defined(WINDOWS) || defined(LINUX)
    TString runtime = FilePath::IncludeTrailingSlash(GetPackageRootDirectory()) + _T("runtime");
#endif //WINDOWS || LINUX
#ifdef MAC
    TString runtime;
    config->GetValue(keys[JVM_RUNTIME_KEY], runtime);
    runtime = FilePath::IncludeTrailingSlash(GetPackageRootDirectory()) +
        FilePath::IncludeTrailingSlash(_T("Contents/Plugins")) + runtime;
#endif //MAC

    FBootFields->FIsRuntimeBundled = FilePath::DirectoryExists(runtime);

    // Get JVMPath.
    if (IsRuntimeBundled() == true) {
        FBootFields->FJVMPath = platform.GetJvmPath();
    }
    else {
        FBootFields->FJVMPath = platform.GetSystemJvmPath();
    }

    // Read args if none were passed in.
    if (FBootFields->FArgs.size() == 0) {
        FBootFields->FArgs = Helpers::GetArgsFromConfig(config);
    }  

    // Read all jvmargs.
    FBootFields->FJVMArgs = Helpers::GetJVMArgsFromConfig(config);

    // Read all jvmuserarg defaults.
    FDefaultJVMUserArgs = Helpers::GetJVMUserArgsFromConfig(config);

    // Read jvmuserarg overrides.
    FJVMUserConfig = NULL;

    // Load user JVM overrides.
    if (FilePath::FileExists(platform.GetJVMUserArgsConfigFileName()) == true) {
        // Load new location for user VM overrides.
        FJVMUserConfig = new PropertyFile(platform.GetJVMUserArgsConfigFileName());
    }
    else {
        // Attemp to load java.util.prefs for VM overrides.
        JavaUserPreferences* javaPreferences = JavaUserPreferences::CreateInstance(); //TODO implement for Mac

        if (javaPreferences->Load(GetAppID()) == true) {
            FJVMUserConfig = new PropertyFile(javaPreferences->GetData());
        }
        else {
            FJVMUserConfig = new PropertyFile();
        }

        delete javaPreferences;
    }

    delete config;
    FJVMUserConfig->SetReadOnly(false);
    MergeJVMDefaultsWithOverrides();
}

void Package::SetCommandLineArguments(int argc, TCHAR* argv[]) {
    if (argc > 0) {
        std::list<TString> args;

        // Prepare app arguments. Skip value at index 0 - this is path to executable.
        FBootFields->FCommandName = argv[0];

        // Path to .exe is at 0 index so start at index 1.
        for (int index = 1; index < argc; index++) {
            TString arg = argv[index];

            if (arg == _T("/Debug")) {
#ifdef DEBUG
                //TODO setup for debugging.
                FDebugging = true;
#endif //DEBUG
                continue;
            }
#ifdef MAC
            if (arg.find(_T("-psn_"), 0) != TString::npos) {
                Platform& platform = Platform::GetInstance();

                if (platform.IsMainThread() == true) {
                    printf("%s\n", arg.c_str()); //TODO remove
                    continue;
                }
            }

            if (arg == _T("-NSDocumentRevisionsDebugMode")) {
                // Ignore -NSDocumentRevisionsDebugMode and the following YES/NO
                index++;
                continue;
            }
#endif //MAC

            args.push_back(arg);
        }

        if (args.size() > 0) {
            FBootFields->FArgs = args;
        }
    }
}

Package& Package::GetInstance() {
    static Package instance; // Guaranteed to be destroyed. Instantiated on first use.
    return instance;
}

Package::~Package(void) {
}

void Package::Shutdown() {
    if (FJVMUserConfig->IsModified()) {
        Platform& platform = Platform::GetInstance();
        FJVMUserConfig->SaveToFile(platform.GetJVMUserArgsConfigFileName());
    }

    delete FJVMUserConfig;
}

void Package::FreeBootFields() {
    delete FBootFields;
    FBootFields = NULL;
}

std::map<TString, TValueIndex> Package::GetJVMArgs() {
    return FBootFields->FJVMArgs;
}

std::map<TString, TValueIndex> Package::GetDefaultJVMUserArgs() {
    return FDefaultJVMUserArgs;
}

std::map<TString, TValueIndex> Package::GetJVMUserArgOverrides() {
    return Helpers::GetJVMUserArgsFromConfig(FJVMUserConfig);
}

void Package::SetJVMUserArgOverrides(std::map<TString, TValueIndex> Value) {
    // Overwrite JVM user config overrides with provided key/value pair.
    FJVMUserConfig->Assign(Helpers::GetConfigFromJVMUserArgs(Value));

    // Iterate over all new values and remove any that are the same as the defaults.
    std::map<TString, TValueIndex> defaults = GetDefaultJVMUserArgs();
    std::map<TString, TValueIndex> overrides = GetJVMUserArgOverrides();

    // Remove entries that are in the overrides that are the same as the defaults.
    for (std::map<TString, TValueIndex>::iterator iterator = overrides.begin();
        iterator != overrides.end();
        iterator++) {

        if (defaults.find(iterator->first) != defaults.end()) {
            TValueIndex defaultValue = defaults[iterator->first];

            if (defaultValue.value == iterator->second.value) {
                FJVMUserConfig->RemoveKey(iterator->first);
            }
        }
    }

    MergeJVMDefaultsWithOverrides();
}

std::map<TString, TValueIndex> Package::GetJVMUserArgs() {
    // Merge jvmuserarg defaults and jvmuserarg overrides to populate FJVMUserArgs.
    // 1. If the key is in the config file and not the java.user.preferences the default value is used,
    //    the one from the config file.
    // 2. If the key is in the java.user.preferences then the vaue from the java.user.preferences is used.
    //    The config file value is ignored.
    // 3. If the key is not in the config file but it is in the java.user.preferences then it is added anyway.
    //    And if it is removed it won�t show back up.
    if (FJVMUserConfig->IsModified() == true) {
        MergeJVMDefaultsWithOverrides();
    }

    return FJVMUserArgs;
}

void Package::MergeJVMDefaultsWithOverrides() {
    FJVMUserArgs.clear();
    FJVMUserArgs.insert(FDefaultJVMUserArgs.begin(), FDefaultJVMUserArgs.end());

    std::map<TString, TValueIndex> overrides = GetJVMUserArgOverrides();
    std::list<TString> indexedKeys = Helpers::GetOrderedKeysFromMap(overrides);

    size_t index = overrides.size() + 1;

    for (std::map<TString, TValueIndex>::iterator iterator = overrides.begin();
        iterator != overrides.end();
        iterator++) {

        TString name = iterator->first;
        TValueIndex item;

        if (FJVMUserArgs.find(name) != FJVMUserArgs.end()) {
            item = FJVMUserArgs[name];
            item.value = iterator->second.value;

        }
        else {
            item.value = iterator->second.value;
            item.index = index;
        }

        FJVMUserArgs[name] = item;
        index++;
    }
}

std::list<TString> Package::GetArgs() {
    return FBootFields->FArgs;
}

TString Package::GetPackageRootDirectory() {
    return FBootFields->FPackageRootDirectory;
}

TString Package::GetPackageAppDirectory() {
    return FBootFields->FPackageAppDirectory;
}

TString Package::GetPackageLauncherDirectory() {
    return FBootFields->FPackageLauncherDirectory;
}

TString Package::GetAppID() {
    return FBootFields->FAppID;
}

TString Package::GetClassPath() {
    return FBootFields->FClassPath;
}

TString Package::GetMainJar() {
    return FBootFields->FMainJar;
}

TString Package::GetMainClassName() {
    return FBootFields->FMainClassName;
}

bool Package::IsRuntimeBundled() {
    return FBootFields->FIsRuntimeBundled;
}

TString Package::GetJVMPath() {
    return FBootFields->FJVMPath;
}

TString Package::GetSplashScreenFileName() {
    return FBootFields->FSplashScreenFileName;
}

bool Package::HasSplashScreen() {
    return FilePath::FileExists(FBootFields->FSplashScreenFileName);
}

TString Package::GetCommandName() {
    return FBootFields->FCommandName;
}

size_t Package::GetMemorySize() {
    return FBootFields->FMemorySize;
}

PackageBootFields::MemoryState Package::GetMemoryState() {
    return FBootFields->FMemoryState;
}
