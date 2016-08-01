/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

/**
 * A TouchFilter processes and modifies TouchStates before sending touch events
 */
interface TouchFilter {

    /**
     * The priority value of a filter that is applied before IDs are assigned to
     * points
     */
    static final int PRIORITY_PRE_ID = 100;

    /**
     * The priority value of the filter that applies IDs to points
     */
    static final int PRIORITY_ID = 0;

    /**
     * The priority value of a filter that is applied after IDs are assigned to
     * points
     */
    static final int PRIORITY_POST_ID = -100;

    /**
     * Filters a touch state
     *
     * @param state The new state to be filtered or modified
     * @return true if the state is consumed, in which case no further
     * processing will be done on this touch state and no events will be sent.
     */
    boolean filter(TouchState state);

    /**
     * Flushes this filter's state. If this filter wants to send any additional
     * events it should fill in the provided state object.
     *
     * @param state a TouchState object to be filled in by the filter
     * @return true if the filter put data into the state
     */
    boolean flush(TouchState state);

    /**
     * Gets the priority of this touch filter. Touch filters are applied in
     * order, sorting first by their priority and then by the order in which
     * they were requested. Higher priority numbers are applied first.
     */
    int getPriority();

}
