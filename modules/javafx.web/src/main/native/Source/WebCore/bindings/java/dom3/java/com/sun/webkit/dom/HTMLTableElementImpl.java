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
import org.w3c.dom.html.HTMLTableCaptionElement;
import org.w3c.dom.html.HTMLTableElement;
import org.w3c.dom.html.HTMLTableSectionElement;

public class HTMLTableElementImpl extends HTMLElementImpl implements HTMLTableElement {
    HTMLTableElementImpl(long peer) {
        super(peer);
    }

    static HTMLTableElement getImpl(long peer) {
        return (HTMLTableElement)create(peer);
    }


// Attributes
    @Override
    public HTMLTableCaptionElement getCaption() {
        return HTMLTableCaptionElementImpl.getImpl(getCaptionImpl(getPeer()));
    }
    static long getCaptionImpl(long peer) {
        return HTMLTableElementNative.getCaption(peer);
    }

    @Override
    public void setCaption(HTMLTableCaptionElement value) throws DOMException {
        setCaptionImpl(getPeer(), HTMLTableCaptionElementImpl.getPeer(value));
    }
    static void setCaptionImpl(long peer, long value) {
        HTMLTableElementNative.setCaption(peer, value);
    }

    @Override
    public HTMLTableSectionElement getTHead() {
        return HTMLTableSectionElementImpl.getImpl(getTHeadImpl(getPeer()));
    }
    static long getTHeadImpl(long peer) {
        return HTMLTableElementNative.getTHead(peer);
    }

    @Override
    public void setTHead(HTMLTableSectionElement value) throws DOMException {
        setTHeadImpl(getPeer(), HTMLTableSectionElementImpl.getPeer(value));
    }
    static void setTHeadImpl(long peer, long value) {
        HTMLTableElementNative.setTHead(peer, value);
    }

    @Override
    public HTMLTableSectionElement getTFoot() {
        return HTMLTableSectionElementImpl.getImpl(getTFootImpl(getPeer()));
    }
    static long getTFootImpl(long peer) {
        return HTMLTableElementNative.getTFoot(peer);
    }

    @Override
    public void setTFoot(HTMLTableSectionElement value) throws DOMException {
        setTFootImpl(getPeer(), HTMLTableSectionElementImpl.getPeer(value));
    }
    static void setTFootImpl(long peer, long value) {
        HTMLTableElementNative.setTFoot(peer, value);
    }

    @Override
    public HTMLCollection getRows() {
        return HTMLCollectionImpl.getImpl(getRowsImpl(getPeer()));
    }
    static long getRowsImpl(long peer) {
        return HTMLTableElementNative.getRows(peer);
    }

    @Override
    public HTMLCollection getTBodies() {
        return HTMLCollectionImpl.getImpl(getTBodiesImpl(getPeer()));
    }
    static long getTBodiesImpl(long peer) {
        return HTMLTableElementNative.getTBodies(peer);
    }

    @Override
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    static String getAlignImpl(long peer) {
        return HTMLTableElementNative.getAlign(peer);
    }

    @Override
    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    static void setAlignImpl(long peer, String value) {
        HTMLTableElementNative.setAlign(peer, value);
    }

    @Override
    public String getBgColor() {
        return getBgColorImpl(getPeer());
    }
    static String getBgColorImpl(long peer) {
        return HTMLTableElementNative.getBgColor(peer);
    }

    @Override
    public void setBgColor(String value) {
        setBgColorImpl(getPeer(), value);
    }
    static void setBgColorImpl(long peer, String value) {
        HTMLTableElementNative.setBgColor(peer, value);
    }

    @Override
    public String getBorder() {
        return getBorderImpl(getPeer());
    }
    static String getBorderImpl(long peer) {
        return HTMLTableElementNative.getBorder(peer);
    }

    @Override
    public void setBorder(String value) {
        setBorderImpl(getPeer(), value);
    }
    static void setBorderImpl(long peer, String value) {
        HTMLTableElementNative.setBorder(peer, value);
    }

    @Override
    public String getCellPadding() {
        return getCellPaddingImpl(getPeer());
    }
    static String getCellPaddingImpl(long peer) {
        return HTMLTableElementNative.getCellPadding(peer);
    }

    @Override
    public void setCellPadding(String value) {
        setCellPaddingImpl(getPeer(), value);
    }
    static void setCellPaddingImpl(long peer, String value) {
        HTMLTableElementNative.setCellPadding(peer, value);
    }

