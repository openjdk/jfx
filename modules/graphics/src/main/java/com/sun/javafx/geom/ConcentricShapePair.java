/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.geom;

import com.sun.javafx.geom.transform.BaseTransform;

/**
 * An implementation of {@link ShapePair} that represents the space between
 * two concentric, non-intersecting Shapes.  The pair of shapes could also be
 * represented by a regular {@link Path2D} by simply concatenating the two
 * shapes and using a {@code WIND_EVEN_ODD} winding rule.  (Note that a
 * {@code WIND_NON_ZERO} winding rule could be used if the two shapes have
 * opposite windings, but that cannot be guaranteed without further inspection
 * of the two shapes and the {@code WIND_NON_ZERO} rule works fine without
 * that restriction.)
 *
 */
public final class ConcentricShapePair extends ShapePair {
    private final Shape outer;
    private final Shape inner;

    public ConcentricShapePair(Shape outer, Shape inner) {
        this.outer = outer;
        this.inner = inner;
    }

    @Override
    public int getCombinationType() {
        return TYPE_SUBTRACT;
    }

    @Override
    public Shape getOuterShape() {
        return outer;
    }

    @Override
    public Shape getInnerShape() {
        return inner;
    }

    @Override
    public Shape copy() {
        return new ConcentricShapePair(outer.copy(), inner.copy());
    }

    @Override
    public boolean contains(float x, float y) {
        return outer.contains(x, y) && !inner.contains(x, y);
    }

    @Override
    public boolean intersects(float x, float y, float w, float h) {
        return outer.intersects(x, y, w, h) && !inner.contains(x, y, w, h);
    }

    @Override
    public boolean contains(float x, float y, float w, float h) {
        return outer.contains(x, y, w, h) && !inner.intersects(x, y, w, h);
    }

    @Override
    public RectBounds getBounds() {
        return outer.getBounds();
    }

    @Override
    public PathIterator getPathIterator(BaseTransform tx) {
        return new PairIterator(outer.getPathIterator(tx),
                                inner.getPathIterator(tx));
    }

    @Override
    public PathIterator getPathIterator(BaseTransform tx, float flatness) {
        return new PairIterator(outer.getPathIterator(tx, flatness),
                                inner.getPathIterator(tx, flatness));
    }

    static class PairIterator implements PathIterator {
        PathIterator outer;
        PathIterator inner;

        PairIterator(PathIterator outer, PathIterator inner) {
            this.outer = outer;
            this.inner = inner;
        }

        public int getWindingRule() {
            return WIND_EVEN_ODD;
        }

        public int currentSegment(float[] coords) {
            if (outer.isDone()) {
                return inner.currentSegment(coords);
            } else {
                return outer.currentSegment(coords);
            }
        }

        public boolean isDone() {
            return outer.isDone() && inner.isDone();
        }

        public void next() {
            if (outer.isDone()) {
                inner.next();
            } else {
                outer.next();
            }
        }
    }
}
