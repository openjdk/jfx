/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;

import com.sun.javafx.event.EventHandlerManager;
import java.util.Comparator;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import static javafx.scene.control.TreeSortMode.*;

/**
 * The model for a single node supplying a hierarchy of values to a control such
 * as {@link TreeView}. The model may be implemented such that values may be loaded in
 * memory as they are needed.
 * <p>
 * The model allows registration of listeners which will be notified as the
 * number of items changes, their position or if the values themselves change.
 * Note however that a TreeItem is <b>not</b> a Node, and therefore no visual
 * events will be fired on the TreeItem. To get these events, it is necessary to
 * add relevant observers to the TreeCell instances (via a custom cell factory -
 * see the {@link Cell} class documentation for more details).
 *
 * <p>In the simplest case, TreeItem instances may be created in memory as such:
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
 * This approach works well for simple tree structures, or when the data is not
 * excessive (so that it can easily fit in memory). In situations where the size
 * of the tree structure is unknown (and therefore potentially huge), there is
 * the option of creating TreeItem instances on-demand in a memory-efficient way.
 * To demonstrate this, the code below creates a file system browser:
 *
 * <pre><code>
 *  private TreeView buildFileSystemBrowser() {
 *      TreeItem&lt;File&gt; root = createNode(new File("/"));
 *      return new TreeView&lt;File&gt;(root);
 *  }
 *
 *  // This method creates a TreeItem to represent the given File. It does this
 *  // by overriding the TreeItem.getChildren() and TreeItem.isLeaf() methods
 *  // anonymously, but this could be better abstracted by creating a
 *  // 'FileTreeItem' subclass of TreeItem. However, this is left as an exercise
 *  // for the reader.
 *  private TreeItem&lt;File&gt; createNode(final File f) {
 *      return new TreeItem&lt;File&gt;(f) {
 *          // We cache whether the File is a leaf or not. A File is a leaf if
 *          // it is not a directory and does not have any files contained within
 *          // it. We cache this as isLeaf() is called often, and doing the
 *          // actual check on File is expensive.
 *          private boolean isLeaf;
 *
 *          // We do the children and leaf testing only once, and then set these
 *          // booleans to false so that we do not check again during this
 *          // run. A more complete implementation may need to handle more
 *          // dynamic file system situations (such as where a folder has files
 *          // added after the TreeView is shown). Again, this is left as an
 *          // exercise for the reader.
 *          private boolean isFirstTimeChildren = true;
 *          private boolean isFirstTimeLeaf = true;
 *
 *          &#064;Override public ObservableList&lt;TreeItem&lt;File&gt;&gt; getChildren() {
 *              if (isFirstTimeChildren) {
 *                  isFirstTimeChildren = false;
 *
 *                  // First getChildren() call, so we actually go off and
 *                  // determine the children of the File contained in this TreeItem.
 *                  super.getChildren().setAll(buildChildren(this));
 *              }
 *              return super.getChildren();
 *          }
 *
 *          &#064;Override public boolean isLeaf() {
 *              if (isFirstTimeLeaf) {
 *                  isFirstTimeLeaf = false;
 *                  File f = (File) getValue();
 *                  isLeaf = f.isFile();
 *              }
 *
 *              return isLeaf;
 *          }
 *
 *          private ObservableList&lt;TreeItem&lt;File&gt;&gt; buildChildren(TreeItem&lt;File&gt; TreeItem) {
 *              File f = TreeItem.getValue();
 *              if (f != null &amp;&amp; f.isDirectory()) {
 *                  File[] files = f.listFiles();
 *                  if (files != null) {
 *                      ObservableList&lt;TreeItem&lt;File&gt;&gt; children = FXCollections.observableArrayList();
 *
 *                      for (File childFile : files) {
 *                          children.add(createNode(childFile));
 *                      }
 *
 *                      return children;
 *                  }
 *              }
 *
 *              return FXCollections.emptyObservableList();
 *          }
 *      };
 *  }</code></pre>
 *
 * <strong>TreeItem Events</strong>
 * <p>TreeItem supports the same event bubbling concept as elsewhere in the
 * scenegraph. This means that it is not necessary to listen for events on all
 * TreeItems (and this is certainly not encouraged!). A better, and far more low
 * cost solution is to instead attach event listeners to the TreeView
 * {@link TreeView#rootProperty() root} item. As long as there is a path between
 * where the event occurs and the root TreeItem, the event will be bubbled to the
 * root item.
 *
 * <p>It is important to note however that a TreeItem is <strong>not</strong> a
 * Node, which means that only the event types defined in TreeItem will be
 * delivered. To listen to general events (for example mouse interactions), it is
 * necessary to add the necessary listeners to the {@link Cell cells} contained
 * within the TreeView (by providing a {@link TreeView#cellFactoryProperty()
 * cell factory}).
 *
 * <p>The TreeItem class defines a number of events, with a defined hierarchy. These
 * are shown below (follow the links to learn more about each event type):
 *
 * <ul>
 *   <li>{@link TreeItem#treeNotificationEvent() TreeItem.treeNotificationEvent()}
 *   <ul>
 *     <li>{@link TreeItem#valueChangedEvent() TreeItem.valueChangedEvent()}</li>
 *     <li>{@link TreeItem#graphicChangedEvent() TreeItem.graphicChangedEvent()}</li>
 *     <li>{@link TreeItem#expandedItemCountChangeEvent() TreeItem.expandedItemCountChangeEvent()}
 *     <ul>
 *       <li>{@link TreeItem#branchExpandedEvent() TreeItem.branchExpandedEvent()}</li>
 *       <li>{@link TreeItem#branchCollapsedEvent() TreeItem.branchCollapsedEvent()}</li>
 *       <li>{@link TreeItem#childrenModificationEvent() TreeItem.childrenModificationEvent()}</li>
 *     </ul>
 *     </li>
 *   </ul>
 *   </li>
 * </ul>
 *
 * <p>The indentation shown above signifies the relationship between event types.
 * For example, all TreeItem event types have
 * {@link TreeItem#treeNotificationEvent() treeNotificationEvent()} as their
 * parent event type, and the branch
 * {@link TreeItem#branchExpandedEvent() expand} /
 * {@link TreeItem#branchCollapsedEvent() collapse} event types are both
 * {@link TreeItem#treeNotificationEvent() treeNotificationEvent()}. For
 * performance reasons, it is encouraged to listen
 * to only the events you need to listen to. This means that it is encouraged
 * that it is better to listen to, for example,
 * {@link TreeItem#valueChangedEvent() TreeItem.valueChangedEvent()},
 * rather than {@link TreeItem#treeNotificationEvent() TreeItem.treeNotificationEvent()}.
 *
 * @param <T> The type of the {@link #getValue() value} property within TreeItem.
 * @since JavaFX 2.0
 * @see TreeView
 */
