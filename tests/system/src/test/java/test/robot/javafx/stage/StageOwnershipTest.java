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
package test.robot.javafx.stage;

import com.sun.javafx.PlatformUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import test.robot.testharness.VisualTestBase;
import test.util.Util;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static test.util.Util.PARAMETERIZED_TEST_DISPLAY;
import static test.util.Util.TIMEOUT;

@Timeout(value = TIMEOUT, unit = TimeUnit.MILLISECONDS)
class StageOwnershipTest extends VisualTestBase {
    private static final int WIDTH = 200;
    private static final int HEIGHT = 200;
    private static final double BOUNDS_EDGE_DELTA = 75;
    private Stage topStage;
    private Stage bottomStage;
    private static final Color TOP_COLOR = Color.RED;
    private static final Color BOTTOM_COLOR = Color.LIME;
    private static final Color COLOR0 = Color.RED;
    private static final Color COLOR1 = Color.ORANGE;
    private static final Color COLOR2 = Color.YELLOW;
    private static final Color COLOR3 = Color.GREEN;
    private static final int X_DELTA = 15; // shadows
    private static final int Y_DELTA = 75; // shadows + decoration

    private static final double TOLERANCE = 0.07;
    private static final int WAIT_TIME = 500;
    private static final int LONG_WAIT_TIME = 1000;

    @Override
    protected Stage getStage(boolean alwaysOnTop) {
        Stage stage = super.getStage(alwaysOnTop);
        stage.setFullScreenExitHint(
                "Will BEEP on macOS when exiting fullscreen due to OS trying to focus a disabled stage");
        return stage;
    }

    private void setupBottomStage() throws InterruptedException {
        final CountDownLatch shownLatch = new CountDownLatch(1);

        runAndWait(() -> {
            bottomStage = getStage(false);
            bottomStage.initStyle(StageStyle.DECORATED);
            Scene bottomScene = new Scene(getFocusedLabel(BOTTOM_COLOR, bottomStage), WIDTH, HEIGHT);
            bottomScene.setFill(BOTTOM_COLOR);
            bottomStage.setScene(bottomScene);
            bottomStage.setX(0);
            bottomStage.setY(0);
            bottomStage.setOnShown(e -> Platform.runLater(shownLatch::countDown));
            bottomStage.show();
        });
        assertTrue(shownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS),
                "Timeout waiting for bottom stage to be shown");

