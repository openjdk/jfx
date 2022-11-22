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
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;

public class HTMLInputElementImpl extends HTMLElementImpl implements HTMLInputElement {
    HTMLInputElementImpl(long peer) {
        super(peer);
    }

    static HTMLInputElement getImpl(long peer) {
        return (HTMLInputElement)create(peer);
    }


// Attributes
    public String getAccept() {
        return getAcceptImpl(getPeer());
    }
    native static String getAcceptImpl(long peer);

    public void setAccept(String value) {
        setAcceptImpl(getPeer(), value);
    }
    native static void setAcceptImpl(long peer, String value);

    public String getAlt() {
        return getAltImpl(getPeer());
    }
    native static String getAltImpl(long peer);

    public void setAlt(String value) {
        setAltImpl(getPeer(), value);
    }
    native static void setAltImpl(long peer, String value);

    public String getAutocomplete() {
        return getAutocompleteImpl(getPeer());
    }
    native static String getAutocompleteImpl(long peer);

    public void setAutocomplete(String value) {
        setAutocompleteImpl(getPeer(), value);
    }
    native static void setAutocompleteImpl(long peer, String value);

    public boolean getAutofocus() {
        return getAutofocusImpl(getPeer());
    }
    native static boolean getAutofocusImpl(long peer);

    public void setAutofocus(boolean value) {
        setAutofocusImpl(getPeer(), value);
    }
    native static void setAutofocusImpl(long peer, boolean value);

    public boolean getDefaultChecked() {
        return getDefaultCheckedImpl(getPeer());
    }
    native static boolean getDefaultCheckedImpl(long peer);

    public void setDefaultChecked(boolean value) {
        setDefaultCheckedImpl(getPeer(), value);
    }
    native static void setDefaultCheckedImpl(long peer, boolean value);

    public boolean getChecked() {
        return getCheckedImpl(getPeer());
    }
    native static boolean getCheckedImpl(long peer);

    public void setChecked(boolean value) {
        setCheckedImpl(getPeer(), value);
    }
    native static void setCheckedImpl(long peer, boolean value);

    public String getDirName() {
        return getDirNameImpl(getPeer());
    }
    native static String getDirNameImpl(long peer);

    public void setDirName(String value) {
        setDirNameImpl(getPeer(), value);
    }
    native static void setDirNameImpl(long peer, String value);

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

    public String getFormAction() {
        return getFormActionImpl(getPeer());
    }
    native static String getFormActionImpl(long peer);

    public void setFormAction(String value) {
        setFormActionImpl(getPeer(), value);
    }
    native static void setFormActionImpl(long peer, String value);

    public String getFormEnctype() {
        return getFormEnctypeImpl(getPeer());
    }
    native static String getFormEnctypeImpl(long peer);

    public void setFormEnctype(String value) {
        setFormEnctypeImpl(getPeer(), value);
    }
    native static void setFormEnctypeImpl(long peer, String value);

    public String getFormMethod() {
        return getFormMethodImpl(getPeer());
    }
    native static String getFormMethodImpl(long peer);

    public void setFormMethod(String value) {
        setFormMethodImpl(getPeer(), value);
    }
    native static void setFormMethodImpl(long peer, String value);

    public boolean getFormNoValidate() {
        return getFormNoValidateImpl(getPeer());
    }
    native static boolean getFormNoValidateImpl(long peer);

    public void setFormNoValidate(boolean value) {
        setFormNoValidateImpl(getPeer(), value);
    }
    native static void setFormNoValidateImpl(long peer, boolean value);

    public String getFormTarget() {
        return getFormTargetImpl(getPeer());
    }
    native static String getFormTargetImpl(long peer);

    public void setFormTarget(String value) {
        setFormTargetImpl(getPeer(), value);
    }
    native static void setFormTargetImpl(long peer, String value);

    public int getHeight() {
        return getHeightImpl(getPeer());
    }
    native static int getHeightImpl(long peer);

    public void setHeight(int value) {
        setHeightImpl(getPeer(), value);
    }
    native static void setHeightImpl(long peer, int value);

    public boolean getIndeterminate() {
        return getIndeterminateImpl(getPeer());
    }
    native static boolean getIndeterminateImpl(long peer);

    public void setIndeterminate(boolean value) {
        setIndeterminateImpl(getPeer(), value);
    }
    native static void setIndeterminateImpl(long peer, boolean value);

    public String getMax() {
        return getMaxImpl(getPeer());
    }
    native static String getMaxImpl(long peer);

    public void setMax(String value) {
        setMaxImpl(getPeer(), value);
    }
    native static void setMaxImpl(long peer, String value);

    public int getMaxLength() {
        return getMaxLengthImpl(getPeer());
    }
    native static int getMaxLengthImpl(long peer);

    public void setMaxLength(int value) throws DOMException {
        setMaxLengthImpl(getPeer(), value);
    }
    native static void setMaxLengthImpl(long peer, int value);

    public String getMin() {
        return getMinImpl(getPeer());
    }
    native static String getMinImpl(long peer);

    public void setMin(String value) {
        setMinImpl(getPeer(), value);
    }
    native static void setMinImpl(long peer, String value);

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

    public String getPattern() {
        return getPatternImpl(getPeer());
    }
    native static String getPatternImpl(long peer);

    public void setPattern(String value) {
        setPatternImpl(getPeer(), value);
    }
    native static void setPatternImpl(long peer, String value);

