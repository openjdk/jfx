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

package com.sun.glass.ui.monocle;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class SimpleMouseTest {

    private UInput ui;
    @Rule public TestName name = new TestName();

    @Before public void setUpScreen() throws Exception {
        TestLog.reset();
        TestLog.log(name.getMethodName());
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.movePointerTo(300, 300);
        initDevice();
    }

    public void initDevice() throws Exception {
        ui = new UInput();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("KEYBIT BTN_LEFT");
        ui.processLine("EVBIT EV_REL");
        ui.processLine("RELBIT REL_X");
        ui.processLine("RELBIT REL_Y");
        ui.processLine("RELBIT REL_WHEEL");
        ui.processLine("PROPERTY ID_INPUT_MOUSE 1");
        ui.processLine("CREATE");
    }

    @After public void destroyDevice() throws Exception {
        if (ui != null) {
            ui.waitForQuiet();
            try {
                ui.processLine("DESTROY");
            } catch (RuntimeException e) { }
            ui.processLine("CLOSE");
            ui.dispose();
        }
    }

    @Test
    public void testRelativeMove() throws Exception {
        ui.processLine("EV_REL REL_X -100");
        ui.processLine("EV_REL REL_Y -50");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse moved: 200, 250", 3000);
    }

    @Test
    public void testRelativeDrag() throws Exception {
        ui.processLine("EV_KEY BTN_LEFT 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_REL REL_X -100");
        ui.processLine("EV_REL REL_Y -50");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY BTN_LEFT 0");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse pressed: 300, 300", 3000);
        TestLog.waitForLog("Mouse dragged: 200, 250", 3000);
        TestLog.waitForLog("Mouse released: 200, 250", 3000);
    }

    @Test
    public void testWheel() throws Exception {
        TestApplication.getStage().getScene().setOnScroll(
                (e) -> TestLog.format("Scroll: %.0g",
                                      Math.signum(e.getDeltaY())));
        ui.processLine("EV_SYN");
        ui.processLine("EV_REL REL_WHEEL 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_REL REL_WHEEL 0");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Scroll: 1");
        TestLog.reset();

        ui.processLine("EV_REL REL_WHEEL -1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_REL REL_WHEEL 0");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Scroll: -1");
    }

    @Test
    public void testWheelSequence() throws Exception {
        TestApplication.getStage().getScene().setOnScroll(
                (e) -> TestLog.format("Scroll: %.0g",
                                      Math.signum(e.getDeltaY())));
        ui.processLine("EV_REL REL_WHEEL 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_REL REL_WHEEL 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_REL REL_WHEEL 1");
        ui.processLine("EV_SYN");
        new TestRunnable() {
            @Override
            public void test() {
                Assert.assertEquals(3, TestLog.countLogContaining("Scroll: 1"));
            }
        }.invokeAndWaitUntilSuccess(3000l);
        TestLog.reset();

        ui.processLine("EV_REL REL_WHEEL -1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_REL REL_WHEEL -1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_REL REL_WHEEL -1");
        ui.processLine("EV_SYN");
        new TestRunnable() {
            @Override
            public void test() {
                Assert.assertEquals(3, TestLog.countLogContaining("Scroll: -1"));
            }
        }.invokeAndWaitUntilSuccess(3000l);
    }

    @Test
    public void testClickLeft() throws Exception {
        ui.processLine("EV_KEY BTN_LEFT 1");
        ui.processLine("EV_SYN SYN_REPORT 0");
        ui.processLine("EV_KEY BTN_LEFT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLogContaining("Mouse pressed: 300, 300");
        TestLog.waitForLogContaining("Mouse released: 300, 300");
        TestLog.waitForLogContaining("Mouse clicked: 300, 300");
    }

    @Test
    public void testClickRight() throws Exception {
        ui.processLine("EV_KEY BTN_RIGHT 1");
        ui.processLine("EV_SYN SYN_REPORT 0");
        ui.processLine("EV_KEY BTN_RIGHT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLogContaining("Mouse pressed: 300, 300");
        TestLog.waitForLogContaining("Mouse released: 300, 300");
        TestLog.waitForLogContaining("Mouse clicked: 300, 300");
    }

    @Test
    public void testDragLookahead() throws Exception {
        Assume.assumeTrue(TestApplication.isMonocle());
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestLog.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = (int) Math.round(width * 0.1);
        final int y1 = (int) Math.round(height * 0.1);
        final int delta = (int) Math.min(width / 2.0, height / 2.0);
        final int x2 = x1 + delta;
        final int y2 = y1 + delta;
        final int x3 = (int) Math.round(width * 0.9);
        final int y3 = (int) Math.round(height * 0.9);
        // Move the mouse to 0, 0
        ui.processLine("EV_REL REL_X " + -width);
        ui.processLine("EV_REL REL_Y " + -height);
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse moved: 0, 0");
        // Move to x1, y1
        ui.processLine("EV_REL REL_X " + x1);
        ui.processLine("EV_REL REL_Y " + y1);
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse moved: %d, %d", x1, y1);
        // Push events while on the event thread, making sure that events
        // will be buffered up and enabling filtering to take place
        TestRunnable.invokeAndWait(() -> {
            ui.processLine("EV_KEY BTN_LEFT 1");
            ui.processLine("EV_SYN");
            for (int i = 0; i < delta; i++) {
                ui.processLine("EV_REL REL_X 1");
                ui.processLine("EV_REL REL_Y 1");
                ui.processLine("EV_SYN");
            }
            ui.processLine("EV_REL REL_X " + (x3 - x2));
            ui.processLine("EV_REL REL_Y " + (y3 - y2));
            ui.processLine("EV_SYN");
            ui.processLine("EV_KEY BTN_LEFT 0");
            ui.processLine("EV_SYN");
        });
        // Check that the initial point reported is correct
        TestLog.waitForLog("Mouse pressed: %d, %d", x1, y1);
        // Check that the final point reported is correct
        TestLog.waitForLog("Mouse released: %d, %d", x3, y3);
        TestLog.waitForLog("Mouse dragged: %d, %d", x3, y3);
        // Check that moves in between were filtered
        Assert.assertTrue(TestLog.countLogContaining("Mouse dragged") <= (x2 - x1) / 10);
    }

    @Test
    public void testMoveLookahead() throws Exception {
        Assume.assumeTrue(TestApplication.isMonocle());
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestLog.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = (int) Math.round(width * 0.1);
        final int y1 = (int) Math.round(height * 0.1);
        final int delta = (int) Math.min(width / 2.0, height / 2.0);
        final int x2 = x1 + delta;
        final int y2 = y1 + delta;
        final int x3 = (int) Math.round(width * 0.9);
        final int y3 = (int) Math.round(height * 0.9);
        // Move the mouse to 0, 0
        ui.processLine("EV_REL REL_X " + -width);
        ui.processLine("EV_REL REL_Y " + -height);
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse moved: 0, 0");
        // Move to x1, y1
        ui.processLine("EV_REL REL_X " + x1);
        ui.processLine("EV_REL REL_Y " + y1);
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse moved: %d, %d", x1, y1);
        // Push events while on the event thread, making sure that events
        // will be buffered up and enabling filtering to take place
        TestRunnable.invokeAndWait(() -> {
            for (int i = 0; i < delta; i++) {
                ui.processLine("EV_REL REL_X 1");
                ui.processLine("EV_REL REL_Y 1");
                ui.processLine("EV_SYN");
            }
            ui.processLine("EV_REL REL_X " + (x3 - x2));
            ui.processLine("EV_REL REL_Y " + (y3 - y2));
            ui.processLine("EV_SYN");
        });
        // Check that the final point reported is correct
        TestLog.waitForLog("Mouse moved: %d, %d", x3, y3);
        // Check that moves in between were filtered
        Assert.assertTrue(TestLog.countLogContaining("Mouse moved") <= (x2 - x1) / 10);
        // Check that we didn't get any other events
        Assert.assertEquals(0, TestLog.countLogContaining("Mouse pressed"));
        Assert.assertEquals(0, TestLog.countLogContaining("Mouse released"));
        Assert.assertEquals(0, TestLog.countLogContaining("Mouse clicked"));
    }

    @Test
    public void testGrab1() throws Exception {
        TestApplication.showInMiddleOfScreen();
        TestApplication.addMouseListeners();
        Rectangle2D r = TestApplication.getScreenBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = (int) Math.round(width * 0.5);
        final int y1 = (int) Math.round(height * 0.5);
        final int x2 = (int) Math.round(width * 0.7);
        final int y2 = (int) Math.round(height * 0.7);
        final int x3 = (int) Math.round(width * 0.9);
        final int y3 = (int) Math.round(height * 0.9);
        TestApplication.movePointerTo(x1, y1);
        // press
        ui.processLine("EV_KEY BTN_LEFT 1");
        ui.processLine("EV_SYN");
        // drag to x2, y2
        ui.processLine("EV_REL REL_X " + (x2 - x1));
        ui.processLine("EV_REL REL_Y " + (y2 - y1));
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse dragged: %d, %d", x2, y2);
        // drag to x3, y3
        ui.processLine("EV_REL REL_X " + (x3 - x2));
        ui.processLine("EV_REL REL_Y " + (y3 - y2));
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse dragged: %d, %d", x3, y3);
        TestLog.waitForLog("Mouse exited: %d, %d", x3, y3);
        // drag to x2, y2
        ui.processLine("EV_REL REL_X " + (x2 - x3));
        ui.processLine("EV_REL REL_Y " + (y2 - y3));
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse dragged: %d, %d", x2, y2);
        TestLog.waitForLog("Mouse entered: %d, %d", x2, y2);
        // release
        ui.processLine("EV_KEY BTN_LEFT 0");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse released: %d, %d", x2, y2);
        TestLog.waitForLog("Mouse clicked: %d, %d", x2, y2);
    }

    @Test
    public void testGrab2() throws Exception {
        TestApplication.showInMiddleOfScreen();
        TestApplication.addMouseListeners();
        Assume.assumeTrue(TestApplication.isMonocle());
        Rectangle2D r = TestApplication.getScreenBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = (int) Math.round(width * 0.5);
        final int y1 = (int) Math.round(height * 0.5);
        final int x2 = (int) Math.round(width * 0.7);
        final int y2 = (int) Math.round(height * 0.7);
        final int x3 = (int) Math.round(width * 0.9);
        final int y3 = (int) Math.round(height * 0.9);
        TestApplication.movePointerTo(x1, y1);
        // press
        ui.processLine("EV_KEY BTN_LEFT 1");
        ui.processLine("EV_SYN");
        // drag to x2, y2
        ui.processLine("EV_REL REL_X " + (x2 - x1));
        ui.processLine("EV_REL REL_Y " + (y2 - y1));
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse dragged: %d, %d", x2, y2);
        // drag to x3, y3
        ui.processLine("EV_REL REL_X " + (x3 - x2));
        ui.processLine("EV_REL REL_Y " + (y3 - y2));
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse dragged: %d, %d", x3, y3);
        TestLog.waitForLog("Mouse exited: %d, %d", x3, y3);
        // release
        ui.processLine("EV_KEY BTN_LEFT 0");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Mouse released: %d, %d", x3, y3);
    }

}
