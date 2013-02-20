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

import java.lang.reflect.Method;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.junit.Before;
import org.junit.Test;

public class FloatMapTest extends EffectsTestBase {
    private FloatMap floatMap;
    private DisplacementMap displacementMap;

    @Before
    public void setUp() {
        floatMap = new FloatMap();
        displacementMap = new DisplacementMap();
        displacementMap.setMapData(floatMap);
        setupTest(displacementMap);
    }

    @Test
    public void testSetWidth() {
        // try setting correct value
        floatMap.setWidth(2);
        assertEquals(2, floatMap.getWidth());
        pulse();
        assertEquals(2, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getWidth());
    }

    @Test
    public void testDefaultWidth() {
        // default value should be 1
        assertEquals(1, floatMap.getWidth());
        assertEquals(1, floatMap.widthProperty().get());
        pulse();
        assertEquals(1, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getWidth());
    }

    @Test
    public void testMinWidth() {
        // 1 should be ok
        floatMap.setWidth(1);
        // try setting value smaller than minimal
        floatMap.setWidth(0);
        assertEquals(0, floatMap.getWidth());
        pulse();
        assertEquals(1, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getWidth());      
    }

    @Test
    public void testMaxWidth() {
        // 4096 should be ok
        floatMap.setWidth(4096);
        // try setting value greater than maximal
        floatMap.setWidth(4097);
        assertEquals(4097, floatMap.getWidth());
        pulse();
        assertEquals(4096, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getWidth());              
    }

    @Test
    public void testSetHeight() {
        // try setting correct value
        floatMap.setHeight(5);
        assertEquals(5, floatMap.getHeight());
        pulse();
        assertEquals(5, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getHeight());
    }

    @Test
    public void testDefaultHeight() {
        // default value should be 1
        assertEquals(1, floatMap.getHeight());
        assertEquals(1, floatMap.heightProperty().get());
        pulse();
        assertEquals(1, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getHeight());
    }

    @Test
    public void testMinHeight() {
        // 1 should be ok
        floatMap.setHeight(1);
        // try setting value smaller than minimal
        floatMap.setHeight(0);
        assertEquals(0, floatMap.getHeight());
        pulse();
        assertEquals(1, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getHeight());
    }

    @Test
    public void testMaxHeight() {
        // 4096 should be ok
        floatMap.setHeight(4096);
        // try setting value greater than maximal
        floatMap.setHeight(4097);
        assertEquals(4097, floatMap.getHeight());
        pulse();
        assertEquals(4096, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getHeight());
    }

    @Test
    public void testSetSamples3() {
        floatMap.setSamples(0, 0, 0.5f);
        pulse();
        com.sun.scenario.effect.FloatMap fm = floatMap.getImpl();
        float[] data = fm.getData();
        assertEquals(0.5f, data[0], 1e-100);
        assertEquals(0.0f, data[1], 1e-100);
        assertEquals(0.0f, data[2], 1e-100);
        assertEquals(0.0f, data[3], 1e-100);
    }

    @Test
    public void testSetSamples4() {
        floatMap.setSamples(0, 0, 0.5f, 0.4f);
        pulse();
        com.sun.scenario.effect.FloatMap fm = floatMap.getImpl();
        float[] data = fm.getData();
        assertEquals(0.5f, data[0], 1e-100);
        assertEquals(0.4f, data[1], 1e-100);
        assertEquals(0.0f, data[2], 1e-100);
        assertEquals(0.0f, data[3], 1e-100);        
    }

    @Test
    public void testSetSamples5() {
        floatMap.setSamples(0, 0, 0.5f, 0.4f, 0.3f);
        pulse();
        com.sun.scenario.effect.FloatMap fm = floatMap.getImpl();
        float[] data = fm.getData();
        assertEquals(0.5f, data[0], 1e-100);
        assertEquals(0.4f, data[1], 1e-100);
        assertEquals(0.3f, data[2], 1e-100);
        assertEquals(0.0f, data[3], 1e-100);        
    }
    
    @Test
    public void testSetSamples6() {
        floatMap.setSamples(0, 0, 0.5f, 0.4f, 0.3f, 0.2f);
        pulse();
        com.sun.scenario.effect.FloatMap fm = floatMap.getImpl();
        float[] data = fm.getData();
        assertEquals(0.5f, data[0], 1e-100);
        assertEquals(0.4f, data[1], 1e-100);
        assertEquals(0.3f, data[2], 1e-100);
        assertEquals(0.2f, data[3], 1e-100);      
    }
    
    @Test
    public void testSetSample() {
        floatMap.setSample(0, 0, 0, 0.5f);
        floatMap.setSample(0, 0, 1, 0.4f);
        floatMap.setSample(0, 0, 2, 0.3f);
        floatMap.setSample(0, 0, 3, 0.2f);
        pulse();
        com.sun.scenario.effect.FloatMap fm = floatMap.getImpl();
        float[] data = fm.getData();
        assertEquals(0.5f, data[0], 1e-100);
        assertEquals(0.4f, data[1], 1e-100);
        assertEquals(0.3f, data[2], 1e-100);
        assertEquals(0.2f, data[3], 1e-100);      
    }

    @Test
    public void testHeightSynced() throws Exception {
        checkIntPropertySynced(
                "javafx.scene.effect.FloatMap", "height",
                "com.sun.scenario.effect.FloatMap", "height", 10);
    }

    @Test
    public void testWidthSynced() throws Exception {
        checkIntPropertySynced(
                "javafx.scene.effect.FloatMap", "width",
                "com.sun.scenario.effect.FloatMap", "width", 10);
    }

    // special version of check method, because floatMap itself is not
    // an effect and its peer isn't created until after pulse happens
    @Override
    protected void checkIntPropertySynced(
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            int expected)
            throws Exception {
        final Class effectClass = Class.forName(effectName);
        final Class pgEffectClass = Class.forName(pgEffectName);

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();
        final Method pgGetter = pgEffectClass.getMethod(pgGetterName);

        IntegerProperty v = new SimpleIntegerProperty();
        Method m = effectClass.getMethod(propertyName + "Property", new Class[] {});
        ((IntegerProperty)m.invoke(floatMap)).bind(v);

        pulse(); // make sure that the dirty flag is cleaned before testing of binding
        v.set(expected);
        pulse();
        assertEquals(expected, ((Number)pgGetter.invoke(floatMap.getImpl())).intValue());
    }

    @Test
    public void testCreateWithParams() {
        floatMap = new FloatMap(2, 3);
        displacementMap = new DisplacementMap();
        displacementMap.setMapData(floatMap);
        setupTest(displacementMap);
        assertEquals(2, floatMap.getWidth(), 1e-100);
        assertEquals(3, floatMap.getHeight(), 1e-100);
        pulse();
        assertEquals(2, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getWidth());
        assertEquals(3, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getHeight());
    }
    
    @Test
    public void testCreateWithDefaultParams() {
        floatMap = new FloatMap(1, 1);
        displacementMap = new DisplacementMap();
        displacementMap.setMapData(floatMap);
        setupTest(displacementMap);
        assertEquals(1, floatMap.getWidth(), 1e-100);
        assertEquals(1, floatMap.getHeight(), 1e-100);
        pulse();
        assertEquals(1, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getWidth());
        assertEquals(1, ((com.sun.scenario.effect.FloatMap) floatMap.getImpl()).getHeight());
    }

}
