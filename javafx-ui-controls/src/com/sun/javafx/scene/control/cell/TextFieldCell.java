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

import javafx.event.EventHandler;
import javafx.scene.control.Cell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

// Package protected - not intended for external use
class TextFieldCell {

    static <T> void updateItem(Cell<T> cell, TextField textField) {
        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);
        } else {
            if (cell.isEditing()) {
                if (textField != null) {
                    textField.setText(getItemText(cell));
                }
                cell.setText(null);
                cell.setGraphic(textField);
            } else {
                cell.setText(getItemText(cell));
                cell.setGraphic(null);
            }
        }
    }
    
    static <T> void startEdit(
            final Cell<T> cell, 
            TextField textField, 
            final StringConverter<T> converter) {
        if (textField == null) {
            textField = createTextField(cell, converter);
        }
        textField.setText(getItemText(cell));
        
        cell.setText(null);
        cell.setGraphic(textField);
        
        textField.selectAll();
    }
    
    static <T> void cancelEdit(Cell<T> cell) {
        cell.setText(getItemText(cell));
        cell.setGraphic(null);
    }
    
    private static <T> TextField createTextField(final Cell<T> cell, final StringConverter<T> converter) {
        final TextField textField = new TextField(getItemText(cell));
        textField.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent t) {
                if (t.getCode() == KeyCode.ENTER) {
                    cell.commitEdit(converter.fromString(textField.getText()));
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cell.cancelEdit();
                }
            }
        });
        return textField;
    }
    
    private static <T> String getItemText(Cell<T> cell) {
        return cell.getItem() == null ? "" : cell.getItem().toString();
    }
}
