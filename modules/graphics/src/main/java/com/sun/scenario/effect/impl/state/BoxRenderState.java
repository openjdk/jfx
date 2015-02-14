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
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.scenario.effect.Color4f;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.BufferUtil;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;
import java.nio.FloatBuffer;

/**
 * The RenderState for a box filter kernel that can be applied using a
 * standard linear convolution kernel.
 * A box filter has a size that represents how large of an area around a
 * given pixel should be averaged.  If the size is 1.0 then just the pixel
 * itself should be averaged and the operation is a NOP.  Values smaller
 * than that are automatically treated as 1.0/NOP.
 * For any odd size, the kernel weights the center pixel and an equal number
 * of pixels on either side of it equally, so the weights for size 2N+1 are:
 * [ {N copes of 1.0} 1.0 {N more copies of 1.0} ]
 * As the size grows past that integer size, we must then add another kernel
 * weight entry on both sides of the existing array of 1.0 weights and give
 * them a fractional weight of half of the amount we exceeded the last odd
 * size, so the weights for some size (2N+1)+e (e for epsilon) are:
 * [ e/2.0 {2*N+1 copies of 1.0} e/2.0 ]
 * As the size continues to grow, when it reaches the next even size, we get
 * weights for size 2*N+1+1 to be:
 * [ 0.5 {2*N+1 copies of 1.0} 0.5 ]
 * and as the size continues to grow and approaches the next odd number, we
 * see that 2(N+1)+1 == 2N+2+1 == 2N+1 + 2, so (e) approaches 2 and the
 * numbers on each end of the weights array approach e/2.0 == 1.0 and we end
 * up back at the pattern for an odd size again:
 * [ 1.0 {2*N+1 copies of 1.0} 1.0 ]
 * 
 * ***************************
 * SOFTWARE LIMITATION CAVEAT:
 * ***************************
 * 
 * Note that the highly optimized software filters for BoxBlur/Shadow will
 * actually do a very optimized "running sum" operation that is only currently
 * implemented for equal weighted kernels.  Also, until recently we had always
 * been rounding down the size by casting it to an integer at a high level (in
 * the FX layer peer synchronization code), so for now the software filters
 * may only implement a subset of the above theory and new optimized loops that
 * allow partial sums on the first and last values will need to be written.
 * Until then we will be rounding the sizes to an odd size, but only in the
 * sw loops.
 */
public class BoxRenderState extends LinearConvolveRenderState {
    private static final int MAX_BOX_SIZES[] = {
        getMaxSizeForKernelSize(MAX_KERNEL_SIZE, 0),
        getMaxSizeForKernelSize(MAX_KERNEL_SIZE, 1),
        getMaxSizeForKernelSize(MAX_KERNEL_SIZE, 2),
        getMaxSizeForKernelSize(MAX_KERNEL_SIZE, 3),
    };

    private final boolean isShadow;
    private final int blurPasses;
    private final float spread;
    private Color4f shadowColor;

    private EffectCoordinateSpace space;
    private BaseTransform inputtx;
    private BaseTransform resulttx;
    private final float inputSizeH;
    private final float inputSizeV;
    private final int spreadPass;
    private float samplevectors[];

    private int validatedPass;
    private float passSize;
    private FloatBuffer weights;
    private float weightsValidSize;
    private float weightsValidSpread;
    private boolean swCompatible;  // true if we can use the sw peers

    public static int getMaxSizeForKernelSize(int kernelSize, int blurPasses) {
        if (blurPasses == 0) {
            return Integer.MAX_VALUE;
        }
        // Kernel sizes are always odd, so if the supplied ksize is even then
        // we need to use ksize-1 to compute the max as that is actually the
        // largest kernel we will be able to produce that is no larger than
        // ksize for any given pass size.
        int passSize = (kernelSize - 1) | 1;
        passSize = ((passSize - 1) / blurPasses) | 1;
        assert getKernelSize(passSize, blurPasses) <= kernelSize;
        return passSize;
    }

    public static int getKernelSize(int passSize, int blurPasses) {
        int kernelSize = (passSize < 1) ? 1 : passSize;
        kernelSize = (kernelSize-1) * blurPasses + 1;
        kernelSize |= 1;
        return kernelSize;
    }

