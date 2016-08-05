/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.ext.device.ios.sensors;

/**
 * Status bar orientation.
 */
public enum StatusBarOrientation {

    /**
     * The status bar is displayed on the "top" side for the current orientation.
     */
    PORTRAIT(1),

    /**
     * The status bar is displayed on the "bottom" side for the current orientation.
     */
    PORTRAIT_UPSIDE_DOWN(2),

    /**
     * The status bar is displayed on the "right" side for the current orientation.
     */
    LANDSCAPE_RIGHT(3),

    /**
     * The status bar is displayed on the "left" side for the current orientation.
     */
    LANDSCAPE_LEFT(4);


    private final int value;

    private StatusBarOrientation(int value) {
        this.value = value;
    }

    /**
     * Returns the numeric value for the status bar orientation.
     * @return the numeric value for the status bar orientation.
     */
    public int value() { return value; }
}
