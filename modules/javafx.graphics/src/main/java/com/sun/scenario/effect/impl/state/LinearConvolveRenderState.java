/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.geom.Rectangle;
import com.sun.scenario.effect.Color4f;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;
import java.nio.FloatBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * The {@code LinearConvolveRenderState} object manages the strategies of
 * applying a 1 or 2 pass linear convolution to an input and calculates the
 * necessary data for the filter shader to compute the convolution.
 * The object is constructed based on the transform that was provided for
 * the entire filter operation and determines its strategy.
 * Methods prefixed by {@code getInput*()} return information about the
 * general plan for obtaining and managing the input source image.
 * After the input effect is called with the information from the
 * {@code getInput*()} methods and its result {@code ImageData} is obtained,
 * the {@code validatePassInput()} method is used to examine the size and
 * transform of the supplied input and determine the parameters needed to
 * perform the convolution for the first pass.
 * Once validated, the methods prefixed by {@code getPass*()} return information
 * for applying the convolution for that validated pass.
 * If necessary, the {@code validatePassInput()} method is called on the
 * results of the first pass to calculate further data for the second pass.
 * Finally the {@code getResultTransform()} method is used to possibly transform
 * the final resulting {@code ImageData} of the last pass.
 */
public abstract class LinearConvolveRenderState implements RenderState {
    public static final int MAX_COMPILED_KERNEL_SIZE = 128;
    public static final int MAX_KERNEL_SIZE;

    static final float MIN_EFFECT_RADIUS = 1.0f / 256.0f;

    static final float[] BLACK_COMPONENTS =
        Color4f.BLACK.getPremultipliedRGBComponents();

