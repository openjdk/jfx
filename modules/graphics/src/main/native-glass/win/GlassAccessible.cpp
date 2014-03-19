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

/* static */ void GlassAccessible::copyVariant(JNIEnv *env, jobject jVariant, VARIANT* pRetVal)
{
    if (pRetVal == NULL) return;
    if (jVariant == NULL) {
        pRetVal->vt = VT_EMPTY;
        return;
    }
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
                pRetVal->vt = VT_EMPTY;
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
            if (str != NULL) {
                UINT length = env->GetStringLength(str);
                const jchar* ptr = env->GetStringCritical(str, NULL);
                pRetVal->bstrVal = SysAllocStringLen(reinterpret_cast<const OLECHAR *>(ptr), length);
                env->ReleaseStringCritical(str, ptr);
            } else {
                pRetVal->vt = VT_EMPTY;
            }
            break;
        }
        case VT_R8 | VT_ARRAY: {
            jarray list = (jarray)env->GetObjectField(jVariant, fid_pDblVal);
            pRetVal->parray = NULL;
            if (list) {
                jsize size = env->GetArrayLength(list);
                SAFEARRAY *psa = SafeArrayCreateVector(VT_R8, 0, size);
                if (psa) {
                    jdouble* listPtr = (jdouble*)env->GetPrimitiveArrayCritical(list, 0);
                    for (LONG i = 0; i < size; i++) {
                        SafeArrayPutElement(psa, &i, (void*)&(listPtr[i]));
                    }
                    env->ReleasePrimitiveArrayCritical(list, listPtr, 0);
                    pRetVal->parray = psa;
                }
            }
            if (pRetVal->parray == NULL) {
                pRetVal->vt = VT_EMPTY;
            }
            break;
        }
    }
}

jlong GlassAccessible::callLongMethod(jmethodID mid, ...)
{
    va_list vl;
    va_start(vl, mid);
    JNIEnv* env = GetEnv();
    jlong ptr = env->CallLongMethodV(m_jAccessible, mid, vl);
    CheckAndClearException(env);
    va_end(vl);

    /* AddRef the result */
    IUnknown* iUnknown = reinterpret_cast<IUnknown*>(ptr);
    if (iUnknown) iUnknown->AddRef();

    return ptr;
}

