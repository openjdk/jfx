/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.robot.impl;

import static javafx.scene.input.KeyCode.ALT;
import static javafx.scene.input.KeyCode.CONTROL;
import static javafx.scene.input.KeyCode.META;
import static javafx.scene.input.KeyCode.SHIFT;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import com.sun.javafx.robot.FXRobot;
import com.sun.javafx.robot.FXRobotImage;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.input.ScrollEvent;

/**
 * An implementation of FXRobot which injects the events into the FX event queue
 *
 */
public class BaseFXRobot extends FXRobot {
    static {
        // force initialization of the input accessors
        String stuff = KeyEvent.CHAR_UNDEFINED;
    }

    private static final boolean debugOut = computeDebugOut();

    private static boolean computeDebugOut() {
        boolean debug = false;
        try {
            debug = "true".equals(System.getProperty("fxrobot.verbose", "false"));
        } catch (Throwable th) {}
        return debug;
    };

    private static void out(String s) {
        if (debugOut) {
            System.out.println("[FXRobot] " + s);
        }
    }

    private static Map<KeyCode,String> keyTextMap;
    private static String getKeyText(KeyCode keyCode) {
        return keyCode.getName();
    }

    private Scene target;

    public BaseFXRobot(Scene target) {
        this.target = target;
    }

    private boolean isShiftDown = false;
    private boolean isControlDown = false;
    private boolean isAltDown = false;
    private boolean isMetaDown = false;

    private boolean isButton1Pressed = false;
    private boolean isButton2Pressed = false;
    private boolean isButton3Pressed = false;

    private MouseButton lastButtonPressed = null;

    private double sceneMouseX;
    private double sceneMouseY;
    private double screenMouseX;
    private double screenMouseY;

    // TODO: need to devise a cross implementation way fo doing this
//    public function waitForFirstRepaint(timeout: Integer) : Void {
//        def latch : CountDownLatch = new CountDownLatch(1);
//        FX.deferAction(function(): Void {
//            var tracker = com.sun.javafx.perf.PerformanceTracker.getSceneTracker(target);
//            var f = tracker.onFirstPulse;
//            tracker.onFirstPulse = function():Void {
//                latch.countDown();
//                if (f != null) {
//                    tracker.onFirstPulse = f;
//                }
//            }
//        });
//        while (true) {
//            try {
//                latch.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
//                break;
//            } catch (e: java.lang.InterruptedException) {}
//        }
//    }

