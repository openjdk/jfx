/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

import com.sun.javafx.Logging;
import com.sun.javafx.TempState;
import com.sun.javafx.Utils;
import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.collections.VetoableObservableList;
import com.sun.javafx.collections.annotations.ReturnsUnmodifiableCollection;
import com.sun.javafx.css.Selector;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.scene.CSSFlags;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.sg.PGGroup;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

/**
 * The base class for all nodes that have children in the scene graph.
 * <p>
 * This class handles all hierarchical scene graph operations, including adding/removing
 * child nodes, marking branches dirty for layout and rendering, picking,
 * bounds calculations, and executing the layout pass on each pulse.
 * <p>
 * There are three direct concrete Parent subclasses
 * <ul>
 * <li>{@link Group} effects and transforms to be applied to a collection of child nodes.</li>
 * <li>{@link javafx.scene.layout.Region} class for nodes that can be styled with CSS and layout children. </li>
 * <li>{@link javafx.scene.control.Control base Control} class for high-level skinnable nodes designed for user interaction.</li>
 * </ul>
 *
 */
public abstract class Parent extends Node {

    private static final int DIRTY_CHILDREN_THRESHOLD = 10;

    // If set to true, generate a warning message whenever adding a node to a
    // parent if it is currently a child of another parent.
    private static final boolean warnOnAutoMove = PropertyHelper.getBooleanProperty("javafx.sg.warn");

    /**
     * Threshold when it's worth to populate list of removed children.
     */
    private static final int REMOVED_CHILDREN_THRESHOLD = 20;
    
    /**
     * Do not populate list of removed children when its number exceeds threshold,
     * but mark whole parent dirty.
     */
    private boolean removedChildrenExceedsThreshold = false;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public void impl_updatePG() {
        super.impl_updatePG();

        List<PGNode> pgnodes = getPGGroup().getChildren();
        if (Utils.assertionEnabled()) {
            if (pgnodes.size() != pgChildrenSize) {
                java.lang.System.err.println("*** pgnodes.size() [" + pgnodes.size() + "] != pgChildrenSize [" + pgChildrenSize + "]");
            }
        }

        if (impl_isDirty(DirtyBits.PARENT_CHILDREN)) {
            PGGroup peer = getPGGroup();
            // Whether a permutation, or children having been added or
            // removed, we'll want to clear out the PG side starting
            // from startIdx. We know that everything up to but not
            // including startIdx is identical between the FX and PG
            // sides, so we only need to update the remaining portion.
            peer.clearFrom(startIdx);
            for (int idx = startIdx; idx < children.size(); idx++) {
                    Node n = children.get(idx);
                    if (n != null) {
                        peer.add(idx, n.impl_getPGNode());
                    }
                }
            if (removedChildrenExceedsThreshold) {
                peer.markDirty();
                removedChildrenExceedsThreshold = false;
            } else {
                if (removed != null && !removed.isEmpty()) {
                    for(int i = 0; i < removed.size(); i++) {
                        peer.addToRemoved(removed.get(i).impl_getPGNode());
                    }
                    removed.clear();
                }
            }
            pgChildrenSize = children.size();
            startIdx = pgChildrenSize;
        }

        if (Utils.assertionEnabled()) validatePG();
    }


    /***********************************************************************
     *                        Scenegraph Structure                         *
     *                                                                     *
     *  Functions and variables related to the scenegraph structure,       *
     *  modifying the structure, and walking the structure.                *
     *                                                                     *
     **********************************************************************/

    // Used to check for duplicate nodes
    private final Set<Node> childSet = new HashSet<Node>();

    // starting child index from which we need to send the children to the PGGroup
    private int startIdx = 0;

    // double of children in the PGGroup as of the last update
    private int pgChildrenSize = 0;

    void validatePG() {
        boolean assertionFailed = false;
        List<PGNode> pgnodes = getPGGroup().getChildren();
        if (pgnodes.size() != children.size()) {
            java.lang.System.err.println("*** pgnodes.size validatePG() [" + pgnodes.size() + "] != children.size() [" + children.size() + "]");
            assertionFailed = true;
        } else {
            for (int idx = 0; idx < children.size(); idx++) {
                Node n = children.get(idx);
                if (n.getParent() != this) {
                    java.lang.System.err.println("*** this=" + this + " validatePG children[" + idx + "].parent= " + n.getParent());
                    assertionFailed = true;
                }
                if (n.impl_getPGNode() != pgnodes.get(idx)) {
                    java.lang.System.err.println("*** pgnodes[" + idx + "] validatePG != children[" + idx + "]");
                    assertionFailed = true;
                }
            }
        }
        if (assertionFailed) {
            throw new java.lang.AssertionError("validation of PGGroup children failed");
        }

    }

    void printSeq(String prefix, List<Node> nodes) {
        String str = prefix;
        for (Node nn : nodes) {
            str += nn + " ";
        }
        System.out.println(str);
    }

    /**
     * Variable used to avoid executing the body of the on replace trigger on
     * children. This is specifically used when we know that changes to the
     * children is going to be valid so as to avoid all the scenegraph surgery
     * validation routines in the trigger.
     */
    private boolean ignoreChildrenTrigger = false;

    // Variable used to indicate that the change to the children ObservableList is
    // a simple permutation as the result of a toFront or toBack operation.
    // We can avoid almost all of the processing of the on replace trigger in
    // this case.
    private boolean childrenTriggerPermutation = false;

    //accumulates all removed nodes between pulses, for dirty area calculation.
    private List<Node> removed;
    
