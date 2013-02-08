/*
 * Copyright (c) 2009, 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.nio.FloatBuffer;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Color4f;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;

/**
 * The helper class for defining a 1 dimensional linear convolution kernel
 * for either the LinearConvolve or LinearConvolveShadow shaders.
 * This class is abstract and must be subclassed for specific linear
 * convolutions.
 */
public abstract class LinearConvolveKernel {
    public static final int MAX_KERNEL_SIZE = 128;

    public enum PassType {
        /**
         * The kernel on this pass will be applied horizontally with
         * the kernel centered symmetrically around each pixel.
         * The specific conditions indicated by this type are:
         * <ul>
         * <li>The kernel is an odd size {@code (2*k+1)}
         * <li>The data for destination pixel {@code (x,y)} is taken from
         *     pixels {@code x-k,y} through {@code (x+k,y)} with the weights
         *     applied in that same order.
         * <li>If the bounds of the source image are {@code (x,y,w,h)} then
         *     the bounds of the destination will be {@code (x-k,y,w+2*k,h)}.
         * </ul>
         */
        HORIZONTAL_CENTERED,

        /**
         * The kernel on this pass will be applied vertically with
         * the kernel centered symmetrically around each pixel.
         * The specific conditions indicated by this type are:
         * <ul>
         * <li>The kernel is an odd size {@code (2*k+1)}
         * <li>The data for destination pixel {@code (x,y)} is taken from
         *     pixels {@code x,y-k} through {@code (x,y+k)} with the weights
         *     applied in that same order.
         * <li>If the bounds of the source image are {@code (x,y,w,h)} then
         *     the bounds of the destination will be {@code (x,y-k,w,h+2*k)}.
         * </ul>
         */
        VERTICAL_CENTERED,

        /**
         * The kernel on this pass can be applied in any direction or with
         * any kind of offset.
         * No assumptions are made about the offset and delta of the kernel
         * vector.
         */
        GENERAL_VECTOR,
    };

    /**
     * Returns the peer sample count for a given kernel size.  There are
     * only a few peers defined to operate on specific sizes of convolution
     * kernel.  If there are peers defined only for kernel sizes of 8 and 16
     * and a given effect has a linear convolution kernel with 5 weights,
     * then the peer for size 8 will be used and the buffer of weights must
     * be padded out to the appropriate size with 0s so that the shader
     * constant pool will be fully initialized and the extra unneeded
     * convolution samples will be ignored by the 0 weights.
     * 
     * @param ksize the number of computed convolution kernel weights
     * @return the number of convolution weights which will be applied by
     *         the associated peer.
     */
    public static int getPeerSize(int ksize) {
        if (ksize < 32) return ((ksize + 3) & (~3));
        if (ksize <= MAX_KERNEL_SIZE) return ((ksize + 31) & (~31));
        throw new RuntimeException("No peer available for kernel size: "+ksize);
    }

    /**
     * Returns true if this is a LinearConvolveShadow operation, or false
     * if the operation is a regular LinearConvolve.
     *
     * @return true if this is a Shadow operation
     */
    public boolean isShadow() {
        return false;
    }

    /**
     * Returns the number of linear convolution passes the algorithm must make
     * to complete its work.  Most subclasses will use only 1 or 2 passes
     * (typically broken down into a horizontal pass and a vertical pass as
     * necessary).
     *
     * @return the number of passes to be made
     */
    public abstract int getNumberOfPasses();

    /**
     * Returns true if the entire operation of this linear convolution
     * would have no effect on the source data.
     * 
     * @return true if the operation is a NOP
     */
    public boolean isNop() {
        return false;
    }

    /**
     * Returns true if the operation of a particular pass of this linear
     * convolution would have no effect on the source data.
     *
     * @param pass the algorithm pass being performed
     * @return true if the given pass is a NOP
     */
    public boolean isNop(int pass) {
        return false;
    }

    /**
     * Returns the {@link PassType} that indicates the assumptions that
     * can be made in optimizing the application of the kernel on this
     * pass.
     * 
     * @param pass the algorithm pass being performed
     * @return the {@link PassType} that describes the kernel vector for
     *         this pass
     */
    public PassType getPassType(int pass) {
        return PassType.GENERAL_VECTOR;
    }

    /**
     * Returns the size of the output image needed for a given input
     * image dimensions and a given pass of the algorithm.
     * 
     * @param srcdimension the bounds of the input image
     * @param pass the algorithm pass being performed
     * @return the bounds of the result image
     */
    public abstract Rectangle getResultBounds(Rectangle srcdimension, int pass);

