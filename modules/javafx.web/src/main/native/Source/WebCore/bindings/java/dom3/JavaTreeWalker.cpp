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

#include "DOMException.h"
#include <WebCore/Node.h>
#include <WebCore/NodeFilter.h>
#include <WebCore/TreeWalker.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<TreeWalker*>(jlong_to_ptr(peer)))

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_dispose(JNIEnv*, jclass, jlong peer)
{
    IMPL->deref();
}


// Attributes
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_getRootImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->root()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_getWhatToShowImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->whatToShow();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_getFilterImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeFilter>(env, WTF::getPtr(IMPL->filter()));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_getExpandEntityReferencesImpl(JNIEnv*, jclass, jlong)
{
    WebCore::JSMainThreadNullState state;
    return JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_getCurrentNodeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->currentNode()));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_setCurrentNodeImpl(JNIEnv* env, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    if (!value) {
        raiseTypeErrorException(env);
        return;
    }
    IMPL->setCurrentNode(*static_cast<Node*>(jlong_to_ptr(value)));
}


// Functions
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_parentNodeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->parentNode();
    if (result.hasException()) {
        return {};
    }
    return JavaReturn<Node>(env, WTF::getPtr(result.releaseReturnValue()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_firstChildImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->firstChild();
    if (result.hasException()) {
        return {};
    }
    return JavaReturn<Node>(env, WTF::getPtr(result.releaseReturnValue()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_lastChildImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->lastChild();
    if (result.hasException()) {
        return {};
    }
    return JavaReturn<Node>(env, WTF::getPtr(result.releaseReturnValue()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_previousSiblingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->previousSibling();
    if (result.hasException()) {
        return {};
    }
    return JavaReturn<Node>(env, WTF::getPtr(result.releaseReturnValue()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_nextSiblingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->nextSibling();
    if (result.hasException()) {
        return {};
    }
    return JavaReturn<Node>(env, WTF::getPtr(result.releaseReturnValue()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_previousNodeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->previousNode();
    if (result.hasException()) {
        return {};
    }
    return JavaReturn<Node>(env, WTF::getPtr(result.releaseReturnValue()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_TreeWalkerImpl_nextNodeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;

    auto result = IMPL->nextNode();
    if (result.hasException()) {
        return {};
    }
    return JavaReturn<Node>(env, WTF::getPtr(result.releaseReturnValue()));
}


}
