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

import java.util.Comparator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A helper and marker interface used for {@code ObservableList}s that implement sorting algorithms that report
 * the sort as one change.
 *
 * @param <E> the type of elements in this list
 * @see FXCollections#sort(ObservableList, Comparator)
 */
public interface SortableList<E> extends ObservableList<E> {

    @SuppressWarnings("unchecked")
    @Override
    public default void sort(Comparator<? super E> comparator) {
        if (size() == 0 || size() == 1) {
            return;
        }
        // The cast will succeed, but a ClassCastException will be thrown as specified when compare is called
        comparator = comparator != null ? comparator : (Comparator<? super E>) Comparator.naturalOrder();
        doSort(comparator);
    }

    /**
     * Sorts the list and reports it as one change event.
     *
     * @param comparator the comparator for the sorting; never {@code null}
     */
    void doSort(Comparator<? super E> comparator);
}
