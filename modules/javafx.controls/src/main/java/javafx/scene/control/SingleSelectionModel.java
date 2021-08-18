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

package javafx.scene.control;

/**
 * A SelectionModel which enforces the requirement that only a single index
 * be selected at any given time. This class exists for controls that allow for
 * pluggable selection models, but which do not allow for multiple selection.
 * A good example is the {@link ChoiceBox} control. Conversely, most other
 * controls ({@link ListView}, {@link TreeView}, {@link TableView}, etc)
 * require {@link MultipleSelectionModel} implementations (although
 * MultipleSelectionModel does still allow for single selection to be set via the
 * {@link MultipleSelectionModel#selectionModeProperty() selectionMode}
 * property).
 *
 * @see SelectionModel
 * @see MultipleSelectionModel
 * @see SelectionMode
 * @param <T> The type of the item contained in the control that can be selected.
 * @since JavaFX 2.0
 */
public abstract class SingleSelectionModel<T> extends SelectionModel<T> {

    /* *************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default SingleSelectionModel instance.
     */
    public SingleSelectionModel() { }



    /* *************************************************************************
     *                                                                         *
     * Selection API                                                           *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void clearSelection() {
        updateSelectedIndex(-1);
    }

    /**
     * Clears the selection of the given index, if it is currently selected.
     */
    @Override public void clearSelection(int index) {
        if (getSelectedIndex() == index) {
            clearSelection();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean isEmpty() {
        return getItemCount() == 0 || getSelectedIndex() == -1;
    }

    /**
     * <p>This method will return true if the given index is the currently
     * selected index in this SingleSelectionModel.
     *
     * @param index The index to check as to whether it is currently selected
     *      or not.
     * @return True if the given index is selected, false otherwise.
     */
    @Override public boolean isSelected(int index) {
        return getSelectedIndex() == index;
    }

    /**
     * In the SingleSelectionModel, this method is functionally equivalent to
     * calling <code>select(index)</code>, as only one selection is allowed at
     * a time.
     */
    @Override public void clearAndSelect(int index) {
        select(index);
    }

    /**
     * Selects the index for the first instance of given object in the underlying
     * data model. Since the SingleSelectionModel can
     * only support having a single index selected at a time, this also causes
     * any previously selected index to be unselected.
     */
    @Override public void select(T obj) {
        if (obj == null) {
            setSelectedIndex(-1);
            setSelectedItem(null);
            return;
        }

        final int itemCount = getItemCount();

        for (int i = 0; i < itemCount; i++) {
            final T value = getModelItem(i);
            if (value != null && value.equals(obj)) {
                select(i);
                return;
            }
        }

        // if we are here, we did not find the item in the entire data model.
        // Even still, we allow for this item to be set to the give object.
        // We expect that in concrete subclasses of this class we observe the
        // data model such that we check to see if the given item exists in it,
        // whilst SelectedIndex == -1 && SelectedItem != null.
        setSelectedItem(obj);
    }

    /**
     * Selects the given index. Since the SingleSelectionModel can only support having
     * a single index selected at a time, this also causes any previously selected
     * index to be unselected.
     */
    @Override public void select(int index) {
        if (index == -1) {
            clearSelection();
            return;
        }
        final int itemCount = getItemCount();
        if (itemCount == 0 || index < 0 || index >= itemCount) return;
        updateSelectedIndex(index);
    }

    /**
     * Selects the previous index. Since the SingleSelectionModel can only support having
     * a single index selected at a time, this also causes any previously selected
     * index to be unselected.
     */
    @Override public void selectPrevious() {
        if (getSelectedIndex() == 0) return;
        select(getSelectedIndex() - 1);
    }

    /**
     * Selects the next index. Since the SingleSelectionModel can only support having
     * a single index selected at a time, this also causes any previously selected
     * index to be unselected.
     */
    @Override public void selectNext() {
        select(getSelectedIndex() + 1);
    }

    /**
     * Selects the first index. Since the SingleSelectionModel can only support having
     * a single index selected at a time, this also causes any previously selected
     * index to be unselected.
     */
    @Override public void selectFirst() {
        if (getItemCount() > 0) {
            select(0);
        }
    }

    /**
     * Selects the last index. Since the SingleSelectionModel can only support having
     * a single index selected at a time, this also causes any previously selected
     * index to be unselected.
     */
    @Override public void selectLast() {
        int numItems = getItemCount();
        if (numItems > 0 && getSelectedIndex() < numItems - 1) {
            select(numItems - 1);
        }
    }

    /**
     * Gets the data model item associated with a specific index.
     * @param index The position of the item in the underlying data model.
     * @return The item that exists at the given index.
     */
    protected abstract T getModelItem(int index);

    /**
     * Gets the number of items available for the selection model. If the number
     * of items can change dynamically, it is the responsibility of the
     * concrete SingleSelectionModel implementation to ensure that items are
     * selected or unselected as appropriate as the items change.
     * @return A number greater than or equal to 0.
     */
    protected abstract int getItemCount();

    // Private Implementation
    private void updateSelectedIndex(int newIndex) {
        int currentIndex = getSelectedIndex();
        T currentItem = getSelectedItem();

        setSelectedIndex(newIndex);

        if (currentIndex == -1 && currentItem != null && newIndex == -1) {
            // no-op: the current selection isn't in the underlying data model -
            // we should keep the selected item as the new index is -1
        } else {
            // we don't use newIndex here, to prevent RT-32139 (which has a unit
            // test developed to prevent regressions in the future)
            setSelectedItem(getModelItem(getSelectedIndex()));
        }
    }
}
