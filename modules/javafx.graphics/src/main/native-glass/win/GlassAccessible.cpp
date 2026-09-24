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


#include "common.h"
#include "GlassAccessible.h"
#include "GlassTextRangeProvider.h"
#include "GlassApplication.h"


/*
 * ---- The callback table of glass_win_api.h's accessibility section ----
 *
 * Installed by gwin_a11y_set_callbacks. A slot Java leaves NULL is replaced by the matching no-op
 * below, so the upcall sites never test a slot; what they test is Callbacks() returning NULL, which
 * means "no table installed" and leaves them no way to reach Java, so they answer the E_FAIL that the
 * JNI arm of commit 033187ad90 answered when GetEnv() gave it no JNIEnv. Every no-op answers GWIN_OK
 * with zeroed out-parameters, which is what a cleared JNI exception looked like to this file.
 */
namespace {

int32_t NoopVoid(int64_t) { return GWIN_OK; }
int32_t NoopOutInt(int64_t, int32_t* out) { *out = 0; return GWIN_OK; }
int32_t NoopOutDouble(int64_t, double* out) { *out = 0.0; return GWIN_OK; }
int32_t NoopOutLong(int64_t, int64_t* out) { *out = 0; return GWIN_OK; }
int32_t NoopIntOutLong(int64_t, int32_t, int64_t* out) { *out = 0; return GWIN_OK; }
int32_t NoopIntIntOutLong(int64_t, int32_t, int32_t, int64_t* out) { *out = 0; return GWIN_OK; }
int32_t NoopLongOutLong(int64_t, int64_t, int64_t* out) { *out = 0; return GWIN_OK; }
int32_t NoopDoubleDoubleOutLong(int64_t, double, double, int64_t* out) { *out = 0; return GWIN_OK; }
int32_t NoopAdvise(int64_t, int32_t, int64_t) { return GWIN_OK; }
int32_t NoopSetDouble(int64_t, double) { return GWIN_OK; }
int32_t NoopSetDoubleDouble(int64_t, double, double) { return GWIN_OK; }
int32_t NoopSetIntInt(int64_t, int32_t, int32_t) { return GWIN_OK; }
int32_t NoopSetString(int64_t, const uint16_t*, int32_t) { return GWIN_OK; }
int32_t NoopOutStringBlock(int64_t, uint16_t** out, int32_t* out_len)
{
    *out = NULL;
    *out_len = 0;
    return GWIN_OK;
}
int32_t NoopOutLongBlock(int64_t, int64_t** out, int32_t* out_count)
{
    *out = NULL;
    *out_count = 0;
    return GWIN_OK;
}
int32_t NoopOutIntBlock(int64_t, int32_t** out, int32_t* out_count)
{
    *out = NULL;
    *out_count = 0;
    return GWIN_OK;
}
int32_t NoopOutVariant(int64_t, int32_t, GwinVariant* out)
{
    memset(out, 0, sizeof(GwinVariant));
    return GWIN_OK;
}
int32_t NoopOutBounds(int64_t, float* out4, int32_t* out_written)
{
    out4[0] = out4[1] = out4[2] = out4[3] = 0.0f;
    *out_written = 0;
    return GWIN_OK;
}
void NoopDisposed(int64_t) {}

/* Member-by-member, not a positional initializer: with 70 same-shaped slots a transposition would
 * compile silently. */
void ResetToNoops(GwinAccessibleCallbacks& t)
{
    t.get_pattern_provider = NoopIntOutLong;
    t.get_host_raw_element_provider = NoopOutLong;
    t.get_property_value = NoopOutVariant;
    t.get_bounding_rectangle = NoopOutBounds;
    t.get_fragment_root = NoopOutLong;
    t.get_embedded_fragment_roots = NoopOutLongBlock;
    t.get_runtime_id = NoopOutIntBlock;
    t.navigate = NoopIntOutLong;
    t.set_focus = NoopVoid;
    t.element_provider_from_point = NoopDoubleDoubleOutLong;
    t.get_focus = NoopOutLong;
    t.advise_event_added = NoopAdvise;
    t.advise_event_removed = NoopAdvise;
    t.invoke = NoopVoid;
    t.get_selection = NoopOutLongBlock;
    t.get_can_select_multiple = NoopOutInt;
    t.get_is_selection_required = NoopOutInt;
    t.select = NoopVoid;
    t.add_to_selection = NoopVoid;
    t.remove_from_selection = NoopVoid;
    t.get_is_selected = NoopOutInt;
    t.get_selection_container = NoopOutLong;
    t.set_value = NoopSetDouble;
    t.get_value = NoopOutDouble;
    t.get_is_read_only = NoopOutInt;
    t.get_maximum = NoopOutDouble;
    t.get_minimum = NoopOutDouble;
    t.get_large_change = NoopOutDouble;
    t.get_small_change = NoopOutDouble;
    t.set_value_string = NoopSetString;
    t.get_value_string = NoopOutStringBlock;
    t.get_visible_ranges = NoopOutLongBlock;
    t.range_from_child = NoopLongOutLong;
    t.range_from_point = NoopDoubleDoubleOutLong;
    t.get_document_range = NoopOutLong;
    t.get_supported_text_selection = NoopOutInt;
    t.get_column_count = NoopOutInt;
    t.get_row_count = NoopOutInt;
    t.get_item = NoopIntIntOutLong;
    t.get_column = NoopOutInt;
    t.get_column_span = NoopOutInt;
    t.get_containing_grid = NoopOutLong;
    t.get_row = NoopOutInt;
    t.get_row_span = NoopOutInt;
    t.get_column_headers = NoopOutLongBlock;
    t.get_row_headers = NoopOutLongBlock;
    t.get_row_or_column_major = NoopOutInt;
    t.get_column_header_items = NoopOutLongBlock;
    t.get_row_header_items = NoopOutLongBlock;
    t.toggle = NoopVoid;
    t.get_toggle_state = NoopOutInt;
    t.collapse = NoopVoid;
    t.expand = NoopVoid;
    t.get_expand_collapse_state = NoopOutInt;
    t.get_can_move = NoopOutInt;
    t.get_can_resize = NoopOutInt;
    t.get_can_rotate = NoopOutInt;
    t.move = NoopSetDoubleDouble;
    t.resize = NoopSetDoubleDouble;
    t.rotate = NoopSetDouble;
    t.scroll = NoopSetIntInt;
    t.set_scroll_percent = NoopSetDoubleDouble;
    t.get_horizontally_scrollable = NoopOutInt;
    t.get_horizontal_scroll_percent = NoopOutDouble;
    t.get_horizontal_view_size = NoopOutDouble;
    t.get_vertically_scrollable = NoopOutInt;
    t.get_vertical_scroll_percent = NoopOutDouble;
    t.get_vertical_view_size = NoopOutDouble;
    t.scroll_into_view = NoopVoid;
    t.accessible_disposed = NoopDisposed;
}

GwinAccessibleCallbacks s_accessibleCallbacks;
bool s_accessibleCallbacksInstalled = false;

} // namespace

