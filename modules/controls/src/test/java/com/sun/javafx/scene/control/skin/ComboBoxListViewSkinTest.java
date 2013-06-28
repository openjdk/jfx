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
