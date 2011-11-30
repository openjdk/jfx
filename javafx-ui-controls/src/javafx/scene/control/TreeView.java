/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.util.Callback;

import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.scene.control.WeakEventHandler;
import com.sun.javafx.scene.control.skin.VirtualContainerBase;
import java.lang.ref.WeakReference;
import javafx.beans.DefaultProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.WeakChangeListener;

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
 * <p>
 * The visuals of a TreeView can be entirely customized by creating a
 * {@link #cellFactoryProperty()  cellFactory} for the TreeView. The cell 
 * factory is used for generating {@link TreeCell} instances which will be used 
 * to represent a single TreeItem in the TreeView. See the {@link Cell} class 
 * documentation for a more complete description of how to write custom cells.
 *
 * @see TreeItem
 * @see TreeCell
 * @param <T> The type of the item contained within the {@link TreeItem} value
 *      property for all tree items in this TreeView.
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
     * type of all other edit events: {@link #editStartEvent},
     *  {@link #editCommitEvent} and {@link #editCancelEvent}.
     * 
     * @return An EventType that indicates some edit event has occurred.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editAnyEvent() {
        return (EventType<EditEvent<T>>) EDIT_ANY_EVENT;
    }
    private static final EventType<?> EDIT_ANY_EVENT =
            new EventType<EditEvent<Object>>(Event.ANY, "EDIT");

    /**
     * An EventType used to indicate that an edit event has started within the
     * TreeView upon which the event was fired.
     * 
     * @return An EventType used to indicate that an edit event has started.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editStartEvent() {
        return (EventType<EditEvent<T>>) EDIT_START_EVENT;
    }
    private static final EventType<?> EDIT_START_EVENT =
            new EventType<EditEvent<Object>>(editAnyEvent(), "EDIT_START");

    /**
     * An EventType used to indicate that an edit event has just been canceled
     * within the TreeView upon which the event was fired.
     * 
     * @return An EventType used to indicate that an edit event has just been
     *      canceled.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editCancelEvent() {
        return (EventType<EditEvent<T>>) EDIT_CANCEL_EVENT;
    }
    private static final EventType<?> EDIT_CANCEL_EVENT =
            new EventType<EditEvent<Object>>(editAnyEvent(), "EDIT_CANCEL");

    /**
     * An EventType that is used to indicate that an edit in a TreeView has been
     * committed. This means that user has made changes to the data of a
     * TreeItem, and that the UI should be updated.
     * 
     * @return An EventType that is used to indicate that an edit in a TreeView
     *      has been committed.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editCommitEvent() {
        return (EventType<EditEvent<T>>) EDIT_COMMIT_EVENT;
    }
    private static final EventType<?> EDIT_COMMIT_EVENT =
            new EventType<EditEvent<Object>>(editAnyEvent(), "EDIT_COMMIT");
    
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
        if (node == null) return -1;

        int level = 0;
        TreeItem parent = node.getParent();
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
     */
    public TreeView() {
        this(null);
    }

    /**
     * Creates a TreeView with the provided root node.
     * @param root The node to be the root in this TreeView.
     */
    public TreeView(TreeItem<T> root) {
        getStyleClass().setAll("tree-view");

        setRoot(root);
        updateTreeItemCount();

        // install default selection and focus models - it's unlikely this will be changed
        // by many users.
        MultipleSelectionModel sm = new TreeViewBitSetSelectionModel<T>(this);
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
    private boolean treeItemCountDirty = true;
    
    
    /***************************************************************************
     *                                                                         *
     * Callbacks and Events                                                    *
     *                                                                         *
     **************************************************************************/
    
    // we use this to forward events that have bubbled up TreeItem instances
    // to the TreeViewSkin, to force it to recalculate teh item count and redraw
    // if necessary
    private final EventHandler<TreeModificationEvent<T>> rootEvent = new EventHandler<TreeModificationEvent<T>>() {
        @Override public void handle(TreeModificationEvent<T> e) {
            // this forces layoutChildren at the next pulse, and therefore
            // updates the item count if necessary
            EventType eventType = e.getEventType();
            boolean match = false;
            while (eventType != null) {
                if (eventType.equals(TreeItem.<T>treeItemCountChangeEvent())) {
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
    
    private WeakEventHandler weakRootEventListener;
    
    
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
     */
    public final Callback<TreeView<T>, TreeCell<T>> getCellFactory() { 
        return cellFactory == null ? null : cellFactory.get(); 
    }
    
    /**
     * Represents the cell factory that will be used for creating TreeCells,
     * which are used to represent items in the TreeView. 
     */
    public final ObjectProperty<Callback<TreeView<T>, TreeCell<T>>> cellFactoryProperty() {
        if (cellFactory == null) {
            cellFactory = new SimpleObjectProperty<Callback<TreeView<T>, TreeCell<T>>>(this, "cellFactory");
        }
        return cellFactory;
    }

    
    // --- Root
    private ObjectProperty<TreeItem<T>> root = new ObjectPropertyBase<TreeItem<T>>() {
        private WeakReference<TreeItem<T>> weakOldItem;

        @Override protected void invalidated() {
            TreeItem<T> oldTreeItem = weakOldItem == null ? null : weakOldItem.get();
            if (oldTreeItem != null && weakRootEventListener != null) {
                oldTreeItem.removeEventHandler(TreeItem.<T>treeNotificationEvent(), weakRootEventListener);
            }

            TreeItem<T> root = getRoot();
            if (root != null) {
                weakRootEventListener = new WeakEventHandler(root, TreeItem.<T>treeNotificationEvent(), rootEvent);
                getRoot().addEventHandler(TreeItem.<T>treeNotificationEvent(), weakRootEventListener);
                weakOldItem = new WeakReference<TreeItem<T>>(root);
            }

            setTreeItemCount(0);
            updateRootExpanded();
        }

        @Override public Object getBean() {
            return TreeView.this;
        }

        @Override public String getName() {
            return "root";
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
     */
    public final boolean isShowRoot() {
        return showRoot == null ? true : showRoot.get();
    }

    /**
     * Property that represents whether or not the TreeView root node is visible.
     */
    public final BooleanProperty showRootProperty() {
        if (showRoot == null) {
            showRoot = new BooleanPropertyBase(true) {
                @Override protected void invalidated() {
                    updateRootExpanded();
                }

                @Override
                public Object getBean() {
                    return TreeView.this;
                }

                @Override
                public String getName() {
                    return "showRoot";
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
     */
    public final void setSelectionModel(MultipleSelectionModel<TreeItem<T>> value) {
        selectionModelProperty().set(value);
    }

    /**
     * Returns the currently installed selection model.
     */
    public final MultipleSelectionModel<TreeItem<T>> getSelectionModel() {
        return selectionModel == null ? null : selectionModel.get();
    }

    /**
     * The SelectionModel provides the API through which it is possible
     * to select single or multiple items within a TreeView, as  well as inspect
     * which rows have been selected by the user. Note that it has a generic
     * type that must match the type of the TreeView itself.
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
     */
    public final void setFocusModel(FocusModel<TreeItem<T>> value) {
        focusModelProperty().set(value);
    }

    /**
     * Returns the currently installed {@link FocusModel}.
     */
    public final FocusModel<TreeItem<T>> getFocusModel() {
        return focusModel == null ? null : focusModel.get();
    }

    /**
     * The FocusModel provides the API through which it is possible
     * to control focus on zero or one rows of the TreeView. Generally the
     * default implementation should be more than sufficient.
     */
    public final ObjectProperty<FocusModel<TreeItem<T>>> focusModelProperty() {
        if (focusModel == null) {
            focusModel = new SimpleObjectProperty<FocusModel<TreeItem<T>>>(this, "focusModel");
        }
        return focusModel;
    }
    
    
    // --- Tree node count
    private IntegerProperty treeItemCount = new SimpleIntegerProperty(this, "impl_treeItemCount", 0);

    private void setTreeItemCount(int value) {
        impl_treeItemCountProperty().set(value);
    }

    /**
     * <p>Represents the number of tree nodes presently able to be visible in the
     * TreeView. This is essentially the count of all expanded tree nodes, and
     * their children.
     *
     * <p>For example, if just the root node is visible, the treeItemCount will
     * be one. If the root had three children and the root was expanded, the value
     * will be four.
     * 
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final int impl_getTreeItemCount() {
        return treeItemCount.get();
    }

    /**
     * @treatasprivate implementation detail
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
     * Specifies whether this TreeView is editable - only if the TreeView and
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
    private ReadOnlyObjectWrapper<TreeItem<T>> editingItem;

    private void setEditingItem(TreeItem<T> value) {
        editingItemPropertyImpl().set(value);
    }

    /**
     * Returns the TreeItem that is currently being edited in the TreeView,
     * or null if no item is being edited.
     */
    public final TreeItem<T> getEditingItem() {
        return editingItem == null ? null : editingItem.get();
    }

    /**
     * <p>A property used to represent the TreeItem currently being edited
     * in the TreeView, if editing is taking place, or -1 if no item is being edited.
     * 
     * <p>It is not possible to set the editing item, instead it is required that
     * you call {@link #edit(javafx.scene.control.TreeItem)}.
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
     */
    public final void setOnEditStart(EventHandler<EditEvent<T>> value) {
        onEditStartProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user begins
     * an edit.
     */
    public final EventHandler<EditEvent<T>> getOnEditStart() {
        return onEditStart == null ? null : onEditStart.get();
    }

    /**
     * This event handler will be fired when the user successfully initiates
     * editing.
     */
    public final ObjectProperty<EventHandler<EditEvent<T>>> onEditStartProperty() {
        if (onEditStart == null) {
            onEditStart = new ObjectPropertyBase<EventHandler<EditEvent<T>>>() {
                @Override protected void invalidated() {
                    setEventHandler(TreeView.<T>editStartEvent(), get());
                }

                @Override
                public Object getBean() {
                    return TreeView.this;
                }

                @Override
                public String getName() {
                    return "onEditStart";
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
     */
    public final void setOnEditCommit(EventHandler<EditEvent<T>> value) {
        onEditCommitProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user commits
     * an edit.
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
     */
    public final ObjectProperty<EventHandler<EditEvent<T>>> onEditCommitProperty() {
        if (onEditCommit == null) {
            onEditCommit = new ObjectPropertyBase<EventHandler<EditEvent<T>>>() {
                @Override protected void invalidated() {
                    setEventHandler(TreeView.<T>editCommitEvent(), get());
                }

                @Override
                public Object getBean() {
                    return TreeView.this;
                }

                @Override
                public String getName() {
                    return "onEditCommit";
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
     */
    public final void setOnEditCancel(EventHandler<EditEvent<T>> value) {
        onEditCancelProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user cancels
     * an edit.
     */
    public final EventHandler<EditEvent<T>> getOnEditCancel() {
        return onEditCancel == null ? null : onEditCancel.get();
    }

    /**
     * This event handler will be fired when the user cancels editing a cell.
     */
    public final ObjectProperty<EventHandler<EditEvent<T>>> onEditCancelProperty() {
        if (onEditCancel == null) {
            onEditCancel = new ObjectPropertyBase<EventHandler<EditEvent<T>>>() {
                @Override protected void invalidated() {
                    setEventHandler(TreeView.<T>editCancelEvent(), get());
                }

                @Override
                public Object getBean() {
                    return TreeView.this;
                }

                @Override
                public String getName() {
                    return "onEditCancel";
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
        if (treeItemCountDirty) {
            updateTreeItemCount();
            treeItemCountDirty = false;
        }
        
        super.layoutChildren();
    }
    
    
    /**
     * Instructs the TreeView to begin editing the given TreeItem. Once
     * this method is called, if the current 
     * {@link #cellFactoryProperty() cell factory} is set up to support editing,
     * the Cell will switch its visual state to enable the user input to take place.
     * 
     * @param item The TreeItem in the TreeView that should be edited.
     */
    public void edit(TreeItem<T> item) {
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
       getProperties().put(VirtualContainerBase.SCROLL_TO_INDEX, index);
    }

    /**
     * Returns the index position of the given TreeItem, taking into account the
     * current state of each TreeItem (i.e. whether or not it is expanded).
     * 
     * @param item The TreeItem for which the index is sought.
     * @return An integer representing the location in the current TreeView of the
     *      first instance of the given TreeItem, or -1 if it is null or can not 
     *      be found.
     */
    public int getRow(TreeItem<T> item) {
        if (item == null) {
            return -1;
        } else if (isShowRoot() && item.equals(getRoot())) {
            return 0;
        }
        
        int row = 0;
        TreeItem<T> i = item;
        TreeItem<T> p = item.getParent();
        
        TreeItem<T> s;
        List<TreeItem<T>> siblings;
        
        while (!i.equals(getRoot()) && p != null) {
            siblings = p.getChildren();
            
            // work up each sibling, from the current item
            int itemIndex = siblings.indexOf(i);
            for (int pos = itemIndex - 1; pos > -1; pos--) {
                s = siblings.get(pos);
                if (s == null) continue;
                
                row += getExpandedDescendantCount(s);
                
                if (s.equals(getRoot())) {
                    if (! isShowRoot()) {
                        // special case: we've found out that our sibling is 
                        // actually the root node AND we aren't showing root nodes.
                        // This means that the item shouldn't actually be shown.
                        return -1;
                    }
                    return row;
                }
            }
            
            i = p;
            p = p.getParent();
            row++;
        }
        
        return p == null && row == 0 ? -1 : isShowRoot() ? row : row - 1;
    }

    /**
     * Returns the TreeItem in the given index, or null if it is out of bounds.
     * 
     * @param row The index of the TreeItem being sought.
     * @return The TreeItem in the given index, or null if it is out of bounds.
     */
    public TreeItem<T> getTreeItem(int row) {
        // normalize the requested row based on whether showRoot is set
        int r = isShowRoot() ? row : (row + 1);
        return getItem(getRoot(), r);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/  
    
    private int getExpandedDescendantCount(TreeItem<T> node) {
        if (node == null) return 0;
        if (node.isLeaf()) return 1;
        
        return node.getExpandedDescendentCount();
    }
    
    private void updateTreeItemCount() {
        if (getRoot() == null) {
            setTreeItemCount(0);
        } else if (! getRoot().isExpanded()) {
            setTreeItemCount(1);
        } else {
            int count = getExpandedDescendantCount(getRoot());
            if (! isShowRoot()) count--;

            setTreeItemCount(count);
        }
    }

    private TreeItem getItem(TreeItem<T> parent, int itemIndex) {
        if (parent == null) return null;

        // if itemIndex is 0 then our item is what we were looking for
        if (itemIndex == 0) return parent;

        // if itemIndex is > the total item count, then it is out of range
        if (itemIndex >= getExpandedDescendantCount(parent)) return null;

        // if we got here, then one of our descendants is the item we're after
        TreeItem<T> ret;
        int idx = itemIndex - 1;

        List<TreeItem<T>> children = parent.getChildren();
        if (children != null) {
            for (TreeItem c : children) {
                ret = getItem(c, idx);
                idx -= getExpandedDescendantCount(c);
                if (ret != null) return ret;
            }
        }

        // We might get here if getItem(0) is called on an empty tree
        return null;
    }

    private void updateRootExpanded() {
        // if we aren't showing the root, and the root isn't expanded, we expand
        // it now so that something is shown.
        if (!isShowRoot() && getRoot() != null && ! getRoot().isExpanded()) {
            getRoot().setExpanded(true);
        }

        updateTreeItemCount();
    }


    
    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static class StyleableProperties {
        private static final List<StyleableProperty> STYLEABLES;
        private static final int[] bitIndices;
        static {
            final List<StyleableProperty> styleables = 
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            STYLEABLES = Collections.unmodifiableList(styleables);
            
            bitIndices = new int[StyleableProperty.getMaxIndex()];
            java.util.Arrays.fill(bitIndices, -1);
            for(int bitIndex=0; bitIndex<STYLEABLES.size(); bitIndex++) {
                bitIndices[STYLEABLES.get(bitIndex).getIndex()] = bitIndex;
            }
        }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected int[] impl_cssStyleablePropertyBitIndices() {
        return TreeView.StyleableProperties.bitIndices;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return TreeView.StyleableProperties.STYLEABLES;
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
     */
    public static class EditEvent<T> extends Event {
        private final T oldValue;
        private final T newValue;
        private transient final TreeItem<T> treeItem;
        
        /**
         * Creates a new EditEvent instance to represent an edit event. This 
         * event is used for {@link #EDIT_START_EVENT}, 
         * {@link #EDIT_COMMIT_EVENT} and {@link #EDIT_CANCEL_EVENT} types.
         */
        public EditEvent(TreeView<T> source,
                         EventType<? extends EditEvent> eventType,
                         TreeItem<T> treeItem, T oldValue, T newValue) {
            super(source, Event.NULL_SOURCE_TARGET, eventType);
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.treeItem = treeItem;
        }

        /**
         * Returns the TreeView upon which the edit took place.
         */
        @Override public TreeView<T> getSource() {
            return (TreeView) super.getSource();
        }

        /**
         * Returns the {@link TreeItem} upon which the edit took place.
         */
        public TreeItem<T> getTreeItem() {
            return treeItem;
        }
        
        /**
         * Returns the new value input into the TreeItem by the end user.
         */
        public T getNewValue() {
            return newValue;
        }
        
        /**
         * Returns the old value that existed in the TreeItem prior to the current
         * edit event.
         */
        public T getOldValue() {
            return oldValue;
        }
    }
    
    
    
    



    // package for testing
    static class TreeViewBitSetSelectionModel<T> extends MultipleSelectionModelBase<TreeItem<T>> {

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
                    
            updateTreeEventListener(null, treeView.getRoot());
        }
        
        private void updateTreeEventListener(TreeItem<T> oldRoot, TreeItem<T> newRoot) {
            if (oldRoot != null && weakTreeItemListener != null) {
                oldRoot.removeEventHandler(TreeItem.<T>treeItemCountChangeEvent(), weakTreeItemListener);
            }
            
            if (newRoot != null) {
                weakTreeItemListener = new WeakEventHandler(newRoot, TreeItem.<T>treeItemCountChangeEvent(), treeItemListener);
                newRoot.addEventHandler(TreeItem.<T>treeItemCountChangeEvent(), weakTreeItemListener);
            }
        }
        
        private ChangeListener rootPropertyListener = new ChangeListener<TreeItem<T>>() {
            @Override public void changed(ObservableValue<? extends TreeItem<T>> observable, 
                    TreeItem<T> oldValue, TreeItem<T> newValue) {
                setSelectedIndex(-1);
                updateTreeEventListener(oldValue, newValue);
            }
        };
        
        private EventHandler<TreeModificationEvent<T>> treeItemListener = new EventHandler<TreeModificationEvent<T>>() {
            @Override public void handle(TreeModificationEvent<T> e) {
                // we only shift selection from this row - everything before it
                // is safe. We might change this below based on certain criteria
                int startRow = treeView.getRow(e.getTreeItem());
                
                int shift = 0;
                if (e.wasExpanded()) {
                    // need to shuffle selection by the number of visible children
                    shift = e.getTreeItem().getExpandedDescendentCount() - 1;
                    startRow++;
                } else if (e.wasCollapsed()) {
                    // remove selection from any child treeItem
                    int row = treeView.getRow(e.getTreeItem());
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
                        
                        TreeItem<T> item = getSelectedItems().get(index);
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
         * Internal properties                                                 *
         *                                                                     *
         **********************************************************************/

        private final TreeView<T> treeView;


        
        /***********************************************************************
         *                                                                     *
         * Public selection API                                                *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override public void select(TreeItem<T> obj) {
//        if (getRowCount() <= 0) return;
            
            // we firstly expand the path down such that the given object is
            // visible. This fixes RT-14456, where selection was not happening
            // correctly on TreeItems that are not visible.
            TreeItem<?> item = obj;
            while (item != null) {
                item.setExpanded(true);
                item = item.getParent();
            }
            
            // Fix for RT-15419. We eagerly update the tree item count, such that
            // selection occurs on the row
            treeView.updateTreeItemCount();
            
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
                setSelectedItem(obj);
            } else {
                select(row);
            }
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
        }

        /** {@inheritDoc} */
        @Override protected int getFocusedIndex() {
            if (treeView.getFocusModel() == null) return -1;
            return treeView.getFocusModel().getFocusedIndex();
        }
        
        /** {@inheritDoc} */
        @Override protected int getItemCount() {
            return treeView == null ? 0 : treeView.impl_getTreeItemCount();
        }

        /** {@inheritDoc} */
        @Override public TreeItem<T> getModelItem(int index) {
            if (treeView == null) return null;

            if (index < 0 || index >= treeView.impl_getTreeItemCount()) return null;

            return treeView.getTreeItem(index);
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
        }
        
        private final ChangeListener rootPropertyListener = new ChangeListener<TreeItem<T>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<T>> observable, TreeItem<T> oldValue, TreeItem<T> newValue) {
                updateTreeEventListener(oldValue, newValue);
            }
        };
                
        private final WeakChangeListener weakRootPropertyListener =
                new WeakChangeListener(rootPropertyListener);
        
        private void updateTreeEventListener(TreeItem<T> oldRoot, TreeItem<T> newRoot) {
            if (oldRoot != null && weakTreeItemListener != null) {
                oldRoot.removeEventHandler(TreeItem.<T>treeItemCountChangeEvent(), weakTreeItemListener);
            }
            
            if (newRoot != null) {
                weakTreeItemListener = new WeakEventHandler(newRoot, TreeItem.<T>treeItemCountChangeEvent(), treeItemListener);
                newRoot.addEventHandler(TreeItem.<T>treeItemCountChangeEvent(), weakTreeItemListener);
            }
        }
        
        private EventHandler<TreeModificationEvent<T>> treeItemListener = new EventHandler<TreeModificationEvent<T>>() {
            @Override public void handle(TreeModificationEvent<T> e) {

                // don't shift focus if the event occurred on a tree item after
                // the focused row
                int row = treeView.getRow(e.getTreeItem());
                
                int shift = 0;
                if (e.wasExpanded()) {
                    if (row > getFocusedIndex()) {
                        // need to shuffle selection by the number of visible children
                        shift = e.getTreeItem().getExpandedDescendentCount() - 1;
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
                        row = treeView.getRow(item);
                        
                        if (row <= getFocusedIndex()) {
//                            shift = e.getTreeItem().isExpanded() ? e.getAddedSize() : 0;
                            shift += item.getExpandedDescendentCount();
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

        @Override protected int getItemCount() {
            return treeView == null ? -1 : treeView.impl_getTreeItemCount();
        }

        @Override protected TreeItem<T> getModelItem(int index) {
            if (treeView == null) return null;

            if (index < 0 || index >= treeView.impl_getTreeItemCount()) return null;

            return treeView.getTreeItem(index);
        }
    }
}
