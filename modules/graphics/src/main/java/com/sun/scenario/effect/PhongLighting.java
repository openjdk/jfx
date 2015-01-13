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
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;
import com.sun.scenario.effect.light.Light;

/**
 * An effect that applies diffuse and specular lighting to an arbitrary
 * input using a positionable light source.
 */
public class PhongLighting extends CoreEffect<RenderState> {

    private float surfaceScale;
    private float diffuseConstant;
    private float specularConstant;
    private float specularExponent;
    private Light light;

    /**
     * Constructs a new {@code PhongLighting} effect for the given
     * {@code Light}, with default values for all other properties,
     * using the default input for source data.
     * This is a convenience constructor that automatically generates a
     * bump map using the default input.
     *
     * @param light the light source
     * @throws IllegalArgumentException if {@code light} is null
     */
    public PhongLighting(Light light) {
        this(light, new GaussianShadow(10f), DefaultInput);
    }

    /**
     * Constructs a new {@code PhongLighting} effect for the given
     * {@code Light} and the given bump and content input {@code Effect}s
     * with default values for all other properties.
     *
     * @param light the light source
     * @param bumpInput the input containing the bump map
     * @param contentInput the input containing the content data
     * @throws IllegalArgumentException if {@code light} is null
     */
    public PhongLighting(Light light, Effect bumpInput, Effect contentInput) {
        super(bumpInput, contentInput);

        this.surfaceScale = 1f;
        this.diffuseConstant = 1f;
        this.specularConstant = 1f;
        this.specularExponent = 1f;

        setLight(light);
    }

    /**
     * Returns the bump input for this {@code Effect}.
     *
     * @return the bump input for this {@code Effect}
     */
    public final Effect getBumpInput() {
        return getInputs().get(0);
    }

    /**
     * Sets the bump input for this {@code Effect} to a specific
     * {@code Effect} or to the default input if {@code input} is
     * {@code null}.
     *
     * @param bumpInput the bump input for this {@code Effect}
     */
    public void setBumpInput(Effect bumpInput) {
        setInput(0, bumpInput);
    }

    /**
     * Returns the content input for this {@code Effect}.
     *
     * @return the content input for this {@code Effect}
     */
    public final Effect getContentInput() {
        return getInputs().get(1);
    }

    private Effect getContentInput(Effect defaultInput) {
        return getDefaultedInput(1, defaultInput);
    }

    /**
     * Sets the content input for this {@code Effect} to a specific
     * {@code Effect} or to the default input if {@code input} is
     * {@code null}.
     *
     * @param contentInput the content input for this {@code Effect}
     */
    public void setContentInput(Effect contentInput) {
        setInput(1, contentInput);
    }

    /**
     * Returns the light source.
     *
     * @return the light source
     */
    public Light getLight() {
        return light;
    }

    /**
     * Sets the light source.
     *
     * @param light the light source
     * @throws IllegalArgumentException if {@code light} is null
     */
    public void setLight(Light light) {
        if (light == null) {
            throw new IllegalArgumentException("Light must be non-null");
        }
        this.light = light;
        updatePeerKey("PhongLighting_" + light.getType().name());
    }
    
    /**
     * Returns the diffuse constant.
     *
     * @return the diffuse constant value
     */
    public float getDiffuseConstant() {
        return diffuseConstant;
    }

    /**
     * Sets the diffuse constant.
     * <pre>
     *       Min: 0.0
     *       Max: 2.0
     *   Default: 1.0
     *  Identity: n/a
     * </pre>
     *
     * @param diffuseConstant the diffuse constant value
     * @throws IllegalArgumentException if {@code diffuseConstant} is outside
     * the allowable range
     */
    public void setDiffuseConstant(float diffuseConstant) {
        if (diffuseConstant < 0f || diffuseConstant > 2f) {
            throw new IllegalArgumentException("Diffuse constant must be in the range [0,2]");
        }
        float old = this.diffuseConstant;
        this.diffuseConstant = diffuseConstant;
    }

    /**
     * Returns the specular constant.
     *
     * @return the specular constant value
     */
    public float getSpecularConstant() {
        return specularConstant;
    }

