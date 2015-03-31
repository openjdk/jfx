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


#include "Java.h"
#include "PlatformString.h"

#include <list>


//--------------------------------------------------------------------------------------------------

#ifdef DEBUG
TString JavaException::CreateExceptionMessage(JNIEnv* Env, jthrowable Exception,
    jmethodID GetCauseMethod, jmethodID GetStackTraceMethod, jmethodID ThrowableToTStringMethod,
    jmethodID FrameToTStringMethod) {

    TString result;
    jobjectArray frames = (jobjectArray)Env->CallObjectMethod(Exception, GetStackTraceMethod);

    // Append Throwable.toTString().
    if (0 != frames) {
        jstring jstr = (jstring)Env->CallObjectMethod(Exception, ThrowableToTStringMethod);
        const char* str = Env->GetStringUTFChars(jstr, 0);
        result += PlatformString(str).toPlatformString();
        Env->ReleaseStringUTFChars(jstr, str);
        Env->DeleteLocalRef(jstr);
    }

    // Append stack trace if one exists.
    if (Env->GetArrayLength(frames) > 0) {
        jsize i = 0;

        for (i = 0; i < Env->GetArrayLength(frames); i++) {
            // Get the string from the next frame and append it to
            // the error message.
            jobject frame = Env->GetObjectArrayElement(frames, i);
            jstring obj = (jstring)Env->CallObjectMethod(frame, FrameToTStringMethod);
            const char* str = Env->GetStringUTFChars(obj, 0);
            result += _T("\n  ");
            result += PlatformString(str).toPlatformString();
            Env->ReleaseStringUTFChars(obj, str);
            Env->DeleteLocalRef(obj);
            Env->DeleteLocalRef(frame);
        }
    }

    // If Exception has a cause then append the stack trace messages.
    if (0 != frames) {
        jthrowable cause = (jthrowable)Env->CallObjectMethod(Exception, GetCauseMethod);

        if (cause != NULL) {
            result += CreateExceptionMessage(Env, cause, GetCauseMethod,
                GetStackTraceMethod, ThrowableToTStringMethod,
                FrameToTStringMethod);
        }
    }

    return result;
}
#endif //DEBUG

JavaException::JavaException() : Exception() {}

//#ifdef WINDOWS
JavaException::JavaException(JNIEnv *Env, const TString Message) : Exception(Message) {
//#endif //WINDOWS
//#ifdef POSIX
//JavaException::JavaException(JNIEnv *Env, TString message) {
//#endif //POSIX

    FEnv = Env;
    FException = Env->ExceptionOccurred();
    Env->ExceptionClear();

#ifdef DEBUG
    Platform& platform = Platform::GetInstance();

    if (platform.GetDebugState() == dsNone) {
        jclass ThrowableClass = Env->FindClass("java/lang/Throwable");

        if (FEnv->ExceptionCheck() == JNI_TRUE) {
            Env->ExceptionClear();
            return;
        }

        jmethodID GetCauseMethod = Env->GetMethodID(ThrowableClass,
                                                    "getCause",
                                                    "()Ljava/lang/Throwable;");

        if (FEnv->ExceptionCheck() == JNI_TRUE) {
            Env->ExceptionClear();
            return;
        }

        jmethodID GetStackTraceMethod = Env->GetMethodID(ThrowableClass,
                                                            "getStackTrace",
                                                            "()[Ljava/lang/StackTraceElement;");

        if (FEnv->ExceptionCheck() == JNI_TRUE) {
            Env->ExceptionClear();
            return;
        }

        jmethodID ThrowableToTStringMethod = Env->GetMethodID(ThrowableClass,
                                                                "toString",
                                                                "()Ljava/lang/String;");

        if (FEnv->ExceptionCheck() == JNI_TRUE) {
            Env->ExceptionClear();
            return;
        }

        jclass FrameClass = Env->FindClass("java/lang/StackTraceElement");

        if (FEnv->ExceptionCheck() == JNI_TRUE) {
            Env->ExceptionClear();
            return;
        }

        jmethodID FrameToTStringMethod = Env->GetMethodID(FrameClass,
                                                            "toString",
                                                            "()Ljava/lang/String;");

        if (FEnv->ExceptionCheck() == JNI_TRUE) {
            Env->ExceptionClear();
            return;
        }

        TString lmessage = CreateExceptionMessage(Env, FException, GetCauseMethod,
            GetStackTraceMethod, ThrowableToTStringMethod, FrameToTStringMethod);
        SetMessage(lmessage);
    }
#endif //DEBUG
}

void JavaException::Rethrow() {
    FEnv->Throw(FException);
}

//--------------------------------------------------------------------------------------------------

JavaStaticMethod::JavaStaticMethod(JNIEnv *Env, jclass Class, jmethodID Method) {
    FEnv = Env;
    FClass = Class;
    FMethod = Method;
}

