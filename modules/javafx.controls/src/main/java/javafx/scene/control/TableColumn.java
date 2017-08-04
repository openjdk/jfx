/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.Properties;
import javafx.scene.control.skin.NestedTableColumnHeader;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.TableViewSkinBase;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import javafx.collections.WeakListChangeListener;
import java.util.Collections;

import java.util.List;
import java.util.Map;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;

/**
 * A {@link TableView} is made up of a number of TableColumn instances. Each
 * TableColumn in a table is responsible for displaying (and editing) the contents
 * of that column. As well as being responsible for displaying and editing data
 * for a single column, a TableColumn also contains the necessary properties to:
 * <ul>
 *    <li>Be resized (using {@link #minWidthProperty() minWidth}/{@link #prefWidthProperty() prefWidth}/{@link #maxWidthProperty() maxWidth}
 *      and {@link #widthProperty() width} properties)
 *    <li>Have its {@link #visibleProperty() visibility} toggled
 *    <li>Display {@link #textProperty() header text}
 *    <li>Display any {@link #getColumns() nested columns} it may contain
 *    <li>Have a {@link #contextMenuProperty() context menu} when the user
 *      right-clicks the column header area
 *    <li>Have the contents of the table be sorted (using
 *      {@link #comparatorProperty() comparator}, {@link #sortableProperty() sortable} and
 *      {@link #sortTypeProperty() sortType})
 * </ul>
 *
 * When creating a TableColumn instance, perhaps the two most important properties
 * to set are the column {@link #textProperty() text} (what to show in the column
 * header area), and the column {@link #cellValueFactoryProperty() cell value factory}
 * (which is used to populate individual cells in the column). This can be
 * achieved using some variation on the following code:
 *
 * <pre>
 * {@code
 * ObservableList<Person> data = ...
 * TableView<Person> tableView = new TableView<Person>(data);
 *
 * TableColumn<Person,String> firstNameCol = new TableColumn<Person,String>("First Name");
 * firstNameCol.setCellValueFactory(new Callback<CellDataFeatures<Person, String>, ObservableValue<String>>() {
 *     public ObservableValue<String> call(CellDataFeatures<Person, String> p) {
 *         // p.getValue() returns the Person instance for a particular TableView row
 *         return p.getValue().firstNameProperty();
 *     }
 *  });
 * }
 * tableView.getColumns().add(firstNameCol);}</pre>
 *
 * This approach assumes that the object returned from <code>p.getValue()</code>
 * has a JavaFX {@link ObservableValue} that can simply be returned. The benefit of this
 * is that the TableView will internally create bindings to ensure that,
 * should the returned {@link ObservableValue} change, the cell contents will be
 * automatically refreshed.
 *
 * <p>In situations where a TableColumn must interact with classes created before
 * JavaFX, or that generally do not wish to use JavaFX apis for properties, it is
 * possible to wrap the returned value in a {@link ReadOnlyObjectWrapper} instance. For
 * example:
 *
 * <pre>
 * {@code
 * firstNameCol.setCellValueFactory(new Callback<CellDataFeatures<Person, String>, ObservableValue<String>>() {
 *     public ObservableValue<String> call(CellDataFeatures<Person, String> p) {
 *         return new ReadOnlyObjectWrapper(p.getValue().getFirstName());
 *     }
 *  });}</pre>
 *
 * It is hoped that over time there will be convenience cell value factories
 * developed and made available to developers. As of the JavaFX 2.0 release,
 * there is one such convenience class: {@link PropertyValueFactory}. This class
 * removes the need to write the code above, instead relying on reflection to
 * look up a given property from a String. Refer to the
 * <code>PropertyValueFactory</code> class documentation for more information
 * on how to use this with a TableColumn.
 *
 * Finally, for more detail on how to use TableColumn, there is further documentation in
 * the {@link TableView} class documentation.
 *
 * @param <S> The type of the TableView generic type (i.e. S == TableView&lt;S&gt;)
 * @param <T> The type of the content in all cells in this TableColumn.
 * @see TableView
 * @see TableCell
 * @see TablePosition
 * @since JavaFX 2.0
 */
public class TableColumn<S,T> extends TableColumnBase<S,T> implements EventTarget {

