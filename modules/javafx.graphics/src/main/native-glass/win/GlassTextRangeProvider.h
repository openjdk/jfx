/*
 * Copyright (c) 2014, 2026, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _GLASSTEXTRANGEPROVIDER_
#define _GLASSTEXTRANGEPROVIDER_

#include <UIAutomation.h>
#include "GlassAccessible.h"

class GlassTextRangeProvider : public ITextRangeProvider
{

public:
    /*
     * rangeId is the id Java assigns to the WinTextRangeProvider (glass_win_api.h, IDENTITY); 0 is
     * this library's "no Java peer" value, which the sibling-range guards test for. The range AddRefs
     * the accessible it belongs to, so a live range pins its provider.
     */
    GlassTextRangeProvider(int64_t rangeId, GlassAccessible* glassProvider);

    int64_t GetId() { return m_id; }

    /* glass_win_api.h's gwin_a11y_text_range_set_callbacks: by value, NULL slots become no-ops. */
    static void SetCallbacks(const GwinTextRangeCallbacks* cb);

    /* The installed table, or NULL when none is installed and every upcall site answers E_FAIL. */
    static const GwinTextRangeCallbacks* Callbacks();

    // IUnknown methods
    IFACEMETHODIMP_(ULONG) AddRef();
    IFACEMETHODIMP_(ULONG) Release();
    IFACEMETHODIMP QueryInterface(REFIID riid, void**);

    // ITextRangeProvider
    IFACEMETHODIMP Clone(ITextRangeProvider **pRetVal);
    IFACEMETHODIMP Compare(ITextRangeProvider *range, BOOL *pRetVal);
    IFACEMETHODIMP CompareEndpoints(TextPatternRangeEndpoint endpoint, ITextRangeProvider *targetRange,
                                    TextPatternRangeEndpoint targetEndpoint, int *pRetVal);
    IFACEMETHODIMP ExpandToEnclosingUnit(TextUnit unit);
    IFACEMETHODIMP FindAttribute(TEXTATTRIBUTEID attributeId, VARIANT val, BOOL backward, ITextRangeProvider **pRetVal);
    IFACEMETHODIMP FindText(BSTR text, BOOL backward, BOOL ignoreCase, ITextRangeProvider **pRetVal);
    IFACEMETHODIMP GetAttributeValue(TEXTATTRIBUTEID attributeId, VARIANT *pRetVal);
    IFACEMETHODIMP GetBoundingRectangles(SAFEARRAY * *pRetVal);
    IFACEMETHODIMP GetEnclosingElement(IRawElementProviderSimple **pRetVal);
    IFACEMETHODIMP GetText(int maxLength, BSTR *pRetVal);
    IFACEMETHODIMP Move(TextUnit unit, int count, int *pRetVal);
    IFACEMETHODIMP MoveEndpointByUnit(TextPatternRangeEndpoint endpoint, TextUnit unit, int count, int *pRetVal);
    IFACEMETHODIMP MoveEndpointByRange(TextPatternRangeEndpoint endpoint, ITextRangeProvider *targetRange,
                                       TextPatternRangeEndpoint targetEndpoint);
    IFACEMETHODIMP Select();
    IFACEMETHODIMP AddToSelection();
    IFACEMETHODIMP RemoveFromSelection();
    IFACEMETHODIMP ScrollIntoView(BOOL alignToTop);
    IFACEMETHODIMP GetChildren(SAFEARRAY * *pRetVal);


private:
    virtual ~GlassTextRangeProvider();

    ULONG m_refCount;
    int64_t m_id;                  // The Java-assigned id every callback slot carries; 0 = no Java peer
    GlassAccessible* m_glassAccessible;

};

#endif //_GLASSTEXTRANGEPROVIDER_
