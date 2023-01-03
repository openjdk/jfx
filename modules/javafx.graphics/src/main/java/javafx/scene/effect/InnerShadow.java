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
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import com.sun.javafx.util.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.javafx.tk.Toolkit;


/**
 * A high-level effect that renders a shadow inside the edges of the
 * given content with the specified color, radius, and offset.
 *
 * <p>
 * Example:
 * <pre>{@code
 * InnerShadow innerShadow = new InnerShadow();
 * innerShadow.setOffsetX(4);
 * innerShadow.setOffsetY(4);
 * innerShadow.setColor(Color.web("0x3b596d"));
 *
 * Text text = new Text();
 * text.setEffect(innerShadow);
 * text.setX(20);
 * text.setY(100);
 * text.setText("InnerShadow");
 * text.setFill(Color.ALICEBLUE);
 * text.setFont(Font.font(null, FontWeight.BOLD, 50));
 * }</pre>
 * <p> The code above produces the following: </p>
 * <p>
 * <img src="doc-files/innershadow.png" alt="The visual effect of InnerShadow on
 * text">
 * </p>
 * @since JavaFX 2.0
 */
public class InnerShadow extends Effect {
    private boolean changeIsLocal;

    /**
     * Creates a new instance of InnerShadow with default parameters.
     */
    public InnerShadow() {}

    /**
     * Creates a new instance of InnerShadow with specified radius and color.
     * @param radius the radius of the shadow blur kernel
     * @param color the shadow {@code Color}
     */
    public InnerShadow(double radius, Color color) {
        setRadius(radius);
        setColor(color);
    }

    /**
     * Creates a new instance of InnerShadow with specified radius, offsetX,
     * offsetY and color.
     * @param radius the radius of the shadow blur kernel
     * @param offsetX the shadow offset in the x direction
     * @param offsetY the shadow offset in the y direction
     * @param color the shadow {@code Color}
     */
    public InnerShadow(double radius, double offsetX, double offsetY, Color color) {
        setRadius(radius);
        setOffsetX(offsetX);
        setOffsetY(offsetY);
        setColor(color);
    }

    /**
     * Creates a new instance of InnerShadow with the specified blurType, color,
     * radius, spread, offsetX and offsetY.
     * @param blurType the algorithm used to blur the shadow
     * @param color the shadow {@code Color}
     * @param radius the radius of the shadow blur kernel
     * @param choke the portion of the radius where the contribution of
     * the source material will be 100%
     * @param offsetX the shadow offset in the x direction
     * @param offsetY the shadow offset in the y direction
     * @since JavaFX 2.1
     */
    public InnerShadow(BlurType blurType, Color color, double radius, double choke,
            double offsetX, double offsetY) {
        setBlurType(blurType);
        setColor(color);
        setRadius(radius);
        setChoke(choke);
        setOffsetX(offsetX);
        setOffsetY(offsetY);
    }

