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

package com.sun.glass.ui.monocle.input.filters;

import com.sun.glass.ui.monocle.input.TouchInput;
import com.sun.glass.ui.monocle.input.TouchState;

public class LookaheadTouchFilter implements TouchFilter {

    private TouchInput touch = TouchInput.getInstance();
    private TouchState previousState = new TouchState();
    private TouchState tmpState = new TouchState();
    private boolean assignIDs;
    private boolean processedFirstEvent;

    /**
     * Creates a new LookaheadTouchFilter
     *
     * @param assignIDs Sets whether or not we are asking the touch pipeline to
     *                  assign touch point IDs
     */
    public LookaheadTouchFilter(boolean assignIDs) {
        this.assignIDs = assignIDs;
    }

    @Override
    public boolean filter(TouchState state) {
        if (!processedFirstEvent) {
            touch.getState(previousState);
            if (state.canBeFoldedWith(previousState, assignIDs)) {
                processedFirstEvent = true;
            } else {
                state.copyTo(previousState);
                return false; // send state
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
                state.copyTo(tmpState);
                previousState.copyTo(state);
                tmpState.copyTo(previousState);
                return false;
            }
        }
        state.copyTo(previousState);
        return true;
    }

    @Override
    public boolean flush(TouchState state) {
        if (processedFirstEvent) {
            previousState.copyTo(state);
            processedFirstEvent = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getPriority() {
        return PRIORITY_PRE_ID + 1;
    }

    @Override
    public String toString() {
        return "Lookahead";
    }

}
