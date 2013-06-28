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

package javafx.scene.effect;

import static com.sun.javafx.test.TestHelper.box;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class SepiaToneTest extends EffectsTestBase {
    private SepiaTone effect;

    @Before
    public void setUp() {
        effect = new SepiaTone();
        setupTest(effect);
    }

    @Test
    public void testSetLevel() {
        // try setting correct value
        effect.setLevel(0.5f);
        assertEquals(0.5f, effect.getLevel(), 1e-100);
        pulse();
        assertEquals(0.5f, ((com.sun.scenario.effect.SepiaTone)effect.impl_getImpl()).getLevel(), 1e-100);
    }
    
    @Test
    public void testDefaultLevel() {
        // default value should be 1
        assertEquals(1f, effect.getLevel(), 1e-100);
        assertEquals(1f, effect.levelProperty().get(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.SepiaTone)effect.impl_getImpl()).getLevel(), 1e-100);
    }
    
    @Test
    public void testMinLevel() {
        // 0 should be ok
        effect.setLevel(0);
        // try setting value smaller than minimal
        effect.setLevel(-0.1f);
        assertEquals(-0.1f, effect.getLevel(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.SepiaTone)effect.impl_getImpl()).getLevel(), 1e-100);        
    }
    
    @Test
    public void testMaxLevel() {
        // 1 should be ok
        effect.setLevel(1);
        // try setting value greater than maximal
        effect.setLevel(1.1f); 
        assertEquals(1.1f, effect.getLevel(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.SepiaTone)effect.impl_getImpl()).getLevel(), 1e-100);        
    }

    @Test
    public void testLevelSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.SepiaTone", "level",
                "com.sun.scenario.effect.SepiaTone", "level", 0.5);
    }

    @Test
    public void testInputSynced() throws Exception {
        BoxBlur blur = new BoxBlur();
        checkEffectPropertySynced(
                "javafx.scene.effect.SepiaTone", "input",
                "com.sun.scenario.effect.SepiaTone", "input",
                blur, (com.sun.scenario.effect.BoxBlur)blur.impl_getImpl());
    }

    @Test
    public void testBounds() {
        assertEquals(box(0, 0, 100, 100), n.getBoundsInLocal());
    }

    @Test
    public void testBoundsWidthInput() {
        assertEquals(box(0, 0, 100, 100), n.getBoundsInLocal());
        BoxBlur blur = new BoxBlur();
        effect.setInput(blur);
        assertEquals(box(-2, -2, 104, 104), n.getBoundsInLocal());
    }

    @Test
    public void testCreateWithParams() {
        effect = new SepiaTone(0.1);
        setupTest(effect);
        assertEquals(0.1, effect.getLevel(), 1e-100);
        pulse();
        assertEquals(0.1f, ((com.sun.scenario.effect.SepiaTone) effect.impl_getImpl()).getLevel(), 1e-100);
    }

    @Test
    public void testCreateWithDefaultParams() {
        effect = new SepiaTone(1);
        setupTest(effect);
        assertEquals(1, effect.getLevel(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.SepiaTone) effect.impl_getImpl()).getLevel(), 1e-100);
    }
}
