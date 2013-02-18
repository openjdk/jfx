/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.transform;

import org.junit.Test;
import static org.junit.Assert.*;

public class MatrixTypeTest {

    @Test
    public void testIs2D() {
        assertTrue(MatrixType.MT_2D_2x3.is2D());
        assertTrue(MatrixType.MT_2D_3x3.is2D());
        assertFalse(MatrixType.MT_3D_3x4.is2D());
        assertFalse(MatrixType.MT_3D_4x4.is2D());
    }

    @Test
    public void testRows() {
        assertEquals(2, MatrixType.MT_2D_2x3.rows());
        assertEquals(3, MatrixType.MT_2D_3x3.rows());
        assertEquals(3, MatrixType.MT_3D_3x4.rows());
        assertEquals(4, MatrixType.MT_3D_4x4.rows());
    }

    @Test
    public void testColumns() {
        assertEquals(3, MatrixType.MT_2D_2x3.columns());
        assertEquals(3, MatrixType.MT_2D_3x3.columns());
        assertEquals(4, MatrixType.MT_3D_3x4.columns());
        assertEquals(4, MatrixType.MT_3D_4x4.columns());
    }

    @Test
    public void testElements() {
        assertEquals(6, MatrixType.MT_2D_2x3.elements());
        assertEquals(9, MatrixType.MT_2D_3x3.elements());
        assertEquals(12, MatrixType.MT_3D_3x4.elements());
        assertEquals(16, MatrixType.MT_3D_4x4.elements());
    }
}
