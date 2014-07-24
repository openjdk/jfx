/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/** Native cursor for use with X11 when we are NOT using x11 input
 *
 */
public class X11WarpingCursor extends X11Cursor {

    private int nextX, nextY;
    private static X xLib = X.getX();

    /** Update the next coordinates for the cursor.  The actual move will occur
     * on the next buffer swap
     * @param x the new X location on the screen
     * @param y the new Y location on the screen
     */
    @Override
    void setLocation(int x, int y) {
        if (x != nextX || y != nextY) {
            nextX = x;
            nextY = y;
            MonocleWindowManager.getInstance().repaintAll();
        }
    }

    /** Called from the swap buffer implementation for this accelerated screen.
     *  Tells X to move the cursor by a given x/y offset from the current
     *  location.
     */
    void warp() {
        if (isVisible) {
            int[] position = new int[2];
            xLib.XQueryPointer(xdisplay, xwindow, position);
            if (position[0] != nextX || position[1] != nextY) {
                xLib.XWarpPointer(xdisplay, 0l, 0l, 0, 0, 0, 0,
                               nextX - position[0],
                               nextY - position[1]);
            }
        }
    }
}