        sleep(WAIT_TIME);
    }

    private void setupTopStage(Stage owner, StageStyle stageStyle, Modality modality) {
        runAndWait(() -> {
            topStage = getStage(true);
            if (stageStyle != null) {
                topStage.initStyle(stageStyle);
            }
            Scene topScene = new Scene(getFocusedLabel(TOP_COLOR, topStage), WIDTH, HEIGHT);
            topScene.setFill(TOP_COLOR);
            topStage.setScene(topScene);
            if (owner != null) {
                topStage.initOwner(owner);
            }
            if (modality != null) {
                topStage.initModality(modality);
            }
            topStage.setWidth(WIDTH);
            topStage.setHeight(HEIGHT);
            topStage.setX(0);
            topStage.setY(0);
        });
    }

    private void assertColorEqualsVisualBounds(Color expected) {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        int x = (int) (visualBounds.getWidth() - BOUNDS_EDGE_DELTA);
        int y = (int) (visualBounds.getHeight() - BOUNDS_EDGE_DELTA);

        Color color = getColor(x, y);
        assertColorEquals(expected, color, TOLERANCE);
    }

    private Stage createStage(StageStyle stageStyle, Color color, Stage owner, Modality modality, int x, int y) {
        Stage stage = getStage(true);
        stage.initStyle(stageStyle);
        Pane pane = getFocusedLabel(color, stage);
        Scene scene = new Scene(pane, WIDTH, HEIGHT);
        scene.setFill(color);
        stage.setScene(scene);
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        if (x != -1) {
            stage.setX(x);
        }
        if (y != -1) {
            stage.setY(y);
        }
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(modality);
        return stage;
    }

    private static Pane getFocusedLabel(Color color, Stage stage) {
        Label label = new Label();
        label.textProperty().bind(Bindings.when(stage.focusedProperty())
                .then("Focused").otherwise("Unfocused"));

        BorderPane pane = new BorderPane();
        pane.setBackground(Background.EMPTY);
        pane.setBottom(label);
        BorderPane.setAlignment(label, Pos.CENTER_RIGHT);

        double luminance = 0.2126 * color.getRed()
                + 0.7152 * color.getGreen()
                + 0.0722 * color.getBlue();

        Color textColor = luminance < 0.5 ? Color.WHITE : Color.BLACK;

        label.setTextFill(textColor);
        return pane;
    }

    private void assertColorEquals(Color expected, Stage stage) {
        Color color = getColor((int) stage.getX() + X_DELTA, (int) stage.getY() + Y_DELTA);
        assertColorEquals(expected, color, TOLERANCE);
    }

    private static Stream<Arguments> getTestsParams() {
        return Stream.of(StageStyle.DECORATED, StageStyle.UNDECORATED, StageStyle.EXTENDED)
                .flatMap(stageStyle -> Stream.of(Modality.APPLICATION_MODAL, Modality.WINDOW_MODAL)
                        .map(modality -> Arguments.of(stageStyle, modality)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @MethodSource("getTestsParams")
    void openingModalChildStageWhileMaximizedShouldNotUnmaximize(StageStyle stageStyle, Modality modality)
            throws InterruptedException {
        setupBottomStage();
        setupTopStage(bottomStage, stageStyle, modality);

        Util.doTimeLine(WAIT_TIME,
            () -> bottomStage.setMaximized(true),
            topStage::show,
            () -> {
                assertTrue(bottomStage.isMaximized());
                // Make sure state is still maximized
                assertColorEqualsVisualBounds(BOTTOM_COLOR);

                Color color = getColor(100, 100);
                assertColorEquals(TOP_COLOR, color, TOLERANCE);
            });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @MethodSource("getTestsParams")
    // This test causes BEEP on macOS
    void openingModalChildStageWhileFullScreenShouldHaveFocus(StageStyle stageStyle, Modality modality)
            throws InterruptedException {
        setupBottomStage();
        setupTopStage(bottomStage, stageStyle, modality);

        Util.doTimeLine(LONG_WAIT_TIME,
                () -> bottomStage.setFullScreen(true),
                topStage::show,
                () -> {
                    assertTrue(bottomStage.isFullScreen());

                    // Make sure state is still fullscreen
                    assertColorEqualsVisualBounds(BOTTOM_COLOR);

                    Color color = getColor(100, 100);
                    assertColorEquals(TOP_COLOR, color, TOLERANCE);
                });
    }

    private Stage stage0;
    private Stage stage1;
    private Stage stage2;
    private Stage stage3;

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @MethodSource("getTestsParams")
    void closingModalWindowShouldFocusParent(StageStyle style, Modality modality) {
        CountDownLatch shownLatch = new CountDownLatch(1);
        Util.runAndWait(() -> {
            stage0 = createStage(style, COLOR0, null, null, 100, 100);
            stage1 = createStage(style, COLOR1, stage0, null, 150, 150);
            stage2 = createStage(style, COLOR2, stage1, modality, 200, 200);

            stage0.setOnShown(e -> Platform.runLater(shownLatch::countDown));
            stage0.show();
        });

        Util.await(shownLatch);
        Util.sleep(WAIT_TIME);

        Util.doTimeLine(WAIT_TIME,
            stage1::show,
            stage2::show,
            () -> {
                assertTrue(stage2.isFocused());
                assertColorEquals(COLOR2, stage2);
                assertFalse(stage1.isFocused());
                assertFalse(stage0.isFocused());
            },
            stage2::close,
            () -> {
                assertTrue(stage1.isFocused());
                assertColorEquals(COLOR1, stage1);
                assertFalse(stage0.isFocused());
            },
            stage1::close,
            () -> {
                assertTrue(stage0.isFocused());
                assertColorEquals(COLOR0, stage0);
            });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED"})
    void iconifyParentShouldHideChildren(StageStyle style) {
        if (style == StageStyle.EXTENDED || style == StageStyle.DECORATED) {
            // Windows just hides the first-level ownership (bug?)
            assumeTrue(!PlatformUtil.isWindows());
        }

        CountDownLatch shownLatch = new CountDownLatch(4);
        Util.runAndWait(() -> {
            stage0 = createStage(StageStyle.UNDECORATED, COLOR0, null, null, 0, 0);
            stage0.setMaximized(true);
            stage1 = createStage(style, COLOR1, null, null, 100, 100);
            stage2 = createStage(style, COLOR2, stage1, null, 200, 150);
            stage3 = createStage(style, COLOR3, stage2, null, 300, 200);

            List.of(stage0, stage1, stage2, stage3).forEach(stage -> {
                stage.setOnShown(e -> Platform.runLater(shownLatch::countDown));
                stage.show();
            });
        });

        Util.await(shownLatch);
        Util.sleep(WAIT_TIME);

        Util.doTimeLine(WAIT_TIME,
            () -> stage1.setIconified(true),
            () -> {
                assertTrue(stage1.isIconified());
                assertColorEquals(COLOR0, stage1);
                assertColorEquals(COLOR0, stage2);
                assertColorEquals(COLOR0, stage3);
            },
            () -> stage1.setIconified(false),
            () -> {
                assertFalse(stage1.isIconified());
                assertColorEquals(COLOR1, stage1);
                assertColorEquals(COLOR2, stage2);
                assertColorEquals(COLOR3, stage3);
            });
    }
}
