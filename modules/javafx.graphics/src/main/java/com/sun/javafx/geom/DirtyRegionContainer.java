/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.geom;

import java.util.Arrays;

/**
 * Container for array of dirty regions. This container internally holds
 * pointer to the first empty dirty region in the array and index of last
 * modified dirty region. It also introduces convenient methods to modify
 * the array of dirty regions.
 */
public final class DirtyRegionContainer {

    public static final int DTR_OK = 1;
    public static final int DTR_CONTAINS_CLIP = 0;

    private RectBounds[] dirtyRegions;
    private int emptyIndex;

    public DirtyRegionContainer(int count) {
        initDirtyRegions(count);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DirtyRegionContainer) {
            DirtyRegionContainer drc = (DirtyRegionContainer)obj;
            if (size() != drc.size()) return false;
            for(int i = 0; i < emptyIndex; i++) {
                if (!getDirtyRegion(i).equals(drc.getDirtyRegion(i))) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Arrays.deepHashCode(this.dirtyRegions);
        hash = 97 * hash + this.emptyIndex;
        return hash;
    }

    public DirtyRegionContainer deriveWithNewRegion(RectBounds region) {
        if (region == null) {
            return this;
        }
        dirtyRegions[0].deriveWithNewBounds(region);
        emptyIndex = 1;
        return this;
    }

    public DirtyRegionContainer deriveWithNewRegions(RectBounds[] regions) {
        if (regions == null ||
            regions.length == 0) {

            return this;
        }
        if (regions.length > maxSpace()) {
            initDirtyRegions(regions.length);
        }

        regioncopy(regions, 0, dirtyRegions, 0, regions.length);
        emptyIndex = regions.length;
        return this;
    }

    public DirtyRegionContainer deriveWithNewContainer(DirtyRegionContainer other) {
        if (other == null ||
            other.maxSpace() == 0) {

            return this;
        }

        if (other.maxSpace() > maxSpace()) {
            initDirtyRegions(other.maxSpace());
        }

        regioncopy(other.dirtyRegions, 0, dirtyRegions, 0, other.emptyIndex);
        emptyIndex = other.emptyIndex;
        return this;
    }

    private void initDirtyRegions(int count) {
        dirtyRegions = new RectBounds[count];
        for (int i = 0; i < count; i++) {
            dirtyRegions[i] = new RectBounds();
        }
        emptyIndex = 0;
    }

    public DirtyRegionContainer copy() {
        DirtyRegionContainer drc = new DirtyRegionContainer(maxSpace());
        regioncopy(dirtyRegions, 0, drc.dirtyRegions, 0, emptyIndex);
        drc.emptyIndex = emptyIndex;
        return drc;
    }

    public int maxSpace() {
        return dirtyRegions.length;
    }

    /**
     * Gets the dirty region at given index.
     * @param index the index of requested dirty region
     * @return dirty region at given index
     */
    public RectBounds getDirtyRegion(int index) {
        return dirtyRegions[index];
    }

    public void setDirtyRegion(int index, RectBounds region) {
        dirtyRegions[index] = region;
    }

    /**
     * Adds new dirty region to the array.
     * @param region the dirty region.
     */
    public void addDirtyRegion(final RectBounds region) {
            if (region.isEmpty())
                return;

            RectBounds dr, tmp;
            int tempIndex = 0;
            int regionCount = emptyIndex;

            for(int i = 0; i < regionCount; i++) {
                //can't have overlapping regions
                dr = dirtyRegions[tempIndex];
                if (region.intersects(dr)) {
                    region.unionWith(dr);
                    tmp = dirtyRegions[tempIndex];
                    dirtyRegions[tempIndex] = dirtyRegions[emptyIndex - 1];
                    dirtyRegions[emptyIndex - 1] = tmp;
                    emptyIndex--;
                } else {
                    tempIndex++;
                }
            }//for
            if (hasSpace()) {
                dr = dirtyRegions[emptyIndex];
                dr.deriveWithNewBounds(region);
                emptyIndex++;
                return;
            }
            //match region into existing dirty regions
            if (dirtyRegions.length == 1)
                dirtyRegions[0].deriveWithUnion(region);
            else
                compress(region);
    }

    public void merge(DirtyRegionContainer other) {
        int otherSize = other.size();
        for(int i = 0; i < otherSize; i++) {
            addDirtyRegion(other.getDirtyRegion(i));
        }
    }

    public int size() {
        return emptyIndex;
    }

    public void reset() {
        emptyIndex = 0;
    }

    private RectBounds compress(final RectBounds region) {
        compress_heap();
        addDirtyRegion(region);
        return region;
    }

    /**
     * If there are empty regions in the dirty regions array.
     * @return true if there is empty region in the array; false otherwise
     */
    private boolean hasSpace() {
        return emptyIndex < dirtyRegions.length;
    }

    private void regioncopy(RectBounds[] src, int from, RectBounds[] dest, int to, int length) {
        RectBounds rb;
        for (int i = 0; i < length; i++) {
            rb = src[from++];
            if (rb == null) {
                dest[to++].makeEmpty();
            } else {
                dest[to++].deriveWithNewBounds(rb);
            }
        }
    }

    public boolean checkAndClearRegion(int index) {
        boolean removed = false;
        if (dirtyRegions[index].isEmpty()) {
            System.arraycopy(dirtyRegions, index + 1, dirtyRegions, index, emptyIndex - index - 1);
            --emptyIndex;
            removed = true;
        }

        return removed;
    }

    public void grow(int horizontal, int vertical) {
        if (horizontal != 0 || vertical != 0) {
            for (int i = 0; i < emptyIndex; i++) {
                getDirtyRegion(i).grow(horizontal, vertical);
            }
        }
    }

    public void roundOut() {
        for (int i = 0; i < emptyIndex; ++i) {
            dirtyRegions[i].roundOut();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < emptyIndex; i++) {
            sb.append(dirtyRegions[i]);
            sb.append('\n');
        }
        return sb.toString();
    }

    /***************************************************************************
     * Shared for all compressing algorithms
     ***************************************************************************/
    private int[][] heap; // heap used for compressing dirty regions
    private int heapSize;
    private long invalidMask;

    private void heapCompress() {
        invalidMask = 0;
        int[] map = new int[dirtyRegions.length];
        for (int i = 0; i < map.length; ++i) {
            map[i] = i;
        }

        int[] min;
        for (int i = 0; i < dirtyRegions.length / 2; ++i) { //compress to 1/2
            min = takeMinWithMap(map);
            int idx0 = resolveMap(map, min[1]);
            int idx1 = resolveMap(map, min[2]);
            if (idx0 != idx1) {
                dirtyRegions[idx0].deriveWithUnion(dirtyRegions[idx1]);
                map[idx1] = idx0;
                invalidMask |= 1 << idx0;
                invalidMask |= 1 << idx1;
            }
        }

        // move the unused regions to the end
        RectBounds tmp;
        for (int i = 0; i < emptyIndex; ++i) {
            if (map[i] != i) {
                while(map[emptyIndex - 1] != emptyIndex - 1 ) --emptyIndex;
                if (i < emptyIndex - 1) {
                    tmp = dirtyRegions[emptyIndex - 1];
                    dirtyRegions[emptyIndex - 1] = dirtyRegions[i];
                    dirtyRegions[i] = tmp;
                    map[i] = i; // indicate that this element is OK
                    --emptyIndex;
                }
            }
        }
    }

    private void heapify() {
        for (int i = heapSize / 2; i >= 0; --i) {
            siftDown(i);
        }
    }

    private void siftDown(int i) {
        int end = heapSize >> 1;
        int[] temp;
        while (i < end) {
            int child = (i << 1) + 1;
            int[] left = heap[child];
            if (child + 1 < heapSize && heap[child + 1][0] < left[0]) {
                child = child + 1;
            }
            if (heap[child][0] >= heap[i][0]) {
                break;
            }
            temp = heap[child];
            heap[child] = heap[i];
            heap[i] = temp;
            i = child;
        }
    }

    private int[] takeMinWithMap(int[] map) {
        int[] temp = heap[0];

        // Check if the minimum element wasn't computed with some already merged region
        // If so, we can recompute it and push it down (as the new merged region can be
        // only greater than the original region)
        while (((1 << temp[1] | 1 << temp[2]) & invalidMask) > 0) {
            temp[0] = unifiedRegionArea(resolveMap(map, temp[1]), resolveMap(map, temp[2]));
            siftDown(0);
            if (heap[0] == temp) {
                break;
            }
            temp = heap[0];
        }

        heap[heapSize - 1] = temp;
        siftDown(0);
        heapSize--;
        return temp;
    }

    private int[] takeMin() {
        int[] temp = heap[0];
        heap[0] = heap[heapSize - 1];
        heap[heapSize - 1] = temp;
        siftDown(0);
        heapSize--;
        return temp;
    }

    private int resolveMap(int[] map, int idx) {
        while(map[idx] != idx) idx = map[idx];
        return idx;
    }

    private int unifiedRegionArea(int i0, int i1) {
        RectBounds r0 = dirtyRegions[i0];
        RectBounds r1 = dirtyRegions[i1];
        float minX, minY, maxX, maxY;
        minX = r0.getMinX() < r1.getMinX() ? r0.getMinX() : r1.getMinX();
        minY = r0.getMinY() < r1.getMinY() ? r0.getMinY() : r1.getMinY();
        maxX = r0.getMaxX() > r1.getMaxX() ? r0.getMaxX() : r1.getMaxX();
        maxY = r0.getMaxY() > r1.getMaxY() ? r0.getMaxY() : r1.getMaxY();

        return (int) ((maxX - minX) * (maxY - minY));
    }
    /***************************************************************************
     * Heap-based compressing algorithm
     ***************************************************************************/

    private void compress_heap() {
        assert dirtyRegions.length == emptyIndex; // call only when there is no space left
        if (heap == null) {
            int n = dirtyRegions.length;
            heap = new int[n * (n-1) / 2][3];
        }
        heapSize = heap.length;
        int k = 0;
        for (int i = 0; i < dirtyRegions.length - 1; ++i) {
            for (int j = i + 1; j < dirtyRegions.length; ++j) {
                heap[k][0] = unifiedRegionArea(i, j);
                heap[k][1] = i;
                heap[k++][2] = j;
            }
        }
        heapify();
        heapCompress();
    }

    /***************************************************************************
     * Simple Monte-Carlo variant of compressing algorithm
     ***************************************************************************/

//    private void compress_mc() {
//        assert dirtyRegions.length == emptyIndex; // call only when there is no space left
//        heapSize = dirtyRegions.length;
//        if (heap == null) {
//            heap = new int[heapSize][3];
//        }
//        for (int i = 0; i < heapSize; ++i) { //number of tries
//            int i0 = random(dirtyRegions.length);
//            int i1 = random(dirtyRegions.length);
//            if (i1 == i0) i1 = (i0 + random(dirtyRegions.length / 2) + 1) % dirtyRegions.length;
//            heap[i][0] = unifiedRegionArea(i0, i1);
//            heap[i][1] = i0;
//            heap[i][2] = i1;
//        }
//        heapify();
//        heapCompress();
//    }
//
//    private static long rnd = System.currentTimeMillis();
//    // XOR Random generator by George Marsaglia http://www.jstatsoft.org/v08/i14/
//    // The LCG algorithm of Random() has an upleasant trait that the numbers generated in
//    // pairs always have some (tight) mathematical relationship
//    private int random(int n) {
//        rnd ^= (rnd << 21);
//        rnd ^= (rnd >>> 35);
//        rnd ^= (rnd << 4);
//        return (int) ((rnd) % n + n) % n;     // the problem with this algorithm is that 0 is never produced.
//                                              // Given it's independed probability for every bit, we can safely do % n here
//    }
}
