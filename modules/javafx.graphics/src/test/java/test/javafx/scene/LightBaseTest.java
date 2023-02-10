/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.*;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.NodeShim;
import javafx.scene.Parent;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.prism.NGLightBaseShim;
import com.sun.javafx.sg.prism.NGPointLight;
import com.sun.javafx.sg.prism.NGShape3D;
import com.sun.javafx.tk.Toolkit;

import test.com.sun.javafx.pgstub.StubToolkit;

public class LightBaseTest {

    private static final String ADD_SCOPE = "Node added to scope, should be contained";
    private static final String ADD_EXC_SCOPE = "Node added to exclusion scope, should be contained";
    private static final String REMOVE_SCOPE = "Node removed from scope, should not be contained";
    private static final String REMOVE_EXC_SCOPE = "Node removed from exclusion scope, should not be contained";
    private static final String SILENT_REMOVE_SCOPE = "Node silently removed from scope, should not be contained";
    private static final String SILENT_REMOVE_EXC_SCOPE = "Node silently removed from exclusion scope, should not be contained";
    private static final String NO_CHANGE_SCOPE = "Node not added to scope, should not be contained";
    private static final String NO_CHANGE_EXC_SCOPE = "Node not added to exclusion scope, should not be contained";

    private static final String NO_CHANGE_NOT_DIRTY = "Shape did not change scope, should not be dirty";
    private static final String CHANGE_DIRTY = "Shape changed scope, should be dirty";
    private static final String PARENT_CHANGE_DIRTY = "Parent changed scope, should be dirty";

    private static final String IN_SCOPE_AFFECTED = "Shape in scope, should be affected";
    private static final String PARENT_IN_SCOPE_AFFECTED = "Parent in scope, should be affected";
    private static final String IN_EXC_SCOPE_NOT_AFFECTED = "Shape in exclusion scope, should not be affected";
    private static final String PARENT_IN_EXC_SCOPE_NOT_AFFECTED = "Parent in exclusion scope, should not be affected";
    private static final String NOT_IN_SCOPE_NOT_AFFECTED = "Shape not in scope, should not be affected";
    private static final String SCOPE_EMPTY_AFFECTED = "Scope is empty, should be affected";

    private Shape3D shape1 = new Sphere();
    private Shape3D shape2 = new Sphere();
    private Shape3D shape3 = new Sphere();
    private Shape3D shape4 = new Sphere();

    private Parent parent1 = new Group(shape1, shape2);
    private Parent parent2 = new Group(shape3, shape4);

    private PointLight pointLight = new PointLight();
    private ObservableList<Node> scope = pointLight.getScope();
    private ObservableList<Node> exclusionScope = pointLight.getExclusionScope();

    private Group root = new Group(parent1, parent2, pointLight);

    private StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
    private Stage stage = new Stage();

    @Before
    public void setUp() {
        stage.setScene(new Scene(root));
        stage.show();
    }

    @After
    public void tearDown() {
        stage.close();
    }

    // This test tests several methods together because a single operation causes multiple changes.
    // This avoids a lot of duplication.
    @Test
    public void testMarkChildrenDirtyAndIsAffected() {
        // Each test relies on an initial and a resulting state. To avoid duplication, the resulting state of
        // one test is used as the initial state of the next, so these methods should be executed in order.
        verifyInitialState();
        addShape1ToScope();
        addParent1ToScope();
        addShape2ToExcScope();
        addRootToScope();
        moveParent1ToExcScope();
        moveShape2ToScope();
        removeShape1FromScope();
        removeRootFromScope();
        moveShape2ToExcScope();
        moveParent1ToScope();
        removeShape2FromExcScope();
        removeParent1FromScope();
        verifyEmpty();
    }

    private void verifyInitialState() {
        assertTrue("Scope list should be empty", scope.isEmpty());
        assertTrue("Exclusion scope should be empty", exclusionScope.isEmpty());

        toolkit.fireTestPulse();
        assertTrue(SCOPE_EMPTY_AFFECTED, isAffected(shape1));
        assertTrue(SCOPE_EMPTY_AFFECTED, isAffected(shape2));
        assertTrue(SCOPE_EMPTY_AFFECTED, isAffected(shape3));
        assertTrue(SCOPE_EMPTY_AFFECTED, isAffected(shape4));
    }

