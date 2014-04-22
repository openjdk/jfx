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


#include "common.h"
#include "com_sun_glass_ui_win_WinAccessible.h"
#include "GlassAccessible.h"

/* WinAccessible Method IDs */
static jmethodID mid_GetPatternProvider;
static jmethodID mid_get_HostRawElementProvider;
static jmethodID mid_GetPropertyValue;

static jmethodID mid_get_BoundingRectangle;
static jmethodID mid_get_FragmentRoot;
static jmethodID mid_GetEmbeddedFragmentRoots;
static jmethodID mid_GetRuntimeId;
static jmethodID mid_Navigate;
static jmethodID mid_SetFocus;

static jmethodID mid_ElementProviderFromPoint;
static jmethodID mid_GetFocus;

static jmethodID mid_Invoke;

static jmethodID mid_GetSelection;
static jmethodID mid_get_CanSelectMultiple;
static jmethodID mid_get_IsSelectionRequired;

static jmethodID mid_Select;
static jmethodID mid_AddToSelection;
static jmethodID mid_RemoveFromSelection;
static jmethodID mid_get_IsSelected;
static jmethodID mid_get_SelectionContainer;

static jmethodID mid_SetValue;
static jmethodID mid_get_Value;
static jmethodID mid_get_IsReadOnly;
static jmethodID mid_get_Maximum;
static jmethodID mid_get_Minimum;
static jmethodID mid_get_LargeChange;
static jmethodID mid_get_SmallChange;

static jmethodID mid_SetValueString;
static jmethodID mid_get_ValueString;

static jmethodID mid_GetVisibleRanges;
static jmethodID mid_RangeFromChild;
static jmethodID mid_RangeFromPoint;
static jmethodID mid_get_DocumentRange;
static jmethodID mid_get_SupportedTextSelection;

static jmethodID mid_get_ColumnCount;
static jmethodID mid_get_RowCount;
static jmethodID mid_GetItem;

static jmethodID mid_get_Row;
static jmethodID mid_get_RowSpan;
static jmethodID mid_get_ContainingGrid;
static jmethodID mid_get_Column;
static jmethodID mid_get_ColumnSpan;

static jmethodID mid_GetColumnHeaders;
static jmethodID mid_GetRowHeaders;
static jmethodID mid_get_RowOrColumnMajor;

static jmethodID mid_GetColumnHeaderItems;
static jmethodID mid_GetRowHeaderItems;

static jmethodID mid_Toggle;
static jmethodID mid_get_ToggleState;

static jmethodID mid_Collapse;
static jmethodID mid_Expand;
static jmethodID mid_get_ExpandCollapseState;

static jmethodID mid_get_CanMove;
static jmethodID mid_get_CanResize;
static jmethodID mid_get_CanRotate;
static jmethodID mid_Move;
static jmethodID mid_Resize;
static jmethodID mid_Rotate;

static jmethodID mid_Scroll;
static jmethodID mid_SetScrollPercent;
static jmethodID mid_get_HorizontallyScrollable;
static jmethodID mid_get_HorizontalScrollPercent;
static jmethodID mid_get_HorizontalViewSize;
static jmethodID mid_get_VerticallyScrollable;
static jmethodID mid_get_VerticalScrollPercent;
static jmethodID mid_get_VerticalViewSize;

static jmethodID mid_ScrollIntoView;

/* Variant Field IDs */
static jfieldID fid_vt;
static jfieldID fid_iVal;
static jfieldID fid_lVal;
static jfieldID fid_punkVal;
static jfieldID fid_fltVal;
static jfieldID fid_dblVal;
static jfieldID fid_boolVal;
static jfieldID fid_bstrVal;
static jfieldID fid_pDblVal;


/* static */ HRESULT GlassAccessible::copyString(JNIEnv *env, jstring jString, BSTR* pbstrVal)
{
    if (pbstrVal != NULL) {
        UINT length = env->GetStringLength(jString);
        const jchar* ptr = env->GetStringCritical(jString, NULL);
        if (ptr != NULL) {
            *pbstrVal = SysAllocStringLen(reinterpret_cast<const OLECHAR *>(ptr), length);
            env->ReleaseStringCritical(jString, ptr);
            return S_OK;
        }
    }
    return E_FAIL;
}

