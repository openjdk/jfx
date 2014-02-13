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

import com.sun.glass.ui.monocle.input.TestLog;
import com.sun.glass.ui.monocle.input.UInput;

import java.util.HashSet;
import java.util.Set;

/** The touch screen used in the Freescale i.MX6Q Sabre Device Platform,
 * extrapolated to five touch points. There is some guesswork here as to
 * whether the screen always resends stationary points. This class assumes that
 * it resends stationary points.
 */
public class EGalaxMultiTouchDevice2 extends TestTouchDevice {

    private Set<Integer> modifiedPoints = new HashSet<>();

    public EGalaxMultiTouchDevice2() {
        super(5);
    }

    @Override
    public void create() {
        ui = new UInput();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("ABSBIT ABS_MT_TRACKING_ID");
        ui.processLine("ABSBIT ABS_MT_POSITION_X");
        ui.processLine("ABSBIT ABS_MT_POSITION_Y");
        ui.processLine("ABSBIT ABS_MT_TOUCH_MAJOR");
        ui.processLine("ABSMIN ABS_MT_TRACKING_ID 0");
        ui.processLine("ABSMAX ABS_MT_TRACKING_ID 5");
        ui.processLine("ABSMIN ABS_MT_POSITION_X 0");
        ui.processLine("ABSMAX ABS_MT_POSITION_X 32767");
        ui.processLine("ABSMIN ABS_MT_POSITION_Y 0");
        ui.processLine("ABSMAX ABS_MT_POSITION_Y 32767");
        ui.processLine("ABSMIN ABS_MT_TOUCH_MAJOR 0");
        ui.processLine("ABSMAX ABS_MT_TOUCH_MAJOR 255");
        ui.processLine("PROPBIT INPUT_PROP_POINTER");
        ui.processLine("PROPBIT INPUT_PROP_DIRECT");
        ui.processLine("PROPERTY ID_INPUT_TOUCHSCREEN 1");
        ui.processLine("CREATE");
        setAbsScale(32768, 32768);
    }

    @Override
    public int addPoint(double x, double y) {
        int p = super.addPoint(x, y);
        ui.processLine("EV_ABS ABS_MT_TRACKING_ID " + p);
        ui.processLine("EV_ABS ABS_MT_TOUCH_MAJOR 1");
        ui.processLine("EV_ABS ABS_MT_POSITION_X " + transformedXs[p]);
        ui.processLine("EV_ABS ABS_MT_POSITION_Y " + transformedYs[p]);
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        modifiedPoints.add(p);
        return p;
    }

    @Override
    public void removePoint(int p) {
        super.removePoint(p);
        ui.processLine("EV_ABS ABS_MT_TRACKING_ID " + p);
        ui.processLine("EV_ABS ABS_MT_TOUCH_MAJOR 0");
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
    }

    @Override
    public void setPoint(int p, double x, double y) {
        super.setPoint(p, x, y);
        ui.processLine("EV_ABS ABS_MT_TRACKING_ID " + p);
        ui.processLine("EV_ABS ABS_MT_TOUCH_MAJOR 1");
        ui.processLine("EV_ABS ABS_MT_POSITION_X " + transformedXs[p]);
        ui.processLine("EV_ABS ABS_MT_POSITION_Y " + transformedYs[p]);
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        modifiedPoints.add(p);
    }

    @Override
    public void setAndRemovePoint(int p, double x, double y) {
        // This device doesn't send move and release at the same time
        removePoint(p);
    }

    @Override
    public void sync() {
        for (int p = 0; p < points.length; p++) {
            if (points[p] && !modifiedPoints.contains(p)) {
                ui.processLine("EV_ABS ABS_MT_TRACKING_ID " + p);
                ui.processLine("EV_ABS ABS_MT_TOUCH_MAJOR 1");
                ui.processLine("EV_ABS ABS_MT_POSITION_X " + transformedXs[p]);
                ui.processLine("EV_ABS ABS_MT_POSITION_Y " + transformedYs[p]);
                ui.processLine("EV_SYN SYN_MT_REPORT 0");
            }
        }
        modifiedPoints.clear();
        super.sync();
    }

    @Override
    public void resendStateAndSync() {
        TestLog.log("TestTouchDevice: sync");
        for (int p = 0; p < points.length; p++) {
            if (points[p]) {
                ui.processLine("EV_ABS ABS_MT_TRACKING_ID " + p);
                ui.processLine("EV_ABS ABS_MT_TOUCH_MAJOR 1");
                ui.processLine("EV_ABS ABS_MT_POSITION_X " + transformedXs[p]);
                ui.processLine("EV_ABS ABS_MT_POSITION_Y " + transformedYs[p]);
                ui.processLine("EV_SYN SYN_MT_REPORT 0");
            }
        }
        modifiedPoints.clear();
        super.sync();
    }

}
