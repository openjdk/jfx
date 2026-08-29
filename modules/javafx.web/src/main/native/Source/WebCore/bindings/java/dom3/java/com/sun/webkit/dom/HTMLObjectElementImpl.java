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

import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLObjectElement;

public class HTMLObjectElementImpl extends HTMLElementImpl implements HTMLObjectElement {
    HTMLObjectElementImpl(long peer) {
        super(peer);
    }

    static HTMLObjectElement getImpl(long peer) {
        return (HTMLObjectElement)create(peer);
    }


// Attributes
    @Override
    public HTMLFormElement getForm() {
        return HTMLFormElementImpl.getImpl(getFormImpl(getPeer()));
    }
    static long getFormImpl(long peer) {
        return HTMLObjectElementNative.getForm(peer);
    }

    @Override
    public String getCode() {
        return getCodeImpl(getPeer());
    }
    static String getCodeImpl(long peer) {
        return HTMLObjectElementNative.getCode(peer);
    }

    @Override
    public void setCode(String value) {
        setCodeImpl(getPeer(), value);
    }
    static void setCodeImpl(long peer, String value) {
        HTMLObjectElementNative.setCode(peer, value);
    }

    @Override
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    static String getAlignImpl(long peer) {
        return HTMLObjectElementNative.getAlign(peer);
    }

    @Override
    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    static void setAlignImpl(long peer, String value) {
        HTMLObjectElementNative.setAlign(peer, value);
    }

    @Override
    public String getArchive() {
        return getArchiveImpl(getPeer());
    }
    static String getArchiveImpl(long peer) {
        return HTMLObjectElementNative.getArchive(peer);
    }

    @Override
    public void setArchive(String value) {
        setArchiveImpl(getPeer(), value);
    }
    static void setArchiveImpl(long peer, String value) {
        HTMLObjectElementNative.setArchive(peer, value);
    }

    @Override
    public String getBorder() {
        return getBorderImpl(getPeer());
    }
    static String getBorderImpl(long peer) {
        return HTMLObjectElementNative.getBorder(peer);
    }

    @Override
    public void setBorder(String value) {
        setBorderImpl(getPeer(), value);
    }
    static void setBorderImpl(long peer, String value) {
        HTMLObjectElementNative.setBorder(peer, value);
    }

    @Override
    public String getCodeBase() {
        return getCodeBaseImpl(getPeer());
    }
    static String getCodeBaseImpl(long peer) {
        return HTMLObjectElementNative.getCodeBase(peer);
    }

    @Override
    public void setCodeBase(String value) {
        setCodeBaseImpl(getPeer(), value);
    }
    static void setCodeBaseImpl(long peer, String value) {
        HTMLObjectElementNative.setCodeBase(peer, value);
    }

    @Override
    public String getCodeType() {
        return getCodeTypeImpl(getPeer());
    }
    static String getCodeTypeImpl(long peer) {
        return HTMLObjectElementNative.getCodeType(peer);
    }

    @Override
    public void setCodeType(String value) {
        setCodeTypeImpl(getPeer(), value);
    }
    static void setCodeTypeImpl(long peer, String value) {
        HTMLObjectElementNative.setCodeType(peer, value);
    }

    @Override
    public String getData() {
        return getDataImpl(getPeer());
    }
    static String getDataImpl(long peer) {
        return HTMLObjectElementNative.getData(peer);
    }

    @Override
    public void setData(String value) {
        setDataImpl(getPeer(), value);
    }
    static void setDataImpl(long peer, String value) {
        HTMLObjectElementNative.setData(peer, value);
    }

    @Override
    public boolean getDeclare() {
        return getDeclareImpl(getPeer());
    }
    static boolean getDeclareImpl(long peer) {
        return HTMLObjectElementNative.getDeclare(peer);
    }

    @Override
    public void setDeclare(boolean value) {
        setDeclareImpl(getPeer(), value);
    }
    static void setDeclareImpl(long peer, boolean value) {
        HTMLObjectElementNative.setDeclare(peer, value);
    }

