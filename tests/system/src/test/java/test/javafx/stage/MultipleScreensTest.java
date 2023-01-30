/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class MultipleScreensTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static ObservableList<Screen> screens;
    static Screen primaryScreen;
    static Screen otherScreen;

    Stage stage;

    @BeforeClass
    public static void initFX() throws Exception {
        Platform.setImplicitExit(false);
        Util.startup(startupLatch, startupLatch::countDown);

        // Get primary screen and list of all screens, skip tests if there is only one
        primaryScreen = Screen.getPrimary();
        assertNotNull("Primary screen is null", primaryScreen);
        screens = Screen.getScreens();
        assertNotNull("List of screens is null", screens);
        assumeTrue(screens.size() > 1);

        // Get a screen other than the primary screen
        otherScreen = screens.stream()
                .filter(s -> !primaryScreen.equals(s))
                .findFirst()
                .orElseThrow();
        assertNotNull("Secondary screen is null", otherScreen);
    }

    @AfterClass
    public static void exitFX() {
        Util.shutdown();
    }

    @Before
    public void initTest() {
        Util.runAndWait(() -> stage = new Stage());
    }

    @After
    public void cleanupTest() {
        if (stage != null) {
            Platform.runLater(stage::hide);
        }
    }

    /**
     * Test all four combinations of [primary,secondary]Screen x [with/without]Scene
     */
    private void createAndShowStage(Screen screen, boolean hasScene) throws Exception {
        assertNotNull("Stage is null", stage);

        final CountDownLatch shownLatch = new CountDownLatch(1);

        Util.runAndWait(() -> {
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> shownLatch.countDown());

            Rectangle2D bounds = screen.getBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());

            if (hasScene) {
                stage.setScene(new Scene(new Group()));
            }

            stage.show();
        });

        Util.waitForLatch(shownLatch, 5, "Stage failed to show");
    }

    @Test(timeout = 15000)
    public void showStageNoScenePrimaryScreen() throws Exception {
        createAndShowStage(primaryScreen, false);
    }

    @Test(timeout = 15000)
    public void showStageNoSceneOtherScreen() throws Exception {
        createAndShowStage(otherScreen, false);
    }

    @Test(timeout = 15000)
    public void showStageScenePrimaryScreen() throws Exception {
        createAndShowStage(primaryScreen, true);
    }

    @Test(timeout = 15000)
    public void showStageSceneOtherScreen() throws Exception {
        createAndShowStage(otherScreen, true);
    }

}
