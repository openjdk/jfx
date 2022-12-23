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
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

/**
 * An effect that shifts each pixel according to an (x,y) distance from
 * the (red,green) channels of a map image, respectively.
 */
public class DisplacementMap extends CoreEffect<RenderState> {

    private FloatMap mapData;
    private float scaleX = 1f;
    private float scaleY = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private boolean wrap;

    /**
     * Constructs a new {@code DisplacementMap} effect,
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new DisplacementMap(mapData, DefaultInput)
     * </pre>
     *
     * @throws IllegalArgumentException if {@code mapData} is null
     */
    public DisplacementMap(FloatMap mapData) {
        this(mapData, DefaultInput);
    }

    /**
     * Constructs a new {@code DisplacementMap} effect using the specified
     * input {@code Effect} for source data, or the default input if
     * {@code contentInput} is {@code null}.
     *
     * @param mapData the map data
     * @param contentInput the content input {@code Effect}
     * @throws IllegalArgumentException if {@code mapData} is null
     */
    public DisplacementMap(FloatMap mapData, Effect contentInput) {
        super(contentInput);
        setMapData(mapData);
        updatePeerKey("DisplacementMap");
    }

    /**
     * Returns the map data for this {@code Effect}.
     *
     * @return the map data for this {@code Effect}
     */
    public final FloatMap getMapData() {
        return mapData;
    }

    /**
     * Sets the map data for this {@code Effect}.
     *
     * @param mapData the map data for this {@code Effect}
     * @throws IllegalArgumentException if {@code mapData} is null
     */
    public void setMapData(FloatMap mapData) {
        if (mapData == null) {
            throw new IllegalArgumentException("Map data must be non-null");
        }
        FloatMap old = this.mapData;
        this.mapData = mapData;
    }

    /**
     * Returns the content input for this {@code Effect}.
     *
     * @return the content input for this {@code Effect}
     */
    public final Effect getContentInput() {
        return getInputs().get(0);
    }

    /**
     * Sets the content input for this {@code Effect} to a specific
     * {@code Effect} or to the default input if {@code input} is
     * {@code null}.
     *
     * @param contentInput the content input for this {@code Effect}
     */
    public void setContentInput(Effect contentInput) {
        setInput(0, contentInput);
    }

    /**
     * Returns the x scale factor.
     *
     * @return the x scale factor
     */
    public float getScaleX() {
        return scaleX;
    }

    /**
     * Sets the x scale factor.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 1.0
     *  Identity: 1.0
     * </pre>
     *
     * @param scaleX the x scale factor
     */
    public void setScaleX(float scaleX) {
        float old = this.scaleX;
        this.scaleX = scaleX;
    }

    /**
     * Returns the y scale factor.
     *
     * @return the y scale factor
     */
    public float getScaleY() {
        return scaleY;
    }

    /**
     * Sets the y scale factor.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 1.0
     *  Identity: 1.0
     * </pre>
     *
     * @param scaleY the y scale factor
     */
    public void setScaleY(float scaleY) {
        float old = this.scaleY;
        this.scaleY = scaleY;
    }

    /**
     * Returns the x offset factor.
     *
     * @return the x offset factor
     */
    public float getOffsetX() {
        return offsetX;
    }

    /**
     * Sets the x offset factor.
     * <pre>
     *       Min:  n/a
     *       Max:  n/a
     *   Default: -0.5
     *  Identity:  0.0
     * </pre>
     *
     * @param offsetX the x offset factor
     */
    public void setOffsetX(float offsetX) {
        float old = this.offsetX;
        this.offsetX = offsetX;
    }

    /**
     * Returns the y offset factor.
     *
     * @return the y offset factor
     */
    public float getOffsetY() {
        return offsetY;
    }

    /**
     * Sets the y offset factor.
     * <pre>
     *       Min:  n/a
     *       Max:  n/a
     *   Default: -0.5
     *  Identity:  0.0
     * </pre>
     *
     * @param offsetY the y offset factor
     */
    public void setOffsetY(float offsetY) {
        float old = this.offsetY;
        this.offsetY = offsetY;
    }

