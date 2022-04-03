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

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * The abstract base class for FocusModel implementations.
 * @since JavaFX 2.0
 */
public abstract class FocusModel<T> {

    /* *********************************************************************
     *                                                                     *
     * Constructors                                                        *
     *                                                                     *
     **********************************************************************/

    /**
     * Creates a default FocusModel instance.
     */
    public FocusModel() {
        focusedIndexProperty().addListener(valueModel -> {
            // we used to lazily retrieve the focused item, but now we just
            // do it when the focused index changes.
            setFocusedItem(getModelItem(getFocusedIndex()));
        });
    }



    /* *************************************************************************
     *                                                                         *
     * Focus Properties                                                        *
     *                                                                         *
     **************************************************************************/

    /**
     * The index of the current item in the FocusModel which has the focus. It
     * is possible that this will be -1, but only if the control is empty.
     * If the control is not itself focused, this property will still
     * reference the row index that would receive the keyboard focus if the control
     * itself were focused.
     */
    private ReadOnlyIntegerWrapper focusedIndex = new ReadOnlyIntegerWrapper(this, "focusedIndex", -1);
    public final ReadOnlyIntegerProperty focusedIndexProperty() { return focusedIndex.getReadOnlyProperty();  }
    public final int getFocusedIndex() { return focusedIndex.get(); }
    final void setFocusedIndex(int value) { focusedIndex.set(value); }



    /**
     * The current item in the FocusModel which has the focus. It
     * is possible that this will be null, but only if the control is empty.
     * If the control is not itself focused, this property will still
     * reference the item that would receive the keyboard focus if the control
     * itself were focused.
     */
    private ReadOnlyObjectWrapper<T> focusedItem = new ReadOnlyObjectWrapper<T>(this, "focusedItem");
    public final ReadOnlyObjectProperty<T> focusedItemProperty() { return focusedItem.getReadOnlyProperty(); }
    public final T getFocusedItem() { return focusedItemProperty().get(); }
    final void setFocusedItem(T value) { focusedItem.set(value); }



    /* *********************************************************************
     *                                                                     *
     * Public Focus API                                                    *
     *                                                                     *
     **********************************************************************/


    /**
     * Returns the number of items in the data model that underpins the control.
     * An example would be that a ListView focus model would likely return
     * <code>listView.getItems().size()</code>. The valid range of focusable
     * indices is between 0 and whatever is returned by this method.
     * @return the number of items in the data model that underpins the control
     */
    protected abstract int getItemCount();

    /**
     * Returns the item at the given index. An example using ListView would be
     * <code>listView.getItems().get(index)</code>.
     *
     * @param index The index of the item that is requested from the underlying
     *      data model.
     * @return Returns null if the index is out of bounds, or an element of type
     *      T that is related to the given index.
     */
    protected abstract T getModelItem(int index);

    /**
     * <p>Convenience method to inform if the given index is currently focused
     * in this SelectionModel. Is functionally equivalent to calling
     * <pre><code>getFocusedIndex() == index</code></pre>.
     *
     * @param index The index to check as to whether it is currently focused
     *      or not.
     * @return True if the given index is focused, false otherwise.
     */
    public boolean isFocused(int index) {
        if (index < 0 || index >= getItemCount()) return false;

        return getFocusedIndex() == index;
    }

    /**
     * Causes the item at the given index to receive the focus. This does not
     * cause the current selection to change. Updates the focusedItem and
     * focusedIndex properties such that <code>focusedIndex = -1</code> unless
     * <code>0 &lt;= index &lt; model size</code>.
     *
     * @param index The index of the item to get focus.
     */
    public void focus(int index) {
        if (index < 0 || index >= getItemCount()) {
            setFocusedIndex(-1);
        } else {
            int oldFocusIndex = getFocusedIndex();
            setFocusedIndex(index);

            if (oldFocusIndex == index) {
                // manually update the focus item to ensure consistency
                setFocusedItem(getModelItem(index));
            }
        }
    }

    /**
     * Attempts to give focus to the row previous to the currently focused row.
     * If the current focus owner is the first row, or is -1 (representing that
     * there is no current focus owner), calling this method will have no result.
     */
    public void focusPrevious() {
        if (getFocusedIndex() == -1) {
            focus(0);
        } else if (getFocusedIndex() > 0) {
            focus(getFocusedIndex() - 1);
        }
    }

    /**
     * Attempts to give focus to the row after to the currently focused row.
     * If the current focus owner is the last row, calling this method will have
     * no result.
     */
    public void focusNext() {
        if (getFocusedIndex() == -1) {
            focus(0);
        } else if (getFocusedIndex() != getItemCount() -1) {
            focus(getFocusedIndex() + 1);
        }
    }
}
