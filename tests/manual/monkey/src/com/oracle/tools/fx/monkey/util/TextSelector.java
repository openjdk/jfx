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
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

/**
 * General purpose text selector.
 */
public class TextSelector {
    public static record Pair(String display, String value) { }

    private final ComboBox<Object> field = new ComboBox<>();

    public TextSelector(String id, Consumer<String> client, Object... items) {
        FX.name(field, id);
        field.getItems().setAll(items);
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
        field.getSelectionModel().selectedItemProperty().addListener((p) -> {
            String text = getSelectedText();
            client.accept(text);
        });
    }

    public static TextSelector fromPairs(String id, Consumer<String> client, Object... pairs) {
        ArrayList<Pair> a = new ArrayList<>();
        for (int i = 0; i < pairs.length;) {
            String display = (String)pairs[i++];
            String value = (String)pairs[i++];
            a.add(new Pair(display, value));
        }

        return new TextSelector(id, client, a.toArray());
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
        List<Object> list = field.getItems();
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

    protected String toValue(Object x) {
        if (x == null) {
            return null;
        } else if (x instanceof Pair p) {
            return p.value();
        } else {
            return x.toString();
        }
    }

    public void addPair(String display, String value) {
        field.getItems().add(new Pair(display, value));
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
