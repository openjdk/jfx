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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BoxBlurTest extends EffectsTestBase {
    private BoxBlur effect;

    @Before
    public void setUp() {
        effect = new BoxBlur();
        setupTest(effect);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSetWidth() {
        // try setting correct value
        effect.setWidth(1.0f);
        assertEquals(1.0f, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(1, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getHorizontalSize());
    }
    
    @Test
    public void testDefaultWidth() {
        // default value should be 5
        assertEquals(5.0f, effect.getWidth(), 1e-100);
        assertEquals(5.0f, effect.widthProperty().get(), 1e-100);
        pulse();
        assertEquals(5, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getHorizontalSize());
    }
    
    @Test
    public void testMinWidth() {
        // 0 should be ok
        effect.setWidth(0);
        // try setting value smaller than minimal
        effect.setWidth(-0.1f);
        assertEquals(-0.1f, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(0, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getHorizontalSize());
    }

    @Test
    public void testMaxWidth() {
        // 255 should be ok
        effect.setWidth(255);
        // try setting value greater than maximal
        effect.setWidth(255.1f); 
        assertEquals(255.1f, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(255, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getHorizontalSize());
    }
    
    @Test
    public void testSetHeight() {
        // try setting correct value
        effect.setHeight(1.0f);
        assertEquals(1.0f, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(1, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getVerticalSize());
    }
    
    @Test
    public void testDefaultHeight() {
        // default value should be 5
        assertEquals(5.0f, effect.getHeight(), 1e-100);
        assertEquals(5.0f, effect.heightProperty().get(), 1e-100);
        pulse();
        assertEquals(5, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getVerticalSize());
    }
    
    @Test
    public void testMinHeight() {
        // 0 should be ok
        effect.setHeight(0);
        // try setting value smaller than minimal
        effect.setHeight(-0.1f);
        assertEquals(-0.1f, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(0, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getVerticalSize());
    }

    @Test
    public void testMaxHeight() {
        // 255 should be ok
        effect.setHeight(255);
        // try setting value greater than maximal
        effect.setHeight(255.1f); 
        assertEquals(255.1f, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(255, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getVerticalSize());
    }
    
    @Test
    public void testSetIterations() {
        // try setting correct value
        effect.setIterations(2);
        assertEquals(2, effect.getIterations());
        pulse();
        assertEquals(2, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getPasses());
    }
    
    @Test
    public void testDefaultIterations() {
        // default value should be 1
        assertEquals(1, effect.getIterations());
        assertEquals(1, effect.iterationsProperty().get());
        pulse();
        assertEquals(1, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getPasses());
    }
    
    @Test
    public void testMinIterations() {
        // 0 should be ok
        effect.setIterations(0);
        // try setting value smaller than minimal
        effect.setIterations(-1);
        assertEquals(-1, effect.getIterations());
        pulse();
        assertEquals(0, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getPasses());
    }

    @Test
    public void testMaxIterations() {
        // 3 should be ok
        effect.setIterations(3);
        // try setting value greater than maximal
        effect.setIterations(4); 
        assertEquals(4, effect.getIterations());
        pulse();
        assertEquals(3, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getPasses());
    }

    @Test
    public void testCreateWithParams() {
        effect = new BoxBlur(1, 1, 3);
        setupTest(effect);
        assertEquals(1, effect.getWidth(), 1e-100);
        assertEquals(1, effect.getHeight(), 1e-100);
        assertEquals(3, effect.getIterations());
        pulse();
        assertEquals(1, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getHorizontalSize());
        assertEquals(1, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getVerticalSize());
        assertEquals(3, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getPasses());
    }

    @Test
    public void testCreateWithDefaultParams() {
        effect = new BoxBlur(5, 5, 1);
        setupTest(effect);
        assertEquals(5, effect.getWidth(), 1e-100);
        assertEquals(5, effect.getHeight(), 1e-100);
        assertEquals(1, effect.getIterations());
        pulse();
        assertEquals(5, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getHorizontalSize());
        assertEquals(5, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getVerticalSize());
        assertEquals(1, ((com.sun.scenario.effect.BoxBlur)effect.impl_getImpl()).getPasses());
    }

    @Test
    public void testHeightSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.BoxBlur", "height",
                "com.sun.scenario.effect.BoxBlur", "verticalSize", 10);
    }

    @Test
    public void testWidthSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.BoxBlur", "width",
                "com.sun.scenario.effect.BoxBlur", "horizontalSize", 10);
    }

    @Test
    public void testIterationsSynced() throws Exception {
        checkIntPropertySynced(
                "javafx.scene.effect.BoxBlur", "iterations",
                "com.sun.scenario.effect.BoxBlur", "passes", 2);
    }

    @Test
    public void testInputSynced() throws Exception {
        BoxBlur blur = new BoxBlur();
        checkEffectPropertySynced(
                "javafx.scene.effect.BoxBlur", "input",
                "com.sun.scenario.effect.BoxBlur", "input",
                blur, (com.sun.scenario.effect.BoxBlur)blur.impl_getImpl());
    }
}
