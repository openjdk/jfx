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


#include "Package.h"
#include "Lock.h"
#include "Helpers.h"
#include "JavaUserPreferences.h"
#include "Macros.h"
#include "IniFile.h"

#include <assert.h>


Package::Package(void) {
    FInitialized = false;
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
    if (FInitialized == true) {
        return;
    }

    Platform& platform = Platform::GetInstance();

    FBootFields = new PackageBootFields();
    FDebugging = dsNone;

    FBootFields->FPackageRootDirectory = platform.GetPackageRootDirectory();
    FBootFields->FPackageAppDirectory = platform.GetPackageAppDirectory();
    FBootFields->FPackageLauncherDirectory = platform.GetPackageLauncherDirectory();
    FBootFields->FAppDataDirectory = platform.GetAppDataDirectory();

    std::map<TString, TString> keys = platform.GetKeys();

    // Read from configure.cfg/Info.plist
    AutoFreePtr<ISectionalPropertyContainer> config = platform.GetConfigFile(platform.GetConfigFileName());

    config->GetValue(keys[CONFIG_SECTION_APPLICATION], keys[CONFIG_APP_ID_KEY], FBootFields->FAppID);
    config->GetValue(keys[CONFIG_SECTION_APPLICATION], keys[PACKAGER_APP_DATA_DIR], FBootFields->FPackageAppDataDirectory);
    FBootFields->FPackageAppDataDirectory = FilePath::FixPathForPlatform(FBootFields->FPackageAppDataDirectory);

    // Main JAR.
    config->GetValue(keys[CONFIG_SECTION_APPLICATION], keys[CONFIG_MAINJAR_KEY], FBootFields->FMainJar);
    FBootFields->FMainJar = FilePath::IncludeTrailingSeparater(GetPackageAppDirectory()) +
                            FilePath::FixPathForPlatform(FBootFields->FMainJar);

    // Classpath.
    // 1. If the provided class path contains main jar then only use provided class path.
    // 2. If class path provided by config file is empty then add main jar.
    // 3. If main jar is not in provided class path then add it.
    config->GetValue(keys[CONFIG_SECTION_APPLICATION], keys[CONFIG_CLASSPATH_KEY], FBootFields->FClassPath);
    FBootFields->FClassPath = FilePath::FixPathSeparatorForPlatform(FBootFields->FClassPath);

    if (FBootFields->FClassPath.empty() == true) {
        FBootFields->FClassPath = GetMainJar();
    }
    else if (FBootFields->FClassPath.find(GetMainJar()) == TString::npos) {
        FBootFields->FClassPath = GetMainJar() + FilePath::PathSeparator() + FBootFields->FClassPath;
    }

    // Main Class.
    config->GetValue(keys[CONFIG_SECTION_APPLICATION], keys[CONFIG_MAINCLASSNAME_KEY], FBootFields->FMainClassName);

    // Splash Screen.
    if (config->GetValue(keys[CONFIG_SECTION_APPLICATION], keys[CONFIG_SPLASH_KEY], FBootFields->FSplashScreenFileName) == true) {
        FBootFields->FSplashScreenFileName = FilePath::IncludeTrailingSeparater(GetPackageAppDirectory()) +
                                             FilePath::FixPathForPlatform(FBootFields->FSplashScreenFileName);

        if (FilePath::FileExists(FBootFields->FSplashScreenFileName) == false) {
            FBootFields->FSplashScreenFileName = _T("");
        }
    }

    // Runtime.
    FBootFields->FIsRuntimeBundled = true;
    config->GetValue(keys[CONFIG_SECTION_APPLICATION], keys[JVM_RUNTIME_KEY], FBootFields->FJVMRuntimeDirectory);

    if (FBootFields->FJVMRuntimeDirectory.empty()) {
        FBootFields->FIsRuntimeBundled = false;
        FBootFields->FJVMRuntimeDirectory = platform.GetSystemJRE();
    }

    // Read jvmargs.
    PromoteAppCDSState(config);
    ReadJVMArgs(config);

    // Read args if none were passed in.
    if (FBootFields->FArgs.size() == 0) {
        OrderedMap<TString, TString> args;

        if (config->GetSection(keys[CONFIG_SECTION_ARGOPTIONS], args) == true) {
            FBootFields->FArgs = Helpers::MapToNameValueList(args);
        }
    }

    // Read jvmuserarg defaults.
    config->GetSection(keys[CONFIG_SECTION_JVMUSEROPTIONS], FDefaultJVMUserArgs);

    // Load JVM user overrides.
    TString jvmUserArgsConfigFileName = GetJVMUserArgsConfigFileName();

    if (FilePath::FileExists(jvmUserArgsConfigFileName) == true) {
        // Load new location for user VM overrides.
        IniFile userConfig;

        if (userConfig.LoadFromFile(jvmUserArgsConfigFileName) == false) {
            // New property file format was not found, attempt to load old property file format.
            userConfig.GetSection(keys[CONFIG_SECTION_JVMUSEROVERRIDESOPTIONS], FJVMUserArgsOverrides);
        }

        userConfig.GetSection(keys[CONFIG_SECTION_JVMUSEROVERRIDESOPTIONS], FJVMUserArgsOverrides);
    }
    else {
        // Attemp to load java.util.prefs for legacy JVM user overrides.
        AutoFreePtr<JavaUserPreferences> javaPreferences(JavaUserPreferences::CreateInstance());

        if (javaPreferences->Load(GetAppID()) == true) {
            FJVMUserArgsOverrides = javaPreferences->GetData();
        }
    }

    // Auto Memory.
    TString autoMemory;

    if (config->GetValue(keys[CONFIG_SECTION_APPLICATION], keys[CONFIG_APP_MEMORY], autoMemory) == true) {
        if (autoMemory == _T("auto") || autoMemory == _T("100%")) {
            FBootFields->FMemoryState = PackageBootFields::msAuto;
            FBootFields->FMemorySize = platform.GetMemorySize();
        }
        else if (autoMemory.length() == 2 && isdigit(autoMemory[0]) && autoMemory[1] == '%') {
            FBootFields->FMemoryState = PackageBootFields::msAuto;
            FBootFields->FMemorySize = StringToPercentageOfNumber(autoMemory.substr(0, 1), platform.GetMemorySize());
        }
        else if (autoMemory.length() == 3 && isdigit(autoMemory[0]) && isdigit(autoMemory[1]) && autoMemory[2] == '%') {
            FBootFields->FMemoryState = PackageBootFields::msAuto;
            FBootFields->FMemorySize = StringToPercentageOfNumber(autoMemory.substr(0, 2), platform.GetMemorySize());
        }
        else {
            FBootFields->FMemoryState = PackageBootFields::msManual;
            FBootFields->FMemorySize = 0;
        }
    }

    MergeJVMDefaultsWithOverrides();
}

