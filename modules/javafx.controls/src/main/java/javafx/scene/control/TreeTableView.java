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

import com.sun.javafx.collections.MappingChange;
import com.sun.javafx.collections.NonIterableChange;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.SelectedCellsMap;

import com.sun.javafx.scene.control.behavior.TableCellBehavior;
import com.sun.javafx.scene.control.behavior.TableCellBehaviorBase;
import com.sun.javafx.scene.control.behavior.TreeTableCellBehavior;

import javafx.beans.property.DoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;

import javafx.css.converter.SizeConverter;
import com.sun.javafx.scene.control.ReadOnlyUnbackedObservableList;
import com.sun.javafx.scene.control.TableColumnComparatorBase;

import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.event.WeakEventHandler;

import javafx.scene.control.skin.TreeTableViewSkin;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javafx.application.Platform;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.util.Callback;

/**
 * The TreeTableView control is designed to visualize an unlimited number of rows
 * of data, broken out into columns. The TreeTableView control is conceptually
 * very similar to the {@link TreeView} and {@link TableView} controls,
 * and as you read on you'll come to see the APIs are largely the same.
 * However, to give a high-level overview, you'll note that the TreeTableView
 * uses the same {@link TreeItem} API as {@link TreeView},
 * and that you therefore are required to simply set the
 * {@link #rootProperty() root node} in the TreeTableView. Similarly, the
 * TreeTableView control makes use of the same TableColumn-based approach that
 * the {@link TableView} control uses, except instead of using the
 * TableView-specific {@link TableColumn} class, you should instead use the
 * TreeTableView-specific {@link TreeTableColumn} class instead. For an
 * example on how to create a TreeTableView instance, refer to the 'Creating a
 * TreeTableView' control section below.
 *
 * <p>As with the {@link TableView} control, the TreeTableView control has a
 * number of features, including:
 * <ul>
 * <li>Powerful {@link TreeTableColumn} API:
 *   <ul>
 *   <li>Support for {@link TreeTableColumn#cellFactoryProperty() cell factories} to
 *      easily customize {@link Cell cell} contents in both rendering and editing
 *      states.
 *   <li>Specification of {@link TreeTableColumn#minWidthProperty() minWidth}/
 *      {@link TreeTableColumn#prefWidthProperty() prefWidth}/
 *      {@link TreeTableColumn#maxWidthProperty() maxWidth},
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
 *
 * <h2>Creating a TreeTableView</h2>
 *
 * <p>Creating a TreeTableView is a multi-step process, and also depends on the
 * underlying data model needing to be represented. For this example we'll use
 * the TreeTableView to visualise a file system, and will therefore make use
 * of an imaginary (and vastly simplified) File class as defined below:
 *
 * <pre> {@code public class File {
 *     private StringProperty name;
 *     public void setName(String value) { nameProperty().set(value); }
 *     public String getName() { return nameProperty().get(); }
 *     public StringProperty nameProperty() {
 *         if (name == null) name = new SimpleStringProperty(this, "name");
 *         return name;
 *     }
 *
 *     private LongProperty lastModified;
 *     public void setLastModified(long value) { lastModifiedProperty().set(value); }
 *     public long getLastModified() { return lastModifiedProperty().get(); }
 *     public LongProperty lastModifiedProperty() {
 *         if (lastModified == null) lastModified = new SimpleLongProperty(this, "lastModified");
 *         return lastModified;
 *     }
 *
 *     public File(String name, long size) {
 *         setName(name);
 *         setSize(size);
 *     }
 * }}</pre>
 *
 * <p>The data we will use for this example is a single root with 3 files:
 *
 * <pre> {@code File rootFile = new File("Images", 900);
 * List<File> files = List.of(
 *     new File("Cat.png", 300),
 *     new File("Dog.png", 500),
 *     new File("Bird.png", 100));}</pre>
 *
 * <p>Firstly, we need to create a data model. As mentioned, for this example,
 * we'll be representing a file system using File instances. To do this, we need
 * to define the root node of the tree table and its hierarchy:
 *
 * <pre> {@code TreeItem<File> root = new TreeItem<>(rootFile);
 * files.forEach(file -> root.getChildren().add(new TreeItem<>(file)));}</pre>
 *
 * <p> Then we create a TreeTableView instance:
 *
 * <pre> {@code TreeTableView<File> treeTable = new TreeTableView<>(root);}</pre>
 *
 * <p>With the root set as such, the TreeTableView will automatically update whenever
 * the {@link TreeItem#getChildren() children} of the root change.
 *
 * <p>At this point we have a TreeTableView hooked up to observe the root
 * TreeItem instance. The missing ingredient
 * now is the means of splitting out the data contained within the model and
 * representing it in one or more {@link TreeTableColumn} instances. To
 * create a two-column TreeTableView to show the file name and size
 * properties, we write:
 *
 * <pre> {@code TreeTableColumns<File, String> fileNameCol = new TreeTableColumn<>("Filename");
 * TreeTableColumns<File, Long> sizeCol = new TreeTableColumn<>("Size");
 *
 * treeTable.getColumns().setAll(fileNameCol, sizeCol);}</pre>
 *
 * <p>With the code shown above we have nearly fully defined the minimum properties
 * required to create a TreeTableView instance. The only thing missing is the
 * {@link javafx.scene.control.TreeTableColumn#cellValueFactoryProperty() cell value factories}
 * for the two columns - it is these that are responsible for determining the value
 * of a cell in a given row. Commonly these can be specified using the
 * {@link javafx.scene.control.cell.TreeItemPropertyValueFactory} class, but
 * failing that you can also create an anonymous inner class and do whatever is
 * necessary. For example, using {@link javafx.scene.control.cell.TreeItemPropertyValueFactory}
 * you would do the following:
 *
 * <pre> {@code fileNameCol.setCellValueFactory(new TreeItemPropertyValueFactory(rootFile.nameProperty().getName()));
 * sizeCol.setCellValueFactory(new TreeItemPropertyValueFactory(rootFile.sizeProperty().getName()));}</pre>
 *
 * <img src="doc-files/TreeTableView.png" alt="Image of the TreeTableView control">
 *
 * <p>Running this code will result in a TreeTableView as shown above with two columns
 * for name and size. Any other properties the File class might have will not be shown,
 * as no TreeTableColumns are defined for them.
 *
 * <h3>TreeTableView support for classes that don't contain properties</h3>
 *
 * <p>The code shown above is the shortest possible code for creating a TreeTableView
 * when the domain objects are designed with JavaFX properties in mind
 * (additionally, {@link javafx.scene.control.cell.TreeItemPropertyValueFactory} supports
 * normal JavaBean properties too, although there is a caveat to this, so refer
 * to the class documentation for more information). When this is not the case,
 * it is necessary to provide a custom cell value factory. More information
 * about cell value factories can be found in the {@link TreeTableColumn} API
 * documentation, but briefly, here is how a TreeTableColumns could be specified:
 *
 * <pre> {@code firstNameCol.setCellValueFactory(new Callback<CellDataFeatures<Person, String>, ObservableValue<String>>() {
 *     public ObservableValue<String> call(CellDataFeatures<Person, String> p) {
 *         // p.getValue() returns the TreeItem<Person> instance for a particular TreeTableView row,
 *         // p.getValue().getValue() returns the Person instance inside the TreeItem<Person>
 *         return p.getValue().getValue().firstNameProperty();
 *     }
 * });
 *
 * // or with a lambda expression:
 * firstNameCol.setCellValueFactory(p -> p.getValue().getValue().firstNameProperty());}</pre>
 *
 * <h3>TreeTableView Selection / Focus APIs</h3>
 * <p>To track selection and focus, it is necessary to become familiar with the
 * {@link SelectionModel} and {@link FocusModel} classes. A TreeTableView has at most
 * one instance of each of these classes, available from
 * {@link #selectionModelProperty() selectionModel} and
 * {@link #focusModelProperty() focusModel} properties, respectively.
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
 * <pre> {@code treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);}</pre>
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
 * {@link TreeTableColumn#cellFactoryProperty() cell factory} property.
 *
 * <p>See the {@link Cell} class documentation for a more complete
 * description of how to write custom Cells.
 *
 * <h3>Editing</h3>
 * <p>This control supports inline editing of values, and this section attempts to
 * give an overview of the available APIs and how you should use them.</p>
 *
 * <p>Firstly, cell editing most commonly requires a different user interface
 * than when a cell is not being edited. This is the responsibility of the
 * {@link Cell} implementation being used. For TreeTableView, it is highly
 * recommended that editing be
 * {@link javafx.scene.control.TreeTableColumn#cellFactoryProperty() per-TreeTableColumn},
 * rather than {@link #rowFactoryProperty() per row}, as more often than not
 * you want users to edit each column value differently, and this approach allows
 * for editors specific to each column. It is your choice whether the cell is
 * permanently in an editing state (e.g. this is common for {@link CheckBox} cells),
 * or to switch to a different UI when editing begins (e.g. when a double-click
 * is received on a cell).</p>
 *
 * <p>To know when editing has been requested on a cell,
 * simply override the {@link javafx.scene.control.Cell#startEdit()} method, and
 * update the cell {@link javafx.scene.control.Cell#textProperty() text} and
 * {@link javafx.scene.control.Cell#graphicProperty() graphic} properties as
 * appropriate (e.g. set the text to null and set the graphic to be a
 * {@link TextField}). Additionally, you should also override
 * {@link Cell#cancelEdit()} to reset the UI back to its original visual state
 * when the editing concludes. In both cases it is important that you also
 * ensure that you call the super method to have the cell perform all duties it
 * must do to enter or exit its editing mode.</p>
 *
 * <p>Once your cell is in an editing state, the next thing you are most probably
 * interested in is how to commit or cancel the editing that is taking place. This is your
 * responsibility as the cell factory provider. Your cell implementation will know
 * when the editing is over, based on the user input (e.g. when the user presses
 * the Enter or ESC keys on their keyboard). When this happens, it is your
 * responsibility to call {@link Cell#commitEdit(Object)} or
 * {@link Cell#cancelEdit()}, as appropriate.</p>
 *
 * <p>When you call {@link Cell#commitEdit(Object)} an event is fired to the
 * TreeTableView, which you can observe by adding an {@link EventHandler} via
 * {@link TreeTableColumn#setOnEditCommit(javafx.event.EventHandler)}. Similarly,
 * you can also observe edit events for
 * {@link TreeTableColumn#setOnEditStart(javafx.event.EventHandler) edit start}
 * and {@link TreeTableColumn#setOnEditCancel(javafx.event.EventHandler) edit cancel}.</p>
 *
 * <p>By default the TreeTableColumn edit commit handler is non-null, with a default
 * handler that attempts to overwrite the property value for the
 * item in the currently-being-edited row. It is able to do this as the
 * {@link Cell#commitEdit(Object)} method is passed in the new value, and this
 * is passed along to the edit commit handler via the
 * {@link javafx.scene.control.TreeTableColumn.CellEditEvent CellEditEvent} that is
 * fired. It is simply a matter of calling
 * {@link javafx.scene.control.TreeTableColumn.CellEditEvent#getNewValue()} to
 * retrieve this value.
 *
 * <p>It is very important to note that if you call
 * {@link TreeTableColumn#setOnEditCommit(javafx.event.EventHandler)} with your own
 * {@link EventHandler}, then you will be removing the default handler. Unless
 * you then handle the writeback to the property (or the relevant data source),
 * nothing will happen. You can work around this by using the
 * {@link TreeTableColumn#addEventHandler(javafx.event.EventType, javafx.event.EventHandler)}
 * method to add a {@link TreeTableColumn#EDIT_COMMIT_EVENT} {@link EventType} with
 * your desired {@link EventHandler} as the second argument. Using this method,
 * you will not replace the default implementation, but you will be notified when
 * an edit commit has occurred.</p>
 *
 * <p>Hopefully this summary answers some of the commonly asked questions.
 * Fortunately, JavaFX ships with a number of pre-built cell factories that
 * handle all the editing requirements on your behalf. You can find these
 * pre-built cell factories in the javafx.scene.control.cell package.</p>
 *
 * @see TreeTableColumn
 * @see TreeTablePosition
 * @param <S> The type of the TreeItem instances used in this TreeTableView.
 * @since JavaFX 8.0
 */
@DefaultProperty("root")
public class TreeTableView<S> extends Control {

    /* *************************************************************************
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
        setAccessibleRole(AccessibleRole.TREE_TABLE_VIEW);

        setRoot(root);
        updateExpandedItemCount(root);

        // install default selection and focus models - it's unlikely this will be changed
        // by many users.
        setSelectionModel(new TreeTableViewArrayListSelectionModel<S>(this));
        setFocusModel(new TreeTableViewFocusModel<S>(this));

        // we watch the columns list, such that when it changes we can update
        // the leaf columns and visible leaf columns lists (which are read-only).
        getColumns().addListener(weakColumnsObserver);

        // watch for changes to the sort order list - and when it changes run
        // the sort method.
        getSortOrder().addListener((ListChangeListener.Change<? extends TreeTableColumn<S, ?>> c) -> {
            doSort(TableUtil.SortEventType.SORT_ORDER_CHANGE, c);
        });

        // We're watching for changes to the content width such
        // that the resize policy can be run if necessary. This comes from
        // TreeTableViewSkin.
        getProperties().addListener((MapChangeListener<Object, Object>) c -> {
            if (c.wasAdded() && TableView.SET_CONTENT_WIDTH.equals(c.getKey())) {
                if (c.getValueAdded() instanceof Number) {
                    setContentWidth((Double) c.getValueAdded());
                }
                getProperties().remove(TableView.SET_CONTENT_WIDTH);
            }
        });

        pseudoClassStateChanged(PseudoClass.getPseudoClass(getColumnResizePolicy().toString()), true);

        isInited = true;
    }



    /* *************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * An EventType that indicates some edit event has occurred. It is the parent
     * type of all other edit events: {@link #editStartEvent},
     *  {@link #editCommitEvent} and {@link #editCancelEvent}.
     *
     * @param <S> The type of the TreeItem instances used in this TreeTableView
     * @return An EventType that indicates some edit event has occurred
     */
    @SuppressWarnings("unchecked")
    public static <S> EventType<TreeTableView.EditEvent<S>> editAnyEvent() {
        return (EventType<TreeTableView.EditEvent<S>>) EDIT_ANY_EVENT;
    }
    private static final EventType<?> EDIT_ANY_EVENT =
            new EventType<>(Event.ANY, "TREE_TABLE_VIEW_EDIT");

