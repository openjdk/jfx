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

package javafx.scene.control;

import com.sun.javafx.collections.MappingChange;
import com.sun.javafx.collections.NonIterableChange;
import static javafx.scene.control.SelectionMode.SINGLE;

import java.util.AbstractList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import com.sun.javafx.scene.control.ReadOnlyUnbackedObservableList;


/**
 * An abstract class that implements more of the abstract MultipleSelectionModel 
 * abstract class. However, this class is package-protected and not intended
 * for public use.
 * 
 * @param <T> The type of the underlying data model for the UI control.
 */
abstract class MultipleSelectionModelBase<T> extends MultipleSelectionModel<T> {

    /***********************************************************************
     *                                                                     *
     * Constructors                                                        *
     *                                                                     *
     **********************************************************************/

    public MultipleSelectionModelBase() {
        selectedIndexProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                // we used to lazily retrieve the selected item, but now we just
                // do it when the selection changes. This is hardly likely to be
                // expensive, and we still lazily handle the multiple selection
                // cases over in MultipleSelectionModel.
                setSelectedItem(getModelItem(getSelectedIndex()));
            }
        });
        
        selectedIndices = new BitSet();

        selectedIndicesSeq = new ReadOnlyUnbackedObservableList<Integer>() {
            @Override public Integer get(int index) {
                if (index < 0 || index >= getItemCount()) return -1;

                for (int pos = 0, val = selectedIndices.nextSetBit(0);
                    val >= 0 || pos == index;
                    pos++, val = selectedIndices.nextSetBit(val+1)) {
                        if (pos == index) return val;
                }

                return -1;
            }

            @Override public int size() {
                return selectedIndices.cardinality();
            }

            @Override public boolean contains(Object o) {
                if (o instanceof Number) {
                    Number n = (Number) o;
                    int index = n.intValue();

                    return index > 0 && index < selectedIndices.length() &&
                            selectedIndices.get(index);
                }

                return false;
            }
        };
        
        final MappingChange.Map<Integer,T> map = new MappingChange.Map<Integer,T>() {
            @Override public T map(Integer f) {
                return getModelItem(f);
            }
        };
        
        selectedIndicesSeq.addListener(new ListChangeListener<Integer>() {
            @Override public void onChanged(final Change<? extends Integer> c) {
                // when the selectedIndices ObservableList changes, we manually call
                // the observers of the selectedItems ObservableList.
                selectedItemsSeq.callObservers(new MappingChange<Integer,T>(c, map, selectedItemsSeq));
                c.reset();
            }
        });


        selectedItemsSeq = new ReadOnlyUnbackedObservableList<T>() {
            @Override public T get(int i) {
                int pos = selectedIndicesSeq.get(i);
                return getModelItem(pos);
            }

            @Override public int size() {
                return selectedIndices.cardinality();
            }
        };
    }



    /***********************************************************************
     *                                                                     *
     * Observable properties                                               *
     *                                                                     *
     **********************************************************************/

    /*
     * We only maintain the values of the selectedIndex and selectedIndices
     * properties. The value of the selectedItem and selectedItems properties
     * is determined on-demand. We fire the SELECTED_ITEM and SELECTED_ITEMS
     * property change events whenever the related SELECTED_INDEX or
     * SELECTED_INDICES properties change.
     *
     * This means that the cost of the ListViewSelectionModel is cheap in most
     * cases, assuming that the end-consumer isn't calling getSelectedItems
     * too aggressively. Of course, this is only an issue when the ListViewModel
     * is being populated by some remote, expensive to query data source.
     *
     * In addition, we do not provide ObservableLists for the selected indices or the
     * selected items properties, as this would allow the API consumer to add
     * observers to these ObservableLists. This would make life tougher as we would
     * then be forced to keep these ObservableLists in-sync at all times, which for
     * the selectedItems ObservableList, would require potentially a lot of work and
     * memory. Instead, we return a List, and allow for changes to these Lists
     * to be observed through the SELECTED_INDICES and SELECTED_ITEMS
     * properties.
     */


    private final BitSet selectedIndices;
    private final ReadOnlyUnbackedObservableList<Integer> selectedIndicesSeq;
    @Override public ObservableList<Integer> getSelectedIndices() {
        return selectedIndicesSeq;
    }
