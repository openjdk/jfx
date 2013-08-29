/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.control.FocusModel;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.sun.javafx.scene.control.Logging;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.Level;

/**
 */
public class ListCellBehavior<T> extends CellBehaviorBase<ListCell<T>> {

    /***************************************************************************
     *                                                                         *
     * Private static implementation                                           *
     *                                                                         *
     **************************************************************************/

    private static final String ANCHOR_PROPERTY_KEY = "list.anchor";

    static int getAnchor(ListView<?> list) {
        FocusModel<?> fm = list.getFocusModel();
        if (fm == null) return -1;

        return hasAnchor(list) ?
                (int)list.getProperties().get(ANCHOR_PROPERTY_KEY) :
                fm.getFocusedIndex();
    }

    static void setAnchor(ListView<?> list, int anchor) {
        if (list != null && anchor < 0) {
            removeAnchor(list);
        } else {
            list.getProperties().put(ANCHOR_PROPERTY_KEY, anchor);
        }
    }

    static boolean hasAnchor(ListView<?> list) {
        return list.getProperties().get(ANCHOR_PROPERTY_KEY) != null;
    }

    static void removeAnchor(ListView<?> list) {
        list.getProperties().remove(ANCHOR_PROPERTY_KEY);
    }



    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    // For RT-17456: have selection occur as fast as possible with mouse input.
    // The idea is (consistently with some native applications we've tested) to
    // do the action as soon as you can. It takes a bit more coding but provides
    // the best feel:
    //  - when you click on a not-selected item, you can select immediately on press
    //  - when you click on a selected item, you need to wait whether DragDetected or Release comes first
    //
    // To support touch devices, we have to slightly modify this behavior, such
    // that selection only happens on mouse release, if only minimal dragging
    // has occurred.
    private boolean latePress = false;
    private boolean wasSelected = false;


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ListCellBehavior(ListCell<T> control) {
        super(control, Collections.EMPTY_LIST);
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    @Override public void mousePressed(MouseEvent event) {
        boolean selectedBefore = getControl().isSelected();

        if (getControl().isSelected()) {
            latePress = true;
            return;
        }

        doSelect(event);

        if (IS_TOUCH_SUPPORTED && selectedBefore) {
            wasSelected = getControl().isSelected();
        }
    }

    @Override public void mouseReleased(MouseEvent event) {
        if (latePress) {
            latePress = false;
            doSelect(event);
        }

        wasSelected = false;
    }

    @Override public void mouseDragged(MouseEvent event) {
        latePress = false;

        // the mouse has now been dragged on a touch device, we should
        // remove the selection if we just added it in the last mouse press
        // event
        if (IS_TOUCH_SUPPORTED && ! wasSelected && getControl().isSelected()) {
            getControl().getListView().getSelectionModel().clearSelection(getControl().getIndex());
        }
    }



    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void doSelect(MouseEvent e) {
        // Note that list.select will reset selection
        // for out of bounds indexes. So, need to check
        ListCell<T> listCell = getControl();
        ListView<T> listView = getControl().getListView();
        if (listView == null) return;

        // If the mouse event is not contained within this ListCell, then
        // we don't want to react to it.
        if (listCell.isEmpty() || ! listCell.contains(e.getX(), e.getY())) {
            final PlatformLogger logger = Logging.getControlsLogger();
            if (listCell.isEmpty() && logger.isLoggable(Level.WARNING)) {
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

        MultipleSelectionModel<T> sm = listView.getSelectionModel();
        if (sm == null) return;

        FocusModel<T> fm = listView.getFocusModel();
        if (fm == null) return;

        // if shift is down, and we don't already have the initial focus index
        // recorded, we record the focus index now so that subsequent shift+clicks
        // result in the correct selection occuring (whilst the focus index moves
        // about).
        if (e.isShiftDown()) {
            if (! hasAnchor(listView)) {
                setAnchor(listView, fm.getFocusedIndex());
            }
        } else {
            removeAnchor(listView);
        }

        MouseButton button = e.getButton();
        if (button == MouseButton.PRIMARY || (button == MouseButton.SECONDARY && !selected)) {
            if (sm.getSelectionMode() == SelectionMode.SINGLE) {
                simpleSelect(e);
            } else {
                if (e.isControlDown() || e.isMetaDown()) {
                    if (selected) {
                        // we remove this row from the current selection
                        sm.clearSelection(index);
                        fm.focus(index);
                    } else {
                        // We add this row to the current selection
                        sm.select(index);
                    }
                } else if (e.isShiftDown()) {
                    // we add all rows between the current focus and
                    // this row (inclusive) to the current selection.
                    final int focusIndex = getAnchor(listView);
                    final boolean asc = focusIndex < index;

                    // and then determine all row and columns which must be selected
                    int minRow = Math.min(focusIndex, index);
                    int maxRow = Math.max(focusIndex, index);

                    // and then perform the selection.
                    // We do this by deselecting the elements that are not in
                    // range, and then selecting all elements that are in range.
                    // To prevent RT-32119, we make a copy of the selected indices
                    // list first, so that we are not iterating and modifying it
                    // concurrently.
                    List<Integer> selectedIndices = new ArrayList<>(sm.getSelectedIndices());
                    for (int i = 0, max = selectedIndices.size(); i < max; i++) {
                        int selectedIndex = selectedIndices.get(i);
                        if (selectedIndex < minRow || selectedIndex > maxRow) {
                            sm.clearSelection(selectedIndex);
                        }
                    }

                    // RT-21444: We need to put the range in in the correct
                    // order or else the last selected row will not be the
                    // last item in the selectedItems list of the selection
                    // model,
                    if (asc) {
                        sm.selectRange(minRow, maxRow + 1);
                    } else {
                        sm.selectRange(maxRow, minRow - 1);
                    }

                    // return selection back to the focus owner
                    fm.focus(index);
                } else {
                    simpleSelect(e);
                }
            }
        }
    }

    private void simpleSelect(MouseEvent e) {
        ListView<T> lv = getControl().getListView();
        int index = getControl().getIndex();
        MultipleSelectionModel<T> sm = lv.getSelectionModel();
        boolean isAlreadySelected = sm.isSelected(index);

        lv.getSelectionModel().clearAndSelect(index);

        // handle editing, which only occurs with the primary mouse button
        if (e.getButton() == MouseButton.PRIMARY) {
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
