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

package com.sun.prism.impl.shape;

import com.sun.javafx.geom.PathIterator;
import com.sun.prism.BasicStroke;
import org.junit.Test;

public class NativePiscesRasterizerTest {
    static final int JOIN_BEVEL = BasicStroke.JOIN_BEVEL;
    static final int JOIN_MITER = BasicStroke.JOIN_MITER;
    static final int JOIN_ROUND = BasicStroke.JOIN_ROUND;

    static final int CAP_SQUARE = BasicStroke.CAP_SQUARE;
    static final int CAP_ROUND  = BasicStroke.CAP_ROUND;
    static final int CAP_BUTT   = BasicStroke.CAP_BUTT;

    static final byte SEG_MOVETO  = PathIterator.SEG_MOVETO;
    static final byte SEG_LINETO  = PathIterator.SEG_LINETO;
    static final byte SEG_QUADTO  = PathIterator.SEG_QUADTO;
    static final byte SEG_CUBICTO = PathIterator.SEG_CUBICTO;
    static final byte SEG_CLOSE   = PathIterator.SEG_CLOSE;

    static final float coords6[] = new float[6];
    static final float coords1[] = new float[1];
    static final float coords3[] = new float[3];
    static final float coords5[] = new float[5];
    static final float coords7[] = new float[7];
    static final byte move_arr[] = { SEG_MOVETO };
    static final byte moveline_arr[] = { SEG_MOVETO, SEG_LINETO };
    static final byte movequad_arr[] = { SEG_MOVETO, SEG_QUADTO };
    static final byte movecubic_arr[] = { SEG_MOVETO, SEG_CUBICTO };
    static final int bounds10[] = { 0, 0, 10, 10 };
    static final byte mask1k[] = new byte[1024];

    @Test(expected=java.lang.NullPointerException.class)
    public void FillNullCoords() {
        NativePiscesRasterizer.produceFillAlphas(null, move_arr, 1, true,
                                                 1, 0, 0, 0, 1, 0,
                                                 bounds10, mask1k);
    }

    @Test(expected=java.lang.NullPointerException.class)
    public void FillNullCommands() {
        NativePiscesRasterizer.produceFillAlphas(coords6, null, 1, true,
                                                 1, 0, 0, 0, 1, 0,
                                                 bounds10, mask1k);
    }

    @Test(expected=java.lang.NullPointerException.class)
    public void FillNullBounds() {
        NativePiscesRasterizer.produceFillAlphas(coords6, move_arr, 1, true,
                                                 1, 0, 0, 0, 1, 0,
                                                 null, mask1k);
    }

