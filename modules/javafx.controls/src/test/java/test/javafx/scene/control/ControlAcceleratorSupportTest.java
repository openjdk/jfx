/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.value.ChangeListener;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import test.com.sun.javafx.binding.ExpressionHelperUtility;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.util.memory.JMemoryBuddy;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ControlAcceleratorSupportTest {

    @Test
    public void testNumberOfListenersByRemovingAndAddingMenuItems() {

        Menu menu1 = new Menu("1");
        MenuItem item11 = new MenuItem("Item 1");
        MenuItem item12 = new MenuItem("Item 2");
        menu1.getItems().addAll(item11, item12);

        Menu menu2 = new Menu("2");
        MenuItem item21 = new MenuItem("Item 1");
        MenuItem item22 = new MenuItem("Item 2");
        menu2.getItems().addAll(item21, item22);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(menu1, menu2);
        BorderPane pane = new BorderPane();
        pane.setTop(menuBar);

        StageLoader sl = new StageLoader(pane);

        assertEquals(1, getListenerCount(item11.acceleratorProperty()));
        assertEquals(1, getListenerCount(item12.acceleratorProperty()));
        assertEquals(1, getListenerCount(item21.acceleratorProperty()));
        assertEquals(1, getListenerCount(item22.acceleratorProperty()));

        menu1.getItems().clear();
        assertEquals(0, getListenerCount(item11.acceleratorProperty()));
        assertEquals(0, getListenerCount(item12.acceleratorProperty()));
        assertEquals(1, getListenerCount(item21.acceleratorProperty()));
        assertEquals(1, getListenerCount(item22.acceleratorProperty()));

        menu2.getItems().clear();
        assertEquals(0, getListenerCount(item11.acceleratorProperty()));
        assertEquals(0, getListenerCount(item12.acceleratorProperty()));
        assertEquals(0, getListenerCount(item21.acceleratorProperty()));
        assertEquals(0, getListenerCount(item22.acceleratorProperty()));

        menu1.getItems().addAll(item11, item12);
        assertEquals(1, getListenerCount(item11.acceleratorProperty()));
        assertEquals(1, getListenerCount(item12.acceleratorProperty()));
        assertEquals(0, getListenerCount(item21.acceleratorProperty()));
        assertEquals(0, getListenerCount(item22.acceleratorProperty()));

        menu2.getItems().addAll(item21, item22);
        assertEquals(1, getListenerCount(item11.acceleratorProperty()));
        assertEquals(1, getListenerCount(item12.acceleratorProperty()));
        assertEquals(1, getListenerCount(item21.acceleratorProperty()));
        assertEquals(1, getListenerCount(item22.acceleratorProperty()));

        menu2.getItems().clear();
        menu1.getItems().clear();

        assertEquals(0, getListenerCount(item11.acceleratorProperty()));
        assertEquals(0, getListenerCount(item12.acceleratorProperty()));
        assertEquals(0, getListenerCount(item21.acceleratorProperty()));
        assertEquals(0, getListenerCount(item22.acceleratorProperty()));

        sl.dispose();
    }

    @Test
    public void testMemoryLeak_JDK_8274022() {
        JMemoryBuddy.memoryTest(checker -> {
            MenuItem menuItem = new MenuItem("LeakingItem");
            MenuBar menuBar = new MenuBar(new Menu("MENU_BAR", null, menuItem));
            StageLoader sl = new StageLoader(new StackPane(menuBar));
            sl.getStage().close();

            // Set listener to something on the scene, to make sure the listener references the whole scene.
            menuItem.setOnAction((e) -> { menuItem.fire();});

            checker.assertCollectable(menuItem);
        });
    }

    @Test
    public void testMemoryButtonSkinDoesntAddAdditionalListeners() {
        // JDK-8296409
        MenuItem menuItem = new MenuItem("Menu Item");
        MenuButton menuButton = new MenuButton("Menu Button", null, menuItem);
        StackPane root = new StackPane(menuButton);
        StageLoader sl = new StageLoader(root);
        assertEquals(1, getListenerCount(menuItem.acceleratorProperty()));
        root.getChildren().remove(menuButton);
        assertEquals(0, getListenerCount(menuItem.acceleratorProperty()));
        root.getChildren().add(menuButton);
        assertEquals(1, getListenerCount(menuItem.acceleratorProperty()));
        sl.dispose();
    }

    @Test
    public void testMemoryButtonSkinDoesntAddAdditionalListenersOnSceneChange() {
        // JDK-8296409
        MenuItem menuItem = new MenuItem("Menu Item");
        MenuButton menuButton = new MenuButton("Menu Button", null, menuItem);
        StackPane root = new StackPane(menuButton);
        StackPane root2 = new StackPane();
        StageLoader sl1 = new StageLoader(root);
        StageLoader sl2 = new StageLoader(root2);
        assertEquals(1, getListenerCount(menuItem.acceleratorProperty()));
        ChangeListener originalChangeListener =
                ExpressionHelperUtility.getChangeListeners(menuItem.acceleratorProperty()).get(0);
        root2.getChildren().add(menuButton);
        assertEquals(1, getListenerCount(menuItem.acceleratorProperty()));
        ChangeListener secondChangeListener =
                ExpressionHelperUtility.getChangeListeners(menuItem.acceleratorProperty()).get(0);
        assertNotEquals(originalChangeListener, secondChangeListener);
        root.getChildren().add(menuButton);
        assertEquals(1, getListenerCount(menuItem.acceleratorProperty()));
        ChangeListener thirdChangeListener =
                ExpressionHelperUtility.getChangeListeners(menuItem.acceleratorProperty()).get(0);
        assertNotEquals(secondChangeListener,thirdChangeListener);
        sl1.dispose();
        sl2.dispose();
    }
}
