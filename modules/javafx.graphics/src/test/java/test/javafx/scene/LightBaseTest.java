/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @BeforeEach
    public void setUp() {
        stage.setScene(new Scene(root));
        stage.show();
    }

    @AfterEach
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
        assertTrue(scope.isEmpty(), "Scope list should be empty");
        assertTrue(exclusionScope.isEmpty(), "Exclusion scope should be empty");

        toolkit.fireTestPulse();
        assertTrue(isAffected(shape1), SCOPE_EMPTY_AFFECTED);
        assertTrue(isAffected(shape2), SCOPE_EMPTY_AFFECTED);
        assertTrue(isAffected(shape3), SCOPE_EMPTY_AFFECTED);
        assertTrue(isAffected(shape4), SCOPE_EMPTY_AFFECTED);
    }

    private void addShape1ToScope() {
        scope.add(shape1);
        assertTrue(scope.contains(shape1)); // shap, ADD_SCOPEe1
        assertFalse(exclusionScope.contains(shape1), NO_CHANGE_EXC_SCOPE);

        assertTrue(isDrawModeDirty(shape1), CHANGE_DIRTY);
        // shapes 2-4 fell out of scope when it ceased being empty so they need a redraw (depending on their parents'
        // inclusion in the scopes too). However, we are testing here only markChildrenDirty. Redraw happens anyway for
        // all shapes through the scene when NodeHelper.markDirty is called.
        assertFalse(isDrawModeDirty(shape2), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape3), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape4), NO_CHANGE_NOT_DIRTY);

        toolkit.fireTestPulse();
        assertTrue(isAffected(shape1), IN_SCOPE_AFFECTED);
        assertFalse(isAffected(shape2), NOT_IN_SCOPE_NOT_AFFECTED);
        assertFalse(isAffected(shape3), NOT_IN_SCOPE_NOT_AFFECTED);
        assertFalse(isAffected(shape4), NOT_IN_SCOPE_NOT_AFFECTED);
    }

    private void addParent1ToScope() {
        scope.add(parent1);
        assertTrue(scope.contains(parent1)); // shape1, paren, ADD_SCOPEt1
        assertFalse(exclusionScope.contains(parent1), NO_CHANGE_EXC_SCOPE);

        assertFalse(isDrawModeDirty(shape1), NO_CHANGE_NOT_DIRTY);
        assertTrue(isDrawModeDirty(shape2), PARENT_CHANGE_DIRTY);
        assertFalse(isDrawModeDirty(shape3), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape4), NO_CHANGE_NOT_DIRTY);

        toolkit.fireTestPulse();
        assertTrue(isAffected(shape1), IN_SCOPE_AFFECTED);
        assertTrue(isAffected(shape2), PARENT_IN_SCOPE_AFFECTED);
        assertFalse(isAffected(shape3), NOT_IN_SCOPE_NOT_AFFECTED);
        assertFalse(isAffected(shape4), NOT_IN_SCOPE_NOT_AFFECTED);
    }

    private void addShape2ToExcScope() {
        exclusionScope.add(shape2);
        assertFalse(scope.contains(shape2)); // shape1, paren, NO_CHANGE_SCOPEt1
        assertTrue(exclusionScope.contains(shape2)); // shap, ADD_EXC_SCOPEe2

        assertFalse(isDrawModeDirty(shape1), NO_CHANGE_NOT_DIRTY);
        assertTrue(isDrawModeDirty(shape2), CHANGE_DIRTY);
        assertFalse(isDrawModeDirty(shape3), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape4), NO_CHANGE_NOT_DIRTY);

        toolkit.fireTestPulse();
        assertTrue(isAffected(shape1), IN_SCOPE_AFFECTED);
        assertFalse(isAffected(shape2), IN_EXC_SCOPE_NOT_AFFECTED);
        assertFalse(isAffected(shape3), NOT_IN_SCOPE_NOT_AFFECTED);
        assertFalse(isAffected(shape4), NOT_IN_SCOPE_NOT_AFFECTED);
    }

    private void addRootToScope() {
        scope.add(root);
        assertTrue(scope.contains(root)); // shape1, parent1, ro, ADD_SCOPEot
        assertFalse(exclusionScope.contains(root)); // shap, NO_CHANGE_EXC_SCOPEe2

        assertFalse(isDrawModeDirty(shape1), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape2), NO_CHANGE_NOT_DIRTY);
        assertTrue(isDrawModeDirty(shape3), PARENT_CHANGE_DIRTY);
        assertTrue(isDrawModeDirty(shape4), PARENT_CHANGE_DIRTY);

        toolkit.fireTestPulse();
        assertTrue(isAffected(shape1), IN_SCOPE_AFFECTED);
        assertFalse(isAffected(shape2), IN_EXC_SCOPE_NOT_AFFECTED);
        assertTrue(isAffected(shape3), PARENT_IN_SCOPE_AFFECTED);
        assertTrue(isAffected(shape4), PARENT_IN_SCOPE_AFFECTED);
    }

    private void moveParent1ToExcScope() {
        exclusionScope.add(parent1);
        assertFalse(scope.contains(parent1)); // shape1, ro, SILENT_REMOVE_SCOPEot
        assertTrue(exclusionScope.contains(parent1)); // shape2, paren, ADD_EXC_SCOPEt1

        assertFalse(isDrawModeDirty(shape1), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape2), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape3), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape4), NO_CHANGE_NOT_DIRTY);

        toolkit.fireTestPulse();
        assertTrue(isAffected(shape1), IN_SCOPE_AFFECTED);
        assertFalse(isAffected(shape2), IN_EXC_SCOPE_NOT_AFFECTED);
        assertTrue(isAffected(shape3), PARENT_IN_SCOPE_AFFECTED);
        assertTrue(isAffected(shape4), PARENT_IN_SCOPE_AFFECTED);
    }

    private void moveShape2ToScope() {
        scope.add(shape2);
        assertTrue(scope.contains(shape2)); // shape1, root, shap, ADD_SCOPEe2
        assertFalse(exclusionScope.contains(shape2)); // paren, SILENT_REMOVE_EXC_SCOPEt1

        assertFalse(isDrawModeDirty(shape1), NO_CHANGE_NOT_DIRTY);
        assertTrue(isDrawModeDirty(shape2), CHANGE_DIRTY);
        assertFalse(isDrawModeDirty(shape3), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape4), NO_CHANGE_NOT_DIRTY);

        toolkit.fireTestPulse();
        assertTrue(isAffected(shape1), IN_SCOPE_AFFECTED);
        assertTrue(isAffected(shape2), IN_SCOPE_AFFECTED);
        assertTrue(isAffected(shape3), PARENT_IN_SCOPE_AFFECTED);
        assertTrue(isAffected(shape4), PARENT_IN_SCOPE_AFFECTED);
    }

    private void removeShape1FromScope() {
        scope.remove(shape1);
        assertFalse(scope.contains(shape1)); // root, shap, REMOVE_SCOPEe2
        assertFalse(exclusionScope.contains(shape1)); // paren, NO_CHANGE_EXC_SCOPEt1

        assertTrue(isDrawModeDirty(shape1), CHANGE_DIRTY);
        assertFalse(isDrawModeDirty(shape2), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape3), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape4), NO_CHANGE_NOT_DIRTY);

        toolkit.fireTestPulse();
        assertFalse(isAffected(shape1), PARENT_IN_EXC_SCOPE_NOT_AFFECTED);
        assertTrue(isAffected(shape2), IN_SCOPE_AFFECTED);
        assertTrue(isAffected(shape3), PARENT_IN_SCOPE_AFFECTED);
        assertTrue(isAffected(shape4), PARENT_IN_SCOPE_AFFECTED);
    }

    private void removeRootFromScope() {
        scope.remove(root);
        assertFalse(scope.contains(root)); // shap, REMOVE_SCOPEe2
        assertFalse(exclusionScope.contains(root)); // paren, NO_CHANGE_EXC_SCOPEt1

        assertFalse(isDrawModeDirty(shape1), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape2), NO_CHANGE_NOT_DIRTY);
        assertTrue(isDrawModeDirty(shape3), PARENT_CHANGE_DIRTY);
        assertTrue(isDrawModeDirty(shape4), PARENT_CHANGE_DIRTY);

        toolkit.fireTestPulse();
        assertFalse(isAffected(shape1), PARENT_IN_EXC_SCOPE_NOT_AFFECTED);
        assertTrue(isAffected(shape2), IN_SCOPE_AFFECTED);
        assertFalse(isAffected(shape3), NOT_IN_SCOPE_NOT_AFFECTED);
        assertFalse(isAffected(shape4), NOT_IN_SCOPE_NOT_AFFECTED);
    }

    private void moveShape2ToExcScope() {
        exclusionScope.add(shape2);
        assertFalse(scope.contains(shape2), SILENT_REMOVE_SCOPE);
        assertTrue(exclusionScope.contains(shape2)); // parent1, shap, ADD_EXC_SCOPEe2

        assertFalse(isDrawModeDirty(shape1), NO_CHANGE_NOT_DIRTY);
        assertTrue(isDrawModeDirty(shape2), CHANGE_DIRTY);
        assertFalse(isDrawModeDirty(shape3), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape4), NO_CHANGE_NOT_DIRTY);

        toolkit.fireTestPulse();
        assertFalse(isAffected(shape1), PARENT_IN_EXC_SCOPE_NOT_AFFECTED);
        assertFalse(isAffected(shape2), IN_EXC_SCOPE_NOT_AFFECTED);
        assertTrue(isAffected(shape3), SCOPE_EMPTY_AFFECTED);
        assertTrue(isAffected(shape4), SCOPE_EMPTY_AFFECTED);
    }

    private void moveParent1ToScope() {
        scope.add(parent1);
        assertTrue(scope.contains(parent1)); // paren, ADD_SCOPEt1
        assertFalse(exclusionScope.contains(parent1)); // shap, SILENT_REMOVE_EXC_SCOPEe2

        assertTrue(isDrawModeDirty(shape1), PARENT_CHANGE_DIRTY);
        assertFalse(isDrawModeDirty(shape2), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape3), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape4), NO_CHANGE_NOT_DIRTY);

        toolkit.fireTestPulse();
        assertTrue(isAffected(shape1), PARENT_IN_SCOPE_AFFECTED);
        assertFalse(isAffected(shape2), IN_EXC_SCOPE_NOT_AFFECTED);
        assertFalse(isAffected(shape3), NOT_IN_SCOPE_NOT_AFFECTED);
        assertFalse(isAffected(shape4), NOT_IN_SCOPE_NOT_AFFECTED);
    }

    private void removeShape2FromExcScope() {
        exclusionScope.remove(shape2);
        assertFalse(scope.contains(shape2)); // paren, NO_CHANGE_SCOPEt1
        assertFalse(exclusionScope.contains(shape2), REMOVE_EXC_SCOPE);

        assertFalse(isDrawModeDirty(shape1), NO_CHANGE_NOT_DIRTY);
        assertTrue(isDrawModeDirty(shape2), CHANGE_DIRTY);
        assertFalse(isDrawModeDirty(shape3), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape4), NO_CHANGE_NOT_DIRTY);

        toolkit.fireTestPulse();
        assertTrue(isAffected(shape1), PARENT_IN_SCOPE_AFFECTED);
        assertTrue(isAffected(shape2), PARENT_IN_SCOPE_AFFECTED);
        assertFalse(isAffected(shape3), NOT_IN_SCOPE_NOT_AFFECTED);
        assertFalse(isAffected(shape4), NOT_IN_SCOPE_NOT_AFFECTED);
    }

    private void removeParent1FromScope() {
        scope.remove(parent1);
        assertFalse(scope.contains(parent1), REMOVE_SCOPE);
        assertFalse(exclusionScope.contains(parent1), NO_CHANGE_EXC_SCOPE);

        assertTrue(isDrawModeDirty(shape1), PARENT_CHANGE_DIRTY);
        assertTrue(isDrawModeDirty(shape2), PARENT_CHANGE_DIRTY);
        // Shapes 3 and 4 require redraw because scope became empty, but they are not marked dirty by markChildrenDirty
        // because they or their parents didn't change scope.
        assertFalse(isDrawModeDirty(shape3), NO_CHANGE_NOT_DIRTY);
        assertFalse(isDrawModeDirty(shape4), NO_CHANGE_NOT_DIRTY);

        toolkit.fireTestPulse();
        assertTrue(isAffected(shape1), SCOPE_EMPTY_AFFECTED);
        assertTrue(isAffected(shape2), SCOPE_EMPTY_AFFECTED);
        assertTrue(isAffected(shape3), SCOPE_EMPTY_AFFECTED);
        assertTrue(isAffected(shape4), SCOPE_EMPTY_AFFECTED);
    }

    private void verifyEmpty() {
        assertTrue(scope.isEmpty(), "Scope is empty");
        assertTrue(exclusionScope.isEmpty(), "Exclusion scope is empty");
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