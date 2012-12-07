/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.BooleanProperty;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javafx.scene.control.TableColumnTest.Person;

import static org.junit.Assert.*;

/**
 */
public class TreeTableColumnTest {
    private static final double MIN_WIDTH = 35;
    private static final double MAX_WIDTH = 2000;
    private static final double PREF_WIDTH = 100;

    private TreeTableColumn<Person,String> column;
    private TreeTableView<Person> table;
//    private ObservableList<Person> model;
    
//    private static ObservableList<String> data = FXCollections.<String>observableArrayList();
//    private static final String ROW_1_VALUE = "Row 1";
//    private static final String ROW_2_VALUE = "Row 2";
//    private static final String ROW_5_VALUE = "Row 5";
//    private static final String ROW_20_VALUE = "Row 20";
    private static final TreeItem<Person> root;
//    private static final TreeItem<String> ROW_2_TREE_VALUE;
//    private static final TreeItem<String> ROW_5_TREE_VALUE;
    
    static {
//        data.addAll(ROW_1_VALUE, ROW_2_VALUE, "Long Row 3", "Row 4", ROW_5_VALUE, "Row 6",
//                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
//                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", ROW_20_VALUE);
//        
        root = new TreeItem<Person>(null);
        root.setExpanded(true);
        
        root.getChildren().addAll(
                new TreeItem(new Person("Humphrey McPhee", 76)),
                new TreeItem(new Person("Justice Caldwell", 30)),
                new TreeItem(new Person("Orrin Davies", 30)),
                new TreeItem(new Person("Emma Wilson", 8)));
//        for (int i = 1; i < data.size(); i++) {
//            root.getChildren().add(new TreeItem<String>(data.get(i)));
//        }
//        ROW_2_TREE_VALUE = root.getChildren().get(0);
//        ROW_5_TREE_VALUE = root.getChildren().get(3);
    }

