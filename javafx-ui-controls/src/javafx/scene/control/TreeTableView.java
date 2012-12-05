/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.MappingChange;
import com.sun.javafx.collections.annotations.ReturnsUnmodifiableCollection;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.control.ReadOnlyUnbackedObservableList;
import com.sun.javafx.scene.control.TableColumnComparator;
import javafx.event.WeakEventHandler;
import com.sun.javafx.scene.control.skin.TreeTableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualContainerBase;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.util.Callback;

/**
 * The TreeTableView control is designed to visualize an unlimited number of rows
 * of data, broken out into columns. A TreeTableView is therefore very similar to the
 * {@link ListView} and {@link TableView} controls. For an
 * example on how to create a TreeTableView, refer to the 'Creating a TreeTableView'
 * control section below.
 *
 * <p>The TreeTableView control has a number of features, including:
 * <ul>
 * <li>Powerful {@link TreeTableColumn} API:
 *   <ul>
 *   <li>Support for {@link TreeTableColumn#cellFactoryProperty() cell factories} to
 *      easily customize {@link Cell cell} contents in both rendering and editing
 *      states.
 *   <li>Specification of {@link #minWidthProperty() minWidth}/
 *      {@link #prefWidthProperty() prefWidth}/{@link #maxWidthProperty() maxWidth},
 *      and also {@link TreeTableColumn#resizableProperty() fixed width columns}.
 *   <li>Width resizing by the user at runtime.
 *   <li>Column reordering by the user at runtime.
 *   <li>Built-in support for {@link TreeTableColumn#getColumns() column nesting}
 *   </ul>
 * <li>Different {@link #columnResizePolicyProperty() resizing policies} to 
 *      dictate what happens when the user resizes columns.
 * <li>Support for {@link #getSortOrder() multiple column sorting} by clicking 
 *      the column header (hold down Shift keyboard key whilst clicking on a 
 *      header to sort by multiple columns).
 * </ul>
 * </p>
 *
 * <p>Note that TreeTableView is intended to be used to visualize data - it is not
 * intended to be used for laying out your user interface. If you want to lay
 * your user interface out in a grid-like fashion, consider the 
 * {@link GridPane} layout.</p>
 *
 * <h2>Creating a TreeTableView</h2>
 * 
 * TODO update to a relevant example
 *
 * <p>Creating a TreeTableView is a multi-step process, and also depends on the
 * underlying data model needing to be represented. For this example we'll use
 * the TreeTableView to visualise a file system, and will therefore make use
 * of an imaginary (and vastly simplified) File class as defined below:
 * 
 * <pre>
 * {@code
 * public class File {
 *     private StringProperty name;
 *     public void setName(String value) { nameProperty().set(value); }
 *     public String getName() { return nameProperty().get(); }
 *     public StringProperty nameProperty() { 
 *         if (name == null) name = new SimpleStringProperty(this, "name");
 *         return name; 
 *     }
 * 
 *     private DoubleProperty lastModified;
 *     public void setLastModified(Double value) { lastModifiedProperty().set(value); }
 *     public DoubleProperty getLastModified() { return lastModifiedProperty().get(); }
 *     public DoubleProperty lastModifiedProperty() { 
 *         if (lastModified == null) lastModified = new SimpleDoubleProperty(this, "lastModified");
 *         return lastModified; 
 *     } 
 * }}</pre>
 * 
 * <p>Firstly, a TreeTableView instance needs to be defined, as such:
 * 
 * <pre>
 * {@code
 * TreeTableView<File> treeTable = new TreeTableView<File>();}</pre>
 *
 * <p>With the basic tree table defined, we next focus on the data model. As mentioned,
 * for this example, we'll be representing a file system using File instances. To
 * do this, we need to define the root node of the tree table, as such:
 *
 * <pre>
 * {@code
 * TreeItem<File> root = new TreeItem<File>(new File("/"));
 * treeTable.setRoot(root);}</pre>
 * 
 * <p>With the root set as such, the TreeTableView will automatically update whenever
 * the {@link TreeItem#getChildren() children} of the root changes. 
 * 
 * <p>At this point we now have a TreeTableView hooked up to observe the root 
 * TreeItem instance. The missing ingredient 
 * now is the means of splitting out the data contained within the model and 
 * representing it in one or more {@link TreeTableColumn} instances. To 
 * create a two-column TreeTableView to show the file name and last modified 
 * properties, we extend the code shown above as follows:
 * 
 * <pre>
 * {@code
 * TreeItem<File> root = new TreeItem<File>(new File("/"));
 * treeTable.setRoot(root);
 * 
 * // TODO this is not valid TreeTableView code
 * TreeTableColumns<Person,String> firstNameCol = new TreeTableColumns<Person,String>("First Name");
 * firstNameCol.setCellValueFactory(new PropertyValueFactory("firstName"));
 * TreeTableColumns<Person,String> lastNameCol = new TreeTableColumns<Person,String>("Last Name");
 * lastNameCol.setCellValueFactory(new PropertyValueFactory("lastName"));
 * 
 * table.getColumns().setAll(firstNameCol, lastNameCol);}</pre>
 * 
 * <p>With the code shown above we have fully defined the minimum properties
 * required to create a TreeTableView instance. Running this code (assuming the
 * file system structure is probably built up in memory) will result in a TreeTableView being
 * shown with two columns for name and lastModified. Any other properties of the
 * File class will not be shown, as no TreeTableColumnss are defined for them.
 * 
 * <h3>TreeTableView support for classes that don't contain properties</h3>
 *
 * // TODO update - this is not correct for TreeTableView
 * 
 * <p>The code shown above is the shortest possible code for creating a TreeTableView
 * when the domain objects are designed with JavaFX properties in mind 
 * (additionally, {@link javafx.scene.control.cell.PropertyValueFactory} supports
 * normal JavaBean properties too, although there is a caveat to this, so refer 
 * to the class documentation for more information). When this is not the case, 
 * it is necessary to provide a custom cell value factory. More information
 * about cell value factories can be found in the {@link TreeTableColumns} API 
 * documentation, but briefly, here is how a TreeTableColumns could be specified:
 * 
 * <pre>
 * {@code
 * firstNameCol.setCellValueFactory(new Callback<CellDataFeatures<Person, String>, ObservableValue<String>>() {
 *     public ObservableValue<String> call(CellDataFeatures<Person, String> p) {
 *         // p.getValue() returns the Person instance for a particular TreeTableView row
 *         return p.getValue().firstNameProperty();
 *     }
 *  });
 * }}</pre>
 * 
 * <h3>TreeTableView Selection / Focus APIs</h3>
 * <p>To track selection and focus, it is necessary to become familiar with the
 * {@link SelectionModel} and {@link FocusModel} classes. A TreeTableView has at most
 * one instance of each of these classes, available from 
 * {@link #selectionModelProperty() selectionModel} and 
 * {@link #focusModelProperty() focusModel} properties respectively.
 * Whilst it is possible to use this API to set a new selection model, in
 * most circumstances this is not necessary - the default selection and focus
 * models should work in most circumstances.
 * 
 * <p>The default {@link SelectionModel} used when instantiating a TreeTableView is
 * an implementation of the {@link MultipleSelectionModel} abstract class. 
 * However, as noted in the API documentation for
 * the {@link MultipleSelectionModel#selectionModeProperty() selectionMode}
 * property, the default value is {@link SelectionMode#SINGLE}. To enable 
 * multiple selection in a default TreeTableView instance, it is therefore necessary
 * to do the following:
 * 
 * <pre>
 * {@code 
 * treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);}</pre>
 *
 * <h3>Customizing TreeTableView Visuals</h3>
 * <p>The visuals of the TreeTableView can be entirely customized by replacing the 
 * default {@link #rowFactoryProperty() row factory}. A row factory is used to
 * generate {@link TreeTableRow} instances, which are used to represent an entire
 * row in the TreeTableView. 
 * 
 * <p>In many cases, this is not what is desired however, as it is more commonly
 * the case that cells be customized on a per-column basis, not a per-row basis.
 * It is therefore important to note that a {@link TreeTableRow} is not a 
 * {@link TreeTableCell}. A  {@link TreeTableRow} is simply a container for zero or more
 * {@link TreeTableCell}, and in most circumstances it is more likely that you'll 
 * want to create custom TreeTableCells, rather than TreeTableRows. The primary use case
 * for creating custom TreeTableRow instances would most probably be to introduce
 * some form of column spanning support.
 * 
 * <p>You can create custom {@link TreeTableCell} instances per column by assigning 
 * the appropriate function to the TreeTableColumns
 * {@link TreeTableColumns#cellFactoryProperty() cell factory} property.
 * 
 * <p>See the {@link Cell} class documentation for a more complete
 * description of how to write custom Cells.
 *
 * @see TreeTableColumn
 * @see TreeTablePosition
 * @param <S> The type of the TreeItem instances used in this TreeTableView.
 */
@DefaultProperty("root")
public class TreeTableView<S> extends Control {
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates an empty TreeTableView.
     * 
     * <p>Refer to the {@link TreeTableView} class documentation for details on the
     * default state of other properties.
     */
    public TreeTableView() {
        this(null);
    }

    /**
     * Creates a TreeTableView with the provided root node.
     * 
     * <p>Refer to the {@link TreeTableView} class documentation for details on the
     * default state of other properties.
     * 
     * @param root The node to be the root in this TreeTableView.
     */
    public TreeTableView(TreeItem<S> root) {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);

        setRoot(root);
        updateTreeItemCount(root);

        // install default selection and focus models - it's unlikely this will be changed
        // by many users.
        setSelectionModel(new TreeTableViewArrayListSelectionModel<S>(this));
        setFocusModel(new TreeTableViewFocusModel<S>(this));
        