    /**
     * A ObservableList of child {@code Node}s.
     * <p>
     * See the class documentation for {@link Node} for scene graph structure
     * restrictions on setting a {@link Parent}'s children ObservableList.
     * If these restrictions are violated by a change to the children ObservableList,
     * the change is ignored and the previous value of the child ObservableList is
     * restored.
     *
     * {@code <p>Throws AssignToBoundException} if the same node
     * appears in two different bound ObservableList.
     *
     * @defaultValue empty
     * @since JavaFX 1.3
     */
    private final ObservableList<Node> children = new VetoableObservableList<Node>() {
        // set to true if either childRemoved or childAdded returns
        // true. These functions will indicate whether the geom
        // bounds for the parent have changed
        private boolean geomChanged;
        private boolean childrenModified;

        @Override
        protected void onProposedChange(final List<Node> newNodes, int[] toBeRemoved) {
            if (ignoreChildrenTrigger) {
                return;
            }
            if (Parent.this.getScene() != null) {
                // NOTE: this will throw IllegalStateException if we are on the wrong thread
                Toolkit.getToolkit().checkFxUserThread();
            }
            geomChanged = false;

            long newLength = children.size() + newNodes.size();
            int removedLength = 0;
            for (int i = 0; i < toBeRemoved.length; i += 2) {
                removedLength += toBeRemoved[i + 1] - toBeRemoved[i];
            }
            newLength -= removedLength;

            // If the childrenTriggerPermutation flag is set, then we know it
            // is a simple permutation and no further checking is needed.
            childrenModified = false;
            if (childrenTriggerPermutation) {
                return;
            }

            // If the childrenTriggerPermutation flag is not set, then we will
            // check to see whether any element in the ObservableList has changed,
            // or whether the new ObservableList is a permutation on the existing
            // ObservableList. Note that even if the childrenModified flag is false,
            // we still have to check for duplicates. If it is a simple
            // permutation, we can avoid checking for cycles or other parents.
            childrenModified = true;
            if (newLength == childSet.size()) {
                childrenModified = false;
                for (Node n : newNodes) {
                    if (!childSet.contains(n)) {
                        childrenModified = true;
                        break;
                    }
                }
            }

            // Enforce scene graph invariants, and check for structural errors.
            //
            // 1. If a child has been added to this parent more than once,
            // then it is an error
            //
            // 2. If a child is a target of a clip, then it is an error.
            //
            // 3. If a node would cause a cycle, then it is an error.
            //
            // 4. If a node is null
            //
            // Note that if a node is the child of another parent, we will
            // implicitly remove the node from its former Parent after first
            // checking for errors.

            // iterate over the nodes that were removed and remove them from
            // the hash set.
            for (int i = 0; i < toBeRemoved.length; i += 2) {
                for (int j = toBeRemoved[i]; j < toBeRemoved[i + 1]; j++) {
                    childSet.remove(children.get(j));
                }
            }

            try {
                childSet.addAll(newNodes);
                if (childSet.size() != newLength) {
                    throw new IllegalArgumentException(
                            constructExceptionMessage(
                                "duplicate children added", null));
                }

                if (childrenModified) {
                    for (Node node : newNodes) {
                        if (node == null) {
                            throw new NullPointerException(
                                    constructExceptionMessage(
                                        "child node is null", null));
                        }
                        if (node.impl_getClipParent() != null) {
                            throw new IllegalArgumentException(
                                    constructExceptionMessage(
                                        "node already used as a clip", node));
                        }
                        if (wouldCreateCycle(Parent.this, node)) {
                            throw new IllegalArgumentException(
                                    constructExceptionMessage(
                                        "cycle detected", node));
                        }
                    }
                }
            } catch (RuntimeException e) {
                //Return children to it's original state
                childSet.clear();
                childSet.addAll(children);

                // rethrow
                throw e;
            }

            // Done with error checking

            if (!childrenModified) {
                return;
            }

            // iterate over the nodes that were removed and clear their
            // parent and scene. Add to them also to removed list for further
            // dirty regions calculation.
            if (removed == null) {
                removed = new ArrayList<Node>();
            }
            if (removed.size() + removedLength > REMOVED_CHILDREN_THRESHOLD) {
                //do not populate too many children in removed list
                removedChildrenExceedsThreshold = true;
            }
            for (int i = 0; i < toBeRemoved.length; i += 2) {
                for (int j = toBeRemoved[i]; j < toBeRemoved[i + 1]; j++) {
                    Node old = children.get(j);
                    if (dirtyChildren != null) {
                        dirtyChildren.remove(old);
                    }
                    if (old == null) {
                        continue;
                    }

                    if (!geomChanged && old.isVisible()) {
                        geomChanged = childRemoved(old);
                    }
                    if (old.getParent() == Parent.this) {
                        old.setParent(null);
                        old.setScene(null);
                    }
                    if (!removedChildrenExceedsThreshold) {
                        removed.add(old);
                    }
                }
            }
        }

        @Override
        protected void onChanged(Change<Node> c) {
            // proceed with updating the scene graph
            if (childrenModified) {
                boolean relayout = false;
                
                while (c.next()) {
                    for (Node n : c.getAddedSubList()) {
                        if (n != null && n.getParent() != null && n.getParent() != Parent.this) {
                            if (warnOnAutoMove) {
                                java.lang.System.err.println("WARNING added to a new parent without first removing it from its current");
                                java.lang.System.err.println("    parent. It will be automatically removed from its current parent.");
                                java.lang.System.err.println("    node=" + n + " oldparent= " + n.getParent() + " newparent=" + this);
                            }
                            n.getParent().children.remove(n);
                            if (n.isManaged()) {
                                relayout = true;
                            }
                            if (warnOnAutoMove) {
                                Thread.dumpStack();
                            }
                        }
                    }

                    for (Node node : c.getRemoved()) {
                        if (node != null && node.isManaged()) {
                            relayout = true;
                        }
                    }
                    
                    // update the parent and scene for each new node
                    for (Node node : c.getAddedSubList()) {
                        if (node != null) {
                            if (node.isManaged()) {
                                relayout = true;
                            }
                            node.setParent(Parent.this);
                            node.setScene(getScene());
                            if (dirtyChildren != null) {
                                dirtyChildren.add(node);
                            }
                            if (!geomChanged && node.isVisible()) {
                                geomChanged = childAdded(node);
                            }
                        }
                    }
                }

                // check to see if the number of children exceeds
                // DIRTY_CHILDREN_THRESHOLD and dirtyChildren is null.
                // If so, then we need to create dirtyChildren and
                // populate it.
                if (dirtyChildren == null && children.size() > DIRTY_CHILDREN_THRESHOLD) {
                    dirtyChildren = new LinkedHashSet<Node>(2 * children.size());
                    // only bother populating children if geom has
                    // changed, otherwise there is no need
                    if (geomChanged) {
                        for (Node ch : children) {
                            if (ch.isVisible() && ch.boundsChanged) {
                                dirtyChildren.add(ch);
                            }
                        }
                    }
                }

                if (geomChanged) {
                    impl_geomChanged();
                }

                //
                // Note that the styles of a child do not affect the parent or
                // its siblings. Thus, it is only necessary to reapply css to
                // the Node just added and not to this parent and all of its
                // children. So the following call to impl_reapplyCSS was moved
                // to Node.parentProperty. The orginal comment and code were
                // purpopsely left here as documentation should there be any
                // question about how the code used to work and why the change
                // was made.
                //
                // if children have changed then I need to reapply
                // CSS from this node on down
//                impl_reapplyCSS();
                //

                // request layout if a Group subclass has overridden doLayout OR
                // if one of the new children needs layout, in which case need to ensure
                // the needsLayout flag is set all the way to the root so the next layout
                // pass will reach the child.
                if (relayout) {
                    requestLayout();
                }
            }

            // Note the starting index at which we need to update the
            // PGGroup on the next update, and mark the children dirty
            c.reset();
            c.next();
            if (startIdx > c.getFrom()) {
                startIdx = c.getFrom();
            }

            impl_markDirty(DirtyBits.PARENT_CHILDREN);
        }

        private String constructExceptionMessage(
                String cause, Node offendingNode) {
            final StringBuilder sb = new StringBuilder("Children: ");
            sb.append(cause);
            sb.append(": parent = ").append(Parent.this);
            if (offendingNode != null) {
                sb.append(", node = ").append(offendingNode);
            }

            return sb.toString();
        }
    };

