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
#include "AccessibleSelectionProvider.h"
#include "AccessibleBaseProvider.h"

static jmethodID midCanSelectMultiple;
static jmethodID midGetIsSelectionRequired;
static jmethodID midGetSelection;

/**
 * Constructor
 *
 * env:     JNI envrionment
 * self:    Java side caller
 */
AccessibleSelectionProvider::AccessibleSelectionProvider(JNIEnv* env, jobject self) : AccessibleBasePatternProvider(env, self) {
    //LOG("AccessibleSelectionProvider::ctor\n");
}

/**
 * Destructor
 */
AccessibleSelectionProvider::~AccessibleSelectionProvider() {
    //LOG("AccessibleSelectionProvider::dtor\n");
}

/**
 * Increment the reference counter
 * 
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleSelectionProvider::AddRef() {
        //LOG("AccessibleSelectionProvider::AddRef\n");
    return AccessibleBasePatternProvider::AddRef();
}

/**
 * Decrement the reference counter
 *
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleSelectionProvider::Release() {
        //LOG("AccessibleSelectionProvider::Release\n");
    return AccessibleBasePatternProvider::Release();
}

/**
 * Get an interface pointer (overrides base class implementation)
 *
 * riid:        interface ID
 * ppInterface: receiver of the reference to the interface
 *
 * returns:     S_OK, E_NOINTERFACE
 */
IFACEMETHODIMP AccessibleSelectionProvider::QueryInterface(REFIID riid, void** ppInterface) {
    //LOG("In AccessibleSelectionProvider::QueryInterface\n");
    //LOG(" AccessibleBasePatternProvider this: %p\n", this);
    if (riid == __uuidof(ISelectionProvider)) {
        //LOG("  AccessibleBasePatternProvider riid: ISelectionProvider\n");
        *ppInterface = static_cast<ISelectionProvider*>(this);
        AddRef();
        return S_OK;
    } else {
        return AccessibleBasePatternProvider::QueryInterface(riid, ppInterface);
    }
}

// IRawElementProviderSimple implementation

/** 
 * Get a pattern provider
 *
 * patternID:   ID of the requested pattern
 * pRetvalue:   receiver of the pattern provider
 *
 * returns:     S_OK
 */
IFACEMETHODIMP AccessibleSelectionProvider::GetPatternProvider(PATTERNID patternId, IUnknown** pRetVal) {
    LOG("In IREPS AccessibleSelectionProvider::GetPatternProvider\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    LOG("  returning this\n");
    *pRetVal = static_cast<ISelectionProvider*>(this);
    AddRef();
    return S_OK;  
}

// ISelectionItemProvider implementation

/**
 * Get the state of the control.
 *
 * pRetVal:                receiver of the state
 *
 * returns:     S_OK
 */
IFACEMETHODIMP AccessibleSelectionProvider::get_IsSelectionRequired(BOOL *pRetVal) {
    LOG("In AccessibleSelectionProvider::get_IsSelectionRequired\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    *pRetVal = NULL;
    JNIEnv* env = GetEnv();
    jboolean value = env->CallBooleanMethod(m_self, midGetIsSelectionRequired);
    CheckAndClearException(env);  
    if (value) {
        LOG("  returning true\n");
        *pRetVal = TRUE;
    } else {
        LOG("  returning false\n");
        *pRetVal = FALSE;
    }
    return S_OK;
}

IFACEMETHODIMP AccessibleSelectionProvider::get_CanSelectMultiple(BOOL *pRetVal) {
    LOG("In AccessibleSelectionProvider::get_CanSelectMultiple\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    *pRetVal = NULL;
    JNIEnv* env = GetEnv();
    jboolean value = env->CallBooleanMethod(m_self, midCanSelectMultiple);
    CheckAndClearException(env);  
    if (value) {
        LOG("  returning true\n");
        *pRetVal = TRUE;
    } else {
        LOG("  returning false\n");
        *pRetVal = FALSE;
    }
    return S_OK;
}

IFACEMETHODIMP AccessibleSelectionProvider::GetSelection(SAFEARRAY **pRetVal) {
    LOG("In AccessibleSelectionProvider::GetSelection\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    JNIEnv* env = GetEnv();
        jlongArray selectedElements = static_cast<jlongArray>(env->CallObjectMethod(m_self, midGetSelection));
        if( selectedElements == NULL )
                return S_OK;
        jint size = env->GetArrayLength(selectedElements);
    if (size != 0) {
                //LOG("  size: %d\n", size);
        jlong* selectedElementsPatterns = env->GetLongArrayElements(selectedElements, JNI_FALSE);
                SAFEARRAY *psaElements = SafeArrayCreateVector(VT_I4, 0, size);
                if (psaElements == NULL) {
                        return E_OUTOFMEMORY;
                }
                for (LONG idx = 0; idx < size; idx++) {
                        LOG("  idx: %d\n", idx);
                        LOG("  selectedElements: %p\n", reinterpret_cast<void*>(jlong_to_ptr(selectedElementsPatterns[idx])) );
            SafeArrayPutElement(psaElements, &idx, (void*)(jlong_to_ptr(selectedElementsPatterns[idx])));
                }
        env->ReleaseLongArrayElements(selectedElements, selectedElementsPatterns, JNI_FALSE);
                *pRetVal = psaElements;
    } else 
        LOG("AccessibleSelectionProvider size=%d\n" , size);
    return S_OK;
}

////////////////////////////////////////
//
// Functions for JNI downcalls
//
////////////////////////////////////////

extern "C" {

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleSelectionProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleSelectionProvider__1initIDs(
    JNIEnv *env, jclass cls) {
    LOG("In WinAccessibleSelectionProvider._initIDs\n");
    midCanSelectMultiple = env->GetMethodID(cls, "canSelectMultiple", "()Z");
    ASSERT(midCanSelectMultiple);
        midGetIsSelectionRequired= env->GetMethodID(cls, "isSelectionRequired", "()Z");
    ASSERT(midGetIsSelectionRequired);
    midGetSelection = env->GetMethodID(cls, "getSelection", "()[J");
    ASSERT(midGetSelection);
}


/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleSelectionProvider
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
 *
 * note:        The ref count starts at 1
 */
JNIEXPORT jlong JNICALL
Java_com_sun_glass_ui_accessible_win_WinAccessibleSelectionProvider__1createAccessible(
        JNIEnv* env, jobject self, jlong accSimple) {
    LOG("In WinAccessibleSelectionProvider._createAccessible\n");
    LOG("  accSimple: %p\n", accSimple);
    // PTB: Do we need try/catch around the new?
    AccessibleSelectionProvider* acc = new AccessibleSelectionProvider(env, self);
    LOG("  acc: %p\n", acc);
        // Add this to the simple provider
        if (accSimple != 0) {
                AccessibleBaseProvider* accessibleSimple =
            reinterpret_cast<AccessibleBaseProvider *>(accSimple) ;
                if (accessibleSimple != NULL) {
                        accessibleSimple->AddPatternObject(reinterpret_cast<IUnknown*>(acc));
                } else {
                        LOG("  AddPatternObject not called; accessibleSimple is NULL.\n");
        }
        }
    return reinterpret_cast<jlong>(acc);
}

} /* extern "C" */

