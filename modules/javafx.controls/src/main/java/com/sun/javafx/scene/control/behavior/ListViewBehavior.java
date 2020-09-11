/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.FocusModel;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.sun.javafx.scene.control.inputmap.InputMap.*;
import static javafx.scene.input.KeyCode.*;

public class ListViewBehavior<T> extends BehaviorBase<ListView<T>> {
    private final InputMap<ListView<T>> listViewInputMap;

    private final EventHandler<KeyEvent> keyEventListener = e -> {
        if (!e.isConsumed()) {
            // RT-12751: we want to keep an eye on the user holding down the shift key,
            // so that we know when they enter/leave multiple selection mode. This
            // changes what happens when certain key combinations are pressed.
            isShiftDown = e.getEventType() == KeyEvent.KEY_PRESSED && e.isShiftDown();
            isShortcutDown = e.getEventType() == KeyEvent.KEY_PRESSED && e.isShortcutDown();
        }
    };



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ListViewBehavior(ListView<T> control) {
        super(control);

        // create a map for listView-specific mappings
        listViewInputMap = createInputMap();

        // add focus traversal mappings
        Supplier<Boolean> isListViewOfComboBox =
                (Supplier<Boolean>) control.getProperties().get("editableComboBox");
        Predicate<KeyEvent> isInComboBox = e -> isListViewOfComboBox != null;
        Predicate<KeyEvent> isInEditableComboBox =
                e -> isListViewOfComboBox != null && isListViewOfComboBox.get();
        if (isListViewOfComboBox == null) {
            addDefaultMapping(listViewInputMap, FocusTraversalInputMap.getFocusTraversalMappings());
        }
        addDefaultMapping(listViewInputMap,
            new KeyMapping(new KeyBinding(HOME), e -> selectFirstRow(), isInEditableComboBox),
            new KeyMapping(new KeyBinding(END), e -> selectLastRow(), isInEditableComboBox),
            new KeyMapping(new KeyBinding(HOME).shift(), e -> selectAllToFirstRow(), isInComboBox),
            new KeyMapping(new KeyBinding(END).shift(), e -> selectAllToLastRow(), isInComboBox),
            new KeyMapping(new KeyBinding(PAGE_UP).shift(), e -> selectAllPageUp()),
            new KeyMapping(new KeyBinding(PAGE_DOWN).shift(), e -> selectAllPageDown()),

            new KeyMapping(new KeyBinding(SPACE).shift(), e -> selectAllToFocus(false)),
            new KeyMapping(new KeyBinding(SPACE).shortcut().shift(), e -> selectAllToFocus(true)),

            new KeyMapping(PAGE_UP, e -> scrollPageUp()),
            new KeyMapping(PAGE_DOWN, e -> scrollPageDown()),

            new KeyMapping(ENTER, e -> activate()),
            new KeyMapping(SPACE, e -> activate()),
            new KeyMapping(F2, e -> activate()),
            new KeyMapping(ESCAPE, e -> cancelEdit()),

            new KeyMapping(new KeyBinding(A).shortcut(), e -> selectAll(), isInComboBox),
            new KeyMapping(new KeyBinding(HOME).shortcut(), e -> focusFirstRow(), isInComboBox),
            new KeyMapping(new KeyBinding(END).shortcut(), e -> focusLastRow(), isInComboBox),
            new KeyMapping(new KeyBinding(PAGE_UP).shortcut(), e -> focusPageUp()),
            new KeyMapping(new KeyBinding(PAGE_DOWN).shortcut(), e -> focusPageDown()),

            new KeyMapping(new KeyBinding(BACK_SLASH).shortcut(), e -> clearSelection()),

            new MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed)
        );

        // create OS-specific child mappings
        // --- mac OS
        InputMap<ListView<T>> macInputMap = new InputMap<>(control);
        macInputMap.setInterceptor(event -> !PlatformUtil.isMac());
        addDefaultMapping(macInputMap, new KeyMapping(new KeyBinding(SPACE).shortcut().ctrl(), e -> toggleFocusOwnerSelection()));
        addDefaultChildMap(listViewInputMap, macInputMap);

        // --- all other platforms
        InputMap<ListView<T>> otherOsInputMap = new InputMap<>(control);
        otherOsInputMap.setInterceptor(event -> PlatformUtil.isMac());
        addDefaultMapping(otherOsInputMap, new KeyMapping(new KeyBinding(SPACE).ctrl(), e -> toggleFocusOwnerSelection()));
        addDefaultChildMap(listViewInputMap, otherOsInputMap);

        // create two more child maps, one for vertical listview and one for horizontal listview
        // --- vertical listview
        InputMap<ListView<T>> verticalListInputMap = new InputMap<>(control);
        verticalListInputMap.setInterceptor(event -> control.getOrientation() != Orientation.VERTICAL);