    private void addShape1ToScope() {
        scope.add(shape1);
        assertTrue(ADD_SCOPE, scope.contains(shape1)); // shape1
        assertFalse(NO_CHANGE_EXC_SCOPE, exclusionScope.contains(shape1));

        assertTrue(CHANGE_DIRTY, isDrawModeDirty(shape1));
        // shapes 2-4 fell out of scope when it ceased being empty so they need a redraw (depending on their parents'
        // inclusion in the scopes too). However, we are testing here only markChildrenDirty. Redraw happens anyway for
        // all shapes through the scene when NodeHelper.markDirty is called.
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape2));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape3));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertTrue(IN_SCOPE_AFFECTED, isAffected(shape1));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape2));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape3));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape4));
    }

    private void addParent1ToScope() {
        scope.add(parent1);
        assertTrue(ADD_SCOPE, scope.contains(parent1)); // shape1, parent1
        assertFalse(NO_CHANGE_EXC_SCOPE, exclusionScope.contains(parent1));

        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape1));
        assertTrue(PARENT_CHANGE_DIRTY, isDrawModeDirty(shape2));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape3));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertTrue(IN_SCOPE_AFFECTED, isAffected(shape1));
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape2));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape3));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape4));
    }

    private void addShape2ToExcScope() {
        exclusionScope.add(shape2);
        assertFalse(NO_CHANGE_SCOPE, scope.contains(shape2)); // shape1, parent1
        assertTrue(ADD_EXC_SCOPE, exclusionScope.contains(shape2)); // shape2

        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape1));
        assertTrue(CHANGE_DIRTY, isDrawModeDirty(shape2));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape3));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertTrue(IN_SCOPE_AFFECTED, isAffected(shape1));
        assertFalse(IN_EXC_SCOPE_NOT_AFFECTED, isAffected(shape2));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape3));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape4));
    }

    private void addRootToScope() {
        scope.add(root);
        assertTrue(ADD_SCOPE, scope.contains(root)); // shape1, parent1, root
        assertFalse(NO_CHANGE_EXC_SCOPE, exclusionScope.contains(root)); // shape2

        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape1));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape2));
        assertTrue(PARENT_CHANGE_DIRTY, isDrawModeDirty(shape3));
        assertTrue(PARENT_CHANGE_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertTrue(IN_SCOPE_AFFECTED, isAffected(shape1));
        assertFalse(IN_EXC_SCOPE_NOT_AFFECTED, isAffected(shape2));
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape3));
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape4));
    }

    private void moveParent1ToExcScope() {
        exclusionScope.add(parent1);
        assertFalse(SILENT_REMOVE_SCOPE, scope.contains(parent1)); // shape1, root
        assertTrue(ADD_EXC_SCOPE, exclusionScope.contains(parent1)); // shape2, parent1

        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape1));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape2));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape3));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertTrue(IN_SCOPE_AFFECTED, isAffected(shape1));
        assertFalse(IN_EXC_SCOPE_NOT_AFFECTED, isAffected(shape2));
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape3));
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape4));
    }

    private void moveShape2ToScope() {
        scope.add(shape2);
        assertTrue(ADD_SCOPE, scope.contains(shape2)); // shape1, root, shape2
        assertFalse(SILENT_REMOVE_EXC_SCOPE, exclusionScope.contains(shape2)); // parent1

        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape1));
        assertTrue(CHANGE_DIRTY, isDrawModeDirty(shape2));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape3));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertTrue(IN_SCOPE_AFFECTED, isAffected(shape1));
        assertTrue(IN_SCOPE_AFFECTED, isAffected(shape2));
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape3));
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape4));
    }

    private void removeShape1FromScope() {
        scope.remove(shape1);
        assertFalse(REMOVE_SCOPE, scope.contains(shape1)); // root, shape2
        assertFalse(NO_CHANGE_EXC_SCOPE, exclusionScope.contains(shape1)); // parent1

        assertTrue(CHANGE_DIRTY, isDrawModeDirty(shape1));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape2));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape3));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertFalse(PARENT_IN_EXC_SCOPE_NOT_AFFECTED, isAffected(shape1));
        assertTrue(IN_SCOPE_AFFECTED, isAffected(shape2));
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape3));
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape4));
    }

    private void removeRootFromScope() {
        scope.remove(root);
        assertFalse(REMOVE_SCOPE, scope.contains(root)); // shape2
        assertFalse(NO_CHANGE_EXC_SCOPE, exclusionScope.contains(root)); // parent1

        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape1));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape2));
        assertTrue(PARENT_CHANGE_DIRTY, isDrawModeDirty(shape3));
        assertTrue(PARENT_CHANGE_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertFalse(PARENT_IN_EXC_SCOPE_NOT_AFFECTED, isAffected(shape1));
        assertTrue(IN_SCOPE_AFFECTED, isAffected(shape2));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape3));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape4));
    }

    private void moveShape2ToExcScope() {
        exclusionScope.add(shape2);
        assertFalse(SILENT_REMOVE_SCOPE, scope.contains(shape2));
        assertTrue(ADD_EXC_SCOPE, exclusionScope.contains(shape2)); // parent1, shape2

        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape1));
        assertTrue(CHANGE_DIRTY, isDrawModeDirty(shape2));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape3));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertFalse(PARENT_IN_EXC_SCOPE_NOT_AFFECTED, isAffected(shape1));
        assertFalse(IN_EXC_SCOPE_NOT_AFFECTED, isAffected(shape2));
        assertTrue(SCOPE_EMPTY_AFFECTED, isAffected(shape3));
        assertTrue(SCOPE_EMPTY_AFFECTED, isAffected(shape4));
    }

    private void moveParent1ToScope() {
        scope.add(parent1);
        assertTrue(ADD_SCOPE, scope.contains(parent1)); // parent1
        assertFalse(SILENT_REMOVE_EXC_SCOPE, exclusionScope.contains(parent1)); // shape2

        assertTrue(PARENT_CHANGE_DIRTY, isDrawModeDirty(shape1));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape2));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape3));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape1));
        assertFalse(IN_EXC_SCOPE_NOT_AFFECTED, isAffected(shape2));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape3));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape4));
    }

    private void removeShape2FromExcScope() {
        exclusionScope.remove(shape2);
        assertFalse(NO_CHANGE_SCOPE, scope.contains(shape2)); // parent1
        assertFalse(REMOVE_EXC_SCOPE, exclusionScope.contains(shape2));

        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape1));
        assertTrue(CHANGE_DIRTY, isDrawModeDirty(shape2));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape3));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape1));
        assertTrue(PARENT_IN_SCOPE_AFFECTED, isAffected(shape2));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape3));
        assertFalse(NOT_IN_SCOPE_NOT_AFFECTED, isAffected(shape4));
    }

    private void removeParent1FromScope() {
        scope.remove(parent1);
        assertFalse(REMOVE_SCOPE, scope.contains(parent1));
        assertFalse(NO_CHANGE_EXC_SCOPE, exclusionScope.contains(parent1));

        assertTrue(PARENT_CHANGE_DIRTY, isDrawModeDirty(shape1));
        assertTrue(PARENT_CHANGE_DIRTY, isDrawModeDirty(shape2));
        // Shapes 3 and 4 require redraw because scope became empty, but they are not marked dirty by markChildrenDirty
        // because they or their parents didn't change scope.
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape3));
        assertFalse(NO_CHANGE_NOT_DIRTY, isDrawModeDirty(shape4));

        toolkit.fireTestPulse();
        assertTrue(SCOPE_EMPTY_AFFECTED, isAffected(shape1));
        assertTrue(SCOPE_EMPTY_AFFECTED, isAffected(shape2));
        assertTrue(SCOPE_EMPTY_AFFECTED, isAffected(shape3));
        assertTrue(SCOPE_EMPTY_AFFECTED, isAffected(shape4));
    }

    private void verifyEmpty() {
        assertTrue("Scope is empty", scope.isEmpty());
        assertTrue("Exclusion scope is empty", exclusionScope.isEmpty());
    }

    private boolean isAffected(Shape3D shape) {
        var shapePeer = NodeShim.<NGShape3D>getPeer(shape);
        var lightPeer = NodeShim.<NGPointLight>getPeer(pointLight);
        return NGLightBaseShim.affects(lightPeer, shapePeer);
    }

    private boolean isDrawModeDirty(Shape3D shape) {
        return NodeShim.isDirty(shape, DirtyBits.NODE_DRAWMODE);
    }
}