/* static */ HRESULT GlassAccessible::copyList(JNIEnv *env, jarray list, SAFEARRAY** pparrayVal, VARTYPE vt)
{
    if (list) {
        jsize size = env->GetArrayLength(list);
        SAFEARRAY *psa = SafeArrayCreateVector(vt, 0, size);
        if (psa) {
            void* listPtr = env->GetPrimitiveArrayCritical(list, 0);
            jint* intPtr = (jint*)listPtr;
            jlong* longPtr = (jlong*)listPtr;
            jdouble* doublePtr = (jdouble*)listPtr;
            for (LONG i = 0; i < size; i++) {
                if (vt == VT_UNKNOWN) {
                    //TODO make sure AddRef on elements is not required ?
                    SafeArrayPutElement(psa, &i,  (void*)longPtr[i]);
                } else if (vt == VT_I4){
                    SafeArrayPutElement(psa, &i, (void*)&(intPtr[i]));
                } else if (vt == VT_R8){
                    SafeArrayPutElement(psa, &i, (void*)&(doublePtr[i]));
                }
            }
            env->ReleasePrimitiveArrayCritical(list, listPtr, 0);
            *pparrayVal = psa;
            return S_OK;
        }
    }
    return E_FAIL;
}

/* static */ HRESULT GlassAccessible::copyVariant(JNIEnv *env, jobject jVariant, VARIANT* pRetVal)
{
    if (pRetVal == NULL) return E_FAIL;
    if (jVariant == NULL) {
        pRetVal->vt = VT_EMPTY;
        return E_FAIL;
    }
    HRESULT hr = S_OK;
    pRetVal->vt = (VARTYPE)env->GetShortField(jVariant, fid_vt);
    switch (pRetVal->vt) {
        case VT_I2:
            pRetVal->iVal = env->GetShortField(jVariant, fid_iVal);
            break;
        case VT_I4:
            pRetVal->lVal = env->GetIntField(jVariant, fid_lVal);
            break;
        case VT_UNKNOWN:
            pRetVal->punkVal = (IUnknown*)env->GetLongField(jVariant, fid_punkVal);
            if (pRetVal->punkVal != NULL) {
                pRetVal->punkVal->AddRef();
            } else {
                hr = E_FAIL;
            }
            break;
        case VT_R4:
            pRetVal->fltVal = env->GetFloatField(jVariant, fid_fltVal);
            break;
        case VT_R8:
            pRetVal->dblVal = env->GetDoubleField(jVariant, fid_dblVal);
            break;
        case VT_BOOL: {
            jboolean boolVal = env->GetBooleanField(jVariant, fid_boolVal);
            pRetVal->boolVal = boolVal ? VARIANT_TRUE : VARIANT_FALSE;
            break;
        }
        case VT_BSTR: {
            jstring str = (jstring)env->GetObjectField(jVariant, fid_bstrVal);
            hr = GlassAccessible::copyString(env, str, &(pRetVal->bstrVal));
            break;
        }
        case VT_R8 | VT_ARRAY: {
            jarray list = (jarray)env->GetObjectField(jVariant, fid_pDblVal);
            hr = GlassAccessible::copyList(env, list, &(pRetVal->parray), VT_R8);
            break;
        }
    }
    if (FAILED(hr)) pRetVal->vt = VT_EMPTY;
    return hr;
}

HRESULT GlassAccessible::callLongMethod(jmethodID mid,  IUnknown **pRetVal, ...)
{
    va_list vl;
    va_start(vl, mid);
    JNIEnv* env = GetEnv();
    jlong ptr = env->CallLongMethodV(m_jAccessible, mid, vl);
    va_end(vl);
    if (CheckAndClearException(env)) return E_FAIL;

    /* AddRef the result */
    IUnknown* iUnknown = reinterpret_cast<IUnknown*>(ptr);
    if (iUnknown) iUnknown->AddRef();
    *pRetVal = iUnknown;
    return S_OK;
}

HRESULT GlassAccessible::callArrayMethod(jmethodID mid, VARTYPE vt, SAFEARRAY **pRetVal)
{
    JNIEnv* env = GetEnv();
    jarray list = (jarray)env->CallObjectMethod(m_jAccessible, mid);
    if (CheckAndClearException(env)) return E_FAIL;

    return GlassAccessible::copyList(env, list, pRetVal, vt);
}

GlassAccessible::GlassAccessible(JNIEnv* env, jobject jAccessible)
: m_refCount(1)
{
    m_jAccessible = env->NewGlobalRef(jAccessible);
}

