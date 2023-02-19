/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include <WebCore/MediaList.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<MediaList*>(jlong_to_ptr(peer)))

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_MediaListImpl_dispose(JNIEnv*, jclass, jlong peer)
{
    IMPL->deref();
}


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_MediaListImpl_getMediaTextImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->mediaText());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_MediaListImpl_setMediaTextImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setMediaText(AtomString {String(env, value)});
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_MediaListImpl_getLengthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}


// Functions
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_MediaListImpl_itemImpl(JNIEnv* env, jclass, jlong peer
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->item(index));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_MediaListImpl_deleteMediumImpl(JNIEnv* env, jclass, jlong peer
    , jstring oldMedium)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->deleteMedium(AtomString {String(env, oldMedium)}));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_MediaListImpl_appendMediumImpl(JNIEnv* env, jclass, jlong peer
    , jstring newMedium)
{
    WebCore::JSMainThreadNullState state;
    IMPL->appendMedium(AtomString{String(env, newMedium)});
}


}