    /***************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * Parent event for any TableColumn edit event.
     * @param <S> The type of the TableView generic type
     * @param <T> The type of the content in all cells in this TableColumn
     * @return The any TableColumn edit event
     */
    @SuppressWarnings("unchecked")
    public static <S,T> EventType<CellEditEvent<S,T>> editAnyEvent() {
        return (EventType<CellEditEvent<S,T>>) EDIT_ANY_EVENT;
    }
    private static final EventType<?> EDIT_ANY_EVENT =
            new EventType<>(Event.ANY, "TABLE_COLUMN_EDIT");

    /**
     * Indicates that the user has performed some interaction to start an edit
     * event, or alternatively the {@link TableView#edit(int, javafx.scene.control.TableColumn)}
     * method has been called.
     * @param <S> The type of the TableView generic type
     * @param <T> The type of the content in all cells in this TableColumn
     * @return The start an edit event
     */
    @SuppressWarnings("unchecked")
    public static <S,T> EventType<CellEditEvent<S,T>> editStartEvent() {
        return (EventType<CellEditEvent<S,T>>) EDIT_START_EVENT;
    }
    private static final EventType<?> EDIT_START_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_START");

    /**
     * Indicates that the editing has been canceled, meaning that no change should
     * be made to the backing data source.
     * @param <S> The type of the TableView generic type
     * @param <T> The type of the content in all cells in this TableColumn
     * @return The cancel an edit event
     */
    @SuppressWarnings("unchecked")
    public static <S,T> EventType<CellEditEvent<S,T>> editCancelEvent() {
        return (EventType<CellEditEvent<S,T>>) EDIT_CANCEL_EVENT;
    }
    private static final EventType<?> EDIT_CANCEL_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_CANCEL");

    /**
     * Indicates that the editing has been committed by the user, meaning that
     * a change should be made to the backing data source to reflect the new
     * data.
     * @param <S> The type of the TableView generic type
     * @param <T> The type of the content in all cells in this TableColumn
     * @return The commit an edit event
     */
    @SuppressWarnings("unchecked")
    public static <S,T> EventType<CellEditEvent<S,T>> editCommitEvent() {
        return (EventType<CellEditEvent<S,T>>) EDIT_COMMIT_EVENT;
    }
    private static final EventType<?> EDIT_COMMIT_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_COMMIT");



    /**
     * If no cellFactory is specified on a TableColumn instance, then this one
     * will be used by default. At present it simply renders the TableCell item
     * property within the {@link TableCell#graphicProperty() graphic} property
     * if the {@link Cell#item item} is a Node, or it simply calls
     * <code>toString()</code> if it is not null, setting the resulting string
     * inside the {@link Cell#textProperty() text} property.
     */
    public static final Callback<TableColumn<?,?>, TableCell<?,?>> DEFAULT_CELL_FACTORY =
            new Callback<TableColumn<?,?>, TableCell<?,?>>() {

        @Override public TableCell<?,?> call(TableColumn<?,?> param) {
            return new TableCell<Object,Object>() {
                @Override protected void updateItem(Object item, boolean empty) {
                    if (item == getItem()) return;

                    super.updateItem(item, empty);

                    if (item == null) {
                        super.setText(null);
                        super.setGraphic(null);
                    } else if (item instanceof Node) {
                        super.setText(null);
                        super.setGraphic((Node)item);
                    } else {
                        super.setText(item.toString());
                        super.setGraphic(null);
                    }
                }
            };
        }
    };



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default TableColumn with default cell factory, comparator, and
     * onEditCommit implementation.
     */
    public TableColumn() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        setOnEditCommit(DEFAULT_EDIT_COMMIT_HANDLER);

        // we listen to the columns list here to ensure that widths are
        // maintained properly, and to also set the column hierarchy such that
        // all children columns know that this TableColumn is their parent.
        getColumns().addListener(weakColumnsListener);

