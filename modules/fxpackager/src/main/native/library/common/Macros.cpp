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
}

Macros::~Macros(void) {
}

void Macros::Initialize() {
    Package& package = Package::GetInstance();
    Macros& macros = Macros::GetInstance();

    // Public macros.
    macros.AddMacro(_T("$APPDIR"), package.GetPackageRootDirectory());
    macros.AddMacro(_T("$PACKAGEDIR"), package.GetPackageAppDirectory());
    macros.AddMacro(_T("$LAUNCHERDIR"), package.GetPackageLauncherDirectory());
    macros.AddMacro(_T("$APPDATADIR"), package.GetAppDataDirectory());

    TString javaHome = FilePath::ExtractFilePath(package.GetJVMLibraryFileName());
    macros.AddMacro(_T("$JREHOME"), javaHome);

    // App CDS Macros
    macros.AddMacro(_T("$CACHEDIR"), package.GetAppCDSCacheDirectory());

    // Private macros.
    TString javaVMLibraryName = FilePath::ExtractFileName(javaHome);
    macros.AddMacro(_T("$JAVAVMLIBRARYNAME"), javaVMLibraryName);
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
            result = Helpers::ReplaceString(Value, name, lvalue);
            result = ExpandMacros(result);
            break;
        }
    }

    return result;
}

void Macros::AddMacro(TString Key, TString Value) {
    FData.insert(std::map<TString, TString>::value_type(Key, Value));
}