GlassAccessible::~GlassAccessible()
{
    JNIEnv* env = GetEnv();
    if (env) env->DeleteGlobalRef(m_jAccessible);
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
    JNIEnv* env = GetEnv();
    jlong hwnd = env->CallLongMethod(m_jAccessible, mid_get_HostRawElementProvider);
    if (CheckAndClearException(env)) return E_FAIL;
    
    // We ignore the return value of UiaHostProviderFromHwnd because it returns E_INVALIDARG
    // when invoked with NULL hwnd. We use NULL hwnds to represent "lightweight" accessibles.
    // If we don't ignore it and return it from GlassAccessible::get_HostRawElementProvider,
    // then a11y is broken on Windows 7.
    UiaHostProviderFromHwnd(reinterpret_cast<HWND>(hwnd), pRetVal);

    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_ProviderOptions(ProviderOptions* pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    /*
     * Very important to use ProviderOptions_UseComThreading, otherwise the call
     * to the providers are sent in a different thread (GetEnv() returns NULL).
     */
    *pRetVal = ProviderOptions_ServerSideProvider | ProviderOptions_UseComThreading;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::GetPatternProvider(PATTERNID patternId, IUnknown** pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    return callLongMethod(mid_GetPatternProvider, pRetVal, patternId);
}

IFACEMETHODIMP GlassAccessible::GetPropertyValue(PROPERTYID propertyId, VARIANT* pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    JNIEnv* env = GetEnv();
    jobject jVariant = env->CallObjectMethod(m_jAccessible, mid_GetPropertyValue, propertyId);
    if (CheckAndClearException(env)) return E_FAIL;

    return copyVariant(env, jVariant, pRetVal);
}

/***********************************************/
/*       IRawElementProviderFragment           */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_BoundingRectangle(UiaRect *pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    JNIEnv* env = GetEnv();
    jfloatArray bounds = (jfloatArray)env->CallObjectMethod(m_jAccessible, mid_get_BoundingRectangle);
    if (CheckAndClearException(env)) return E_FAIL;

    if (bounds) {
        jfloat* boundsPtr = (jfloat*)env->GetPrimitiveArrayCritical(bounds, 0);
        pRetVal->left = boundsPtr[0];
        pRetVal->top = boundsPtr[1];
        pRetVal->width = boundsPtr[2];
        pRetVal->height = boundsPtr[3];
        env->ReleasePrimitiveArrayCritical(bounds, boundsPtr, 0);
    }
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_FragmentRoot(IRawElementProviderFragmentRoot **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    IUnknown* ptr = NULL;
    HRESULT hr = callLongMethod(mid_get_FragmentRoot, &ptr);
    *pRetVal = reinterpret_cast<IRawElementProviderFragmentRoot*>(ptr);
    return hr;
}

IFACEMETHODIMP GlassAccessible::GetEmbeddedFragmentRoots(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    return callArrayMethod(mid_GetEmbeddedFragmentRoots, VT_UNKNOWN, pRetVal);
}

IFACEMETHODIMP GlassAccessible::GetRuntimeId(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    return callArrayMethod(mid_GetRuntimeId, VT_I4, pRetVal);
}

IFACEMETHODIMP GlassAccessible::Navigate(NavigateDirection direction, IRawElementProviderFragment **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    IUnknown* ptr = NULL;
    HRESULT hr = callLongMethod(mid_Navigate, &ptr, direction);
    *pRetVal = reinterpret_cast<IRawElementProviderFragment*>(ptr);
    return hr;
}

IFACEMETHODIMP GlassAccessible::SetFocus()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_SetFocus);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

/***********************************************/
/*     IRawElementProviderFragmentRoot         */
/***********************************************/
IFACEMETHODIMP GlassAccessible::ElementProviderFromPoint(double x, double y, IRawElementProviderFragment **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    IUnknown* ptr = NULL;
    HRESULT hr = callLongMethod(mid_ElementProviderFromPoint, &ptr, x, y);
    *pRetVal = reinterpret_cast<IRawElementProviderFragment*>(ptr);
    return hr;
}

IFACEMETHODIMP GlassAccessible::GetFocus(IRawElementProviderFragment **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    IUnknown* ptr = NULL;
    HRESULT hr = callLongMethod(mid_GetFocus, &ptr);
    *pRetVal = reinterpret_cast<IRawElementProviderFragment*>(ptr);
    return hr;
}

/***********************************************/
/*             IInvokeProvider                 */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Invoke()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Invoke);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

/***********************************************/
/*           ISelectionProvider                */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetSelection(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    return callArrayMethod(mid_GetSelection, VT_UNKNOWN, pRetVal);
}

IFACEMETHODIMP GlassAccessible::get_CanSelectMultiple(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_CanSelectMultiple);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_IsSelectionRequired(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_IsSelectionRequired);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

/***********************************************/
/*         ISelectionItemProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Select()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Select);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::AddToSelection()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_AddToSelection);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::RemoveFromSelection()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_RemoveFromSelection);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_IsSelected(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_IsSelected);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_SelectionContainer(IRawElementProviderSimple **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    IUnknown* ptr = NULL;
    HRESULT hr = callLongMethod(mid_get_SelectionContainer, &ptr);
    *pRetVal = reinterpret_cast<IRawElementProviderSimple*>(ptr);
    return hr;
}

/***********************************************/
/*           IRangeValueProvider               */
/***********************************************/
IFACEMETHODIMP GlassAccessible::SetValue(double val)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_SetValue, val);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_Value(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_Value);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_IsReadOnly(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_IsReadOnly);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_Maximum(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_Maximum);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_Minimum(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_Minimum);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_LargeChange(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_LargeChange);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_SmallChange(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_SmallChange);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

