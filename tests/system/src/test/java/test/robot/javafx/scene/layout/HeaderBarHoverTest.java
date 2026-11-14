/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene.layout;

import com.sun.glass.ui.HeaderButtonOverlay;
import com.sun.glass.ui.Window;
import com.sun.javafx.PlatformUtil;
import java.util.concurrent.CountDownLatch;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderButtonType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.robot.testharness.VisualTestBase;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Checks mouse transitions between application content and the window button overlay.
 */
@Timeout(20)
public class HeaderBarHoverTest extends VisualTestBase {

    private static final PseudoClass HOVER = PseudoClass.getPseudoClass("hover");

    private Stage stage;
    private BorderPane root;
    private Button button;
    private Node windowButton;

    @BeforeEach
    public void showStage() {
        assumeTrue(PlatformUtil.isWindows() || PlatformUtil.isLinux(), "Requires a window button overlay");

        runAndWait(() -> {
            button = new Button("Settings");
            HeaderBar headerBar = new HeaderBar();
            headerBar.setRight(button);
            root = new BorderPane();
            root.setTop(headerBar);
            stage = getStage();
            stage.initStyle(StageStyle.EXTENDED);
            stage.setScene(new Scene(root, 600, 320));
            stage.show();
            stage.centerOnScreen();
        });

        waitFirstFrame();
        moveTo(root);
        waitNextFrame();
    }

    @ParameterizedTest
    @EnumSource(HeaderButtonType.class)
    public void hoverClearsWhenEnteringWindowButton(HeaderButtonType buttonType) {
        selectWindowButton(buttonType);
        enterApplicationButton();
        enterWindowButton();
        enterApplicationButton();
        checkHoverAfterLayoutChange();
        enterWindowButton();

        // Leave the native window while the scene already considers the mouse outside its content.
        runAndWait(() -> getRobot().mouseMove(stage.getX() + stage.getWidth() / 2, stage.getY() - 20));
        waitNextFrame();
        runAndWait(() -> {
            assertFalse(windowButton.getPseudoClassStates().contains(HOVER));
            assertFalse(root.isHover());
        });

        // A native enter over a window button must keep the application content outside the hover path.
        moveTo(windowButton);
        waitNextFrame();
        runAndWait(() -> {
            assertTrue(windowButton.getPseudoClassStates().contains(HOVER));
            assertFalse(root.isHover());
        });

        enterApplicationButton();
    }

    @Test
    public void hoverRestoresWhenWindowButtonsAreHidden() {
        selectWindowButton(HeaderButtonType.ICONIFY);
        enterApplicationButton();
        enterWindowButton();

        // Hide the window buttons while the cursor is on one of them. The application must get the cursor back.
        CountDownLatch entered = new CountDownLatch(1);
        runAndWait(() -> {
            button.setOnMouseEntered(_ -> entered.countDown());
            HeaderBar.setSystemButtonHeight(stage, 0);
            windowButton = null;
        });

        waitNextFrame();
        moveTo(button);
        awaitApplicationButton(entered);
        checkHoverAfterLayoutChange();

        // Show the window buttons again. This creates a new overlay, which must also get the cursor.
        runAndWait(() -> HeaderBar.setSystemButtonHeight(stage, HeaderBar.USE_DEFAULT_SIZE));
        waitNextFrame();
        selectWindowButton(HeaderButtonType.ICONIFY);
        enterApplicationButton();
        enterWindowButton();
    }

    @Test
    public void applicationKeepsGestureOverWindowButton() {
        selectWindowButton(HeaderButtonType.ICONIFY);
        enterApplicationButton();

        CountDownLatch released = new CountDownLatch(1);
        runAndWait(() -> {
            button.setOnMouseReleased(_ -> released.countDown());
            getRobot().mousePress(MouseButton.PRIMARY);
        });

        moveTo(windowButton);
        waitNextFrame();
        runAndWait(() -> {
            assertTrue(root.isHover());
            assertFalse(windowButton.getPseudoClassStates().contains(HOVER));
        });

        runAndWait(() -> getRobot().mouseRelease(MouseButton.PRIMARY));
        Util.waitForLatch(released, 5, "Application button did not receive the release");

        moveTo(windowButton, 2);
        waitNextFrame();
        runAndWait(() -> {
            assertFalse(button.isHover());
            assertFalse(root.isHover());
            assertTrue(windowButton.getPseudoClassStates().contains(HOVER));
        });
    }

    private void selectWindowButton(HeaderButtonType buttonType) {
        runAndWait(() -> {
            HeaderButtonOverlay overlay = Window.getWindows().stream()
                .map(window -> window.headerButtonOverlayProperty().get())
                .filter(node -> node instanceof HeaderButtonOverlay && node.getScene() == stage.getScene())
                .map(node -> (HeaderButtonOverlay)node)
                .findFirst().orElseThrow();

            String selector = switch (buttonType) {
                case ICONIFY -> ".-FX-INTERNAL-iconify-button";
                case MAXIMIZE -> ".-FX-INTERNAL-maximize-button";
                case CLOSE -> ".-FX-INTERNAL-close-button";
            };

            windowButton = overlay.lookup(selector);
            assertNotNull(windowButton);
            assertTrue(windowButton.isVisible());
        });
    }

    private void enterApplicationButton() {
        CountDownLatch entered = new CountDownLatch(1);
        runAndWait(() -> button.setOnMouseEntered(_ -> entered.countDown()));
        moveTo(button);
        awaitApplicationButton(entered);
    }

    private void awaitApplicationButton(CountDownLatch entered) {
        Util.waitForLatch(entered, 5, "Mouse did not enter the application button");

        runAndWait(() -> {
            assertTrue(button.isHover());
            assertTrue(root.isHover());
            if (windowButton != null) {
                assertFalse(windowButton.getPseudoClassStates().contains(HOVER));
            }
        });
    }

    private void checkHoverAfterLayoutChange() {
        CountDownLatch exited = new CountDownLatch(1);
        runAndWait(() -> {
            button.setOnMouseExited(_ -> exited.countDown());
            button.setTranslateX(-2 * button.getWidth());
        });

        Util.waitForLatch(exited, 5, "Layout change did not clear the application button's hover state");
        runAndWait(() -> {
            assertFalse(button.isHover());
            assertTrue(root.isHover());
        });

        CountDownLatch entered = new CountDownLatch(1);
        runAndWait(() -> {
            button.setOnMouseEntered(event -> entered.countDown());
            button.setTranslateX(0);
        });

        awaitApplicationButton(entered);
    }

    private void enterWindowButton() {
        CountDownLatch exited = new CountDownLatch(1);
        runAndWait(() -> button.setOnMouseExited(event -> exited.countDown()));
        moveTo(windowButton);

        Util.waitForLatch(exited, 5, "Mouse did not exit the application button");
        runAndWait(() -> {
            assertFalse(button.isHover());
            assertFalse(root.isHover());
            assertTrue(windowButton.getPseudoClassStates().contains(HOVER));
        });
    }

    private void moveTo(Node node) {
        moveTo(node, 0);
    }

    private void moveTo(Node node, double offsetX) {
        runAndWait(() -> {
            Bounds bounds = node.getBoundsInLocal();
            Point2D point = node.localToScreen(bounds.getCenterX() + offsetX, bounds.getCenterY());
            assertNotNull(point);
            getRobot().mouseMove(point);
        });
    }
}
