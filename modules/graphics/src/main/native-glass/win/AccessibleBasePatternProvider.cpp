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
#include "AccessibleBasePatternProvider.h"

// Need to define static member data; otherwise get "unresolved external symbol" from linker
LONG AccessibleBasePatternProviderChildIDFactory::sm_ChildID = 0;
std::vector<LONG> AccessibleBasePatternProviderChildIDFactory::sm_reusePool;

/**
 * Constructor
 *
 * env:     JNI envrionment
 * self:    Java side caller
 */
AccessibleBasePatternProvider::AccessibleBasePatternProvider(JNIEnv* env, jobject self) : m_refCount(1) {
    //LOG("AccessibleBasePatternProvider::ctor\n");
    m_self = env->NewGlobalRef(self);
}

/**
 * Destructor
 */
AccessibleBasePatternProvider::~AccessibleBasePatternProvider() {
    //LOG("AccessibleBasePatternProvider::dtor\n");
    JNIEnv* env = GetEnv();
    if (env) env->DeleteGlobalRef(m_self);
}

// IUnknown implementation.

/**
 * Increment the reference counter
 * 
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleBasePatternProvider::AddRef() {
    //LOG("In AccessibleBasePatternProvider::AddRef\n");
    return InterlockedIncrement(&m_refCount);
}

/**
 * Decrement the reference counter
 *
 * returns: the new reference count
 */
IFACEMETHODIMP_(ULONG) AccessibleBasePatternProvider::Release() {
    //LOG("In AccessibleBasePatternProvider::Release\n");
    long val = InterlockedDecrement(&m_refCount);
    if (val == 0) {
        AccessibleBasePatternProviderChildIDFactory::releaseChildID(m_id);
        //LOG("  ChildID: %d\n", m_id);
        delete this;
    }
    return val;
}

/**
 * Get an interface pointer (overrides base class implementation)
 *
 * riid:        interface ID
 * ppInterface: receiver of the reference to the interface
 *
 * returns:     S_OK, E_NOINTERFACE
 */
IFACEMETHODIMP AccessibleBasePatternProvider::QueryInterface(REFIID riid, void** ppInterface) {
    //LOG("In AccessibleBasePatternProvider::QueryInterface\n");
    //LOG("  AccessibleBasePatternProvider this: %p\n", this);
    if (riid == __uuidof(IUnknown)) {
        //LOG(" AccessibleBasePatternProvider riid: IUnknown\n");
        *ppInterface = static_cast<IUnknown*>(this);
    } else {
        //LOG(" AccessibleBasePatternProvider Unhandled riid \n");
        *ppInterface = NULL;
    }
    return E_NOINTERFACE;
}

/** 
 * Get a pattern provider
 *
 * patternID:   ID of the requested pattern
 * pRetvalue:   receiver of the pattern provider
 *
 * returns:     S_OK
 */
IFACEMETHODIMP AccessibleBasePatternProvider::GetPatternProvider(PATTERNID patternId, IUnknown** pRetVal) {
    LOG("In IREPS AccessibleBasePatternProvider::GetPatternProvider\n");
    LOG("  this: %p\n", this);
    if (pRetVal == NULL)
        return E_INVALIDARG;
    return S_OK;  
}


//////////////////////////////////////////////
//
// Methods for AccessibleBasePatternProviderChildIDFactory
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
AccessibleBasePatternProviderChildIDFactory::getChildID(void) {
    LOG("In AccessibleBasePatternProviderChildIDFactory::getChildID\n");
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
AccessibleBasePatternProviderChildIDFactory::releaseChildID(LONG id) {
    LOG("In AccessibleBasePatternProviderChildIDFactory::releaseChildID\n");
    sm_reusePool.push_back(id);
}

////////////////////////////////////////
//
// Functions for JNI downcalls
//
////////////////////////////////////////

extern "C" {

/**
 * Class:       com_sun_glass_ui_accessible_win_WinAccessibleBasePatternProvider
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
Java_com_sun_glass_ui_accessible_win_WinAccessibleBasePatternProvider__1destroyAccessible(
    JNIEnv* env, jobject self, jlong acc) {
    LOG("In downcall for WinAccessibleBasePatternProvider._destroyAccessible\n");
    LOG("  acc: %p\n", acc);
    if (acc) {
        AccessibleBasePatternProvider* accessible = reinterpret_cast<AccessibleBasePatternProvider*>(acc);
        accessible->Release();
    }
}

} /* extern "C" */
