/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

import com.sun.javafx.util.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;


/**
 * An effect that allows for per-pixel adjustments of hue, saturation,
 * brightness, and contrast.
 *
 * <p>
 * Example:
 * <pre>{@code
 * ColorAdjust colorAdjust = new ColorAdjust();
 * colorAdjust.setContrast(0.1);
 * colorAdjust.setHue(-0.05);
 * colorAdjust.setBrightness(0.1);
 * colorAdjust.setSaturation(0.2);
 *
 * Image image = new Image("boat.jpg");
 * ImageView imageView = new ImageView(image);
 * imageView.setFitWidth(200);
 * imageView.setPreserveRatio(true);
 * imageView.setEffect(colorAdjust);
 * }</pre>
 * <p> The code above applied on this image: </p>
 * <p>
 * <img src="doc-files/photo.png" alt="A photo">
 * </p>
 * <p> produces the following: </p>
 * <p>
 * <img src="doc-files/coloradjust.png" alt="The visual effect of ColorAdjust on
 * photo">
 * </p>
 * @since JavaFX 2.0
 */
public class ColorAdjust extends Effect {
    /**
     * Creates a new instance of ColorAdjust with default parameters.
     */
    public ColorAdjust() {}

    /**
     * Creates a new instance of ColorAdjust with the specified hue, saturation,
     * brightness, and contrast.
     * @param hue the hue adjustment value
     * @param saturation the saturation adjustment value
     * @param brightness the brightness adjustment value
     * @param contrast the contrast adjustment value
     * @since JavaFX 2.1
     */
    public ColorAdjust(double hue,
                       double saturation,
                       double brightness,
                       double contrast) {
        setBrightness(brightness);
        setContrast(contrast);
        setHue(hue);
        setSaturation(saturation);
    }

    @Override
    com.sun.scenario.effect.ColorAdjust createPeer() {
        return new com.sun.scenario.effect.ColorAdjust();
    };
    /**
     * The input for this {@code Effect}.
     * If set to {@code null}, or left unspecified, a graphical image of
     * the {@code Node} to which the {@code Effect} is attached will be
     * used as the input.
     * @defaultValue null
     */
    private ObjectProperty<Effect> input;


    public final void setInput(Effect value) {
        inputProperty().set(value);
    }

    public final Effect getInput() {
        return input == null ? null : input.get();
    }

    public final ObjectProperty<Effect> inputProperty() {
        if (input == null) {
            input = new EffectInputProperty("input");
        }
        return input;
    }

    @Override
    boolean checkChainContains(Effect e) {
        Effect localInput = getInput();
        if (localInput == null)
            return false;
        if (localInput == e)
            return true;
        return localInput.checkChainContains(e);
    }

    /**
     * The hue adjustment value.
     * <pre>
     *       Min: -1.0
     *       Max: +1.0
     *   Default:  0.0
     *  Identity:  0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty hue;


    public final void setHue(double value) {
        hueProperty().set(value);
    }

    public final double getHue() {
        return hue == null ? 0 : hue.get();
    }

    public final DoubleProperty hueProperty() {
        if (hue == null) {
            hue = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return ColorAdjust.this;
                }

                @Override
                public String getName() {
                    return "hue";
                }
            };
        }
        return hue;
    }

    /**
     * The saturation adjustment value.
     * <pre>
     *       Min: -1.0
     *       Max: +1.0
     *   Default:  0.0
     *  Identity:  0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty saturation;


    public final void setSaturation(double value) {
        saturationProperty().set(value);
    }

    public final double getSaturation() {
        return saturation == null ? 0 : saturation.get();
    }

    public final DoubleProperty saturationProperty() {
        if (saturation == null) {
            saturation = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return ColorAdjust.this;
                }

                @Override
                public String getName() {
                    return "saturation";
                }
            };
        }
        return saturation;
    }

    /**
     * The brightness adjustment value.
     * <pre>
     *       Min: -1.0
     *       Max: +1.0
     *   Default:  0.0
     *  Identity:  0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty brightness;


    public final void setBrightness(double value) {
        brightnessProperty().set(value);
    }

    public final double getBrightness() {
        return brightness == null ? 0 : brightness.get();
    }

    public final DoubleProperty brightnessProperty() {
        if (brightness == null) {
            brightness = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return ColorAdjust.this;
                }

                @Override
                public String getName() {
                    return "brightness";
                }
            };
        }
        return brightness;
    }

    /**
     * The contrast adjustment value.
     * <pre>
     *       Min: -1.0
     *       Max: +1.0
     *   Default:  0.0
     *  Identity:  0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty contrast;


    public final void setContrast(double value) {
        contrastProperty().set(value);
    }

    public final double getContrast() {
        return contrast == null ? 0 : contrast.get();
    }

    public final DoubleProperty contrastProperty() {
        if (contrast == null) {
            contrast = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return ColorAdjust.this;
                }

                @Override
                public String getName() {
                    return "contrast";
                }
            };
        }
        return contrast;
    }

    @Override
    void update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.sync();
        }

        com.sun.scenario.effect.ColorAdjust peer =
                (com.sun.scenario.effect.ColorAdjust) getPeer();
        peer.setInput(localInput == null ? null : localInput.getPeer());
        peer.setHue((float)Utils.clamp(-1, getHue(), 1));
        peer.setSaturation((float)Utils.clamp(-1, getSaturation(), 1));
        peer.setBrightness((float)Utils.clamp(-1, getBrightness(), 1));
        peer.setContrast((float)Utils.clamp(-1, getContrast(), 1));
    }

    @Override
    BaseBounds getBounds(BaseBounds bounds,
                         BaseTransform tx,
                         Node node,
                         BoundsAccessor boundsAccessor) {
        return getInputBounds(bounds, tx, node, boundsAccessor, getInput());
    }

    @Override
    Effect copy() {
        ColorAdjust ca = new ColorAdjust(this.getHue(), this.getSaturation(),
                this.getBrightness(), this.getContrast());
        ca.setInput(ca.getInput());
        return ca;
    }
}
