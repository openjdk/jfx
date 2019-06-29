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
#include <WebCore/DOMSelection.h>
#include <WebCore/Node.h>
#include <WebCore/Range.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<DOMSelection*>(jlong_to_ptr(peer)))

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_dispose(JNIEnv* env, jclass, jlong peer)
{
    IMPL->deref();
}


// Attributes
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getAnchorNodeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->anchorNode()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getAnchorOffsetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->anchorOffset();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getFocusNodeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->focusNode()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getFocusOffsetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->focusOffset();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getIsCollapsedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->isCollapsed();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getRangeCountImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->rangeCount();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getBaseNodeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->baseNode()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getBaseOffsetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->baseOffset();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getExtentNodeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->extentNode()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getExtentOffsetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->extentOffset();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getTypeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->type());
}


// Functions
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_collapseImpl(JNIEnv* env, jclass, jlong peer
    , jlong node
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->collapse(static_cast<Node*>(jlong_to_ptr(node))
            , index));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_collapseToEndImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->collapseToEnd());
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_collapseToStartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->collapseToStart());
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_deleteFromDocumentImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->deleteFromDocument();
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_containsNodeImpl(JNIEnv* env, jclass, jlong peer
    , jlong node
    , jboolean allowPartial)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->containsNode(static_cast<Node*>(jlong_to_ptr(node))
            , allowPartial);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_selectAllChildrenImpl(JNIEnv* env, jclass, jlong peer
    , jlong node)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->selectAllChildren(static_cast<Node*>(jlong_to_ptr(node))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_extendImpl(JNIEnv* env, jclass, jlong peer
    , jlong node
    , jint offset)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->extend(static_cast<Node*>(jlong_to_ptr(node))
            , offset));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_getRangeAtImpl(JNIEnv* env, jclass, jlong peer
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Range>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->getRangeAt(index))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_removeAllRangesImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->removeAllRanges();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_addRangeImpl(JNIEnv* env, jclass, jlong peer
    , jlong range)
{
    WebCore::JSMainThreadNullState state;
    IMPL->addRange(static_cast<Range*>(jlong_to_ptr(range)));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_modifyImpl(JNIEnv* env, jclass, jlong peer
    , jstring alter
    , jstring direction
    , jstring granularity)
{
    WebCore::JSMainThreadNullState state;
    IMPL->modify(String(env, alter)
            , String(env, direction)
            , String(env, granularity));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_setBaseAndExtentImpl(JNIEnv* env, jclass, jlong peer
    , jlong baseNode
    , jint baseOffset
    , jlong extentNode
    , jint extentOffset)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->setBaseAndExtent(static_cast<Node*>(jlong_to_ptr(baseNode))
            , baseOffset
            , static_cast<Node*>(jlong_to_ptr(extentNode))
            , extentOffset));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_setPositionImpl(JNIEnv* env, jclass, jlong peer
    , jlong node
    , jint offset)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->setPosition(static_cast<Node*>(jlong_to_ptr(node))
            , offset));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMSelectionImpl_emptyImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->empty();
}


}
