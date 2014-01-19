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
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * This is a regression test for RT-33771 - Lens:FXML-LoginDemo throws
 * java.lang.RuntimeException: Platform reported wrong touch point ID. 
 *  
 * and  RT-33687 - Lens:some touch events are been dropped in native 
 * causing exceptions to be thrown. 
 *  
 */
public class DragTouchInAndOutAWindowTest {

    private UInput ui;
    @Rule public TestName name = new TestName();

    @Before public void setUpScreen() throws Exception {
        TestLog.reset();
        TestLog.log(name.getMethodName());
      
        TestApplication.showInMiddleOfScreen();
        TestApplication.addTouchListeners();

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
            try {
                ui.processLine("DESTROY");
            } catch (RuntimeException e) { }
            ui.processLine("CLOSE");
            ui.dispose();
        }
    }

    private void resetState() {
        //make sure we reset the state of the window manager in case of exception
        ui.processLine("EV_ABS ABS_MT_POSITION_X 0");
        ui.processLine("EV_ABS ABS_MT_POSITION_Y 0");
        ui.processLine("EV_SYN SYN_MT_REPORT 0"); 
        ui.processLine("EV_SYN SYN_REPORT 0");

         ui.processLine("EV_SYN SYN_MT_REPORT 0");
         ui.processLine("EV_SYN SYN_REPORT 0");
    }

    /** 
     * RT-33771 stated that exceptions are been thrown because the state of the 
     * point, when entering the window, is wrong. 
     * Test check that states are ok and no exception is been thrown 
     *  
     * Test update for RT-34191 - make sure no touch event received if drag 
     * started outside the window 
     */
    @Test
    public void singleTouch_dragPointIntoTheWindow() throws Exception {
        Assume.assumeTrue(!TestApplication.isMonocle()); // RT-35406
        TestLog.reset();
        Stage stage = TestApplication.getStage();
        int windowRightEnd = (int)(stage.getX() + stage.getWidth());
        int windowMiddleHeight = (int)(stage.getY() + (stage.getHeight() / 2));


        //start outside the window and drag point into it (move in big steps
        //to avoid filtering)
        //expected:
        //1) no exception
        //2) no  press | move | release notifications
        for (int i = 50; i >= -50 ; i -= 3) {
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + (windowRightEnd + i));
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + windowMiddleHeight);
            ui.processLine("EV_SYN SYN_MT_REPORT 0");
            ui.processLine("EV_SYN SYN_REPORT 0");
        }

        //release
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");

        //check that tested window didn't recive any notifications

        //wait for results and make sure no event received
        Assert.assertEquals(0, TestLog.countLogContaining("TouchPoint: PRESSED"));
        Assert.assertEquals(0, TestLog.countLogContaining("TouchPoint: MOVED"));
        Assert.assertEquals(0, TestLog.countLogContaining("TouchPoint: RELEASED"));
    }

    @Test
    /**
     * This test is also related to RT-33687 - Lens:some touch events are been 
     * dropped in native causing exceptions to be thrown. 
     * In short there was a problem that when touch point moved outside a window 
     * no notifications were sent, especially releases.
     * 
     */
    public void singleTouch_dragPointoutsideAwindow() throws Exception {
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int screenWidth = (int) r.getWidth();
        TestLog.reset();
        Stage stage = TestApplication.getStage();
        int windowMiddleWidth = (int)(stage.getX() + stage.getWidth() / 2);
        int WindowMiddleHeight = (int)(stage.getY() + (stage.getHeight() / 2));
        resetState();


        //touch inside the window and drag the touch point to the end of the screen
        for (int i = 0; i + windowMiddleWidth < screenWidth ; i += 5) {
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + (i + windowMiddleWidth));
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + WindowMiddleHeight);
            ui.processLine("EV_SYN SYN_MT_REPORT 0");
            ui.processLine("EV_SYN SYN_REPORT 0");
        }

        //wait for results
        TestLog.waitForLogContaining("TouchPoint: PRESSED", 3000);
        TestLog.waitForLogContaining("TouchPoint: MOVED", 3000);

        //release outside the window
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        //check that we get the event
        TestLog.waitForLogContaining("TouchPoint: RELEASED", 3000);
    }

     @Test
    /**
     * Combining the two test cases above, start a touch sequence inside a 
     * window, drag the 'finger' out and in again and see that we gat the 
     * events. 
     * 
     */
    public void singleTouch_dragPointInandOutAwindow() throws Exception {
        TestLog.reset();
        Stage stage = TestApplication.getStage();
        int windowMiddleWidth = (int)(stage.getX() + stage.getWidth() / 2);
        int WindowMiddleHeight = (int)(stage.getY() + (stage.getHeight() / 2));
        int windowRightEnd = (int)(stage.getX() + stage.getWidth());
        int i;
        resetState();

        //start inside the window and drag point outside 
        for (i = windowMiddleWidth; i <= windowRightEnd+100 ; i += 10) {
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + i);
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + WindowMiddleHeight);
            ui.processLine("EV_SYN SYN_MT_REPORT 0");
            ui.processLine("EV_SYN SYN_REPORT 0");
        }

        //wait for results
        TestLog.waitForLogContaining("TouchPoint: PRESSED", 3000);
        TestLog.waitForLogContaining("TouchPoint: MOVED", 3000);

        //continue from where we stopped and drag point back to window
        for (; i >= windowMiddleWidth  ; i -= 10) {
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + i);
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + WindowMiddleHeight);
            ui.processLine("EV_SYN SYN_MT_REPORT 0");
            ui.processLine("EV_SYN SYN_REPORT 0");
        }

        //release inside the window
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        //check that we get the event
        TestLog.waitForLogContaining("TouchPoint: RELEASED", 3000);
    }

     @Test
    /**
     * Same test as above, but for multi touch. 
     * Test should pass in either single touch mode or multi touch mode 
     * Main point is to see that no exception is been thrown
     * 
     */
    public void multiTouch_dragPointInandOutAwindow() throws Exception {
        Assume.assumeTrue(!TestApplication.isMonocle()); // RT-35406
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int screenWidth = (int) r.getWidth();
        TestLog.reset();
        Stage stage = TestApplication.getStage();
        int windowMiddleWidth = (int)(stage.getX() + stage.getWidth() / 2);
        int WindowMiddleHeight = (int)(stage.getY() + (stage.getHeight() / 2));
        int windowRightEnd = (int)(stage.getX() + stage.getWidth());
        int i;
        resetState();

        //start outside the window and drag point into the center of window 
        for (i = windowRightEnd+15; i >= windowMiddleWidth ; i -= 3) {
            //first finger
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + i);
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + WindowMiddleHeight);
            ui.processLine("EV_SYN SYN_MT_REPORT 0");
            //second finger
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + i);
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + (WindowMiddleHeight + 10));
            ui.processLine("EV_SYN SYN_MT_REPORT 0");
            ui.processLine("EV_SYN SYN_REPORT 0");
        }

        //continue from where we stopped and drag point outside the window to 
        //the end of screen
        for (; i + windowMiddleWidth < screenWidth ; i += 5) {
            //first finger
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + (i + windowMiddleWidth));
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + WindowMiddleHeight);
            ui.processLine("EV_SYN SYN_MT_REPORT 0");
            //second finger
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + i);
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + WindowMiddleHeight+10);
            ui.processLine("EV_SYN SYN_MT_REPORT 0");
            
            ui.processLine("EV_SYN SYN_REPORT 0");
        }

        //release all points outside the window
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");

        //wait for results and make sure no event received
        Assert.assertEquals(0, TestLog.countLogContaining("TouchPoint: PRESSED"));
        Assert.assertEquals(0, TestLog.countLogContaining("TouchPoint: MOVED"));
        Assert.assertEquals(0, TestLog.countLogContaining("TouchPoint: RELEASED"));
    }
}
