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

import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLLabelElement;

public class HTMLLabelElementImpl extends HTMLElementImpl implements HTMLLabelElement {
    HTMLLabelElementImpl(long peer) {
        super(peer);
    }

    static HTMLLabelElement getImpl(long peer) {
        return (HTMLLabelElement)create(peer);
    }


// Attributes
    public HTMLFormElement getForm() {
        return HTMLFormElementImpl.getImpl(getFormImpl(getPeer()));
    }
    native static long getFormImpl(long peer);

    public String getHtmlFor() {
        return getHtmlForImpl(getPeer());
    }
    native static String getHtmlForImpl(long peer);

    public void setHtmlFor(String value) {
        setHtmlForImpl(getPeer(), value);
    }
    native static void setHtmlForImpl(long peer, String value);

    public HTMLElement getControl() {
        return HTMLElementImpl.getImpl(getControlImpl(getPeer()));
    }
    native static long getControlImpl(long peer);

    public String getAccessKey() {
        return getAccessKeyImpl(getPeer());
    }
    native static String getAccessKeyImpl(long peer);

    public void setAccessKey(String value) {
        setAccessKeyImpl(getPeer(), value);
    }
    native static void setAccessKeyImpl(long peer, String value);

}

