/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
import javafx.css.CssMetaData;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.skin.*;
import javafx.util.Callback;
import javafx.css.Styleable;

/**
 * A {@link TreeTableView} is made up of a number of TreeTableColumn instances. Each
 * TreeTableColumn in a {@link TreeTableView} is responsible for displaying
 * (and editing) the contents of that column. As well as being responsible for
 * displaying and editing data for a single column, a TreeTableColumn also
 * contains the necessary properties to:
 * <ul>
 *    <li>Be resized (using {@link #minWidthProperty() minWidth}/
 *    {@link #prefWidthProperty() prefWidth}/
 *    {@link #maxWidthProperty() maxWidth} and {@link #widthProperty() width} properties)
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
 * When creating a TreeTableColumn instance, perhaps the two most important properties
 * to set are the column {@link #textProperty() text} (what to show in the column
 * header area), and the column {@link #cellValueFactoryProperty() cell value factory}
 * (which is used to populate individual cells in the column). This can be
 * achieved using some variation on the following code:
 *
 * <pre>{@code
 * firstNameCol.setCellValueFactory(new Callback<CellDataFeatures<Person, String>, ObservableValue<String>>() {
 *     public ObservableValue<String> call(CellDataFeatures<Person, String> p) {
 *         // p.getValue() returns the TreeItem<Person> instance for a particular TreeTableView row,
 *         // p.getValue().getValue() returns the Person instance inside the TreeItem<Person>
 *         return p.getValue().getValue().firstNameProperty();
 *     }
 *  });
 * }}</pre>
 *
 * This approach assumes that the object returned from <code>p.getValue().getValue()</code>
 * has a JavaFX {@link ObservableValue} that can simply be returned. The benefit of this
 * is that the TableView will internally create bindings to ensure that,
 * should the returned {@link ObservableValue} change, the cell contents will be
 * automatically refreshed.
 *
 * <p>In situations where a TableColumn must interact with classes created before
 * JavaFX, or that generally do not wish to use JavaFX APIs for properties, it is
 * possible to wrap the returned value in a {@link ReadOnlyObjectWrapper} instance. For
 * example:
 *
 *<pre>{@code
 * firstNameCol.setCellValueFactory(new Callback<CellDataFeatures<Person, String>, ObservableValue<String>>() {
 *     public ObservableValue<String> call(CellDataFeatures<Person, String> p) {
 *         // p.getValue() returns the TreeItem<Person> instance for a particular TreeTableView row,
 *         // p.getValue().getValue() returns the Person instance inside the TreeItem<Person>
 *         return new ReadOnlyObjectWrapper(p.getValue().getValue().getFirstName());
 *     }
 *  });
 * }}</pre>
 *
 * It is hoped that over time there will be convenience cell value factories
 * developed and made available to developers. As of the JavaFX 2.0 release,
 * there is one such convenience class: {@link javafx.scene.control.cell.TreeItemPropertyValueFactory}.
 * This class removes the need to write the code above, instead relying on reflection to
 * look up a given property from a String. Refer to the
 * <code>TreeItemPropertyValueFactory</code> class documentation for more information
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
 * @see javafx.scene.control.cell.TreeItemPropertyValueFactory
 * @since JavaFX 8.0
 */
public class TreeTableColumn<S,T> extends TableColumnBase<TreeItem<S>,T> implements EventTarget {

    /* *************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * Parent event for any TreeTableColumn edit event.
     * @param <S> the type of the TreeTableView generic type
     * @param <T> the type of the content in all cells in this TreeTableColumn
     * @return the edit event
     */
    @SuppressWarnings("unchecked")
    public static <S,T> EventType<TreeTableColumn.CellEditEvent<S,T>> editAnyEvent() {
        return (EventType<TreeTableColumn.CellEditEvent<S,T>>) EDIT_ANY_EVENT;
    }
    private static final EventType<?> EDIT_ANY_EVENT =
            new EventType<>(Event.ANY, "TREE_TABLE_COLUMN_EDIT");

    /**
     * Indicates that the user has performed some interaction to start an edit
     * event, or alternatively the
     * {@link TreeTableView#edit(int, javafx.scene.control.TreeTableColumn)}
     * method has been called.
     * @param <S> the type of the TreeTableView generic type
     * @param <T> the type of the content in all cells in this TreeTableColumn
     * @return the edit start event
     */
    @SuppressWarnings("unchecked")
    public static <S,T> EventType<TreeTableColumn.CellEditEvent<S,T>> editStartEvent() {
        return (EventType<TreeTableColumn.CellEditEvent<S,T>>) EDIT_START_EVENT;
    }
    private static final EventType<?> EDIT_START_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_START");

