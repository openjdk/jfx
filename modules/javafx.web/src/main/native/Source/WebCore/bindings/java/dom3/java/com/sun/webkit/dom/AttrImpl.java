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

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;

public class AttrImpl extends NodeImpl implements Attr {
    AttrImpl(long peer) {
        super(peer);
    }

    static Attr getImpl(long peer) {
        return (Attr)create(peer);
    }


// Attributes
    public String getName() {
        return getNameImpl(getPeer());
    }
    native static String getNameImpl(long peer);

    public boolean getSpecified() {
        return getSpecifiedImpl(getPeer());
    }
    native static boolean getSpecifiedImpl(long peer);

    public String getValue() {
        return getValueImpl(getPeer());
    }
    native static String getValueImpl(long peer);

    public void setValue(String value) throws DOMException {
        setValueImpl(getPeer(), value);
    }
    native static void setValueImpl(long peer, String value);

    public Element getOwnerElement() {
        return ElementImpl.getImpl(getOwnerElementImpl(getPeer()));
    }
    native static long getOwnerElementImpl(long peer);

    public boolean isId() {
        return isIdImpl(getPeer());
    }
    native static boolean isIdImpl(long peer);


//stubs
    public TypeInfo getSchemaTypeInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

