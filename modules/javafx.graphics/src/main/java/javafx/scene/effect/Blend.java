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
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Node;

import com.sun.javafx.util.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.scenario.effect.Blend.Mode;


/**
 * An effect that blends the two inputs together using one of the
 * pre-defined {@link BlendMode}s.
 *
 * <p>
 * Example:
 * <pre>{@code
 * Blend blend = new Blend();
 * blend.setMode(BlendMode.COLOR_BURN);
 *
 * ColorInput colorInput = new ColorInput();
 * colorInput.setPaint(Color.STEELBLUE);
 * colorInput.setX(10);
 * colorInput.setY(10);
 * colorInput.setWidth(100);
 * colorInput.setHeight(180);
 *
 * blend.setTopInput(colorInput);
 *
 * Rectangle rect = new Rectangle();
 * rect.setWidth(220);
 * rect.setHeight(100);
 * Stop[] stops = new Stop[]{new Stop(0, Color.LIGHTSTEELBLUE), new Stop(1, Color.PALEGREEN)};
 * LinearGradient lg = new LinearGradient(0, 0, 0.25, 0.25, true, CycleMethod.REFLECT, stops);
 * rect.setFill(lg);
 *
 * Text text = new Text();
 * text.setX(15);
 * text.setY(65);
 * text.setFill(Color.PALEVIOLETRED);
 * text.setText("COLOR_BURN");
 * text.setFont(Font.font(null, FontWeight.BOLD, 30));
 *
 * Group g = new Group();
 * g.setEffect(blend);
 * g.getChildren().addAll(rect, text);
 * }</pre>
 *
 * <p> The code above produces the following: </p>
 * <p> <img src="doc-files/blend.png" alt="The visual effect of blending color,
 * gradient and text"> </p>
 * @since JavaFX 2.0
 */
public class Blend extends Effect {

    static private Mode toPGMode(BlendMode mode) {
        if (mode == null) {
            return Mode.SRC_OVER; // Default value
        } else if (mode == BlendMode.SRC_OVER) {
            return Mode.SRC_OVER;
        } else if (mode == BlendMode.SRC_ATOP) {
            return Mode.SRC_ATOP;
        } else if (mode == BlendMode.ADD) {
            return Mode.ADD;
        } else if (mode == BlendMode.MULTIPLY) {
            return Mode.MULTIPLY;
        } else if (mode == BlendMode.SCREEN) {
            return Mode.SCREEN;
        } else if (mode == BlendMode.OVERLAY) {
            return Mode.OVERLAY;
        } else if (mode == BlendMode.DARKEN) {
            return Mode.DARKEN;
        } else if (mode == BlendMode.LIGHTEN) {
            return Mode.LIGHTEN;
        } else if (mode == BlendMode.COLOR_DODGE) {
            return Mode.COLOR_DODGE;
        } else if (mode == BlendMode.COLOR_BURN) {
            return Mode.COLOR_BURN;
        } else if (mode == BlendMode.HARD_LIGHT) {
            return Mode.HARD_LIGHT;
        } else if (mode == BlendMode.SOFT_LIGHT) {
            return Mode.SOFT_LIGHT;
        } else if (mode == BlendMode.DIFFERENCE) {
            return Mode.DIFFERENCE;
        } else if (mode == BlendMode.EXCLUSION) {
            return Mode.EXCLUSION;
        } else if (mode == BlendMode.RED) {
            return Mode.RED;
        } else if (mode == BlendMode.GREEN) {
            return Mode.GREEN;
        } else if (mode == BlendMode.BLUE) {
            return Mode.BLUE;
        } else {
            throw new java.lang.AssertionError("Unrecognized blend mode: {mode}");
        }
    }

    /**
     * Used by Group to convert the FX BlendMode enum value into a Decora value.
     */
    static Mode getToolkitMode(BlendMode mode) {
        return toPGMode(mode);
    }

    /**
     * Creates a new instance of Blend with default parameters.
     */
    public Blend() {}

    /**
     * Creates a new instance of Blend with the specified mode.
     * @param mode the {@code BlendMode} used to blend the two inputs together
     * @since JavaFX 2.1
     */
    public Blend(BlendMode mode) {
        setMode(mode);
    }

    /**
     * Creates a new instance of Blend with the specified mode and bottom
     * and top inputs.
     * @param mode the {@code BlendMode} used to blend the two inputs together
     * @param bottomInput the bottom input for this {@code Blend} operation
     * @param topInput the top input for this {@code Blend} operation
     * @since JavaFX 2.1
     */
    public Blend(BlendMode mode, Effect bottomInput, Effect topInput) {
        setMode(mode);
        setBottomInput(bottomInput);
        setTopInput(topInput);
    }

