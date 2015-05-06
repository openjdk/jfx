/*
 * Copyright (c) 2015, Oracle and/or its affiliates.
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

#include "IniFile.h"
#include "Helpers.h"

#include <string>


IniFile::IniFile() : ISectionalPropertyContainer() {
}

IniFile::~IniFile() {
    for (OrderedMap<TString, IniSectionData*>::iterator iterator = FMap.begin(); iterator != FMap.end(); iterator++) {
        pair<TString, IniSectionData*> *item = *iterator;
        delete item->second;
    }
}

bool IniFile::LoadFromFile(const TString FileName) {
    bool result = false;
    Platform& platform = Platform::GetInstance();

    std::list<TString> contents = platform.LoadFromFile(FileName);

    if (contents.empty() == false) {
        bool found = false;

        // Determine the if file is an INI file or property file. Assign FDefaultSection if it is
        // an INI file. Otherwise FDefaultSection is NULL.
        for (std::list<TString>::const_iterator iterator = contents.begin(); iterator != contents.end(); iterator++) {
            TString line = *iterator;

            if (line[0] == ';') {
                // Semicolon is a comment so ignore the line.
                continue;
            }
            else {
                if (line[0] == '[') {
                    found = true;
                }

                break;
            }
        }

        if (found == true) {
            TString sectionName;

            for (std::list<TString>::const_iterator iterator = contents.begin(); iterator != contents.end(); iterator++) {
                TString line = *iterator;

                if (line[0] == ';') {
                    // Semicolon is a comment so ignore the line.
                    continue;
                }
                else if (line[0] == '[' && line[line.length() - 1] == ']') {
                    sectionName = line.substr(1, line.size() - 2);
                }
                else if (sectionName.empty() == false) {
                    TString name;
                    TString value;

                    if (Helpers::SplitOptionIntoNameValue(line, name, value) == true) {
                        Append(sectionName, name, value);
                    }
                }
            }

            result = true;
        }
    }

    return result;
}

bool IniFile::SaveToFile(const TString FileName, bool ownerOnly) {
    bool result = false;

        std::list<TString> contents;
        std::vector<TString> keys = FMap.GetKeys();

        for (unsigned int index = 0; index < keys.size(); index++) {
            TString name = keys[index];
        IniSectionData *section;

                if (FMap.GetValue(name, section) == true) {
                    contents.push_back(_T("[") + name + _T("]"));
                    std::list<TString> lines = section->GetLines();
                    contents.insert(contents.end(), lines.begin(), lines.end());
                    contents.push_back(_T(""));
                }
        }

        Platform& platform = Platform::GetInstance();
        platform.SaveToFile(FileName, contents, ownerOnly);
        result = true;
    return result;
}

void IniFile::Append(const TString SectionName, const TString Key, TString Value) {
    if (FMap.ContainsKey(SectionName) == true) {
        IniSectionData* section;

        if (FMap.GetValue(SectionName, section) == true && section != NULL) {
            section->SetValue(Key, Value);
        }
    }
    else {
        IniSectionData *section = new IniSectionData();
        section->SetValue(Key, Value);
        FMap.Append(SectionName, section);
    }
}

void IniFile::AppendSection(const TString SectionName, OrderedMap<TString, TString> Values) {
    if (FMap.ContainsKey(SectionName) == true) {
        IniSectionData* section;

        if (FMap.GetValue(SectionName, section) == true && section != NULL) {
            section->Append(Values);
        }
    }
    else {
        IniSectionData *section = new IniSectionData(Values);
        FMap.Append(SectionName, section);
    }
}

bool IniFile::GetValue(const TString SectionName, const TString Key, TString& Value) {
    bool result = false;
    IniSectionData* section;

    if (FMap.GetValue(SectionName, section) == true && section != NULL) {
        result = section->GetValue(Key, Value);
    }

    return result;
}

bool IniFile::SetValue(const TString SectionName, const TString Key, TString Value) {
    bool result = false;
    IniSectionData* section;

    if (FMap.GetValue(SectionName, section) && section != NULL) {
        result = section->SetValue(Key, Value);
    }
    else {
        Append(SectionName, Key, Value);
    }


    return result;
}

bool IniFile::GetSection(const TString SectionName, OrderedMap<TString, TString> &Data) {
    bool result = false;

    if (FMap.ContainsKey(SectionName) == true) {
        IniSectionData* section;

        if (FMap.GetValue(SectionName, section) == true && section != NULL) {
            OrderedMap<TString, TString> data = section->GetData();
            Data.Append(data);
            result = true;
        }
    }

    return result;
}

bool IniFile::ContainsSection(const TString SectionName) {
    return FMap.ContainsKey(SectionName);
}

//--------------------------------------------------------------------------------------------------

IniSectionData::IniSectionData() {
    FMap.SetAllowDuplicates(true);
}

IniSectionData::IniSectionData(OrderedMap<TString, TString> Values) {
    FMap = Values;
}

std::vector<TString> IniSectionData::GetKeys() {
    return FMap.GetKeys();
}

std::list<TString> IniSectionData::GetLines() {
    std::list<TString> result;
    std::vector<TString> keys = FMap.GetKeys();

    for (unsigned int index = 0; index < keys.size(); index++) {
        TString name = keys[index];
        TString value;

        if (FMap.GetValue(name, value) == true) {
            TString line = name + _T('=') + value;
            result.push_back(line);
        }
    }

    return result;
}

OrderedMap<TString, TString> IniSectionData::GetData() {
    OrderedMap<TString, TString> result = FMap;
    return result;
}

bool IniSectionData::GetValue(const TString Key, TString& Value) {
    return FMap.GetValue(Key, Value);
}

bool IniSectionData::SetValue(const TString Key, TString Value) {
    return FMap.SetValue(Key, Value);
}

void IniSectionData::Append(OrderedMap<TString, TString> Values) {
    FMap.Append(Values);
}

size_t IniSectionData::GetCount() {
    return FMap.Count();
}