    public String getPlaceholder() {
        return getPlaceholderImpl(getPeer());
    }
    native static String getPlaceholderImpl(long peer);

    public void setPlaceholder(String value) {
        setPlaceholderImpl(getPeer(), value);
    }
    native static void setPlaceholderImpl(long peer, String value);

    public boolean getReadOnly() {
        return getReadOnlyImpl(getPeer());
    }
    native static boolean getReadOnlyImpl(long peer);

    public void setReadOnly(boolean value) {
        setReadOnlyImpl(getPeer(), value);
    }
    native static void setReadOnlyImpl(long peer, boolean value);

    public boolean getRequired() {
        return getRequiredImpl(getPeer());
    }
    native static boolean getRequiredImpl(long peer);

    public void setRequired(boolean value) {
        setRequiredImpl(getPeer(), value);
    }
    native static void setRequiredImpl(long peer, boolean value);

    public String getSize() {
        return getSizeImpl(getPeer())+"";
    }
    native static String getSizeImpl(long peer);

    public void setSize(String value) {
        setSizeImpl(getPeer(), value);
    }
    native static void setSizeImpl(long peer, String value);

    public String getSrc() {
        return getSrcImpl(getPeer());
    }
    native static String getSrcImpl(long peer);

    public void setSrc(String value) {
        setSrcImpl(getPeer(), value);
    }
    native static void setSrcImpl(long peer, String value);

    public String getStep() {
        return getStepImpl(getPeer());
    }
    native static String getStepImpl(long peer);

    public void setStep(String value) {
        setStepImpl(getPeer(), value);
    }
    native static void setStepImpl(long peer, String value);

    public String getType() {
        return getTypeImpl(getPeer());
    }
    native static String getTypeImpl(long peer);

    public void setType(String value) {
        setTypeImpl(getPeer(), value);
    }
    native static void setTypeImpl(long peer, String value);

    public String getDefaultValue() {
        return getDefaultValueImpl(getPeer());
    }
    native static String getDefaultValueImpl(long peer);

    public void setDefaultValue(String value) {
        setDefaultValueImpl(getPeer(), value);
    }
    native static void setDefaultValueImpl(long peer, String value);

    public String getValue() {
        return getValueImpl(getPeer());
    }
    native static String getValueImpl(long peer);

    public void setValue(String value) {
        setValueImpl(getPeer(), value);
    }
    native static void setValueImpl(long peer, String value);

    public long getValueAsDate() {
        return getValueAsDateImpl(getPeer());
    }
    native static long getValueAsDateImpl(long peer);

    public void setValueAsDate(long value) throws DOMException {
        setValueAsDateImpl(getPeer(), value);
    }
    native static void setValueAsDateImpl(long peer, long value);

    public double getValueAsNumber() {
        return getValueAsNumberImpl(getPeer());
    }
    native static double getValueAsNumberImpl(long peer);

    public void setValueAsNumber(double value) throws DOMException {
        setValueAsNumberImpl(getPeer(), value);
    }
    native static void setValueAsNumberImpl(long peer, double value);

    public int getWidth() {
        return getWidthImpl(getPeer());
    }
    native static int getWidthImpl(long peer);

    public void setWidth(int value) {
        setWidthImpl(getPeer(), value);
    }
    native static void setWidthImpl(long peer, int value);

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

    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    native static String getAlignImpl(long peer);

    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    native static void setAlignImpl(long peer, String value);

    public String getUseMap() {
        return getUseMapImpl(getPeer());
    }
    native static String getUseMapImpl(long peer);

    public void setUseMap(String value) {
        setUseMapImpl(getPeer(), value);
    }
    native static void setUseMapImpl(long peer, String value);

    public boolean getIncremental() {
        return getIncrementalImpl(getPeer());
    }
    native static boolean getIncrementalImpl(long peer);

    public void setIncremental(boolean value) {
        setIncrementalImpl(getPeer(), value);
    }
    native static void setIncrementalImpl(long peer, boolean value);

    public String getAccessKey() {
        return getAccessKeyImpl(getPeer());
    }
    native static String getAccessKeyImpl(long peer);

    public void setAccessKey(String value) {
        setAccessKeyImpl(getPeer(), value);
    }
    native static void setAccessKeyImpl(long peer, String value);


// Functions
    public void stepUp(int n) throws DOMException
    {
        stepUpImpl(getPeer()
            , n);
    }
    native static void stepUpImpl(long peer
        , int n);


    public void stepDown(int n) throws DOMException
    {
        stepDownImpl(getPeer()
            , n);
    }
    native static void stepDownImpl(long peer
        , int n);


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


    public void select()
    {
        selectImpl(getPeer());
    }
    native static void selectImpl(long peer);


    public void setRangeText(String replacement) throws DOMException
    {
        setRangeTextImpl(getPeer()
            , replacement);
    }
    native static void setRangeTextImpl(long peer
        , String replacement);


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
    native static void setRangeTextExImpl(long peer
        , String replacement
        , int start
        , int end
        , String selectionMode);


    public void click()
    {
        clickImpl(getPeer());
    }
    native static void clickImpl(long peer);


    public void setValueForUser(String value)
    {
        setValueForUserImpl(getPeer()
            , value);
    }
    native static void setValueForUserImpl(long peer
        , String value);


}

