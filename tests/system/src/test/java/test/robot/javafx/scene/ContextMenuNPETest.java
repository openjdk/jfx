/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene;

import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.util.Util;

/*
 * Test for verifying context menu NPE error
 *
 * There is 1 test in this file.
 * Steps for testContextMenuNPE()
 * 1. Create a menu button.
 * 2. Load a custom skin which extends ContextMenuSkin class.
 * 3. Add menu items and sub menu.
 * 4. Click on MenuButton to open context menu.
 * 5. Got to the first item and select first item of submenu.
 * 6. Repeat step 4 and 5 and check if NPE is thrown on second attempt.
 */

public class ContextMenuNPETest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;
    static MenuButton menuButton;
    static Parent fxmlContent;

    static volatile Throwable exception;
    static volatile Stage stage;
    static volatile Scene scene;

    CountDownLatch onShownLatch;
    CountDownLatch onHiddenLatch;

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                    (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    private void showMenuButtonContextMenu() throws Exception {
        onShownLatch = new CountDownLatch(1);
        mouseClick(menuButton.getLayoutX() + menuButton.getWidth() / 2,
                    menuButton.getLayoutY() + menuButton.getHeight() / 2);
        Thread.sleep(200); // Small delay to wait for context menu to display.
        Util.waitForLatch(onShownLatch, 10, "Failed to show context menu.");
    }

    private void selectSubmenuItem() throws Exception {
        onHiddenLatch = new CountDownLatch(1);
        Util.runAndWait(() -> {
            robot.keyType(KeyCode.DOWN);
            robot.keyType(KeyCode.RIGHT);
            robot.keyType(KeyCode.ENTER);
        });
        Util.waitForIdle(scene);
        Util.waitForLatch(onHiddenLatch, 10, "Failed to hide context menu.");
    }

    @Test
    public void testContextMenuNPE() throws Throwable {

        showMenuButtonContextMenu();
        selectSubmenuItem();

        showMenuButtonContextMenu();
        selectSubmenuItem();
        if (exception != null) {
            exception.printStackTrace();
            throw exception;
        }

        Assertions.assertEquals(0, onHiddenLatch.getCount());
    }

    @AfterEach
    public void resetUI() {
        Platform.runLater(() -> {
            menuButton.setOnShown(null);
            menuButton.setOnHidden(null);
        });
    }

    @BeforeEach
    public void setupUI() {
        Platform.runLater(() -> {
            menuButton.setOnShown(e -> {
                onShownLatch.countDown();
            });
            menuButton.setOnHidden(e -> {
                onHiddenLatch.countDown();
            });
        });
    }

    @BeforeAll
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown();
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            try {
                fxmlContent = FXMLLoader.load(getClass().getResource("ContextMenuNPEDemo.fxml"));
                scene = new Scene(fxmlContent);
                stage = primaryStage;
                stage.setScene(scene);
                menuButton = (MenuButton) scene.lookup("#idMenuButton");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                                    Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();

            Thread.currentThread().setUncaughtExceptionHandler((t2, e) -> {
                exception = e;
            });
        }
    }
}
