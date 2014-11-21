/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import javafx.scene.layout.Background;
import java.util.HashMap;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.logging.PulseLogger;
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;
import com.sun.prism.Graphics;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.packrect.RectanglePacker;

/**
 * RegionImageCache - A fixed pixel count sized cache of Images keyed by arbitrary set of arguments. All images are held with
 * SoftReferences so they will be dropped by the GC if heap memory gets tight. When our size hits max pixel count least
 * recently requested images are removed first.
 *
 */
class RegionImageCache {

    // Maximum cached image size in pixels
    private final static int MAX_SIZE = 300 * 300;
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 1024;

    private HashMap<Integer, CachedImage> imageMap;
    private RTTexture backingStore;
    private RectanglePacker hPacker;
    private RectanglePacker vPacker;


    RegionImageCache(final ResourceFactory factory) {
        imageMap = new HashMap<>();
        WrapMode mode;
        int pad;
        if (factory.isWrapModeSupported(WrapMode.CLAMP_TO_ZERO)) {
            mode = WrapMode.CLAMP_TO_ZERO;
            pad = 0;
        } else {
            mode = WrapMode.CLAMP_NOT_NEEDED;
            pad = 1;
        }
        backingStore = factory.createRTTexture(WIDTH + WIDTH, HEIGHT, mode);
        backingStore.contentsUseful();
        backingStore.makePermanent();
        factory.setRegionTexture(backingStore);
        // Subdivide the texture in two halves where on half is used to store
        // horizontal regions and the other vertical regions. Otherwise, mixing
        // horizontal and vertical regions on the same area, would result in
        // a lot of waste texture space.
        // Note that requests are already padded on the right and bottom edges
        // (and that includes the gap between the caches) so we only have to
        // pad top and left edges if CLAMP_TO_ZERO needs to be simulated.
        hPacker = new RectanglePacker(backingStore, pad, pad, WIDTH-pad, HEIGHT-pad, false);
        vPacker = new RectanglePacker(backingStore, WIDTH, pad, WIDTH, HEIGHT-pad, true);
    }

    /**
     * Check if the image size is to big to be stored in the cache
     *
     * @param w The image width
     * @param h The image height
     * @return True if the image size is less than max
     */
    boolean isImageCachable(int w, int h) {
        return 0 < w && w < WIDTH &&
               0 < h && h < HEIGHT &&
               (w * h) < MAX_SIZE;
    }

    RTTexture getBackingStore() {
        return backingStore;
    }

    /**
     * Search the cache for a background image representing the arguments.
     * When this method succeeds the x and y coordinates in rect are adjust
     * to the location in the backing store when the image is stored.
     * If a failure occurred the rect is set to empty to indicate the caller
     * to disable caching.
     *
     * @param key the hash key for the image
     * @param rect the rect image. On input, width and height determine the requested
     *        texture space. On ouput, the x and y the location in the texture
     * @param background the background used to validated if the correct image was found
     * @param shape the shape used to validated if the correct image was found
     * @param g the graphics to flush if the texture needs to be restarted
     * @return true means to caller needs to render to rect to initialize the content.
     */
    boolean getImageLocation(Integer key, Rectangle rect, Background background,
                             Shape shape, Graphics g) {
        CachedImage cache = imageMap.get(key);
        if (cache != null) {
            if (cache.equals(rect.width, rect.height, background, shape)) {
                rect.x = cache.x;
                rect.y = cache.y;
                return false;
            }
            // hash collision, mark rectangle empty indicates the caller to
            // disable caching
            rect.width = rect.height = -1;
            return false;
        }
        boolean vertical = rect.height > 64;
        RectanglePacker packer = vertical ? vPacker : hPacker;

        if (!packer.add(rect)) {
            g.sync();

            vPacker.clear();
            hPacker.clear();
            imageMap.clear();
            packer.add(rect);
            backingStore.createGraphics().clear();
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.incrementCounter("Region image cache flushed");
            }
        }
        imageMap.put(key, new CachedImage(rect, background, shape));
        return true;
    }

    static class CachedImage {
        Background background;
        Shape shape;
        int x, y, width, height;

        CachedImage(Rectangle rect, Background background, Shape shape) {
            this.x = rect.x;
            this.y = rect.y;
            this.width = rect.width;
            this.height = rect.height;
            this.background = background;
            this.shape = shape;
        }

        public boolean equals(int width, int height, Background background, Shape shape) {
            return this.width == width &&
                   this.height == height &&
                   (this.background == null ? background == null : this.background.equals(background)) &&
                   (this.shape == null ? shape == null : this.shape.equals(shape));
        }
    }

}