    @Before public void setup() {
        column = new TreeTableColumn<Person,String>("");
        table = new TreeTableView<Person>(root);
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void defaultConstructorHasDefaultCellFactory() {
        assertSame(TreeTableColumn.DEFAULT_CELL_FACTORY, column.getCellFactory());
        assertSame(TreeTableColumn.DEFAULT_CELL_FACTORY, column.cellFactoryProperty().get());
    }

    @Test public void defaultConstructorHasDefaultComparator() {
        assertSame(TreeTableColumn.DEFAULT_COMPARATOR, column.getComparator());
        assertSame(TreeTableColumn.DEFAULT_COMPARATOR, column.comparatorProperty().get());
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
        assertNull(column.getTreeTableView());
        assertNull(column.treeTableViewProperty().get());
    }

    @Test public void tableViewCanBeSpecified() {
        column.setTreeTableView(table);
        assertSame(table, column.getTreeTableView());
        assertSame(table, column.treeTableViewProperty().get());
    }

    @Test public void tableViewCanBeResetToNull() {
        column.setTreeTableView(table);
        column.setTreeTableView(null);
        assertNull(column.getTreeTableView());
        assertNull(column.treeTableViewProperty().get());
    }

    @Test public void treeTableViewPropertyBeanIsCorrect() {
        assertSame(column, column.treeTableViewProperty().getBean());
    }

    @Test public void treeTableViewPropertyNameIsCorrect() {
        assertEquals("treeTableView", column.treeTableViewProperty().getName());
    }

    @Test public void whenTableViewIsChangedChildColumnsAreUpdated() {
        TreeTableColumn<Person,String> child = new TreeTableColumn<Person,String>();
        column.getColumns().add(child);
        table.getColumns().add(column);

        TreeTableView<Person> other = new TreeTableView<Person>();
        table.getColumns().clear();
        other.getColumns().add(column);

        assertSame(other, child.getTreeTableView());
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
        TreeTableColumn child = new TreeTableColumn();
        assertNull(child.getParentColumn());
        assertNull(child.parentColumnProperty().get());
    }

    @Test public void parentColumnIsUpdatedWhenAddedToParent() {
        TreeTableColumn child = new TreeTableColumn();
        column.getColumns().add(child);
        assertSame(column, child.getParentColumn());
        assertSame(column, child.parentColumnProperty().get());
    }

    @Test public void parentColumnIsClearedWhenRemovedFromParent() {
        TreeTableColumn child = new TreeTableColumn();
        column.getColumns().add(child);
        column.getColumns().remove(child);
        assertNull(child.getParentColumn());
        assertNull(child.parentColumnProperty().get());
    }

    @Test public void parentColumnIsClearedWhenParentColumnsIsCleared() {
        TreeTableColumn child = new TreeTableColumn();
        column.getColumns().add(child);
        column.getColumns().clear();
        assertNull(child.getParentColumn());
        assertNull(child.parentColumnProperty().get());
    }

    @Test public void tableViewReferenceIsUpdatedWhenAddedToParent() {
        table.getColumns().add(column);
        TreeTableColumn child = new TreeTableColumn();
        column.getColumns().add(child);
        assertSame(table, child.getTreeTableView());
        assertSame(table, child.treeTableViewProperty().get());
    }

    @Test public void tableViewReferenceIsClearedWhenRemovedFromParent() {
        table.getColumns().add(column);
        TreeTableColumn child = new TreeTableColumn();
        column.getColumns().add(child);
        column.getColumns().remove(child);
        assertNull(child.getTreeTableView());
        assertNull(child.treeTableViewProperty().get());
    }

    @Test public void tableViewReferenceIsClearedWhenParentColumnsIsCleared() {
        table.getColumns().add(column);
        TreeTableColumn child = new TreeTableColumn();
        column.getColumns().add(child);
        column.getColumns().clear();
        assertNull(child.getTreeTableView());
        assertNull(child.treeTableViewProperty().get());
    }

    @Test public void visibleIsUpdatedWhenParentColumnVisibleChanges() {
        TreeTableColumn child = new TreeTableColumn();
        column.getColumns().add(child);
        column.setVisible(true);
        assertTrue(child.visibleProperty().get());
        column.setVisible(false);
        assertFalse(child.isVisible());
    }

    @Test public void visibleIsTrueWhenAddedToParentColumnWithVisibleTrue() {
        TreeTableColumn child = new TreeTableColumn();
        column.setVisible(true);
        column.getColumns().add(child);
        assertTrue(child.isVisible());
        assertTrue(child.visibleProperty().get());
    }

    @Test public void visibleIsFalseWhenAddedToParentColumnWithVisibleFalse() {
        TreeTableColumn child = new TreeTableColumn();
        column.setVisible(false);
        column.getColumns().add(child);
        assertFalse(child.isVisible());
        assertFalse(child.visibleProperty().get());
    }

    @Test public void visibleIsNotChangedWhenRemovedFromParentColumn() {
        TreeTableColumn child = new TreeTableColumn();
        column.getColumns().add(child);
        column.setVisible(false);
        column.getColumns().clear();
        assertFalse(child.isVisible());
        assertFalse(child.visibleProperty().get());
    }

    @Test public void childVisibleChangesAccordingToParentVisibleWhenParentVisibleIsBound() {
        TreeTableColumn<Person,String> child = new TreeTableColumn<Person,String>();
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
        CellValueFactory<Person,String> factory = new CellValueFactory<Person,String>() {
            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
                return param.getValue().getValue().nameProperty();
            }
        };

        column.setCellValueFactory(factory);
        assertSame(factory, column.getCellValueFactory());
        assertSame(factory, column.cellValueFactoryProperty().get());
    }

