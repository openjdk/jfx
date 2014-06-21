/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

/**
 * An effect that returns a mask that is the inverse of the input (i.e.,
 * opaque areas of the input become transparent and vice versa) with a
 * given offset and padding.
 */
public class InvertMask extends CoreEffect<RenderState> {

    private int pad;
    private int xoff;
    private int yoff;

    /**
     * Constructs a new {@code InvertMask} effect with the default pad (10),
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new InvertMask(10, DefaultInput)
     * </pre>
     */
    public InvertMask() {
        this(10);
    }

    /**
     * Constructs a new {@code InvertMask} effect with the default pad (10),
     * using the given {@code Effect} as the input.
     * This is a shorthand equivalent to:
     * <pre>
     *     new InvertMask(10, input)
     * </pre>
     *
     * @param input the single input {@code Effect}
     */
    public InvertMask(Effect input) {
        this(10, input);
    }

    /**
     * Constructs a new {@code InvertMask} effect with the given pad value
     * using the default input for source data.
     *
     * @param pad the amount of padding on each side of the resulting image
     * @throws IllegalArgumentException if {@code pad} is negative
     */
    public InvertMask(int pad) {
        this(pad, DefaultInput);
    }

    /**
     * Constructs a new {@code InvertMask} effect with the given pad value
     * and effect input.
     *
     * @param pad the amount of padding on each side of the resulting image
     * @param input the single input {@code Effect}
     * @throws IllegalArgumentException if {@code pad} is negative
     */
    public InvertMask(int pad, Effect input) {
        super(input);
        setPad(pad);
        updatePeerKey("InvertMask");
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
     * Returns the amount of padding added to each side of the resulting
     * image, in pixels.
     *
     * @return the amount of padding, in pixels
     */
    public int getPad() {
        return pad;
    }

    /**
     * Sets the amount of padding added to each side of the resulting
     * image, in pixels.
     * <pre>
     *       Min: 0
     *       Max: Integer.MAX_VALUE
     *   Default: 0
     *  Identity: 0
     * </pre>
     *
     * @param pad the amount of padding, in pixels
     * @throws IllegalArgumentException if {@code pad} is negative
     */
    public void setPad(int pad) {
        if (pad < 0) {
            throw new IllegalArgumentException("Pad value must be non-negative");
        }
        int old = this.pad;
        this.pad = pad;
    }

    /**
     * Returns the offset in the x direction, in pixels.
     *
     * @return the offset in the x direction, in pixels.
     */
    public int getOffsetX() {
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
    public void setOffsetX(int xoff) {
        int old = this.xoff;
        this.xoff = xoff;
    }

    /**
     * Returns the offset in the y direction, in pixels.
     *
     * @return the offset in the y direction, in pixels.
     */
    public int getOffsetY() {
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
    public void setOffsetY(int yoff) {
        float old = this.yoff;
        this.yoff = yoff;
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform, Effect defaultInput) {
        BaseBounds bounds = super.getBounds(BaseTransform.IDENTITY_TRANSFORM, defaultInput);
        BaseBounds ret = new RectBounds(bounds.getMinX(), bounds.getMinY(),
                bounds.getMaxX(), bounds.getMaxY());
        ((RectBounds) ret).grow(pad, pad);
        if (!transform.isIdentity()) {
            ret = transformBounds(transform, ret);
        }
        return ret;
    }

    @Override
    public Rectangle getResultBounds(BaseTransform transform,
                                     Rectangle outputClip,
                                     ImageData... inputDatas)
    {
        Rectangle r = super.getResultBounds(transform, outputClip, inputDatas);
        Rectangle ret = new Rectangle(r);
        ret.grow(pad, pad);
        return ret;
    }

    @Override
    public RenderState getRenderState(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      Object renderHelper,
                                      Effect defaultInput)
    {
        return new RenderState() {
            @Override
            public EffectCoordinateSpace getEffectTransformSpace() {
                return EffectCoordinateSpace.UserSpace;
            }

            @Override
            public BaseTransform getInputTransform(BaseTransform filterTransform) {
                return BaseTransform.IDENTITY_TRANSFORM;
            }

            @Override
            public BaseTransform getResultTransform(BaseTransform filterTransform) {
                return filterTransform;
            }

            @Override
            public Rectangle getInputClip(int i, Rectangle filterClip) {
                // Typically the mask gets padded by synthetic opaque mask data
                // that is computed from the lack of input pixels in the padded
                // area.  But in the case where a clip has cut down on the
                // amount of data we are generating then the padding for this
                // particular (clipped) operation may not be synthetic, rather it
                // may actually represent inversions of real input pixels.  Thus,
                // the clip for the input needs to make sure it includes any
                // valid input pixels that may appear not just in the output
                // clip, but also in its padded fringe.
                if (filterClip != null) {
                    if (pad != 0) {
                        filterClip = new Rectangle(filterClip);
                        filterClip.grow(pad, pad);
                    }
                }
                return filterClip;
            }
        };
    }

    @Override
    public boolean reducesOpaquePixels() {
        return true;
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