/* By value - this library never retains the caller's struct - and a NULL slot keeps its no-op.
 * Installed once, before any provider can exist; not safe against a concurrent install - the flag is a
 * plain bool and the table a plain struct assignment, with no release store between them. */
void GlassAccessible::SetCallbacks(const GwinAccessibleCallbacks* cb)
{
    GwinAccessibleCallbacks t;
    ResetToNoops(t);
    if (cb != NULL) {
        if (cb->get_pattern_provider) t.get_pattern_provider = cb->get_pattern_provider;
        if (cb->get_host_raw_element_provider) t.get_host_raw_element_provider = cb->get_host_raw_element_provider;
        if (cb->get_property_value) t.get_property_value = cb->get_property_value;
        if (cb->get_bounding_rectangle) t.get_bounding_rectangle = cb->get_bounding_rectangle;
        if (cb->get_fragment_root) t.get_fragment_root = cb->get_fragment_root;
        if (cb->get_embedded_fragment_roots) t.get_embedded_fragment_roots = cb->get_embedded_fragment_roots;
        if (cb->get_runtime_id) t.get_runtime_id = cb->get_runtime_id;
        if (cb->navigate) t.navigate = cb->navigate;
        if (cb->set_focus) t.set_focus = cb->set_focus;
        if (cb->element_provider_from_point) t.element_provider_from_point = cb->element_provider_from_point;
        if (cb->get_focus) t.get_focus = cb->get_focus;
        if (cb->advise_event_added) t.advise_event_added = cb->advise_event_added;
        if (cb->advise_event_removed) t.advise_event_removed = cb->advise_event_removed;
        if (cb->invoke) t.invoke = cb->invoke;
        if (cb->get_selection) t.get_selection = cb->get_selection;
        if (cb->get_can_select_multiple) t.get_can_select_multiple = cb->get_can_select_multiple;
        if (cb->get_is_selection_required) t.get_is_selection_required = cb->get_is_selection_required;
        if (cb->select) t.select = cb->select;
        if (cb->add_to_selection) t.add_to_selection = cb->add_to_selection;
        if (cb->remove_from_selection) t.remove_from_selection = cb->remove_from_selection;
        if (cb->get_is_selected) t.get_is_selected = cb->get_is_selected;
        if (cb->get_selection_container) t.get_selection_container = cb->get_selection_container;
        if (cb->set_value) t.set_value = cb->set_value;
        if (cb->get_value) t.get_value = cb->get_value;
        if (cb->get_is_read_only) t.get_is_read_only = cb->get_is_read_only;
        if (cb->get_maximum) t.get_maximum = cb->get_maximum;
        if (cb->get_minimum) t.get_minimum = cb->get_minimum;
        if (cb->get_large_change) t.get_large_change = cb->get_large_change;
        if (cb->get_small_change) t.get_small_change = cb->get_small_change;
        if (cb->set_value_string) t.set_value_string = cb->set_value_string;
        if (cb->get_value_string) t.get_value_string = cb->get_value_string;
        if (cb->get_visible_ranges) t.get_visible_ranges = cb->get_visible_ranges;
        if (cb->range_from_child) t.range_from_child = cb->range_from_child;
        if (cb->range_from_point) t.range_from_point = cb->range_from_point;
        if (cb->get_document_range) t.get_document_range = cb->get_document_range;
        if (cb->get_supported_text_selection) t.get_supported_text_selection = cb->get_supported_text_selection;
        if (cb->get_column_count) t.get_column_count = cb->get_column_count;
        if (cb->get_row_count) t.get_row_count = cb->get_row_count;
        if (cb->get_item) t.get_item = cb->get_item;
        if (cb->get_column) t.get_column = cb->get_column;
        if (cb->get_column_span) t.get_column_span = cb->get_column_span;
        if (cb->get_containing_grid) t.get_containing_grid = cb->get_containing_grid;
        if (cb->get_row) t.get_row = cb->get_row;
        if (cb->get_row_span) t.get_row_span = cb->get_row_span;
        if (cb->get_column_headers) t.get_column_headers = cb->get_column_headers;
        if (cb->get_row_headers) t.get_row_headers = cb->get_row_headers;
        if (cb->get_row_or_column_major) t.get_row_or_column_major = cb->get_row_or_column_major;
        if (cb->get_column_header_items) t.get_column_header_items = cb->get_column_header_items;
        if (cb->get_row_header_items) t.get_row_header_items = cb->get_row_header_items;
        if (cb->toggle) t.toggle = cb->toggle;
        if (cb->get_toggle_state) t.get_toggle_state = cb->get_toggle_state;
        if (cb->collapse) t.collapse = cb->collapse;
        if (cb->expand) t.expand = cb->expand;
        if (cb->get_expand_collapse_state) t.get_expand_collapse_state = cb->get_expand_collapse_state;
        if (cb->get_can_move) t.get_can_move = cb->get_can_move;
        if (cb->get_can_resize) t.get_can_resize = cb->get_can_resize;
        if (cb->get_can_rotate) t.get_can_rotate = cb->get_can_rotate;
        if (cb->move) t.move = cb->move;
        if (cb->resize) t.resize = cb->resize;
        if (cb->rotate) t.rotate = cb->rotate;
        if (cb->scroll) t.scroll = cb->scroll;
        if (cb->set_scroll_percent) t.set_scroll_percent = cb->set_scroll_percent;
        if (cb->get_horizontally_scrollable) t.get_horizontally_scrollable = cb->get_horizontally_scrollable;
        if (cb->get_horizontal_scroll_percent) t.get_horizontal_scroll_percent = cb->get_horizontal_scroll_percent;
        if (cb->get_horizontal_view_size) t.get_horizontal_view_size = cb->get_horizontal_view_size;
        if (cb->get_vertically_scrollable) t.get_vertically_scrollable = cb->get_vertically_scrollable;
        if (cb->get_vertical_scroll_percent) t.get_vertical_scroll_percent = cb->get_vertical_scroll_percent;
        if (cb->get_vertical_view_size) t.get_vertical_view_size = cb->get_vertical_view_size;
        if (cb->scroll_into_view) t.scroll_into_view = cb->scroll_into_view;
        if (cb->accessible_disposed) t.accessible_disposed = cb->accessible_disposed;
    }
    s_accessibleCallbacksInstalled = false;
    s_accessibleCallbacks = t;
    s_accessibleCallbacksInstalled = cb != NULL;
}

