/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.SceneHelper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.skin.MenuBarSkinShim;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventGenerator;
import com.sun.javafx.scene.control.ContextMenuContent;
import com.sun.javafx.scene.control.MenuBarMenuButtonShim;
import javafx.scene.control.skin.MenuBarSkin;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;


/**
 *
 * @author lubermud
 */

public class MenuBarTest {
    private MenuBar menuBar;
    private Toolkit tk;
    private Scene scene;
    private Stage stage;

    @Before public void setup() {
        setUncaughtExceptionHandler();

        tk = (StubToolkit)Toolkit.getToolkit();
        menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);
    }

    @After public void cleanup() {
        if (stage != null) {
            stage.hide();
        }
        removeUncaughtExceptionHandler();
    }

    private void setUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }

    private void removeUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    protected void startApp(Parent root) {
        scene = new Scene(root,800,600);
        stage = new Stage();
        stage.setX(0); // The MouseEventGenerator set the screen X/Y to be the same as local Node X/Y, so we need to keep stage at 0,0
        stage.setY(0);
        stage.setScene(scene);
        stage.show();
        tk.firePulse();
    }

    @Test public void defaultConstructorHasFalseFocusTraversable() {
        assertFalse(menuBar.isFocusTraversable());
    }

    @Test public void defaultConstructorButSetTrueFocusTraversable() {
        menuBar.setFocusTraversable(true);
        assertTrue(menuBar.isFocusTraversable());
    }

    @Test public void testFocusOnEmptyMenubar() {
        menuBar.setFocusTraversable(true);

        AnchorPane root = new AnchorPane();
        root.getChildren().add(menuBar);
        startApp(root);

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        menuBar.getScene().getWindow().requestFocus();

        int focusedIndex = MenuBarSkinShim.getFocusedMenuIndex(skin);
        assertEquals(-1, focusedIndex);
    }

    @Test public void testSimulateTraverseIntoEmptyMenubar() {
        menuBar.setFocusTraversable(true);

        AnchorPane root = new AnchorPane();
        root.getChildren().add(menuBar);
        startApp(root);

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        // simulate notification from traversalListener
        MenuBarSkinShim.setFocusedMenuIndex(skin, 0);
        int focusedIndex = MenuBarSkinShim.getFocusedMenuIndex(skin);
        assertEquals(-1, focusedIndex);
    }

    @Test public void getMenusHasSizeZero() {
        assertEquals(0, menuBar.getMenus().size());
    }

    @Test public void getMenusIsAddable() {
        menuBar.getMenus().add(new Menu(""));
        assertTrue(menuBar.getMenus().size() > 0);
    }

    @Test public void getMenusIsClearable() {
        menuBar.getMenus().add(new Menu(""));
        menuBar.getMenus().clear();
        assertEquals(0, menuBar.getMenus().size());
    }

     @Test public void getMenusIsRemovable() {
           menuBar.getMenus().add(new Menu("blah"));
           menuBar.getMenus().add(new Menu("foo"));
           menuBar.getMenus().remove(0);
           assertEquals(1, menuBar.getMenus().size());
     }

    @Test public void testMenuShowHideWithMenuBarWithXYTranslation() {
        final MouseEventGenerator generator = new MouseEventGenerator();
        AnchorPane root = new AnchorPane();
        Menu menu = new Menu("Menu");
        menu.getItems().add(new MenuItem("MenuItem"));
        menuBar.getMenus().add(menu);
        menuBar.setLayoutX(100);
        menuBar.setLayoutY(100);
        root.getChildren().add(menuBar);
        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        double xval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinX();
        double yval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinY();

        MenuButton mb = MenuBarSkinShim.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();
        SceneHelper.processMouseEvent(scene,
            generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            generator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        assertTrue(menu.isShowing());

    }

    @Test public void testSubMenuDismissalWithKeyNavigation() {
        final MouseEventGenerator generator = new MouseEventGenerator();
        AnchorPane root = new AnchorPane();
        Menu menu = new Menu("Menu");
        Menu menu1 = new Menu("Menu With SubMenu");
        menu.getItems().add(menu1);

        MenuItem menuItem1 = new MenuItem("MenuItem1");
        MenuItem menuItem2 = new MenuItem("MenuItem2");
        menu1.getItems().addAll(menuItem1, menuItem2);

        menuBar.getMenus().add(menu);
        menuBar.setLayoutX(100);
        menuBar.setLayoutY(100);

        root.getChildren().add(menuBar);
        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        double xval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinX();
        double yval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinY();

        MenuButton mb = MenuBarSkinShim.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();
        SceneHelper.processMouseEvent(scene,
            generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            generator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        assertTrue(menu.isShowing());
         /* ------------------------------------------------------------------ */

        // Show subMenu
        ContextMenuContent menuContent = MenuBarSkinShim.getMenuContent(mb); // ContextMenuContent
        Node displayNode = MenuBarMenuButtonShim.getDisplayNodeForMenuItem(menuContent, 0); // MenuItemContainer

        displayNode.getScene().getWindow().requestFocus();
        assertTrue(displayNode.getScene().getWindow().isFocused());

        displayNode.requestFocus(); // requestFocus on 1st Menu
        assertTrue(displayNode.isFocused());
        // update currentFocusedIndex
        MenuBarMenuButtonShim.setCurrentFocusedIndex(menuContent, 0);

        // fire KeyEvent (Enter) on menu1 to show submenu
        KeyEventFirer keyboard = new KeyEventFirer(menuContent);
        keyboard.doKeyPress(KeyCode.ENTER);
        tk.firePulse();
        assertTrue(menu1.isShowing()); // subMenu is showing
        /* ------------------------------------------------------------------ */

        // Get 1st MenuItem from the submenu
        ContextMenuContent subMenuContent = MenuBarMenuButtonShim.getSubMenuContent(menuContent);
        subMenuContent.getScene().getWindow().requestFocus(); // requestFocus on submenu
        assertTrue(subMenuContent.getScene().getWindow().isFocused());

        displayNode = MenuBarMenuButtonShim.getDisplayNodeForMenuItem(subMenuContent, 0);
        displayNode.requestFocus();
        assertTrue(displayNode.isFocused());

        MenuBarMenuButtonShim.setCurrentFocusedIndex(subMenuContent, 0);
        // fire KeyEvent (Enter) on menuItem1 to hide all menus
        keyboard = new KeyEventFirer(subMenuContent);
        keyboard.doKeyPress(KeyCode.ENTER);
        tk.firePulse();

        // confirm all menus are closed.
        assertTrue(!menu1.isShowing());
        assertTrue(!menu.isShowing());
    }

    @Test public void checkMenuBarMenusSelectionResetAfterMenuItemIsSelected() {
        final MouseEventGenerator generator = new MouseEventGenerator();
        AnchorPane root = new AnchorPane();
        Menu menu = new Menu("Menu");
        MenuItem menuItem = new MenuItem("MenuItem");
        menu.getItems().add(menuItem);

        menuBar.getMenus().add(menu);
        menuBar.setLayoutX(100);
        menuBar.setLayoutY(100);

        root.getChildren().addAll(menuBar);

        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);
        MenuButton mb = MenuBarSkinShim.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();

        double xval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinX();
        double yval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinY();

        SceneHelper.processMouseEvent(scene,
            generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            generator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        assertTrue(menu.isShowing());
         /* ------------------------------------------------------------------ */

        // Show Menu
        ContextMenuContent menuContent = MenuBarSkinShim.getMenuContent(mb); // ContextMenuContent
        Node displayNode = MenuBarMenuButtonShim.getDisplayNodeForMenuItem(menuContent, 0); // MenuItemContainer

        displayNode.getScene().getWindow().requestFocus();
        assertTrue(displayNode.getScene().getWindow().isFocused());

        displayNode.requestFocus(); // requestFocus on 1st Menu
        assertTrue(displayNode.isFocused());

        // fire KeyEvent (Enter) on menuitem
        KeyEventFirer keyboard = new KeyEventFirer(menuContent);
        keyboard.doKeyPress(KeyCode.ENTER);
        tk.firePulse();

        // confirm menu is closed.
        assertTrue(!menu.isShowing());
        keyboard.doKeyPress(KeyCode.LEFT);
        tk.firePulse();

        // check if focusedMenuIndex is reset to -1 so navigation happens.
        int focusedIndex = MenuBarSkinShim.getFocusedMenuIndex(skin);
        assertEquals(-1, focusedIndex);
    }

    @Test public void testMenuOnShownEventFiringWithKeyNavigation() {
        final MouseEventGenerator generator = new MouseEventGenerator();
        VBox root = new VBox();
        Menu menu = new Menu("Menu");

        MenuItem menuItem1 = new MenuItem("MenuItem1");
        MenuItem menuItem2 = new MenuItem("MenuItem2");
        menu.getItems().addAll(menuItem1, menuItem2);

        menuBar.getMenus().add(menu);
        menuBar.setLayoutX(100);
        menuBar.setLayoutY(100);

        final CheckBox cb = new CheckBox("showing");

        root.getChildren().addAll(cb,menuBar);
        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        MenuButton mb = MenuBarSkinShim.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();
        assertTrue(mb.getScene().getWindow().isFocused());


        KeyEventFirer keyboard = new KeyEventFirer(mb.getScene());
        keyboard.doKeyPress(KeyCode.TAB);
        tk.firePulse();
        mb.requestFocus();
        assertTrue(mb.isFocused());

        keyboard = new KeyEventFirer(mb);
        keyboard.doDownArrowPress();
        tk.firePulse();
        assertEquals(menu.showingProperty().get(), true);
    }

    @Test public void testKeyNavigationWithDisabledMenuItem() {
        VBox root = new VBox();
        Menu menu1 = new Menu("Menu1");
        Menu menu2 = new Menu("Menu2");
        Menu menu3 = new Menu("Menu3");

        MenuItem menuItem1 = new MenuItem("MenuItem1");
        MenuItem menuItem2 = new MenuItem("MenuItem2");
        MenuItem menuItem3 = new MenuItem("MenuItem3");

        menu1.getItems().add(menuItem1);
        menu2.getItems().add(menuItem2);
        menu3.getItems().add(menuItem3);

        menuBar.getMenus().addAll(menu1, menu2, menu3);
        menu2.setDisable(true);

        root.getChildren().addAll(menuBar);
        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        double xval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinX();
        double yval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinY();

        MenuButton mb = MenuBarSkinShim.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        assertTrue(menu1.isShowing());

        KeyEventFirer keyboard = new KeyEventFirer(mb.getScene());
        keyboard.doKeyPress(KeyCode.RIGHT);
        tk.firePulse();
        assertTrue(menu3.isShowing());
    }

    @Test public void testKeyNavigationForward() {
        VBox root = new VBox();
        Menu menu1 = new Menu("Menu1");
        Menu menu2 = new Menu("Menu2");
        Menu menu3 = new Menu("Menu3");

        MenuItem menuItem1 = new MenuItem("MenuItem1");
        MenuItem menuItem2 = new MenuItem("MenuItem2");
        MenuItem menuItem3 = new MenuItem("MenuItem3");

        menu1.getItems().add(menuItem1);
        menu2.getItems().add(menuItem2);
        menu3.getItems().add(menuItem3);

        menuBar.getMenus().addAll(menu1, menu2, menu3);
        menu2.setDisable(true);

        root.getChildren().addAll(menuBar);
        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        double xval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinX();
        double yval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinY();

        // Click on menu1 to open it
        MenuButton mb = MenuBarSkinShim.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        assertTrue(menu1.isShowing());

        // Right key press should skip menu2 that is disabled
        // and should open up menu3
        KeyEventFirer keyboard = new KeyEventFirer(mb.getScene());
        keyboard.doKeyPress(KeyCode.RIGHT);
        tk.firePulse();
        assertTrue(menu3.isShowing());

        // Another Right key press should loop back and should open up menu1
        keyboard.doKeyPress(KeyCode.RIGHT);
        tk.firePulse();
        assertTrue(menu1.isShowing());
    }

    @Test public void testKeyNavigationBackward() {
        VBox root = new VBox();
        Menu menu1 = new Menu("Menu1");
        Menu menu2 = new Menu("Menu2");
        Menu menu3 = new Menu("Menu3");

        MenuItem menuItem1 = new MenuItem("MenuItem1");
        MenuItem menuItem2 = new MenuItem("MenuItem2");
        MenuItem menuItem3 = new MenuItem("MenuItem3");

        menu1.getItems().add(menuItem1);
        menu2.getItems().add(menuItem2);
        menu3.getItems().add(menuItem3);

        menuBar.getMenus().addAll(menu1, menu2, menu3);
        menu2.setDisable(true);

        root.getChildren().addAll(menuBar);
        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        double xval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinX();
        double yval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinY();

        // Click on menu1 to open it
        MenuButton mb = MenuBarSkinShim.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        assertTrue(menu1.isShowing());

        // Left key press should cycle back and should open up menu3
        KeyEventFirer keyboard = new KeyEventFirer(mb.getScene());
        keyboard.doKeyPress(KeyCode.LEFT);
        tk.firePulse();
        assertTrue(menu3.isShowing());

        // Another Left key press should skip menu2 that is disabled
        // and should open up menu1
        keyboard.doKeyPress(KeyCode.LEFT);
        tk.firePulse();
        assertTrue(menu1.isShowing());
    }

    @Test public void testKeyNavigationWithAllDisabledMenuItems() {
        // Test key navigation with a single disabled menu in menubar
        VBox root = new VBox();
        Menu menu1 = new Menu("Menu1");
        MenuItem menuItem1 = new MenuItem("MenuItem1");

        menu1.getItems().add(menuItem1);
        menuBar.getMenus().addAll(menu1);
        menu1.setDisable(true);

        root.getChildren().addAll(menuBar);
        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        MenuButton mb = MenuBarSkinShim.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();
        KeyEventFirer keyboard = new KeyEventFirer(mb.getScene());

        // Selection key press
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.ALT);
        tk.firePulse();
        assertFalse(menu1.isShowing());


        // Test key navigation with multiple disabled menus in menubar
        Menu menu2 = new Menu("Menu2");
        Menu menu3 = new Menu("Menu3");

        MenuItem menuItem2 = new MenuItem("MenuItem2");
        MenuItem menuItem3 = new MenuItem("MenuItem3");

        menu2.getItems().add(menuItem2);
        menu3.getItems().add(menuItem3);

        menuBar.getMenus().add(menu2);
        menuBar.getMenus().add(menu3);

        menu2.setDisable(true);
        menu3.setDisable(true);

        // Selection key press
        keyboard.doKeyPress(KeyCode.RIGHT, KeyModifier.ALT);
        tk.firePulse();
        assertFalse(menu1.isShowing());
        assertFalse(menu2.isShowing());
        assertFalse(menu3.isShowing());
    }

     @Test public void testMenuOnShowingEventFiringWithMenuHideOperation() {
        VBox root = new VBox();
        Menu menu = new Menu("Menu");

        MenuItem menuItem1 = new MenuItem("MenuItem1");
        menu.getItems().addAll(menuItem1);

        menuBar.getMenus().add(menu);
        menuBar.setLayoutX(100);
        menuBar.setLayoutY(100);

        root.getChildren().addAll(menuBar);
        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);
        double xval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinX();
        double yval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinY();

        boolean click = true;
        final Boolean firstClick = new Boolean(click);

        menu.setOnShowing(t -> {
            // we should not get here when the menu is hidden
            assertEquals(firstClick.booleanValue(), true);
        });

        MenuButton mb = MenuBarSkinShim.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();
        mb.requestFocus();
        assertTrue(mb.isFocused());
        // click on menu to show
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        tk.firePulse();
        assertEquals(menu.showingProperty().get(), true);
        click = false;
        // click on menu to hide
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        tk.firePulse();
        assertEquals(menu.showingProperty().get(), false);
    }

    @Test public void testMenuBarUpdateOnMenuVisibilityChange() {
        VBox root = new VBox();
        Menu menu1 = new Menu("Menu1");
        Menu menu2 = new Menu("Menu2");

        menuBar.getMenus().addAll(menu1, menu2);

        root.getChildren().addAll(menuBar);
        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        // Test menuBar height to be non-zero as both menus are
        // visible by default
        int x = Double.compare(menuBar.getHeight(), 0.0);
        assertTrue(x > 0);

        // Test menubar height to be zero in case all of its
        // children are invisible
        menu1.setVisible(false);
        menu2.setVisible(false);
        tk.firePulse();

        assertEquals(menuBar.getHeight(), 0.0, 0.0001);

        // Test menuBar height to be non-zero if another menu is added
        Menu menu3 = new Menu("Menu3");
        menuBar.getMenus().add(menu3);
        tk.firePulse();

        x = Double.compare(menuBar.getHeight(), 0.0);
        assertTrue(x > 0);

        // Test menuBar height to be zero in case all of its
        // children are invisible
        menu3.setVisible(false);
        tk.firePulse();

        assertEquals(menuBar.getHeight(), 0.0, 0.0001);
    }

    @Test public void testRemovingMenuItemFromMenuNotInScene() {
        VBox root = new VBox();
        Menu menu = new Menu("Menu");

        MenuItem menuItem1 = new MenuItem("MenuItem1");
        menu.getItems().addAll(menuItem1);

        menuBar.getMenus().add(menu);
        root.getChildren().addAll(menuBar);
        startApp(root);
        tk.firePulse();

        // remove menu from menubar
        menuBar.getMenus().remove(menu);
        //remove menuitem from menu that was just removed itself.
        menu.getItems().remove(menuItem1);
        assertEquals(true, menu.getItems().isEmpty());
    }

    @Test public void test_rt_37118() {
        MenuBar menuBar = new MenuBar();
        MenuBarSkin menuBarSkin = new MenuBarSkin(menuBar);
    }

    @Test public void testMenuButtonMouseSelection() {
        VBox root = new VBox();
        Menu menu1 = new Menu("Menu1");
        MenuItem menuItem1 = new MenuItem("MenuItem1");

        menu1.getItems().add(menuItem1);

        menuBar.getMenus().addAll(menu1);
        root.getChildren().addAll(menuBar);
        startApp(root);
        tk.firePulse();

        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);

        double xval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinX();
        double yval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinY();

        MenuButton mb = MenuBarSkinShim.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();

        // Expand menu1 by mouse
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        assertTrue(menu1.isShowing());

        // Collapse menu1 by mouse
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        assertFalse(menu1.isShowing());

        // check if focusedMenuIndex is 0 (menu1 is still in selected state).
        int focusedIndex = MenuBarSkinShim.getFocusedMenuIndex(skin);
        assertEquals(0, focusedIndex);
    }
}
