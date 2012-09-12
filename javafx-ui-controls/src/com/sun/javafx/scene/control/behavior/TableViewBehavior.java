/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyCode.BACK_SLASH;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.END;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.F2;
import static javafx.scene.input.KeyCode.HOME;
import static javafx.scene.input.KeyCode.KP_DOWN;
import static javafx.scene.input.KeyCode.KP_LEFT;
import static javafx.scene.input.KeyCode.KP_RIGHT;
import static javafx.scene.input.KeyCode.KP_UP;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyCode.UP;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import com.sun.javafx.PlatformUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.control.*;
import javafx.util.Callback;

public class TableViewBehavior<T> extends BehaviorBase<TableView<T>> {
    /**************************************************************************
     *                          Setup KeyBindings                             *
     *************************************************************************/
    protected static final List<KeyBinding> TABLE_VIEW_BINDINGS = new ArrayList<KeyBinding>();

    static {
        TABLE_VIEW_BINDINGS.add(new KeyBinding(TAB, "TraverseNext"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(TAB, "TraversePrevious").shift());

        TABLE_VIEW_BINDINGS.add(new KeyBinding(HOME, "SelectFirstRow"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(END, "SelectLastRow"));
        
        TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "ScrollUp"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "ScrollDown"));

        TABLE_VIEW_BINDINGS.add(new KeyBinding(LEFT, "SelectLeftCell"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_LEFT, "SelectLeftCell"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(RIGHT, "SelectRightCell"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_RIGHT, "SelectRightCell"));

        TABLE_VIEW_BINDINGS.add(new KeyBinding(UP, "SelectPreviousRow"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_UP, "SelectPreviousRow"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "SelectNextRow"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_DOWN, "SelectNextRow"));

        TABLE_VIEW_BINDINGS.add(new KeyBinding(LEFT, "TraverseLeft"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_LEFT, "TraverseLeft"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(RIGHT, "SelectNextRow"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_RIGHT, "SelectNextRow"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(UP, "TraverseUp"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_UP, "TraverseUp"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "TraverseDown"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_DOWN, "TraverseDown"));

        TABLE_VIEW_BINDINGS.add(new KeyBinding(HOME, "SelectAllToFirstRow").shift());
        TABLE_VIEW_BINDINGS.add(new KeyBinding(END, "SelectAllToLastRow").shift());
        TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "SelectAllPageUp").shift());
        TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "SelectAllPageDown").shift());

        TABLE_VIEW_BINDINGS.add(new KeyBinding(UP, "AlsoSelectPrevious").shift());
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_UP, "AlsoSelectPrevious").shift());
        TABLE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "AlsoSelectNext").shift());
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_DOWN, "AlsoSelectNext").shift());
        
        TABLE_VIEW_BINDINGS.add(new KeyBinding(SPACE, "SelectAllToFocus").shift());

