/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.marlin;

import static com.sun.marlin.ArrayCacheConst.ARRAY_SIZES;
import static com.sun.marlin.ArrayCacheConst.BUCKETS;
import static com.sun.marlin.ArrayCacheConst.MAX_ARRAY_SIZE;

import static com.sun.marlin.MarlinConst.DO_STATS;
import static com.sun.marlin.MarlinConst.DO_CHECKS;
import static com.sun.marlin.MarlinConst.DO_LOG_WIDEN_ARRAY;
import static com.sun.marlin.MarlinConst.DO_LOG_OVERSIZE;

import static com.sun.marlin.MarlinUtils.logInfo;
import static com.sun.marlin.MarlinUtils.logException;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import com.sun.marlin.ArrayCacheConst.BucketStats;
import com.sun.marlin.ArrayCacheConst.CacheStats;

/*
 * Note that the ArrayCache[BYTE/INT/FLOAT/DOUBLE] files are nearly identical except
 * for a few type and name differences. Typically, the [BYTE]ArrayCache.java file
 * is edited manually and then [INT/FLOAT/DOUBLE]ArrayCache.java
 * files are generated with the following command lines:
 */

public final class ArrayCacheIntClean {

    /* members */
    private final int bucketCapacity;
    private WeakReference<Bucket[]> refBuckets = null;
    final CacheStats stats;

    ArrayCacheIntClean(final int bucketCapacity) {
        this.bucketCapacity = bucketCapacity;
        this.stats = (DO_STATS) ?
            new CacheStats("ArrayCacheInt(Clean)") : null;
    }

    Bucket getCacheBucket(final int length) {
        final int bucket = ArrayCacheConst.getBucket(length);
        return getBuckets()[bucket];
    }

    private Bucket[] getBuckets() {
        // resolve reference:
        Bucket[] buckets = (refBuckets != null) ? refBuckets.get() : null;

        // create a new buckets ?
        if (buckets == null) {
            buckets = new Bucket[BUCKETS];

            for (int i = 0; i < BUCKETS; i++) {
                buckets[i] = new Bucket(ARRAY_SIZES[i], bucketCapacity,
                        (DO_STATS) ? stats.bucketStats[i] : null);
            }

            // update weak reference:
            refBuckets = new WeakReference<>(buckets);
        }
        return buckets;
    }

    Reference createRef(final int initialSize) {
        return new Reference(this, initialSize);
    }

    static final class Reference {

        // initial array reference (direct access)
        final int[] initial;
        private final ArrayCacheIntClean cache;

        Reference(final ArrayCacheIntClean cache, final int initialSize) {
            this.cache = cache;
            this.initial = createArray(initialSize);
            if (DO_STATS) {
                cache.stats.totalInitial += initialSize;
            }
        }

        int[] getArray(final int length) {
            if (length <= MAX_ARRAY_SIZE) {
                return cache.getCacheBucket(length).getArray();
            }
            if (DO_STATS) {
                cache.stats.oversize++;
            }
            if (DO_LOG_OVERSIZE) {
                logInfo("ArrayCacheInt(Clean): "
                        + "getArray[oversize]: length=\t" + length);
            }
            return createArray(length);
        }

        int[] widenArray(final int[] array, final int usedSize,
                          final int needSize)
        {
            final int length = array.length;
            if (DO_CHECKS && length >= needSize) {
                return array;
            }
            if (DO_STATS) {
                cache.stats.resize++;
            }

            // maybe change bucket:
            // ensure getNewSize() > newSize:
            final int[] res = getArray(ArrayCacheConst.getNewSize(usedSize, needSize));

            // use wrapper to ensure proper copy:
            System.arraycopy(array, 0, res, 0, usedSize); // copy only used elements

            // maybe return current array:
            putArray(array, 0, usedSize); // ensure array is cleared

            if (DO_LOG_WIDEN_ARRAY) {
                logInfo("ArrayCacheInt(Clean): "
                        + "widenArray[" + res.length
                        + "]: usedSize=\t" + usedSize + "\tlength=\t" + length
                        + "\tneeded length=\t" + needSize);
            }
            return res;
        }

        boolean doSetRef(final int[] array) {
            return (array != initial);
        }

        int[] putArrayClean(final int[] array)
        {
            // must be protected by doSetRef() call !
            if (array.length <= MAX_ARRAY_SIZE) {
                // ensure to never store initial arrays in cache:
                cache.getCacheBucket(array.length).putArray(array);
            }
            return initial;
        }

        int[] putArray(final int[] array, final int fromIndex,
                        final int toIndex)
        {
            if (array.length <= MAX_ARRAY_SIZE) {
                if (toIndex != 0) {
                    // clean-up array of dirty part[fromIndex; toIndex[
                    fill(array, fromIndex, toIndex, 0);
                }
                // ensure to never store initial arrays in cache:
                if (array != initial) {
                    cache.getCacheBucket(array.length).putArray(array);
                }
            }
            return initial;
        }
    }

    static final class Bucket {

        private int tail = 0;
        private final int arraySize;
        private final int[][] arrays;
        private final BucketStats stats;

        Bucket(final int arraySize,
               final int capacity, final BucketStats stats)
        {
            this.arraySize = arraySize;
            this.stats = stats;
            this.arrays = new int[capacity][];
        }

        int[] getArray() {
            if (DO_STATS) {
                stats.getOp++;
            }
            // use cache:
            if (tail != 0) {
                final int[] array = arrays[--tail];
                arrays[tail] = null;
                return array;
            }
            if (DO_STATS) {
                stats.createOp++;
            }
            return createArray(arraySize);
        }

        void putArray(final int[] array)
        {
            if (DO_CHECKS && (array.length != arraySize)) {
                logInfo("ArrayCacheInt(Clean): "
                        + "bad length = " + array.length);
                return;
            }
            if (DO_STATS) {
                stats.returnOp++;
            }
            // fill cache:
            if (arrays.length > tail) {
                arrays[tail++] = array;

                if (DO_STATS) {
                    stats.updateMaxSize(tail);
                }
            } else if (DO_CHECKS) {
                logInfo("ArrayCacheInt(Clean): "
                        + "array capacity exceeded !");
            }
        }
    }

    static int[] createArray(final int length) {
        return new int[length];
    }

    static void fill(final int[] array, final int fromIndex,
                     final int toIndex, final int value)
    {
        // clear array data:
        Arrays.fill(array, fromIndex, toIndex, value);
        if (DO_CHECKS) {
            check(array, fromIndex, toIndex, value);
        }
    }

    public static void check(final int[] array, final int fromIndex,
                      final int toIndex, final int value)
    {
        if (DO_CHECKS) {
            // check zero on full array:
            for (int i = 0; i < array.length; i++) {
                if (array[i] != value) {
                    logException("Invalid value at: " + i + " = " + array[i]
                            + " from: " + fromIndex + " to: " + toIndex + "\n"
                            + Arrays.toString(array), new Throwable());

                    // ensure array is correctly filled:
                    Arrays.fill(array, value);

                    return;
                }
            }
        }
    }
}
