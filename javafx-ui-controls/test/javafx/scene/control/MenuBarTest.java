/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;


/**
 *
 * @author lubermud
 */
public class MenuBarTest {
    private MenuBar menuBar;

    @Before public void setup() {
        menuBar = new MenuBar();
    }

    @Test public void defaultConstructorHasFalseFocusTraversable() {
        assertFalse(menuBar.isFocusTraversable());
    }

    @Test public void defaultConstructorButSetTrueFocusTraversable() {
        menuBar.setFocusTraversable(true);
        assertTrue(menuBar.isFocusTraversable());
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

}