    /**
     * Gets the list of children of this {@code Parent}.
     *
     * <p>
     * See the class documentation for {@link Node} for scene graph structure
     * restrictions on setting a {@link Parent}'s children list.
     * If these restrictions are violated by a change to the list of children,
     * the change is ignored and the previous value of the children list is
     * restored. An {@link IllegalArgumentException} is thrown in this case.
     *
     * <p>
     * If this {@link Parent} node is attached to a {@link Scene}, then its
     * list of children must only be modified on the JavaFX Application Thread.
     * An {@link IllegalStateException} is thrown if this restriction is
     * violated.
     *
     * @return the list of children of this {@code Parent}.
     */
    protected ObservableList<Node> getChildren() {
        return children;
    }

    /**
     * Gets the list of children of this {@code Parent} as a read-only
     * list.
     *
     * @return read-only access to this parent's children ObservableList
     */
    @ReturnsUnmodifiableCollection
    public ObservableList<Node> getChildrenUnmodifiable() {
        return FXCollections.unmodifiableObservableList(children);
    }

    /**
     * Gets the list of all managed children of this {@code Parent}.
     *
     * @param <E> the type of the children nodes
     * @return list of all managed children in this parent
     */
    protected <E extends Node> List<E> getManagedChildren() {
        List<E> managed = new ArrayList<E>();
        for (E e : (ObservableList<E>)getChildren()) {
            if (e != null && e.isManaged()) {
                managed.add(e);
            }
        }
        return managed;
    }

    // implementation of Node.toFront function
    final void impl_toFront(Node node) {
        if (Utils.assertionEnabled()) {
            if (!childSet.contains(node)) {
                throw new java.lang.AssertionError(
                        "specified node is not in the list of children");
            }
        }

        if (children.get(children.size() - 1) != node) {
            childrenTriggerPermutation = true;
            try {
                children.remove(node);
                children.add(node);
            } finally {
                childrenTriggerPermutation = false;
            }
        }
    }

    // implementation of Node.toBack function
    final void impl_toBack(Node node) {
        if (Utils.assertionEnabled()) {
            if (!childSet.contains(node)) {
                throw new java.lang.AssertionError(
                        "specified node is not in the list of children");
            }
        }

        if (children.get(0) != node) {
            childrenTriggerPermutation = true;
            try {
                children.remove(node);
                children.add(0, node);
            } finally {
                childrenTriggerPermutation = false;
            }
        }
    }

    private void computeDirtyScene(Scene old) {
        for (Node node : getChildren()) {
            if (node != null) {
                node.setScene(getScene());
            }
        }

        if (isNeedsLayout() && old != null) {
            old.removeFromDirtyLayoutList(this);
        }
        if (getScene() != null && isNeedsLayout() && isLayoutRoot()) {
            getScene().addToDirtyLayoutList(this);
        }
    }

    @Override void impl_sceneChanged(Scene old) {
        computeDirtyScene(old);
    }

    // define focus traversal order
    // keep it package private for now
    Node getFirstChild() {
        if (children.size() > 0) {
            return children.get(0);
        } else {
            return null;
        }
    }

    Node getLastChild() {
        if (!children.isEmpty()) {
            return children.get(children.size() - 1);
        } else {
            return null;
        }
    }

    Node getNextChild(Node child) {
        Node previous = null;
        for (Node node : children)  {
            if (child == previous) {
                return node;
            }
            previous = node;
        }
        return null;
    }

    Node getPreviousChild(Node child) {
        Node previous = null;
        for (int i = (children.size()-1); i >= 0; i--) {
            Node node = children.get(i);
            if (child == previous) {
                return node;
            }
            previous = node;
        }
        return null;
    }

