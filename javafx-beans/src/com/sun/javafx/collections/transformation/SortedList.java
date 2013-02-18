/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.collections.transformation;

import com.sun.javafx.collections.NonIterableChange.GenericAddRemoveChange;
import com.sun.javafx.collections.NonIterableChange.SimpleAddChange;
import com.sun.javafx.collections.NonIterableChange.SimplePermutationChange;
import com.sun.javafx.collections.NonIterableChange.SimpleRemovedChange;
import java.util.ArrayList;
import com.sun.javafx.collections.SortHelper;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javafx.collections.ListChangeListener.Change;
import com.sun.javafx.collections.transformation.SortableList.SortMode;

/**
 * A wrapper implementation of {@link SortableList}.
 * It wraps an ObservableList. All changes in the ObservableList are propagated immediately
 * to the SortedList, however in {@link SortMode#BATCH} mode, new values are appended to the list,
 * as opposed to the {@link SortMode#LIVE} mode which sorts them immediately into the list.
 * Removed elements are removed immediately in both modes.
 *
 * Note: invalid SortedList (as a result of broken comparison) doesn't send any notification to listeners on becoming
 * valid again.
 * 
 * Warning: {@link SortMode#LIVE} mode expects the underlying list to contain immutable objects.
 * Object mutation (while object is in the list) will result in broken SortedList!
 * 
 * {@link SortMode#BATCH} mode however, is capable of handling mutable objects.
 * 
 * @see TransformationList
 */
public final class SortedList<E> extends TransformationList<E, E> implements SortableList<E>{

    private SortMode mode;
    private ElementComparator<E> comparator;
    private Element<E>[] sorted;
    private int size;

    private boolean comparisonFailed;
    private SortHelper helper; 
    
    private Element<E> tempElement = new Element<E>(null, -1);
    
    
    private SortHelper getSortHelper() {
        if (helper == null) {
            helper = new SortHelper();
        }
        return helper;
    }

    private void updatePermutationIndexes(Change<? extends E> change) {
        for (int i = 0; i < size; ++i) {
            sorted[i].index = change.getPermutation(sorted[i].index);
        }
    }

    private static class Element<E> {

        public Element(E e, int index) {
            this.e = e;
            this.index = index;
        }
        
        private E e;
        private int index;
        private boolean removeFlag;
    }
    
    private static class ElementComparator<E> implements Comparator<Element<E>> {

        private Comparator<? super E> comparator;
        
        public ElementComparator(Comparator<? super E> comparator) {
            this.comparator = comparator;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public int compare(Element<E> o1, Element<E> o2) {
            if (comparator == null) {
                if (o1.e == null && o2.e == null) {
                    return 0;
                }
                if (o1.e == null) {
                    return -1;
                }
                if (o2.e == null) {
                    return 1;
                }

                if (o1.e instanceof Comparable) {
                    return ((Comparable) o1.e).compareTo(o2.e);
                }

                return Collator.getInstance().compare(o1.e.toString(), o2.e.toString());
            }
            return comparator.compare(o1.e, o2.e);
        }
        
    }

    @SuppressWarnings("unchecked")
    private void ensureSize(int size) {
        if (sorted.length < size) {
            Element<E>[] replacement = new Element[size * 3/2 + 1];
            System.arraycopy(sorted, 0, replacement, 0, this.size);
            sorted = (Element<E>[]) replacement;
        }
    }

    private int findPosition(E e) {
        if (sorted.length == 0) {
            return 0;
        }
        tempElement.e = e;
        int pos = Arrays.binarySearch(sorted, 0, size, tempElement, comparator);
        return pos;
    }
    
    private int compare(E e1, E e2) {
        return comparator.comparator == null ? ((Comparable)e1).compareTo(e2) :
                comparator.comparator.compare(e1, e2);
    }
    
