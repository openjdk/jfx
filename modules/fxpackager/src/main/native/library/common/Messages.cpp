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


#include "Messages.h"
#include "Platform.h"
#include "Lock.h"
#include "FilePath.h"
#include "Helpers.h"
#include "Macros.h"


Messages::Messages(void) {
    FMessages.SetReadOnly(false);
    FMessages.SetValue(LIBRARY_NOT_FOUND, _T("Failed to find library"));
    FMessages.SetValue(FAILED_CREATING_JVM, _T("Failed to create JVM"));
    FMessages.SetValue(FAILED_LOCATING_JVM_ENTRY_POINT, _T("Failed to locate JNI_CreateJavaVM"));
    FMessages.SetValue(NO_MAIN_CLASS_SPECIFIED, _T("No main class specified"));
    FMessages.SetValue(METHOD_NOT_FOUND, _T("No method %s in class %s."));
    FMessages.SetValue(CLASS_NOT_FOUND, _T("Class %s not found."));
    FMessages.SetValue(ERROR_INVOKING_METHOD, _T("Error invoking method."));
    //FMessages.SetValue(CONFIG_FILE_NOT_FOUND, _T("Configuration file %s is not found."));
    //FMessages.SetValue(BUNDLED_JVM_NOT_FOUND, _T("$JAVAVMLIBRARYNAME is not found in the bundled runtime."));
}

Messages& Messages::GetInstance() {
    Lock lock;
    static Messages instance; // Guaranteed to be destroyed. Instantiated on first use.
    return instance;
}

Messages::~Messages(void) {
}

TString Messages::GetMessage(const TString Key) {
    TString result;
    FMessages.GetValue(Key, result);
    Macros& macros = Macros::GetInstance();
    result = macros.ExpandMacros(result);
    return result;
}
