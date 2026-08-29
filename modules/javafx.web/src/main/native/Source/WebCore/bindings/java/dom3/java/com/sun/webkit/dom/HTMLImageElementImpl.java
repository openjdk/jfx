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

import org.w3c.dom.html.HTMLImageElement;

public class HTMLImageElementImpl extends HTMLElementImpl implements HTMLImageElement {
    HTMLImageElementImpl(long peer) {
        super(peer);
    }

    static HTMLImageElement getImpl(long peer) {
        return (HTMLImageElement)create(peer);
    }


// Attributes
    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLImageElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLImageElementNative.setName(peer, value);
    }

    @Override
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    static String getAlignImpl(long peer) {
        return HTMLImageElementNative.getAlign(peer);
    }

    @Override
    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    static void setAlignImpl(long peer, String value) {
        HTMLImageElementNative.setAlign(peer, value);
    }

    @Override
    public String getAlt() {
        return getAltImpl(getPeer());
    }
    static String getAltImpl(long peer) {
        return HTMLImageElementNative.getAlt(peer);
    }

    @Override
    public void setAlt(String value) {
        setAltImpl(getPeer(), value);
    }
    static void setAltImpl(long peer, String value) {
        HTMLImageElementNative.setAlt(peer, value);
    }

    @Override
    public String getBorder() {
        return getBorderImpl(getPeer());
    }
    static String getBorderImpl(long peer) {
        return HTMLImageElementNative.getBorder(peer);
    }

    @Override
    public void setBorder(String value) {
        setBorderImpl(getPeer(), value);
    }
    static void setBorderImpl(long peer, String value) {
        HTMLImageElementNative.setBorder(peer, value);
    }

    public String getCrossOrigin() {
        return getCrossOriginImpl(getPeer());
    }
    static String getCrossOriginImpl(long peer) {
        return HTMLImageElementNative.getCrossOrigin(peer);
    }

    public void setCrossOrigin(String value) {
        setCrossOriginImpl(getPeer(), value);
    }
    static void setCrossOriginImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLImageElementImpl.setCrossOriginImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    @Override
    public String getHeight() {
        return getHeightImpl(getPeer())+"";
    }
    static int getHeightImpl(long peer) {
        return HTMLImageElementNative.getHeight(peer);
    }

    @Override
    public void setHeight(String value) {
        setHeightImpl(getPeer(), Integer.parseInt(value));
    }
    static void setHeightImpl(long peer, int value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLImageElementImpl.setHeightImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    @Override
    public String getHspace() {
        return getHspaceImpl(getPeer())+"";
    }
    static int getHspaceImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLImageElementImpl.getHspaceImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    @Override
    public void setHspace(String value) {
        setHspaceImpl(getPeer(), Integer.parseInt(value));
    }
    static void setHspaceImpl(long peer, int value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLImageElementImpl.setHspaceImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    @Override
    public boolean getIsMap() {
        return getIsMapImpl(getPeer());
    }
    static boolean getIsMapImpl(long peer) {
        return HTMLImageElementNative.getIsMap(peer);
    }

    @Override
    public void setIsMap(boolean value) {
        setIsMapImpl(getPeer(), value);
    }
    static void setIsMapImpl(long peer, boolean value) {
        HTMLImageElementNative.setIsMap(peer, value);
    }

    @Override
    public String getLongDesc() {
        return getLongDescImpl(getPeer());
    }
    static String getLongDescImpl(long peer) {
        return HTMLImageElementNative.getLongDesc(peer);
    }

    @Override
    public void setLongDesc(String value) {
        setLongDescImpl(getPeer(), value);
    }
    static void setLongDescImpl(long peer, String value) {
        HTMLImageElementNative.setLongDesc(peer, value);
    }

    @Override
    public String getSrc() {
        return getSrcImpl(getPeer());
    }
    static String getSrcImpl(long peer) {
        return HTMLImageElementNative.getSrc(peer);
    }

    @Override
    public void setSrc(String value) {
        setSrcImpl(getPeer(), value);
    }
    static void setSrcImpl(long peer, String value) {
        HTMLImageElementNative.setSrc(peer, value);
    }

    public String getSrcset() {
        return getSrcsetImpl(getPeer());
    }
    static String getSrcsetImpl(long peer) {
        return HTMLImageElementNative.getSrcset(peer);
    }

    public void setSrcset(String value) {
        setSrcsetImpl(getPeer(), value);
    }
    static void setSrcsetImpl(long peer, String value) {
        HTMLImageElementNative.setSrcset(peer, value);
    }

    public String getSizes() {
        return getSizesImpl(getPeer());
    }
    static String getSizesImpl(long peer) {
        return HTMLImageElementNative.getSizes(peer);
    }

    public void setSizes(String value) {
        setSizesImpl(getPeer(), value);
    }
    static void setSizesImpl(long peer, String value) {
        HTMLImageElementNative.setSizes(peer, value);
    }

    public String getCurrentSrc() {
        return getCurrentSrcImpl(getPeer());
    }
    static String getCurrentSrcImpl(long peer) {
        return HTMLImageElementNative.getCurrentSrc(peer);
    }

    @Override
    public String getUseMap() {
        return getUseMapImpl(getPeer());
    }
    static String getUseMapImpl(long peer) {
        return HTMLImageElementNative.getUseMap(peer);
    }

    @Override
    public void setUseMap(String value) {
        setUseMapImpl(getPeer(), value);
    }
    static void setUseMapImpl(long peer, String value) {
        HTMLImageElementNative.setUseMap(peer, value);
    }

    @Override
    public String getVspace() {
        return getVspaceImpl(getPeer())+"";
    }
    static int getVspaceImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLImageElementImpl.getVspaceImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    @Override
    public void setVspace(String value) {
        setVspaceImpl(getPeer(), Integer.parseInt(value));
    }
    static void setVspaceImpl(long peer, int value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLImageElementImpl.setVspaceImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    @Override
    public String getWidth() {
        return getWidthImpl(getPeer())+"";
    }
    static int getWidthImpl(long peer) {
        return HTMLImageElementNative.getWidth(peer);
    }

    @Override
    public void setWidth(String value) {
        setWidthImpl(getPeer(), Integer.parseInt(value));
    }
    static void setWidthImpl(long peer, int value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLImageElementImpl.setWidthImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    public boolean getComplete() {
        return getCompleteImpl(getPeer());
    }
    static boolean getCompleteImpl(long peer) {
        return HTMLImageElementNative.getComplete(peer);
    }

    public String getLowsrc() {
        return getLowsrcImpl(getPeer());
    }
    static String getLowsrcImpl(long peer) {
        return HTMLImageElementNative.getLowsrc(peer);
    }

    public void setLowsrc(String value) {
        setLowsrcImpl(getPeer(), value);
    }
    static void setLowsrcImpl(long peer, String value) {
        HTMLImageElementNative.setLowsrc(peer, value);
    }

    public int getNaturalHeight() {
        return getNaturalHeightImpl(getPeer());
    }
    static int getNaturalHeightImpl(long peer) {
        return HTMLImageElementNative.getNaturalHeight(peer);
    }

    public int getNaturalWidth() {
        return getNaturalWidthImpl(getPeer());
    }
    static int getNaturalWidthImpl(long peer) {
        return HTMLImageElementNative.getNaturalWidth(peer);
    }

    public int getX() {
        return getXImpl(getPeer());
    }
    static int getXImpl(long peer) {
        return HTMLImageElementNative.getX(peer);
    }

    public int getY() {
        return getYImpl(getPeer());
    }
    static int getYImpl(long peer) {
        return HTMLImageElementNative.getY(peer);
    }


//stubs
    @Override
    public void setLowSrc(String lowSrc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public String getLowSrc() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

