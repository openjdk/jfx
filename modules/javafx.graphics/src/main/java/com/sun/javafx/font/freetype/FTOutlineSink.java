/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.freetype;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import java.util.Arrays;

/**
 * The accumulator behind {@link FTNative#FT_Outline_Decompose(long)}: the {@code PathData} struct and the
 * four {@code JFX_Outline_*Func} callbacks of {@code freetype.c} (commit {@code 7b43255b30}, lines
 * 494-604), in Java. One instance serves one {@code FT_Outline_Decompose} call on the thread that makes
 * it: FreeType invokes the outline functions synchronously, in contour order, before the downcall returns.
 * <p>
 * Every callback appends one type byte - the literal 0, 1, 2 or 3 of the C, which are
 * {@link PathIterator#SEG_MOVETO}, {@code SEG_LINETO}, {@code SEG_QUADTO} and {@code SEG_CUBICTO} - and its
 * points as {@code (float) v / 64.0} for x and {@code -(float) v / 64.0} for y ({@code F26DOT6TOFLOAT},
 * freetype.c:494, with the C's operand order and its double intermediate), so a point on the baseline
 * gives {@code -0.0f} exactly as the C did. {@code SEG_CLOSE} is never emitted. The arrays start at the
 * C's 10 types and 50 coordinates ({@code DEFAULT_LEN_TYPES}, {@code DEFAULT_LEN_COORDS}) and grow on
 * demand; {@link #toPath()} hands {@code Path2D} arrays of exactly {@code numTypes} and {@code numCoords}
 * elements through the {@code Path2D(int, byte[], int, float[], int)} constructor, as
 * {@code NewByteArray(numTypes)} and {@code NewFloatArray(numCoords)} did (freetype.c:651-667), with the
 * winding rule 0 the C passed.
 * <p>
 * The C's {@code checkSize} returned {@code FT_Err_Array_Too_Large} to FreeType when {@code realloc}
 * failed, and the native then answered {@code null}. Here the same error is returned by the upcall for any
 * failure of the accumulator, recorded through {@link #failed(Throwable)}; the segment limit of
 * {@link #FTOutlineSink(int)} exists so that a test can provoke that path with a real outline.
 */
final class FTOutlineSink {

    /** {@code DEFAULT_LEN_TYPES} and {@code DEFAULT_LEN_COORDS}, freetype.c:495-496. */
    static final int DEFAULT_LEN_TYPES = 10;
    static final int DEFAULT_LEN_COORDS = 50;

    private final int maxTypes;
    private byte[] pointTypes = new byte[DEFAULT_LEN_TYPES];
    private int numTypes;
    private float[] pointCoords = new float[DEFAULT_LEN_COORDS];
    private int numCoords;
    private Throwable failure;

    /** An accumulator with no limit but the Java heap, which is what production uses. */
    FTOutlineSink() {
        this(Integer.MAX_VALUE);
    }

    /**
     * An accumulator that refuses the segment after {@code maxTypes}: the call that would append it throws
     * {@link IllegalStateException}, which the upcall turns into {@code FT_Err_Array_Too_Large}.
     */
    FTOutlineSink(int maxTypes) {
        if (maxTypes < 0) {
            throw new IllegalArgumentException("maxTypes " + maxTypes);
        }
        this.maxTypes = maxTypes;
    }

    /** {@code JFX_Outline_MoveToFunc}, freetype.c:536-547. */
    void moveTo(long x, long y) {
        append(PathIterator.SEG_MOVETO, 1);
        point(x, y);
    }

    /** {@code JFX_Outline_LineToFunc}, freetype.c:549-560. */
    void lineTo(long x, long y) {
        append(PathIterator.SEG_LINETO, 1);
        point(x, y);
    }

    /** {@code JFX_Outline_ConicToFunc}, freetype.c:562-576: the control point first, then the end point. */
    void conicTo(long controlX, long controlY, long x, long y) {
        append(PathIterator.SEG_QUADTO, 2);
        point(controlX, controlY);
        point(x, y);
    }

    /** {@code JFX_Outline_CubicToFunc}, freetype.c:578-595: both control points, then the end point. */
    void cubicTo(long control1X, long control1Y, long control2X, long control2Y, long x, long y) {
        append(PathIterator.SEG_CUBICTO, 3);
        point(control1X, control1Y);
        point(control2X, control2Y);
        point(x, y);
    }

    /**
     * {@code checkSize} (freetype.c:506-534) followed by the type append: room for one more type and
     * {@code points * 2} more coordinates, then the type byte.
     */
    private void append(int type, int points) {
        if (numTypes >= maxTypes) {
            throw new IllegalStateException("outline exceeds " + maxTypes + " segments");
        }
        if (numTypes == pointTypes.length) {
            pointTypes = Arrays.copyOf(pointTypes, grown(pointTypes.length, DEFAULT_LEN_TYPES));
        }
        int needed = numCoords + points * 2;
        if (needed > pointCoords.length) {
            pointCoords = Arrays.copyOf(pointCoords, Math.max(needed, grown(pointCoords.length, DEFAULT_LEN_COORDS)));
        }
        pointTypes[numTypes++] = (byte) type;
    }

    /** At least {@code step} more elements, doubling once the array is larger than that. */
    private static int grown(int length, int step) {
        return length + Math.max(length, step);
    }

    /** {@code F26DOT6TOFLOAT(x)} and {@code -F26DOT6TOFLOAT(y)}: {@code (float) v / 64.0} as a float. */
    private void point(long x, long y) {
        pointCoords[numCoords++] = (float) ((float) x / 64.0);
        pointCoords[numCoords++] = (float) (-(float) y / 64.0);
    }

    /** Records the first failure of a callback; the upcall answers FreeType with an error either way. */
    void failed(Throwable t) {
        if (failure == null) {
            failure = t;
        }
    }

    /** The first failure a callback recorded, or {@code null}. */
    Throwable failure() {
        return failure;
    }

    /** Segments appended so far. */
    int numTypes() {
        return numTypes;
    }

    /** Coordinates appended so far. */
    int numCoords() {
        return numCoords;
    }

    /**
     * {@code new Path2D(0, types, numTypes, coords, numCoords)} over arrays of exactly those lengths
     * (freetype.c:651-667). An empty outline gives a path with no segment, as the C's empty arrays did.
     */
    Path2D toPath() {
        return new Path2D(Path2D.WIND_EVEN_ODD, Arrays.copyOf(pointTypes, numTypes), numTypes,
                          Arrays.copyOf(pointCoords, numCoords), numCoords);
    }
}
