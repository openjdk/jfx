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
import org.w3c.dom.html.HTMLInputElement;

public class HTMLInputElementImpl extends HTMLElementImpl implements HTMLInputElement {
    HTMLInputElementImpl(long peer) {
        super(peer);
    }

    static HTMLInputElement getImpl(long peer) {
        return (HTMLInputElement)create(peer);
    }


// Attributes
    @Override
    public String getAccept() {
        return getAcceptImpl(getPeer());
    }
    static String getAcceptImpl(long peer) {
        return HTMLInputElementNative.getAccept(peer);
    }

    @Override
    public void setAccept(String value) {
        setAcceptImpl(getPeer(), value);
    }
    static void setAcceptImpl(long peer, String value) {
        HTMLInputElementNative.setAccept(peer, value);
    }

    @Override
    public String getAlt() {
        return getAltImpl(getPeer());
    }
    static String getAltImpl(long peer) {
        return HTMLInputElementNative.getAlt(peer);
    }

    @Override
    public void setAlt(String value) {
        setAltImpl(getPeer(), value);
    }
    static void setAltImpl(long peer, String value) {
        HTMLInputElementNative.setAlt(peer, value);
    }

    public String getAutocomplete() {
        return getAutocompleteImpl(getPeer());
    }
    static String getAutocompleteImpl(long peer) {
        return HTMLInputElementNative.getAutocomplete(peer);
    }

    public void setAutocomplete(String value) {
        setAutocompleteImpl(getPeer(), value);
    }
    static void setAutocompleteImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.setAutocompleteImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    public boolean getAutofocus() {
        return getAutofocusImpl(getPeer());
    }
    static boolean getAutofocusImpl(long peer) {
        return HTMLInputElementNative.getAutofocus(peer);
    }

    public void setAutofocus(boolean value) {
        setAutofocusImpl(getPeer(), value);
    }
    static void setAutofocusImpl(long peer, boolean value) {
        HTMLInputElementNative.setAutofocus(peer, value);
    }

    @Override
    public boolean getDefaultChecked() {
        return getDefaultCheckedImpl(getPeer());
    }
    static boolean getDefaultCheckedImpl(long peer) {
        return HTMLInputElementNative.getDefaultChecked(peer);
    }

    @Override
    public void setDefaultChecked(boolean value) {
        setDefaultCheckedImpl(getPeer(), value);
    }
    static void setDefaultCheckedImpl(long peer, boolean value) {
        HTMLInputElementNative.setDefaultChecked(peer, value);
    }

    @Override
    public boolean getChecked() {
        return getCheckedImpl(getPeer());
    }
    static boolean getCheckedImpl(long peer) {
        return HTMLInputElementNative.getChecked(peer);
    }

    @Override
    public void setChecked(boolean value) {
        setCheckedImpl(getPeer(), value);
    }
    static void setCheckedImpl(long peer, boolean value) {
        HTMLInputElementNative.setChecked(peer, value);
    }

    public String getDirName() {
        return getDirNameImpl(getPeer());
    }
    static String getDirNameImpl(long peer) {
        return HTMLInputElementNative.getDirName(peer);
    }

    public void setDirName(String value) {
        setDirNameImpl(getPeer(), value);
    }
    static void setDirNameImpl(long peer, String value) {
        HTMLInputElementNative.setDirName(peer, value);
    }

    @Override
    public boolean getDisabled() {
        return getDisabledImpl(getPeer());
    }
    static boolean getDisabledImpl(long peer) {
        return HTMLInputElementNative.getDisabled(peer);
    }

    @Override
    public void setDisabled(boolean value) {
        setDisabledImpl(getPeer(), value);
    }
    static void setDisabledImpl(long peer, boolean value) {
        HTMLInputElementNative.setDisabled(peer, value);
    }

    @Override
    public HTMLFormElement getForm() {
        return HTMLFormElementImpl.getImpl(getFormImpl(getPeer()));
    }
    static long getFormImpl(long peer) {
        return HTMLInputElementNative.getForm(peer);
    }

    public String getFormAction() {
        return getFormActionImpl(getPeer());
    }
    static String getFormActionImpl(long peer) {
        return HTMLInputElementNative.getFormAction(peer);
    }

    public void setFormAction(String value) {
        setFormActionImpl(getPeer(), value);
    }
    static void setFormActionImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.setFormActionImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    public String getFormEnctype() {
        return getFormEnctypeImpl(getPeer());
    }
    static String getFormEnctypeImpl(long peer) {
        return HTMLInputElementNative.getFormEnctype(peer);
    }

