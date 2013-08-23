/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.packrect;

import com.sun.javafx.geom.Rectangle;
import com.sun.prism.Texture;
import java.util.ArrayList;
import java.util.List;

/**
 * Packs rectangles supplied by the user (typically representing image regions)
 * into a larger backing store rectangle (typically representing a large
 * texture). Supports automatic compaction of the space on the backing store,
 * and automatic expansion of the backing store, when necessary.
 */
public class RectanglePacker {
    /**
     * A reference to the backing store that was created (lazily)
     * by the backing store manager.
     */
    private Texture backingStore;

    /** Manages a list of Levels; this is the core data structure
     contained within the RectanglePacker and encompasses the storage
     algorithm for the contained Rects. */
    // Maintained in sorted order by increasing Y coordinate
    private List<Level> levels = new ArrayList<Level>(150);
    private static final int MIN_SIZE = 8; // The minimum size of level
    private static final int ROUND_UP = 4; // Round up to multiple of 4
    private int recentUsedLevelIndex = 0;
    private int length;
    private int size;
    private int sizeOffset;
    private int x;
    private int y;
    private boolean vertical;

    public RectanglePacker(Texture backingStore, int x, int y,
                           int width, int height, boolean vertical) {
        this.backingStore = backingStore;
        if (vertical) {
            this.length = height;
            this.size = width;
        } else {
            this.length = width;
            this.size = height;
        }
        this.x = x;
        this.y = y;
        this.vertical = vertical;
    }

    /**
     * Creates a new RectanglePacker. You must specify the texture used as the
     * backing store, and the width and height of the space within which rectangles
     * are to be packed.
     *
     * @param backingStore The backing store texture, must not be null
     * @param width The width of the backing store, must be > 0 (typically > 512)
     * @param height The height of the backing store, must be > 0 (typically > 512)
     */
    public RectanglePacker(Texture backingStore, int width, int height) {
        this(backingStore, 0, 0, width, height, false);
    }

    /**
     * Gets a reference to the backing store, creating it lazily if necessary.
     * @return A reference to the backing store.
     */
    public final Texture getBackingStore() {
        return backingStore;
    }

    /**
     * Decides upon an (x, y) position for the given rectangle (leaving
     * its width and height unchanged) and places it on the backing
     * store.
     */
    public final boolean add(Rectangle rect) {
        // N need to continue if the rectangle simply won't fit.
        final int requestedLength = vertical ? rect.height : rect.width;
        final int requestedSize = vertical ? rect.width : rect.height;

        if (requestedLength > length) return false;
        if (requestedSize > size) return false;

        int newSize = MIN_SIZE > requestedSize ? MIN_SIZE : requestedSize;

        // Round up
        newSize = (newSize + ROUND_UP - 1) - (newSize - 1) % ROUND_UP;

        int newIndex;
        // If it does not match recent used level, using binary search to find
        // the best fit level's index
        if (recentUsedLevelIndex < levels.size() &&
            levels.get(recentUsedLevelIndex).size != newSize) {
            newIndex = binarySearch(levels, newSize);
        } else {
            newIndex = recentUsedLevelIndex;
        }

        // Can create a new level with newSize
        final boolean newLevelFlag = sizeOffset + newSize <= size;

        // Go through the levels check whether we can satisfy the allocation
        // request
        for (int i = newIndex, max = levels.size(); i < max; i++) {
            Level level = levels.get(i);
            // If level's height is more than (newHeight + ROUND_UP * 2) and
            // the cache still has some space left, go create a new level
            if (level.size > (newSize + ROUND_UP * 2) && newLevelFlag) {
                break;
            } else if (level.add(rect, x, y, requestedLength, requestedSize, vertical)) {
                recentUsedLevelIndex = i;
                return true;
            }
        }

        // Try to add a new Level.
        if (!newLevelFlag) {
            return false;
        }

        Level newLevel = new Level(length, newSize, sizeOffset);
        sizeOffset += newSize;

        // For a rect that cannot fit into the existing level, create a new
        // level and add at the end of levels that have the same height
        if (newIndex < levels.size() && levels.get(newIndex).size <= newSize) {
            levels.add(newIndex + 1, newLevel);
            recentUsedLevelIndex = newIndex + 1;
        } else {
            levels.add(newIndex, newLevel);
            recentUsedLevelIndex = newIndex;
        }
        return newLevel.add(rect, x, y, requestedLength, requestedSize, vertical);
    }

    /**
     * Clears all Rectangles contained in this RectanglePacker.
     */
    public void clear() {
        levels.clear();
        sizeOffset = 0;
        recentUsedLevelIndex = 0;
    }

    /**
     * Disposes the backing store allocated by the
     * BackingStoreManager. This RectanglePacker may no longer be used
     * after calling this method.
     */
    public void dispose() {
        if (backingStore != null) {
            backingStore.dispose();
        }

        backingStore = null;
        levels = null;
    }

    /** Using binary search to find the last index of best fit level for k,
     where k is a rounded-up value. */
    private static int binarySearch(List<Level> levels, int k) {

        // k+1 is used to find the last index of the level with height of k. Because of rounding up, more
        // likely, there are a bunch of levels have the same height. But, we always keep adding levels and
        // rects at the end. k+1 is a trick to find the last index by finding the next greater value's index
        // and go back one.
        // Note that since the sizes are quantized, k+1 is a special value that will not appear in the list
        // of level sizes and so the search for it will find the gap between the size for k and the size
        // for the next quantum.
        int key = k + 1;
        int from = 0, to = levels.size() - 1;
        int mid = 0;
        int midSize = 0;

        if (to < 0) {
            return 0;
        }

        while (from <= to) {
            mid = (from + to) / 2;
            midSize = levels.get(mid).size;
            if (key < midSize) {
                to = mid - 1;
            } else {
                from = mid + 1;
            }
        }

        if (midSize < k) {
            return mid + 1;
        } else if (midSize > k) {
            return mid > 0 ? mid - 1 : 0;
        } else {
            return mid;
        }
    }
}
