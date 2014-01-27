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

import java.util.ArrayList;

public class TouchPipeline {

    private TouchInput touch = TouchInput.getInstance();
    private ArrayList<TouchFilter> filters = new ArrayList<TouchFilter>();
    private TouchState flushState = new TouchState();

    /**
     * Adds the filters in the given pipeline to this pipeline
     */
    public void add(TouchPipeline pipeline) {
        for (int i = 0; i < pipeline.filters.size(); i++) {
            addFilter(pipeline.filters.get(i));
        }
    }

    /**
     * Attempts to add the filters named in the comma-separated list provided.
     */
    public void addNamedFilters(String filterNameList) {
        String[] touchFilterNames = filterNameList.split(",");
        if (touchFilterNames != null) {
            for (String touchFilterName : touchFilterNames) {
                String s = touchFilterName.trim();
                if (s.length() > 0) {
                    addNamedFilter(s);
                }
            }
        }
    }

    public void addNamedFilter(String filterName) {
        try {
            // install known filters without reflection
            if (filterName.equals("SmallMove")) {
                addFilter(new SmallMoveTouchFilter());
            } else if (filterName.equals("NearbyPoints")) {
                addFilter(new NearbyPointsTouchFilter());
            } else if (filterName.equals("AssignPointID")) {
                addFilter(new AssignPointIDTouchFilter());
            } else {
                Class cls;
                if (!filterName.contains(".")) {
                    filterName = "com.sun.glass.ui.monocle.input.filters."
                            + filterName + "TouchFilter";
                }
                addFilter((TouchFilter) Class.forName(filterName).newInstance());
            }
        } catch (Exception e) {
            System.err.println(
                    "Cannot install touch filter '" + filterName + "'");
            e.printStackTrace();
        }
    }

    public void addFilter(TouchFilter filter) {
        int priority = filter.getPriority();
        int i;
        for (i = 0; i < filters.size(); i++) {
            if (filters.get(i).equals(filter)) {
                return;
            }
            if (filters.get(i).getPriority() < priority) {
                break;
            }
        }
        filters.add(i, filter);
    }

    public boolean filter(TouchState state) {
        for (int i = 0; i < filters.size(); i++) {
            if (filters.get(i).filter(state)) {
                return true;
            }
        }
        return false;
    }

    public void pushState(TouchState state) {
        if (!filter(state)) {
            touch.setState(state);
        }
    }

    /** Flushes any remaining data in the pipeline, possibly pushing more
     * state objects to TouchInput.
     */
    public void flush() {
        for (int i = 0; i < filters.size(); i++) {
            TouchFilter filter = filters.get(i);
            while (filter.flush(flushState)) {
                boolean consumed = false;
                for (int j = i + 1; j < filters.size() && !consumed; j++) {
                    consumed = filters.get(j).filter(flushState);
                }
                if (!consumed) {
                    touch.setState(flushState);
                }
            }
        }
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
    public void pullState(TouchState state, boolean clearPoints) {
        touch.getState(state);
        if (clearPoints) {
            state.clear();
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("TouchPipeline[");
        for (int i = 0; i < filters.size(); i++) {
            sb.append(filters.get(i));
            if (i < filters.size() - 1) {
                sb.append(" -> ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
