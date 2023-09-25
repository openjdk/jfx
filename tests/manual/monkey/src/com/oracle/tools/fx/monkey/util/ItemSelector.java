/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

/**
 * General purpose item selector.
 */
public class ItemSelector<T> {
    public static record Pair(String display, Object value) { }

    private final ComboBox<Pair> field = new ComboBox<>();

    public ItemSelector(String id, Consumer<T> client, Object... displayValuePairs) {
        FX.name(field, "PosSelector");
        field.getItems().setAll(toPairs(displayValuePairs));
        field.setConverter(new StringConverter<Pair>() {
            @Override
            public String toString(Pair x) {
                return toDisplay(x);
            }

            @Override
            public Pair fromString(String text) {
                return null;
            }
        });

        field.getSelectionModel().selectFirst();

        field.getSelectionModel().selectedItemProperty().addListener((p) -> {
            Object v = field.getSelectionModel().getSelectedItem();
            T text = toValue(v);
            client.accept(text);
        });
    }

    public T getSelectedItem() {
        Object x = field.getSelectionModel().getSelectedItem();
        T v = toValue(x);
        return v;
    }

    private Pair[] toPairs(Object[] pairs) {
        ArrayList<Pair> a = new ArrayList<>();
        for (int i = 0; i < pairs.length; ) {
            String display = (String)pairs[i++];
            T value = (T)pairs[i++];
            a.add(new Pair(display, value));
        }
        return a.toArray(new Pair[a.size()]);
    }

    public Node node() {
        return field;
    }

    public void select(Object item) {
        int ix = indexOf(item);
        if (ix >= 0) {
            field.getSelectionModel().select(ix);
        }
    }

    private int indexOf(Object item) {
        List<Pair> list = field.getItems();
        int sz = list.size();
        for (int i = 0; i < sz; i++) {
            Object x = list.get(i);
            if (eq(item, x)) {
                return i;
            } else if (x instanceof Pair p) {
                if (eq(item, p.display()) || eq(item, p.value())) {
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

    protected String toDisplay(Object x) {
        if (x == null) {
            return null;
        } else if (x instanceof Pair p) {
            return p.display();
        } else {
            return x.toString();
        }
    }

    protected T toValue(Object x) {
        if (x instanceof Pair p) {
            return (T)p.value();
        } else {
            return null;
        }
    }

    public void add(String display, T value) {
        field.getItems().add(new Pair(display, value));
    }

    public void add(String display, Supplier<T> value) {
        field.getItems().add(new Pair(display, value));
    }
}
