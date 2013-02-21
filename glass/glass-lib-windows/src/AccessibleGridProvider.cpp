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
#include "AccessibleGridProvider.h"
#include "AccessibleBaseProvider.h"

static jmethodID midGetColumnCount;
static jmethodID midGetRowCount;
static jmethodID midGetItem;

/**
 * Constructor
 *
 * env:     JNI envrionment
 * self:    Java side caller
 */
AccessibleGridProvider::AccessibleGridProvider(JNIEnv* env, jobject self) : AccessibleBasePatternProvider(env, self) {
    //LOG("AccessibleGridProvider::ctor\n");
}

/**
 * Destructor
 */
AccessibleGridProvider::~AccessibleGridProvider() {
    //LOG("AccessibleGridProvider::dtor\n");
}

// IUnknown implementation.

/**
 * Increment the reference counter
 * 
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleGridProvider::AddRef() {
    //LOG("In AccessibleGridProvider::AddRef\n");
    //LOG("  this: %p\n", this);
    return AccessibleBasePatternProvider::AddRef();
}

/**
 * Decrement the reference counter
 *
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleGridProvider::Release() {
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
IFACEMETHODIMP AccessibleGridProvider::QueryInterface(REFIID riid, void** ppInterface) {
    //LOG("In AccessibleGridProvider::QueryInterface\n");
    //LOG("  this: %p\n", this);
    if (riid == __uuidof(IGridProvider)) {
        //LOG("  riid: IGridProvider\n");
        *ppInterface = static_cast<IGridProvider*>(this);
        //LOG("  Calling AddRef from AccessibleGridProvider::QI\n");
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
IFACEMETHODIMP AccessibleGridProvider::GetPatternProvider(PATTERNID patternId, IUnknown** pRetVal) {
    LOG("In IREPS AccessibleGridProvider::GetPatternProvider\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    LOG("  returning this\n");
    *pRetVal = static_cast<IGridProvider*>(this);
    AddRef();
    return S_OK;  
}

// AccessibleGridProvider implementation

/**
 * Get the ordinal number of the column that contains this cell or item
 *
 * pRetVal:                receiver of the state
 *
 * returns:     S_OK
 */
IFACEMETHODIMP AccessibleGridProvider::get_ColumnCount(int *pRetVal) {
    LOG("In AccessibleGridProvider::get_ColumnCount\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    *pRetVal = NULL;
    JNIEnv* env = GetEnv();
    jint value = env->CallIntMethod(m_self, midGetColumnCount);
    CheckAndClearException(env);  
    LOG("  returning %d\n", value);
        *pRetVal = value ;
    return S_OK;
}
 
IFACEMETHODIMP AccessibleGridProvider::get_RowCount(int *pRetVal) {
    LOG("In AccessibleGridProvider::get_RowCount\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    *pRetVal = NULL;
    JNIEnv* env = GetEnv();
    jint value = env->CallIntMethod(m_self, midGetRowCount);
        *pRetVal = value ;
    return S_OK;
}

IFACEMETHODIMP AccessibleGridProvider::GetItem(int row,int column,IRawElementProviderSimple **pRetVal) {
    LOG("In AccessibleGridProvider::get_GetItem\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    *pRetVal = NULL;
    JNIEnv* env = GetEnv();
    jlong acc = env->CallLongMethod(m_self, midGetItem, row, column);
    if (acc != NULL) {
        LOG("  returning: %p\n", acc);
        *pRetVal = reinterpret_cast<IRawElementProviderSimple *>(acc);
        reinterpret_cast<IUnknown*>(acc)->AddRef();
    } else {
        LOG("  returning NULL\n");
    }
    return S_OK;
}

////////////////////////////////////////
//
// Functions for JNI downcalls
//
////////////////////////////////////////

extern "C" {

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleGridProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleGridProvider__1initIDs(
    JNIEnv *env, jclass cls) {
    LOG("In WinAccessibleGridProvider._initIDs\n");
    midGetColumnCount = env->GetMethodID(cls, "getColumnCount", "()I");
    ASSERT(midGetColumnCount);
    midGetRowCount = env->GetMethodID(cls, "getRowCount", "()I");
    ASSERT(midGetRowCount);
        midGetItem = env->GetMethodID(cls, "getItem", "(II)J");
    ASSERT(midGetItem);
}

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleGridProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleGridProvider__1createAccessible(
    JNIEnv* env, jobject self, jlong accSimple) {
    LOG("In WinAccessibleGridProvider._createAccessible\n");
    LOG("  accSimple: %p\n", accSimple);
    // PTB: Do we need try/catch around the new?
    AccessibleGridProvider* acc = new AccessibleGridProvider(env, self);
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
