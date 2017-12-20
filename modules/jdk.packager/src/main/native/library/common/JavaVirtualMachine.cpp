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


#include "JavaVirtualMachine.h"
#include "Platform.h"
#include "PlatformString.h"
#include "FilePath.h"
#include "Package.h"
#include "Java.h"
#include "Helpers.h"
#include "Messages.h"
#include "Macros.h"
#include "PlatformThread.h"

#include "jni.h"

#include <map>
#include <list>
#include <sstream>


bool RunVM(JvmLaunchType type) {
    bool result = false;
    JavaVirtualMachine javavm;

    switch (type){
        case USER_APP_LAUNCH:
            result = javavm.StartJVM();
            break;
        case SINGLE_INSTANCE_NOTIFICATION_LAUNCH:
            result = javavm.NotifySingleInstance();
            break;
    }

    if (!result) {
        Platform& platform = Platform::GetInstance();
        platform.ShowMessage(_T("Failed to launch JVM\n"));
    }

    return result;
}

JavaLibrary::JavaLibrary() : Library(), FCreateProc(NULL)  {
}

bool JavaLibrary::JavaVMCreate(size_t argc, char *argv[]) {
    if (FCreateProc == NULL) {
        FCreateProc = (JVM_CREATE)GetProcAddress(LAUNCH_FUNC);
    }

    if (FCreateProc == NULL) {
        Platform& platform = Platform::GetInstance();
        Messages& messages = Messages::GetInstance();
        platform.ShowMessage(messages.GetMessage(FAILED_LOCATING_JVM_ENTRY_POINT));
        return false;
    }

    return FCreateProc((int)argc, argv,
            0, NULL,
            0, NULL,
            "",
            "",
            "java",
            "java",
            false,
            false,
            false,
            0) == 0;
}

//--------------------------------------------------------------------------------------------------

JavaOptions::JavaOptions(): FOptions(NULL) {
}

JavaOptions::~JavaOptions() {
    if (FOptions != NULL) {
        for (unsigned int index = 0; index < GetCount(); index++) {
            delete[] FOptions[index].optionString;
        }

        delete[] FOptions;
    }
}

void JavaOptions::AppendValue(const TString Key, TString Value, void* Extra) {
    JavaOptionItem item;
    item.name = Key;
    item.value = Value;
    item.extraInfo = Extra;
    FItems.push_back(item);
}

void JavaOptions::AppendValue(const TString Key, TString Value) {
    AppendValue(Key, Value, NULL);
}

void JavaOptions::AppendValue(const TString Key) {
    AppendValue(Key, _T(""), NULL);
}

void JavaOptions::AppendValues(OrderedMap<TString, TString> Values) {
    std::vector<TString> orderedKeys = Values.GetKeys();

    for (std::vector<TString>::const_iterator iterator = orderedKeys.begin();
        iterator != orderedKeys.end(); iterator++) {
        TString name = *iterator;
        TString value;

        if (Values.GetValue(name, value) == true) {
            AppendValue(name, value);
        }
    }
}

void JavaOptions::ReplaceValue(const TString Key, TString Value) {
    for (std::list<JavaOptionItem>::iterator iterator = FItems.begin();
        iterator != FItems.end(); iterator++) {

        TString lkey = iterator->name;

        if (lkey == Key) {
            JavaOptionItem item = *iterator;
            item.value = Value;
            iterator = FItems.erase(iterator);
            FItems.insert(iterator, item);
            break;
        }
    }
}

std::list<TString> JavaOptions::ToList() {
    std::list<TString> result;
    Macros& macros = Macros::GetInstance();

    for (std::list<JavaOptionItem>::const_iterator iterator = FItems.begin();
        iterator != FItems.end(); iterator++) {
        TString key = iterator->name;
        TString value = iterator->value;
        TString option = Helpers::NameValueToString(key, value);
        option = macros.ExpandMacros(option);
        result.push_back(option);
    }

    return result;
}

size_t JavaOptions::GetCount() {
    return FItems.size();
}

