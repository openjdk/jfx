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

/**
 * A class that encapsulates all of the information needed to plan and execute
 * a single filter operation.  An instance of the class is instantiated at
 * the start of an {@link Effect.filter()} operation and it is queried for
 * various pieces of information required to perform that operation including,
 * but not limited to, the coordinate space to be used for the child input
 * {@code Effect} operations and the coordinate transform to be applied to
 * the resulting final {@code ImageData} object produced as the result of that
 * filter operation.  Some effect-specific subclasses may also plan and supply
 * information about how the various pixels are to be computed in the inner
 * loops of the effect filter algorithm.
 */
public interface RenderState {
    /**
     * This enum characterizes the types of coordinate spaces that will be
     * used for the filter operation (i.e. handed to the dependent input
     * {@code Effect} objects) and applied to the result of the filter.
     */
    public static enum EffectCoordinateSpace {
        /**
         * The {@link RenderState} object will specify an IDENTITY transform
         * for the input transform and the original filter transform as
         * the result transform.
         */
        UserSpace,

        /**
         * The {@link RenderState} object will specify custom transform objects
         * for both the input transform and the result transform with the only
         * constraint that the two will concatenate to produce the original
         * filter transform.
         * <pre>
         *     BaseTransform inputtx = getInputTransform(filtertx);
         *     BaseTransform resulttx = getResultTransform(filtertx);
         *     // Ignoring the potential for concatenate to modify the
         *     // return values from the above two methods, this virtual
         *     // assert statement is just for the sake of documenting
         *     // the intended constraints.
         *     assert(filtertx.equalsApproximately(resulttx.concatenate(inputtx)));
         * </pre>
         */
        CustomSpace,

        /**
         * The {@link RenderState} object will specify the original filter
         * transform as the input transform and an IDENTITY transform for
         * the result transform.
         */
        RenderSpace,
    }

    /**
     * A helper implementation of {@link RenderState} that handles the
     * case of {@code EffectCoordinateSpace.UserSpace} and passes along
     * the outputClip to the inputs unmodified.
     */
    public static final RenderState UserSpaceRenderState =
        new RenderState() {
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
                return filterClip;
            }
        };

    /**
     * A helper implementation of {@link RenderState} that handles the
     * case of {@code EffectCoordinateSpace.UserSpace} and passes along
     * the outputClip to the inputs unmodified.
     */
    public static final RenderState UnclippedUserSpaceRenderState =
        new RenderState() {
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
                return null;
            }
        };

    /**
     * A helper implementation of {@link RenderState} that handles the
     * case of {@code EffectCoordinateSpace.RenderSpace} and passes along
     * the outputClip to the inputs unmodified.
     */
    public static final RenderState RenderSpaceRenderState =
        new RenderState() {
            @Override
            public EffectCoordinateSpace getEffectTransformSpace() {
                return EffectCoordinateSpace.RenderSpace;
            }

            @Override
            public BaseTransform getInputTransform(BaseTransform filterTransform) {
                return filterTransform;
            }

            @Override
            public BaseTransform getResultTransform(BaseTransform filterTransform) {
                return BaseTransform.IDENTITY_TRANSFORM;
            }

            @Override
            public Rectangle getInputClip(int i, Rectangle filterClip) {
                // REMIND: Need to factor out a few implementations here...
                return filterClip;
            }
        };

    /**
     * Return a hint indicating which coordinate space should be used for
     * the pixel filtering for this particular filtering operation.
     * The {@link #getEffectTransform(com.sun.javafx.geom.transform.BaseTransform)
     * getEffectTransform()} and {@link #getResultTransform(com.sun.javafx.geom.transform.BaseTransform)
     * getResultTransform()} methods will always be used to get the actual
     * transforms to be used to get input data and transform the results, but
     * this method can help to set the expectations of the caller to optimize
     * techniques.
     *
     * @return an {@link EffectSpace} value to describe the expected output
     * from the {@code getEffectTransform(...)} and {@code getResultTransform(...)}
     * methods.
     */
    public EffectCoordinateSpace getEffectTransformSpace();

    /**
     * Return the transform that should be used to obtain pixel input from the
     * {@code Effect} inputs for this filter operation.
     * The returned transform is handed to all input {@code Effect} objects
     * to obtain pixel data for the inputs.
     * Typically, the output of {@code getInputTransform(transform)} and
     * {@code getResultTransform(transform)} could be concatenated to produce
     * the original {@code filterTransform}.
     *
     * @param filterTransform the {@code BaseTransform} object for the filter operation
     * @return the {@code BaseTransform} object to use for the input effects
     */
    public BaseTransform getInputTransform(BaseTransform filterTransform);

    /**
     * Return the transform that should be used to transform the results of
     * the filter operation.
     * The returned transform is combined with the resulting filter result
     * texture to produce an output ImageData object.
     * Typically, the output of {@code getInputTransform(transform)} and
     * {@code getResultTransform(transform)} could be concatenated to produce
     * the original {@code filterTransform}.
     *
     * @param filterTransform the {@code BaseTransform} object for the filter operation
     * @return the {@code BaseTransform} object to be applied to the result
     * texture
     */
    public BaseTransform getResultTransform(BaseTransform filterTransform);

    /**
     * Return the clip for the indicated input based on the indicated output
     * clip.
     *
     * @param i the index of the input being processed
     * @param filterClip the output clip supplied to the given filter operation
     * @return the required rectangle from the indicated input to provide
     *         enough pixels to produce the indicated output clip
     */
    public Rectangle getInputClip(int i, Rectangle filterClip);
}
