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


#ifndef PLATFORMSTRING_H
#define PLATFORMSTRING_H


#include <string>
#include <list>
#include <stdio.h>
#include <stdlib.h>

#include "jni.h"
#include "Platform.h"


struct WideString {
    size_t length;
    wchar_t* data;

    WideString() { length = 0; data = NULL; }
};

struct MultibyteString {
    size_t length;
    char* data;

    MultibyteString() { length = 0; data = NULL; }
};


template <typename T>
class DynamicBuffer {
private:
    T* FData;
    size_t FSize;

public:
    DynamicBuffer(size_t Size) {
        FSize = 0;
        FData = NULL;
        Resize(Size);
    }

    ~DynamicBuffer() {
        delete[] FData;
    }

    T* GetData() { return FData; }
    size_t GetSize() { return FSize; }

    void Resize(size_t Size) {
        FSize = Size;

        if (FData != NULL) {
            delete[] FData;
            FData = NULL;
        }

        if (FSize != 0) {
            FData = new T[Size];
        }
    }

    T& operator[](size_t index) {
        return FData[index];
    }
};


class PlatformString {
private:
    char* FData; // Stored as UTF-8
    size_t FLength;
    wchar_t* FWideTStringToFree;

    void initialize();

    static void CopyTString(char *Destination, size_t NumberOfElements, const char *Source);
    static WideString MultibyteStringToWideString(const char* value);
    static MultibyteString WideStringToMultibyteString(const wchar_t* value);

// Prohibit Heap-Based PlatformStrings
private:
    static void *operator new(size_t size);
    static void operator delete(void *ptr);

public:
    PlatformString(void);
    PlatformString(const PlatformString &value);
    PlatformString(const char* value);
    PlatformString(const wchar_t* value);
    PlatformString(const std::string &value);
    PlatformString(const std::wstring &value);
    PlatformString(JNIEnv *env, jstring value);
    PlatformString(size_t Value);

    static std::string Format(std::string value, ...);

    ~PlatformString(void);

    size_t length();

    char* c_str();
    char* toMultibyte();
    wchar_t* toWideString();
    std::wstring toUnicodeString();
    std::string toStdString();
    jstring toJString(JNIEnv *env);
    TCHAR* toPlatformString();
    TString toString();

    operator char* ();
    operator wchar_t* ();
    operator std::wstring ();

    // Caller must free using delete[]
    static char* duplicate(const char* Value);
};

#endif //PLATFORMSTRING_H
