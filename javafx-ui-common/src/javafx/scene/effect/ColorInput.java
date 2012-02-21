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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.effect.EffectUtils;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;


/**
 * An effect that renders a rectangular region that is filled ("flooded")
 * with the given {@code Paint}.  This is equivalent to rendering a
 * filled rectangle into an image and using an {@code ImageInput} effect,
 * except that it is more convenient and potentially much more efficient.
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
    com.sun.scenario.effect.Flood impl_createImpl() {
        return new com.sun.scenario.effect.Flood(Color.RED.impl_getPlatformPaint());
    };
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

    @Override
    void impl_update() {
        com.sun.scenario.effect.Flood peer =
                (com.sun.scenario.effect.Flood) impl_getImpl();
        peer.setPaint(getPaint().impl_getPlatformPaint());
        peer.setFloodBounds(new RectBounds(
                (float)getX(), (float)getY(),
                (float)(getX() + getWidth()),
                (float)(getY() + getHeight())));
    }

    @Override
    boolean impl_checkChainContains(Effect e) {
        return false;
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
        RectBounds ret = new RectBounds(
                (float)getX(), (float)getY(),
                (float)(getX() + getWidth()),
                (float)(getY() + getHeight()));
        return EffectUtils.transformBounds(tx, ret);
    }
}
