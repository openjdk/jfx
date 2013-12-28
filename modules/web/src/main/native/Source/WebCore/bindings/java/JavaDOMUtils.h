/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef _JavaDOMUtils_h
#define _JavaDOMUtils_h

#include <jni.h>

#include "wtf/RefPtr.h"
#include "wtf/text/WTFString.h"
#include "WebCore/dom/RangeException.h"
#include "WebCore/dom/ExceptionCodePlaceholder.h"

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

class JavaException {
private:
    ExceptionCode m_ec;
    JNIEnv* m_env;
    JavaExceptionType m_type1;
    JavaExceptionType m_type2;
public:
    JavaException(JNIEnv* env, JavaExceptionType type1, JavaExceptionType type2 = JavaUndefinedException)
    : m_ec(0)
    , m_env(env)
    , m_type1(std::min(type1, type2))
    , m_type2(std::max(type1, type2))
    {
    }

    ~JavaException() {
        if (m_ec) {
            JavaExceptionType type = (JavaRangeException == m_type2
                && (
                       m_ec == RangeException::BAD_BOUNDARYPOINTS_ERR
                    || m_ec == RangeException::INVALID_NODE_TYPE_ERR
                )
            )
            ? JavaRangeException
            : m_type1;

            //lazy init
            jclass clz = 0L;
            jmethodID  mid = 0;
            switch(type) {
            case JavaDOMException:
                {
                    static JGClass exceptionClass(m_env->FindClass("org/w3c/dom/DOMException"));
                    static jmethodID midCtor = m_env->GetMethodID(exceptionClass, "<init>", "(SLjava/lang/String;)V");
                    clz = exceptionClass;
                    mid = midCtor;
                }
                break;
            case JavaEventException:
                {
                    static JGClass exceptionClass(m_env->FindClass("org/w3c/dom/events/EventException"));
                    static jmethodID midCtor = m_env->GetMethodID(exceptionClass, "<init>", "(SLjava/lang/String;)V");
                    clz = exceptionClass;
                    mid = midCtor;
                }
                break;
            case JavaRangeException:
                {
                    static JGClass exceptionClass(m_env->FindClass("org/w3c/dom/ranges/RangeException"));
                    static jmethodID midCtor = m_env->GetMethodID(exceptionClass, "<init>", "(SLjava/lang/String;)V");
                    clz = exceptionClass;
                    mid = midCtor;
                }
                break;
            }

            ASSERT(mid);
            m_env->Throw(JLocalRef<jthrowable>(
                (jthrowable)m_env->NewObject(clz, mid, (jshort)m_ec, 0)
            ));
        }
    }

    operator ExceptionCode&() {
        return m_ec;
    }

    ExceptionCode* operator&() {
        m_ec = 0;
        return &m_ec;
    }
};

template <typename T> class JavaReturn {
    RefPtr<T> m_returnValue;
    JNIEnv* m_env;
public:
    JavaReturn(JNIEnv* env, PassRefPtr<T> returnValue)
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
    WTF::String m_returnValue;
    JNIEnv* m_env;
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

}

extern "C" {

uint32_t getJavaHashCode(jobject o);
bool isJavaEquals(jobject o1, jobject o2);

}


#endif
