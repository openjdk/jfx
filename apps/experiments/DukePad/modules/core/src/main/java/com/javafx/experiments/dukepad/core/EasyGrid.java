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

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

/**
 */
public class EasyGrid extends GridPane {
    private int nextRow = 0;

    public EasyGrid() {
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHalignment(HPos.RIGHT);
        column1.setPercentWidth(30);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(70);
        getColumnConstraints().addAll(column1,column2);

        RowConstraints lastRow = new RowConstraints();
        lastRow.setVgrow(Priority.ALWAYS);
        getRowConstraints().add(lastRow);

        setHgap(20);
        setVgap(10);
    }

    public int addRow(String labelText, Node content) {
        Label label = new Label(labelText);
        getChildren().add(label);
        GridPane.setConstraints(label, 0, nextRow,1,1, HPos.RIGHT, VPos.TOP, Priority.NEVER,Priority.NEVER, new Insets(5,0,0,0));
        getChildren().add(content);
        GridPane.setConstraints(content, 1, nextRow);
        getRowConstraints().add(0, new RowConstraints());
        nextRow ++;
        return nextRow - 1;
    }

    public int addRow(Node content) {
        getChildren().add(content);
        GridPane.setConstraints(content, 0, nextRow, 2, 1, HPos.LEFT, VPos.TOP, Priority.ALWAYS, Priority.ALWAYS, Insets.EMPTY);
        getRowConstraints().add(0, new RowConstraints());
        nextRow ++;
        return nextRow - 1;
    }

    public void clear() {
        getChildren().clear();
        nextRow = 0;
    }
}
