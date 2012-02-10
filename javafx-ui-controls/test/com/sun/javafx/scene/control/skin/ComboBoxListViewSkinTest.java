/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.*;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ComboBoxListViewSkinTest {
    private ComboBox<String> comboBox;
    private SelectionModel<String> sm;
    private ListView<String> listView;
    private SelectionModel<String> listSm;
    private ComboBoxListViewSkin<String> skin;

    @Before public void setup() {
        comboBox = new ComboBox();
        skin = new ComboBoxListViewSkin(comboBox);
        comboBox.setSkin(skin);
        
        sm = comboBox.getSelectionModel();
        listView = skin.getListView();
        listSm = listView.getSelectionModel();
    }
    
    @Test public void testListViewSelectionEqualsComboBox() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select("Orange");
        assertEquals("Orange", comboBox.getValue());
        assertEquals("Orange", sm.getSelectedItem());
        assertEquals("Orange", listSm.getSelectedItem());
    }
    
    @Test public void test_rt19431_ListViewSelectionIsNullWhenComBoxChangesEditableProperty() {
        comboBox.getItems().addAll("Apple", "Orange", "Banana");
        sm.select("Orange");
        comboBox.setEditable(true);
        assertNull(comboBox.getValue());
        assertNull(sm.getSelectedItem());
        assertNull(listSm.getSelectedItem());
    }
    
}
