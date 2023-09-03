/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.collections.transformation;

import com.sun.javafx.collections.NonIterableChange.GenericAddRemoveChange;
import com.sun.javafx.collections.SortHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

/**
 * Wraps an ObservableList and filters its content using the provided Predicate.
 * All changes in the ObservableList are propagated immediately
 * to the FilteredList.
 *
 * @see TransformationList
 * @since JavaFX 8.0
 */
public final class FilteredList<E> extends TransformationList<E, E>{

    private int[] filtered;
    private int size;

    private SortHelper helper;

    /**
     * Constructs a new FilteredList wrapper around the source list.
     * The provided predicate will match the elements in the source list that will be visible.
     * If the predicate is null, all elements will be matched and the list is equal to the source list.
     * @param source the source list
     * @param predicate the predicate to match the elements or null to match all elements.
     */
    public FilteredList(@NamedArg("source") ObservableList<E> source, @NamedArg("predicate") Predicate<? super E> predicate) {
        super(source);
        filtered = new int[source.size() * 3 / 2  + 1];
        if (predicate != null) {
            setPredicate(predicate);
        } else {
            for (size = 0; size < source.size(); size++) {
                filtered[size] = size;
            }
        }
    }

    /**
     * Constructs a new FilteredList wrapper around the source list.
     * This list has an "always true" predicate, containing all the elements
     * of the source list.
     * <p>
     * This constructor might be useful if you want to bind {@link #predicateProperty()}
     * of this list.
     * @param source the source list
     */
    public FilteredList(@NamedArg("source") ObservableList<E> source) {
        this(source, null);
    }

    /**
     * The predicate that will match the elements that will be in this FilteredList.
     * Elements not matching the predicate will be filtered-out.
     * Null predicate means "always true" predicate, all elements will be matched.
     */
    private ObjectProperty<Predicate<? super E>> predicate;

    public final ObjectProperty<Predicate<? super E>> predicateProperty() {
        if (predicate == null) {
            predicate = new ObjectPropertyBase<>() {
                @Override
                protected void invalidated() {
                    refilter();
                }

                @Override
                public Object getBean() {
                    return FilteredList.this;
                }

                @Override
                public String getName() {
                    return "predicate";
                }

            };
        }
        return predicate;
    }

    public final Predicate<? super E> getPredicate() {
        return predicate == null ? null : predicate.get();
    }

    public final void setPredicate(Predicate<? super E> predicate) {
        predicateProperty().set(predicate);
    }

    private Predicate<? super E> getPredicateImpl() {
        if (getPredicate() != null) {
            return getPredicate();
        }
        return t -> true;
    }

    @Override
    protected void sourceChanged(Change<? extends E> c) {
        beginChange();
        while (c.next()) {
            if (c.wasPermutated()) {
                permutate(c);
            } else if (c.wasUpdated()) {
                update(c);
            } else {
                addRemove(c);
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
        return getSource().get(filtered[index]);
    }

    @Override
    public int getSourceIndex(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return filtered[index];
    }

    @Override
    public int getViewIndex(int index) {
        return Arrays.binarySearch(filtered, 0, size, index);
    }

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

    private void addRemove(Change<? extends E> c) {
        Predicate<? super E> pred = getPredicateImpl();
        ensureSize(getSource().size());
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

        ListIterator<? extends E> it = getSource().listIterator(pos);
        for (; fpos < to && it.nextIndex() < c.getTo();) {
            if (pred.test(it.next())) {
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
                if (pred.test(it.next())) {
                    System.arraycopy(filtered, fpos, filtered, fpos + 1, size - fpos);
                    filtered[fpos] = it.previousIndex();
                    nextAdd(fpos, fpos + 1);
                    ++fpos;
                    ++size;
                }
                ++pos;
            }
        }
    }

    private void update(Change<? extends E> c) {
        Predicate<? super E> pred = getPredicateImpl();
        ensureSize(getSource().size());
        int sourceFrom = c.getFrom();
        int sourceTo = c.getTo();
        int filterFrom = findPosition(sourceFrom);
        int filterTo = findPosition(sourceTo);
        ListIterator<? extends E> it = getSource().listIterator(sourceFrom);
        int pos = filterFrom;
        while (pos < filterTo || sourceFrom < sourceTo) {
            E el = it.next();
            if (pos < size && filtered[pos] == sourceFrom) {
                if (!pred.test(el)) {
                    nextRemove(pos, el);
                    System.arraycopy(filtered, pos + 1, filtered, pos, size - pos - 1);
                    --size;
                    --filterTo;
                } else {
                    nextUpdate(pos);
                    ++pos;
                }
            } else {
                if (pred.test(el)) {
                    nextAdd(pos, pos + 1);
                    System.arraycopy(filtered, pos, filtered, pos + 1, size - pos);
                    filtered[pos] = sourceFrom;
                    ++size;
                    ++pos;
                    ++filterTo;
                }
            }
            sourceFrom++;
        }
    }

    private void refilter() {
        ensureSize(getSource().size());
        List<E> removed = null;
        if (hasListeners()) {
            removed = new ArrayList<>(this);
        }
        size = 0;
        int i = 0;
        Predicate<? super E> pred = getPredicateImpl();
        for (Iterator<? extends E> it = getSource().iterator();it.hasNext(); ) {
            final E next = it.next();
            if (pred.test(next)) {
                filtered[size++] = i;
            }
            ++i;
        }
        if (hasListeners()) {
            fireChange(new GenericAddRemoveChange<>(0, size, removed, this));
        }
    }

}
