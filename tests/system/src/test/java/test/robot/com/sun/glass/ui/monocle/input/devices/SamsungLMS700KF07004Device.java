/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.com.sun.glass.ui.monocle.input.devices;

import org.junit.jupiter.api.Assumptions;
import test.robot.com.sun.glass.ui.monocle.TestApplication;
import test.robot.com.sun.glass.ui.monocle.UInput;

/**
 * SamsungLMS700KF07004Device sends ABS_X and ABS_Y events. It uses BTN_TOUCH to notify
 * presses and releases. It does not send ABS_X or ABS_Y if that coordinate has
 * not changed. It also sends BTN_TOOL_PEN events on press events, but not on
 * release events.
 */
public class SamsungLMS700KF07004Device extends TestTouchDevice {

    public SamsungLMS700KF07004Device() {
        super(1);
    }

    @Override
    public void create() {
        Assumptions.assumeTrue(TestApplication.isMonocle());
        ui = new UInput();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("KEYBIT BTN_TOUCH");
        ui.processLine("KEYBIT BTN_TOOL_PEN");
        ui.processLine("EVBIT EV_ABS");
        ui.processLine("ABSBIT ABS_X");
        ui.processLine("ABSBIT ABS_Y");
        ui.processLine("ABSMIN ABS_X 0");
        ui.processLine("ABSMAX ABS_X 4095");
        ui.processLine("ABSMIN ABS_Y 0");
        ui.processLine("ABSMAX ABS_Y 4095");
        ui.processLine("PROPBIT INPUT_PROP_POINTER");
        ui.processLine("PROPBIT INPUT_PROP_DIRECT");
        ui.processLine("PROPERTY ID_INPUT_TABLET 1");
        ui.processLine("PROPERTY ID_VENDOR eGalax_Inc.");
        ui.processLine("PROPERTY ID_VENDOR_ID 0x0eef");
        ui.processLine("PROPERTY ID_VENDOR_ENC eGalax");
        ui.processLine("PROPERTY ID_BUS usb");
        ui.processLine("PROPERTY ID_INPUT 1");
        ui.processLine("PROPERTY ID_MODEL Touch");
        ui.processLine("PROPERTY ID_MODEL_ENC Touch");
        ui.processLine("PROPERTY ID_MODEL_ID 0001");
        ui.processLine("PROPERTY ID_REVISION 0100");
        ui.processLine("PROPERTY ID_SERIAL eGalax_Inc._Touch");
        ui.processLine("PROPERTY ID_TYPE hid");
        ui.processLine("PROPERTY ID_USB_DRIVER usbhid");
        ui.processLine("PROPERTY ID_MODEL_ID 0001");
        ui.processLine("PROPERTY ID_REVISION 0100");
        ui.processLine("PROPERTY ID_USB_INTERFACES :030000:");
        ui.processLine("PROPERTY ID_USB_INTERFACE_NUM 00");
        ui.processLine("PROPERTY MAJOR 13");
        ui.processLine("PROPERTY MINOR 65");
        ui.processLine("PROPERTY SUBSYSTEM input");
        ui.processLine("CREATE");
        setAbsScale(4096, 4096);
    }

    @Override
    public int addPoint(double x, double y) {
        int p = super.addPoint(x, y);
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_KEY BTN_TOOL_PEN 1");
        ui.processLine("EV_ABS ABS_X " + transformedXs[p]);
        ui.processLine("EV_ABS ABS_Y " + transformedYs[p]);
        return p;
    }

    @Override
    public void removePoint(int p) {
        super.removePoint(p);
        ui.processLine("EV_KEY BTN_TOUCH 0");
    }

    @Override
    public void setPoint(int p, double x, double y) {
        int oldX = transformedXs[p];
        int oldY = transformedYs[p];
        super.setPoint(p, x, y);
        // if neither X nor Y have changed, we send X
        if (oldX != transformedXs[p] || oldY == transformedYs[p]) {
            ui.processLine("EV_ABS ABS_X " + transformedXs[p]);
        }
        if (oldY != transformedYs[p]) {
            ui.processLine("EV_ABS ABS_Y " + transformedYs[p]);
        }
    }

    @Override
    public void resendStateAndSync() {
        if (points[0]) {
            ui.processLine("EV_ABS ABS_X " + transformedXs[0]);
            ui.processLine("EV_ABS ABS_Y " + transformedYs[0]);
        }
        sync();
    }

}
