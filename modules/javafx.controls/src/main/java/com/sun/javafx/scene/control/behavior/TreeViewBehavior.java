/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.control.inputmap.KeyBinding;

import static javafx.scene.input.KeyCode.*;
import static com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;

public class TreeViewBehavior<T> extends BehaviorBase<TreeView<T>> {

    private final InputMap<TreeView<T>> treeViewInputMap;

    private final EventHandler<KeyEvent> keyEventListener = e -> {
        if (!e.isConsumed()) {
            // RT-12751: we want to keep an eye on the user holding down the shift key,
            // so that we know when they enter/leave multiple selection mode. This
            // changes what happens when certain key combinations are pressed.
            isShiftDown = e.getEventType() == KeyEvent.KEY_PRESSED && e.isShiftDown();
            isShortcutDown = e.getEventType() == KeyEvent.KEY_PRESSED && e.isShortcutDown();
        }
    };

    /**************************************************************************
     *                         State and Functions                            *
     *************************************************************************/

    private boolean isShiftDown = false;
    private boolean isShortcutDown = false;

    // Support for RT-13826:
    // set when focus is moved by keyboard to allow for proper selection positions
//    private int selectPos = -1;

    private Callback<Boolean, Integer> onScrollPageUp;
    public void setOnScrollPageUp(Callback<Boolean, Integer> c) { onScrollPageUp = c; }

    private Callback<Boolean, Integer> onScrollPageDown;
    public void setOnScrollPageDown(Callback<Boolean, Integer> c) { onScrollPageDown = c; }

    private Runnable onSelectPreviousRow;
    public void setOnSelectPreviousRow(Runnable r) { onSelectPreviousRow = r; }

    private Runnable onSelectNextRow;
    public void setOnSelectNextRow(Runnable r) { onSelectNextRow = r; }

    private Runnable onMoveToFirstCell;
    public void setOnMoveToFirstCell(Runnable r) { onMoveToFirstCell = r; }

    private Runnable onMoveToLastCell;
    public void setOnMoveToLastCell(Runnable r) { onMoveToLastCell = r; }

    private Runnable onFocusPreviousRow;
    public void setOnFocusPreviousRow(Runnable r) { onFocusPreviousRow = r; }

    private Runnable onFocusNextRow;
    public void setOnFocusNextRow(Runnable r) { onFocusNextRow = r; }

    private boolean selectionChanging = false;

    private final ListChangeListener<Integer> selectedIndicesListener = c -> {
        int newAnchor = getAnchor();

        while (c.next()) {
            if (c.wasReplaced()) {
                if (TreeCellBehavior.hasDefaultAnchor(getNode())) {
                    TreeCellBehavior.removeAnchor(getNode());
                    continue;
                }
            }

            final int shift = c.wasPermutated() ? c.getTo() - c.getFrom() : 0;

            MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();

            // there are no selected items, so lets clear out the anchor
            if (! selectionChanging) {
                if (sm.isEmpty()) {
                    newAnchor = -1;
                } else if (hasAnchor() && ! sm.isSelected(getAnchor() + shift)) {
                    newAnchor = -1;
                }
            }

            // we care about the situation where the selection changes, and there is no anchor. In this
            // case, we set a new anchor to be the selected index
            if (newAnchor == -1) {
                int addedSize = c.getAddedSize();
                newAnchor = addedSize > 0 ? c.getAddedSubList().get(addedSize - 1) : newAnchor;
            }
        }

        if (newAnchor > -1) {
            setAnchor(newAnchor);
        }
    };

    private final ChangeListener<MultipleSelectionModel<TreeItem<T>>> selectionModelListener =
            new ChangeListener<MultipleSelectionModel<TreeItem<T>>>() {
        @Override public void changed(ObservableValue<? extends MultipleSelectionModel<TreeItem<T>>> observable,
                    MultipleSelectionModel<TreeItem<T>> oldValue,
                    MultipleSelectionModel<TreeItem<T>> newValue) {
            if (oldValue != null) {
                oldValue.getSelectedIndices().removeListener(weakSelectedIndicesListener);
            }
            if (newValue != null) {
                newValue.getSelectedIndices().addListener(weakSelectedIndicesListener);
            }
        }
    };