//        TABLE_VIEW_BINDINGS.add(new KeyBinding(UP, "AlsoSelectPreviousCell").shift());
//        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_UP, "AlsoSelectPreviousCell").shift());
//        TABLE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "AlsoSelectNextCell").shift());
//        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_DOWN, "AlsoSelectNextCell").shift());
        TABLE_VIEW_BINDINGS.add(new KeyBinding(LEFT, "AlsoSelectLeftCell").shift());
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_LEFT, "AlsoSelectLeftCell").shift());
        TABLE_VIEW_BINDINGS.add(new KeyBinding(RIGHT, "AlsoSelectRightCell").shift());
        TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_RIGHT, "AlsoSelectRightCell").shift());

        if (PlatformUtil.isMac()) {
            TABLE_VIEW_BINDINGS.add(new KeyBinding(UP, "FocusPreviousRow").meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "FocusNextRow").meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(RIGHT, "FocusRightCell").meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_RIGHT, "FocusRightCell").meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(LEFT, "FocusLeftCell").meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_LEFT, "FocusLeftCell").meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(A, "SelectAll").meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(HOME, "FocusFirstRow").meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(END, "FocusLastRow").meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(SPACE, "toggleFocusOwnerSelection").ctrl().meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "FocusPageUp").meta());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "FocusPageDown").meta());
            
            TABLE_VIEW_BINDINGS.add(new KeyBinding(UP, "DiscontinuousSelectPreviousRow").meta().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "DiscontinuousSelectNextRow").meta().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(LEFT, "DiscontinuousSelectPreviousColumn").meta().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(RIGHT, "DiscontinuousSelectNextColumn").meta().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "DiscontinuousSelectPageUp").meta().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "DiscontinuousSelectPageDown").meta().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(HOME, "DiscontinuousSelectAllToFirstRow").meta().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(END, "DiscontinuousSelectAllToLastRow").meta().shift());
        } else {
            TABLE_VIEW_BINDINGS.add(new KeyBinding(UP, "FocusPreviousRow").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "FocusNextRow").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(RIGHT, "FocusRightCell").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_RIGHT, "FocusRightCell").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(LEFT, "FocusLeftCell").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(KP_LEFT, "FocusLeftCell").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(A, "SelectAll").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(HOME, "FocusFirstRow").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(END, "FocusLastRow").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(SPACE, "toggleFocusOwnerSelection").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "FocusPageUp").ctrl());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "FocusPageDown").ctrl());
            
            TABLE_VIEW_BINDINGS.add(new KeyBinding(UP, "DiscontinuousSelectPreviousRow").ctrl().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "DiscontinuousSelectNextRow").ctrl().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(LEFT, "DiscontinuousSelectPreviousColumn").ctrl().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(RIGHT, "DiscontinuousSelectNextColumn").ctrl().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "DiscontinuousSelectPageUp").ctrl().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "DiscontinuousSelectPageDown").ctrl().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(HOME, "DiscontinuousSelectAllToFirstRow").ctrl().shift());
            TABLE_VIEW_BINDINGS.add(new KeyBinding(END, "DiscontinuousSelectAllToLastRow").ctrl().shift());
        }

        TABLE_VIEW_BINDINGS.add(new KeyBinding(ENTER, "Activate"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(SPACE, "Activate"));
        TABLE_VIEW_BINDINGS.add(new KeyBinding(F2, "Activate"));
//        TABLE_VIEW_BINDINGS.add(new KeyBinding(SPACE, "Activate").ctrl());
        
        TABLE_VIEW_BINDINGS.add(new KeyBinding(ESCAPE, "CancelEdit"));

        if (PlatformUtil.isMac()) {
            TABLE_VIEW_BINDINGS.add(new KeyBinding(BACK_SLASH, "ClearSelection").meta());
        } else {
            TABLE_VIEW_BINDINGS.add(new KeyBinding(BACK_SLASH, "ClearSelection").ctrl());
        }
    }

    @Override protected void callAction(String name) {
        if ("SelectPreviousRow".equals(name)) selectPreviousRow();
        else if ("SelectNextRow".equals(name)) selectNextRow();
        else if ("SelectLeftCell".equals(name)) selectLeftCell();
        else if ("SelectRightCell".equals(name)) selectRightCell();
        else if ("SelectFirstRow".equals(name)) selectFirstRow();
        else if ("SelectLastRow".equals(name)) selectLastRow();
        else if ("SelectAll".equals(name)) selectAll();
        else if ("SelectAllPageUp".equals(name)) selectAllPageUp();
        else if ("SelectAllPageDown".equals(name)) selectAllPageDown();
        else if ("SelectAllToFirstRow".equals(name)) selectAllToFirstRow();
        else if ("SelectAllToLastRow".equals(name)) selectAllToLastRow();
        else if ("AlsoSelectNext".equals(name)) alsoSelectNext();
        else if ("AlsoSelectPrevious".equals(name)) alsoSelectPrevious();
        else if ("AlsoSelectLeftCell".equals(name)) alsoSelectLeftCell();
        else if ("AlsoSelectRightCell".equals(name)) alsoSelectRightCell();
        else if ("ClearSelection".equals(name)) clearSelection();
        else if ("ScrollUp".equals(name)) scrollUp();
        else if ("ScrollDown".equals(name)) scrollDown();
        else if ("FocusPreviousRow".equals(name)) focusPreviousRow();
        else if ("FocusNextRow".equals(name)) focusNextRow();
        else if ("FocusLeftCell".equals(name)) focusLeftCell();
        else if ("FocusRightCell".equals(name)) focusRightCell();
        else if ("Activate".equals(name)) activate();
        else if ("CancelEdit".equals(name)) cancelEdit();
        else if ("FocusFirstRow".equals(name)) focusFirstRow();
        else if ("FocusLastRow".equals(name)) focusLastRow();
        else if ("toggleFocusOwnerSelection".equals(name)) toggleFocusOwnerSelection();
        else if ("SelectAllToFocus".equals(name)) selectAllToFocus();
        else if ("FocusPageUp".equals(name)) focusPageUp();
        else if ("FocusPageDown".equals(name)) focusPageDown();
        else if ("DiscontinuousSelectNextRow".equals(name)) discontinuousSelectNextRow();
        else if ("DiscontinuousSelectPreviousRow".equals(name)) discontinuousSelectPreviousRow();
        else if ("DiscontinuousSelectNextColumn".equals(name)) discontinuousSelectNextColumn();
        else if ("DiscontinuousSelectPreviousColumn".equals(name)) discontinuousSelectPreviousColumn();
        else if ("DiscontinuousSelectPageUp".equals(name)) discontinuousSelectPageUp();
        else if ("DiscontinuousSelectPageDown".equals(name)) discontinuousSelectPageDown();
        else if ("DiscontinuousSelectAllToLastRow".equals(name)) discontinuousSelectAllToLastRow();
        else if ("DiscontinuousSelectAllToFirstRow".equals(name)) discontinuousSelectAllToFirstRow();
        else super.callAction(name);
    }

    @Override protected List<KeyBinding> createKeyBindings() {
        return TABLE_VIEW_BINDINGS;
    }
    
    @Override protected void callActionForEvent(KeyEvent e) {
        // RT-12751: we want to keep an eye on the user holding down the shift key, 
        // so that we know when they enter/leave multiple selection mode. This
        // changes what happens when certain key combinations are pressed.
        isShiftDown = e.getEventType() == KeyEvent.KEY_PRESSED && e.isShiftDown();
        isCtrlDown = e.getEventType() == KeyEvent.KEY_PRESSED && e.isControlDown();
        
        super.callActionForEvent(e);
    }

    /**************************************************************************
     *                         State and Functions                            *
     *************************************************************************/
    
    private boolean selectionChanging = false;
    
    private final ListChangeListener<TablePosition> selectedCellsListener = new ListChangeListener<TablePosition>() {
        @Override public void onChanged(ListChangeListener.Change c) {
            while (c.next()) {
                TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
                if (sm == null) return;
                
                TablePosition anchor = getAnchor();
                boolean cellSelectionEnabled = sm.isCellSelectionEnabled();
                
                if (! selectionChanging) {
                    // there are no selected items, so lets clear out the anchor
                    if (c.getList().isEmpty()) {
                        setAnchor(null);
                    } else if (! c.getList().contains(getAnchor())) {
                        setAnchor(null);
                    }
                } 
                
                int addedSize = c.getAddedSize();
                List<TablePosition> addedSubList = (List<TablePosition>) c.getAddedSubList();
                
                if (! hasAnchor() && addedSize > 0) {
                    for (int i = 0; i < addedSize; i++) {
                        TablePosition tp = addedSubList.get(i);
                        if (tp.getRow() >= 0) {
                            setAnchor(tp);
                            break;
                        }
                    }
                }
                
                if (!hasAnchor() && cellSelectionEnabled && ! selectionPathDeviated) {
                    // check if the selection is on the same row or column, 
                    // otherwise set selectionPathDeviated to true
                    for (int i = 0; i < addedSize; i++) {
                        TablePosition tp = addedSubList.get(i);
                        if (anchor.getRow() != -1 && tp.getRow() != anchor.getRow() && tp.getColumn() != anchor.getColumn()) {
                            selectionPathDeviated = true;
                            break;
                        }
                    }
                }
            }
        }
    };
    
    private final ChangeListener<TableView.TableViewSelectionModel<T>> selectionModelListener = 
            new ChangeListener<TableView.TableViewSelectionModel<T>>() {
        @Override
        public void changed(ObservableValue<? extends TableView.TableViewSelectionModel<T>> observable, 
                    TableView.TableViewSelectionModel<T> oldValue, 
                    TableView.TableViewSelectionModel<T> newValue) {
            if (oldValue != null) {
                oldValue.getSelectedCells().removeListener(weakSelectedCellsListener);
            }
            if (newValue != null) {
                newValue.getSelectedCells().addListener(weakSelectedCellsListener);
            }
        }
    };
    
    private final WeakListChangeListener<TablePosition> weakSelectedCellsListener = 
            new WeakListChangeListener<TablePosition>(selectedCellsListener);
    private final WeakChangeListener<TableView.TableViewSelectionModel<T>> weakSelectionModelListener = 
            new WeakChangeListener<TableView.TableViewSelectionModel<T>>(selectionModelListener);

    public TableViewBehavior(TableView control) {
        super(control);
        
        // Fix for RT-16565
        getControl().selectionModelProperty().addListener(weakSelectionModelListener);
        if (getControl().getSelectionModel() != null) {
            getControl().getSelectionModel().getSelectedCells().addListener(selectedCellsListener);
        }
    }

    @Override public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        
        // FIXME can't assume (yet) cells.get(0) is necessarily the lead cell
        ObservableList<TablePosition> cells = getControl().getSelectionModel().getSelectedCells();
        setAnchor(cells.isEmpty() ? null : cells.get(0));
        
        if (!getControl().isFocused() && getControl().isFocusTraversable()) {
            getControl().requestFocus();
        }
    }
    
    private boolean isCtrlDown = false;
    private boolean isShiftDown = false;
    private boolean selectionPathDeviated = false;
    
    
    /*
     * Anchor is created upon
     * - initial selection of an item (by mouse or keyboard)
     * 
     * Anchor is changed when you
     * - move the selection to an item by UP/DOWN/LEFT/RIGHT arrow keys
     * - select an item by mouse click
     * - add/remove an item to/from an existing selection by CTRL+SPACE shortcut
     * - add/remove an items to/from an existing selection by CTRL+mouse click
     * 
     * Note that if an item is removed from an existing selection by 
     * CTRL+SPACE/CTRL+mouse click, anchor still remains on this item even 
     * though it is not selected.
     * 
     * Anchor is NOT changed when you
     * - create linear multi-selection by SHIFT+UP/DOWN/LEFT/RIGHT arrow keys
     * - create linear multi-selection by SHIFT+SPACE arrow keys
     * - create linear multi-selection by SHIFT+mouse click
     * 
     * In case there is a discontinuous selection in the list, creating linear 
     * multi-selection between anchor and focused item will cancel the 
     * discontinuous selection. It means that only items that are located between
     * anchor and focused item will be selected. 
     */
    private void setAnchor(int row, TableColumn col) {
        setAnchor(row == -1 && col == null ? null : 
                new TablePosition(getControl(), row, col));
        
        selectionPathDeviated = false;
    }
    private void setAnchor(TablePosition tp) {
        TableCellBehavior.setAnchor(getControl(), tp);
        selectionPathDeviated = false;
    }
    private TablePosition getAnchor() {
        return TableCellBehavior.getAnchor(getControl());
    }
    
    private boolean hasAnchor() {
        return TableCellBehavior.hasAnchor(getControl());
    }
    
