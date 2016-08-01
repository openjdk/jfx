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

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.scenario.effect.impl.state.RenderState;

/**
 * The implementation base class for {@link Effect} subclasses that operate
 * by filtering the inputs at the pixel level.
 * @param <T> the specific subclass of {@link RenderState} returned from the
 *        {@link #getRenderState()} method.
 */
public abstract class FilterEffect<T extends RenderState> extends Effect {
    protected FilterEffect() {
        super();
    }

    protected FilterEffect(Effect input) {
        super(input);
    }

    protected FilterEffect(Effect input1, Effect input2) {
        super(input1, input2);
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform,
                                Effect defaultInput)
    {
        int numinputs = getNumInputs();
        RenderState rstate = getRenderState(null, transform, null,
                                            null, defaultInput);
        BaseTransform inputtx = rstate.getInputTransform(transform);
        BaseBounds ret;
        if (numinputs == 1) {
            Effect input = getDefaultedInput(0, defaultInput);
            ret = input.getBounds(inputtx, defaultInput);
        } else {
            BaseBounds inputBounds[] = new BaseBounds[numinputs];
            for (int i = 0; i < numinputs; i++) {
                Effect input = getDefaultedInput(i, defaultInput);
                inputBounds[i] = input.getBounds(inputtx, defaultInput);
            }
            ret = combineBounds(inputBounds);
        }
        return transformBounds(rstate.getResultTransform(transform), ret);
    }

    protected static Rectangle untransformClip(BaseTransform transform,
                                               Rectangle clip)
    {
        if (transform.isIdentity() || clip == null || clip.isEmpty()) {
            return clip;
        }
        // We are asked to produce samples for the pixels in the
        // Rectangular clip.  The samples requested are delivered for
        // the centers of the pixels for every pixel in that range.
        // Thus, we need valid data for the clip inset by 0.5 pixels
        // all around.
        // But, when we untransform, we need to make sure that the data
        // we provide can be used to provide a valid sample for each of
        // those points.  If the mapped sample coordinate falls on a
        // non-integer coordinate then we need the data for the 4 pixels
        // around that point.  Thus, we need a sample for the pixel that it
        // falls on, and potentially a sample for the next pixel over if
        // we are within 0.5 pixels of the edge of those border pixels.
        // The full operation is then:
        //     clip.inset(0.5)       // reduce to requested pixel centers
        //     tx.untransform(clip)  // untransform to new source space
        //     clip.outset(0.5)      // expand for bilinear interpolation
        //     clip.roundtopixels()  // clamp to pixel edges
        Rectangle transformedBounds = new Rectangle();
        if (transform.isTranslateOrIdentity()) {
            // In this case the inset and outset cancel each other out
            // and the floor(x0,y0) and ceil(x1,y1) are enough to provide
            // whatever padding is needed.
            transformedBounds.setBounds(clip);
            double tx = -transform.getMxt();
            double ty = -transform.getMyt();
            int itx = (int) Math.floor(tx);
            int ity = (int) Math.floor(ty);
            transformedBounds.translate(itx, ity);
            if (itx != tx) {
                // floor(x0) is 1 more pixel away from ceil(x1)
                transformedBounds.width++;
            }
            if (ity != ty) {
                // floor(y0) is 1 more pixel away from ceil(y1)
                transformedBounds.height++;
            }
            return transformedBounds;
        }
        RectBounds b = new RectBounds(clip);
        try {
            b.grow(-0.5f, -0.5f);
            b = (RectBounds) transform.inverseTransform(b, b);
            b.grow(0.5f, 0.5f);
            transformedBounds.setBounds(b);
        } catch (NoninvertibleTransformException e) {
            // Non-invertible means the transform has collapsed onto
            // a point or line and so the results of the effect are
            // not visible so we can use the empty bounds object we
            // created for transformedBounds.  Ideally this would be
            // checked further up in the chain, but in case we get here
            // we might as well do as little work as we can.
        }
        return transformedBounds;
    }

    /**
     * Returns the object representing the rendering strategy and state for
     * the filter operation characterized by the specified arguments.
     * This call can also be used to get a state object for non-rendering
     * cases like querying the bounds of an operation in which case the
     * {@link FilterContext} object may be null.
     * {@code outputClip} and {@code renderHelper} may always be null just
     * as they may be null for a given filter operation.
     *
     * @param fctx the context object that would be used by the Renderer
     *             if this call is preparing for a render operation, or null
     * @param transform the transform for the output of this operation
     * @param outputClip the clip rectangle that may restrict this operation, or null
     * @param renderHelper the rendering helper object that can be used to shortcut
     *                     this operation under certain conditions, or null
     * @param defaultInput the {@link Effect} to be used in place of any null inputs
     * @return
     */
    public abstract T getRenderState(FilterContext fctx,
                                     BaseTransform transform,
                                     Rectangle outputClip,
                                     Object renderHelper,
                                     Effect defaultInput);

    @Override
    public ImageData filter(FilterContext fctx,
                            BaseTransform transform,
                            Rectangle outputClip,
                            Object renderHelper,
                            Effect defaultInput)
    {
        T rstate = getRenderState(fctx, transform, outputClip,
                                  renderHelper, defaultInput);
        int numinputs = getNumInputs();
        ImageData inputDatas[] = new ImageData[numinputs];
        Rectangle filterClip;
        BaseTransform inputtx = rstate.getInputTransform(transform);
        BaseTransform resulttx = rstate.getResultTransform(transform);
        if (resulttx.isIdentity()) {
            filterClip = outputClip;
        } else {
            filterClip = untransformClip(resulttx, outputClip);
        }
        for (int i = 0; i < numinputs; i++) {
            Effect input = getDefaultedInput(i, defaultInput);
            inputDatas[i] =
                input.filter(fctx, inputtx,
                             rstate.getInputClip(i, filterClip),
                             null, defaultInput);
            if (!inputDatas[i].validate(fctx)) {
                for (int j = 0; j <= i; j++) {
                    inputDatas[j].unref();
                }
                return new ImageData(fctx, null, null);
            }
        }
        ImageData ret = filterImageDatas(fctx, inputtx, filterClip, rstate, inputDatas);
        for (int i = 0; i < numinputs; i++) {
            inputDatas[i].unref();
        }
        if (!resulttx.isIdentity()) {
            if (renderHelper instanceof ImageDataRenderer) {
                ImageDataRenderer renderer = (ImageDataRenderer) renderHelper;
                renderer.renderImage(ret, resulttx, fctx);
                ret.unref();
                ret = null;
            } else {
                ret = ret.transform(resulttx);
            }
        }
        return ret;
    }

    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        return getDefaultedInput(0, defaultInput).transform(p, defaultInput);
    }

    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        return getDefaultedInput(0, defaultInput).untransform(p, defaultInput);
    }

    protected abstract ImageData filterImageDatas(FilterContext fctx,
                                                  BaseTransform transform,
                                                  Rectangle outputClip,
                                                  T rstate,
                                                  ImageData... inputDatas);
}
