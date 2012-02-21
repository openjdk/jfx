/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.effect;

import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Node;

import com.sun.javafx.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.effect.EffectUtils;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.scenario.effect.PhongLighting;

/**
 * An effect that simulates a light source shining on the given content,
 * which can be used to give flat objects a more realistic, three-dimensional
 * appearance.
 *
<PRE>
import javafx.scene.*;
import javafx.scene.text.*;
import javafx.scene.paint.*;
import javafx.scene.effect.*;
import javafx.geometry.*;

Light.Distant light = new Light.Distant();
light.setAzimuth(-135.0);

Lighting l = new Lighting();
l.setLight(light);
l.setSurfaceScale(5.0);

Text t = new Text();
t.setText("JavaFX!");
t.setFill(Color.RED);
t.setFont(Font.font(null, FontWeight.BOLD, 90));
t.setX(10.0);
t.setY(10.0);
t.setTextOrigin(VPos.TOP);

t.setEffect(l);
</PRE>
 */
public class Lighting extends Effect {
    @Override
    com.sun.scenario.effect.PhongLighting impl_createImpl() {
        return new PhongLighting(getLight().impl_getImpl());
    };

    /**
     * Creates a new instance of Lighting with default parameters.
     */
    public Lighting() {
        Shadow shadow = new Shadow();
        shadow.setRadius(10.0f);
        setBumpInput(shadow);
    }

    /**
     * Creates a new instance of Lighting with the specified light.
     * @param light the light source for this {@code Lighting} effect
     */
    public Lighting(Light light) {
        Shadow shadow = new Shadow();
        shadow.setRadius(10.0f);
        setBumpInput(shadow);
        setLight(light);
    }

    /**
     * The light source for this {@code Lighting} effect.
     */
    private ObjectProperty<Light> light = new ObjectPropertyBase<Light>(new Light.Distant()) {
        @Override
        public void invalidated() {
            markDirty(EffectDirtyBits.EFFECT_DIRTY);
            effectBoundsChanged();
        }

        @Override
        public Object getBean() {
            return Lighting.this;
        }

        @Override
        public String getName() {
            return "light";
        }
    };


    public final void setLight(Light value) {
        lightProperty().set(value);
    }

    public final Light getLight() {
        return light.get();
    }

    public final ObjectProperty<Light> lightProperty() {
        return light;
    }

    private final LightChangeListener lightChangeListener = new LightChangeListener();
    private class LightChangeListener extends EffectChangeListener {
        Light light;

        public void register(Light value) {
            light = value;
            super.register(light == null ? null : light.effectDirtyProperty());
        }

        @Override
        public void invalidated(Observable valueModel) {
            if (light.impl_isEffectDirty()) {
                markDirty(EffectDirtyBits.EFFECT_DIRTY);
                effectBoundsChanged();
            }
        }
    };
    /**
     * The optional bump map input.
     * If not specified, a bump map will be automatically generated
     * from the default input.
     * If set to {@code null}, or left unspecified, a graphical image of
     * the {@code Node} to which the {@code Effect} is attached will be
     * used to generate a default bump map.
     * @defaultValue a Shadow effect with a radius of 10
     */
    private ObjectProperty<Effect> bumpInput;


    public final void setBumpInput(Effect value) {
        bumpInputProperty().set(value);
    }

    public final Effect getBumpInput() {
        return bumpInput == null ? null : bumpInput.get();
    }

    public final ObjectProperty<Effect> bumpInputProperty() {
        if (bumpInput == null) {
            bumpInput = new EffectInputProperty("bumpInput");
        }
        return bumpInput;
    }

    /**
     * The content input for this {@code Effect}.
     * If set to {@code null}, or left unspecified, a graphical image of
     * the {@code Node} to which the {@code Effect} is attached will be
     * used as the input.
     * @defaultValue null
     */
    private ObjectProperty<Effect> contentInput;


    public final void setContentInput(Effect value) {
        contentInputProperty().set(value);
    }

    public final Effect getContentInput() {
        return contentInput == null ? null : contentInput.get();
    }

    public final ObjectProperty<Effect> contentInputProperty() {
        if (contentInput == null) {
            contentInput = new EffectInputProperty("contentInput");
        }
        return contentInput;
    }

    @Override
    boolean impl_checkChainContains(Effect e) {
        Effect localBumpInput = getBumpInput();
        Effect localContentInput = getContentInput();
        if (localContentInput == e || localBumpInput == e)
            return true;
        if (localContentInput != null && localContentInput.impl_checkChainContains(e))
            return true;
        if (localBumpInput != null && localBumpInput.impl_checkChainContains(e))
            return true;

        return false;
    }

