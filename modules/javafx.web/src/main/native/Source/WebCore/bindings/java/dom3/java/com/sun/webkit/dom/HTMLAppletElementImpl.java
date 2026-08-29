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

import org.w3c.dom.html.HTMLAppletElement;

public class HTMLAppletElementImpl extends HTMLElementImpl implements HTMLAppletElement {
    HTMLAppletElementImpl(long peer) {
        super(peer);
    }

    static HTMLAppletElement getImpl(long peer) {
        return (HTMLAppletElement)create(peer);
    }


// Attributes
    @Override
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    static String getAlignImpl(long peer) {
        return HTMLAppletElementNative.getAlign(peer);
    }

    @Override
    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    static void setAlignImpl(long peer, String value) {
        HTMLAppletElementNative.setAlign(peer, value);
    }

    @Override
    public String getAlt() {
        return getAltImpl(getPeer());
    }
    static String getAltImpl(long peer) {
        return HTMLAppletElementNative.getAlt(peer);
    }

    @Override
    public void setAlt(String value) {
        setAltImpl(getPeer(), value);
    }
    static void setAltImpl(long peer, String value) {
        HTMLAppletElementNative.setAlt(peer, value);
    }

    @Override
    public String getArchive() {
        return getArchiveImpl(getPeer());
    }
    static String getArchiveImpl(long peer) {
        return HTMLAppletElementNative.getArchive(peer);
    }

    @Override
    public void setArchive(String value) {
        setArchiveImpl(getPeer(), value);
    }
    static void setArchiveImpl(long peer, String value) {
        HTMLAppletElementNative.setArchive(peer, value);
    }

    @Override
    public String getCode() {
        return getCodeImpl(getPeer());
    }
    static String getCodeImpl(long peer) {
        return HTMLAppletElementNative.getCode(peer);
    }

    @Override
    public void setCode(String value) {
        setCodeImpl(getPeer(), value);
    }
    static void setCodeImpl(long peer, String value) {
        HTMLAppletElementNative.setCode(peer, value);
    }

    @Override
    public String getCodeBase() {
        return getCodeBaseImpl(getPeer());
    }
    static String getCodeBaseImpl(long peer) {
        return HTMLAppletElementNative.getCodeBase(peer);
    }

    @Override
    public void setCodeBase(String value) {
        setCodeBaseImpl(getPeer(), value);
    }
    static void setCodeBaseImpl(long peer, String value) {
        HTMLAppletElementNative.setCodeBase(peer, value);
    }

    @Override
    public String getHeight() {
        return getHeightImpl(getPeer());
    }
    static String getHeightImpl(long peer) {
        return HTMLAppletElementNative.getHeight(peer);
    }

    @Override
    public void setHeight(String value) {
        setHeightImpl(getPeer(), value);
    }
    static void setHeightImpl(long peer, String value) {
        HTMLAppletElementNative.setHeight(peer, value);
    }

    @Override
    public String getHspace() {
        return getHspaceImpl(getPeer())+"";
    }
    static int getHspaceImpl(long peer) {
        return HTMLAppletElementNative.getHspace(peer);
    }

    @Override
    public void setHspace(String value) {
        setHspaceImpl(getPeer(), Integer.parseInt(value));
    }
    static void setHspaceImpl(long peer, int value) {
        HTMLAppletElementNative.setHspace(peer, value);
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLAppletElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLAppletElementNative.setName(peer, value);
    }

    @Override
    public String getObject() {
        return getObjectImpl(getPeer());
    }
    static String getObjectImpl(long peer) {
        return HTMLAppletElementNative.getObject(peer);
    }

    @Override
    public void setObject(String value) {
        setObjectImpl(getPeer(), value);
    }
    static void setObjectImpl(long peer, String value) {
        HTMLAppletElementNative.setObject(peer, value);
    }

    @Override
    public String getVspace() {
        return getVspaceImpl(getPeer())+"";
    }
    static int getVspaceImpl(long peer) {
        return HTMLAppletElementNative.getVspace(peer);
    }

    @Override
    public void setVspace(String value) {
        setVspaceImpl(getPeer(), Integer.parseInt(value));
    }
    static void setVspaceImpl(long peer, int value) {
        HTMLAppletElementNative.setVspace(peer, value);
    }

    @Override
    public String getWidth() {
        return getWidthImpl(getPeer());
    }
    static String getWidthImpl(long peer) {
        return HTMLAppletElementNative.getWidth(peer);
    }

    @Override
    public void setWidth(String value) {
        setWidthImpl(getPeer(), value);
    }
    static void setWidthImpl(long peer, String value) {
        HTMLAppletElementNative.setWidth(peer, value);
    }

}

