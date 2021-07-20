/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class SelectedItemsReadOnlyObservableList<E> extends ObservableListBase<E> {

    // This is the actual observable list of selected indices used in the selection model
    private final ObservableList<Integer> selectedIndices;
    private final Supplier<Integer> modelSizeSupplier;
    private final List<WeakReference<E>> itemsRefList;

    public SelectedItemsReadOnlyObservableList(ObservableList<Integer> selectedIndices, Supplier<Integer> modelSizeSupplier) {
        this.modelSizeSupplier = modelSizeSupplier;
        this.selectedIndices = selectedIndices;
        this.itemsRefList = new ArrayList<>();

        selectedIndices.addListener((ListChangeListener<Integer>)c -> {
            int totalRemovedSize = 0;
            beginChange();

            while (c.next()) {
                if (c.wasReplaced()) {
                    List<E> removed = getRemovedElements(c, totalRemovedSize);
                    List<E> added = getAddedElements(c);
                    if (!removed.equals(added)) {
                        nextReplace(c.getFrom(), c.getTo(), removed);
                    }
                } else if (c.wasAdded()) {
                    nextAdd(c.getFrom(), c.getTo());
                } else if (c.wasRemoved()) {
                    int removedSize = c.getRemovedSize();
                    if (removedSize == 1) {
                        nextRemove(c.getFrom(), getRemovedModelItem(totalRemovedSize + c.getFrom()));
                    } else {
                        nextRemove(c.getFrom(), getRemovedElements(c, totalRemovedSize));
                    }
                    totalRemovedSize += removedSize;
                } else if (c.wasPermutated()) {
                    int[] permutation = new int[size()];
                    for (int i = 0; i < size(); i++) {
                        permutation[i] = c.getPermutation(i);
                    }
                    nextPermutation(c.getFrom(), c.getTo(), permutation);
                } else if (c.wasUpdated()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        nextUpdate(i);
                    }
                }
            }

            // regardless of the change, we recreate the itemsRefList to reflect the current items list.
            // This is important for cases where items are removed (and so must their selection, but we lose
            // access to the item before we can fire the event).
            // FIXME we could make this more efficient by only making the reported changes to the list
            itemsRefList.clear();
            for (int selectedIndex : selectedIndices) {
                itemsRefList.add(new WeakReference<>(getModelItem(selectedIndex)));
            }

            endChange();
        });
    }

    protected abstract E getModelItem(int index);

    @Override
    public E get(int index) {
        int pos = selectedIndices.get(index);
        return getModelItem(pos);
    }

    @Override
    public int size() {
        return selectedIndices.size();
    }

    private E _getModelItem(int index) {
        if (index >= modelSizeSupplier.get()) {
            // attempt to return from the itemsRefList instead
            return getRemovedModelItem(index);
        } else {
            return getModelItem(index);
        }
    }

    private E getRemovedModelItem(int index) {
        // attempt to return from the itemsRefList instead
        return index < 0 || index >= itemsRefList.size() ? null : itemsRefList.get(index).get();
    }

    private List<E> getRemovedElements(ListChangeListener.Change<? extends Integer> c, int totalRemovedSize) {
        List<E> removed = new ArrayList<>(c.getRemovedSize());
        final int startPos = c.getFrom();
        for (int i = startPos, max = startPos + c.getRemovedSize(); i < max; i++) {
            removed.add(getRemovedModelItem(i + totalRemovedSize));
        }
        return removed;
    }

    private List<E> getAddedElements(ListChangeListener.Change<? extends Integer> c) {
        List<E> added = new ArrayList<>(c.getAddedSize());
        for (int index : c.getAddedSubList()) {
            added.add(_getModelItem(index));
        }
        return added;
    }
}