    /**
     * The diffuse constant.
     * <pre>
     *       Min: 0.0
     *       Max: 2.0
     *   Default: 1.0
     *  Identity: n/a
     * </pre>
     * @defaultValue 1.0
     */
    private DoubleProperty diffuseConstant;


    public final void setDiffuseConstant(double value) {
        diffuseConstantProperty().set(value);
    }

    public final double getDiffuseConstant() {
        return diffuseConstant == null ? 1 : diffuseConstant.get();
    }

    public final DoubleProperty diffuseConstantProperty() {
        if (diffuseConstant == null) {
            diffuseConstant = new DoublePropertyBase(1) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Lighting.this;
                }

                @Override
                public String getName() {
                    return "diffuseConstant";
                }
            };
        }
        return diffuseConstant;
    }

    /**
     * The specular constant.
     * <pre>
     *       Min: 0.0
     *       Max: 2.0
     *   Default: 0.3
     *  Identity: n/a
     * </pre>
     * @defaultValue 0.3
     */
    private DoubleProperty specularConstant;


    public final void setSpecularConstant(double value) {
        specularConstantProperty().set(value);
    }

    public final double getSpecularConstant() {
        return specularConstant == null ? 0.3 : specularConstant.get();
    }

    public final DoubleProperty specularConstantProperty() {
        if (specularConstant == null) {
            specularConstant = new DoublePropertyBase(0.3) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Lighting.this;
                }

                @Override
                public String getName() {
                    return "specularConstant";
                }
            };
        }
        return specularConstant;
    }

    /**
     * The specular exponent.
     * <pre>
     *       Min:  0.0
     *       Max: 40.0
     *   Default: 20.0
     *  Identity:  n/a
     * </pre>
     * @defaultValue 20.0
     */
    private DoubleProperty specularExponent;


    public final void setSpecularExponent(double value) {
        specularExponentProperty().set(value);
    }

    public final double getSpecularExponent() {
        return specularExponent == null ? 20 : specularExponent.get();
    }

    public final DoubleProperty specularExponentProperty() {
        if (specularExponent == null) {
            specularExponent = new DoublePropertyBase(20) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Lighting.this;
                }

                @Override
                public String getName() {
                    return "specularExponent";
                }
            };
        }
        return specularExponent;
    }

    /**
     * The surface scale factor.
     * <pre>
     *       Min:  0.0
     *       Max: 10.0
     *   Default:  1.5
     *  Identity:  n/a
     * </pre>
     * @defaultValue 1.5
     */
    private DoubleProperty surfaceScale;


    public final void setSurfaceScale(double value) {
        surfaceScaleProperty().set(value);
    }

    public final double getSurfaceScale() {
        return surfaceScale == null ? 1.5 : surfaceScale.get();
    }

    public final DoubleProperty surfaceScaleProperty() {
        if (surfaceScale == null) {
            surfaceScale = new DoublePropertyBase(1.5) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Lighting.this;
                }

                @Override
                public String getName() {
                    return "surfaceScale";
                }
            };
        }
        return surfaceScale;
    }

    @Override
    void impl_update() {
        Effect localBumpInput = getBumpInput();

        if (localBumpInput != null) {
            localBumpInput.impl_sync();
        }

        Effect localContentInput = getContentInput();
        if (localContentInput != null) {
            localContentInput.impl_sync();
        }

        PhongLighting peer = (PhongLighting) impl_getImpl();
        peer.setBumpInput(localBumpInput == null ? null : localBumpInput.impl_getImpl());
        peer.setContentInput(localContentInput == null ? null : localContentInput.impl_getImpl());
        peer.setDiffuseConstant((float)Utils.clamp(0, getDiffuseConstant(), 2));
        peer.setSpecularConstant((float)Utils.clamp(0, getSpecularConstant(), 2));
        peer.setSpecularExponent((float)Utils.clamp(0, getSpecularExponent(), 40));
        peer.setSurfaceScale((float)Utils.clamp(0, getSurfaceScale(), 10));
        Light localLight = getLight(); // FIXME null value
        lightChangeListener.register(localLight);
        if (localLight != null) {
            localLight.impl_sync();
            peer.setLight(localLight.impl_getImpl());
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_getBounds(BaseBounds bounds,
                                     BaseTransform tx,
                                     Node node,
                                     BoundsAccessor boundsAccessor) {
        return EffectUtils.getInputBounds(bounds, tx,
                                          node, boundsAccessor,
                                          getContentInput());
    }
}
