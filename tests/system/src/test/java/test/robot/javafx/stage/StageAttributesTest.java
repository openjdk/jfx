/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.robot.testharness.VisualTestBase;
import test.util.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.util.Util.PARAMETERIZED_TEST_DISPLAY;
import static test.util.Util.TIMEOUT;

public class StageAttributesTest extends VisualTestBase {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;

    private static final Color BOTTOM_COLOR = Color.LIME;
    private static final Color TOP_COLOR = Color.RED;

    private static final double TOLERANCE = 0.07;

    private static final int WAIT = 1000;
    private static final int SHORT_WAIT = 100;

    private Stage bottomStage;
    private Scene topScene;
    private Stage topStage;

    private void setupStages(boolean overlayed, boolean topShown, StageStyle topStageStyle)
            throws InterruptedException {
        final CountDownLatch bottomShownLatch = new CountDownLatch(1);
        final CountDownLatch topShownLatch = new CountDownLatch(1);

        runAndWait(() -> {
            // Bottom stage, should be visible after top stage is iconified
            bottomStage = getStage(false);
            bottomStage.initStyle(StageStyle.DECORATED);
            Scene bottomScene = new Scene(new Pane(), WIDTH, HEIGHT);
            bottomScene.setFill(BOTTOM_COLOR);
            bottomStage.setScene(bottomScene);
            bottomStage.setX(0);
            bottomStage.setY(0);
            bottomStage.setOnShown(e -> Platform.runLater(bottomShownLatch::countDown));
            bottomStage.show();
        });

        assertTrue(bottomShownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS), "Timeout waiting for bottom stage to be shown");

        runAndWait(() -> {
            // Top stage, will be inconified
            topStage = getStage(true);
            topStage.initStyle(topStageStyle);
            topScene = new Scene(new Pane(), WIDTH, HEIGHT);
            topScene.setFill(TOP_COLOR);
            topStage.setScene(topScene);
            if (overlayed) {
                topStage.setX(0);
                topStage.setY(0);
            } else {
                topStage.setX(WIDTH);
                topStage.setY(HEIGHT);
            }
            if (topShown) {
                topStage.setOnShown(e -> Platform.runLater(topShownLatch::countDown));
                topStage.show();
            }
        });

