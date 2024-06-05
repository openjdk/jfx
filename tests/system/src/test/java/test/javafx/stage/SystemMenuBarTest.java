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

package test.javafx.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;
import test.util.memory.JMemoryBuddy;

public class SystemMenuBarTest {
    @BeforeClass
    public static void initFX() throws Exception {
        CountDownLatch startupLatch = new CountDownLatch(1);
        Platform.setImplicitExit(false);

        Util.startup(startupLatch, () -> {
            startupLatch.countDown();
        });
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    CountDownLatch menubarLatch = new CountDownLatch(1);
    CountDownLatch memoryLatch = new CountDownLatch(1);
    AtomicBoolean failed = new AtomicBoolean(false);

    @Test
    public void testFailingMenuBar() throws InterruptedException {
        Util.runAndWait(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t,e) -> {
                e.printStackTrace();
                failed.set(true);
            });
            createMenuBarStage();
        });

        menubarLatch.await();

        assertFalse(failed.get());
    }

    public void createMenuBarStage() {
        Stage stage = new Stage();
        VBox root = new VBox();

        root.getChildren().add(createFailingMenuBar());

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public MenuBar createFailingMenuBar() {
        MenuBar menuBar = new MenuBar();

        menuBar.setUseSystemMenuBar(true);

        Menu systemMenu = new Menu("systemMenu");
        menuBar.getMenus().add(systemMenu);

        var newItem = new MenuItem();
        newItem.setVisible(false);
        systemMenu.getItems().add(newItem);

        Platform.runLater(() -> {
            javafx.scene.control.Menu systemMenuContributions = new Menu("123");
            systemMenu.getItems().add(systemMenuContributions);
            menubarLatch.countDown();
        });

        return menuBar;
    }

    @Test
    public void testMemoryLeak() throws InterruptedException {
        Util.runAndWait(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t,e) -> {
                e.printStackTrace();
                failed.set(true);
                memoryLatch.countDown();
            });
            createMenuBarWithItemsStage();
        });
        memoryLatch.await();
        assertFalse(failed.get());
    }

    private void createMenuBarWithItemsStage() {
        final ArrayList<WeakReference<MenuItem>> uncollectedMenuItems = new ArrayList<>();

        Stage stage = new Stage();
        VBox root = new VBox();
        final MenuBar menuBar = new MenuBar();
        final Menu menu = new Menu("MyMenu");
        menuBar.getMenus().add(menu);
        menuBar.setUseSystemMenuBar(true);
        root.getChildren().add(menuBar);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
        stage.requestFocus();
        Thread t = new Thread() {
            @Override public void run() {
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(20);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        menu.getItems().clear();
                        MenuItem menuItem = new MenuItem("MyItem");
                        WeakReference<MenuItem> wr = new WeakReference<>(menuItem);
                        uncollectedMenuItems.add(wr);
                        menu.getItems().add(menuItem);
                    });
                }
                Platform.runLater( () -> {
                    int strongCount = 0;
                    for (WeakReference<MenuItem> wr: uncollectedMenuItems) {
                        if (!JMemoryBuddy.checkCollectable(wr)) strongCount++;
                    }
                    assertEquals(1, strongCount, "Only the last menuItem should be alive");
                    memoryLatch.countDown();
                });
            }
        };
        t.start();
    }

}
