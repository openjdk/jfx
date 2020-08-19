/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import test.javafx.scene.shape.TestUtils;
import test.javafx.scene.shape.CircleTest;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.Translate2D;
import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.scene.shape.RectangleHelper;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGRectangle;
import test.com.sun.javafx.test.objects.TestScene;
import test.com.sun.javafx.test.objects.TestStage;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.Utils;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Set;

import javafx.scene.Group;
import javafx.scene.GroupShim;
import javafx.scene.Node;
import javafx.scene.NodeShim;
import javafx.scene.ParallelCamera;
import javafx.scene.ParentShim;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneShim;
import javafx.scene.SubScene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import static org.junit.Assert.*;
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

    @Test
    public void testGetPseudoClassStatesShouldReturnSameSet() {
        Rectangle node = new Rectangle();
        Set<PseudoClass> set1 = node.getPseudoClassStates();
        Set<PseudoClass> set2 = node.getPseudoClassStates();
        assertSame("getPseudoClassStates() should always return the same instance",
                set1, set2);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPseudoClassStatesIsUnmodifiable() {
        Node node = new Rectangle();
        node.getPseudoClassStates().add(PseudoClass.getPseudoClass("dummy"));
    }

    @Test
    public void testUnmodifiablePseudoClassStatesEqualsBackingStates() {
        Rectangle node = new Rectangle();
        PseudoClass pseudo = PseudoClass.getPseudoClass("Pseudo");
        node.pseudoClassStateChanged(pseudo, true);
        assertEquals(1, node.getPseudoClassStates().size());
        assertEquals(NodeShim.pseudoClassStates(node).size(), node.getPseudoClassStates().size());
        assertTrue(NodeShim.pseudoClassStates(node).contains(pseudo));
        assertTrue(node.getPseudoClassStates().contains(pseudo));
    }

    private boolean isInvalidationListenerInvoked;
    private boolean isChangeListenerInvoked;
    @Test
    public void testPseudoClassStatesListenersAreInvoked() {
        Rectangle node = new Rectangle();
        node.getPseudoClassStates().addListener((InvalidationListener) inv -> {
            isInvalidationListenerInvoked = true;
        });
        node.getPseudoClassStates().addListener((SetChangeListener<PseudoClass>) c -> {
            isChangeListenerInvoked = true;
        });

        PseudoClass pseudo = PseudoClass.getPseudoClass("Pseudo");
        node.pseudoClassStateChanged(pseudo, true);
        assertTrue(isInvalidationListenerInvoked);
        assertTrue(isChangeListenerInvoked);
    }

    @Test
    public void testPseudoClassStatesNotGCed() {
        Node node = new Rectangle();
        WeakReference<Set<?>> weakRef = new WeakReference<>(node.getPseudoClassStates());
        TestUtils.attemptGC(weakRef);
        assertNotNull("pseudoClassStates must not be gc'ed", weakRef.get());
    }

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
        ParentShim.getChildren(scene.getRoot()).add(rect);

        PickResultChooser res = new PickResultChooser();
        NodeHelper.pickNode(rect, new PickRay(50, 50, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), res);
        assertSame(rect, res.getIntersectedNode());
        res = new PickResultChooser();
        NodeHelper.pickNode(rect, new PickRay(0, 0, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), res);
        assertNull(res.getIntersectedNode());
    }

    @Test public void testPickingNodeDirectlyWithTransforms() {
        Rectangle rect = new Rectangle();
        rect.setTranslateX(10);
        rect.setTranslateY(10);
        rect.setWidth(100);
        rect.setHeight(100);

        // needed since picking doesn't work unless rooted in a scene and visible
        Scene scene = new Scene(new Group());
        ParentShim.getChildren(scene.getRoot()).add(rect);

        PickResultChooser res = new PickResultChooser();
        NodeHelper.pickNode(rect, new PickRay(50, 50, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), res);
        assertSame(rect, res.getIntersectedNode());
        res = new PickResultChooser();
        NodeHelper.pickNode(rect, new PickRay(0, 0, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), res);
        assertNull(res.getIntersectedNode());
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

        final NGNode peer = NodeHelper.getPeer(node);
        final Class<? extends NGNode> impl_class = peer.getClass();
        final Method impl_getter = impl_class.getMethod(getterName);


        // 1. Create test scene
        final Scene scene = new Scene(new Group());
        ParentShim.getChildren(scene.getRoot()).add(node);

        // 2. Initial setup
        setter.invoke(node, initialValue);
        NodeHelper.syncPeer(node);
        assertEquals(initialValue, getter.invoke(node));
        assertEquals(initialValue, impl_getter.invoke(peer));

        // 3. Change value of the property
        setter.invoke(node, newValue);

        // 4. Check that the property value has changed but has not propagated to PGNode
        assertEquals(newValue, getter.invoke(node));
        assertEquals(initialValue, impl_getter.invoke(peer));

        // 5. Propagate the property value to PGNode
        NodeHelper.syncPeer(node);

        // 6. Check that the value has been propagated to PGNode
        assertEquals(newValue, impl_getter.invoke(peer));
    }


    public static void testFloatPropertyPropagation(
        final Node node,
        final String propertyName,
        final float initialValue,
        final float newValue) throws Exception {

        testFloatPropertyPropagation(node, propertyName, propertyName, initialValue, newValue);
    }

    public static void syncNode(Node node) {
        NodeShim.updateBounds(node);
        NodeHelper.syncPeer(node);
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

        ParentShim.getChildren(scene.getRoot()).add(node);

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

        ParentShim.getChildren(scene.getRoot()).add(node);

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
        boolean res = (Math.abs(expected.doubleValue() - value.doubleValue()) < delta);
        if (!res) {
            System.err.println("expected=" + expected + ", value=" + value);
        }
        return res;
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

        ParentShim.getChildren(scene.getRoot()).add(node);

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

        ParentShim.getChildren(scene.getRoot()).add(node);

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

        ParentShim.getChildren(scene.getRoot()).add(node);

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

        ParentShim.getChildren(scene.getRoot()).add(node);

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

        final NGNode peer = NodeHelper.getPeer(node);
        final Class<? extends NGNode> impl_class = peer.getClass();
        final Method impl_getter = impl_class.getMethod(pgGetterName);


        // 1. Create test scene
        final Scene scene = new Scene(new Group());
        ParentShim.getChildren(scene.getRoot()).add(node);

        // 2. Initial setup
        setter.invoke(node, initialValue);
        NodeHelper.syncPeer(node);
        assertEquals(initialValue, (Float) getter.invoke(node), 1e-100);
        assertEquals(initialValue, (Float) impl_getter.invoke(peer), 1e-100);

        // 3. Change value of the property
        setter.invoke(node, newValue);

        // 4. Check that the property value has changed but has not propagated to PGNode
        assertEquals(newValue, (Float) getter.invoke(node), 1e-100);
        assertEquals(initialValue, (Float) impl_getter.invoke(peer), 1e-100);

        // 5. Propagate the property value to PGNode
        NodeHelper.syncPeer(node);

        // 6. Check that the value has been propagated to PGNode
        assertEquals(newValue, (Float) impl_getter.invoke(peer), 1e-100);
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

        final NGNode peer = NodeHelper.getPeer(node);
        final Class<? extends NGNode> impl_class = peer.getClass();
        final Method impl_getter = impl_class.getMethod(pgGetterName);


        // 1. Create test scene
        final Scene scene = new Scene(new Group());
        ParentShim.getChildren(scene.getRoot()).add(node);

        // 2. Initial setup
        setter.invoke(node, initialValue);
        NodeHelper.syncPeer(node);
        assertEquals(initialValue, (Double) getter.invoke(node), 1e-100);
        assertEquals((float) initialValue, (Float) impl_getter.invoke(peer), 1e-100);

        // 3. Change value of the property
        setter.invoke(node, newValue);

        // 4. Check that the property value has changed but has not propagated to PGNode
        assertEquals(newValue, (Double) getter.invoke(node), 1e-100);
        assertEquals((float) initialValue, (Float) impl_getter.invoke(peer), 1e-100);

        // 5. Propagate the property value to PGNode
        NodeHelper.syncPeer(node);

        // 6. Check that the value has been propagated to PGNode
        assertEquals((float) newValue, (Float) impl_getter.invoke(peer), 1e-100);
    }

    public interface ObjectValueConvertor {
        Object toSg(Object pgValue);
    }

    public static final Comparator DEFAULT_OBJ_COMPARATOR =
            (sgValue, pgValue) -> {
                assertEquals(sgValue, pgValue);
                return 0;
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
                (sgValue, pgValue) -> {
                    assertEquals(sgValue, convertor.toSg(pgValue));
                    return 0;
                }
        );
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

        final NGNode peer = NodeHelper.getPeer(node);
        final Class<? extends NGNode> impl_class = peer.getClass();
        final Method impl_getter = impl_class.getMethod(pgGetterName);


        // 1. Create test scene
        final Scene scene = new Scene(new Group());
        ParentShim.getChildren(scene.getRoot()).add(node);

        // 2. Initial setup
        setter.invoke(node, initialValue);
        NodeHelper.syncPeer(node);
        assertEquals(initialValue, getter.invoke(node));
        assertEquals(0, comparator.compare(initialValue,
                                           impl_getter.invoke(peer)));

        // 3. Change value of the property
        setter.invoke(node, newValue);

        // 4. Check that the property value has changed but has not propagated to PGNode
        assertEquals(newValue, getter.invoke(node));
        assertEquals(0, comparator.compare(initialValue,
                                           impl_getter.invoke(peer)));

        // 5. Propagate the property value to PGNode
        NodeHelper.syncPeer(node);

        // 6. Check that the value has been propagated to PGNode
        assertEquals(0, comparator.compare(newValue,
                                           impl_getter.invoke(peer)));
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

        final NGNode peer = NodeHelper.getPeer(node);
        final Class<? extends NGNode> impl_class = peer.getClass();
        final Method impl_getter = impl_class.getMethod(pgGetterName);


        // 1. Create test scene
        final Scene scene = new Scene(new Group());
        ParentShim.getChildren(scene.getRoot()).add(node);

        // 2. Initial setup
        setter.invoke(node, initialValue);
        assertEquals(initialValue, getter.invoke(node));
        NodeHelper.syncPeer(node);
        assertEquals(initialValue, ((Number) impl_getter.invoke(peer)).intValue());

        // 3. Change value of the property
        setter.invoke(node, newValue);

        // 4. Check that the property value has changed but has not propagated to PGNode
        assertEquals(newValue, getter.invoke(node));
        assertEquals(initialValue, ((Number) impl_getter.invoke(peer)).intValue());

        // 5. Propagate the property value to PGNode
        NodeHelper.syncPeer(node);

        // 6. Check that the value has been propagated to PGNode
        assertEquals(newValue, ((Number) impl_getter.invoke(peer)).intValue());
    }

    public static void callSyncPGNode(final Node node) {
        NodeHelper.syncPeer(node);
    }

    @Test
    public void testToFront() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Group g = new Group();

        Scene scene = new Scene(g);
        ParentShim.getChildren(g).add(rect1);
        ParentShim.getChildren(g).add(rect2);

        rect1.toFront();
        rect2.toFront();

        // toFront should not remove rectangle from scene
        assertEquals(scene, rect2.getScene());
        assertEquals(scene, rect1.getScene());
        // test corect order of scene content
        assertEquals(rect2, ParentShim.getChildren(g).get(1));
        assertEquals(rect1, ParentShim.getChildren(g).get(0));

        rect1.toFront();
        assertEquals(scene, rect2.getScene());
        assertEquals(scene, rect1.getScene());
        assertEquals(rect1, ParentShim.getChildren(g).get(1));
        assertEquals(rect2, ParentShim.getChildren(g).get(0));
    }

    @Test
    public void testClip() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        rect1.setClip(rect2);

        Scene scene = new Scene(new Group());
        ParentShim.getChildren(scene.getRoot()).add(rect1);
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
            if (!NodeShim.isDirty(node, db)) {
                System.out.printf("@NodeTest:check dirty: %s [%d]\n",db,db.ordinal());
                return false;
            }
        }
        return true;
    }

    @Test
    public void testDefaultValueForViewOrderIsZeroWhenReadFromGetter() {
        final Node node = new Rectangle();
        assertEquals(0, node.getViewOrder(), .005);
    }

    @Test
    public void testDefaultValueForViewOrderIsZeroWhenReadFromProperty() {
        final Node node = new Rectangle();
        assertEquals(0, node.viewOrderProperty().get(), .005);
    }

    @Test
    public void settingViewOrderThroughSetterShouldAffectBothGetterAndProperty() {
        final Node node = new Rectangle();
        node.setViewOrder(.5);
        assertEquals(.5, node.getViewOrder(), .005);
        assertEquals(.5, node.viewOrderProperty().get(), .005);
    }

    @Test
    public void settingViewOrderThroughPropertyShouldAffectBothGetterAndProperty() {
        final Node node = new Rectangle();
        node.viewOrderProperty().set(.5);
        assertEquals(.5, node.getViewOrder(), .005);
        assertEquals(.5, node.viewOrderProperty().get(), .005);
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
        final Circle c = new CircleTest.StubCircle(50);
        final NGGroup sg = NodeHelper.getPeer(g);
        final CircleTest.StubNGCircle sc = NodeHelper.getPeer(c);
        ParentShim.getChildren(g).add(c);

        syncNode(g);
        syncNode(c);
        assertFalse(sg.getChildren().isEmpty());
        assertEquals(50.0, sc.getRadius(), 0.01);

        g.setVisible(false);

        syncNode(g);
        syncNode(c);
        assertFalse(sg.isVisible());

        final Rectangle r = new Rectangle();
        ParentShim.getChildren(g).add(r);
        c.setRadius(100);

        syncNode(g);
        syncNode(c);
        // Group with change in children will always be synced even if it is invisible
        assertEquals(2, sg.getChildren().size());
        assertEquals(50.0, sc.getRadius(), 0.01);

        g.setVisible(true);

        syncNode(g);
        syncNode(c);
        assertEquals(2, sg.getChildren().size());
        assertEquals(100.0, sc.getRadius(), 0.01);

    }

    @Test
    public void testIsTreeVisible() {
        final Group g = new Group();
        final Circle c = new CircleTest.StubCircle(50);

        ParentShim.getChildren(g).add(c);

        Scene s = new Scene(g);
        Stage st = new Stage();

        assertTrue(NodeHelper.isTreeVisible(g));
        assertTrue(NodeHelper.isTreeVisible(c));
        assertFalse(NodeHelper.isTreeShowing(g));
        assertFalse(NodeHelper.isTreeShowing(c));

        st.show();
        st.setScene(s);

        assertTrue(NodeHelper.isTreeVisible(g));
        assertTrue(NodeHelper.isTreeVisible(c));
        assertTrue(NodeHelper.isTreeShowing(g));
        assertTrue(NodeHelper.isTreeShowing(c));

        SceneShim.scenePulseListener_pulse(s);

        assertTrue(NodeHelper.isTreeVisible(g));
        assertTrue(NodeHelper.isTreeVisible(c));
        assertTrue(NodeHelper.isTreeShowing(g));
        assertTrue(NodeHelper.isTreeShowing(c));

        g.setVisible(false);
        SceneShim.scenePulseListener_pulse(s);

        assertFalse(NodeHelper.isTreeVisible(g));
        assertFalse(NodeHelper.isTreeVisible(c));
        assertFalse(NodeHelper.isTreeShowing(g));
        assertFalse(NodeHelper.isTreeShowing(c));

        g.setVisible(true);
        SceneShim.scenePulseListener_pulse(s);

        assertTrue(NodeHelper.isTreeVisible(g));
        assertTrue(NodeHelper.isTreeVisible(c));
        assertTrue(NodeHelper.isTreeShowing(g));
        assertTrue(NodeHelper.isTreeShowing(c));

        c.setVisible(false);
        SceneShim.scenePulseListener_pulse(s);

        assertTrue(NodeHelper.isTreeVisible(g));
        assertFalse(NodeHelper.isTreeVisible(c));
        assertTrue(NodeHelper.isTreeShowing(g));
        assertFalse(NodeHelper.isTreeShowing(c));

        c.setVisible(true);
        SceneShim.scenePulseListener_pulse(s);

        assertTrue(NodeHelper.isTreeVisible(g));
        assertTrue(NodeHelper.isTreeVisible(c));
        assertTrue(NodeHelper.isTreeShowing(g));
        assertTrue(NodeHelper.isTreeShowing(c));

        s.setRoot(new Group());
        SceneShim.scenePulseListener_pulse(s);

        assertTrue(NodeHelper.isTreeVisible(g));
        assertTrue(NodeHelper.isTreeVisible(c));
        assertFalse(NodeHelper.isTreeShowing(g));
        assertFalse(NodeHelper.isTreeShowing(c));

        s.setRoot(g);
        SceneShim.scenePulseListener_pulse(s);

        assertTrue(NodeHelper.isTreeVisible(g));
        assertTrue(NodeHelper.isTreeVisible(c));
        assertTrue(NodeHelper.isTreeShowing(g));
        assertTrue(NodeHelper.isTreeShowing(c));

        st.hide();
        SceneShim.scenePulseListener_pulse(s);

        assertTrue(NodeHelper.isTreeVisible(g));
        assertTrue(NodeHelper.isTreeVisible(c));
        assertFalse(NodeHelper.isTreeShowing(g));
        assertFalse(NodeHelper.isTreeShowing(c));

    }

    @Test
    public void testSynchronizationOfInvisibleNodes_2() {
        final Group g = new Group();
        final Circle c = new CircleTest.StubCircle(50);

        Scene s = new Scene(g);
        Stage st = new Stage();
        st.show();
        st.setScene(s);

        final NGGroup sg = NodeHelper.getPeer(g);
        final CircleTest.StubNGCircle sc = NodeHelper.getPeer(c);

        ParentShim.getChildren(g).add(c);

        SceneShim.scenePulseListener_pulse(s);

        g.setVisible(false);

        SceneShim.scenePulseListener_pulse(s);

        assertFalse(sg.isVisible());
        assertTrue(sc.isVisible());

        c.setCenterX(10);             // Make the circle dirty. It won't be synchronized as it is practically invisible (through the parent)

        SceneShim.scenePulseListener_pulse(s);

        c.setVisible(false);         // As circle is invisible and dirty, this won't trigger a synchronization

        SceneShim.scenePulseListener_pulse(s);

        assertFalse(sg.isVisible());
        assertTrue(sc.isVisible()); // This has not been synchronized, as it's not necessary
                                    // The rendering will stop at the Group, which is invisible

        g.setVisible(true);

        SceneShim.scenePulseListener_pulse(s);

        assertTrue(sg.isVisible());
        assertFalse(sc.isVisible()); // Now the group is visible again, we need to synchronize also
                                     // the Circle
    }

    @Test
    public void testSynchronizationOfInvisibleNodes_2_withClip() {
        final Group g = new Group();
        final Circle c = new CircleTest.StubCircle(50);

        Scene s = new Scene(g);
        Stage st = new Stage();
        st.show();
        st.setScene(s);

        final NGGroup sg = NodeHelper.getPeer(g);
        final CircleTest.StubNGCircle sc = NodeHelper.getPeer(c);

        g.setClip(c);

        SceneShim.scenePulseListener_pulse(s);

        g.setVisible(false);

        SceneShim.scenePulseListener_pulse(s);

        assertFalse(sg.isVisible());
        assertTrue(sc.isVisible());

        c.setCenterX(10);             // Make the circle dirty. It won't be synchronized as it is practically invisible (through the parent)

        SceneShim.scenePulseListener_pulse(s);

        c.setVisible(false);         // As circle is invisible and dirty, this won't trigger a synchronization

        SceneShim.scenePulseListener_pulse(s);

        assertFalse(sg.isVisible());
        assertTrue(sc.isVisible()); // This has not been synchronized, as it's not necessary
                                    // The rendering will stop at the Group, which is invisible

        g.setVisible(true);

        SceneShim.scenePulseListener_pulse(s);

        assertTrue(sg.isVisible());
        assertFalse(sc.isVisible()); // Now the group is visible again, we need to synchronize also
                                     // the Circle
    }

    @Test
    public void testLocalToScreen() {
        Rectangle rect = new Rectangle();

        rect.setTranslateX(10);
        rect.setTranslateY(20);

        TestScene scene = new TestScene(new Group(rect));
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        scene.set_window(testStage);
        Point2D p = rect.localToScreen(new Point2D(1, 2));
        assertEquals(111.0, p.getX(), 0.0001);
        assertEquals(222.0, p.getY(), 0.0001);
        Bounds b = rect.localToScreen(new BoundingBox(1, 2, 3, 4));
        assertEquals(111.0, b.getMinX(), 0.0001);
        assertEquals(222.0, b.getMinY(), 0.0001);
        assertEquals(3.0, b.getWidth(), 0.0001);
        assertEquals(4.0, b.getHeight(), 0.0001);
    }

    @Test
    public void testLocalToScreen3D() {
        Box box = new Box(10, 10, 10);

        box.setTranslateX(10);
        box.setTranslateY(20);

        TestScene scene = new TestScene(new Group(box));
        scene.setCamera(new PerspectiveCamera());
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        scene.set_window(testStage);

        Point2D p = box.localToScreen(new Point3D(1, 2, -5));
        assertEquals(111.42, p.getX(), 0.1);
        assertEquals(223.14, p.getY(), 0.1);
        Bounds b = box.localToScreen(new BoundingBox(1, 2, -5, 1, 2, 10));
        assertEquals(110.66, b.getMinX(), 0.1);
        assertEquals(221.08, b.getMinY(), 0.1);
        assertEquals(1.88, b.getWidth(), 0.1);
        assertEquals(4.3, b.getHeight(), 0.1);
        assertEquals(0.0, b.getDepth(), 0.0001);
    }

    @Test
    public void testScreenToLocal() {
        Rectangle rect = new Rectangle();

        rect.setTranslateX(10);
        rect.setTranslateY(20);

        TestScene scene = new TestScene(new Group(rect));
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        scene.set_window(testStage);

        assertEquals(new Point2D(1, 2), rect.screenToLocal(new Point2D(111, 222)));
        assertEquals(new BoundingBox(1, 2, 3, 4), rect.screenToLocal(new BoundingBox(111, 222, 3, 4)));
    }

    @Test
    public void testLocalToScreenWithTranslatedCamera() {
        Rectangle rect = new Rectangle();

        rect.setTranslateX(10);
        rect.setTranslateY(20);

        ParallelCamera cam = new ParallelCamera();
        TestScene scene = new TestScene(new Group(rect, cam));
        scene.setCamera(cam);
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        cam.setTranslateX(30);
        cam.setTranslateY(20);
        scene.set_window(testStage);

        Point2D p = rect.localToScreen(new Point2D(1, 2));
        assertEquals(81.0, p.getX(), 0.0001);
        assertEquals(202.0, p.getY(), 0.0001);
        Bounds b = rect.localToScreen(new BoundingBox(1, 2, 3, 4));
        assertEquals(81.0, b.getMinX(), 0.0001);
        assertEquals(202.0, b.getMinY(), 0.0001);
        assertEquals(3.0, b.getWidth(), 0.0001);
        assertEquals(4.0, b.getHeight(), 0.0001);
    }

    @Test
    public void testScreenToLocalWithTranslatedCamera() {
        Rectangle rect = new Rectangle();

        rect.setTranslateX(10);
        rect.setTranslateY(20);

        ParallelCamera cam = new ParallelCamera();
        TestScene scene = new TestScene(new Group(rect, cam));
        scene.setCamera(cam);
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        cam.setTranslateX(30);
        cam.setTranslateY(20);
        scene.set_window(testStage);

        assertEquals(new Point2D(31, 22), rect.screenToLocal(new Point2D(111, 222)));
        assertEquals(new BoundingBox(31, 22, 3, 4), rect.screenToLocal(new BoundingBox(111, 222, 3, 4)));
    }

    @Test
    public void testLocalToScreenInsideSubScene() {
        Rectangle rect = new Rectangle();
        rect.setTranslateX(4);
        rect.setTranslateY(9);
        SubScene subScene = new SubScene(new Group(rect), 100, 100);
        subScene.setTranslateX(6);
        subScene.setTranslateY(11);

        TestScene scene = new TestScene(new Group(subScene));
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        scene.set_window(testStage);

        Point2D p = rect.localToScreen(new Point2D(1, 2));
        assertEquals(111.0, p.getX(), 0.0001);
        assertEquals(222.0, p.getY(), 0.0001);
        Bounds b = rect.localToScreen(new BoundingBox(1, 2, 3, 4));
        assertEquals(111.0, b.getMinX(), 0.0001);
        assertEquals(222.0, b.getMinY(), 0.0001);
        assertEquals(3.0, b.getWidth(), 0.0001);
        assertEquals(4.0, b.getHeight(), 0.0001);
    }

    @Test
    public void testScreenToLocalInsideSubScene() {
        Rectangle rect = new Rectangle();
        rect.setTranslateX(4);
        rect.setTranslateY(9);
        SubScene subScene = new SubScene(new Group(rect), 100, 100);
        subScene.setTranslateX(6);
        subScene.setTranslateY(11);

        TestScene scene = new TestScene(new Group(subScene));
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        scene.set_window(testStage);

        assertEquals(new Point2D(1, 2), rect.screenToLocal(new Point2D(111, 222)));
        assertEquals(new BoundingBox(1, 2, 3, 4), rect.screenToLocal(new BoundingBox(111, 222, 3, 4)));
    }

    @Test
    public void test2DLocalToScreenOn3DRotatedSubScene() {
        Rectangle rect = new Rectangle();
        rect.setTranslateX(5);
        rect.setTranslateY(10);
        SubScene subScene = new SubScene(new Group(rect), 100, 100);
        subScene.setTranslateX(5);
        subScene.setTranslateY(10);
        subScene.setRotationAxis(Rotate.Y_AXIS);
        subScene.setRotate(40);

        TestScene scene = new TestScene(new Group(subScene));
        scene.setCamera(new PerspectiveCamera());
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        scene.set_window(testStage);

        Point2D p = rect.localToScreen(new Point2D(1, 2));
        assertEquals(124.36, p.getX(), 0.1);
        assertEquals(226.0, p.getY(), 0.1);
        Bounds b = rect.localToScreen(new BoundingBox(1, 2, 3, 4));
        assertEquals(124.36, b.getMinX(), 0.1);
        assertEquals(225.75, b.getMinY(), 0.1);
        assertEquals(1.85, b.getWidth(), 0.1);
        assertEquals(3.76, b.getHeight(), 0.1);
    }

    @Test
    public void test2DScreenToLocalTo3DRotatedSubScene() {
        Rectangle rect = new Rectangle();
        rect.setTranslateX(5);
        rect.setTranslateY(10);
        SubScene subScene = new SubScene(new Group(rect), 100, 100);
        subScene.setTranslateX(5);
        subScene.setTranslateY(10);
        subScene.setRotationAxis(Rotate.Y_AXIS);
        subScene.setRotate(40);

        TestScene scene = new TestScene(new Group(subScene));
        scene.setCamera(new PerspectiveCamera());
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        scene.set_window(testStage);

        Point2D p = rect.screenToLocal(new Point2D(124.36, 226.0));
        assertEquals(1, p.getX(), 0.1);
        assertEquals(2, p.getY(), 0.1);
        Bounds b = rect.screenToLocal(new BoundingBox(124.36, 225.75, 1.85, 3.76));
        assertEquals(1, b.getMinX(), 0.1);
        assertEquals(1.72, b.getMinY(), 0.1);
        assertEquals(3, b.getWidth(), 0.1);
        assertEquals(4.52, b.getHeight(), 0.1);
    }

    @Test
    public void testScreenToLocalWithNonInvertibleTransform() {
        Rectangle rect = new Rectangle();

        rect.setScaleX(0.0);

        TestScene scene = new TestScene(new Group(rect));
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        scene.set_window(testStage);

        assertNull(rect.screenToLocal(new Point2D(111, 222)));
        assertNull(rect.screenToLocal(new BoundingBox(111, 222, 3, 4)));
    }

    @Test
    public void testScreenToLocalInsideNonInvertibleSubScene() {
        Rectangle rect = new Rectangle();
        rect.setTranslateX(4);
        rect.setTranslateY(9);
        SubScene subScene = new SubScene(new Group(rect), 100, 100);
        subScene.setScaleX(0.0);

        TestScene scene = new TestScene(new Group(subScene));
        final TestStage testStage = new TestStage("");
        testStage.setX(100);
        testStage.setY(200);
        scene.set_window(testStage);

        assertNull(rect.screenToLocal(new Point2D(111, 222)));
        assertNull(rect.screenToLocal(new BoundingBox(111, 222, 3, 4)));
    }

    @Test
    public void testRootMirroringWithTranslate() {
        final Group rootGroup = new Group();
        rootGroup.setTranslateX(20);
        rootGroup.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        final Scene scene = new Scene(rootGroup, 200, 200);

        final Point2D trPoint = scene.getRoot().localToScene(0, 0);
        assertEquals(180, trPoint.getX(), 0.1);
    }


    @Test
    public void testLayoutXYTriggersParentSizeChange() {
        final Group rootGroup = new Group();
        final Group subGroup = new Group();
        ParentShim.getChildren(rootGroup).add(subGroup);

        Rectangle r = new Rectangle(50,50);
        r.setManaged(false);
        Rectangle staticR = new Rectangle(1,1);
        ParentShim.getChildren(subGroup).addAll(r, staticR);

        assertEquals(50,subGroup.getLayoutBounds().getWidth(), 1e-10);
        assertEquals(50,subGroup.getLayoutBounds().getHeight(), 1e-10);

        r.setLayoutX(50);

        rootGroup.layout();

        assertEquals(100,subGroup.getLayoutBounds().getWidth(), 1e-10);
        assertEquals(50,subGroup.getLayoutBounds().getHeight(), 1e-10);

    }

    @Test
    public void testLayoutXYWontBreakLayout() {
        final Group rootGroup = new Group();
        final AnchorPane pane = new AnchorPane();
        ParentShim.getChildren(rootGroup).add(pane);

        Rectangle r = new Rectangle(50,50);
        ParentShim.getChildren(pane).add(r);

        AnchorPane.setLeftAnchor(r, 10d);
        AnchorPane.setTopAnchor(r, 10d);

        rootGroup.layout();

        assertEquals(10, r.getLayoutX(), 1e-10);
        assertEquals(10, r.getLayoutY(), 1e-10);

        r.setLayoutX(50);

        assertEquals(50, r.getLayoutX(), 1e-10);
        assertEquals(10, r.getLayoutY(), 1e-10);

        rootGroup.layout();

        assertEquals(10, r.getLayoutX(), 1e-10);
        assertEquals(10, r.getLayoutY(), 1e-10);

    }

    @Test
    public void clipShouldUpdateAfterParentVisibilityChange() {

        final Group root = new Group();
        Scene scene = new Scene(root, 300, 300);

        final Group parent = new Group();
        parent.setVisible(false);

        final Circle circle = new Circle(100, 100, 100);
        ParentShim.getChildren(parent).add(circle);

        final Rectangle clip = new StubRect(100, 100);
        circle.setClip(clip);

        ParentShim.getChildren(root).add(parent);
        parent.setVisible(true);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        ((StubToolkit) Toolkit.getToolkit()).firePulse();

        clip.setWidth(300);

        ((StubToolkit) Toolkit.getToolkit()).firePulse();

        assertEquals(300, ((MockNGRect) NodeHelper.getPeer(clip)).w, 1e-10);
    }

    @Test
    public void untransformedNodeShouldSyncIdentityTransform() {
        final Node node = createTestRect();
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(BaseTransform.IDENTITY_TRANSFORM,
                ((MockNGRect) NodeHelper.getPeer(node)).t);
    }

    @Test
    public void nodeTransfomedByIdentitiesShouldSyncIdentityTransform() {
        final Node node = createTestRect();
        node.setRotationAxis(Rotate.X_AXIS);
        node.getTransforms().add(new Translate());
        node.getTransforms().add(new Scale());
        node.getTransforms().add(new Affine());
        node.getTransforms().add(new Rotate(0, Rotate.Y_AXIS));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(BaseTransform.IDENTITY_TRANSFORM,
                ((MockNGRect) NodeHelper.getPeer(node)).t);
    }

    @Test
    public void translatedNodeShouldSyncTranslateTransform1() {
        final Node node = createTestRect();
        node.setTranslateX(30);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Translate2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void translatedNodeShouldSyncTranslateTransform2() {
        final Node node = createTestRect();
        node.getTransforms().add(new Translate(20, 10));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Translate2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void multitranslatedNodeShouldSyncTranslateTransform() {
        final Node node = createTestRect();
        node.setTranslateX(30);
        node.getTransforms().add(new Translate(20, 10));
        node.getTransforms().add(new Translate(10, 20));
        node.getTransforms().add(new Translate(5, 5, 0));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Translate2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void mirroringShouldSyncAffine2DTransform() {
        final Node node = createTestRect();
        node.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void rotatedNodeShouldSyncAffine2DTransform1() {
        final Node node = createTestRect();
        node.setRotate(20);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void rotatedNodeShouldSyncAffine2DTransform2() {
        final Node node = createTestRect();
        node.getTransforms().add(new Rotate(20));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void multiRotatedNodeShouldSyncAffine2DTransform() {
        final Node node = createTestRect();
        node.setRotate(20);
        node.getTransforms().add(new Rotate(20));
        node.getTransforms().add(new Rotate(0, Rotate.X_AXIS));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void scaledNodeShouldSyncAffine2DTransform1() {
        final Node node = createTestRect();
        node.setScaleX(2);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void scaledNodeShouldSyncAffine2DTransform2() {
        final Node node = createTestRect();
        node.getTransforms().add(new Scale(2, 1));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void multiScaledNodeShouldSyncAffine2DTransform() {
        final Node node = createTestRect();
        node.setScaleX(20);
        node.getTransforms().add(new Scale(2, 1));
        node.getTransforms().add(new Scale(0.5, 2, 1));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void shearedNodeShouldSyncAffine2DTransform() {
        final Node node = createTestRect();
        node.getTransforms().add(new Shear(2, 1));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine2D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void ztranslatedNodeShouldSyncAffine3DTransform1() {
        final Node node = createTestRect();
        node.setTranslateZ(30);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine3D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void ztranslatedNodeShouldSyncAffine3DTransform2() {
        final Node node = createTestRect();
        node.getTransforms().add(new Translate(0, 0, 10));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine3D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void zscaledNodeShouldSyncAffine3DTransform1() {
        final Node node = createTestRect();
        node.setScaleZ(0.5);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine3D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void zscaledNodeShouldSyncAffine3DTransform2() {
        final Node node = createTestRect();
        node.getTransforms().add(new Scale(1, 1, 2));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine3D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void nonZRotatedNodeShouldSyncAffine3DTransform1() {
        final Node node = createTestRect();
        node.setRotationAxis(Rotate.Y_AXIS);
        node.setRotate(10);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine3D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void nonZRotatedNodeShouldSyncAffine3DTransform2() {
        final Node node = createTestRect();
        node.getTransforms().add(new Rotate(10, Rotate.X_AXIS));
        ((StubToolkit) Toolkit.getToolkit()).firePulse();
        assertSame(Affine3D.class,
                ((MockNGRect) NodeHelper.getPeer(node)).t.getClass());
    }

    @Test
    public void translateTransformShouldBeReusedWhenPossible() {
        final Node node = createTestRect();
        node.setTranslateX(10);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();

        BaseTransform t = ((MockNGRect) NodeHelper.getPeer(node)).t;

        ((MockNGRect) NodeHelper.getPeer(node)).t = null;
        node.setTranslateX(20);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();

        assertSame(t, ((MockNGRect) NodeHelper.getPeer(node)).t);
    }

    @Test
    public void affine2DTransformShouldBeReusedWhenPossible() {
        final Node node = createTestRect();
        node.setScaleX(10);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();

        BaseTransform t = ((MockNGRect) NodeHelper.getPeer(node)).t;

        ((MockNGRect) NodeHelper.getPeer(node)).t = null;
        node.setRotate(20);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();

        assertSame(t, ((MockNGRect) NodeHelper.getPeer(node)).t);
    }

    @Test
    public void affine3DTransformShouldBeReusedWhenPossible() {
        final Node node = createTestRect();
        node.setScaleZ(10);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();

        BaseTransform t = ((MockNGRect) NodeHelper.getPeer(node)).t;

        ((MockNGRect) NodeHelper.getPeer(node)).t = null;
        node.setRotate(20);
        ((StubToolkit) Toolkit.getToolkit()).firePulse();

        assertSame(t, ((MockNGRect) NodeHelper.getPeer(node)).t);
    }

    @Test
    public void rtlSceneSizeShouldBeComputedCorrectly() {
        Scene scene = new Scene(new Group(new Rectangle(100, 100)));
        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        assertEquals(100.0, scene.getWidth(), 0.00001);
    }

    private Node createTestRect() {
        final Rectangle rect = new StubRect();
        Scene scene = new Scene(new Group(rect));
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        return rect;
    }

    private static class MockNGRect extends NGRectangle {
        double w = 0;
        BaseTransform t = null;

        @Override public void updateRectangle(float x, float y, float width,
                float height, float arcWidth, float arcHeight) {
            w = width;
        }

        @Override
        public void setTransformMatrix(BaseTransform tx) {
            t = tx;
        }
    }

    static class StubRect extends Rectangle {
        static {
            StubRectHelper.setStubRectAccessor(new StubRectHelper.StubRectAccessor() {
                @Override
                public NGNode doCreatePeer(Node node) {
                    return ((StubRect) node).doCreatePeer();
                }
            });
        }

        StubRect() {
            super();
            StubRectHelper.initHelper(this);
        }

        StubRect(double width, double height) {
            super(width, height);
            StubRectHelper.initHelper(this);
        }

        private NGNode doCreatePeer() {
            return new MockNGRect();
        }
    }

    public static class StubRectHelper extends RectangleHelper {

        private static final StubRectHelper theInstance;
        private static StubRectAccessor stubRectAccessor;

        static {
            theInstance = new StubRectHelper();
            Utils.forceInit(StubRect.class);
        }

        private static StubRectHelper getInstance() {
            return theInstance;
        }

        public static void initHelper(StubRect stubRect) {
            setHelper(stubRect, getInstance());
        }

        public static void setStubRectAccessor(final StubRectAccessor newAccessor) {
            if (stubRectAccessor != null) {
                throw new IllegalStateException();
            }

            stubRectAccessor = newAccessor;
        }

        @Override
        protected NGNode createPeerImpl(Node node) {
            return stubRectAccessor.doCreatePeer(node);
        }

        public interface StubRectAccessor {
            NGNode doCreatePeer(Node node);
        }

    }
}
