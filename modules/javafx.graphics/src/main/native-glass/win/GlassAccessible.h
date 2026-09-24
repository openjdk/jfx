/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _GLASSACCESSIBLE_
#define _GLASSACCESSIBLE_

#include <UIAutomation.h>
#include "glass_win_api.h"

class GlassAccessible : public IRawElementProviderSimple,
                        public IRawElementProviderFragment,
                        public IRawElementProviderFragmentRoot,
                        public IRawElementProviderAdviseEvents,
                        public IInvokeProvider,
                        public ISelectionProvider,
                        public ISelectionItemProvider,
                        public IRangeValueProvider,
                        public IValueProvider,
                        public ITextProvider,
                        public IGridProvider,
                        public IGridItemProvider,
                        public ITableProvider,
                        public ITableItemProvider,
                        public IToggleProvider,
                        public IExpandCollapseProvider,
                        public ITransformProvider,
                        public IScrollProvider,
                        public IScrollItemProvider
{

public:
    /*
     * accessibleId is the id Java assigns to the WinAccessible (glass_win_api.h, IDENTITY). 0 is this
     * library's "no Java peer" value and is what the sibling-range guards test for.
     */
    GlassAccessible(int64_t accessibleId);

    int64_t GetId() { return m_id; }

    /* glass_win_api.h's gwin_a11y_set_callbacks: by value, NULL slots become no-ops, NULL clears. */
    static void SetCallbacks(const GwinAccessibleCallbacks* cb);

    /* The installed table, or NULL when none is installed and every upcall site answers E_FAIL. */
    static const GwinAccessibleCallbacks* Callbacks();

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

    // IRawElementProviderAdviseEvents
    IFACEMETHODIMP AdviseEventAdded(EVENTID eventId, SAFEARRAY *propertyIDs);
    IFACEMETHODIMP AdviseEventRemoved(EVENTID eventId, SAFEARRAY *propertyIDs);

    // IInvokeProvider
    IFACEMETHODIMP Invoke();

    // ISelectionProvider
    IFACEMETHODIMP GetSelection(SAFEARRAY **pRetVal);
    IFACEMETHODIMP get_CanSelectMultiple(BOOL *pRetVal);
    IFACEMETHODIMP get_IsSelectionRequired(BOOL *pRetVal);

    // ISelectionItemProvider
    IFACEMETHODIMP Select();
    IFACEMETHODIMP AddToSelection();
    IFACEMETHODIMP RemoveFromSelection();
    IFACEMETHODIMP get_IsSelected(BOOL *pRetVal);
    IFACEMETHODIMP get_SelectionContainer(IRawElementProviderSimple **pRetVal);

    // IRangeValueProvider
    IFACEMETHODIMP SetValue(double val);
    IFACEMETHODIMP get_Value(double *pRetVal);
    IFACEMETHODIMP get_IsReadOnly(BOOL *pRetVal);
    IFACEMETHODIMP get_Maximum(double *pRetVal);
    IFACEMETHODIMP get_Minimum(double *pRetVal);
    IFACEMETHODIMP get_LargeChange(double *pRetVal);
    IFACEMETHODIMP get_SmallChange(double *pRetVal);

    // IValueProvider
    IFACEMETHODIMP SetValue(LPCWSTR val);
    IFACEMETHODIMP get_Value(BSTR *pRetVal);
//    IFACEMETHODIMP get_IsReadOnly(BOOL *pRetVal); /* Defined in IRangeValueProvider */

    // ITextProvider
//    IFACEMETHODIMP GetSelection(SAFEARRAY **pRetVal); /* Defined in ISelectionProvider */
    IFACEMETHODIMP GetVisibleRanges(SAFEARRAY **pRetVal);
    IFACEMETHODIMP RangeFromChild(IRawElementProviderSimple *childElement,  ITextRangeProvider **pRetVal);
    IFACEMETHODIMP RangeFromPoint(UiaPoint point, ITextRangeProvider **pRetVal);
    IFACEMETHODIMP get_DocumentRange(ITextRangeProvider **pRetVal);
    IFACEMETHODIMP get_SupportedTextSelection(SupportedTextSelection *pRetVal);

    // IGridProvider
    IFACEMETHODIMP get_ColumnCount(int *pRetVal);
    IFACEMETHODIMP get_RowCount(int *pRetVal);
    IFACEMETHODIMP GetItem(int row, int column, IRawElementProviderSimple **pRetVal);

    // IGridItemProvider
    IFACEMETHODIMP get_Column(int *pRetVal);
    IFACEMETHODIMP get_ColumnSpan(int *pRetVal);
    IFACEMETHODIMP get_ContainingGrid(IRawElementProviderSimple **pRetVal);
    IFACEMETHODIMP get_Row(int *pRetVal);
    IFACEMETHODIMP get_RowSpan(int *pRetVal);

    // ITableProvider
    IFACEMETHODIMP GetColumnHeaders(SAFEARRAY **pRetVal);
    IFACEMETHODIMP GetRowHeaders(SAFEARRAY **pRetVal);
    IFACEMETHODIMP get_RowOrColumnMajor(RowOrColumnMajor *pRetVal);

    // ITableItemProvider
    IFACEMETHODIMP GetColumnHeaderItems(SAFEARRAY **pRetVal);
    IFACEMETHODIMP GetRowHeaderItems(SAFEARRAY **pRetVal);

    // IToggleProvider
    IFACEMETHODIMP Toggle();
    IFACEMETHODIMP get_ToggleState(ToggleState *pRetVal);

    // IExpandCollapseProvider
    IFACEMETHODIMP Collapse();
    IFACEMETHODIMP Expand();
    IFACEMETHODIMP get_ExpandCollapseState(ExpandCollapseState *pRetVal);

    // ITransformProvider
    IFACEMETHODIMP get_CanMove(BOOL *pRetVal);
    IFACEMETHODIMP get_CanResize(BOOL *pRetVal);
    IFACEMETHODIMP get_CanRotate(BOOL *pRetVal);
    IFACEMETHODIMP Move(double x, double y);
    IFACEMETHODIMP Resize(double width, double height);
    IFACEMETHODIMP Rotate(double degrees);

    // IScrollProvider
    IFACEMETHODIMP Scroll(ScrollAmount horizontalAmount, ScrollAmount verticalAmount);
    IFACEMETHODIMP SetScrollPercent(double horizontalPercent, double verticalPercent);
    IFACEMETHODIMP get_HorizontallyScrollable(BOOL *pRetVal);
    IFACEMETHODIMP get_HorizontalScrollPercent(double *pRetVal);
    IFACEMETHODIMP get_HorizontalViewSize(double *pRetVal);
    IFACEMETHODIMP get_VerticallyScrollable(BOOL *pRetVal);
    IFACEMETHODIMP get_VerticalScrollPercent(double *pRetVal);
    IFACEMETHODIMP get_VerticalViewSize(double *pRetVal);

    // IScrollItemProvider
    IFACEMETHODIMP ScrollIntoView();

    /*
     * The callback-table forms of GlassAccessible::copyString / copyList / copyVariant as commit
     * 033187ad90 had them: same steps, same failure codes, a gwin_alloc-ed block instead of a jarray /
     * jstring. Each RELEASES the block it was given with gwin_free on every path - a slot hands its
     * block over (glass_win_api.h, OWNERSHIP) - and treats a NULL block as the null the Java target
     * returned, i.e. E_FAIL. A status other than GWIN_OK is E_FAIL with pRetVal untouched, which is
     * where the JNI arm returned after CheckAndClearException.
     */
    static HRESULT copyBlockString(int32_t status, uint16_t* block, int32_t len, BSTR* pbstrVal);
    static HRESULT copyBlockList(int32_t status, void* block, int32_t count, SAFEARRAY** pparrayVal,
                                 VARTYPE vt);

    /*
     * GwinVariant -> VARIANT, copyVariant's switch over a flat struct. takeOwnership frees the
     * variant's two blocks with gwin_free (the out-parameter direction: a slot handed them over);
     * false leaves them to the caller (the gwin_a11y_raise_property_changed direction).
     */
    static HRESULT variantFromGwin(const GwinVariant* variant, VARIANT* pRetVal, bool takeOwnership);

    /* GWIN_OK -> S_OK, anything else -> the E_FAIL every JNI arm returned for a pending Throwable. */
    static HRESULT statusToHr(int32_t status);

    /*
     * The GlassAccessible* a slot handed back, AddRefed as GlassAccessible::callLongMethod of commit
     * 033187ad90 AddRefed it - and NULL when the slot failed, where callLongMethod returned before
     * touching the caller's pointer.
     */
    static GlassAccessible* takeAccessible(int32_t status, int64_t value);

private:
    virtual ~GlassAccessible();

    /* The copyBlockString / copyBlockList steps without the gwin_free, for the GwinVariant fields,
     * whose blocks are released once for the whole struct. A NULL buffer is E_FAIL, as it was. */
    static HRESULT copyRawString(const uint16_t* text, int32_t len, BSTR* pbstrVal);
    static HRESULT copyRawList(const void* data, int32_t count, SAFEARRAY** pparrayVal, VARTYPE vt);

    /*
     * The three slot shapes that repeat: no out-parameter, one int32 out-parameter (BOOL and the UIA
     * enums are int-sized, hence the template) and one double out-parameter. Each pre-zeroes the
     * out-parameter and writes it BEFORE it looks at the status, because the JNI arm wrote *pRetVal
     * before it checked for a pending exception.
     */
    HRESULT slotVoid(int32_t (*slot)(int64_t))
    {
        return statusToHr(slot(m_id));
    }

    template <typename T> HRESULT slotInt(int32_t (*slot)(int64_t, int32_t*), T* pRetVal)
    {
        int32_t value = 0;
        int32_t status = slot(m_id, &value);
        *pRetVal = (T) value;
        return statusToHr(status);
    }

    HRESULT slotDouble(int32_t (*slot)(int64_t, double*), double* pRetVal)
    {
        double value = 0.0;
        int32_t status = slot(m_id, &value);
        *pRetVal = value;
        return statusToHr(status);
    }

    ULONG m_refCount;
    int64_t m_id;           // The Java-assigned id every callback slot carries; 0 = no Java peer

};

#endif //_GLASSACCESSIBLE_