public class TreeItem<T> implements EventTarget { //, Comparable<TreeItem<T>> {

    /* *************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * The base EventType used to indicate that an event has occurred within a
     * TreeItem. When an event occurs in a TreeItem, the event is fired to any
     * listeners on the TreeItem that the event occurs, before it 'bubbles' up the
     * TreeItem chain by following the TreeItem parent property. This repeats
     * until a TreeItem whose parent TreeItem is null is reached At this point
     * the event stops 'bubbling' and goes no further. This means that events
     * that occur on a TreeItem can be relatively cheap, as a listener needs only
     * be installed on the TreeView root node to be alerted of events happening
     * at any point in the tree.
     *
     * @param <T> The type of the value contained within the TreeItem.
     * @return the base EventType when an event has occurred within a TreeItem
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> treeNotificationEvent() {
        return (EventType<TreeModificationEvent<T>>) TREE_NOTIFICATION_EVENT;
    }
    private static final EventType<?> TREE_NOTIFICATION_EVENT
            = new EventType<>(Event.ANY, "TreeNotificationEvent");

    /**
     * The general EventType used when the TreeItem receives a modification that
     * results in the number of children being visible changes.
     * This is normally achieved via one of the sub-types of this
     * EventType (see {@link #branchExpandedEvent()},
     * {@link #branchCollapsedEvent()} and {@link #childrenModificationEvent()}
     * for the three sub-types).
     *
     * @param <T> The type of the value contained within the TreeItem.
     * @return The general EventType when the TreeItem receives a modification
     * @since JavaFX 8.0
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> expandedItemCountChangeEvent() {
        return (EventType<TreeModificationEvent<T>>) EXPANDED_ITEM_COUNT_CHANGE_EVENT;
    }
    private static final EventType<?> EXPANDED_ITEM_COUNT_CHANGE_EVENT
            = new EventType<>(treeNotificationEvent(), "ExpandedItemCountChangeEvent");

    /**
     * An EventType used when the TreeItem receives a modification to its
     * expanded property, such that the TreeItem is now in the expanded state.
     *
     * @param <T> The type of the value contained within the TreeItem.
     * @return The EventType used when the TreeItem receives a modification
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> branchExpandedEvent() {
        return (EventType<TreeModificationEvent<T>>) BRANCH_EXPANDED_EVENT;
    }
    private static final EventType<?> BRANCH_EXPANDED_EVENT
            = new EventType<>(expandedItemCountChangeEvent(), "BranchExpandedEvent");

    /**
     * An EventType used when the TreeItem receives a modification to its
     * expanded property, such that the TreeItem is now in the collapsed state.
     *
     * @param <T> The type of the value contained within the TreeItem.
     * @return The EventType when the TreeItem receives a modification
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> branchCollapsedEvent() {
        return (EventType<TreeModificationEvent<T>>) BRANCH_COLLAPSED_EVENT;
    }
    private static final EventType<?> BRANCH_COLLAPSED_EVENT
            = new EventType<>(expandedItemCountChangeEvent(), "BranchCollapsedEvent");

    /**
     * An EventType used when the TreeItem receives a direct modification to its
     * children list.
     *
     * @param <T> The type of the value contained within the TreeItem.
     * @return The EventType when the TreeItem receives a direct modification to
     * its children list
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> childrenModificationEvent() {
        return (EventType<TreeModificationEvent<T>>) CHILDREN_MODIFICATION_EVENT;
    }
    private static final EventType<?> CHILDREN_MODIFICATION_EVENT
            = new EventType<>(expandedItemCountChangeEvent(), "ChildrenModificationEvent");

    /**
     * An EventType used when the TreeItem receives a modification to its
     * value property.
     *
     * @param <T> The type of the value contained within the TreeItem.
     * @return The EventType when the TreeItem receives a modification to its
     * value property
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> valueChangedEvent() {
        return (EventType<TreeModificationEvent<T>>) VALUE_CHANGED_EVENT;
    }
    private static final EventType<?> VALUE_CHANGED_EVENT
            = new EventType<>(treeNotificationEvent(), "ValueChangedEvent");

    /**
     * An EventType used when the TreeItem receives a modification to its
     * graphic property.
     *
     * @param <T> The type of the value contained within the TreeItem.
     * @return The EventType when the TreeItem receives a modification to its
     * graphic property
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> graphicChangedEvent() {
        return (EventType<TreeModificationEvent<T>>) GRAPHIC_CHANGED_EVENT;
    }
    private static final EventType<?> GRAPHIC_CHANGED_EVENT
            = new EventType<>(treeNotificationEvent(), "GraphicChangedEvent");



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates an empty TreeItem.
     */
    public TreeItem() {
        this(null);
    }

