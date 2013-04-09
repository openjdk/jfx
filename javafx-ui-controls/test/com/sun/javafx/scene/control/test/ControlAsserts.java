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
import com.sun.javafx.scene.control.skin.VirtualScrollBar;
import com.sun.javafx.tk.Toolkit;
import java.util.Arrays;
import java.util.List;

import javafx.geometry.Orientation;
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
    
    public static void assertListContainsItemsInOrder(final List items, final Object... expected) {
        assertEquals(expected.length, items.size());
        for (int i = 0; i < expected.length; i++) {
            Object item = items.get(i);
            assertEquals(expected[i], item);
        }
    }
    
    public static void assertRowsEmpty(final Control control, final int startRow, final int endRow) {
        assertRows(control, startRow, endRow, true);
    }
    
    public static void assertRowsNotEmpty(final Control control, final int startRow, final int endRow) {
        assertRows(control, startRow, endRow, false);
    }
    
    public static void assertCellEmpty(IndexedCell cell) {
        final String text = cell.getText();
//        System.out.println("assertCellEmpty: " + cell.getIndex() + " : " + text);
        assertTrue("Expected null, found '" + text + "'", text == null || text.isEmpty());
    }
    
    public static void assertCellNotEmpty(IndexedCell cell) {
        final String text = cell.getText();
//        System.out.println("assertCellNotEmpty: " + cell.getIndex() + " : " + text);
        assertTrue("Expected a non-null, found '" + text + "'", text != null && ! text.isEmpty());
    }
    
    private static void assertRows(final Control control, final int startRow, final int endRow, final boolean expectEmpty) {
        Callback<IndexedCell<?>, Void> callback = new Callback<IndexedCell<?>, Void>() {
            @Override public Void call(IndexedCell<?> indexedCell) {
                boolean hasChildrenCell = false;
                for (Node n : indexedCell.getChildrenUnmodifiable()) {
                    if (! (n instanceof IndexedCell)) {
                        continue;
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
    
    public static void assertCellTextEquals(final Control control, final int index, final String... expected) {
        if (expected == null || expected.length == 0) return;
        
        Callback<IndexedCell<?>, Void> callback = new Callback<IndexedCell<?>, Void>() {
            @Override public Void call(IndexedCell<?> indexedCell) {
                if (indexedCell.getIndex() != index) return null;
        
                if (expected.length == 1) {
                    assertEquals(expected[0], indexedCell.getText());
                } else {
                    int jump = 0;
                    for (int i = 0; i < expected.length; i++) {
                        Node childNode = indexedCell.getChildrenUnmodifiable().get(i + jump);
                        String text = null;
                        if (! (childNode instanceof IndexedCell)) {
                            jump++;
                            continue;
                        }
                        
                        text = ((IndexedCell) childNode).getText();
                        assertEquals(expected[i], text);
                    }
                }
                return null;
            }
        };
        
        assertCallback(control, index, index + 1, callback);
    }
    
    public static void assertTableCellTextEquals(final Control control, final int row, final int column, final String expected) {
        Callback<IndexedCell<?>, Void> callback = new Callback<IndexedCell<?>, Void>() {
            @Override public Void call(IndexedCell<?> indexedCell) {
                if (indexedCell.getIndex() != row) return null;
                
                IndexedCell cell = (IndexedCell) indexedCell.getChildrenUnmodifiable().get(column);
                assertEquals(expected, cell.getText());
                return null;
            }
        };
        
        assertCallback(control, row, row + 1, callback);
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
    
    public static IndexedCell getCell(final Control control, final int index) {
        return getVirtualFlow(control).getCell(index);
    }
    
    public static void assertCallback(final Control control, final int startRow, final int endRow, final Callback<IndexedCell<?>, Void> callback) {
        VirtualFlow<?> flow = getVirtualFlow(control);
        
//        Region clippedContainer = (Region) flow.getChildrenUnmodifiable().get(0);
//        Group sheet = (Group) clippedContainer.getChildrenUnmodifiable().get(0);
        
//        final int sheetSize = sheet.getChildren().size();
        final int sheetSize = flow.getCellCount();
        final int end = endRow == -1 ? sheetSize : Math.min(endRow, sheetSize);
        for (int row = startRow; row < end; row++) {
            // old approach:
            // callback.call((IndexedCell<?>)sheet.getChildren().get(row));
            
            // new approach:
            IndexedCell cell = flow.getCell(row);
//            System.out.println("cell index: " + cell.getIndex());
            callback.call(cell);
        }
    }
    
    public static void assertCellCount(final Control control, final int expected) {
        assertEquals(getVirtualFlow(control).getCellCount(), expected);
    }
    
    public static VirtualFlow<?> getVirtualFlow(final Control control) {
        Group group = new Group();
        Scene scene = new Scene(group);

        Stage stage = new Stage();
        stage.setScene(scene);

        group.getChildren().setAll(control);
        stage.show();

        Toolkit.getToolkit().firePulse();

        VirtualFlow<?> flow = (VirtualFlow<?>)control.lookup("#virtual-flow");
        return flow;
    }
    
    public static VirtualScrollBar getVirtualFlowVerticalScrollbar(final Control control) {
        VirtualFlow<?> flow = getVirtualFlow(control);
        VirtualScrollBar scrollBar = null;
        for (Node child : flow.getChildrenUnmodifiable()) {
            if (child instanceof VirtualScrollBar) {
                if (((VirtualScrollBar)child).getOrientation() == Orientation.VERTICAL) {
                    scrollBar = (VirtualScrollBar) child;
                }
            }
        }
        
//        Toolkit.getToolkit().firePulse();
        return scrollBar;
    }
}
