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

import org.junit.Test;

/**
 */
public class BorderImageSlicesTest {
    @Test public void instanceCreation() {
//        BorderImageSlices slices = new BorderImageSlices(.1, .2, .3, .4, false, true, false, true, false);
//        assertEquals(.1, slices.getTop(), 0);
//        assertEquals(.2, slices.getRight(), 0);
//        assertEquals(.3, slices.getBottom(), 0);
//        assertEquals(.4, slices.getLeft(), 0);
//        assertFalse(slices.isTopAsPercentage());
//        assertTrue(slices.isRightAsPercentage());
//        assertFalse(slices.isBottomAsPercentage());
//        assertTrue(slices.isLeftAsPercentage());
//        assertFalse(slices.isFilled());
    }
//
//    @Test public void instanceCreation2() {
//        BorderImageSlices slices = new BorderImageSlices(.1, .2, .3, .4, true, false, true, false, true);
//        assertEquals(.1, slices.getTop(), 0);
//        assertEquals(.2, slices.getRight(), 0);
//        assertEquals(.3, slices.getBottom(), 0);
//        assertEquals(.4, slices.getLeft(), 0);
//        assertTrue(slices.isTopAsPercentage());
//        assertFalse(slices.isRightAsPercentage());
//        assertTrue(slices.isBottomAsPercentage());
//        assertFalse(slices.isLeftAsPercentage());
//        assertTrue(slices.isFilled());
//    }
//
//    @Test public void instanceCreation3() {
//        BorderImageSlices slices = new BorderImageSlices(.1, .2, .3, .4, true, false, false, true, false);
//        assertEquals(.1, slices.getTop(), 0);
//        assertEquals(.2, slices.getRight(), 0);
//        assertEquals(.3, slices.getBottom(), 0);
//        assertEquals(.4, slices.getLeft(), 0);
//        assertTrue(slices.isTopAsPercentage());
//        assertFalse(slices.isRightAsPercentage());
//        assertFalse(slices.isBottomAsPercentage());
//        assertTrue(slices.isLeftAsPercentage());
//    }
//
//    @Test public void topPercentGreaterThanOneShouldBeClampedToOne() {
//        BorderImageSlices slices = new BorderImageSlices(2, 0, 0, 0, true, false, false, false, false);
//        assertEquals(1, slices.getTop(), 0);
//    }
//
//    @Test public void rightPercentGreaterThanOneShouldBeClampedToOne() {
//        BorderImageSlices slices = new BorderImageSlices(0, 2, 0, 0, false, true, false, false, false);
//        assertEquals(1, slices.getRight(), 0);
//    }
//
//    @Test public void bottomPercentGreaterThanOneShouldBeClampedToOne() {
//        BorderImageSlices slices = new BorderImageSlices(0, 0, 2, 0, false, false, true, false, false);
//        assertEquals(1, slices.getBottom(), 0);
//    }
//
//    @Test public void leftPercentGreaterThanOneShouldBeClampedToOne() {
//        BorderImageSlices slices = new BorderImageSlices(0, 0, 0, 2, false, false, false, true, false);
//        assertEquals(1, slices.getLeft(), 0);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void cannotSpecifyNegativeTop() {
//        new BorderImageSlices(-2, 0, 0, 0, false, false, false, false, false);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void cannotSpecifyNegativeRight() {
//        new BorderImageSlices(0, -2, 0, 0, false, false, false, false, false);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void cannotSpecifyNegativeBottom() {
//        new BorderImageSlices(0, 0, -2, 0, false, false, false, false, false);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void cannotSpecifyNegativeLeft() {
//        new BorderImageSlices(0, 0, 0, -2, false, false, false, false, false);
//    }
//
//    @Test public void equality() {
//        BorderImageSlices a = new BorderImageSlices(1, 2, 3, 4, true, false, false, true, false);
//        BorderImageSlices b = new BorderImageSlices(1, 2, 3, 4, true, false, false, true, false);
//        assertEquals(a, b);
//    }
//
//    @Test public void same() {
//        assertEquals(BorderImageSlices.EMPTY, BorderImageSlices.EMPTY);
//    }
//
//    @Test public void different() {
//        BorderImageSlices a = new BorderImageSlices(.5, 2, 3, 4, true, false, false, true, false);
//        BorderImageSlices b = new BorderImageSlices(.6, 2, 3, 4, true, false, false, true, false);
//        assertFalse(a.equals(b));
//    }
//
//    @Test public void different2() {
//        BorderImageSlices a = new BorderImageSlices(1, 2, 3, 4, true, false, false, true, false);
//        BorderImageSlices b = new BorderImageSlices(1, 3, 3, 4, true, false, false, true, false);
//        assertFalse(a.equals(b));
//    }
//
//    @Test public void different3() {
//        BorderImageSlices a = new BorderImageSlices(1, 2, 3, 4, true, false, false, true, false);
//        BorderImageSlices b = new BorderImageSlices(1, 2, 4, 4, true, false, false, true, false);
//        assertFalse(a.equals(b));
//    }
//
//    @Test public void different4() {
//        BorderImageSlices a = new BorderImageSlices(1, 2, 3, .4, true, false, false, true, false);
//        BorderImageSlices b = new BorderImageSlices(1, 2, 3, .5, true, false, false, true, false);
//        assertFalse(a.equals(b));
//    }
//
//    @Test public void different5() {
//        BorderImageSlices a = new BorderImageSlices(1, 2, 3, 4, true, false, false, true, false);
//        BorderImageSlices b = new BorderImageSlices(1, 2, 3, 4, false, false, false, true, false);
//        assertFalse(a.equals(b));
//    }
//
//    @Test public void different6() {
//        BorderImageSlices a = new BorderImageSlices(1, 2, 3, 4, true, false, false, true, false);
//        BorderImageSlices b = new BorderImageSlices(1, 2, 3, 4, true, true, false, true, false);
//        assertFalse(a.equals(b));
//    }
//
//    @Test public void different7() {
//        BorderImageSlices a = new BorderImageSlices(1, 2, 3, 4, true, false, false, true, false);
//        BorderImageSlices b = new BorderImageSlices(1, 2, 3, 4, true, false, true, true, false);
//        assertFalse(a.equals(b));
//    }
//
//    @Test public void different8() {
//        BorderImageSlices a = new BorderImageSlices(1, 2, 3, 4, true, false, false, true, false);
//        BorderImageSlices b = new BorderImageSlices(1, 2, 3, 4, true, false, false, false, false);
//        assertFalse(a.equals(b));
//    }
//
//    @Test public void noEqualToNull() {
//        assertFalse(BorderImageSlices.EMPTY.equals(null));
//    }
//
//    @Test public void noEqualToRandom() {
//        assertFalse(BorderImageSlices.EMPTY.equals("Some random value"));
//    }
}
