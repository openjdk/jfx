/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import com.sun.javafx.util.Utils;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableFloatProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableLongProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleableStringProperty;
import java.util.Objects;

public final class StyleablePropertyHelper {

    private StyleablePropertyHelper() {}

    static {
        Utils.forceInit(StyleableBooleanProperty.class);
        Utils.forceInit(StyleableDoubleProperty.class);
        Utils.forceInit(StyleableFloatProperty.class);
        Utils.forceInit(StyleableIntegerProperty.class);
        Utils.forceInit(StyleableLongProperty.class);
        Utils.forceInit(StyleableObjectProperty.class);
        Utils.forceInit(StyleableStringProperty.class);
    }

    private static Accessor booleanAccessor;
    private static Accessor doubleAccessor;
    private static Accessor floatAccessor;
    private static Accessor integerAccessor;
    private static Accessor longAccessor;
    private static Accessor objectAccessor;
    private static Accessor stringAccessor;

    public static void setBooleanAccessor(Accessor accessor) {
        booleanAccessor = accessor;
    }

    public static void setDoubleAccessor(Accessor accessor) {
        doubleAccessor = accessor;
    }

    public static void setFloatAccessor(Accessor accessor) {
        floatAccessor = accessor;
    }

    public static void setIntegerAccessor(Accessor accessor) {
        integerAccessor = accessor;
    }

    public static void setLongAccessor(Accessor accessor) {
        longAccessor = accessor;
    }

    public static void setObjectAccessor(Accessor accessor) {
        objectAccessor = accessor;
    }

    public static void setStringAccessor(Accessor accessor) {
        stringAccessor = accessor;
    }

    public static boolean equalsEndValue(StyleableProperty<?> property, Object value) {
        return switch (property) {
            case StyleableDoubleProperty p -> doubleAccessor.equalsEndValue(p, value);
            case StyleableObjectProperty<?> p -> objectAccessor.equalsEndValue(p, value);
            case StyleableBooleanProperty p -> booleanAccessor.equalsEndValue(p, value);
            case StyleableStringProperty p -> stringAccessor.equalsEndValue(p, value);
            case StyleableIntegerProperty p -> integerAccessor.equalsEndValue(p, value);
            case StyleableLongProperty p -> longAccessor.equalsEndValue(p, value);
            case StyleableFloatProperty p -> floatAccessor.equalsEndValue(p, value);
            default -> Objects.equals(property.getValue(), value);
        };
    }

    public interface Accessor {
        boolean equalsEndValue(StyleableProperty<?> property, Object value);
    }
}
