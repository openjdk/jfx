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

import java.util.*;

import static org.junit.Assert.*;


/**
 * Tests for iterators of ObservableList.
 * 
 */
public abstract class ObservableListIteratorTestBase {

    // ========== Test Fixture ==========
    final Callable<? extends List<String>> listFactory;
    List<String> list;
    ListIterator<String> iter;

    public ObservableListIteratorTestBase(final Callable<? extends List<String>> listFactory) {
        this.listFactory = listFactory;
    }

    @Before
    public void setup() throws Exception {
        list = listFactory.call();
        list.addAll(Arrays.asList("a", "b", "c", "d", "e", "f"));
        iter = list.listIterator();
    }

    // ========== Utility Functions ==========

    List<String> copyOut(Iterator<String> itr) {
        List<String> out = new ArrayList<String>();
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

    @Test
    public void testCompleteIteration() {
        assertEquals("[a, b, c, d, e, f]", copyOut(iter).toString());
    }

    @Test
    public void testBeginningState() {
        assertTrue(iter.hasNext());
        assertFalse(iter.hasPrevious());
        assertEquals(-1, iter.previousIndex());
        assertEquals(0, iter.nextIndex());
    }

    @Test
    public void testMiddleState() {
        advance(iter, 3);
        assertTrue(iter.hasNext());
        assertTrue(iter.hasPrevious());
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
    }

    @Test
    public void testEndState() {
        toEnd(iter);
        assertFalse(iter.hasNext());
        assertTrue(iter.hasPrevious());
        assertEquals(5, iter.previousIndex());
        assertEquals(6, iter.nextIndex());
    }

    @Test
    public void testSwitchDirections() {
        advance(iter, 2);
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("d", iter.previous());
        assertEquals("c", iter.previous());
        assertEquals("c", iter.next());
    }

    @Test(expected = NoSuchElementException.class)
    public void testBoundaryStart() {
        iter.previous();
    }

    @Test(expected = NoSuchElementException.class)
    public void testBoundaryEnd() {
        advance(iter, 6);
        iter.next();
    }

    @Test
    public void testForEachLoop() {
        List<String> output = new ArrayList<String>();
        for (String s : list) {
            output.add(s);
        }
        assertEquals(list, output);
    }

    // ========== Add Tests ==========

    @Test
    public void testAddBeginning() {
        iter.add("X");

        assertEquals(0, iter.previousIndex());
        assertEquals(1, iter.nextIndex());
        assertEquals("a", iter.next());
        assertEquals("[X, a, b, c, d, e, f]", list.toString());
    }

    @Test
    public void testAddMiddle() {
        advance(iter, 3);
        iter.add("X");

        assertEquals(3, iter.previousIndex());
        assertEquals(4, iter.nextIndex());
        assertEquals("d", iter.next());
        assertEquals("[a, b, c, X, d, e, f]", list.toString());
    }

    @Test
    public void testAddEnd() {
        advance(iter, 6);
        iter.add("X");

        assertEquals(6, iter.previousIndex());
        assertEquals(7, iter.nextIndex());
        assertFalse(iter.hasNext());
        assertEquals("[a, b, c, d, e, f, X]", list.toString());
    }

    @Test
    public void testPreviousAfterAddBeginning() {
        iter.add("X");
        assertEquals("X", iter.previous());
        assertEquals(-1, iter.previousIndex());
        assertEquals(0, iter.nextIndex());
        assertEquals("[X, a, b, c, d, e, f]", list.toString());
    }

    @Test
    public void testPreviousAfterAddMiddle() {
        advance(iter, 3);
        iter.add("X");
        assertEquals("X", iter.previous());
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("[a, b, c, X, d, e, f]", list.toString());
    }

    @Test
    public void testPreviousAfterAddEnd() {
        advance(iter, 6);
        iter.add("X");
        assertEquals("X", iter.previous());
        assertEquals(5, iter.previousIndex());
        assertEquals(6, iter.nextIndex());
        assertEquals("[a, b, c, d, e, f, X]", list.toString());
    }

    @Test
    public void testAddMultiple() {
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

    @Test
    public void testAddAfterPrevious() {
        advance(iter, 3);
        iter.previous(); // c
        iter.add("X");
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("[a, b, X, c, d, e, f]", list.toString());
    }

    @Test
    public void testAddAfterRemove() {
        advance(iter, 3);
        iter.remove();
        iter.add("X");
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("[a, b, X, d, e, f]", list.toString());
    }

    @Test
    public void testAddAfterSet() {
        advance(iter, 3);
        iter.set("X");
        iter.add("Y");
        assertEquals(3, iter.previousIndex());
        assertEquals(4, iter.nextIndex());
        assertEquals("[a, b, X, Y, d, e, f]", list.toString());
    }

    // ========== Remove Tests ==========

    @Test
    public void testRemoveBeginning() {
        iter.next();
        iter.remove();
        assertEquals(-1, iter.previousIndex());
        assertEquals(0, iter.nextIndex());
        assertEquals("[b, c, d, e, f]", list.toString());
    }

    @Test
    public void testRemoveMiddle() {
        advance(iter, 3);
        iter.remove();
        assertEquals(1, iter.previousIndex());
        assertEquals(2, iter.nextIndex());
        assertEquals("[a, b, d, e, f]", list.toString());
    }

    @Test
    public void testRemoveEnd() {
        toEnd(iter);
        iter.remove();
        assertEquals(4, iter.previousIndex());
        assertEquals(5, iter.nextIndex());
        assertEquals("[a, b, c, d, e]", list.toString());
    }

    @Test
    public void testRemoveAfterPrevious() {
        advance(iter, 4);
        iter.previous(); // d
        iter.previous(); // c
        iter.remove();
        assertEquals(1, iter.previousIndex());
        assertEquals(2, iter.nextIndex());
        assertEquals("[a, b, d, e, f]", list.toString());
    }

    @Test
    public void testRemoveAfterSet() {
        advance(iter, 3);
        iter.set("X");
        iter.remove();
        assertEquals(1, iter.previousIndex());
        assertEquals(2, iter.nextIndex());
        assertEquals("[a, b, d, e, f]", list.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveInitialThrowsException() {
        iter.remove();
    }

    @Test
    public void testRemoveTwiceThrowsException() {
        iter.next();
        iter.remove();
        try { iter.remove(); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    @Test
    public void testRemoveAfterAddThrowsException() {
        iter.next();
        iter.add("X");
        try { iter.remove(); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    // ========== Set Tests ==========

    @Test
    public void testSetBeginning() {
        iter.next();
        iter.set("X");
        assertEquals(0, iter.previousIndex());
        assertEquals(1, iter.nextIndex());
        assertEquals("[X, b, c, d, e, f]", list.toString());
    }

    @Test
    public void testSetMiddle() {
        advance(iter, 3);
        iter.set("X");
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("[a, b, X, d, e, f]", list.toString());
    }

    @Test
    public void testSetEnd() {
        toEnd(iter);
        iter.set("X");
        assertEquals(5, iter.previousIndex());
        assertEquals(6, iter.nextIndex());
        assertEquals("[a, b, c, d, e, X]", list.toString());
    }

    @Test
    public void testSetTwice() {
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

    @Test
    public void testSetAfterPrevious() {
        advance(iter, 4);
        iter.previous(); // d
        iter.previous(); // c
        iter.set("X");
        assertEquals(1, iter.previousIndex());
        assertEquals(2, iter.nextIndex());
        assertEquals("[a, b, X, d, e, f]", list.toString());
    }

    @Test
    public void testSetInitialThrowsException() {
        try { iter.set("X"); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    @Test
    public void testSetAfterAddThrowsException() {
        iter.next();
        iter.add("X");
        try { iter.set("Y"); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    @Test
    public void testSetAfterRemoveThrowsException() {
        iter.next();
        iter.remove();
        try { iter.set("X"); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    // ========== Positioned Iterator Tests ==========

    @Test
    public void testPosBeginning() {
        iter = list.listIterator(0);
        assertFalse(iter.hasPrevious());
        assertTrue(iter.hasNext());
        assertEquals(-1, iter.previousIndex());
        assertEquals(0, iter.nextIndex());
        assertEquals("a", iter.next());
    }

    @Test
    public void testPosMiddle() {
        iter = list.listIterator(3);
        assertTrue(iter.hasPrevious());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.previousIndex());
        assertEquals(3, iter.nextIndex());
        assertEquals("d", iter.next());
    }

    @Test
    public void testPosEnd() {
        iter = list.listIterator(6);
        assertTrue(iter.hasPrevious());
        assertFalse(iter.hasNext());
        assertEquals(5, iter.previousIndex());
        assertEquals(6, iter.nextIndex());
        assertEquals("f", iter.previous());
    }

    @Test
    public void testPosAdd() {
        iter = list.listIterator(3);
        iter.add("X");
        assertEquals(3, iter.previousIndex());
        assertEquals(4, iter.nextIndex());
        assertEquals("[a, b, c, X, d, e, f]", list.toString());
    }

    @Test
    public void testPosInitialRemoveThrowsException() {
        iter = list.listIterator(3);
        try { iter.remove(); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    @Test
    public void testPosInitialSetThrowsException() {
        iter = list.listIterator(3);
        try { iter.set("X"); } catch (IllegalStateException e) {return;}
        fail("Expected IllegalStateException");
    }

    // ========== Concurrency Tests ==========

    @Test
    public void testConcurrencyAddIteratorNext() {
        iter = list.listIterator();
        list.add("aa");
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddIndexedIteratorNext() {
        iter = list.listIterator();
        list.add(1, "aa");
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyRemoveIteratorNext() {
        iter = list.listIterator();
        list.remove("b");
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRemoveIndexedIteratorNext() {
        iter = list.listIterator();
        list.remove(2);
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddAllIteratorNext() {
        iter = list.listIterator();
        list.addAll(Collections.singleton("f"));
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddAllIndexedIteratorNext() {
        iter = list.listIterator();
        list.addAll(1, Collections.singleton("f"));
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    
    @Test
    public void testConcurrencyGetIteratorNext() {
        iter = list.listIterator();
        list.get(2);
        iter.next();
    }

    @Test
    public void testConcurrencyRemoveAllIteratorNext() {
        iter = list.listIterator();
        list.removeAll(Arrays.asList("a", "c"));
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRetainAllIteratorNext() {
        iter = list.listIterator();
        list.retainAll(Arrays.asList("a", "c"));
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyIteratorIteratorNext() {
        iter = list.listIterator();
        final Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();
        try { iter.next(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddIteratorPrevious() {
        iter = list.listIterator(2);
        list.add("aa");
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddIndexedIteratorPrevious() {
        iter = list.listIterator(2);
        list.add(1, "aa");
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyRemoveIteratorPrevious() {
        iter = list.listIterator(2);
        list.remove("b");
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRemoveIndexedIteratorPrevious() {
        iter = list.listIterator(2);
        list.remove(2);
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddAllIteratorPrevious() {
        iter = list.listIterator(2);
        list.addAll(Collections.singleton("f"));
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddAllIndexedIteratorPrevious() {
        iter = list.listIterator(2);
        list.addAll(1, Collections.singleton("f"));
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyGetIteratorPrevious() {
        iter = list.listIterator(2);
        list.get(2);
        iter.previous();
    }

    @Test
    public void testConcurrencyRemoveAllIteratorPrevious() {
        iter = list.listIterator(2);
        list.removeAll(Arrays.asList("a", "c"));
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRetainAllIteratorPrevious() {
        iter = list.listIterator(2);
        list.retainAll(Arrays.asList("a", "c"));
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyIteratorIteratorPrevious() {
        iter = list.listIterator(2);
        final Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();
        try { iter.previous(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyAddIteratorRemove() {
        iter = list.listIterator();
        iter.next();
        list.add("aa");
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    
    @Test
    public void testConcurrencyAddIndexedIteratorRemove() {
        iter = list.listIterator();
        iter.next();
        list.add(1, "aa");
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRemoveIteratorRemove() {
        iter = list.listIterator();
        iter.next();
        list.remove("b");
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRemoveIndexedIteratorRemove() {
        iter = list.listIterator();
        iter.next();
        list.remove(2);
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddAllIteratorRemove() {
        iter = list.listIterator();
        iter.next();
        list.addAll(Collections.singleton("f"));
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddAllIndexedIteratorRemove() {
        iter = list.listIterator();
        iter.next();
        list.addAll(1, Collections.singleton("f"));
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyGetIteratorRemove() {
        iter = list.listIterator();
        iter.next();
        list.get(2);
        iter.remove();
    }

    @Test
    public void testConcurrencyRemoveAllIteratorRemove() {
        iter = list.listIterator();
        iter.next();
        list.removeAll(Arrays.asList("a", "c"));
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRetainAllIteratorRemove() {
        iter = list.listIterator();
        iter.next();
        list.retainAll(Arrays.asList("a", "c"));
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyIteratorIteratorRemove() {
        iter = list.listIterator();
        iter.next();
        final Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();
        try { iter.remove(); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyAddIteratorSet() {
        iter = list.listIterator();
        iter.next();
        list.add("aa");
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyAddIndexedIteratorSet() {
        iter = list.listIterator();
        iter.next();
        list.add(1, "aa");
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRemoveIteratorSet() {
        iter = list.listIterator();
        iter.next();
        list.remove("b");
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRemoveIndexedIteratorSet() {
        iter = list.listIterator();
        iter.next();
        list.remove(2);
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddAllIteratorSet() {
        iter = list.listIterator();
        iter.next();
        list.addAll(Collections.singleton("f"));
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
    @Test
    public void testConcurrencyAddAllIndexedIteratorSet() {
        iter = list.listIterator();
        iter.next();
        list.addAll(1, Collections.singleton("f"));
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyGetIteratorSet() {
        iter = list.listIterator();
        iter.next();
        list.get(2);
        iter.set("x");
    }

    @Test
    public void testConcurrencyRemoveAllIteratorSet() {
        iter = list.listIterator();
        iter.next();
        list.removeAll(Arrays.asList("a", "c"));
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyRetainAllIteratorSet() {
        iter = list.listIterator();
        iter.next();
        list.retainAll(Arrays.asList("a", "c"));
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }

    @Test
    public void testConcurrencyIteratorIteratorSet() {
        iter = list.listIterator();
        iter.next();
        final Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();
        try { iter.set("x"); } catch (ConcurrentModificationException e) {return;}
        fail("Expected ConcurrentModificationException");
    }
}