void JavaStaticMethod::CallVoidMethod(int Count, ...) {
    va_list args;
    va_start(args, Count);
    FEnv->CallStaticVoidMethodV(FClass, FMethod, args);
    va_end(args);

    if (FEnv->ExceptionCheck() == JNI_TRUE) {
        Messages& messages = Messages::GetInstance();
        throw JavaException(FEnv, messages.GetMessage(ERROR_INVOKING_METHOD));
    }
}

JavaStaticMethod::operator jmethodID () {
    return FMethod;
}

//--------------------------------------------------------------------------------------------------

JavaMethod::JavaMethod(JNIEnv *Env, jobject Obj, jmethodID Method) {
    FEnv = Env;
    FObj = Obj;
    FMethod = Method;
}

void JavaMethod::CallVoidMethod(int Count, ...) {
    va_list args;
    va_start(args, Count);
    FEnv->CallVoidMethodV(FObj, FMethod, args);
    va_end(args);

    if (FEnv->ExceptionCheck() == JNI_TRUE) {
        Messages& messages = Messages::GetInstance();
        throw JavaException(FEnv, messages.GetMessage(ERROR_INVOKING_METHOD));
    }
}

JavaMethod::operator jmethodID () {
    return FMethod;
}

//--------------------------------------------------------------------------------------------------

JavaClass::JavaClass(JNIEnv *Env, TString Name) {
    FEnv = Env;
    FClassName = Name;
    FClass = FEnv->FindClass(PlatformString(FClassName));

    if (FClass == NULL || FEnv->ExceptionCheck() == JNI_TRUE) {
        Messages& messages = Messages::GetInstance();
        TString message = messages.GetMessage(CLASS_NOT_FOUND);
        message = PlatformString::Format(message, FClassName.data());
        throw JavaException(FEnv, message);
    }
}

JavaClass::~JavaClass() {
    FEnv->DeleteLocalRef(FClass);

    if (FEnv->ExceptionCheck() == JNI_TRUE) {
        throw JavaException(FEnv, _T("Error"));
    }
}

JavaStaticMethod JavaClass::GetStaticMethod(TString Name, TString Signature) {
    jmethodID method = FEnv->GetStaticMethodID(FClass, PlatformString(Name), PlatformString(Signature));

    if (method == NULL || FEnv->ExceptionCheck() == JNI_TRUE) {
        Messages& messages = Messages::GetInstance();
        TString message = messages.GetMessage(METHOD_NOT_FOUND);
        message = PlatformString::Format(message, Name.data(), FClassName.data());
        throw JavaException(FEnv, message);
    }

    return JavaStaticMethod(FEnv, FClass, method);
}

JavaClass::operator jclass () {
    return FClass;
}

//--------------------------------------------------------------------------------------------------

void JavaStringArray::Initialize(size_t Size) {
    JavaClass jstringClass(FEnv, _T("java/lang/String"));

    if (FEnv->ExceptionCheck() == JNI_TRUE) {
        Messages& messages = Messages::GetInstance();
        TString message = messages.GetMessage(CLASS_NOT_FOUND);
        message = PlatformString::Format(message, _T("String"));
        throw JavaException(FEnv, message.data());
    }

    jstring str = PlatformString("").toJString(FEnv);
    FData = (jobjectArray)FEnv->NewObjectArray((jsize)Size, jstringClass, str);

    if (FEnv->ExceptionCheck() == JNI_TRUE) {
        throw JavaException(FEnv, _T("Error"));
    }
}

JavaStringArray::JavaStringArray(JNIEnv *Env, size_t Size) {
    FEnv = Env;
    Initialize(Size);
}

JavaStringArray::JavaStringArray(JNIEnv *Env, jobjectArray Data) {
    FEnv = Env;
    FData = Data;
}

JavaStringArray::JavaStringArray(JNIEnv *Env, std::list<TString> Items) {
    FEnv = Env;
    Initialize(Items.size());
    unsigned int index = 0;

    for (std::list<TString>::const_iterator iterator = Items.begin(); iterator != Items.end(); iterator++) {
        TString item = *iterator;
        SetValue(index, PlatformString(item).toJString(FEnv));
        index++;
    }
}

jobjectArray JavaStringArray::GetData() {
    return FData;
}

void JavaStringArray::SetValue(jsize Index, jstring Item) {
    FEnv->SetObjectArrayElement(FData, Index, Item);

    if (FEnv->ExceptionCheck() == JNI_TRUE) {
        throw JavaException(FEnv, _T("Error"));
    }
}

jstring JavaStringArray::GetValue(jsize Index) {
    jstring result = (jstring)FEnv->GetObjectArrayElement(FData, Index);

    if (FEnv->ExceptionCheck() == JNI_TRUE) {
        throw JavaException(FEnv, _T("Error"));
    }

    return result;
}

unsigned int JavaStringArray::Count() {
    unsigned int result = FEnv->GetArrayLength(FData);

    if (FEnv->ExceptionCheck() == JNI_TRUE) {
        throw JavaException(FEnv, _T("Error"));
    }

    return result;
}

//--------------------------------------------------------------------------------------------------
