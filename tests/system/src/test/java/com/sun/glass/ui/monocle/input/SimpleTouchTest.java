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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for ABS_X ABS_Y touch events
 */
public class SimpleTouchTest extends TouchTestBase {

    @Before
    public void createDevice() throws Exception {
        ui = new UInput();
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.addTouchListeners();
        TestLog.reset();
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
        setAbsScale(4096, 4096);
    }

    /**
     * Touch down and up
     */
    @Test
    public void tap() throws Exception {
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x = Math.round(width * 0.5f);
        final int y = Math.round(height * 0.5f);
        // tap
        absPosition(x, y);
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_SYN SYN_REPORT 0");
        // release
        ui.processLine("EV_KEY BTN_TOUCH 0 ");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLog("Mouse pressed: %d, %d", x, y);
        TestLog.waitForLog("Mouse released: %d, %d", x, y);
        TestLog.waitForLog("Mouse clicked: %d, %d", x, y);
        TestLog.waitForLog("Touch pressed: %d, %d", x, y);
        TestLog.waitForLog("Touch released: %d, %d", x, y);
    }

    /**
     * Touch down, drag, touch up
     */
    @Test
    public void tapAndDrag1() throws Exception {
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = Math.round(width * 0.5f);
        final int y1 = Math.round(height * 0.5f);
        final int x2 = Math.round(width * 0.75f);
        final int y2 = Math.round(height * 0.75f);
        // tap
        absPosition(x1, y1);
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_SYN SYN_REPORT 0");
        // drag
        absPosition(x2, y2);
        ui.processLine("EV_SYN SYN_REPORT 0");
        // release
        ui.processLine("EV_KEY BTN_TOUCH 0 ");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Mouse dragged: %d, %d", x2, y2);
        TestLog.waitForLog("Mouse released: %d, %d", x2, y2);
        TestLog.waitForLog("Mouse clicked: %d, %d", x2, y2);
        TestLog.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Touch moved: %d, %d", x2, y2);
        TestLog.waitForLog("Touch released: %d, %d", x2, y2);
    }

    /**
     * Touch down, drag, touch up, with no change in Y coordinate
     */
    @Test
    @Ignore("RT-34296")
    public void tapAndDrag2() throws Exception {
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = Math.round(width * 0.5f);
        final int y1 = Math.round(height * 0.5f);
        final int x2 = Math.round(width * 0.75f);
        // tap
        absPosition(x1, y1);
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_SYN SYN_REPORT 0");
        // drag
        absPosition(x2, UNDEFINED);
        ui.processLine("EV_SYN SYN_REPORT 0");
        // release
        ui.processLine("EV_KEY BTN_TOUCH 0 ");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Mouse dragged: %d, %d", x2, y1);
        TestLog.waitForLog("Mouse released: %d, %d", x2, y1);
        TestLog.waitForLog("Mouse clicked: %d, %d", x2, y1);
        TestLog.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Touch moved: %d, %d", x2, y1);
        TestLog.waitForLog("Touch released: %d, %d", x2, y1);
    }

    /**
     * Touch down, drag, touch up, no change in X coordinate
     */
    @Test
    @Ignore("RT-34296")
    public void tapAndDrag3() throws Exception {
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = Math.round(width * 0.5f);
        final int y1 = Math.round(height * 0.5f);
        final int y2 = Math.round(height * 0.75f);
        // tap
        absPosition(x1, y1);
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_SYN SYN_REPORT 0");
        // drag
        absPosition(UNDEFINED, y2);
        ui.processLine("EV_SYN SYN_REPORT 0");
        // release
        ui.processLine("EV_KEY BTN_TOUCH 0 ");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Mouse dragged: %d, %d", x1, y2);
        TestLog.waitForLog("Mouse released: %d, %d", x1, y2);
        TestLog.waitForLog("Mouse clicked: %d, %d", x1, y2);
        TestLog.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Touch moved: %d, %d", x1, y2);
        TestLog.waitForLog("Touch released: %d, %d", x1, y2);
    }

    /**
     * Touch down, drag, touch up, coordinates sent before tap
     */
    @Test
    @Ignore("RT-34296")
    public void tapAndDrag4() throws Exception {
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = Math.round(width * 0.5f);
        final int y1 = Math.round(height * 0.5f);
        final int x2 = Math.round(width * 0.75f);
        final int y2 = Math.round(height * 0.75f);
        // send coordinates
        absPosition(x2, y2);
        ui.processLine("EV_SYN SYN_REPORT 0");
        // tap
        absPosition(x1, y1);
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_SYN SYN_REPORT 0");
        // drag
        absPosition(x2, y2);
        ui.processLine("EV_SYN SYN_REPORT 0");
        // release
        ui.processLine("EV_KEY BTN_TOUCH 0 ");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLog("Mouse pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Mouse dragged: %d, %d", x2, y2);
        TestLog.waitForLog("Mouse released: %d, %d", x2, y2);
        TestLog.waitForLog("Mouse clicked: %d, %d", x2, y2);
        TestLog.waitForLog("Touch pressed: %d, %d", x1, y1);
        TestLog.waitForLog("Touch moved: %d, %d", x2, y2);
        TestLog.waitForLog("Touch released: %d, %d", x2, y2);
    }

}

