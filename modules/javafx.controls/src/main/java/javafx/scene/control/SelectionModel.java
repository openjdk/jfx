/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;

/**
 * SelectionModel is an abstract class used by UI controls to provide a
 * consistent API for maintaining selection.
 *
 * @param <T> The type of the item contained in the control that can be selected.
 * @since JavaFX 2.0
 */
public abstract class SelectionModel<T> {

    /***************************************************************************
     *                                                                         *
     * Selection Properties                                                    *
     *                                                                         *
     **************************************************************************/

    /**
     * <p>Refers to the selected index property, which is used to indicate
     * the currently selected index value in the selection model. The selected
     * index is either -1,
     * to represent that there is no selection, or an integer value that is within
     * the range of the underlying data model size.
     *
     * <p>The selected index property is most commonly used when the selection
     * model only allows single selection, but is equally applicable when in
     * multiple selection mode. When in this mode, the selected index will always
     * represent the last selection made.
     *
     * <p>Note that in the case of multiple selection, it is possible to add
     * a {@link ListChangeListener} to the collection returned by
     * {@link MultipleSelectionModel#getSelectedIndices()} to be informed whenever
     * the selection changes, and this will also work in the case of single selection.
     * @return the selected index property
     */
    public final ReadOnlyIntegerProperty selectedIndexProperty() { return selectedIndex.getReadOnlyProperty(); }
    private ReadOnlyIntegerWrapper selectedIndex = new ReadOnlyIntegerWrapper(this, "selectedIndex", -1);
    protected final void setSelectedIndex(int value) { selectedIndex.set(value); }

    /**
     * <p>Returns the integer value indicating the currently selected index in
     * this model. If there are multiple items selected, this will return the
     * most recent selection made.
     *
     * <p>Note that the returned value is a snapshot in time - if you wish to
     * observe the selection model for changes to the selected index, you can
     * add a ChangeListener as such:
     *
     * <pre><code>
     * SelectionModel sm = ...;
     * InvalidationListener listener = ...;
     * sm.selectedIndexProperty().addListener(listener);
     * </code></pre>
     * @return the selected index
     */
    public final int getSelectedIndex() { return selectedIndexProperty().get(); }

    /**
     * <p>Refers to the selected item property, which is used to indicate
     * the currently selected item in the selection model. The selected item is
     * either null,
     * to represent that there is no selection, or an Object that is retrieved
     * from the underlying data model of the control the selection model is
     * associated with.
     *
     * <p>The selected item property is most commonly used when the selection
     * model is set to be single selection, but is equally applicable when in
     * multiple selection mode. When in this mode, the selected item will always
     * represent the last selection made.
     * @return the selected item property
     */
    public final ReadOnlyObjectProperty<T> selectedItemProperty() { return selectedItem.getReadOnlyProperty(); }
    private ReadOnlyObjectWrapper<T> selectedItem = new ReadOnlyObjectWrapper<T>(this, "selectedItem");
    protected final void setSelectedItem(T value) { selectedItem.set(value); }

    /**
     * Returns the currently selected object (which resides in the selected index
     * position). If there are multiple items selected, this will return the
     * object contained at the index returned by getSelectedIndex() (which is
     * always the index to the most recently selected item).
     *
     * <p>Note that the returned value is a snapshot in time - if you wish to
     * observe the selection model for changes to the selected item, you can
     * add a ChangeListener as such:
     *
     * <pre><code>
     * SelectionModel sm = ...;
     * InvalidationListener listener = ...;
     * sm.selectedItemProperty().addListener(listener);
     * </code></pre>
     * @return the selected item
     */
    public final T getSelectedItem() { return selectedItemProperty().get(); }


    /***************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default SelectionModel instance.
     */
    public SelectionModel() { }


    /***************************************************************************
     *                                                                         *
     * Selection API                                                           *
     *                                                                         *
     **************************************************************************/


