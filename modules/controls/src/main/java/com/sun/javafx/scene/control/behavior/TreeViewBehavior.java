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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.control.FocusModel;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.PlatformUtil;
import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyCode.ADD;
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
import static javafx.scene.input.KeyCode.MULTIPLY;
import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.SUBTRACT;
import static javafx.scene.input.KeyCode.UP;

public class TreeViewBehavior<T> extends BehaviorBase<TreeView<T>> {

    /**************************************************************************
     *                          Setup KeyBindings                             *
     *************************************************************************/
    protected static final List<KeyBinding> TREE_VIEW_BINDINGS = new ArrayList<KeyBinding>();

    static {
        TREE_VIEW_BINDINGS.add(new KeyBinding(HOME, "SelectFirstRow"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(END, "SelectLastRow"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(HOME, "SelectAllToFirstRow").shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(END, "SelectAllToLastRow").shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "SelectAllPageUp").shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "SelectAllPageDown").shift());
        
        TREE_VIEW_BINDINGS.add(new KeyBinding(SPACE, "SelectAllToFocus").shift());
        
        TREE_VIEW_BINDINGS.add(new KeyBinding(HOME, "FocusFirstRow").shortcut());
        TREE_VIEW_BINDINGS.add(new KeyBinding(END, "FocusLastRow").shortcut());

        TREE_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "ScrollUp"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "ScrollDown"));
        
        if (PlatformUtil.isMac()) {
            TREE_VIEW_BINDINGS.add(new KeyBinding(SPACE, "toggleFocusOwnerSelection").ctrl().shortcut());
        } else {
            TREE_VIEW_BINDINGS.add(new KeyBinding(SPACE, "toggleFocusOwnerSelection").ctrl());
        }

        TREE_VIEW_BINDINGS.add(new KeyBinding(A, "SelectAll").shortcut());
        TREE_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "FocusPageUp").shortcut());
        TREE_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "FocusPageDown").shortcut());
        TREE_VIEW_BINDINGS.add(new KeyBinding(UP, "FocusPreviousRow").shortcut());
        TREE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "FocusNextRow").shortcut());
        TREE_VIEW_BINDINGS.add(new KeyBinding(UP, "DiscontinuousSelectPreviousRow").shortcut().shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "DiscontinuousSelectNextRow").shortcut().shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "DiscontinuousSelectPageUp").shortcut().shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "DiscontinuousSelectPageDown").shortcut().shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(HOME, "DiscontinuousSelectAllToFirstRow").shortcut().shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(END, "DiscontinuousSelectAllToLastRow").shortcut().shift());

        TREE_VIEW_BINDINGS.add(new KeyBinding(LEFT, "CollapseRow"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(KP_LEFT, "CollapseRow"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(RIGHT, "ExpandRow"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(KP_RIGHT, "ExpandRow"));
        
        TREE_VIEW_BINDINGS.add(new KeyBinding(MULTIPLY, "ExpandAll"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(ADD, "ExpandRow"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(SUBTRACT, "CollapseRow"));

        TREE_VIEW_BINDINGS.add(new KeyBinding(UP, "SelectPreviousRow"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(KP_UP, "SelectPreviousRow"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "SelectNextRow"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(KP_DOWN, "SelectNextRow"));

        TREE_VIEW_BINDINGS.add(new KeyBinding(UP, "AlsoSelectPreviousRow").shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(KP_UP, "AlsoSelectPreviousRow").shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(DOWN, "AlsoSelectNextRow").shift());
        TREE_VIEW_BINDINGS.add(new KeyBinding(KP_DOWN, "AlsoSelectNextRow").shift());

        TREE_VIEW_BINDINGS.add(new KeyBinding(ENTER, "Edit"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(F2, "Edit"));
        TREE_VIEW_BINDINGS.add(new KeyBinding(ESCAPE, "CancelEdit"));
    }

    @Override protected void callAction(String name) {
        if ("SelectPreviousRow".equals(name)) selectPreviousRow();
        else if ("SelectNextRow".equals(name)) selectNextRow();
        else if ("SelectFirstRow".equals(name)) selectFirstRow();
        else if ("SelectLastRow".equals(name)) selectLastRow();
        else if ("SelectAllPageUp".equals(name)) selectAllPageUp();
        else if ("SelectAllPageDown".equals(name)) selectAllPageDown();
        else if ("SelectAllToFirstRow".equals(name)) selectAllToFirstRow();
        else if ("SelectAllToLastRow".equals(name)) selectAllToLastRow();
        else if ("AlsoSelectNextRow".equals(name)) alsoSelectNextRow();
        else if ("AlsoSelectPreviousRow".equals(name)) alsoSelectPreviousRow();
        else if ("ClearSelection".equals(name)) clearSelection();
        else if("SelectAll".equals(name)) selectAll();
        else if ("ScrollUp".equals(name)) scrollUp();
        else if ("ScrollDown".equals(name)) scrollDown();
        else if ("ExpandRow".equals(name)) expandRow();
        else if ("CollapseRow".equals(name)) collapseRow();
        else if ("ExpandAll".equals(name)) expandAll();
//        else if ("ExpandOrCollapseRow".equals(name)) expandOrCollapseRow();
        else if ("Edit".equals(name)) edit();
        else if ("CancelEdit".equals(name)) cancelEdit();
        else if ("FocusFirstRow".equals(name)) focusFirstRow();
        else if ("FocusLastRow".equals(name)) focusLastRow();
        else if ("toggleFocusOwnerSelection".equals(name)) toggleFocusOwnerSelection();
        else if ("SelectAllToFocus".equals(name)) selectAllToFocus();
        else if ("FocusPageUp".equals(name)) focusPageUp();
        else if ("FocusPageDown".equals(name)) focusPageDown();
        else if ("FocusPreviousRow".equals(name)) focusPreviousRow();
        else if ("FocusNextRow".equals(name)) focusNextRow();
        else if ("DiscontinuousSelectNextRow".equals(name)) discontinuousSelectNextRow();
        else if ("DiscontinuousSelectPreviousRow".equals(name)) discontinuousSelectPreviousRow();
        else if ("DiscontinuousSelectPageUp".equals(name)) discontinuousSelectPageUp();
        else if ("DiscontinuousSelectPageDown".equals(name)) discontinuousSelectPageDown();
        else if ("DiscontinuousSelectAllToLastRow".equals(name)) discontinuousSelectAllToLastRow();
        else if ("DiscontinuousSelectAllToFirstRow".equals(name)) discontinuousSelectAllToFirstRow();
        else super.callAction(name);
    }

    @Override protected List<KeyBinding> createKeyBindings() {
        return TREE_VIEW_BINDINGS;
    }
    
    @Override protected void callActionForEvent(KeyEvent e) {
        // RT-12751: we want to keep an eye on the user holding down the shift key, 
        // so that we know when they enter/leave multiple selection mode. This
        // changes what happens when certain key combinations are pressed.
        isShiftDown = e.getEventType() == KeyEvent.KEY_PRESSED && e.isShiftDown();
        isShortcutDown = e.getEventType() == KeyEvent.KEY_PRESSED && e.isShortcutDown();
        
        super.callActionForEvent(e);
    }

    /**************************************************************************
     *                         State and Functions                            *
     *************************************************************************/

    private boolean isShiftDown = false;
    private boolean isShortcutDown = false;
    
    // Support for RT-13826:
    // set when focus is moved by keyboard to allow for proper selection positions
//    private int selectPos = -1;
    
    private Callback<Integer, Integer> onScrollPageUp;
    public void setOnScrollPageUp(Callback<Integer, Integer> c) { onScrollPageUp = c; }

    private Callback<Integer, Integer> onScrollPageDown;
    public void setOnScrollPageDown(Callback<Integer, Integer> c) { onScrollPageDown = c; }

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
    
    private final ListChangeListener<Integer> selectedIndicesListener = new ListChangeListener<Integer>() {
        @Override public void onChanged(ListChangeListener.Change<? extends Integer> c) {
            while (c.next()) {
                MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
                
                // there are no selected items, so lets clear out the anchor
                if (! selectionChanging) {
                    if (sm.isEmpty()) {
                        setAnchor(-1);
                    } else if (! sm.isSelected(getAnchor())) {
                        setAnchor(-1);
                    }
                }

                int addedSize = c.getAddedSize();
                if (addedSize > 0 && ! hasAnchor()) {
                    List<? extends Integer> addedSubList = c.getAddedSubList();
                    int index = addedSubList.get(addedSize - 1);
                    setAnchor(index);
                }
            }
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
            new WeakListChangeListener<Integer>(selectedIndicesListener);
    private final WeakChangeListener<MultipleSelectionModel<TreeItem<T>>> weakSelectionModelListener =
            new WeakChangeListener<MultipleSelectionModel<TreeItem<T>>>(selectionModelListener);

    public TreeViewBehavior(TreeView<T> control) {
        super(control);
        
        // Fix for RT-16565
        getControl().selectionModelProperty().addListener(weakSelectionModelListener);
        if (control.getSelectionModel() != null) {
            control.getSelectionModel().getSelectedIndices().addListener(weakSelectedIndicesListener);
        }
    }
    
    @Override public void dispose() {
        TreeCellBehavior.removeAnchor(getControl());
        super.dispose();
    }
    
    private void setAnchor(int anchor) {
        TreeCellBehavior.setAnchor(getControl(), anchor);
    }
    
    private int getAnchor() {
        return TreeCellBehavior.getAnchor(getControl());
    }
    
    private boolean hasAnchor() {
        return TreeCellBehavior.hasAnchor(getControl());
    }

    @Override public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        
        if (! e.isShiftDown()) {
            int index = getControl().getSelectionModel().getSelectedIndex();
            setAnchor(index);
        }
        
        if (! getControl().isFocused() && getControl().isFocusTraversable()) {
            getControl().requestFocus();
        }
    }

    private void clearSelection() {
        getControl().getSelectionModel().clearSelection();
        //select(null);
    }

    private void scrollUp() {
        int newSelectedIndex = -1;
        if (onScrollPageUp != null) {
            newSelectedIndex = onScrollPageUp.call(getAnchor());
        }
        if (newSelectedIndex == -1) return;
        
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        sm.clearAndSelect(newSelectedIndex);
    }

    private void scrollDown() {
        int newSelectedIndex = -1;
        if (onScrollPageDown != null) {
            newSelectedIndex = onScrollPageDown.call(getAnchor());
        }
        if (newSelectedIndex == -1) return;
        
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        sm.clearAndSelect(newSelectedIndex);
    }
    
    private void focusFirstRow() {
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        fm.focus(0);
        
        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }
    
    private void focusLastRow() {
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        fm.focus(getControl().getExpandedItemCount() - 1);
        
        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }
    
    private void focusPreviousRow() {
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        fm.focusPrevious();
        
        if (! isShortcutDown || getAnchor() == -1) {
            setAnchor(fm.getFocusedIndex());
        }
        
        if (onFocusPreviousRow != null) onFocusPreviousRow.run();
    }

    private void focusNextRow() {
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        fm.focusNext();
        
        if (! isShortcutDown || getAnchor() == -1) {
            setAnchor(fm.getFocusedIndex());
        }
        
        if (onFocusNextRow != null) onFocusNextRow.run();
    }
    
    private void focusPageUp() {
        int newFocusIndex = onScrollPageUp.call(getAnchor());
        
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        fm.focus(newFocusIndex);
    }
    
    private void focusPageDown() {
        int newFocusIndex = onScrollPageDown.call(getAnchor());
        
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        fm.focus(newFocusIndex);
    }

    private void alsoSelectPreviousRow() {
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
//        final int focusIndex = fm.getFocusedIndex();
        
        if (isShiftDown && getAnchor() != -1) {
            int newRow = fm.getFocusedIndex() - 1;
            int anchor = getAnchor();
            
            if (! hasAnchor()) {
                setAnchor(fm.getFocusedIndex());
            } 
            
            clearSelectionOutsideRange(anchor, newRow);

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
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        if (isShiftDown && getAnchor() != -1) {
            int newRow = fm.getFocusedIndex() + 1;
            int anchor = getAnchor();
            
            if (! hasAnchor()) {
                setAnchor(fm.getFocusedIndex());
            } 
            
            clearSelectionOutsideRange(anchor, newRow);

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
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
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

    private void selectPreviousRow() {
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        if (focusIndex <= 0) {
            return;
        }

        setAnchor(focusIndex - 1);
        getControl().getSelectionModel().clearAndSelect(focusIndex - 1);
        onSelectPreviousRow.run();
    }

    private void selectNextRow() {
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        if (focusIndex == getControl().getExpandedItemCount() - 1) {
            return;
        }

        setAnchor(focusIndex + 1);
        getControl().getSelectionModel().clearAndSelect(focusIndex + 1);
        onSelectNextRow.run();
    }

    private void selectFirstRow() {
        if (getControl().getExpandedItemCount() > 0) {
            getControl().getSelectionModel().clearAndSelect(0);
            if (onMoveToFirstCell != null) onMoveToFirstCell.run();
        }
    }

    private void selectLastRow() {
        getControl().getSelectionModel().clearAndSelect(getControl().getExpandedItemCount() - 1);
        onMoveToLastCell.run();
    }

    private void selectAllToFirstRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        int leadIndex = sm.getSelectedIndex();
        
        if (isShiftDown) {
            leadIndex = getAnchor() == -1 ? sm.getSelectedIndex() : getAnchor();
        }
        
        sm.clearSelection();
        sm.selectRange(0, leadIndex + 1);
        
        if (isShiftDown) {
            setAnchor(leadIndex);
        }

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    private void selectAllToLastRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;

        int leadIndex = sm.getSelectedIndex();
        
        if (isShiftDown) {
            leadIndex = getAnchor() == -1 ? sm.getSelectedIndex() : getAnchor();
        }
        
        sm.clearSelection();
        sm.selectRange(leadIndex, getControl().getExpandedItemCount() - 1);
        
        if (isShiftDown) {
            setAnchor(leadIndex);
        }

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private void selectAll() {
        getControl().getSelectionModel().selectAll();
    }
    
    private void selectAllPageUp() {
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        if (isShiftDown) {
            leadIndex = getAnchor() == -1 ? leadIndex : getAnchor();
            setAnchor(leadIndex);
        }
        
        int leadSelectedIndex = onScrollPageUp.call(getAnchor());
        
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        selectionChanging = true;
        sm.clearSelection();
        sm.selectRange(leadSelectedIndex, leadIndex + 1);
        selectionChanging = false;
    }
    
    private void selectAllPageDown() {
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        int leadIndex = fm.getFocusedIndex();
        if (isShiftDown) {
            leadIndex = getAnchor() == -1 ? leadIndex : getAnchor();
            setAnchor(leadIndex);
        }
        
        int leadSelectedIndex = onScrollPageDown.call(getAnchor());
        
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        selectionChanging = true;
        sm.clearSelection();
        sm.selectRange(leadIndex, leadSelectedIndex + 1);
        selectionChanging = false;
    }
    
    private void selectAllToFocus() {
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        int anchor = getAnchor();
        
        sm.clearSelection();
        int startPos = anchor;
        int endPos = anchor > focusIndex ? focusIndex - 1 : focusIndex + 1;
        sm.selectRange(startPos, endPos);
        setAnchor(anchor);
    }
    
    private void expandRow() {
        Callback<TreeItem<T>, Integer> getIndex = new Callback<TreeItem<T>, Integer>() {
            @Override public Integer call(TreeItem<T> p) {
                return getControl().getRow(p);
            }
        };
        TreeViewBehavior.expandRow(getControl().getSelectionModel(), getIndex);
    }
    
    private void expandAll() {
        TreeViewBehavior.expandAll(getControl().getRoot());
    }
    
    private void collapseRow() {
        TreeView<T> control = getControl();
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
        // root by pressing the left-arrow key too many times
        if (! isShowRoot && ! selectedItem.isExpanded() && root.equals(selectedItem.getParent())) {
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
        getControl().edit(null);
    }

    private void edit() {
        TreeItem<T> treeItem = getControl().getSelectionModel().getSelectedItem();
        if (treeItem == null) return;

        getControl().edit(treeItem);
    }
    
    private void toggleFocusOwnerSelection() {
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;

        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
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
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        int index = fm.getFocusedIndex() - 1;
        if (index < 0) return;
        sm.select(index);
    }
    
    private void discontinuousSelectNextRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex() + 1;
        sm.select(index);
    }
    
    private void discontinuousSelectPageUp() {
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        int leadSelectedIndex = onScrollPageUp.call(getAnchor());
        sm.selectRange(leadSelectedIndex, leadIndex + 1);
    }
    
    private void discontinuousSelectPageDown() {
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        int leadIndex = fm.getFocusedIndex();
        int leadSelectedIndex = onScrollPageDown.call(getAnchor());
        sm.selectRange(leadIndex, leadSelectedIndex + 1);
    }
    
    private void discontinuousSelectAllToFirstRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex();
        sm.selectRange(0, index);
        fm.focus(0);

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }
    
    private void discontinuousSelectAllToLastRow() {
        MultipleSelectionModel<TreeItem<T>> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<TreeItem<T>> fm = getControl().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex() + 1;
        sm.selectRange(index, getControl().getExpandedItemCount());

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }
}
