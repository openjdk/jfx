/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

import java.util.HashMap;
import java.util.Random;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.StringConverter;

/**
 * Elastic Data Row (With Randomly Generated Values)
 */
public class DataRow {
    private final HashMap<Object, ObjectProperty> values = new HashMap();
    private static StringConverter<Object> converter;

    public DataRow() {
    }

    public ObjectProperty getValue(Object key) {
        ObjectProperty rv = values.get(key);
        if (rv == null) {
            Object v = createValue();
            rv = new SimpleObjectProperty(v);
            values.put(key, rv);
        }
        return rv;
    }

    private Object createValue() {
        // TODO doubles, longs, strings, integers, boolean
        return String.valueOf(new Random().nextInt());
    }

    public static StringConverter<Object> converter() {
        if (converter == null) {
            converter = new StringConverter<>() {
                @Override
                public String toString(Object x) {
                    return x == null ? null : x.toString();
                }

                @Override
                public Object fromString(String s) {
                    return s;
                }
            };
        }
        return converter;
    }
}
