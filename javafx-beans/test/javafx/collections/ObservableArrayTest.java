/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.beans.InvalidationListener;

import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests for ObservableArray.
 */
@RunWith(Parameterized.class)
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
                assertEquals("expected = " + expected[j] + ", actual = " + actual[i],
                        Float.floatToRawIntBits(actual[i]),
                        Float.floatToRawIntBits(expected[j]));
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
    final ArrayWrapper wrapper;
    private int initialSize;
    private Object initialElements;
    private ObservableArray array;
    private MockArrayObserver mao;

    public ObservableArrayTest(final ArrayWrapper arrayWrapper) {
        this.wrapper = arrayWrapper;
    }

    @Parameterized.Parameters
    public static Collection createParameters() {
        Object[][] data = new Object[][] {
            { new FloatArrayWrapper() },
            { new IntegerArrayWrapper() },
         };
        return Arrays.asList(data);
    }

    @Before
    public void setUp() throws Exception {
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

    @Test public void testSize() {
        mao.check0();
        assertEquals(INITIAL_SIZE, array.size());
    }

    @Test public void testClear() {
        array.clear();
        mao.checkOnlySizeChanged(array);
        assertEquals(0, array.size());
    }

    @Test public void testGet() {
        for (int i = 0; i < array.size(); i++) {
            Object expected = wrapper.get(initialElements, i);
            Object actural = wrapper.get(i);
            assertEquals(expected, actural);
        }
        assertUnchanged();
    }

    @Test public void testToArray() {
        Object expected = initialElements;
        Object actual = wrapper.toArray(null);
        assertEquals(INITIAL_SIZE, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, array.size(), expected, 0);
        assertUnchanged();
    }

    // ========== add/remove listener tests ==========

    @Test public void testAddRemoveListener() {
        MockArrayObserver mao2 = new MockArrayObserver();
        array.addListener(mao2);
        array.removeListener(mao);
        wrapper.set(0, wrapper.getNextValue());
        mao.check0();
        mao2.check(array, false, 0, 1);
    }

    @Test public void testAddTwoListenersElementChange() {
        MockArrayObserver mao2 = new MockArrayObserver();
        array.addListener(mao2);
        wrapper.set(0, wrapper.getNextValue());
        mao.check(array, false, 0, 1);
        mao2.check(array, false, 0, 1);
    }

    @Test public void testAddTwoListenersSizeChange() {
        MockArrayObserver mao2 = new MockArrayObserver();
        array.addListener(mao2);
        array.resize(3);
        mao.checkOnlySizeChanged(array);
        mao2.checkOnlySizeChanged(array);
    }

    @Test public void testAddThreeListeners() {
        MockArrayObserver mao2 = new MockArrayObserver();
        MockArrayObserver mao3 = new MockArrayObserver();
        array.addListener(mao2);
        array.addListener(mao3);
        wrapper.set(0, wrapper.getNextValue());
        mao.check(array, false, 0, 1);
        mao2.check(array, false, 0, 1);
        mao3.check(array, false, 0, 1);
    }

    @Test public void testAddThreeListenersSizeChange() {
        MockArrayObserver mao2 = new MockArrayObserver();
        MockArrayObserver mao3 = new MockArrayObserver();
        array.addListener(mao2);
        array.addListener(mao3);
        array.resize(10);
        mao.checkOnlySizeChanged(array);
        mao2.checkOnlySizeChanged(array);
        mao3.checkOnlySizeChanged(array);
    }

    @Test @Ignore
    public void testAddListenerTwice() {
        array.addListener(mao); // add it a second time
        wrapper.set(1, wrapper.getNextValue());
        mao.check(array, false, 1, 2);
    }

    @Test public void testRemoveListenerTwice() {
        array.removeListener(mao);
        array.removeListener(mao);
        wrapper.set(1, wrapper.getNextValue());
        mao.check0();
    }

    @Test (expected = NullPointerException.class)
    public void testAddNullArrayChangeListener() {
        try {
            array.addListener((ArrayChangeListener) null);
        } finally {
            mao.check0();
            array.resize(1);
            mao.check1();
        }
    }

    @Test (expected = NullPointerException.class)
    public void testAddNullInvalidationListener() {
        try {
            array.addListener((InvalidationListener) null);
        } finally {
            mao.check0();
            array.resize(1);
            mao.check1();
        }
    }

    @Test (expected = NullPointerException.class)
    public void testRemoveNullArrayChangeListener() {
        try {
            array.removeListener((ArrayChangeListener) null);
        } finally {
            mao.check0();
            array.resize(1);
            mao.check1();
        }
    }

    @Test (expected = NullPointerException.class)
    public void testRemoveNullInvalidationListener() {
        try {
            array.removeListener((InvalidationListener) null);
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
                wrapper.createPrimitiveArray(Math.max(0, newSize - matchingElements)), 0);
    }

    @Test public void testResizeTo0() {
        testResize(false, 0, 0);
    }

    @Test public void testResizeToSmaller() {
        testResize(false, 3, 3);
    }

    @Test public void testResizeToSameSize() {
        testResize(true, array.size(), array.size());
    }

    @Ignore("RT-30865")
    @Test public void testResizeToBigger() {
        testResize(false, 10, array.size());
    }

    @Ignore("RT-30865")
    @Test public void testResizeOnEmpty() {
        makeEmpty();
        testResize(false, 10, 0);
    }

    @Test public void testResizeOnEmptyToEmpty() {
        makeEmpty();
        testResize(true, 0, 0);
    }

    @Test (expected = NegativeArraySizeException.class)
    public void testResizeToNegative() {
        try {
            array.resize(-5);
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

    @Test public void testSetAllASmaller() {
        testSetAllA(true, 3);
    }

    @Test public void testSetAllABigger() {
        testSetAllA(true, 10);
    }

    @Test public void testSetAllAOnSameSize() {
        testSetAllA(false, INITIAL_SIZE);
    }

    @Test public void testSetAllAOnEmpty() {
        makeEmpty();
        testSetAllA(true, 3);
    }

    @Test public void testSetAllAOnEmptyToEmpty() {
        makeEmpty();
        wrapper.setAllA(wrapper.createPrimitiveArray(0));
        assertUnchanged();
        assertEquals(0, array.size());
    }

    @Test (expected = NullPointerException.class)
    public void testSetAllAToNull() {
        try {
            wrapper.setAllA(null);
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

    @Test public void testSetAllTSmaller() {
        testSetAllT(true, 3);
    }

    @Test public void testSetAllTBigger() {
        testSetAllT(true, 10);
    }

    @Test public void testSetAllTOnSameSize() {
        testSetAllT(false, INITIAL_SIZE);
    }

    @Test public void testSetAllTOnEmpty() {
        makeEmpty();
        testSetAllT(true, 3);
    }

    @Test public void testSetAllTOnEmptyToEmpty() {
        makeEmpty();
        wrapper.setAllT(wrapper.newInstance().createEmptyArray());
        assertUnchanged();
        assertEquals(0, array.size());
    }

    @Test (expected = NullPointerException.class)
    public void testSetAllTToNull() {
        try {
            wrapper.setAllT(null);
        } finally {
            assertUnchanged();
        }
    }

    @Test public void testSetAllTSelf() {

        wrapper.setAllT(array);

        mao.check0();
        Object actual = wrapper.toArray(null);
        assertEquals(initialSize, array.size());
        assertEquals(initialSize, wrapper.arrayLength(actual));
        wrapper.assertElementsEqual(actual, 0, initialSize, initialElements, 0);
    }

    @Test public void testSetAllTSelfEmpty() {
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

    @Test public void testSetAllARange1() {
        testSetAllARange(false, INITIAL_SIZE, 0, INITIAL_SIZE);
    }

    @Test public void testSetAllARange2() {
        testSetAllARange(false, INITIAL_SIZE + 10, 0, INITIAL_SIZE);
    }

    @Test public void testSetAllARange3() {
        testSetAllARange(false, INITIAL_SIZE + 10, 10, INITIAL_SIZE);
    }

    @Test public void testSetAllARange4() {
        testSetAllARange(false, INITIAL_SIZE + 10, 2, INITIAL_SIZE);
    }

    @Test public void testSetAllARange5() {
        testSetAllARange(true, INITIAL_SIZE, 0, INITIAL_SIZE / 2);
    }

    @Test public void testSetAllARange6() {
        testSetAllARange(true, INITIAL_SIZE + 10, 0, INITIAL_SIZE + 10);
    }

    @Test public void testSetAllARange7() {
        testSetAllARange(true, INITIAL_SIZE + 20, 10, INITIAL_SIZE + 10);
    }

    @Test public void testSetAllARange8() {
        testSetAllARange(true, INITIAL_SIZE + 10, 2, INITIAL_SIZE - 3);
    }

    @Test public void testSetAllARangeOnEmpty() {
        makeEmpty();
        testSetAllARange(true, INITIAL_SIZE, 1, 3);
    }

    @Test public void testSetAllARangeOnEmptyToEmpty() {
        makeEmpty();
        wrapper.setAllA(wrapper.createPrimitiveArray(INITIAL_SIZE), 1, 0);
        assertUnchanged();
    }

    @Test (expected = NullPointerException.class)
    public void testSetAllARangeToNull() {
        try {
            wrapper.setAllA(null, 0, 0);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllARangeNegative1() {
        try {
            testSetAllARange(true, INITIAL_SIZE, -1, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllARangeNegative2() {
        try {
            testSetAllARange(true, INITIAL_SIZE, 0, INITIAL_SIZE + 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllARangeNegative3() {
        try {
            testSetAllARange(true, INITIAL_SIZE, 1, -1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllARangeNegative4() {
        try {
            testSetAllARange(true, INITIAL_SIZE, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    // ========== setAll(observable array, range) tests ==========

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

    @Test public void testSetAllTRange1() {
        testSetAllTRange(false, INITIAL_SIZE, 0, INITIAL_SIZE);
    }

    @Test public void testSetAllTRange2() {
        testSetAllTRange(false, INITIAL_SIZE + 10, 0, INITIAL_SIZE);
    }

    @Test public void testSetAllTRange3() {
        testSetAllTRange(false, INITIAL_SIZE + 10, 10, INITIAL_SIZE);
    }

    @Test public void testSetAllTRange4() {
        testSetAllTRange(false, INITIAL_SIZE + 10, 2, INITIAL_SIZE);
    }

    @Test public void testSetAllTRange5() {
        testSetAllTRange(true, INITIAL_SIZE, 0, INITIAL_SIZE / 2);
    }

    @Test public void testSetAllTRange6() {
        testSetAllTRange(true, INITIAL_SIZE + 10, 0, INITIAL_SIZE + 10);
    }

    @Test public void testSetAllTRange7() {
        testSetAllTRange(true, INITIAL_SIZE + 20, 10, INITIAL_SIZE + 10);
    }

    @Test public void testSetAllTRange8() {
        testSetAllTRange(true, INITIAL_SIZE + 10, 2, INITIAL_SIZE - 3);
    }

    @Test public void testSetAllTRangeOnEmpty() {
        makeEmpty();
        testSetAllTRange(true, INITIAL_SIZE, 1, 3);
    }

    @Test public void testSetAllTRangeOnEmptyToEmpty() {
        makeEmpty();
        wrapper.setAllT(wrapper.newInstance().createNotEmptyArray(wrapper.createPrimitiveArray(INITIAL_SIZE)), 1, 0);
        assertUnchanged();
    }

    @Test (expected = NullPointerException.class)
    public void testSetAllTRangeToNull() {
        try {
            wrapper.setAllT(null, 0, 0);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeNegative1() {
        try {
            testSetAllTRange(true, INITIAL_SIZE, -1, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeNegative2() {
        try {
            testSetAllTRange(true, INITIAL_SIZE, 0, INITIAL_SIZE + 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeNegative3() {
        try {
            testSetAllTRange(true, INITIAL_SIZE, 1, -1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeNegative4() {
        try {
            testSetAllTRange(true, INITIAL_SIZE, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeNegativeAfterSrcEnsureCapacity() {
        Object expected = wrapper.createPrimitiveArray(INITIAL_SIZE);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(expected);
        src.ensureCapacity(INITIAL_SIZE * 2);
        try {
            wrapper.setAllT(src, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeNegativeAfterSrcClear() {
        Object expected = wrapper.createPrimitiveArray(INITIAL_SIZE);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(expected);
        src.clear();
        try {
            wrapper.setAllT(src, 0, 1);
        } finally {
            assertUnchanged();
        }
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

    @Test public void testSetAllTRangeSelf() {
        testSetAllTRangeSelf(true, 0, INITIAL_SIZE);
    }

    @Test public void testSetAllTRangeSelfBeginning() {
        testSetAllTRangeSelf(true, 0, INITIAL_SIZE / 2);
    }

    @Test public void testSetAllTRangeSelfTrailing() {
        testSetAllTRangeSelf(true, INITIAL_SIZE / 2, INITIAL_SIZE / 2);
    }

    @Test public void testSetAllTRangeSelfMiddle() {
        testSetAllTRangeSelf(true, 3, 2);
    }

    @Test public void testSetAllTRangeSelfEmpty() {
        makeEmpty();
        testSetAllTRangeSelf(false, 0, 0);
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeSelfNegative1() {
        try {
            wrapper.setAllT(array, -1, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeSelfNegative2() {
        try {
            wrapper.setAllT(array, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeSelfNegative3() {
        try {
            wrapper.setAllT(array, 0, -1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeSelfNegative4() {
        try {
            wrapper.setAllT(array, 0, INITIAL_SIZE + 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeSelfNegativeAfterEnsureCapacity() {
        array.ensureCapacity(INITIAL_SIZE * 2);
        try {
            wrapper.setAllT(array, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetAllTRangeSelfNegativeAfterClear() {
        makeEmpty();
        try {
            wrapper.setAllT(array, 0, 1);
        } finally {
            assertUnchanged();
        }
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

    @Test public void testAddAllA0() {
        wrapper.addAllA(wrapper.createPrimitiveArray(0));
        assertUnchanged();
    }

    @Test public void testAddAllA1() {
        testAddAllA(1);
    }

    @Test public void testAddAllA3() {
        testAddAllA(3);
    }

    @Test public void testAddAllABig() {
        testAddAllA(INITIAL_SIZE * 2);
    }

    @Test public void testAddAllASameSize() {
        testAddAllA(INITIAL_SIZE);
    }

    @Test public void testAddAllAOnEmpty1() {
        makeEmpty();
        testAddAllA(1);
    }

    @Test public void testAddAllAOnEmptySameSize() {
        makeEmpty();
        testAddAllA(INITIAL_SIZE);
    }

    @Test public void testAddAllAOnEmptyBig() {
        makeEmpty();
        testAddAllA(INITIAL_SIZE * 3);
    }

    @Test public void testAddAllAOnEmpty0() {
        makeEmpty();
        wrapper.addAllA(wrapper.createPrimitiveArray(0));
        assertUnchanged();
    }

    @Test (expected = NullPointerException.class)
    public void testAddAllANull() {
        try {
            wrapper.addAllA(null);
        } finally {
            assertUnchanged();
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

    @Test public void testAddAllT0() {
        wrapper.addAllT(wrapper.newInstance().createEmptyArray());
        assertUnchanged();
    }

    @Test public void testAddAllT1() {
        testAddAllT(1);
    }

    @Test public void testAddAllT3() {
        testAddAllT(3);
    }

    @Test public void testAddAllTBig() {
        testAddAllT(INITIAL_SIZE * 2);
    }

    @Test public void testAddAllTSameSize() {
        testAddAllT(INITIAL_SIZE);
    }

    @Test public void testAddAllTOnEmpty1() {
        makeEmpty();
        testAddAllT(1);
    }

    @Test public void testAddAllTOnEmptySameSize() {
        makeEmpty();
        testAddAllT(INITIAL_SIZE);
    }

    @Test public void testAddAllTOnEmptyBig() {
        makeEmpty();
        testAddAllT(INITIAL_SIZE * 3);
    }

    @Test public void testAddAllTOnEmpty0() {
        makeEmpty();
        wrapper.addAllT(wrapper.newInstance().createEmptyArray());
        assertUnchanged();
    }

    @Test (expected = NullPointerException.class)
    public void testAddAllTNull() {
        try {
            wrapper.addAllT(null);
        } finally {
            assertUnchanged();
        }
    }

    @Test public void testAddAllTSelf() {
        wrapper.addAllT(array);

        mao.check(array, true, initialSize, initialSize * 2);
        assertEquals(initialSize * 2, array.size());
        Object actual = wrapper.toArray(null);
        wrapper.assertElementsEqual(actual, 0, initialSize, initialElements, 0);
        wrapper.assertElementsEqual(actual, initialSize, initialSize * 2, initialElements, 0);
    }

    @Test public void testAddAllTSelfEmpty() {
        makeEmpty();

        wrapper.addAllT(array);

        mao.check0();
        Object actual = wrapper.toArray(null);
        assertEquals(0, array.size());
        assertEquals(0, wrapper.arrayLength(actual));
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

    @Test public void testAddAllARange1() {
        testAddAllARange(INITIAL_SIZE, 0, INITIAL_SIZE);
    }

    @Test public void testAddAllARange2() {
        testAddAllARange(INITIAL_SIZE + 10, 0, INITIAL_SIZE);
    }

    @Test public void testAddAllARange3() {
        testAddAllARange(INITIAL_SIZE + 10, 10, INITIAL_SIZE);
    }

    @Test public void testAddAllARange4() {
        testAddAllARange(INITIAL_SIZE + 10, 2, INITIAL_SIZE);
    }

    @Test public void testAddAllARange5() {
        testAddAllARange(INITIAL_SIZE, 0, INITIAL_SIZE / 2);
    }

    @Test public void testAddAllARange6() {
        testAddAllARange(INITIAL_SIZE + 10, 0, INITIAL_SIZE + 10);
    }

    @Test public void testAddAllARange7() {
        testAddAllARange(INITIAL_SIZE + 20, 10, INITIAL_SIZE + 10);
    }

    @Test public void testAddAllARange8() {
        testAddAllARange(INITIAL_SIZE + 10, 2, INITIAL_SIZE - 3);
    }

    @Test public void testAddAllARangeOnEmpty1() {
        makeEmpty();
        testAddAllARange(INITIAL_SIZE, 1, 3);
    }

    @Test public void testAddAllARangeOnEmpty2() {
        makeEmpty();
        testAddAllARange(INITIAL_SIZE * 3, INITIAL_SIZE, INITIAL_SIZE * 2);
    }

    @Test public void testAddAllARangeOnEmpty3() {
        makeEmpty();
        wrapper.addAllA(wrapper.createPrimitiveArray(INITIAL_SIZE), 1, 0);
        assertUnchanged();
    }

    @Test (expected = NullPointerException.class)
    public void testAddAllARangeNull() {
        try {
            wrapper.addAllA(null, 0, 0);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllARangeNegative1() {
        try {
            testAddAllARange(INITIAL_SIZE, -1, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllARangeNegative2() {
        try {
            testAddAllARange(INITIAL_SIZE, 0, INITIAL_SIZE + 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllARangeNegative3() {
        try {
            testAddAllARange(INITIAL_SIZE, 1, -1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllARangeNegative4() {
        try {
            testAddAllARange(INITIAL_SIZE, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
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

    @Test public void testAddAllTRange1() {
        testAddAllTRange(INITIAL_SIZE, 0, INITIAL_SIZE);
    }

    @Test public void testAddAllTRange2() {
        testAddAllTRange(INITIAL_SIZE + 10, 0, INITIAL_SIZE);
    }

    @Test public void testAddAllTRange3() {
        testAddAllTRange(INITIAL_SIZE + 10, 10, INITIAL_SIZE);
    }

    @Test public void testAddAllTRange4() {
        testAddAllTRange(INITIAL_SIZE + 10, 2, INITIAL_SIZE);
    }

    @Test public void testAddAllTRange5() {
        testAddAllTRange(INITIAL_SIZE, 0, INITIAL_SIZE / 2);
    }

    @Test public void testAddAllTRange6() {
        testAddAllTRange(INITIAL_SIZE + 10, 0, INITIAL_SIZE + 10);
    }

    @Test public void testAddAllTRange7() {
        testAddAllTRange(INITIAL_SIZE + 20, 10, INITIAL_SIZE + 10);
    }

    @Test public void testAddAllTRange8() {
        testAddAllTRange(INITIAL_SIZE + 10, 2, INITIAL_SIZE - 3);
    }

    @Test public void testAddAllTRangeOnEmpty1() {
        makeEmpty();
        testAddAllTRange(INITIAL_SIZE, 1, 3);
    }

    @Test public void testAddAllTRangeOnEmpty2() {
        makeEmpty();
        testAddAllTRange(INITIAL_SIZE * 3, INITIAL_SIZE, INITIAL_SIZE * 2);
    }

    @Test public void testAddAllTRangeOnEmpty3() {
        makeEmpty();
        wrapper.addAllT(wrapper.newInstance().createNotEmptyArray(wrapper.createPrimitiveArray(INITIAL_SIZE)), 1, 0);
        assertUnchanged();
    }

    @Test (expected = NullPointerException.class)
    public void testAddAllTRangeNull() {
        try {
            wrapper.addAllT(null, 0, 0);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeNegative1() {
        try {
            testAddAllTRange(INITIAL_SIZE, -1, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeNegative2() {
        try {
            testAddAllTRange(INITIAL_SIZE, 0, INITIAL_SIZE + 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeNegative3() {
        try {
            testAddAllTRange(INITIAL_SIZE, 1, -1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeNegative4() {
        try {
            testAddAllTRange(INITIAL_SIZE, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeNegativeAfterSrcEnsureCapacity() {
        Object srcA = wrapper.createPrimitiveArray(INITIAL_SIZE);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(srcA);
        src.ensureCapacity(INITIAL_SIZE * 2);
        try {
            wrapper.addAllT(src, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeNegativeAfterSrcClear() {
        Object srcA = wrapper.createPrimitiveArray(INITIAL_SIZE);
        ObservableArray src = wrapper.newInstance().createNotEmptyArray(srcA);
        src.clear();
        try {
            wrapper.addAllT(src, 0, 1);
        } finally {
            assertUnchanged();
        }
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

    @Test public void testAddAllTRangeSelf() {
        testAddAllTRangeSelf(0, INITIAL_SIZE);
    }

    @Test public void testAddAllTRangeSelfBeginning() {
        testAddAllTRangeSelf(0, INITIAL_SIZE / 2);
    }

    @Test public void testAddAllTRangeSelfTrailing() {
        testAddAllTRangeSelf(INITIAL_SIZE / 2, INITIAL_SIZE / 2);
    }

    @Test public void testAddAllTRangeSelfMiddle() {
        testAddAllTRangeSelf(2, 2);
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeSelfNegative1() {
        try {
            testAddAllTRangeSelf(-1, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeSelfNegative2() {
        try {
            testAddAllTRangeSelf(0, INITIAL_SIZE + 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeSelfNegative3() {
        try {
            testAddAllTRangeSelf(1, -1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeSelfNegative4() {
        try {
            testAddAllTRangeSelf(INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeSelfNegativeAfterEnsureCapacity() {
        array.ensureCapacity(INITIAL_SIZE * 2);
        try {
            wrapper.addAllT(array, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testAddAllTRangeSelfNegativeAfterClear() {
        makeEmpty();
        try {
            wrapper.addAllT(array, 0, 1);
        } finally {
            assertUnchanged();
        }
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

    @Test public void testSetARange1() {
        testSetARange(5, 0, 0, 5);
    }

    @Test public void testSetARange2() {
        testSetARange(3, 2, 0, 3);
    }

    @Test public void testSetARange3() {
        testSetARange(5, 0, 2, 3);
    }

    @Test public void testSetARange4() {
        testSetARange(5, 0, 0, 3);
    }

    @Test public void testSetARange5() {
        testSetARange(10, 3, 5, 3);
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetARangeNegative1() {
        try {
            testSetARange(10, -1, 0, 3);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetARangeNegative2() {
        try {
            testSetARange(10, 0, -1, 3);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetARangeNegative3() {
        try {
            testSetARange(10, 1, 1, -1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetARangeNegative4() {
        try {
            testSetARange(10, INITIAL_SIZE, 0, 3);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetARangeNegative5() {
        try {
            testSetARange(10, 0, 10, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetARangeNegative6() {
        try {
            testSetARange(3, 0, 1, 4);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetARangeNegative7() {
        try {
            testSetARange(10, INITIAL_SIZE - 3, 0, 4);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetARangeNegativeAfterEnsureCapacity() {
        array.ensureCapacity(INITIAL_SIZE * 2);
        try {
            testSetARange(1, INITIAL_SIZE, 0, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetARangeNegativeAfterClear() {
        makeEmpty();
        try {
            testSetARange(1, 0, 0, 1);
        } finally {
            assertUnchanged();
        }
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

    @Test public void testSetTRange1() {
        testSetTRange(5, 0, 0, 5);
    }

    @Test public void testSetTRange2() {
        testSetTRange(3, 2, 0, 3);
    }

    @Test public void testSetTRange3() {
        testSetTRange(5, 0, 2, 3);
    }

    @Test public void testSetTRange4() {
        testSetTRange(5, 0, 0, 3);
    }

    @Test public void testSetTRange5() {
        testSetTRange(10, 3, 5, 3);
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegative1() {
        try {
            testSetTRange(10, -1, 0, 3);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegative2() {
        try {
            testSetTRange(10, 0, -1, 3);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegative3() {
        try {
            testSetTRange(10, 1, 1, -1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegative4() {
        try {
            testSetTRange(10, INITIAL_SIZE, 0, 3);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegative5() {
        try {
            testSetTRange(10, 0, 10, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegative6() {
        try {
            testSetTRange(3, 0, 1, 4);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegative7() {
        try {
            testSetTRange(10, INITIAL_SIZE - 3, 0, 4);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegativeAfterEnsureCapacity() {
        array.ensureCapacity(INITIAL_SIZE * 2);
        try {
            testSetTRange(1, INITIAL_SIZE, 0, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegativeAfterClear() {
        makeEmpty();
        try {
            testSetTRange(1, 0, 0, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegativeAfterSrcEnsureCapacity() {
        try {
            Object srcA = wrapper.createPrimitiveArray(1);
            ObservableArray src = wrapper.newInstance().createNotEmptyArray(srcA);
            src.ensureCapacity(2);

            wrapper.setT(0, src, 1, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeNegativeAfterSrcClear() {
        try {
            Object srcA = wrapper.createPrimitiveArray(1);
            ObservableArray src = wrapper.newInstance().createNotEmptyArray(srcA);
            src.clear();

            wrapper.setT(0, src, 0, 1);
        } finally {
            assertUnchanged();
        }
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

    @Test public void testSetTRangeSelf() {
        testSetTRangeSelf(0, 0, INITIAL_SIZE);
    }

    @Test public void testSetTRangeSelfLeft() {
        testSetTRangeSelf(0, 1, INITIAL_SIZE - 1);
    }

    @Test public void testSetTRangeSelfRight() {
        testSetTRangeSelf(1, 0, INITIAL_SIZE - 1);
    }

    @Test public void testSetTRangeSelfRightDifferentParts() {
        testSetTRangeSelf(0, INITIAL_SIZE / 2, INITIAL_SIZE / 2);
    }

    @Test public void testSetTRangeSelfLeftDifferentParts() {
        testSetTRangeSelf(INITIAL_SIZE / 2, 0, INITIAL_SIZE / 2);
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeSelfNegative1() {
        try {
            wrapper.setT(-1, array, 0, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeSelfNegative2() {
        try {
            wrapper.setT(0, array, -1, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeSelfNegative3() {
        try {
            wrapper.setT(0, array, 0, INITIAL_SIZE + 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeSelfNegative4() {
        try {
            wrapper.setT(0, array, 1, -1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeSelfNegative5() {
        try {
            wrapper.setT(INITIAL_SIZE, array, 0, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeSelfNegative6() {
        try {
            wrapper.setT(0, array, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeSelfNegativeAfterEnsureCapacity() {
        try {
            array.ensureCapacity(INITIAL_SIZE * 2);

            wrapper.setT(0, array, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetTRangeSelfNegativeAfterClear() {
        makeEmpty();
        try {
            wrapper.setT(0, array, 0, 1);
        } finally {
            assertUnchanged();
        }
    }


    // ========== negative get(index) tests ==========

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testGetNegative() {
        try {
            wrapper.get(-1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testGetOutOfBounds() {
        try {
            wrapper.get(array.size());
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testGetAfterEnsureCapacity() {
        try {
            array.ensureCapacity(INITIAL_SIZE * 2);
            wrapper.get(INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testGetAfterClear() {
        makeEmpty();
        try {
            wrapper.get(0);
        } finally {
            assertUnchanged();
        }
    }

    // ================== set(index) tests ===============

    @Test public void testSetValue() {
        for (int i = 0; i < INITIAL_SIZE; i++) {
            Object expected = wrapper.getNextValue();

            wrapper.set(i, expected);

            mao.check(array, false, i, i + 1);
            mao.reset();
            assertEquals(expected, wrapper.get(i));
            assertEquals(INITIAL_SIZE, array.size());
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetValueNegative() {
        try {
            wrapper.set(-1, wrapper.getNextValue());
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetValueOutOfBounds() {
        try {
            wrapper.set(INITIAL_SIZE, wrapper.getNextValue());
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetValueNegativeAfterClear() {
        makeEmpty();
        try {
            wrapper.set(0, wrapper.getNextValue());
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testSetValueNegativeAfterEnsureCapacity() {
        array.ensureCapacity(INITIAL_SIZE * 2);
        try {
            wrapper.set(INITIAL_SIZE, wrapper.getNextValue());
        } finally {
            assertUnchanged();
        }
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

    @Test public void testToArraySameSize() {
        testToArray(array.size(), true);
    }

    @Test public void testToArraySmaller() {
        testToArray(3, false);
    }

    @Test public void testToArrayBigger() {
        testToArray(10, true);
    }

    @Test public void testToArrayEmpty() {
        makeEmpty();
        testToArray(10, true);
    }

    @Test public void testToArrayEmptyToEmpty() {
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

    @Test public void testToArrayRange0() {
        testToArrayRange(0, array.size(), array.size());
    }

    @Test public void testToArrayRange1() {
        testToArrayRange(3, array.size(), array.size() - 3);
    }

    @Test public void testToArrayRange2() {
        testToArrayRange(0, array.size(), array.size() - 3);
    }

    @Test public void testToArrayRange3() {
        testToArrayRange(2, array.size(), 2);
    }

    @Test public void testToArrayRange4() {
        testToArrayRange(2, 0, 0);
    }

    @Test public void testToArrayRange5() {
        makeEmpty();
        testToArrayRange(0, 0, 0);
    }

    @Test public void testToArrayRange6() {
        testToArrayRange(3, 2, 2);
    }

    @Test public void testToArrayRange7() {
        testToArrayRange(5, 1, 1);
    }

    @Test public void testToArrayRange8() {
        testToArrayRange(0, array.size() * 2, array.size());
    }

    @Test public void testToArrayRange9() {
        testToArrayRange(0, array.size() - 1, array.size());
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testToArrayRangeNegative1() {
        try {
            testToArrayRange(-1, array.size(), 2);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testToArrayRangeNegative2() {
        try {
            testToArrayRange(array.size(), array.size(), 2);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testToArrayRangeNegative3() {
        try {
            testToArrayRange(5, array.size(), array.size() + 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testToArrayRangeNegative5() {
        makeEmpty();
        try {
            testToArrayRange(2, 0, 0);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testToArrayRangeNegativeAfterEnsureCapacity() {
        array.ensureCapacity(INITIAL_SIZE * 2);
        try {
            testToArrayRange(INITIAL_SIZE, 1, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testToArrayRangeNegativeAfterClear() {
        makeEmpty();
        try {
            testToArrayRange(0, 1, 1);
        } finally {
            assertUnchanged();
        }
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

    @Test public void testCopyToA0() {
        testCopyToA(0, array.size(), 0, array.size());
    }

    @Test public void testCopyToA1() {
        testCopyToA(1, array.size(), 2, 3);
    }

    @Test public void testCopyToA2() {
        testCopyToA(2, array.size(), 2, 2);
    }

    @Test public void testCopyToA3() {
        testCopyToA(0, array.size(), 2, 2);
    }

    @Test public void testCopyToA4() {
        testCopyToA(0, 3, 1, 2);
    }

    @Test public void testCopyToA5() {
        testCopyToA(0, array.size() * 3, array.size() * 2, array.size());
    }

    @Test public void testCopyToA6() {
        testCopyToA(3, array.size(), 0, array.size() - 3);
    }

    @Test public void testCopyToA7() {
        testCopyToA(0, 10, 7, 3);
    }

    @Test public void testCopyToA8() {
        testCopyToA(1, 0, 0, 0);
    }

    @Test public void testCopyToA9() {
        makeEmpty();
        testCopyToA(0, 0, 0, 0);
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToANegative1() {
        try {
            testCopyToA(-1, array.size(), 0, array.size());
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToANegative2() {
        try {
            testCopyToA(0, array.size() / 2, 0, array.size());
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToANegative3() {
        try {
            testCopyToA(array.size(), array.size(), 0, array.size());
        } finally {
            assertUnchanged();
        }

    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToANegative4() {
        try {
            testCopyToA(0, array.size(), -1, array.size());
        } finally {
            assertUnchanged();
        }

    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToANegative5() {
        try {
            testCopyToA(0, array.size(), array.size(), array.size());
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToANegative6() {
        try {
            testCopyToA(0, array.size(), 0, array.size() * 2);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToANegative7() {
        makeEmpty();
        try {
            testCopyToA(1, 0, 0, 0);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToANegative8() {
        try {
            testCopyToA(0, 0, 1, 0);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToANegativeAfterEnsureCapacity() {
        array.ensureCapacity(INITIAL_SIZE * 2);
        try {
            testCopyToA(INITIAL_SIZE, 1, 0, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToANegativeAfterClear() {
        makeEmpty();
        try {
            testCopyToA(0, 1, 0, 1);
        } finally {
            assertUnchanged();
        }
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

    @Test public void testCopyToT0() {
        testCopyToT(0, array.size(), 0, array.size());
    }

    @Test public void testCopyToT1() {
        testCopyToT(1, array.size(), 2, 3);
    }

    @Test public void testCopyToT2() {
        testCopyToT(2, array.size(), 2, 2);
    }

    @Test public void testCopyToT3() {
        testCopyToT(0, array.size(), 2, 2);
    }

    @Test public void testCopyToT4() {
        testCopyToT(0, 3, 1, 2);
    }

    @Test public void testCopyToT5() {
        testCopyToT(0, array.size() * 3, array.size() * 2, array.size());
    }

    @Test public void testCopyToT6() {
        testCopyToT(3, array.size(), 0, array.size() - 3);
    }

    @Test public void testCopyToT7() {
        testCopyToT(0, 10, 7, 3);
    }

    @Test public void testCopyToT8() {
        testCopyToT(1, 0, 0, 0);
    }

    @Test public void testCopyToT9() {
        makeEmpty();
        testCopyToT(0, 0, 0, 0);
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegative1() {
        try {
            testCopyToT(-1, array.size(), 0, array.size());
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegative2() {
        try {
            testCopyToT(0, array.size() / 2, 0, array.size());
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegative3() {
        try {
            testCopyToT(array.size(), array.size(), 0, array.size());
        } finally {
            assertUnchanged();
        }

    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegative4() {
        try {
            testCopyToT(0, array.size(), -1, array.size());
        } finally {
            assertUnchanged();
        }

    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegative5() {
        try {
            testCopyToT(0, array.size(), array.size(), array.size());
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegative6() {
        try {
            testCopyToT(0, array.size(), 0, array.size() * 2);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegative7() {
        makeEmpty();
        try {
            testCopyToT(1, 0, 0, 0);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegative8() {
        try {
            testCopyToT(0, 0, 1, 0);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegativeAfterEnsureCapacity() {
        array.ensureCapacity(INITIAL_SIZE * 2);
        try {
            testCopyToT(INITIAL_SIZE, 1, 0, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegativeAfterClear() {
        makeEmpty();
        try {
            testCopyToT(0, 1, 0, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegativeAfterDestEnsureCapacity() {
        try {
            ArrayWrapper wrapper2 = wrapper.newInstance();
            Object destA = wrapper2.createPrimitiveArray(1);
            ObservableArray dest = wrapper2.createNotEmptyArray(destA);
            dest.ensureCapacity(2);

            wrapper.copyToT(0, dest, 1, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTNegativeAfterDestClear() {
        try {
            ArrayWrapper wrapper2 = wrapper.newInstance();
            Object destA = wrapper2.createPrimitiveArray(1);
            ObservableArray dest = wrapper2.createNotEmptyArray(destA);
            dest.clear();

            wrapper.copyToT(0, dest, 0, 1);
        } finally {
            assertUnchanged();
        }
    }

    private void testCopyToTSelf(int srcIndex, int destIndex, int length) {

        wrapper.copyToT(srcIndex, array, destIndex, length);

        mao.checkOnlyElementsChanged(array, destIndex, destIndex + length);
        Object actual = wrapper.toArray(null);
        wrapper.assertElementsEqual(actual, 0, destIndex, initialElements, 0);
        wrapper.assertElementsEqual(actual, destIndex, destIndex + length, initialElements, srcIndex);
        wrapper.assertElementsEqual(actual, destIndex + length, initialSize, initialElements, destIndex + length);
    }

    @Test public void testCopyToTSelf() {
        testCopyToTSelf(0, 0, INITIAL_SIZE);
    }

    @Test public void testCopyToTSelfRight() {
        testCopyToTSelf(0, 1, INITIAL_SIZE - 1);
    }

    @Test public void testCopyToTSelfLeft() {
        testCopyToTSelf(1, 0, INITIAL_SIZE - 1);
    }

    @Test public void testCopyToTSelfRightDifferentParts() {
        testCopyToTSelf(0, INITIAL_SIZE / 2, INITIAL_SIZE / 2);
    }

    @Test public void testCopyToTSelfLeftDifferentParts() {
        testCopyToTSelf(INITIAL_SIZE / 2, 0, INITIAL_SIZE / 2);
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTSelfNegative1() {
        try {
            testCopyToTSelf(-1, 0, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTSelfNegative2() {
        try {
            testCopyToTSelf(INITIAL_SIZE, 0, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTSelfNegative3() {
        try {
            testCopyToTSelf(0, -1, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTSelfNegative4() {
        try {
            testCopyToTSelf(0, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTSelfNegative5() {
        try {
            testCopyToTSelf(1, 1, -1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTSelfNegative6() {
        try {
            testCopyToTSelf(0, 1, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTSelfNegative7() {
        try {
            testCopyToTSelf(1, 0, INITIAL_SIZE);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTSelfNegativeAfterEnsureCapacity() {
        array.ensureCapacity(INITIAL_SIZE * 2);
        try {
            testCopyToTSelf(0, INITIAL_SIZE, 1);
        } finally {
            assertUnchanged();
        }
    }

    @Test (expected = ArrayIndexOutOfBoundsException.class)
    public void testCopyToTSelfNegativeAfterClear() {
        makeEmpty();
        try {
            testCopyToTSelf(0, 0, 1);
        } finally {
            assertUnchanged();
        }
    }

    // ============ ensureCapacity() and trimToSize() tests ====================

    @Test public void testTrimToSize() {
        array.trimToSize();
        assertUnchanged();
    }

    @Test public void testTrimToSizeEmpty() {
        makeEmpty();
        array.trimToSize();
        assertUnchanged();
    }

    @Test public void testTrimToSizeResize() {
        array.resize(3);
        initialSize = 3;
        mao.reset();

        array.trimToSize();

        assertUnchanged();
    }

    @Test public void testTrimToSizeAddRemove() {
        array.resize(1000);
        array.resize(INITIAL_SIZE);
        mao.reset();

        array.trimToSize();

        assertUnchanged();
    }

    @Test public void testEnsureCapacity0() {
        array.ensureCapacity(0);
        assertUnchanged();
    }

    @Test public void testEnsureCapacityBy1() {
        array.ensureCapacity(INITIAL_SIZE + 1);
        assertUnchanged();
    }

    @Test public void testEnsureCapacity1000() {
        array.ensureCapacity(1000);
        assertUnchanged();
    }

    @Test public void testEnsureCapacitySmaller() {
        array.ensureCapacity(INITIAL_SIZE / 2);
        assertUnchanged();
    }

    @Test public void testEnsureCapacityNegative() {
        array.ensureCapacity(-1000);
        assertUnchanged();
    }

    @Test public void testEnsureCapacityOnEmpty() {
        makeEmpty();
        array.ensureCapacity(100);
        assertUnchanged();
    }

    @Test public void testEnsureCapacityOnEmpty0() {
        makeEmpty();
        array.ensureCapacity(0);
        assertUnchanged();
    }

    @Test public void testEnsureCapacityOnEmptyNegative() {
        makeEmpty();
        array.ensureCapacity(-1);
        assertUnchanged();
    }

    @Test public void testTrimToSizeEnsureCapacity() {
        array.ensureCapacity(1000);
        array.trimToSize();
        assertUnchanged();
    }

    // ================= clear() tests ====================

    @Test public void testClearEmpty() {
        makeEmpty();
        array.clear();
        mao.check0();
        assertEquals(0, array.size());
    }

    @Test public void testClear1000() {
        array.resize(1000);
        mao.reset();

        array.clear();

        mao.checkOnlySizeChanged(array);
        assertEquals(0, array.size());
    }

    // ================= toString() tests ===================

    @Test public void testToString() {
        String actual = array.toString();
        String expected = wrapper.primitiveArrayToString(wrapper.toArray(null));
        assertEquals(expected, actual);
        String regex = "\\[[0-9]+(\\.[0-9]+){0,1}(\\, [0-9]+(.[0-9]+){0,1}){" + (initialSize - 1) + "}\\]";
        assertTrue("toString() output matches to regex '" + regex + "'. Actual = '" + actual + "'",
                actual.matches(regex));
    }

    @Test public void testToStringAfterResize() {
        array.resize(initialSize / 2);
        String actual = array.toString();
        String expected = wrapper.primitiveArrayToString(wrapper.toArray(null));
        assertEquals(expected, actual);
        String regex = "\\[[0-9]+(\\.[0-9]+){0,1}(\\, [0-9]+(.[0-9]+){0,1}){" + (array.size() - 1) + "}\\]";
        assertTrue("toString() output matches to regex '" + regex + "'. Actual = '" + actual + "'",
                actual.matches(regex));
    }

    @Test public void testToStringAfterClear() {
        array.clear();
        String actual = array.toString();
        assertEquals("[]", actual);
    }
}
