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

package com.sun.glass.ui.monocle.input.devices;

import com.sun.glass.ui.monocle.input.TestApplication;
import com.sun.glass.ui.monocle.input.UInput;
import org.junit.Assume;

public class EGalaxSingleTouchDevice2 extends EGalaxSingleTouchDeviceBase {

    @Override
    public int addPoint(double x, double y) {
        int p = super.addPoint(x, y);
        ui.processLine("EV_ABS ABS_X " + transformedXs[p]);
        ui.processLine("EV_ABS ABS_Y " + transformedYs[p]);
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_PRESSURE 1");
        return p;
    }

    @Override
    public void removePoint(int p) {
        super.removePoint(p);
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_ABS ABS_PRESSURE 0");
    }
}
