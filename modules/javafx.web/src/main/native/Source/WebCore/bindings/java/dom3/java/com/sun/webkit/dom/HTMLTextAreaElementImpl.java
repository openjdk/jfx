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
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLTextAreaElement;

public class HTMLTextAreaElementImpl extends HTMLElementImpl implements HTMLTextAreaElement {
    HTMLTextAreaElementImpl(long peer) {
        super(peer);
    }

    static HTMLTextAreaElement getImpl(long peer) {
        return (HTMLTextAreaElement)create(peer);
    }


// Attributes
    public boolean getAutofocus() {
        return getAutofocusImpl(getPeer());
    }
    static boolean getAutofocusImpl(long peer) {
        return HTMLTextAreaElementNative.getAutofocus(peer);
    }

    public void setAutofocus(boolean value) {
        setAutofocusImpl(getPeer(), value);
    }
    static void setAutofocusImpl(long peer, boolean value) {
        HTMLTextAreaElementNative.setAutofocus(peer, value);
    }

    public String getDirName() {
        return getDirNameImpl(getPeer());
    }
    static String getDirNameImpl(long peer) {
        return HTMLTextAreaElementNative.getDirName(peer);
    }

    public void setDirName(String value) {
        setDirNameImpl(getPeer(), value);
    }
    static void setDirNameImpl(long peer, String value) {
        HTMLTextAreaElementNative.setDirName(peer, value);
    }

    @Override
    public boolean getDisabled() {
        return getDisabledImpl(getPeer());
    }
    static boolean getDisabledImpl(long peer) {
        return HTMLTextAreaElementNative.getDisabled(peer);
    }

    @Override
    public void setDisabled(boolean value) {
        setDisabledImpl(getPeer(), value);
    }
    static void setDisabledImpl(long peer, boolean value) {
        HTMLTextAreaElementNative.setDisabled(peer, value);
    }

    @Override
    public HTMLFormElement getForm() {
        return HTMLFormElementImpl.getImpl(getFormImpl(getPeer()));
    }
    static long getFormImpl(long peer) {
        return HTMLTextAreaElementNative.getForm(peer);
    }

    public int getMaxLength() {
        return getMaxLengthImpl(getPeer());
    }
    static int getMaxLengthImpl(long peer) {
        return HTMLTextAreaElementNative.getMaxLength(peer);
    }

    public void setMaxLength(int value) throws DOMException {
        setMaxLengthImpl(getPeer(), value);
    }
    static void setMaxLengthImpl(long peer, int value) {
        HTMLTextAreaElementNative.setMaxLength(peer, value);
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLTextAreaElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLTextAreaElementNative.setName(peer, value);
    }

    public String getPlaceholder() {
        return getPlaceholderImpl(getPeer());
    }
    static String getPlaceholderImpl(long peer) {
        return HTMLTextAreaElementNative.getPlaceholder(peer);
    }

    public void setPlaceholder(String value) {
        setPlaceholderImpl(getPeer(), value);
    }
    static void setPlaceholderImpl(long peer, String value) {
        HTMLTextAreaElementNative.setPlaceholder(peer, value);
    }

    @Override
    public boolean getReadOnly() {
        return getReadOnlyImpl(getPeer());
    }
    static boolean getReadOnlyImpl(long peer) {
        return HTMLTextAreaElementNative.getReadOnly(peer);
    }

    @Override
    public void setReadOnly(boolean value) {
        setReadOnlyImpl(getPeer(), value);
    }
    static void setReadOnlyImpl(long peer, boolean value) {
        HTMLTextAreaElementNative.setReadOnly(peer, value);
    }

    public boolean getRequired() {
        return getRequiredImpl(getPeer());
    }
    static boolean getRequiredImpl(long peer) {
        return HTMLTextAreaElementNative.getRequired(peer);
    }

    public void setRequired(boolean value) {
        setRequiredImpl(getPeer(), value);
    }
    static void setRequiredImpl(long peer, boolean value) {
        HTMLTextAreaElementNative.setRequired(peer, value);
    }

    @Override
    public int getRows() {
        return getRowsImpl(getPeer());
    }
    static int getRowsImpl(long peer) {
        return HTMLTextAreaElementNative.getRows(peer);
    }

    @Override
    public void setRows(int value) {
        setRowsImpl(getPeer(), value);
    }
    static void setRowsImpl(long peer, int value) {
        HTMLTextAreaElementNative.setRows(peer, value);
    }

    @Override
    public int getCols() {
        return getColsImpl(getPeer());
    }
    static int getColsImpl(long peer) {
        return HTMLTextAreaElementNative.getCols(peer);
    }

    @Override
    public void setCols(int value) {
        setColsImpl(getPeer(), value);
    }
    static void setColsImpl(long peer, int value) {
        HTMLTextAreaElementNative.setCols(peer, value);
    }

    public String getWrap() {
        return getWrapImpl(getPeer());
    }
    static String getWrapImpl(long peer) {
        return HTMLTextAreaElementNative.getWrap(peer);
    }

    public void setWrap(String value) {
        setWrapImpl(getPeer(), value);
    }
    static void setWrapImpl(long peer, String value) {
        HTMLTextAreaElementNative.setWrap(peer, value);
    }

    @Override
    public String getType() {
        return getTypeImpl(getPeer());
    }
    static String getTypeImpl(long peer) {
        return HTMLTextAreaElementNative.getType(peer);
    }

    @Override
    public String getDefaultValue() {
        return getDefaultValueImpl(getPeer());
    }
    static String getDefaultValueImpl(long peer) {
        return HTMLTextAreaElementNative.getDefaultValue(peer);
    }

