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

import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLOptionElement;

public class HTMLOptionElementImpl extends HTMLElementImpl implements HTMLOptionElement {
    HTMLOptionElementImpl(long peer) {
        super(peer);
    }

    static HTMLOptionElement getImpl(long peer) {
        return (HTMLOptionElement)create(peer);
    }


// Attributes
    @Override
    public boolean getDisabled() {
        return getDisabledImpl(getPeer());
    }
    static boolean getDisabledImpl(long peer) {
        return HTMLOptionElementNative.getDisabled(peer);
    }

    @Override
    public void setDisabled(boolean value) {
        setDisabledImpl(getPeer(), value);
    }
    static void setDisabledImpl(long peer, boolean value) {
        HTMLOptionElementNative.setDisabled(peer, value);
    }

    @Override
    public HTMLFormElement getForm() {
        return HTMLFormElementImpl.getImpl(getFormImpl(getPeer()));
    }
    static long getFormImpl(long peer) {
        return HTMLOptionElementNative.getForm(peer);
    }

    @Override
    public String getLabel() {
        return getLabelImpl(getPeer());
    }
    static String getLabelImpl(long peer) {
        return HTMLOptionElementNative.getLabel(peer);
    }

    @Override
    public void setLabel(String value) {
        setLabelImpl(getPeer(), value);
    }
    static void setLabelImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLOptionElementImpl.setLabelImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    @Override
    public boolean getDefaultSelected() {
        return getDefaultSelectedImpl(getPeer());
    }
    static boolean getDefaultSelectedImpl(long peer) {
        return HTMLOptionElementNative.getDefaultSelected(peer);
    }

    @Override
    public void setDefaultSelected(boolean value) {
        setDefaultSelectedImpl(getPeer(), value);
    }
    static void setDefaultSelectedImpl(long peer, boolean value) {
        HTMLOptionElementNative.setDefaultSelected(peer, value);
    }

    @Override
    public boolean getSelected() {
        return getSelectedImpl(getPeer());
    }
    static boolean getSelectedImpl(long peer) {
        return HTMLOptionElementNative.getSelected(peer);
    }

    @Override
    public void setSelected(boolean value) {
        setSelectedImpl(getPeer(), value);
    }
    static void setSelectedImpl(long peer, boolean value) {
        HTMLOptionElementNative.setSelected(peer, value);
    }

    @Override
    public String getValue() {
        return getValueImpl(getPeer());
    }
    static String getValueImpl(long peer) {
        return HTMLOptionElementNative.getValue(peer);
    }

    @Override
    public void setValue(String value) {
        setValueImpl(getPeer(), value);
    }
    static void setValueImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLOptionElementImpl.setValueImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    @Override
    public String getText() {
        return getTextImpl(getPeer());
    }
    static String getTextImpl(long peer) {
        return HTMLOptionElementNative.getText(peer);
    }

    @Override
    public int getIndex() {
        return getIndexImpl(getPeer());
    }
    static int getIndexImpl(long peer) {
        return HTMLOptionElementNative.getIndex(peer);
    }

}

