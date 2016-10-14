/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.mac;

import java.lang.annotation.Native;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import javafx.geometry.Bounds;

final class MacVariant {
    @Native final static int NSArray_id = 1;
    @Native final static int NSArray_NSString = 2;
    @Native final static int NSArray_int = 3;
    @Native final static int NSArray_range = 4;
    @Native final static int NSAttributedString = 5; /* Uses string for the text and variantArray for the styles */
    @Native final static int NSData = 6;
    @Native final static int NSDate = 7;
    @Native final static int NSDictionary = 8; /* Uses longArray for keys (NSString) and variantArray for values */
    @Native final static int NSNumber_Boolean = 9;
    @Native final static int NSNumber_Int = 10;
    @Native final static int NSNumber_Float = 11;
    @Native final static int NSNumber_Double = 12;
    @Native final static int NSString = 13;
    @Native final static int NSURL = 14;
    @Native final static int NSValue_point = 15;
    @Native final static int NSValue_size = 16;
    @Native final static int NSValue_rectangle = 17;
    @Native final static int NSValue_range = 18;
    @Native final static int NSObject = 19; /* id */

    int type;
    long[] longArray;
    int[] intArray;
    String[] stringArray;
    MacVariant[] variantArray; /* Used by NSAttributedString and NSDictionary */
    float float1;
    float float2;
    float float3;
    float float4;
    int int1;
    int int2;
    String string;
    long long1;
    double double1;

    /* Used when the Variant represents an attribute within a NSAttributedString */
    int location;
    int length;
    long key;

    static MacVariant createNSArray(Object result) {
        MacVariant variant = new MacVariant();
        variant.type = NSArray_id;
        variant.longArray = (long[])result;
        return variant;
    }

    static MacVariant createNSObject(Object result) {
        MacVariant variant = new MacVariant();
        variant.type = NSObject;
        variant.long1 = (Long)result;
        return variant;
    }

    static MacVariant createNSString(Object result) {
        MacVariant variant = new MacVariant();
        variant.type = NSString;
        variant.string = (String)result;
        return variant;
    }

    static MacVariant createNSAttributedString(Object result) {
        MacVariant variant = new MacVariant();
        variant.type = NSAttributedString;
        variant.string = (String)result;
        return variant;
    }

    static MacVariant createNSDate(Object result) {
        /* Note: [NSDate dateWithTimeIntervalSince1970] used natively
         * takes the number of seconds from the first instant of 1 January 1970, GMT.
         */
        MacVariant variant = new MacVariant();
        variant.type = NSDate;
        variant.long1 = ((LocalDate)result).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        return variant;
    }

    static MacVariant createNSValueForSize(Object result) {
        Bounds bounds = (Bounds)result;
        MacVariant variant = new MacVariant();
        variant.type = NSValue_size;
        variant.float1 = (float)bounds.getWidth();
        variant.float2 = (float)bounds.getHeight();
        return variant;
    }

    static MacVariant createNSValueForPoint(Object result) {
        Bounds bounds = (Bounds)result;
        MacVariant variant = new MacVariant();
        variant.type = NSValue_point;
        variant.float1 = (float)bounds.getMinX();
        variant.float2 = (float)bounds.getMinY();
        return variant;
    }

    static MacVariant createNSValueForRectangle(Object result) {
        Bounds bounds = (Bounds)result;
        MacVariant variant = new MacVariant();
        variant.type = NSValue_rectangle;
        variant.float1 = (float)bounds.getMinX();
        variant.float2 = (float)bounds.getMinY();
        variant.float3 = (float)bounds.getWidth();
        variant.float4 = (float)bounds.getHeight();
        return variant;
    }

    static MacVariant createNSValueForRange(Object result) {
        int[] range = (int[])result;
        MacVariant variant = new MacVariant();
        variant.type = NSValue_range;
        variant.int1 = range[0];
        variant.int2 = range[1];
        return variant;
    }

    static MacVariant createNSNumberForBoolean(Object result) {
        Boolean value = (Boolean)result;
        MacVariant variant = new MacVariant();
        variant.type = NSNumber_Boolean;
        variant.int1 = value ? 1 : 0;
        return variant;
    }

    static MacVariant createNSNumberForDouble(Object result) {
        MacVariant variant = new MacVariant();
        variant.type = NSNumber_Double;
        variant.double1 = (Double)result;
        return variant;
    }

    static MacVariant createNSNumberForInt(Object result) {
        MacVariant variant = new MacVariant();
        variant.type = NSNumber_Int;
        variant.int1 = (Integer)result;
        return variant;
    }

    Object getValue() {
        switch (type) {
            case NSNumber_Boolean: return int1 != 0;
            case NSNumber_Int: return int1;
            case NSNumber_Double: return double1;
            case NSArray_id: return longArray;
            case NSArray_int: return intArray;
            case NSValue_range: return new int[] {int1, int2};
            case NSValue_point: return new float[] {float1, float2};
            case NSValue_size: return new float[] {float1, float2};
            case NSValue_rectangle: return new float[] {float1, float2, float3, float4};
            case NSString: return string;
            case NSAttributedString: return string;
            //TODO REST
        }
        return null;
    }

    @Override
    public String toString() {
        Object v = getValue();
        switch (type) {
            case NSArray_id: v = Arrays.toString((long[])v); break;
            case NSArray_int: v = Arrays.toString((int[])v); break;
            case NSValue_range: v = Arrays.toString((int[])v); break;
            case NSAttributedString: v += Arrays.toString(variantArray); break;
            case NSDictionary: v = "keys: " + Arrays.toString(longArray) + " values: " + Arrays.toString(variantArray);
        }
        return "MacVariant type: " + type + " value " + v;
    }
}
