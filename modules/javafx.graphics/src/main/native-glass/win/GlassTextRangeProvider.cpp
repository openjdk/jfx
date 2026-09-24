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


#include "common.h"
#include "GlassTextRangeProvider.h"
#include "GlassAccessible.h"

/*
 * ---- The callback table of glass_win_api.h's accessibility section, range half ----
 *
 * Installed by gwin_a11y_text_range_set_callbacks. A slot Java leaves NULL is replaced by the no-op
 * below; what the upcall sites test is Callbacks() returning NULL, which means "no table installed"
 * and leaves them no way to reach Java, so they answer the E_FAIL that the JNI arm of commit
 * 033187ad90 answered when GetEnv() gave it no JNIEnv.
 */
namespace {

int32_t NoopRangeVoid(int64_t) { return GWIN_OK; }
int32_t NoopRangeSetInt(int64_t, int32_t) { return GWIN_OK; }
int32_t NoopRangeOutLong(int64_t, int64_t* out) { *out = 0; return GWIN_OK; }
int32_t NoopRangeCompare(int64_t, int64_t, int32_t* out) { *out = 0; return GWIN_OK; }
int32_t NoopRangeCompareEndpoints(int64_t, int32_t, int64_t, int32_t, int32_t* out)
{
    *out = 0;
    return GWIN_OK;
}
int32_t NoopRangeFindAttribute(int64_t, int32_t, const GwinVariant*, int32_t, int64_t* out)
{
    *out = 0;
    return GWIN_OK;
}
int32_t NoopRangeFindText(int64_t, const uint16_t*, int32_t, int32_t, int32_t, int64_t* out)
{
    *out = 0;
    return GWIN_OK;
}
int32_t NoopRangeOutVariant(int64_t, int32_t, GwinVariant* out)
{
    memset(out, 0, sizeof(GwinVariant));
    return GWIN_OK;
}
int32_t NoopRangeOutDoubleBlock(int64_t, double** out, int32_t* out_count)
{
    *out = NULL;
    *out_count = 0;
    return GWIN_OK;
}
int32_t NoopRangeOutLongBlock(int64_t, int64_t** out, int32_t* out_count)
{
    *out = NULL;
    *out_count = 0;
    return GWIN_OK;
}
int32_t NoopRangeGetText(int64_t, int32_t, uint16_t** out, int32_t* out_len)
{
    *out = NULL;
    *out_len = 0;
    return GWIN_OK;
}
int32_t NoopRangeMove(int64_t, int32_t, int32_t, int32_t* out) { *out = 0; return GWIN_OK; }
int32_t NoopRangeMoveEndpointByUnit(int64_t, int32_t, int32_t, int32_t, int32_t* out)
{
    *out = 0;
    return GWIN_OK;
}
int32_t NoopRangeMoveEndpointByRange(int64_t, int32_t, int64_t, int32_t) { return GWIN_OK; }
void NoopRangeDisposed(int64_t) {}

/* Member-by-member, not a positional initializer: a transposition between two same-shaped slots
 * would compile silently. */
void ResetToNoops(GwinTextRangeCallbacks& t)
{
    t.clone = NoopRangeOutLong;
    t.compare = NoopRangeCompare;
    t.compare_endpoints = NoopRangeCompareEndpoints;
    t.expand_to_enclosing_unit = NoopRangeSetInt;
    t.find_attribute = NoopRangeFindAttribute;
    t.find_text = NoopRangeFindText;
    t.get_attribute_value = NoopRangeOutVariant;
    t.get_bounding_rectangles = NoopRangeOutDoubleBlock;
    t.get_enclosing_element = NoopRangeOutLong;
    t.get_text = NoopRangeGetText;
    t.move = NoopRangeMove;
    t.move_endpoint_by_unit = NoopRangeMoveEndpointByUnit;
    t.move_endpoint_by_range = NoopRangeMoveEndpointByRange;
    t.select = NoopRangeVoid;
    t.add_to_selection = NoopRangeVoid;
    t.remove_from_selection = NoopRangeVoid;
    t.scroll_into_view = NoopRangeSetInt;
    t.get_children = NoopRangeOutLongBlock;
    t.range_disposed = NoopRangeDisposed;
}

GwinTextRangeCallbacks s_rangeCallbacks;
bool s_rangeCallbacksInstalled = false;

/* The provider methods below map every non-GWIN_OK status to the E_FAIL the JNI arm of commit
 * 033187ad90 returned. */
HRESULT statusToHr(int32_t status)
{
    return GlassAccessible::statusToHr(status);
}

} // namespace

/* By value - this library never retains the caller's struct - and a NULL slot keeps its no-op.
 * Installed once, before any provider can exist; not safe against a concurrent install - the flag is a
 * plain bool and the table a plain struct assignment, with no release store between them. */
