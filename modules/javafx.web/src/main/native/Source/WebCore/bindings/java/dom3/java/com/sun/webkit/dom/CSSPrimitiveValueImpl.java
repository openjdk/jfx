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
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.Counter;
import org.w3c.dom.css.RGBColor;
import org.w3c.dom.css.Rect;

public class CSSPrimitiveValueImpl extends CSSValueImpl implements CSSPrimitiveValue {
    CSSPrimitiveValueImpl(long peer) {
        super(peer);
    }

    static CSSPrimitiveValue getImpl(long peer) {
        return (CSSPrimitiveValue)create(peer);
    }


// Constants
    public static final int CSS_UNKNOWN = 0;
    public static final int CSS_NUMBER = 1;
    public static final int CSS_PERCENTAGE = 2;
    public static final int CSS_EMS = 3;
    public static final int CSS_EXS = 4;
    public static final int CSS_PX = 5;
    public static final int CSS_CM = 6;
    public static final int CSS_MM = 7;
    public static final int CSS_IN = 8;
    public static final int CSS_PT = 9;
    public static final int CSS_PC = 10;
    public static final int CSS_DEG = 11;
    public static final int CSS_RAD = 12;
    public static final int CSS_GRAD = 13;
    public static final int CSS_MS = 14;
    public static final int CSS_S = 15;
    public static final int CSS_HZ = 16;
    public static final int CSS_KHZ = 17;
    public static final int CSS_DIMENSION = 18;
    public static final int CSS_STRING = 19;
    public static final int CSS_URI = 20;
    public static final int CSS_IDENT = 21;
    public static final int CSS_ATTR = 22;
    public static final int CSS_COUNTER = 23;
    public static final int CSS_RECT = 24;
    public static final int CSS_RGBCOLOR = 25;
    public static final int CSS_VW = 26;
    public static final int CSS_VH = 27;
    public static final int CSS_VMIN = 28;
    public static final int CSS_VMAX = 29;

// Attributes
    public short getPrimitiveType() {
        return getPrimitiveTypeImpl(getPeer());
    }
    native static short getPrimitiveTypeImpl(long peer);


// Functions
    public void setFloatValue(short unitType
        , float floatValue) throws DOMException
    {
        setFloatValueImpl(getPeer()
            , unitType
            , floatValue);
    }
    native static void setFloatValueImpl(long peer
        , short unitType
        , float floatValue);


    public float getFloatValue(short unitType) throws DOMException
    {
        return getFloatValueImpl(getPeer()
            , unitType);
    }
    native static float getFloatValueImpl(long peer
        , short unitType);


    public void setStringValue(short stringType
        , String stringValue) throws DOMException
    {
        setStringValueImpl(getPeer()
            , stringType
            , stringValue);
    }
    native static void setStringValueImpl(long peer
        , short stringType
        , String stringValue);


    public String getStringValue() throws DOMException
    {
        return getStringValueImpl(getPeer());
    }
    native static String getStringValueImpl(long peer);


    public Counter getCounterValue() throws DOMException
    {
        return CounterImpl.getImpl(getCounterValueImpl(getPeer()));
    }
    native static long getCounterValueImpl(long peer);


    public Rect getRectValue() throws DOMException
    {
        return RectImpl.getImpl(getRectValueImpl(getPeer()));
    }
    native static long getRectValueImpl(long peer);


    public RGBColor getRGBColorValue() throws DOMException
    {
        return RGBColorImpl.getImpl(getRGBColorValueImpl(getPeer()));
    }
    native static long getRGBColorValueImpl(long peer);


}

