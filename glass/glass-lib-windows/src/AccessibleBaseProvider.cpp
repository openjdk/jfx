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
#include "AccessibleBaseProvider.h"

static jmethodID midGetFragmentRoot;
static jmethodID midGetPropertyValue;
static jmethodID midNavigate;
static jmethodID midGetPatternProvider;

// Need to define static member data; otherwise get "unresolved external symbol" from linker
LONG AccessibleBaseProviderChildIDFactory::sm_ChildID = 0;
std::vector<LONG> AccessibleBaseProviderChildIDFactory::sm_reusePool;

/**
 * Constructor
 *
 * env:     JNI envrionment
 * self:    Java side caller
 */
AccessibleBaseProvider::AccessibleBaseProvider(JNIEnv* env, jobject self) : m_refCount(1),m_patternObjectCnt(0) {
    LOG("In AccessibleBaseProvider::ctor\n");
    m_self = env->NewGlobalRef(self);
}

/**
 * Destructor
 */
AccessibleBaseProvider::~AccessibleBaseProvider() {
    LOG("In AccessibleBaseProvider::dtor\n");
    JNIEnv* env = GetEnv();
    if (env) env->DeleteGlobalRef(m_self);
}

// IUnknown implementation.

/**
 * Increment the reference counter
 * 
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleBaseProvider::AddRef() {
    //LOG("In AccessibleBaseProvider::AddRef\n");
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
IFACEMETHODIMP_(ULONG) AccessibleBaseProvider::Release() {
    //LOG("In AccessibleBaseProvider::Release\n");
    //LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    long val = InterlockedDecrement(&m_refCount);
    if (val == 0) {
        AccessibleBaseProviderChildIDFactory::releaseChildID(m_id);
        LOG("  ChildID: %d\n", m_id);
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
IFACEMETHODIMP AccessibleBaseProvider::QueryInterface(REFIID riid, void** ppInterface) {
    LOG("In AccessibleBaseProvider::QueryInterface\n");
    LOG("  this: %p\n", this);
    // Use the pattern composition objects to determine which specific object to return
    if( FindPatternObject(riid, ppInterface) == S_OK ) {
        return S_OK;
    } else if (riid == __uuidof(IUnknown)) {
        LOG("  riid: IUnknown\n");
        *ppInterface = static_cast<IRawElementProviderSimple*>(this);
    } else if (riid == __uuidof(IRawElementProviderSimple)) {
        LOG("  riid: IRawElementProviderSimple\n");
        *ppInterface = static_cast<IRawElementProviderSimple*>(this);
    } else if (riid == __uuidof(IRawElementProviderFragment)) {
        LOG("  riid: IRawElementProviderFragment\n");
        *ppInterface = static_cast<IRawElementProviderFragment*>(this);
    } else if (riid == __uuidof(IRawElementProviderAdviseEvents)) {
        LOG("  Unhandled riid: IRawElementProviderAdviseEvents\n");
        *ppInterface = NULL;
        return E_NOINTERFACE;
    } else if (riid == __uuidof(IAccIdentity)) {
        LOG("  Unhandled riid: IAccIdentity\n");
        *ppInterface = NULL;
        return E_NOINTERFACE;
    } else if ( riid.Data1 == 0x65074F7F && riid.Data2 == 0x63C0 && riid.Data3 == 0x304E &&
                riid.Data4[0] == 0xAF && riid.Data4[1] == 0x0A &&
                riid.Data4[2] == 0xD5 && riid.Data4[3] == 0x17 &&
                riid.Data4[4] == 0x41 && riid.Data4[5] == 0xCB &&
                riid.Data4[6] == 0x4A && riid.Data4[7] == 0x8D ) {
        LOG("  Unhandled riid: _Object\n");
        *ppInterface = NULL;
        return E_NOINTERFACE;
    } else {
        LOG( "  Unhandled riid: %08X-%04X-%04X-%02X%02X-%02X%02X%02X%02X%02X%02X\n", 
                riid.Data1, riid.Data2, riid.Data3,
                riid.Data4[0], riid.Data4[1], riid.Data4[2], riid.Data4[3],
                riid.Data4[4], riid.Data4[5], riid.Data4[6], riid.Data4[7] );
        *ppInterface = NULL;
        return E_NOINTERFACE;
    }
    //LOG("  Calling AddRef from AccessibleBaseProvider::QI\n");
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
IFACEMETHODIMP AccessibleBaseProvider::get_HostRawElementProvider(IRawElementProviderSimple** pRetVal) {
    LOG("In IREPS AccessibleBaseProvider::get_HostRawElementProvider\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    if (pRetVal == NULL)
        return E_INVALIDARG;
    *pRetVal = NULL;
    return S_OK;
}

/**
 * Get provider options
 *
 * pRetVal: receiver of the return value
 *
 * returns: S_OK
 */

