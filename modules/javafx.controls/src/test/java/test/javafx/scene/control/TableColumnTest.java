/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javafx.scene.control.CellShim;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnShim;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;

import test.com.sun.javafx.scene.control.test.Person;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class TableColumnTest {
    private static final double MIN_WIDTH = 35;
    private static final double MAX_WIDTH = 2000;
    private static final double PREF_WIDTH = 100;

    private TableColumn<Person,String> column;
    private TableView<Person> table;
    private ObservableList<Person> model;

    @Before public void setup() {
        column = new TableColumn<Person,String>("");
        model = FXCollections.observableArrayList(
                new Person("Humphrey McPhee", 76),
                new Person("Justice Caldwell", 30),
                new Person("Orrin Davies", 30),
                new Person("Emma Wilson", 8)
        );
        table = new TableView<Person>(model);
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void defaultConstructorHasDefaultCellFactory() {
        assertSame(TableColumn.DEFAULT_CELL_FACTORY, column.getCellFactory());
        assertSame(TableColumn.DEFAULT_CELL_FACTORY, column.cellFactoryProperty().get());
    }

    @Test public void defaultConstructorHasDefaultComparator() {
        assertSame(TableColumn.DEFAULT_COMPARATOR, column.getComparator());
        assertSame(TableColumn.DEFAULT_COMPARATOR, column.comparatorProperty().get());
    }

    @Test public void defaultConstructorHasEmptyStringText() {
        assertEquals("", column.getText());
        assertEquals("", column.textProperty().get());
    }

//    @Test public void defaultConstructorHasNonNullEditCommitHandler() {
//        // This is questionable
//    }

    /*********************************************************************
     * Tests for the tableView property                                  *
     ********************************************************************/

    @Test public void tableViewIsNullByDefault() {
        assertNull(column.getTableView());
        assertNull(column.tableViewProperty().get());
    }

    @Test public void tableViewCanBeSpecified() {
        TableColumnShim.setTableView(column, table);
        assertSame(table, column.getTableView());
        assertSame(table, column.tableViewProperty().get());
    }

    @Test public void tableViewCanBeResetToNull() {
        TableColumnShim.setTableView(column, table);
        TableColumnShim.setTableView(column, null);
        assertNull(column.getTableView());
        assertNull(column.tableViewProperty().get());
    }

    @Test public void tableViewPropertyBeanIsCorrect() {
        assertSame(column, column.tableViewProperty().getBean());
    }

    @Test public void tableViewPropertyNameIsCorrect() {
        assertEquals("tableView", column.tableViewProperty().getName());
    }

    @Test public void whenTableViewIsChangedChildColumnsAreUpdated() {
        TableColumn<Person,String> child = new TableColumn<Person,String>();
        column.getColumns().add(child);
        table.getColumns().add(column);

        TableView<Person> other = new TableView<Person>();
        table.getColumns().clear();
        other.getColumns().add(column);

        assertSame(other, child.getTableView());
    }

    /*********************************************************************
     * Tests for the text property                                       *
     ********************************************************************/

    @Test public void textIsEmptyByDefault() {
        assertEquals("", column.getText());
        assertEquals("", column.textProperty().get());
    }

    @Test public void textCanBeSpecified() {
        column.setText("Name");
        assertEquals("Name", column.getText());
        assertEquals("Name", column.textProperty().get());
    }

    @Test public void textCanBeSetToNull() {
        column.setText(null);
        assertNull(column.getText());
        assertNull(column.textProperty().get());
    }

    @Test public void textPropertyBeanIsCorrect() {
        assertSame(column, column.textProperty().getBean());
    }

    @Test public void textPropertyNameIsCorrect() {
        assertEquals("text", column.textProperty().getName());
    }

    @Test public void textCanBeBound() {
        StringProperty other = new SimpleStringProperty("Name");
        column.textProperty().bind(other);
        assertEquals("Name", column.getText());
        assertEquals("Name", column.textProperty().get());
        other.set("Age");
        assertEquals("Age", column.getText());
        assertEquals("Age", column.textProperty().get());
    }

    /*********************************************************************
     * Tests for the visible property                                    *
     ********************************************************************/

    @Test public void visibleIsTrueByDefault() {
        assertTrue(column.isVisible());
        assertTrue(column.visibleProperty().get());
    }

    @Test public void visibleCanBeSpecified() {
        column.setVisible(false);
        assertFalse(column.isVisible());
        assertFalse(column.visibleProperty().get());
    }

    @Test public void visiblePropertyBeanIsCorrect() {
        assertSame(column, column.visibleProperty().getBean());
    }

    @Test public void visiblePropertyNameIsCorrect() {
        assertEquals("visible", column.visibleProperty().getName());
    }

    @Test public void visibleCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(false);
        column.visibleProperty().bind(other);
        assertFalse(column.isVisible());
        assertFalse(column.visibleProperty().get());
        other.set(true);
        assertTrue(column.isVisible());
        assertTrue(column.visibleProperty().get());
    }

    /*********************************************************************
     * Tests for the parentColumn property                               *
     ********************************************************************/

    @Test public void parentColumnIsNullByDefault() {
        TableColumn child = new TableColumn();
        assertNull(child.getParentColumn());
        assertNull(child.parentColumnProperty().get());
    }

    @Test public void parentColumnIsUpdatedWhenAddedToParent() {
        TableColumn child = new TableColumn();
        column.getColumns().add(child);
        assertSame(column, child.getParentColumn());
        assertSame(column, child.parentColumnProperty().get());
    }

    @Test public void parentColumnIsClearedWhenRemovedFromParent() {
        TableColumn child = new TableColumn();
        column.getColumns().add(child);
        column.getColumns().remove(child);
        assertNull(child.getParentColumn());
        assertNull(child.parentColumnProperty().get());
    }

    @Test public void parentColumnIsClearedWhenParentColumnsIsCleared() {
        TableColumn child = new TableColumn();
        column.getColumns().add(child);
        column.getColumns().clear();
        assertNull(child.getParentColumn());
        assertNull(child.parentColumnProperty().get());
    }

    @Test public void tableViewReferenceIsUpdatedWhenAddedToParent() {
        table.getColumns().add(column);
        TableColumn child = new TableColumn();
        column.getColumns().add(child);
        assertSame(table, child.getTableView());
        assertSame(table, child.tableViewProperty().get());
    }

    @Test public void tableViewReferenceIsClearedWhenRemovedFromParent() {
        table.getColumns().add(column);
        TableColumn child = new TableColumn();
        column.getColumns().add(child);
        column.getColumns().remove(child);
        assertNull(child.getTableView());
        assertNull(child.tableViewProperty().get());
    }

    @Test public void tableViewReferenceIsClearedWhenParentColumnsIsCleared() {
        table.getColumns().add(column);
        TableColumn child = new TableColumn();
        column.getColumns().add(child);
        column.getColumns().clear();
        assertNull(child.getTableView());
        assertNull(child.tableViewProperty().get());
    }

    @Test public void visibleIsUpdatedWhenParentColumnVisibleChanges() {
        TableColumn child = new TableColumn();
        column.getColumns().add(child);
        column.setVisible(true);
        assertTrue(child.visibleProperty().get());
        column.setVisible(false);
        assertFalse(child.isVisible());
    }

    @Test public void visibleIsTrueWhenAddedToParentColumnWithVisibleTrue() {
        TableColumn child = new TableColumn();
        column.setVisible(true);
        column.getColumns().add(child);
        assertTrue(child.isVisible());
        assertTrue(child.visibleProperty().get());
    }

    @Test public void visibleIsNotChangedWhenAddedToParentColumnWithVisibleFalse() {
        TableColumn child = new TableColumn();
        child.setVisible(true);
        column.setVisible(false);
        column.getColumns().add(child);
        assertTrue(child.isVisible());
        assertFalse(column.isVisible());
    }

    @Test public void visibleIsNotChangedWhenRemovedFromParentColumn() {
        TableColumn child = new TableColumn();
        column.getColumns().add(child);
        column.setVisible(false);
        column.getColumns().clear();
        assertFalse(child.isVisible());
        assertFalse(child.visibleProperty().get());
    }

    @Test public void childVisibleChangesAccordingToParentVisibleWhenParentVisibleIsBound() {
        TableColumn<Person,String> child = new TableColumn<Person,String>();
        column.getColumns().add(child);
        BooleanProperty other = new SimpleBooleanProperty(false);
        column.visibleProperty().bind(other);
        assertFalse(child.isVisible());
        assertFalse(child.visibleProperty().get());
        other.set(true);
        assertTrue(child.isVisible());
        assertTrue(child.visibleProperty().get());
    }

    /*********************************************************************
     * Tests for the contextMenu property                                *
     ********************************************************************/

    @Test public void contextMenuIsNullByDefault() {
        assertNull(column.getContextMenu());
        assertNull(column.contextMenuProperty().get());
    }

    @Test public void contextMenuCanBeSpecified() {
        ContextMenu ctx = new ContextMenu();
        column.setContextMenu(ctx);
        assertSame(ctx, column.getContextMenu());
        assertSame(ctx, column.contextMenuProperty().get());
    }

    @Test public void contextMenuCanBeSetBackToNull() {
        ContextMenu ctx = new ContextMenu();
        column.setContextMenu(ctx);
        column.setContextMenu(null);
        assertNull(column.getContextMenu());
        assertNull(column.contextMenuProperty().get());
    }

    @Test public void contextMenuPropertyBeanIsCorrect() {
        assertSame(column, column.contextMenuProperty().getBean());
    }

    @Test public void contextMenuPropertyNameIsCorrect() {
        assertEquals("contextMenu", column.contextMenuProperty().getName());
    }

    @Test public void contextMenuCanBeBound() {
        ContextMenu ctx = new ContextMenu();
        ObjectProperty<ContextMenu> other = new SimpleObjectProperty<ContextMenu>(ctx);
        column.contextMenuProperty().bind(other);
        assertSame(ctx, column.getContextMenu());
        assertSame(ctx, column.contextMenuProperty().get());
        other.set(null);
        assertNull(column.getContextMenu());
        assertNull(column.contextMenuProperty().get());
    }

    /*********************************************************************
     * Tests for the cellValueFactory property                           *
     ********************************************************************/

    @Test public void cellValueFactoryIsNullByDefault() {
        assertNull(column.getCellValueFactory());
        assertNull(column.cellValueFactoryProperty().get());
    }

    @Test public void cellValueFactoryCanBeSpecified() {
        CellValueFactory<Person,String> factory = param -> param.getValue().firstNameProperty();

        column.setCellValueFactory(factory);
        assertSame(factory, column.getCellValueFactory());
        assertSame(factory, column.cellValueFactoryProperty().get());
    }

    @Test public void cellValueFactoryCanBeResetToNull() {
        CellValueFactory<Person,String> factory = param -> param.getValue().firstNameProperty();

        column.setCellValueFactory(factory);
        column.setCellValueFactory(null);
        assertNull(column.getCellValueFactory());
        assertNull(column.cellValueFactoryProperty().get());
    }

    @Test public void cellValueFactoryPropertyBeanIsCorrect() {
        assertSame(column, column.cellValueFactoryProperty().getBean());
    }

    @Test public void cellValueFactoryPropertyNameIsCorrect() {
        assertEquals("cellValueFactory", column.cellValueFactoryProperty().getName());
    }

    @Test public void cellValueFactoryCanBeBound() {
        CellValueFactory<Person,String> factory = param -> param.getValue().firstNameProperty();
        ObjectProperty<CellValueFactory<Person,String>> other =
                new SimpleObjectProperty<CellValueFactory<Person, String>>(factory);
        column.cellValueFactoryProperty().bind(other);
        assertSame(factory, column.getCellValueFactory());
        assertSame(factory, column.cellValueFactoryProperty().get());
        other.set(null);
        assertNull(column.getCellValueFactory());
        assertNull(column.cellValueFactoryProperty().get());
    }

    /*********************************************************************
     * Tests for the cellFactory property                                *
     ********************************************************************/

    @Test public void cellFactoryCanBeSpecified() {
        CellFactory<Person,String> factory = param -> null;
        column.setCellFactory(factory);
        assertSame(factory, column.getCellFactory());
        assertSame(factory, column.cellFactoryProperty().get());
    }

    @Test public void cellFactoryCanBeResetToNull() {
        CellFactory<Person,String> factory = param -> null;
        column.setCellFactory(factory);
        column.setCellFactory(null);
        assertNull(column.getCellFactory());
        assertNull(column.cellFactoryProperty().get());
    }

    @Test public void cellFactoryPropertyBeanIsCorrect() {
        assertSame(column, column.cellFactoryProperty().getBean());
    }

    @Test public void cellFactoryPropertyNameIsCorrect() {
        assertEquals("cellFactory", column.cellFactoryProperty().getName());
    }

    @Test public void cellFactoryCanBeBound() {
        CellFactory<Person,String> factory = param -> null;
        ObjectProperty<CellFactory<Person,String>> other =
                new SimpleObjectProperty<CellFactory<Person, String>>(factory);
        column.cellFactoryProperty().bind(other);
        assertSame(factory, column.getCellFactory());
        assertSame(factory, column.cellFactoryProperty().get());
        other.set(null);
        assertNull(column.getCellFactory());
        assertNull(column.cellFactoryProperty().get());
    }

    /****************************************************************************
     * minWidth Tests                                                           *
     ***************************************************************************/

    @Test public void minWidthIs_DEFAULT_MIN_WIDTH_ByDefault() {
        assertEquals(TableColumnShim.DEFAULT_MIN_WIDTH, column.getMinWidth(), 0);
        assertEquals(TableColumnShim.DEFAULT_MIN_WIDTH, column.minWidthProperty().get(), 0);
    }

    @Test public void minWidthCanBeSet() {
        column.setMinWidth(234);
        assertEquals(234, column.getMinWidth(), 0);
        assertEquals(234, column.minWidthProperty().get(), 0);
    }

    @Test public void minWidthCanBeBound() {
        DoubleProperty other = new SimpleDoubleProperty(939);
        column.minWidthProperty().bind(other);
        assertEquals(939, column.getMinWidth(), 0);
        other.set(332);
        assertEquals(332, column.getMinWidth(), 0);
    }

    @Test public void minWidthPropertyHasBeanReference() {
        assertSame(column, column.minWidthProperty().getBean());
    }

    @Test public void minWidthPropertyHasName() {
        assertEquals("minWidth", column.minWidthProperty().getName());
    }

    /****************************************************************************
     * maxWidth Tests                                                           *
     ***************************************************************************/

    @Test public void maxWidthIs_DEFAULT_MAX_WIDTH_ByDefault() {
        assertEquals(TableColumnShim.DEFAULT_MAX_WIDTH, column.getMaxWidth(), 0);
        assertEquals(TableColumnShim.DEFAULT_MAX_WIDTH, column.maxWidthProperty().get(), 0);
    }

    @Test public void maxWidthCanBeSet() {
        column.setMaxWidth(500);
        assertEquals(500, column.getMaxWidth(), 0);
        assertEquals(500, column.maxWidthProperty().get(), 0);
    }

    @Test public void maxWidthCanBeBound() {
        DoubleProperty other = new SimpleDoubleProperty(939);
        column.maxWidthProperty().bind(other);
        assertEquals(939, column.getMaxWidth(), 0);
        other.set(332);
        assertEquals(332, column.getMaxWidth(), 0);
    }

    @Test public void maxWidthPropertyHasBeanReference() {
        assertSame(column, column.maxWidthProperty().getBean());
    }

    @Test public void maxWidthPropertyHasName() {
        assertEquals("maxWidth", column.maxWidthProperty().getName());
    }

    /****************************************************************************
     * prefWidth Tests                                                          *
     ***************************************************************************/

    @Test public void prefWidthIs_DEFAULT_WIDTH_ByDefault() {
        assertEquals(TableColumnShim.DEFAULT_WIDTH, column.getPrefWidth(), 0);
        assertEquals(TableColumnShim.DEFAULT_WIDTH, column.prefWidthProperty().get(), 0);
    }

    @Test public void prefWidthCanBeSet() {
        column.setPrefWidth(80);
        assertEquals(80, column.getPrefWidth(), 0);
        assertEquals(80, column.prefWidthProperty().get(), 0);
    }

    @Test public void prefWidthCanBeBound() {
        DoubleProperty other = new SimpleDoubleProperty(939);
        column.prefWidthProperty().bind(other);
        assertEquals(939, column.getPrefWidth(), 0);
        other.set(332);
        assertEquals(332, column.getPrefWidth(), 0);
    }

    @Test public void prefWidthPropertyHasBeanReference() {
        assertSame(column, column.prefWidthProperty().getBean());
    }

    @Test public void prefWidthPropertyHasName() {
        assertEquals("prefWidth", column.prefWidthProperty().getName());
    }

    /*********************************************************************
     * Tests for the resizable property                                  *
     ********************************************************************/

    @Test public void resizableIsTrueByDefault() {
        assertTrue(column.isResizable());
        assertTrue(column.resizableProperty().get());
    }

    @Test public void resizableCanBeSpecified() {
        column.setResizable(false);
        assertFalse(column.isResizable());
        assertFalse(column.resizableProperty().get());
    }

    @Test public void resizablePropertyBeanIsCorrect() {
        assertSame(column, column.resizableProperty().getBean());
    }

    @Test public void resizablePropertyNameIsCorrect() {
        assertEquals("resizable", column.resizableProperty().getName());
    }

    @Test public void resizableCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(false);
        column.resizableProperty().bind(other);
        assertFalse(column.isResizable());
        assertFalse(column.resizableProperty().get());
        other.set(true);
        assertTrue(column.isResizable());
        assertTrue(column.resizableProperty().get());
    }

    /*********************************************************************
     * Tests for the sortable property                                   *
     ********************************************************************/

    @Test public void sortableIsTrueByDefault() {
        assertTrue(column.isSortable());
        assertTrue(column.sortableProperty().get());
    }

    @Test public void sortableCanBeSpecified() {
        column.setSortable(false);
        assertFalse(column.isSortable());
        assertFalse(column.sortableProperty().get());
    }

    @Test public void sortablePropertyBeanIsCorrect() {
        assertSame(column, column.sortableProperty().getBean());
    }

    @Test public void sortablePropertyNameIsCorrect() {
        assertEquals("sortable", column.sortableProperty().getName());
    }

    @Test public void sortableCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(false);
        column.sortableProperty().bind(other);
        assertFalse(column.isSortable());
        assertFalse(column.sortableProperty().get());
        other.set(true);
        assertTrue(column.isSortable());
        assertTrue(column.sortableProperty().get());
    }

    /*********************************************************************
     * Tests for the sortType property                                   *
     ********************************************************************/

    @Test public void sortTypeIsASCENDINGByDefault() {
        assertSame(TableColumn.SortType.ASCENDING, column.getSortType());
        assertSame(TableColumn.SortType.ASCENDING, column.sortTypeProperty().get());
    }

    @Test public void sortTypeCanBeSpecified() {
        column.setSortType(TableColumn.SortType.DESCENDING);
        assertSame(TableColumn.SortType.DESCENDING, column.getSortType());
        assertSame(TableColumn.SortType.DESCENDING, column.sortTypeProperty().get());
    }

    @Test public void sortTypeCanBeNull() {
        column.setSortType(null);
        assertNull(column.getSortType());
        assertNull(column.sortTypeProperty().get());
    }

    @Test public void sortTypePropertyBeanIsCorrect() {
        assertSame(column, column.sortTypeProperty().getBean());
    }

    @Test public void sortTypePropertyNameIsCorrect() {
        assertEquals("sortType", column.sortTypeProperty().getName());
    }

    @Test public void sortTypeCanBeBound() {
        ObjectProperty<TableColumn.SortType> other =
                new SimpleObjectProperty<TableColumn.SortType>(TableColumn.SortType.DESCENDING);
        column.sortTypeProperty().bind(other);
        assertSame(TableColumn.SortType.DESCENDING, column.getSortType());
        assertSame(TableColumn.SortType.DESCENDING, column.sortTypeProperty().get());
        other.set(null);
        assertNull(column.getSortType());
        assertNull(column.sortTypeProperty().get());
    }

    /*********************************************************************
     * Tests for the editable property                                   *
     ********************************************************************/

    @Test public void editableIsTrueByDefault() {
        assertTrue(column.isEditable());
        assertTrue(column.editableProperty().get());
    }

    @Test public void editableCanBeSpecified() {
        column.setEditable(false);
        assertFalse(column.isEditable());
        assertFalse(column.editableProperty().get());
    }

    @Test public void editablePropertyBeanIsCorrect() {
        assertSame(column, column.editableProperty().getBean());
    }

    @Test public void editablePropertyNameIsCorrect() {
        assertEquals("editable", column.editableProperty().getName());
    }

    @Test public void editableCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(false);
        column.editableProperty().bind(other);
        assertFalse(column.isEditable());
        assertFalse(column.editableProperty().get());
        other.set(true);
        assertTrue(column.isEditable());
        assertTrue(column.editableProperty().get());
    }

    /*********************************************************************
     * Tests for the reorderable property                                *
     ********************************************************************/

    @Test public void reorderableIsTrueByDefault() {
        assertTrue(column.isReorderable());
        assertTrue(column.reorderableProperty().get());
    }

    @Test public void reorderableCanBeSpecified() {
        column.setReorderable(false);
        assertFalse(column.isReorderable());
        assertFalse(column.reorderableProperty().get());
    }

    @Test public void reorderablePropertyBeanIsCorrect() {
        assertSame(column, column.reorderableProperty().getBean());
    }

    @Test public void reorderablePropertyNameIsCorrect() {
        assertEquals("reorderable", column.reorderableProperty().getName());
    }

    @Test public void reorderableCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(false);
        column.reorderableProperty().bind(other);
        assertFalse(column.isReorderable());
        assertFalse(column.reorderableProperty().get());
        other.set(true);
        assertTrue(column.isReorderable());
        assertTrue(column.reorderableProperty().get());
    }

    /*********************************************************************
     * Tests for the comparator property                                 *
     ********************************************************************/

    @Test public void comparatorCanBeSpecified() {
        Comparator<String> comparator = (o1, o2) -> o1.compareTo(o2);
        column.setComparator(comparator);
        assertSame(comparator, column.getComparator());
        assertSame(comparator, column.comparatorProperty().get());
    }

    @Test public void comparatorCanBeResetToNull() {
        Comparator<String> comparator = (o1, o2) -> o1.compareTo(o2);
        column.setComparator(comparator);
        column.setComparator(null);
        assertNull(column.getComparator());
        assertNull(column.comparatorProperty().get());
    }

    @Test public void comparatorPropertyBeanIsCorrect() {
        assertSame(column, column.comparatorProperty().getBean());
    }

    @Test public void comparatorPropertyNameIsCorrect() {
        assertEquals("comparator", column.comparatorProperty().getName());
    }

    @Test public void comparatorCanBeBound() {
        Comparator<String> comparator = (o1, o2) -> o1.compareTo(o2);
        ObjectProperty<Comparator<String>> other =
                new SimpleObjectProperty<Comparator<String>>(comparator);
        column.comparatorProperty().bind(other);
        assertSame(comparator, column.getComparator());
        assertSame(comparator, column.comparatorProperty().get());
        other.set(null);
        assertNull(column.getComparator());
        assertNull(column.comparatorProperty().get());
    }

    /*********************************************************************
     * Tests for the onEditStart property                                *
     ********************************************************************/

    @Test public void onEditStartIsNullByDefault() {
        assertNull(column.getOnEditStart());
        assertNull(column.onEditStartProperty().get());
    }

    @Test public void onEditStartCanBeSpecified() {
        EventHandler<TableColumn.CellEditEvent<Person,String>> handler =
                event -> {
                };
        column.setOnEditStart(handler);
        assertSame(handler, column.getOnEditStart());
        assertSame(handler, column.onEditStartProperty().get());
    }

    @Test public void onEditStartCanBeResetToNull() {
        EventHandler<TableColumn.CellEditEvent<Person,String>> handler =
                event -> {
                };
        column.setOnEditStart(handler);
        column.setOnEditStart(null);
        assertNull(column.getOnEditStart());
        assertNull(column.onEditStartProperty().get());
    }

    @Test public void onEditStartPropertyBeanIsCorrect() {
        assertSame(column, column.onEditStartProperty().getBean());
    }

    @Test public void onEditStartPropertyNameIsCorrect() {
        assertEquals("onEditStart", column.onEditStartProperty().getName());
    }

    @Test public void onEditStartCanBeBound() {
        EventHandler<TableColumn.CellEditEvent<Person,String>> handler =
                event -> {
                };
        ObjectProperty<EventHandler<TableColumn.CellEditEvent<Person,String>>> other =
                new SimpleObjectProperty<EventHandler<TableColumn.CellEditEvent<Person, String>>>(handler);
        column.onEditStartProperty().bind(other);
        assertSame(handler, column.getOnEditStart());
        assertSame(handler, column.onEditStartProperty().get());
        other.set(null);
        assertNull(column.getOnEditStart());
        assertNull(column.onEditStartProperty().get());
    }

    /*********************************************************************
     * Tests for the onEditCancel property                               *
     ********************************************************************/

    @Test public void onEditCancelIsNullByDefault() {
        assertNull(column.getOnEditCancel());
        assertNull(column.onEditCancelProperty().get());
    }

    @Test public void onEditCancelCanBeSpecified() {
        EventHandler<TableColumn.CellEditEvent<Person,String>> handler =
                event -> {
                };
        column.setOnEditCancel(handler);
        assertSame(handler, column.getOnEditCancel());
        assertSame(handler, column.onEditCancelProperty().get());
    }

    @Test public void onEditCancelCanBeResetToNull() {
        EventHandler<TableColumn.CellEditEvent<Person,String>> handler =
                event -> {
                };
        column.setOnEditCancel(handler);
        column.setOnEditCancel(null);
        assertNull(column.getOnEditCancel());
        assertNull(column.onEditCancelProperty().get());
    }

    @Test public void onEditCancelPropertyBeanIsCorrect() {
        assertSame(column, column.onEditCancelProperty().getBean());
    }

    @Test public void onEditCancelPropertyNameIsCorrect() {
        assertEquals("onEditCancel", column.onEditCancelProperty().getName());
    }

    @Test public void onEditCancelCanBeBound() {
        EventHandler<TableColumn.CellEditEvent<Person,String>> handler =
                event -> {
                };
        ObjectProperty<EventHandler<TableColumn.CellEditEvent<Person,String>>> other =
                new SimpleObjectProperty<EventHandler<TableColumn.CellEditEvent<Person, String>>>(handler);
        column.onEditCancelProperty().bind(other);
        assertSame(handler, column.getOnEditCancel());
        assertSame(handler, column.onEditCancelProperty().get());
        other.set(null);
        assertNull(column.getOnEditCancel());
        assertNull(column.onEditCancelProperty().get());
    }

    /*********************************************************************
     * Tests for the onEditCommit property                               *
     ********************************************************************/

