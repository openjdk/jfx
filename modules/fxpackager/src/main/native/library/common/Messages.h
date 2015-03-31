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


#ifndef MESSAGES_H
#define MESSAGES_H

#include "PropertyFile.h"

#define LIBRARY_NOT_FOUND _T("library.not.found")
#define FAILED_CREATING_JVM _T("failed.creating.jvm")
#define FAILED_LOCATING_JVM_ENTRY_POINT _T("failed.locating.jvm.entry.point")
#define NO_MAIN_CLASS_SPECIFIED _T("no.main.class.specified")

#define METHOD_NOT_FOUND _T("method.not.found")
#define CLASS_NOT_FOUND _T("class.not.found")
#define ERROR_INVOKING_METHOD _T("error.invoking.method")

#define CONFIG_FILE_NOT_FOUND _T("config.file.not.found")

#define BUNDLED_JVM_NOT_FOUND _T("bundled.jvm.not.found")

#define APPCDS_CACHE_FILE_NOT_FOUND _T("appcds.cache.file.not.found")

class Messages {
private:
    PropertyFile FMessages;

    Messages(void);
public:
    static Messages& GetInstance();
    ~Messages(void);

    TString GetMessage(const TString Key);
};

#endif //MESSAGES_H
