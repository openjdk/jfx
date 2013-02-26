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

#ifndef _ACCESSIBLE_BASE_PATTERN_PROVIDER_
#define _ACCESSIBLE_BASE_PATTERN_PROVIDER_

#include <UIAutomation.h>
#include <vector>

class AccessibleBasePatternProvider: public IUnknown {

public:
    AccessibleBasePatternProvider(JNIEnv* env, jobject self);
    // IUnknown methods
    IFACEMETHODIMP_(ULONG) AddRef();
    IFACEMETHODIMP_(ULONG) Release();
    IFACEMETHODIMP QueryInterface(REFIID riid, void**);
    IFACEMETHODIMP GetPatternProvider(PATTERNID patternId, IUnknown **pRetVal);
    LONG m_id;  // A unique ID for this accessible 
protected:
    virtual ~AccessibleBasePatternProvider();  // Only this object deletes itself, thus the d'tor is private         

protected:
        jobject m_self;     // The related Java side object
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
class AccessibleBasePatternProviderChildIDFactory {
    private:
        static LONG sm_ChildID;
        static std::vector<LONG> sm_reusePool;
    public:
        static LONG getChildID(void);
        static void releaseChildID(LONG);
};


#endif
