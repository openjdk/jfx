/*
 * Copyright (c) 2012, 2013, Oracle  and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.swt;

import com.sun.glass.ui.Robot;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

final class SWTRobot extends Robot {

    @Override protected void _create() {
    }

    @Override protected void _destroy() {
    }

    @Override protected void _keyPress(int code) {
        Event event = new Event();
        event.type = SWT.KeyDown;
        event.character = (char) SWTApplication.getSWTKeyCode(code);
        Display.getDefault().post(event);
    }
    
    @Override protected void _keyRelease(int code) {
        Event event = new Event();
        event.type = SWT.KeyUp;
        event.character = (char) SWTApplication.getSWTKeyCode(code);
        Display.getDefault().post(event);
    }

    @Override protected void _mouseMove(int x, int y) {
        Event event = new Event();
        event.type = SWT.MouseMove;
        event.x = x;
        event.y = y;
        Display.getDefault().post(event);
    }

    @Override protected void _mousePress(int buttons) {
        Event event = new Event();
        event.type = SWT.MouseDown;
        if (buttons == Robot.MOUSE_LEFT_BTN) event.button = 1;
        if (buttons == Robot.MOUSE_MIDDLE_BTN) event.button = 2;
        if (buttons == Robot.MOUSE_RIGHT_BTN) event.button = 3;
        Display.getDefault().post(event);
    }

    @Override protected void _mouseRelease(int buttons) {
        Event event = new Event();
        event.type = SWT.MouseUp;
        if (buttons == Robot.MOUSE_LEFT_BTN) event.button = 1;
        if (buttons == Robot.MOUSE_MIDDLE_BTN) event.button = 2;
        if (buttons == Robot.MOUSE_RIGHT_BTN) event.button = 3;
        Display.getDefault().post(event);
    }

    @Override protected void _mouseWheel(int wheelAmt) {
        Event event = new Event();
        event.type = SWT.MouseVerticalWheel;
        //TODO - not tested, determine correct value for robot wheel
        event.count = wheelAmt;
        Display.getDefault().post(event);
    }

    @Override protected int _getMouseX() {
        //TODO - write native version that avoids thread check
        return Display.getDefault().getCursorLocation().x;
    }

    @Override protected int _getMouseY() {
        //TODO - write native version that avoids thread check
        return Display.getDefault().getCursorLocation().y;
    }

    @Override protected int _getPixelColor(int x, int y) {
        Display display = Display.getDefault();
        GC gc = new GC(display);
        final Image image = new Image(display, 1, 1);
        gc.copyArea(image, x, y);
        gc.dispose();
        ImageData imageData = image.getImageData();
        return imageData.getPixel(x, y);
    }
    
    @Override protected void _getScreenCapture(int x, int y, int width, int height, int[] data) {
//        Display display = Display.getDefault();
//        GC gc = new GC(display);
//        final Image image = new Image(display, display.getBounds());
//        gc.copyArea(image, 0, 0);
//        gc.dispose();
//        ImageData imageData = image.getImageData();
        //TODO - put bits into data
    }
}