IFACEMETHODIMP AccessibleBaseProvider::get_ProviderOptions(ProviderOptions* pRetVal) {
    LOG("In IREPS AccessibleBaseProvider::get_ProviderOptions\n");
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
IFACEMETHODIMP AccessibleBaseProvider::GetPatternProvider(PATTERNID patternId, IUnknown** pRetVal) {
    LOG("In IREPS AccessibleBaseProvider::GetPatternProvider\n");
    LOG("  this: %p\n", this);
    LOG("  patternId: %d\n", patternId);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    JNIEnv* env = GetEnv();
    jlong acc = env->CallLongMethod(m_self, midGetPatternProvider, patternId);
    if (acc != NULL) {
        LOG("  returning: %p\n", acc);
        *pRetVal = reinterpret_cast<IUnknown *>(acc);
        reinterpret_cast<IUnknown*>(acc)->AddRef();
    } else {
        LOG("  returning NULL\n");
    }
    return S_OK;

    // TODO: Use AddRef when implementing
}

/**
 * Get a property
 *
 * propertyID:  ID of the requested property
 * pRetVal:     receiver of the property
 *
 * returns:     S_OK
 */
IFACEMETHODIMP AccessibleBaseProvider::GetPropertyValue(PROPERTYID propertyId, VARIANT* pRetVal) {
    LOG("In IREPS AccessibleBaseProvider::GetPropertyValue\n");
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
    switch(propertyId) {
    case UIA_BoundingRectanglePropertyId: {
        LOG("  ID: BoundingRectangle\n");
        double* pDoubleInSafeArray = NULL;  // pointer to doubles inside the safe array

        SAFEARRAY* psa = SafeArrayCreateVector(VT_R8, 0, 4);
        if (psa) {
            if (SUCCEEDED(hr = SafeArrayAccessData(psa, (void**)&pDoubleInSafeArray))) {
                jdoubleArray javaCoordinateArray =
                    static_cast<jdoubleArray>(env->CallObjectMethod( m_self, midGetPropertyValue,
                                                                     static_cast<jint>(propertyId) ));
                CheckAndClearException(env);  // TODO: Is this needed on Get/ReleaseDoubleArrayElements below?
                jint size = env->GetArrayLength(javaCoordinateArray);
                if (size != 0) {
                    // TODO: Are there any concerns that I am not using Get/ReleasePrimitiveArrayCritical?
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
        // controlType
        LOG("  ID: ControlType\n");
        jobject javaInteger = env->CallObjectMethod(m_self, midGetPropertyValue, static_cast<jint>(propertyId));
        if (javaInteger != NULL) {
            // get java.lang.Integer class
            jclass cls = env->GetObjectClass(javaInteger);
            // get method id
            jmethodID midIntValue = env->GetMethodID(cls, "intValue", "()I");
            if (midIntValue == NULL) {
                hr = E_FAIL;  // TODO: Is this right?
                LOG("    failure: intValue of ControlType\n");
            } else {
                // get value
                jint type = env->CallIntMethod(javaInteger, midIntValue);
                LOG("    type: %d\n", type);
                CheckAndClearException(env);  // TODO: Is this the right place for this?
                pRetVal->vt = VT_I4;
                pRetVal->lVal = type;
            }
        }
        break;
    }
    case UIA_HasKeyboardFocusPropertyId:
    case UIA_IsContentElementPropertyId:
    case UIA_IsControlElementPropertyId:
    case UIA_IsEnabledPropertyId:
    case UIA_IsKeyboardFocusablePropertyId:
    case UIA_IsSelectionPatternAvailablePropertyId:
    case UIA_IsSelectionItemPatternAvailablePropertyId:
    case UIA_IsRangeValuePatternAvailablePropertyId:
    case UIA_IsTogglePatternAvailablePropertyId: {
        // debug
        switch (propertyId) {
            case UIA_HasKeyboardFocusPropertyId:
                LOG("  ID: HasKeyboardFocus\n");
                break;
            case UIA_IsContentElementPropertyId:
                LOG("  ID: IsContentElement\n");
                break;
            case UIA_IsControlElementPropertyId:
                LOG("  ID: IsControlElement\n");
                break;
            case UIA_IsEnabledPropertyId:
                LOG("  ID: IsEnabled\n");
                break;
            case UIA_IsKeyboardFocusablePropertyId:
                LOG("  ID: IsKeyboardFocusable\n");
                break;
            case UIA_IsSelectionPatternAvailablePropertyId:
                LOG("  ID: IsSelectionPatternAvailable\n");
                break;
            case UIA_IsSelectionItemPatternAvailablePropertyId:
                LOG("  ID: IsSelectionItemPatternAvailable\n");
                break;
            case UIA_IsTogglePatternAvailablePropertyId:
                LOG("  ID: IsTogglePatternAvailable\n");
                break;
        }
        jobject javaBoolean = env->CallObjectMethod(m_self, midGetPropertyValue, static_cast<jint>(propertyId));
        if (javaBoolean == NULL) {
            LOG("  returning E_FAIL; javaBoolean is NULL\n");
            hr = E_FAIL;
        } else {
            // get java.lang.Boolean class
            jclass cls = env->GetObjectClass(javaBoolean);
            // get method id
            jmethodID midBooleanValue = env->GetMethodID(cls, "booleanValue", "()Z");
            if (midBooleanValue == NULL) {
                hr = E_FAIL;  // TODO: correct failure code?
                LOG("  returning E_FAIL; midBooleanValue is NULL\n");
            } else {
                // get value
                jboolean value = env->CallBooleanMethod(javaBoolean, midBooleanValue);
                CheckAndClearException(env);  // TODO: Is this the right place for this?
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
        CheckAndClearException(env);  // TODO: Is this needed on GetStringCritical, GetStringLength below?
        if (name == NULL) {
            hr = E_FAIL;  // TODO: correct failure code?
        } else {
            const jchar* jcstr = env->GetStringCritical(name, NULL);
            pRetVal->vt = VT_BSTR;
            pRetVal->bstrVal =
                ::SysAllocStringLen(reinterpret_cast<const OLECHAR *>(jcstr), env->GetStringLength(name));
            LOG("  Name: %ls\n", pRetVal->bstrVal);
            env->ReleaseStringCritical(name, jcstr);
        }
        //CheckAndClearException(env);
        // In cases where there are several env-> calls instead of several calls to CheckAndClearException
        // would it work to just put one at the end?
        hr = S_OK;
        break;
    }
    case UIA_NativeWindowHandlePropertyId:
        LOG("  ID: NativeWindowHandle\n");
        // TODO: Add more code later to return the HWND if there is one, else 0;
        pRetVal->vt = VT_I4;
        pRetVal->lVal = 0;
        break;
    default:
        LOG("  Unhandled Property ID: %d\n", propertyId);
    }
    return hr;
}

// IRawElementProviderFragment implementation

IFACEMETHODIMP AccessibleBaseProvider::get_BoundingRectangle(UiaRect *pRetVal) {
    LOG("In IREPF AccessibleBaseProvider::get_BoundingRectangle\n");
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
    CheckAndClearException(env);  // TODO: Is this needed on Get/ReleaseDoubleArrayElements below?
    jint size = env->GetArrayLength(javaCoordinateArray);
    if (size != 0) {
        // TODO: Are there any concerns that I am not using Get/ReleasePrimitiveArrayCritical?
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

/**
 * Get the fragment's root
 *
 * pRetVal:     receiver of the root
 *
 * returns:     S_OK
 */
IFACEMETHODIMP AccessibleBaseProvider::get_FragmentRoot(IRawElementProviderFragmentRoot **pRetVal) {
    LOG("In IREPF AccessibleBaseProvider::get_FragmentRoot\n");
    LOG("  this: %p\n", this);
   //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    if (pRetVal == NULL)
        return E_INVALIDARG;
    JNIEnv* env = GetEnv();
    jlong acc = env->CallLongMethod(m_self, midGetFragmentRoot);
    LOG("  acc: %p\n", acc);
    if (acc != NULL) {
        //LOG("  Calling AddRef from get_FragmentRoot\n");
        reinterpret_cast<IUnknown *>(acc)->AddRef();
        *pRetVal = reinterpret_cast<IRawElementProviderFragmentRoot *>(acc);
        return S_OK;
    } else {
        return E_FAIL;  // TODO: Is this the right failure code?
    }
}

IFACEMETHODIMP AccessibleBaseProvider::GetEmbeddedFragmentRoots(SAFEARRAY **pRetVal) {
    LOG("In IREPF AccessibleBaseProvider::GetEmbeddedFragmentRoots\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    *pRetVal = NULL;
    return S_OK;
}

IFACEMETHODIMP AccessibleBaseProvider::GetRuntimeId(SAFEARRAY **pRetVal) {
    LOG("In IREPF AccessibleBaseProvider::GetRuntimeId\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    if (pRetVal == NULL)
        return E_INVALIDARG;
    int rId[] = { UiaAppendRuntimeId, m_id };
    SAFEARRAY *psa = SafeArrayCreateVector(VT_I4, 0, 2);
    if (psa == NULL) {
        return E_OUTOFMEMORY;
    }
    for (LONG i = 0; i < 2; i++) {
        SafeArrayPutElement(psa, &i, (void*)&(rId[i]));
    }
    *pRetVal = psa;
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
 * Note: If there is no parent/sibling, NULL is returned via pRetVal.
 */
IFACEMETHODIMP AccessibleBaseProvider::Navigate(NavigateDirection direction, IRawElementProviderFragment **pRetVal) {
    LOG("In IREPF AccessibleBaseProvider::Navigate\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  direction: %d\n", direction); 
    if (pRetVal == NULL || direction < 0 || direction > 4)
        return E_INVALIDARG;
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
    *pRetVal = NULL;
    JNIEnv* env = GetEnv();
    jlong acc = env->CallLongMethod(m_self, midNavigate, direction);
    if (acc != NULL) {
        LOG("  returning: %p\n", acc);
        *pRetVal = reinterpret_cast<IRawElementProviderFragment *>(acc);
        //LOG("  Calling AddRef from AccessibleBaseProvider::Navigate\n");
        reinterpret_cast<IUnknown*>(acc)->AddRef();
    } else {
        LOG("  returning NULL\n");
    }
    return S_OK;
}

IFACEMETHODIMP AccessibleBaseProvider::SetFocus() {
    LOG("In IREPF AccessibleBaseProvider::SetFocus\n");
    LOG("  this: %p\n", this);
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  NOT IMPLEMENTED\n");
    return E_NOTIMPL;
}

void AccessibleBaseProvider::AddPatternObject(IUnknown* native)
{
    LOG("In AccessibleBaseProvider::AddPatternObject\n");
    LOG("  native: %p\n", native);
    LOG("  m_patternObjectCnt: %d\n", m_patternObjectCnt);
    m_patternObjects[m_patternObjectCnt++] = native ;
}

IFACEMETHODIMP AccessibleBaseProvider::FindPatternObject(REFIID riid,void **ppInterface)
{
    LOG("In AccessibleBaseProvider::FindPatternObject\n");
    LOG("  this: %p\n", this);
    bool found = false ;
    AccessibleBasePatternProvider *patternObject=NULL;
    for(int idx=0; idx<m_patternObjectCnt; idx++)
    {
        patternObject=(AccessibleBasePatternProvider*)m_patternObjects[idx];
        LOG("  patternObject %p\n", patternObject);
        if( patternObject->QueryInterface(riid,ppInterface) == S_OK )
        {
            LOG("  AccessibleBasePatternProvider found\n");
            return S_OK ;
        }
    }
    LOG("  AccessibleBasePatternProvider not found\n");
    // pattern not found in composition
    *ppInterface = NULL;
    return E_NOINTERFACE;
}


//////////////////////////////////////////////
//
// Methods for AccessibleBaseProviderChildIDFactory
//
//////////////////////////////////////////////

/**
 * getChildID
 *
 * Provides a negative, unique childID, either from a reuse pool for or a new one if
 * the reuse pool is empty.  These values are always negative to differentiate from
 * normal MSAA childIDs. See the class declaration in the h file for more information.
 *
 * Returns: LONG containing a unique childID
 *
 */
LONG
AccessibleBaseProviderChildIDFactory::getChildID(void) {
    LOG("In AccessibleBaseProviderChildIDFactory::getChildID\n");
    if (sm_reusePool.empty()) {
        // TODO: Is this the best way to handle this?  It shouldn't ever happen, i.e. we'd never have 2G active accessibles.
        ASSERT(sm_ChildID != LONG_MIN); 
        return --sm_ChildID;
    } else {
        // fetch the childID from the end of the vector and remove it from the vector
        LONG id = sm_reusePool.back();
        sm_reusePool.pop_back();
        return id;
    }
}

/**
 * releaseChildID
 *
 * Return a childID to the reuse pool
 *
 * id:       a LONG indicating the ID to return to the pool
 *
 * Returns: void
 *
 */
void
AccessibleBaseProviderChildIDFactory::releaseChildID(LONG id) {
    LOG("In AccessibleBaseProviderChildIDFactory::releaseChildID\n");
    sm_reusePool.push_back(id);
}

////////////////////////////////////////
//
// Functions for JNI downcalls
//
////////////////////////////////////////

extern "C" {

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider__1initIDs(
    JNIEnv *env, jclass cls) {
    LOG("In downcall for WinAccessibleBaseProvider._initIDs\n");
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    midGetFragmentRoot = env->GetMethodID(cls, "getFragmentRoot", "()J");
    ASSERT(midGetFragmentRoot);
    midGetPropertyValue = env->GetMethodID(cls, "getPropertyValue", "(I)Ljava/lang/Object;");
    ASSERT(midGetPropertyValue);
    midNavigate = env->GetMethodID(cls, "navigate", "(I)J");
    ASSERT(midNavigate);
    midGetPatternProvider = env->GetMethodID(cls, "getPatternProvider", "(I)J");
    ASSERT(midGetPatternProvider);
}

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider__1createAccessible(
    JNIEnv* env, jobject self) {
    LOG("In downcall for WinAccessibleBaseProvider._createAccessible\n");
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    // TODO: Do we need try/catch around the new?
    AccessibleBaseProvider* acc = new AccessibleBaseProvider(env, self);  // Starts with ref count of 1
    LOG("  acc: %p\n", acc);
    acc->m_id = AccessibleBaseProviderChildIDFactory::getChildID();
    LOG("  ChildID: %d\n", acc->m_id);
    return reinterpret_cast<jlong>(acc);
}

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider__1destroyAccessible(
    JNIEnv* env, jobject self, jlong acc) {
    LOG("In downcall for WinAccessibleBaseProvider._destroyAccessible\n");
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  acc: %p\n", acc);
    if (acc) {
        AccessibleBaseProvider* accessible = reinterpret_cast<AccessibleBaseProvider*>(acc);
        accessible->Release();
    }
}

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider
 * Method:      _fireEvent
 * Signature:   (JI)V
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider__1fireEvent(
    JNIEnv* env, jobject self, jlong acc, jint eventID) {
    LOG("In downcall for WinAccessibleBaseProvider._fireEvent\n");
    //TCHAR name[2048];
    //::GetModuleFileName(NULL, name, 2048);
    //LOG("  Process name: %S\n", name);
    //LOG("  Process ID: %d\n", ::GetCurrentProcessId);
    //LOG("  Thread ID: %d\n", ::GetCurrentThreadId());
    LOG("  acc: %p\n", acc);
    LOG("  eventID:");
    if (eventID == 20005) 
        LOG("  AUTOMATION_FOCUS_CHANGED\n");
    else
        LOG("  %d\n", eventID);
    UiaRaiseAutomationEvent(reinterpret_cast<AccessibleBaseProvider*>(acc), static_cast<EVENTID>(eventID));
}

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider
 * Method:      _firePropertyChange
 * Signature:   (JI)V
 * Java code:   native void _firePropertyChange(long nativeAccessible, int eventID, int oldProperty, int newProperty);
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider__1firePropertyChange__JIII(
    JNIEnv* env, jobject self, jlong acc, jint eventID, jint oldProperty, jint newProperty) {
    LOG("In downcall for WinAccessibleBaseProvider._firePropertyChange\n");
    LOG("  acc: %p\n", acc);
    LOG("  eventID:");
    VARIANT vtOld , vtNew ;
    VariantInit(&vtOld);
    vtOld.vt = VT_I4 ;
    vtOld.iVal = static_cast<int>(oldProperty);
    VariantInit(&vtNew);
    vtNew.vt = VT_I4 ;
    vtNew.iVal = static_cast<int>(newProperty);
    UiaRaiseAutomationPropertyChangedEvent(reinterpret_cast<AccessibleBaseProvider*>(acc), 
        static_cast<EVENTID>(eventID), vtOld, vtNew);
}
 
/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider
 * Method:      _firePropertyChange
 * Signature:   (JI)V
 * Java code:   native void _firePropertyChange(long nativeAccessible, int eventID, jboolean oldProperty, jboolean newProperty);
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleBaseProvider__1firePropertyChange__JIZZ(
    JNIEnv* env, jobject self, jlong acc, jint eventID, jboolean oldProperty, jboolean newProperty) {
    LOG("In downcall for WinAccessibleBaseProvider._firePropertyChangeBool Old %d New %d\n", oldProperty,newProperty );
    LOG("  acc: %p\n", acc);
    LOG("  eventID:");
    VARIANT vtOld , vtNew ;
    VariantInit(&vtOld);
    vtOld.vt = VT_BOOL ;
    vtOld.boolVal = static_cast<BOOLEAN>(oldProperty);
    VariantInit(&vtNew);
    vtNew.vt = VT_BOOL ;
    vtNew.boolVal = static_cast<BOOLEAN>(newProperty);
    UiaRaiseAutomationPropertyChangedEvent(reinterpret_cast<AccessibleBaseProvider*>(acc), 
        static_cast<EVENTID>(eventID), vtOld, vtNew);
}

} /* extern "C" */
