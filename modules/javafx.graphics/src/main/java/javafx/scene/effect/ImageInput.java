/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.image.Image;

import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.javafx.tk.Toolkit;


/**
 * A type of source effect that simply passes the given {@code Image}
 * through, unmodified, as an input to another {@code Effect}.
 * @since JavaFX 2.0
 */
public class ImageInput extends Effect {
    /**
     * Creates a new instance of ImageInput with default parameters.
     */
    public ImageInput() {}

    /**
     * Creates a new instance of ImageInput with the specified source.
     * @param source the source {@code Image}.
     * @since JavaFX 2.1
     */
    public ImageInput(Image source) {
        setSource(source);
    }

    /**
     * Creates a new instance of ImageInput with the specified source, x and y.
     * @param source the source {@code Image}.
     * @param x the x location of the source image
     * @param y the y location of the source image
     * @since JavaFX 2.1
     */
    public ImageInput(Image source, double x, double y) {
        setSource(source);
        setX(x);
        setY(y);
    }

    @Override
    com.sun.scenario.effect.Identity createPeer() {
        return new com.sun.scenario.effect.Identity(null);
    };
    /**
     * The source {@code Image}.
     */
    private ObjectProperty<Image> source;


    public final void setSource(Image value) {
        sourceProperty().set(value);
    }

    public final Image getSource() {
        return source == null ? null : source.get();
    }

    private final AbstractNotifyListener platformImageChangeListener =
        new AbstractNotifyListener() {
            @Override
            public void invalidated(Observable valueModel) {
                markDirty(EffectDirtyBits.EFFECT_DIRTY);
                effectBoundsChanged();
            }
        };
    private Image oldImage;
    public final ObjectProperty<Image> sourceProperty() {
        if (source == null) {
            source = new ObjectPropertyBase<Image>() {

                private boolean needsListeners = false;

                @Override
                public void invalidated() {
                    Image _image = get();

                    Toolkit.ImageAccessor accessor = Toolkit.getImageAccessor();

                    if (needsListeners) {
                        accessor.getImageProperty(oldImage).
                                removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (accessor.isAnimation(_image) ||
                                                        _image.getProgress() < 1);
                    oldImage = _image;

                    if (needsListeners) {
                        accessor.getImageProperty(_image).
                                addListener(platformImageChangeListener.getWeakListener());
                    }
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return ImageInput.this;
                }

                @Override
                public String getName() {
                    return "source";
                }
            };
        }
        return source;
    }

    /**
     * Sets the x location of the source image, relative to the
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
        return x == null ? 0.0 : x.get();
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
                    return ImageInput.this;
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
     * Sets the y location of the source image, relative to the
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
        return y == null ? 0.0 : y.get();
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
                    return ImageInput.this;
                }

                @Override
                public String getName() {
                    return "y";
                }
            };
        }
        return y;
    }

    @Override
    void update() {
        com.sun.scenario.effect.Identity peer =
                (com.sun.scenario.effect.Identity) getPeer();
        Image localSource = getSource();
        if (localSource != null && Toolkit.getImageAccessor().getPlatformImage(localSource) != null) {
            peer.setSource(Toolkit.getToolkit().toFilterable(localSource));
        } else {
            peer.setSource(null);
        }
        peer.setLocation(new com.sun.javafx.geom.Point2D((float)getX(), (float)getY()));
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
        Image localSource = getSource();
        if (localSource != null && Toolkit.getImageAccessor().getPlatformImage(localSource) != null) {
            float localX = (float) getX();
            float localY = (float) getY();
            float localWidth = (float) localSource.getWidth();
            float localHeight = (float) localSource.getHeight();
            BaseBounds r = new RectBounds(
                    localX, localY,
                    localX + localWidth, localY + localHeight);
            return transformBounds(tx, r);
        } else {
            return new RectBounds();
        }
    }

    @Override
    Effect copy() {
        return new ImageInput(this.getSource(), this.getX(), this.getY());
    }
}
