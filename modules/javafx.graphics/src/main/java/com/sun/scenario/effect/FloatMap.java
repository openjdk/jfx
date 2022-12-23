/*
 * Copyright (c) 2008, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import com.sun.scenario.effect.impl.Renderer;

/**
 * A buffer that contains floating point data, intended for use as a parameter
 * to effects such as {@code DisplacementMap}.
 */
public class FloatMap {

    private final int width;
    private final int height;
    private final FloatBuffer buf;
    private boolean cacheValid;

    /**
     * Constructs a new {@code FloatMap} of the given width and height.
     *
     * @param width the width of the map, in pixels
     * @param height the height of the map, in pixels
     * @throws IllegalArgumentException if either {@code width} or
     * {@code height} is outside the range [1, 4096]
     */
    public FloatMap(int width, int height) {
        if (width <= 0 || width > 4096 || height <= 0 || height > 4096) {
            throw new IllegalArgumentException("Width and height must be in the range [1, 4096]");
        }
        this.width = width;
        this.height = height;
        int size = width * height * 4;
        // We allocate a heap-based (indirect) FloatBuffer here because
        // some Decora backends aren't yet prepared to deal with direct
        // FloatBuffers (and to that end we've exposed the getData() method,
        // whose return value is float[]).  Note that we use wrap() instead of
        // allocate() since the latter is not supported on CDC (specifically
        // the NIO subset from JSR 239).  The byte order of the FloatBuffer
        // will be the native order of the underlying hardware, which is what
        // the various Decora backends expect.
        this.buf = FloatBuffer.wrap(new float[size]);
    }

    /**
     * Returns the width of the map, in pixels.
     *
     * @return the width of the map, in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the map, in pixels.
     *
     * @return the height of the map, in pixels
     */
    public int getHeight() {
        return height;
    }

    public float[] getData() {
        // we only use heap-based FloatBuffers (see comment in constructor
        // above) so the following is safe
        return buf.array();
    }

    public FloatBuffer getBuffer() {
        return buf;
    }

    /**
     * Gets the sample for a specific band from the given {@code (x,y)} location.
     *
     * @param x the x location
     * @param y the y location
     * @param band the band to get (must be 1, 2, 3, or 4)
     * @return the sample of the specified band at the specified location
     */
    public float getSample(int x, int y, int band) {
        return buf.get(((x+(y*width))*4)+band);
    }

    /**
     * Sets the sample for a specific band at the given (x,y) location.
     *
     * @param x the x location
     * @param y the y location
     * @param band the band to set (must be 1, 2, 3, or 4)
     * @param sample the sample value to set
     */
    public void setSample(int x, int y, int band, float sample) {
        buf.put(((x+(y*width))*4)+band, sample);
        cacheValid = false;
    }

    /**
     * Sets the sample for the first band at the given (x,y) location.
     *
     * @param x the x location
     * @param y the y location
     * @param s0 the sample value to set for the first band
     */
    public void setSamples(int x, int y, float s0) {
        int index = (x+(y*width))*4;
        buf.put(index+0, s0);
        cacheValid = false;
    }

    /**
     * Sets the sample for the first two bands at the given (x,y) location.
     *
     * @param x the x location
     * @param y the y location
     * @param s0 the sample value to set for the first band
     * @param s1 the sample value to set for the second band
     */
    public void setSamples(int x, int y, float s0, float s1) {
        int index = (x+(y*width))*4;
        buf.put(index+0, s0);
        buf.put(index+1, s1);
        cacheValid = false;
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
    public void setSamples(int x, int y, float s0, float s1, float s2) {
        int index = (x+(y*width))*4;
        buf.put(index+0, s0);
        buf.put(index+1, s1);
        buf.put(index+2, s2);
        cacheValid = false;
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
    public void setSamples(int x, int y, float s0, float s1, float s2, float s3) {
        int index = (x+(y*width))*4;
        buf.put(index+0, s0);
        buf.put(index+1, s1);
        buf.put(index+2, s2);
        buf.put(index+3, s3);
        cacheValid = false;
    }

    public void put(float[] floatBuf) {
        buf.rewind();
        buf.put(floatBuf);
        buf.rewind();
        cacheValid = false;
    }

    private Map<FilterContext, Entry> cache;

    public LockableResource getAccelData(FilterContext fctx) {
        if (cache == null) {
            cache = new HashMap<>();
        } else if (!cacheValid) {
            for (Entry entry : cache.values()) {
                entry.valid = false;
            }
            cacheValid = true;
        }

        // RT-27553
        // TODO: ideally this method wouldn't be public in the first place,
        // but even worse, we're assuming that it is called on the QFT from
        // HWTwoSamplerPeer.filter()...
        Renderer renderer = Renderer.getRenderer(fctx);
        Entry entry = cache.get(fctx);
        if (entry != null) {
            entry.texture.lock();
            if (entry.texture.isLost()) {
                entry.texture.unlock();
                cache.remove(fctx);
                entry = null;
            }
        }
        if (entry == null) {
            LockableResource texture = renderer.createFloatTexture(width, height);
            entry = new Entry(texture);
            cache.put(fctx, entry);
        }
        if (!entry.valid) {
            renderer.updateFloatTexture(entry.texture, this);
            entry.valid = true;
        }

        return entry.texture;
    }

    private static class Entry {
        LockableResource texture;
        boolean valid;
        Entry(LockableResource texture) {
            this.texture = texture;
        }
    }
}
