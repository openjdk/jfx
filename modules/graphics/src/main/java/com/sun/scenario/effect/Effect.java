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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.AccessHelper;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;

/**
 * The base class for all filter effects.
 */
public abstract class Effect {

    /**
     * A convenient constant for using a readable value to specify
     * a {@code null} value for input {@code Effect}s in method and
     * constructor parameter lists.
     * Specifying {@code effect.setInput(DefaultInput)} is equivalent
     * to specifying {@code effect.setInput(null)}.
     */
    public static final Effect DefaultInput = null;

    private final List<Effect> inputs;
    private final List<Effect> unmodifiableInputs;
    private final int maxInputs;

    static {
        AccessHelper.setStateAccessor(effect -> effect.getState());
    }

    /**
     * Constructs an {@code Effect} with no inputs.
     */
    protected Effect() {
        this.inputs = Collections.emptyList();
        this.unmodifiableInputs = inputs;
        this.maxInputs = 0;
    }

    /**
     * Constructs an {@code Effect} with exactly one input.
     *
     * @param input the input {@code Effect}
     */
    protected Effect(Effect input) {
        this.inputs = new ArrayList<Effect>(1);
        this.unmodifiableInputs = Collections.unmodifiableList(inputs);
        this.maxInputs = 1;
        setInput(0, input);
    }

    /**
     * Constructs an {@code Effect} with exactly two inputs.
     *
     * @param input1 the first input {@code Effect}
     * @param input2 the second input {@code Effect}
     */
    protected Effect(Effect input1, Effect input2) {
        this.inputs = new ArrayList<Effect>(2);
        this.unmodifiableInputs = Collections.unmodifiableList(inputs);
        this.maxInputs = 2;
        setInput(0, input1);
        setInput(1, input2);
    }

    /**
     * Returns state object that is associated with this effect instance.
     * Subclasses may override this method to return some sort of state
     * object that contains implementation details that are hidden from
     * the public API.  Classes outside this package can use the AccessHelper
     * class to get access to this package-private method.
     */
    Object getState() {
        return null;
    }

    /**
     * Returns the number of inputs processed by this {@code Effect}.
     *
     * @return the number of inputs for this {@code Effect}
     */
    public int getNumInputs() {
        return inputs.size();
    }

    /**
     * Returns the (immutable) list of input {@code Effect}s, or an empty
     * list if no inputs were specified at construction time.
     *
     * @return the list of input {@code Effect}s
     */
    public final List<Effect> getInputs() {
        return unmodifiableInputs;
    }

    /**
     * Sets the indexed input for this {@code Effect} to a specific
     * {@code Effect} or to the default input if {@code input} is
     * {@code null}.
     *
     * @param index the index of the input {@code Effect}
     * @param input the input {@code Effect}
     * @throws IllegalArgumentException if {@code index} is less than
     * zero or greater than or equal to the number of inputs specified
     * at construction time
     */
    protected void setInput(int index, Effect input) {
        if (index < 0 || index >= maxInputs) {
            throw new IllegalArgumentException("Index must be within allowable range");
        }

        if (index < inputs.size()) {
            inputs.set(index, input);
        } else {
            inputs.add(input);
        }
    }

    public static BaseBounds combineBounds(BaseBounds... inputBounds) {
        BaseBounds ret = null;
        if (inputBounds.length == 1) {
            ret = inputBounds[0];
        } else {
            for (int i = 0; i < inputBounds.length; i++) {
                BaseBounds r = inputBounds[i];
                if (r != null && !r.isEmpty()) {
                    if (ret == null) {
                        ret = new RectBounds();
                        ret = ret.deriveWithNewBounds(r);
                    } else {
                        ret = ret.deriveWithUnion(r);
                    }
                }
            }
        }
        if (ret == null) {
            ret = new RectBounds();
        }
        return ret;
    }

    public static Rectangle combineBounds(Rectangle... inputBounds) {
        Rectangle ret = null;
        if (inputBounds.length == 1) {
            ret = inputBounds[0];
        } else {
            for (int i = 0; i < inputBounds.length; i++) {
                Rectangle r = inputBounds[i];
                if (r != null && !r.isEmpty()) {
                    if (ret == null) {
                        ret = new Rectangle(r);
                    } else {
                        ret.add(r);
                    }
                }
            }
        }
        if (ret == null) {
            ret = new Rectangle();
        }
        return ret;
    }

