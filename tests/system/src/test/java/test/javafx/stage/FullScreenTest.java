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

import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.util.Util;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.util.Util.PARAMETERIZED_TEST_DISPLAY;

class FullScreenTest extends StageTestBase {
    private static final int POS_X = 100;
    private static final int POS_Y = 150;
    private static final int WIDTH = 100;
    private static final int HEIGHT = 150;

    private static final Consumer<Stage> TEST_SETTINGS = s -> {
        s.setWidth(WIDTH);
        s.setHeight(HEIGHT);
        s.setX(POS_X);
        s.setY(POS_Y);
    };

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "TRANSPARENT"})
    void testFullScreenShouldKeepGeometryOnRestore(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, TEST_SETTINGS);

        Util.doTimeLine(LONG_WAIT,
                () -> getStage().setFullScreen(true),
                () -> assertTrue(getStage().isFullScreen()),
                () -> getStage().setFullScreen(false));

        Util.sleep(LONG_WAIT);
        assertSizePosition();
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "TRANSPARENT"})
    void testFullScreenBeforeShowShouldKeepGeometryOnRestore(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, TEST_SETTINGS.andThen(s -> s.setFullScreen(true)));

        Util.sleep(LONG_WAIT);
        Util.runAndWait(() -> {
            assertTrue(getStage().isFullScreen());
            getStage().setFullScreen(false);
        });

        Util.sleep(LONG_WAIT);
        assertSizePosition();
    }

    private void assertSizePosition() {
        assertEquals(WIDTH, getStage().getWidth(), SIZING_DELTA, "Stage's width should have remained");
        assertEquals(HEIGHT, getStage().getHeight(), SIZING_DELTA, "Stage's height should have remained");
        assertEquals(POS_X, getStage().getX(), POSITION_DELTA,  "Stage's X position should have remained");
        assertEquals(POS_Y, getStage().getY(), POSITION_DELTA, "Stage's Y position should have remained");
    }
}