    private int findPosition(int from, E e) {
        if (mode == SortMode.BATCH) {
            for (int i = 0; i < size; ++i) {
                if (sorted[i].index == from) {
                    return i;
                }
            }
            return -1;
        }
        int pos = findPosition(e);
        if (sorted[pos].index == from) {
            return pos;
        }
        int tmp = pos;
        while (sorted[--tmp].index != from && compare(sorted[tmp].e, e) == 0);
        if (sorted[tmp].index == from) {
            return tmp;
        }
        tmp = pos;
        while (sorted[++tmp].index != from && compare(sorted[tmp].e, e) == 0);
        if (sorted[tmp].index == from) {
            return tmp;
        }
        return -1;
    }

    private void insertUnsorted(int from, int to){
        ensureSize(size + to - from);
        updateIndices(from, to - from);
        for (int i = from; i < to; ++i) {
            sorted[size + i - from] = new Element<E>(source.get(i), i);
        }
        size += to - from;
        fireChange(new SimpleAddChange<E>(size - to + from, size, this));
    }

    private void insertOneSorted(E e, int idx) {
        int pos = findPosition(e);
        if (pos < 0) {
            pos = ~pos;
        }
        ensureSize(size + 1);
        updateIndices(idx, 1);
        System.arraycopy(sorted, pos, sorted, pos + 1, size - pos);
        sorted[pos] = new Element<E>(e, idx);
        ++size;
        fireChange(new SimpleAddChange<E>(pos, pos + 1, this));
        
    }

    private void removeOne(int from, E e) {
        int pos = findPosition(from, e);
        System.arraycopy(sorted, pos + 1, sorted, pos, size - pos - 1);
        --size;
        updateIndices(from + 1, - 1);
        fireChange(new SimpleRemovedChange<E>(pos, pos, e, this));
    }

    private void updateUnsorted(Change<? extends E> c) {
        // We know that c.getRemovedSize() > 1
        List<E> toBeRemoved = new ArrayList<E>();
        List<? extends E> removedList = c.getRemoved();
        int lo = Integer.MAX_VALUE, hi = - 1;
        for (int i = 0; i < c.getRemovedSize(); ++i) {
            E e = removedList.get(i);
            int pos = findPosition(c.getFrom() + i, e);
            if (pos < lo) {
                lo = pos;
            }
            if (pos + 1 > hi) {
                hi = pos + 1;
            }
            sorted[pos].removeFlag = true;
        }
        if (hi == -1) {
            lo = hi = 0;
        }
        
        for (int i = lo; i < hi; ++i) {
            toBeRemoved.add(sorted[i].e);
        }
        
        // Now do the actual update
        for (int i = lo; i < hi; ++i) {
            if (sorted[i].removeFlag) {
                System.arraycopy(sorted, i + 1, sorted, i, size - i - 1);
                --size; // after removing one element, the size is smaller
                --hi;   // we are iterating to smaller index
                --i;    // and we repeat over current i once more
            }
        }
        
        updateIndices(c.getFrom() + c.getRemovedSize(), c.getAddedSize() - c.getRemovedSize());
        if (c.wasAdded()) {
            ensureSize(size + c.getAddedSize());
            // And add the new elements to the end of this range
            System.arraycopy(sorted, hi, sorted, hi + c.getAddedSize(), size - hi);
            size += c.getAddedSize();
            for (int i = c.getFrom(); i < c.getTo(); ++i) {
                sorted[hi++] = new Element<E>(c.getList().get(i), i);
            }
        }
        
        fireChange(new GenericAddRemoveChange<E>(lo, hi, Collections.unmodifiableList(toBeRemoved), this));
    } 
    
