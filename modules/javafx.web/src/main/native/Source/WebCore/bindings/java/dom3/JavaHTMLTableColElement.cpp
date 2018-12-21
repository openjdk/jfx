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

#include <WebCore/HTMLNames.h>
#include <WebCore/HTMLTableColElement.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLTableColElement*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_getAlignImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::alignAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_setAlignImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::alignAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_getChImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::charAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_setChImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::charAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_getChOffImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::charoffAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_setChOffImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::charoffAttr, String(env, value));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_getSpanImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->span();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_setSpanImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSpan(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_getVAlignImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::valignAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_setVAlignImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::valignAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_getWidthImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::widthAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableColElementImpl_setWidthImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::widthAttr, String(env, value));
}

}
