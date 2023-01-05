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
package test.javafx.scene.control;

import com.sun.javafx.tk.Toolkit;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.*;
import test.com.sun.javafx.pgstub.StubToolkit;
import test.util.Util;
import test.util.memory.JMemoryBuddy;
import javafx.application.Platform;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class SystemMenuBarTest {

    @BeforeClass
    public static void initFX() throws Exception {
        CountDownLatch startupLatch = new CountDownLatch(1);
        Platform.setImplicitExit(false);
        Util.startup(startupLatch, startupLatch::countDown);
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Test
    public void test_JDK_8299423() {
        /**
         * We want to test, if we add a menu to the system-menu, whether it's callable get's collected after the entry is removed.
         *
         */
        JMemoryBuddy.memoryTest((checker) -> {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                Stage stage = new Stage();
                MenuBar menuBar = new MenuBar();
                VBox root = new VBox(menuBar);
                stage.setScene(new Scene(root));
                menuBar.setUseSystemMenuBar(true);
                Menu menu1 = new Menu("menu-1");
                var item2 = new MenuItem("remove above");
                menu1.getItems().add(item2);
                menuBar.getMenus().add(menu1);
                EventHandler<ActionEvent> listener = new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        System.out.println("I'm the listener, but no one calls me.");
                    }
                };
                item2.setOnAction(listener);
                stage.show();

                checker.assertCollectable(listener);

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        menu1.getItems().clear();
                        latch.countDown();
                    });
                }).start();
            });

            try {
                assertTrue("Timeout waiting for setOnShown", latch.await(15, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

}
