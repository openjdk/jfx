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

public class GaussianBlurTest extends EffectsTestBase {
    private GaussianBlur effect;

    @Before
    public void setUp() {
        effect = new GaussianBlur();
        setupTest(effect);
    }

    @Test
    public void testSetRadius() {
        // try setting correct value
        effect.setRadius(1.0f);
        assertEquals(1.0f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.GaussianBlur)effect.impl_getImpl()).getRadius(), 1e-100);
    }
    
    @Test
    public void testDefaultRadius() {
        // default value should be 10
        assertEquals(10, effect.getRadius(), 1e-10);
        assertEquals(10, effect.radiusProperty().get(), 1e-10);
        pulse();
        assertEquals(10, ((com.sun.scenario.effect.GaussianBlur)effect.impl_getImpl()).getRadius(), 1e-100);
    }
    
    @Test
    public void testMinRadius() {
        // 0 should be ok
        effect.setRadius(0);
        // try setting value smaller than minimal
        effect.setRadius(-0.1f);
        assertEquals(-0.1f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.GaussianBlur)effect.impl_getImpl()).getRadius(), 1e-100);        
    }

    @Test
    public void testMaxRadius() {
        // 63 should be ok
        effect.setRadius(63);
        // try setting value greater than maximal
        effect.setRadius(63.1f); 
        assertEquals(63.1f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(63f, ((com.sun.scenario.effect.GaussianBlur)effect.impl_getImpl()).getRadius(), 1e-100);        
    }

    @Test
    public void testRadiusSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.GaussianBlur", "radius",
                "com.sun.scenario.effect.GaussianBlur", "radius", 15);
    }

    @Test
    public void testInputSynced() throws Exception {
        BoxBlur blur = new BoxBlur();
        checkEffectPropertySynced(
                "javafx.scene.effect.GaussianBlur", "input",
                "com.sun.scenario.effect.GaussianBlur", "input",
                blur, (com.sun.scenario.effect.BoxBlur)blur.impl_getImpl());
    }

    @Test
    public void testCreateWithParams() {
        effect = new GaussianBlur(4);
        setupTest(effect);
        assertEquals(4, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(4f, ((com.sun.scenario.effect.GaussianBlur) effect.impl_getImpl()).getRadius(), 1e-100);
    }

    @Test
    public void testCreateWithDefaultParams() {
        effect = new GaussianBlur(10);
        setupTest(effect);
        assertEquals(10, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(10f, ((com.sun.scenario.effect.GaussianBlur) effect.impl_getImpl()).getRadius(), 1e-100);
    }
}
