/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javafx.menu.MenuBase;
import com.sun.javafx.stage.WindowHelper;
import com.sun.javafx.tk.Toolkit;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.MenuBarSkin;
import javafx.scene.control.skin.MenuBarSkinShim;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;

/**
 * This fails with IllegalStateException because of the toolkit's check for the FX application thread
 */
public class MenuBarSkinTest {
    private MenuBar menubar;
    private MenuBarSkinMock skin;
    private static Toolkit tk;
    private Scene scene;
    private Stage stage;


    @BeforeClass public static void initToolKit() {
        tk = Toolkit.getToolkit();
    }

    @Before public void setup() {
        menubar = new MenuBar();
        menubar.setUseSystemMenuBar(false);
        menubar.getMenus().addAll(new Menu("File"), new Menu("Edit"));

        // Pending RT-37118, MenuBar needs to be in a scene in order to set the skin.
        scene = new Scene(new Group(menubar));
        skin = new MenuBarSkinMock(menubar);
        menubar.setSkin(skin);

        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        menubar.setPadding(new Insets(10, 10, 10, 10));

        stage = new Stage();

        // MenuBar needs to have a stage in order for system menus to work
        stage.setScene(scene);

        // Stage has to be focused in order for system menus to work
        WindowHelper.setFocused(stage, true);
    }

    @Test public void maxHeightTracksPreferred() {
        menubar.setPrefHeight(100);
        assertEquals(100, menubar.maxHeight(-1), 0);
    }

    @Test public void testDispose() {

        if (tk.getSystemMenu().isSupported()) {
            // setting system menu bar true should create a sceneProperty listener for RT-36554
            menubar.setUseSystemMenuBar(true);
            assertEquals(menubar.getMenus().size(), getSystemMenus().size());
        }

        // This will cause the dispose method to be called.
        menubar.setSkin(null);

        if (tk.getSystemMenu().isSupported()) {

            // dispose should clean up the system menu.
            assertEquals(0, getSystemMenus().size());

        }

    }

    @Test public void testSetUseSystemMenuBar() {
        if (tk.getSystemMenu().isSupported()) {
            menubar.setUseSystemMenuBar(true);
            assertEquals(menubar.getMenus().size(), getSystemMenus().size());

            menubar.setUseSystemMenuBar(false);
            assertEquals(0, getSystemMenus().size());

            menubar.setUseSystemMenuBar(true);
            assertEquals(menubar.getMenus().size(), getSystemMenus().size());
        }
    }

    @Test public void testSystemMenuBarUpdatesWhenMenusChange() {

        if (tk.getSystemMenu().isSupported()) {
            menubar.setUseSystemMenuBar(true);
            assertEquals(menubar.getMenus().size(), getSystemMenus().size());

            menubar.getMenus().add(new Menu("testSystemMenuBarUpdatesWhenMenusChange"));
            assertEquals(menubar.getMenus().size(), getSystemMenus().size());
        }
    }

    @Test public void testRT_36554() {

        if (tk.getSystemMenu().isSupported()) {

            menubar.setUseSystemMenuBar(true);
            assertEquals(menubar.getMenus().size(), getSystemMenus().size());

            // removing the menubar from the scene should remove the system menus.
            ((Group)scene.getRoot()).getChildren().remove(menubar);
            assertEquals(0, getSystemMenus().size());

            // adding the menubar from the scene should add back the system menus.
            ((Group)scene.getRoot()).getChildren().add(menubar);
            assertEquals(menubar.getMenus().size(), getSystemMenus().size());

            // remove, then set useSystemMenuBar to false. Upon re-adding,
            // there should still be no system menu.
            ((Group)scene.getRoot()).getChildren().remove(menubar);
            assertEquals(0, getSystemMenus().size());

            menubar.setUseSystemMenuBar(false);
            ((Group)scene.getRoot()).getChildren().add(menubar);
            assertEquals(0, getSystemMenus().size());

            // setting useSystemMenuBar to true again, should add back the system menus.
            menubar.setUseSystemMenuBar(true);
            assertEquals(menubar.getMenus().size(), getSystemMenus().size());
        }
    }

    @Test public void testModifyingNonSystemMenuBar() {
        if (tk.getSystemMenu().isSupported()) {
            // Set system menubar to true
            menubar.setUseSystemMenuBar(true);

            // Create a secondary menubar that is not
            // a system menubar
            MenuBar secondaryMenuBar = new MenuBar(
                    new Menu("Menu 1", null, new MenuItem("Item 1")),
                    new Menu("Menu 2", null, new MenuItem("Item 2")));
            secondaryMenuBar.setSkin(new MenuBarSkin(secondaryMenuBar));

            // Add the secondary menubar to the scene
            ((Group)scene.getRoot()).getChildren().add(secondaryMenuBar);

            // Verify that the menubar is the system menubar
            assertTrue(menubar.isUseSystemMenuBar());

            // Remove a menu from the secondary menubar
            // to trigger a rebuild of its UI and a call
            // to the sceneProperty listener
            secondaryMenuBar.getMenus().remove(1);

            // Verify that this has not affected whether the
            // original menubar is the system menubar
            assertTrue(menubar.isUseSystemMenuBar());
        }
    }

    @Test
    public void testInvisibleMenuNavigation() {
        menubar.getMenus().get(0).setVisible(false);
        MenuBarSkinShim.setFocusedMenuIndex(skin, 0);

        KeyEventFirer keyboard = new KeyEventFirer(menubar);
        keyboard.doKeyPress(KeyCode.LEFT);
        tk.firePulse();
    }

    public static final class MenuBarSkinMock extends MenuBarSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public MenuBarSkinMock(MenuBar menubar) {
            super(menubar);
        }

        public void addWatchedProperty(ObservableValue<?> p) {
            p.addListener(o -> {
                propertyChanged = true;
                propertyChangeCount++;
            });
        }
    }

    private List<MenuBase> getSystemMenus() {
        return ((StubToolkit.StubSystemMenu)tk.getSystemMenu()).getMenus();
    }

}
