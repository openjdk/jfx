/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class BackgroundSizeTest {
    @Test public void instanceCreation() {
        BackgroundSize size = new BackgroundSize(1, 2, true, false, true, false);
        assertEquals(1, size.getWidth(), 0);
        assertEquals(2, size.getHeight(), 0);
        assertTrue(size.isWidthAsPercentage());
        assertFalse(size.isHeightAsPercentage());
        assertTrue(size.isContain());
        assertFalse(size.isCover());
    }

    @Test public void instanceCreation2() {
        BackgroundSize size = new BackgroundSize(0, Double.MAX_VALUE, false, true, false, true);
        assertEquals(0, size.getWidth(), 0);
        assertEquals(Double.MAX_VALUE, size.getHeight(), 0);
        assertFalse(size.isWidthAsPercentage());
        assertTrue(size.isHeightAsPercentage());
        assertFalse(size.isContain());
        assertTrue(size.isCover());
    }

    @Test public void instanceCreation3() {
        BackgroundSize size = new BackgroundSize(.5, .5, true, true, false, false);
        assertEquals(.5, size.getWidth(), 0);
        assertEquals(.5, size.getHeight(), 0);
        assertTrue(size.isWidthAsPercentage());
        assertTrue(size.isHeightAsPercentage());
        assertFalse(size.isContain());
        assertFalse(size.isCover());
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeWidthThrowsException() {
        new BackgroundSize(-.2, 1, true, true, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeWidthThrowsException2() {
        new BackgroundSize(-2, 1, true, true, false, false);
    }

    @Ignore("Surprised that MIN_VALUE is not < 0")
    @Test(expected = IllegalArgumentException.class)
    public void negativeWidthThrowsException3() {
        new BackgroundSize(Double.MIN_VALUE, 1, true, true, false, false);
    }

    @Ignore("Not handling positive infinity")
    @Test(expected = IllegalArgumentException.class)
    public void positiveInfinityWidthThrowsException() {
        new BackgroundSize(Double.POSITIVE_INFINITY, 1, true, true, false, false);
    }

    @Ignore("Not handling negative infinity")
    @Test(expected = IllegalArgumentException.class)
    public void negativeInfinityWidthThrowsException() {
        new BackgroundSize(Double.NEGATIVE_INFINITY, 1, true, true, false, false);
    }

    @Ignore("Not handling NaN")
    @Test(expected = IllegalArgumentException.class)
    public void nanWidthThrowsException() {
        new BackgroundSize(Double.NaN, 1, true, true, false, false);
    }

    @Test public void negativeZeroWidthIsOK() {
        BackgroundSize size = new BackgroundSize(-0, 1, true, true, false, false);
        assertEquals(0, size.getWidth(), 0);
    }

    @Test public void autoWidthIsOK() {
        BackgroundSize size = new BackgroundSize(-1, 1, true, true, false, false);
        assertEquals(BackgroundSize.AUTO, size.getWidth(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeHeightThrowsException() {
        new BackgroundSize(1, -.1, true, true, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeHeightThrowsException2() {
        new BackgroundSize(1, -2, true, true, false, false);
    }

    @Ignore("Surprised that MIN_VALUE is not < 0")
    @Test(expected = IllegalArgumentException.class)
    public void negativeHeightThrowsException3() {
        new BackgroundSize(1, Double.MIN_VALUE, true, true, false, false);
    }

    @Ignore("Not handling positive infinity")
    @Test(expected = IllegalArgumentException.class)
    public void positiveInfinityHeightThrowsException() {
        new BackgroundSize(1, Double.POSITIVE_INFINITY, true, true, false, false);
    }

    @Ignore("Not handling negative infinity")
    @Test(expected = IllegalArgumentException.class)
    public void negativeInfinityHeightThrowsException() {
        new BackgroundSize(1, Double.NEGATIVE_INFINITY, true, true, false, false);
    }

    @Ignore("Not handling NaN")
    @Test(expected = IllegalArgumentException.class)
    public void nanHeightThrowsException() {
        new BackgroundSize(1, Double.NaN, true, true, false, false);
    }

    @Test public void negativeZeroHeightIsOK() {
        BackgroundSize size = new BackgroundSize(1, -0, true, true, false, false);
        assertEquals(0, size.getHeight(), 0);
    }

    @Test public void autoHeightIsOK() {
        BackgroundSize size = new BackgroundSize(1, -1, true, true, false, false);
        assertEquals(BackgroundSize.AUTO, size.getHeight(), 0);
    }

    @Test public void equivalent() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertEquals(a, b);
    }

    @Test public void equivalent2() {
        BackgroundSize a = new BackgroundSize(1, .5, false, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, false, true, true, true);
        assertEquals(a, b);
    }

    @Test public void equivalent3() {
        BackgroundSize a = new BackgroundSize(1, .5, true, false, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, false, true, true);
        assertEquals(a, b);
    }

    @Test public void equivalent4() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, false, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, false, true);
        assertEquals(a, b);
    }

    @Test public void equivalent5() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, false);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, false);
        assertEquals(a, b);
    }

    @Test public void equivalentHaveSameHashCode() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHaveSameHashCode2() {
        BackgroundSize a = new BackgroundSize(1, .5, false, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, false, true, true, true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHaveSameHashCode3() {
        BackgroundSize a = new BackgroundSize(1, .5, true, false, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, false, true, true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHaveSameHashCode4() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, false, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, false, true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void equivalentHaveSameHashCode5() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, false);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, false);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void notEquivalent() {
        BackgroundSize a = new BackgroundSize(0, .5, true, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEquivalent2() {
        BackgroundSize a = new BackgroundSize(1, 1, true, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEquivalent3() {
        BackgroundSize a = new BackgroundSize(1, .5, false, true, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEquivalent4() {
        BackgroundSize a = new BackgroundSize(1, .5, true, false, true, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEquivalent5() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, false, true);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEquivalent6() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, false);
        BackgroundSize b = new BackgroundSize(1, .5, true, true, true, true);
        assertFalse(a.equals(b));
    }

    @Test public void notEqualToNull() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, false);
        assertFalse(a.equals(null));
    }

    @Test public void notEqualToRandom() {
        BackgroundSize a = new BackgroundSize(1, .5, true, true, true, false);
        assertFalse(a.equals("Some random object"));
    }
}
