/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import com.oracle.tools.fx.monkey.util.EnterTextDialog;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.NamedValue;

/**
 * Text Choice Option Bound to a Property.
 */
public class TextChoiceOption extends BorderPane {
    private final SimpleStringProperty property = new SimpleStringProperty();
    private final ComboBox<Object> field;

    public TextChoiceOption(String name, boolean allowEditButton, StringProperty p) {
        FX.name(this, name);
        if (p != null) {
            property.bindBidirectional(p);
        }

        field = new ComboBox<>();
        field.setMaxWidth(Double.MAX_VALUE);
        field.setConverter(new StringConverter<Object>() {
            @Override
            public String toString(Object x) {
                return toDisplay(x);
            }

            @Override
            public Object fromString(String text) {
                return text;
            }
        });
        field.getSelectionModel().selectedItemProperty().addListener((pr) -> {
            String text = getSelectedText();
            property.set(text);
        });

        if (allowEditButton) {
            Button editButton = FX.button("Edit", EnterTextDialog.getRunnable(this, property));
            setRight(editButton);
            setMargin(editButton, new Insets(0, 0, 0, 2));
        }

        setCenter(field);
        setMaxWidth(Double.MAX_VALUE);
    }

    public SimpleStringProperty property() {
        return property;
    }

    public void clearChoices() {
        field.getItems().clear();
    }

    public void addChoice(String value) {
        field.getItems().add(new NamedValue<>(value, value));
    }

    public void addChoice(String name, String item) {
        field.getItems().add(new NamedValue<>(name, item));
    }

    public void addChoiceSupplier(String name, Supplier<String> gen) {
        field.getItems().add(new NamedValue<>(name, null) {
            @Override
            public String getValue() {
                return gen.get();
            }
        });
    }

    public void select(String item) {
        int ix = indexOf(item);
        if (ix >= 0) {
            field.getSelectionModel().select(ix);
        }
    }

    private int indexOf(String item) {
        List<Object> list = field.getItems();
        int sz = list.size();
        for (int i = 0; i < sz; i++) {
            Object x = list.get(i);
            if (eq(item, x)) {
                return i;
            } else if (x instanceof NamedValue p) {
                if (eq(item, p.getDisplay()) || eq(item, p.getValue())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean eq(Object a, Object b) {
        if (a == null) {
            return (b == null);
        } else {
            return a.equals(b);
        }
    }

    public void selectFirst() {
        field.getSelectionModel().selectFirst();
    }

    private String toDisplay(Object x) {
        if (x == null) {
            return null;
        } else if (x instanceof NamedValue p) {
            return p.getDisplay();
        } else {
            return x.toString();
        }
    }

    private String toValue(Object x) {
        if (x == null) {
            return null;
        } else if (x instanceof NamedValue p) {
            return (String)p.getValue();
        } else {
            return x.toString();
        }
    }

    public String getSelectedText() {
        Object v = field.getSelectionModel().getSelectedItem();
        return toValue(v);
    }

    public void removeChoice(String name) {
        int ix = 0;
        for (Object x: field.getItems()) {
            String s = toDisplay(x);
            if (Objects.equals(name, s)) {
                field.getItems().remove(ix);
                return;
            }
            ix++;
        }
    }
}
