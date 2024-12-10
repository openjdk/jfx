/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.glass.ui;

import com.sun.glass.ui.WindowControlsOverlay;
import com.sun.javafx.binding.ObjectConstant;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Dimension2D;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import test.util.ReflectionUtils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class WindowControlsOverlayTest {

    /**
     * Asserts that the buttons are laid out on the right side of the control (left-to-right orientation).
     */
    @Test
    void rightPlacement() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button-container { -fx-button-placement: right; }
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 140, 0, 20, 10);
        assertLayoutBounds(children.get(1), 160, 0, 20, 10);
        assertLayoutBounds(children.get(2), 180, 0, 20, 10);
        assertEquals(HorizontalDirection.RIGHT, overlay.metricsProperty().get().placement());
        assertEquals(new Dimension2D(60, 10), overlay.metricsProperty().get().size());
    }

    /**
     * Asserts that the buttons are laid out on the left side of the control (right-to-left orientation).
     */
    @Test
    void rightPlacement_rightToLeft() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button-container { -fx-button-placement: right; }
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 40, 0, 20, 10);
        assertLayoutBounds(children.get(1), 20, 0, 20, 10);
        assertLayoutBounds(children.get(2), 0, 0, 20, 10);
        assertEquals(HorizontalDirection.LEFT, overlay.metricsProperty().get().placement());
        assertEquals(new Dimension2D(60, 10), overlay.metricsProperty().get().size());
    }

    /**
     * Asserts that the buttons are laid out on the left side of the control (left-to-right orientation).
     */
    @Test
    void leftPlacement() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button-container { -fx-button-placement: left; }
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 0, 0, 20, 10);
        assertLayoutBounds(children.get(1), 20, 0, 20, 10);
        assertLayoutBounds(children.get(2), 40, 0, 20, 10);
        assertEquals(HorizontalDirection.LEFT, overlay.metricsProperty().get().placement());
        assertEquals(new Dimension2D(60, 10), overlay.metricsProperty().get().size());
    }

    /**
     * Asserts that the buttons are laid out on the right side of the control (right-to-left orientation).
     */
    @Test
    void leftPlacement_rightToLeft() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button-container { -fx-button-placement: left; }
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 180, 0, 20, 10);
        assertLayoutBounds(children.get(1), 160, 0, 20, 10);
        assertLayoutBounds(children.get(2), 140, 0, 20, 10);
        assertEquals(HorizontalDirection.RIGHT, overlay.metricsProperty().get().placement());
        assertEquals(new Dimension2D(60, 10), overlay.metricsProperty().get().size());
    }

    /**
     * Asserts that the buttons are laid out in a custom order (left-to-right orientation).
     */
    @Test
    void customButtonOrder() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
                .minimize-button { -fx-button-order: 5; }
                .maximize-button { -fx-button-order: 1; }
                .close-button { -fx-button-order: 3; }
            """), false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertTrue(children.get(0).getStyleClass().contains("minimize-button"));
        assertLayoutBounds(children.get(0), 180, 0, 20, 10);
        assertTrue(children.get(1).getStyleClass().contains("maximize-button"));
        assertLayoutBounds(children.get(1), 140, 0, 20, 10);
        assertTrue(children.get(2).getStyleClass().contains("close-button"));
        assertLayoutBounds(children.get(2), 160, 0, 20, 10);
    }

    /**
     * Asserts that the buttons are laid out in a custom order (right-to-left orientation).
     */
    @Test
    void customButtonOrder_rightToLeft() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
                .minimize-button { -fx-button-order: 5; }
                .maximize-button { -fx-button-order: 1; }
                .close-button { -fx-button-order: 3; }
            """), false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertTrue(children.get(0).getStyleClass().contains("minimize-button"));
        assertLayoutBounds(children.get(0), 0, 0, 20, 10);
        assertTrue(children.get(1).getStyleClass().contains("maximize-button"));
        assertLayoutBounds(children.get(1), 40, 0, 20, 10);
        assertTrue(children.get(2).getStyleClass().contains("close-button"));
        assertLayoutBounds(children.get(2), 20, 0, 20, 10);
    }

    @Test
    void utilityDecorationIsOnlyCloseButton() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), true);

        var children = overlay.getChildrenUnmodifiable();
        assertEquals(1, children.size());
        assertTrue(children.getFirst().getStyleClass().contains("close-button"));
    }

    /**
     * Asserts that the buttons are laid out on the right, even though the node orientation is right-to-left.
     */
    @Test
    void disallowRightToLeft() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button-container { -fx-button-placement: right; -fx-allow-rtl: false; }
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertLayoutBounds(children.get(0), 140, 0, 20, 10);
        assertLayoutBounds(children.get(1), 160, 0, 20, 10);
        assertLayoutBounds(children.get(2), 180, 0, 20, 10);
        assertEquals(HorizontalDirection.RIGHT, overlay.metricsProperty().get().placement());
        assertEquals(new Dimension2D(60, 10), overlay.metricsProperty().get().size());
    }

    @Test
    void activePseudoClassCorrespondsToStageFocusedProperty() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button-container { -fx-button-placement: right; }
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false);

        var scene = new Scene(overlay);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();

        assertTrue(stage.isFocused());
        assertTrue(overlay.getChildrenUnmodifiable().get(0).getPseudoClassStates().stream().anyMatch(
                pc -> pc.getPseudoClassName().equals("active")));

        ReflectionUtils.invokeMethod(stage, "setFocused", new Class[] { boolean.class }, false);

        assertFalse(stage.isFocused());
        assertTrue(overlay.getChildrenUnmodifiable().get(0).getPseudoClassStates().stream().noneMatch(
                pc -> pc.getPseudoClassName().equals("active")));
    }

    /**
     * Asserts that the maximize button is disabled when the stage is not resizable.
     */
    @Test
    void maximizeButtonIsDisabledWhenStageIsNotResizable() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button-container { -fx-button-placement: right; }
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false);

        var scene = new Scene(overlay);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();

        var maxButton = overlay.getChildrenUnmodifiable().get(1);
        assertTrue(maxButton.getStyleClass().contains("maximize-button"));
        assertTrue(stage.isResizable());
        assertFalse(maxButton.isDisabled());

        stage.setResizable(false);
        assertTrue(maxButton.isDisabled());
    }

    /**
     * Asserts that the .restore style class is added to the maximize button when the stage is maximized.
     */
    @Test
    void restoreStyleClassIsPresentWhenStageIsMaximized() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button-container { -fx-button-placement: right; }
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false);

        var scene = new Scene(overlay);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();

        var maxButton = overlay.getChildrenUnmodifiable().get(1);
        assertTrue(maxButton.getStyleClass().contains("maximize-button"));
        assertFalse(maxButton.getStyleClass().contains("restore"));

        stage.setMaximized(true);
        assertTrue(maxButton.getStyleClass().contains("restore"));
    }

    /**
     * Asserts that the .dark style class is added to all buttons when {@link Scene#getFill()} is dark.
     */
    @Test
    void darkStyleClassIsPresentWhenSceneFillIsDark() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button-container { -fx-button-placement: right; }
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false);

        var scene = new Scene(overlay);

        scene.setFill(Color.WHITE);
        assertTrue(overlay.getChildrenUnmodifiable().stream().noneMatch(b -> b.getStyleClass().contains("dark")));

        scene.setFill(Color.BLACK);
        assertTrue(overlay.getChildrenUnmodifiable().stream().allMatch(b -> b.getStyleClass().contains("dark")));
    }

    /**
     * Tests button picking using {@link WindowControlsOverlay#buttonAt(double, double)}.
     */
    @Test
    void pickButtonAtCoordinates() {
        var overlay = new WindowControlsOverlay(getStylesheet("""
                .window-button-container { -fx-button-placement: right; }
                .window-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false);

        var unused = new Scene(overlay);
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertNull(overlay.buttonAt(139, 5));
        assertEquals(WindowControlsOverlay.ButtonType.MINIMIZE, overlay.buttonAt(140, 0));
        assertEquals(WindowControlsOverlay.ButtonType.MAXIMIZE, overlay.buttonAt(165, 5));
        assertEquals(WindowControlsOverlay.ButtonType.CLOSE, overlay.buttonAt(181, 10));
    }

    private static ObservableValue<String> getStylesheet(String text) {
        String stylesheet = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));

        return ObjectConstant.valueOf(stylesheet);
    }

    private static void assertLayoutBounds(Node node, double x, double y, double width, double height) {
        assertEquals(x, node.getLayoutX());
        assertEquals(y, node.getLayoutY());
        assertSize(node, width, height);
    }

    private static void assertSize(Node node, double width, double height) {
        assertEquals(width, node.getLayoutBounds().getWidth());
        assertEquals(height, node.getLayoutBounds().getHeight());
    }
}
