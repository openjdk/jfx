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

package com.sun.glass.ui.monocle.input;

/**
 * TouchLookahead handles compression of touch event streams by folding
 * together adjacent events that differ only in their coordinates but not in
 * number of touch points or the IDs assigned to those points.
 *
 * pullState(..) gets the current state from TouchInput
 * pushState() updates TouchInput with new touch point data
 * flushState() must be called at the end of event processing to clear the
 * pipeline.
 */
public class TouchLookahead {

    private TouchInput touch = TouchInput.getInstance();
    private TouchState previousState = new TouchState();
    private TouchState state = new TouchState();
    private boolean assignIDs;
    private boolean processedFirstEvent;

    public TouchState getState() {
        return state;
    }

    /** Sets whether or not we are asking TouchInput to assign touch point IDs */
    public void setAssignIDs(boolean assignIDs) {
        this.assignIDs = assignIDs;
    }

    /**
     * Updates the local touch point state from TouchInput
     *
     * @param clearPoints Whether to clear touch point data in the updated local
     *                    state. Stateless Touch processors getting their input
     *                    with drivers that send each touch point on every event
     *                    might need to set this; touch processors using drivers
     *                    that send only the delta from the previous state will
     *                    not want to clear the points.
     */
    public void pullState(boolean clearPoints) {
        touch.getState(state);
        if (clearPoints) {
            state.clear();
        }
    }

    public void pushState() {
        if (!processedFirstEvent) {
            touch.getState(previousState);
            if (state.canBeFoldedWith(previousState, assignIDs)) {
                processedFirstEvent = true;
            } else {
                touch.setState(state, true);
            }
        }
        if (processedFirstEvent) {
            // fold together TouchStates that have the same touch point count
            // and IDs. For Protocol A devices the touch IDs are not initialized
            // yet, which means the only differentiator will be the number of
            // points.
            state.sortPointsByID();
            if (!state.canBeFoldedWith(previousState, assignIDs)) {
                // the events are different. Send "previousState".
                touch.setState(previousState, true);
            }
        }
        state.copyTo(previousState);
    }

    public void flushState() {
        touch.setState(previousState, assignIDs);
        processedFirstEvent = false;
    }

}
