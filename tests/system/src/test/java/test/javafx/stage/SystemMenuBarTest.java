/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

        System.err.println("FAILED IS: " + failed.get());
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
}
