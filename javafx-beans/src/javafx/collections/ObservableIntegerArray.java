/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package javafx.collections;

/**
 * An int[] array that allows listeners to track changes when they occur. To achieve
 * that internal array is encapsulated and there is no direct access available
 * from outside. Bulk operations are supported but they always do copy of the 
 * data range.
 * 
 * @see ArrayChangeListener
 * @since JavaFX 8.0
 */
public interface ObservableIntegerArray extends ObservableArray<ObservableIntegerArray> {

    /**
     * Copies specified portion of array into {@code dest} array. Throws
     * the same exceptions as {@link System#arraycopy(java.lang.Object,
     * int, java.lang.Object, int, int) }
     * @param srcIndex starting position in the observable array
     * @param dest destination array
     * @param destIndex starting position in destination array
     * @param length length of portion to copy
     */
    public void copyTo(int srcIndex, int[] dest, int destIndex, int length);
    public void copyTo(int srcIndex, ObservableIntegerArray dest, int destIndex, int length);

    /**
     * Gets a single value of array. This is generally as fast as direct access
     * to an array and eliminates necessity to make a copy of array.
     * @param index index of element to get
     * @return value at the given index
     * @throws ArrayIndexOutOfBoundsException if {@code index} is outside
     * array bounds
     */
    public int get(int index);

    /**
     * Appends given {@code elements} to the end of array. Capacity is increased
     * if necessary to match the new size of the data.
     * @param elements
     */
    public void addAll(int... elements);
    public void addAll(ObservableIntegerArray src);
    public void addAll(int[] src, int srcIndex, int length);
    public void addAll(ObservableIntegerArray src, int srcIndex, int length);

    /**
     * Sets observable array to a copy of given array. Capacity is increased
     * if necessary to match the new size of the data
     * @param elements source array to copy.
     * @throws NullPointerException if {@code array} is null
     */
    public void setAll(int... elements);
    public void setAll(int[] src, int srcIndex, int length);
    public void setAll(ObservableIntegerArray src);
    public void setAll(ObservableIntegerArray src, int srcIndex, int length);

    /**
     * Sets portion of observable array to a copy of given array. Throws 
     * the same exceptions as {@link System#arraycopy(java.lang.Object, 
     * int, java.lang.Object, int, int) }
     * @param destIndex the starting destination index in this observable array
     * @param src source array to copy
     * @param srcIndex starting position in source array
     * @param length length of portion to copy
     */
    public void set(int destIndex, int[] src, int srcIndex, int length);
    public void set(int destIndex, ObservableIntegerArray src, int srcIndex, int length);

    /**
     * Sets a single value in the array. Avoid using this method if many values
     * are updated, use {@linkplain #setAll(int, int[], int, int)} update method
     * instead with as minimum number of invocations as possible.
     * @param index index of the value to setAll
     * @param value new value for the given index
     * @throws ArrayIndexOutOfBoundsException if {@code index} is outside
     * array bounds
     */
    public void set(int index, int value);

    /**
     * Returns an array containing copy of the observable array. 
     * If the observable array fits in the specified array, it is copied therein. 
     * Otherwise, a new array is allocated with the size of the observable array.
     *
     * @param dest the array into which the observable array to be copied,
     *          if it is big enough; otherwise, a new int array is allocated. 
     *          Ignored, if null.
     * @return an int array containing the copy of the observable array
     */
    public int[] toArray(int[] dest);

    /**
     * Returns an array containing copy of specified portion of the observable array. 
     * If specified portion of the observable array fits in the specified array, 
     * it is copied therein. Otherwise, a new array of given length is allocated.
     *
     * @param srcIndex starting position in the observable array
     * @param dest the array into which specified portion of the observable array 
     *          to be copied, if it is big enough; 
     *          otherwise, a new int array is allocated. 
     *          Ignored, if null.
     * @param length length of portion to copy
     * @return an int array containing the copy of specified portion the observable array
     */
    public int[] toArray(int srcIndex, int[] dest, int length);
    
}
