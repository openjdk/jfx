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

package com.sun.javafx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class WeakReferenceMapTest {
    @Test
    public void testAdd() {
        WeakReferenceMap m = new WeakReferenceMap();
        String s = new String("Wow!");
        m.put(s, "value");
        assertEquals(1, m.size());
    }

    @Test
    public void testRemove() {
        WeakReferenceMap m = new WeakReferenceMap();
        String a = new String("a");
        m.put(a, "a-value");
        String b = new String("b");
        m.put(b, "b-value");
        String c = new String("c");
        m.put(c, "c-value");

        assertEquals(3, m.size());
        m.remove(a);
        m.remove(c);
        assertEquals(1, m.size());
    }

    @Test
    public void testCleanup() {
        WeakReferenceMap m = new WeakReferenceMap();
        String a = new String("a");
        m.put(a, "a-value");
        String b = new String("b");
        m.put(b, "b-value");
        String c = new String("c");
        m.put(c, "c-value");

        assertEquals(3, m.size());
        a = null;
        c = null;
        System.gc();
        System.gc();
        System.gc(); // hope that worked!
        assertEquals(1, m.size());
    }

    @Test
    public void testGet() {
        WeakReferenceMap m = new WeakReferenceMap();
        String a = new String("a");
        m.put(a, "a-value");
        String b = new String("b");
        m.put(b, "b-value");
        String c = new String("c");
        m.put(c, "c-value");

        assertEquals("a-value", m.get(a));
        assertEquals("b-value", m.get(b));
        assertEquals("c-value", m.get(c));
        assertNull(m.get("z"));
    }
}
