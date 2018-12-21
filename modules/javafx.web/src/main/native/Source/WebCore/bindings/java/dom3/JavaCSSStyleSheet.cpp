/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

#undef IMPL

#include "config.h"

#include <WebCore/CSSImportRule.h>
#include <WebCore/CSSRule.h>
#include <WebCore/CSSRuleList.h>
#include <WebCore/CSSStyleSheet.h>
#include "DOMException.h"
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<CSSStyleSheet*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_CSSStyleSheetImpl_getOwnerRuleImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<CSSRule>(env, WTF::getPtr(IMPL->ownerRule()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_CSSStyleSheetImpl_getCssRulesImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<CSSRuleList>(env, WTF::getPtr(IMPL->cssRules()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_CSSStyleSheetImpl_getRulesImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<CSSRuleList>(env, WTF::getPtr(IMPL->rules()));
}


// Functions
JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_CSSStyleSheetImpl_insertRuleImpl(JNIEnv* env, jclass, jlong peer
    , jstring rule
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    return raiseOnDOMError(env, IMPL->insertRule(String(env, rule)
            , index));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_CSSStyleSheetImpl_deleteRuleImpl(JNIEnv* env, jclass, jlong peer
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->deleteRule(index));
}


JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_CSSStyleSheetImpl_addRuleImpl(JNIEnv* env, jclass, jlong peer
    , jstring selector
    , jstring style
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    return raiseOnDOMError(env, IMPL->addRule(String(env, selector)
            , String(env, style)
            , index));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_CSSStyleSheetImpl_removeRuleImpl(JNIEnv* env, jclass, jlong peer
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->removeRule(index));
}


}
