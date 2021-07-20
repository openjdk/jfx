/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.SizeLimitedList;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.PlatformUtil;
import static javafx.scene.input.KeyCode.*;
import static com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;

public abstract class TableViewBehaviorBase<C extends Control, T, TC extends TableColumnBase<T,?>> extends BehaviorBase<C> {

    /**************************************************************************
     *                                                                        *
     * Internal fields                                                        *
     *                                                                        *
     *************************************************************************/

    private final InputMap<C> tableViewInputMap;

    protected boolean isShortcutDown = false;
    protected boolean isShiftDown = false;
    private boolean selectionPathDeviated = false;
    protected boolean selectionChanging = false;

    private final EventHandler<KeyEvent> keyEventListener = e -> {
        if (!e.isConsumed()) {
            // RT-12751: we want to keep an eye on the user holding down the shift key,
            // so that we know when they enter/leave multiple selection mode. This
            // changes what happens when certain key combinations are pressed.
            isShiftDown = e.getEventType() == KeyEvent.KEY_PRESSED && e.isShiftDown();
            isShortcutDown = e.getEventType() == KeyEvent.KEY_PRESSED && e.isShortcutDown();
        }
    };

    private final SizeLimitedList<TablePositionBase> selectionHistory = new SizeLimitedList<>(10);

    protected final ListChangeListener<TablePositionBase> selectedCellsListener = c -> {
        while (c.next()) {
            if (c.wasReplaced()) {
                if (TreeTableCellBehavior.hasDefaultAnchor(getNode())) {
                    TreeTableCellBehavior.removeAnchor(getNode());
                }
            }

            if (! c.wasAdded()) {
                continue;
            }

            TableSelectionModel sm = getSelectionModel();
            if (sm == null) return;

            TablePositionBase anchor = getAnchor();
            boolean cellSelectionEnabled = sm.isCellSelectionEnabled();

            int addedSize = c.getAddedSize();
            List<TablePositionBase> addedSubList = (List<TablePositionBase>) c.getAddedSubList();

            for (TablePositionBase tpb : addedSubList) {
                if (! selectionHistory.contains(tpb)) {
                    selectionHistory.add(tpb);
                }
            }

            // newest selection
            if (addedSize > 0 && ! hasAnchor()) {
                TablePositionBase tp = addedSubList.get(addedSize - 1);
                setAnchor(tp);
            }

            if (anchor != null && cellSelectionEnabled && ! selectionPathDeviated) {
                // check if the selection is on the same row or column,
                // otherwise set selectionPathDeviated to true
                for (int i = 0; i < addedSize; i++) {
                    TablePositionBase tp = addedSubList.get(i);
                    if (anchor.getRow() != -1 && tp.getRow() != anchor.getRow() && tp.getColumn() != anchor.getColumn()) {
                        setSelectionPathDeviated(true);
                        break;
                    }
                }
            }
        }
    };

    protected final WeakListChangeListener<TablePositionBase> weakSelectedCellsListener =
            new WeakListChangeListener<TablePositionBase>(selectedCellsListener);



    /**************************************************************************
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     *************************************************************************/

