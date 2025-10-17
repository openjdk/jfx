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

package test.javafx.scene.layout;

import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.tk.HeaderAreaType;
import com.sun.javafx.tk.TKSceneListener;
import java.lang.reflect.Method;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import test.com.sun.javafx.pgstub.StubScene;
import test.javafx.util.ReflectionUtils;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class HeaderBarTest {

    Stage stage;
    Scene scene;
    HeaderBar headerBar;

    @BeforeEach
    void setup() {
        headerBar = new HeaderBar();
        scene = new Scene(headerBar);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
    }

    @AfterEach
    void teardown() {
        stage.close();
    }

    <T> T getAttachedProperty(String name) {
        try {
            Class<?> propertiesClass = Class.forName(HeaderBar.class.getName() + "$AttachedProperties");
            Method method = propertiesClass.getMethod("of", Stage.class);
            method.setAccessible(true);
            return ReflectionUtils.getFieldValue(method.invoke(null, stage), name);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void emptyHeaderBar() {
        assertNull(headerBar.getLeft());
        assertNull(headerBar.getCenter());
        assertNull(headerBar.getRight());
    }

    @Test
    void minHeight_correspondsToMinSystemHeight_ifNotSetByUser() {
        DoubleProperty minSystemHeight = getAttachedProperty("minSystemHeight");
        minSystemHeight.set(100);
        assertEquals(100, headerBar.minHeight(-1));

        headerBar.setMinHeight(50);
        minSystemHeight.set(200);
        assertEquals(50, headerBar.minHeight(-1));
    }

    @Nested
    class LayoutTest {
        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 10, 10, 100, 80",
            "TOP_CENTER, 10, 10, 100, 80",
            "TOP_RIGHT, 10, 10, 100, 80",
            "CENTER_LEFT, 10, 10, 100, 80",
            "CENTER, 10, 10, 100, 80",
            "CENTER_RIGHT, 10, 10, 100, 80",
            "BOTTOM_LEFT, 10, 10, 100, 80",
            "BOTTOM_CENTER, 10, 10, 100, 80",
            "BOTTOM_RIGHT, 10, 10, 100, 80"
        })
        void alignmentOfLeftChildOnly_resizable(Pos pos, double x, double y, double width, double height) {
            var content = new MockResizable(100, 50);
            HeaderBar.setAlignment(content, pos);
            HeaderBar.setMargin(content, new Insets(10));
            headerBar.setLeft(content);
            headerBar.resize(1000, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, content);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 10, 10, 100, 50",
            "TOP_CENTER, 10, 10, 100, 50",
            "TOP_RIGHT, 10, 10, 100, 50",
            "CENTER_LEFT, 10, 25, 100, 50",
            "CENTER, 10, 25, 100, 50",
            "CENTER_RIGHT, 10, 25, 100, 50",
            "BOTTOM_LEFT, 10, 40, 100, 50",
            "BOTTOM_CENTER, 10, 40, 100, 50",
            "BOTTOM_RIGHT, 10, 40, 100, 50"
        })
        void alignmentOfLeftChildOnly_notResizable(Pos pos, double x, double y, double width, double height) {
            var content = new Rectangle(100, 50);
            HeaderBar.setAlignment(content, pos);
            HeaderBar.setMargin(content, new Insets(10));
            headerBar.setLeft(content);
            headerBar.resize(1000, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, content);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 890, 10, 100, 80",
            "TOP_CENTER, 890, 10, 100, 80",
            "TOP_RIGHT, 890, 10, 100, 80",
            "CENTER_LEFT, 890, 10, 100, 80",
            "CENTER, 890, 10, 100, 80",
            "CENTER_RIGHT, 890, 10, 100, 80",
            "BOTTOM_LEFT, 890, 10, 100, 80",
            "BOTTOM_CENTER, 890, 10, 100, 80",
            "BOTTOM_RIGHT, 890, 10, 100, 80"
        })
        void alignmentOfRightChildOnly_resizable(Pos pos, double x, double y, double width, double height) {
            var content = new MockResizable(100, 50);
            HeaderBar.setAlignment(content, pos);
            HeaderBar.setMargin(content, new Insets(10));
            headerBar.setRight(content);
            headerBar.resize(1000, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, content);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 890, 10, 100, 50",
            "TOP_CENTER, 890, 10, 100, 50",
            "TOP_RIGHT, 890, 10, 100, 50",
            "CENTER_LEFT, 890, 25, 100, 50",
            "CENTER, 890, 25, 100, 50",
            "CENTER_RIGHT, 890, 25, 100, 50",
            "BOTTOM_LEFT, 890, 40, 100, 50",
            "BOTTOM_CENTER, 890, 40, 100, 50",
            "BOTTOM_RIGHT, 890, 40, 100, 50"
        })
        void alignmentOfRightChildOnly_notResizable(Pos pos, double x, double y, double width, double height) {
            var content = new Rectangle(100, 50);
            HeaderBar.setAlignment(content, pos);
            HeaderBar.setMargin(content, new Insets(10));
            headerBar.setRight(content);
            headerBar.resize(1000, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, content);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 10, 10, 200, 80",
            "TOP_CENTER, 400, 10, 200, 80",
            "TOP_RIGHT, 790, 10, 200, 80",
            "CENTER_LEFT, 10, 10, 200, 80",
            "CENTER, 400, 10, 200, 80",
            "CENTER_RIGHT, 790, 10, 200, 80",
            "BOTTOM_LEFT, 10, 10, 200, 80",
            "BOTTOM_CENTER, 400, 10, 200, 80",
            "BOTTOM_RIGHT, 790, 10, 200, 80"
        })
        void alignmentOfCenterChildOnly_resizable(
                Pos pos, double x, double y, double width, double height) {
            var content = new MockResizable(0, 0, 100, 50, 200, 100);
            HeaderBar.setAlignment(content, pos);
            HeaderBar.setMargin(content, new Insets(10));
            headerBar.setCenter(content);
            headerBar.resize(1000, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, content);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 10, 10, 100, 50",
            "TOP_CENTER, 450, 10, 100, 50",
            "TOP_RIGHT, 890, 10, 100, 50",
            "CENTER_LEFT, 10, 25, 100, 50",
            "CENTER, 450, 25, 100, 50",
            "CENTER_RIGHT, 890, 25, 100, 50",
            "BOTTOM_LEFT, 10, 40, 100, 50",
            "BOTTOM_CENTER, 450, 40, 100, 50",
            "BOTTOM_RIGHT, 890, 40, 100, 50"
        })
        void alignmentOfCenterChildOnly_notResizable(Pos pos, double x, double y, double width, double height) {
            var content = new Rectangle(100, 50);
            HeaderBar.setAlignment(content, pos);
            HeaderBar.setMargin(content, new Insets(10));
            headerBar.setCenter(content);
            headerBar.resize(1000, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, content);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 60, 10, 200, 80",
            "TOP_CENTER, 400, 10, 200, 80",
            "TOP_RIGHT, 640, 10, 200, 80",
            "CENTER_LEFT, 60, 10, 200, 80",
            "CENTER, 400, 10, 200, 80",
            "CENTER_RIGHT, 640, 10, 200, 80",
            "BOTTOM_LEFT, 60, 10, 200, 80",
            "BOTTOM_CENTER, 400, 10, 200, 80",
            "BOTTOM_RIGHT, 640, 10, 200, 80"
        })
        void alignmentOfCenterChild_resizable_withNonEmptyLeftAndRightChild(
                Pos pos, double x, double y, double width, double height) {
            var left = new MockResizable(50, 50);
            var center = new MockResizable(0, 0, 100, 50, 200, 100);
            var right = new MockResizable(150, 50);
            HeaderBar.setAlignment(center, pos);
            HeaderBar.setMargin(center, new Insets(10));
            headerBar.setLeft(left);
            headerBar.setCenter(center);
            headerBar.setRight(right);
            headerBar.resize(1000, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, center);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 60, 10, 100, 50",
            "TOP_CENTER, 450, 10, 100, 50",
            "TOP_RIGHT, 740, 10, 100, 50",
            "CENTER_LEFT, 60, 25, 100, 50",
            "CENTER, 450, 25, 100, 50",
            "CENTER_RIGHT, 740, 25, 100, 50",
            "BOTTOM_LEFT, 60, 40, 100, 50",
            "BOTTOM_CENTER, 450, 40, 100, 50",
            "BOTTOM_RIGHT, 740, 40, 100, 50"
        })
        void alignmentOfCenterChild_notResizable_withNonEmptyLeftAndRightChild(
                Pos pos, double x, double y, double width, double height) {
            var left = new Rectangle(50, 50);
            var center = new Rectangle(100, 50);
            var right = new Rectangle(150, 50);
            HeaderBar.setAlignment(center, pos);
            HeaderBar.setMargin(center, new Insets(10));
            headerBar.setLeft(left);
            headerBar.setCenter(center);
            headerBar.setRight(right);
            headerBar.resize(1000, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, center);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 160, 10, 680, 80",
            "TOP_CENTER, 160, 10, 680, 80",
            "TOP_RIGHT, 160, 10, 680, 80",
            "CENTER_LEFT, 160, 10, 680, 80",
            "CENTER, 160, 10, 680, 80",
            "CENTER_RIGHT, 160, 10, 680, 80",
            "BOTTOM_LEFT, 160, 10, 680, 80",
            "BOTTOM_CENTER, 160, 10, 680, 80",
            "BOTTOM_RIGHT, 160, 10, 680, 80"
        })
        void alignmentOfCenterChild_withLeftSystemInset(Pos pos, double x, double y, double width, double height) {
            ObjectProperty<Dimension2D> leftSystemInset = getAttachedProperty("leftSystemInset");
            leftSystemInset.set(new Dimension2D(100, 100));
            alignmentOfCenterChildImpl(pos, 1000, 1000, x, y, width, height);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 160, 10, 100, 80",
            "TOP_CENTER, 450, 10, 100, 80",
            "TOP_RIGHT, 740, 10, 100, 80",
            "CENTER_LEFT, 160, 10, 100, 80",
            "CENTER, 450, 10, 100, 80",
            "CENTER_RIGHT, 740, 10, 100, 80",
            "BOTTOM_LEFT, 160, 10, 100, 80",
            "BOTTOM_CENTER, 450, 10, 100, 80",
            "BOTTOM_RIGHT, 740, 10, 100, 80"
        })
        void alignmentOfCenterChild_withLeftSystemInset_andMaxWidthConstraint(
                Pos pos, double x, double y, double width, double height) {
            ObjectProperty<Dimension2D> leftSystemInset = getAttachedProperty("leftSystemInset");
            leftSystemInset.set(new Dimension2D(100, 100));
            alignmentOfCenterChildImpl(pos, 1000, 100, x, y, width, height);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 60, 10, 680, 80",
            "TOP_CENTER, 60, 10, 680, 80",
            "TOP_RIGHT, 60, 10, 680, 80",
            "CENTER_LEFT, 60, 10, 680, 80",
            "CENTER, 60, 10, 680, 80",
            "CENTER_RIGHT, 60, 10, 680, 80",
            "BOTTOM_LEFT, 60, 10, 680, 80",
            "BOTTOM_CENTER, 60, 10, 680, 80",
            "BOTTOM_RIGHT, 60, 10, 680, 80"
        })
        void alignmentOfCenterChild_withRightSystemInset(Pos pos, double x, double y, double width, double height) {
            ObjectProperty<Dimension2D> rightSystemInset = getAttachedProperty("rightSystemInset");
            rightSystemInset.set(new Dimension2D(100, 100));
            alignmentOfCenterChildImpl(pos, 1000, 1000, x, y, width, height);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 60, 10, 100, 80",
            "TOP_CENTER, 450, 10, 100, 80",
            "TOP_RIGHT, 640, 10, 100, 80",
            "CENTER_LEFT, 60, 10, 100, 80",
            "CENTER, 450, 10, 100, 80",
            "CENTER_RIGHT, 640, 10, 100, 80",
            "BOTTOM_LEFT, 60, 10, 100, 80",
            "BOTTOM_CENTER, 450, 10, 100, 80",
            "BOTTOM_RIGHT, 640, 10, 100, 80"
        })
        void alignmentOfCenterChild_withRightSystemInset_andMaxWidthConstraint(
                Pos pos, double x, double y, double width, double height) {
            ObjectProperty<Dimension2D> rightSystemInset = getAttachedProperty("rightSystemInset");
            rightSystemInset.set(new Dimension2D(100, 100));
            alignmentOfCenterChildImpl(pos, 1000, 100, x, y, width, height);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_CENTER, 260, 10, 80, 80",
            "CENTER, 260, 10, 80, 80",
            "BOTTOM_CENTER, 260, 10, 80, 80"
        })
        void alignmentOfCenterChild_withLeftSystemInset_andOffsetCausedByInsufficientHorizontalSpace(
                Pos pos, double x, double y, double width, double height) {
            ObjectProperty<Dimension2D> leftSystemInset = getAttachedProperty("leftSystemInset");
            leftSystemInset.set(new Dimension2D(200, 100));
            alignmentOfCenterChildImpl(pos, 500, 100, x, y, width, height);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_CENTER, 60, 10, 80, 80",
            "CENTER, 60, 10, 80, 80",
            "BOTTOM_CENTER, 60, 10, 80, 80"
        })
        void alignmentOfCenterChild_withRightSystemInset_andOffsetCausedByInsufficientHorizontalSpace(
                Pos pos, double x, double y, double width, double height) {
            ObjectProperty<Dimension2D> rightSystemInset = getAttachedProperty("rightSystemInset");
            rightSystemInset.set(new Dimension2D(200, 100));
            alignmentOfCenterChildImpl(pos, 500, 100, x, y, width, height);
        }

        private void alignmentOfCenterChildImpl(Pos pos, double headerBarWidth, double maxWidth,
                                                double x, double y, double width, double height) {
            var left = new MockResizable(50, 50);
            var center = new MockResizable(0, 0, 100, 50, maxWidth, 100);
            var right = new MockResizable(150, 50);
            HeaderBar.setAlignment(center, pos);
            HeaderBar.setMargin(center, new Insets(10));
            headerBar.setLeft(left);
            headerBar.setCenter(center);
            headerBar.setRight(right);
            headerBar.resize(headerBarWidth, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, center);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_LEFT, 10, 10, 50, 50",
            "CENTER, 10, 25, 50, 50",
            "BOTTOM_LEFT, 10, 40, 50, 50"
        })
        void alignmentOfLeftChild_notResizable_withoutReservedArea(
                Pos pos, double x, double y, double width, double height) {
            ObjectProperty<Dimension2D> leftSystemInset = getAttachedProperty("leftSystemInset");
            leftSystemInset.set(new Dimension2D(100, 100));
            var left = new Rectangle(50, 50);
            HeaderBar.setAlignment(left, pos);
            HeaderBar.setMargin(left, new Insets(10));
            headerBar.setLeftSystemPadding(false);
            headerBar.setLeft(left);
            headerBar.resize(1000, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, left);
        }

        @ParameterizedTest
        @CsvSource({
            "TOP_RIGHT, 940, 10, 50, 50",
            "CENTER, 940, 25, 50, 50",
            "BOTTOM_RIGHT, 940, 40, 50, 50"
        })
        void alignmentOfRightChild_notResizable_withoutReservedArea(
                Pos pos, double x, double y, double width, double height) {
            ObjectProperty<Dimension2D> rightSystemInset = getAttachedProperty("rightSystemInset");
            rightSystemInset.set(new Dimension2D(100, 100));
            var right = new Rectangle(50, 50);
            HeaderBar.setAlignment(right, pos);
            HeaderBar.setMargin(right, new Insets(10));
            headerBar.setRightSystemPadding(false);
            headerBar.setRight(right);
            headerBar.resize(1000, 100);
            headerBar.layout();

            assertBounds(x, y, width, height, right);
        }

        private void assertBounds(double x, double y, double width, double height, Node node) {
            var bounds = node.getLayoutBounds();
            assertEquals(x, node.getLayoutX());
            assertEquals(y, node.getLayoutY());
            assertEquals(width, bounds.getWidth());
            assertEquals(height, bounds.getHeight());
        }
    }

    @Nested
    class PickingTest {
        /**
         * For picking tests, we use a header bar with four nested boxes, arranged from left to right.
         *
         * <pre>
         *     0        50       100      150      200
         *     ┌────────┬───────────────────────────────────┐
         *     │  (HB)  │  box1  ┌──────────────────────────┤
         *     │        │        │  box2  ┌─────────────────┤
         *     │        │        │        │  box3  ┌────────┤
         *     │        │        │        │        │  box4  │
         *     ╞════════╧════════╧════════╧════════╧════════╡
         *     │                                            │
         * </pre>
         */
        private static class TestHeaderBar extends HeaderBar {
            final Box box4 = new Box(null, 50, 0, 50, 100);
            final Box box3 = new Box(box4, 50, 0, 100, 100);
            final Box box2 = new Box(box3, 50, 0, 150, 100);
            final Box box1 = new Box(box2, 50, 0, 200, 100);

            TestHeaderBar() {
                resize(250, 100);
                setCenter(box1);
            }
        }

        private static class Box extends StackPane {
            Box(Node child, double x, double y, double width, double height) {
                setManaged(false);
                resizeRelocate(x, y, width, height);

                if (child != null) {
                    getChildren().add(child);
                }
            }
        }

        @Test
         void pickDraggableNode() {
            var headerBar = new TestHeaderBar();
            HeaderBar.setDragType(headerBar.box1, HeaderDragType.DRAGGABLE);

            var scene = new Scene(headerBar, 250, 200);
            var stage = new Stage();
            stage.setScene(scene);
            stage.show();

            // 1. HeaderBar is always draggable
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 10, 10));

            // 2. box1 is draggable because its drag type is DRAGGABLE
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 60, 10));

            // 3. box2/box3/box4 are not draggable, because they don't inherit DRAGGABLE from box1
            assertNull(pickHeaderArea(scene, 110, 10));
            assertNull(pickHeaderArea(scene, 160, 10));
            assertNull(pickHeaderArea(scene, 210, 10));
        }

        @Test
        void pickDraggableNodeInSubtree() {
            var headerBar = new TestHeaderBar();
            HeaderBar.setDragType(headerBar.box1, HeaderDragType.DRAGGABLE_SUBTREE);

            var scene = new Scene(headerBar, 250, 200);
            var stage = new Stage();
            stage.setScene(scene);
            stage.show();

            // 1. HeaderBar is always draggable
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 10, 10));

            // 2. box1 is draggable because its drag type is DRAGGABLE_SUBTREE
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 60, 10));

            // 3. box2/box3/box4 are draggable, because they inherit DRAGGABLE_SUBTREE from box1
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 110, 10));
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 160, 10));
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 210, 10));
        }

        @Test
        void stopInheritanceOfDraggableSubtree() {
            var headerBar = new TestHeaderBar();
            HeaderBar.setDragType(headerBar.box1, HeaderDragType.DRAGGABLE_SUBTREE);
            HeaderBar.setDragType(headerBar.box3, HeaderDragType.NONE);

            var scene = new Scene(headerBar, 250, 200);
            var stage = new Stage();
            stage.setScene(scene);
            stage.show();

            // 1. HeaderBar is always draggable
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 10, 10));

            // 2. box1 is draggable because its drag type is DRAGGABLE_SUBTREE
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 60, 10));

            // 3. box2 is draggable, because it inherits DRAGGABLE_SUBTREE from box1
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 110, 10));

            // 4. box3/box4 are not draggable, because NONE stops the inherited DRAGGABLE_SUBTREE
            assertNull(pickHeaderArea(scene, 160, 10));
            assertNull(pickHeaderArea(scene, 210, 10));
        }

        @Test
        void draggableNodeDoesNotStopInheritanceOfDraggableSubtree() {
            var headerBar = new TestHeaderBar();
            HeaderBar.setDragType(headerBar.box1, HeaderDragType.DRAGGABLE_SUBTREE);
            HeaderBar.setDragType(headerBar.box3, HeaderDragType.DRAGGABLE);

            var scene = new Scene(headerBar, 250, 200);
            var stage = new Stage();
            stage.setScene(scene);
            stage.show();

            // 1. HeaderBar is always draggable
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 10, 10));

            // 2. box1 is draggable because its drag type is DRAGGABLE_SUBTREE
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 60, 10));

            // 3. box2 is draggable, because it inherits DRAGGABLE_SUBTREE from box1
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 110, 10));

            // 4. box3/box4 are draggable, because DRAGGABLE doesn't stop the inherited DRAGGABLE_SUBTREE from box1
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 160, 10));
            assertEquals(HeaderAreaType.DRAGBAR, pickHeaderArea(scene, 210, 10));
        }

        private static HeaderAreaType pickHeaderArea(Scene scene, double x, double y) {
            var peer = (StubScene)SceneHelper.getPeer(scene);
            TKSceneListener listener = ReflectionUtils.getFieldValue(peer, "listener");
            return listener.pickHeaderArea(x, y);
        }
    }
}
