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

package com.sun.prism.paint;

import com.sun.javafx.geom.transform.BaseTransform;
import java.util.List;

public abstract class Gradient extends Paint {

    public static final int PAD = 0;
    public static final int REFLECT = 1;
    public static final int REPEAT = 2;

    private final int numStops;
    private final List<Stop> stops;
    private final BaseTransform gradientTransform;
    private final int spreadMethod;
    private long cacheOffset = -1;

    protected Gradient(Type type,
                       BaseTransform gradientTransform,
                       boolean proportional,
                       int spreadMethod,
                       List<Stop> stops)
    {
        super(type, proportional, false);
        if (gradientTransform != null) {
            this.gradientTransform = gradientTransform.copy();
        } else {
            this.gradientTransform = BaseTransform.IDENTITY_TRANSFORM;
        }
        this.spreadMethod = spreadMethod;
        this.numStops = stops.size();
        this.stops = stops;
    }


    public int getSpreadMethod() {
        return spreadMethod;
    }

    public BaseTransform getGradientTransformNoClone() {
        return gradientTransform;
    }

    public int getNumStops() {
        return numStops;
    }

    public List<Stop> getStops() {
        return stops;
    }

    public void setGradientOffset(long offset) {
        this.cacheOffset = offset;
    }

    public long getGradientOffset() {
        return cacheOffset;
    }

    @Override
    public boolean isOpaque() {
        for (int i = 0; i < numStops; i++) {
            if (!stops.get(i).getColor().isOpaque()) {
                return false;
            }
        }
        return true;
    }
}
