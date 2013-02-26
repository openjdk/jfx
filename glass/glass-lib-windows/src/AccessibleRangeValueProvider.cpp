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
#include "AccessibleRangeValueProvider.h"
#include "AccessibleBaseProvider.h"

static jmethodID midGetValue;
static jmethodID midSetValue;
static jmethodID midGetMaximum;
static jmethodID midGetMinimum;
static jmethodID midGetSmallChange;
static jmethodID midGetIsReadOnly;

/**
 * Constructor
 *
 * env:     JNI envrionment
 * self:    Java side caller
 */
AccessibleRangeValueProvider::AccessibleRangeValueProvider(JNIEnv* env, jobject self) : AccessibleBasePatternProvider(env, self) {
    //LOG("AccessibleRangeValueProvider::ctor\n");
}

/**
 * Destructor
 */
AccessibleRangeValueProvider::~AccessibleRangeValueProvider() {
    //LOG("AccessibleRangeValueProvider::dtor\n");
}

// IUnknown implementation.

/**
 * Increment the reference counter
 * 
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleRangeValueProvider::AddRef() {
    //LOG("In AccessibleRangeValueProvider::AddRef\n");
    //LOG("  this: %p\n", this);
    return AccessibleBasePatternProvider::AddRef();
}

/**
 * Decrement the reference counter
 *
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleRangeValueProvider::Release() {
    //LOG("In AccessibleElement::Release\n");
    //LOG("  this: %p\n", this);
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
IFACEMETHODIMP AccessibleRangeValueProvider::QueryInterface(REFIID riid, void** ppInterface) {
    //LOG("In AccessibleRangeValueProvider::QueryInterface\n");
    //LOG("  this: %p\n", this);
    if (riid == __uuidof(IRangeValueProvider)) {
        //LOG("  riid: IRangeValueProvider\n");
        *ppInterface = static_cast<IRangeValueProvider*>(this);
        //LOG("  Calling AddRef from AccessibleRangeValueProvider::QI\n");
        AccessibleBasePatternProvider::AddRef();
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
IFACEMETHODIMP AccessibleRangeValueProvider::GetPatternProvider(PATTERNID patternId, IUnknown** pRetVal) {
    LOG("In IREPS AccessibleRangeValueProvider::GetPatternProvider\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    LOG("  returning this\n");
    *pRetVal = static_cast<IRangeValueProvider*>(this);
    AccessibleBasePatternProvider::AddRef();
    return S_OK;  
}

// IRangeValueProvider implementation

/**
 * Get the state of the control.
 *
 * pRetVal:                receiver of the state
 *
 * returns:     S_OK
 */
IFACEMETHODIMP AccessibleRangeValueProvider::get_Value(double *valuePtr) {
    LOG("In AccessibleRangeValueProvider::get_Value\n");
    LOG("  this: %p\n", this);
    if (valuePtr == NULL)
        return E_INVALIDARG;
    *valuePtr = NULL;
    JNIEnv* env = GetEnv();
    jdouble value = env->CallDoubleMethod(m_self, midGetValue);
    CheckAndClearException(env);  // PTB: Is this the right place for this?
    *valuePtr = value;
    return S_OK;
}

IFACEMETHODIMP AccessibleRangeValueProvider::get_IsReadOnly(BOOL *pRetVal) {
        LOG("In AccessibleRangeValueProvider::get_IsReadOnly\n");
    LOG("  this: %p\n", this);
        JNIEnv* env = GetEnv();
    jboolean val = env->CallBooleanMethod(m_self, midGetIsReadOnly);
    CheckAndClearException(env);  // PTB: Is this the right place for this?
    *pRetVal = val;
        LOG(" Returning %f\n",val);
    return S_OK;
}
        
IFACEMETHODIMP AccessibleRangeValueProvider::get_Maximum(double *pRetVal) {
        LOG("In AccessibleRangeValueProvider::get_Maximum\n");
    LOG("  this: %p\n", this);
        *pRetVal = NULL;
    JNIEnv* env = GetEnv();
    jdouble val = env->CallDoubleMethod(m_self, midGetMaximum);
    CheckAndClearException(env);  // PTB: Is this the right place for this?
    *pRetVal = val;
        LOG(" Returning %f\n",val);
    return S_OK;
}

