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
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLTableRowElement;

public class HTMLTableRowElementImpl extends HTMLElementImpl implements HTMLTableRowElement {
    HTMLTableRowElementImpl(long peer) {
        super(peer);
    }

    static HTMLTableRowElement getImpl(long peer) {
        return (HTMLTableRowElement)create(peer);
    }


// Attributes
    @Override
    public int getRowIndex() {
        return getRowIndexImpl(getPeer());
    }
    static int getRowIndexImpl(long peer) {
        return HTMLTableRowElementNative.getRowIndex(peer);
    }

    @Override
    public int getSectionRowIndex() {
        return getSectionRowIndexImpl(getPeer());
    }
    static int getSectionRowIndexImpl(long peer) {
        return HTMLTableRowElementNative.getSectionRowIndex(peer);
    }

    @Override
    public HTMLCollection getCells() {
        return HTMLCollectionImpl.getImpl(getCellsImpl(getPeer()));
    }
    static long getCellsImpl(long peer) {
        return HTMLTableRowElementNative.getCells(peer);
    }

    @Override
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    static String getAlignImpl(long peer) {
        return HTMLTableRowElementNative.getAlign(peer);
    }

    @Override
    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    static void setAlignImpl(long peer, String value) {
        HTMLTableRowElementNative.setAlign(peer, value);
    }

    @Override
    public String getBgColor() {
        return getBgColorImpl(getPeer());
    }
    static String getBgColorImpl(long peer) {
        return HTMLTableRowElementNative.getBgColor(peer);
    }

    @Override
    public void setBgColor(String value) {
        setBgColorImpl(getPeer(), value);
    }
    static void setBgColorImpl(long peer, String value) {
        HTMLTableRowElementNative.setBgColor(peer, value);
    }

    @Override
    public String getCh() {
        return getChImpl(getPeer());
    }
    static String getChImpl(long peer) {
        return HTMLTableRowElementNative.getCh(peer);
    }

    @Override
    public void setCh(String value) {
        setChImpl(getPeer(), value);
    }
    static void setChImpl(long peer, String value) {
        HTMLTableRowElementNative.setCh(peer, value);
    }

    @Override
    public String getChOff() {
        return getChOffImpl(getPeer());
    }
    static String getChOffImpl(long peer) {
        return HTMLTableRowElementNative.getChOff(peer);
    }

    @Override
    public void setChOff(String value) {
        setChOffImpl(getPeer(), value);
    }
    static void setChOffImpl(long peer, String value) {
        HTMLTableRowElementNative.setChOff(peer, value);
    }

    @Override
    public String getVAlign() {
        return getVAlignImpl(getPeer());
    }
    static String getVAlignImpl(long peer) {
        return HTMLTableRowElementNative.getVAlign(peer);
    }

    @Override
    public void setVAlign(String value) {
        setVAlignImpl(getPeer(), value);
    }
    static void setVAlignImpl(long peer, String value) {
        HTMLTableRowElementNative.setVAlign(peer, value);
    }


// Functions
    @Override
    public HTMLElement insertCell(int index) throws DOMException
    {
        return HTMLElementImpl.getImpl(insertCellImpl(getPeer()
            , index));
    }
    static long insertCellImpl(long peer
        , int index) {
        return HTMLTableRowElementNative.insertCell(peer, index);
    }


    @Override
    public void deleteCell(int index) throws DOMException
    {
        deleteCellImpl(getPeer()
            , index);
    }
    static void deleteCellImpl(long peer
        , int index) {
        HTMLTableRowElementNative.deleteCell(peer, index);
    }


}

