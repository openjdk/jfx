/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.collections.SortHelper;
import com.sun.javafx.collections.transformation.FilterableList.FilterMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javafx.collections.ListChangeListener.Change;

/**
 * A wrapper implementation of {@link FilterableList}.
 * It wraps an ObservableList. All changes in the ObservableList are propagated immediately
 * to the FilteredList, however in {@link FilterMode#BATCH} mode, filter is not applied
 * to the new values, as opposed to the {@link FilterMode#LIVE} mode.
 * Removed elements are removed immediately in both modes.
 * 
 * Warning: {@link FilterMode#LIVE} mode expects the underlying list to contain immutable objects.
 * Object mutation (while object is in the list) will result in broken FilteredList!
 * 
 * {@link FilterMode#BATCH} mode however, is capable of handling mutable objects.
 * 
 *
 * @see TransformationList
 */
public final class FilteredList<E> extends TransformationList<E, E> implements FilterableList<E>{

    private FilterMode mode;
    private Matcher<? super E> matcher;

    private int[] filtered;
    private int size;
    
    private boolean matcherChanged; // BATCH mode only

    private SortHelper helper;

    private SortHelper getSortHelper() {
        if (helper == null) {
            helper = new SortHelper();
        }
        return helper;
    }

    private int findPosition(int p) {
        if (filtered.length == 0) {
            return 0;
        }
        if (p == 0) {
            return 0;
        }
        int pos = Arrays.binarySearch(filtered, 0, size, p);
        if (pos < 0 ) {
            pos = ~pos;
        }
        return pos;
    }


    @SuppressWarnings("unchecked")
    private void ensureSize(int size) {
        if (filtered.length < size) {
            int[] replacement = new int[size * 3/2 + 1];
            System.arraycopy(filtered, 0, replacement, 0, this.size);
            filtered = replacement;
        }
    }

    private void updateIndexes(int from, int delta) {
        for (int i = from; i < size; ++i) {
            filtered[i] += delta;
        }
    }

    /**
     * Constructs a new FilteredList wrapper around the source list.
     * The matcher provided will match the elements in the source list that will be visible. 
     * If the mode will be the {@link FilterMode#BATCH} mode, the FilteredList will
     * contain all elements from the source list until the {@link #filter()} is call for 
     * the first time.
     * @param source the source list 
     * @param matcher the matcher to match the elements. Cannot be null.
     * @param mode the selected mode
     */
    public FilteredList(List<E> source, Matcher<? super E> matcher, FilterMode mode) {
        super(source);
        if (matcher == null) {
            throw new NullPointerException();
        }
        this.matcher = matcher;
        this.mode = mode;
        filtered = new int[source.size() * 3 / 2  + 1];
        if (mode == FilterMode.LIVE) {
            if (!observable) {
                throw new IllegalArgumentException("Cannot use LIVE mode with list that is not an ObservableList");
            }
            refilter();
        } else {
            size = source.size();
            for (int i = 0 ; i < size; ++i) {
                filtered[i] = i;
            }
        }
        
    }

    /**
     * Constructs a new FilteredList wrapper around the source list using the {@link FilterMode#BATCH} mode.
     * @param source the source list
     * @param matcher the matcher to match the elements. Cannot be null.
     * @see #FilteredList(java.util.List, javafx.collections.Matcher, javafx.collections.FilterableList.FilterMode) 
     */
    public FilteredList(List<E> source, Matcher<? super E> matcher) {
        this(source, matcher, FilterMode.LIVE);
    }

    private void permutate(Change<? extends E> c) {
        int from = findPosition(c.getFrom());
        int to = findPosition(c.getTo());

        if (to > from) {
            for (int i = from; i < to; ++i) {
                filtered[i] = c.getPermutation(filtered[i]);
            }

            int[] perm = getSortHelper().sort(filtered, from, to);
            nextPermutation(from, to, perm);
        }
    }

    private void update(Change<? extends E> c) {
        int from = findPosition(c.getFrom());
        int to = findPosition(c.getTo());

        // NOTE: this is sub-optimal, as we may mark some Nodes as "updated" even though
        // they will be removed from the list in the next step
        for (int i = from; i < to; ++i) {
            nextUpdate(i);
        }

        updateFilter(c.getFrom(), c.getTo());
    }

    private void addRemove(Change<? extends E> c, boolean filter) {
        ensureSize(source.size());
        final int from = findPosition(c.getFrom());
        final int to = findPosition(c.getFrom() + c.getRemovedSize());

        // Mark the nodes that are going to be removed
        for (int i = from; i < to; ++i) {
            nextRemove(from, c.getRemoved().get(filtered[i] - c.getFrom()));
        }

        // Update indexes of the sublist following the last element that was removed
        updateIndexes(to, c.getAddedSize() - c.getRemovedSize());

        // Replace as many removed elements as possible
        int fpos = from;
        int pos = c.getFrom();

        if (filter) {
            ListIterator<? extends E> it = source.listIterator(pos);
            for (; fpos < to && it.nextIndex() < c.getTo();) {
                if (matcher.matches(it.next())) {
                    filtered[fpos] = it.previousIndex();
                    nextAdd(fpos, fpos + 1);
                    ++fpos;
                }
            }

            if (fpos < to) {
                // If there were more removed elements than added
                System.arraycopy(filtered, to, filtered, fpos, size - to);
                size -= to - fpos;
            } else {
                // Add the remaining elements
                while (it.nextIndex() < c.getTo()) {
                    if (matcher.matches(it.next())) {
                        System.arraycopy(filtered, fpos, filtered, fpos + 1, size - fpos);
                        filtered[fpos] = it.previousIndex();
                        nextAdd(fpos, fpos + 1);
                        ++fpos;
                        ++size;
                    }
                    ++pos;
                }
            }
        } else {
            for (; fpos < to && pos < c.getTo(); ++fpos, ++pos) {
                filtered[fpos] = pos;
                nextAdd(fpos, fpos + 1);
            }

            if (fpos < to) {
                // If there were more removed elements than added
                System.arraycopy(filtered, to, filtered, fpos, size - to);
                size -= to - fpos;
            } else {
                // Add the remaining elements
                while (pos < c.getTo()) {
                    System.arraycopy(filtered, fpos, filtered, fpos + 1, size - fpos);
                    filtered[fpos] = pos;
                    nextAdd(fpos, fpos + 1);
                    ++fpos;
                    ++size;
                    ++pos;
                }
            }
        }
    }

