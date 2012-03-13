/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.pgstub;


public class StubColor extends StubPaint {

    private double red, green, blue;
    private double opacity;

    public StubColor(double r, double g, double b, double o) {
        red = r;
        green = g;
        blue = b;
        opacity = o;
    }


    @Override
    public String toString() {
        int r = (int)Math.round(red * 255f);
        int g = (int)Math.round(green * 255f);
        int b = (int)Math.round(blue * 255f);
        int o = (int)Math.round(opacity * 255f);
        return String.format("0x%02x%02x%02x%02x" , r, g, b, o);
    }
}
