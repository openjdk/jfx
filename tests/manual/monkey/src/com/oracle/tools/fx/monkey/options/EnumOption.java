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
package com.oracle.tools.fx.monkey.options;

import java.util.Arrays;
import java.util.Comparator;
import javafx.beans.property.ObjectProperty;

/**
 * Enum Option Bound to a Property.
 */
public class EnumOption<T extends Enum> extends ObjectOption<T> {
    public EnumOption(String name, boolean allowNull, Class<T> type, ObjectProperty<T> p) {
        super(name, p);

        T[] values = type.getEnumConstants();
        Arrays.sort(values, new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return a.toString().compareTo(b.toString());
            }
        });

        if (allowNull) {
            addChoice("<null>", null);
        }
        for (T v : values) {
            addChoice(v.toString(), v);
        }

        selectInitialValue();
    }

    public EnumOption(String name, Class<T> type, ObjectProperty<T> p) {
        this(name, true, type, p);
    }
}
