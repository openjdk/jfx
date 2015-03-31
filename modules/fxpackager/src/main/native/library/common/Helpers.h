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


#ifndef HELPERS_H
#define HELPERS_H

#include "Platform.h"
#include "OrderedMap.h"
#include "IniFile.h"


class Helpers {
private:
    Helpers(void) {}
    ~Helpers(void) {}

public:
    // Supports two formats for option:
    // Example 1:
    // foo=bar
    //
    // Example 2:
    // <name=foo=, value=goo>
    static bool SplitOptionIntoNameValue(TString option, TString& Name, TString& Value);
    static TString ReplaceString(TString subject, const TString& search,
                                 const TString& replace);
    static TString ConvertIdToFilePath(TString Value);
    static TString ConvertIdToJavaPath(TString Value);
    static TString ConvertPathToId(TString Value);

    static OrderedMap<TString, TString> GetJVMArgsFromConfig(IPropertyContainer* config);
    static OrderedMap<TString, TString> GetJVMUserArgsFromConfig(IPropertyContainer* config);
    //static OrderedMap<TString, TString> GetConfigFromJVMUserArgs(OrderedMap<TString, TString> Value);
    static std::list<TString> GetArgsFromConfig(IPropertyContainer* config);

    static void LoadOldConfigFile(TString FileName, IniFile* Container);
    static void LoadOldUserConfigFile(TString FileName, IniFile* Container);

    static std::list<TString> MapToNameValueList(OrderedMap<TString, TString> Map);

    static TString NameValueToString(TString name, TString value);
};

#endif //HELPERS_H