    /**
     * An EventType used to indicate that an edit event has started within the
     * TreeTableView upon which the event was fired.
     *
     * @param <S> The type of the TreeItem instances used in this TreeTableView
     * @return An EventType used to indicate that an edit event has started
     */
    @SuppressWarnings("unchecked")
    public static <S> EventType<TreeTableView.EditEvent<S>> editStartEvent() {
        return (EventType<TreeTableView.EditEvent<S>>) EDIT_START_EVENT;
    }
    private static final EventType<?> EDIT_START_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_START");

    /**
     * An EventType used to indicate that an edit event has just been canceled
     * within the TreeTableView upon which the event was fired.
     *
     * @param <S> The type of the TreeItem instances used in this TreeTableView
     * @return An EventType used to indicate that an edit event has just been
     *      canceled
     */
    @SuppressWarnings("unchecked")
    public static <S> EventType<TreeTableView.EditEvent<S>> editCancelEvent() {
        return (EventType<TreeTableView.EditEvent<S>>) EDIT_CANCEL_EVENT;
    }
    private static final EventType<?> EDIT_CANCEL_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_CANCEL");

    /**
     * An EventType that is used to indicate that an edit in a TreeTableView has been
     * committed. This means that user has made changes to the data of a
     * TreeItem, and that the UI should be updated.
     *
     * @param <S> The type of the TreeItem instances used in this TreeTableView
     * @return An EventType that is used to indicate that an edit in a TreeTableView
     *      has been committed
     */
    @SuppressWarnings("unchecked")
    public static <S> EventType<TreeTableView.EditEvent<S>> editCommitEvent() {
        return (EventType<TreeTableView.EditEvent<S>>) EDIT_COMMIT_EVENT;
    }
    private static final EventType<?> EDIT_COMMIT_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_COMMIT");

