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

import com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import com.sun.glass.ui.monocle.input.devices.TestTouchDevices;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class TouchButtonTest extends ParameterizedTestBase {

    private Node button1;
    private Node button2;
    private Node button3;

    public TouchButtonTest(TestTouchDevice device) {
        super(device);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    public Node createButton(String text, int x, int y, boolean setListeners) {
        final Node button = new Rectangle(100.0, 20.0);
        button.setId(text);
        button.setLayoutX(x);
        button.setLayoutY(y);
        button.setOnMousePressed((e) -> button.requestFocus());
        if (setListeners) {
            button.addEventHandler(MouseEvent.ANY, e ->
                TestLog.log(e.getEventType().getName() +": " 
                                                     + (int) e.getScreenX()
                                                     + ", " + (int) e.getScreenY()));
            button.focusedProperty().addListener((observable, oldValue, newValue) ->
                    TestLog.log(button.getId() + " isFocused=" + newValue));
        }

        return button;
    }

    @Before
    public void createButtons() throws Exception {
        TestRunnable.invokeAndWait(() -> {
            int X = (int) width / 2;
            int Y = (int) height / 2;

            button1 = createButton("button1", X, Y - 100, true);
            button2 = createButton("button2", X, Y + 100, true);
            button3 = createButton("button3", 0, 0, false);

            TestApplication.getRootGroup().getChildren().clear();
            TestApplication.getRootGroup().getChildren().addAll(
                    button1, button2, button3);
            button3.requestFocus();
        });

        TestApplication.waitForLayout();
    }

    /**
     * Tests
     */

    @Test
    public void tapOnButton() throws Exception {
        Point2D clickAt = tapInsideButton(button1);
        waitForFocusGainOn("button1");
        waitForMouseEnteredAt(clickAt);
        waitForMouseClickAt(clickAt);
    }

    @Test
    public void tapOn2Buttons() throws Exception {
        Point2D clickAt = tapInsideButton(button1);
        waitForFocusGainOn("button1");
        waitForMouseEnteredAt(clickAt);
        waitForMouseClickAt(clickAt);

        clickAt = tapInsideButton(button2);
        waitForFocusLostOn("button1");
        waitForFocusGainOn("button2");
        waitForMouseEnteredAt(clickAt);
        waitForMouseClickAt(clickAt);
    }

    @Test
    public void tapOutAndInButton() throws Exception {
        tapOutSideButton();
        TestLog.reset();
        Point2D clickAt = tapInsideButton(button1);
        waitForMouseClickAt(clickAt);
        waitForFocusGainOn("button1");
    }

    @Test
    public void tapOutInAndOutButton() throws Exception {
        tapOutSideButton();
        TestLog.reset();
        Point2D clickAt = tapInsideButton(button1);
        waitForMouseClickAt(clickAt);
        waitForFocusGainOn("button1");

        tapOutSideButton();
        tapInsideButton(button3);
        waitForFocusLostOn("button1");
    }

    @Test
    public void tapInAndOutLoop() throws Exception {
        tapOutSideButton();
        TestLog.reset();
        for (int i = 0 ; i < 2 ; i++) {
            tapOutSideButton();
            tapInsideButton(button3);
            TestLog.reset();
            Point2D clickAt = tapInsideButton(button1);
            waitForFocusGainOn("button1");
            waitForMouseEnteredAt(clickAt);
            waitForMouseClickAt(clickAt);

            tapOutSideButton();
            tapInsideButton(button3);
            waitForFocusLostOn("button1");
            TestLog.reset();

            clickAt = tapInsideButton(button2);
            waitForFocusGainOn("button2");
            waitForMouseEnteredAt(clickAt);

            waitForMouseClickAt(clickAt);
            TestLog.reset();
            tapOutSideButton();
            tapInsideButton(button3);
            waitForFocusLostOn("button2");
        }
    }

    /**
     * RT-34625 - we should get a click when tapping on a control, dragging the 
     * finger and release the finger inside the control 
     */
    @Test
    public void tapAndDrag() throws Exception {
        Bounds buttonBounds = getButtonBounds(button2);

        //start at right most x and center y
        int x = (int) buttonBounds.getMaxX() - 1;
        int y = (int) (buttonBounds.getMinY() + buttonBounds.getMaxY()) / 2;

        ///tap
        int p = device.addPoint(x, y);
        device.sync();

        waitForFocusGainOn("button2");

        //drag inside button         
        for (; x > buttonBounds.getMinX(); x-- ) {
            device.setPoint(p, x, y);
            device.sync();
        }

        //release inside the button
        device.removePoint(p);
        device.sync();
        TestLog.waitForLogContaining("MOUSE_CLICKED:", 3000l);
        TestLog.waitForLogContaining("MOUSE_RELEASED:", 3000l);
    }

    /**
     * RT-34625 - Currently a control will not generate a click when tapping on 
     * it, drag the finger outside the control and release the finger. 
     * This might be a desired behavior, but sometime there are small 
     * unintentional drags that resulting in a finger release outside the
     * control.
     *  
     * This test should fail and throw RuntimeException
     * 
     */
    @Ignore("RT-34625")
    @Test
    public void tapAndDrag_fail() throws Exception {
        Bounds buttonBounds = getButtonBounds(button2);

        //start at right most x and center y
        int x = (int) buttonBounds.getMaxX() - 1;
        int y = (int) (buttonBounds.getMinY() + buttonBounds.getMaxY()) / 2;

        ///tap
        int p = device.addPoint(x, y);
        device.sync();

        waitForFocusGainOn("button2");

        //drag outside button         
        for (; x > buttonBounds.getMinX() - device.getTapRadius() - 10; x-- ) {
            device.setPoint(p, x, y);
            device.sync();
        }

        //release outside the button
        device.removePoint(p);
        device.sync();
        TestLog.waitForLogContaining("MOUSE_CLICKED:", 3000l);
    }

    @Test
    public void tapping_oneButtonOnScreen () throws Exception {
        AtomicReference<Node> buttonRef = new AtomicReference<>();
        TestRunnable.invokeAndWait(() -> {
            Node button4 = createButton("button4", 0, 0, true);
            buttonRef.set(button4);
            TestApplication.getRootGroup().getChildren().clear();
            TestApplication.getRootGroup().getChildren().addAll(button4);
        });
        TestApplication.waitForLayout();

        for (int i = 0; i < 5; i++) {
            Point2D clickAt = tapInsideButton(buttonRef.get());
            waitForMouseClickAt(clickAt);
            TestLog.reset();
        }
    }
    
    /** utilities */
    public Bounds getButtonBounds(Node button) throws Exception {
        AtomicReference<Bounds> ref = new AtomicReference<>();
        TestRunnable.invokeAndWait(() -> {
            ref.set(button.localToScreen(
                    new BoundingBox(0, 0,
                                    button.getBoundsInParent().getWidth(),
                                    button.getBoundsInParent().getHeight())));
            TestLog.log("Bounds for " + button.getId() + " are " + ref.get());
        });
        return ref.get();
    }

    public Point2D getCenterOfButton(Node button) throws Exception {
        Bounds buttonBounds = getButtonBounds(button);
        Point2D clickAt = new Point2D(
                buttonBounds.getMinX()+ buttonBounds.getWidth() / 2,
                buttonBounds.getMinY()+ buttonBounds.getHeight() / 2);
        return clickAt;
    }

    public Point2D tapInsideButton(Node button) throws Exception {
        Point2D clickAt = getCenterOfButton(button);
        //tap
        int p = device.addPoint(clickAt.getX(), clickAt.getY());
        device.sync();
        //release
        device.removePoint(p);
        device.sync();
        TestLog.waitForLog("Mouse clicked: %.0f, %.0f", clickAt.getX(), clickAt.getY());
        return clickAt;
    }

    public void tapOutSideButton() throws Exception {
        Bounds buttonBounds = getButtonBounds(button3);
        ///tap
        double x = buttonBounds.getMaxX() + device.getTapRadius() + 10;
        double y = buttonBounds.getMaxY() + device.getTapRadius() + 10;
        int p = device.addPoint(x, y);
        device.sync();
        //release
        device.removePoint(p);
        device.sync();
        TestLog.waitForLog("Mouse clicked: %.0f, %.0f", x, y);
    }

    public void waitForMouseClickAt(Point2D clickAt) throws Exception{
        TestLog.waitForLog("MOUSE_CLICKED: %d, %d",
                           Math.round(clickAt.getX()),
                           Math.round(clickAt.getY()));
    }

    public void waitForMouseEnteredAt(Point2D clickAt) throws Exception{
        TestLog.waitForLog("MOUSE_ENTERED: %d, %d",
                           Math.round(clickAt.getX()),
                           Math.round(clickAt.getY()));
    }

    public void waitForFocus(String id, boolean focusState) throws Exception {
        TestLog.waitForLog("%s isFocused=%b", id, focusState);
    }

    public void waitForFocusGainOn(String id) throws Exception{
        waitForFocus(id, true);
    }

    public void waitForFocusLostOn(String id) throws Exception{
        waitForFocus(id, false);
    }

}
