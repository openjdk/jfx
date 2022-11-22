/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.dom;

import com.sun.webkit.Disposer;
import com.sun.webkit.DisposerRecord;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.html.HTMLDocument;

public class DOMImplementationImpl implements DOMImplementation {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            DOMImplementationImpl.dispose(peer);
        }
    }

    DOMImplementationImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static DOMImplementation create(long peer) {
        if (peer == 0L) return null;
        return new DOMImplementationImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof DOMImplementationImpl) && (peer == ((DOMImplementationImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(DOMImplementation arg) {
        return (arg == null) ? 0L : ((DOMImplementationImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static DOMImplementation getImpl(long peer) {
        return (DOMImplementation)create(peer);
    }


// Functions
    public boolean hasFeature(String feature
        , String version)
    {
        return hasFeatureImpl(getPeer()
            , feature
            , version);
    }
    native static boolean hasFeatureImpl(long peer
        , String feature
        , String version);


    public DocumentType createDocumentType(String qualifiedName
        , String publicId
        , String systemId) throws DOMException
    {
        return DocumentTypeImpl.getImpl(createDocumentTypeImpl(getPeer()
            , qualifiedName
            , publicId
            , systemId));
    }
    native static long createDocumentTypeImpl(long peer
        , String qualifiedName
        , String publicId
        , String systemId);


    public Document createDocument(String namespaceURI
        , String qualifiedName
        , DocumentType doctype) throws DOMException
    {
        return DocumentImpl.getImpl(createDocumentImpl(getPeer()
            , namespaceURI
            , qualifiedName
            , DocumentTypeImpl.getPeer(doctype)));
    }
    native static long createDocumentImpl(long peer
        , String namespaceURI
        , String qualifiedName
        , long doctype);


    public CSSStyleSheet createCSSStyleSheet(String title
        , String media) throws DOMException
    {
        return CSSStyleSheetImpl.getImpl(createCSSStyleSheetImpl(getPeer()
            , title
            , media));
    }
    native static long createCSSStyleSheetImpl(long peer
        , String title
        , String media);


    public HTMLDocument createHTMLDocument(String title)
    {
        return HTMLDocumentImpl.getImpl(createHTMLDocumentImpl(getPeer()
            , title));
    }
    native static long createHTMLDocumentImpl(long peer
        , String title);



//stubs
    public Object getFeature(String feature, String version) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

