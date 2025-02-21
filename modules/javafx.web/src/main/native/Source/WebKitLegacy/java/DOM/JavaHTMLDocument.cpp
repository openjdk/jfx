/*
 * Copyright (c) 2013, 2025, Oracle and/or its affiliates. All rights reserved.
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


#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLDocument.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLDocument*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getEmbedsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->embeds()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getPluginsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->embeds()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getScriptsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->scripts()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getDirImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->dir());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_setDirImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setDir(AtomString {String(env, value)});
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getDesignModeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->designMode());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_setDesignModeImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setDesignMode(AtomString {String(env, value)});
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getCompatModeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->compatMode());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getBgColorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->bgColor());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_setBgColorImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBgColor(AtomString {String(env, value)});
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getFgColorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->fgColor());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_setFgColorImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setFgColor(AtomString {String(env, value)});
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getAlinkColorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->alinkColor());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_setAlinkColorImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAlinkColor(AtomString {String(env, value)});
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getLinkColorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->linkColorForBindings());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_setLinkColorImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setLinkColorForBindings(AtomString {String(env, value)});
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_getVlinkColorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->vlinkColor());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_setVlinkColorImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setVlinkColor(AtomString {String(env, value)});
}


// Functions
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_openImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->open();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_closeImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->close();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_writeImpl(JNIEnv* env, jclass, jlong peer
    , jstring text)
{
    WebCore::JSMainThreadNullState state;
    WTF::FixedVector<String> textVector { String(env, text) };
    IMPL->write(nullptr, WTFMove(textVector));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_writelnImpl(JNIEnv* env, jclass, jlong peer
    , jstring text)
{
    WebCore::JSMainThreadNullState state;
    WTF::FixedVector<String> textVector { String(env, text) };
    IMPL->writeln(nullptr, WTFMove(textVector));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_clearImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->clear();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_captureEventsImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->captureEvents();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLDocumentImpl_releaseEventsImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->releaseEvents();
}


}
