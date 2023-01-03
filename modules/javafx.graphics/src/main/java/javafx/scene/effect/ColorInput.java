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
import javafx.scene.paint.Paint;

import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.javafx.tk.Toolkit;


/**
 * An effect that renders a rectangular region that is filled ("flooded")
 * with the given {@code Paint}.  This is equivalent to rendering a
 * filled rectangle into an image and using an {@code ImageInput} effect,
 * except that it is more convenient and potentially much more efficient.
 * @since JavaFX 2.0
 */
public class ColorInput extends Effect {
    /**
     * Creates a new instance of ColorInput with default parameters.
     */
    public ColorInput() {}

    /**
     * Creates a new instance of ColorInput with the specified x, y, width,
     * height, and paint.
     * @param x the x location of the region to be flooded
     * @param y the y location of the region to be flooded
     * @param width the width of the region to be flooded
     * @param height the height of the region to be flooded
     * @param paint the {@code Paint} used to flood the region
     * @since JavaFX 2.1
     */
    public ColorInput(double x,
                      double y,
                      double width,
                      double height,
                      Paint paint) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        setPaint(paint);
    }

    @Override
    com.sun.scenario.effect.Flood createPeer() {
        return new com.sun.scenario.effect.Flood(
                Toolkit.getPaintAccessor().getPlatformPaint(Color.RED));
    }
    /**
     * The {@code Paint} used to flood the region.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: Color.RED
     *  Identity: n/a
     * </pre>
     * @defaultValue RED
     */
    private ObjectProperty<Paint> paint;


    public final void setPaint(Paint value) {
        paintProperty().set(value);
    }

    public final Paint getPaint() {
        return paint == null ? Color.RED : paint.get();
    }

    public final ObjectProperty<Paint> paintProperty() {
        if (paint == null) {
            paint = new ObjectPropertyBase<Paint>(Color.RED) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return ColorInput.this;
                }

                @Override
                public String getName() {
                    return "paint";
                }
            };
        }
        return paint;
    }

    /**
     * Sets the x location of the region to be flooded, relative to the
     * local coordinate space of the content {@code Node}.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty x;


    public final void setX(double value) {
        xProperty().set(value);
    }

    public final double getX() {
        return x == null ? 0 : x.get();
    }

    public final DoubleProperty xProperty() {
        if (x == null) {
            x = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return ColorInput.this;
                }

                @Override
                public String getName() {
                    return "x";
                }
            };
        }
        return x;
    }

    /**
     * Sets the y location of the region to be flooded, relative to the
     * local coordinate space of the content {@code Node}.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty y;


    public final void setY(double value) {
        yProperty().set(value);
    }

    public final double getY() {
        return y == null ? 0 : y.get();
    }

    public final DoubleProperty yProperty() {
        if (y == null) {
            y = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return ColorInput.this;
                }

                @Override
                public String getName() {
                    return "y";
                }
            };
        }
        return y;
    }

    /**
     * Sets the width of the region to be flooded, relative to the
     * local coordinate space of the content {@code Node}.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty width;


    public final void setWidth(double value) {
        widthProperty().set(value);
    }

    public final double getWidth() {
        return width == null ? 0 : width.get();
    }

    public final DoubleProperty widthProperty() {
        if (width == null) {
            width = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return ColorInput.this;
                }

                @Override
                public String getName() {
                    return "width";
                }
            };
        }
        return width;
    }

    /**
     * Sets the height of the region to be flooded, relative to the
     * local coordinate space of the content {@code Node}.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 0.0
     *  Identity: 0.0
     * </pre>
     * @defaultValue 0.0
     */
    private DoubleProperty height;


    public final void setHeight(double value) {
        heightProperty().set(value);
    }

    public final double getHeight() {
        return height == null ? 0 : height.get();
    }

    public final DoubleProperty heightProperty() {
        if (height == null) {
            height = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return ColorInput.this;
                }

                @Override
                public String getName() {
                    return "height";
                }
            };
        }
        return height;
    }

    private Paint getPaintInternal() {
        Paint p = getPaint();
        return p == null ? Color.RED : p;
    }

    @Override
    void update() {
        com.sun.scenario.effect.Flood peer =
                (com.sun.scenario.effect.Flood) getPeer();
        peer.setPaint(Toolkit.getPaintAccessor().getPlatformPaint(getPaintInternal()));
        peer.setFloodBounds(new RectBounds(
                (float)getX(), (float)getY(),
                (float)(getX() + getWidth()),
                (float)(getY() + getHeight())));
    }

    @Override
    boolean checkChainContains(Effect e) {
        return false;
    }

    @Override
    BaseBounds getBounds(BaseBounds bounds,
                         BaseTransform tx,
                         Node node,
                         BoundsAccessor boundsAccessor) {
        RectBounds ret = new RectBounds(
                (float)getX(), (float)getY(),
                (float)(getX() + getWidth()),
                (float)(getY() + getHeight()));
        return transformBounds(tx, ret);
    }

    @Override
    Effect copy() {
        return new ColorInput(this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.getPaint());
    }
}
