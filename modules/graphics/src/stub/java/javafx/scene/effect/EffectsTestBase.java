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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import com.sun.scenario.effect.Color4f;

public class EffectsTestBase {
    private Scene scene;
    private StubToolkit toolkit;
    private Stage stage;
    protected Node n;
    private Effect e;
    
    protected void setupTest(Effect effect) {
        e = effect;
        Group root = new Group();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
        toolkit = (StubToolkit) Toolkit.getToolkit(); 
        n = new Rectangle(100, 100);
        n.setEffect(effect);
        root.getChildren().add(n);
    }

    protected void setEffect(Effect effect) {
        n.setEffect(effect);
    }
    
    protected void pulse() {
        toolkit.fireTestPulse();
    }

    protected void checkDoublePropertySynced(
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            double expected)
            throws Exception {
       checkDoublePropertySynced(e, e.impl_getImpl(), effectName, propertyName, pgEffectName, pgPropertyName, expected);
    }

    protected void checkDoublePropertySynced(
            Object inputObject,
            Object pgObject,
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            double expected)
            throws Exception {
        final Class effectClass = Class.forName(effectName);
        final Class pgEffectClass = Class.forName(pgEffectName);

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();
        final Method pgGetter = pgEffectClass.getMethod(pgGetterName);

        DoubleProperty v = new SimpleDoubleProperty();
        Method m = effectClass.getMethod(propertyName + "Property", new Class[] {});
        ((DoubleProperty)m.invoke(inputObject)).bind(v);

        pulse(); // make sure that the dirty flag is cleaned before testing of binding
        v.set(expected);
        pulse();
        assertEquals((float)expected, ((Number)pgGetter.invoke(pgObject)).floatValue(), 1e-100);
    }

    protected void checkIntPropertySynced(
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            int expected)
            throws Exception {
        checkIntPropertySynced(e, e.impl_getImpl(), effectName, propertyName, pgEffectName, pgPropertyName, expected);
    }

    protected void checkIntPropertySynced(
            Object inputObject,
            Object pgObject,
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
        ((IntegerProperty)m.invoke(inputObject)).bind(v);

        pulse(); // make sure that the dirty flag is cleaned before testing of binding
        v.set(expected);
        pulse();
        assertEquals(expected, ((Number)pgGetter.invoke(pgObject)).intValue());
    }

    protected void checkBooleanPropertySynced(
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            boolean expected)
            throws Exception {
        checkBooleanPropertySynced(e, e.impl_getImpl(), effectName, propertyName, pgEffectName, pgPropertyName, expected);
    }

    protected void checkBooleanPropertySynced(
            Object inputObject,
            Object pgObject,
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            boolean expected)
            throws Exception {
        final Class effectClass = Class.forName(effectName);
        final Class pgEffectClass = Class.forName(pgEffectName);

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();
        final Method pgGetter = pgEffectClass.getMethod(pgGetterName);

        BooleanProperty v = new SimpleBooleanProperty();
        Method m = effectClass.getMethod(propertyName + "Property", new Class[] {});
        ((BooleanProperty)m.invoke(inputObject)).bind(v);

        pulse(); // make sure that the dirty flag is cleaned before testing of binding
        v.set(expected);
        pulse();
        assertEquals(expected, pgGetter.invoke(pgObject));
    }

    protected void checkObjectPropertySynced(
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            Object expected,
            Object pgExpected,
            Object defaultVal)
            throws Exception {
       checkObjectPropertySynced(e, e.impl_getImpl(), effectName, propertyName, pgEffectName, pgPropertyName, expected, pgExpected, defaultVal);
    }

    protected void checkObjectPropertySynced(
            Object inputObject,
            Object pgObject,
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            Object expected,
            Object pgExpected,
            Object defaultVal)
            throws Exception {
        final Class effectClass = Class.forName(effectName);
        final Class pgEffectClass = Class.forName(pgEffectName);

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();
        final Method pgGetter = pgEffectClass.getMethod(pgGetterName);

        ObjectProperty v = new SimpleObjectProperty(defaultVal);
        Method m = effectClass.getMethod(propertyName + "Property", new Class[] {});

        ((ObjectProperty)m.invoke(inputObject)).bind(v);

        pulse(); // make sure that the dirty flag is cleaned before testing of binding
        v.set(expected);
        pulse();
        assertEquals(pgExpected, pgGetter.invoke(pgObject));
    }

    protected static void assertColor4fEquals(Color4f expected, Color4f actual) {
        assertEquals(expected.getRed(), actual.getRed(), 1e-100);
        assertEquals(expected.getGreen(), actual.getGreen(), 1e-100);
        assertEquals(expected.getBlue(), actual.getBlue(), 1e-100);
        assertEquals(expected.getAlpha(), actual.getAlpha(), 1e-100);
    }

    protected void checkEffectPropertySynced(
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            BoxBlur expected,
            com.sun.scenario.effect.BoxBlur pgExpected)
            throws Exception {
        final Class effectClass = Class.forName(effectName);
        final Class pgEffectClass = Class.forName(pgEffectName);

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();
        final Method pgGetter = pgEffectClass.getMethod(pgGetterName);

        ObjectProperty v = new SimpleObjectProperty();
        Method m = effectClass.getMethod(propertyName + "Property", new Class[] {});
        ((ObjectProperty)m.invoke(e)).bind(v);

        pulse(); // make sure that the dirty flag is cleaned before testing of binding
        v.set(expected);
        pulse();
        assertEquals(pgExpected, pgGetter.invoke(e.impl_getImpl()));

        // test wheter input listeners were correctly registered
        expected.setWidth(150);
        assertEquals(150, expected.getWidth(), 1e-100);
        pulse();
        assertEquals(150, pgExpected.getHorizontalSize());
    }

    protected Object getObjectPropertySynced(
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            Object expected)
            throws Exception {
        return getObjectPropertySynced(e, e.impl_getImpl(), effectName, propertyName, pgEffectName, pgPropertyName, expected);
    }

    protected Object getObjectPropertySynced(
            Object inputObject,
            Object pgObject,
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            Object expected)
            throws Exception {
        final Class effectClass = Class.forName(effectName);
        final Class pgEffectClass = Class.forName(pgEffectName);

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();
        final Method pgGetter = pgEffectClass.getMethod(pgGetterName);

        ObjectProperty v = new SimpleObjectProperty(Color.BLACK);
        Method m = effectClass.getMethod(propertyName + "Property", new Class[] {});

        ((ObjectProperty)m.invoke(inputObject)).bind(v);
        pulse(); // make sure that the dirty flag is cleaned before testing of binding
        v.set(expected);
        pulse();
        return pgGetter.invoke(pgObject);
    }
    
    protected Object getDoublePropertySynced(
            String effectName,
            String propertyName,
            String pgEffectName,
            String pgPropertyName,
            double expected)
            throws Exception {
        final Class effectClass = Class.forName(effectName);
        final Class pgEffectClass = Class.forName(pgEffectName);

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();
        final Method pgGetter = pgEffectClass.getMethod(pgGetterName);

        DoubleProperty v = new SimpleDoubleProperty();
        Method m = effectClass.getMethod(propertyName + "Property", new Class[] {});

        ((DoubleProperty)m.invoke(e)).bind(v);
        pulse(); // make sure that the dirty flag is cleaned before testing of binding
        v.set(expected);
        pulse();
        return pgGetter.invoke(e.impl_getImpl());
    }
}
