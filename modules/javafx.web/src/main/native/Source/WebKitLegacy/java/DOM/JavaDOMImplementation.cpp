/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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


#include <WebCore/CSSStyleSheet.h>
#include <WebCore/DOMException.h>
#include <WebCore/DOMImplementation.h>
#include <WebCore/Document.h>
#if PLATFORM(JAVA)
#include "DocumentInlines.h"
#endif
#include <WebCore/DocumentType.h>
#include <WebCore/HTMLDocument.h>
#include <WebCore/SVGTests.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<DOMImplementation*>(wkj_to_ptr(peer)))

WKJ_EXPORT void wkj_dom_DOMImplementation_dispose(int64_t peer) {
    WKJCallScope wkjScope;
    IMPL->deref();
}


WKJ_EXPORT int64_t wkj_dom_DOMImplementation_createDocumentType(int64_t peer, const uint16_t* qualifiedName, int32_t qualifiedName_length, const uint16_t* publicId, int32_t publicId_length, const uint16_t* systemId, int32_t systemId_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<DocumentType>(WTF::getPtr(raiseOnDOMError(IMPL->createDocumentType(AtomString{WKJString(qualifiedName, qualifiedName_length)}
            , AtomString{ WKJString(publicId, publicId_length) }
            , AtomString{ WKJString(systemId, systemId_length)} ))));
}


WKJ_EXPORT int64_t wkj_dom_DOMImplementation_createDocument(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* qualifiedName, int32_t qualifiedName_length, int64_t doctype)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Document>(WTF::getPtr(raiseOnDOMError(IMPL->createDocument(AtomString{WKJString(namespaceURI, namespaceURI_length)}
            , AtomString{WKJString(qualifiedName, qualifiedName_length)}
            , static_cast<DocumentType*>(wkj_to_ptr(doctype))))));
}


WKJ_EXPORT int64_t wkj_dom_DOMImplementation_createCSSStyleSheet(int64_t peer, const uint16_t* title, int32_t title_length, const uint16_t* media, int32_t media_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<CSSStyleSheet>(WTF::getPtr(IMPL->createCSSStyleSheet(AtomString{WKJString(title, title_length)}, AtomString{WKJString(media, media_length)})));
}


WKJ_EXPORT int64_t wkj_dom_DOMImplementation_createHTMLDocument(int64_t peer, const uint16_t* title, int32_t title_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLDocument>(WTF::getPtr(IMPL->createHTMLDocument(WKJString(title, title_length))));
}


}
