/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.collections.ArrayChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for ObservableArray.
 */
public class ObservableArrayTest  {
    public static final int INITIAL_SIZE = 6;

    /**
     * @param <T> ObservableArray subclass
     * @param <A> corresponding primitive array
     * @param <P> corresponding class for boxed elements
     */
    public static abstract class ArrayWrapper<T extends ObservableArray<T>, A, P> {

        T array;
        final T array() {
            return array;
        }

        abstract T createEmptyArray();
        abstract T createNotEmptyArray(A src);
        abstract ArrayWrapper<T, A, P> newInstance();
        abstract P getNextValue();
        abstract void set(int index, P value);
        abstract void setAllA(A src);
        abstract void setAllT(T src);
        abstract void setAllA(A src, int srcIndex, int length);
        abstract void setAllT(T src, int srcIndex, int length);
        abstract void addAllA(A src);
        abstract void addAllT(T src);
        abstract void addAllA(A src, int srcIndex, int length);
        abstract void addAllT(T src, int srcIndex, int length);
        abstract void setA(int destIndex, A src, int srcIndex, int length);
        abstract void setT(int destIndex, T src, int srcIndex, int length);
        abstract void copyToA(int srcIndex, A dest, int destIndex, int length);
        abstract void copyToT(int srcIndex, T dest, int destIndex, int length);
        abstract P get(int index);
        abstract A toArray(A dest);
        abstract A toArray(int srcIndex, A dest, int length);

        A createPrimitiveArray(int size) {
            return createPrimitiveArray(size, true);
        }
        abstract A createPrimitiveArray(int size, boolean fillWithData);
        abstract A clonePrimitiveArray(A array);
        abstract int arrayLength(A array);
        abstract P get(A array, int index);
        abstract void assertElementsEqual(A actual, int from, int to, A expected, int expFrom);
        abstract String primitiveArrayToString(A array);
    }

    private static class IntegerArrayWrapper extends ArrayWrapper<ObservableIntegerArray, int[], Integer> {

        int nextValue = 0;

        @Override IntegerArrayWrapper newInstance() {
            return new IntegerArrayWrapper();
        }

        @Override ObservableIntegerArray createEmptyArray() {
            return array = FXCollections.observableIntegerArray();
        }

        @Override ObservableIntegerArray createNotEmptyArray(int[] src) {
            return array = FXCollections.observableIntegerArray(src);
        }

        @Override Integer getNextValue() {
            return nextValue++;
        }

        @Override void set(int index, Integer value) {
            array.set(index, value);
        }

        @Override int[] createPrimitiveArray(int size, boolean fillWithData) {
            int[] res = new int[size];
            if (fillWithData) {
                for (int i = 0; i < size; i++) {
                    res[i] = nextValue++;
                }
            }
            return res;
        }

        @Override void setAllA(int[] src) {
            array.setAll(src);
        }

        @Override void setAllA(int[] src, int srcIndex, int length) {
            array.setAll(src, srcIndex, length);
        }

        @Override void setAllT(ObservableIntegerArray src) {
            array.setAll(src);
        }

        @Override void setAllT(ObservableIntegerArray src, int srcIndex, int length) {
            array.setAll(src, srcIndex, length);
        }

        @Override void addAllA(int[] src) {
            array.addAll(src);
        }

        @Override void addAllA(int[] src, int srcIndex, int length) {
            array.addAll(src, srcIndex, length);
        }

        @Override void addAllT(ObservableIntegerArray src) {
            array.addAll(src);
        }

        @Override void addAllT(ObservableIntegerArray src, int srcIndex, int length) {
            array.addAll(src, srcIndex, length);
        }

        @Override void copyToA(int srcIndex, int[] dest, int destIndex, int length) {
            array.copyTo(srcIndex, dest, destIndex, length);
        }

        @Override void copyToT(int srcIndex, ObservableIntegerArray dest, int destIndex, int length) {
            array.copyTo(srcIndex, dest, destIndex, length);
        }

        @Override Integer get(int index) {
            return array.get(index);
        }

        @Override int[] toArray(int[] src) {
            return array.toArray(src);
        }

        @Override int[] toArray(int srcIndex, int[] dest, int length) {
            return array.toArray(srcIndex, dest, length);
        }

        @Override void setA(int destIndex, int[] src, int srcIndex, int length) {
            array.set(destIndex, src, srcIndex, length);
        }

        @Override void setT(int destIndex, ObservableIntegerArray src, int srcIndex, int length) {
            array.set(destIndex, src, srcIndex, length);
        }

        @Override int arrayLength(int[] array) {
            return array.length;
        }

        @Override
        Integer get(int[] array, int index) {
            return array[index];
        }

        @Override
        void assertElementsEqual(int[] actual, int from, int to, int[] expected, int expFrom) {
            for(int i = from, j = expFrom; i < to; i++, j++) {
                assertEquals(actual[i], expected[j]);
            }
        }

        @Override int[] clonePrimitiveArray(int[] array) {
            return Arrays.copyOf(array, array.length);
        }

        @Override
        String primitiveArrayToString(int[] array) {
            return Arrays.toString(array);
        }
    }

    private static class FloatArrayWrapper extends ArrayWrapper<ObservableFloatArray, float[], Float> {

        float nextValue = 0;

        @Override FloatArrayWrapper newInstance() {
            return new FloatArrayWrapper();
        }

        @Override ObservableFloatArray createEmptyArray() {
            return array = FXCollections.observableFloatArray();
        }

        @Override ObservableFloatArray createNotEmptyArray(float[] elements) {
            return array = FXCollections.observableFloatArray(elements);
        }

        @Override
        Float getNextValue() {
            return nextValue++;
        }

        @Override void set(int index, Float value) {
            array.set(index, value);
        }

        @Override float[] createPrimitiveArray(int size, boolean fillWithData) {
            float[] res = new float[size];
            if (fillWithData) {
                for (int i = 0; i < size; i++) {
                    res[i] = nextValue++;
                }
            }
            return res;
        }

        @Override void setAllA(float[] src) {
            array.setAll(src);
        }

        @Override void copyToA(int srcIndex, float[] dest, int destIndex, int length) {
            array.copyTo(srcIndex, dest, destIndex, length);
        }

        @Override void copyToT(int srcIndex, ObservableFloatArray dest, int destIndex, int length) {
            array.copyTo(srcIndex, dest, destIndex, length);
        }

