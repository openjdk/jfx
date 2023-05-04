/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.collections.WeakListChangeListener;
import java.util.Objects;

public final class ConcatenatedObservableList<E> extends ObservableListBase<E> {

    private final ObservableList<? extends E>[] lists;
    private final ListChangeListener<? super E> listChangeListener;
    private int size;

    public ConcatenatedObservableList(ObservableList<? extends E>[] lists) {
        this.lists = lists.clone(); // implicit null check of lists array
        this.listChangeListener = this::onListChanged;

        ListChangeListener<? super E> weakListChangeListener = new WeakListChangeListener<>(listChangeListener);
        for (ObservableList<? extends E> list : lists) {
            list.addListener(weakListChangeListener); // implicit null check of list
            size = addSize(list.size());
        }
    }

    private int computeListOffset(ObservableList<? extends E> list) {
        int offset = 0;

        for (ObservableList<? extends E> l : lists) {
            if (l == list) {
                return offset;
            }

            offset += l.size();
        }

        throw new IllegalArgumentException("list");
    }

    private void onListChanged(ListChangeListener.Change<? extends E> change) {
        int listOffset = computeListOffset(change.getList());

        while (change.next()) {
            beginChange();

            if (change.wasPermutated()) {
                onPermutated(change, listOffset);
            } else if (change.wasUpdated()) {
                onUpdated(change, listOffset);
            } else if (change.wasReplaced()) {
                onReplaced(change, listOffset);
            } else if (change.wasRemoved()) {
                onRemoved(change, listOffset);
            } else if (change.wasAdded()) {
                onAdded(change, listOffset);
            }

            endChange();
        }
    }

    private void onPermutated(ListChangeListener.Change<? extends E> change, int listOffset) {
        int from = change.getFrom();
        int to = change.getTo();

        if (listOffset == 0 && change instanceof NonIterableChange.SimplePermutationChange<?> permChange) {
            nextPermutation(from, to, permChange.getPermutation());
        } else {
            int[] perm = new int[to - from];

            for (int i = 0, oldIndex = change.getFrom(), max = change.getTo(); oldIndex < max; ++i, ++oldIndex) {
                int newIndex = change.getPermutation(oldIndex);
                perm[i] = newIndex + listOffset;
            }

            nextPermutation(from, to, perm);
        }
    }

    private void onUpdated(ListChangeListener.Change<? extends E> change, int listOffset) {
        for (int index = change.getFrom(), max = change.getTo(); index < max; ++index) {
            nextUpdate(listOffset + index);
        }
    }

    private void onReplaced(ListChangeListener.Change<? extends E> change, int listOffset) {
        int from = change.getFrom(), to = change.getTo();
        size = addSize(to - from - change.getRemovedSize());
        nextReplace(listOffset + from, listOffset + to, change.getRemoved());
    }

    private void onRemoved(ListChangeListener.Change<? extends E> change, int listOffset) {
        size -= change.getRemovedSize();
        nextRemove(change.getFrom() + listOffset, change.getRemoved());
    }

    private void onAdded(ListChangeListener.Change<? extends E> change, int listOffset) {
        int from = change.getFrom(), to = change.getTo();
        size = addSize(to - from);
        nextAdd(from + listOffset, to + listOffset);
    }

    @Override
    public E get(int index) {
        Objects.checkIndex(index, size);
        int newIndex = index;

        for (ObservableList<? extends E> list : lists) {
            int listSize = list.size();
            if (newIndex < listSize) {
                return list.get(newIndex);
            }

            newIndex -= listSize;
        }

        // Can't happen because the index was already validated by Objects.checkIndex
        throw new AssertionError();
    }

    @Override
    public int size() {
        return size;
    }

    private int addSize(int size) {
        int r = this.size + size;
        if (((this.size ^ r) & (size ^ r)) < 0) {
            throw new IndexOutOfBoundsException("Maximum list size exceeded");
        }

        return r;
    }

}
