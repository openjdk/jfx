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

import org.w3c.dom.html.HTMLImageElement;

public class HTMLImageElementImpl extends HTMLElementImpl implements HTMLImageElement {
    HTMLImageElementImpl(long peer) {
        super(peer);
    }

    static HTMLImageElement getImpl(long peer) {
        return (HTMLImageElement)create(peer);
    }


// Attributes
    public String getName() {
        return getNameImpl(getPeer());
    }
    native static String getNameImpl(long peer);

    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    native static void setNameImpl(long peer, String value);

    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    native static String getAlignImpl(long peer);

    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    native static void setAlignImpl(long peer, String value);

    public String getAlt() {
        return getAltImpl(getPeer());
    }
    native static String getAltImpl(long peer);

    public void setAlt(String value) {
        setAltImpl(getPeer(), value);
    }
    native static void setAltImpl(long peer, String value);

    public String getBorder() {
        return getBorderImpl(getPeer());
    }
    native static String getBorderImpl(long peer);

    public void setBorder(String value) {
        setBorderImpl(getPeer(), value);
    }
    native static void setBorderImpl(long peer, String value);

    public String getCrossOrigin() {
        return getCrossOriginImpl(getPeer());
    }
    native static String getCrossOriginImpl(long peer);

    public void setCrossOrigin(String value) {
        setCrossOriginImpl(getPeer(), value);
    }
    native static void setCrossOriginImpl(long peer, String value);

    public String getHeight() {
        return getHeightImpl(getPeer())+"";
    }
    native static int getHeightImpl(long peer);

    public void setHeight(String value) {
        setHeightImpl(getPeer(), Integer.parseInt(value));
    }
    native static void setHeightImpl(long peer, int value);

    public String getHspace() {
        return getHspaceImpl(getPeer())+"";
    }
    native static int getHspaceImpl(long peer);

    public void setHspace(String value) {
        setHspaceImpl(getPeer(), Integer.parseInt(value));
    }
    native static void setHspaceImpl(long peer, int value);

    public boolean getIsMap() {
        return getIsMapImpl(getPeer());
    }
    native static boolean getIsMapImpl(long peer);

    public void setIsMap(boolean value) {
        setIsMapImpl(getPeer(), value);
    }
    native static void setIsMapImpl(long peer, boolean value);

    public String getLongDesc() {
        return getLongDescImpl(getPeer());
    }
    native static String getLongDescImpl(long peer);

    public void setLongDesc(String value) {
        setLongDescImpl(getPeer(), value);
    }
    native static void setLongDescImpl(long peer, String value);

    public String getSrc() {
        return getSrcImpl(getPeer());
    }
    native static String getSrcImpl(long peer);

    public void setSrc(String value) {
        setSrcImpl(getPeer(), value);
    }
    native static void setSrcImpl(long peer, String value);

    public String getSrcset() {
        return getSrcsetImpl(getPeer());
    }
    native static String getSrcsetImpl(long peer);

    public void setSrcset(String value) {
        setSrcsetImpl(getPeer(), value);
    }
    native static void setSrcsetImpl(long peer, String value);

    public String getSizes() {
        return getSizesImpl(getPeer());
    }
    native static String getSizesImpl(long peer);

    public void setSizes(String value) {
        setSizesImpl(getPeer(), value);
    }
    native static void setSizesImpl(long peer, String value);

    public String getCurrentSrc() {
        return getCurrentSrcImpl(getPeer());
    }
    native static String getCurrentSrcImpl(long peer);

    public String getUseMap() {
        return getUseMapImpl(getPeer());
    }
    native static String getUseMapImpl(long peer);

    public void setUseMap(String value) {
        setUseMapImpl(getPeer(), value);
    }
    native static void setUseMapImpl(long peer, String value);

    public String getVspace() {
        return getVspaceImpl(getPeer())+"";
    }
    native static int getVspaceImpl(long peer);

    public void setVspace(String value) {
        setVspaceImpl(getPeer(), Integer.parseInt(value));
    }
    native static void setVspaceImpl(long peer, int value);

    public String getWidth() {
        return getWidthImpl(getPeer())+"";
    }
    native static int getWidthImpl(long peer);

    public void setWidth(String value) {
        setWidthImpl(getPeer(), Integer.parseInt(value));
    }
    native static void setWidthImpl(long peer, int value);

    public boolean getComplete() {
        return getCompleteImpl(getPeer());
    }
    native static boolean getCompleteImpl(long peer);

    public String getLowsrc() {
        return getLowsrcImpl(getPeer());
    }
    native static String getLowsrcImpl(long peer);

    public void setLowsrc(String value) {
        setLowsrcImpl(getPeer(), value);
    }
    native static void setLowsrcImpl(long peer, String value);

    public int getNaturalHeight() {
        return getNaturalHeightImpl(getPeer());
    }
    native static int getNaturalHeightImpl(long peer);

    public int getNaturalWidth() {
        return getNaturalWidthImpl(getPeer());
    }
    native static int getNaturalWidthImpl(long peer);

    public int getX() {
        return getXImpl(getPeer());
    }
    native static int getXImpl(long peer);

    public int getY() {
        return getYImpl(getPeer());
    }
    native static int getYImpl(long peer);


//stubs
    public void setLowSrc(String lowSrc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public String getLowSrc() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

