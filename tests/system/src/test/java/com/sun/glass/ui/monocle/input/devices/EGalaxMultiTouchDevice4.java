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

/** The touch screen used in the Freescale i.MX6Q Sabre Device Platform,
 * extrapolated to five touch points. There is some guesswork here as to
 * whether the screen always resends stationary points. This class assumes that
 * it resends stationary points.
 * On release of the last point this device does not send a tracking ID.
 * Instead it sends:
 *
 * EV_ABS ABS_MT_TOUCH_MAJOR 0
 * EV_KEY BTN_TOUCH 0
 * EV_SYN SYN_MT_REPORT 0
 */
public class EGalaxMultiTouchDevice4 extends EGalaxMultiTouchDeviceBase {

    public EGalaxMultiTouchDevice4() {
        super(true, true, true, SendIDOnRelease.DONT_SEND);
    }

}