    @Test public void cellValueFactoryCanBeResetToNull() {
        CellValueFactory<Person,String> factory = new CellValueFactory<Person,String>() {
            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
                return param.getValue().getValue().nameProperty();
            }
        };

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
        CellValueFactory<Person,String> factory = new CellValueFactory<Person,String>() {
            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
                return param.getValue().getValue().nameProperty();
            }
        };
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
        CellFactory<Person,String> factory = new CellFactory<Person, String>() {
            @Override public TreeTableCell call(TreeTableColumn<Person, String> param) {
                return null;
            }
        };
        column.setCellFactory(factory);
        assertSame(factory, column.getCellFactory());
        assertSame(factory, column.cellFactoryProperty().get());
    }

    @Test public void cellFactoryCanBeResetToNull() {
        CellFactory<Person,String> factory = new CellFactory<Person, String>() {
            @Override public TreeTableCell call(TreeTableColumn<Person,String> param) {
                return null;
            }
        };
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
        CellFactory<Person,String> factory = new CellFactory<Person, String>() {
            @Override public TreeTableCell call(TreeTableColumn<Person,String> param) {
                return null;
            }
        };
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

    @Ignore ("Fails with hardcoded value of 10")
    @Test public void minWidthIs_USE_COMPUTED_SIZE_ByDefault() {
        assertEquals(Control.USE_COMPUTED_SIZE, column.getMinWidth(), 0);
        assertEquals(Control.USE_COMPUTED_SIZE, column.minWidthProperty().get(), 0);
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

    @Ignore ("Fails with hardcoded value of 5000")
    @Test public void maxWidthIs_USE_COMPUTED_SIZE_ByDefault() {
        assertEquals(Control.USE_COMPUTED_SIZE, column.getMaxWidth(), 0);
        assertEquals(Control.USE_COMPUTED_SIZE, column.maxWidthProperty().get(), 0);
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

    @Ignore ("Fails with hardcoded value of 80")
    @Test public void prefWidthIs_USE_COMPUTED_SIZE_ByDefault() {
        assertEquals(Control.USE_COMPUTED_SIZE, column.getPrefWidth(), 0);
        assertEquals(Control.USE_COMPUTED_SIZE, column.prefWidthProperty().get(), 0);
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
        assertSame(TreeTableColumn.SortType.ASCENDING, column.getSortType());
        assertSame(TreeTableColumn.SortType.ASCENDING, column.sortTypeProperty().get());
    }

    @Test public void sortTypeCanBeSpecified() {
        column.setSortType(TreeTableColumn.SortType.DESCENDING);
        assertSame(TreeTableColumn.SortType.DESCENDING, column.getSortType());
        assertSame(TreeTableColumn.SortType.DESCENDING, column.sortTypeProperty().get());
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
        ObjectProperty<TreeTableColumn.SortType> other =
                new SimpleObjectProperty<TreeTableColumn.SortType>(TreeTableColumn.SortType.DESCENDING);
        column.sortTypeProperty().bind(other);
        assertSame(TreeTableColumn.SortType.DESCENDING, column.getSortType());
        assertSame(TreeTableColumn.SortType.DESCENDING, column.sortTypeProperty().get());
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
     * Tests for the comparator property                                 *
     ********************************************************************/

    @Test public void comparatorCanBeSpecified() {
        Comparator<String> comparator = new Comparator<String>() {
            @Override public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };
        column.setComparator(comparator);
        assertSame(comparator, column.getComparator());
        assertSame(comparator, column.comparatorProperty().get());
    }

    @Test public void comparatorCanBeResetToNull() {
        Comparator<String> comparator = new Comparator<String>() {
            @Override public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };
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
        Comparator<String> comparator = new Comparator<String>() {
            @Override public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };
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
        EventHandler<TreeTableColumn.CellEditEvent<Person,String>> handler =
                new EventHandler<TreeTableColumn.CellEditEvent<Person, String>>() {
            @Override public void handle(TreeTableColumn.CellEditEvent<Person, String> event) {
            }
        };
        column.setOnEditStart(handler);
        assertSame(handler, column.getOnEditStart());
        assertSame(handler, column.onEditStartProperty().get());
    }

    @Test public void onEditStartCanBeResetToNull() {
        EventHandler<TreeTableColumn.CellEditEvent<Person,String>> handler =
                new EventHandler<TreeTableColumn.CellEditEvent<Person, String>>() {
            @Override public void handle(TreeTableColumn.CellEditEvent<Person, String> event) {
            }
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
        EventHandler<TreeTableColumn.CellEditEvent<Person,String>> handler =
                new EventHandler<TreeTableColumn.CellEditEvent<Person, String>>() {
            @Override public void handle(TreeTableColumn.CellEditEvent<Person, String> event) {
            }
        };
        ObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<Person,String>>> other =
                new SimpleObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<Person, String>>>(handler);
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
        EventHandler<TreeTableColumn.CellEditEvent<Person,String>> handler =
                new EventHandler<TreeTableColumn.CellEditEvent<Person, String>>() {
            @Override public void handle(TreeTableColumn.CellEditEvent<Person, String> event) {
            }
        };
        column.setOnEditCancel(handler);
        assertSame(handler, column.getOnEditCancel());
        assertSame(handler, column.onEditCancelProperty().get());
    }

    @Test public void onEditCancelCanBeResetToNull() {
        EventHandler<TreeTableColumn.CellEditEvent<Person,String>> handler =
                new EventHandler<TreeTableColumn.CellEditEvent<Person, String>>() {
            @Override public void handle(TreeTableColumn.CellEditEvent<Person, String> event) {
            }
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
        EventHandler<TreeTableColumn.CellEditEvent<Person,String>> handler =
                new EventHandler<TreeTableColumn.CellEditEvent<Person, String>>() {
            @Override public void handle(TreeTableColumn.CellEditEvent<Person, String> event) {
            }
        };
        ObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<Person,String>>> other =
                new SimpleObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<Person, String>>>(handler);
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
        EventHandler<TreeTableColumn.CellEditEvent<Person,String>> handler =
                new EventHandler<TreeTableColumn.CellEditEvent<Person, String>>() {
            @Override public void handle(TreeTableColumn.CellEditEvent<Person, String> event) {
            }
        };
        column.setOnEditCommit(handler);
        assertSame(handler, column.getOnEditCommit());
        assertSame(handler, column.onEditCommitProperty().get());
    }

    @Test public void onEditCommitCanBeResetToNull() {
        EventHandler<TreeTableColumn.CellEditEvent<Person,String>> handler =
                new EventHandler<TreeTableColumn.CellEditEvent<Person, String>>() {
            @Override public void handle(TreeTableColumn.CellEditEvent<Person, String> event) {
            }
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
        EventHandler<TreeTableColumn.CellEditEvent<Person,String>> handler =
                new EventHandler<TreeTableColumn.CellEditEvent<Person, String>>() {
            @Override public void handle(TreeTableColumn.CellEditEvent<Person, String> event) {
            }
        };
        ObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<Person,String>>> other =
                new SimpleObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<Person, String>>>(handler);
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

//    @Test public void getCellDataReturnsNullWhenTableViewIsNull2() {
//        assertNull(column.getCellData(table.getItems().get(1)));
//    }
//
//    @Test public void getCellDataReturnsNullWhenTableViewItemsIsNull() {
//        table.getColumns().add(column);
//        table.setItems(null);
//        assertNull(column.getCellData(0));
//    }
//
//    @Test public void getCellDataReturnsNullWhenTableViewItemsIsNull2() {
//        final Person person = table.getItems().get(1);
//        table.getColumns().add(column);
//        table.setItems(null);
//        assertNull(column.getCellData(person));
//    }
//
//    @Test public void getCellDataReturnsNullWhenRowDataIsNullByDefault() {
//        table.getColumns().add(column);
//        table.getItems().set(0, null);
//        assertNull(column.getCellData(0));
//    }

    @Test public void getCellDataReturnsNullWhenRowDataIsNullByDefault2() {
        table.getColumns().add(column);
        assertNull(column.getCellData(null));
    }

    @Test public void getCellDataReturnsNullWhenIndexIsNegative() {
        table.getColumns().add(column);
        assertNull(column.getCellData(-1));
    }

//    @Test public void getCellDataReturnsNullWhenIndexIsTooLarge() {
//        table.getColumns().add(column);
//        assertNull(column.getCellData(table.getItems().size()));
//    }

    @Test public void getCellDataReturnsNullWhenCellValueFactoryIsNull() {
        table.getColumns().add(column);
        assertNull(column.getCellData(0));
    }

//    @Test public void getCellDataReturnsNullWhenCellValueFactoryIsNull2() {
//        table.getColumns().add(column);
//        assertNull(column.getCellData(table.getItems().get(1)));
//    }

    @Test public void getCellDataReturnsValue() {
        table.getColumns().add(column);
        column.setCellValueFactory(new CellValueFactory<Person, String>() {
            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
                return param.getValue().getValue().nameProperty();
            }
        });
        assertEquals("Humphrey McPhee", column.getCellData(1));
    }

//    @Test public void getCellDataReturnsValue2() {
//        table.getColumns().add(column);
//        column.setCellValueFactory(new CellValueFactory<Person, String>() {
//            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
//                return param.getValue().getValue().nameProperty();
//            }
//        });
//        assertEquals("Humphrey McPhee", column.getCellData(table.getItems().get(0)));
//    }
//
//    @Test public void cellDataFeaturesHasTableViewSpecified() {
//        final boolean[] passed = new boolean[] { false };
//        table.getColumns().add(column);
//        column.setCellValueFactory(new CellValueFactory<Person, String>() {
//            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
//                passed[0] = param.getTreeTableView() == table;
//                return param.getValue().getValue().nameProperty();
//            }
//        });
//        column.getCellData(table.getItems().get(0));
//        assertTrue(passed[0]);
//    }
//
//    @Test public void cellDataFeaturesHasTreeTableColumnSpecified() {
//        final boolean[] passed = new boolean[] { false };
//        table.getColumns().add(column);
//        column.setCellValueFactory(new CellValueFactory<Person, String>() {
//            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
//                passed[0] = param.getTreeTableColumn() == column;
//                return param.getValue().getValue().nameProperty();
//            }
//        });
//        column.getCellData(table.getItems().get(0));
//        assertTrue(passed[0]);
//    }
//
//    @Test public void cellDataFeaturesHasRowItemSpecified() {
//        final boolean[] passed = new boolean[] { false };
//        table.getColumns().add(column);
//        column.setCellValueFactory(new CellValueFactory<Person,String>() {
//            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
//                passed[0] = param.getValue() == table.getItems().get(0);
//                return param.getValue().nameProperty();
//            }
//        });
//        column.getCellData(table.getItems().get(0));
//        assertTrue(passed[0]);
//    }

    /*********************************************************************
     * Tests for the DEFAULT_EDIT_COMMIT_HANDLER. By default each        *
     * TreeTableColumn has a DEFAULT_EDIT_COMMIT_HANDLER installed which     *
     * will, if the value returned by cellValueFactory is a              *
     * WritableValue, commit. If not, then nothing happens.              *
     ********************************************************************/

    @Test public void onEditCommitHandlerInstalledByDefault() {
        assertNotNull(column.getOnEditCommit());
    }

//    @Test public void defaultOnEditCommitHandlerWillSaveToWritableValue() {
//        table.getColumns().add(column);
//        column.setCellValueFactory(new CellValueFactory<Person, String>() {
//            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
//                return param.getValue().getValue().nameProperty();
//            }
//        });
//        TreeTablePosition<Person,String> pos = new TreeTablePosition<Person, String>(table, 0, column);
//        EventType<TreeTableColumn.CellEditEvent<Person,String>> eventType = TreeTableColumn.editCommitEvent();
//        column.getOnEditCommit().handle(new TreeTableColumn.CellEditEvent<Person,String>(
//                table, pos, (EventType)eventType, "Richard Bair"));
//        assertEquals("Richard Bair", table.getItems().get(0).getName());
//    }
//
//    @Test public void defaultOnEditCommitHandlerWillIgnoreReadOnlyValue() {
//        TreeTableColumn<Person,Number> ageColumn = new TreeTableColumn<Person,Number>();
//        table.getColumns().add(ageColumn);
//        ageColumn.setCellValueFactory(new CellValueFactory<Person, Number>() {
//            @Override public ObservableValue<Number> call(TreeTableColumn.CellDataFeatures<Person, Number> param) {
//                return param.getValue().getValue().ageProperty();
//            }
//        });
//        TreeTablePosition<Person,Number> pos = new TreeTablePosition<Person, Number>(table, 0, ageColumn);
//        EventType<TreeTableColumn.CellEditEvent<Person,Number>> eventType = TreeTableColumn.editCommitEvent();
//        ageColumn.getOnEditCommit().handle(new TreeTableColumn.CellEditEvent<Person,Number>(
//                table, pos, (EventType)eventType, 109));
//        assertEquals(76, table.getItems().get(0).getAge());
//    }

    @Test(expected=NullPointerException.class)
    public void defaultOnEditCommitHandlerDealsWithNullTableView() {
        table.getColumns().add(column);
        column.setCellValueFactory(new CellValueFactory<Person, String>() {
            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
                return param.getValue().getValue().nameProperty();
            }
        });
        TreeTablePosition<Person,String> pos = new TreeTablePosition<Person, String>(table, 0, column);
        EventType<TreeTableColumn.CellEditEvent<Person,String>> eventType = TreeTableColumn.editCommitEvent();
        column.getOnEditCommit().handle(new TreeTableColumn.CellEditEvent<Person, String>(
                null, pos, (EventType) eventType, "Richard Bair"));
    }

    @Test(expected=NullPointerException.class)
    public void defaultOnEditCommitHandlerDealsWithNullTablePosition() {
        table.getColumns().add(column);
        column.setCellValueFactory(new CellValueFactory<Person, String>() {
            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
                return param.getValue().getValue().nameProperty();
            }
        });
        TreeTablePosition<Person,String> pos = new TreeTablePosition<Person, String>(table, 0, column);
        EventType<TreeTableColumn.CellEditEvent<Person,String>> eventType = TreeTableColumn.editCommitEvent();
        column.getOnEditCommit().handle(new TreeTableColumn.CellEditEvent<Person, String>(
                table, null, (EventType) eventType, "Richard Bair"));
    }
//
//    @Test public void defaultOnEditCommitHandlerDealsWithInvalidTablePosition_indexIsNegative() {
//        table.getColumns().add(column);
//        column.setCellValueFactory(new CellValueFactory<Person, String>() {
//            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
//                return param.getValue().getValue().nameProperty();
//            }
//        });
//        TreeTablePosition<Person,String> pos = new TreeTablePosition<Person, String>(table, -1, column);
//        EventType<TreeTableColumn.CellEditEvent<Person,String>> eventType = TreeTableColumn.editCommitEvent();
//        column.getOnEditCommit().handle(new TreeTableColumn.CellEditEvent<Person,String>(
//                table, pos, (EventType)eventType, "Richard Bair"));
//        assertEquals("Humphrey McPhee", table.getItems().get(0).getName());
//    }
//
//    @Test public void defaultOnEditCommitHandlerDealsWithInvalidTablePosition_indexIsTooLarge() {
//        table.getColumns().add(column);
//        column.setCellValueFactory(new CellValueFactory<Person, String>() {
//            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Person, String> param) {
//                return param.getValue().getValue().nameProperty();
//            }
//        });
//        TreeTablePosition<Person,String> pos = new TreeTablePosition<Person, String>(table, 100, column);
//        EventType<TreeTableColumn.CellEditEvent<Person,String>> eventType = TreeTableColumn.editCommitEvent();
//        column.getOnEditCommit().handle(new TreeTableColumn.CellEditEvent<Person, String>(
//                table, pos, (EventType) eventType, "Richard Bair"));
//        assertEquals("Humphrey McPhee", table.getItems().get(0).getName());
//    }

    /*********************************************************************
     * Tests for the default comparator                                  *
     ********************************************************************/

    @Test public void sortingWithNullItems() {
        List<String> items = Arrays.asList("Hippo", null, "Fish", "Eagle", null);
        Collections.sort(items, TreeTableColumn.DEFAULT_COMPARATOR);
        assertNull(items.get(0));
        assertNull(items.get(1));
        assertEquals("Eagle", items.get(2));
        assertEquals("Fish", items.get(3));
        assertEquals("Hippo", items.get(4));
    }

    @Test public void sortingWithSameItems() {
        List<String> items = Arrays.asList("Hippo", "Hippo", "Fish", "Eagle", "Fish");
        Collections.sort(items, TreeTableColumn.DEFAULT_COMPARATOR);
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
        Collections.sort(items, TreeTableColumn.DEFAULT_COMPARATOR);
        assertSame(barney, items.get(0));
        assertSame(fred, items.get(1));
        assertSame(wilma, items.get(2));
    }

    @Ignore("This started failing when I upgraded to Java 7")
    @Test public void sortingMixOfComparableAndNonComparable() {
        Person fred = new Person("Fred", 36);
        Person wilma = new Person("Wilma", 34);
        Person barney = new Person("Barney", 34);
        List<Object> items = Arrays.asList(fred, wilma, barney, "Pebbles");
        Collections.sort(items, TreeTableColumn.DEFAULT_COMPARATOR);
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
        TreeTableCell<Person,String> cell = column.getCellFactory().call(column);
        assertNull(cell.getText());
    }

    @Test public void defaultCellFactoryHasNullGraphicByDefault() {
        table.getColumns().add(column);
        TreeTableCell<Person,String> cell = column.getCellFactory().call(column);
        assertNull(cell.getGraphic());
    }

//    @Test public void defaultCellFactorySetsTextToNameWhenItemIsPerson() {
//        table.getColumns().add(column);
//        TreeTableCell<Person,String> cell = column.getCellFactory().call(column);
//        cell.updateIndex(0);
//        cell.updateItem(table.getItems().get(0).getName(), false);
//        assertEquals(table.getItems().get(0).getName(), cell.getText());
//    }
//
//    @Test public void defaultCellFactorySetsTextToNullWhenItemIsNull() {
//        table.getColumns().add(column);
//        TreeTableCell<Person,String> cell = column.getCellFactory().call(column);
//        // First have to set to a value, or it short-cuts
//        cell.updateIndex(0);
//        cell.updateItem(table.getItems().get(0).getName(), false);
//        // Now we're good to go
//        table.getItems().set(0, null);
//        cell.updateItem(null, false);
//        assertNull(cell.getText());
//    }
//
//    @Test public void defaultCellFactorySetsGraphicToNullWhenItemIsPerson() {
//        table.getColumns().add(column);
//        TreeTableCell<Person,String> cell = column.getCellFactory().call(column);
//        cell.updateIndex(0);
//        cell.updateItem(table.getItems().get(0).getName(), false);
//        assertNull(cell.getGraphic());
//    }
//
//    @Test public void defaultCellFactorySetsGraphicToNullWhenItemIsNull() {
//        table.getColumns().add(column);
//        TreeTableCell<Person,String> cell = column.getCellFactory().call(column);
//        table.getItems().set(0, null);
//        cell.updateIndex(0);
//        cell.updateItem(null, false);
//        assertNull(cell.getGraphic());
//    }

    @Test public void defaultCellFactorySetsGraphicToItemWhenItemIsNode() {
        TreeTableView<Node> nodeTable = new TreeTableView<Node>();
        Rectangle rect = new Rectangle();
        nodeTable.setRoot(new TreeItem<Node>(rect));
        TreeTableColumn<Node,Node> nodeColumn = new TreeTableColumn<Node,Node>();
        nodeTable.getColumns().add(nodeColumn);
        TreeTableCell<Node,Node> cell = nodeColumn.getCellFactory().call(nodeColumn);
        cell.updateIndex(0);
        cell.updateItem(rect, false);
        assertSame(rect, cell.getGraphic());
    }

    @Test public void defaultCellFactorySetsTextToNullWhenItemIsNode() {
        TreeTableView<Node> nodeTable = new TreeTableView<Node>();
        Rectangle rect = new Rectangle();
        nodeTable.setRoot(new TreeItem<Node>(rect));
        TreeTableColumn<Node,Node> nodeColumn = new TreeTableColumn<Node,Node>();
        nodeTable.getColumns().add(nodeColumn);
        TreeTableCell<Node,Node> cell = nodeColumn.getCellFactory().call(nodeColumn);
        cell.updateIndex(0);
        cell.updateItem(rect, false);
        assertNull(cell.getText());
    }

    // column with null cellValueFactory still updates item when row item changes

    // children widths always add up to parent width
        // change sibling width
        // change parent width


//    public static class Person {
//        public Person() { }
//        public Person(String name, int age) {
//            setName(name);
//            this.age.set(age);
//        }
//
//        private final StringProperty name = new SimpleStringProperty();
//        public final String getName() {return name.get();}
//        public void setName(String value) {name.set(value);}
//        public StringProperty nameProperty() {return name;}
//
//        private final ReadOnlyIntegerWrapper age = new ReadOnlyIntegerWrapper();
//        public final int getAge() {return age.get();}
//        public ReadOnlyIntegerProperty ageProperty() {return age.getReadOnlyProperty();}
//
//        @Override public String toString() {
//            return getName();
//        }
//    }

    public interface CellValueFactory<S,T> extends Callback<TreeTableColumn.CellDataFeatures<S,T>, ObservableValue<T>> {
    }

    public interface CellFactory<S,T> extends Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>> {
    }
}