    @Override
    void setDerivedDepthTest(boolean value) {
        super.setDerivedDepthTest(value);

        ObservableList<Node> myChildren = getChildren();
        for (int i = 0; i < myChildren.size(); i++) {
            Node node = myChildren.get(i);
            node.computeDerivedDepthTest();
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected Node impl_pickNodeLocal(double localX, double localY) {
        if (containsBounds(localX, localY)) {
            for (int i = children.size()-1; i >= 0; i--) {
                Node picked = children.get(i).impl_pickNode(localX, localY);
                if (picked != null) {
                    return picked;
                }
            }
        }
        return null;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected Node impl_pickNodeLocal(PickRay pickRay) {
            for (int i = children.size()-1; i >= 0; i--) {
                Node picked = children.get(i).impl_pickNode(pickRay);

                if (picked != null) {
                    return picked;
                }
            }
        return null;
    }

    @Override boolean isConnected() {
        return super.isConnected() || isSceneRoot();
    }

    @Override public Node lookup(String selector) {
        Node n = super.lookup(selector);
        if (n == null) {
            for (Node c : getChildren()) {
                n = c.lookup(selector);
                if (n != null) return n;
            }
        }
        return n;
    }

    /**
     * Please Note: This method should never create the results set,
     * let the Node class implementation do this!
     */
    @Override List<Node> lookupAll(Selector selector, List<Node> results) {
        results = super.lookupAll(selector, results);
        for (Node c : getChildren()) {
            results = c.lookupAll(selector, results);
        }
        return results;
    }
    
    /** @treatAsPrivate implementation detail */
    private javafx.beans.property.ObjectProperty<TraversalEngine> impl_traversalEngine;


    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_traversalEngine(TraversalEngine value) {
        impl_traversalEngineProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final TraversalEngine getImpl_traversalEngine() {
        return impl_traversalEngine == null ? null : impl_traversalEngine.get();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final ObjectProperty<TraversalEngine> impl_traversalEngineProperty() {
        if (impl_traversalEngine == null) {
            impl_traversalEngine =
                    new SimpleObjectProperty<TraversalEngine>(
                            this, "impl_traversalEngine");
        }
        return impl_traversalEngine;
    }

    /***********************************************************************
     *                               Layout                                *
     *                                                                     *
     *  Functions and variables related to the layout scheme used by       *
     *  JavaFX. Includes both public and private API.                      *
     *                                                                     *
     **********************************************************************/
    /**
     * Indicates that this Node and its subnodes requires a layout pass on
     * the next pulse.
     */
    private ReadOnlyBooleanWrapper needsLayout;

    protected final void setNeedsLayout(boolean value) {
        needsLayoutPropertyImpl().set(value);
    }

    public final boolean isNeedsLayout() {
        return needsLayout == null ? true : needsLayout.get();
    }

    public final ReadOnlyBooleanProperty needsLayoutProperty() {
        return needsLayoutPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper needsLayoutPropertyImpl() {
        if (needsLayout == null) {
            needsLayout = new ReadOnlyBooleanWrapper(this, "needsLayout", true);
        }
        return needsLayout;
    }

    /**
     * This package levelis used only by Node. It is set to true while
     * the layout() function is processing and set to false on the conclusion.
     * It is used by the Node to decide whether to perform CSS updates
     * synchronously or asynchronously.
     */
    boolean performingLayout = false;

    private boolean sizeCacheClear = true;
    private double prefWidthCache = -1;
    private double prefHeightCache = -1;
    private double minWidthCache = -1;
    private double minHeightCache = -1;

    /**
     * Requests a layout pass to be performed before the next scene is
     * rendered. This is batched up asynchronously to happen once per
     * "pulse", or frame of animation.
     * <p>
     * If this parent is either a layout root or unmanaged, then it will be
     * added directly to the scene's dirty layout list, otherwise requestLayout
     * will be invoked on its parent.
     */
    public void requestLayout() {
        if (!isNeedsLayout()) {
            prefWidthCache = -1;
            prefHeightCache = -1;
            minWidthCache = -1;
            minHeightCache = -1;
            PlatformLogger logger = Logging.getLayoutLogger();
            if (logger.isLoggable(PlatformLogger.FINER)) {
                logger.finer(this.toString());
            }

            setNeedsLayout(true);
            if (isLayoutRoot()) {
                if (getScene() != null) {
                    if (logger.isLoggable(PlatformLogger.FINER)) {
                        logger.finer(this.toString()+" layoutRoot added to scene dirty layout list");
                    }
                    getScene().addToDirtyLayoutList(this);
                }
            } else if (getParent() != null) {
                getParent().requestLayout();
            }
        } else {
            clearSizeCache();
        }
    }

    void clearSizeCache() {
        if (sizeCacheClear) {
            return;
        }
        sizeCacheClear = true;
        prefWidthCache = -1;
        prefHeightCache = -1;
        minWidthCache = -1;
        minHeightCache = -1;
        if (!isLayoutRoot()) {
            if (getParent() != null) {
                getParent().clearSizeCache();
            }
        }
    }

    @Override public double prefWidth(double height) {
        if (height == -1) {
            if (prefWidthCache == -1) {
                prefWidthCache = computePrefWidth(-1);
                sizeCacheClear = false;
            } 
            return prefWidthCache;
        } else {
            return computePrefWidth(height);
        }
    }

    @Override public double prefHeight(double width) {
        if (width == -1) {
            if (prefHeightCache == -1) {
                prefHeightCache = computePrefHeight(-1);
                sizeCacheClear = false;
            } 
            return prefHeightCache;
        } else {
            return computePrefHeight(width);
        }
    }

    @Override public double minWidth(double height) {
        if (height == -1) {
            if (minWidthCache == -1) {
                minWidthCache = computeMinWidth(-1);
                sizeCacheClear = false;
            }
            return minWidthCache;
        } else {
            return computeMinWidth(height);
        }
    }

    @Override public double minHeight(double width) {
        if (width == -1) {
            if (minHeightCache == -1) {
                minHeightCache = computeMinHeight(-1);
                sizeCacheClear = false;
            }
            return minHeightCache;
        } else {
            return computeMinHeight(width);
        }
    }

    // PENDING_DOC_REVIEW
    /**
     * Calculates the preferred width of this {@code Parent}. The default
     * implementation calculates this width as the width of the area occupied
     * by its managed children when they are positioned at their
     * current positions at their preferred widths.
     *
     * @param height the height that should be used if preferred width depends
     *      on it
     * @return the calculated preferred width
     */
    protected double computePrefWidth(double height) {
        double minX = 0;
        double maxX = 0;
        for (int i = 0; i < getChildren().size(); i++) {
            Node node = getChildren().get(i);
            if (node.isManaged()) {
                final double x = node.getLayoutBounds().getMinX() + node.getLayoutX();
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x + node.prefWidth(-1));
            }
        }
        return maxX - minX;
    }

    // PENDING_DOC_REVIEW
    /**
     * Calculates the preferred height of this {@code Parent}. The default
     * implementation calculates this height as the height of the area occupied
     * by its managed children when they are positioned at their current
     * positions at their preferred heights.
     *
     * @param width the width that should be used if preferred height depends
     *      on it
     * @return the calculated preferred height
     */
    protected double computePrefHeight(double width) {
        double minY = 0;
        double maxY = 0;
        for (int i = 0; i < getChildren().size(); i++) {
            Node node = getChildren().get(i);
            if (node.isManaged()) {
                final double y = node.getLayoutBounds().getMinY() + node.getLayoutY();
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y + node.prefHeight(-1));
            }
        }
        return maxY - minY;
    }

    /**
     * Calculates the minimum width of this {@code Parent}. The default
     * implementation simply returns the pref width.
     *
     * @param height the height that should be used if min width depends
     *      on it
     * @return the calculated min width
     */
    protected double computeMinWidth(double height) {
        return super.prefWidth(height);
    }

    // PENDING_DOC_REVIEW
    /**
     * Calculates the min height of this {@code Parent}. The default
     * implementation simply returns the pref height;
     *
     * @param width the width that should be used if min height depends
     *      on it
     * @return the calculated min height
     */
    protected double computeMinHeight(double width) {
        return super.prefHeight(width);
    }

    /**
     * Calculates the baseline offset based on the first managed child. If there
     * is no such child, returns {@link Node#getBaselineOffset()}.
     *
     * @return baseline offset
     */
    @Override public double getBaselineOffset() {
        for (Node child: getChildren()) {
            if (child.isManaged()) {
                return child.getLayoutBounds().getMinY() + child.getLayoutY() + child.getBaselineOffset();
            }
        }
        return super.getBaselineOffset();
    }

    /**
     * Executes a top-down layout pass on the scene graph under this parent.
     */
    public final void layout() {
        if (isNeedsLayout()) {
            performingLayout = true;

            PlatformLogger logger = Logging.getLayoutLogger();
            if (logger.isLoggable(PlatformLogger.FINE)) {
                logger.fine(this+" size: "+
                        getLayoutBounds().getWidth()+" x "+getLayoutBounds().getHeight());
            }

            // layout the children in this parent.
            layoutChildren();
            // clear flag before recursing down so requestLayout calls arn't swallowed
            setNeedsLayout(false);

            // Perform layout on each child, hoping it has random access performance!
            final ObservableList<Node> _children_ = getChildren();
            final int count = _children_.size();
            for (int i=0; i<count; i++) {
                final Node c = _children_.get(i);
                if (c instanceof Parent) {
                    ((Parent) c).layout();
                }
            }
            performingLayout = false;
        }
    }

    /**
     * Invoked during the layout pass to layout the children in this
     * {@code Parent}. By default it will only set the size of managed,
     * resizable content to their preferred sizes and does not do any node
     * positioning.
     * <p>
     * Subclasses should override this function to layout content as needed.
     */
    protected void layoutChildren() {
        impl_resizeChildren(true);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected final void impl_resizeChildren(boolean snapToPixel) {
        for (Node node : getChildren()) {
            if (node.isResizable() && node.isManaged()) {
                node.autosize();
            }
        }
    }

    boolean isSceneRoot() {
        return getScene() != null? getScene().getRoot() == this : false;
    }

    boolean isLayoutRoot() {
        return !isManaged() || isSceneRoot();
    }

    /***********************************************************************
     *                                                                     *
     *                         Stylesheet Handling                         *
     *                                                                     *
     **********************************************************************/

    
    /**
     * A ObservableList of string URLs linking to the stylesheets to use with this scene's
     * contents. For additional information about using CSS with the
     * scene graph, see the <a href="doc-files/cssref.html">CSS Reference
     * Guide</a>.
     */
    private final ObservableList<String> stylesheets = new TrackableObservableList<String>() {
        @Override
        protected void onChanged(Change<String> c) {
            StyleManager.getInstance().parentStylesheetsChanged(c);
            // RT-9784 - if stylesheet is removed, reset styled properties to 
            // their initial value.
            while(c.next()) {
                if (c.wasRemoved() == false) continue;
                impl_cssResetInitialValues();
                break; // no point in resetting more than once...
            }
            impl_reapplyCSS();
        }
    };

    /**
     * Gets an observable list of string URLs linking to the stylesheets to use 
     * with this Parent's contents. For additional information about using CSS 
     * with the scene graph, see the <a href="doc-files/cssref.html">CSS Reference
     * Guide</a>.
     *
     * @return the list of stylesheets to use with this Parent
     */
    public final ObservableList<String> getStylesheets() { return stylesheets; }
    
    
    /**
     * This method recurses up the parent chain until parent is null. As the
     * stack unwinds, if the Parent has stylesheets, they are added to the
     * list.
     * 
     * It is possible to override this method to stop the recursion. This allows
     * a Parent to have a set of stylesheets distinct from its Parent. 
     * 
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated // SB-dependency: RT-21247 has been filed to track this
    public /* Do not make this final! */ List<String> impl_getAllParentStylesheets() {
        
        List<String> list = null;
        final Parent myParent = getParent();
        if (myParent != null) {

            //
            // recurse so that stylesheets of Parents closest to the root are 
            // added to the list first. The ensures that declarations for 
            // stylesheets further down the tree (closer to the leaf) have
            // a higer ordinal in the cascade.
            //
            list = myParent.impl_getAllParentStylesheets();
        }
        
        if (stylesheets != null && stylesheets.isEmpty() == false) {
            if (list == null) list = new ArrayList<String>(stylesheets.size());
            for (int n=0,nMax=stylesheets.size(); n<nMax; n++) 
                list.add(stylesheets.get(n));
        }
        
        return list;
        
    }
    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public void impl_processCSS(boolean reapply) {
        // Determine whether we will need to reapply from here on down
        boolean flag = reapply || cssFlag == CSSFlags.REAPPLY;
        // Let the super implementation handle CSS for this node
        super.impl_processCSS(flag);
        // For each child, process CSS
        final List kids = this.getChildren();
        final int max = kids.size();
        for (int c=0; c<max; c++) {
            Node kid = (Node)kids.get(c);
            if (kid != null) {
                kid.impl_processCSS(flag);
            }
        }
    }
    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public void impl_cssResetInitialValues() {
        
        // RT-9784
        
        super.impl_cssResetInitialValues();

        final List kids = this.getChildren();
        final int max = kids.size();
        for (int c=0; c<max; c++) {
            Node kid = (Node)kids.get(c);
            if (kid != null) {
                kid.impl_cssResetInitialValues();
            }
        }
    }
    

    /***********************************************************************
     *                               Misc                                  *
     *                                                                     *
     *  Initialization and other functions                                 *
     *                                                                     *
     **********************************************************************/


    /**
     * Constructs a new {@code Parent}.
     */
    protected Parent() {
        computeDirtyScene(null);
        requestLayout();
    }


    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected @Override PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGGroup();
    }

    PGGroup getPGGroup() {
        return ((PGGroup) impl_getPGNode());
    }


    /***************************************************************************
     *                                                                         *
     *                         Bounds Computations                             *
     *                                                                         *
     *  This code originated in GroupBoundsHelper (part of javafx-sg-common)   *
     *  but has been ported here to the FX side since we cannot rely on the PG *
     *  side for computing the bounds (due to the decoupling of the two        *
     *  scenegraphs for threading and other purposes).                         *
     *                                                                         *
     *  Unfortunately, we cannot simply reuse GroupBoundsHelper without some  *
     *  major (and hacky) modification due to the fact that GroupBoundsHelper  *
     *  relies on PG state and we need to do similar things here that rely on  *
     *  core scenegraph state. Unfortunately, that means we made a port.       *
     *                                                                         *
     **************************************************************************/

    private BaseBounds tmp = new RectBounds();
    /**
     * The cached bounds for the Group. If the cachedBounds are invalid
     * then we have no history of what the bounds are, or were.
     */
    private BaseBounds cachedBounds = new RectBounds();

    /**
     * Indicates that the cachedBounds is invalid (or old) and need to be recomputed.
     * If cachedBoundsInvalid is true, but cachedBounds is not empty,
     * then when we recompute the cachedBounds we can consider the
     * values in cachedBounds to represent the last valid bounds for the group.
     * This is useful for several fast paths.
     */
    private boolean cachedBoundsInvalid = true;

    /**
     * This set is used to track all of the children of this group which are
     * dirty. It is only used in cases where the number of children is > some
     * value (currently 10). For very wide trees, this can provide a very
     * important speed boost. For the sake of memory consumption, this is
     * null unless the number of children ever crosses the threshold where
     * it will be activated.
     */
    private LinkedHashSet<Node> dirtyChildren = null;

    private Node top;
    private Node left;
    private Node bottom;
    private Node right;
    private Node near;
    private Node far;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        // If we have no children, our bounds are invalid
        if (children.isEmpty()) {
            return bounds.makeEmpty();
        }

        if (tx.isTranslateOrIdentity()) {
            // this is a transform which is only doing translations, or nothing
            // at all (no scales, rotates, or shears)
            // so in this case we can easily use the cached bounds
            if (cachedBoundsInvalid) recomputeBounds();
            if (tx != null && !tx.isIdentity()) {
                bounds = bounds.deriveWithNewBounds((float)(cachedBounds.getMinX() + tx.getMxt()),
                                 (float)(cachedBounds.getMinY() + tx.getMyt()),
                                 (float)(cachedBounds.getMinZ() + tx.getMzt()),
                                 (float)(cachedBounds.getMaxX() + tx.getMxt()),
                                 (float)(cachedBounds.getMaxY() + tx.getMyt()),
                                 (float)(cachedBounds.getMaxZ() + tx.getMzt()));
            } else {
                bounds = bounds.deriveWithNewBounds(cachedBounds);
            }
            if (dirtyChildren != null) dirtyChildren.clear();
            return bounds;
        } else {
            // there is a scale, shear, or rotation happening, so need to
            // do the full transform!
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;
            boolean first = true;
            for (Node node : getChildren()) {
                if (node.isVisible()) {
                    bounds = node.getTransformedBounds(bounds, tx);
                    // if the bounds of the child are invalid, we don't want
                    // to use those in the remaining computations.
                    if (bounds.isEmpty()) continue;
                    if (first) {
                        minX = bounds.getMinX();
                        minY = bounds.getMinY();
                        minZ = bounds.getMinZ();
                        maxX = bounds.getMaxX();
                        maxY = bounds.getMaxY();
                        maxZ = bounds.getMaxZ();
                        first = false;
                    } else {
                        minX = Math.min(bounds.getMinX(), minX);
                        minY = Math.min(bounds.getMinY(), minY);
                        minZ = Math.min(bounds.getMinZ(), minZ);
                        maxX = Math.max(bounds.getMaxX(), maxX);
                        maxY = Math.max(bounds.getMaxY(), maxY);
                        maxZ = Math.max(bounds.getMaxZ(), maxZ);
                    }
                }
            }
            // if "first" is still true, then we didn't have any children with
            // non-empty bounds and thus we must return an empty bounds,
            // otherwise we have non-empty bounds so go for it.
            if (first)
                bounds.makeEmpty();
            else
                bounds = bounds.deriveWithNewBounds((float)minX, (float)minY, (float)minZ,
                        (float)maxX, (float)maxY, (float)maxZ);
            if (dirtyChildren != null) dirtyChildren.clear();
            return bounds;
        }
    }

    // Returns TRUE if the bounds have changed, FALSE otherwise
    boolean childAdded(Node node) {
        // If there is no history as to what the bounds were (ie, if
        // cachedBounds are invalid) then we punt since the next call
        // to get the bounds will force a full recomputation anyway.
        
        // The above comment is not right. With invalid but non-empty bounds 
        // the recomputation goes through fast-path which doesn't consider
        // added and removed nodes. I'm turning off this optimizaion.

        //if (cachedBoundsInvalid) return true;

        // If one of the edges has been cleared by the removal of a node,
        // then there is no point to continuing because we will have to
        // completely revalidate later.
        if (top == null || bottom == null || left == null || right == null
                || near == null || far == null) {
            cachedBounds.makeEmpty();
            cachedBoundsInvalid = true;
            return true;
        }

        // If we got here then the top/bottom/left/right/near/far are still set
        // to a valid node and we know the old size of the Group. So what we are
        // going to do is execute a fastpath where we figure out if any of the
        // new Nodes "extend" the bounds, and if so, they will form the new
        // edges and the cachedBounds will be updated accordingly. We will
        // also communicate that as a result of adding these Nodes, the
        // bounds have changed.

        boolean extended = false;

        // set to indicate which edges need to be cleared so we can recompute
        if (node.isVisible()) {
            tmp = node.getTransformedBounds(tmp, BaseTransform.IDENTITY_TRANSFORM);
            if (!tmp.isEmpty()) {
                node.boundsChanged = false;
                double tmpx = tmp.getMinX();
                double tmpy = tmp.getMinY();
                double tmpz = tmp.getMinZ();
                double tmpx2 = tmp.getMaxX();
                double tmpy2 = tmp.getMaxY();
                double tmpz2 = tmp.getMaxZ();
                double cx = cachedBounds.getMinX();
                double cy = cachedBounds.getMinY();
                double cz = cachedBounds.getMinZ();
                double cx2 = cachedBounds.getMaxX();
                double cy2 = cachedBounds.getMaxY();
                double cz2 = cachedBounds.getMaxZ();

                double minX = Math.min(tmpx, cx);
                double minY = Math.min(tmpy, cy);
                double minZ = Math.min(tmpz, cz);
                double maxX = Math.max(tmpx2, cx2);
                double maxY = Math.max(tmpy2, cy2);
                double maxZ = Math.max(tmpz2, cz2);

                if (tmpy < cy) { extended = true; top = node; }
                if (tmpx < cx) { extended = true; left = node; }
                if (tmpz < cz) { extended = true; near = node; }
                if (tmpy2 > cy2) { extended = true; bottom = node; }
                if (tmpx2 > cx2) { extended = true; right = node; }
                if (tmpz2 > cz2) { extended = true; far = node; }

                // update the cached bounds
                cachedBounds = cachedBounds.deriveWithNewBounds((float)minX, (float)minY, (float)minZ,
                        (float)maxX, (float)maxY, (float)maxZ);
            }
        }
        // return whether or not the bounds have been extended (and thus, are
        // dirty)
        return extended;
    }

    // This is called when either the child is actually removed, OR IF IT IS
    // TOGGLED TO BE INVISIBLE. This is because in both cases it needs to be
    // cleared from the state which manages bounds.
    boolean childRemoved(Node node) {
        // clear this flag because Group no longer knows whether the bounds
        // have changed or not relative to itself
        node.boundsChanged = false;
        // If there is no history as to what the bounds were (ie, if
        // cachedBounds are invalid) then we punt since the next call
        // to get the bounds will force a full recomputation anyway.

        // The above comment is not right. With invalid but non-empty bounds 
        // the recomputation goes through fast-path which doesn't consider
        // added and removed nodes. I'm turning off this optimizaion.

        // if (cachedBoundsInvalid) return true;

        // Since there was some prior size to the Group, all the nodes that
        // were removed are checked to see if they formed one of the edges. If
        // so, the edge variable is cleared
        if (node == top) top = null;
        if (node == left) left = null;
        if (node == bottom) bottom = null;
        if (node == right) right = null;
        if (node == near) near = null;
        if (node == far) far = null;

        // If one of the edges has been cleared by the removal of a node,
        // then there is no point to continuing because we will have to
        // completely revalidate later.
        if (top == null || bottom == null || left == null || right == null
                || near == null || far == null) {
            cachedBounds.makeEmpty();
            cachedBoundsInvalid = true;
            top = left = bottom = right = near = far = null;
            return true;
        }

        // Return false since nothing has changed that will affect the bounds.
        return false;
    }

    /**
     * Recomputes the bounds from scratch and saves the cached bounds.
     */
    void recomputeBounds() {
        // fast path for case of no children
        if (children.isEmpty()) {
            if (dirtyChildren != null) dirtyChildren.clear();
            cachedBoundsInvalid = false;
            cachedBounds.makeEmpty();
            return;
        }

        // fast path for case of 1 child
        if (children.size() == 1) {
            if (dirtyChildren != null) dirtyChildren.clear();
            cachedBoundsInvalid = false;
            Node node = children.get(0);
            if (node == null) return;

            if (node.isVisible()) {
                cachedBounds = node.getTransformedBounds(cachedBounds, BaseTransform.IDENTITY_TRANSFORM);
            } else {
                cachedBounds.makeEmpty();
            }
            node.boundsChanged = false;
            return;
        }

        // We will attempt to use a fastpath through this function. We can use
        // the fastpath as long as we have an old size of the Group recorded
        // (cachedBounds is invalid but not empty), and each of the edge
        // variables have a value.
        if (cachedBounds != null
            && !cachedBounds.isEmpty()
            && top != null
            && left != null
            && bottom != null
            && right != null
            && near != null
            && far != null)
        {
            // The fastpath will simply iterate over each of the Nodes in the
            // group. If any node has "dirty" bounds, then I know that I must
            // recompute its bounds. When I do so, I will take special care
            // if it was once one of my edges, and also special care if it has
            // moved to become a new edge.

            // First, look at the nodes which form the top, left, bottom, right,
            // near and far edges. If any of them are now invisible (visible = false)
            // then they no longer contribute to the bounds of the group and
            // therefore, this fast path cannot be used
            if (top.isVisible() && left.isVisible() && bottom.isVisible() && right.isVisible()
                    && near.isVisible() && far.isVisible()) {
                // This fastpath becomes degenerate if I get to the end only to
                // discover that the very last node has moved inward, in which
                // case I have to throw away my work and simply sum up all the
                // nodes in the group.
                boolean quit = false;

                // These are local references to the cached bounds vars
                double cx = cachedBounds.getMinX();
                double cy = cachedBounds.getMinY();
                double cz = cachedBounds.getMinZ();
                double cx2 = cachedBounds.getMaxX();
                double cy2 = cachedBounds.getMaxY();
                double cz2 = cachedBounds.getMaxZ();

                // These indicate the bounds of the Group as computed by this
                // function (had to add the 'b' suffix to suppress a spurious
                // compiler error message)
                double minXb=cx;
                double minYb=cy;
                double minZb=cz;
                double maxXb=cx2;
                double maxYb=cy2;
                double maxZb=cz2;

                // iterate over all the dirty nodes
                if (dirtyChildren != null) {
                    for (Node node : dirtyChildren) {
                        if (node.isVisible() && node.boundsChanged) {
                            tmp = node.getTransformedBounds(tmp, BaseTransform.IDENTITY_TRANSFORM);
                            if (tmp.isEmpty()) {
                                // If this node formed one of the edges, and if it has
                                // moved off that edge (inward), then we break out of this
                                // loop and quit the fast path
                                if (node == top) { quit = true; break; }
                                if (node == left) { quit = true; break; }
                                if (node == bottom) { quit = true; break; }
                                if (node == right) { quit = true; break; }
                                if (node == near) { quit = true; break; }
                                if (node == far) { quit = true; break; }
                            } else {
                                double tmpx = tmp.getMinX();
                                double tmpy = tmp.getMinY();
                                double tmpz = tmp.getMinZ();
                                double tmpx2 = tmp.getMaxX();
                                double tmpy2 = tmp.getMaxY();
                                double tmpz2 = tmp.getMaxZ();

                                // If this node formed one of the edges, and if it has
                                // moved off that edge (inward), then we break out of this
                                // loop and quit the fast path
                                if (node == top && tmpy > cy) { quit = true; break; }
                                if (node == left && tmpx > cx) { quit = true; break; }
                                if (node == near && tmpz > cz) { quit = true; break; }
                                if (node == bottom && tmpy2 < cy2) { quit = true; break; }
                                if (node == right && tmpx2 < cx2) { quit = true; break; }
                                if (node == far && tmpz2 < cz2) { quit = true; break; }

                                // If this node forms an edge, then we will set it to be the
                                // node for this edge and update the min/max values
                                if (tmpy < minYb) { minYb = tmpy; top = node; }
                                if (tmpx < minXb) { minXb = tmpx; left = node; }
                                if (tmpz < minZb) { minZb = tmpz; near = node; }
                                if (tmpy2 > maxYb) { maxYb = tmpy2; bottom = node; }
                                if (tmpx2 > maxXb) { maxXb = tmpx2; right = node; }
                                if (tmpz2 > maxZb) { maxZb = tmpz2; far = node; }
                            }
                        }
                        node.boundsChanged = false;
                    }
                } else {
                    final ObservableList<Node> _children_ = getChildren();
                    final int count = _children_.size();
                    for (int i=0; i<count; i++) {
                        final Node node = _children_.get(i);
                        if (node == null)
                            continue;
                        if (node.isVisible() && node.boundsChanged) {
                            tmp = node.getTransformedBounds(tmp, BaseTransform.IDENTITY_TRANSFORM);
                            if (tmp.isEmpty()) {
                                // If this node formed one of the edges, and if it has
                                // moved off that edge (inward), then we break out of this
                                // loop and quit the fast path
                                if (node == top) { quit = true; break; }
                                if (node == left) { quit = true; break; }
                                if (node == bottom) { quit = true; break; }
                                if (node == right) { quit = true; break; }
                                if (node == near) { quit = true; break; }
                                if (node == far) { quit = true; break; }
                            } else {
                                double tmpx = tmp.getMinX();
                                double tmpy = tmp.getMinY();
                                double tmpz = tmp.getMinZ();
                                double tmpx2 = tmp.getMaxX();
                                double tmpy2 = tmp.getMaxY();
                                double tmpz2 = tmp.getMaxZ();

                                // If this node formed one of the edges, and if it has
                                // moved off that edge (inward), then we break out of this
                                // loop and quit the fast path
                                if (node == top && tmpy > cy) { quit = true; break; }
                                if (node == left && tmpx > cx) { quit = true; break; }
                                if (node == near && tmpz > cz) { quit = true; break; }
                                if (node == bottom && tmpy2 < cy2) { quit = true; break; }
                                if (node == right && tmpx2 < cx2) { quit = true; break; }
                                if (node == far && tmpz2 < cz2) { quit = true; break; }

                                // If this node forms an edge, then we will set it to be the
                                // node for this edge and update the min/max values
                                if (tmpy < minYb) { minYb = tmpy; top = node; }
                                if (tmpx < minXb) { minXb = tmpx; left = node; }
                                if (tmpz < minZb) { minZb = tmpz; near = node; }
                                if (tmpy2 > maxYb) { maxYb = tmpy2; bottom = node; }
                                if (tmpx2 > maxXb) { maxXb = tmpx2; right = node; }
                                if (tmpz2 > maxZb) { maxZb = tmpz2; far = node; }
                            }
                        }
                        node.boundsChanged = false;
                    }
                }

                if (dirtyChildren != null) dirtyChildren.clear();

                if (!quit) {
                    // The fastpath succeeded, so we can simply update the
                    // cachedBounds, clear the cachedBoundsInvalid flag and return
                    cachedBoundsInvalid = false;
                    cachedBounds = cachedBounds.deriveWithNewBounds((float)minXb, (float)minYb, (float)minZb,
                            (float)maxXb, (float)maxYb, (float)maxZb);
                    return;
                }
            }
            // fall through to the long case
        }

        // These indicate the bounds of the Group as computed by this function
        double minX = 0;
        double minY = 0;
        double minZ = 0;
        double maxX = 0;
        double maxY = 0;
        double maxZ = 0;

        // The long case is pretty simple -- visit each and every node, get
        // the bounds of each node, figure out the edge nodes, get the min/max
        // for the x/y/x2/y2 and then update cachedBounds and cachedBoundsInvalid
        boolean first = true;
        final ObservableList<Node> _children_ = getChildren();
        final int count = _children_.size();
        for (int i=0; i<count; i++) {
            final Node node = _children_.get(i);
            if (node == null)
                continue;

            node.boundsChanged = false;
            if (node.isVisible()) {
                tmp = node.getTransformedBounds(tmp, BaseTransform.IDENTITY_TRANSFORM);
                if (!tmp.isEmpty()) {
                    double tmpx = tmp.getMinX();
                    double tmpy = tmp.getMinY();
                    double tmpz = tmp.getMinZ();
                    double tmpx2 = tmp.getMaxX();
                    double tmpy2 = tmp.getMaxY();
                    double tmpz2 = tmp.getMaxZ();

                    if (first) {
                        top = left = bottom = right = near = far = node;
                        minX = tmpx;
                        minY = tmpy;
                        minZ = tmpz;
                        maxX = tmpx2;
                        maxY = tmpy2;
                        maxZ = tmpz2;
                        first = false;
                    } else {
                        if (tmpy < minY) { minY = tmpy; top = node; }
                        if (tmpx < minX) { minX = tmpx; left = node; }
                        if (tmpz < minZ) { minZ = tmpz; near = node; }
                        if (tmpy2 > maxY) { maxY = tmpy2; bottom = node; }
                        if (tmpx2 > maxX) { maxX = tmpx2; right = node; }
                        if (tmpz2 > maxZ) { maxZ = tmpz2; far = node; }
                    }
                }
            }
        }

        if (dirtyChildren != null) dirtyChildren.clear();
        cachedBoundsInvalid = false;
        if (first) {
            top = left = bottom = right = near = far = null;
            cachedBounds.makeEmpty();
        } else {
            cachedBounds = cachedBounds.deriveWithNewBounds((float)minX, (float)minY, (float)minZ,
                    (float)maxX, (float)maxY, (float)maxZ);
        }
    }

    /**
     * Overridden to make sure boundsInvalid gets set to true
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_geomChanged() {
        cachedBoundsInvalid = true;
        super.impl_geomChanged();
    }

    /**
     * Called by Node whenever its bounds have changed.
     */
    void childBoundsChanged(Node node) {
        if (!node.isVisible()) {
            // the change in bounds of this child node has no bearing on
            // the bounds of the group, so forget it
            return;
        }

        // mark the node such that the parent knows that the child's bounds
        // are not in sync with this parent. In this way, when the bounds
        // need to be computed, we'll come back and figure out the new bounds
        // for all the children which have boundsChanged set to true
        if (!node.boundsChanged) {
            node.boundsChanged = true;
            if (dirtyChildren != null) dirtyChildren.add(node);
        }
        // go ahead and indicate that the geom has changed for this parent,
        // even though once we figure it all out it may be that the bounds
        // have not changed
        impl_geomChanged();
    }

    /**
     * Called by node whenever the visibility of the node changes.
     */
    void childVisibilityChanged(Node node) {
        // cachedBoundsInvalid will be set to true in impl_geomChanged();
        cachedBounds.makeEmpty();
        impl_geomChanged();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        final Point2D tempPt = TempState.getInstance().point;
        for (Node node : getChildren()) {
            tempPt.x = (float)localX;
            tempPt.y = (float)localY;
            try {
                node.parentToLocal(tempPt);
            } catch (NoninvertibleTransformException e) {
                continue;
            }
            if (node.contains(tempPt.x, tempPt.y)) {
                return true;
            }
        }
        return false;
    }

    /** 
     * temporary to help debug scene graph
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final void impl_printBranch() {
        final int nodecount = printBranch("");
        System.out.println("total node count="+nodecount);
    }

    private int printBranch(String indent) {
        int nodecount = 0;

        print(this,indent);
        nodecount++;
        for (Node child: getChildren()) {
            if (child instanceof Parent) {
                nodecount += ((Parent)child).printBranch(indent+"  ");
            } else {
                print(child,indent);
                nodecount++;
            }
        }

        return nodecount;
    }

    private void print(Node node, String indent) {
        javafx.geometry.Bounds lb = node.getLayoutBounds();
        System.out.println(indent+node.getClass().getName()+" "+getId()+
                " layout="+(lb.getMinX()+node.getLayoutX())+","+(lb.getMinY()+node.getLayoutY())+" "+
                lb.getWidth()+"x"+lb.getHeight()+" needsLayout="+isNeedsLayout());
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        return alg.processContainerNode(this, ctx);
    }
}
