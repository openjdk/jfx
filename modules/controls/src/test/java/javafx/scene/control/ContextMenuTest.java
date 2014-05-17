/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import com.sun.javafx.scene.control.infrastructure.StageLoader;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import javafx.geometry.Side;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static com.sun.javafx.scene.control.skin.ContextMenuContentRetriever.*;

public class ContextMenuTest {
    private MenuItem menuItem0, menuItem1, menuItem2, menuItem3;

    private ContextMenu contextMenu;
    private ContextMenu contextMenuWithOneItem;
    private ContextMenu contextMenuWithManyItems;

    private StageLoader sl;
    private Button anchorBtn;

    @Before public void setup() {
        // earlier test items
        menuItem0 = new MenuItem();
        menuItem1 = new MenuItem(); 
        menuItem2 = new MenuItem(); 
        menuItem3 = new MenuItem();

        contextMenu = new ContextMenu();
        contextMenuWithOneItem = new ContextMenu(menuItem0);
        contextMenuWithManyItems = new ContextMenu(menuItem1, menuItem2, menuItem3);


        // more recent test items
        // specify items (layout relates to the item positioning inside menus)
        menuItem = new MenuItem("MenuItem 1");
        subMenu = new Menu("submenu");
            subMenuItem1 = new MenuItem("SubMenuItem 1");
            customMenuItem = new CustomMenuItem(new Label("CustomMenuItem 1"));

        // install items into menus
        subMenu.getItems().setAll(subMenuItem1, customMenuItem);

        // for the show/hide tests, we need a stage with an anchor in it
        anchorBtn = new Button("Anchor");
        sl = new StageLoader(anchorBtn);
    }

    @After public void after() {
        sl.dispose();
    }

    @Test public void defaultGetId() {
        assertNull(contextMenu.getId());
    }

    @Test public void getStyleClassNotNull() {
        assertNotNull(contextMenu.getStyleClass());
    }

    @Test public void shouldBeAutoHideOn() {
        assertTrue(contextMenu.isAutoHide());
    }

    @Test public void shouldHaveZeroItems() {
        assertEquals(0, contextMenu.getItems().size());
    }

    @Test public void shouldHaveOneItem() {
        assertEquals(1, contextMenuWithOneItem.getItems().size());
    }

    @Test public void shouldHaveManyItems() {
        assertEquals(3, contextMenuWithManyItems.getItems().size());
    }

    @Test public void getDefaultSetOnActionHandler() {
        assertNull(contextMenu.getOnAction());
    }

    @Test public void getSpecifiedSetOnActionHandler() {
        EventHandlerStub handler = new EventHandlerStub();
        contextMenu.setOnAction(handler);
        assertEquals(handler, contextMenu.getOnAction());
    }

    @Test public void setTwiceAndGetSpecifiedSetOnActionHandler() {
        EventHandlerStub handler1 = new EventHandlerStub();
        EventHandlerStub handler2 = new EventHandlerStub();
        contextMenu.setOnAction(handler1);
        contextMenu.setOnAction(handler2);
        assertEquals(handler2, contextMenu.getOnAction());
    }

    @Test public void getNullSetOnActionHandler() {
        contextMenu.setOnAction(null);
        assertNull(contextMenu.getOnAction());
    }

    @Test public void defaultOnActionPropertyNotNull() {
        assertNotNull(contextMenu.onActionProperty());
    }

    @Test public void getOnActionPropertyBean() {
        assertEquals(contextMenu, contextMenu.onActionProperty().getBean());
    }

    @Test public void getOnActionPropertyName() {
        assertEquals("onAction", contextMenu.onActionProperty().getName());
    }

    @Test public void removedItemsAreChanged() {
        contextMenuWithManyItems.getItems().remove(menuItem2);
        assertNull(menuItem2.getParentPopup());
    }

    @Test public void addedItemsAreChanged() {
        MenuItem addedMenuItem = new MenuItem();
        contextMenuWithManyItems.getItems().add(addedMenuItem);
        assertEquals(contextMenuWithManyItems, addedMenuItem.getParentPopup());
    }

    @Test public void test_rt_34106_menus_should_not_be_reused() {
        // This test ensures the new behavior of ContextMenu's whereby it is only
        // allowed for a Menu/MenuItem to be in one parentPopup at a time.
        // Previously we allowed multiple ContextMenus to refer to the same
        // Menu/MenuItem, but this didn't work as there was no way to discern
        // when to show
        //
        MenuItem item1 = new MenuItem("MenuItem 1");
        Menu menu = new Menu("Menu");
        menu.getItems().addAll(item1);

        ContextMenu cm1 = new ContextMenu(menu);
        assertEquals(1, cm1.getItems().size());
        assertEquals(menu, cm1.getItems().get(0));
        assertEquals(cm1, menu.getParentPopup());
        assertEquals(cm1, item1.getParentPopup());

        ContextMenu cm2 = new ContextMenu(menu);
        assertEquals(0, cm1.getItems().size());
        assertEquals(1, cm2.getItems().size());
        assertEquals(menu, cm2.getItems().get(0));
        assertEquals(cm2, menu.getParentPopup());
        assertEquals(cm2, item1.getParentPopup());
    }

