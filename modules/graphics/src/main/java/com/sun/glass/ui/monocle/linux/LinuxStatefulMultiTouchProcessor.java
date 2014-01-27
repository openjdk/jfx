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

package com.sun.glass.ui.monocle.linux;

import com.sun.glass.ui.monocle.input.TouchState;
import com.sun.glass.ui.monocle.input.filters.LookaheadTouchFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * This multitouch processor works with drivers that send tracking IDs.
 */
public class LinuxStatefulMultiTouchProcessor extends LinuxTouchProcessor {

    private static final int ID_UNASSIGNED = -1;
    private static final int SLOT_UNASSIGNED = -1;
    private static final int COORD_UNDEFINED = Integer.MIN_VALUE;

    private final Map<Integer, Integer> slotToIDMap =
            new HashMap<Integer, Integer>();

    LinuxStatefulMultiTouchProcessor(LinuxInputDevice device) {
        super(device);
        pipeline.addFilter(new LookaheadTouchFilter(false));
    }

    @Override
    public void processEvents(LinuxInputDevice device) {
        LinuxEventBuffer buffer = device.getBuffer();
        pipeline.pullState(state, false);
        int currentID = ID_UNASSIGNED;
        int currentSlot = SLOT_UNASSIGNED;
        int x = COORD_UNDEFINED;
        int y = COORD_UNDEFINED;
        while (buffer.hasNextEvent()) {
            System.out.println("Processing " + buffer.getEventDescription());
            switch (buffer.getEventType()) {
                case Input.EV_ABS: {
                    int value = transform.getValue(buffer);
                    switch (transform.getAxis(buffer)) {
                        case Input.ABS_MT_SLOT:
                            // We expect ABS_MT_SLOT and ABS_MT_TRACKING_ID
                            // to precede coordinates
                            currentSlot = value;
                            currentID = slotToIDMap.getOrDefault(currentSlot,
                                                                 ID_UNASSIGNED);
                            break;
                        case Input.ABS_MT_TRACKING_ID:
                            currentID = value;
                            if (currentSlot != SLOT_UNASSIGNED) {
                                slotToIDMap.put(currentSlot, currentID);
                            }
                            break;
                        case Input.ABS_X:
                        case Input.ABS_MT_POSITION_X:
                            x = value;
                            break;
                        case Input.ABS_Y:
                        case Input.ABS_MT_POSITION_Y:
                            y = value;
                            break;
                    }
                    break;
                }
                case Input.EV_SYN:
                    switch (buffer.getEventCode()) {
                        case Input.SYN_MT_REPORT: {
                            if (currentID != ID_UNASSIGNED) {
                                if (x == COORD_UNDEFINED && y == COORD_UNDEFINED) {
                                    state.removePointForID(currentID);
                                } else {
                                    TouchState.Point p = state
                                            .getPointForID(currentID, false);
                                    if (p != null && p.id != currentID) {
                                        System.out.println("error");
                                    }
                                    if (p == null) {
                                        p = new TouchState.Point();
                                        p.id = currentID;
                                        p = state.addPoint(p);
                                    }
                                    if (x != COORD_UNDEFINED) {
                                        p.x = x;
                                    }
                                    if (y != COORD_UNDEFINED) {
                                        p.y = y;
                                    }
                                }
                            }
                            x = y = COORD_UNDEFINED;
                            currentID = ID_UNASSIGNED;
                            break;
                        }
                        case Input.SYN_REPORT:
                            pipeline.pushState(state);
                            pipeline.pullState(state, false);
                            currentID = ID_UNASSIGNED;
                            currentSlot = SLOT_UNASSIGNED;
                            x = y = COORD_UNDEFINED;
                            break;
                        default: // ignore
                    }
                    break;
            }
            buffer.nextEvent();
        }
        pipeline.flush();
    }

}
