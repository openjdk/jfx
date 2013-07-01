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

public class ColorAdjustTest extends EffectsTestBase {
    private ColorAdjust effect;

    @Before
    public void setUp() {
        effect = new ColorAdjust();
        setupTest(effect);
    }

    @Test
    public void testSetBrightness() {
        // try setting correct value
        effect.setBrightness(1.0f);
        assertEquals(1.0f, effect.getBrightness(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getBrightness(), 1e-100);
    }
    
    @Test
    public void testDefaultBrightness() {
        // default value should be 0
        assertEquals(0f, effect.getBrightness(), 1e-100);
        assertEquals(0f, effect.brightnessProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getBrightness(), 1e-100);
    }
    
    @Test
    public void testMinBrightness() {
        // -1 should be ok
        effect.setBrightness(-1);
        // try setting value smaller than minimal
        effect.setBrightness(-1.1f);
        assertEquals(-1.1f, effect.getBrightness(), 1e-100);
        pulse();
        assertEquals(-1f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getBrightness(), 1e-100);
    }

    @Test
    public void testMaxBrightness() {
        // 1 should be ok
        effect.setBrightness(1);
        // try setting value greater than maximal
        effect.setBrightness(1.1f); 
        assertEquals(1.1f, effect.getBrightness(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getBrightness(), 1e-100);
    }
        
    @Test
    public void testSetContrast() {
        // try setting correct value
        effect.setContrast(0.5);
        assertEquals(0.5, effect.getContrast(), 1e-100);
        pulse();
        assertEquals(0.5, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getContrast(), 1e-100);
    }
    
    @Test
    public void testDefaultContrast() {
        // default value should be 0
        assertEquals(0.0, effect.getContrast(), 1e-100);
        assertEquals(0.0, effect.contrastProperty().get(), 1e-100);
        pulse();
        assertEquals(0.0, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getContrast(), 1e-100);
    }
    
    @Test
    public void testMinContrast() {
        // -1 should be ok
        effect.setContrast(-1.0);
        // try setting value smaller than minimal
        effect.setContrast(-1.1);
        assertEquals(-1.1, effect.getContrast(), 1e-100);
        pulse();
        assertEquals(-1.0, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getContrast(), 1e-100);
    }

    @Test
    public void testMaxContrast() {
        // 1 should be ok
        effect.setContrast(1.0);
        // try setting value greater than maximal
        effect.setContrast(1.1);
        assertEquals(1.1, effect.getContrast(), 1e-100);
        pulse();
        assertEquals(1.0, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getContrast(), 1e-100);
    }
        
    @Test
    public void testSetHue() {
        // try setting correct value
        effect.setHue(1.0f);
        assertEquals(1.0f, effect.getHue(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getHue(), 1e-100);
    }
    
    @Test
    public void testDefaultHue() {
        // default value should be 0
        assertEquals(0f, effect.getHue(), 1e-100);
        assertEquals(0f, effect.hueProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getHue(), 1e-100);
    }
    
    @Test
    public void testMinHue() {
        // -1 should be ok
        effect.setHue(-1);
        // try setting value smaller than minimal
        effect.setHue(-1.1f);
        assertEquals(-1.1f, effect.getHue(), 1e-100);
        pulse();
        assertEquals(-1f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getHue(), 1e-100);
    }

    @Test
    public void testMaxHue() {
        // 1 should be ok
        effect.setHue(1);
        // try setting value greater than maximal
        effect.setHue(1.1f); 
        assertEquals(1.1f, effect.getHue(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getHue(), 1e-100);
    }
        
    @Test
    public void testSetSaturation() {
        // try setting correct value
        effect.setSaturation(1.0f);
        assertEquals(1.0f, effect.getSaturation(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getSaturation(), 1e-100);
    }
    
    @Test
    public void testDefaultSaturation() {
        // default value should be 0
        assertEquals(0f, effect.getSaturation(), 1e-100);
        assertEquals(0f, effect.saturationProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getSaturation(), 1e-100);
    }
    
    @Test
    public void testMinSaturation() {
        // -1 should be ok
        effect.setSaturation(-1);
        // try setting value smaller than minimal
        effect.setSaturation(-1.1f);
        assertEquals(-1.1f, effect.getSaturation(), 1e-100);
        pulse();
        assertEquals(-1f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getSaturation(), 1e-100);
    }

    @Test
    public void testMaxSaturation() {
        // 1 should be ok
        effect.setSaturation(1);
        // try setting value greater than maximal
        effect.setSaturation(1.1f); 
        assertEquals(1.1f, effect.getSaturation(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.ColorAdjust)effect.impl_getImpl()).getSaturation(), 1e-100);
    }

    @Test
    public void testBrightnessSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.ColorAdjust", "brightness",
                "com.sun.scenario.effect.ColorAdjust", "brightness", 0.3);
    }

    @Test
    public void testContrastSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.ColorAdjust", "contrast",
                "com.sun.scenario.effect.ColorAdjust", "contrast", 0.3);
    }

    @Test
    public void testHueSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.ColorAdjust", "hue",
                "com.sun.scenario.effect.ColorAdjust", "hue", 0.3);
    }

    @Test
    public void testSaturationSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.ColorAdjust", "saturation",
                "com.sun.scenario.effect.ColorAdjust", "saturation", 0.3);
    }

    @Test
    public void testInputSynced() throws Exception {
        BoxBlur blur = new BoxBlur();
        checkEffectPropertySynced(
                "javafx.scene.effect.ColorAdjust", "input",
                "com.sun.scenario.effect.ColorAdjust", "input",
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
        effect = new ColorAdjust(-.5, 0.5, -0.1, 0.1);
        setupTest(effect);
        assertEquals(-0.5, effect.getHue(), 1e-100);
        assertEquals(0.5, effect.getSaturation(), 1e-100);
        assertEquals(-0.1, effect.getBrightness(), 1e-100);
        assertEquals(0.1, effect.getContrast(), 1e-100);
        pulse();
        assertEquals(-0.5f, ((com.sun.scenario.effect.ColorAdjust) effect.impl_getImpl()).getHue(), 1e-100);
        assertEquals(0.5f, ((com.sun.scenario.effect.ColorAdjust) effect.impl_getImpl()).getSaturation(), 1e-100);
        assertEquals(-0.1f, ((com.sun.scenario.effect.ColorAdjust) effect.impl_getImpl()).getBrightness(), 1e-100);
        assertEquals(0.1f, ((com.sun.scenario.effect.ColorAdjust) effect.impl_getImpl()).getContrast(), 1e-100);
    }
    
    @Test
    public void testCreateWithDefaultParams() {
        effect = new ColorAdjust(0, 0, 0, 0);
        setupTest(effect);
        assertEquals(0, effect.getHue(), 1e-100);
        assertEquals(0, effect.getSaturation(), 1e-100);
        assertEquals(0, effect.getBrightness(), 1e-100);
        assertEquals(0, effect.getContrast(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.ColorAdjust) effect.impl_getImpl()).getHue(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.ColorAdjust) effect.impl_getImpl()).getSaturation(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.ColorAdjust) effect.impl_getImpl()).getBrightness(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.ColorAdjust) effect.impl_getImpl()).getContrast(), 1e-100);
    }
}
