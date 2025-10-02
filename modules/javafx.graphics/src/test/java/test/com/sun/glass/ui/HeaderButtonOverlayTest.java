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

package test.com.sun.glass.ui;

import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.HeaderButtonOverlay;
import com.sun.javafx.binding.ObjectConstant;
import test.javafx.util.ReflectionUtils;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Dimension2D;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HeaderButtonType;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
public class HeaderButtonOverlayTest {

    private static final Dimension2D EMPTY = new Dimension2D(0, 0);

    /**
     * Asserts that the buttons are laid out on the right side of the control (left-to-right orientation).
     */
    @Test
    void rightPlacement_stretchAlignment() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: right;
                                                       -fx-button-default-height: 20;
                                                       -fx-button-vertical-alignment: stretch; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; }
            """), false, false, false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 140, 0, 20, 20);
        assertLayoutBounds(children.get(1), 160, 0, 20, 20);
        assertLayoutBounds(children.get(2), 180, 0, 20, 20);
        assertEquals(EMPTY, overlay.metricsProperty().get().leftInset());
        assertEquals(new Dimension2D(60, 20), overlay.metricsProperty().get().rightInset());
    }

    /**
     * Asserts that the buttons are laid out on the right side of the control (right-to-left orientation).
     */
    @Test
    void rightPlacement_stretchAlignment_rightToLeft() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: right;
                                                        -fx-button-default-height: 20;
                                                        -fx-button-vertical-alignment: stretch; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; }
            """), false, false, true);

        var scene = new Scene(overlay);
        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 40, 0, 20, 20);
        assertLayoutBounds(children.get(1), 20, 0, 20, 20);
        assertLayoutBounds(children.get(2), 0, 0, 20, 20);
        assertEquals(new Dimension2D(60, 20), overlay.metricsProperty().get().leftInset());
        assertEquals(EMPTY, overlay.metricsProperty().get().rightInset());
    }

    /**
     * Asserts that the buttons are laid out on the right side of the control (left-to-right orientation)
     * with center alignment (including offsets caused by center alignment).
     */
    @Test
    void rightPlacement_centerAlignment() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: right;
                                                        -fx-button-default-height: 20;
                                                        -fx-button-vertical-alignment: center; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, false, false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 135, 5, 20, 10);
        assertLayoutBounds(children.get(1), 155, 5, 20, 10);
        assertLayoutBounds(children.get(2), 175, 5, 20, 10);
        assertEquals(EMPTY, overlay.metricsProperty().get().leftInset());
        assertEquals(new Dimension2D(70, 20), overlay.metricsProperty().get().rightInset());
    }

    /**
     * Asserts that the buttons are laid out on the left side of the control (right-to-left orientation)
     * with center alignment (including offsets caused by center alignment).
     */
    @Test
    void rightPlacement_centerAlignment_rightToLeft() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: right;
                                                        -fx-button-default-height: 20;
                                                        -fx-button-vertical-alignment: center; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, false, true);

        var scene = new Scene(overlay);
        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 45, 5, 20, 10);
        assertLayoutBounds(children.get(1), 25, 5, 20, 10);
        assertLayoutBounds(children.get(2), 5, 5, 20, 10);
        assertEquals(new Dimension2D(70, 20), overlay.metricsProperty().get().leftInset());
        assertEquals(EMPTY, overlay.metricsProperty().get().rightInset());
    }

    /**
     * Asserts that the buttons are laid out on the left side of the control (left-to-right orientation).
     */
    @Test
    void leftPlacement_stretchAlignment() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: left;
                                                        -fx-button-default-height: 20;
                                                        -fx-button-vertical-alignment: stretch; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; }
            """), false, false, false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 0, 0, 20, 20);
        assertLayoutBounds(children.get(1), 20, 0, 20, 20);
        assertLayoutBounds(children.get(2), 40, 0, 20, 20);
        assertEquals(new Dimension2D(60, 20), overlay.metricsProperty().get().leftInset());
        assertEquals(EMPTY, overlay.metricsProperty().get().rightInset());
    }

    /**
     * Asserts that the buttons are laid out on the left side of the control (right-to-left orientation).
     */
    @Test
    void leftPlacement_stretchAlignment_rightToLeft() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: left;
                                                        -fx-button-default-height: 20;
                                                        -fx-button-vertical-alignment: stretch; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; }
            """), false, false, true);

        var scene = new Scene(overlay);
        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 180, 0, 20, 20);
        assertLayoutBounds(children.get(1), 160, 0, 20, 20);
        assertLayoutBounds(children.get(2), 140, 0, 20, 20);
        assertEquals(EMPTY, overlay.metricsProperty().get().leftInset());
        assertEquals(new Dimension2D(60, 20), overlay.metricsProperty().get().rightInset());
    }

    /**
     * Asserts that the buttons are laid out on the left side of the control (left-to-right orientation)
     * with center alignment (including offsets caused by center alignment).
     */
    @Test
    void leftPlacement_centerAlignment() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: left;
                                                        -fx-button-default-height: 20;
                                                        -fx-button-vertical-alignment: center; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, false, false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 5, 5, 20, 10);
        assertLayoutBounds(children.get(1), 25, 5, 20, 10);
        assertLayoutBounds(children.get(2), 45, 5, 20, 10);
        assertEquals(new Dimension2D(70, 20), overlay.metricsProperty().get().leftInset());
        assertEquals(EMPTY, overlay.metricsProperty().get().rightInset());
    }

    /**
     * Asserts that the buttons are laid out on the left side of the control (right-to-left orientation)
     * with center alignment (including offsets caused by center alignment).
     */
    @Test
    void leftPlacement_centerAlignment_rightToLeft() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: left;
                                                        -fx-button-default-height: 20;
                                                        -fx-button-vertical-alignment: center; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, false, true);

        var scene = new Scene(overlay);
        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertSize(overlay, 200, 100);
        assertLayoutBounds(children.get(0), 175, 5, 20, 10);
        assertLayoutBounds(children.get(1), 155, 5, 20, 10);
        assertLayoutBounds(children.get(2), 135, 5, 20, 10);
        assertEquals(EMPTY, overlay.metricsProperty().get().leftInset());
        assertEquals(new Dimension2D(70, 20), overlay.metricsProperty().get().rightInset());
    }

    /**
     * Asserts that the buttons are laid out in a custom order (left-to-right orientation).
     */
    @Test
    void customButtonOrder() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-vertical-alignment: stretch; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
                .-FX-INTERNAL-iconify-button { -fx-button-order: 5; }
                .-FX-INTERNAL-maximize-button { -fx-button-order: 1; }
                .-FX-INTERNAL-close-button { -fx-button-order: 3; }
            """), false, false, false);

        var unused = new Scene(overlay);
        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertTrue(children.get(0).getStyleClass().contains("-FX-INTERNAL-iconify-button"));
        assertLayoutBounds(children.get(0), 180, 0, 20, 10);
        assertTrue(children.get(1).getStyleClass().contains("-FX-INTERNAL-maximize-button"));
        assertLayoutBounds(children.get(1), 140, 0, 20, 10);
        assertTrue(children.get(2).getStyleClass().contains("-FX-INTERNAL-close-button"));
        assertLayoutBounds(children.get(2), 160, 0, 20, 10);
    }

    /**
     * Asserts that the buttons are laid out in a custom order (right-to-left orientation).
     */
    @Test
    void customButtonOrder_rightToLeft() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-vertical-alignment: stretch; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
                .-FX-INTERNAL-iconify-button { -fx-button-order: 5; }
                .-FX-INTERNAL-maximize-button { -fx-button-order: 1; }
                .-FX-INTERNAL-close-button { -fx-button-order: 3; }
            """), false, false, true);

        var scene = new Scene(overlay);
        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        var children = overlay.getChildrenUnmodifiable();
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertTrue(children.get(0).getStyleClass().contains("-FX-INTERNAL-iconify-button"));
        assertLayoutBounds(children.get(0), 0, 0, 20, 10);
        assertTrue(children.get(1).getStyleClass().contains("-FX-INTERNAL-maximize-button"));
        assertLayoutBounds(children.get(1), 40, 0, 20, 10);
        assertTrue(children.get(2).getStyleClass().contains("-FX-INTERNAL-close-button"));
        assertLayoutBounds(children.get(2), 20, 0, 20, 10);
    }

    @Test
    void utilityDecorationIsOnlyCloseButton() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, true, false);

        var children = overlay.getChildrenUnmodifiable();
        assertEquals(1, children.size());
        assertTrue(children.getFirst().getStyleClass().contains("-FX-INTERNAL-close-button"));
    }

    enum ButtonDisabledStateTest {
        RESIZABLE(true, false, false, false),
        UNRESIZABLE(false, false, false, true),
        MODAL_RESIZABLE(true, true, true, false),
        MODAL_UNRESIZABLE(false, true, true, true);

        ButtonDisabledStateTest(boolean resizable, boolean modalOrOwned,
                                boolean iconifyDisabled, boolean maximizeDisabled) {
            this.resizable = resizable;
            this.modalOrOwned = modalOrOwned;
            this.iconifyDisabled = iconifyDisabled;
            this.maximizeDisabled = maximizeDisabled;
        }

        final boolean resizable;
        final boolean modalOrOwned;
        final boolean iconifyDisabled;
        final boolean maximizeDisabled;
    }

    /**
     * Tests the disabled states of the iconify and maximize buttons for all combinations
     * of resizable and modal window attributes.
     */
    @ParameterizedTest
    @EnumSource(ButtonDisabledStateTest.class)
    void buttonDisabledStateIsCorrect(ButtonDisabledStateTest test) {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), test.modalOrOwned, false, false);

        var scene = new Scene(overlay);
        var stage = new Stage();
        stage.setScene(scene);
        stage.setResizable(test.resizable);

        var children = overlay.getChildrenUnmodifiable();
        assertEquals(3, children.size());

        assertTrue(children.get(0).getStyleClass().contains("-FX-INTERNAL-iconify-button"));
        assertEquals(test.iconifyDisabled, children.get(0).isDisabled());

        assertTrue(children.get(1).getStyleClass().contains("-FX-INTERNAL-maximize-button"));
        assertEquals(test.maximizeDisabled, children.get(1).isDisabled());

        assertTrue(children.get(2).getStyleClass().contains("-FX-INTERNAL-close-button"));
        assertFalse(children.get(2).isDisabled());
    }

    @Test
    void activePseudoClassCorrespondsToStageFocusedProperty() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: right; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, false, false);

        var scene = new Scene(overlay);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();

        assertTrue(stage.isFocused());
        assertTrue(overlay.getChildrenUnmodifiable().getFirst().getPseudoClassStates().stream().anyMatch(
                pc -> pc.getPseudoClassName().equals("active")));

        ReflectionUtils.invokeMethod(stage, "setFocused", new Class[] { boolean.class }, false);

        assertFalse(stage.isFocused());
        assertTrue(overlay.getChildrenUnmodifiable().getFirst().getPseudoClassStates().stream().noneMatch(
                pc -> pc.getPseudoClassName().equals("active")));
    }

    /**
     * Asserts that the maximize button is disabled when the stage is not resizable.
     */
    @Test
    void maximizeButtonIsDisabledWhenStageIsNotResizable() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: right; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, false, false);

        var scene = new Scene(overlay);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();

        var maxButton = overlay.getChildrenUnmodifiable().get(1);
        assertTrue(maxButton.getStyleClass().contains("-FX-INTERNAL-maximize-button"));
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
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: right; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, false, false);

        var scene = new Scene(overlay);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();

        var maxButton = overlay.getChildrenUnmodifiable().get(1);
        assertTrue(maxButton.getStyleClass().contains("-FX-INTERNAL-maximize-button"));
        assertFalse(maxButton.getStyleClass().contains("restore"));

        stage.setMaximized(true);
        assertTrue(maxButton.getStyleClass().contains("restore"));
    }

    /**
     * Asserts that the .dark style class is added to all buttons when {@link Scene#getFill()} is dark.
     */
    @Test
    void darkStyleClassIsPresentWhenSceneFillIsDark() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: right; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, false, false);

        var scene = new Scene(overlay);

        scene.setFill(Color.WHITE);
        assertTrue(overlay.getChildrenUnmodifiable().stream().noneMatch(b -> b.getStyleClass().contains("dark")));

        scene.setFill(Color.BLACK);
        assertTrue(overlay.getChildrenUnmodifiable().stream().allMatch(b -> b.getStyleClass().contains("dark")));
    }

    /**
     * Tests button picking using {@link HeaderButtonOverlay#buttonAt(double, double)}.
     */
    @Test
    void pickButtonAtCoordinates() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: right; -fx-button-vertical-alignment: stretch; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, false, false);

        var unused = new Scene(overlay);
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        assertNull(overlay.buttonAt(139, 5));
        assertEquals(HeaderButtonType.ICONIFY, overlay.buttonAt(140, 0));
        assertEquals(HeaderButtonType.MAXIMIZE, overlay.buttonAt(165, 5));
        assertEquals(HeaderButtonType.CLOSE, overlay.buttonAt(181, 10));
    }

    /**
     * Tests that clicking the close button fires the {@link WindowEvent#WINDOW_CLOSE_REQUEST} event.
     */
    @Test
    void closeButtonFiresWindowCloseRequestEvent() {
        var overlay = new HeaderButtonOverlay(getStylesheet("""
                .-FX-INTERNAL-header-button-container { -fx-button-placement: right; -fx-button-vertical-alignment: stretch; }
                .-FX-INTERNAL-header-button { -fx-pref-width: 20; -fx-pref-height: 10; }
            """), false, false, false);

        var stage = new Stage();
        var scene = new Scene(overlay);
        stage.setScene(scene);
        overlay.resize(200, 100);
        overlay.applyCss();
        overlay.layout();

        var flag = new boolean[1];
        stage.setOnCloseRequest(_ -> flag[0] = true);
        overlay.handleMouseEvent(MouseEvent.DOWN, MouseEvent.BUTTON_LEFT, 181, 10);
        overlay.handleMouseEvent(MouseEvent.UP, MouseEvent.BUTTON_LEFT, 181, 10);
        assertTrue(flag[0]);
    }

    private static ObservableValue<String> getStylesheet(String text) {
        String stylesheet = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));

        return ObjectConstant.valueOf(stylesheet);
    }

    private static void assertLayoutBounds(Node node, double x, double y, double width, double height) {
        var bounds = node.localToScene(node.getBoundsInLocal());
        assertEquals(x, bounds.getMinX());
        assertEquals(y, bounds.getMinY());
        assertSize(node, width, height);
    }

    private static void assertSize(Node node, double width, double height) {
        assertEquals(width, node.getLayoutBounds().getWidth());
        assertEquals(height, node.getLayoutBounds().getHeight());
    }
}
