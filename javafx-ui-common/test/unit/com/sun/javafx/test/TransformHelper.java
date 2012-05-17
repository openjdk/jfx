/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.test;

import javafx.scene.transform.Transform;
import static org.junit.Assert.assertEquals;

public class TransformHelper {
    public static void assertMatrix(Transform matrix,
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        assertEquals(mxx, matrix.getMxx(), 0.00001);
        assertEquals(mxy, matrix.getMxy(), 0.00001);
        assertEquals(mxz, matrix.getMxz(), 0.00001);
        assertEquals(tx, matrix.getTx(), 0.00001);
        assertEquals(myx, matrix.getMyx(), 0.00001);
        assertEquals(myy, matrix.getMyy(), 0.00001);
        assertEquals(myz, matrix.getMyz(), 0.00001);
        assertEquals(ty, matrix.getTy(), 0.00001);
        assertEquals(mzx, matrix.getMzx(), 0.00001);
        assertEquals(mzy, matrix.getMzy(), 0.00001);
        assertEquals(mzz, matrix.getMzz(), 0.00001);
        assertEquals(tz, matrix.getTz(), 0.00001);
    }

}
