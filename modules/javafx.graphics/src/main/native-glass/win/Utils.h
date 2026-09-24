/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#define IS_WINVER_ATLEAST(maj, min) \
                          (LOBYTE(LOWORD(::GetVersion())) >  (maj) || \
                           LOBYTE(LOWORD(::GetVersion())) == (maj) && \
                           HIBYTE(LOWORD(::GetVersion())) >= (min))
#define IS_WINXP IS_WINVER_ATLEAST(5, 1)
#define IS_WINVISTA IS_WINVER_ATLEAST(6, 0)
#define IS_WIN7 IS_WINVER_ATLEAST(6, 1)
#define IS_WIN8 IS_WINVER_ATLEAST(6, 2)

#if defined(DEBUG) || defined(_DEBUG)
#define LOG(msg, ...) do { printf(msg, __VA_ARGS__); } while (0)
#else
#define LOG(msg, ...) do { } while (0)
#endif

///////////////////////////////////////////////////////////
// Keyboard and mouse-button modifier state
///////////////////////////////////////////////////////////

// The com.sun.glass.events.KeyEvent MODIFIER_* bits held down right now (GetKeyState), for the event
// callbacks of ViewContainer.cpp and GlassWindow.cpp. The JNI helpers that used to share this banner
// (GetEnv / CheckAndClearException / InitExceptionReporting) served UI Automation only and went with
// GlassAccessibleJni.h when the accessibility JNI was deleted.

int32_t GetModifiers();

// DNT == double null terminated
class DNTString {
public:
    DNTString(int limit) :
       m_limit(limit), m_length(0), m_substrings(NULL), m_count(0)
    {
        if (limit < MIN_LIMIT) {
            m_limit = limit = MIN_LIMIT;
        }
        wszStr = new wchar_t[limit];
        if (wszStr) {
            memset(wszStr, 0, limit * sizeof(wchar_t));
        } else {
            m_limit = MIN_LIMIT;
        }
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

        if (limit < MIN_LIMIT) {
            limit = MIN_LIMIT;
        }
        m_limit = limit;
        wszStr = new wchar_t[m_limit];
        if (wszStr) {
            memset(wszStr, 0, m_limit * sizeof(wchar_t));

            if (copy && oldStr) {
                wmemcpy_s(wszStr, m_limit - 1, oldStr, min(oldLimit - 1, m_limit - 1));
                m_length = min(m_length, m_limit - MIN_LIMIT);
            }

            if (oldStr) {
                delete[] oldStr;
            }
        } else {
            wszStr = oldStr;
            m_limit = oldLimit;
        }
    }

    UINT count() {
        calculateSubstrings();
        return m_count;
    }

    wchar_t* substring(UINT i) {
        if (wszStr == NULL) {
            return NULL;
        }
        calculateSubstrings();
        return wszStr+m_substrings[i];
    }

    // appends the count characters of the src string to the DNT string
    void append(const wchar_t *wszSrc, const size_t count, bool allowGrow = false) {
        if (wszSrc == NULL) {
            return;
        }
        if (allowGrow) {
            if (count > SIZE_MAX - m_length - 2) {
                return;
            }
            if (m_length + count > m_limit - 2) {
                const size_t GROWTH_RATE = 2; // consider parameterizing this const

                if ((m_length + count + 2) < (SIZE_MAX / GROWTH_RATE)) {
                    setLimit((m_length + count + 2) * GROWTH_RATE, true);
                } else {
                    setLimit((m_length + count + 2), true);
                }
            }
        }

        if (m_limit - m_length - 1 >= count) {
            // "-1" because this is a _double_ null terminated string
            int res = wcsncpy_s(wszStr + m_length, m_limit - m_length - 1, wszSrc, count);
            if (res == 0) { // wcsncpy_s succeeded
                m_length += count;
                if (m_length > m_limit) {
                    m_length = m_limit;
                }
            } else {
                m_length = 0;
            }
        }
    }

    // recalculates the length of the DNT string
    // use the function when wszStr could be modified directly
    void calculateLength() {
        if (wszStr == NULL) {
            return;
        }
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
        if (wszStr == NULL) {
            return;
        }
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
    static const size_t MIN_LIMIT = 2;

    size_t *m_substrings;
    UINT m_count; // the count of the substrings
};

template <class T>
class MemHolder
{
public:
    MemHolder(size_t count)
    {
        const bool invalid = (count == 0 || count > SIZE_MAX / sizeof(T));
        m_pMem = reinterpret_cast<T *>(invalid
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


#endif //_GLASS_UTILS_

