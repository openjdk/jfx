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
#include <WebCore/DocumentFragment.h>
#include <WebCore/Element.h>
#include <WebCore/HTMLCollection.h>
#include <WebCore/NodeList.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<DocumentFragment*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentFragmentImpl_getChildrenImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->children()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentFragmentImpl_getFirstElementChildImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->firstElementChild()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentFragmentImpl_getLastElementChildImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->lastElementChild()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DocumentFragmentImpl_getChildElementCountImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->childElementCount();
}


// Functions
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentFragmentImpl_getElementByIdImpl(JNIEnv* env, jclass, jlong peer
    , jstring elementId)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->getElementById(String(env, elementId))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentFragmentImpl_querySelectorImpl(JNIEnv* env, jclass, jlong peer
    , jstring selectors)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->querySelector(String(env, selectors)))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentFragmentImpl_querySelectorAllImpl(JNIEnv* env, jclass, jlong peer
    , jstring selectors)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->querySelectorAll(String(env, selectors)))));
}


}
