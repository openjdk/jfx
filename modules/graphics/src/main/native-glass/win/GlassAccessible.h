/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

class GlassAccessible : public IRawElementProviderSimple,
                        public IRawElementProviderFragment,
                        public IRawElementProviderFragmentRoot,
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
    GlassAccessible(JNIEnv* env, jobject jAccessible);

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

    static void copyVariant(JNIEnv *env, jobject jVariant, VARIANT* pRetVal);

private:
    virtual ~GlassAccessible();

    /* Call the method specified by 'mid', AddRef the returning ptr (expects result to be IUnkonwn) */
    virtual jlong callLongMethod(jmethodID mid, ...);

    /* Call the method specified by 'mid' and converts the returning jarray to a SAFEARRAY */
    virtual SAFEARRAY* callArrayMethod(jmethodID mid, VARTYPE vt);

    ULONG m_refCount;
    jobject m_jAccessible;  // The GlobalRef Java side object

};

#endif //_GLASSACCESSIBLE_
