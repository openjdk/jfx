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
import org.junit.Test;

import com.sun.scenario.effect.Color4f;

public class PointLightTest extends LightTestBase {
    private Light.Point effect;

    @Before
    public void setUp() {
        effect = new Light.Point();
        setupTest(effect);
    }

    @Test
    public void testSetX() {
        // try setting correct value
        effect.setX(1.0f);
        assertEquals(1.0f, effect.getX(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getX(), 1e-100);
    }

    @Test
    public void testDefaultX() {
        // default value should be 0
        assertEquals(0f, effect.getX(), 1e-100);
        assertEquals(0f, effect.xProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getX(), 1e-100);
    }

    @Test
    public void testSetY() {
        // try setting correct value
        effect.setY(1.0f);
        assertEquals(1.0f, effect.getY(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getY(), 1e-100);
    }

    @Test
    public void testDefaultY() {
        // default value should be 0
        assertEquals(0f, effect.getY(), 1e-100);
        assertEquals(0f, effect.yProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getY(), 1e-100);
    }

    @Test
    public void testSetZ() {
        // try setting correct value
        effect.setZ(1.0f);
        assertEquals(1.0f, effect.getZ(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getZ(), 1e-100);
    }

    @Test
    public void testDefaultZ() {
        // default value should be 0
        assertEquals(0f, effect.getZ(), 1e-100);
        assertEquals(0f, effect.zProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getZ(), 1e-100);
    }

    @Test
    public void testXSynced() throws Exception {
        checkDoublePropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Point", "x",
                "com.sun.scenario.effect.light.PointLight", "x", 0.3);
    }

    @Test
    public void testYSynced() throws Exception {
        checkDoublePropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Point", "y",
                "com.sun.scenario.effect.light.PointLight", "y", 0.3);
    }

    @Test
    public void testZSynced() throws Exception {
        checkDoublePropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Point", "z",
                "com.sun.scenario.effect.light.PointLight", "z", 0.3);
    }

    @Test
    public void testColorSynced() throws Exception {
        Color color = Color.RED;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f result = (Color4f) getObjectPropertySynced(
                effect, effect.impl_getImpl(),
                "javafx.scene.effect.Light$Point", "color",
                "com.sun.scenario.effect.light.PointLight", "color",
                Color.RED);
        assertColor4fEquals(red, result);
    }

    @Test
    public void testCreateWithParams() {
        effect = new Light.Point(1, 2, 3, Color.RED);
        setupTest(effect);
        assertEquals(1, effect.getX(), 1e-100);
        assertEquals(2, effect.getY(), 1e-100);
        assertEquals(3, effect.getZ(), 1e-100);
        assertEquals(Color.RED, effect.getColor());
        pulse();        
        assertEquals(1.0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getX(), 1e-100);
        assertEquals(2.0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getY(), 1e-100);
        assertEquals(3.0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getZ(), 1e-100);
        Color4f c = ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getColor();
        assertEquals(1f, c.getRed(), 1e-5);
        assertEquals(0f, c.getGreen(), 1e-5);
        assertEquals(0f, c.getBlue(), 1e-5);
        assertEquals(1f, c.getAlpha(), 1e-5);
    }

    @Test
    public void testCreateWithDefaultParams() {
        effect = new Light.Point(0, 0, 0, Color.WHITE);
        setupTest(effect);
        assertEquals(0, effect.getX(), 1e-100);
        assertEquals(0, effect.getY(), 1e-100);
        assertEquals(0, effect.getZ(), 1e-100);
        assertEquals(Color.WHITE, effect.getColor());
        pulse();        
        assertEquals(0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getX(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getY(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getZ(), 1e-100);
        Color4f c = ((com.sun.scenario.effect.light.PointLight) effect.impl_getImpl()).getColor();
        assertEquals(1f, c.getRed(), 1e-5);
        assertEquals(1f, c.getGreen(), 1e-5);
        assertEquals(1f, c.getBlue(), 1e-5);
        assertEquals(1f, c.getAlpha(), 1e-5);
    }
}
