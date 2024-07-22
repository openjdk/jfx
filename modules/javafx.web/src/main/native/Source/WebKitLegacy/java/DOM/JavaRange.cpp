/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
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
#include <WebCore/Document.h>
#include <WebCore/DocumentFragment.h>
#include <WebCore/Node.h>
#include <WebCore/Range.h>
#include <WebCore/SimpleRange.h>
#include <WebCore/TextIterator.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<WebCore::Range*>(jlong_to_ptr(peer)))

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_dispose(JNIEnv*, jclass, jlong peer)
{
    IMPL->deref();
}


// Attributes
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_RangeImpl_getStartContainerImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->startContainer()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_RangeImpl_getStartOffsetImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->startOffset();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_RangeImpl_getEndContainerImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->endContainer()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_RangeImpl_getEndOffsetImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->endOffset();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_RangeImpl_getCollapsedImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->collapsed();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_RangeImpl_getCommonAncestorContainerImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->commonAncestorContainer()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_RangeImpl_getTextImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;

    auto range = makeSimpleRange(*IMPL);
    range.start.document().updateLayout();
    return JavaReturn<String>(env, plainText(range));
}


// Functions
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_setStartImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode
    , jint offset)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->setStart(*static_cast<Node*>(jlong_to_ptr(refNode)), offset));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_setEndImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode
    , jint offset)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->setEnd(*static_cast<Node*>(jlong_to_ptr(refNode))
            , offset));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_setStartBeforeImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->setStartBefore(*static_cast<Node*>(jlong_to_ptr(refNode))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_setStartAfterImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->setStartAfter(*static_cast<Node*>(jlong_to_ptr(refNode))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_setEndBeforeImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->setEndBefore(*static_cast<Node*>(jlong_to_ptr(refNode))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_setEndAfterImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->setEndAfter(*static_cast<Node*>(jlong_to_ptr(refNode))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_collapseImpl(JNIEnv*, jclass, jlong peer
    , jboolean toStart)
{
    WebCore::JSMainThreadNullState state;
    IMPL->collapse(toStart);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_selectNodeImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->selectNode(*static_cast<Node*>(jlong_to_ptr(refNode))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_selectNodeContentsImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->selectNodeContents(*static_cast<Node*>(jlong_to_ptr(refNode))));
}


JNIEXPORT jshort JNICALL Java_com_sun_webkit_dom_RangeImpl_compareBoundaryPointsImpl(JNIEnv* env, jclass, jlong peer
    , jshort how
    , jlong sourceRange)
{
    WebCore::JSMainThreadNullState state;
    if (!sourceRange) {
        raiseTypeErrorException(env);
        return 0;
    }
    return raiseOnDOMError(env, IMPL->compareBoundaryPoints(static_cast<WebCore::Range::CompareHow>(how), *static_cast<WebCore::Range*>(jlong_to_ptr(sourceRange))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_deleteContentsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->deleteContents());
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_RangeImpl_extractContentsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DocumentFragment>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->extractContents())));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_RangeImpl_cloneContentsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DocumentFragment>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->cloneContents())));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_insertNodeImpl(JNIEnv* env, jclass, jlong peer
    , jlong newNode)
{
    WebCore::JSMainThreadNullState state;
    if (!newNode) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->insertNode(*static_cast<Node*>(jlong_to_ptr(newNode))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_surroundContentsImpl(JNIEnv* env, jclass, jlong peer
    , jlong newParent)
{
    WebCore::JSMainThreadNullState state;
    if (!newParent) {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->surroundContents(*static_cast<Node*>(jlong_to_ptr(newParent))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_RangeImpl_cloneRangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<WebCore::Range>(env, WTF::getPtr(IMPL->cloneRange()));
}


JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_RangeImpl_toStringImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->toString());
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_detachImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->detach();
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_RangeImpl_createContextualFragmentImpl(JNIEnv* env, jclass, jlong peer
    , jstring html)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DocumentFragment>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createContextualFragment(AtomString {String(env, html)}))));
}


JNIEXPORT jshort JNICALL Java_com_sun_webkit_dom_RangeImpl_compareNodeImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return 0;
    }
    return raiseOnDOMError(env, IMPL->compareNode(*static_cast<Node*>(jlong_to_ptr(refNode))));
}


JNIEXPORT jshort JNICALL Java_com_sun_webkit_dom_RangeImpl_comparePointImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode
    , jint offset)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return 0;
    }
    return raiseOnDOMError(env, IMPL->comparePoint(*static_cast<Node*>(jlong_to_ptr(refNode))
            , offset));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_RangeImpl_intersectsNodeImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return JNI_FALSE;
    }
    return IMPL->intersectsNode(*static_cast<Node*>(jlong_to_ptr(refNode)));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_RangeImpl_isPointInRangeImpl(JNIEnv* env, jclass, jlong peer
    , jlong refNode
    , jint offset)
{
    WebCore::JSMainThreadNullState state;
    if (!refNode) {
        raiseTypeErrorException(env);
        return JNI_FALSE;
    }
    return raiseOnDOMError(env, IMPL->isPointInRange(*static_cast<Node*>(jlong_to_ptr(refNode))
            , offset));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_RangeImpl_expandImpl(JNIEnv* env, jclass, jlong peer
    , jstring unit)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->expand(AtomString{String(env, unit)}));
}


}
