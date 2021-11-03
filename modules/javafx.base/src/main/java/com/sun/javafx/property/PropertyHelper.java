/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.property;

import com.sun.javafx.binding.ExpressionHelperBase;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableValue;

import static com.sun.javafx.beans.BeanErrors.*;

public class PropertyHelper {

    private PropertyHelper() {}

    public static String toString(
            ReadOnlyProperty<?> property, Class<? extends ReadOnlyProperty> displayClass) {
        return toString(property, displayClass, false);
    }

    public static String toString(
            ReadOnlyProperty<?> property, Class<? extends ReadOnlyProperty> displayClass, boolean valid) {
        StringBuilder result = new StringBuilder(displayClass.getSimpleName()).append(" [");
        Object bean = property.getBean();
        String name = property.getName();

        if (bean != null) {
            result.append("bean: ").append(bean).append(", ");
        }

        if (name != null && !name.isBlank()) {
            result.append("name: ").append(name).append(", ");
        }

        if (property instanceof Property<?> && ((Property<?>)property).isBound()) {
            result.append("bound, ");

            if (valid) {
                result.append("value: ").append(property.getValue());
            } else {
                result.append("invalid");
            }
        } else {
            result.append("value: ").append(property.getValue());
        }

        result.append("]");
        return result.toString();
    }

    public static void checkBind(Property<?> self, ObservableValue<?> source, ExpressionHelperBase helper) {
        if (source == null) {
            throw new NullPointerException(BINDING_SOURCE_NULL.getMessage());
        }

        if (self == source) {
            throw new IllegalArgumentException(CANNOT_BIND_PROPERTY_TO_ITSELF.getMessage(self));
        }

        if (ExpressionHelperBase.isBoundBidirectional(helper)) {
            throw new IllegalStateException(BIND_CONFLICT_UNIDIRECTIONAL.getMessage(self));
        }
    }

}
