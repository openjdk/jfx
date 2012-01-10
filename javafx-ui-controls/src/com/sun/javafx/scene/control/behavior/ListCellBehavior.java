/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.scene.control.Logging;
import java.util.WeakHashMap;

import javafx.scene.control.FocusModel;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;

/**
 */
public class ListCellBehavior extends CellBehaviorBase<ListCell> {
    // global map used to store the focus index for a list view when it is first
    // shift-clicked. This allows for proper keyboard interactions, in particular
    // resolving RT-11446
    private static final WeakHashMap<ListView, Integer> map = new WeakHashMap<ListView, Integer>();
    
    static int getAnchor(ListView list) {
        FocusModel fm = list.getFocusModel();
        if (fm == null) return -1;
        
        return hasAnchor(list) ? map.get(list) : fm.getFocusedIndex();
    }
    
    static void setAnchor(ListView list, int anchor) {
        if (list != null && anchor < 0) {
            map.remove(list);
        } else {
            map.put(list, anchor);
        }
    }
    
    static boolean hasAnchor(ListView list) {
        return map.containsKey(list);
    }
    
    // For RT-17456: have selection occur as fast as possible with mouse input.
    // The idea is (consistently with some native applications we've tested) to 
    // do the action as soon as you can. It takes a bit more coding but provides
    // the best feel:
    //  - when you click on a not-selected item, you can select immediately on press
    //  - when you click on a selected item, you need to wait whether DragDetected or Release comes first 
    private boolean selected = false;
    private boolean latePress = false;

    public ListCellBehavior(ListCell control) {
        super(control);
    }
    
    @Override public void mousePressed(MouseEvent event) {
        if (selected) {
            latePress = true;
            return;
        }
        
        doSelect(event);
    }
    
    @Override public void mouseReleased(MouseEvent event) {
        if (latePress) {
            latePress = false;
            doSelect(event);
        }
    }
    
    @Override public void mouseDragged(MouseEvent event) {
        latePress = false;
    }
    
    private void doSelect(MouseEvent e) {
        // Note that list.select will reset selection
        // for out of bounds indexes. So, need to check
        ListCell listCell = getControl();
        ListView listView = getControl().getListView();
        if (listView == null) return;

        // If the mouse event is not contained within this ListCell, then
        // we don't want to react to it.
        if (listCell.isEmpty() || ! listCell.contains(e.getX(), e.getY())) {
            final PlatformLogger logger = Logging.getControlsLogger();
            if (listCell.isEmpty() && logger.isLoggable(PlatformLogger.WARNING)) {
//                logger.warning("ListCell is empty, so mouse pressed event is "
//                        + "ignored. If you've created a custom cell and overridden "
//                        + "updateItem, be sure to call super.updateItem(item, empty)");
            }
            return;
        }

        int rowCount = listView.getItems() == null ? 0 : listView.getItems().size();
        if (listCell.getIndex() >= rowCount) return;

        int index = listCell.getIndex();
        boolean selected = listCell.isSelected();

        MultipleSelectionModel sm = listView.getSelectionModel();
        if (sm == null) return;

        FocusModel fm = listView.getFocusModel();
        if (fm == null) return;
        
        // if shift is down, and we don't already have the initial focus index
        // recorded, we record the focus index now so that subsequent shift+clicks
        // result in the correct selection occuring (whilst the focus index moves
        // about).
        if (e.isShiftDown()) {
            if (! map.containsKey(listView)) {
                setAnchor(listView, fm.getFocusedIndex());
            }
        } else {
            map.remove(listView);
        }
        
        if (sm.getSelectionMode() == SelectionMode.SINGLE) {
            simpleSelect(e);
        } else {
            if (e.isControlDown() || e.isMetaDown()) {
                if (selected) {
                    // we remove this row from the current selection
                    sm.clearSelection(index);
                } else {
                    // We add this row to the current selection
                    sm.select(index);
                }
            } else if (e.isShiftDown()) {
                // we add all rows between the current focus and
                // this row (inclusive) to the current selection.
                int focusIndex = getAnchor(listView);

                // and then determine all row and columns which must be selected
                int minRow = Math.min(focusIndex, index);
                int maxRow = Math.max(focusIndex, index);

                // and then perform the selection
                sm.clearSelection();
                sm.selectRange(minRow, maxRow+1);

                // return selection back to the focus owner
                fm.focus(index);
            } else {
                simpleSelect(e);
            }
        }
    }

    private void simpleSelect(MouseEvent e) {
        ListView lv = getControl().getListView();
        int index = getControl().getIndex();
        MultipleSelectionModel sm = lv.getSelectionModel();
        boolean isAlreadySelected = sm.isSelected(index);

        lv.getSelectionModel().clearAndSelect(index);

        // handle editing, which only occurs with the primary mouse button
        if (e.isPrimaryButtonDown()) {
            if (e.getClickCount() == 1 && isAlreadySelected) {
                lv.edit(index);
            } else if (e.getClickCount() == 1) {
                // cancel editing
                lv.edit(-1);
            } else if (e.getClickCount() == 2 && getControl().isEditable()) {
                lv.edit(index);
            }
        }
    }
}
