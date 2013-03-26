/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.sg.prism;

import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

public class NGTriangleMeshTest {

    /**
     * Test of setFaceSmoothingGroups method, of class NGTriangleMesh.
     */
    @Test
    public void testSetFaceSmoothingGroups_intArr() {
        int[] faceSmoothingGroups = new int[]{0, 1, 2, 3, 4, 5};
        NGTriangleMesh instance = new NGTriangleMesh();
        instance.setFaceSmoothingGroups(faceSmoothingGroups);
        int[] actuals = instance.test_getShiftedFaceSmoothingGroups();
        int[] expecteds = new int[]{1, 2, 4, 8, 16, 32};
        assertArrayEquals(expecteds, actuals);
    }

    /**
     * Test of setFaceSmoothingGroups method, of class NGTriangleMesh.
     */
    @Test
    public void testSetFaceSmoothingGroups_3args() {
        int[] faceSmoothingGroups = new int[]{0, 1, 2, 3, 4, 5};
        NGTriangleMesh instance = new NGTriangleMesh();
        instance.setFaceSmoothingGroups(faceSmoothingGroups);
        Arrays.fill(faceSmoothingGroups, 1);
        instance.setFaceSmoothingGroups(faceSmoothingGroups, 1, 4);
        int[] actuals = instance.test_getShiftedFaceSmoothingGroups();
        int[] expecteds = new int[]{1, 2, 2, 2, 2, 32};
        assertArrayEquals(expecteds, actuals);
    }
}
