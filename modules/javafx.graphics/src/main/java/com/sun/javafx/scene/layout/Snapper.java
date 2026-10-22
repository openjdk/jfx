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

package com.sun.javafx.scene.layout;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.layout.RenderScaleContext;

/**
 * Turns a {@link RenderScaleContext}'s raw scale factors into the actual snapping
 * operations used by layout math.
 */
public interface Snapper {

    /**
     * A default snapper for 1.0 scaling.
     */
    static final Snapper IDENTITY = new Snapper() {

        @Override
        public double snapPositionX(double value) {
            return Math.round(value);
        }

        @Override
        public double snapPositionY(double value) {
            return Math.round(value);
        }

        @Override
        public double snapSpaceX(double value) {
            return Math.round(value);
        }

        @Override
        public double snapSpaceY(double value) {
            return Math.round(value);
        }

        @Override
        public double snapSizeX(double value) {
            return Math.ceil(value - Math.ulp(value));
        }

        @Override
        public double snapSizeY(double value) {
            return Math.ceil(value - Math.ulp(value));
        }
    };

    /**
     * A default snapper that does no snapping at all.
     */
    static final Snapper NO_SNAPPING = new Snapper() {
        @Override
        public double snapPositionX(double value) {
            return value;
        }

        @Override
        public double snapPositionY(double value) {
            return value;
        }

        @Override
        public double snapSpaceX(double value) {
            return value;
        }

        @Override
        public double snapSpaceY(double value) {
            return value;
        }

        @Override
        public double snapSizeX(double value) {
            return value;
        }

        @Override
        public double snapSizeY(double value) {
            return value;
        }
    };

    class Cache {
        private static final Map<RenderScaleContext, Snapper> INSTANCES = new ConcurrentHashMap<>();

        {
            INSTANCES.put(RenderScaleContext.DEFAULT, IDENTITY);
        }
    }

    static Snapper createSnapper(RenderScaleContext context) {
        return Cache.INSTANCES.computeIfAbsent(context, _ -> new Snapper() {
            final double ssx = context.snapScaleX();
            final double ssy = context.snapScaleY();

            @Override
            public double snapPositionX(double value) {
                return ScaledMath.round(value, ssx);
            }

            @Override
            public double snapPositionY(double value) {
                return ScaledMath.round(value, ssy);
            }

            @Override
            public double snapSpaceX(double value) {
                return ScaledMath.round(value, ssx);
            }

            @Override
            public double snapSpaceY(double value) {
                return ScaledMath.round(value, ssy);
            }

            @Override
            public double snapSizeX(double value) {
                return ScaledMath.ceil(value, ssx);
            }

            @Override
            public double snapSizeY(double value) {
                return ScaledMath.ceil(value, ssy);
            }
        });
    }

    double snapPositionX(double value);
    double snapPositionY(double value);
    double snapSpaceX(double value);
    double snapSpaceY(double value);
    double snapSizeX(double value);
    double snapSizeY(double value);
}
