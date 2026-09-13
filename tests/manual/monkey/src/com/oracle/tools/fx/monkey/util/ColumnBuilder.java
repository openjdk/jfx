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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.control.TableColumnBase;

/**
 * Column Builder.
 */
public class ColumnBuilder<T extends TableColumnBase<?, ?>> {
    private final Supplier<TableColumnBase> generator;
    private final ArrayList<T> columns = new ArrayList<>();
    private T last;
    private int id;

    public ColumnBuilder(Supplier<TableColumnBase> generator) {
        this.generator = generator;
    }

    public ColumnBuilder<T> col(String name) {
        last = (T)generator.get();
        last.setText(name);
        columns.add(last);
        return this;
    }

    public ColumnBuilder<T> min(double width) {
        last.setMinWidth(width);
        return this;
    }

    public ColumnBuilder<T> max(double width) {
        last.setMaxWidth(width);
        return this;
    }

    public ColumnBuilder<T> pref(double width) {
        last.setPrefWidth(width);
        return this;
    }

    public ColumnBuilder<T> fixed(double width) {
        last.setMinWidth(width);
        last.setMaxWidth(width);
        return this;
    }

    public ColumnBuilder<T> combine(int index, int count) {
        var tc = generator.get();
        tc.setText("N" + (++id));

        for (int i = 0; i < count; i++) {
            T c = columns.remove(index);
            tc.getColumns().add(c);
        }
        columns.add(index, (T)tc);
        return this;
    }

    public List<T> asList() {
        return columns;
    }
}