    public Rectangle getResultBounds(BaseTransform transform,
                                     Rectangle outputClip,
                                     ImageData... inputDatas)
    {
        int numinputs = inputDatas.length;
        Rectangle inputBounds[] = new Rectangle[numinputs];
        for (int i = 0; i < numinputs; i++) {
            inputBounds[i] = inputDatas[i].getTransformedBounds(outputClip);
        }
        Rectangle rb = combineBounds(inputBounds);
        return rb;
    }

    /**
     * Applies this filter effect to the series of images represented by
     * the input {@code Effect}s and/or the given {@code defaultInput}
     * viewed under the given {@code transform}.
     * The filter does not need to create pixel data for any pixels that
     * fall outside of the destination device-space (pixel) bounds specified
     * by the {@code outputClip} {@code Rectangle}.
     * <p>
     * The filter might be able to use the {@code renderHelper} object to
     * render the results directly on its own if the object is not null and
     * implements an interface, such as {@link ImageHelper}, that the filter
     * recognizes.
     * If the effect renders itself then it will return a {@code null} for
     * the {@code ImageData} result.
     * <p>
     * Note that the {@code ImageData} object returned by this method must be
     * validated prior to use with
     * {@link ImageData#validate(com.sun.scenario.effect.FilterContext) } method.
     * <p>
     * <pre>
       boolean valid;

       do {
           ImageData res = filter(fctx, transform, clip, renderer, defaultInput);
           if (res == null) {
               break;
           }
           if (valid = res.validate(fctx)) {
               // Render res.getImage() to the appropriate destination
               // or use it as an input to another chain of effects.
           }
           res.unref();
       } while (!valid);
       </pre>
     * <p>
     * @param fctx the {@code FilterContext} that determines the
     * environment (e.g. the graphics device or code path) on which
     * the filter operation will be performed
     * @param transform an optional transform under which the filter and
     * its inputs will be viewed
     * @param outputClip the device space (pixel) bounds of the output
     * image or window or clip into which the result of the Effect will
     * be rendered, or null if the output dimensions are not known.
     * @param renderHelper an object which might be used to render
     * the results of the effect directly.
     * @param defaultInput the default input {@code Effect} to be used in
     * all cases where a filter has a null input.
     * @return the {@code ImageData} holding the result of this filter
     * operation or {@code null} if the filter had no output or used the
     * {@code renderHelper} to render its results directly.
     */
    public abstract ImageData filter(FilterContext fctx,
                                     BaseTransform transform,
                                     Rectangle outputClip,
                                     Object renderHelper,
                                     Effect defaultInput);

    public static BaseBounds transformBounds(BaseTransform tx, BaseBounds r) {
        if (tx == null || tx.isIdentity()) {
            return r;
        }
        BaseBounds ret = new RectBounds();
        ret = tx.transform(r, ret);
        return ret;
    }

    protected ImageData ensureTransform(FilterContext fctx,
                                        ImageData original,
                                        BaseTransform transform,
                                        Rectangle clip)
    {
        if (transform == null || transform.isIdentity()) {
            return original;
        }
        if (!original.validate(fctx)) {
            original.unref();
            return new ImageData(fctx, null, new Rectangle());
        }
        return original.transform(transform);
//
//        Rectangle origBounds = original.getBounds();
//        if (transform.isTranslateOrIdentity()) {
//            double tx = transform.getMxt();
//            double ty = transform.getMyt();
//            int itx = (int) tx;
//            int ity = (int) ty;
//            if (itx == tx && ity == ty) {
//                Rectangle r = new Rectangle(origBounds);
//                r.translate(itx, ity);
//                ImageData ret = new ImageData(original, r);
//                original.unref();
//                return ret;
//            }
//        }
//        RectBounds transformedBounds = transformBounds(transform, origBounds.toRectBounds());
//        Rectangle xformBounds = new Rectangle(transformedBounds);
//        if (clip != null) {
//            xformBounds.intersectWith(clip);
//        }
//        return Renderer.getRenderer(fctx).
//            transform(fctx, original, transform, origBounds, xformBounds);
    }

