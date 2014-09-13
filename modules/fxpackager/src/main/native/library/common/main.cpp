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

#include "jni.h"

#include "Lock.h" //TODO remove?

#ifdef WINDOWS
#include <Shellapi.h>
#endif

#ifdef MAC
#include <locale.h>
#include <pthread.h>
#endif //MAC


// Create the VM from a thread so we have the main thread.
class VMThread : public PlatformThread {
public:
    virtual void Execute() {
        Platform& platform = Platform::GetInstance();
        Package& package = Package::GetInstance();

        TString appFolder = package.GetPackageAppDirectory();
        platform.SetCurrentDirectory(appFolder);
        JavaVirtualMachine javavm;

        if (javavm.StartJVM() == true) {
        }
        else {
            platform.ShowError(_T("Failed to launch JVM\n"));
        }
    }
};

//TODO update comment.
/*
   This is launcher program for application package on Windows.

   Basic approach:
      - we read app/package.cfg file to find out details on what and how to launch
         (package.cfg is property file)
      - load JVM with requested JVM settings (client JVM if availble, server otherwise)
      - load embedded launcher class com.javafx.main.Main class and run main()
      - wait for JVM to exit and then exit from WinMain
      - support a way to "debug" application by setting env variable
        or passing "/Debug" option on command line
      - TODO: default directory is set to user's Documents and Settings
      - TODO: application folder is added to the library path (so LoadLibrary()) works

   Limitations and future work:
      - Running Java code in primordial thread may cause problems
        (example: can not use custom stack size).
        Solution used by java launcher is to create a new thread to invoke JVM.
        See CR 6316197 for more information.
      - Reuse code between windows/linux launchers and borrow more code from
        java.exe launcher implementation.
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
        
        // Platform and Package must be initialize first.
        Platform& platform = Platform::GetInstance();

#ifdef DEBUG
        if (argc > 1 && TString(argv[1]) == _T("/Debug")) {
#ifdef WINDOWS
            if (::MessageBox(NULL, PlatformString(platform.GetProcessID()), _T("Debug?"), MB_OKCANCEL) != IDCANCEL) {
#endif //WINDOWS
#ifdef POSIX
            printf("%s\n", PlatformString(platform.GetProcessID()).c_str());
#endif //POSIX
                while (platform.IsNativeDebuggerPresent() == false) {
                }
#ifdef WINDOWS
            }
#endif //WINDOWS
        }
#endif //DEBUG

        Package& package = Package::GetInstance();

        package.SetCommandLineArguments(argc, argv);
        platform.SetCurrentDirectory(package.GetPackageAppDirectory());
        JavaVirtualMachine javavm;

        if (javavm.StartJVM() == true) {
            result = true;
        }
        else {
            platform.ShowError(_T("Failed to launch JVM\n"));
        }

        return result;
    }
    
    void stop_launcher() {
        Package& package = Package::GetInstance();
        package.Shutdown();
    }
}