/***********************************************/
/*           IValueProvider                    */
/***********************************************/
IFACEMETHODIMP GlassAccessible::SetValue(LPCWSTR val)
{
    if (!val) return S_OK;
    size_t size = wcslen(val);
    JNIEnv* env = GetEnv();
    jstring str = env->NewString((const jchar *)val, (jsize)size);
    if (!CheckAndClearException(env)) {
    	env->CallVoidMethod(m_jAccessible, mid_SetValueString, str);
    	if (CheckAndClearException(env)) return E_FAIL;
    }
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_Value(BSTR *pRetVal)
{
    JNIEnv* env = GetEnv();
    jstring str = (jstring)env->CallObjectMethod(m_jAccessible, mid_get_ValueString);
    if (CheckAndClearException(env)) return E_FAIL;
    if (str) {
        UINT length = env->GetStringLength(str);
        const jchar* ptr = env->GetStringCritical(str, NULL);
        if (ptr != NULL) {
        	*pRetVal = SysAllocStringLen(reinterpret_cast<const OLECHAR *>(ptr), length);
        	env->ReleaseStringCritical(str, ptr);
        }
    } else {
        *pRetVal = NULL;
    }
    return S_OK;
}

/***********************************************/
/*              ITextProvider                  */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetVisibleRanges(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    return callArrayMethod(mid_GetVisibleRanges, VT_UNKNOWN, pRetVal);
}

IFACEMETHODIMP GlassAccessible::RangeFromChild(IRawElementProviderSimple *childElement,  ITextRangeProvider **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    IUnknown* ptr = NULL;
    HRESULT hr = callLongMethod(mid_RangeFromChild, &ptr, (jlong)childElement);
    *pRetVal = reinterpret_cast<ITextRangeProvider*>(ptr);
    return hr;
}

IFACEMETHODIMP GlassAccessible::RangeFromPoint(UiaPoint point, ITextRangeProvider **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    IUnknown* ptr = NULL;
    HRESULT hr = callLongMethod(mid_RangeFromPoint, &ptr, point.x, point.y);
    *pRetVal = reinterpret_cast<ITextRangeProvider*>(ptr);
    return hr;
}

IFACEMETHODIMP GlassAccessible::get_DocumentRange(ITextRangeProvider **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    IUnknown* ptr = NULL;
    HRESULT hr = callLongMethod(mid_get_DocumentRange, &ptr);
    *pRetVal = reinterpret_cast<ITextRangeProvider*>(ptr);
    return hr;
}

IFACEMETHODIMP GlassAccessible::get_SupportedTextSelection(SupportedTextSelection *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = (SupportedTextSelection)env->CallIntMethod(m_jAccessible, mid_get_SupportedTextSelection);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

/***********************************************/
/*              IGridProvider                  */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_ColumnCount(int *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_ColumnCount);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_RowCount(int *pRetVal) {
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_RowCount);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::GetItem(int row, int column, IRawElementProviderSimple **pRetVal) {
    if (pRetVal == NULL) return E_INVALIDARG;
    IUnknown* ptr = NULL;
    HRESULT hr = callLongMethod(mid_GetItem, &ptr, row, column);
    *pRetVal = reinterpret_cast<IRawElementProviderSimple*>(ptr);
    return hr;
}

/***********************************************/
/*              IGridItemProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_Column(int *pRetVal) {
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_Column);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_ColumnSpan(int *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_ColumnSpan);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_ContainingGrid(IRawElementProviderSimple **pRetVal){
    if (pRetVal == NULL) return E_INVALIDARG;
    IUnknown* ptr = NULL;
    HRESULT hr = callLongMethod(mid_get_ContainingGrid, &ptr);
    *pRetVal = reinterpret_cast<IRawElementProviderSimple*>(ptr);
    return hr;
}

IFACEMETHODIMP GlassAccessible::get_Row(int *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_Row);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_RowSpan(int *pRetVal) {
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_RowSpan);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

/***********************************************/
/*              ITableProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetColumnHeaders(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    return callArrayMethod(mid_GetColumnHeaders, VT_UNKNOWN, pRetVal);
}

IFACEMETHODIMP GlassAccessible::GetRowHeaders(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    return callArrayMethod(mid_GetRowHeaders, VT_UNKNOWN, pRetVal);
}

IFACEMETHODIMP GlassAccessible::get_RowOrColumnMajor(RowOrColumnMajor *pRetVal) {
    JNIEnv* env = GetEnv();
    *pRetVal = (RowOrColumnMajor) env->CallIntMethod(m_jAccessible, mid_get_RowOrColumnMajor);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}


/***********************************************/
/*              ITableItemProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetColumnHeaderItems(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    return callArrayMethod(mid_GetColumnHeaderItems, VT_UNKNOWN, pRetVal);
}

IFACEMETHODIMP GlassAccessible::GetRowHeaderItems(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    return callArrayMethod(mid_GetRowHeaderItems, VT_UNKNOWN, pRetVal);
}


/***********************************************/
/*              IToggleProvider                */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Toggle()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Toggle);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_ToggleState(ToggleState *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = (ToggleState) env->CallIntMethod(m_jAccessible, mid_get_ToggleState);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

/***********************************************/
/*         IExpandCollapseProvider             */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Collapse()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Collapse);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::Expand()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Expand);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_ExpandCollapseState(ExpandCollapseState *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = (ExpandCollapseState) env->CallIntMethod(m_jAccessible, mid_get_ExpandCollapseState);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

/***********************************************/
/*         ITransformProvider                  */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_CanMove(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_CanMove);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_CanResize(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_CanResize);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_CanRotate(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_CanRotate);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::Move(double x, double y)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Move, x, y);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::Resize(double width, double height)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Resize, width, height);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::Rotate(double degrees)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Rotate, degrees);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

