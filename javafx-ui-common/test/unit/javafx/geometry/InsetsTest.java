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

package javafx.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InsetsTest {
    @Test
    public void testEquals() {
        Insets p1 = new Insets(0, 0, 0, 0);
        Insets p2 = new Insets(0, 1, 1, 1);
        Insets p3 = new Insets(1, 0, 1, 1);

        assertTrue(p1.equals(p1));
        assertTrue(p1.equals(new Insets(0, 0, 0, 0)));
        assertFalse(p1.equals(new Object()));
        assertFalse(p1.equals(p2));
        assertFalse(p1.equals(p3));
    }

    @Test
    public void testHash() {
        Insets p1 = new Insets(0, 0, 0, 0);
        Insets p2 = new Insets(0, 1, 0, 0);
        Insets p3 = new Insets(0, 1, 0, 0);

        assertEquals(p3.hashCode(), p2.hashCode());
        assertEquals(p3.hashCode(), p2.hashCode());
        assertFalse(p1.hashCode() == p2.hashCode());
    }

    @Test
    public void testToString() {
        Insets p1 = new Insets(0, 0, 0, 0);
        assertNotNull(p1.toString());

    }
}
