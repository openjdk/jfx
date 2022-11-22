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
    public HTMLTableCaptionElement getCaption() {
        return HTMLTableCaptionElementImpl.getImpl(getCaptionImpl(getPeer()));
    }
    native static long getCaptionImpl(long peer);

    public void setCaption(HTMLTableCaptionElement value) throws DOMException {
        setCaptionImpl(getPeer(), HTMLTableCaptionElementImpl.getPeer(value));
    }
    native static void setCaptionImpl(long peer, long value);

    public HTMLTableSectionElement getTHead() {
        return HTMLTableSectionElementImpl.getImpl(getTHeadImpl(getPeer()));
    }
    native static long getTHeadImpl(long peer);

    public void setTHead(HTMLTableSectionElement value) throws DOMException {
        setTHeadImpl(getPeer(), HTMLTableSectionElementImpl.getPeer(value));
    }
    native static void setTHeadImpl(long peer, long value);

    public HTMLTableSectionElement getTFoot() {
        return HTMLTableSectionElementImpl.getImpl(getTFootImpl(getPeer()));
    }
    native static long getTFootImpl(long peer);

    public void setTFoot(HTMLTableSectionElement value) throws DOMException {
        setTFootImpl(getPeer(), HTMLTableSectionElementImpl.getPeer(value));
    }
    native static void setTFootImpl(long peer, long value);

    public HTMLCollection getRows() {
        return HTMLCollectionImpl.getImpl(getRowsImpl(getPeer()));
    }
    native static long getRowsImpl(long peer);

    public HTMLCollection getTBodies() {
        return HTMLCollectionImpl.getImpl(getTBodiesImpl(getPeer()));
    }
    native static long getTBodiesImpl(long peer);

    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    native static String getAlignImpl(long peer);

    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    native static void setAlignImpl(long peer, String value);

    public String getBgColor() {
        return getBgColorImpl(getPeer());
    }
    native static String getBgColorImpl(long peer);

    public void setBgColor(String value) {
        setBgColorImpl(getPeer(), value);
    }
    native static void setBgColorImpl(long peer, String value);

    public String getBorder() {
        return getBorderImpl(getPeer());
    }
    native static String getBorderImpl(long peer);

    public void setBorder(String value) {
        setBorderImpl(getPeer(), value);
    }
    native static void setBorderImpl(long peer, String value);

    public String getCellPadding() {
        return getCellPaddingImpl(getPeer());
    }
    native static String getCellPaddingImpl(long peer);

    public void setCellPadding(String value) {
        setCellPaddingImpl(getPeer(), value);
    }
    native static void setCellPaddingImpl(long peer, String value);

    public String getCellSpacing() {
        return getCellSpacingImpl(getPeer());
    }
    native static String getCellSpacingImpl(long peer);

    public void setCellSpacing(String value) {
        setCellSpacingImpl(getPeer(), value);
    }
    native static void setCellSpacingImpl(long peer, String value);

    public String getFrame() {
        return getFrameImpl(getPeer());
    }
    native static String getFrameImpl(long peer);

    public void setFrame(String value) {
        setFrameImpl(getPeer(), value);
    }
    native static void setFrameImpl(long peer, String value);

    public String getRules() {
        return getRulesImpl(getPeer());
    }
    native static String getRulesImpl(long peer);

    public void setRules(String value) {
        setRulesImpl(getPeer(), value);
    }
    native static void setRulesImpl(long peer, String value);

    public String getSummary() {
        return getSummaryImpl(getPeer());
    }
    native static String getSummaryImpl(long peer);

    public void setSummary(String value) {
        setSummaryImpl(getPeer(), value);
    }
    native static void setSummaryImpl(long peer, String value);

    public String getWidth() {
        return getWidthImpl(getPeer());
    }
    native static String getWidthImpl(long peer);

    public void setWidth(String value) {
        setWidthImpl(getPeer(), value);
    }
    native static void setWidthImpl(long peer, String value);


// Functions
    public HTMLElement createTHead()
    {
        return HTMLElementImpl.getImpl(createTHeadImpl(getPeer()));
    }
    native static long createTHeadImpl(long peer);


    public void deleteTHead()
    {
        deleteTHeadImpl(getPeer());
    }
    native static void deleteTHeadImpl(long peer);


    public HTMLElement createTFoot()
    {
        return HTMLElementImpl.getImpl(createTFootImpl(getPeer()));
    }
    native static long createTFootImpl(long peer);


    public void deleteTFoot()
    {
        deleteTFootImpl(getPeer());
    }
    native static void deleteTFootImpl(long peer);


    public HTMLElement createTBody()
    {
        return HTMLElementImpl.getImpl(createTBodyImpl(getPeer()));
    }
    native static long createTBodyImpl(long peer);


    public HTMLElement createCaption()
    {
        return HTMLElementImpl.getImpl(createCaptionImpl(getPeer()));
    }
    native static long createCaptionImpl(long peer);


    public void deleteCaption()
    {
        deleteCaptionImpl(getPeer());
    }
    native static void deleteCaptionImpl(long peer);


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