    static {
        /*
         * Set the maximum linear convolve kernel size used in LinearConvolveRenderState.
         * The default value is set to 64 if platform is an embedded system and 128 otherwise.
         */
        final int defSize = PlatformUtil.isEmbedded() ? 64 : MAX_COMPILED_KERNEL_SIZE;
        @SuppressWarnings("removal")
        int size = AccessController.doPrivileged(
                (PrivilegedAction<Integer>) () -> Integer.getInteger(
                        "decora.maxLinearConvolveKernelSize", defSize));
        if (size > MAX_COMPILED_KERNEL_SIZE) {
            System.out.println("Clamping maxLinearConvolveKernelSize to "
                    + MAX_COMPILED_KERNEL_SIZE);
            size = MAX_COMPILED_KERNEL_SIZE;
        }
        MAX_KERNEL_SIZE = size;
    }

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
    }

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
     * Returns true if summing v over size pixels ends up close enough to
     * 0.0 that we will not have shifted the sampling by enough to see any
     * changes.
     * "Close enough" in this context is measured by whether or not using
     * the coordinate in a linear interpolating sampling operation on 8-bit
     * per sample images will cause the next pixel over to be blended in.
     *
     * @param v the value being summed across the pixels
     * @param size the number of pixels being summed across
     * @return true if the accumulated value will be negligible
     */
    static boolean nearZero(float v, int size) {
        return (Math.abs(v * size) < 1.0/512.0);
    }

    /**
     * Returns true if summing v over size pixels ends up close enough to
     * size.0 that we will not have shifted the sampling by enough to see any
     * changes.
     * "Close enough" in this context is measured by whether or not using
     * the coordinate in a linear interpolating sampling operation on 8-bit
     * per sample images will cause the next pixel over to be blended in.
     *
     * @param v the value being summed across the pixels
     * @param size the number of pixels being summed across
     * @return true if the accumulated value will be close enough to size
     */
    static boolean nearOne(float v, int size) {
        return (Math.abs(v * size - size) < 1.0/512.0);
    }

    /**
     * Returns true if this is a shadow convolution operation where a
     * constant color is substituted for the color components of the
     * output.
     * This value is dependent only on the original {@code Effect} from which
     * this {@code RenderState} was instantiated and does not vary as the
     * filter operation progresses.
     *
     * @return true if this is a shadow operation
     */
    public abstract boolean isShadow();

    /**
     * Returns the {@code Color4f} representing the shadow color if this
     * is a shadow operation.
     * This value is dependent only on the original {@code Effect} from which
     * this {@code RenderState} was instantiated and does not vary as the
     * filter operation progresses.
     *
     * @return the {@code Color4f} for the shadow color, or null
     */
    public abstract Color4f getShadowColor();

    /**
     * Returns the size of the desired convolution kernel for the given pass
     * as it would be applied in the coordinate space indicated by the
     * {@link #getInputKernelSize(int)} method.
     * This value is calculated at the start of the render operation and
     * does not vary as the filter operation progresses, but it may not
     * represent the actual kernel size used when the indicated pass actually
     * occurs if the {@link #validatePassInput()} method needs to choose
     * different values when it sees the incoming image source.
     *
     * @param pass the pass for which the intended kernel size is desired
     * @return the intended kernel size for the requested pass
     */
    public abstract int getInputKernelSize(int pass);

    /**
     * Returns true if the resulting operation is globally a NOP operation.
     * This condition is calculated at the start of the render operation and
     * is based on whether the perturbations of the convolution kernel would
     * be noticeable at all in the coordinate space of the output.
     *
     * @return true if the operation is a global NOP
     */
    public abstract boolean isNop();

    /**
     * Validates the {@code RenderState} object for a given pass of the
     * convolution.
     * The supplied source image is provided so that the {@code RenderState}
     * object can determine if it needs to change its strategy for how the
     * convolution operation will be performed and to scale its data for
     * the {@code getPass*()} methods relative to the source dimensions and
     * transform.
     *
     * @param src the {@code ImageData} object supplied by the source effect
     * @param pass the pass of the operation being applied (usually horizontal
     *             for pass 0 and vertical for pass 1)
     * @return the {@code ImageData} to be used for the actual convolution
     *         operation
     */
    public abstract ImageData validatePassInput(ImageData src, int pass);

    /**
     * Returns true if the operation of the currently validated pass would
     * be a NOP operation.
     *
     * @return true if the current pass is a NOP
     */
    public abstract boolean isPassNop();

    /**
     * Return the {@code EffectPeer} to be used to perform the currently
     * validated pass of the convolution operation, or null if this pass
     * is a NOP.
     *
     * @param r the {@code Renderer} being used for this filter operation
     * @param fctx the {@code FilterContext} being used for this filter operation
     * @return the {@code EffectPeer} to use for this pass, or null
     */
    public EffectPeer<? extends LinearConvolveRenderState>
        getPassPeer(Renderer r, FilterContext fctx)
    {
        if (isPassNop()) {
            return null;
        }
        int ksize = getPassKernelSize();
        int psize = getPeerSize(ksize);
        String opname = isShadow() ? "LinearConvolveShadow" : "LinearConvolve";
        return r.getPeerInstance(fctx, opname, psize);
    }

    /**
     * Returns the size of the scaled result image needed to hold the output
     * for the currently validated pass with the indicated input dimensions
     * and output clip.
     * The image may be further scaled after the shader operation is through
     * to obtain the final result bounds.
     * This value is only of use to the actual shader to understand exactly
     * how much room to allocate for the shader result.
     *
     * @param srcdimension the bounds of the input image
     * @param outputClip the area needed for the final result
     * @return the bounds of the result image for the current pass
     */
    public abstract Rectangle getPassResultBounds(Rectangle srcdimension,
                                                  Rectangle outputClip);

    /**
     * Return a hint about the way that the weights will be applied to the
     * pixels for the currently validated pass.
     *
     * @return the appropriate {@link PassType} that describes the filtering
     *         operation for this pass of the algorithm
     */
    public PassType getPassType() {
        return PassType.GENERAL_VECTOR;
    }

    /**
     * A {@link FloatBuffer} padded out to the required size as specified by
     * the {@link #getPeerSize()} method filled with the convolution weights
     * needed for the currently validated pass.
     *
     * @return a {@code FloatBuffer} containing the kernel convolution weights
     */
    public abstract FloatBuffer getPassWeights();

    /**
     * Returns the maximum number of valid float4 elements that should be
     * referenced from the buffer returned by getWeights() for the currently
     * validated pass.
     *
     * @return the maximum number of valid float4 elements in the weights buffer
     */
    public abstract int getPassWeightsArrayLength();

    /**
     * Returns an array of 4 floats used to initialize a float4 Shader
     * constant with the relative starting location of the first weight
     * in the convolution kernel and the incremental offset between each
     * sample to be weighted and accumulated.  The values are stored in
     * the array in the following order:
     * <pre>
     *     shadervec.x = vector[0] = incdx // X delta between subsequent samples
     *     shadervec.y = vector[1] = incdy // Y delta between subsequent samples
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
     * </pre>
     * The values are relative to the texture coordinate space which are
     * normalized to the range [0,1] over the source texture.
     *
     * @return an array of 4 floats representing
     *         {@code [ incdx, incdy, startdx, startdy ]}
     */
    public abstract float[] getPassVector();

    /**
     * For a shadow convolution operation, return the 4 float versions of
     * the color components, in the range {@code [0, 1]} for the shadow color
     * to be substituted for the input colors.
     * This method will only be called if {@link #isShadow()} returns true.
     *
     * @return the array of 4 floats representing the shadow color components
     */
    public abstract float[] getPassShadowColorComponents();

    /**
     * Returns the appropriate kernel size for the pass that was last
     * validated using validateInput().
     *
     * @return the pixel kernel size of the current pass
     */
    public abstract int getPassKernelSize();
}