void Package::Clear() {
    FreeBootFields();
    FJVMUserArgsOverrides.Clear();
    FDefaultJVMUserArgs.Clear();
    FJVMUserArgs.Clear();
    FInitialized = false;
}

// This is the only location that the AppCDS state should be modified except
// by command line arguments provided by the user.
//
// The state of AppCDS is as follows:
//
// -> cdsUninitialized
//    -> cdsGenCache If -Xappcds:generatecache
//    -> cdsDisabled If -Xappcds:off
//    -> cdsEnabled If "AppCDSJVMOptions" section is present
//    -> cdsAuto If "AppCDSJVMOptions" section is present and app.appcds.cache=auto
//    -> cdsDisabled Default
//
void Package::PromoteAppCDSState(ISectionalPropertyContainer* Config) {
    Platform& platform = Platform::GetInstance();
    std::map<TString, TString> keys = platform.GetKeys();

    // The AppCDS state can change at this point.
    switch (platform.GetAppCDSState()) {
        case cdsEnabled:
        case cdsAuto:
        case cdsDisabled:
        case cdsGenCache: {
            // Do nothing.
            break;
        }

        case cdsUninitialized: {
            if (Config->ContainsSection(keys[CONFIG_SECTION_APPCDSJVMOPTIONS]) == true) {
                // If the AppCDS section is present then enable AppCDS.
                TString appCDSCacheValue;

                // If running with AppCDS enabled, and the configuration has been setup so "auto" is enabled, then
                // the launcher will attempt to generate the cache file automatically and run the application.
                if (Config->GetValue(keys[CONFIG_SECTION_APPLICATION], _T("app.appcds.cache"), appCDSCacheValue) == true &&
                    appCDSCacheValue == _T("auto")) {
                    platform.SetAppCDSState(cdsAuto);
                }
                else {
                    platform.SetAppCDSState(cdsEnabled);
                }
            }
            else {

                platform.SetAppCDSState(cdsDisabled);
            }
        }
    }
}

