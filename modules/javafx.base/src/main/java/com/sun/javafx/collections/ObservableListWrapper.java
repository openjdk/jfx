/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.collections;

import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

import com.sun.javafx.collections.NonIterableChange.SimplePermutationChange;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ModifiableObservableListBase;
import javafx.util.Callback;

/**
 * A List wrapper class that implements observability.
 *
 */
public class ObservableListWrapper<E> extends ModifiableObservableListBase<E> implements SortableList<E>, RandomAccess {

    private final List<E> backingList;
    private final ElementObserver<E> elementObserver;

    public ObservableListWrapper(List<E> list) {
        backingList = list;
        elementObserver = null;
    }

    public ObservableListWrapper(List<E> list, Callback<E, Observable[]> extractor) {
        backingList = list;
        this.elementObserver = new ElementObserver<>(extractor, new Callback<E, InvalidationListener>() {

            @Override
            public InvalidationListener call(final E e) {
                return new InvalidationListener() {

                    @Override
                    public void invalidated(Observable observable) {
                        beginChange();
                        int i = 0;
                        final int size = size();
                        for (; i < size; ++i) {
                            if (get(i) == e) {
                                nextUpdate(i);
                            }
                        }
                        endChange();
                    }
                };
            }
        }, this);
        final int sz = backingList.size();
        for (int i = 0; i < sz; ++i) {
            elementObserver.attachListener(backingList.get(i));
        }
    }


    @Override
    public E get(int index) {
        return backingList.get(index);
    }

    @Override
    public int size() {
        return backingList.size();
    }

    @Override
    protected void doAdd(int index, E element) {
        Objects.checkIndex(index, size() + 1);
        if (elementObserver != null)
            elementObserver.attachListener(element);
        backingList.add(index, element);
    }

    @Override
    protected E doSet(int index, E element) {
        E removed =  backingList.set(index, element);
        if (elementObserver != null) {
            elementObserver.detachListener(removed);
            elementObserver.attachListener(element);
        }
        return removed;
    }

    @Override
    protected E doRemove(int index) {
        E removed =  backingList.remove(index);
        if (elementObserver != null)
            elementObserver.detachListener(removed);
        return removed;
    }

    @Override
    public int indexOf(Object o) {
        return backingList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return backingList.lastIndexOf(o);
    }

    @Override
    public boolean contains(Object o) {
        return backingList.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backingList.containsAll(c);
    }

    @Override
    public void clear() {
        if (elementObserver != null) {
            final int sz = size();
            for (int i = 0; i < sz; ++i) {
                elementObserver.detachListener(get(i));
            }
        }
        if (hasListeners()) {
            beginChange();
            nextRemove(0, this);
        }
        backingList.clear();
        ++modCount;
        if (hasListeners()) {
            endChange();
        }
    }

    @Override
    public void remove(int fromIndex, int toIndex) {
        Objects.checkFromToIndex(fromIndex, toIndex, size());
        beginChange();
        for (int i = fromIndex; i < toIndex; ++i) {
            remove(fromIndex);
        }
        endChange();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        // implicit check to ensure c != null
        if (c.isEmpty() || backingList.isEmpty()) {
            return false;
        }

        beginChange();
        BitSet bs = new BitSet(c.size());
        for (int i = 0; i < size(); ++i) {
            if (c.contains(get(i))) {
                bs.set(i);
            }
        }
        if (!bs.isEmpty()) {
            int cur = size();
            while ((cur = bs.previousSetBit(cur - 1)) >= 0) {
                remove(cur);
            }
        }
        endChange();
        return !bs.isEmpty();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // implicit check to ensure c != null
        if (c.isEmpty() && !backingList.isEmpty()) {
            clear();
            return true;
        }

        if (backingList.isEmpty()) {
            return false;
        }

        beginChange();
        BitSet bs = new BitSet(c.size());
        for (int i = 0; i < size(); ++i) {
            if (!c.contains(get(i))) {
                bs.set(i);
            }
        }
        if (!bs.isEmpty()) {
            int cur = size();
            while ((cur = bs.previousSetBit(cur - 1)) >= 0) {
                remove(cur);
            }
        }
        endChange();
        return !bs.isEmpty();
    }

    private SortHelper helper;

    @Override
    public void doSort(Comparator<? super E> comparator) {
        int[] perm = getSortHelper().sort(backingList, comparator);
        fireChange(new SimplePermutationChange<>(0, size(), perm, this));
    }

    private SortHelper getSortHelper() {
        if (helper == null) {
            helper = new SortHelper();
        }
        return helper;
    }
}
