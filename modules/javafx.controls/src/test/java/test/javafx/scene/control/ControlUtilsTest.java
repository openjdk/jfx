/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import com.sun.javafx.scene.control.ReadOnlyUnbackedObservableList;
import javafx.collections.ObservableList;
import javafx.scene.control.ControlUtilsShim;
import org.junit.Test;
import test.javafx.collections.MockListObserver;
import java.util.List;

public class ControlUtilsTest {

    @Test
    public void reducingChange_removeFirstIndexInSingleChange() {
        TestResult result = reducingChangeTest(List.of(1, 2, 3, 4, 5), List.of(1));
        result.observer.check1();
        result.observer.checkAddRemove(0, result.list, List.of(1), 0, 0);
    }

    @Test
    public void reducingChange_removeMiddleIndexInSingleChange() {
        TestResult result = reducingChangeTest(List.of(1, 2, 3, 4, 5), List.of(3));
        result.observer.check1();
        result.observer.checkAddRemove(0, result.list, List.of(3), 2, 2);
    }

    @Test
    public void reducingChange_removeLastIndexInSingleChange() {
        TestResult result = reducingChangeTest(List.of(1, 2, 3, 4, 5), List.of(5));
        result.observer.check1();
        result.observer.checkAddRemove(0, result.list, List.of(5), 4, 4);
    }

    @Test
    public void reducingChange_removeAllIndicesInSingleChange() {
        TestResult result = reducingChangeTest(List.of(1, 2, 3, 4, 5), List.of(1, 2, 3, 4, 5));
        result.observer.check1();
        result.observer.checkAddRemove(0, result.list, List.of(1, 2, 3, 4, 5), 0, 0);
    }

    @Test
    public void reducingChange_removeInteriorIndicesInSingleChange() {
        TestResult result = reducingChangeTest(List.of(1, 2, 3, 4, 5), List.of(2, 3, 4));
        result.observer.check1();
        result.observer.checkAddRemove(0, result.list, List.of(2, 3, 4), 1, 1);
    }

    @Test
    public void reducingChange_removeFirstAndLastIndexInTwoChanges() {
        TestResult result = reducingChangeTest(List.of(1, 2, 3, 4, 5), List.of(1, 5));
        result.observer.checkN(2);
        result.observer.checkAddRemove(0, result.list, List.of(1), 0, 0);
        result.observer.checkAddRemove(1, result.list, List.of(5), 3, 3);
    }

    @Test
    public void reducingChange_removeDisjointIndicesInTwoChanges() {
        TestResult result = reducingChangeTest(List.of(1, 2, 3, 4, 5, 6), List.of(1, 2, 4, 5, 6));
        result.observer.checkN(2);
        result.observer.checkAddRemove(0, result.list, List.of(1, 2), 0, 0);
        result.observer.checkAddRemove(1, result.list, List.of(4, 5, 6), 1, 1);
    }

    @Test
    public void reducingChange_removeDisjointIndicesInThreeChanges() {
        TestResult result = reducingChangeTest(List.of(1, 2, 3, 4, 5, 6, 7, 8), List.of(1, 2, 4, 5, 7, 8));
        result.observer.checkN(3);
        result.observer.checkAddRemove(0, result.list, List.of(1, 2), 0, 0);
        result.observer.checkAddRemove(1, result.list, List.of(4, 5), 1, 1);
        result.observer.checkAddRemove(2, result.list, List.of(7, 8), 2, 2);
    }

    private TestResult reducingChangeTest(List<Integer> indices, List<Integer> removed) {
        ReadOnlyUnbackedObservableList<Integer> list = new ReadOnlyUnbackedObservableList<>() {
            @Override
            public Integer get(int i) {
                return indices.get(i);
            }

            @Override
            public int size() {
                return indices.size();
            }
        };

        MockListObserver<Integer> observer = new MockListObserver<>();
        list.addListener(observer);
        list._beginChange();
        ControlUtilsShim.reducingChange(list, removed);
        list._endChange();

        return new TestResult(list, observer);
    }

    private static class TestResult {
        final ObservableList<Integer> list;
        final MockListObserver<Integer> observer;
        TestResult(ObservableList<Integer> list, MockListObserver<Integer> observer) {
            this.list = list;
            this.observer = observer;
        }
    }

}
