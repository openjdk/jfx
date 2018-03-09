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


#include "PosixPlatform.h"

#ifdef POSIX

#include "PlatformString.h"
#include "FilePath.h"
#include "Helpers.h"

#include <assert.h>
#include <stdbool.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/sysctl.h>
#include <sys/file.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <errno.h>
#include <limits.h>
#include <pwd.h>
#include <iostream>
#include <algorithm>
#include <dlfcn.h>
#include <signal.h>


PosixPlatform::PosixPlatform(void) {
}

PosixPlatform::~PosixPlatform(void) {
    if (!SingleInstanceFile.empty()) {
        unlink(SingleInstanceFile.c_str());
    }
}

TString PosixPlatform::GetTempDirectory() {
    struct passwd* pw = getpwuid(getuid());
    TString homedir(pw->pw_dir);
    homedir += getTmpDirString();
    if (!FilePath::DirectoryExists(homedir)) {
        if (!FilePath::CreateDirectory(homedir, false)) {
            homedir.clear();
        }
    }

    return homedir;
}

TString PosixPlatform::fixName(const TString& name) {
    TString fixedName(name);
    const TString chars("?:*<>/\\");
    for (TString::const_iterator it = chars.begin(); it != chars.end(); it++) {
        fixedName.erase(std::remove(fixedName.begin(), fixedName.end(), *it), fixedName.end());
    }
    return fixedName;
}

// returns true if another instance is already running.
// if false, we need to continue regular launch.
bool PosixPlatform::CheckForSingleInstance(TString appName) {
    TString tmpDir = GetTempDirectory();
    if (tmpDir.empty()) {
        printf("Unable to check for single instance.\n");
        return false;
    }

    TString lockFile = tmpDir + "/" + fixName(appName);
    SingleInstanceFile = lockFile;
    int pid_file = open(lockFile.c_str(), O_CREAT | O_RDWR, 0666);
    int rc = flock(pid_file, LOCK_EX | LOCK_NB);

    if (rc) {
        if (EWOULDBLOCK == errno) {
            // another instance is running
            pid_t pid = 0;
            read(pid_file, (void*)&pid, sizeof(pid_t));
            printf("Another instance is running PID: %d\n", pid);
            if (pid != 0) {
                singleInstanceProcessId = pid;
                SingleInstanceFile.clear();
                return true;
            }
        } else {
            printf("Unable to check for single instance.\n");
        }
    } else {
        // It is the first instance.
        pid_t pid = getpid();
        write(pid_file, (void*)&pid, sizeof(pid_t));
    }

    return false;
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
    FOutputHandle = 0;
    FInputHandle = 0;
}

PosixProcess::~PosixProcess() {
    Terminate();
}

void PosixProcess::Cleanup() {
    if (FOutputHandle != 0) {
        close(FOutputHandle);
        FOutputHandle = 0;
    }

    if (FInputHandle != 0) {
        close(FInputHandle);
        FInputHandle = 0;
    }

#ifdef MAC
    sigaction(SIGINT, &savintr, (struct sigaction *)0);
    sigaction(SIGQUIT, &savequit, (struct sigaction *)0);
    sigprocmask(SIG_SETMASK, &saveblock, (sigset_t *)0);
#endif //MAC
}

bool PosixProcess::ReadOutput() {
    bool result = false;

    if (FOutputHandle != 0 && IsRunning() == true) {
        char buffer[4096];
        //select(p[0] + 1, &rfds, NULL, NULL, NULL);

        ssize_t count = read(FOutputHandle, buffer, sizeof(buffer));

        if (count == -1) {
            if (errno == EINTR) {
                //continue;
            } else {
                perror("read");
                exit(1);
            }
        } else if (count == 0) {
            //break;
        } else {
            if (buffer[count] == EOF) {
                buffer[count] = '\0';
            }

            std::list<TString> output = Helpers::StringToArray(buffer);
            FOutput.splice(FOutput.end(), output, output.begin(), output.end());
            result = true;
        }
    }

    return false;
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

#define PIPE_READ 0
#define PIPE_WRITE 1

bool PosixProcess::Execute(const TString Application, const std::vector<TString> Arguments, bool AWait) {
    bool result = false;

    if (FRunning == false) {
        FRunning = true;

        int handles[2];

        if (pipe(handles) == -1) {
            //perror("pipe");
            //exit(1);
            return false;
        }

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
//            dup2(FOutputHandle, STDOUT_FILENO);
//            dup2(FInputHandle, STDIN_FILENO);
//            close(FOutputHandle);
//            close(FInputHandle);

            dup2(handles[PIPE_READ], STDIN_FILENO);
            dup2(handles[PIPE_WRITE], STDOUT_FILENO);
//            setvbuf(stdout,NULL,_IONBF,0);
//            setvbuf(stdin,NULL,_IONBF,0);

            close(handles[PIPE_READ]);
            close(handles[PIPE_WRITE]);

            execl("/bin/sh", "sh", "-c", command.data(), (char *)0);

            _exit(127);
        } else {
//            close(handles[PIPE_READ]);
//            close(handles[PIPE_WRITE]);

//            close(output[1]);
//            int nbytes = read(output[0], foo, sizeof(foo));
//            printf("Output: (%.*s)\n", nbytes, foo);
//            wait(NULL);
            FOutputHandle = handles[PIPE_READ];
            FInputHandle = handles[PIPE_WRITE];

            if (AWait == true) {
                ReadOutput();
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

    int status = 0;
    pid_t wpid = 0;

#ifdef LINUX
    wait(&status);
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

void PosixProcess::SetInput(TString Value) {
    if (FInputHandle != 0) {
        write(FInputHandle, Value.data(), Value.size());
    }
}

std::list<TString> PosixProcess::GetOutput() {
    ReadOutput();
    return Process::GetOutput();
}

#endif //POSIX