void GlassTextRangeProvider::SetCallbacks(const GwinTextRangeCallbacks* cb)
{
    GwinTextRangeCallbacks t;
    ResetToNoops(t);
    if (cb != NULL) {
        if (cb->clone) t.clone = cb->clone;
        if (cb->compare) t.compare = cb->compare;
        if (cb->compare_endpoints) t.compare_endpoints = cb->compare_endpoints;
        if (cb->expand_to_enclosing_unit) t.expand_to_enclosing_unit = cb->expand_to_enclosing_unit;
        if (cb->find_attribute) t.find_attribute = cb->find_attribute;
        if (cb->find_text) t.find_text = cb->find_text;
        if (cb->get_attribute_value) t.get_attribute_value = cb->get_attribute_value;
        if (cb->get_bounding_rectangles) t.get_bounding_rectangles = cb->get_bounding_rectangles;
        if (cb->get_enclosing_element) t.get_enclosing_element = cb->get_enclosing_element;
        if (cb->get_text) t.get_text = cb->get_text;
        if (cb->move) t.move = cb->move;
        if (cb->move_endpoint_by_unit) t.move_endpoint_by_unit = cb->move_endpoint_by_unit;
        if (cb->move_endpoint_by_range) t.move_endpoint_by_range = cb->move_endpoint_by_range;
        if (cb->select) t.select = cb->select;
        if (cb->add_to_selection) t.add_to_selection = cb->add_to_selection;
        if (cb->remove_from_selection) t.remove_from_selection = cb->remove_from_selection;
        if (cb->scroll_into_view) t.scroll_into_view = cb->scroll_into_view;
        if (cb->get_children) t.get_children = cb->get_children;
        if (cb->range_disposed) t.range_disposed = cb->range_disposed;
    }
    s_rangeCallbacksInstalled = false;
    s_rangeCallbacks = t;
    s_rangeCallbacksInstalled = cb != NULL;
}

/* static */ const GwinTextRangeCallbacks* GlassTextRangeProvider::Callbacks()
{
    return s_rangeCallbacksInstalled ? &s_rangeCallbacks : NULL;
}

GlassTextRangeProvider::GlassTextRangeProvider(int64_t rangeId, GlassAccessible* glassAccessible)
: m_refCount(1), m_id(rangeId)
{
    m_glassAccessible = glassAccessible;
    m_glassAccessible->AddRef();
}

GlassTextRangeProvider::~GlassTextRangeProvider()
{
    const GwinTextRangeCallbacks* cb = Callbacks();
    if (cb != NULL) {
        /* Where DeleteGlobalRef stood at commit 033187ad90: the last COM reference has gone, so Java
         * may drop its registry entry for m_id. For a range nothing in Java retains - Clone,
         * RangeFromPoint, FindText, FindAttribute and RangeFromChild all hand out a range the Java
         * side forgets - this is the only thing that keeps its peer reachable, and the COM caller owns
         * that one reference. */
        cb->range_disposed(m_id);
    }
    m_glassAccessible->Release();
}

/***********************************************/
/*                  IUnknown                   */
/***********************************************/
IFACEMETHODIMP_(ULONG) GlassTextRangeProvider::AddRef()
{
    return InterlockedIncrement(&m_refCount);
}

IFACEMETHODIMP_(ULONG) GlassTextRangeProvider::Release()
{
    long val = InterlockedDecrement(&m_refCount);
    if (val == 0) {
        delete this;
    }
    return val;
}

