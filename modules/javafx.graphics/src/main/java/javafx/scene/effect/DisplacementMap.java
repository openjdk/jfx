/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Node;

import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;

/**
 * An effect that shifts each pixel by a distance specified by
 * the first two bands of of the specified {@link FloatMap}.
 * For each pixel in the output, the corresponding data from the
 * {@code mapData} is retrieved, scaled and offset by the {@code scale}
 * and {@code offset} attributes, scaled again by the size of the
 * source input image and used as an offset from the destination pixel
 * to retrieve the pixel data from the source input.
 * <p>
 * {@code dst[x, y] = src[(x, y) + (offset + scale * map[x, y]) * (srcw, srch)]}
 * <p>
 * A value of {@code (0.0, 0.0)} would specify no offset for the
 * pixel data whereas a value of {@code (0.5, 0.5)} would specify
 * an offset of half of the source image size.
 * <p>
 * <b>Note</b> that the mapping is the offset from a destination pixel to
 * the source pixel location from which it is sampled which means that
 * filling the map with all values of {@code 0.5} would displace the
 * image by half of its size towards the upper left since each destination
 * pixel would contain the data that comes from the source pixel below and
 * to the right of it.
 * </p>
 * <p>
 * Also note that this effect does not adjust the coordinates of input
 * events or any methods that measure containment on a {@code Node}.
 * The results of mouse picking and the containment methods are undefined
 * when a {@code Node} has a {@code DisplacementMap} effect in place.
 * </p>
 * <p>
 * Example:
 * <pre>{@code  int width = 220;
 * int height = 100;
 *
 * FloatMap floatMap = new FloatMap();
 * floatMap.setWidth(width);
 * floatMap.setHeight(height);
 *
 * for (int i = 0; i < width; i++) {
 *     double v = (Math.sin(i / 20.0 * Math.PI) - 0.5) / 40.0;
 *     for (int j = 0; j < height; j++) {
 *         floatMap.setSamples(i, j, 0.0f, (float) v);
 *     }
 * }
 *
 * DisplacementMap displacementMap = new DisplacementMap();
 * displacementMap.setMapData(floatMap);
 *
 * Text text = new Text();
 * text.setX(40.0);
 * text.setY(80.0);
 * text.setText("Wavy Text");
 * text.setFill(Color.web("0x3b596d"));
 * text.setFont(Font.font(null, FontWeight.BOLD, 50));
 * text.setEffect(displacementMap);}</pre>
 *
 * <p> The code above produces the following: </p>
 * <p> <img src="doc-files/displacementmap.png" alt="The visual effect of
 * DisplacementMap on text"> </p>
 * @since JavaFX 2.0
 */
public class DisplacementMap extends Effect {
    @Override
    com.sun.scenario.effect.DisplacementMap createPeer() {
        return new com.sun.scenario.effect.DisplacementMap(
                            new com.sun.scenario.effect.FloatMap(1, 1),
                            com.sun.scenario.effect.Effect.DefaultInput);
    };

    /**
     * Creates a new instance of DisplacementMap with default parameters.
     */
    public DisplacementMap() {
        setMapData(new FloatMap(1, 1));
    }

    /**
     * Creates a new instance of DisplacementMap with the specified mapData.
     * @param mapData the map data for this displacement map effect
     * @since JavaFX 2.1
     */
    public DisplacementMap(FloatMap mapData) {
        setMapData(mapData);
    }

