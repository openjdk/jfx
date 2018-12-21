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

#include <WebCore/DeprecatedCSSOMPrimitiveValue.h>
#include <WebCore/DeprecatedCSSOMCounter.h>
#include "DOMException.h"
#include <WebCore/DeprecatedCSSOMRGBColor.h>
#include <WebCore/DeprecatedCSSOMRect.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<DeprecatedCSSOMPrimitiveValue*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jshort JNICALL Java_com_sun_webkit_dom_CSSPrimitiveValueImpl_getPrimitiveTypeImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->primitiveType();
}


// Functions
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_CSSPrimitiveValueImpl_setFloatValueImpl(JNIEnv* env, jclass, jlong peer
    , jshort unitType
    , jfloat floatValue)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->setFloatValue(unitType
            , floatValue));
}


JNIEXPORT jfloat JNICALL Java_com_sun_webkit_dom_CSSPrimitiveValueImpl_getFloatValueImpl(JNIEnv* env, jclass, jlong peer
    , jshort unitType)
{
    WebCore::JSMainThreadNullState state;
    return raiseOnDOMError(env, IMPL->getFloatValue(unitType));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_CSSPrimitiveValueImpl_setStringValueImpl(JNIEnv* env, jclass, jlong peer
    , jshort stringType
    , jstring stringValue)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->setStringValue(stringType
            , String(env, stringValue)));
}


JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_CSSPrimitiveValueImpl_getStringValueImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, raiseOnDOMError(env, IMPL->getStringValue()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_CSSPrimitiveValueImpl_getCounterValueImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DeprecatedCSSOMCounter>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->getCounterValue())));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_CSSPrimitiveValueImpl_getRectValueImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DeprecatedCSSOMRect>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->getRectValue())));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_CSSPrimitiveValueImpl_getRGBColorValueImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DeprecatedCSSOMRGBColor>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->getRGBColorValue())));
}


}
