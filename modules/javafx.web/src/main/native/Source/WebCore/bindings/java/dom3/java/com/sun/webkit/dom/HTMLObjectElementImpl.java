/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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
    native static long getFormImpl(long peer);

    @Override
    public String getCode() {
        return getCodeImpl(getPeer());
    }
    native static String getCodeImpl(long peer);

    @Override
    public void setCode(String value) {
        setCodeImpl(getPeer(), value);
    }
    native static void setCodeImpl(long peer, String value);

    @Override
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    native static String getAlignImpl(long peer);

    @Override
    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    native static void setAlignImpl(long peer, String value);

    @Override
    public String getArchive() {
        return getArchiveImpl(getPeer());
    }
    native static String getArchiveImpl(long peer);

    @Override
    public void setArchive(String value) {
        setArchiveImpl(getPeer(), value);
    }
    native static void setArchiveImpl(long peer, String value);

    @Override
    public String getBorder() {
        return getBorderImpl(getPeer());
    }
    native static String getBorderImpl(long peer);

    @Override
    public void setBorder(String value) {
        setBorderImpl(getPeer(), value);
    }
    native static void setBorderImpl(long peer, String value);

    @Override
    public String getCodeBase() {
        return getCodeBaseImpl(getPeer());
    }
    native static String getCodeBaseImpl(long peer);

    @Override
    public void setCodeBase(String value) {
        setCodeBaseImpl(getPeer(), value);
    }
    native static void setCodeBaseImpl(long peer, String value);

    @Override
    public String getCodeType() {
        return getCodeTypeImpl(getPeer());
    }
    native static String getCodeTypeImpl(long peer);

    @Override
    public void setCodeType(String value) {
        setCodeTypeImpl(getPeer(), value);
    }
    native static void setCodeTypeImpl(long peer, String value);

    @Override
    public String getData() {
        return getDataImpl(getPeer());
    }
    native static String getDataImpl(long peer);

    @Override
    public void setData(String value) {
        setDataImpl(getPeer(), value);
    }
    native static void setDataImpl(long peer, String value);

    @Override
    public boolean getDeclare() {
        return getDeclareImpl(getPeer());
    }
    native static boolean getDeclareImpl(long peer);

    @Override
    public void setDeclare(boolean value) {
        setDeclareImpl(getPeer(), value);
    }
    native static void setDeclareImpl(long peer, boolean value);

    @Override
    public String getHeight() {
        return getHeightImpl(getPeer());
    }
    native static String getHeightImpl(long peer);

    @Override
    public void setHeight(String value) {
        setHeightImpl(getPeer(), value);
    }
    native static void setHeightImpl(long peer, String value);

    @Override
    public String getHspace() {
        return getHspaceImpl(getPeer())+"";
    }
    native static int getHspaceImpl(long peer);

    @Override
    public void setHspace(String value) {
        setHspaceImpl(getPeer(), Integer.parseInt(value));
    }
    native static void setHspaceImpl(long peer, int value);

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    native static String getNameImpl(long peer);

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    native static void setNameImpl(long peer, String value);

    @Override
    public String getStandby() {
        return getStandbyImpl(getPeer());
    }
    native static String getStandbyImpl(long peer);

    @Override
    public void setStandby(String value) {
        setStandbyImpl(getPeer(), value);
    }
    native static void setStandbyImpl(long peer, String value);

    @Override
    public String getType() {
        return getTypeImpl(getPeer());
    }
    native static String getTypeImpl(long peer);

    @Override
    public void setType(String value) {
        setTypeImpl(getPeer(), value);
    }
    native static void setTypeImpl(long peer, String value);

    @Override
    public String getUseMap() {
        return getUseMapImpl(getPeer());
    }
    native static String getUseMapImpl(long peer);

    @Override
    public void setUseMap(String value) {
        setUseMapImpl(getPeer(), value);
    }
    native static void setUseMapImpl(long peer, String value);

    @Override
    public String getVspace() {
        return getVspaceImpl(getPeer())+"";
    }
    native static int getVspaceImpl(long peer);

    @Override
    public void setVspace(String value) {
        setVspaceImpl(getPeer(), Integer.parseInt(value));
    }
    native static void setVspaceImpl(long peer, int value);

    @Override
    public String getWidth() {
        return getWidthImpl(getPeer());
    }
    native static String getWidthImpl(long peer);

    @Override
    public void setWidth(String value) {
        setWidthImpl(getPeer(), value);
    }
    native static void setWidthImpl(long peer, String value);

    public boolean getWillValidate() {
        return getWillValidateImpl(getPeer());
    }
    native static boolean getWillValidateImpl(long peer);

    public String getValidationMessage() {
        return getValidationMessageImpl(getPeer());
    }
    native static String getValidationMessageImpl(long peer);

    @Override
    public Document getContentDocument() {
        return DocumentImpl.getImpl(getContentDocumentImpl(getPeer()));
    }
    native static long getContentDocumentImpl(long peer);


// Functions
    public boolean checkValidity()
    {
        return checkValidityImpl(getPeer());
    }
    native static boolean checkValidityImpl(long peer);


    public void setCustomValidity(String error)
    {
        setCustomValidityImpl(getPeer()
            , error);
    }
    native static void setCustomValidityImpl(long peer
        , String error);


}