    private final WeakListChangeListener<Integer> weakSelectedIndicesListener =
            new WeakListChangeListener<>(selectedIndicesListener);
    private final WeakChangeListener<MultipleSelectionModel<TreeItem<T>>> weakSelectionModelListener =
            new WeakChangeListener<>(selectionModelListener);

    public TreeViewBehavior(TreeView<T> control) {
        super(control);

        // create a map for treeView-specific mappings
        treeViewInputMap = createInputMap();

//        // add focus traversal mappings
//        addDefaultMapping(treeViewInputMap, FocusTraversalInputMap.getFocusTraversalMappings());
        addDefaultMapping(treeViewInputMap,
            new KeyMapping(HOME, e -> selectFirstRow()),
            new KeyMapping(END, e -> selectLastRow()),
            new KeyMapping(new KeyBinding(HOME).shift(), e -> selectAllToFirstRow()),
            new KeyMapping(new KeyBinding(END).shift(), e -> selectAllToLastRow()),
            new KeyMapping(new KeyBinding(PAGE_UP).shift(), e -> selectAllPageUp()),
            new KeyMapping(new KeyBinding(PAGE_DOWN).shift(), e -> selectAllPageDown()),

            new KeyMapping(new KeyBinding(SPACE).shift(), e -> selectAllToFocus(false)),
            new KeyMapping(new KeyBinding(SPACE).shortcut().shift(), e -> selectAllToFocus(true)),

            new KeyMapping(new KeyBinding(HOME).shortcut(), e -> focusFirstRow()),
            new KeyMapping(new KeyBinding(END).shortcut(), e -> focusLastRow()),

            new KeyMapping(PAGE_UP, e -> scrollUp()),
            new KeyMapping(PAGE_DOWN, e -> scrollDown()),

            new KeyMapping(SPACE, e -> toggleFocusOwnerSelection()),

            new KeyMapping(new KeyBinding(A).shortcut(), e -> selectAll()),
            new KeyMapping(new KeyBinding(PAGE_UP).shortcut(), e -> focusPageUp()),
            new KeyMapping(new KeyBinding(PAGE_DOWN).shortcut(), e -> focusPageDown()),
            new KeyMapping(new KeyBinding(UP).shortcut(), e -> focusPreviousRow()),
            new KeyMapping(new KeyBinding(DOWN).shortcut(), e -> focusNextRow()),
            new KeyMapping(new KeyBinding(UP).shortcut().shift(), e -> discontinuousSelectPreviousRow()),
            new KeyMapping(new KeyBinding(DOWN).shortcut().shift(), e -> discontinuousSelectNextRow()),
            new KeyMapping(new KeyBinding(PAGE_UP).shortcut().shift(), e -> discontinuousSelectPageUp()),
            new KeyMapping(new KeyBinding(PAGE_DOWN).shortcut().shift(), e -> discontinuousSelectPageDown()),
            new KeyMapping(new KeyBinding(HOME).shortcut().shift(), e -> discontinuousSelectAllToFirstRow()),
            new KeyMapping(new KeyBinding(END).shortcut().shift(), e -> discontinuousSelectAllToLastRow()),

            // these should be read as 'if RTL, use the first method, otherwise use the second'
            new KeyMapping(LEFT, e -> rtl(control, this::expandRow, this::collapseRow)),
            new KeyMapping(KP_LEFT, e -> rtl(control, this::expandRow, this::collapseRow)),
            new KeyMapping(RIGHT, e -> rtl(control, this::collapseRow, this::expandRow)),
            new KeyMapping(KP_RIGHT, e -> rtl(control, this::collapseRow, this::expandRow)),

            new KeyMapping(MULTIPLY, e -> expandAll()),
            new KeyMapping(ADD, e -> expandRow()),
            new KeyMapping(SUBTRACT, e -> collapseRow()),

            new KeyMapping(UP, e -> selectPreviousRow()),
            new KeyMapping(KP_UP, e -> selectPreviousRow()),
            new KeyMapping(DOWN, e -> selectNextRow()),
            new KeyMapping(KP_DOWN, e -> selectNextRow()),

            new KeyMapping(new KeyBinding(UP).shift(), e -> alsoSelectPreviousRow()),
            new KeyMapping(new KeyBinding(KP_UP).shift(), e -> alsoSelectPreviousRow()),
            new KeyMapping(new KeyBinding(DOWN).shift(), e -> alsoSelectNextRow()),
            new KeyMapping(new KeyBinding(KP_DOWN).shift(), e -> alsoSelectNextRow()),

            new KeyMapping(ENTER, e -> edit()),
            new KeyMapping(F2, e -> edit()),
            new KeyMapping(ESCAPE, e -> cancelEdit()),

            new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed)
        );