    /**
     * Returns the dirty region container containing dirty regions affected
     * by this filter operation.
     *
     * @param defaultInput the default input {@code Effect} to be used in
     *                     all cases where a filter has a null input
     * @param drc the container of dirty regions in scene coordinates.
     * @param regionPool the pool of dirty regions
     * @return the dirty region container
     */
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        DirtyRegionContainer merge = null;
        for (int i = 0; i < inputs.size(); i++) {
            DirtyRegionContainer drc = getDefaultedInput(i, defaultInput).getDirtyRegions(defaultInput, regionPool);
            if (merge == null) {
                merge = drc;
            } else {
                merge.merge(drc);
                regionPool.checkIn(drc);
            }
        }

        if (merge == null) {
            merge = regionPool.checkOut();
        }

        return merge;
    }

    Effect getDefaultedInput(int inputIndex, Effect defaultInput) {
        return getDefaultedInput(inputs.get(inputIndex), defaultInput);
    }

    static Effect getDefaultedInput(Effect listedInput, Effect defaultInput) {
        return (listedInput == null) ? defaultInput : listedInput;
    }

    /**
     * Returns the bounding box that will be affected by this filter
     * operation when viewed under the specified {@code transform},
     * given its list of input {@code Effect}s and the specified
     * {@code defaultInput} effect.
     * Note that the returned bounds can be smaller or larger than one
     * or more of the inputs.
     *
     * @param transform the transform the effect will be viewed under
     * @param defaultInput the default input {@code Effect} to be used in
     *                     all cases where a filter has a null input
     * @return the bounding box of this filter
     */
    public abstract BaseBounds getBounds(BaseTransform transform,
                                       Effect defaultInput);

    /**
     * Transform the specified point {@code p} from the coordinate space
     * of the primary content input to the coordinate space of the effect
     * output.
     * In essence, this method asks the question "Which output coordinate
     * is most affected by the data at the specified coordinate in the
     * primary source input?"
     * <p>
     * The definition of which input represents the primary content input
     * and how the coordinate space of that input compares to the coordinate
     * space of the result varies from effect to effect.
     * Note that some effects may have a reasonable definition of how to
     * map source coordinates to destination coordinates, but not the
     * reverse.
     * In particular, effects which map source coordinates discontiguously
     * into the result may have several output coordinates that are affected
     * by a given input coordinate and may choose to return one of many
     * equally valid answers, or an undefined result such as {@code NaN},
     * or some other anomalous value.
     * Most effects perform simple transformations of the color of each
     * pixel and so represent an identity transform and return the point
     * unchanged.
     *
     * @param p the point in the coordinate space of the primary content
     *          input to be transformed
     * @param defaultInput the default input {@code Effect} to be used in
     *                     all cases where a filter has a null input
     * @return the transformed point in the coordinate space of the result
     */
    public Point2D transform(Point2D p, Effect defaultInput) {
        return p;
    }

    /**
     * Transform the specified point {@code p} from the coordinate space
     * of the output of the effect into the coordinate space of the
     * primary content input.
     * In essence, this method asks the question "Which source coordinate
     * contributes most to the definition of the output at the specified
     * coordinate?"
     * <p>
     * The definition of which input represents the primary content input
     * and how the coordinate space of that input compares to the coordinate
     * space of the result varies from effect to effect.
     * Note that some effects may have a reasonable definition of how to
     * map destination coordinates back to source coordinates, but not the
     * reverse.
     * In particular, effects which produce entirely synthetic results not
     * based on any content input may not be able to give a meaningful
     * result to this query and may return undefined coordinates such as
     * {@code 0}, {@code Infinity}, or {@code NaN}.
     * Most effects perform simple transformations of the color of each
     * pixel and so represent an identity transform and return the point
     * unchanged.
     *
     * @param p the point in the coordinate space of the result output
     *          to be transformed
     * @param defaultInput the default input {@code Effect} to be used in
     *                     all cases where a filter has a null input
     * @return the untransformed point in the coordinate space of the
     *         primary content input
     */
    public Point2D untransform(Point2D p, Effect defaultInput) {
        return p;
    }

    /**
     * Returns a new image that is most compatible with the
     * given {@code FilterContext}.  This method will select the image
     * type that is most appropriate for use with the current rendering
     * pipeline, graphics hardware, and screen pixel layout.
     * The image will be cleared prior to being returned.
     *
     * This method may return {@code null} if the image can't be created so
     * callers have to check for return value.
     *
     * @param fctx the {@code FilterContext} for the target screen device
     * @param w the width of the image
     * @param h the height of the image
     * @return a new image with the given dimensions, or null if one
     * can't be created
     * @throws IllegalArgumentException if {@code gc} is null, or if
     * either {@code w} or {@code h} is non-positive
     */
    public static Filterable createCompatibleImage(FilterContext fctx, int w, int h) {
        return Renderer.getRenderer(fctx).createCompatibleImage(w, h);
    }

    /**
     * Returns an image that is most compatible with the
     * given {@code FilterContext}.  This method will select the image
     * type that is most appropriate for use with the current rendering
     * pipeline, graphics hardware, and screen pixel layout.
     * The image will be cleared prior to being returned.
     * <p>
     * Note that the framework attempts to pool images for recycling purposes
     * whenever possible.  Therefore, when finished using an image returned
     * by this method, it is highly recommended that you
     * {@link #releaseCompatibleImage release} the image back to the
     * shared pool for others to use.
     *
     * This method may return {@code null} if the image can't be created so
     * callers have to check for return value.
     *
     * @param fctx the {@code FilterContext} for the target screen device
     * @param w the width of the image
     * @param h the height of the image
     * @return an image with the given dimensions or null if one can't
     * be created
     * @throws IllegalArgumentException if {@code gc} is null, or if
     * either {@code w} or {@code h} is non-positive
     * @see #releaseCompatibleImage
     */
    public static Filterable getCompatibleImage(FilterContext fctx, int w, int h) {
        return Renderer.getRenderer(fctx).getCompatibleImage(w, h);
    }

    /**
     * Releases an image created by the
     * {@link #getCompatibleImage getCompatibleImage()} method
     * back into the shared pool.
     *
     * @param fctx the {@code FilterContext} for the target screen device
     * @param image the image to be released
     * @see #getCompatibleImage
     */
    public static void releaseCompatibleImage(FilterContext fctx, Filterable image) {
        Renderer.getRenderer(fctx).releaseCompatibleImage(image);
    }

    /**
     * Whether an opacity for any pixel is different (lower)
     * than the corresponding pixel in the default input.
     * It is always safe to return true from this method,
     * though the consequences may be that the caller chooses to not utilize a planned optimization.
     * @return true if this effect may reduce opacity of some pixels of one of it's input
     * (and thus the default input) or it's relevant input(s) might have reduced opaque pixels
     * of the default input already.
     */
    public abstract boolean reducesOpaquePixels();

    /**
     * A set of values that represent the possible levels of acceleration
     * for an {@code Effect} implementation.
     *
     * @see Effect#getAccelType
     */
    public enum AccelType {
        /**
         * Indicates that this {@code Effect} is implemented on top of
         * intrinsic operations built in to the Java 2D APIs.
         */
        INTRINSIC("Intrinsic"),
        /**
         * Indicates that this {@code Effect} is implemented in software
         * (i.e., running on the CPU), without any special acceleration.
         */
        NONE("CPU/Java"),
        /**
         * Indicates that this {@code Effect} is implemented in software
         * (i.e., running on the CPU), accelerated using native
         * SIMD instructions (e.g. SSE).
         */
        SIMD("CPU/SIMD"),
        /**
         * Indicates that this {@code Effect} is implemented in software
         * (i.e., running on the CPU), accelerated using native
         * fixed-point arithmetic.
         */
        FIXED("CPU/Fixed"),
        /**
         * Indicates that this {@code Effect} is being accelerated in
         * graphics hardware via OpenGL.
         */
        OPENGL("OpenGL"),
        /**
         * Indicates that this {@code Effect} is being accelerated in
         * graphics hardware via Direct3D.
         */
        DIRECT3D("Direct3D");

        private String text;

        private AccelType(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * Returns one of the {@link AccelType AccelType} values, indicating
     * whether this {@code Effect} is accelerated in hardware for the
     * given {@code FilterContext}.
     *
     * @param config the {@code FilterContext} that will be used
     * for performing the filter operation
     * @return one of the {@code AccelType} values
     */
    public abstract AccelType getAccelType(FilterContext fctx);
}
