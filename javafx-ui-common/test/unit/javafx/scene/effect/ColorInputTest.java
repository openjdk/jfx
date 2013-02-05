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

import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.tk.Toolkit;

public class ColorInputTest extends EffectsTestBase {
    private ColorInput effect;

    @Before
    public void setUp() {
        effect = new ColorInput();
        setupTest(effect);
    }

    @Test
    public void testSetX() {
        // try setting correct value
        effect.setX(1.0f);
        assertEquals(1.0f, effect.getX(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getMinX(), 1e-100);
    }

    @Test
    public void testDefaultX() {
        // default value should be 0
        assertEquals(0, effect.getX(), 1e-100);
        assertEquals(0, effect.xProperty().get(), 1e-100);
        pulse();
        assertEquals(0, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getMinX(), 1e-100);
    }

    @Test
    public void testSetY() {
        // try setting correct value
        effect.setY(1.0f);
        assertEquals(1.0f, effect.getY(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getMinY(), 1e-100);
    }

    @Test
    public void testDefaultY() {
        // default value should be 0
        assertEquals(0, effect.getY(), 1e-100);
        assertEquals(0, effect.yProperty().get(), 1e-100);
        pulse();
        assertEquals(0, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getMinY(), 1e-100);
    }

    @Test
    public void testSetWidth() {
        // try setting correct value
        effect.setWidth(1.0f);
        assertEquals(1.0f, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getWidth(), 1e-100);
    }

    @Test
    public void testDefaultWidth() {
        // default value should be 0
        assertEquals(0, effect.getWidth(), 1e-100);
        assertEquals(0, effect.widthProperty().get(), 1e-100);
        pulse();
        assertEquals(0, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getWidth(), 1e-100);
    }

    @Test
    public void testSetHeight() {
        // try setting correct value
        effect.setHeight(1.0f);
        assertEquals(1.0f, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getHeight(), 1e-100);
    }

    @Test
    public void testDefaultHeight() {
        // default value should be 0
        assertEquals(0, effect.getHeight(), 1e-100);
        assertEquals(0, effect.heightProperty().get(), 1e-100);
        pulse();
        assertEquals(0, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getHeight(), 1e-100);
    }
     
    @Test
    public void testSetPaint() {
        // try setting correct value
        effect.setPaint(Color.BLUE);
        assertEquals(Color.BLUE, effect.getPaint());
        pulse();
        assertEquals(Toolkit.getPaintAccessor().getPlatformPaint(Color.BLUE), ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getPaint());
    }
    
    @Test
    public void testDefaultPaint() {
        // default value should be RED
        assertEquals(Color.RED, effect.getPaint());
        assertEquals(Color.RED, effect.paintProperty().get());
        pulse();
        assertEquals(Toolkit.getPaintAccessor().getPlatformPaint(Color.RED), ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getPaint());
    }

    @Test
    public void testHeightSynced() throws Exception {
        RectBounds floodBounds = (RectBounds) getDoublePropertySynced(
                "javafx.scene.effect.ColorInput", "height",
                "com.sun.scenario.effect.Flood", "floodBounds", 10);
        assertEquals(10, floodBounds.getHeight(), 1e-100);
    }

    @Test
    public void testWidthSynced() throws Exception {
         RectBounds floodBounds = (RectBounds)  getDoublePropertySynced(
                "javafx.scene.effect.ColorInput", "width",
                "com.sun.scenario.effect.Flood", "floodBounds", 10);
         assertEquals(10, floodBounds.getWidth(), 1e-100);
    }

    @Test
    public void testXSynced() throws Exception {
         RectBounds floodBounds = (RectBounds)  getDoublePropertySynced(
                "javafx.scene.effect.ColorInput", "x",
                "com.sun.scenario.effect.Flood", "floodBounds", 10);
         assertEquals(10, floodBounds.getMinX(), 1e-100);
    }

    @Test
    public void testYSynced() throws Exception {
         RectBounds floodBounds = (RectBounds)  getDoublePropertySynced(
                "javafx.scene.effect.ColorInput", "y",
                "com.sun.scenario.effect.Flood", "floodBounds", 10);
         assertEquals(10, floodBounds.getMinY(), 1e-100);
    }

    @Test
    public void testPaintSynced() throws Exception {
        Object paint = getObjectPropertySynced(
                "javafx.scene.effect.ColorInput", "paint",
                "com.sun.scenario.effect.Flood", "paint",
                Color.RED);
        assertEquals(Toolkit.getPaintAccessor().getPlatformPaint(Color.RED), paint);
    }

    @Test
    public void testCreateWithParams() {
        effect = new ColorInput(1, 2, 3, 4, Color.BLUE);
        setupTest(effect);
        assertEquals(1, effect.getX(), 1e-100);
        assertEquals(2, effect.getY(), 1e-100);
        assertEquals(3, effect.getWidth(), 1e-100);
        assertEquals(4, effect.getHeight(), 1e-100);
        assertEquals(Color.BLUE, effect.getPaint());
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getMinX(), 1e-100);
        assertEquals(2f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getMinY(), 1e-100);
        assertEquals(3f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getWidth(), 1e-100);
        assertEquals(4f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getHeight(), 1e-100);
        assertEquals(Toolkit.getPaintAccessor().getPlatformPaint(Color.BLUE), ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getPaint());
    }
    
    @Test
    public void testCreateWithDefaultParams() {
        effect = new ColorInput(0, 0, 0, 0, Color.RED);
        setupTest(effect);
        assertEquals(0, effect.getX(), 1e-100);
        assertEquals(0, effect.getY(), 1e-100);
        assertEquals(0, effect.getWidth(), 1e-100);
        assertEquals(0, effect.getHeight(), 1e-100);
        assertEquals(Color.RED, effect.getPaint());
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getMinX(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getMinY(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getWidth(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getFloodBounds().getHeight(), 1e-100);
        assertEquals(Toolkit.getPaintAccessor().getPlatformPaint(Color.RED), ((com.sun.scenario.effect.Flood) effect.impl_getImpl()).getPaint());
    }
}