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

import java.util.List;
import java.util.Objects;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.Formats;
import com.oracle.tools.fx.monkey.util.NamedValue;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Double Option Bound to a Property.
 */
public class DoubleOption extends ComboBox<Object> {
    private final SimpleObjectProperty<Number> property = new SimpleObjectProperty<>();

    public DoubleOption(String name, Property<Number> p) {
        FX.name(this, name);

        property.bindBidirectional(p);

        setEditable(true);
        setConverter(new StringConverter<Object>() {
            @Override
            public String toString(Object x) {
                if (x instanceof NamedValue n) {
                    return n.getDisplay();
                }
                return String.valueOf(getValue(x));
            }

            @Override
            public Object fromString(String s) {
                return parseValue(s);
            }
        });
        setOnAction((ev) -> {
            Object x = getValue();
            Number n = getValue(x);
            if (n != null) {
                property.set(n);
            }
        });
        property.addListener((s, old, cur) -> {
            select(cur, false);
        });
    }

    private Number parseValue(String s) {
        if (Utils.isBlank(s)) {
            return null;
        }
        NamedValue<Number> n = find(s);
        if (n == null) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return null;
        }
        return n.getValue();
    }

    private NamedValue<Number> find(String s) {
        for (Object x: getItems()) {
            if (x instanceof NamedValue n) {
                if (s.equals(n.getDisplay())) {
                    return n;
                }
            }
        }
        return null;
    }

    /**
     * Selects the property value, adding it to the list of items under "<INITIAL>" name.
     */
    public void selectInitialValue() {
        Number value = property.get();
        select(value, true);
    }

    private void select(Number value, boolean initial) {
        List<Object> items = getItems();
        int sz = items.size();
        for (int i = 0; i < sz; i++) {
            Object x = items.get(i);
            Number v = getValue(x);
            if (Objects.equals(value, v)) {
                select(i);
                return;
            }
        }

        Object v;
        if (initial) {
            String text = "<INITIAL " + value + ">";
            v = new NamedValue<Number>(text, value);
        } else {
            v = value;
        }
        items.add(v);
        select(sz);
    }

    private Number getValue(Object x) {
        if (x == null) {
            return null;
        } else if (x instanceof Number n) {
            return n;
        } else if (x instanceof NamedValue n) {
            return getValue(n.getValue());
        }
        throw new Error("?" + x);
    }

    /**
     * Selects the given index.  Does nothing if the index is outside of the valid range.
     * @param ix
     */
    public void select(int ix) {
        if ((ix >= 0) && (ix < getItems().size())) {
            getSelectionModel().select(ix);
        }
    }

    public void addChoice(String name, Number item) {
        getItems().add(new NamedValue<>(name, item));
    }

    public void addChoice(Number item) {
        getItems().add(item);
    }

    public static DoubleOption of(String name, Property<Number> p, double... values) {
        DoubleOption d = new DoubleOption(name, p);
        for (double v: values) {
            String text = Formats.formatDouble(v);
            d.addChoice(text, v);
        }
        d.selectInitialValue();
        return d;
    }
}
