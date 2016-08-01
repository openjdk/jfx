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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;

import com.sun.javafx.util.Utils;


/**
 * A buffer that contains floating point data, intended for use as a parameter
 * to effects such as {@link DisplacementMap}.
 * @since JavaFX 2.0
 */
public class FloatMap {
    private com.sun.scenario.effect.FloatMap map;
    private float[] buf;
    private boolean mapBufferDirty = true;

    com.sun.scenario.effect.FloatMap getImpl() {
        return map;
    }

    private void updateBuffer() {
        if (getWidth() > 0 && getHeight() > 0) {
            int w = Utils.clampMax(getWidth(), 4096);
            int h = Utils.clampMax(getHeight(), 4096);
            int size = w * h * 4;
            buf = new float[size];
            mapBufferDirty = true;
        }
    }

    private void update() {
        if (mapBufferDirty) {
            map = new com.sun.scenario.effect.FloatMap(
                    Utils.clamp(1, getWidth(), 4096),
                    Utils.clamp(1, getHeight(), 4096));
            mapBufferDirty = false;
        }
        map.put(buf);
    }

    void sync() {
        if (isEffectDirty()) {
            update();
            clearDirty();
        }
    }
    private BooleanProperty effectDirty;


    private void setEffectDirty(boolean value) {
        effectDirtyProperty().set(value);
    }

    final BooleanProperty effectDirtyProperty() {
        if (effectDirty == null) {
            effectDirty = new SimpleBooleanProperty(this, "effectDirty");
        }
        return effectDirty;
    }

    boolean isEffectDirty() {
        return effectDirty == null ? false : effectDirty.get();
    }

    private void markDirty() {
        setEffectDirty(true);
    }

    private void clearDirty() {
        setEffectDirty(false);
    }

    /**
     * Creates a new instance of FloatMap with default parameters.
     */
    public FloatMap() {
        updateBuffer();
        markDirty();
    }

    /**
     * Creates a new instance of FloatMap with the specified width and height.
     * @param width the width of the map, in pixels
     * @param height the height of the map, in pixels
     * @since JavaFX 2.1
     */
    public FloatMap(int width, int height) {
        setWidth(width);
        setHeight(height);
        updateBuffer();
        markDirty();
    }

    /**
     * The width of the map, in pixels.
     * <pre>
     *       Min:    1
     *       Max: 4096
     *   Default:    1
     *  Identity:  n/a
     * </pre>
     * @defaultValue 1
     */
    private IntegerProperty width;


    public final void setWidth(int value) {
        widthProperty().set(value);
    }

    public final int getWidth() {
        return width == null ? 1 : width.get();
    }

    public final IntegerProperty widthProperty() {
        if (width == null) {
            width = new IntegerPropertyBase(1) {

                @Override
                public void invalidated() {
                    updateBuffer();
                    markDirty();
                }

                @Override
                public Object getBean() {
                    return FloatMap.this;
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
     * The height of the map, in pixels.
     * <pre>
     *       Min:    1
     *       Max: 4096
     *   Default:    1
     *  Identity:  n/a
     * </pre>
     * @defaultValue 1
     */
    private IntegerProperty height;


    public final void setHeight(int value) {
        heightProperty().set(value);
    }

    public final int getHeight() {
        return height == null ? 1 : height.get();
    }

    public final IntegerProperty heightProperty() {
        if (height == null) {
            height = new IntegerPropertyBase(1) {

                @Override
                public void invalidated() {
                    updateBuffer();
                    markDirty();
                }

                @Override
                public Object getBean() {
                    return FloatMap.this;
                }

                @Override
                public String getName() {
                    return "height";
                }
            };
        }
        return height;
    }

    /**
     * Sets the sample for a specific band at the given (x,y) location.
     *
     * @param x the x location
     * @param y the y location
     * @param band the band to set (must be 0, 1, 2, or 3)
     * @param s the sample value to set
     */
    public void setSample(int x, int y, int band, float s) {
        buf[((x+(y*getWidth()))*4) + band] = s;
        markDirty();
    }

    /**
     * Sets the sample for the first band at the given (x,y) location.
     *
     * @param x the x location
     * @param y the y location
     * @param s0 the sample value to set for the first band
     */
    public void setSamples(int x, int y, float s0)
    {
        int index = (x+(y*getWidth()))*4;
        buf[index + 0] = s0;
        markDirty();
    }

    /**
     * Sets the sample for the first two bands at the given (x,y) location.
     *
     * @param x the x location
     * @param y the y location
     * @param s0 the sample value to set for the first band
     * @param s1 the sample value to set for the second band
     */
    public void setSamples(int x, int y, float s0, float s1)
    {
        int index = (x+(y*getWidth()))*4;
        buf[index + 0] = s0;
        buf[index + 1] = s1;
        markDirty();
    }

    /**
     * Sets the sample for the first three bands at the given (x,y) location.
     *
     * @param x the x location
     * @param y the y location
     * @param s0 the sample value to set for the first band
     * @param s1 the sample value to set for the second band
     * @param s2 the sample value to set for the third band
     */
    public void setSamples(int x, int y, float s0, float s1, float s2)
    {
        int index = (x+(y*getWidth()))*4;
        buf[index + 0] = s0;
        buf[index + 1] = s1;
        buf[index + 2] = s2;
        markDirty();
    }

    /**
     * Sets the sample for each of the four bands at the given (x,y) location.
     *
     * @param x the x location
     * @param y the y location
     * @param s0 the sample value to set for the first band
     * @param s1 the sample value to set for the second band
     * @param s2 the sample value to set for the third band
     * @param s3 the sample value to set for the fourth band
     */
    public void setSamples(int x, int y,
                           float s0, float s1, float s2, float s3)
    {
        int index = (x+(y*getWidth()))*4;
        buf[index + 0] = s0;
        buf[index + 1] = s1;
        buf[index + 2] = s2;
        buf[index + 3] = s3;
        markDirty();
    }

    FloatMap copy() {
        FloatMap dest = new FloatMap(this.getWidth(), this.getHeight());
        System.arraycopy(buf, 0, dest.buf, 0, buf.length);
        return dest;
    }
}
