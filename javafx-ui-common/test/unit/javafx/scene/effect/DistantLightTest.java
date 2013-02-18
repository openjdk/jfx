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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Test;

import com.sun.scenario.effect.Color4f;

public class DistantLightTest extends LightTestBase {
    private Light.Distant effect;

    @Before
    public void setUp() {
        effect = new Light.Distant();
        setupTest(effect);
    }

    @Test
    public void testSetAzimuth() {
        // try setting correct value
        effect.setAzimuth(1.0f);
        assertEquals(1.0f, effect.getAzimuth(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getAzimuth(), 1e-100);
    }

    @Test
    public void testDefaultAzimuth() {
        // default value should be 45
        assertEquals(45f, effect.getAzimuth(), 1e-100);
        assertEquals(45f, effect.azimuthProperty().get(), 1e-100);
        pulse();
        assertEquals(45f, ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getAzimuth(), 1e-100);
    }

    @Test
    public void testSetElevation() {
        // try setting correct value
        effect.setElevation(1.0f);
        assertEquals(1.0f, effect.getElevation(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getElevation(), 1e-100);
    }

    @Test
    public void testDefaultElevation() {
        // default value should be 45
        assertEquals(45f, effect.getElevation(), 1e-100);
        assertEquals(45f, effect.elevationProperty().get(), 1e-100);
        pulse();
        assertEquals(45f, ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getElevation(), 1e-100);
    }

    @Test
    public void testSetColor() {
        // try setting correct value
        effect.setColor(Color.BLUE);
        assertEquals(Color.BLUE, effect.getColor());
        pulse();
        Color4f c = ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getColor();        
        assertEquals(0f, c.getRed(), 1e-5);
        assertEquals(0f, c.getGreen(), 1e-5);
        assertEquals(1f, c.getBlue(), 1e-5);
        assertEquals(1f, c.getAlpha(), 1e-5); 
    }

    @Test
    public void testDefaultColor() {
        // default value should be RED
        assertEquals(Color.WHITE, effect.getColor());
        assertEquals(Color.WHITE, effect.colorProperty().get());
        pulse();
        Color4f c = ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getColor();        
        assertEquals(1f, c.getRed(), 1e-5);
        assertEquals(1f, c.getGreen(), 1e-5);
        assertEquals(1f, c.getBlue(), 1e-5);
        assertEquals(1f, c.getAlpha(), 1e-5);      
    }
    
    @Test
    public void testNullColor() {
        // try setting null value
        effect.setColor(null);
        assertNull(effect.getColor());
        pulse();
        // null should not be propagated
        assertNotNull(((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getColor());
    }

    @Test
    public void testAzimuthSynced() throws Exception {
        checkDoublePropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Distant", "azimuth",
                "com.sun.scenario.effect.light.DistantLight", "azimuth", 0.3);
    }

    @Test
    public void testColorSynced() throws Exception {
        Color color = Color.RED;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f result = (Color4f) getObjectPropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Distant", "color",
                "com.sun.scenario.effect.light.DistantLight", "color",
                Color.RED);
        assertColor4fEquals(red, result);
    }

    @Test
    public void testCreateWithParams() {
        effect = new Light.Distant(1, 2, Color.BLUE);
        setupTest(effect);
        assertEquals(1, effect.getAzimuth(), 1e-100);
        assertEquals(2, effect.getElevation(), 1e-100);
        assertEquals(Color.BLUE, effect.getColor());
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getAzimuth(), 1e-100);
        assertEquals(2.0f, ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getElevation(), 1e-100);
        Color4f c = ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getColor();
        assertEquals(0f, c.getRed(), 1e-5);
        assertEquals(0f, c.getGreen(), 1e-5);
        assertEquals(1f, c.getBlue(), 1e-5);
        assertEquals(1f, c.getAlpha(), 1e-5);
    }
    
    @Test
    public void testCreateWithDefaultParams() {
        effect = new Light.Distant(45, 45, Color.RED);
        setupTest(effect);
        assertEquals(45, effect.getAzimuth(), 1e-100);
        assertEquals(45, effect.getElevation(), 1e-100);
        assertEquals(Color.RED, effect.getColor());
        pulse();
        assertEquals(45f, ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getAzimuth(), 1e-100);
        assertEquals(45f, ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getElevation(), 1e-100);
        Color4f c = ((com.sun.scenario.effect.light.DistantLight) effect.impl_getImpl()).getColor();
        assertEquals(1f, c.getRed(), 1e-5);
        assertEquals(0f, c.getGreen(), 1e-5);
        assertEquals(0f, c.getBlue(), 1e-5);
        assertEquals(1f, c.getAlpha(), 1e-5);
    }
}
