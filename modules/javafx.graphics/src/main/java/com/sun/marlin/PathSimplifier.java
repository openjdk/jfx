/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.marlin;

public final class PathSimplifier implements DPathConsumer2D {

    // distance threshold in pixels (device)
    private static final double PIX_THRESHOLD = MarlinProperties.getPathSimplifierPixelTolerance();
    // squared tolerance in pixels
    private static final double SQUARE_TOLERANCE = PIX_THRESHOLD * PIX_THRESHOLD;

    // members:
    private DPathConsumer2D delegate;
    // current reference point
    private double cx, cy;
    // flag indicating if the given point was skipped
    private boolean skipped;
    // last skipped point
    private double sx, sy;

    PathSimplifier() {
    }

    public PathSimplifier init(final DPathConsumer2D delegate) {
        this.delegate = delegate;
        skipped = false;
        return this; // fluent API
    }

    private void finishPath() {
        if (skipped) {
            _lineTo(sx, sy);
        }
    }

    @Override
    public void pathDone() {
        finishPath();
        delegate.pathDone();
    }

    @Override
    public void closePath() {
        finishPath();
        delegate.closePath();
    }

    @Override
    public void moveTo(final double xe, final double ye) {
        finishPath();
        delegate.moveTo(xe, ye);
        cx = xe;
        cy = ye;
    }

    @Override
    public void lineTo(final double xe, final double ye) {
        // Test if segment is too small:
        double dx = (xe - cx);
        double dy = (ye - cy);

        if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
            skipped = true;
            sx = xe;
            sy = ye;
            return;
        }
        _lineTo(xe, ye);
    }

    private void _lineTo(final double xe, final double ye) {
        delegate.lineTo(xe, ye);
        cx = xe;
        cy = ye;
        skipped = false;
    }

    @Override
    public void quadTo(final double x1, final double y1,
                       final double xe, final double ye)
    {
        // Test if curve is too small:
        double dx = (xe - cx);
        double dy = (ye - cy);

        if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
            // check control points P1:
            dx = (x1 - cx);
            dy = (y1 - cy);

            if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
                skipped = true;
                sx = xe;
                sy = ye;
                return;
            }
        }
        delegate.quadTo(x1, y1, xe, ye);
        cx = xe;
        cy = ye;
        skipped = false;
    }

    @Override
    public void curveTo(final double x1, final double y1,
                        final double x2, final double y2,
                        final double xe, final double ye)
    {
        // Test if curve is too small:
        double dx = (xe - cx);
        double dy = (ye - cy);

        if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
            // check control points P1:
            dx = (x1 - cx);
            dy = (y1 - cy);

            if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
                // check control points P2:
                dx = (x2 - cx);
                dy = (y2 - cy);

                if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
                    skipped = true;
                    sx = xe;
                    sy = ye;
                    return;
                }
            }
        }
        delegate.curveTo(x1, y1, x2, y2, xe, ye);
        cx = xe;
        cy = ye;
        skipped = false;
    }
}