        // create OS-specific child mappings
        // --- mac OS
        InputMap<TreeView<T>> macInputMap = new InputMap<>(control);
        macInputMap.setInterceptor(event -> !PlatformUtil.isMac());
        addDefaultMapping(macInputMap, new KeyMapping(new KeyBinding(SPACE).shortcut().ctrl(), e -> toggleFocusOwnerSelection()));
        addDefaultChildMap(treeViewInputMap, macInputMap);

        // --- all other platforms
        InputMap<TreeView<T>> otherOsInputMap = new InputMap<>(control);
        otherOsInputMap.setInterceptor(event -> PlatformUtil.isMac());
        addDefaultMapping(otherOsInputMap, new KeyMapping(new KeyBinding(SPACE).ctrl(), e -> toggleFocusOwnerSelection()));
        addDefaultChildMap(treeViewInputMap, otherOsInputMap);

        // set up other listeners
        // We make this an event _filter_ so that we can determine the state
        // of the shift key before the event handlers get a shot at the event.
        control.addEventFilter(KeyEvent.ANY, keyEventListener);

        // Fix for RT-16565
        control.selectionModelProperty().addListener(weakSelectionModelListener);
        if (control.getSelectionModel() != null) {
            control.getSelectionModel().getSelectedIndices().addListener(weakSelectedIndicesListener);
        }
    }

    @Override public InputMap<TreeView<T>> getInputMap() {
        return treeViewInputMap;
    }

    @Override public void dispose() {
        getNode().selectionModelProperty().removeListener(weakSelectionModelListener);
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm != null) {
            sm.getSelectedIndices().removeListener(weakSelectedIndicesListener);
        }
        getNode().removeEventFilter(KeyEvent.ANY, keyEventListener);
        TreeCellBehavior.removeAnchor(getNode());
        super.dispose();
    }

    private void setAnchor(int anchor) {
        TreeCellBehavior.setAnchor(getNode(), anchor < 0 ? null : anchor, false);
    }

    private int getAnchor() {
        return TreeCellBehavior.getAnchor(getNode(), getNode().getFocusModel().getFocusedIndex());
    }

    private boolean hasAnchor() {
        return TreeCellBehavior.hasNonDefaultAnchor(getNode());
    }

    public void mousePressed(MouseEvent e) {
        if (! e.isShiftDown()) {
            int index = getNode().getSelectionModel().getSelectedIndex();
            setAnchor(index);
        }

        if (! getNode().isFocused() && getNode().isFocusTraversable()) {
            getNode().requestFocus();
        }
    }

    private void clearSelection() {
        getNode().getSelectionModel().clearSelection();
        //select(null);
    }

    private void scrollUp() {
        int newSelectedIndex = -1;
        if (onScrollPageUp != null) {
            newSelectedIndex = onScrollPageUp.call(false);
        }
        if (newSelectedIndex == -1) return;

        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;
        sm.clearAndSelect(newSelectedIndex);
    }

    private void scrollDown() {
        int newSelectedIndex = -1;
        if (onScrollPageDown != null) {
            newSelectedIndex = onScrollPageDown.call(false);
        }
        if (newSelectedIndex == -1) return;

        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;
        sm.clearAndSelect(newSelectedIndex);
    }

    private void focusFirstRow() {
        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;
        fm.focus(0);

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    private void focusLastRow() {
        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;
        fm.focus(getNode().getExpandedItemCount() - 1);

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private void focusPreviousRow() {
        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        fm.focusPrevious();

        if (! isShortcutDown || getAnchor() == -1) {
            setAnchor(fm.getFocusedIndex());
        }

        if (onFocusPreviousRow != null) onFocusPreviousRow.run();
    }

    private void focusNextRow() {
        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        fm.focusNext();

        if (! isShortcutDown || getAnchor() == -1) {
            setAnchor(fm.getFocusedIndex());
        }

        if (onFocusNextRow != null) onFocusNextRow.run();
    }

    private void focusPageUp() {
        int newFocusIndex = onScrollPageUp.call(true);

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;
        fm.focus(newFocusIndex);
    }

    private void focusPageDown() {
        int newFocusIndex = onScrollPageDown.call(true);

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;
        fm.focus(newFocusIndex);
    }

    private void alsoSelectPreviousRow() {
        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        if (isShiftDown && getAnchor() != -1) {
            int newRow = fm.getFocusedIndex() - 1;
            if (newRow < 0) return;

            int anchor = getAnchor();

            if (! hasAnchor()) {
                setAnchor(fm.getFocusedIndex());
            }

            if (sm.getSelectedIndices().size() > 1) {
                clearSelectionOutsideRange(anchor, newRow);
            }

            if (anchor > newRow) {
                sm.selectRange(anchor, newRow - 1);
            } else {
                sm.selectRange(anchor, newRow + 1);
            }
        } else {
            sm.selectPrevious();
        }

        onSelectPreviousRow.run();
    }

    private void alsoSelectNextRow() {
        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        if (isShiftDown && getAnchor() != -1) {
            int newRow = fm.getFocusedIndex() + 1;
            int anchor = getAnchor();

            if (! hasAnchor()) {
                setAnchor(fm.getFocusedIndex());
            }

            if (sm.getSelectedIndices().size() > 1) {
                clearSelectionOutsideRange(anchor, newRow);
            }

            if (anchor > newRow) {
                sm.selectRange(anchor, newRow - 1);
            } else {
                sm.selectRange(anchor, newRow + 1);
            }
        } else {
            sm.selectNext();
        }

        onSelectNextRow.run();
    }

    private void clearSelectionOutsideRange(int start, int end) {
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        int min = Math.min(start, end);
        int max = Math.max(start, end);

        List<Integer> indices = new ArrayList<Integer>(sm.getSelectedIndices());

        selectionChanging = true;
        for (int i = 0; i < indices.size(); i++) {
            int index = indices.get(i);
            if (index < min || index > max) {
                sm.clearSelection(index);
            }
        }
        selectionChanging = false;
    }

    private void selectPreviousRow() {
        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        if (focusIndex <= 0) {
            return;
        }

        setAnchor(focusIndex - 1);
        getNode().getSelectionModel().clearAndSelect(focusIndex - 1);
        onSelectPreviousRow.run();
    }

    private void selectNextRow() {
        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        if (focusIndex == getNode().getExpandedItemCount() - 1) {
            return;
        }

        setAnchor(focusIndex + 1);
        getNode().getSelectionModel().clearAndSelect(focusIndex + 1);
        onSelectNextRow.run();
    }

    private void selectFirstRow() {
        if (getNode().getExpandedItemCount() > 0) {
            getNode().getSelectionModel().clearAndSelect(0);
            if (onMoveToFirstCell != null) onMoveToFirstCell.run();
        }
    }

    private void selectLastRow() {
        getNode().getSelectionModel().clearAndSelect(getNode().getExpandedItemCount() - 1);
        onMoveToLastCell.run();
    }

    private void selectAllToFirstRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();

        if (isShiftDown) {
            leadIndex = hasAnchor() ? getAnchor() : leadIndex;
        }

        sm.clearSelection();
        sm.selectRange(leadIndex, -1);

        // RT-18413: Focus must go to first row
        fm.focus(0);

        if (isShiftDown) {
            setAnchor(leadIndex);
        }

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    private void selectAllToLastRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();

        if (isShiftDown) {
            leadIndex = hasAnchor() ? getAnchor() : leadIndex;
        }

        sm.clearSelection();
        sm.selectRange(leadIndex, getNode().getExpandedItemCount());

        if (isShiftDown) {
            setAnchor(leadIndex);
        }

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private void selectAll() {
        getNode().getSelectionModel().selectAll();
    }

    private void selectAllPageUp() {
        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        if (isShiftDown) {
            leadIndex = getAnchor() == -1 ? leadIndex : getAnchor();
            setAnchor(leadIndex);
        }

        int leadSelectedIndex = onScrollPageUp.call(false);

        // fix for RT-34407
        int adjust = leadIndex < leadSelectedIndex ? 1 : -1;

        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        selectionChanging = true;
        if (sm.getSelectionMode() == SelectionMode.SINGLE) {
            sm.select(leadSelectedIndex);
        } else {
            sm.clearSelection();
            sm.selectRange(leadIndex, leadSelectedIndex + adjust);
        }
        selectionChanging = false;
    }

    private void selectAllPageDown() {
        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        if (isShiftDown) {
            leadIndex = getAnchor() == -1 ? leadIndex : getAnchor();
            setAnchor(leadIndex);
        }

        int leadSelectedIndex = onScrollPageDown.call(false);

        // fix for RT-34407
        int adjust = leadIndex < leadSelectedIndex ? 1 : -1;

        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        selectionChanging = true;
        if (sm.getSelectionMode() == SelectionMode.SINGLE) {
            sm.select(leadSelectedIndex);
        } else {
            sm.clearSelection();
            sm.selectRange(leadIndex, leadSelectedIndex + adjust);
        }
        selectionChanging = false;
    }

    private void selectAllToFocus(boolean setAnchorToFocusIndex) {
        // Fix for RT-31241
        final TreeView<T> treeView = getNode();
        if (treeView.getEditingItem() != null) return;

        MultipleSelectionModel<TreeItem<T>> sm = treeView.getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = treeView.getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        int anchor = getAnchor();

        sm.clearSelection();
        int startPos = anchor;
        int endPos = anchor > focusIndex ? focusIndex - 1 : focusIndex + 1;
        sm.selectRange(startPos, endPos);
        setAnchor(setAnchorToFocusIndex ? focusIndex : anchor);
    }

    private void expandRow() {
        Callback<TreeItem<T>, Integer> getIndex = p -> getNode().getRow(p);
        TreeViewBehavior.expandRow(getNode().getSelectionModel(), getIndex);
    }

    private void expandAll() {
        TreeViewBehavior.expandAll(getNode().getRoot());
    }

    private void collapseRow() {
        TreeView<T> control = getNode();
        TreeViewBehavior.collapseRow(control.getSelectionModel(), control.getRoot(), control.isShowRoot());
    }

    static <T> void expandRow(final MultipleSelectionModel<TreeItem<T>> sm, Callback<TreeItem<T>, Integer> getIndex) {
        if (sm == null) return;

        TreeItem<T> treeItem = sm.getSelectedItem();
        if (treeItem == null || treeItem.isLeaf()) return;

        if (treeItem.isExpanded()) {
            // move selection to the first child (RT-17978)
            List<TreeItem<T>> children = treeItem.getChildren();
            if (! children.isEmpty()) {
                sm.clearAndSelect(getIndex.call(children.get(0)));
            }
        } else {
            treeItem.setExpanded(true);
        }
    }

    static <T> void expandAll(final TreeItem<T> root) {
        if (root == null) return;

        root.setExpanded(true);
        expandChildren(root);
    }

    private static <T> void expandChildren(TreeItem<T> node) {
        if (node == null) return;
        List<TreeItem<T>> children = node.getChildren();
        if (children == null) return;

        for (int i = 0; i < children.size(); i++) {
            TreeItem<T> child = children.get(i);
            if (child == null || child.isLeaf()) continue;

            child.setExpanded(true);
            expandChildren(child);
        }
    }

    static <T> void collapseRow(final MultipleSelectionModel<TreeItem<T>> sm, final TreeItem<T> root, final boolean isShowRoot) {
        if (sm == null) return;

        TreeItem<T> selectedItem = sm.getSelectedItem();
        if (selectedItem == null) return;
        if (root == null) return;

        // Fix for RT-17233 where we could hide all items in a tree with no visible
        // root by pressing the left-arrow key too many times.
        // Check for isLeaf() added to resolve JDK-8152106.
        if (!isShowRoot
                && (!selectedItem.isExpanded() || selectedItem.isLeaf())
                && root.equals(selectedItem.getParent())) {
            return;
        }

        // Fix for RT-17833 where the selection highlight could disappear unexpectedly from
        // the root node in certain circumstances
        if (root.equals(selectedItem) && (! root.isExpanded() || root.getChildren().isEmpty())) {
            return;
        }

        // If we're on a leaf or the branch is not expanded, move up to the parent,
        // otherwise collapse the branch.
        if (selectedItem.isLeaf() || ! selectedItem.isExpanded()) {
            sm.clearSelection();
            sm.select(selectedItem.getParent());
        } else {
            selectedItem.setExpanded(false);
        }
    }

    private void cancelEdit() {
        getNode().edit(null);
    }

    private void edit() {
        TreeItem<T> treeItem = getNode().getSelectionModel().getSelectedItem();
        if (treeItem == null) return;

        getNode().edit(treeItem);
    }

    private void toggleFocusOwnerSelection() {
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int focusedIndex = fm.getFocusedIndex();

        if (sm.isSelected(focusedIndex)) {
            sm.clearSelection(focusedIndex);
            fm.focus(focusedIndex);
        } else {
            sm.select(focusedIndex);
        }

        setAnchor(focusedIndex);
    }

    /**************************************************************************
     * Discontinuous Selection                                                *
     *************************************************************************/

    private void discontinuousSelectPreviousRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        if (sm.getSelectionMode() != SelectionMode.MULTIPLE) {
            selectPreviousRow();
            return;
        }

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        final int newFocusIndex = focusIndex - 1;
        if (newFocusIndex < 0) return;

        int startIndex = focusIndex;
        if (isShiftDown) {
            startIndex = getAnchor() == -1 ? focusIndex : getAnchor();
        }

        sm.selectRange(newFocusIndex, startIndex + 1);
        fm.focus(newFocusIndex);

        if (onFocusPreviousRow != null) onFocusPreviousRow.run();
    }

    private void discontinuousSelectNextRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        if (sm.getSelectionMode() != SelectionMode.MULTIPLE) {
            selectNextRow();
            return;
        }

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        final int newFocusIndex = focusIndex + 1;
        if (newFocusIndex >= getNode().getExpandedItemCount()) return;

        int startIndex = focusIndex;
        if (isShiftDown) {
            startIndex = getAnchor() == -1 ? focusIndex : getAnchor();
        }

        sm.selectRange(startIndex, newFocusIndex + 1);
        fm.focus(newFocusIndex);

        if (onFocusNextRow != null) onFocusNextRow.run();
    }

    private void discontinuousSelectPageUp() {
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int anchor = getAnchor();
        int leadSelectedIndex = onScrollPageUp.call(false);
        sm.selectRange(anchor, leadSelectedIndex - 1);
    }

    private void discontinuousSelectPageDown() {
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int anchor = getAnchor();
        int leadSelectedIndex = onScrollPageDown.call(false);
        sm.selectRange(anchor, leadSelectedIndex + 1);
    }

    private void discontinuousSelectAllToFirstRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex();
        sm.selectRange(0, index);
        fm.focus(0);

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    private void discontinuousSelectAllToLastRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = getNode().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex() + 1;
        sm.selectRange(index, getNode().getExpandedItemCount());

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }
}
