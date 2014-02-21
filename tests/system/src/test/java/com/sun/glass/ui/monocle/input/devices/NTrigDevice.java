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

package com.sun.glass.ui.monocle.input.devices;

import com.sun.glass.ui.monocle.input.UInput;

import java.util.Random;

public class NTrigDevice extends TestTouchDevice {
    // this device has a tendency to send garbage for ABS_X and ABS_Y
    private Random random = new Random(1l);

    public NTrigDevice() {
        super(5);
    }

    @Override
    public void create() {
        ui = new UInput();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("KEYBIT BTN_TOUCH");
        ui.processLine("EVBIT EV_ABS");
        ui.processLine("ABSBIT ABS_X");
        ui.processLine("ABSBIT ABS_Y");
        ui.processLine("ABSBIT ABS_MT_POSITION_X");
        ui.processLine("ABSBIT ABS_MT_POSITION_Y");
        ui.processLine("ABSBIT ABS_MT_ORIENTATION");
        ui.processLine("ABSBIT ABS_MT_TOUCH_MAJOR");
        ui.processLine("ABSBIT ABS_MT_TOUCH_MINOR");
        ui.processLine("ABSMIN ABS_X 0");
        ui.processLine("ABSMAX ABS_X 4095");
        ui.processLine("ABSMIN ABS_Y 0");
        ui.processLine("ABSMAX ABS_Y 4095");
        ui.processLine("ABSMIN ABS_MT_POSITION_X 0");
        ui.processLine("ABSMAX ABS_MT_POSITION_X 4095");
        ui.processLine("ABSMIN ABS_MT_POSITION_Y 0");
        ui.processLine("ABSMAX ABS_MT_POSITION_Y 4095");
        ui.processLine("ABSMIN ABS_MT_ORIENTATION 0");
        ui.processLine("ABSMAX ABS_MT_ORIENTATION 1");
        ui.processLine("PROPBIT INPUT_PROP_POINTER");
        ui.processLine("PROPBIT INPUT_PROP_DIRECT");
        ui.processLine("PROPERTY ID_INPUT_TOUCHSCREEN 1");
        ui.processLine("CREATE");
        setAbsScale(4096, 4096);
    }

    @Override
    public void sync() {
        if (pressedPoints > 0) {
            ui.processLine("EV_ABS ABS_X " + random.nextInt(4096));
            ui.processLine("EV_ABS ABS_Y " + random.nextInt(4096));
        }
        for (int p = 0; p < points.length; p++) {
            if (points[p]) {
                ui.processLine("EV_ABS ABS_MT_POSITION_X " + transformedXs[p]);
                ui.processLine("EV_ABS ABS_MT_POSITION_Y " + transformedYs[p]);
                ui.processLine("EV_ABS ABS_MT_TOUCH_MAJOR 635");
                ui.processLine("EV_ABS ABS_MT_TOUCH_MINOR 533");
                ui.processLine("EV_SYN SYN_MT_REPORT 0");
            }
        }
        if (previousPressedPoints == 0 && pressedPoints > 0) {
            ui.processLine("EV_KEY BTN_TOOL_DOUBLETAP 1");
            ui.processLine("EV_KEY BTN_TOUCH 1");
        } else if (previousPressedPoints > 0 && pressedPoints == 0) {
            ui.processLine("EV_KEY BTN_TOOL_DOUBLETAP 0");
            ui.processLine("EV_KEY BTN_TOUCH 0 ");
        }
        super.sync();
    }

}