SAFEARRAY* GlassAccessible::callArrayMethod(jmethodID mid, VARTYPE vt)
{
    JNIEnv* env = GetEnv();
    jarray list = (jarray)env->CallObjectMethod(m_jAccessible, mid);
    CheckAndClearException(env);

    if (list) {
        jsize size = env->GetArrayLength(list);
        SAFEARRAY *psa = SafeArrayCreateVector(vt, 0, size);
        if (psa) {
            jint* listPtr = (jint*)env->GetPrimitiveArrayCritical(list, 0);
            for (LONG i = 0; i < size; i++) {
                if (vt == VT_UNKNOWN) {
                    //TODO make sure AddRef on elements is not required ?
                    SafeArrayPutElement(psa, &i, (void*)listPtr[i]);
                } else {
                    SafeArrayPutElement(psa, &i, (void*)&(listPtr[i]));
                }

            }
            env->ReleasePrimitiveArrayCritical(list, listPtr, 0);
        }
        return psa;
    }
    return NULL;
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
    CheckAndClearException(env);
    
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
    jlong ptr = callLongMethod(mid_GetPatternProvider, patternId);
    *pRetVal = reinterpret_cast<IUnknown*>(ptr);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::GetPropertyValue(PROPERTYID propertyId, VARIANT* pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    JNIEnv* env = GetEnv();
    jobject jVariant = env->CallObjectMethod(m_jAccessible, mid_GetPropertyValue, propertyId);
    CheckAndClearException(env);

    copyVariant(env, jVariant, pRetVal);
    return S_OK;
}

/***********************************************/
/*       IRawElementProviderFragment           */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_BoundingRectangle(UiaRect *pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    JNIEnv* env = GetEnv();
    jfloatArray bounds = (jfloatArray)env->CallObjectMethod(m_jAccessible, mid_get_BoundingRectangle);
    CheckAndClearException(env);

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
    jlong ptr = callLongMethod(mid_get_FragmentRoot);
    *pRetVal = reinterpret_cast<IRawElementProviderFragmentRoot*>(ptr);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::GetEmbeddedFragmentRoots(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    *pRetVal = callArrayMethod(mid_GetEmbeddedFragmentRoots, VT_UNKNOWN);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::GetRuntimeId(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    *pRetVal = callArrayMethod(mid_GetRuntimeId, VT_I4);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::Navigate(NavigateDirection direction, IRawElementProviderFragment **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    jlong ptr = callLongMethod(mid_Navigate, direction);
    *pRetVal = reinterpret_cast<IRawElementProviderFragment*>(ptr);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::SetFocus()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_SetFocus);
    CheckAndClearException(env);
    return S_OK;
}

/***********************************************/
/*     IRawElementProviderFragmentRoot         */
/***********************************************/
IFACEMETHODIMP GlassAccessible::ElementProviderFromPoint(double x, double y, IRawElementProviderFragment **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    jlong ptr = callLongMethod(mid_ElementProviderFromPoint, x, y);
    *pRetVal = reinterpret_cast<IRawElementProviderFragment*>(ptr);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::GetFocus(IRawElementProviderFragment **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    jlong ptr = callLongMethod(mid_GetFocus);
    *pRetVal = reinterpret_cast<IRawElementProviderFragment*>(ptr);
    return S_OK;
}

/***********************************************/
/*             IInvokeProvider                 */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Invoke()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Invoke);
    CheckAndClearException(env);
    return S_OK;
}

/***********************************************/
/*           ISelectionProvider                */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetSelection(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    *pRetVal = callArrayMethod(mid_GetSelection, VT_UNKNOWN);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_CanSelectMultiple(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_CanSelectMultiple);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_IsSelectionRequired(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_IsSelectionRequired);
    CheckAndClearException(env);
    return S_OK;
}

/***********************************************/
/*         ISelectionItemProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Select()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Select);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::AddToSelection()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_AddToSelection);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::RemoveFromSelection()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_RemoveFromSelection);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_IsSelected(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_IsSelected);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_SelectionContainer(IRawElementProviderSimple **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    jlong ptr = callLongMethod(mid_get_SelectionContainer);
    *pRetVal = reinterpret_cast<IRawElementProviderSimple*>(ptr);
    return S_OK;
}

/***********************************************/
/*           IRangeValueProvider               */
/***********************************************/
IFACEMETHODIMP GlassAccessible::SetValue(double val)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_SetValue, val);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_Value(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_Value);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_IsReadOnly(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_IsReadOnly);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_Maximum(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_Maximum);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_Minimum(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_Minimum);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_LargeChange(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_LargeChange);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_SmallChange(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_SmallChange);
    CheckAndClearException(env);
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
    env->CallVoidMethod(m_jAccessible, mid_SetValueString, str);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_Value(BSTR *pRetVal)
{
    JNIEnv* env = GetEnv();
    jstring str = (jstring)env->CallObjectMethod(m_jAccessible, mid_get_ValueString);
    CheckAndClearException(env);
    if (str) {
        UINT length = env->GetStringLength(str);
        const jchar* ptr = env->GetStringCritical(str, NULL);
        *pRetVal = SysAllocStringLen(reinterpret_cast<const OLECHAR *>(ptr), length);
        env->ReleaseStringCritical(str, ptr);
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
    *pRetVal = callArrayMethod(mid_GetVisibleRanges, VT_UNKNOWN);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::RangeFromChild(IRawElementProviderSimple *childElement,  ITextRangeProvider **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    jlong ptr = callLongMethod(mid_RangeFromChild, (jlong)childElement);
    *pRetVal = reinterpret_cast<ITextRangeProvider*>(ptr);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::RangeFromPoint(UiaPoint point, ITextRangeProvider **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    jlong ptr = callLongMethod(mid_RangeFromPoint, point.x, point.y);
    *pRetVal = reinterpret_cast<ITextRangeProvider*>(ptr);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_DocumentRange(ITextRangeProvider **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    jlong ptr = callLongMethod(mid_get_DocumentRange);
    *pRetVal = reinterpret_cast<ITextRangeProvider*>(ptr);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_SupportedTextSelection(SupportedTextSelection *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = (SupportedTextSelection)env->CallIntMethod(m_jAccessible, mid_get_SupportedTextSelection);
    CheckAndClearException(env);
    return S_OK;
}

/***********************************************/
/*              IGridProvider                  */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_ColumnCount(int *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_ColumnCount);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_RowCount(int *pRetVal) {
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_RowCount);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::GetItem(int row, int column, IRawElementProviderSimple **pRetVal) {
    if (pRetVal == NULL) return E_INVALIDARG;
    jlong ptr = callLongMethod(mid_GetItem, row, column);
    *pRetVal = reinterpret_cast<IRawElementProviderSimple*>(ptr);
    return S_OK;
}

/***********************************************/
/*              IGridItemProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_Column(int *pRetVal) {
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_Column);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_ColumnSpan(int *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_ColumnSpan);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_ContainingGrid(IRawElementProviderSimple **pRetVal){
    if (pRetVal == NULL) return E_INVALIDARG;
    jlong ptr = callLongMethod(mid_get_ContainingGrid);
    *pRetVal = reinterpret_cast<IRawElementProviderSimple*>(ptr);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_Row(int *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_Row);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_RowSpan(int *pRetVal) {
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jAccessible, mid_get_RowSpan);
    CheckAndClearException(env);
    return S_OK;
}

/***********************************************/
/*              ITableProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetColumnHeaders(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    *pRetVal = callArrayMethod(mid_GetColumnHeaders, VT_UNKNOWN);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::GetRowHeaders(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    *pRetVal = callArrayMethod(mid_GetRowHeaders, VT_UNKNOWN);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_RowOrColumnMajor(RowOrColumnMajor *pRetVal) {
    JNIEnv* env = GetEnv();
    *pRetVal = (RowOrColumnMajor) env->CallIntMethod(m_jAccessible, mid_get_RowOrColumnMajor);
    CheckAndClearException(env);
    return S_OK;
}


/***********************************************/
/*              ITableItemProvider              */
/***********************************************/
IFACEMETHODIMP GlassAccessible::GetColumnHeaderItems(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    *pRetVal = callArrayMethod(mid_GetColumnHeaderItems, VT_UNKNOWN);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::GetRowHeaderItems(SAFEARRAY **pRetVal)
{
    if (pRetVal == NULL) return E_INVALIDARG;
    *pRetVal = callArrayMethod(mid_GetRowHeaderItems, VT_UNKNOWN);
    return S_OK;
}


/***********************************************/
/*              IToggleProvider                */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Toggle()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Toggle);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_ToggleState(ToggleState *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = (ToggleState) env->CallIntMethod(m_jAccessible, mid_get_ToggleState);
    CheckAndClearException(env);
    return S_OK;
}

/***********************************************/
/*         IExpandCollapseProvider             */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Collapse()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Collapse);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::Expand()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Expand);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_ExpandCollapseState(ExpandCollapseState *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = (ExpandCollapseState) env->CallIntMethod(m_jAccessible, mid_get_ExpandCollapseState);
    CheckAndClearException(env);
    return S_OK;
}

/***********************************************/
/*         ITransformProvider                  */
/***********************************************/
IFACEMETHODIMP GlassAccessible::get_CanMove(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_CanMove);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_CanResize(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_CanResize);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_CanRotate(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_CanRotate);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::Move(double x, double y)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Move, x, y);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::Resize(double width, double height)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Resize, width, height);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::Rotate(double degrees)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Rotate, degrees);
    CheckAndClearException(env);
    return S_OK;
}

