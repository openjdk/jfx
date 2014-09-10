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


#ifndef JAVA_H
#define JAVA_H

#include "Platform.h"
#include "Messages.h"

#include "jni.h"


class JavaClass;
class JavaStaticMethod;
class JavaMethod;
class JavaStringArray;


class JavaException : public std::exception {
// Prohibit Heap-Based Classes.
private:
    static void *operator new(size_t size);

private:
#ifdef DEBUG
    static std::string CreateExceptionMessage(JNIEnv* Env, jthrowable Exception,
        jmethodID GetCauseMethod, jmethodID GetStackTraceMethod, jmethodID ThrowableToStringMethod,
        jmethodID FrameToStringMethod);
#endif //DEBUG

    std::string FMessage;
    jthrowable FException;
    JNIEnv *FEnv;

public:
    explicit JavaException();
    explicit JavaException(const char* const message);
    explicit JavaException(JNIEnv *Env, const char* const message);
    virtual ~JavaException() throw() {}

    virtual const char* what();
    void Rethrow();
};


class JavaStaticMethod {
// Prohibit Heap-Based Classes.
private:
    static void *operator new(size_t size);
    static void operator delete(void *ptr);

private:
    JNIEnv *FEnv;
    jmethodID FMethod;
    jclass FClass;
public:
    JavaStaticMethod(JNIEnv *Env, jclass Class, jmethodID Method);

    void CallVoidMethod(int Count, ...);
    operator jmethodID ();
};


class JavaMethod {
// Prohibit Heap-Based Classes.
private:
    static void *operator new(size_t size);
    static void operator delete(void *ptr);

    JavaMethod(JavaMethod const&); // Don't Implement.
    void operator=(JavaMethod const&); // Don't implement

private:
    JNIEnv *FEnv;
    jmethodID FMethod;
    jobject FObj;
public:
    JavaMethod(JNIEnv *Env, jobject Obj, jmethodID Method);

    void CallVoidMethod(int Count, ...);
    operator jmethodID ();
};


class JavaClass {
// Prohibit Heap-Based Classes.
private:
    static void *operator new(size_t size);
    static void operator delete(void *ptr);

    JavaClass(JavaClass const&); // Don't Implement.
    void operator=(JavaClass const&); // Don't implement

private:
    JNIEnv *FEnv;
    jclass FClass;
    TString FClassName;

public:
    JavaClass(JNIEnv *Env, TString Name);
    ~JavaClass();

    JavaStaticMethod GetStaticMethod(TString Name, TString Signature);
    operator jclass ();
};


class JavaStringArray {
// Prohibit Heap-Based Classes.
private:
    static void *operator new(size_t size);
    static void operator delete(void *ptr);

    JavaStringArray(JavaStringArray const&); // Don't Implement.
    void operator=(JavaStringArray const&); // Don't implement

private:
    JNIEnv *FEnv;
    jobjectArray FData;
    
    void Initialize(size_t Size);

public:
    JavaStringArray(JNIEnv *Env, size_t Size);
    JavaStringArray(JNIEnv *Env, jobjectArray Data);
    JavaStringArray(JNIEnv *Env, std::list<TString> Array);

    jobjectArray GetData();
    void SetValue(jsize Index, jstring Item);
    jstring GetValue(jsize Index);
    unsigned int Count();
};

#endif //JAVA_H