    public void setFormEnctype(String value) {
        setFormEnctypeImpl(getPeer(), value);
    }
    static void setFormEnctypeImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.setFormEnctypeImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    public String getFormMethod() {
        return getFormMethodImpl(getPeer());
    }
    static String getFormMethodImpl(long peer) {
        return HTMLInputElementNative.getFormMethod(peer);
    }

    public void setFormMethod(String value) {
        setFormMethodImpl(getPeer(), value);
    }
    static void setFormMethodImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.setFormMethodImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    public boolean getFormNoValidate() {
        return getFormNoValidateImpl(getPeer());
    }
    static boolean getFormNoValidateImpl(long peer) {
        return HTMLInputElementNative.getFormNoValidate(peer);
    }

    public void setFormNoValidate(boolean value) {
        setFormNoValidateImpl(getPeer(), value);
    }
    static void setFormNoValidateImpl(long peer, boolean value) {
        HTMLInputElementNative.setFormNoValidate(peer, value);
    }

    public String getFormTarget() {
        return getFormTargetImpl(getPeer());
    }
    static String getFormTargetImpl(long peer) {
        return HTMLInputElementNative.getFormTarget(peer);
    }

    public void setFormTarget(String value) {
        setFormTargetImpl(getPeer(), value);
    }
    static void setFormTargetImpl(long peer, String value) {
        HTMLInputElementNative.setFormTarget(peer, value);
    }

    public int getHeight() {
        return getHeightImpl(getPeer());
    }
    static int getHeightImpl(long peer) {
        return HTMLInputElementNative.getHeight(peer);
    }