    /**
     * Creates a new instance of DisplacementMap with the specified mapData,
     * offsetX, offsetY, scaleX, and scaleY.
     * @param mapData the map data for this displacement map effect
     * @param offsetX the offset by which all x coordinate offset values in the
     * {@code FloatMap} are displaced after they are scaled
     * @param offsetY the offset by which all y coordinate offset values in the
     * {@code FloatMap} are displaced after they are scaled
     * @param scaleX the scale factor by which all x coordinate offset values in the
     * {@code FloatMap} are multiplied
     * @param scaleY the scale factor by which all y coordinate offset values in the
     * {@code FloatMap} are multiplied
     * @since JavaFX 2.1
     */
    public DisplacementMap(FloatMap mapData,
                           double offsetX, double offsetY,
                           double scaleX, double scaleY) {
        setMapData(mapData);
        setOffsetX(offsetX);
        setOffsetY(offsetY);
        setScaleX(scaleX);
        setScaleY(scaleY);
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

    private final FloatMap defaultMap = new FloatMap(1, 1);

    /**
     * The map data for this {@code Effect}.
     * @defaultValue an empty map
     */
    private ObjectProperty<FloatMap> mapData;


    public final void setMapData(FloatMap value) {
        mapDataProperty().set(value);
    }

    public final FloatMap getMapData() {
        return mapData == null ? null : mapData.get();
    }

    public final ObjectProperty<FloatMap> mapDataProperty() {
        if (mapData == null) {
            mapData = new ObjectPropertyBase<FloatMap>() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return DisplacementMap.this;
                }

                @Override
                public String getName() {
                    return "mapData";
                }
            };
        }
        return mapData;
    }

    private final MapDataChangeListener mapDataChangeListener = new MapDataChangeListener();

    private class MapDataChangeListener extends EffectChangeListener {
        FloatMap mapData;

        public void register(FloatMap value) {
            mapData = value;
            super.register(mapData == null ? null : mapData.effectDirtyProperty());
        }

        @Override
        public void invalidated(Observable valueModel) {
            if (mapData.isEffectDirty()) {
                markDirty(EffectDirtyBits.EFFECT_DIRTY);
                effectBoundsChanged();
            }
        }
    };

    /**
     * The scale factor by which all x coordinate offset values in the
     * {@code FloatMap} are multiplied.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 1.0
     *  Identity: 1.0
     * </pre>
     * @defaultValue 1.0
     */
    private DoubleProperty scaleX;


    public final void setScaleX(double value) {
        scaleXProperty().set(value);
    }

    public final double getScaleX() {
        return scaleX == null ? 1 : scaleX.get();
    }

    public final DoubleProperty scaleXProperty() {
        if (scaleX == null) {
            scaleX = new DoublePropertyBase(1) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return DisplacementMap.this;
                }

                @Override
                public String getName() {
                    return "scaleX";
                }
            };
        }
        return scaleX;
    }

    /**
     * The scale factor by which all y coordinate offset values in the
     * {@code FloatMap} are multiplied.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: 1.0
     *  Identity: 1.0
     * </pre>
     * @defaultValue 1.0
     */
    private DoubleProperty scaleY;


    public final void setScaleY(double value) {
        scaleYProperty().set(value);
    }

    public final double getScaleY() {
        return scaleY == null ? 1 : scaleY.get();
    }

    public final DoubleProperty scaleYProperty() {
        if (scaleY == null) {
            scaleY = new DoublePropertyBase(1) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return DisplacementMap.this;
                }

                @Override
                public String getName() {
                    return "scaleY";
                }
            };
        }
        return scaleY;
    }

    /**
     * The offset by which all x coordinate offset values in the
     * {@code FloatMap} are displaced after they are scaled.
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
                }

                @Override
                public Object getBean() {
                    return DisplacementMap.this;
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
     * The offset by which all y coordinate offset values in the
     * {@code FloatMap} are displaced after they are scaled.
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
                }

                @Override
                public Object getBean() {
                    return DisplacementMap.this;
                }

                @Override
                public String getName() {
                    return "offsetY";
                }
            };
        }
        return offsetY;
    }

    /**
     * Defines whether values taken from outside the edges of the map
     * "wrap around" or not.
     * <pre>
     *       Min:  n/a
     *       Max:  n/a
     *   Default: false
     *  Identity:  n/a
     * </pre>
     * @defaultValue false
     */
    private BooleanProperty wrap;


    public final void setWrap(boolean value) {
        wrapProperty().set(value);
    }

    public final boolean isWrap() {
        return wrap == null ? false : wrap.get();
    }

    public final BooleanProperty wrapProperty() {
        if (wrap == null) {
            wrap = new BooleanPropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return DisplacementMap.this;
                }

                @Override
                public String getName() {
                    return "wrap";
                }
            };
        }
        return wrap;
    }

    @Override
    void update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.sync();
        }

        com.sun.scenario.effect.DisplacementMap peer =
                (com.sun.scenario.effect.DisplacementMap) getPeer();
        peer.setContentInput(localInput == null ? null : localInput.getPeer());
        FloatMap localMapData = getMapData();
        mapDataChangeListener.register(localMapData);
        if (localMapData != null) {
            localMapData.sync();
            peer.setMapData(localMapData.getImpl());
        } else {
            defaultMap.sync();
            peer.setMapData(defaultMap.getImpl());
        }
        peer.setScaleX((float)getScaleX());
        peer.setScaleY((float)getScaleY());
        peer.setOffsetX((float)getOffsetX());
        peer.setOffsetY((float)getOffsetY());
        peer.setWrap(isWrap());
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
        return transformBounds(tx, bounds);
    }

    @Override
    Effect copy() {
        DisplacementMap dm = new DisplacementMap(this.getMapData().copy(),
                this.getOffsetX(), this.getOffsetY(), this.getScaleX(),
                this.getScaleY());
        dm.setInput(this.getInput());
        return dm;
    }
}
