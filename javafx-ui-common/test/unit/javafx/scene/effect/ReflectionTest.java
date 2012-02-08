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

public class ReflectionTest extends EffectsTestBase {
    private Reflection effect;

    @Before
    public void setUp() {
        effect = new Reflection();
        setupTest(effect);
    }

    @Test
    public void testSetTopOpacity() {
        // try setting correct value
        effect.setTopOpacity(1.0f);
        assertEquals(1.0f, effect.getTopOpacity(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getTopOpacity(), 1e-100);
    }
    
    @Test
    public void testDefaultTopOpacity() {
        // default value should be 0.5
        assertEquals(0.5f, effect.getTopOpacity(), 1e-100);
        assertEquals(0.5f, effect.topOpacityProperty().get(), 1e-100);
        pulse();
        assertEquals(0.5f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getTopOpacity(), 1e-100);
    }
    
    @Test
    public void testMinTopOpacity() {
        // 0 should be ok
        effect.setTopOpacity(0);
        // try setting value smaller than minimal
        effect.setTopOpacity(-0.1f);
        assertEquals(-0.1f, effect.getTopOpacity(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getTopOpacity(), 1e-100);
    }

    @Test
    public void testMaxTopOpacity() {
        // 1 should be ok
        effect.setTopOpacity(1);
        // try setting value greater than maximal
        effect.setTopOpacity(1.1f); 
        assertEquals(1.1f, effect.getTopOpacity(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getTopOpacity(), 1e-100);
    }    
    
    @Test
    public void testSetBottomOpacity() {
        // try setting correct value
        effect.setBottomOpacity(1.0f);
        assertEquals(1.0f, effect.getBottomOpacity(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getBottomOpacity(), 1e-100);
    }
    
    @Test
    public void testDefaultBottomOpacity() {
        // default value should be 1
        assertEquals(0f, effect.getBottomOpacity(), 1e-100);
        assertEquals(0f, effect.bottomOpacityProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getBottomOpacity(), 1e-100);
    }
    
    @Test
    public void testMinBottomOpacity() {
        // 0 should be ok
        effect.setBottomOpacity(0);
        // try setting value smaller than minimal
        effect.setBottomOpacity(-0.1f);
        assertEquals(-0.1f, effect.getBottomOpacity(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getBottomOpacity(), 1e-100);       
    }

    @Test
    public void testMaxBottomOpacity() {
        // 1 should be ok
        effect.setBottomOpacity(1);
        // try setting value greater than maximal
        effect.setBottomOpacity(1.1f);
        assertEquals(1.1f, effect.getBottomOpacity(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getBottomOpacity(), 1e-100);        
    }    
    
    @Test
    public void testSetFraction() {
        // try setting correct value
        effect.setFraction(1.0f);
        assertEquals(1.0f, effect.getFraction(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getFraction(), 1e-100);
    }
    
    @Test
    public void testDefaultFraction() {
        // default value should be 0.75
        assertEquals(0.75f, effect.getFraction(), 1e-100);
        assertEquals(0.75f, effect.fractionProperty().get(), 1e-100);
        pulse();
        assertEquals(0.75f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getFraction(), 1e-100);
    }
    
    @Test
    public void testMinFraction() {
        // 0 should be ok
        effect.setFraction(0);
        // try setting value smaller than minimal
        effect.setFraction(-0.1f);
        assertEquals(-0.1f, effect.getFraction(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getFraction(), 1e-100);      
    }

    @Test
    public void testMaxFraction() {
        // 1 should be ok
        effect.setFraction(1);
        // try setting value greater than maximal
        effect.setFraction(1.1f);
        assertEquals(1.1f, effect.getFraction(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getFraction(), 1e-100);
    }
    
    @Test
    public void testSetTopOffset() {
        // try setting correct value
        effect.setTopOffset(1.0f);
        assertEquals(1.0f, effect.getTopOffset(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getTopOffset(), 1e-100);
    }
    
    @Test
    public void testDefaultTopOffset() {
        // default value should be 0
        assertEquals(0f, effect.getTopOffset(), 1e-100);
        assertEquals(0f, effect.topOffsetProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getTopOffset(), 1e-100);
    }

    @Test
    public void testBottomOpacitySynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.Reflection", "bottomOpacity",
                "com.sun.scenario.effect.Reflection", "bottomOpacity", 0.3);
    }

    @Test
    public void testTopOpacitySynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.Reflection", "topOpacity",
                "com.sun.scenario.effect.Reflection", "topOpacity", 0.3);
    }

    @Test
    public void testTopOffsetSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.Reflection", "topOffset",
                "com.sun.scenario.effect.Reflection", "topOffset", 15);
    }

    @Test
    public void testFractionSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.Reflection", "fraction",
                "com.sun.scenario.effect.Reflection", "fraction", 0.5);
    }

    @Test
    public void testInputSynced() throws Exception {
        BoxBlur blur = new BoxBlur();
        checkEffectPropertySynced(
                "javafx.scene.effect.Reflection", "input",
                "com.sun.scenario.effect.Reflection", "input",
                blur, (com.sun.scenario.effect.BoxBlur)blur.impl_getImpl());
    }

    @Test
    public void testCreateWithParams() {
        effect = new Reflection(1, 0.2, 0.4, 0.5);
        setupTest(effect);
        assertEquals(1, effect.getTopOffset(), 1e-100);
        assertEquals(0.2, effect.getFraction(), 1e-100);
        assertEquals(0.4, effect.getTopOpacity(), 1e-100);
        assertEquals(0.5, effect.getBottomOpacity(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getTopOffset(), 1e-100);
        assertEquals(0.2f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getFraction(), 1e-100);
        assertEquals(0.4f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getTopOpacity(), 1e-100);
        assertEquals(0.5f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getBottomOpacity(), 1e-100);
    }

    @Test
    public void testCreateWithDefaultParams() {
        effect = new Reflection(0, 0.75, 0.5, 1);
        setupTest(effect);
        assertEquals(0, effect.getTopOffset(), 1e-100);
        assertEquals(0.75, effect.getFraction(), 1e-100);
        assertEquals(0.5, effect.getTopOpacity(), 1e-100);
        assertEquals(1, effect.getBottomOpacity(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getTopOffset(), 1e-100);
        assertEquals(0.75f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getFraction(), 1e-100);
        assertEquals(0.5f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getTopOpacity(), 1e-100);
        assertEquals(1f, ((com.sun.scenario.effect.Reflection)effect.impl_getImpl()).getBottomOpacity(), 1e-100);
    }
}
