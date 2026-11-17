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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;


/**
 * Tests for iterators of ObservableList.
 *
 */
public class ObservableListIteratorTest {

    // ========== Test Fixture ==========
    protected Callable<? extends List<String>> listFactory;
    List<String> list;
    ListIterator<String> iter;


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
        list = listFactory.call();
        list.addAll(Arrays.asList("a", "b", "c", "d", "e", "f"));
        iter = list.listIterator();
    }

    // ========== Utility Functions ==========

    List<String> copyOut(Iterator<String> itr) {
        List<String> out = new ArrayList<>();
        while (itr.hasNext()) {
            out.add(itr.next());
        }
        return out;
    }

    void advance(Iterator<String> itr, int count) {
        for (int i = 0; i < count; i += 1) {
            itr.next();
        }
    }

    void toEnd(Iterator<String> itr) {
        while (itr.hasNext()) {
            itr.next();
        }
    }

    void rewind(ListIterator<String> itr) {
        while (itr.hasPrevious()) {
            itr.previous();
        }
    }

    List<String> contents(ListIterator<String> itr) {
        rewind(itr);
        return copyOut(itr);
    }

    // ========== Basic Tests ==========

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testCompleteIteration(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        assertEquals("[a, b, c, d, e, f]", copyOut(iter).toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testBeginningState(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        assertTrue(iter.hasNext());
        assertFalse(iter.hasPrevious());
        assertEquals(-1, iter.previousIndex());
        assertEquals(0, iter.nextIndex());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testMiddleState(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        assertTrue(iter.hasNext());
        assertTrue(iter.hasPrevious());
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testEndState(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        toEnd(iter);
        assertFalse(iter.hasNext());
        assertTrue(iter.hasPrevious());
        assertEquals(5, iter.previousIndex());
        assertEquals(6, iter.nextIndex());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSwitchDirections(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 2);
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("d", iter.previous());
        assertEquals("c", iter.previous());
        assertEquals("c", iter.next());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testBoundaryStart(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        assertThrows(NoSuchElementException.class, () -> iter.previous());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testBoundaryEnd(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 6);
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testForEachLoop(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        List<String> output = new ArrayList<>();
        for (String s : list) {
            output.add(s);
        }
        assertEquals(list.toString(), output.toString());
    }

    // ========== Add Tests ==========

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddBeginning(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.add("X");

        assertEquals(0, iter.previousIndex());
        assertEquals(1, iter.nextIndex());
        assertEquals("a", iter.next());
        assertEquals("[X, a, b, c, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddMiddle(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.add("X");

        assertEquals(3, iter.previousIndex());
        assertEquals(4, iter.nextIndex());
        assertEquals("d", iter.next());
        assertEquals("[a, b, c, X, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddEnd(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 6);
        iter.add("X");

        assertEquals(6, iter.previousIndex());
        assertEquals(7, iter.nextIndex());
        assertFalse(iter.hasNext());
        assertEquals("[a, b, c, d, e, f, X]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testPreviousAfterAddBeginning(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.add("X");
        assertEquals("X", iter.previous());
        assertEquals(-1, iter.previousIndex());
        assertEquals(0, iter.nextIndex());
        assertEquals("[X, a, b, c, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testPreviousAfterAddMiddle(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.add("X");
        assertEquals("X", iter.previous());
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("[a, b, c, X, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testPreviousAfterAddEnd(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 6);
        iter.add("X");
        assertEquals("X", iter.previous());
        assertEquals(5, iter.previousIndex());
        assertEquals(6, iter.nextIndex());
        assertEquals("[a, b, c, d, e, f, X]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddMultiple(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.add("X");
        iter.add("Y");
        iter.add("Z");
        assertEquals(5, iter.previousIndex());
        assertEquals(6, iter.nextIndex());
        assertEquals("d", iter.next());
        assertEquals("d", iter.previous());
        assertEquals("Z", iter.previous());
        assertEquals("[a, b, c, X, Y, Z, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAfterPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.previous(); // c
        iter.add("X");
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("[a, b, X, c, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAfterRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.remove();
        iter.add("X");
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("[a, b, X, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testAddAfterSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.set("X");
        iter.add("Y");
        assertEquals(3, iter.previousIndex());
        assertEquals(4, iter.nextIndex());
        assertEquals("[a, b, X, Y, d, e, f]", list.toString());
    }

    // ========== Remove Tests ==========

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveBeginning(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.next();
        iter.remove();
        assertEquals(-1, iter.previousIndex());
        assertEquals(0, iter.nextIndex());
        assertEquals("[b, c, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveMiddle(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.remove();
        assertEquals(1, iter.previousIndex());
        assertEquals(2, iter.nextIndex());
        assertEquals("[a, b, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveEnd(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        toEnd(iter);
        iter.remove();
        assertEquals(4, iter.previousIndex());
        assertEquals(5, iter.nextIndex());
        assertEquals("[a, b, c, d, e]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveAfterPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 4);
        iter.previous(); // d
        iter.previous(); // c
        iter.remove();
        assertEquals(1, iter.previousIndex());
        assertEquals(2, iter.nextIndex());
        assertEquals("[a, b, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveAfterSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.set("X");
        iter.remove();
        assertEquals(1, iter.previousIndex());
        assertEquals(2, iter.nextIndex());
        assertEquals("[a, b, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveInitialThrowsException(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        assertThrows(IllegalStateException.class, () -> iter.remove());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveTwiceThrowsException(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.next();
        iter.remove();
        try { iter.remove(); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testRemoveAfterAddThrowsException(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.next();
        iter.add("X");
        try { iter.remove(); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    // ========== Set Tests ==========

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetBeginning(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.next();
        iter.set("X");
        assertEquals(0, iter.previousIndex());
        assertEquals(1, iter.nextIndex());
        assertEquals("[X, b, c, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetMiddle(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.set("X");
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("[a, b, X, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetEnd(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        toEnd(iter);
        iter.set("X");
        assertEquals(5, iter.previousIndex());
        assertEquals(6, iter.nextIndex());
        assertEquals("[a, b, c, d, e, X]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetTwice(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 3);
        iter.set("X");
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("[a, b, X, d, e, f]", list.toString());
        iter.set("Y");
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("[a, b, Y, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAfterPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        advance(iter, 4);
        iter.previous(); // d
        iter.previous(); // c
        iter.set("X");
        assertEquals(1, iter.previousIndex());
        assertEquals(2, iter.nextIndex());
        assertEquals("[a, b, X, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetInitialThrowsException(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        try { iter.set("X"); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAfterAddThrowsException(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.next();
        iter.add("X");
        try { iter.set("Y"); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testSetAfterRemoveThrowsException(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter.next();
        iter.remove();
        try { iter.set("X"); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    // ========== Positioned Iterator Tests ==========

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testPosBeginning(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(0);
        assertFalse(iter.hasPrevious());
        assertTrue(iter.hasNext());
        assertEquals(-1, iter.previousIndex());
        assertEquals(0, iter.nextIndex());
        assertEquals("a", iter.next());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testPosMiddle(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(3);
        assertTrue(iter.hasPrevious());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("d", iter.next());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testPosEnd(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(6);
        assertTrue(iter.hasPrevious());
        assertFalse(iter.hasNext());
        assertEquals(5, iter.previousIndex());
        assertEquals(6, iter.nextIndex());
        assertEquals("f", iter.previous());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testPosAdd(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(3);
        iter.add("X");
        assertEquals(3, iter.previousIndex());
        assertEquals(4, iter.nextIndex());
        assertEquals("[a, b, c, X, d, e, f]", list.toString());
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testPosInitialRemoveThrowsException(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(3);
        try { iter.remove(); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testPosInitialSetThrowsException(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(3);
        try { iter.set("X"); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    // ========== Concurrency Tests ==========

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddIteratorNext(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        list.add("aa");
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddIndexedIteratorNext(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        list.add(1, "aa");
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveIteratorNext(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        list.remove("b");
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveIndexedIteratorNext(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        list.remove(2);
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddAllIteratorNext(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        list.addAll(Collections.singleton("f"));
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddAllIndexedIteratorNext(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        list.addAll(1, Collections.singleton("f"));
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyGetIteratorNext(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        list.get(2);
        iter.next();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveAllIteratorNext(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        list.removeAll(Arrays.asList("a", "c"));
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRetainAllIteratorNext(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        list.retainAll(Arrays.asList("a", "c"));
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyIteratorIteratorNext(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        final Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddIteratorPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(2);
        list.add("aa");
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddIndexedIteratorPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(2);
        list.add(1, "aa");
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveIteratorPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(2);
        list.remove("b");
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveIndexedIteratorPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(2);
        list.remove(2);
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddAllIteratorPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(2);
        list.addAll(Collections.singleton("f"));
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddAllIndexedIteratorPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(2);
        list.addAll(1, Collections.singleton("f"));
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyGetIteratorPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(2);
        list.get(2);
        iter.previous();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveAllIteratorPrevious(Callable<? extends List<String>> listFactory) throws Exception  {
        setup(listFactory);
        iter = list.listIterator(2);
        list.removeAll(Arrays.asList("a", "c"));
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRetainAllIteratorPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(2);
        list.retainAll(Arrays.asList("a", "c"));
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyIteratorIteratorPrevious(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator(2);
        final Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddIteratorRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.add("aa");
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddIndexedIteratorRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.add(1, "aa");
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveIteratorRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.remove("b");
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveIndexedIteratorRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.remove(2);
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddAllIteratorRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.addAll(Collections.singleton("f"));
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddAllIndexedIteratorRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.addAll(1, Collections.singleton("f"));
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyGetIteratorRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.get(2);
        iter.remove();
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveAllIteratorRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.removeAll(Arrays.asList("a", "c"));
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRetainAllIteratorRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.retainAll(Arrays.asList("a", "c"));
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyIteratorIteratorRemove(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        final Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddIteratorSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.add("aa");
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddIndexedIteratorSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.add(1, "aa");
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveIteratorSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.remove("b");
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveIndexedIteratorSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.remove(2);
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddAllIteratorSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.addAll(Collections.singleton("f"));
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyAddAllIndexedIteratorSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.addAll(1, Collections.singleton("f"));
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyGetIteratorSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.get(2);
        iter.set("x");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRemoveAllIteratorSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.removeAll(Arrays.asList("a", "c"));
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyRetainAllIteratorSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        list.retainAll(Arrays.asList("a", "c"));
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @ParameterizedTest
    @MethodSource("createParameters")
    public void testConcurrencyIteratorIteratorSet(Callable<? extends List<String>> listFactory) throws Exception {
        setup(listFactory);
        iter = list.listIterator();
        iter.next();
        final Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
}
