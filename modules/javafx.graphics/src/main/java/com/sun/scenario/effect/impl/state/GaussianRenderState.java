/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.scenario.effect.Color4f;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.BufferUtil;
import java.nio.FloatBuffer;

/**
 */
public class GaussianRenderState extends LinearConvolveRenderState {
    public static final float MAX_RADIUS = (MAX_KERNEL_SIZE - 1) / 2;

    // General variables representing the convolve operation
    private boolean isShadow;
    private Color4f shadowColor;
    private float spread;

    // Values specific to this operation, calculated from the rendering context
    private EffectCoordinateSpace space;
    private BaseTransform inputtx;
    private BaseTransform resulttx;
    private float inputRadiusX;  // expected radius given inputtx
    private float inputRadiusY;
    private float spreadPass;

    // Values specific to a given filter pass
    private int validatedPass;
    private PassType passType;
    private float passRadius;   // actual radius for src ImageData
    private FloatBuffer weights;
    private float samplevectors[];  // dx, dy for pixel sampling, both passes
    private float weightsValidRadius;
    private float weightsValidSpread;

    static FloatBuffer getGaussianWeights(FloatBuffer weights,
                                          int pad,
                                          float radius,
                                          float spread)
    {
        int r = pad;
        int klen = (r * 2) + 1;
        if (weights == null) {
            weights = BufferUtil.newFloatBuffer(128);
        }
        weights.clear();
        float sigma = radius / 3;
        float sigma22 = 2 * sigma * sigma;
        if (sigma22 < Float.MIN_VALUE) {
            // Avoid divide by 0 below (it can generate NaN values).
            sigma22 = Float.MIN_VALUE;
        }
        float total = 0.0F;
        for (int row = -r; row <= r; row++) {
            float kval = (float) Math.exp(-(row * row) / sigma22);
            weights.put(kval);
            total += kval;
        }
        total += (weights.get(0) - total) * spread;
        for (int i = 0; i < klen; i++) {
            weights.put(i, weights.get(i) / total);
        }
        int limit = getPeerSize(klen);
        while (weights.position() < limit) {
            weights.put(0.0F);
        }
        weights.limit(limit);
        weights.rewind();
        return weights;
    }

