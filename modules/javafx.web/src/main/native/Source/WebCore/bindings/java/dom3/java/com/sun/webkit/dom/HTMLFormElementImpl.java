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

import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLFormElement;

public class HTMLFormElementImpl extends HTMLElementImpl implements HTMLFormElement {
    HTMLFormElementImpl(long peer) {
        super(peer);
    }

    static HTMLFormElement getImpl(long peer) {
        return (HTMLFormElement)create(peer);
    }


// Attributes
    @Override
    public String getAcceptCharset() {
        return getAcceptCharsetImpl(getPeer());
    }
    static String getAcceptCharsetImpl(long peer) {
        return HTMLFormElementNative.getAcceptCharset(peer);
    }

    @Override
    public void setAcceptCharset(String value) {
        setAcceptCharsetImpl(getPeer(), value);
    }
    static void setAcceptCharsetImpl(long peer, String value) {
        HTMLFormElementNative.setAcceptCharset(peer, value);
    }

    @Override
    public String getAction() {
        return getActionImpl(getPeer());
    }
    static String getActionImpl(long peer) {
        return HTMLFormElementNative.getAction(peer);
    }

    @Override
    public void setAction(String value) {
        setActionImpl(getPeer(), value);
    }
    static void setActionImpl(long peer, String value) {
        HTMLFormElementNative.setAction(peer, value);
    }

    public String getAutocomplete() {
        return getAutocompleteImpl(getPeer());
    }
    static String getAutocompleteImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLFormElementImpl.getAutocompleteImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    public void setAutocomplete(String value) {
        setAutocompleteImpl(getPeer(), value);
    }
    static void setAutocompleteImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLFormElementImpl.setAutocompleteImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    @Override
    public String getEnctype() {
        return getEnctypeImpl(getPeer());
    }
    static String getEnctypeImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLFormElementImpl.getEnctypeImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    @Override
    public void setEnctype(String value) {
        setEnctypeImpl(getPeer(), value);
    }
    static void setEnctypeImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLFormElementImpl.setEnctypeImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    public String getEncoding() {
        return getEncodingImpl(getPeer());
    }
    static String getEncodingImpl(long peer) {
        return HTMLFormElementNative.getEncoding(peer);
    }

    public void setEncoding(String value) {
        setEncodingImpl(getPeer(), value);
    }
    static void setEncodingImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLFormElementImpl.setEncodingImpl: no wkj_* function"
                + " exists for it in any jfxwebkit build");
    }

    @Override
    public String getMethod() {
        return getMethodImpl(getPeer());
    }
    static String getMethodImpl(long peer) {
        return HTMLFormElementNative.getMethod(peer);
    }

    @Override
    public void setMethod(String value) {
        setMethodImpl(getPeer(), value);
    }
    static void setMethodImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLFormElementImpl.setMethodImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLFormElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLFormElementNative.setName(peer, value);
    }

    public boolean getNoValidate() {
        return getNoValidateImpl(getPeer());
    }
    static boolean getNoValidateImpl(long peer) {
        return HTMLFormElementNative.getNoValidate(peer);
    }

    public void setNoValidate(boolean value) {
        setNoValidateImpl(getPeer(), value);
    }
    static void setNoValidateImpl(long peer, boolean value) {
        HTMLFormElementNative.setNoValidate(peer, value);
    }

    @Override
    public String getTarget() {
        return getTargetImpl(getPeer());
    }
    static String getTargetImpl(long peer) {
        return HTMLFormElementNative.getTarget(peer);
    }

    @Override
    public void setTarget(String value) {
        setTargetImpl(getPeer(), value);
    }
    static void setTargetImpl(long peer, String value) {
        HTMLFormElementNative.setTarget(peer, value);
    }

    @Override
    public HTMLCollection getElements() {
        return HTMLCollectionImpl.getImpl(getElementsImpl(getPeer()));
    }
    static long getElementsImpl(long peer) {
        return HTMLFormElementNative.getElements(peer);
    }

    @Override
    public int getLength() {
        return getLengthImpl(getPeer());
    }
    static int getLengthImpl(long peer) {
        return HTMLFormElementNative.getLength(peer);
    }


// Functions
    @Override
    public void submit()
    {
        submitImpl(getPeer());
    }
    static void submitImpl(long peer) {
        HTMLFormElementNative.submit(peer);
    }


    @Override
    public void reset()
    {
        resetImpl(getPeer());
    }
    static void resetImpl(long peer) {
        HTMLFormElementNative.reset(peer);
    }


    public boolean checkValidity()
    {
        return checkValidityImpl(getPeer());
    }
    static boolean checkValidityImpl(long peer) {
        return HTMLFormElementNative.checkValidity(peer);
    }


}

