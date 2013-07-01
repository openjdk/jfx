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

#ifndef _ACCESSIBLE_BASE_PROVIDER_
#define _ACCESSIBLE_BASE_PROVIDER_
#define MAX_PATTERNS 50

#include <UIAutomation.h>
#include <vector>
#include "AccessibleBasePatternProvider.h"

class AccessibleBaseProvider : public IRawElementProviderSimple, public IRawElementProviderFragment {

public:
    AccessibleBaseProvider(JNIEnv* env, jobject self/*, jint hwnd*/);

    // IUnknown methods
    IFACEMETHODIMP_(ULONG) AddRef();
    IFACEMETHODIMP_(ULONG) Release();
    IFACEMETHODIMP QueryInterface(REFIID riid, void**);

    // IRawElementProviderSimple properties and methods
    IFACEMETHODIMP get_HostRawElementProvider(IRawElementProviderSimple **pRetVal);
    IFACEMETHODIMP get_ProviderOptions(ProviderOptions *pRetVal);
    IFACEMETHODIMP GetPatternProvider(PATTERNID patternId, IUnknown **pRetVal);
    IFACEMETHODIMP GetPropertyValue(PROPERTYID propertyId, VARIANT *pRetVal);

    // IRawElementProviderFragment properties and methods
    IFACEMETHODIMP get_BoundingRectangle(UiaRect *pRetVal);
    IFACEMETHODIMP get_FragmentRoot(IRawElementProviderFragmentRoot **pRetVal);
    IFACEMETHODIMP GetEmbeddedFragmentRoots(SAFEARRAY **pRetVal);
    IFACEMETHODIMP GetRuntimeId(SAFEARRAY **pRetVal);
    IFACEMETHODIMP Navigate(NavigateDirection direction, IRawElementProviderFragment **pRetVal);
    IFACEMETHODIMP SetFocus();

    LONG m_id;  // A unique ID for this accessible 

        // helpers for accessing composition pattern object
        void AddPatternObject(IUnknown* native) ;
        IFACEMETHODIMP FindPatternObject(REFIID riid,void **ppInterface);
protected:
    // Only this object or its subclassed objects delete the object, thus the d'tor is protected
    virtual ~AccessibleBaseProvider();
    jobject m_self;     // The related Java side object
        //AccessibleBasePatternProvider *m_patternObjects[MAX_PATTERNS]; // Composition of pattern objects
        IUnknown *m_patternObjects[MAX_PATTERNS]; // Composition of pattern objects
        int m_patternObjectCnt; // Count of pattern objects
private:
    ULONG m_refCount;   // Ref Counter for this COM object

};

/**
 * Factory for creating ChildIDs
 *
 * There is a need for unique ChildIDs.  To implement this a 32 bit static variable counts sequentially
 * downward from -1.  The current value is used for a childID.  Each time an accessible is destructed, its
 * number is saved into a resuse pool. The sequential number is used whenever the reuse pool is empty.
 *
 */
class AccessibleBaseProviderChildIDFactory {
    private:
        static LONG sm_ChildID;
        static std::vector<LONG> sm_reusePool;
    public:
        static LONG getChildID(void);
        static void releaseChildID(LONG);
};

#endif