/***********************************************/
/*         IScrollProvider                     */
/***********************************************/
IFACEMETHODIMP GlassAccessible::Scroll(ScrollAmount horizontalAmount, ScrollAmount verticalAmount)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_Scroll, horizontalAmount, verticalAmount);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::SetScrollPercent(double horizontalPercent, double verticalPercent)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jAccessible, mid_SetScrollPercent, horizontalPercent, verticalPercent);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_HorizontallyScrollable(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_HorizontallyScrollable);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_HorizontalScrollPercent(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_HorizontalScrollPercent);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_HorizontalViewSize(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_HorizontalViewSize);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_VerticallyScrollable(BOOL *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jAccessible, mid_get_VerticallyScrollable);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_VerticalScrollPercent(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_VerticalScrollPercent);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassAccessible::get_VerticalViewSize(double *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallDoubleMethod(m_jAccessible, mid_get_VerticalViewSize);
    CheckAndClearException(env);
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
    mid_get_HostRawElementProvider = env->GetMethodID(jClass, "get_HostRawElementProvider", "()J");
    mid_GetPropertyValue = env->GetMethodID(jClass, "GetPropertyValue", "(I)Lcom/sun/glass/ui/win/WinVariant;");

    /* IRawElementProviderFragment */
    mid_get_BoundingRectangle = env->GetMethodID(jClass, "get_BoundingRectangle", "()[F");
    mid_get_FragmentRoot = env->GetMethodID(jClass, "get_FragmentRoot", "()J");
    mid_GetEmbeddedFragmentRoots = env->GetMethodID(jClass, "GetEmbeddedFragmentRoots", "()[J");
    mid_GetRuntimeId = env->GetMethodID(jClass, "GetRuntimeId", "()[I");
    mid_Navigate = env->GetMethodID(jClass, "Navigate", "(I)J");
    mid_SetFocus = env->GetMethodID(jClass, "SetFocus", "()V");

    /* IRawElementProviderFragmentRoot */
    mid_ElementProviderFromPoint = env->GetMethodID(jClass, "ElementProviderFromPoint", "(DD)J");
    mid_GetFocus = env->GetMethodID(jClass, "GetFocus", "()J");

    /* IInvokeProvider */
    mid_Invoke = env->GetMethodID(jClass, "Invoke", "()V");

    /* ISelectionProvider */
    mid_GetSelection = env->GetMethodID(jClass, "GetSelection", "()[J");
    mid_get_CanSelectMultiple = env->GetMethodID(jClass, "get_CanSelectMultiple", "()Z");
    mid_get_IsSelectionRequired = env->GetMethodID(jClass, "get_IsSelectionRequired", "()Z");

    /* ISelectionItemProvider */
    mid_Select = env->GetMethodID(jClass, "Select", "()V");
    mid_AddToSelection = env->GetMethodID(jClass, "AddToSelection", "()V");
    mid_RemoveFromSelection = env->GetMethodID(jClass, "RemoveFromSelection", "()V");
    mid_get_IsSelected = env->GetMethodID(jClass, "get_IsSelected", "()Z");
    mid_get_SelectionContainer = env->GetMethodID(jClass, "get_SelectionContainer", "()J");

    /* IRangeValueProvider */
    mid_SetValue = env->GetMethodID(jClass, "SetValue", "(D)V");
    mid_get_Value = env->GetMethodID(jClass, "get_Value", "()D");
    mid_get_IsReadOnly = env->GetMethodID(jClass, "get_IsReadOnly", "()Z");
    mid_get_Maximum = env->GetMethodID(jClass, "get_Maximum", "()D");
    mid_get_Minimum = env->GetMethodID(jClass, "get_Minimum", "()D");
    mid_get_LargeChange = env->GetMethodID(jClass, "get_LargeChange", "()D");
    mid_get_SmallChange = env->GetMethodID(jClass, "get_SmallChange", "()D");

    /* IValueProvider */
    mid_SetValueString = env->GetMethodID(jClass, "SetValueString", "(Ljava/lang/String;)V");
    mid_get_ValueString = env->GetMethodID(jClass, "get_ValueString", "()Ljava/lang/String;");

    /* IValueProvider */
    mid_GetVisibleRanges = env->GetMethodID(jClass, "GetVisibleRanges", "()[J");
    mid_RangeFromChild = env->GetMethodID(jClass, "RangeFromChild", "(J)J");
    mid_RangeFromPoint = env->GetMethodID(jClass, "RangeFromPoint", "(DD)J");
    mid_get_DocumentRange = env->GetMethodID(jClass, "get_DocumentRange", "()J");
    mid_get_SupportedTextSelection = env->GetMethodID(jClass, "get_SupportedTextSelection", "()I");

    /* IGridProvider */
    mid_get_ColumnCount = env->GetMethodID(jClass, "get_ColumnCount", "()I");
    mid_get_RowCount = env->GetMethodID(jClass, "get_RowCount", "()I");
    mid_GetItem = env->GetMethodID(jClass, "GetItem", "(II)J");

    /* IGridItemProvider */
    mid_get_Column = env->GetMethodID(jClass, "get_Column", "()I");
    mid_get_ColumnSpan = env->GetMethodID(jClass, "get_ColumnSpan", "()I");
    mid_get_ContainingGrid = env->GetMethodID(jClass, "get_ContainingGrid", "()J");
    mid_get_Row = env->GetMethodID(jClass, "get_Row", "()I");
    mid_get_RowSpan = env->GetMethodID(jClass, "get_RowSpan", "()I");

    /* ITableProvider */
    mid_GetColumnHeaders = env->GetMethodID(jClass, "GetColumnHeaders", "()[J");
    mid_GetRowHeaders = env->GetMethodID(jClass, "GetRowHeaders", "()[J");
    mid_get_RowOrColumnMajor = env->GetMethodID(jClass, "get_RowOrColumnMajor", "()I");

    /* ITableItemProvider */
    mid_GetColumnHeaderItems = env->GetMethodID(jClass, "GetColumnHeaderItems", "()[J");
    mid_GetRowHeaderItems = env->GetMethodID(jClass, "GetRowHeaderItems", "()[J");

    /* IToggleProvider */
    mid_Toggle = env->GetMethodID(jClass, "Toggle", "()V");
    mid_get_ToggleState = env->GetMethodID(jClass, "get_ToggleState", "()I");

    /* IExpandCollapseProvider */
    mid_Collapse= env->GetMethodID(jClass, "Collapse", "()V");
    mid_Expand = env->GetMethodID(jClass, "Expand", "()V");
    mid_get_ExpandCollapseState = env->GetMethodID(jClass, "get_ExpandCollapseState", "()I");

    /* ITransformProvider */
    mid_get_CanMove = env->GetMethodID(jClass, "get_CanMove", "()Z");
    mid_get_CanResize = env->GetMethodID(jClass, "get_CanResize", "()Z");
    mid_get_CanRotate = env->GetMethodID(jClass, "get_CanRotate", "()Z");
    mid_Move = env->GetMethodID(jClass, "Move", "(DD)V");
    mid_Resize = env->GetMethodID(jClass, "Resize", "(DD)V");
    mid_Rotate = env->GetMethodID(jClass, "Rotate", "(D)V");

    /* IScrollProvider */
    mid_Scroll = env->GetMethodID(jClass, "Scroll", "(II)V");
    mid_SetScrollPercent = env->GetMethodID(jClass, "SetScrollPercent", "(DD)V");
    mid_get_HorizontallyScrollable = env->GetMethodID(jClass, "get_HorizontallyScrollable", "()Z");
    mid_get_HorizontalScrollPercent = env->GetMethodID(jClass, "get_HorizontalScrollPercent", "()D");
    mid_get_HorizontalViewSize = env->GetMethodID(jClass, "get_HorizontalViewSize", "()D");
    mid_get_VerticallyScrollable = env->GetMethodID(jClass, "get_VerticallyScrollable", "()Z");
    mid_get_VerticalScrollPercent = env->GetMethodID(jClass, "get_VerticalScrollPercent", "()D");
    mid_get_VerticalViewSize = env->GetMethodID(jClass, "get_VerticalViewSize", "()D");

    /* Variant */
    jclass jVariantClass = env->FindClass("com/sun/glass/ui/win/WinVariant");
    fid_vt = env->GetFieldID(jVariantClass, "vt", "S");
    fid_iVal= env->GetFieldID(jVariantClass, "iVal", "S");
    fid_lVal= env->GetFieldID(jVariantClass, "lVal", "I");
    fid_punkVal= env->GetFieldID(jVariantClass, "punkVal", "J");
    fid_fltVal= env->GetFieldID(jVariantClass, "fltVal", "F");
    fid_dblVal= env->GetFieldID(jVariantClass, "dblVal", "D");
    fid_boolVal= env->GetFieldID(jVariantClass, "boolVal", "Z");
    fid_bstrVal= env->GetFieldID(jVariantClass, "bstrVal", "Ljava/lang/String;");
    fid_pDblVal= env->GetFieldID(jVariantClass, "pDblVal", "[D");
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
//    delete acc;
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
    VARIANT ov, nv;
    
    GlassAccessible::copyVariant(env, oldV, &ov);
    GlassAccessible::copyVariant(env, newV, &nv);

    return (jlong)UiaRaiseAutomationPropertyChangedEvent(pProvider, (PROPERTYID)id, ov, nv);
}

