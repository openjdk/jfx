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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLSelectElement;

public class HTMLSelectElementImpl extends HTMLElementImpl implements HTMLSelectElement {
    HTMLSelectElementImpl(long peer) {
        super(peer);
    }

    static HTMLSelectElement getImpl(long peer) {
        return (HTMLSelectElement)create(peer);
    }


// Attributes
    public boolean getAutofocus() {
        return getAutofocusImpl(getPeer());
    }
    static boolean getAutofocusImpl(long peer) {
        return HTMLSelectElementNative.getAutofocus(peer);
    }

    public void setAutofocus(boolean value) {
        setAutofocusImpl(getPeer(), value);
    }
    static void setAutofocusImpl(long peer, boolean value) {
        HTMLSelectElementNative.setAutofocus(peer, value);
    }

    @Override
    public boolean getDisabled() {
        return getDisabledImpl(getPeer());
    }
    static boolean getDisabledImpl(long peer) {
        return HTMLSelectElementNative.getDisabled(peer);
    }

    @Override
    public void setDisabled(boolean value) {
        setDisabledImpl(getPeer(), value);
    }
    static void setDisabledImpl(long peer, boolean value) {
        HTMLSelectElementNative.setDisabled(peer, value);
    }

    @Override
    public HTMLFormElement getForm() {
        return HTMLFormElementImpl.getImpl(getFormImpl(getPeer()));
    }
    static long getFormImpl(long peer) {
        return HTMLSelectElementNative.getForm(peer);
    }

    @Override
    public boolean getMultiple() {
        return getMultipleImpl(getPeer());
    }
    static boolean getMultipleImpl(long peer) {
        return HTMLSelectElementNative.getMultiple(peer);
    }

    @Override
    public void setMultiple(boolean value) {
        setMultipleImpl(getPeer(), value);
    }
    static void setMultipleImpl(long peer, boolean value) {
        HTMLSelectElementNative.setMultiple(peer, value);
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLSelectElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLSelectElementNative.setName(peer, value);
    }

    public boolean getRequired() {
        return getRequiredImpl(getPeer());
    }
    static boolean getRequiredImpl(long peer) {
        return HTMLSelectElementNative.getRequired(peer);
    }

    public void setRequired(boolean value) {
        setRequiredImpl(getPeer(), value);
    }
    static void setRequiredImpl(long peer, boolean value) {
        HTMLSelectElementNative.setRequired(peer, value);
    }

    @Override
    public int getSize() {
        return getSizeImpl(getPeer());
    }
    static int getSizeImpl(long peer) {
        return HTMLSelectElementNative.getSize(peer);
    }

    @Override
    public void setSize(int value) {
        setSizeImpl(getPeer(), value);
    }
    static void setSizeImpl(long peer, int value) {
        HTMLSelectElementNative.setSize(peer, value);
    }

    @Override
    public String getType() {
        return getTypeImpl(getPeer());
    }
    static String getTypeImpl(long peer) {
        return HTMLSelectElementNative.getType(peer);
    }

    @Override
    public HTMLOptionsCollectionImpl getOptions() {
        return HTMLOptionsCollectionImpl.getImpl(getOptionsImpl(getPeer()));
    }
    static long getOptionsImpl(long peer) {
        return HTMLSelectElementNative.getOptions(peer);
    }

    @Override
    public int getLength() {
        return getLengthImpl(getPeer());
    }
    static int getLengthImpl(long peer) {
        return HTMLSelectElementNative.getLength(peer);
    }

    public HTMLCollection getSelectedOptions() {
        return HTMLCollectionImpl.getImpl(getSelectedOptionsImpl(getPeer()));
    }
    static long getSelectedOptionsImpl(long peer) {
        return HTMLSelectElementNative.getSelectedOptions(peer);
    }

    @Override
    public int getSelectedIndex() {
        return getSelectedIndexImpl(getPeer());
    }
    static int getSelectedIndexImpl(long peer) {
        return HTMLSelectElementNative.getSelectedIndex(peer);
    }

    @Override
    public void setSelectedIndex(int value) {
        setSelectedIndexImpl(getPeer(), value);
    }
    static void setSelectedIndexImpl(long peer, int value) {
        HTMLSelectElementNative.setSelectedIndex(peer, value);
    }

    @Override
    public String getValue() {
        return getValueImpl(getPeer());
    }
    static String getValueImpl(long peer) {
        return HTMLSelectElementNative.getValue(peer);
    }

    @Override
    public void setValue(String value) {
        setValueImpl(getPeer(), value);
    }
    static void setValueImpl(long peer, String value) {
        HTMLSelectElementNative.setValue(peer, value);
    }

    public boolean getWillValidate() {
        return getWillValidateImpl(getPeer());
    }
    static boolean getWillValidateImpl(long peer) {
        return HTMLSelectElementNative.getWillValidate(peer);
    }

    public String getValidationMessage() {
        return getValidationMessageImpl(getPeer());
    }
    static String getValidationMessageImpl(long peer) {
        return HTMLSelectElementNative.getValidationMessage(peer);
    }

    public NodeList getLabels() {
        return NodeListImpl.getImpl(getLabelsImpl(getPeer()));
    }
    static long getLabelsImpl(long peer) {
        return HTMLSelectElementNative.getLabels(peer);
    }

    public String getAutocomplete() {
        return getAutocompleteImpl(getPeer());
    }
    static String getAutocompleteImpl(long peer) {
        return HTMLSelectElementNative.getAutocomplete(peer);
    }

    public void setAutocomplete(String value) {
        setAutocompleteImpl(getPeer(), value);
    }
    static void setAutocompleteImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLSelectElementImpl.setAutocompleteImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }


// Functions
    public Node item(int index)
    {
        return NodeImpl.getImpl(itemImpl(getPeer()
            , index));
    }
    static long itemImpl(long peer
        , int index) {
        return HTMLSelectElementNative.item(peer, index);
    }


    public Node namedItem(String name)
    {
        return NodeImpl.getImpl(namedItemImpl(getPeer()
            , name));
    }
    static long namedItemImpl(long peer
        , String name) {
        return HTMLSelectElementNative.namedItem(peer, name);
    }


    @Override
    public void add(HTMLElement element
        , HTMLElement before) throws DOMException
    {
        addImpl(getPeer()
            , HTMLElementImpl.getPeer(element)
            , HTMLElementImpl.getPeer(before));
    }
    static void addImpl(long peer
        , long element
        , long before) {
        HTMLSelectElementNative.add(peer, element, before);
    }


    @Override
    public void remove(int index)
    {
        removeImpl(getPeer()
            , index);
    }
    static void removeImpl(long peer
        , int index) {
        HTMLSelectElementNative.remove(peer, index);
    }


    public boolean checkValidity()
    {
        return checkValidityImpl(getPeer());
    }
    static boolean checkValidityImpl(long peer) {
        return HTMLSelectElementNative.checkValidity(peer);
    }


    public void setCustomValidity(String error)
    {
        setCustomValidityImpl(getPeer()
            , error);
    }
    static void setCustomValidityImpl(long peer
        , String error) {
        HTMLSelectElementNative.setCustomValidity(peer, error);
    }


}