//    @Test public void onEditCommitIsNullByDefault() {
//        assertNull(column.getOnEditCommit());
//        assertNull(column.onEditCommitProperty().get());
//    }

    @Test public void onEditCommitCanBeSpecified() {
        EventHandler<TableColumn.CellEditEvent<Person,String>> handler =
                event -> {
                };
        column.setOnEditCommit(handler);
        assertSame(handler, column.getOnEditCommit());
        assertSame(handler, column.onEditCommitProperty().get());
    }

    @Test public void onEditCommitCanBeResetToNull() {
        EventHandler<TableColumn.CellEditEvent<Person,String>> handler =
                event -> {
                };
        column.setOnEditCommit(handler);
        column.setOnEditCommit(null);
        assertNull(column.getOnEditCommit());
        assertNull(column.onEditCommitProperty().get());
    }

    @Test public void onEditCommitPropertyBeanIsCorrect() {
        assertSame(column, column.onEditCommitProperty().getBean());
    }

    @Test public void onEditCommitPropertyNameIsCorrect() {
        assertEquals("onEditCommit", column.onEditCommitProperty().getName());
    }

    @Test public void onEditCommitCanBeBound() {
        EventHandler<TableColumn.CellEditEvent<Person,String>> handler =
                event -> {
                };
        ObjectProperty<EventHandler<TableColumn.CellEditEvent<Person,String>>> other =
                new SimpleObjectProperty<EventHandler<TableColumn.CellEditEvent<Person, String>>>(handler);
        column.onEditCommitProperty().bind(other);
        assertSame(handler, column.getOnEditCommit());
        assertSame(handler, column.onEditCommitProperty().get());
        other.set(null);
        assertNull(column.getOnEditCommit());
        assertNull(column.onEditCommitProperty().get());
    }

    /*********************************************************************
     * Tests for getCellData                                             *
     ********************************************************************/

    @Test public void getCellDataReturnsNullWhenTableViewIsNull() {
        assertNull(column.getCellData(0));
    }

    @Test public void getCellDataReturnsNullWhenTableViewIsNull2() {
        assertNull(column.getCellData(table.getItems().get(1)));
    }

    @Test public void getCellDataReturnsNullWhenTableViewItemsIsNull() {
        table.getColumns().add(column);
        table.setItems(null);
        assertNull(column.getCellData(0));
    }

    @Test public void getCellDataReturnsNullWhenTableViewItemsIsNull2() {
        final Person person = table.getItems().get(1);
        table.getColumns().add(column);
        table.setItems(null);
        assertNull(column.getCellData(person));
    }

    @Test public void getCellDataReturnsNullWhenRowDataIsNullByDefault() {
        table.getColumns().add(column);
        table.getItems().set(0, null);
        assertNull(column.getCellData(0));
    }

    @Test public void getCellDataReturnsNullWhenRowDataIsNullByDefault2() {
        table.getColumns().add(column);
        assertNull(column.getCellData(null));
    }

    @Test public void getCellDataReturnsNullWhenIndexIsNegative() {
        table.getColumns().add(column);
        assertNull(column.getCellData(-1));
    }

    @Test public void getCellDataReturnsNullWhenIndexIsTooLarge() {
        table.getColumns().add(column);
        assertNull(column.getCellData(table.getItems().size()));
    }

    @Test public void getCellDataReturnsNullWhenCellValueFactoryIsNull() {
        table.getColumns().add(column);
        assertNull(column.getCellData(0));
    }

    @Test public void getCellDataReturnsNullWhenCellValueFactoryIsNull2() {
        table.getColumns().add(column);
        assertNull(column.getCellData(table.getItems().get(1)));
    }

    @Test public void getCellDataReturnsValue() {
        table.getColumns().add(column);
        column.setCellValueFactory(param -> param.getValue().firstNameProperty());
        assertEquals("Humphrey McPhee", column.getCellData(0));
    }

    @Test public void getCellDataReturnsValue2() {
        table.getColumns().add(column);
        column.setCellValueFactory(param -> param.getValue().firstNameProperty());
        assertEquals("Humphrey McPhee", column.getCellData(table.getItems().get(0)));
    }

    @Test public void cellDataFeaturesHasTableViewSpecified() {
        final boolean[] passed = new boolean[] { false };
        table.getColumns().add(column);
        column.setCellValueFactory(param -> {
            passed[0] = param.getTableView() == table;
            return param.getValue().firstNameProperty();
        });
        column.getCellData(table.getItems().get(0));
        assertTrue(passed[0]);
    }

    @Test public void cellDataFeaturesHasTableColumnSpecified() {
        final boolean[] passed = new boolean[] { false };
        table.getColumns().add(column);
        column.setCellValueFactory(param -> {
            passed[0] = param.getTableColumn() == column;
            return param.getValue().firstNameProperty();
        });
        column.getCellData(table.getItems().get(0));
        assertTrue(passed[0]);
    }

    @Test public void cellDataFeaturesHasRowItemSpecified() {
        final boolean[] passed = new boolean[] { false };
        table.getColumns().add(column);
        column.setCellValueFactory(param -> {
            passed[0] = param.getValue() == table.getItems().get(0);
            return param.getValue().firstNameProperty();
        });
        column.getCellData(table.getItems().get(0));
        assertTrue(passed[0]);
    }

    /*********************************************************************
     * Tests for the DEFAULT_EDIT_COMMIT_HANDLER. By default each        *
     * TableColumn has a DEFAULT_EDIT_COMMIT_HANDLER installed which     *
     * will, if the value returned by cellValueFactory is a              *
     * WritableValue, commit. If not, then nothing happens.              *
     ********************************************************************/

    @Test public void onEditCommitHandlerInstalledByDefault() {
        assertNotNull(column.getOnEditCommit());
    }

    @Test public void defaultOnEditCommitHandlerWillSaveToWritableValue() {
        table.getColumns().add(column);
        column.setCellValueFactory(param -> param.getValue().firstNameProperty());
        TablePosition<Person,String> pos = new TablePosition<Person, String>(table, 0, column);
        EventType<TableColumn.CellEditEvent<Person,String>> eventType = TableColumn.editCommitEvent();
        column.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person,String>(
                table, pos, (EventType)eventType, "Richard Bair"));
        assertEquals("Richard Bair", table.getItems().get(0).getFirstName());
    }

    @Test public void defaultOnEditCommitHandlerWillIgnoreReadOnlyValue() {
        TableColumn<Person,Number> ageColumn = new TableColumn<Person,Number>();
        table.getColumns().add(ageColumn);
        ageColumn.setCellValueFactory(param -> param.getValue().ageProperty());
        TablePosition<Person,Number> pos = new TablePosition<Person, Number>(table, 0, ageColumn);
        EventType<TableColumn.CellEditEvent<Person,Number>> eventType = TableColumn.editCommitEvent();
        ageColumn.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person,Number>(
                table, pos, (EventType)eventType, 109));
        assertEquals(76, table.getItems().get(0).getAge());
    }

    @Test(expected=NullPointerException.class)
    public void defaultOnEditCommitHandlerDealsWithNullTableView() {
        table.getColumns().add(column);
        column.setCellValueFactory(param -> param.getValue().firstNameProperty());
        TablePosition<Person,String> pos = new TablePosition<Person, String>(table, 0, column);
        EventType<TableColumn.CellEditEvent<Person,String>> eventType = TableColumn.editCommitEvent();
        column.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person, String>(
                null, pos, (EventType) eventType, "Richard Bair"));
    }

    @Test(expected=NullPointerException.class)
    public void defaultOnEditCommitHandlerDealsWithNullTablePosition() {
        table.getColumns().add(column);
        column.setCellValueFactory(param -> param.getValue().firstNameProperty());
        TablePosition<Person,String> pos = new TablePosition<Person, String>(table, 0, column);
        EventType<TableColumn.CellEditEvent<Person,String>> eventType = TableColumn.editCommitEvent();
        column.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person, String>(
                table, null, (EventType) eventType, "Richard Bair"));
    }

    @Test public void defaultOnEditCommitHandlerDealsWithInvalidTablePosition_indexIsNegative() {
        table.getColumns().add(column);
        column.setCellValueFactory(param -> param.getValue().firstNameProperty());
        TablePosition<Person,String> pos = new TablePosition<Person, String>(table, -1, column);
        EventType<TableColumn.CellEditEvent<Person,String>> eventType = TableColumn.editCommitEvent();
        column.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person,String>(
                table, pos, (EventType)eventType, "Richard Bair"));
        assertEquals("Humphrey McPhee", table.getItems().get(0).getFirstName());
    }

    @Test public void defaultOnEditCommitHandlerDealsWithInvalidTablePosition_indexIsTooLarge() {
        table.getColumns().add(column);
        column.setCellValueFactory(param -> param.getValue().firstNameProperty());
        TablePosition<Person,String> pos = new TablePosition<Person, String>(table, 100, column);
        EventType<TableColumn.CellEditEvent<Person,String>> eventType = TableColumn.editCommitEvent();
        column.getOnEditCommit().handle(new TableColumn.CellEditEvent<Person, String>(
                table, pos, (EventType) eventType, "Richard Bair"));
        assertEquals("Humphrey McPhee", table.getItems().get(0).getFirstName());
    }

    /*********************************************************************
     * Tests for the default comparator                                  *
     ********************************************************************/

    @Test public void sortingWithNullItems() {
        List<String> items = Arrays.asList("Hippo", null, "Fish", "Eagle", null);
        Collections.sort(items, TableColumn.DEFAULT_COMPARATOR);
        assertNull(items.get(0));
        assertNull(items.get(1));
        assertEquals("Eagle", items.get(2));
        assertEquals("Fish", items.get(3));
        assertEquals("Hippo", items.get(4));
    }

    @Test public void sortingWithSameItems() {
        List<String> items = Arrays.asList("Hippo", "Hippo", "Fish", "Eagle", "Fish");
        Collections.sort(items, TableColumn.DEFAULT_COMPARATOR);
        assertEquals("Eagle", items.get(0));
        assertEquals("Fish", items.get(1));
        assertEquals("Fish", items.get(2));
        assertEquals("Hippo", items.get(3));
        assertEquals("Hippo", items.get(4));
    }

    @Test public void sortingWithNonComparableItemsSortsByToString() {
        Person fred = new Person("Fred", 36);
        Person wilma = new Person("Wilma", 34);
        Person barney = new Person("Barney", 34);
        List<Person> items = Arrays.asList(fred, wilma, barney);
        Collections.sort(items, TableColumn.DEFAULT_COMPARATOR);
        assertSame(barney, items.get(0));
        assertSame(fred, items.get(1));
        assertSame(wilma, items.get(2));
    }

    @Test public void sortingMixOfComparableAndNonComparable() {
        Person fred = new Person("Fred", 36);
        Person wilma = new Person("Wilma", 34);
        Person barney = new Person("Barney", 34);
        List<Object> items = Arrays.asList(fred, wilma, barney, "Pebbles");
        Collections.sort(items, TableColumn.DEFAULT_COMPARATOR);
        assertSame(barney, items.get(0));
        assertSame(fred, items.get(1));
        assertEquals("Pebbles", items.get(2));
        assertSame(wilma, items.get(3));
    }

    /*********************************************************************
     * Tests for the default cell factory                                *
     ********************************************************************/

    @Test public void defaultCellFactoryHasNullTextByDefault() {
        table.getColumns().add(column);
        TableCell<Person,String> cell = column.getCellFactory().call(column);
        assertNull(cell.getText());
    }

    @Test public void defaultCellFactoryHasNullGraphicByDefault() {
        table.getColumns().add(column);
        TableCell<Person,String> cell = column.getCellFactory().call(column);
        assertNull(cell.getGraphic());
    }

    @Test public void defaultCellFactorySetsTextToNameWhenItemIsPerson() {
        table.getColumns().add(column);
        TableCell<Person,String> cell = column.getCellFactory().call(column);
        cell.updateIndex(0);
        CellShim.updateItem(cell, table.getItems().get(0).getFirstName(), false);
        assertEquals(table.getItems().get(0).getFirstName(), cell.getText());
    }

    @Test public void defaultCellFactorySetsTextToNullWhenItemIsNull() {
        table.getColumns().add(column);
        TableCell<Person,String> cell = column.getCellFactory().call(column);
        // First have to set to a value, or it short-cuts
        cell.updateIndex(0);
        CellShim.updateItem(cell, table.getItems().get(0).getFirstName(), false);
        // Now we're good to go
        table.getItems().set(0, null);
        CellShim.updateItem(cell, null, false);
        assertNull(cell.getText());
    }

    @Test public void defaultCellFactorySetsGraphicToNullWhenItemIsPerson() {
        table.getColumns().add(column);
        TableCell<Person,String> cell = column.getCellFactory().call(column);
        cell.updateIndex(0);
        CellShim.updateItem(cell, table.getItems().get(0).getFirstName(), false);
        assertNull(cell.getGraphic());
    }

    @Test public void defaultCellFactorySetsGraphicToNullWhenItemIsNull() {
        table.getColumns().add(column);
        TableCell<Person,String> cell = column.getCellFactory().call(column);
        table.getItems().set(0, null);
        cell.updateIndex(0);
        CellShim.updateItem(cell, null, false);
        assertNull(cell.getGraphic());
    }

    @Test public void defaultCellFactorySetsGraphicToItemWhenItemIsNode() {
        TableView<Node> nodeTable = new TableView<Node>();
        Rectangle rect = new Rectangle();
        nodeTable.getItems().add(rect);
        TableColumn<Node,Node> nodeColumn = new TableColumn<Node,Node>();
        nodeTable.getColumns().add(nodeColumn);
        TableCell<Node,Node> cell = nodeColumn.getCellFactory().call(nodeColumn);
        cell.updateIndex(0);
        CellShim.updateItem(cell, rect, false);
        assertSame(rect, cell.getGraphic());
    }

    @Test public void defaultCellFactorySetsTextToNullWhenItemIsNode() {
        TableView<Node> nodeTable = new TableView<Node>();
        Rectangle rect = new Rectangle();
        nodeTable.getItems().add(rect);
        TableColumn<Node,Node> nodeColumn = new TableColumn<Node,Node>();
        nodeTable.getColumns().add(nodeColumn);
        TableCell<Node,Node> cell = nodeColumn.getCellFactory().call(nodeColumn);
        cell.updateIndex(0);
        CellShim.updateItem(cell, rect, false);
        assertNull(cell.getText());
    }

    // column with null cellValueFactory still updates item when row item changes

    // children widths always add up to parent width
        // change sibling width
        // change parent width


    public interface CellValueFactory<S,T> extends Callback<TableColumn.CellDataFeatures<S,T>, ObservableValue<T>> {
    }

    public interface CellFactory<S,T> extends Callback<TableColumn<S,T>, TableCell<S,T>> {
    }

    @Test public void test_rt36715_idIsNullAtStartup() {
        assertNull(column.getId());
    }

    @Test public void test_rt36715_idIsSettable() {
        column.setId("test-id");
        assertEquals("test-id", column.getId());
    }

    @Test public void test_rt36715_columnHeaderIdMirrorsTableColumnId_setIdBeforeHeaderInstantiation() {
        test_rt36715_columnHeaderPropertiesMirrorTableColumnProperties(true, true, false, false);
    }

    @Test public void test_rt36715_columnHeaderIdMirrorsTableColumnId_setIdAfterHeaderInstantiation() {
        test_rt36715_columnHeaderPropertiesMirrorTableColumnProperties(true, false, false, false);
    }

    @Test public void test_rt36715_styleIsEmptyStringAtStartup() {
        assertEquals("", column.getStyle());
    }

    @Test public void test_rt36715_styleIsSettable() {
        column.setStyle("-fx-border-color: red");
        assertEquals("-fx-border-color: red", column.getStyle());
    }

    @Test public void test_rt36715_columnHeaderStyleMirrorsTableColumnStyle_setStyleBeforeHeaderInstantiation() {
        test_rt36715_columnHeaderPropertiesMirrorTableColumnProperties(false, false, true, true);
    }

    @Test public void test_rt36715_columnHeaderStyleMirrorsTableColumnStyle_setStyleAfterHeaderInstantiation() {
        test_rt36715_columnHeaderPropertiesMirrorTableColumnProperties(false, false, true, false);
    }

    private void test_rt36715_columnHeaderPropertiesMirrorTableColumnProperties(
            boolean setId, boolean setIdBeforeHeaderInstantiation,
            boolean setStyle, boolean setStyleBeforeHeaderInstantiation) {
        table.getColumns().add(column);

        if (setId && setIdBeforeHeaderInstantiation) {
            column.setId("test-id");
        }
        if (setStyle && setStyleBeforeHeaderInstantiation) {
            column.setStyle("-fx-border-color: red");
        }

        StageLoader sl = new StageLoader(table);
        TableColumnHeader header = VirtualFlowTestUtils.getTableColumnHeader(table, column);

        if (setId && ! setIdBeforeHeaderInstantiation) {
            column.setId("test-id");
        }
        if (setStyle && ! setStyleBeforeHeaderInstantiation) {
            column.setStyle("-fx-border-color: red");
        }

        if (setId) {
            assertEquals("test-id", header.getId());
        }
        if (setStyle) {
            assertEquals("-fx-border-color: red", header.getStyle());
        }

        sl.dispose();
    }
}
