/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
#include "com_sun_glass_ui_win_WinTextRangeProvider.h"
#include "GlassTextRangeProvider.h"

/* TextRangeProvider Method IDs */
static jmethodID mid_Clone;
static jmethodID mid_Compare;
static jmethodID mid_CompareEndpoints;
static jmethodID mid_ExpandToEnclosingUnit;
static jmethodID mid_FindAttribute;
static jmethodID mid_FindText;
static jmethodID mid_GetAttributeValue;
static jmethodID mid_GetBoundingRectangles;
static jmethodID mid_GetEnclosingElement;
static jmethodID mid_GetText;
static jmethodID mid_Move;
static jmethodID mid_MoveEndpointByUnit;
static jmethodID mid_MoveEndpointByRange;
static jmethodID mid_Select;
static jmethodID mid_AddToSelection;
static jmethodID mid_RemoveFromSelection;
static jmethodID mid_ScrollIntoView;
static jmethodID mid_GetChildren;

GlassTextRangeProvider::GlassTextRangeProvider(JNIEnv* env, jobject jTextRangeProvider, GlassAccessible* glassAccessible)
: m_refCount(1)
{
    m_jTextRangeProvider = env->NewGlobalRef(jTextRangeProvider);
    m_glassAccessible = glassAccessible;
    m_glassAccessible->AddRef();
}

