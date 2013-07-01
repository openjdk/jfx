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
 * A concrete implementation of {@link ShapePair} for general shapes
 * and combination types.  This class can represent the combination and
 * perform basic operations like bounds, intersects and contains, but
 * it cannot supply a PathIterator for the result of the combination.
 * As a result the renderer will either have to be able to render the
 * combination of two shapes directly or perform some other geometry
 * computation of its own to achieve the subtraction or intersection.
 *
 */
public class GeneralShapePair extends ShapePair {
    private final Shape outer;
    private final Shape inner;
    private final int combinationType;

    public GeneralShapePair(Shape outer, Shape inner, int combinationType) {
        this.outer = outer;
        this.inner = inner;
        this.combinationType = combinationType;
    }

    @Override
    public final int getCombinationType() {
        return combinationType;
    }

    @Override
    public final Shape getOuterShape() {
        return outer;
    }

    @Override
    public final Shape getInnerShape() {
        return inner;
    }

    @Override
    public Shape copy() {
        return new GeneralShapePair(outer.copy(), inner.copy(), combinationType);
    }

    @Override
    public boolean contains(float x, float y) {
        if (combinationType == TYPE_INTERSECT) {
            return outer.contains(x, y) && inner.contains(x, y);
        } else {
            return outer.contains(x, y) && !inner.contains(x, y);
        }
    }

    @Override
    public boolean intersects(float x, float y, float w, float h) {
        if (combinationType == TYPE_INTERSECT) {
            return outer.intersects(x, y, w, h) && inner.intersects(x, y, w, h);
        } else {
            return outer.intersects(x, y, w, h) && !inner.contains(x, y, w, h);
        }
    }

    @Override
    public boolean contains(float x, float y, float w, float h) {
        if (combinationType == TYPE_INTERSECT) {
            return outer.contains(x, y, w, h) && inner.contains(x, y, w, h);
        } else {
            return outer.contains(x, y, w, h) && !inner.intersects(x, y, w, h);
        }
    }

    @Override
    public RectBounds getBounds() {
        RectBounds b = outer.getBounds();
        if (combinationType == TYPE_INTERSECT) {
            b.intersectWith(inner.getBounds());
        }
        return b;
    }

    @Override
    public PathIterator getPathIterator(BaseTransform tx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PathIterator getPathIterator(BaseTransform tx, float flatness) {
        return new FlatteningPathIterator(getPathIterator(tx), flatness);
    }
}
