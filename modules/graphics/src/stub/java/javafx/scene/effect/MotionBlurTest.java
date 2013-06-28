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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class MotionBlurTest extends EffectsTestBase {
    private MotionBlur effect;

    @Before
    public void setUp() {
        effect = new MotionBlur();
        setupTest(effect);
    }

    @Test
    public void testSetAngle() {
        // try setting correct value
        effect.setAngle(1.5f);
        assertEquals(1.5f, effect.getAngle(), 1e-100);
        pulse();
        assertEquals(1.5f, Math.toDegrees(((com.sun.scenario.effect.MotionBlur)effect.impl_getImpl()).getAngle()), 1e-5);
    }
    
    @Test
    public void testDefaultAngle() {
        // default value should be 0
        assertEquals(0f, effect.getAngle(), 1e-100);
        assertEquals(0f, effect.angleProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.MotionBlur)effect.impl_getImpl()).getAngle(), 1e-100);
    }
    
    @Test
    public void testSetRadius() {
        // try setting correct value
        effect.setRadius(0.5f);
        assertEquals(0.5f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(0.5f, ((com.sun.scenario.effect.MotionBlur)effect.impl_getImpl()).getRadius(), 1e-100);
    }
    
    @Test
    public void testDefaultRadius() {
        // default value should be 10
        assertEquals(10f, effect.getRadius(), 1e-100);
        assertEquals(10f, effect.radiusProperty().get(), 1e-100);
        pulse();
        assertEquals(10f, ((com.sun.scenario.effect.MotionBlur)effect.impl_getImpl()).getRadius(), 1e-100);
    }
    
    @Test
    public void testMinRadius() {
        // 0 should be ok
        effect.setRadius(0);
        // try setting value smaller than minimal
        effect.setRadius(-0.1f);
        assertEquals(-0.1f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.MotionBlur)effect.impl_getImpl()).getRadius(), 1e-100);        
    }
    
    @Test
    public void testMaxRadius() {
        // 63 should be ok
        effect.setRadius(63);
        // try setting value greater than maximal
        effect.setRadius(63.1f); 
        assertEquals(63.1f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(63f, ((com.sun.scenario.effect.MotionBlur)effect.impl_getImpl()).getRadius(), 1e-100);        
    }

    @Test
    public void testAngleSynced() throws Exception {
        float result = (Float)getDoublePropertySynced(
                "javafx.scene.effect.MotionBlur", "angle",
                "com.sun.scenario.effect.MotionBlur", "angle", 10);
        assertEquals(10, Math.toDegrees(result), 1e-5);
    }

    @Test
    public void testRadiusSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.MotionBlur", "radius",
                "com.sun.scenario.effect.MotionBlur", "radius", 15);
    }

    @Test
    public void testInputSynced() throws Exception {
        BoxBlur blur = new BoxBlur();
        checkEffectPropertySynced(
                "javafx.scene.effect.MotionBlur", "input",
                "com.sun.scenario.effect.MotionBlur", "input",
                blur, (com.sun.scenario.effect.BoxBlur)blur.impl_getImpl());
    }
    
    @Test
    public void testCreateWithParams() {
        effect = new MotionBlur(1, 2);
        setupTest(effect);
        assertEquals(1, effect.getAngle(), 1e-100);
        assertEquals(2, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(1.0f, Math.toDegrees(((com.sun.scenario.effect.MotionBlur) effect.impl_getImpl()).getAngle()), 1e-5);
        assertEquals(2.0f, ((com.sun.scenario.effect.MotionBlur) effect.impl_getImpl()).getRadius(), 1e-100);
    }

    @Test
    public void testCreateWithDefaultParams() {
        effect = new MotionBlur(0, 10);
        setupTest(effect);
        assertEquals(0, effect.getAngle(), 1e-100);
        assertEquals(10, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(0f, Math.toDegrees(((com.sun.scenario.effect.MotionBlur) effect.impl_getImpl()).getAngle()), 1e-5);
        assertEquals(10f, ((com.sun.scenario.effect.MotionBlur) effect.impl_getImpl()).getRadius(), 1e-100);
    }
}
