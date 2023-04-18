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

package test.com.sun.javafx.collections;

import com.sun.javafx.collections.ConcatenatedObservableList;
import com.sun.javafx.collections.ObservableListWrapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListWrapperShim;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConcatenatedObservableListTest {

    private static class TestData {
        final ObservableListWrapper<String> list1 =
                (ObservableListWrapper<String>)FXCollections.observableArrayList("A", "B");

        final ObservableListWrapper<String> list2 =
                (ObservableListWrapper<String>)FXCollections.observableArrayList("C");

        final ObservableListWrapper<String> list3 =
                (ObservableListWrapper<String>)FXCollections.<String>observableArrayList();

        final ObservableListWrapper<String> list4 =
                (ObservableListWrapper<String>)FXCollections.observableArrayList("D", "E", "F");

        @SuppressWarnings("unchecked")
        final ObservableList<String> concatList = new ConcatenatedObservableList<String>(
                new ObservableList[] { list1, list2, list3, list4 });

        final List<String> trace = new ArrayList<>();

        TestData() {
            concatList.addListener((ListChangeListener<? super String>) change -> {
                while (change.next()) {
                    trace.add(change.toString());
                }
            });
        }

        void assertListEquals(String... expected) {
            assertEquals(List.of(expected), concatList);
            assertEquals(concatList.size(), list1.size() + list2.size() + list3.size() + list4.size());
        }

        void assertTraceEquals(String... trace) {
            assertEquals(List.of(trace), this.trace);
        }
    }

    private static Object[] getTestData() {
        return new Object[] { new TestData() };
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testListIsUnmodifiable(TestData testData) {
        assertThrows(UnsupportedOperationException.class, () -> testData.concatList.set(0, "foo"));
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testIndexOutOfBounds(TestData testData) {
        assertThrows(IndexOutOfBoundsException.class, () -> testData.concatList.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> testData.concatList.get(testData.concatList.size()));
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testConcat(TestData testData) {
        testData.assertListEquals("A", "B", "C", "D", "E", "F");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testAddFront(TestData testData) {
        testData.list1.add(0, "1");
        testData.list1.addAll("2", "3", "4");
        testData.assertListEquals("1", "A", "B", "2", "3", "4", "C", "D", "E", "F");
        testData.assertTraceEquals(
                "{ [1] added at 0 }",
                "{ [2, 3, 4] added at 3 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testAddMiddle(TestData testData) {
        testData.list2.add(0, "1");
        testData.list3.addAll("2", "3", "4");
        testData.assertListEquals("A", "B", "1", "C", "2", "3", "4", "D", "E", "F");
        testData.assertTraceEquals(
                "{ [1] added at 2 }",
                "{ [2, 3, 4] added at 4 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testAddBack(TestData testData) {
        testData.list4.add(0, "1");
        testData.list4.addAll("2", "3", "4");
        testData.assertListEquals("A", "B", "C", "1", "D", "E", "F", "2", "3", "4");
        testData.assertTraceEquals(
                "{ [1] added at 3 }",
                "{ [2, 3, 4] added at 7 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testRemoveFront(TestData testData) {
        testData.list1.remove(0);
        testData.assertListEquals("B", "C", "D", "E", "F");
        testData.list1.clear();
        testData.assertListEquals("C", "D", "E", "F");
        testData.assertTraceEquals(
                "{ [A] removed at 0 }",
                "{ [B] removed at 0 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testRemoveMiddle(TestData testData) {
        testData.list2.remove(0);
        testData.assertListEquals("A", "B", "D", "E", "F");
        testData.assertTraceEquals("{ [C] removed at 2 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testRemoveBack(TestData testData) {
        testData.list4.remove(0, 2);
        testData.assertListEquals("A", "B", "C", "F");
        testData.assertTraceEquals("{ [D, E] removed at 3 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testReplaceFront(TestData testData) {
        testData.list1.set(0, "1");
        testData.assertListEquals("1", "B", "C", "D", "E", "F");
        testData.assertTraceEquals("{ [A] replaced by [1] at 0 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testReplaceMiddle(TestData testData) {
        testData.list2.set(0, "1");
        testData.assertListEquals("A", "B", "1", "D", "E", "F");
        testData.assertTraceEquals("{ [C] replaced by [1] at 2 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testReplaceBack(TestData testData) {
        testData.list4.set(1, "1");
        testData.assertListEquals("A", "B", "C", "D", "1", "F");
        testData.assertTraceEquals("{ [E] replaced by [1] at 4 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testReplaceMultipleFront(TestData testData) {
        testData.list1.setAll("1", "2", "3");
        testData.assertListEquals("1", "2", "3", "C", "D", "E", "F");
        testData.assertTraceEquals("{ [A, B] replaced by [1, 2, 3] at 0 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testReplaceMultipleMiddle(TestData testData) {
        testData.list2.setAll("1", "2");
        testData.assertListEquals("A", "B", "1", "2", "D", "E", "F");
        testData.assertTraceEquals("{ [C] replaced by [1, 2] at 2 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testReplaceMultipleBack(TestData testData) {
        testData.list4.setAll("1", "2");
        testData.assertListEquals("A", "B", "C", "1", "2");
        testData.assertTraceEquals("{ [D, E, F] replaced by [1, 2] at 3 }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testPermuteFront(TestData testData) {
        testData.list1.addAll("2", "3", "1");
        testData.assertListEquals("A", "B", "2", "3", "1", "C", "D", "E", "F");
        testData.list1.sort();
        testData.assertListEquals("1", "2", "3", "A", "B", "C", "D", "E", "F");
        testData.assertTraceEquals(
                "{ [2, 3, 1] added at 2 }",
                "{ permutated by [3, 4, 1, 2, 0] }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testPermuteMiddle(TestData testData) {
        testData.list2.addAll("C2", "C3", "C1");
        testData.assertListEquals("A", "B", "C", "C2", "C3", "C1", "D", "E", "F");
        testData.list2.sort();
        testData.assertListEquals("A", "B", "C", "C1", "C2", "C3", "D", "E", "F");
        testData.assertTraceEquals(
                "{ [C2, C3, C1] added at 3 }",
                "{ permutated by [2, 4, 5, 3] }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testPermuteBack(TestData testData) {
        testData.list4.addAll("2", "3", "1");
        testData.assertListEquals("A", "B", "C", "D", "E", "F", "2", "3", "1");
        testData.list4.sort();
        testData.assertListEquals("A", "B", "C", "1", "2", "3", "D", "E", "F");
        testData.assertTraceEquals(
                "{ [2, 3, 1] added at 6 }",
                "{ permutated by [6, 7, 8, 4, 5, 3] }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testUpdateFront(TestData testData) {
        ObservableListWrapperShim.beginChange(testData.list1);
        ObservableListWrapperShim.nextUpdate(testData.list1, 0);
        ObservableListWrapperShim.nextUpdate(testData.list1, 1);
        ObservableListWrapperShim.endChange(testData.list1);
        testData.assertTraceEquals("{ updated at range [0, 2) }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testUpdateMiddle(TestData testData) {
        ObservableListWrapperShim.beginChange(testData.list2);
        ObservableListWrapperShim.nextUpdate(testData.list2, 0);
        ObservableListWrapperShim.endChange(testData.list2);
        testData.assertTraceEquals("{ updated at range [2, 3) }");
    }

    @ParameterizedTest
    @MethodSource("getTestData")
    void testUpdateBack(TestData testData) {
        ObservableListWrapperShim.beginChange(testData.list4);
        ObservableListWrapperShim.nextUpdate(testData.list4, 1);
        ObservableListWrapperShim.nextUpdate(testData.list4, 2);
        ObservableListWrapperShim.endChange(testData.list4);
        testData.assertTraceEquals("{ updated at range [4, 6) }");
    }

}