/***********************************************/
/*         IScrollProvider                     */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Scroll(ScrollAmount horizontalAmount, ScrollAmount verticalAmount)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Scroll, horizontalAmount, verticalAmount);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::SetScrollPercent(double horizontalPercent, double verticalPercent)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_SetScrollPercent, horizontalPercent, verticalPercent);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_HorizontallyScrollable(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_HorizontallyScrollable);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_HorizontalScrollPercent(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_HorizontalScrollPercent);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_HorizontalViewSize(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_HorizontalViewSize);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_VerticallyScrollable(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_VerticallyScrollable);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_VerticalScrollPercent(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_VerticalScrollPercent);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_VerticalViewSize(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_VerticalViewSize);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

 /***********************************************/
/*         IScrollItemProvider                 */
/***********************************************/
IFACEMETHODIMP GlassAccessible::ScrollIntoView()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_ScrollIntoView);
    if (CheckAndClearException(env)) return E_FAIL;
    return S_OK;
}

/***********************************************/
/*                  JNI                        */
/***********************************************/

/*
 * Class:     com_sun_glass_ui_win_WinAccessible
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinAccessible__1initIDs
  (JNIEnv *env, jclass jClass)
{
    /* IRawElementProviderSimple */
    mid_GetPatternProvider = env->GetMethodID(jClass, "GetPatternProvider", "(I)J");
    if (env->ExceptionCheck()) return;
    mid_get_HostRawElementProvider = env->GetMethodID(jClass, "get_HostRawElementProvider", "()J");
    if (env->ExceptionCheck()) return;
    mid_GetPropertyValue = env->GetMethodID(jClass, "GetPropertyValue", "(I)Lcom/sun/glass/ui/win/WinVariant;");
    if (env->ExceptionCheck()) return;

    /* IRawElementProviderFragment */
    mid_get_BoundingRectangle = env->GetMethodID(jClass, "get_BoundingRectangle", "()[F");
    if (env->ExceptionCheck()) return;
    mid_get_FragmentRoot = env->GetMethodID(jClass, "get_FragmentRoot", "()J");
    if (env->ExceptionCheck()) return;
    mid_GetEmbeddedFragmentRoots = env->GetMethodID(jClass, "GetEmbeddedFragmentRoots", "()[J");
    if (env->ExceptionCheck()) return;
    mid_GetRuntimeId = env->GetMethodID(jClass, "GetRuntimeId", "()[I");
    if (env->ExceptionCheck()) return;
    mid_Navigate = env->GetMethodID(jClass, "Navigate", "(I)J");
    if (env->ExceptionCheck()) return;
    mid_SetFocus = env->GetMethodID(jClass, "SetFocus", "()V");
    if (env->ExceptionCheck()) return;

    /* IRawElementProviderFragmentRoot */
    mid_ElementProviderFromPoint = env->GetMethodID(jClass, "ElementProviderFromPoint", "(DD)J");
    if (env->ExceptionCheck()) return;
    mid_GetFocus = env->GetMethodID(jClass, "GetFocus", "()J");
    if (env->ExceptionCheck()) return;

    /* IInvokeProvider */
    mid_Invoke = env->GetMethodID(jClass, "Invoke", "()V");
    if (env->ExceptionCheck()) return;

    /* ISelectionProvider */
    mid_GetSelection = env->GetMethodID(jClass, "GetSelection", "()[J");
    if (env->ExceptionCheck()) return;
    mid_get_CanSelectMultiple = env->GetMethodID(jClass, "get_CanSelectMultiple", "()Z");
    if (env->ExceptionCheck()) return;
    mid_get_IsSelectionRequired = env->GetMethodID(jClass, "get_IsSelectionRequired", "()Z");
    if (env->ExceptionCheck()) return;

    /* ISelectionItemProvider */
    mid_Select = env->GetMethodID(jClass, "Select", "()V");
    if (env->ExceptionCheck()) return;
    mid_AddToSelection = env->GetMethodID(jClass, "AddToSelection", "()V");
    if (env->ExceptionCheck()) return;
    mid_RemoveFromSelection = env->GetMethodID(jClass, "RemoveFromSelection", "()V");
    if (env->ExceptionCheck()) return;
    mid_get_IsSelected = env->GetMethodID(jClass, "get_IsSelected", "()Z");
    if (env->ExceptionCheck()) return;
    mid_get_SelectionContainer = env->GetMethodID(jClass, "get_SelectionContainer", "()J");
    if (env->ExceptionCheck()) return;

    /* IRangeValueProvider */
    mid_SetValue = env->GetMethodID(jClass, "SetValue", "(D)V");
    if (env->ExceptionCheck()) return;
    mid_get_Value = env->GetMethodID(jClass, "get_Value", "()D");
    if (env->ExceptionCheck()) return;
    mid_get_IsReadOnly = env->GetMethodID(jClass, "get_IsReadOnly", "()Z");
    if (env->ExceptionCheck()) return;
    mid_get_Maximum = env->GetMethodID(jClass, "get_Maximum", "()D");
    if (env->ExceptionCheck()) return;
    mid_get_Minimum = env->GetMethodID(jClass, "get_Minimum", "()D");
    if (env->ExceptionCheck()) return;
    mid_get_LargeChange = env->GetMethodID(jClass, "get_LargeChange", "()D");
    if (env->ExceptionCheck()) return;
    mid_get_SmallChange = env->GetMethodID(jClass, "get_SmallChange", "()D");
    if (env->ExceptionCheck()) return;

    /* IValueProvider */
    mid_SetValueString = env->GetMethodID(jClass, "SetValueString", "(Ljava/lang/String;)V");
    if (env->ExceptionCheck()) return;
    mid_get_ValueString = env->GetMethodID(jClass, "get_ValueString", "()Ljava/lang/String;");
    if (env->ExceptionCheck()) return;

    /* IValueProvider */
    mid_GetVisibleRanges = env->GetMethodID(jClass, "GetVisibleRanges", "()[J");
    if (env->ExceptionCheck()) return;
    mid_RangeFromChild = env->GetMethodID(jClass, "RangeFromChild", "(J)J");
    if (env->ExceptionCheck()) return;
    mid_RangeFromPoint = env->GetMethodID(jClass, "RangeFromPoint", "(DD)J");
    if (env->ExceptionCheck()) return;
    mid_get_DocumentRange = env->GetMethodID(jClass, "get_DocumentRange", "()J");
    if (env->ExceptionCheck()) return;
    mid_get_SupportedTextSelection = env->GetMethodID(jClass, "get_SupportedTextSelection", "()I");
    if (env->ExceptionCheck()) return;

    /* IGridProvider */
    mid_get_ColumnCount = env->GetMethodID(jClass, "get_ColumnCount", "()I");
    if (env->ExceptionCheck()) return;
    mid_get_RowCount = env->GetMethodID(jClass, "get_RowCount", "()I");
    if (env->ExceptionCheck()) return;
    mid_GetItem = env->GetMethodID(jClass, "GetItem", "(II)J");
    if (env->ExceptionCheck()) return;

    /* IGridItemProvider */
    mid_get_Column = env->GetMethodID(jClass, "get_Column", "()I");
    if (env->ExceptionCheck()) return;
    mid_get_ColumnSpan = env->GetMethodID(jClass, "get_ColumnSpan", "()I");
    if (env->ExceptionCheck()) return;
    mid_get_ContainingGrid = env->GetMethodID(jClass, "get_ContainingGrid", "()J");
    if (env->ExceptionCheck()) return;
    mid_get_Row = env->GetMethodID(jClass, "get_Row", "()I");
    if (env->ExceptionCheck()) return;
    mid_get_RowSpan = env->GetMethodID(jClass, "get_RowSpan", "()I");
    if (env->ExceptionCheck()) return;

    /* ITableProvider */
    mid_GetColumnHeaders = env->GetMethodID(jClass, "GetColumnHeaders", "()[J");
    if (env->ExceptionCheck()) return;
    mid_GetRowHeaders = env->GetMethodID(jClass, "GetRowHeaders", "()[J");
    if (env->ExceptionCheck()) return;
    mid_get_RowOrColumnMajor = env->GetMethodID(jClass, "get_RowOrColumnMajor", "()I");
    if (env->ExceptionCheck()) return;

    /* ITableItemProvider */
    mid_GetColumnHeaderItems = env->GetMethodID(jClass, "GetColumnHeaderItems", "()[J");
    if (env->ExceptionCheck()) return;
    mid_GetRowHeaderItems = env->GetMethodID(jClass, "GetRowHeaderItems", "()[J");
    if (env->ExceptionCheck()) return;

    /* IToggleProvider */
    mid_Toggle = env->GetMethodID(jClass, "Toggle", "()V");
    if (env->ExceptionCheck()) return;
    mid_get_ToggleState = env->GetMethodID(jClass, "get_ToggleState", "()I");
    if (env->ExceptionCheck()) return;

    /* IExpandCollapseProvider */
    mid_Collapse= env->GetMethodID(jClass, "Collapse", "()V");
    if (env->ExceptionCheck()) return;
    mid_Expand = env->GetMethodID(jClass, "Expand", "()V");
    if (env->ExceptionCheck()) return;
    mid_get_ExpandCollapseState = env->GetMethodID(jClass, "get_ExpandCollapseState", "()I");
    if (env->ExceptionCheck()) return;

    /* ITransformProvider */
    mid_get_CanMove = env->GetMethodID(jClass, "get_CanMove", "()Z");
    if (env->ExceptionCheck()) return;
    mid_get_CanResize = env->GetMethodID(jClass, "get_CanResize", "()Z");
    if (env->ExceptionCheck()) return;
    mid_get_CanRotate = env->GetMethodID(jClass, "get_CanRotate", "()Z");
    if (env->ExceptionCheck()) return;
    mid_Move = env->GetMethodID(jClass, "Move", "(DD)V");
    if (env->ExceptionCheck()) return;
    mid_Resize = env->GetMethodID(jClass, "Resize", "(DD)V");
    if (env->ExceptionCheck()) return;
    mid_Rotate = env->GetMethodID(jClass, "Rotate", "(D)V");
    if (env->ExceptionCheck()) return;

    /* IScrollProvider */
    mid_Scroll = env->GetMethodID(jClass, "Scroll", "(II)V");
    if (env->ExceptionCheck()) return;
    mid_SetScrollPercent = env->GetMethodID(jClass, "SetScrollPercent", "(DD)V");
    if (env->ExceptionCheck()) return;
    mid_get_HorizontallyScrollable = env->GetMethodID(jClass, "get_HorizontallyScrollable", "()Z");
    if (env->ExceptionCheck()) return;
    mid_get_HorizontalScrollPercent = env->GetMethodID(jClass, "get_HorizontalScrollPercent", "()D");
    if (env->ExceptionCheck()) return;
    mid_get_HorizontalViewSize = env->GetMethodID(jClass, "get_HorizontalViewSize", "()D");
    if (env->ExceptionCheck()) return;
    mid_get_VerticallyScrollable = env->GetMethodID(jClass, "get_VerticallyScrollable", "()Z");
    if (env->ExceptionCheck()) return;
    mid_get_VerticalScrollPercent = env->GetMethodID(jClass, "get_VerticalScrollPercent", "()D");
    if (env->ExceptionCheck()) return;
    mid_get_VerticalViewSize = env->GetMethodID(jClass, "get_VerticalViewSize", "()D");
    if (env->ExceptionCheck()) return;
    
    /* IScrollItemProvider */
    mid_ScrollIntoView = env->GetMethodID(jClass, "ScrollIntoView", "()V");
    if (env->ExceptionCheck()) return;

    /* Variant */
    jclass jVariantClass = env->FindClass("com/sun/glass/ui/win/WinVariant");
    if (env->ExceptionCheck()) return;
    fid_vt = env->GetFieldID(jVariantClass, "vt", "S");
    if (env->ExceptionCheck()) return;
    fid_iVal= env->GetFieldID(jVariantClass, "iVal", "S");
    if (env->ExceptionCheck()) return;
    fid_lVal= env->GetFieldID(jVariantClass, "lVal", "I");
    if (env->ExceptionCheck()) return;
    fid_punkVal= env->GetFieldID(jVariantClass, "punkVal", "J");
    if (env->ExceptionCheck()) return;
    fid_fltVal= env->GetFieldID(jVariantClass, "fltVal", "F");
    if (env->ExceptionCheck()) return;
    fid_dblVal= env->GetFieldID(jVariantClass, "dblVal", "D");
    if (env->ExceptionCheck()) return;
    fid_boolVal= env->GetFieldID(jVariantClass, "boolVal", "Z");
    if (env->ExceptionCheck()) return;
    fid_bstrVal= env->GetFieldID(jVariantClass, "bstrVal", "Ljava/lang/String;");
    if (env->ExceptionCheck()) return;
    fid_pDblVal= env->GetFieldID(jVariantClass, "pDblVal", "[D");
    if (env->ExceptionCheck()) return;
}

