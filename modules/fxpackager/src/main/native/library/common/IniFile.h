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

#ifndef INIFILE_H
#define INIFILE_H

#include "Platform.h"
#include "OrderedMap.h"

#include <map>


class IniSectionData : public IPropertyContainer {
private:
    OrderedMap<TString, TString> FMap;

public:
    IniSectionData();
    IniSectionData(OrderedMap<TString, TString> Values);

    std::vector<TString> GetKeys();
    std::list<TString> GetLines();
    OrderedMap<TString, TString> GetData();

    bool SetValue(const TString Key, TString Value);
    void Append(OrderedMap<TString, TString> Values);

    virtual bool GetValue(const TString Key, TString& Value);
    virtual size_t GetCount();
};


class IniFile : public ISectionalPropertyContainer {
private:
    OrderedMap<TString, IniSectionData*> FMap;

public:
    IniFile();
    virtual ~IniFile();

    void internalTest();

    bool LoadFromFile(const TString FileName);
    bool SaveToFile(const TString FileName, bool ownerOnly = true);

    void Append(const TString SectionName, const TString Key, TString Value);
    void AppendSection(const TString SectionName, OrderedMap<TString, TString> Values);
    bool SetValue(const TString SectionName, const TString Key, TString Value);

    // ISectionalPropertyContainer
    virtual bool GetSection(const TString SectionName, OrderedMap<TString, TString> &Data);
    virtual bool ContainsSection(const TString SectionName);
    virtual bool GetValue(const TString SectionName, const TString Key, TString& Value);
};

#endif //INIFILE_H
