/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class AcceleratorParameterizedTest {

    private int eventCounter = 0;

    private MenuItem item1;
    private ContextMenu menu;

    private ObjectProperty<ContextMenu> contextMenuProperty;

    private Button btn;
    private Tab tab;
    private TableColumn<?,?> tableColumn;
    private TreeTableColumn<?,?> treeTableColumn;

    private StageLoader sl;
    private Scene scene;
    private KeyEventFirer keyboard;

    private Class<?> testClass;

    @Parameterized.Parameters
    public static Collection implementations() {
        return Arrays.asList(new Object[][]{
            {Button.class},
            {Tab.class},
            {TableColumn.class},
            {TreeTableColumn.class}
        });
    }

    public AcceleratorParameterizedTest(Class<?> testClass) {
        this.testClass = testClass;
    }

    @Before public void setup() {
        eventCounter = 0;

        item1 = new MenuItem("Item 1");
        item1.setOnAction(e -> eventCounter++);
        item1.setAccelerator(KeyCombination.valueOf("alt+1"));

        menu = new ContextMenu(item1);

        if (testClass == Button.class) {
            btn = new Button("Btn");
            btn.setOnAction(e -> fail("This shouldn't ever happen!"));
            btn.setContextMenu(menu);
            contextMenuProperty = btn.contextMenuProperty();

            sl = new StageLoader(btn);
        } else if (testClass == Tab.class) {
            tab = new Tab("Tab");
            tab.setContextMenu(menu);
            contextMenuProperty = tab.contextMenuProperty();

            TabPane tabPane = new TabPane();
            tabPane.getTabs().add(tab);
            sl = new StageLoader(tabPane);
        } else if (testClass == TableColumn.class) {
            tableColumn = new TableColumn<>("TableColumn");
            tableColumn.setContextMenu(menu);
            contextMenuProperty = tableColumn.contextMenuProperty();

            TableView tableView = new TableView();
            tableView.getColumns().add(tableColumn);

            sl = new StageLoader(tableView);
        } else if (testClass == TreeTableColumn.class) {
            treeTableColumn = new TreeTableColumn<>("TableColumn");
            treeTableColumn.setContextMenu(menu);
            contextMenuProperty = treeTableColumn.contextMenuProperty();

            TreeTableView tableView = new TreeTableView();
            tableView.getColumns().add(treeTableColumn);

            sl = new StageLoader(tableView);
        }

        scene = sl.getStage().getScene();
        keyboard = new KeyEventFirer(scene);
    }

    @After public void cleanup() {
        sl.dispose();
    }

    // this lets us test to ensure that a KeyCombination (i.e. an accelerator)
    // is not leaking in the Scene after the accelerator has been removed
    // (e.g. the accelerator is changed, nulled out, menu item removed, etc).
    private void assertSceneDoesNotContainKeyCombination(KeyCombination keyCombination) {
        assertFalse(scene.getAccelerators().containsKey(keyCombination));
    }


    @Test public void rt_28136_assertContextMenuAcceleratorWorks() {
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);

        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(3, eventCounter);
    }

    @Test public void rt_28136_assertContextMenuAcceleratorWorksWithNewMenuItemAdded() {
        MenuItem item2 = new MenuItem("Item 2");
        item2.setOnAction(e -> eventCounter++);
        item2.setAccelerator(KeyCombination.valueOf("alt+2"));
        menu.getItems().add(item2);

        // fire old accelerator on first menu item
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);

        // fire the new menu item accelerator
        keyboard.doKeyPress(KeyCode.DIGIT2, KeyModifier.ALT);
        assertEquals(2, eventCounter);
    }

    @Test public void rt_28136_assertContextMenuStopsFiringWhenMenuItemRemoved() {
        menu.getItems().remove(item1);
        assertSceneDoesNotContainKeyCombination(KeyCombination.keyCombination("alt+1"));

        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(0, eventCounter);

        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(0, eventCounter);
    }

    @Test public void rt_28136_assertAcceleratorChangeToNullWorks_oldAcceleratorStopsFiring() {
        item1.setAccelerator(null);
        assertSceneDoesNotContainKeyCombination(KeyCombination.keyCombination("alt+1"));

        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(0, eventCounter);
    }

    @Test public void rt_28136_assertAcceleratorChangeToNonNullWorks_oldAcceleratorStopsFiring() {
        item1.setAccelerator(KeyCombination.valueOf("alt+A"));
        assertSceneDoesNotContainKeyCombination(KeyCombination.keyCombination("alt+1"));

        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(0, eventCounter);
    }

    @Test public void rt_28136_assertAcceleratorChangeToNonNullWorks_newAcceleratorStartsFiring() {
        item1.setAccelerator(KeyCombination.valueOf("alt+A"));
        assertSceneDoesNotContainKeyCombination(KeyCombination.keyCombination("alt+1"));

        keyboard.doKeyPress(KeyCode.A, KeyModifier.ALT);
        assertEquals(1, eventCounter);
    }

    @Test public void rt_28136_assertNewMenuWithMenuItemAcceleratorsFire() {
        MenuItem item2 = new MenuItem("Item 2");
        item2.setOnAction(e -> eventCounter++);
        item2.setAccelerator(KeyCombination.valueOf("alt+2"));

        Menu subMenu = new Menu("Submenu");
        subMenu.getItems().add(item2);

        menu.getItems().add(subMenu);

        // fire old accelerator on first menu item
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);

        // fire the new menu item accelerator
        keyboard.doKeyPress(KeyCode.DIGIT2, KeyModifier.ALT);
        assertEquals(2, eventCounter);
    }

    @Test public void rt_28136_assertRemovedMenuItemAcceleratorInSubmenuDoesNotFire() {
        MenuItem item2 = new MenuItem("Item 2");
        item2.setOnAction(e -> eventCounter++);
        item2.setAccelerator(KeyCombination.valueOf("alt+2"));

        Menu subMenu = new Menu("Submenu");
        subMenu.getItems().add(item2);

        menu.getItems().add(subMenu);

        // fire old accelerator on first menu item
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);

        // fire the new menu item accelerator
        keyboard.doKeyPress(KeyCode.DIGIT2, KeyModifier.ALT);
        assertEquals(2, eventCounter);

        // remove the menu item in the submenu
        subMenu.getItems().remove(item2);
        assertSceneDoesNotContainKeyCombination(KeyCombination.keyCombination("alt+2"));

        // fire old accelerator on first menu item
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(3, eventCounter);

        // fire the new menu item accelerator (which has now been removed)
        keyboard.doKeyPress(KeyCode.DIGIT2, KeyModifier.ALT);
        assertEquals(3, eventCounter);
    }

    @Test public void rt_28136_assertRemovedMenuWithMenuItemAcceleratorsDoesNotFire() {
        MenuItem item2 = new MenuItem("Item 2");
        item2.setOnAction(e -> eventCounter++);
        item2.setAccelerator(KeyCombination.valueOf("alt+2"));

        Menu subMenu = new Menu("Submenu");
        subMenu.getItems().add(item2);

        menu.getItems().add(subMenu);

        // fire old accelerator on first menu item
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);

        // fire the new menu item accelerator
        keyboard.doKeyPress(KeyCode.DIGIT2, KeyModifier.ALT);
        assertEquals(2, eventCounter);

        // remove the menu entirely
        menu.getItems().remove(subMenu);
        assertSceneDoesNotContainKeyCombination(KeyCombination.keyCombination("alt+2"));

        // fire old accelerator on first menu item
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(3, eventCounter);

        // fire the new menu item accelerator (which has now been removed)
        keyboard.doKeyPress(KeyCode.DIGIT2, KeyModifier.ALT);
        assertEquals(3, eventCounter);
    }

    @Test public void rt_28136_assertAcceleratorIsNotFiredWhenContextMenuIsRemoved() {
        contextMenuProperty.set(null);

        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(0, eventCounter);
    }

    @Test public void testAcceleratorShouldNotGetFiredWhenMenuItemRemovedFromScene() {
        KeyEventFirer kb = new KeyEventFirer(item1, scene);

        kb.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);
        assertEquals(1, getListenerCount(item1.acceleratorProperty()));

        // Remove all children from the scene
        Group root = (Group)scene.getRoot();
        root.getChildren().clear();

        assertEquals(0, getListenerCount(item1.acceleratorProperty()));
        kb.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);
    }

    @Test public void testAcceleratorShouldGetFiredWhenMenuItemRemovedAndAddedBackToScene() {
        KeyEventFirer kb = new KeyEventFirer(item1, scene);

        kb.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);

        Group root = (Group)scene.getRoot();
        scene.setRoot(new Group()); // Remove all children from the scene
        scene.setRoot(root); // Add the children to the same scene

        assertEquals(1, getListenerCount(item1.acceleratorProperty()));
        kb.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(2, eventCounter);
    }

    @Test public void testAcceleratorShouldGetFiredWhenMenuItemRemovedAndAddedToDifferentScene() {
        KeyEventFirer kb = new KeyEventFirer(item1, scene);

        kb.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);

        Group root = (Group)scene.getRoot();
        scene.setRoot(new Group()); // Remove all children from the scene
        Scene diffScene = new Scene(root); // Add the children to a different scene
        sl.getStage().setScene(diffScene);
        kb = new KeyEventFirer(item1, diffScene);

        assertEquals(1, getListenerCount(item1.acceleratorProperty()));
        kb.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(2, eventCounter);
    }

    @Ignore("JDK-8268374")
    @Test public void testAcceleratorShouldNotGetFiredWhenControlsIsRemovedFromSceneThenContextMenuIsSetToNullAndControlIsAddedBackToScene() {
        KeyEventFirer kb = new KeyEventFirer(item1, scene);
        kb.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);

        Group root = (Group)scene.getRoot();
        scene.setRoot(new Group()); // Remove all children from the scene

        if (testClass == Button.class) {
            btn.setContextMenu(null);
        } else if (testClass == Tab.class) {
            tab.setContextMenu(null);
        } else if (testClass == TableColumn.class) {
            tableColumn.setContextMenu(null);
        } else if (testClass == TreeTableColumn.class) {
            treeTableColumn.setContextMenu(null);
        }
        scene.setRoot(root); // Add the children to a different scene

        assertEquals(0, getListenerCount(item1.acceleratorProperty()));
        kb.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);
    }
}