IFACEMETHODIMP GlassTextRangeProvider::QueryInterface(REFIID riid, void** ppInterface)
{
    if (riid == __uuidof(IUnknown)) {
        *ppInterface = static_cast<ITextRangeProvider*>(this);
    } else if (riid == __uuidof(ITextRangeProvider)) {
        *ppInterface = static_cast<ITextRangeProvider*>(this);
    } else {
        *ppInterface = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;
}

/***********************************************/
/*             ITextRangeProvider              */
/***********************************************/
IFACEMETHODIMP GlassTextRangeProvider::Clone(ITextRangeProvider **pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->clone(m_id, &value);
        if (status != GWIN_OK) return E_FAIL;
        /* Not AddRefed - see the comment below. */
        *pRetVal = static_cast<ITextRangeProvider*>(
            reinterpret_cast<GlassTextRangeProvider*>(value));
        return S_OK;
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::Compare(ITextRangeProvider *range, BOOL *pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        GlassTextRangeProvider* other = reinterpret_cast<GlassTextRangeProvider*>(range);
        if (other == NULL || other->GetId() == 0) {
            *pRetVal = FALSE;
            return S_OK;
        }
        int32_t value = 0;
        int32_t status = cb->compare(m_id, other->GetId(), &value);
        *pRetVal = value;
        return statusToHr(status);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::CompareEndpoints(TextPatternRangeEndpoint endpoint, ITextRangeProvider *targetRange,
                                                        TextPatternRangeEndpoint targetEndpoint, int *pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        GlassTextRangeProvider* other = reinterpret_cast<GlassTextRangeProvider*>(targetRange);
        if (other == NULL || other->GetId() == 0) {
            *pRetVal = FALSE;
            return S_OK;
        }
        int32_t value = 0;
        int32_t status = cb->compare_endpoints(m_id, endpoint, other->GetId(),
                                               targetEndpoint, &value);
        *pRetVal = value;
        return statusToHr(status);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::ExpandToEnclosingUnit(TextUnit unit)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        return statusToHr(cb->expand_to_enclosing_unit(m_id, unit));
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::FindAttribute(TEXTATTRIBUTEID attributeId, VARIANT val, BOOL backward, ITextRangeProvider **pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        /* NULL for the VARIANT, reproducing the jobject jVal = NULL below. */
        int32_t status = cb->find_attribute(m_id, attributeId, NULL, backward, &value);
        if (status != GWIN_OK) return E_FAIL;
        /* Not AddRefed - see the comment below. */
        *pRetVal = static_cast<ITextRangeProvider*>(
            reinterpret_cast<GlassTextRangeProvider*>(value));
        return S_OK;
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::FindText(BSTR text, BOOL backward, BOOL ignoreCase, ITextRangeProvider **pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->find_text(m_id, (const uint16_t*)text, (int32_t)SysStringLen(text),
                                       backward, ignoreCase, &value);
        if (status != GWIN_OK) return E_FAIL;
        /* Not AddRefed - see the comment below. */
        *pRetVal = static_cast<ITextRangeProvider*>(
            reinterpret_cast<GlassTextRangeProvider*>(value));
        return S_OK;
    }
    return E_FAIL;
}
IFACEMETHODIMP GlassTextRangeProvider::GetAttributeValue(TEXTATTRIBUTEID attributeId, VARIANT *pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        GwinVariant variant;
        memset(&variant, 0, sizeof(variant));
        int32_t status = cb->get_attribute_value(m_id, attributeId, &variant);
        if (status != GWIN_OK) return E_FAIL;
        return GlassAccessible::variantFromGwin(&variant, pRetVal, true);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::GetBoundingRectangles(SAFEARRAY **pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        double* block = NULL;
        int32_t count = 0;
        int32_t status = cb->get_bounding_rectangles(m_id, &block, &count);
        return GlassAccessible::copyBlockList(status, block, count, pRetVal, VT_R8);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::GetEnclosingElement(IRawElementProviderSimple **pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        int64_t value = 0;
        int32_t status = cb->get_enclosing_element(m_id, &value);
        *pRetVal = static_cast<IRawElementProviderSimple*>(
            GlassAccessible::takeAccessible(status, value));
        return statusToHr(status);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::GetText(int maxLength, BSTR *pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        uint16_t* block = NULL;
        int32_t len = 0;
        int32_t status = cb->get_text(m_id, maxLength, &block, &len);
        return GlassAccessible::copyBlockString(status, block, len, pRetVal);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::Move(TextUnit unit, int count, int *pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        int32_t value = 0;
        int32_t status = cb->move(m_id, unit, count, &value);
        *pRetVal = value;
        return statusToHr(status);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::MoveEndpointByUnit(TextPatternRangeEndpoint endpoint, TextUnit unit, int count, int *pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        int32_t value = 0;
        int32_t status = cb->move_endpoint_by_unit(m_id, endpoint, unit, count, &value);
        *pRetVal = value;
        return statusToHr(status);
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::MoveEndpointByRange(TextPatternRangeEndpoint endpoint, ITextRangeProvider *targetRange,
                                                           TextPatternRangeEndpoint targetEndpoint)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        GlassTextRangeProvider* other = reinterpret_cast<GlassTextRangeProvider*>(targetRange);
        if (other == NULL || other->GetId() == 0) {
            return S_OK;
        }
        return statusToHr(cb->move_endpoint_by_range(m_id, endpoint, other->GetId(),
                                                     targetEndpoint));
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::Select()
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) return statusToHr(cb->select(m_id));
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::AddToSelection()
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) return statusToHr(cb->add_to_selection(m_id));
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::RemoveFromSelection()
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) return statusToHr(cb->remove_from_selection(m_id));
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::ScrollIntoView(BOOL alignToTop)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        return statusToHr(cb->scroll_into_view(m_id, alignToTop));
    }
    return E_FAIL;
}

IFACEMETHODIMP GlassTextRangeProvider::GetChildren(SAFEARRAY **pRetVal)
{
    if (const GwinTextRangeCallbacks* cb = Callbacks()) {
        int64_t* block = NULL;
        int32_t count = 0;
        int32_t status = cb->get_children(m_id, &block, &count);
        return GlassAccessible::copyBlockList(status, block, count, pRetVal, VT_UNKNOWN);
    }
    return E_FAIL;
}

/***********************************************/
/*      glass_win_api.h - text ranges          */
/***********************************************/

int32_t gwin_sizeof_text_range_callbacks(void)
{
    return (int32_t) sizeof(GwinTextRangeCallbacks);
}

int32_t gwin_a11y_text_range_set_callbacks(const GwinTextRangeCallbacks* cb)
{
    GlassTextRangeProvider::SetCallbacks(cb);   // by value, NULL slots become no-ops
    return GWIN_OK;
}

/*
 * The statement of the former Java_com_sun_glass_ui_win_WinTextRangeProvider__1createTextRangeProvider,
 * with the id where the jobject was. NULL for a NULL accessible, as that entry point answered.
 */
gwin_text_range_t gwin_a11y_text_range_create(gwin_accessible_t acc, int64_t range_id)
{
    GlassAccessible* ga = reinterpret_cast<GlassAccessible*>(acc);
    if (ga == NULL) return NULL;
    return (gwin_text_range_t) new (std::nothrow) GlassTextRangeProvider(range_id, ga);
}

void gwin_a11y_text_range_destroy(gwin_text_range_t range)
{
    /* Release, not delete; no NULL check, exactly as
     * Java_com_sun_glass_ui_win_WinTextRangeProvider__1destroyTextRangeProvider and
     * WinTextRangeProvider.dispose() have always been. */
    reinterpret_cast<GlassTextRangeProvider*>(range)->Release();
}

namespace {

/* The GwinTextRangeCallbacks slots that write nothing through `out`. */
bool TextRangeSlotNeedsOut(int32_t slot)
{
    switch (slot) {
        case 3:                      // expand_to_enclosing_unit
        case 12:                     // move_endpoint_by_range
        case 13: case 14: case 15:   // select / add_to_selection / remove_from_selection
        case 16:                     // scroll_into_view
        case 18:                     // range_disposed
            return false;
        default:
            return true;
    }
}

} // namespace

int64_t gwin_test_fire_text_range_callback(int32_t slot, int64_t range_id, void* out)
{
    static const uint16_t text[2] = { 0x0041, 0x0042 };

    const GwinTextRangeCallbacks* cb = GlassTextRangeProvider::Callbacks();
    if (cb == NULL || slot < 0 || slot > 18) return GWIN_ERR_INVALID_ARG;
    if (TextRangeSlotNeedsOut(slot) && out == NULL) return GWIN_ERR_INVALID_ARG;

    int32_t* outInt = (int32_t*) out;
    int64_t* outLong = (int64_t*) out;
    int32_t* outCount = (int32_t*) ((char*) out + 8);

    try {
        switch (slot) {
            case 0: return cb->clone(range_id, outLong);
            case 1: return cb->compare(range_id, (int64_t) 0x100000002LL, outInt);
            case 2: return cb->compare_endpoints(range_id, 1001, (int64_t) 0x100000002LL, 1002, outInt);
            case 3: return cb->expand_to_enclosing_unit(range_id, 1001);
            case 4: return cb->find_attribute(range_id, 1001, NULL, 1, outLong);
            case 5: return cb->find_text(range_id, text, 2, 1, 1, outLong);
            case 6: return cb->get_attribute_value(range_id, 1001, (GwinVariant*) out);
            case 7: return cb->get_bounding_rectangles(range_id, (double**) out, outCount);
            case 8: return cb->get_enclosing_element(range_id, outLong);
            case 9: return cb->get_text(range_id, 1001, (uint16_t**) out, outCount);
            case 10: return cb->move(range_id, 1001, 1002, outInt);
            case 11: return cb->move_endpoint_by_unit(range_id, 1001, 1002, 1003, outInt);
            case 12: return cb->move_endpoint_by_range(range_id, 1001, (int64_t) 0x100000002LL, 1002);
            case 13: return cb->select(range_id);
            case 14: return cb->add_to_selection(range_id);
            case 15: return cb->remove_from_selection(range_id);
            case 16: return cb->scroll_into_view(range_id, 1);
            case 17: return cb->get_children(range_id, (int64_t**) out, outCount);
            case 18: cb->range_disposed(range_id); return 0;
        }
        return GWIN_ERR_INVALID_ARG;
    } catch (...) {
        return GWIN_ERR_INVALID_ARG;
    }
}
