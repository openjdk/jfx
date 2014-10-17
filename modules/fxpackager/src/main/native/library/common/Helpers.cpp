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


#include "Helpers.h"
#include "PlatformString.h"


bool Helpers::SplitOptionIntoNameValue(TString option, TString& Name, TString& Value) {
    bool result = false;
    size_t position = option.find('=');

    if (position != TString::npos) {
        Name = option.substr(0, position);
        Value = option.substr(position + 1, option.length() - position + 1);
        result = true;
    }
    else {
        Name = option;
    }

    return result;
}

TString Helpers::ReplaceString(TString subject, const TString& search,
                            const TString& replace) {
    size_t pos = 0;
    while((pos = subject.find(search, pos)) != TString::npos) {
            subject.replace(pos, search.length(), replace);
            pos += replace.length();
    }
    return subject;
}

TString Helpers::ConvertIdToFilePath(TString Value) {
    TString search;
    search = '/';
    TString replace;
    replace = '\\';
    TString result = ReplaceString(Value, search, replace);
    return result;
}

TString Helpers::ConvertIdToJavaPath(TString Value) {
    TString search;
    search = '.';
    TString replace;
    replace = '/';
    TString result = ReplaceString(Value, search, replace);
    search = '\\';
    result = ReplaceString(result, search, replace);
    return result;
}

TString Helpers::ConvertPathToId(TString Value) {
    TString search;
    search = '/';
    TString replace;
    replace = '.';
    TString result = ReplaceString(Value, search, replace);
    return result;
}

std::map<TString, TValueIndex> Helpers::GetJVMArgsFromConfig(PropertyFile* config) {
    std::map<TString, TValueIndex> result;

    for (unsigned int index = 0; index < config->GetCount(); index++) {
        TString argname = TString(_T("jvmarg.")) + PlatformString(index + 1).toString();
        TString argvalue;

        if (config->GetValue(argname, argvalue) == false) {
            break;
        }
        else if (argvalue.empty() == false) {
            TString name;
            TValueIndex value;
            Helpers::SplitOptionIntoNameValue(argvalue, name, value.value);
            result.insert(std::map<TString, TValueIndex>::value_type(name, value));
        }
    }

    return result;
}

std::map<TString, TValueIndex> Helpers::GetJVMUserArgsFromConfig(PropertyFile* config) {
    std::map<TString, TValueIndex> result;

    for (unsigned int index = 0; index < config->GetCount(); index++) {
        TString prefix = TString(_T("jvmuserarg.")) + PlatformString(index + 1).toString();
        TString argname = prefix + _T(".name");
        TString argvalue = prefix + _T(".value");
        TString name;
        TValueIndex value;

        if ((config->GetValue(argname, name) == false) || (config->GetValue(argvalue, value.value) == false)) {
            break;
        }
        else if ((name.empty() == false) && (value.value.empty() == false)) {
            result.insert(std::map<TString, TValueIndex>::value_type(name, value));
        }
    }

    return result;
}

std::map<TString, TString> Helpers::GetConfigFromJVMUserArgs(std::map<TString, TValueIndex> OrderedMap) {
    std::map<TString, TString> result;
    
    for (std::map<TString, TValueIndex>::iterator iterator = OrderedMap.begin();
         iterator != OrderedMap.end();
         iterator++) {
        size_t index = iterator->second.index;
        TString prefix = TString(_T("jvmuserarg.")) + PlatformString(index + 1).toString();
        TString argname = prefix + _T(".name");
        TString argvalue = prefix + _T(".value");
        TString name = iterator->first;
        TString value = iterator->second.value;
        
        result.insert(std::map<TString, TString>::value_type(argname, name));
        result.insert(std::map<TString, TString>::value_type(argvalue, value));
    }
    
    return result;
}

std::list<TString> Helpers::GetArgsFromConfig(PropertyFile* config) {
    std::list<TString> result;

    for (unsigned int index = 0; index < config->GetCount(); index++) {
        TString argname = TString(_T("arg.")) + PlatformString(index + 1).toString();
        TString argvalue;

        if (config->GetValue(argname, argvalue) == false) {
            break;
        }
        else if (argvalue.empty() == false) {
            result.push_back((argvalue));
        }
    }

    return result;
}

bool comp(const TValueIndex& a, const TValueIndex& b) {
    return a.index < b.index;
}

std::list<TString> Helpers::GetOrderedKeysFromMap(std::map<TString, TValueIndex> OrderedMap) {
    std::list<TString> result;
    std::list<TValueIndex> indexedList;
    
    for (std::map<TString, TValueIndex>::iterator iterator = OrderedMap.begin();
         iterator != OrderedMap.end();
         iterator++) {
        TValueIndex item;
        item.value = iterator->first;
        item.index = iterator->second.index;
        indexedList.push_back(item);
    }
    
    indexedList.sort(comp);
    
    for (std::list<TValueIndex>::const_iterator iterator = indexedList.begin(); iterator != indexedList.end(); iterator++) {
        TString name = iterator->value;
        result.push_back(name);
    }
    
    return result;
}

TString Helpers::NameValueToString(TString name, TString value) {
    TString result;
    
    if (value.empty() == true) {
        result = name;
    }
    else {
        result = name + TString(_T("=")) + value;
    }
    
    return result;
}