    /**
     * Creates a TreeItem with the value property set to the provided object.
     *
     * @param value The object to be stored as the value of this TreeItem.
     */
    public TreeItem(final T value) {
        this(value, (Node)null);
    }

    /**
     * Creates a TreeItem with the value property set to the provided object, and
     * the graphic set to the provided Node.
     *
     * @param value The object to be stored as the value of this TreeItem.
     * @param graphic The Node to show in the TreeView next to this TreeItem.
     */
    public TreeItem(final T value, final Node graphic) {
        setValue(value);
        setGraphic(graphic);

        addEventHandler(TreeItem.<Object>expandedItemCountChangeEvent(), itemListener);
    }

    private final EventHandler<TreeModificationEvent<Object>> itemListener =
        new EventHandler<>() {
            @Override public void handle(TreeModificationEvent<Object> event) {
                expandedDescendentCountDirty = true;
            }
    };


    /* *************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    private boolean ignoreSortUpdate = false;

    private boolean expandedDescendentCountDirty = true;

    // The ObservableList containing all children belonging to this TreeItem.
    // It is important that interactions with this list go directly into the
    // children property, rather than via getChildren(), as this may be
    // a very expensive call.
    ObservableList<TreeItem<T>> children;

    // Made static based on findings of RT-18344 - EventHandlerManager is an
    // expensive class and should be reused amongst classes if at all possible.
    private final EventHandlerManager eventHandlerManager =
            new EventHandlerManager(this);


    // Rather than have the TreeView need to (pretty well) constantly determine
    // the expanded descendent count of a TreeItem, we instead cache it locally
    // based on tree item modification events.
    private int expandedDescendentCount = 1;

    // we record the previous value also, so that we can easily determine how
    // many items just disappeared on a TreeItem collapse event. Note that the
    // actual number of items that disappeared is one less than this value,
    // because we obviously are also counting this node, which hasn't disappeared
    // when all children are collapsed.
    int previousExpandedDescendentCount = 1;

    Comparator<TreeItem<T>> lastComparator = null;
    TreeSortMode lastSortMode = null;

    // Refer to the TreeItem.updateChildrenParent method below for more context
    // and a description of this field
    private int parentLinkCount = 0;



    /* *************************************************************************
     *                                                                         *
     * Callbacks and events                                                    *
     *                                                                         *
     **************************************************************************/

    // called whenever the contents of the children sequence changes
    private ListChangeListener<TreeItem<T>> childrenListener = c -> {
        expandedDescendentCountDirty = true;
        updateChildren(c);
    };



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- Value
    private ObjectProperty<T> value;

    /**
     * Sets the application-specific data represented by this TreeItem.
     * @param value the application-specific data
     */
    public final void setValue(T value) { valueProperty().setValue(value); }

    /**
     * Returns the application-specific data represented by this TreeItem.
     * @return the data represented by this TreeItem
     */
    public final T getValue() { return value == null ? null : value.getValue(); }

