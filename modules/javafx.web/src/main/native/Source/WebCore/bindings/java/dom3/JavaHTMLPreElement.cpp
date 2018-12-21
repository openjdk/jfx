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
#include <WebCore/HTMLPreElement.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLPreElement*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLPreElementImpl_getWidthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->getIntegralAttribute(WebCore::HTMLNames::widthAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLPreElementImpl_setWidthImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setIntegralAttribute(WebCore::HTMLNames::widthAttr, value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLPreElementImpl_getWrapImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::wrapAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLPreElementImpl_setWrapImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::wrapAttr, value);
}

}