    @Override
    public String getCellSpacing() {
        return getCellSpacingImpl(getPeer());
    }
    static String getCellSpacingImpl(long peer) {
        return HTMLTableElementNative.getCellSpacing(peer);
    }

    @Override
    public void setCellSpacing(String value) {
        setCellSpacingImpl(getPeer(), value);
    }
    static void setCellSpacingImpl(long peer, String value) {
        HTMLTableElementNative.setCellSpacing(peer, value);
    }

    @Override
    public String getFrame() {
        return getFrameImpl(getPeer());
    }
    static String getFrameImpl(long peer) {
        return HTMLTableElementNative.getFrame(peer);
    }

    @Override
    public void setFrame(String value) {
        setFrameImpl(getPeer(), value);
    }
    static void setFrameImpl(long peer, String value) {
        HTMLTableElementNative.setFrame(peer, value);
    }

    @Override
    public String getRules() {
        return getRulesImpl(getPeer());
    }
    static String getRulesImpl(long peer) {
        return HTMLTableElementNative.getRules(peer);
    }

    @Override
    public void setRules(String value) {
        setRulesImpl(getPeer(), value);
    }
    static void setRulesImpl(long peer, String value) {
        HTMLTableElementNative.setRules(peer, value);
    }

    @Override
    public String getSummary() {
        return getSummaryImpl(getPeer());
    }
    static String getSummaryImpl(long peer) {
        return HTMLTableElementNative.getSummary(peer);
    }

    @Override
    public void setSummary(String value) {
        setSummaryImpl(getPeer(), value);
    }
    static void setSummaryImpl(long peer, String value) {
        HTMLTableElementNative.setSummary(peer, value);
    }

    @Override
    public String getWidth() {
        return getWidthImpl(getPeer());
    }
    static String getWidthImpl(long peer) {
        return HTMLTableElementNative.getWidth(peer);
    }

    @Override
    public void setWidth(String value) {
        setWidthImpl(getPeer(), value);
    }
    static void setWidthImpl(long peer, String value) {
        HTMLTableElementNative.setWidth(peer, value);
    }


// Functions
    @Override
    public HTMLElement createTHead()
    {
        return HTMLElementImpl.getImpl(createTHeadImpl(getPeer()));
    }
    static long createTHeadImpl(long peer) {
        return HTMLTableElementNative.createTHead(peer);
    }


    @Override
    public void deleteTHead()
    {
        deleteTHeadImpl(getPeer());
    }
    static void deleteTHeadImpl(long peer) {
        HTMLTableElementNative.deleteTHead(peer);
    }


    @Override
    public HTMLElement createTFoot()
    {
        return HTMLElementImpl.getImpl(createTFootImpl(getPeer()));
    }
    static long createTFootImpl(long peer) {
        return HTMLTableElementNative.createTFoot(peer);
    }


    @Override
    public void deleteTFoot()
    {
        deleteTFootImpl(getPeer());
    }
    static void deleteTFootImpl(long peer) {
        HTMLTableElementNative.deleteTFoot(peer);
    }


    public HTMLElement createTBody()
    {
        return HTMLElementImpl.getImpl(createTBodyImpl(getPeer()));
    }
    static long createTBodyImpl(long peer) {
        return HTMLTableElementNative.createTBody(peer);
    }


    @Override
    public HTMLElement createCaption()
    {
        return HTMLElementImpl.getImpl(createCaptionImpl(getPeer()));
    }
    static long createCaptionImpl(long peer) {
        return HTMLTableElementNative.createCaption(peer);
    }


    @Override
    public void deleteCaption()
    {
        deleteCaptionImpl(getPeer());
    }
    static void deleteCaptionImpl(long peer) {
        HTMLTableElementNative.deleteCaption(peer);
    }


    @Override
    public HTMLElement insertRow(int index) throws DOMException
    {
        return HTMLElementImpl.getImpl(insertRowImpl(getPeer()
            , index));
    }
    static long insertRowImpl(long peer
        , int index) {
        return HTMLTableElementNative.insertRow(peer, index);
    }


    @Override
    public void deleteRow(int index) throws DOMException
    {
        deleteRowImpl(getPeer()
            , index);
    }
    static void deleteRowImpl(long peer
        , int index) {
        HTMLTableElementNative.deleteRow(peer, index);
    }


}