    public void setHeight(int value) {
        setHeightImpl(getPeer(), value);
    }
    static void setHeightImpl(long peer, int value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.setHeightImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    public boolean getIndeterminate() {
        return getIndeterminateImpl(getPeer());
    }
    static boolean getIndeterminateImpl(long peer) {
        return HTMLInputElementNative.getIndeterminate(peer);
    }

    public void setIndeterminate(boolean value) {
        setIndeterminateImpl(getPeer(), value);
    }
    static void setIndeterminateImpl(long peer, boolean value) {
        HTMLInputElementNative.setIndeterminate(peer, value);
    }

    public String getMax() {
        return getMaxImpl(getPeer());
    }
    static String getMaxImpl(long peer) {
        return HTMLInputElementNative.getMax(peer);
    }

    public void setMax(String value) {
        setMaxImpl(getPeer(), value);
    }
    static void setMaxImpl(long peer, String value) {
        HTMLInputElementNative.setMax(peer, value);
    }

    @Override
    public int getMaxLength() {
        return getMaxLengthImpl(getPeer());
    }
    static int getMaxLengthImpl(long peer) {
        return HTMLInputElementNative.getMaxLength(peer);
    }

    @Override
    public void setMaxLength(int value) throws DOMException {
        setMaxLengthImpl(getPeer(), value);
    }
    static void setMaxLengthImpl(long peer, int value) {
        HTMLInputElementNative.setMaxLength(peer, value);
    }

    public String getMin() {
        return getMinImpl(getPeer());
    }
    static String getMinImpl(long peer) {
        return HTMLInputElementNative.getMin(peer);
    }

    public void setMin(String value) {
        setMinImpl(getPeer(), value);
    }
    static void setMinImpl(long peer, String value) {
        HTMLInputElementNative.setMin(peer, value);
    }

    public boolean getMultiple() {
        return getMultipleImpl(getPeer());
    }
    static boolean getMultipleImpl(long peer) {
        return HTMLInputElementNative.getMultiple(peer);
    }

    public void setMultiple(boolean value) {
        setMultipleImpl(getPeer(), value);
    }
    static void setMultipleImpl(long peer, boolean value) {
        HTMLInputElementNative.setMultiple(peer, value);
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLInputElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLInputElementNative.setName(peer, value);
    }

    public String getPattern() {
        return getPatternImpl(getPeer());
    }
    static String getPatternImpl(long peer) {
        return HTMLInputElementNative.getPattern(peer);
    }

    public void setPattern(String value) {
        setPatternImpl(getPeer(), value);
    }
    static void setPatternImpl(long peer, String value) {
        HTMLInputElementNative.setPattern(peer, value);
    }

    public String getPlaceholder() {
        return getPlaceholderImpl(getPeer());
    }
    static String getPlaceholderImpl(long peer) {
        return HTMLInputElementNative.getPlaceholder(peer);
    }

    public void setPlaceholder(String value) {
        setPlaceholderImpl(getPeer(), value);
    }
    static void setPlaceholderImpl(long peer, String value) {
        HTMLInputElementNative.setPlaceholder(peer, value);
    }

    @Override
    public boolean getReadOnly() {
        return getReadOnlyImpl(getPeer());
    }
    static boolean getReadOnlyImpl(long peer) {
        return HTMLInputElementNative.getReadOnly(peer);
    }

    @Override
    public void setReadOnly(boolean value) {
        setReadOnlyImpl(getPeer(), value);
    }
    static void setReadOnlyImpl(long peer, boolean value) {
        HTMLInputElementNative.setReadOnly(peer, value);
    }

    public boolean getRequired() {
        return getRequiredImpl(getPeer());
    }
    static boolean getRequiredImpl(long peer) {
        return HTMLInputElementNative.getRequired(peer);
    }

    public void setRequired(boolean value) {
        setRequiredImpl(getPeer(), value);
    }
    static void setRequiredImpl(long peer, boolean value) {
        HTMLInputElementNative.setRequired(peer, value);
    }

    @Override
    public String getSize() {
        return getSizeImpl(getPeer())+"";
    }
    static String getSizeImpl(long peer) {
        return HTMLInputElementNative.getSize(peer);
    }

    @Override
    public void setSize(String value) {
        setSizeImpl(getPeer(), value);
    }
    static void setSizeImpl(long peer, String value) {
        HTMLInputElementNative.setSize(peer, value);
    }

    @Override
    public String getSrc() {
        return getSrcImpl(getPeer());
    }
    static String getSrcImpl(long peer) {
        return HTMLInputElementNative.getSrc(peer);
    }

    @Override
    public void setSrc(String value) {
        setSrcImpl(getPeer(), value);
    }
    static void setSrcImpl(long peer, String value) {
        HTMLInputElementNative.setSrc(peer, value);
    }

    public String getStep() {
        return getStepImpl(getPeer());
    }
    static String getStepImpl(long peer) {
        return HTMLInputElementNative.getStep(peer);
    }

    public void setStep(String value) {
        setStepImpl(getPeer(), value);
    }
    static void setStepImpl(long peer, String value) {
        HTMLInputElementNative.setStep(peer, value);
    }

    @Override
    public String getType() {
        return getTypeImpl(getPeer());
    }
    static String getTypeImpl(long peer) {
        return HTMLInputElementNative.getType(peer);
    }

    public void setType(String value) {
        setTypeImpl(getPeer(), value);
    }
    static void setTypeImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.setTypeImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    @Override
    public String getDefaultValue() {
        return getDefaultValueImpl(getPeer());
    }
    static String getDefaultValueImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.getDefaultValueImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    @Override
    public void setDefaultValue(String value) {
        setDefaultValueImpl(getPeer(), value);
    }
    static void setDefaultValueImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.setDefaultValueImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    @Override
    public String getValue() {
        return getValueImpl(getPeer());
    }
    static String getValueImpl(long peer) {
        return HTMLInputElementNative.getValue(peer);
    }

    @Override
    public void setValue(String value) {
        setValueImpl(getPeer(), value);
    }
    static void setValueImpl(long peer, String value) {
        HTMLInputElementNative.setValue(peer, value);
    }

    public long getValueAsDate() {
        return getValueAsDateImpl(getPeer());
    }
    static long getValueAsDateImpl(long peer) {
        return HTMLInputElementNative.getValueAsDate(peer);
    }

    public void setValueAsDate(long value) throws DOMException {
        setValueAsDateImpl(getPeer(), value);
    }
    static void setValueAsDateImpl(long peer, long value) {
        HTMLInputElementNative.setValueAsDate(peer, value);
    }

    public double getValueAsNumber() {
        return getValueAsNumberImpl(getPeer());
    }
    static double getValueAsNumberImpl(long peer) {
        return HTMLInputElementNative.getValueAsNumber(peer);
    }

    public void setValueAsNumber(double value) throws DOMException {
        setValueAsNumberImpl(getPeer(), value);
    }
    static void setValueAsNumberImpl(long peer, double value) {
        HTMLInputElementNative.setValueAsNumber(peer, value);
    }

    public int getWidth() {
        return getWidthImpl(getPeer());
    }
    static int getWidthImpl(long peer) {
        return HTMLInputElementNative.getWidth(peer);
    }

    public void setWidth(int value) {
        setWidthImpl(getPeer(), value);
    }
    static void setWidthImpl(long peer, int value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.setWidthImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    public boolean getWillValidate() {
        return getWillValidateImpl(getPeer());
    }
    static boolean getWillValidateImpl(long peer) {
        return HTMLInputElementNative.getWillValidate(peer);
    }

    public String getValidationMessage() {
        return getValidationMessageImpl(getPeer());
    }
    static String getValidationMessageImpl(long peer) {
        return HTMLInputElementNative.getValidationMessage(peer);
    }

    public NodeList getLabels() {
        return NodeListImpl.getImpl(getLabelsImpl(getPeer()));
    }
    static long getLabelsImpl(long peer) {
        return HTMLInputElementNative.getLabels(peer);
    }

    @Override
    public String getAlign() {
        return getAlignImpl(getPeer());
    }
    static String getAlignImpl(long peer) {
        return HTMLInputElementNative.getAlign(peer);
    }

    @Override
    public void setAlign(String value) {
        setAlignImpl(getPeer(), value);
    }
    static void setAlignImpl(long peer, String value) {
        HTMLInputElementNative.setAlign(peer, value);
    }

    @Override
    public String getUseMap() {
        return getUseMapImpl(getPeer());
    }
    static String getUseMapImpl(long peer) {
        return HTMLInputElementNative.getUseMap(peer);
    }

    @Override
    public void setUseMap(String value) {
        setUseMapImpl(getPeer(), value);
    }
    static void setUseMapImpl(long peer, String value) {
        HTMLInputElementNative.setUseMap(peer, value);
    }

    public boolean getIncremental() {
        return getIncrementalImpl(getPeer());
    }
    static boolean getIncrementalImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.getIncrementalImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    public void setIncremental(boolean value) {
        setIncrementalImpl(getPeer(), value);
    }
    static void setIncrementalImpl(long peer, boolean value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLInputElementImpl.setIncrementalImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    @Override
    public String getAccessKey() {
        return getAccessKeyImpl(getPeer());
    }
    static String getAccessKeyImpl(long peer) {
        return HTMLInputElementNative.getAccessKey(peer);
    }

    @Override
    public void setAccessKey(String value) {
        setAccessKeyImpl(getPeer(), value);
    }
    static void setAccessKeyImpl(long peer, String value) {
        HTMLInputElementNative.setAccessKey(peer, value);
    }


// Functions
    public void stepUp(int n) throws DOMException
    {
        stepUpImpl(getPeer()
            , n);
    }
    static void stepUpImpl(long peer
        , int n) {
        HTMLInputElementNative.stepUp(peer, n);
    }


    public void stepDown(int n) throws DOMException
    {
        stepDownImpl(getPeer()
            , n);
    }
    static void stepDownImpl(long peer
        , int n) {
        HTMLInputElementNative.stepDown(peer, n);
    }


    public boolean checkValidity()
    {
        return checkValidityImpl(getPeer());
    }
    static boolean checkValidityImpl(long peer) {
        return HTMLInputElementNative.checkValidity(peer);
    }


    public void setCustomValidity(String error)
    {
        setCustomValidityImpl(getPeer()
            , error);
    }
    static void setCustomValidityImpl(long peer
        , String error) {
        HTMLInputElementNative.setCustomValidity(peer, error);
    }


    @Override
    public void select()
    {
        selectImpl(getPeer());
    }
    static void selectImpl(long peer) {
        HTMLInputElementNative.select(peer);
    }


    public void setRangeText(String replacement) throws DOMException
    {
        setRangeTextImpl(getPeer()
            , replacement);
    }
    static void setRangeTextImpl(long peer
        , String replacement) {
        HTMLInputElementNative.setRangeText(peer, replacement);
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
        HTMLInputElementNative.setRangeTextEx(peer, replacement, start, end, selectionMode);
    }


    @Override
    public void click()
    {
        clickImpl(getPeer());
    }
    static void clickImpl(long peer) {
        HTMLInputElementNative.click(peer);
    }


    public void setValueForUser(String value)
    {
        setValueForUserImpl(getPeer()
            , value);
    }
    static void setValueForUserImpl(long peer
        , String value) {
        HTMLInputElementNative.setValueForUser(peer, value);
    }


}

