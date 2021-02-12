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

import com.sun.javafx.scene.control.ContextMenuContent;
import com.sun.javafx.scene.control.ContextMenuContentShim;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static com.sun.javafx.scene.control.ContextMenuContentShim.*;

import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;

public class ContextMenuTest {

    public static void pressDownKey(ContextMenu menu) {
        Optional<ContextMenuContent> showingMenuContent = ContextMenuContentShim.getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            new KeyEventFirer(content).doDownArrowPress();
        }
    }

    public static void pressUpKey(ContextMenu menu) {
        Optional<ContextMenuContent> showingMenuContent = ContextMenuContentShim.getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            new KeyEventFirer(content).doUpArrowPress();
        }
    }

    public static void pressRightKey(ContextMenu menu) {
        Optional<ContextMenuContent> showingMenuContent = ContextMenuContentShim.getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            new KeyEventFirer(content).doRightArrowPress();
        }
    }

    public static void pressEnterKey(ContextMenu menu) {
        Optional<ContextMenuContent> showingMenuContent = ContextMenuContentShim.getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            new KeyEventFirer(content).doKeyPress(KeyCode.ENTER);
        }
    }

    public static void pressLeftKey(ContextMenu menu) {
        Optional<ContextMenuContent> showingMenuContent = ContextMenuContentShim.getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent content = showingMenuContent.get();
            new KeyEventFirer(content).doLeftArrowPress();
        }
    }

    public static void pressMouseButton(ContextMenu menu) {
        Optional<ContextMenuContent> showingMenuContent = ContextMenuContentShim.getShowingMenuContent(menu);
        if (showingMenuContent.isPresent()) {
            ContextMenuContent.MenuItemContainer itemContainer =  (ContextMenuContent.MenuItemContainer)
                    ContextMenuContentShim.get_selectedBackground(showingMenuContent.get());
            MenuItem item = itemContainer.getItem();
            if (item instanceof CustomMenuItem) {
                Node customContent = ((CustomMenuItem) item).getContent();
                new MouseEventFirer(customContent).fireMouseClicked();
            } else {
                new MouseEventFirer(itemContainer).fireMousePressAndRelease();
            }
        }
    }

    private MenuItem menuItem0, menuItem1, menuItem2, menuItem3;

    private ContextMenu contextMenu;
    private ContextMenu contextMenuWithOneItem;
    private ContextMenu contextMenuWithManyItems;

    private StageLoader sl;
    private Button anchorBtn;

    @Before public void setup() {
        // earlier test items
        menuItem0 = new MenuItem("0");
        menuItem1 = new MenuItem("1");
        menuItem2 = new MenuItem("2");
        menuItem3 = new MenuItem("3");

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

    @Test public void test_emptySubMenu_rightKeyDoesNothing() {
        Menu testMenu = new Menu("Menu1");
        ContextMenu testCM = new ContextMenu();

        testCM.getItems().addAll(testMenu);
        testCM.show(anchorBtn, Side.RIGHT, 0, 0);
        assertNotNull(getShowingMenuContent(testCM));

        // Go to testMenu
        pressDownKey(testCM);

        // testMenu does not have any subMenu - try to open it
        // this used to casue NPE - fixed in JDK-8241710
        pressRightKey(testCM);
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

    @Test public void test_navigateMenu_withInvisibleItems_rt40689() {
        ContextMenu cm = contextMenuWithManyItems;
        cm.show(anchorBtn, Side.RIGHT, 0, 0);

        menuItem2.setVisible(false);

        assertNotNull(getShowingMenuContent(cm));
        assertEquals(-1, getCurrentFocusedIndex(cm));

        // press down once to go to menuItem
        pressDownKey(cm);
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals("Expected " + menuItem1.getText() + ", found " + focusedItem.getText(), menuItem1, focusedItem);

        // press down again should skip invisible menuItem2 and proceed to menuItem3
        pressDownKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals("Expected " + menuItem3.getText() + ", found " + focusedItem.getText(), menuItem3, focusedItem);

        // press up should skip invisible menuItem2 and proceed to menuItem1
        pressUpKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals("Expected " + menuItem1.getText() + ", found " + focusedItem.getText(), menuItem1, focusedItem);
    }

    @Test public void test_jdk_8167132_issue_1() {
        ContextMenu cm = createContextMenu(true);

        MenuItem item1, item2, item3, item4;
        cm.getItems().setAll(
                item1 = new MenuItem("Item 1"),
                item2 = new MenuItem("Item 2"),
                item3 = new MenuItem("Item 3"),
                item4 = new MenuItem("Item 4"));

        assertEquals(-1, getCurrentFocusedIndex(cm));

        // press down once to go to item1
        pressDownKey(cm);
        MenuItem focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + item1.getText() + ", found " + focusedItem.getText(),
                item1, focusedItem);

        // press down once to go to item2
        pressDownKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals(1, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + item2.getText() + ", found " + focusedItem.getText(),
                item2, focusedItem);

        // hide context menu
        cm.hide();

        // show context menu again
        cm.show(anchorBtn, Side.RIGHT, 0, 0);

        // assert that focus is now not anywhere to be seen
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals(-1, getCurrentFocusedIndex(cm));
        assertNull(focusedItem);

        // press down once to go to item1
        pressDownKey(cm);
        focusedItem = getCurrentFocusedItem(cm);
        assertEquals(0, getCurrentFocusedIndex(cm));
        assertEquals("Expected " + item1.getText() + ", found " + focusedItem.getText(),
                item1, focusedItem);
    }

    @Test public void test_position_showOnScreen() {
        ContextMenu cm = createContextMenu(false);
        cm.show(anchorBtn, 100, 100);

        assertEquals(100, cm.getAnchorX(), 0.0);
        assertEquals(100, cm.getAnchorY(), 0.0);
    }

    @Test public void test_position_showOnTop() throws InterruptedException {
        ContextMenu cm = createContextMenu(false);
        cm.show(anchorBtn, Side.TOP, 0, 0);

        Bounds anchorBounds = anchorBtn.localToScreen(anchorBtn.getLayoutBounds());
        Node cmNode = cm.getScene().getRoot();
        Bounds cmBounds = cm.getScene().getRoot().localToScreen(cmNode.getLayoutBounds());

        assertEquals(anchorBounds.getMinX(), cmBounds.getMinX(), 0.0);
        assertEquals(anchorBounds.getMinY(), cmBounds.getMaxY(), 0.0);
    }

    @Test public void test_position_showOnTopOffset() throws InterruptedException {
        ContextMenu cm = createContextMenu(false);
        cm.show(anchorBtn, Side.TOP, 3, 5);

        Bounds anchorBounds = anchorBtn.localToScreen(anchorBtn.getLayoutBounds());
        Node cmNode = cm.getScene().getRoot();
        Bounds cmBounds = cm.getScene().getRoot().localToScreen(cmNode.getLayoutBounds());

        assertEquals(anchorBounds.getMinX() + 3, cmBounds.getMinX(), 0.0);
        assertEquals(anchorBounds.getMinY() + 5, cmBounds.getMaxY(), 0.0);
    }

    @Test public void test_position_withOrientationTop() throws InterruptedException {
        ContextMenu cm = createContextMenu(false);
        anchorBtn.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        cm.show(anchorBtn, Side.TOP, 0, 0);

        Bounds anchorBounds = anchorBtn.localToScreen(anchorBtn.getLayoutBounds());
        Node cmNode = cm.getScene().getRoot();
        Bounds cmBounds = cm.getScene().getRoot().localToScreen(cmNode.getLayoutBounds());

        assertEquals(anchorBounds.getMaxX(), cmBounds.getMaxX(), 0.0);
        assertEquals(anchorBounds.getMinY(), cmBounds.getMaxY(), 0.0);
    }

    @Test public void test_position_withOrientationLeft() throws InterruptedException {
        ContextMenu cm = createContextMenu(false);
        anchorBtn.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        cm.show(anchorBtn, Side.LEFT, 0, 0);

        Bounds anchorBounds = anchorBtn.localToScreen(anchorBtn.getLayoutBounds());
        Node cmNode = cm.getScene().getRoot();
        Bounds cmBounds = cm.getScene().getRoot().localToScreen(cmNode.getLayoutBounds());

        assertEquals(anchorBounds.getMaxX(), cmBounds.getMinX(), 0.0);
        assertEquals(anchorBounds.getMinY(), cmBounds.getMinY(), 0.0);
    }


    @Test public void test_position_withCSS() throws InterruptedException {
        anchorBtn.getScene().getStylesheets().add(
            getClass().getResource("test_position_showOnTopWithCSS.css").toExternalForm()
        );
        test_position_showOnTop();
        test_position_showOnRight();
        test_position_showOnLeft();
        test_position_showOnBottom();
    }

    @Test public void test_position_showOnRight() {
        ContextMenu cm = createContextMenu(false);
        cm.show(anchorBtn, Side.RIGHT, 0, 0);

        Bounds anchorBounds = anchorBtn.localToScreen(anchorBtn.getLayoutBounds());
        Node cmNode = cm.getScene().getRoot();
        Bounds cmBounds = cm.getScene().getRoot().localToScreen(cmNode.getLayoutBounds());

        assertEquals(anchorBounds.getMaxX(), cmBounds.getMinX(), 0.0);
        assertEquals(anchorBounds.getMinY(), cmBounds.getMinY(), 0.0);
    }

    @Test public void test_position_showOnRightOffset() {
        ContextMenu cm = createContextMenu(false);
        cm.show(anchorBtn, Side.RIGHT, 3, 5);

        Bounds anchorBounds = anchorBtn.localToScreen(anchorBtn.getLayoutBounds());
        Node cmNode = cm.getScene().getRoot();
        Bounds cmBounds = cm.getScene().getRoot().localToScreen(cmNode.getLayoutBounds());

        assertEquals(anchorBounds.getMaxX() + 3, cmBounds.getMinX(), 0.0);
        assertEquals(anchorBounds.getMinY() + 5, cmBounds.getMinY(), 0.0);
    }

    @Test public void test_position_showOnBottom() {
        ContextMenu cm = createContextMenu(false);
        cm.show(anchorBtn, Side.BOTTOM, 0, 0);

        Bounds anchorBounds = anchorBtn.localToScreen(anchorBtn.getLayoutBounds());
        Node cmNode = cm.getScene().getRoot();
        Bounds cmBounds = cm.getScene().getRoot().localToScreen(cmNode.getLayoutBounds());

        assertEquals(anchorBounds.getMinX(), cmBounds.getMinX(), 0.0);
        assertEquals(anchorBounds.getMaxY(), cmBounds.getMinY(), 0.0);
    }

    @Test public void test_position_showOnLeft() {
        ContextMenu cm = createContextMenu(false);
        cm.show(anchorBtn, Side.LEFT, 0, 0);

        Bounds anchorBounds = anchorBtn.localToScreen(anchorBtn.getLayoutBounds());
        Node cmNode = cm.getScene().getRoot();
        Bounds cmBounds = cm.getScene().getRoot().localToScreen(cmNode.getLayoutBounds());

        assertEquals(anchorBounds.getMinX(), cmBounds.getMaxX(), 0.0);
        assertEquals(anchorBounds.getMinY(), cmBounds.getMinY(), 0.0);
    }

    @Test public void test_graphic_padding_onDialogPane() {
        DialogPane dialogPane = new DialogPane();
        anchorBtn.setGraphic(dialogPane);
        // Since DialogPane is not set in a Dialog, PseudoClass is activated manually
        dialogPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("no-header"), true);

        final ImageView graphic = new ImageView(new Image(ContextMenuTest.class.getResource("icon.png").toExternalForm()));
        final MenuItem menuItem = new MenuItem("Menu Item Text", graphic);
        final ContextMenu contextMenu = new ContextMenu(menuItem);
        contextMenu.show(dialogPane, 0, 0);

        final Insets padding = ((StackPane) graphic.getParent()).getPadding();
        final double fontSize = Font.getDefault().getSize();

        // -fx-padding: 0em 0.333em 0em 0em;
        assertEquals(0, padding.getTop(), 0.0);
        assertEquals(0.333 * fontSize, padding.getRight(), 0.01);
        assertEquals(0, padding.getBottom(), 0.0);
        assertEquals(0, padding.getLeft(), 0.0);
        anchorBtn.setGraphic(null);
    }
}
