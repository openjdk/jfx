/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TablePositionBase;

import java.util.*;

/**
 * Implementation code used by the TableSelectionModel implementations. In short
 * this code exists to speed up some common use cases which were incredibly
 * slow in the old approach. The old approach essentially required a lot of
 * iterating through the selectedCells list. The new approach is to keep this
 * list for what it is good for (representing selection order primarily), and
 * introduce a Map<Integer, BitSet> to speed up the slow parts - namely looking
 * up whether a given row/column intersection is selected or not.
 *
 * Note that a map that contains an empty bitset is used to represent that the
 * row is selected.
 *
 * Refer to RT-33442 for more information on this issue.
 */
// T == TablePosition<S,?>
public abstract class SelectedCellsMap<T extends TablePositionBase> {
    private final ObservableList<T> selectedCells;
    private final ObservableList<T> sortedSelectedCells;

    private final Map<Integer, BitSet> selectedCellBitSetMap;

    public SelectedCellsMap(final ListChangeListener<T> listener) {
        selectedCells = FXCollections.<T>observableArrayList();
        sortedSelectedCells = new SortedList<>(selectedCells, (T o1, T o2) -> {
            int result = o1.getRow() - o2.getRow();
            return result == 0 ? (o1.getColumn() - o2.getColumn()) : result;
        });
        sortedSelectedCells.addListener(listener);

        selectedCellBitSetMap = new TreeMap<>((o1, o2) -> o1.compareTo(o2));
    }

    public abstract boolean isCellSelectionEnabled();

    public int size() {
        return selectedCells.size();
    }

    public T get(int i) {
        if (i < 0) {
            return null;
        }
        return sortedSelectedCells.get(i);
    }

    public void add(T tp) {
        final int row = tp.getRow();
        final int columnIndex = tp.getColumn();

        // update the bitset map
        boolean isNewBitSet = false;
        BitSet bitset;
        if (! selectedCellBitSetMap.containsKey(row)) {
            bitset = new BitSet();
            selectedCellBitSetMap.put(row, bitset);
            isNewBitSet = true;
        } else {
            bitset = selectedCellBitSetMap.get(row);
        }

        final boolean cellSelectionModeEnabled = isCellSelectionEnabled();

        if (cellSelectionModeEnabled) {
            if (columnIndex >= 0) {
                boolean isAlreadySet = bitset.get(columnIndex);

                if (!isAlreadySet) {
                    bitset.set(columnIndex);

                    // add into the list
                    selectedCells.add(tp);
                }
            } else {
                // FIXME slow path (for now)
                if (!selectedCells.contains(tp)) {
                    selectedCells.add(tp);
                }
            }
        } else {
            if (isNewBitSet) {
                if (columnIndex >= 0) {
                    bitset.set(columnIndex);
                }
                selectedCells.add(tp);
            }
        }
    }

    public void addAll(Collection<T> cells) {
        // update bitset
        for (T tp : cells) {
            final int row = tp.getRow();
            final int columnIndex = tp.getColumn();

            // update the bitset map
            BitSet bitset;
            if (! selectedCellBitSetMap.containsKey(row)) {
                bitset = new BitSet();
                selectedCellBitSetMap.put(row, bitset);
            } else {
                bitset = selectedCellBitSetMap.get(row);
            }

            if (columnIndex < 0) {
                continue;
            }

            bitset.set(columnIndex);
        }

        // add into the list
        selectedCells.addAll(cells);
    }

    public void setAll(Collection<T> cells) {
        // update bitset
        selectedCellBitSetMap.clear();
        for (T tp : cells) {
            final int row = tp.getRow();
            final int columnIndex = tp.getColumn();

            // update the bitset map
            BitSet bitset;
            if (! selectedCellBitSetMap.containsKey(row)) {
                bitset = new BitSet();
                selectedCellBitSetMap.put(row, bitset);
            } else {
                bitset = selectedCellBitSetMap.get(row);
            }

            if (columnIndex < 0) {
                continue;
            }

            bitset.set(columnIndex);
        }

        // add into the list
        selectedCells.setAll(cells);
    }

    public void remove(T tp) {
        final int row = tp.getRow();
        final int columnIndex = tp.getColumn();

        // update the bitset map
        if (selectedCellBitSetMap.containsKey(row)) {
            BitSet bitset = selectedCellBitSetMap.get(row);

            if (columnIndex >= 0) {
                bitset.clear(columnIndex);
            }

            if (bitset.isEmpty()) {
                selectedCellBitSetMap.remove(row);
            }
        }

        // update list
        selectedCells.remove(tp);
    }

    public void clear() {
        // update bitset
        selectedCellBitSetMap.clear();

        // update list
        selectedCells.clear();
    }

    public boolean isSelected(int row, int columnIndex) {
        if (columnIndex < 0) {
            return selectedCellBitSetMap.containsKey(row);
        } else {
            return selectedCellBitSetMap.containsKey(row) ? selectedCellBitSetMap.get(row).get(columnIndex) : false;
        }
    }

    public int indexOf(T tp) {
        return sortedSelectedCells.indexOf(tp);
    }

    public boolean isEmpty() {
        return selectedCells.isEmpty();
    }

    public ObservableList<T> getSelectedCells() {
        return selectedCells;
    }
}