    /**
     * Returns the size of the scaled result image needed to hold the output
     * for a given input image dimensions and a given pass of the algorithm.
     * The image may be further scaled after the shader operation is through
     * to obtain the final result bounds.
     * This value is only of use to the actual shader to understand exactly
     * how much room to allocate for the shader result.
     *
     * @param srcdimension the bounds of the input image
     * @param pass the algorithm pass being performed
     * @return the bounds of the result image
     */
    public Rectangle getScaledResultBounds(Rectangle srcdimension, int pass) {
        return getResultBounds(srcdimension, pass);
    }

    /**
     * Returns an array of 4 floats used to initialize a float4 Shader
     * constant with the relative starting location of the first weight
     * in the convolution kernel and the incremental offset between each
     * sample to be weighted and convolved.  The values are stored in
     * the array in the following order:
     * <pre>
     *     shadervec.x = vector[0] = incdx // X offset between subsequent samples
     *     shadervec.y = vector[1] = incdy // Y offset between subsequent samples
     *     shadervec.z = vector[2] = startdx // X offset to first convolution sample
     *     shadervec.w = vector[3] = startdy // Y offset to first convolution sample
     * </pre>
     * These values are used in the shader loop as follows:
     * <pre>
     *     samplelocation = outputpixellocation.xy + shadervec.zw;
     *     for (each weight) {
     *         sum += weight * sample(samplelocation.xy);
     *         samplelocation.xy += shadervec.xy;
     *     }
     *
     * @param srcnativedimensions the native dimensions (including unused
     *                            padding) of the input source
     * @param pass the pass of the algorithm being performed
     * @return an array of 4 floats representing
     *         {@code [ incdx, incdy, startdx, startdy ]}
     */
    public abstract float[] getVector(Rectangle srcnativedimensions,
                                      BaseTransform transform, int pass);

    /**
     * Returns the size of the kernel for a given pass.
     *
     * @param pass the pass of the algorithm being performed
     * @return the size of the kernel for the given pass
     */
    public abstract int getKernelSize(int pass);

    /**
     * Returns the size of the kernel used in the shader for a given pass,
     * taking into account the scaling specified by the getPow2ScaleXY methods.
     *
     * @param pass the pass of the algorithm being performed
     * @return the size of the kernel for the scaled operation
     */
    public int getScaledKernelSize(int pass) {
        return getKernelSize(pass);
    }

    /**
     * Returns the number of power of 2 scales along the X axis.
     * Positive numbers mean to scale the image larger by the indicated
     * factors of 2.0.
     * Negative numbers mean to scale the image smaller by the indicated
     * factors of 0.5.
     * Overall the image will be scaled by {@code pow(2.0, getPow2ScaleX())}.
     * <p>
     * The kernel specified by the {@link #getWeights()} method will be
     * relative to the scale factor recommended by this method.
     * This scaling allows larger kernels to be reduced in size to save
     * computation if the resolution reduction will not alter the quality
     * of the convolution (eg. for blur convolutions).
     *
     * @return the power of 2.0 by which to scale the source image along the
     *         X axis.
     */
    public int getPow2ScaleX() {
        return 0;
    }

    /**
     * Returns the number of power of 2 scales along the Y axis.
     * Positive numbers mean to scale the image larger by the indicated
     * factors of 2.0.
     * Negative numbers mean to scale the image smaller by the indicated
     * factors of 0.5.
     * Overall the image will be scaled by {@code pow(2.0, getPow2ScaleY())}.
     * <p>
     * The kernel specified by the {@link #getWeights()} method will be
     * relative to the scale factor recommended by this method.
     * This scaling allows larger kernels to be reduced in size to save
     * computation if the resolution reduction will not alter the quality
     * of the convolution (eg. for blur convolutions).
     *
     * @return the power of 2.0 by which to scale the source image along the
     *         Y axis.
     */
    public int getPow2ScaleY() {
        return 0;
    }

    /**
     * A {@link FloatBuffer} padded out to the required size as specified by
     * the {@link #getPeerSize()} method.
     *
     * @param pass the pass of the algorithm being performed
     * @return a {@code FloatBuffer} containing the kernel convolution weights
     */
    public abstract FloatBuffer getWeights(int pass);

    /**
     * Returns the maximum number of valid float4 elements that should be
     * referenced from the buffer returned by getWeights() for the given pass.
     *
     * @param pass the pass of the algorithm being performed
     * @return the maximum number of valid float4 elements in the weights buffer
     */
    public int getWeightsArrayLength(int pass) {
        int ksize = getScaledKernelSize(pass);
        int psize = getPeerSize(ksize);
        return psize / 4;
    }