    @Override
    com.sun.scenario.effect.Blend createPeer() {
        return new com.sun.scenario.effect.Blend(
                        toPGMode(BlendMode.SRC_OVER),
                        com.sun.scenario.effect.Effect.DefaultInput,
                        com.sun.scenario.effect.Effect.DefaultInput);
    }

    /**
     * The {@code BlendMode} used to blend the two inputs together.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: BlendMode.SRC_OVER
     *  Identity: n/a
     * </pre>
     * @defaultValue SRC_OVER
     */
    private ObjectProperty<BlendMode> mode;


    public final void setMode(BlendMode value) {
        modeProperty().set(value);
    }

    public final BlendMode getMode() {
        return mode == null ? BlendMode.SRC_OVER : mode.get();
    }

    public final ObjectProperty<BlendMode> modeProperty() {
        if (mode == null) {
            mode = new ObjectPropertyBase<BlendMode>(BlendMode.SRC_OVER) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Blend.this;
                }

                @Override
                public String getName() {
                    return "mode";
                }
            };
        }
        return mode;
    }

    /**
     * The opacity value, which is modulated with the top input prior
     * to blending.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 1.0
     *  Identity: 1.0
     * </pre>
     * @defaultValue 1.0
     */
    private DoubleProperty opacity;


    public final void setOpacity(double value) {
        opacityProperty().set(value);
    }

    public final double getOpacity() {
        return opacity == null ? 1 : opacity.get();
    }

    public final DoubleProperty opacityProperty() {
        if (opacity == null) {
            opacity = new DoublePropertyBase(1) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Blend.this;
                }

                @Override
                public String getName() {
                    return "opacity";
                }
            };
        }
        return opacity;
    }

    /**
     * The bottom input for this {@code Blend} operation.
     * If set to {@code null}, or left unspecified, a graphical image of
     * the {@code Node} to which the {@code Effect} is attached will be
     * used as the input.
     * @defaultValue null
     */
    private ObjectProperty<Effect> bottomInput;


    public final void setBottomInput(Effect value) {
        bottomInputProperty().set(value);
    }

    public final Effect getBottomInput() {
        return bottomInput == null ? null : bottomInput.get();
    }

    public final ObjectProperty<Effect> bottomInputProperty() {
        if (bottomInput == null) {
            bottomInput = new EffectInputProperty("bottomInput");
        }
        return bottomInput;
    }

    /**
     * The top input for this {@code Blend} operation.
     * If set to {@code null}, or left unspecified, a graphical image of
     * the {@code Node} to which the {@code Effect} is attached will be
     * used as the input.
     * @defaultValue null
     */
    private ObjectProperty<Effect> topInput;


    public final void setTopInput(Effect value) {
        topInputProperty().set(value);
    }

    public final Effect getTopInput() {
        return topInput == null ? null : topInput.get();
    }

    public final ObjectProperty<Effect> topInputProperty() {
        if (topInput == null) {
            topInput = new EffectInputProperty("topInput");
        }
        return topInput;
    }

    @Override
    boolean checkChainContains(Effect e) {
        Effect localTopInput = getTopInput();
        Effect localBottomInput = getBottomInput();
        if (localTopInput == e || localBottomInput == e)
            return true;
        if (localTopInput != null && localTopInput.checkChainContains(e))
            return true;
        if (localBottomInput != null && localBottomInput.checkChainContains(e))
            return true;

        return false;
    }

    @Override
    void update() {
        Effect localBottomInput = getBottomInput();
        Effect localTopInput = getTopInput();

        if (localTopInput != null) {
            localTopInput.sync();
        }
        if (localBottomInput != null) {
            localBottomInput.sync();
        }

        com.sun.scenario.effect.Blend peer =
                (com.sun.scenario.effect.Blend) getPeer();
        peer.setTopInput(localTopInput == null ? null : localTopInput.getPeer());
        peer.setBottomInput(localBottomInput == null ? null : localBottomInput.getPeer());
        peer.setOpacity((float)Utils.clamp(0, getOpacity(), 1));
        peer.setMode(toPGMode(getMode()));
    }

    @Override
    BaseBounds getBounds(BaseBounds bounds,
                         BaseTransform tx,
                         Node node,
                         BoundsAccessor boundsAccessor) {
        BaseBounds topBounds = new RectBounds();
        BaseBounds bottomBounds = new RectBounds();
        bottomBounds = getInputBounds(bottomBounds, tx,
                                      node, boundsAccessor,
                                      getBottomInput());
        topBounds = getInputBounds(topBounds, tx,
                                   node, boundsAccessor,
                                   getTopInput());
        BaseBounds ret = topBounds.deriveWithUnion(bottomBounds);
        return ret;
    }

    @Override
    Effect copy() {
        return new Blend(this.getMode(), this.getBottomInput(), this.getTopInput());
    }
}
