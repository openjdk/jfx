/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.lens;

import com.sun.glass.events.TouchEvent;
import com.sun.glass.events.MouseEvent;
import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.TouchInputSupport;
import com.sun.glass.ui.GestureSupport;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.View;

import java.lang.Integer;
import java.security.AccessController;
import java.security.PrivilegedAction;


final class LensTouchInputSupport {

    /**
     * This property define the size of the tap radius which can be seen as the
     * 'finger size'. After the first tap, a touch point will be considered 
     * STILL as long as the point coordinates are within the tap radius. When the 
     * point coordinates move outside the tap radius the point will be considered
     * as 'dragging' and all move events will be reported as long as they are 
     * greater then the touchMoveSensitivity property 
     * Property is used by Lens native input driver 
     * 
     */
    private static final int touchTapRadius;
    /**
     * This property determine the sensitivity of move events from touch. The 
     * bigger the value the less sensitive is the touch screen. In practice move
     * events with a delta smaller then the value of this property will be 
     * filtered out.The value of the property is in pixels. 
     * Property is used by Lens native input driver 
     */    
    private static final int touchMoveSensitivity;

    /**
     * This property enable/disable multi touch support by the input driver.
     * When the property is disabled and a multitouch screen is connected, the
     * input driver will 'downgrade' the screen events to a single touch 
     * point, as if a single touch screen was connected 
     * 
     */
    private static final boolean useMultiTouch;

    static {
        touchTapRadius = AccessController.doPrivileged(
        new PrivilegedAction<Integer>() {
            @Override
            public Integer run() {
                return Integer.getInteger("lens.input.touch.TapRadius", 20);
            }
        });

        touchMoveSensitivity = AccessController.doPrivileged(
        new PrivilegedAction<Integer>() {
            @Override
            public Integer run() {
                return Integer.getInteger("lens.input.touch.MoveSensitivity", 3);
            }
        });

        useMultiTouch = AccessController.doPrivileged(
        new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.getBoolean("com.sun.javafx.experimental.embedded.multiTouch");
            }
        });
    }


    private final static GestureSupport gestures = new GestureSupport(false);
    private final static TouchInputSupport touches =
        new TouchInputSupport(gestures.createTouchCountListener(), false);

    static void postTouchEvent(LensView view, int state, long id,
                               int x, int y, int absX, int absY) {
        touches.notifyBeginTouchEvent(view, 0, true, 1);
        touches.notifyNextTouchEvent(view, state, id, x, y, absX, absY);
        touches.notifyEndTouchEvent(view);
    }

    static void postMultiTouchEvent(LensView view, int[] states, long[] ids,
                                    int[] xs, int[] ys, int dx, int dy) {
        touches.notifyBeginTouchEvent(view, 0, true, states.length);
        for (int i = 0; i < states.length; i++) {
            touches.notifyNextTouchEvent(view, states[i], ids[i],
                                         xs[i] + dx, ys[i] + dy,
                                         xs[i], ys[i]);
        }
        touches.notifyEndTouchEvent(view);
    }
}

