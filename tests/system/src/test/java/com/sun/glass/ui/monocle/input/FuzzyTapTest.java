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

import javafx.event.Event;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.io.PrintStream;

public class FuzzyTapTest extends TouchTestBase {

    /** Touch down and up at different coordinates */
    @Test
    public void tap1() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestLog.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x = width / 2;
        final int y = height / 2;
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
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_X " + x);
        ui.processLine("EV_ABS ABS_Y " + y);
        ui.processLine("EV_SYN");
        ui.processLine("EV_ABS ABS_X " + (x));
        ui.processLine("EV_ABS ABS_Y " + (y));
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_ABS ABS_X " + (x));
        ui.processLine("EV_ABS ABS_Y " + (y));
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse pressed: "
                + x + ", " + y, 3000);
        TestLog.waitForLog("Mouse released: "
                + x + ", " + y, 3000);
    }

    /** Touch down, touch up at a slightly different location*/
    @Test
    public void tap2() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestLog.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x = width / 2;
        final int y = height / 2;
        final int tapRadius = TestApplication.getTapRadius();
        final int x1 = x + tapRadius / 2;
        final int y1 = y + tapRadius / 2;
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
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_X " + x);
        ui.processLine("EV_ABS ABS_Y " + y);
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_ABS ABS_X " + x1);
        ui.processLine("EV_ABS ABS_Y " + y1);
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse pressed: " + x + ", " + y, 3000);
        TestLog.waitForLog("Mouse clicked: " + x + ", " + y, 3000);
        TestLog.waitForLog("Mouse released: " + x + ", " + y, 3000);
        Assert.assertEquals(0, TestLog.countLogContaining("Mouse dragged:"));
        Assert.assertEquals(0, TestLog.countLogContaining("Touch moved:"));
    }

    /** Touch down, small move, touch up */
    @Test
    public void tap2a() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestLog.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x = width / 2;
        final int y = height / 2;
        final int tapRadius = TestApplication.getTapRadius();
        final int x1 = x + tapRadius / 2;
        final int y1 = y + tapRadius / 2;
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
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_X " + x);
        ui.processLine("EV_ABS ABS_Y " + y);
        ui.processLine("EV_SYN");
        ui.processLine("EV_ABS ABS_X " + x1);
        ui.processLine("EV_ABS ABS_Y " + y1);
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_ABS ABS_X " + x1);
        ui.processLine("EV_ABS ABS_Y " + y1);
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse pressed: " + x + ", " + y, 3000);
        TestLog.waitForLog("Mouse clicked: " + x + ", " + y, 3000);
        TestLog.waitForLog("Mouse released: " + x + ", " + y, 3000);
        Assert.assertEquals(0, TestLog.countLogContaining("Mouse dragged:"));
        Assert.assertEquals(0, TestLog.countLogContaining("Touch moved:"));
    }

    /** Touch down, touch up outside the tap radius NOTE: its not clear if
     *  release event, on single-touch screen. must include coordinates */
    @Test
    public void tap3() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.addTouchListeners();
        TestLog.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x = width / 2;
        final int y = height / 2;
        final int tapRadius = TestApplication.getTapRadius();
        final int x1 = x + tapRadius * 2;
        final int y1 = y + tapRadius * 2;
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
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_X " + x);
        ui.processLine("EV_ABS ABS_Y " + y);
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_ABS ABS_X " + x1);
        ui.processLine("EV_ABS ABS_Y " + y1);
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse pressed: " + x + ", " + y, 3000);
        TestLog.waitForLog("Mouse released: " + x + ", " + y, 3000);
        TestLog.waitForLog("Mouse clicked: " + x + ", " + y, 3000);
        TestLog.waitForLog("Touch pressed: " + x + ", " + y, 3000);
        TestLog.waitForLog("Touch released: " + x + ", " + y, 3000);
        Assert.assertEquals(1, TestLog.countLogContaining("Mouse clicked:"));
    }

    /** Touch down, move outside touch radius, touch up */
    @Test
    public void tap3a() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.addTouchListeners();
        TestLog.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x = width / 2;
        final int y = height / 2;
        final int tapRadius = TestApplication.getTapRadius();
        final int x1 = x + tapRadius * 2;
        final int y1 = y + tapRadius * 2;
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
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_X " + x);
        ui.processLine("EV_ABS ABS_Y " + y);
        ui.processLine("EV_SYN");
        ui.processLine("EV_ABS ABS_X " + x1);
        ui.processLine("EV_ABS ABS_Y " + y1);
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_ABS ABS_X " + x1);
        ui.processLine("EV_ABS ABS_Y " + y1);
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse pressed: " + x + ", " + y, 3000);
        TestLog.waitForLog("Mouse dragged: " + x1 + ", " + y1, 3000);
        TestLog.waitForLog("Mouse released: " + x1 + ", " + y1, 3000);
        TestLog.waitForLog("Mouse clicked: " + x1 + ", " + y1, 3000);
        TestLog.waitForLog("Touch pressed: " + x + ", " + y, 3000);
        TestLog.waitForLog("Touch moved: " + x1 + ", " + y1, 3000);
        TestLog.waitForLog("Touch released: " + x1 + ", " + y1, 3000);
        Assert.assertEquals(1, TestLog.countLogContaining("Mouse clicked: " + x1 + ", " + y1));
    }

    /** Touch down, drift outside touch radius, touch up */
    @Test
    public void tap3b() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.addTouchListeners();
        TestLog.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x = width / 2;
        final int y = height / 2;
        final int tapRadius = TestApplication.getTapRadius();
        final int x1 = x + tapRadius * 2;
        final int y1 = y + tapRadius * 2;
        final int x2 = x + tapRadius * 3;
        final int y2 = y + tapRadius * 3;
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
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_X " + x);
        ui.processLine("EV_ABS ABS_Y " + y);
        ui.processLine("EV_SYN");
        // drift out of the tap radius
        for (int i = 0; i < tapRadius * 2; i++) {
            ui.processLine("EV_ABS ABS_X " + (x + i));
            ui.processLine("EV_ABS ABS_Y " + (y + i));
            ui.processLine("EV_SYN");
        }
        // extra moves to make sure the final move is not filtered out
        ui.processLine("EV_ABS ABS_X 0");
        ui.processLine("EV_ABS ABS_Y 0");
        ui.processLine("EV_SYN");
        ui.processLine("EV_ABS ABS_X " + x2);
        ui.processLine("EV_ABS ABS_Y " + y2);
        ui.processLine("EV_SYN");
        // and release
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_ABS ABS_X " + x2);
        ui.processLine("EV_ABS ABS_Y " + y2);
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse pressed: " + x + ", " + y, 3000);
        TestLog.waitForLog("Mouse dragged: " + x2 + ", " + y2, 3000);
        TestLog.waitForLog("Mouse released: " + x2 + ", " + y2, 3000);
        TestLog.waitForLog("Mouse clicked: " + x2 + ", " + y2, 3000);
        TestLog.waitForLog("Touch pressed: " + x + ", " + y, 3000);
        TestLog.waitForLog("Touch moved: " + x2 + ", " + y2, 3000);
        TestLog.waitForLog("Touch released: " + x2 + ", " + y2, 3000);
        Assert.assertEquals(1, TestLog.countLogContaining("Mouse clicked: " + x2 + ", " + y2));
    }
}
