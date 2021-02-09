/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.TreeShowingExpression;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class TreeShowingExpressionTest {
    private final Parent root;
    private final Node node;
    private final TreeShowingExpression expression;

    @Parameters
    public static Collection<Object[]> parameters() {
        Supplier<RootAndNodeToTest> supplier1 = () -> {
            Node node = new StackPane();
            return new RootAndNodeToTest(new StackPane(node), node);
        };

        Supplier<RootAndNodeToTest> supplier2 = () -> {
            StackPane node = new StackPane();
            return new RootAndNodeToTest(new StackPane(new SubScene(node, 100.0, 100.0)), node);
        };

        return Arrays.asList(new Object[][] { { supplier1 }, { supplier2 } });
    }

    static class RootAndNodeToTest {
        RootAndNodeToTest(Parent root, Node nodeToTest) {
            this.root = root;
            this.nodeToTest = nodeToTest;
        }

        Parent root;
        Node nodeToTest;
    }

    public TreeShowingExpressionTest(Supplier<RootAndNodeToTest> nodeSupplier) {
        RootAndNodeToTest nodes = nodeSupplier.get();

        this.root = nodes.root;
        this.node = nodes.nodeToTest;
        this.expression = new TreeShowingExpression(this.node);
    }

    @Test
    public void nodeNotAttachedToSceneShouldNotBeShowing() {
        assertFalse(expression.get());
    }

    @Test
    public void getShouldTrackChangesInShowingStateForGivenNode() {
        assertFalse(expression.get());  // not showing initially as not attached to a Scene

        Scene scene = new Scene(root);

        assertFalse(expression.get());  // not showing because Scene is not attached to a Window

        Stage stage = new Stage();
        stage.setScene(scene);

        assertFalse(expression.get());  // not showing as Window is not shown

        stage.show();

        assertTrue(expression.get());  // showing as Window is shown

        stage.hide();

        assertFalse(expression.get());  // not showing again as Window is hidden
    }

    @Test
    public void changeListenerShouldRegisterAndUnregisterCorrectly() {
        AtomicReference<Boolean> state = new AtomicReference<>();
        ChangeListener<Boolean> listener = (obs, old, current) -> state.set(current);

        expression.addListener(listener);

        assertNull(state.getAndSet(null));  // no change fired so far

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.show();

        assertTrue(state.getAndSet(null));  // expect a change indicating the node is showing now

        expression.removeListener(listener);

        stage.hide();

        assertNull(state.getAndSet(null));  // no change fired as listener was unregistered
    }

    @Test
    public void invalidationListenerShouldRegisterAndUnregisterCorrectly() {
        AtomicReference<Boolean> state = new AtomicReference<>();
        InvalidationListener listener = obs -> state.set(true);

        expression.addListener(listener);

        assertNull(state.getAndSet(null));  // no invalidation fired so far

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.show();

        assertTrue(state.getAndSet(null));  // expect an invalidation as node is showing now

        expression.get();  // make valid again
        expression.removeListener(listener);

        stage.hide();

        assertNull(state.getAndSet(null));  // expect no invalidation as listener was unregistered
    }

    @Test
    public void changeListenerShouldTrackShowingState() {
        AtomicReference<Boolean> state = new AtomicReference<>();

        expression.addListener((obs, old, current) -> state.set(current));

        assertNull(state.getAndSet(null));  // no change fired so far

        Scene scene = new Scene(root);

        assertNull(state.getAndSet(null));  // attaching to an invisible Scene fires no change

        Stage stage = new Stage();
        stage.setWidth(100);
        stage.setHeight(100);
        stage.setScene(scene);

        assertNull(state.getAndSet(null));  // attaching to an invisible Scene fires no change

        stage.show();

        assertTrue(state.getAndSet(null));  // expect a change indicating the node is showing now

        stage.setScene(null);

        assertFalse(state.getAndSet(null));  // detaching stage from scene should fire not showing change

        stage.setScene(scene);

        assertTrue(state.getAndSet(null));  // reattaching stage should fire showing change

        stage.hide();

        assertFalse(state.getAndSet(null));  // expect a change indicating the node is no longer showing

        Stage stage2 = new Stage();
        stage2.setWidth(100);
        stage2.setHeight(100);
        stage2.show();
        stage2.setScene(scene);

        assertTrue(state.getAndSet(null));  // switching between invisible/visible Scene should trigger showing change

        stage2.hide();

        assertFalse(state.getAndSet(null));  // hiding attached window should trigger not showing change

        stage.show();

        assertNull(state.getAndSet(null));  // changing visibility of unattached stage should not do anything

        scene.setRoot(new StackPane());
        Scene scene2 = new Scene(root);
        stage.setScene(scene2);

        assertTrue(state.getAndSet(null));  // making root part of a different visible scene should trigger showing change
    }

    @Test
    public void invalidationListenerShouldNotifyOfChangesInShowingState() {
        AtomicReference<Boolean> state = new AtomicReference<>();

        expression.addListener(obs -> state.set(true));

        assertNull(state.getAndSet(null));  // no invalidation fired so far

        Scene scene = new Scene(root);

        assertNull(state.getAndSet(null));  // attaching to an invisible Scene fires no invalidation

        Stage stage = new Stage();
        stage.setWidth(100);
        stage.setHeight(100);
        stage.setScene(scene);

        assertNull(state.getAndSet(null));  // attaching to an invisible Scene fires no invalidation

        stage.show();

        assertTrue(state.getAndSet(null));  // expect an invalidation as the node is showing now

        expression.get();  // make valid
        stage.setScene(null);

        assertTrue(state.getAndSet(null));  // detaching stage from scene should fire invalidation

        expression.get();  // make valid
        stage.setScene(scene);

        assertTrue(state.getAndSet(null));  // reattaching stage should fire invalidation

        // didn't make valid here
        stage.hide();

        assertNull(state.getAndSet(null));  // expect nothing as expression still invalid

        stage.show();
        expression.get();  // make valid
        stage.hide();

        assertTrue(state.getAndSet(null));  // expect an invalidation as the node is no longer showing now

        Stage stage2 = new Stage();
        stage2.setWidth(100);
        stage2.setHeight(100);
        stage2.show();
        expression.get();  // make valid
        stage2.setScene(scene);

        assertTrue(state.getAndSet(null));  // switching between invisible/visible Scene should trigger invalidation

        expression.get();  // make valid
        stage2.hide();

        assertTrue(state.getAndSet(null));  // hiding attached window should trigger invalidation

        expression.get();  // make valid
        stage.show();

        assertNull(state.getAndSet(null));  // changing visibility of unattached stage should not do anything

        scene.setRoot(new StackPane());
        Scene scene2 = new Scene(root);
        expression.get();  // make valid
        stage.setScene(scene2);

        assertTrue(state.getAndSet(null));  // making root part of a different visible scene should trigger invalidation
    }

    @Test
    public void disposeShouldUnregisterListenersOnGivenNode() {
        AtomicReference<Boolean> state = new AtomicReference<>();

        expression.addListener((obs, old, current) -> state.set(current));

        // verify change listener works:
        Stage stage = new Stage();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
        assertTrue(state.getAndSet(null));

        expression.dispose();

        // verify change listener no longer responds:
        stage.hide();
        assertNull(state.getAndSet(null));

        // another check:
        Stage stage2 = new Stage();
        stage2.setWidth(100);
        stage2.setHeight(100);
        stage2.show();
        scene.setRoot(new StackPane());
        stage2.setScene(new Scene(root));
        assertNull(state.getAndSet(null));
    }
}
