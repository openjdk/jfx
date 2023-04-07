/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.binding;

import java.util.Arrays;
import java.util.Objects;

/**
 * Helper to manage an array as a data and size field in order to save
 * space overhead introduced by a dedicated class.<p>
 *
 * To use this class, store an instance of this class in a static field,
 * supplying the {@link Accessor} for a specific array and size. Then just call
 * methods on the array manager supplying the instance each time.
 */
public class ArrayManager<I, E> {

    /**
     * The minimum array size. (3 is a good number considering it would use exactly 24 bytes
     * with compressed oops with wasted space due to alignment issues).
     */
    private static final int MINIMUM_SIZE = 3;

    private final Accessor<I, E> accessor;

    /**
     * Provides access to two fields to manage an array with.
     *
     * @param <I> the type containing the fields
     * @param <E> the type of the array elements
     */
    public interface Accessor<I, E> {
        E[] getArray(I instance);
        void setArray(I instance, E[] array);
        int getOccupiedSlots(I instance);
        void setOccupiedSlots(I instance, int occupiedSlots);
    }

    /**
     * Constructs a new instance.
     *
     * @param accessor an {@link Accessor}, cannot be {@code null}
     */
    public ArrayManager(Accessor<I, E> accessor) {
        this.accessor = Objects.requireNonNull(accessor);
    }

    /**
     * Adds an element at the end of the array, growing the arrow if necessary.
     * If the array needs to be grown, this function will call {@link #compact(Object, Object[])}
     * first to reclaim any space before deciding to grow the array.
     *
     * @param instance a reference to the instance where the array is stored, never {@code null}
     * @param element an element to add, can be {@code null}
     * @throws NullPointerException when the given instance was {@code null}
     */
    public void add(I instance, E element) {
        E[] array = accessor.getArray(instance);
        int occupiedSlots = accessor.getOccupiedSlots(instance);

        if (array == null) {
            accessor.setArray(instance, array = (E[]) new Object[MINIMUM_SIZE]);
            accessor.setOccupiedSlots(instance, occupiedSlots + 1);

            array[0] = element;

            return;
        }

        int newSize = calculateOptimalSize(array.length, occupiedSlots + 1);

        assert newSize >= array.length : newSize + " >= " + array.length;

        if (newSize > array.length) {
            occupiedSlots -= compact(instance, array);

            int optimalSize = calculateOptimalSize(array.length, occupiedSlots + 1);

            if (optimalSize != array.length) {
                accessor.setArray(instance, array = Arrays.copyOf(array, optimalSize));
            }
        }

        accessor.setOccupiedSlots(instance, occupiedSlots + 1);

        array[occupiedSlots] = element;
    }

