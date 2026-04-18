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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static test.util.Util.FOCUS_DELAY;
import static test.util.Util.GEOMETRY_DELAY;
import static test.util.Util.PARAMETERIZED_TEST_DISPLAY;
import static test.util.Util.STATE_DELAY;
import static test.util.Util.TIMEOUT;
import static test.util.Util.waitForBoolean;

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

    @Override
    protected Stage getStage(boolean alwaysOnTop) {
        Stage stage = super.getStage(alwaysOnTop);
        stage.setFullScreenExitHint(
                "Will BEEP on macOS when exiting fullscreen due to OS trying to focus a disabled stage");
        return stage;
    }

    private void setupBottomStage() {
        runAndWait(() -> {
            bottomStage = getStage(false);
            bottomStage.initStyle(StageStyle.DECORATED);
            Scene bottomScene = new Scene(getFocusedLabel(BOTTOM_COLOR, bottomStage), WIDTH, HEIGHT);
            bottomScene.setFill(BOTTOM_COLOR);
            bottomStage.setScene(bottomScene);
            bottomStage.setX(0);
            bottomStage.setY(0);
        });
        showStageAndWait(bottomStage);
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
        if (modality != null) {
            stage.initModality(modality);
        }
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

    private void showStageAndWait(Stage... stages) {
        CountDownLatch latch = new CountDownLatch(stages.length);
        runAndWait(() -> {
            for (Stage stage : stages) {
                stage.setOnShown(e -> Platform.runLater(latch::countDown));
                stage.show();
            }
        });
        try {
            assertTrue(latch.await(TIMEOUT, TimeUnit.MILLISECONDS),
                    "Timeout waiting for stage(s) to be shown");
        } catch (InterruptedException e) {
            fail(e);
        }
        Util.sleep(FOCUS_DELAY);
    }

    private static Stream<Arguments> getTestsParams() {
        return Stream.of(StageStyle.DECORATED, StageStyle.UNDECORATED, StageStyle.EXTENDED)
                .flatMap(stageStyle -> Stream.of(Modality.APPLICATION_MODAL, Modality.WINDOW_MODAL)
                        .map(modality -> Arguments.of(stageStyle, modality)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @MethodSource("getTestsParams")
    void openingModalChildStageWhileMaximizedShouldNotUnmaximize(StageStyle stageStyle, Modality modality) {
        setupBottomStage();
        setupTopStage(bottomStage, stageStyle, modality);

        runAndWait(() -> bottomStage.setMaximized(true));
        waitForBoolean(bottomStage.maximizedProperty(), true);

        showStageAndWait(topStage);

        runAndWait(() -> {
            assertColorEqualsVisualBounds(BOTTOM_COLOR);

            Color color = getColor(100, 100);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @MethodSource("getTestsParams")
    // This test causes BEEP on macOS
    void openingModalChildStageWhileFullScreenShouldHaveFocus(StageStyle stageStyle, Modality modality) {
        setupBottomStage();
        setupTopStage(bottomStage, stageStyle, modality);

        runAndWait(() -> bottomStage.setFullScreen(true));
        waitForBoolean(bottomStage.fullScreenProperty(), true);
        Util.sleep(STATE_DELAY);

        showStageAndWait(topStage);

        runAndWait(() -> {
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
        runAndWait(() -> {
            stage0 = createStage(style, COLOR0, null, null, 100, 100);
            stage1 = createStage(style, COLOR1, stage0, null, 150, 150);
            stage2 = createStage(style, COLOR2, stage1, modality, 200, 200);
        });

        showStageAndWait(stage0);
        showStageAndWait(stage1);
        showStageAndWait(stage2);

        waitForBoolean(stage0.focusedProperty(), false);
        Util.sleep(FOCUS_DELAY);
        waitForBoolean(stage1.focusedProperty(), false);
        Util.sleep(FOCUS_DELAY);
        waitForBoolean(stage2.focusedProperty(), true);
        Util.sleep(FOCUS_DELAY);

        runAndWait(() -> assertColorEquals(COLOR2, stage2));

        runAndWait(stage2::close);
        waitForBoolean(stage0.focusedProperty(), false);
        waitForBoolean(stage1.focusedProperty(), true);
        Util.sleep(FOCUS_DELAY);

        runAndWait(() -> assertColorEquals(COLOR1, stage1));

        runAndWait(stage1::close);
        waitForBoolean(stage0.focusedProperty(), true);
        Util.sleep(FOCUS_DELAY);

        runAndWait(() -> assertColorEquals(COLOR0, stage0));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED"})
    void closingOwnerShouldCloseOwnedChildren(StageStyle style) {
        runAndWait(() -> {
            stage0 = createStage(style, COLOR0, null, null, 100, 100);
            stage1 = createStage(style, COLOR1, stage0, null, 150, 150);
            stage2 = createStage(style, COLOR2, stage1, null, 200, 200);
        });

        showStageAndWait(stage0, stage1, stage2);

        runAndWait(() -> {
            assertTrue(stage0.isShowing());
            assertTrue(stage1.isShowing());
            assertTrue(stage2.isShowing());
        });

        runAndWait(stage0::close);
        waitForBoolean(stage0.showingProperty(), false);
        waitForBoolean(stage1.showingProperty(), false);
        waitForBoolean(stage2.showingProperty(), false);
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED"})
    void closingMiddleStageInChainShouldCloseDescendants(StageStyle style) {
        runAndWait(() -> {
            stage0 = createStage(style, COLOR0, null, null, 100, 100);
            stage1 = createStage(style, COLOR1, stage0, null, 150, 150);
            stage2 = createStage(style, COLOR2, stage1, null, 200, 200);
        });

        showStageAndWait(stage0, stage1, stage2);
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(() -> {
            assertTrue(stage0.isShowing());
            assertTrue(stage1.isShowing());
            assertTrue(stage2.isShowing());
        });

        runAndWait(stage1::close);
        waitForBoolean(stage0.showingProperty(), true);
        waitForBoolean(stage1.showingProperty(), false);
        waitForBoolean(stage2.showingProperty(), false);
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED"})
    void ownedStageShouldAlwaysBeOnTopOfOwner(StageStyle style) {
        runAndWait(() -> {
            stage0 = createStage(style, COLOR0, null, null, 0, 0);
            stage1 = createStage(style, COLOR1, stage0, null, 0, 0);
        });

        showStageAndWait(stage0, stage1);

        runAndWait(() -> assertColorEquals(COLOR1, stage1));
        runAndWait(stage0::toFront);
        Util.sleep(FOCUS_DELAY);

        runAndWait(() -> assertColorEquals(COLOR1, stage1));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED"})
    void multipleChildrenOfSameOwner(StageStyle style) {
        runAndWait(() -> {
            stage0 = createStage(style, COLOR0, null, null, 0, 0);
            stage0.setMaximized(true);
            stage1 = createStage(style, COLOR1, stage0, null, 100, 100);
            stage2 = createStage(style, COLOR2, stage0, null, 350, 100);
        });
        showStageAndWait(stage0, stage1, stage2);
        waitForBoolean(stage0.maximizedProperty(), true);
        Util.sleep(STATE_DELAY);

        runAndWait(() -> {
            assertColorEquals(COLOR1, stage1);
            assertColorEquals(COLOR2, stage2);
        });

        runAndWait(stage1::close);
        waitForBoolean(stage1.showingProperty(), false);

        runAndWait(() -> {
            assertTrue(stage2.isShowing());
            assertColorEquals(COLOR2, stage2);
        });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED"})
    void closingNonModalChildShouldFocusOwner(StageStyle style) {
        runAndWait(() -> {
            stage0 = createStage(style, COLOR0, null, null, 100, 100);
            stage1 = createStage(style, COLOR1, stage0, null, 150, 150);
        });

        showStageAndWait(stage0, stage1);
        waitForBoolean(stage1.focusedProperty(), true);
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(stage1::close);
        waitForBoolean(stage0.focusedProperty(), true);
        Util.sleep(FOCUS_DELAY);
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED"})
    void maximizingOwnerShouldNotHideOwnedChildren(StageStyle style) {
        runAndWait(() -> {
            stage0 = createStage(style, COLOR0, null, null, 100, 100);
            stage1 = createStage(style, COLOR1, stage0, null, 150, 150);
        });

        showStageAndWait(stage0, stage1);

        runAndWait(() -> stage0.setMaximized(true));
        waitForBoolean(stage0.maximizedProperty(), true);
        Util.sleep(GEOMETRY_DELAY);
        waitForBoolean(stage1.showingProperty(), true);
        Util.sleep(FOCUS_DELAY);

        runAndWait(() -> assertColorEquals(COLOR1, stage1));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED"})
    void iconifyParentShouldHideChildren(StageStyle style) {
        if (style == StageStyle.EXTENDED || style == StageStyle.DECORATED) {
            assumeTrue(!PlatformUtil.isWindows());
        }

        runAndWait(() -> {
            stage0 = createStage(StageStyle.UNDECORATED, COLOR0, null, null, 0, 0);
            stage0.setMaximized(true);
            stage1 = createStage(style, COLOR1, null, null, 100, 100);
            stage2 = createStage(style, COLOR2, stage1, null, 200, 150);
            stage3 = createStage(style, COLOR3, stage2, null, 300, 200);
        });

        showStageAndWait(stage0, stage1, stage2, stage3);
        waitForBoolean(stage0.maximizedProperty(), true);
        Util.sleep(GEOMETRY_DELAY);

        runAndWait(() -> stage1.setIconified(true));
        waitForBoolean(stage1.iconifiedProperty(), true);
        Util.sleep(STATE_DELAY);

        runAndWait(() -> {
            assertColorEquals(COLOR0, stage1);
            assertColorEquals(COLOR0, stage2);
            assertColorEquals(COLOR0, stage3);
        });

        runAndWait(() -> stage1.setIconified(false));
        waitForBoolean(stage1.iconifiedProperty(), false);
        Util.sleep(STATE_DELAY);

        runAndWait(() -> {
            assertFalse(stage1.isIconified());
            assertColorEquals(COLOR1, stage1);
            assertColorEquals(COLOR2, stage2);
            assertColorEquals(COLOR3, stage3);
        });
    }
}