        @Override Float get(int index) {
            return array.get(index);
        }

        @Override float[] toArray(float[] dest) {
            return array.toArray(dest);
        }

        @Override float[] toArray(int srcIndex, float[] dest, int length) {
            return array.toArray(srcIndex, dest, length);
        }

        @Override void setA(int destIndex, float[] src, int srcIndex, int length) {
            array.set(destIndex, src, srcIndex, length);
        }

        @Override int arrayLength(float[] array) {
            return array.length;
        }

        @Override
        Float get(float[] array, int index) {
            return array[index];
        }

        @Override
        void assertElementsEqual(float[] actual, int from, int to, float[] expected, int expFrom) {
            for(int i = from, j = expFrom; i < to; i++, j++) {
                assertEquals(
                        Float.floatToRawIntBits(expected[j]),
                        Float.floatToRawIntBits(actual[i]),
                        "expected float = " + expected[j] + ", actual float = " + actual[i]
                );
            }
        }

        @Override float[] clonePrimitiveArray(float[] array) {
            return Arrays.copyOf(array, array.length);
        }

        @Override void setAllT(ObservableFloatArray src) {
            array.setAll(src);
        }

        @Override void setAllA(float[] src, int srcIndex, int length) {
            array.setAll(src, srcIndex, length);
        }

        @Override void setAllT(ObservableFloatArray src, int srcIndex, int length) {
            array.setAll(src, srcIndex, length);
        }

        @Override void addAllA(float[] src) {
            array.addAll(src);
        }

        @Override void addAllT(ObservableFloatArray src) {
            array.addAll(src);
        }

        @Override void addAllA(float[] src, int srcIndex, int length) {
            array.addAll(src, srcIndex, length);
        }

        @Override void addAllT(ObservableFloatArray src, int srcIndex, int length) {
            array.addAll(src, srcIndex, length);
        }

        @Override void setT(int destIndex, ObservableFloatArray src, int srcIndex, int length) {
            array.set(destIndex, src, srcIndex, length);
        }

        @Override
        String primitiveArrayToString(float[] array) {
            return Arrays.toString(array);
        }
    }

    static final List<String> EMPTY = Collections.emptyList();
    ArrayWrapper wrapper;
    private int initialSize;
    private Object initialElements;
    private ObservableArray array;
    private MockArrayObserver mao;

    public static Collection createParameters() {
        Object[][] data = new Object[][] {
                { new FloatArrayWrapper() },
                { new IntegerArrayWrapper() },
        };
        return Arrays.asList(data);
    }

    private void setUp(ArrayWrapper arrayWrapper) throws Exception {
        this.wrapper = arrayWrapper;
        initialSize = INITIAL_SIZE;
        initialElements = wrapper.createPrimitiveArray(initialSize);
        array = wrapper.createNotEmptyArray(initialElements);
        mao = new MockArrayObserver();
        array.addListener(mao);
    }

    private void makeEmpty() {
        initialSize = 0;
        initialElements = wrapper.createPrimitiveArray(initialSize);
        array.clear();
        mao.reset();
    }

    private void assertUnchanged() {
        mao.check0();
        assertEquals(initialSize, array.size());
        Object actual = wrapper.toArray(null);
        assertEquals(initialSize, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, array.size(), initialElements, 0);
    }

