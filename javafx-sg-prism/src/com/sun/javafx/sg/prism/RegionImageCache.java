/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.sun.javafx.logging.PulseLogger;
import com.sun.prism.RTTexture;

/**
 * RegionImageCache - A fixed pixel count sized cache of Images keyed by arbitrary set of arguments. All images are held with
 * SoftReferences so they will be dropped by the GC if heap memory gets tight. When our size hits max pixel count least
 * recently requested images are removed first.
 *
 */
class RegionImageCache {
    // Ordered Map keyed by args hash, ordered by most recent accessed entry.
    private final LinkedHashMap<Integer, PixelCountSoftReference> map =
            new LinkedHashMap<Integer, PixelCountSoftReference>(16, 0.75f, true);
    // Maximum number of pixels to cache, this is used if maxCount
    private final int maxPixelCount;
    // Maximum cached image size in pixels
    private final int maxSingleImagePixelSize;
    // The current number of pixels stored in the cache
    private int currentPixelCount = 0;
    // Lock for concurrent access to map
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    // Reference queue for tracking lost soft references to images in the cache
    private ReferenceQueue<RTTexture> referenceQueue = new ReferenceQueue<RTTexture>();

    RegionImageCache() {
        this.maxPixelCount = (8 * 1024 * 1024) / 4; // 8Mb of pixels
        this.maxSingleImagePixelSize = 300 * 300;
    }

    /** Clear the cache */
    void flush() {
        lock.readLock().lock();
        try {
            map.clear();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if the image size is to big to be stored in the cache
     *
     * @param w The image width
     * @param h The image height
     * @return True if the image size is less than max
     */
    boolean isImageCachable(int w, int h) {
        return w > 0 && h > 0 && (w * h) < maxSingleImagePixelSize;
    }

    /**
     * Get the cached image for given keys
     *
     * @param config The Screen used with the target Graphics. Used as part of cache key
     * @param w      The image width, used as part of cache key
     * @param h      The image height, used as part of cache key
     * @param args   Other arguments to use as part of the cache key
     * @return Returns the cached Image, or null there is no cached image for key
     */
    RTTexture getImage(Object config, int w, int h, Object... args) {
        lock.readLock().lock();
        try {
            final int hash = hash(config, w, h, args);
            PixelCountSoftReference ref = map.get(hash);
            // Check reference has not been lost and the key truly matches, in case of false positive hash match
            if (ref != null && ref.equals(config, w, h, args)) {
                RTTexture image = ref.get();
                // If the image surface is lost, then we will remove it from the cache and report back
                // to the caller that we have no cached image.
                if (image != null && image.isSurfaceLost()) {
                    if (PulseLogger.PULSE_LOGGING_ENABLED) {
                        PulseLogger.PULSE_LOGGER.renderIncrementCounter("Region RTTexture surface lost");
                    }
                    currentPixelCount -= ref.pixelCount;
                    map.remove(hash);
                    return null;
                }
                return ref.get();
            } else {
                return null;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets the cached image for the specified constraints.
     *
     * @param image  The image to store in cache
     * @param config The Screen used with the target Graphics. Used as part of cache key
     * @param w      The image width, used as part of cache key
     * @param h      The image height, used as part of cache key
     * @param args   Other arguments to use as part of the cache key
     * @return true if the image could be cached or false if the image is too big
     */
    boolean setImage(RTTexture image, Object config, int w, int h, Object... args) {
        if (!isImageCachable(w, h)) return false;
        int hash = hash(config, w, h, args);
        lock.writeLock().lock();
        try {
            PixelCountSoftReference ref = map.get(hash);
            // check if currently in map
            if (ref != null && ref.get() == image) {
                return true;
            }
            // clear out old
            if (ref != null) {
                if (PulseLogger.PULSE_LOGGING_ENABLED) {
                    PulseLogger.PULSE_LOGGER.renderIncrementCounter("Region RTTexture replaced in RegionImageCache");
                }
                currentPixelCount -= ref.pixelCount;
                map.remove(hash);
            }
            // add new image to pixel count
            int newPixelCount = image.getContentWidth() * image.getContentHeight();
            currentPixelCount += newPixelCount;
            // clean out lost references if not enough space
            if (currentPixelCount > maxPixelCount) {
                while ((ref = (PixelCountSoftReference)referenceQueue.poll()) != null){
                    //reference lost
                    map.remove(ref.hash);
                    currentPixelCount -= ref.pixelCount;
                }
            }
            // remove old items till there is enough free space
            if (currentPixelCount > maxPixelCount) {
                Iterator<Map.Entry<Integer, PixelCountSoftReference>> mapIter = map.entrySet().iterator();
                while ((currentPixelCount > maxPixelCount) && mapIter.hasNext()) {
                    Map.Entry<Integer, PixelCountSoftReference> entry = mapIter.next();
                    mapIter.remove();
                    RTTexture img = entry.getValue().get();
//                    if (img != null) img.flush();
                    currentPixelCount -= entry.getValue().pixelCount;
                }
            }
            // finally put new in map
            map.put(hash, new PixelCountSoftReference(image, referenceQueue, newPixelCount,hash, config, w, h, args));
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Create a unique hash from all the input */
    private int hash(Object config, int w, int h, Object ... args) {
        int hash;
        hash = (config != null ? config.hashCode() : 0);
        hash = 31 * hash + w;
        hash = 31 * hash + h;
        hash = 31 * hash + Arrays.deepHashCode(args);
        return hash;
    }

    /** Extended SoftReference that stores the pixel count even after the image is lost */
    private static class PixelCountSoftReference extends SoftReference<RTTexture> {
        private final int pixelCount;
        private final int hash;
        // key parts
        private final Object config;
        private final int w;
        private final int h;
        private final Object[] args;

        public PixelCountSoftReference(RTTexture referent, ReferenceQueue<? super RTTexture> q, int pixelCount, int hash,
                                       Object config, int w, int h, Object[] args) {
            super(referent, q);
            this.pixelCount = pixelCount;
            this.hash = hash;
            this.config = config;
            this.w = w;
            this.h = h;
            this.args = args;
        }

        public boolean equals (Object config, int w, int h, Object[] args){
            return config == this.config &&
                            w == this.w &&
                            h == this.h &&
                            Arrays.equals(args, this.args);
        }
    }
}