/*
 * Class:     com_sun_glass_ui_win_WinAccessible
 * Method:    _createGlassAccessible
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinAccessible__1createGlassAccessible
  (JNIEnv *env, jobject jAccessible)
{
    GlassAccessible* acc = new (std::nothrow) GlassAccessible(env, jAccessible);
    return reinterpret_cast<jlong>(acc);
}

/*
 * Class:     com_sun_glass_ui_win_WinAccessible
 * Method:    _destroyGlassAccessible
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinAccessible__1destroyGlassAccessible
  (JNIEnv *env, jobject jAccessible, jlong winAccessible)
{
    GlassAccessible* acc = reinterpret_cast<GlassAccessible*>(winAccessible);
    acc->Release();
}

/*
 * Class:     com_sun_glass_ui_win_WinAccessible
 * Method:    UiaRaiseAutomationEvent
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinAccessible_UiaRaiseAutomationEvent
  (JNIEnv *env, jclass jClass, jlong jAccessible, jint id)
{
    IRawElementProviderSimple* pProvider = reinterpret_cast<IRawElementProviderSimple*>(jAccessible);
    return (jlong)UiaRaiseAutomationEvent(pProvider, (EVENTID)id);
}

/*
 * Class:     com_sun_glass_ui_win_WinAccessible
 * Method:    UiaRaiseAutomationPropertyChangedEvent
 * Signature: (JILcom/sun/glass/ui/win/WinVariant;Lcom/sun/glass/ui/win/WinVariant;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinAccessible_UiaRaiseAutomationPropertyChangedEvent
  (JNIEnv *env, jclass jClass, jlong jAccessible, jint id, jobject oldV, jobject newV)
{
    IRawElementProviderSimple* pProvider = reinterpret_cast<IRawElementProviderSimple*>(jAccessible);
    VARIANT ov = {0}, nv = {0};
    HRESULT hr = E_FAIL;

    hr = GlassAccessible::copyVariant(env, oldV, &ov);
    if (FAILED(hr)) return (jlong)hr;
    hr = GlassAccessible::copyVariant(env, newV, &nv);
    if (FAILED(hr)) return (jlong)hr;

    return (jlong)UiaRaiseAutomationPropertyChangedEvent(pProvider, (PROPERTYID)id, ov, nv);
}

