/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener.Change;
import javafx.util.Callback;

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
        selectedIndexProperty().addListener(valueModel -> {
            // we used to lazily retrieve the selected item, but now we just
            // do it when the selection changes. This is hardly likely to be
            // expensive, and we still lazily handle the multiple selection
            // cases over in MultipleSelectionModel.
            setSelectedItem(getModelItem(getSelectedIndex()));
        });
        
        selectedIndices = new BitSet();

        selectedIndicesSeq = createListFromBitSet(selectedIndices);
        
        final MappingChange.Map<Integer,T> map = f -> getModelItem(f);
        
        selectedIndicesSeq.addListener(new ListChangeListener<Integer>() {
            @Override public void onChanged(final Change<? extends Integer> c) {
                // when the selectedIndices ObservableList changes, we manually call
                // the observers of the selectedItems ObservableList.

                // Fix for a bug identified whilst fixing RT-37395:
                // We shouldn't fire events on the selectedItems list unless
                // the indices list has actually changed. This means that index
                // permutation events should not be forwarded blindly through the
                // items list, as a index permutation implies the items list is
                // unchanged, not changed!
                boolean hasRealChangeOccurred = false;
                while (c.next() && ! hasRealChangeOccurred) {
                    hasRealChangeOccurred = c.wasAdded() || c.wasRemoved();
                }

                if (hasRealChangeOccurred) {
                    if (selectedItemChange != null) {
                        selectedItemsSeq.callObservers(selectedItemChange);
                    } else {
                        c.reset();
                        selectedItemsSeq.callObservers(new MappingChange<Integer, T>(c, map, selectedItemsSeq));
                    }
                }
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


    final BitSet selectedIndices;
    final ReadOnlyUnbackedObservableList<Integer> selectedIndicesSeq;
    @Override public ObservableList<Integer> getSelectedIndices() {
        return selectedIndicesSeq;
    }

    private final ReadOnlyUnbackedObservableList<T> selectedItemsSeq;
    @Override public ObservableList<T> getSelectedItems() {
        return selectedItemsSeq;
    }



    /***********************************************************************
     *                                                                     *
     * Internal field                                                      *
     *                                                                     *
     **********************************************************************/

    ListChangeListener.Change selectedItemChange;

    // Fix for RT-20945 (and numerous other issues!)
    private int atomicityCount = 0;
    boolean isAtomic() {
        return atomicityCount > 0;
    }
    void startAtomic() {
        atomicityCount++;
    }
    void stopAtomic() {
        atomicityCount = Math.max(0, --atomicityCount);
    }


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
    
    static class ShiftParams {
        private final int clearIndex;
        private final int setIndex;
        private final boolean selected;
        
        ShiftParams(int clearIndex, int setIndex, boolean selected) {
            this.clearIndex = clearIndex;
            this.setIndex = setIndex;
            this.selected = selected;
        }
        
        public final int getClearIndex() {
            return clearIndex;
        }
        
        public final int getSetIndex() {
            return setIndex;
        }
        
        public final boolean isSelected() {
            return selected;
        }
    }
    
    // package only
    void shiftSelection(int position, int shift, final Callback<ShiftParams, Void> callback) {
        // with no check here, we get RT-15024
        if (position < 0) return;
        if (shift == 0) return;
        
        int selectedIndicesCardinality = selectedIndices.cardinality(); // number of true bits
        if (selectedIndicesCardinality == 0) return;
        
        int selectedIndicesSize = selectedIndices.size();   // number of bits reserved 
        
        int[] perm = new int[selectedIndicesSize];
        int idx = 0;
        boolean hasPermutated = false;
        
        if (shift > 0) {
            for (int i = selectedIndicesSize - 1; i >= position && i >= 0; i--) {
                boolean selected = selectedIndices.get(i);
                
                if (callback == null) {
                    selectedIndices.clear(i);
                    selectedIndices.set(i + shift, selected);
                } else {
                    callback.call(new ShiftParams(i, i + shift, selected));
                }

                if (selected) {
                    perm[idx++] = i + 1;
                    hasPermutated = true;
                }
            }
            selectedIndices.clear(position);
        } else if (shift < 0) {
            for (int i = position; i < selectedIndicesSize; i++) {
                if ((i + shift) < 0) continue;
                if ((i + 1 + shift) < position) continue;
                boolean selected = selectedIndices.get(i + 1);
                
                if (callback == null) {
                    selectedIndices.clear(i + 1);
                    selectedIndices.set(i + 1 + shift, selected);
                } else {
                    callback.call(new ShiftParams(i + 1, i + 1 + shift, selected));
                }

                if (selected) {
                    perm[idx++] = i;
                    hasPermutated = true;
                }
            }
        }
        
        // This ensure that the selection remains accurate when a shift occurs.
        final int selectedIndex = getSelectedIndex();
        if (selectedIndex >= position && selectedIndex > -1) {
            // Fix for RT-38787: we used to not enter this block if
            // selectedIndex + shift resulted in a value less than zero, whereas
            // now we just set the newSelectionLead to zero in that instance.
            // There exists unit tests that cover this.
            final int newSelectionLead = Math.max(0, selectedIndex + shift);

            setSelectedIndex(newSelectionLead);

            // added the selectedIndices call for RT-30356.
            // changed to check if hasPermutated, and to call select(..) for RT-40010.
            // This forces the selection event to go through the system and fire
            // the necessary events.
            if (hasPermutated) {
                selectedIndices.set(newSelectionLead, true);
            } else {
                select(newSelectionLead);
            }

            // removed due to RT-27185
//            focus(newSelectionLead);
        }

        if (hasPermutated) {
            selectedIndicesSeq.callObservers(
                new NonIterableChange.SimplePermutationChange<Integer>(
                        0, 
                        selectedIndicesCardinality, 
                        perm, 
                        selectedIndicesSeq));
        }
    }

    @Override public void clearAndSelect(int row) {
        if (row < 0 || row >= getItemCount()) {
            clearSelection();
            return;
        }

        final boolean wasSelected = isSelected(row);

        // RT-33558 if this method has been called with a given row, and that
        // row is the only selected row currently, then this method becomes a no-op.
        if (wasSelected && getSelectedIndices().size() == 1) {
            // before we return, we double-check that the selected item
            // is equal to the item in the given index
            if (getSelectedItem() == getModelItem(row)) {
                return;
            }
        }

        // firstly we make a copy of the selection, so that we can send out
        // the correct details in the selection change event.
        // We remove the new selection from the list seeing as it is not removed.
        BitSet selectedIndicesCopy = new BitSet();
        selectedIndicesCopy.or(selectedIndices);
        selectedIndicesCopy.clear(row);
        List<Integer> previousSelectedIndices = createListFromBitSet(selectedIndicesCopy);

        // RT-32411 We used to call quietClearSelection() here, but this
        // resulted in the selectedItems and selectedIndices lists never
        // reporting that they were empty.
        // makeAtomic toggle added to resolve RT-32618
        startAtomic();

        // then clear the current selection
        clearSelection();

        // and select the new row
        select(row);
        stopAtomic();

        // fire off a single add/remove/replace notification (rather than
        // individual remove and add notifications) - see RT-33324
        ListChangeListener.Change<Integer> change;

        /*
         * getFrom() documentation:
         *   If wasAdded is true, the interval contains all the values that were added.
         *   If wasPermutated is true, the interval marks the values that were permutated.
         *   If wasRemoved is true and wasAdded is false, getFrom() and getTo() should
         *   return the same number - the place where the removed elements were positioned in the list.
         */
        if (wasSelected) {
            change = ControlUtils.buildClearAndSelectChange(selectedIndicesSeq, previousSelectedIndices, row);
        } else {
            int changeIndex = selectedIndicesSeq.indexOf(row);
            change = new NonIterableChange.GenericAddRemoveChange<>(
                    changeIndex, changeIndex+1, previousSelectedIndices, selectedIndicesSeq);
        }

        selectedIndicesSeq.callObservers(change);
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

        startAtomic();
        if (! selectedIndices.get(row)) {
            if (getSelectionMode() == SINGLE) {
                quietClearSelection();
            }
            selectedIndices.set(row);
        }

        setSelectedIndex(row);
        focus(row);

        stopAtomic();

        if (! isAtomic()) {
            int changeIndex = selectedIndicesSeq.indexOf(row);
            selectedIndicesSeq.callObservers(new NonIterableChange.SimpleAddChange<Integer>(changeIndex, changeIndex+1, selectedIndicesSeq));
        }
        
        if (fireUpdatedItemEvent) {
            setSelectedItem(newItem);
        }
    }

    @Override public void select(T obj) {
//        if (getItemCount() <= 0) return;
        
        if (obj == null && getSelectionMode() == SelectionMode.SINGLE) {
            clearSelection();
            return;
        }
        
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
        setSelectedIndex(-1);
        setSelectedItem(obj);
    }

    @Override public void selectIndices(int row, int... rows) {
        if (rows == null || rows.length == 0) {
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
                    selectedIndices.set(index);
                    select(index);
                    break;
                }
            }

            if (selectedIndices.isEmpty()) {
                if (row > 0 && row < rowCount) {
                    selectedIndices.set(row);
                    select(row);
                }
            }

            selectedIndicesSeq.callObservers(new NonIterableChange.SimpleAddChange<Integer>(0, 1, selectedIndicesSeq));
        } else {
            final List<Integer> actualSelectedRows = new ArrayList<Integer>();
            
            int lastIndex = -1;
            if (row >= 0 && row < rowCount) {
                lastIndex = row;
                if (! selectedIndices.get(row)) {
                    selectedIndices.set(row);
                    actualSelectedRows.add(row);
                }
            }

            for (int i = 0; i < rows.length; i++) {
                int index = rows[i];
                if (index < 0 || index >= rowCount) continue;
                lastIndex = index;
                
                if (! selectedIndices.get(index)) {
                    selectedIndices.set(index);
                    actualSelectedRows.add(index);
                }
            }

            if (lastIndex != -1) {
                setSelectedIndex(lastIndex);
                focus(lastIndex);
                setSelectedItem(getModelItem(lastIndex));
            }

            // need to come up with ranges based on the actualSelectedRows, and
            // then fire the appropriate number of changes. We also need to
            // translate from a desired row to select to where that row is 
            // represented in the selectedIndices list. For example,
            // we may have requested to select row 5, and the selectedIndices
            // list may therefore have the following: [1,4,5], meaning row 5
            // is in position 2 of the selectedIndices list
            Collections.sort(actualSelectedRows);
            Change<Integer> change = createRangeChange(selectedIndicesSeq, actualSelectedRows);
            selectedIndicesSeq.callObservers(change);
        }
    }
    
    static Change<Integer> createRangeChange(final ObservableList<Integer> list, final List<Integer> addedItems) {
        Change<Integer> change = new Change<Integer>(list) {
            private final int[] EMPTY_PERM = new int[0];
            private final int addedSize = addedItems.size(); 
            
            private boolean invalid = true;
            
            private int pos = 0;
            private int from = pos;
            private int to = pos;
            
            @Override public int getFrom() {
                checkState();
                return from;
            }

            @Override public int getTo() {
                checkState();
                return to;
            }

            @Override public List<Integer> getRemoved() {
                checkState();
                return Collections.<Integer>emptyList();
            }

            @Override protected int[] getPermutation() {
                checkState();
                return EMPTY_PERM;
            }
            
            @Override public int getAddedSize() {
                return to - from;
            }

            @Override public boolean next() {
                if (pos >= addedSize) return false;
                
                // starting from pos, we keep going until the value is
                // not the next value
                int startValue = addedItems.get(pos++);
                from = list.indexOf(startValue);
                to = from + 1;
                int endValue = startValue;
                while (pos < addedSize) {
                    int previousEndValue = endValue;
                    endValue = addedItems.get(pos++);
                    ++to;
                    if (previousEndValue != (endValue - 1)) {
                        break;
                    }
                }

                if (invalid) {
                    invalid = false;
                    return true; 
                }
                
                // we keep going until we've represented all changes!
                return pos < addedSize;
            }

            @Override public void reset() {
                invalid = true;
                pos = 0;
            }
            
            private void checkState() {
                if (invalid) {
                    throw new IllegalStateException("Invalid Change state: next() must be called before inspecting the Change.");
                }
            }
            
        };
        return change;
    }

    @Override public void selectAll() {
        if (getSelectionMode() == SINGLE) return;

        if (getItemCount() <= 0) return;

        final int rowCount = getItemCount();
        final int focusedIndex = getFocusedIndex();

        // set all selected indices to true
        clearSelection();
        selectedIndices.set(0, rowCount, true);
        selectedIndicesSeq.callObservers(new NonIterableChange.SimpleAddChange<>(0, rowCount, selectedIndicesSeq));

        if (focusedIndex == -1) {
            setSelectedIndex(rowCount - 1);
            focus(rowCount - 1);
        } else {
            setSelectedIndex(focusedIndex);
            focus(focusedIndex);
        }
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

        if (!isAtomic()) {
            // we pass in (index, index) here to represent that nothing was added
            // in this change.
            selectedIndicesSeq.callObservers(
                    new NonIterableChange.GenericAddRemoveChange<>(index, index,
                            Collections.singletonList(index), selectedIndicesSeq));
        }
    }

    @Override public void clearSelection() {
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

        if (! isAtomic()) {
            setSelectedIndex(-1);
            focus(-1);
            selectedIndicesSeq.callObservers(
                    new NonIterableChange.GenericAddRemoveChange<>(0, 0,
                    removed, selectedIndicesSeq));
        }
    }

    private void quietClearSelection() {
        selectedIndices.clear();
    }

    @Override public boolean isSelected(int index) {
        // Note the change in semantics here - we used to check to ensure that
        // the index is less than the item count, but now simply ensure that
        // it is less than the length of the selectedIndices bitset. This helps
        // to resolve issues such as RT-26721, where isSelected(int) was being
        // called for indices that exceeded the item count, as a TreeItem (e.g.
        // the root) was being collapsed.
//        if (index >= 0 && index < getItemCount()) {
        if (index >= 0 && index < selectedIndices.length()) {
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



    /***********************************************************************
     *                                                                     *
     * Private implementation                                              *
     *                                                                     *
     **********************************************************************/

    private ReadOnlyUnbackedObservableList<Integer> createListFromBitSet(final BitSet bitset) {
        return new ReadOnlyUnbackedObservableList<Integer>() {
            @Override public Integer get(int index) {
                if (index < 0 || index >= getItemCount()) return -1;

                for (int pos = 0, val = bitset.nextSetBit(0);
                     val >= 0 || pos == index;
                     pos++, val = bitset.nextSetBit(val+1)) {
                    if (pos == index) return val;
                }

                return -1;
            }

            @Override public int size() {
                return bitset.cardinality();
            }

            @Override public boolean contains(Object o) {
                if (o instanceof Number) {
                    Number n = (Number) o;
                    int index = n.intValue();

                    return index >= 0 && index < bitset.length() &&
                            bitset.get(index);
                }

                return false;
            }
        };
    }
}