    public BoxRenderState(float hsize, float vsize, int blurPasses, float spread,
                          boolean isShadow, Color4f shadowColor, BaseTransform filtertx)
    {
        /*
         * The operation starts as a description of the size of a (pair of)
         * box filter kernels measured relative to that user space coordinate
         * system and to be applied horizontally and vertically in that same
         * space.  The presence of a filter transform can mean that the
         * direction we apply the box convolutions could change as well
         * as the new size of the box summations relative to the pixels
         * produced under that transform.
         * 
         * Since the box filter is best described by the summation of a range
         * of discrete pixels horizontally and vertically, and since the
         * software algorithms vastly prefer applying the sums horizontally
         * and vertically to groups of whole pixels using an incremental "add
         * the next pixel at the front edge of the box and subtract the pixel
         * that is at the back edge of the box" technique, we will constrain
         * our box size to an integer size and attempt to force the inputs
         * to produce an axis aligned intermediate image.  But, in the end,
         * we must be prepared for an arbitrary transform on the input image
         * which essentially means being able to back off to an arbitrary
         * invocation on the associated LinearConvolvePeer from the software
         * hand-written Box peers.
         * 
         * We will track the direction and size of the box as we traverse
         * different coordinate spaces with the intent that eventually we
         * will perform the math of the convolution with weights calculated
         * for one sample per pixel in the indicated direction and applied as
         * closely to the intended final filter transform as we can achieve
         * with the following caveats (very similar to the caveats for the
         * more general GaussianRenderState):
         * 
         * - There is a maximum kernel size that the hardware pixel shaders
         *   can apply so we will try to keep the scaling of the filtered
         *   pixels low enough that we do not exceed that data limitation.
         * 
         * - Software vastly prefers to apply these weights along horizontal
         *   and vertical vectors, but can apply them in an arbitrary direction
         *   if need be by backing off to the generic LinearConvolvePeer.
         * 
         * - If the box is large enough, then applying a smaller box kernel
         *   to a downscaled input is close enough to applying the larger box
         *   to a larger scaled input.  Our maximum kernel size is large enough
         *   for this effect to be hidden if we max out the kernel.
         * 
         * - We can tell the inputs what transform we want them to use, but
         *   they can always produce output under a different transform and
         *   then return a result with a "post-processing" trasnform to be
         *   applied (as we are doing here ourselves).  Thus, we can plan
         *   how we want to apply the convolution weights and samples here,
         *   but we will have to reevaluate our actions when the actual
         *   input pixels are created later.
         * 
         * - We will try to blur at a nice axis-aligned orientation (which is
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
        this.blurPasses = blurPasses;
        if (filtertx == null) filtertx = BaseTransform.IDENTITY_TRANSFORM;
        double txScaleX = Math.hypot(filtertx.getMxx(), filtertx.getMyx());
        double txScaleY = Math.hypot(filtertx.getMxy(), filtertx.getMyy());
        float fSizeH = (float) (hsize * txScaleX);
        float fSizeV = (float) (vsize * txScaleY);
        int maxPassSize = MAX_BOX_SIZES[blurPasses];
        if (fSizeH > maxPassSize) {
            txScaleX = maxPassSize / hsize;
            fSizeH = maxPassSize;
        }
        if (fSizeV > maxPassSize) {
            txScaleY = maxPassSize / vsize;
            fSizeV = maxPassSize;
        }
        this.inputSizeH = fSizeH;
        this.inputSizeV = fSizeV;
        this.spreadPass = (fSizeV > 1) ? 1 : 0;
        // We always want to use an unrotated space to do our filtering, so
        // we interpose our scaled-only space in all cases, but we do check
        // if it happens to be equivalent (ignoring translations) to the
        // original filtertx so we can avoid introducing extra layers of
        // transforms.
        boolean custom = (txScaleX != filtertx.getMxx() ||
                          0.0      != filtertx.getMyx() ||
                          txScaleY != filtertx.getMyy() ||
                          0.0      != filtertx.getMxy());
        if (custom) {
            this.space = EffectCoordinateSpace.CustomSpace;
            this.inputtx = BaseTransform.getScaleInstance(txScaleX, txScaleY);
            this.resulttx = filtertx
                .copy()
                .deriveWithScale(1.0 / txScaleX, 1.0 / txScaleY, 1.0);
        } else {
            this.space = EffectCoordinateSpace.RenderSpace;
            this.inputtx = filtertx;
            this.resulttx = BaseTransform.IDENTITY_TRANSFORM;
        }
        // assert inputtx.mxy == inputtx.myx == 0.0
    }

    public int getBoxPixelSize(int pass) {
        float size = passSize;
        if (size < 1.0f) size = 1.0f;
        int boxsize = ((int) Math.ceil(size)) | 1;
        return boxsize;
    }

    public int getBlurPasses() {
        return blurPasses;
    }

    public float getSpread() {
        return spread;
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
    public EffectPeer<BoxRenderState> getPassPeer(Renderer r, FilterContext fctx) {
        if (isPassNop()) {
            return null;
        }
        int ksize = getPassKernelSize();
        int psize = getPeerSize(ksize);
        Effect.AccelType actype = r.getAccelType();
        String name;
        switch (actype) {
            case NONE:
            case SIMD:
                if (swCompatible && spread == 0.0f) {
                    name = isShadow() ? "BoxShadow" : "BoxBlur";
                    break;
                }
                /* FALLS THROUGH */
            default:
                name = isShadow() ? "LinearConvolveShadow" : "LinearConvolve";
                break;
        }
        EffectPeer peer = r.getPeerInstance(fctx, name, psize);
        return peer;
    }

    @Override
    public Rectangle getInputClip(int i, Rectangle filterClip) {
        if (filterClip != null) {
            int klenh = getInputKernelSize(0);
            int klenv = getInputKernelSize(1);
            if ((klenh | klenv) > 1) {
                filterClip = new Rectangle(filterClip);
                // We actually want to grow them by (klen-1)/2, but since we
                // have forced the klen sizes to be odd above, a simple integer
                // divide by 2 is enough...
                filterClip.grow(klenh/2, klenv/2);
            }
        }
        return filterClip;
    }

    @Override
    public ImageData validatePassInput(ImageData src, int pass) {
        this.validatedPass = pass;
        BaseTransform srcTx = src.getTransform();
        samplevectors = new float[2];
        samplevectors[pass] = 1.0f;
        float iSize = (pass == 0) ? inputSizeH : inputSizeV;
        if (srcTx.isTranslateOrIdentity()) {
            this.swCompatible = true;
            this.passSize = iSize;
        } else {
            // The input produced a texture that requires transformation,
            // reevaluate our box sizes.
            // First (inverse) transform our sample vectors from the intended
            // srcTx space back into the actual pixel space of the src texture.
            // Then evaluate their length and attempt to absorb as much of any
            // implicit scaling that would happen into our final pixelSizes,
            // but if we overflow the maximum supportable pass size then we will
            // just have to sample sparsely with a longer than unit vector.
            // REMIND: we should also downsample the texture by powers of
            // 2 if our sampling will be more sparse than 1 sample per 2
            // pixels.
            try {
                srcTx.inverseDeltaTransform(samplevectors, 0, samplevectors, 0, 1);
            } catch (NoninvertibleTransformException ex) {
                this.passSize = 0.0f;
                samplevectors[0] = samplevectors[1] = 0.0f;
                this.swCompatible = true;
                return src;
            }
            double srcScale = Math.hypot(samplevectors[0], samplevectors[1]);
            float pSize = (float) (iSize * srcScale);
            pSize *= srcScale;
            int maxPassSize = MAX_BOX_SIZES[blurPasses];
            if (pSize > maxPassSize) {
                pSize = maxPassSize;
                srcScale = maxPassSize / iSize;
            }
            this.passSize = pSize;
            // For a pixelSize that was less than maxPassSize, the following
            // lines renormalize the un-transformed vector back into a unit
            // vector in the proper direction and we absorbed its length
            // into the pixelSize that we will apply for the box filter weights.
            // If we clipped the pixelSize to maxPassSize, then it will not
            // actually end up as a unit vector, but it will represent the
            // proper sampling deltas for the indicated box size (which should
            // be maxPassSize in that case).
            samplevectors[0] /= srcScale;
            samplevectors[1] /= srcScale;
            // If we are still sampling by an axis aligned unit vector, then the
            // optimized software filters can still do their "incremental sum"
            // magic.
            // REMIND: software loops could actually do an infinitely sized
            // kernel with only memory requirements getting in the way, but
            // the values being tested here are constrained by the limits of
            // the hardware peers.  It is not clear how to fix this since we
            // have to choose how to proceed before we have enough information
            // to know if the inputs will be cooperative enough to assume
            // software limits, and then once we get here, we may have already
            // constrained ourselves into a situation where we must use the
            // hardware peers.  Still, there may be more "fighting" we can do
            // to hold on to compatibility with the software loops perhaps?
            Rectangle srcSize = src.getUntransformedBounds();
            if (pass == 0) {
                this.swCompatible = nearOne(samplevectors[0], srcSize.width)
                                && nearZero(samplevectors[1], srcSize.width);
            } else {
                this.swCompatible = nearZero(samplevectors[0], srcSize.height)
                                  && nearOne(samplevectors[1], srcSize.height);
            }
        }
        Filterable f = src.getUntransformedImage();
        samplevectors[0] /= f.getPhysicalWidth();
        samplevectors[1] /= f.getPhysicalHeight();
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
        Rectangle ret = new Rectangle(srcdimension);
        if (validatedPass == 0) {
            ret.grow(getInputKernelSize(0) / 2, 0);
        } else {
            ret.grow(0, getInputKernelSize(1) / 2);
        }
        if (outputClip != null) {
            if (validatedPass == 0) {
                outputClip = new Rectangle(outputClip);
                outputClip.grow(0, getInputKernelSize(1) / 2);
            }
            ret.intersectWith(outputClip);
        }
        return ret;
    }

    @Override
    public float[] getPassVector() {
        float xoff = samplevectors[0];
        float yoff = samplevectors[1];
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

    private void validateWeights() {
        float pSize;
        if (blurPasses == 0) {
            pSize = 1.0f;
        } else {
            pSize = passSize;
            // 1.0f is the minimum size and is a NOP (each pixel averaged
            // over itself)
            if (pSize < 1.0f) pSize = 1.0f;
        }
        float passSpread = (validatedPass == spreadPass) ? spread : 0f;
        if (weights != null &&
            weightsValidSize == pSize &&
            weightsValidSpread == passSpread)
        {
            return;
        }

        // round klen up to a full pixel size and make sure it is odd so
        // that we center the kernel around each pixel center (1.0 of the
        // total size/weight is centered on the current pixel and then
        // the remainder is split (size-1.0)/2 on each side.
        // If the size is 2, then we don't want to average each pair of
        // pixels together (weights: 0.5, 0.5), instead we want to take each
        // pixel and average in half of each of its neighbors with it
        // (weights: 0.25, 0.5, 0.25).
        int klen = ((int) Math.ceil(pSize)) | 1;
        int totalklen = klen;
        for (int p = 1; p < blurPasses; p++) {
            totalklen += klen - 1;
        }
        double ik[] = new double[totalklen];
        for (int i = 0; i < klen; i++) {
            ik[i] = 1.0;
        }
        // The sum of the ik[] array is now klen, but we want the sum to
        // be size.  The worst case difference will be less than 2.0 since
        // the klen length is the ceil of the actual size possibly bumped up
        // to an odd number.  Thus it can have been bumped up by no more than
        // 2.0. If there is an excess, we need to take half of it out of each
        // of the two end weights (first and last).
        double excess = klen - pSize;
        if (excess > 0.0) {
            // assert (excess * 0.5 < 1.0)
            ik[0] = ik[klen-1] = 1.0 - excess * 0.5;
        }
        int filledklen = klen;
        for (int p = 1; p < blurPasses; p++) {
            filledklen += klen - 1;
            int i = filledklen - 1;
            while (i > klen) {
                double sum = ik[i];
                for (int k = 1; k < klen; k++) {
                    sum += ik[i-k];
                }
                ik[i--] = sum;
            }
            while (i > 0) {
                double sum = ik[i];
                for (int k = 0; k < i; k++) {
                    sum += ik[k];
                }
                ik[i--] = sum;
            }
        }
        // assert (filledklen == totalklen == ik.length)
        double sum = 0.0;
        for (int i = 0; i < ik.length; i++) {
            sum += ik[i];
        }
        // We need to apply the spread on only one pass
        // Prefer pass1 if r1 is not trivial
        // Otherwise use pass 0 so that it doesn't disappear
        sum += (1.0 - sum) * passSpread;

        if (weights == null) {
            // peersize(MAX_KERNEL_SIZE) rounded up to the next multiple of 4
            int maxbufsize = getPeerSize(MAX_KERNEL_SIZE);
            maxbufsize = (maxbufsize + 3) & (~3);
            weights = BufferUtil.newFloatBuffer(maxbufsize);
        }
        weights.clear();
        for (int i = 0; i < ik.length; i++) {
            weights.put((float) (ik[i] / sum));
        }
        int limit = getPeerSize(ik.length);
        while (weights.position() < limit) {
            weights.put(0f);
        }
        weights.limit(limit);
        weights.rewind();
    }

    @Override
    public int getInputKernelSize(int pass) {
        float size = (pass == 0) ? inputSizeH : inputSizeV;
        if (size < 1.0f) size = 1.0f;
        int klen = ((int) Math.ceil(size)) | 1;
        int totalklen = 1;
        for (int p = 0; p < blurPasses; p++) {
            totalklen += klen - 1;
        }
        return totalklen;
    }

    @Override
    public int getPassKernelSize() {
        float size = passSize;
        if (size < 1.0f) size = 1.0f;
        int klen = ((int) Math.ceil(size)) | 1;
        int totalklen = 1;
        for (int p = 0; p < blurPasses; p++) {
            totalklen += klen - 1;
        }
        return totalklen;
    }

    @Override
    public boolean isNop() {
        if (isShadow) return false;
        return (blurPasses == 0
                || (inputSizeH <= 1.0f && inputSizeV <= 1.0f));
    }

    @Override
    public boolean isPassNop() {
        if (isShadow && validatedPass == 1) return false;
        return (blurPasses == 0 || (passSize) <= 1.0f);
    }
}
