/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect;

/**
 * An implementation supertype for both Gaussian and Box filter based
 * shadows to facilitate their conditional usage inside the various
 * composite shadow effects like DropShadow and InnerShadow.
 *
 * The radius, width and height parameters all refer to the corresponding
 * dimension that a "Gaussian" blur filter would use - other shadow
 * implementations will have to use whatever parameters produce a
 * similar effect.
 * The width and height parameters should relate to the radius parameter
 * by the equation {@code w,h = 2 * r + 1} and if the width and height are
 * set to something different then the radius parameter will be an average
 * of the corresponding individual dimensional radius values.
 */
public abstract class AbstractShadow extends LinearConvolveCoreEffect {
    public AbstractShadow(Effect input) {
        super(input);
    }

    public enum ShadowMode {
        ONE_PASS_BOX,
        TWO_PASS_BOX,
        THREE_PASS_BOX,
        GAUSSIAN,
    }

    public abstract ShadowMode getMode();
    public abstract AbstractShadow implFor(ShadowMode m);
    public abstract float getGaussianRadius();
    public abstract void  setGaussianRadius(float r);
    public abstract float getGaussianWidth();
    public abstract void  setGaussianWidth(float w);
    public abstract float getGaussianHeight();
    public abstract void  setGaussianHeight(float h);
    public abstract float getSpread();
    public abstract void  setSpread(float spread);
    public abstract Color4f getColor();
    public abstract void setColor(Color4f c);
    public abstract Effect getInput();
    public abstract void setInput(Effect input);
}