    /**
     * A property representing the application-specific data contained within
     * this TreeItem.
     * @return the property representing the application-specific data contained
     * within this TreeItem
     */
    public final ObjectProperty<T> valueProperty() {
        if (value == null) {
            value = new ObjectPropertyBase<>() {
                @Override protected void invalidated() {
                    fireEvent(new TreeModificationEvent<>(VALUE_CHANGED_EVENT, TreeItem.this, get()));
                }

                @Override public Object getBean() {
                    return TreeItem.this;
                }

                @Override public String getName() {
                    return "value";
                }
            };
        }
        return value;
    }


    // --- Graphic
    private ObjectProperty<Node> graphic;

    /**
     * Sets the node that is generally shown to the left of the value property.
     * For best effect, this tends to be a 16x16 image.
     *
     * @param value The graphic node that will be displayed to the user.
     */
    public final void setGraphic(Node value) { graphicProperty().setValue(value); }

    /**
     * Returns the node that is generally shown to the left of the value property.
     * For best effect, this tends to be a 16x16 image.
     *
     * @return The graphic node that will be displayed to the user.
     */
    public final Node getGraphic() { return graphic == null ? null : graphic.getValue(); }

    /**
     * The node that is generally shown to the left of the value property. For
     * best effect, this tends to be a 16x16 image.
     * @return The node that is generally shown to the left of the value property
     */
    public final ObjectProperty<Node> graphicProperty() {
        if (graphic == null) {
            graphic = new ObjectPropertyBase<>() {
                @Override protected void invalidated() {
                    fireEvent(new TreeModificationEvent<>(GRAPHIC_CHANGED_EVENT, TreeItem.this));
                }

                @Override
                public Object getBean() {
                    return TreeItem.this;
                }

                @Override
                public String getName() {
                    return "graphic";
                }
            };
        }
        return graphic;
    }


    // --- Expanded
    private BooleanProperty expanded;

    /**
     * Sets the expanded state of this TreeItem. This has no effect on a TreeItem
     * with no children. On a TreeItem with children however, the result of
     * toggling this property is that visually the children will either become
     * visible or hidden, based on whether expanded is set to true or false.
     *
     * @param value If this TreeItem has children, calling setExpanded with
     *      <code>true</code> will result in the children becoming visible.
     *      Calling setExpanded with <code>false</code> will hide any children
     *      belonging to the TreeItem.
     */
    public final void setExpanded(boolean value) {
        if (! value && expanded == null) return;
        expandedProperty().setValue(value);
    }

    /**
     * Returns the expanded state of this TreeItem.
     *
     * @return Returns the expanded state of this TreeItem.
     */
    public final boolean isExpanded() { return expanded == null ? false : expanded.getValue(); }

    /**
     * The expanded state of this TreeItem.
     * @return The expanded state property of this TreeItem
     */
    public final BooleanProperty expandedProperty() {
        if (expanded == null) {
            expanded = new BooleanPropertyBase() {
                @Override protected void invalidated() {
                    // We don't fire expanded events for leaf nodes (RT-32620)
                    if (isLeaf()) return;

                    EventType<?> evtType = isExpanded() ?
                        BRANCH_EXPANDED_EVENT : BRANCH_COLLAPSED_EVENT;

                    fireEvent(new TreeModificationEvent<>(evtType, TreeItem.this, isExpanded()));
                }

                @Override
                public Object getBean() {
                    return TreeItem.this;
                }

                @Override
                public String getName() {
                    return "expanded";
                }
            };
        }
        return expanded;
    }


    // --- Leaf
    private ReadOnlyBooleanWrapper leaf;
    private void setLeaf(boolean value) {
        if (value && leaf == null) {
            return;
        } else if (leaf == null) {
            leaf = new ReadOnlyBooleanWrapper(this, "leaf", true);
        }
        leaf.setValue(value);
    }

    /**
     * A TreeItem is a leaf if it has no children. The isLeaf method may of
     * course be overridden by subclasses to support alternate means of defining
     * how a TreeItem may be a leaf, but the general premise is the same: a
     * leaf can not be expanded by the user, and as such will not show a
     * disclosure node or respond to expansion requests.
     * @return true if this TreeItem has no children
     */
    public boolean isLeaf() { return leaf == null ? true : leaf.getValue(); }

    /**
     * Represents the TreeItem leaf property, which is true if the TreeItem has no children.
     * @return the TreeItem leaf property
     */
    public final ReadOnlyBooleanProperty leafProperty() {
        if (leaf == null) {
            leaf = new ReadOnlyBooleanWrapper(this, "leaf", true);
        }
        return leaf.getReadOnlyProperty();
    }


    // --- Parent
    private ReadOnlyObjectWrapper<TreeItem<T>> parent = new ReadOnlyObjectWrapper<>(this, "parent");
    private void setParent(TreeItem<T> value) { parent.setValue(value); }

    /**
     * The parent of this TreeItem. Each TreeItem can have no more than one
     * parent. If a TreeItem has no parent, it represents a root in the tree model.
     *
     * @return The parent of this TreeItem, or null if the TreeItem has no parent.
     */
    public final TreeItem<T> getParent() { return parent == null ? null : parent.getValue(); }

