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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.robot.testharness.VisualTestBase;
import test.util.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.util.Util.PARAMETERIZED_TEST_DISPLAY;
import static test.util.Util.TIMEOUT;

class StageOwnershipTest extends VisualTestBase {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;
    private static final double BOUNDS_EDGE_DELTA = 75;
    private Stage topStage;
    private Stage bottomStage;
    private static final Color TOP_COLOR = Color.RED;
    private static final Color BOTTOM_COLOR = Color.LIME;
    private static final Color COLOR1 = Color.RED;
    private static final Color COLOR2 = Color.ORANGE;
    private static final Color COLOR3 = Color.YELLOW;
    private static final Color COLOR4 = Color.GREEN;
    private static final Color COLOR5 = Color.BLUE;
    private static final Color COLOR6 = Color.INDIGO;
    private static final Color COLOR7 = Color.VIOLET;
    private static final double TOLERANCE = 0.07;
    private static final int WAIT_TIME = 500;

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

    private void setupTopStage(Stage owner, Modality modality) {
        runAndWait(() -> {
            topStage = getStage(true);
            topStage.initStyle(StageStyle.DECORATED);
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

    @Test
    void testOpeningModalChildStageWhileMaximized() throws InterruptedException {
        setupBottomStage();
        setupTopStage(bottomStage, Modality.WINDOW_MODAL);

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

    @Test
    void testOpeningModalChildStageWhileFullSceen() throws InterruptedException {
        setupBottomStage();
        setupTopStage(bottomStage, Modality.WINDOW_MODAL);

        Util.doTimeLine(WAIT_TIME,
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

    @Test
    void testOpeningAppModalStageWhileMaximized() throws InterruptedException {
        setupBottomStage();
        setupTopStage(null, Modality.APPLICATION_MODAL);

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

    @Test
    void testOpeningAppModalStageWhileFullScreen() throws InterruptedException {
        setupBottomStage();
        setupTopStage(null, Modality.APPLICATION_MODAL);

        Util.doTimeLine(WAIT_TIME,
                () -> bottomStage.setFullScreen(true),
                topStage::show,
                () -> {
                    assertTrue(bottomStage.isFullScreen());

                    // Make sure state is still maximized
                    assertColorEqualsVisualBounds(BOTTOM_COLOR);

                    Color color = getColor(100, 100);
                    assertColorEquals(TOP_COLOR, color, TOLERANCE);
                });
    }

    private Stage createStage(StageStyle stageStyle, Color color, Stage owner, Modality modality, int x, int y) {
        Stage stage = getStage(true);
        stage.initStyle(stageStyle);
        StackPane pane = getFocusedLabel(color, stage);
        Scene scene = new Scene(pane, WIDTH, HEIGHT);
        scene.setFill(color);
        stage.setScene(scene);
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        if (x > -1) stage.setX(x);
        if (y > -1) stage.setY(y);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(modality);
        return stage;
    }

    private static StackPane getFocusedLabel(Color color, Stage stage) {
        Label label = new Label();
        label.textProperty().bind(Bindings.when(stage.focusedProperty())
                .then("Focused").otherwise("Unfocused"));
        StackPane pane = new StackPane(label);
        pane.setBackground(Background.EMPTY);

        double luminance = 0.2126 * color.getRed()
                + 0.7152 * color.getGreen()
                + 0.0722 * color.getBlue();

        Color textColor = luminance < 0.5 ? Color.WHITE : Color.BLACK;

        label.setTextFill(textColor);
        return pane;
    }

    private void assertColorEquals(Color expected, Stage stage) {
        Color color = getColor((int) stage.getX() + 15, (int) stage.getY() + 55);
        assertColorEquals(expected, color, TOLERANCE);
    }

    private void assertColorDoesNotEqual(Color notExpected, Stage stage) {
        Color color = getColor((int) stage.getX() + 15, (int) stage.getY() + 55);
        assertColorDoesNotEqual(notExpected, color, TOLERANCE);
    }

    private Stage stage0;
    private Stage stage1;
    private Stage stage2;
    private Stage stage3;
    private Stage stage4;
    private Stage stage5;
    private Stage stage6;


    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"UNDECORATED", "DECORATED"})
    void testLayeredModality(StageStyle style) {
        Util.runAndWait(() -> {
            stage0 = createStage(style, COLOR1, null, null, 100, 100);
            stage1 = createStage(style, COLOR2, stage0, Modality.WINDOW_MODAL, 150, 150);
            stage2 = createStage(style, COLOR3, stage1, Modality.WINDOW_MODAL, 200, 200);
            stage3 = createStage(style, COLOR4, stage2, Modality.WINDOW_MODAL, 250, 250);
            stage4 = createStage(style, COLOR5, stage3, Modality.WINDOW_MODAL,  300, 300);
            stage5 = createStage(style, COLOR6, stage4, Modality.WINDOW_MODAL,  350, 350);
            stage6 = createStage(style, COLOR7, stage5, Modality.WINDOW_MODAL,  400, 400);
        });

        Util.doTimeLine(300,
                stage0::show,
                stage1::show,
                stage2::show,
                stage3::show,
                stage4::show,
                stage5::show,
                stage6::show,
                () -> {
                    assertColorEquals(COLOR1, stage0);
                    assertColorEquals(COLOR2, stage1);
                    assertColorEquals(COLOR3, stage2);
                    assertColorEquals(COLOR4, stage3);
                    assertColorEquals(COLOR5, stage4);
                    assertColorEquals(COLOR6, stage5);
                    assertColorEquals(COLOR7, stage6);
                },
                () -> assertTrue(stage6.isFocused()),
                stage5::close,
                () -> assertTrue(stage6.isFocused()),
                stage6::close,
                stage5::close,
                () -> assertColorEquals(COLOR5, stage4));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"UNDECORATED", "DECORATED"})
    void testMultiLayeredModality(StageStyle style) {
        Util.runAndWait(() -> {
            stage0 = createStage(style, COLOR1, null, Modality.NONE, 100, 100);
            stage1 = createStage(style, COLOR2, stage0, Modality.WINDOW_MODAL, 150, 150);
            stage2 = createStage(style, COLOR3, stage1, Modality.WINDOW_MODAL, 200, 200);

            stage3 = createStage(style, COLOR4, null,  Modality.NONE, 600, 100);
            stage4 = createStage(style, COLOR5, stage3,  Modality.WINDOW_MODAL, 650, 150);
            stage5 = createStage(style, COLOR6, stage4,  Modality.WINDOW_MODAL, 700, 200);
        });

        Util.doTimeLine(WAIT_TIME,
                stage0::show,
                stage1::show,
                stage2::show,
                stage3::show,
                stage4::show,
                stage5::show,
                () -> {
                    assertColorEquals(COLOR1, stage0);
                    assertColorEquals(COLOR2, stage1);
                    assertColorEquals(COLOR3, stage2);
                    assertColorEquals(COLOR4, stage3);
                    assertColorEquals(COLOR5, stage4);
                    assertColorEquals(COLOR6, stage5);
                },
                () -> assertTrue(stage5.isFocused()),
                stage5::close,
                () -> assertTrue(stage4.isFocused()),
                stage4::close,
                () -> assertTrue(stage3.isFocused()),
                stage3::close,
                () -> assertTrue(stage2.isFocused()),
                stage2::close,
                () -> assertTrue(stage1.isFocused()),
                stage1::close,
                () -> assertTrue(stage0.isFocused()));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"UNDECORATED", "DECORATED"})
    void testIconfyRestoreChildren(StageStyle style) {
        Util.runAndWait(() -> {
            stage0 = createStage(style, COLOR1, null, Modality.NONE, 100, 100);
            stage1 = createStage(style, COLOR2, stage0, Modality.WINDOW_MODAL, 150, 150);
            stage2 = createStage(style, COLOR3, stage1, Modality.WINDOW_MODAL, 200, 200);
        });

        Util.doTimeLine(WAIT_TIME,
                stage0::show,
                stage1::show,
                stage2::show,
                () -> stage0.setIconified(true),
                () -> {
                    assertTrue(stage0.isIconified());
                    assertTrue(stage1.isIconified());
                    assertTrue(stage2.isIconified());
                    assertColorDoesNotEqual(COLOR1, stage0);
                    assertColorDoesNotEqual(COLOR2, stage1);
                    assertColorDoesNotEqual(COLOR3, stage2);
                },
                () -> stage2.setIconified(false),
                () -> {
                    assertColorEquals(COLOR1, stage0);
                    assertColorEquals(COLOR2, stage1);
                    assertColorEquals(COLOR3, stage2);
                });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"UNDECORATED", "DECORATED"})
    void testChildStageWithoutModality(StageStyle style) {
        Util.runAndWait(() -> {
            stage0 = createStage(style, COLOR1, null, Modality.NONE, 100, 100);
            stage1 = createStage(style, COLOR2, stage0, Modality.NONE, 150, 150);
            stage2 = createStage(style, COLOR3, stage1, Modality.NONE, 200, 200);
        });

        Util.doTimeLine(WAIT_TIME,
                stage0::show,
                stage1::show,
                stage2::show,
                () -> stage0.setIconified(true),
                () -> {
                    assertTrue(stage0.isIconified());
                    assertTrue(stage1.isIconified());
                    assertTrue(stage2.isIconified());
                    assertColorDoesNotEqual(COLOR1, stage0);
                    assertColorDoesNotEqual(COLOR2, stage1);
                    assertColorDoesNotEqual(COLOR3, stage2);
                },
                () -> stage2.setIconified(false),
                () -> {
                    assertColorEquals(COLOR1, stage0);
                    assertColorEquals(COLOR2, stage1);
                    assertColorEquals(COLOR3, stage2);
                });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"UNDECORATED", "DECORATED"})
    void testMultipleChildren(StageStyle style) {
        Util.runAndWait(() -> {
            stage0 = createStage(style, COLOR1, null, Modality.NONE, -1, -1);
            stage1 = createStage(style, COLOR2, stage0, Modality.NONE, -1, -1);
            stage2 = createStage(style, COLOR3, stage0, Modality.NONE, -1, -1);
        });
        Util.doTimeLine(WAIT_TIME,
                () -> {
                    stage0.show();
                    stage1.show();
                    stage2.show();
                },
                () -> {
                    stage2.setY(stage0.getY());
                    stage1.setX(stage0.getX() - 300);
                    stage1.setY(stage0.getY());
                    stage2.setX(stage0.getX() + 300);
                },
                () -> stage0.setIconified(true),
                () -> {
                    assertColorDoesNotEqual(COLOR1, stage0);
                    assertColorDoesNotEqual(COLOR2, stage1);
                    assertColorDoesNotEqual(COLOR3, stage2);
                },
                () -> stage0.setIconified(false),
                () -> {
                    assertColorEquals(COLOR1, stage0);
                    assertColorEquals(COLOR2, stage1);
                    assertColorEquals(COLOR3, stage2);
                });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"UNDECORATED", "DECORATED"})
    void testClosingAppModalShouldFocusParent(StageStyle style) {
        Util.runAndWait(() -> {
            stage0 = createStage(style, COLOR1, null, Modality.NONE, -1, -1);
            stage1 = createStage(style, COLOR2, null, Modality.NONE, -1, -1);
            stage2 = createStage(style, COLOR3, stage0, Modality.APPLICATION_MODAL, -1, -1);
        });

        Util.doTimeLine(WAIT_TIME,
                stage1::show,
                stage0::show,
                () -> stage1.requestFocus(),
                stage2::show,
                stage2::close,
                () -> assertTrue(stage1.isFocused()));
    }
}