        addDefaultMapping(verticalListInputMap,
            new KeyMapping(UP, e -> selectPreviousRow()),
            new KeyMapping(KP_UP, e -> selectPreviousRow()),
            new KeyMapping(DOWN, e -> selectNextRow()),
            new KeyMapping(KP_DOWN, e -> selectNextRow()),

            new KeyMapping(new KeyBinding(UP).shift(), e -> alsoSelectPreviousRow()),
            new KeyMapping(new KeyBinding(KP_UP).shift(), e -> alsoSelectPreviousRow()),
            new KeyMapping(new KeyBinding(DOWN).shift(), e -> alsoSelectNextRow()),
            new KeyMapping(new KeyBinding(KP_DOWN).shift(), e -> alsoSelectNextRow()),

            new KeyMapping(new KeyBinding(UP).shortcut(), e -> focusPreviousRow()),
            new KeyMapping(new KeyBinding(DOWN).shortcut(), e -> focusNextRow()),

            new KeyMapping(new KeyBinding(UP).shortcut().shift(), e -> discontinuousSelectPreviousRow()),
            new KeyMapping(new KeyBinding(DOWN).shortcut().shift(), e -> discontinuousSelectNextRow()),
            new KeyMapping(new KeyBinding(PAGE_UP).shortcut().shift(), e -> discontinuousSelectPageUp()),
            new KeyMapping(new KeyBinding(PAGE_DOWN).shortcut().shift(), e -> discontinuousSelectPageDown()),
            new KeyMapping(new KeyBinding(HOME).shortcut().shift(), e -> discontinuousSelectAllToFirstRow(), isInComboBox),
            new KeyMapping(new KeyBinding(END).shortcut().shift(), e -> discontinuousSelectAllToLastRow(), isInComboBox)
        );
        addDefaultChildMap(listViewInputMap, verticalListInputMap);

        // --- horizontal listview
        InputMap<ListView<T>> horizontalListInputMap = new InputMap<>(control);
        horizontalListInputMap.setInterceptor(event -> control.getOrientation() != Orientation.HORIZONTAL);

        addDefaultMapping(horizontalListInputMap,
            new KeyMapping(LEFT, e -> selectPreviousRow()),
            new KeyMapping(KP_LEFT, e -> selectPreviousRow()),
            new KeyMapping(RIGHT, e -> selectNextRow()),
            new KeyMapping(KP_RIGHT, e -> selectNextRow()),

            new KeyMapping(new KeyBinding(LEFT).shift(), e -> alsoSelectPreviousRow()),
            new KeyMapping(new KeyBinding(KP_LEFT).shift(), e -> alsoSelectPreviousRow()),
            new KeyMapping(new KeyBinding(RIGHT).shift(), e -> alsoSelectNextRow()),
            new KeyMapping(new KeyBinding(KP_RIGHT).shift(), e -> alsoSelectNextRow()),

            new KeyMapping(new KeyBinding(LEFT).shortcut(), e -> focusPreviousRow()),
            new KeyMapping(new KeyBinding(RIGHT).shortcut(), e -> focusNextRow()),

            new KeyMapping(new KeyBinding(LEFT).shortcut().shift(), e -> discontinuousSelectPreviousRow()),
            new KeyMapping(new KeyBinding(RIGHT).shortcut().shift(), e -> discontinuousSelectNextRow())
        );

        addDefaultChildMap(listViewInputMap, horizontalListInputMap);

        // set up other listeners
        // We make this an event _filter_ so that we can determine the state
        // of the shift key before the event handlers get a shot at the event.
        control.addEventFilter(KeyEvent.ANY, keyEventListener);

        control.itemsProperty().addListener(weakItemsListener);
        if (control.getItems() != null) {
            control.getItems().addListener(weakItemsListListener);
        }

