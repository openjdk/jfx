/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.state;

import com.sun.scenario.effect.ZoomRadialBlur;

public class ZoomRadialBlurState {

    private float dx = -1f;
    private float dy = -1f;
    private final ZoomRadialBlur effect;

    public ZoomRadialBlurState(ZoomRadialBlur effect) {
        this.effect = effect;
    }

    public int getRadius() {
        return effect.getRadius();
    }

    /**
     * Updates offsets by X and Y axes.
     */
    public void updateDeltas(float dx, float dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Invalidates X and Y offsets.
     */
    public void invalidateDeltas() {
        this.dx = -1f;
        this.dy = -1f;
    }

    /**
     * Returns offset by X axis to the next pixel
     * @return offset by X axis to the next pixel
     */
    public float getDx() {
        return dx;
    }

    /**
     * Returns offset by Y axis to the next pixel
     * @return offset by Y axis to the next pixel
     */
    public float getDy() {
        return dy;
    }

    public int getNumSteps() {
        int r = getRadius();
        return r * 2 + 1;
    }

    public float getAlpha() {
        float r = getRadius();
        return 1.0f/(2.0f*r + 1.0f);
    }
}