/* static */ const GwinAccessibleCallbacks* GlassAccessible::Callbacks()
{
    return s_accessibleCallbacksInstalled ? &s_accessibleCallbacks : NULL;
}

/* static */ HRESULT GlassAccessible::statusToHr(int32_t status)
{
    return status == GWIN_OK ? S_OK : E_FAIL;
}

/* static */ GlassAccessible* GlassAccessible::takeAccessible(int32_t status, int64_t value)
{
    if (status != GWIN_OK) {
        /* GlassAccessible::callLongMethod of commit 033187ad90 returned before it touched the caller's
         * pointer. */
        return NULL;
    }
    GlassAccessible* ga = reinterpret_cast<GlassAccessible*>(value);
    if (ga) ga->AddRef();
    return ga;
}

/* static */ HRESULT GlassAccessible::copyRawString(const uint16_t* text, int32_t len, BSTR* pbstrVal)
{
    if (pbstrVal != NULL && text != NULL && len >= 0) {
        /* SysAllocStringLen is the one step GlassAccessible::copyString of commit 033187ad90 took once it
         * held the code units; its NULL return was not checked there either. */
        *pbstrVal = SysAllocStringLen(reinterpret_cast<const OLECHAR*>(text), (UINT)len);
        return S_OK;
    }
    return E_FAIL;
}

/* static */ HRESULT GlassAccessible::copyRawList(const void* data, int32_t count,
                                                  SAFEARRAY** pparrayVal, VARTYPE vt)
{
    if (data != NULL && count >= 0) {
        SAFEARRAY *psa = SafeArrayCreateVector(vt, 0, count);
        if (psa) {
            const int32_t* intPtr = (const int32_t*)data;
            const int64_t* longPtr = (const int64_t*)data;
            const double* doublePtr = (const double*)data;
            for (LONG i = 0; i < count; i++) {
                if (vt == VT_UNKNOWN) {
                    /* SafeArrayPutElement AddRefs the IUnknown, as it did for the long[] elements. */
                    SafeArrayPutElement(psa, &i, (void*)longPtr[i]);
                } else if (vt == VT_I4) {
                    SafeArrayPutElement(psa, &i, (void*)&(intPtr[i]));
                } else if (vt == VT_R8) {
                    SafeArrayPutElement(psa, &i, (void*)&(doublePtr[i]));
                }
            }
            *pparrayVal = psa;
            return S_OK;
        }
    }
    return E_FAIL;
}

/* static */ HRESULT GlassAccessible::copyBlockString(int32_t status, uint16_t* block, int32_t len,
                                                      BSTR* pbstrVal)
{
    HRESULT hr = status == GWIN_OK ? copyRawString(block, len, pbstrVal) : E_FAIL;
    gwin_free(block);
    return hr;
}

/* static */ HRESULT GlassAccessible::copyBlockList(int32_t status, void* block, int32_t count,
                                                    SAFEARRAY** pparrayVal, VARTYPE vt)
{
    HRESULT hr = status == GWIN_OK ? copyRawList(block, count, pparrayVal, vt) : E_FAIL;
    gwin_free(block);
    return hr;
}

