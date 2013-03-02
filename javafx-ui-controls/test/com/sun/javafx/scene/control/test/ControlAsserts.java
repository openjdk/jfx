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
package com.sun.javafx.scene.control.test;

import com.sun.javafx.scene.control.skin.LabeledText;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import com.sun.javafx.tk.Toolkit;
import java.util.List;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Callback;
import static org.junit.Assert.*;

public class ControlAsserts {
    
    public static void assertRowsEmpty(final Control control, final int startRow, final int endRow) {
        assertRows(control, startRow, endRow, true);
    }
    
    public static void assertRowsNotEmpty(final Control control, final int startRow, final int endRow) {
        assertRows(control, startRow, endRow, false);
    }
    
    public static void assertCellEmpty(IndexedCell cell) {
        assertNull("Expected null, found '" + cell.getText() + "'", cell.getText());
    }
    
    public static void assertCellNotEmpty(IndexedCell cell) {
        assertNotNull("Expected a non-null, found '" + cell.getText() + "'", cell.getText());
        assertFalse(cell.getText().isEmpty());
    }
    
    private static void assertRows(final Control control, final int startRow, final int endRow, final boolean expectEmpty) {
        Callback<IndexedCell<?>, Void> callback = new Callback<IndexedCell<?>, Void>() {
            @Override public Void call(IndexedCell<?> indexedCell) {
                boolean hasChildrenCell = false;
                for (Node n : indexedCell.getChildrenUnmodifiable()) {
                    if (! (n instanceof IndexedCell)) {
                        break;
                    }
                    hasChildrenCell = true;
                    IndexedCell<?> childCell = (IndexedCell<?>)n;

                    if (expectEmpty) {
                        assertCellEmpty(childCell);
                    } else {
                        assertCellNotEmpty(childCell);
                    }
                }

                if (! hasChildrenCell) {
                    if (expectEmpty) {
                        assertCellEmpty(indexedCell);
                    } else {
                        assertCellNotEmpty(indexedCell);
                    }
                }
                return null;
            }
        };
        
        assertCallback(control, startRow, endRow, callback);
    }
    
    private static void assertCellTextEquals(final Control control, final int startRow, final int endRow, final String expected) {
        Callback<IndexedCell<?>, Void> callback = new Callback<IndexedCell<?>, Void>() {
            @Override public Void call(IndexedCell<?> indexedCell) {
                boolean hasChildrenCell = false;
                for (Node n : indexedCell.getChildrenUnmodifiable()) {
                    if (! (n instanceof IndexedCell)) {
                        break;
                    }
                    hasChildrenCell = true;
                    IndexedCell<?> childCell = (IndexedCell<?>)n;

                    if (expectEmpty) {
                        assertCellEmpty(childCell);
                    } else {
                        assertCellNotEmpty(childCell);
                    }
                }

                if (! hasChildrenCell) {
                    if (expectEmpty) {
                        assertCellEmpty(indexedCell);
                    } else {
                        assertCellNotEmpty(indexedCell);
                    }
                }
                return null;
            }
        };
        
        assertCallback(control, startRow, endRow, callback);
    }
    
    // used by TreeView / TreeTableView to ensure the correct indentation
    // (although note that it has only been developed so far for TreeView)
    public static void assertLayoutX(final Control control, final int startRow, final int endRow, final double expectedLayoutX) {
        Callback<IndexedCell<?>, Void> callback = new Callback<IndexedCell<?>, Void>() {
            @Override public Void call(IndexedCell<?> indexedCell) {
                List<Node> childrenOfCell = indexedCell.getChildrenUnmodifiable();
                LabeledText labeledText = null;
                for (int j = 0; j < childrenOfCell.size(); j++) {
                    Node child = childrenOfCell.get(j);
                    if (child instanceof LabeledText) {
                        labeledText = (LabeledText) child;
                    }
                }

                String error = "Element in row " + indexedCell.getIndex() + " has incorrect indentation. "
                        + "Expected " + expectedLayoutX + ", but found " + labeledText.getLayoutX();
                assertEquals(error, expectedLayoutX, labeledText.getLayoutX(), 0.0);
                return null;
            }
        };
        
        assertCallback(control, startRow, endRow, callback);
    }
    
    public static void assertCallback(final Control control, final int startRow, final int endRow, final Callback<IndexedCell<?>, Void> callback) {
        Group group = new Group();
        Scene scene = new Scene(group);
        
        Stage stage = new Stage();
        stage.setScene(scene);
        
        group.getChildren().setAll(control);
        stage.show();
        
        Toolkit.getToolkit().firePulse();
        
        VirtualFlow<?> flow = (VirtualFlow<?>)control.lookup("#virtual-flow");
        
        Region clippedContainer = (Region) flow.getChildrenUnmodifiable().get(0);
        Group sheet = (Group) clippedContainer.getChildrenUnmodifiable().get(0);
        
        final int sheetSize = sheet.getChildren().size();
        final int end = endRow == -1 ? sheetSize : Math.min(endRow, sheetSize);
        for (int row = startRow; row < end; row++) {
            callback.call((IndexedCell<?>)sheet.getChildren().get(row));
        }
    }
}
