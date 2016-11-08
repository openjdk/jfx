/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.shape;

public class NativePiscesRasterizerShim {

    public static void produceFillAlphas(float coords[], byte commands[], int nsegs, boolean nonzero,
                                         double mxx, double mxy, double mxt,
                                         double myx, double myy, double myt,
                                         int bounds[], byte mask[]) {
        NativePiscesRasterizer.produceFillAlphas(
                coords, commands, nsegs, nonzero,
                mxx, mxy, mxt,
                myx, myy, myt,
                bounds, mask);
    }

    public static void produceStrokeAlphas(float coords[], byte commands[], int nsegs,
                                           float lw, int cap, int join, float mlimit,
                                           float dashes[], float dashoff,
                                           double mxx, double mxy, double mxt,
                                           double myx, double myy, double myt,
                                           int bounds[], byte mask[]) {
        NativePiscesRasterizer.produceStrokeAlphas(
                coords, commands, nsegs,
                lw, cap, join, mlimit,
                dashes, dashoff,
                mxx, mxy, mxt,
                myx, myy, myt,
                bounds, mask);
    }

}