//    private void setSelectedIndices(BitSet rows) {
//        this.selectedIndices.clear();
//        this.selectedIndices.or(rows);
//    }

    private final ReadOnlyUnbackedObservableList<T> selectedItemsSeq;
    @Override public ObservableList<T> getSelectedItems() {
        return selectedItemsSeq;
    }



    /***********************************************************************
     *                                                                     *
     * Internal field                                                      *
     *                                                                     *
     **********************************************************************/

    // Fix for RT-20945
    boolean makeAtomic = false;


    /***********************************************************************
     *                                                                     *
     * Public selection API                                                *
     *                                                                     *
     **********************************************************************/

    /**
     * Returns the number of items in the data model that underpins the control.
     * An example would be that a ListView selection model would likely return
     * <code>listView.getItems().size()</code>. The valid range of selectable
     * indices is between 0 and whatever is returned by this method.
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
    protected abstract void focus(int index);
    protected abstract int getFocusedIndex();
    
    
    // package only
    void shiftSelection(int position, int shift) {
        // with no check here, we get RT-15024
        if (position < 0) return;
        if (shift == 0) return;
        
        int selectedIndicesCardinality = selectedIndices.cardinality(); // number of true bits
        if (selectedIndicesCardinality == 0) return;
        
        int selectedIndicesSize = selectedIndices.size();   // number of bits reserved 
        
        int[] perm = new int[selectedIndicesSize];
        int idx = 0;
        
        if (shift > 0) {
            for (int i = selectedIndicesSize - 1; i >= position && i >= 0; i--) {
                boolean selected = selectedIndices.get(i);
                selectedIndices.clear(i);
                selectedIndices.set(i + shift, selected);

                if (selected) {
                    perm[idx++] = i + 1;
                }
            }
            selectedIndices.clear(position);
        } else if (shift < 0) {
            for (int i = position; i < selectedIndicesSize; i++) {
                if ((i + shift) < 0) continue;
                boolean selected = selectedIndices.get(i + 1);
                selectedIndices.clear(i + 1);
                selectedIndices.set(i + 1 + shift, selected);

                if (selected) {
                    perm[idx++] = i;
                }
            }
        }
        
        // This ensure that the selection remains accurate when a shift occurs.
        if (getFocusedIndex() >= position && getFocusedIndex() > -1 && getFocusedIndex() + shift > -1) {
            final int newFocus = getFocusedIndex() + shift;
            setSelectedIndex(newFocus);
 
            // removed due to RT-27185
            // focus(newFocus);
        }
         
        selectedIndicesSeq.callObservers(
                new NonIterableChange.SimplePermutationChange<Integer>(
                        0, 
                        selectedIndicesCardinality, 
                        perm, 
                        selectedIndicesSeq));
    }

    @Override public void clearAndSelect(int row) {
        // clear out all other selection quietly - so that we don't fire events
        quietClearSelection();

        // and select
        select(row);
    }

    @Override public void select(int row) {
        if (row == -1) {
            clearSelection();
            return;
        }
        if (row < 0 || row >= getItemCount()) {
            return;
        }
        
        boolean isSameRow = row == getSelectedIndex();
        T currentItem = getSelectedItem();
        T newItem = getModelItem(row);
        boolean isSameItem = newItem != null && newItem.equals(currentItem);
        boolean fireUpdatedItemEvent = isSameRow && ! isSameItem;

        if (! selectedIndices.get(row)) {
            if (getSelectionMode() == SINGLE) {
                quietClearSelection();
            }
            selectedIndices.set(row);
        }

        setSelectedIndex(row);
        focus(row);
        
        int changeIndex = selectedIndicesSeq.indexOf(row);
        selectedIndicesSeq.callObservers(new NonIterableChange.SimpleAddChange<Integer>(changeIndex, changeIndex+1, selectedIndicesSeq));
        
        if (fireUpdatedItemEvent) {
            setSelectedItem(newItem);
        }
    }

    @Override public void select(T obj) {
//        if (getItemCount() <= 0) return;
        
        // We have no option but to iterate through the model and select the
        // first occurrence of the given object. Once we find the first one, we
        // don't proceed to select any others.
        Object rowObj = null;
        for (int i = 0, max = getItemCount(); i < max; i++) {
            rowObj = getModelItem(i);
            if (rowObj == null) continue;

            if (rowObj.equals(obj)) {
                if (isSelected(i)) {
                    return;
                }

                if (getSelectionMode() == SINGLE) {
                    quietClearSelection();
                }

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

    @Override public void selectIndices(int row, int... rows) {
        if (rows == null) {
            select(row);
            return;
        }

        /*
         * Performance optimisation - if multiple selection is disabled, only
         * process the end-most row index.
         */

        int rowCount = getItemCount();

        if (getSelectionMode() == SINGLE) {
            quietClearSelection();

            for (int i = rows.length - 1; i >= 0; i--) {
                int index = rows[i];
                if (index >= 0 && index < rowCount) {
                    selectedIndices.set((int) index);
                    select(index);
                    break;
                }
            }

            if (selectedIndices.isEmpty()) {
                if (row > 0 && row < rowCount) {
                    selectedIndices.set((int) row);
                    select((int) row);
                }
            }

            selectedIndicesSeq.callObservers(new NonIterableChange.SimpleAddChange<Integer>(0, 1, selectedIndicesSeq));
        } else {
            int lastIndex = -1;
            if (row >= 0 && row < rowCount) {
                lastIndex = row;
                selectedIndices.set((int) row);
            }

            for (int i = 0; i < rows.length; i++) {
                int index = rows[i];
                if (index < 0 || index >= rowCount) continue;
                lastIndex = index;
                selectedIndices.set((int) index);
            }

            if (lastIndex != -1) {
                select(lastIndex);
            }

            if (rows.length == 0) {
                // TODO this isn't accurate
                selectedIndicesSeq.callObservers(new NonIterableChange.SimpleAddChange<Integer>((int) row, (int) row, selectedIndicesSeq));
            } else {
                // TODO this isn't accurate
                selectedIndicesSeq.callObservers(new NonIterableChange.SimpleAddChange<Integer>((int) row, (int) rows[rows.length - 1], selectedIndicesSeq));
            }
        }
    }

    @Override public void selectAll() {
        if (getSelectionMode() == SINGLE) return;

        quietClearSelection();
        if (getItemCount() <= 0) return;

        int rowCount = getItemCount();

        // set all selected indices to true
        quietClearSelection();
        selectedIndices.set(0, (int) rowCount, true);
        selectedIndicesSeq.callObservers(new NonIterableChange.SimpleAddChange<Integer>(0, (int) rowCount, selectedIndicesSeq));

        setSelectedIndex(rowCount - 1);

        focus(getSelectedIndex());
    }
    
    @Override public void selectFirst() {
        if (getSelectionMode() == SINGLE) {
            quietClearSelection();
        }
            
        if (getItemCount() > 0) {
            select(0);
        }
    }

    @Override public void selectLast() {
        if (getSelectionMode() == SINGLE) {
            quietClearSelection();
        }
            
        int numItems = getItemCount();
        if (numItems > 0 && getSelectedIndex() < numItems - 1) {
            select(numItems - 1);
        }
    }

    @Override public void clearSelection(int index) {
        if (index < 0) return;
        
        // TODO shouldn't directly access like this
        // TODO might need to update focus and / or selected index/item
        boolean wasEmpty = selectedIndices.isEmpty();
        selectedIndices.clear(index);
        
        if (! wasEmpty && selectedIndices.isEmpty()) {
            clearSelection();
        }

        selectedIndicesSeq.callObservers(
                new NonIterableChange.GenericAddRemoveChange<Integer>(index, index+1, 
                Collections.singletonList(index), selectedIndicesSeq));
    }

    @Override public void clearSelection() {
        if (! makeAtomic) {
            setSelectedIndex(-1);
            focus(-1);
        }

        if (! selectedIndices.isEmpty()) {
            List<Integer> removed = new AbstractList<Integer>() {
                final BitSet clone = (BitSet) selectedIndices.clone();

                @Override public Integer get(int index) {
                    return clone.nextSetBit(index);
                }

                @Override public int size() {
                    return clone.cardinality();
                }
            };

            quietClearSelection();
            
            selectedIndicesSeq.callObservers(
                    new NonIterableChange.GenericAddRemoveChange<Integer>(0, 0, 
                    removed, selectedIndicesSeq));
        }
    }

    private void quietClearSelection() {
        selectedIndices.clear();
    }

    @Override public boolean isSelected(int index) {
        if (index >= 0 && index < getItemCount()) {
            return selectedIndices.get(index);
        }

        return false;
    }

    @Override public boolean isEmpty() {
        return selectedIndices.isEmpty();
    }

    @Override public void selectPrevious() {
        int focusIndex = getFocusedIndex();

        if (getSelectionMode() == SINGLE) {
            quietClearSelection();
        }
        
        if (focusIndex == -1) {
            select(getItemCount() - 1);
        } else if (focusIndex > 0) {
            select(focusIndex - 1);
        }
    }

    @Override public void selectNext() {
        int focusIndex = getFocusedIndex();

        if (getSelectionMode() == SINGLE) {
            quietClearSelection();
        }

        if (focusIndex == -1) {
            select(0);
        } else if (focusIndex != getItemCount() -1) {
            select(focusIndex + 1);
        }
    }
    
    
}
