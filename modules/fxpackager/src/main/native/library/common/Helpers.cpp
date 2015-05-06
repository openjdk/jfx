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
#include "PropertyFile.h"


bool Helpers::SplitOptionIntoNameValue(TString option, TString& Name, TString& Value) {
    bool result = false;
    Name = _T("");
    Value = _T("");
    unsigned int index = 0;

    for (; index < option.length(); index++) {
        TCHAR c = option[index];

        switch (c) {
            case '=': {
                index++;
                result = true;
                break;
            }

            case '\\': {
                if (index + 1 < option.length()) {
                    c = option[index + 1];

                    switch (c) {
                        case '\\': {
                            index++;
                            Name += '\\';
                            break;
                        }

                        case '=': {
                            index++;
                            Name += '=';
                            break;
                        }
                    }

                }

                continue;
            }

            default: {
                Name += c;
                continue;
            }
        }

        break;
    }

    if (result) {
        Value = option.substr(index, index - option.length());
    }

    return true;
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
    search = '.';
    TString replace;
    replace = '/';
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
    search = TRAILING_PATHSEPARATOR;
    TString replace;
    replace = '.';
    TString result = ReplaceString(Value, search, replace);
    return result;
}

OrderedMap<TString, TString> Helpers::GetJVMArgsFromConfig(IPropertyContainer* config) {
    OrderedMap<TString, TString> result;

    for (unsigned int index = 0; index < config->GetCount(); index++) {
        TString argname = TString(_T("jvmarg.")) + PlatformString(index + 1).toString();
        TString argvalue;

        if (config->GetValue(argname, argvalue) == false) {
            break;
        }
        else if (argvalue.empty() == false) {
            TString name;
            TString value;
            Helpers::SplitOptionIntoNameValue(argvalue, name, value);
            result.Append(name, value);
        }
    }

    return result;
}

OrderedMap<TString, TString> Helpers::GetJVMUserArgsFromConfig(IPropertyContainer* config) {
    OrderedMap<TString, TString> result;

    for (unsigned int index = 0; index < config->GetCount(); index++) {
        TString prefix = TString(_T("jvmuserarg.")) + PlatformString(index + 1).toString();
        TString argname = prefix + _T(".name");
        TString argvalue = prefix + _T(".value");
        TString name;
        TString value;

        if ((config->GetValue(argname, name) == false) || (config->GetValue(argvalue, value) == false)) {
            break;
        }
        else if ((name.empty() == false) && (value.empty() == false)) {
            result.Append(name, value);
        }
    }

    return result;
}

std::list<TString> Helpers::GetArgsFromConfig(IPropertyContainer* config) {
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

void AppendToIni(PropertyFile &Source, IniFile* Destination, TString Key) {
    TString value;

    if (Source.GetValue(Key, value) == true) {
        Platform& platform = Platform::GetInstance();
        std::map<TString, TString> keys = platform.GetKeys();
        Destination->Append(keys[CONFIG_SECTION_APPLICATION], Key, value);
    }
}

void Helpers::LoadOldConfigFile(TString FileName, IniFile* Container) {
    PropertyFile propertyFile;

    if (propertyFile.LoadFromFile(FileName) == true) {
        Platform& platform = Platform::GetInstance();

        std::map<TString, TString> keys = platform.GetKeys();

        // Application Section
        AppendToIni(propertyFile, Container, keys[CONFIG_MAINJAR_KEY]);
        AppendToIni(propertyFile, Container, keys[CONFIG_MAINCLASSNAME_KEY]);
        AppendToIni(propertyFile, Container, keys[CONFIG_CLASSPATH_KEY]);
        AppendToIni(propertyFile, Container, keys[APP_NAME_KEY]);
        AppendToIni(propertyFile, Container, keys[CONFIG_APP_ID_KEY]);
        AppendToIni(propertyFile, Container, keys[JVM_RUNTIME_KEY]);
        AppendToIni(propertyFile, Container, keys[PACKAGER_APP_DATA_DIR]);

        AppendToIni(propertyFile, Container, keys[CONFIG_APP_MEMORY]);
        AppendToIni(propertyFile, Container, keys[CONFIG_SPLASH_KEY]);

        // JVMOptions Section
        OrderedMap<TString, TString> JVMArgs = Helpers::GetJVMArgsFromConfig(&propertyFile);
        Container->AppendSection(keys[CONFIG_SECTION_JVMOPTIONS], JVMArgs);

        // JVMUserOptions Section
        OrderedMap<TString, TString> defaultJVMUserArgs = Helpers::GetJVMUserArgsFromConfig(&propertyFile);
        Container->AppendSection(keys[CONFIG_SECTION_JVMUSEROPTIONS], defaultJVMUserArgs);

        // ArgOptions Section
        std::list<TString> args = Helpers::GetArgsFromConfig(&propertyFile);
        OrderedMap<TString, TString> convertedArgs;

        for (std::list<TString>::iterator iterator = args.begin(); iterator != args.end(); iterator++) {
            TString arg = *iterator;
            TString name;
            TString value;

            if (Helpers::SplitOptionIntoNameValue(arg, name, value) == true) {
                convertedArgs.Append(name, value);
            }
        }

        Container->AppendSection(keys[CONFIG_SECTION_ARGOPTIONS], convertedArgs);
    }
}

void Helpers::LoadOldUserConfigFile(TString FileName, IniFile* Container) {
    PropertyFile propertyFile;
    Container = NULL;

    if (propertyFile.LoadFromFile(FileName) == true) {
        Container = new IniFile();
        Platform& platform = Platform::GetInstance();

        std::map<TString, TString> keys = platform.GetKeys();

        // JVMUserOverridesOptions Section
        OrderedMap<TString, TString> defaultJVMUserArgs = Helpers::GetJVMUserArgsFromConfig(&propertyFile);
        Container->AppendSection(keys[CONFIG_SECTION_JVMUSEROVERRIDESOPTIONS], defaultJVMUserArgs);
    }
}

std::list<TString> Helpers::MapToNameValueList(OrderedMap<TString, TString> Map) {
    std::list<TString> result;
    std::vector<TString> keys = Map.GetKeys();

    for (OrderedMap<TString, TString>::const_iterator iterator = Map.begin(); iterator != Map.end(); iterator++) {
       pair<TString, TString> *item = *iterator;
       TString key = item->first;
       TString value = item->second;
       
       if (value.length() == 0) {
           result.push_back(key);
       } else {
           result.push_back(key + _T('=') + value);
        }
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
