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

import org.w3c.dom.html.HTMLTableCellElement;

public class HTMLTableCellElementImpl extends HTMLElementImpl implements HTMLTableCellElement {
    HTMLTableCellElementImpl(long peer) {
        super(peer);
    }

    static HTMLTableCellElement getImpl(long peer) {
        return (HTMLTableCellElement)create(peer);
    }


// Attributes
    @Override
    public int getCellIndex() {
        return getCellIndexImpl(getPeer());
    }
    static int getCellIndexImpl(long peer) {
        return HTMLTableCellElementNative.getCellIndex(peer);
    }

    @Override
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    static String getAlignImpl(long peer) {
        return HTMLTableCellElementNative.getAlign(peer);
    }

    @Override
    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    static void setAlignImpl(long peer, String value) {
        HTMLTableCellElementNative.setAlign(peer, value);
    }

    @Override
    public String getAxis() {
        return getAxisImpl(getPeer());
    }
    static String getAxisImpl(long peer) {
        return HTMLTableCellElementNative.getAxis(peer);
    }

    @Override
    public void setAxis(String value) {
        setAxisImpl(getPeer(), value);
    }
    static void setAxisImpl(long peer, String value) {
        HTMLTableCellElementNative.setAxis(peer, value);
    }

    @Override
    public String getBgColor() {
        return getBgColorImpl(getPeer());
    }
    static String getBgColorImpl(long peer) {
        return HTMLTableCellElementNative.getBgColor(peer);
    }

    @Override
    public void setBgColor(String value) {
        setBgColorImpl(getPeer(), value);
    }
    static void setBgColorImpl(long peer, String value) {
        HTMLTableCellElementNative.setBgColor(peer, value);
    }

    @Override
    public String getCh() {
        return getChImpl(getPeer());
    }
    static String getChImpl(long peer) {
        return HTMLTableCellElementNative.getCh(peer);
    }

    @Override
    public void setCh(String value) {
        setChImpl(getPeer(), value);
    }
    static void setChImpl(long peer, String value) {
        HTMLTableCellElementNative.setCh(peer, value);
    }

    @Override
    public String getChOff() {
        return getChOffImpl(getPeer());
    }
    static String getChOffImpl(long peer) {
        return HTMLTableCellElementNative.getChOff(peer);
    }

    @Override
    public void setChOff(String value) {
        setChOffImpl(getPeer(), value);
    }
    static void setChOffImpl(long peer, String value) {
        HTMLTableCellElementNative.setChOff(peer, value);
    }

    @Override
    public int getColSpan() {
        return getColSpanImpl(getPeer());
    }
    static int getColSpanImpl(long peer) {
        return HTMLTableCellElementNative.getColSpan(peer);
    }

    @Override
    public void setColSpan(int value) {
        setColSpanImpl(getPeer(), value);
    }
    static void setColSpanImpl(long peer, int value) {
        HTMLTableCellElementNative.setColSpan(peer, value);
    }

    @Override
    public int getRowSpan() {
        return getRowSpanImpl(getPeer());
    }
    static int getRowSpanImpl(long peer) {
        return HTMLTableCellElementNative.getRowSpan(peer);
    }

    @Override
    public void setRowSpan(int value) {
        setRowSpanImpl(getPeer(), value);
    }
    static void setRowSpanImpl(long peer, int value) {
        HTMLTableCellElementNative.setRowSpan(peer, value);
    }

    @Override
    public String getHeaders() {
        return getHeadersImpl(getPeer());
    }
    static String getHeadersImpl(long peer) {
        return HTMLTableCellElementNative.getHeaders(peer);
    }

    @Override
    public void setHeaders(String value) {
        setHeadersImpl(getPeer(), value);
    }
    static void setHeadersImpl(long peer, String value) {
        HTMLTableCellElementNative.setHeaders(peer, value);
    }

    @Override
    public String getHeight() {
        return getHeightImpl(getPeer());
    }
    static String getHeightImpl(long peer) {
        return HTMLTableCellElementNative.getHeight(peer);
    }

    @Override
    public void setHeight(String value) {
        setHeightImpl(getPeer(), value);
    }
    static void setHeightImpl(long peer, String value) {
        HTMLTableCellElementNative.setHeight(peer, value);
    }

    @Override
    public boolean getNoWrap() {
        return getNoWrapImpl(getPeer());
    }
    static boolean getNoWrapImpl(long peer) {
        return HTMLTableCellElementNative.getNoWrap(peer);
    }

    @Override
    public void setNoWrap(boolean value) {
        setNoWrapImpl(getPeer(), value);
    }
    static void setNoWrapImpl(long peer, boolean value) {
        HTMLTableCellElementNative.setNoWrap(peer, value);
    }

    @Override
    public String getVAlign() {
        return getVAlignImpl(getPeer());
    }
    static String getVAlignImpl(long peer) {
        return HTMLTableCellElementNative.getVAlign(peer);
    }

    @Override
    public void setVAlign(String value) {
        setVAlignImpl(getPeer(), value);
    }
    static void setVAlignImpl(long peer, String value) {
        HTMLTableCellElementNative.setVAlign(peer, value);
    }

    @Override
    public String getWidth() {
        return getWidthImpl(getPeer());
    }
    static String getWidthImpl(long peer) {
        return HTMLTableCellElementNative.getWidth(peer);
    }

    @Override
    public void setWidth(String value) {
        setWidthImpl(getPeer(), value);
    }
    static void setWidthImpl(long peer, String value) {
        HTMLTableCellElementNative.setWidth(peer, value);
    }

    @Override
    public String getAbbr() {
        return getAbbrImpl(getPeer());
    }
    static String getAbbrImpl(long peer) {
        return HTMLTableCellElementNative.getAbbr(peer);
    }

    @Override
    public void setAbbr(String value) {
        setAbbrImpl(getPeer(), value);
    }
    static void setAbbrImpl(long peer, String value) {
        HTMLTableCellElementNative.setAbbr(peer, value);
    }

    @Override
    public String getScope() {
        return getScopeImpl(getPeer());
    }
    static String getScopeImpl(long peer) {
        return HTMLTableCellElementNative.getScope(peer);
    }

    @Override
    public void setScope(String value) {
        setScopeImpl(getPeer(), value);
    }
    static void setScopeImpl(long peer, String value) {
        HTMLTableCellElementNative.setScope(peer, value);
    }

}

