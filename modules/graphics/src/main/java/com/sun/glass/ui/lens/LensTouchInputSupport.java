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

    private static final int touchTapRadius;

    static {
        touchTapRadius = AccessController.doPrivileged(
        new PrivilegedAction<Integer>() {
            @Override
            public Integer run() {
                return Integer.getInteger("lens.touchTapRadius", 20);
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