// jvmuserargs can have a trailing equals in the key. This needs to be removed to use
// other parts of the launcher.
OrderedMap<TString, TString> RemoveTrailingEquals(OrderedMap<TString, TString> Map) {
    OrderedMap<TString, TString> result;

    std::vector<TString> keys = Map.GetKeys();

    for (size_t index = 0; index < keys.size(); index++) {
        TString name = keys[index];
        TString value;

        if (Map.GetValue(name, value) == true) {
            // If the last character of the key is an equals, then remove it. If there is no
            // equals then combine the two as a key.
            TString::iterator i = name.end();
            i--;

            if (*i == '=') {
                name = name.substr(0, name.size() - 1);
            }
            else {
                i = value.begin();

                if (*i == '=') {
                    value = value.substr(1, value.size() - 1);
                }
                else {
                    name = name + value;
                    value = _T("");
                }
            }

            result.Append(name, value);
        }
    }

    return result;
}

//--------------------------------------------------------------------------------------------------

JavaVirtualMachine::JavaVirtualMachine() {
}

JavaVirtualMachine::~JavaVirtualMachine(void) {
}

bool JavaVirtualMachine::StartJVM() {
    Platform& platform = Platform::GetInstance();
    Package& package = Package::GetInstance();

    TString classpath = package.GetClassPath();
    TString modulepath = package.GetModulePath();
    JavaOptions options;

    if (modulepath.empty() == false) {
        options.AppendValue(_T("-Djava.module.path"), modulepath);
    }

    options.AppendValue(_T("-Djava.library.path"), package.GetPackageAppDirectory() + FilePath::PathSeparator() + package.GetPackageLauncherDirectory());
    options.AppendValue(_T("-Djava.launcher.path"), package.GetPackageLauncherDirectory());
    options.AppendValue(_T("-Dapp.preferences.id"), package.GetAppID());
    options.AppendValues(package.GetJVMArgs());
    options.AppendValues(RemoveTrailingEquals(package.GetJVMUserArgs()));

#ifdef DEBUG
    if (package.Debugging() == dsJava) {
        options.AppendValue(_T("-Xdebug"), _T(""));
        options.AppendValue(_T("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=localhost:5005"), _T(""));
        platform.ShowMessage(_T("localhost:5005"));
    }
#endif //DEBUG

    TString maxHeapSizeOption;
    TString minHeapSizeOption;


    if (package.GetMemoryState() == PackageBootFields::msAuto) {
        TPlatformNumber memorySize = package.GetMemorySize();
        TString memory = PlatformString((size_t)memorySize).toString() + _T("m");
        maxHeapSizeOption = TString(_T("-Xmx")) + memory;
        options.AppendValue(maxHeapSizeOption, _T(""));

        if (memorySize > 256)
            minHeapSizeOption = _T("-Xms256m");
        else
            minHeapSizeOption = _T("-Xms") + memory;

        options.AppendValue(minHeapSizeOption, _T(""));
    }

    TString mainClassName = package.GetMainClassName();
    TString mainModule = package.GetMainModule();

    if (mainClassName.empty() == true && mainModule.empty() == true) {
        Messages& messages = Messages::GetInstance();
        platform.ShowMessage(messages.GetMessage(NO_MAIN_CLASS_SPECIFIED));
        return false;
    }

    configureLibrary();

    // Initialize the arguments to JLI_Launch()
    //
    // On Mac OS X JLI_Launch spawns a new thread that actually starts the JVM. This
    // new thread simply re-runs main(argc, argv). Therefore we do not want
    // to add new args if we are still in the original main thread so we
    // will treat them as command line args provided by the user ...
    // Only propagate original set of args first time.

    options.AppendValue(_T("-classpath"));
    options.AppendValue(classpath);

    std::list<TString> vmargs;
    vmargs.push_back(package.GetCommandName());

    if (package.HasSplashScreen() == true) {
        options.AppendValue(TString(_T("-splash:")) + package.GetSplashScreenFileName(), _T(""));
    }

    if (mainModule.empty() == true) {
        options.AppendValue(Helpers::ConvertJavaPathToId(mainClassName), _T(""));
    }
    else {
        options.AppendValue(_T("-m"));
        options.AppendValue(mainModule);
    }

    return launchVM(options, vmargs, false);
}

