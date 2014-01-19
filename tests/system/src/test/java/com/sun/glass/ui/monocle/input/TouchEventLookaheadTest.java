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

package com.sun.glass.ui.monocle.input;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class TouchEventLookaheadTest extends TouchTestBase {

    /** Merge together similar moves */
    @Test
    public void mergeMoves() throws Exception {
        Assume.assumeTrue(TestApplication.isMonocle());
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.addTouchListeners();
        TestLog.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = width / 2;
        final int y1 = height / 2;
        final int x2 = (width * 3) / 4;
        final int y2 = (height * 3) / 4;
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("KEYBIT BTN_TOUCH");
        ui.processLine("EVBIT EV_ABS");
        ui.processLine("ABSBIT ABS_X");
        ui.processLine("ABSBIT ABS_Y");
        ui.processLine("ABSMIN ABS_X 0");
        ui.processLine("ABSMAX ABS_X " + width);
        ui.processLine("ABSMIN ABS_Y 0");
        ui.processLine("ABSMAX ABS_Y " + height);
        ui.processLine("PROPBIT INPUT_PROP_POINTER");
        ui.processLine("PROPBIT INPUT_PROP_DIRECT");
        ui.processLine("PROPERTY ID_INPUT_TOUCHSCREEN 1");
        ui.processLine("CREATE");
        // Push events while on the event thread, making sure that events
        // will be buffered up and enabling filtering to take place
        TestRunnable.invokeAndWait(() -> {
            ui.processLine("EV_KEY BTN_TOUCH 1");
            ui.processLine("EV_ABS ABS_X " + x1);
            ui.processLine("EV_ABS ABS_Y " + y1);
            ui.processLine("EV_SYN");
            for (int x = x1; x <= x2; x += (x2 - x1) / 10) {
                ui.processLine("EV_ABS ABS_X " + x);
                ui.processLine("EV_ABS ABS_Y " + y1);
                ui.processLine("EV_SYN");
            }
            for (int y = y1; y <= y2; y += (y2 - y1) / 10) {
                ui.processLine("EV_ABS ABS_X " + x2);
                ui.processLine("EV_ABS ABS_Y " + y);
                ui.processLine("EV_SYN");
            }
            ui.processLine("EV_KEY BTN_TOUCH 0");
            ui.processLine("EV_ABS ABS_X " + x2);
            ui.processLine("EV_ABS ABS_Y " + y2);
            ui.processLine("EV_SYN");
        });
        // Check that the initial point reported is correct
        TestLog.waitForLog("Mouse pressed: " + x1 + ", " + y1, 3000);
        TestLog.waitForLog("Touch pressed: " + x1 + ", " + y1, 3000);
        // Check that the final point reported is correct
        TestLog.waitForLog("Mouse released: " + x2 + ", " + y2, 3000);
        TestLog.waitForLog("Touch released: " + x2 + ", " + y2, 3000);
        // Check that there was only one move in between
        TestLog.waitForLog("Mouse dragged: " + x2 + ", " + y2, 3000);
        TestLog.waitForLog("Touch moved: " + x2 + ", " + y2, 3000);
        Assert.assertEquals(1, TestLog.countLogContaining("Mouse dragged"));
        Assert.assertEquals(1, TestLog.countLogContaining("Touch moved"));
    }
}