    @Override
    public void setDefaultValue(String value) {
        setDefaultValueImpl(getPeer(), value);
    }
    static void setDefaultValueImpl(long peer, String value) {
        HTMLTextAreaElementNative.setDefaultValue(peer, value);
    }

    @Override
    public String getValue() {
        return getValueImpl(getPeer());
    }
    static String getValueImpl(long peer) {
        return HTMLTextAreaElementNative.getValue(peer);
    }

    @Override
    public void setValue(String value) {
        setValueImpl(getPeer(), value);
    }
    static void setValueImpl(long peer, String value) {
        HTMLTextAreaElementNative.setValue(peer, value);
    }

    public int getTextLength() {
        return getTextLengthImpl(getPeer());
    }
    static int getTextLengthImpl(long peer) {
        return HTMLTextAreaElementNative.getTextLength(peer);
    }

    public boolean getWillValidate() {
        return getWillValidateImpl(getPeer());
    }
    static boolean getWillValidateImpl(long peer) {
        return HTMLTextAreaElementNative.getWillValidate(peer);
    }

    public String getValidationMessage() {
        return getValidationMessageImpl(getPeer());
    }
    static String getValidationMessageImpl(long peer) {
        return HTMLTextAreaElementNative.getValidationMessage(peer);
    }

    public NodeList getLabels() {
        return NodeListImpl.getImpl(getLabelsImpl(getPeer()));
    }
    static long getLabelsImpl(long peer) {
        return HTMLTextAreaElementNative.getLabels(peer);
    }

    public int getSelectionStart() {
        return getSelectionStartImpl(getPeer());
    }
    static int getSelectionStartImpl(long peer) {
        return HTMLTextAreaElementNative.getSelectionStart(peer);
    }

    public void setSelectionStart(int value) {
        setSelectionStartImpl(getPeer(), value);
    }
    static void setSelectionStartImpl(long peer, int value) {
        HTMLTextAreaElementNative.setSelectionStart(peer, value);
    }

    public int getSelectionEnd() {
        return getSelectionEndImpl(getPeer());
    }
    static int getSelectionEndImpl(long peer) {
        return HTMLTextAreaElementNative.getSelectionEnd(peer);
    }

    public void setSelectionEnd(int value) {
        setSelectionEndImpl(getPeer(), value);
    }
    static void setSelectionEndImpl(long peer, int value) {
        HTMLTextAreaElementNative.setSelectionEnd(peer, value);
    }

    public String getSelectionDirection() {
        return getSelectionDirectionImpl(getPeer());
    }
    static String getSelectionDirectionImpl(long peer) {
        return HTMLTextAreaElementNative.getSelectionDirection(peer);
    }

    public void setSelectionDirection(String value) {
        setSelectionDirectionImpl(getPeer(), value);
    }
    static void setSelectionDirectionImpl(long peer, String value) {
        HTMLTextAreaElementNative.setSelectionDirection(peer, value);
    }

    @Override
    public String getAccessKey() {
        return getAccessKeyImpl(getPeer());
    }
    static String getAccessKeyImpl(long peer) {
        return HTMLTextAreaElementNative.getAccessKey(peer);
    }

    @Override
    public void setAccessKey(String value) {
        setAccessKeyImpl(getPeer(), value);
    }
    static void setAccessKeyImpl(long peer, String value) {
        HTMLTextAreaElementNative.setAccessKey(peer, value);
    }

    public String getAutocomplete() {
        return getAutocompleteImpl(getPeer());
    }
    static String getAutocompleteImpl(long peer) {
        return HTMLTextAreaElementNative.getAutocomplete(peer);
    }

    public void setAutocomplete(String value) {
        setAutocompleteImpl(getPeer(), value);
    }
    static void setAutocompleteImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLTextAreaElementImpl.setAutocompleteImpl: no wkj_*"
                + " function exists for it in any jfxwebkit build");
    }


// Functions
    public boolean checkValidity()
    {
        return checkValidityImpl(getPeer());
    }
    static boolean checkValidityImpl(long peer) {
        return HTMLTextAreaElementNative.checkValidity(peer);
    }


    public void setCustomValidity(String error)
    {
        setCustomValidityImpl(getPeer()
            , error);
    }
    static void setCustomValidityImpl(long peer
        , String error) {
        HTMLTextAreaElementNative.setCustomValidity(peer, error);
    }


    @Override
    public void select()
    {
        selectImpl(getPeer());
    }
    static void selectImpl(long peer) {
        HTMLTextAreaElementNative.select(peer);
    }


    public void setRangeText(String replacement) throws DOMException
    {
        setRangeTextImpl(getPeer()
            , replacement);
    }
    static void setRangeTextImpl(long peer
        , String replacement) {
        HTMLTextAreaElementNative.setRangeText(peer, replacement);
    }


    public void setRangeTextEx(String replacement
        , int start
        , int end
        , String selectionMode) throws DOMException
    {
        setRangeTextExImpl(getPeer()
            , replacement
            , start
            , end
            , selectionMode);
    }
    static void setRangeTextExImpl(long peer
        , String replacement
        , int start
        , int end
        , String selectionMode) {
        HTMLTextAreaElementNative.setRangeTextEx(peer, replacement, start, end, selectionMode);
    }


    public void setSelectionRange(int start
        , int end
        , String direction)
    {
        setSelectionRangeImpl(getPeer()
            , start
            , end
            , direction);
    }
    static void setSelectionRangeImpl(long peer
        , int start
        , int end
        , String direction) {
        HTMLTextAreaElementNative.setSelectionRange(peer, start, end, direction);
    }


}