bool JavaVirtualMachine::NotifySingleInstance() {
    Package& package = Package::GetInstance();

    std::list<TString> vmargs;
    vmargs.push_back(package.GetCommandName());

    JavaOptions options;
    options.AppendValue(_T("-Djava.library.path"), package.GetPackageAppDirectory()
                      + FilePath::PathSeparator() + package.GetPackageLauncherDirectory());
    options.AppendValue(_T("-Djava.launcher.path"), package.GetPackageLauncherDirectory());
    // launch SingleInstanceNewActivation.main() to pass arguments to another instance
    options.AppendValue(_T("-m"));
    options.AppendValue(_T("jdk.packager.services/jdk.packager.services.singleton.SingleInstanceNewActivation"));

    configureLibrary();

    return launchVM(options, vmargs, true);
}

void JavaVirtualMachine::configureLibrary() {
    Platform& platform = Platform::GetInstance();
    Package& package = Package::GetInstance();
    // TODO: Clean this up. Because of bug JDK-8131321 the opening of the PE file
    // fails in WindowsPlatform.cpp on the check to
    // if (pNTHeader->Signature == IMAGE_NT_SIGNATURE)
    TString libName = package.GetJVMLibraryFileName();
#ifdef _WIN64
    if (FilePath::FileExists(_T("msvcr100.dll")) == true) {
        javaLibrary.AddDependency(_T("msvcr100.dll"));
    }
#else
    javaLibrary.AddDependencies(
        platform.FilterOutRuntimeDependenciesForPlatform(platform.GetLibraryImports(libName)));
#endif
    javaLibrary.Load(libName);
}

bool JavaVirtualMachine::launchVM(JavaOptions& options, std::list<TString>& vmargs, bool addSiProcessId) {
    Platform& platform = Platform::GetInstance();
    Package& package = Package::GetInstance();

#ifdef MAC
    // Mac adds a ProcessSerialNumber to args when launched from .app
    // filter out the psn since they it's not expected in the app
    if (platform.IsMainThread() == false) {
        std::list<TString> loptions = options.ToList();
        vmargs.splice(vmargs.end(), loptions, loptions.begin(), loptions.end());
    }
#else
    std::list<TString> loptions = options.ToList();
    vmargs.splice(vmargs.end(), loptions, loptions.begin(), loptions.end());
#endif

    if (addSiProcessId) {
        // add single instance process ID as a first argument
        TProcessID pid = platform.GetSingleInstanceProcessId();
        std::ostringstream s;
        s << pid;
        std::string procIdStr(s.str());
        vmargs.push_back(TString(procIdStr.begin(), procIdStr.end()));
    }

    std::list<TString> largs = package.GetArgs();
    vmargs.splice(vmargs.end(), largs, largs.begin(), largs.end());

    size_t argc = vmargs.size();
    DynamicBuffer<char*> argv(argc + 1);
    unsigned int index = 0;
    for (std::list<TString>::const_iterator iterator = vmargs.begin();
        iterator != vmargs.end(); iterator++) {
        TString item = *iterator;
        std::string arg = PlatformString(item).toStdString();
#ifdef DEBUG
        printf("%i %s\n", index, arg.c_str());
#endif //DEBUG
        argv[index] = PlatformString::duplicate(arg.c_str());
        index++;
    }

    argv[argc] = NULL;

// On Mac we can only free the boot fields if the calling thread is not the main thread.
#ifdef MAC
    if (platform.IsMainThread() == false) {
        package.FreeBootFields();
    }
#else
    package.FreeBootFields();
#endif //MAC

    if (javaLibrary.JavaVMCreate(argc, argv.GetData()) == true) {
        return true;
    }

    for (index = 0; index < argc; index++) {
        if (argv[index] != NULL) {
            delete[] argv[index];
        }
    }

    return false;
}
