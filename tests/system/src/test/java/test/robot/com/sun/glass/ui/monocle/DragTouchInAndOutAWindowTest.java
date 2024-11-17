/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.com.sun.glass.ui.monocle;

import java.util.Collection;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.glass.ui.monocle.TestLogShim;
import test.com.sun.glass.ui.monocle.TestRunnable;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

/**
 * This is a regression test for RT-33771 - Lens:FXML-LoginDemo throws
 * java.lang.RuntimeException: Platform reported wrong touch point ID.
 *
 * and  RT-33687 - Lens:some touch events are been dropped in native
 * causing exceptions to be thrown.
 *
 */
public final class DragTouchInAndOutAWindowTest extends ParameterizedTestBase {

    private static Collection<TestTouchDevice> parameters() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }


    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    private void setUpScreen(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        TestApplication.showInMiddleOfScreen();
        TestApplication.addTouchListeners();
        int p = device.addPoint(0, 0);
        device.sync();
        device.removePoint(p);
        device.sync();
        TestLogShim.reset();
    }

    /**
     * RT-33771 stated that exceptions are been thrown because the state of the
     * point, when entering the window, is wrong.
     * Test check that states are ok and no exception is been thrown
     *
     * Test update for RT-34191 - make sure no touch event received if drag
     * started outside the window
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void singleTouch_dragPointIntoTheWindow(TestTouchDevice device) throws Exception {
        setUpScreen(device);
        Stage stage = TestApplication.getStage();
        int windowRightEnd = (int)(stage.getX() + stage.getWidth());
        int windowMiddleHeight = (int)(stage.getY() + (stage.getHeight() / 2));

        //start outside the window and drag point into it (move in big steps
        //to avoid filtering)
        //expected:
        //1) no exception
        //2) no  press | move | release notifications
        int p = device.addPoint(windowRightEnd + 50, windowMiddleHeight);
        device.sync();
        for (int i = 49; i >= -50 ; i -= 3) {
            device.setPoint(p, windowRightEnd + i, windowMiddleHeight);
            device.sync();
        }

        //
        device.removePoint(p);
        device.sync();

        //check that tested window didn't recive any notifications

        //wait for results and make sure no event received
        Assertions.assertEquals(0, TestLogShim.countLogContaining("TouchPoint: PRESSED"));
        Assertions.assertEquals(0, TestLogShim.countLogContaining("TouchPoint: MOVED"));
        Assertions.assertEquals(0, TestLogShim.countLogContaining("TouchPoint: RELEASED"));
    }

    /**
     * This test is also related to RT-33687 - Lens:some touch events are been
     * dropped in native causing exceptions to be thrown.
     * In short there was a problem that when touch point moved outside a window
     * no notifications were sent, especially releases.
     *
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void singleTouch_dragPointoutsideAwindow(TestTouchDevice device) throws Exception {
        setUpScreen(device);
        Stage stage = TestApplication.getStage();
        int windowMiddleWidth = (int)(stage.getX() + stage.getWidth() / 2);
        int windowMiddleHeight = (int)(stage.getY() + (stage.getHeight() / 2));

        //touch inside the window and drag the touch point to the end of the screen
        int p = device.addPoint(windowMiddleWidth, windowMiddleHeight);
        device.sync();
        for (int i = 0; i + windowMiddleWidth < width ; i += 5) {
            device.setPoint(p, windowMiddleWidth + i, windowMiddleHeight);
            device.sync();
        }

        //wait for results
        TestLogShim.waitForLogContaining("TouchPoint: PRESSED", 3000);

        //release outside the window
        device.removePoint(p);
        device.sync();
        //check that we get the event
        TestLogShim.waitForLogContaining("TouchPoint: RELEASED", 3000);
    }

    /**
     * Combining the two test cases above, start a touch sequence inside a
     * window, drag the 'finger' out and in again and see that we gat the
     * events.
     *
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void singleTouch_dragPointInandOutAwindow(TestTouchDevice device) throws Exception {
        setUpScreen(device);
        Stage stage = TestApplication.getStage();
        int windowMiddleWidth = (int)(stage.getX() + stage.getWidth() / 2);
        int windowMiddleHeight = (int)(stage.getY() + (stage.getHeight() / 2));
        int windowRightEnd = (int)(stage.getX() + stage.getWidth());
        int i;

        //start inside the window and drag point outside
        int p = device.addPoint(windowMiddleWidth, windowMiddleHeight);
        device.sync();
        for (i = windowMiddleWidth; i <= windowRightEnd + 100 ; i += 10) {
            device.setPoint(p, i, windowMiddleHeight);
            device.sync();
        }

        //wait for results
        TestLogShim.waitForLogContaining("TouchPoint: PRESSED", 3000);
        TestLogShim.waitForLogContaining("TouchPoint: MOVED", 3000);

        //continue from where we stopped and drag point back to window
        for (; i >= windowMiddleWidth  ; i -= 10) {
            device.setPoint(p, i, windowMiddleHeight);
            device.sync();
        }

        //release inside the window
        device.removePoint(p);
        device.sync();
        //check that we get the event
        TestLogShim.waitForLogContaining("TouchPoint: RELEASED", 3000);
    }

    /**
     * Same test as above, but for multi touch.
     * Test should pass in either single touch mode or multi touch mode
     * Main point is to see that no exception is been thrown
     *
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void multiTouch_dragPointInandOutAwindow(TestTouchDevice device) throws Exception {
        Assumptions.assumeTrue(device.getPointCount() >= 2);
        setUpScreen(device);
        Stage stage = TestApplication.getStage();
        int windowMiddleWidth = (int)(stage.getX() + stage.getWidth() / 2);
        int windowMiddleHeight = (int)(stage.getY() + (stage.getHeight() / 2));
        int windowRightEnd = (int)(stage.getX() + stage.getWidth());
        int i;
        int p1 = device.addPoint(windowRightEnd + 15, windowMiddleHeight);
        int p2 = device.addPoint(windowRightEnd + 15, windowMiddleHeight + 10);
        device.sync();
        //start outside the window and drag point into the center of window
        for (i = windowRightEnd + 12; i >= windowMiddleWidth ; i -= 3) {
            //first finger
            device.setPoint(p1, i, windowMiddleHeight);
            //second finger
            device.setPoint(p2, i, windowMiddleHeight + 10);
            device.sync();
        }

        //continue from where we stopped and drag point outside the window to
        //the end of screen
        for (; i + windowMiddleWidth < width ; i += 5) {
            //first finger
            device.setPoint(p1, i, windowMiddleHeight);
            //second finger
            device.setPoint(p2, i, windowMiddleHeight + 10);
            device.sync();
        }

        //release all points outside the window
        device.removePoint(p1);
        device.removePoint(p2);
        device.sync();

        //wait for results and make sure no event received
        Assertions.assertEquals(0, TestLogShim.countLogContaining("TouchPoint: PRESSED"));
        Assertions.assertEquals(0, TestLogShim.countLogContaining("TouchPoint: MOVED"));
        Assertions.assertEquals(0, TestLogShim.countLogContaining("TouchPoint: RELEASED"));
    }

    /**
     * Drag two touch points simultaneously from outside the window (from the
     * right side) to the window's center.
     * No "move", "press" or "release" events should be sent.
     */
    @Disabled("RT-38482")
    @ParameterizedTest
    @MethodSource("parameters")
    public void multiTouch_dragTwoPointsIntoTheWindow(TestTouchDevice device) throws Exception {
        Assumptions.assumeTrue(device.getPointCount() >= 2);
        setUpScreen(device);
        Stage stage = TestApplication.getStage();
        double[] bounds = {0.0, 0.0, 0.0, 0.0};
        TestRunnable.invokeAndWait(() -> {
            bounds[0] = stage.getX();
            bounds[1] = stage.getY();
            bounds[2] = stage.getWidth();
            bounds[3] = stage.getHeight();
        });
        Rectangle2D stageBounds = new Rectangle2D(bounds[0], bounds[1],
                                                  bounds[2], bounds[3]);
        int windowX = (int) (stageBounds.getMinX());
        int windowY = (int) (stageBounds.getMinY());
        int windowMiddleX = (int) (stageBounds.getMinX() + stageBounds.getWidth() / 2);
        int windowMiddleY = (int) (stageBounds.getMinY() + stageBounds.getHeight() / 2);
        int windowRightEnd = (int) (stageBounds.getMaxX());
        //distance between tap points
        int distance = device.getTapRadius() + 2;
        int x1 = windowRightEnd + distance;
        int y1 = windowMiddleY;
        int x2 = windowRightEnd + distance * 2;
        int y2 = y1;
        Assertions.assertTrue(x1 < width && x2 < width);
        //press two fingers
        int p1 = device.addPoint(x1, y1);
        int p2 = device.addPoint(x2, y2);
        device.sync();

        //drag the fingers into the center of window
        for (int i = x1 - 3; i >= windowMiddleX; i -= 3) {
            device.setPoint(p1, i, windowMiddleY);
            device.setPoint(p2, i + distance, windowMiddleY);
            device.sync();
        }

        //release all points
        device.removePoint(p1);
        device.removePoint(p2);
        device.sync();

        //tap in the window in order to verify all events were received
        int x3 = windowX;
        int y3 = windowY;
        int p = device.addPoint(x3, y3);
        device.sync();
        device.removePoint(p);
        device.sync();
        //verify events press/release were received
        TestLogShim.waitForLogContaining("TouchPoint: PRESSED %d, %d", x3, y3);
        TestLogShim.waitForLogContaining("TouchPoint: RELEASED %d, %d", x3, y3);

        //Verify press/release events were received only once
        Assertions.assertEquals(1, TestLogShim.countLogContaining("TouchPoint: PRESSED"));
        Assertions.assertEquals(1, TestLogShim.countLogContaining("TouchPoint: RELEASED"));

        //make sure no move event was received
        Assertions.assertEquals(0, TestLogShim.countLogContaining("TouchPoint: MOVED"));
    }
}