        tableViewProperty().addListener(observable -> {
            // set all children of this tableView to have the same TableView
            // as this column
            for (TableColumn<S, ?> tc : getColumns()) {
                tc.setTableView(getTableView());
            }

            // This code was commented out due to RT-22391, with this enabled
            // the parent column will be null, which is not desired
//                // set the parent of this column to also have this tableView
//                if (getParentColumn() != null) {
//                    getParentColumn().setTableView(getTableView());
//                }
        });
    }

    /**
     * Creates a TableColumn with the text set to the provided string, with
     * default cell factory, comparator, and onEditCommit implementation.
     * @param text The string to show when the TableColumn is placed within the TableView.
     */
    public TableColumn(String text) {
        this();
        setText(text);
    }



    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private EventHandler<CellEditEvent<S,T>> DEFAULT_EDIT_COMMIT_HANDLER = t -> {
        int index = t.getTablePosition().getRow();
        List<S> list = t.getTableView().getItems();
        if (list == null || index < 0 || index >= list.size()) return;
        S rowData = list.get(index);
        ObservableValue<T> ov = getCellObservableValue(rowData);

        if (ov instanceof WritableValue) {
            ((WritableValue)ov).setValue(t.getNewValue());
        }
    };

    private ListChangeListener<TableColumn<S,?>> columnsListener = c -> {
        while (c.next()) {
            // update the TableColumn.tableView property
            for (TableColumn<S,?> tc : c.getRemoved()) {
                // Fix for RT-16978. In TableColumnHeader we add before we
                // remove when moving a TableColumn. This means that for
                // a very brief moment the tc is duplicated, and we can prevent
                // nulling out the tableview and parent column. Without this
                // here, in a very special circumstance it is possible to null
                // out the entire content of a column by reordering and then
                // sorting another column.
                if (getColumns().contains(tc)) continue;

                tc.setTableView(null);
                tc.setParentColumn(null);
            }
            for (TableColumn<S,?> tc : c.getAddedSubList()) {
                tc.setTableView(getTableView());
            }

            updateColumnWidths();
        }
    };

    private WeakListChangeListener<TableColumn<S,?>> weakColumnsListener =
            new WeakListChangeListener<TableColumn<S,?>>(columnsListener);



    /***************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    // Contains any children columns that should be nested within this column
    private final ObservableList<TableColumn<S,?>> columns = FXCollections.<TableColumn<S,?>>observableArrayList();



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- TableView
    /**
     * The TableView that this TableColumn belongs to.
     */
    private ReadOnlyObjectWrapper<TableView<S>> tableView = new ReadOnlyObjectWrapper<TableView<S>>(this, "tableView");
    public final ReadOnlyObjectProperty<TableView<S>> tableViewProperty() {
        return tableView.getReadOnlyProperty();
    }
    final void setTableView(TableView<S> value) { tableView.set(value); }
    public final TableView<S> getTableView() { return tableView.get(); }



    // --- Cell value factory
    /**
     * The cell value factory needs to be set to specify how to populate all
     * cells within a single TableColumn. A cell value factory is a {@link Callback}
     * that provides a {@link CellDataFeatures} instance, and expects an
     * {@link ObservableValue} to be returned. The returned ObservableValue instance
     * will be observed internally to allow for immediate updates to the value
     * to be reflected on screen.
     *
     * An example of how to set a cell value factory is:
     *
     * <pre><code>
     * lastNameCol.setCellValueFactory(new Callback&lt;CellDataFeatures&lt;Person, String&gt;, ObservableValue&lt;String&gt;&gt;() {
     *     public ObservableValue&lt;String&gt; call(CellDataFeatures&lt;Person, String&gt; p) {
     *         // p.getValue() returns the Person instance for a particular TableView row
     *         return p.getValue().lastNameProperty();
     *     }
     *  });
     * }
     * </code></pre>
     *
     * A common approach is to want to populate cells in a TableColumn using
     * a single value from a Java bean. To support this common scenario, there
     * is the {@link PropertyValueFactory} class. Refer to this class for more
     * information on how to use it, but briefly here is how the above use case
     * could be simplified using the PropertyValueFactory class:
     *
     * <pre><code>
     * lastNameCol.setCellValueFactory(new PropertyValueFactory&lt;Person,String&gt;("lastName"));
     * </code></pre>
     *
     * @see PropertyValueFactory
     */
    private ObjectProperty<Callback<CellDataFeatures<S,T>, ObservableValue<T>>> cellValueFactory;
    public final void setCellValueFactory(Callback<CellDataFeatures<S,T>, ObservableValue<T>> value) {
        cellValueFactoryProperty().set(value);
    }
    public final Callback<CellDataFeatures<S,T>, ObservableValue<T>> getCellValueFactory() {
        return cellValueFactory == null ? null : cellValueFactory.get();
    }
    public final ObjectProperty<Callback<CellDataFeatures<S,T>, ObservableValue<T>>> cellValueFactoryProperty() {
        if (cellValueFactory == null) {
            cellValueFactory = new SimpleObjectProperty<Callback<CellDataFeatures<S,T>, ObservableValue<T>>>(this, "cellValueFactory");
        }
        return cellValueFactory;
    }


    // --- Cell Factory
    /**
     * The cell factory for all cells in this column. The cell factory
     * is responsible for rendering the data contained within each TableCell for
     * a single table column.
     *
     * <p>By default TableColumn uses the {@link #DEFAULT_CELL_FACTORY default cell
     * factory}, but this can be replaced with a custom implementation, for
     * example to show data in a different way or to support editing.There is a
     * lot of documentation on creating custom cell factories
     * elsewhere (see {@link Cell} and {@link TableView} for example).</p>
     *
     * <p>Finally, there are a number of pre-built cell factories available in the
     * {@link javafx.scene.control.cell} package.
     */
    private final ObjectProperty<Callback<TableColumn<S,T>, TableCell<S,T>>> cellFactory =
        new SimpleObjectProperty<Callback<TableColumn<S,T>, TableCell<S,T>>>(
            this, "cellFactory", (Callback<TableColumn<S,T>, TableCell<S,T>>) ((Callback) DEFAULT_CELL_FACTORY)) {
                @Override protected void invalidated() {
                    TableView<S> table = getTableView();
                    if (table == null) return;
                    Map<Object,Object> properties = table.getProperties();
                    if (properties.containsKey(Properties.RECREATE)) {
                        properties.remove(Properties.RECREATE);
                    }
                    properties.put(Properties.RECREATE, Boolean.TRUE);
                }
            };

    public final void setCellFactory(Callback<TableColumn<S,T>, TableCell<S,T>> value) {
        cellFactory.set(value);
    }

    public final Callback<TableColumn<S,T>, TableCell<S,T>> getCellFactory() {
        return cellFactory.get();
    }

    public final ObjectProperty<Callback<TableColumn<S,T>, TableCell<S,T>>> cellFactoryProperty() {
        return cellFactory;
    }



    // --- Sort Type
    /**
     * Used to state whether this column, if it is part of a sort order (see
     * {@link TableView#getSortOrder()} for more details), should be sorted in
     * ascending or descending order.
     * Simply toggling this property will result in the sort order changing in
     * the TableView, assuming of course that this column is in the
     * sortOrder ObservableList to begin with.
     */
    private ObjectProperty<SortType> sortType;
    public final ObjectProperty<SortType> sortTypeProperty() {
        if (sortType == null) {
            sortType = new SimpleObjectProperty<SortType>(this, "sortType", SortType.ASCENDING);
        }
        return sortType;
    }
    public final void setSortType(SortType value) {
        sortTypeProperty().set(value);
    }
    public final SortType getSortType() {
        return sortType == null ? SortType.ASCENDING : sortType.get();
    }



    // --- On Edit Start
    private ObjectProperty<EventHandler<CellEditEvent<S,T>>> onEditStart;
    public final void setOnEditStart(EventHandler<CellEditEvent<S,T>> value) {
        onEditStartProperty().set(value);
    }
    public final EventHandler<CellEditEvent<S,T>> getOnEditStart() {
        return onEditStart == null ? null : onEditStart.get();
    }
    /**
     * This event handler will be fired when the user successfully initiates
     * editing.
     * @return the on edit start property
     */
    public final ObjectProperty<EventHandler<CellEditEvent<S,T>>> onEditStartProperty() {
        if (onEditStart == null) {
            onEditStart = new SimpleObjectProperty<EventHandler<CellEditEvent<S,T>>>(this, "onEditStart") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(TableColumn.<S,T>editStartEvent(), get());
                }
            };
        }
        return onEditStart;
    }


    // --- On Edit Commit
    private ObjectProperty<EventHandler<CellEditEvent<S,T>>> onEditCommit;
    public final void setOnEditCommit(EventHandler<CellEditEvent<S,T>> value) {
        onEditCommitProperty().set(value);
    }
    public final EventHandler<CellEditEvent<S,T>> getOnEditCommit() {
        return onEditCommit == null ? null : onEditCommit.get();
    }
    /**
     * This event handler will be fired when the user successfully commits their
     * editing.
     * @return the on edit commit property
     */
    public final ObjectProperty<EventHandler<CellEditEvent<S,T>>> onEditCommitProperty() {
        if (onEditCommit == null) {
            onEditCommit = new SimpleObjectProperty<EventHandler<CellEditEvent<S,T>>>(this, "onEditCommit") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(TableColumn.<S,T>editCommitEvent(), get());
                }
            };
        }
        return onEditCommit;
    }


    // --- On Edit Cancel
    private ObjectProperty<EventHandler<CellEditEvent<S,T>>> onEditCancel;
    public final void setOnEditCancel(EventHandler<CellEditEvent<S,T>> value) {
        onEditCancelProperty().set(value);
    }
    public final EventHandler<CellEditEvent<S,T>> getOnEditCancel() {
        return onEditCancel == null ? null : onEditCancel.get();
    }
    /**
     * This event handler will be fired when the user cancels editing a cell.
     * @return the on edit cancel property
     */
    public final ObjectProperty<EventHandler<CellEditEvent<S,T>>> onEditCancelProperty() {
        if (onEditCancel == null) {
            onEditCancel = new SimpleObjectProperty<EventHandler<CellEditEvent<S, T>>>(this, "onEditCancel") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(TableColumn.<S,T>editCancelEvent(), get());
                }
            };
        }
        return onEditCancel;
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public final ObservableList<TableColumn<S,?>> getColumns() {
        return columns;
    }

    /** {@inheritDoc} */
    @Override public final ObservableValue<T> getCellObservableValue(int index) {
        if (index < 0) return null;

        // Get the table
        final TableView<S> table = getTableView();
        if (table == null || table.getItems() == null) return null;

        // Get the rowData
        final List<S> items = table.getItems();
        if (index >= items.size()) return null; // Out of range

        final S rowData = items.get(index);
        return getCellObservableValue(rowData);
    }

    /** {@inheritDoc} */
    @Override public final ObservableValue<T> getCellObservableValue(S item) {
        // Get the factory
        final Callback<CellDataFeatures<S,T>, ObservableValue<T>> factory = getCellValueFactory();
        if (factory == null) return null;

        // Get the table
        final TableView<S> table = getTableView();
        if (table == null) return null;

        // Call the factory
        final CellDataFeatures<S,T> cdf = new CellDataFeatures<S,T>(table, this, item);
        return factory.call(cdf);
    }



    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "table-column";

    /**
     * {@inheritDoc}
     * @return "TableColumn"
     * @since JavaFX 8.0
     */
    @Override
    public String getTypeSelector() {
        return "TableColumn";
    }

    /**
     * {@inheritDoc}
     * @return {@code getTableView()}
     * @since JavaFX 8.0
     */
    @Override
    public Styleable getStyleableParent() {
        return getTableView();    }


    /**
     * {@inheritDoc}
    * @since JavaFX 8.0
    */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override public Node getStyleableNode() {
        if (! (getTableView().getSkin() instanceof TableViewSkin)) return null;
        TableViewSkin<?> skin = (TableViewSkin<?>) getTableView().getSkin();

        TableHeaderRow tableHeader = null;
        for (Node n : skin.getChildren()) {
            if (n instanceof TableHeaderRow) {
                tableHeader = (TableHeaderRow)n;
            }
        }

        NestedTableColumnHeader rootHeader = null;
        for (Node n : tableHeader.getChildren()) {
            if (n instanceof NestedTableColumnHeader) {
                rootHeader = (NestedTableColumnHeader) n;
            }
        }

        // we now need to do a search for the header. We'll go depth-first.
        return scan(rootHeader);
    }

    private TableColumnHeader scan(TableColumnHeader header) {
        // firstly test that the parent isn't what we are looking for
        if (TableColumn.this.equals(header.getTableColumn())) {
            return header;
        }

        if (header instanceof NestedTableColumnHeader) {
            NestedTableColumnHeader parent = (NestedTableColumnHeader) header;
            for (int i = 0; i < parent.getColumnHeaders().size(); i++) {
                TableColumnHeader result = scan(parent.getColumnHeaders().get(i));
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }



    /***************************************************************************
     *                                                                         *
     * Support Interfaces                                                      *
     *                                                                         *
     **************************************************************************/

    /**
     * A support class used in TableColumn as a wrapper class
     * to provide all necessary information for a particular {@link Cell}. Once
     * instantiated, this class is immutable.
     *
     * @param <S> The TableView type
     * @param <T> The TableColumn type
     * @since JavaFX 2.0
     */
    public static class CellDataFeatures<S,T> {
        private final TableView<S> tableView;
        private final TableColumn<S,T> tableColumn;
        private final S value;

        /**
         * Instantiates a CellDataFeatures instance with the given properties
         * set as read-only values of this instance.
         *
         * @param tableView The TableView that this instance refers to.
         * @param tableColumn The TableColumn that this instance refers to.
         * @param value The value for a row in the TableView.
         */
        public CellDataFeatures(TableView<S> tableView,
                TableColumn<S,T> tableColumn, S value) {
            this.tableView = tableView;
            this.tableColumn = tableColumn;
            this.value = value;
        }

        /**
         * Returns the value passed in to the constructor.
         * @return the value passed in to the constructor
         */
        public S getValue() {
            return value;
        }

        /**
         * Returns the {@link TableColumn} passed in to the constructor.
         * @return the TableColumn passed in to the constructor
         */
        public TableColumn<S,T> getTableColumn() {
            return tableColumn;
        }

        /**
         * Returns the {@link TableView} passed in to the constructor.
         * @return the TableView passed in to the constructor
         */
        public TableView<S> getTableView() {
            return tableView;
        }
    }



    /**
     * An event that is fired when a user performs an edit on a table cell.
     * @param <S> The type of the TableView generic type
     * @param <T> The type of the content in all cells in this TableColumn
     * @since JavaFX 2.0
     */
    public static class CellEditEvent<S,T> extends Event {
        private static final long serialVersionUID = -609964441682677579L;

        /**
         * Common supertype for all cell edit event types.
         * @since JavaFX 8.0
         */
        public static final EventType<?> ANY = EDIT_ANY_EVENT;

        // represents the new value input by the end user. This is NOT the value
        // to go back into the TableView.items list - this new value represents
        // just the input for a single cell, so it is likely that it needs to go
        // back into a property within an item in the TableView.items list.
        private final T newValue;

        // The location of the edit event
        private transient final TablePosition<S,T> pos;

        /**
         * Creates a new event that can be subsequently fired to the relevant listeners.
         *
         * @param table The TableView on which this event occurred.
         * @param pos The position upon which this event occurred.
         * @param eventType The type of event that occurred.
         * @param newValue The value input by the end user.
         */
        public CellEditEvent(TableView<S> table, TablePosition<S,T> pos,
                EventType<CellEditEvent<S,T>> eventType, T newValue) {
            super(table, Event.NULL_SOURCE_TARGET, eventType);

            if (table == null) {
                throw new NullPointerException("TableView can not be null");
            }
            this.pos = pos;
            this.newValue = newValue;
        }

        /**
         * Returns the TableView upon which this event occurred.
         * @return The TableView control upon which this event occurred.
         */
        public TableView<S> getTableView() {
            return pos.getTableView();
        }

        /**
         * Returns the TableColumn upon which this event occurred.
         *
         * @return The TableColumn that the edit occurred in.
         */
        public TableColumn<S,T> getTableColumn() {
            return pos.getTableColumn();
        }

        /**
         * Returns the position upon which this event occurred.
         * @return The position upon which this event occurred.
         */
        public TablePosition<S,T> getTablePosition() {
            return pos;
        }

        /**
         * Returns the new value input by the end user. This is <b>not</b> the value
         * to go back into the TableView.items list - this new value represents
         * just the input for a single cell, so it is likely that it needs to go
         * back into a property within an item in the TableView.items list.
         *
         * @return An Object representing the new value input by the user.
         */
        public T getNewValue() {
            return newValue;
        }

        /**
         * Attempts to return the old value at the position referred to in the
         * TablePosition returned by {@link #getTablePosition()}. This may return
         * null for a number of reasons.
         *
         * @return Returns the value stored in the position being edited, or null
         *     if it can not be retrieved.
         */
        public T getOldValue() {
            S rowData = getRowValue();
            if (rowData == null || pos.getTableColumn() == null) {
                return null;
            }

            // if we are here, we now need to get the data for the specific column
            return (T) pos.getTableColumn().getCellData(rowData);
        }

        /**
         * Convenience method that returns the value for the row (that is, from
         * the TableView {@link TableView#itemsProperty() items} list), for the
         * row contained within the {@link TablePosition} returned in
         * {@link #getTablePosition()}.
         * @return the value for the row
         */
        public S getRowValue() {
            List<S> items = getTableView().getItems();
            if (items == null) return null;

            int row = pos.getRow();
            if (row < 0 || row >= items.size()) return null;

            return items.get(row);
        }
    }

    /**
     * Enumeration that specifies the type of sorting being applied to a specific
     * column.
     * @since JavaFX 2.0
     */
    public static enum SortType {
        /**
         * Column will be sorted in an ascending order.
         */
        ASCENDING,

        /**
         * Column will be sorted in a descending order.
         */
        DESCENDING;

        // UNSORTED
    }
}
