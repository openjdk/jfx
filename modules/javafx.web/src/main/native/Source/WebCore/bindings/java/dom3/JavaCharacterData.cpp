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

#include <WebCore/CharacterData.h>
#include "DOMException.h"
#include <WebCore/Element.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<CharacterData*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_getDataImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->data());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_setDataImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setData(String(env, value));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_getLengthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_getPreviousElementSiblingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->previousElementSibling()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_getNextElementSiblingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->nextElementSibling()));
}


// Functions
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_substringDataImpl(JNIEnv* env, jclass, jlong peer
    , jint offset
    , jint length)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, raiseOnDOMError(env, IMPL->substringData(offset
            , length)));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_appendDataImpl(JNIEnv* env, jclass, jlong peer
    , jstring data)
{
    WebCore::JSMainThreadNullState state;
    IMPL->appendData(String(env, data));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_insertDataImpl(JNIEnv* env, jclass, jlong peer
    , jint offset
    , jstring data)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->insertData(offset
            , String(env, data)));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_deleteDataImpl(JNIEnv* env, jclass, jlong peer
    , jint offset
    , jint length)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->deleteData(offset
            , length));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_replaceDataImpl(JNIEnv* env, jclass, jlong peer
    , jint offset
    , jint length
    , jstring data)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->replaceData(offset
            , length
            , String(env, data)));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_CharacterDataImpl_removeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->remove());
}


}