    @Override
    public String getHeight() {
        return getHeightImpl(getPeer());
    }
    static String getHeightImpl(long peer) {
        return HTMLObjectElementNative.getHeight(peer);
    }

    @Override
    public void setHeight(String value) {
        setHeightImpl(getPeer(), value);
    }
    static void setHeightImpl(long peer, String value) {
        HTMLObjectElementNative.setHeight(peer, value);
    }

    @Override
    public String getHspace() {
        return getHspaceImpl(getPeer())+"";
    }
    static int getHspaceImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLObjectElementImpl.getHspaceImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    @Override
    public void setHspace(String value) {
        setHspaceImpl(getPeer(), Integer.parseInt(value));
    }
    static void setHspaceImpl(long peer, int value) {
        HTMLObjectElementNative.setHspace(peer, value);
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLObjectElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLObjectElementNative.setName(peer, value);
    }

    @Override
    public String getStandby() {
        return getStandbyImpl(getPeer());
    }
    static String getStandbyImpl(long peer) {
        return HTMLObjectElementNative.getStandby(peer);
    }

    @Override
    public void setStandby(String value) {
        setStandbyImpl(getPeer(), value);
    }
    static void setStandbyImpl(long peer, String value) {
        HTMLObjectElementNative.setStandby(peer, value);
    }

    @Override
    public String getType() {
        return getTypeImpl(getPeer());
    }
    static String getTypeImpl(long peer) {
        return HTMLObjectElementNative.getType(peer);
    }

    @Override
    public void setType(String value) {
        setTypeImpl(getPeer(), value);
    }
    static void setTypeImpl(long peer, String value) {
        HTMLObjectElementNative.setType(peer, value);
    }

    @Override
    public String getUseMap() {
        return getUseMapImpl(getPeer());
    }
    static String getUseMapImpl(long peer) {
        return HTMLObjectElementNative.getUseMap(peer);
    }

    @Override
    public void setUseMap(String value) {
        setUseMapImpl(getPeer(), value);
    }
    static void setUseMapImpl(long peer, String value) {
        HTMLObjectElementNative.setUseMap(peer, value);
    }

    @Override
    public String getVspace() {
        return getVspaceImpl(getPeer())+"";
    }
    static int getVspaceImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLObjectElementImpl.getVspaceImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    @Override
    public void setVspace(String value) {
        setVspaceImpl(getPeer(), Integer.parseInt(value));
    }
    static void setVspaceImpl(long peer, int value) {
        HTMLObjectElementNative.setVspace(peer, value);
    }

    @Override
    public String getWidth() {
        return getWidthImpl(getPeer());
    }
    static String getWidthImpl(long peer) {
        return HTMLObjectElementNative.getWidth(peer);
    }

    @Override
    public void setWidth(String value) {
        setWidthImpl(getPeer(), value);
    }
    static void setWidthImpl(long peer, String value) {
        HTMLObjectElementNative.setWidth(peer, value);
    }

    public boolean getWillValidate() {
        return getWillValidateImpl(getPeer());
    }
    static boolean getWillValidateImpl(long peer) {
        return HTMLObjectElementNative.getWillValidate(peer);
    }

    public String getValidationMessage() {
        return getValidationMessageImpl(getPeer());
    }
    static String getValidationMessageImpl(long peer) {
        return HTMLObjectElementNative.getValidationMessage(peer);
    }

    @Override
    public Document getContentDocument() {
        return DocumentImpl.getImpl(getContentDocumentImpl(getPeer()));
    }
    static long getContentDocumentImpl(long peer) {
        return HTMLObjectElementNative.getContentDocument(peer);
    }


// Functions
    public boolean checkValidity()
    {
        return checkValidityImpl(getPeer());
    }
    static boolean checkValidityImpl(long peer) {
        return HTMLObjectElementNative.checkValidity(peer);
    }


    public void setCustomValidity(String error)
    {
        setCustomValidityImpl(getPeer()
            , error);
    }
    static void setCustomValidityImpl(long peer
        , String error) {
        HTMLObjectElementNative.setCustomValidity(peer, error);
    }


}

