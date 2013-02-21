/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "common.h"
#include "AccessibleRoot.h"

static jmethodID midGetHostHwnd;
static jmethodID midGetPropertyValue;
static jmethodID midNavigate;

/**
 * Constructor
 *
 * env:     JNI envrionment
 * self:    Java side caller
 */
AccessibleRoot::AccessibleRoot(JNIEnv* env, jobject self) : m_refCount(1) {
    //LOG("AccessibleRoot::ctor\n");
    m_self = env->NewGlobalRef(self);
}

/**
 * Destructor
 */
AccessibleRoot::~AccessibleRoot() {
    //LOG("AccessibleRoot::dtor\n");
    JNIEnv* env = GetEnv();
    if (env) env->DeleteGlobalRef(m_self);
}

// IUnknown implementation.

/**
 * Increment the reference counter
 * 
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleRoot::AddRef() {
    //LOG("In AccessibleRoot::AddRef\n");
    //LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    return InterlockedIncrement(&m_refCount);
}

/**
 * Decrement the reference counter
 *
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleRoot::Release() {
    //LOG("In AccessibleRoot::Release\n");
    //LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    long val = InterlockedDecrement(&m_refCount);
    if (val == 0) {
        delete this;
    }
    return val;
}

/**
 * Get an interface pointer
 *
 * riid:        interface ID
 * ppInterface: receiver of the reference to the interface
 *
 * returns:     S_OK, E_NOINTERFACE
 */
IFACEMETHODIMP AccessibleRoot::QueryInterface(REFIID riid, void** ppInterface)
{
    //LOG("In AccessibleRoot::QueryInterface\n");
    //LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    if (riid == __uuidof(IUnknown)) {
        //LOG("  riid: IUnknown\n");
        *ppInterface = static_cast<IRawElementProviderSimple*>(this);
    } else if (riid == __uuidof(IRawElementProviderSimple)) {
        //LOG("  riid: IRawElementProviderSimple\n");
        *ppInterface = static_cast<IRawElementProviderSimple*>(this);
    } else if (riid == __uuidof(IRawElementProviderFragment)) {
        //LOG("  riid: IRawElementProviderFragment\n");
        *ppInterface = static_cast<IRawElementProviderFragment*>(this);
    } else if (riid == __uuidof(IRawElementProviderFragmentRoot)) {
        //LOG("  riid: IRawElementProviderFragmentRoot\n");
        *ppInterface = static_cast<IRawElementProviderFragmentRoot*>(this);
    } else if (riid == __uuidof(IRawElementProviderAdviseEvents)) {
        //LOG("  Unhandled riid: IRawElementProviderAdviseEvents\n");
        *ppInterface = NULL;
        return E_NOINTERFACE;
    } else if (riid == __uuidof(IProxyProviderWinEventHandler)) {
        //LOG("  Unhandled riid: IProxyProviderWinEventHandler\n");
        *ppInterface = NULL;
        return E_NOINTERFACE;
    } else {
        /*LOG( "  Unhandled riid: %08X-%04X-%04X-%02X%02X-%02X%02X%02X%02X%02X%02X\n", 
                riid.Data1, riid.Data2, riid.Data3,
                riid.Data4[0], riid.Data4[1], riid.Data4[2], riid.Data4[3],
                riid.Data4[4], riid.Data4[5], riid.Data4[6], riid.Data4[7] );*/
        *ppInterface = NULL;
        return E_NOINTERFACE;
    }

    //LOG("  Calling AddRef from AccessibleRoot::QI\n");
    AddRef();
    return S_OK;
}

// IRawElementProviderSimple implementation

/**
 * Get the UI Automation provider for the host window.
 *
 * ppRetVal:    receiver of the provider
 *
 * returns:     S_OK or a COM error code
 */
