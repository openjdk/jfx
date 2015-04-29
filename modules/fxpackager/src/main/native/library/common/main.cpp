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
#include "PlatformString.h"
#include "FilePath.h"
#include "PropertyFile.h"
#include "JavaVirtualMachine.h"
#include "Package.h"
#include "PlatformThread.h"
#include "Macros.h"
#include "Messages.h"


#ifdef WINDOWS
#include <Shellapi.h>
#endif


#include <stdio.h>
#include <signal.h>
#include <stdlib.h>

/*
This is the launcher program for application packaging on Windows, Mac and Linux.

Basic approach:
  - Launcher executable loads packager.dll/libpackager.dylib/libpackager.so and calls start_launcher below.
  - Reads app/package.cfg or Info.plist or app/<appname>.cfg for application launch configuration
     (package.cfg is property file).
  - Load JVM with requested JVM settings (bundled client JVM if availble, server or installed JVM otherwise).
  - Wait for JVM to exit and then exit from Main
  - To debug application by set env variable (TODO) or pass "/Debug" option on command line.
  - TODO: default directory is set to user's Documents and Settings.
  - Application folder is added to the library path (so LoadLibrary()) works.

Limitations and future work:
  - Running Java code in primordial thread may cause problems
    (example: can not use custom stack size).
    Solution used by java launcher is to create a new thread to invoke JVM.
    See CR 6316197 for more information.
*/

extern "C" {

#ifdef WINDOWS
    BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {
        return true;
    }

    __declspec(dllexport)
#endif //WINDOWS

    bool start_launcher(int argc, TCHAR* argv[]) {
        bool result = false;
        bool parentProcess = true;

        // Platform must be initialize first.
        Platform& platform = Platform::GetInstance();

        try {
            for (int index = 0; index < argc; index++) {
                TString argument = argv[index];

                if (argument == _T("-Xappcds:generatecache")) {
                    platform.SetAppCDSState(cdsGenCache);
                }
                else if (argument == _T("-Xappcds:off")) {
                    platform.SetAppCDSState(cdsDisabled);
                }
                else if (argument == _T("-Xapp:child")) {
                    parentProcess = false;
                }
#ifdef DEBUG
//TODO There appears to be a compiler bug on Mac overloading ShowResponseMessage. Investigate.
                else if (argument == _T("-nativedebug")) {
                    if (platform.ShowResponseMessage(_T("Test"),
                                                     TString(_T("Would you like to debug?\n\nProcessID: ")) +
                                                     PlatformString(platform.GetProcessID()).toString()) == mrOK) {
                        while (platform.IsNativeDebuggerPresent() == false) {
                        }
                    }
                }
#endif //DEBUG
            }

            // Package must be initialized after Platform is fully initialized.
            Package& package = Package::GetInstance();
            Macros::Initialize();
            package.SetCommandLineArguments(argc, argv);
            platform.SetCurrentDirectory(package.GetPackageAppDirectory());

            switch (platform.GetAppCDSState()) {
                case cdsDisabled:
                case cdsUninitialized:
                case cdsEnabled: {
                    break;
                }

                case cdsGenCache: {
                        TString cacheDirectory = package.GetAppCDSCacheDirectory();

                        if (FilePath::DirectoryExists(cacheDirectory) == false) {
                            FilePath::CreateDirectory(cacheDirectory, true);
                        }
                        else {
                            TString cacheFileName = package.GetAppCDSCacheFileName();

                            if (FilePath::FileExists(cacheFileName) == true) {
                                FilePath::DeleteFile(cacheFileName);
                            }
                        }

                        break;
                    }

                case cdsAuto: {
                    TString cacheFileName = package.GetAppCDSCacheFileName();

                    if (parentProcess == true && FilePath::FileExists(cacheFileName) == false) {
                        AutoFreePtr<Process> process = platform.CreateProcess();
                        std::vector<TString> args;
                        args.push_back(_T("-Xappcds:generatecache"));
                        args.push_back(_T("-Xapp:child"));
                        process->Execute(platform.GetModuleFileName(), args, true);

                        if (FilePath::FileExists(cacheFileName) == false) {
                            // Cache does not exist after trying to generate it,
                            // so run without cache.
                            platform.SetAppCDSState(cdsDisabled);
                            package.Clear();
                            package.Initialize();
                        }
                    }

                    break;
                }
            }

            // Validation
            {
                switch (platform.GetAppCDSState()) {
                    case cdsDisabled:
                    case cdsGenCache: {
                        // Do nothing.
                        break;
                    }

                    case cdsEnabled:
                    case cdsAuto: {
                            TString cacheFileName = package.GetAppCDSCacheFileName();

                            if (FilePath::FileExists(cacheFileName) == false) {
                                Messages& messages = Messages::GetInstance();
                                TString message = PlatformString::Format(messages.GetMessage(APPCDS_CACHE_FILE_NOT_FOUND), cacheFileName.data());
                                throw FileNotFoundException(message);
                            }
                            break;
                        }

                    case cdsUninitialized: {
                        throw Exception(_T("Internal Error"));
                }
            }
            }

            // Run App
            result = RunVM();
        }
        catch (FileNotFoundException &e) {
            platform.ShowMessage(e.GetMessage());
        }

        return result;
    }

    void stop_launcher() {
    }
}
