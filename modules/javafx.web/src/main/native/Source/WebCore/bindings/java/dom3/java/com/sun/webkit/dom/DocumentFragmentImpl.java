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

package com.sun.webkit.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLCollection;

public class DocumentFragmentImpl extends NodeImpl implements DocumentFragment {
    DocumentFragmentImpl(long peer) {
        super(peer);
    }

    static DocumentFragment getImpl(long peer) {
        return (DocumentFragment)create(peer);
    }


// Attributes
    public HTMLCollection getChildren() {
        return HTMLCollectionImpl.getImpl(getChildrenImpl(getPeer()));
    }
    static long getChildrenImpl(long peer) {
        return DocumentFragmentNative.getChildren(peer);
    }

    public Element getFirstElementChild() {
        return ElementImpl.getImpl(getFirstElementChildImpl(getPeer()));
    }
    static long getFirstElementChildImpl(long peer) {
        return DocumentFragmentNative.getFirstElementChild(peer);
    }

    public Element getLastElementChild() {
        return ElementImpl.getImpl(getLastElementChildImpl(getPeer()));
    }
    static long getLastElementChildImpl(long peer) {
        return DocumentFragmentNative.getLastElementChild(peer);
    }

    public int getChildElementCount() {
        return getChildElementCountImpl(getPeer());
    }
    static int getChildElementCountImpl(long peer) {
        return DocumentFragmentNative.getChildElementCount(peer);
    }


// Functions
    public Element getElementById(String elementId)
    {
        return ElementImpl.getImpl(getElementByIdImpl(getPeer()
            , elementId));
    }
    static long getElementByIdImpl(long peer
        , String elementId) {
        return DocumentFragmentNative.getElementById(peer, elementId);
    }


    public Element querySelector(String selectors) throws DOMException
    {
        return ElementImpl.getImpl(querySelectorImpl(getPeer()
            , selectors));
    }
    static long querySelectorImpl(long peer
        , String selectors) {
        return DocumentFragmentNative.querySelector(peer, selectors);
    }


    public NodeList querySelectorAll(String selectors) throws DOMException
    {
        return NodeListImpl.getImpl(querySelectorAllImpl(getPeer()
            , selectors));
    }
    static long querySelectorAllImpl(long peer
        , String selectors) {
        return DocumentFragmentNative.querySelectorAll(peer, selectors);
    }


}

