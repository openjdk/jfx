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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class DisplacementMapTest extends EffectsTestBase {
    private DisplacementMap effect;

    @Before
    public void setUp() {
        effect = new DisplacementMap();
        setupTest(effect);
    }

    @Test
    public void testSetOffsetX() {
        // try setting correct value
        effect.setOffsetX(1.0f);
        assertEquals(1.0f, effect.getOffsetX(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getOffsetX(), 1e-100);
    }

    @Test
    public void testDefaultOffsetX() {
        // default value should be 0
        assertEquals(0f, effect.getOffsetX(), 1e-100);
        assertEquals(0f, effect.offsetXProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getOffsetX(), 1e-100);
    }

    @Test
    public void testSetOffsetY() {
        // try setting correct value
        effect.setOffsetY(1.0f);
        assertEquals(1.0f, effect.getOffsetY(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getOffsetY(), 1e-100);
    }

    @Test
    public void testDefaultOffsetY() {
        // default value should be 0
        assertEquals(0f, effect.getOffsetY(), 1e-100);
        assertEquals(0f, effect.offsetYProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getOffsetY(), 1e-100);
    }

    @Test
    public void testSetScaleX() {
        // try setting correct value
        effect.setScaleX(1.1f);
        assertEquals(1.1f, effect.getScaleX(), 1e-100);
        pulse();
        assertEquals(1.1f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getScaleX(), 1e-100);
    }

    @Test
    public void testDefaultScaleX() {
        // default value should be 1
        assertEquals(1f, effect.getScaleX(), 1e-100);
        assertEquals(1f, effect.scaleXProperty().get(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getScaleX(), 1e-100);
    }

    @Test
    public void testSetScaleY() {
        // try setting correct value
        effect.setScaleY(1.1f);
        assertEquals(1.1f, effect.getScaleY(), 1e-100);
        pulse();
        assertEquals(1.1f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getScaleY(), 1e-100);
    }

    @Test
    public void testDefaultScaleY() {
        // default value should be 1
        assertEquals(1f, effect.getScaleY(), 1e-100);
        assertEquals(1f, effect.scaleYProperty().get(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getScaleY(), 1e-100);
    }

    @Test
    public void testSetWrap() {
        // try setting correct value
        effect.setWrap(true);
        assertTrue(effect.isWrap());
        pulse();
        assertTrue(((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getWrap());
    }

    @Test
    public void testDefaultWrap() {
        // default value should be false
        assertFalse(effect.isWrap());
        assertFalse(effect.wrapProperty().get());
        pulse();
        assertFalse(((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getWrap());
    }

    private FloatMap createFloatMap(int width, int height) {
        FloatMap map = new FloatMap();
        map.setWidth(width);
        map.setHeight(height);

        for (int i = 0; i < width; i++) {
            double v = 1.0;
            for (int j = 0; j < height; j++) {
                map.setSamples(i, j, 0.0f, (float) v);
            }
        }

        return map;
    }
    
    @Test
    public void testSetMap() {
        int w = 100;
        int h = 50;

        FloatMap map = createFloatMap(w, h);
        map.setWidth(w);
        map.setHeight(h);

        effect.setMapData(map);
        
        assertEquals(w, map.getWidth());
        assertEquals(h, map.getHeight());
        pulse();
        com.sun.scenario.effect.FloatMap mapImpl =
                ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getMapData();
        assertNotNull(mapImpl);
        assertEquals(w, mapImpl.getWidth());
        assertEquals(h, mapImpl.getHeight());      
    }

    @Test
    public void testDefaultMap() {
        // Default is empty map
        FloatMap map = effect.getMapData();
        assertNotNull(map);
        assertEquals(1, map.getWidth());
        assertEquals(1, map.getHeight());
        pulse();
        com.sun.scenario.effect.FloatMap mapImpl =
                ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getMapData();
        assertNotNull(mapImpl);
        assertEquals(1, mapImpl.getWidth());
        assertEquals(1, mapImpl.getHeight());

        assertEquals(map, effect.mapDataProperty().get());
    }

    @Test
    public void testDefaultMapNotChangedByThisEffect() {
        // Default is empty map
        FloatMap map = effect.getMapData();
        map.setHeight(100);
        map.setWidth(200);

        effect.setMapData(null);

        // null map data should default to empty map
        assertNull(effect.getMapData());
        pulse();
        com.sun.scenario.effect.FloatMap mapImpl =
                ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getMapData();
        assertNotNull(mapImpl);
        assertEquals(1, mapImpl.getWidth());
        assertEquals(1, mapImpl.getHeight());
    }

    @Test
    public void testOffsetXSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.DisplacementMap", "offsetX",
                "com.sun.scenario.effect.DisplacementMap", "offsetX", 10);
    }

    @Test
    public void testOffsetYSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.DisplacementMap", "offsetY",
                "com.sun.scenario.effect.DisplacementMap", "offsetY", 10);
    }

    @Test
    public void testScaleXSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.DisplacementMap", "scaleX",
                "com.sun.scenario.effect.DisplacementMap", "scaleX", 10);
    }

    @Test
    public void testScaleYSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.DisplacementMap", "scaleY",
                "com.sun.scenario.effect.DisplacementMap", "scaleY", 10);
    }

    @Test
    public void testWrapSynced() throws Exception {
        checkBooleanPropertySynced(
                "javafx.scene.effect.DisplacementMap", "wrap",
                "com.sun.scenario.effect.DisplacementMap", "wrap", true);
    }

    @Test
    public void testInputSynced() throws Exception {
        BoxBlur blur = new BoxBlur();
        checkEffectPropertySynced(
                "javafx.scene.effect.DisplacementMap", "input",
                "com.sun.scenario.effect.DisplacementMap", "contentInput",
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
        int w = 100;
        int h = 50;

        FloatMap map = createFloatMap(w, h);
        effect = new DisplacementMap(map);
        setupTest(effect);
        assertEquals(map, effect.getMapData());
        pulse();
        com.sun.scenario.effect.FloatMap mapImpl =
                ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getMapData();
        assertNotNull(mapImpl);
        assertEquals(w, mapImpl.getWidth());
        assertEquals(h, mapImpl.getHeight());
    }

    @Test
    public void testCreateWithParams5() {
        int w = 100;
        int h = 50;

        FloatMap map = createFloatMap(w, h);
        effect = new DisplacementMap(map, 1, 2, 3, 4);
        setupTest(effect);
        assertEquals(1, effect.getOffsetX(), 1e-100);
        assertEquals(2, effect.getOffsetY(), 1e-100);
        assertEquals(3, effect.getScaleX(), 1e-100);
        assertEquals(4, effect.getScaleY(), 1e-100);
        assertEquals(map, effect.getMapData());
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getOffsetX(), 1e-100);
        assertEquals(2f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getOffsetY(), 1e-100);
        assertEquals(3f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getScaleX(), 1e-100);
        assertEquals(4f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getScaleY(), 1e-100);
        com.sun.scenario.effect.FloatMap mapImpl =
                ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getMapData();
        assertNotNull(mapImpl);
        assertEquals(w, mapImpl.getWidth());
        assertEquals(h, mapImpl.getHeight());
    }

    @Test
    public void testCreateWithDefaultParams() {
        int w = 1;
        int h = 1;

        FloatMap map = createFloatMap(w, h);
        effect = new DisplacementMap(map);
        setupTest(effect);
        assertEquals(map, effect.getMapData());
        pulse();
        com.sun.scenario.effect.FloatMap mapImpl =
                ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getMapData();
        assertNotNull(mapImpl);
        assertEquals(w, mapImpl.getWidth());
        assertEquals(h, mapImpl.getHeight());
    }

    @Test
    public void testCreateWithDefaultParams5() {
        int w = 1;
        int h = 1;

        FloatMap map = createFloatMap(w, h);
        effect = new DisplacementMap(map, 0, 0, 1, 1);
        setupTest(effect);
        assertEquals(0, effect.getOffsetX(), 1e-100);
        assertEquals(0, effect.getOffsetY(), 1e-100);
        assertEquals(1, effect.getScaleX(), 1e-100);
        assertEquals(1, effect.getScaleY(), 1e-100);
        assertEquals(map, effect.getMapData());
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getOffsetX(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getOffsetY(), 1e-100);
        assertEquals(1f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getScaleX(), 1e-100);
        assertEquals(1f, ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getScaleY(), 1e-100);
        com.sun.scenario.effect.FloatMap mapImpl =
                ((com.sun.scenario.effect.DisplacementMap) effect.impl_getImpl()).getMapData();
        assertNotNull(mapImpl);
        assertEquals(w, mapImpl.getWidth());
        assertEquals(h, mapImpl.getHeight());
    }
}
