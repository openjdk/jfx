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

import java.util.LinkedList;
import java.util.Hashtable;

final class LensTouchInputSupport {

    private final static GestureSupport gestures = new GestureSupport(false);
    private final static TouchInputSupport touches =
        new TouchInputSupport(gestures.createTouchCountListener(), false);

    private static class LensTouchPoint {
        LensView view;
        int state;
        long id;
        int x;
        int y;
        int absX;
        int absY;

        LensTouchPoint(LensView view, int state, long id,
                       int x, int y, int absX, int absY) {
            this.view = view;
            this.state = state;
            this.id = id;
            this.x = x;
            this.y = y;
            this.absX = absX;
            this.absY = absY;
        }

        @Override
        public String toString() {
            return "LensTouchPoint[view=" + view + ", state=" + state +  ", id=" + id + ", x=" +
                   x + ", y=" + y + ", absX= " + absX + ", absY=" + absY + "]";
        }
    }

    private static LinkedList<LensTouchPoint> touchPointsBuffer = new LinkedList<LensTouchPoint>();

    private static Hashtable<Long, LensTouchPoint> touchPointsById = new Hashtable<Long, LensTouchPoint>();
    private static final int TP_MIN_XY_CHANGE = 10;

    private static boolean shouldFilterTouchPoint(LensTouchPoint tp) {
        synchronized (touchPointsById) {

            LensTouchPoint storedTP = touchPointsById.get(tp.id);
            if (storedTP == null) {
                touchPointsById.put(tp.id, tp);
                return false;
            }
            
            //Filter move events with small change in coordinates
            if (tp.state == TouchEvent.TOUCH_MOVED) {
                int distanceX =  Math.abs(tp.absX - storedTP.absX);
                int distanceY =  Math.abs(tp.absY - storedTP.absY);
                if (distanceX < TP_MIN_XY_CHANGE && distanceY < TP_MIN_XY_CHANGE) {
                    return true;
                }
            }

            if (tp.state != TouchEvent.TOUCH_RELEASED) {
                touchPointsById.put(tp.id, tp);
            } else {
                touchPointsById.remove(tp.id);
            }
        }

        return false;
    }



    static private void notifyMouseEvent(LensTouchPoint tp) {

        LensApplication lensApplication = (LensApplication)Application.GetApplication();

        int type = 0;
        int modifier = KeyEvent.MODIFIER_NONE;
        int button = MouseEvent.BUTTON_NONE;
        switch (tp.state) {
            case TouchEvent.TOUCH_PRESSED :
                type = MouseEvent.DOWN;
                modifier = KeyEvent.MODIFIER_BUTTON_PRIMARY;
                button = MouseEvent.BUTTON_LEFT;
                break;
            case TouchEvent.TOUCH_RELEASED :
                type = MouseEvent.UP;
                button = MouseEvent.BUTTON_LEFT;
                break;
            case TouchEvent.TOUCH_MOVED :
                type = MouseEvent.MOVE;
                modifier = KeyEvent.MODIFIER_BUTTON_PRIMARY;
                break;
        }

        lensApplication.notifyMouseEvent(tp.view, type, tp.x, tp.y,
                                         tp.absX, tp.absY, button, modifier, false, false);
    }

    static void postTouchEvent(LensView view, int state, long id,
                                            int x, int y, int absX, int absY) {
        synchronized (touchPointsBuffer) {
            touchPointsBuffer.add(new LensTouchPoint(view, state, id, x, y, absX, absY));
        }
    }

    static void processTouchEvents() {
        LensTouchPoint tp = null;

        synchronized (touchPointsBuffer) {
            if (touchPointsBuffer.size() > 0) {
                tp = touchPointsBuffer.pollFirst();
            }
        }

        if (tp != null) {
            if (!shouldFilterTouchPoint(tp)) {
                touches.notifyBeginTouchEvent(tp.view, 0, true, 1);
                touches.notifyNextTouchEvent(tp.view, tp.state, tp.id, tp.x, tp.y,
                                             tp.absX, tp.absY);
                touches.notifyEndTouchEvent(tp.view);

                // Create similar mouse event for applications that use mouse events.
                notifyMouseEvent(tp);
            }
        }
    }
}

