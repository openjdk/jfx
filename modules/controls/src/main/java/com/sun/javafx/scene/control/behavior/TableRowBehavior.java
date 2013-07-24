/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.behavior;

import java.util.List;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class TableRowBehavior<T> extends CellBehaviorBase<TableRow<T>> {

    public TableRowBehavior(TableRow<T> control) {
        super(control);
    }

    @Override public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        
        if (e.getButton() != MouseButton.PRIMARY) return;
        
        final TableRow<T> tableRow = getControl();
        final TableView<T> table = tableRow.getTableView();
        if (table == null) return;
        final TableSelectionModel<T> sm = table.getSelectionModel();
        if (sm == null || sm.isCellSelectionEnabled()) return;
        
        final int index = getControl().getIndex();
        final boolean isAlreadySelected = sm.isSelected(index);
        int clickCount = e.getClickCount();
        if (clickCount == 1) {
            // get width of all visible columns (we only care about clicks to the
            // right of the right-most column)
            List<TableColumn<T, ?>> columns = table.getVisibleLeafColumns();
            double width = 0.0;
            for (int i = 0; i < columns.size(); i++) {
                width += columns.get(i).getWidth();
            }
            
            if (e.getX() < width) return;
            
            // In the case of clicking to the right of the rightmost
            // TreeTableCell, we should still support selection, so that
            // is what we are doing here.
            if (isAlreadySelected && e.isShortcutDown()) {
                sm.clearSelection(index);
            } else {
                if (e.isShortcutDown()) {
                    sm.select(tableRow.getIndex());
                } else {
                    sm.clearAndSelect(tableRow.getIndex());
                }
            }
        }
    }
}
