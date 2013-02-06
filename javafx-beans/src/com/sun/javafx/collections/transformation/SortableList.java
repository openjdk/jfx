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

import java.util.Comparator;
import java.util.List;

/**
 * SortableList allows to be sorted by provided Comparator or by natural ordering of it's elements.
 * {@link SortMode#BATCH} mode requires a call to {@link #sort()} method to trigger sorting.
 * In {@link SortMode#LIVE} mode, the sorting is done automatically.
 *
 * In {@link SortMode#LIVE} mode, a List can become invalid if an comparison fails.
 * To resolve this, provide a correct Comparator. Although this cannot happen in BATCH mode, switching
 * from LIVE to BATCH mode in invalid state doesn't resolve this issue.
 *
 * <b>Important</b>: Sortable list can never be modifiable using the {@link List} modification methods.
 * It might have a different methods to use for a modification.
 *
 */
public interface SortableList<E> extends List<E> {

    /**
     * Specifies a sorting method.
     */
    public enum SortMode {
        /**
         * Sort only on demand by using the {@link #sort()} method.
         */
        BATCH,
        /**
         * Sorting is done automatically.
         * Note: if comparison fails in this mode, behavior of all method calls of this List are undefined!
         */
        LIVE;
    }

    /**
     * Sort using provided or default comparator.
     * @throws ClassCastException if no comparator was provided and if some of the elements cannot be cast to Comparable
     */
    public void sort();

    /**
     * Set the current mode.
     * @param mode the new mode
     * @see #getMode
     */
    public void setMode(SortMode mode);

    /**
     * Current mode
     * @return the current mode
     * @see #setMode
     */
    public SortMode getMode();

    /**
     * Change the current comparator to Comparator c.
     * Passing null will make element comparison using natural ordering,
     * providing the elements implements Comparable.
     * @param c the new comparator or null if natural ordering is required
     * @see #getComparator
     */
    public void setComparator(Comparator<? super E> c);

    /**
     * Current comparator
     * @return comparator or null if natural ordering is applied
     * @see #setComparator
     */
    public Comparator<? super E> getComparator();

}