IFACEMETHODIMP AccessibleRangeValueProvider::get_Minimum(double *pRetVal) {
        LOG("In AccessibleRangeValueProvider::get_Minimum\n");
    LOG("  this: %p\n", this);
        *pRetVal = NULL;
    JNIEnv* env = GetEnv();
    jdouble val = env->CallDoubleMethod(m_self, midGetMinimum);
    CheckAndClearException(env);  // PTB: Is this the right place for this?
    *pRetVal = val;
        LOG(" Returning %f\n",val);
    return S_OK;
}
        
IFACEMETHODIMP AccessibleRangeValueProvider::get_LargeChange(double *pRetVal) {
        LOG("In AccessibleRangeValueProvider::get_LargeChange\n");
    LOG("  this: %p\n", this);
        LOG("  NOT IMPLEMENTED\n");
    return E_NOTIMPL;
}
        
IFACEMETHODIMP AccessibleRangeValueProvider::get_SmallChange(double *pRetVal) {
        LOG("In AccessibleRangeValueProvider::get_SmallChange\n");
    LOG("  this: %p\n", this);
        *pRetVal = NULL;
    JNIEnv* env = GetEnv();
    jdouble val = env->CallDoubleMethod(m_self, midGetSmallChange);
    CheckAndClearException(env);  // PTB: Is this the right place for this?
    *pRetVal = val;
        LOG(" Returning %f\n",val);
    return S_OK;
}

IFACEMETHODIMP AccessibleRangeValueProvider::SetValue(double) {
    LOG("In AccessibleRangeValueProvider::setValue\n");
    LOG("  this: %p\n", this);
    JNIEnv* env = GetEnv();
    //env->CallVoidMethod(m_self, midSetValue);
    //CheckAndClearException(env);  // PTB: Is this the right place for this
    return S_OK;
}

////////////////////////////////////////
//
// Functions for JNI downcalls
//
////////////////////////////////////////

extern "C" {

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleRangeValueProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleRangeValueProvider__1initIDs(
    JNIEnv *env, jclass cls) {
    LOG("In WinAccessibleRangeValueProvider._initIDs\n");
    midGetValue = env->GetMethodID(cls, "getValue", "()D");
    ASSERT(midGetValue);
        midGetMaximum = env->GetMethodID(cls, "getMaximum", "()D");
    ASSERT(midGetMaximum);
        midGetMinimum = env->GetMethodID(cls, "getMinimum", "()D");
    ASSERT(midGetMinimum);
        midGetSmallChange = env->GetMethodID(cls, "getSmallChange", "()D");
    ASSERT(midGetSmallChange);
        midGetIsReadOnly = env->GetMethodID(cls, "getIsReadOnly", "()Z");
    ASSERT(midGetIsReadOnly);
        //midSetValue = env->GetMethodID(cls, "setValue", "()");
    //ASSERT(midSetValue);
}

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleRangeValueProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleRangeValueProvider__1createAccessible(
    JNIEnv* env, jobject self, jlong accSimple) {
    LOG("In WinAccessibleRangeValueProvider._createAccessible\n");
    LOG("  accSimple: %p\n", accSimple);
    // PTB: Do we need try/catch around the new?
    AccessibleRangeValueProvider* acc = new AccessibleRangeValueProvider(env, self);
    LOG("  acc: %p\n", acc);
        // Add this to the simple provider
        if (accSimple != 0) {
                AccessibleBaseProvider* accessibleSimple =
            reinterpret_cast<AccessibleBaseProvider *>(accSimple);
                if (accessibleSimple != NULL) {
                        accessibleSimple->AddPatternObject(reinterpret_cast<IUnknown*>(acc));
        } else {
                        LOG("  AddPatternObject not called; accessibleSimple is NULL.\n");
        }
        }
    return reinterpret_cast<jlong>(acc);
}

} /* extern "C" */