IFACEMETHODIMP AccessibleRoot::get_HostRawElementProvider(IRawElementProviderSimple** pRetVal) {
    LOG("In IREPS AccessibleRoot::get_HostRawElementProvider\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    if (pRetVal == NULL)
        return E_INVALIDARG;
    JNIEnv* env = GetEnv();
    jlong hwnd = env->CallLongMethod(m_self, midGetHostHwnd);
    if (hwnd != NULL) {
        return UiaHostProviderFromHwnd(reinterpret_cast<HWND>(hwnd), pRetVal);
    } else {
        return E_FAIL;  // PTB: Is this the right failure code?
    }
}

/**
 * Get provider options
 *
 * pRetVal: receiver of the return value
 *
 * returns: S_OK
 */

IFACEMETHODIMP AccessibleRoot::get_ProviderOptions(ProviderOptions* pRetVal) {
    LOG("In IREPS AccessibleRoot::get_ProviderOptions\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    if (pRetVal == NULL)
        return E_INVALIDARG;
    *pRetVal = ProviderOptions_ServerSideProvider;
    return S_OK;
}

/** 
 * Get a pattern provider
 *
 * patternID:   ID of the requested pattern
 * pRetvalue:   receiver of the pattern provider
 *
 * returns:     S_OK
 */
IFACEMETHODIMP AccessibleRoot::GetPatternProvider(PATTERNID patternId, IUnknown** pRetVal) {
    LOG("In IREPS AccessibleRoot::GetPatternProvider\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  patternId: %d\n", patternId);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    LOG("  NOT IMPLEMENTED\n");
    LOG("  returning NULL\n");
    *pRetVal = NULL;
    return S_OK;  
    // PTB: Use AddRef when implementing
}

/**
 * Get a property
 *
 * propertyID:  ID of the requested property
 * pRetVal:     receiver of the property
 *
 * returns:     S_OK
 */
IFACEMETHODIMP AccessibleRoot::GetPropertyValue(PROPERTYID propertyId, VARIANT* pRetVal) {
    LOG("In IREPS AccessibleRoot::GetPropertyValue\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    if (pRetVal == NULL)
        return E_INVALIDARG;
    // Set defaults
    pRetVal->vt = VT_EMPTY;
    HRESULT hr = S_OK;
    JNIEnv* env = GetEnv();
    if (!env) {
        LOG("  env is NULL; reattaching\n");
        GetJVM()->AttachCurrentThread((void**)&env, NULL);
    }
    switch (propertyId) {
    case UIA_BoundingRectanglePropertyId: {
        double* pDoubleInSafeArray = NULL;  // pointer to doubles inside the safe array

        SAFEARRAY* psa = SafeArrayCreateVector(VT_R8, 0, 4);
        if (psa) {
            if (SUCCEEDED(hr = SafeArrayAccessData(psa, (void**)&pDoubleInSafeArray))) {
                jdoubleArray javaCoordinateArray =
                    static_cast<jdoubleArray>(env->CallObjectMethod( m_self, midGetPropertyValue,
                                                                     static_cast<jint>(propertyId) ));
                CheckAndClearException(env);  // PTB: Is this needed on Get/ReleaseDoubleArrayElements below?
                jint size = env->GetArrayLength(javaCoordinateArray);
                if (size != 0) {
                    // PTB: Are there any concerns that I am not using Get/ReleasePrimitiveArrayCritical?
                    jdouble* pJavaCoordinates = env->GetDoubleArrayElements(javaCoordinateArray, JNI_FALSE);
                    for (int i = 0; i < 4; i++) {
                        // Load safe array with screen coordinates for left, top, width, height
                        *pDoubleInSafeArray = static_cast<double>(*pJavaCoordinates);
                        ++pDoubleInSafeArray;
                        ++pJavaCoordinates;
                    }
                    env->ReleaseDoubleArrayElements(javaCoordinateArray, pJavaCoordinates, JNI_FALSE);
                } else {
                    // if the object is hidden the caller will return an empty array; return all 0s
                    for (int i = 0; i < 4; ++i) {
                        *pDoubleInSafeArray = 0;
                        ++pDoubleInSafeArray;
                    }
                }
                hr = SafeArrayUnaccessData(psa);
                if (SUCCEEDED(hr)) {
                    pRetVal->vt = VT_ARRAY | VT_R8;
                    pRetVal->parray = psa; 
                }
            }
        }
        break;
    }
    case UIA_ControlTypePropertyId: {
        LOG("  ID: ControlType\n");
        jobject javaInteger = env->CallObjectMethod(m_self, midGetPropertyValue, static_cast<jint>(propertyId));
        if (javaInteger != NULL) {
            // get java.lang.Integer class
            jclass cls = env->GetObjectClass(javaInteger);
            // get method id
            jmethodID midIntValue = env->GetMethodID(cls, "intValue", "()I");
            if (midIntValue == NULL) {
                hr = E_FAIL;  // PTB: Is this right?
            } else {
                // get value
                jint type = env->CallIntMethod(javaInteger, midIntValue);
                CheckAndClearException(env);  // PTB: Is this the right place for this?
                pRetVal->vt = VT_I4;
                pRetVal->lVal = type;
            }
        }
        break;
    }
    case UIA_HasKeyboardFocusPropertyId:
        LOG("  ID: HasKeyboardFocus\n");
        pRetVal->vt = VT_BOOL;
        pRetVal->boolVal = false;
        break;
    case UIA_IsControlElementPropertyId:
        LOG("  ID: IsControlElement\n");
        pRetVal->vt = VT_BOOL;
        pRetVal->boolVal = false;
        break;
    case UIA_IsKeyboardFocusablePropertyId: {
        LOG("  ID: IsKeyboardFocusable\n");
        jobject javaBoolean = env->CallObjectMethod(m_self, midGetPropertyValue, static_cast<jint>(propertyId));
        if (javaBoolean == NULL) {
            hr = E_FAIL;
        } else {
            // get java.lang.Boolean class
            jclass cls = env->GetObjectClass(javaBoolean);
            // get method id
            jmethodID midBooleanValue = env->GetMethodID(cls, "booleanValue", "()Z");
            if (midBooleanValue == NULL) {
                hr = E_FAIL;  // PTB: correct failure code?
            } else {
                // get value
                jboolean value = env->CallBooleanMethod(javaBoolean, midBooleanValue);
                CheckAndClearException(env);  // PTB: Is this the right place for this?
                pRetVal->vt = VT_BOOL;
                if (value) {
                    LOG("  returning true\n");
                    pRetVal->boolVal = VARIANT_TRUE;
                } else {
                    LOG("  returning false\n");
                    pRetVal->boolVal = VARIANT_FALSE;
                }
            }
        }
        break;
    }
    case UIA_NamePropertyId: {
        LOG("  ID: Name\n");
        jstring name = static_cast<jstring>(env->CallObjectMethod( m_self, midGetPropertyValue,
                                                                   static_cast<jint>(propertyId) ));
        CheckAndClearException(env);  // PTB: Is this needed on GetStringCritical, GetStringLength below?
        if (name == NULL) {
            hr = E_FAIL;  // PTB: correct failure code?
        } else {
            const jchar* jcstr = env->GetStringCritical(name, NULL);
            pRetVal->vt = VT_BSTR;
            pRetVal->bstrVal =
                ::SysAllocStringLen(reinterpret_cast<const OLECHAR *>(jcstr), env->GetStringLength(name));
            LOG("  Name: %ls\n", pRetVal->bstrVal);
            env->ReleaseStringCritical(name, jcstr);
        }
        //CheckAndClearException(env);
        // PTB: In cases where there are several env-> calls instead of several calls to CheckAndClearException
        // would it work to just put one at the end?
        break;
    }
    case UIA_NativeWindowHandlePropertyId: {
        LOG("  ID: NativeWindowHandle\n");
        pRetVal->vt = VT_I4;
        JNIEnv* env = GetEnv();
        // PTB: Maybe, lfater, change this to CallLongMethod; and change Javaside from returning jlong to jint
        //   Even on 64 bit Wins HWNDs are 32 bits
        jlong hwnd = env->CallLongMethod(m_self, midGetHostHwnd);
        if (hwnd != NULL) {
            pRetVal->lVal = static_cast<LONG>(hwnd);
            LOG("  Handle: %X\n", pRetVal->lVal);
        } else {
            pRetVal->lVal = 0;  // PTB: Is this the right way to handle lack of a HWND?
            // PTB: Note that S_OK is sreturned
        }
        break;
    }
    default:
        LOG("  ID: Unhandled Propety ID: %d\n", propertyId);
    }
    return hr;
}

// IRawElementProviderFragment implementation

IFACEMETHODIMP AccessibleRoot::get_BoundingRectangle(UiaRect *pRetVal) {
    LOG("In IREPF AccessibleRoot::get_BoundingRectangle\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  NOT IMPLEMENTED\n");
    return E_NOTIMPL;
#if 0 // not ready yet
    if (pRetVal == NULL)
        return E_INVALIDARG;
    JNIEnv* env = GetEnv();
    HRESULT hr = E_FAIL;

    jdoubleArray javaCoordinateArray =
        static_cast<jdoubleArray>(env->CallObjectMethod( m_self, midGetPropertyValue,
                                                         static_cast<jint>(propertyId) ));
    CheckAndClearException(env);  // PTB: Is this needed on Get/ReleaseDoubleArrayElements below?
    jint size = env->GetArrayLength(javaCoordinateArray);
    if (size != 0) {
        // PTB: Are there any concerns that I am not using Get/ReleasePrimitiveArrayCritical?
        jdouble* pJavaCoordinates = env->GetDoubleArrayElements(javaCoordinateArray, JNI_FALSE);
        for (int i = 0; i < 4; i++) {
            // Load safe array with screen coordinates for left, top, width, height
            *pDoubleInSafeArray = static_cast<double>(*pJavaCoordinates);
            ++pDoubleInSafeArray;
            ++pJavaCoordinates;
        }
        env->ReleaseDoubleArrayElements(javaCoordinateArray, pJavaCoordinates, JNI_FALSE);
    } else {
        pRetVal->height = 0; pRetVal->left = 0; pRetVal->top = 0; pRetVal->width = 0;
    }

    return hr;
#endif
}

IFACEMETHODIMP AccessibleRoot::get_FragmentRoot(IRawElementProviderFragmentRoot **pRetVal) {
    LOG("In IREPF AccessibleRoot::get_FragmentRoot\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    *pRetVal = this;  // PTB: Is this right?
    //LOG("  Calling AddRef from get_FragmentRoot\n");
    AddRef();
    LOG("  returning: %p\n", this);
    return S_OK;
}

IFACEMETHODIMP AccessibleRoot::GetEmbeddedFragmentRoots(SAFEARRAY **pRetVal) {
    LOG("In IREPF AccessibleRoot::GetEmbeddedFragmentRoots\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    *pRetVal = NULL;
    return S_OK;
}

IFACEMETHODIMP AccessibleRoot::GetRuntimeId(SAFEARRAY **pRetVal) {
    LOG("In IREPF AccessibleRoot::GetRuntimeId\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    *pRetVal = NULL;
    return S_OK;
}

/**
 * Get a parent/child/sibling
 *
 * direction:   specifies target (parent/child/sibling)
 * pRetVal:     specifies where the target should be returned
 *
 * returns:     S_OK
 *
 * Note: Fragment roots have no parent or siblings.
 */
IFACEMETHODIMP AccessibleRoot::Navigate(NavigateDirection direction, IRawElementProviderFragment **pRetVal) {
    LOG("In IREPF AccessibleRoot::Navigate\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  direction:");
    if (pRetVal == NULL || direction < 0 || direction > 4)
        return E_INVALIDARG;
    *pRetVal = NULL;
    // debug
    switch (direction) {
    case NavigateDirection_Parent:
        LOG("  NavigateDirection_Parent\n");
        break;
    case NavigateDirection_NextSibling:
        LOG("  NavigateDirection_NextSibling\n");
        break;
    case NavigateDirection_PreviousSibling:
        LOG("  NavigateDirection_PreviousSibling\n");
        break;
    case NavigateDirection_FirstChild:
        LOG("  NavigateDirection_FirstChild\n");
        break;
    case NavigateDirection_LastChild:
        LOG("  NavigateDirection_LastChild\n");
        break;
    }
    if (direction == NavigateDirection_FirstChild || direction == NavigateDirection_LastChild ) {
        JNIEnv* env = GetEnv();
        jlong acc = env->CallLongMethod(m_self, midNavigate, direction);
        if (acc != 0) {
            LOG("  returning: %p\n", acc);
            *pRetVal = reinterpret_cast<IRawElementProviderFragment *>(acc);
            //LOG("  Calling AddRef from AccessibleRoot::Navigate\n");
            reinterpret_cast<IUnknown*>(acc)->AddRef();
        }
    }
    if (*pRetVal == NULL)
        LOG("  returning NULL\n");
    return S_OK;
}

IFACEMETHODIMP AccessibleRoot::SetFocus() {
    LOG("In IREPF AccessibleRoot::SetFocus\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  NOT IMPLEMENTED\n");
    return E_NOTIMPL;
}

// IRawElementProviderFragmentRoot implementation

IFACEMETHODIMP AccessibleRoot::ElementProviderFromPoint( double x, double y, 
                                                         IRawElementProviderFragment **pRetVal ) {
    LOG("In IREPFRoot AccessibleRoot::ElementProviderFromPoint\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  NOT IMPLEMENTED\n");
    return E_NOTIMPL;
    // PTB: Use AddRef when implementing
}

IFACEMETHODIMP AccessibleRoot::GetFocus(IRawElementProviderFragment **pRetVal) {
    LOG("In IREPFRoot AccessibleRoot::GetFocus\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  NOT IMPLEMENTED\n");
    return E_NOTIMPL;
    // PTB: Use AddRef when implementing
}

////////////////////////////////////////
//
// Functions for JNI downcalls
//
////////////////////////////////////////

extern "C" {

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleRoot
 * Method:      _initIDs
 * Signature:   ()V
 * Java Code:   native static void _initIDs();
 * 
 * Down call to intialize JNI IDs, e.g. method IDs
 *
 * env: JNIEnv* referencing the JNI environment
 * cls: jclass of the calling class
 *
 * Note: This method is called from a Java class method (not an instance method),
 *       thus the second parameter is a jclass rather than a jobject.
 */
JNIEXPORT void JNICALL
Java_com_sun_glass_ui_accessible_win_WinAccessibleRoot__1initIDs(JNIEnv *env, jclass cls) {
    LOG("In WinAccessibleRoot._initIDs\n");
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    midGetHostHwnd = env->GetMethodID(cls, "getHostHwnd", "()J");
    ASSERT(midGetHostHwnd);
    midGetPropertyValue = env->GetMethodID(cls, "getPropertyValue", "(I)Ljava/lang/Object;");
    ASSERT(midGetPropertyValue);
    midNavigate = env->GetMethodID(cls, "navigate", "(I)J");
    ASSERT(midNavigate);
}

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleRoot
 * Method:      _createAccessible
 * Signature:   (I)J
 * Java code:   native long _createAccessible();
 *
 * Down call to create a native accessible.
 *
 * env:         JNIEnv* referencing the JNI environment
 * self:        jobject of the calling java side accessible
 *
 * returns:     jlong returning the address of the native accessible
 *              if 0 the Java side should throw an exception
 */
JNIEXPORT jlong JNICALL
Java_com_sun_glass_ui_accessible_win_WinAccessibleRoot__1createAccessible(
    JNIEnv* env, jobject self) {
    LOG("In WinAccessibleRoot._createAccessible\n");
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    // PTB: Do we need try/catch around the new?
    AccessibleRoot* acc = new AccessibleRoot(env, self);  // Starts with ref count of 1
    LOG("  acc: %p\n", acc);
    return reinterpret_cast<jlong>(acc);
}

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleRoot
 * Method:      _destroyAccessible
 * Signature:   (J)V
 * Java code:   native void _destroyAccessible(long nativeAccessible);
 *
 * Down call to destroy a native accessible
 *
 * env:         JNIEnv* referencing the JNI environment
 * self:        jobject of the calling java side accessible
 * acc:         jlong containing the address of the native accessible to be destroyed
 *
 * returns:     void
 */
JNIEXPORT void JNICALL
Java_com_sun_glass_ui_accessible_win_WinAccessibleRoot__1destroyAccessible(
    JNIEnv* env, jobject self, jlong acc) {
    LOG("In WinAccessibleRoot._destroyAccessible\n");
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  acc: %p\n", acc);
    if (acc) {
        AccessibleRoot* accessible = reinterpret_cast<AccessibleRoot*>(acc);
        accessible->Release();
    }
}

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleRoot
 * Method:      _fireEvent
 * Signature:   (I)V
 * Java code:   native void _fireEvent(long nativeAccessible, int eventID);
 *
 * Down call to fire an event
 *
 * env:         JNIEnv* referencing the JNI environment
 * self:        jobject of the calling java side accessible
 * acc:         jlong containing the address of the native accessible to be destroyed
 * eventID:     jint containing the event ID
 *
 * returns:     void
 */
JNIEXPORT void JNICALL
Java_com_sun_glass_ui_accessible_win_WinAccessibleRoot__1fireEvent(
    JNIEnv* env, jobject self, jlong acc, jint eventID) {
    LOG("In downcall for WinAccessibleRoot._fireEvent\n");
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  acc: %p\n", acc);
    UiaRaiseAutomationEvent(reinterpret_cast<AccessibleRoot*>(acc), static_cast<EVENTID>(eventID));
}

} /* extern "C" */
