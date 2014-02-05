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

import com.sun.glass.ui.monocle.input.TestApplication;
import com.sun.glass.ui.monocle.input.UInput;
import org.junit.Assume;

import java.util.HashMap;
import java.util.Map;

/**
 * TouchRevolutionFusionDevice sends ABS_X, ABS_Y, ABS_MT_POSITION_X and
 * ABS_MT_POSITION_Y events. It uses BTN_TOUCH to notify presses and releases.
 * It does not send events on coordinate that has not changed. It sends tracking
 * IDs for touch points and uses slots.
 */
public class TouchRevolutionFusionDevice extends TestTouchDevice {

    private int currentSlot = 0;
    private Map<Integer, Integer> slotsToPoints = new HashMap<>();
    private Map<Integer, Integer> pointsToSlots = new HashMap<>();

    public TouchRevolutionFusionDevice() {
        super(5);
    }

    @Override
    public void create() {
        Assume.assumeTrue(TestApplication.isMonocle());
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
        ui.processLine("ABSBIT ABS_MT_TRACKING_ID");
        ui.processLine("ABSBIT ABS_MT_SLOT");
        ui.processLine("ABSMIN ABS_X 0");
        ui.processLine("ABSMAX ABS_X 4095");
        ui.processLine("ABSMIN ABS_Y 0");
        ui.processLine("ABSMAX ABS_Y 4095");
        ui.processLine("ABSMIN ABS_MT_POSITION_X 0");
        ui.processLine("ABSMAX ABS_MT_POSITION_X 4095");
        ui.processLine("ABSMIN ABS_MT_POSITION_Y 0");
        ui.processLine("ABSMAX ABS_MT_POSITION_Y 4095");
        ui.processLine("PROPBIT INPUT_PROP_POINTER");
        ui.processLine("PROPBIT INPUT_PROP_DIRECT");
        ui.processLine("PROPERTY ID_INPUT_TOUCHSCREEN 1");
        ui.processLine("BUS 0x0003");
        ui.processLine("VENDOR 0x1391");
        ui.processLine("PRODUCT 0x3001");
        ui.processLine("VERSION 0x0111");
        ui.processLine("CREATE");
        setAbsScale(4096, 4096);
    }

    @Override
    public int addPoint(double x, double y) {
        int p = super.addPoint(x, y);
        int slot = -1;
        for (int i = 0; i < points.length; i++) {
            if (!slotsToPoints.containsKey(i)) {
                slot = i;
                break;
            }
        }
        if (slot == -1) {
            throw new IllegalStateException("No free slot");
        }
        if (currentSlot != slot) {
            ui.processLine("EV_ABS ABS_MT_SLOT " + slot);
            currentSlot = slot;
        }
        slotsToPoints.put(slot, p);
        pointsToSlots.put(p, slot);
        ui.processLine("EV_ABS ABS_MT_TRACKING_ID " + getID(p));
        ui.processLine("EV_ABS ABS_MT_POSITION_X " + transformedXs[p]);
        ui.processLine("EV_ABS ABS_MT_POSITION_Y " + transformedYs[p]);
        if (pressedPoints == 1) {
            ui.processLine("EV_KEY BTN_TOUCH 1");
        }
        ui.processLine("EV_ABS ABS_X " + transformedXs[p]);
        ui.processLine("EV_ABS ABS_Y " + transformedYs[p]);
        return p;
    }

    private int selectSlotForPoint(int p) {
        int slot = pointsToSlots.get(p);
        if (slot != currentSlot) {
            ui.processLine("EV_ABS ABS_MT_SLOT " + slot);
            currentSlot = slot;
        }
        return currentSlot;
    }

    @Override
    public void removePoint(int p) {
        super.removePoint(p);
        int slot = selectSlotForPoint(p);
        pointsToSlots.remove(p);
        slotsToPoints.remove(slot);
        ui.processLine("EV_ABS ABS_MT_TRACKING_ID -1");
        if (pressedPoints == 0) {
            ui.processLine("EV_KEY BTN_TOUCH 0");
        }
    }

    @Override
    public void setPoint(int p, double x, double y) {
        int oldX = transformedXs[p];
        int oldY = transformedYs[p];
        super.setPoint(p, x, y);
        if (oldX != transformedXs[p]) {
            selectSlotForPoint(p);
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + transformedXs[p]);
            ui.processLine("EV_ABS ABS_X " + transformedXs[p]);
        }
        if (oldY != transformedYs[p]) {
            selectSlotForPoint(p);
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + transformedYs[p]);
            ui.processLine("EV_ABS ABS_Y " + transformedYs[p]);
        }
    }

    @Override
    public void resendStateAndSync() {
        for (int p = 0; p < points.length; p++) {
            if (points[p]) {
                selectSlotForPoint(p);
                ui.processLine("EV_ABS ABS_MT_POSITION_X " + transformedXs[p]);
                ui.processLine("EV_ABS ABS_X " + transformedXs[p]);
                ui.processLine("EV_ABS ABS_MT_POSITION_Y " + transformedYs[p]);
                ui.processLine("EV_ABS ABS_Y " + transformedYs[p]);
            }
        }
        sync();
    }

}
