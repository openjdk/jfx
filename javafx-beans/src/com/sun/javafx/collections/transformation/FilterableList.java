/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/**
 * FilterableList allows to filter itself using a {@link Matcher}.
 * It can operate in 2 modes: a {@link FilterMode#BATCH} mode (filtering is done on demand)
 * or {@link FilterMode#LIVE} mode (on-line filtering).
 *
 * Matcher of the FilterableList should be always set to a non-null value. Elements
 * that are not matched by the matcher are filtered-out from this list.
 *
 * <b>Important</b>: Filterable list can never be modifiable using the {@link List} modification methods.
 * It might have a different methods to use for a modification.
 *
 */
public interface FilterableList<E> extends List<E> {

    /**
     * Specifies current filtering method
     */
    public enum FilterMode {
        /**
         * Filter only on demand by using the {@link #filter()} method.
         */
        BATCH,
        /**
         * Filtering is done automatically.
         */
        LIVE;
    }

    /**
     * Filter using provided matcher. Has no effect in {@link FilterMode#LIVE} mode.
     */
    public void filter();

    /**
     * Set the current mode.
     * @param mode a mode to set
     * @see #getMode
     */
    public void setMode(FilterMode mode);

    /**
     * Current mode.
     * @return the current mode
     * @see #setMode
     */
    public FilterMode getMode();

    /**
     * Change the current matcher to Matcher m.
     * In {@link FilterMode#LIVE} mode, the list is immediately refiltered.
     * @param m non-null matcher
     * @see #getMatcher
     */
    public void setMatcher(Matcher<? super E> m);

    /**
     * Returns the current matcher.
     * @return the current matcher
     * @see #setMatcher
     */
    public Matcher<? super E> getMatcher();
}
