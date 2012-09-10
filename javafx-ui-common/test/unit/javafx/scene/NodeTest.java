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

package javafx.scene;

import com.sun.javafx.pgstub.StubGroup;
import com.sun.javafx.pgstub.StubCircle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Comparator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Point2D;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.TestUtils;
import javafx.scene.transform.Transform;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.PGNode;
import javafx.scene.shape.Circle;
/**
 * Tests various aspects of Node.
 *
 */
public class NodeTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // Things to test:
        // When parent is changed, should cursor on toolkit change as well if
        // the current node has the mouse over it and didn't explicitly set a
        // cursor??

        // Test CSS integration

        // Events:
            // Events should *not* be delivered to invisible nodes as per the
            // specification for visible

        // A Node must lose focus when it is no longer visible

        // A node made invisible must cause the cursor to be updated

        // Setting the cursor should override the parent cursor when hover
        // (test that this happens both when the node already has hover set and
        // when hover is changed to true)

        // Setting the cursor to null should revert to parent cursor when hover
        // (test that this happens both when the node already has hover set and
        // when hover is changed to true)

        // Clip:
            // Test setting/clearing the clip affects the bounds
            // Test changing bounds / smooth / etc on clip updates bounds of clipped Node

        // Effect:
            // Test setting/clearing the effect affects the bounds
            // Test changing state on Effect updates bounds of Node

        // Test that a disabled Group affects the disabled property of child nodes

        // Test contains, intersects methods
        // Test parentToLocal/localToStage/etc

        // Test computeCompleteBounds
        // (other bounds test situtations explicitly tested in BoundsTest)

        // Test transforms end up setting the correct matrix on the peer
        // In particular, test that pivots are taken correctly into account

        // Test hover is updated when mouse enters
        // Test hover is updated when mouse exists
        // Test hover is updated when mouse was over but a higher node then
        // turns on blocks mouse
        // Test hover is updated when node moves out from under the cursor
        // TODO most of these cases cannot be handled until/unless we update
        // the list of nodes under the cursor on pulse events

        // Test pressed is updated when mouse is pressed
        // Test pressed is updated when mouse is released
        // TODO shoudl pressed obey the semantics of a button that is armed & pressed?
        // Or should "armed" be put on Node? What to do here?

        // Test various onMouseXXX event handlers

        // Test onKeyXXX handlers

        // Test focused is updated?
        // Test nodes which are not focusable are not focused!
        // Test focus... (SHOULD NOT DEPEND ON KEY LISTENERS BEING INSTALLED!!)

        // Test that clip is taken into account for both "contains" and
        // "intersects". See http://javafx-jira.kenai.com/browse/RT-646



    /***************************************************************************
     *                                                                         *
     *                              Basic Node Tests                           *
     *                                                                         *
     **************************************************************************/

// TODO disable this because it depends on TestNode
//    @Test public void testPeerNotifiedOfVisibilityChanges() {
//        Rectangle rect = new Rectangle();
//        Node peer = rect.impl_getPGNode();
//        assertEquals(peer.visible, rect.visible);
//
//        rect.visible = false;
//        assertEquals(peer.visible, rect.visible);
//
//        rect.visible = true;
//        assertEquals(peer.visible, rect.visible);
//    }

    /***************************************************************************
     *                                                                         *
     *                            Testing Node Bounds                          *
     *                                                                         *
     **************************************************************************/

// TODO disable this because it depends on TestNode
//     public function testContainsCallsPeer():Void {
//         var rect = Rectangle { };
//         var peer = rect.impl_getPGNode() as TestNode;
//         peer.numTimesContainsCalled = 0;
//
//         rect.contains(0, 0);
//         assertEquals(1, peer.numTimesContainsCalled);
//
//         rect.contains(Point2D { x:10, y:10 });
//         assertEquals(2, peer.numTimesContainsCalled);
//     }

