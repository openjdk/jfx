/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates.
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


#ifndef JAVAVIRTUALMACHINE_H
#define JAVAVIRTUALMACHINE_H


#include "jni.h"
#include "Platform.h"


enum JvmLaunchType {
    USER_APP_LAUNCH,
    SINGLE_INSTANCE_NOTIFICATION_LAUNCH,
    JVM_LAUNCH_TYPES_NUM
};

struct JavaOptionItem {
    TString name;
    TString value;
    void* extraInfo;
};

class JavaOptions {
private:
    std::list<JavaOptionItem> FItems;
    JavaVMOption* FOptions;

public:
    JavaOptions();
    ~JavaOptions();

    void AppendValue(const TString Key, TString Value, void* Extra);
    void AppendValue(const TString Key, TString Value);
    void AppendValue(const TString Key);
    void AppendValues(OrderedMap<TString, TString> Values);
    void ReplaceValue(const TString Key, TString Value);
    std::list<TString> ToList();
    size_t GetCount();
};

// Private typedef for function pointer casting
#define LAUNCH_FUNC "JLI_Launch"

typedef int (JNICALL *JVM_CREATE)(int argc, char ** argv,
                                  int jargc, const char** jargv,
                                  int appclassc, const char** appclassv,
                                  const char* fullversion,
                                  const char* dotversion,
                                  const char* pname,
                                  const char* lname,
                                  jboolean javaargs,
                                  jboolean cpwildcard,
                                  jboolean javaw,
                                  jint ergo);

class JavaLibrary : public Library {
    JVM_CREATE FCreateProc;
    JavaLibrary(const TString &FileName);
public:
    JavaLibrary();
    bool JavaVMCreate(size_t argc, char *argv[]);
};

class JavaVirtualMachine {
private:
    JavaLibrary javaLibrary;

    void configureLibrary();
    bool launchVM(JavaOptions& options, std::list<TString>& vmargs, bool addSiProcessId);
public:
    JavaVirtualMachine();
    ~JavaVirtualMachine(void);

    bool StartJVM();
    bool NotifySingleInstance();
};

bool RunVM(JvmLaunchType type);

#endif //JAVAVIRTUALMACHINE_H