    private void updateSorted(Change<? extends E> c) {
        // We know that c.getRemovedSize() > 1
        List<E> toBeRemoved = new ArrayList<E>();
        List<? extends E> removedList = c.getRemoved();
        int lo = Integer.MAX_VALUE, hi = - 1;
        for (int i = 0; i < c.getRemovedSize(); ++i) {
            E e = removedList.get(i);
            int pos = findPosition(c.getFrom() + i, e);
            if (pos < lo) {
                lo = pos;
            }
            if (pos + 1 > hi) {
                hi = pos + 1;
            }
            sorted[pos].removeFlag = true;
        }
        
        if (hi == -1) {
            lo = hi = 0;
        }
        
        for (int i = c.getFrom(); i < c.getTo(); ++i) {
            int pos = findPosition(c.getList().get(i));
            if (pos < 0) {
                pos = ~pos;
            }
            if (pos < lo) {
                lo = pos;
            }
            if (pos > hi) {
                hi = pos;
            }
        }
        
        for (int i = lo; i < hi; ++i) {
            toBeRemoved.add(sorted[i].e);
        }
        
        // Now do the actual update
        for (int i = lo; i < hi; ++i) {
            if (sorted[i].removeFlag) {
                System.arraycopy(sorted, i + 1, sorted, i, size - i - 1);
                --size; // after removing one element, the size is smaller
                --hi;   // we are iterating to smaller index
                --i;    // and we repeat over current i once more
            }
        }
        
        updateIndices(c.getFrom() + c.getRemovedSize(), c.getAddedSize() - c.getRemovedSize());
        if (c.wasAdded()) {
            ensureSize(size + c.getAddedSize());
            // And add the new elements
            for (int i = c.getFrom(); i < c.getTo(); ++i) {
                int pos = findPosition(c.getList().get(i));
                if (pos < 0) {
                    pos = ~pos;
                }
                System.arraycopy(sorted, pos, sorted, pos + 1, size - pos);
                sorted[pos] = new Element<E>(c.getList().get(i), i);
                ++size;
                ++hi;
            }
        }
        fireChange(new GenericAddRemoveChange<E>(lo, hi, Collections.unmodifiableList(toBeRemoved), this));
    }
    
    private void updateIndices(int from, int difference) {
        for (int i = 0 ; i < size; ++i) {
            if (sorted[i].index >= from) {
                sorted[i].index += difference;
            }
        }
    }

    /**
     * Creates a new SortedList wrapped around the source list. 
     * The source list will be sorted using the comparator provided. If null is provided, a natural
     * ordering of the elements is used if possible. Otherwise, the SortedList tries to sort the elements
     * by their toString().
     * The {@link SortMode#LIVE} mode can be used only if the provided list is an observable list.
     * Using the {@link SortMode#BATCH} mode, the sorting will be done upon the first {@link #sort()} call.
     * @param source a list to wrap
     * @param comparator a comparator to use or null if natural ordering is required
     * @param mode a mode to use
     */
    @SuppressWarnings("unchecked")
    public SortedList(List<? extends E> source, Comparator<? super E> comparator, SortMode mode) {
        super(source);
        if (mode == SortMode.LIVE && !observable) {
            throw new IllegalArgumentException("Cannot create live mode SortedList with list that is not an ObservableList");
        }
        this.comparator = new ElementComparator<E>(comparator);
        this.mode = mode;
        sorted = (Element<E>[]) new Element[source.size() *3/2 + 1];
        size = source.size();
        for (int i = 0; i < size; ++i) {
            sorted[i] = new Element<E>(source.get(i), i);
        }
        if (mode == SortMode.LIVE) {
            try {
                doArraysSort();
            } catch (ClassCastException e) {
                comparisonFailed = true;
            }
        }

    }

    /**
     * Constructs a new SortedList wrapper around the source list.
     * The mode will be the {@link SortMode#LIVE} mode and provided comparator will be used
     * @param source the source list
     * @param comparator a comparator to use or null if natural ordering is required
     * @see #SortedList(java.util.List, java.util.Comparator, javafx.collections.SortableList.SortMode) 
     */
    public SortedList(List<E> source, Comparator<? super E> comparator) {
        this(source, comparator, SortMode.LIVE);
    }

