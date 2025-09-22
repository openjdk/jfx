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

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.util.Util;
import com.sun.javafx.PlatformUtil;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class HideIconifiedTest {

    private static final int STAGE_WIDTH = 400;
    private static final int STAGE_HEIGHT = 350;
    private static final int STAGE_X = 150;
    private static final int STAGE_Y = 200;

    static CountDownLatch startupLatch = new CountDownLatch(1);
    static volatile Stage stage;

    @BeforeAll
    static void initFX() throws Exception {
        Platform.setImplicitExit(false);
        Util.startup(startupLatch, startupLatch::countDown);
    }

    @AfterAll
    static void teardown() {
        Util.shutdown();
    }

    @AfterEach
    void afterEach() {
        Util.runAndWait(() -> {
            if (stage != null) {
                stage.hide();
                stage = null;
            }
        });
    }

    private void assertStageProperties() {
        assertEquals(STAGE_WIDTH, stage.getWidth(), 0.6, "Stage width changed");
        assertEquals(STAGE_HEIGHT, stage.getHeight(), 0.6, "Stage height changed");
        assertEquals(STAGE_X, stage.getX(), 0.6, "Stage x position changed");
        assertEquals(STAGE_Y, stage.getY(), 0.6, "Stage y position changed");
    }

    private void createAndShowStage(StageStyle stageStyle) {
        Util.runAndWait(() -> {
            stage = new Stage();
            stage.initStyle(stageStyle);
            stage.setWidth(STAGE_WIDTH);
            stage.setHeight(STAGE_HEIGHT);
            stage.setX(STAGE_X);
            stage.setY(STAGE_Y);
            stage.show();
            stage.setIconified(true);
            stage.hide();
        });

        // We're not waiting for the stage to be iconified since that's
        // a synchronous operation. But on some systems OS notifications
        // arrive later so we wait for them.
        Util.sleep(100);
        assertStageProperties();
    }

    @ParameterizedTest
    @EnumSource(names = { "DECORATED", "UNDECORATED", "TRANSPARENT", "EXTENDED" })
    void hideWhileIconifiedThenShow(StageStyle stageStyle) {
        // Disable on Linux until JDK-8354943 is fixed
        assumeFalse(PlatformUtil.isLinux());
        createAndShowStage(stageStyle);

        Util.runAndWait(() -> {
            stage.show();
            stage.setIconified(false);
        });
        Util.sleep(100);
        assertStageProperties();
    }
}
