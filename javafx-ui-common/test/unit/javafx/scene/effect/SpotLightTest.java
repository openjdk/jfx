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
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sun.scenario.effect.Color4f;

public class SpotLightTest extends LightTestBase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private Light.Spot effect;

    @Before
    public void setUp() {
        effect = new Light.Spot();
        setupTest(effect);
    }

    @Test
    public void testSetPointsAtX() {
        // try setting correct value
        effect.setPointsAtX(1.0f);
        assertEquals(1.0f, effect.getPointsAtX(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getPointsAtX(), 1e-100);
    }

    @Test
    public void testDefaultPointsAtX() {
        // default value should be 0
        assertEquals(0f, effect.getPointsAtX(), 1e-100);
        assertEquals(0f, effect.pointsAtXProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getPointsAtX(), 1e-100);
    }

    @Test
    public void testSetPointsAtY() {
        // trPointsAtY setting correct value
        effect.setPointsAtY(1.0f);
        assertEquals(1.0f, effect.getPointsAtY(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getPointsAtY(), 1e-100);
    }

    @Test
    public void testDefaultPointsAtY() {
        // default value should be 0
        assertEquals(0f, effect.getPointsAtY(), 1e-100);
        assertEquals(0f, effect.pointsAtYProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getPointsAtY(), 1e-100);
    }

    @Test
    public void testSetPointsAtZ() {
        // try setting correct value
        effect.setPointsAtZ(1.0f);
        assertEquals(1.0f, effect.getPointsAtZ(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getPointsAtZ(), 1e-100);
    }

    @Test
    public void testDefaultPointsAtZ() {
        // default value should be 0
        assertEquals(0f, effect.getPointsAtZ(), 1e-100);
        assertEquals(0f, effect.pointsAtZProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getPointsAtZ(), 1e-100);
    }
    
    @Test
    public void testSetSpecularExponent() {
        // try setting correct value
        effect.setSpecularExponent(1.1f);
        assertEquals(1.1f, effect.getSpecularExponent(), 1e-100);
        pulse();
        assertEquals(1.1f, ((com.sun.scenario.effect.light.SpotLight)effect.impl_getImpl()).getSpecularExponent(), 1e-100);
    }
    
    @Test
    public void testDefaultSpecularExponent() {
        // default value should be 1
        assertEquals(1f, effect.getSpecularExponent(), 1e-100);
        assertEquals(1f, effect.specularExponentProperty().get(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.light.SpotLight)effect.impl_getImpl()).getSpecularExponent(), 1e-100);
    }
    
    @Test
    public void testMinSpecularExponent() {
        // 0 should be ok
        effect.setSpecularExponent(0);
        // try setting value smaller than minimal
        effect.setSpecularExponent(-0.1f);
        assertEquals(-0.1f, effect.getSpecularExponent(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.light.SpotLight)effect.impl_getImpl()).getSpecularExponent(), 1e-100);
    }

    @Test
    public void testMaxSpecularExponent() {
        // 4 should be ok
        effect.setSpecularExponent(4);
        // try setting value greater than maximal
        effect.setSpecularExponent(4.1f);
        assertEquals(4.1f, effect.getSpecularExponent(), 1e-100);
        pulse();
        assertEquals(4f, ((com.sun.scenario.effect.light.SpotLight)effect.impl_getImpl()).getSpecularExponent(), 1e-100);
    }

    @Test
    public void testPointsAtXSynced() throws Exception {
        checkDoublePropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Spot", "pointsAtX",
                "com.sun.scenario.effect.light.SpotLight", "pointsAtX", 0.3);
    }

    @Test
    public void testPointsAtYSynced() throws Exception {
        checkDoublePropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Spot", "pointsAtY",
                "com.sun.scenario.effect.light.SpotLight", "pointsAtY", 0.3);
    }

    @Test
    public void testSpecularExponentSynced() throws Exception {
        checkDoublePropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Spot", "specularExponent",
                "com.sun.scenario.effect.light.SpotLight", "specularExponent", 0.3);
    }

    @Test
    public void testPointsAtZSynced() throws Exception {
        checkDoublePropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Spot", "pointsAtZ",
                "com.sun.scenario.effect.light.SpotLight", "pointsAtZ", 0.3);
    }

    @Test
    public void testColorSynced() throws Exception {
        Color color = Color.RED;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f result = (Color4f) getObjectPropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Spot", "color",
                "com.sun.scenario.effect.light.SpotLight", "color",
                Color.RED);
        assertColor4fEquals(red, result);
    }

    @Test
    public void testCreateWithParams() {
        effect = new Light.Spot(1, 2, 3, 4, Color.RED);
        setupTest(effect);
        assertEquals(1, effect.getX(), 1e-100);
        assertEquals(2, effect.getY(), 1e-100);
        assertEquals(3, effect.getZ(), 1e-100);
        assertEquals(4, effect.getSpecularExponent(), 1e-100);
        assertEquals(Color.RED, effect.getColor());
        pulse();        
        assertEquals(1.0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getX(), 1e-100);
        assertEquals(2.0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getY(), 1e-100);
        assertEquals(3.0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getZ(), 1e-100);
        assertEquals(4.0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getSpecularExponent(), 1e-100);
        Color4f c = ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getColor();
        assertEquals(1f, c.getRed(), 1e-5);
        assertEquals(0f, c.getGreen(), 1e-5);
        assertEquals(0f, c.getBlue(), 1e-5);
        assertEquals(1f, c.getAlpha(), 1e-5);
    }

    @Test
    public void testCreateWithDefaultParams() {
        effect = new Light.Spot(0, 0, 0, 1, Color.WHITE);
        setupTest(effect);
        assertEquals(0, effect.getX(), 1e-100);
        assertEquals(0, effect.getY(), 1e-100);
        assertEquals(0, effect.getZ(), 1e-100);
        assertEquals(1, effect.getSpecularExponent(), 1e-100);
        assertEquals(Color.WHITE, effect.getColor());
        pulse();        
        assertEquals(0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getX(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getY(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getZ(), 1e-100);
        assertEquals(1.0f, ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getSpecularExponent(), 1e-100);
        Color4f c = ((com.sun.scenario.effect.light.SpotLight) effect.impl_getImpl()).getColor();
        assertEquals(1f, c.getRed(), 1e-5);
        assertEquals(1f, c.getGreen(), 1e-5);
        assertEquals(1f, c.getBlue(), 1e-5);
        assertEquals(1f, c.getAlpha(), 1e-5);
    }
}
