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

import org.w3c.dom.DOMException;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLTableSectionElement;

public class HTMLTableSectionElementImpl extends HTMLElementImpl implements HTMLTableSectionElement {
    HTMLTableSectionElementImpl(long peer) {
        super(peer);
    }

    static HTMLTableSectionElement getImpl(long peer) {
        return (HTMLTableSectionElement)create(peer);
    }


// Attributes
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    native static String getAlignImpl(long peer);

    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    native static void setAlignImpl(long peer, String value);

    public String getCh() {
        return getChImpl(getPeer());
    }
    native static String getChImpl(long peer);

    public void setCh(String value) {
        setChImpl(getPeer(), value);
    }
    native static void setChImpl(long peer, String value);

    public String getChOff() {
        return getChOffImpl(getPeer());
    }
    native static String getChOffImpl(long peer);

    public void setChOff(String value) {
        setChOffImpl(getPeer(), value);
    }
    native static void setChOffImpl(long peer, String value);

    public String getVAlign() {
        return getVAlignImpl(getPeer());
    }
    native static String getVAlignImpl(long peer);

    public void setVAlign(String value) {
        setVAlignImpl(getPeer(), value);
    }
    native static void setVAlignImpl(long peer, String value);

    public HTMLCollection getRows() {
        return HTMLCollectionImpl.getImpl(getRowsImpl(getPeer()));
    }
    native static long getRowsImpl(long peer);


// Functions
    public HTMLElement insertRow(int index) throws DOMException
    {
        return HTMLElementImpl.getImpl(insertRowImpl(getPeer()
            , index));
    }
    native static long insertRowImpl(long peer
        , int index);


    public void deleteRow(int index) throws DOMException
    {
        deleteRowImpl(getPeer()
            , index);
    }
    native static void deleteRowImpl(long peer
        , int index);


}