    /**
     * Constructs a {@link RenderState} for a 2 dimensional Gaussian convolution.
     *
     * @param xradius the Gaussian radius along the user space X axis
     * @param yradius the Gaussian radius along the user space Y axis
     * @param spread the spread amount
     * @param isShadow true if this is a shadow operation
     * @param shadowColor the color of the shadow operation
     * @param filtertx the transform applied to the filter operation
     */
    public GaussianRenderState(float xradius, float yradius, float spread,
                               boolean isShadow, Color4f shadowColor, BaseTransform filtertx)
    {
        /*
         * The operation starts as a description of the size of a (pair of)
         * Gaussian kernels measured relative to that user space coordinate
         * system and to be applied horizontally and vertically in that same
         * space.  The presence of a filter transform can mean that the
         * direction we apply the gaussian convolutions could change as well
         * as the new size of that Gaussian distribution curve relative to
         * the pixels produced under that transform.
         *
         * We will track the direction and size of the Gaussian as we traverse
         * different coordinate spaces with the intent that eventually we
         * will perform the math of the convolution with weights calculated
         * for one sample per pixel in the indicated direction and applied as
         * closely to the intended final filter transform as we can achieve
         * with the following caveats:
         *
         * - There is a maximum kernel size that the hardware pixel shaders
         *   can apply so we will try to keep the scaling of the filtered
         *   pixels low enough that we do not exceed that data limitation.
         *
         * - Software prefers to apply these weights along horizontal and
         *   vertical vectors, but can apply them in an arbitrary direction
         *   if need be.
         *
         * - If the Gaussian kernel is large enough, then applying a smaller
         *   Gaussian kernel to a downscaled input is indistinguishable to
         *   applying the larger kernel to a larger scaled input.  Our maximum
         *   kernel size is large enough for this effect to be hidden if we
         *   max out the kernel.
         *
         * - We can tell the inputs what transform we want them to use, but
         *   they can always produce output under a different transform and
         *   then return a result with a "post-processing" trasnform to be
         *   applied (as we are doing here ourselves).  Thus, we can plan
         *   how we want to apply the convolution weights and samples here,
         *   but we will have to reevaluate our actions when the actual
         *   input pixels are created later.
         *
         * - If we are blurring enough to trigger the MAX_RADIUS exceptions
         *   then we can blur at a nice axis-aligned orientation (which is
         *   preferred for the software versions of the shaders) and perform
         *   any rotation and skewing in the final post-processing result
         *   transform as that amount of blurring will quite effectively cover
         *   up any distortion that would occur by not rendering at the
         *   appropriate angles.
         *
         * To achieve this we start out with untransformed sample vectors
         * which are unit vectors along the X and Y axes.  We transform them
         * into the requested filter space, adjust the kernel size and see
         * if we can support that kernel size.  If it is too large of a
         * projected kernel, then we request the input at a smaller scale
         * and perform a maximum kernel convolution on it and then indicate
         * that this result will need to be scaled by the caller.  When this
         * method is done we will have computed what we need to do to the
         * input pixels when they come in if the inputtx was honored, otherwise
         * we may have to adjust the values further in {@link @validateInput()}.
         */
        this.isShadow = isShadow;
        this.shadowColor = shadowColor;
        this.spread = spread;
        if (filtertx == null) filtertx = BaseTransform.IDENTITY_TRANSFORM;
        double mxx = filtertx.getMxx();
        double mxy = filtertx.getMxy();
        double myx = filtertx.getMyx();
        double myy = filtertx.getMyy();
        // Transformed unit axis vectors are essentially (mxx, myx) and (mxy, myy).
        double txScaleX = Math.hypot(mxx, myx);
        double txScaleY = Math.hypot(mxy, myy);
        boolean scaled = false;
        float scaledRadiusX = (float) (xradius * txScaleX);
        float scaledRadiusY = (float) (yradius * txScaleY);
        if (scaledRadiusX < MIN_EFFECT_RADIUS && scaledRadiusY < MIN_EFFECT_RADIUS) {
            // Entire blur is essentially a NOP in device space, we should
            // set up the values to force NOP processing rather than relying
            // on calculations to do it for us.
            this.inputRadiusX = 0.0f;
            this.inputRadiusY = 0.0f;
            this.spreadPass = 0;
            this.space = EffectCoordinateSpace.RenderSpace;
            this.inputtx = filtertx;
            this.resulttx = BaseTransform.IDENTITY_TRANSFORM;
            this.samplevectors = new float[] { 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f };
        } else {
            if (scaledRadiusX > MAX_RADIUS) {
                scaledRadiusX = MAX_RADIUS;
                txScaleX = MAX_RADIUS / xradius;
                scaled = true;
            }
            if (scaledRadiusY > MAX_RADIUS) {
                scaledRadiusY = MAX_RADIUS;
                txScaleY = MAX_RADIUS / yradius;
                scaled = true;
            }
            this.inputRadiusX = scaledRadiusX;
            this.inputRadiusY = scaledRadiusY;
            // We need to apply the spread on only one pass
            // Prefer pass1 if r1 is not tiny (or at least bigger than r0)
            // Otherwise use pass 0 so that it doesn't disappear
            this.spreadPass = (inputRadiusY > 1f || inputRadiusY >= inputRadiusX) ? 1 : 0;
            if (scaled) {
                this.space = EffectCoordinateSpace.CustomSpace;
                this.inputtx = BaseTransform.getScaleInstance(txScaleX, txScaleY);
                this.resulttx = filtertx
                        .copy()
                        .deriveWithScale(1.0 / txScaleX, 1.0 / txScaleY, 1.0);
                // assert resulttx.deriveWithConcatenation(inputtx).equals(filtertx)
                this.samplevectors = new float[] { 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f };
            } else {
                this.space = EffectCoordinateSpace.RenderSpace;
                this.inputtx = filtertx;
                this.resulttx = BaseTransform.IDENTITY_TRANSFORM;
                // These values should produce 2 normalized unit vectors in the
                // direction of the transformed axis vectors.
                this.samplevectors = new float[] { (float) (mxx / txScaleX),
                                                   (float) (myx / txScaleX),
                                                   (float) (mxy / txScaleY),
                                                   (float) (myy / txScaleY),
                                                   0.0f, 0.0f };
            }
        }
        // If the input honors our requested transforms then samplevectors
        // will be the unit vectors in the correct direction to sample by
        // pixel distances in the input texture and the inputRadii will be
        // the correct Gaussian dimension to blur them.
    }

