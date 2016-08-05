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
package com.sun.glass.ui.gtk;

import com.sun.glass.ui.*;
import java.nio.IntBuffer;

final class GtkRobot extends Robot {

    @Override
    protected void _create() {
        // no-op
    }

    @Override
    protected void _destroy() {
        // no-op
    }

    @Override
    protected native void _keyPress(int code);

    @Override
    protected native void _keyRelease(int code);

    @Override
    protected native void _mouseMove(int x, int y);

    @Override
    protected native void _mousePress(int buttons);

    @Override
    protected native void _mouseRelease(int buttons);

    @Override
    protected native void _mouseWheel(int wheelAmt);

    @Override
    protected native int _getMouseX();

    @Override
    protected native int _getMouseY();

    @Override
    protected int _getPixelColor(int x, int y) {
        Screen mainScreen = Screen.getMainScreen();
        x = (int) Math.floor((x + 0.5) * mainScreen.getPlatformScaleX());
        y = (int) Math.floor((y + 0.5) * mainScreen.getPlatformScaleY());
        int[] result = new int[1];
        _getScreenCapture(x, y, 1, 1, result);
        return result[0];
    }

    native private void _getScreenCapture(int x, int y, int width, int height, int[] data);
    @Override protected Pixels _getScreenCapture(int x, int y, int width, int height, boolean isHiDPI) {
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

    static int interp(int pixels[], int x, int y, int w, int h, int fractx1, int fracty1) {
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

    static int interp(int rgb0, int rgb1, int fract0, int fract1) {
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