        // we watch the columns list, such that when it changes we can update
        // the leaf columns and visible leaf columns lists (which are read-only).
        getColumns().addListener(weakColumnsObserver);
        getColumns().addListener(new ListChangeListener<TreeTableColumn<S,?>>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends TreeTableColumn<S,?>> c) {
                while (c.next()) {
                    // update the TreeTableColumn.tableView property
                    for (TreeTableColumn<S,?> tc : c.getRemoved()) {
                        tc.setTreeTableView(null);
                    }
                    for (TreeTableColumn<S,?> tc : c.getAddedSubList()) {
                        tc.setTreeTableView(TreeTableView.this);
                    }

                    // set up listeners
                    TableUtil.removeTableColumnListener(c.getRemoved(),
                            weakColumnVisibleObserver,
                            weakColumnSortableObserver,
                            weakColumnSortTypeObserver);
                    TableUtil.addTableColumnListener(c.getAddedSubList(),
                            weakColumnVisibleObserver,
                            weakColumnSortableObserver,
                            weakColumnSortTypeObserver);
                }
                    
                // We don't maintain a bind for leafColumns, we simply call this update
                // function behind the scenes in the appropriate places.
                updateVisibleLeafColumns();
            }
        });

        // watch for changes to the sort order list - and when it changes run
        // the sort method.
        getSortOrder().addListener(new ListChangeListener<TreeTableColumn<S,?>>() {
            @Override public void onChanged(ListChangeListener.Change<? extends TreeTableColumn<S,?>> c) {
                sort();
            }
        });

        // We're watching for changes to the content width such
        // that the resize policy can be run if necessary. This comes from
        // TreeTableViewSkin.
        getProperties().addListener(new MapChangeListener<Object, Object>() {
            @Override
            public void onChanged(Change<? extends Object, ? extends Object> c) {
                if (c.wasAdded() && TableView.SET_CONTENT_WIDTH.equals(c.getKey())) {
                    if (c.getValueAdded() instanceof Number) {
                        setContentWidth((Double) c.getValueAdded());
                    }
                    getProperties().remove(TableView.SET_CONTENT_WIDTH);
                }
            }
        });

        isInited = true;
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/
    
    /** 
     * An EventType that indicates some edit event has occurred. It is the parent
     * type of all other edit events: {@link #editStartEvent},
     *  {@link #editCommitEvent} and {@link #editCancelEvent}.
     * 
     * @return An EventType that indicates some edit event has occurred.
     */
    @SuppressWarnings("unchecked")
    public static <S> EventType<TreeTableView.EditEvent<S>> editAnyEvent() {
        return (EventType<TreeTableView.EditEvent<S>>) EDIT_ANY_EVENT;
    }
    private static final EventType<?> EDIT_ANY_EVENT =
            new EventType(Event.ANY, "TREE_TABLE_VIEW_EDIT");

    /**
     * An EventType used to indicate that an edit event has started within the
     * TreeTableView upon which the event was fired.
     * 
     * @return An EventType used to indicate that an edit event has started.
     */
    @SuppressWarnings("unchecked")
    public static <S> EventType<TreeTableView.EditEvent<S>> editStartEvent() {
        return (EventType<TreeTableView.EditEvent<S>>) EDIT_START_EVENT;
    }
    private static final EventType<?> EDIT_START_EVENT =
            new EventType(editAnyEvent(), "EDIT_START");

    /**
     * An EventType used to indicate that an edit event has just been canceled
     * within the TreeTableView upon which the event was fired.
     * 
     * @return An EventType used to indicate that an edit event has just been
     *      canceled.
     */
    @SuppressWarnings("unchecked")
    public static <S> EventType<TreeTableView.EditEvent<S>> editCancelEvent() {
        return (EventType<TreeTableView.EditEvent<S>>) EDIT_CANCEL_EVENT;
    }
    private static final EventType<?> EDIT_CANCEL_EVENT =
            new EventType(editAnyEvent(), "EDIT_CANCEL");

    /**
     * An EventType that is used to indicate that an edit in a TreeTableView has been
     * committed. This means that user has made changes to the data of a
     * TreeItem, and that the UI should be updated.
     * 
     * @return An EventType that is used to indicate that an edit in a TreeTableView
     *      has been committed.
     */
    @SuppressWarnings("unchecked")
    public static <S> EventType<TreeTableView.EditEvent<S>> editCommitEvent() {
        return (EventType<TreeTableView.EditEvent<S>>) EDIT_COMMIT_EVENT;
    }
    private static final EventType<?> EDIT_COMMIT_EVENT =
            new EventType(editAnyEvent(), "EDIT_COMMIT");
    
    /**
     * Returns the number of levels of 'indentation' of the given TreeItem, 
     * based on how many times getParent() can be recursively called. If the 
     * given TreeItem is the root node, or if the TreeItem does not have any 
     * parent set, the returned value will be zero. For each time getParent() is 
     * recursively called, the returned value is incremented by one.
     * 
     * @param node The TreeItem for which the level is needed.
     * @return An integer representing the number of parents above the given node,
     *         or -1 if the given TreeItem is null.
     */
    public static int getNodeLevel(TreeItem<?> node) {
        return TreeView.getNodeLevel(node);
    }

    /**
     * <p>Very simple resize policy that just resizes the specified column by the
     * provided delta and shifts all other columns (to the right of the given column)
     * further to the right (when the delta is positive) or to the left (when the
     * delta is negative).
     *
     * <p>It also handles the case where we have nested columns by sharing the new space,
     * or subtracting the removed space, evenly between all immediate children columns.
     * Of course, the immediate children may themselves be nested, and they would
     * then use this policy on their children.
     */
    public static final Callback<TreeTableView.ResizeFeatures, Boolean> UNCONSTRAINED_RESIZE_POLICY = 
            new Callback<TreeTableView.ResizeFeatures, Boolean>() {
        
        @Override public String toString() {
            return "unconstrained-resize";
        }
        
        @Override public Boolean call(TreeTableView.ResizeFeatures prop) {
            double result = TableUtil.resize(prop.getColumn(), prop.getDelta());
            return Double.compare(result, 0.0) == 0;
        }
    };

    /**
     * <p>Simple policy that ensures the width of all visible leaf columns in 
     * this table sum up to equal the width of the table itself.
     * 
     * <p>When the user resizes a column width with this policy, the table automatically
     * adjusts the width of the right hand side columns. When the user increases a
     * column width, the table decreases the width of the rightmost column until it
     * reaches its minimum width. Then it decreases the width of the second
     * rightmost column until it reaches minimum width and so on. When all right
     * hand side columns reach minimum size, the user cannot increase the size of
     * resized column any more.
     */
    public static final Callback<TreeTableView.ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY = 
            new Callback<TreeTableView.ResizeFeatures, Boolean>() {

        private boolean isFirstRun = true;
        
        @Override public String toString() {
            return "constrained-resize";
        }
        
        @Override public Boolean call(TreeTableView.ResizeFeatures prop) {
            TreeTableView<?> table = prop.getTable();
            List<? extends TableColumnBase<?,?>> visibleLeafColumns = table.getVisibleLeafColumns();
            Boolean result = TableUtil.constrainedResize(prop, 
                                               isFirstRun, 
                                               table.contentWidth,
                                               visibleLeafColumns);
            isFirstRun = false;
            return result;
        }
    };
    
    
    
    /***************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/    
    
    // used in the tree item modification event listener. Used by the 
    // layoutChildren method to determine whether the tree item count should
    // be recalculated.
    private boolean treeItemCountDirty = true;

    // this is the only publicly writable list for columns. This represents the
    // columns as they are given initially by the developer.
    private final ObservableList<TreeTableColumn<S,?>> columns = FXCollections.observableArrayList();

    // Finally, as convenience, we also have an observable list that contains
    // only the leaf columns that are currently visible.
    private final ObservableList<TreeTableColumn<S,?>> visibleLeafColumns = FXCollections.observableArrayList();
    private final ObservableList<TreeTableColumn<S,?>> unmodifiableVisibleLeafColumns = FXCollections.unmodifiableObservableList(visibleLeafColumns);
    
    // Allows for multiple column sorting based on the order of the TreeTableColumns
    // in this observableArrayList. Each TreeTableColumn is responsible for whether it is
    // sorted using ascending or descending order.
    private ObservableList<TreeTableColumn<S,?>> sortOrder = FXCollections.observableArrayList();

    // width of VirtualFlow minus the vbar width
    private double contentWidth;
    
    // Used to minimise the amount of work performed prior to the table being
    // completely initialised. In particular it reduces the amount of column
    // resize operations that occur, which slightly improves startup time.
    private boolean isInited = false;
    
    
    
    /***************************************************************************
     *                                                                         *
     * Callbacks and Events                                                    *
     *                                                                         *
     **************************************************************************/
    
    // we use this to forward events that have bubbled up TreeItem instances
    // to the TreeTableViewSkin, to force it to recalculate teh item count and redraw
    // if necessary
    private final EventHandler<TreeItem.TreeModificationEvent<S>> rootEvent = new EventHandler<TreeItem.TreeModificationEvent<S>>() {
        @Override public void handle(TreeItem.TreeModificationEvent<S> e) {
            // this forces layoutChildren at the next pulse, and therefore
            // updates the item count if necessary
            EventType eventType = e.getEventType();
            boolean match = false;
            while (eventType != null) {
                if (eventType.equals(TreeItem.<S>treeItemCountChangeEvent())) {
                    match = true;
                    break;
                }
                eventType = eventType.getSuperType();
            }
            
            if (match) {
                treeItemCountDirty = true;
                requestLayout();
            }
        }
    };
    
    private final ListChangeListener<TreeTableColumn<S,?>> columnsObserver = new ListChangeListener<TreeTableColumn<S,?>>() {
        @Override public void onChanged(ListChangeListener.Change<? extends TreeTableColumn<S,?>> c) {
            updateVisibleLeafColumns();
            
            // Fix for RT-15194: Need to remove removed columns from the 
            // sortOrder list.
            while (c.next()) {
                TableUtil.removeColumnsListener(c.getRemoved(), weakColumnsObserver);
                TableUtil.addColumnsListener(c.getAddedSubList(), weakColumnsObserver);
                
                if (c.wasRemoved()) {
                    for (int i = 0; i < c.getRemovedSize(); i++) {
                        getSortOrder().remove(c.getRemoved().get(i));
                    }
                }
            }
        }
    };
    
    private final InvalidationListener columnVisibleObserver = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            updateVisibleLeafColumns();
        }
    };
    
    private final InvalidationListener columnSortableObserver = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            TreeTableColumn col = (TreeTableColumn) ((BooleanProperty)valueModel).getBean();
            if (! getSortOrder().contains(col)) return;
            sort();
        }
    };

    private final InvalidationListener columnSortTypeObserver = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            TreeTableColumn col = (TreeTableColumn) ((ObjectProperty)valueModel).getBean();
            if (! getSortOrder().contains(col)) return;
            sort();
        }
    };
    
    private WeakEventHandler weakRootEventListener;
    
    private final WeakInvalidationListener weakColumnVisibleObserver = 
            new WeakInvalidationListener(columnVisibleObserver);
    
    private final WeakInvalidationListener weakColumnSortableObserver = 
            new WeakInvalidationListener(columnSortableObserver);
    
    private final WeakInvalidationListener weakColumnSortTypeObserver = 
            new WeakInvalidationListener(columnSortTypeObserver);
    
    private final WeakListChangeListener weakColumnsObserver = 
            new WeakListChangeListener(columnsObserver);
    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- Root
    private ObjectProperty<TreeItem<S>> root = new SimpleObjectProperty<TreeItem<S>>(this, "root") {
        private WeakReference<TreeItem<S>> weakOldItem;

        @Override protected void invalidated() {
            TreeItem<S> oldTreeItem = weakOldItem == null ? null : weakOldItem.get();
            if (oldTreeItem != null && weakRootEventListener != null) {
                oldTreeItem.removeEventHandler(TreeItem.<S>treeNotificationEvent(), weakRootEventListener);
            }

            TreeItem<S> root = getRoot();
            if (root != null) {
                weakRootEventListener = new WeakEventHandler(rootEvent);
                getRoot().addEventHandler(TreeItem.<S>treeNotificationEvent(), weakRootEventListener);
                weakOldItem = new WeakReference<TreeItem<S>>(root);
            }

            treeItemCountDirty = true;
            updateRootExpanded();
        }
    };
    
    /**
     * Sets the root node in this TreeTableView. See the {@link TreeItem} class level
     * documentation for more details.
     * 
     * @param value The {@link TreeItem} that will be placed at the root of the
     *      TreeTableView.
     */
    public final void setRoot(TreeItem<S> value) {
        rootProperty().set(value);
    }

    /**
     * Returns the current root node of this TreeTableView, or null if no root node
     * is specified.
     * @return The current root node, or null if no root node exists.
     */
    public final TreeItem<S> getRoot() {
        return root == null ? null : root.get();
    }

    /**
     * Property representing the root node of the TreeTableView.
     */
    public final ObjectProperty<TreeItem<S>> rootProperty() {
        return root;
    }

    
    
    // --- Show Root
    private BooleanProperty showRoot;
    
    /**
     * Specifies whether the root {@code TreeItem} should be shown within this 
     * TreeTableView.
     * 
     * @param value If true, the root TreeItem will be shown, and if false it
     *      will be hidden.
     */
    public final void setShowRoot(boolean value) {
        showRootProperty().set(value);
    }

    /**
     * Returns true if the root of the TreeTableView should be shown, and false if
     * it should not. By default, the root TreeItem is visible in the TreeTableView.
     */
    public final boolean isShowRoot() {
        return showRoot == null ? true : showRoot.get();
    }

    /**
     * Property that represents whether or not the TreeTableView root node is visible.
     */
    public final BooleanProperty showRootProperty() {
        if (showRoot == null) {
            showRoot = new SimpleBooleanProperty(this, "showRoot", true) {
                @Override protected void invalidated() {
                    updateRootExpanded();
                    updateTreeItemCount(getRoot());
                }
            };
        }
        return showRoot;
    }
    
    
    // --- Selection Model
    private ObjectProperty<TreeTableViewSelectionModel<S>> selectionModel;

    /**
     * Sets the {@link MultipleSelectionModel} to be used in the TreeTableView. 
     * Despite a TreeTableView requiring a <code><b>Multiple</b>SelectionModel</code>,
     * it is possible to configure it to only allow single selection (see 
     * {@link MultipleSelectionModel#setSelectionMode(javafx.scene.control.SelectionMode)}
     * for more information).
     */
    public final void setSelectionModel(TreeTableViewSelectionModel<S> value) {
        selectionModelProperty().set(value);
    }

    /**
     * Returns the currently installed selection model.
     */
    public final TreeTableViewSelectionModel<S> getSelectionModel() {
        return selectionModel == null ? null : selectionModel.get();
    }

    /**
     * The SelectionModel provides the API through which it is possible
     * to select single or multiple items within a TreeTableView, as  well as inspect
     * which rows have been selected by the user. Note that it has a generic
     * type that must match the type of the TreeTableView itself.
     */
    public final ObjectProperty<TreeTableViewSelectionModel<S>> selectionModelProperty() {
        if (selectionModel == null) {
            selectionModel = new SimpleObjectProperty<TreeTableViewSelectionModel<S>>(this, "selectionModel");
        }
        return selectionModel;
    }
    
    
    // --- Focus Model
    private ObjectProperty<TreeTableViewFocusModel<S>> focusModel;

    /**
     * Sets the {@link FocusModel} to be used in the TreeTableView. 
     */
    public final void setFocusModel(TreeTableViewFocusModel<S> value) {
        focusModelProperty().set(value);
    }

    /**
     * Returns the currently installed {@link FocusModel}.
     */
    public final TreeTableViewFocusModel<S> getFocusModel() {
        return focusModel == null ? null : focusModel.get();
    }

    /**
     * The FocusModel provides the API through which it is possible
     * to control focus on zero or one rows of the TreeTableView. Generally the
     * default implementation should be more than sufficient.
     */
    public final ObjectProperty<TreeTableViewFocusModel<S>> focusModelProperty() {
        if (focusModel == null) {
            focusModel = new SimpleObjectProperty<TreeTableViewFocusModel<S>>(this, "focusModel");
        }
        return focusModel;
    }
    
    
    
    // --- Span Model
    private ObjectProperty<SpanModel<TreeItem<S>>> spanModel 
            = new SimpleObjectProperty<SpanModel<TreeItem<S>>>(this, "spanModel") {

        @Override protected void invalidated() {
            ObservableList<String> styleClass = getStyleClass();
            if (getSpanModel() == null) {
                styleClass.remove(CELL_SPAN_TABLE_VIEW_STYLE_CLASS);
            } else if (! styleClass.contains(CELL_SPAN_TABLE_VIEW_STYLE_CLASS)) {
                styleClass.add(CELL_SPAN_TABLE_VIEW_STYLE_CLASS);
            }
        }
    };

    public final ObjectProperty<SpanModel<TreeItem<S>>> spanModelProperty() {
        return spanModel;
    }
    public final void setSpanModel(SpanModel<TreeItem<S>> value) {
        spanModelProperty().set(value);
    }

    public final SpanModel<TreeItem<S>> getSpanModel() {
        return spanModel.get();
    }
    
    
    
    // --- Tree node count
    private IntegerProperty treeItemCount = new SimpleIntegerProperty(this, "impl_treeItemCount", 0);

    private void setTreeItemCount(int value) {
        impl_treeItemCountProperty().set(value);
    }

    /**
     * <p>Represents the number of tree nodes presently able to be visible in the
     * TreeTableView. This is essentially the count of all expanded tree nodes, and
     * their children.
     *
     * <p>For example, if just the root node is visible, the treeItemCount will
     * be one. If the root had three children and the root was expanded, the value
     * will be four.
     * 
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final int impl_getTreeItemCount() {
        if (treeItemCountDirty) {
            updateTreeItemCount(getRoot());
        }
        return treeItemCount.get();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final IntegerProperty impl_treeItemCountProperty() {
        return treeItemCount;
    }
    
    
    // --- Editable
    private BooleanProperty editable;
    public final void setEditable(boolean value) {
        editableProperty().set(value);
    }
    public final boolean isEditable() {
        return editable == null ? false : editable.get();
    }
    /**
     * Specifies whether this TreeTableView is editable - only if the TreeTableView and
     * the TreeCells within it are both editable will a TreeCell be able to go
     * into their editing state.
     */
    public final BooleanProperty editableProperty() {
        if (editable == null) {
            editable = new SimpleBooleanProperty(this, "editable", false);
        }
        return editable;
    }
    
    
    // --- Editing Item
    private ReadOnlyObjectWrapper<TreeItem<S>> editingItem;

    private void setEditingItem(TreeItem<S> value) {
        editingItemPropertyImpl().set(value);
    }

    /**
     * Returns the TreeItem that is currently being edited in the TreeTableView,
     * or null if no item is being edited.
     */
    public final TreeItem<S> getEditingItem() {
        return editingItem == null ? null : editingItem.get();
    }

    /**
     * <p>A property used to represent the TreeItem currently being edited
     * in the TreeTableView, if editing is taking place, or -1 if no item is being edited.
     * 
     * <p>It is not possible to set the editing item, instead it is required that
     * you call {@link #edit(javafx.scene.control.TreeItem)}.
     */
    public final ReadOnlyObjectProperty<TreeItem<S>> editingItemProperty() {
        return editingItemPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<TreeItem<S>> editingItemPropertyImpl() {
        if (editingItem == null) {
            editingItem = new ReadOnlyObjectWrapper<TreeItem<S>>(this, "editingItem");
        }
        return editingItem;
    }
    
    
    // --- On Edit Start
    private ObjectProperty<EventHandler<TreeTableView.EditEvent<S>>> onEditStart;

    /**
     * Sets the {@link EventHandler} that will be called when the user begins
     * an edit. 
     */
    public final void setOnEditStart(EventHandler<TreeTableView.EditEvent<S>> value) {
        onEditStartProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user begins
     * an edit.
     */
    public final EventHandler<TreeTableView.EditEvent<S>> getOnEditStart() {
        return onEditStart == null ? null : onEditStart.get();
    }

    /**
     * This event handler will be fired when the user successfully initiates
     * editing.
     */
    public final ObjectProperty<EventHandler<TreeTableView.EditEvent<S>>> onEditStartProperty() {
        if (onEditStart == null) {
            onEditStart = new SimpleObjectProperty<EventHandler<TreeTableView.EditEvent<S>>>(this, "onEditStart") {
                @Override protected void invalidated() {
                    setEventHandler(TreeTableView.<S>editStartEvent(), get());
                }
            };
        }
        return onEditStart;
    }


    // --- On Edit Commit
    private ObjectProperty<EventHandler<TreeTableView.EditEvent<S>>> onEditCommit;

    /**
     * Sets the {@link EventHandler} that will be called when the user commits
     * an edit. 
     */
    public final void setOnEditCommit(EventHandler<TreeTableView.EditEvent<S>> value) {
        onEditCommitProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user commits
     * an edit.
     */
    public final EventHandler<TreeTableView.EditEvent<S>> getOnEditCommit() {
        return onEditCommit == null ? null : onEditCommit.get();
    }

    /**
     * <p>This property is used when the user performs an action that should
     * result in their editing input being persisted.</p>
     *
     * <p>The EventHandler in this property should not be called directly - 
     * instead call {@link TreeCell#commitEdit(java.lang.Object)} from within
     * your custom TreeCell. This will handle firing this event, updating the 
     * view, and switching out of the editing state.</p>
     */
    public final ObjectProperty<EventHandler<TreeTableView.EditEvent<S>>> onEditCommitProperty() {
        if (onEditCommit == null) {
            onEditCommit = new SimpleObjectProperty<EventHandler<TreeTableView.EditEvent<S>>>(this, "onEditCommit") {
                @Override protected void invalidated() {
                    setEventHandler(TreeTableView.<S>editCommitEvent(), get());
                }
            };
        }
        return onEditCommit;
    }


    // --- On Edit Cancel
    private ObjectProperty<EventHandler<TreeTableView.EditEvent<S>>> onEditCancel;

    /**
     * Sets the {@link EventHandler} that will be called when the user cancels
     * an edit.
     */
    public final void setOnEditCancel(EventHandler<TreeTableView.EditEvent<S>> value) {
        onEditCancelProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user cancels
     * an edit.
     */
    public final EventHandler<TreeTableView.EditEvent<S>> getOnEditCancel() {
        return onEditCancel == null ? null : onEditCancel.get();
    }

    /**
     * This event handler will be fired when the user cancels editing a cell.
     */
    public final ObjectProperty<EventHandler<TreeTableView.EditEvent<S>>> onEditCancelProperty() {
        if (onEditCancel == null) {
            onEditCancel = new SimpleObjectProperty<EventHandler<TreeTableView.EditEvent<S>>>(this, "onEditCancel") {
                @Override protected void invalidated() {
                    setEventHandler(TreeTableView.<S>editCancelEvent(), get());
                }
            };
        }
        return onEditCancel;
    }
    

    // --- Table menu button visible
    private BooleanProperty tableMenuButtonVisible;
    /**
     * This controls whether a menu button is available when the user clicks
     * in a designated space within the TableView, within which is a radio menu
     * item for each TreeTableColumn in this table. This menu allows for the user to
     * show and hide all TreeTableColumns easily.
     */
    public final BooleanProperty tableMenuButtonVisibleProperty() {
        if (tableMenuButtonVisible == null) {
            tableMenuButtonVisible = new SimpleBooleanProperty(this, "tableMenuButtonVisible");
        }
        return tableMenuButtonVisible;
    }
    public final void setTableMenuButtonVisible (boolean value) {
        tableMenuButtonVisibleProperty().set(value);
    }
    public final boolean isTableMenuButtonVisible() {
        return tableMenuButtonVisible == null ? false : tableMenuButtonVisible.get();
    }
    
    
    // --- Column Resize Policy
    private ObjectProperty<Callback<TreeTableView.ResizeFeatures, Boolean>> columnResizePolicy;
    public final void setColumnResizePolicy(Callback<TreeTableView.ResizeFeatures, Boolean> callback) {
        columnResizePolicyProperty().set(callback);
    }
    public final Callback<TreeTableView.ResizeFeatures, Boolean> getColumnResizePolicy() {
        return columnResizePolicy == null ? UNCONSTRAINED_RESIZE_POLICY : columnResizePolicy.get();
    }

    /**
     * This is the function called when the user completes a column-resize
     * operation. The two most common policies are available as static functions
     * in the TableView class: {@link #UNCONSTRAINED_RESIZE_POLICY} and
     * {@link #CONSTRAINED_RESIZE_POLICY}.
     */
    public final ObjectProperty<Callback<TreeTableView.ResizeFeatures, Boolean>> columnResizePolicyProperty() {
        if (columnResizePolicy == null) {
            columnResizePolicy = new SimpleObjectProperty<Callback<TreeTableView.ResizeFeatures, Boolean>>(this, "columnResizePolicy", UNCONSTRAINED_RESIZE_POLICY) {
                private Callback<TreeTableView.ResizeFeatures, Boolean> oldPolicy;
                
                @Override protected void invalidated() {
                    if (isInited) {
                        get().call(new TreeTableView.ResizeFeatures(TreeTableView.this, null, 0.0));
                        refresh();
                
                        if (oldPolicy != null) {
                            impl_pseudoClassStateChanged(oldPolicy.toString());
                        }
                        if (get() != null) {
                            impl_pseudoClassStateChanged(get().toString());
                        }
                        oldPolicy = get();
                    }
                }
            };
        }
        return columnResizePolicy;
    }
    
    
    // --- Row Factory
    private ObjectProperty<Callback<TreeTableView<S>, TreeTableRow<S>>> rowFactory;

    /**
     * A function which produces a TreeTableRow. The system is responsible for
     * reusing TreeTableRows. Return from this function a TreeTableRow which
     * might be usable for representing a single row in a TableView.
     * <p>
     * Note that a TreeTableRow is <b>not</b> a TableCell. A TreeTableRow is
     * simply a container for a TableCell, and in most circumstances it is more
     * likely that you'll want to create custom TableCells, rather than
     * TreeTableRows. The primary use case for creating custom TreeTableRow
     * instances would most probably be to introduce some form of column
     * spanning support.
     * <p>
     * You can create custom TableCell instances per column by assigning the
     * appropriate function to the cellFactory property in the TreeTableColumn class.
     */
    public final ObjectProperty<Callback<TreeTableView<S>, TreeTableRow<S>>> rowFactoryProperty() {
        if (rowFactory == null) {
            rowFactory = new SimpleObjectProperty<Callback<TreeTableView<S>, TreeTableRow<S>>>(this, "rowFactory");
        }
        return rowFactory;
    }
    public final void setRowFactory(Callback<TreeTableView<S>, TreeTableRow<S>> value) {
        rowFactoryProperty().set(value);
    }
    public final Callback<TreeTableView<S>, TreeTableRow<S>> getRowFactory() {
        return rowFactory == null ? null : rowFactory.get();
    }
    
    
    // --- Placeholder Node
    private ObjectProperty<Node> placeholder;
    /**
     * This Node is shown to the user when the table has no content to show.
     * This may be the case because the table model has no data in the first
     * place, that a filter has been applied to the table model, resulting
     * in there being nothing to show the user, or that there are no currently
     * visible columns.
     */
    public final ObjectProperty<Node> placeholderProperty() {
        if (placeholder == null) {
            placeholder = new SimpleObjectProperty<Node>(this, "placeholder");
        }
        return placeholder;
    }
    public final void setPlaceholder(Node value) {
        placeholderProperty().set(value);
    }
    public final Node getPlaceholder() {
        return placeholder == null ? null : placeholder.get();
    }


    
    // --- Editing Cell
    private ReadOnlyObjectWrapper<TreeTablePosition<S,?>> editingCell;
    private void setEditingCell(TreeTablePosition<S,?> value) {
        editingCellPropertyImpl().set(value);
    }
    public final TreeTablePosition<S,?> getEditingCell() {
        return editingCell == null ? null : editingCell.get();
    }

    /**
     * Represents the current cell being edited, or null if
     * there is no cell being edited.
     */
    public final ReadOnlyObjectProperty<TreeTablePosition<S,?>> editingCellProperty() {
        return editingCellPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<TreeTablePosition<S,?>> editingCellPropertyImpl() {
        if (editingCell == null) {
            editingCell = new ReadOnlyObjectWrapper<TreeTablePosition<S,?>>(this, "editingCell");
        }
        return editingCell;
    }


    // --- SortMode
    /**
     * Specifies the sort mode to use when sorting the contents of this TreeTableView,
     * should any columns be specified in the {@link #getSortOrder() sort order}
     * list.
     */
    private ObjectProperty<TreeSortMode> sortMode;
    public final ObjectProperty<TreeSortMode> sortModeProperty() {
        if (sortMode == null) {
            sortMode = new SimpleObjectProperty(this, "sortMode", TreeSortMode.ALL_DESCENDANTS);
        }
        return sortMode;
    }
    public final void setSortMode(TreeSortMode value) {
        sortModeProperty().set(value);
    }
    public final TreeSortMode getSortMode() {
        return sortMode == null ? TreeSortMode.ALL_DESCENDANTS : sortMode.get();
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/
    
    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        if (treeItemCountDirty) {
            updateTreeItemCount(getRoot());
        }
        
        super.layoutChildren();
    }
    
    
    /**
     * Instructs the TreeTableView to begin editing the given TreeItem, if 
     * the TreeTableView is {@link #editableProperty() editable}. Once
     * this method is called, if the current 
     * {@link #cellFactoryProperty() cell factory} is set up to support editing,
     * the Cell will switch its visual state to enable the user input to take place.
     * 
     * @param item The TreeItem in the TreeTableView that should be edited.
     */
    public void edit(TreeItem<S> item) {
        if (!isEditable()) return;
        setEditingItem(item);
    }
    

    /**
     * Scrolls the TreeTableView such that the item in the given index is visible to
     * the end user.
     * 
     * @param index The index that should be made visible to the user, assuming
     *      of course that it is greater than, or equal to 0, and less than the
     *      number of the visible items in the TreeTableView.
     */
    public void scrollTo(int index) {
       getProperties().put(VirtualContainerBase.SCROLL_TO_INDEX_CENTERED, index);
    }

    /**
     * Returns the index position of the given TreeItem, taking into account the
     * current state of each TreeItem (i.e. whether or not it is expanded).
     * 
     * @param item The TreeItem for which the index is sought.
     * @return An integer representing the location in the current TreeTableView of the
     *      first instance of the given TreeItem, or -1 if it is null or can not 
     *      be found.
     */
    public int getRow(TreeItem<S> item) {
        return TreeUtil.getRow(item, getRoot(), treeItemCountDirty, isShowRoot());
    }

    /**
     * Returns the TreeItem in the given index, or null if it is out of bounds.
     * 
     * @param row The index of the TreeItem being sought.
     * @return The TreeItem in the given index, or null if it is out of bounds.
     */
    public TreeItem<S> getTreeItem(int row) {
        // normalize the requested row based on whether showRoot is set
        int r = isShowRoot() ? row : (row + 1);
        return TreeUtil.getItem(getRoot(), r, treeItemCountDirty);
    }
    
    /**
     * The TreeTableColumns that are part of this TableView. As the user reorders
     * the TableView columns, this list will be updated to reflect the current
     * visual ordering.
     *
     * <p>Note: to display any data in a TableView, there must be at least one
     * TreeTableColumn in this ObservableList.</p>
     */
    public final ObservableList<TreeTableColumn<S,?>> getColumns() {
        return columns;
    }
    
    /**
     * The sortOrder list defines the order in which {@link TreeTableColumn} instances
     * are sorted. An empty sortOrder list means that no sorting is being applied
     * on the TableView. If the sortOrder list has one TreeTableColumn within it, 
     * the TableView will be sorted using the 
     * {@link TreeTableColumn#sortTypeProperty() sortType} and
     * {@link TreeTableColumn#comparatorProperty() comparator} properties of this
     * TreeTableColumn (assuming 
     * {@link TreeTableColumn#sortableProperty() TreeTableColumn.sortable} is true).
     * If the sortOrder list contains multiple TreeTableColumn instances, then
     * the TableView is firstly sorted based on the properties of the first 
     * TreeTableColumn. If two elements are considered equal, then the second
     * TreeTableColumn in the list is used to determine ordering. This repeats until
     * the results from all TreeTableColumn comparators are considered, if necessary.
     * 
     * @return An ObservableList containing zero or more TreeTableColumn instances.
     */
    public final ObservableList<TreeTableColumn<S,?>> getSortOrder() {
        return sortOrder;
    }
    
    /**
     * Applies the currently installed resize policy against the given column,
     * resizing it based on the delta value provided.
     */
    public boolean resizeColumn(TreeTableColumn<S,?> column, double delta) {
        if (column == null || Double.compare(delta, 0.0) == 0) return false;

        boolean allowed = getColumnResizePolicy().call(new TreeTableView.ResizeFeatures<S>(TreeTableView.this, column, delta));
        if (!allowed) return false;

        // This fixes the issue where if the column width is reduced and the
        // table width is also reduced, horizontal scrollbars will begin to
        // appear at the old width. This forces the VirtualFlow.maxPrefBreadth
        // value to be reset to -1 and subsequently recalculated. Of course
        // ideally we'd just refreshView, but for the time-being no such function
        // exists.
        refresh();
        return true;
    }

    /**
     * Causes the cell at the given row/column view indexes to switch into
     * its editing state, if it is not already in it, and assuming that the 
     * TableView and column are also editable.
     */
    public void edit(int row, TreeTableColumn<S,?> column) {
        if (!isEditable() || (column != null && ! column.isEditable())) return;
        setEditingCell(new TreeTablePosition(this, row, column));
    }
    
    /**
     * Returns an unmodifiable list containing the currently visible leaf columns.
     */
    @ReturnsUnmodifiableCollection
    public ObservableList<TreeTableColumn<S,?>> getVisibleLeafColumns() {
        return unmodifiableVisibleLeafColumns;
    }
    
    /**
     * Returns the position of the given column, relative to all other 
     * visible leaf columns.
     */
    public int getVisibleLeafIndex(TreeTableColumn<S,?> column) {
        return getVisibleLeafColumns().indexOf(column);
    }

    /**
     * Returns the TableColumn in the given column index, relative to all other
     * visible leaf columns.
     */
    public TreeTableColumn<S,?> getVisibleLeafColumn(int column) {
        if (column < 0 || column >= visibleLeafColumns.size()) return null;
        return visibleLeafColumns.get(column);
    }

    
    
    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/
    
    private void updateTreeItemCount(TreeItem treeItem) {
        setTreeItemCount(TreeUtil.updateTreeItemCount(treeItem, treeItemCountDirty, isShowRoot()));
        treeItemCountDirty = false;
    }

    private void updateRootExpanded() {
        // if we aren't showing the root, and the root isn't expanded, we expand
        // it now so that something is shown.
        if (!isShowRoot() && getRoot() != null && ! getRoot().isExpanded()) {
            getRoot().setExpanded(true);
        }
    }

    /**
     * Call this function to force the TableView to re-evaluate itself. This is
     * useful when the underlying data model is provided by a TableModel, and
     * you know that the data model has changed. This will force the TableView
     * to go back to the dataProvider and get the row count, as well as update
     * the view to ensure all sorting is still correct based on any changes to
     * the data model.
     */
    private void refresh() {
        getProperties().put(TableView.REFRESH, Boolean.TRUE);
    }
    
    /**
     * Sometimes we want to force a sort to run - this is the recommended way
     * of doing it internally. External users of the TableView API should just
     * stick to modifying the TableView.sortOrder ObservableList (or the contents
     * of the TreeTableColumns within it - in particular the
     * TreeTableColumn.sortAscending boolean).
     */
    private void sort() {
        TreeItem rootItem = getRoot();
        if (rootItem == null) return;
        
        TreeSortMode sortMode = getSortMode();
        if (sortMode == null) return;
        
        // build up a new comparator based on the current table columms
        TableColumnComparator comparator = new TableColumnComparator();
        for (TreeTableColumn<S,?> tc : getSortOrder()) {
            comparator.getColumns().add(tc);
        }

        rootItem.lastSortMode = sortMode;
        rootItem.lastComparator = comparator;
        rootItem.sort();
    }
    
    // --- Content width
    private void setContentWidth(double contentWidth) {
        this.contentWidth = contentWidth;
        if (isInited) {
            // sometimes the current column resize policy will have to modify the
            // column width of all columns in the table if the table width changes,
            // so we short-circuit the resize function and just go straight there
            // with a null TreeTableColumn, which indicates to the resize policy function
            // that it shouldn't actually do anything specific to one column.
            getColumnResizePolicy().call(new TreeTableView.ResizeFeatures<S>(TreeTableView.this, null, 0.0));
            refresh();
        }
    }
    
    /**
     * Recomputes the currently visible leaf columns in this TableView.
     */
    private void updateVisibleLeafColumns() {
        // update visible leaf columns list
        List<TreeTableColumn<S,?>> cols = new ArrayList<TreeTableColumn<S,?>>();
        buildVisibleLeafColumns(getColumns(), cols);
        visibleLeafColumns.setAll(cols);

        // sometimes the current column resize policy will have to modify the
        // column width of all columns in the table if the table width changes,
        // so we short-circuit the resize function and just go straight there
        // with a null TreeTableColumn, which indicates to the resize policy function
        // that it shouldn't actually do anything specific to one column.
        getColumnResizePolicy().call(new TreeTableView.ResizeFeatures<S>(TreeTableView.this, null, 0.0));
        refresh();
    }

    private void buildVisibleLeafColumns(List<TreeTableColumn<S,?>> cols, List<TreeTableColumn<S,?>> vlc) {
        for (TreeTableColumn<S,?> c : cols) {
            if (c == null) continue;

            boolean hasChildren = ! c.getColumns().isEmpty();

            if (hasChildren) {
                buildVisibleLeafColumns(c.getColumns(), vlc);
            } else if (c.isVisible()) {
                vlc.add(c);
            }
        }
    }


    
    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "tree-table-view";
    private static final String CELL_SPAN_TABLE_VIEW_STYLE_CLASS = "cell-span-tree-table-view";
    
    private static final String PSEUDO_CLASS_CELL_SELECTION = "cell-selection";
    private static final String PSEUDO_CLASS_ROW_SELECTION = "row-selection";


    private static final long CELL_SELECTION_PSEUDOCLASS_STATE =
            StyleManager.getPseudoclassMask(PSEUDO_CLASS_CELL_SELECTION);
    private static final long ROW_SELECTION_PSEUDOCLASS_STATE =
            StyleManager.getPseudoclassMask(PSEUDO_CLASS_ROW_SELECTION);

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        if (getSelectionModel() != null) {
            mask |= (getSelectionModel().isCellSelectionEnabled()) ?
                CELL_SELECTION_PSEUDOCLASS_STATE : ROW_SELECTION_PSEUDOCLASS_STATE;
        }
        return mask;
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TreeTableViewSkin<S>(this);
    }

    

    /***************************************************************************
     *                                                                         *
     * Support Classes                                                         *
     *                                                                         *
     **************************************************************************/

     /**
      * An immutable wrapper class for use in the TableView 
     * {@link TreeTableView#columnResizePolicyProperty() column resize} functionality.
      */
     public static class ResizeFeatures<S> extends ResizeFeaturesBase<TreeItem<S>> {
        private TreeTableView<S> treeTable;

        /**
         * Creates an instance of this class, with the provided TreeTableView, 
         * TreeTableColumn and delta values being set and stored in this immutable
         * instance.
         * 
         * @param table The TreeTableView upon which the resize operation is occurring.
         * @param column The column upon which the resize is occurring, or null
         *      if this ResizeFeatures instance is being created as a result of a
         *      TreeTableView resize operation.
         * @param delta The amount of horizontal space added or removed in the 
         *      resize operation.
         */
        public ResizeFeatures(TreeTableView<S> treeTable, TreeTableColumn<S,?> column, Double delta) {
            super(column, delta);
            this.treeTable = treeTable;
        }
        
        /**
         * Returns the column upon which the resize is occurring, or null
         * if this ResizeFeatures instance was created as a result of a
         * TreeTableView resize operation.
         */
        @Override public TreeTableColumn<S,?> getColumn() { 
            return (TreeTableColumn) super.getColumn(); 
        }
        
        /**
         * Returns the TreeTableView upon which the resize operation is occurring.
         */
        public TreeTableView<S> getTable() { return treeTable; }
    }


    
    /**
     * An {@link Event} subclass used specifically in TreeTableView for representing
     * edit-related events. It provides additional API to easily access the 
     * TreeItem that the edit event took place on, as well as the input provided
     * by the end user.
     * 
     * @param <S> The type of the input, which is the same type as the TreeTableView 
     *      itself.
     */
    public static class EditEvent<S> extends Event {
        private static final long serialVersionUID = -4437033058917528976L;
        
        private final S oldValue;
        private final S newValue;
        private transient final TreeItem<S> treeItem;
        
        /**
         * Creates a new EditEvent instance to represent an edit event. This 
         * event is used for {@link #EDIT_START_EVENT}, 
         * {@link #EDIT_COMMIT_EVENT} and {@link #EDIT_CANCEL_EVENT} types.
         */
        public EditEvent(TreeTableView<S> source,
                         EventType<? extends TreeTableView.EditEvent> eventType,
                         TreeItem<S> treeItem, S oldValue, S newValue) {
            super(source, Event.NULL_SOURCE_TARGET, eventType);
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.treeItem = treeItem;
        }

        /**
         * Returns the TreeTableView upon which the edit took place.
         */
        @Override public TreeTableView<S> getSource() {
            return (TreeTableView) super.getSource();
        }

        /**
         * Returns the {@link TreeItem} upon which the edit took place.
         */
        public TreeItem<S> getTreeItem() {
            return treeItem;
        }
        
        /**
         * Returns the new value input into the TreeItem by the end user.
         */
        public S getNewValue() {
            return newValue;
        }
        
        /**
         * Returns the old value that existed in the TreeItem prior to the current
         * edit event.
         */
        public S getOldValue() {
            return oldValue;
        }
    }
    
    
     
     /**
     * A simple extension of the {@link SelectionModel} abstract class to
     * allow for special support for TableView controls.
     */
    public static abstract class TreeTableViewSelectionModel<S> extends 
            TableSelectionModel<TreeItem<S>, TreeTableColumn<S, ?>> {

        private final TreeTableView<S> treeTableView;

        /**
         * Builds a default TableViewSelectionModel instance with the provided
         * TableView.
         * @param tableView The TableView upon which this selection model should
         *      operate.
         * @throws NullPointerException TableView can not be null.
         */
        public TreeTableViewSelectionModel(final TreeTableView<S> treeTableView) {
            if (treeTableView == null) {
                throw new NullPointerException("TreeTableView can not be null");
            }

            this.treeTableView = treeTableView;
            
            cellSelectionEnabledProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable o) {
                    isCellSelectionEnabled();
                    clearSelection();
                    treeTableView.impl_pseudoClassStateChanged(TreeTableView.PSEUDO_CLASS_CELL_SELECTION);
                    treeTableView.impl_pseudoClassStateChanged(TreeTableView.PSEUDO_CLASS_ROW_SELECTION);
                }
            });
        }

        /**
         * A read-only ObservableList representing the currently selected cells 
         * in this TableView. Rather than directly modify this list, please
         * use the other methods provided in the TableViewSelectionModel.
         */
        public abstract ObservableList<TreeTablePosition> getSelectedCells();

        /**
         * Returns the TableView instance that this selection model is installed in.
         */
        public TreeTableView<S> getTreeTableView() {
            return treeTableView;
        }
    }
    
    

    /**
     * A primitive selection model implementation, using a List<Integer> to store all
     * selected indices.
     */
    // package for testing
    static class TreeTableViewArrayListSelectionModel<S> extends TreeTableViewSelectionModel<S> {

        /***********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        public TreeTableViewArrayListSelectionModel(final TreeTableView<S> treeTableView) {
            super(treeTableView);
            this.treeTableView = treeTableView;
            
            this.treeTableView.rootProperty().addListener(weakRootPropertyListener);
            updateTreeEventListener(null, treeTableView.getRoot());
            
            final MappingChange.Map<TreeTablePosition,TreeItem<S>> cellToItemsMap = new MappingChange.Map<TreeTablePosition, TreeItem<S>>() {
                @Override public TreeItem<S> map(TreeTablePosition f) {
                    return getModelItem(f.getRow());
                }
            };
            
            final MappingChange.Map<TreeTablePosition,Integer> cellToIndicesMap = new MappingChange.Map<TreeTablePosition, Integer>() {
                @Override public Integer map(TreeTablePosition f) {
                    return f.getRow();
                }
            };
            
            selectedCells = FXCollections.<TreeTablePosition>observableArrayList();
            selectedCells.addListener(new ListChangeListener<TreeTablePosition>() {
                @Override
                public void onChanged(final ListChangeListener.Change<? extends TreeTablePosition> c) {
                    // when the selectedCells observableArrayList changes, we manually call
                    // the observers of the selectedItems, selectedIndices and
                    // selectedCells lists.
                    
                    // create an on-demand list of the removed objects contained in the
                    // given rows
                    selectedItems.callObservers(new MappingChange<TreeTablePosition, TreeItem<S>>(c, cellToItemsMap, selectedItems));
                    c.reset();

                    selectedIndices.callObservers(new MappingChange<TreeTablePosition, Integer>(c, cellToIndicesMap, selectedIndices));
                    c.reset();

                    selectedCellsSeq.callObservers(new MappingChange<TreeTablePosition, TreeTablePosition>(c, MappingChange.NOOP_MAP, selectedCellsSeq));
                    c.reset();
                }
            });

            selectedIndices = new ReadOnlyUnbackedObservableList<Integer>() {
                @Override public Integer get(int i) {
                    return selectedCells.get(i).getRow();
                }

                @Override public int size() {
                    return selectedCells.size();
                }
            };

            selectedItems = new ReadOnlyUnbackedObservableList<TreeItem<S>>() {
                @Override public TreeItem<S> get(int i) {
                    return getModelItem(selectedIndices.get(i));
                }

                @Override public int size() {
                    return selectedIndices.size();
                }
            };
            
            selectedCellsSeq = new ReadOnlyUnbackedObservableList<TreeTablePosition>() {
                @Override public TreeTablePosition get(int i) {
                    return selectedCells.get(i);
                }

                @Override public int size() {
                    return selectedCells.size();
                }
            };
        }
        
        private final TreeTableView<S> treeTableView;
        
        private void updateTreeEventListener(TreeItem<S> oldRoot, TreeItem<S> newRoot) {
            if (oldRoot != null && weakTreeItemListener != null) {
                oldRoot.removeEventHandler(TreeItem.<S>treeItemCountChangeEvent(), weakTreeItemListener);
            }
            
            if (newRoot != null) {
                weakTreeItemListener = new WeakEventHandler(treeItemListener);
                newRoot.addEventHandler(TreeItem.<S>treeItemCountChangeEvent(), weakTreeItemListener);
            }
        }
        
        private ChangeListener rootPropertyListener = new ChangeListener<TreeItem<S>>() {
            @Override public void changed(ObservableValue<? extends TreeItem<S>> observable, 
                    TreeItem<S> oldValue, TreeItem<S> newValue) {
                setSelectedIndex(-1);
                updateTreeEventListener(oldValue, newValue);
            }
        };
        
        private EventHandler<TreeItem.TreeModificationEvent<S>> treeItemListener = new EventHandler<TreeItem.TreeModificationEvent<S>>() {
            @Override public void handle(TreeItem.TreeModificationEvent<S> e) {
                
                if (getSelectedIndex() == -1 && getSelectedItem() == null) return;
                
                // we only shift selection from this row - everything before it
                // is safe. We might change this below based on certain criteria
                int startRow = treeTableView.getRow(e.getTreeItem());
                
                int shift = 0;
                if (e.wasExpanded()) {
                    // need to shuffle selection by the number of visible children
                    shift = e.getTreeItem().getExpandedDescendentCount(false) - 1;
                    startRow++;
                } else if (e.wasCollapsed()) {
                    // remove selection from any child treeItem
                    int row = treeTableView.getRow(e.getTreeItem());
                    int count = e.getTreeItem().previousExpandedDescendentCount;
                    boolean wasAnyChildSelected = false;
                    for (int i = row; i < row + count; i++) {
                        if (isSelected(i)) {
                            wasAnyChildSelected = true;
                            break;
                        }
                    }

                    // put selection onto the collapsed tree item
                    if (wasAnyChildSelected) {
                        select(startRow);
                    }

                    shift = - e.getTreeItem().previousExpandedDescendentCount + 1;
                    startRow++;
                } else if (e.wasAdded()) {
                    // shuffle selection by the number of added items
                    shift = e.getTreeItem().isExpanded() ? e.getAddedSize() : 0;
                } else if (e.wasRemoved()) {
                    // shuffle selection by the number of removed items
                    shift = e.getTreeItem().isExpanded() ? -e.getRemovedSize() : 0;
                    
                    // whilst we are here, we should check if the removed items
                    // are part of the selectedItems list - and remove them
                    // from selection if they are (as per RT-15446)
                    List<Integer> indices = getSelectedIndices();
                    for (int i = 0; i < indices.size() && ! getSelectedItems().isEmpty(); i++) {
                        int index = indices.get(i);
                        if (index > getSelectedItems().size()) break;
                        
                        TreeItem<S> item = getSelectedItems().get(index);
                        if (item == null || e.getRemovedChildren().contains(item)) {
                            clearSelection(index);
                        }
                    }
                }
                
                shiftSelection(startRow, shift);
            }
        };
        
        private WeakChangeListener weakRootPropertyListener =
                new WeakChangeListener(rootPropertyListener);
        
        private WeakEventHandler weakTreeItemListener;
        
        

        /***********************************************************************
         *                                                                     *
         * Observable properties (and getters/setters)                         *
         *                                                                     *
         **********************************************************************/
        
        // the only 'proper' internal observableArrayList, selectedItems and selectedIndices
        // are both 'read-only and unbacked'.
        private final ObservableList<TreeTablePosition> selectedCells;

        // NOTE: represents selected ROWS only - use selectedCells for more data
        private final ReadOnlyUnbackedObservableList<Integer> selectedIndices;
        @Override public ObservableList<Integer> getSelectedIndices() {
            return selectedIndices;
        }

        // used to represent the _row_ backing data for the selectedCells
        private final ReadOnlyUnbackedObservableList<TreeItem<S>> selectedItems;
        @Override public ObservableList<TreeItem<S>> getSelectedItems() {
            return selectedItems;
        }

        private final ReadOnlyUnbackedObservableList<TreeTablePosition> selectedCellsSeq;
        @Override public ObservableList<TreeTablePosition> getSelectedCells() {
            return selectedCellsSeq;
        }


        /***********************************************************************
         *                                                                     *
         * Internal properties                                                 *
         *                                                                     *
         **********************************************************************/

        

        /***********************************************************************
         *                                                                     *
         * Public selection API                                                *
         *                                                                     *
         **********************************************************************/

        @Override public void clearAndSelect(int row) {
            clearAndSelect(row, null);
        }

        @Override public void clearAndSelect(int row, TreeTableColumn<S,?> column) {
            quietClearSelection();
            select(row, column);
        }

        @Override public void select(int row) {
            select(row, null);
        }

        @Override public void select(int row, TreeTableColumn<S,?> column) {
            // TODO we need to bring in the TreeView selection stuff here...
            if (row < 0 || row >= getRowCount()) return;

            // if I'm in cell selection mode but the column is null, I don't want
            // to select the whole row instead...
            if (isCellSelectionEnabled() && column == null) return;
//            
//            // If I am not in cell selection mode (so I want to select rows only),
//            // if a column is given, I return
//            if (! isCellSelectionEnabled() && column != null) return;

            TreeTablePosition pos = new TreeTablePosition(getTreeTableView(), row, column);
            
            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();
            }

            if (! selectedCells.contains(pos)) {
                selectedCells.add(pos);
            }

            updateSelectedIndex(row);
            focus(row, column);
        }

        @Override public void select(TreeItem<S> obj) {
            // We have no option but to iterate through the model and select the
            // first occurrence of the given object. Once we find the first one, we
            // don't proceed to select any others.
            TreeItem<S> rowObj = null;
            for (int i = 0; i < getRowCount(); i++) {
                rowObj = treeTableView.getTreeItem(i);
                if (rowObj == null) continue;

                if (rowObj.equals(obj)) {
                    if (isSelected(i)) {
                        return;
                    }

                    if (getSelectionMode() == SelectionMode.SINGLE) {
                        quietClearSelection();
                    }

                    select(i);
                    return;
                }
            }

            // if we are here, we did not find the item in the entire data model.
            // Even still, we allow for this item to be set to the give object.
            // We expect that in concrete subclasses of this class we observe the
            // data model such that we check to see if the given item exists in it,
            // whilst SelectedIndex == -1 && SelectedItem != null.
            setSelectedItem(obj);
        }

        @Override public void selectIndices(int row, int... rows) {
            if (rows == null) {
                select(row);
                return;
            }

            /*
             * Performance optimisation - if multiple selection is disabled, only
             * process the end-most row index.
             */
            int rowCount = getRowCount();

            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();

                for (int i = rows.length - 1; i >= 0; i--) {
                    int index = rows[i];
                    if (index >= 0 && index < rowCount) {
                        select(index);
                        break;
                    }
                }

                if (selectedCells.isEmpty()) {
                    if (row > 0 && row < rowCount) {
                        select(row);
                    }
                }
            } else {
                int lastIndex = -1;
                List<TreeTablePosition> positions = new ArrayList<TreeTablePosition>();

                if (row >= 0 && row < rowCount) {
                    positions.add(new TreeTablePosition(getTreeTableView(), row, null));
                    lastIndex = row;
                }

                for (int i = 0; i < rows.length; i++) {
                    int index = rows[i];
                    if (index < 0 || index >= rowCount) continue;
                    lastIndex = index;
                    TreeTablePosition pos = new TreeTablePosition(getTreeTableView(), index, null);
                    if (selectedCells.contains(pos)) continue;

                    positions.add(pos);
                }

                selectedCells.addAll(positions);

                if (lastIndex != -1) {
                    select(lastIndex);
                }
            }
        }

        @Override public void selectAll() {
            if (getSelectionMode() == SelectionMode.SINGLE) return;

            quietClearSelection();
//            if (getTableModel() == null) return;

            if (isCellSelectionEnabled()) {
                List<TreeTablePosition> indices = new ArrayList<TreeTablePosition>();
                TreeTableColumn column;
                TreeTablePosition tp = null;
                for (int col = 0; col < getTreeTableView().getVisibleLeafColumns().size(); col++) {
                    column = getTreeTableView().getVisibleLeafColumns().get(col);
                    for (int row = 0; row < getRowCount(); row++) {
                        tp = new TreeTablePosition(getTreeTableView(), row, column);
                        indices.add(tp);
                    }
                }
                selectedCells.setAll(indices);
                
                if (tp != null) {
                    select(tp.getRow(), tp.getTableColumn());
                    focus(tp.getRow(), tp.getTableColumn());
                }
            } else {
                List<TreeTablePosition> indices = new ArrayList<TreeTablePosition>();
                for (int i = 0; i < getRowCount(); i++) {
                    indices.add(new TreeTablePosition(getTreeTableView(), i, null));
                }
                selectedCells.setAll(indices);
                select(getRowCount() - 1);
                focus(indices.get(indices.size() - 1));
            }
        }

        @Override public void clearSelection(int index) {
            clearSelection(index, null);
        }

        @Override public void clearSelection(int row, TreeTableColumn<S,?> column) {
            TreeTablePosition tp = new TreeTablePosition(getTreeTableView(), row, column);

            boolean csMode = isCellSelectionEnabled();
            
            for (TreeTablePosition pos : getSelectedCells()) {
                if ((! csMode && pos.getRow() == row) || (csMode && pos.equals(tp))) {
                    selectedCells.remove(pos);

                    // give focus to this cell index
                    focus(row);

                    return;
                }
            }
        }

        @Override public void clearSelection() {
            updateSelectedIndex(-1);
            focus(-1);
            quietClearSelection();
        }

        private void quietClearSelection() {
            selectedCells.clear();
        }

        @Override public boolean isSelected(int index) {
            return isSelected(index, null);
        }

        @Override public boolean isSelected(int row, TreeTableColumn<S,?> column) {
            // When in cell selection mode, we currently do NOT support selecting
            // entire rows, so a isSelected(row, null) 
            // should always return false.
            if (isCellSelectionEnabled() && (column == null)) return false;
            
            for (TreeTablePosition tp : getSelectedCells()) {
                boolean columnMatch = ! isCellSelectionEnabled() || 
                        (column == null && tp.getTableColumn() == null) || 
                        (column != null && column.equals(tp.getTableColumn()));
                
                if (tp.getRow() == row && columnMatch) {
                    return true;
                }
            }
            return false;
        }

        @Override public boolean isEmpty() {
            return selectedCells.isEmpty();
        }

        @Override public void selectPrevious() {
            if (isCellSelectionEnabled()) {
                // in cell selection mode, we have to wrap around, going from
                // right-to-left, and then wrapping to the end of the previous line
                TreeTablePosition<S,?> pos = getFocusedCell();
                if (pos.getColumn() - 1 >= 0) {
                    // go to previous row
                    select(pos.getRow(), getTableColumn(pos.getTableColumn(), -1));
                } else if (pos.getRow() < getRowCount() - 1) {
                    // wrap to end of previous row
                    select(pos.getRow() - 1, getTableColumn(getTreeTableView().getVisibleLeafColumns().size() - 1));
                }
            } else {
                int focusIndex = getFocusedIndex();
                if (focusIndex == -1) {
                    select(getRowCount() - 1);
                } else if (focusIndex > 0) {
                    select(focusIndex - 1);
                }
            }
        }

        @Override public void selectNext() {
            if (isCellSelectionEnabled()) {
                // in cell selection mode, we have to wrap around, going from
                // left-to-right, and then wrapping to the start of the next line
                TreeTablePosition<S,?> pos = getFocusedCell();
                if (pos.getColumn() + 1 < getTreeTableView().getVisibleLeafColumns().size()) {
                    // go to next column
                    select(pos.getRow(), getTableColumn(pos.getTableColumn(), 1));
                } else if (pos.getRow() < getRowCount() - 1) {
                    // wrap to start of next row
                    select(pos.getRow() + 1, getTableColumn(0));
                }
            } else {
                int focusIndex = getFocusedIndex();
                if (focusIndex == -1) {
                    select(0);
                } else if (focusIndex < getRowCount() -1) {
                    select(focusIndex + 1);
                }
            }
        }

        @Override public void selectAboveCell() {
            TreeTablePosition pos = getFocusedCell();
            if (pos.getRow() == -1) {
                select(getRowCount() - 1);
            } else if (pos.getRow() > 0) {
                select(pos.getRow() - 1, pos.getTableColumn());
            }
        }

        @Override public void selectBelowCell() {
            TreeTablePosition pos = getFocusedCell();

            if (pos.getRow() == -1) {
                select(0);
            } else if (pos.getRow() < getRowCount() -1) {
                select(pos.getRow() + 1, pos.getTableColumn());
            }
        }

        @Override public void selectFirst() {
            TreeTablePosition focusedCell = getFocusedCell();

            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();
            }

            if (getRowCount() > 0) {
                if (isCellSelectionEnabled()) {
                    select(0, focusedCell.getTableColumn());
                } else {
                    select(0);
                }
            }
        }

        @Override public void selectLast() {
            TreeTablePosition focusedCell = getFocusedCell();

            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();
            }

            int numItems = getRowCount();
            if (numItems > 0 && getSelectedIndex() < numItems - 1) {
                if (isCellSelectionEnabled()) {
                    select(numItems - 1, focusedCell.getTableColumn());
                } else {
                    select(numItems - 1);
                }
            }
        }

        @Override public void selectLeftCell() {
            if (! isCellSelectionEnabled()) return;

            TreeTablePosition pos = getFocusedCell();
            if (pos.getColumn() - 1 >= 0) {
                select(pos.getRow(), getTableColumn(pos.getTableColumn(), -1));
            }
        }

        @Override public void selectRightCell() {
            if (! isCellSelectionEnabled()) return;

            TreeTablePosition pos = getFocusedCell();
            if (pos.getColumn() + 1 < getTreeTableView().getVisibleLeafColumns().size()) {
                select(pos.getRow(), getTableColumn(pos.getTableColumn(), 1));
            }
        }



        /***********************************************************************
         *                                                                     *
         * Support code                                                        *
         *                                                                     *
         **********************************************************************/
        
        private TreeTableColumn<S,?> getTableColumn(int pos) {
            return getTreeTableView().getVisibleLeafColumn(pos);
        }
        
