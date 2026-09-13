/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.stage;

import java.util.ArrayList;
import java.util.stream.Stream;

import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Popup;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import test.com.sun.javafx.test.objects.TestGroup;
import test.com.sun.javafx.test.objects.TestNode;
import test.com.sun.javafx.test.objects.TestScene;
import test.com.sun.javafx.test.objects.TestStage;


/**
 * This test specifically checks for ownerNode and ownerWindow properties.
 * Because those properties are set via different show() methods instead of
 * regular setter method, we can't use PropertiesTestBase for this check.
 * Instead we rely on checking this manually.
 */
public final class Popup_owner_Test {

    public static Stream<Arguments> data() {
        ArrayList<Arguments> configurations = new ArrayList<>();
        TestObjects to;

        to = new TestObjects();
        configurations.add(config(to.testPopup, to.testStage1, to.testStage2));

        to = new TestObjects();
        configurations.add(config(to.testPopup, to.testRoot1, to.testRoot2));

        to = new TestObjects();
        configurations.add(config(to.testPopup, to.testNode1, to.testNode2));

        return configurations.stream();
    }

    private static Arguments config(final Popup popup, final Node o1, final Node o2) {
        return Arguments.of(new ConfigurationNode(popup, o1, o2, 2));
    }

    private static Arguments config(final Popup popup, final Stage o1, final Stage o2) {
        return Arguments.of(new ConfigurationStage(popup, o1, o2, 1));
    }

    private static final class TestObjects {
        public final Popup testPopup;
        public final TestNode testNode1;
        public final TestNode testNode2;
        public final TestGroup testRoot1;
        public final TestGroup testRoot2;
        public final TestScene testScene1;
        public final TestScene testScene2;
        public final TestStage testStage1;
        public final TestStage testStage2;

        public TestObjects() {
            testRoot1 = new TestGroup("ROOT_1");
            testRoot2 = new TestGroup("ROOT_2");

            testNode1 = new TestNode("NODE_1");
            testNode2 = new TestNode("NODE_2");

            testRoot1.getChildren().add(testNode1);
            testRoot2.getChildren().add(testNode2);

            testScene1 = new TestScene("SCENE_1", testRoot1);
            testScene2 = new TestScene("SCENE_2", testRoot2);

            testStage1 = new TestStage("STAGE_1");
            testStage2 = new TestStage("STAGE_2");

            testStage1.setScene(testScene1);
            testStage2.setScene(testScene2);

            testPopup = new Popup();
        }
    }

    public static abstract class Configuration {
        public final Popup testPopup;
        public final int expectedListenerCalls;

        public Configuration(Popup popup, int expectedCalls) {
            this.testPopup = popup;
            this.expectedListenerCalls = expectedCalls;
        }

        public abstract void showFirst();
        public abstract void showSecond();

        public void addInvalidationListener(InvalidationListener listener) {
            testPopup.ownerNodeProperty().addListener(listener);
            testPopup.ownerWindowProperty().addListener(listener);
        }

        public void removeInvalidationListener(InvalidationListener listener) {
            testPopup.ownerNodeProperty().removeListener(listener);
            testPopup.ownerWindowProperty().removeListener(listener);
        }
    }

    public static final class ConfigurationNode extends Configuration {
        public final Node testOwner1;
        public final Node testOwner2;

        private void showAssert(Node n) {
            testPopup.show(n, 0.0, 0.0);
            assertEquals(n, testPopup.ownerNodeProperty().get());
            assertEquals(n.getScene().getWindow(), testPopup.ownerWindowProperty().get());
        }

        public ConfigurationNode(Popup p, Node o1, Node o2, int expectedCalls) {
            super(p, expectedCalls);
            testOwner1 = o1;
            testOwner2 = o2;
        }

        @Override
        public void showFirst() {
            showAssert(testOwner1);
        }

        @Override
        public void showSecond() {
            showAssert(testOwner2);
        }
    }

    public static final class ConfigurationStage extends Configuration {
        public final Stage testOwner1;
        public final Stage testOwner2;

        private void showAssert(Stage s) {
            testPopup.show(s, 0.0, 0.0);
            assertEquals(s, testPopup.ownerWindowProperty().get());
        }

        public ConfigurationStage(Popup p, Stage o1, Stage o2, int expectedCalls) {
            super(p, expectedCalls);
            testOwner1 = o1;
            testOwner2 = o2;
        }

        @Override
        public void showFirst() {
            showAssert(testOwner1);
        }

        @Override
        public void showSecond() {
            showAssert(testOwner2);
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testPopupOwnerProperties(Configuration objects) {
        // This test follows what PropertiesTestBase.testBasicAccess() does
        // however it is adapted specifically for Popup.ownerNode and Popup.ownerWindow
        // (instead of setting property directly it calls Popup.show() which should
        // set the properties).

        // set to first value and verify dependent value
        objects.showFirst();

        // register listener
        final ValueInvalidationListener invalidationListener =
                new ValueInvalidationListener(objects.expectedListenerCalls);
        objects.addInvalidationListener(invalidationListener);

        // set to second value
        objects.showSecond();

        // verify that the listener has been called
        invalidationListener.assertCalled();
        invalidationListener.reset();

        // set to the second value again
        objects.showSecond();

        // verify that the listener has not been called
        invalidationListener.assertNotCalled();

        // unregister listener
        objects.removeInvalidationListener(invalidationListener);

        // set to the first value again and test
        objects.showFirst();

        // verify that the listener has not been called
        invalidationListener.assertNotCalled();
    }

    private static final class ValueInvalidationListener
            implements InvalidationListener {
        private int counter;
        private final int expected;

        public ValueInvalidationListener(int expected) {
            this.counter = 0;
            this.expected = expected;
        }

        public void reset() {
            counter = 0;
        }

        public void assertCalled() {
            assertEquals(expected, counter, "Listener has not been called, or was not called enough times!");
        }

        public void assertNotCalled() {
            assertTrue(counter == 0, "Listener has been called when it shouldn't be!");
        }

        @Override
        public void invalidated(final Observable valueModel) {
            ++counter;
        }
    }
}
