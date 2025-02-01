/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.util;

import com.sun.javafx.UnmodifiableArrayList;
import javafx.animation.Interpolatable;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Objects;

public final class InterpolationUtils {

    private InterpolationUtils() {}

    /**
     * Utility function that interpolates between two double values.
     */
    public static double interpolate(double from, double to, double t) {
        return from + t * (to - from);
    }

    /**
     * Utility function that interpolates between two discrete values, returning {@code from}
     * when {@code t < 0.5}, and {@code to} otherwise.
     */
    public static <T> T interpolateDiscrete(T from, T to, double t) {
        return t < 0.5 ? from : to;
    }

    /**
     * Utility function that interpolates between two discrete values, returning {@code from}
     * when {@code t < 0.5}, and {@code to} otherwise.
     */
    public static double interpolateDiscrete(double from, double to, double t) {
        return t < 0.5 ? from : to;
    }

    /**
     * Computes an intermediate list that consists of the pairwise interpolation between two lists,
     * using the following rules:
     * <ol>
     *     <li>The size of the returned list corresponds to the size of the second list.
     *     <li>If the first list has fewer elements than the second list, the missing elements are copied
     *         from the second list.
     *     <li>If the first list has more elements than the second list, the excess elements are discarded.
     *     <li>If the intermediate list is shallow-equal to the first list passed into the method (i.e. its
     *         elements are references to the same objects), the existing list is returned.
     *     <li>If a new list is returned, it is unmodifiable.
     * </ol>
     * This method preferably returns existing list instances (i.e. the {@code firstList} or
     * {@code secondList} arguments) as an indication to the caller that the result is shallow-equal
     * to either of the input arguments. Callers might depend on this behavior to optimize their own
     * object allocation strategy by quickly detecting whether anything has changed at all.
     *
     * @param firstList the first list, not {@code null}
     * @param secondList the second list, not {@code null}
     * @return the intermediate list
     */
    public static <T extends Interpolatable<T>> List<T> interpolateListsPairwise(
            List<T> firstList, List<T> secondList, double t) {
        Objects.requireNonNull(firstList, "firstList");
        Objects.requireNonNull(secondList, "secondList");

        if (secondList.isEmpty()) {
            return firstList.isEmpty() ? firstList : secondList;
        }

        int listSize = firstList.size();

        // For small equisized lists (up to 8 elements), we use an optimization to prevent the allocation
        // of a new array in case the intermediate list would be equal to the existing list.
        if (listSize <= 8 && listSize == secondList.size()) {
            return interpolateEquisizedListsPairwise(firstList, secondList, t);
        }

        @SuppressWarnings("unchecked")
        T[] newArray = (T[])new Interpolatable[secondList.size()];
        boolean equal = firstList.size() == secondList.size();

        for (int i = 0, firstListSize = firstList.size(); i < newArray.length; ++i) {
            if (firstListSize > i) {
                newArray[i] = firstList.get(i).interpolate(secondList.get(i), t);
                equal &= newArray[i] == firstList.get(i);
            } else {
                newArray[i] = secondList.get(i);
            }
        }

        return equal ? firstList : new UnmodifiableArrayList<>(newArray, newArray.length);
    }

    /**
     * Computes an intermediate list that consists of the pairwise interpolation between two lists
     * of equal size, each containing up to 8 elements.
     * <p>
     * This method is an optimization: it does not allocate memory when the intermediate list is
     * shallow-equal to the list that is passed into this method, i.e. its elements are references
     * to the same objects. The existing list is returned in this case.
     */
    private static <T extends Interpolatable<T>> List<T> interpolateEquisizedListsPairwise(
            List<T> firstList, List<T> secondList, double t) {
        int listSize = firstList.size();
        if (listSize > 8 || listSize != secondList.size()) {
            throw new AssertionError();
        }

        T item0 = null, item1 = null, item2 = null, item3 = null, item4 = null, item5 = null, item6 = null, item7 = null;
        boolean same = true;

        switch (listSize) { // fall-through intended
            case 8: item7 = firstList.get(7).interpolate(secondList.get(7), t);
                    same &= item7 == firstList.get(7);
            case 7: item6 = firstList.get(6).interpolate(secondList.get(6), t);
                    same &= item6 == firstList.get(6);
            case 6: item5 = firstList.get(5).interpolate(secondList.get(5), t);
                    same &= item5 == firstList.get(5);
            case 5: item4 = firstList.get(4).interpolate(secondList.get(4), t);
                    same &= item4 == firstList.get(4);
            case 4: item3 = firstList.get(3).interpolate(secondList.get(3), t);
                    same &= item3 == firstList.get(3);
            case 3: item2 = firstList.get(2).interpolate(secondList.get(2), t);
                    same &= item2 == firstList.get(2);
            case 2: item1 = firstList.get(1).interpolate(secondList.get(1), t);
                    same &= item1 == firstList.get(1);
            case 1: item0 = firstList.get(0).interpolate(secondList.get(0), t);
                    same &= item0 == firstList.get(0);
        }

        if (same) {
            return firstList;
        }

        @SuppressWarnings("unchecked")
        T[] newArray = (T[])new Interpolatable[listSize];

        switch (listSize) { // fall-through intended
            case 8: newArray[7] = item7;
            case 7: newArray[6] = item6;
            case 6: newArray[5] = item5;
            case 5: newArray[4] = item4;
            case 4: newArray[3] = item3;
            case 3: newArray[2] = item2;
            case 2: newArray[1] = item1;
            case 1: newArray[0] = item0;
        }

        return new UnmodifiableArrayList<>(newArray, listSize);
    }

