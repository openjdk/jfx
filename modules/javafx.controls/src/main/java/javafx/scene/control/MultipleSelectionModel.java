/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * An abstract class that extends {@link SelectionModel} to add API to support
 * multiple selection.
 *
 * @see SelectionModel
 * @see SelectionMode
 * @param <T> The type of the item contained in the control that can be selected.
 * @since JavaFX 2.0
 */
public abstract class MultipleSelectionModel<T> extends SelectionModel<T> {

    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * <p>Specifies the selection mode to use in this selection model. The
     * selection mode specifies how many items in the underlying data model can
     * be selected at any one time.
     *
     * <p>By default, the selection mode is <code>SelectionMode.SINGLE</code>.
     */
    private ObjectProperty<SelectionMode> selectionMode;
    public final void setSelectionMode(SelectionMode value) {
        selectionModeProperty().set(value);
    }

    public final SelectionMode getSelectionMode() {
        return selectionMode == null ? SelectionMode.SINGLE : selectionMode.get();
    }

    public final ObjectProperty<SelectionMode> selectionModeProperty() {
        if (selectionMode == null) {
            selectionMode = new ObjectPropertyBase<SelectionMode>(SelectionMode.SINGLE) {
                @Override protected void invalidated() {
                    if (getSelectionMode() == SelectionMode.SINGLE) {
                        // we need to pick out just the last selected item, as we've gone
                        // to single selection
                        if (! isEmpty()) {
                            int lastIndex = getSelectedIndex();
                            clearSelection();
                            select(lastIndex);
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MultipleSelectionModel.this;
                }

                @Override
                public String getName() {
                    return "selectionMode";
                }
            };
        }
        return selectionMode;
    }



    /* *************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default MultipleSelectionModel instance.
     */
    public MultipleSelectionModel() { }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * <p>Returns a <b>read-only</b> ObservableList of all selected indices. The
     * ObservableList will be updated  by the selection model to always reflect
     * changes in selection. This can be observed by adding a
     * {@link ListChangeListener} to the returned ObservableList.
     * @return the list of selected indices
     */
    public abstract ObservableList<Integer> getSelectedIndices();

    /**
     * <p>Returns a <b>read-only</b> ObservableList of all selected items. The
     * ObservableList will be updated further by the selection model to always reflect
     * changes in selection. This can be observed by adding a
     * {@link ListChangeListener} to the returned ObservableList.
     * @return the list of selected items
     */
    public abstract ObservableList<T> getSelectedItems();

    /**
     * <p>This method allows for one or more selections to be set at the same time.
     * It will ignore any value that is not within the valid range (i.e. greater
     * than or equal to zero, and less than the total number of items in the
     * underlying data model). Any duplication of indices will be ignored.
     *
     * <p>If there is already one or more indices selected in this model, calling
     * this method will <b>not</b> clear these selections - to do so it is
     * necessary to first call clearSelection.
     *
     * <p>The last valid value given will become the selected index / selected
     * item.
     * @param index the first index to select
     * @param indices zero or more additional indices to select
     */
    public abstract void selectIndices(int index, int... indices);

    /**
     * <p>Selects all indices from the given start index to the item before the
     * given end index. This means that the selection is inclusive of the start
     * index, and exclusive of the end index. This method will work regardless
     * of whether start &lt; end or start &gt; end: the only constant is that the
     * index before the given end index will become the selected index.
     *
     * <p>If there is already one or more indices selected in this model, calling
     * this method will <b>not</b> clear these selections - to do so it is
     * necessary to first call clearSelection.
     *
     * @param start The first index to select - this index will be selected.
     * @param end The last index of the selection - this index will not be selected.
     */
    public void selectRange(final int start, final int end) {
        if (start == end) return;

        final boolean asc = start < end;
        final int low = asc ? start : end;      // Math.min(start, end);
        final int high = asc ? end : start;     //Math.max(start, end);
        final int arrayLength = high - low - 1;

        int[] indices = new int[arrayLength];

        int startValue = asc ? low : high;
        int firstVal = asc ? startValue++ : startValue--;
        for (int i = 0; i < arrayLength; i++) {
            indices[i] = asc ? startValue++ : startValue--;
        }
        selectIndices(firstVal, indices);
    }

    /**
     * <p>Convenience method to select all available indices.</p>
     */
    public abstract void selectAll();

    /**
     * <p>This method will attempt to select the first index in the control. If
     * clearSelection is not called first, this method
     * will have the result of selecting the first index, whilst retaining
     * the selection of any other currently selected indices.</p>
     *
     * <p>If the first index is already selected, calling this method will have
     * no result, and no selection event will take place.</p>
     */
    @Override public abstract void selectFirst();

    /**
     * <p>This method will attempt to select the last index in the control. If
     * clearSelection is not called first, this method
     * will have the result of selecting the last index, whilst retaining
     * the selection of any other currently selected indices.</p>
     *
     * <p>If the last index is already selected, calling this method will have
     * no result, and no selection event will take place.</p>
     */
    @Override public abstract void selectLast();
}
