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

import org.w3c.dom.html.HTMLFieldSetElement;
import org.w3c.dom.html.HTMLFormElement;

public class HTMLFieldSetElementImpl extends HTMLElementImpl implements HTMLFieldSetElement {
    HTMLFieldSetElementImpl(long peer) {
        super(peer);
    }

    static HTMLFieldSetElement getImpl(long peer) {
        return (HTMLFieldSetElement)create(peer);
    }


// Attributes
    public boolean getDisabled() {
        return getDisabledImpl(getPeer());
    }
    static boolean getDisabledImpl(long peer) {
        return HTMLFieldSetElementNative.getDisabled(peer);
    }

    public void setDisabled(boolean value) {
        setDisabledImpl(getPeer(), value);
    }
    static void setDisabledImpl(long peer, boolean value) {
        HTMLFieldSetElementNative.setDisabled(peer, value);
    }

    @Override
    public HTMLFormElement getForm() {
        return HTMLFormElementImpl.getImpl(getFormImpl(getPeer()));
    }
    static long getFormImpl(long peer) {
        return HTMLFieldSetElementNative.getForm(peer);
    }

    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLFieldSetElementNative.getName(peer);
    }

    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLFieldSetElementNative.setName(peer, value);
    }

    public String getType() {
        return getTypeImpl(getPeer());
    }
    static String getTypeImpl(long peer) {
        return HTMLFieldSetElementNative.getType(peer);
    }

    public boolean getWillValidate() {
        return getWillValidateImpl(getPeer());
    }
    static boolean getWillValidateImpl(long peer) {
        return HTMLFieldSetElementNative.getWillValidate(peer);
    }

    public String getValidationMessage() {
        return getValidationMessageImpl(getPeer());
    }
    static String getValidationMessageImpl(long peer) {
        return HTMLFieldSetElementNative.getValidationMessage(peer);
    }


// Functions
    public boolean checkValidity()
    {
        return checkValidityImpl(getPeer());
    }
    static boolean checkValidityImpl(long peer) {
        return HTMLFieldSetElementNative.checkValidity(peer);
    }


    public void setCustomValidity(String error)
    {
        setCustomValidityImpl(getPeer()
            , error);
    }
    static void setCustomValidityImpl(long peer
        , String error) {
        HTMLFieldSetElementNative.setCustomValidity(peer, error);
    }


}

