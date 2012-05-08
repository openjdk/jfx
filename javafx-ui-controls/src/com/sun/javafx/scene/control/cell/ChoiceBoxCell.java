/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.cell;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.Cell;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;

// Package protected - not intended for external use
class ChoiceBoxCell {
    
    static <T> void updateItem(
            final Cell<T> cell, ChoiceBox<T> choiceBox, 
            final StringConverter<T> converter) {
        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);
        } else {
            if (cell.isEditing()) {
                if (choiceBox != null) {
                    choiceBox.getSelectionModel().select(cell.getItem());
                }
                cell.setText(null);
                cell.setGraphic(choiceBox);
            } else {
                cell.setText(getItemText(cell, converter));
                cell.setGraphic(null);
            }
        }
    };
    
    static <T> ChoiceBox<T> createChoiceBox(
            final Cell<T> cell, 
            final ObservableList<T> items) {
        ChoiceBox<T> choiceBox = new ChoiceBox<T>(items);
        choiceBox.setMaxWidth(Double.MAX_VALUE);
        choiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> ov, T oldValue, T newValue) {
                if (cell.isEditing()) {
                    cell.commitEdit(newValue);
                }
            }
        });
        return choiceBox;
    }
    
    private static <T> String getItemText(Cell<T> cell, StringConverter<T> converter) {
        return converter == null ?
            cell.getItem() == null ? "" : cell.getItem().toString() :
            converter.toString(cell.getItem());
    }
}
