/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.core;

import javafx.geometry.Insets;
import javafx.scene.Node;

/**
 */
public class BaseSettings implements Settings {
    private EasyGrid gridPane;
    private String name;

    public BaseSettings(final String name) {
        this.name = name;
    }

    protected int addRow(String labelText, Node content) {
        return gridPane.addRow(labelText, content);
    }

    protected int addRow(Node content) {
        return gridPane.addRow(content);
    }

    @Override public final String getName() {
        return name;
    }

    public void disposeUI() {
        gridPane = null;
    }

    @Override public final Node createUI() {
        gridPane = new EasyGrid();
        gridPane.setPadding(new Insets(0, 0, 0, 33));
        buildUI();

        return gridPane;
    }

    protected void buildUI() { }

    @Override public Type getType() {
        return Type.APPLICATION;
    }

    @Override public int getSortOrder() {
        return 0;
    }
}
