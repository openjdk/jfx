/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Region;

/**
 * Context Menus
 */
public class Menus {
    /**
     * Creates a submenu with the specified values.
     */
    public static <T> Menu subMenu(ContextMenu cm, String text, Function<T, String> fmt, Consumer<T> setter, Supplier<T> getter, T... values) {
        // we could pass the property instead and to highlight the chosen value with a checkmark for example
        Menu m = FX.menu(cm, text);
        for (T value: values) {
            T v = getter == null ? null : getter.get();
            String name;
            if(fmt == null) {
                name = String.valueOf(value);
            } else {
                name = fmt.apply(value);
            }
            if ((getter != null) && Objects.equals(v, value)) {
                name += " ✓";
            }
            item(m, name, () -> {
                setter.accept(value);
            });
        }
        return m;
    }

    public static void marginSubMenu(ContextMenu cm, Consumer<Insets> setter, Supplier<Insets> getter) {
        Insets[] values = {
            null,
            new Insets(0),
            new Insets(10, 0, 0, 0),
            new Insets(0, 10, 0, 0),
            new Insets(0, 0, 10, 0),
            new Insets(0, 0, 0, 10),
            new Insets(10, 20, 30, 40)
        };
        subMenu(cm, "Margin", Formats::formatInsets, setter, getter, values);
    }

    public static <E extends Enum> void enumSubMenu(ContextMenu cm, String text, Class<E> type, boolean includeNull, Consumer<E> setter, Supplier<E> getter) {
        E[] values = includeNull ? Utils.withNull(type) : type.getEnumConstants();
        subMenu(cm, text, null, setter, getter, values);
    }

    public static void intSubMenu(ContextMenu cm, String text, Consumer<Integer> setter, Supplier<Integer> getter, int min, int max) {
        ArrayList<Integer> vs = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            vs.add(i);
        }
        Integer[] values = vs.toArray(Integer[]::new);
        subMenu(cm, text, null, setter, getter, values);
    }

    public static void booleanSubMenu(ContextMenu cm, String text, Consumer<Boolean> setter, Supplier<Boolean> getter) {
        boolean val = Boolean.TRUE.equals(getter.get());
        FX.checkItem(cm, text, val, setter);
    }

    private static void subMenu(ContextMenu cm, String text, DoubleProperty p, double[] values) {
        Menu m = FX.menu(cm, text);
        double val = p.get();
        for (double v: values) {
            String name = format(v);
            if (v == val) {
                name += " ✓";
            }
            item(m, name, () -> {
                p.set(v);
            });
        }
    }

    public static void sizeSubMenus(ContextMenu cm, Region r) {
        double[] min = {
            Region.USE_COMPUTED_SIZE,
            Region.USE_PREF_SIZE,
            0,
            10,
            25,
            50,
            100,
            250,
            500
        };
        subMenu(cm, "Min Height", r.minHeightProperty(), min);
        subMenu(cm, "Min Width", r.minWidthProperty(), min);

        double[] pref = {
            Region.USE_COMPUTED_SIZE,
            0,
            10,
            25,
            50,
            100,
            250,
            500
        };
        subMenu(cm, "Pref Height", r.prefHeightProperty(), pref);
        subMenu(cm, "Pref Width", r.prefWidthProperty(), pref);

        double[] max = {
            Region.USE_COMPUTED_SIZE,
            Region.USE_PREF_SIZE,
            0,
            10,
            25,
            50,
            100,
            250,
            500,
            1000,
            2500,
            5000,
            Double.POSITIVE_INFINITY
        };
        subMenu(cm, "Max Height", r.maxHeightProperty(), max);
        subMenu(cm, "Max Width", r.maxWidthProperty(), max);
    }

    private static String format(double v) {
        if (v == Region.USE_COMPUTED_SIZE) {
            return "USE_COMPUTED_SIZE";
        } else if (v == Region.USE_PREF_SIZE) {
            return "USE_PREF_SIZE";
        } else if (v == Double.POSITIVE_INFINITY) {
            return "INFINITY";
        }
        return Formats.format2DP(v);
    }

    private static MenuItem item(Menu m, String text, Runnable action) {
        MenuItem mi = new MenuItem(text);
        mi.setMnemonicParsing(false);
        mi.setOnAction((ev) -> action.run());
        m.getItems().add(mi);
        return mi;
    }
}
