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

import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLButtonElement;
import org.w3c.dom.html.HTMLFormElement;

public class HTMLButtonElementImpl extends HTMLElementImpl implements HTMLButtonElement {
    HTMLButtonElementImpl(long peer) {
        super(peer);
    }

    static HTMLButtonElement getImpl(long peer) {
        return (HTMLButtonElement)create(peer);
    }


// Attributes
    public boolean getAutofocus() {
        return getAutofocusImpl(getPeer());
    }
    static boolean getAutofocusImpl(long peer) {
        return HTMLButtonElementNative.getAutofocus(peer);
    }

    public void setAutofocus(boolean value) {
        setAutofocusImpl(getPeer(), value);
    }
    static void setAutofocusImpl(long peer, boolean value) {
        HTMLButtonElementNative.setAutofocus(peer, value);
    }

    @Override
    public boolean getDisabled() {
        return getDisabledImpl(getPeer());
    }
    static boolean getDisabledImpl(long peer) {
        return HTMLButtonElementNative.getDisabled(peer);
    }

    @Override
    public void setDisabled(boolean value) {
        setDisabledImpl(getPeer(), value);
    }
    static void setDisabledImpl(long peer, boolean value) {
        HTMLButtonElementNative.setDisabled(peer, value);
    }

    @Override
    public HTMLFormElement getForm() {
        return HTMLFormElementImpl.getImpl(getFormImpl(getPeer()));
    }
    static long getFormImpl(long peer) {
        return HTMLButtonElementNative.getForm(peer);
    }

    public String getFormAction() {
        return getFormActionImpl(getPeer());
    }
    static String getFormActionImpl(long peer) {
        return HTMLButtonElementNative.getFormAction(peer);
    }

    public void setFormAction(String value) {
        setFormActionImpl(getPeer(), value);
    }
    static void setFormActionImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLButtonElementImpl.setFormActionImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    public String getFormEnctype() {
        return getFormEnctypeImpl(getPeer());
    }
    static String getFormEnctypeImpl(long peer) {
        return HTMLButtonElementNative.getFormEnctype(peer);
    }

    public void setFormEnctype(String value) {
        setFormEnctypeImpl(getPeer(), value);
    }
    static void setFormEnctypeImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLButtonElementImpl.setFormEnctypeImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    public String getFormMethod() {
        return getFormMethodImpl(getPeer());
    }
    static String getFormMethodImpl(long peer) {
        return HTMLButtonElementNative.getFormMethod(peer);
    }

    public void setFormMethod(String value) {
        setFormMethodImpl(getPeer(), value);
    }
    static void setFormMethodImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLButtonElementImpl.setFormMethodImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    @Override
    public String getType() {
        return getTypeImpl(getPeer());
    }
    static String getTypeImpl(long peer) {
        return HTMLButtonElementNative.getType(peer);
    }

    public void setType(String value) {
        setTypeImpl(getPeer(), value);
    }
    static void setTypeImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLButtonElementImpl.setTypeImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    public boolean getFormNoValidate() {
        return getFormNoValidateImpl(getPeer());
    }
    static boolean getFormNoValidateImpl(long peer) {
        return HTMLButtonElementNative.getFormNoValidate(peer);
    }

    public void setFormNoValidate(boolean value) {
        setFormNoValidateImpl(getPeer(), value);
    }
    static void setFormNoValidateImpl(long peer, boolean value) {
        HTMLButtonElementNative.setFormNoValidate(peer, value);
    }

    public String getFormTarget() {
        return getFormTargetImpl(getPeer());
    }
    static String getFormTargetImpl(long peer) {
        return HTMLButtonElementNative.getFormTarget(peer);
    }

    public void setFormTarget(String value) {
        setFormTargetImpl(getPeer(), value);
    }
    static void setFormTargetImpl(long peer, String value) {
        HTMLButtonElementNative.setFormTarget(peer, value);
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLButtonElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLButtonElementNative.setName(peer, value);
    }

    @Override
    public String getValue() {
        return getValueImpl(getPeer());
    }
    static String getValueImpl(long peer) {
        return HTMLButtonElementNative.getValue(peer);
    }

    @Override
    public void setValue(String value) {
        setValueImpl(getPeer(), value);
    }
    static void setValueImpl(long peer, String value) {
        HTMLButtonElementNative.setValue(peer, value);
    }

    public boolean getWillValidate() {
        return getWillValidateImpl(getPeer());
    }
    static boolean getWillValidateImpl(long peer) {
        return HTMLButtonElementNative.getWillValidate(peer);
    }

    public String getValidationMessage() {
        return getValidationMessageImpl(getPeer());
    }
    static String getValidationMessageImpl(long peer) {
        return HTMLButtonElementNative.getValidationMessage(peer);
    }

    public NodeList getLabels() {
        return NodeListImpl.getImpl(getLabelsImpl(getPeer()));
    }
    static long getLabelsImpl(long peer) {
        return HTMLButtonElementNative.getLabels(peer);
    }

    @Override
    public String getAccessKey() {
        return getAccessKeyImpl(getPeer());
    }
    static String getAccessKeyImpl(long peer) {
        return HTMLButtonElementNative.getAccessKey(peer);
    }

    @Override
    public void setAccessKey(String value) {
        setAccessKeyImpl(getPeer(), value);
    }
    static void setAccessKeyImpl(long peer, String value) {
        HTMLButtonElementNative.setAccessKey(peer, value);
    }


// Functions
    public boolean checkValidity()
    {
        return checkValidityImpl(getPeer());
    }
    static boolean checkValidityImpl(long peer) {
        return HTMLButtonElementNative.checkValidity(peer);
    }


    public void setCustomValidity(String error)
    {
        setCustomValidityImpl(getPeer()
            , error);
    }
    static void setCustomValidityImpl(long peer
        , String error) {
        HTMLButtonElementNative.setCustomValidity(peer, error);
    }


    @Override
    public void click()
    {
        clickImpl(getPeer());
    }
    static void clickImpl(long peer) {
        HTMLButtonElementNative.click(peer);
    }


}

