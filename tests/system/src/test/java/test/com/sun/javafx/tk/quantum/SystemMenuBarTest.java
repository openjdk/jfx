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

package test.com.sun.javafx.tk.quantum;

import com.sun.javafx.menu.MenuBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;
import test.util.memory.JMemoryBuddy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.javafx.tk.quantum.GlassSystemMenuShim;
import com.sun.javafx.scene.control.GlobalMenuAdapter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
    CountDownLatch memoryFocusLatch = new CountDownLatch(1);
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

    @Test
    public void testFocusMemoryLeak() throws InterruptedException {
        Util.runAndWait(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t,e) -> {
                e.printStackTrace();
                failed.set(true);
                memoryFocusLatch.countDown();
            });
            createAndRefocusMenuBarStage();
        });
        memoryFocusLatch.await();
        assertFalse(failed.get());
    }

    public void createAndRefocusMenuBarStage() {
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
        final ArrayList<WeakReference<MenuBase>> uncollectedMenus = new ArrayList<>();
        GlassSystemMenuShim gsmh = new GlassSystemMenuShim();
        assumeTrue("SystemMenu only supported on MacOS", gsmh.isSupported());
        Menu m1 = new Menu("Menu");

        MenuBase menuBase = GlobalMenuAdapter.adapt(m1);
        ArrayList<MenuBase> menus = new ArrayList<>();
        gsmh.createMenuBar();
        for (int i = 0; i < 100; i++) {
            Platform.runLater(() -> {
                gsmh.setMenus(List.of(menuBase));
            });
        }
        Platform.runLater(() -> {
            int strongCount = 0;
            final List<WeakReference<com.sun.glass.ui.Menu>> u2 = gsmh.getWeakMenuReferences();
            for (WeakReference<com.sun.glass.ui.Menu> wr : u2) {
                if (!JMemoryBuddy.checkCollectable(wr)) {
                    strongCount++;
                    assertTrue("Too many references", strongCount < 2);
                }
            }
            assertEquals(1, strongCount, "Exactly one reference should be reachable");
            memoryFocusLatch.countDown();
        });
    }

    CountDownLatch removeMenuLatch = new CountDownLatch(1);

    @Test
    public void testRemoveMenu() throws InterruptedException {
        failed.set(false);
        Util.runAndWait(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t,e) -> {
                e.printStackTrace();
                failed.set(true);
                removeMenuLatch.countDown();
            });
            createRemoveMenuStage();
        });
        removeMenuLatch.await();
        assertFalse(failed.get());
    }

    public void createRemoveMenuStage() {
        Stage stage = new Stage();
        VBox root = new VBox();

        final MenuBar menuBar = new MenuBar();

        Menu mainMenu = new Menu("MainMenu");
        Menu subMenu = new Menu("SubMenu");
        subMenu.getItems().addAll(new MenuItem("submenuitem1"), new MenuItem("submenuitem2"));
        mainMenu.getItems().add(subMenu);
        menuBar.getMenus().add(mainMenu);

        menuBar.setUseSystemMenuBar(true);
        root.getChildren().add(menuBar);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> {
            mainMenu.getItems().clear();
            mainMenu.getItems().add(subMenu);
            subMenu.getItems().addAll(new MenuItem("new item 1"), new MenuItem("new item 2"));
            removeMenuLatch.countDown();
        });
    }

    @Test // adding/removing/changing items should not throw an Exception
    public void testJDK8309935() throws InterruptedException {
        MenuBar menuBar = new MenuBar();
        AtomicReference<Throwable> throwableRef = new AtomicReference<>();
        Util.runAndWait(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
                e.printStackTrace();
                throwableRef.set(e);
            });
            menuBar.setUseSystemMenuBar(true);
            Menu menu1 = new Menu("menu 1");
            menu1.getItems().add(new MenuItem("item 1"));
            menu1.getItems().add(new MenuItem("item 2"));
            menuBar.getMenus().add(menu1);
            Menu menu2 = new Menu(" menu 2");
            menu2.getItems().add(new MenuItem("item 1"));
            menu2.getItems().add(new MenuItem("item 2"));
            menu2.getItems().add(new SeparatorMenuItem());
            menuBar.getMenus().add(menu2);
            Menu test1 = new Menu("test 1");
            test1.getItems().add(new MenuItem("item 1"));
            test1.getItems().add(new MenuItem("item 2"));
            Menu test2 = new Menu("test 2");
            test2.getItems().add(new MenuItem("item 1"));
            test2.getItems().add(new MenuItem("item 2"));
            menu2.addEventFilter(Menu.ON_SHOWING, e -> {
                menu2.getItems().removeIf(o -> Objects.equals(o.getText(), test1.getText()));
                menu2.getItems().add(test1);
                menu2.getItems().removeIf(o -> Objects.equals(o.getText(), test2.getText()));
                menu2.getItems().add(test2);
            });
            BorderPane root = new BorderPane();
            root.setTop(menuBar);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
        });
        Util.runAndWait(() -> {
            menuBar.getMenus().forEach(menu -> {
                menu.setVisible(false);
            });
        });
        Util.runAndWait(() -> {
            menuBar.getMenus().forEach(menu -> {
                menu.setVisible(true);
            });
        });
        CountDownLatch cdl = new CountDownLatch(1);
        Util.runAndWait(() -> {
            Menu test3 = new Menu("test 3");
            test3.getItems().add(new MenuItem("item 1"));
            test3.getItems().add(new MenuItem("item 2"));
            Menu test4 = new Menu("test 4");
            test4.getItems().add(new MenuItem("item 1"));
            test4.getItems().add(new MenuItem("item 2"));
            menuBar.getMenus().get(1).getItems().addAll(test3, test4);
            Platform.runLater(() -> cdl.countDown());
        });
        boolean success = cdl.await(10, TimeUnit.SECONDS);
        assertTrue(success);
        if (throwableRef.get() != null) {
            fail(throwableRef.get());
        }
    }
}