    final static float[] BLACK_COMPONENTS =
        Color4f.BLACK.getPremultipliedRGBComponents();

    /**
     * Returns the color components to be used for a linearly convolved shadow.
     * Only the LinearConvolveShadow shader uses this method.  State
     * subclasses that are only intended to be used with the LinearConvolve
     * shader do not need to override this method.
     *
     * @param pass the pass of the algorithm being performed
     * @return the color components for the shadow color for the given pass
     */
    public float[] getShadowColorComponents(int pass) {
        return BLACK_COMPONENTS;
    }

    public EffectPeer getPeer(Renderer r, FilterContext fctx, int pass) {
        if (isNop(pass)) {
            return null;
        }
        int ksize = getScaledKernelSize(pass);
        int psize = getPeerSize(ksize);
        String opname = isShadow() ? "LinearConvolveShadow" : "LinearConvolve";
        return r.getPeerInstance(fctx, opname, psize);
    }

    public Rectangle transform(Rectangle clip,
                               int xpow2scales, int ypow2scales)
    {
        // Modeled after Renderer.transform(fctx, img, hscale, vscale)
        if (clip == null || (xpow2scales | ypow2scales) == 0) {
            return clip;
        }
        clip = new Rectangle(clip);
        if (xpow2scales < 0) {
            xpow2scales = -xpow2scales;
            clip.width = (clip.width + (1 << xpow2scales) - 1) >> xpow2scales;
            clip.x >>= xpow2scales;
        } else if (xpow2scales > 0) {
            clip.width = clip.width << xpow2scales;
            clip.x <<= xpow2scales;
        }
        if (ypow2scales < 0) {
            ypow2scales = -ypow2scales;
            clip.height = (clip.height + (1 << ypow2scales) - 1) >> ypow2scales;
            clip.y >>= ypow2scales;
        } else if (ypow2scales > 0) {
            clip.height = clip.height << ypow2scales;
            clip.y <<= ypow2scales;
        }
        return clip;
    }

    public ImageData filterImageDatas(Effect effect,
                                      FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      ImageData... inputs)
    {
        ImageData src = inputs[0];
        src.addref();
        if (isNop()) {
            return src;
        }
        Rectangle approxBounds = inputs[0].getUntransformedBounds();
        int approxW = approxBounds.width;
        int approxH = approxBounds.height;
        Renderer r = Renderer.getRenderer(fctx, effect, approxW, approxH);
        EffectPeer peer0 = getPeer(r, fctx, 0);
        EffectPeer peer1 = getPeer(r, fctx, 1);
        int hscale = 0;
        int vscale = 0;
        if (peer0 instanceof LinearConvolvePeer) {
            hscale = ((LinearConvolvePeer) peer0).getPow2ScaleX(this);
        }
        if (peer1 instanceof LinearConvolvePeer) {
            vscale = ((LinearConvolvePeer) peer1).getPow2ScaleY(this);
        }
        Rectangle filterClip = outputClip;
        if ((hscale | vscale) != 0) {
            src = r.transform(fctx, src, hscale, vscale);
            if (!src.validate(fctx)) {
                src.unref();
                return src;
            }
            filterClip = transform(outputClip, hscale, vscale);
        }
        if (filterClip != null) {
            // The inputClip was already grown by the padding when the
            // inputs were filtered, but now we need to make sure that
            // the peers pass out padded results from the already padded
            // input data, so we grow the clip here by the size of the
            // scaled kernel padding.
            int hgrow = getScaledKernelSize(0) / 2;
            int vgrow = getScaledKernelSize(1) / 2;
            if ((hgrow | vgrow) != 0) {
                if (filterClip == outputClip) {
                    filterClip = new Rectangle(outputClip);
                }
                filterClip.grow(hgrow, vgrow);
            }
        }
        if (peer0 != null) {
            peer0.setPass(0);
            ImageData res = peer0.filter(effect, transform, filterClip, src);
            src.unref();
            src = res;
            if (!src.validate(fctx)) {
                src.unref();
                return src;
            }
        }

        if (peer1 != null) {
            peer1.setPass(1);
            ImageData res = peer1.filter(effect, transform, filterClip, src);
            src.unref();
            src = res;
            if (!src.validate(fctx)) {
                src.unref();
                return src;
            }
        }

        if ((hscale | vscale) != 0) {
            src = r.transform(fctx, src, -hscale, -vscale);
        }
        return src;
    }
}
