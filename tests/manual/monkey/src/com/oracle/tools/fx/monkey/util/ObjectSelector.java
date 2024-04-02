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

import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.control.ComboBox;

/**
 * Unidirectional Object Selector.
 */
public class ObjectSelector<T> extends ComboBox<NamedValue<T>> {
    public ObjectSelector(String name, Consumer<T> client) {
        FX.name(this, name);

        getSelectionModel().selectedItemProperty().addListener((s, pr, c) -> {
            T v = c.getValue();
            try {
                client.accept(v);
            } catch (Throwable e) {
                e.printStackTrace();
            }
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

    public void select(int ix) {
        if ((ix >= 0) && (ix < getItems().size())) {
            getSelectionModel().select(ix);
        }
    }

    public void selectFirst() {
        select(0);
    }

    public T getSelectedValue() {
        NamedValue<T> v = getSelectionModel().getSelectedItem();
        return v == null ? null : v.getValue();
    }
}
