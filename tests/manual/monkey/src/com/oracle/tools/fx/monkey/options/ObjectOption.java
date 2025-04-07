/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.Supplier;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBox;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.NamedValue;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Object Selector Bound to a Property.
 */
public class ObjectOption<T> extends ComboBox<NamedValue<T>> {
    private final SimpleObjectProperty<T> property = new SimpleObjectProperty<>();

    public ObjectOption(String name, Property<T> p) {
        FX.name(this, name);
        property.bindBidirectional(p);

        // TODO add the current value to choices and select it

        getSelectionModel().selectedItemProperty().addListener((s, pr, c) -> {
            T v = c.getValue();
            if (!Utils.eq(v, property.getValue())) {
                property.set(v);
            }
        });

        property.addListener((s,prev,v) -> {
            selectValue(v);
        });
    }

    public void clearChoices() {
        getItems().clear();
    }

    public void addChoice(String name, T item) {
        getItems().add(new NamedValue<>(name, item));
    }

    public void addChoiceSupplier(String name, Supplier<T> gen) {
        getItems().add(new NamedValue<>(name, null) {
            @Override
            public T getValue() {
                return gen.get();
            }
        });
    }

    /**
     * Selects the property value, adding it to the list of items under "<INITIAL>" name.
     */
    public void selectInitialValue() {
        T value = property.get();
        List<NamedValue<T>> items = getItems();
        int sz = items.size();
        for (int i = 0; i < sz; i++) {
            NamedValue<T> item = items.get(i);
            if (Objects.equals(value, item.getValue())) {
                select(i);
                return;
            }
        }

        String text = "<INITIAL " + value + ">";
        items.add(new NamedValue<T>(text, value));
        select(sz);
    }

    /**
     * Selects the specified value.
     */
    public void selectValue(T value) {
        List<NamedValue<T>> items = getItems();
        int sz = items.size();
        for (int i = 0; i < sz; i++) {
            NamedValue<T> item = items.get(i);
            if (Objects.equals(value, item.getValue())) {
                select(i);
                return;
            }
        }
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

    public void selectFirst() {
        select(0);
    }
}
