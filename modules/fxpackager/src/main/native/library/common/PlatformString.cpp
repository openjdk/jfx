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


#include "PlatformString.h"

#include "Java.h"
#include "Helpers.h"

#include <stdio.h>
#include <stdlib.h>
#include <stdlib.h>
#include <memory.h>
#include <sstream>
#include <string.h>

#include "jni.h"


void PlatformString::initialize() {
    FWideTStringToFree = NULL;
    FLength = 0;
    FData = NULL;
}

void PlatformString::CopyString(char *Destination, size_t NumberOfElements, const char *Source) {
#ifdef WINDOWS
    strcpy_s(Destination, NumberOfElements, Source);
#endif //WINDOWS
#ifdef POSIX
    strncpy(Destination, Source, NumberOfElements);
#endif //POSIX
    Destination[NumberOfElements - 1] = '\0';
}

PlatformString::PlatformString(void) {
    initialize();
}

PlatformString::~PlatformString(void) {
    if (FData != NULL) {
        delete[] FData;
    }

    if (FWideTStringToFree != NULL) {
        delete[] FWideTStringToFree;
    }
}

// Owner must free the return value.
MultibyteString PlatformString::WideStringToMultibyteString(const wchar_t* value) {
    MultibyteString result;
    size_t count = 0;

#ifdef WINDOWS
    wcstombs_s(&count, NULL, 0, value, 0);

    if (count > 0) {
        result.data = new char[count + 1];
        wcstombs_s(&result.length, result.data, count, value, count);

#endif //WINDOWS
#ifdef POSIX
    count = wcstombs(NULL, value, 0);

    if (count > 0) {
        result.data = new char[count + 1];
        result.data[count] = '\0';
        result.length = count;
        wcstombs(result.data, value, count);
#endif //POSIX
    }

    return result;
}

// Owner must free the return value.
WideString PlatformString::MultibyteStringToWideString(const char* value) {
    WideString result;
    size_t count = 0;

#ifdef WINDOWS
    mbstowcs_s(&count, NULL, 0, value, _TRUNCATE);

    if (count > 0) {
        result.data = new wchar_t[count + 1];
        mbstowcs_s(&result.length, result.data, count, value, count);
#endif // WINDOWS
#ifdef POSIX
    count = mbstowcs(NULL, value, 0);

    if (count > 0) {
        result.data = new wchar_t[count + 1];
        result.data[count] = '\0';
        result.length = count;
        mbstowcs(result.data, value, count);
#endif //POSIX
    }

    return result;
}

PlatformString::PlatformString(const PlatformString &value) {
    initialize();
    FLength = value.FLength;
    FData = new char[FLength + 1];
    PlatformString::CopyString(FData, FLength + 1, value.FData);
}

PlatformString::PlatformString(const char* value) {
    initialize();
    FLength = strlen(value);
    FData = new char[FLength + 1];
    PlatformString::CopyString(FData, FLength + 1, value);
}

PlatformString::PlatformString(size_t Value) {
    initialize();
    
    std::stringstream ss;
    std::string s;
    ss << Value;
    s = ss.str();
    
    FLength = strlen(s.c_str());
    FData = new char[FLength + 1];
    PlatformString::CopyString(FData, FLength + 1, s.c_str());
}
    
PlatformString::PlatformString(const wchar_t* value) {
    initialize();
    MultibyteString temp = WideStringToMultibyteString(value);
    FLength = temp.length;
    FData = temp.data;
}

PlatformString::PlatformString(const std::string &value) {
    initialize();
    const char* lvalue = value.data();
    FLength = value.size();
    FData = new char[FLength + 1];
    PlatformString::CopyString(FData, FLength + 1, lvalue);
}

PlatformString::PlatformString(const std::wstring &value) {
    initialize();
    const wchar_t* lvalue = value.data();
    MultibyteString temp = WideStringToMultibyteString(lvalue);
    FLength = temp.length;
    FData = temp.data;
}

PlatformString::PlatformString(JNIEnv *env, jstring value) {
    initialize();

    if (env != NULL) {
        const char* lvalue = env->GetStringUTFChars(value, JNI_FALSE);

        if (lvalue == NULL || env->ExceptionCheck() == JNI_TRUE) {
            throw JavaException();
        }

        if (lvalue != NULL) {
            FLength = env->GetStringUTFLength(value);

            if (env->ExceptionCheck() == JNI_TRUE) {
                throw JavaException();
            }

            FData = new char[FLength + 1];
            PlatformString::CopyString(FData, FLength + 1, lvalue);

            env->ReleaseStringUTFChars(value, lvalue);

            if (env->ExceptionCheck() == JNI_TRUE) {
                throw JavaException();
            }
        }
    }
}

std::string PlatformString::Format(std::string value, ...) {
    std::string result = value;

    va_list arglist;
    va_start(arglist, value);

    while (1) {
        size_t pos = result.find("%s", 0);

        if (pos == TString::npos) {
            break;
        }
        else {
            char* arg = va_arg(arglist, char*);

            if (arg == NULL) {
                break;
            }
            else {
                result.replace(pos, strlen("%s"), arg);
            }
        }
    }

    va_end(arglist);

    return result;
}

size_t PlatformString::length() {
    return FLength;
}

char* PlatformString::c_str() {
    return FData;
}

char* PlatformString::toMultibyte() {
    return FData;
}

wchar_t* PlatformString::toWideString() {
    WideString result = MultibyteStringToWideString(FData);

    if (result.data != NULL) {
        if (FWideTStringToFree != NULL) {
            delete [] FWideTStringToFree;
        }

        FWideTStringToFree = result.data;
    }

    return result.data;
}

std::wstring PlatformString::toUnicodeString() {
    std::wstring result;
    wchar_t* data = toWideString();

    if (FLength != 0 && data != NULL) {
        // NOTE: Cleanup of result is handled by PlatformString destructor.
        result = data;
    }

    return result;
}

std::string PlatformString::toStdString() {
    std::string result;
    char* data = toMultibyte();

    if (FLength > 0 && data != NULL) {
        result = data;
    }

    return result;
}

jstring PlatformString::toJString(JNIEnv *env) {
    jstring result = NULL;

    if (env != NULL) {
        result = env->NewStringUTF(c_str());

        if (result == NULL || env->ExceptionCheck() == JNI_TRUE) {
            throw JavaException();
        }
    }

    return result;
}

TCHAR* PlatformString::toPlatformString() {
#ifdef _UNICODE
    return toWideString();
#else
    return c_str();
#endif //_UNICODE
}

TString PlatformString::toString() {
#ifdef _UNICODE
    return toUnicodeString();
#else
    return toStdString();
#endif //_UNICODE
}

PlatformString::operator char* () {
    return c_str();
}

PlatformString::operator wchar_t* () {
    return toWideString();
}

PlatformString::operator std::wstring () {
    return toUnicodeString();
}

char* PlatformString::duplicate(const char* Value) {
    size_t length = strlen(Value);
    char* result = new char[length + 1];
    PlatformString::CopyString(result, length + 1, Value);
    return result;
}
