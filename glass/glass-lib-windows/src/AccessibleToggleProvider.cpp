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
#include "AccessibleToggleProvider.h"
#include "AccessibleBaseProvider.h"

static jmethodID midGetToggleState;

/**
 * Constructor
 *
 * env:     JNI envrionment
 * self:    Java side caller
 */
AccessibleToggleProvider::AccessibleToggleProvider(JNIEnv* env, jobject self) : AccessibleBasePatternProvider(env, self) {
    //LOG("AccessibleToggleProvider::ctor\n");
}

/**
 * Destructor
 */
AccessibleToggleProvider::~AccessibleToggleProvider() {
    //LOG("AccessibleToggleProvider::dtor\n");
}

// IUnknown implementation.

/**
 * Increment the reference counter
 * 
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleToggleProvider::AddRef() {
    //LOG("In AccessibleToggleProvider::AddRef\n");
    //LOG("  this: %p\n", this);
    return AccessibleBasePatternProvider::AddRef();
}

/**
 * Decrement the reference counter
 *
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleToggleProvider::Release() {
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
IFACEMETHODIMP AccessibleToggleProvider::QueryInterface(REFIID riid, void** ppInterface) {
    //LOG("In AccessibleToggleProvider::QueryInterface\n");
    //LOG("  this: %p\n", this);
    if (riid == __uuidof(IToggleProvider)) {
        //LOG("  riid: IToggleProvider\n");
        *ppInterface = static_cast<IToggleProvider*>(this);
        //LOG("  Calling AddRef from AccessibleToggleProvider::QI\n");
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
IFACEMETHODIMP AccessibleToggleProvider::GetPatternProvider(PATTERNID patternId, IUnknown** pRetVal) {
    LOG("In IREPS AccessibleToggleProvider::GetPatternProvider\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    LOG("  returning this\n");
    *pRetVal = static_cast<IToggleProvider*>(this);
    AccessibleBasePatternProvider::AddRef();
    return S_OK;  
}

// IToggleProvider implementation

/**
 * Get the state of the control.
 *
 * pRetVal:                receiver of the state
 *
 * returns:     S_OK
 */
IFACEMETHODIMP AccessibleToggleProvider::get_ToggleState(ToggleState* pRetVal) {
    LOG("In AccessibleToggleProvider::get_ToggleState\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    JNIEnv* env = GetEnv();
    jint state = env->CallIntMethod(m_self, midGetToggleState);
    switch (state) {
    case 0:
        *pRetVal = ToggleState_Off;
        LOG("  returning ToggleState_Off\n");
        break;
    case 1:
        *pRetVal = ToggleState_On;
        LOG("  returning ToggleState_On\n");
        break;
    default:
        *pRetVal = ToggleState_Indeterminate;
        LOG("  returning ToggleState_Indeterminate\n");
    }
    return S_OK;
}

/**
 * Toggle the state of the control
 *
 * returns:        S_OK
 */
IFACEMETHODIMP AccessibleToggleProvider::Toggle() {
    LOG("In AccessibleToggleProvider::Toggle\n");
    LOG("  this: %p\n", this);
    LOG("  NOT IMPLEMENTED\n");
    return E_NOTIMPL;
}

////////////////////////////////////////
//
// Functions for JNI downcalls
//
////////////////////////////////////////

extern "C" {

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleToggleProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleToggleProvider__1initIDs(
    JNIEnv *env, jclass cls) {
    LOG("In WinAccessibleToggleProvider._initIDs\n");
    midGetToggleState = env->GetMethodID(cls, "getToggleState", "()I");
    ASSERT(midGetToggleState);
}

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleToggleProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleToggleProvider__1createAccessible(
    JNIEnv* env, jobject self, jlong accSimple) {
    LOG("In WinAccessibleToggleProvider._createAccessible\n");
    LOG("  accSimple: %p\n", accSimple);
    // PTB: Do we need try/catch around the new?
    AccessibleToggleProvider* acc = new AccessibleToggleProvider(env, self);
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

