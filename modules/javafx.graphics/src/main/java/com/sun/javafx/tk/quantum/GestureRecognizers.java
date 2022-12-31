/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import java.util.Vector;
import java.util.Collection;

class GestureRecognizers implements GestureRecognizer {

    private Collection<GestureRecognizer> recognizers = new Vector<>();
    private GestureRecognizer workList[];

    void add(GestureRecognizer r) {
        if (!contains(r)) {
            recognizers.add(r);
            workList = null;
        }
    }

    void remove(GestureRecognizer r) {
        if (contains(r)) {
            recognizers.remove(r);
            workList = null;
        }
    }

    boolean contains(GestureRecognizer r) {
        return recognizers.contains(r);
    }

    private GestureRecognizer[] synchWorkList() {
        if (workList == null) {
            workList = recognizers.toArray(new GestureRecognizer[0]);
        }
        return workList;
    }

    @Override
    public void notifyBeginTouchEvent(long time, int modifiers, boolean isDirect, int touchEventCount) {
        final GestureRecognizer[] wl = synchWorkList();
        for (int idx = 0; idx != wl.length; ++idx) {
            wl[idx].notifyBeginTouchEvent(time, modifiers, isDirect, touchEventCount);
        }
    }

    @Override
    public void notifyNextTouchEvent(long time, int type, long touchId,
                                     int x, int y, int xAbs, int yAbs)
    {
        final GestureRecognizer[] wl = synchWorkList();
        for (int idx = 0; idx != wl.length; ++idx) {
            wl[idx].notifyNextTouchEvent(time, type, touchId, x, y, xAbs, yAbs);
        }
    }

    @Override
    public void notifyEndTouchEvent(long time) {
        final GestureRecognizer[] wl = synchWorkList();
        for (int idx = 0; idx != wl.length; ++idx) {
            wl[idx].notifyEndTouchEvent(time);
        }
    }
}