    /**
     * Returns the number of levels of 'indentation' of the given TreeItem,
     * based on how many times {@link javafx.scene.control.TreeItem#getParent()}
     * can be recursively called. If the TreeItem does not have any parent set,
     * the returned value will be zero. For each time getParent() is recursively
     * called, the returned value is incremented by one.
     *
     * <p><strong>Important note: </strong>This method is deprecated as it does
     * not consider the root node. This means that this method will iterate
     * past the root node of the TreeTableView control, if the root node has a parent.
     * If this is important, call {@link TreeTableView#getTreeItemLevel(TreeItem)}
     * instead.
     *
     * @param node The TreeItem for which the level is needed.
     * @return An integer representing the number of parents above the given node,
     *         or -1 if the given TreeItem is null.
     * @deprecated This method does not correctly calculate the distance from the
     *          given TreeItem to the root of the TreeTableView. As of JavaFX 8.0_20,
     *          the proper way to do this is via
     *          {@link TreeTableView#getTreeItemLevel(TreeItem)}
     */
    @Deprecated(since="8u20")
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
            isFirstRun = ! isFirstRun ? false : ! result;
            return result;
        }
    };

    /**
     * The default {@link #sortPolicyProperty() sort policy} that this TreeTableView
     * will use if no other policy is specified. The sort policy is a simple
     * {@link Callback} that accepts a TreeTableView as the sole argument and expects
     * a Boolean response representing whether the sort succeeded or not. A Boolean
     * response of true represents success, and a response of false (or null) will
     * be considered to represent failure.
     */
    public static final Callback<TreeTableView, Boolean> DEFAULT_SORT_POLICY = new Callback<TreeTableView, Boolean>() {
        @Override public Boolean call(TreeTableView table) {
            try {
                TreeItem rootItem = table.getRoot();
                if (rootItem == null || rootItem.getChildren().isEmpty()) return false;

                TreeSortMode sortMode = table.getSortMode();
                if (sortMode == null) return false;

                rootItem.lastSortMode = sortMode;
                rootItem.lastComparator = table.getComparator();
                rootItem.sort();
                return true;
            } catch (UnsupportedOperationException e) {
                // TODO might need to support other exception types including:
                // ClassCastException - if the class of the specified element prevents it from being added to this list
                // NullPointerException - if the specified element is null and this list does not permit null elements
                // IllegalArgumentException - if some property of this element prevents it from being added to this list

                // If we are here the list does not support sorting, so we gracefully
                // fail the sort request and ensure the UI is put back to its previous
                // state. This is handled in the code that calls the sort policy.

                return false;
            }
        }
    };



    /* *************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    // used in the tree item modification event listener. Used by the
    // layoutChildren method to determine whether the tree item count should
    // be recalculated.
    private boolean expandedItemCountDirty = true;

    // Used in the getTreeItem(int row) method to act as a cache.
    // See RT-26716 for the justification and performance gains.
    private Map<Integer, SoftReference<TreeItem<S>>> treeItemCacheMap = new HashMap<>();

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
    // package protected for testing only
    double contentWidth;

    // Used to minimise the amount of work performed prior to the table being
    // completely initialised. In particular it reduces the amount of column
    // resize operations that occur, which slightly improves startup time.
    private boolean isInited = false;



    /* *************************************************************************
     *                                                                         *
     * Callbacks and Events                                                    *
     *                                                                         *
     **************************************************************************/

    // we use this to forward events that have bubbled up TreeItem instances
    // to the TreeTableViewSkin, to force it to recalculate teh item count and redraw
    // if necessary
    private final EventHandler<TreeItem.TreeModificationEvent<S>> rootEvent = e -> {
        // this forces layoutChildren at the next pulse, and therefore
        // updates the item count if necessary
        EventType<?> eventType = e.getEventType();
        boolean match = false;
        while (eventType != null) {
            if (eventType.equals(TreeItem.<S>expandedItemCountChangeEvent())) {
                match = true;
                break;
            }
            eventType = eventType.getSuperType();
        }

        if (match) {
            expandedItemCountDirty = true;
            requestLayout();
        }
    };

    private final ListChangeListener<TreeTableColumn<S,?>> columnsObserver = new ListChangeListener<TreeTableColumn<S,?>>() {
        @Override public void onChanged(ListChangeListener.Change<? extends TreeTableColumn<S,?>> c) {
            final List<TreeTableColumn<S,?>> columns = getColumns();

            // Fix for RT-39822 - don't allow the same column to be installed twice
            while (c.next()) {
                if (c.wasAdded()) {
                    List<TreeTableColumn<S,?>> duplicates = new ArrayList<>();
                    for (TreeTableColumn<S,?> addedColumn : c.getAddedSubList()) {
                        if (addedColumn == null) continue;

                        int count = 0;
                        for (TreeTableColumn<S,?> column : columns) {
                            if (addedColumn == column) {
                                count++;
                            }
                        }

                        if (count > 1) {
                            duplicates.add(addedColumn);
                        }
                    }

                    if (!duplicates.isEmpty()) {
                        String titleList = "";
                        for (TreeTableColumn<S,?> dupe : duplicates) {
                            titleList += "'" + dupe.getText() + "', ";
                        }
                        throw new IllegalStateException("Duplicate TreeTableColumns detected in TreeTableView columns list with titles " + titleList);
                    }
                }
            }
            c.reset();

            // Fix for RT-15194: Need to remove removed columns from the
            // sortOrder list.
            List<TreeTableColumn<S,?>> toRemove = new ArrayList<TreeTableColumn<S,?>>();
            while (c.next()) {
                final List<? extends TreeTableColumn<S, ?>> removed = c.getRemoved();
                final List<? extends TreeTableColumn<S, ?>> added = c.getAddedSubList();

                if (c.wasRemoved()) {
                    toRemove.addAll(removed);
                    for (TreeTableColumn<S,?> tc : removed) {
                        tc.setTreeTableView(null);
                    }
                }

                if (c.wasAdded()) {
                    toRemove.removeAll(added);
                    for (TreeTableColumn<S,?> tc : added) {
                        tc.setTreeTableView(TreeTableView.this);
                    }
                }

                // set up listeners
                TableUtil.removeColumnsListener(removed, weakColumnsObserver);
                TableUtil.addColumnsListener(added, weakColumnsObserver);

                TableUtil.removeTableColumnListener(c.getRemoved(),
                        weakColumnVisibleObserver,
                        weakColumnSortableObserver,
                        weakColumnSortTypeObserver,
                        weakColumnComparatorObserver);
                TableUtil.addTableColumnListener(c.getAddedSubList(),
                        weakColumnVisibleObserver,
                        weakColumnSortableObserver,
                        weakColumnSortTypeObserver,
                        weakColumnComparatorObserver);
            }

            // We don't maintain a bind for leafColumns, we simply call this update
            // function behind the scenes in the appropriate places.
            updateVisibleLeafColumns();

            sortOrder.removeAll(toRemove);

            // Fix for RT-38892.
            final TreeTableViewFocusModel<S> fm = getFocusModel();
            final TreeTableViewSelectionModel<S> sm = getSelectionModel();
            c.reset();

            // we need to collect together all removed and all added columns, because
            // the code below works on the actually removed columns. If we perform
            // the code within this while loop, we'll be deselecting columns that
            // should be deselected (because they have just moved place, for example).
            List<TreeTableColumn<S,?>> removed = new ArrayList<>();
            List<TreeTableColumn<S,?>> added = new ArrayList<>();
            while (c.next()) {
                if (c.wasRemoved()) {
                    removed.addAll(c.getRemoved());
                }
                if (c.wasAdded()) {
                    added.addAll(c.getAddedSubList());
                }
            }
            removed.removeAll(added);


            // Fix for focus - we simply move focus to a cell to the left
            // of the focused cell if the focused cell was located within
            // a column that has been removed.
            if (fm != null) {
                TreeTablePosition<S, ?> focusedCell = fm.getFocusedCell();
                boolean match = false;
                for (TreeTableColumn<S, ?> tc : removed) {
                    match = focusedCell != null && focusedCell.getTableColumn() == tc;
                    if (match) {
                        break;
                    }
                }

                if (match) {
                    int matchingColumnIndex = lastKnownColumnIndex.getOrDefault(focusedCell.getTableColumn(), 0);
                    int newFocusColumnIndex =
                            matchingColumnIndex == 0 ? 0 :
                                    Math.min(getVisibleLeafColumns().size() - 1, matchingColumnIndex - 1);
                    fm.focus(focusedCell.getRow(), getVisibleLeafColumn(newFocusColumnIndex));
                }
            }

            // Fix for selection - we remove selection from all cells that
            // were within the removed column.
            if (sm != null) {
                List<TreeTablePosition> selectedCells = new ArrayList<>(sm.getSelectedCells());
                for (TreeTablePosition selectedCell : selectedCells) {
                    boolean match = false;
                    for (TreeTableColumn<S, ?> tc : removed) {
                        match = selectedCell != null && selectedCell.getTableColumn() == tc;
                        if (match) break;
                    }

                    if (match) {
                        // we can't just use the selectedCell.getTableColumn(), as that
                        // column no longer exists and therefore its index is not correct.
                        int matchingColumnIndex = lastKnownColumnIndex.getOrDefault(selectedCell.getTableColumn(), -1);
                        if (matchingColumnIndex == -1) continue;

                        if (sm instanceof TreeTableViewArrayListSelectionModel) {
                            // Also, because the table column no longer exists in the columns
                            // list at this point, we can't just call:
                            // sm.clearSelection(selectedCell.getRow(), selectedCell.getTableColumn());
                            // as the tableColumn would map to an index of -1, which means that
                            // selection will not be cleared. Instead, we have to create
                            // a new TablePosition with a fixed column index and use that.
                            TreeTablePosition<S,?> fixedTablePosition =
                                    new TreeTablePosition<S,Object>(TreeTableView.this,
                                            selectedCell.getRow(),
                                            selectedCell.getTableColumn());
                            fixedTablePosition.fixedColumnIndex = matchingColumnIndex;

                            ((TreeTableViewArrayListSelectionModel)sm).clearSelection(fixedTablePosition);
                        } else {
                            sm.clearSelection(selectedCell.getRow(), selectedCell.getTableColumn());
                        }
                    }
                }
            }


            // update the lastKnownColumnIndex map
            lastKnownColumnIndex.clear();
            for (TreeTableColumn<S,?> tc : getColumns()) {
                int index = getVisibleLeafIndex(tc);
                if (index > -1) {
                    lastKnownColumnIndex.put(tc, index);
                }
            }
        }
    };

    private final WeakHashMap<TreeTableColumn<S,?>, Integer> lastKnownColumnIndex = new WeakHashMap<>();

    private final InvalidationListener columnVisibleObserver = valueModel -> {
        updateVisibleLeafColumns();
    };

    private final InvalidationListener columnSortableObserver = valueModel -> {
        TreeTableColumn col = (TreeTableColumn) ((BooleanProperty)valueModel).getBean();
        if (! getSortOrder().contains(col)) return;
        doSort(TableUtil.SortEventType.COLUMN_SORTABLE_CHANGE, col);
    };

    private final InvalidationListener columnSortTypeObserver = valueModel -> {
        TreeTableColumn col = (TreeTableColumn) ((ObjectProperty)valueModel).getBean();
        if (! getSortOrder().contains(col)) return;
        doSort(TableUtil.SortEventType.COLUMN_SORT_TYPE_CHANGE, col);
    };

    private final InvalidationListener columnComparatorObserver = valueModel -> {
        TreeTableColumn col = (TreeTableColumn) ((SimpleObjectProperty)valueModel).getBean();
        if (! getSortOrder().contains(col)) return;
        doSort(TableUtil.SortEventType.COLUMN_COMPARATOR_CHANGE, col);
    };

    /* proxy pseudo-class state change from selectionModel's cellSelectionEnabledProperty */
    private final InvalidationListener cellSelectionModelInvalidationListener = o -> {
        boolean isCellSelection = ((BooleanProperty)o).get();
        pseudoClassStateChanged(PSEUDO_CLASS_CELL_SELECTION,  isCellSelection);
        pseudoClassStateChanged(PSEUDO_CLASS_ROW_SELECTION,  !isCellSelection);
    };

    private WeakEventHandler<TreeItem.TreeModificationEvent<S>> weakRootEventListener;

    private final WeakInvalidationListener weakColumnVisibleObserver =
            new WeakInvalidationListener(columnVisibleObserver);

    private final WeakInvalidationListener weakColumnSortableObserver =
            new WeakInvalidationListener(columnSortableObserver);

    private final WeakInvalidationListener weakColumnSortTypeObserver =
            new WeakInvalidationListener(columnSortTypeObserver);

    private final WeakInvalidationListener weakColumnComparatorObserver =
            new WeakInvalidationListener(columnComparatorObserver);

    private final WeakListChangeListener<TreeTableColumn<S,?>> weakColumnsObserver =
            new WeakListChangeListener<TreeTableColumn<S,?>>(columnsObserver);

    private final WeakInvalidationListener weakCellSelectionModelInvalidationListener =
            new WeakInvalidationListener(cellSelectionModelInvalidationListener);



    /* *************************************************************************
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
                weakRootEventListener = new WeakEventHandler<>(rootEvent);
                getRoot().addEventHandler(TreeItem.<S>treeNotificationEvent(), weakRootEventListener);
                weakOldItem = new WeakReference<>(root);
            }

            // Fix for RT-35763
            getSortOrder().clear();

            expandedItemCountDirty = true;
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
     * @return the root property
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
     * @return true if the root of the TreeTableView should be shown
     */
    public final boolean isShowRoot() {
        return showRoot == null ? true : showRoot.get();
    }

    /**
     * Property that represents whether or not the TreeTableView root node is visible.
     * @return the show root property
     */
    public final BooleanProperty showRootProperty() {
        if (showRoot == null) {
            showRoot = new SimpleBooleanProperty(this, "showRoot", true) {
                @Override protected void invalidated() {
                    updateRootExpanded();
                    updateExpandedItemCount(getRoot());
                }
            };
        }
        return showRoot;
    }



    // --- Tree Column
    private ObjectProperty<TreeTableColumn<S,?>> treeColumn;
    /**
     * Property that represents which column should have the disclosure node
     * shown in it (that is, the column with the arrow). By default this will be
     * the left-most column if this property is null, otherwise it will be the
     * specified column assuming it is non-null and contained within the
     * {@link #getVisibleLeafColumns() visible leaf columns} list.
     * @return the tree column property
     */
    public final ObjectProperty<TreeTableColumn<S,?>> treeColumnProperty() {
        if (treeColumn == null) {
            treeColumn = new SimpleObjectProperty<>(this, "treeColumn", null);
        }
        return treeColumn;
    }
    public final void setTreeColumn(TreeTableColumn<S,?> value) {
        treeColumnProperty().set(value);
    }
    public final TreeTableColumn<S,?> getTreeColumn() {
        return treeColumn == null ? null : treeColumn.get();
    }



    // --- Selection Model
    private ObjectProperty<TreeTableViewSelectionModel<S>> selectionModel;

    /**
     * Sets the {@link MultipleSelectionModel} to be used in the TreeTableView.
     * Despite a TreeTableView requiring a <code><b>Multiple</b>SelectionModel</code>,
     * it is possible to configure it to only allow single selection (see
     * {@link MultipleSelectionModel#setSelectionMode(javafx.scene.control.SelectionMode)}
     * for more information).
     * @param value the {@link MultipleSelectionModel} to be used
     */
    public final void setSelectionModel(TreeTableViewSelectionModel<S> value) {
        selectionModelProperty().set(value);
    }

    /**
     * Returns the currently installed selection model.
     * @return the currently installed selection model
     */
    public final TreeTableViewSelectionModel<S> getSelectionModel() {
        return selectionModel == null ? null : selectionModel.get();
    }

    /**
     * The SelectionModel provides the API through which it is possible
     * to select single or multiple items within a TreeTableView, as  well as inspect
     * which rows have been selected by the user. Note that it has a generic
     * type that must match the type of the TreeTableView itself.
     * @return the selection model property
     */
    public final ObjectProperty<TreeTableViewSelectionModel<S>> selectionModelProperty() {
        if (selectionModel == null) {
            selectionModel = new SimpleObjectProperty<TreeTableViewSelectionModel<S>>(this, "selectionModel") {

                TreeTableViewSelectionModel<S> oldValue = null;

                @Override protected void invalidated() {
                    // need to listen to the cellSelectionEnabledProperty
                    // in order to set pseudo-class state
                    if (oldValue != null) {
                        oldValue.cellSelectionEnabledProperty().removeListener(weakCellSelectionModelInvalidationListener);

                        if (oldValue instanceof TreeTableViewArrayListSelectionModel) {
                            ((TreeTableViewArrayListSelectionModel)oldValue).dispose();
                        }
                    }

                    oldValue = get();

                    if (oldValue != null) {
                        oldValue.cellSelectionEnabledProperty().addListener(weakCellSelectionModelInvalidationListener);
                        // fake invalidation to ensure updated pseudo-class states
                        weakCellSelectionModelInvalidationListener.invalidated(oldValue.cellSelectionEnabledProperty());
                    }
                }
            };
        }
        return selectionModel;
    }


    // --- Focus Model
    private ObjectProperty<TreeTableViewFocusModel<S>> focusModel;

    /**
     * Sets the {@link FocusModel} to be used in the TreeTableView.
     * @param value the {@link FocusModel} to be used
     */
    public final void setFocusModel(TreeTableViewFocusModel<S> value) {
        focusModelProperty().set(value);
    }

    /**
     * Returns the currently installed {@link FocusModel}.
     * @return the currently installed {@link FocusModel}
     */
    public final TreeTableViewFocusModel<S> getFocusModel() {
        return focusModel == null ? null : focusModel.get();
    }

    /**
     * The FocusModel provides the API through which it is possible
     * to control focus on zero or one rows of the TreeTableView. Generally the
     * default implementation should be more than sufficient.
     * @return the focus model property
     */
    public final ObjectProperty<TreeTableViewFocusModel<S>> focusModelProperty() {
        if (focusModel == null) {
            focusModel = new SimpleObjectProperty<TreeTableViewFocusModel<S>>(this, "focusModel");
        }
        return focusModel;
    }


    // --- Tree node count
    /**
     * <p>Represents the number of tree nodes presently able to be visible in the
     * TreeTableView. This is essentially the count of all expanded tree items, and
     * their children.
     *
     * <p>For example, if just the root node is visible, the expandedItemCount will
     * be one. If the root had three children and the root was expanded, the value
     * will be four.
     */
    private ReadOnlyIntegerWrapper expandedItemCount = new ReadOnlyIntegerWrapper(this, "expandedItemCount", 0);
    public final ReadOnlyIntegerProperty expandedItemCountProperty() {
        return expandedItemCount.getReadOnlyProperty();
    }
    private void setExpandedItemCount(int value) {
        expandedItemCount.set(value);
    }
    public final int getExpandedItemCount() {
        if (expandedItemCountDirty) {
            updateExpandedItemCount(getRoot());
        }
        return expandedItemCount.get();
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
     * @return the editable property
     */
    public final BooleanProperty editableProperty() {
        if (editable == null) {
            editable = new SimpleBooleanProperty(this, "editable", false);
        }
        return editable;
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
     * @return the editing cell property
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


    // --- Table menu button visible
    private BooleanProperty tableMenuButtonVisible;
    /**
     * This controls whether a menu button is available when the user clicks
     * in a designated space within the TableView, within which is a radio menu
     * item for each TreeTableColumn in this table. This menu allows for the user to
     * show and hide all TreeTableColumns easily.
     * @return the table menu button visible property
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
     * @return the column resize policy property
     */
    public final ObjectProperty<Callback<TreeTableView.ResizeFeatures, Boolean>> columnResizePolicyProperty() {
        if (columnResizePolicy == null) {
            columnResizePolicy = new SimpleObjectProperty<Callback<TreeTableView.ResizeFeatures, Boolean>>(this, "columnResizePolicy", UNCONSTRAINED_RESIZE_POLICY) {
                private Callback<TreeTableView.ResizeFeatures, Boolean> oldPolicy;

                @Override protected void invalidated() {
                    if (isInited) {
                        get().call(new TreeTableView.ResizeFeatures(TreeTableView.this, null, 0.0));

                        if (oldPolicy != null) {
                            PseudoClass state = PseudoClass.getPseudoClass(oldPolicy.toString());
                            pseudoClassStateChanged(state, false);
                        }
                        if (get() != null) {
                            PseudoClass state = PseudoClass.getPseudoClass(get().toString());
                            pseudoClassStateChanged(state, true);
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
     * @return the row factory property
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
     * @return the placeholder property
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


    // --- Fixed cell size
    private DoubleProperty fixedCellSize;

    /**
     * Sets the new fixed cell size for this control. Any value greater than
     * zero will enable fixed cell size mode, whereas a zero or negative value
     * (or Region.USE_COMPUTED_SIZE) will be used to disabled fixed cell size
     * mode.
     *
     * @param value The new fixed cell size value, or a value less than or equal
     *              to zero (or Region.USE_COMPUTED_SIZE) to disable.
     * @since JavaFX 8.0
     */
    public final void setFixedCellSize(double value) {
        fixedCellSizeProperty().set(value);
    }

    /**
     * Returns the fixed cell size value. A value less than or equal to zero is
     * used to represent that fixed cell size mode is disabled, and a value
     * greater than zero represents the size of all cells in this control.
     *
     * @return A double representing the fixed cell size of this control, or a
     *      value less than or equal to zero if fixed cell size mode is disabled.
     * @since JavaFX 8.0
     */
    public final double getFixedCellSize() {
        return fixedCellSize == null ? Region.USE_COMPUTED_SIZE : fixedCellSize.get();
    }
    /**
     * Specifies whether this control has cells that are a fixed height (of the
     * specified value). If this value is less than or equal to zero,
     * then all cells are individually sized and positioned. This is a slow
     * operation. Therefore, when performance matters and developers are not
     * dependent on variable cell sizes it is a good idea to set the fixed cell
     * size value. Generally cells are around 24px, so setting a fixed cell size
     * of 24 is likely to result in very little difference in visuals, but a
     * improvement to performance.
     *
     * <p>To set this property via CSS, use the -fx-fixed-cell-size property.
     * This should not be confused with the -fx-cell-size property. The difference
     * between these two CSS properties is that -fx-cell-size will size all
     * cells to the specified size, but it will not enforce that this is the
     * only size (thus allowing for variable cell sizes, and preventing the
     * performance gains from being possible). Therefore, when performance matters
     * use -fx-fixed-cell-size, instead of -fx-cell-size. If both properties are
     * specified in CSS, -fx-fixed-cell-size takes precedence.</p>
     *
     * @return the fixed cell size property
     * @since JavaFX 8.0
     */
    public final DoubleProperty fixedCellSizeProperty() {
        if (fixedCellSize == null) {
            fixedCellSize = new StyleableDoubleProperty(Region.USE_COMPUTED_SIZE) {
                @Override public CssMetaData<TreeTableView<?>,Number> getCssMetaData() {
                    return StyleableProperties.FIXED_CELL_SIZE;
                }

                @Override public Object getBean() {
                    return TreeTableView.this;
                }

                @Override public String getName() {
                    return "fixedCellSize";
                }
            };
        }
        return fixedCellSize;
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
            sortMode = new SimpleObjectProperty<>(this, "sortMode", TreeSortMode.ALL_DESCENDANTS);
        }
        return sortMode;
    }
    public final void setSortMode(TreeSortMode value) {
        sortModeProperty().set(value);
    }
    public final TreeSortMode getSortMode() {
        return sortMode == null ? TreeSortMode.ALL_DESCENDANTS : sortMode.get();
    }


    // --- Comparator (built via sortOrder list, so read-only)
    /**
     * The comparator property is a read-only property that is representative of the
     * current state of the {@link #getSortOrder() sort order} list. The sort
     * order list contains the columns that have been added to it either programmatically
     * or via a user clicking on the headers themselves.
     */
    private ReadOnlyObjectWrapper<Comparator<TreeItem<S>>> comparator;
    private void setComparator(Comparator<TreeItem<S>> value) {
        comparatorPropertyImpl().set(value);
    }
    public final Comparator<TreeItem<S>> getComparator() {
        return comparator == null ? null : comparator.get();
    }
    public final ReadOnlyObjectProperty<Comparator<TreeItem<S>>> comparatorProperty() {
        return comparatorPropertyImpl().getReadOnlyProperty();
    }
    private ReadOnlyObjectWrapper<Comparator<TreeItem<S>>> comparatorPropertyImpl() {
        if (comparator == null) {
            comparator = new ReadOnlyObjectWrapper<>(this, "comparator");
        }
        return comparator;
    }


    // --- sortPolicy
    /**
     * The sort policy specifies how sorting in this TreeTableView should be performed.
     * For example, a basic sort policy may just recursively sort the children of
     * the root tree item, whereas a more advanced sort policy may call to a
     * database to perform the necessary sorting on the server-side.
     *
     * <p>TreeTableView ships with a {@link TableView#DEFAULT_SORT_POLICY default
     * sort policy} that does precisely as mentioned above: it simply attempts
     * to sort the tree hierarchy in-place.
     *
     * <p>It is recommended that rather than override the {@link TreeTableView#sort() sort}
     * method that a different sort policy be provided instead.
     */
    private ObjectProperty<Callback<TreeTableView<S>, Boolean>> sortPolicy;
    public final void setSortPolicy(Callback<TreeTableView<S>, Boolean> callback) {
        sortPolicyProperty().set(callback);
    }
    @SuppressWarnings("unchecked")
    public final Callback<TreeTableView<S>, Boolean> getSortPolicy() {
        return sortPolicy == null ?
                (Callback<TreeTableView<S>, Boolean>)(Object) DEFAULT_SORT_POLICY :
                sortPolicy.get();
    }
    @SuppressWarnings("unchecked")
    public final ObjectProperty<Callback<TreeTableView<S>, Boolean>> sortPolicyProperty() {
        if (sortPolicy == null) {
            sortPolicy = new SimpleObjectProperty<Callback<TreeTableView<S>, Boolean>>(
                    this, "sortPolicy", (Callback<TreeTableView<S>, Boolean>)(Object) DEFAULT_SORT_POLICY) {
                @Override protected void invalidated() {
                    sort();
                }
            };
        }
        return sortPolicy;
    }


    // onSort
    /**
     * Called when there's a request to sort the control.
     */
    private ObjectProperty<EventHandler<SortEvent<TreeTableView<S>>>> onSort;

    public void setOnSort(EventHandler<SortEvent<TreeTableView<S>>> value) {
        onSortProperty().set(value);
    }

    public EventHandler<SortEvent<TreeTableView<S>>> getOnSort() {
        if( onSort != null ) {
            return onSort.get();
        }
        return null;
    }

    public ObjectProperty<EventHandler<SortEvent<TreeTableView<S>>>> onSortProperty() {
        if( onSort == null ) {
            onSort = new ObjectPropertyBase<EventHandler<SortEvent<TreeTableView<S>>>>() {
                @Override protected void invalidated() {
                    EventType<SortEvent<TreeTableView<S>>> eventType = SortEvent.sortEvent();
                    EventHandler<SortEvent<TreeTableView<S>>> eventHandler = get();
                    setEventHandler(eventType, eventHandler);
                }

                @Override public Object getBean() {
                    return TreeTableView.this;
                }

                @Override public String getName() {
                    return "onSort";
                }
            };
        }
        return onSort;
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        if (expandedItemCountDirty) {
            updateExpandedItemCount(getRoot());
        }

        super.layoutChildren();
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
        ControlUtils.scrollToIndex(this, index);
    }

    /**
     * Called when there's a request to scroll an index into view using {@link #scrollTo(int)}
     */
    private ObjectProperty<EventHandler<ScrollToEvent<Integer>>> onScrollTo;

    public void setOnScrollTo(EventHandler<ScrollToEvent<Integer>> value) {
        onScrollToProperty().set(value);
    }

    public EventHandler<ScrollToEvent<Integer>> getOnScrollTo() {
        if( onScrollTo != null ) {
            return onScrollTo.get();
        }
        return null;
    }

    public ObjectProperty<EventHandler<ScrollToEvent<Integer>>> onScrollToProperty() {
        if( onScrollTo == null ) {
            onScrollTo = new ObjectPropertyBase<EventHandler<ScrollToEvent<Integer>>>() {
                @Override protected void invalidated() {
                    setEventHandler(ScrollToEvent.scrollToTopIndex(), get());
                }

                @Override public Object getBean() {
                    return TreeTableView.this;
                }

                @Override public String getName() {
                    return "onScrollTo";
                }
            };
        }
        return onScrollTo;
    }

    /**
     * Scrolls the TreeTableView so that the given column is visible within the viewport.
     * @param column The column that should be visible to the user.
     */
    public void scrollToColumn(TreeTableColumn<S, ?> column) {
        ControlUtils.scrollToColumn(this, column);
    }

    /**
     * Scrolls the TreeTableView so that the given index is visible within the viewport.
     * @param columnIndex The index of a column that should be visible to the user.
     */
    public void scrollToColumnIndex(int columnIndex) {
        if( getColumns() != null ) {
            ControlUtils.scrollToColumn(this, getColumns().get(columnIndex));
        }
    }

    /**
     * Called when there's a request to scroll a column into view using {@link #scrollToColumn(TreeTableColumn)}
     * or {@link #scrollToColumnIndex(int)}
     */
    private ObjectProperty<EventHandler<ScrollToEvent<TreeTableColumn<S, ?>>>> onScrollToColumn;

    public void setOnScrollToColumn(EventHandler<ScrollToEvent<TreeTableColumn<S, ?>>> value) {
        onScrollToColumnProperty().set(value);
    }

    public EventHandler<ScrollToEvent<TreeTableColumn<S, ?>>> getOnScrollToColumn() {
        if( onScrollToColumn != null ) {
            return onScrollToColumn.get();
        }
        return null;
    }

    public ObjectProperty<EventHandler<ScrollToEvent<TreeTableColumn<S, ?>>>> onScrollToColumnProperty() {
        if( onScrollToColumn == null ) {
            onScrollToColumn = new ObjectPropertyBase<EventHandler<ScrollToEvent<TreeTableColumn<S, ?>>>>() {
                @Override
                protected void invalidated() {
                    EventType<ScrollToEvent<TreeTableColumn<S, ?>>> type = ScrollToEvent.scrollToColumn();
                    setEventHandler(type, get());
                }
                @Override
                public Object getBean() {
                    return TreeTableView.this;
                }

                @Override
                public String getName() {
                    return "onScrollToColumn";
                }
            };
        }
        return onScrollToColumn;
    }

    /**
     * Returns the index position of the given TreeItem, assuming that it is
     * currently accessible through the tree hierarchy (most notably, that all
     * parent tree items are expanded). If a parent tree item is collapsed,
     * the result is that this method will return -1 to indicate that the
     * given tree item is not accessible in the tree.
     *
     * @param item The TreeItem for which the index is sought.
     * @return An integer representing the location in the current TreeTableView of the
     *      first instance of the given TreeItem, or -1 if it is null or can not
     *      be found (for example, if a parent (all the way up to the root) is
     *      collapsed).
     */
    public int getRow(TreeItem<S> item) {
        return TreeUtil.getRow(item, getRoot(), expandedItemCountDirty, isShowRoot());
    }

    /**
     * Returns the TreeItem in the given index, or null if it is out of bounds.
     *
     * @param row The index of the TreeItem being sought.
     * @return The TreeItem in the given index, or null if it is out of bounds.
     */
    public TreeItem<S> getTreeItem(int row) {
        if (row < 0) return null;

        // normalize the requested row based on whether showRoot is set
        final int _row = isShowRoot() ? row : (row + 1);

        if (expandedItemCountDirty) {
            updateExpandedItemCount(getRoot());
        } else {
            if (treeItemCacheMap.containsKey(_row)) {
                SoftReference<TreeItem<S>> treeItemRef = treeItemCacheMap.get(_row);
                TreeItem<S> treeItem = treeItemRef.get();
                if (treeItem != null) {
                    return treeItem;
                }
            }
        }

        TreeItem<S> treeItem = TreeUtil.getItem(getRoot(), _row, expandedItemCountDirty);
        treeItemCacheMap.put(_row, new SoftReference<>(treeItem));
        return treeItem;
    }

    /**
     * Returns the number of levels of 'indentation' of the given TreeItem,
     * based on how many times getParent() can be recursively called. If the
     * given TreeItem is the root node of this TreeTableView, or if the TreeItem
     * does not have any parent set, the returned value will be zero. For each
     * time getParent() is recursively called, the returned value is incremented
     * by one.
     *
     * @param node The TreeItem for which the level is needed.
     * @return An integer representing the number of parents above the given node,
     *         or -1 if the given TreeItem is null.
     */
    public int getTreeItemLevel(TreeItem<?> node) {
        final TreeItem<?> root = getRoot();

        if (node == null) return -1;
        if (node == root) return 0;

        int level = 0;
        TreeItem<?> parent = node.getParent();
        while (parent != null) {
            level++;

            if (parent == root) {
                break;
            }

            parent = parent.getParent();
        }

        return level;
    }

    /**
     * The TreeTableColumns that are part of this TableView. As the user reorders
     * the TableView columns, this list will be updated to reflect the current
     * visual ordering.
     *
     * <p>Note: to display any data in a TableView, there must be at least one
     * TreeTableColumn in this ObservableList.</p>
     * @return the table table column
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
     * @param column the column
     * @param delta the delta
     * @return true if column resizing is applied
     */
    public boolean resizeColumn(TreeTableColumn<S,?> column, double delta) {
        if (column == null || Double.compare(delta, 0.0) == 0) return false;

        boolean allowed = getColumnResizePolicy().call(new TreeTableView.ResizeFeatures<S>(TreeTableView.this, column, delta));
        if (!allowed) return false;
        return true;
    }

    /**
     * Causes the cell at the given row/column view indexes to switch into
     * its editing state, if it is not already in it, and assuming that the
     * TableView and column are also editable.
     * @param row the row
     * @param column the column
     */
    public void edit(int row, TreeTableColumn<S,?> column) {
        if (!isEditable() || (column != null && ! column.isEditable())) {
            return;
        }

        if (row < 0 && column == null) {
            setEditingCell(null);
        } else {
            setEditingCell(new TreeTablePosition<>(this, row, column));
        }
    }

    /**
     * Returns an unmodifiable list containing the currently visible leaf columns.
     * @return an unmodifiable list containing the currently visible leaf columns
     */
    public ObservableList<TreeTableColumn<S,?>> getVisibleLeafColumns() {
        return unmodifiableVisibleLeafColumns;
    }

    /**
     * Returns the position of the given column, relative to all other
     * visible leaf columns.
     * @param column the column
     * @return the position of the given column, relative to all other
     * visible leaf columns
     */
    public int getVisibleLeafIndex(TreeTableColumn<S,?> column) {
        return getVisibleLeafColumns().indexOf(column);
    }

    /**
     * Returns the TreeTableColumn in the given column index, relative to all other
     * visible leaf columns.
     * @param column the column
     * @return the TreeTableColumn in the given column index, relative to all other
     * visible leaf columns
     */
    public TreeTableColumn<S,?> getVisibleLeafColumn(int column) {
        if (column < 0 || column >= visibleLeafColumns.size()) return null;
        return visibleLeafColumns.get(column);
    }

    private boolean sortingInProgress;
    boolean isSortingInProgress() {
        return sortingInProgress;
    }

    /**
     * The sort method forces the TreeTableView to re-run its sorting algorithm. More
     * often than not it is not necessary to call this method directly, as it is
     * automatically called when the {@link #getSortOrder() sort order},
     * {@link #sortPolicyProperty() sort policy}, or the state of the
     * TreeTableColumn {@link TreeTableColumn#sortTypeProperty() sort type} properties
     * change. In other words, this method should only be called directly when
     * something external changes and a sort is required.
     */
    public void sort() {
        sortingInProgress = true;
        final ObservableList<TreeTableColumn<S,?>> sortOrder = getSortOrder();

        // update the Comparator property
        final Comparator<TreeItem<S>> oldComparator = getComparator();
        setComparator(sortOrder.isEmpty() ? null : new TableColumnComparatorBase.TreeTableColumnComparator(sortOrder));

        // fire the onSort event and check if it is consumed, if
        // so, don't run the sort
        SortEvent<TreeTableView<S>> sortEvent = new SortEvent<>(TreeTableView.this, TreeTableView.this);
        fireEvent(sortEvent);
        if (sortEvent.isConsumed()) {
            // if the sort is consumed we could back out the last action (the code
            // is commented out right below), but we don't as we take it as a
            // sign that the developer has decided to handle the event themselves.

            // sortLock = true;
            // TableUtil.handleSortFailure(sortOrder, lastSortEventType, lastSortEventSupportInfo);
            // sortLock = false;
            sortingInProgress = false;
            return;
        }

        final List<TreeTablePosition<S,?>> prevState = new ArrayList<>(getSelectionModel().getSelectedCells());
        final int itemCount = prevState.size();

        // we set makeAtomic to true here, so that we don't fire intermediate
        // sort events - instead we send a single permutation event at the end
        // of this method.
        getSelectionModel().startAtomic();

        // get the sort policy and run it
        Callback<TreeTableView<S>, Boolean> sortPolicy = getSortPolicy();
        if (sortPolicy == null) {
            sortingInProgress = false;
            return;
        }
        Boolean success = sortPolicy.call(this);

        if (getSortMode() == TreeSortMode.ALL_DESCENDANTS) {
            Set<TreeItem<S>> sortedParents = new HashSet<>();
            for (TreeTablePosition<S,?> selectedPosition : prevState) {
                // This null check is not required ideally.
                // The selectedPosition.getTreeItem() should always return a valid TreeItem.
                // But, it is possible to be null due to JDK-8248217.
                if (selectedPosition.getTreeItem() != null) {
                    TreeItem<S> parent = selectedPosition.getTreeItem().getParent();
                    while (parent != null && sortedParents.add(parent)) {
                        parent.getChildren();
                        parent = parent.getParent();
                    }
                }
            }
        }
        getSelectionModel().stopAtomic();

        if (success == null || ! success) {
            // the sort was a failure. Need to backout if possible
            sortLock = true;
            TableUtil.handleSortFailure(sortOrder, lastSortEventType, lastSortEventSupportInfo);
            setComparator(oldComparator);
            sortLock = false;
        } else {
            // sorting was a success, now we possibly fire an event on the
            // selection model that the items list has 'permutated' to a new ordering

            // FIXME we should support alternative selection model implementations!
            if (getSelectionModel() instanceof TreeTableViewArrayListSelectionModel) {
                final TreeTableViewArrayListSelectionModel<S> sm = (TreeTableViewArrayListSelectionModel<S>) getSelectionModel();
                final ObservableList<TreeTablePosition<S, ?>> newState = sm.getSelectedCells();

                List<TreeTablePosition<S, ?>> removed = new ArrayList<>();
                for (int i = 0; i < itemCount; i++) {
                    TreeTablePosition<S, ?> prevItem = prevState.get(i);
                    if (!newState.contains(prevItem)) {
                        removed.add(prevItem);
                    }
                }
                if (!removed.isEmpty()) {
                    // the sort operation effectively permutates the selectedCells list,
                    // but we cannot fire a permutation event as we are talking about
                    // TreeTablePosition's changing (which may reside in the same list
                    // position before and after the sort). Therefore, we need to fire
                    // a single add/remove event to cover the added and removed positions.
                    ListChangeListener.Change<TreeTablePosition<S, ?>> c = new NonIterableChange.GenericAddRemoveChange<>(0, itemCount, removed, newState);
                    sm.fireCustomSelectedCellsListChangeEvent(c);
                }
            }
            getSelectionModel().setSelectedIndex(getRow(getSelectionModel().getSelectedItem()));
            getFocusModel().focus(getSelectionModel().getSelectedIndex());
        }
        sortingInProgress = false;
    }

    /**
     * Calling {@code refresh()} forces the TreeTableView control to recreate and
     * repopulate the cells necessary to populate the visual bounds of the control.
     * In other words, this forces the TreeTableView to update what it is showing to
     * the user. This is useful in cases where the underlying data source has
     * changed in a way that is not observed by the TreeTableView itself.
     *
     * @since JavaFX 8u60
     */
    public void refresh() {
        getProperties().put(Properties.RECREATE, Boolean.TRUE);
    }



    /* *************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private boolean sortLock = false;
    private TableUtil.SortEventType lastSortEventType = null;
    private Object[] lastSortEventSupportInfo = null;

    private void doSort(final TableUtil.SortEventType sortEventType, final Object... supportInfo) {
        if (sortLock) {
            return;
        }

        this.lastSortEventType = sortEventType;
        this.lastSortEventSupportInfo = supportInfo;
        sort();
        this.lastSortEventType = null;
        this.lastSortEventSupportInfo = null;
    }

    private void updateExpandedItemCount(TreeItem<S> treeItem) {
        setExpandedItemCount(TreeUtil.updateExpandedItemCount(treeItem, expandedItemCountDirty, isShowRoot()));

        if (expandedItemCountDirty) {
            // this is a very inefficient thing to do, but for now having a cache
            // is better than nothing at all...
            treeItemCacheMap.clear();
        }

        expandedItemCountDirty = false;
    }

    private void updateRootExpanded() {
        // if we aren't showing the root, and the root isn't expanded, we expand
        // it now so that something is shown.
        if (!isShowRoot() && getRoot() != null && ! getRoot().isExpanded()) {
            getRoot().setExpanded(true);
        }
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



    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "tree-table-view";

    private static final PseudoClass PSEUDO_CLASS_CELL_SELECTION =
            PseudoClass.getPseudoClass("cell-selection");
    private static final PseudoClass PSEUDO_CLASS_ROW_SELECTION =
            PseudoClass.getPseudoClass("row-selection");

    private static class StyleableProperties {
        private static final CssMetaData<TreeTableView<?>,Number> FIXED_CELL_SIZE =
                new CssMetaData<TreeTableView<?>,Number>("-fx-fixed-cell-size",
                                                     SizeConverter.getInstance(),
                                                     Region.USE_COMPUTED_SIZE) {

                    @Override public Double getInitialValue(TreeTableView<?> node) {
                        return node.getFixedCellSize();
                    }

                    @Override public boolean isSettable(TreeTableView<?> n) {
                        return n.fixedCellSize == null || !n.fixedCellSize.isBound();
                    }

                    @Override public StyleableProperty<Number> getStyleableProperty(TreeTableView<?> n) {
                        return (StyleableProperty<Number>)(WritableValue<Number>) n.fixedCellSizeProperty();
                    }
                };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<CssMetaData<? extends Styleable, ?>>(Control.getClassCssMetaData());
            styleables.add(FIXED_CELL_SIZE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TreeTableViewSkin<S>(this);
    }



    /* *************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case ROW_COUNT: return getExpandedItemCount();
            case COLUMN_COUNT: return getVisibleLeafColumns().size();

            /*
             * TreeTableViewSkin returns TreeTableRows back to TreeTableView.
             * TreeTableRowSkin returns TreeTableCells back to TreeTableRow.
             */
            case SELECTED_ITEMS: {
                @SuppressWarnings("unchecked")
                ObservableList<TreeTableRow<S>> rows = (ObservableList<TreeTableRow<S>>)super.queryAccessibleAttribute(attribute, parameters);
                List<Node> selection = new ArrayList<>();
                for (TreeTableRow<S> row : rows) {
                    @SuppressWarnings("unchecked")
                    ObservableList<Node> cells = (ObservableList<Node>)row.queryAccessibleAttribute(attribute, parameters);
                    if (cells != null) selection.addAll(cells);
                }
                return FXCollections.observableArrayList(selection);
            }
            case FOCUS_ITEM: {
                Node row = (Node)super.queryAccessibleAttribute(attribute, parameters);
                if (row == null) return null;
                Node cell = (Node)row.queryAccessibleAttribute(attribute, parameters);
                /* cell equals to null means the row is a placeholder node */
                return cell != null ?  cell : row;
            }
            case CELL_AT_ROW_COLUMN: {
                @SuppressWarnings("unchecked")
                TreeTableRow<S> row = (TreeTableRow<S>)super.queryAccessibleAttribute(attribute, parameters);
                return row != null ? row.queryAccessibleAttribute(attribute, parameters) : null;
            }
            case MULTIPLE_SELECTION: {
                TreeTableViewSelectionModel<S> sm = getSelectionModel();
                return sm != null && sm.getSelectionMode() == SelectionMode.MULTIPLE;
            }
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /* *************************************************************************
     *                                                                         *
     * Support Classes                                                         *
     *                                                                         *
     **************************************************************************/

     /**
      * An immutable wrapper class for use in the TableView
     * {@link TreeTableView#columnResizePolicyProperty() column resize} functionality.
      * @since JavaFX 8.0
      */
     public static class ResizeFeatures<S> extends ResizeFeaturesBase<TreeItem<S>> {
        private TreeTableView<S> treeTable;

        /**
         * Creates an instance of this class, with the provided TreeTableView,
         * TreeTableColumn and delta values being set and stored in this immutable
         * instance.
         *
         * @param treeTable The TreeTableView upon which the resize operation is occurring.
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
            return (TreeTableColumn<S,?>) super.getColumn();
        }

        /**
         * Returns the TreeTableView upon which the resize operation is occurring.
         * @return the TreeTableView upon which the resize operation is occurring
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
     * @since JavaFX 8.0
     */
    public static class EditEvent<S> extends Event {
        private static final long serialVersionUID = -4437033058917528976L;

        /**
         * Common supertype for all edit event types.
         */
        public static final EventType<?> ANY = EDIT_ANY_EVENT;

        private final TreeTableView<S> source;
        private final S oldValue;
        private final S newValue;
        private transient final TreeItem<S> treeItem;

        /**
         * Creates a new EditEvent instance to represent an edit event. This
         * event is used for {@link #editStartEvent()},
         * {@link #editCommitEvent()} and {@link #editCancelEvent()} types.
         * @param source the source
         * @param eventType the eventType
         * @param treeItem the treeItem
         * @param oldValue the oldValue
         * @param newValue the newValue
         */
        public EditEvent(TreeTableView<S> source,
                         EventType<? extends TreeTableView.EditEvent> eventType,
                         TreeItem<S> treeItem, S oldValue, S newValue) {
            super(source, Event.NULL_SOURCE_TARGET, eventType);
            this.source = source;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.treeItem = treeItem;
        }

        /**
         * Returns the TreeTableView upon which the edit took place.
         * @return the TreeTableView upon which the edit took place
         */
        @Override public TreeTableView<S> getSource() {
            return source;
        }

        /**
         * Returns the {@link TreeItem} upon which the edit took place.
         * @return the {@link TreeItem} upon which the edit took place
         */
        public TreeItem<S> getTreeItem() {
            return treeItem;
        }

        /**
         * Returns the new value input into the TreeItem by the end user.
         * @return the new value input into the TreeItem by the end user
         */
        public S getNewValue() {
            return newValue;
        }

        /**
         * Returns the old value that existed in the TreeItem prior to the current
         * edit event.
         * @return the old value that existed in the TreeItem prior to the current
         * edit event
         */
        public S getOldValue() {
            return oldValue;
        }
    }



     /**
     * A simple extension of the {@link SelectionModel} abstract class to
     * allow for special support for TreeTableView controls.
      *
     * @since JavaFX 8.0
     */
    public static abstract class TreeTableViewSelectionModel<S> extends
            TableSelectionModel<TreeItem<S>> {

        /* *********************************************************************
         *                                                                     *
         * Private fields                                                      *
         *                                                                     *
         **********************************************************************/

        private final TreeTableView<S> treeTableView;


        /* *********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        /**
         * Builds a default TreeTableViewSelectionModel instance with the provided
         * TreeTableView.
         * @param treeTableView The TreeTableView upon which this selection model should
         *      operate.
         * @throws NullPointerException TreeTableView can not be null.
         */
        public TreeTableViewSelectionModel(final TreeTableView<S> treeTableView) {
            if (treeTableView == null) {
                throw new NullPointerException("TreeTableView can not be null");
            }

            this.treeTableView = treeTableView;
        }



        /* *********************************************************************
         *                                                                     *
         * Abstract API                                                        *
         *                                                                     *
         **********************************************************************/

         /**
         * A read-only ObservableList representing the currently selected cells
         * in this TreeTableView. Rather than directly modify this list, please
         * use the other methods provided in the TreeTableViewSelectionModel.
         * @return a list of selected cells
         */
        public abstract ObservableList<TreeTablePosition<S,?>> getSelectedCells();



        /* *********************************************************************
         *                                                                     *
         * Public API                                                          *
         *                                                                     *
         **********************************************************************/

         /**
          * Returns the TreeTableView instance that this selection model is installed in.
         * @return the TreeTableView instance that this selection model is installed in
          */
         public TreeTableView<S> getTreeTableView() {
             return treeTableView;
         }

         /** {@inheritDoc} */
         @Override public TreeItem<S> getModelItem(int index) {
             return treeTableView.getTreeItem(index);
         }

         /** {@inheritDoc} */
         @Override protected int getItemCount() {
             return treeTableView.getExpandedItemCount();
         }

         /** {@inheritDoc} */
         @Override public void focus(int row) {
             focus(row, null);
         }

         /** {@inheritDoc} */
         @Override public int getFocusedIndex() {
             return getFocusedCell().getRow();
         }

         /** {@inheritDoc} */
         @Override public void selectRange(int minRow, TableColumnBase<TreeItem<S>,?> minColumn,
                                           int maxRow, TableColumnBase<TreeItem<S>,?> maxColumn) {
             final int minColumnIndex = treeTableView.getVisibleLeafIndex((TreeTableColumn<S,?>)minColumn);
             final int maxColumnIndex = treeTableView.getVisibleLeafIndex((TreeTableColumn<S,?>)maxColumn);
             for (int _row = minRow; _row <= maxRow; _row++) {
                 for (int _col = minColumnIndex; _col <= maxColumnIndex; _col++) {
                     select(_row, treeTableView.getVisibleLeafColumn(_col));
                 }
             }
         }



        /* *********************************************************************
         *                                                                     *
         * Private implementation                                              *
         *                                                                     *
         **********************************************************************/

         private void focus(int row, TreeTableColumn<S,?> column) {
             focus(new TreeTablePosition<>(getTreeTableView(), row, column));
         }

         private void focus(TreeTablePosition<S,?> pos) {
             if (getTreeTableView().getFocusModel() == null) return;

             getTreeTableView().getFocusModel().focus(pos.getRow(), pos.getTableColumn());
         }

         private TreeTablePosition<S,?> getFocusedCell() {
             if (treeTableView.getFocusModel() == null) {
                 return new TreeTablePosition<>(treeTableView, -1, null);
             }
             return treeTableView.getFocusModel().getFocusedCell();
         }
     }



    /**
     * A primitive selection model implementation, using a List<Integer> to store all
     * selected indices.
     */
    // package for testing
    static class TreeTableViewArrayListSelectionModel<S> extends TreeTableViewSelectionModel<S> {

        private final MappingChange.Map<TreeTablePosition<S,?>,Integer> cellToIndicesMap = f -> f.getRow();

        private TreeTableView<S> treeTableView = null;

        /* *********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        public TreeTableViewArrayListSelectionModel(final TreeTableView<S> treeTableView) {
            super(treeTableView);
            this.treeTableView = treeTableView;

            this.treeTableView.rootProperty().addListener(weakRootPropertyListener);
            this.treeTableView.showRootProperty().addListener(showRootPropertyListener);
            updateTreeEventListener(null, treeTableView.getRoot());

            selectedCellsMap = new SelectedCellsMap<TreeTablePosition<S,?>>(this::fireCustomSelectedCellsListChangeEvent) {
                @Override public boolean isCellSelectionEnabled() {
                    return TreeTableViewArrayListSelectionModel.this.isCellSelectionEnabled();
                }
            };

            selectedCellsSeq = new ReadOnlyUnbackedObservableList<TreeTablePosition<S,?>>() {
                @Override public TreeTablePosition<S,?> get(int i) {
                    return selectedCellsMap.get(i);
                }

                @Override public int size() {
                    return selectedCellsMap.size();
                }
            };
//            selectedCellsSeq.addListener((ListChangeListener<? super TreeTablePosition<S,?>>) c -> {
//                ControlUtils.updateSelectedIndices(this, c);
//            });

            updateDefaultSelection();

            cellSelectionEnabledProperty().addListener(o -> {
                updateDefaultSelection();
                TableCellBehaviorBase.setAnchor(treeTableView, getFocusedCell(), true);
            });
        }

        private void dispose() {
            this.treeTableView.rootProperty().removeListener(weakRootPropertyListener);
            this.treeTableView.showRootProperty().removeListener(showRootPropertyListener);

            TreeItem<S> root = this.treeTableView.getRoot();
            if (root != null) {
                root.removeEventHandler(TreeItem.<S>expandedItemCountChangeEvent(), weakTreeItemListener);
            }
        }

        private void updateTreeEventListener(TreeItem<S> oldRoot, TreeItem<S> newRoot) {
            if (oldRoot != null && weakTreeItemListener != null) {
                oldRoot.removeEventHandler(TreeItem.<S>expandedItemCountChangeEvent(), weakTreeItemListener);
            }

            if (newRoot != null) {
                weakTreeItemListener = new WeakEventHandler<>(treeItemListener);
                newRoot.addEventHandler(TreeItem.<S>expandedItemCountChangeEvent(), weakTreeItemListener);
            }
        }

        private ChangeListener<TreeItem<S>> rootPropertyListener = (observable, oldValue, newValue) -> {
            updateDefaultSelection();

            updateTreeEventListener(oldValue, newValue);
        };

        private InvalidationListener showRootPropertyListener = o -> {
            shiftSelection(0, treeTableView.isShowRoot() ? 1 : -1, null);
        };

        private EventHandler<TreeItem.TreeModificationEvent<S>> treeItemListener = new EventHandler<>() {
            @Override public void handle(TreeItem.TreeModificationEvent<S> e) {

                if (getSelectedIndex() == -1 && getSelectedItem() == null) return;

                final TreeItem<S> treeItem = e.getTreeItem();
                if (treeItem == null) return;

                final int oldSelectedIndex = getSelectedIndex();

                treeTableView.expandedItemCountDirty = true;

                // we only shift selection from this row - everything before it
                // is safe. We might change this below based on certain criteria
                int startRow = treeTableView.getRow(treeItem);

                int shift = 0;
                ListChangeListener.Change<? extends TreeItem<?>> change = e.getChange();
                if (change != null) {
                    change.next();
                }

                do {
                    final int addedSize = change == null ? 0 : change.getAddedSize();
                    final int removedSize = change == null ? 0 : change.getRemovedSize();

                    if (e.wasExpanded()) {
                        // need to shuffle selection by the number of visible children
                        shift += treeItem.getExpandedDescendentCount(false) - 1;
                        startRow++;
                    } else if (e.wasCollapsed()) {
                        // remove selection from any child treeItem, and also determine
                        // if any child item was selected (in which case the parent
                        // takes the selection on collapse)
                        treeItem.getExpandedDescendentCount(false);
                        final int count = treeItem.previousExpandedDescendentCount;

                        final int selectedIndex = getSelectedIndex();
                        final boolean wasPrimarySelectionInChild =
                                selectedIndex >= (startRow + 1) &&
                                        selectedIndex < (startRow + count);

                        boolean wasAnyChildSelected = false;
                        final boolean isCellSelectionMode = isCellSelectionEnabled();
                        ObservableList<TreeTableColumn<S, ?>> columns = getTreeTableView().getVisibleLeafColumns();

                        selectedIndices._beginChange();
                        final int from = startRow + 1;
                        final int to = startRow + count;
                        final List<Integer> removed = new ArrayList<>();
                        TreeTableColumn<S, ?> selectedColumn = null;
                        for (int i = from; i < to; i++) {
                            // we have to handle cell selection mode differently than
                            // row selection mode. Refer to RT-34103 for the bug report
                            // that drove this change, but in short the issue was that
                            // when collapsing a branch that had selection, we were
                            // always calling isSelected(row), but that always returns
                            // false in cell selection mode.
                            if (isCellSelectionMode) {
                                for (int column = 0; column < columns.size(); column++) {
                                    final TreeTableColumn<S, ?> col = columns.get(column);
                                    if (isSelected(i, col)) {
                                        wasAnyChildSelected = true;
                                        clearSelection(i, col);
                                        selectedColumn = col;
                                    }
                                }
                            } else {
                                if (isSelected(i)) {
                                    wasAnyChildSelected = true;
                                    removed.add(i);
                                }
                            }
                        }

                        if (!removed.isEmpty()) {
                            selectedIndices._nextRemove(selectedIndices.indexOf(removed.get(0)), removed);
                        }

                        for (int index : removed) {
                            startAtomic();
                            // we pass in false here to prevent a lookup into the TreeItem, as it is unnecessary
                            // and results in JDK-8152396
                            clearSelection(new TreeTablePosition<>(treeTableView, index, null, false));
                            stopAtomic();
                        }
                        selectedIndices._endChange();

                        // put selection onto the newly-collapsed tree item
                        if (wasPrimarySelectionInChild && wasAnyChildSelected) {
                            select(startRow, selectedColumn);
                        }

                        shift += -count + 1;
                        startRow++;
                    } else if (e.wasPermutated()) {
                        // Approach:
                        // Get the current selection.
                        // Create a new selection with updated index(row).
                        // Update the current selection with new selection.
                        // If sorting is in progress then one Selection change event will be sent from
                        // TreeTableView.sort() method, and should not be sent from here.
                        // else, in case otherwise, the selection change events would be generated.
                        // Do not call shiftSelection() in case of permutation change(when shift == 0).

                        List<TreeTablePosition<S, ?>> currentSelection = new ArrayList<>(selectedCellsMap.getSelectedCells());
                        List<TreeTablePosition<S, ?>> updatedSelection = new ArrayList<>();

                        boolean selectionIndicesChanged = false;
                        for (TreeTablePosition<S, ?> selectedCell : currentSelection) {
                            int newRow = treeTableView.getRow(selectedCell.getTreeItem());
                            if (selectedCell.getRow() != newRow) {
                                selectionIndicesChanged = true;
                            }
                            updatedSelection.add(new TreeTablePosition<>(selectedCell, newRow));
                        }
                        if (selectionIndicesChanged) {
                            if (treeTableView.isSortingInProgress()) {
                                startAtomic();
                                selectedCellsMap.setAll(updatedSelection);
                                stopAtomic();
                            } else {
                                startAtomic();
                                quietClearSelection();
                                stopAtomic();
                                selectedCellsMap.setAll(updatedSelection);
                                int selectedIndex = treeTableView.getRow(getSelectedItem());
                                setSelectedIndex(selectedIndex);
                                focus(selectedIndex);
                            }
                        }
                    } else if (e.wasAdded()) {
                        // shuffle selection by the number of added items
                        shift += treeItem.isExpanded() ? addedSize : 0;

                        // RT-32963: We were taking the startRow from the TreeItem
                        // in which the children were added, rather than from the
                        // actual position of the new child. This led to selection
                        // being moved off the parent TreeItem by mistake.
                        // The 'if (e.getAddedSize() == 1)' condition here was
                        // subsequently commented out due to RT-33894.
                        startRow = treeTableView.getRow(e.getChange().getAddedSubList().get(0));

                        TreeTablePosition<S, ?> anchor = TreeTableCellBehavior.getAnchor(treeTableView, null);
                        if (anchor != null) {
                            boolean isAnchorSelected = isSelected(anchor.getRow(), anchor.getTableColumn());
                            if (isAnchorSelected) {
                                TreeTablePosition<S, ?> newAnchor = new TreeTablePosition<>(treeTableView, anchor.getRow() + shift, anchor.getTableColumn());
                                TreeTableCellBehavior.setAnchor(treeTableView, newAnchor, false);
                            }
                        }
                    } else if (e.wasRemoved()) {
                        // shuffle selection by the number of removed items
                        shift += treeItem.isExpanded() ? -removedSize : 0;

                        // the start row is incorrect - it is _not_ the index of the
                        // TreeItem in which the children were removed from (which is
                        // what it currently represents). We need to take the 'from'
                        // value out of the event and make use of that to understand
                        // what actually changed inside the children list.
                        startRow += e.getFrom() + 1;

                        // whilst we are here, we should check if the removed items
                        // are part of the selectedItems list - and remove them
                        // from selection if they are (as per RT-15446)
                        final List<Integer> selectedIndices = getSelectedIndices();
                        final List<TreeItem<S>> selectedItems = getSelectedItems();
                        final TreeItem<S> selectedItem = getSelectedItem();
                        final List<? extends TreeItem<S>> removedChildren = e.getChange().getRemoved();

                        for (int i = 0; i < selectedIndices.size() && !selectedItems.isEmpty(); i++) {
                            int index = selectedIndices.get(i);
                            if (index > selectedItems.size()) break;

                            if (removedChildren.size() == 1 &&
                                    selectedItems.size() == 1 &&
                                    selectedItem != null &&
                                    selectedItem.equals(removedChildren.get(0))) {
                                // Bug fix for RT-28637
                                if (oldSelectedIndex < getItemCount()) {
                                    final int previousRow = oldSelectedIndex == 0 ? 0 : oldSelectedIndex - 1;
                                    TreeItem<S> newSelectedItem = getModelItem(previousRow);
                                    if (!selectedItem.equals(newSelectedItem)) {
                                        clearAndSelect(previousRow);
                                    }
                                }
                            }
                        }
                    }
                } while (e.getChange() != null && e.getChange().next());

                if (shift != 0) {
                    shiftSelection(startRow, shift, new Callback<ShiftParams, Void>() {
                        @Override public Void call(ShiftParams param) {

                            // we make the shifts atomic, as otherwise listeners to
                            // the items / indices lists get a lot of intermediate
                            // noise. They eventually get the summary event fired
                            // from within shiftSelection, so this is ok.
                            startAtomic();

                            final int clearIndex = param.getClearIndex();
                            final int setIndex = param.getSetIndex();
                            TreeTablePosition<S,?> oldTP = null;
                            if (clearIndex > -1) {
                                for (int i = 0; i < selectedCellsMap.size(); i++) {
                                    TreeTablePosition<S,?> tp = selectedCellsMap.get(i);
                                    if (tp.getRow() == clearIndex) {
                                        oldTP = tp;
                                        selectedCellsMap.remove(tp);
                                    } else if (tp.getRow() == setIndex && !param.isSelected()) {
                                        selectedCellsMap.remove(tp);
                                    }
                                }
                            }

                            if (oldTP != null && param.isSelected()) {
                                TreeTablePosition<S,?> newTP = new TreeTablePosition<>(
                                        treeTableView, param.getSetIndex(), oldTP.getTableColumn());

                                selectedCellsMap.add(newTP);
                            }

                            stopAtomic();

                            return null;
                        }
                    });
                }
            }
        };

        private WeakChangeListener<TreeItem<S>> weakRootPropertyListener =
                new WeakChangeListener<>(rootPropertyListener);

        private WeakEventHandler<TreeItem.TreeModificationEvent<S>> weakTreeItemListener;



        /* *********************************************************************
         *                                                                     *
         * Observable properties (and getters/setters)                         *
         *                                                                     *
         **********************************************************************/

        // the only 'proper' internal data structure, selectedItems and selectedIndices
        // are both 'read-only and unbacked'.
        private final SelectedCellsMap<TreeTablePosition<S,?>> selectedCellsMap;

        private final ReadOnlyUnbackedObservableList<TreeTablePosition<S,?>> selectedCellsSeq;
        @Override public ObservableList<TreeTablePosition<S,?>> getSelectedCells() {
            return selectedCellsSeq;
        }


        /* *********************************************************************
         *                                                                     *
         * Internal properties                                                 *
         *                                                                     *
         **********************************************************************/



        /* *********************************************************************
         *                                                                     *
         * Public selection API                                                *
         *                                                                     *
         **********************************************************************/

        @Override public void clearAndSelect(int row) {
            clearAndSelect(row, null);
        }

        @Override public void clearAndSelect(int row, TableColumnBase<TreeItem<S>,?> column) {
            if (row < 0 || row >= getItemCount()) return;

            final TreeTablePosition<S,?> newTablePosition = new TreeTablePosition<>(getTreeTableView(), row, (TreeTableColumn<S,?>)column);
            final boolean isCellSelectionEnabled = isCellSelectionEnabled();

            // replace the anchor
            TreeTableCellBehavior.setAnchor(treeTableView, newTablePosition, false);

            // firstly we make a copy of the selection, so that we can send out
            // the correct details in the selection change event.
            List<TreeTablePosition<S,?>> previousSelection = new ArrayList<>(selectedCellsMap.getSelectedCells());

            // secondly we check if we can short-circuit out of here because the new selection
            // equals the current selection
            final boolean wasSelected = isSelected(row, column);
            if (wasSelected && previousSelection.size() == 1) {
                // before we return, we double-check that the selected item
                // is equal to the item in the given index
                TreeTablePosition<S,?> selectedCell = getSelectedCells().get(0);
                if (getSelectedItem() == getModelItem(row)) {
                    if (selectedCell.getRow() == row && selectedCell.getTableColumn() == column) {
                        return;
                    }
                }
            }

            // RT-32411: We used to call quietClearSelection() here, but this
            // resulted in the selectedItems and selectedIndices lists never
            // reporting that they were empty.
            // makeAtomic toggle added to resolve RT-32618
            startAtomic();

            // then clear the current selection
            clearSelection();

            // and select the new cell
            select(row, column);

            stopAtomic();

            // We remove the new selection from the list seeing as it is not removed.
            if (isCellSelectionEnabled) {
                previousSelection.remove(newTablePosition);
            } else {
                for (TreeTablePosition<S,?> tp : previousSelection) {
                    if (tp.getRow() == row) {
                        previousSelection.remove(tp);
                        break;
                    }
                }
            }

            // fire off a single add/remove/replace notification (rather than
            // individual remove and add notifications) - see RT-33324
            ListChangeListener.Change<TreeTablePosition<S, ?>> change;

            /*
             * getFrom() documentation:
             *   If wasAdded is true, the interval contains all the values that were added.
             *   If wasPermutated is true, the interval marks the values that were permutated.
             *   If wasRemoved is true and wasAdded is false, getFrom() and getTo() should
             *   return the same number - the place where the removed elements were positioned in the list.
             */
            if (wasSelected) {
                change = ControlUtils.buildClearAndSelectChange(
                        selectedCellsSeq, previousSelection, newTablePosition, Comparator.comparing(TreeTablePosition::getRow));
            } else {
                final int changeIndex = isCellSelectionEnabled ? 0 : Math.max(0, selectedCellsSeq.indexOf(newTablePosition));
                final int changeSize = isCellSelectionEnabled ? getSelectedCells().size() : 1;
                change = new NonIterableChange.GenericAddRemoveChange<>(
                        changeIndex, changeIndex + changeSize, previousSelection, selectedCellsSeq);
//                selectedCellsSeq._beginChange();
//                selectedCellsSeq._nextAdd(changeIndex, changeIndex + changeSize);
//                selectedCellsSeq._nextRemove(changeIndex, previousSelection);
//                selectedCellsSeq._endChange();
            }

            fireCustomSelectedCellsListChangeEvent(change);
        }

        @Override public void select(int row) {
            select(row, null);
        }

        @Override public void select(int row, TableColumnBase<TreeItem<S>,?> column) {
            // TODO we need to bring in the TreeView selection stuff here...
            if (row < 0 || row >= getRowCount()) return;

            // if I'm in cell selection mode but the column is null, select each
            // of the contained cells individually
            if (isCellSelectionEnabled() && column == null) {
                List<TreeTableColumn<S,?>> columns = getTreeTableView().getVisibleLeafColumns();
                for (int i = 0; i < columns.size(); i++) {
                    select(row, columns.get(i));
                }
                return;
            }

            if (TableCellBehavior.hasDefaultAnchor(treeTableView)) {
                TableCellBehavior.removeAnchor(treeTableView);
            }

            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();
            }
            selectedCellsMap.add(new TreeTablePosition<>(getTreeTableView(), row, (TreeTableColumn<S,?>)column));

            updateSelectedIndex(row);
            focus(row, (TreeTableColumn<S, ?>) column);
        }

        @Override public void select(TreeItem<S> obj) {
            if (obj == null && getSelectionMode() == SelectionMode.SINGLE) {
                clearSelection();
                return;
            }

            int firstIndex = treeTableView.getRow(obj);
            if (firstIndex > -1) {
                if (isSelected(firstIndex)) {
                    return;
                }

                if (getSelectionMode() == SelectionMode.SINGLE) {
                    quietClearSelection();
                }

                select(firstIndex);
            } else {
                // if we are here, we did not find the item in the entire data model.
                // Even still, we allow for this item to be set to the give object.
                // We expect that in concrete subclasses of this class we observe the
                // data model such that we check to see if the given item exists in it,
                // whilst SelectedIndex == -1 && SelectedItem != null.
                setSelectedIndex(-1);
                setSelectedItem(obj);
            }
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

                if (selectedCellsMap.isEmpty()) {
                    if (row > 0 && row < rowCount) {
                        select(row);
                    }
                }
            } else {
                int lastIndex = -1;
                Set<TreeTablePosition<S,?>> positions = new LinkedHashSet<>();

                // --- firstly, we special-case the non-varargs 'row' argument
                if (row >= 0 && row < rowCount) {
                    // if I'm in cell selection mode, we want to select each
                    // of the contained cells individually
                    if (isCellSelectionEnabled()) {
                        List<TreeTableColumn<S,?>> columns = getTreeTableView().getVisibleLeafColumns();
                        for (int column = 0; column < columns.size(); column++) {
                            if (! selectedCellsMap.isSelected(row, column)) {
                                positions.add(new TreeTablePosition<>(getTreeTableView(), row, columns.get(column)));
                            }
                        }
                    } else {
                        boolean match = selectedCellsMap.isSelected(row, -1);
                        if (!match) {
                            positions.add(new TreeTablePosition<>(getTreeTableView(), row, null));
                        }
                    }

                    lastIndex = row;
                }

                // --- now we iterate through all varargs values
                for (int i = 0; i < rows.length; i++) {
                    int index = rows[i];
                    if (index < 0 || index >= rowCount) continue;
                    lastIndex = index;

                    if (isCellSelectionEnabled()) {
                        List<TreeTableColumn<S,?>> columns = getTreeTableView().getVisibleLeafColumns();
                        for (int column = 0; column < columns.size(); column++) {
                            if (! selectedCellsMap.isSelected(index, column)) {
                                positions.add(new TreeTablePosition<>(getTreeTableView(), index, columns.get(column)));
                                lastIndex = index;
                            }
                        }
                    } else {
                        if (! selectedCellsMap.isSelected(index, -1)) {
                            // if we are here then we have successfully gotten through the for-loop above
                            positions.add(new TreeTablePosition<>(getTreeTableView(), index, null));
                        }
                    }
                }

                selectedCellsMap.addAll(positions);

                if (lastIndex != -1) {
                    select(lastIndex);
                }
            }
        }

        @Override public void selectAll() {
            if (getSelectionMode() == SelectionMode.SINGLE) return;

            if (isCellSelectionEnabled()) {
                List<TreeTablePosition<S,?>> indices = new ArrayList<>();
                TreeTableColumn<S,?> column;
                TreeTablePosition<S,?> tp = null;
                for (int col = 0; col < getTreeTableView().getVisibleLeafColumns().size(); col++) {
                    column = getTreeTableView().getVisibleLeafColumns().get(col);
                    for (int row = 0; row < getRowCount(); row++) {
                        tp = new TreeTablePosition<>(getTreeTableView(), row, column);
                        indices.add(tp);
                    }
                }
                selectedCellsMap.setAll(indices);

                if (tp != null) {
                    select(tp.getRow(), tp.getTableColumn());
                    focus(tp.getRow(), tp.getTableColumn());
                }
            } else {
                List<TreeTablePosition<S,?>> indices = new ArrayList<>();
                for (int i = 0; i < getRowCount(); i++) {
                    indices.add(new TreeTablePosition<>(getTreeTableView(), i, null));
                }
                selectedCellsMap.setAll(indices);

                int focusedIndex = getFocusedIndex();
                if (focusedIndex == -1) {
                    final int itemCount = getItemCount();
                    if (itemCount > 0) {
                        select(itemCount - 1);
                        focus(indices.get(indices.size() - 1));
                    }
                } else {
                    select(focusedIndex);
                    focus(focusedIndex);
                }
            }
        }

        @Override public void selectRange(int minRow, TableColumnBase<TreeItem<S>,?> minColumn,
                                          int maxRow, TableColumnBase<TreeItem<S>,?> maxColumn) {
            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();
                select(maxRow, maxColumn);
                return;
            }

            startAtomic();

            final int itemCount = getItemCount();
            final boolean isCellSelectionEnabled = isCellSelectionEnabled();

            final int minColumnIndex = treeTableView.getVisibleLeafIndex((TreeTableColumn<S,?>)minColumn);
            final int maxColumnIndex = treeTableView.getVisibleLeafIndex((TreeTableColumn<S,?>)maxColumn);
            final int _minColumnIndex = Math.min(minColumnIndex, maxColumnIndex);
            final int _maxColumnIndex = Math.max(minColumnIndex, maxColumnIndex);

            final int _minRow = Math.min(minRow, maxRow);
            final int _maxRow = Math.max(minRow, maxRow);

            List<TreeTablePosition<S,?>> cellsToSelect = new ArrayList<>();

            for (int _row = _minRow; _row <= _maxRow; _row++) {
                // begin copy/paste of select(int, column) method (with some
                // slight modifications)
                if (_row < 0 || _row >= itemCount) continue;

                if (! isCellSelectionEnabled) {
                    cellsToSelect.add(new TreeTablePosition<>(treeTableView, _row, (TreeTableColumn<S,?>)minColumn));
                } else {
                    for (int _col = _minColumnIndex; _col <= _maxColumnIndex; _col++) {
                        final TreeTableColumn<S, ?> column = treeTableView.getVisibleLeafColumn(_col);

                        // if I'm in cell selection mode but the column is null, I don't want
                        // to select the whole row instead...
                        if (column == null && isCellSelectionEnabled) continue;

                        cellsToSelect.add(new TreeTablePosition<>(treeTableView, _row, column));
                        // end copy/paste
                    }
                }
            }

            // to prevent duplication we remove all currently selected cells from
            // our list of cells to select.
            cellsToSelect.removeAll(getSelectedCells());

            selectedCellsMap.addAll(cellsToSelect);
            stopAtomic();

            // fire off events
            // Note that focus and selection always goes to maxRow, not _maxRow.
            updateSelectedIndex(maxRow);
            focus(maxRow, (TreeTableColumn<S,?>)maxColumn);

            final TreeTableColumn<S,?> startColumn = (TreeTableColumn<S,?>)minColumn;
            final TreeTableColumn<S,?> endColumn = isCellSelectionEnabled ? (TreeTableColumn<S,?>)maxColumn : startColumn;
            final int startChangeIndex = selectedCellsMap.indexOf(new TreeTablePosition<>(treeTableView, minRow, startColumn));
            final int endChangeIndex = selectedCellsMap.indexOf(new TreeTablePosition<>(treeTableView, maxRow, endColumn));

            if (startChangeIndex > -1 && endChangeIndex > -1) {
                final int startIndex = Math.min(startChangeIndex, endChangeIndex);
                final int endIndex = Math.max(startChangeIndex, endChangeIndex);

                ListChangeListener.Change c = new NonIterableChange.SimpleAddChange<>(startIndex, endIndex + 1, selectedCellsSeq);
                fireCustomSelectedCellsListChangeEvent(c);
//                selectedCellsSeq.fireChange(() -> selectedCellsSeq._nextAdd(startIndex, endIndex + 1));
            }
        }

        @Override public void clearSelection(int index) {
            clearSelection(index, null);
        }

        @Override
        public void clearSelection(int row, TableColumnBase<TreeItem<S>,?> column) {
            clearSelection(new TreeTablePosition<S,Object>(getTreeTableView(), row, (TreeTableColumn)column));
        }

        private void clearSelection(TreeTablePosition<S,?> tp) {
            final boolean csMode = isCellSelectionEnabled();
            final int row = tp.getRow();
            final boolean columnIsNull = tp.getTableColumn() == null;

            List<TreeTablePosition> toRemove = new ArrayList<>();
            for (TreeTablePosition pos : getSelectedCells()) {
                if (!csMode) {
                    if (pos.getRow() == row) {
                        toRemove.add(pos);
                        break;
                    }
                } else {
                    if (columnIsNull && pos.getRow() == row) {
                        // if we are in cell selection mode and the column is null,
                        // we remove all items in the row
                        toRemove.add(pos);
                    } else if (pos.equals(tp)) {
                        toRemove.add(tp);
                        break;
                    }
                }
            }
            toRemove.stream().forEach(selectedCellsMap::remove);

            if (isEmpty() && ! isAtomic()) {
                updateSelectedIndex(-1);
                selectedCellsMap.clear();
            }
        }

        @Override public void clearSelection() {
            final List<TreeTablePosition<S,?>> removed = new ArrayList<>((Collection)getSelectedCells());

            quietClearSelection();

            if (! isAtomic()) {
                updateSelectedIndex(-1);
                focus(-1);

                if (!removed.isEmpty()) {
//                    selectedCellsSeq.fireChange(() -> selectedCellsSeq._nextRemove(0, removed));
                    ListChangeListener.Change<TreeTablePosition<S, ?>> c = new NonIterableChange<TreeTablePosition<S, ?>>(0, 0, selectedCellsSeq) {
                        @Override public List<TreeTablePosition<S, ?>> getRemoved() {
                            return removed;
                        }
                    };
                    fireCustomSelectedCellsListChangeEvent(c);
                }
            }
        }

        private void quietClearSelection() {
            startAtomic();
            selectedCellsMap.clear();
            stopAtomic();
        }

        @Override public boolean isSelected(int index) {
            return isSelected(index, null);
        }

        @Override public boolean isSelected(int row, TableColumnBase<TreeItem<S>,?> column) {
            // When in cell selection mode, if the column is null, then we interpret
            // the users query to be asking if _all_ of the cells in the row are selected,
            // rather than if _any_ of the cells in the row are selected.
            final boolean isCellSelectionEnabled = isCellSelectionEnabled();
            if (isCellSelectionEnabled && column == null) {
                int columnCount = treeTableView.getVisibleLeafColumns().size();
                for (int col = 0; col < columnCount; col++) {
                    if (!selectedCellsMap.isSelected(row, col)) {
                        return false;
                    }
                }
                return true;
            } else {
                int columnIndex = !isCellSelectionEnabled || column == null ? -1 : treeTableView.getVisibleLeafIndex((TreeTableColumn<S, ?>) column);
                return selectedCellsMap.isSelected(row, columnIndex);
            }
        }

        @Override public boolean isEmpty() {
            return selectedCellsMap.isEmpty();
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
            TreeTablePosition<S,?> pos = getFocusedCell();
            if (pos.getRow() == -1) {
                select(getRowCount() - 1);
            } else if (pos.getRow() > 0) {
                select(pos.getRow() - 1, pos.getTableColumn());
            }
        }

        @Override public void selectBelowCell() {
            TreeTablePosition<S,?> pos = getFocusedCell();

            if (pos.getRow() == -1) {
                select(0);
            } else if (pos.getRow() < getRowCount() -1) {
                select(pos.getRow() + 1, pos.getTableColumn());
            }
        }

        @Override public void selectFirst() {
            TreeTablePosition<S,?> focusedCell = getFocusedCell();

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
            TreeTablePosition<S,?> focusedCell = getFocusedCell();

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

            TreeTablePosition<S,?> pos = getFocusedCell();
            if (pos.getColumn() - 1 >= 0) {
                select(pos.getRow(), getTableColumn(pos.getTableColumn(), -1));
            }
        }

        @Override public void selectRightCell() {
            if (! isCellSelectionEnabled()) return;

            TreeTablePosition<S,?> pos = getFocusedCell();
            if (pos.getColumn() + 1 < getTreeTableView().getVisibleLeafColumns().size()) {
                select(pos.getRow(), getTableColumn(pos.getTableColumn(), 1));
            }
        }



        /* *********************************************************************
         *                                                                     *
         * Support code                                                        *
         *                                                                     *
         **********************************************************************/

        private void updateDefaultSelection() {
            // when the items list totally changes, we should clear out
            // the selection
            int newSelectionIndex = -1;
            TreeItem<S> selectedItem = getSelectedItem();
            if (selectedItem != null) {
                newSelectionIndex = treeTableView.getRow(selectedItem);
            }

            // we put focus onto the first item, if there is at least
            // one item in the list
            int newFocusIndex = newSelectionIndex != -1 ? newSelectionIndex : treeTableView.getExpandedItemCount() > 0 ? 0 : -1;

            clearSelection();
            select(newSelectionIndex, isCellSelectionEnabled() ? getTableColumn(0) : null);
            focus(newFocusIndex, isCellSelectionEnabled() ? getTableColumn(0) : null);
        }

        private TreeTableColumn<S,?> getTableColumn(int pos) {
            return getTreeTableView().getVisibleLeafColumn(pos);
        }

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
            focus(new TreeTablePosition<>(getTreeTableView(), row, column));
        }

        private void focus(TreeTablePosition<S,?> pos) {
            if (getTreeTableView().getFocusModel() == null) return;

            getTreeTableView().getFocusModel().focus(pos.getRow(), pos.getTableColumn());
            getTreeTableView().notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM);
        }

        @Override public int getFocusedIndex() {
            return getFocusedCell().getRow();
        }

        private TreeTablePosition<S,?> getFocusedCell() {
            if (treeTableView.getFocusModel() == null) {
                return new TreeTablePosition<>(treeTableView, -1, null);
            }
            return treeTableView.getFocusModel().getFocusedCell();
        }

        private int getRowCount() {
            return treeTableView.getExpandedItemCount();
        }

        private void fireCustomSelectedCellsListChangeEvent(ListChangeListener.Change<? extends TreeTablePosition<S,?>> c) {
            ControlUtils.updateSelectedIndices(this, c);

            if (isAtomic()) {
                return;
            }

            selectedCellsSeq.callObservers(new MappingChange<>(c, MappingChange.NOOP_MAP, selectedCellsSeq));
        }
    }




    /**
     * A {@link FocusModel} with additional functionality to support the requirements
     * of a TableView control.
     *
     * @see TableView
     * @since JavaFX 8.0
     */
    public static class TreeTableViewFocusModel<S> extends TableFocusModel<TreeItem<S>, TreeTableColumn<S,?>> {

        private final TreeTableView<S> treeTableView;

        private final TreeTablePosition EMPTY_CELL;

        /**
         * Creates a default TableViewFocusModel instance that will be used to
         * manage focus of the provided TableView control.
         *
         * @param treeTableView The tableView upon which this focus model operates.
         * @throws NullPointerException The TableView argument can not be null.
         */
        public TreeTableViewFocusModel(final TreeTableView<S> treeTableView) {
            if (treeTableView == null) {
                throw new NullPointerException("TableView can not be null");
            }

            this.treeTableView = treeTableView;
            this.EMPTY_CELL = new TreeTablePosition<>(treeTableView, -1, null);

            this.treeTableView.rootProperty().addListener(weakRootPropertyListener);
            updateTreeEventListener(null, treeTableView.getRoot());

            int focusRow = getItemCount() > 0 ? 0 : -1;
            TreeTablePosition<S,?> pos = new TreeTablePosition<>(treeTableView, focusRow, null);
            setFocusedCell(pos);

            showRootListener = obs -> {
                if (isFocused(0)) {
                    focus(-1);
                    focus(0);
                }
            };
            treeTableView.showRootProperty().addListener(new WeakInvalidationListener(showRootListener));

            focusedCellProperty().addListener(o -> {
                treeTableView.notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM);
            });
        }

        private final ChangeListener<TreeItem<S>> rootPropertyListener = (observable, oldValue, newValue) -> {
            updateTreeEventListener(oldValue, newValue);
        };

        private final WeakChangeListener<TreeItem<S>> weakRootPropertyListener =
                new WeakChangeListener<>(rootPropertyListener);

        private final InvalidationListener showRootListener;

        private void updateTreeEventListener(TreeItem<S> oldRoot, TreeItem<S> newRoot) {
            if (oldRoot != null && weakTreeItemListener != null) {
                oldRoot.removeEventHandler(TreeItem.<S>expandedItemCountChangeEvent(), weakTreeItemListener);
            }

            if (newRoot != null) {
                weakTreeItemListener = new WeakEventHandler<>(treeItemListener);
                newRoot.addEventHandler(TreeItem.<S>expandedItemCountChangeEvent(), weakTreeItemListener);
            }
        }

        private EventHandler<TreeItem.TreeModificationEvent<S>> treeItemListener = new EventHandler<TreeItem.TreeModificationEvent<S>>() {
            @Override public void handle(TreeItem.TreeModificationEvent<S> e) {
                // don't shift focus if the event occurred on a tree item after
                // the focused row, or if there is no focus index at present
                if (getFocusedIndex() == -1) return;

                int shift = 0;
                if (e.getChange() != null) {
                    e.getChange().next();
                }

                do {
                    int row = treeTableView.getRow(e.getTreeItem());

                    if (e.wasExpanded()) {
                        if (row < getFocusedIndex()) {
                            // need to shuffle selection by the number of visible children
                            shift += e.getTreeItem().getExpandedDescendentCount(false) - 1;
                        }
                    } else if (e.wasCollapsed()) {
                        if (row < getFocusedIndex()) {
                            // need to shuffle selection by the number of visible children
                            // that were just hidden
                            shift += -e.getTreeItem().previousExpandedDescendentCount + 1;
                        }
                    } else if (e.wasAdded()) {
                        // get the TreeItem the event occurred on - we only need to
                        // shift if the tree item is expanded
                        TreeItem<S> eventTreeItem = e.getTreeItem();
                        if (eventTreeItem.isExpanded()) {
                            for (int i = 0; i < e.getAddedChildren().size(); i++) {
                                // get the added item and determine the row it is in
                                TreeItem<S> item = e.getAddedChildren().get(i);
                                row = treeTableView.getRow(item);

                                if (item != null && row <= (shift+getFocusedIndex())) {
                                    shift += item.getExpandedDescendentCount(false);
                                }
                            }
                        }
                    } else if (e.wasRemoved()) {
                        row += e.getFrom() + 1;

                        for (int i = 0; i < e.getRemovedChildren().size(); i++) {
                            TreeItem<S> item = e.getRemovedChildren().get(i);
                            if (item != null && item.equals(getFocusedItem())) {
                                focus(Math.max(0, getFocusedIndex() - 1));
                                return;
                            }
                        }

                        if (row <= getFocusedIndex()) {
                            // shuffle selection by the number of removed items
                            shift += e.getTreeItem().isExpanded() ? -e.getRemovedSize() : 0;
                        }
                    }
                } while (e.getChange() != null && e.getChange().next());

                if (shift != 0) {
                    TreeTablePosition<S, ?> focusedCell = getFocusedCell();
                    final int newFocus = focusedCell.getRow() + shift;
                    if (newFocus >= 0) {
                        Platform.runLater(() -> focus(newFocus, focusedCell.getTableColumn()));
                    }
                }
            }
        };

        private WeakEventHandler<TreeItem.TreeModificationEvent<S>> weakTreeItemListener;

        /** {@inheritDoc} */
        @Override protected int getItemCount() {
//            if (tableView.getItems() == null) return -1;
//            return tableView.getItems().size();
            return treeTableView.getExpandedItemCount();
        }

        /** {@inheritDoc} */
        @Override protected TreeItem<S> getModelItem(int index) {
            if (index < 0 || index >= getItemCount()) return null;
            return treeTableView.getTreeItem(index);
        }

        /**
         * The position of the current item in the TableView which has the focus.
         */
        private ReadOnlyObjectWrapper<TreeTablePosition<S,?>> focusedCell;
        public final ReadOnlyObjectProperty<TreeTablePosition<S,?>> focusedCellProperty() {
            return focusedCellPropertyImpl().getReadOnlyProperty();
        }
        private void setFocusedCell(TreeTablePosition<S,?> value) { focusedCellPropertyImpl().set(value);  }
        public final TreeTablePosition<S,?> getFocusedCell() { return focusedCell == null ? EMPTY_CELL : focusedCell.get(); }

        private ReadOnlyObjectWrapper<TreeTablePosition<S,?>> focusedCellPropertyImpl() {
            if (focusedCell == null) {
                focusedCell = new ReadOnlyObjectWrapper<TreeTablePosition<S,?>>(EMPTY_CELL) {
                    private TreeTablePosition<S,?> old;
                    @Override protected void invalidated() {
                        if (get() == null) return;

                        if (old == null || !old.equals(get())) {
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
                TreeTablePosition<S,?> oldFocusCell = getFocusedCell();
                TreeTablePosition<S,?> newFocusCell = new TreeTablePosition<>(treeTableView, row, column);
                setFocusedCell(newFocusCell);

                if (newFocusCell.equals(oldFocusCell)) {
                    // manually update the focus properties to ensure consistency
                    setFocusedIndex(row);
                    setFocusedItem(getModelItem(row));
                }
            }
        }

        /**
         * Convenience method for setting focus on a particular row or cell
         * using a {@link TablePosition}.
         *
         * @param pos The table position where focus should be set.
         */
        public void focus(TreeTablePosition<S,?> pos) {
            if (pos == null) return;
            focus(pos.getRow(), pos.getTableColumn());
        }


        /* *********************************************************************
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

            TreeTablePosition<S,?> cell = getFocusedCell();
            boolean columnMatch = column == null || column.equals(cell.getTableColumn());

            return cell.getRow() == row && columnMatch;
        }

        /**
         * Causes the item at the given index to receive the focus. This does not
         * cause the current selection to change. Updates the focusedItem and
         * focusedIndex properties such that <code>focusedIndex = -1</code> unless
         * <pre><code>0 &lt;= index &lt; model size</code></pre>.
         *
         * @param index The index of the item to get focus.
         */
        @Override public void focus(int index) {
            if (treeTableView.expandedItemCountDirty) {
                treeTableView.updateExpandedItemCount(treeTableView.getRoot());
            }

            if (index < 0 || index >= getItemCount()) {
                setFocusedCell(EMPTY_CELL);
            } else {
                setFocusedCell(new TreeTablePosition<>(treeTableView, index, null));
            }
        }

        /**
         * Attempts to move focus to the cell above the currently focused cell.
         */
        @Override public void focusAboveCell() {
            TreeTablePosition<S,?> cell = getFocusedCell();

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
            TreeTablePosition<S,?> cell = getFocusedCell();
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
            TreeTablePosition<S,?> cell = getFocusedCell();
            if (cell.getColumn() <= 0) return;
            focus(cell.getRow(), getTableColumn(cell.getTableColumn(), -1));
        }

        /**
         * Attempts to move focus to the cell to the right of the the currently focused cell.
         */
        @Override public void focusRightCell() {
            TreeTablePosition<S,?> cell = getFocusedCell();
            if (cell.getColumn() == getColumnCount() - 1) return;
            focus(cell.getRow(), getTableColumn(cell.getTableColumn(), 1));
        }

        /** {@inheritDoc} */
        @Override public void focusPrevious() {
            if (getFocusedIndex() == -1) {
                focus(0);
            } else if (getFocusedIndex() > 0) {
                focusAboveCell();
            }
        }

        /** {@inheritDoc} */
        @Override public void focusNext() {
            if (getFocusedIndex() == -1) {
                focus(0);
            } else if (getFocusedIndex() != getItemCount() -1) {
                focusBelowCell();
            }
        }



         /* *********************************************************************
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