        if (topShown) {
            assertTrue(topShownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS), "Timeout waiting for top stage to be shown");
        }

        sleep(WAIT);
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class, mode = EnumSource.Mode.INCLUDE, names = {"DECORATED", "UNDECORATED"})
    public void testIconifiedStage(StageStyle stageStyle) throws InterruptedException {
        setupStages(true, true, stageStyle);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);

            topStage.setIconified(true);
        });

        // wait a bit to let window system animate the change
        sleep(WAIT);

        runAndWait(() -> {
            assertTrue(topStage.isIconified());
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);
        });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    public void testMaximizedStage(StageStyle stageStyle) throws InterruptedException {
        setupStages(false, true, stageStyle);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

            topStage.setMaximized(true);
        });

        // wait a bit to let window system animate the change
        sleep(WAIT);

        runAndWait(() -> {
            assertTrue(topStage.isMaximized());

            // maximized stage should take over the bottom stage
            Color color = getColor(200, 200);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });

        // Do not test decorations for UNDECORATED
        if (stageStyle.equals(StageStyle.UNDECORATED)) {
            return;
        }

        // wait a little bit between getColor() calls - on macOS the below one
        // would fail without this wait
        sleep(SHORT_WAIT);

        runAndWait(() -> {
            // top left corner (plus some tolerance) should show decorations (so not TOP_COLOR)
            Color color = getColor((int)topStage.getX() + 10, (int)topStage.getY() + 10);
            assertColorDoesNotEqual(TOP_COLOR, color, TOLERANCE);
            assertColorDoesNotEqual(BOTTOM_COLOR, color, TOLERANCE);
        });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    public void testFullScreenStage(StageStyle stageStyle) throws InterruptedException {
        setupStages(false, true, stageStyle);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

            topStage.setFullScreen(true);
        });

        // wait a bit to let window system animate the change
        sleep(WAIT);

        runAndWait(() -> {
            assertTrue(topStage.isFullScreen());

            // fullscreen stage should take over the bottom stage
            Color color = getColor(200, 200);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });

        // wait a little bit between getColor() calls - on macOS the below one
        // would fail without this wait
        sleep(SHORT_WAIT);

        runAndWait(() -> {
            // top left corner (plus some tolerance) should NOT show decorations
            Color color = getColor((int)topStage.getX() + 5, (int)topStage.getY() + 5);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    public void testIconifiedStageBeforeShow(StageStyle stageStyle) throws InterruptedException {
        setupStages(true, false, stageStyle);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            // top stage was not shown yet in this case, but the bottom stage should be ready
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

            topStage.setIconified(true);
            topStage.show();
        });

        // wait a bit to let window system animate the change
        sleep(WAIT);

        runAndWait(() -> {
            assertTrue(topStage.isIconified());

            // bottom stage should still be visible
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);
        });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    public void testMaximizedStageBeforeShow(StageStyle stageStyle) throws InterruptedException {
        setupStages(false, false, stageStyle);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

            topStage.setMaximized(true);
            topStage.show();
        });

        // wait a bit to let window system animate the change
        sleep(WAIT);

        runAndWait(() -> {
            assertTrue(topStage.isMaximized());

            // maximized stage should take over the bottom stage
            Color color = getColor(200, 200);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });


        // Do not test decorations for UNDECORATED
        if (stageStyle.equals(StageStyle.UNDECORATED)) {
            return;
        }

        // wait a little bit between getColor() calls - on macOS the below one
        // would fail without this wait
        sleep(SHORT_WAIT);

        runAndWait(() -> {
            // top left corner (plus some tolerance) should show decorations (so not TOP_COLOR)
            Color color = getColor((int)topStage.getX() + 10, (int)topStage.getY() + 10);
            assertColorDoesNotEqual(TOP_COLOR, color, TOLERANCE);
            assertColorDoesNotEqual(BOTTOM_COLOR, color, TOLERANCE);
        });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    public void testFullScreenStageBeforeShow(StageStyle stageStyle) throws InterruptedException {
        setupStages(false, false, stageStyle);

        runAndWait(() -> {
            Color color = getColor(200, 200);
            assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

            topStage.setFullScreen(true);
            topStage.show();
        });

        // wait a bit to let window system animate the change
        sleep(WAIT);

        runAndWait(() -> {
            assertTrue(topStage.isFullScreen());

            // fullscreen stage should take over the bottom stage
            Color color = getColor(200, 200);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });

        // wait a little bit between getColor() calls - on macOS the below one
        // would fail without this wait
        sleep(SHORT_WAIT);

        runAndWait(() -> {
            // top left corner (plus some tolerance) should NOT show decorations
            Color color = getColor((int)topStage.getX() + 5, (int)topStage.getY() + 5);
            assertColorEquals(TOP_COLOR, color, TOLERANCE);
        });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    void testStageStatePrecedenceOrderIconifiedMaximizedBeforeShow(StageStyle stageStyle) throws InterruptedException {
        setupStages(false, false, stageStyle);

        Util.doTimeLine(WAIT,
                () -> {
                    Color color = getColor(200, 200);
                    assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

                    topStage.setIconified(true);
                    topStage.setMaximized(true);
                    topStage.show();
                },
                () -> {
                    assertTrue(topStage.isIconified());
                    assertTrue(topStage.isMaximized());

                    Color color = getColor(200, 200);
                    // Should remain iconified
                    assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);
                },
                () -> topStage.setIconified(false),
                () -> {
                    assertTrue(topStage.isMaximized());
                    assertFalse(topStage.isIconified());

                    Color color = getColor(200, 200);
                    assertColorEquals(TOP_COLOR, color, TOLERANCE);
                });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    void testStageStatePrecedenceOrderIconifiedFullScreenBeforeShow(StageStyle stageStyle) throws InterruptedException {
        setupStages(false, false, stageStyle);

        Util.doTimeLine(WAIT,
                () -> {
                    Color color = getColor(200, 200);
                    assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);

                    topStage.setIconified(true);
                    topStage.setFullScreen(true);
                    topStage.show();
                },
                () -> {
                    assertTrue(topStage.isIconified());
                    assertTrue(topStage.isFullScreen());

                    Color color = getColor(200, 200);
                    // Should remain iconified
                    assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);
                },
                () -> topStage.setIconified(false),
                () -> {
                    assertTrue(topStage.isFullScreen());
                    assertFalse(topStage.isIconified());

                    Color color = getColor(200, 200);
                    assertColorEquals(TOP_COLOR, color, TOLERANCE);
                });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    void testStageStatePrecedenceOrderIconifiedMaximizedAfterShow(StageStyle stageStyle) throws InterruptedException {
        setupStages(true, true, stageStyle);

        Util.doTimeLine(WAIT,
                () -> {
                    Color color = getColor(200, 200);
                    assertColorEquals(TOP_COLOR, color, TOLERANCE);

                    topStage.setIconified(true);
                    topStage.setMaximized(true);
                },
                () -> {
                    assertTrue(topStage.isMaximized());
                    assertTrue(topStage.isIconified());

                    Color color = getColor(200, 200);
                    // Should remain iconified
                    assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);
                },
                () -> topStage.setIconified(false),
                () -> {
                    assertTrue(topStage.isMaximized());
                    assertFalse(topStage.isIconified());

                    Color color = getColor(200, 200);
                    assertColorEquals(TOP_COLOR, color, TOLERANCE);
                });
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED"})
    void testStageStatePrecedenceOrderIconifiedFullScreenAfterShow(StageStyle stageStyle) throws InterruptedException {
        setupStages(true, true, stageStyle);

        Util.doTimeLine(WAIT,
                () -> {
                    Color color = getColor(200, 200);
                    assertColorEquals(TOP_COLOR, color, TOLERANCE);

                    topStage.setIconified(true);
                    topStage.setFullScreen(true);
                },
                () -> {
                    assertTrue(topStage.isFullScreen());
                    assertTrue(topStage.isIconified());

                    Color color = getColor(200, 200);
                    // Should remain iconified
                    assertColorEquals(BOTTOM_COLOR, color, TOLERANCE);
                },
                () -> topStage.setIconified(false),
                () -> {
                    assertTrue(topStage.isFullScreen());
                    assertFalse(topStage.isIconified());

                    Color color = getColor(200, 200);
                    assertColorEquals(TOP_COLOR, color, TOLERANCE);
                });
    }
}