    /**
     * Indicates that the editing has been canceled, meaning that no change should
     * be made to the backing data source.
     * @param <S> the type of the TreeTableView generic type
     * @param <T> the type of the content in all cells in this TreeTableColumn
     * @return the edit cancel event
     */
    @SuppressWarnings("unchecked")
    public static <S,T> EventType<TreeTableColumn.CellEditEvent<S,T>> editCancelEvent() {
        return (EventType<TreeTableColumn.CellEditEvent<S,T>>) EDIT_CANCEL_EVENT;
    }
    private static final EventType<?> EDIT_CANCEL_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_CANCEL");

    /**
     * Indicates that the editing has been committed by the user, meaning that
     * a change should be made to the backing data source to reflect the new
     * data.
     * @param <S> the type of the TreeTableView generic type
     * @param <T> the type of the content in all cells in this TreeTableColumn
     * @return the edit commit event
     */
    @SuppressWarnings("unchecked")
    public static <S,T> EventType<TreeTableColumn.CellEditEvent<S,T>> editCommitEvent() {
        return (EventType<TreeTableColumn.CellEditEvent<S,T>>) EDIT_COMMIT_EVENT;
    }
    private static final EventType<?> EDIT_COMMIT_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_COMMIT");



    /**
     * If no cellFactory is specified on a TreeTableColumn instance, then this one
     * will be used by default. At present it simply renders the TableCell item
     * property within the {@link TableCell#graphicProperty() graphic} property
     * if the {@link Cell#itemProperty() item} is a Node, or it simply calls
     * <code>toString()</code> if it is not null, setting the resulting string
     * inside the {@link Cell#textProperty() text} property.
     */
    public static final Callback<TreeTableColumn<?,?>, TreeTableCell<?,?>> DEFAULT_CELL_FACTORY =
            new Callback<TreeTableColumn<?,?>, TreeTableCell<?,?>>() {
        @Override public TreeTableCell<?,?> call(TreeTableColumn<?,?> param) {
            return new TreeTableCell() {
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



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default TreeTableColumn with default cell factory, comparator, and
     * onEditCommit implementation.
     */
    public TreeTableColumn() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        setOnEditCommit(DEFAULT_EDIT_COMMIT_HANDLER);

        // we listen to the columns list here to ensure that widths are
        // maintained properly, and to also set the column hierarchy such that
        // all children columns know that this TreeTableColumn is their parent.
        getColumns().addListener(weakColumnsListener);

        treeTableViewProperty().addListener(observable -> {
            // set all children of this tableView to have the same TableView
            // as this column
            for (TreeTableColumn<S, ?> tc : getColumns()) {
                tc.setTreeTableView(getTreeTableView());
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
     * Creates a TreeTableColumn with the text set to the provided string, with
     * default cell factory, comparator, and onEditCommit implementation.
     *
     * @param text The string to show when the TreeTableColumn is placed within
     *      the TreeTableView.
     */
    public TreeTableColumn(String text) {
        this();
        setText(text);
    }



    /* *************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private EventHandler<TreeTableColumn.CellEditEvent<S,T>> DEFAULT_EDIT_COMMIT_HANDLER =
            t -> {
                ObservableValue<T> ov = getCellObservableValue(t.getRowValue());
                if (ov instanceof WritableValue) {
                    ((WritableValue)ov).setValue(t.getNewValue());
                }
            };

    private ListChangeListener<TreeTableColumn<S, ?>> columnsListener = new ListChangeListener<TreeTableColumn<S,?>>() {
        @Override public void onChanged(ListChangeListener.Change<? extends TreeTableColumn<S,?>> c) {
            while (c.next()) {
                // update the TreeTableColumn.treeTableView property
                for (TreeTableColumn<S,?> tc : c.getRemoved()) {
                    // Fix for RT-16978. In TableColumnHeader we add before we
                    // remove when moving a TreeTableColumn. This means that for
                    // a very brief moment the tc is duplicated, and we can prevent
                    // nulling out the tableview and parent column. Without this
                    // here, in a very special circumstance it is possible to null
                    // out the entire content of a column by reordering and then
                    // sorting another column.
                    if (getColumns().contains(tc)) continue;

                    tc.setTreeTableView(null);
                    tc.setParentColumn(null);
                }
                for (TreeTableColumn<S,?> tc : c.getAddedSubList()) {
                    tc.setTreeTableView(getTreeTableView());
                }

                updateColumnWidths();
            }
        }
    };

    private WeakListChangeListener<TreeTableColumn<S, ?>> weakColumnsListener =
            new WeakListChangeListener<>(columnsListener);


    /* *************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    // Contains any children columns that should be nested within this column
    private final ObservableList<TreeTableColumn<S,?>> columns = FXCollections.<TreeTableColumn<S,?>>observableArrayList();



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/


    // --- TreeTableView
    /**
     * The TreeTableView that this TreeTableColumn belongs to.
     */
    private ReadOnlyObjectWrapper<TreeTableView<S>> treeTableView =
            new ReadOnlyObjectWrapper<TreeTableView<S>>(this, "treeTableView");
    public final ReadOnlyObjectProperty<TreeTableView<S>> treeTableViewProperty() {
        return treeTableView.getReadOnlyProperty();
    }
    final void setTreeTableView(TreeTableView<S> value) { treeTableView.set(value); }
    public final TreeTableView<S> getTreeTableView() { return treeTableView.get(); }



    // --- Cell value factory
    /**
     * The cell value factory needs to be set to specify how to populate all
     * cells within a single TreeTableColumn. A cell value factory is a {@link Callback}
     * that provides a {@link CellDataFeatures} instance, and expects an
     * {@link ObservableValue} to be returned. The returned ObservableValue instance
     * will be observed internally to allow for updates to the value to be
     * immediately reflected on screen.
     *
     * <p>An example of how to set a cell value factory is:
     *
     * <pre>{@code
     * firstNameCol.setCellValueFactory(new Callback<CellDataFeatures<Person, String>, ObservableValue<String>>() {
     *     public ObservableValue<String> call(CellDataFeatures<Person, String> p) {
     *         // p.getValue() returns the TreeItem<Person> instance for a particular TreeTableView row,
     *         // p.getValue().getValue() returns the Person instance inside the TreeItem<Person>
     *         return p.getValue().getValue().firstNameProperty();
     *     }
     *  });
     * }}</pre>
     *
     * A common approach is to want to populate cells in a TreeTableColumn using
     * a single value from a Java bean. To support this common scenario, there
     * is the {@link javafx.scene.control.cell.TreeItemPropertyValueFactory} class.
     * Refer to this class for more information on how to use it, but briefly
     * here is how the above use case could be simplified using the TreeItemPropertyValueFactory class:
     *
     * <pre><code>
     * firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory&lt;Person,String&gt;("firstName"));
     * </code></pre>
     *
     * @see javafx.scene.control.cell.TreeItemPropertyValueFactory
     */
    private ObjectProperty<Callback<TreeTableColumn.CellDataFeatures<S,T>, ObservableValue<T>>> cellValueFactory;
    public final void setCellValueFactory(Callback<TreeTableColumn.CellDataFeatures<S,T>, ObservableValue<T>> value) {
        cellValueFactoryProperty().set(value);
    }
    public final Callback<TreeTableColumn.CellDataFeatures<S,T>, ObservableValue<T>> getCellValueFactory() {
        return cellValueFactory == null ? null : cellValueFactory.get();
    }
    public final ObjectProperty<Callback<TreeTableColumn.CellDataFeatures<S,T>, ObservableValue<T>>> cellValueFactoryProperty() {
        if (cellValueFactory == null) {
            cellValueFactory = new SimpleObjectProperty<Callback<TreeTableColumn.CellDataFeatures<S,T>, ObservableValue<T>>>(this, "cellValueFactory");
        }
        return cellValueFactory;
    }


    // --- Cell Factory
    /**
     * The cell factory for all cells in this column. The cell factory
     * is responsible for rendering the data contained within each TreeTableCell
     * for a single TreeTableColumn.
     *
     * <p>By default TreeTableColumn uses a {@link #DEFAULT_CELL_FACTORY default cell
     * factory}, but this can be replaced with a custom implementation, for
     * example to show data in a different way or to support editing. There is a
     * lot of documentation on creating custom cell factories
     * elsewhere (see {@link Cell} and {@link TreeTableView} for example).</p>
     *
     * <p>Finally, there are a number of pre-built cell factories available in the
     * {@link javafx.scene.control.cell} package.
     *
     */
    private final ObjectProperty<Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>>> cellFactory =
        new SimpleObjectProperty<Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>>>(
            this, "cellFactory", (Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>>) ((Callback) DEFAULT_CELL_FACTORY)) {
                @Override protected void invalidated() {
                    TreeTableView<S> table = getTreeTableView();
                    if (table == null) return;
                    Map<Object,Object> properties = table.getProperties();
                    if (properties.containsKey(Properties.RECREATE)) {
                        properties.remove(Properties.RECREATE);
                    }
                    properties.put(Properties.RECREATE, Boolean.TRUE);
                }
            };
    public final void setCellFactory(Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>> value) {
        cellFactory.set(value);
    }
    public final Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>> getCellFactory() {
        return cellFactory.get();
    }
    public final ObjectProperty<Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>>> cellFactoryProperty() {
        return cellFactory;
    }


    // --- Sort Type
    /**
     * Used to state whether this column, if it is part of a sort order (see
     * {@link TreeTableView#getSortOrder()} for more details), should be sorted
     * in ascending or descending order.
     * Simply toggling this property will result in the sort order changing in
     * the TreeTableView, assuming of course that this column is in the
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
    /**
     * This event handler will be fired when the user successfully initiates
     * editing.
     */
    private ObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<S,T>>> onEditStart;
    public final void setOnEditStart(EventHandler<TreeTableColumn.CellEditEvent<S,T>> value) {
        onEditStartProperty().set(value);
    }
    public final EventHandler<TreeTableColumn.CellEditEvent<S,T>> getOnEditStart() {
        return onEditStart == null ? null : onEditStart.get();
    }
    public final ObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<S,T>>> onEditStartProperty() {
        if (onEditStart == null) {
            onEditStart = new SimpleObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<S,T>>>(this, "onEditStart") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(TreeTableColumn.<S,T>editStartEvent(), get());
                }
            };
        }
        return onEditStart;
    }


    // --- On Edit Commit
    /**
     * This event handler will be fired when the user successfully commits their
     * editing.
     */
    private ObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<S,T>>> onEditCommit;
    public final void setOnEditCommit(EventHandler<TreeTableColumn.CellEditEvent<S,T>> value) {
        onEditCommitProperty().set(value);
    }
    public final EventHandler<TreeTableColumn.CellEditEvent<S,T>> getOnEditCommit() {
        return onEditCommit == null ? null : onEditCommit.get();
    }
    public final ObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<S,T>>> onEditCommitProperty() {
        if (onEditCommit == null) {
            onEditCommit = new SimpleObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<S,T>>>(this, "onEditCommit") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(TreeTableColumn.<S,T>editCommitEvent(), get());
                }
            };
        }
        return onEditCommit;
    }


    // --- On Edit Cancel
    /**
     * This event handler will be fired when the user cancels editing a cell.
     */
    private ObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<S,T>>> onEditCancel;
    public final void setOnEditCancel(EventHandler<TreeTableColumn.CellEditEvent<S,T>> value) {
        onEditCancelProperty().set(value);
    }
    public final EventHandler<TreeTableColumn.CellEditEvent<S,T>> getOnEditCancel() {
        return onEditCancel == null ? null : onEditCancel.get();
    }
    public final ObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<S,T>>> onEditCancelProperty() {
        if (onEditCancel == null) {
            onEditCancel = new SimpleObjectProperty<EventHandler<TreeTableColumn.CellEditEvent<S,T>>>(this, "onEditCancel") {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(TreeTableColumn.<S,T>editCancelEvent(), get());
                }
            };
        }
        return onEditCancel;
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public final ObservableList<TreeTableColumn<S,?>> getColumns() {
        return columns;
    }

    /** {@inheritDoc} */
    @Override public final ObservableValue<T> getCellObservableValue(int index) {
        if (index < 0) return null;

        // Get the table
        final TreeTableView<S> table = getTreeTableView();
        if (table == null || index >= table.getExpandedItemCount()) return null;

        // Get the rowData
        TreeItem<S> item = table.getTreeItem(index);
        return getCellObservableValue(item);
    }

    /** {@inheritDoc} */
    @Override public final ObservableValue<T> getCellObservableValue(TreeItem<S> item) {
        // Get the factory
        final Callback<TreeTableColumn.CellDataFeatures<S,T>, ObservableValue<T>> factory = getCellValueFactory();
        if (factory == null) return null;

        // Get the table
        final TreeTableView<S> table = getTreeTableView();
        if (table == null) return null;

        // Call the factory
        final TreeTableColumn.CellDataFeatures<S,T> cdf = new TreeTableColumn.CellDataFeatures<S,T>(table, this, item);
        return factory.call(cdf);
    }



    /* *************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/



    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "table-column";

    /**
     * {@inheritDoc}
     * @return "TreeTableColumn"
     */
    @Override public String getTypeSelector() {
        return "TreeTableColumn";
    }

    /**
     * {@inheritDoc}
     * @return {@code getTreeTableView()}
     */
    @Override public Styleable getStyleableParent() {
        return getTreeTableView();
    }

    /**
     * {@inheritDoc}
    */
    @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override public Node getStyleableNode() {
        if (! (getTreeTableView().getSkin() instanceof TreeTableViewSkin)) return null;
        TreeTableViewSkin<?> skin = (TreeTableViewSkin<?>) getTreeTableView().getSkin();

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
        if (TreeTableColumn.this.equals(header.getTableColumn())) {
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



    /* *************************************************************************
     *                                                                         *
     * Support Interfaces                                                      *
     *                                                                         *
     **************************************************************************/

    /**
     * A support class used in TreeTableColumn as a wrapper class
     * to provide all necessary information for a particular {@link Cell}. Once
     * instantiated, this class is immutable.
     *
     * @param <S> The TableView type
     * @param <T> The TreeTableColumn type
     * @since JavaFX 8.0
     */
    public static class CellDataFeatures<S,T> {
        private final TreeTableView<S> treeTableView;
        private final TreeTableColumn<S,T> tableColumn;
        private final TreeItem<S> value;

        /**
         * Instantiates a CellDataFeatures instance with the given properties
         * set as read-only values of this instance.
         *
         * @param treeTableView The TableView that this instance refers to.
         * @param tableColumn The TreeTableColumn that this instance refers to.
         * @param value The value for a row in the TableView.
         */
        public CellDataFeatures(TreeTableView<S> treeTableView,
                TreeTableColumn<S,T> tableColumn, TreeItem<S> value) {
            this.treeTableView = treeTableView;
            this.tableColumn = tableColumn;
            this.value = value;
        }

        /**
         * Returns the value passed in to the constructor.
         * @return the value passed in to the constructor
         */
        public TreeItem<S> getValue() {
            return value;
        }

        /**
         * Returns the {@link TreeTableColumn} passed in to the constructor.
         * @return the {@link TreeTableColumn} passed in to the constructor
         */
        public TreeTableColumn<S,T> getTreeTableColumn() {
            return tableColumn;
        }

        /**
         * Returns the {@link TableView} passed in to the constructor.
         * @return the {@link TableView} passed in to the constructor
         */
        public TreeTableView<S> getTreeTableView() {
            return treeTableView;
        }
    }



    /**
     * An event that is fired when a user performs an edit on a table cell.
     * @since JavaFX 8.0
     */
    public static class CellEditEvent<S,T> extends Event {
        private static final long serialVersionUID = -609964441682677579L;

        /**
         * Common supertype for all cell edit event types.
         */
        public static final EventType<?> ANY = EDIT_ANY_EVENT;

        // represents the new value input by the end user. This is NOT the value
        // to go back into the TableView.items list - this new value represents
        // just the input for a single cell, so it is likely that it needs to go
        // back into a property within an item in the TableView.items list.
        private final T newValue;

        // The location of the edit event
        private transient final TreeTablePosition<S,T> pos;

        /**
         * Creates a new event that can be subsequently fired to the relevant listeners.
         *
         * @param table The TableView on which this event occurred.
         * @param pos The position upon which this event occurred.
         * @param eventType The type of event that occurred.
         * @param newValue The value input by the end user.
         */
        public CellEditEvent(TreeTableView<S> table, TreeTablePosition<S,T> pos,
                EventType<TreeTableColumn.CellEditEvent<S,T>> eventType, T newValue) {
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
        public TreeTableView<S> getTreeTableView() {
            return pos.getTreeTableView();
        }

        /**
         * Returns the TreeTableColumn upon which this event occurred.
         *
         * @return The TreeTableColumn that the edit occurred in.
         */
        public TreeTableColumn<S,T> getTableColumn() {
            return pos.getTableColumn();
        }

        /**
         * Returns the position upon which this event occurred.
         * @return The position upon which this event occurred.
         */
        public TreeTablePosition<S,T> getTreeTablePosition() {
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
         * TablePosition returned by {@link #getTreeTablePosition()}. This may return
         * null for a number of reasons.
         *
         * @return Returns the value stored in the position being edited, or null
         *     if it can not be retrieved.
         */
        public T getOldValue() {
            TreeItem<S> rowData = getRowValue();
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
         * {@link #getTreeTablePosition()}.
         * @return the row value
         */
        public TreeItem<S> getRowValue() {
//            List<S> items = getTreeTableView().getItems();
//            if (items == null) return null;

            TreeTableView<S> treeTable = getTreeTableView();
            int row = pos.getRow();
            if (row < 0 || row >= treeTable.getExpandedItemCount()) return null;

            return treeTable.getTreeItem(row);
        }
    }

    /**
     * Enumeration that specifies the type of sorting being applied to a specific
     * column.
     * @since JavaFX 8.0
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