    @Override
    com.sun.scenario.effect.InnerShadow createPeer() {
        return new com.sun.scenario.effect.InnerShadow();
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
     * The radius of the shadow blur kernel.
     * This attribute controls the distance that the shadow is spread
     * to each side of the source pixels.
     * Setting the radius is equivalent to setting both the {@code width}
     * and {@code height} attributes to a value of {@code (2 * radius + 1)}.
     * <pre>
     *       Min:   0.0
     *       Max: 127.0
     *   Default:  10.0
     *  Identity:   0.0
     * </pre>
     * @defaultValue 10.0
     */
    private DoubleProperty radius;


    public final void setRadius(double value) {
        radiusProperty().set(value);
    }

    public final double getRadius() {
        return radius == null ? 10 : radius.get();
    }

    public final DoubleProperty radiusProperty() {
        if (radius == null) {
            radius = new DoublePropertyBase(10) {

                @Override
                public void invalidated() {
                    // gettter here is necessary to make the property valid
                    double localRadius = getRadius();
                    if (!changeIsLocal) {
                        changeIsLocal = true;
                        updateRadius(localRadius);
                        changeIsLocal = false;
                        markDirty(EffectDirtyBits.EFFECT_DIRTY);
                        effectBoundsChanged();
                    }
                }

                @Override
                public Object getBean() {
                    return InnerShadow.this;
                }

                @Override
                public String getName() {
                    return "radius";
                }
            };
        }
        return radius;
    }

    private void updateRadius(double value) {
        double newdim = (value * 2 + 1);
        if (width != null && width.isBound()) {
            if (height == null || !height.isBound()) {
                setHeight(newdim * 2 - getWidth());
            }
        } else if (height != null && height.isBound()) {
            setWidth(newdim * 2 - getHeight());
        } else {
            setWidth(newdim);
            setHeight(newdim);
        }
    }

    /**
     * The horizontal size of the shadow blur kernel.
     * This attribute controls the horizontal size of the total area over
     * which the shadow of a single pixel is distributed by the blur algorithm.
     * Values less than {@code 1.0} are not distributed beyond the original
     * pixel and so have no blurring effect on the shadow.
     * <pre>
     *       Min:   0.0
     *       Max: 255.0
     *   Default:  21.0
     *  Identity:  &lt;1.0
     * </pre>
     * @defaultValue 21.0
     */
    private DoubleProperty width;


    public final void setWidth(double value) {
        widthProperty().set(value);
    }

    public final double getWidth() {
        return width == null ? 21 : width.get();
    }

    public final DoubleProperty widthProperty() {
        if (width == null) {
            width = new DoublePropertyBase(21) {

                @Override
                public void invalidated() {
                    // gettter here is necessary to make the property valid
                    double localWidth = getWidth();
                    if (!changeIsLocal) {
                        changeIsLocal = true;
                        updateWidth(localWidth);
                        changeIsLocal = false;
                        markDirty(EffectDirtyBits.EFFECT_DIRTY);
                        effectBoundsChanged();
                    }
                }

                @Override
                public Object getBean() {
                    return InnerShadow.this;
                }

                @Override
                public String getName() {
                    return "width";
                }
            };
        }
        return width;
    }

    private void updateWidth(double value) {
        if (radius == null || !radius.isBound()) {
            double newrad = ((value + getHeight()) / 2);
            newrad = ((newrad - 1) / 2);
            if (newrad < 0) {
                newrad = 0;
            }
            setRadius(newrad);
        } else {
            if (height == null || !height.isBound()) {
                double newdim = (getRadius() * 2 + 1);
                setHeight(newdim * 2 - value);
            }
        }
    }

    /**
     * The vertical size of the shadow blur kernel.
     * This attribute controls the vertical size of the total area over
     * which the shadow of a single pixel is distributed by the blur algorithm.
     * Values less than {@code 1.0} are not distributed beyond the original
     * pixel and so have no blurring effect on the shadow.
     * <pre>
     *       Min:   0.0
     *       Max: 255.0
     *   Default:  21.0
     *  Identity:  &lt;1.0
     * </pre>
     * @defaultValue 21.0
     */
    private DoubleProperty height;


    public final void setHeight(double value) {
        heightProperty().set(value);
    }

    public final double getHeight() {
        return height == null ? 21 : height.get();
    }

    public final DoubleProperty heightProperty() {
        if (height == null) {
            height = new DoublePropertyBase(21) {

                @Override
                public void invalidated() {
                    // gettter here is necessary to make the property valid
                    double localHeight = getHeight();
                    if (!changeIsLocal) {
                        changeIsLocal = true;
                        updateHeight(localHeight);
                        changeIsLocal = false;
                        markDirty(EffectDirtyBits.EFFECT_DIRTY);
                        effectBoundsChanged();
                    }
                }

                @Override
                public Object getBean() {
                    return InnerShadow.this;
                }

                @Override
                public String getName() {
                    return "height";
                }
            };
        }
        return height;
    }
    private void updateHeight(double value) {
        if (radius == null || !radius.isBound()) {
            double newrad = ((getWidth() + value) / 2);
            newrad = ((newrad - 1) / 2);
            if (newrad < 0) {
                newrad = 0;
            }
            setRadius(newrad);
        } else {
            if (width == null || !width.isBound()) {
                double newdim = (getRadius() * 2 + 1);
                setWidth(newdim * 2 - value);
            }
        }
    }

    /**
     * The algorithm used to blur the shadow.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: BlurType.THREE_PASS_BOX
     *  Identity: n/a
     * </pre>
     * @defaultValue THREE_PASS_BOX
     */
    private ObjectProperty<BlurType> blurType;


    public final void setBlurType(BlurType value) {
        blurTypeProperty().set(value);
    }

    public final BlurType getBlurType() {
        return blurType == null ? BlurType.THREE_PASS_BOX : blurType.get();
    }

    public final ObjectProperty<BlurType> blurTypeProperty() {
        if (blurType == null) {
            blurType = new ObjectPropertyBase<BlurType>(BlurType.THREE_PASS_BOX) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return InnerShadow.this;
                }

                @Override
                public String getName() {
                    return "blurType";
                }
            };
        }
        return blurType;
    }

    /**
     * The choke of the shadow.
     * The choke is the portion of the radius where the contribution of
     * the source material will be 100%.
     * The remaining portion of the radius will have a contribution
     * controlled by the blur kernel.
     * A choke of {@code 0.0} will result in a distribution of the
     * shadow determined entirely by the blur algorithm.
     * A choke of {@code 1.0} will result in a solid growth inward of the
     * shadow from the edges to the limit of the radius with a very sharp
     * cutoff to transparency inside the radius.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty choke;


    public final void setChoke(double value) {
        chokeProperty().set(value);
    }

    public final double getChoke() {
        return choke == null ? 0 : choke.get();
    }

    public final DoubleProperty chokeProperty() {
        if (choke == null) {
            choke = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return InnerShadow.this;
                }

                @Override
                public String getName() {
                    return "choke";
                }
            };
        }
        return choke;
    }

    /**
     * The shadow {@code Color}.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: Color.BLACK
     *  Identity: n/a
     * </pre>
     * @defaultValue BLACK
     */
    private ObjectProperty<Color> color;


    public final void setColor(Color value) {
        colorProperty().set(value);
    }

    public final Color getColor() {
        return color == null ? Color.BLACK : color.get();
    }

    public final ObjectProperty<Color> colorProperty() {
        if (color == null) {
            color = new ObjectPropertyBase<Color>(Color.BLACK) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return InnerShadow.this;
                }

                @Override
                public String getName() {
                    return "color";
                }
            };
        }
        return color;
    }

    /**
     * The shadow offset in the x direction, in pixels.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty offsetX;


    public final void setOffsetX(double value) {
        offsetXProperty().set(value);
    }

    public final double getOffsetX() {
        return offsetX == null ? 0 : offsetX.get();
    }

    public final DoubleProperty offsetXProperty() {
        if (offsetX == null) {
            offsetX = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return InnerShadow.this;
                }

                @Override
                public String getName() {
                    return "offsetX";
                }
            };
        }
        return offsetX;
    }

    /**
     * The shadow offset in the y direction, in pixels.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty offsetY;


    public final void setOffsetY(double value) {
        offsetYProperty().set(value);
    }

    public final double getOffsetY() {
        return offsetY == null ? 0 : offsetY.get();
    }

    public final DoubleProperty offsetYProperty() {
        if (offsetY == null) {
            offsetY = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return InnerShadow.this;
                }

                @Override
                public String getName() {
                    return "offsetY";
                }
            };
        }
        return offsetY;
    }

    private Color getColorInternal() {
        Color c = getColor();
        return c == null ? Color.BLACK : c;
    }

    private BlurType getBlurTypeInternal() {
        BlurType bt = getBlurType();
        return bt == null ? BlurType.THREE_PASS_BOX : bt;
    }

    @Override
    void update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.sync();
        }

        com.sun.scenario.effect.InnerShadow peer =
                (com.sun.scenario.effect.InnerShadow) getPeer();
        peer.setShadowSourceInput(localInput == null ? null : localInput.getPeer());
        peer.setContentInput(localInput == null ? null : localInput.getPeer());
        peer.setGaussianWidth((float)Utils.clamp(0, getWidth(), 255));
        peer.setGaussianHeight((float)Utils.clamp(0, getHeight(), 255));
        peer.setShadowMode(Toolkit.getToolkit().toShadowMode(getBlurTypeInternal()));
        peer.setColor(Toolkit.getToolkit().toColor4f(getColorInternal()));
        peer.setChoke((float)Utils.clamp(0, getChoke(), 1));
        peer.setOffsetX((int) getOffsetX());
        peer.setOffsetY((int) getOffsetY());
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
        InnerShadow is = new InnerShadow(this.getBlurType(), this.getColor(),
                this.getRadius(), this.getChoke(), this.getOffsetX(),
                this.getOffsetY());
        is.setInput(this.getInput());
        is.setWidth(this.getWidth());
        is.setHeight(this.getHeight());
        return is;
    }
}
