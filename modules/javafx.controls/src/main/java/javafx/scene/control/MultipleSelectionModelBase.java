/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.NonIterableChange;
import static javafx.scene.control.SelectionMode.SINGLE;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sun.javafx.scene.control.MultipleAdditionAndRemovedChange;
import com.sun.javafx.scene.control.ReadOnlyUnbackedObservableList;
import com.sun.javafx.scene.control.SelectedItemsReadOnlyObservableList;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.util.Callback;

import javafx.util.Pair;


/**
 * An abstract class that implements more of the abstract MultipleSelectionModel
 * abstract class. However, this class is package-protected and not intended
 * for public use.
 *
 * @param <T> The type of the underlying data model for the UI control.
 */
abstract class MultipleSelectionModelBase<T> extends MultipleSelectionModel<T> {

    /* *********************************************************************
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

        selectedIndices = new SelectedIndicesList();

        selectedItems = new SelectedItemsReadOnlyObservableList<>(selectedIndices, () -> getItemCount()) {
            @Override protected T getModelItem(int index) {
                return MultipleSelectionModelBase.this.getModelItem(index);
            }
        };
    }



    /* *********************************************************************
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


    final SelectedIndicesList selectedIndices;
    @Override public ObservableList<Integer> getSelectedIndices() {
        return selectedIndices;
    }

    private final ObservableListBase<T> selectedItems;
    @Override public ObservableList<T> getSelectedItems() {
        return selectedItems;
    }



    /* *********************************************************************
     *                                                                     *
     * Internal field                                                      *
     *                                                                     *
     **********************************************************************/

    ListChangeListener.Change selectedItemChange;



    /* *********************************************************************
     *                                                                     *
     * Public selection API                                                *
     *                                                                     *
     **********************************************************************/

    /**
     * Returns the number of items in the data model that underpins the control.
     * An example would be that a ListView selection model would likely return
     * <code>listView.getItems().size()</code>. The valid range of selectable
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
     * Focuses the item at the given index.
     * @param index the index of the item to be focused
     */
    protected abstract void focus(int index);

