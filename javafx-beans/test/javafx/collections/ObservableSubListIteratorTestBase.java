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
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * Tests for iterators of sublists of ObservableList.
 * Note that this is a subclass of ObservableListIteratorTest.
 * As such, it inherits all the tests from that class, but they
 * are run using the sublist-based fixture. There are also some
 * additional tests that make assertions about the underlying list
 * after mutating the sublist via the iterator.
 * 
 */
public abstract class ObservableSubListIteratorTestBase extends ObservableListIteratorTestBase {

    // ========== Test Fixture ==========

    List<String> fullList;

    public ObservableSubListIteratorTestBase(final Callable<? extends List<String>> listFactory) {
        super(listFactory);
    }

    @Before @Override
    public void setup() throws Exception {
        list = listFactory.call();
        list.addAll(
            Arrays.asList("P", "Q", "a", "b", "c", "d", "e", "f", "R", "S"));
        fullList = list;
        list = fullList.subList(2, 8);
        iter = list.listIterator();
    }

    // ========== Sublist Iterator Tests ==========

    @Test
    public void testSubAddBeginning() {
        iter.add("X");
        assertEquals("[P, Q, X, a, b, c, d, e, f, R, S]", fullList.toString());
    }

    @Test
    public void testSubAddMiddle() {
        advance(iter, 3);
        iter.add("X");
        assertEquals("[P, Q, a, b, c, X, d, e, f, R, S]", fullList.toString());
    }

    @Test
    public void testSubAddEnd() {
        toEnd(iter);
        iter.add("X");
        assertEquals("[P, Q, a, b, c, d, e, f, X, R, S]", fullList.toString());
    }

    @Test
    public void testSubRemoveBeginning() {
        iter.next();
        iter.remove();
        assertEquals("[P, Q, b, c, d, e, f, R, S]", fullList.toString());
    }

    @Test
    public void testSubRemoveMiddle() {
        advance(iter, 3);
        iter.remove();
        assertEquals("[P, Q, a, b, d, e, f, R, S]", fullList.toString());
    }

    @Test
    public void testSubRemoveEnd() {
        toEnd(iter);
        iter.remove();
        assertEquals("[P, Q, a, b, c, d, e, R, S]", fullList.toString());
    }

    @Test
    public void testSubSetBeginning() {
        iter.next();
        iter.set("X");
        assertEquals("[P, Q, X, b, c, d, e, f, R, S]", fullList.toString());
    }

    @Test
    public void testSubSetMiddle() {
        advance(iter, 3);
        iter.set("X");
        assertEquals("[P, Q, a, b, X, d, e, f, R, S]", fullList.toString());
    }

    @Test
    public void testSubSetEnd() {
        toEnd(iter);
        iter.set("X");
        assertEquals("[P, Q, a, b, c, d, e, X, R, S]", fullList.toString());
    }
}
