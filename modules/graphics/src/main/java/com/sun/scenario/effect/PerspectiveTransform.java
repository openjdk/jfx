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

import com.sun.scenario.effect.impl.state.PerspectiveTransformState;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

public class PerspectiveTransform extends CoreEffect<RenderState> {
    private float tx[][] = new float[3][3];
    private float ulx, uly, urx, ury, lrx, lry, llx, lly;
    private float devcoords[] = new float[8];
    private final PerspectiveTransformState state = new PerspectiveTransformState();

    public PerspectiveTransform() {
        this(DefaultInput);
    }

    public PerspectiveTransform(Effect input) {
        super(input);
        setQuadMapping(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f);
        updatePeerKey("PerspectiveTransform");
    }

    @Override
    Object getState() {
        return state;
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
     * Sets the transform to map the unit square to the indicated
     * quadrilateral coordinates.
     * The resulting perspective transform will perform the following
     * coordinate mappings:
     * <pre>
     *     T(0, 0) = (ulx, uly)
     *     T(0, 1) = (urx, ury)
     *     T(1, 1) = (lrx, lry)
     *     T(1, 0) = (llx, lly)
     * </pre>
     * Note that the upper left corner of the unit square {@code (0, 0)}
     * is mapped to the coordinates specified by {@code (ulx, uly)} and
     * so on around the unit square in a clockwise direction.
     *
     * @param ulx The X coordinate to which {@code (0, 0)} is mapped.
     * @param uly The Y coordinate to which {@code (0, 0)} is mapped.
     * @param urx The X coordinate to which {@code (1, 0)} is mapped.
     * @param ury The Y coordinate to which {@code (1, 0)} is mapped.
     * @param lrx The X coordinate to which {@code (1, 1)} is mapped.
     * @param lry The Y coordinate to which {@code (1, 1)} is mapped.
     * @param llx The X coordinate to which {@code (0, 1)} is mapped.
     * @param lly The Y coordinate to which {@code (0, 1)} is mapped.
     */
    private void setUnitQuadMapping(float ulx, float uly,
                                    float urx, float ury,
                                    float lrx, float lry,
                                    float llx, float lly)
    {
        float dx3 = ulx - urx + lrx - llx;
        float dy3 = uly - ury + lry - lly;

        tx[2][2] = 1.0F;

        if ((dx3 == 0.0F) && (dy3 == 0.0F)) { // TODO: use tolerance (RT-27402)
            tx[0][0] = urx - ulx;
            tx[0][1] = lrx - urx;
            tx[0][2] = ulx;
            tx[1][0] = ury - uly;
            tx[1][1] = lry - ury;
            tx[1][2] = uly;
            tx[2][0] = 0.0F;
            tx[2][1] = 0.0F;
        } else {
            float dx1 = urx - lrx;
            float dy1 = ury - lry;
            float dx2 = llx - lrx;
            float dy2 = lly - lry;

            float invdet = 1.0F/(dx1*dy2 - dx2*dy1);
            tx[2][0] = (dx3*dy2 - dx2*dy3)*invdet;
            tx[2][1] = (dx1*dy3 - dx3*dy1)*invdet;
            tx[0][0] = urx - ulx + tx[2][0]*urx;
            tx[0][1] = llx - ulx + tx[2][1]*llx;
            tx[0][2] = ulx;
            tx[1][0] = ury - uly + tx[2][0]*ury;
            tx[1][1] = lly - uly + tx[2][1]*lly;
            tx[1][2] = uly;
        }
        state.updateTx(tx);
    }

    public final void setQuadMapping(float ulx, float uly,
                                     float urx, float ury,
                                     float lrx, float lry,
                                     float llx, float lly)
    {
        this.ulx = ulx;
        this.uly = uly;
        this.urx = urx;
        this.ury = ury;
        this.lrx = lrx;
        this.lry = lry;
        this.llx = llx;
        this.lly = lly;
    }

    @Override
    public RectBounds getBounds(BaseTransform transform,
                              Effect defaultInput)
    {
        setupDevCoords(transform);

        float minx, miny, maxx, maxy;
        minx = maxx = devcoords[0];
        miny = maxy = devcoords[1];
        for (int i = 2; i < devcoords.length; i += 2) {
            if (minx > devcoords[i]) minx = devcoords[i];
            else if (maxx < devcoords[i]) maxx = devcoords[i];
            if (miny > devcoords[i+1]) miny = devcoords[i+1];
            else if (maxy < devcoords[i+1]) maxy = devcoords[i+1];
        }
        return new RectBounds(minx, miny, maxx, maxy);
    }

    private void setupDevCoords(BaseTransform transform) {
        devcoords[0] = ulx;
        devcoords[1] = uly;
        devcoords[2] = urx;
        devcoords[3] = ury;
        devcoords[4] = lrx;
        devcoords[5] = lry;
        devcoords[6] = llx;
        devcoords[7] = lly;
        transform.transform(devcoords, 0, devcoords, 0, 4);
    }

    @Override
    public ImageData filter(FilterContext fctx,
                            BaseTransform transform,
                            Rectangle outputClip,
                            Object renderHelper,
                            Effect defaultInput)
    {
        setupTransforms(transform);
        RenderState rstate = getRenderState(fctx, transform, outputClip,
                                            renderHelper, defaultInput);
        Effect input = getDefaultedInput(0, defaultInput);
        Rectangle inputClip = rstate.getInputClip(0, outputClip);
        ImageData inputData =
            input.filter(fctx, BaseTransform.IDENTITY_TRANSFORM,
                         inputClip, null, defaultInput);
        if (!inputData.validate(fctx)) {
            inputData.unref();
            return new ImageData(fctx, null, inputData.getUntransformedBounds());
        }
        ImageData ret = filterImageDatas(fctx, transform, outputClip, rstate, inputData);
        inputData.unref();
        return ret;
    }

    @Override
    public Rectangle getResultBounds(BaseTransform transform,
                                     Rectangle outputClip,
                                     ImageData... inputDatas)
    {
        Rectangle ob = new Rectangle(getBounds(transform, null));
        ob.intersectWith(outputClip);
        return ob;
    }

    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        setupTransforms(BaseTransform.IDENTITY_TRANSFORM);
        Effect input = getDefaultedInput(0, defaultInput);
        p = input.transform(p, defaultInput);
        BaseBounds b = input.getBounds(BaseTransform.IDENTITY_TRANSFORM, defaultInput);
        float sx = (float) ((p.x - b.getMinX()) / b.getWidth());
        float sy = (float) ((p.y - b.getMinY()) / b.getHeight());
        float dx = tx[0][0] * sx + tx[0][1] * sy + tx[0][2];
        float dy = tx[1][0] * sx + tx[1][1] * sy + tx[1][2];
        float dw = tx[2][0] * sx + tx[2][1] * sy + tx[2][2];
        p = new Point2D(dx / dw, dy / dw);
        return p;
    }

    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        setupTransforms(BaseTransform.IDENTITY_TRANSFORM);
        Effect input = getDefaultedInput(0, defaultInput);
        float dx = (float) p.x;
        float dy = (float) p.y;
        float itx[][] = state.getITX();
        float sx = itx[0][0] * dx + itx[0][1] * dy + itx[0][2];
        float sy = itx[1][0] * dx + itx[1][1] * dy + itx[1][2];
        float sw = itx[2][0] * dx + itx[2][1] * dy + itx[2][2];
        BaseBounds b = input.getBounds(BaseTransform.IDENTITY_TRANSFORM, defaultInput);
        p = new Point2D(b.getMinX() + (sx / sw) * b.getWidth(),
                        b.getMinY() + (sy / sw) * b.getHeight());
        p = getDefaultedInput(0, defaultInput).untransform(p, defaultInput);
        return p;
    }

    private void setupTransforms(BaseTransform transform) {
        setupDevCoords(transform);
        setUnitQuadMapping(devcoords[0], devcoords[1],
                           devcoords[2], devcoords[3],
                           devcoords[4], devcoords[5],
                           devcoords[6], devcoords[7]);
    }

    @Override
    public RenderState getRenderState(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      Object renderHelper,
                                      Effect defaultInput)
    {
        // RT-27402
        // TODO: We could inverse map the output bounds through the perspective
        // transform to see what portions of the input contribute to the result,
        // but until we implement such a process we will just use the stock
        // object that specifies no clipping of the inputs.
        return RenderState.UnclippedUserSpaceRenderState;
    }

    @Override
    public boolean reducesOpaquePixels() {
        return true;
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        DirtyRegionContainer drc = regionPool.checkOut();

        //RT-28197 - Dirty regions could be computed in more efficient way
        drc.deriveWithNewRegion((RectBounds) getBounds(BaseTransform.IDENTITY_TRANSFORM, defaultInput));

        return drc;
    }
}