    /**
     * Constructs a new SortedList wrapper around the source list.
     * The mode will be the {@link SortMode#LIVE} mode and the natural ordering will be used.
     * @param source the source list
     * @see #SortedList(java.util.List, java.util.Comparator, javafx.collections.SortableList.SortMode) 
     */
    public SortedList(List<E> source) {
        this(source, null, SortMode.LIVE);
    }


    @Override
    protected void onSourceChanged(Change<? extends E> c) {
        if (comparisonFailed) {
            if (mode == SortMode.LIVE) {
                try {
                    resort();
                } catch (ClassCastException e) {
                    // cannot do anything, just wait for list to be fixed or comparator to be set
                }
            }
            return;
        }
        if (c.wasPermutated()) {
            updatePermutationIndexes(c);
            return;
        }
        
        if (mode == SortMode.BATCH) {
            if (c.wasAdded() && !c.wasRemoved()) {
                insertUnsorted(c.getFrom(), c.getTo());
            } else {
                updateUnsorted(c);
            }
        } else {
            try {
                if (c.wasAdded() && !c.wasRemoved() && c.getAddedSize() == 1) {
                    insertOneSorted(c.getList().get(c.getFrom()), c.getFrom());
                } else if (c.wasRemoved() && c.getRemovedSize() == 1) {
                    removeOne(c.getFrom(), c.getRemoved().get(0));
                } else {
                    updateSorted(c);
                }
            } catch (ClassCastException e) {
                comparisonFailed = true;
                throw e;
            }
        }
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param  index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public E get(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException();
        }
        if (comparisonFailed) {
            throw new ClassCastException("Cannot use natural comparison as underlying list contains element(s) that are not Comparable");
        }
        return sorted[index].e;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        if (comparisonFailed) {
            throw new ClassCastException("Cannot use natural comparison as underlying list contains element(s) that are not Comparable");
        }
        return size;
    }

    @Override
    public void sort() {
        if (comparisonFailed) {
            resort();
        } else if (mode == SortMode.BATCH) {
            try {
                doSortWithPermutationChange();
            } catch (ClassCastException e) {
                comparisonFailed = true;
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void doArraysSort() {
        Arrays.sort(sorted, 0, size, comparator);
    }
    
    private void doSortWithPermutationChange() {
        int[] perm = getSortHelper().sort(sorted, 0, size, comparator);
        fireChange(new SimplePermutationChange<E>(0, size, perm, this));
    }

    @Override
    public void setMode(SortMode mode) {
        if (this.mode != mode) {
            if (mode == SortMode.LIVE && !observable) {
                throw new IllegalArgumentException("Cannot switch to LIVE mode. A source list is not an ObservableList");
            }
            this.mode = mode;
            if (mode == SortMode.LIVE) {
                sort();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void resort() {
        ensureSize(source.size());
        source.toArray(sorted);
        size = source.size();
        doArraysSort();
        comparisonFailed = false;
    }

    @Override
    public SortMode getMode() {
        return mode;
    }

    /**
     * Change the current comparator to Comparator c.
     * Passing null will make element comparison using natural ordering,
     * providing the elements implements Comparable.
     * If elements are not Comparable and no Comparator is provided, String returned 
     * by toString() is used for comparison.
     * @param c the new comparator or null if natural ordering is required
     * @see #getComparator
     */
    @Override
    public void setComparator(Comparator<? super E> c) {
        this.comparator = new ElementComparator<E>(c);
        if (mode == SortMode.LIVE) {
            try {
                if (comparisonFailed) {
                    resort();
                } else {
                    doSortWithPermutationChange();
                }
            } catch (ClassCastException e) {
                comparisonFailed = true;
            }
        }
    }

    @Override
    public Comparator<? super E> getComparator() {
        return comparator.comparator;
    }

    @Override
    public int getSourceIndex(int index) {
        return sorted[index].index;
    }


}
