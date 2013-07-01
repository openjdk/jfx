/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