    public TableViewBehaviorBase(C control) {
        super(control);


        // create a map for TableView(Base)-specific mappings
        tableViewInputMap = createInputMap();

        KeyMapping enterKeyActivateMapping, escapeKeyCancelEditMapping;
        addDefaultMapping(tableViewInputMap,
                new KeyMapping(TAB, FocusTraversalInputMap::traverseNext),
                new KeyMapping(new KeyBinding(TAB).shift(), FocusTraversalInputMap::traversePrevious),

                new KeyMapping(HOME, e -> selectFirstRow()),
                new KeyMapping(END, e -> selectLastRow()),

                new KeyMapping(PAGE_UP, e -> scrollUp()),
                new KeyMapping(PAGE_DOWN, e -> scrollDown()),

                new KeyMapping(LEFT, e -> { if(isRTL()) selectRightCell(); else selectLeftCell(); }),
                new KeyMapping(KP_LEFT,e -> { if(isRTL()) selectRightCell(); else selectLeftCell(); }),
                new KeyMapping(RIGHT, e -> { if(isRTL()) selectLeftCell(); else selectRightCell(); }),
                new KeyMapping(KP_RIGHT, e -> { if(isRTL()) selectLeftCell(); else selectRightCell(); }),

                new KeyMapping(UP, e -> selectPreviousRow()),
                new KeyMapping(KP_UP, e -> selectPreviousRow()),
                new KeyMapping(DOWN, e -> selectNextRow()),
                new KeyMapping(KP_DOWN, e -> selectNextRow()),

                new KeyMapping(LEFT,   e -> { if(isRTL()) focusTraverseRight(); else focusTraverseLeft(); }),
                new KeyMapping(KP_LEFT, e -> { if(isRTL()) focusTraverseRight(); else focusTraverseLeft(); }),
                new KeyMapping(RIGHT, e -> { if(isRTL()) focusTraverseLeft(); else focusTraverseRight(); }),
                new KeyMapping(KP_RIGHT, e -> { if(isRTL()) focusTraverseLeft(); else focusTraverseRight(); }),
                new KeyMapping(UP, FocusTraversalInputMap::traverseUp),
                new KeyMapping(KP_UP, FocusTraversalInputMap::traverseUp),
                new KeyMapping(DOWN, FocusTraversalInputMap::traverseDown),
                new KeyMapping(KP_DOWN, FocusTraversalInputMap::traverseDown),

                new KeyMapping(new KeyBinding(HOME).shift(), e -> selectAllToFirstRow()),
                new KeyMapping(new KeyBinding(END).shift(), e -> selectAllToLastRow()),
                new KeyMapping(new KeyBinding(PAGE_UP).shift(), e -> selectAllPageUp()),
                new KeyMapping(new KeyBinding(PAGE_DOWN).shift(), e -> selectAllPageDown()),

                new KeyMapping(new KeyBinding(UP).shift(), e -> alsoSelectPrevious()),
                new KeyMapping(new KeyBinding(KP_UP).shift(), e -> alsoSelectPrevious()),
                new KeyMapping(new KeyBinding(DOWN).shift(), e -> alsoSelectNext()),
                new KeyMapping(new KeyBinding(KP_DOWN).shift(), e -> alsoSelectNext()),

                new KeyMapping(new KeyBinding(SPACE).shift(), e -> selectAllToFocus(false)),
                new KeyMapping(new KeyBinding(SPACE).shortcut().shift(), e -> selectAllToFocus(true)),

                new KeyMapping(new KeyBinding(LEFT).shift(), e -> { if(isRTL()) alsoSelectRightCell(); else alsoSelectLeftCell(); }),
                new KeyMapping(new KeyBinding(KP_LEFT).shift(),  e -> { if(isRTL()) alsoSelectRightCell(); else alsoSelectLeftCell(); }),
                new KeyMapping(new KeyBinding(RIGHT).shift(),  e -> { if(isRTL()) alsoSelectLeftCell(); else alsoSelectRightCell(); }),
                new KeyMapping(new KeyBinding(KP_RIGHT).shift(), e -> { if(isRTL()) alsoSelectLeftCell(); else alsoSelectRightCell(); }),

                new KeyMapping(new KeyBinding(UP).shortcut(), e -> focusPreviousRow()),
                new KeyMapping(new KeyBinding(DOWN).shortcut(), e -> focusNextRow()),
                new KeyMapping(new KeyBinding(RIGHT).shortcut(), e -> { if(isRTL()) focusLeftCell(); else focusRightCell(); }),
                new KeyMapping(new KeyBinding(KP_RIGHT).shortcut(), e -> { if(isRTL()) focusLeftCell(); else focusRightCell(); }),
                new KeyMapping(new KeyBinding(LEFT).shortcut(), e -> { if(isRTL()) focusRightCell(); else focusLeftCell(); }),
                new KeyMapping(new KeyBinding(KP_LEFT).shortcut(), e -> { if(isRTL()) focusRightCell(); else focusLeftCell(); }),

                new KeyMapping(new KeyBinding(A).shortcut(), e -> selectAll()),
                new KeyMapping(new KeyBinding(HOME).shortcut(), e -> focusFirstRow()),
                new KeyMapping(new KeyBinding(END).shortcut(), e -> focusLastRow()),
                new KeyMapping(new KeyBinding(PAGE_UP).shortcut(), e -> focusPageUp()),
                new KeyMapping(new KeyBinding(PAGE_DOWN).shortcut(), e -> focusPageDown()),

                new KeyMapping(new KeyBinding(UP).shortcut().shift(), e -> discontinuousSelectPreviousRow()),
                new KeyMapping(new KeyBinding(DOWN).shortcut().shift(), e -> discontinuousSelectNextRow()),
                new KeyMapping(new KeyBinding(LEFT).shortcut().shift(), e -> { if(isRTL()) discontinuousSelectNextColumn(); else discontinuousSelectPreviousColumn(); }),
                new KeyMapping(new KeyBinding(RIGHT).shortcut().shift(), e -> { if(isRTL()) discontinuousSelectPreviousColumn(); else discontinuousSelectNextColumn(); }),
                new KeyMapping(new KeyBinding(PAGE_UP).shortcut().shift(), e -> discontinuousSelectPageUp()),
                new KeyMapping(new KeyBinding(PAGE_DOWN).shortcut().shift(), e -> discontinuousSelectPageDown()),
                new KeyMapping(new KeyBinding(HOME).shortcut().shift(), e -> discontinuousSelectAllToFirstRow()),
                new KeyMapping(new KeyBinding(END).shortcut().shift(), e -> discontinuousSelectAllToLastRow()),

                enterKeyActivateMapping = new KeyMapping(ENTER, this::activate),
                new KeyMapping(SPACE, this::activate),
                new KeyMapping(F2, this::activate),
                escapeKeyCancelEditMapping = new KeyMapping(ESCAPE, this::cancelEdit),

                new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed)
        );

