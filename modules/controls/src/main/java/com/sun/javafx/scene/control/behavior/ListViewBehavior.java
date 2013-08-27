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
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.EventType;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.control.skin.Utils;
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
import static javafx.scene.input.KeyCode.UP;

/**
 *
 */
public class ListViewBehavior<T> extends BehaviorBase<ListView<T>> {

    /**************************************************************************
     *                          Setup KeyBindings                             *
     *************************************************************************/
    protected static final List<KeyBinding> LIST_VIEW_BINDINGS = new ArrayList<KeyBinding>();

    static {
        LIST_VIEW_BINDINGS.add(new KeyBinding(HOME, "SelectFirstRow"));
        LIST_VIEW_BINDINGS.add(new KeyBinding(END, "SelectLastRow"));
        LIST_VIEW_BINDINGS.add(new KeyBinding(HOME, "SelectAllToFirstRow").shift());
        LIST_VIEW_BINDINGS.add(new KeyBinding(END, "SelectAllToLastRow").shift());
        LIST_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "SelectAllPageUp").shift());
        LIST_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "SelectAllPageDown").shift());
        
        LIST_VIEW_BINDINGS.add(new KeyBinding(SPACE, "SelectAllToFocus").shift());
        
        LIST_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "ScrollUp"));
        LIST_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "ScrollDown"));

        LIST_VIEW_BINDINGS.add(new KeyBinding(ENTER, "Activate"));
        LIST_VIEW_BINDINGS.add(new KeyBinding(SPACE, "Activate"));
        LIST_VIEW_BINDINGS.add(new KeyBinding(F2, "Activate"));
        LIST_VIEW_BINDINGS.add(new KeyBinding(ESCAPE, "CancelEdit"));

        LIST_VIEW_BINDINGS.add(new KeyBinding(A, "SelectAll").shortcut());
        LIST_VIEW_BINDINGS.add(new KeyBinding(HOME, "FocusFirstRow").shortcut());
        LIST_VIEW_BINDINGS.add(new KeyBinding(END, "FocusLastRow").shortcut());
        LIST_VIEW_BINDINGS.add(new KeyBinding(PAGE_UP, "FocusPageUp").shortcut());
        LIST_VIEW_BINDINGS.add(new KeyBinding(PAGE_DOWN, "FocusPageDown").shortcut());
            
        if (PlatformUtil.isMac()) {
            LIST_VIEW_BINDINGS.add(new KeyBinding(SPACE, "toggleFocusOwnerSelection").ctrl().shortcut());
        } else {
            LIST_VIEW_BINDINGS.add(new KeyBinding(SPACE, "toggleFocusOwnerSelection").ctrl());
        }

        // if listView is vertical...
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(UP, "SelectPreviousRow").vertical());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(KP_UP, "SelectPreviousRow").vertical());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(DOWN, "SelectNextRow").vertical());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(KP_DOWN, "SelectNextRow").vertical());

        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(UP, "AlsoSelectPreviousRow").vertical().shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(KP_UP, "AlsoSelectPreviousRow").vertical().shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(DOWN, "AlsoSelectNextRow").vertical().shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(KP_DOWN, "AlsoSelectNextRow").vertical().shift());

        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(UP, "FocusPreviousRow").vertical().shortcut());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(DOWN, "FocusNextRow").vertical().shortcut());

        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(UP, "DiscontinuousSelectPreviousRow").vertical().shortcut().shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(DOWN, "DiscontinuousSelectNextRow").vertical().shortcut().shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(PAGE_UP, "DiscontinuousSelectPageUp").vertical().shortcut().shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(PAGE_DOWN, "DiscontinuousSelectPageDown").vertical().shortcut().shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(HOME, "DiscontinuousSelectAllToFirstRow").vertical().shortcut().shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(END, "DiscontinuousSelectAllToLastRow").vertical().shortcut().shift());
        // --- end of vertical



        // if listView is horizontal...
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(LEFT, "SelectPreviousRow"));
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(KP_LEFT, "SelectPreviousRow"));
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(RIGHT, "SelectNextRow"));
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(KP_RIGHT, "SelectNextRow"));

        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(LEFT, "AlsoSelectPreviousRow").shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(KP_LEFT, "AlsoSelectPreviousRow").shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(RIGHT, "AlsoSelectNextRow").shift());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(KP_RIGHT, "AlsoSelectNextRow").shift());

        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(LEFT, "FocusPreviousRow").shortcut());
        LIST_VIEW_BINDINGS.add(new ListViewKeyBinding(RIGHT, "FocusNextRow").shortcut());
        // --- end of horizontal

        LIST_VIEW_BINDINGS.add(new KeyBinding(BACK_SLASH, "ClearSelection").shortcut());
    }
    
    protected /*final*/ String matchActionForEvent(KeyEvent e) {
        String action = super.matchActionForEvent(e);
        if (action != null) {
            if (e.getCode() == LEFT || e.getCode() == KP_LEFT) {
                if (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                    if (e.isShiftDown()) {
                        action = "AlsoSelectNextRow";
                    } else {
                        if (e.isShortcutDown()) {
                            action = "FocusNextRow";
                        } else {
                            action = getControl().getOrientation() == Orientation.HORIZONTAL ? "SelectNextRow" : "TraverseRight";
                        }
                    }
                }
            } else if (e.getCode() == RIGHT || e.getCode() == KP_RIGHT) {
                if (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                    if (e.isShiftDown()) {
                        action = "AlsoSelectPreviousRow";
                    } else {
                        if (e.isShortcutDown()) {
                            action = "FocusPreviousRow";
                        } else {
                            action = getControl().getOrientation() == Orientation.HORIZONTAL ? "SelectPreviousRow" : "TraverseLeft";
                        }
                    }
                }
            }
        }
        return action;
    }

    @Override protected void callAction(String name) {
        if ("SelectPreviousRow".equals(name)) selectPreviousRow();
        else if ("SelectNextRow".equals(name)) selectNextRow();
        else if ("SelectFirstRow".equals(name)) selectFirstRow();
        else if ("SelectLastRow".equals(name)) selectLastRow();
        else if ("SelectAllToFirstRow".equals(name)) selectAllToFirstRow();
        else if ("SelectAllToLastRow".equals(name)) selectAllToLastRow();
        else if ("SelectAllPageUp".equals(name)) selectAllPageUp();
        else if ("SelectAllPageDown".equals(name)) selectAllPageDown();
        else if ("AlsoSelectNextRow".equals(name)) alsoSelectNextRow();
        else if ("AlsoSelectPreviousRow".equals(name)) alsoSelectPreviousRow();
        else if ("ClearSelection".equals(name)) clearSelection();
        else if ("SelectAll".equals(name)) selectAll();
        else if ("ScrollUp".equals(name)) scrollPageUp();
        else if ("ScrollDown".equals(name)) scrollPageDown();
        else if ("FocusPreviousRow".equals(name)) focusPreviousRow();
        else if ("FocusNextRow".equals(name)) focusNextRow();
        else if ("FocusPageUp".equals(name)) focusPageUp();
        else if ("FocusPageDown".equals(name)) focusPageDown();
        else if ("Activate".equals(name)) activate();
        else if ("CancelEdit".equals(name)) cancelEdit();
        else if ("FocusFirstRow".equals(name)) focusFirstRow();
        else if ("FocusLastRow".equals(name)) focusLastRow();
        else if ("toggleFocusOwnerSelection".equals(name)) toggleFocusOwnerSelection();
        else if ("SelectAllToFocus".equals(name)) selectAllToFocus();
        else if ("DiscontinuousSelectNextRow".equals(name)) discontinuousSelectNextRow();
        else if ("DiscontinuousSelectPreviousRow".equals(name)) discontinuousSelectPreviousRow();
        else if ("DiscontinuousSelectPageUp".equals(name)) discontinuousSelectPageUp();
        else if ("DiscontinuousSelectPageDown".equals(name)) discontinuousSelectPageDown();
        else if ("DiscontinuousSelectAllToLastRow".equals(name)) discontinuousSelectAllToLastRow();
        else if ("DiscontinuousSelectAllToFirstRow".equals(name)) discontinuousSelectAllToFirstRow();
        else super.callAction(name);
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
    
    private Callback<Integer, Integer> onScrollPageUp;
    private Callback<Integer, Integer> onScrollPageDown;
    private Runnable onFocusPreviousRow;
    private Runnable onFocusNextRow;
    private Runnable onSelectPreviousRow;
    private Runnable onSelectNextRow;
    private Runnable onMoveToFirstCell;
    private Runnable onMoveToLastCell;

    public void setOnScrollPageUp(Callback<Integer, Integer> c) { onScrollPageUp = c; }
    public void setOnScrollPageDown(Callback<Integer, Integer> c) { onScrollPageDown = c; }
    public void setOnFocusPreviousRow(Runnable r) { onFocusPreviousRow = r; }
    public void setOnFocusNextRow(Runnable r) { onFocusNextRow = r; }
    public void setOnSelectPreviousRow(Runnable r) { onSelectPreviousRow = r; }
    public void setOnSelectNextRow(Runnable r) { onSelectNextRow = r; }
    public void setOnMoveToFirstCell(Runnable r) { onMoveToFirstCell = r; }
    public void setOnMoveToLastCell(Runnable r) { onMoveToLastCell = r; }
    
    private boolean selectionChanging = false;
    
    private final ListChangeListener<Integer> selectedIndicesListener = new ListChangeListener<Integer>() {
        @Override public void onChanged(ListChangeListener.Change<? extends Integer> c) {
            while (c.next()) {
                MultipleSelectionModel<T> sm = getControl().getSelectionModel();
                
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
    
    private final ListChangeListener<T> itemsListListener = new ListChangeListener<T>() {
        @Override public void onChanged(Change<? extends T> c) {
            while (c.next()) {
                if (c.wasAdded() && c.getFrom() <= getAnchor()) {
                    setAnchor(getAnchor() + c.getAddedSize());
                } else if (c.wasRemoved() && c.getFrom() <= getAnchor()) {
                    setAnchor(getAnchor() - c.getRemovedSize());
                }
            }
        }
    };
    
    private final ChangeListener<ObservableList<T>> itemsListener = new ChangeListener<ObservableList<T>>() {
        @Override
        public void changed(
                ObservableValue<? extends ObservableList<T>> observable,
                ObservableList<T> oldValue, ObservableList<T> newValue) {
            if (oldValue != null) {
                 oldValue.removeListener(weakItemsListListener);
             } if (newValue != null) {
                 newValue.addListener(weakItemsListListener);
             }
        }
    };
    
    private final ChangeListener<MultipleSelectionModel<T>> selectionModelListener = new ChangeListener<MultipleSelectionModel<T>>() {
        @Override public void changed(
                    ObservableValue<? extends MultipleSelectionModel<T>> observable, 
                    MultipleSelectionModel<T> oldValue, 
                    MultipleSelectionModel<T> newValue) {
            if (oldValue != null) {
                oldValue.getSelectedIndices().removeListener(weakSelectedIndicesListener);
            }
            if (newValue != null) {
                newValue.getSelectedIndices().addListener(weakSelectedIndicesListener);
            }
        }
    };
    
    private final WeakChangeListener<ObservableList<T>> weakItemsListener = 
            new WeakChangeListener<ObservableList<T>>(itemsListener);
    private final WeakListChangeListener<Integer> weakSelectedIndicesListener = 
            new WeakListChangeListener<Integer>(selectedIndicesListener);
    private final WeakListChangeListener<T> weakItemsListListener = 
            new WeakListChangeListener<>(itemsListListener);
    private final WeakChangeListener<MultipleSelectionModel<T>> weakSelectionModelListener = 
            new WeakChangeListener<MultipleSelectionModel<T>>(selectionModelListener);
    
    private TwoLevelFocusListBehavior tlFocus;

    public ListViewBehavior(ListView<T> control) {
        super(control, LIST_VIEW_BINDINGS);
        
        control.itemsProperty().addListener(weakItemsListener);
        if (control.getItems() != null) {
            control.getItems().addListener(weakItemsListListener);
        }
        
        // Fix for RT-16565
        getControl().selectionModelProperty().addListener(weakSelectionModelListener);
        if (control.getSelectionModel() != null) {
            control.getSelectionModel().getSelectedIndices().addListener(weakSelectedIndicesListener);
        }

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusListBehavior(control); // needs to be last.
        }
    }
    
    @Override public void dispose() {
        ListCellBehavior.removeAnchor(getControl());
        if (tlFocus != null) tlFocus.dispose();
        super.dispose();
    }

    private void setAnchor(int anchor) {
        ListCellBehavior.setAnchor(getControl(), anchor);
    }
    
    private int getAnchor() {
        return ListCellBehavior.getAnchor(getControl());
    }
    
    private boolean hasAnchor() {
        return ListCellBehavior.hasAnchor(getControl());
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
    
    private int getRowCount() {
        return getControl().getItems() == null ? 0 : getControl().getItems().size();
    }

    private void clearSelection() {
        getControl().getSelectionModel().clearSelection();
    }

    private void scrollPageUp() {
        int newSelectedIndex = -1;
        if (onScrollPageUp != null) {
            newSelectedIndex = onScrollPageUp.call(getAnchor());
        }
        if (newSelectedIndex == -1) return;
        
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        sm.clearAndSelect(newSelectedIndex);
    }

    private void scrollPageDown() {
        int newSelectedIndex = -1;
        if (onScrollPageDown != null) {
            newSelectedIndex = onScrollPageDown.call(getAnchor());
        }
        if (newSelectedIndex == -1) return;
        
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        sm.clearAndSelect(newSelectedIndex);
    }
    
    private void focusFirstRow() {
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        fm.focus(0);
        
        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }
    
    private void focusLastRow() {
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        fm.focus(getRowCount() - 1);
        
        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private void focusPreviousRow() {
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        fm.focusPrevious();
        
        if (! isShortcutDown || getAnchor() == -1) {
            setAnchor(fm.getFocusedIndex());
        }
        
        if (onFocusPreviousRow != null) onFocusPreviousRow.run();
    }

    private void focusNextRow() {
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        fm.focusNext();
        
        if (! isShortcutDown || getAnchor() == -1) {
            setAnchor(fm.getFocusedIndex());
        }
        
        if (onFocusNextRow != null) onFocusNextRow.run();
    }
    
    private void focusPageUp() {
        int newFocusIndex = onScrollPageUp.call(getAnchor());
        
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        fm.focus(newFocusIndex);
    }
    
    private void focusPageDown() {
        int newFocusIndex = onScrollPageDown.call(getAnchor());
        
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        fm.focus(newFocusIndex);
    }

    private void alsoSelectPreviousRow() {
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
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
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
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
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
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
        FocusModel<T> fm = getControl().getFocusModel();
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
        ListView<T> listView = getControl();
        FocusModel<T> fm = listView.getFocusModel();
        if (fm == null) return;
        
        int focusIndex = fm.getFocusedIndex();
        if (focusIndex == getRowCount() - 1) {
            return;
        }
        
        MultipleSelectionModel<T> sm = listView.getSelectionModel();
        if (sm == null) return;
        
        setAnchor(focusIndex + 1);
        sm.clearAndSelect(focusIndex + 1);
        if (onSelectNextRow != null) onSelectNextRow.run();
    }

    private void selectFirstRow() {
        if (getRowCount() > 0) {
            getControl().getSelectionModel().clearAndSelect(0);
            if (onMoveToFirstCell != null) onMoveToFirstCell.run();
        }
    }

    private void selectLastRow() {
        getControl().getSelectionModel().clearAndSelect(getRowCount() - 1);
        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }
    
    private void selectAllPageUp() {
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        if (isShiftDown) {
            leadIndex = getAnchor() == -1 ? leadIndex : getAnchor();
            setAnchor(leadIndex);
        }
        
        int leadSelectedIndex = onScrollPageUp.call(getAnchor());
        
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        selectionChanging = true;
        if (sm.getSelectionMode() == SelectionMode.SINGLE) {
            sm.select(leadSelectedIndex);
        } else {
            sm.clearSelection();
            sm.selectRange(leadSelectedIndex, leadIndex + 1);
        }
        selectionChanging = false;
    }
    
    private void selectAllPageDown() {
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        int leadIndex = fm.getFocusedIndex();
        if (isShiftDown) {
            leadIndex = getAnchor() == -1 ? leadIndex : getAnchor();
            setAnchor(leadIndex);
        }
        
        int leadSelectedIndex = onScrollPageDown.call(getAnchor());
        
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        selectionChanging = true;
        if (sm.getSelectionMode() == SelectionMode.SINGLE) {
            sm.select(leadSelectedIndex);
        } else {
            sm.clearSelection();
            sm.selectRange(leadIndex, leadSelectedIndex + 1);
        }
        selectionChanging = false;
    }

    private void selectAllToFirstRow() {
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
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
        
        // RT-18413: Focus must go to first row
        getControl().getFocusModel().focus(0);

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    private void selectAllToLastRow() {
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;

        int leadIndex = sm.getSelectedIndex();
        
        if (isShiftDown) {
            leadIndex = hasAnchor() ? sm.getSelectedIndex() : getAnchor();
        }
        
        sm.clearSelection();
        sm.selectRange(leadIndex, getRowCount());
        
        if (isShiftDown) {
            setAnchor(leadIndex);
        }

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private void selectAll() {
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        sm.selectAll();
    }
    
    private void selectAllToFocus() {
        // Fix for RT-31241
        final ListView listView = getControl();
        if (listView.getEditingIndex() >= 0) return;

        MultipleSelectionModel<T> sm = listView.getSelectionModel();
        if (sm == null) return;

        FocusModel<T> fm = listView.getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        int anchor = getAnchor();
        
        sm.clearSelection();
        int startPos = anchor;
        int endPos = anchor > focusIndex ? focusIndex - 1 : focusIndex + 1;
        sm.selectRange(startPos, endPos);
        setAnchor(anchor);
    }
    
    private void cancelEdit() {
        getControl().edit(-1);
    }

    private void activate() {
        int focusedIndex = getControl().getFocusModel().getFocusedIndex();
        getControl().getSelectionModel().select(focusedIndex);

        // edit this row also
        if (focusedIndex >= 0) {
            getControl().edit(focusedIndex);
        }
    }
    
    private void toggleFocusOwnerSelection() {
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;

        FocusModel<T> fm = getControl().getFocusModel();
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
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        int index = fm.getFocusedIndex() - 1;
        if (index < 0) return;
        sm.select(index);
    }
    
    private void discontinuousSelectNextRow() {
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex() + 1;
        sm.select(index);
    }
    
    private void discontinuousSelectPageUp() {
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        int leadSelectedIndex = onScrollPageUp.call(getAnchor());
        sm.selectRange(leadSelectedIndex, leadIndex + 1);
    }
    
    private void discontinuousSelectPageDown() {
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;
        
        int leadIndex = fm.getFocusedIndex();
        int leadSelectedIndex = onScrollPageDown.call(getAnchor());
        sm.selectRange(leadIndex, leadSelectedIndex + 1);
    }
    
    private void discontinuousSelectAllToFirstRow() {
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex();
        sm.selectRange(0, index);
        fm.focus(0);

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }
    
    private void discontinuousSelectAllToLastRow() {
        MultipleSelectionModel<T> sm = getControl().getSelectionModel();
        if (sm == null) return;
        
        FocusModel<T> fm = getControl().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex() + 1;
        sm.selectRange(index, getRowCount());

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private static class ListViewKeyBinding extends OrientedKeyBinding {

        public ListViewKeyBinding(KeyCode code, String action) {
            super(code, action);
        }

        public ListViewKeyBinding(KeyCode code, EventType<KeyEvent> type, String action) {
            super(code, type, action);
        }

        @Override public boolean getVertical(Control control) {
            return ((ListView<?>)control).getOrientation() == Orientation.VERTICAL;
        }
    }

}
