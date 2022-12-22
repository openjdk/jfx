/*
 * Copyright (c) 2008, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.Translate2D;

/**
 * A trivial effect that offsets the input image by the given x/y
 * adjustment values.
 */
public class Offset extends Effect {

    private int xoff;
    private int yoff;

    /**
     * Constructs a new {@code Offset} effect with the given x/y
     * adjustment values.
     *
     * @param xoff the offset in the x direction, in pixels
     * @param yoff the offset in the y direction, in pixels
     * @param input the single input {@code Effect}
     */
    public Offset(int xoff, int yoff, Effect input) {
        super(input);
        this.xoff = xoff;
        this.yoff = yoff;
    }

    /**
     * Returns the input for this {@code Effect}.
     *
     * @return the input for this {@code Effect}
     */
    public final Effect getInput() {
        return getInputs().get(0);
    }

    /**
     * Sets the input for this {@code Effect} to a specific
     * {@code Effect} or to the default input if {@code input} is
     * {@code null}.
     *
     * @param input the input for this {@code Effect}
     */
    public void setInput(Effect input) {
        setInput(0, input);
    }

    /**
     * Returns the offset in the x direction, in pixels.
     *
     * @return the offset in the x direction, in pixels.
     */
    public int getX() {
        return xoff;
    }

    /**
     * Sets the offset in the x direction, in pixels.
     * <pre>
     *       Min: Integer.MIN_VALUE
     *       Max: Integer.MAX_VALUE
     *   Default: 0
     *  Identity: 0
     * </pre>
     *
     * @param xoff the offset in the x direction, in pixels
     */
    public void setX(int xoff) {
        int old = this.xoff;
        this.xoff = xoff;
    }

    /**
     * Returns the offset in the y direction, in pixels.
     *
     * @return the offset in the y direction, in pixels.
     */
    public int getY() {
        return yoff;
    }

    /**
     * Sets the offset in the y direction, in pixels.
     * <pre>
     *       Min: Integer.MIN_VALUE
     *       Max: Integer.MAX_VALUE
     *   Default: 0
     *  Identity: 0
     * </pre>
     *
     * @param yoff the offset in the y direction, in pixels
     */
    public void setY(int yoff) {
        float old = this.yoff;
        this.yoff = yoff;
    }

    static BaseTransform getOffsetTransform(BaseTransform transform,
                                          double xoff, double yoff)
    {
        if (transform == null || transform.isIdentity()) {
            return Translate2D.getInstance(xoff, yoff);
        } else {
            return transform.copy().deriveWithTranslation(xoff, yoff);
        }
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform,
                              Effect defaultInput)
    {
        BaseTransform at = getOffsetTransform(transform, xoff, yoff);
        Effect input = getDefaultedInput(0, defaultInput);
        return input.getBounds(at, defaultInput);
    }

    @Override
    public ImageData filter(FilterContext fctx,
                            BaseTransform transform,
                            Rectangle outputClip,
                            Object renderHelper,
                            Effect defaultInput)
    {
        BaseTransform at = getOffsetTransform(transform, xoff, yoff);
        // REMIND: Does the outputClip need to be transformed?
        // Probably not since we are modifying the transform of the
        // full operation and the clip is in "device space"...
        Effect input = getDefaultedInput(0, defaultInput);
        return input.filter(fctx, at, outputClip, renderHelper, defaultInput);
    }

    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        p = getDefaultedInput(0, defaultInput).transform(p, defaultInput);
        float x = p.x + xoff;
        float y = p.y + yoff;
        p = new Point2D(x, y);
        return p;
    }

    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        float x = p.x - xoff;
        float y = p.y - yoff;
        p = new Point2D(x, y);
        p = getDefaultedInput(0, defaultInput).untransform(p, defaultInput);
        return p;
    }

    @Override
    public AccelType getAccelType(FilterContext fctx) {
        return getInputs().get(0).getAccelType(fctx);
    }

    @Override
    public boolean reducesOpaquePixels() {
        return getX() != 0 || getY() != 0 || (getInput() != null && getInput().reducesOpaquePixels());
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        Effect di = getDefaultedInput(0, defaultInput);
        DirtyRegionContainer drc = di.getDirtyRegions(defaultInput, regionPool);

        if (xoff != 0 || yoff != 0) {
            for (int i = 0; i < drc.size(); i++) {
                drc.getDirtyRegion(i).translate(xoff, yoff, 0);
            }
        }

        return drc;
    }
}