    @Override
    protected void onSourceChanged(Change<? extends E> c) {
        beginChange();
        if (mode == FilterMode.BATCH) {
            while (c.next()) {
                if (c.wasPermutated()) {
                    permutate(c);
                } else if (!c.wasUpdated()) {
                    addRemove(c, false);
                }
            }
        } else {
            while (c.next()) {
                if (c.wasPermutated()) {
                    permutate(c);
                } else if (c.wasUpdated()) {
                    update(c);
                } else {
                    addRemove(c, true);
                }
            }
        }
        endChange();
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return size;
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
        return source.get(filtered[index]);
    }

    private void updateFilter(int sourceFrom, int sourceTo) {
        beginChange();
        // Fast path for single element update
        if (sourceFrom == sourceTo - 1) {
            int pos = findPosition(sourceFrom);
            final E sourceFromElement = source.get(sourceFrom);
            if (filtered[pos] == sourceFrom) {
                if (!matcher.matches(sourceFromElement)) {
                    nextRemove(pos, sourceFromElement);
                    System.arraycopy(filtered, pos + 1, filtered, pos, size - pos - 1);
                    --size;
                }
            } else {
                ensureSize(source.size());
                if (matcher.matches(sourceFromElement)) {
                    nextAdd(pos, pos + 1);
                    System.arraycopy(filtered, pos, filtered, pos + 1, size - pos);
                    filtered[pos] = sourceFrom;
                    ++size;
                }
            }
        } else {
            ensureSize(source.size());
            int filterFrom = findPosition(sourceFrom);
            int filterTo = findPosition(sourceTo);

            int i = filterFrom; // The index that traverses filtered[] array

            if (i == 0) {
                // Look at the beginning
                final int jTo = size == 0 ? sourceTo : filtered[0];
                final ListIterator<? extends E> it = source.listIterator(sourceFrom);
                for (; it.nextIndex() < jTo;) {
                    E el = it.next();
                    if (matcher.matches(el)) {
                        nextAdd(i, i + 1);
                        System.arraycopy(filtered, i, filtered, i + 1, size - i);
                        filtered[i] = it.previousIndex();
                        size++;
                        filterTo++;
                        i++;
                    }
                }
            }


            // Now traverse the rest of the list. We first check the item in the filtered
            // array, if it still matches the filter
            ListIterator<? extends E> it = source.listIterator(filtered[i]);
            for (; i < filterTo; ++i) {
                advanceTo(it, filtered[i]);
                final E el = it.next();
                if (!matcher.matches(el)) {
                    nextRemove(i, el);
                    System.arraycopy(filtered, i + 1, filtered, i, size - i - 1);
                    size--;
                    filterTo--;
                    i--;
                }
                final int jTo = (i == filterTo - 1 ? sourceTo : filtered[i + 1]);
                // Then we look at the elements that are between the current element in filtered[] array
                // and it's successor
                while (it.nextIndex() < jTo) {
                    final E midEl = it.next();
                    if (matcher.matches(midEl)) {
                        nextAdd(i + 1, i + 2);
                        System.arraycopy(filtered, i + 1, filtered, i + 2, size - i - 1);
                        filtered[i + 1] = it.previousIndex();
                        size++;
                        filterTo++;
                        i++;
                    }
                }
            }

        }
        endChange();
    }

    private static void advanceTo(ListIterator<?> it, int index) {
        while(it.nextIndex() < index) {
            it.next();
        }
    }
    
    @SuppressWarnings("unchecked")
    private void refilter() {
        ensureSize(source.size());
        List<E> removed = null;
        if (hasListChangeListener()) {
            removed = new ArrayList<E>(this);
        }
        size = 0;
        int i = 0;
        for (Iterator<? extends E> it = source.iterator();it.hasNext(); ) {
            final E next = it.next();
            if (matcher.matches(next)) {
                filtered[size++] = i;
            }
            ++i;
        }
        if (hasListChangeListener()) {
            callObservers(new GenericAddRemoveChange<E>(0, size, removed, this));
        }
    }
    
    @Override
    public void filter() {
        if (mode == FilterMode.BATCH) {
            if (matcherChanged) {
                refilter();
                matcherChanged = false;
                return;
            }

            updateFilter(0, source.size());
        }
    }

    @Override
    public void setMode(FilterMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            if (mode == FilterMode.LIVE) {
                filter();
            }
        }
    }

    @Override
    public FilterMode getMode() {
        return mode;
    }

    @Override
    public void setMatcher(Matcher<? super E> m) {
        if (this.matcher != m) {
            this.matcher = m;
            if (mode == FilterMode.LIVE) {
                refilter();
            } else {
                matcherChanged = true;
            }
        }
    }

    @Override
    public Matcher<? super E> getMatcher() {
        return matcher;
    }

    @Override
    public int getSourceIndex(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return filtered[index];
    }

}