    /**
     * Gets the index of the focused item.
     * @return the index of the focused item
     */
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
        shiftSelection(Arrays.asList(new Pair<>(position, shift)), callback);
    }

    void shiftSelection(List<Pair<Integer, Integer>> shifts, final Callback<ShiftParams, Void> callback) {
        int selectedIndicesCardinality = selectedIndices.size(); // number of true bits
        if (selectedIndicesCardinality == 0) return;

        int selectedIndicesSize = selectedIndices.bitsetSize();   // number of bits reserved

        int[] perm = new int[selectedIndicesSize];
        Arrays.fill(perm, -1);

        // sort the list so that we iterate from highest position to lowest position
        Collections.sort(shifts, (s1, s2) -> Integer.compare(s2.getKey(), s1.getKey()));
        final int lowestShiftPosition = shifts.get(shifts.size() - 1).getKey();

        // make a copy of the selectedIndices before so we can compare to it afterwards
        BitSet selectedIndicesCopy = (BitSet) selectedIndices.bitset.clone();

        startAtomic();
        for (Pair<Integer, Integer> shift : shifts) {
            doShift(shift, callback, perm);
        }
        stopAtomic();

        // strip out all useless -1 default values from the perm array
        final int[] prunedPerm = Arrays.stream(perm).filter(value -> value > -1).toArray();
        final boolean hasSelectionChanged = prunedPerm.length > 0;

        // This ensure that the selection remains accurate when a shift occurs.
        final int selectedIndex = getSelectedIndex();
        if (selectedIndex >= lowestShiftPosition && selectedIndex > -1) {
            // sum up the total shift, where the position is less than or equal
            // to the previously selected index
            int totalShift = shifts.stream()
                    .filter(shift -> shift.getKey() <= selectedIndex)
                    .mapToInt(shift -> shift.getValue())
                    .sum();

            // Fix for RT-38787: we used to not enter this block if
            // selectedIndex + shift resulted in a value less than zero, whereas
            // now we just set the newSelectionLead to zero in that instance.
            // There exists unit tests that cover this.
            final int newSelectionLead = Math.max(0, selectedIndex + totalShift);

            setSelectedIndex(newSelectionLead);

            // added the selectedIndices call for RT-30356.
            // changed to check if hasPermutated, and to call select(..) for RT-40010.
            // This forces the selection event to go through the system and fire
            // the necessary events.
            if (hasSelectionChanged) {
                selectedIndices.set(newSelectionLead, true);
            } else {
                select(newSelectionLead);
            }

            // removed due to RT-27185
//            focus(newSelectionLead);
        }

        if (hasSelectionChanged) {
            // work out what indices were removed and added
            BitSet removed = (BitSet) selectedIndicesCopy.clone();
            removed.andNot(selectedIndices.bitset);

            BitSet added = (BitSet) selectedIndices.bitset.clone();
            added.andNot(selectedIndicesCopy);

            selectedIndices.reset();
            selectedIndices.callObservers(new MultipleAdditionAndRemovedChange<>(
                    added.stream().boxed().collect(Collectors.toList()),
                    removed.stream().boxed().collect(Collectors.toList()),
                    selectedIndices
            ));
        }
    }

    private void doShift(Pair<Integer, Integer> shiftPair, final Callback<ShiftParams, Void> callback, int[] perm) {
        final int position = shiftPair.getKey();
        final int shift = shiftPair.getValue();

        // with no check here, we get RT-15024
        if (position < 0) return;
        if (shift == 0) return;

        int idx = (int) Arrays.stream(perm).filter(value -> value > -1).count();

        int selectedIndicesSize = selectedIndices.bitsetSize() - idx;   // number of bits reserved

        if (shift > 0) {
            for (int i = selectedIndicesSize - 1; i >= position && i >= 0; i--) {
                boolean selected = selectedIndices.isSelected(i);

                if (callback == null) {
                    selectedIndices.clear(i);
                    selectedIndices.set(i + shift, selected);
                } else {
                    callback.call(new ShiftParams(i, i + shift, selected));
                }

                if (selected) {
                    perm[idx++] = i + 1;
                }
            }
            selectedIndices.clear(position);
        } else if (shift < 0) {
            for (int i = position; i < selectedIndicesSize; i++) {
                if ((i + shift) < 0) continue;
                if ((i + 1 + shift) < position) continue;
                boolean selected = selectedIndices.isSelected(i + 1);

                if (callback == null) {
                    selectedIndices.clear(i + 1);
                    selectedIndices.set(i + 1 + shift, selected);
                } else {
                    callback.call(new ShiftParams(i + 1, i + 1 + shift, selected));
                }

                if (selected) {
                    perm[idx++] = i;
                }
            }
        }
    }

    void startAtomic() {
        selectedIndices.startAtomic();
    }

    void stopAtomic() {
        selectedIndices.stopAtomic();
    }

    boolean isAtomic() {
        return selectedIndices.isAtomic();
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
        selectedIndicesCopy.or(selectedIndices.bitset);
        selectedIndicesCopy.clear(row);
        // No modifications should be made to 'selectedIndicesCopy' to honour the constructor.
        List<Integer> previousSelectedIndices = new SelectedIndicesList(selectedIndicesCopy);

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
            change = ControlUtils.buildClearAndSelectChange(
                    selectedIndices, previousSelectedIndices, row, Comparator.naturalOrder());
        } else {
            int changeIndex = Math.max(0, selectedIndices.indexOf(row));
            change = new NonIterableChange.GenericAddRemoveChange<>(
                    changeIndex, changeIndex+1, previousSelectedIndices, selectedIndices);
        }

        selectedIndices.callObservers(change);
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

        // focus must come first so that we have the anchors set appropriately
        focus(row);

        if (! selectedIndices.isSelected(row)) {
            if (getSelectionMode() == SINGLE) {
                startAtomic();
                quietClearSelection();
                stopAtomic();
            }
            selectedIndices.set(row);
        }

        setSelectedIndex(row);

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
        } else {
            selectedIndices.set(row, rows);

            IntStream.concat(IntStream.of(row), IntStream.of(rows))
                     .filter(index -> index >= 0 && index < rowCount)
                     .reduce((first, second) -> second)
                     .ifPresent(lastIndex -> {
                         setSelectedIndex(lastIndex);
                         focus(lastIndex);
                         setSelectedItem(getModelItem(lastIndex));
                     });
        }
    }

    @Override public void selectAll() {
        if (getSelectionMode() == SINGLE) return;

        if (getItemCount() <= 0) return;

        final int rowCount = getItemCount();
        final int focusedIndex = getFocusedIndex();

        // set all selected indices to true
        clearSelection();
        selectedIndices.set(0, rowCount, true);

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
    }

    @Override public void clearSelection() {
        quietClearSelection();

        if (! isAtomic()) {
            setSelectedIndex(-1);
            focus(-1);
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
        if (index >= 0 && index < selectedIndices.bitsetSize()) {
            return selectedIndices.isSelected(index);
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



    /* *********************************************************************
     *                                                                     *
     * Private implementation                                              *
     *                                                                     *
     **********************************************************************/

    class SelectedIndicesList extends ReadOnlyUnbackedObservableList<Integer> {
        private final BitSet bitset;

        private int size = -1;
        private int lastGetIndex = -1;
        private int lastGetValue = -1;

        // Fix for RT-20945 (and numerous other issues!)
        private int atomicityCount = 0;

//        @Override
//        public void callObservers(Change<Integer> c) {
//            throw new RuntimeException("callObservers unavailable");
//        }

        /**
         * Constructs a new instance of SelectedIndicesList
         */
        public SelectedIndicesList() {
            this(new BitSet());
        }

        /**
         * Constructs a new instance of SelectedIndicesList from the provided BitSet.
         * The underlying source BitSet shouldn't be modified once it has been passed to the constructor.
         * @param bitset Bitset to be used.
         */
        public SelectedIndicesList(BitSet bitset) {
            this.bitset = bitset;
        }

        boolean isAtomic() {
            return atomicityCount > 0;
        }
        void startAtomic() {
            atomicityCount++;
        }
        void stopAtomic() {
            atomicityCount = Math.max(0, atomicityCount - 1);
        }

        // Returns the selected index at the given index.
        // e.g. if our selectedIndices are [1,3,5], then an index of 2 will return 5 here.
        @Override public Integer get(int index) {
            final int itemCount = size();
            if (index < 0 || index >= itemCount)  {
                throw new IndexOutOfBoundsException(index + " >= " + itemCount);
            }
            if (lastGetIndex == index) {
                return lastGetValue;
            } else if (index == (lastGetIndex + 1) && lastGetValue < itemCount) {
                // we're iterating forward in order, short circuit for
                // performance reasons (RT-39776)
                lastGetIndex++;
                lastGetValue = bitset.nextSetBit(lastGetValue + 1);
                return lastGetValue;
            } else if (index == (lastGetIndex - 1) && lastGetValue > 0) {
                // we're iterating backward in order, short circuit for
                // performance reasons (RT-39776)
                lastGetIndex--;
                lastGetValue = bitset.previousSetBit(lastGetValue - 1);
                return lastGetValue;
            } else {
                for (lastGetIndex = 0, lastGetValue = bitset.nextSetBit(0);
                     lastGetValue >= 0 || lastGetIndex == index;
                     lastGetIndex++, lastGetValue = bitset.nextSetBit(lastGetValue + 1)) {
                    if (lastGetIndex == index) {
                        return lastGetValue;
                    }
                }
            }

            return -1;
        }

        public void set(int index) {
            if (!isValidIndex(index) || isSelected(index)) {
                return;
            }

            _beginChange();
            size = -1;
            bitset.set(index);
            if (index <= lastGetValue) reset();
            int indicesIndex = indexOf(index);
            _nextAdd(indicesIndex, indicesIndex + 1);
            _endChange();
        }

        private boolean isValidIndex(int index) {
            return index >= 0 && index < getItemCount();
        }

        public void set(int index, boolean isSet) {
            if (isSet) {
                set(index);
            } else {
                clear(index);
            }
        }

        public void set(int index, int end, boolean isSet) {
            _beginChange();
            size = -1;
            if (isSet) {
                bitset.set(index, end, isSet);
                if (index <= lastGetValue) reset();
                int indicesIndex = indexOf(index);
                int span = end - index;
                _nextAdd(indicesIndex, indicesIndex + span);
            } else {
                // TODO handle remove
                bitset.set(index, end, isSet);
                if (index <= lastGetValue) reset();
            }
            _endChange();
        }

        public void set(int index, int... indices) {
            if (indices == null || indices.length == 0) {
                set(index);
            } else {
                // we reduce down to the minimal number of changes possible
                // by finding all contiguous indices, of all indices that are
                // not already selected, and which are in the valid range
                startAtomic();
                List<Integer> sortedNewIndices =
                        IntStream.concat(IntStream.of(index), IntStream.of(indices))
                        .distinct()
                        .filter(this::isValidIndex)
                        .filter(this::isNotSelected)
                        .sorted()
                        .boxed()
                        .peek(this::set) // we also set here, but it's atomic!
                        .collect(Collectors.toList());
                stopAtomic();

                final int size = sortedNewIndices.size();
                if (size == 0) {
                    // no-op
                } else if (size == 1) {
                    _beginChange();
                    int _index = sortedNewIndices.get(0);
                    int indicesIndex = indexOf(_index);
                    _nextAdd(indicesIndex, indicesIndex + 1);
                    _endChange();
                } else {
                    _beginChange();

                    int startIndex = indexOf(sortedNewIndices.get(0));
                    int endIndex = startIndex + 1;

                    for (int i = 1; i < sortedNewIndices.size(); ++i) {
                        int currentValue = get(endIndex);
                        int currentNewValue = sortedNewIndices.get(i);
                        if (currentValue != currentNewValue) {
                            _nextAdd(startIndex, endIndex);
                            while (get(endIndex) != currentNewValue) ++endIndex;
                            startIndex = endIndex++;
                        } else {
                            ++endIndex;
                        }
                        if (i == sortedNewIndices.size() - 1) {
                            _nextAdd(startIndex, endIndex);
                        }
                    }

                    _endChange();
                }
            }
        }

        @Override
        public void clear() {
            _beginChange();
            List<Integer> removed = bitset.stream().boxed().collect(Collectors.toList());
            size = 0;
            bitset.clear();
            reset();
            _nextRemove(0, removed);
            _endChange();
        }

        public void clear(int index) {
            if (!bitset.get(index)) return;

            int indicesIndex = indexOf(index);
            _beginChange();
            size = -1;
            bitset.clear(index);
            if (index <= lastGetValue) reset();
            _nextRemove(indicesIndex, index);
            _endChange();
        }

        public boolean isSelected(int index) {
            return bitset.get(index);
        }

        public boolean isNotSelected(int index) {
            return !isSelected(index);
        }

        /** Returns number of true bits in BitSet */
        @Override public int size() {
            if (size >= 0) {
                return size;
            }
            size = bitset.cardinality();
            return size;
        }

        /** Returns the number of bits reserved in the BitSet */
        public int bitsetSize() {
            return bitset.size();
        }

        @Override public int indexOf(Object obj) {
            if (!(obj instanceof Number)) {
                return -1;
            }
            Number n = (Number) obj;
            int index = n.intValue();
            if (!bitset.get(index)) {
                return -1;
            }

            // is left most bit
            if (index == 0) {
                return 0;
            }

            // is right most bit
            if (index == bitset.length() - 1) {
                return size() - 1;
            }

            // count right bit
            if (index > bitset.length() / 2) {
                int count = 1;
                for (int i = bitset.nextSetBit(index+1); i >= 0; i = bitset.nextSetBit(i+1)) {
                    count++;
                }
                return size() - count;
            }

            // count left bit
            int count = 0;
            for (int i = bitset.previousSetBit(index-1);  i >= 0; i = bitset.previousSetBit(i-1)) {
                count++;
            }
            return count;
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

        public void reset() {
            this.lastGetIndex = -1;
            this.lastGetValue = -1;
        }

        @Override public void _beginChange() {
            if (!isAtomic()) {
                super._beginChange();
            }
        }

        @Override public void _endChange() {
            if (!isAtomic()) {
                super._endChange();
            }
        }

        @Override public final void _nextUpdate(int pos) {
            if (!isAtomic()) {
                nextUpdate(pos);
            }
        }

        @Override public final void _nextSet(int idx, Integer old) {
            if (!isAtomic()) {
                nextSet(idx, old);
            }
        }

        @Override public final void _nextReplace(int from, int to, List<? extends Integer> removed) {
            if (!isAtomic()) {
                nextReplace(from, to, removed);
            }
        }

        @Override public final void _nextRemove(int idx, List<? extends Integer> removed) {
            if (!isAtomic()) {
                nextRemove(idx, removed);
            }
        }

        @Override public final void _nextRemove(int idx, Integer removed) {
            if (!isAtomic()) {
                nextRemove(idx, removed);
            }
        }

        @Override public final void _nextPermutation(int from, int to, int[] perm) {
            if (!isAtomic()) {
                nextPermutation(from, to, perm);
            }
        }

        @Override public final void _nextAdd(int from, int to) {
            if (!isAtomic()) {
                nextAdd(from, to);
            }
        }
    }
}