GlassTextRangeProvider::~GlassTextRangeProvider()
{
    JNIEnv* env = GetEnv();
    if (env) env->DeleteGlobalRef(m_jTextRangeProvider);
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
    JNIEnv* env = GetEnv();
    jlong ptr = env->CallLongMethod(m_jTextRangeProvider, mid_Clone);
    CheckAndClearException(env);

    ITextRangeProvider* iUnknown = reinterpret_cast<ITextRangeProvider*>(ptr);
//    if (iUnknown) iUnknown->AddRef(); /* The refcount of the a new object is one. Do we want to keep a reference to this guy ? */
    *pRetVal = iUnknown;
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::Compare(ITextRangeProvider *range, BOOL *pRetVal)
{
    GlassTextRangeProvider* glassRange = reinterpret_cast<GlassTextRangeProvider*>(range);
    if (glassRange == NULL ||  glassRange->m_jTextRangeProvider == NULL) {
        fprintf(stdout, "GlassTextRangeProvider::Compare() failed to map range\n");
        fflush(stdout);
        *pRetVal = FALSE; /* Can't compare against an unknown object */
        return S_OK;
    }
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallBooleanMethod(m_jTextRangeProvider, mid_Compare, glassRange->m_jTextRangeProvider);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::CompareEndpoints(TextPatternRangeEndpoint endpoint, ITextRangeProvider *targetRange,
                                                        TextPatternRangeEndpoint targetEndpoint, int *pRetVal)
{
    GlassTextRangeProvider* glassRange = reinterpret_cast<GlassTextRangeProvider*>(targetRange);
    if (glassRange == NULL ||  glassRange->m_jTextRangeProvider == NULL) {
        fprintf(stdout, "GlassTextRangeProvider::CompareEndpoints() failed to map range\n");
        fflush(stdout);
        *pRetVal = FALSE; /* Can't compare against an unknown object */
        return S_OK;
    }
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jTextRangeProvider, mid_CompareEndpoints, endpoint, glassRange->m_jTextRangeProvider, targetEndpoint);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::ExpandToEnclosingUnit(TextUnit unit)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jTextRangeProvider, mid_ExpandToEnclosingUnit, unit);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::FindAttribute(TEXTATTRIBUTEID attributeId, VARIANT val, BOOL backward, ITextRangeProvider **pRetVal)
{
    //TODO VAL TO JVAL
    jobject jVal = NULL;
    JNIEnv* env = GetEnv();
    jlong ptr = env->CallLongMethod(m_jTextRangeProvider, mid_FindAttribute, attributeId, jVal, backward);
    CheckAndClearException(env);

    //TODO AddRef ptr
    *pRetVal = reinterpret_cast<ITextRangeProvider*>(ptr);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::FindText(BSTR text, BOOL backward, BOOL ignoreCase, ITextRangeProvider **pRetVal)
{
    //TODO TEXT TO JTEXT
    jstring jText = NULL;
    JNIEnv* env = GetEnv();
    jlong ptr = env->CallLongMethod(m_jTextRangeProvider, mid_FindText, jText, backward, ignoreCase);
    CheckAndClearException(env);

    //TODO AddRef ptr
    *pRetVal = reinterpret_cast<ITextRangeProvider*>(ptr);
    return S_OK;
}
IFACEMETHODIMP GlassTextRangeProvider::GetAttributeValue(TEXTATTRIBUTEID attributeId, VARIANT *pRetVal)
{
    JNIEnv* env = GetEnv();
    jobject jval = env->CallObjectMethod(m_jTextRangeProvider, mid_GetAttributeValue, attributeId);
    CheckAndClearException(env);

    //TODO jval to val
//    *pRetVal =
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::GetBoundingRectangles(SAFEARRAY * *pRetVal)
{
    JNIEnv* env = GetEnv();
    jarray bounds = (jarray)env->CallObjectMethod(m_jTextRangeProvider, mid_GetBoundingRectangles);
    CheckAndClearException(env);

    //TODO bounds to SAFEARRAY
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::GetEnclosingElement(IRawElementProviderSimple **pRetVal)
{
    JNIEnv* env = GetEnv();
    jlong ptr = env->CallLongMethod(m_jTextRangeProvider, mid_GetEnclosingElement);
    CheckAndClearException(env);

    //TODO AddRef ptr
    *pRetVal = reinterpret_cast<IRawElementProviderSimple*>(ptr);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::GetText(int maxLength, BSTR *pRetVal)
{
    JNIEnv* env = GetEnv();
    jstring string = (jstring)env->CallObjectMethod(m_jTextRangeProvider, mid_GetText, maxLength);
    CheckAndClearException(env);

    //TODO String to BSTR*
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::Move(TextUnit unit, int count, int *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jTextRangeProvider, mid_Move, unit, count);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::MoveEndpointByUnit(TextPatternRangeEndpoint endpoint, TextUnit unit, int count, int *pRetVal)
{
    JNIEnv* env = GetEnv();
    *pRetVal = env->CallIntMethod(m_jTextRangeProvider, mid_MoveEndpointByUnit, endpoint, unit, count);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::MoveEndpointByRange(TextPatternRangeEndpoint endpoint, ITextRangeProvider *targetRange,
                                                           TextPatternRangeEndpoint targetEndpoint)
{

    GlassTextRangeProvider* glassRange = reinterpret_cast<GlassTextRangeProvider*>(targetRange);//TODO is that okay ?
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jTextRangeProvider, mid_MoveEndpointByRange, endpoint, glassRange->m_jTextRangeProvider, targetEndpoint);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::Select()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jTextRangeProvider, mid_Select);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::AddToSelection()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jTextRangeProvider, mid_AddToSelection);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::RemoveFromSelection()
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jTextRangeProvider, mid_RemoveFromSelection);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::ScrollIntoView(BOOL alignToTop)
{
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(m_jTextRangeProvider, mid_ScrollIntoView, alignToTop);
    CheckAndClearException(env);
    return S_OK;
}

IFACEMETHODIMP GlassTextRangeProvider::GetChildren(SAFEARRAY * *pRetVal)
{
    JNIEnv* env = GetEnv();
    jarray children = (jarray)env->CallObjectMethod(m_jTextRangeProvider, mid_GetChildren);
    CheckAndClearException(env);

    //TODO children to SAFEARRAY
    return S_OK;
}

/***********************************************/
/*                  JNI                        */
/***********************************************/

/*
 * Class:     com_sun_glass_ui_win_WinTextRangeProvider
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinTextRangeProvider__1initIDs
  (JNIEnv *env, jclass jClass)
{
    mid_Clone = env->GetMethodID(jClass, "Clone", "()J");
    mid_Compare = env->GetMethodID(jClass, "Compare", "(Lcom/sun/glass/ui/win/WinTextRangeProvider;)Z");
    mid_CompareEndpoints = env->GetMethodID(jClass, "CompareEndpoints", "(ILcom/sun/glass/ui/win/WinTextRangeProvider;I)I");
    mid_ExpandToEnclosingUnit = env->GetMethodID(jClass, "ExpandToEnclosingUnit", "(I)V");
    mid_FindAttribute = env->GetMethodID(jClass, "FindAttribute", "(ILcom/sun/glass/ui/win/WinVariant;Z)J");
    mid_FindText = env->GetMethodID(jClass, "FindText", "(Ljava/lang/String;ZZ)J");
    mid_GetAttributeValue = env->GetMethodID(jClass, "GetAttributeValue", "(I)Lcom/sun/glass/ui/win/WinVariant;");
    mid_GetBoundingRectangles = env->GetMethodID(jClass, "GetBoundingRectangles", "()[D");
    mid_GetEnclosingElement = env->GetMethodID(jClass, "GetEnclosingElement", "()J");
    mid_GetText = env->GetMethodID(jClass, "GetText", "(I)Ljava/lang/String;");
    mid_Move = env->GetMethodID(jClass, "Move", "(II)I");
    mid_MoveEndpointByUnit = env->GetMethodID(jClass, "MoveEndpointByUnit", "(III)I");
    mid_MoveEndpointByRange = env->GetMethodID(jClass, "MoveEndpointByRange", "(ILcom/sun/glass/ui/win/WinTextRangeProvider;I)V");
    mid_Select = env->GetMethodID(jClass, "Select", "()V");
    mid_AddToSelection = env->GetMethodID(jClass, "AddToSelection", "()V");
    mid_RemoveFromSelection = env->GetMethodID(jClass, "RemoveFromSelection", "()V");
    mid_ScrollIntoView = env->GetMethodID(jClass, "ScrollIntoView", "(Z)V");
    mid_GetChildren = env->GetMethodID(jClass, "GetChildren", "()[J");
}

/*
 * Class:     com_sun_glass_ui_win_WinTextRangeProvider
 * Method:    _createTextRangeProvider
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinTextRangeProvider__1createTextRangeProvider
  (JNIEnv *env, jobject jTextRangeProvider, jlong glassAccessible)
{
    GlassAccessible* acc = reinterpret_cast<GlassAccessible*>(glassAccessible);
    if (acc == NULL) return NULL;
    GlassTextRangeProvider* provider = new (std::nothrow) GlassTextRangeProvider(env, jTextRangeProvider, acc);
    return reinterpret_cast<jlong>(provider);
}

/*
 * Class:     com_sun_glass_ui_win_WinTextRangeProvider
 * Method:    _destroyTextRangeProvider
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinTextRangeProvider__1destroyTextRangeProvider
  (JNIEnv *env, jobject object, jlong provider)
{
    GlassTextRangeProvider* p = reinterpret_cast<GlassTextRangeProvider*>(provider);
    p->Release();
}
