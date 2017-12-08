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
    native static boolean getAutofocusImpl(long peer);

    public void setAutofocus(boolean value) {
        setAutofocusImpl(getPeer(), value);
    }
    native static void setAutofocusImpl(long peer, boolean value);

    public boolean getDisabled() {
        return getDisabledImpl(getPeer());
    }
    native static boolean getDisabledImpl(long peer);

    public void setDisabled(boolean value) {
        setDisabledImpl(getPeer(), value);
    }
    native static void setDisabledImpl(long peer, boolean value);

    public HTMLFormElement getForm() {
        return HTMLFormElementImpl.getImpl(getFormImpl(getPeer()));
    }
    native static long getFormImpl(long peer);

    public boolean getMultiple() {
        return getMultipleImpl(getPeer());
    }
    native static boolean getMultipleImpl(long peer);

    public void setMultiple(boolean value) {
        setMultipleImpl(getPeer(), value);
    }
    native static void setMultipleImpl(long peer, boolean value);

    public String getName() {
        return getNameImpl(getPeer());
    }
    native static String getNameImpl(long peer);

    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    native static void setNameImpl(long peer, String value);

    public boolean getRequired() {
        return getRequiredImpl(getPeer());
    }
    native static boolean getRequiredImpl(long peer);

    public void setRequired(boolean value) {
        setRequiredImpl(getPeer(), value);
    }
    native static void setRequiredImpl(long peer, boolean value);

    public int getSize() {
        return getSizeImpl(getPeer());
    }
    native static int getSizeImpl(long peer);

    public void setSize(int value) {
        setSizeImpl(getPeer(), value);
    }
    native static void setSizeImpl(long peer, int value);

    public String getType() {
        return getTypeImpl(getPeer());
    }
    native static String getTypeImpl(long peer);

    public HTMLOptionsCollectionImpl getOptions() {
        return HTMLOptionsCollectionImpl.getImpl(getOptionsImpl(getPeer()));
    }
    native static long getOptionsImpl(long peer);

    public int getLength() {
        return getLengthImpl(getPeer());
    }
    native static int getLengthImpl(long peer);

    public HTMLCollection getSelectedOptions() {
        return HTMLCollectionImpl.getImpl(getSelectedOptionsImpl(getPeer()));
    }
    native static long getSelectedOptionsImpl(long peer);

    public int getSelectedIndex() {
        return getSelectedIndexImpl(getPeer());
    }
    native static int getSelectedIndexImpl(long peer);

    public void setSelectedIndex(int value) {
        setSelectedIndexImpl(getPeer(), value);
    }
    native static void setSelectedIndexImpl(long peer, int value);

    public String getValue() {
        return getValueImpl(getPeer());
    }
    native static String getValueImpl(long peer);

    public void setValue(String value) {
        setValueImpl(getPeer(), value);
    }
    native static void setValueImpl(long peer, String value);

    public boolean getWillValidate() {
        return getWillValidateImpl(getPeer());
    }
    native static boolean getWillValidateImpl(long peer);

    public String getValidationMessage() {
        return getValidationMessageImpl(getPeer());
    }
    native static String getValidationMessageImpl(long peer);

    public NodeList getLabels() {
        return NodeListImpl.getImpl(getLabelsImpl(getPeer()));
    }
    native static long getLabelsImpl(long peer);

    public String getAutocomplete() {
        return getAutocompleteImpl(getPeer());
    }
    native static String getAutocompleteImpl(long peer);

    public void setAutocomplete(String value) {
        setAutocompleteImpl(getPeer(), value);
    }
    native static void setAutocompleteImpl(long peer, String value);


// Functions
    public Node item(int index)
    {
        return NodeImpl.getImpl(itemImpl(getPeer()
            , index));
    }
    native static long itemImpl(long peer
        , int index);


    public Node namedItem(String name)
    {
        return NodeImpl.getImpl(namedItemImpl(getPeer()
            , name));
    }
    native static long namedItemImpl(long peer
        , String name);


    public void add(HTMLElement element
        , HTMLElement before) throws DOMException
    {
        addImpl(getPeer()
            , HTMLElementImpl.getPeer(element)
            , HTMLElementImpl.getPeer(before));
    }
    native static void addImpl(long peer
        , long element
        , long before);


    public void remove(int index)
    {
        removeImpl(getPeer()
            , index);
    }
    native static void removeImpl(long peer
        , int index);


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