void Package::ReadJVMArgs(ISectionalPropertyContainer* Config) {
    Platform& platform = Platform::GetInstance();
    std::map<TString, TString> keys = platform.GetKeys();

    // Evaluate based on the current AppCDS state.
    switch (platform.GetAppCDSState()) {
        case cdsUninitialized: {
            throw Exception(_T("Internal Error"));
    }

        case cdsDisabled: {
            Config->GetSection(keys[CONFIG_SECTION_JVMOPTIONS], FBootFields->FJVMArgs);
            break;
        }

        case cdsGenCache: {
            Config->GetSection(keys[CONFIG_SECTION_APPCDSGENERATECACHEJVMOPTIONS], FBootFields->FJVMArgs);
            break;
        }

        case cdsAuto:
        case cdsEnabled: {
            if (Config->GetValue(keys[CONFIG_SECTION_APPCDSJVMOPTIONS],
                                 _T( "-XX:SharedArchiveFile"), FBootFields->FAppCDSCacheFileName) == true) {
                // File names may contain the incorrect path separators. The cache file name must be
                // corrected at this point.
                if (FBootFields->FAppCDSCacheFileName.empty() == false) {
                    IniFile* iniConfig = dynamic_cast<IniFile*>(Config);

                    if (iniConfig != NULL) {
                        FBootFields->FAppCDSCacheFileName = FilePath::FixPathForPlatform(FBootFields->FAppCDSCacheFileName);
                        iniConfig->SetValue(keys[CONFIG_SECTION_APPCDSJVMOPTIONS],
                                     _T( "-XX:SharedArchiveFile"), FBootFields->FAppCDSCacheFileName);
                    }
                }

                Config->GetSection(keys[CONFIG_SECTION_APPCDSJVMOPTIONS], FBootFields->FJVMArgs);
            }

            break;
        }
    }
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
    FreeBootFields();
}

void Package::FreeBootFields() {
    if (FBootFields != NULL) {
        delete FBootFields;
        FBootFields = NULL;
    }
}

OrderedMap<TString, TString> Package::GetJVMArgs() {
    return FBootFields->FJVMArgs;
}

OrderedMap<TString, TString> Package::GetDefaultJVMUserArgs() {
    return FDefaultJVMUserArgs;
}

OrderedMap<TString, TString> Package::GetJVMUserArgOverrides() {
    return FJVMUserArgsOverrides;
}

std::vector<TString> GetKeysThatAreNotDuplicates(OrderedMap<TString, TString> &Defaults,
                                               OrderedMap<TString, TString> &Overrides) {
    std::vector<TString> result;
    std::vector<TString> overrideKeys = Overrides.GetKeys();

    for (size_t index = 0; index < overrideKeys.size(); index++) {
        TString overridesKey = overrideKeys[index];
        TString overridesValue;
        TString defaultValue;

        if ((Defaults.ContainsKey(overridesKey) == false) ||
           (Defaults.GetValue(overridesKey, defaultValue) == true &&
            Overrides.GetValue(overridesKey, overridesValue) == true &&
            defaultValue != overridesValue)) {
            result.push_back(overridesKey);
        }
    }

    return result;
}

OrderedMap<TString, TString> CreateOrderedMapFromKeyList(OrderedMap<TString, TString> &Map,
                                                         std::vector<TString> &Keys) {
    OrderedMap<TString, TString> result;

    for (size_t index = 0; index < Keys.size(); index++) {
        TString key = Keys[index];
        TString value;

        if (Map.GetValue(key, value) == true) {
            result.Append(key, value);
        }
    }

    return result;
}

void Package::SetJVMUserArgOverrides(OrderedMap<TString, TString> Value) {
    OrderedMap<TString, TString> defaults = GetDefaultJVMUserArgs();
    OrderedMap<TString, TString> overrides = Value;

    // 1. Remove entries in the overrides that are the same as the defaults.
    std::vector<TString> overrideKeys = GetKeysThatAreNotDuplicates(defaults, overrides);

    // 2. Create an ordered map from the overrides that weren't removed.
    FJVMUserArgsOverrides = CreateOrderedMapFromKeyList(overrides, overrideKeys);

    // 3. Overwrite JVM user config overrides with provided key/value pair.
    SaveJVMUserArgOverrides(FJVMUserArgsOverrides);

    // 4. Merge defaults and overrides to produce FJVMUserArgs.
    MergeJVMDefaultsWithOverrides();
}

