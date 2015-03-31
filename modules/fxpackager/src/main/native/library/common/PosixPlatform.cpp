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


#include "PosixPlatform.h"

#ifdef POSIX

#include "PlatformString.h"
#include "FilePath.h"

#include <assert.h>
#include <stdbool.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/sysctl.h>
#include <iostream>
#include <dlfcn.h>
#include <signal.h>


PosixPlatform::PosixPlatform(void) {
}

PosixPlatform::~PosixPlatform(void) {
}

MessageResponse PosixPlatform::ShowResponseMessage(TString title, TString description) {
    MessageResponse result = mrCancel;

    printf("%s %s (Y/N)\n", PlatformString(title).toPlatformString(), PlatformString(description).toPlatformString());
    fflush(stdout);

    std::string input;
    std::cin >> input;

    if (input == "Y") {
        result = mrOK;
    }

    return result;
}

//MessageResponse PosixPlatform::ShowResponseMessageB(TString description) {
//    TString appname = GetModuleFileName();
//    appname = FilePath::ExtractFileName(appname);
//    return ShowResponseMessage(appname, description);
//}

void PosixPlatform::SetCurrentDirectory(TString Value) {
    chdir(StringToFileSystemString(Value));
}

Module PosixPlatform::LoadLibrary(TString FileName) {
    return dlopen(StringToFileSystemString(FileName), RTLD_LAZY);
}

void PosixPlatform::FreeLibrary(Module AModule) {
    dlclose(AModule);
}

Procedure PosixPlatform::GetProcAddress(Module AModule, std::string MethodName) {
    return dlsym(AModule, PlatformString(MethodName));
}

std::vector<std::string> PosixPlatform::GetLibraryImports(const TString FileName) {
 std::vector<TString> result;
 return result;
}

std::vector<TString> PosixPlatform::FilterOutRuntimeDependenciesForPlatform(std::vector<TString> Imports) {
 std::vector<TString> result;
 return result;
}

Process* PosixPlatform::CreateProcess() {
    return new PosixProcess();
}

//--------------------------------------------------------------------------------------------------


PosixProcess::PosixProcess() : Process() {
    FChildPID = 0;
    FRunning = false;
}

PosixProcess::~PosixProcess() {
    Terminate();
}

void PosixProcess::Cleanup() {
#ifdef MAC
    sigaction(SIGINT, &savintr, (struct sigaction *)0);
    sigaction(SIGQUIT, &savequit, (struct sigaction *)0);
    sigprocmask(SIG_SETMASK, &saveblock, (sigset_t *)0);
#endif //MAC
}

bool PosixProcess::IsRunning() {
    bool result = false;

    if (kill(FChildPID, 0) == 0) {
        result = true;
    }

    return result;
}

bool PosixProcess::Terminate() {
    bool result = false;

    if (IsRunning() == true && FRunning == true) {
        FRunning = false;
        Cleanup();
        int status = kill(FChildPID, SIGTERM);

        if (status == 0) {
            result = true;
        }
        else {
#ifdef DEBUG
            if (errno == EINVAL)
                printf("Kill error: The value of the sig argument is an invalid or unsupported signal number.");
            else if (errno == EPERM)
                printf("Kill error: The process does not have permission to send the signal to any receiving process.");
            else if (errno == ESRCH)
                printf("Kill error: No process or process group can be found corresponding to that specified by pid.");
#endif //DEBUG
            if (IsRunning() == true) {
                status = kill(FChildPID, SIGKILL);

                if (status == 0) {
                    result = true;
                }
            }
        }
    }

    return result;
}

bool PosixProcess::Execute(const TString Application, const std::vector<TString> Arguments, bool AWait) {
    bool result = false;

    if (FRunning == false) {
        FRunning = true;

        struct sigaction sa;
        sa.sa_handler = SIG_IGN;
        sigemptyset(&sa.sa_mask);
        sa.sa_flags = 0;
#ifdef MAC
        sigemptyset(&savintr.sa_mask);
        sigemptyset(&savequit.sa_mask);
        sigaction(SIGINT, &sa, &savintr);
        sigaction(SIGQUIT, &sa, &savequit);
        sigaddset(&sa.sa_mask, SIGCHLD);
        sigprocmask(SIG_BLOCK, &sa.sa_mask, &saveblock);
#endif //MAC
        FChildPID = fork();

        // PID returned by vfork is 0 for the child process and the PID of the child
        // process for the parent.
        if (FChildPID == -1) {
            // Error
            TString message = PlatformString::Format(_T("Error: Unable to create process %s"), Application.data());
            throw Exception(message);
        }
        else if (FChildPID == 0) {
            Cleanup();
            TString command = Application;

            for (std::vector<TString>::const_iterator iterator = Arguments.begin(); iterator != Arguments.end(); iterator++) {
                command += TString(_T(" ")) + *iterator;
            }
#ifdef DEBUG
            printf("%s\n", command.data());
#endif //DEBUG
            execl("/bin/sh", "sh", "-c", command.data(), (char *)0);
            _exit(127);
        } else {
            if (AWait == true) {
                Wait();
                Cleanup();
                FRunning = false;
                result = true;
            }
            else {
                result = true;
            }
        }
    }

    return result;
}

bool PosixProcess::Wait() {
    bool result = false;

    int status;
    pid_t wpid;

    //TODO Use waitpid instead of wait
#ifdef LINUX
    wait();
#endif
#ifdef MAC
    wpid = wait(&status);
#endif

    if (!WIFEXITED(status) || WEXITSTATUS(status) != 0) {
        if (errno != EINTR){
            status = -1;
        }
    }

#ifdef DEBUG
    if (WIFEXITED(status)) {
        printf("child exited, status=%d\n", WEXITSTATUS(status));
    } else if (WIFSIGNALED(status)) {
        printf("child killed (signal %d)\n", WTERMSIG(status));
    } else if (WIFSTOPPED(status)) {
        printf("child stopped (signal %d)\n", WSTOPSIG(status));
#ifdef WIFCONTINUED // Not all implementations support this
    } else if (WIFCONTINUED(status)) {
        printf("child continued\n");
#endif //WIFCONTINUED
    } else { // Non-standard case -- may never happen
        printf("Unexpected status (0x%x)\n", status);
    }
#endif //DEBUG

    if (wpid != -1) {
        result = true;
    }

    return result;
}

TProcessID PosixProcess::GetProcessID() {
    return FChildPID;
}

#endif //POSIX
