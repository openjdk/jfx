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

import org.w3c.dom.html.HTMLAppletElement;

public class HTMLAppletElementImpl extends HTMLElementImpl implements HTMLAppletElement {
    HTMLAppletElementImpl(long peer) {
        super(peer);
    }

    static HTMLAppletElement getImpl(long peer) {
        return (HTMLAppletElement)create(peer);
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

    public String getAlt() {
        return getAltImpl(getPeer());
    }
    native static String getAltImpl(long peer);

    public void setAlt(String value) {
        setAltImpl(getPeer(), value);
    }
    native static void setAltImpl(long peer, String value);

    public String getArchive() {
        return getArchiveImpl(getPeer());
    }
    native static String getArchiveImpl(long peer);

    public void setArchive(String value) {
        setArchiveImpl(getPeer(), value);
    }
    native static void setArchiveImpl(long peer, String value);

    public String getCode() {
        return getCodeImpl(getPeer());
    }
    native static String getCodeImpl(long peer);

    public void setCode(String value) {
        setCodeImpl(getPeer(), value);
    }
    native static void setCodeImpl(long peer, String value);

    public String getCodeBase() {
        return getCodeBaseImpl(getPeer());
    }
    native static String getCodeBaseImpl(long peer);

    public void setCodeBase(String value) {
        setCodeBaseImpl(getPeer(), value);
    }
    native static void setCodeBaseImpl(long peer, String value);

    public String getHeight() {
        return getHeightImpl(getPeer());
    }
    native static String getHeightImpl(long peer);

    public void setHeight(String value) {
        setHeightImpl(getPeer(), value);
    }
    native static void setHeightImpl(long peer, String value);

    public String getHspace() {
        return getHspaceImpl(getPeer())+"";
    }
    native static int getHspaceImpl(long peer);

    public void setHspace(String value) {
        setHspaceImpl(getPeer(), Integer.parseInt(value));
    }
    native static void setHspaceImpl(long peer, int value);

    public String getName() {
        return getNameImpl(getPeer());
    }
    native static String getNameImpl(long peer);

    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    native static void setNameImpl(long peer, String value);

    public String getObject() {
        return getObjectImpl(getPeer());
    }
    native static String getObjectImpl(long peer);

    public void setObject(String value) {
        setObjectImpl(getPeer(), value);
    }
    native static void setObjectImpl(long peer, String value);

    public String getVspace() {
        return getVspaceImpl(getPeer())+"";
    }
    native static int getVspaceImpl(long peer);

    public void setVspace(String value) {
        setVspaceImpl(getPeer(), Integer.parseInt(value));
    }
    native static void setVspaceImpl(long peer, int value);

    public String getWidth() {
        return getWidthImpl(getPeer());
    }
    native static String getWidthImpl(long peer);

    public void setWidth(String value) {
        setWidthImpl(getPeer(), value);
    }
    native static void setWidthImpl(long peer, String value);

}