void Package::SaveJVMUserArgOverrides(OrderedMap<TString, TString> Data) {
    IniFile userConfig;
    Platform& platform = Platform::GetInstance();
    std::map<TString, TString> keys = platform.GetKeys();
    userConfig.AppendSection(keys[CONFIG_SECTION_JVMUSEROVERRIDESOPTIONS], Data);
    userConfig.SaveToFile(GetJVMUserArgsConfigFileName());
}

OrderedMap<TString, TString> Package::GetJVMUserArgs() {
    return FJVMUserArgs;
}

std::vector<TString> GetKeysThatAreNotOverridesOfDefaultValues(OrderedMap<TString, TString> &Defaults,
                                                               OrderedMap<TString, TString> &Overrides) {
    std::vector<TString> result;
    std::vector<TString> keys = Overrides.GetKeys();

    for (unsigned int index = 0; index< keys.size(); index++) {
        TString key = keys[index];

        if (Defaults.ContainsKey(key) == true) {
            try {
                TString value = Overrides[key];
                Defaults[key] = value;
            }
            catch (std::out_of_range) {
            }
        }
        else {
            result.push_back(key);
        }
    }

    return result;
}

void Package::MergeJVMDefaultsWithOverrides() {
    // Merge jvmuserarg defaults and jvmuserarg overrides to populate FJVMUserArgs.
    // 1. If the key is in the config file and not the java.user.preferences the default value is used,
    //    the one from the config file.
    // 2. If the key is in the java.user.preferences then the value from the java.user.preferences is used and
    //    the config file value is ignored.
    // 3. If the key is not in the config file but it is in the java.user.preferences then it is added anyway.
    //    And if it is removed it won't show back up.
    FJVMUserArgs.Clear();
    FJVMUserArgs.Append(FDefaultJVMUserArgs);

    OrderedMap<TString, TString> overrides = GetJVMUserArgOverrides();

    // 1. Iterate over all elements in overrides to see if any items
    //    override a default value.
    std::vector<TString> keys = GetKeysThatAreNotOverridesOfDefaultValues(FJVMUserArgs, overrides);


    // 2. All remaining items in overrides are appended to the end.
    for (unsigned int index = 0; index< keys.size(); index++) {
        TString key = keys[index];
        TString value;

        if (overrides.GetValue(key, value) == true) {
            FJVMUserArgs.Append(key, value);
        }
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

TString Package::GetAppDataDirectory() {
    assert(FBootFields != NULL);
    return FBootFields->FAppDataDirectory;
}

TString Package::GetJVMUserArgsConfigFileName() {
    if (FJVMUserArgsConfigFileName.empty()) {
        Platform& platform = Platform::GetInstance();

        FJVMUserArgsConfigFileName = FilePath::IncludeTrailingSeparater(platform.GetAppDataDirectory()) +
                                        FilePath::IncludeTrailingSeparater(GetPackageAppDataDirectory()) +
                                        FilePath::IncludeTrailingSeparater(_T("packager")) +
                                        _T("jvmuserargs.cfg");
    }

    return FJVMUserArgsConfigFileName;
}

TString Package::GetAppCDSCacheDirectory() {
    if (FAppCDSCacheDirectory.empty()) {
        Platform& platform = Platform::GetInstance();
        FAppCDSCacheDirectory = FilePath::IncludeTrailingSeparater(platform.GetAppDataDirectory()) +
                                FilePath::IncludeTrailingSeparater(GetPackageAppDataDirectory()) +
                                _T("cache");

        Macros& macros = Macros::GetInstance();
        FAppCDSCacheDirectory = macros.ExpandMacros(FAppCDSCacheDirectory);
        FAppCDSCacheDirectory = FilePath::FixPathForPlatform(FAppCDSCacheDirectory);
    }

    return FAppCDSCacheDirectory;
}

TString Package::GetAppCDSCacheFileName() {
    assert(FBootFields != NULL);

    if (FBootFields->FAppCDSCacheFileName.empty() == false) {
        Macros& macros = Macros::GetInstance();
        FBootFields->FAppCDSCacheFileName = macros.ExpandMacros(FBootFields->FAppCDSCacheFileName);
        FBootFields->FAppCDSCacheFileName = FilePath::FixPathForPlatform(FBootFields->FAppCDSCacheFileName);
    }

    return FBootFields->FAppCDSCacheFileName;
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
            TString jvmRuntimePath = macros.ExpandMacros(GetJVMRuntimeDirectory());
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
