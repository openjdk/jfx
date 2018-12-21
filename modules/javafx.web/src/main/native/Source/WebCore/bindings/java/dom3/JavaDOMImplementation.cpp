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

#include <WebCore/CSSStyleSheet.h>
#include "DOMException.h"
#include <WebCore/DOMImplementation.h>
#include <WebCore/Document.h>
#include <WebCore/DocumentType.h>
#include <WebCore/HTMLDocument.h>
#include <WebCore/SVGTests.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<DOMImplementation*>(jlong_to_ptr(peer)))

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMImplementationImpl_dispose(JNIEnv*, jclass, jlong peer) {
    IMPL->deref();
}


// Functions
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DOMImplementationImpl_hasFeatureImpl(JNIEnv* env, jclass, jlong
    , jstring feature
    , jstring version)
{
    WebCore::JSMainThreadNullState state;
    return WebCore::SVGTests::hasFeatureForLegacyBindings(String(env, feature), String(env, version));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMImplementationImpl_createDocumentTypeImpl(JNIEnv* env, jclass, jlong peer
    , jstring qualifiedName
    , jstring publicId
    , jstring systemId)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DocumentType>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createDocumentType(String(env, qualifiedName)
            , String(env, publicId)
            , String(env, systemId)))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMImplementationImpl_createDocumentImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI
    , jstring qualifiedName
    , jlong doctype)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Document>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createDocument(String(env, namespaceURI)
            , String(env, qualifiedName)
            , static_cast<DocumentType*>(jlong_to_ptr(doctype))))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMImplementationImpl_createCSSStyleSheetImpl(JNIEnv* env, jclass, jlong peer
    , jstring title
    , jstring media)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<CSSStyleSheet>(env, WTF::getPtr(IMPL->createCSSStyleSheet(String(env, title), String(env, media))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMImplementationImpl_createHTMLDocumentImpl(JNIEnv* env, jclass, jlong peer
    , jstring title)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLDocument>(env, WTF::getPtr(IMPL->createHTMLDocument(String(env, title))));
}


}
