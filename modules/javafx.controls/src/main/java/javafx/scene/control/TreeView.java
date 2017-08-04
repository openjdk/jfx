/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.converter.SizeConverter;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.behavior.TreeCellBehavior;
import javafx.scene.control.skin.TreeViewSkin;

import javafx.application.Platform;
import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
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
import javafx.collections.ListChangeListener;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.event.WeakEventHandler;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The TreeView control provides a view on to a tree root (of type
 * {@link TreeItem}). By using a TreeView, it is possible to drill down into the
 * children of a TreeItem, recursively until a TreeItem has no children (that is,
 * it is a <i>leaf</i> node in the tree). To facilitate this, unlike controls
 * like {@link ListView}, in TreeView it is necessary to <strong>only</strong>
 * specify the {@link #rootProperty() root} node.
 *
 * <p>
 * For more information on building up a tree using this approach, refer to the
 * {@link TreeItem} class documentation. Briefly however, to create a TreeView,
 * you should do something along the lines of the following:
 * <pre><code>
 * TreeItem&lt;String&gt; root = new TreeItem&lt;String&gt;("Root Node");
 * root.setExpanded(true);
 * root.getChildren().addAll(
 *     new TreeItem&lt;String&gt;("Item 1"),
 *     new TreeItem&lt;String&gt;("Item 2"),
 *     new TreeItem&lt;String&gt;("Item 3")
 * );
 * TreeView&lt;String&gt; treeView = new TreeView&lt;String&gt;(root);
 * </code></pre>
 *
 * <p>
 * A TreeView may be configured to optionally hide the root node by setting the
 * {@link #setShowRoot(boolean) showRoot} property to {@code false}. If the root
 * node is hidden, there is one less level of indentation, and all children
 * nodes of the root node are shown. By default, the root node is shown in the
 * TreeView.
 *
 * <h3>TreeView Selection / Focus APIs</h3>
 * <p>To track selection and focus, it is necessary to become familiar with the
 * {@link SelectionModel} and {@link FocusModel} classes. A TreeView has at most
 * one instance of each of these classes, available from
 * {@link #selectionModelProperty() selectionModel} and
 * {@link #focusModelProperty() focusModel} properties respectively.
 * Whilst it is possible to use this API to set a new selection model, in
 * most circumstances this is not necessary - the default selection and focus
 * models should work in most circumstances.
 *
 * <p>The default {@link SelectionModel} used when instantiating a TreeView is
 * an implementation of the {@link MultipleSelectionModel} abstract class.
 * However, as noted in the API documentation for
 * the {@link MultipleSelectionModel#selectionModeProperty() selectionMode}
 * property, the default value is {@link SelectionMode#SINGLE}. To enable
 * multiple selection in a default TreeView instance, it is therefore necessary
 * to do the following:
 *
 * <pre>
 * {@code
 * treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);}</pre>
 *
 * <h3>Customizing TreeView Visuals</h3>
 * <p>The visuals of the TreeView can be entirely customized by replacing the
 * default {@link #cellFactoryProperty() cell factory}. A cell factory is used to
 * generate {@link TreeCell} instances, which are used to represent an item in the
 * TreeView. See the {@link Cell} class documentation for a more complete
 * description of how to write custom Cells.
 *
 * <h3>Editing</h3>
 * <p>This control supports inline editing of values, and this section attempts to
 * give an overview of the available APIs and how you should use them.</p>
 *
 * <p>Firstly, cell editing most commonly requires a different user interface
 * than when a cell is not being edited. This is the responsibility of the
 * {@link Cell} implementation being used. For TreeView, this is the responsibility
 * of the {@link #cellFactoryProperty() cell factory}. It is your choice whether the cell is
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
 * TreeView, which you can observe by adding an {@link EventHandler} via
 * {@link TreeView#setOnEditCommit(javafx.event.EventHandler)}. Similarly,
 * you can also observe edit events for
 * {@link TreeView#setOnEditStart(javafx.event.EventHandler) edit start}
 * and {@link TreeView#setOnEditCancel(javafx.event.EventHandler) edit cancel}.</p>
 *
 * <p>By default the TreeView edit commit handler is non-null, with a default
 * handler that attempts to overwrite the property value for the
 * item in the currently-being-edited row. It is able to do this as the
 * {@link Cell#commitEdit(Object)} method is passed in the new value, and this
 * is passed along to the edit commit handler via the
 * {@link EditEvent} that is fired. It is simply a matter of calling
 * {@link EditEvent#getNewValue()} to retrieve this value.
 *
 * <p>It is very important to note that if you call
 * {@link TreeView#setOnEditCommit(javafx.event.EventHandler)} with your own
 * {@link EventHandler}, then you will be removing the default handler. Unless
 * you then handle the writeback to the property (or the relevant data source),
 * nothing will happen. You can work around this by using the
 * {@link TreeView#addEventHandler(javafx.event.EventType, javafx.event.EventHandler)}
 * method to add a {@link TreeView#editCommitEvent()} {@link EventType} with
 * your desired {@link EventHandler} as the second argument. Using this method,
 * you will not replace the default implementation, but you will be notified when
 * an edit commit has occurred.</p>
 *
 * <p>Hopefully this summary answers some of the commonly asked questions.
 * Fortunately, JavaFX ships with a number of pre-built cell factories that
 * handle all the editing requirements on your behalf. You can find these
 * pre-built cell factories in the javafx.scene.control.cell package.</p>
 *
 * @see TreeItem
 * @see TreeCell
 * @param <T> The type of the item contained within the {@link TreeItem} value
 *      property for all tree items in this TreeView.
 * @since JavaFX 2.0
 */
@DefaultProperty("root")
public class TreeView<T> extends Control {

    /***************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * An EventType that indicates some edit event has occurred. It is the parent
     * type of all other edit events: {@link #editStartEvent()},
     *  {@link #editCommitEvent()} and {@link #editCancelEvent()}.
     *
     * @param <T> the type of the TreeItem instances used in this TreeView
     * @return An EventType that indicates some edit event has occurred.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editAnyEvent() {
        return (EventType<EditEvent<T>>) EDIT_ANY_EVENT;
    }
    private static final EventType<?> EDIT_ANY_EVENT =
            new EventType<>(Event.ANY, "TREE_VIEW_EDIT");

    /**
     * An EventType used to indicate that an edit event has started within the
     * TreeView upon which the event was fired.
     *
     * @param <T> the type of the TreeItem instances used in this TreeView
     * @return An EventType used to indicate that an edit event has started.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editStartEvent() {
        return (EventType<EditEvent<T>>) EDIT_START_EVENT;
    }
    private static final EventType<?> EDIT_START_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_START");

    /**
     * An EventType used to indicate that an edit event has just been canceled
     * within the TreeView upon which the event was fired.
     *
     * @param <T> the type of the TreeItem instances used in this TreeView
     * @return An EventType used to indicate that an edit event has just been
     *      canceled.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editCancelEvent() {
        return (EventType<EditEvent<T>>) EDIT_CANCEL_EVENT;
    }
    private static final EventType<?> EDIT_CANCEL_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_CANCEL");

    /**
     * An EventType that is used to indicate that an edit in a TreeView has been
     * committed. This means that user has made changes to the data of a
     * TreeItem, and that the UI should be updated.
     *
     * @param <T> the type of the TreeItem instances used in this TreeView
     * @return An EventType that is used to indicate that an edit in a TreeView
     *      has been committed.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editCommitEvent() {
        return (EventType<EditEvent<T>>) EDIT_COMMIT_EVENT;
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
     * past the root node of the TreeView control, if the root node has a parent.
     * If this is important, call {@link TreeView#getTreeItemLevel(TreeItem)}
     * instead.
     *
     * @param node The TreeItem for which the level is needed.
     * @return An integer representing the number of parents above the given node,
     *          or -1 if the given TreeItem is null.
     * @deprecated This method does not correctly calculate the distance from the
     *          given TreeItem to the root of the TreeView. As of JavaFX 8.0_20,
     *          the proper way to do this is via
     *          {@link TreeView#getTreeItemLevel(TreeItem)}
     */
    @Deprecated(since="8u20")
    public static int getNodeLevel(TreeItem<?> node) {
        if (node == null) return -1;

        int level = 0;
        TreeItem<?> parent = node.getParent();
        while (parent != null) {
            level++;
            parent = parent.getParent();
        }

        return level;
    }


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates an empty TreeView.
     *
     * <p>Refer to the {@link TreeView} class documentation for details on the
     * default state of other properties.
     */
    public TreeView() {
        this(null);
    }

    /**
     * Creates a TreeView with the provided root node.
     *
     * <p>Refer to the {@link TreeView} class documentation for details on the
     * default state of other properties.
     *
     * @param root The node to be the root in this TreeView.
     */
    public TreeView(TreeItem<T> root) {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.TREE_VIEW);

        setRoot(root);
        updateExpandedItemCount(root);

        // install default selection and focus models - it's unlikely this will be changed
        // by many users.
        MultipleSelectionModel<TreeItem<T>> sm = new TreeViewBitSetSelectionModel<T>(this);
        setSelectionModel(sm);
        setFocusModel(new TreeViewFocusModel<T>(this));
    }



    /***************************************************************************
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
    private Map<Integer, SoftReference<TreeItem<T>>> treeItemCacheMap = new HashMap<>();


    /***************************************************************************
     *                                                                         *
     * Callbacks and Events                                                    *
     *                                                                         *
     **************************************************************************/

    // we use this to forward events that have bubbled up TreeItem instances
    // to the TreeViewSkin, to force it to recalculate teh item count and redraw
    // if necessary
    private final EventHandler<TreeModificationEvent<T>> rootEvent = e -> {
        // this forces layoutChildren at the next pulse, and therefore
        // updates the item count if necessary
        EventType<?> eventType = e.getEventType();
        boolean match = false;
        while (eventType != null) {
            if (eventType.equals(TreeItem.<T>expandedItemCountChangeEvent())) {
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

    private WeakEventHandler<TreeModificationEvent<T>> weakRootEventListener;



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/


    // --- Cell Factory
    private ObjectProperty<Callback<TreeView<T>, TreeCell<T>>> cellFactory;

    /**
     * Sets the cell factory that will be used for creating TreeCells,
     * which are used to represent items in the
     * TreeView. The factory works identically to the cellFactory in ListView
     * and other complex composite controls. It is called to create a new
     * TreeCell only when the system has determined that it doesn't have enough
     * cells to represent the currently visible items. The TreeCell is reused
     * by the system to represent different items in the tree when possible.
     *
     * <p>Refer to the {@link Cell} class documentation for more details.
     *
     * @param value The {@link Callback} to use for generating TreeCell instances,
     *      or null if the default cell factory should be used.
     */
    public final void setCellFactory(Callback<TreeView<T>, TreeCell<T>> value) {
        cellFactoryProperty().set(value);
    }

    /**
     * <p>Returns the cell factory that will be used for creating TreeCells,
     * which are used to represent items in the TreeView, or null if no custom
     * cell factory has been set.
     * @return the cell factory
     */
    public final Callback<TreeView<T>, TreeCell<T>> getCellFactory() {
        return cellFactory == null ? null : cellFactory.get();
    }

    /**
     * Represents the cell factory that will be used for creating TreeCells,
     * which are used to represent items in the TreeView.
     * @return the cell factory property
     */
    public final ObjectProperty<Callback<TreeView<T>, TreeCell<T>>> cellFactoryProperty() {
        if (cellFactory == null) {
            cellFactory = new SimpleObjectProperty<Callback<TreeView<T>, TreeCell<T>>>(this, "cellFactory");
        }
        return cellFactory;
    }


    // --- Root
    private ObjectProperty<TreeItem<T>> root = new SimpleObjectProperty<TreeItem<T>>(this, "root") {
        private WeakReference<TreeItem<T>> weakOldItem;

        @Override protected void invalidated() {
            TreeItem<T> oldTreeItem = weakOldItem == null ? null : weakOldItem.get();
            if (oldTreeItem != null && weakRootEventListener != null) {
                oldTreeItem.removeEventHandler(TreeItem.<T>treeNotificationEvent(), weakRootEventListener);
            }

            TreeItem<T> root = getRoot();
            if (root != null) {
                weakRootEventListener = new WeakEventHandler<>(rootEvent);
                getRoot().addEventHandler(TreeItem.<T>treeNotificationEvent(), weakRootEventListener);
                weakOldItem = new WeakReference<>(root);
            }

            // Fix for RT-37853
            edit(null);

            expandedItemCountDirty = true;
            updateRootExpanded();
        }
    };

    /**
     * Sets the root node in this TreeView. See the {@link TreeItem} class level
     * documentation for more details.
     *
     * @param value The {@link TreeItem} that will be placed at the root of the
     *      TreeView.
     */
    public final void setRoot(TreeItem<T> value) {
        rootProperty().set(value);
    }

    /**
     * Returns the current root node of this TreeView, or null if no root node
     * is specified.
     * @return The current root node, or null if no root node exists.
     */
    public final TreeItem<T> getRoot() {
        return root == null ? null : root.get();
    }

    /**
     * Property representing the root node of the TreeView.
     * @return the root node property
     */
    public final ObjectProperty<TreeItem<T>> rootProperty() {
        return root;
    }



    // --- Show Root
    private BooleanProperty showRoot;

    /**
     * Specifies whether the root {@code TreeItem} should be shown within this
     * TreeView.
     *
     * @param value If true, the root TreeItem will be shown, and if false it
     *      will be hidden.
     */
    public final void setShowRoot(boolean value) {
        showRootProperty().set(value);
    }

    /**
     * Returns true if the root of the TreeView should be shown, and false if
     * it should not. By default, the root TreeItem is visible in the TreeView.
     * @return true if the root of the TreeView should be shown
     */
    public final boolean isShowRoot() {
        return showRoot == null ? true : showRoot.get();
    }

    /**
     * Property that represents whether or not the TreeView root node is visible.
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


    // --- Selection Model
    private ObjectProperty<MultipleSelectionModel<TreeItem<T>>> selectionModel;

    /**
     * Sets the {@link MultipleSelectionModel} to be used in the TreeView.
     * Despite a TreeView requiring a <code><b>Multiple</b>SelectionModel</code>,
     * it is possible to configure it to only allow single selection (see
     * {@link MultipleSelectionModel#setSelectionMode(javafx.scene.control.SelectionMode)}
     * for more information).
     * @param value the {@link MultipleSelectionModel} to be used
     */
    public final void setSelectionModel(MultipleSelectionModel<TreeItem<T>> value) {
        selectionModelProperty().set(value);
    }

    /**
     * Returns the currently installed selection model.
     * @return the currently installed selection model
     */
    public final MultipleSelectionModel<TreeItem<T>> getSelectionModel() {
        return selectionModel == null ? null : selectionModel.get();
    }

    /**
     * The SelectionModel provides the API through which it is possible
     * to select single or multiple items within a TreeView, as  well as inspect
     * which rows have been selected by the user. Note that it has a generic
     * type that must match the type of the TreeView itself.
     * @return the selection model property
     */
    public final ObjectProperty<MultipleSelectionModel<TreeItem<T>>> selectionModelProperty() {
        if (selectionModel == null) {
            selectionModel = new SimpleObjectProperty<MultipleSelectionModel<TreeItem<T>>>(this, "selectionModel");
        }
        return selectionModel;
    }


    // --- Focus Model
    private ObjectProperty<FocusModel<TreeItem<T>>> focusModel;

    /**
     * Sets the {@link FocusModel} to be used in the TreeView.
     * @param value the {@link FocusModel} to be used
     */
    public final void setFocusModel(FocusModel<TreeItem<T>> value) {
        focusModelProperty().set(value);
    }

    /**
     * Returns the currently installed {@link FocusModel}.
     * @return the currently installed {@link FocusModel}
     */
    public final FocusModel<TreeItem<T>> getFocusModel() {
        return focusModel == null ? null : focusModel.get();
    }

    /**
     * The FocusModel provides the API through which it is possible
     * to control focus on zero or one rows of the TreeView. Generally the
     * default implementation should be more than sufficient.
     * @return the focus model property
     */
    public final ObjectProperty<FocusModel<TreeItem<T>>> focusModelProperty() {
        if (focusModel == null) {
            focusModel = new SimpleObjectProperty<FocusModel<TreeItem<T>>>(this, "focusModel");
        }
        return focusModel;
    }


    // --- Expanded node count
    /**
     * <p>Represents the number of tree nodes presently able to be visible in the
     * TreeView. This is essentially the count of all expanded tree items, and
     * their children.
     *
     * <p>For example, if just the root node is visible, the expandedItemCount will
     * be one. If the root had three children and the root was expanded, the value
     * will be four.
     * @since JavaFX 8.0
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
                @Override public CssMetaData<TreeView<?>,Number> getCssMetaData() {
                    return StyleableProperties.FIXED_CELL_SIZE;
                }

                @Override public Object getBean() {
                    return TreeView.this;
                }

                @Override public String getName() {
                    return "fixedCellSize";
                }
            };
        }
        return fixedCellSize;
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
     * Specifies whether this TreeView is editable - only if the TreeView and
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


    // --- Editing Item
    private ReadOnlyObjectWrapper<TreeItem<T>> editingItem;

    private void setEditingItem(TreeItem<T> value) {
        editingItemPropertyImpl().set(value);
    }

    /**
     * Returns the TreeItem that is currently being edited in the TreeView,
     * or null if no item is being edited.
     * @return the TreeItem that is currently being edited in the TreeView
     */
    public final TreeItem<T> getEditingItem() {
        return editingItem == null ? null : editingItem.get();
    }

    /**
     * <p>A property used to represent the TreeItem currently being edited
     * in the TreeView, if editing is taking place, or null if no item is being edited.
     *
     * <p>It is not possible to set the editing item, instead it is required that
     * you call {@link #edit(javafx.scene.control.TreeItem)}.
     * @return the editing item property
     */
    public final ReadOnlyObjectProperty<TreeItem<T>> editingItemProperty() {
        return editingItemPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<TreeItem<T>> editingItemPropertyImpl() {
        if (editingItem == null) {
            editingItem = new ReadOnlyObjectWrapper<TreeItem<T>>(this, "editingItem");
        }
        return editingItem;
    }


    // --- On Edit Start
    private ObjectProperty<EventHandler<EditEvent<T>>> onEditStart;

    /**
     * Sets the {@link EventHandler} that will be called when the user begins
     * an edit.
     * @param value the {@link EventHandler} that will be called when the user
     * begins an edit
     */
    public final void setOnEditStart(EventHandler<EditEvent<T>> value) {
        onEditStartProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user begins
     * an edit.
     * @return the {@link EventHandler} when the user begins an edit
     */
    public final EventHandler<EditEvent<T>> getOnEditStart() {
        return onEditStart == null ? null : onEditStart.get();
    }

    /**
     * This event handler will be fired when the user successfully initiates
     * editing.
     * @return the event handler when the user successfully initiates editing
     */
    public final ObjectProperty<EventHandler<EditEvent<T>>> onEditStartProperty() {
        if (onEditStart == null) {
            onEditStart = new SimpleObjectProperty<EventHandler<EditEvent<T>>>(this, "onEditStart") {
                @Override protected void invalidated() {
                    setEventHandler(TreeView.<T>editStartEvent(), get());
                }
            };
        }
        return onEditStart;
    }


    // --- On Edit Commit
    private ObjectProperty<EventHandler<EditEvent<T>>> onEditCommit;

    /**
     * Sets the {@link EventHandler} that will be called when the user commits
     * an edit.
     * @param value the {@link EventHandler} that will be called when the user
     * commits an edit
     */
    public final void setOnEditCommit(EventHandler<EditEvent<T>> value) {
        onEditCommitProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user commits
     * an edit.
     * @return the {@link EventHandler} that will be called when the user commits
     * an edit
     */
    public final EventHandler<EditEvent<T>> getOnEditCommit() {
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
     * @return the event handler when the user performs an action that result in
     * their editing input being persisted
     */
    public final ObjectProperty<EventHandler<EditEvent<T>>> onEditCommitProperty() {
        if (onEditCommit == null) {
            onEditCommit = new SimpleObjectProperty<EventHandler<EditEvent<T>>>(this, "onEditCommit") {
                @Override protected void invalidated() {
                    setEventHandler(TreeView.<T>editCommitEvent(), get());
                }
            };
        }
        return onEditCommit;
    }


    // --- On Edit Cancel
    private ObjectProperty<EventHandler<EditEvent<T>>> onEditCancel;

    /**
     * Sets the {@link EventHandler} that will be called when the user cancels
     * an edit.
     * @param value the {@link EventHandler} that will be called when the user
     * cancels an edit
     */
    public final void setOnEditCancel(EventHandler<EditEvent<T>> value) {
        onEditCancelProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user cancels
     * an edit.
     * @return the {@link EventHandler} that will be called when the user cancels
     * an edit
     */
    public final EventHandler<EditEvent<T>> getOnEditCancel() {
        return onEditCancel == null ? null : onEditCancel.get();
    }

    /**
     * This event handler will be fired when the user cancels editing a cell.
     * @return the event handler will be fired when the user cancels editing a
     * cell
     */
    public final ObjectProperty<EventHandler<EditEvent<T>>> onEditCancelProperty() {
        if (onEditCancel == null) {
            onEditCancel = new SimpleObjectProperty<EventHandler<EditEvent<T>>>(this, "onEditCancel") {
                @Override protected void invalidated() {
                    setEventHandler(TreeView.<T>editCancelEvent(), get());
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
    @Override protected void layoutChildren() {
        if (expandedItemCountDirty) {
            updateExpandedItemCount(getRoot());
        }

        super.layoutChildren();
    }


    /**
     * Instructs the TreeView to begin editing the given TreeItem, if
     * the TreeView is {@link #editableProperty() editable}. Once
     * this method is called, if the current
     * {@link #cellFactoryProperty() cell factory} is set up to support editing,
     * the Cell will switch its visual state to enable the user input to take place.
     *
     * @param item The TreeItem in the TreeView that should be edited.
     */
    public void edit(TreeItem<T> item) {
        if (!isEditable()) return;
        setEditingItem(item);
    }


    /**
     * Scrolls the TreeView such that the item in the given index is visible to
     * the end user.
     *
     * @param index The index that should be made visible to the user, assuming
     *      of course that it is greater than, or equal to 0, and less than the
     *      number of the visible items in the TreeView.
     */
    public void scrollTo(int index) {
        ControlUtils.scrollToIndex(this, index);
    }

    /**
     * Called when there's a request to scroll an index into view using {@link #scrollTo(int)}
     * @since JavaFX 8.0
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
                @Override
                protected void invalidated() {
                    setEventHandler(ScrollToEvent.scrollToTopIndex(), get());
                }
                @Override
                public Object getBean() {
                    return TreeView.this;
                }

                @Override
                public String getName() {
                    return "onScrollTo";
                }
            };
        }
        return onScrollTo;
    }

    /**
     * Returns the index position of the given TreeItem, assuming that it is
     * currently accessible through the tree hierarchy (most notably, that all
     * parent tree items are expanded). If a parent tree item is collapsed,
     * the result is that this method will return -1 to indicate that the
     * given tree item is not accessible in the tree.
     *
     * @param item The TreeItem for which the index is sought.
     * @return An integer representing the location in the current TreeView of the
     *      first instance of the given TreeItem, or -1 if it is null or can not
     *      be found (for example, if a parent (all the way up to the root) is
     *      collapsed).
     */
    public int getRow(TreeItem<T> item) {
        return TreeUtil.getRow(item, getRoot(), expandedItemCountDirty, isShowRoot());
    }

    /**
     * Returns the TreeItem in the given index, or null if it is out of bounds.
     *
     * @param row The index of the TreeItem being sought.
     * @return The TreeItem in the given index, or null if it is out of bounds.
     */
    public TreeItem<T> getTreeItem(int row) {
        if (row < 0) return null;

        // normalize the requested row based on whether showRoot is set
        final int _row = isShowRoot() ? row : (row + 1);

        if (expandedItemCountDirty) {
            updateExpandedItemCount(getRoot());
        } else {
            if (treeItemCacheMap.containsKey(_row)) {
                SoftReference<TreeItem<T>> treeItemRef = treeItemCacheMap.get(_row);
                TreeItem<T> treeItem = treeItemRef.get();
                if (treeItem != null) {
                    return treeItem;
                }
            }
        }

        TreeItem<T> treeItem = TreeUtil.getItem(getRoot(), _row, expandedItemCountDirty);
        treeItemCacheMap.put(_row, new SoftReference<>(treeItem));
        return treeItem;
    }

    /**
     * Returns the number of levels of 'indentation' of the given TreeItem,
     * based on how many times getParent() can be recursively called. If the
     * given TreeItem is the root node of this TreeView, or if the TreeItem does
     * not have any parent set, the returned value will be zero. For each time
     * getParent() is recursively called, the returned value is incremented by one.
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

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TreeViewSkin<T>(this);
    }

    /**
     * Calling {@code refresh()} forces the TreeView control to recreate and
     * repopulate the cells necessary to populate the visual bounds of the control.
     * In other words, this forces the TreeView to update what it is showing to
     * the user. This is useful in cases where the underlying data source has
     * changed in a way that is not observed by the TreeView itself.
     *
     * @since JavaFX 8u60
     */
    public void refresh() {
        getProperties().put(Properties.RECREATE, Boolean.TRUE);
    }



    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void updateExpandedItemCount(TreeItem<T> treeItem) {
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



    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "tree-view";

    private static class StyleableProperties {
        private static final CssMetaData<TreeView<?>,Number> FIXED_CELL_SIZE =
                new CssMetaData<TreeView<?>,Number>("-fx-fixed-cell-size",
                                                     SizeConverter.getInstance(),
                                                     Region.USE_COMPUTED_SIZE) {

                    @Override public Double getInitialValue(TreeView<?> node) {
                        return node.getFixedCellSize();
                    }

                    @Override public boolean isSettable(TreeView<?> n) {
                        return n.fixedCellSize == null || !n.fixedCellSize.isBound();
                    }

                    @Override public StyleableProperty<Number> getStyleableProperty(TreeView<?> n) {
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
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @since JavaFX 8.0
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



    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case MULTIPLE_SELECTION: {
                MultipleSelectionModel<TreeItem<T>> sm = getSelectionModel();
                return sm != null && sm.getSelectionMode() == SelectionMode.MULTIPLE;
            }
            case ROW_COUNT: return getExpandedItemCount();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }



    /***************************************************************************
     *                                                                         *
     * Support Interfaces                                                      *
     *                                                                         *
     **************************************************************************/



    /***************************************************************************
     *                                                                         *
     * Support Classes                                                         *
     *                                                                         *
     **************************************************************************/


    /**
     * An {@link Event} subclass used specifically in TreeView for representing
     * edit-related events. It provides additional API to easily access the
     * TreeItem that the edit event took place on, as well as the input provided
     * by the end user.
     *
     * @param <T> The type of the input, which is the same type as the TreeView
     *      itself.
     * @since JavaFX 2.0
     */
    public static class EditEvent<T> extends Event {
        private static final long serialVersionUID = -4437033058917528976L;

        /**
         * Common supertype for all edit event types.
         * @since JavaFX 8.0
         */
        public static final EventType<?> ANY = EDIT_ANY_EVENT;

        private final TreeView<T> source;
        private final T oldValue;
        private final T newValue;
        private transient final TreeItem<T> treeItem;

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
        public EditEvent(TreeView<T> source,
                         EventType<? extends EditEvent> eventType,
                         TreeItem<T> treeItem, T oldValue, T newValue) {
            super(source, Event.NULL_SOURCE_TARGET, eventType);
            this.source = source;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.treeItem = treeItem;
        }

        /**
         * Returns the TreeView upon which the edit took place.
         */
        @Override public TreeView<T> getSource() {
            return source;
        }

        /**
         * Returns the {@link TreeItem} upon which the edit took place.
         * @return the {@link TreeItem} upon which the edit took place
         */
        public TreeItem<T> getTreeItem() {
            return treeItem;
        }

        /**
         * Returns the new value input into the TreeItem by the end user.
         * @return the new value input into the TreeItem by the end user
         */
        public T getNewValue() {
            return newValue;
        }

        /**
         * Returns the old value that existed in the TreeItem prior to the current
         * edit event.
         * @return the old value that existed in the TreeItem prior to the current
         * edit event
         */
        public T getOldValue() {
            return oldValue;
        }
    }







    // package for testing
    static class TreeViewBitSetSelectionModel<T> extends MultipleSelectionModelBase<TreeItem<T>> {

        /***********************************************************************
         *                                                                     *
         * Internal fields                                                     *
         *                                                                     *
         **********************************************************************/

        private TreeView<T> treeView = null;



        /***********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        public TreeViewBitSetSelectionModel(final TreeView<T> treeView) {
            if (treeView == null) {
                throw new IllegalArgumentException("TreeView can not be null");
            }

            this.treeView = treeView;
            this.treeView.rootProperty().addListener(weakRootPropertyListener);
            this.treeView.showRootProperty().addListener(o -> {
                shiftSelection(0, treeView.isShowRoot() ? 1 : -1, null);
            });

            updateTreeEventListener(null, treeView.getRoot());

            updateDefaultSelection();
        }

        private void updateTreeEventListener(TreeItem<T> oldRoot, TreeItem<T> newRoot) {
            if (oldRoot != null && weakTreeItemListener != null) {
                oldRoot.removeEventHandler(TreeItem.<T>expandedItemCountChangeEvent(), weakTreeItemListener);
            }

            if (newRoot != null) {
                weakTreeItemListener = new WeakEventHandler<>(treeItemListener);
                newRoot.addEventHandler(TreeItem.<T>expandedItemCountChangeEvent(), weakTreeItemListener);
            }
        }

        private ChangeListener<TreeItem<T>> rootPropertyListener = (observable, oldValue, newValue) -> {
            updateDefaultSelection();
            updateTreeEventListener(oldValue, newValue);
        };

        private EventHandler<TreeModificationEvent<T>> treeItemListener = e -> {
            if (getSelectedIndex() == -1 && getSelectedItem() == null) return;

            final TreeItem<T> treeItem = e.getTreeItem();
            if (treeItem == null) return;

            treeView.expandedItemCountDirty = true;

            // we only shift selection from this row - everything before it
            // is safe. We might change this below based on certain criteria
            int startRow = treeView.getRow(treeItem);

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

                    selectedIndices._beginChange();
                    final int from = startRow + 1;
                    final int to = startRow + count;
                    final List<Integer> removed = new ArrayList<>();
                    for (int i = from; i < to; i++) {
                        if (isSelected(i)) {
                            wasAnyChildSelected = true;
                            removed.add(i);
                        }
                    }

                    ControlUtils.reducingChange(selectedIndices, removed);

                    for (int index : removed) {
                        startAtomic();
                        clearSelection(index);
                        stopAtomic();
                    }
                    selectedIndices._endChange();

                    // put selection onto the newly-collapsed tree item
                    if (wasPrimarySelectionInChild && wasAnyChildSelected) {
                        select(startRow);
                    }

                    shift += -count + 1;
                    startRow++;
                } else if (e.wasPermutated()) {
                    // no-op
                } else if (e.wasAdded()) {
                    // shuffle selection by the number of added items
                    shift += treeItem.isExpanded() ? addedSize : 0;

                    // RT-32963: We were taking the startRow from the TreeItem
                    // in which the children were added, rather than from the
                    // actual position of the new child. This led to selection
                    // being moved off the parent TreeItem by mistake.
                    // The 'if (e.getAddedSize() == 1)' condition here was
                    // subsequently commented out due to RT-33894.
                    startRow = treeView.getRow(e.getChange().getAddedSubList().get(0));
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
                    final List<Integer> selectedIndices1 = getSelectedIndices();
                    final int selectedIndex = getSelectedIndex();
                    final List<TreeItem<T>> selectedItems = getSelectedItems();
                    final TreeItem<T> selectedItem = getSelectedItem();
                    final List<? extends TreeItem<T>> removedChildren = e.getChange().getRemoved();

                    for (int i = 0; i < selectedIndices1.size() && !selectedItems.isEmpty(); i++) {
                        int index = selectedIndices1.get(i);
                        if (index > selectedItems.size()) break;

                        if (removedChildren.size() == 1 &&
                                selectedItems.size() == 1 &&
                                selectedItem != null &&
                                selectedItem.equals(removedChildren.get(0))) {
                            // Bug fix for RT-28637
                            if (selectedIndex < getItemCount()) {
                                final int previousRow = selectedIndex == 0 ? 0 : selectedIndex - 1;
                                TreeItem<T> newSelectedItem = getModelItem(previousRow);
                                if (!selectedItem.equals(newSelectedItem)) {
                                    select(newSelectedItem);
                                }
                            }
                        }
                    }
                }
            } while (e.getChange() != null && e.getChange().next());

            shiftSelection(startRow, shift, null);

            if (e.wasAdded() || e.wasRemoved()) {
                Integer anchor = TreeCellBehavior.getAnchor(treeView, null);
                if (anchor != null && isSelected(anchor + shift)) {
                    TreeCellBehavior.setAnchor(treeView, anchor + shift, false);
                }
            }
        };

        private WeakChangeListener<TreeItem<T>> weakRootPropertyListener =
                new WeakChangeListener<>(rootPropertyListener);

        private WeakEventHandler<TreeModificationEvent<T>> weakTreeItemListener;



        /***********************************************************************
         *                                                                     *
         * Public selection API                                                *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override public void selectAll() {
            // when a selectAll happens, the anchor should not change, so we store it
            // before, and restore it afterwards
            final int anchor = TreeCellBehavior.getAnchor(treeView, -1);
            super.selectAll();
            TreeCellBehavior.setAnchor(treeView, anchor, false);
        }

        /** {@inheritDoc} */
        @Override public void select(TreeItem<T> obj) {
//        if (getRowCount() <= 0) return;

            if (obj == null && getSelectionMode() == SelectionMode.SINGLE) {
                clearSelection();
                return;
            }

            // we firstly expand the path down such that the given object is
            // visible. This fixes RT-14456, where selection was not happening
            // correctly on TreeItems that are not visible.

            if (obj != null) {
                TreeItem<?> item = obj.getParent();
                while (item != null) {
                    item.setExpanded(true);
                    item = item.getParent();
                }
            }

            // Fix for RT-15419. We eagerly update the tree item count, such that
            // selection occurs on the row
            treeView.updateExpandedItemCount(treeView.getRoot());

            // We have no option but to iterate through the model and select the
            // first occurrence of the given object. Once we find the first one, we
            // don't proceed to select any others.
            int row = treeView.getRow(obj);

            if (row == -1) {
                // if we are here, we did not find the item in the entire data model.
                // Even still, we allow for this item to be set to the give object.
                // We expect that in concrete subclasses of this class we observe the
                // data model such that we check to see if the given item exists in it,
                // whilst SelectedIndex == -1 && SelectedItem != null.
                setSelectedIndex(-1);
                setSelectedItem(obj);
            } else {
                select(row);
            }
        }

        /** {@inheritDoc} */
        @Override public void clearAndSelect(int row) {
            TreeCellBehavior.setAnchor(treeView, row, false);
            super.clearAndSelect(row);
        }


        /***********************************************************************
         *                                                                     *
         * Support code                                                        *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override protected void focus(int itemIndex) {
            if (treeView.getFocusModel() != null) {
                treeView.getFocusModel().focus(itemIndex);
            }

            // FIXME this is not the correct location for fire selection events (and does not take into account multiple selection)
            treeView.notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM);
        }

        /** {@inheritDoc} */
        @Override protected int getFocusedIndex() {
            if (treeView.getFocusModel() == null) return -1;
            return treeView.getFocusModel().getFocusedIndex();
        }

        /** {@inheritDoc} */
        @Override protected int getItemCount() {
            return treeView == null ? 0 : treeView.getExpandedItemCount();
        }

        /** {@inheritDoc} */
        @Override public TreeItem<T> getModelItem(int index) {
            if (treeView == null) return null;

            if (index < 0 || index >= treeView.getExpandedItemCount()) return null;

            return treeView.getTreeItem(index);
        }



        /***********************************************************************
         *                                                                     *
         * Private implementation                                              *
         *                                                                     *
         **********************************************************************/

        private void updateDefaultSelection() {
            clearSelection();

            // we put focus onto the first item, if there is at least
            // one item in the list
            focus(getItemCount() > 0 ? 0 : -1);
        }
    }



    /**
     *
     * @param <T>
     */
    static class TreeViewFocusModel<T> extends FocusModel<TreeItem<T>> {

        private final TreeView<T> treeView;

        public TreeViewFocusModel(final TreeView<T> treeView) {
            this.treeView = treeView;
            this.treeView.rootProperty().addListener(weakRootPropertyListener);
            updateTreeEventListener(null, treeView.getRoot());

            if (treeView.getExpandedItemCount() > 0) {
                focus(0);
            }

            treeView.showRootProperty().addListener(o -> {
                if (isFocused(0)) {
                    focus(-1);
                    focus(0);
                }
            });

            focusedIndexProperty().addListener(o -> {
                treeView.notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM);
            });
        }

        private final ChangeListener<TreeItem<T>> rootPropertyListener = (observable, oldValue, newValue) -> {
            updateTreeEventListener(oldValue, newValue);
        };

        private final WeakChangeListener<TreeItem<T>> weakRootPropertyListener =
                new WeakChangeListener<>(rootPropertyListener);

        private void updateTreeEventListener(TreeItem<T> oldRoot, TreeItem<T> newRoot) {
            if (oldRoot != null && weakTreeItemListener != null) {
                oldRoot.removeEventHandler(TreeItem.<T>expandedItemCountChangeEvent(), weakTreeItemListener);
            }

            if (newRoot != null) {
                weakTreeItemListener = new WeakEventHandler<>(treeItemListener);
                newRoot.addEventHandler(TreeItem.<T>expandedItemCountChangeEvent(), weakTreeItemListener);
            }
        }

        private EventHandler<TreeModificationEvent<T>> treeItemListener = new EventHandler<TreeModificationEvent<T>>() {
            @Override public void handle(TreeModificationEvent<T> e) {
                // don't shift focus if the event occurred on a tree item after
                // the focused row, or if there is no focus index at present
                if (getFocusedIndex() == -1) return;

                int row = treeView.getRow(e.getTreeItem());

                int shift = 0;
                if (e.getChange() != null) {
                    e.getChange().next();
                }

                do {
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
                        TreeItem<T> eventTreeItem = e.getTreeItem();
                        if (eventTreeItem.isExpanded()) {
                            for (int i = 0; i < e.getAddedChildren().size(); i++) {
                                // get the added item and determine the row it is in
                                TreeItem<T> item = e.getAddedChildren().get(i);
                                row = treeView.getRow(item);

                                if (item != null && row <= (shift+getFocusedIndex())) {
                                    shift += item.getExpandedDescendentCount(false);
                                }
                            }
                        }
                    } else if (e.wasRemoved()) {
                        row += e.getFrom() + 1;

                        for (int i = 0; i < e.getRemovedChildren().size(); i++) {
                            TreeItem<T> item = e.getRemovedChildren().get(i);
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

                if(shift != 0) {
                    final int newFocus = getFocusedIndex() + shift;
                    if (newFocus >= 0) {
                        Platform.runLater(() -> focus(newFocus));
                    }
                }
            }
        };

        private WeakEventHandler<TreeModificationEvent<T>> weakTreeItemListener;

        @Override protected int getItemCount() {
            return treeView == null ? -1 : treeView.getExpandedItemCount();
        }

        @Override protected TreeItem<T> getModelItem(int index) {
            if (treeView == null) return null;

            if (index < 0 || index >= treeView.getExpandedItemCount()) return null;

            return treeView.getTreeItem(index);
        }

        /** {@inheritDoc} */
        @Override public void focus(int index) {
            if (treeView.expandedItemCountDirty) {
                treeView.updateExpandedItemCount(treeView.getRoot());
            }

            super.focus(index);
        }
    }
}
