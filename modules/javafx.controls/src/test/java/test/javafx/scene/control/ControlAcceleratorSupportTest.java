/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.getListenerCount;
import java.lang.ref.WeakReference;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.binding.ExpressionHelperUtility;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.util.memory.JMemoryBuddy;

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

    @Test
    public void testMenuButtonSceneChangeDoesntLeakScene() {
        // JDK-8283551
        // The scene was leaked in a ListChangeListener added by ControlAcceleratorSupport
        MenuItem menuItem = new MenuItem("Menu Item");
        MenuButton menuButton = new MenuButton("Menu Button", null, menuItem);
        StackPane root = new StackPane(menuButton);
        StackPane root2 = new StackPane();
        StageLoader sl1 = new StageLoader(root);
        StageLoader sl2 = new StageLoader(root2);
        WeakReference<Scene> scene1 = new WeakReference<>(sl1.getStage().getScene());
        root2.getChildren().add(menuButton);
        sl1.dispose();
        JMemoryBuddy.assertCollectable(scene1);
        sl2.dispose();
    }

    @Test
    public void testMenuButtonSceneChangeDoesntAddExtraListChangeListeners() {
        // JDK-8283551
        MenuItem menuItem = new MenuItem("Menu Item");
        Menu subMenu = new Menu("Sub Menu", null);
        MenuButton menuButton = new MenuButton("Menu Button", null, menuItem, subMenu);

        StackPane root = new StackPane(menuButton);
        StackPane root2 = new StackPane();
        StageLoader sl1 = new StageLoader(root);
        StageLoader sl2 = new StageLoader(root2);
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(menuButton.getItems()).size());
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(subMenu.getItems()).size());

        ListChangeListener originalMenuButtonListChangeListener =
                ExpressionHelperUtility.getListChangeListeners(menuButton.getItems()).get(1);
        ListChangeListener originalSubMenuListChangeListener =
                ExpressionHelperUtility.getListChangeListeners(subMenu.getItems()).get(1);

        // move the menu to another scene and check that the listeners got removed and recreated
        root2.getChildren().add(menuButton);
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(menuButton.getItems()).size());
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(subMenu.getItems()).size());

        ListChangeListener newMenuButtonListChangeListener =
                ExpressionHelperUtility.getListChangeListeners(menuButton.getItems()).get(1);
        ListChangeListener newSubMenuListChangeListener =
                ExpressionHelperUtility.getListChangeListeners(subMenu.getItems()).get(1);

        // use == instead of .equals because the list change listeners override equals to compare the observable list
        assertFalse(originalMenuButtonListChangeListener == newMenuButtonListChangeListener);
        assertFalse(originalSubMenuListChangeListener == newSubMenuListChangeListener);

        // Change to weak references and check that there are no remaining references
        WeakReference<ListChangeListener> wOriginalMenuButtonListChangeListener =
                new WeakReference<>(originalMenuButtonListChangeListener);
        WeakReference<ListChangeListener> wOriginalSubMenuListChangeListener =
                new WeakReference<>(originalSubMenuListChangeListener);
        originalMenuButtonListChangeListener = null;
        originalSubMenuListChangeListener = null;
        JMemoryBuddy.assertCollectable(wOriginalMenuButtonListChangeListener);
        JMemoryBuddy.assertCollectable(wOriginalSubMenuListChangeListener);

        sl1.dispose();
        sl2.dispose();
    }

    private void setActionMenuItem(Tab tab, MenuItem menuItem) {
        menuItem.setOnAction(e -> tab.setText("Tab Renamed"));
    }

    @Disabled("JDK-8283449")
    @Test
    public void testTabWithContextMenuReferencingTabDoesntCauseMemoryLeak() {
        // JDK-8283449 Tab with context menu that references tab gets leaked
        Tab tab = new Tab("Tab");
        TabPane tabPane = new TabPane(tab);

        MenuItem menuItemWithReferenceToTab = new MenuItem("RenameTabMenuItem");
        // method call so that we can set tab to null later without dealing with
        // "variable used in lambda expression should be final or effectively final"
        setActionMenuItem(tab, menuItemWithReferenceToTab);
        ContextMenu contextMenu = new ContextMenu(menuItemWithReferenceToTab);
        tab.setContextMenu(contextMenu);

        StackPane root = new StackPane(tabPane);
        StageLoader sl = new StageLoader(root);

        WeakReference<Tab> wTab = new WeakReference<>(tab);
        tab = null;
        menuItemWithReferenceToTab = null; // also holds a reference to tab through its action
        contextMenu = null;
        JMemoryBuddy.assertNotCollectable(wTab);
        tabPane.getTabs().remove(0);
        tabPane.getTabs().add(new Tab());
        // the TabPane still holds onto the tab through a reference in stackpane
        // adding a new tab and running a pulse gets rid of it
        Toolkit.getToolkit().firePulse();
        JMemoryBuddy.assertCollectable(wTab);
        sl.dispose();
    }

    @Test
    public void testSingleTabContextMenuGetsNewListChangeListenersWhenSceneChange() {
        // JDK-8283551
        // Test moving a tab pane from one scene to another removes and re-adds the appropriate ListChangeListeners
        Tab t = new Tab();
        TabPane tabPane = new TabPane(t);

        ContextMenu contextMenu1 = new ContextMenu();
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        StackPane root = new StackPane(tabPane);
        t.setContextMenu(contextMenu1); // set before in scene
        StageLoader sl1 = new StageLoader(root);

        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        // removing and re-adding removes and re-adds listeners
        root.getChildren().clear();
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        root.getChildren().add(tabPane);
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        ListChangeListener<? super MenuItem> originalListener1 =
                ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).get(1);

        StageLoader sl2 = new StageLoader(root); // change the scene
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        root.getChildren().clear();
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        root.getChildren().add(tabPane);
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());

        // the listeners are new
        for (ListChangeListener l : ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems())) {
            // can't use contains because the equals method for one of these listeners will return true
            assertFalse(l == originalListener1);
        }
        sl1.dispose();
        sl2.dispose();
    }

    @Disabled // Only the first tab's context menu gets a scene change listener right now
    @Test
    public void testMultipleTabContextMenuGetsNewListChangeListenersWhenSceneChange() {
        Tab t = new Tab();
        Tab t2 = new Tab();
        TabPane tabPane = new TabPane(t,t2);

        ContextMenu contextMenu1 = new ContextMenu();
        ContextMenu contextMenu2 = new ContextMenu();
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).size());
        StackPane root = new StackPane(tabPane);
        t.setContextMenu(contextMenu1); // set before in scene
        t2.setContextMenu(contextMenu2);
        StageLoader sl1 = new StageLoader(root);

        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).size());
        // removing and re-adding removes and re-adds listeners
        root.getChildren().clear();
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).size());
        root.getChildren().add(tabPane);
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).size());
        ListChangeListener<? super MenuItem> originalListener1 =
                ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).get(1);
        ListChangeListener<? super MenuItem> originalListener2 =
                ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).get(1);

        StageLoader sl2 = new StageLoader(root); // change the scene
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).size());
        root.getChildren().clear();
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).size());
        root.getChildren().add(tabPane);
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).size());

        // the listeners are new
        for (ListChangeListener l : ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems())) {
            // can't use contains because the equals method for one of these listeners will return true
            assertFalse(l == originalListener1);
        }
        for (ListChangeListener l : ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems())) {
            // can't use contains because the equals method for one of these listeners will return true
            assertFalse(l == originalListener2);
        }
        sl1.dispose();
        sl2.dispose();
    }

    @Test
    public void testTabContextMenuSceneChangeDoesntLeakScene() {
        // JDK-8283551
        // The scene was leaked in a ListChangeListener added by ControlAcceleratorSupport
        // For tab context menus, either the ListChangeListener is removed when the TabPane changes scenes
        // or when a new context menu is set. Both removals are tested here.

        Tab t = new Tab();
        TabPane tabPane = new TabPane(t);

        ContextMenu contextMenu1 = new ContextMenu();
        ContextMenu contextMenu2 = new ContextMenu();
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).size());
        StackPane root = new StackPane(tabPane);
        StageLoader sl1 = new StageLoader(root);
        t.setContextMenu(contextMenu1);

        // adding the context menu to the tab adds a list change listener
        assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());

        // swapping the context menu removes the previously added list change listener
        t.setContextMenu(contextMenu2);
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());

        // This assert fails because right now each anchor has one scene change listener only
        // There needs to be a scene change listener per ObservableList
        //assertEquals(2, ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).size());

        // Both context menus shouldn't have a list change listener that references the scene after its removed
        WeakReference<Scene> scene1 = new WeakReference<>(sl1.getStage().getScene());
        sl1.dispose();

        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu2.getItems()).size());
        JMemoryBuddy.assertCollectable(scene1);
    }

    @Disabled("JDK-8268374")
    @Test
    public void testContextMenuOnTabSetToNullWhenNotInSceneRemovesListeners() {
        // JDK-8268374
        Tab t = new Tab();
        TabPane tabPane = new TabPane(t);

        ContextMenu contextMenu1 = new ContextMenu();
        MenuItem item1 = new MenuItem();
        contextMenu1.getItems().add(item1);
        assertEquals(0, getListenerCount(item1.acceleratorProperty()));
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        t.setContextMenu(contextMenu1);
        StackPane root = new StackPane(tabPane);
        StageLoader sl1 = new StageLoader(root);
        assertEquals(1, getListenerCount(item1.acceleratorProperty()));
        root.getChildren().setAll(new StackPane()); // remove tabpane from scene
        assertEquals(0, getListenerCount(item1.acceleratorProperty()));
        t.setContextMenu(null);
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        root.getChildren().setAll(tabPane);
        assertEquals(0, getListenerCount(item1.acceleratorProperty()));
        sl1.dispose();
    }

    @Disabled("JDK-8268374")
    @Test
    public void testContextMenuOnTabSetToNullAfterTabIsRemoved() {
        // JDK-8268374
        Tab t = new Tab();
        TabPane tabPane = new TabPane(t);

        ContextMenu contextMenu1 = new ContextMenu();
        MenuItem item1 = new MenuItem();
        contextMenu1.getItems().add(item1);
        assertEquals(0, getListenerCount(item1.acceleratorProperty()));
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        t.setContextMenu(contextMenu1);
        StackPane root = new StackPane(tabPane);
        StageLoader sl1 = new StageLoader(root);
        assertEquals(1, getListenerCount(item1.acceleratorProperty()));
        root.getChildren().setAll(new StackPane());
        tabPane.getTabs().remove(0);
        assertEquals(0, getListenerCount(item1.acceleratorProperty()));
        t.setContextMenu(null);
        assertEquals(0, ExpressionHelperUtility.getListChangeListeners(contextMenu1.getItems()).size());
        tabPane.getTabs().add(t);
        root.getChildren().setAll(tabPane);
        assertEquals(0, getListenerCount(item1.acceleratorProperty()));
        sl1.dispose();
    }
}