    /**
     * Computes an intermediate array series that consists of the pairwise interpolation between two
     * array series, using the same rules as described in {@link #interpolateArraysPairwise}.
     * <p>
     * This method preferably returns existing array instances (i.e. the {@code firstArraySeries} or
     * {@code secondArraySeries} arguments) as an indication to the caller that the result is shallow-equal
     * to either of the input arguments. Callers might depend on this behavior to optimize their own
     * object allocation strategy by quickly detecting whether anything has changed at all.
     *
     * @param firstArraySeries the first array series, not {@code null}
     * @param secondArraySeries the second array series, not {@code null}
     * @return the intermediate array series
     */
    public static <T extends Interpolatable<T>> T[][] interpolateArraySeriesPairwise(
            T[][] firstArraySeries, T[][] secondArraySeries, double t) {
        Objects.requireNonNull(firstArraySeries, "firstArraySeries");
        Objects.requireNonNull(secondArraySeries, "secondArraySeries");

        if (secondArraySeries.length == 0) {
            return firstArraySeries.length == 0 ? firstArraySeries : secondArraySeries;
        }

        Class<?> arrayType = firstArraySeries.getClass().componentType();

        @SuppressWarnings("unchecked")
        T[][] newArray = (T[][]) Array.newInstance(arrayType, secondArraySeries.length);
        boolean equal = firstArraySeries.length == secondArraySeries.length;

        for (int i = 0, firstListSize = firstArraySeries.length; i < newArray.length; ++i) {
            if (firstListSize > i) {
                newArray[i] = interpolateArraysPairwise(firstArraySeries[i], secondArraySeries[i], t);
                equal &= newArray[i] == firstArraySeries[i];
            } else {
                newArray[i] = secondArraySeries[i];
            }
        }

        return equal ? firstArraySeries : newArray;
    }

    /**
     * Computes an intermediate array that consists of the pairwise interpolation between two arrays,
     * using the following rules:
     * <ol>
     *     <li>The size of the returned array corresponds to the size of the second array.
     *     <li>If the first array has fewer elements than the second array, the missing elements are copied
     *         from the second array.
     *     <li>If the first array has more elements than the second array, the excess elements are discarded.
     *     <li>If the intermediate array is shallow-equal to the first array passed into the method (i.e. its
     *         elements are references to the same objects), the existing array is returned.
     * </ol>
     * This method preferably returns existing array instances (i.e. the {@code firstArray} or
     * {@code secondArray} arguments) as an indication to the caller that the result is shallow-equal
     * to either of the input arguments. Callers might depend on this behavior to optimize their own
     * object allocation strategy by quickly detecting whether anything has changed at all.
     *
     * @param firstArray the first array, not {@code null}
     * @param secondArray the second array, not {@code null}
     * @return the intermediate list
     */
    public static <T extends Interpolatable<T>> T[] interpolateArraysPairwise(
            T[] firstArray, T[] secondArray, double t) {
        Objects.requireNonNull(firstArray, "firstArray");
        Objects.requireNonNull(secondArray, "secondArray");

        if (secondArray.length == 0) {
            return firstArray.length == 0 ? firstArray : secondArray;
        }

        int arraySize = firstArray.length;

        // For small equisized arrays (up to 8 elements), we use an optimization to prevent the allocation
        // of a new array in case the intermediate array would be equal to the existing array.
        if (arraySize <= 8 && arraySize == secondArray.length) {
            return interpolateEquisizedArraysPairwise(firstArray, secondArray, t);
        }

        Class<?> componentType = firstArray.getClass().componentType();

        @SuppressWarnings("unchecked")
        T[] newArray = (T[])Array.newInstance(componentType, secondArray.length);
        boolean equal = firstArray.length == secondArray.length;

        for (int i = 0; i < newArray.length; ++i) {
            if (arraySize > i) {
                newArray[i] = firstArray[i].interpolate(secondArray[i], t);
                equal &= newArray[i] == firstArray[i];
            } else {
                newArray[i] = secondArray[i];
            }
        }

        return equal ? firstArray : newArray;
    }

