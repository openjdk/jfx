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

#ifndef _OLE_UTILS_H
#define _OLE_UTILS_H


inline void snvTrace(LPCTSTR lpszFormat, va_list argList)
{
    _vftprintf(stderr, _bstr_t(lpszFormat) + _T("\n"), argList);
    fflush(stderr);
}
inline void snTraceEmp(LPCTSTR, ...) { }
inline void snTrace(LPCTSTR lpszFormat, ... )
{
    va_list argList;
    va_start(argList, lpszFormat);
    snvTrace(lpszFormat, argList);
    va_end(argList);
}

#define STRACE1       snTrace
#if   defined(_DEBUG) || defined(DEBUG)
  #define STRACE      snTrace
#else // _DEBUG
  #define STRACE      snTraceEmp
#endif// _DEBUG
#define STRACE0       snTraceEmp

#define OLE_BAD_COOKIE ((DWORD)-1)

#define OLE_TRACENOTIMPL(msg)\
        STRACE(_T("Warning:%s"), msg);\
        return E_NOTIMPL;

#define OLE_TRACEOK(msg)\
        STRACE0(_T("Info:%s"), msg);\
        return S_OK;

#define OLE_DECL\
        HRESULT _hr_ = S_OK;

#define OLE_NEXT_TRY\
        try{

#define OLE_TRY\
        OLE_DECL\
        try{

#define OLE_HRT(fnc)\
        _hr_ = fnc;\
        if(FAILED(_hr_)){\
            STRACE1(_T("Error:%08x in ") _T(#fnc),  _hr_);\
            _com_raise_error(_hr_);\
        }

#define OLE_WINERROR2HR(msg, erCode)\
        _hr_ = erCode;\
        STRACE1(_T("OSError:%d in ") msg,  _hr_);\
        _hr_ = HRESULT_FROM_WIN32(_hr_);

#define OLE_REPORT_ERR(msg)\
        OLE_WINERROR2HR(msg, ::GetLastError())
        
#define OLE_THROW_LASTERROR(msg)\
        OLE_WINERROR2HR(msg, ::GetLastError())\
        _com_raise_error(_hr_);

#define OLE_CHECK_NOTNULL(x)\
        if(!(x)){\
            STRACE1(_T("Null pointer:") _T(#x));\
            _com_raise_error(_hr_ = E_POINTER);\
        }

#define OLE_CHECK_NOTNULLSP(x)\
        if(!bool(x)){\
            STRACE1(_T("Null pointer:") _T(#x));\
            _com_raise_error(_hr_ = E_POINTER);\
        }

#define OLE_HRW32(fnc)\
        _hr_ = fnc;\
        if(ERROR_SUCCESS!=_hr_){\
            STRACE1(_T("OSError:%d in ") _T(#fnc),  _hr_);\
            _com_raise_error(_hr_ = HRESULT_FROM_WIN32(_hr_));\
        }

#define OLE_HRW32_BOOL(fnc)\
        if(!fnc){\
            OLE_THROW_LASTERROR(_T(#fnc))\
        }

#define OLE_CATCH\
        }catch(_com_error &e){\
            _hr_ = e.Error();\
            STRACE1(_T("COM Error:%08x %s"), _hr_, e.ErrorMessage());\
        }

#define OLE_CATCH_BAD_ALLOC\
        }catch(_com_error &e){\
            _hr_ = e.Error();\
            STRACE1(_T("COM Error:%08x %s"), _hr_, e.ErrorMessage());\
        }catch(std::bad_alloc&){\
            _hr_ = E_OUTOFMEMORY;\
            STRACE1(_T("Error: Out of Memory"));\
        }

#define OLE_CATCH_ALL\
        }catch(_com_error &e){\
            _hr_ = e.Error();\
            STRACE1(_T("COM Error:%08x %s"), _hr_, e.ErrorMessage());\
        }catch(...){\
            _hr_ = E_FAIL;\
            STRACE1(_T("Error: General Pritection Failor"));\
        }

#define OLE_RETURN_SUCCESS return SUCCEEDED(_hr_);
#define OLE_RETURN_HR      return _hr_;
#define OLE_HR             _hr_

#define E_JAVAEXCEPTION  MAKE_HRESULT(SEVERITY_ERROR, 0xDE, 1)

#ifndef JNI_UTIL_H
inline void JNICALL JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg)
{
    jclass cls = env->FindClass(name);
    if (cls != 0) /* Otherwise an exception has already been thrown */
        env->ThrowNew(cls, msg);
}
inline void JNICALL JNU_ThrowIllegalAccessException(JNIEnv *env, const char *msg)
{
    JNU_ThrowByName(env, "java/lang/IllegalAccessException", msg);
}
inline void JNICALL JNU_ThrowIOException(JNIEnv *env, const char *msg)
{
    JNU_ThrowByName(env, "java/io/IOException", msg);
}
#endif

// The function is currently unused.
// Commented out to suppress a compiler warning on using unsafe _itow.
// Consider using _itow_s (req. VS2005+), or removing the function.
/*
inline void ThrowJNIErrorOnOleError(JNIEnv *env, HRESULT hr, const char *msg)
{
    if (SUCCEEDED(hr)) {
        return;
    }

    _bstr_t err(msg);
    WCHAR conv[64] = L"COM error:0x";
    _itow(hr, conv + 12, 16);
    err += conv;
    msg = err;

    WORD fs = (WORD)HRESULT_FACILITY(hr);
    WORD sc = (WORD)SCODE_CODE(hr);
    if (
        FACILITY_SECURITY == fs
        || (
                (
                    FACILITY_WINDOWS == fs ||
                    FACILITY_STORAGE == fs ||
                    FACILITY_RPC == fs ||
                    FACILITY_WIN32 == fs
                ) && ERROR_ACCESS_DENIED == sc
            )
    ) {
        JNU_ThrowIllegalAccessException(env, msg);
        return;
    }
    JNU_ThrowIOException(env, msg);
}
*/

inline HRESULT checkJavaException(JNIEnv *env)
{
    if (!env->ExceptionCheck()) {
        return S_OK;
    } else {
        JLocalRef<jthrowable> ex(env, env->ExceptionOccurred());
        if(ex){
            env->ExceptionClear();
            jclass cls = env->FindClass("java/lang/Throwable");    
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
                return E_JAVAEXCEPTION;
            }
            static jmethodID s_jcidThrowable_getMessage = env->GetMethodID(
                JLClass(env, cls),
                "getMessage",
                "()Ljava/lang/String;");            
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
                return E_JAVAEXCEPTION;
            }
            JLString jsMessage(env, (jstring)env->CallObjectMethod(
                ex,
                s_jcidThrowable_getMessage
            ));
            if(jsMessage){
                STRACE1(_T("Java Messsge:%s"), (LPCWSTR)JString(env, jsMessage) );
            }
            env->ExceptionDescribe();
        }
        env->ExceptionClear();
    }
    return E_JAVAEXCEPTION;
}

struct OLEHolder
{
    OLEHolder()
    : m_hr(::OleInitialize(NULL))
    {
        if (SUCCEEDED(m_hr)) {
            STRACE(_T("{OLE"));
        }
    }

    ~OLEHolder(){
        if (SUCCEEDED(m_hr)) {
            ::OleUninitialize();
            STRACE(_T("}OLE"));
        }
    }
    operator bool() const { return TRUE==SUCCEEDED(m_hr); }
    HRESULT m_hr;
};

template<typename _Interface>
struct IUnknownImpl : public _Interface
{
    IUnknownImpl() : m_cRef(1) {}
    virtual ~IUnknownImpl() {}

    //IUnknown
    STDMETHOD(QueryInterface)(REFIID riid, void **ppvObject)
    {
        OLE_DECL
        *ppvObject = NULL;
        if( IsEqualGUID(riid, IID_IUnknown) || IsEqualGUID(riid, __uuidof(_Interface))) {
            *ppvObject = (_Interface *)this;
        } else {
            return E_NOINTERFACE;
        }
        AddRef();
        return S_OK;
    }

    STDMETHOD_(ULONG, AddRef)()
    {
        return InterlockedIncrement(&m_cRef);
    }

    STDMETHOD_(ULONG, Release)()
    {
        LONG cRef = InterlockedDecrement(&m_cRef);
        if (0 == cRef) {
            delete this;
        }
        return cRef;
    }

protected:
    LONG    m_cRef;
};


#endif//_OLE_UTILS_H
