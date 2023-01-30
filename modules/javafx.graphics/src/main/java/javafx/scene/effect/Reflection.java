/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * An effect that renders a reflected version of the input below the
 * actual input content.
 * <p>
 * Note that the reflection of a {@code Node} with a {@code Reflection}
 * effect installed will not respond to mouse events or the containment
 * methods on the {@code Node}.
 *
 * <p>
 * Example:
 * <pre>{@code
 * Reflection reflection = new Reflection();
 * reflection.setFraction(0.7);
 *
 * Text text = new Text();
 * text.setX(10.0);
 * text.setY(50.0);
 * text.setCache(true);
 * text.setText("Reflections on JavaFX...");
 * text.setFill(Color.web("0x3b596d"));
 * text.setFont(Font.font(null, FontWeight.BOLD, 40));
 * text.setEffect(reflection);
 * }</pre>
 * <p> The code above produces the following: </p>
 * <p>
 * <img src="doc-files/reflection.png" alt="The visual effect of Reflection on text">
 * </p>
 * @since JavaFX 2.0
 */
public class Reflection extends Effect {
    /**
     * Creates a new instance of Reflection with default parameters.
     */
    public Reflection() {}

    /**
     * Creates a new instance of Reflection with the specified topOffset, fraction,
     * topOpacity and bottomOpacity.
     * @param topOffset the distance between the bottom of the input and the top of the reflection
     * @param fraction the fraction of the input that is visible in the reflection
     * @param topOpacity the opacity of the reflection at its top extreme
     * @param bottomOpacity the opacity of the reflection at its bottom extreme
     * @since JavaFX 2.1
     */
    public Reflection(double topOffset, double fraction,
                      double topOpacity, double bottomOpacity) {
        setBottomOpacity(bottomOpacity);
        setTopOffset(topOffset);
        setTopOpacity(topOpacity);
        setFraction(fraction);
    }

    @Override
    com.sun.scenario.effect.Reflection createPeer() {
        return new com.sun.scenario.effect.Reflection();
    }
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
     * The top offset adjustment, which is the distance between the
     * bottom of the input and the top of the reflection.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty topOffset;


    public final void setTopOffset(double value) {
        topOffsetProperty().set(value);
    }

    public final double getTopOffset() {
        return topOffset == null ? 0 : topOffset.get();
    }

    public final DoubleProperty topOffsetProperty() {
        if (topOffset == null) {
            topOffset = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return Reflection.this;
                }

                @Override
                public String getName() {
                    return "topOffset";
                }
            };
        }
        return topOffset;
    }

    /**
     * The top opacity value, which is the opacity of the reflection
     * at its top extreme.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.5
     *  Identity: 1.0
     * </pre>
     * @defaultValue 0.5
     */
    private DoubleProperty topOpacity;


    public final void setTopOpacity(double value) {
        topOpacityProperty().set(value);
    }

    public final double getTopOpacity() {
        return topOpacity == null ? 0.5 : topOpacity.get();
    }

    public final DoubleProperty topOpacityProperty() {
        if (topOpacity == null) {
            topOpacity = new DoublePropertyBase(0.5) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Reflection.this;
                }

                @Override
                public String getName() {
                    return "topOpacity";
                }
            };
        }
        return topOpacity;
    }

    /**
     * The bottom opacity value, which is the opacity of the reflection
     * at its bottom extreme.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.0
     *  Identity: 1.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty bottomOpacity;


    public final void setBottomOpacity(double value) {
        bottomOpacityProperty().set(value);
    }

    public final double getBottomOpacity() {
        return bottomOpacity == null ? 0 : bottomOpacity.get();
    }

    public final DoubleProperty bottomOpacityProperty() {
        if (bottomOpacity == null) {
            bottomOpacity = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Reflection.this;
                }

                @Override
                public String getName() {
                    return "bottomOpacity";
                }
            };
        }
        return bottomOpacity;
    }

    /**
     * The fraction of the input that is visible in the reflection.
     * For example, a value of 0.5 means that only the bottom half of the
     * input will be visible in the reflection.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.75
     *  Identity: 1.0
     * </pre>
     * @defaultValue 0.75
     */
    private DoubleProperty fraction;


    public final void setFraction(double value) {
        fractionProperty().set(value);
    }

    public final double getFraction() {
        return fraction == null ? 0.75 : fraction.get();
    }

    public final DoubleProperty fractionProperty() {
        if (fraction == null) {
            fraction = new DoublePropertyBase(0.75) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return Reflection.this;
                }

                @Override
                public String getName() {
                    return "fraction";
                }
            };
        }
        return fraction;
    }

    private float getClampedFraction() {
        return (float)Utils.clamp(0, getFraction(), 1);
    }

    private float getClampedBottomOpacity() {
        return (float)Utils.clamp(0, getBottomOpacity(), 1);
    }

    private float getClampedTopOpacity() {
        return (float)Utils.clamp(0, getTopOpacity(), 1);
    }

    @Override
    void update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.sync();
        }

        com.sun.scenario.effect.Reflection peer =
                (com.sun.scenario.effect.Reflection) getPeer();
        peer.setInput(localInput == null ? null : localInput.getPeer());
        peer.setFraction(getClampedFraction());
        peer.setTopOffset((float)getTopOffset());
        peer.setBottomOpacity(getClampedBottomOpacity());
        peer.setTopOpacity(getClampedTopOpacity());
    }

    @Override
    BaseBounds getBounds(BaseBounds bounds,
                         BaseTransform tx,
                         Node node,
                         BoundsAccessor boundsAccessor) {
        bounds = getInputBounds(bounds,
                                BaseTransform.IDENTITY_TRANSFORM,
                                node, boundsAccessor,
                                getInput());
        bounds.roundOut();

        float x1 = bounds.getMinX();
        float y1 = bounds.getMaxY() + (float)getTopOffset();
        float z1 = bounds.getMinZ();
        float x2 = bounds.getMaxX();
        float y2 = y1 + (getClampedFraction() * bounds.getHeight());
        float z2 = bounds.getMaxZ();

        BaseBounds ret = BaseBounds.getInstance(x1, y1, z1, x2, y2, z2);
        ret = ret.deriveWithUnion(bounds);

        return transformBounds(tx, ret);
    }

    @Override
    Effect copy() {
        Reflection ref = new Reflection(this.getTopOffset(), this.getFraction(),
                this.getTopOpacity(), this.getBottomOpacity());
        ref.setInput(ref.getInput());
        return ref;
    }
}
