/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * A helper class containing algorithms taken from JDK 6 that additionally track the permutations that're created
 * during the process.
 *
 * @see Arrays
 */
public class SortHelper {

    private int[] permutation;
    private int[] reversePermutation;

    private static final int INSERTIONSORT_THRESHOLD = 7;

    // Used only by Observable[Sequential]ListWrapper, comparator can't be null here
    public <T> int[] sort(List<T> list, Comparator<? super T> comparator) {
        @SuppressWarnings("unchecked")
        T[] a = (T[]) list.toArray();
        int[] result = sort(a, comparator);
        ListIterator<T> i = list.listIterator();
        for (int j = 0; j < a.length; j++) {
            i.next();
            i.set(a[j]);
        }
        return result;
    }

    private <T> int[] sort(T[] items, Comparator<? super T> comparator) {
        T[] aux = items.clone();
        int[] result = initPermutation(items.length);
        mergeSort(aux, items, 0, items.length, 0, comparator);
        reversePermutation = null;
        permutation = null;
        return result;
    }

    // Used only by {@link SortedList}, comparator can't be null here
    public <T> int[] sort(T[] items, int fromIndex, int toIndex, Comparator<? super T> comparator) {
        rangeCheck(items.length, fromIndex, toIndex);
        T[] aux = copyOfRange(items, fromIndex, toIndex);
        int[] result = initPermutation(items.length);
        mergeSort(aux, items, fromIndex, toIndex, -fromIndex, comparator);
        reversePermutation = null;
        permutation = null;
        return Arrays.copyOfRange(result, fromIndex, toIndex);
    }

    // used only by {@link FilteredList}
    public int[] sort(int[] indices, int fromIndex, int toIndex) {
        rangeCheck(indices.length, fromIndex, toIndex);
        int[] aux = copyOfRange(indices, fromIndex, toIndex);
        int[] result = initPermutation(indices.length);
        mergeSort(aux, indices, fromIndex, toIndex, -fromIndex);
        reversePermutation = null;
        permutation = null;
        return Arrays.copyOfRange(result, fromIndex, toIndex);
    }

    private static void rangeCheck(int arrayLen, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        }
        if (toIndex > arrayLen) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }


    private static int[] copyOfRange(int[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        int[] copy = new int[newLength];
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    private static <T> T[] copyOfRange(T[] original, int from, int to) {
        return copyOfRange(original, from, to, (Class<T[]>) original.getClass());
    }

    private static <T, U> T[] copyOfRange(U[] original, int from, int to, Class<? extends T[]> newType) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        T[] copy = ((Object) newType == (Object) Object[].class) ? (T[]) new Object[newLength]
                : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }


    private void mergeSort(int[] src, int[] dest, int low, int high, int off) {
        int length = high - low;

        // Insertion sort on smallest arrays
        if (length < INSERTIONSORT_THRESHOLD) {
            for (int i = low; i < high; i++) {
                for (int j = i; j > low && Integer.compare(dest[j - 1], dest[j]) > 0; j--) {
                    swap(dest, j, j - 1);
                }
            }
            return;
        }

        // Recursively sort halves of dest into src
        int destLow = low;
        int destHigh = high;
        low += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off);
        mergeSort(dest, src, mid, high, -off);

        // If list is already sorted, just copy from src to dest. This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (Integer.compare(src[mid - 1], src[mid]) <= 0) {
            System.arraycopy(src, low, dest, destLow, length);
            return;
        }

        // Merge sorted halves (now in src) into dest
        for (int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && Integer.compare(src[p], src[q]) <= 0) {
                dest[i] = src[p];
                permutation[reversePermutation[p++]] = i;
            } else {
                dest[i] = src[q];
                permutation[reversePermutation[q++]] = i;
            }
        }

        for (int i = destLow; i < destHigh; ++i) {
            reversePermutation[permutation[i]] = i;
        }
    }


    private <T> void mergeSort(T[] src, T[] dest, int low, int high, int off, Comparator<? super T> comparator) {
        int length = high - low;

        // Insertion sort on smallest arrays
        if (length < INSERTIONSORT_THRESHOLD) {
            for (int i = low; i < high; i++) {
                for (int j = i; j > low && comparator.compare(dest[j - 1], dest[j]) > 0; j--) {
                    swap(dest, j, j - 1);
                }
            }
            return;
        }

        // Recursively sort halves of dest into src
        int destLow = low;
        int destHigh = high;
        low += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off, comparator);
        mergeSort(dest, src, mid, high, -off, comparator);

        // If list is already sorted, just copy from src to dest. This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (comparator.compare(src[mid - 1], src[mid]) <= 0) {
            System.arraycopy(src, low, dest, destLow, length);
            return;
        }

        // Merge sorted halves (now in src) into dest
        for (int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && comparator.compare(src[p], src[q]) <= 0) {
                dest[i] = src[p];
                permutation[reversePermutation[p++]] = i;
            } else {
                dest[i] = src[q];
                permutation[reversePermutation[q++]] = i;
            }
        }

        for (int i = destLow; i < destHigh; ++i) {
            reversePermutation[permutation[i]] = i;
        }
    }

    private void swap(int[] x, int a, int b) {
        int t = x[a];
        x[a] = x[b];
        x[b] = t;
        permutation[reversePermutation[a]] = b;
        permutation[reversePermutation[b]] = a;
        int tp = reversePermutation[a];
        reversePermutation[a] = reversePermutation[b];
        reversePermutation[b] = tp;
    }

    private void swap(Object[] x, int a, int b) {
        Object t = x[a];
        x[a] = x[b];
        x[b] = t;
        permutation[reversePermutation[a]] = b;
        permutation[reversePermutation[b]] = a;
        int tp = reversePermutation[a];
        reversePermutation[a] = reversePermutation[b];
        reversePermutation[b] = tp;
    }

    private int[] initPermutation(int length) {
        permutation = new int[length];
        reversePermutation = new int[length];
        for (int i = 0; i < length; ++i) {
            permutation[i] = reversePermutation[i] = i;
        }
        return permutation;
    }
}
