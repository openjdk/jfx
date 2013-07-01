/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.ps;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.sun.prism.impl.ps.AATesselatorImpl.*;

public class HullTest {

    @Test
    public void linesThatShouldCross() {
        assertTrue(linesCross(0, 0, 100, 100, 0, 100, 100, 0));
        assertTrue(linesCross(100, 100, 0, 0, 0, 100, 100, 0));
        assertTrue(linesCross(100, 100, 0, 0, 100, 0, 0, 100));
    }

    @Test
    public void linesThatShouldNotCross() {
        assertFalse(linesCross(0, 0, 100, 100, 200, 200, 300, 300));
        assertFalse(linesCross(0, 0, -100, -100, -200, -200, -300, -300));
        assertFalse(linesCross(0, 0, 100, 100, 10, 10, 110, 110));
    }

    @Test
    public void linesThatShareAPointButShouldNotCross() {
        assertFalse(linesCross(0, 0, 100, 100, 0, 0, 10, 100));
        assertFalse(linesCross(0, 0, 100, 0, 50, 0, 50, 100));
    }

    @Test
    public void linesThatAreCoincident() {
        assertFalse(linesCross(0, 0, 100, 100, 0, 0, 100, 100));
        assertFalse(linesCross(100, 100, 0, 0, 100, 100, 0, 0));
        assertFalse(linesCross(0, 0, 100, 100, 80, 80, 20, 20));
    }

    @Test
    public void triangleShouldContainPoint() {
        assertTrue(triangleContainsPoint(0, 0, -100, -100, 100, -100, 0, 100));
        assertTrue(triangleContainsPoint(10, 1, 0, 0, 200, 0, 200, 100));
    }

    @Test
    public void triangleShouldNotContainPoint() {
        assertFalse(triangleContainsPoint(0, 0, 100, 0, 200, 0, 200, 100));
        assertFalse(triangleContainsPoint(0, 0, 0, 0, 200, 0, 200, 100));
        assertFalse(triangleContainsPoint(50, 0, 0, 0, 200, 0, 200, 100));

        // test case where denom goes to zero
        assertFalse(triangleContainsPoint(200, 10, 0, 0, 400, 200, 200, 100));
    }
}
