/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _GLASS_UTILS_
#define _GLASS_UTILS_

#ifndef _WIN32_WINNT
#   error The header should not be included directly. Do #include "common.h" instead.
#endif

#ifndef USER_TIMER_MINIMUM
#define USER_TIMER_MINIMUM 0x0000000A
#define USER_TIMER_MAXIMUM 0x7FFFFFFF
#endif

#if defined(DEBUG) || defined(_DEBUG)
#define ASSERT(condition)   \
        if (!(condition)) { \
            fprintf(stderr, "ERROR: %s (%s, %s, line %d)\n",    \
                #condition, __FUNCTION__, __FILE__, __LINE__);  \
            fflush(stderr); \
            ::DebugBreak(); \
        }
#else
#define ASSERT(condition)
#endif

#ifdef _WIN64
#define jlong_to_ptr(a) ((void*)(a))
#define ptr_to_jlong(a) ((jlong)(a))
#else
#define jlong_to_ptr(a) ((void*)(int)(a))
#define ptr_to_jlong(a) ((jlong)(int)(a))
#endif

#define jbool_to_bool(a) (((a) == JNI_TRUE) ? TRUE : FALSE)
#define bool_to_jbool(a) ((a) ? JNI_TRUE : JNI_FALSE)

#define IS_WINVER_ATLEAST(maj, min) \
                          (LOBYTE(LOWORD(::GetVersion())) >  (maj) || \
                           LOBYTE(LOWORD(::GetVersion())) == (maj) && \
                           HIBYTE(LOWORD(::GetVersion())) >= (min))
#define IS_WINXP IS_WINVER_ATLEAST(5, 1)
#define IS_WINVISTA IS_WINVER_ATLEAST(6, 0)
#define IS_WIN7 IS_WINVER_ATLEAST(6, 1)

#if defined(DEBUG) || defined(_DEBUG)
#define LOG(msg, ...) do { printf(msg, __VA_ARGS__); } while (0)
#else
#define LOG(msg, ...) do { } while (0)
#endif

///////////////////////////////////////////////////////////
// Java helper routines
///////////////////////////////////////////////////////////

JavaVM* GetJVM();
JNIEnv* GetEnv();

// Returns JNI_TRUE if there are exceptions
jboolean CheckAndClearException(JNIEnv *env);

jint GetModifiers();

class JString {
public:
    JString(JNIEnv *env, jstring jString) {
        init(env, jString, true);
    }
    JString(JNIEnv *env, jstring jString, bool autoDelete) {
        init(env, jString, autoDelete);
    }

    ~JString() {
        if (m_wszStr && m_autoDelete) {
            delete[] m_wszStr;
        }
    }

    operator wchar_t*() { return m_wszStr; }

    int length() { return m_len; }

private:
    void init(JNIEnv *env, jstring jString, bool autoDelete) {
        m_len = env->GetStringLength(jString);
        m_wszStr = new wchar_t[m_len + 1];
        env->GetStringRegion(jString, 0, m_len, (jchar *)m_wszStr);
        m_wszStr[m_len] = L'\0';
        m_autoDelete = autoDelete;
    }

    wchar_t *m_wszStr;
    int m_len;
    bool m_autoDelete;
};

// DNT == double null terminated
class DNTString {
public:
    DNTString(int limit) :
       m_limit(limit), m_length(0), m_substrings(NULL), m_count(0)
    {
        wszStr = new wchar_t[limit];
        memset(wszStr, 0, limit*sizeof(wchar_t));
    }
    ~DNTString() {
        if (wszStr) {
            delete[] wszStr;
        }
        if (m_substrings) {
            delete[] m_substrings;
        }
    }

    operator wchar_t*() { return wszStr; }

    size_t length() { return m_length; }

    size_t limit() { return m_limit; }

    void setLimit(size_t limit, bool copy = false) {
        wchar_t * const oldStr = wszStr;
        const size_t oldLimit = m_limit;

        m_limit = limit;
        wszStr = new wchar_t[limit];
        memset(wszStr, 0, limit*sizeof(wchar_t));

        if (copy && oldStr) {
            wmemcpy_s(wszStr, m_limit - 1, oldStr, min(oldLimit - 1, m_limit - 1));
            m_length = min(m_length, m_limit - 2);
        }

        if (oldStr) {
            delete[] oldStr;
        }
    }

    UINT count() {
        calculateSubstrings();
        return m_count;
    }

    wchar_t* substring(UINT i) {
        calculateSubstrings();
        return wszStr+m_substrings[i];
    }

    // appends the count characters of the src string to the DNT string
    void append(const wchar_t *wszSrc, const size_t count, bool allowGrow = false) {
        if (allowGrow) {
            if (m_length + count > m_limit - 2) {
                const size_t GROWTH_RATE = 2; // consider parameterizing this const

                setLimit((m_length + count + 2)*GROWTH_RATE, true);
            }
        }

        // "-1" because this is a _double_ null terminated string
        wcsncpy_s(wszStr + m_length, m_limit - m_length - 1, wszSrc, count);
        m_length += count; 
        if (m_length > m_limit) {
            m_length = m_limit;
        }
    }

    // recalculates the length of the DNT string
    // use the function when wszStr could be modified directly
    void calculateLength() {
        size_t i = 0;
        while(wszStr[i] != L'\0' || wszStr[i+1] != L'\0') {
            i++;
            if (i>= m_limit-1) {
                i = m_limit;
                break;
            }
        }
        m_length = i;
    }

private:

    void calculateSubstrings() {
        if (m_substrings)
            return;

        wchar_t prevChar = '\0';
        for (size_t i = 0; i < m_length; i++) {
            if (prevChar == '\0' && wszStr[i] != '\0') { // new substring
                m_count++;
            }
            prevChar = wszStr[i];
        }

        m_substrings = new size_t[m_count];
        m_count = 0;
        prevChar = '\0';
        for (size_t i = 0; i < m_length; i++) {
            if (prevChar == '\0' && wszStr[i] != '\0') { // new substring
                m_substrings[m_count] = i;
                m_count++;
            }
            prevChar = wszStr[i];
        }
    }

    wchar_t *wszStr;
    size_t m_length, m_limit;

    size_t *m_substrings;
    UINT m_count; // the count of the substrings
};

inline jstring CreateJString(JNIEnv *env, const wchar_t *wszStr) {
    if (wszStr == NULL)
        return NULL;
    jstring jStr = env->NewString((const jchar *)wszStr, jsize(wcslen(wszStr)));
    if (CheckAndClearException(env)) return NULL;
    return jStr;
}

inline jstring CreateJString(JNIEnv *env, const char *szStr) {
    if (szStr == NULL)
        return NULL;
    jstring jStr = env->NewStringUTF(szStr);
    if (CheckAndClearException(env)) return NULL;
    return jStr;
}

inline jstring ConcatJStrings(JNIEnv *env, jstring str1, jstring str2) {
    if (str1 == NULL || str2 == NULL)
        return NULL;
    jclass cls = env->FindClass("java/lang/String");
    if (CheckAndClearException(env)) {
        return NULL;
    }
    jmethodID mid = env->GetMethodID(cls, "concat", "(Ljava/lang/String;)Ljava/lang/String;");
    if (CheckAndClearException(env)) return NULL;
    jstring ret = (jstring)env->CallObjectMethod(str1, mid, str2);
    CheckAndClearException(env);

    return ret;
}

template <class T>
class JLocalRef {
    JNIEnv* m_env;
    T m_localJRef;

public:
    JLocalRef(JNIEnv* env, T localJRef = NULL)
        : m_env(env),
        m_localJRef(localJRef)
    {}
    T Detach() {
        T ret = m_localJRef;
        m_localJRef = NULL;
        return ret;
    }
    void Attach(T newValue) {
        if (m_localJRef) {
            m_env->DeleteLocalRef((jobject)m_localJRef);
        }
        m_localJRef = newValue;
    }

    operator T() { return m_localJRef; }
    operator bool() { return NULL!=m_localJRef; }
    bool operator !() { return NULL==m_localJRef; }

    ~JLocalRef() {
        if (m_localJRef) {
            m_env->DeleteLocalRef((jobject)m_localJRef);
        }
    }
};

template <class T>
class JGlobalRef {
    T m_globalJRef;

public:
    JGlobalRef() : m_globalJRef(NULL) {}

    JGlobalRef(T o) : m_globalJRef(NULL) // make sure it's NULL initially
    {
        Attach(GetEnv(), o);
    }

    void Attach(JNIEnv* env, T localJRef){
        if (m_globalJRef) {
            env->DeleteGlobalRef((jobject)m_globalJRef);
        }
        m_globalJRef = (T)(localJRef 
            ? env->NewGlobalRef((jobject)localJRef)
            : NULL);
    }

    JGlobalRef<T>& operator = (T localRef)
    {
        Attach(GetEnv(), localRef);
        return *this;
    }

    operator T() { return m_globalJRef; }
    operator bool() { return NULL!=m_globalJRef; }
    bool operator !() { return NULL==m_globalJRef; }

    ~JGlobalRef() {
        if (m_globalJRef) {
            GetEnv()->DeleteGlobalRef((jobject)m_globalJRef);
        }
    }
};


template <class T>
class MemHolder
{
public:
    MemHolder(size_t count)
    {
        m_pMem = reinterpret_cast<T *>(0==count
            ? NULL
            : malloc(count*sizeof(T)));
    }
    ~MemHolder(){
        free(m_pMem);
    }

    MemHolder& operator = (MemHolder &r)
    {
        if(this != &r){
            m_pMem = r.m_pMem;
            r.m_pMem = NULL;
        }
        return *this;
    }

    inline T *get()          { return m_pMem;  }
    inline operator T *()    { return m_pMem;  }
    inline operator bool()   { return NULL!=m_pMem;}
    inline bool operator !() { return NULL==m_pMem;}
private:
    T *m_pMem;
};

typedef JLocalRef<jobject> JLObject;
typedef JLocalRef<jstring> JLString;
typedef JLocalRef<jclass>  JLClass;
typedef JLocalRef<jobjectArray>  JLObjectArray;

template <class T>
class JArray {
    public:
        JArray() : data(NULL) {}
        ~JArray()
        {
            if (data) {
                GetEnv()->ReleasePrimitiveArrayCritical(array, data, JNI_ABORT);
            }
        }

        void Attach(JNIEnv *env, jarray a)
        {
            array.Attach(env, a);
        }

        T* GetPtr()
        {
            if (!data && array) {
                data = (T*)GetEnv()->GetPrimitiveArrayCritical(array, NULL);
            }
            return data;
        }

        operator bool() { return array; }
    private:
        JGlobalRef<jarray> array;
        T * data;
};

template <class T>
class JBufferArray {
    public:
        JBufferArray() : data(NULL), offset(0) {}

        void Attach(JNIEnv *env, jobject buf, jarray arr, jint offs)
        {
            if (!arr) {
                data = (T*)env->GetDirectBufferAddress(buf);
            } else {
                array.Attach(env, arr);
                offset = offs;
            }
        }

        T* GetPtr()
        {
            if (!data && array) {
                data = array.GetPtr();
                data += offset;
            }
            return data;
        }

        operator bool() { return data || array; }

    private:
        T* data;
        JArray<T> array;
        jint offset;
};

typedef struct _tagJavaIDs {
    struct {
        jmethodID notifyFocus;
        jmethodID notifyFocusDisabled;
        jmethodID notifyFocusUngrab;
        jmethodID notifyDestroy;
        jmethodID notifyDelegatePtr;
    } Window;
    struct {
        jmethodID notifyResize;
        jmethodID notifyRepaint;
        jmethodID notifyKey;
        jmethodID notifyMouse;
        jmethodID notifyMenu;
        jmethodID notifyScroll;
        jmethodID notifyInputMethod;
        jmethodID notifyInputMethodCandidatePosRequest;

        jmethodID notifyDragEnter;
        jmethodID notifyDragOver;
        jmethodID notifyDragLeave;
        jmethodID notifyDragDrop;

        jmethodID notifyView;

        jmethodID getWidth;
        jmethodID getHeight;
        jmethodID getAccessible;

        jfieldID  ptr;
    } View;
    struct {
        jmethodID init;
    } Size;
    struct {
        jmethodID attachData;
    } Pixels;
    struct {
        jmethodID getType;
        jmethodID getNativeCursor;
    } Cursor;
    struct {
        struct {
            jmethodID getDescription;
            jmethodID extensionsToArray;
        } ExtensionFilter;
        jmethodID createFileChooserResult;
    } CommonDialogs;
    struct {
        jmethodID run;
    } Runnable; 
    struct {
        jmethodID add;
    } List;
    struct {
        jmethodID gesturePerformedMID;
        jmethodID inertiaGestureFinishedMID;
        jmethodID notifyBeginTouchEventMID;
        jmethodID notifyNextTouchEventMID;
        jmethodID notifyEndTouchEventMID;
    } Gestures;
    struct {
        jmethodID init;
        jmethodID notifySettingsChanged;
    } Screen;
    struct {
        jmethodID reportExceptionMID;
        jmethodID notifyThemeChangedMID;
    } Application;
} JavaIDs;

extern JavaIDs javaIDs;


#endif //_GLASS_UTILS_

