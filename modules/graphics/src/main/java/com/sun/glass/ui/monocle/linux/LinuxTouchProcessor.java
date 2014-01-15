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

import com.sun.glass.ui.monocle.input.TouchLookahead;

public class LinuxTouchProcessor implements LinuxInputProcessor {

    private final TouchLookahead tl = new TouchLookahead();
    private final LinuxTouchTransform transform;

    LinuxTouchProcessor(LinuxInputDevice device) {
        tl.setAssignIDs(true);
        transform = new LinuxTouchTransform(device);
    }

    @Override
    public void processEvents(LinuxInputDevice device) {
        LinuxEventBuffer buffer = device.getBuffer();
        tl.pullState(true);
        boolean touchReleased = false;
        while (buffer.hasNextEvent()) {
            switch (buffer.getEventType()) {
                case Input.EV_ABS: {
                    int value = transform.getValue(buffer);
                    switch (transform.getAxis(buffer)) {
                        case Input.ABS_X:
                        case Input.ABS_MT_POSITION_X:
                            tl.getState().getPointForID(0, true).x = value;
                            break;
                        case Input.ABS_Y:
                        case Input.ABS_MT_POSITION_Y:
                            tl.getState().getPointForID(0, true).y = value;
                            break;
                    }
                    break;
                }
                case Input.EV_KEY:
                    switch (buffer.getEventCode()) {
                        case Input.BTN_TOUCH:
                            if (buffer.getEventValue() == 0) {
                                touchReleased = true;
                            } else {
                                // restore an old point
                                tl.getState().getPointForID(0, true);
                            }
                            break;
                    }
                    break;
                case Input.EV_SYN:
                    switch (buffer.getEventCode()) {
                        case Input.SYN_REPORT:
                            if (touchReleased) {
                                // remove points
                                tl.getState().clear();
                                touchReleased = false;
                            }
                            tl.pushState();
                            tl.pullState(true);
                            break;
                        default: // ignore
                    }
                    break;
            }
            buffer.nextEvent();
        }
        tl.flushState();
    }

}
