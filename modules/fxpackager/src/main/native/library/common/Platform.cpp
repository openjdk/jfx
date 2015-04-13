/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates.
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


#include "Platform.h"
#include "Lock.h"
#include "Messages.h"

#include "WindowsPlatform.h"
#include "LinuxPlatform.h"
#include "MacPlatform.h"


//--------------------------------------------------------------------------------------------------

Platform& Platform::GetInstance() {
    //Lock lock(true);
#ifdef WINDOWS
    static WindowsPlatform instance;
#endif // WINDOWS
#ifdef LINUX
    static LinuxPlatform instance;
#endif // LINUX
#ifdef MAC
    static MacPlatform instance;
#endif // MAC
    return instance;
}

//--------------------------------------------------------------------------------------------------


Library::Library() {
    Initialize();
}

Library::Library(const TString &FileName) {
    Initialize();
    Load(FileName);
}

Library::~Library() {
    Unload();
}

void Library::Initialize() {
    FModule = NULL;
    FDependentLibraryNames = NULL;
    FDependenciesLibraries = NULL;
}

void Library::InitializeDependencies() {
    if (FDependentLibraryNames == NULL) {
        FDependentLibraryNames = new std::vector<TString>();
    }

    if (FDependenciesLibraries == NULL) {
        FDependenciesLibraries = new std::vector<Library*>();
    }
}

void Library::LoadDependencies() {
    if (FDependentLibraryNames != NULL && FDependenciesLibraries != NULL) {
        for (std::vector<TString>::const_iterator iterator = FDependentLibraryNames->begin();
                iterator != FDependentLibraryNames->end(); iterator++) {
            Library* library = new Library();

            if (library->Load(*iterator) == true) {
                FDependenciesLibraries->push_back(library);
            }
        }

        delete FDependentLibraryNames;
        FDependentLibraryNames = NULL;
    }
}

void Library::UnloadDependencies() {
    if (FDependenciesLibraries != NULL) {
        for (std::vector<Library*>::const_iterator iterator = FDependenciesLibraries->begin();
                iterator != FDependenciesLibraries->end(); iterator++) {
            Library* library = *iterator;

            if (library != NULL) {
                library->Unload();
                delete library;
            }
        }

        delete FDependenciesLibraries;
        FDependenciesLibraries = NULL;
    }
}

Procedure Library::GetProcAddress(std::string MethodName) {
    Platform& platform = Platform::GetInstance();
    return platform.GetProcAddress(FModule, MethodName);
}

bool Library::Load(const TString &FileName) {
    bool result = true;

    if (FModule == NULL) {
        LoadDependencies();
        Platform& platform = Platform::GetInstance();
        FModule = platform.LoadLibrary(FileName);

        if (FModule == NULL) {
            Messages& messages = Messages::GetInstance();
            platform.ShowMessage(messages.GetMessage(LIBRARY_NOT_FOUND), FileName);
            result = false;
        }
    }

    return result;
}

bool Library::Unload() {
    bool result = false;

    if (FModule != NULL) {
        Platform& platform = Platform::GetInstance();
        platform.FreeLibrary(FModule);
        FModule = NULL;
        UnloadDependencies();
        result = true;
    }

    return result;
}

void Library::AddDependency(const TString &FileName) {
    InitializeDependencies();

    if (FDependentLibraryNames != NULL) {
        FDependentLibraryNames->push_back(FileName);
    }
}

void Library::AddDependencies(const std::vector<TString> &Dependencies) {
    if (Dependencies.size() > 0) {
        InitializeDependencies();

        if (FDependentLibraryNames != NULL) {
            for (std::vector<TString>::const_iterator iterator = FDependentLibraryNames->begin();
                iterator != FDependentLibraryNames->end(); iterator++) {
                TString fileName = *iterator;
                AddDependency(fileName);
            }
        }
    }
}
//--------------------------------------------------------------------------------------------------
