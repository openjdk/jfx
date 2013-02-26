/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import java.nio.IntBuffer;

import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;

public abstract class Robot {

    private final static Permission allPermission = new AllPermission();

    final static public int MOUSE_LEFT_BTN   = 1;
    final static public int MOUSE_RIGHT_BTN  = 2;
    final static public int MOUSE_MIDDLE_BTN = 4;

    protected abstract void _create();
    protected Robot() {
        // Fix for RT-22633
        // Ideally, we should restrict user access to Glass packages at all,
        // but that's not the case right now. To prevent applications from
        // using Robot, we check for AllPermission here as we don't have
        // (and will unlikely have in the future) specific permission for
        // this functionality
        if (System.getSecurityManager() != null) {
            AccessController.checkPermission(allPermission);
        }
        Application.checkEventThread();
        _create();
    }

    protected abstract void _destroy();
    public void destroy() {
        Application.checkEventThread();
        _destroy();
    }

    protected abstract void _keyPress(int code);
    /**
     * Generate a key pressed event.
     * @param code key code for this event
     */
    public void keyPress(int code) {
        Application.checkEventThread();
        _keyPress(code);
    }

    protected abstract void _keyRelease(int code);
    /**
     * Generate a key released event.
     *
     * @param code key code for this event
     */
    public void keyRelease(int code) {
        Application.checkEventThread();
        _keyRelease(code);
    }

    protected abstract void _mouseMove(int x, int y);
    /**
     * Generate a mouse moved event.
     *
     * @param x screen coordinate x
     * @param y screen coordinate y
     */
    public void mouseMove(int x, int y) {
        Application.checkEventThread();
        _mouseMove(x, y);
    }

    protected abstract void _mousePress(int buttons);
    /**
     * Generate a mouse press event with specified buttons mask.
     *
     * Up to 32-buttons mice are supported. Other buttons are inaccessible
     * by the robot. Bits 0, 1, and 2 mean LEFT, RIGHT, and MIDDLE mouse buttons
     * respectively.
     *
     * @param buttons buttons to have generated the event
     */
    public void mousePress(int buttons) {
        Application.checkEventThread();
        _mousePress(buttons);
    }

    protected abstract void _mouseRelease(int buttons);
    /**
     * Generate a mouse release event with specified buttons mask.
     *
     * @param buttons buttons to have generated the event
     */
    public void mouseRelease(int buttons) {
        Application.checkEventThread();
        _mouseRelease(buttons);
    }

    protected abstract void _mouseWheel(int wheelAmt);
    /**
     * Generate a mouse wheel event.
     *
     * @param wheelAmt amount the wheel has turned of wheel turning
     */
    public void mouseWheel(int wheelAmt) {
        Application.checkEventThread();
        _mouseWheel(wheelAmt);
    }

    protected abstract int _getMouseX();
    public int getMouseX() {
        Application.checkEventThread();
        return _getMouseX();
    }

    protected abstract int _getMouseY();
    public int getMouseY() {
        Application.checkEventThread();
        return _getMouseY();
    }

    protected abstract int _getPixelColor(int x, int y);
    /**
     * Returns pixel color at specified screen coordinates in IntARGB format.
     */
    public int getPixelColor(int x, int y) {
        Application.checkEventThread();
        return _getPixelColor(x, y);
    }

    protected abstract void _getScreenCapture(int x, int y, int width, int height, int[] data);
    /**
     * Returns a capture of the specified area of the screen.
     */
    public Pixels getScreenCapture(int x, int y, int width, int height) {
        Application.checkEventThread();
        int data[] = new int[width * height];

        _getScreenCapture(x, y, width, height, data);

        return Application.GetApplication().createPixels(width, height, IntBuffer.wrap(data));
    }
}
