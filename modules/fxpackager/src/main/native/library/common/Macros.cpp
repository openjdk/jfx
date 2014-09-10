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


#include "Macros.h"
#include "Package.h"
#include "Helpers.h"


Macros::Macros(void) {
    Platform& platform = Platform::GetInstance();
    Package& package = Package::GetInstance();

    // Public macros.
    FData.insert(std::map<TString, TString>::value_type(_T("$APPDIR"), package.GetPackageRootDirectory()));

    FData.insert(std::map<TString, TString>::value_type(_T("$PACKAGEDIR"), package.GetPackageAppDirectory()));
    
    FData.insert(std::map<TString, TString>::value_type(_T("$LAUNCHERDIR"), package.GetPackageLauncherDirectory()));

    TString javaHome = FilePath::ExtractFilePath(platform.GetJvmPath());
    FData.insert(std::map<TString, TString>::value_type(_T("$JREHOME"), javaHome));

    // Private macros.
    TString javaVMLibraryName = FilePath::ExtractFileName(platform.GetJvmPath());
    FData.insert(std::map<TString, TString>::value_type(_T("$JAVAVMLIBRARYNAME"), javaVMLibraryName));
}

Macros::~Macros(void) {
}

Macros& Macros::GetInstance() {
    static Macros instance;
    return instance;
}

TString Macros::ExpandMacros(TString Value) {
    TString result = Value;

    for (std::map<TString, TString>::iterator iterator = FData.begin();
        iterator != FData.end();
        iterator++) {

        TString name = iterator->first;

        if (Value.find(name) != TString::npos) {
            TString lvalue = iterator->second;
            result = Helpers::ReplaceTString(Value, name, lvalue);
            break;
        }
    }

    return result;
}
