/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;

import com.sun.javafx.menu.MenuBase;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.stage.Stage;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
        stage.setFocused(true);
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

    public static final class MenuBarSkinMock extends MenuBarSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public MenuBarSkinMock(MenuBar menubar) {
            super(menubar);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }

    private List<MenuBase> getSystemMenus() {
        return ((StubToolkit.StubSystemMenu)tk.getSystemMenu()).getMenus();
    }

}
