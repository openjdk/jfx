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
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;


/**
 * Tests for iterators of sublists of ObservableList.
 * Note that this is a subclass of ObservableListIteratorTest.
 * As such, it inherits all the tests from that class, but they
 * are run using the sublist-based fixture. There are also some
 * additional tests that make assertions about the underlying list
 * after mutating the sublist via the iterator.
 *
 */

public class ObservableSubListIteratorTest extends ObservableListIteratorTest {

    // ========== Test Fixture ==========

    List<String> fullList;

    public static Stream<Arguments> createParameters() {
        return Stream.of(
                Arguments.of(TestedObservableLists.ARRAY_LIST),
                Arguments.of(TestedObservableLists.LINKED_LIST),
                Arguments.of(TestedObservableLists.VETOABLE_LIST),
                Arguments.of(TestedObservableLists.CHECKED_OBSERVABLE_ARRAY_LIST),
                Arguments.of(TestedObservableLists.SYNCHRONIZED_OBSERVABLE_ARRAY_LIST)
        );
    }

    private void setup(Callable<? extends List<String>> listFactory) throws Exception {
        this.listFactory = listFactory;
        this.list = listFactory.call();
        this.list.addAll(Arrays.asList("P", "Q", "a", "b", "c", "d", "e", "f", "R", "S"));
        this.fullList = list;
        this.list = fullList.subList(2, 8);
        this.iter = list.listIterator();
    }

    // ========== Sublist Iterator Tests ==========

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubAddBeginning(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.add("X");
        assertEquals("[P, Q, X, a, b, c, d, e, f, R, S]", fullList.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubAddMiddle(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.add("X");
        assertEquals("[P, Q, a, b, c, X, d, e, f, R, S]", fullList.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubAddEnd(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        toEnd(iter);
        iter.add("X");
        assertEquals("[P, Q, a, b, c, d, e, f, X, R, S]", fullList.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubRemoveBeginning(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.next();
        iter.remove();
        assertEquals("[P, Q, b, c, d, e, f, R, S]", fullList.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubRemoveMiddle(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.remove();
        assertEquals("[P, Q, a, b, d, e, f, R, S]", fullList.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubRemoveEnd(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        toEnd(iter);
        iter.remove();
        assertEquals("[P, Q, a, b, c, d, e, R, S]", fullList.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubSetBeginning(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.next();
        iter.set("X");
        assertEquals("[P, Q, X, b, c, d, e, f, R, S]", fullList.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubSetMiddle(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.set("X");
        assertEquals("[P, Q, a, b, X, d, e, f, R, S]", fullList.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSubSetEnd(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        toEnd(iter);
        iter.set("X");
        assertEquals("[P, Q, a, b, c, d, e, X, R, S]", fullList.toString());
    }
}
