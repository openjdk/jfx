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

package com.sun.glass.ui.monocle;

class LinuxSimpleTouchProcessor extends LinuxTouchProcessor {

    LinuxSimpleTouchProcessor(LinuxInputDevice device) {
        super(device);
        pipeline.addFilter(new LookaheadTouchFilter(true));
        pipeline.addFilter(new AssignPointIDTouchFilter());
    }

    @Override
    public void processEvents(LinuxInputDevice device) {
        LinuxEventBuffer buffer = device.getBuffer();
        state.clear();
        boolean touchReleased = false;
        while (buffer.hasNextEvent()) {
            switch (buffer.getEventType()) {
                case LinuxInput.EV_ABS: {
                    int value = transform.getValue(buffer);
                    switch (transform.getAxis(buffer)) {
                        case LinuxInput.ABS_X:
                        case LinuxInput.ABS_MT_POSITION_X:
                            if (state.getPointCount() == 0) {
                                state.addPoint(null).x = value;
                            } else {
                                state.getPoint(0).x = value;
                            }
                            break;
                        case LinuxInput.ABS_Y:
                        case LinuxInput.ABS_MT_POSITION_Y:
                            if (state.getPointCount() == 0) {
                                state.addPoint(null).y = value;
                            } else {
                                state.getPoint(0).y = value;
                            }
                            break;
                    }
                    break;
                }
                case LinuxInput.EV_KEY:
                    switch (buffer.getEventCode()) {
                        case LinuxInput.BTN_TOUCH:
                            if (buffer.getEventValue() == 0) {
                                touchReleased = true;
                            } else if (state.getPointCount() == 0) {
                                // restore an old point
                                state.addPoint(null);
                            }
                            break;
                    }
                    break;
                case LinuxInput.EV_SYN:
                    switch (buffer.getEventCode()) {
                        case LinuxInput.SYN_REPORT:
                            if (touchReleased) {
                                // remove points
                                state.clear();
                                touchReleased = false;
                            }
                            pipeline.pushState(state);
                            state.clear();
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