    /**
     * A property that represents the parent of this TreeItem.
     * @return the parent property of this TreeItem
     */
    public final ReadOnlyObjectProperty<TreeItem<T>> parentProperty() { return parent.getReadOnlyProperty(); }



    /* *********************************************************************
     *                                                                     *
     * TreeItem API                                                        *
     *                                                                     *
     **********************************************************************/

    /**
     * The children of this TreeItem. This method is called frequently, and
     * it is therefore recommended that the returned list be cached by
     * any TreeItem implementations.
     *
     * @return a list that contains the child TreeItems belonging to the TreeItem.
     */
    public ObservableList<TreeItem<T>> getChildren() {
        if (children == null) {
            children = FXCollections.observableArrayList();
            children.addListener(childrenListener);
        }

        // we need to check if this TreeItem needs to have its children sorted.
        // There are two different ways that this could be possible.
        if (children.isEmpty()) return children;

        // checkSortState should in almost all instances be called, but there
        // are situations where checking the sort state will result in
        // unwanted permutation events being fired (if a sort is applied). To
        // avoid this (which resolves RT-37593), we set the ignoreSortUpdate
        // to true (and of course, we're careful to set it back to false again)
        if (!ignoreSortUpdate) {
            checkSortState();
        }

        return children;
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns the previous sibling of the TreeItem. Ordering is based on the
     * position of the TreeItem relative to its siblings in the children
     * list belonging to the parent of the TreeItem.
     *
     * @return A TreeItem that is the previous sibling of the current TreeItem,
     *      or null if no such sibling can be found.
     */
    public TreeItem<T> previousSibling() {
        return previousSibling(this);
    }

    /**
     * Returns the previous sibling after the given node. Ordering is based on the
     * position of the given TreeItem relative to its siblings in the children
     * list belonging to the parent of the TreeItem.
     *
     * @param beforeNode The TreeItem for which the previous sibling is being
     *      sought.
     * @return A TreeItem that is the previous sibling of the given TreeItem,
     *      or null if no such sibling can be found.
     */
    public TreeItem<T> previousSibling(final TreeItem<T> beforeNode) {
        if (getParent() == null || beforeNode == null) {
            return null;
        }

        List<TreeItem<T>> parentChildren = getParent().getChildren();
        final int childCount = parentChildren.size();
        int pos = -1;
        for (int i = 0; i < childCount; i++) {
            if (beforeNode.equals(parentChildren.get(i))) {
                pos = i - 1;
                return pos < 0 ? null : parentChildren.get(pos);
            }
        }
        return null;
    }

    /**
     * Returns the next sibling of the TreeItem. Ordering is based on the
     * position of the TreeItem relative to its siblings in the children
     * list belonging to the parent of the TreeItem.
     *
     * @return A TreeItem that is the next sibling of the current TreeItem,
     *      or null if no such sibling can be found.
     */
    public TreeItem<T> nextSibling() {
        return nextSibling(this);
    }

    /**
     * Returns the next sibling after the given node. Ordering is based on the
     * position of the given TreeItem relative to its siblings in the children
     * list belonging to the parent of the TreeItem.
     *
     * @param afterNode The TreeItem for which the next sibling is being
     *      sought.
     * @return A TreeItem that is the next sibling of the given TreeItem,
     *      or null if no such sibling can be found.
     */
    public TreeItem<T> nextSibling(final TreeItem<T> afterNode) {
        if (getParent() == null || afterNode == null) {
            return null;
        }

        List<TreeItem<T>> parentChildren = getParent().getChildren();
        final int childCount = parentChildren.size();
        int pos = -1;
        for (int i = 0; i < childCount; i++) {
            if (afterNode.equals(parentChildren.get(i))) {
                pos = i + 1;
                return pos >= childCount ? null : parentChildren.get(pos);
            }
        }
        return null;
    }

    /**
     * Returns a string representation of this {@code TreeItem} object.
     * @return a string representation of this {@code TreeItem} object.
     */
    @Override public String toString() {
        return "TreeItem [ value: " + getValue() + " ]";
    }

    private void fireEvent(TreeModificationEvent<T> evt) {
        Event.fireEvent(this, evt);
    }




    /* *************************************************************************
     *                                                                         *
     * Event Target Implementation / API                                       *
     *                                                                         *
     **************************************************************************/

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        // To allow for a TreeView (and its skin) to be notified of changes in the
        // tree, this method recursively calls up to the root node, at which point
        // it fires a ROOT_NOTIFICATION_EVENT, which the TreeView may be watching for.
        if (getParent() != null) {
            getParent().buildEventDispatchChain(tail);
        }
        return tail.append(eventHandlerManager);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The {@code TreeItem} class allows registration of listeners which will be notified as the number of items
     * changes, their position, or if the values themselves change. Note that {@code TreeItem} is <b>not</b> a
     * {@link Node}, and therefore no visual events will be fired on it. To get these events, it is necessary to
     * add relevant observers to the {@code TreeCell} instances via a custom cell factory (see the {@link Cell}
     * class documentation for more details).
     */
    @Override
    public <E extends Event> void addEventHandler(EventType<E> eventType, EventHandler<? super E> eventHandler) {
        eventHandlerManager.addEventHandler(eventType, eventHandler);
    }

    @Override
    public <E extends Event> void removeEventHandler(EventType<E> eventType, EventHandler<? super E> eventHandler) {
        eventHandlerManager.removeEventHandler(eventType, eventHandler);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The {@code TreeItem} class allows registration of listeners which will be notified as the number of items
     * changes, their position, or if the values themselves change. Note that {@code TreeItem} is <b>not</b> a
     * {@link Node}, and therefore no visual events will be fired on it. To get these events, it is necessary to
     * add relevant observers to the {@code TreeCell} instances via a custom cell factory (see the {@link Cell}
     * class documentation for more details).
     */
    @Override
    public <E extends Event> void addEventFilter(EventType<E> eventType, EventHandler<? super E> eventHandler) {
        eventHandlerManager.addEventFilter(eventType, eventHandler);
    }

    @Override
    public <E extends Event> void removeEventFilter(EventType<E> eventType, EventHandler<? super E> eventHandler) {
        eventHandlerManager.removeEventFilter(eventType, eventHandler);
    }

    /* *************************************************************************
     *                                                                         *
     * private methods                                                         *
     *                                                                         *
     **************************************************************************/

    void sort() {
        sort(children, lastComparator, lastSortMode);
    }

    private void sort(final ObservableList<TreeItem<T>> children,
                         final Comparator<TreeItem<T>> comparator,
                         final TreeSortMode sortMode) {

        if (comparator == null) return;

        runSort(children, comparator, sortMode);

        // if we're at the root node, we'll fire an event so that the control
        // can update its display
        if (getParent() == null) {
            TreeModificationEvent<T> e = new TreeModificationEvent<>(TreeItem.childrenModificationEvent(), this);
            e.wasPermutated = true;
            fireEvent(e);
        }
    }

    private void checkSortState() {
        TreeItem<T> rootNode = getRoot();

        TreeSortMode sortMode = rootNode.lastSortMode;
        Comparator<TreeItem<T>> comparator = rootNode.lastComparator;

        if (comparator != null && comparator != lastComparator) {
            lastComparator = comparator;
            runSort(children, comparator, sortMode);
        }
    }

    private void runSort(ObservableList<TreeItem<T>> children, Comparator<TreeItem<T>> comparator, TreeSortMode sortMode) {
        if (sortMode == ALL_DESCENDANTS) {
            doSort(children, comparator);
        } else if (sortMode == ONLY_FIRST_LEVEL) {
            // if we are here we presume that the current node is the root node
            // (but we can test to see if getParent() returns null to be sure).
            // We also know that ONLY_FIRST_LEVEL only applies to the children
            // of the root, so we return straight after we sort these children.
            if (getParent() == null) {
                doSort(children, comparator);
            }
//        } else if (sortMode == ONLY_LEAVES) {
//            if (isLeaf()) {
//                // sort the parent once
//            }
//        } else if (sortMode == ALL_BUT_LEAVES) {
//
        } else {
            // Unknown sort mode
        }
    }

    private TreeItem<T> getRoot() {
        TreeItem<T> parent = getParent();
        if (parent == null) return this;

        while (true) {
            TreeItem<T> newParent = parent.getParent();
            if (newParent == null) return parent;
            parent = newParent;
        }
    }

    private void doSort(ObservableList<TreeItem<T>> children, final Comparator<TreeItem<T>> comparator) {
        if (!isLeaf() && isExpanded()) {
            FXCollections.sort(children, comparator);
        }
    }

    // This value is package accessible so that it may be retrieved from TreeView.
    int getExpandedDescendentCount(boolean reset) {
        if (reset || expandedDescendentCountDirty) {
            updateExpandedDescendentCount(reset);
            expandedDescendentCountDirty = false;
        }
        return expandedDescendentCount;
    }

    private void updateExpandedDescendentCount(boolean reset) {
        previousExpandedDescendentCount = expandedDescendentCount;
        expandedDescendentCount = 1;

        ignoreSortUpdate = true;
        if (!isLeaf() && isExpanded()) {
            for (TreeItem<T> child : getChildren()) {
                if (child == null) continue;
                expandedDescendentCount += child.isExpanded() ? child.getExpandedDescendentCount(reset) : 1;
            }
        }
        ignoreSortUpdate = false;
    }

    private void updateChildren(ListChangeListener.Change<? extends TreeItem<T>> c) {
        setLeaf(children.isEmpty());

        final List<TreeItem<T>> added = new ArrayList<>();
        final List<TreeItem<T>> removed = new ArrayList<>();

        while (c.next()) {
            added.addAll(c.getAddedSubList());
            removed.addAll(c.getRemoved());
        }

        // update the relationships such that all added children point to
        // this node as the parent (and all removed children point to null)
        updateChildrenParent(removed, null);
        updateChildrenParent(added, this);

        c.reset();

        // fire an event up the parent hierarchy such that any listening
        // TreeViews (which only listen to their root node) can redraw
        fireEvent(new TreeModificationEvent<>(
                CHILDREN_MODIFICATION_EVENT, this, added, removed, c));
    }

    // Convenience method to set the parent of all children in the given list to
    // the given parent TreeItem
    private static <T> void updateChildrenParent(List<? extends TreeItem<T>> treeItems, final TreeItem<T> newParent) {
        if (treeItems == null) return;
        for (final TreeItem<T> treeItem : treeItems) {
            if (treeItem == null) continue;

            TreeItem<T> currentParent = treeItem.getParent();

            // We only replace the parent if the parentLinkCount of the given
            // TreeItem is zero (which indicates that this TreeItem has not been
            // 'linked' to its parent multiple times). This can happen in
            // situations such as what is shown in RT-28668 (and tested for in
            // TreeViewTest.test_rt28556()). Specifically, when a sort is applied
            // to the children of a TreeItem, it is possible for them to be
            // sorted in such a way that the element is considered to be
            // added in multiple places in the child list and then removed from
            // one of those places subsequently. In doing this final removal,
            // the parent of that TreeItem is set to null when it should in fact
            // remain with the parent that it belongs to.
            if (treeItem.parentLinkCount == 0) {
                treeItem.setParent(newParent);
            }

            boolean parentMatch = currentParent != null && currentParent.equals(newParent);
            if (parentMatch) {
                if (newParent == null) {
                    treeItem.parentLinkCount--;
                } else {
                    treeItem.parentLinkCount++;
                }
            }
         }
    }

    /**
     * An {@link Event} that contains relevant information for all forms of
     * TreeItem modifications.
     * @param <T> The TreeModificationEvent
     * @since JavaFX 2.0
     */
    public static class TreeModificationEvent<T> extends Event {
        private static final long serialVersionUID = 4741889985221719579L;

        /**
         * Common supertype for all tree modification event types.
         * @since JavaFX 8.0
         */
        public static final EventType<?> ANY = TREE_NOTIFICATION_EVENT;

        private transient final TreeItem<T> treeItem;
        private final T newValue;

        private final List<? extends TreeItem<T>> added;
        private final List<? extends TreeItem<T>> removed;
        private final ListChangeListener.Change<? extends TreeItem<T>> change;

        private final boolean wasExpanded;
        private final boolean wasCollapsed;
        private boolean wasPermutated;

        /**
         * Constructs a basic TreeModificationEvent - this is useful in situations
         * where the tree item has not received a new value, has not changed
         * between expanded/collapsed states, and whose children has not changed.
         * An example of when this constructor is used is when the TreeItem
         * graphic property changes.
         *
         * @param eventType The type of the event that has occurred.
         * @param treeItem The TreeItem on which this event occurred.
         */
        public TreeModificationEvent(EventType<? extends Event> eventType, TreeItem<T> treeItem) {
            this (eventType, treeItem, null);
        }

        /**
         * Constructs a TreeModificationEvent for when the TreeItem has had its
         * {@link TreeItem#valueProperty()} changed.
         *
         * @param eventType The type of the event that has occurred.
         * @param treeItem The TreeItem on which this event occurred.
         * @param newValue The new value that has been put into the
         *      {@link TreeItem#valueProperty()}.
         */
        public TreeModificationEvent(EventType<? extends Event> eventType,
                TreeItem<T> treeItem, T newValue) {
            super(eventType);
            this.treeItem = treeItem;
            this.newValue = newValue;
            this.added = null;
            this.removed = null;
            this.change = null;
            this.wasExpanded = false;
            this.wasCollapsed = false;
        }

        /**
         * Constructs a TreeModificationEvent for when the TreeItem has had its
         * {@link TreeItem#expandedProperty()} changed.
         *
         * @param eventType The type of the event that has occurred.
         * @param treeItem The TreeItem on which this event occurred.
         * @param expanded A boolean to represent the current expanded
         *      state of the TreeItem.
         */
        public TreeModificationEvent(EventType<? extends Event> eventType,
                TreeItem<T> treeItem, boolean expanded) {
            super(eventType);
            this.treeItem = treeItem;
            this.newValue = null;
            this.added = null;
            this.removed = null;
            this.change = null;
            this.wasExpanded = expanded;
            this.wasCollapsed = ! expanded;
        }

        /**
         * Constructs a TreeModificationEvent for when the TreeItem has had its
         * children list changed.
         *
         * @param eventType The type of the event that has occurred.
         * @param treeItem The TreeItem on which this event occurred.
         * @param added A list of the items added to the children list of the
         *      given TreeItem.
         * @param removed A list of the items removed from the children list of
         *      the given TreeItem.
         */
        public TreeModificationEvent(EventType<? extends Event> eventType,
                                     TreeItem<T> treeItem,
                                     List<? extends TreeItem<T>> added,
                                     List<? extends TreeItem<T>> removed) {
            this(eventType, treeItem, added, removed, null);
        }

        /**
         * Constructs a TreeModificationEvent for when the TreeItem has had its
         * children list changed, including the
         * {@link javafx.collections.ListChangeListener.Change} that has taken place.
         *
         * @param eventType The type of the event that has occurred.
         * @param treeItem The TreeItem on which this event occurred.
         * @param added A list of the items added to the children list of the
         *      given TreeItem.
         * @param removed A list of the items removed from the children list of
         *      the given TreeItem.
         * @param change The actual change that has taken place on the children list.
         */
        private TreeModificationEvent(EventType<? extends Event> eventType,
                                     TreeItem<T> treeItem,
                                     List<? extends TreeItem<T>> added,
                                     List<? extends TreeItem<T>> removed,
                                     ListChangeListener.Change<? extends TreeItem<T>> change) {
            super(eventType);
            this.treeItem = treeItem;
            this.newValue = null;
            this.added = added;
            this.removed = removed;
            this.change = change;
            this.wasExpanded = false;
            this.wasCollapsed = false;

            this.wasPermutated = added != null && removed != null &&
                                 added.size() == removed.size() &&
                                 added.containsAll(removed);
        }

        /**
         * Returns the TreeItem upon which this event occurred.
         * @since JavaFX 2.1
         */
        @Override public TreeItem<T> getSource() {
            return this.treeItem;
        }

        /**
         * Returns the TreeItem that this event occurred upon.
         * @return The TreeItem that this event occurred upon.
         */
        public TreeItem<T> getTreeItem() {
            return treeItem;
        }

        /**
         * If the value of the TreeItem changed, this method will return the new
         * value. If it did not change, this method will return null.
         * @return The new value of the TreeItem if it changed, null otherwise.
         */
        public T getNewValue() {
            return newValue;
        }

        /**
         * Returns the children added to the TreeItem in this event, or an empty
         * list if no children were added.
         * @return The newly added children, or an empty list if no children
         *      were added.
         */
        public List<? extends TreeItem<T>> getAddedChildren() {
            return added == null ? Collections.<TreeItem<T>>emptyList() : added;
        }

        /**
         * Returns the children removed from the TreeItem in this event, or an
         * empty list if no children were added.
         * @return The removed children, or an empty list if no children
         *      were removed.
         */
        public List<? extends TreeItem<T>> getRemovedChildren() {
            return removed == null ? Collections.<TreeItem<T>>emptyList() : removed;
        }

        /**
         * Returns the number of children items that were removed in this event,
         * or zero if no children were removed.
         * @return The number of removed children items, or zero if no children
         *      were removed.
         */
        public int getRemovedSize() {
            return getRemovedChildren().size();
        }

        /**
         * Returns the number of children items that were added in this event,
         * or zero if no children were added.
         * @return The number of added children items, or zero if no children
         *      were added.
         */
        public int getAddedSize() {
            return getAddedChildren().size();
        }

        /**
         * Returns true if this event represents a TreeItem expansion event,
         * and false if the TreeItem was not expanded.
         * @return true if this event represents a TreeItem expansion event,
         * and false if the TreeItem was not expanded
         */
        public boolean wasExpanded() { return wasExpanded; }

        /**
         * Returns true if this event represents a TreeItem collapse event,
         * and false if the TreeItem was not collapsed.
         * @return true if this event represents a TreeItem collapse event,
         * and false if the TreeItem was not collapsed
         */
        public boolean wasCollapsed() { return wasCollapsed; }

        /**
         * Returns true if this event represents a TreeItem event where children
         * TreeItems were added.
         * @return true if this event represents a TreeItem event where children
         * TreeItems were added
         */
        public boolean wasAdded() { return getAddedSize() > 0; }

        /**
         * Returns true if this event represents a TreeItem event where children
         * TreeItems were removed.
         * @return true if this event represents a TreeItem event where children
         * TreeItems were removed
         */
        public boolean wasRemoved() { return getRemovedSize() > 0; }

        /**
         * Returns true if the order of the TreeItem children list has changed,
         * but that there have been no additions or removals.
         * @return true if the order of the TreeItem children list has changed,
         * but that there have been no additions or removals
         */
        public boolean wasPermutated() { return wasPermutated; }

        int getFrom() { return change == null ? -1 : change.getFrom(); }
        int getTo() { return change == null ? -1 : change.getTo(); }
        ListChangeListener.Change<? extends TreeItem<T>> getChange() { return change; }
    }
}