/* static */ HRESULT GlassAccessible::variantFromGwin(const GwinVariant* variant, VARIANT* pRetVal,
                                                      bool takeOwnership)
{
    if (pRetVal == NULL) return E_FAIL;
    if (variant == NULL) {
        pRetVal->vt = VT_EMPTY;
        return E_FAIL;
    }
    HRESULT hr = S_OK;
    pRetVal->vt = (VARTYPE)variant->vt;
    switch (pRetVal->vt) {
        case VT_EMPTY:
            /*
             * Java had nothing. In the out-parameter direction (get_property_value /
             * get_attribute_value, takeOwnership true) that is the null GlassAccessible::copyVariant
             * answered E_FAIL for at commit 033187ad90, and GetPropertyValue answers null for every
             * property WinAccessible does not handle. In the caller-owned direction
             * (gwin_a11y_raise_property_changed) copyVariant saw a WinVariant object whose vt was 0,
             * fell through its switch and answered S_OK, so that direction keeps answering S_OK.
             */
            if (takeOwnership) hr = E_FAIL;
            break;
        case VT_I2:
            pRetVal->iVal = variant->i_val;
            break;
        case VT_I4:
            pRetVal->lVal = variant->l_val;
            break;
        case VT_UNKNOWN:
            pRetVal->punkVal = reinterpret_cast<IUnknown*>(variant->punk_val);
            if (pRetVal->punkVal != NULL) {
                pRetVal->punkVal->AddRef();
            } else {
                hr = E_FAIL;
            }
            break;
        case VT_R4:
            pRetVal->fltVal = variant->flt_val;
            break;
        case VT_R8:
            pRetVal->dblVal = variant->dbl_val;
            break;
        case VT_BOOL:
            pRetVal->boolVal = variant->bool_val ? VARIANT_TRUE : VARIANT_FALSE;
            break;
        case VT_BSTR:
            hr = copyRawString(variant->bstr_val, variant->bstr_len, &(pRetVal->bstrVal));
            break;
        case VT_R8 | VT_ARRAY:
            hr = copyRawList(variant->p_dbl_val, variant->p_dbl_count, &(pRetVal->parray), VT_R8);
            break;
    }
    if (takeOwnership) {
        /* Unconditionally, whatever vt says: a block Java allocated is this library's to release, and
         * gwin_free(NULL) is a no-op. */
        gwin_free((void*)variant->bstr_val);
        gwin_free((void*)variant->p_dbl_val);
    }
    if (FAILED(hr)) pRetVal->vt = VT_EMPTY;
    return hr;
}

GlassAccessible::GlassAccessible(int64_t accessibleId)
: m_refCount(1), m_id(accessibleId)
{
    GlassApplication::IncrementAccessibility();
}

GlassAccessible::~GlassAccessible()
{
    const GwinAccessibleCallbacks* cb = Callbacks();
    if (cb != NULL) {
        /*
         * Where DeleteGlobalRef stood at commit 033187ad90: the LAST COM reference has gone, which is
         * the only moment Java may drop its registry entry for m_id - not WinAccessible.dispose(),
         * which UIA calls keep arriving after. It can arrive on the thread that dropped that
         * reference, which for a UIA-held reference is a COM/RPC thread; DeleteGlobalRef leaked there,
         * because GetEnv() answered NULL on a thread the JVM had never seen.
         */
        cb->accessible_disposed(m_id);
    }
    GlassApplication::DecrementAccessibility();
}

/***********************************************/
/*                  IUnknown                   */
/***********************************************/
IFACEMETHODIMP_(ULONG) GlassAccessible::AddRef()
{
    return InterlockedIncrement(&m_refCount);
}

IFACEMETHODIMP_(ULONG) GlassAccessible::Release()
{
    long val = InterlockedDecrement(&m_refCount);
    if (val == 0) {
        delete this;
    }
    return val;
}

