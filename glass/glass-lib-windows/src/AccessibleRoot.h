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

#ifndef _ACCESSIBLE_ROOT_
#define _ACCESSIBLE_ROOT_

#include <UIAutomation.h>

class AccessibleRoot : public IRawElementProviderSimple, public IRawElementProviderFragment,
                       public IRawElementProviderFragmentRoot {

public:
    AccessibleRoot(JNIEnv* env, jobject self);

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

    // IRawElementProviderFragmentRoot methods
    IFACEMETHODIMP ElementProviderFromPoint(double x, double y, IRawElementProviderFragment **pRetVal);
    IFACEMETHODIMP GetFocus(IRawElementProviderFragment **pRetVal);

private:
    virtual ~AccessibleRoot();  // Only this object deletes itself, thus the d'tor is private
    ULONG m_refCount;  // Ref Counter for this COM object
    jobject m_self;  // The related Java side object

};

#endif
