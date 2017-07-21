/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include <jni.h>

#include <wtf/RefPtr.h>
#include <wtf/text/WTFString.h>
#include <WebCore/ExceptionCode.h>
#include <WebCore/ExceptionOr.h>

// Note that a pointer to a Node is not necessarily the same address
// as a pointer to an Element: a static_cast between the two is not
// necessarily a no-op.  (Though with most C++ implementations it will be.)
// Therefore, if p is a long (or a void*) then:
//   static_cast<Element*>(static_cast<Node*>(p))
// is not necessarily the same as:
//   static_cast<Element*>(p)
#define jlong_to_Nodeptr(p) static_cast<Node*>(jlong_to_ptr(p))

namespace WebCore {

enum JavaExceptionType {
    JavaDOMException = 0,
    JavaEventException,
    JavaRangeException,
    JavaUndefinedException
};

uint32_t getJavaHashCode(jobject o);
bool isJavaEquals(jobject o1, jobject o2);

void raiseTypeErrorException(JNIEnv*);
void raiseNotSupportedErrorException(JNIEnv*);

void raiseDOMErrorException(JNIEnv*, Exception&&);

template<typename T> T raiseOnDOMError(JNIEnv*, ExceptionOr<T>&&);
void raiseOnDOMError(JNIEnv*, ExceptionOr<void>&&);

inline void raiseOnDOMError(JNIEnv* env, ExceptionOr<void>&& possibleException)
{
    if (possibleException.hasException())
        raiseDOMErrorException(env, possibleException.releaseException());
}

template<typename T> inline T raiseOnDOMError(JNIEnv* env, ExceptionOr<T>&& exceptionOrReturnValue)
{
    if (exceptionOrReturnValue.hasException())
        raiseDOMErrorException(env, exceptionOrReturnValue.releaseException());
    return exceptionOrReturnValue.releaseReturnValue();
}

template <typename T> class JavaReturn {
    JNIEnv* m_env;
    RefPtr<T> m_returnValue;
public:
    JavaReturn(JNIEnv* env, T* returnValue)
    : m_env(env)
    , m_returnValue(returnValue)
    {}

    // JavaReturn(JNIEnv* env, T& returnValue)
    // : m_env(env)
    // // , m_returnValue(*returnValue)
    // {
    //    m_returnValue = *returnValue;
    // }

    // JavaReturn(JNIEnv* env, RefPtr<T>&& returnValue)
    // : m_env(env)
    // // , m_returnValue(returnValue)
    // {
    //     m_returnValue = WTF::move(returnValue);
    // }

    JavaReturn(JNIEnv* env, RefPtr<T> returnValue)
    : m_env(env)
    , m_returnValue(returnValue)
    {}

    operator jlong() {
        // there was a Java exception
        if (JNI_TRUE == m_env->ExceptionCheck())
            return 0L;
        //paired deref() call are in dispose Java method.
        return ptr_to_jlong(m_returnValue.release().leakRef());
    }
};

template <> class JavaReturn<WTF::String> {
    JNIEnv* m_env;
    WTF::String m_returnValue;
public:
    JavaReturn(JNIEnv* env, WTF::String returnValue)
    : m_env(env)
    , m_returnValue(returnValue)
    {}

    operator jstring() {
        // there was a Java exception
        if (JNI_TRUE == m_env->ExceptionCheck())
            return NULL;
        return m_returnValue.toJavaString(m_env).releaseLocal();
    }
};

} // namespace WebCore
