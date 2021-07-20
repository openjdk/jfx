/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#pragma once

#include <jni.h>

#include <wtf/Ref.h>
#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>
#include <wtf/text/WTFString.h>
#include "ExceptionOr.h"

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
template<typename T> T* raiseOnDOMError(JNIEnv*, ExceptionOr<Ref<T>>&&);
String raiseOnDOMError(JNIEnv*, ExceptionOr<String>&&);
void raiseOnDOMError(JNIEnv*, ExceptionOr<void>&&);

inline void raiseOnDOMError(JNIEnv* env, ExceptionOr<void>&& possibleException)
{
    if (possibleException.hasException())
        raiseDOMErrorException(env, possibleException.releaseException());
}

inline String raiseOnDOMError(JNIEnv* env, ExceptionOr<String>&& exceptionOrReturnValue)
{
    if (exceptionOrReturnValue.hasException()) {
        raiseDOMErrorException(env, exceptionOrReturnValue.releaseException());
        return emptyString();
    }
    return exceptionOrReturnValue.releaseReturnValue();
}

template<typename T> inline T* raiseOnDOMError(JNIEnv* env, ExceptionOr<Ref<T>>&& exceptionOrReturnValue)
{
    if (exceptionOrReturnValue.hasException()) {
        raiseDOMErrorException(env, exceptionOrReturnValue.releaseException());
        return nullptr;
    }
    return WTF::getPtr(exceptionOrReturnValue.returnValue());
}

template<typename T> inline T raiseOnDOMError(JNIEnv* env, ExceptionOr<T>&& exceptionOrReturnValue)
{
    if (exceptionOrReturnValue.hasException()) {
        raiseDOMErrorException(env, exceptionOrReturnValue.releaseException());
        return static_cast<T>(NULL);
    }
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
        return ptr_to_jlong(WTFMove(m_returnValue).leakRef());
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