    /**
     * Constructs a {@link RenderState} for a single dimensional, directional
     * Gaussian convolution (as for a MotionBlur operation).
     *
     * @param radius the Gaussian radius along the indicated direction
     * @param dx the delta X of the unit vector along which to apply the convolution
     * @param dy the delta Y of the unit vector along which to apply the convolution
     * @param filtertx the transform applied to the filter operation
     */
    public GaussianRenderState(float radius, float dx, float dy, BaseTransform filtertx) {
        // This is a special case of the above 2 dimensional Gaussian, most of
        // the same strategies and caveats apply except as relevant to our
        // directional single-axis peculiarities
        this.isShadow = false;
        this.spread = 0.0f;
        if (filtertx == null) filtertx = BaseTransform.IDENTITY_TRANSFORM;
        double mxx = filtertx.getMxx();
        double mxy = filtertx.getMxy();
        double myx = filtertx.getMyx();
        double myy = filtertx.getMyy();
        // Manually transform the unit vector and determine its added "scale"
        double tdx = mxx * dx + mxy * dy;
        double tdy = myx * dx + myy * dy;
        double txScale = Math.hypot(tdx, tdy);
        boolean scaled = false;
        float scaledRadius = (float) (radius * txScale);
        if (scaledRadius < MIN_EFFECT_RADIUS) {
            this.inputRadiusX = 0.0f;
            this.inputRadiusY = 0.0f;
            this.spreadPass = 0;
            this.space = EffectCoordinateSpace.RenderSpace;
            this.inputtx = filtertx;
            this.resulttx = BaseTransform.IDENTITY_TRANSFORM;
            this.samplevectors = new float[] { 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
        } else {
            if (scaledRadius > MAX_RADIUS) {
                scaledRadius = MAX_RADIUS;
                txScale = MAX_RADIUS / radius;
                scaled = true;
            }
            this.inputRadiusX = scaledRadius;
            this.inputRadiusY = 0.0f;
            this.spreadPass = 0;
            if (scaled) {
                // Since this is a highly directed blur and any change in
                // scaling perpendicular to the blur angle could result in
                // visible artifacts not absorbed by the Gaussian convolution,
                // we will try to focus any changes in intermediate scaling
                // on just that direction that the blur is applied along.
                // We will need to calculate 2 disjoint scale factors, one
                // along the blur (already calculated in txScale) and one
                // perpendicular to that vector, then we will provide the
                // inputs with an animorphically scaled coordinate system
                // that uses a smaller scale along the direction of the blur
                // and as close as possible to the original scale in the
                // orthogonal direction...
                // Determine the orthogonal scale factor:
                double odx = mxy * dx - mxx * dy;
                double ody = myy * dx - myx * dy;
                double txOScale = Math.hypot(odx, ody);
                this.space = EffectCoordinateSpace.CustomSpace;
                Affine2D a2d = new Affine2D();
                a2d.scale(txScale, txOScale);
                a2d.rotate(dx, -dy);
                BaseTransform a2di;
                try {
                    a2di = a2d.createInverse();
                } catch (NoninvertibleTransformException ex) {
                    a2di = BaseTransform.IDENTITY_TRANSFORM;
                }
                this.inputtx = a2d;
                this.resulttx = filtertx
                        .copy()
                        .deriveWithConcatenation(a2di);
                // assert resulttx.deriveWithConcatenation(inputtx).equals(filtertx)
                this.samplevectors = new float[] { 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
            } else {
                this.space = EffectCoordinateSpace.RenderSpace;
                this.inputtx = filtertx;
                this.resulttx = BaseTransform.IDENTITY_TRANSFORM;
                // These values should produce a normalized unit vector in the
                // direction of the transformed sample vector.
                this.samplevectors = new float[] { (float) (tdx / txScale),
                                                   (float) (tdy / txScale),
                                                   0.0f, 0.0f, 0.0f, 0.0f };
            }
        }
        // If the input honors our requested transforms then samplevectors
        // will be the unit vector in the correct direction to sample by
        // pixel distances in the input texture and the inputRadiusX will be
        // the correct Gaussian dimension to blur them.
        // The second vector in samplevectors is ignored since the associated
        // inputRadiusY is hard-coded to 0.
    }

    @Override
    public boolean isShadow() {
        return isShadow;
    }

    @Override
    public Color4f getShadowColor() {
        return shadowColor;
    }

    @Override
    public float[] getPassShadowColorComponents() {
        return (validatedPass == 0)
            ? BLACK_COMPONENTS
            : shadowColor.getPremultipliedRGBComponents();
    }

    @Override
    public EffectCoordinateSpace getEffectTransformSpace() {
        return space;
    }

    @Override
    public BaseTransform getInputTransform(BaseTransform filterTransform) {
        return inputtx;
    }

    @Override
    public BaseTransform getResultTransform(BaseTransform filterTransform) {
        return resulttx;
    }

    @Override
    public Rectangle getInputClip(int i, Rectangle filterClip) {
        if (filterClip != null) {
            double dx0 = samplevectors[0] * inputRadiusX;
            double dy0 = samplevectors[1] * inputRadiusX;
            double dx1 = samplevectors[2] * inputRadiusY;
            double dy1 = samplevectors[3] * inputRadiusY;
            int padx = (int) Math.ceil(dx0+dx1);
            int pady = (int) Math.ceil(dy0+dy1);
            if ((padx | pady) != 0) {
                filterClip = new Rectangle(filterClip);
                filterClip.grow(padx, pady);
            }
        }
        return filterClip;
    }

    @Override
    public ImageData validatePassInput(ImageData src, int pass) {
        this.validatedPass = pass;
        Filterable f = src.getUntransformedImage();
        BaseTransform srcTx = src.getTransform();
        float iRadius = (pass == 0) ? inputRadiusX : inputRadiusY;
        int vecindex = pass * 2;
        if (srcTx.isTranslateOrIdentity()) {
            // The input effect gave us exactly what we wanted, proceed as planned
            this.passRadius = iRadius;
            samplevectors[4] = samplevectors[vecindex];
            samplevectors[5] = samplevectors[vecindex+1];
            if (validatedPass == 0) {
                if ( nearOne(samplevectors[4], f.getPhysicalWidth()) &&
                    nearZero(samplevectors[5], f.getPhysicalWidth()))
                {
                    passType = PassType.HORIZONTAL_CENTERED;
                } else {
                    passType = PassType.GENERAL_VECTOR;
                }
            } else {
                if (nearZero(samplevectors[4], f.getPhysicalHeight()) &&
                     nearOne(samplevectors[5], f.getPhysicalHeight()))
                {
                    passType = PassType.VERTICAL_CENTERED;
                } else {
                    passType = PassType.GENERAL_VECTOR;
                }
            }
        } else {
            // The input produced a texture that requires transformation,
            // reevaluate our radii.
            // First (inverse) transform our sample vectors from the intended
            // srcTx space back into the actual pixel space of the src texture.
            // Then evaluate their length and attempt to absorb as much of any
            // implicit scaling that would happen into our final pixelRadii,
            // but if we overflow the maximum supportable radius then we will
            // just have to sample sparsely with a longer than unit vector.
            // REMIND: we should also downsample the texture by powers of
            // 2 if our sampling will be more sparse than 1 sample per 2
            // pixels.
            passType = PassType.GENERAL_VECTOR;
            try {
                srcTx.inverseDeltaTransform(samplevectors, vecindex, samplevectors, 4, 1);
            } catch (NoninvertibleTransformException ex) {
                this.passRadius = 0.0f;
                samplevectors[4] = samplevectors[5] = 0.0f;
                return src;
            }
            double srcScale = Math.hypot(samplevectors[4], samplevectors[5]);
            float pRad = (float) (iRadius * srcScale);
            if (pRad > MAX_RADIUS) {
                pRad = MAX_RADIUS;
                srcScale = MAX_RADIUS / iRadius;
            }
            this.passRadius = pRad;
            // For a pixelRadius that was less than MAX_RADIUS, the following
            // lines renormalize the un-transformed vectors back into unit
            // vectors in the proper direction and we absorbed their length
            // into the pixelRadius that we will apply for the Gaussian weights.
            // If we clipped the pixelRadius to MAX_RADIUS, then they will not
            // actually end up as unit vectors, but they will represent the
            // proper sampling deltas for the indicated radius (which should
            // be MAX_RADIUS in that case).
            samplevectors[4] /= srcScale;
            samplevectors[5] /= srcScale;
        }
        samplevectors[4] /= f.getPhysicalWidth();
        samplevectors[5] /= f.getPhysicalHeight();
        return src;
    }

    @Override
    public Rectangle getPassResultBounds(Rectangle srcdimension, Rectangle outputClip) {
        // Note that the pass vector and the pass radius may be adjusted for
        // a transformed input, but our output will be in the untransformed
        // "filter" coordinate space so we need to use the "input" values that
        // are in that same coordinate space.
        // The srcdimension is padded by the amount of extra data we produce
        // for this pass.
        // The outputClip is padded by the amount of extra input data we will
        // need for subsequent passes to do their work.
        double r = (validatedPass == 0) ? inputRadiusX : inputRadiusY;
        int i = validatedPass * 2;
        double dx = samplevectors[i+0] * r;
        double dy = samplevectors[i+1] * r;
        int padx = (int) Math.ceil(Math.abs(dx));
        int pady = (int) Math.ceil(Math.abs(dy));
        Rectangle ret = new Rectangle(srcdimension);
        ret.grow(padx, pady);
        if (outputClip != null) {
            if (validatedPass == 0) {
                // Pass 0 needs to retain any added area for Pass 1 to
                // compute the bounds within the outputClip, so we expand
                // the outputClip accordingly.
                dx = samplevectors[2] * r;
                dy = samplevectors[3] * r;
                padx = (int) Math.ceil(Math.abs(dx));
                pady = (int) Math.ceil(Math.abs(dy));
                if ((padx | pady) != 0) {
                    outputClip = new Rectangle(outputClip);
                    outputClip.grow(padx, pady);
                }
            }
            ret.intersectWith(outputClip);
        }
        return ret;
    }

    @Override
    public PassType getPassType() {
        return passType;
    }

    @Override
    public float[] getPassVector() {
        float xoff = samplevectors[4]; // / srcNativeBounds.width;
        float yoff = samplevectors[5]; // / srcNativeBounds.height;
        int ksize = getPassKernelSize();
        int center = ksize / 2;
        float ret[] = new float[4];
        ret[0] = xoff;
        ret[1] = yoff;
        ret[2] = -center * xoff;
        ret[3] = -center * yoff;
        return ret;
    }

    @Override
    public int getPassWeightsArrayLength() {
        validateWeights();
        return weights.limit() / 4;
    }

    @Override
    public FloatBuffer getPassWeights() {
        validateWeights();
        weights.rewind();
        return weights;
    }

    @Override
    public int getInputKernelSize(int pass) {
        return 1 + 2 * (int) Math.ceil((pass == 0) ? inputRadiusX : inputRadiusY);
    }

    @Override
    public int getPassKernelSize() {
        return 1 + 2 * (int) Math.ceil(passRadius);
    }

    @Override
    public boolean isNop() {
        if (isShadow) return false;
        return inputRadiusX < MIN_EFFECT_RADIUS
            && inputRadiusY < MIN_EFFECT_RADIUS;
    }

    @Override
    public boolean isPassNop() {
        if (isShadow && validatedPass == 1) return false;
        return (passRadius) < MIN_EFFECT_RADIUS;
    }

    private void validateWeights() {
        float r = passRadius;
        float s = (validatedPass == spreadPass) ? spread : 0f;
        if (weights == null ||
            weightsValidRadius != r ||
            weightsValidSpread != s)
        {
            weights = getGaussianWeights(weights, (int) Math.ceil(r), r, s);
            weightsValidRadius = r;
            weightsValidSpread = s;
        }
    }
}
