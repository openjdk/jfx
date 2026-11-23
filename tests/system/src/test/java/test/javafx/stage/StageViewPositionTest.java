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

import javafx.stage.StageStyle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static test.util.Util.PARAMETERIZED_TEST_DISPLAY;
import static test.util.Util.STATE_DELAY;
import static test.util.Util.runAndWait;

class StageViewPositionTest extends StageTestBase {

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED"})
    void fullScreenBeforeShowViewPostionTest(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> s.setFullScreen(true));
        Util.sleep(STATE_DELAY);

        assertEquals(0, getScene().getX(), "FullScreen stage should have no view X offset");
        assertEquals(0, getScene().getY(), "FullScreen stage should have no view Y offset");

        Util.runAndWait(() -> getStage().setFullScreen(false));
        Util.sleep(STATE_DELAY);

        assertNotEquals(0, getScene().getY(), "UnFullScreened stage should not have view Y offset");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED"})
    void fullScreenAferShowViewPostionTest(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, null);

        runAndWait(() -> getStage().setFullScreen(true));
        Util.sleep(STATE_DELAY);

        assertEquals(0, getScene().getY(), "FullScreen stage should have no view Y offset");

        Util.runAndWait(() -> getStage().setFullScreen(false));
        Util.sleep(STATE_DELAY);

        assertNotEquals(0, getScene().getY(), "UnFullScreened stage should have view Y offset");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"UNDECORATED", "EXTENDED", "TRANSPARENT"})
    void undecoratedStagesShouldHaveNoViewPositionOffset(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, null);

        assertEquals(0, getScene().getY(), "Stages with no decoration shouldn't have view Y offset");
    }
}
