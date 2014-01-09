/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.glass.ui.monocle.input.TouchInput;
import com.sun.glass.ui.monocle.input.TouchState;

public class LinuxTouchProcessor implements LinuxInputProcessor {

    private TouchState state = new TouchState();

    @Override
    public void processEvents(LinuxInputDevice device) {
        TouchInput touch = TouchInput.getInstance();
        while (device.hasNextEvent()) {
            switch (device.getEventType()) {
                case Input.EV_ABS: {
                    int pixelValue = toPixelValue(device,
                                                  device.getEventCode(),
                                                  device.getEventValue());
                    switch (device.getEventCode()) {
                        case Input.ABS_X:
                            state.getPointZero().x = pixelValue;
                            break;
                        case Input.ABS_Y:
                            state.getPointZero().y = pixelValue;
                            break;
                    }
                }
                case Input.EV_SYN:
                    switch (device.getEventCode()) {
                        case Input.SYN_REPORT:
                            touch.setState(state);
                            state.clear();
                            break;
                        default: // ignore
                    }
                    break;
            }
            device.nextEvent();
        }
    }

    private static int toPixelValue(LinuxInputDevice device, int axis, int value) {
        switch (axis) {
            case Input.ABS_X:
            case Input.ABS_MT_POSITION_X:
                return toPixelX(device, axis, value);
            case Input.ABS_Y:
            case Input.ABS_MT_POSITION_Y:
                return toPixelY(device, axis, value);
            default:
                return value;
        }
    }

    private static int toPixelX(LinuxInputDevice device, int axis, int value) {
        AbsoluteInputCapabilities caps = device.getAbsoluteInputCapabilities(axis);
        int minimum = caps.getMinimum();
        int maximum = caps.getMaximum();
        int screenWidth = NativePlatformFactory.getNativePlatform().getScreen().getWidth();
        int pixel = ((value - minimum) * screenWidth) / (maximum - minimum);
        return pixel;
    }

    private static int toPixelY(LinuxInputDevice device, int axis, int value) {
        AbsoluteInputCapabilities caps = device.getAbsoluteInputCapabilities(axis);
        int minimum = caps.getMinimum();
        int maximum = caps.getMaximum();
        int screenHeight = NativePlatformFactory.getNativePlatform().getScreen().getHeight();
        int pixel = ((value - minimum) * screenHeight) / (maximum - minimum);
        return pixel;
    }

}
