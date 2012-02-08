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
package javafx.scene.effect;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class PerspectiveTransformTest extends EffectsTestBase {
    private PerspectiveTransform effect;

    @Before
    public void setUp() {
        effect = new PerspectiveTransform();
        setupTest(effect);
    }

    @Test
    public void testLlx() {
        // try setting correct value
        effect.setLlx(1.0f);
        assertEquals(1.0f, effect.getLlx(), 1e-100);
    }

    @Test
    public void testLly() {
        // try setting correct value
        effect.setLly(1.0f);
        assertEquals(1.0f, effect.getLly(), 1e-100);
    }

    @Test
    public void testLrx() {
        // try setting correct value
        effect.setLrx(1.0f);
        assertEquals(1.0f, effect.getLrx(), 1e-100);
    }

    @Test
    public void testLry() {
        // try setting correct value
        effect.setLry(1.0f);
        assertEquals(1.0f, effect.getLry(), 1e-100);
    }

    @Test
    public void testULx() {
        // try setting correct value
        effect.setUlx(1.0f);
        assertEquals(1.0f, effect.getUlx(), 1e-100);
    }

    @Test
    public void testUly() {
        // try setting correct value
        effect.setUly(1.0f);
        assertEquals(1.0f, effect.getUly(), 1e-100);
    }

    @Test
    public void testUrx() {
        // try setting correct value
        effect.setUrx(1.0f);
        assertEquals(1.0f, effect.getUrx(), 1e-100);
    }

    @Test
    public void testUry() {
        // try setting correct value
        effect.setUry(1.0f);
        assertEquals(1.0f, effect.getUry(), 1e-100);
    }
    
    @Test
    public void testCreateWithParams() {
        effect = new PerspectiveTransform(1, 2, 3, 4, 5, 6, 7, 8);
        setupTest(effect);
        assertEquals(1, effect.getUlx(), 1e-100);
        assertEquals(2, effect.getUly(), 1e-100);
        assertEquals(3, effect.getUrx(), 1e-100);
        assertEquals(4, effect.getUry(), 1e-100);
        assertEquals(5, effect.getLrx(), 1e-100);
        assertEquals(6, effect.getLry(), 1e-100);
        assertEquals(7, effect.getLlx(), 1e-100);
        assertEquals(8, effect.getLly(), 1e-100);
    }

    @Test
    public void testCreateWithDefaultParams() {
        effect = new PerspectiveTransform(0, 0, 0, 0, 0, 0, 0, 0);
        setupTest(effect);
        assertEquals(0, effect.getUlx(), 1e-100);
        assertEquals(0, effect.getUly(), 1e-100);
        assertEquals(0, effect.getUrx(), 1e-100);
        assertEquals(0, effect.getUry(), 1e-100);
        assertEquals(0, effect.getLrx(), 1e-100);
        assertEquals(0, effect.getLry(), 1e-100);
        assertEquals(0, effect.getLlx(), 1e-100);
        assertEquals(0, effect.getLly(), 1e-100);
    }
}