//    private void shiftAnchor(boolean rowDirection, int delta) {
//        if (anchor == null) return;
//        if (rowDirection) {
//            int currentRow = anchor.getRow();
//            int newRow = currentRow + delta;
//            if (newRow >= 0 && newRow < getItemCount()) {
//                setAnchor(newRow, anchor.getTableColumn());
//            }
//        } else {
//            System.err.println("can not shift in column direction yet");
//        }
//    }
    
    private int getItemCount() {
        return getControl().getItems() == null ? 0 : getControl().getItems().size();
    }
    
    
    
//    // Support for RT-13826:
//    // set when focus is moved by keyboard to allow for proper selection positions
//    private int selectPos = -1;

    private Callback<Void, Integer> onScrollPageUp;
    public void setOnScrollPageUp(Callback<Void, Integer> c) { onScrollPageUp = c; }

    private Callback<Void, Integer> onScrollPageDown;
    public void setOnScrollPageDown(Callback<Void, Integer> c) { onScrollPageDown = c; }

    private Runnable onFocusPreviousRow;
    public void setOnFocusPreviousRow(Runnable r) { onFocusPreviousRow = r; }

    private Runnable onFocusNextRow;
    public void setOnFocusNextRow(Runnable r) { onFocusNextRow = r; }

    private Runnable onSelectPreviousRow;
    public void setOnSelectPreviousRow(Runnable r) { onSelectPreviousRow = r; }

    private Runnable onSelectNextRow;
    public void setOnSelectNextRow(Runnable r) { onSelectNextRow = r; }

    private Runnable onMoveToFirstCell;
    public void setOnMoveToFirstCell(Runnable r) { onMoveToFirstCell = r; }

    private Runnable onMoveToLastCell;
    public void setOnMoveToLastCell(Runnable r) { onMoveToLastCell = r; }

    private Runnable onSelectRightCell;
    public void setOnSelectRightCell(Runnable r) { onSelectRightCell = r; }

    private Runnable onSelectLeftCell;
    public void setOnSelectLeftCell(Runnable r) { onSelectLeftCell = r; }
    
    private void scrollUp() {
        TableView.TableViewSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null || sm.getSelectedCells().isEmpty()) return;
        
        TablePosition selectedCell = sm.getSelectedCells().get(0);
        
        int newSelectedIndex = -1;
        if (onScrollPageUp != null) {
            newSelectedIndex = onScrollPageUp.call(null);
        }
        if (newSelectedIndex == -1) return;
        
        sm.clearAndSelect(newSelectedIndex, selectedCell.getTableColumn());
    }

    private void scrollDown() {
        TableView.TableViewSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null || sm.getSelectedCells().isEmpty()) return;
        
        TablePosition selectedCell = sm.getSelectedCells().get(0);
        
        int newSelectedIndex = -1;
        if (onScrollPageDown != null) {
            newSelectedIndex = onScrollPageDown.call(null);
        }
        if (newSelectedIndex == -1) return;
        
        sm.clearAndSelect(newSelectedIndex, selectedCell.getTableColumn());
    }
    
    private void focusFirstRow() {
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        
        TableColumn tc = fm.getFocusedCell() == null ? null : fm.getFocusedCell().getTableColumn();
        fm.focus(0, tc);
        
        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }
    
    private void focusLastRow() {
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        
        TableColumn tc = fm.getFocusedCell() == null ? null : fm.getFocusedCell().getTableColumn();
        fm.focus(getItemCount() - 1, tc);
        
        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private void focusPreviousRow() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        if (sm.isCellSelectionEnabled()) {
            fm.focusAboveCell();
        } else {
            fm.focusPrevious();
        }
        
        if (! isCtrlDown || getAnchor() == null) {
            setAnchor(fm.getFocusedIndex(), null);
        }

        if (onFocusPreviousRow != null) onFocusPreviousRow.run();
    }

    private void focusNextRow() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        
        if (sm.isCellSelectionEnabled()) {
            fm.focusBelowCell();
        } else {
            fm.focusNext();
        }
        
        if (! isCtrlDown || getAnchor() == null) {
            setAnchor(fm.getFocusedIndex(), null);
        }
        
        if (onFocusNextRow != null) onFocusNextRow.run();
    }

    private void focusLeftCell() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        fm.focusLeftCell();
        if (onFocusPreviousRow != null) onFocusPreviousRow.run();
    }

    private void focusRightCell() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        fm.focusRightCell();
        if (onFocusNextRow != null) onFocusNextRow.run();
    }
    
    private void focusPageUp() {
        int newFocusIndex = onScrollPageUp.call(null);
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        TableColumn tc = fm.getFocusedCell() == null ? null : fm.getFocusedCell().getTableColumn();
        fm.focus(newFocusIndex, tc);
    }
    
    private void focusPageDown() {
        int newFocusIndex = onScrollPageDown.call(null);
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        TableColumn tc = fm.getFocusedCell() == null ? null : fm.getFocusedCell().getTableColumn();
        fm.focus(newFocusIndex, tc);
    }

    private void clearSelection() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        sm.clearSelection();
    }
    
    private void clearSelectionOutsideRange(int start, int end) {
        TableView.TableViewSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        
        List<Integer> indices = new ArrayList<Integer>(sm.getSelectedIndices());
        
        selectionChanging = true;
        for (int i = 0; i < indices.size(); i++) {
            int index = indices.get(i);
            if (index < min || index >= max) {
                sm.clearSelection(index);
            }
        }
        selectionChanging = false;
    }

    private void alsoSelectPrevious() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null || sm.getSelectionMode() == SelectionMode.SINGLE) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        
        if (sm.isCellSelectionEnabled()) {
            updateCellVerticalSelection(-1, new Runnable() {
                @Override public void run() {
                    getControl().getSelectionModel().selectAboveCell();
                }
            });
        } else {
            if (isShiftDown && hasAnchor()) {
                updateRowSelection(-1);
            } else {
                sm.selectPrevious();
            }
        }
        onSelectPreviousRow.run();
    }
    
    private void alsoSelectNext() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null || sm.getSelectionMode() == SelectionMode.SINGLE) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        if (sm.isCellSelectionEnabled()) {
            updateCellVerticalSelection(1, new Runnable() {
                @Override public void run() {
                    getControl().getSelectionModel().selectBelowCell();
                }
            });
        } else {
            if (isShiftDown && hasAnchor()) {
                updateRowSelection(1);
            } else {
                sm.selectNext();
            }
        }
        onSelectNextRow.run();
    }
    
    private void alsoSelectLeftCell() {
        updateCellHorizontalSelection(-1, new Runnable() {
            @Override public void run() { 
                getControl().getSelectionModel().selectLeftCell();
            }
        });
    }

    private void alsoSelectRightCell() {
        updateCellHorizontalSelection(1, new Runnable() {
            @Override public void run() { 
                getControl().getSelectionModel().selectRightCell();
            }
        });
    }
    
    private void updateRowSelection(int delta) {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null || sm.getSelectionMode() == SelectionMode.SINGLE) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        
        int newRow = fm.getFocusedIndex() + delta;
        TablePosition anchor = getAnchor();
        
        if (! hasAnchor()) {
            setAnchor(fm.getFocusedCell());
        } 

        clearSelectionOutsideRange(anchor.getRow(), newRow);

        if (anchor.getRow() > newRow) {
            sm.selectRange(anchor.getRow(), newRow - 1);
        } else {
            sm.selectRange(anchor.getRow(), newRow + 1);
        }
    }
    
    private void updateCellVerticalSelection(int delta, Runnable defaultAction) {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null || sm.getSelectionMode() == SelectionMode.SINGLE) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        
        TablePosition focusedCell = fm.getFocusedCell();
        if (isShiftDown && sm.isSelected(focusedCell.getRow() + delta, focusedCell.getTableColumn())) {
            int newFocusOwner = focusedCell.getRow() + delta;
            sm.clearSelection(selectionPathDeviated ? newFocusOwner : focusedCell.getRow(), focusedCell.getTableColumn());
            fm.focus(newFocusOwner, focusedCell.getTableColumn());
        } else if (isShiftDown && getAnchor() != null && ! selectionPathDeviated) {
            int newRow = fm.getFocusedIndex() + delta;

            int start = Math.min(getAnchor().getRow(), newRow);
            int end = Math.max(getAnchor().getRow(), newRow);
            for (int _row = start; _row <= end; _row++) {
                sm.select(_row, focusedCell.getTableColumn());
            }
            fm.focus(newRow, focusedCell.getTableColumn());
        } else {
            final int focusIndex = fm.getFocusedIndex();
            if (! sm.isSelected(focusIndex, focusedCell.getTableColumn())) {
                sm.select(focusIndex, focusedCell.getTableColumn());
            }
            defaultAction.run();
        }
    }
    
    private void updateCellHorizontalSelection(int delta, Runnable defaultAction) {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null || sm.getSelectionMode() == SelectionMode.SINGLE) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        
        TablePosition focusedCell = fm.getFocusedCell();
        if (focusedCell == null || focusedCell.getTableColumn() == null) return;
        
        TableColumn adjacentColumn = getColumn(focusedCell.getTableColumn(), delta);
        if (adjacentColumn == null) return;
        
        if (isShiftDown && getAnchor() != null && 
            sm.isSelected(focusedCell.getRow(), adjacentColumn) &&
            ! (focusedCell.getRow() == getAnchor().getRow() && focusedCell.getTableColumn().equals(adjacentColumn))) {
                sm.clearSelection(focusedCell.getRow(),selectionPathDeviated ? adjacentColumn : focusedCell.getTableColumn());
                fm.focus(focusedCell.getRow(), adjacentColumn);
        } else if (isShiftDown && getAnchor() != null && ! selectionPathDeviated) {
            int newColumn = focusedCell.getColumn() + delta;

            int start = Math.min(getAnchor().getColumn(), newColumn);
            int end = Math.max(getAnchor().getColumn(), newColumn);
            for (int _col = start; _col <= end; _col++) {
                sm.select(focusedCell.getRow(), getColumn(_col));
            }
            fm.focus(focusedCell.getRow(), getColumn(newColumn));
        } else {
            defaultAction.run();
        }
    }
    
    private TableColumn getColumn(int index) {
        return getControl().getVisibleLeafColumn(index);
    }
    
    private TableColumn getColumn(TableColumn tc, int delta) {
        return getControl().getVisibleLeafColumn(getControl().getVisibleLeafIndex(tc) + delta);
    }

    private void selectFirstRow() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        ObservableList<TablePosition> selection = sm.getSelectedCells();
        TableColumn<?,?> selectedColumn = selection.size() == 0 ? null : selection.get(0).getTableColumn();
        sm.clearAndSelect(0, selectedColumn);

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    private void selectLastRow() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        ObservableList<TablePosition> selection = sm.getSelectedCells();
        TableColumn<?,?> selectedColumn = selection.size() == 0 ? null : selection.get(0).getTableColumn();
        sm.clearAndSelect(getItemCount() - 1, selectedColumn);

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private void selectPreviousRow() {
        selectCell(-1, 0);
        if (onSelectPreviousRow != null) onSelectPreviousRow.run();
    }

    private void selectNextRow() {
        selectCell(1, 0);
        if (onSelectNextRow != null) onSelectNextRow.run();
    }

    private void selectLeftCell() {
        selectCell(0, -1);
        if (onSelectLeftCell != null) onSelectLeftCell.run();
    }

    private void selectRightCell() {
        selectCell(0, 1);
        if (onSelectRightCell != null) onSelectRightCell.run();
    }

    private void selectCell(int rowDiff, int columnDiff) {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        TablePosition focusedCell = fm.getFocusedCell();
        int currentRow = focusedCell.getRow();
        int currentColumn = focusedCell.getColumn();
        if (rowDiff < 0 && currentRow == 0) return;
        else if (rowDiff > 0 && currentRow == getItemCount() - 1) return;
        else if (columnDiff < 0 && currentColumn == 0) return;
        else if (columnDiff > 0 && currentColumn == getControl().getVisibleLeafColumns().size() - 1) return;

        TableColumn tc = focusedCell.getTableColumn();
        tc = getColumn(tc, columnDiff);
        
        int row = focusedCell.getRow() + rowDiff;
        sm.clearAndSelect(row, tc);
        setAnchor(row, tc);
    }
    
    private void cancelEdit() {
        getControl().edit(-1, null);
    }

    private void activate() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        TablePosition cell = fm.getFocusedCell();
        sm.select(cell.getRow(), cell.getTableColumn());

        // edit this row also
        getControl().edit(cell.getRow(), cell.getTableColumn());
    }
    
    private void selectAllToFocus() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        TablePosition focusedCell = fm.getFocusedCell();
        int focusRow = focusedCell.getRow();
        
        TablePosition anchor = getAnchor();
        int anchorRow = anchor.getRow();
        
        sm.clearSelection();
        if (! sm.isCellSelectionEnabled()) {
            int startPos = anchorRow;
            int endPos = anchorRow > focusRow ? focusRow - 1 : focusRow + 1;
            sm.selectRange(startPos, endPos);
        } else {
            // we add all cells/rows between the current selection focus and
            // the acnhor (inclusive) to the current selection.

            // and then determine all row and columns which must be selected
            int minRow = Math.min(focusedCell.getRow(), anchorRow);
            int maxRow = Math.max(focusedCell.getRow(), anchorRow);
            int minColumn = Math.min(focusedCell.getColumn(), anchor.getColumn());
            int maxColumn = Math.max(focusedCell.getColumn(), anchor.getColumn());

            // clear selection
            sm.clearSelection();

            // and then perform the selection
            for (int _row = minRow; _row <= maxRow; _row++) {
                for (int _col = minColumn; _col <= maxColumn; _col++) {
                    sm.select(_row, getControl().getVisibleLeafColumn(_col));
                }
            }
        }
        
        setAnchor(anchor);
    }
    
    private void selectAll() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;
        sm.selectAll();
    }

    private void selectAllToFirstRow() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        TablePosition focusedCell = fm.getFocusedCell();
        
        int leadIndex = focusedCell.getRow();
        
        if (isShiftDown) {
            leadIndex = getAnchor() == null ? leadIndex : getAnchor().getRow();
        }

        sm.clearSelection();
        if (! sm.isCellSelectionEnabled()) {
            // we are going from 0 to one before the focused cell as that is
            // the requirement of selectRange, so we call focus on the 0th row
            sm.selectRange(0, leadIndex + 1);
            getControl().getFocusModel().focus(0);
//            setAnchor(leadIndex, null);
        } else {
            // TODO
            
//            setAnchor(leadIndex, );
        }
        
        if (isShiftDown) {
            setAnchor(leadIndex, null);
        }

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    private void selectAllToLastRow() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        TablePosition focusedCell = fm.getFocusedCell();
        
        int leadIndex = focusedCell.getRow();
        
        if (isShiftDown) {
            leadIndex = getAnchor() == null ? leadIndex : getAnchor().getRow();
        }
        
        sm.clearSelection();
        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(leadIndex, getItemCount());
        } else {
            // TODO
        }
        
        if (isShiftDown) {
            setAnchor(leadIndex, null);
        }

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }
    
    private void selectAllPageUp() {
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        if (isShiftDown) {
            leadIndex = getAnchor() == null ? leadIndex : getAnchor().getRow();
            setAnchor(leadIndex, null);
        }
        
        int leadSelectedIndex = onScrollPageUp.call(null);
        
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        selectionChanging = true;
        sm.clearSelection();
        sm.selectRange(leadSelectedIndex, leadIndex + 1);
        selectionChanging = false;
    }
    
    private void selectAllPageDown() {
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        
        int leadIndex = fm.getFocusedIndex();
        if (isShiftDown) {
            leadIndex = getAnchor() == null ? leadIndex : getAnchor().getRow();
            setAnchor(leadIndex, null);
        }
        
        int leadSelectedIndex = onScrollPageDown.call(null);
        
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        selectionChanging = true;
        sm.clearSelection();
        sm.selectRange(leadIndex, leadSelectedIndex + 1);
        selectionChanging = false;
    }
    
    private void toggleFocusOwnerSelection() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;

        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        TablePosition focusedCell = fm.getFocusedCell();
        
        if (sm.isSelected(focusedCell.getRow(), focusedCell.getTableColumn())) {
            sm.clearSelection(focusedCell.getRow(), focusedCell.getTableColumn());
            fm.focus(focusedCell.getRow(), focusedCell.getTableColumn());
        } else {
            sm.select(focusedCell.getRow(), focusedCell.getTableColumn());
        }
        
        setAnchor(focusedCell.getRow(), focusedCell.getTableColumn());
    }
    
    // This functionality was added, but then removed when it was realised by 
    // UX that TableView should not include 'spreadsheet-like' functionality.
    // When / if we ever introduce this kind of control, this functionality can
    // be re-enabled then.
    /*
    private void moveToLeftMostColumn() {
        // Functionality as described in RT-12752
        if (onMoveToLeftMostColumn != null) onMoveToLeftMostColumn.run();
        
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null || ! sm.isCellSelectionEnabled()) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        TablePosition focusedCell = fm.getFocusedCell();
        
        TableColumn endColumn = getControl().getVisibleLeafColumn(0);
        sm.clearAndSelect(focusedCell.getRow(), endColumn);
    }
    
    private void moveToRightMostColumn() {
        // Functionality as described in RT-12752
        if (onMoveToRightMostColumn != null) onMoveToRightMostColumn.run();
        
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null || ! sm.isCellSelectionEnabled()) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        TablePosition focusedCell = fm.getFocusedCell();
        
        TableColumn endColumn = getControl().getVisibleLeafColumn(getControl().getVisibleLeafColumns().size() - 1);
        sm.clearAndSelect(focusedCell.getRow(), endColumn);
    }
     */
    
    
    /**************************************************************************
     * Discontinuous Selection                                                *
     *************************************************************************/
    
    private void discontinuousSelectPreviousRow() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        
        int index = fm.getFocusedIndex() - 1;
        if (index < 0) return;
        
        if (! sm.isCellSelectionEnabled()) {
            sm.select(index);
        } else {
            sm.select(index, fm.getFocusedCell().getTableColumn());
        }
    }
    
    private void discontinuousSelectNextRow() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex() + 1;
        
        if (! sm.isCellSelectionEnabled()) {
            sm.select(index);
        } else {
            sm.select(index, fm.getFocusedCell().getTableColumn());
        }
    }
    
    private void discontinuousSelectPreviousColumn() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null || ! sm.isCellSelectionEnabled()) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        TableColumn tc = getColumn(fm.getFocusedCell().getTableColumn(), -1);
        sm.select(fm.getFocusedIndex(), tc);
    }
    
    private void discontinuousSelectNextColumn() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null || ! sm.isCellSelectionEnabled()) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        TableColumn tc = getColumn(fm.getFocusedCell().getTableColumn(), 1);
        sm.select(fm.getFocusedIndex(), tc);
    }
    
    private void discontinuousSelectPageUp() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        int leadSelectedIndex = onScrollPageUp.call(null);
        
        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(leadSelectedIndex, leadIndex + 1);
        }
    }
    
    private void discontinuousSelectPageDown() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel fm = getControl().getFocusModel();
        if (fm == null) return;
        
        int leadIndex = fm.getFocusedIndex();
        int leadSelectedIndex = onScrollPageDown.call(null);
        
        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(leadIndex, leadSelectedIndex + 1);
        }
    }
    
    private void discontinuousSelectAllToFirstRow() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex();
        
        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(0, index);
        } else {
            for (int i = 0; i < index; i++) {
                sm.select(i, fm.getFocusedCell().getTableColumn());
            }
        }

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }
    
    private void discontinuousSelectAllToLastRow() {
        TableView.TableViewSelectionModel sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        TableViewFocusModel fm = getControl().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex() + 1;
        
        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(index, getItemCount());
        } else {
            for (int i = index; i < getItemCount(); i++) {
                sm.select(i, fm.getFocusedCell().getTableColumn());
            }
        }

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }   
}
