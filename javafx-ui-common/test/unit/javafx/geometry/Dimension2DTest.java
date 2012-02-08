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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 */

package javafx.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class Dimension2DTest {
    @Test
    public void testEquals() {
        Dimension2D p1 = new Dimension2D(1, 1);
        Dimension2D p2 = new Dimension2D(0, 0);
        Dimension2D p3 = new Dimension2D(0, 0);

        assertTrue(p1.equals(p1));
        assertTrue(p1.equals(new Dimension2D(1, 1)));
        assertFalse(p1.equals(new Object()));
        assertFalse(p1.equals(p2));
        assertFalse(p1.equals(p3));
        assertTrue(p2.equals(p3));
    }

    @Test
    public void testHashCode() {
        Dimension2D d = new Dimension2D(1, 2);
        Dimension2D d2 = new Dimension2D(1, 1);
        int h = d.hashCode();
        assertEquals(h, d.hashCode());
        assertFalse(d.hashCode() == d2.hashCode());
    }

    @Test
    public void testToString() {
        assertNotNull(new Dimension2D(0,0).toString());
    }


}
