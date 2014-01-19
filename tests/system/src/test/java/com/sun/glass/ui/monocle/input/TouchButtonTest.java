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

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.css.Styleable;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class TouchButtonTest {

    private UInput ui;
    private Node button1;
    private Node button2;
    private Node button3;
    private final Object lock = new Object();
    private final int tapRadius = Integer.getInteger("lens.input.touch.TapRadius", 20);

    @Rule public TestName name = new TestName();

    @Before public void setUpScreen() throws Exception {
        TestLog.log(name.getMethodName());
        TestApplication.showFullScreenScene();
        createButtons();
        initDevice();
        TestLog.reset();
    }

    public Node createButton(String text, int x, int y, boolean setListeners) {
        final Node button = new Rectangle(100.0, 20.0);
        button.setId(text);
        button.setLayoutX(x);
        button.setLayoutY(y);
//        button.setFocusTraversable(true);
        button.setOnMousePressed((e) -> {
            button.requestFocus();
        });
        if (setListeners) {
            button.addEventHandler(MouseEvent.ANY, (e) -> {
                TestLog.log(e.getEventType().getName() +": " 
                                                     + (int) e.getScreenX()
                                                     + ", " + (int) e.getScreenY());
                });

            button.focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
                      ReadOnlyBooleanProperty prop = (ReadOnlyBooleanProperty)observableValue;
                      TestLog.log(button.getId() + " isFocused=" + prop.get());
                }
            });
        }

        return button;
    }

    public void createButtons() throws Exception {
        TestRunnable.invokeAndWait(() -> {
            int screenWidth =  (int) Screen.getPrimary().getVisualBounds().getWidth();
            int screenHeight = (int) Screen.getPrimary().getVisualBounds().getHeight();
            int X = screenWidth / 2;
            int Y = screenHeight / 2;

            button1 = createButton("button1", X, Y - 100, true);
            button2 = createButton("button2", X, Y + 100, true);
            button3 = createButton("button3", 0, 0, false);

            TestApplication.getRootGroup().getChildren().clear();
            TestApplication.getRootGroup().getChildren().addAll(button1, button2, button3);
            button3.requestFocus();
        });
        TestApplication.waitForNextPulse();
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

    /**
     * Tests
     */

    @Test
    public void tapOnButton() throws Exception {
        Point2D clickAt = tapInsideButton(button1);
        waitForFocusGainOn(button1);
        waitForMouseEnteredAt(clickAt);
        waitForMouseClickAt(clickAt);
    }

    @Test
    public void tapOn2Buttons() throws Exception {
        Point2D clickAt = tapInsideButton(button1);
        waitForFocusGainOn(button1);
        waitForMouseEnteredAt(clickAt);
        waitForMouseClickAt(clickAt);

        clickAt = tapInsideButton(button2);
        waitForFocusLostOn(button1);
        waitForFocusGainOn(button2);
        waitForMouseEnteredAt(clickAt);
        waitForMouseClickAt(clickAt);
    }

    @Test
    public void tapOutAndInButton() throws Exception {
        tapOutSideButton();
        TestLog.reset();
        Point2D clickAt = tapInsideButton(button1);
        waitForMouseClickAt(clickAt);
        waitForFocusGainOn(button1);
    }

    @Test
    public void tapOutInAndOutButton() throws Exception {
        tapOutSideButton();
        TestLog.reset();
        Point2D clickAt = tapInsideButton(button1);
        waitForMouseClickAt(clickAt);
        waitForFocusGainOn(button1);

        tapOutSideButton();
        tapInsideButton(button3);
        waitForFocusLostOn(button1);
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
            waitForFocusGainOn(button1);
            waitForMouseEnteredAt(clickAt);
            waitForMouseClickAt(clickAt);

            tapOutSideButton();
            tapInsideButton(button3);
            waitForFocusLostOn(button1);
            TestLog.reset();

            clickAt = tapInsideButton(button2);
            waitForFocusGainOn(button2);
            waitForMouseEnteredAt(clickAt);
            waitForMouseClickAt(clickAt);

            tapOutSideButton();
            tapInsideButton(button3);
            waitForFocusLostOn(button2);
            TestLog.reset();
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
        ui.processLine("EV_ABS ABS_MT_POSITION_X " + x);
        ui.processLine("EV_ABS ABS_MT_POSITION_Y " + y);
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");

        waitForFocusGainOn(button2);

        //drag inside button         
        for (; x > buttonBounds.getMinX(); x-- ) {
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + x);
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + y);
            ui.processLine("EV_SYN SYN_MT_REPORT 0");
            ui.processLine("EV_SYN SYN_REPORT 0");
        }

        //release inside the button
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLogContaining("MOUSE_CLICKED:", 3000l);
    }

    /**
     * RT-34625 - Currently a control will not generate a click when tapping on 
     * it, drag the finger outside the control and release the finger. 
     * This might be a desired behavior, but sometime there are small 
     * unintentional drags that resulting in a finger release outside the control. 
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
        ui.processLine("EV_ABS ABS_MT_POSITION_X " + x);
        ui.processLine("EV_ABS ABS_MT_POSITION_Y " + y);
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");

        waitForFocusGainOn(button2);

        //drag outside button         
        for (; x > buttonBounds.getMinX() - tapRadius - 10; x-- ) {
            ui.processLine("EV_ABS ABS_MT_POSITION_X " + x);
            ui.processLine("EV_ABS ABS_MT_POSITION_Y " + y);
            ui.processLine("EV_SYN SYN_MT_REPORT 0");
            ui.processLine("EV_SYN SYN_REPORT 0");
        }

        //release outside the button
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        TestLog.waitForLogContaining("MOUSE_CLICKED:", 3000l);
    }

    @Test
    public void tapping_oneButtonOnScreen () throws Exception {
        Node button4 = createButton("button", 0, 0, true);
        button4.layoutYProperty().addListener((ov, t, t1) -> {
            synchronized (lock) {
                TestLog.log("Layout complete");
                lock.notifyAll();
            }
        });

        TestRunnable.invokeAndWait(() -> {
            TestApplication.getRootGroup().getChildren().clear();
            TestApplication.getRootGroup().getChildren().addAll(button4);
        });

        TestApplication.waitForNextPulse();

        for (int i = 0; i < 5; i++) {
            Point2D clickAt = tapInsideButton(button4);
            waitForMouseClickAt(clickAt);
            TestLog.reset();
        }
    }
    
    /** utilities */
    public Bounds getButtonBounds(Node button) {
        return button.localToScreen(new BoundingBox(0, 0, 
            button.getBoundsInParent().getWidth(),
            button.getBoundsInParent().getHeight()));
    }

    public Point2D getCenterOfButton(Node button) {
        Bounds buttonBounds = getButtonBounds(button);
        Point2D clickAt = new Point2D(
                buttonBounds.getMinX()+ buttonBounds.getWidth() / 2,
                buttonBounds.getMinY()+ buttonBounds.getHeight() / 2);
        return clickAt;
    }

    public Point2D tapInsideButton(Node button) {
        Point2D clickAt = getCenterOfButton(button);
        //tap
        ui.processLine("EV_ABS ABS_MT_POSITION_X " + ((int)(clickAt.getX())));
        ui.processLine("EV_ABS ABS_MT_POSITION_Y " + ((int)(clickAt.getY())));
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        //release
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");

        return clickAt;
    }

    public void tapOutSideButton() {
        Bounds buttonBounds = getButtonBounds(button3);
        ///tap
        ui.processLine("EV_ABS ABS_MT_POSITION_X " + ((int)(buttonBounds.getMaxX() + tapRadius + 10)));
        ui.processLine("EV_ABS ABS_MT_POSITION_Y " + ((int)(buttonBounds.getMaxY() + tapRadius + 10)));
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
        //release
        ui.processLine("EV_SYN SYN_MT_REPORT 0");
        ui.processLine("EV_SYN SYN_REPORT 0");
    }

    public void waitForMouseClickAt(Point2D clickAt) throws Exception{
        TestLog.waitForLog("MOUSE_CLICKED: "
                           + (int)clickAt.getX() + ", "
                           + (int)clickAt.getY(), 3000l);
    }

    public void waitForMouseEnteredAt(Point2D clickAt) throws Exception{
        TestLog.waitForLog("MOUSE_ENTERED: "
                           + (int)clickAt.getX() + ", "
                           + (int)clickAt.getY(), 3000l);
    }

    public void waitForFocus(Styleable button, boolean focusState) throws Exception{
        TestLog.waitForLog(button.getId() + " isFocused=" + focusState, 3000l);
    }

    public void waitForFocusGainOn(Node button) throws Exception{
        waitForFocus(button, true);
    }

    public void waitForFocusLostOn(Node button) throws Exception{
        waitForFocus(button, false);
    }

}