    /**
     * A method that clears any selection prior to setting the selection to the
     * given index. The purpose of this method is to avoid having to call
     * {@link #clearSelection()} first, meaning that observers that are listening to
     * the {@link #selectedIndexProperty() selected index} property will not
     * see the selected index being temporarily set to -1.
     *
     * @param index The index that should be the only selected index in this
     *      selection model.
     */
    public abstract void clearAndSelect(int index);

    /**
     * <p>This will select the given index in the selection model, assuming the
     * index is within the valid range (i.e. greater than or equal to zero, and
     * less than the total number of items in the underlying data model).
     *
     * <p>If there is already one or more indices selected in this model, calling
     * this method will <b>not</b> clear these selections - to do so it is
     * necessary to first call {@link #clearSelection()}.
     *
     * <p>If the index is already selected, it will not be selected again, or
     * unselected. However, if multiple selection is implemented, then calling
     * select on an already selected index will have the effect of making the index
     * the new selected index (as returned by {@link #getSelectedIndex()}.
     *
     * @param index The position of the item to select in the selection model.
     */
    public abstract void select(int index);

    /**
     * <p>This method will attempt to select the index that contains the given
     * object. It will iterate through the underlying data model until it finds
     * an item whose value is equal to the given object. At this point it will
     * stop iterating - this means that this method will not select multiple
     * indices.
     *
     * @param obj The object to attempt to select in the underlying data model.
     */
    public abstract void select(T obj);

    /**
     * <p>This method will clear the selection of the item in the given index.
     * If the given index is not selected, nothing will happen.
     *
     * @param index The selected item to deselect.
     */
    public abstract void clearSelection(int index);

    /**
     * <p>Clears the selection model of all selected indices.
     */
    public abstract void clearSelection();

    /**
     * <p>Convenience method to inform if the given index is currently selected
     * in this SelectionModel. Is functionally equivalent to calling
     * <code>getSelectedIndices().contains(index)</code>.
     *
     * @param index The index to check as to whether it is currently selected
     *      or not.
     * @return True if the given index is selected, false otherwise.
     */
    public abstract boolean isSelected(int index);

    /**
     * This method is available to test whether there are any selected
     * indices/items. It will return true if there are <b>no</b> selected items,
     * and false if there are.
     *
     * @return Will return true if there are <b>no</b> selected items, and false
     *          if there are.
     */
    public abstract boolean isEmpty();

    /**
     * <p>This method will attempt to select the index directly before the current
     * focused index. If clearSelection is not called first, this method
     * will have the result of selecting the previous index, whilst retaining
     * the selection of any other currently selected indices.</p>
     *
     * <p>Calling this method will only succeed if:</p>
     *
     * <ul>
     *   <li>There is currently a lead/focused index.
     *   <li>The lead/focus index is not the first index in the control.
     *   <li>The previous index is not already selected.
     * </ul>
     *
     * <p>If any of these conditions is false, no selection event will take
     * place.</p>
     */
    public abstract void selectPrevious();

    /**
     * <p>This method will attempt to select the index directly after the current
     * focused index. If clearSelection is not called first, this method
     * will have the result of selecting the next index, whilst retaining
     * the selection of any other currently selected indices.</p>
     *
     * <p>Calling this method will only succeed if:</p>
     *
     * <ul>
     *   <li>There is currently a lead/focused index.
     *   <li>The lead/focus index is not the last index in the control.
     *   <li>The next index is not already selected.
     * </ul>
     *
     * <p>If any of these conditions is false, no selection event will take
     * place.</p>
     */
    public abstract void selectNext();

    /**
     * <p>This method will attempt to select the first index in the control. If
     * clearSelection is not called first, this method
     * will have the result of selecting the first index, whilst retaining
     * the selection of any other currently selected indices.</p>
     *
     * <p>If the first index is already selected, calling this method will have
     * no result, and no selection event will take place.</p>
     */
    public abstract void selectFirst();

    /**
     * <p>This method will attempt to select the last index in the control. If
     * clearSelection is not called first, this method
     * will have the result of selecting the last index, whilst retaining
     * the selection of any other currently selected indices.</p>
     *
     * <p>If the last index is already selected, calling this method will have
     * no result, and no selection event will take place.</p>
     */
    public abstract void selectLast();
}
