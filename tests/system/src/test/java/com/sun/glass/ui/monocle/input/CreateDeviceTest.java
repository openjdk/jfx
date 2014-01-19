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

package com.sun.glass.ui.monocle.input;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CreateDeviceTest {

    private UInput ui;

    @Before public void initDevice() {
        TestLog.reset();
        ui = new UInput();
    }

    @After public void destroyDevice() throws InterruptedException {
        try {
            ui.processLine("DESTROY");
        } catch (RuntimeException e) { }
        ui.processLine("CLOSE");
        ui.dispose();
    }

    @Test
    public void testCreateKeyDevice() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addKeyListeners();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("KEYBIT KEY_A");
        ui.processLine("KEYBIT KEY_LEFTSHIFT");
        ui.processLine("PROPERTY ID_INPUT_KEYBOARD 1");
        ui.processLine("CREATE");
        ui.processLine("EV_KEY KEY_LEFTSHIFT 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_LEFTSHIFT 0");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Key pressed: SHIFT", 3000);
        TestLog.clear();

        ui.processLine("EV_KEY KEY_A 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_A 0");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Key typed: a", 3000);
        ui.processLine("EV_KEY KEY_LEFTSHIFT 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_A 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_A 0");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_LEFTSHIFT 0");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Key typed: A", 3000);
        // make sure only two key typed events were received
        String[] log = TestLog.getLog();
        int typedEvents = 0;
        for (int i = 0; i < log.length; i++) {
            if (log[i].startsWith("Key typed")) {
                typedEvents ++;
            }
        }
        Assert.assertEquals("Expected two typed events", 2, typedEvents);
    }

    @Test
    public void testCreateMouseDevice() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.movePointerTo(300, 300);
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("KEYBIT BTN_LEFT");
        ui.processLine("EVBIT EV_REL");
        ui.processLine("RELBIT REL_X");
        ui.processLine("RELBIT REL_Y");
        ui.processLine("PROPERTY ID_INPUT_MOUSE 1");
        ui.processLine("CREATE");
        TestLog.clear();
        ui.processLine("EV_KEY BTN_LEFT 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY BTN_LEFT 0");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse pressed: 300, 300", 3000);
        ui.processLine("EV_REL REL_X -10");
        ui.processLine("EV_REL REL_Y -5");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse moved: 290, 295", 3000);
    }

    @Test
    public void testCreateTouchDevice() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("KEYBIT BTN_TOUCH");
        ui.processLine("EVBIT EV_ABS");
        ui.processLine("ABSBIT ABS_X");
        ui.processLine("ABSBIT ABS_Y");
        ui.processLine("ABSMIN ABS_X 0");
        ui.processLine("ABSMAX ABS_X 4095");
        ui.processLine("ABSMIN ABS_Y 0");
        ui.processLine("ABSMAX ABS_Y 4095");
        ui.processLine("PROPBIT INPUT_PROP_POINTER");
        ui.processLine("PROPBIT INPUT_PROP_DIRECT");
        ui.processLine("PROPERTY ID_INPUT_TOUCHSCREEN 1");
        ui.processLine("CREATE");
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_X 2048");
        ui.processLine("EV_ABS ABS_Y 2048");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_ABS ABS_X 2048");
        ui.processLine("EV_ABS ABS_Y 2048");
        ui.processLine("EV_SYN");
        Rectangle2D r = Screen.getPrimary().getBounds();
        TestLog.waitForLog("Mouse pressed: "
                        + (int) r.getWidth() / 2 + ", " + (int) r.getHeight() / 2   , 3000);
    }

}
