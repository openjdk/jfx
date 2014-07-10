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
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class TouchLagTest {

    private UInput ui;
    @Rule public TestName name = new TestName();

    @Before public void setUpScreen() throws Exception {
        TestLog.reset();
        TestLog.log(name.getMethodName());
        TestApplication.showFullScreenScene();
        TestApplication.addTouchListeners();
        TestApplication.addMouseListeners();
        initDevice();
    }

    public void initDevice() throws Exception {
        ui = new UInput();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
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
        ui.processLine("ABSMAX ABS_X " + width);
        ui.processLine("ABSMIN ABS_Y 0");
        ui.processLine("ABSMAX ABS_Y " + height);
        ui.processLine("ABSMIN ABS_MT_POSITION_X 0");
        ui.processLine("ABSMAX ABS_MT_POSITION_X " + width);
        ui.processLine("ABSMIN ABS_MT_POSITION_Y 0");
        ui.processLine("ABSMAX ABS_MT_POSITION_Y " + height);
        ui.processLine("ABSMIN ABS_MT_ORIENTATION 0");
        ui.processLine("ABSMAX ABS_MT_ORIENTATION 1");
        ui.processLine("PROPBIT INPUT_PROP_POINTER");
        ui.processLine("PROPBIT INPUT_PROP_DIRECT");
        ui.processLine("PROPERTY ID_INPUT_TOUCHSCREEN 1");
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

    /** Make sure we can process 1000 touch move events per second. We are
     * not required to report all these events, but must report the last one.
     */
    @Test
    public void testTouchLag() throws Exception {
        TestLog.reset();
        ui.processLine("EV_ABS ABS_X 300");
        ui.processLine("EV_ABS ABS_Y 300");
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_MT_POSITION_X 300");
        ui.processLine("EV_ABS ABS_MT_POSITION_Y 300");
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLogContaining("TouchPoint: PRESSED", 3000l);
        // pre-process move event data into a byte array. That way we don't
        // have to count the time it takes to convert string event descriptions
        // into a byte stream.
        byte[] b = new byte[1024];
        int offset = 0;
        int[] xs = new int[2];
        int[] ys = new int[2];
        offset = ui.writeLine(b, offset, "EV_ABS ABS_X 0");
        xs[0] = offset - 4;
        offset = ui.writeLine(b, offset, "EV_ABS ABS_Y 0");
        ys[0] = offset - 4;
        offset = ui.writeLine(b, offset, "EV_ABS ABS_MT_POSITION_X 0");
        xs[1] = offset - 4;
        offset = ui.writeLine(b, offset, "EV_ABS ABS_MT_POSITION_Y 0");
        ys[1] = offset - 4;
        offset = ui.writeLine(b, offset, "EV_SYN SYN_MT_REPORT 0");
        offset = ui.writeLine(b, offset, "EV_SYN SYN_REPORT 0");
        int moveLength = offset;
        // Spam JavaFX with touch move events
        long startTime = System.currentTimeMillis();
        for (int y = 300; y < 310; y++) {
            for (int x = 300; x > 150; x--) {
                ui.writeValue(b, xs[0], x);
                ui.writeValue(b, xs[1], x);
                ui.writeValue(b, ys[0], y);
                ui.writeValue(b, ys[1], y);
                ui.write(b, 0, moveLength);
            }
            for (int x = 150; x < 300; x++) {
                ui.writeValue(b, xs[0], x);
                ui.writeValue(b, xs[1], x);
                ui.writeValue(b, ys[0], y);
                ui.writeValue(b, ys[1], y);
                ui.write(b, 0, moveLength);
            }
        }
        long t = System.currentTimeMillis() - startTime;
        // Make sure events could be sent in the required time
        Assert.assertTrue("Took " + t + "ms to send 3000 events",
                          t < (long) (3000l * TestApplication.getTimeScale()));
        TestLog.log("Sent 3000 events in " + t + "ms");
        // move to 400, 410
        ui.writeValue(b, xs[0], 400);
        ui.writeValue(b, xs[1], 400);
        ui.writeValue(b, ys[0], 410);
        ui.writeValue(b, ys[1], 410);
        ui.write(b, 0, moveLength);
        // release 
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        // Make sure events could be delivered in the required time
        TestLog.waitForLog("Touch moved: 400, 410", 3000l - t);
    }

    /** Make sure we can process 1000 multitouch move events per second. We are
     * not required to report all these events, but must report the last one.
     */
    @Test
    public void testMultitouchLag() throws Exception {
        TestLog.reset();
        ui.processLine("EV_ABS ABS_X 300");
        ui.processLine("EV_ABS ABS_Y 300");
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_MT_POSITION_X 300");
        ui.processLine("EV_ABS ABS_MT_POSITION_Y 300");
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLogContaining("TouchPoint: PRESSED", 3000);
        // pre-process move event data into a byte array. That way we don't
        // have to count the time it takes to convert string event descriptions
        // into a byte stream.
        byte[] b = new byte[1024];
        int offset = 0;
        int baseX, baseY;
        int[] xs = new int[2];
        int[] ys = new int[2];
        offset = ui.writeLine(b, offset, "EV_ABS ABS_X 0");
        baseX = offset - 4;
        offset = ui.writeLine(b, offset, "EV_ABS ABS_Y 0");
        baseY = offset - 4;
        offset = ui.writeLine(b, offset, "EV_ABS ABS_MT_POSITION_X 0");
        xs[0] = offset - 4;
        offset = ui.writeLine(b, offset, "EV_ABS ABS_MT_POSITION_Y 0");
        ys[0] = offset - 4;
        offset = ui.writeLine(b, offset, "EV_SYN SYN_MT_REPORT 0");
        offset = ui.writeLine(b, offset, "EV_ABS ABS_MT_POSITION_X 0");
        xs[1] = offset - 4;
        offset = ui.writeLine(b, offset, "EV_ABS ABS_MT_POSITION_Y 0");
        ys[1] = offset - 4;
        offset = ui.writeLine(b, offset, "EV_SYN SYN_MT_REPORT 0");
        offset = ui.writeLine(b, offset, "EV_SYN SYN_REPORT 0");
        int moveLength = offset;
        // Spam JavaFX with touch move events
        long startTime = System.currentTimeMillis();
        for (int y = 300; y < 310; y++) {
            for (int x = 300; x > 150; x--) {
                ui.writeValue(b, baseX, x);
                ui.writeValue(b, baseY, y);
                ui.writeValue(b, xs[0], x);
                ui.writeValue(b, xs[1], (x * 3) / 2);
                ui.writeValue(b, ys[0], y);
                ui.writeValue(b, ys[1], (y * 2) / 3);
                ui.write(b, 0, moveLength);
            }
            for (int x = 150; x < 300; x++) {
                ui.writeValue(b, baseX, x);
                ui.writeValue(b, baseY, y);
                ui.writeValue(b, xs[0], x);
                ui.writeValue(b, xs[1], (x * 3) / 2);
                ui.writeValue(b, ys[0], y);
                ui.writeValue(b, ys[1], (y * 2) / 3);
                ui.write(b, 0, moveLength);
            }
        }
        long t = System.currentTimeMillis() - startTime;
        // Make sure events could be sent in the required time
        Assert.assertTrue("Took " + t + "ms to send 3000 events",
                          t < (long) (3000l * TestApplication.getTimeScale()));
        TestLog.log("Sent 3000 events in " + t + "ms");
        // move to (400, 410), (350, 360);
        ui.writeValue(b, baseX, 400);
        ui.writeValue(b, baseY, 410);
        ui.writeValue(b, xs[0], 400);
        ui.writeValue(b, ys[0], 410);
        ui.writeValue(b, xs[1], 350);
        ui.writeValue(b, ys[1], 360);
        ui.write(b, 0, moveLength);
        // release one finger at 350, 360
        ui.processLine("EV_ABS ABS_X 400");
        ui.processLine("EV_ABS ABS_Y 410");
        ui.processLine("EV_ABS ABS_MT_POSITION_X 400");
        ui.processLine("EV_ABS ABS_MT_POSITION_Y 410");
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        // release the other finger
        ui.processLine("EV_ABS ABS_X 400");
        ui.processLine("EV_ABS ABS_Y 410");
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        // Make sure events could be delivered in the required time
        TestLog.waitForLog("Touch released: 400, 410", 3000l - t);
    }
}
