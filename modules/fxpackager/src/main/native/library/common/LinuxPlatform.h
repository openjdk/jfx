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

#ifdef LINUX

#ifndef LINUXPLATFORM_H
#define LINUXPLATFORM_H

#include "PosixPlatform.h"
#include "GenericPlatform.h"
#include "JavaUserPreferences.h"

#include <pthread.h>


#pragma warning( push )
#pragma warning( disable : 4250 ) // C4250 - 'class1' : inherits 'class2::member'
class LinuxPlatform : virtual public Platform, GenericPlatform, PosixPlatform
{
private:
    pthread_t FMainThread;

public:
    LinuxPlatform(void);
    virtual ~LinuxPlatform(void);

    virtual void ShowError(TString title, TString description);
    virtual void ShowError(TString description);
    
    virtual TCHAR* ConvertStringToFileSystemString(TCHAR* Source, bool &release);
    virtual TCHAR* ConvertFileSystemStringToString(TCHAR* Source, bool &release);

    virtual void SetCurrentDirectory(TString Value);
    virtual TString GetPackageRootDirectory();
    virtual TString GetAppDataDirectory();

    virtual TString GetModuleFileName();

    virtual TString GetBundledJVMLibraryFileName(TString RuntimePath);
    virtual TString GetSystemJVMLibraryFileName();
    virtual TString GetSystemJRE();

    virtual PropertyContainer* GetConfigFile(TString FileName);

    virtual bool IsMainThread();
    virtual TPlatformNumber GetMemorySize();

#ifdef DEBUG
    virtual bool IsNativeDebuggerPresent();
    virtual int GetProcessID();
#endif //DEBUG
};
#pragma warning( pop ) // C4250


class LinuxJavaUserPreferences : public JavaUserPreferences {
private:
    TString GetUserPrefFileName(TString Appid);

public:
    LinuxJavaUserPreferences(void);
    ~LinuxJavaUserPreferences(void);

    virtual bool Load(TString Appid);
};

#endif //LINUXPLATFORM_H

#endif //LINUX
