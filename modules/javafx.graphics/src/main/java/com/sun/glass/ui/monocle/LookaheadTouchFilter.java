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

class LookaheadTouchFilter implements TouchFilter {

    private TouchState previousState = new TouchState();
    private TouchState tmpState = new TouchState();
    private boolean assignIDs;

    private enum FilterState {
        /** No events processed yet */
        CLEAN,
        /**
         * previousState contains an event that must be sent as is. This will
         * happen when an event is the first to be processed on this pulse, or
         * "Substantially different" means that two events differ in the number
         * of touch points or in the IDs assigned to those points.
         */
        PENDING_UNMODIFIABLE,
        /**
         * previousState contains an event that we are allowed to change before
         * sending. An event can be modified if it is substantially different
         * neither from the event that preceded it nor from the event that
         * follows it.
         */
        PENDING_MODIFIABLE
    }

    private FilterState filterState = FilterState.CLEAN;

    /**
     * Creates a new LookaheadTouchFilter
     *
     * @param assignIDs Sets whether or not we are asking the touch pipeline to
     *                  assign touch point IDs
     */
    LookaheadTouchFilter(boolean assignIDs) {
        this.assignIDs = assignIDs;
    }

    @Override
    public boolean filter(TouchState state) {
        state.sortPointsByID();
        switch (filterState) {
            case CLEAN:
                state.copyTo(previousState);
                filterState = FilterState.PENDING_UNMODIFIABLE;
                return true;
            case PENDING_UNMODIFIABLE:
                // send the previous state and hold the new state as pending
                state.copyTo(tmpState);
                previousState.copyTo(state);
                tmpState.copyTo(previousState);
                if (state.canBeFoldedWith(previousState, assignIDs)) {
                    filterState = FilterState.PENDING_MODIFIABLE;
                }
                return false;
            case PENDING_MODIFIABLE:
                if (state.canBeFoldedWith(previousState, assignIDs)) {
                    state.copyTo(previousState);
                    return true;
                } else {
                    // send the previous state and hold the new state as pending
                    state.copyTo(tmpState);
                    previousState.copyTo(state);
                    tmpState.copyTo(previousState);
                    filterState = FilterState.PENDING_UNMODIFIABLE;
                    return false;
                }
            default:
                return false;
        }
    }

    @Override
    public boolean flush(TouchState state) {
        switch (filterState) {
            case PENDING_MODIFIABLE:
            case PENDING_UNMODIFIABLE:
                previousState.copyTo(state);
                filterState = FilterState.CLEAN;
                return true;
            default:
                return false;
        }
    }

    @Override
    public int getPriority() {
        return PRIORITY_PRE_ID + 1;
    }

    @Override
    public String toString() {
        return "Lookahead[previousState="
                + previousState
                + ",filterState=" + filterState
                + "]";
    }

}