//        private TableColumn<S,?> getTableColumn(TableColumn<S,?> column) {
//            return getTableColumn(column, 0);
//        }

        // Gets a table column to the left or right of the current one, given an offset
        private TreeTableColumn<S,?> getTableColumn(TreeTableColumn<S,?> column, int offset) {
            int columnIndex = getTreeTableView().getVisibleLeafIndex(column);
            int newColumnIndex = columnIndex + offset;
            return getTreeTableView().getVisibleLeafColumn(newColumnIndex);
        }

        private void updateSelectedIndex(int row) {
            setSelectedIndex(row);
            setSelectedItem(getModelItem(row));
        }
        
        @Override public void focus(int row) {
            focus(row, null);
        }

        private void focus(int row, TreeTableColumn<S,?> column) {
            focus(new TreeTablePosition(getTreeTableView(), row, column));
        }

        private void focus(TreeTablePosition pos) {
            if (getTreeTableView().getFocusModel() == null) return;

            getTreeTableView().getFocusModel().focus(pos.getRow(), pos.getTableColumn());
        }

        @Override public int getFocusedIndex() {
            return getFocusedCell().getRow();
        }

        private TreeTablePosition getFocusedCell() {
            if (treeTableView.getFocusModel() == null) {
                return new TreeTablePosition(treeTableView, -1, null);
            }
            return treeTableView.getFocusModel().getFocusedCell();
        }

        private int getRowCount() {
            return treeTableView.impl_getTreeItemCount();
        }

        @Override public TreeItem<S> getModelItem(int index) {
            return treeTableView.getTreeItem(index);
        }

        @Override protected int getItemCount() {
            return treeTableView.impl_getTreeItemCount();
        }
    }
    
    
    
    
    /**
     * A {@link FocusModel} with additional functionality to support the requirements
     * of a TableView control.
     * 
     * @see TableView
     */
    public static class TreeTableViewFocusModel<S> extends TableFocusModel<TreeItem<S>, TreeTableColumn<S,?>> {

        private final TreeTableView<S> treeTableView;

        private final TreeTablePosition EMPTY_CELL;

        /**
         * Creates a default TableViewFocusModel instance that will be used to
         * manage focus of the provided TableView control.
         * 
         * @param tableView The tableView upon which this focus model operates.
         * @throws NullPointerException The TableView argument can not be null.
         */
        public TreeTableViewFocusModel(final TreeTableView<S> treeTableView) {
            if (treeTableView == null) {
                throw new NullPointerException("TableView can not be null");
            }

            this.treeTableView = treeTableView;
            
            this.treeTableView.rootProperty().addListener(weakRootPropertyListener);
            updateTreeEventListener(null, treeTableView.getRoot());

            TreeTablePosition pos = new TreeTablePosition(treeTableView, -1, null);
            setFocusedCell(pos);
            EMPTY_CELL = pos;
        }
        
        private final ChangeListener rootPropertyListener = new ChangeListener<TreeItem<S>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<S>> observable, TreeItem<S> oldValue, TreeItem<S> newValue) {
                updateTreeEventListener(oldValue, newValue);
            }
        };
                
        private final WeakChangeListener weakRootPropertyListener =
                new WeakChangeListener(rootPropertyListener);
        
        private void updateTreeEventListener(TreeItem<S> oldRoot, TreeItem<S> newRoot) {
            if (oldRoot != null && weakTreeItemListener != null) {
                oldRoot.removeEventHandler(TreeItem.<S>treeItemCountChangeEvent(), weakTreeItemListener);
            }
            
            if (newRoot != null) {
                weakTreeItemListener = new WeakEventHandler(treeItemListener);
                newRoot.addEventHandler(TreeItem.<S>treeItemCountChangeEvent(), weakTreeItemListener);
            }
        }
        
        private EventHandler<TreeItem.TreeModificationEvent<S>> treeItemListener = new EventHandler<TreeItem.TreeModificationEvent<S>>() {
            @Override public void handle(TreeItem.TreeModificationEvent<S> e) {
                // don't shift focus if the event occurred on a tree item after
                // the focused row, or if there is no focus index at present
                if (getFocusedIndex() == -1) return;
                
                int row = treeTableView.getRow(e.getTreeItem());
                int shift = 0;
                if (e.wasExpanded()) {
                    if (row > getFocusedIndex()) {
                        // need to shuffle selection by the number of visible children
                        shift = e.getTreeItem().getExpandedDescendentCount(false) - 1;
                    }
                } else if (e.wasCollapsed()) {
                    if (row > getFocusedIndex()) {
                        // need to shuffle selection by the number of visible children
                        // that were just hidden
                        shift = - e.getTreeItem().previousExpandedDescendentCount + 1;
                    }
                } else if (e.wasAdded()) {
                    for (int i = 0; i < e.getAddedChildren().size(); i++) {
                        TreeItem item = e.getAddedChildren().get(i);
                        row = treeTableView.getRow(item);
                        
                        if (item != null && row <= getFocusedIndex()) {
//                            shift = e.getTreeItem().isExpanded() ? e.getAddedSize() : 0;
                            shift += item.getExpandedDescendentCount(false);
                        }
                    }
                } else if (e.wasRemoved()) {
                    for (int i = 0; i < e.getRemovedChildren().size(); i++) {
                        TreeItem item = e.getRemovedChildren().get(i);
                        if (item != null && item.equals(getFocusedItem())) {
                            focus(-1);
                            return;
                        }
                    }
                    
                    if (row <= getFocusedIndex()) {
                        // shuffle selection by the number of removed items
                        shift = e.getTreeItem().isExpanded() ? -e.getRemovedSize() : 0;
                    }
                }
                
                focus(getFocusedIndex() + shift);
            }
        };
        
        private WeakEventHandler weakTreeItemListener;

        /** {@inheritDoc} */
        @Override protected int getItemCount() {
//            if (tableView.getItems() == null) return -1;
//            return tableView.getItems().size();
            return treeTableView.impl_getTreeItemCount();
        }

        /** {@inheritDoc} */
        @Override protected TreeItem<S> getModelItem(int index) {
            if (index < 0 || index >= getItemCount()) return null;
            return treeTableView.getTreeItem(index);
        }

        /**
         * The position of the current item in the TableView which has the focus.
         */
        private ReadOnlyObjectWrapper<TreeTablePosition> focusedCell;
        public final ReadOnlyObjectProperty<TreeTablePosition> focusedCellProperty() {
            return focusedCellPropertyImpl().getReadOnlyProperty();
        }
        private void setFocusedCell(TreeTablePosition value) { focusedCellPropertyImpl().set(value);  }
        public final TreeTablePosition getFocusedCell() { return focusedCell == null ? EMPTY_CELL : focusedCell.get(); }

        private ReadOnlyObjectWrapper<TreeTablePosition> focusedCellPropertyImpl() {
            if (focusedCell == null) {
                focusedCell = new ReadOnlyObjectWrapper<TreeTablePosition>(EMPTY_CELL) {
                    private TreeTablePosition old;
                    @Override protected void invalidated() {
                        if (get() == null) return;

                        if (old == null || (old != null && !old.equals(get()))) {
                            setFocusedIndex(get().getRow());
                            setFocusedItem(getModelItem(getValue().getRow()));
                            
                            old = get();
                        }
                    }

                    @Override
                    public Object getBean() {
                        return TreeTableView.TreeTableViewFocusModel.this;
                    }

                    @Override
                    public String getName() {
                        return "focusedCell";
                    }
                };
            }
            return focusedCell;
        }


        /**
         * Causes the item at the given index to receive the focus.
         *
         * @param row The row index of the item to give focus to.
         * @param column The column of the item to give focus to. Can be null.
         */
        @Override public void focus(int row, TreeTableColumn<S,?> column) {
            if (row < 0 || row >= getItemCount()) {
                setFocusedCell(EMPTY_CELL);
            } else {
                setFocusedCell(new TreeTablePosition(treeTableView, row, column));
            }
        }

        /**
         * Convenience method for setting focus on a particular row or cell
         * using a {@link TablePosition}.
         * 
         * @param pos The table position where focus should be set.
         */
        public void focus(TreeTablePosition pos) {
            if (pos == null) return;
            focus(pos.getRow(), pos.getTableColumn());
        }


        /***********************************************************************
         *                                                                     *
         * Public API                                                          *
         *                                                                     *
         **********************************************************************/

        /**
         * Tests whether the row / cell at the given location currently has the
         * focus within the TableView.
         */
        @Override public boolean isFocused(int row, TreeTableColumn<S,?> column) {
            if (row < 0 || row >= getItemCount()) return false;

            TreeTablePosition cell = getFocusedCell();
            boolean columnMatch = column == null || column.equals(cell.getTableColumn());

            return cell.getRow() == row && columnMatch;
        }

        /**
         * Causes the item at the given index to receive the focus. This does not
         * cause the current selection to change. Updates the focusedItem and
         * focusedIndex properties such that <code>focusedIndex = -1</code> unless
         * <pre><code>0 <= index < model size</code></pre>.
         *
         * @param index The index of the item to get focus.
         */
        @Override public void focus(int index) {
            if (index < 0 || index >= getItemCount()) {
                setFocusedCell(EMPTY_CELL);
            } else {
                setFocusedCell(new TreeTablePosition(treeTableView, index, null));
            }
        }

        /**
         * Attempts to move focus to the cell above the currently focused cell.
         */
        @Override public void focusAboveCell() {
            TreeTablePosition cell = getFocusedCell();

            if (getFocusedIndex() == -1) {
                focus(getItemCount() - 1, cell.getTableColumn());
            } else if (getFocusedIndex() > 0) {
                focus(getFocusedIndex() - 1, cell.getTableColumn());
            }
        }

        /**
         * Attempts to move focus to the cell below the currently focused cell.
         */
        @Override public void focusBelowCell() {
            TreeTablePosition cell = getFocusedCell();
            if (getFocusedIndex() == -1) {
                focus(0, cell.getTableColumn());
            } else if (getFocusedIndex() != getItemCount() -1) {
                focus(getFocusedIndex() + 1, cell.getTableColumn());
            }
        }

        /**
         * Attempts to move focus to the cell to the left of the currently focused cell.
         */
        @Override public void focusLeftCell() {
            TreeTablePosition cell = getFocusedCell();
            if (cell.getColumn() <= 0) return;
            focus(cell.getRow(), getTableColumn(cell.getTableColumn(), -1));
        }

        /**
         * Attempts to move focus to the cell to the right of the the currently focused cell.
         */
        @Override public void focusRightCell() {
            TreeTablePosition cell = getFocusedCell();
            if (cell.getColumn() == getColumnCount() - 1) return;
            focus(cell.getRow(), getTableColumn(cell.getTableColumn(), 1));
        }



         /***********************************************************************
         *                                                                     *
         * Private Implementation                                              *
         *                                                                     *
         **********************************************************************/

        private int getColumnCount() {
            return treeTableView.getVisibleLeafColumns().size();
        }

        // Gets a table column to the left or right of the current one, given an offset
        private TreeTableColumn<S,?> getTableColumn(TreeTableColumn<S,?> column, int offset) {
            int columnIndex = treeTableView.getVisibleLeafIndex(column);
            int newColumnIndex = columnIndex + offset;
            return treeTableView.getVisibleLeafColumn(newColumnIndex);
        }
    }
}