IFACEMETHODIMP GlassAccessible::QueryInterface(REFIID riid, void** ppInterface)
{
    if (riid == __uuidof(IUnknown)) {
        *ppInterface = static_cast<IRawElementProviderSimple*>(this);
    } else if (riid == __uuidof(IRawElementProviderSimple)) {
        *ppInterface = static_cast<IRawElementProviderSimple*>(this);
    } else if (riid == __uuidof(IRawElementProviderFragment)) {
        *ppInterface = static_cast<IRawElementProviderFragment*>(this);
    } else if (riid == __uuidof(IRawElementProviderFragmentRoot)) {
        *ppInterface = static_cast<IRawElementProviderFragmentRoot*>(this);
    } else if (riid == __uuidof(IRawElementProviderAdviseEvents)) {
        *ppInterface = static_cast<IRawElementProviderAdviseEvents*>(this);
    } else if (riid == __uuidof(IInvokeProvider)) {
        *ppInterface = static_cast<IInvokeProvider*>(this);
    } else if (riid == __uuidof(ISelectionProvider)) {
        *ppInterface = static_cast<ISelectionProvider*>(this);
    } else if (riid == __uuidof(ISelectionItemProvider)) {
        *ppInterface = static_cast<ISelectionItemProvider*>(this);
    } else if (riid == __uuidof(IRangeValueProvider)) {
        *ppInterface = static_cast<IRangeValueProvider*>(this);
    } else if (riid == __uuidof(IValueProvider)) {
        *ppInterface = static_cast<IValueProvider*>(this);
    } else if (riid == __uuidof(ITextProvider)) {
        *ppInterface = static_cast<ITextProvider*>(this);
    } else if (riid == __uuidof(IGridProvider)) {
        *ppInterface = static_cast<IGridProvider*>(this);
    } else if (riid == __uuidof(IGridItemProvider)) {
        *ppInterface = static_cast<IGridItemProvider*>(this);
    } else if (riid == __uuidof(ITableProvider)) {
        *ppInterface = static_cast<ITableProvider*>(this);
    } else if (riid == __uuidof(ITableItemProvider)) {
        *ppInterface = static_cast<ITableItemProvider*>(this);
    } else if (riid == __uuidof(IToggleProvider)) {
        *ppInterface = static_cast<IToggleProvider*>(this);
    } else if (riid == __uuidof(IExpandCollapseProvider)) {
        *ppInterface = static_cast<IExpandCollapseProvider*>(this);
    } else if (riid == __uuidof(ITransformProvider)) {
        *ppInterface = static_cast<ITransformProvider*>(this);
    } else if (riid == __uuidof(IScrollProvider)) {
        *ppInterface = static_cast<IScrollProvider*>(this);
    } else if (riid == __uuidof(IScrollItemProvider)) {
        *ppInterface = static_cast<IScrollItemProvider*>(this);
    } else {
        *ppInterface = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;
}

/***********************************************/
/*        IRawElementProviderSimple            */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_HostRawElementProvider(IRawElementProviderSimple** pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t hwnd = 0;
        int32_t status = cb->get_host_raw_element_provider(m_id, &hwnd);
        if (status != GWIN_OK) return E_FAIL;
        /* The HRESULT stays ignored, for the reason the comment below gives. */
        UiaHostProviderFromHwnd(reinterpret_cast<HWND>(hwnd), pRetVal);
        return S_OK;
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_ProviderOptions(ProviderOptions* pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    /*
     * Very important to use ProviderOptions_UseComThreading, otherwise the call
     * to the providers are sent in a different thread (GetEnv() returned NULL there, and every
     * provider method gave up; the callback tables of glass_win_api.h keep the marshalling either
     * way, so this flag stays).
     */
    *pRetVal = ProviderOptions_ServerSideProvider | ProviderOptions_UseComThreading;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::GetPatternProvider(PATTERNID patternId, IUnknown** pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->get_pattern_provider(m_id, patternId, &value);
        *pRetVal = reinterpret_cast<IUnknown*>(takeAccessible(status, value));
        return statusToHr(status);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::GetPropertyValue(PROPERTYID propertyId, VARIANT* pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        GwinVariant variant;
        memset(&variant, 0, sizeof(variant));
        int32_t status = cb->get_property_value(m_id, propertyId, &variant);
        /* A throwing slot leaves the caller's VARIANT untouched, where the JNI arm returned
         * before copyVariant; a Java null arrives as VT_EMPTY and becomes E_FAIL below. */
        if (status != GWIN_OK) return E_FAIL;
        return variantFromGwin(&variant, pRetVal, true);
    }
    return E_FAIL;
}

/***********************************************/
/*       IRawElementProviderFragment           */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_BoundingRectangle(UiaRect *pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        float bounds[4] = { 0.0f, 0.0f, 0.0f, 0.0f };
        int32_t written = 0;
        int32_t status = cb->get_bounding_rectangle(m_id, bounds, &written);
        if (status != GWIN_OK) return E_FAIL;
        if (written) {
            pRetVal->left = bounds[0];
            pRetVal->top = bounds[1];
            pRetVal->width = bounds[2];
            pRetVal->height = bounds[3];
        }
        return S_OK;
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_FragmentRoot(IRawElementProviderFragmentRoot **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->get_fragment_root(m_id, &value);
        *pRetVal = static_cast<IRawElementProviderFragmentRoot*>(takeAccessible(status, value));
        return statusToHr(status);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::GetEmbeddedFragmentRoots(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t* block = NULL;
        int32_t count = 0;
        int32_t status = cb->get_embedded_fragment_roots(m_id, &block, &count);
        return copyBlockList(status, block, count, pRetVal, VT_UNKNOWN);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::GetRuntimeId(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int32_t* block = NULL;
        int32_t count = 0;
        int32_t status = cb->get_runtime_id(m_id, &block, &count);
        return copyBlockList(status, block, count, pRetVal, VT_I4);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::Navigate(NavigateDirection direction, IRawElementProviderFragment **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->navigate(m_id, direction, &value);
        *pRetVal = static_cast<IRawElementProviderFragment*>(takeAccessible(status, value));
        return statusToHr(status);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::SetFocus()
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotVoid(cb->set_focus);
    return E_FAIL;
}

/***********************************************/
/*     IRawElementProviderFragmentRoot         */
/***********************************************/
IFACEMETHODIMP GlassAccessible::ElementProviderFromPoint(double x, double y, IRawElementProviderFragment **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->element_provider_from_point(m_id, x, y, &value);
        *pRetVal = static_cast<IRawElementProviderFragment*>(takeAccessible(status, value));
        return statusToHr(status);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::GetFocus(IRawElementProviderFragment **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->get_focus(m_id, &value);
        *pRetVal = static_cast<IRawElementProviderFragment*>(takeAccessible(status, value));
        return statusToHr(status);
    }
    return E_FAIL;
}

/***********************************************/
/*     IRawElementProviderAdviseEvents         */
/***********************************************/
IFACEMETHODIMP GlassAccessible::AdviseEventAdded(EVENTID eventId, SAFEARRAY *propertyIDs)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        return statusToHr(cb->advise_event_added(m_id, eventId, (int64_t)propertyIDs));
    }
    return E_FAIL;
}


IFACEMETHODIMP GlassAccessible::AdviseEventRemoved(EVENTID eventId, SAFEARRAY *propertyIDs)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        return statusToHr(cb->advise_event_removed(m_id, eventId, (int64_t)propertyIDs));
    }
    return E_FAIL;
}

/***********************************************/
/*             IInvokeProvider                 */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Invoke()
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotVoid(cb->invoke);
    return E_FAIL;
}

/***********************************************/
/*           ISelectionProvider                */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetSelection(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t* block = NULL;
        int32_t count = 0;
        int32_t status = cb->get_selection(m_id, &block, &count);
        return copyBlockList(status, block, count, pRetVal, VT_UNKNOWN);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_CanSelectMultiple(BOOL *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_can_select_multiple, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_IsSelectionRequired(BOOL *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_is_selection_required, pRetVal);
    return E_FAIL;
}

/***********************************************/
/*         ISelectionItemProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Select()
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotVoid(cb->select);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::AddToSelection()
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotVoid(cb->add_to_selection);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::RemoveFromSelection()
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotVoid(cb->remove_from_selection);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_IsSelected(BOOL *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_is_selected, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_SelectionContainer(IRawElementProviderSimple **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->get_selection_container(m_id, &value);
        *pRetVal = static_cast<IRawElementProviderSimple*>(takeAccessible(status, value));
        return statusToHr(status);
    }
    return E_FAIL;
}

/***********************************************/
/*           IRangeValueProvider               */
/***********************************************/
IFACEMETHODIMP GlassAccessible::SetValue(double val)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return statusToHr(cb->set_value(m_id, val));
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_Value(double *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotDouble(cb->get_value, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_IsReadOnly(BOOL *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_is_read_only, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_Maximum(double *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotDouble(cb->get_maximum, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_Minimum(double *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotDouble(cb->get_minimum, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_LargeChange(double *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotDouble(cb->get_large_change, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_SmallChange(double *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotDouble(cb->get_small_change, pRetVal);
    return E_FAIL;
}

/***********************************************/
/*           IValueProvider                    */
/***********************************************/
IFACEMETHODIMP GlassAccessible::SetValue(LPCWSTR val)
{
    if (!val) return S_OK;
    size_t size = wcslen(val);
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        return statusToHr(cb->set_value_string(m_id, (const uint16_t*)val, (int32_t)size));
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_Value(BSTR *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        uint16_t* block = NULL;
        int32_t len = 0;
        int32_t status = cb->get_value_string(m_id, &block, &len);
        return copyBlockString(status, block, len, pRetVal);
    }
    return E_FAIL;
}

/***********************************************/
/*              ITextProvider                  */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetVisibleRanges(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t* block = NULL;
        int32_t count = 0;
        int32_t status = cb->get_visible_ranges(m_id, &block, &count);
        return copyBlockList(status, block, count, pRetVal, VT_UNKNOWN);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::RangeFromChild(IRawElementProviderSimple *childElement,  ITextRangeProvider **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->range_from_child(m_id, (int64_t)childElement, &value);
        if (status != GWIN_OK) return E_FAIL;
        GlassTextRangeProvider* gtrp = reinterpret_cast<GlassTextRangeProvider*>(value);
        *pRetVal = static_cast<ITextRangeProvider*>(gtrp);
        return S_OK;
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::RangeFromPoint(UiaPoint point, ITextRangeProvider **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->range_from_point(m_id, point.x, point.y, &value);
        if (status != GWIN_OK) return E_FAIL;
        GlassTextRangeProvider* gtrp = reinterpret_cast<GlassTextRangeProvider*>(value);
        *pRetVal = static_cast<ITextRangeProvider*>(gtrp);
        return S_OK;
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_DocumentRange(ITextRangeProvider **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->get_document_range(m_id, &value);
        if (status != GWIN_OK) return E_FAIL;
        GlassTextRangeProvider* gtrp = reinterpret_cast<GlassTextRangeProvider*>(value);
        if (gtrp) gtrp->AddRef();
        *pRetVal = static_cast<ITextRangeProvider*>(gtrp);
        return S_OK;
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_SupportedTextSelection(SupportedTextSelection *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_supported_text_selection, pRetVal);
    return E_FAIL;
}

/***********************************************/
/*              IGridProvider                  */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_ColumnCount(int *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_column_count, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_RowCount(int *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_row_count, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::GetItem(int row, int column, IRawElementProviderSimple **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->get_item(m_id, row, column, &value);
        *pRetVal = static_cast<IRawElementProviderSimple*>(takeAccessible(status, value));
        return statusToHr(status);
    }
    return E_FAIL;
}

/***********************************************/
/*              IGridItemProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_Column(int *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_column, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_ColumnSpan(int *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_column_span, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_ContainingGrid(IRawElementProviderSimple **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->get_containing_grid(m_id, &value);
        *pRetVal = static_cast<IRawElementProviderSimple*>(takeAccessible(status, value));
        return statusToHr(status);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_Row(int *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_row, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_RowSpan(int *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_row_span, pRetVal);
    return E_FAIL;
}

/***********************************************/
/*              ITableProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetColumnHeaders(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t* block = NULL;
        int32_t count = 0;
        int32_t status = cb->get_column_headers(m_id, &block, &count);
        return copyBlockList(status, block, count, pRetVal, VT_UNKNOWN);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::GetRowHeaders(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t* block = NULL;
        int32_t count = 0;
        int32_t status = cb->get_row_headers(m_id, &block, &count);
        return copyBlockList(status, block, count, pRetVal, VT_UNKNOWN);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_RowOrColumnMajor(RowOrColumnMajor *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_row_or_column_major, pRetVal);
    return E_FAIL;
}


/***********************************************/
/*              ITableItemProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetColumnHeaderItems(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t* block = NULL;
        int32_t count = 0;
        int32_t status = cb->get_column_header_items(m_id, &block, &count);
        return copyBlockList(status, block, count, pRetVal, VT_UNKNOWN);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::GetRowHeaderItems(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        int64_t* block = NULL;
        int32_t count = 0;
        int32_t status = cb->get_row_header_items(m_id, &block, &count);
        return copyBlockList(status, block, count, pRetVal, VT_UNKNOWN);
    }
    return E_FAIL;
}


/***********************************************/
/*              IToggleProvider                */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Toggle()
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotVoid(cb->toggle);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_ToggleState(ToggleState *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_toggle_state, pRetVal);
    return E_FAIL;
}

/***********************************************/
/*         IExpandCollapseProvider             */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Collapse()
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotVoid(cb->collapse);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::Expand()
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotVoid(cb->expand);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_ExpandCollapseState(ExpandCollapseState *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_expand_collapse_state, pRetVal);
    return E_FAIL;
}

/***********************************************/
/*         ITransformProvider                  */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_CanMove(BOOL *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_can_move, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_CanResize(BOOL *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_can_resize, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_CanRotate(BOOL *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_can_rotate, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::Move(double x, double y)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return statusToHr(cb->move(m_id, x, y));
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::Resize(double width, double height)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return statusToHr(cb->resize(m_id, width, height));
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::Rotate(double degrees)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return statusToHr(cb->rotate(m_id, degrees));
    return E_FAIL;
}

/***********************************************/
/*         IScrollProvider                     */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Scroll(ScrollAmount horizontalAmount, ScrollAmount verticalAmount)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        return statusToHr(cb->scroll(m_id, horizontalAmount, verticalAmount));
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::SetScrollPercent(double horizontalPercent, double verticalPercent)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) {
        return statusToHr(cb->set_scroll_percent(m_id, horizontalPercent, verticalPercent));
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_HorizontallyScrollable(BOOL *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_horizontally_scrollable, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_HorizontalScrollPercent(double *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotDouble(cb->get_horizontal_scroll_percent, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_HorizontalViewSize(double *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotDouble(cb->get_horizontal_view_size, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_VerticallyScrollable(BOOL *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotInt(cb->get_vertically_scrollable, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_VerticalScrollPercent(double *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotDouble(cb->get_vertical_scroll_percent, pRetVal);
    return E_FAIL;
}

IFACEMETHODIMP GlassAccessible::get_VerticalViewSize(double *pRetVal)
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotDouble(cb->get_vertical_view_size, pRetVal);
    return E_FAIL;
}

 /***********************************************/
/*         IScrollItemProvider                 */
/***********************************************/
IFACEMETHODIMP GlassAccessible::ScrollIntoView()
{
    if (const GwinAccessibleCallbacks* cb = Callbacks()) return slotVoid(cb->scroll_into_view);
    return E_FAIL;
}

/***********************************************/
/*      glass_win_api.h - accessibility        */
/***********************************************/

int32_t gwin_sizeof_accessible_callbacks(void)
{
    return (int32_t) sizeof(GwinAccessibleCallbacks);
}

int32_t gwin_sizeof_variant(void)
{
    return (int32_t) sizeof(GwinVariant);
}

int32_t gwin_a11y_set_callbacks(const GwinAccessibleCallbacks* cb)
{
    GlassAccessible::SetCallbacks(cb);   // by value, NULL slots become no-ops; cb == NULL clears
    return GWIN_OK;
}

/*
 * The statement of the former Java_com_sun_glass_ui_win_WinAccessible__1createGlassAccessible, with
 * the id where the jobject was. No OS call and no thread marshal, so it works without a toolkit.
 */
gwin_accessible_t gwin_a11y_create(int64_t accessible_id)
{
    return (gwin_accessible_t) new (std::nothrow) GlassAccessible(accessible_id);
}

void gwin_a11y_destroy(gwin_accessible_t acc)
{
    /* Release, not delete: the object lives until UIA drops its last reference. No NULL check - the
     * JNI entry point had none either, and WinAccessible guards with `if (peer != 0L)`. */
    reinterpret_cast<GlassAccessible*>(acc)->Release();
}

int64_t gwin_a11y_raise_property_changed(gwin_accessible_t acc, int32_t property_id,
                                         const GwinVariant* old_value, const GwinVariant* new_value)
{
    GlassAccessible* ga = reinterpret_cast<GlassAccessible*>(acc);
    IRawElementProviderSimple* pProvider = static_cast<IRawElementProviderSimple*>(ga);
    VARIANT ov = {0}, nv = {0};

    /* The caller owns both GwinVariants and their blocks, hence takeOwnership false. Neither VARIANT
     * is VariantClear-ed and a failing second conversion leaks the first, exactly as the JNI body
     * left it. */
    HRESULT hr = GlassAccessible::variantFromGwin(old_value, &ov, false);
    if (FAILED(hr)) return (int64_t)hr;
    hr = GlassAccessible::variantFromGwin(new_value, &nv, false);
    if (FAILED(hr)) return (int64_t)hr;

    return (int64_t)UiaRaiseAutomationPropertyChangedEvent(pProvider, (PROPERTYID)property_id, ov, nv);
}

int32_t gwin_test_variant_offsets(int32_t* out11)
{
    if (out11 == NULL) return GWIN_ERR_INVALID_ARG;
    out11[0] = (int32_t) offsetof(GwinVariant, vt);
    out11[1] = (int32_t) offsetof(GwinVariant, i_val);
    out11[2] = (int32_t) offsetof(GwinVariant, l_val);
    out11[3] = (int32_t) offsetof(GwinVariant, flt_val);
    out11[4] = (int32_t) offsetof(GwinVariant, dbl_val);
    out11[5] = (int32_t) offsetof(GwinVariant, bool_val);
    out11[6] = (int32_t) offsetof(GwinVariant, punk_val);
    out11[7] = (int32_t) offsetof(GwinVariant, bstr_val);
    out11[8] = (int32_t) offsetof(GwinVariant, bstr_len);
    out11[9] = (int32_t) offsetof(GwinVariant, p_dbl_val);
    out11[10] = (int32_t) offsetof(GwinVariant, p_dbl_count);
    return GWIN_OK;
}

namespace {

/* The GwinAccessibleCallbacks slots that write nothing through `out`; every other slot needs it. */
bool AccessibleSlotNeedsOut(int32_t slot)
{
    switch (slot) {
        case 8:                      // set_focus
        case 11: case 12:            // advise_event_added / advise_event_removed
        case 13:                     // invoke
        case 17: case 18: case 19:   // select / add_to_selection / remove_from_selection
        case 22:                     // set_value
        case 29:                     // set_value_string
        case 49:                     // toggle
        case 51: case 52:            // collapse / expand
        case 57: case 58: case 59:   // move / resize / rotate
        case 60: case 61:            // scroll / set_scroll_percent
        case 68:                     // scroll_into_view
        case 69:                     // accessible_disposed
            return false;
        default:
            return true;
    }
}

} // namespace

int64_t gwin_test_fire_accessible_callback(int32_t slot, int64_t accessible_id, void* out)
{
    static const uint16_t text[2] = { 0x0041, 0x0042 };

    const GwinAccessibleCallbacks* cb = GlassAccessible::Callbacks();
    if (cb == NULL || slot < 0 || slot > 69) return GWIN_ERR_INVALID_ARG;
    if (AccessibleSlotNeedsOut(slot) && out == NULL) return GWIN_ERR_INVALID_ARG;

    int32_t* outInt = (int32_t*) out;
    int64_t* outLong = (int64_t*) out;
    double* outDouble = (double*) out;
    float* outFloat = (float*) out;
    /* The second out-parameter of the two-value slots: a count at offset 8, the written flag of
     * get_bounding_rectangle at offset 16 (four floats precede it). */
    int32_t* outCount = (int32_t*) ((char*) out + 8);
    int32_t* outWritten = (int32_t*) ((char*) out + 16);

    try {
        switch (slot) {
            case 0: return cb->get_pattern_provider(accessible_id, 1001, outLong);
            case 1: return cb->get_host_raw_element_provider(accessible_id, outLong);
            case 2: return cb->get_property_value(accessible_id, 1001, (GwinVariant*) out);
            case 3: return cb->get_bounding_rectangle(accessible_id, outFloat, outWritten);
            case 4: return cb->get_fragment_root(accessible_id, outLong);
            case 5: return cb->get_embedded_fragment_roots(accessible_id, (int64_t**) out, outCount);
            case 6: return cb->get_runtime_id(accessible_id, (int32_t**) out, outCount);
            case 7: return cb->navigate(accessible_id, 1001, outLong);
            case 8: return cb->set_focus(accessible_id);
            case 9: return cb->element_provider_from_point(accessible_id, 1.5, 2.5, outLong);
            case 10: return cb->get_focus(accessible_id, outLong);
            case 11: return cb->advise_event_added(accessible_id, 1001, (int64_t) 0x100000002LL);
            case 12: return cb->advise_event_removed(accessible_id, 1001, (int64_t) 0x100000002LL);
            case 13: return cb->invoke(accessible_id);
            case 14: return cb->get_selection(accessible_id, (int64_t**) out, outCount);
            case 15: return cb->get_can_select_multiple(accessible_id, outInt);
            case 16: return cb->get_is_selection_required(accessible_id, outInt);
            case 17: return cb->select(accessible_id);
            case 18: return cb->add_to_selection(accessible_id);
            case 19: return cb->remove_from_selection(accessible_id);
            case 20: return cb->get_is_selected(accessible_id, outInt);
            case 21: return cb->get_selection_container(accessible_id, outLong);
            case 22: return cb->set_value(accessible_id, 1.5);
            case 23: return cb->get_value(accessible_id, outDouble);
            case 24: return cb->get_is_read_only(accessible_id, outInt);
            case 25: return cb->get_maximum(accessible_id, outDouble);
            case 26: return cb->get_minimum(accessible_id, outDouble);
            case 27: return cb->get_large_change(accessible_id, outDouble);
            case 28: return cb->get_small_change(accessible_id, outDouble);
            case 29: return cb->set_value_string(accessible_id, text, 2);
            case 30: return cb->get_value_string(accessible_id, (uint16_t**) out, outCount);
            case 31: return cb->get_visible_ranges(accessible_id, (int64_t**) out, outCount);
            case 32: return cb->range_from_child(accessible_id, (int64_t) 0x100000002LL, outLong);
            case 33: return cb->range_from_point(accessible_id, 1.5, 2.5, outLong);
            case 34: return cb->get_document_range(accessible_id, outLong);
            case 35: return cb->get_supported_text_selection(accessible_id, outInt);
            case 36: return cb->get_column_count(accessible_id, outInt);
            case 37: return cb->get_row_count(accessible_id, outInt);
            case 38: return cb->get_item(accessible_id, 1001, 1002, outLong);
            case 39: return cb->get_column(accessible_id, outInt);
            case 40: return cb->get_column_span(accessible_id, outInt);
            case 41: return cb->get_containing_grid(accessible_id, outLong);
            case 42: return cb->get_row(accessible_id, outInt);
            case 43: return cb->get_row_span(accessible_id, outInt);
            case 44: return cb->get_column_headers(accessible_id, (int64_t**) out, outCount);
            case 45: return cb->get_row_headers(accessible_id, (int64_t**) out, outCount);
            case 46: return cb->get_row_or_column_major(accessible_id, outInt);
            case 47: return cb->get_column_header_items(accessible_id, (int64_t**) out, outCount);
            case 48: return cb->get_row_header_items(accessible_id, (int64_t**) out, outCount);
            case 49: return cb->toggle(accessible_id);
            case 50: return cb->get_toggle_state(accessible_id, outInt);
            case 51: return cb->collapse(accessible_id);
            case 52: return cb->expand(accessible_id);
            case 53: return cb->get_expand_collapse_state(accessible_id, outInt);
            case 54: return cb->get_can_move(accessible_id, outInt);
            case 55: return cb->get_can_resize(accessible_id, outInt);
            case 56: return cb->get_can_rotate(accessible_id, outInt);
            case 57: return cb->move(accessible_id, 1.5, 2.5);
            case 58: return cb->resize(accessible_id, 1.5, 2.5);
            case 59: return cb->rotate(accessible_id, 1.5);
            case 60: return cb->scroll(accessible_id, 1001, 1002);
            case 61: return cb->set_scroll_percent(accessible_id, 1.5, 2.5);
            case 62: return cb->get_horizontally_scrollable(accessible_id, outInt);
            case 63: return cb->get_horizontal_scroll_percent(accessible_id, outDouble);
            case 64: return cb->get_horizontal_view_size(accessible_id, outDouble);
            case 65: return cb->get_vertically_scrollable(accessible_id, outInt);
            case 66: return cb->get_vertical_scroll_percent(accessible_id, outDouble);
            case 67: return cb->get_vertical_view_size(accessible_id, outDouble);
            case 68: return cb->scroll_into_view(accessible_id);
            case 69: cb->accessible_disposed(accessible_id); return 0;
        }
        return GWIN_ERR_INVALID_ARG;
    } catch (...) {
        return GWIN_ERR_INVALID_ARG;
    }
}