    @Override public void waitForIdle() {
        // TODO: use better scheme for waiting when pulses stop
        final CountDownLatch latch = new CountDownLatch(1);
        Toolkit.getToolkit().defer(new Runnable() {
            public void run() {
                latch.countDown();
            }
        });
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException e) {}
        }
    }

    @Override public void keyPress(KeyCode keyCode) {
        doKeyEvent(KeyEvent.KEY_PRESSED, keyCode, "");
    }

    @Override public void keyRelease(KeyCode keyCode) {
        doKeyEvent(KeyEvent.KEY_RELEASED, keyCode, "");
    }

    @Override public void keyType(KeyCode keyCode, String keyChar) {
        doKeyEvent(KeyEvent.KEY_TYPED, keyCode, keyChar);
    }

    @Override public void mouseMove(int x, int y) {
        doMouseEvent(x, y, lastButtonPressed, 0, MouseEvent.MOUSE_MOVED);
    }
    @Override public void mousePress(MouseButton button, int clickCount) {
        doMouseEvent(sceneMouseX, sceneMouseY, button, clickCount, MouseEvent.MOUSE_PRESSED);
    }
    @Override public void mouseRelease(MouseButton button, int clickCount) {
        doMouseEvent(sceneMouseX, sceneMouseY, button, clickCount, MouseEvent.MOUSE_RELEASED);
    }
    @Override public void mouseClick(MouseButton button, int clickCount) {
        doMouseEvent(sceneMouseX, sceneMouseY, button, clickCount,
                     MouseEvent.MOUSE_CLICKED);
    }
    @Override public void mouseDrag(MouseButton button) {
        doMouseEvent(sceneMouseX, sceneMouseY, button, 0, MouseEvent.MOUSE_DRAGGED);
    }
    @Override public void mouseWheel(int wheelAmt) {
        doScrollEvent(sceneMouseX, sceneMouseY, wheelAmt, ScrollEvent.SCROLL);
    }

    @Override public int getPixelColor(int x, int y) {
        FXRobotImage image = getSceneCapture(0, 0, 100, 100);
        if (image != null) {
            return image.getArgb(x, y);
        }
        return 0;
    }

    private Object lastImage;
    private FXRobotImage lastConvertedImage;
    @Override public FXRobotImage getSceneCapture(int x, int y, int w, int h) {
        Object ret = FXRobotHelper.sceneAccessor.renderToImage(target, lastImage);
        if (ret != null) {
            lastImage = ret;
            lastConvertedImage =
                FXRobotHelper.imageConvertor.convertToFXRobotImage(ret);
        }
        return lastConvertedImage;
    }

    private void doKeyEvent(EventType<KeyEvent> eventType, KeyCode keyCode, String character) {
        boolean pressed = eventType == KeyEvent.KEY_PRESSED;
        boolean typed = eventType == KeyEvent.KEY_TYPED;
        if (keyCode == SHIFT) {
            isShiftDown = pressed;
        }
        if (keyCode == CONTROL) {
            isControlDown = pressed;
        }
        if (keyCode == ALT) {
            isAltDown = pressed;
        }
        if (keyCode == META) {
            isMetaDown = pressed;
        }

        String keyText = typed ? "" : getKeyText(keyCode);
        String keyChar = typed ? character : KeyEvent.CHAR_UNDEFINED;

        final KeyEvent e = FXRobotHelper.inputAccessor.
            createKeyEvent(eventType, keyCode, keyChar, keyText,
                           isShiftDown, isControlDown, isAltDown, isMetaDown);

        Toolkit.getToolkit().defer(new Runnable() {
            public void run() {
                out("doKeyEvent: injecting: {e}");
                FXRobotHelper.sceneAccessor.processKeyEvent(target, e);
            }
        });
        if (autoWait) {
            waitForIdle();
        }
    }

    private void doMouseEvent(double x, double y, MouseButton passedButton,
            int clickCount, EventType<MouseEvent> passedType)
    {
        screenMouseX = target.getWindow().getX() + target.getX() + x;
        screenMouseY = target.getWindow().getY() + target.getY() + y;
        sceneMouseX = x;
        sceneMouseY = y;

        MouseButton button = passedButton;
        EventType<MouseEvent> type = passedType;
        if (type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_RELEASED) {
            boolean pressed = type == MouseEvent.MOUSE_PRESSED;
            if (button == MouseButton.PRIMARY) {
                isButton1Pressed = pressed;
            } else if (button == MouseButton.MIDDLE) {
                isButton2Pressed = pressed;
            } else if (button == MouseButton.SECONDARY) {
                isButton3Pressed = pressed;
            }
            if (pressed) {
                lastButtonPressed = button;
            } else {
                if (!(isButton1Pressed || isButton2Pressed || isButton3Pressed)) {
                    lastButtonPressed = MouseButton.NONE;
                }
            }
        } else if (type == MouseEvent.MOUSE_MOVED) {
            boolean someButtonPressed = isButton1Pressed || isButton2Pressed || isButton3Pressed;
            if (someButtonPressed) {
                type = MouseEvent.MOUSE_DRAGGED;
                button = MouseButton.NONE;
            }
        }

        final MouseEvent e = FXRobotHelper.inputAccessor.
            createMouseEvent(type, (int)sceneMouseX, (int)sceneMouseY,
                             (int)screenMouseX, (int)screenMouseY,
                             button, clickCount,
                             isShiftDown,
                             isControlDown,
                             isAltDown,
                             isMetaDown,
                             button == MouseButton.SECONDARY,
                             isButton1Pressed,
                             isButton2Pressed,
                             isButton3Pressed);
        Toolkit.getToolkit().defer(new Runnable() {
            public void run() {
                out("doMouseEvent: injecting: " + e);
                FXRobotHelper.sceneAccessor.processMouseEvent(target, e);
            }
        });
        if (autoWait) {
            waitForIdle();
        }
    }

    private void doScrollEvent(double x, double y, double rotation,
            EventType<ScrollEvent> type)
    {
        screenMouseX = target.getWindow().getX() + target.getX() + x;
        screenMouseY = target.getWindow().getY() + target.getY() + y;
        sceneMouseX = x;
        sceneMouseY = y;

        final ScrollEvent e = FXRobotHelper.inputAccessor.
            createScrollEvent(type, 0, (int)rotation * 40,
                             ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                             ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                             (int)sceneMouseX, (int)sceneMouseY,
                             (int)screenMouseX, (int)screenMouseY,
                             isShiftDown,
                             isControlDown,
                             isAltDown,
                             isMetaDown);
        Toolkit.getToolkit().defer(new Runnable() {
            public void run() {
                out("doScrollEvent: injecting: " + e);
                FXRobotHelper.sceneAccessor.processScrollEvent(target, e);
            }
        });
        if (autoWait) {
            waitForIdle();
        }
    }
}