// TODO disable this because it depends on TestNode
//     public function testIntersectsCallsPeer():Void {
//         var rect = Rectangle { };
//         var peer = rect.impl_getPGNode() as TestNode;
//         peer.numTimesIntersectsCalled = 0;
//
//         rect.intersects(0, 0, 10, 10);
//         assertEquals(1, peer.numTimesIntersectsCalled);
//
//         rect.intersects(BoundingBox { minX:10, minY:10, width:100, height:100 });
//         assertEquals(2, peer.numTimesIntersectsCalled);
//     }

    /***************************************************************************
     *                                                                         *
     *                          Testing Node transforms                        *
     *                                                                         *
     **************************************************************************/

    /**
     * Tests that the function which converts a com.sun.javafx.geom.Point2D
     * in parent coords to local coords works properly.
     */
    @Test public void testParentToLocalGeomPoint() {
        Rectangle rect = new Rectangle();
        rect.setTranslateX(10);
        rect.setTranslateY(10);
        rect.setWidth(100);
        rect.setHeight(100);
        rect.getTransforms().clear();
        rect.getTransforms().addAll(Transform.scale(2, 2), Transform.translate(30, 30));

        Point2D pt = new Point2D(0, 0);
        pt = rect.parentToLocal(pt);
        assertEquals(new Point2D(-35, -35), pt);
    }

    // TODO need to test with some observableArrayList of transforms which cannot be
    // cleanly inverted so that we can test that code path

    @Test public void testLocalToParentGeomPoint() {
        Rectangle rect = new Rectangle();
        rect.setTranslateX(10);
        rect.setTranslateY(10);
        rect.setWidth(100);
        rect.setHeight(100);
        rect.getTransforms().clear();
        rect.getTransforms().addAll(Transform.scale(2, 2), Transform.translate(30, 30));

        Point2D pt = new Point2D(0, 0);
        pt = rect.localToParent(pt);
        assertEquals(new Point2D(70, 70), pt);
    }

    @Test public void testPickingNodeDirectlyNoTransforms() {
        Rectangle rect = new Rectangle();
        rect.setX(10);
        rect.setY(10);
        rect.setWidth(100);
        rect.setHeight(100);

        // needed since picking doesn't work unless rooted in a scene and visible
        Scene scene = new Scene(new Group());
        scene.getRoot().getChildren().add(rect);

        assertSame(rect, rect.impl_pickNode(50, 50));
        assertNull(rect.impl_pickNode(0, 0));
    }

    @Test public void testPickingNodeDirectlyWithTransforms() {
        Rectangle rect = new Rectangle();
        rect.setTranslateX(10);
        rect.setTranslateY(10);
        rect.setWidth(100);
        rect.setHeight(100);

        // needed since picking doesn't work unless rooted in a scene and visible
        Scene scene = new Scene(new Group());
        scene.getRoot().getChildren().add(rect);

        assertSame(rect, rect.impl_pickNode(50, 50));
        assertNull(rect.impl_pickNode(0, 0));
    }

    @Test public void testEffectSharedOnNodes() {
        Effect effect = new DropShadow();
        Rectangle node = new Rectangle();
        node.setEffect(effect);

        Rectangle node2 = new Rectangle();
        node2.setEffect(effect);

        assertEquals(effect, node.getEffect());
        assertEquals(effect, node2.getEffect());
    }

    public static void testBooleanPropertyPropagation(
        final Node node,
        final String propertyName,
        final boolean initialValue,
        final boolean newValue) throws Exception {

        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));
        final String setterName = new StringBuilder("set").append(propertyNameBuilder).toString();
        final String getterName = new StringBuilder("is").append(propertyNameBuilder).toString();

        final Class<? extends Node> nodeClass = node.getClass();
        final Method setter = nodeClass.getMethod(setterName, boolean.class);
        final Method getter = nodeClass.getMethod(getterName);

        final PGNode pgNode = node.impl_getPGNode();
        final Class<? extends PGNode> impl_class = pgNode.getClass();
        final Method impl_getter = impl_class.getMethod(getterName);


        // 1. Create test scene
        final Scene scene = new Scene(new Group());
        scene.getRoot().getChildren().add(node);

        // 2. Initial setup
        setter.invoke(node, initialValue);
        node.impl_syncPGNode();
        assertEquals(initialValue, getter.invoke(node));
        assertEquals(initialValue, impl_getter.invoke(pgNode));

        // 3. Change value of the property
        setter.invoke(node, newValue);

        // 4. Check that the property value has changed but has not propagated to PGNode
        assertEquals(newValue, getter.invoke(node));
        assertEquals(initialValue, impl_getter.invoke(pgNode));

        // 5. Propagate the property value to PGNode
        node.impl_syncPGNode();

        // 6. Check that the value has been propagated to PGNode
        assertEquals(newValue, impl_getter.invoke(pgNode));
    }


    public static void testFloatPropertyPropagation(
        final Node node,
        final String propertyName,
        final float initialValue,
        final float newValue) throws Exception {

        testFloatPropertyPropagation(node, propertyName, propertyName, initialValue, newValue);
    }

    public static void syncNode(Node node) {
        node.updateBounds();
        node.impl_syncPGNode();
    }

    public static void assertBooleanPropertySynced(
            final Node node,
            final String propertyName,
            final String pgPropertyName,
            final boolean value) throws Exception {

        final Scene scene = new Scene(new Group(), 500, 500);

        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));
        final String getterName = new StringBuilder("is").append(propertyNameBuilder).toString();
        Method getterMethod = node.getClass().getMethod(getterName, new Class[]{});
        Boolean defaultValue = (Boolean)getterMethod.invoke(node);
        BooleanProperty v = new SimpleBooleanProperty(defaultValue);

        Method modelMethod = node.getClass().getMethod(
                propertyName + "Property",
                new Class[]{});
        BooleanProperty model = (BooleanProperty)modelMethod.invoke(node);
        model.bind(v);

        ((Group)scene.getRoot()).getChildren().add(node);

        NodeTest.syncNode(node);
        assertEquals(defaultValue, TestUtils.getBooleanValue(node, pgPropertyName));

        v.set(value);
        NodeTest.syncNode(node);
        assertEquals(value, TestUtils.getBooleanValue(node, pgPropertyName));
    }

    public static void assertIntPropertySynced(
            final Node node,
            final String propertyName,
            final String pgPropertyName,
            final int value) throws Exception {

        final Scene scene = new Scene(new Group(), 500, 500);

        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));
        final String getterName = new StringBuilder("get").append(propertyNameBuilder).toString();
        Method getterMethod = node.getClass().getMethod(getterName, new Class[]{});
        Integer defaultValue = (Integer)getterMethod.invoke(node);
        IntegerProperty v = new SimpleIntegerProperty(defaultValue);

        Method modelMethod = node.getClass().getMethod(
                propertyName + "Property",
                new Class[]{});
        IntegerProperty model = (IntegerProperty)modelMethod.invoke(node);
        model.bind(v);

        ((Group)scene.getRoot()).getChildren().add(node);

        NodeTest.syncNode(node);
        assertTrue(numbersEquals(defaultValue,
                (Number)TestUtils.getObjectValue(node, pgPropertyName)));

        v.set(value);
        NodeTest.syncNode(node);
        assertTrue(numbersEquals(new Integer(value),
                (Number)TestUtils.getObjectValue(node, pgPropertyName)));
    }

    public static boolean numbersEquals(Number expected, Number value) {
        return numbersEquals(expected, value, 0.001);
    }

    public static boolean numbersEquals(Number expected, Number value, double delta) {
        return (Math.abs(expected.doubleValue() - value.doubleValue()) < delta);
    }

    public static void assertDoublePropertySynced(
            final Node node,
            final String propertyName,
            final String pgPropertyName,
            final double value) throws Exception {

        final Scene scene = new Scene(new Group(), 500, 500);

        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));
        final String getterName = new StringBuilder("get").append(propertyNameBuilder).toString();
        Method getterMethod = node.getClass().getMethod(getterName, new Class[]{});
        Double defaultValue = (Double)getterMethod.invoke(node);
        DoubleProperty v = new SimpleDoubleProperty(defaultValue);

        Method modelMethod = node.getClass().getMethod(
                propertyName + "Property",
                new Class[]{});
        DoubleProperty model = (DoubleProperty)modelMethod.invoke(node);
        model.bind(v);

        ((Group)scene.getRoot()).getChildren().add(node);

         NodeTest.syncNode(node);
        assertTrue(numbersEquals(defaultValue,
                (Number)TestUtils.getObjectValue(node, pgPropertyName)));

        v.set(value);
        NodeTest.syncNode(node);
        assertTrue(numbersEquals(new Double(value),
                (Number)TestUtils.getObjectValue(node, pgPropertyName)));
    }


    public static void assertObjectPropertySynced(
            final Node node,
            final String propertyName,
            final String pgPropertyName,
            final Object value) throws Exception {

        final Scene scene = new Scene(new Group(), 500, 500);

        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));
        final String getterName = new StringBuilder("get").append(propertyNameBuilder).toString();
        Method getterMethod = node.getClass().getMethod(getterName, new Class[]{});
        Object defaultValue = getterMethod.invoke(node);
        ObjectProperty v = new SimpleObjectProperty(defaultValue);

        Method modelMethod = node.getClass().getMethod(
                propertyName + "Property",
                new Class[]{});
        ObjectProperty model = (ObjectProperty)modelMethod.invoke(node);
        model.bind(v);

        ((Group)scene.getRoot()).getChildren().add(node);

        NodeTest.syncNode(node);
        // sometimes enum is used on node but int on PGNode
        Object result1 = TestUtils.getObjectValue(node, pgPropertyName);
        if (result1 instanceof Integer) {
            assertTrue(((Enum)defaultValue).ordinal() == ((Integer)result1).intValue());
        } else {
            assertEquals(defaultValue, TestUtils.getObjectValue(node, pgPropertyName));
        }

        v.set(value);
        NodeTest.syncNode(node);

        Object result2 = TestUtils.getObjectValue(node, pgPropertyName);
        if (result2 instanceof Integer) {
            assertTrue(((Enum)value).ordinal() == ((Integer)result2).intValue());
        } else {
            assertEquals(value, TestUtils.getObjectValue(node, pgPropertyName));
        }
    }



    public static void assertObjectProperty_AsStringSynced(
            final Node node,
            final String propertyName,
            final String pgPropertyName,
            final Object value) throws Exception {

        final Scene scene = new Scene(new Group(), 500, 500);

        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));
        final String getterName = new StringBuilder("get").append(propertyNameBuilder).toString();
        Method getterMethod = node.getClass().getMethod(getterName, new Class[]{});
        Object defaultValue = getterMethod.invoke(node);
        ObjectProperty v = new SimpleObjectProperty(defaultValue);

        Method modelMethod = node.getClass().getMethod(
                propertyName + "Property",
                new Class[]{});
        ObjectProperty model = (ObjectProperty)modelMethod.invoke(node);
        model.bind(v);

        ((Group)scene.getRoot()).getChildren().add(node);

        NodeTest.syncNode(node);
        assertEquals(
                defaultValue.toString(),
                TestUtils.getObjectValue(node, pgPropertyName).toString());

        v.set(value);
        NodeTest.syncNode(node);

        assertEquals(
                value.toString(),
                TestUtils.getObjectValue(node, pgPropertyName).toString());
    }

    public static void assertStringPropertySynced(
            final Node node,
            final String propertyName,
            final String pgPropertyName,
            final String value) throws Exception {

        final Scene scene = new Scene(new Group(), 500, 500);

        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));
        final String getterName = new StringBuilder("get").append(propertyNameBuilder).toString();
        Method getterMethod = node.getClass().getMethod(getterName, new Class[]{});
        String defaultValue = (String)getterMethod.invoke(node);
        StringProperty v = new SimpleStringProperty(defaultValue);

        Method modelMethod = node.getClass().getMethod(
                propertyName + "Property",
                new Class[]{});
        StringProperty model = (StringProperty)modelMethod.invoke(node);
        model.bind(v);

        ((Group)scene.getRoot()).getChildren().add(node);

        NodeTest.syncNode(node);
        assertEquals(
                defaultValue,
                TestUtils.getStringValue(node, pgPropertyName));

        v.set(value);
        NodeTest.syncNode(node);

        assertEquals(
                value,
                TestUtils.getStringValue(node, pgPropertyName));
    }

    public static void testFloatPropertyPropagation(
        final Node node,
        final String propertyName,
        final String pgPropertyName,
        final float initialValue,
        final float newValue) throws Exception {

        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));

        final String setterName = new StringBuilder("set").append(propertyNameBuilder).toString();
        final String getterName = new StringBuilder("get").append(propertyNameBuilder).toString();
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();

        final Class<? extends Node> nodeClass = node.getClass();
        final Method setter = nodeClass.getMethod(setterName, float.class);
        final Method getter = nodeClass.getMethod(getterName);

        final PGNode pgNode = node.impl_getPGNode();
        final Class<? extends PGNode> impl_class = pgNode.getClass();
        final Method impl_getter = impl_class.getMethod(pgGetterName);


        // 1. Create test scene
        final Scene scene = new Scene(new Group());
        scene.getRoot().getChildren().add(node);

        // 2. Initial setup
        setter.invoke(node, initialValue);
        node.impl_syncPGNode();
        assertEquals(initialValue, (Float) getter.invoke(node), 1e-100);
        assertEquals(initialValue, (Float) impl_getter.invoke(pgNode), 1e-100);

        // 3. Change value of the property
        setter.invoke(node, newValue);

        // 4. Check that the property value has changed but has not propagated to PGNode
        assertEquals(newValue, (Float) getter.invoke(node), 1e-100);
        assertEquals(initialValue, (Float) impl_getter.invoke(pgNode), 1e-100);

        // 5. Propagate the property value to PGNode
        node.impl_syncPGNode();

        // 6. Check that the value has been propagated to PGNode
        assertEquals(newValue, (Float) impl_getter.invoke(pgNode), 1e-100);
    }

    public static void testDoublePropertyPropagation(
        final Node node,
        final String propertyName,
        final double initialValue,
        final double newValue) throws Exception {

        testDoublePropertyPropagation(node, propertyName, propertyName, initialValue, newValue);
    }


    public static void testDoublePropertyPropagation(
        final Node node,
        final String propertyName,
        final String pgPropertyName,
        final double initialValue,
        final double newValue) throws Exception {

        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));

        final String setterName = new StringBuilder("set").append(propertyNameBuilder).toString();
        final String getterName = new StringBuilder("get").append(propertyNameBuilder).toString();
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();

        final Class<? extends Node> nodeClass = node.getClass();
        final Method setter = nodeClass.getMethod(setterName, double.class);
        final Method getter = nodeClass.getMethod(getterName);

        final PGNode pgNode = node.impl_getPGNode();
        final Class<? extends PGNode> impl_class = pgNode.getClass();
        final Method impl_getter = impl_class.getMethod(pgGetterName);


        // 1. Create test scene
        final Scene scene = new Scene(new Group());
        scene.getRoot().getChildren().add(node);

        // 2. Initial setup
        setter.invoke(node, initialValue);
        node.impl_syncPGNode();
        assertEquals(initialValue, (Double) getter.invoke(node), 1e-100);
        assertEquals((float) initialValue, (Float) impl_getter.invoke(pgNode), 1e-100);

        // 3. Change value of the property
        setter.invoke(node, newValue);

        // 4. Check that the property value has changed but has not propagated to PGNode
        assertEquals(newValue, (Double) getter.invoke(node), 1e-100);
        assertEquals((float) initialValue, (Float) impl_getter.invoke(pgNode), 1e-100);

        // 5. Propagate the property value to PGNode
        node.impl_syncPGNode();

        // 6. Check that the value has been propagated to PGNode
        assertEquals((float) newValue, (Float) impl_getter.invoke(pgNode), 1e-100);
    }

    public interface ObjectValueConvertor {
        Object toSg(Object pgValue);
    }

    public static final Comparator DEFAULT_OBJ_COMPARATOR =
            new Comparator() {
                @Override
                public int compare(final Object sgValue, final Object pgValue) {
                    assertEquals(sgValue, pgValue);
                    return 0;
                }
            };

    public static void testObjectPropertyPropagation(
        final Node node,
        final String propertyName,
        final Object initialValue,
        final Object newValue) throws Exception {

        testObjectPropertyPropagation(node, propertyName, propertyName, initialValue, newValue);
    }

    public static void testObjectPropertyPropagation(
            final Node node,
            final String propertyName,
            final String pgPropertyName,
            final Object initialValue,
            final Object newValue) throws Exception {
        testObjectPropertyPropagation(node, propertyName, pgPropertyName,
                initialValue, newValue, DEFAULT_OBJ_COMPARATOR);
    }

    public static void testObjectPropertyPropagation(
            final Node node,
            final String propertyName,
            final String pgPropertyName,
            final Object initialValue,
            final Object newValue,
            final ObjectValueConvertor convertor) throws Exception {
        testObjectPropertyPropagation(
                node, propertyName, pgPropertyName,
                initialValue, newValue,
                new Comparator() {
                    @Override
                    public int compare(final Object sgValue,
                                       final Object pgValue) {
                        assertEquals(sgValue, convertor.toSg(pgValue));
                        return 0;
                    }
                });
    }

    public static void testObjectPropertyPropagation(
            final Node node,
            final String propertyName,
            final String pgPropertyName,
            final Object initialValue,
            final Object newValue,
            final Comparator comparator) throws Exception {
        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));

        final String setterName = new StringBuilder("set").append(propertyNameBuilder).toString();
        final String getterName = new StringBuilder("get").append(propertyNameBuilder).toString();
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();

        final Class<? extends Node> nodeClass = node.getClass();
        final Method getter = nodeClass.getMethod(getterName);
        final Method setter = nodeClass.getMethod(setterName, getter.getReturnType());

        final PGNode pgNode = node.impl_getPGNode();
        final Class<? extends PGNode> impl_class = pgNode.getClass();
        final Method impl_getter = impl_class.getMethod(pgGetterName);


        // 1. Create test scene
        final Scene scene = new Scene(new Group());
        scene.getRoot().getChildren().add(node);

        // 2. Initial setup
        setter.invoke(node, initialValue);
        node.impl_syncPGNode();
        assertEquals(initialValue, getter.invoke(node));
        assertEquals(0, comparator.compare(initialValue,
                                           impl_getter.invoke(pgNode)));

        // 3. Change value of the property
        setter.invoke(node, newValue);

        // 4. Check that the property value has changed but has not propagated to PGNode
        assertEquals(newValue, getter.invoke(node));
        assertEquals(0, comparator.compare(initialValue,
                                           impl_getter.invoke(pgNode)));

        // 5. Propagate the property value to PGNode
        node.impl_syncPGNode();

        // 6. Check that the value has been propagated to PGNode
        assertEquals(0, comparator.compare(newValue,
                                           impl_getter.invoke(pgNode)));
    }


    public static void testIntPropertyPropagation(
        final Node node,
        final String propertyName,
        final int initialValue,
        final int newValue) throws Exception {

        testIntPropertyPropagation(node, propertyName, propertyName, initialValue, newValue);
    }


    public static void testIntPropertyPropagation(
        final Node node,
        final String propertyName,
        final String pgPropertyName,
        final int initialValue,
        final int newValue) throws Exception {

        final StringBuilder propertyNameBuilder = new StringBuilder(propertyName);
        propertyNameBuilder.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));

        final StringBuilder pgPropertyNameBuilder = new StringBuilder(pgPropertyName);
        pgPropertyNameBuilder.setCharAt(0, Character.toUpperCase(pgPropertyName.charAt(0)));

        final String setterName = new StringBuilder("set").append(propertyNameBuilder).toString();
        final String getterName = new StringBuilder("get").append(propertyNameBuilder).toString();
        final String pgGetterName = new StringBuilder("get").append(pgPropertyNameBuilder).toString();

        final Class<? extends Node> nodeClass = node.getClass();
        final Method getter = nodeClass.getMethod(getterName);
        final Method setter = nodeClass.getMethod(setterName, getter.getReturnType());

        final PGNode pgNode = node.impl_getPGNode();
        final Class<? extends PGNode> impl_class = pgNode.getClass();
        final Method impl_getter = impl_class.getMethod(pgGetterName);


        // 1. Create test scene
        final Scene scene = new Scene(new Group());
        scene.getRoot().getChildren().add(node);

        // 2. Initial setup
        setter.invoke(node, initialValue);
        assertEquals(initialValue, getter.invoke(node));
        node.impl_syncPGNode();
        assertEquals(initialValue, ((Number) impl_getter.invoke(pgNode)).intValue());

        // 3. Change value of the property
        setter.invoke(node, newValue);

        // 4. Check that the property value has changed but has not propagated to PGNode
        assertEquals(newValue, getter.invoke(node));
        assertEquals(initialValue, ((Number) impl_getter.invoke(pgNode)).intValue());

        // 5. Propagate the property value to PGNode
        node.impl_syncPGNode();

        // 6. Check that the value has been propagated to PGNode
        assertEquals(newValue, ((Number) impl_getter.invoke(pgNode)).intValue());
    }

    public static void callSyncPGNode(final Node node) {
        node.impl_syncPGNode();
    }

    @Test
    public void testToFront() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Group g = new Group();

        Scene scene = new Scene(g);
        g.getChildren().add(rect1);
        g.getChildren().add(rect2);

        rect1.toFront();
        rect2.toFront();

        // toFront should not remove rectangle from scene
        assertEquals(scene, rect2.getScene());
        assertEquals(scene, rect1.getScene());
        // test corect order of scene content
        assertEquals(rect2, g.getChildren().get(1));
        assertEquals(rect1, g.getChildren().get(0));

        rect1.toFront();
        assertEquals(scene, rect2.getScene());
        assertEquals(scene, rect1.getScene());
        assertEquals(rect1, g.getChildren().get(1));
        assertEquals(rect2, g.getChildren().get(0));
    }

    @Test
    public void testClip() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        rect1.setClip(rect2);

        Scene scene = new Scene(new Group());
        scene.getRoot().getChildren().add(rect1);
        assertEquals(rect2, rect1.getClip());
        assertEquals(scene, rect2.getScene());

    }

    @Test
    public void testInvalidClip() {
        Rectangle rectA = new Rectangle(300, 300);
        Rectangle clip1 = new Rectangle(10, 10);
        Rectangle clip2 = new Rectangle(100, 100);
        clip2.setClip(rectA);
        rectA.setClip(clip1);
        assertEquals(rectA.getClip(), clip1);
        thrown.expect(IllegalArgumentException.class);
        try {
            rectA.setClip(clip2);
        } catch (final IllegalArgumentException e) {
            assertNotSame(rectA.getClip(), clip2);
            throw e;
        }
    }

    @Test public void testProperties() {
        Rectangle node = new Rectangle();
        javafx.collections.ObservableMap<Object, Object> properties = node.getProperties();

        /* If we ask for it, we should get it.
         */
        assertNotNull(properties);

        /* What we put in, we should get out.
         */
        properties.put("MyKey", "MyValue");
        assertEquals("MyValue", properties.get("MyKey"));

        /* If we ask for it again, we should get the same thing.
         */
        javafx.collections.ObservableMap<Object, Object> properties2 = node.getProperties();
        assertEquals(properties2, properties);

        /* What we put in to the other one, we should get out of this one because
         * they should be the same thing.
         */
        assertEquals("MyValue", properties2.get("MyKey"));
    }

    public static boolean isDirty(Node node, DirtyBits[] dbs) {
        for(DirtyBits db:dbs) {
            if (!node.impl_isDirty(db)) {
                System.out.printf("@NodeTest:check dirty: %s [%d]\n",db,db.ordinal());
                return false;
            }
        }
        return true;
    }

    @Test
    public void testDefaultValueForOpacityIsOneWhenReadFromGetter() {
        final Node node = new Rectangle();
        assertEquals(1, node.getOpacity(), .005);
    }

    @Test
    public void testDefaultValueForOpacityIsOneWhenReadFromProperty() {
        final Node node = new Rectangle();
        assertEquals(1, node.opacityProperty().get(), .005);
    }

    @Test
    public void settingOpacityThroughSetterShouldAffectBothGetterAndProperty() {
        final Node node = new Rectangle();
        node.setOpacity(.5);
        assertEquals(.5, node.getOpacity(), .005);
        assertEquals(.5, node.opacityProperty().get(), .005);
    }

    @Test
    public void settingOpacityThroughPropertyShouldAffectBothGetterAndProperty() {
        final Node node = new Rectangle();
        node.opacityProperty().set(.5);
        assertEquals(.5, node.getOpacity(), .005);
        assertEquals(.5, node.opacityProperty().get(), .005);
    }

    @Test
    public void testDefaultValueForVisibleIsTrueWhenReadFromGetter() {
        final Node node = new Rectangle();
        assertTrue(node.isVisible());
    }

    @Test
    public void testDefaultValueForVisibleIsTrueWhenReadFromProperty() {
        final Node node = new Rectangle();
        assertTrue(node.visibleProperty().get());
    }

    @Test
    public void settingVisibleThroughSetterShouldAffectBothGetterAndProperty() {
        final Node node = new Rectangle();
        node.setVisible(false);
        assertFalse(node.isVisible());
        assertFalse(node.visibleProperty().get());
    }

    @Test
    public void settingVisibleThroughPropertyShouldAffectBothGetterAndProperty() {
        final Node node = new Rectangle();
        node.visibleProperty().set(false);
        assertFalse(node.isVisible());
        assertFalse(node.visibleProperty().get());
    }

    @Test
    public void testDefaultStyleIsEmptyString() {
        final Node node = new Rectangle();
        assertEquals("", node.getStyle());
        assertEquals("", node.styleProperty().get());
        node.setStyle(null);
        assertEquals("", node.styleProperty().get());
        assertEquals("", node.getStyle());
    }

    @Test
    public void testSynchronizationOfInvisibleNodes() {
        final Group g = new Group();
        final Circle c = new Circle(50);
        final StubGroup sg = (StubGroup)g.impl_getPGNode();
        final StubCircle sc = (StubCircle)c.impl_getPGNode();
        g.getChildren().add(c);

        syncNode(g);
        syncNode(c);
        assertFalse(sg.getChildren().isEmpty());
        assertEquals(50.0, sc.getRadius(), 0.01);

        g.setVisible(false);

        syncNode(g);
        syncNode(c);
        assertFalse(sg.isVisible());

        final Rectangle r = new Rectangle();
        g.getChildren().add(r);
        c.setRadius(100);

        syncNode(g);
        syncNode(c);
        assertEquals(1, sg.getChildren().size());
        assertEquals(50.0, sc.getRadius(), 0.01);

        g.setVisible(true);

        syncNode(g);
        syncNode(c);
        assertEquals(2, sg.getChildren().size());
        assertEquals(100.0, sc.getRadius(), 0.01);
        
    }
}
