/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.cell;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ComboBoxListCellTest {

    private StringConverter<Object> converter;

    @Before public void setup() {
        converter = new StringConverter<Object>() {
            @Override public String toString(Object object) {
                return null;
            }

            @Override public Object fromString(String string) {
                return null;
            }
        };
    }

    /**************************************************************************
     *
     * Test for public static Callback<ListView<T>, ListCell<T>> forListView(T... items)
     *
     **************************************************************************/


    @Test public void testStatic_forListView_noArgs_ensureCellFactoryIsNotNull() {
        Callback<ListView<String>, ListCell<String>> cellFactory = ComboBoxListCell.forListView();
        assertNotNull(cellFactory);
    }

    @Test public void testStatic_forListView_noArgs_ensureCellFactoryCreatesCells() {
        Callback<ListView<String>, ListCell<String>> cellFactory = ComboBoxListCell.forListView();

        ListView<String> listView = new ListView<>();
        ComboBoxListCell<String> cell = (ComboBoxListCell<String>)cellFactory.call(listView);
        assertNotNull(cell);
    }

    @Test public void testStatic_forListView_noArgs_ensureCellHasNonNullStringConverter() {
        Callback<ListView<String>, ListCell<String>> cellFactory = ComboBoxListCell.forListView();

        ListView<String> listView = new ListView<>();
        ComboBoxListCell<String> cell = (ComboBoxListCell<String>)cellFactory.call(listView);
        assertNotNull(cell.getConverter());
    }



    /**************************************************************************
     *
     * Test for public static <T> Callback<ListView<T>, ListCell<T>> forListView(
     *       final ObservableList<T> items)
     *
     **************************************************************************/

    @Test public void testStatic_forListView_items_ensureSuccessWhenItemsIsNull() {
        ObservableList<String> items = null;
        Callback<ListView<String>, ListCell<String>> cellFactory = ComboBoxListCell.forListView(items);
        assertNotNull(cellFactory);
    }

    @Test public void testStatic_forListView_items_ensureCellFactoryIsNotNull() {
        ObservableList<String> items = FXCollections.emptyObservableList();
        Callback<ListView<String>, ListCell<String>> cellFactory = ComboBoxListCell.forListView(items);
        assertNotNull(cellFactory);
    }

    @Test public void testStatic_forListView_items_ensureCellFactoryCreatesCells() {
        ObservableList<String> items = FXCollections.emptyObservableList();
        Callback<ListView<String>, ListCell<String>> cellFactory = ComboBoxListCell.forListView(items);

        ListView<String> listView = new ListView<>();
        ComboBoxListCell<String> cell = (ComboBoxListCell<String>)cellFactory.call(listView);
        assertNotNull(cell);
    }

    @Test public void testStatic_forListView_items_ensureCellHasNonNullStringConverter() {
        ObservableList<String> items = FXCollections.emptyObservableList();
        Callback<ListView<String>, ListCell<String>> cellFactory = ComboBoxListCell.forListView(items);

        ListView<String> listView = new ListView<>();
        ComboBoxListCell<String> cell = (ComboBoxListCell<String>)cellFactory.call(listView);
        assertNotNull(cell.getConverter());
    }

    /**************************************************************************
     *
     * Constructor tests for default constructor
     *
     **************************************************************************/

    @Test public void testConstructor_noArgs_defaultStringConverterIsNotNull() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        assertNotNull(cell.getConverter());
    }

    @Test public void testConstructor_noArgs_defaultStyleClass() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        assertTrue(cell.getStyleClass().contains("combo-box-list-cell"));
    }

    @Test public void testConstructor_noArgs_defaultGraphicIsNull() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        assertNull(cell.getGraphic());
    }


    /**************************************************************************
     *
     * Constructor tests for one-arg constructor
     *
     **************************************************************************/

    @Test public void testConstructor_converter_defaultStringConverterIsNotNull() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>(converter);
        assertNotNull(cell.getConverter());
    }

    @Test public void testConstructor_converter_defaultStyleClass() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>(converter);
        assertTrue(cell.getStyleClass().contains("combo-box-list-cell"));
    }

    @Test public void testConstructor_converter_defaultGraphicIsACheckBox() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>(converter);
        assertNull(cell.getGraphic());
    }


    /**************************************************************************
     *
     * updateItem tests
     *
     **************************************************************************/

    @Test public void test_updateItem_isEmpty_graphicIsNull() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.updateItem("TEST", true);
        assertNull(cell.getGraphic());
    }

    @Test public void test_updateItem_isEmpty_textIsNull() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.updateItem("TEST", true);
        assertNull(cell.getText());
    }

    @Test public void test_updateItem_isNotEmpty_textIsNotNull() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.updateItem("TEST", false);
        assertNotNull(cell.getText());
        assertEquals("TEST", cell.getText());
    }

    @Test public void test_updateItem_isNotEmpty_textIsNotNull_nullConverter() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.setConverter(null);
        cell.updateItem("TEST", false);
        assertNotNull(cell.getText());
        assertEquals("TEST", cell.getText());
    }

    @Test public void test_updateItem_isNotEmpty_textIsNotNull_nonNullConverter() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.setConverter(
                new StringConverter<Object>() {
                    @Override public Object fromString(String string) {
                        return null;
                    }

                    @Override public String toString(Object object) {
                        return "CONVERTED";
                    }
                });
        cell.updateItem("TEST", false);
        assertNotNull(cell.getText());
        assertEquals("CONVERTED", cell.getText());
    }


    /**************************************************************************
     *
     * editing tests
     *
     **************************************************************************/

    // --- is Empty
    @Test public void test_startEdit_cellEditableIsFalse_isEmpty() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.setEditable(false);
        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_listViewEditableIsFalse_isEmpty() {
        ListView listView = new ListView();
        listView.setEditable(false);
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.updateListView(listView);

        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_listViewEditableIsTrue_isEmpty() {
        ListView listView = new ListView();
        listView.setEditable(true);
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.updateListView(listView);

        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_listViewEditableIsTrue_cellEditableIsTrue_isEmpty() {
        ListView listView = new ListView();
        listView.setEditable(true);
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.setEditable(true);
        cell.updateListView(listView);

        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    // --- is Not Empty
    @Test public void test_startEdit_cellEditableIsFalse_isNotEmpty() {
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.updateItem("TEST", false);
        cell.setEditable(false);
        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_listViewEditableIsFalse_isNotEmpty() {
        ListView listView = new ListView();
        listView.setEditable(false);
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.updateListView(listView);
        cell.updateItem("TEST", false);

        cell.startEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_startEdit_listViewEditableIsTrue_isNotEmpty() {
        ListView listView = new ListView();
        listView.setEditable(true);
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.updateListView(listView);
        cell.updateItem("TEST", false);

        cell.startEdit();
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());
    }

    @Test public void test_startEdit_listViewEditableIsTrue_cellEditableIsTrue_isNotEmpty() {
        ListView listView = new ListView();
        listView.setEditable(true);
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.setEditable(true);
        cell.updateListView(listView);
        cell.updateItem("TEST", false);

        cell.startEdit();
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());
    }

    // --- cancel edit
    @Test public void test_cancelEdit() {
        ListView listView = new ListView();
        listView.setEditable(true);
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.updateListView(listView);
        cell.updateItem("TEST", false);

        cell.startEdit();
        assertTrue(cell.isEditing());
        assertNotNull(cell.getGraphic());

        cell.cancelEdit();
        assertFalse(cell.isEditing());
        assertNull(cell.getGraphic());
    }

    @Test public void test_rt_29320() {
        ListView listView = new ListView();
        listView.setEditable(true);
        ComboBoxListCell<Object> cell = new ComboBoxListCell<>();
        cell.updateListView(listView);
        cell.updateItem("TEST", false);
        cell.setEditable(true);

        cell.startEdit();
        ComboBox cb = (ComboBox) cell.getGraphic();

        // initially the choiceBox converter should equal the cell converter
        assertNotNull(cell.getConverter());
        assertNotNull(cb.getConverter());
        assertEquals(cell.getConverter(), cb.getConverter());

        // and if the cell changes the choicebox should follow
        cell.setConverter(null);
        assertNull(cb.getConverter());

        StringConverter<Object> customConverter = new StringConverter<Object>() {
            @Override public String toString(Object object) { return null; }
            @Override public Object fromString(String string) { return null; }
        };
        cell.setConverter(customConverter);
        assertEquals(customConverter, cb.getConverter());
    }
}