        enterKeyActivateMapping.setAutoConsume(false);
        escapeKeyCancelEditMapping.setAutoConsume(false);

        // create OS-specific child mappings
        // --- mac OS
        InputMap<C> macInputMap = new InputMap<>(control);
        macInputMap.setInterceptor(event -> !PlatformUtil.isMac());
        addDefaultMapping(macInputMap, new KeyMapping(new KeyBinding(SPACE).shortcut().ctrl(), e -> toggleFocusOwnerSelection()));
        addDefaultChildMap(tableViewInputMap, macInputMap);

        // --- all other platforms
        InputMap<C> otherOsInputMap = new InputMap<>(control);
        otherOsInputMap.setInterceptor(event -> PlatformUtil.isMac());
        addDefaultMapping(otherOsInputMap, new KeyMapping(new KeyBinding(SPACE).ctrl(), e -> toggleFocusOwnerSelection()));
        addDefaultChildMap(tableViewInputMap, otherOsInputMap);

        // set up other listeners
        // We make this an event _filter_ so that we can determine the state
        // of the shift key before the event handlers get a shot at the event.
        control.addEventFilter(KeyEvent.ANY, keyEventListener);
    }



    /**************************************************************************
     *                                                                        *
     * Abstract API                                                           *
     *                                                                        *
     *************************************************************************/

    /** {@inheritDoc} */
    @Override public InputMap<C> getInputMap() {
        return tableViewInputMap;
    }

    /**
     * Call to record the current anchor position
     */
    protected void setAnchor(TablePositionBase tp) {
        TableCellBehaviorBase.setAnchor(getNode(), tp, false);
        setSelectionPathDeviated(false);
    }

    /**
     * Will return the current anchor position.
     */
    protected TablePositionBase getAnchor() {
        return TableCellBehaviorBase.getAnchor(getNode(), getFocusedCell());
    }

    /**
     * Returns true if there is an anchor set, and false if not anchor is set.
     */
    protected boolean hasAnchor() {
        return TableCellBehaviorBase.hasNonDefaultAnchor(getNode());
    }

    /**
     * Returns the number of items in the underlying data model.
     */
    protected abstract int getItemCount();

    /**
     * Returns the focus model for the underlying UI control (which must extend
     * from TableFocusModel).
     */
    protected abstract TableFocusModel getFocusModel();

    /**
     * Returns the selection model for the underlying UI control (which must extend
     * from TableSelectionModel).
     */
    protected abstract TableSelectionModel<T> getSelectionModel();

    /**
     * Returns an observable list of all cells that are currently selected in
     * the selection model of the underlying control.
     */
    protected abstract ObservableList<? extends TablePositionBase/*<C,TC>*/> getSelectedCells();

    /**
     * Returns the focused cell from the focus model of the underlying control.
     */
    protected abstract TablePositionBase getFocusedCell();

    /**
     * Returns the position of the given table column in the visible leaf columns
     * list of the underlying control.
     */
    protected abstract int getVisibleLeafIndex(TableColumnBase tc);

    /**
     * Returns the column at the given index in the visible leaf columns list of
     * the underlying control.
     */
    protected abstract TableColumnBase getVisibleLeafColumn(int index);

    /**
     * Returns true if the control (i.e. TableView / TreeTableView) is editable
     */
    protected abstract boolean isControlEditable();

    /**
     * Begins the edit process in the underlying control for the given row/column
     * position.
     */
    protected abstract void editCell(int row, TableColumnBase tc);

    /**
     * Returns an observable list of all visible leaf columns in the underlying
     * control.
     */
    protected abstract ObservableList<? extends TableColumnBase> getVisibleLeafColumns();

    /**
     * Creates a TablePositionBase instance using the underlying controls
     * concrete implementation for the given row/column intersection.
     */
    protected abstract TablePositionBase<TC> getTablePosition(int row, TableColumnBase<T,?> tc);



    /**************************************************************************
     *                                                                        *
     * Public API                                                             *
     *                                                                        *
     *************************************************************************/

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
    protected void setAnchor(int row, TableColumnBase col) {
        setAnchor(row == -1 && col == null ? null : getTablePosition(row, col));
    }

    private Callback<Boolean, Integer> onScrollPageUp;
    public void setOnScrollPageUp(Callback<Boolean, Integer> c) { onScrollPageUp = c; }

    private Callback<Boolean, Integer> onScrollPageDown;
    public void setOnScrollPageDown(Callback<Boolean, Integer> c) { onScrollPageDown = c; }

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

    private Runnable onFocusRightCell;
    public void setOnFocusRightCell(Runnable r) { onFocusRightCell = r; }

    private Runnable onFocusLeftCell;
    public void setOnFocusLeftCell(Runnable r) { onFocusLeftCell = r; }

    public void mousePressed(MouseEvent e) {
//        // FIXME can't assume (yet) cells.get(0) is necessarily the lead cell
//        ObservableList<? extends TablePositionBase> cells = getSelectedCells();
//        setAnchor(cells.isEmpty() ? null : cells.get(0));

        if (!getNode().isFocused() && getNode().isFocusTraversable()) {
            getNode().requestFocus();
        }
    }

    protected boolean isRTL() {
        return (getNode().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT);
    }


    /**************************************************************************
     *                                                                        *
     * Private implementation                                                 *
     *                                                                        *
     *************************************************************************/

    private void setSelectionPathDeviated(boolean selectionPathDeviated) {
        this.selectionPathDeviated = selectionPathDeviated;
    }

    protected void scrollUp() {
        TableSelectionModel<T> sm = getSelectionModel();
        if (sm == null || getSelectedCells().isEmpty()) return;

        TablePositionBase<TC> selectedCell = getSelectedCells().get(0);

        int newSelectedIndex = -1;
        if (onScrollPageUp != null) {
            newSelectedIndex = onScrollPageUp.call(false);
        }
        if (newSelectedIndex == -1) return;

        sm.clearAndSelect(newSelectedIndex, selectedCell.getTableColumn());
    }

    protected void scrollDown() {
        TableSelectionModel<T> sm = getSelectionModel();
        if (sm == null || getSelectedCells().isEmpty()) return;

        TablePositionBase<TC> selectedCell = getSelectedCells().get(0);

        int newSelectedIndex = -1;
        if (onScrollPageDown != null) {
            newSelectedIndex = onScrollPageDown.call(false);
        }
        if (newSelectedIndex == -1) return;

        sm.clearAndSelect(newSelectedIndex, selectedCell.getTableColumn());
    }

    protected void focusFirstRow() {
        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TableColumnBase tc = getFocusedCell() == null ? null : getFocusedCell().getTableColumn();
        fm.focus(0, tc);

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    protected void focusLastRow() {
        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TableColumnBase tc = getFocusedCell() == null ? null : getFocusedCell().getTableColumn();
        fm.focus(getItemCount() - 1, tc);

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    protected void focusPreviousRow() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        if (sm.isCellSelectionEnabled()) {
            fm.focusAboveCell();
        } else {
            fm.focusPrevious();
        }

        if (! isShortcutDown || getAnchor() == null) {
            setAnchor(fm.getFocusedIndex(), null);
        }

        if (onFocusPreviousRow != null) onFocusPreviousRow.run();
    }

    protected void focusNextRow() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        if (sm.isCellSelectionEnabled()) {
            fm.focusBelowCell();
        } else {
            fm.focusNext();
        }

        if (! isShortcutDown || getAnchor() == null) {
            setAnchor(fm.getFocusedIndex(), null);
        }

        if (onFocusNextRow != null) onFocusNextRow.run();
    }

    protected void focusLeftCell() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        fm.focusLeftCell();
        if (onFocusLeftCell != null) onFocusLeftCell.run();
    }

    protected void focusRightCell() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        fm.focusRightCell();
        if (onFocusRightCell != null) onFocusRightCell.run();
    }

    protected void focusPageUp() {
        int newFocusIndex = onScrollPageUp.call(true);

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;
        TableColumnBase tc = getFocusedCell() == null ? null : getFocusedCell().getTableColumn();
        fm.focus(newFocusIndex, tc);
    }

    protected void focusPageDown() {
        int newFocusIndex = onScrollPageDown.call(true);

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;
        TableColumnBase tc = getFocusedCell() == null ? null : getFocusedCell().getTableColumn();
        fm.focus(newFocusIndex, tc);
    }

    protected void clearSelection() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        sm.clearSelection();
    }

    protected void clearSelectionOutsideRange(int start, int end, TableColumnBase<T,?> column) {
        TableSelectionModel<T> sm = getSelectionModel();
        if (sm == null) return;

        int min = Math.min(start, end);
        int max = Math.max(start, end);

        List<Integer> indices = new ArrayList<Integer>(sm.getSelectedIndices());

        selectionChanging = true;
        for (int i = 0; i < indices.size(); i++) {
            int index = indices.get(i);
            if (index < min || index > max) {
                sm.clearSelection(index, column);
            }
        }
        selectionChanging = false;
    }

    protected void alsoSelectPrevious() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        if (sm.getSelectionMode() == SelectionMode.SINGLE) {
            selectPreviousRow();
            return;
        }

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        if (sm.isCellSelectionEnabled()) {
            updateCellVerticalSelection(-1, () -> {
                getSelectionModel().selectAboveCell();
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

    protected void alsoSelectNext() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        if (sm.getSelectionMode() == SelectionMode.SINGLE) {
            selectNextRow();
            return;
        }

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        if (sm.isCellSelectionEnabled()) {
            updateCellVerticalSelection(1, () -> {
                getSelectionModel().selectBelowCell();
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

    protected void alsoSelectLeftCell() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null || !sm.isCellSelectionEnabled()) return;

        updateCellHorizontalSelection(-1, () -> getSelectionModel().selectLeftCell());
        onSelectLeftCell.run();
    }

    protected void alsoSelectRightCell() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null || !sm.isCellSelectionEnabled()) return;

        updateCellHorizontalSelection(1, () -> getSelectionModel().selectRightCell());
        onSelectRightCell.run();
    }

    protected void updateRowSelection(int delta) {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null || sm.getSelectionMode() == SelectionMode.SINGLE) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        int newRow = fm.getFocusedIndex() + delta;
        TablePositionBase anchor = getAnchor();

        if (! hasAnchor()) {
            setAnchor(getFocusedCell());
        }

        if (sm.getSelectedIndices().size() > 1) {
            clearSelectionOutsideRange(anchor.getRow(), newRow, null);
        }

        if (anchor.getRow() > newRow) {
            sm.selectRange(anchor.getRow(), newRow - 1);
        } else {
            sm.selectRange(anchor.getRow(), newRow + 1);
        }
    }

    protected void updateCellVerticalSelection(int delta, Runnable defaultAction) {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null || sm.getSelectionMode() == SelectionMode.SINGLE) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        final TablePositionBase focusedCell = getFocusedCell();
        final int focusedCellRow = focusedCell.getRow();

        if (isShiftDown && sm.isSelected(focusedCellRow + delta, focusedCell.getTableColumn())) {
            int newFocusOwner = focusedCellRow + delta;

            // work out if we're backtracking
            boolean backtracking = false;
            if (selectionHistory.size() >= 2) {
                TablePositionBase<TC> secondToLastSelectedCell = selectionHistory.get(1);
                backtracking = secondToLastSelectedCell.getRow() == newFocusOwner &&
                        secondToLastSelectedCell.getColumn() == focusedCell.getColumn();
            }

            // if the selection path has deviated from the anchor row / column, then we need to see if we're moving
            // backwards to the previous selection or not (as it determines what cell row we clear out)
            int cellRowToClear = selectionPathDeviated ?
                    (backtracking ? focusedCellRow : newFocusOwner) :
                    focusedCellRow;

            sm.clearSelection(cellRowToClear, focusedCell.getTableColumn());
            fm.focus(newFocusOwner, focusedCell.getTableColumn());
        } else if (isShiftDown && getAnchor() != null && ! selectionPathDeviated) {
            int newRow = fm.getFocusedIndex() + delta;

            // we don't let the newRow go outside the bounds of the data
            newRow = Math.max(Math.min(getItemCount() - 1, newRow), 0);

            int start = Math.min(getAnchor().getRow(), newRow);
            int end = Math.max(getAnchor().getRow(), newRow);

            if (sm.getSelectedIndices().size() > 1) {
                clearSelectionOutsideRange(start, end, focusedCell.getTableColumn());
            }

            for (int _row = start; _row <= end; _row++) {
                if (sm.isSelected(_row, focusedCell.getTableColumn())) {
                    continue;
                }
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

    protected void updateCellHorizontalSelection(int delta, Runnable defaultAction) {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null || sm.getSelectionMode() == SelectionMode.SINGLE) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        final TablePositionBase focusedCell = getFocusedCell();
        if (focusedCell == null || focusedCell.getTableColumn() == null) return;

        boolean atEnd = false;
        TableColumnBase adjacentColumn = getColumn(focusedCell.getTableColumn(), delta);
        if (adjacentColumn == null) {
            // if adjacentColumn is null, we use the focusedCell column, as we are
            // most probably at the very beginning or end of the row
            adjacentColumn = focusedCell.getTableColumn();
            atEnd = true;
        }

        final int focusedCellRow = focusedCell.getRow();

        if (isShiftDown && sm.isSelected(focusedCellRow, adjacentColumn)) {
            if (atEnd) {
                return;
            }

            // work out if we're backtracking
            boolean backtracking = false;
            ObservableList<? extends TablePositionBase> selectedCells = getSelectedCells();
            if (selectedCells.size() >= 2) {
                TablePositionBase<TC> secondToLastSelectedCell = selectedCells.get(selectedCells.size() - 2);
                backtracking = secondToLastSelectedCell.getRow() == focusedCellRow &&
                        secondToLastSelectedCell.getTableColumn().equals(adjacentColumn);
            }

            // if the selection path has deviated from the anchor row / column, then we need to see if we're moving
            // backwards to the previous selection or not (as it determines what cell column we clear out)
            TableColumnBase<?,?> cellColumnToClear = selectionPathDeviated ?
                    (backtracking ? focusedCell.getTableColumn() : adjacentColumn) :
                    focusedCell.getTableColumn();

            sm.clearSelection(focusedCellRow, cellColumnToClear);
            fm.focus(focusedCellRow, adjacentColumn);
        } else if (isShiftDown && getAnchor() != null && ! selectionPathDeviated) {
            final int anchorColumn = getAnchor().getColumn();

            // we don't let the newColumn go outside the bounds of the data
            int newColumn = getVisibleLeafIndex(focusedCell.getTableColumn()) + delta;
            newColumn = Math.max(Math.min(getVisibleLeafColumns().size() - 1, newColumn), 0);

            int start = Math.min(anchorColumn, newColumn);
            int end = Math.max(anchorColumn, newColumn);

            for (int _col = start; _col <= end; _col++) {
                sm.select(focusedCell.getRow(), getColumn(_col));
            }
            fm.focus(focusedCell.getRow(), getColumn(newColumn));
        } else {
            defaultAction.run();
        }
    }

    protected TableColumnBase getColumn(int index) {
        return getVisibleLeafColumn(index);
    }

    protected TableColumnBase getColumn(TableColumnBase tc, int delta) {
        return getVisibleLeafColumn(getVisibleLeafIndex(tc) + delta);
    }

    protected void selectFirstRow() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        ObservableList<? extends TablePositionBase> selection = getSelectedCells();
        TableColumnBase<?,?> selectedColumn = selection.size() == 0 ? null : selection.get(0).getTableColumn();
        sm.clearAndSelect(0, selectedColumn);

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    protected void selectLastRow() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        ObservableList<? extends TablePositionBase> selection = getSelectedCells();
        TableColumnBase<?,?> selectedColumn = selection.size() == 0 ? null : selection.get(0).getTableColumn();
        sm.clearAndSelect(getItemCount() - 1, selectedColumn);

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    protected void selectPreviousRow() {
        selectCell(-1, 0);
        if (onSelectPreviousRow != null) onSelectPreviousRow.run();
    }

    protected void selectNextRow() {
        selectCell(1, 0);
        if (onSelectNextRow != null) onSelectNextRow.run();
    }

    protected void selectLeftCell() {
        selectCell(0, -1);
        if (onSelectLeftCell != null) onSelectLeftCell.run();
    }

    protected void selectRightCell() {
        selectCell(0, 1);
        if (onSelectRightCell != null) onSelectRightCell.run();
    }

    protected void selectCell(int rowDiff, int columnDiff) {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TablePositionBase<TC> focusedCell = getFocusedCell();
        int currentRow = focusedCell.getRow();
        int currentColumn = getVisibleLeafIndex(focusedCell.getTableColumn());

        if (rowDiff > 0 && currentRow >= getItemCount() - 1) return;
        else if (columnDiff < 0 && currentColumn <= 0) return;
        else if (columnDiff > 0 && currentColumn >= getVisibleLeafColumns().size() - 1) return;
        else if (columnDiff > 0 && currentColumn == -1) return;

        TableColumnBase tc = focusedCell.getTableColumn();
        tc = getColumn(tc, columnDiff);

        //JDK-8222214: Moved this "if" here because the first row might be focused and not selected, so
        // this makes sure it gets selected when the users presses UP. If not it ends calling
        // VirtualFlow.scrollTo(-1) at and the content of the TableView disappears.
        int row = (currentRow <= 0 && rowDiff <= 0) ? 0 : focusedCell.getRow() + rowDiff;
        sm.clearAndSelect(row, tc);
        setAnchor(row, tc);
    }

    protected void cancelEdit(KeyEvent e) {
        if (isControlEditable()) {
            editCell(-1, null);
            e.consume();
        }
    }

    protected void activate(KeyEvent e) {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TablePositionBase<TC> cell = getFocusedCell();
        TC tableColumn = cell.getTableColumn();
        sm.select(cell.getRow(), tableColumn);
        setAnchor(cell);

        if (tableColumn == null) {
           return;
        }

        // check if we are editable
        boolean isEditable = isControlEditable() && tableColumn.isEditable();

        // edit this row also
        if (isEditable && cell.getRow() >= 0) {
            editCell(cell.getRow(), tableColumn);
            e.consume();
        }
    }

    protected void selectAllToFocus(boolean setAnchorToFocusIndex) {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TablePositionBase<TC> focusedCell = getFocusedCell();
        int focusRow = focusedCell.getRow();

        TablePositionBase<TC> anchor = getAnchor();
        int anchorRow = anchor.getRow();

        sm.clearSelection();
        if (! sm.isCellSelectionEnabled()) {
            int startPos = anchorRow;
            int endPos = anchorRow > focusRow ? focusRow - 1 : focusRow + 1;
            sm.selectRange(startPos, endPos);
        } else {
            // we add all cells/rows between the current selection focus and
            // the anchor (inclusive) to the current selection.
            // We want focus to end up on the current focus position.
            sm.selectRange(anchor.getRow(), anchor.getTableColumn(),
                           focusedCell.getRow(), focusedCell.getTableColumn());
        }

        setAnchor(setAnchorToFocusIndex ? focusedCell : anchor);
    }

    protected void selectAll() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;
        sm.selectAll();
    }

    protected void selectAllToFirstRow() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        final boolean isSingleSelection = sm.getSelectionMode() == SelectionMode.SINGLE;
        final TablePositionBase focusedCell = getFocusedCell();
        final TableColumnBase<?,?> column = getFocusedCell().getTableColumn();
        int leadIndex = focusedCell.getRow();

        if (isShiftDown) {
            leadIndex = getAnchor() == null ? leadIndex : getAnchor().getRow();
        }

        sm.clearSelection();
        if (! sm.isCellSelectionEnabled()) {
            // we are going from 0 to one before the focused cell as that is
            // the requirement of selectRange, so we call focus on the 0th row
            if (isSingleSelection) {
                sm.select(0);
            } else {
                sm.selectRange(leadIndex, -1);
            }
            fm.focus(0);
        } else {
            if (isSingleSelection) {
                sm.select(0, column);
            } else {
                sm.selectRange(leadIndex, column, -1, column);
            }
            fm.focus(0, column);
        }

        if (isShiftDown) {
            setAnchor(leadIndex, column);
        }

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    protected void selectAllToLastRow() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        final int itemCount = getItemCount();
        final TablePositionBase focusedCell = getFocusedCell();
        final TableColumnBase<?,?> column = getFocusedCell().getTableColumn();
        int leadIndex = focusedCell.getRow();

        if (isShiftDown) {
            leadIndex = getAnchor() == null ? leadIndex : getAnchor().getRow();
        }

        sm.clearSelection();
        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(leadIndex, itemCount);
        } else {
            sm.selectRange(leadIndex, column, itemCount - 1, column);
        }

        if (isShiftDown) {
            setAnchor(leadIndex, column);
        }

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    protected void selectAllPageUp() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        final TableColumnBase col = sm.isCellSelectionEnabled() ? getFocusedCell().getTableColumn() : null;
        if (isShiftDown) {
            leadIndex = getAnchor() == null ? leadIndex : getAnchor().getRow();
            setAnchor(leadIndex, col);
        }

        int leadSelectedIndex = onScrollPageUp.call(false);

        selectionChanging = true;
        if (sm.getSelectionMode() == null || sm.getSelectionMode() == SelectionMode.SINGLE) {
            if (sm.isCellSelectionEnabled()) {
                sm.select(leadSelectedIndex, col);
            } else {
                sm.select(leadSelectedIndex);
            }
        } else {
            sm.clearSelection();
            if (sm.isCellSelectionEnabled()) {
                sm.selectRange(leadIndex, col, leadSelectedIndex, col);
            } else {
                // fix for RT-34407
                int adjust = leadIndex < leadSelectedIndex ? 1 : -1;
                sm.selectRange(leadIndex, leadSelectedIndex + adjust);
            }
        }
        selectionChanging = false;
    }

    protected void selectAllPageDown() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        final TableColumnBase col = sm.isCellSelectionEnabled() ? getFocusedCell().getTableColumn() : null;
        if (isShiftDown) {
            leadIndex = getAnchor() == null ? leadIndex : getAnchor().getRow();
            setAnchor(leadIndex, col);
        }

        int leadSelectedIndex = onScrollPageDown.call(false);

        selectionChanging = true;
        if (sm.getSelectionMode() == null || sm.getSelectionMode() == SelectionMode.SINGLE) {
            if (sm.isCellSelectionEnabled()) {
                sm.select(leadSelectedIndex, col);
            } else {
                sm.select(leadSelectedIndex);
            }
        } else {
            sm.clearSelection();

            if (sm.isCellSelectionEnabled()) {
                sm.selectRange(leadIndex, col, leadSelectedIndex, col);
            } else {
                // fix for RT-34407
                int adjust = leadIndex < leadSelectedIndex ? 1 : -1;
                sm.selectRange(leadIndex, leadSelectedIndex + adjust);
            }
        }
        selectionChanging = false;
    }

    protected void toggleFocusOwnerSelection() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TablePositionBase focusedCell = getFocusedCell();

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
    protected void moveToLeftMostColumn() {
        // Functionality as described in RT-12752
        if (onMoveToLeftMostColumn != null) onMoveToLeftMostColumn.run();

        TableSelectionModel sm = getSelectionModel();
        if (sm == null || ! sm.isCellSelectionEnabled()) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TablePosition focusedCell = fm.getFocusedCell();

        TableColumn endColumn = getNode().getVisibleLeafColumn(0);
        sm.clearAndSelect(focusedCell.getRow(), endColumn);
    }

    protected void moveToRightMostColumn() {
        // Functionality as described in RT-12752
        if (onMoveToRightMostColumn != null) onMoveToRightMostColumn.run();

        TableSelectionModel sm = getSelectionModel();
        if (sm == null || ! sm.isCellSelectionEnabled()) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TablePosition focusedCell = fm.getFocusedCell();

        TableColumn endColumn = getNode().getVisibleLeafColumn(getNode().getVisibleLeafColumns().size() - 1);
        sm.clearAndSelect(focusedCell.getRow(), endColumn);
    }
     */


    /**************************************************************************
     * Discontinuous Selection                                                *
     *************************************************************************/

    protected void discontinuousSelectPreviousRow() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        if (sm.getSelectionMode() != SelectionMode.MULTIPLE) {
            selectPreviousRow();
            return;
        }

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        final int newFocusIndex = focusIndex - 1;
        if (newFocusIndex < 0) return;

        int startIndex = focusIndex;
        final TableColumnBase col = sm.isCellSelectionEnabled() ? getFocusedCell().getTableColumn() : null;
        if (isShiftDown) {
            startIndex = getAnchor() == null ? focusIndex : getAnchor().getRow();
        }

        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(newFocusIndex, startIndex + 1);
            fm.focus(newFocusIndex);
        } else {
            for (int i = newFocusIndex; i < startIndex + 1; i++) {
                sm.select(i, col);
            }
            fm.focus(newFocusIndex, col);
        }

        if (onFocusPreviousRow != null) onFocusPreviousRow.run();
    }

    protected void discontinuousSelectNextRow() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        if (sm.getSelectionMode() != SelectionMode.MULTIPLE) {
            selectNextRow();
            return;
        }

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        final int newFocusIndex = focusIndex + 1;
        if (newFocusIndex >= getItemCount()) return;

        int startIndex = focusIndex;
        final TableColumnBase col = sm.isCellSelectionEnabled() ? getFocusedCell().getTableColumn() : null;
        if (isShiftDown) {
            startIndex = getAnchor() == null ? focusIndex : getAnchor().getRow();
        }

        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(startIndex, newFocusIndex + 1);
            fm.focus(newFocusIndex);
        } else {
            for (int i = startIndex; i < newFocusIndex + 1; i++) {
                sm.select(i, col);
            }
            fm.focus(newFocusIndex, col);
        }

        if (onFocusNextRow != null) onFocusNextRow.run();
    }

    protected void discontinuousSelectPreviousColumn() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null || ! sm.isCellSelectionEnabled()) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TableColumnBase tc = getColumn(getFocusedCell().getTableColumn(), -1);
        sm.select(fm.getFocusedIndex(), tc);
    }

    protected void discontinuousSelectNextColumn() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null || ! sm.isCellSelectionEnabled()) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        TableColumnBase tc = getColumn(getFocusedCell().getTableColumn(), 1);
        sm.select(fm.getFocusedIndex(), tc);
    }

    protected void discontinuousSelectPageUp() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        int anchor = hasAnchor() ? getAnchor().getRow() : fm.getFocusedIndex();
        int leadSelectedIndex = onScrollPageUp.call(false);

        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(anchor, leadSelectedIndex - 1);
        }
    }

    protected void discontinuousSelectPageDown() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        int anchor = hasAnchor() ? getAnchor().getRow() : fm.getFocusedIndex();
        int leadSelectedIndex = onScrollPageDown.call(false);

        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(anchor, leadSelectedIndex + 1);
        }
    }

    protected void discontinuousSelectAllToFirstRow() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex();

        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(0, index);
            fm.focus(0);
        } else {
            for (int i = 0; i < index; i++) {
                sm.select(i, getFocusedCell().getTableColumn());
            }
            fm.focus(0, getFocusedCell().getTableColumn());
        }

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    protected void discontinuousSelectAllToLastRow() {
        TableSelectionModel sm = getSelectionModel();
        if (sm == null) return;

        TableFocusModel fm = getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex() + 1;

        if (! sm.isCellSelectionEnabled()) {
            sm.selectRange(index, getItemCount());
        } else {
            for (int i = index; i < getItemCount(); i++) {
                sm.select(i, getFocusedCell().getTableColumn());
            }
        }

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private EventHandler<KeyEvent> focusTraverseLeft() {
        return FocusTraversalInputMap::traverseLeft;
    }

    private EventHandler<KeyEvent> focusTraverseRight() {
        return FocusTraversalInputMap::traverseRight;
    }
}
