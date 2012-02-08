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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Node;

import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.effect.EffectUtils;
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
 * <pre>
 *     dst[x,y] = src[(x,y) + (offset+scale*map[x,y])*(srcw,srch)]
 * </pre>
 * A value of {@code (0.0,&nbsp;0.0)} would specify no offset for the
 * pixel data whereas a value of {@code (0.5,&nbsp;0.5)} would specify
 * an offset of half of the source image size.
 * <p>
 * <b>Note</b> that the mapping is the offset from a destination pixel to
 * the source pixel location from which it is sampled which means that
 * filling the map with all values of {@code 0.5} would displace the
 * image by half of its size towards the upper left since each destination
 * pixel would contain the data that comes from the source pixel below and
 * to the right of it.
 * <p>
 * Also note that this effect does not adjust the coordinates of input
 * events or any methods that measure containment on a {@code Node}.
 * The results of mouse picking and the containment methods are undefined
 * when a {@code Node} has a {@code DisplacementMap} effect in place.
 *
<PRE>
import javafx.scene.*;
import javafx.scene.text.*;
import javafx.scene.shape.*;
import javafx.scene.paint.*;
import javafx.scene.effect.*;

int w = 220;
int h = 100;
FloatMap map = new FloatMap();
map.setWidth(w);
map.setHeight(h);

for (int i = 0; i < w; i++) {
    double v = (Math.sin(i/50.0*Math.PI)-0.5)/40.0;
    for (int j = 0; j < h; j++) {
        map.setSamples(i, j, 0.0f,(float) v);
    }
}

Group g = new Group();
DisplacementMap dm = new DisplacementMap();
dm.setMapData(map);

g.setEffect(dm);
g.setCache(true);

Rectangle r = new Rectangle();
r.setX(20.0);
r.setY(20.0);
r.setWidth(w);
r.setHeight(h);
r.setFill(Color.BLUE);

g.getChildren().add(r);

Text t = new Text();
t.setX(40.0);
t.setY(80.0);
t.setText("Wavy Text");
t.setFill(Color.YELLOW);
t.setFont(Font.font(null, FontWeight.BOLD, 36));

g.getChildren().add(t);
</PRE>
 *
 * @profile common conditional effect
 */
public class DisplacementMap extends Effect {
    @Override
    com.sun.scenario.effect.DisplacementMap impl_createImpl() {
        return new com.sun.scenario.effect.DisplacementMap(
                            new com.sun.scenario.effect.FloatMap(1, 1),
                            com.sun.scenario.effect.Effect.DefaultInput);
    };

    /**
     * Creates a new instance of DisplacementMap with default parameters.
     */
    public DisplacementMap() {
        FloatMap fm = new FloatMap();
        fm.setWidth(1);
        fm.setHeight(1);
        setMapData(fm);
    }

    /**
     * Creates a new instance of DisplacementMap with the specified mapData.
     * @param mapData the map data for this displacement map effect
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
     * @defaultvalue null
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
    boolean impl_checkChainContains(Effect e) {
        Effect localInput = getInput();
        if (localInput == null)
            return false;
        if (localInput == e)
            return true;
        return localInput.impl_checkChainContains(e);
    }

    /**
     * The map data for this {@code Effect}.
     * @defaultvalue an empty map
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
            if (mapData.impl_isEffectDirty()) {
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
     * @defaultvalue 1.0
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
     * @defaultvalue 1.0
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
     * @defaultvalue 0.0
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
     * @defaultvalue 0.0
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
     * @defaultvalue false
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
    void impl_update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.impl_sync();
        }

        com.sun.scenario.effect.DisplacementMap peer =
                (com.sun.scenario.effect.DisplacementMap) impl_getImpl();
        peer.setContentInput(localInput == null ? null : localInput.impl_getImpl());
        FloatMap localMapData = getMapData();
        mapDataChangeListener.register(localMapData);
        if (localMapData != null) {
            localMapData.impl_sync();
            peer.setMapData(localMapData.getImpl());
        } else {
            peer.setMapData(null);
        }
        peer.setScaleX((float)getScaleX());
        peer.setScaleY((float)getScaleY());
        peer.setOffsetX((float)getOffsetX());
        peer.setOffsetY((float)getOffsetY());
        peer.setWrap(isWrap());
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_getBounds(BaseBounds bounds,
                                     BaseTransform tx,
                                     Node node,
                                     BoundsAccessor boundsAccessor) {
        bounds = EffectUtils.getInputBounds(bounds,
                                            BaseTransform.IDENTITY_TRANSFORM,
                                            node, boundsAccessor,
                                            getInput());
        return EffectUtils.transformBounds(tx, bounds);
    }
}