    /**
     * Sets the specular constant.
     * <pre>
     *       Min: 0.0
     *       Max: 2.0
     *   Default: 1.0
     *  Identity: n/a
     * </pre>
     *
     * @param specularConstant the specular constant value
     * @throws IllegalArgumentException if {@code specularConstant} is outside
     * the allowable range
     */
    public void setSpecularConstant(float specularConstant) {
        if (specularConstant < 0f || specularConstant > 2f) {
            throw new IllegalArgumentException("Specular constant must be in the range [0,2]");
        }
        float old = this.specularConstant;
        this.specularConstant = specularConstant;
    }

    /**
     * Returns the specular exponent.
     *
     * @return the specular exponent value
     */
    public float getSpecularExponent() {
        return specularExponent;
    }

    /**
     * Sets the specular exponent.
     * <pre>
     *       Min:  0.0
     *       Max: 40.0
     *   Default:  1.0
     *  Identity:  n/a
     * </pre>
     *
     * @param specularExponent the specular exponent value
     * @throws IllegalArgumentException if {@code specularExponent} is outside
     * the allowable range
     */
    public void setSpecularExponent(float specularExponent) {
        if (specularExponent < 0f || specularExponent > 40f) {
            throw new IllegalArgumentException("Specular exponent must be in the range [0,40]");
        }
        float old = this.specularExponent;
        this.specularExponent = specularExponent;
    }

    /**
     * Returns the surface scale.
     *
     * @return the surface scale value
     */
    public float getSurfaceScale() {
        return surfaceScale;
    }

    /**
     * Sets the surface scale.
     * <pre>
     *       Min:  0.0
     *       Max: 10.0
     *   Default:  1.0
     *  Identity:  n/a
     * </pre>
     *
     * @param surfaceScale the surface scale value
     * @throws IllegalArgumentException if {@code surfaceScale} is outside
     * the allowable range
     */
    public void setSurfaceScale(float surfaceScale) {
        if (surfaceScale < 0f || surfaceScale > 10f) {
            throw new IllegalArgumentException("Surface scale must be in the range [0,10]");
        }
        float old = this.surfaceScale;
        this.surfaceScale = surfaceScale;
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform,
                              Effect defaultInput)
    {
        // effect inherits its bounds from the content input
        return getContentInput(defaultInput).getBounds(transform, defaultInput);
    }

    @Override
    public Rectangle getResultBounds(BaseTransform transform,
                                     Rectangle outputClip,
                                     ImageData... inputDatas)
    {
        // result inherits its dimensions from the content input
        return super.getResultBounds(transform, outputClip, inputDatas[1]);
    }

    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        return getContentInput(defaultInput).transform(p, defaultInput);
    }

    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        return getContentInput(defaultInput).untransform(p, defaultInput);
    }

    @Override
    public RenderState getRenderState(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      Object renderHelper,
                                      Effect defaultInput)
    {
        // RT-27564
        // TODO: Since only the content input is used for the output bounds
        // we could attempt to factor the bounds of the content input in our
        // answer for the getInputClip() method of the RenderState, but for
        // now we will just use (a close copy of) the stock RenderSpaceRenderState object.
        return new RenderState() {
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
                if (i == 0 && filterClip != null) {
                    Rectangle r = new Rectangle(filterClip);
                    r.grow(1, 1);
                    return r;
                }
                return filterClip;
            }
        };
    }

    @Override
    public boolean reducesOpaquePixels() {
        final Effect contentInput = getContentInput();
        return contentInput != null && contentInput.reducesOpaquePixels();
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        Effect bump = getDefaultedInput(0, defaultInput);
        DirtyRegionContainer drc1 = bump.getDirtyRegions(defaultInput, regionPool);
        drc1.grow(1, 1);

        Effect content = getDefaultedInput(1, defaultInput);
        DirtyRegionContainer drc2 = content.getDirtyRegions(defaultInput, regionPool);

        drc1.merge(drc2);
        regionPool.checkIn(drc2);
        
        return drc1;
    }
}
