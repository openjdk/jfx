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
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLOptionElement;

public class HTMLOptionsCollectionImpl extends HTMLCollectionImpl {
    HTMLOptionsCollectionImpl(long peer) {
        super(peer);
    }

    static HTMLOptionsCollectionImpl getImpl(long peer) {
        return (HTMLOptionsCollectionImpl)create(peer);
    }


// Attributes
    public int getSelectedIndex() {
        return getSelectedIndexImpl(getPeer());
    }
    static int getSelectedIndexImpl(long peer) {
        return HTMLOptionsCollectionNative.getSelectedIndex(peer);
    }

    public void setSelectedIndex(int value) {
        setSelectedIndexImpl(getPeer(), value);
    }
    static void setSelectedIndexImpl(long peer, int value) {
        HTMLOptionsCollectionNative.setSelectedIndex(peer, value);
    }

    @Override
    public int getLength() {
        return getLengthImpl(getPeer());
    }
    static int getLengthImpl(long peer) {
        return HTMLOptionsCollectionNative.getLength(peer);
    }

    public void setLength(int value) throws DOMException {
        setLengthImpl(getPeer(), value);
    }
    static void setLengthImpl(long peer, int value) {
        HTMLOptionsCollectionNative.setLength(peer, value);
    }


// Functions
    @Override
    public Node namedItem(String name)
    {
        return NodeImpl.getImpl(namedItemImpl(getPeer()
            , name));
    }
    static long namedItemImpl(long peer
        , String name) {
        return HTMLOptionsCollectionNative.namedItem(peer, name);
    }


    public void add(HTMLOptionElement option
        , int index) throws DOMException
    {
        addImpl(getPeer()
            , HTMLOptionElementImpl.getPeer(option)
            , index);
    }
    static void addImpl(long peer
        , long option
        , int index) {
        HTMLOptionsCollectionNative.add(peer, option, index);
    }


    @Override
    public Node item(int index)
    {
        return NodeImpl.getImpl(itemImpl(getPeer()
            , index));
    }
    static long itemImpl(long peer
        , int index) {
        return HTMLOptionsCollectionNative.item(peer, index);
    }


}