    public static final class EventHandlerStub implements EventHandler<ActionEvent> {
        boolean called = false;
        @Override public void handle(ActionEvent event) {
            called = true;
        }
    };





    private MenuItem menuItem;
    private Menu subMenu;
    private MenuItem subMenuItem1;
    private CustomMenuItem customMenuItem;

    private ContextMenu createContextMenu(boolean showMenu) {
        // create and return the context menu with the root menu in it
        ContextMenu contextMenu = new ContextMenu(menuItem, subMenu);

        if (showMenu) {
            contextMenu.show(anchorBtn, Side.RIGHT, 0, 0);
        }
        return contextMenu;
    }

    private void showMenu(ContextMenu cm, MenuItem... browseTo) {
        cm.show(anchorBtn, Side.RIGHT, 0, 0);

        if (browseTo == null) return;

        // navigate through the browseTo list, focusing / expanding as necessary
        for (int i = 0; i < browseTo.length; i++) {
            MenuItem item = browseTo[i];

            // find item in current showing menu
            boolean found = false;
            while (true) {
                MenuItem focusedItem = getCurrentFocusedItem(cm);
                if (item == focusedItem) {
                    found = true;
                    break;
                }

                pressDownKey(cm);
            }

            if (! found) {
                break;
            } else {
                if (item instanceof Menu) {
                    pressRightKey(cm);
                }
            }
        }
    }

    private ContextMenu createContextMenuAndShowSubMenu() {
        ContextMenu cm = createContextMenu(true);

        // press down twice to go to subMenu
        pressDownKey(cm);
        pressDownKey(cm);

        // ensure the submenu isn't showing (it should only show on right-arrow)
        assertFalse(subMenu.isShowing());

        // open up the submenu
        pressRightKey(cm);
        assertTrue(subMenu.isShowing());
        assertEquals(subMenu, getShowingSubMenu(cm));

        // make sure the first item of the subMenu is focused
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + subMenuItem1.getText() + ", found " + focusedItem.getText(),
                subMenuItem1, focusedItem);

