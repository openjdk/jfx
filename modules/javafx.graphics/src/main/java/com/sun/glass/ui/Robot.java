/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.javafx.FXPermissions.CREATE_ROBOT_PERMISSION;
import java.lang.annotation.Native;
import java.nio.IntBuffer;

public abstract class Robot {

    @Native public static final int MOUSE_LEFT_BTN   = 1;
    @Native public static final int MOUSE_RIGHT_BTN  = 2;
    @Native public static final int MOUSE_MIDDLE_BTN = 4;

    protected abstract void _create();
    protected Robot() {
        // Ensure proper permission
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(CREATE_ROBOT_PERMISSION);
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

    // Subclasses must override and implement at least one of the following two
    // _getScreenCapture methods

    protected void _getScreenCapture(int x, int y, int width, int height, int[] data) {
        throw new UnsupportedOperationException("Not implementated in the base class");
    }

    protected Pixels _getScreenCapture(int x, int y, int width, int height, boolean isHiDPI) {
        Screen mainScreen = Screen.getMainScreen();
        float uiScaleX = mainScreen.getPlatformScaleX();
        float uiScaleY = mainScreen.getPlatformScaleY();
        int data[];
        int dw, dh;
        if (uiScaleX == 1.0f && uiScaleY == 1.0f) {
            data = new int[width * height];
            _getScreenCapture(x, y, width, height, data);
            dw = width;
            dh = height;
        } else {
            int pminx = (int) Math.floor(x * uiScaleX);
            int pminy = (int) Math.floor(y * uiScaleY);
            int pmaxx = (int) Math.ceil((x + width) * uiScaleX);
            int pmaxy = (int) Math.ceil((y + height) * uiScaleY);
            int pwidth = pmaxx - pminx;
            int pheight = pmaxy - pminy;
            int tmpdata[] = new int[pwidth * pheight];
            _getScreenCapture(pminx, pminy, pwidth, pheight, tmpdata);
            if (isHiDPI) {
                data = tmpdata;
                dw = pwidth;
                dh = pheight;
            } else {
                data = new int[width * height];
                int index = 0;
                for (int iy = 0; iy < height; iy++) {
                    float rely = ((y + iy + 0.5f) * uiScaleY) - (pminy + 0.5f);
                    int irely = (int) Math.floor(rely);
                    int fracty = (int) ((rely - irely) * 256);
                    for (int ix = 0; ix < width; ix++) {
                        float relx = ((x + ix + 0.5f) * uiScaleX) - (pminx + 0.5f);
                        int irelx = (int) Math.floor(relx);
                        int fractx = (int) ((relx - irelx) * 256);
                        data[index++] =
                            interp(tmpdata, irelx, irely, pwidth, pheight, fractx, fracty);
                    }
                }
                dw = width;
                dh = height;
            }
        }
        return Application.GetApplication().createPixels(dw, dh, IntBuffer.wrap(data));
    }

    /**
     * Returns a capture of the specified rectangular area of the screen.
     *
     * If {@code isHiDPI} argument is {@code true}, the returned Pixels object
     * dimensions may differ from the requested {@code width} and {@code
     * height} depending on how many physical pixels the area occupies on the
     * screen.  E.g. in HiDPI mode on the Mac (aka Retina display) the pixels
     * are doubled, and thus a screen capture of an area of size (10x10) pixels
     * will result in a Pixels object with dimensions (20x20). Calling code
     * should use the returned objects's getWidth() and getHeight() methods
     * to determine the image size.
     *
     * If (@code isHiDPI) is {@code false}, the returned Pixels object is of
     * the requested size. Note that in this case the image may be scaled in
     * order to fit to the requested dimensions if running on a HiDPI display.
     */
    public Pixels getScreenCapture(int x, int y, int width, int height, boolean isHiDPI) {
        Application.checkEventThread();
        return _getScreenCapture(x, y, width, height, isHiDPI);
    }

    /**
     * Returns a capture of the specified area of the screen.
     * It is equivalent to calling getScreenCapture(x, y, width, height, false),
     * i.e. this method takes a "LowDPI" screen shot.
     */
    public Pixels getScreenCapture(int x, int y, int width, int height) {
        return getScreenCapture(x, y, width, height, false);
    }

    private static int interp(int pixels[], int x, int y, int w, int h, int fractx1, int fracty1) {
        int fractx0 = 256 - fractx1;
        int fracty0 = 256 - fracty1;
        int i = y * w + x;
        int rgb00 = (x < 0 || y < 0 || x >= w || y >= h) ? 0 : pixels[i];
        if (fracty1 == 0) {
            // No interplation with pixels[y+1]
            if (fractx1 == 0) {
                // No interpolation with any neighbors
                return rgb00;
            }
            int rgb10 = (y < 0 || x+1 >= w || y >= h) ? 0 : pixels[i+1];
            return interp(rgb00, rgb10, fractx0, fractx1);
        } else if (fractx1 == 0) {
            // No interpolation with pixels[x+1]
            int rgb01 = (x < 0 || x >= w || y+1 >= h) ? 0 : pixels[i+w];
            return interp(rgb00, rgb01, fracty0, fracty1);
        } else {
            // All 4 neighbors must be interpolated
            int rgb10 = (y < 0 || x+1 >= w || y >= h) ? 0 : pixels[i+1];
            int rgb01 = (x < 0 || x >= w || y+1 >= h) ? 0 : pixels[i+w];
            int rgb11 = (x+1 >= w || y+1 >= h) ? 0 : pixels[i+w+1];
            return interp(interp(rgb00, rgb10, fractx0, fractx1),
                          interp(rgb01, rgb11, fractx0, fractx1),
                          fracty0, fracty1);
        }
    }

    private static int interp(int rgb0, int rgb1, int fract0, int fract1) {
        int a0 = (rgb0 >> 24) & 0xff;
        int r0 = (rgb0 >> 16) & 0xff;
        int g0 = (rgb0 >>  8) & 0xff;
        int b0 = (rgb0      ) & 0xff;
        int a1 = (rgb1 >> 24) & 0xff;
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >>  8) & 0xff;
        int b1 = (rgb1      ) & 0xff;
        int a = (a0 * fract0 + a1 * fract1) >> 8;
        int r = (r0 * fract0 + r1 * fract1) >> 8;
        int g = (g0 * fract0 + g1 * fract1) >> 8;
        int b = (b0 * fract0 + b1 * fract1) >> 8;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
