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


#include "PropertyFile.h"

#include "Helpers.h"
#include "FilePath.h"

#include <string>


PropertyFile::PropertyFile(void) : PropertyContainer() {
    FReadOnly = false;
    FModified = false;
}

PropertyFile::PropertyFile(const TString FileName) : PropertyContainer() {
    FReadOnly = true;
    FModified = false;
    LoadFromFile(FileName);
}

PropertyFile::PropertyFile(std::map<TString, TString> Value) : PropertyContainer() {
    FData.insert(Value.begin(), Value.end());
}

PropertyFile::~PropertyFile(void) {
}

void PropertyFile::SetModified(bool Value) {
    FModified = Value;
}

bool PropertyFile::IsModified() {
    return FModified;
}

bool PropertyFile::GetReadOnly() {
    return FReadOnly;
}

void PropertyFile::SetReadOnly(bool Value) {
    FReadOnly = Value;
}

void PropertyFile::Assign(std::map<TString, TString> Value) {
    FData.clear();
    FData.insert(Value.begin(), Value.end());
    SetModified(true);
}

bool PropertyFile::LoadFromFile(const TString FileName) {
    bool result = false;
    Platform& platform = Platform::GetInstance();

    std::list<TString> contents = platform.LoadFromFile(FileName);

    if (contents.empty() == false) {
        for (std::list<TString>::const_iterator iterator = contents.begin(); iterator != contents.end(); iterator++) {
            TString line = *iterator;
            TString name;
            TString value;

            if (Helpers::SplitOptionIntoNameValue(line, name, value) == true) {
                FData.insert(std::map<TString, TString>::value_type(name, value));
            }
        }

        SetModified(false);
        result = true;
    }

    return result;
}

bool PropertyFile::SaveToFile(const TString FileName, bool ownerOnly) {
    bool result = false;

    if (GetReadOnly() == false && IsModified()) {
        std::list<TString> contents;

        for (std::map<TString, TString>::iterator iterator = FData.begin();
            iterator != FData.end();
            iterator++) {

            TString name = iterator->first;
            TString value = iterator->second;
            TString line = name + _T('=') + value;
            contents.push_back(line);
        }

        Platform& platform = Platform::GetInstance();
        platform.SaveToFile(FileName, contents, ownerOnly);

        SetModified(false);
        result = true;
    }

    return result;
}

bool PropertyFile::GetValue(const TString Key, TString& Value) {
    bool result = false;
    std::map<TString, TString>::const_iterator iterator = FData.find(Key);

    if (iterator != FData.end()) {
        Value = iterator->second;
        result = true;
    }

    return result;
}

bool PropertyFile::SetValue(const TString Key, TString Value) {
    bool result = false;

    if (GetReadOnly() == false) {
        FData[Key] = Value;
        SetModified(true);
        result = true;
    }

    return result;
}

bool PropertyFile::RemoveKey(const TString Key) {
    bool result = false;

    if (GetReadOnly() == false) {
        std::map<TString, TString>::iterator iterator = FData.find(Key);

        if (iterator != FData.end()) {
            FData.erase(iterator);
            SetModified(true);
            result = true;
        }
    }

    return result;
}

size_t PropertyFile::GetCount() {
    return FData.size();
}

std::map<TString, TString> PropertyFile::GetData() {
    return FData;
}
