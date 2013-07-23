/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef JavaRef_h
#define JavaRef_h

#include <jni.h>

extern JavaVM* jvm;

ALWAYS_INLINE JNIEnv* JNICALL JavaScriptCore_GetJavaEnv()
{
    if (jvm) {
        void* env;
        jvm->GetEnv(&env, JNI_VERSION_1_2);
        return (JNIEnv*)env;
    }
    return 0;
}

extern bool CheckAndClearException(JNIEnv* env);

#define jlong_to_ptr(a) ((void*)(uintptr_t)(a))
#define ptr_to_jlong(a) ((jlong)(uintptr_t)(a))


template<typename T> class JLocalRef;
template<typename T> class JGlobalRef;

template<typename T = jobject> class JLocalRef
{
private:
    T m_jref;

public:
    void clear()
    {
        JNIEnv* env = JavaScriptCore_GetJavaEnv();
        if (env && m_jref) {
            env->DeleteLocalRef(m_jref);
            m_jref = NULL;
        }
    }

    static T copy(T ref)
    {
        JNIEnv* env = JavaScriptCore_GetJavaEnv();
        return (env && ref)
            ? static_cast<T>(env->NewLocalRef(ref))
            : 0;
    }

    friend class JGlobalRef<T>;

    JLocalRef(T ref = NULL, bool bycopy = false)
        : m_jref(bycopy ? copy(ref) : ref)
    {
    }

    JLocalRef(const JLocalRef<T>& other)
        : m_jref(copy(other))
    {
    }

    JLocalRef(const JGlobalRef<T>& other)
        : m_jref(copy(other))
    {
    }

    ~JLocalRef()
    {
        clear();
    }

    ALWAYS_INLINE bool operator!() const { return !m_jref; }

    ALWAYS_INLINE operator const T&() const
    {
        return m_jref;
    }

    ALWAYS_INLINE bool operator==(const JLocalRef<T>& other)
    {
        return m_jref==other.m_jref;
    }

    ALWAYS_INLINE bool operator!=(const JLocalRef<T>& other)
    {
        return m_jref!=other.m_jref;
    }

    ALWAYS_INLINE JLocalRef& operator=(const JLocalRef<T>& other)
    {
        if (other != *this) {
            clear();
            m_jref = copy(other);
        }
        return *this;
    }

    T releaseLocal()
    {
        T ret = m_jref;
        m_jref = 0;
        return ret;
    }
};

template<typename T = jobject> class JGlobalRef
{
private:
    T m_jref;

public:
    void clear()
    {
        JNIEnv* env = JavaScriptCore_GetJavaEnv();
        if (env && m_jref) {
            env->DeleteGlobalRef(m_jref);
            m_jref = NULL;
        }
    }

    static T copy(T ref)
    {
        JNIEnv* env = JavaScriptCore_GetJavaEnv();
        return (env && ref)
            ? static_cast<T>(env->NewGlobalRef(ref))
            : 0;
    }

    friend class JLocalRef<T>;

    JGlobalRef(T ref = NULL)
        : m_jref(copy(JLocalRef<T>(ref)))
    {
    }

    JGlobalRef(const JLocalRef<T>& other)
        : m_jref(copy(other))
    {
    }

    JGlobalRef(const JGlobalRef<T>& other)
        : m_jref(copy(other))
    {
    }

    ~JGlobalRef()
    {
        clear();
    }

    ALWAYS_INLINE operator const T&() const
    {
        return m_jref;
    }

    ALWAYS_INLINE bool operator==(const JGlobalRef<T>& other)
    {
        return m_jref==other.m_jref;
    }

    ALWAYS_INLINE bool operator!=(const JGlobalRef<T>& other)
    {
        return m_jref!=other.m_jref;
    }

    ALWAYS_INLINE JGlobalRef& operator=(const JGlobalRef<T>& other)
    {
        if (other != *this) {
            clear();
            m_jref = copy(other);
        }
        return *this;
    }

    ALWAYS_INLINE JGlobalRef& operator=(const JLocalRef<T>& other)
    {
        clear();
        m_jref = copy(other);
        return *this;
    }

    T releaseGlobal()
    {
        T ret = m_jref;
        m_jref = 0;
        return ret;
    }
};

#define WrapJavaRef(jref) jref

typedef JLocalRef<jstring>   JLString;
typedef JLocalRef<jclass>    JLClass;
typedef JLocalRef<jobject>   JLObject;
typedef JLocalRef<jobjectArray> JLObjectArray;
typedef JLocalRef<jbyteArray> JLByteArray;

typedef JGlobalRef<jstring>   JGString;
typedef JGlobalRef<jclass>    JGClass;
typedef JGlobalRef<jobject>   JGObject;
typedef JGlobalRef<jobjectArray> JGObjectArray;

#endif // JavaRef_h
