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

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.util.Util;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static test.util.Util.PARAMETERIZED_TEST_DISPLAY;
import static test.util.Util.waitForBoolean;

class FullScreenTest extends StageTestBase {
    private static final int POS_X = 100;
    private static final int POS_Y = 150;
    private static final int WIDTH = 200;
    private static final int HEIGHT = 250;

    private static final Consumer<Stage> TEST_SETTINGS = s -> {
        s.setWidth(WIDTH);
        s.setHeight(HEIGHT);
        s.setX(POS_X);
        s.setY(POS_Y);
    };

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT"})
    void fullScreenShouldKeepGeometryOnRestore(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, TEST_SETTINGS);

        setFullScreen(true);
        setFullScreen(false);
        Util.waitForIdle(getScene());

        assertSizePosition();
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT"})
    void fullScreenBeforeShowShouldKeepGeometryOnRestore(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, TEST_SETTINGS.andThen(s -> s.setFullScreen(true)));

        waitForBoolean(getStage().fullScreenProperty(), true, "stage to enter full screen");
        setFullScreen(false);
        Util.waitForIdle(getScene());
        assertSizePosition();
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT"})
    void fullScreenShouldFillScreen(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, TEST_SETTINGS);

        setFullScreen(true);
        Util.waitForIdle(getScene());
        assertFullScreenFillsScreen();
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT"})
    void fullScreenRestoreCycle(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, TEST_SETTINGS);

        setFullScreen(true);
        Util.waitForIdle(getScene());
        setFullScreen(false);
        Util.waitForIdle(getScene());
        assertSizePosition();

        setFullScreen(true);
        Util.waitForIdle(getScene());
        setFullScreen(false);
        Util.waitForIdle(getScene());
        assertSizePosition();
    }

    /**
     * Sets the full screen state and waits for the property to reach the expected value.
     */
    private void setFullScreen(boolean value) {
        Util.runAndWait(() -> getStage().setFullScreen(value));
        waitForBoolean(getStage().fullScreenProperty(), value,
                "stage to " + (value ? "enter" : "exit") + " full screen");
    }

    /**
     * Waits until the stage geometry returns to the expected size and position
     * by listening for property changes on the stage's geometry properties.
     */
    private void assertSizePosition() {
        Util.waitForIdle(getScene());
        assertEquals(WIDTH, getStage().getWidth(), SIZING_DELTA, "Stage's width should have remained");
        assertEquals(HEIGHT, getStage().getHeight(), SIZING_DELTA, "Stage's height should have remained");
        assertEquals(POS_X, getStage().getX(), POSITION_DELTA, "Stage's X position should have remained");
        assertEquals(POS_Y, getStage().getY(), POSITION_DELTA, "Stage's Y position should have remained");
    }

    private void assertFullScreenFillsScreen() {
        Util.waitForIdle(getScene());
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        assertEquals(screenBounds.getWidth(), getStage().getWidth(), SIZING_DELTA,
                "Full screen width should match screen width");
        assertEquals(screenBounds.getHeight(), getStage().getHeight(), SIZING_DELTA,
                "Full screen height should match screen height");
    }
}