    /**
     * Finds the first occurrence of the given element in the array, and if
     * found, returns its index. If not found, -1 is returned.<p>
     *
     * This method does not modify the array fields.
     *
     * @param instance a reference to the instance where the array is stored, never {@code null}
     * @param element an element to locate, can be {@code null}
     * @return the index of the first occurrence of the given element, or -1 if no such element was present
     * @throws NullPointerException when the given instance was {@code null}
     */
    public int indexOf(I instance, E element) {
        E[] array = accessor.getArray(instance);
        int occupiedSlots = accessor.getOccupiedSlots(instance);

        for (int i = 0; i < occupiedSlots; i++) {
            if (Objects.equals(element, array[i])) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Removes the element at the given index from the array.
     *
     * @param instance a reference to the instance where the array is stored, never {@code null}
     * @param index an index to remove, cannot be negative, or greater than or equal to the number of occupied slots
     * @returns the element that was removed, can be {@code null} if the element at the given index was {@code null}
     * @throws NullPointerException when the given instance was {@code null}
     * @throws IndexOutOfBoundsException when the given index was out of range
     */
    public E remove(I instance, int index) {
        E[] array = accessor.getArray(instance);
        int occupiedSlots = accessor.getOccupiedSlots(instance);

        Objects.checkIndex(index, occupiedSlots--);

        E oldElement = array[index];
        int newSize = calculateOptimalSize(array.length, occupiedSlots);

        assert newSize <= array.length : newSize + " <= " + array.length;

        accessor.setOccupiedSlots(instance, occupiedSlots);

        if(newSize == array.length) {
            if(index < occupiedSlots) {
                System.arraycopy(array, index + 1, array, index, occupiedSlots - index);
            }

            array[occupiedSlots] = null;
        }
        else if(newSize == 0) {
            accessor.setArray(instance, null);
        }
        else {
            E[] newArray = (E[])new Object[newSize];

            System.arraycopy(array, 0, newArray, 0, index);

            if(index < occupiedSlots) {
                System.arraycopy(array, index + 1, newArray, index, occupiedSlots - index);
            }

            accessor.setArray(instance, newArray);
        }

        return oldElement;
    }

    /**
     * Returns the element at the given index from the array.<p>
     *
     * This method does not modify the array fields.
     *
     * @param instance a reference to the instance where the array is stored, never {@code null}
     * @param index an index, cannot be negative, or greater than or equal to the number of occupied slots
     * @returns the element at the given index, can be {@code null} if the element at the given index was {@code null}
     * @throws NullPointerException when the given instance was {@code null}
     * @throws IndexOutOfBoundsException when the given index was out of range
     */
    public E get(I instance, int index) {
        E[] array = accessor.getArray(instance);
        int occupiedSlots = accessor.getOccupiedSlots(instance);

        Objects.checkIndex(index, occupiedSlots);

        return array[index];
    }

    /**
     * Sets the element at the given index to the given element.<p>
     *
     * This method does not modify the array fields.
     *
     * @param instance a reference to the instance where the array is stored, never {@code null}
     * @param index an index to set, cannot be negative, or greater than or equal to the number of occupied slots
     * @param element an element to set, can be {@code null}
     * @returns the element that was previously at the given index, can be {@code null} if the element at the given index was {@code null}
     * @throws NullPointerException when the given instance was {@code null}
     * @throws IndexOutOfBoundsException when the given index was out of range
     */
    public E set(I instance, int index, E element) {
        E[] array = accessor.getArray(instance);
        int occupiedSlots = accessor.getOccupiedSlots(instance);

        Objects.checkIndex(index, occupiedSlots);

        E oldElement = array[index];
        array[index] = element;

        return oldElement;
    }

    /**
     * Called when all slots in the array are occupied and the
     * array would need to be grown. If compaction was possible,
     * return the amount of slots reclaimed. The freed up slots
     * must be the last slots in the array after this call
     * completes.<p>
     *
     * Note: it is not allowed to change the array fields during this call;
     * doing so will result in undefined behavior. Only the array's content
     * may be changed.<p>
     *
     * By default, no compaction takes place and this method returns
     * 0 to indicate no slots were reclaimed.<p>
     *
     * @param instance a reference to the instance where the array is stored, never {@code null}
     * @param array an array to compact, never {@code null}
     * @return the number of slots reclaimed, never negative
     */
    protected int compact(I instance, E[] array) {
        return 0;  // no compaction took place
    }

    private static int calculateOptimalSize(int size, int needed) {

        /*
         * Keeps the array in an optimal range. The current size
         * is used to calculate the bottom end of the range.
         *
         * If needed is within that range, the size returned is
         * the same as the current size, otherwise a new size is
         * suggested.
         *
         * When the minimum size of the array is 3 (MINIMUM_SIZE),
         * then the resizing algorithm used here always uses specific
         * sizes: 3, 7, 13, 22, 36, etc...
         *
         * When the maximum size is exceeded, the next larger size
         * which would fit needed is returned. When the amount needed
         * is smaller than or equal to the middle point between the
         * previous smaller size and its previous smaller size, then
         * the next smaller size which would fit needed is returned.
         *
         * This makes the ranges: [1..3], [2..7], [6..13], [11..22], [18..36], [30..57]
         *
         * The overlap is intended to avoid resizing too often.
         */

        if (needed == 0) {
            return 0;
        }

        int mid = decrease(size);
        int min = decrease(mid);
        int max = size;

        while (needed <= (min + mid) / 2) {
            max = mid;
            mid = min;
            min = decrease(min);
        }

        while (needed > max) {
            min = mid;
            mid = max;
            max = increase(max);
        }

        return max;
    }

    // note: must be the exact inverse of increase, so the array sizes used
    // are always the same values.
    private static int decrease(int size) {
        return (int)(((size - MINIMUM_SIZE) * 2L + 2) / 3);
    }

    private static int increase(int size) {
        return (int)(size * 3L / 2 + MINIMUM_SIZE);
    }
}

