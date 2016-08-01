/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Abstraction of an input device and its capabilities
 */
interface InputDevice {

    /**
     * @return true if the device provides touch input for at least one touch
     * point.
     */
    boolean isTouch();

    /**
     * @return true if the device provides touch input for more than one touch
     * point.
     */
    boolean isMultiTouch();

    /**
     * @return true if the device provides relative pointing events. Relative
     * pointing events are those received from devices where the event signifies
     * a relative change in cursor position on the screen. For example, a mouse,
     * trackball, trackpad or joystick is a relative pointing device. A touch
     * screen is an sbsolute pointing device.
     */
    boolean isRelative();

    /**
     * @return true if the device provides direction arrow keys (UP, DOWN, LEFT
     * and RIGHT) and a selection key.
     */
    boolean is5Way();

    /**
     * @return true if the device provides keys for the letters 'A' through 'Z',
     * the digits '0' through '9' and also SPACE, SHIFT and TAB keys in addition
     * to directional arrows and a selection key.
     */
    boolean isFullKeyboard();

}
