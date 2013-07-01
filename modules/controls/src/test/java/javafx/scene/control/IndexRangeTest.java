/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class IndexRangeTest {

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void createRangeWithStartLessThanEnd() {
        IndexRange range = new IndexRange(10, 20);
        assertEquals(10, range.getStart());
        assertEquals(20, range.getEnd());
    }

    @Test public void createRangeWithStartEqualToEnd() {
        IndexRange range = new IndexRange(20, 20);
        assertEquals(20, range.getStart());
        assertEquals(20, range.getEnd());
    }

    @Test (expected = IllegalArgumentException.class)
    public void createRangeWithEndLessThanStartResultsIn_IAE() {
        new IndexRange(20, 10);
    }

    @Test public void createRangeUsingCopyConstructor() {
        IndexRange range = new IndexRange(10, 20);
        range = new IndexRange(range);
        assertEquals(10, range.getStart());
        assertEquals(20, range.getEnd());
    }

    @Test (expected = NullPointerException.class)
    public void createRangeUsingCopyConstructorPassingNullResultsIn_NPE() {
        new IndexRange(null);
    }

    /*********************************************************************
     * Tests for length                                                  *
     ********************************************************************/

    // Note that the start is inclusive, the end is exclusive.
    @Test public void lengthIsCorrect() {
        IndexRange range = new IndexRange(0, 1);
        assertEquals(1, range.getLength());
        range = new IndexRange(10, 20);
        assertEquals(10, range.getLength());
    }

    @Test public void lengthIsCorrectWhenStartEqualsEnd() {
        IndexRange range = new IndexRange(0, 0);
        assertEquals(0, range.getLength());
    }

    @Test public void lengthIsCorrectForNegativeRanges() {
        IndexRange range = new IndexRange(-1, 0);
        assertEquals(1, range.getLength());
        range = new IndexRange(-20, -10);
        assertEquals(10, range.getLength());
    }

    /*********************************************************************
     * Tests for equality                                                *
     ********************************************************************/

    @Test public void equalsComparingSameInstanceReturnsTrue() {
        IndexRange range = new IndexRange(10, 20);
        assertEquals(range, range);
    }

    @Test public void equalsComparingEquivalentInstancesReturnsTrue() {
        IndexRange a = new IndexRange(10, 20);
        IndexRange b = new IndexRange(10, 20);
        assertEquals(a, b);
    }

    @Test public void equalsComparingDifferingInstancesReturnsFalse() {
        IndexRange a = new IndexRange(10, 20);
        IndexRange b = new IndexRange(5, 15);
        assertFalse(a.equals(b));
    }

    @Test public void equalsComparingToNullReturnsFalse() {
        IndexRange range = new IndexRange(10, 20);
        assertFalse(range.equals(null));
    }

    /*********************************************************************
     * Tests for hashCode                                                *
     ********************************************************************/

    @Test public void hashForSameInstanceIsSame() {
        IndexRange range = new IndexRange(10, 20);
        assertEquals(range.hashCode(), range.hashCode());
    }

    @Test public void hashForEquivalentInstancesIsSame() {
        IndexRange a = new IndexRange(10, 20);
        IndexRange b = new IndexRange(10, 20);
        assertEquals(a.hashCode(), b.hashCode());
    }

    /*********************************************************************
     * Tests for normalize                                               *
     ********************************************************************/

    @Test public void normalizeAValidRange() {
        IndexRange range = IndexRange.normalize(10, 20);
        assertEquals(new IndexRange(10, 20), range);
    }

    @Test public void normalizeAnInvalidPositiveRange() {
        IndexRange range = IndexRange.normalize(20, 10);
        assertEquals(new IndexRange(10, 20), range);
    }

    @Test public void normalizeAnInvalidNegativeRange() {
        IndexRange range = IndexRange.normalize(-5, -10);
        assertEquals(new IndexRange(-10, -5), range);
    }

    @Test public void normalizeWhereTheStartAndEndAreTheSame() {
        IndexRange range = IndexRange.normalize(10, 10);
        assertEquals(new IndexRange(10, 10), range);
    }
}