    @Test(expected=java.lang.NullPointerException.class)
    public void FillNullMask() {
        NativePiscesRasterizer.produceFillAlphas(coords6, move_arr, 1, true,
                                                 1, 0, 0, 0, 1, 0,
                                                 bounds10, null);
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void FillShortBounds() {
        NativePiscesRasterizer.produceFillAlphas(coords6, move_arr, 1, true,
                                                 1, 0, 0, 0, 1, 0,
                                                 new int[3], mask1k);
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void FillShortCommands() {
        NativePiscesRasterizer.produceFillAlphas(coords6, move_arr, 2, true,
                                                 1, 0, 0, 0, 1, 0,
                                                 bounds10, mask1k);
    }

    @Test
    public void FillBadCommands() {
        byte badcmd_arr[] = new byte[2];
        badcmd_arr[0] = SEG_MOVETO;
        for (int i = 0; i < 256; i++) {
            switch (i) {
                case SEG_MOVETO:
                case SEG_LINETO:
                case SEG_QUADTO:
                case SEG_CUBICTO:
                case SEG_CLOSE:
                    continue;
                default:
                    badcmd_arr[1] = (byte) i;
                    try {
                        NativePiscesRasterizer.produceFillAlphas(coords6, badcmd_arr, 2, true,
                                                                 1, 0, 0, 0, 1, 0,
                                                                 bounds10, mask1k);
                        throw new RuntimeException("allowed bad command: "+i);
                    } catch (InternalError e) {
                    }
                    break;
            }
        }
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void FillShortMoveCoords() {
        NativePiscesRasterizer.produceFillAlphas(coords1, move_arr, 1, true,
                                                 1, 0, 0, 0, 1, 0,
                                                 bounds10, mask1k);
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void FillShortLineCoords() {
        NativePiscesRasterizer.produceFillAlphas(coords3, moveline_arr, 2, true,
                                                 1, 0, 0, 0, 1, 0,
                                                 bounds10, mask1k);
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void FillShortQuadCoords() {
        NativePiscesRasterizer.produceFillAlphas(coords5, movequad_arr, 2, true,
                                                 1, 0, 0, 0, 1, 0,
                                                 bounds10, mask1k);
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void FillShortCubicCoords() {
        NativePiscesRasterizer.produceFillAlphas(coords7, movecubic_arr, 2, true,
                                                 1, 0, 0, 0, 1, 0,
                                                 bounds10, mask1k);
    }

    @Test(expected=java.lang.NullPointerException.class)
    public void StrokeNullCoords() {
        NativePiscesRasterizer.produceStrokeAlphas(null, move_arr, 1,
                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                   1, 0, 0, 0, 1, 0,
                                                   bounds10, mask1k);
    }

    @Test(expected=java.lang.NullPointerException.class)
    public void StrokeNullCommands() {
        NativePiscesRasterizer.produceStrokeAlphas(coords6, null, 1,
                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                   1, 0, 0, 0, 1, 0,
                                                   bounds10, mask1k);
    }

    @Test(expected=java.lang.NullPointerException.class)
    public void StrokeNullBounds() {
        NativePiscesRasterizer.produceStrokeAlphas(coords6, move_arr, 1,
                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                   1, 0, 0, 0, 1, 0,
                                                   null, mask1k);
    }

    @Test(expected=java.lang.NullPointerException.class)
    public void StrokeNullMask() {
        NativePiscesRasterizer.produceStrokeAlphas(coords6, move_arr, 1,
                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                   1, 0, 0, 0, 1, 0,
                                                   bounds10, null);
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void StrokeShortBounds() {
        NativePiscesRasterizer.produceStrokeAlphas(coords6, move_arr, 1,
                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                   1, 0, 0, 0, 1, 0,
                                                   new int[3], mask1k);
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void StrokeShortCommands() {
        NativePiscesRasterizer.produceStrokeAlphas(coords6, move_arr, 2,
                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                   1, 0, 0, 0, 1, 0,
                                                   bounds10, mask1k);
    }

    @Test
    public void StrokeBadCommands() {
        byte badcmd_arr[] = new byte[2];
        badcmd_arr[0] = SEG_MOVETO;
        for (int i = 0; i < 256; i++) {
            switch (i) {
                case SEG_MOVETO:
                case SEG_LINETO:
                case SEG_QUADTO:
                case SEG_CUBICTO:
                case SEG_CLOSE:
                    continue;
                default:
                    badcmd_arr[1] = (byte) i;
                    try {
                        NativePiscesRasterizer.produceStrokeAlphas(coords6, badcmd_arr, 2,
                                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                                   1, 0, 0, 0, 1, 0,
                                                                   bounds10, mask1k);
                        throw new RuntimeException("allowed bad command: "+i);
                    } catch (InternalError e) {
                    }
                    break;
            }
        }
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void StrokeShortMoveCoords() {
        NativePiscesRasterizer.produceStrokeAlphas(coords1, move_arr, 1,
                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                   1, 0, 0, 0, 1, 0,
                                                   bounds10, mask1k);
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void StrokeShortLineCoords() {
        NativePiscesRasterizer.produceStrokeAlphas(coords3, moveline_arr, 2,
                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                   1, 0, 0, 0, 1, 0,
                                                   bounds10, mask1k);
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void StrokeShortQuadCoords() {
        NativePiscesRasterizer.produceStrokeAlphas(coords5, movequad_arr, 2,
                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                   1, 0, 0, 0, 1, 0,
                                                   bounds10, mask1k);
    }

    @Test(expected=java.lang.ArrayIndexOutOfBoundsException.class)
    public void StrokeShortCubicCoords() {
        NativePiscesRasterizer.produceStrokeAlphas(coords7, movecubic_arr, 2,
                                                   10, CAP_ROUND, JOIN_ROUND, 10, null, 0,
                                                   1, 0, 0, 0, 1, 0,
                                                   bounds10, mask1k);
    }
}