    /**
     * Returns whether values taken from outside the edges of the map
     * "wrap around" or not.
     *
     * @return true if wrapping is enabled, false otherwise
     */
    public boolean getWrap() {
        return this.wrap;
    }

    /**
     * Sets whether values taken from outside the edges of the map
     * "wrap around" or not.
     * <pre>
     *       Min:  n/a
     *       Max:  n/a
     *   Default: false
     *  Identity:  n/a
     * </pre>
     *
     * @param wrap true if wrapping is enabled, false otherwise
     */
    public void setWrap(boolean wrap) {
        boolean old = this.wrap;
        this.wrap = wrap;
    }

    /**
     * Transform the specified point {@code p} from the coordinate space
     * of the primary content input to the coordinate space of the effect
     * output.
     * In essence, this method asks the question "Which output coordinate
     * is most affected by the data at the specified coordinate in the
     * primary source input?"
     * <p>
     * Since the displacement map represents a mapping from output pixels
     * to relative source pixels, there may be multiple answers or no
     * answer for any given input coordinate so this method returns
     * {@code (NaN, NaN)}.
     *
     * @param p the point in the coordinate space of the primary content
     *          input to be transformed
     * @param defaultInput the default input {@code Effect} to be used in
     *                     all cases where a filter has a null input
     * @return the undefined point {@code (NaN, NaN)}
     */
    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        return new Point2D(Float.NaN, Float.NaN);
    }

    /**
     * Transform the specified point {@code p} from the coordinate space
     * of the output of the effect into the coordinate space of the
     * primary content input.
     * In essence, this method asks the question "Which source coordinate
     * contributes most to the definition of the output at the specified
     * coordinate?"
     * <p>
     * This method returns the floating point coordinate where the
     * backwards mapping displacement map tells it to get the data for
     * the specified coordinate.
     *
     * @param p the point in the coordinate space of the result output
     *          to be transformed
     * @param defaultInput the default input {@code Effect} to be used in
     *                     all cases where a filter has a null input
     * @return the untransformed point in the coordinate space of the
     *         primary content input
     */
    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        BaseBounds r = getBounds(BaseTransform.IDENTITY_TRANSFORM, defaultInput);
        float rw = r.getWidth();
        float rh = r.getHeight();
        float x = (p.x - r.getMinX()) / rw;
        float y = (p.y - r.getMinY()) / rh;
        // If the coordinates are outside of the effect there is no
        // displacement effect occuring so we do not transform the point.
        if (x >= 0 && y >= 0 && x < 1 && y < 1) {
            int mx = (int) (x * mapData.getWidth());
            int my = (int) (y * mapData.getHeight());
            float dx = mapData.getSample(mx, my, 0);
            float dy = mapData.getSample(mx, my, 1);
            x += scaleX * (dx + offsetX);
            y += scaleY * (dy + offsetY);
            if (wrap) {
                x -= Math.floor(x);
                y -= Math.floor(y);
            }
            p = new Point2D(x * rw + r.getMinX(),
                            y * rh + r.getMinY());
        }
        return getDefaultedInput(0, defaultInput).untransform(p, defaultInput);
    }

    @Override
    public ImageData filterImageDatas(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      RenderState rstate,
                                      ImageData... inputs)
    {
        // NOTE: The floatmap is mapped to the entire output bounds so
        // we need to use unclipped math in the peers to get the
        // texture mapping right.
        return super.filterImageDatas(fctx, transform, null, rstate, inputs);
    }
    @Override
    public RenderState getRenderState(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      Object renderHelper,
                                      Effect defaultInput)
    {
        // NOTE: We could scan the map and calculate all possible input points
        // that might contribute to the clipped output?  Until then, any
        // pixel in the output could come from any pixel in the input so
        // we will need the full input bounds to proceed...
        return RenderState.UnclippedUserSpaceRenderState;
    }

    @Override
    public boolean reducesOpaquePixels() {
        return true;
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        DirtyRegionContainer drc = regionPool.checkOut();
        drc.deriveWithNewRegion((RectBounds) getBounds(BaseTransform.IDENTITY_TRANSFORM, defaultInput));

        return drc;
    }
}
