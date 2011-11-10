/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author lubermud
 */
public class ContextMenuTest {
    private MenuItem menuItem1, menuItem2, menuItem3;

    private ContextMenu contextMenu;
    private ContextMenu contextMenuWithOneItem;
    private ContextMenu contextMenuWithManyItems;

    @Before public void setup() {
        menuItem1 = new MenuItem(); 
        menuItem2 = new MenuItem(); 
        menuItem3 = new MenuItem();

        contextMenu = new ContextMenu();
        contextMenuWithOneItem = new ContextMenu(menuItem1);
        contextMenuWithManyItems = new ContextMenu(menuItem1, menuItem2, menuItem3);
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

    public static final class EventHandlerStub implements EventHandler<ActionEvent> {
        boolean called = false;
        @Override public void handle(ActionEvent event) {
            called = true;
        }
    };

    //TODO: test show/hide


}