        // Fix for RT-16565
        control.selectionModelProperty().addListener(weakSelectionModelListener);
        if (control.getSelectionModel() != null) {
            control.getSelectionModel().getSelectedIndices().addListener(weakSelectedIndicesListener);
        }

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusListBehavior(control); // needs to be last.
        }
    }


    /***************************************************************************
     *                                                                         *
     * Implementation of BehaviorBase API                                      *
     *                                                                         *
     **************************************************************************/

    @Override public InputMap<ListView<T>> getInputMap() {
        return listViewInputMap;
    }

    @Override public void dispose() {
        ListView<T> control = getNode();

        ListCellBehavior.removeAnchor(control);
        control.selectionModelProperty().removeListener(weakSelectionModelListener);
        if (control.getSelectionModel() != null) {
            control.getSelectionModel().getSelectedIndices().removeListener(weakSelectedIndicesListener);
        }
        control.itemsProperty().removeListener(weakItemsListener);
        if (control.getItems() != null) {
            control.getItems().removeListener(weakItemsListListener);
        }

        if (tlFocus != null) tlFocus.dispose();
        control.removeEventFilter(KeyEvent.ANY, keyEventListener);
        super.dispose();
    }

    /**************************************************************************
     *                         State and Functions                            *
     *************************************************************************/

    private boolean isShiftDown = false;
    private boolean isShortcutDown = false;

    private Callback<Boolean, Integer> onScrollPageUp;
    private Callback<Boolean, Integer> onScrollPageDown;
    private Runnable onFocusPreviousRow;
    private Runnable onFocusNextRow;
    private Runnable onSelectPreviousRow;
    private Runnable onSelectNextRow;
    private Runnable onMoveToFirstCell;
    private Runnable onMoveToLastCell;

    public void setOnScrollPageUp(Callback<Boolean, Integer> c) { onScrollPageUp = c; }
    public void setOnScrollPageDown(Callback<Boolean, Integer> c) { onScrollPageDown = c; }
    public void setOnFocusPreviousRow(Runnable r) { onFocusPreviousRow = r; }
    public void setOnFocusNextRow(Runnable r) { onFocusNextRow = r; }
    public void setOnSelectPreviousRow(Runnable r) { onSelectPreviousRow = r; }
    public void setOnSelectNextRow(Runnable r) { onSelectNextRow = r; }
    public void setOnMoveToFirstCell(Runnable r) { onMoveToFirstCell = r; }
    public void setOnMoveToLastCell(Runnable r) { onMoveToLastCell = r; }

    private boolean selectionChanging = false;

    private final ListChangeListener<Integer> selectedIndicesListener = c -> {
        int newAnchor = getAnchor();

        while (c.next()) {
            if (c.wasReplaced()) {
                if (ListCellBehavior.hasDefaultAnchor(getNode())) {
                    ListCellBehavior.removeAnchor(getNode());
                    continue;
                }
            }

            final int shift = c.wasPermutated() ? c.getTo() - c.getFrom() : 0;

            MultipleSelectionModel<T> sm = getNode().getSelectionModel();

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

    private final ListChangeListener<T> itemsListListener = c -> {
        while (c.next()) {
            if (!hasAnchor()) continue;

            int newAnchor = (hasAnchor() ? getAnchor() : 0);

            if (c.wasAdded() && c.getFrom() <= newAnchor) {
                newAnchor += c.getAddedSize();
            } else if (c.wasRemoved() && c.getFrom() <= newAnchor) {
                newAnchor -= c.getRemovedSize();
            }

            setAnchor(newAnchor < 0 ? 0 : newAnchor);
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

    private void setAnchor(int anchor) {
        ListCellBehavior.setAnchor(getNode(), anchor < 0 ? null : anchor, false);
    }

    private int getAnchor() {
        return ListCellBehavior.getAnchor(getNode(), getNode().getFocusModel().getFocusedIndex());
    }

    private boolean hasAnchor() {
        return ListCellBehavior.hasNonDefaultAnchor(getNode());
    }

    private void mousePressed(MouseEvent e) {
        if (! e.isShiftDown() && ! e.isSynthesized()) {
            int index = getNode().getSelectionModel().getSelectedIndex();
            setAnchor(index);
        }

        if (! getNode().isFocused() && getNode().isFocusTraversable()) {
            getNode().requestFocus();
        }
    }

    private int getRowCount() {
        return getNode().getItems() == null ? 0 : getNode().getItems().size();
    }

    private void clearSelection() {
        getNode().getSelectionModel().clearSelection();
    }

    private void scrollPageUp() {
        int newSelectedIndex = -1;
        if (onScrollPageUp != null) {
            newSelectedIndex = onScrollPageUp.call(false);
        }
        if (newSelectedIndex == -1) return;

        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;
        sm.clearAndSelect(newSelectedIndex);
    }

    private void scrollPageDown() {
        int newSelectedIndex = -1;
        if (onScrollPageDown != null) {
            newSelectedIndex = onScrollPageDown.call(false);
        }
        if (newSelectedIndex == -1) return;

        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;
        sm.clearAndSelect(newSelectedIndex);
    }

    private void focusFirstRow() {
        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;
        fm.focus(0);

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    private void focusLastRow() {
        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;
        fm.focus(getRowCount() - 1);

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private void focusPreviousRow() {
        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        fm.focusPrevious();

        if (! isShortcutDown || getAnchor() == -1) {
            setAnchor(fm.getFocusedIndex());
        }

        if (onFocusPreviousRow != null) onFocusPreviousRow.run();
    }

    private void focusNextRow() {
        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        fm.focusNext();

        if (! isShortcutDown || getAnchor() == -1) {
            setAnchor(fm.getFocusedIndex());
        }

        if (onFocusNextRow != null) onFocusNextRow.run();
    }

    private void focusPageUp() {
        int newFocusIndex = onScrollPageUp.call(true);

        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;
        fm.focus(newFocusIndex);
    }

    private void focusPageDown() {
        int newFocusIndex = onScrollPageDown.call(true);

        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;
        fm.focus(newFocusIndex);
    }

    private void alsoSelectPreviousRow() {
        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
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
        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
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
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        int min = Math.min(start, end);
        int max = Math.max(start, end);

        List<Integer> indices = new ArrayList<>(sm.getSelectedIndices());

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
        FocusModel<T> fm = getNode().getFocusModel();
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
        ListView<T> listView = getNode();
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
            getNode().getSelectionModel().clearAndSelect(0);
            if (onMoveToFirstCell != null) onMoveToFirstCell.run();
        }
    }

    private void selectLastRow() {
        getNode().getSelectionModel().clearAndSelect(getRowCount() - 1);
        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private void selectAllPageUp() {
        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        if (isShiftDown) {
            leadIndex = getAnchor() == -1 ? leadIndex : getAnchor();
            setAnchor(leadIndex);
        }

        int leadSelectedIndex = onScrollPageUp.call(false);

        // fix for RT-34407
        int adjust = leadIndex < leadSelectedIndex ? 1 : -1;

        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
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
        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();
        if (isShiftDown) {
            leadIndex = getAnchor() == -1 ? leadIndex : getAnchor();
            setAnchor(leadIndex);
        }

        int leadSelectedIndex = onScrollPageDown.call(false);

        // fix for RT-34407
        int adjust = leadIndex < leadSelectedIndex ? 1 : -1;

        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
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

    private void selectAllToFirstRow() {
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<T> fm = getNode().getFocusModel();
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
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        int leadIndex = fm.getFocusedIndex();

        if (isShiftDown) {
            leadIndex = hasAnchor() ? getAnchor() : leadIndex;
        }

        sm.clearSelection();
        sm.selectRange(leadIndex, getRowCount());

        if (isShiftDown) {
            setAnchor(leadIndex);
        }

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }

    private void selectAll() {
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;
        sm.selectAll();
    }

    private void selectAllToFocus(boolean setAnchorToFocusIndex) {
        // Fix for RT-31241
        final ListView<T> listView = getNode();
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
        setAnchor(setAnchorToFocusIndex ? focusIndex : anchor);
    }

    private void cancelEdit() {
        getNode().edit(-1);
    }

    private void activate() {
        int focusedIndex = getNode().getFocusModel().getFocusedIndex();
        getNode().getSelectionModel().select(focusedIndex);
        setAnchor(focusedIndex);

        // edit this row also
        if (focusedIndex >= 0) {
            getNode().edit(focusedIndex);
        }
    }

    private void toggleFocusOwnerSelection() {
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<T> fm = getNode().getFocusModel();
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
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        if (sm.getSelectionMode() != SelectionMode.MULTIPLE) {
            selectPreviousRow();
            return;
        }

        FocusModel<T> fm = getNode().getFocusModel();
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
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        if (sm.getSelectionMode() != SelectionMode.MULTIPLE) {
            selectNextRow();
            return;
        }

        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        int focusIndex = fm.getFocusedIndex();
        final int newFocusIndex = focusIndex + 1;
        if (newFocusIndex >= getRowCount()) return;

        int startIndex = focusIndex;
        if (isShiftDown) {
            startIndex = getAnchor() == -1 ? focusIndex : getAnchor();
        }

        sm.selectRange(startIndex, newFocusIndex + 1);
        fm.focus(newFocusIndex);

        if (onFocusNextRow != null) onFocusNextRow.run();
    }

    private void discontinuousSelectPageUp() {
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        int anchor = getAnchor();
        int leadSelectedIndex = onScrollPageUp.call(false);
        sm.selectRange(anchor, leadSelectedIndex - 1);
    }

    private void discontinuousSelectPageDown() {
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        int anchor = getAnchor();
        int leadSelectedIndex = onScrollPageDown.call(false);
        sm.selectRange(anchor, leadSelectedIndex + 1);
    }

    private void discontinuousSelectAllToFirstRow() {
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex();
        sm.selectRange(0, index);
        fm.focus(0);

        if (onMoveToFirstCell != null) onMoveToFirstCell.run();
    }

    private void discontinuousSelectAllToLastRow() {
        MultipleSelectionModel<T> sm = getNode().getSelectionModel();
        if (sm == null) return;

        FocusModel<T> fm = getNode().getFocusModel();
        if (fm == null) return;

        int index = fm.getFocusedIndex() + 1;
        sm.selectRange(index, getRowCount());

        if (onMoveToLastCell != null) onMoveToLastCell.run();
    }
}
