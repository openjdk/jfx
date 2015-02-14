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
#include "Macros.h"

#include <assert.h>


Package::Package(void) {
    Initialize();
}

TPlatformNumber StringToPercentageOfNumber(TString Value, TPlatformNumber Number) {
    TPlatformNumber result = 0;
    size_t percentage = atoi(PlatformString(Value.c_str()));

    if (percentage > 0 && Number > 0) {
        result = Number * percentage / 100;
    }

    return result;
}

void Package::Initialize() {
    Platform& platform = Platform::GetInstance();

    FBootFields = new PackageBootFields();
    FDebugging = dsNone;
    std::map<TString, TString> keys = platform.GetKeys();

    FBootFields->FPackageRootDirectory = platform.GetPackageRootDirectory();
    FBootFields->FPackageAppDirectory = platform.GetPackageAppDirectory();
    FBootFields->FPackageLauncherDirectory = platform.GetPackageLauncherDirectory();

    // Read from configure.cfg/Info.plist

    AutoFreePtr<PropertyContainer> config = platform.GetConfigFile(platform.GetConfigFileName());
    config->GetValue(keys[CONFIG_APP_ID_KEY], FBootFields->FAppID);
    config->GetValue(keys[PACKAGER_APP_DATA_DIR], FBootFields->FPackageAppDataDirectory);
    
    // Auto Memory.
    TString temp;
    config->GetValue(keys[CONFIG_APP_MEMORY], temp);
    
    if (temp == _T("auto") || temp == _T("100%")) {
        FBootFields->FMemoryState = PackageBootFields::msAuto;
        FBootFields->FMemorySize = platform.GetMemorySize();
    }
    else if (temp.length() == 2 && isdigit(temp[0]) && temp[1] == '%') {
        FBootFields->FMemoryState = PackageBootFields::msAuto;
        FBootFields->FMemorySize = StringToPercentageOfNumber(temp.substr(0, 1), platform.GetMemorySize());
    }
    else if (temp.length() == 3 && isdigit(temp[0]) && isdigit(temp[1]) && temp[2] == '%') {
        FBootFields->FMemoryState = PackageBootFields::msAuto;
        FBootFields->FMemorySize = StringToPercentageOfNumber(temp.substr(0, 2), platform.GetMemorySize());
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

    FBootFields->FIsRuntimeBundled = true;
    config->GetValue(keys[JVM_RUNTIME_KEY], FBootFields->FJVMRuntimeDirectory);
    
    if (FBootFields->FJVMRuntimeDirectory.empty()) {
        FBootFields->FIsRuntimeBundled = false;
        FBootFields->FJVMRuntimeDirectory = platform.GetSystemJRE();
    }

    // Read args if none were passed in.
    if (FBootFields->FArgs.size() == 0) {
        FBootFields->FArgs = Helpers::GetArgsFromConfig(config);
    }  

    // Read all jvmargs.
    FBootFields->FJVMArgs = Helpers::GetJVMArgsFromConfig(config);

    // Read all jvmuserarg defaults.
    FDefaultJVMUserArgs = Helpers::GetJVMUserArgsFromConfig(config);

    // Load JVM user overrides.
    TString jvmUserArgsConfigFileName = GetJVMUserArgsConfigFileName();

    if (FilePath::FileExists(jvmUserArgsConfigFileName) == true) {
        // Load new location for user VM overrides.
        AutoFreePtr<PropertyFile> userConfig = new PropertyFile(jvmUserArgsConfigFileName);
        FJVMUserArgsOverrides = Helpers::GetJVMUserArgsFromConfig(userConfig);
    }
    else {
        // Attemp to load java.util.prefs for legacy JVM user overrides.
        AutoFreePtr<JavaUserPreferences> javaPreferences = JavaUserPreferences::CreateInstance();

        if (javaPreferences->Load(GetAppID()) == true) {
            FJVMUserArgsOverrides = javaPreferences->GetData();
        }
    }

    MergeJVMDefaultsWithOverrides();
}

void Package::SetCommandLineArguments(int argc, TCHAR* argv[]) {
    if (argc > 0) {
        std::list<TString> args;

        // Prepare app arguments. Skip value at index 0 - this is path to executable.
        FBootFields->FCommandName = argv[0];

        // Path to executable is at 0 index so start at index 1.
        for (int index = 1; index < argc; index++) {
            TString arg = argv[index];

#ifdef DEBUG
            if (arg == _T("-debug")) {
                FDebugging = dsNative;
            }
            
            if (arg == _T("-javadebug")) {
                FDebugging = dsJava;
            }
#endif //DEBUG
#ifdef MAC
            if (arg.find(_T("-psn_"), 0) != TString::npos) {
                Platform& platform = Platform::GetInstance();

                if (platform.IsMainThread() == true) {
#ifdef DEBUG
                    printf("%s\n", arg.c_str());
#endif //DEBUG
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

void Package::FreeBootFields() {
    delete FBootFields;
    FBootFields = NULL;
}

TOrderedMap Package::GetJVMArgs() {
    assert(FBootFields != NULL);
    return FBootFields->FJVMArgs;
}

TOrderedMap Package::GetDefaultJVMUserArgs() {
    return FDefaultJVMUserArgs;
}

TOrderedMap Package::GetJVMUserArgOverrides() {
    return FJVMUserArgsOverrides;//Helpers::GetJVMUserArgsFromConfig(FJVMUserConfig);
}

void Package::SetJVMUserArgOverrides(TOrderedMap Value) {
    TOrderedMap defaults = GetDefaultJVMUserArgs();
    TOrderedMap overrides = Value;
    std::list<TString> overrideKeys = Helpers::GetOrderedKeysFromMap(overrides);

    // 1. Remove entries in the overrides that are the same as the defaults.
    for (TOrderedMap::const_iterator iterator = overrides.begin();
        iterator != overrides.end();
        iterator++) {

        TString overridesKey = iterator->first;
        TString overridesValue = iterator->second.value;
        
        if (defaults.find(overridesKey) != defaults.end()) {
            TString defaultValue = defaults[overridesKey].value;
            
            if (defaultValue == overridesValue) {
                overrideKeys.remove(overridesKey);
            }
        }
    }

    // 2. Create an ordered map from the overrides that weren't removed.
    TOrderedMap orderedOverrides;
    size_t index = 1;

    for (std::list<TString>::const_iterator iterator = overrideKeys.begin();
         iterator != overrideKeys.end(); iterator++) {
        TString key = *iterator;
        TValueIndex item;
        item.value = overrides[key].value;
        item.index = index;

        orderedOverrides.insert(TOrderedMap::value_type(key, item));
        index++;
    }
    
    FJVMUserArgsOverrides = orderedOverrides;

    // 3. Overwrite JVM user config overrides with provided key/value pair.
    SaveJVMUserArgOverrides(orderedOverrides);

    // 4. Merge defaults and overrides to produce FJVMUserArgs.
    MergeJVMDefaultsWithOverrides();
}

void Package::SaveJVMUserArgOverrides(TOrderedMap Data) {
    AutoFreePtr<PropertyFile> userConfig = new PropertyFile();
    userConfig->Assign(Helpers::GetConfigFromJVMUserArgs(Data));
    userConfig->SetReadOnly(false);
    userConfig->SaveToFile(GetJVMUserArgsConfigFileName(), true);
}

TOrderedMap Package::GetJVMUserArgs() {
    return FJVMUserArgs;
}

void Package::MergeJVMDefaultsWithOverrides() {
    // Merge jvmuserarg defaults and jvmuserarg overrides to populate FJVMUserArgs.
    // 1. If the key is in the config file and not the java.user.preferences the default value is used,
    //    the one from the config file.
    // 2. If the key is in the java.user.preferences then the value from the java.user.preferences is used and
    //    the config file value is ignored.
    // 3. If the key is not in the config file but it is in the java.user.preferences then it is added anyway.
    //    And if it is removed it won't show back up.
    FJVMUserArgs.clear();
    FJVMUserArgs.insert(FDefaultJVMUserArgs.begin(), FDefaultJVMUserArgs.end());

    TOrderedMap overrides = GetJVMUserArgOverrides();
    std::list<TString> indexedKeys = Helpers::GetOrderedKeysFromMap(overrides);

    // 1. Iterate over all elements in overrides to see if any items
    //    override a default value.
    for (TOrderedMap::iterator iterator = overrides.begin();
         iterator != overrides.end();
         iterator++) {

        TString name = iterator->first;
        TString value = iterator->second.value;
        TValueIndex item;

        if (FJVMUserArgs.find(name) != FJVMUserArgs.end()) {
            item = FJVMUserArgs[name];
            item.value = value;
            FJVMUserArgs[name] = item;
            indexedKeys.remove(name);
        }
    }
    
    // 2. All remaining items in overrides are appended to the end.
    size_t index = FDefaultJVMUserArgs.size();
    
    for (std::list<TString>::const_iterator iterator = indexedKeys.begin();
         iterator != indexedKeys.end(); iterator++) {
        
        TString name = *iterator;
        TValueIndex item = overrides[name];
        item.index = index;
        index++;
        FJVMUserArgs[name] = item;
    }
}

std::list<TString> Package::GetArgs() {
    assert(FBootFields != NULL);
    return FBootFields->FArgs;
}

TString Package::GetPackageRootDirectory() {
    assert(FBootFields != NULL);
    return FBootFields->FPackageRootDirectory;
}

TString Package::GetPackageAppDirectory() {
    assert(FBootFields != NULL);
    return FBootFields->FPackageAppDirectory;
}

TString Package::GetPackageLauncherDirectory() {
    assert(FBootFields != NULL);
    return FBootFields->FPackageLauncherDirectory;
}

TString Package::GetJVMUserArgsConfigFileName() {
    if (FJVMUserArgsConfigFileName.empty()) {
        Platform& platform = Platform::GetInstance();

        FJVMUserArgsConfigFileName = FilePath::IncludeTrailingSlash(platform.GetAppDataDirectory()) +
                                        FilePath::IncludeTrailingSlash(GetPackageAppDataDirectory()) +
                                        FilePath::IncludeTrailingSlash(_T("packager")) +
                                        _T("jvmuserargs.cfg");
    }
    
    return FJVMUserArgsConfigFileName;
}

TString Package::GetAppID() {
    assert(FBootFields != NULL);
    return FBootFields->FAppID;
}

TString Package::GetPackageAppDataDirectory() {
    assert(FBootFields != NULL);
    return FBootFields->FPackageAppDataDirectory;
}

TString Package::GetClassPath() {
    assert(FBootFields != NULL);
    return FBootFields->FClassPath;
}

TString Package::GetMainJar() {
    assert(FBootFields != NULL);
    return FBootFields->FMainJar;
}

TString Package::GetMainClassName() {
    assert(FBootFields != NULL);
    return FBootFields->FMainClassName;
}

bool Package::IsRuntimeBundled() {
    assert(FBootFields != NULL);
    return FBootFields->FIsRuntimeBundled;
}

TString Package::GetJVMLibraryFileName() {
    assert(FBootFields != NULL);
    
    if (FBootFields->FJVMLibraryFileName.empty() == true) {
        Platform& platform = Platform::GetInstance();
        if (IsRuntimeBundled() == true) {
            Macros& macros = Macros::GetInstance();
            TString jvmRuntimePath = macros.ExpandMacros(FBootFields->FJVMRuntimeDirectory);
            FBootFields->FJVMLibraryFileName = platform.GetBundledJVMLibraryFileName(jvmRuntimePath);
        }
        else {
            FBootFields->FJVMLibraryFileName = platform.GetSystemJVMLibraryFileName();
        }
    }

    return FBootFields->FJVMLibraryFileName;
}

TString Package::GetJVMRuntimeDirectory() {
    assert(FBootFields != NULL);
    return FBootFields->FJVMRuntimeDirectory;
}

TString Package::GetSplashScreenFileName() {
    assert(FBootFields != NULL);
    return FBootFields->FSplashScreenFileName;
}

bool Package::HasSplashScreen() {
    assert(FBootFields != NULL);
    return FilePath::FileExists(FBootFields->FSplashScreenFileName);
}

TString Package::GetCommandName() {
    assert(FBootFields != NULL);
    return FBootFields->FCommandName;
}

TPlatformNumber Package::GetMemorySize() {
    assert(FBootFields != NULL);
    return FBootFields->FMemorySize;
}

PackageBootFields::MemoryState Package::GetMemoryState() {
    assert(FBootFields != NULL);
    return FBootFields->FMemoryState;
}

DebugState Package::Debugging() {
    return FDebugging;
}