        return cm;
    }

    @Test public void test_showAndHide() {
        ContextMenu cm = createContextMenu(false);
        assertFalse(cm.isShowing());

        cm.show(anchorBtn, Side.RIGHT, 0, 0);
        assertTrue(cm.isShowing());

        cm.hide();
        assertFalse(cm.isShowing());
    }

    @Test public void test_navigateMenu_downwards() {
        ContextMenu cm = createContextMenu(true);

        assertNotNull(getShowingMenuContent(cm));
        assertEquals(-1, getCurrentFocusedIndex(cm));

        // press down once to go to menuItem
        pressDownKey(cm);
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + menuItem.getText() + ", found " + focusedItem.getText(),
                menuItem, focusedItem);

        // press down once to go to subMenu
        pressDownKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals(1, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + subMenu.getText() + ", found " + focusedItem.getText(),
                subMenu, focusedItem);

        // ensure the submenu isn't showing (it should only show on right-arrow)
        assertFalse(subMenu.isShowing());

        // press down once more to loop back to the top (menuItem)
        pressDownKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + menuItem.getText() + ", found " + focusedItem.getText(),
                menuItem, focusedItem);
    }

    @Test public void test_navigateMenu_upwards() {
        ContextMenu cm = createContextMenu(true);

        assertNotNull(getShowingMenuContent(cm));
        assertEquals(-1, getCurrentFocusedIndex(cm));

        // press up once to loop to the last menu item (subMenu)
        pressUpKey(cm);
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals(1, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + subMenu.getText() + ", found " + focusedItem.getText(),
                subMenu, focusedItem);

        // press up once to go to menuItem
        pressDownKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + menuItem.getText() + ", found " + focusedItem.getText(),
                menuItem, focusedItem);
    }

    @Test public void test_navigateMenu_showSubMenu() {
        createContextMenuAndShowSubMenu();
    }

    @Test public void test_navigateSubMenu_downwards() {
        ContextMenu cm = createContextMenuAndShowSubMenu();

        // we now have focus in the submenu, and on its first item, so lets navigate it
        assertNotNull(getShowingMenuContent(cm));
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + subMenuItem1.getText() + ", found " + focusedItem.getText(),
                subMenuItem1, focusedItem);

        // press down once to go to customMenuItem
        pressDownKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals(1, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + customMenuItem.getText() + ", found " + focusedItem.getText(),
                customMenuItem, focusedItem);

        // press down once to go to wrap back around to subMenuItem1
        pressDownKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + subMenuItem1.getText() + ", found " + focusedItem.getText(),
                subMenuItem1, focusedItem);
    }

    @Test public void test_navigateSubMenu_upwards() {
        ContextMenu cm = createContextMenuAndShowSubMenu();

        // we now have focus in the submenu, and on its first item, so lets navigate it
        assertNotNull(getShowingMenuContent(cm));
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + subMenuItem1.getText() + ", found " + focusedItem.getText(),
                subMenuItem1, focusedItem);

        // press up once to go to customMenuItem
        pressUpKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals(1, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + customMenuItem.getText() + ", found " + focusedItem.getText(),
                customMenuItem, focusedItem);

        // press up once to go to subMenuItem1
        pressUpKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + subMenuItem1.getText() + ", found " + focusedItem.getText(),
                subMenuItem1, focusedItem);
    }

    @Test public void test_navigateSubMenu_rightKeyDoesNothing() {
        ContextMenu cm = createContextMenuAndShowSubMenu();

        // we now have focus in the submenu, and on its first item, so lets navigate it
        pressRightKey(cm);
        assertNotNull(getShowingMenuContent(cm));
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + subMenuItem1.getText() + ", found " + focusedItem.getText(),
                subMenuItem1, focusedItem);
    }

    @Test public void test_navigateSubMenu_leftKeyClosesSubMenu() {
        ContextMenu cm = createContextMenuAndShowSubMenu();

        // we now have focus in the submenu, and on its first item.
        // If we press left we expect the submenu to close and for focus to go
        // back to the parent menu.
        pressLeftKey(cm);
        assertNotNull(getShowingMenuContent(cm));
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals(1, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + subMenu.getText() + ", found " + focusedItem.getText(),
                subMenu, focusedItem);
    }

    private int rt_37127_count = 0;
    @Test public void test_rt_37127_keyboard() {
        ContextMenu cm = createContextMenuAndShowSubMenu();

        customMenuItem.setOnAction(event -> rt_37127_count++);

        // we now have focus in the submenu, and on its first item.
        // For this test we now need to move focus down to the custom menu item
        // and press the enter key. We expect to only receive one event
        pressDownKey(cm);
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals(1, getCurrentFocusedIndex(cm));
        assertEquals(customMenuItem, focusedItem);

        assertEquals(0, rt_37127_count);
        pressEnterKey(cm);
        assertEquals(1, rt_37127_count);

        // now go back to the customMenuItem and press it again
        showMenu(cm, subMenu, customMenuItem);
        pressEnterKey(cm);
        assertEquals(2, rt_37127_count);

        // and once more....
        showMenu(cm, subMenu, customMenuItem);
        pressEnterKey(cm);
        assertEquals(3, rt_37127_count);
    }

    @Test public void test_rt_37127_mouse() {
        ContextMenu cm = createContextMenuAndShowSubMenu();

        customMenuItem.setOnAction(event -> rt_37127_count++);

        // we now have focus in the submenu, and on its first item.
        // For this test we now need to move focus down to the custom menu item
        // and press the enter key. We expect to only receive one event
        pressDownKey(cm);
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals(1, getCurrentFocusedIndex(cm));
        assertEquals(customMenuItem, focusedItem);

        assertEquals(0, rt_37127_count);
        pressMouseButton(cm);
        assertEquals(1, rt_37127_count);

        // now go back to the customMenuItem and press it again
        showMenu(cm, subMenu, customMenuItem);
        pressMouseButton(cm);
        assertEquals(2, rt_37127_count);

        // and once more....
        showMenu(cm, subMenu, customMenuItem);
        pressMouseButton(cm);
        assertEquals(3, rt_37127_count);
    }

    @Test public void test_rt_37102() {
        // This resulted in a NPE before the bug was fixed
        ContextMenu cm = createContextMenuAndShowSubMenu();
        pressLeftKey(cm);
        showMenu(cm, subMenu);
    }

    @Test public void test_rt_37091() {
        ContextMenu cm = createContextMenuAndShowSubMenu();

        assertEquals(subMenu, getShowingSubMenu(cm));
        assertEquals(subMenu, getOpenSubMenu(cm));

        cm.hide();
        assertNull(getOpenSubMenu(cm));

        cm.getItems().clear();
        cm.getItems().add(subMenu);

        assertNull(getOpenSubMenu(cm));
        cm.show(anchorBtn, Side.RIGHT, 0, 0);
        pressDownKey(cm);
        pressDownKey(cm);
        pressRightKey(cm);
        assertEquals(subMenu, getShowingSubMenu(cm));
        assertEquals(subMenuItem1, getCurrentFocusedItem(cm));
    }
}
