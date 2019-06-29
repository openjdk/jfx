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



#include <WebCore/DOMException.h>

#include <WebCore/HTMLOptGroupElement.h>
#include <WebCore/HTMLOptionElement.h>
#include <WebCore/HTMLOptionsCollection.h>
#include <WebCore/JSExecState.h>
#include <WebCore/Node.h>
#include <WebCore/ThreadCheck.h>
#include <wtf/GetPtr.h>
#include <wtf/RefPtr.h>
#include <wtf/URL.h>
#include <wtf/Variant.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLOptionsCollection*>(jlong_to_ptr(peer)))

// Attributes
JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLOptionsCollectionImpl_getSelectedIndexImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->selectedIndex();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLOptionsCollectionImpl_setSelectedIndexImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectedIndex(value);
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLOptionsCollectionImpl_getLengthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLOptionsCollectionImpl_setLengthImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setLength(value);
}


// Functions
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLOptionsCollectionImpl_namedItemImpl(JNIEnv* env, jclass, jlong peer
    , jstring name)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->namedItem(String(env, name))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLOptionsCollectionImpl_addImpl(JNIEnv* env, jclass, jlong peer
    , jlong option
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    if (!option) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->add(static_cast<HTMLOptionElement*>(jlong_to_ptr(option)), Optional<WebCore::HTMLOptionsCollection::HTMLElementOrInt> { static_cast<int>(index) }));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLOptionsCollectionImpl_itemImpl(JNIEnv* env, jclass, jlong peer
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->item(index)));
}


}