    /**
     * Computes an intermediate array that consists of the pairwise interpolation between two arrays
     * of equal size, each containing up to 8 elements.
     * <p>
     * This method is an optimization: it does not allocate memory when the intermediate array is
     * shallow-equal to the array that is passed into this method, i.e. its elements are references
     * to the same objects. The existing array is returned in this case.
     */
    private static <T extends Interpolatable<T>> T[] interpolateEquisizedArraysPairwise(
            T[] firstArray, T[] secondArray, double t) {
        int arraySize = firstArray.length;
        if (arraySize > 8 || arraySize != secondArray.length) {
            throw new AssertionError();
        }

        T item0 = null, item1 = null, item2 = null, item3 = null, item4 = null, item5 = null, item6 = null, item7 = null;
        boolean same = true;

        switch (arraySize) { // fall-through intended
            case 8: item7 = firstArray[7].interpolate(secondArray[7], t);
                    same &= item7 == firstArray[7];
            case 7: item6 = firstArray[6].interpolate(secondArray[6], t);
                    same &= item6 == firstArray[6];
            case 6: item5 = firstArray[5].interpolate(secondArray[5], t);
                    same &= item5 == firstArray[5];
            case 5: item4 = firstArray[4].interpolate(secondArray[4], t);
                    same &= item4 == firstArray[4];
            case 4: item3 = firstArray[3].interpolate(secondArray[3], t);
                    same &= item3 == firstArray[3];
            case 3: item2 = firstArray[2].interpolate(secondArray[2], t);
                    same &= item2 == firstArray[2];
            case 2: item1 = firstArray[1].interpolate(secondArray[1], t);
                    same &= item1 == firstArray[1];
            case 1: item0 = firstArray[0].interpolate(secondArray[0], t);
                    same &= item0 == firstArray[0];
        }

        if (same) {
            return firstArray;
        }

        Class<?> componentType = firstArray.getClass().componentType();

        @SuppressWarnings("unchecked")
        T[] newArray = (T[])Array.newInstance(componentType, arraySize);

        switch (arraySize) { // fall-through intended
            case 8: newArray[7] = item7;
            case 7: newArray[6] = item6;
            case 6: newArray[5] = item5;
            case 5: newArray[4] = item4;
            case 4: newArray[3] = item3;
            case 3: newArray[2] = item2;
            case 2: newArray[1] = item1;
            case 1: newArray[0] = item0;
        }

        return newArray;
    }

    /**
     * Interpolates between potentially different types of paint.
     * <p>
     * In addition to homogeneous interpolations between paints of the same type, the following
     * heterogeneous interpolations are supported:
     * <ul>
     *     <li>Color ↔ LinearGradient
     *     <li>Color ↔ RadialGradient
     * </ul>
     * If a paint is not interpolatable, {@code startValue} is returned for {@code t < 0.5},
     * and {@code endValue} is returned otherwise.
     */
    public static Paint interpolatePaint(Paint startValue, Paint endValue, double t) {
        if (startValue instanceof Color start) {
            if (endValue instanceof Color end) {
                return start.interpolate(end, t);
            }

            if (endValue instanceof LinearGradient end) {
                return newSolidGradient(end, start).interpolate(end, t);
            }

            if (endValue instanceof RadialGradient end) {
                return newSolidGradient(end, start).interpolate(end, t);
            }
        }

        if (startValue instanceof LinearGradient start) {
            if (endValue instanceof LinearGradient end) {
                return start.interpolate(end, t);
            }

            if (endValue instanceof Color end) {
                return start.interpolate(newSolidGradient(start, end), t);
            }
        }

        if (startValue instanceof RadialGradient start) {
            if (endValue instanceof RadialGradient end) {
                return start.interpolate(end, t);
            }

            if (endValue instanceof Color end) {
                return start.interpolate(newSolidGradient(start, end), t);
            }
        }

        if (startValue instanceof ImagePattern start && endValue instanceof ImagePattern end) {
            return start.interpolate(end, t);
        }

        return t < 0.5 ? startValue : endValue;
    }

    /**
     * Creates a new linear gradient that consists of two stops with the same color.
     */
    public static LinearGradient newSolidGradient(LinearGradient source, Color color) {
        return new LinearGradient(
                source.getStartX(), source.getStartY(),
                source.getEndX(), source.getEndY(),
                source.isProportional(),
                source.getCycleMethod(),
                List.of(new Stop(0, color), new Stop(1, color)));
    }

    /**
     * Creates a new radial gradient that consists of two stops with the same color.
     */
    public static RadialGradient newSolidGradient(RadialGradient source, Color color) {
        return new RadialGradient(
                source.getFocusAngle(), source.getFocusDistance(),
                source.getCenterX(), source.getCenterY(),
                source.getRadius(),
                source.isProportional(),
                source.getCycleMethod(),
                List.of(new Stop(0, color), new Stop(1, color)));
    }
}