    // ========== pre-condition tests ================

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSize(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        mao.check0();
        assertEquals(INITIAL_SIZE, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.clear();
        mao.checkOnlySizeChanged(array);
        assertEquals(0, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testGet(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        for (int i = 0; i < array.size(); i++) {
            Object expected = wrapper.get(initialElements, i);
            Object actural = wrapper.get(i);
            assertEquals(expected, actural);
        }
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArray(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        Object expected = initialElements;
        Object actual = wrapper.toArray(null);
        assertEquals(INITIAL_SIZE, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, array.size(), expected, 0);
        assertUnchanged();
    }

    // ========== add/remove listener tests ==========

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddRemoveListener(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        MockArrayObserver mao2 = new MockArrayObserver();
        array.addListener(mao2);
        array.removeListener(mao);
        wrapper.set(0, wrapper.getNextValue());
        mao.check0();
        mao2.check(array, false, 0, 1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddTwoListenersElementChange(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        MockArrayObserver mao2 = new MockArrayObserver();
        array.addListener(mao2);
        wrapper.set(0, wrapper.getNextValue());
        mao.check(array, false, 0, 1);
        mao2.check(array, false, 0, 1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddTwoListenersSizeChange(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        MockArrayObserver mao2 = new MockArrayObserver();
        array.addListener(mao2);
        array.resize(3);
        mao.checkOnlySizeChanged(array);
        mao2.checkOnlySizeChanged(array);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddThreeListeners(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        MockArrayObserver mao2 = new MockArrayObserver();
        MockArrayObserver mao3 = new MockArrayObserver();
        array.addListener(mao2);
        array.addListener(mao3);
        wrapper.set(0, wrapper.getNextValue());
        mao.check(array, false, 0, 1);
        mao2.check(array, false, 0, 1);
        mao3.check(array, false, 0, 1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddThreeListenersSizeChange(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        MockArrayObserver mao2 = new MockArrayObserver();
        MockArrayObserver mao3 = new MockArrayObserver();
        array.addListener(mao2);
        array.addListener(mao3);
        array.resize(10);
        mao.checkOnlySizeChanged(array);
        mao2.checkOnlySizeChanged(array);
        mao3.checkOnlySizeChanged(array);
    }

    @Test @Disabled
    public void testAddListenerTwice() {
        array.addListener(mao); // add it a second time
        wrapper.set(1, wrapper.getNextValue());
        mao.check(array, false, 1, 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveListenerTwice(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        setUp(arrayWrapper);
        array.removeListener(mao);
        array.removeListener(mao);
        wrapper.set(1, wrapper.getNextValue());
        mao.check0();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddNullArrayChangeListener(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(NullPointerException.class, () -> {
            array.addListener((ArrayChangeListener) null);
        });

        // Finally block is outside assertThrows() to ensure it always executes
        mao.check0();
        array.resize(1);
        mao.check1();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddNullInvalidationListener(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        setUp(arrayWrapper);
        try {
            assertThrows(NullPointerException.class, () -> {
                array.addListener((ArrayChangeListener) null);
            });
        } finally {
            mao.check0();
            array.resize(1);
            mao.check1();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveNullArrayChangeListener(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        try {
            assertThrows(NullPointerException.class, () -> {
                array.addListener((ArrayChangeListener) null);
            });
        } finally {
            mao.check0();
            array.resize(1);
            mao.check1();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveNullInvalidationListener(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        try {
            assertThrows(NullPointerException.class, () -> {
                array.addListener((ArrayChangeListener) null);
            });
        } finally {
            mao.check0();
            array.resize(1);
            mao.check1();
        }
    }

    // ========== resize tests ==========

    private void testResize(boolean noChange, int newSize, int matchingElements) {
        Object expected = wrapper.toArray(null);
        array.resize(newSize);
        if (noChange) {
            assertUnchanged();
        } else {
            mao.checkOnlySizeChanged(array);
        }
        Object actual = wrapper.toArray(null);
        assertEquals(newSize, array.size());
        assertEquals(newSize, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, matchingElements, expected, 0);
        wrapper.assertElementsEqual(actual, matchingElements, newSize,
                wrapper.createPrimitiveArray(Math.max(0, newSize - matchingElements), false), 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testResizeTo0(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testResize(false, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testResizeToSmaller(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testResize(false, 3, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testResizeToSameSize(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testResize(true, array.size(), array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testResizeToBigger(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testResize(false, 10, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testResizeOnEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testResize(false, 10, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testResizeOnEmptyToEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testResize(true, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testResizeToNegative(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        try {
            assertThrows(NegativeArraySizeException.class, () -> {
                array.resize(-5);
            });
        } finally {
            assertUnchanged();
        }
    }

    // ========== setAll(primitive array) tests ==========

    private void testSetAllA(boolean sizeChanged, int newSize) {
        Object expected = wrapper.createPrimitiveArray(newSize);

        wrapper.setAllA(expected);

        mao.check(array, sizeChanged, 0, newSize);
        Object actual = wrapper.toArray(null);
        assertEquals(wrapper.arrayLength(expected), array.size());
        assertEquals(wrapper.arrayLength(expected), wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, wrapper.arrayLength(expected), expected, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllASmaller(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllA(true, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllABigger(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllA(true, 10);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllAOnSameSize(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllA(false, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllAOnEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testSetAllA(true, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllAOnEmptyToEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        wrapper.setAllA(wrapper.createPrimitiveArray(0));
        assertUnchanged();
        assertEquals(0, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllAToNull(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        try {
            assertThrows(NullPointerException.class, () -> {
                wrapper.setAllA(null);
            });
        } finally {
            assertUnchanged();
        }
    }

    // ========== setAll(ObservableArray) tests ==========

    private void testSetAllT(boolean sizeChanged, int newSize) {
        ArrayWrapper wrapper2 = wrapper.newInstance();
        Object expected = wrapper.createPrimitiveArray(newSize);
        ObservableArray src = wrapper2.createNotEmptyArray(expected);

        wrapper.setAllT(src);

        mao.check(array, sizeChanged, 0, newSize);
        Object actual = wrapper.toArray(null);
        assertEquals(wrapper.arrayLength(expected), array.size());
        assertEquals(wrapper.arrayLength(expected), wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, wrapper.arrayLength(expected), expected, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTSmaller(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllT(true, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTBigger(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllT(true, 10);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTOnSameSize(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllT(false, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTOnEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testSetAllT(true, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTOnEmptyToEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        wrapper.setAllT(wrapper.newInstance().createEmptyArray());
        assertUnchanged();
        assertEquals(0, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTToNull(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        try {
            assertThrows(NullPointerException.class, () -> {
                wrapper.setAllA(null);
            });
        } finally {
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTSelf(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        wrapper.setAllT(array);

        mao.check0();
        Object actual = wrapper.toArray(null);
        assertEquals(initialSize, array.size());
        assertEquals(initialSize, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, initialSize, initialElements, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTSelfEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();

        wrapper.setAllT(array);

        mao.check0();
        Object actual = wrapper.toArray(null);
        assertEquals(0, array.size());
        assertEquals(0, wrapper.arrayLength(actual));
    }

    // ========== setAll(primitive array, range) tests ==========

    private void testSetAllARange(boolean sizeChanged, int newSize, int srcIndex, int length) {
        Object expected = wrapper.createPrimitiveArray(newSize);

        wrapper.setAllA(expected, srcIndex, length);

        mao.check(array, sizeChanged, 0, length);
        Object actual = wrapper.toArray(null);
        assertEquals(length, array.size());
        assertEquals(length, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, length, expected, srcIndex);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARange1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllARange(false, INITIAL_SIZE, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARange2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllARange(false, INITIAL_SIZE + 10, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARange3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllARange(false, INITIAL_SIZE + 10, 10, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARange4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllARange(false, INITIAL_SIZE + 10, 2, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARange5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllARange(true, INITIAL_SIZE, 0, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARange6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllARange(true, INITIAL_SIZE + 10, 0, INITIAL_SIZE + 10);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARange7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllARange(true, INITIAL_SIZE + 20, 10, INITIAL_SIZE + 10);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARange8(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllARange(true, INITIAL_SIZE + 10, 2, INITIAL_SIZE - 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARangeOnEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testSetAllARange(true, INITIAL_SIZE, 1, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARangeOnEmptyToEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        wrapper.setAllA(wrapper.createPrimitiveArray(INITIAL_SIZE), 1, 0);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARangeToNull(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        try {
            assertThrows(NullPointerException.class, () -> {
                wrapper.setAllA(null, 0, 0);
            });
        } finally {
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARangeNegative1(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        try {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
                testSetAllARange(true, INITIAL_SIZE, -1, INITIAL_SIZE);
            });
        } finally {
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARangeNegative2(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        try {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
                testSetAllARange(true, INITIAL_SIZE, 0, INITIAL_SIZE + 1);
            });
        } finally {
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARangeNegative3(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        try {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
                testSetAllARange(true, INITIAL_SIZE, 1, -1);
            });
        } finally {
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllARangeNegative4(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        try {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
                testSetAllARange(true, INITIAL_SIZE, INITIAL_SIZE, 1);
            });
        } finally {
            assertUnchanged();
        }
    }

    private void testSetAllTRange(boolean sizeChanged, int srcSize, int srcIndex, int length) {
        Object expected = wrapper.createPrimitiveArray(srcSize);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(expected);

        wrapper.setAllT(src, srcIndex, length);

        mao.check(array, sizeChanged, 0, length);
        Object actual = wrapper.toArray(null);
        assertEquals(length, array.size());
        assertEquals(length, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, length, expected, srcIndex);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRange1(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testSetAllTRange(false, INITIAL_SIZE, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRange2(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testSetAllTRange(false, INITIAL_SIZE + 10, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRange3(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testSetAllTRange(false, INITIAL_SIZE + 10, 10, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRange4(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testSetAllTRange(false, INITIAL_SIZE + 10, 2, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRange5(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testSetAllTRange(true, INITIAL_SIZE, 0, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRange6(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testSetAllTRange(true, INITIAL_SIZE + 10, 0, INITIAL_SIZE + 10);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRange7(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testSetAllTRange(true, INITIAL_SIZE + 20, 10, INITIAL_SIZE + 10);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRange8(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testSetAllTRange(true, INITIAL_SIZE + 10, 2, INITIAL_SIZE - 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeOnEmpty(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        makeEmpty();
        testSetAllTRange(true, INITIAL_SIZE, 1, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeOnEmptyToEmpty(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        makeEmpty();
        wrapper.setAllT(wrapper.newInstance().createNotEmptyArray(wrapper.createPrimitiveArray(INITIAL_SIZE)), 1, 0);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeToNull(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        try {
            assertThrows(NullPointerException.class, () -> {
                wrapper.setAllT(null, 0, 0);
            });
        } finally {
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeNegative1(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        try {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
                testSetAllTRange(true, INITIAL_SIZE, -1, INITIAL_SIZE);
            });
        } finally {
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeNegative2(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        try {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
                testSetAllTRange(true, INITIAL_SIZE, 0, INITIAL_SIZE + 1);
            });
        } finally {
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeNegative3(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        try {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
                testSetAllTRange(true, INITIAL_SIZE, 1, -1);
            });
        } finally {
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeNegative4(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        try {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
                testSetAllTRange(true, INITIAL_SIZE, INITIAL_SIZE, 1);
            });
        } finally {
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetAllTRangeNegativeAfterSrcEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        Object expected = wrapper.createPrimitiveArray(INITIAL_SIZE);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(expected);
        src.ensureCapacity(INITIAL_SIZE * 2);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            wrapper.setAllT(src, INITIAL_SIZE, 1);
        });

        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetAllTRangeNegativeAfterSrcClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        Object expected = wrapper.createPrimitiveArray(INITIAL_SIZE);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(expected);
        src.clear();

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            wrapper.setAllT(src, 0, 1);
        });

        assertUnchanged();
    }


    private void testSetAllTRangeSelf(boolean sizeChanged, int srcIndex, int length) {

        wrapper.setAllT(array, srcIndex, length);

        if (srcIndex == 0) {
            if (length == initialSize) {
                mao.check0();
            } else {
                mao.checkOnlySizeChanged(array);
            }
        } else {
            mao.check(array, sizeChanged, 0, length);
        }
        Object actual = wrapper.toArray(null);
        assertEquals(length, array.size());
        assertEquals(length, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, length, initialElements, srcIndex);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeSelf(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllTRangeSelf(true, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeSelfBeginning(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllTRangeSelf(true, 0, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeSelfTrailing(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllTRangeSelf(true, INITIAL_SIZE / 2, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeSelfMiddle(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetAllTRangeSelf(true, 3, 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAllTRangeSelfEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testSetAllTRangeSelf(false, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetAllTRangeSelfNegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                wrapper.setAllT(array, -1, INITIAL_SIZE)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetAllTRangeSelfNegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                wrapper.setAllT(array, INITIAL_SIZE, 1)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetAllTRangeSelfNegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                wrapper.setAllT(array, 0, -1)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetAllTRangeSelfNegative4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                wrapper.setAllT(array, 0, INITIAL_SIZE + 1)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetAllTRangeSelfNegativeAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                wrapper.setAllT(array, INITIAL_SIZE, 1)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetAllTRangeSelfNegativeAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                wrapper.setAllT(array, 0, 1)
        );
        assertUnchanged();
    }

    // ========== addAll(primitive array) tests ==========

    private void testAddAllA(int srcSize) {
        Object src = wrapper.createPrimitiveArray(srcSize);
        int oldSize = array.size();

        wrapper.addAllA(src);

        int newSize = oldSize + srcSize;
        boolean sizeChanged = newSize != oldSize;
        mao.check(array, sizeChanged, oldSize, newSize);
        Object actual = wrapper.toArray(null);
        assertEquals(newSize, array.size());
        assertEquals(newSize, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, oldSize, initialElements, 0);
        wrapper.assertElementsEqual(actual, oldSize, newSize, src, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllA0(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        wrapper.addAllA(wrapper.createPrimitiveArray(0));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllA1(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testAddAllA(1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllA3(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testAddAllA(3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllABig(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllA(INITIAL_SIZE * 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllASameSize(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        testAddAllA(INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllAOnEmpty1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testAddAllA(1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllAOnEmptySameSize(ArrayWrapper arrayWrapper)  throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testAddAllA(INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllAOnEmptyBig(ArrayWrapper arrayWrapper)  throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testAddAllA(INITIAL_SIZE * 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllAOnEmpty0(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        wrapper.addAllA(wrapper.createPrimitiveArray(0));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllANull(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(NullPointerException.class, () ->
                wrapper.addAllA(null)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllAManyPoints(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        for (int i = 0; i < 65_000; i++) {
            wrapper.addAllA(wrapper.createPrimitiveArray(3));
        }
    }

    // ========== addAll(ObservableArray) tests ==========

    private void testAddAllT(int srcSize) {
        Object src = wrapper.createPrimitiveArray(srcSize);
        int oldSize = array.size();

        wrapper.addAllT(wrapper.newInstance().createNotEmptyArray(src));

        int newSize = oldSize + srcSize;
        boolean sizeChanged = newSize != oldSize;
        mao.check(array, sizeChanged, oldSize, newSize);
        Object actual = wrapper.toArray(null);
        assertEquals(newSize, array.size());
        assertEquals(newSize, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, oldSize, initialElements, 0);
        wrapper.assertElementsEqual(actual, oldSize, newSize, src, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllT0(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        wrapper.addAllT(wrapper.newInstance().createEmptyArray());
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllT1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllT(1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllT3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllT(3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTBig(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllT(INITIAL_SIZE * 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTSameSize(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllT(INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTOnEmpty1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testAddAllT(1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTOnEmptySameSize(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testAddAllT(INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTOnEmptyBig(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testAddAllT(INITIAL_SIZE * 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTOnEmpty0(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        wrapper.addAllT(wrapper.newInstance().createEmptyArray());
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTNull(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(NullPointerException.class, () ->
                wrapper.addAllT(null)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTSelf(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        wrapper.addAllT(array);

        mao.check(array, true, initialSize, initialSize * 2);
        assertEquals(initialSize * 2, array.size());
        Object actual = wrapper.toArray(null);
        wrapper.assertElementsEqual(actual, 0, initialSize, initialElements, 0);
        wrapper.assertElementsEqual(actual, initialSize, initialSize * 2, initialElements, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTSelfEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();

        wrapper.addAllT(array);

        mao.check0();
        Object actual = wrapper.toArray(null);
        assertEquals(0, array.size());
        assertEquals(0, wrapper.arrayLength(actual));
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTManyPoints(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        for (int i = 0; i < 65_000; i++) {
            wrapper.addAllT(wrapper.createNotEmptyArray(wrapper.createPrimitiveArray(3)));
        }
    }

    // ========== addAll(primitive array, range) tests ==========

    private void testAddAllARange(int srcSize, int srcIndex, int length) {
        Object src = wrapper.createPrimitiveArray(srcSize);
        int oldSize = array.size();

        wrapper.addAllA(src, srcIndex, length);

        int newSize = oldSize + length;
        boolean sizeChanged = newSize != oldSize;

        mao.check(array, sizeChanged, oldSize, newSize);
        Object actual = wrapper.toArray(null);
        assertEquals(newSize, array.size());
        assertEquals(newSize, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, oldSize, initialElements, 0);
        wrapper.assertElementsEqual(actual, oldSize, newSize, src, srcIndex);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARange1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllARange(INITIAL_SIZE, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARange2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllARange(INITIAL_SIZE + 10, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARange3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllARange(INITIAL_SIZE + 10, 10, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARange4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllARange(INITIAL_SIZE + 10, 2, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARange5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllARange(INITIAL_SIZE, 0, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARange6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllARange(INITIAL_SIZE + 10, 0, INITIAL_SIZE + 10);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARange7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllARange(INITIAL_SIZE + 20, 10, INITIAL_SIZE + 10);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARange8(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllARange(INITIAL_SIZE + 10, 2, INITIAL_SIZE - 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARangeOnEmpty1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testAddAllARange(INITIAL_SIZE, 1, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARangeOnEmpty2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testAddAllARange(INITIAL_SIZE * 3, INITIAL_SIZE, INITIAL_SIZE * 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllARangeOnEmpty3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        wrapper.addAllA(wrapper.createPrimitiveArray(INITIAL_SIZE), 1, 0);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllARangeNull(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(NullPointerException.class, () ->
                wrapper.addAllA(null, 0, 0)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllARangeNegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                testAddAllARange(INITIAL_SIZE, -1, INITIAL_SIZE)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllARangeNegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                testAddAllARange(INITIAL_SIZE, 0, INITIAL_SIZE + 1)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllARangeNegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                testAddAllARange(INITIAL_SIZE, 1, -1)
        );
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllARangeNegative4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                testAddAllARange(INITIAL_SIZE, INITIAL_SIZE, 1)
        );
        assertUnchanged();
    }

    // ========== addAll(observable array, range) tests ==========

    private void testAddAllTRange(int srcSize, int srcIndex, int length) {
        Object src = wrapper.createPrimitiveArray(srcSize);
        int oldSize = array.size();

        wrapper.addAllT(wrapper.newInstance().createNotEmptyArray(src), srcIndex, length);

        int newSize = oldSize + length;
        boolean sizeChanged = newSize != oldSize;

        mao.check(array, sizeChanged, oldSize, newSize);
        Object actual = wrapper.toArray(null);
        assertEquals(newSize, array.size());
        assertEquals(newSize, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, oldSize, initialElements, 0);
        wrapper.assertElementsEqual(actual, oldSize, newSize, src, srcIndex);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRange1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRange(INITIAL_SIZE, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRange2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRange(INITIAL_SIZE + 10, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRange3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRange(INITIAL_SIZE + 10, 10, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRange4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRange(INITIAL_SIZE + 10, 2, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRange5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRange(INITIAL_SIZE, 0, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRange6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRange(INITIAL_SIZE + 10, 0, INITIAL_SIZE + 10);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRange7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRange(INITIAL_SIZE + 20, 10, INITIAL_SIZE + 10);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRange8(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRange(INITIAL_SIZE + 10, 2, INITIAL_SIZE - 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRangeOnEmpty1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testAddAllTRange(INITIAL_SIZE, 1, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRangeOnEmpty2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testAddAllTRange(INITIAL_SIZE * 3, INITIAL_SIZE, INITIAL_SIZE * 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRangeOnEmpty3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        wrapper.addAllT(wrapper.newInstance().createNotEmptyArray(wrapper.createPrimitiveArray(INITIAL_SIZE)), 1, 0);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeNull(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(NullPointerException.class, () -> wrapper.addAllT(null, 0, 0));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeNegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testAddAllTRange(INITIAL_SIZE, -1, INITIAL_SIZE));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeNegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testAddAllTRange(INITIAL_SIZE, 0, INITIAL_SIZE + 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeNegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testAddAllTRange(INITIAL_SIZE, 1, -1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeNegative4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testAddAllTRange(INITIAL_SIZE, INITIAL_SIZE, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeNegativeAfterSrcEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        Object srcA = wrapper.createPrimitiveArray(INITIAL_SIZE);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(srcA);
        src.ensureCapacity(INITIAL_SIZE * 2);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.addAllT(src, INITIAL_SIZE, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeNegativeAfterSrcClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        Object srcA = wrapper.createPrimitiveArray(INITIAL_SIZE);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(srcA);
        src.clear();

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.addAllT(src, 0, 1));
        assertUnchanged();
    }

    private void testAddAllTRangeSelf(int srcIndex, int length) {
        wrapper.addAllT(array, srcIndex, length);

        int expSize = initialSize + length;
        mao.check(array, true, initialSize, expSize);
        Object actual = wrapper.toArray(null);
        assertEquals(expSize, array.size());
        assertEquals(expSize, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, initialSize, initialElements, 0);
        wrapper.assertElementsEqual(actual, initialSize, expSize, initialElements, srcIndex);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRangeSelf(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRangeSelf(0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRangeSelfBeginning(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRangeSelf(0, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRangeSelfTrailing(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRangeSelf(INITIAL_SIZE / 2, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAllTRangeSelfMiddle(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testAddAllTRangeSelf(2, 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeSelfNegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testAddAllTRangeSelf(-1, INITIAL_SIZE));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeSelfNegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testAddAllTRangeSelf(0, INITIAL_SIZE + 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeSelfNegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testAddAllTRangeSelf(1, -1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeSelfNegative4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testAddAllTRangeSelf(INITIAL_SIZE, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeSelfNegativeAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.addAllT(array, INITIAL_SIZE, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testAddAllTRangeSelfNegativeAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.addAllT(array, 0, 1));
        assertUnchanged();
    }

    // ========== set(primitive array, range) tests ==========

    private void testSetARange(int srcLength, int destIndex, int srcIndex, int length) {
        Object expected = wrapper.createPrimitiveArray(srcLength);

        wrapper.setA(destIndex, expected, srcIndex, length);

        mao.checkOnlyElementsChanged(array, destIndex, destIndex + length);
        Object actual = wrapper.toArray(null);
        assertEquals(INITIAL_SIZE, array.size());
        assertEquals(INITIAL_SIZE, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, destIndex, initialElements, 0);
        wrapper.assertElementsEqual(actual, destIndex, destIndex + length, expected, srcIndex);
        wrapper.assertElementsEqual(actual, destIndex + length, INITIAL_SIZE, initialElements, destIndex + length);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetARange1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetARange(5, 0, 0, 5);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetARange2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetARange(3, 2, 0, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetARange3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetARange(5, 0, 2, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetARange4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetARange(5, 0, 0, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetARange5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetARange(10, 3, 5, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetARangeNegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetARange(10, -1, 0, 3));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetARangeNegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetARange(10, 0, -1, 3));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetARangeNegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetARange(10, 1, 1, -1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetARangeNegative4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetARange(10, INITIAL_SIZE, 0, 3));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetARangeNegative5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetARange(10, 0, 10, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetARangeNegative6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetARange(3, 0, 1, 4));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetARangeNegative7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetARange(10, INITIAL_SIZE - 3, 0, 4));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetARangeNegativeAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetARange(1, INITIAL_SIZE, 0, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetARangeNegativeAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetARange(1, 0, 0, 1));
        assertUnchanged();
    }

    // ========== set(ObservableArray, range) tests ==========

    private void testSetTRange(int srcLength, int destIndex, int srcIndex, int length) {
        Object expected = wrapper.createPrimitiveArray(srcLength);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(expected);

        wrapper.setT(destIndex, src, srcIndex, length);

        mao.checkOnlyElementsChanged(array, destIndex, destIndex + length);
        Object actual = wrapper.toArray(null);
        assertEquals(INITIAL_SIZE, array.size());
        assertEquals(INITIAL_SIZE, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, destIndex, initialElements, 0);
        wrapper.assertElementsEqual(actual, destIndex, destIndex + length, expected, srcIndex);
        wrapper.assertElementsEqual(actual, destIndex + length, initialSize, initialElements, destIndex + length);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTRange1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetTRange(5, 0, 0, 5);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTRange2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetTRange(3, 2, 0, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTRange3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetTRange(5, 0, 2, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTRange4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetTRange(5, 0, 0, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTRange5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetTRange(10, 3, 5, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetTRange(10, -1, 0, 3));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetTRange(10, 0, -1, 3));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetTRange(10, 1, 1, -1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegative4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetTRange(10, INITIAL_SIZE, 0, 3));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegative5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetTRange(10, 0, 10, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegative6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetTRange(3, 0, 1, 4));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegative7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetTRange(10, INITIAL_SIZE - 3, 0, 4));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegativeAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetTRange(1, INITIAL_SIZE, 0, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegativeAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testSetTRange(1, 0, 0, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegativeAfterSrcEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        Object srcA = wrapper.createPrimitiveArray(1);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(srcA);
        src.ensureCapacity(2);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.setT(0, src, 1, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeNegativeAfterSrcClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        Object srcA = wrapper.createPrimitiveArray(1);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(srcA);
        src.clear();

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.setT(0, src, 0, 1));
        assertUnchanged();
    }

    private void testSetTRangeSelf(int destIndex, int srcIndex, int length) {

        wrapper.setT(destIndex, array, srcIndex, length);

        mao.checkOnlyElementsChanged(array, destIndex, destIndex + length);
        Object actual = wrapper.toArray(null);
        assertEquals(INITIAL_SIZE, array.size());
        assertEquals(INITIAL_SIZE, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, destIndex, initialElements, 0);
        wrapper.assertElementsEqual(actual, destIndex, destIndex + length, initialElements, srcIndex);
        wrapper.assertElementsEqual(actual, destIndex + length, initialSize, initialElements, destIndex + length);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTRangeSelf(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetTRangeSelf(0, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTRangeSelfLeft(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetTRangeSelf(0, 1, INITIAL_SIZE - 1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTRangeSelfRight(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetTRangeSelf(1, 0, INITIAL_SIZE - 1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTRangeSelfRightDifferentParts(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetTRangeSelf(0, INITIAL_SIZE / 2, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTRangeSelfLeftDifferentParts(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testSetTRangeSelf(INITIAL_SIZE / 2, 0, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeSelfNegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.setT(-1, array, 0, INITIAL_SIZE));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeSelfNegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.setT(0, array, -1, INITIAL_SIZE));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeSelfNegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.setT(0, array, 0, INITIAL_SIZE + 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeSelfNegative4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.setT(0, array, 1, -1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeSelfNegative5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.setT(INITIAL_SIZE, array, 0, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeSelfNegative6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.setT(0, array, INITIAL_SIZE, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeSelfNegativeAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.setT(0, array, INITIAL_SIZE, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetTRangeSelfNegativeAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.setT(0, array, 0, 1));
        assertUnchanged();
    }


    @ParameterizedTest
    @MethodSource("createParameters")
    void testGetNegative(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.get(-1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testGetOutOfBounds(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.get(array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testGetAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.get(INITIAL_SIZE));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testGetAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.get(0));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetValue(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        for (int i = 0; i < INITIAL_SIZE; i++) {
            Object expected = wrapper.getNextValue();

            wrapper.set(i, expected);

            mao.check(array, false, i, i + 1);
            mao.reset();
            assertEquals(expected, wrapper.get(i));
            assertEquals(INITIAL_SIZE, array.size());
        }
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetValueNegative(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.set(-1, wrapper.getNextValue()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetValueOutOfBounds(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.set(INITIAL_SIZE, wrapper.getNextValue()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetValueNegativeAfterClear(ArrayWrapper arrayWrapper) throws Exception  {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.set(0, wrapper.getNextValue()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testSetValueNegativeAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.set(INITIAL_SIZE, wrapper.getNextValue()));
        assertUnchanged();
    }

    // ================= toArray(array) tests ======================

    private void testToArray(int arraySize, boolean same) {
        Object param = wrapper.createPrimitiveArray(arraySize);
        Object actual = wrapper.toArray(param);
        assertUnchanged();
        if (same) {
            assertSame(param, actual);
        } else {
            assertNotSame(param, actual);
            assertEquals(array.size(), wrapper.arrayLength(actual));
        }
        wrapper.assertElementsEqual(actual, 0, array.size(), wrapper.toArray(null), 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArraySameSize(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArray(array.size(), true);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArraySmaller(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArray(3, false);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayBigger(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArray(10, true);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testToArray(10, true);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayEmptyToEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testToArray(0, true);
    }

    // ============ toArray range tests =========================

    private void testToArrayRange(int srcIndex, int destSize, int length) {
        Object dest = wrapper.createPrimitiveArray(destSize);
        Object initial = wrapper.clonePrimitiveArray(dest);
        Object actual = wrapper.toArray(srcIndex, dest, length);
        assertUnchanged();
        Object expected = wrapper.toArray(null);
        wrapper.assertElementsEqual(actual, 0, length, expected, srcIndex);
        wrapper.assertElementsEqual(actual, length, wrapper.arrayLength(actual), initial, length);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayRange0(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArrayRange(0, array.size(), array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayRange1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArrayRange(3, array.size(), array.size() - 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayRange2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArrayRange(0, array.size(), array.size() - 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayRange3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArrayRange(2, array.size(), 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayRange4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArrayRange(2, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayRange5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testToArrayRange(0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayRange6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArrayRange(3, 2, 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayRange7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArrayRange(5, 1, 1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayRange8(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArrayRange(0, array.size() * 2, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToArrayRange9(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testToArrayRange(0, array.size() - 1, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testToArrayRangeNegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testToArrayRange(-1, array.size(), 2));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testToArrayRangeNegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testToArrayRange(array.size(), array.size(), 2));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testToArrayRangeNegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testToArrayRange(5, array.size(), array.size() + 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testToArrayRangeNegative5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testToArrayRange(2, 0, 0));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testToArrayRangeNegativeAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testToArrayRange(INITIAL_SIZE, 1, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testToArrayRangeNegativeAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testToArrayRange(0, 1, 1));
        assertUnchanged();
    }

    // ============ copyTo(primitive array) tests =========================

    private void testCopyToA(int srcIndex, int destSize, int destIndex, int length) {
        Object actual = wrapper.createPrimitiveArray(destSize);
        Object initial = wrapper.clonePrimitiveArray(actual);
        wrapper.copyToA(srcIndex, actual, destIndex, length);
        assertUnchanged();
        Object expected = wrapper.toArray(null);
        wrapper.assertElementsEqual(actual, 0, destIndex, initial, 0);
        wrapper.assertElementsEqual(actual, destIndex, destIndex + length, expected, srcIndex);
        wrapper.assertElementsEqual(actual, destIndex + length, wrapper.arrayLength(actual), initial, destIndex + length);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToA0(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToA(0, array.size(), 0, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToA1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToA(1, array.size(), 2, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToA2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToA(2, array.size(), 2, 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToA3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToA(0, array.size(), 2, 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToA4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToA(0, 3, 1, 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToA5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToA(0, array.size() * 3, array.size() * 2, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToA6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToA(3, array.size(), 0, array.size() - 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToA7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToA(0, 10, 7, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToA8(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToA(1, 0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToA9(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testCopyToA(0, 0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToANegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToA(-1, array.size(), 0, array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToANegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToA(0, array.size() / 2, 0, array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToANegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToA(array.size(), array.size(), 0, array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToANegative4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToA(0, array.size(), -1, array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToANegative5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToA(0, array.size(), array.size(), array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToANegative6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToA(0, array.size(), 0, array.size() * 2));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToANegative7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToA(1, 0, 0, 0));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToANegative8(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToA(0, 0, 1, 0));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToANegativeAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToA(INITIAL_SIZE, 1, 0, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToANegativeAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToA(0, 1, 0, 1));
        assertUnchanged();
    }

    // ============ copyTo(ObservableArray) tests =========================

    private void testCopyToT(int srcIndex, int destSize, int destIndex, int length) {
        ArrayWrapper wrapper2 = wrapper.newInstance();
        Object initial = wrapper2.createPrimitiveArray(destSize);
        ObservableArray dest = wrapper2.createNotEmptyArray(initial);

        wrapper.copyToT(srcIndex, dest, destIndex, length);

        assertUnchanged();
        Object expected = wrapper.toArray(null);
        Object actual = wrapper2.toArray(null);
        wrapper.assertElementsEqual(actual, 0, destIndex, initial, 0);
        wrapper.assertElementsEqual(actual, destIndex, destIndex + length, expected, srcIndex);
        wrapper.assertElementsEqual(actual, destIndex + length, wrapper.arrayLength(actual), initial, destIndex + length);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToT0(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToT(0, array.size(), 0, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToT1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToT(1, array.size(), 2, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToT2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToT(2, array.size(), 2, 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToT3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToT(0, array.size(), 2, 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToT4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToT(0, 3, 1, 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToT5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToT(0, array.size() * 3, array.size() * 2, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToT6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToT(3, array.size(), 0, array.size() - 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToT7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToT(0, 10, 7, 3);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToT8(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToT(1, 0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToT9(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        testCopyToT(0, 0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToT(-1, array.size(), 0, array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToT(0, array.size() / 2, 0, array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToT(array.size(), array.size(), 0, array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegative4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToT(0, array.size(), -1, array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegative5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToT(0, array.size(), array.size(), array.size()));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegative6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToT(0, array.size(), 0, array.size() * 2));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegative7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToT(1, 0, 0, 0));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegative8(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToT(0, 0, 1, 0));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegativeAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToT(INITIAL_SIZE, 1, 0, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegativeAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToT(0, 1, 0, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegativeAfterDestEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        ArrayWrapper wrapper2 = wrapper.newInstance();
        Object destA = wrapper2.createPrimitiveArray(1);
        ObservableArray dest = wrapper2.createNotEmptyArray(destA);
        dest.ensureCapacity(2);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.copyToT(0, dest, 1, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTNegativeAfterDestClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        ArrayWrapper wrapper2 = wrapper.newInstance();
        Object destA = wrapper2.createPrimitiveArray(1);
        ObservableArray dest = wrapper2.createNotEmptyArray(destA);
        dest.clear();

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> wrapper.copyToT(0, dest, 0, 1));
        assertUnchanged();
    }

    private void testCopyToTSelf(int srcIndex, int destIndex, int length) {

        wrapper.copyToT(srcIndex, array, destIndex, length);

        mao.checkOnlyElementsChanged(array, destIndex, destIndex + length);
        Object actual = wrapper.toArray(null);
        wrapper.assertElementsEqual(actual, 0, destIndex, initialElements, 0);
        wrapper.assertElementsEqual(actual, destIndex, destIndex + length, initialElements, srcIndex);
        wrapper.assertElementsEqual(actual, destIndex + length, initialSize, initialElements, destIndex + length);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToTSelf(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToTSelf(0, 0, INITIAL_SIZE);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToTSelfRight(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToTSelf(0, 1, INITIAL_SIZE - 1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToTSelfLeft(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToTSelf(1, 0, INITIAL_SIZE - 1);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToTSelfRightDifferentParts(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToTSelf(0, INITIAL_SIZE / 2, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCopyToTSelfLeftDifferentParts(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        testCopyToTSelf(INITIAL_SIZE / 2, 0, INITIAL_SIZE / 2);
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTSelfNegative1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToTSelf(-1, 0, INITIAL_SIZE));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTSelfNegative2(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToTSelf(INITIAL_SIZE, 0, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTSelfNegative3(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToTSelf(0, -1, INITIAL_SIZE));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTSelfNegative4(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToTSelf(0, INITIAL_SIZE, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTSelfNegative5(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToTSelf(1, 1, -1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTSelfNegative6(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToTSelf(0, 1, INITIAL_SIZE));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTSelfNegative7(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToTSelf(1, 0, INITIAL_SIZE));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTSelfNegativeAfterEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE * 2);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToTSelf(0, INITIAL_SIZE, 1));
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    void testCopyToTSelfNegativeAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> testCopyToTSelf(0, 0, 1));
        assertUnchanged();
    }

    // ============ ensureCapacity() and trimToSize() tests ====================

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testTrimToSize(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.trimToSize();
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testTrimToSizeEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        array.trimToSize();
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testTrimToSizeResize(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.resize(3);
        initialSize = 3;
        mao.reset();

        array.trimToSize();

        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testTrimToSizeAddRemove(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.resize(1000);
        array.resize(INITIAL_SIZE);
        mao.reset();

        array.trimToSize();

        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEnsureCapacity0(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(0);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEnsureCapacityBy1(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE + 1);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEnsureCapacity1000(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(1000);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEnsureCapacitySmaller(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(INITIAL_SIZE / 2);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEnsureCapacityNegative(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(-1000);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEnsureCapacityOnEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        array.ensureCapacity(100);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEnsureCapacityOnEmpty0(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        array.ensureCapacity(0);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEnsureCapacityOnEmptyNegative(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        array.ensureCapacity(-1);
        assertUnchanged();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testTrimToSizeEnsureCapacity(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.ensureCapacity(1000);
        array.trimToSize();
        assertUnchanged();
    }

    // ================= clear() tests ====================

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testClearEmpty(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        makeEmpty();
        array.clear();
        mao.check0();
        assertEquals(0, array.size());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testClear1000(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.resize(1000);
        mao.reset();

        array.clear();

        mao.checkOnlySizeChanged(array);
        assertEquals(0, array.size());
    }

    // ================= toString() tests ===================

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToString(ArrayWrapper arrayWrapper) throws Exception{
        setUp(arrayWrapper);
        String actual = array.toString();
        String expected = wrapper.primitiveArrayToString(wrapper.toArray(null));
        assertEquals(expected, actual);
        String regex = "\\[[0-9]+(\\.[0-9]+){0,1}(\\, [0-9]+(.[0-9]+){0,1}){" + (initialSize - 1) + "}\\]";
        assertTrue(actual.matches(regex),
                () -> "toString() output matches regex '" + regex + "'. Actual = '" + actual + "'");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToStringAfterResize(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.resize(initialSize / 2);
        String actual = array.toString();
        String expected = wrapper.primitiveArrayToString(wrapper.toArray(null));
        assertEquals(expected, actual);
        String regex = "\\[[0-9]+(\\.[0-9]+){0,1}(\\, [0-9]+(.[0-9]+){0,1}){" + (array.size() - 1) + "}\\]";
        assertTrue(actual.matches(regex),
                () -> "toString() output does not match regex '" + regex + "'. Actual = '" + actual + "'");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testToStringAfterClear(ArrayWrapper arrayWrapper) throws Exception {
        setUp(arrayWrapper);
        array.clear();
        String actual = array.toString();
        assertEquals("[]", actual);
    }
}
